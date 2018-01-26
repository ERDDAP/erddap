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
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import gov.noaa.pfel.erddap.*;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.variable.*;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

//import org.verisign.joid.consumer.OpenIdFilter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

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
     * <br>It's okay if .001 used for minor releases.
     *    Some code deals with it as a double, but never d.dd.
     * <br>1.48 released on 2014-09-04
     * <br>1.50 released on 2014-09-06
     * <br>1.52 released on 2014-10-03
     * <br>1.54 released on 2014-10-24
     * <br>1.56 released on 2014-12-16
     * <br>1.58 released on 2015-02-25
     * <br>1.60 released on 2015-03-12
     * <br>1.62 released on 2015-06-08
     * <br>1.64 released on 2015-08-19
     * <br>1.66 released on 2016-01-19
     * <br>1.68 released on 2016-02-08
     * <br>1.70 released on 2016-04-15
     * <br>1.72 released on 2016-05-12
     * <br>1.74 released on 2016-10-07
     * <br>1.76 released on 2017-05-12
     * <br>1.78 released on 2017-05-27
     * <br>1.80 released on 2017-08-04
     * <br>1.82 released on 2018-01-26
     *
     * For master branch releases, this will be a floating point
     * number with 2 decimal digits, with no additional text. 
     * !!! People other than the main ERDDAP developer (Bob) 
     * should not change the *number* below.
     * If you need to identify a fork of ERDDAP, please append "_" + other 
     * ASCII text (no spaces or control characters) to the number below,
     * e.g., "1.82_MyFork".
     * In a few places in ERDDAP, this string is parsed as a number. 
     * The parser now disregards "_" and anything following it.
     * A request to http.../erddap/version will return just the number (as text).
     * A request to http.../erddap/version_string will return the full string.
     */   
    public static String erddapVersion = "1.82"; //see comment above

    /** 
     * This is almost always false.  
     * During development, Bob sets this to true. No one else needs to. 
     * If true, ERDDAP uses setup2.xml and datasets2.xml (and messages2.xml if it exists). 
     */
public static boolean developmentMode = false;

    /** This identifies the dods server/version that this mimics. */
    public static String dapVersion = "DAP/2.0";   
    public static String serverVersion = "dods/3.7"; //this is what thredds replies
      //drds at https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.ver replies "DODS/3.2"
      //both reply with server version, neither replies with coreVersion
      //spec says #.#.#, but Gallagher says #.# is fine.

    /** 
     * contentDirectory is the local directory on this computer, e.g., [tomcat]/content/erddap/ 
     * It will have a slash at the end.
     */
    public static String contentDirectory;

    public final static String INSTITUTION = "institution";
    public final static int TITLE_DOT_LENGTH = 95; //max nChar before using " ... "

    /* contextDirectory is the local directory on this computer, e.g., [tomcat]/webapps/erddap/ */
    public static String contextDirectory = SSR.getContextDirectory(); //with / separator and / at the end
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
    public static String errorsDuringMajorReload = "";
    public static StringBuffer majorLoadDatasetsTimeSeriesSB = new StringBuffer(""); //thread-safe (1 thread writes but others may read)
    public static HashSet requestBlacklist = null;
    public static volatile int slowDownTroubleMillis = 1000;
    public static long startupMillis = System.currentTimeMillis();
    public static String startupLocalDateTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
    public static int nGridDatasets = 0;  
    public static int nTableDatasets = 0;
    public static long lastMajorLoadDatasetsStartTimeMillis = System.currentTimeMillis();
    public static long lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis() - 1;
    private static ConcurrentHashMap<String,String> sessionNonce = new ConcurrentHashMap(16, 0.75f, 4); //for a session: loggedInAs -> nonce

    /** userHashMap. 
     * key=username (if email address, they are lowercased) 
     * value=[encoded password, sorted roles String[]] 
     * It is empty until the first LoadDatasets is finished and puts a new HashMap in place.
     * It is private so no other code can access the information except
     * via doesPasswordMatch() and getRoles().
     * MD5'd and SHA'd passwords should all already be lowercase.
     * No need to be thread-safe: one thread writes it, then put here where read only.
     */
    private static HashMap userHashMap = new HashMap(); 

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
    public final static String DONT_LOG_THIS_EMAIL     = "!!! DON'T LOG THIS EMAIL: ";

    /** 
     * These values are loaded from the [contentDirectory]setup.xml file. 
     * See comments in the [contentDirectory]setup.xml file.
     */
    public static String 
        baseUrl,
        baseHttpsUrl, //won't be null, may be "(not specified)"
        bigParentDirectory,
        unitTestDataDir,
        unitTestBigDataDir,
         
        adminInstitution,
        adminInstitutionUrl,
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

        //the unencoded EDDGrid...Example attributes
        EDDGridErddapUrlExample,
        EDDGridIdExample,
        EDDGridDimensionExample,
        EDDGridNoHyperExample,
        EDDGridDimNamesExample,
        EDDGridDataTimeExample,
        EDDGridDataValueExample,
        EDDGridDataIndexExample,
        EDDGridGraphExample,
        EDDGridMapExample,
        EDDGridMatlabPlotExample,

        //variants encoded to be Html Examples
        EDDGridDimensionExampleHE,
        EDDGridDataIndexExampleHE,
        EDDGridDataValueExampleHE,
        EDDGridDataTimeExampleHE,
        EDDGridGraphExampleHE,
        EDDGridMapExampleHE,

        //variants encoded to be Html Attributes
        EDDGridDimensionExampleHA,
        EDDGridDataIndexExampleHA,
        EDDGridDataValueExampleHA,
        EDDGridDataTimeExampleHA,
        EDDGridGraphExampleHA,
        EDDGridMapExampleHA,

        //the unencoded EDDTable...Example attributes
        EDDTableErddapUrlExample,
        EDDTableIdExample,
        EDDTableVariablesExample,
        EDDTableConstraintsExample,
        EDDTableDataTimeExample,
        EDDTableDataValueExample,
        EDDTableGraphExample,
        EDDTableMapExample,
        EDDTableMatlabPlotExample,

        //variants encoded to be Html Examples
        EDDTableConstraintsExampleHE,
        EDDTableDataTimeExampleHE,
        EDDTableDataValueExampleHE,
        EDDTableGraphExampleHE,
        EDDTableMapExampleHE,

        //variants encoded to be Html Attributes
        EDDTableConstraintsExampleHA,
        EDDTableDataTimeExampleHA,
        EDDTableDataValueExampleHA,
        EDDTableGraphExampleHA,
        EDDTableMapExampleHA,

        /* For the wcs examples, pick one of your grid datasets that has longitude and latitude axes.
        The sample variable must be a variable in the sample grid dataset.
        The bounding box values are minx,miny,maxx,maxy.
        */
        wcsSampleDatasetID = "jplMURSST41",
        wcsSampleVariable = "analysed_sst",
        wcsSampleBBox = "-179.98,-89.98,179.98,89.98",
        wcsSampleAltitude = "0",
        wcsSampleTime = "2002-06-01T09:00:00Z",

        /* For the wms examples, pick one of your grid datasets that has longitude
        and latitude axes.
        The sample variable must be a variable in the sample grid dataset.
        The bounding box values are minx,miny,maxx,maxy.
        The default for wmsActive is "true".
        */
        wmsSampleDatasetID = "jplMURSST41",
        wmsSampleVariable = "analysed_sst",
        /* The bounding box values are minLongitude,minLatitude,maxLongitude,maxLatitude.
           Longitude values within -180 to 180, or 0 to 360, are now okay. */
        wmsSampleBBox110 = "-179.99,-89.99,180.0,89.99",
        wmsSampleBBox130 = "-89.99,-179.99,89.99,180.0",
        wmsSampleTime = "2002-06-01T09:00:00Z",

        sosFeatureOfInterest,
        sosUrnBase,
        sosBaseGmlName,
        sosStandardNamePrefix,

        authentication,  //will be one of "", "custom" ["openid"]. If baseHttpsUrl doesn't start with https:, this will be "".
        datasetsRegex,
        drawLandMask,
        emailEverythingToCsv, 
        emailDailyReportToCsv,
        emailSubscriptionsFrom,
        flagKeyKey,
        fontFamily,
        googleClientID, //if authentication=google, this will be something
        googleEarthLogoFile,
        highResLogoImageFile,
        legendTitle1, 
        legendTitle2, 
        lowResLogoImageFile,
        passwordEncoding, //will be one of "MD5", "UEPMD5", "SHA256", "UEPSHA256"
        questionMarkImageFile,
        searchEngine,
        warName;
    public static String ampLoginInfo = "&loginInfo;";
    public static String accessibleViaNC4; //"" if accessible, else message why not
    public static int 
        lowResLogoImageFileWidth,  lowResLogoImageFileHeight,
        highResLogoImageFileWidth, highResLogoImageFileHeight,
        googleEarthLogoFileWidth,  googleEarthLogoFileHeight;
    public static Color graphBackgroundColor;
    private static String legal;
    private static int   ampLoginInfoPo = -1;
    /** These are special because other loggedInAs must be String2.justPrintable
        loggedInAsHttps is for using https without being logged in, 
          but &amp;loginInfo; indicates user isn't logged in.
          It is a reserved username -- LoadDatasets prohibits defining a user with that name.
        Tab is useful here: LoadDatasets prohibits it as valid userName, 
          but it won't cause big trouble when printed in tally info.
     */
    public final static String loggedInAsHttps     = "[https]";     //final so not changeable
    public final static String loggedInAsSuperuser = "\tsuperuser"; //final so not changeable
    public final static int minimumPasswordLength = 8;
    private static String startBodyHtml,  endBodyHtml, startHeadHtml; //see xxx() methods

    public static boolean listPrivateDatasets, 
        reallyVerbose,
        subscriptionSystemActive,  convertersActive, slideSorterActive,
        fgdcActive, iso19115Active, jsonldActive, geoServicesRestActive, 
        filesActive, dataProviderFormActive, 
        outOfDateDatasetsActive, politicalBoundariesActive, 
        wmsClientActive, 
        sosActive, wcsActive, wmsActive,
        quickRestart, subscribeToRemoteErddapDataset,
        useOriginalSearchEngine, useLuceneSearchEngine,  //exactly one will be true
        variablesMustHaveIoosCategory,
        verbose;
    public static String  categoryAttributes[];       //as it appears in metadata (and used for hashmap)
    public static String  categoryAttributesInURLs[]; //fileNameSafe (as used in URLs)
    public static boolean categoryIsGlobal[];
    public static int variableNameCategoryAttributeIndex = -1;
    public static int 
        logMaxSizeMB,
        unusualActivity = 10000,
        partialRequestMaxBytes = 490000000, //this is just below tds default <opendap><binLimit> of 500MB
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
        preferredErddapUrl,  //without slash at end   (https if avail, else http)
        fullDatasetDirectory,  //all the Directory's have slash at end
        fullCacheDirectory,
        fullLogsDirectory,
        fullCopyDirectory,
        fullLuceneDirectory,
        fullResetFlagDirectory,
        fullHardFlagDirectory,
        fullCptCacheDirectory,         
        fullPlainFileNcCacheDirectory,         
        fullSgtMapTopographyCacheDirectory,
        fullTestCacheDirectory,
        fullWmsCacheDirectory,

        imageDirUrl,
        imageDirHttpsUrl,
        //downloadDirUrl,
        computerName; //e.g., coastwatch (or "")
    public static Subscriptions subscriptions;


    /** These values are loaded from the [contentDirectory]messages.xml file (if present)
        or .../classes/gov/noaapfel/erddap/util/messages.xml. */
    public static String 
        addConstraints,
        admKeywords,
        admSubsetVariables,
        admSummary,
        admTitle,
        advl_datasetID,
        advc_accessible,
        advl_accessible,
        advl_institution,
        advc_dataStructure,
        advl_dataStructure,
        advr_dataStructure,
        advl_cdm_data_type,
        advr_cdm_data_type,
        advl_class,
        advr_class,
        advl_title,
        advl_minLongitude,
        advl_maxLongitude,
        advl_longitudeSpacing,
        advl_minLatitude,
        advl_maxLatitude,
        advl_latitudeSpacing,
        advl_minAltitude,
        advl_maxAltitude,
        advl_minTime,
        advc_maxTime,
        advl_maxTime,
        advl_timeSpacing,
        advc_griddap,
        advl_griddap,
        advl_subset,
        advc_tabledap,
        advl_tabledap,
        advl_MakeAGraph,
        advc_sos,
        advl_sos,
        advl_wcs,
        advl_wms,
        advc_files,
        advl_files,
        advc_fgdc,
        advl_fgdc,
        advc_iso19115,
        advl_iso19115,
        advc_metadata,
        advl_metadata,
        advl_sourceUrl,
        advl_infoUrl,
        advl_rss,
        advc_email,
        advl_email,
        advl_summary,
        advc_testOutOfDate,
        advl_testOutOfDate,
        advc_outOfDate,
        advl_outOfDate,
        advn_outOfDate,

        advancedSearch,
        advancedSearchResults,
        advancedSearchDirections,
        advancedSearchTooltip,
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
        autoRefresh,
        blacklistMsg,
        categoryTitleHtml,
        category1Html,
        category2Html,
        category3Html,
        categoryPickAttribute,
        categorySearchHtml,
        categorySearchDifferentHtml,
        categoryClickHtml,
        categoryNotAnOption,
        caughtInterrupted,
        clickAccess,
        clickBackgroundInfo,
        clickERDDAP,
        clickInfo,
        clickToSubmit,
        convertOceanicAtmosphericAcronyms,        
        convertOceanicAtmosphericAcronymsIntro,
        convertOceanicAtmosphericAcronymsNotes,
        convertOceanicAtmosphericAcronymsService,
        convertOceanicAtmosphericVariableNames,        
        convertOceanicAtmosphericVariableNamesIntro,
        convertOceanicAtmosphericVariableNamesNotes,
        convertOceanicAtmosphericVariableNamesService,
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
        convertTimeBypass,
        convertTimeReference,
        convertTimeIntro,
        convertTimeNotes,
        convertTimeService,
        convertTimeNumberTooltip,
        convertTimeStringTimeTooltip,
        convertTimeUnitsTooltip,
        convertTimeUnitsHelp,
        convertTimeIsoFormatError,
        convertTimeNoSinceError,
        convertTimeNumberError,
        convertTimeNumericTimeError,
        convertTimeParametersError,
        convertTimeStringFormatError,
        convertTimeTwoTimeError,
        convertTimeUnitsError,
        convertUnits,             
        convertUnitsComparison,   
        convertUnitsFilter,
        convertUnitsIntro,
        convertUnitsNotes,
        convertUnitsService,
        cookiesHelp,
        daf,
        dafGridBypassTooltip,
        dafGridTooltip,
        dafTableBypassTooltip,
        dafTableTooltip,
        dasTitle,
        dataAccessNotAllowed,
        databaseUnableToConnect,
        disabled,
        distinctValuesTooltip,
        doWithGraphs,

        dtAccessible,
        dtAccessibleYes,
        dtAccessibleGraphs,
        dtAccessibleNo,
        dtAccessibleLogIn,
        dtLogIn,
        dtDAF1,
        dtDAF2,
        dtFiles,
        dtMAG,
        dtSOS,
        dtSubset,
        dtWCS,
        dtWMS,

        EDDDatasetID,
        EDDFgdc,
        EDDFgdcMetadata,
        EDDFiles,
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
        EDDGridDownloadDataTooltip,
        EDDGridDimension,
        EDDGridDimensionRanges,
        EDDGridFirst,
        EDDGridLast,
        EDDGridStart,
        EDDGridStop,
        EDDGridStartStopTooltip,
        EDDGridStride,
        EDDGridNValues,
        EDDGridNValuesHtml,
        EDDGridSpacing,
        EDDGridJustOneValue,
        EDDGridEven,
        EDDGridUneven,
        EDDGridDimensionTooltip,
        EDDGridDimensionFirstTooltip,
        EDDGridDimensionLastTooltip,
        EDDGridVarHasDimTooltip,
        EDDGridSSSTooltip,
        EDDGridStartTooltip,
        EDDGridStopTooltip,
        EDDGridStrideTooltip,
        EDDGridSpacingTooltip,
        EDDGridDownloadTooltip,
        EDDGridGridVariableHtml,

        EDDTableConstraints,
        EDDTableTabularDatasetTooltip,
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
        EDDTableNumericConstraintTooltip,
        EDDTableStringConstraintTooltip,
        EDDTableTimeConstraintTooltip,
        EDDTableConstraintTooltip,
        EDDTableSelectConstraintTooltip,
        EDDTableDapDescription,
        EDDTableDapLongDescription,
        EDDTableDownloadDataTooltip,

        errorTitle,
        errorRequestUrl,
        errorRequestQuery,
        errorTheError,
        errorCopyFrom,
        errorFileNotFound,
        errorFileNotFoundImage,
        errorInternal,
        errorJsonpFunctionName,
        errorJsonpNotAllowed,
        errorMoreThan2GB,
        errorNotFound,
        errorNotFoundIn,
        errorOdvLLTGrid,
        errorOdvLLTTable,
        errorOnWebPage,
        errorXWasntSpecified,
        errorXWasTooLong,
        externalLink,
        externalWebSite,
        fileHelp_asc,
        fileHelp_csv,
        fileHelp_csvp,
        fileHelp_csv0,
        fileHelp_das,
        fileHelp_dds,
        fileHelp_dods,
        fileHelpGrid_esriAscii,
        fileHelpTable_esriCsv,
        fileHelp_fgdc,
        fileHelp_geoJson,
        fileHelp_graph,
        fileHelpGrid_help,
        fileHelpTable_help,
        fileHelp_html,
        fileHelp_htmlTable,
        fileHelp_iso19115,
        fileHelp_itxGrid,
        fileHelp_itxTable,
        fileHelp_json,
        fileHelp_jsonlCSV,
        fileHelp_jsonlKVP,
        fileHelp_mat,
        fileHelpGrid_nc3,
        fileHelpGrid_nc4,
        fileHelpTable_nc3,
        fileHelpTable_nc4,
        fileHelp_nc3Header,
        fileHelp_nc4Header,
        fileHelp_nccsv,
        fileHelp_nccsvMetadata,
        fileHelp_ncCF,
        fileHelp_ncCFHeader,
        fileHelp_ncCFMA,
        fileHelp_ncCFMAHeader,
        fileHelp_ncml,
        fileHelp_ncoJson,
        fileHelpGrid_odvTxt,
        fileHelpTable_odvTxt,
        fileHelp_subset,
        fileHelp_timeGaps,
        fileHelp_tsv,
        fileHelp_tsvp,
        fileHelp_tsv0,
        fileHelp_wav,
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
        filesDescription,
        filesDocumentation,
        filesSort,
        filesWarning,
        functions,
        functionTooltip,
        functionDistinctCheck,
        functionDistinctTooltip,
        functionOrderByExtra,
        functionOrderByTooltip,
        functionOrderBySort,
        functionOrderBySort1,
        functionOrderBySort2,
        functionOrderBySort3,
        functionOrderBySort4,
        functionOrderBySortLeast,
        functionOrderBySortRowMax,
        generatedAt,
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
        indexAllDatasetsSearch,        
        indexOpenSearch,        
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
        inotifyFix,
        justGenerateAndView,
        justGenerateAndViewTooltip,
        justGenerateAndViewUrl,
        justGenerateAndViewGraphUrlTooltip,
        license,
        listAll,
        listOfDatasets,
        LogIn,
        login,
        loginAttemptBlocked,
        loginDescribeCustom,
        loginDescribeEmail,
        loginDescribeGoogle,
        loginDescribeOpenID,
        loginCanNot,
        loginAreNot,
        loginToLogIn,
        loginEmailAddress,
        loginYourEmailAddress,
        loginUserName,
        loginPassword,
        loginUserNameAndPassword,
        loginGoogleSignIn,
        loginGoogleErddap,
        loginOpenID,
        loginOpenIDOr,
        loginOpenIDCreate,
        loginOpenIDFree,
        loginOpenIDSame,
        loginAs,
        loginFailed,
        loginSucceeded,
        loginInvalid,
        loginNot,
        loginBack,
        loginProblemExact,
        loginProblemExpire,
        loginProblemGoogleAgain,
        loginProblemSameBrowser,
        loginProblem3Times,
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
        magGSYAscendingTooltip,
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
        magGridTooltip,
        magTableTooltip,        
        metadataDownload,
        moreInformation,
        nMatching1,
        nMatching,
        nMatchingAlphabetical,
        nMatchingMostRelevant,
        nMatchingPage,
        nMatchingCurrent,
        noDataFixedValue,
        noDataNoLL,
        noDatasetWith,
        noPage1,
        noPage2,
        notAllowed,
        notAuthorized,
        notAuthorizedForData,
        notAvailable,
        noXxx,
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
        noXxxNoSubsetVariables,
        noXxxNoOLLSubsetVariables,
        noXxxNoMinMax,
        noXxxItsGridded,
        noXxxItsTabular,
        optional,
        options,
        orRefineSearchWith,
        orSearchWith,
        orComma,
        outOfDateHtml,
        palettes[],
        palettes0[],
        paletteSections[] = {
            "","2","3","4","5","6","7","8","9",
            "10","11","12","13","14","15","16","17","18","19",
            "20","21","22","23","24","25","26","27","28","29",
            "30","31","32","33","34","35","36","37","38","39", "40"},
        patientData,
        patientYourGraph,
        percentEncode,
        pickADataset,
        protocolSearchHtml,
        protocolSearch2Html,
        protocolClick,

        queryError,
        queryError180,
        queryError1Value,
        queryError1Var,
        queryError2Var,
        queryErrorActualRange,
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
        queryErrorOrderByVariable,
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
        rssNo,
        searchTitle,
        searchDoFullTextHtml,
        searchFullTextHtml,
        searchHintsLuceneTooltip, 
        searchHintsOriginalTooltip, 
        searchHintsTooltip, 
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
        sosDescriptionHtml,
        sosLongDescriptionHtml,
        sparqlP01toP02pre,
        sparqlP01toP02post,
        ssUse,
        ssUsePlain,
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
        statusHtml,
        submit,
        submitTooltip, 
        subscriptionsTitle,
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
        subscription0Html, 
        subscription1Html, 
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

        tabledapVideoIntro,
        Then,
        unknownDatasetID,
        unknownProtocol,
        unsupportedFileType,
        updateUrlsFrom[],
        updateUrlsTo[],
        updateUrlsSkipAttributes[],
        viewAllDatasetsHtml,
        waitThenTryAgain,
        warning,

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
    
    /** These are only created/used by GenerateDatasetsXml threads. 
     *  See the related methods below that create them.
     */
    private static Table gdxAcronymsTable; 
    private static HashMap<String,String> gdxAcronymsHashMap, gdxVariableNamesHashMap; 

    /** This static block reads this class's static String values from
     * contentDirectory, which must contain setup.xml and datasets.xml 
     * (and may contain messages.xml).
     * It may be a defined environment variable ("erddapContentDirectory")
     * or a subdir of <tomcat> (e.g., usr/local/tomcat/content/erddap/)
     *   (more specifically, a sibling of 'tomcat'/webapps).
     *
     * @throws RuntimeException if trouble
     */
    static {

    String erdStartup = "ERD Low Level Startup";
    String errorInMethod = "";
    try {

        //route calls to a logger to com.cohort.util.String2Log
        String2.setupCommonsLogging(-1);

        String eol = String2.lineSeparator;
        String2.log(eol + "////**** " + erdStartup + eol +
            "localTime=" + Calendar2.getCurrentISODateTimeStringLocalTZ() + eol +
            String2.standardHelpAboutMessage());

        //**** find contentDirectory
        String ecd = "erddapContentDirectory"; //the name of the environment variable
        errorInMethod = 
            "Couldn't find 'content' directory ([tomcat]/content/erddap/ ?) " +
            "because '" + ecd + "' environment variable not found " +
            "and couldn't find '/webapps/' in classPath=" + 
            String2.getClassPath() + //with / separator and / at the end
            " (and 'content/erddap' should be a sibling of <tomcat>/webapps): ";
        contentDirectory = System.getProperty(ecd);        
        if (contentDirectory == null) {
            //Or, it must be sibling of webapps
            //e.g., c:/programs/_tomcat/webapps/erddap/WEB-INF/classes/[these classes]
            //On windows, contentDirectory may have spaces as %20(!)
            contentDirectory = String2.replaceAll(
                String2.getClassPath(), //with / separator and / at the end
                "%20", " "); 
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
        errorInMethod = "ERROR while reading " + setupFileName + ": ";
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
        File2.verbose = verbose;
        FilledMarkerRenderer.verbose = verbose;
        gov.noaa.pfel.coastwatch.griddata.Grid.verbose = verbose;
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
        File2.reallyVerbose = reallyVerbose;
        FilledMarkerRenderer.reallyVerbose = reallyVerbose;
        GridDataAccessor.reallyVerbose = reallyVerbose;
        GSHHS.reallyVerbose = reallyVerbose;
        LoadDatasets.reallyVerbose = reallyVerbose;
        NcHelper.reallyVerbose = reallyVerbose;
        PathCartesianRenderer.reallyVerbose = reallyVerbose;
        SgtGraph.reallyVerbose = reallyVerbose;
        SgtMap.reallyVerbose = reallyVerbose;
        SgtUtil.reallyVerbose = reallyVerbose;
        SSR.reallyVerbose = reallyVerbose;
        Subscriptions.reallyVerbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;
        //Table.debug = reallyVerbose; //for debugging
        TaskThread.reallyVerbose = reallyVerbose;

        bigParentDirectory = setup.getNotNothingString("bigParentDirectory", ""); 
        bigParentDirectory = File2.addSlash(bigParentDirectory);
        Test.ensureTrue(File2.isDirectory(bigParentDirectory),  
            "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");
        unitTestDataDir    = setup.getString("unitTestDataDir",    "[specify <unitTestDataDir> in setup.xml]"); 
        unitTestBigDataDir = setup.getString("unitTestBigDataDir", "[specify <unitTestBigDataDir> in setup.xml]"); 
        unitTestDataDir    = File2.addSlash(unitTestDataDir);
        unitTestBigDataDir = File2.addSlash(unitTestBigDataDir);
        String2.unitTestDataDir    = unitTestDataDir;
        String2.unitTestBigDataDir = unitTestBigDataDir;

        //email  (do early on so email can be sent if trouble later in this method)
        emailSmtpHost          = setup.getString("emailSmtpHost",  null);
        emailSmtpPort          = setup.getInt(   "emailSmtpPort",  25);
        emailUserName          = setup.getString("emailUserName",  null);
        emailPassword          = setup.getString("emailPassword",  null);
        emailProperties        = setup.getString("emailProperties",  null);
        emailFromAddress       = setup.getString("emailFromAddress", null);
        emailEverythingToCsv   = setup.getString("emailEverythingTo", "");  //won't be null
        emailDailyReportToCsv  = setup.getString("emailDailyReportTo", ""); //won't be null

        String tsar[] = String2.split(emailEverythingToCsv, ',');
        if (emailEverythingToCsv.length() > 0)
            for (int i = 0; i < tsar.length; i++)
                if (!String2.isEmailAddress(tsar[i]) || tsar[i].startsWith("your.")) //prohibit the default email addresses
                    throw new RuntimeException("setup.xml error: invalid email address=" + tsar[i] + 
                        " in <emailEverythingTo>.");  
        emailSubscriptionsFrom = tsar.length > 0? tsar[0] : ""; //won't be null

        tsar = String2.split(emailDailyReportToCsv, ',');
        if (emailDailyReportToCsv.length() > 0)
            for (int i = 0; i < tsar.length; i++)
                if (!String2.isEmailAddress(tsar[i]) || tsar[i].startsWith("your.")) //prohibit the default email addresses
                    throw new RuntimeException("setup.xml error: invalid email address=" + tsar[i] + 
                        " in <emailDailyReportTo>.");  

        //test of email
        //Test.error("This is a test of emailing an error in Erddap constructor.");

        //2014-09-03 deleting all cache and public files was moved from here to ERDDAP constructor

        //*** set up directories  //all with slashes at end
        //before 2011-12-30, was fullDatasetInfoDirectory datasetInfo/; see conversion below
        fullDatasetDirectory     = bigParentDirectory + "dataset/";  
        fullCacheDirectory       = bigParentDirectory + "cache/";
        fullResetFlagDirectory   = bigParentDirectory + "flag/";
        fullHardFlagDirectory    = bigParentDirectory + "hardFlag/";
        fullLogsDirectory        = bigParentDirectory + "logs/";
        fullCopyDirectory        = bigParentDirectory + "copy/";
        fullLuceneDirectory      = bigParentDirectory + "lucene/";

        Test.ensureTrue(File2.isDirectory(fullPaletteDirectory),  
            "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
        errorInMethod = "ERROR while creating directories: "; //File2.makeDir throws exception if failure
        File2.makeDirectory(fullPublicDirectory);  //make it, because Git doesn't track empty dirs
        File2.makeDirectory(fullDatasetDirectory); 
        File2.makeDirectory(fullCacheDirectory);
        File2.makeDirectory(fullResetFlagDirectory);
        File2.makeDirectory(fullHardFlagDirectory);
        File2.makeDirectory(fullLogsDirectory);
        File2.makeDirectory(fullCopyDirectory);
        File2.makeDirectory(fullLuceneDirectory);

        String2.log(
            "logLevel=" + logLevel + ": verbose=" + verbose + " reallyVerbose=" + reallyVerbose + eol +
            "bigParentDirectory=" + bigParentDirectory + eol +
            "contextDirectory=" + contextDirectory);

        //are bufferedImages hardware accelerated?
        String2.log(SgtUtil.isBufferedImageAccelerated());

        //2011-12-30 convert /datasetInfo/[datasetID]/ to 
        //                   /dataset/[last2char]/[datasetID]/
        //to prepare for huge number of datasets
        String oldBaseDir = bigParentDirectory + "datasetInfo/";   //the old name
        if (File2.isDirectory(oldBaseDir)) {
            errorInMethod = "ERROR while converting from oldBaseDir=" + oldBaseDir + ": ";
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
                        if (!ofName.matches(".*[0-9]{7}"))  //skip temp files
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

        //make some subdirectories of fullCacheDirectory
        //'_' distinguishes from dataset cache dirs
        errorInMethod = "ERROR while creating directories: ";
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
       

        //get other info from setup.xml
        errorInMethod = "ERROR while reading " + setupFileName + ": ";
        baseUrl                    = setup.getNotNothingString("baseUrl",                    errorInMethod);
        baseHttpsUrl               = setup.getString(          "baseHttpsUrl",               "(not specified)"); //not "" (to avoid relative urls)
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

        String wmsActiveString     = setup.getString("wmsActive",                  ""); 
        wmsActive                  = String2.isSomething(wmsActiveString)? String2.parseBoolean(wmsActiveString) : true;
        wmsSampleDatasetID         = setup.getString("wmsSampleDatasetID",         wmsSampleDatasetID);
        wmsSampleVariable          = setup.getString("wmsSampleVariable",          wmsSampleVariable);
        wmsSampleBBox110           = setup.getString("wmsSampleBBox110",           wmsSampleBBox110);
        wmsSampleBBox130           = setup.getString("wmsSampleBBox130",           wmsSampleBBox130);
        wmsSampleTime              = setup.getString("wmsSampleTime",              wmsSampleTime);

        adminInstitution           = setup.getNotNothingString("adminInstitution",           errorInMethod);
        adminInstitutionUrl        = setup.getNotNothingString("adminInstitutionUrl",        errorInMethod);
        adminIndividualName        = setup.getNotNothingString("adminIndividualName",        errorInMethod);
        adminPosition              = setup.getNotNothingString("adminPosition",              errorInMethod);
        adminPhone                 = setup.getNotNothingString("adminPhone",                 errorInMethod); 
        adminAddress               = setup.getNotNothingString("adminAddress",               errorInMethod);
        adminCity                  = setup.getNotNothingString("adminCity",                  errorInMethod);
        adminStateOrProvince       = setup.getNotNothingString("adminStateOrProvince",       errorInMethod); 
        adminPostalCode            = setup.getNotNothingString("adminPostalCode",            errorInMethod);
        adminCountry               = setup.getNotNothingString("adminCountry",               errorInMethod);
        adminEmail                 = setup.getNotNothingString("adminEmail",                 errorInMethod);

        if (adminInstitution.startsWith("Your"))
            throw new RuntimeException("setup.xml error: invalid <adminInstitution>=" + adminInstitution);  
        if (!adminInstitutionUrl.startsWith("http") || !String2.isUrl(adminInstitutionUrl))
            throw new RuntimeException("setup.xml error: invalid <adminInstitutionUrl>=" + adminInstitutionUrl);  
        if (adminIndividualName.startsWith("Your"))
            throw new RuntimeException("setup.xml error: invalid <adminIndividualName>=" + adminIndividualName);             
        //if (adminPosition.length() == 0)
        //    throw new RuntimeException("setup.xml error: invalid <adminPosition>=" + adminPosition);             
        if (adminPhone.indexOf("999-999") >= 0)
            throw new RuntimeException("setup.xml error: invalid <adminPhone>=" + adminPhone);              
        if (adminAddress.equals("123 Main St."))
            throw new RuntimeException("setup.xml error: invalid <adminAddress>=" + adminAddress);  
        if (adminCity.equals("Some Town"))
            throw new RuntimeException("setup.xml error: invalid <adminCity>=" + adminCity);  
        //if (adminStateOrProvince.length() == 0)
        //    throw new RuntimeException("setup.xml error: invalid <adminStateOrProvince>=" + adminStateOrProvince);  
        if (adminPostalCode.equals("99999"))
            throw new RuntimeException("setup.xml error: invalid <adminPostalCode>=" + adminPostalCode);  
        //if (adminCountry.length() == 0)
        //    throw new RuntimeException("setup.xml error: invalid <adminCountry>=" + adminCountry);  
        if (!String2.isEmailAddress(adminEmail) || adminEmail.startsWith("your.")) 
            throw new RuntimeException("setup.xml error: invalid <adminEmail>=" + adminEmail);  

        accessConstraints          = setup.getNotNothingString("accessConstraints",          errorInMethod); 
        accessRequiresAuthorization= setup.getNotNothingString("accessRequiresAuthorization",errorInMethod); 
        fees                       = setup.getNotNothingString("fees",                       errorInMethod);
        keywords                   = setup.getNotNothingString("keywords",                   errorInMethod);
        legal                      = setup.getNotNothingString("legal",                      errorInMethod);

        units_standard             = setup.getString(          "units_standard",             "UDUNITS");

        fgdcActive                 = setup.getBoolean(         "fgdcActive",                 true); 
        iso19115Active             = setup.getBoolean(         "iso19115Active",             true); 
        jsonldActive               = setup.getBoolean(         "jsonldActive",               true); 
//until geoServicesRest is finished, it is always inactive
geoServicesRestActive      = false; //setup.getBoolean(         "geoServicesRestActive",      false); 
        filesActive                = setup.getBoolean(         "filesActive",                true); 
        dataProviderFormActive     = setup.getBoolean(         "dataProviderFormActive",     true); 
        outOfDateDatasetsActive    = setup.getBoolean(         "outOfDateDatasetsActive",    true); 
        politicalBoundariesActive  = setup.getBoolean(         "politicalBoundariesActive",  true); 
        wmsClientActive            = setup.getBoolean(         "wmsClientActive",            true); 
        SgtMap.drawPoliticalBoundaries = politicalBoundariesActive;

//until SOS is finished, it is always inactive
sosActive = false;//        sosActive                  = setup.getBoolean(         "sosActive",                  false); 
        if (sosActive) {
            sosFeatureOfInterest   = setup.getNotNothingString("sosFeatureOfInterest",       errorInMethod);
            sosStandardNamePrefix  = setup.getNotNothingString("sosStandardNamePrefix",      errorInMethod);
            sosUrnBase             = setup.getNotNothingString("sosUrnBase",                 errorInMethod);

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
wcsActive = false; //setup.getBoolean(         "wcsActive",                  false); 

        authentication             = setup.getString(          "authentication",             "");
        datasetsRegex              = setup.getString(          "datasetsRegex",              ".*");
        drawLandMask               = setup.getString(          "drawLandMask",               null);
        if (drawLandMask == null) //2014-08-28 changed defaults below to "under". It will be in v1.48
            drawLandMask           = setup.getString(          "drawLand",                   "under"); 
        if (!drawLandMask.equals("under") && 
            !drawLandMask.equals("over"))
             drawLandMask = "under"; //default
        flagKeyKey                 = setup.getNotNothingString("flagKeyKey",                 errorInMethod);
        if (flagKeyKey.toUpperCase().indexOf("CHANGE THIS") >= 0)
              //really old default: "A stitch in time saves nine. CHANGE THIS!!!"  
              //current default:    "CHANGE THIS TO YOUR FAVORITE QUOTE"
            throw new RuntimeException(
            String2.ERROR + ": You must change the <flagKeyKey> in setup.xml to a new, unique, non-default value. " +
            "NOTE that this will cause the flagKeys used by your datasets to change. " +
            "Any subscriptions using the old flagKeys will need to be redone.");
        fontFamily                 = setup.getString(          "fontFamily",                 "SansSerif");
        graphBackgroundColor = new Color(String2.parseInt(
                                     setup.getString(          "graphBackgroundColor",       "0xffccccff")), true); //hasAlpha
        googleClientID             = setup.getString(          "googleClientID",             null);
        googleEarthLogoFile        = setup.getNotNothingString("googleEarthLogoFile",        errorInMethod);
        highResLogoImageFile       = setup.getNotNothingString("highResLogoImageFile",       errorInMethod);
        legendTitle1               = setup.getString(          "legendTitle1",               null);
        legendTitle2               = setup.getString(          "legendTitle2",               null);
        listPrivateDatasets        = setup.getBoolean(         "listPrivateDatasets",        false);
        loadDatasetsMinMillis      = Math.max(1,setup.getInt(  "loadDatasetsMinMinutes",     15)) * 60000L;
        loadDatasetsMaxMillis      = setup.getInt(             "loadDatasetsMaxMinutes",     60) * 60000L;
        loadDatasetsMaxMillis      = Math.max(loadDatasetsMinMillis * 2, loadDatasetsMaxMillis);
        logMaxSizeMB               = Math2.minMax(1, 2000, setup.getInt("logMaxSizeMB", 20));  //2048MB=2GB
        lowResLogoImageFile        = setup.getNotNothingString("lowResLogoImageFile",        errorInMethod);
        partialRequestMaxBytes     = setup.getInt(             "partialRequestMaxBytes",     partialRequestMaxBytes);
        partialRequestMaxCells     = setup.getInt(             "partialRequestMaxCells",     partialRequestMaxCells);
        questionMarkImageFile      = setup.getNotNothingString("questionMarkImageFile",      errorInMethod);
        quickRestart               = setup.getBoolean(         "quickRestart",               true);      
        passwordEncoding           = setup.getString(          "passwordEncoding",           "UEPSHA256");
        searchEngine               = setup.getString(          "searchEngine",               "original");

        subscribeToRemoteErddapDataset = setup.getBoolean(     "subscribeToRemoteErddapDataset", true);
        subscriptionSystemActive   = setup.getBoolean(         "subscriptionSystemActive",   true);
        convertersActive           = setup.getBoolean(         "convertersActive",           true);
        slideSorterActive          = setup.getBoolean(         "slideSorterActive",          true);
        theShortDescriptionHtml    = setup.getNotNothingString("theShortDescriptionHtml",    errorInMethod);
        unusualActivity            = setup.getInt(             "unusualActivity",            unusualActivity);
        variablesMustHaveIoosCategory = setup.getBoolean(      "variablesMustHaveIoosCategory", true);
        warName                    = setup.getString(          "warName",                    "erddap");

        //use Lucence?
        if (searchEngine.equals("lucene")) {
            useLuceneSearchEngine = true;
        } else {
            Test.ensureEqual(searchEngine, "original", 
                "<searchEngine> must be \"original\" (the default) or \"lucene\".");
            useOriginalSearchEngine = true;
        }
        
       
        errorInMethod = "ERROR while initializing SgtGraph: ";
        sgtGraph = new SgtGraph(fontFamily);

        //ensure erddapVersion is okay
        int upo = erddapVersion.indexOf('_');
        double ev = String2.parseDouble(upo >= 0? erddapVersion.substring(0, upo) :
            erddapVersion);
        if (upo == -1 && erddapVersion.length() == 4 && ev > 1.8 && ev < 10) {} //it's just a number
        else if ((upo != -1 && upo != 4) ||
            ev <= 1.8 || ev >=10 || Double.isNaN(ev) || 
            erddapVersion.indexOf(' ') >= 0 ||
            !String2.isAsciiPrintable(erddapVersion))
            throw new SimpleException(
                "The format of EDStatic.erddapVersion must be d.dd[_someAsciiText]. (ev=" + ev + ")");            

        //ensure authentication setup is okay
        errorInMethod = "ERROR while checking authentication setup: ";
        if (authentication == null)
            authentication = "";
        authentication = authentication.trim().toLowerCase();
        if (!authentication.equals("") &&
            !authentication.equals("custom") &&
            !authentication.equals("email")  &&
            !authentication.equals("google") 
            //&& !authentication.equals("openid")  //OUT-OF-DATE
            )
            throw new RuntimeException(
                "setup.xml error: authentication=" + authentication + 
                " must be (nothing)|custom|google|email.");  //was also "openid"  //google NOT FINISHED
        if (!authentication.equals("") && !baseHttpsUrl.startsWith("https://"))
            throw new RuntimeException(
                "setup.xml error: " + 
                ": For any <authentication> other than \"\", the baseHttpsUrl=" + baseHttpsUrl + 
                " must start with \"https://\".");
        if (authentication.equals("google") && !String2.isSomething(googleClientID))
            throw new RuntimeException(
                "setup.xml error: " + 
                ": When authentication=google, you must provide your <googleClientID>.");
        if (authentication.equals("custom") &&
            (!passwordEncoding.equals("MD5") &&
             !passwordEncoding.equals("UEPMD5") &&
             !passwordEncoding.equals("SHA256") &&
             !passwordEncoding.equals("UEPSHA256")))
            throw new RuntimeException(
                "setup.xml error: When authentication=custom, passwordEncoding=" + passwordEncoding + 
                " must be MD5|UEPMD5|SHA256|UEPSHA256.");  
        //String2.log("authentication=" + authentication);


        //things set as a consequence of setup.xml
        erddapUrl          = baseUrl        + "/" + warName;
        erddapHttpsUrl     = baseHttpsUrl   + "/" + warName;
        preferredErddapUrl = baseHttpsUrl.startsWith("https://")? erddapHttpsUrl : erddapUrl;
        imageDirUrl        = erddapUrl      + "/" + IMAGES_DIR;   
        imageDirHttpsUrl   = erddapHttpsUrl + "/" + IMAGES_DIR;   
        //downloadDirUrl   = erddapUrl      + "/" + DOWNLOAD_DIR;  //if uncommented, you need downloadDirHttpsUrl too

        //???if logoImgTag is needed, convert to method logoImgTag(loggedInAs)
        //logoImgTag = "      <img src=\"" + imageDirUrl(loggedInAs) + lowResLogoImageFile + "\" " +
        //    "alt=\"logo\" title=\"logo\">\n";


        
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
            messagesFileName = String2.getClassPath() + //with / separator and / at the end
                "gov/noaa/pfel/erddap/util/messages.xml";
            String2.log("Using default messages.xml from  " + messagesFileName);
        }
        errorInMethod = "ERROR while reading messages.xml: ";
        ResourceBundle2 messages = ResourceBundle2.fromXml(XML.parseXml(messagesFileName, false));


        //read all the static Strings from messages.xml
        addConstraints             = messages.getNotNothingString("addConstraints",             errorInMethod);
        admKeywords                = messages.getNotNothingString("admKeywords",                errorInMethod);
        admSubsetVariables         = messages.getNotNothingString("admSubsetVariables",         errorInMethod);
        admSummary                 = messages.getNotNothingString("admSummary",                 errorInMethod);
        admTitle                   = messages.getNotNothingString("admTitle",                   errorInMethod);
        advl_datasetID             = messages.getNotNothingString("advl_datasetID",             errorInMethod);
        advc_accessible            = messages.getNotNothingString("advc_accessible",            errorInMethod);
        advl_accessible            = messages.getNotNothingString("advl_accessible",            errorInMethod);
        advl_institution           = messages.getNotNothingString("advl_institution",           errorInMethod);
        advc_dataStructure         = messages.getNotNothingString("advc_dataStructure",         errorInMethod);
        advl_dataStructure         = messages.getNotNothingString("advl_dataStructure",         errorInMethod);
        advr_dataStructure         = messages.getNotNothingString("advr_dataStructure",         errorInMethod);
        advl_cdm_data_type         = messages.getNotNothingString("advl_cdm_data_type",         errorInMethod);
        advr_cdm_data_type         = messages.getNotNothingString("advr_cdm_data_type",         errorInMethod);
        advl_class                 = messages.getNotNothingString("advl_class",                 errorInMethod);
        advr_class                 = messages.getNotNothingString("advr_class",                 errorInMethod);
        advl_title                 = messages.getNotNothingString("advl_title",                 errorInMethod);
        advl_minLongitude          = messages.getNotNothingString("advl_minLongitude",          errorInMethod);
        advl_maxLongitude          = messages.getNotNothingString("advl_maxLongitude",          errorInMethod);
        advl_longitudeSpacing      = messages.getNotNothingString("advl_longitudeSpacing",      errorInMethod);
        advl_minLatitude           = messages.getNotNothingString("advl_minLatitude",           errorInMethod);
        advl_maxLatitude           = messages.getNotNothingString("advl_maxLatitude",           errorInMethod);
        advl_latitudeSpacing       = messages.getNotNothingString("advl_latitudeSpacing",       errorInMethod);
        advl_minAltitude           = messages.getNotNothingString("advl_minAltitude",           errorInMethod);
        advl_maxAltitude           = messages.getNotNothingString("advl_maxAltitude",           errorInMethod);
        advl_minTime               = messages.getNotNothingString("advl_minTime",               errorInMethod);
        advc_maxTime               = messages.getNotNothingString("advc_maxTime",               errorInMethod);
        advl_maxTime               = messages.getNotNothingString("advl_maxTime",               errorInMethod);
        advl_timeSpacing           = messages.getNotNothingString("advl_timeSpacing",           errorInMethod);
        advc_griddap               = messages.getNotNothingString("advc_griddap",               errorInMethod);
        advl_griddap               = messages.getNotNothingString("advl_griddap",               errorInMethod);
        advl_subset                = messages.getNotNothingString("advl_subset",                errorInMethod);
        advc_tabledap              = messages.getNotNothingString("advc_tabledap",              errorInMethod);
        advl_tabledap              = messages.getNotNothingString("advl_tabledap",              errorInMethod);
        advl_MakeAGraph            = messages.getNotNothingString("advl_MakeAGraph",            errorInMethod);
        advc_sos                   = messages.getNotNothingString("advc_sos",                   errorInMethod);
        advl_sos                   = messages.getNotNothingString("advl_sos",                   errorInMethod);
        advl_wcs                   = messages.getNotNothingString("advl_wcs",                   errorInMethod);
        advl_wms                   = messages.getNotNothingString("advl_wms",                   errorInMethod);
        advc_files                 = messages.getNotNothingString("advc_files",                 errorInMethod);
        advl_files                 = messages.getNotNothingString("advl_files",                 errorInMethod);
        advc_fgdc                  = messages.getNotNothingString("advc_fgdc",                  errorInMethod);
        advl_fgdc                  = messages.getNotNothingString("advl_fgdc",                  errorInMethod);
        advc_iso19115              = messages.getNotNothingString("advc_iso19115",              errorInMethod);
        advl_iso19115              = messages.getNotNothingString("advl_iso19115",              errorInMethod);
        advc_metadata              = messages.getNotNothingString("advc_metadata",              errorInMethod);
        advl_metadata              = messages.getNotNothingString("advl_metadata",              errorInMethod);
        advl_sourceUrl             = messages.getNotNothingString("advl_sourceUrl",             errorInMethod);
        advl_infoUrl               = messages.getNotNothingString("advl_infoUrl",               errorInMethod);
        advl_rss                   = messages.getNotNothingString("advl_rss",                   errorInMethod);
        advc_email                 = messages.getNotNothingString("advc_email",                 errorInMethod);
        advl_email                 = messages.getNotNothingString("advl_email",                 errorInMethod);
        advl_summary               = messages.getNotNothingString("advl_summary",               errorInMethod);
        advc_testOutOfDate         = messages.getNotNothingString("advc_testOutOfDate",             errorInMethod);
        advl_testOutOfDate         = messages.getNotNothingString("advl_testOutOfDate",             errorInMethod);
        advc_outOfDate             = messages.getNotNothingString("advc_outOfDate",             errorInMethod);
        advl_outOfDate             = messages.getNotNothingString("advl_outOfDate",             errorInMethod);
        advn_outOfDate             = messages.getNotNothingString("advn_outOfDate",             errorInMethod);
        advancedSearch             = messages.getNotNothingString("advancedSearch",             errorInMethod);
        advancedSearchResults      = messages.getNotNothingString("advancedSearchResults",      errorInMethod);
        advancedSearchDirections   = messages.getNotNothingString("advancedSearchDirections",   errorInMethod);
        advancedSearchTooltip      = messages.getNotNothingString("advancedSearchTooltip",      errorInMethod);
        advancedSearchBounds       = messages.getNotNothingString("advancedSearchBounds",       errorInMethod);
        advancedSearchMinLat       = messages.getNotNothingString("advancedSearchMinLat",       errorInMethod);
        advancedSearchMaxLat       = messages.getNotNothingString("advancedSearchMaxLat",       errorInMethod);
        advancedSearchMinLon       = messages.getNotNothingString("advancedSearchMinLon",       errorInMethod);
        advancedSearchMaxLon       = messages.getNotNothingString("advancedSearchMaxLon",       errorInMethod);
        advancedSearchMinMaxLon    = messages.getNotNothingString("advancedSearchMinMaxLon",    errorInMethod);
        advancedSearchMinTime      = messages.getNotNothingString("advancedSearchMinTime",      errorInMethod);
        advancedSearchMaxTime      = messages.getNotNothingString("advancedSearchMaxTime",      errorInMethod);
        advancedSearchClear        = messages.getNotNothingString("advancedSearchClear",        errorInMethod);
        advancedSearchClearHelp    = messages.getNotNothingString("advancedSearchClearHelp",    errorInMethod);
        advancedSearchCategoryTooltip = messages.getNotNothingString("advancedSearchCategoryTooltip", errorInMethod);
        advancedSearchRangeTooltip = messages.getNotNothingString("advancedSearchRangeTooltip", errorInMethod);
        advancedSearchMapTooltip   = messages.getNotNothingString("advancedSearchMapTooltip",   errorInMethod);
        advancedSearchLonTooltip   = messages.getNotNothingString("advancedSearchLonTooltip",   errorInMethod);
        advancedSearchTimeTooltip  = messages.getNotNothingString("advancedSearchTimeTooltip",  errorInMethod);
        advancedSearchWithCriteria = messages.getNotNothingString("advancedSearchWithCriteria", errorInMethod);
        advancedSearchFewerCriteria= messages.getNotNothingString("advancedSearchFewerCriteria",errorInMethod);
        advancedSearchNoCriteria   = messages.getNotNothingString("advancedSearchNoCriteria",   errorInMethod);
        autoRefresh                = messages.getNotNothingString("autoRefresh",                errorInMethod);
        blacklistMsg               = messages.getNotNothingString("blacklistMsg",               errorInMethod);
        PrimitiveArray.ArrayAddN           = messages.getNotNothingString("ArrayAddN",          errorInMethod);
        PrimitiveArray.ArrayAppendTables   = messages.getNotNothingString("ArrayAppendTables",  errorInMethod);
        PrimitiveArray.ArrayAtInsert       = messages.getNotNothingString("ArrayAtInsert",      errorInMethod);
        PrimitiveArray.ArrayDiff           = messages.getNotNothingString("ArrayDiff",          errorInMethod);
        PrimitiveArray.ArrayDifferentSize  = messages.getNotNothingString("ArrayDifferentSize", errorInMethod);
        PrimitiveArray.ArrayDifferentValue = messages.getNotNothingString("ArrayDifferentValue",errorInMethod);
        PrimitiveArray.ArrayDiffString     = messages.getNotNothingString("ArrayDiffString",    errorInMethod);
        PrimitiveArray.ArrayMissingValue   = messages.getNotNothingString("ArrayMissingValue",  errorInMethod);
        PrimitiveArray.ArrayNotAscending   = messages.getNotNothingString("ArrayNotAscending",  errorInMethod);
        PrimitiveArray.ArrayNotDescending  = messages.getNotNothingString("ArrayNotDescending", errorInMethod);
        PrimitiveArray.ArrayNotEvenlySpaced= messages.getNotNothingString("ArrayNotEvenlySpaced",errorInMethod);
        PrimitiveArray.ArrayRemove         = messages.getNotNothingString("ArrayRemove",        errorInMethod);
        PrimitiveArray.ArraySubsetStart    = messages.getNotNothingString("ArraySubsetStart",   errorInMethod);
        PrimitiveArray.ArraySubsetStride   = messages.getNotNothingString("ArraySubsetStride",  errorInMethod);
        categoryTitleHtml          = messages.getNotNothingString("categoryTitleHtml",          errorInMethod);
        category1Html              = messages.getNotNothingString("category1Html",              errorInMethod);
        category2Html              = messages.getNotNothingString("category2Html",              errorInMethod);
        category3Html              = messages.getNotNothingString("category3Html",              errorInMethod);
        categoryPickAttribute      = messages.getNotNothingString("categoryPickAttribute",      errorInMethod);
        categorySearchHtml         = messages.getNotNothingString("categorySearchHtml",         errorInMethod);
        categorySearchDifferentHtml= messages.getNotNothingString("categorySearchDifferentHtml",errorInMethod);
        categoryClickHtml          = messages.getNotNothingString("categoryClickHtml",          errorInMethod);
        categoryNotAnOption        = messages.getNotNothingString("categoryNotAnOption",        errorInMethod);
        caughtInterrupted    = " " + messages.getNotNothingString("caughtInterrupted",          errorInMethod);
        clickAccess                = messages.getNotNothingString("clickAccess",                errorInMethod);
        clickBackgroundInfo        = messages.getNotNothingString("clickBackgroundInfo",        errorInMethod);
        clickERDDAP                = messages.getNotNothingString("clickERDDAP",                errorInMethod);
        clickInfo                  = messages.getNotNothingString("clickInfo",                  errorInMethod);
        clickToSubmit              = messages.getNotNothingString("clickToSubmit",              errorInMethod);
        convertOceanicAtmosphericAcronyms             = messages.getNotNothingString("convertOceanicAtmosphericAcronyms",             errorInMethod);
        convertOceanicAtmosphericAcronymsIntro        = messages.getNotNothingString("convertOceanicAtmosphericAcronymsIntro",        errorInMethod);
        convertOceanicAtmosphericAcronymsNotes        = messages.getNotNothingString("convertOceanicAtmosphericAcronymsNotes",        errorInMethod);
        convertOceanicAtmosphericAcronymsService      = messages.getNotNothingString("convertOceanicAtmosphericAcronymsService",      errorInMethod);
        convertOceanicAtmosphericVariableNames        = messages.getNotNothingString("convertOceanicAtmosphericVariableNames",        errorInMethod);
        convertOceanicAtmosphericVariableNamesIntro   = messages.getNotNothingString("convertOceanicAtmosphericVariableNamesIntro",   errorInMethod);
        convertOceanicAtmosphericVariableNamesNotes   = messages.getNotNothingString("convertOceanicAtmosphericVariableNamesNotes",   errorInMethod);
        convertOceanicAtmosphericVariableNamesService = messages.getNotNothingString("convertOceanicAtmosphericVariableNamesService", errorInMethod);
        convertFipsCounty          = messages.getNotNothingString("convertFipsCounty",          errorInMethod);
        convertFipsCountyIntro     = messages.getNotNothingString("convertFipsCountyIntro",     errorInMethod);
        convertFipsCountyNotes     = messages.getNotNothingString("convertFipsCountyNotes",     errorInMethod);
        convertFipsCountyService   = messages.getNotNothingString("convertFipsCountyService",   errorInMethod);
        convertHtml                = messages.getNotNothingString("convertHtml",                errorInMethod);
        convertKeywords            = messages.getNotNothingString("convertKeywords",            errorInMethod);
        convertKeywordsCfTooltip   = messages.getNotNothingString("convertKeywordsCfTooltip",   errorInMethod);
        convertKeywordsGcmdTooltip = messages.getNotNothingString("convertKeywordsGcmdTooltip", errorInMethod);
        convertKeywordsIntro       = messages.getNotNothingString("convertKeywordsIntro",       errorInMethod);
        convertKeywordsNotes       = messages.getNotNothingString("convertKeywordsNotes",       errorInMethod);
        convertKeywordsService     = messages.getNotNothingString("convertKeywordsService",     errorInMethod);
        convertTime                = messages.getNotNothingString("convertTime",                errorInMethod);
        convertTimeBypass          = messages.getNotNothingString("convertTimeBypass",          errorInMethod);
        convertTimeReference       = messages.getNotNothingString("convertTimeReference",       errorInMethod);
        convertTimeIntro           = messages.getNotNothingString("convertTimeIntro",           errorInMethod);
        convertTimeNotes           = messages.getNotNothingString("convertTimeNotes",           errorInMethod);
        convertTimeService         = messages.getNotNothingString("convertTimeService",         errorInMethod);
        convertTimeNumberTooltip   = messages.getNotNothingString("convertTimeNumberTooltip",   errorInMethod);
        convertTimeStringTimeTooltip=messages.getNotNothingString("convertTimeStringTimeTooltip",errorInMethod);
        convertTimeUnitsTooltip    = messages.getNotNothingString("convertTimeUnitsTooltip",    errorInMethod);
        convertTimeUnitsHelp       = messages.getNotNothingString("convertTimeUnitsHelp",       errorInMethod);
        convertTimeIsoFormatError  = messages.getNotNothingString("convertTimeIsoFormatError",  errorInMethod);
        convertTimeNoSinceError    = messages.getNotNothingString("convertTimeNoSinceError",    errorInMethod);
        convertTimeNumberError     = messages.getNotNothingString("convertTimeNumberError",     errorInMethod);
        convertTimeNumericTimeError= messages.getNotNothingString("convertTimeNumericTimeError",errorInMethod);
        convertTimeParametersError = messages.getNotNothingString("convertTimeParametersError", errorInMethod);
        convertTimeStringFormatError=messages.getNotNothingString("convertTimeStringFormatError",errorInMethod);
        convertTimeTwoTimeError    = messages.getNotNothingString("convertTimeTwoTimeError",    errorInMethod);
        convertTimeUnitsError      = messages.getNotNothingString("convertTimeUnitsError",      errorInMethod);
        convertUnits               = messages.getNotNothingString("convertUnits",               errorInMethod);
        convertUnitsComparison     = messages.getNotNothingString("convertUnitsComparison",     errorInMethod);
        convertUnitsFilter         = messages.getNotNothingString("convertUnitsFilter",         errorInMethod);
        convertUnitsIntro          = messages.getNotNothingString("convertUnitsIntro",          errorInMethod);
        convertUnitsNotes          = messages.getNotNothingString("convertUnitsNotes",          errorInMethod);
        convertUnitsService        = messages.getNotNothingString("convertUnitsService",        errorInMethod);
        cookiesHelp                = messages.getNotNothingString("cookiesHelp",                errorInMethod);
        daf                        = messages.getNotNothingString("daf",                        errorInMethod);
        dafGridBypassTooltip       = messages.getNotNothingString("dafGridBypassTooltip",       errorInMethod);
        dafGridTooltip             = messages.getNotNothingString("dafGridTooltip",             errorInMethod);
        dafTableBypassTooltip      = messages.getNotNothingString("dafTableBypassTooltip",      errorInMethod);
        dafTableTooltip            = messages.getNotNothingString("dafTableTooltip",            errorInMethod);
        dasTitle                   = messages.getNotNothingString("dasTitle",                   errorInMethod);
        dataAccessNotAllowed       = messages.getNotNothingString("dataAccessNotAllowed",       errorInMethod);
        databaseUnableToConnect    = messages.getNotNothingString("databaseUnableToConnect",    errorInMethod);
        disabled                   = messages.getNotNothingString("disabled",                   errorInMethod);
        distinctValuesTooltip      = messages.getNotNothingString("distinctValuesTooltip",      errorInMethod);
        doWithGraphs               = messages.getNotNothingString("doWithGraphs",               errorInMethod);

        dtAccessible               = messages.getNotNothingString("dtAccessible",               errorInMethod);
        dtAccessibleYes            = messages.getNotNothingString("dtAccessibleYes",            errorInMethod);
        dtAccessibleGraphs         = messages.getNotNothingString("dtAccessibleGraphs",         errorInMethod);
        dtAccessibleNo             = messages.getNotNothingString("dtAccessibleNo",             errorInMethod);
        dtAccessibleLogIn          = messages.getNotNothingString("dtAccessibleLogIn",          errorInMethod);
        dtLogIn                    = messages.getNotNothingString("dtLogIn",                    errorInMethod);
        dtDAF1                     = messages.getNotNothingString("dtDAF1",                     errorInMethod);
        dtDAF2                     = messages.getNotNothingString("dtDAF2",                     errorInMethod);
        dtFiles                    = messages.getNotNothingString("dtFiles",                    errorInMethod);
        dtMAG                      = messages.getNotNothingString("dtMAG",                      errorInMethod);
        dtSOS                      = messages.getNotNothingString("dtSOS",                      errorInMethod);
        dtSubset                   = messages.getNotNothingString("dtSubset",                   errorInMethod);
        dtWCS                      = messages.getNotNothingString("dtWCS",                      errorInMethod);
        dtWMS                      = messages.getNotNothingString("dtWMS",                      errorInMethod);
        
        EDDDatasetID               = messages.getNotNothingString("EDDDatasetID",               errorInMethod);
        EDDFgdc                    = messages.getNotNothingString("EDDFgdc",                    errorInMethod);
        EDDFgdcMetadata            = messages.getNotNothingString("EDDFgdcMetadata",            errorInMethod);
        EDDFiles                   = messages.getNotNothingString("EDDFiles",                   errorInMethod);
        EDDIso19115                = messages.getNotNothingString("EDDIso19115",                errorInMethod);
        EDDIso19115Metadata        = messages.getNotNothingString("EDDIso19115Metadata",        errorInMethod);
        EDDMetadata                = messages.getNotNothingString("EDDMetadata",                errorInMethod);
        EDDBackground              = messages.getNotNothingString("EDDBackground",              errorInMethod);
        EDDClickOnSubmitHtml       = messages.getNotNothingString("EDDClickOnSubmitHtml",       errorInMethod);
        EDDInformation             = messages.getNotNothingString("EDDInformation",             errorInMethod);
        EDDInstitution             = messages.getNotNothingString("EDDInstitution",             errorInMethod);
        EDDSummary                 = messages.getNotNothingString("EDDSummary",                 errorInMethod);
        EDDDatasetTitle            = messages.getNotNothingString("EDDDatasetTitle",            errorInMethod);
        EDDDownloadData            = messages.getNotNothingString("EDDDownloadData",            errorInMethod);
        EDDMakeAGraph              = messages.getNotNothingString("EDDMakeAGraph",              errorInMethod);
        EDDMakeAMap                = messages.getNotNothingString("EDDMakeAMap",                errorInMethod);
        EDDFileType                = messages.getNotNothingString("EDDFileType",                errorInMethod);
        EDDFileTypeInformation     = messages.getNotNothingString("EDDFileTypeInformation",     errorInMethod);
        EDDSelectFileType          = messages.getNotNothingString("EDDSelectFileType",          errorInMethod);
        EDDMinimum                 = messages.getNotNothingString("EDDMinimum",                 errorInMethod);
        EDDMaximum                 = messages.getNotNothingString("EDDMaximum",                 errorInMethod);
        EDDConstraint              = messages.getNotNothingString("EDDConstraint",              errorInMethod);

        EDDChangedWasnt            = messages.getNotNothingString("EDDChangedWasnt",            errorInMethod);
        EDDChangedDifferentNVar    = messages.getNotNothingString("EDDChangedDifferentNVar",    errorInMethod);
        EDDChanged2Different       = messages.getNotNothingString("EDDChanged2Different",       errorInMethod);
        EDDChanged1Different       = messages.getNotNothingString("EDDChanged1Different",       errorInMethod);
        EDDChangedCGADifferent     = messages.getNotNothingString("EDDChangedCGADifferent",     errorInMethod);
        EDDChangedAxesDifferentNVar= messages.getNotNothingString("EDDChangedAxesDifferentNVar",errorInMethod);
        EDDChangedAxes2Different   = messages.getNotNothingString("EDDChangedAxes2Different",   errorInMethod);
        EDDChangedAxes1Different   = messages.getNotNothingString("EDDChangedAxes1Different",   errorInMethod);
        EDDChangedNoValue          = messages.getNotNothingString("EDDChangedNoValue",          errorInMethod);
        EDDChangedTableToGrid      = messages.getNotNothingString("EDDChangedTableToGrid",      errorInMethod);

        EDDSimilarDifferentNVar    = messages.getNotNothingString("EDDSimilarDifferentNVar",    errorInMethod);
        EDDSimilarDifferent        = messages.getNotNothingString("EDDSimilarDifferent",        errorInMethod);

        EDDGridDownloadTooltip     = messages.getNotNothingString("EDDGridDownloadTooltip",     errorInMethod);
        EDDGridDapDescription      = messages.getNotNothingString("EDDGridDapDescription",      errorInMethod);
        EDDGridDapLongDescription  = messages.getNotNothingString("EDDGridDapLongDescription",  errorInMethod);
        EDDGridDownloadDataTooltip = messages.getNotNothingString("EDDGridDownloadDataTooltip", errorInMethod);
        EDDGridDimension           = messages.getNotNothingString("EDDGridDimension",           errorInMethod);
        EDDGridDimensionRanges     = messages.getNotNothingString("EDDGridDimensionRanges",     errorInMethod);
        EDDGridFirst               = messages.getNotNothingString("EDDGridFirst",               errorInMethod);
        EDDGridLast                = messages.getNotNothingString("EDDGridLast",                errorInMethod);
        EDDGridStart               = messages.getNotNothingString("EDDGridStart",               errorInMethod);
        EDDGridStop                = messages.getNotNothingString("EDDGridStop",                errorInMethod);
        EDDGridStartStopTooltip    = messages.getNotNothingString("EDDGridStartStopTooltip",    errorInMethod);
        EDDGridStride              = messages.getNotNothingString("EDDGridStride",              errorInMethod);
        EDDGridNValues             = messages.getNotNothingString("EDDGridNValues",             errorInMethod);
        EDDGridNValuesHtml         = messages.getNotNothingString("EDDGridNValuesHtml",         errorInMethod);
        EDDGridSpacing             = messages.getNotNothingString("EDDGridSpacing",             errorInMethod);
        EDDGridJustOneValue        = messages.getNotNothingString("EDDGridJustOneValue",        errorInMethod);
        EDDGridEven                = messages.getNotNothingString("EDDGridEven",                errorInMethod);
        EDDGridUneven              = messages.getNotNothingString("EDDGridUneven",              errorInMethod);
        EDDGridDimensionTooltip    = messages.getNotNothingString("EDDGridDimensionTooltip",    errorInMethod);
        EDDGridDimensionFirstTooltip = messages.getNotNothingString("EDDGridDimensionFirstTooltip", errorInMethod);
        EDDGridDimensionLastTooltip  = messages.getNotNothingString("EDDGridDimensionLastTooltip",  errorInMethod);
        EDDGridVarHasDimTooltip    = messages.getNotNothingString("EDDGridVarHasDimTooltip",    errorInMethod);
        EDDGridSSSTooltip          = messages.getNotNothingString("EDDGridSSSTooltip",          errorInMethod);
        EDDGridStartTooltip        = messages.getNotNothingString("EDDGridStartTooltip",        errorInMethod);
        EDDGridStopTooltip         = messages.getNotNothingString("EDDGridStopTooltip",         errorInMethod);
        EDDGridStrideTooltip       = messages.getNotNothingString("EDDGridStrideTooltip",       errorInMethod);
        EDDGridSpacingTooltip      = messages.getNotNothingString("EDDGridSpacingTooltip",      errorInMethod);
        EDDGridGridVariableHtml    = messages.getNotNothingString("EDDGridGridVariableHtml",    errorInMethod);

        //default EDDGrid...Example
        EDDGridErddapUrlExample    = messages.getNotNothingString("EDDGridErddapUrlExample",    errorInMethod);
        EDDGridIdExample           = messages.getNotNothingString("EDDGridIdExample",           errorInMethod);
        EDDGridDimensionExample    = messages.getNotNothingString("EDDGridDimensionExample",    errorInMethod);
        EDDGridNoHyperExample      = messages.getNotNothingString("EDDGridNoHyperExample",      errorInMethod);
        EDDGridDimNamesExample     = messages.getNotNothingString("EDDGridDimNamesExample",     errorInMethod);
        EDDGridDataTimeExample     = messages.getNotNothingString("EDDGridDataTimeExample",     errorInMethod);
        EDDGridDataValueExample    = messages.getNotNothingString("EDDGridDataValueExample",    errorInMethod);
        EDDGridDataIndexExample    = messages.getNotNothingString("EDDGridDataIndexExample",    errorInMethod);
        EDDGridGraphExample        = messages.getNotNothingString("EDDGridGraphExample",        errorInMethod);
        EDDGridMapExample          = messages.getNotNothingString("EDDGridMapExample",          errorInMethod);
        EDDGridMatlabPlotExample   = messages.getNotNothingString("EDDGridMatlabPlotExample",          errorInMethod);

        //admin provides EDDGrid...Example
        EDDGridErddapUrlExample    = setup.getString("EDDGridErddapUrlExample",    EDDGridErddapUrlExample);
        EDDGridIdExample           = setup.getString("EDDGridIdExample",           EDDGridIdExample);
        EDDGridDimensionExample    = setup.getString("EDDGridDimensionExample",    EDDGridDimensionExample);
        EDDGridNoHyperExample      = setup.getString("EDDGridNoHyperExample",      EDDGridNoHyperExample);
        EDDGridDimNamesExample     = setup.getString("EDDGridDimNamesExample",     EDDGridDimNamesExample);
        EDDGridDataIndexExample    = setup.getString("EDDGridDataIndexExample",    EDDGridDataIndexExample);
        EDDGridDataValueExample    = setup.getString("EDDGridDataValueExample",    EDDGridDataValueExample);
        EDDGridDataTimeExample     = setup.getString("EDDGridDataTimeExample",     EDDGridDataTimeExample);
        EDDGridGraphExample        = setup.getString("EDDGridGraphExample",        EDDGridGraphExample);
        EDDGridMapExample          = setup.getString("EDDGridMapExample",          EDDGridMapExample);
        EDDGridMatlabPlotExample   = setup.getString("EDDGridMatlabPlotExample",   EDDGridMatlabPlotExample);

        //variants encoded to be Html Examples
        EDDGridDimensionExampleHE  = XML.encodeAsHTML(EDDGridDimensionExample);
        EDDGridDataIndexExampleHE  = XML.encodeAsHTML(EDDGridDataIndexExample);
        EDDGridDataValueExampleHE  = XML.encodeAsHTML(EDDGridDataValueExample);
        EDDGridDataTimeExampleHE   = XML.encodeAsHTML(EDDGridDataTimeExample);
        EDDGridGraphExampleHE      = XML.encodeAsHTML(EDDGridGraphExample);
        EDDGridMapExampleHE        = XML.encodeAsHTML(EDDGridMapExample);

        //variants encoded to be Html Attributes
        EDDGridDimensionExampleHA  = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDGridDimensionExample));
        EDDGridDataIndexExampleHA  = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDGridDataIndexExample));
        EDDGridDataValueExampleHA  = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDGridDataValueExample));
        EDDGridDataTimeExampleHA   = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDGridDataTimeExample));
        EDDGridGraphExampleHA      = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDGridGraphExample));
        EDDGridMapExampleHA        = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDGridMapExample)); 


        EDDTableConstraints        = messages.getNotNothingString("EDDTableConstraints",        errorInMethod);
        EDDTableDapDescription     = messages.getNotNothingString("EDDTableDapDescription",     errorInMethod);
        EDDTableDapLongDescription = messages.getNotNothingString("EDDTableDapLongDescription", errorInMethod);
        EDDTableDownloadDataTooltip  =messages.getNotNothingString("EDDTableDownloadDataTooltip",   errorInMethod);
        EDDTableTabularDatasetTooltip=messages.getNotNothingString("EDDTableTabularDatasetTooltip", errorInMethod);
        EDDTableVariable           = messages.getNotNothingString("EDDTableVariable",           errorInMethod);
        EDDTableCheckAll           = messages.getNotNothingString("EDDTableCheckAll",           errorInMethod);
        EDDTableCheckAllTooltip    = messages.getNotNothingString("EDDTableCheckAllTooltip",    errorInMethod);
        EDDTableUncheckAll         = messages.getNotNothingString("EDDTableUncheckAll",         errorInMethod);
        EDDTableUncheckAllTooltip  = messages.getNotNothingString("EDDTableUncheckAllTooltip",  errorInMethod);
        EDDTableMinimumTooltip     = messages.getNotNothingString("EDDTableMinimumTooltip",     errorInMethod);
        EDDTableMaximumTooltip     = messages.getNotNothingString("EDDTableMaximumTooltip",     errorInMethod);
        EDDTableCheckTheVariables  = messages.getNotNothingString("EDDTableCheckTheVariables",  errorInMethod);
        EDDTableSelectAnOperator   = messages.getNotNothingString("EDDTableSelectAnOperator",   errorInMethod);
        EDDTableOptConstraint1Html = messages.getNotNothingString("EDDTableOptConstraint1Html", errorInMethod);
        EDDTableOptConstraint2Html = messages.getNotNothingString("EDDTableOptConstraint2Html", errorInMethod);
        EDDTableOptConstraintVar   = messages.getNotNothingString("EDDTableOptConstraintVar",   errorInMethod);
        EDDTableNumericConstraintTooltip=messages.getNotNothingString("EDDTableNumericConstraintTooltip",errorInMethod);
        EDDTableStringConstraintTooltip =messages.getNotNothingString("EDDTableStringConstraintTooltip",errorInMethod);
        EDDTableTimeConstraintTooltip   =messages.getNotNothingString("EDDTableTimeConstraintTooltip", errorInMethod);
        EDDTableConstraintTooltip  = messages.getNotNothingString("EDDTableConstraintTooltip",  errorInMethod);
        EDDTableSelectConstraintTooltip =messages.getNotNothingString("EDDTableSelectConstraintTooltip",   errorInMethod);

        //default EDDGrid...Example
        EDDTableErddapUrlExample   = messages.getNotNothingString("EDDTableErddapUrlExample",   errorInMethod);
        EDDTableIdExample          = messages.getNotNothingString("EDDTableIdExample",          errorInMethod);
        EDDTableVariablesExample   = messages.getNotNothingString("EDDTableVariablesExample",   errorInMethod);
        EDDTableConstraintsExample = messages.getNotNothingString("EDDTableConstraintsExample", errorInMethod);
        EDDTableDataValueExample   = messages.getNotNothingString("EDDTableDataValueExample",   errorInMethod);
        EDDTableDataTimeExample    = messages.getNotNothingString("EDDTableDataTimeExample",    errorInMethod);
        EDDTableGraphExample       = messages.getNotNothingString("EDDTableGraphExample",       errorInMethod);
        EDDTableMapExample         = messages.getNotNothingString("EDDTableMapExample",         errorInMethod);
        EDDTableMatlabPlotExample  = messages.getNotNothingString("EDDTableMatlabPlotExample",  errorInMethod);

        //admin provides EDDGrid...Example
        EDDTableErddapUrlExample   = setup.getString("EDDTableErddapUrlExample",   EDDTableErddapUrlExample);
        EDDTableIdExample          = setup.getString("EDDTableIdExample",          EDDTableIdExample);
        EDDTableVariablesExample   = setup.getString("EDDTableVariablesExample",   EDDTableVariablesExample);
        EDDTableConstraintsExample = setup.getString("EDDTableConstraintsExample", EDDTableConstraintsExample);
        EDDTableDataValueExample   = setup.getString("EDDTableDataValueExample",   EDDTableDataValueExample);
        EDDTableDataTimeExample    = setup.getString("EDDTableDataTimeExample",    EDDTableDataTimeExample);
        EDDTableGraphExample       = setup.getString("EDDTableGraphExample",       EDDTableGraphExample);
        EDDTableMapExample         = setup.getString("EDDTableMapExample",         EDDTableMapExample);
        EDDTableMatlabPlotExample  = setup.getString("EDDTableMatlabPlotExample",  EDDTableMatlabPlotExample);

        //variants encoded to be Html Examples
        EDDTableConstraintsExampleHE = XML.encodeAsHTML(EDDTableConstraintsExample);
        EDDTableDataTimeExampleHE    = XML.encodeAsHTML(EDDTableDataTimeExample);
        EDDTableDataValueExampleHE   = XML.encodeAsHTML(EDDTableDataValueExample);
        EDDTableGraphExampleHE       = XML.encodeAsHTML(EDDTableGraphExample);
        EDDTableMapExampleHE         = XML.encodeAsHTML(EDDTableMapExample);

        //variants encoded to be Html Attributes
        EDDTableConstraintsExampleHA = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDTableConstraintsExample));
        EDDTableDataTimeExampleHA    = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDTableDataTimeExample));
        EDDTableDataValueExampleHA   = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDTableDataValueExample));
        EDDTableGraphExampleHA       = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDTableGraphExample));
        EDDTableMapExampleHA         = XML.encodeAsHTMLAttribute(SSR.psuedoPercentEncode(EDDTableMapExample));

        errorTitle                 = messages.getNotNothingString("errorTitle",                 errorInMethod);
        errorRequestUrl            = messages.getNotNothingString("errorRequestUrl",            errorInMethod);
        errorRequestQuery          = messages.getNotNothingString("errorRequestQuery",          errorInMethod);
        errorTheError              = messages.getNotNothingString("errorTheError",              errorInMethod);
        errorCopyFrom              = messages.getNotNothingString("errorCopyFrom",              errorInMethod);
        errorFileNotFound          = messages.getNotNothingString("errorFileNotFound",          errorInMethod);
        errorFileNotFoundImage     = messages.getNotNothingString("errorFileNotFoundImage",     errorInMethod);
        errorInternal              = messages.getNotNothingString("errorInternal",              errorInMethod) + " ";
        errorJsonpFunctionName     = messages.getNotNothingString("errorJsonpFunctionName",     errorInMethod);
        errorJsonpNotAllowed       = messages.getNotNothingString("errorJsonpNotAllowed",       errorInMethod);
        errorMoreThan2GB           = messages.getNotNothingString("errorMoreThan2GB",           errorInMethod);
        errorNotFound              = messages.getNotNothingString("errorNotFound",              errorInMethod);
        errorNotFoundIn            = messages.getNotNothingString("errorNotFoundIn",            errorInMethod);
        errorOdvLLTGrid            = messages.getNotNothingString("errorOdvLLTGrid",            errorInMethod);
        errorOdvLLTTable           = messages.getNotNothingString("errorOdvLLTTable",           errorInMethod);
        errorOnWebPage             = messages.getNotNothingString("errorOnWebPage",             errorInMethod);
        errorXWasntSpecified       = messages.getNotNothingString("errorXWasntSpecified",       errorInMethod);
        HtmlWidgets.errorXWasntSpecified = errorXWasntSpecified;
        errorXWasTooLong           = messages.getNotNothingString("errorXWasTooLong",           errorInMethod);
        HtmlWidgets.errorXWasTooLong = errorXWasTooLong;
        externalLink         = " " + messages.getNotNothingString("externalLink",               errorInMethod);
        externalWebSite            = messages.getNotNothingString("externalWebSite",            errorInMethod);
        fileHelp_asc               = messages.getNotNothingString("fileHelp_asc",               errorInMethod);
        fileHelp_csv               = messages.getNotNothingString("fileHelp_csv",               errorInMethod);
        fileHelp_csvp              = messages.getNotNothingString("fileHelp_csvp",              errorInMethod);
        fileHelp_csv0              = messages.getNotNothingString("fileHelp_csv0",              errorInMethod);
        fileHelp_das               = messages.getNotNothingString("fileHelp_das",               errorInMethod);
        fileHelp_dds               = messages.getNotNothingString("fileHelp_dds",               errorInMethod);
        fileHelp_dods              = messages.getNotNothingString("fileHelp_dods",              errorInMethod);
        fileHelpGrid_esriAscii     = messages.getNotNothingString("fileHelpGrid_esriAscii",     errorInMethod);
        fileHelpTable_esriCsv      = messages.getNotNothingString("fileHelpTable_esriCsv",      errorInMethod);
        fileHelp_fgdc              = messages.getNotNothingString("fileHelp_fgdc",              errorInMethod);
        fileHelp_geoJson           = messages.getNotNothingString("fileHelp_geoJson",           errorInMethod);
        fileHelp_graph             = messages.getNotNothingString("fileHelp_graph",             errorInMethod);
        fileHelpGrid_help          = messages.getNotNothingString("fileHelpGrid_help",          errorInMethod);
        fileHelpTable_help         = messages.getNotNothingString("fileHelpTable_help",         errorInMethod);
        fileHelp_html              = messages.getNotNothingString("fileHelp_html",              errorInMethod);
        fileHelp_htmlTable         = messages.getNotNothingString("fileHelp_htmlTable",         errorInMethod);
        fileHelp_iso19115          = messages.getNotNothingString("fileHelp_iso19115",          errorInMethod);
        fileHelp_itxGrid           = messages.getNotNothingString("fileHelp_itxGrid",           errorInMethod);
        fileHelp_itxTable          = messages.getNotNothingString("fileHelp_itxTable",          errorInMethod);
        fileHelp_json              = messages.getNotNothingString("fileHelp_json",              errorInMethod);
        fileHelp_jsonlCSV          = messages.getNotNothingString("fileHelp_jsonlCSV",          errorInMethod);
        fileHelp_jsonlKVP          = messages.getNotNothingString("fileHelp_jsonlKVP",          errorInMethod);
        fileHelp_mat               = messages.getNotNothingString("fileHelp_mat",               errorInMethod);
        fileHelpGrid_nc3           = messages.getNotNothingString("fileHelpGrid_nc3",           errorInMethod);
        fileHelpGrid_nc4           = messages.getNotNothingString("fileHelpGrid_nc4",           errorInMethod);
        fileHelpTable_nc3          = messages.getNotNothingString("fileHelpTable_nc3",          errorInMethod);
        fileHelpTable_nc4          = messages.getNotNothingString("fileHelpTable_nc4",          errorInMethod);
        fileHelp_nc3Header         = messages.getNotNothingString("fileHelp_nc3Header",         errorInMethod);
        fileHelp_nc4Header         = messages.getNotNothingString("fileHelp_nc4Header",         errorInMethod);
        fileHelp_nccsv             = messages.getNotNothingString("fileHelp_nccsv",             errorInMethod);
        fileHelp_nccsvMetadata     = messages.getNotNothingString("fileHelp_nccsvMetadata",     errorInMethod);
        fileHelp_ncCF              = messages.getNotNothingString("fileHelp_ncCF",              errorInMethod);
        fileHelp_ncCFHeader        = messages.getNotNothingString("fileHelp_ncCFHeader",        errorInMethod);
        fileHelp_ncCFMA            = messages.getNotNothingString("fileHelp_ncCFMA",            errorInMethod);
        fileHelp_ncCFMAHeader      = messages.getNotNothingString("fileHelp_ncCFMAHeader",      errorInMethod);
        fileHelp_ncml              = messages.getNotNothingString("fileHelp_ncml",              errorInMethod);
        fileHelp_ncoJson           = messages.getNotNothingString("fileHelp_ncoJson",           errorInMethod);
        fileHelpGrid_odvTxt        = messages.getNotNothingString("fileHelpGrid_odvTxt",        errorInMethod);
        fileHelpTable_odvTxt       = messages.getNotNothingString("fileHelpTable_odvTxt",       errorInMethod);
        fileHelp_subset            = messages.getNotNothingString("fileHelp_subset",            errorInMethod);
        fileHelp_timeGaps          = messages.getNotNothingString("fileHelp_timeGaps",          errorInMethod);
        fileHelp_tsv               = messages.getNotNothingString("fileHelp_tsv",               errorInMethod);
        fileHelp_tsvp              = messages.getNotNothingString("fileHelp_tsvp",              errorInMethod);
        fileHelp_tsv0              = messages.getNotNothingString("fileHelp_tsv0",              errorInMethod);
        fileHelp_wav               = messages.getNotNothingString("fileHelp_wav",               errorInMethod);
        fileHelp_xhtml             = messages.getNotNothingString("fileHelp_xhtml",             errorInMethod);
        fileHelp_geotif            = messages.getNotNothingString("fileHelp_geotif",            errorInMethod);
        fileHelpGrid_kml           = messages.getNotNothingString("fileHelpGrid_kml",           errorInMethod);
        fileHelpTable_kml          = messages.getNotNothingString("fileHelpTable_kml",          errorInMethod);
        fileHelp_smallPdf          = messages.getNotNothingString("fileHelp_smallPdf",          errorInMethod);
        fileHelp_pdf               = messages.getNotNothingString("fileHelp_pdf",               errorInMethod);
        fileHelp_largePdf          = messages.getNotNothingString("fileHelp_largePdf",          errorInMethod);
        fileHelp_smallPng          = messages.getNotNothingString("fileHelp_smallPng",          errorInMethod);
        fileHelp_png               = messages.getNotNothingString("fileHelp_png",               errorInMethod);
        fileHelp_largePng          = messages.getNotNothingString("fileHelp_largePng",          errorInMethod);
        fileHelp_transparentPng    = messages.getNotNothingString("fileHelp_transparentPng",    errorInMethod);
        filesDescription           = messages.getNotNothingString("filesDescription",           errorInMethod);
        filesDocumentation         = messages.getNotNothingString("filesDocumentation",         errorInMethod);
        filesSort                  = messages.getNotNothingString("filesSort",                  errorInMethod);
        filesWarning               = messages.getNotNothingString("filesWarning",               errorInMethod);
        functions                  = messages.getNotNothingString("functions",                  errorInMethod);
        functionTooltip            = messages.getNotNothingString("functionTooltip",            errorInMethod);
        functionDistinctCheck      = messages.getNotNothingString("functionDistinctCheck",      errorInMethod);
        functionDistinctTooltip    = messages.getNotNothingString("functionDistinctTooltip",    errorInMethod);
        functionOrderByExtra       = messages.getNotNothingString("functionOrderByExtra",       errorInMethod);
        functionOrderByTooltip     = messages.getNotNothingString("functionOrderByTooltip",     errorInMethod);
        functionOrderBySort        = messages.getNotNothingString("functionOrderBySort",        errorInMethod);
        functionOrderBySort1       = messages.getNotNothingString("functionOrderBySort1",       errorInMethod);
        functionOrderBySort2       = messages.getNotNothingString("functionOrderBySort2",       errorInMethod);
        functionOrderBySort3       = messages.getNotNothingString("functionOrderBySort3",       errorInMethod);
        functionOrderBySort4       = messages.getNotNothingString("functionOrderBySort4",       errorInMethod);
        functionOrderBySortLeast   = messages.getNotNothingString("functionOrderBySortLeast",   errorInMethod);
        functionOrderBySortRowMax  = messages.getNotNothingString("functionOrderBySortRowMax",  errorInMethod);
        generatedAt                = messages.getNotNothingString("generatedAt",                errorInMethod);
        geoServicesDescription     = messages.getNotNothingString("geoServicesDescription",     errorInMethod);
        getStartedHtml             = messages.getNotNothingString("getStartedHtml",             errorInMethod);
        TableWriterHtmlTable.htmlTableMaxMB     = messages.getInt("htmlTableMaxMB", TableWriterHtmlTable.htmlTableMaxMB);                                   
        htmlTableMaxMessage        = messages.getNotNothingString("htmlTableMaxMessage",        errorInMethod);
        imageDataCourtesyOf        = messages.getNotNothingString("imageDataCourtesyOf",        errorInMethod);
        imageWidths                = String2.toIntArray(String2.split(messages.getNotNothingString("imageWidths",  errorInMethod), ','));
        imageHeights               = String2.toIntArray(String2.split(messages.getNotNothingString("imageHeights", errorInMethod), ','));
        indexViewAll               = messages.getNotNothingString("indexViewAll",               errorInMethod);
        indexSearchWith            = messages.getNotNothingString("indexSearchWith",            errorInMethod);
        indexDevelopersSearch      = messages.getNotNothingString("indexDevelopersSearch",      errorInMethod);
        indexProtocol              = messages.getNotNothingString("indexProtocol",              errorInMethod);
        indexDescription           = messages.getNotNothingString("indexDescription",           errorInMethod);
        indexDatasets              = messages.getNotNothingString("indexDatasets",              errorInMethod);
        indexDocumentation         = messages.getNotNothingString("indexDocumentation",         errorInMethod);
        indexRESTfulSearch         = messages.getNotNothingString("indexRESTfulSearch",         errorInMethod);
        indexAllDatasetsSearch     = messages.getNotNothingString("indexAllDatasetsSearch",     errorInMethod);
        indexOpenSearch            = messages.getNotNothingString("indexOpenSearch",            errorInMethod);
        indexServices              = messages.getNotNothingString("indexServices",              errorInMethod);
        indexDescribeServices      = messages.getNotNothingString("indexDescribeServices",      errorInMethod);
        indexMetadata              = messages.getNotNothingString("indexMetadata",              errorInMethod);
        indexWAF1                  = messages.getNotNothingString("indexWAF1",                  errorInMethod);
        indexWAF2                  = messages.getNotNothingString("indexWAF2",                  errorInMethod);
        indexConverters            = messages.getNotNothingString("indexConverters",            errorInMethod);
        indexDescribeConverters    = messages.getNotNothingString("indexDescribeConverters",    errorInMethod);
        infoAboutFrom              = messages.getNotNothingString("infoAboutFrom",              errorInMethod);
        infoTableTitleHtml         = messages.getNotNothingString("infoTableTitleHtml",         errorInMethod);
        infoRequestForm            = messages.getNotNothingString("infoRequestForm",            errorInMethod);
        inotifyFix                 = messages.getNotNothingString("inotifyFix",                 errorInMethod);
        justGenerateAndView        = messages.getNotNothingString("justGenerateAndView",        errorInMethod);
        justGenerateAndViewTooltip = messages.getNotNothingString("justGenerateAndViewTooltip", errorInMethod);
        justGenerateAndViewUrl     = messages.getNotNothingString("justGenerateAndViewUrl",     errorInMethod);
        justGenerateAndViewGraphUrlTooltip = messages.getNotNothingString("justGenerateAndViewGraphUrlTooltip", errorInMethod);
        license                    = messages.getNotNothingString("license",                    errorInMethod);
        listAll                    = messages.getNotNothingString("listAll",                    errorInMethod);
        listOfDatasets             = messages.getNotNothingString("listOfDatasets",             errorInMethod);
        LogIn                      = messages.getNotNothingString("LogIn",                      errorInMethod);
        login                      = messages.getNotNothingString("login",                      errorInMethod);
        loginAttemptBlocked        = messages.getNotNothingString("loginAttemptBlocked",        errorInMethod);
        loginDescribeCustom        = messages.getNotNothingString("loginDescribeCustom",        errorInMethod);
        loginDescribeEmail         = messages.getNotNothingString("loginDescribeEmail",         errorInMethod);
        loginDescribeGoogle        = messages.getNotNothingString("loginDescribeGoogle",        errorInMethod);
        loginDescribeOpenID        = messages.getNotNothingString("loginDescribeOpenID",        errorInMethod);
        loginCanNot                = messages.getNotNothingString("loginCanNot",                errorInMethod);
        loginAreNot                = messages.getNotNothingString("loginAreNot",                errorInMethod);
        loginToLogIn               = messages.getNotNothingString("loginToLogIn",               errorInMethod);
        loginEmailAddress          = messages.getNotNothingString("loginEmailAddress",          errorInMethod);
        loginYourEmailAddress      = messages.getNotNothingString("loginYourEmailAddress",      errorInMethod);
        loginUserName              = messages.getNotNothingString("loginUserName",              errorInMethod);
        loginPassword              = messages.getNotNothingString("loginPassword",              errorInMethod);
        loginUserNameAndPassword   = messages.getNotNothingString("loginUserNameAndPassword",   errorInMethod);
        loginGoogleSignIn          = messages.getNotNothingString("loginGoogleSignIn",          errorInMethod);
        loginGoogleErddap          = messages.getNotNothingString("loginGoogleErddap",          errorInMethod);
        loginOpenID                = messages.getNotNothingString("loginOpenID",                errorInMethod);
        loginOpenIDOr              = messages.getNotNothingString("loginOpenIDOr",              errorInMethod);
        loginOpenIDCreate          = messages.getNotNothingString("loginOpenIDCreate",          errorInMethod);
        loginOpenIDFree            = messages.getNotNothingString("loginOpenIDFree",            errorInMethod);
        loginOpenIDSame            = messages.getNotNothingString("loginOpenIDSame",            errorInMethod);
        loginAs                    = messages.getNotNothingString("loginAs",                    errorInMethod);
        loginFailed                = messages.getNotNothingString("loginFailed",                errorInMethod);
        loginSucceeded             = messages.getNotNothingString("loginSucceeded",             errorInMethod);
        loginInvalid               = messages.getNotNothingString("loginInvalid",               errorInMethod);
        loginNot                   = messages.getNotNothingString("loginNot",                   errorInMethod);
        loginBack                  = messages.getNotNothingString("loginBack",                  errorInMethod);
        loginProblemExact          = messages.getNotNothingString("loginProblemExact",          errorInMethod);
        loginProblemExpire         = messages.getNotNothingString("loginProblemExpire",         errorInMethod);
        loginProblemGoogleAgain    = messages.getNotNothingString("loginProblemGoogleAgain",    errorInMethod);
        loginProblemSameBrowser    = messages.getNotNothingString("loginProblemSameBrowser",    errorInMethod);
        loginProblem3Times         = messages.getNotNothingString("loginProblem3Times",         errorInMethod);
        loginProblems              = messages.getNotNothingString("loginProblems",              errorInMethod);
        loginProblemsAfter         = messages.getNotNothingString("loginProblemsAfter",         errorInMethod);
        loginPublicAccess          = messages.getNotNothingString("loginPublicAccess",          errorInMethod);
        LogOut                     = messages.getNotNothingString("LogOut",                     errorInMethod);
        logout                     = messages.getNotNothingString("logout",                     errorInMethod);
        logoutOpenID               = messages.getNotNothingString("logoutOpenID",               errorInMethod);
        logoutSuccess              = messages.getNotNothingString("logoutSuccess",              errorInMethod);
        mag                        = messages.getNotNothingString("mag",                        errorInMethod);
        magAxisX                   = messages.getNotNothingString("magAxisX",                   errorInMethod);
        magAxisY                   = messages.getNotNothingString("magAxisY",                   errorInMethod);
        magAxisColor               = messages.getNotNothingString("magAxisColor",               errorInMethod);
        magAxisStickX              = messages.getNotNothingString("magAxisStickX",              errorInMethod);
        magAxisStickY              = messages.getNotNothingString("magAxisStickY",              errorInMethod);
        magAxisVectorX             = messages.getNotNothingString("magAxisVectorX",             errorInMethod);
        magAxisVectorY             = messages.getNotNothingString("magAxisVectorY",             errorInMethod);
        magAxisHelpGraphX          = messages.getNotNothingString("magAxisHelpGraphX",          errorInMethod);
        magAxisHelpGraphY          = messages.getNotNothingString("magAxisHelpGraphY",          errorInMethod);
        magAxisHelpMarkerColor     = messages.getNotNothingString("magAxisHelpMarkerColor",     errorInMethod);
        magAxisHelpSurfaceColor    = messages.getNotNothingString("magAxisHelpSurfaceColor",    errorInMethod);
        magAxisHelpStickX          = messages.getNotNothingString("magAxisHelpStickX",          errorInMethod);
        magAxisHelpStickY          = messages.getNotNothingString("magAxisHelpStickY",          errorInMethod);
        magAxisHelpMapX            = messages.getNotNothingString("magAxisHelpMapX",            errorInMethod);
        magAxisHelpMapY            = messages.getNotNothingString("magAxisHelpMapY",            errorInMethod);
        magAxisHelpVectorX         = messages.getNotNothingString("magAxisHelpVectorX",         errorInMethod);
        magAxisHelpVectorY         = messages.getNotNothingString("magAxisHelpVectorY",         errorInMethod);
        magAxisVarHelp             = messages.getNotNothingString("magAxisVarHelp",             errorInMethod);
        magAxisVarHelpGrid         = messages.getNotNothingString("magAxisVarHelpGrid",         errorInMethod);
        magConstraintHelp          = messages.getNotNothingString("magConstraintHelp",          errorInMethod);
        magDocumentation           = messages.getNotNothingString("magDocumentation",           errorInMethod);
        magDownload                = messages.getNotNothingString("magDownload",                errorInMethod);
        magDownloadTooltip         = messages.getNotNothingString("magDownloadTooltip",         errorInMethod);
        magFileType                = messages.getNotNothingString("magFileType",                errorInMethod);
        magGraphType               = messages.getNotNothingString("magGraphType",               errorInMethod);
        magGraphTypeTooltipGrid    = messages.getNotNothingString("magGraphTypeTooltipGrid",    errorInMethod);
        magGraphTypeTooltipTable   = messages.getNotNothingString("magGraphTypeTooltipTable",   errorInMethod);
        magGS                      = messages.getNotNothingString("magGS",                      errorInMethod);
        magGSMarkerType            = messages.getNotNothingString("magGSMarkerType",            errorInMethod);
        magGSSize                  = messages.getNotNothingString("magGSSize",                  errorInMethod);
        magGSColor                 = messages.getNotNothingString("magGSColor",                 errorInMethod);
        magGSColorBar              = messages.getNotNothingString("magGSColorBar",              errorInMethod);
        magGSColorBarTooltip       = messages.getNotNothingString("magGSColorBarTooltip",       errorInMethod);
        magGSContinuity            = messages.getNotNothingString("magGSContinuity",            errorInMethod);
        magGSContinuityTooltip     = messages.getNotNothingString("magGSContinuityTooltip",     errorInMethod);
        magGSScale                 = messages.getNotNothingString("magGSScale",                 errorInMethod);
        magGSScaleTooltip          = messages.getNotNothingString("magGSScaleTooltip",          errorInMethod);
        magGSMin                   = messages.getNotNothingString("magGSMin",                   errorInMethod);
        magGSMinTooltip            = messages.getNotNothingString("magGSMinTooltip",            errorInMethod);
        magGSMax                   = messages.getNotNothingString("magGSMax",                   errorInMethod);
        magGSMaxTooltip            = messages.getNotNothingString("magGSMaxTooltip",            errorInMethod);
        magGSNSections             = messages.getNotNothingString("magGSNSections",             errorInMethod);
        magGSNSectionsTooltip      = messages.getNotNothingString("magGSNSectionsTooltip",      errorInMethod);
        magGSLandMask              = messages.getNotNothingString("magGSLandMask",              errorInMethod);
        magGSLandMaskTooltipGrid   = messages.getNotNothingString("magGSLandMaskTooltipGrid",   errorInMethod);
        magGSLandMaskTooltipTable  = messages.getNotNothingString("magGSLandMaskTooltipTable",  errorInMethod);
        magGSVectorStandard        = messages.getNotNothingString("magGSVectorStandard",        errorInMethod);
        magGSVectorStandardTooltip = messages.getNotNothingString("magGSVectorStandardTooltip", errorInMethod);
        magGSYAscendingTooltip     = messages.getNotNothingString("magGSYAscendingTooltip",     errorInMethod);
        magGSYAxisMin              = messages.getNotNothingString("magGSYAxisMin",              errorInMethod);
        magGSYAxisMax              = messages.getNotNothingString("magGSYAxisMax",              errorInMethod);
        magGSYRangeMinTooltip      = messages.getNotNothingString("magGSYRangeMinTooltip",      errorInMethod); 
        magGSYRangeMaxTooltip      = messages.getNotNothingString("magGSYRangeMaxTooltip",      errorInMethod);
        magGSYRangeTooltip         = messages.getNotNothingString("magGSYRangeTooltip",         errorInMethod);        
        magItemFirst               = messages.getNotNothingString("magItemFirst",               errorInMethod);
        magItemPrevious            = messages.getNotNothingString("magItemPrevious",            errorInMethod);
        magItemNext                = messages.getNotNothingString("magItemNext",                errorInMethod);
        magItemLast                = messages.getNotNothingString("magItemLast",                errorInMethod);
        magJust1Value              = messages.getNotNothingString("magJust1Value",              errorInMethod);
        magRange                   = messages.getNotNothingString("magRange",                   errorInMethod);
        magRangeTo                 = messages.getNotNothingString("magRangeTo",                 errorInMethod);
        magRedraw                  = messages.getNotNothingString("magRedraw",                  errorInMethod);
        magRedrawTooltip           = messages.getNotNothingString("magRedrawTooltip",           errorInMethod);
        magTimeRange               = messages.getNotNothingString("magTimeRange",               errorInMethod);
        magTimeRangeFirst          = messages.getNotNothingString("magTimeRangeFirst",          errorInMethod);
        magTimeRangeBack           = messages.getNotNothingString("magTimeRangeBack",           errorInMethod);
        magTimeRangeForward        = messages.getNotNothingString("magTimeRangeForward",        errorInMethod);
        magTimeRangeLast           = messages.getNotNothingString("magTimeRangeLast",           errorInMethod);
        magTimeRangeTooltip        = messages.getNotNothingString("magTimeRangeTooltip",        errorInMethod);
        magTimeRangeTooltip2       = messages.getNotNothingString("magTimeRangeTooltip2",       errorInMethod);
        magTimesVary               = messages.getNotNothingString("magTimesVary",               errorInMethod);
        magViewUrl                 = messages.getNotNothingString("magViewUrl",                 errorInMethod);
        magZoom                    = messages.getNotNothingString("magZoom",                    errorInMethod);
        magZoomCenter              = messages.getNotNothingString("magZoomCenter",              errorInMethod);
        magZoomCenterTooltip       = messages.getNotNothingString("magZoomCenterTooltip",       errorInMethod);
        magZoomIn                  = messages.getNotNothingString("magZoomIn",                  errorInMethod);
        magZoomInTooltip           = messages.getNotNothingString("magZoomInTooltip",           errorInMethod);
        magZoomOut                 = messages.getNotNothingString("magZoomOut",                 errorInMethod);
        magZoomOutTooltip          = messages.getNotNothingString("magZoomOutTooltip",          errorInMethod);
        magZoomALittle             = messages.getNotNothingString("magZoomALittle",             errorInMethod);
        magZoomData                = messages.getNotNothingString("magZoomData",                errorInMethod);
        magZoomOutData             = messages.getNotNothingString("magZoomOutData",             errorInMethod);
        magGridTooltip             = messages.getNotNothingString("magGridTooltip",             errorInMethod);
        magTableTooltip            = messages.getNotNothingString("magTableTooltip",            errorInMethod);
        Math2.memoryTooMuchData    = messages.getNotNothingString("memoryTooMuchData",          errorInMethod);
        Math2.memoryArraySize      = messages.getNotNothingString("memoryArraySize",            errorInMethod);
      Math2.memoryThanCurrentlySafe= messages.getNotNothingString("memoryThanCurrentlySafe",    errorInMethod);
        Math2.memoryThanSafe       = messages.getNotNothingString("memoryThanSafe",             errorInMethod);
        metadataDownload           = messages.getNotNothingString("metadataDownload",           errorInMethod);
        moreInformation            = messages.getNotNothingString("moreInformation",            errorInMethod);
        MustBe.THERE_IS_NO_DATA    = messages.getNotNothingString("MustBeThereIsNoData",        errorInMethod);
        MustBe.NotNull             = messages.getNotNothingString("MustBeNotNull",              errorInMethod);
        MustBe.NotEmpty            = messages.getNotNothingString("MustBeNotEmpty",             errorInMethod);
        MustBe.InternalError       = messages.getNotNothingString("MustBeInternalError",        errorInMethod);
        MustBe.OutOfMemoryError    = messages.getNotNothingString("MustBeOutOfMemoryError",     errorInMethod);
        nMatching1                 = messages.getNotNothingString("nMatching1",                 errorInMethod);
        nMatching                  = messages.getNotNothingString("nMatching",                 errorInMethod);
        nMatchingAlphabetical      = messages.getNotNothingString("nMatchingAlphabetical",      errorInMethod);
        nMatchingMostRelevant      = messages.getNotNothingString("nMatchingMostRelevant",      errorInMethod);
        nMatchingPage              = messages.getNotNothingString("nMatchingPage",              errorInMethod);
        nMatchingCurrent           = messages.getNotNothingString("nMatchingCurrent",           errorInMethod);
        noDataFixedValue           = messages.getNotNothingString("noDataFixedValue",           errorInMethod);
        noDataNoLL                 = messages.getNotNothingString("noDataNoLL",                 errorInMethod);
        noDatasetWith              = messages.getNotNothingString("noDatasetWith",              errorInMethod);
        noPage1                    = messages.getNotNothingString("noPage1",                    errorInMethod);
        noPage2                    = messages.getNotNothingString("noPage2",                    errorInMethod);
        notAllowed                 = messages.getNotNothingString("notAllowed",                 errorInMethod);
        notAuthorized              = messages.getNotNothingString("notAuthorized",              errorInMethod);
        notAuthorizedForData       = messages.getNotNothingString("notAuthorizedForData",       errorInMethod);
        notAvailable               = messages.getNotNothingString("notAvailable",               errorInMethod);
        noXxx                      = messages.getNotNothingString("noXxx",                      errorInMethod);
        noXxxBecause               = messages.getNotNothingString("noXxxBecause",               errorInMethod);
        noXxxBecause2              = messages.getNotNothingString("noXxxBecause2",              errorInMethod);
        noXxxNotActive             = messages.getNotNothingString("noXxxNotActive",             errorInMethod);
        noXxxNoAxis1               = messages.getNotNothingString("noXxxNoAxis1",               errorInMethod);
        noXxxNoCdmDataType         = messages.getNotNothingString("noXxxNoCdmDataType",         errorInMethod);
        noXxxNoColorBar            = messages.getNotNothingString("noXxxNoColorBar",            errorInMethod);
        noXxxNoLL                  = messages.getNotNothingString("noXxxNoLL",                  errorInMethod);
        noXxxNoLLEvenlySpaced      = messages.getNotNothingString("noXxxNoLLEvenlySpaced",      errorInMethod);
        noXxxNoLLGt1               = messages.getNotNothingString("noXxxNoLLGt1",               errorInMethod);
        noXxxNoLLT                 = messages.getNotNothingString("noXxxNoLLT",                 errorInMethod);
        noXxxNoLonIn180            = messages.getNotNothingString("noXxxNoLonIn180",            errorInMethod);
        noXxxNoNonString           = messages.getNotNothingString("noXxxNoNonString",           errorInMethod);
        noXxxNo2NonString          = messages.getNotNothingString("noXxxNo2NonString",          errorInMethod);
        noXxxNoStation             = messages.getNotNothingString("noXxxNoStation",             errorInMethod);
        noXxxNoStationID           = messages.getNotNothingString("noXxxNoStationID",           errorInMethod);
        noXxxNoSubsetVariables     = messages.getNotNothingString("noXxxNoSubsetVariables",     errorInMethod);
        noXxxNoOLLSubsetVariables  = messages.getNotNothingString("noXxxNoOLLSubsetVariables",  errorInMethod);
        noXxxNoMinMax              = messages.getNotNothingString("noXxxNoMinMax",              errorInMethod);
        noXxxItsGridded            = messages.getNotNothingString("noXxxItsGridded",            errorInMethod);
        noXxxItsTabular            = messages.getNotNothingString("noXxxItsTabular",            errorInMethod);
        optional                   = messages.getNotNothingString("optional",                   errorInMethod);
        options                    = messages.getNotNothingString("options",                    errorInMethod);
        orRefineSearchWith         = messages.getNotNothingString("orRefineSearchWith",         errorInMethod);
        orRefineSearchWith += " ";
        orSearchWith               = messages.getNotNothingString("orSearchWith",               errorInMethod);
        orSearchWith += " ";
        orComma                    = messages.getNotNothingString("orComma",                    errorInMethod);
        orComma += " ";
        outOfDateHtml              = messages.getNotNothingString("outOfDateHtml",              errorInMethod);
        palettes                   = String2.split(messages.getNotNothingString("palettes",     errorInMethod), ',');
        palettes0 = new String[palettes.length + 1];
        palettes0[0] = "";
        System.arraycopy(palettes, 0, palettes0, 1, palettes.length);
        patientData                = messages.getNotNothingString("patientData",                errorInMethod);
        patientYourGraph           = messages.getNotNothingString("patientYourGraph",           errorInMethod);
        pdfWidths                  = String2.toIntArray(String2.split(messages.getNotNothingString("pdfWidths",  errorInMethod), ','));
        pdfHeights                 = String2.toIntArray(String2.split(messages.getNotNothingString("pdfHeights", errorInMethod), ','));
        percentEncode              = messages.getNotNothingString("percentEncode",              errorInMethod);
        pickADataset               = messages.getNotNothingString("pickADataset",               errorInMethod);
        protocolSearchHtml         = messages.getNotNothingString("protocolSearchHtml",         errorInMethod);
        protocolSearch2Html        = messages.getNotNothingString("protocolSearch2Html",        errorInMethod);
        protocolClick              = messages.getNotNothingString("protocolClick",              errorInMethod);
        queryError                 = messages.getNotNothingString("queryError",                 errorInMethod) + " ";
        Table.QUERY_ERROR = queryError;
        queryError180              = messages.getNotNothingString("queryError180",              errorInMethod);
        queryError1Value           = messages.getNotNothingString("queryError1Value",           errorInMethod);
        queryError1Var             = messages.getNotNothingString("queryError1Var",             errorInMethod);
        queryError2Var             = messages.getNotNothingString("queryError2Var",             errorInMethod);
        queryErrorActualRange      = messages.getNotNothingString("queryErrorActualRange",      errorInMethod);
        queryErrorAdjusted         = messages.getNotNothingString("queryErrorAdjusted",         errorInMethod);
        queryErrorAscending        = messages.getNotNothingString("queryErrorAscending",        errorInMethod);
        queryErrorConstraintNaN    = messages.getNotNothingString("queryErrorConstraintNaN",    errorInMethod);
        queryErrorEqualSpacing     = messages.getNotNothingString("queryErrorEqualSpacing",     errorInMethod);
        queryErrorExpectedAt       = messages.getNotNothingString("queryErrorExpectedAt",       errorInMethod);
        queryErrorFileType         = messages.getNotNothingString("queryErrorFileType",         errorInMethod);
        queryErrorInvalid          = messages.getNotNothingString("queryErrorInvalid",          errorInMethod);
        queryErrorLL               = messages.getNotNothingString("queryErrorLL",               errorInMethod);
        queryErrorLLGt1            = messages.getNotNothingString("queryErrorLLGt1",            errorInMethod);
        queryErrorLLT              = messages.getNotNothingString("queryErrorLLT",              errorInMethod);
        queryErrorNeverTrue        = messages.getNotNothingString("queryErrorNeverTrue",        errorInMethod);
        queryErrorNeverBothTrue    = messages.getNotNothingString("queryErrorNeverBothTrue",    errorInMethod);
        queryErrorNotAxis          = messages.getNotNothingString("queryErrorNotAxis",          errorInMethod);
        queryErrorNotExpectedAt    = messages.getNotNothingString("queryErrorNotExpectedAt",    errorInMethod);
        queryErrorNotFoundAfter    = messages.getNotNothingString("queryErrorNotFoundAfter",    errorInMethod);
        queryErrorOccursTwice      = messages.getNotNothingString("queryErrorOccursTwice",      errorInMethod);
        Table.ORDER_BY_CLOSEST_ERROR=messages.getNotNothingString("queryErrorOrderByClosest",   errorInMethod);
        Table.ORDER_BY_LIMIT_ERROR = messages.getNotNothingString("queryErrorOrderByLimit",     errorInMethod);
        queryErrorOrderByVariable  = messages.getNotNothingString("queryErrorOrderByVariable",  errorInMethod);
        queryErrorUnknownVariable  = messages.getNotNothingString("queryErrorUnknownVariable",  errorInMethod);

        queryErrorGrid1Axis        = messages.getNotNothingString("queryErrorGrid1Axis",        errorInMethod);
        queryErrorGridAmp          = messages.getNotNothingString("queryErrorGridAmp",          errorInMethod);
        queryErrorGridDiagnostic   = messages.getNotNothingString("queryErrorGridDiagnostic",   errorInMethod);
        queryErrorGridBetween      = messages.getNotNothingString("queryErrorGridBetween",      errorInMethod);
        queryErrorGridLessMin      = messages.getNotNothingString("queryErrorGridLessMin",      errorInMethod);
        queryErrorGridGreaterMax   = messages.getNotNothingString("queryErrorGridGreaterMax",   errorInMethod);
        queryErrorGridMissing      = messages.getNotNothingString("queryErrorGridMissing",      errorInMethod);
        queryErrorGridNoAxisVar    = messages.getNotNothingString("queryErrorGridNoAxisVar",    errorInMethod);
        queryErrorGridNoDataVar    = messages.getNotNothingString("queryErrorGridNoDataVar",    errorInMethod);
        queryErrorGridNotIdentical = messages.getNotNothingString("queryErrorGridNotIdentical", errorInMethod);
        queryErrorGridSLessS       = messages.getNotNothingString("queryErrorGridSLessS",       errorInMethod);
        queryErrorLastEndP         = messages.getNotNothingString("queryErrorLastEndP",         errorInMethod);
        queryErrorLastExpected     = messages.getNotNothingString("queryErrorLastExpected",     errorInMethod);
        queryErrorLastUnexpected   = messages.getNotNothingString("queryErrorLastUnexpected",   errorInMethod);
        queryErrorLastPMInvalid    = messages.getNotNothingString("queryErrorLastPMInvalid",    errorInMethod);
        queryErrorLastPMInteger    = messages.getNotNothingString("queryErrorLastPMInteger",    errorInMethod);        
        rangesFromTo               = messages.getNotNothingString("rangesFromTo",               errorInMethod);
        requestFormatExamplesHtml  = messages.getNotNothingString("requestFormatExamplesHtml",  errorInMethod);
        resetTheForm               = messages.getNotNothingString("resetTheForm",               errorInMethod);
        resetTheFormWas            = messages.getNotNothingString("resetTheFormWas",            errorInMethod);
        resourceNotFound           = messages.getNotNothingString("resourceNotFound",           errorInMethod);
        resultsFormatExamplesHtml  = messages.getNotNothingString("resultsFormatExamplesHtml",  errorInMethod);
        resultsOfSearchFor         = messages.getNotNothingString("resultsOfSearchFor",         errorInMethod);
        restfulInformationFormats  = messages.getNotNothingString("restfulInformationFormats",  errorInMethod);
        restfulViaService          = messages.getNotNothingString("restfulViaService",          errorInMethod);
        rows                       = messages.getNotNothingString("rows",                       errorInMethod);
        rssNo                      = messages.getNotNothingString("rssNo",                      errorInMethod);
        searchTitle                = messages.getNotNothingString("searchTitle",                errorInMethod);
        searchDoFullTextHtml       = messages.getNotNothingString("searchDoFullTextHtml",       errorInMethod);
        searchFullTextHtml         = messages.getNotNothingString("searchFullTextHtml",         errorInMethod);
        searchButton               = messages.getNotNothingString("searchButton",               errorInMethod);
        searchClickTip             = messages.getNotNothingString("searchClickTip",             errorInMethod);
        searchHintsLuceneTooltip   = messages.getNotNothingString("searchHintsLuceneTooltip",   errorInMethod);
        searchHintsOriginalTooltip = messages.getNotNothingString("searchHintsOriginalTooltip", errorInMethod);
        searchHintsTooltip         = messages.getNotNothingString("searchHintsTooltip",         errorInMethod);
        searchNotAvailable         = messages.getNotNothingString("searchNotAvailable",         errorInMethod);
        searchTip                  = messages.getNotNothingString("searchTip",                  errorInMethod);
        searchSpelling             = messages.getNotNothingString("searchSpelling",             errorInMethod);
        searchFewerWords           = messages.getNotNothingString("searchFewerWords",           errorInMethod);
        searchWithQuery            = messages.getNotNothingString("searchWithQuery",            errorInMethod);
        selectNext                 = messages.getNotNothingString("selectNext",                 errorInMethod);
        selectPrevious             = messages.getNotNothingString("selectPrevious",             errorInMethod);
        seeProtocolDocumentation   = messages.getNotNothingString("seeProtocolDocumentation",   errorInMethod);
        sosDescriptionHtml         = messages.getNotNothingString("sosDescriptionHtml",         errorInMethod);
        sosLongDescriptionHtml     = messages.getNotNothingString("sosLongDescriptionHtml",     errorInMethod); 
        sparqlP01toP02pre          = messages.getNotNothingString("sparqlP01toP02pre",          errorInMethod); 
        sparqlP01toP02post         = messages.getNotNothingString("sparqlP01toP02post",         errorInMethod); 
        ssUse                      = messages.getNotNothingString("ssUse",                      errorInMethod);
        ssUsePlain                 = XML.removeHTMLTags(ssUse);
        ssBePatient                = messages.getNotNothingString("ssBePatient",                errorInMethod);
        ssInstructionsHtml         = messages.getNotNothingString("ssInstructionsHtml",         errorInMethod);
        standardShortDescriptionHtml=messages.getNotNothingString("standardShortDescriptionHtml",errorInMethod);
        standardShortDescriptionHtml=setup.getString(             "standardShortDescriptionHtml",standardShortDescriptionHtml);
        standardLicense            = messages.getNotNothingString("standardLicense",            errorInMethod);
        standardLicense            = setup.getString(             "standardLicense",            standardLicense);
        standardContact            = messages.getNotNothingString("standardContact",            errorInMethod);
        standardContact            = setup.getString(             "standardContact",            standardContact);
        standardDataLicenses       = messages.getNotNothingString("standardDataLicenses",       errorInMethod);
        standardDataLicenses       = setup.getString(             "standardDataLicenses",       standardDataLicenses);
        standardDisclaimerOfExternalLinks=messages.getNotNothingString("standardDisclaimerOfExternalLinks", errorInMethod);
        standardDisclaimerOfExternalLinks=setup.getString(             "standardDisclaimerOfExternalLinks", standardDisclaimerOfExternalLinks);
        standardDisclaimerOfEndorsement  =messages.getNotNothingString("standardDisclaimerOfEndorsement",   errorInMethod);
        standardDisclaimerOfEndorsement  =setup.getString(             "standardDisclaimerOfEndorsement",   standardDisclaimerOfEndorsement);
        standardGeneralDisclaimer  = messages.getNotNothingString("standardGeneralDisclaimer",  errorInMethod);
        standardGeneralDisclaimer  = setup.getString(             "standardGeneralDisclaimer",  standardGeneralDisclaimer);
        standardPrivacyPolicy      = messages.getNotNothingString("standardPrivacyPolicy",      errorInMethod);
        standardPrivacyPolicy      = setup.getString(             "standardPrivacyPolicy",      standardPrivacyPolicy);

        startHeadHtml              = messages.getNotNothingString("startHeadHtml5",           errorInMethod);
        startHeadHtml              = setup.getString(             "startHeadHtml5",           startHeadHtml);
        startBodyHtml              = messages.getNotNothingString("startBodyHtml5",           errorInMethod);
        startBodyHtml              = setup.getString(             "startBodyHtml5",           startBodyHtml);
        endBodyHtml                = messages.getNotNothingString("endBodyHtml5",             errorInMethod);
        endBodyHtml                = setup.getString(             "endBodyHtml5",             endBodyHtml);
        endBodyHtml                = String2.replaceAll(endBodyHtml, "&erddapVersion;", erddapVersion);
        //ensure HTML5
        Test.ensureTrue(startHeadHtml.startsWith("<!DOCTYPE html>"),
            "<startHeadHtml> in setup.xml must start with \"<!DOCTYPE html>\".");

        statusHtml                 = messages.getNotNothingString("statusHtml",                 errorInMethod);
        submit                     = messages.getNotNothingString("submit",                     errorInMethod);
        submitTooltip              = messages.getNotNothingString("submitTooltip",              errorInMethod);
        subscriptionsTitle         = messages.getNotNothingString("subscriptionsTitle",         errorInMethod);
        subscriptionAdd            = messages.getNotNothingString("subscriptionAdd",            errorInMethod);
        subscriptionValidate       = messages.getNotNothingString("subscriptionValidate",       errorInMethod);
        subscriptionList           = messages.getNotNothingString("subscriptionList",           errorInMethod);
        subscriptionRemove         = messages.getNotNothingString("subscriptionRemove",         errorInMethod);
        subscription0Html          = messages.getNotNothingString("subscription0Html",          errorInMethod);
        subscription1Html          = messages.getNotNothingString("subscription1Html",          errorInMethod);
        subscription2Html          = messages.getNotNothingString("subscription2Html",          errorInMethod);
        subscriptionAbuse          = messages.getNotNothingString("subscriptionAbuse",          errorInMethod);
        subscriptionAddError       = messages.getNotNothingString("subscriptionAddError",       errorInMethod);
        subscriptionAddHtml        = messages.getNotNothingString("subscriptionAddHtml",        errorInMethod);
        subscriptionAdd2           = messages.getNotNothingString("subscriptionAdd2",           errorInMethod);
        subscriptionAddSuccess     = messages.getNotNothingString("subscriptionAddSuccess",     errorInMethod);
        subscriptionEmail          = messages.getNotNothingString("subscriptionEmail",          errorInMethod);
        subscriptionEmailInvalid   = messages.getNotNothingString("subscriptionEmailInvalid",   errorInMethod);
        subscriptionEmailTooLong   = messages.getNotNothingString("subscriptionEmailTooLong",   errorInMethod);
        subscriptionEmailUnspecified=messages.getNotNothingString("subscriptionEmailUnspecified",errorInMethod);
        subscriptionIDInvalid      = messages.getNotNothingString("subscriptionIDInvalid",      errorInMethod);
        subscriptionIDTooLong      = messages.getNotNothingString("subscriptionIDTooLong",      errorInMethod);
        subscriptionIDUnspecified  = messages.getNotNothingString("subscriptionIDUnspecified",  errorInMethod);
        subscriptionKeyInvalid     = messages.getNotNothingString("subscriptionKeyInvalid",     errorInMethod);
        subscriptionKeyUnspecified = messages.getNotNothingString("subscriptionKeyUnspecified", errorInMethod);
        subscriptionListError      = messages.getNotNothingString("subscriptionListError",      errorInMethod);
        subscriptionListHtml       = messages.getNotNothingString("subscriptionListHtml",       errorInMethod);
        subscriptionListSuccess    = messages.getNotNothingString("subscriptionListSuccess",    errorInMethod);
        subscriptionRemoveError    = messages.getNotNothingString("subscriptionRemoveError",    errorInMethod);
        subscriptionRemoveHtml     = messages.getNotNothingString("subscriptionRemoveHtml",     errorInMethod);
        subscriptionRemove2        = messages.getNotNothingString("subscriptionRemove2",        errorInMethod);
        subscriptionRemoveSuccess  = messages.getNotNothingString("subscriptionRemoveSuccess",  errorInMethod);
        subscriptionRSS            = messages.getNotNothingString("subscriptionRSS",            errorInMethod);
        subscriptionsNotAvailable  = messages.getNotNothingString("subscriptionsNotAvailable",  errorInMethod);
        subscriptionUrlHtml        = messages.getNotNothingString("subscriptionUrlHtml",        errorInMethod);
        subscriptionUrlInvalid     = messages.getNotNothingString("subscriptionUrlInvalid",     errorInMethod);
        subscriptionUrlTooLong     = messages.getNotNothingString("subscriptionUrlTooLong",     errorInMethod);
        subscriptionValidateError  = messages.getNotNothingString("subscriptionValidateError",  errorInMethod);
        subscriptionValidateHtml   = messages.getNotNothingString("subscriptionValidateHtml",   errorInMethod);
        subscriptionValidateSuccess= messages.getNotNothingString("subscriptionValidateSuccess",errorInMethod);
        subset                     = messages.getNotNothingString("subset",                     errorInMethod);
        subsetSelect               = messages.getNotNothingString("subsetSelect",               errorInMethod);
        subsetNMatching            = messages.getNotNothingString("subsetNMatching",            errorInMethod);
        subsetInstructions         = messages.getNotNothingString("subsetInstructions",         errorInMethod);
        subsetOption               = messages.getNotNothingString("subsetOption",               errorInMethod);
        subsetOptions              = messages.getNotNothingString("subsetOptions",              errorInMethod);
        subsetRefineMapDownload    = messages.getNotNothingString("subsetRefineMapDownload",    errorInMethod);
        subsetRefineSubsetDownload = messages.getNotNothingString("subsetRefineSubsetDownload", errorInMethod);
        subsetClickResetClosest    = messages.getNotNothingString("subsetClickResetClosest",    errorInMethod);
        subsetClickResetLL         = messages.getNotNothingString("subsetClickResetLL",         errorInMethod);
        subsetMetadata             = messages.getNotNothingString("subsetMetadata",             errorInMethod);
        subsetCount                = messages.getNotNothingString("subsetCount",                errorInMethod);
        subsetPercent              = messages.getNotNothingString("subsetPercent",              errorInMethod);
        subsetViewSelect           = messages.getNotNothingString("subsetViewSelect",           errorInMethod);
        subsetViewSelectDistinctCombos= messages.getNotNothingString("subsetViewSelectDistinctCombos",errorInMethod);
        subsetViewSelectRelatedCounts = messages.getNotNothingString("subsetViewSelectRelatedCounts", errorInMethod);
        subsetWhen                 = messages.getNotNothingString("subsetWhen",                 errorInMethod);
        subsetWhenNoConstraints    = messages.getNotNothingString("subsetWhenNoConstraints",    errorInMethod);
        subsetWhenCounts           = messages.getNotNothingString("subsetWhenCounts",           errorInMethod);
        subsetComboClickSelect     = messages.getNotNothingString("subsetComboClickSelect",     errorInMethod);
        subsetNVariableCombos      = messages.getNotNothingString("subsetNVariableCombos",      errorInMethod);
        subsetShowingAllRows       = messages.getNotNothingString("subsetShowingAllRows",       errorInMethod);
        subsetShowingNRows         = messages.getNotNothingString("subsetShowingNRows",         errorInMethod);
        subsetChangeShowing        = messages.getNotNothingString("subsetChangeShowing",        errorInMethod);
        subsetNRowsRelatedData     = messages.getNotNothingString("subsetNRowsRelatedData",     errorInMethod);
        subsetViewRelatedChange    = messages.getNotNothingString("subsetViewRelatedChange",    errorInMethod);
        subsetTotalCount           = messages.getNotNothingString("subsetTotalCount",           errorInMethod);
        subsetView                 = messages.getNotNothingString("subsetView",                 errorInMethod);
        subsetViewCheck            = messages.getNotNothingString("subsetViewCheck",            errorInMethod);
        subsetViewCheck1           = messages.getNotNothingString("subsetViewCheck1",           errorInMethod);
        subsetViewDistinctMap      = messages.getNotNothingString("subsetViewDistinctMap",      errorInMethod);
        subsetViewRelatedMap       = messages.getNotNothingString("subsetViewRelatedMap",       errorInMethod);
        subsetViewDistinctDataCounts= messages.getNotNothingString("subsetViewDistinctDataCounts",errorInMethod);
        subsetViewDistinctData     = messages.getNotNothingString("subsetViewDistinctData",     errorInMethod);
        subsetViewRelatedDataCounts= messages.getNotNothingString("subsetViewRelatedDataCounts",errorInMethod);
        subsetViewRelatedData      = messages.getNotNothingString("subsetViewRelatedData",      errorInMethod);
        subsetViewDistinctMapTooltip       = messages.getNotNothingString("subsetViewDistinctMapTooltip",       errorInMethod);
        subsetViewRelatedMapTooltip        = messages.getNotNothingString("subsetViewRelatedMapTooltip",        errorInMethod);
        subsetViewDistinctDataCountsTooltip= messages.getNotNothingString("subsetViewDistinctDataCountsTooltip",errorInMethod);
        subsetViewDistinctDataTooltip      = messages.getNotNothingString("subsetViewDistinctDataTooltip",      errorInMethod);
        subsetViewRelatedDataCountsTooltip = messages.getNotNothingString("subsetViewRelatedDataCountsTooltip", errorInMethod);
        subsetViewRelatedDataTooltip       = messages.getNotNothingString("subsetViewRelatedDataTooltip",       errorInMethod);
        subsetWarn                 = messages.getNotNothingString("subsetWarn",                 errorInMethod);                    
        subsetWarn10000            = messages.getNotNothingString("subsetWarn10000",            errorInMethod);
        subsetTooltip              = messages.getNotNothingString("subsetTooltip",              errorInMethod);
        subsetNotSetUp             = messages.getNotNothingString("subsetNotSetUp",             errorInMethod);
        subsetLongNotShown         = messages.getNotNothingString("subsetLongNotShown",         errorInMethod);

        tabledapVideoIntro         = messages.getNotNothingString("tabledapVideoIntro",         errorInMethod);
        theLongDescriptionHtml     = messages.getNotNothingString("theLongDescriptionHtml",     errorInMethod);
        Then                       = messages.getNotNothingString("Then",                       errorInMethod);
        unknownDatasetID           = messages.getNotNothingString("unknownDatasetID",           errorInMethod);
        unknownProtocol            = messages.getNotNothingString("unknownProtocol",            errorInMethod);
        unsupportedFileType        = messages.getNotNothingString("unsupportedFileType",        errorInMethod);
        String tUpdateUrls[]       = String2.split(
                                     messages.getNotNothingString("updateUrls",                 errorInMethod) + "\n", '\n'); // +\n since xml content is trimmed.
        updateUrlsSkipAttributes   = StringArray.arrayFromCSV(
                                     messages.getNotNothingString("updateUrlsSkipAttributes",   errorInMethod));
        viewAllDatasetsHtml        = messages.getNotNothingString("viewAllDatasetsHtml",        errorInMethod);
        waitThenTryAgain           = messages.getNotNothingString("waitThenTryAgain",           errorInMethod);
        gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.waitThenTryAgain = waitThenTryAgain;
        warning                    = messages.getNotNothingString("warning",                    errorInMethod);
        wcsDescriptionHtml         = messages.getNotNothingString("wcsDescriptionHtml",         errorInMethod);
        wcsLongDescriptionHtml     = messages.getNotNothingString("wcsLongDescriptionHtml",     errorInMethod); 
        wmsDescriptionHtml         = messages.getNotNothingString("wmsDescriptionHtml",         errorInMethod);
        wmsInstructions            = messages.getNotNothingString("wmsInstructions",            errorInMethod); 
        wmsLongDescriptionHtml     = messages.getNotNothingString("wmsLongDescriptionHtml",     errorInMethod); 
        wmsManyDatasets            = messages.getNotNothingString("wmsManyDatasets",            errorInMethod); 

        Test.ensureEqual(imageWidths.length,  3, "imageWidths.length must be 3.");
        Test.ensureEqual(imageHeights.length, 3, "imageHeights.length must be 3.");
        Test.ensureEqual(pdfWidths.length,    3, "pdfWidths.length must be 3.");
        Test.ensureEqual(pdfHeights.length,   3, "pdfHeights.length must be 3.");

        Test.ensureEqual(tUpdateUrls.length % 3, 0, "updateUrls must have 1 or more groups of 3 lines: from, to, blank.");
        int nUpdateUrls = tUpdateUrls.length / 3;
        updateUrlsFrom = new String[nUpdateUrls];
        updateUrlsTo   = new String[nUpdateUrls];
        for (int i = 0; i < nUpdateUrls; i++) {
            int i3 = i * 3;
            updateUrlsFrom[i] = tUpdateUrls[i3];
            updateUrlsTo[  i] = tUpdateUrls[i3 + 1];
            Test.ensureTrue( String2.isSomething(tUpdateUrls[ i3]),   "updateUrls line #" + (i3 + 0) + " is empty.");
            Test.ensureTrue( String2.isSomething(tUpdateUrls[ i3+1]), "updateUrls line #" + (i3 + 0) + " is empty.");
            Test.ensureEqual(tUpdateUrls[i3+2], "", "updateUrls line #" + (i3 + 0) + " isn't empty.");
        }       

        for (int p = 0; p < palettes.length; p++) {
            String tName = fullPaletteDirectory + palettes[p] + ".cpt";
            Test.ensureTrue(File2.isFile(tName),
                "\"" + palettes[p] + 
                "\" is listed in <palettes>, but there is no file " + tName);
        }

        //try to create an nc4 file
accessibleViaNC4 = ".nc4 is not yet supported.";
/* DISABLED until nc4 is thread safe -- next netcdf-java
        String testNc4Name = fullTestCacheDirectory + 
            "testNC4_" + Calendar2.getCompactCurrentISODateTimeStringLocal() + ".nc";
        //String2.log("testNc4Name=" + testNc4Name);
        try {
            NetcdfFileWriter nc = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf4, testNc4Name);
            try {
                Group rootGroup = nc.addGroup(null, "");
                nc.setFill(false);
            
                int nRows = 4;
                Dimension dimension  = nc.addDimension(rootGroup, "row", nRows);
                Variable var = nc.addVariable(rootGroup, "myLongs", 
                    DataType.getType(long.class), Arrays.asList(dimension)); 

                //leave "define" mode
                nc.create();  //error is thrown here if netcdf-c not found

                //write the data
                Array array = Array.factory(long.class, new int[]{nRows}, new long[]{0,1,2,3});
                nc.write(var, new int[]{0}, array);

                //if close throws Throwable, it is trouble
                nc.close(); //it calls flush() and doesn't like flush called separately

                //success!
                accessibleViaNC4 = "";
                String2.log(".nc4 files can be created in this ERDDAP installation.");

            } catch (Throwable t2) {
                //try to close the file
                try {
                    nc.close(); //it calls flush() and doesn't like flush called separately
                } catch (Throwable t3) {
                    //don't care
                }

                throw t2;
            }

        } catch (Throwable t) {
            accessibleViaNC4 = String2.canonical(
                MessageFormat.format(noXxxBecause2, ".nc4",
                    resourceNotFound + " netcdf-c library"));
            String2.log(t.toString() + "\n" + accessibleViaNC4);
        }
//        File2.delete(testNc4Name);
*/

        ampLoginInfoPo = startBodyHtml.indexOf(ampLoginInfo); 
        //String2.log("ampLoginInfoPo=" + ampLoginInfoPo);

        searchHintsTooltip = 
            "<div class=\"standard_max_width\">" +
            searchHintsTooltip + "\n" +
            (useLuceneSearchEngine? searchHintsLuceneTooltip : searchHintsOriginalTooltip) +
            "</div>";
        advancedSearchDirections = String2.replaceAll(advancedSearchDirections, "&searchButton;", searchButton);

        //always non-https: url
        convertOceanicAtmosphericAcronymsService      = MessageFormat.format(convertOceanicAtmosphericAcronymsService, erddapUrl) + "\n";
        convertOceanicAtmosphericVariableNamesService = MessageFormat.format(convertOceanicAtmosphericVariableNamesService, erddapUrl) + "\n";
        convertFipsCountyService = MessageFormat.format(convertFipsCountyService, erddapUrl) + "\n";
        convertKeywordsService   = MessageFormat.format(convertKeywordsService,   erddapUrl) + "\n";
        convertTimeNotes         = MessageFormat.format(convertTimeNotes,         erddapUrl, convertTimeUnitsHelp) + "\n";
        convertTimeService       = MessageFormat.format(convertTimeService,       erddapUrl) + "\n"; 
        convertUnitsFilter       = MessageFormat.format(convertUnitsFilter,       erddapUrl, units_standard) + "\n";
        convertUnitsService      = MessageFormat.format(convertUnitsService,      erddapUrl) + "\n"; 

        //standardContact is used by legal
        String tEmail = SSR.getSafeEmailAddress(adminEmail);
        filesDocumentation = String2.replaceAll(filesDocumentation, "&adminEmail;", tEmail); 
        standardContact    = String2.replaceAll(standardContact,    "&adminEmail;", tEmail);
        legal = String2.replaceAll(legal,"[standardContact]",                   standardContact                   + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDataLicenses]",              standardDataLicenses              + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDisclaimerOfExternalLinks]", standardDisclaimerOfExternalLinks + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDisclaimerOfEndorsement]",   standardDisclaimerOfEndorsement   + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardPrivacyPolicy]",             standardPrivacyPolicy             + "\n\n"); 

        loginProblems      = String2.replaceAll(loginProblems,      "&cookiesHelp;",   cookiesHelp);
        loginProblems      = String2.replaceAll(loginProblems,      "&adminContact;",  adminContact()) + "\n\n"; 
        loginProblemsAfter = String2.replaceAll(loginProblemsAfter, "&adminContact;",  adminContact()) + "\n\n"; 
        loginPublicAccess += "\n"; 
        logoutSuccess += "\n"; 

        doWithGraphs = String2.replaceAll(doWithGraphs, "&ssUse;", slideSorterActive? ssUse : "");

        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&ssUse;", slideSorterActive? ssUse : "");
        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&requestFormatExamplesHtml;", requestFormatExamplesHtml);
        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&resultsFormatExamplesHtml;", resultsFormatExamplesHtml);

        standardShortDescriptionHtml = String2.replaceAll(standardShortDescriptionHtml, "&convertTimeReference;", convertersActive? convertTimeReference : "");
        standardShortDescriptionHtml = String2.replaceAll(standardShortDescriptionHtml, "&wmsManyDatasets;", wmsActive? wmsManyDatasets : "");

        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "[standardShortDescriptionHtml]", standardShortDescriptionHtml);
        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "&requestFormatExamplesHtml;",    requestFormatExamplesHtml);
        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "&resultsFormatExamplesHtml;",    resultsFormatExamplesHtml);

        try {
            computerName = System.getenv("COMPUTERNAME");  //windows 
            if (computerName == null)
                computerName = System.getenv("HOSTNAME");  //linux
            if (computerName == null)
                computerName = java.net.InetAddress.getLocalHost().getHostName(); //coastwatch.pfeg.noaa.gov
            if (computerName == null)
                computerName = "";
            int dotPo = computerName.indexOf('.');
            if (dotPo > 0)
                computerName = computerName.substring(0, dotPo);
        } catch (Throwable t2) {
            computerName = "";
        }


        //**************************************************************** 
        //other initialization

        //trigger CfToGcmd initialization to ensure CfToGcmd.txt file is valid.
        String testCfToGcmd[] = CfToFromGcmd.cfToGcmd("sea_water_temperature");
        Test.ensureTrue(testCfToGcmd.length > 0, 
            "testCfToGcmd=" + String2.toCSSVString(testCfToGcmd));

        //successfully finished
        String2.log("*** " + erdStartup + " finished successfully." + eol);

    } catch (Throwable t) {
        errorInMethod = "ERROR during " + erdStartup + ":\n" + 
            errorInMethod + "\n" + 
            MustBe.throwableToString(t);
        System.out.println(errorInMethod);
//        if (String2.logFileName() != null) 
//            String2.log(errorInMethod);
//        String2.returnLoggingToSystemOut();
        throw new RuntimeException(errorInMethod);
    }

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
        return loggedInAs == null? baseUrl : baseHttpsUrl; //works because of loggedInAsHttps
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
        return loggedInAs == null? erddapUrl : erddapHttpsUrl;  //works because of loggedInAsHttps
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
        return loggedInAs == null? imageDirUrl : imageDirHttpsUrl; //works because of loggedInAsHttps
    }

    /** 
     * This returns the html needed to display the external.png image 
     * with the warning that the link is to an external web site.
     *
     * @param tErddapUrl
     * @return the html needed to display the external.png image and messages.
     */
    public static String externalLinkHtml(String tErddapUrl) {
        return 
            "<img\n" +
            "    src=\"" + tErddapUrl + "/images/external.png\" " +
                "alt=\"" + externalLink + "\"\n" + 
            "    title=\"" + externalWebSite + "\">";
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP.
     * 
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHere() {
        return 
            "\n<h1>" + ProgramName + "</h1>\n";
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
            "\n<h1 class=\"nowrap\">" + erddapHref(erddapUrl(loggedInAs)) +
            " &gt; " + protocol + "</h1>\n";
    }

    /** This returns a not-yet-HTML-encoded protocol URL.
     * You may want to encode it with XML.encodeAsHTML(url)
     */
    public static String protocolUrl(String tErddapUrl, String protocol) {
        return tErddapUrl + "/" + protocol + 
            (protocol.equals("files")? "/" : "/index.html") +
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
            "\n<h1 class=\"nowrap\">" + erddapHref(tErddapUrl) +
            "\n &gt; <a rel=\"contents\" " +
                "href=\"" + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol)) +
                "\">" + protocol + "</a>" +
            "\n &gt; " + datasetID + "</h1>\n";
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
            "\n<h1 class=\"nowrap\">" + erddapHref(tErddapUrl) + 
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
            "\n<h1 class=\"nowrap\">" + erddapHref(tErddapUrl) + 
            "\n &gt; <a rel=\"contents\" " +
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
     *     For plain text, generate html from XML.encodeAsPreHTML(plainText, 82).
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
            destinationDataTypeClass == long.class? "long" :   
            destinationDataTypeClass == char.class? "char" :  //???   
            OpendapHelper.getAtomicType(destinationDataTypeClass);

        StringBuilder sb = OpendapHelper.dasToStringBuilder(
            destType + " " + destinationName, attributes, false); //false, do encoding below
        //String2.log("htmlTooltipImage sb=" + sb.toString());
        return htmlTooltipImage(loggedInAs, 
            "<div class=\"standard_max_width\">" + XML.encodeAsPreHTML(sb.toString()) +
            "</div>");
    }


    /**
     * This sends the specified email to one or more emailAddresses.
     *
     * @param emailAddressCsv  comma-separated list (may have ", ")
     * @return an error message ("" if no error).
     *     If emailAddress is null or "", this logs the message and returns "".
     */
    public static String email(String emailAddressCsv, String subject, String content) {
        return email(String2.split(emailAddressCsv, ','), subject, content);
    }


    /**
     * This sends the specified email to the emailAddresses.
     * <br>This won't throw an exception if trouble.
     * <br>This method always prepends the subject and content with [erddapUrl],
     *   so that it will be clear which ERDDAP this came from 
     *   (in case you administer multiple ERDDAPs).
     * <br>This method always logs that an email was sent: to whom and the subject, 
     *   but not the content.
     * <br>This method logs all emails to the email log, e.g., 
     *   (bigParentDirectory)/emailLog2009-01.txt
     *
     * @param emailAddresses   each e.g., john.doe@company.com
     * @param subject If error, recommended: "Error in [someClass]".
     *   If this starts with EDStatic.DONT_LOG_THIS_EMAIL, this email won't be logged
     *   (which is useful for confidential emails).
     * @param content If error, recommended: MustBe.throwableToString(t);
     * @return an error message ("" if no error).
     *     If emailAddresses is null or length==0, this logs the message and returns "".
     */
    public static String email(String emailAddresses[], String subject, String content) {

        //write the email to the log
        String emailAddressesCSSV = String2.toCSSVString(emailAddresses);
        String localTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
        boolean logIt = !subject.startsWith(DONT_LOG_THIS_EMAIL);
        if (!logIt) 
            subject = subject.substring(DONT_LOG_THIS_EMAIL.length());
        subject = (computerName.length() > 0? computerName + " ": "") + "ERDDAP: " + 
            String2.replaceAll(subject, '\n', ' ');

        //almost always write to emailLog
        try {
            //Always note that email sent in regular log.
            String2.log("Emailing \"" + subject + "\" to " + emailAddressesCSSV);

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
            emailLogFile.write(
"\n==== BEGIN =====================================================================" +
"\n     To: " + emailAddressesCSSV + 
"\nSubject: " + subject +  //always non-https url
"\n   Date: " + localTime + 
"\n--------------------------------------------------------------------------------" +
(logIt?
"\n" + erddapUrl + " reports:" +  //always non-https url
"\n" + content : 
"\n[CONFIDENTIAL]") +
"\n==== END =======================================================================" +
"\n");
            emailLogFile.flush();

        } catch (Throwable t) {
            try {
                String2.log(MustBe.throwable("Error: Writing to emailLog failed.", t));
            } catch (Throwable t2) {
            }
            if (emailLogFile != null) {
                try {emailLogFile.close(); 
                } catch (Throwable t3) {
                }
                emailLogFile = null;
            }
        }

        //done?
        if (emailAddressesCSSV == null || emailAddressesCSSV.length() == 0 ||
            emailSmtpHost == null || emailSmtpHost.length() == 0) 
            return "";


        //send email
        String errors = "";
        try {
            //catch common problem: sending email to one invalid address
            if (emailAddresses.length == 1 &&
                (!String2.isEmailAddress(emailAddresses[0]) ||
                 emailAddresses[0].startsWith("nobody@") || 
                 emailAddresses[0].startsWith("your.name") || 
                 emailAddresses[0].startsWith("your.email"))) {
                errors = "Error in EDStatic.email: invalid emailAddresses=" + emailAddressesCSSV;
                String2.log(errors);
            }

            if (errors.length() == 0)
//??? THREAD SAFE? SYNCHRONIZED? 
//I don't think use of this needs to be synchronized. I could be wrong. I haven't tested.
                SSR.sendEmail(emailSmtpHost, emailSmtpPort, emailUserName, 
                    emailPassword, emailProperties, emailFromAddress, emailAddressesCSSV, 
                    subject, 
                    erddapUrl + " reports:\n" + content); //always non-https url
        } catch (Throwable t) {
            String msg = "Error: Sending email to " + emailAddressesCSSV + " failed";
            try {String2.log(MustBe.throwable(msg, t));
            } catch (Throwable t4) {
            }
            errors = msg + ": " + t.toString() + "\n";
        }

        //write errors to email log
        if (errors.length() > 0 && emailLogFile != null) {
            try {
                //do in one write encourages threads not to intermingle   (or synchronize on emailLogFile?)
                emailLogFile.write("\n********** ERRORS **********\n" + errors);
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

        return errors;
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
        if (nBytes < Math2.ensureMemoryAvailableTrigger) 
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
        if (memoryInUse + nBytes <= Math2.maxSafeMemory)  //it'll work
            return;

        //lots of memory is in use
        //is the request is too big for right now?
        Math2.gcAndWait();  //part of ensureMemoryAvailable: lots of memory in use
        memoryInUse = Math2.getMemoryInUse();
        if (memoryInUse + nBytes > Math2.maxSafeMemory) {
            //eek! not enough memory! 
            //Wait, then try gc again and hope that some other request requiring lots of memory will finish.
            //If nothing else, this 1 second delay will delay another request by same user (e.g., programmatic re-request)
            Math2.sleep(1000);
            Math2.gcAndWait(); //in ensureMemoryIsAvailable, lots of memory in use
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
        sb.append("Current time is " + Calendar2.getCurrentISODateTimeStringLocalTZ()  + "\n");
        sb.append("Startup was at  " + startupLocalDateTime + "\n");
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
        sb.append(errorsDuringMajorReload);
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
        if (majorLoadDatasetsTimeSeriesSB.length() > 0) {
            sb.append(
"Major LoadDatasets Time Series: MLD    Datasets Loaded    Requests (medianTime in seconds)     Number of Threads      Memory (MB)\n" +
"  timestamp                    time   nTry nFail nTotal  nSuccess (median) nFailed (median)  tomWait inotify other  inUse highWater\n");
            sb.append(majorLoadDatasetsTimeSeriesSB);
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

        sb.append("Response Failed Time Distribution (since last major LoadDatasets):\n");
        sb.append(String2.getDistributionStatistics(failureTimesDistributionLoadDatasets)); sb.append('\n');
        sb.append("Response Failed Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(failureTimesDistribution24)); sb.append('\n');
        sb.append("Response Failed Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(failureTimesDistributionTotal)); sb.append('\n');
        sb.append('\n');

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
     * @return null (using http), 
     *    loggedInAsHttps (using https and not logged in), 
     *    or userName (using https and logged in).
     */
    public static String getLoggedInAs(HttpServletRequest request) {
        if (request == null)
            return null;

        //request is via http? treat as not logged in
        String fullRequestUrl = request.getRequestURL().toString(); //has proxied port#, e.g. :8080
        if (!fullRequestUrl.startsWith("https://")) 
            return null;

        //request is via https, but authentication=""?
        if (authentication.length() == 0)
            return loggedInAsHttps;

        //see if user is logged in
        //NOTE: session is associated with https urls, not http urls!
        //  So user only appears logged in to https urls.
        HttpSession session = request.getSession(false); //don't make one if none already
        //String2.log("session=" + (session==null? "null" : session.getServletContext().getServletContextName()));

        if (session == null)
            return loggedInAsHttps;  

        //session != null
        String loggedInAs = null;
        if (authentication.equals("custom") ||
            authentication.equals("email") ||
            authentication.equals("google")) {
            loggedInAs = (String)session.getAttribute("loggedInAs:" + warName);

        //} else if (authentication.equals("openid")) 
        //    loggedInAs = OpenIdFilter.getCurrentUser(session);
        }

        //ensure printable characters only (which makes loggedInAsSuperuser special)
        return loggedInAs == null?
            loggedInAsHttps : 
            String2.justPrintable(loggedInAs);
    }

    /**
     * This generates a nonce (a long "random" string related to basis).
     */
    public static String nonce(String basis) {
        return String2.passwordDigest("SHA-256",
            Math2.random(Integer.MAX_VALUE) + "_" + 
            System.currentTimeMillis() + "_" +
            basis + "_" + flagKeyKey); 
    }

    /** This allows LoadDatasets to set EDStatic.userHashMap (which is private).
     * There is no getUserHashMap (so info remains private).
     * MD5'd and SHA256'd passwords should all already be lowercase.
     */
    public static void setUserHashMap(HashMap tUserHashMap) {
        userHashMap = tUserHashMap;
    }

    /**
     * This returns true if the plaintextPassword (after passwordEncoding as 
     * specified in setup.xml) matches the stored password for user.
     *
     * @param username the user's log in name
     * @param plaintextPassword that the user entered on a log-in form
     * @return true if the plaintextPassword (after passwordEncoding as 
     *    specified in setup.xml) matches the stored password for username.
     *    If user==null or user has no password defined in datasets.xml, this returns false.
     */
    public static boolean doesPasswordMatch(String username, String plaintextPassword) {
        if (username == null || username.length() == 0 ||
            !username.equals(String2.justPrintable(username)) ||
            plaintextPassword == null || plaintextPassword.length() < minimumPasswordLength)
            return false;

        Object oar[] = (Object[])userHashMap.get(username);
        if (oar == null) {
            String2.log("username=" + username + " not found in userHashMap."); 
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
            observed = String2.md5Hex(username + ":ERDDAP:" + plaintextPassword); //it will be lowercase
        else if (passwordEncoding.equals("SHA256"))
            observed = String2.passwordDigest("SHA-256", plaintextPassword); //it will be lowercase
        else if (passwordEncoding.equals("UEPSHA256"))
            observed = String2.passwordDigest("SHA-256", username + ":ERDDAP:" + plaintextPassword); //it will be lowercase
        else throw new RuntimeException("Unexpected passwordEncoding=" + passwordEncoding);
        //only for debugging:
        //String2.log("username=" + username +
        //    "\nobsPassword=" + observed +
        //    "\nexpPassword=" + expected);

        boolean ok = observed.equals(expected);
        if (reallyVerbose)
            String2.log("username=" + username + " password matched: " + ok);
        return ok; 
    }

    /**
     * This indicates if a user is on the list of potential users 
     * (i.e., there's a user tag for this user in datasets.xml).
     *
     * @param userName the user's potential user name 
     * @return true if a user is on the list of potential users
     * (i.e., there's a user tag for this user in datasets.xml).
     */
    public static boolean onListOfUsers(String userName) {
        if (!String2.isSomething(userName))
            return false;
        return userHashMap.get(userName) != null;
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
        if (oar == null)
            return null;
        return (String[])oar[1];
    }

    /**
     * If the user tries to access a dataset to which he doesn't have access,
     * call this to send Http UNAUTHORIZED error.
     * (was: redirectToLogin: redirect him to the login page).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetID  or use "" for general login.
     * @param graphsAccessibleToPublic From edd.graphsAccessibleToPublic(). 
     *   If this is true, then this method
     *   was called because the request was for data from a dataset that
     *   allows graphics|metadata requests from the public.
     * @throws Throwable (notably ClientAbortException)
     */
    public static void sendHttpUnauthorizedError(String loggedInAs, 
        HttpServletResponse response, String datasetID, 
        boolean graphsAccessibleToPublic) throws Throwable {

        String message = null;
        try {
            tally.add("Request refused: not authorized (since startup)", datasetID); 
            tally.add("Request refused: not authorized (since last daily report)", datasetID);

            if (datasetID != null && datasetID.length() > 0) 
                message = MessageFormat.format(
                    graphsAccessibleToPublic?
                        EDStatic.notAuthorizedForData :
                        EDStatic.notAuthorized, 
                    loggedInAsHttps.equals(loggedInAs)? "" : loggedInAs, 
                    datasetID);

            if (message == null)
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            else 
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);

        } catch (Throwable t2) {
            EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}
            String2.log("Error in sendHttpUnauthorizedError:\n" + 
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
     *   Special case: "loggedInAsHttps" is for using https without being logged in, 
     *   but &amp;loginInfo; indicates user isn't logged in.
     */
    public static String getLoginHtml(String loggedInAs) {
        if (authentication.equals("")) {
            //user can't log in
            return "";
        } else {
            return loggedInAs == null || loggedInAsHttps.equals(loggedInAs)?  //ie not logged in
                //always use the erddapHttpsUrl for login/logout pages
                "<a href=\"" + erddapHttpsUrl + "/login.html\">" + login + "</a>" :
                "<a href=\"" + erddapHttpsUrl + "/login.html\"><strong>" + XML.encodeAsHTML(loggedInAs) + "</strong></a> | \n" + 
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
     *   Special case: "loggedInAsHttps" is for using https without being logged in, 
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
        return "<a title=\"" + clickERDDAP + "\" \n" +
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
        ae = String2.replaceAll(ae, ".", " dot ");
        return adminIndividualName + " (email: " + ae + ")";
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
            "<p><span class=\"warningColor\"><strong>" + errorOnWebPage + "</strong></span>\n" +
            "<pre>" + XML.encodeAsPreHTML(message, 100) +
            "</pre>\n";
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

            //shutdown Cassandra clusters/sessions
            EDDTableFromCassandra.shutdown();

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
                        Calendar2.getCurrentISODateTimeStringLocalTZ();
                    email(emailEverythingToCsv, "taskThread Stalled", tError);
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
                    Calendar2.getCurrentISODateTimeStringLocalTZ() + ")\n");
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
                    Calendar2.getCurrentISODateTimeStringLocalTZ() + " nPendingTasks=" + nPending + "\n");
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
     * This returns the Oceanic/Atmospheric Acronyms table: col 0=acronym 1=fullName.
     * <br>Acronyms are case-sensitive, sometimes with common variants included.
     * <br>The table is basically sorted by acronym, but with longer acronyms
     *  (e.g., AMSRE) before shorter siblings (e.g., AMSR).
     * <br>Many of these are from
     * https://www.nodc.noaa.gov/General/mhdj_acronyms3.html
     * and http://www.psmsl.org/train_and_info/training/manuals/acronyms.html
     *
     * @return the oceanic/atmospheric acronyms table
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table oceanicAtmosphericAcronymsTable() throws Exception {
        Table table = new Table();
        StringArray col1 = new StringArray();
        StringArray col2 = new StringArray();
        table.addColumn("acronym", col1);
        table.addColumn("fullName", col2);
        String lines[] = String2.readLinesFromFile(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/OceanicAtmosphericAcronyms.tsv",
            String2.ISO_8859_1, 1);
        int nLines = lines.length;
        for (int i = 1; i < nLines; i++) { //1 because skip colNames
            String s = lines[i].trim();
            if (s.length() == 0 || s.startsWith("//"))
                continue;
            int po = s.indexOf('\t');
            if (po < 0) 
                po = s.length();
            col1.add(s.substring(0, po).trim());
            col2.add(s.substring(po + 1).trim());
        }
        return table;
    }
    
    /**
     * This returns the Oceanic/Atmospheric Variable Names table: col 0=variableName 1=fullName.
     * <br>varNames are all lower-case.  long_names are mostly first letter of each word capitalized.
     * <br>The table is basically sorted by varName.
     * <br>Many of these are from
     * https://www.esrl.noaa.gov/psd/data/gridded/conventions/variable_abbreviations.html
     *
     * @return the oceanic/atmospheric variable names table
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table oceanicAtmosphericVariableNamesTable() throws Exception {
        Table table = new Table();
        StringArray col1 = new StringArray();
        StringArray col2 = new StringArray();
        table.addColumn("variableName", col1);
        table.addColumn("fullName", col2);
        String lines[] = String2.readLinesFromFile(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/OceanicAtmosphericVariableNames.tsv",
            String2.ISO_8859_1, 1);
        int nLines = lines.length;
        for (int i = 1; i < nLines; i++) {
            String s = lines[i].trim();
            if (s.length() == 0 || s.startsWith("//"))
                continue;
            int po = s.indexOf('\t');
            if (po < 0) 
                po = s.length();
            col1.add(s.substring(0, po).trim());
            col2.add(s.substring(po + 1).trim());
        }
        return table;
    }

    /**
     * This returns the Oceanic/Atmospheric Acronyms table as a Table:
     * col0=acronym, col1=fullName.
     * THIS IS ONLY FOR GenerateDatasetsXml THREADS -- a few common acronyms are removed.
     *
     * @return the oceanic/atmospheric variable names table with some common acronyms removed
     * @throws Exception if trouble (e.g., file not found)
     */
    public static Table gdxAcronymsTable() throws Exception {
        if (gdxAcronymsTable == null) {
            Table table = oceanicAtmosphericAcronymsTable();
            StringArray acronymSA  = (StringArray)(table.getColumn(0));
            StringArray fullNameSA = (StringArray)(table.getColumn(1));

            //remove some really common acronyms I don't want to expand
            BitSet keep = new BitSet();
            keep.set(0, acronymSA.size());
            String common[] = {//"DOC", "DOD", "DOE", "USDOC", "USDOD", "USDOE", 
                "NOAA", "NASA", "US"};
            for (int c = 0; c < common.length; c++) {
                int po = acronymSA.indexOf(common[c]); 
                if (po >= 0) 
                    keep.clear(po);
            }
            table.justKeep(keep);

            gdxAcronymsTable = table; //swap into place
        }
        return gdxAcronymsTable;
    }

    /**
     * This returns the Oceanic/Atmospheric Acronyms table as a HashMap:
     * key=acronym, value=fullName.
     * THIS IS ONLY FOR GenerateDatasetsXml THREADS -- a few common acronyms are removed.
     *
     * @return the oceanic/atmospheric variable names table as a HashMap
     * @throws Exception if trouble (e.g., file not found)
     */
    public static HashMap<String,String> gdxAcronymsHashMap() throws Exception {
        if (gdxAcronymsHashMap == null) {
            Table table = gdxAcronymsTable();
            StringArray acronymSA  = (StringArray)(table.getColumn(0));
            StringArray fullNameSA = (StringArray)(table.getColumn(1));
            int n = table.nRows();
            HashMap<String,String> hm = new HashMap();
            for (int i = 1; i < n; i++)
                hm.put(acronymSA.get(i), fullNameSA.get(i));
            gdxAcronymsHashMap = hm; //swap into place
        }
        return gdxAcronymsHashMap;
    }

    /**
     * This returns the Oceanic/Atmospheric Variable Names table as a HashMap:
     * key=variableName, value=fullName.
     * THIS IS ONLY FOR GenerateDatasetsXml THREADS.
     *
     * @return the oceanic/atmospheric variable names table as a HashMap
     * @throws Exception if trouble (e.g., file not found)
     */
    public static HashMap<String,String> gdxVariableNamesHashMap() throws Exception {
        if (gdxVariableNamesHashMap == null) {
            Table table = oceanicAtmosphericVariableNamesTable();
            StringArray varNameSA  = (StringArray)(table.getColumn(0));
            StringArray fullNameSA = (StringArray)(table.getColumn(1));
            int n = table.nRows();
            HashMap<String,String> hm = new HashMap();
            for (int i = 1; i < n; i++)
                hm.put(varNameSA.get(i), fullNameSA.get(i));
            gdxVariableNamesHashMap = hm; //swap into place
        }
        return gdxVariableNamesHashMap;
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
            0, 1, "", null, null, null, null, false); //false = don't simplify
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
        return majorLoadDatasetsTimeSeriesSB.length() == 0;
    }

    /** This is called by the ERDDAP constructor to initialize Lucene. */
    public static void initializeLucene() {
        //ERDDAP consciously doesn't use any stopWords (words not included in the index)
        //1) this matches the behaviour of the original searchEngine
        //2) this is what users expect, e.g., when searching for a phrase
        //3) the content here isn't prose, so the stop words aren't nearly as common
        HashSet stopWords = new HashSet();
        luceneAnalyzer = new StandardAnalyzer(luceneVersion, stopWords);
        //it is important that the queryParser use the same analyzer as the indexWriter
        luceneQueryParser = new QueryParser(luceneVersion, luceneDefaultField, luceneAnalyzer);
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
                (System.currentTimeMillis() - tTime) + "ms");
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
                        (System.currentTimeMillis() - rTime) + "ms");

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
                        (System.currentTimeMillis() - rTime) + "ms");

                    //if successful, we no longer needNewLuceneIndexReader
                    needNewLuceneIndexReader = false;  
                    //if successful, fall through

                } catch (Throwable t) {
                    String subject = String2.ERROR + " while creating Lucene Searcher";
                    String msg = MustBe.throwableToString(t);
                    email(emailEverythingToCsv, subject, msg);
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
                //String2.log("  luceneParseQuery finished.  time=" + (System.currentTimeMillis() - qTime) + "ms"); //always 0
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
            String parts[] = Table.getDapQueryParts(request.getQueryString()); //decoded.  Does some validity checking.
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
     * This updates out-of-date http: references to https: within a string.
     * This is very safe won't otherwise change the string (even "" or null).
     */
    public static String updateUrls(String s) {
        if (!String2.isSomething(s))
            return s;

        //change some non-http things
        StringBuilder sb = new StringBuilder(s);
        String2.replaceAll(sb,  //reversed in naming_authority
            "gov.noaa.pfel.",
            "gov.noaa.pfeg."); 

        if (sb.indexOf("http") < 0)
            return sb.toString();

        int n = updateUrlsFrom.length;
        for (int i = 0; i < n; i++)
            String2.replaceAll(sb, updateUrlsFrom[i], updateUrlsTo[i]);
        return sb.toString();
    }

    /**
     * This calls updateUrls for every String attribute (except EDStatic.updateUrlsSkipAttributes)
     * and writes changes to addAtts.
     * 
     * @param sourceAtts may be null
     * @param addAtts mustn't be null.
     */
    public static void updateUrls(Attributes sourceAtts, Attributes addAtts) {
        //get all the attribute names
        HashSet<String> hs = new HashSet();
        String names[];
        if (sourceAtts != null) {
            names = sourceAtts.getNames();
            for (int i = 0; i < names.length; i++)
                hs.add(names[i]);
        }
        names = addAtts.getNames();
        for (int i = 0; i < names.length; i++)
            hs.add(names[i]);
        names = hs.toArray(new String[]{});

        //updateUrls in all attributes
        for (int i = 0; i < names.length; i++) {
            if (String2.indexOf(updateUrlsSkipAttributes, names[i]) >= 0)
                continue;
            PrimitiveArray pa = addAtts.get(names[i]);
            if (pa == null && sourceAtts != null)
                pa = sourceAtts.get(names[i]);
            if (pa != null && pa.size() > 0 && pa.elementClass() == String.class) {
                String oValue = pa.getString(0);        
                String value = updateUrls(oValue);
                if (!value.equals(oValue))
                    addAtts.set(names[i], value); 
            }
        }
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
     * Normal use: Use this first thing in catch, before throwing WaitThenTryAgainException.
     *
     * @param t the exception which will be thrown again if it is a ClientAbortException
     * @throws Throwable
     */
    public static void rethrowClientAbortException(Throwable t) throws Throwable {
        if (isClientAbortException(t)) 
            throw t;
    }


    public static void testUpdateUrls() throws Exception {
        String2.log("\n***** EDStatic.testUpdateUrls");
        Attributes source = new Attributes();
        Attributes add    = new Attributes();
        source.set("a", "http://coastwatch.pfel.noaa.gov");
        source.set("nine", 9.0);
        add.set(   "b", "http://www.whoi.edu");
        add.set(   "ten", 10.0);
        add.set(   "sourceUrl", "http://coastwatch.pfel.noaa.gov");
        EDStatic.updateUrls(source, add);
        String results = add.toString();
        String expected = 
"    a=https://coastwatch.pfeg.noaa.gov\n" +
"    b=https://www.whoi.edu\n" +
"    sourceUrl=http://coastwatch.pfel.noaa.gov\n" + //unchanged
"    ten=10.0d\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        source = new Attributes();
        add    = new Attributes();
        add.set("a", "http://coastwatch.pfel.noaa.gov");
        add.set("b", "http://www.whoi.edu");
        add.set("nine", 9.0);
        add.set("sourceUrl", "http://coastwatch.pfel.noaa.gov");
        EDStatic.updateUrls(null, add);
        results = add.toString();
        expected = 
"    a=https://coastwatch.pfeg.noaa.gov\n" +
"    b=https://www.whoi.edu\n" +
"    nine=9.0d\n" +
"    sourceUrl=http://coastwatch.pfel.noaa.gov\n"; //unchanged
        Test.ensureEqual(results, expected, "results=\n" + results);
    }

    /** 
     * This tests some of the methods in this class.
     * @throws an Exception if trouble.
     */
    public static void test() throws Throwable {
        String2.log("\n*** EDStatic.test");
        testUpdateUrls();
    }

}
