/* 
 * EDStatic Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.PrimitiveArray;
import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.String2LogOutputStream;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.Projects;
import gov.noaa.pfel.coastwatch.sgt.Boundaries;
import gov.noaa.pfel.coastwatch.sgt.FilledMarkerRenderer;
import gov.noaa.pfel.coastwatch.sgt.GSHHS;
import gov.noaa.pfel.coastwatch.sgt.PathCartesianRenderer;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import gov.noaa.pfel.erddap.*;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.variable.*;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import org.apache.commons.codec.digest.DigestUtils;  //in netcdf-all.jar

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.index.IndexWriter;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.store.SimpleFSDirectory;


import org.verisign.joid.consumer.OpenIdFilter;

/** 
 * This class holds a lot of static information set from the setup.xml and messages.xml
 * files and used by all the other ERDDAP classes. 
 */
public class EDStatic { 

    /** The all lowercase name for the program that appears in urls. */
    public final static String programname = "erddap";

    /** The uppercase name for the program that appears on web pages. */
    public final static String ProgramName = "ERDDAP";

    /** This changes with each release. 
     * <br>See Changes information in /downloads/setup.html .
     * <br>0.1 started on 2007-09-17
     * <br>0.11 released on 2007-11-09
     * <br>0.12 released on 2007-12-05
     * <br>0.2 released on 2008-01-10
     * <br>From here on, odd .01 are used during development
     * <br>0.22 released on 2008-02-21
     * <br>0.24 released on 2008-03-03
     * <br>0.26 released on 2008-03-11
     * <br>0.28 released on 2008-04-14
     * <br>1.00 released on 2008-05-06
     * <br>1.02 released on 2008-05-26
     * <br>1.04 released on 2008-06-10
     * <br>1.06 released on 2008-06-20
     * <br>1.08 released on 2008-07-13
     * <br>1.10 released on 2008-10-14
     * <br>1.12 released on 2008-11-02
     * <br>1.14 released on 2009-03-17
     * <br>1.16 released on 2009-03-26
     * <br>1.18 released on 2009-04-08
     * <br>1.20 released on 2009-07-02
     * <br>1.22 released on 2009-07-05
     * <br>1.24 released on 2010-08-06
     * <br>1.26 released on 2010-08-25
     * <br>1.28 released on 2010-08-27
     * <br>1.30 released on 2011-04-29
     * <br>1.32 released on 2011-05-20
     * <br>1.34 released on 2011-06-15
     * <br>1.36 released on 2011-08-01
     * <br>1.38 released on 2012-04-21
     * <br>1.40 released on 2012-10-25
     * <br>1.42 released on 2012-11-26
     * <br>1.44 released on 2013-05-30
     * <br>1.46 released on 2013-07-09
     */   
    public static String erddapVersion = "1.46";  

    /** 
     * This is almost always false.  
     * During development, Bob sets this to true. No one else needs to. 
     * If true, ERDDAP uses setup2.xml and datasets2.xml (and messages2.xml if it exists). 
     */
public static boolean developmentMode = false;

    /** This identifies the dods server/version that this mimics. */
    public static String dapVersion = "DAP/2.0";   //???
    public static String serverVersion = "dods/3.7"; //this is what thredds replies
      //drds at http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.ver replies "DODS/3.2"
      //both reply with server version, neither replies with coreVersion
      //spec says #.#.#, but Gallagher says #.# is fine.

    /** 
     * contentDirectory is the local directory on this computer, e.g., [tomcat]/content/erddap/ 
     * It will have a slash at the end.
     */
    public static String contentDirectory;

    public final static String INSTITUTION = "institution";

    /* contextDirectory is the local directory on this computer, e.g., [tomcat]/webapps/erddap/ */
    public static String contextDirectory = SSR.getContextDirectory();
    //fgdc and iso19115XmlDirectory are used for virtual URLs.
    public final static String fgdcXmlDirectory     = "metadata/fgdc/xml/";     //virtual
    public final static String iso19115XmlDirectory = "metadata/iso19115/xml/"; //virtual
    public final static String DOWNLOAD_DIR         = "download/";
    public final static String IMAGES_DIR           = "images/";
    public final static String PUBLIC_DIR           = "public/"; 
    public static String
        fullPaletteDirectory = contextDirectory + "WEB-INF/cptfiles/",
        fullPublicDirectory  = contextDirectory + PUBLIC_DIR,
        downloadDir          = contextDirectory + DOWNLOAD_DIR, //local directory on this computer
        imageDir             = contextDirectory + IMAGES_DIR;   //local directory on this computer
    public static Tally tally = new Tally();
    public static int failureTimesDistributionLoadDatasets[] = new int[String2.DistributionSize];
    public static int failureTimesDistribution24[]           = new int[String2.DistributionSize];
    public static int failureTimesDistributionTotal[]        = new int[String2.DistributionSize];
    public static int majorLoadDatasetsDistribution24[]      = new int[String2.DistributionSize];
    public static int majorLoadDatasetsDistributionTotal[]   = new int[String2.DistributionSize];
    public static int minorLoadDatasetsDistribution24[]      = new int[String2.DistributionSize];
    public static int minorLoadDatasetsDistributionTotal[]   = new int[String2.DistributionSize];
    public static int responseTimesDistributionLoadDatasets[]= new int[String2.DistributionSize];
    public static int responseTimesDistribution24[]          = new int[String2.DistributionSize];
    public static int responseTimesDistributionTotal[]       = new int[String2.DistributionSize];
    public static int taskThreadFailedDistribution24[]       = new int[String2.DistributionSize];
    public static int taskThreadFailedDistributionTotal[]    = new int[String2.DistributionSize];
    public static int taskThreadSucceededDistribution24[]    = new int[String2.DistributionSize];
    public static int taskThreadSucceededDistributionTotal[] = new int[String2.DistributionSize];

    public static String datasetsThatFailedToLoad = "";
    public static String duplicateDatasetIDsMsg = "";
    public static StringBuffer memoryUseLoadDatasetsSB     = new StringBuffer(""); //thread-safe (1 thread writes but others may read)
    public static StringBuffer failureTimesLoadDatasetsSB  = new StringBuffer(""); //thread-safe (1 thread writes but others may read)
    public static StringBuffer responseTimesLoadDatasetsSB = new StringBuffer(""); //thread-safe (1 thread writes but others may read)
    public static HashSet requestBlacklist = null;
    public static String startupLocalDateTime = Calendar2.getCurrentISODateTimeStringLocal();
    public static int nGridDatasets = 0;  
    public static int nTableDatasets = 0;
    public static long lastMajorLoadDatasetsStartTimeMillis = System.currentTimeMillis();
    public static long lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis() - 1;

    /** userHashMap 
     * (key=user value=[encoded password, sorted roles String[]]) 
     * It is empty until the first LoadDatasets is finished and puts a new HashMap in place.
     * It is private so no other code can access the information except
     * via doesPasswordMatch() and getRoles().
     * MD5'd passwords should all already be lowercase.
     * No need to be thread-safe: one thread writes it, then put here where read only.
     */
    private static HashMap userHashMap = new HashMap(); 

    /** postUserHashMap is an additional list of users with a data structure just like userHashMap.
     * It may be created by EDDTableFromPostDatabase.
     * Note that the role is usually just "post" (not the POST-specific roles),
     * since the issue here is what *datasets* does the user have access to
     * (and the POST datasets are usually publicly available).
     * PostDatabase can just add users to userHashMap because userHashMap
     * is replaced every major LoadDatasets; to POST user info would be lost.
     * Passwords are MD5'd and so should all already be lowercase.
     * No need to be thread-safe: one thread writes it, then put here where read only.
     */
    private static HashMap postUserHashMap = null;  //only non-null if in use

    /** This is a HashMap of key=id value=thread that need to be interrupted/killed
     * when erddap is stopped in Tomcat.
     * For example, key="taskThread", value=taskThread.
     * The key make it easy to get a specific thread (e.g., to remove it).
     */
    public static ConcurrentHashMap runningThreads = new ConcurrentHashMap(16, 0.75f, 4); 

    //taskThread variables
    //Funnelling all taskThread tasks through one taskThread ensures
    //  that the memory requirements, bandwidth usage, cpu usage,
    //  and stress on remote servers will be minimal 
    //  (although at the cost of not doing the tasks faster / in parallel).
    //In a grid of erddaps, each will have its own taskThread, which is appropriate.
    public static ArrayList taskList = new ArrayList(); //keep here in case TaskThread needs to be restarted
    private static TaskThread taskThread;
    /** lastAssignedTask is used by EDDxxxCopy instances to keep track of 
     * the number of the last task assigned to taskThread.
     * key=datasetID value=Integer(task#)
     */
    public static ConcurrentHashMap lastAssignedTask = new ConcurrentHashMap(16, 0.75f, 4); 
    /** 
     * This returns the index number of the task in taskList (-1,0..) of the last completed task
     * (successful or not).
     * nFinishedTasks = lastFinishedTask + 1;
     */
    public static volatile int lastFinishedTask = -1;
    /** 
     * This returns the index number of the task in taskList (0..) that will be started
     * when the current task is finished.
     */
    public static volatile int nextTask = 0;


    /** This recieves key=startOfLocalSourceUrl value=startOfPublicSourceUrl from LoadDatasets 
     * and is used by EDD.convertToPublicSourceUrl. 
     */
    public static ConcurrentHashMap convertToPublicSourceUrl = new ConcurrentHashMap(16, 0.75f, 4);

    /** 
     * This returns the position of the "/" in if tFrom has "[something]//[something]/...",
     * and is thus a valid tFrom for convertToPublicSourceUrl.
     *
     * @return the po of the end "/" (or -1 if invalid).
     */
    public static int convertToPublicSourceUrlFromSlashPo(String tFrom) {
        if (tFrom == null) 
            return -1;
        int spo = tFrom.indexOf("//");
        if (spo > 0)
            spo = tFrom.indexOf("/", spo + 2);
        return spo;
    }

    /** For Lucene. */
    //The latest version as of writing this.
    //Since I recreate the index when erddap restarted, I can change anything
    //  (e.g., Directory type, Version) any time
    //  (no worries about compatibility with existing index).
    //useful documentatino 
    //  http://lucene.apache.org/java/3_5_0/queryparsersyntax.html
    //  http://wiki.apache.org/lucene-java/LuceneFAQ
    //  http://wiki.apache.org/lucene-java/BasicsOfPerformance
    //  http://affy.blogspot.com/2003/04/codebit-examples-for-all-of-lucenes.html
    public  static Version       luceneVersion = Version.LUCENE_35;
    public  final static String  luceneDefaultField = "text";
    //special characters to be escaped
    //see bottom of http://lucene.apache.org/java/3_5_0/queryparsersyntax.html
    public  static String        luceneSpecialCharacters = "+-&|!(){}[]^\"~*?:\\";

    //made below if useLuceneSearchEngine
    //there are many analyzers; this is a good starting point
    public  static Analyzer      luceneAnalyzer;    
    private static QueryParser   luceneQueryParser; //not thread-safe

    //made once by RunLoadDatasets
    public  static Directory     luceneDirectory;
    public  static IndexWriter   luceneIndexWriter; //is thread-safe

    //made/returned by luceneIndexSearcher
    private static IndexReader   luceneIndexReader; //is thread-safe, but only need/want one
    private static Object        luceneIndexReaderLock = Calendar2.newGCalendarLocal();
    public  static boolean       needNewLuceneIndexReader = true;  
    private static IndexSearcher luceneIndexSearcher; //is thread-safe, so can reuse
    private static String[]      luceneDatasetIDFieldCache;

    //also see updateLucene in LoadDatasets

    public final static int defaultItemsPerPage = 1000; //1000, for /info/index.xxx and search
    public final static String defaultPIppQuery        = "page=1&itemsPerPage=" + defaultItemsPerPage;
    public final static String allPIppQuery            = "page=1&itemsPerPage=1000000000";
    /** The HTML/XML encoded form */
    public final static String encodedDefaultPIppQuery = "page=1&#x26;itemsPerPage=" + defaultItemsPerPage;
    public final static String encodedAllPIppQuery     = "page=1&#x26;itemsPerPage=1000000000";

    /** 
     * These values are loaded from the [contentDirectory]setup.xml file. 
     * See comments in the [contentDirectory]setup.xml file.
     */
    public static String 
        baseUrl,
        baseHttpsUrl, //won't be null
        bigParentDirectory,

        EDDGridIdExample,
        EDDGridDimensionExample,
        EDDGridNoHyperExample,
        EDDGridDimNamesExample,
        EDDGridDataIndexExample,
        EDDGridDataValueExample,
        EDDGridDataTimeExample,
        EDDGridGraphExample,
        EDDGridMapExample,
        EDDGridMatlabPlotExample,

        EDDTableIdExample,
        EDDTableVariablesExample,
        EDDTableConstraintsExample,
        EDDTableDataValueExample,
        EDDTableDataTimeExample,
        EDDTableGraphExample,
        EDDTableMapExample,
        EDDTableMatlabPlotExample,

        adminInstitution,
        adminIndividualName,
        adminPosition,
        adminPhone, 
        adminAddress,
        adminCity,
        adminStateOrProvince, 
        adminPostalCode,
        adminCountry,
        adminEmail,

        accessConstraints,
        accessRequiresAuthorization,
        fees,
        keywords,
        units_standard,
   
        sosFeatureOfInterest,
        sosUrnBase,
        sosBaseGmlName,
        sosStandardNamePrefix,

        wmsSampleDatasetID,
        wmsSampleVariable,
        wmsSampleBBox,
    
        PostSurgeryDatasetID,
        PostDetectionDatasetID,
        PostSubset[],
        PostUserTableName,
        PostRoleTableName,
        PostNameColumnName,
        PostPasswordColumnName,
        PostRoleColumnName,
        PostDatePublicColumnName,
        PostSampleTag,

        authentication,  //will be one of "", "custom", "openid"
        datasetsRegex,
        drawLandMask,
        emailEverythingTo[], 
        emailDailyReportTo[],
        flagKeyKey,
        fontFamily,
        googleEarthLogoFile,
        highResLogoImageFile,
        legendTitle1, 
        legendTitle2, 
        lowResLogoImageFile,
        passwordEncoding, //will be one of "plaintext", "MD5", or "UEPMD5"
        questionMarkImageFile,
        searchEngine,
        warName;
    public static String ampLoginInfo = "&loginInfo;";
    public static int 
        lowResLogoImageFileWidth,  lowResLogoImageFileHeight,
        highResLogoImageFileWidth, highResLogoImageFileHeight,
        googleEarthLogoFileWidth,  googleEarthLogoFileHeight;
    private static String legal, PostIndex1Html, PostIndex2Html, PostIndex3Html;
    private static int   ampLoginInfoPo = -1;
    /** These are special because other loggedInAs must be String2.justPrintable
        loggedInAsLoggingIn is used by login.html so https is used for images, 
        but &amp;loginInfo; indicates user isn't logged in.
     */
    public final static String loggedInAsLoggingIn = "\u0000loggingIn\uffff"; //final so not changeable
    public final static String loggedInAsSuperuser = "\u0000superuser\uffff"; //final so not changeable
    private static String startBodyHtml,  endBodyHtml, startHeadHtml; //see xxx() methods

    public static boolean displayDiagnosticInfo, listPrivateDatasets, 
        reallyVerbose,
        postShortDescriptionActive, //if true, PostIndexHtml is on home page and /post/index.html redirects there        
        subscriptionSystemActive,  convertersActive, slideSorterActive,
        fgdcActive, iso19115Active, geoServicesRestActive, sosActive, wcsActive, wmsActive,
        quickRestart,
        useOriginalSearchEngine, useLuceneSearchEngine,  //exactly one will be true
        variablesMustHaveIoosCategory,
        verbose;
    public static String  categoryAttributes[];       //as it appears in metadata (and used for hashmap)
    public static String  categoryAttributesInURLs[]; //fileNameSafe (as used in URLs)
    public static boolean categoryIsGlobal[];
    public static int variableNameCategoryAttributeIndex = -1;
    public static int 
        unusualActivity = 10000,
        partialRequestMaxBytes = 100000000, 
        partialRequestMaxCells = 100000;
    public static long cacheMillis, loadDatasetsMinMillis, loadDatasetsMaxMillis;
    private static String
        emailSmtpHost, emailUserName, emailFromAddress, emailPassword, emailProperties; 
    private static int emailSmtpPort;
    private static String emailLogDate = "";
    private static BufferedWriter emailLogFile;


    //these are set as a consequence of setup.xml info
    public static SgtGraph sgtGraph;
    public static String 
        erddapUrl,  //without slash at end
        erddapHttpsUrl,  //without slash at end   (may be useless, but won't be null)
        fullDatasetDirectory,  //all the Directory's have slash at end
        fullCacheDirectory,
        fullLogsDirectory,
        fullCopyDirectory,
        fullLuceneDirectory,
        fullResetFlagDirectory,
        fullCptCacheDirectory,         
        fullPlainFileNcCacheDirectory,         
        fullSgtMapTopographyCacheDirectory,
        fullTestCacheDirectory,
        fullWmsCacheDirectory,

        imageDirUrl,
        imageDirHttpsUrl;
        //downloadDirUrl;
    public static Subscriptions subscriptions;


    /** These values are loaded from the [contentDirectory]messages.xml file (if present)
        or .../classes/gov/noaapfel/erddap/util/messages.xml. */
    public static String 
        advancedSearch,
        advancedSearchResults,
        advancedSearchDirections,
        advancedSearchHtml,
        advancedSearchBounds,
        advancedSearchMinLat,
        advancedSearchMaxLat,
        advancedSearchMinLon,
        advancedSearchMaxLon,
        advancedSearchMinMaxLon,
        advancedSearchMinTime,
        advancedSearchMaxTime,
        advancedSearchClear,
        advancedSearchClearHelp,
        advancedSearchCategoryTooltip,
        advancedSearchRangeTooltip,
        advancedSearchMapTooltip,
        advancedSearchLonTooltip,
        advancedSearchTimeTooltip,
        advancedSearchWithCriteria,
        advancedSearchFewerCriteria,
        advancedSearchNoCriteria,
        categoryTitleHtml,
        category1Html,
        category2Html,
        category3Html,
        categoryPickAttribute,
        categorySearchHtml,
        categorySearchDifferentHtml,
        categoryClickHtml,
        categoryNotAnOption,
        clickAccessHtml,
        clickAccess,
        clickBackgroundInfo,
        clickInfo,
        clickToSubmit,
        convertFipsCounty,        
        convertFipsCountyIntro,
        convertFipsCountyNotes,
        convertFipsCountyService,
        convertHtml,
        convertKeywords,          
        convertKeywordsCfTooltip,
        convertKeywordsGcmdTooltip,
        convertKeywordsIntro,
        convertKeywordsNotes,
        convertKeywordsService,
        convertTime,           
        convertTimeReference,
        convertTimeIntro,
        convertTimeNotes,
        convertTimeService,
        convertTimeUnitsHelp,
        convertUnits,             
        convertUnitsComparison,   
        convertUnitsFilter,
        convertUnitsIntro,
        convertUnitsNotes,
        convertUnitsService,
        daf,
        dafGridBypass,
        dafGridHtml,
        dafTableBypass,
        dafTableHtml,
        dasTitle,
        dataAccessNotAllowed,
        disabled,
        distinctValuesHtml,
        doWithGraphs,

        dtAccessible,
        dtAccessibleYes,
        dtAccessibleNo,
        dtAccessibleLogIn,
        dtLogIn,
        dtDAF1,
        dtDAF2,
        dtMAG,
        dtSOS,
        dtSubset,
        dtWCS,
        dtWMS,

        EDDDatasetID,
        EDDFgdc,
        EDDFgdcMetadata,
        EDDIso19115,
        EDDIso19115Metadata,
        EDDMetadata,
        EDDBackground,
        EDDClickOnSubmitHtml,
        EDDInstitution,
        EDDInformation,
        EDDSummary,
        EDDDatasetTitle,
        EDDDownloadData,
        EDDMakeAGraph,
        EDDMakeAMap,
        EDDFileType,
        EDDFileTypeInformation,
        EDDSelectFileType,
        EDDMinimum,
        EDDMaximum,
        EDDConstraint,

        EDDChangedWasnt,
        EDDChangedDifferentNVar,
        EDDChanged2Different,
        EDDChanged1Different,
        EDDChangedCGADifferent,
        EDDChangedAxesDifferentNVar,
        EDDChangedAxes2Different,
        EDDChangedAxes1Different,
        EDDChangedNoValue,
        EDDChangedTableToGrid,

        EDDSimilarDifferentNVar,
        EDDSimilarDifferent,

        EDDGridDapDescription,
        EDDGridDapLongDescription,
        EDDGridDownloadDataHtml,
        EDDGridDimension,
        EDDGridDimensionRanges,
        EDDGridFirst,
        EDDGridLast,
        EDDGridStart,
        EDDGridStop,
        EDDGridStartStopHelp,
        EDDGridStride,
        EDDGridNValues,
        EDDGridNValuesHtml,
        EDDGridSpacing,
        EDDGridJustOneValue,
        EDDGridEven,
        EDDGridUneven,
        EDDGridDimensionHtml,
        EDDGridDimensionFirstHtml,
        EDDGridDimensionLastHtml,
        EDDGridVarHasDimHtml,
        EDDGridSSSHtml,
        EDDGridStartHtml,
        EDDGridStopHtml,
        EDDGridStrideHtml,
        EDDGridSpacingHtml,
        EDDGridDownloadTooltipHtml,
        EDDGridGridVariableHtml,

        EDDTableConstraints,
        EDDTableTabularDatasetHtml,
        EDDTableVariable,
        EDDTableCheckAll,
        EDDTableCheckAllTooltip,
        EDDTableUncheckAll,
        EDDTableUncheckAllTooltip,
        EDDTableMinimumTooltip,
        EDDTableMaximumTooltip,
        EDDTableCheckTheVariables,
        EDDTableSelectAnOperator,
        EDDTableOptConstraint1Html,
        EDDTableOptConstraint2Html,
        EDDTableOptConstraintVar,
        EDDTableNumericConstraintHtml,
        EDDTableStringConstraintHtml,
        EDDTableTimeConstraintHtml,
        EDDTableConstraintHtml,
        EDDTableSelectConstraint,
        EDDTableDapDescription,
        EDDTableDapLongDescription,
        EDDTableDownloadDataHtml,

        errorTitle,
        errorRequestUrl,
        errorRequestQuery,
        errorTheError,
        errorCopyFrom,
        errorFileNotFound,
        errorFileNotFoundImage,
        errorInternal,
        errorJsonpFunctionName,
        errorMoreThan2GB,
        errorNotFound,
        errorNotFoundIn,
        errorOdvLLTGrid,
        errorOdvLLTTable,
        fileHelp_asc,
        fileHelp_csv,
        fileHelp_csvp,
        fileHelp_das,
        fileHelp_dds,
        fileHelp_dods,
        fileHelpGrid_esriAscii,
        fileHelpTable_esriAscii,
        fileHelp_fgdc,
        fileHelp_geoJson,
        fileHelp_graph,
        fileHelpGrid_help,
        fileHelpTable_help,
        fileHelp_html,
        fileHelp_htmlTable,
        fileHelp_iso19115,
        fileHelp_json,
        fileHelp_mat,
        fileHelpGrid_nc,
        fileHelpTable_nc,
        fileHelp_ncHeader,
        fileHelp_ncCF,
        fileHelp_ncCFHeader,
        fileHelp_ncCFMA,
        fileHelp_ncCFMAHeader,
        fileHelpGrid_odvTxt,
        fileHelpTable_odvTxt,
        fileHelp_subset,
        fileHelp_tsv,
        fileHelp_tsvp,
        fileHelp_xhtml,
        fileHelp_geotif,  //graphical
        fileHelpGrid_kml,
        fileHelpTable_kml,
        fileHelp_smallPdf,
        fileHelp_pdf,
        fileHelp_largePdf,
        fileHelp_smallPng,
        fileHelp_png,
        fileHelp_largePng,
        fileHelp_transparentPng,
        functions,
        functionHtml,
        functionDistinctCheck,
        functionDistinctHtml,
        functionOrderByHtml,
        functionOrderBySort,
        functionOrderBySort1,
        functionOrderBySort2,
        functionOrderBySort3,
        functionOrderBySort4,
        functionOrderBySortLeast,
        functionOrderBySortRowMax,
        geoServicesDescription,
        getStartedHtml,
        htmlTableMaxMessage,
        imageDataCourtesyOf,
        indexViewAll,
        indexSearchWith,
        indexDevelopersSearch,
        indexProtocol,
        indexDescription,
        indexDatasets,
        indexDocumentation,
        indexRESTfulSearch,        
        indexServices,
        indexDescribeServices,
        indexMetadata,
        indexWAF1,
        indexWAF2,
        indexConverters,
        indexDescribeConverters,
        infoAboutFrom,
        infoTableTitleHtml,
        infoRequestForm,
        justGenerateAndView,
        justGenerateAndViewHtml,
        justGenerateAndViewUrl,
        justGenerateAndViewGraphUrlHtml,
        license,
        listAll,
        listOfDatasets,
        LogIn,
        login,
        loginAttemptBlocked,
        loginDescribeCustom,
        loginDescribeOpenID,
        loginCanNot,
        loginWereNot,
        loginPleaseLogIn,
        loginUserName,
        loginPassword,
        loginUserNameAndPassword,
        loginOpenID,
        loginOpenIDOr,
        loginOpenIDCreate,
        loginOpenIDFree,
        loginOpenIDSame,
        loginAs,
        loginFailed,
        loginInvalid,
        loginNot,
        loginBack,
        loginProblems,
        loginProblemsAfter,
        loginPublicAccess,
        LogOut,
        logout,
        logoutOpenID,
        logoutSuccess,
        mag,
        magAxisX,
        magAxisY,
        magAxisColor,
        magAxisStickX,
        magAxisStickY,
        magAxisVectorX,
        magAxisVectorY,
        magAxisHelpGraphX,
        magAxisHelpGraphY,
        magAxisHelpMarkerColor,
        magAxisHelpSurfaceColor,
        magAxisHelpStickX,
        magAxisHelpStickY,
        magAxisHelpMapX,
        magAxisHelpMapY,
        magAxisHelpVectorX,
        magAxisHelpVectorY,
        magAxisVarHelp,
        magAxisVarHelpGrid,
        magConstraintHelp,
        magDocumentation,
        magDownload,
        magDownloadTooltip,
        magFileType,
        magGraphType,
        magGraphTypeTooltipGrid,
        magGraphTypeTooltipTable,
        magGS,
        magGSMarkerType,
        magGSSize,
        magGSColor,
        magGSColorBar,
        magGSColorBarTooltip,
        magGSContinuity,
        magGSContinuityTooltip,
        magGSScale,
        magGSScaleTooltip,
        magGSMin,
        magGSMinTooltip,
        magGSMax,
        magGSMaxTooltip,
        magGSNSections,
        magGSNSectionsTooltip,
        magGSLandMask,
        magGSLandMaskTooltipGrid,
        magGSLandMaskTooltipTable,
        magGSVectorStandard,
        magGSVectorStandardTooltip,
        magGSYAxisMin,
        magGSYAxisMax,
        magGSYRangeMinTooltip,
        magGSYRangeMaxTooltip,
        magGSYRangeTooltip,
        magItemFirst,
        magItemPrevious,
        magItemNext,
        magItemLast,
        magJust1Value,
        magRange,
        magRangeTo,
        magRedraw,
        magRedrawTooltip,
        magTimeRange,
        magTimeRangeFirst,
        magTimeRangeBack,
        magTimeRangeForward,
        magTimeRangeLast,
        magTimeRangeTooltip,
        magTimeRangeTooltip2,
        magTimesVary,
        magViewUrl,
        magZoom,
        magZoomCenter,
        magZoomCenterTooltip,
        magZoomIn,
        magZoomInTooltip,
        magZoomOut,
        magZoomOutTooltip,
        magZoomALittle,
        magZoomData,
        magZoomOutData,
        magGridHtml,
        magTableHtml,        
        metadataDownload,
        moreInformation,
        nMatching1,
        nMatchingAlphabetical,
        nMatchingMostRelevant,
        nMatchingPage,
        nMatchingCurrent,
        noDataFixedValue,
        noDataNoLL,
        noDatasetWith,
        noPage1,
        noPage2,
        notAuthorized,
        notAvailable,
        noXxxBecause,
        noXxxBecause2,
        noXxxNotActive,
        noXxxNoAxis1,
        noXxxNoColorBar,
        noXxxNoCdmDataType,
        noXxxNoLL,
        noXxxNoLLEvenlySpaced,
        noXxxNoLLGt1,
        noXxxNoLLT,
        noXxxNoLonIn180,
        noXxxNoNonString,
        noXxxNo2NonString,
        noXxxNoStation,
        noXxxNoStationID,
        noXxxNoMinMax,
        noXxxItsGridded,
        noXxxItsTabular,
        optional,
        orRefineSearchWith,
        orSearchWith,
        orComma,
        palettes[],
        palettes0[],
        paletteSections[] = {
            "","2","3","4","5","6","7","8","9",
            "10","11","12","13","14","15","16","17","18","19",
            "20","21","22","23","24","25","26","27","28","29",
            "30","31","32","33","34","35","36","37","38","39", "40"},
        patientData,
        patientYourGraph,
        pickADataset,
        protocolSearchHtml,
        protocolSearch2Html,
        protocolClick,

        queryError,
        queryError180,
        queryError1Value,
        queryError1Var,
        queryError2Var,
        queryErrorAdjusted,
        queryErrorAscending,
        queryErrorConstraintNaN,
        queryErrorEqualSpacing,
        queryErrorExpectedAt,
        queryErrorFileType,
        queryErrorInvalid,
        queryErrorLL,
        queryErrorLLGt1,
        queryErrorLLT,
        queryErrorNeverTrue,
        queryErrorNeverBothTrue,
        queryErrorNotAxis,
        queryErrorNotExpectedAt,
        queryErrorNotFoundAfter,
        queryErrorOccursTwice,
        queryErrorUnknownVariable,

        queryErrorGrid1Axis,
        queryErrorGridAmp,
        queryErrorGridDiagnostic,
        queryErrorGridBetween,
        queryErrorGridLessMin,
        queryErrorGridGreaterMax,
        queryErrorGridMissing,
        queryErrorGridNoAxisVar,
        queryErrorGridNoDataVar,
        queryErrorGridNotIdentical,
        queryErrorGridSLessS,
        queryErrorLastEndP,
        queryErrorLastExpected,
        queryErrorLastUnexpected,
        queryErrorLastPMInvalid,
        queryErrorLastPMInteger,        
        rangesFromTo,
        resetTheForm,
        resetTheFormWas,
        resourceNotFound,
        requestFormatExamplesHtml,
        resultsFormatExamplesHtml,
        resultsOfSearchFor,
        restfulInformationFormats,
        restfulViaService,
        rows,
        searchTitle,
        searchDoFullTextHtml,
        searchFullTextHtml,
        searchHintsHtml, 
        searchHintsLuceneHtml, 
        searchHintsOriginalHtml, 
        searchButton,
        searchClickTip,
        searchNotAvailable,
        searchTip,
        searchSpelling,
        searchFewerWords,
        searchWithQuery,
        seeProtocolDocumentation,
        selectNext,
        selectPrevious,
        ssUse,
        ssBePatient, 
        ssInstructionsHtml,
        standardShortDescriptionHtml,
        standardLicense,
        standardContact,
        standardDataLicenses,
        standardDisclaimerOfEndorsement,
        standardDisclaimerOfExternalLinks,
        standardGeneralDisclaimer,
        standardPrivacyPolicy,
        submit,
        submitTooltip, 
        subscriptionsTitle,
        subscriptionOptions,
        subscriptionAdd,
        subscriptionAddHtml,
        subscriptionValidate,
        subscriptionValidateHtml,
        subscriptionList,
        subscriptionListHtml,
        subscriptionRemove,
        subscriptionRemoveHtml,
        subscriptionAbuse,
        subscriptionAddError,
        subscriptionAdd2,
        subscriptionAddSuccess,
        subscriptionEmail,
        subscriptionEmailInvalid,
        subscriptionEmailTooLong,
        subscriptionEmailUnspecified,
        subscriptionHtml, 
        subscription2Html, 
        subscriptionIDInvalid,
        subscriptionIDTooLong,
        subscriptionIDUnspecified,
        subscriptionKeyInvalid,
        subscriptionKeyUnspecified,
        subscriptionListError,
        subscriptionListSuccess,
        subscriptionRemoveError,
        subscriptionRemove2,
        subscriptionRemoveSuccess,
        subscriptionRSS,
        subscriptionsNotAvailable,
        subscriptionUrlHtml,
        subscriptionUrlInvalid,
        subscriptionUrlTooLong,        
        subscriptionValidateError,
        subscriptionValidateSuccess,
        subset,
        subsetSelect,
        subsetNMatching,
        subsetInstructions,
        subsetOption,
        subsetOptions,
        subsetRefineMapDownload,
        subsetRefineSubsetDownload,
        subsetClickResetClosest,
        subsetClickResetLL,
        subsetMetadata,
        subsetCount,
        subsetPercent,
        subsetViewSelect,
        subsetViewSelectDistinctCombos,
        subsetViewSelectRelatedCounts,
        subsetWhen,
        subsetWhenNoConstraints,
        subsetWhenCounts,
        subsetComboClickSelect,
        subsetNVariableCombos,
        subsetShowingAllRows,
        subsetShowingNRows,
        subsetChangeShowing,
        subsetNRowsRelatedData,
        subsetViewRelatedChange,
        subsetTotalCount,
        subsetView,
        subsetViewCheck,
        subsetViewCheck1,
        subsetViewDistinctMap,
        subsetViewRelatedMap,
        subsetViewDistinctDataCounts,
        subsetViewDistinctData,
        subsetViewRelatedDataCounts,
        subsetViewRelatedData,
        subsetViewDistinctMapTooltip,
        subsetViewRelatedMapTooltip,
        subsetViewDistinctDataCountsTooltip,
        subsetViewDistinctDataTooltip,
        subsetViewRelatedDataCountsTooltip,
        subsetViewRelatedDataTooltip,
        subsetWarn,                    
        subsetWarn10000,
        subsetTooltip,
        subsetNotSetUp,
        subsetLongNotShown,

        unknownDatasetID,
        unknownProtocol,
        unsupportedFileType,
        viewAllDatasetsHtml,
        waitThenTryAgain,

        sosDescriptionHtml,
        sosLongDescriptionHtml,
        wcsDescriptionHtml,
        wcsLongDescriptionHtml,
        wmsDescriptionHtml,
        wmsInstructions,
        wmsLongDescriptionHtml,
        wmsManyDatasets;

    public static int[] imageWidths, imageHeights, pdfWidths, pdfHeights;
    private static String        
        theShortDescriptionHtml, theLongDescriptionHtml; //see the xxx() methods
    public static String errorFromDataSource = String2.ERROR + " from data source: ";


    /** This static block reads this class's static String values from
     * contentDirectory, which must contain setup.xml and datasets.xml (and may contain messages.xml).
     * It may be a defined environment variable (erddapContent)
     * or a subdir of <tomcat> (e.g., usr/local/tomcat/content/erddap/)
     *   (more specifically, a sibling of 'tomcat'/webapps).
     *
     * @throws RuntimeException if trouble
     */
    static {

    String errorInMethod = "";
    try {

        //*** Set up logging systems.
        //route calls to a logger to com.cohort.util.String2Log
        String2.setupCommonsLogging(-1);

        //**** find contentDirectory
        errorInMethod = 
            "Couldn't find 'content' directory (<tomcat>/content/erddap/) " +
            "because 'erddapContent' environment variable not found " +
            "and couldn't find '/webapps/' in classPath=" + String2.getClassPath() +
            " (and 'content/erddap' should be a sibling of <tomcat>/webapps).";
        contentDirectory = System.getProperty("erddapContentDirectory");        
        if (contentDirectory == null) {
            //Or, it must be sibling of webapps
            //e.g., c:/programs/tomcat/webapps/erddap/WEB-INF/classes/[these classes]
            //On windows, contentDirectory may have spaces as %20(!)
            contentDirectory = String2.replaceAll(String2.getClassPath(), "%20", " "); 
            int po = contentDirectory.indexOf("/webapps/");
            contentDirectory = contentDirectory.substring(0, po) + "/content/erddap/"; //exception if po=-1
        } else {
            contentDirectory = File2.addSlash(contentDirectory);
        }
        Test.ensureTrue(File2.isDirectory(contentDirectory),  
            "contentDirectory (" + contentDirectory + ") doesn't exist.");


        //**** setup.xml *************************************************************
        //read static Strings from setup.xml 
        String setupFileName = contentDirectory + 
            "setup" + (developmentMode? "2" : "") + ".xml";
        errorInMethod = "EDStatic error while reading " + setupFileName;
        ResourceBundle2 setup = ResourceBundle2.fromXml(XML.parseXml(setupFileName, false));

        //logLevel may be: warning, info(default), all
        String logLevel = setup.getString("logLevel", "info").toLowerCase();  
        verbose = !logLevel.equals("warning");
        AxisDataAccessor.verbose = verbose;
        Boundaries.verbose = verbose;
        Calendar2.verbose = verbose;
        EDD.verbose = verbose;
        EDV.verbose = verbose;      
        Erddap.verbose = verbose;
        FilledMarkerRenderer.verbose = verbose;
        GridDataAccessor.verbose = verbose;
        GSHHS.verbose = verbose;
        LoadDatasets.verbose = verbose;
        NcHelper.verbose = verbose;
        OutputStreamFromHttpResponse.verbose = verbose;
        PathCartesianRenderer.verbose = verbose;
        Projects.verbose = verbose;
        //ResourceBundle2.verbose = verbose;
        RunLoadDatasets.verbose = verbose;
        SgtGraph.verbose = verbose;
        SgtMap.verbose = verbose;
        SgtUtil.verbose = verbose;
        SSR.verbose = verbose;
        Subscriptions.verbose = verbose;
        Table.verbose = verbose;
        TaskThread.verbose = verbose;

        reallyVerbose = logLevel.equals("all");
        AxisDataAccessor.reallyVerbose = reallyVerbose;
        Boundaries.reallyVerbose = reallyVerbose;
        Calendar2.reallyVerbose = reallyVerbose;
        EDD.reallyVerbose = reallyVerbose;
        EDV.reallyVerbose = reallyVerbose;
        FilledMarkerRenderer.reallyVerbose = reallyVerbose;
        GridDataAccessor.reallyVerbose = reallyVerbose;
        GSHHS.reallyVerbose = reallyVerbose;
        NcHelper.reallyVerbose = reallyVerbose;
        PathCartesianRenderer.reallyVerbose = reallyVerbose;
        SgtGraph.reallyVerbose = reallyVerbose;
        SgtMap.reallyVerbose = reallyVerbose;
        SgtUtil.reallyVerbose = reallyVerbose;
        SSR.reallyVerbose = reallyVerbose;
        Subscriptions.reallyVerbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;
        TaskThread.reallyVerbose = reallyVerbose;

        bigParentDirectory = setup.getNotNothingString("bigParentDirectory", ""); 
        File2.addSlash(bigParentDirectory);
        Test.ensureTrue(File2.isDirectory(bigParentDirectory),  
            "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");


        //email  (do early on so email can be sent if trouble later in this method)
        emailSmtpHost      = setup.getString("emailSmtpHost",  null);
        emailSmtpPort      = setup.getInt(   "emailSmtpPort",  25);
        emailUserName      = setup.getString("emailUserName",  null);
        emailPassword      = setup.getString("emailPassword",  null);
        emailProperties    = setup.getString("emailProperties",  null);
        emailFromAddress   = setup.getString("emailFromAddress", null);
        String ts          = setup.getString("emailEverythingTo", null); 
        emailEverythingTo  = (ts == null || ts.length() == 0)? null : String2.split(ts, ',');
        ts                 = setup.getString("emailDailyReportTo", null);
        emailDailyReportTo = (ts == null || ts.length() == 0)? null : String2.split(ts, ',');

        //test of email
        //Test.error("This is a test of emailing an error in Erddap constructor.");

        //*** set up directories  //all with slashes at end
        //before 2011-12-30, was fullDatasetInfoDirectory datasetInfo/; see conversion below
        fullDatasetDirectory     = bigParentDirectory + "dataset/";  
        fullCacheDirectory       = bigParentDirectory + "cache/";
        fullResetFlagDirectory   = bigParentDirectory + "flag/";
        fullLogsDirectory        = bigParentDirectory + "logs/";
        fullCopyDirectory        = bigParentDirectory + "copy/";
        fullLuceneDirectory      = bigParentDirectory + "lucene/";

        Test.ensureTrue(File2.isDirectory(fullPaletteDirectory),  
            "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
        Test.ensureTrue(File2.isDirectory(fullPublicDirectory),  
            "fullPublicDirectory (" + fullPublicDirectory + ") doesn't exist.");
        errorInMethod = "EDStatic error while creating directories.";
        File2.makeDirectory(fullPublicDirectory);  //make it, because Git doesn't track empty dirs
        File2.makeDirectory(fullDatasetDirectory);
        File2.makeDirectory(fullCacheDirectory);
        File2.makeDirectory(fullResetFlagDirectory);
        File2.makeDirectory(fullLogsDirectory);
        File2.makeDirectory(fullCopyDirectory);
        File2.makeDirectory(fullLuceneDirectory);

        //set up log (after fullLogsDirectory is known)
        errorInMethod = "EDStatic error while setting up log files.";
        String timeStamp = String2.replaceAll(Calendar2.getCurrentISODateTimeStringLocal(), ":", ".");
        try {
            //rename log.txt to preserve it so it can be analyzed if there was trouble before restart
            if (File2.isFile(bigParentDirectory + "log.txt")) {
                //pre ERDDAP version 1.15
                File2.copy(  bigParentDirectory + "log.txt", 
                              fullLogsDirectory + "logArchivedAt" + timeStamp + ".txt");
                File2.delete(bigParentDirectory + "log.txt");
            } 
            if (File2.isFile(fullLogsDirectory + "log.txt"))
                File2.rename(fullLogsDirectory + "log.txt", 
                             fullLogsDirectory + "logArchivedAt" + timeStamp + ".txt");
        } catch (Throwable t) {
            String2.log("WARNING: " + MustBe.throwableToString(t));
        }
        try {
            //rename log.txt.previous to preserve it so it can be analyzed if there was trouble before restart
            if (File2.isFile(bigParentDirectory + "log.txt.previous")) {
                //pre ERDDAP version 1.15
                File2.copy(  bigParentDirectory + "log.txt.previous", 
                              fullLogsDirectory + "logPreviousArchivedAt" + timeStamp + ".txt");
                File2.delete(bigParentDirectory + "log.txt.previous");
            }
            if (File2.isFile(fullLogsDirectory + "log.txt.previous"))
                File2.rename(fullLogsDirectory + "log.txt.previous", 
                             fullLogsDirectory + "logPreviousArchivedAt" + timeStamp + ".txt");
        } catch (Throwable t) {
            String2.log("WARNING: " + MustBe.throwableToString(t));
        }
        //open String2 log system
        String2.setupLog(false, false, 
            fullLogsDirectory + "log.txt",
            true, true, 20000000);
        try {
            //copy (not rename!) subscriptionsV1.txt to preserve it 
            if (File2.isFile(bigParentDirectory + "subscriptionsV1.txt"))
                File2.copy(  bigParentDirectory + "subscriptionsV1.txt", 
                             bigParentDirectory + "subscriptionsV1ArchivedAt" + timeStamp + ".txt");
        } catch (Throwable t) {
            String2.log("WARNING: " + MustBe.throwableToString(t));
        }

        //after log is setup
        Math2.gc(200); 
        String2.log("\n////**** ERDDAP/EDStatic initialization. localTime=" + 
            Calendar2.getCurrentISODateTimeStringLocal() + "\n" +
            String2.standardHelpAboutMessage() + "\n" +
            Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n" +
            "logLevel=" + logLevel + ": verbose=" + verbose + " reallyVerbose=" + reallyVerbose + "\n");

        String2.log(
            "bigParentDirectory=" + bigParentDirectory +
            "\ncontextDirectory=" + contextDirectory +
            "\nfullPaletteDirectory=" + fullPaletteDirectory +
            "\nfullPublicDirectory=" + fullPublicDirectory +
            "\nfullCacheDirectory=" + fullCacheDirectory +
            "\nfullResetFlagDirectory=" + fullResetFlagDirectory);

        //are bufferedImages hardware accelerated?
        String2.log(SgtUtil.isBufferedImageAccelerated());

        //get rid of old "private" directory (as of 1.14, ERDDAP uses fullCacheDirectory instead)
        //remove this code 2014?
        File2.deleteAllFiles(bigParentDirectory + "private", true, true); //empty it
        File2.delete(        bigParentDirectory + "private"); //delete it

        //2011-12-30 convert /datasetInfo/[datasetID]/ to 
        //                   /dataset/[last2char]/[datasetID]/
        //to prepare for huge number of datasets
        String oldBaseDir = bigParentDirectory + "datasetInfo/";   //the old name
        if (File2.isDirectory(oldBaseDir)) {
            try { 
                String2.log("[[converting datasetInfo/ to dataset/");
                String oldBaseDirList[] = (new File(oldBaseDir)).list();
                int oldBaseDirListSize = oldBaseDirList == null? 0 : oldBaseDirList.length;
                for (int od = 0; od < oldBaseDirListSize; od++) {
                    String odName = oldBaseDirList[od];
                    if (File2.isFile(oldBaseDir + odName)) {
                        //delete obsolete files
                        File2.delete(oldBaseDir + odName); 
                        continue;
                    }
                    if (!File2.isDirectory(oldBaseDir + odName)) {
                        //link??
                        continue;
                    }
                    String fullNdName = EDD.datasetDir(odName);
                    File2.makeDirectory(fullNdName);
                    String oldFileList[] = (new File(oldBaseDir + odName)).list();
                    int oldFileListSize = oldFileList == null? 0 : oldFileList.length;
                    for (int of = 0; of < oldFileListSize; of++) {
                        String ofName = oldFileList[of];
                        String fullOfName = oldBaseDir + odName + "/" + ofName;
                        if (!ofName.matches(".*[0-9]{7}")) //skip temp files
                            File2.copy(fullOfName, fullNdName + ofName); //dir will be created
                        File2.delete(fullOfName);
                    }
                    File2.deleteAllFiles(oldBaseDir + odName);  //should be already empty
                }
                File2.deleteAllFiles(oldBaseDir, true, true);  //and delete empty subdir
                File2.delete(oldBaseDir);  //hopefully empty
                String2.log("]]datasetInfo/ was successfully converted to dataset/");

            } catch (Throwable t) {
                String2.log("WARNING: " + MustBe.throwableToString(t));
            }
        }


        //deal with cache
        //how many millis should files be left in the cache (if untouched)?
        cacheMillis = setup.getInt("cacheMinutes", 60) * 60000L; // millis/min
        displayDiagnosticInfo = setup.getBoolean("displayDiagnosticInfo", false);

        //on start up, always delete all files from fullPublicDirectory and fullCacheDirectory
        File2.deleteAllFiles(fullPublicDirectory,      true, false);  //recursive, deleteEmptySubdirectories 
        File2.deleteAllFiles(fullCacheDirectory,       true, true);   

        //make some subdirectories of fullCacheDirectory
        //'_' distinguishes from dataset cache dirs
        errorInMethod = "EDStatic error while creating directories.";
        fullCptCacheDirectory              = fullCacheDirectory + "_cpt/";   
        fullPlainFileNcCacheDirectory      = fullCacheDirectory + "_plainFileNc/";   
        fullSgtMapTopographyCacheDirectory = fullCacheDirectory + "_SgtMapTopography/";
        fullTestCacheDirectory             = fullCacheDirectory + "_test/";
        fullWmsCacheDirectory              = fullCacheDirectory + "_wms/"; //for all-datasets WMS and subdirs for non-data layers
        File2.makeDirectory(fullCptCacheDirectory);
        File2.makeDirectory(fullPlainFileNcCacheDirectory);
        File2.makeDirectory(fullSgtMapTopographyCacheDirectory);       
        File2.makeDirectory(fullTestCacheDirectory);
        File2.makeDirectory(fullWmsCacheDirectory);
        File2.makeDirectory(fullWmsCacheDirectory + "Land");  //includes LandMask
        File2.makeDirectory(fullWmsCacheDirectory + "Coastlines");
        File2.makeDirectory(fullWmsCacheDirectory + "LakesAndRivers");
        File2.makeDirectory(fullWmsCacheDirectory + "Nations");
        File2.makeDirectory(fullWmsCacheDirectory + "States");

        //2009-11-01 delete old dir name for topography; but someday remove this (only needed once)
        File2.deleteAllFiles(fullCacheDirectory + "_SgtMapBathymetry", false, true);
        File2.delete(        fullCacheDirectory + "_SgtMapBathymetry"); 
        

        //get other info from setup.xml
        errorInMethod = "EDStatic error while reading " + setupFileName;
        baseUrl                    = setup.getNotNothingString("baseUrl",                    "");
        baseHttpsUrl               = setup.getString(          "baseHttpsUrl",               "(not specified)");
        categoryAttributes         = String2.split(setup.getNotNothingString("categoryAttributes", ""), ',');
        int nCat = categoryAttributes.length;
        categoryAttributesInURLs = new String[nCat];
        categoryIsGlobal = new boolean[nCat]; //initially all false
        for (int cati = 0; cati < nCat; cati++) {
            String cat = categoryAttributes[cati];
            if (cat.startsWith("global:")) {
                categoryIsGlobal[cati] = true;
                cat = cat.substring(7);
                categoryAttributes[cati] = cat;
            } else if (cat.equals("institution")) { //legacy special case
                categoryIsGlobal[cati] = true;
            }
            categoryAttributesInURLs[cati] = String2.modifyToBeFileNameSafe(cat);
        }
        variableNameCategoryAttributeIndex =
            String2.indexOf(categoryAttributes, "variableName");

        EDDGridIdExample           = setup.getNotNothingString("EDDGridIdExample",           "");
        EDDGridDimensionExample    = setup.getNotNothingString("EDDGridDimensionExample",    "");
        EDDGridNoHyperExample      = setup.getNotNothingString("EDDGridNoHyperExample",      "");
        EDDGridDimNamesExample     = setup.getNotNothingString("EDDGridDimNamesExample",     "");
        EDDGridDataIndexExample    = setup.getNotNothingString("EDDGridDataIndexExample",    "");
        EDDGridDataValueExample    = setup.getNotNothingString("EDDGridDataValueExample",    "");
        EDDGridDataTimeExample     = setup.getNotNothingString("EDDGridDataTimeExample",     "");
        EDDGridGraphExample        = setup.getNotNothingString("EDDGridGraphExample",        "");
        EDDGridMapExample          = setup.getNotNothingString("EDDGridMapExample",          "");
        EDDGridMatlabPlotExample   = setup.getNotNothingString("EDDGridMatlabPlotExample",   "");

        EDDTableIdExample          = setup.getNotNothingString("EDDTableIdExample",          "");
        EDDTableVariablesExample   = setup.getNotNothingString("EDDTableVariablesExample",   "");
        EDDTableConstraintsExample = setup.getNotNothingString("EDDTableConstraintsExample", "");
        EDDTableDataValueExample   = setup.getNotNothingString("EDDTableDataValueExample",   "");
        EDDTableDataTimeExample    = setup.getNotNothingString("EDDTableDataTimeExample",    "");
        EDDTableGraphExample       = setup.getNotNothingString("EDDTableGraphExample",       "");
        EDDTableMapExample         = setup.getNotNothingString("EDDTableMapExample",         "");
        EDDTableMatlabPlotExample  = setup.getNotNothingString("EDDTableMatlabPlotExample",  "");

        adminInstitution           = setup.getNotNothingString("adminInstitution",           "");
        adminIndividualName        = setup.getNotNothingString("adminIndividualName",        "");
        adminPosition              = setup.getNotNothingString("adminPosition",              "");
        adminPhone                 = setup.getNotNothingString("adminPhone",                 ""); 
        adminAddress               = setup.getNotNothingString("adminAddress",               "");
        adminCity                  = setup.getNotNothingString("adminCity",                  "");
        adminStateOrProvince       = setup.getNotNothingString("adminStateOrProvince",       ""); 
        adminPostalCode            = setup.getNotNothingString("adminPostalCode",            "");
        adminCountry               = setup.getNotNothingString("adminCountry",               "");
        adminEmail                 = setup.getNotNothingString("adminEmail",                 "");

        accessConstraints          = setup.getNotNothingString("accessConstraints",          ""); 
        accessRequiresAuthorization= setup.getNotNothingString("accessRequiresAuthorization",""); 
        fees                       = setup.getNotNothingString("fees",                       "");
        keywords                   = setup.getNotNothingString("keywords",                   "");
        legal                      = setup.getNotNothingString("legal",                      "");
        units_standard             = setup.getString(          "units_standard",             "UDUNITS");

        fgdcActive                 = setup.getBoolean(         "fgdcActive",                 true); 
        iso19115Active             = setup.getBoolean(         "iso19115Active",             true); 
//until it is finished, it is always inactive
geoServicesRestActive      = false; //setup.getBoolean(         "geoServicesRestActive",      false); 
        sosActive                  = setup.getBoolean(         "sosActive",                  false); 
        if (sosActive) {
            sosFeatureOfInterest   = setup.getNotNothingString("sosFeatureOfInterest",       "");
            sosStandardNamePrefix  = setup.getNotNothingString("sosStandardNamePrefix",      "");
            sosUrnBase             = setup.getNotNothingString("sosUrnBase",                 "");

            //make the sosGmlName, e.g., http://coastwatch.pfeg.noaa.gov:8080 -> gov.noaa.pfeg.coastwatch
            sosBaseGmlName = baseUrl; 
            int po = sosBaseGmlName.indexOf("//");
            if (po > 0)
                sosBaseGmlName = sosBaseGmlName.substring(po + 2);
            po = sosBaseGmlName.indexOf(":");
            if (po > 0)
                sosBaseGmlName = sosBaseGmlName.substring(0, po); 
            StringArray sbgn = new StringArray(String2.split(sosBaseGmlName, '.'));
            sbgn.reverse();
            sosBaseGmlName = String2.toSVString(sbgn.toArray(), ".", false);     
        }

//until it is finished, it is always inactive
wcsActive                  = false; //setup.getBoolean(         "wcsActive",                  false); 

        wmsActive                  = setup.getBoolean(         "wmsActive",                  true); 
        wmsSampleDatasetID         = setup.getNotNothingString("wmsSampleDatasetID",         "");
        wmsSampleVariable          = setup.getNotNothingString("wmsSampleVariable",          "");
        wmsSampleBBox              = setup.getNotNothingString("wmsSampleBBox",              "");

        PostIndex1Html             = setup.getString(          "PostIndex1Html", "[PostIndex1Html in setup.xml]");
        PostIndex2Html             = setup.getString(          "PostIndex2Html", "[PostIndex2Html in setup.xml]");
        PostIndex3Html             = setup.getString(          "PostIndex3Html", "[PostIndex3Html in setup.xml]");

        //EDDTableFromPost checks that these are non-null, non-""
        PostSurgeryDatasetID       = setup.getString(          "PostSurgeryDatasetID",       "");
        PostDetectionDatasetID     = setup.getString(          "PostDetectionDatasetID",     "");
        PostSubset   = String2.split(setup.getString(          "PostSubset",                 ""), ',');
        PostUserTableName          = setup.getString(          "PostUserTableName",          "");
        PostRoleTableName          = setup.getString(          "PostRoleTableName",          "");
        PostNameColumnName         = setup.getString(          "PostNameColumnName",         "");
        PostPasswordColumnName     = setup.getString(          "PostPasswordColumnName",     "");
        PostRoleColumnName         = setup.getString(          "PostRoleColumnName",         "");
        PostDatePublicColumnName   = setup.getString(          "PostDatePublicColumnName",   "");
        PostSampleTag              = setup.getString(          "PostSampleTag",              "");


        authentication             = setup.getString(          "authentication",             "");
        datasetsRegex              = setup.getString(          "datasetsRegex",              ".*");
        drawLandMask               = setup.getString(          "drawLandMask",               null);
        if (drawLandMask == null) 
            drawLandMask           = setup.getString(          "drawLand",                   "over"); //legacy
        if (!drawLandMask.equals("under") && 
            !drawLandMask.equals("over"))
             drawLandMask = "over"; //default
        endBodyHtml                = setup.getNotNothingString("endBodyHtml",                "");
        endBodyHtml                = String2.replaceAll(endBodyHtml, "&erddapVersion;", erddapVersion);
        flagKeyKey                 = setup.getString(          "flagKeyKey",                 "");
        if (flagKeyKey == null || flagKeyKey.length() == 0)  flagKeyKey = "flagKeyKey";
        fontFamily                 = setup.getString(          "fontFamily",                 "SansSerif");
        googleEarthLogoFile        = setup.getNotNothingString("googleEarthLogoFile",        "");
        highResLogoImageFile       = setup.getNotNothingString("highResLogoImageFile",       "");
        legendTitle1               = setup.getString(          "legendTitle1",               null);
        legendTitle2               = setup.getString(          "legendTitle2",               null);
        listPrivateDatasets        = setup.getBoolean(         "listPrivateDatasets",        false);
        loadDatasetsMinMillis      = Math.max(1,setup.getInt(  "loadDatasetsMinMinutes",     15)) * 60000L;
        loadDatasetsMaxMillis      = setup.getInt(             "loadDatasetsMaxMinutes",     60) * 60000L;
        loadDatasetsMaxMillis      = Math.max(loadDatasetsMinMillis * 2, loadDatasetsMaxMillis);
        lowResLogoImageFile        = setup.getNotNothingString("lowResLogoImageFile",        "");
        partialRequestMaxBytes     = setup.getInt(             "partialRequestMaxBytes",     partialRequestMaxBytes);
        partialRequestMaxCells     = setup.getInt(             "partialRequestMaxCells",     partialRequestMaxCells);
        questionMarkImageFile      = setup.getNotNothingString("questionMarkImageFile",      "");
        quickRestart               = setup.getBoolean(         "quickRestart",               true);
        passwordEncoding           = setup.getString(          "passwordEncoding",           "UEPMD5");
        searchEngine               = setup.getString(          "searchEngine",               "original");
        if (searchEngine.equals("lucene")) {
            useLuceneSearchEngine = true;
            //ERDDAP consciously doesn't use any stopWords
            //1) this matches the behaviour of the original searchEngine
            //2) this is what users expect, e.g., when searching for a phrase
            //3) the content here isn't prose, so the stop words aren't nearly as common
            HashSet stopWords = new HashSet();
            luceneAnalyzer = new StandardAnalyzer(luceneVersion, stopWords);
            //it is important that the queryParser use the same analyzer as the indexWriter
            luceneQueryParser = new QueryParser(luceneVersion, luceneDefaultField, luceneAnalyzer);
        } else {
            Test.ensureEqual(searchEngine, "original", 
                "<searchEngine> must be \"original\" (the default) or \"lucene\".");
            useOriginalSearchEngine = true;
        }
        startBodyHtml              = setup.getNotNothingString("startBodyHtml",              "");
        startHeadHtml              = setup.getNotNothingString("startHeadHtml",              "");
        subscriptionSystemActive   = setup.getBoolean(         "subscriptionSystemActive",   true);
        convertersActive           = setup.getBoolean(         "convertersActive",           true);
        slideSorterActive          = setup.getBoolean(         "slideSorterActive",          true);
        theShortDescriptionHtml    = setup.getNotNothingString("theShortDescriptionHtml",    "");
        unusualActivity            = setup.getInt(             "unusualActivity",            unusualActivity);
        variablesMustHaveIoosCategory = setup.getBoolean(      "variablesMustHaveIoosCategory", true);
        warName                    = setup.getNotNothingString("warName",                    "");
 
        //copy all <contentDirectory>images/ (and subdirectories) files to imageDir (and subdirectories)
        errorInMethod = "EDStatic error while copying " + contentDirectory + "images/ .";
        String imageFiles[] = RegexFilenameFilter.recursiveFullNameList(
            contentDirectory + "images/", ".+", false);
        for (int i = 0; i < imageFiles.length; i++) {
            int tpo = imageFiles[i].indexOf("/images/");
            if (tpo < 0) tpo = imageFiles[i].indexOf("\\images\\");
            if (tpo < 0) {
                String2.log("'/images/' not found in images/ file: " + imageFiles[i]);
                continue;
            }
            String tName = imageFiles[i].substring(tpo + 8);
            if (verbose) String2.log("  copying images/ file: " + tName);
            File2.copy(contentDirectory + "images/" + tName,  imageDir + tName);
        }

        errorInMethod = "EDStatic error while initializing SgtGraph.";
        sgtGraph = new SgtGraph(fontFamily);

        //ensure authentication setup is okay
        errorInMethod = "EDStatic error while checking authentication setup.";
        if (authentication == null)
            authentication = "";
        authentication = authentication.trim().toLowerCase();
        if (!authentication.equals("") &&
            //!authentication.equals("basic") &&
            !authentication.equals("custom") &&
            !authentication.equals("openid"))
            throw new RuntimeException("setup.xml error: authentication=" + authentication + 
                " must be (nothing)|custom|openid.");  //"basic"
        if (!authentication.equals("") && !baseHttpsUrl.startsWith("https://"))
            throw new RuntimeException("setup.xml error: " + 
                ": For any <authentication> other than \"\", the baseHttpsUrl=" + baseHttpsUrl + 
                " must start with \"https://\".");
        if (!passwordEncoding.equals("plaintext") &&
            !passwordEncoding.equals("MD5") &&
            !passwordEncoding.equals("UEPMD5"))
            throw new RuntimeException("setup.xml error: passwordEncoding=" + passwordEncoding + 
                " must be plaintext|MD5|UEPMD5.");  
        //String2.log("authentication=" + authentication);


        //things set as a consequence of setup.xml
        erddapUrl        = baseUrl        + "/" + warName;
        erddapHttpsUrl   = baseHttpsUrl   + "/" + warName;
        imageDirUrl      = erddapUrl      + "/" + IMAGES_DIR;   //static images, never need https:
        imageDirHttpsUrl = erddapHttpsUrl + "/" + IMAGES_DIR;   //static images, never need https:
        //downloadDirUrl   = erddapUrl      + "/" + DOWNLOAD_DIR;  //if uncommented, you need downloadDirHttpsUrl too

        //???if logoImgTag is needed, convert to method logoImgTag(loggedInAs)
        //logoImgTag = "      <img src=\"" + imageDirUrl(loggedInAs) + lowResLogoImageFile + "\" " +
        //    "alt=\"logo\" title=\"logo\">\n";
        if (subscriptionSystemActive) {
            //make subscriptions
            errorInMethod = "Error while initializing Subscriptions.";
            subscriptions = new Subscriptions(
                bigParentDirectory + "subscriptionsV1.txt", 48, //maxHoursPending, 
                erddapUrl); //always use non-https url                
        }

        //ensure images exist and get their sizes
        errorInMethod = "Error while ensuring images exist.";
        Image tImage = Image2.getImage(imageDir + lowResLogoImageFile, 10000, false);
        lowResLogoImageFileWidth   = tImage.getWidth(null);
        lowResLogoImageFileHeight  = tImage.getHeight(null);
        tImage = Image2.getImage(imageDir + highResLogoImageFile, 10000, false);
        highResLogoImageFileWidth  = tImage.getWidth(null);
        highResLogoImageFileHeight = tImage.getHeight(null);
        tImage = Image2.getImage(imageDir + googleEarthLogoFile, 10000, false);
        googleEarthLogoFileWidth   = tImage.getWidth(null);
        googleEarthLogoFileHeight  = tImage.getHeight(null);


        //**** messages.xml *************************************************************
        //read static messages from messages(2).xml in contentDirectory?
        String messagesFileName = contentDirectory + 
            "messages" + (developmentMode? "2" : "") + ".xml";
        if (File2.isFile(messagesFileName)) {
            String2.log("Using custom messages.xml from " + messagesFileName);
        } else {
            //use default messages.xml
            String2.log("Custom messages.xml not found at " + messagesFileName);
            //use String2.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
            messagesFileName = String2.getClassPath() + "gov/noaa/pfel/erddap/util/messages.xml";
            String2.log("Using default messages.xml from  " + messagesFileName);
        }
        errorInMethod = "Error while reading messages.xml.";
        ResourceBundle2 messages = ResourceBundle2.fromXml(XML.parseXml(messagesFileName, false));


        //read all the static Strings from messages.xml
        advancedSearch             = messages.getNotNothingString("advancedSearch",             "");
        advancedSearchResults      = messages.getNotNothingString("advancedSearchResults",      "");
        advancedSearchDirections   = messages.getNotNothingString("advancedSearchDirections",   "");
        advancedSearchHtml         = messages.getNotNothingString("advancedSearchHtml",         "");
        advancedSearchBounds       = messages.getNotNothingString("advancedSearchBounds",       "");
        advancedSearchMinLat       = messages.getNotNothingString("advancedSearchMinLat",       "");
        advancedSearchMaxLat       = messages.getNotNothingString("advancedSearchMaxLat",       "");
        advancedSearchMinLon       = messages.getNotNothingString("advancedSearchMinLon",       "");
        advancedSearchMaxLon       = messages.getNotNothingString("advancedSearchMaxLon",       "");
        advancedSearchMinMaxLon    = messages.getNotNothingString("advancedSearchMinMaxLon",    "");
        advancedSearchMinTime      = messages.getNotNothingString("advancedSearchMinTime",      "");
        advancedSearchMaxTime      = messages.getNotNothingString("advancedSearchMaxTime",      "");
        advancedSearchClear        = messages.getNotNothingString("advancedSearchClear",        "");
        advancedSearchClearHelp    = messages.getNotNothingString("advancedSearchClearHelp",    "");
        advancedSearchCategoryTooltip = messages.getNotNothingString("advancedSearchCategoryTooltip", "");
        advancedSearchRangeTooltip = messages.getNotNothingString("advancedSearchRangeTooltip", "");
        advancedSearchMapTooltip   = messages.getNotNothingString("advancedSearchMapTooltip",   "");
        advancedSearchLonTooltip   = messages.getNotNothingString("advancedSearchLonTooltip",   "");
        advancedSearchTimeTooltip  = messages.getNotNothingString("advancedSearchTimeTooltip",  "");
        advancedSearchWithCriteria = messages.getNotNothingString("advancedSearchWithCriteria", "");
        advancedSearchFewerCriteria = messages.getNotNothingString("advancedSearchFewerCriteria", "");
        advancedSearchNoCriteria   = messages.getNotNothingString("advancedSearchNoCriteria",   "");
        PrimitiveArray.ArrayAddN           = messages.getNotNothingString("ArrayAddN",          "");
        PrimitiveArray.ArrayAppendTables   = messages.getNotNothingString("ArrayAppendTables",  "");
        PrimitiveArray.ArrayDiff           = messages.getNotNothingString("ArrayDiff",          "");
        PrimitiveArray.ArrayDifferentSize  = messages.getNotNothingString("ArrayDifferentSize", "");
        PrimitiveArray.ArrayDifferentValue = messages.getNotNothingString("ArrayDifferentValue","");
        PrimitiveArray.ArrayDiffString     = messages.getNotNothingString("ArrayDiffString",    "");
        PrimitiveArray.ArrayMissingValue   = messages.getNotNothingString("ArrayMissingValue",  "");
        PrimitiveArray.ArrayNotAscending   = messages.getNotNothingString("ArrayNotAscending",  "");
        PrimitiveArray.ArrayNotDescending  = messages.getNotNothingString("ArrayNotDescending", "");
        PrimitiveArray.ArrayNotEvenlySpaced= messages.getNotNothingString("ArrayNotEvenlySpaced","");
        PrimitiveArray.ArrayRemove         = messages.getNotNothingString("ArrayRemove",        "");
        PrimitiveArray.ArraySubsetStart    = messages.getNotNothingString("ArraySubsetStart",   "");
        PrimitiveArray.ArraySubsetStride   = messages.getNotNothingString("ArraySubsetStride",  "");
        categoryTitleHtml          = messages.getNotNothingString("categoryTitleHtml",          "");
        category1Html              = messages.getNotNothingString("category1Html",              "");
        category2Html              = messages.getNotNothingString("category2Html",              "");
        category3Html              = messages.getNotNothingString("category3Html",              "");
        categoryPickAttribute      = messages.getNotNothingString("categoryPickAttribute",      "");
        categorySearchHtml         = messages.getNotNothingString("categorySearchHtml",         "");
        categorySearchDifferentHtml= messages.getNotNothingString("categorySearchDifferentHtml","");
        categoryClickHtml          = messages.getNotNothingString("categoryClickHtml",          "");
        categoryNotAnOption        = messages.getNotNothingString("categoryNotAnOption",        "");
        clickAccessHtml            = messages.getNotNothingString("clickAccessHtml",            "");
        clickAccess                = messages.getNotNothingString("clickAccess",                "");
        clickBackgroundInfo        = messages.getNotNothingString("clickBackgroundInfo",        "");
        clickInfo                  = messages.getNotNothingString("clickInfo",                  "");
        clickToSubmit              = messages.getNotNothingString("clickToSubmit",              "");
        convertFipsCounty          = messages.getNotNothingString("convertFipsCounty",          "");
        convertFipsCountyIntro     = messages.getNotNothingString("convertFipsCountyIntro",     "");
        convertFipsCountyNotes     = messages.getNotNothingString("convertFipsCountyNotes",     "");
        convertFipsCountyService   = messages.getNotNothingString("convertFipsCountyService",   "");
        convertHtml                = messages.getNotNothingString("convertHtml",                "");
        convertKeywords            = messages.getNotNothingString("convertKeywords",            "");
        convertKeywordsCfTooltip   = messages.getNotNothingString("convertKeywordsCfTooltip",   "");
        convertKeywordsGcmdTooltip = messages.getNotNothingString("convertKeywordsGcmdTooltip", "");
        convertKeywordsIntro       = messages.getNotNothingString("convertKeywordsIntro",       "");
        convertKeywordsNotes       = messages.getNotNothingString("convertKeywordsNotes",       "");
        convertKeywordsService     = messages.getNotNothingString("convertKeywordsService",     "");
        convertTime                = messages.getNotNothingString("convertTime",                "");
        convertTimeReference       = messages.getNotNothingString("convertTimeReference",       "");
        convertTimeIntro           = messages.getNotNothingString("convertTimeIntro",           "");
        convertTimeNotes           = messages.getNotNothingString("convertTimeNotes",           "");
        convertTimeService         = messages.getNotNothingString("convertTimeService",         "");
        convertTimeUnitsHelp       = messages.getNotNothingString("convertTimeUnitsHelp",       "");
        convertUnits               = messages.getNotNothingString("convertUnits",               "");
        convertUnitsComparison     = messages.getNotNothingString("convertUnitsComparison",     "");
        convertUnitsFilter         = messages.getNotNothingString("convertUnitsFilter",         "");
        convertUnitsIntro          = messages.getNotNothingString("convertUnitsIntro",          "");
        convertUnitsNotes          = messages.getNotNothingString("convertUnitsNotes",          "");
        convertUnitsService        = messages.getNotNothingString("convertUnitsService",        "");
        daf                        = messages.getNotNothingString("daf",                        "");
        dafGridBypass              = messages.getNotNothingString("dafGridBypass",              "");
        dafGridHtml                = messages.getNotNothingString("dafGridHtml",                "");
        dafTableBypass             = messages.getNotNothingString("dafTableBypass",             "");
        dafTableHtml               = messages.getNotNothingString("dafTableHtml",               "");
        dasTitle                   = messages.getNotNothingString("dasTitle",                   "");
        dataAccessNotAllowed       = messages.getNotNothingString("dataAccessNotAllowed",       "");
        disabled                   = messages.getNotNothingString("disabled",                   "");
        distinctValuesHtml         = messages.getNotNothingString("distinctValuesHtml",         "");
        doWithGraphs               = messages.getNotNothingString("doWithGraphs",               "");

        dtAccessible               = messages.getNotNothingString("dtAccessible",               "");
        dtAccessibleYes            = messages.getNotNothingString("dtAccessibleYes",            "");
        dtAccessibleNo             = messages.getNotNothingString("dtAccessibleNo",             "");
        dtAccessibleLogIn          = messages.getNotNothingString("dtAccessibleLogIn",          "");
        dtLogIn                    = messages.getNotNothingString("dtLogIn",                    "");
        dtDAF1                     = messages.getNotNothingString("dtDAF1",                     "");
        dtDAF2                     = messages.getNotNothingString("dtDAF2",                     "");
        dtMAG                      = messages.getNotNothingString("dtMAG",                      "");
        dtSOS                      = messages.getNotNothingString("dtSOS",                      "");
        dtSubset                   = messages.getNotNothingString("dtSubset",                   "");
        dtWCS                      = messages.getNotNothingString("dtWCS",                      "");
        dtWMS                      = messages.getNotNothingString("dtWMS",                      "");
        
        EDDDatasetID               = messages.getNotNothingString("EDDDatasetID",               "");
        EDDFgdc                    = messages.getNotNothingString("EDDFgdc",                    "");
        EDDFgdcMetadata            = messages.getNotNothingString("EDDFgdcMetadata",            "");
        EDDIso19115                = messages.getNotNothingString("EDDIso19115",                "");
        EDDIso19115Metadata        = messages.getNotNothingString("EDDIso19115Metadata",        "");
        EDDMetadata                = messages.getNotNothingString("EDDMetadata",                "");
        EDDBackground              = messages.getNotNothingString("EDDBackground",              "");
        EDDClickOnSubmitHtml       = messages.getNotNothingString("EDDClickOnSubmitHtml",       "");
        EDDInformation             = messages.getNotNothingString("EDDInformation",             "");
        EDDInstitution             = messages.getNotNothingString("EDDInstitution",             "");
        EDDSummary                 = messages.getNotNothingString("EDDSummary",                 "");
        EDDDatasetTitle            = messages.getNotNothingString("EDDDatasetTitle",            "");
        EDDDownloadData            = messages.getNotNothingString("EDDDownloadData",            "");
        EDDMakeAGraph              = messages.getNotNothingString("EDDMakeAGraph",              "");
        EDDMakeAMap                = messages.getNotNothingString("EDDMakeAMap",                "");
        EDDFileType                = messages.getNotNothingString("EDDFileType",                "");
        EDDFileTypeInformation     = messages.getNotNothingString("EDDFileTypeInformation",     "");
        EDDSelectFileType          = messages.getNotNothingString("EDDSelectFileType",          "");
        EDDMinimum                 = messages.getNotNothingString("EDDMinimum",                 "");
        EDDMaximum                 = messages.getNotNothingString("EDDMaximum",                 "");
        EDDConstraint              = messages.getNotNothingString("EDDConstraint",              "");

        EDDChangedWasnt            = messages.getNotNothingString("EDDChangedWasnt",            "");
        EDDChangedDifferentNVar    = messages.getNotNothingString("EDDChangedDifferentNVar",    "");
        EDDChanged2Different       = messages.getNotNothingString("EDDChanged2Different",       "");
        EDDChanged1Different       = messages.getNotNothingString("EDDChanged1Different",       "");
        EDDChangedCGADifferent     = messages.getNotNothingString("EDDChangedCGADifferent",     "");
        EDDChangedAxesDifferentNVar= messages.getNotNothingString("EDDChangedAxesDifferentNVar","");
        EDDChangedAxes2Different   = messages.getNotNothingString("EDDChangedAxes2Different",   "");
        EDDChangedAxes1Different   = messages.getNotNothingString("EDDChangedAxes1Different",   "");
        EDDChangedNoValue          = messages.getNotNothingString("EDDChangedNoValue",          "");
        EDDChangedTableToGrid      = messages.getNotNothingString("EDDChangedTableToGrid",      "");

        EDDSimilarDifferentNVar    = messages.getNotNothingString("EDDSimilarDifferentNVar",    "");
        EDDSimilarDifferent        = messages.getNotNothingString("EDDSimilarDifferent",        "");

        EDDGridDownloadTooltipHtml = messages.getNotNothingString("EDDGridDownloadTooltipHtml", "");
        EDDGridDapDescription      = messages.getNotNothingString("EDDGridDapDescription",      "");
        EDDGridDapLongDescription  = messages.getNotNothingString("EDDGridDapLongDescription",  "");
        EDDGridDownloadDataHtml    = messages.getNotNothingString("EDDGridDownloadDataHtml",    "");
        EDDGridDimension           = messages.getNotNothingString("EDDGridDimension",           "");
        EDDGridDimensionRanges     = messages.getNotNothingString("EDDGridDimensionRanges",     "");
        EDDGridFirst               = messages.getNotNothingString("EDDGridFirst",               "");
        EDDGridLast                = messages.getNotNothingString("EDDGridLast",                "");
        EDDGridStart               = messages.getNotNothingString("EDDGridStart",               "");
        EDDGridStop                = messages.getNotNothingString("EDDGridStop",                "");
        EDDGridStartStopHelp       = messages.getNotNothingString("EDDGridStartStopHelp",       "");
        EDDGridStride              = messages.getNotNothingString("EDDGridStride",              "");
        EDDGridNValues             = messages.getNotNothingString("EDDGridNValues",             "");
        EDDGridNValuesHtml         = messages.getNotNothingString("EDDGridNValuesHtml",         "");
        EDDGridSpacing             = messages.getNotNothingString("EDDGridSpacing",             "");
        EDDGridJustOneValue        = messages.getNotNothingString("EDDGridJustOneValue",        "");
        EDDGridEven                = messages.getNotNothingString("EDDGridEven",                "");
        EDDGridUneven              = messages.getNotNothingString("EDDGridUneven",              "");
        EDDGridDimensionHtml       = messages.getNotNothingString("EDDGridDimensionHtml",       "");
        EDDGridDimensionFirstHtml  = messages.getNotNothingString("EDDGridDimensionFirstHtml",  "");
        EDDGridDimensionLastHtml   = messages.getNotNothingString("EDDGridDimensionLastHtml",   "");
        EDDGridVarHasDimHtml       = messages.getNotNothingString("EDDGridVarHasDimHtml",       "");
        EDDGridSSSHtml             = messages.getNotNothingString("EDDGridSSSHtml",             "");
        EDDGridStartHtml           = messages.getNotNothingString("EDDGridStartHtml",           "");
        EDDGridStopHtml            = messages.getNotNothingString("EDDGridStopHtml",            "");
        EDDGridStrideHtml          = messages.getNotNothingString("EDDGridStrideHtml",          "");
        EDDGridSpacingHtml         = messages.getNotNothingString("EDDGridSpacingHtml",         "");
        EDDGridGridVariableHtml    = messages.getNotNothingString("EDDGridGridVariableHtml",    "");

        EDDTableConstraints        = messages.getNotNothingString("EDDTableConstraints",        "");
        EDDTableDapDescription     = messages.getNotNothingString("EDDTableDapDescription",     "");
        EDDTableDapLongDescription = messages.getNotNothingString("EDDTableDapLongDescription", "");
        EDDTableDownloadDataHtml   = messages.getNotNothingString("EDDTableDownloadDataHtml",   "");
        EDDTableTabularDatasetHtml = messages.getNotNothingString("EDDTableTabularDatasetHtml", "");
        EDDTableVariable           = messages.getNotNothingString("EDDTableVariable",           "");
        EDDTableCheckAll           = messages.getNotNothingString("EDDTableCheckAll",           "");
        EDDTableCheckAllTooltip    = messages.getNotNothingString("EDDTableCheckAllTooltip",    "");
        EDDTableUncheckAll         = messages.getNotNothingString("EDDTableUncheckAll",         "");
        EDDTableUncheckAllTooltip  = messages.getNotNothingString("EDDTableUncheckAllTooltip",  "");
        EDDTableMinimumTooltip     = messages.getNotNothingString("EDDTableMinimumTooltip",     "");
        EDDTableMaximumTooltip     = messages.getNotNothingString("EDDTableMaximumTooltip",     "");
        EDDTableCheckTheVariables  = messages.getNotNothingString("EDDTableCheckTheVariables",  "");
        EDDTableSelectAnOperator   = messages.getNotNothingString("EDDTableSelectAnOperator",   "");
        EDDTableOptConstraint1Html = messages.getNotNothingString("EDDTableOptConstraint1Html", "");
        EDDTableOptConstraint2Html = messages.getNotNothingString("EDDTableOptConstraint2Html", "");
        EDDTableOptConstraintVar   = messages.getNotNothingString("EDDTableOptConstraintVar",   "");
        EDDTableNumericConstraintHtml=messages.getNotNothingString("EDDTableNumericConstraintHtml","");
        EDDTableStringConstraintHtml=messages.getNotNothingString("EDDTableStringConstraintHtml","");
        EDDTableTimeConstraintHtml = messages.getNotNothingString("EDDTableTimeConstraintHtml", "");
        EDDTableConstraintHtml     = messages.getNotNothingString("EDDTableConstraintHtml",     "");
        EDDTableSelectConstraint   = messages.getNotNothingString("EDDTableSelectConstraint",   "");

        errorTitle                 = messages.getNotNothingString("errorTitle",                 "");
        errorRequestUrl            = messages.getNotNothingString("errorRequestUrl",            "");
        errorRequestQuery          = messages.getNotNothingString("errorRequestQuery",          "");
        errorTheError              = messages.getNotNothingString("errorTheError",              "");
        errorCopyFrom              = messages.getNotNothingString("errorCopyFrom",              "");
        errorFileNotFound          = messages.getNotNothingString("errorFileNotFound",          "");
        errorFileNotFoundImage     = messages.getNotNothingString("errorFileNotFoundImage",     "");
        errorInternal              = messages.getNotNothingString("errorInternal",              "") +
            " ";
        errorJsonpFunctionName     = messages.getNotNothingString("errorJsonpFunctionName",     "");
        errorMoreThan2GB           = messages.getNotNothingString("errorMoreThan2GB",           "");
        errorNotFound              = messages.getNotNothingString("errorNotFound",              "");
        errorNotFoundIn            = messages.getNotNothingString("errorNotFoundIn",            "");
        errorOdvLLTGrid            = messages.getNotNothingString("errorOdvLLTGrid",            "");
        errorOdvLLTTable           = messages.getNotNothingString("errorOdvLLTTable",           "");
        fileHelp_asc               = messages.getNotNothingString("fileHelp_asc",               "");
        fileHelp_csv               = messages.getNotNothingString("fileHelp_csv",               "");
        fileHelp_csvp              = messages.getNotNothingString("fileHelp_csvp",              "");
        fileHelp_das               = messages.getNotNothingString("fileHelp_das",               "");
        fileHelp_dds               = messages.getNotNothingString("fileHelp_dds",               "");
        fileHelp_dods              = messages.getNotNothingString("fileHelp_dods",              "");
        fileHelpGrid_esriAscii     = messages.getNotNothingString("fileHelpGrid_esriAscii",     "");
        fileHelpTable_esriAscii    = messages.getNotNothingString("fileHelpTable_esriAscii",    "");
        fileHelp_fgdc              = messages.getNotNothingString("fileHelp_fgdc",              "");
        fileHelp_geoJson           = messages.getNotNothingString("fileHelp_geoJson",           "");
        fileHelp_graph             = messages.getNotNothingString("fileHelp_graph",             "");
        fileHelpGrid_help          = messages.getNotNothingString("fileHelpGrid_help",          "");
        fileHelpTable_help         = messages.getNotNothingString("fileHelpTable_help",         "");
        fileHelp_html              = messages.getNotNothingString("fileHelp_html",              "");
        fileHelp_htmlTable         = messages.getNotNothingString("fileHelp_htmlTable",         "");
        fileHelp_iso19115          = messages.getNotNothingString("fileHelp_iso19115",          "");
        fileHelp_json              = messages.getNotNothingString("fileHelp_json",              "");
        fileHelp_mat               = messages.getNotNothingString("fileHelp_mat",               "");
        fileHelpGrid_nc            = messages.getNotNothingString("fileHelpGrid_nc",            "");
        fileHelpTable_nc           = messages.getNotNothingString("fileHelpTable_nc",           "");
        fileHelp_ncHeader          = messages.getNotNothingString("fileHelp_ncHeader",          "");
        fileHelp_ncCF              = messages.getNotNothingString("fileHelp_ncCF",              "");
        fileHelp_ncCFHeader        = messages.getNotNothingString("fileHelp_ncCFHeader",        "");
        fileHelp_ncCFMA            = messages.getNotNothingString("fileHelp_ncCFMA",            "");
        fileHelp_ncCFMAHeader      = messages.getNotNothingString("fileHelp_ncCFMAHeader",      "");
        fileHelpGrid_odvTxt        = messages.getNotNothingString("fileHelpGrid_odvTxt",        "");
        fileHelpTable_odvTxt       = messages.getNotNothingString("fileHelpTable_odvTxt",       "");
        fileHelp_subset            = messages.getNotNothingString("fileHelp_subset",            "");
        fileHelp_tsv               = messages.getNotNothingString("fileHelp_tsv",               "");
        fileHelp_tsvp              = messages.getNotNothingString("fileHelp_tsvp",              "");
        fileHelp_xhtml             = messages.getNotNothingString("fileHelp_xhtml",             "");
        fileHelp_geotif            = messages.getNotNothingString("fileHelp_geotif",            "");
        fileHelpGrid_kml           = messages.getNotNothingString("fileHelpGrid_kml",           "");
        fileHelpTable_kml          = messages.getNotNothingString("fileHelpTable_kml",          "");
        fileHelp_smallPdf          = messages.getNotNothingString("fileHelp_smallPdf",          "");
        fileHelp_pdf               = messages.getNotNothingString("fileHelp_pdf",               "");
        fileHelp_largePdf          = messages.getNotNothingString("fileHelp_largePdf",          "");
        fileHelp_smallPng          = messages.getNotNothingString("fileHelp_smallPng",          "");
        fileHelp_png               = messages.getNotNothingString("fileHelp_png",               "");
        fileHelp_largePng          = messages.getNotNothingString("fileHelp_largePng",          "");
        fileHelp_transparentPng    = messages.getNotNothingString("fileHelp_transparentPng",    "");
        functions                  = messages.getNotNothingString("functions",                  "");
        functionHtml               = messages.getNotNothingString("functionHtml",               "");
        functionDistinctCheck      = messages.getNotNothingString("functionDistinctCheck",      "");
        functionDistinctHtml       = messages.getNotNothingString("functionDistinctHtml",       "");
        functionOrderByHtml        = messages.getNotNothingString("functionOrderByHtml",        "");
        functionOrderBySort        = messages.getNotNothingString("functionOrderBySort",        "");
        functionOrderBySort1       = messages.getNotNothingString("functionOrderBySort1",       "");
        functionOrderBySort2       = messages.getNotNothingString("functionOrderBySort2",       "");
        functionOrderBySort3       = messages.getNotNothingString("functionOrderBySort3",       "");
        functionOrderBySort4       = messages.getNotNothingString("functionOrderBySort4",       "");
        functionOrderBySortLeast   = messages.getNotNothingString("functionOrderBySortLeast",   "");
        functionOrderBySortRowMax  = messages.getNotNothingString("functionOrderBySortRowMax",  "");
        geoServicesDescription     = messages.getNotNothingString("geoServicesDescription",     "");
        getStartedHtml             = messages.getNotNothingString("getStartedHtml",             "");
        TableWriterHtmlTable.htmlTableMaxMB     = messages.getInt("htmlTableMaxMB", TableWriterHtmlTable.htmlTableMaxMB);                                   
        htmlTableMaxMessage        = messages.getNotNothingString("htmlTableMaxMessage",        "");
        imageDataCourtesyOf        = messages.getNotNothingString("imageDataCourtesyOf",        "");
        imageWidths                = String2.toIntArray(String2.split(messages.getNotNothingString("imageWidths",  ""), ','));
        imageHeights               = String2.toIntArray(String2.split(messages.getNotNothingString("imageHeights", ""), ','));
        indexViewAll               = messages.getNotNothingString("indexViewAll",               "");
        indexSearchWith            = messages.getNotNothingString("indexSearchWith",            "");
        indexDevelopersSearch      = messages.getNotNothingString("indexDevelopersSearch",      "");
        indexProtocol              = messages.getNotNothingString("indexProtocol",              "");
        indexDescription           = messages.getNotNothingString("indexDescription",           "");
        indexDatasets              = messages.getNotNothingString("indexDatasets",              "");
        indexDocumentation         = messages.getNotNothingString("indexDocumentation",         "");
        indexRESTfulSearch         = messages.getNotNothingString("indexRESTfulSearch",         "");
        indexServices              = messages.getNotNothingString("indexServices",              "");
        indexDescribeServices      = messages.getNotNothingString("indexDescribeServices",      "");
        indexMetadata              = messages.getNotNothingString("indexMetadata",              "");
        indexWAF1                  = messages.getNotNothingString("indexWAF1",                  "");
        indexWAF2                  = messages.getNotNothingString("indexWAF2",                  "");
        indexConverters            = messages.getNotNothingString("indexConverters",            "");
        indexDescribeConverters    = messages.getNotNothingString("indexDescribeConverters",    "");
        infoAboutFrom              = messages.getNotNothingString("infoAboutFrom",              "");
        infoTableTitleHtml         = messages.getNotNothingString("infoTableTitleHtml",         "");
        infoRequestForm            = messages.getNotNothingString("infoRequestForm",            "");
        justGenerateAndView        = messages.getNotNothingString("justGenerateAndView",        "");
        justGenerateAndViewHtml    = messages.getNotNothingString("justGenerateAndViewHtml",    "");
        justGenerateAndViewUrl     = messages.getNotNothingString("justGenerateAndViewUrl",     "");
        justGenerateAndViewGraphUrlHtml = messages.getNotNothingString("justGenerateAndViewGraphUrlHtml", "");
        license                    = messages.getNotNothingString("license",                    "");
        listAll                    = messages.getNotNothingString("listAll",                    "");
        listOfDatasets             = messages.getNotNothingString("listOfDatasets",             "");
        LogIn                      = messages.getNotNothingString("LogIn",                      "");
        login                      = messages.getNotNothingString("login",                      "");
        loginAttemptBlocked        = messages.getNotNothingString("loginAttemptBlocked",        "");
        loginDescribeCustom        = messages.getNotNothingString("loginDescribeCustom",        "");
        loginDescribeOpenID        = messages.getNotNothingString("loginDescribeOpenID",        "");
        loginCanNot                = messages.getNotNothingString("loginCanNot",                "");
        loginWereNot               = messages.getNotNothingString("loginWereNot",               "");
        loginPleaseLogIn           = messages.getNotNothingString("loginPleaseLogIn",           "");
        loginUserName              = messages.getNotNothingString("loginUserName",              "");
        loginPassword              = messages.getNotNothingString("loginPassword",              "");
        loginUserNameAndPassword   = messages.getNotNothingString("loginUserNameAndPassword",   "");
        loginOpenID                = messages.getNotNothingString("loginOpenID",                "");
        loginOpenIDOr              = messages.getNotNothingString("loginOpenIDOr",              "");
        loginOpenIDCreate          = messages.getNotNothingString("loginOpenIDCreate",          "");
        loginOpenIDFree            = messages.getNotNothingString("loginOpenIDFree",            "");
        loginOpenIDSame            = messages.getNotNothingString("loginOpenIDSame",            "");
        loginAs                    = messages.getNotNothingString("loginAs",                    "");
        loginFailed                = messages.getNotNothingString("loginFailed",                "");
        loginInvalid               = messages.getNotNothingString("loginInvalid",               "");
        loginNot                   = messages.getNotNothingString("loginNot",                   "");
        loginBack                  = messages.getNotNothingString("loginBack",                  "");
        loginProblems              = messages.getNotNothingString("loginProblems",              "");
        loginProblemsAfter         = messages.getNotNothingString("loginProblemsAfter",         "");
        loginPublicAccess          = messages.getNotNothingString("loginPublicAccess",          "");
        LogOut                     = messages.getNotNothingString("LogOut",                     "");
        logout                     = messages.getNotNothingString("logout",                     "");
        logoutOpenID               = messages.getNotNothingString("logoutOpenID",               "");
        logoutSuccess              = messages.getNotNothingString("logoutSuccess",              "");
        mag                        = messages.getNotNothingString("mag",                        "");
        magAxisX                   = messages.getNotNothingString("magAxisX",                   "");
        magAxisY                   = messages.getNotNothingString("magAxisY",                   "");
        magAxisColor               = messages.getNotNothingString("magAxisColor",               "");
        magAxisStickX              = messages.getNotNothingString("magAxisStickX",              "");
        magAxisStickY              = messages.getNotNothingString("magAxisStickY",              "");
        magAxisVectorX             = messages.getNotNothingString("magAxisVectorX",             "");
        magAxisVectorY             = messages.getNotNothingString("magAxisVectorY",             "");
        magAxisHelpGraphX          = messages.getNotNothingString("magAxisHelpGraphX",          "");
        magAxisHelpGraphY          = messages.getNotNothingString("magAxisHelpGraphY",          "");
        magAxisHelpMarkerColor     = messages.getNotNothingString("magAxisHelpMarkerColor",     "");
        magAxisHelpSurfaceColor    = messages.getNotNothingString("magAxisHelpSurfaceColor",    "");
        magAxisHelpStickX          = messages.getNotNothingString("magAxisHelpStickX",          "");
        magAxisHelpStickY          = messages.getNotNothingString("magAxisHelpStickY",          "");
        magAxisHelpMapX            = messages.getNotNothingString("magAxisHelpMapX",            "");
        magAxisHelpMapY            = messages.getNotNothingString("magAxisHelpMapY",            "");
        magAxisHelpVectorX         = messages.getNotNothingString("magAxisHelpVectorX",         "");
        magAxisHelpVectorY         = messages.getNotNothingString("magAxisHelpVectorY",         "");
        magAxisVarHelp             = messages.getNotNothingString("magAxisVarHelp",             "");
        magAxisVarHelpGrid         = messages.getNotNothingString("magAxisVarHelpGrid",         "");
        magConstraintHelp          = messages.getNotNothingString("magConstraintHelp",          "");
        magDocumentation           = messages.getNotNothingString("magDocumentation",           "");
        magDownload                = messages.getNotNothingString("magDownload",                "");
        magDownloadTooltip         = messages.getNotNothingString("magDownloadTooltip",         "");
        magFileType                = messages.getNotNothingString("magFileType",                "");
        magGraphType               = messages.getNotNothingString("magGraphType",               "");
        magGraphTypeTooltipGrid    = messages.getNotNothingString("magGraphTypeTooltipGrid",    "");
        magGraphTypeTooltipTable   = messages.getNotNothingString("magGraphTypeTooltipTable",   "");
        magGS                      = messages.getNotNothingString("magGS",                      "");
        magGSMarkerType            = messages.getNotNothingString("magGSMarkerType",            "");
        magGSSize                  = messages.getNotNothingString("magGSSize",                  "");
        magGSColor                 = messages.getNotNothingString("magGSColor",                 "");
        magGSColorBar              = messages.getNotNothingString("magGSColorBar",              "");
        magGSColorBarTooltip       = messages.getNotNothingString("magGSColorBarTooltip",       "");
        magGSContinuity            = messages.getNotNothingString("magGSContinuity",            "");
        magGSContinuityTooltip     = messages.getNotNothingString("magGSContinuityTooltip",     "");
        magGSScale                 = messages.getNotNothingString("magGSScale",                 "");
        magGSScaleTooltip          = messages.getNotNothingString("magGSScaleTooltip",          "");
        magGSMin                   = messages.getNotNothingString("magGSMin",                   "");
        magGSMinTooltip            = messages.getNotNothingString("magGSMinTooltip",            "");
        magGSMax                   = messages.getNotNothingString("magGSMax",                   "");
        magGSMaxTooltip            = messages.getNotNothingString("magGSMaxTooltip",            "");
        magGSNSections             = messages.getNotNothingString("magGSNSections",             "");
        magGSNSectionsTooltip      = messages.getNotNothingString("magGSNSectionsTooltip",      "");
        magGSLandMask              = messages.getNotNothingString("magGSLandMask",              "");
        magGSLandMaskTooltipGrid   = messages.getNotNothingString("magGSLandMaskTooltipGrid",   "");
        magGSLandMaskTooltipTable  = messages.getNotNothingString("magGSLandMaskTooltipTable",  "");
        magGSVectorStandard        = messages.getNotNothingString("magGSVectorStandard",        "");
        magGSVectorStandardTooltip = messages.getNotNothingString("magGSVectorStandardTooltip", "");
        magGSYAxisMin              = messages.getNotNothingString("magGSYAxisMin",              "");
        magGSYAxisMax              = messages.getNotNothingString("magGSYAxisMax",              "");
        magGSYRangeMinTooltip      = messages.getNotNothingString("magGSYRangeMinTooltip",      ""); 
        magGSYRangeMaxTooltip      = messages.getNotNothingString("magGSYRangeMaxTooltip",      "");
        magGSYRangeTooltip         = messages.getNotNothingString("magGSYRangeTooltip",         "");        
        magItemFirst               = messages.getNotNothingString("magItemFirst",               "");
        magItemPrevious            = messages.getNotNothingString("magItemPrevious",            "");
        magItemNext                = messages.getNotNothingString("magItemNext",                "");
        magItemLast                = messages.getNotNothingString("magItemLast",                "");
        magJust1Value              = messages.getNotNothingString("magJust1Value",              "");
        magRange                   = messages.getNotNothingString("magRange",                   "");
        magRangeTo                 = messages.getNotNothingString("magRangeTo",                 "");
        magRedraw                  = messages.getNotNothingString("magRedraw",                  "");
        magRedrawTooltip           = messages.getNotNothingString("magRedrawTooltip",           "");
        magTimeRange               = messages.getNotNothingString("magTimeRange",               "");
        magTimeRangeFirst          = messages.getNotNothingString("magTimeRangeFirst",          "");
        magTimeRangeBack           = messages.getNotNothingString("magTimeRangeBack",           "");
        magTimeRangeForward        = messages.getNotNothingString("magTimeRangeForward",        "");
        magTimeRangeLast           = messages.getNotNothingString("magTimeRangeLast",           "");
        magTimeRangeTooltip        = messages.getNotNothingString("magTimeRangeTooltip",        "");
        magTimeRangeTooltip2       = messages.getNotNothingString("magTimeRangeTooltip2",       "");
        magTimesVary               = messages.getNotNothingString("magTimesVary",               "");
        magViewUrl                 = messages.getNotNothingString("magViewUrl",                 "");
        magZoom                    = messages.getNotNothingString("magZoom",                    "");
        magZoomCenter              = messages.getNotNothingString("magZoomCenter",              "");
        magZoomCenterTooltip       = messages.getNotNothingString("magZoomCenterTooltip",       "");
        magZoomIn                  = messages.getNotNothingString("magZoomIn",                  "");
        magZoomInTooltip           = messages.getNotNothingString("magZoomInTooltip",           "");
        magZoomOut                 = messages.getNotNothingString("magZoomOut",                 "");
        magZoomOutTooltip          = messages.getNotNothingString("magZoomOutTooltip",          "");
        magZoomALittle             = messages.getNotNothingString("magZoomALittle",             "");
        magZoomData                = messages.getNotNothingString("magZoomData",                "");
        magZoomOutData             = messages.getNotNothingString("magZoomOutData",             "");
        magGridHtml                = messages.getNotNothingString("magGridHtml",                "");
        magTableHtml               = messages.getNotNothingString("magTableHtml",               "");
        Math2.memoryTooMuchData    = messages.getNotNothingString("memoryTooMuchData",          "");
        Math2.memoryArraySize      = messages.getNotNothingString("memoryArraySize",            "");
      Math2.memoryThanCurrentlySafe= messages.getNotNothingString("memoryThanCurrentlySafe",    "");
        Math2.memoryThanSafe       = messages.getNotNothingString("memoryThanSafe",             "");
        metadataDownload           = messages.getNotNothingString("metadataDownload",           "");
        moreInformation            = messages.getNotNothingString("moreInformation",            "");
        MustBe.THERE_IS_NO_DATA    = messages.getNotNothingString("MustBeThereIsNoData",        "");
        MustBe.NotNull             = messages.getNotNothingString("MustBeNotNull",              "");
        MustBe.NotEmpty            = messages.getNotNothingString("MustBeNotEmpty",             "");
        MustBe.InternalError       = messages.getNotNothingString("MustBeInternalError",        "");
        MustBe.OutOfMemoryError    = messages.getNotNothingString("MustBeOutOfMemoryError",     "");
        nMatching1                 = messages.getNotNothingString("nMatching1",                 "");
        nMatchingAlphabetical      = messages.getNotNothingString("nMatchingAlphabetical",      "");
        nMatchingMostRelevant      = messages.getNotNothingString("nMatchingMostRelevant",      "");
        nMatchingPage              = messages.getNotNothingString("nMatchingPage",              "");
        nMatchingCurrent           = messages.getNotNothingString("nMatchingCurrent",           "");
        noDataFixedValue           = messages.getNotNothingString("noDataFixedValue",           "");
        noDataNoLL                 = messages.getNotNothingString("noDataNoLL",                 "");
        noDatasetWith              = messages.getNotNothingString("noDatasetWith",              "");
        noPage1                    = messages.getNotNothingString("noPage1",                    "");
        noPage2                    = messages.getNotNothingString("noPage2",                    "");
        notAuthorized              = messages.getNotNothingString("notAuthorized",              "");
        notAvailable               = messages.getNotNothingString("notAvailable",               "");
        noXxxBecause               = messages.getNotNothingString("noXxxBecause",               "");
        noXxxBecause2              = messages.getNotNothingString("noXxxBecause2",              "");
        noXxxNotActive             = messages.getNotNothingString("noXxxNotActive",             "");
        noXxxNoAxis1               = messages.getNotNothingString("noXxxNoAxis1",               "");
        noXxxNoCdmDataType         = messages.getNotNothingString("noXxxNoCdmDataType",         "");
        noXxxNoColorBar            = messages.getNotNothingString("noXxxNoColorBar",            "");
        noXxxNoLL                  = messages.getNotNothingString("noXxxNoLL",                  "");
        noXxxNoLLEvenlySpaced      = messages.getNotNothingString("noXxxNoLLEvenlySpaced",      "");
        noXxxNoLLGt1               = messages.getNotNothingString("noXxxNoLLGt1",               "");
        noXxxNoLLT                 = messages.getNotNothingString("noXxxNoLLT",                 "");
        noXxxNoLonIn180            = messages.getNotNothingString("noXxxNoLonIn180",            "");
        noXxxNoNonString           = messages.getNotNothingString("noXxxNoNonString",           "");
        noXxxNo2NonString          = messages.getNotNothingString("noXxxNo2NonString",          "");
        noXxxNoStation             = messages.getNotNothingString("noXxxNoStation",             "");
        noXxxNoStationID           = messages.getNotNothingString("noXxxNoStationID",           "");
        noXxxNoMinMax              = messages.getNotNothingString("noXxxNoMinMax",              "");
        noXxxItsGridded            = messages.getNotNothingString("noXxxItsGridded",            "");
        noXxxItsTabular            = messages.getNotNothingString("noXxxItsTabular",            "");
        optional                   = messages.getNotNothingString("optional",                   "");
        orRefineSearchWith         = messages.getNotNothingString("orRefineSearchWith",         "");
        orRefineSearchWith += " ";
        orSearchWith               = messages.getNotNothingString("orSearchWith",               "");
        orSearchWith += " ";
        orComma                    = messages.getNotNothingString("orComma",                    "");
        orComma += " ";
        palettes                   = String2.split(messages.getNotNothingString("palettes",     ""), ',');
        palettes0 = new String[palettes.length + 1];
        palettes0[0] = "";
        System.arraycopy(palettes, 0, palettes0, 1, palettes.length);
        patientData                = messages.getNotNothingString("patientData",                "");
        patientYourGraph           = messages.getNotNothingString("patientYourGraph",           "");
        pdfWidths                  = String2.toIntArray(String2.split(messages.getNotNothingString("pdfWidths",    ""), ','));
        pdfHeights                 = String2.toIntArray(String2.split(messages.getNotNothingString("pdfHeights",   ""), ','));
        pickADataset               = messages.getNotNothingString("pickADataset",               "");
        protocolSearchHtml         = messages.getNotNothingString("protocolSearchHtml",         "");
        protocolSearch2Html        = messages.getNotNothingString("protocolSearch2Html",        "");
        protocolClick              = messages.getNotNothingString("protocolClick",              "");
        queryError                 = messages.getNotNothingString("queryError",                 "") + 
                                     " ";
        queryError180              = messages.getNotNothingString("queryError180",              "");
        queryError1Value           = messages.getNotNothingString("queryError1Value",           "");
        queryError1Var             = messages.getNotNothingString("queryError1Var",             "");
        queryError2Var             = messages.getNotNothingString("queryError2Var",             "");
        queryErrorAdjusted         = messages.getNotNothingString("queryErrorAdjusted",         "");
        queryErrorAscending        = messages.getNotNothingString("queryErrorAscending",        "");
        queryErrorConstraintNaN    = messages.getNotNothingString("queryErrorConstraintNaN",    "");
        queryErrorEqualSpacing     = messages.getNotNothingString("queryErrorEqualSpacing",     "");
        queryErrorExpectedAt       = messages.getNotNothingString("queryErrorExpectedAt",       "");
        queryErrorFileType         = messages.getNotNothingString("queryErrorFileType",         "");
        queryErrorInvalid          = messages.getNotNothingString("queryErrorInvalid",          "");
        queryErrorLL               = messages.getNotNothingString("queryErrorLL",               "");
        queryErrorLLGt1            = messages.getNotNothingString("queryErrorLLGt1",            "");
        queryErrorLLT              = messages.getNotNothingString("queryErrorLLT",              "");
        queryErrorNeverTrue        = messages.getNotNothingString("queryErrorNeverTrue",        "");
        queryErrorNeverBothTrue    = messages.getNotNothingString("queryErrorNeverBothTrue",    "");
        queryErrorNotAxis          = messages.getNotNothingString("queryErrorNotAxis",          "");
        queryErrorNotExpectedAt    = messages.getNotNothingString("queryErrorNotExpectedAt",    "");
        queryErrorNotFoundAfter    = messages.getNotNothingString("queryErrorNotFoundAfter",    "");
        queryErrorOccursTwice      = messages.getNotNothingString("queryErrorOccursTwice",      "");
        queryErrorUnknownVariable  = messages.getNotNothingString("queryErrorUnknownVariable",  "");

        queryErrorGrid1Axis        = messages.getNotNothingString("queryErrorGrid1Axis",        "");
        queryErrorGridAmp          = messages.getNotNothingString("queryErrorGridAmp",          "");
        queryErrorGridDiagnostic   = messages.getNotNothingString("queryErrorGridDiagnostic",   "");
        queryErrorGridBetween      = messages.getNotNothingString("queryErrorGridBetween",      "");
        queryErrorGridLessMin      = messages.getNotNothingString("queryErrorGridLessMin",      "");
        queryErrorGridGreaterMax   = messages.getNotNothingString("queryErrorGridGreaterMax",   "");
        queryErrorGridMissing      = messages.getNotNothingString("queryErrorGridMissing",      "");
        queryErrorGridNoAxisVar    = messages.getNotNothingString("queryErrorGridNoAxisVar",    "");
        queryErrorGridNoDataVar    = messages.getNotNothingString("queryErrorGridNoDataVar",    "");
        queryErrorGridNotIdentical = messages.getNotNothingString("queryErrorGridNotIdentical", "");
        queryErrorGridSLessS       = messages.getNotNothingString("queryErrorGridSLessS",       "");
        queryErrorLastEndP         = messages.getNotNothingString("queryErrorLastEndP",         "");
        queryErrorLastExpected     = messages.getNotNothingString("queryErrorLastExpected",     "");
        queryErrorLastUnexpected   = messages.getNotNothingString("queryErrorLastUnexpected",   "");
        queryErrorLastPMInvalid    = messages.getNotNothingString("queryErrorLastPMInvalid",    "");
        queryErrorLastPMInteger    = messages.getNotNothingString("queryErrorLastPMInteger",    "");        
        rangesFromTo               = messages.getNotNothingString("rangesFromTo",               "");
        requestFormatExamplesHtml  = messages.getNotNothingString("requestFormatExamplesHtml",  "");
        resetTheForm               = messages.getNotNothingString("resetTheForm",               "");
        resetTheFormWas            = messages.getNotNothingString("resetTheFormWas",            "");
        resourceNotFound           = messages.getNotNothingString("resourceNotFound",           "");
        resultsFormatExamplesHtml  = messages.getNotNothingString("resultsFormatExamplesHtml",  "");
        resultsOfSearchFor         = messages.getNotNothingString("resultsOfSearchFor",         "");
        restfulInformationFormats  = messages.getNotNothingString("restfulInformationFormats",  "");
        restfulViaService          = messages.getNotNothingString("restfulViaService",          "");
        rows                       = messages.getNotNothingString("rows",                       "");
        searchTitle                = messages.getNotNothingString("searchTitle",                "");
        searchDoFullTextHtml       = messages.getNotNothingString("searchDoFullTextHtml",       "");
        searchFullTextHtml         = messages.getNotNothingString("searchFullTextHtml",         "");
        searchButton               = messages.getNotNothingString("searchButton",               "");
        searchClickTip             = messages.getNotNothingString("searchClickTip",             "");
        searchHintsHtml            = messages.getNotNothingString("searchHintsHtml",            "");
        searchHintsLuceneHtml      = messages.getNotNothingString("searchHintsLuceneHtml",      "");
        searchHintsOriginalHtml    = messages.getNotNothingString("searchHintsOriginalHtml",    "");
        searchNotAvailable         = messages.getNotNothingString("searchNotAvailable",         "");
        searchTip                  = messages.getNotNothingString("searchTip",                  "");
        searchSpelling             = messages.getNotNothingString("searchSpelling",             "");
        searchFewerWords           = messages.getNotNothingString("searchFewerWords",           "");
        searchWithQuery            = messages.getNotNothingString("searchWithQuery",            "");
        selectNext                 = messages.getNotNothingString("selectNext",                 "");
        selectPrevious             = messages.getNotNothingString("selectPrevious",             "");
        seeProtocolDocumentation   = messages.getNotNothingString("seeProtocolDocumentation",   "");
        ssUse                      = messages.getNotNothingString("ssUse",                      "");
        ssBePatient                = messages.getNotNothingString("ssBePatient",                "");
        ssInstructionsHtml         = messages.getNotNothingString("ssInstructionsHtml",         "");
        standardShortDescriptionHtml=messages.getNotNothingString("standardShortDescriptionHtml","");
        standardLicense            = messages.getNotNothingString("standardLicense",            "");
        standardContact            = messages.getNotNothingString("standardContact",            "");
        standardDataLicenses       = messages.getNotNothingString("standardDataLicenses",       "");
        standardDisclaimerOfExternalLinks=messages.getNotNothingString("standardDisclaimerOfExternalLinks", "");
        standardDisclaimerOfEndorsement=messages.getNotNothingString("standardDisclaimerOfEndorsement",            "");
        standardGeneralDisclaimer  = messages.getNotNothingString("standardGeneralDisclaimer",  "");
        standardPrivacyPolicy      = messages.getNotNothingString("standardPrivacyPolicy",      "");
        submit                     = messages.getNotNothingString("submit",                     "");
        submitTooltip              = messages.getNotNothingString("submitTooltip",              "");
        subscriptionsTitle         = messages.getNotNothingString("subscriptionsTitle",         "");
        subscriptionOptions        = messages.getNotNothingString("subscriptionOptions",        "");
        subscriptionAdd            = messages.getNotNothingString("subscriptionAdd",            "");
        subscriptionValidate       = messages.getNotNothingString("subscriptionValidate",       "");
        subscriptionList           = messages.getNotNothingString("subscriptionList",           "");
        subscriptionRemove         = messages.getNotNothingString("subscriptionRemove",         "");
        subscriptionHtml           = messages.getNotNothingString("subscriptionHtml",           "");
        subscription2Html          = messages.getNotNothingString("subscription2Html",          "");
        subscriptionAbuse          = messages.getNotNothingString("subscriptionAbuse",          "");
        subscriptionAddError       = messages.getNotNothingString("subscriptionAddError",       "");
        subscriptionAddHtml        = messages.getNotNothingString("subscriptionAddHtml",        "");
        subscriptionAdd2           = messages.getNotNothingString("subscriptionAdd2",           "");
        subscriptionAddSuccess     = messages.getNotNothingString("subscriptionAddSuccess",     "");
        subscriptionEmail          = messages.getNotNothingString("subscriptionEmail",          "");
        subscriptionEmailInvalid   = messages.getNotNothingString("subscriptionEmailInvalid",   "");
        subscriptionEmailTooLong   = messages.getNotNothingString("subscriptionEmailTooLong",   "");
        subscriptionEmailUnspecified=messages.getNotNothingString("subscriptionEmailUnspecified","");
        subscriptionIDInvalid      = messages.getNotNothingString("subscriptionIDInvalid",      "");
        subscriptionIDTooLong      = messages.getNotNothingString("subscriptionIDTooLong",      "");
        subscriptionIDUnspecified  = messages.getNotNothingString("subscriptionIDUnspecified",  "");
        subscriptionKeyInvalid     = messages.getNotNothingString("subscriptionKeyInvalid",     "");
        subscriptionKeyUnspecified = messages.getNotNothingString("subscriptionKeyUnspecified", "");
        subscriptionListError      = messages.getNotNothingString("subscriptionListError",      "");
        subscriptionListHtml       = messages.getNotNothingString("subscriptionListHtml",       "");
        subscriptionListSuccess    = messages.getNotNothingString("subscriptionListSuccess",    "");
        subscriptionRemoveError    = messages.getNotNothingString("subscriptionRemoveError",    "");
        subscriptionRemoveHtml     = messages.getNotNothingString("subscriptionRemoveHtml",     "");
        subscriptionRemove2        = messages.getNotNothingString("subscriptionRemove2",        "");
        subscriptionRemoveSuccess  = messages.getNotNothingString("subscriptionRemoveSuccess",  "");
        subscriptionRSS            = messages.getNotNothingString("subscriptionRSS",            "");
        subscriptionsNotAvailable  = messages.getNotNothingString("subscriptionsNotAvailable",  "");
        subscriptionUrlHtml        = messages.getNotNothingString("subscriptionUrlHtml",        "");
        subscriptionUrlInvalid     = messages.getNotNothingString("subscriptionUrlInvalid",     "");
        subscriptionUrlTooLong     = messages.getNotNothingString("subscriptionUrlTooLong",     "");
        subscriptionValidateError  = messages.getNotNothingString("subscriptionValidateError",  "");
        subscriptionValidateHtml   = messages.getNotNothingString("subscriptionValidateHtml",   "");
        subscriptionValidateSuccess= messages.getNotNothingString("subscriptionValidateSuccess","");
        subset                     = messages.getNotNothingString("subset",                     "");
        subsetSelect               = messages.getNotNothingString("subsetSelect",               "");
        subsetNMatching            = messages.getNotNothingString("subsetNMatching",            "");
        subsetInstructions         = messages.getNotNothingString("subsetInstructions",         "");
        subsetOption               = messages.getNotNothingString("subsetOption",               "");
        subsetOptions              = messages.getNotNothingString("subsetOptions",              "");
        subsetRefineMapDownload    = messages.getNotNothingString("subsetRefineMapDownload",    "");
        subsetRefineSubsetDownload = messages.getNotNothingString("subsetRefineSubsetDownload", "");
        subsetClickResetClosest    = messages.getNotNothingString("subsetClickResetClosest",    "");
        subsetClickResetLL         = messages.getNotNothingString("subsetClickResetLL",         "");
        subsetMetadata             = messages.getNotNothingString("subsetMetadata",             "");
        subsetCount                = messages.getNotNothingString("subsetCount",                "");
        subsetPercent              = messages.getNotNothingString("subsetPercent",              "");
        subsetViewSelect           = messages.getNotNothingString("subsetViewSelect",           "");
        subsetViewSelectDistinctCombos= messages.getNotNothingString("subsetViewSelectDistinctCombos","");
        subsetViewSelectRelatedCounts = messages.getNotNothingString("subsetViewSelectRelatedCounts", "");
        subsetWhen                 = messages.getNotNothingString("subsetWhen",                 "");
        subsetWhenNoConstraints    = messages.getNotNothingString("subsetWhenNoConstraints",    "");
        subsetWhenCounts           = messages.getNotNothingString("subsetWhenCounts",           "");
        subsetComboClickSelect     = messages.getNotNothingString("subsetComboClickSelect",     "");
        subsetNVariableCombos      = messages.getNotNothingString("subsetNVariableCombos",      "");
        subsetShowingAllRows       = messages.getNotNothingString("subsetShowingAllRows",       "");
        subsetShowingNRows         = messages.getNotNothingString("subsetShowingNRows",         "");
        subsetChangeShowing        = messages.getNotNothingString("subsetChangeShowing",        "");
        subsetNRowsRelatedData     = messages.getNotNothingString("subsetNRowsRelatedData",     "");
        subsetViewRelatedChange    = messages.getNotNothingString("subsetViewRelatedChange",    "");
        subsetTotalCount           = messages.getNotNothingString("subsetTotalCount",           "");
        subsetView                 = messages.getNotNothingString("subsetView",                 "");
        subsetViewCheck            = messages.getNotNothingString("subsetViewCheck",            "");
        subsetViewCheck1           = messages.getNotNothingString("subsetViewCheck1",           "");
        subsetViewDistinctMap      = messages.getNotNothingString("subsetViewDistinctMap",      "");
        subsetViewRelatedMap       = messages.getNotNothingString("subsetViewRelatedMap",       "");
        subsetViewDistinctDataCounts= messages.getNotNothingString("subsetViewDistinctDataCounts","");
        subsetViewDistinctData     = messages.getNotNothingString("subsetViewDistinctData",     "");
        subsetViewRelatedDataCounts= messages.getNotNothingString("subsetViewRelatedDataCounts","");
        subsetViewRelatedData      = messages.getNotNothingString("subsetViewRelatedData",      "");
        subsetViewDistinctMapTooltip       = messages.getNotNothingString("subsetViewDistinctMapTooltip",       "");
        subsetViewRelatedMapTooltip        = messages.getNotNothingString("subsetViewRelatedMapTooltip",        "");
        subsetViewDistinctDataCountsTooltip= messages.getNotNothingString("subsetViewDistinctDataCountsTooltip","");
        subsetViewDistinctDataTooltip      = messages.getNotNothingString("subsetViewDistinctDataTooltip",      "");
        subsetViewRelatedDataCountsTooltip = messages.getNotNothingString("subsetViewRelatedDataCountsTooltip", "");
        subsetViewRelatedDataTooltip       = messages.getNotNothingString("subsetViewRelatedDataTooltip",       "");
        subsetWarn                 = messages.getNotNothingString("subsetWarn",                 "");                    
        subsetWarn10000            = messages.getNotNothingString("subsetWarn10000",            "");
        subsetTooltip              = messages.getNotNothingString("subsetTooltip",              "");
        subsetNotSetUp             = messages.getNotNothingString("subsetNotSetUp",             "");
        subsetLongNotShown         = messages.getNotNothingString("subsetLongNotShown",         "");

        theLongDescriptionHtml     = messages.getNotNothingString("theLongDescriptionHtml",     "");
        unknownDatasetID           = messages.getNotNothingString("unknownDatasetID",           "");
        unknownProtocol            = messages.getNotNothingString("unknownProtocol",            "");
        unsupportedFileType        = messages.getNotNothingString("unsupportedFileType",        "");
        viewAllDatasetsHtml        = messages.getNotNothingString("viewAllDatasetsHtml",        "");
        waitThenTryAgain           = messages.getNotNothingString("waitThenTryAgain",           "");
        gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.waitThenTryAgain = waitThenTryAgain;
        sosDescriptionHtml         = messages.getNotNothingString("sosDescriptionHtml",         "");
        sosLongDescriptionHtml     = messages.getNotNothingString("sosLongDescriptionHtml",     ""); 
        wcsDescriptionHtml         = messages.getNotNothingString("wcsDescriptionHtml",         "");
        wcsLongDescriptionHtml     = messages.getNotNothingString("wcsLongDescriptionHtml",     ""); 
        wmsDescriptionHtml         = messages.getNotNothingString("wmsDescriptionHtml",         "");
        wmsInstructions            = messages.getNotNothingString("wmsInstructions",            ""); 
        wmsLongDescriptionHtml     = messages.getNotNothingString("wmsLongDescriptionHtml",     ""); 
        wmsManyDatasets            = messages.getNotNothingString("wmsManyDatasets",            ""); 

        Test.ensureEqual(imageWidths.length,  3, "imageWidths.length must be 3.");
        Test.ensureEqual(imageHeights.length, 3, "imageHeights.length must be 3.");
        Test.ensureEqual(pdfWidths.length,    3, "pdfWidths.length must be 3.");
        Test.ensureEqual(pdfHeights.length,   3, "pdfHeights.length must be 3.");

        for (int p = 0; p < palettes.length; p++) {
            String tName = fullPaletteDirectory + palettes[p] + ".cpt";
            Test.ensureTrue(File2.isFile(tName),
                "\"" + palettes[p] + 
                "\" is listed in <palettes>, but there is no file " + tName);
        }

        ampLoginInfoPo = startBodyHtml.indexOf(ampLoginInfo); 
        //String2.log("ampLoginInfoPo=" + ampLoginInfoPo);

        searchHintsHtml = searchHintsHtml + "\n" +
            (useLuceneSearchEngine? searchHintsLuceneHtml : searchHintsOriginalHtml);
        advancedSearchDirections = String2.replaceAll(advancedSearchDirections, "&searchButton;", searchButton);

        //always non-https: url
        convertFipsCountyService = MessageFormat.format(convertFipsCountyService, erddapUrl) + "\n";
        convertKeywordsService   = MessageFormat.format(convertKeywordsService,   erddapUrl) + "\n";
        convertTimeNotes         = MessageFormat.format(convertTimeNotes,         erddapUrl, convertTimeUnitsHelp) + "\n";
        convertTimeService       = MessageFormat.format(convertTimeService,       erddapUrl) + "\n"; 
        convertUnitsFilter       = MessageFormat.format(convertUnitsFilter,       erddapUrl, units_standard) + "\n";
        convertUnitsService      = MessageFormat.format(convertUnitsService,      erddapUrl) + "\n"; 

        //standardContact is used by legal
        String tEmail = String2.replaceAll(adminEmail, "@", " at ");
        tEmail        = String2.replaceAll(tEmail,     ".", " dot ");
        standardContact = String2.replaceAll(standardContact, "&adminEmail;", tEmail);
        legal = String2.replaceAll(legal,"[standardContact]",                   standardContact                   + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDataLicenses]",              standardDataLicenses              + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDisclaimerOfExternalLinks]", standardDisclaimerOfExternalLinks + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDisclaimerOfEndorsement]",   standardDisclaimerOfEndorsement   + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardPrivacyPolicy]",             standardPrivacyPolicy             + "\n\n"); 

        loginProblems      = String2.replaceAll(loginProblems,     "&adminContact;",  adminContact()) + "\n\n"; 
        loginProblemsAfter = String2.replaceAll(loginProblemsAfter,"&adminContact;",  adminContact()) + "\n\n"; 
        loginPublicAccess += "\n"; 
        logoutSuccess += "\n"; 

        PostIndex2Html = String2.replaceAll(PostIndex2Html, "&erddapUrl;",            erddapHttpsUrl);

        PostIndex3Html = String2.replaceAll(PostIndex3Html, "&PostSampleTag;",          PostSampleTag);
        PostIndex3Html = String2.replaceAll(PostIndex3Html, "&PostSurgeryDatasetID;",   PostSurgeryDatasetID);
        PostIndex3Html = String2.replaceAll(PostIndex3Html, "&PostDetectionDatasetID;", PostDetectionDatasetID);

        doWithGraphs = String2.replaceAll(doWithGraphs, "&ssUse;", slideSorterActive? ssUse : "");

        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&ssUse;", slideSorterActive? ssUse : "");
        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&requestFormatExamplesHtml;", requestFormatExamplesHtml);
        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&resultsFormatExamplesHtml;", resultsFormatExamplesHtml);

        standardShortDescriptionHtml = String2.replaceAll(standardShortDescriptionHtml, "&convertTimeReference;", convertersActive? convertTimeReference : "");
        standardShortDescriptionHtml = String2.replaceAll(standardShortDescriptionHtml, "&wmsManyDatasets;", wmsActive? wmsManyDatasets : "");

        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "[standardShortDescriptionHtml]", standardShortDescriptionHtml);
        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "&requestFormatExamplesHtml;",    requestFormatExamplesHtml);
        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "&resultsFormatExamplesHtml;",    resultsFormatExamplesHtml);
        postShortDescriptionActive = theShortDescriptionHtml.indexOf("[standardPostDescriptionHtml]") >= 0;

        //**************************************************************** 
        //other initialization

        //trigger CfToGcmd initialization to ensure CfToGcmd.txt file is valid.
        String testCfToGcmd[] = CfToFromGcmd.cfToGcmd("sea_water_temperature");
        Test.ensureTrue(testCfToGcmd.length > 0, 
            "testCfToGcmd=" + String2.toCSSVString(testCfToGcmd));

    } catch (Throwable t) {
        //display detailed information (e.g., dir names) in log.txt, not on a web page
        errorInMethod = "Error in EDStatic static{}:\n" + 
            errorInMethod + "\n" + 
            MustBe.throwableToString(t);
        String tell = "Ask the ERDDAP administrator to look at the detailed error message in ";
        if (String2.logFileName() == null) {
            System.out.println(errorInMethod);
            throw new RuntimeException(tell + "the tomcat logs (catalina.out?)."); 
        } else {
            String2.log(errorInMethod);
            //ensure log is flushed
            for (int i = 0; i < String2.logFileFlushEveryNth; i++)
                String2.log(""); 
            throw new RuntimeException(tell + "[bigParentDirectory]/logs/log.txt ."); 
        }
    }

    String2.log("EDStatic initialization finished successfully.\n");
    }
  
    /** 
     * If loggedInAs is null, this returns baseUrl, else baseHttpsUrl
     *  (neither has slash at end).
     *
     * @param loggedInAs
     * @return If loggedInAs == null, this returns baseUrl, else baseHttpsUrl
     *  (neither has slash at end).
     */
    public static String baseUrl(String loggedInAs) {
        return loggedInAs == null? baseUrl : baseHttpsUrl;
    }

    /** 
     * If loggedInAs is null, this returns erddapUrl, else erddapHttpsUrl
     *  (neither has slash at end).
     *
     * @param loggedInAs
     * @return If loggedInAs == null, this returns erddapUrl, else erddapHttpsUrl
     *  (neither has slash at end).
     */
    public static String erddapUrl(String loggedInAs) {
        return loggedInAs == null? erddapUrl : erddapHttpsUrl;
    }

    /** 
     * If publicAccess, this returns erddapUrl, else erddapHttpsUrl
     *  (neither has slash at end).
     *
     * @param publicAccess
     * @return If public access, this returns erddapUrl, else erddapHttpsUrl
     *  (neither has slash at end).
     */
    public static String publicErddapUrl(boolean publicAccess) {
        return publicAccess? erddapUrl : erddapHttpsUrl;
    }

    /** 
     * If loggedInAs is null, this returns imageDirUrl, else imageDirHttpsUrl
     *  (with slash at end).
     *
     * @param loggedInAs
     * @return If loggedInAs == null, this returns imageDirUrl, else imageDirHttpsUrl
     *  (with slash at end).
     */
    public static String imageDirUrl(String loggedInAs) {
        return loggedInAs == null? imageDirUrl : imageDirHttpsUrl;
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP.
     * 
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHere() {
        return 
            "\n<h1>" + ProgramName + 
            "\n</h1>\n";
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for a ERDDAP/protocol.
     * 
     * @param loggedInAs 
     * @param protocol e.g., tabledap
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHere(String loggedInAs, String protocol) {
        return 
            "\n<h1>" + erddapHref(erddapUrl(loggedInAs)) +
            "\n &gt; " + protocol + 
            "\n</h1>\n";
    }

    /** This returns a not-yet-HTML-encoded protocol URL.
     * You may want to encode it with XML.encodeAsHTML(url)
     */
    public static String protocolUrl(String tErddapUrl, String protocol) {
        return tErddapUrl + "/" + protocol + "/index.html" +
            (protocol.equals("tabledap") || protocol.equals("griddap")    ||
             protocol.equals("wms")      || protocol.equals("wcs")        ||
             protocol.equals("info")     || protocol.equals("categorize")? 
                "?" + defaultPIppQuery : "");            
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP/protocol/datasetID.
     * 
     * @param loggedInAs
     * @param protocol e.g., tabledap  (must be the same case as in the URL so the link will work)
     * @param datasetID e.g., erdGlobecBottle
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHere(String loggedInAs, String protocol, String datasetID) {
        String tErddapUrl = erddapUrl(loggedInAs);
        return 
            "\n<h1>" + erddapHref(tErddapUrl) +
            "\n &gt; <a rel=\"contents\" rev=\"chapter\" " +
                "href=\"" + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol)) +
                "\">" + protocol + "</a>" +
            "\n &gt; " + datasetID + 
            "\n</h1>\n";
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP/protocol with a helpful information.
     * 
     * @param loggedInAs
     * @param protocol e.g., tabledap
     * @param htmlHelp 
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHereWithHelp(String loggedInAs, String protocol, String htmlHelp) {
        String tErddapUrl = erddapUrl(loggedInAs);
        return 
            "\n<h1>" + erddapHref(tErddapUrl) + 
            "\n &gt; " + protocol + 
            "\n" + htmlTooltipImage(loggedInAs, htmlHelp) +
            "\n</h1>\n";
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP/protocol/datasetID with a helpful information.
     * 
     * @param loggedInAs
     * @param protocol e.g., tabledap
     * @param datasetID e.g., erdGlobecBottle
     * @param htmlHelp 
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHereWithHelp(String loggedInAs, String protocol, 
        String datasetID, String htmlHelp) {

        String tErddapUrl = erddapUrl(loggedInAs);
        return 
            "\n<h1>" + erddapHref(tErddapUrl) + 
            "\n &gt; <a rel=\"contents\" rev=\"chapter\" " +
                "href=\"" + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol)) + 
                "\">" + protocol + "</a>" +
            "\n &gt; " + datasetID + 
            "\n" + htmlTooltipImage(loggedInAs, htmlHelp) + 
            "\n</h1>\n";
    }

    /** THIS IS NO LONGER USED
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP/{protocol}/{attribute}/{category}.
     * IF REVIVED, append current ?page=x&amp;itemsPerPage=y
     * 
     * @param loggedInAs
     * @param protocol e.g., categorize
     * @param attribute e.g., ioos_category
     * @param category e.g., Temperature
     * @return the You Are Here html for this EDD subclass.
     */
    /*public static String youAreHere(String loggedInAs, String protocol, 
        String attribute, String category) {

        String tErddapUrl = erddapUrl(loggedInAs);
        String attributeUrl = tErddapUrl + "/" + protocol + "/" + attribute + "/index.html"; //+?defaultPIppQuery
        return 
            "\n<h1>" + erddapHref(tErddapUrl) + 
            "\n &gt; <a href=\"" + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol)) + 
                "\">" + protocol + "</a>" +
            "\n &gt; <a href=\"" + XML.encodeAsHTMLAttribute(attributeUrl) + "\">" + attribute + "</a>" +
            "\n &gt; " + category + 
            "\n</h1>\n";
    }*/

    /**
     * This returns the html to draw a question mark that has big html tooltip.
     * htmlTooltipScript (see HtmlWidgets) must be already in the document.
     *
     * @param html  the html tooltip text, e.g., "Hi,<br>there!".
     *     It needs explicit br tags to set window width correctly.
     *     For plain text, generate html from XML.encodeAsPreHTML(plainText, 100).
     */
    public static String htmlTooltipImage(String loggedInAs, String html) {
        return HtmlWidgets.htmlTooltipImage(
            imageDirUrl(loggedInAs) + questionMarkImageFile, "?", html, ""); 
    }

    /**
     * This returns the html to draw a question mark that has big html tooltip
     * for an EDDTable EDV data variable.
     *
     * @param edv from an EDDTable
     */
    public static String htmlTooltipImageEDV(String loggedInAs, EDV edv) 
        throws Throwable {

        return htmlTooltipImageLowEDV(loggedInAs,
            edv.destinationDataTypeClass(), 
            edv.destinationName(), 
            edv.combinedAttributes());
    }

    /**
     * This returns the html to draw a question mark that has big html tooltip 
     * for an EDDGrid EDV axis variable.
     *
     * @param edvga
     */
    public static String htmlTooltipImageEDVGA(String loggedInAs, EDVGridAxis edvga)
        throws Throwable {
        
        return htmlTooltipImageLowEDV(loggedInAs,
            edvga.destinationDataTypeClass(), 
            edvga.destinationName() + "[" + edvga.sourceValues().size() + "]",
            edvga.combinedAttributes());
    }

    /**
     * This returns the html to draw a question mark that has big html tooltip 
     * for an EDDGrid EDV data variable.
     *
     * @param edv for a grid variable
     * @param allDimString  from eddGrid.allDimString()
     */
    public static String htmlTooltipImageEDVG(String loggedInAs, EDV edv, String allDimString) 
        throws Throwable {

        return htmlTooltipImageLowEDV(loggedInAs,
            edv.destinationDataTypeClass(), 
            edv.destinationName() + allDimString, 
            edv.combinedAttributes());
    }

    /**
     * This returns the html to draw a question mark that has big html tooltip
     * with a variable's name and attributes.
     * htmlTooltipScript (see HtmlWidgets) must be already in the document.
     *
     * @param destinationDataTypeClass
     * @param destinationName  perhaps with axis information appended (e.g., [time][latitude][longitude]
     * @param attributes
     */
    public static String htmlTooltipImageLowEDV(String loggedInAs, Class destinationDataTypeClass, 
        String destinationName, Attributes attributes) throws Throwable {

        String destType = 
            //long and char aren't handled by getAtomicType. I don't think ever used.
            destinationDataTypeClass == long.class? "Int64" :   
            destinationDataTypeClass == char.class? "UInt16" :  //???   
            OpendapHelper.getAtomicType(destinationDataTypeClass);

        StringBuilder sb = OpendapHelper.dasToStringBuilder(
            destType + " " + destinationName, attributes, false); //false, do encoding below
        //String2.log("htmlTooltipImage sb=" + sb.toString());
        return htmlTooltipImage(loggedInAs, XML.encodeAsPreHTML(sb.toString(), 100));
    }

    /**
     * This writes the diagnostic info to an html writer.
     *
     * @param writer
     * @throws Throwable if trouble
     */
    public static void writeDiagnosticInfoHtml(Writer writer) throws Throwable {

        StringBuffer logSB = String2.getLogStringBuffer();
        if (logSB != null) {
            writer.write("<hr><h3>Diagnostic info</h3><pre>\n");
            //encodeAsHTML(logSB) is essential -- to prevent Cross-site-scripting security vulnerability
            //(which allows hacker to insert his javascript into pages returned by server)
            //See Tomcat (Definitive Guide) pg 147
            writer.write(XML.encodeAsHTML(logSB.toString()));
            writer.write("\n");
            //clear the String2.logStringBuffer
            logSB.setLength(0); 
            writer.write("</pre>\n"); 
        }
    }

    /**
     * This sends the specified email to *one* emailAddress.
     *
     * @param emailAddress   
     * @return an error message ("" if no error).
     *     If emailAddress is null or "", this logs the message and returns "".
     */
    public static String email(String emailAddress, String subject, String content) {
        return email(new String[]{emailAddress}, subject, content);
    }


    /**
     * This sends the specified email to the emailAddress.
     * <br>This won't throw an exception if trouble.
     * <br>This method always prepends the subject and content with [erddapUrl],
     *   so that it will be clear which ERDDAP this came from 
     *   (in case you administer multiple ERDDAPs).
     * <br>This method logs all emails (except duplicate emailDailyReportTo emails) 
     * to log, e.g., (bigParentDirectory)/emailLog2009-01.txt
     *
     * @param emailAddresses   each e.g., john.doe@company.com
     * @param subject If error, recommended: "Error in ERDDAP".
     * @param content If error, recommended: MustBe.throwableToString(t);
     * @return an error message ("" if no error).
     *     If emailAddresses is null or length==0, this logs the message and returns "".
     */
    public static String email(String emailAddresses[], String subject, String content) {

        //write the email to the log
        String localTime = Calendar2.getCurrentISODateTimeStringLocal();
        String fullMessage = 
            "\n==== BEGIN =====================================================================" +
            "\n     To: " + String2.toCSSVString(emailAddresses) + 
            "\nSubject: " + erddapUrl + " " + subject +  //always non-https url
            "\n   Date: " + localTime + 
            "\n--------------------------------------------------------------------------------" +
            "\n" + erddapUrl + " reports:" +  //always non-https url
            "\n" + content +
            "\n==== END =======================================================================" +
            "\n";

        //Don't String2.log(fullMessage) since private info may show up in diagnostic 
        //info at bottom of web pages and since it is written to emailLogFile below.

        //write to emailLog
        if (emailAddresses != null && emailAddresses.equals(emailDailyReportTo)) { 
            //don't log duplicate DailyReports  (feeble, but better than nothing)
            //i.e., only daily reports are sent to emailDailyReportTo
            //      and they are always also sent to emailEverythingTo (which will log it
        } else {
            try {
                String date = localTime.substring(0, 10);
                if (!emailLogDate.equals(date) || emailLogFile == null) {
                    //update emailLogDate
                    //do first so other threads won't do this simultaneously
                    emailLogDate = date;

                    //close the previous file
                    if (emailLogFile != null) {
                        try {emailLogFile.close(); 
                        } catch (Throwable t) {
                        }
                        emailLogFile = null;
                    }

                    //open a new file
                    emailLogFile = new BufferedWriter(new FileWriter(
                        fullLogsDirectory + "emailLog" + date + ".txt", 
                        true)); //true=append
                }

                //write the email to the log
                //do in one write encourages threads not to intermingle   (or synchronize on emailLogFile?)
                emailLogFile.write(fullMessage);
                emailLogFile.flush();

            } catch (Throwable t) {
                String2.log(MustBe.throwable("Error: Writing to emailLog failed.", t));
                if (emailLogFile != null) {
                    try {emailLogFile.close(); 
                    } catch (Throwable t2) {
                    }
                    emailLogFile = null;
                }
            }
        }

        //done?
        if (emailAddresses == null ||
            emailSmtpHost == null || emailSmtpHost.length() == 0) 
            return "";

        //send email
        StringBuilder errors = new StringBuilder();
        for (int ea = 0; ea < emailAddresses.length; ea++) {
            if (emailAddresses[ea] != null && emailAddresses[ea].length() > 0) {
                try {
                    SSR.sendEmail(emailSmtpHost, emailSmtpPort, emailUserName, 
                        emailPassword, emailProperties, emailFromAddress, emailAddresses[ea], 
                        erddapUrl + " " + subject, //always non-https url
                        erddapUrl + " reports:\n" + content); //always non-https url
                } catch (Throwable t) {
                    String msg = "Error: Sending email to " + emailAddresses[ea] + " failed";
                    String2.log(MustBe.throwable(msg, t));
                    errors.append(msg + ": " + t.toString() + "\n");
                   

                }
            }
        }

        //write errors to email log
        if (errors.length() > 0 && emailLogFile != null) {
            try {
                //do in one write encourages threads not to intermingle   (or synchronize on emailLogFile?)
                emailLogFile.write("\n********** ERRORS **********\n" + errors.toString());
                emailLogFile.flush();

            } catch (Throwable t) {
                String2.log(MustBe.throwable("Error: Writing to emailLog failed.", t));
                if (emailLogFile != null) {
                    try {emailLogFile.close(); 
                    } catch (Throwable t2) {
                    }
                    emailLogFile = null;
                }
            }
        }

        return errors.toString();
    }

    /**
     * This undoes the EDStatic initialization set up which sends log messages to 
     * log.txt, and sets up sending messages to System.out (with a buffer of 100000 chars).
     *
     */
    public static void returnLoggingToSystemOut() throws Throwable {
        String2.setupLog(true, false, "", true, false, 100000);
    }

    /**
     * This throws an exception if the requested nBytes are unlikely to be
     * available.
     * This isn't perfect, but is better than nothing. 
     * Future: locks? synchronization? ...?
     *
     * <p>This is almost identical to Math2.ensureMemoryAvailable, but adds tallying.
     *
     * @param nBytes  size of data structure that caller plans to create
     * @param attributeTo for the Tally system, this is the string (datasetID?) 
     *   to which this not-enough-memory issue should be attributed.
     * @throws RuntimeException if the requested nBytes are unlikely to be available.
     */
    public static void ensureMemoryAvailable(long nBytes, String attributeTo) {

        //if it is a small request, don't take the time/effort to check 
        if (nBytes < partialRequestMaxBytes) 
            return;
        String attributeToParen = 
            attributeTo == null || attributeTo.length() == 0? "" : " (" + attributeTo + ")";
        
        //is the request too big under any circumstances?
        if (nBytes > Math2.maxSafeMemory) {
            tally.add("Request refused: not enough memory ever (since startup)", attributeTo);
            tally.add("Request refused: not enough memory ever (since last daily report)", attributeTo);
            throw new RuntimeException(Math2.memoryTooMuchData + "  " +
                MessageFormat.format(Math2.memoryThanSafe, "" + (nBytes / Math2.BytesPerMB),  
                    "" + (Math2.maxSafeMemory / Math2.BytesPerMB)) +
                attributeToParen); 
        }

        //request is fine without gc?
        long memoryInUse = Math2.getMemoryInUse();
        if (memoryInUse + nBytes < Math2.maxSafeMemory / 2)  //getting close to maxSafeMemory?  was /4*3
            return;

        //lots of memory is in use
        //is the request is too big for right now?
        Math2.gc(500);
        memoryInUse = Math2.getMemoryInUse();
        if (memoryInUse + nBytes > Math2.maxSafeMemory) {
            //eek! not enough memory! 
            //Wait, then try gc again and hope that some other request requiring lots of memory will finish.
            //If nothing else, this 5 second delay will delay another request by same user (e.g., programmatic re-request)
            Math2.sleep(5000);
            Math2.gc(500); 
            memoryInUse = Math2.getMemoryInUse();
        }
        if (memoryInUse > Math2.maxSafeMemory) { 
            tally.add("MemoryInUse > MaxSafeMemory (since startup)", attributeTo);
            tally.add("MemoryInUse > MaxSafeMemory (since last daily report)", attributeTo);
        }
        if (memoryInUse + nBytes > Math2.maxSafeMemory) {
            tally.add("Request refused: not enough memory currently (since startup)", attributeTo);
            tally.add("Request refused: not enough memory currently (since last daily report)", attributeTo);
            throw new RuntimeException(Math2.memoryTooMuchData + "  " +
                MessageFormat.format(Math2.memoryThanCurrentlySafe,
                    "" + (nBytes / Math2.BytesPerMB), 
                    "" + ((Math2.maxSafeMemory - memoryInUse) / Math2.BytesPerMB)) +
                attributeToParen); 
        }
    }

    /** 
     * Even if JavaBits is 64, the limit on an array size is Integer.MAX_VALUE.
     *
     * <p>This is almost identical to Math2.ensureArraySizeOkay, but adds tallying.
     * 
     * @param tSize
     * @param attributeTo for the Tally system, this is the string (datasetID?) 
     *   to which this not-enough-memory issue should be attributed.
     * @throws Exception if tSize >= Integer.MAX_VALUE.  
     *  (equals is forbidden for safety since I often use if as missing value / trouble)
     */
    public static void ensureArraySizeOkay(long tSize, String attributeTo) { 
        if (tSize >= Integer.MAX_VALUE) {
            tally.add("Request refused: array size >= Integer.MAX_VALUE (since startup)", attributeTo);
            tally.add("Request refused: array size >= Integer.MAX_VALUE (since last daily report)", attributeTo);
            throw new RuntimeException(Math2.memoryTooMuchData + "  " +
                MessageFormat.format(Math2.memoryArraySize, 
                    "" + tSize, "" + Integer.MAX_VALUE) +
                (attributeTo == null || attributeTo.length() == 0? "" : " (" + attributeTo + ")"));
        }
    }


    /**
     * This sets the request blacklist of numeric ip addresses (e.g., 123.45.67.89)
     * (e.g., to fend of a Denial of Service attack or an overzealous web robot).
     * This sets requestBlacklist to be a HashSet (or null).
     *
     * @param csv the comma separated list of numeric ip addresses
     */
    public static void setRequestBlacklist(String csv) {
        if (csv == null || csv.length() == 0) {
            requestBlacklist = null;
            String2.log("requestBlacklist is now null.");
        } else {
            String rb[] = String2.split(csv, ',');
            HashSet hs = new HashSet(Math2.roundToInt(1.4 * rb.length));
            for (int i = 0; i < rb.length; i++)
                hs.add(rb[i]);
            requestBlacklist = hs; //set in an instant
            String2.log("requestBlacklist is now " + String2.toCSSVString(rb));
        }
    }

    /**
     * This adds the common, publicly accessible statistics to the StringBuilder.
     */
    public static void addIntroStatistics(StringBuilder sb) {
        sb.append("Current time is " + Calendar2.getCurrentISODateTimeStringLocal()  + " local time\n");
        sb.append("Startup was at  " + startupLocalDateTime + " local time\n");
        long loadTime = lastMajorLoadDatasetsStopTimeMillis - lastMajorLoadDatasetsStartTimeMillis;
        sb.append("Last major LoadDatasets started " + Calendar2.elapsedTimeString(1000 * 
            Math2.roundToInt((System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis)/1000) ) + 
            " ago and " +
            (loadTime < 0 ? "is still running.\n" : "finished after " + (loadTime/1000) + " seconds.\n"));
        //would be nice to know if minor LoadDataset is active, for how long
        sb.append("nGridDatasets  = " + nGridDatasets + "\n");
        sb.append("nTableDatasets = " + nTableDatasets + "\n");
        sb.append("nTotalDatasets = " + (nGridDatasets + nTableDatasets) + "\n");
        sb.append(datasetsThatFailedToLoad);
        sb.append(duplicateDatasetIDsMsg);
        sb.append("Response Failed    Time (since last major LoadDatasets) ");
        sb.append(String2.getBriefDistributionStatistics(failureTimesDistributionLoadDatasets) + "\n");
        sb.append("Response Failed    Time (since last Daily Report)       ");
        sb.append(String2.getBriefDistributionStatistics(failureTimesDistribution24) + "\n");
        sb.append("Response Failed    Time (since startup)                 ");
        sb.append(String2.getBriefDistributionStatistics(failureTimesDistributionTotal) + "\n");
        sb.append("Response Succeeded Time (since last major LoadDatasets) ");
        sb.append(String2.getBriefDistributionStatistics(responseTimesDistributionLoadDatasets) + "\n");
        sb.append("Response Succeeded Time (since last Daily Report)       ");
        sb.append(String2.getBriefDistributionStatistics(responseTimesDistribution24) + "\n");
        sb.append("Response Succeeded Time (since startup)                 ");
        sb.append(String2.getBriefDistributionStatistics(responseTimesDistributionTotal) + "\n");

        synchronized(taskList) { //all task-related things synch on taskList
            ensureTaskThreadIsRunningIfNeeded();  //clients (like this class) are responsible for checking on it
            long tElapsedTime = taskThread == null? -1 : taskThread.elapsedTime();
            sb.append("TaskThread has finished " + (lastFinishedTask + 1) + " out of " + 
                taskList.size() + " tasks.  " +
                (tElapsedTime < 0? 
                   "Currently, no task is running.\n" : 
                   "The current task has been running for " + Calendar2.elapsedTimeString(tElapsedTime) + ".\n"));
        }
        sb.append("TaskThread Failed    Time (since last Daily Report)     ");
        sb.append(String2.getBriefDistributionStatistics(taskThreadFailedDistribution24) + "\n");
        sb.append("TaskThread Failed    Time (since startup)               ");
        sb.append(String2.getBriefDistributionStatistics(taskThreadFailedDistributionTotal) + "\n");
        sb.append("TaskThread Succeeded Time (since last Daily Report)     ");
        sb.append(String2.getBriefDistributionStatistics(taskThreadSucceededDistribution24) + "\n");
        sb.append("TaskThread Succeeded Time (since startup)               ");
        sb.append(String2.getBriefDistributionStatistics(taskThreadSucceededDistributionTotal) + "\n");
    }

    /**
     * This adds the common, publicly accessible statistics to the StringBuffer.
     */
    public static void addCommonStatistics(StringBuilder sb) {
        if (memoryUseLoadDatasetsSB.length() > 0) {
            sb.append("Memory Use Summary (time series from ends of major LoadDatasets)\n");
            sb.append(memoryUseLoadDatasetsSB);
            sb.append("\n\n");
        }
        
        sb.append("Major LoadDatasets Times Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(majorLoadDatasetsDistribution24)); sb.append('\n');
        sb.append("Major LoadDatasets Times Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(majorLoadDatasetsDistributionTotal)); sb.append('\n');
        sb.append('\n');

        sb.append("Minor LoadDatasets Times Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(minorLoadDatasetsDistribution24)); sb.append('\n');
        sb.append("Minor LoadDatasets Times Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(minorLoadDatasetsDistributionTotal)); sb.append('\n');
        sb.append('\n');

        if (failureTimesLoadDatasetsSB.length() > 0) {
            sb.append("Response Failed Summary (time series from between major LoadDatasets)\n");
            sb.append(failureTimesLoadDatasetsSB);
            sb.append('\n');
        }
        sb.append("Response Failed Time Distribution (since last major LoadDatasets):\n");
        sb.append(String2.getDistributionStatistics(failureTimesDistributionLoadDatasets)); sb.append('\n');
        sb.append("Response Failed Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(failureTimesDistribution24)); sb.append('\n');
        sb.append("Response Failed Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(failureTimesDistributionTotal)); sb.append('\n');
        sb.append('\n');

        if (responseTimesLoadDatasetsSB.length() > 0) {
            sb.append("Response Succeeded Summary (time series from between major LoadDatasets)\n");
            sb.append(responseTimesLoadDatasetsSB);
            sb.append('\n');
        }
        sb.append("Response Succeeded Time Distribution (since last major LoadDatasets):\n");
        sb.append(String2.getDistributionStatistics(responseTimesDistributionLoadDatasets)); sb.append('\n');
        sb.append("Response Succeeded Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(responseTimesDistribution24)); sb.append('\n');
        sb.append("Response Succeeded Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(responseTimesDistributionTotal)); sb.append('\n');
        sb.append('\n');

        sb.append("TaskThread Failed Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(taskThreadFailedDistribution24)); sb.append('\n');
        sb.append("TaskThread Failed Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(taskThreadFailedDistributionTotal)); sb.append('\n');
        sb.append("TaskThread Succeeded Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(taskThreadSucceededDistribution24)); sb.append('\n');
        sb.append("TaskThread Succeeded Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(taskThreadSucceededDistributionTotal)); sb.append('\n');
        sb.append('\n');

        sb.append(SgtMap.topographyStats() + "\n");
        sb.append(GSHHS.statsString() + "\n");
        sb.append(SgtMap.nationalBoundaries.statsString() + "\n");
        sb.append(SgtMap.stateBoundaries.statsString() + "\n");
        sb.append(SgtMap.rivers.statsString() + "\n");
        sb.append(SgtUtil.isBufferedImageAccelerated() + "\n");
        sb.append('\n');

    }

    /** 
     * This returns the user's login name (or null if not logged in).
     *
     * <p>This relies on EDStatic.authentication
     *
     * <p>This is safe to use this after outputStream has been written to -- 
     *     this won't make a session if the user doesn't have one.
     *
     * @param request
     * @return the user's login name if logged in (or null if not logged in).
     */
    public static String getLoggedInAs(HttpServletRequest request) {
        if (authentication.equals("") || request == null)
            return null;

        //first, look in HTTP header for: Authentication ERDDAP_NVSA1
        /*
        String auth = request.getHeader("Authentication");
        if (auth != null) {
            String parts[] = StringArray.wordsAndQuotedPhrases(auth).toArray();
            if (String2.indexOf(parts, "ERDDAP_NVSA1") >= 0) {
                //yes, it is trying to use the ERDDAP Not Very Secure Authentication method #1
                String reqUser = String2.stringStartsWith(parts, "user=\"");
                String reqPassword  = String2.stringStartsWith(parts, "password=\"");
                reqUser = reqUser == null? "" : reqUser.substring(6, reqUser.length() - 1);
                reqPassword  = reqPassword  == null? "" :  reqPassword.substring(5,  reqPassword.length() - 1).toLowerCase();

                String url   = request.getRequestURI();  //post EDD.baseUrl, pre "?"
                String query = request.getQueryString(); //may be null;  leave encoded
                if (query != null && query.length() > 0)
                    url += "?" + query;
                
                String userPassword = getUserPassword(reqUser);  //see passwordEncoding in setup.xml 
                String expectedPassword = String2.md5Hex(url + ":" + userPassword);
                if (reqUser.length() > 0 && reqPassword.length() > 0 && userPassword != null &&
                    reqPassword.equals(expectedPassword)) {
                    return reqUser;
                } else {
                    String2.log("ERDDAP_NVSA1 authentication for user=" + reqUser + " failed\n" +
                        "  for url=" + url);
                }
            }
        }
        */

        //see if user is logged in with custom or openid methods
        //getSession(false): don't make a session if none currently
        //  that's what causes a problem if outputStream already committed
        //NOTE: session is associated with https urls, not http urls!
        //  So user only appears logged in to https urls.
        String loggedInAs = null;
        HttpSession session = request.getSession(false); //don't make one if none already
        //String2.log("session=" + (session==null? "null" : session.getServletContext().getServletContextName()));

        /*if (authentication.equals("basic")) {
            Principal p = request.getUserPrincipal();
            if (p != null)
                loggedInAs = p.getName();
        }
        */

        if (session != null) {
            
            if (authentication.equals("custom")) 
                loggedInAs = (String)session.getAttribute("loggedInAs:" + warName);

            else if (authentication.equals("openid")) 
                loggedInAs = OpenIdFilter.getCurrentUser(session);
        }

        //ensure printable characters only (which makes loggedInAsSuperuser special)
        if (loggedInAs != null)
            loggedInAs = String2.justPrintable(loggedInAs);
        return loggedInAs;
    }

    /** This allows LoadDatasets to set EDStatic.userHashMap (which is private).
     * There is no getUserHashMap (so info remains private).
     * MD5'd passwords should all already be lowercase.
     */
    public static void setUserHashMap(HashMap tUserHashMap) {
        userHashMap = tUserHashMap;
    }

    /** This allows EDDTableFromPostDatabase to set EDStatic.postUserHashMap (which is private).
     * There is no getPostUserHashMap (so info remains private).
     * MD5'd passwords should all already be lowercase.
     */
    public static void setPostUserHashMap(HashMap tPostUserHashMap) {
        postUserHashMap = tPostUserHashMap;
    }

    /**
     * This returns true if the plaintextPassword (after passwordEncoding as 
     * specified in setup.xml) matches the stored password for user.
     *
     * @param user the user's log in name
     * @param plaintextPassword that the user entered on a log-in form
     * @return true if the plaintextPassword (after passwordEncoding as 
     *    specified in setup.xml) matches the stored password for loggedInAs.
     *    If user==null or user has no password defined in datasets.xml, this returns false.
     */
    public static boolean doesPasswordMatch(String loggedInAs, String plaintextPassword) {
        if (loggedInAs == null || loggedInAs.length() == 0 ||
            !loggedInAs.equals(String2.justPrintable(loggedInAs)) ||
            plaintextPassword == null || plaintextPassword.length() < 7)
            return false;

        Object oar[] = (Object[])userHashMap.get(loggedInAs);
        if (oar == null && postUserHashMap != null)
            oar = (Object[])postUserHashMap.get(loggedInAs);
        if (oar == null) {
            String2.log("loggedInAs=" + loggedInAs + " not found in userHashMap or postUserHashMap.");
            return false;
        }
        String expected = (String)oar[0]; //using passwordEncoding in setup.xml
        if (expected == null)
            return false;

        //generate observedPassword from plaintextPassword via passwordEncoding 
        String observed = plaintextPassword;
        if (passwordEncoding.equals("MD5"))
            observed = String2.md5Hex(plaintextPassword); //it will be lowercase
        else if (passwordEncoding.equals("UEPMD5"))
            observed = String2.md5Hex(loggedInAs + ":ERDDAP:" + plaintextPassword); //it will be lowercase
        //only for debugging:
        //String2.log("loggedInAs=" + loggedInAs +
        //    "\nobsPassword=" + observed +
        //    "\nexpPassword=" + expected);

        boolean ok = observed.equals(expected);
        if (reallyVerbose)
            String2.log("loggedInAs=" + loggedInAs + " password matched: " + ok);
        return ok; 
    }

    /**
     * This returns the roles for a user.
     *
     * @param loggedInAs the user's logged in name (or null if not logged in)
     * @return the roles for the user.
     *    If user==null or user has no roles defined in datasets.xml, this returns null.
     */
    public static String[] getRoles(String loggedInAs) {
        if (loggedInAs == null)
            return null;

        //???future: for authentication="basic", use tomcat-defined roles???

        //all other authentication methods
        Object oar[] = (Object[])userHashMap.get(loggedInAs);
        if (oar == null && postUserHashMap != null)
            oar = (Object[])postUserHashMap.get(loggedInAs);
        if (oar == null)
            return null;
        return (String[])oar[1];
    }

    /**
     * If the user tries to access a dataset to which he doesn't have access,
     * call this to redirect him to the login page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetID  or use "" for general login.
     * @throws Throwable (notably ClientAbortException)
     */
    public static void redirectToLogin(String loggedInAs, 
        HttpServletResponse response, String datasetID) throws Throwable {

        String message = null;
        try {
            tally.add("Log in Redirect (since startup)", datasetID); 
            tally.add("Log in Redirect (since last daily report)", datasetID);
            if (datasetID != null && datasetID.length() > 0) 
                message = "loggedInAs=" + loggedInAs + 
                    " isn't authorized to use datasetID=" + datasetID + ".";

            if (authentication.equals("")) {
                //authentication.equals(""), so no way to log in , so send an error message  
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
                return;
            } 
            
            //BASIC
            /*
            if (authentication.equals("basic")) {
                //redirect to login.html which triggers Tomcat asking user to log in
                response.sendRedirect(erddapHttpsUrl + "/login.html");  //always https url
                return;
            } 
            */
            
            //all other authentication types, e.g., custom, openid
            response.sendRedirect(erddapHttpsUrl + "/login.html" +  //always https url
                "?message=" + SSR.minimalPercentEncode("Error: " + message));
            return;

        } catch (Throwable t2) {
            EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}
            String2.log("Error in redirectToLogin:\n" + 
                (message == null? "" : message + "\n") +
                MustBe.throwableToString(t2));
        }
    }

    /** 
     * This returns the session's login status html (with a link to log in/out)
     * suitable for use at the top of a web page.
     * <br>If logged in: loggedInAs | logout
     * <br>If not logged in:  login
     *
     * <p>This is safe to use this after outputStream has been written to -- 
     *     this won't make a session if the user doesn't have one.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     *   Special case: "loggedInAsLoggingIn" is used by login.html
     *   so that https is used for erddapUrl substitutions, 
     *   but &amp;loginInfo; indicates user isn't logged in.
     */
    public static String getLoginHtml(String loggedInAs) {
        if (authentication.equals("")) {
            //user can't log in
            return "";
        } else {
            String tLoggedInAs = loggedInAsLoggingIn.equals(loggedInAs)?
                null : loggedInAs;
            return tLoggedInAs == null?  //always use the erddapHttpsUrl for login/logout pages
                "<a href=\"" + erddapHttpsUrl + "/login.html\">" + login + "</a>" :
                "<a href=\"" + erddapHttpsUrl + "/login.html\"><b>" + XML.encodeAsHTML(tLoggedInAs) + "</b></a> | \n" + 
                "<a href=\"" + erddapHttpsUrl + "/logout.html\">" + logout + "</a>";
        }
    }

    /**
     * This returns the startBodyHtml (with the user's login info inserted if 
     * &amp;loginInfo; is present).
     *
     * <p>This is safe to use this after outputStream has been written to -- 
     *     this won't make a session if the user doesn't have one.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Special case: "loggedInAsLoggingIn" is used by login.html
     *   so that https is used for erddapUrl substitutions, 
     *   but &amp;loginInfo; indicates user isn't logged in.
     */
    public static String startBodyHtml(String loggedInAs) {
        String s = startBodyHtml;
        if (ampLoginInfoPo >= 0) {
            s = startBodyHtml.substring(0, ampLoginInfoPo) +
                getLoginHtml(loggedInAs) +
                startBodyHtml.substring(ampLoginInfoPo + ampLoginInfo.length());
        }
        return String2.replaceAll(s, "&erddapUrl;", erddapUrl(loggedInAs));
    }

    /**
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     */
    public static String endBodyHtml(            String tErddapUrl) {return String2.replaceAll(endBodyHtml,    "&erddapUrl;", tErddapUrl); }
    public static String legal(                  String tErddapUrl) {return String2.replaceAll(legal,          "&erddapUrl;", tErddapUrl); }
    public static String PostIndex1Html(         String tErddapUrl) {return String2.replaceAll(PostIndex1Html, "&erddapUrl;", tErddapUrl); }
    public static String PostIndex2Html()                           {return PostIndex2Html; }
    public static String PostIndex3Html(         String tErddapUrl) {return String2.replaceAll(PostIndex3Html, "&erddapUrl;", tErddapUrl); }

    /** @param addToTitle has not yet been encodeAsHTML(addToTitle). */
    public static String startHeadHtml(          String tErddapUrl, String addToTitle) {
        String ts = startHeadHtml;
        if (addToTitle.length() > 0)
            ts = String2.replaceAll(ts, "</title>", " - " + XML.encodeAsHTML(addToTitle) + "</title>"); 
        return String2.replaceAll(ts,    "&erddapUrl;", tErddapUrl); 
        }
    public static String theLongDescriptionHtml(   String tErddapUrl) {return String2.replaceAll(theLongDescriptionHtml,  "&erddapUrl;", tErddapUrl); }
    public static String theShortDescriptionHtml(  String tErddapUrl) {return String2.replaceAll(theShortDescriptionHtml, "&erddapUrl;", tErddapUrl); }
    public static String erddapHref(               String tErddapUrl) {
        return "<a title=\"The Environmental Research Division's Data Access Program\" \n" +
            "rel=\"start\" " +
            "href=\"" + tErddapUrl + "/index.html\">" + ProgramName + "</a>"; 
    }

    
    /** This calls pEncode then hEncode. */
    public static String phEncode(String tUrl) {
        return hEncode(pEncode(tUrl));
    }

    /** This calls XML.encodeAsHTML(tUrl). */
    public static String hEncode(String tUrl) {
        return XML.encodeAsHTML(tUrl);
    }

    /** This percent encodes &lt; and &gt; */ 
    public static String pEncode(String tUrl) {
        tUrl = String2.replaceAll(tUrl, "<", "%3C");
        return String2.replaceAll(tUrl, ">", "%3E");
    }

    public static String adminContact() {
        String ae = String2.replaceAll(adminEmail, "@", " at ");
        ae = String2.replaceAll(ae, ",", " dot ");
        return adminIndividualName + " (" + adminPhone + ", " + ae + ")";
    }

    /**
     * This appends an error message to an html page and flushes the writer.
     * This also logs the error.
     *
     * <p>Note that the use of try/catch blocks and htmlForException is necessary
     * because the outputstream is usually gzipped, so erddap can't just 
     * catch the exception in doGet try/catch and append error message 
     * since original outputstream and writer (which own/control the gzip stream) 
     * aren't available.
     */
    public static String htmlForException(Throwable t) {
        String message = MustBe.getShortErrorMessage(t);
        String2.log("HtmlForException is processing:\n " + 
            MustBe.throwableToString(t)); //log full message with stack trace
        return 
            "<p>&nbsp;<hr>\n" +
            "<p><font class=\"warningColor\"><b>An error occurred while writing this web page:</b>\n" +
            "<pre>" + XML.encodeAsPreHTML(message, 120) +
            "</pre></font>\n";
    }

    /** This interrupts/kill all of the thredds in runningThreads. 
     *  Erddap.destroy calls this when tomcat is stopped.
     */
    public static void destroy() {
        long time = System.currentTimeMillis();
        try {
            String names[] = String2.toStringArray(runningThreads.keySet().toArray());
            String2.log("\nEDStatic.destroy will try to interrupt nThreads=" + names.length + 
                "\n  threadNames=" +
                String2.toCSSVString(names));

            //interrupt all of them
            for (int i = 0; i < names.length; i++) {
                try {
                    Thread thread = (Thread)runningThreads.get(names[i]);
                    if (thread != null && thread.isAlive())
                        thread.interrupt();
                    else runningThreads.remove(names[i]);
                } catch (Throwable t) {
                    String2.log(MustBe.throwableToString(t));
                }
            }

            //wait for threads to finish
            int waitedSeconds = 0;
            int maxSeconds = 600; //10 minutes
            while (true) {
                boolean allDone = true;
                for (int i = 0; i < names.length; i++) {
                    try {
                        if (names[i] == null) 
                            continue; //it has already stopped
                        Thread thread = (Thread)runningThreads.get(names[i]);
                        if (thread != null && thread.isAlive()) {
                            allDone = false;
                            if (waitedSeconds > maxSeconds) {
                                String2.log("  " + names[i] + " thread is being stop()ped!!!");
                                thread.stop();
                                runningThreads.remove(names[i]);  
                                names[i] = null;
                            }
                        } else {
                            String2.log("  " + names[i] + " thread recognized the interrupt in " + 
                                waitedSeconds + " s");
                            runningThreads.remove(names[i]);  
                            names[i] = null;
                        }
                    } catch (Throwable t) {
                        String2.log(MustBe.throwableToString(t));
                        allDone = false;
                    }
                }
                if (allDone) {
                    String2.log("EDStatic.destroy successfully interrupted all threads in " + 
                        waitedSeconds + " s");
                    break;
                }
                if (waitedSeconds > maxSeconds) {
                    String2.log("!!! EDStatic.destroy is done, but it had to stop() some threads.");
                    break;
                }
                Math2.sleep(2000);
                waitedSeconds += 2;
            }

            //finally
            if (useLuceneSearchEngine) 
                String2.log("stopping lucene..."); 

            try { 
                if (luceneIndexSearcher != null) luceneIndexSearcher.close(); 
            } catch (Throwable t) {}
            luceneIndexSearcher = null;

            try {
                if (luceneIndexReader   != null) luceneIndexReader.close();  
            } catch (Throwable t) {}
            luceneIndexReader = null;
            luceneDatasetIDFieldCache = null;

            try {
                if (luceneIndexWriter   != null) 
                    //indices will be thrown away, so don't make pending changes
                    luceneIndexWriter.close(false); 
            } catch (Throwable t) {}
            luceneIndexWriter = null;

        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
    }


    /** This interrupts the thread and waits up to maxSeconds for it to finish.
     * If it still isn't finished, it is stopped.
     * 
     */
    public static void stopThread(Thread thread, int maxSeconds) {
        try {
            if (thread == null)
                return;
            String name = thread.getName();
            if (verbose) String2.log("stopThread(" + name + ")...");
            if (!thread.isAlive()) {
                if (verbose) String2.log("thread=" + name + " was already not alive.");
                return;
            }
            thread.interrupt();
            int waitSeconds = 0;
            while (thread.isAlive() && waitSeconds < maxSeconds) {
                waitSeconds += 2;
                Math2.sleep(2000);
            }
            if (thread.isAlive()) {
                if (verbose) String2.log("!!!Stopping thread=" + name + " after " + waitSeconds + " s");
                thread.stop();
            } else {
                if (verbose) String2.log("thread=" + name + " noticed interrupt in " + waitSeconds + " s");
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
    }

    /**
     * This checks if the task thread is running and not stalled.
     * If it is stalled, this will stop it.
     *
     * @return true if the task thread is running.
     *    If false, taskThread will be null.
     */
    public static boolean isTaskThreadRunning() {
        synchronized(taskList) { //all task-related things synch on taskList
            if (taskThread == null)
                return false;

            if (taskThread.isAlive()) {
                //is it stalled?
                long eTime = taskThread.elapsedTime();
                long maxTime = 6 * Calendar2.MILLIS_PER_HOUR; //appropriate??? user settable???
                if (eTime > maxTime) {  

                    //taskThread is stalled; interrupt it
                    String tError = "\n*** Error: EDStatic is interrupting a stalled taskThread (" +
                        Calendar2.elapsedTimeString(eTime) + " > " + 
                        Calendar2.elapsedTimeString(maxTime) + ") at " + 
                        Calendar2.getCurrentISODateTimeStringLocal();
                    email(emailEverythingTo, 
                        "taskThread Stalled", tError);
                    String2.log("\n*** " + tError);

                    stopThread(taskThread, 10); //short time; it is already in trouble
                    //runningThreads.remove   not necessary since new one is put() in below
                    lastFinishedTask = nextTask - 1;
                    taskThread = null;
                    return false;
                }
                return true;
            } else {
                //it isn't alive
                String2.log("\n*** EDStatic noticed that taskThread is finished (" + 
                    Calendar2.getCurrentISODateTimeStringLocal() + ")\n");
                lastFinishedTask = nextTask - 1;
                taskThread = null;
                return false;
            }
        }
    }

    /** 
     * This ensures the task thread is running if there are tasks to do
     */
    public static void ensureTaskThreadIsRunningIfNeeded() {
        synchronized(taskList) { //all task-related things synch on taskList
            try {
                //this checks if it is running and not stalled
                if (isTaskThreadRunning())
                    return;
                
                //taskThread isn't running
                //Are there no tasks to do? 
                int nPending = taskList.size() - nextTask;
                if (nPending <= 0) 
                    return; //no need to start it

                //need to start a new taskThread
                taskThread = new TaskThread(nextTask);
                runningThreads.put(taskThread.getName(), taskThread); 
                String2.log("\n*** new taskThread started at " + 
                    Calendar2.getCurrentISODateTimeStringLocal() + " nPendingTasks=" + nPending + "\n");
                taskThread.start();
                return;            
            } catch (Throwable t) {
            }
        }
    }

    /**
     * This returns the number of unfinished tasks.
     */
    public static int nUnfinishedTasks() {
        return (taskList.size() - lastFinishedTask) - 1;
    }

    /** This adds a task to the taskList if it (other than TASK_SET_FLAG)
     * isn't already on the taskList.
     * @return the task number that was assigned to the task,
     *   or -1 if it was a duplicate task.
     */
    public static int addTask(Object taskOA[]) {
        synchronized(taskList) { //all task-related things synch on taskList

            //Note that all task creators check that
            //   EDStatic.lastFinishedTask >= lastAssignedTask(datasetID).  I.E., tasks are all done,
            //before again creating new tasks.
            //So no need to see if this new task duplicates an existing unfinished task.  
            
            //add the task to the list
            taskList.add(taskOA);
            return taskList.size() - 1;
        }
    }

    /**
     * This returns the FIPS county table: col 0=FIPS (5-digit-FIPS), 1=Name (ST, County Name).
     * <br>States are included (their last 3 digits are 000).
     * <br>The table is sorted (case insensitive) by the county column.
     * <br>The most official source is http://www.itl.nist.gov/fipspubs/fip6-4.htm
     *
     * <p>The table is modified from http://www.census.gov/datamap/fipslist/AllSt.txt .
     * It includes the Appendix A and B counties from 
     * U.S. protectorates and county-equivalent entities of the freely associated atates
     * from http://www.itl.nist.gov/fipspubs/co-codes/states.txt .
     * I changed "lsabela" PR to "Isabela".
     *
     * @return the FIPS county table
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table fipsCountyTable() throws Exception {
        Table table = new Table();
        table.readASCII(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/FipsCounty.tsv", 
            0, 1, null, null, null, null, false); //false = don't simplify
        return table;
    }
    
    /**
     * This returns the complete list of CF Standard Names as a table with 1 column.
     *
     * @return the complete list of CF Standard Names as a table with 1 column
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table keywordsCfTable() throws Exception {
        StringArray sa = StringArray.fromFile(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/cfStdNames.txt");
        Table table = new Table();
        table.addColumn("CfStandardNames", sa);
        return table;
    }
    
    /**
     * This returns the complete list of GCMD Science Keywords as a table with 1 column.
     *
     * @return the complete list of GCMD Science Keywords as a table with 1 column
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table keywordsGcmdTable() throws Exception {
        StringArray sa = StringArray.fromFile(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/gcmdScienceKeywords.txt");
        Table table = new Table();
        table.addColumn("GcmdScienceKeywords", sa);
        return table;
    }
    
    /**
     * This returns the complete CF to GCMD conversion information as a table with 1 column.
     * The GCMD to CF conversion information can be derived from this.
     *
     * @return the CF to GCMD conversion information as a table with 1 column
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table keywordsCfToGcmdTable() throws Exception {
        StringArray sa = StringArray.fromFile(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/CfToGcmd.txt");
        Table table = new Table();
        table.addColumn("CfToGcmd", sa);
        return table;
    }
    
    /**
     * This returns true during the initial loadDatasets.
     * @return true during the initial loadDatasets, else false.
     */
    public static boolean initialLoadDatasets() {
        return memoryUseLoadDatasetsSB.length() == 0;
    }

    /** 
     * This creates an IndexWriter.
     * Normally, this is created once in RunLoadDatasets.
     * But if trouble, a new one will be created.
     *
     * @throws RuntimeException if trouble
     */
    public static void createLuceneIndexWriter(boolean firstTime) {

        try {
            String2.log("createLuceneIndexWriter(" + firstTime + ")");
            long tTime = System.currentTimeMillis();

            //if this is being called, directory shouldn't be locked
            //see javaDocs for indexWriter.close()
            if (IndexWriter.isLocked(luceneDirectory)) 
                IndexWriter.unlock(luceneDirectory);

            //create indexWriter
            IndexWriterConfig lucConfig = 
                new IndexWriterConfig(luceneVersion, luceneAnalyzer);
            lucConfig.setOpenMode(firstTime?
                IndexWriterConfig.OpenMode.CREATE :
                IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            luceneIndexWriter = new IndexWriter(luceneDirectory, lucConfig);
            luceneIndexWriter.setInfoStream(
                verbose? new PrintStream(new String2LogOutputStream()) : null); 
            String2.log("  createLuceneIndexWriter finished.  time=" +
                (System.currentTimeMillis() - tTime));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** 
     * This returns the Lucene IndexSearcher and datasetIDFieldCache (thread-safe).
     * IndexSearch uses IndexReader (also thread-safe).
     * IndexReader works on a snapshot of an index, 
     * so it is recreated if flagged at end of LoadDatasetsevery 
     * via needNewLuceneIndexReader.
     *
     * @return Object[2]: [0]=a luceneIndexSearcher (thread-safe), or null if trouble.
     *    [1]=luceneDatasetIDFieldCache, or null if trouble.
     *    Either both will be null or both will be !null.
     */
    public static Object[] luceneIndexSearcher() {

        //synchronize 
        synchronized (luceneIndexReaderLock) {

            //need a new indexReader?
            //(indexReader is thread-safe, but only need one)
            if (luceneIndexReader == null || needNewLuceneIndexReader ||
                luceneIndexSearcher == null) {

                //clear out old one
                try { 
                    if (luceneIndexSearcher != null) luceneIndexSearcher.close(); 
                } catch (Throwable t) {}
                luceneIndexSearcher = null;

                try {
                    if (luceneIndexReader   != null) luceneIndexReader.close();  
                } catch (Throwable t) {}
                luceneIndexReader = null;
                luceneDatasetIDFieldCache = null;

                needNewLuceneIndexReader = true;

                //create a new one
                try {
                    long rTime = System.currentTimeMillis();
                    luceneIndexReader = IndexReader.open(luceneDirectory); // read-only=true
                    luceneIndexSearcher = new IndexSearcher(luceneIndexReader);
                    String2.log("  new luceneIndexReader+Searcher time=" + 
                        (System.currentTimeMillis() - rTime));

                    //create the luceneDatasetIDFieldCache
                    //save memory by sharing the canonical strings  
                    //(EDD.ensureValid makes datasetID's canonical)
                    rTime = System.currentTimeMillis();
                    luceneDatasetIDFieldCache = FieldCache.DEFAULT.getStrings(luceneIndexReader, 
                        "datasetID");
                    int n = luceneDatasetIDFieldCache.length;
                    for (int i = 0; i < n; i++)
                        luceneDatasetIDFieldCache[i] = String2.canonical(luceneDatasetIDFieldCache[i]);
                    String2.log("  new luceneDatasetIDFieldCache time=" + 
                        (System.currentTimeMillis() - rTime));

                    //if successful, we no longer needNewLuceneIndexReader
                    needNewLuceneIndexReader = false;  
                    //if successful, fall through

                } catch (Throwable t) {
                    String subject = String2.ERROR + " while creating Lucene Searcher";
                    String msg = MustBe.throwableToString(t);
                    email(emailEverythingTo, subject, msg);
                    String2.log(subject + "\n" + msg);            

                    //clear out old one
                    try { 
                        if (luceneIndexSearcher != null) luceneIndexSearcher.close(); 
                    } catch (Throwable t2) {}
                    luceneIndexSearcher = null;

                    try {
                        if (luceneIndexReader   != null) luceneIndexReader.close();  
                    } catch (Throwable t2) {}
                    luceneIndexReader = null;
                    luceneDatasetIDFieldCache = null;

                    needNewLuceneIndexReader = true;

                    //return
                    return new Object[]{null, null};
                }
            }

            return new Object[]{luceneIndexSearcher, luceneDatasetIDFieldCache};
        }
    }

    /** 
     * This parses a query with luceneQueryParser (not thread-safe).
     *
     * @param searchString  the user's searchString, but modified slightly for Lucene
     * @return a Query, or null if trouble
     */
    public static Query luceneParseQuery(String searchString) {

        //queryParser is not thread-safe, so re-use it in a synchronized block
        //(It is fast, so synchronizing on one parser shouldn't be a bottleneck.
        synchronized (luceneQueryParser) {

            try {
                //long qTime = System.currentTimeMillis();
                Query q = luceneQueryParser.parse(searchString);
                //String2.log("  luceneParseQuery finished.  time=" + (System.currentTimeMillis() - qTime)); //always 0
                return q;
            } catch (Throwable t) {
                String2.log("Lucene failed to parse searchString=" + searchString + "\n" +
                    MustBe.throwableToString(t));
                return null;
            }
        }
    }

    /**
     * This gets the raw requested (or inferred) page number and itemsPerPage
     * by checking the request parameters.
     *
     * @param request
     * @return int[2]  
     *     [0]=page         (may be invalid, e.g., -5 or Integer.MAX_VALUE) 
     *     [1]=itemsPerPage (may be invalid, e.g., -5 or Integer.MAX_VALUE)
     */
    public static int[] getRawRequestedPIpp(HttpServletRequest request) {

        return new int[]{
            String2.parseInt(request.getParameter("page")),
            String2.parseInt(request.getParameter("itemsPerPage")) };
    }

    /**
     * This gets the requested (or inferred) page number and itemsPerPage
     * by checking the request parameters.
     *
     * @param request
     * @return int[2]  
     *     [0]=page (will be 1..., but may be too big), 
     *     [1]=itemsPerPage (will be 1...), 
     */
    public static int[] getRequestedPIpp(HttpServletRequest request) {

        int iar[] = getRawRequestedPIpp(request);

        //page is 1..
        if (iar[0] < 1 || iar[0] == Integer.MAX_VALUE)
            iar[0] = 1; //default

        //itemsPerPage
        if (iar[1] < 1 || iar[1] == Integer.MAX_VALUE)
            iar[1] = defaultItemsPerPage; 

        return iar;
    }

    /**
     * This returns the .jsonp=[functionName] part of the request (percent encoded) or "".
     * If not "", it will have "&" at the end.
     *
     * @param request
     * @return the .jsonp=[functionName] part of the request (percent encoded) or "".
     *   If not "", it will have "&" at the end.
     *   If the query has a syntax error, this returns "".
     *   If the !String2.isVariableNameSafe(functionName), this throws a SimpleException.
     */
    public static String passThroughJsonpQuery(HttpServletRequest request) {
        String jsonp = "";
        try {
            String parts[] = EDD.getUserQueryParts(request.getQueryString()); //decoded.  Does some validity checking.
            jsonp = String2.stringStartsWith(parts, ".jsonp="); //may be null
            if (jsonp == null)
                return "";
            String functionName = jsonp.substring(7); //it will be because it starts with .jsonp=
            if (!String2.isJsonpNameSafe(functionName))
                throw new SimpleException(errorJsonpFunctionName); 
            return ".jsonp=" + SSR.minimalPercentEncode(functionName) + "&";
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return "";
        }
    }


    /**
     * This extracts the page= and itemsPerPage= parameters
     * of the request (if any) and returns them lightly validated and formatted for a URL
     * (e.g., "page=1&amp;itemsPerPage=1000").
     *
     * @param request
     * @return e.g., "page=1&amp;itemsPerPage=1000" (encoded here for JavaDocs)
     */
    public static String passThroughPIppQuery(HttpServletRequest request) {
        int pipp[] = getRequestedPIpp(request);
        return "page=" + pipp[0] + "&itemsPerPage=" + pipp[1];
    }

    /**
     * This is like passThroughPIppQuery, but always sets page=1.
     *
     * @param request
     * @return e.g., "page=1&amp;itemsPerPage=1000" (encoded here for JavaDocs)
     */
    public static String passThroughPIppQueryPage1(HttpServletRequest request) {
        int pipp[] = getRequestedPIpp(request);
        return "page=1&itemsPerPage=" + pipp[1];
    }

    /**
     * This is like passThroughPIppQuery, but the ampersand is XML encoded so
     * it is ready to be put into HTML.
     *
     * @param request
     * @return e.g., "page=1&amp;amp;itemsPerPage=1000" (doubly encoded here for JavaDocs)
     */
    public static String encodedPassThroughPIppQuery(HttpServletRequest request) {
        int pipp[] = getRequestedPIpp(request);
        return "page=" + pipp[0] + "&amp;itemsPerPage=" + pipp[1];
    }

    /**
     * This is like encodedPassThroughPIppQuery, but always sets page=1.
     *
     * @param request
     * @return e.g., "page=1&amp;amp;itemsPerPage=1000" (doubly encoded here for JavaDocs)
     */
    public static String encodedPassThroughPIppQueryPage1(HttpServletRequest request) {
        int pipp[] = getRequestedPIpp(request);
        return "page=1&amp;itemsPerPage=" + pipp[1];
    }

    /**
     * This calculates the requested (or inferred) page number and itemsPerPage
     * by checking the request parameters.
     *
     * @param request
     * @param nItems e.g., total number of datasets from search results
     * @return int[4]  
     *     [0]=page (will be 1..., but may be too big), 
     *     [1]=itemsPerPage (will be 1...), 
     *     [2]=startIndex (will be 0..., but may be too big),
     *     [3]=lastPage with items (will be 1...).
     *     Note that page may be greater than lastPage (caller should write error message to user).
     */
    public static int[] calculatePIpp(HttpServletRequest request, int nItems) {

        int pipp[] = getRequestedPIpp(request);
        int page = pipp[0];
        int itemsPerPage = pipp[1]; 
        int startIndex = Math2.narrowToInt((page - 1) * (long)itemsPerPage); //0..
        int lastPage = Math.max(1, Math2.hiDiv(nItems, itemsPerPage));

        return new int[]{page, itemsPerPage, startIndex, lastPage};
    }

    /** This returns the error String[2] if a search yielded no matches.
     *
     */
    public static String[] noSearchMatch(String searchFor) {
        if (searchFor == null)
            searchFor = "";
        return new String[] {
            MustBe.THERE_IS_NO_DATA,
            (searchFor.length()     >  0? searchSpelling + " " : "") +
            (searchFor.indexOf(' ') >= 0? searchFewerWords : "")};
    }

    /** 
     * This returns the error String[2] if page &gt; lastPage.
     *
     * @param page
     * @param lastPage
     * @return String[2] with the two Strings
     */
    public static String[] noPage(int page, int lastPage) {
        return new String[]{
            MessageFormat.format(noPage1, "" + page, "" + lastPage),
            MessageFormat.format(noPage2, "" + page, "" + lastPage)};
    }

    /**
     * This returns the nMatchingDatasets HTML message, with paging options.
     *
     * @param nMatches   this must be 1 or more
     * @param page
     * @param lastPage
     * @param relevant true=most relevant first, false=sorted alphabetically
     * @param urlWithQuery percentEncoded, but not HTML/XML encoded, 
     *    e.g., http://coastwatch.pfeg.noaa.gov/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=temperature%20wind
     * @return string with HTML content
     */
    public static String nMatchingDatasetsHtml(int nMatches, int page, int lastPage,
        boolean relevant, String urlWithQuery) {

        if (nMatches == 1)
            return nMatching1;

        StringBuilder results = new StringBuilder(
            MessageFormat.format(
                relevant?
                    nMatchingMostRelevant :
                    nMatchingAlphabetical,
                "" + nMatches) + "\n");

        if (lastPage > 1) {

            //figure out where page number is so replaceable
            int pagePo = urlWithQuery.indexOf("?page=");
            if (pagePo < 0)
                pagePo = urlWithQuery.indexOf("&page="); 
            if (pagePo < 0) {
                pagePo = urlWithQuery.length();
                urlWithQuery += (urlWithQuery.indexOf('?') < 0? "?" : "&") + "page=" + page;                
            }
            int pageNumberPo = pagePo + 6;

            int ampPo = urlWithQuery.indexOf('&', pageNumberPo);
            if (ampPo < 0)
                ampPo = urlWithQuery.length();

            String url1 = "&nbsp;<a href=\"" + 
                          XML.encodeAsHTMLAttribute(urlWithQuery.substring(0, pageNumberPo));  // + p
            String url2 = XML.encodeAsHTMLAttribute(urlWithQuery.substring(ampPo)) + "\">";    // + p   
            String url3 = "</a>&nbsp;\n";        

            //links, e.g. if page=5 and lastPage=12: _1 ... _4  5 _6 ... _12 
            StringBuilder sb = new StringBuilder();
            if (page >= 2)            sb.append(url1 + 1          + url2 + 1          + url3);
            if (page >= 4)            sb.append("...\n");
            if (page >= 3)            sb.append(url1 + (page - 1) + url2 + (page - 1) + url3);
            sb.append("&nbsp;" + page + "&nbsp;(" + EDStatic.nMatchingCurrent + ")&nbsp;\n"); //always show current page
            if (page <= lastPage - 2) sb.append(url1 + (page + 1) + url2 + (page + 1) + url3);
            if (page <= lastPage - 3) sb.append("...\n");
            if (page <= lastPage - 1) sb.append(url1 + lastPage   + url2 + lastPage   + url3);

            //append to results
            results.append("&nbsp;&nbsp;" +
                MessageFormat.format(nMatchingPage, 
                    "" + page, "" + lastPage, sb.toString()) + 
                "\n");
        }

        return results.toString();
    }

    /** If query is null or "", this returns ""; otherwise, this returns "?" + query. */
    public static String questionQuery(String query) {
        return query == null || query.length() == 0? "" : "?" + query;
    }


    /**
     * This indicates if t is a ClientAbortException.
     *
     * @param t the exception
     * @return true if t is a ClientAbortException.
     * @throws Throwable
     */
    public static boolean isClientAbortException(Throwable t) {
        String tString = t.toString();
        return tString.indexOf("ClientAbortException") >= 0;
    }

    /**
     * If t is a ClientAbortException, this will rethrow it.
     * org.apache.catalina.connector.ClientAbortException is hard to catch
     * since catalina code is linked in after deployment.
     * So this looks for the string.
     *
     * Normal use: Use this first thing in catch, before calling 
     * requestReloadASAP and throwing WaitThenTryAgainException.
     *
     * @param t the exception which will be thrown again if it is a ClientAbortException
     * @throws Throwable
     */
    public static void rethrowClientAbortException(Throwable t) throws Throwable {
        if (isClientAbortException(t)) 
            throw t;
    }
    

    /** 
     * This tests some of the methods in this class.
     * @throws an Exception if trouble.
     */
    public static void test() throws Throwable {
        String2.log("\n*** EDStatic.test");
    }

}
