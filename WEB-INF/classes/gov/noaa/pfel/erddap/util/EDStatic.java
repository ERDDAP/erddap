/* 
 * EDStatic Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
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
import com.cohort.util.Units2;
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
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import gov.noaa.pfel.erddap.*;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.variable.*;

import java.awt.Color;
import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.model.CommonPrefix;
//import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
//import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
//import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.S3Client;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

/** 
 * This class holds a lot of static information set from the setup.xml and messages.xml
 * files and used by all the other ERDDAP classes. 
 */
public class EDStatic { 

    /** The all lowercase name for the program that appears in urls. */
    public final static String programname = "erddap";

    /** The uppercase name for the program that appears on web pages. */
    public final static String ProgramName = "ERDDAP";

    public final static String REQUESTED_RANGE_NOT_SATISFIABLE = 
                              "REQUESTED_RANGE_NOT_SATISFIABLE: ";

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
     * <br>2.00 released on 2019-06-26
     * <br>2.01 released on 2019-07-02
     * <br>2.02 released on 2019-08-21
     * <br>2.10 released on 2020-11-05 (version jump because of new PATypes)
     * <br>2.11 released on 2020-12-04
     * <br>2.12 released on 2021-05-14
     * <br>2.13 none
     * <br>2.14 released on 2021-07-02
     *
     * For master branch releases, this will be a floating point
     * number with 2 decimal digits, with no additional text. 
     * !!! In general, people other than the main ERDDAP developer (Bob) 
     * should not change the *number* below.
     * If you need to identify a fork of ERDDAP, please append "_" + other 
     * ASCII text (no spaces or control characters) to the number below,
     * e.g., "1.82_MyFork".
     * In a few places in ERDDAP, this string is parsed as a number. 
     * The parser now disregards "_" and anything following it.
     * A request to http.../erddap/version will return just the number (as text).
     * A request to http.../erddap/version_string will return the full string.
     */   
    public static String erddapVersion = "2.14"; //see comment above

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
    public final static int TITLE_DOT_LENGTH = 95; //max nChar before inserting newlines (was truncate with " ... ")

    /* contextDirectory is the local directory on this computer, e.g., [tomcat]/webapps/erddap/ */
    public static String webInfParentDirectory = String2.webInfParentDirectory(); //with / separator and / at the end
    //fgdc and iso19115XmlDirectory are used for virtual URLs.
    public final static String fgdcXmlDirectory     = "metadata/fgdc/xml/";     //virtual
    public final static String iso19115XmlDirectory = "metadata/iso19115/xml/"; //virtual
    public final static String DOWNLOAD_DIR         = "download/";
    public final static String IMAGES_DIR           = "images/";
    public final static String PUBLIC_DIR           = "public/"; 
    public static String
        fullPaletteDirectory = webInfParentDirectory + "WEB-INF/cptfiles/",
        fullPublicDirectory  = webInfParentDirectory + PUBLIC_DIR,
        downloadDir          = webInfParentDirectory + DOWNLOAD_DIR, //local directory on this computer
        imageDir             = webInfParentDirectory + IMAGES_DIR;   //local directory on this computer
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
    public static int dangerousMemoryFailures = 0; //since last Major LoadDatasets
    public static StringBuffer suggestAddFillValueCSV = new StringBuffer(); //EDV constructors append message here   //thread-safe but probably doesn't need to be

    public static String datasetsThatFailedToLoad = "";
    public static String errorsDuringMajorReload = "";
    public static StringBuffer majorLoadDatasetsTimeSeriesSB = new StringBuffer(""); //thread-safe (1 thread writes but others may read)
    public static HashSet requestBlacklist = null; //is read-only. Replacement is swapped into place.
    public static long startupMillis = System.currentTimeMillis();
    public static String startupLocalDateTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
    public static int nGridDatasets = 0;  
    public static int nTableDatasets = 0;
    public static long lastMajorLoadDatasetsStartTimeMillis = System.currentTimeMillis();
    public static long lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis() - 1;
    private static ConcurrentHashMap<String,String> sessionNonce = 
        new ConcurrentHashMap(16, 0.75f, 4); //for a session: loggedInAs -> nonce

    public final static String ipAddressNotSetYet = "NotSetYet"; 
    public final static String ipAddressUnknown   = "(unknownIPAddress)";
    public final static ConcurrentHashMap<String,IntArray> ipAddressQueue = new ConcurrentHashMap();  //ipAddress -> list of request#
    public final static int    DEFAULT_ipAddressMaxRequestsActive = 2; //in datasets.xml
    public final static int    DEFAULT_ipAddressMaxRequests = 7; //in datasets.xml //more requests will see Too Many Requests error. This must be at least 6 because browsers make up to 6 simultaneous requests. This can't be >1000.
    public final static String DEFAULT_ipAddressUnlimited = ", " + ipAddressUnknown;
    public static int ipAddressMaxRequestsActive = DEFAULT_ipAddressMaxRequestsActive; //in datasets.xml
    public static int ipAddressMaxRequests = DEFAULT_ipAddressMaxRequests; //in datasets.xml //more requests will see Too Many Requests error. This must be at least 6 because browsers make up to 6 simultaneous requests.
    public static HashSet<String> ipAddressUnlimited =  //in datasets.xml  //read only. New one is swapped into place. You can add and remove addresses as needed.
        new HashSet<String>(String2.toArrayList(StringArray.fromCSVNoBlanks(EDStatic.DEFAULT_ipAddressUnlimited).toArray())); 
    public static int tooManyRequests = 0; //nRequests exceeding ipAddressMaxRequests, since last major datasets reload

    //things that can be specified in datasets.xml (often added in ERDDAP v2.00)
    public final static String DEFAULT_ANGULAR_DEGREE_UNITS = "angular_degree,angular_degrees,arcdeg,arcdegs,degree," +
        "degreeE,degree_E,degree_east,degreeN,degree_N,degree_north,degrees," +
        "degreesE,degrees_E,degrees_east,degreesN,degrees_N,degrees_north," +
        "degreesW,degrees_W,degrees_west,degreeW,degree_W,degree_west";
    public final static String DEFAULT_ANGULAR_DEGREE_TRUE_UNITS = "degreesT,degrees_T,degrees_Tangular_degree,degrees_true," +
        "degreeT,degree_T,degree_true";
    public static Set<String> angularDegreeUnitsSet =
        new HashSet<String>(String2.toArrayList(StringArray.fromCSV(DEFAULT_ANGULAR_DEGREE_UNITS).toArray())); //so canonical
    public static Set<String> angularDegreeTrueUnitsSet = 
        new HashSet<String>(String2.toArrayList(StringArray.fromCSV(DEFAULT_ANGULAR_DEGREE_TRUE_UNITS).toArray())); //so canonical

    public final static int DEFAULT_decompressedCacheMaxGB = 10; //for now, 1 value applies to each dataset's decompressed dir
    public final static int DEFAULT_decompressedCacheMaxMinutesOld = 15;
    public final static int DEFAULT_nGridThreads = 1;
    public final static int DEFAULT_nTableThreads = 1;
    public static String          DEFAULT_palettes[]   = null; //set when messages.xml is read
    public static HashSet<String> DEFAULT_palettes_set = null;  //set when messages.xml is read
    public static int decompressedCacheMaxGB         = DEFAULT_decompressedCacheMaxGB; 
    public static int decompressedCacheMaxMinutesOld = DEFAULT_decompressedCacheMaxMinutesOld; 
    public static int nGridThreads                   = DEFAULT_nGridThreads;  //will be a valid number 1+
    public static int nTableThreads                  = DEFAULT_nTableThreads; //will be a valid number 1+
    public static String convertInterpolateRequestCSVExample = null;         //may be null or ""
    public static String convertInterpolateDatasetIDVariableList[] = new String[0]; //may be [0]

    //things that were in setup.xml (discouraged) and are now in datasets.xml (v2.00+)
    public final static int    DEFAULT_cacheMinutes            = 60;
    public final static String DEFAULT_drawLandMask            = "under";  
    public final static int    DEFAULT_graphBackgroundColorInt = 0xffccccff; 
    public final static int    DEFAULT_loadDatasetsMinMinutes  = 15;
    public final static int    DEFAULT_loadDatasetsMaxMinutes  = 60;
    public final static String DEFAULT_logLevel                = "info"; //warning|info|all
    public final static int    DEFAULT_partialRequestMaxBytes  = 490000000; //this is just below tds default <opendap><binLimit> of 500MB
    public final static int    DEFAULT_partialRequestMaxCells  = 10000000;
    public final static int    DEFAULT_slowDownTroubleMillis   = 1000;
    public final static int    DEFAULT_unusualActivity         = 10000;
    public static long   cacheMillis            = DEFAULT_cacheMinutes           * Calendar2.MILLIS_PER_MINUTE;
    public static String drawLandMask           = DEFAULT_drawLandMask;    
    public static boolean emailDiagnosticsToErdData = true;
    public static Color  graphBackgroundColor   = new Color(DEFAULT_graphBackgroundColorInt, true); //hasAlpha
    public static long   loadDatasetsMinMillis  = DEFAULT_loadDatasetsMinMinutes * Calendar2.MILLIS_PER_MINUTE;
    public static long   loadDatasetsMaxMillis  = DEFAULT_loadDatasetsMaxMinutes * Calendar2.MILLIS_PER_MINUTE;
    //logLevel handled specially by setLogLevel
    public static int    partialRequestMaxBytes = DEFAULT_partialRequestMaxBytes;
    public static int    partialRequestMaxCells = DEFAULT_partialRequestMaxCells;
    public static int    slowDownTroubleMillis  = DEFAULT_slowDownTroubleMillis;
    public static int    unusualActivity        = DEFAULT_unusualActivity;

    //work-in-progress: read the messages in different languages
    private static String[]  //these are set by setup.xml (deprecated) and/or datasets.xml (v2.00+)
        standardShortDescriptionHtml_s,
        DEFAULT_standardLicense_s,
        DEFAULT_standardContact_s,
        DEFAULT_standardDataLicenses_s,
        DEFAULT_standardDisclaimerOfEndorsement_s,
        DEFAULT_standardDisclaimerOfExternalLinks_s,
        DEFAULT_standardGeneralDisclaimer_s,
        DEFAULT_standardPrivacyPolicy_s,
        DEFAULT_startHeadHtml_s, //see xxx() method
        DEFAULT_startBodyHtml_s,
        DEFAULT_theShortDescriptionHtml_s,
        DEFAULT_endBodyHtml_s,

        standardLicense_s,
        standardContact_s,
        standardDataLicenses_s,
        standardDisclaimerOfEndorsement_s,
        standardDisclaimerOfExternalLinks_s,
        standardGeneralDisclaimer_s,
        standardPrivacyPolicy_s,
        startHeadHtml_s, //see xxx() methods
        startBodyHtml_s,
        theShortDescriptionHtml_s,
        endBodyHtml_s;

    public static String  //these are set by setup.xml (deprecated) and/or datasets.xml (v2.00+)
        standardShortDescriptionHtml, 
        DEFAULT_standardLicense,
        DEFAULT_standardContact,
        DEFAULT_standardDataLicenses,
        DEFAULT_standardDisclaimerOfEndorsement,
        DEFAULT_standardDisclaimerOfExternalLinks,
        DEFAULT_standardGeneralDisclaimer,
        DEFAULT_standardPrivacyPolicy,
        DEFAULT_startHeadHtml, //see xxx() methods
        DEFAULT_startBodyHtml, 
        DEFAULT_theShortDescriptionHtml,
        DEFAULT_endBodyHtml, 
 
        standardLicense, 
        standardContact,
        standardDataLicenses,
        standardDisclaimerOfEndorsement,
        standardDisclaimerOfExternalLinks,
        standardGeneralDisclaimer,
        standardPrivacyPolicy,
        startHeadHtml, //see xxx() methods
        startBodyHtml, 
        theShortDescriptionHtml, 
        endBodyHtml;
    public static String //in messages.xml and perhaps in datasets.xml (v2.00+)
        commonStandardNames[],
        DEFAULT_commonStandardNames[];


    //Default max of 25 copy tasks at a time, so different datasets have a chance.
    //Otherwise, some datasets could take months to do all the tasks.
    //And some file downloads are very slow (10 minutes).
    //Remember: last task is to reload the dataset, so that will get the next 25 tasks.
    public static int DefaultMaxMakeCopyFileTasks = 25; 


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
     * the number of the last task assigned to taskThread for a given datasetID.
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
    //Since I recreate the index when erddap restarted, I can change anything
    //  (e.g., Directory type, Version) any time
    //  (no worries about compatibility with existing index).
    //useful documentatino 
    //  https://wiki.apache.org/lucene-java/LuceneFAQ
    //  https://wiki.apache.org/lucene-java/BasicsOfPerformance
    //  http://affy.blogspot.com/2003/04/codebit-examples-for-all-of-lucenes.html
    public  final static String  luceneDefaultField = "text";
    //special characters to be escaped
    //see bottom of https://lucene.apache.org/java/3_5_0/queryparsersyntax.html
    public  static String        luceneSpecialCharacters = "+-&|!(){}[]^\"~*?:\\";

    //made if useLuceneSearchEngine
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
    public  static ConcurrentHashMap<Integer,String> luceneDocNToDatasetID;

    //also see updateLucene in LoadDatasets

    public final static int    defaultItemsPerPage     = 1000; //1000, for /info/index.xxx and search
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

        EDDTableFromHttpGetDatasetDescription_s[],
        EDDTableFromHttpGetAuthorDescription_s[],
        EDDTableFromHttpGetTimestampDescription_s[],

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

        authentication,  //will be one of "", "custom", "email", "google", "orcid", "oauth2". If baseHttpsUrl doesn't start with https:, this will be "".
        datasetsRegex,
        emailEverythingToCsv, 
        emailDailyReportToCsv,
        emailSubscriptionsFrom,
        flagKeyKey,
        fontFamily,
        googleClientID,    //if authentication=google or oauth2, this will be something
        orcidClientID,     //if authentication=orcid  or oauth2, this will be something
        orcidClientSecret, //if authentication=orcid  or oauth2, this will be something
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
    private static volatile int[] ampLoginInfoPo_s;
    public static volatile int ampLoginInfoPo = -1;
    /** These are special because other loggedInAs must be String2.justPrintable
        loggedInAsHttps is for using https without being logged in, 
          but &amp;loginInfo; indicates user isn't logged in.
          It is a reserved username -- LoadDatasets prohibits defining a user with that name.
        Tab is useful here: LoadDatasets prohibits it as valid userName, 
          but it won't cause big trouble when printed in tally info.
        anyoneLoggedIn is a role given to everyone who is logged in e.g., via a specific Google email address.
          It is a reserved username -- LoadDatasets prohibits defining a user with that name.
     */
    public final static String loggedInAsHttps       = "[https]";          //final so not changeable
    public final static String loggedInAsSuperuser   = "\tsuperuser";      //final so not changeable
    public final static String anyoneLoggedIn        = "[anyoneLoggedIn]"; //final so not changeable
    public final static String anyoneLoggedInRoles[] = new String[]{anyoneLoggedIn};
    public final static int minimumPasswordLength = 8;

    //these are all non-null if in awsS3Output mode, otherwise all are null
    public static String   awsS3OutputBucketUrl = null;  
    public static String   awsS3OutputBucket    = null;  //the short name of the bucket
    public static S3Client awsS3OutputClient    = null;

    public static boolean listPrivateDatasets, 
        reallyVerbose,
        subscriptionSystemActive,  convertersActive, slideSorterActive,
        fgdcActive, iso19115Active, jsonldActive, geoServicesRestActive, 
        filesActive, defaultAccessibleViaFiles, dataProviderFormActive, 
        outOfDateDatasetsActive, politicalBoundariesActive, 
        wmsClientActive, 
        sosActive, wcsActive, wmsActive,
        quickRestart, subscribeToRemoteErddapDataset,
        //if useLuceneSearchEngine=false (a setting, or after error), original search engine will be used 
        useLuceneSearchEngine,  
        variablesMustHaveIoosCategory,
        verbose;
    public static String  categoryAttributes[];       //as it appears in metadata (and used for hashmap)
    public static String  categoryAttributesInURLs[]; //fileNameSafe (as used in URLs)
    public static boolean categoryIsGlobal[];
    public static int variableNameCategoryAttributeIndex = -1;
    public static int logMaxSizeMB;
    
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
        fullFileVisitorDirectory,
        fullCacheDirectory,
        fullDecompressedDirectory,
        fullDecompressedGenerateDatasetsXmlDirectory,
        fullLogsDirectory,
        fullCopyDirectory,
        fullLuceneDirectory,
        fullResetFlagDirectory,
        fullBadFilesFlagDirectory,
        fullHardFlagDirectory,
        fullCptCacheDirectory,         
        fullPlainFileNcCacheDirectory,         
        fullSgtMapTopographyCacheDirectory,
        fullTestCacheDirectory,
        fullWmsCacheDirectory,

        imageDirUrl,
        imageDirHttpsUrl,
        inotifyFixCommands,
        EDDIso19115,
        //downloadDirUrl,
        computerName; //e.g., coastwatch (or "")
    public static Subscriptions subscriptions; //null if !EDStatic.subscriptionSystemActive


    /** These values are loaded from the [contentDirectory]messages.xml file (if present)
        or .../classes/gov/noaapfel/erddap/util/messages.xml. */
    private static String[]
        acceptEncodingHtml_s,
        filesDocumentation_s;

    public static String
        advr_dataStructure,
        advr_cdm_data_type,
        advr_class,
        admKeywords,
        admSubsetVariables,

        blacklistMsg,
        extensionsNoRangeRequests[],

        palettes[],
        palettes0[],
        paletteSections[] = {
            "","2","3","4","5","6","7","8","9",
            "10","11","12","13","14","15","16","17","18","19",
            "20","21","22","23","24","25","26","27","28","29",
            "30","31","32","33","34","35","36","37","38","39", "40"},

        queryError,
        resourceNotFound,
        sparqlP01toP02pre,
        sparqlP01toP02post,
        updateUrlsSkipAttributes[],
        updateUrlsFrom[],
        updateUrlsTo[],
        waitThenTryAgain;
    private static String[]
        accessRESTFUL_s,
        acronyms_s,
        addConstraints_s,
        addVarWhereAttName_s,
        addVarWhereAttValue_s,
        addVarWhere_s,
        additionalLinks_s,

        admSummary_s,
        admTitle_s,
        advl_datasetID_s,
        advc_accessible_s,
        advl_accessible_s,
        advl_institution_s,
        advc_dataStructure_s,
        advl_dataStructure_s,
        //advr_dataStructure, advr are same links across all files
        advl_cdm_data_type_s,
        //advr_cdm_data_type,
        advl_class_s,
        //advr_class,
        advl_title_s,
        advl_minLongitude_s,
        advl_maxLongitude_s,
        advl_longitudeSpacing_s,
        advl_minLatitude_s,
        advl_maxLatitude_s,
        advl_latitudeSpacing_s,
        advl_minAltitude_s,
        advl_maxAltitude_s,
        advl_minTime_s,
        advc_maxTime_s,
        advl_maxTime_s,
        advl_timeSpacing_s,
        advc_griddap_s,
        advl_griddap_s,
        advl_subset_s,
        advc_tabledap_s,
        advl_tabledap_s,
        advl_MakeAGraph_s,
        advc_sos_s,
        advl_sos_s,
        advl_wcs_s,
        advl_wms_s,
        advc_files_s,
        advl_files_s,
        advc_fgdc_s,
        advl_fgdc_s,
        advc_iso19115_s,
        advl_iso19115_s,
        advc_metadata_s,
        advl_metadata_s,
        advl_sourceUrl_s,
        advl_infoUrl_s,
        advl_rss_s,
        advc_email_s,
        advl_email_s,
        advl_summary_s,
        advc_testOutOfDate_s,
        advl_testOutOfDate_s,
        advc_outOfDate_s,
        advl_outOfDate_s,
        advn_outOfDate_s,

        advancedSearch_s,
        advancedSearchResults_s,
        advancedSearchDirections_s,
        advancedSearchTooltip_s,
        advancedSearchBounds_s,
        advancedSearchMinLat_s,
        advancedSearchMaxLat_s,
        advancedSearchMinLon_s,
        advancedSearchMaxLon_s,
        advancedSearchMinMaxLon_s,
        advancedSearchMinTime_s,
        advancedSearchMaxTime_s,
        advancedSearchClear_s,
        advancedSearchClearHelp_s,
        advancedSearchCategoryTooltip_s,
        advancedSearchRangeTooltip_s,
        advancedSearchMapTooltip_s,
        advancedSearchLonTooltip_s,
        advancedSearchTimeTooltip_s,
        advancedSearchWithCriteria_s,
        advancedSearchFewerCriteria_s,
        advancedSearchNoCriteria_s,
        advancedSearchErrorHandling_s,
        autoRefresh_s,
       
        categoryTitleHtml_s,
        categoryHtml_s,
        category3Html_s,
        categoryPickAttribute_s,
        categorySearchHtml_s,
        categorySearchDifferentHtml_s,
        categoryClickHtml_s,
        categoryNotAnOption_s,
        caughtInterrupted_s,
        cdmDataTypeHelp_s,
        clickAccess_s,
        clickBackgroundInfo_s,
        clickERDDAP_s,
        clickInfo_s,
        clickToSubmit_s,
        converterWebService_s,
        convertOceanicAtmosphericAcronyms_s,
        convertOceanicAtmosphericAcronymsIntro_s,
        convertOceanicAtmosphericAcronymsNotes_s,
        convertOceanicAtmosphericAcronymsService_s,
        convertOceanicAtmosphericVariableNames_s,
        convertOceanicAtmosphericVariableNamesIntro_s,
        convertOceanicAtmosphericVariableNamesNotes_s,
        convertOceanicAtmosphericVariableNamesService_s,
        convertFipsCounty_s,
        convertFipsCountyIntro_s,
        convertFipsCountyNotes_s,
        convertFipsCountyService_s,
        convertHtml_s,
        convertInterpolate_s,
        convertInterpolateIntro_s,
        convertInterpolateTLLTable_s,
        convertInterpolateTLLTableHelp_s,
        convertInterpolateDatasetIDVariable_s,
        convertInterpolateDatasetIDVariableHelp_s,
        convertInterpolateNotes_s,
        convertInterpolateService_s,
        convertKeywords_s,
        convertKeywordsCfTooltip_s,
        convertKeywordsGcmdTooltip_s,
        convertKeywordsIntro_s,
        convertKeywordsNotes_s,
        convertKeywordsService_s,
        convertTime_s,
        convertTimeBypass_s,
        convertTimeReference_s,
        convertTimeIntro_s,
        convertTimeNotes_s,
        convertTimeService_s,
        convertTimeNumberTooltip_s,
        convertTimeStringTimeTooltip_s,
        convertTimeUnitsTooltip_s,
        convertTimeUnitsHelp_s,
        convertTimeIsoFormatError_s,
        convertTimeNoSinceError_s,
        convertTimeNumberError_s,
        convertTimeNumericTimeError_s,
        convertTimeParametersError_s,
        convertTimeStringFormatError_s,
        convertTimeTwoTimeError_s,
        convertTimeUnitsError_s,
        convertUnits_s,
        convertUnitsComparison_s,
        convertUnitsFilter_s,
        convertUnitsIntro_s,
        convertUnitsNotes_s,
        convertUnitsService_s,
        convertURLs_s,
        convertURLsIntro_s,
        convertURLsNotes_s,
        convertURLsService_s,
        cookiesHelp_s,
        daf_s,
        dafGridBypassTooltip_s,
        dafGridTooltip_s,
        dafTableBypassTooltip_s,
        dafTableTooltip_s,
        dasTitle_s,
        dataAccessNotAllowed_s,
        databaseUnableToConnect_s,
        dataProviderFormSuccess_s,
        dataProviderFormShortDescription_s,
        dataProviderFormLongDescriptionHTML_s,
        dataProviderFormPart1_s,
        dataProviderFormPart2Header_s,
        dataProviderFormPart2GlobalMetadata_s,
        dataProviderContactInfo_s,
        dataProviderData_s,

        dpf_submit_s,
        dpf_fixProblem_s,
        dpf_yourName_s,
        dpf_emailAddress_s,
        dpf_Timestamp_s,
        dpf_frequency_s,
        dpf_title_s,
        dpf_titleTooltip_s,
        dpf_summary_s,
        dpf_summaryTooltip_s,
        dpf_creatorName_s,
        dpf_creatorNameTooltip_s,
        dpf_creatorType_s,
        dpf_creatorTypeTooltip_s,
        dpf_creatorEmail_s,
        dpf_creatorEmailTooltip_s,
        dpf_institution_s,
        dpf_institutionTooltip_s,
        dpf_infoUrl_s,
        dpf_infoUrlTooltip_s,
        dpf_license_s,
        dpf_licenseTooltip_s,
        dpf_howYouStoreData_s,
        dpf_required_s,
        dpf_optional_s,
        dpf_provideIfAvaliable_s,
        dpf_acknowledgement_s,
        dpf_acknowledgementTooltip_s,
        dpf_history_s,
        dpf_historyTooltip_s,
        dpf_idTooltip_s,
        dpf_namingAuthority_s,
        dpf_namingAuthorityTooltip_s,
        dpf_productVersion_s,
        dpf_productVersionTooltip_s,
        dpf_references_s,
        dpf_referencesTooltip_s,
        dpf_comment_s,
        dpf_commentTooltip_s,
        dpf_dataTypeHelp_s,
        dpf_ioosCategory_s,
        dpf_ioosCategoryHelp_s,
        dpf_part3Header_s,
        dpf_variableMetadata_s,
        dpf_sourceName_s,
        dpf_sourceNameTooltip_s,
        dpf_destinationName_s,
        dpf_destinationNameTooltip_s,
        dpf_longName_s,
        dpf_longNameTooltip_s,
        dpf_standardName_s,
        dpf_standardNameTooltip_s,
        dpf_dataType_s,
        dpf_fillValue_s,
        dpf_fillValueTooltip_s,
        dpf_units_s,
        dpf_unitsTooltip_s,
        dpf_range_s,
        dpf_rangeTooltip_s,
        dpf_part4Header_s,
        dpf_otherComment_s,
        dpf_finishPart4_s,
        dpf_congratulation_s,

        disabled_s,
        distinctValuesTooltip_s,
        doWithGraphs_s,

        dtAccessible_s,
        dtAccessibleYes_s,
        dtAccessibleGraphs_s,
        dtAccessibleNo_s,
        dtAccessibleLogIn_s,
        dtLogIn_s,
        dtDAF_s,
        dtFiles_s,
        dtMAG_s,
        dtSOS_s,
        dtSubset_s,
        dtWCS_s,
        dtWMS_s,

        EDDDatasetID_s,
        EDDFgdc_s,
        EDDFgdcMetadata_s,
        EDDFiles_s,
        EDDIso19115Metadata_s,
        EDDMetadata_s,
        EDDBackground_s,
        EDDClickOnSubmitHtml_s,
        EDDInstitution_s,
        EDDInformation_s,
        EDDSummary_s,
        EDDDatasetTitle_s,
        EDDDownloadData_s,
        EDDMakeAGraph_s,
        EDDMakeAMap_s,
        EDDFileType_s,
        EDDFileTypeInformation_s,
        EDDSelectFileType_s,
        EDDMinimum_s,
        EDDMaximum_s,
        EDDConstraint_s,

        EDDChangedWasnt_s,
        EDDChangedDifferentNVar_s,
        EDDChanged2Different_s,
        EDDChanged1Different_s,
        EDDChangedCGADifferent_s,
        EDDChangedAxesDifferentNVar_s,
        EDDChangedAxes2Different_s,
        EDDChangedAxes1Different_s,
        EDDChangedNoValue_s,
        EDDChangedTableToGrid_s,

        EDDSimilarDifferentNVar_s,
        EDDSimilarDifferent_s,

        EDDGridDapDescription_s,
        EDDGridDapLongDescription_s,
        EDDGridDownloadDataTooltip_s,
        EDDGridDimension_s,
        EDDGridDimensionRanges_s,
        EDDGridFirst_s,
        EDDGridLast_s,
        EDDGridStart_s,
        EDDGridStop_s,
        EDDGridStartStopTooltip_s,
        EDDGridStride_s,
        EDDGridNValues_s,
        EDDGridNValuesHtml_s,
        EDDGridSpacing_s,
        EDDGridJustOneValue_s,
        EDDGridEven_s,
        EDDGridUneven_s,
        EDDGridDimensionTooltip_s,
        EDDGridDimensionFirstTooltip_s,
        EDDGridDimensionLastTooltip_s,
        EDDGridVarHasDimTooltip_s,
        EDDGridSSSTooltip_s,
        EDDGridStartTooltip_s,
        EDDGridStopTooltip_s,
        EDDGridStrideTooltip_s,
        EDDGridSpacingTooltip_s,
        EDDGridDownloadTooltip_s,
        EDDGridGridVariableHtml_s,

        EDDTableConstraints_s,
        EDDTableTabularDatasetTooltip_s,
        EDDTableVariable_s,
        EDDTableCheckAll_s,
        EDDTableCheckAllTooltip_s,
        EDDTableUncheckAll_s,
        EDDTableUncheckAllTooltip_s,
        EDDTableMinimumTooltip_s,
        EDDTableMaximumTooltip_s,
        EDDTableCheckTheVariables_s,
        EDDTableSelectAnOperator_s,
        EDDTableFromEDDGridSummary_s,
        EDDTableOptConstraint1Html_s,
        EDDTableOptConstraint2Html_s,
        EDDTableOptConstraintVar_s,
        EDDTableNumericConstraintTooltip_s,
        EDDTableStringConstraintTooltip_s,
        EDDTableTimeConstraintTooltip_s,
        EDDTableConstraintTooltip_s,
        EDDTableSelectConstraintTooltip_s,
        EDDTableDapDescription_s,
        EDDTableDapLongDescription_s,
        EDDTableDownloadDataTooltip_s,

        erddapVersionHTML_s,
        errorTitle_s,
        errorRequestUrl_s,
        errorRequestQuery_s,
        errorTheError_s,
        errorCopyFrom_s,
        errorFileNotFound_s,
        errorFileNotFoundImage_s,
        errorInternal_s,
        errorJsonpFunctionName_s,
        errorJsonpNotAllowed_s,
        errorMoreThan2GB_s,
        errorNotFound_s,
        errorNotFoundIn_s,
        errorOdvLLTGrid_s,
        errorOdvLLTTable_s,
        errorOnWebPage_s,
        errorXWasntSpecified_s,
        errorXWasTooLong_s,
        externalLink_s,
        externalWebSite_s,
        fileHelp_asc_s,
        fileHelp_csv_s,
        fileHelp_csvp_s,
        fileHelp_csv0_s,
        fileHelp_dataTable_s,
        fileHelp_das_s,
        fileHelp_dds_s,
        fileHelp_dods_s,
        fileHelpGrid_esriAscii_s,
        fileHelpTable_esriCsv_s,
        fileHelp_fgdc_s,
        fileHelp_geoJson_s,
        fileHelp_graph_s,
        fileHelpGrid_help_s,
        fileHelpTable_help_s,
        fileHelp_html_s,
        fileHelp_htmlTable_s,
        fileHelp_iso19115_s,
        fileHelp_itxGrid_s,
        fileHelp_itxTable_s,
        fileHelp_json_s,
        fileHelp_jsonlCSV1_s,
        fileHelp_jsonlCSV_s,
        fileHelp_jsonlKVP_s,
        fileHelp_mat_s,
        fileHelpGrid_nc3_s,
        fileHelpGrid_nc4_s,
        fileHelpTable_nc3_s,
        fileHelpTable_nc4_s,
        fileHelp_nc3Header_s,
        fileHelp_nc4Header_s,
        fileHelp_nccsv_s,
        fileHelp_nccsvMetadata_s,
        fileHelp_ncCF_s,
        fileHelp_ncCFHeader_s,
        fileHelp_ncCFMA_s,
        fileHelp_ncCFMAHeader_s,
        fileHelp_ncml_s,
        fileHelp_ncoJson_s,
        fileHelpGrid_odvTxt_s,
        fileHelpTable_odvTxt_s,
        fileHelp_subset_s,
        fileHelp_timeGaps_s,
        fileHelp_tsv_s,
        fileHelp_tsvp_s,
        fileHelp_tsv0_s,
        fileHelp_wav_s,
        fileHelp_xhtml_s,
        fileHelp_geotif_s,  //graphical
        fileHelpGrid_kml_s,
        fileHelpTable_kml_s,
        fileHelp_smallPdf_s,
        fileHelp_pdf_s,
        fileHelp_largePdf_s,
        fileHelp_smallPng_s,
        fileHelp_png_s,
        fileHelp_largePng_s,
        fileHelp_transparentPng_s,
        filesDescription_s,
        filesSort_s,
        filesWarning_s,
        findOutChange_s,
        FIPSCountryCode_s,
        forSOSUse_s,
        forWCSUse_s,
        forWMSUse_s,
        functions_s,
        functionTooltip_s,
        functionDistinctCheck_s,
        functionDistinctTooltip_s,
        functionOrderByExtra_s,
        functionOrderByTooltip_s,
        functionOrderBySort_s,
        functionOrderBySort1_s,
        functionOrderBySort2_s,
        functionOrderBySort3_s,
        functionOrderBySort4_s,
        functionOrderBySortLeast_s,
        functionOrderBySortRowMax_s,
        generatedAt_s,
        geoServicesDescription_s,
        getStartedHtml_s,
        htmlTableMaxMessage_s,

        hpn_information_s,
        hpn_legalNotices_s,
        hpn_dataProviderForm_s,
        hpn_dataProviderFormP1_s,
        hpn_dataProviderFormP2_s,
        hpn_dataProviderFormP3_s,
        hpn_dataProviderFormP4_s,
        hpn_dataProviderFormDone_s,
        hpn_status_s,
        hpn_restfulWebService_s,
        hpn_documentation_s,
        hpn_help_s,
        hpn_files_s,
        hpn_SOS_s,
        hpn_WCS_s,
        hpn_slideSorter_s,
        hpn_add_s,
        hpn_list_s,
        hpn_validate_s,
        hpn_remove_s,
        hpn_convert_s,
        hpn_fipsCounty_s,
        hpn_OAAcronyms_s,
        hpn_OAVariableNames_s,
        hpn_keywords_s,
        hpn_time_s,
        hpn_units_s,

        imageDataCourtesyOf_s,
        indexViewAll_s,
        indexSearchWith_s,
        indexDevelopersSearch_s,
        indexProtocol_s,
        indexDescription_s,
        indexDatasets_s,
        indexDocumentation_s,
        indexRESTfulSearch_s,
        indexAllDatasetsSearch_s,
        indexOpenSearch_s,
        indexServices_s,
        indexDescribeServices_s,
        indexMetadata_s,
        indexWAF1_s,
        indexWAF2_s,
        indexConverters_s,
        indexDescribeConverters_s,
        infoAboutFrom_s,
        infoTableTitleHtml_s,
        infoRequestForm_s,
        inotifyFix_s,
        interpolate_s,
        javaProgramsHTML_s,
        justGenerateAndView_s,
        justGenerateAndViewTooltip_s,
        justGenerateAndViewUrl_s,
        justGenerateAndViewGraphUrlTooltip_s,
        keywords_word_s,
        langCode_s,
        legal_s,
        legalNotices_s,
        license_s,
        listAll_s,
        listOfDatasets_s,
        LogIn_s,
        login_s,
        loginHTML_s,
        loginAttemptBlocked_s,
        loginDescribeCustom_s,
        loginDescribeEmail_s,
        loginDescribeGoogle_s,
        loginDescribeOrcid_s,
        loginDescribeOauth2_s,
        loginErddap_s,
        loginCanNot_s,
        loginAreNot_s,
        loginToLogIn_s,
        loginEmailAddress_s,
        loginYourEmailAddress_s,
        loginUserName_s,
        loginPassword_s,
        loginUserNameAndPassword_s,
        loginGoogleSignIn_s,
        loginGoogleSignIn2_s,
        loginOrcidSignIn_s,
        loginOpenID_s,
        loginOpenIDOr_s,
        loginOpenIDCreate_s,
        loginOpenIDFree_s,
        loginOpenIDSame_s,
        loginAs_s,
        loginPartwayAs_s,
        loginFailed_s,
        loginSucceeded_s,
        loginInvalid_s,
        loginNot_s,
        loginBack_s,
        loginProblemExact_s,
        loginProblemExpire_s,
        loginProblemGoogleAgain_s,
        loginProblemOrcidAgain_s,
        loginProblemOauth2Again_s,
        loginProblemSameBrowser_s,
        loginProblem3Times_s,
        loginProblems_s,
        loginProblemsAfter_s,
        loginPublicAccess_s,
        LogOut_s,
        logout_s,
        logoutOpenID_s,
        logoutSuccess_s,
        mag_s,
        magAxisX_s,
        magAxisY_s,
        magAxisColor_s,
        magAxisStickX_s,
        magAxisStickY_s,
        magAxisVectorX_s,
        magAxisVectorY_s,
        magAxisHelpGraphX_s,
        magAxisHelpGraphY_s,
        magAxisHelpMarkerColor_s,
        magAxisHelpSurfaceColor_s,
        magAxisHelpStickX_s,
        magAxisHelpStickY_s,
        magAxisHelpMapX_s,
        magAxisHelpMapY_s,
        magAxisHelpVectorX_s,
        magAxisHelpVectorY_s,
        magAxisVarHelp_s,
        magAxisVarHelpGrid_s,
        magConstraintHelp_s,
        magDocumentation_s,
        magDownload_s,
        magDownloadTooltip_s,
        magFileType_s,
        magGraphType_s,
        magGraphTypeTooltipGrid_s,
        magGraphTypeTooltipTable_s,
        magGS_s,
        magGSMarkerType_s,
        magGSSize_s,
        magGSColor_s,
        magGSColorBar_s,
        magGSColorBarTooltip_s,
        magGSContinuity_s,
        magGSContinuityTooltip_s,
        magGSScale_s,
        magGSScaleTooltip_s,
        magGSMin_s,
        magGSMinTooltip_s,
        magGSMax_s,
        magGSMaxTooltip_s,
        magGSNSections_s,
        magGSNSectionsTooltip_s,
        magGSLandMask_s,
        magGSLandMaskTooltipGrid_s,
        magGSLandMaskTooltipTable_s,
        magGSVectorStandard_s,
        magGSVectorStandardTooltip_s,
        magGSYAscendingTooltip_s,
        magGSYAxisMin_s,
        magGSYAxisMax_s,
        magGSYRangeMinTooltip_s,
        magGSYRangeMaxTooltip_s,
        magGSYRangeTooltip_s,
        magGSYScaleTooltip_s,
        magItemFirst_s,
        magItemPrevious_s,
        magItemNext_s,
        magItemLast_s,
        magJust1Value_s,
        magRange_s,
        magRangeTo_s,
        magRedraw_s,
        magRedrawTooltip_s,
        magTimeRange_s,
        magTimeRangeFirst_s,
        magTimeRangeBack_s,
        magTimeRangeForward_s,
        magTimeRangeLast_s,
        magTimeRangeTooltip_s,
        magTimeRangeTooltip2_s,
        magTimesVary_s,
        magViewUrl_s,
        magZoom_s,
        magZoomCenter_s,
        magZoomCenterTooltip_s,
        magZoomIn_s,
        magZoomInTooltip_s,
        magZoomOut_s,
        magZoomOutTooltip_s,
        magZoomALittle_s,
        magZoomData_s,
        magZoomOutData_s,
        magGridTooltip_s,
        magTableTooltip_s,
        metadataDownload_s,
        moreInformation_s,
        nMatching1_s,
        nMatching_s,
        nMatchingAlphabetical_s,
        nMatchingMostRelevant_s,
        nMatchingPage_s,
        nMatchingCurrent_s,
        noDataFixedValue_s,
        noDataNoLL_s,
        noDatasetWith_s,
        noPage1_s,
        noPage2_s,
        notAllowed_s,
        notAuthorized_s,
        notAuthorizedForData_s,
        notAvailable_s,
        note_s,
        noXxx_s,
        noXxxBecause_s,
        noXxxBecause2_s,
        noXxxNotActive_s,
        noXxxNoAxis1_s,
        noXxxNoColorBar_s,
        noXxxNoCdmDataType_s,
        noXxxNoLL_s,
        noXxxNoLLEvenlySpaced_s,
        noXxxNoLLGt1_s,
        noXxxNoLLT_s,
        noXxxNoLonIn180_s,
        noXxxNoNonString_s,
        noXxxNo2NonString_s,
        noXxxNoStation_s,
        noXxxNoStationID_s,
        noXxxNoSubsetVariables_s,
        noXxxNoOLLSubsetVariables_s,
        noXxxNoMinMax_s,
        noXxxItsGridded_s,
        noXxxItsTabular_s,
        oneRequestAtATime_s,
        openSearchDescription_s,
        optional_s,
        options_s,
        orRefineSearchWith_s,
        orSearchWith_s,
        orComma_s,
        outOfDateKeepTrack_s,
        outOfDateHtml_s,
        
        patientData_s,
        patientYourGraph_s,
        percentEncode_s,
        pickADataset_s,
        protocolSearchHtml_s,
        protocolSearch2Html_s,
        protocolClick_s,

        
        queryError180_s,
        queryError1Value_s,
        queryError1Var_s,
        queryError2Var_s,
        queryErrorActualRange_s,
        queryErrorAdjusted_s,
        queryErrorAscending_s,
        queryErrorConstraintNaN_s,
        queryErrorEqualSpacing_s,
        queryErrorExpectedAt_s,
        queryErrorFileType_s,
        queryErrorInvalid_s,
        queryErrorLL_s,
        queryErrorLLGt1_s,
        queryErrorLLT_s,
        queryErrorNeverTrue_s,
        queryErrorNeverBothTrue_s,
        queryErrorNotAxis_s,
        queryErrorNotExpectedAt_s,
        queryErrorNotFoundAfter_s,
        queryErrorOccursTwice_s,
        queryErrorOrderByVariable_s,
        queryErrorUnknownVariable_s,

        queryErrorGrid1Axis_s,
        queryErrorGridAmp_s,
        queryErrorGridDiagnostic_s,
        queryErrorGridBetween_s,
        queryErrorGridLessMin_s,
        queryErrorGridGreaterMax_s,
        queryErrorGridMissing_s,
        queryErrorGridNoAxisVar_s,
        queryErrorGridNoDataVar_s,
        queryErrorGridNotIdentical_s,
        queryErrorGridSLessS_s,
        queryErrorLastEndP_s,
        queryErrorLastExpected_s,
        queryErrorLastUnexpected_s,
        queryErrorLastPMInvalid_s,
        queryErrorLastPMInteger_s,
        rangesFromTo_s,
        resetTheForm_s,
        resetTheFormWas_s,
        
        restfulWebServices_s,
        restfulHTML_s,
        restfulHTMLContinued_s,
        restfulGetAllDataset_s,
        restfulProtocols_s,
        SOSDocumentation_s,
        WCSDocumentation_s,
        WMSDocumentation_s,
        requestFormatExamplesHtml_s,
        resultsFormatExamplesHtml_s,
        resultsOfSearchFor_s,
        restfulInformationFormats_s,
        restfulViaService_s,
        rows_s,
        rssNo_s,
        searchTitle_s,
        searchDoFullTextHtml_s,
        searchFullTextHtml_s,
        searchHintsLuceneTooltip_s,
        searchHintsOriginalTooltip_s,
        searchHintsTooltip_s,
        searchButton_s,
        searchClickTip_s,
        searchMultipleERDDAPs_s,
        searchMultipleERDDAPsDescription_s,
        searchNotAvailable_s,
        searchTip_s,
        searchSpelling_s,
        searchFewerWords_s,
        searchWithQuery_s,
        seeProtocolDocumentation_s,
        selectNext_s,
        selectPrevious_s,
        shiftXAllTheWayLeft_s,
        shiftXLeft_s,
        shiftXRight_s,
        shiftXAllTheWayRight_s,
        sosDescriptionHtml_s,
        sosLongDescriptionHtml_s,
        sosOverview1_s,
        sosOverview2_s,
        //sparqlP01toP02pre,
        //sparqlP01toP02post,
        ssUse_s,
        ssUsePlain_s,
        ssBePatient_s,
        ssInstructionsHtml_s,
        statusHtml_s,
        submit_s,
        submitTooltip_s,
        subscriptionRSSHTML_s,
        subscriptionURLHTML_s,
        subscriptionsTitle_s,
        subscriptionAdd_s,
        subscriptionAddHtml_s,
        subscriptionValidate_s,
        subscriptionValidateHtml_s,
        subscriptionList_s,
        subscriptionListHtml_s,
        subscriptionRemove_s,
        subscriptionRemoveHtml_s,
        subscriptionAbuse_s,
        subscriptionAddError_s,
        subscriptionAdd2_s,
        subscriptionAddSuccess_s,
        subscriptionEmail_s,
        subscriptionEmailOnBlacklist_s,
        subscriptionEmailInvalid_s,
        subscriptionEmailTooLong_s,
        subscriptionEmailUnspecified_s,
        subscription0Html_s,
        subscription1Html_s,
        subscription2Html_s,
        subscriptionIDInvalid_s,
        subscriptionIDTooLong_s,
        subscriptionIDUnspecified_s,
        subscriptionKeyInvalid_s,
        subscriptionKeyUnspecified_s,
        subscriptionListError_s,
        subscriptionListSuccess_s,
        subscriptionRemoveError_s,
        subscriptionRemove2_s,
        subscriptionRemoveSuccess_s,
        subscriptionRSS_s,
        subscriptionsNotAvailable_s,
        subscriptionUrlHtml_s,
        subscriptionUrlInvalid_s,
        subscriptionUrlTooLong_s,
        subscriptionValidateError_s,
        subscriptionValidateSuccess_s,
        subset_s,
        subsetSelect_s,
        subsetNMatching_s,
        subsetInstructions_s,
        subsetOption_s,
        subsetOptions_s,
        subsetRefineMapDownload_s,
        subsetRefineSubsetDownload_s,
        subsetClickResetClosest_s,
        subsetClickResetLL_s,
        subsetMetadata_s,
        subsetCount_s,
        subsetPercent_s,
        subsetViewSelect_s,
        subsetViewSelectDistinctCombos_s,
        subsetViewSelectRelatedCounts_s,
        subsetWhen_s,
        subsetWhenNoConstraints_s,
        subsetWhenCounts_s,
        subsetComboClickSelect_s,
        subsetNVariableCombos_s,
        subsetShowingAllRows_s,
        subsetShowingNRows_s,
        subsetChangeShowing_s,
        subsetNRowsRelatedData_s,
        subsetViewRelatedChange_s,
        subsetTotalCount_s,
        subsetView_s,
        subsetViewCheck_s,
        subsetViewCheck1_s,
        subsetViewDistinctMap_s,
        subsetViewRelatedMap_s,
        subsetViewDistinctDataCounts_s,
        subsetViewDistinctData_s,
        subsetViewRelatedDataCounts_s,
        subsetViewRelatedData_s,
        subsetViewDistinctMapTooltip_s,
        subsetViewRelatedMapTooltip_s,
        subsetViewDistinctDataCountsTooltip_s,
        subsetViewDistinctDataTooltip_s,
        subsetViewRelatedDataCountsTooltip_s,
        subsetViewRelatedDataTooltip_s,
        subsetWarn_s,
        subsetWarn10000_s,
        subsetTooltip_s,
        subsetNotSetUp_s,
        subsetLongNotShown_s,

        tabledapVideoIntro_s,
        Then_s,
        time_s,
        timeoutOtherRequests_s,
        units_s,
        unknownDatasetID_s,
        unknownProtocol_s,
        unsupportedFileType_s,
        
        variableNames_s,
        viewAllDatasetsHtml_s,
        
        warning_s,

        wcsDescriptionHtml_s,
        wcsLongDescriptionHtml_s,
        wcsOverview1_s,
        wcsOverview2_s,

        wmsDescriptionHtml_s,
        WMSDocumentation1_s,
        WMSGetCapabilities_s,
        WMSGetMap_s,
        WMSNotes_s,
        wmsInstructions_s,
        wmsLongDescriptionHtml_s,
        wmsManyDatasets_s,
            
        zoomIn_s,
        zoomOut_s;

    
    private static String
        acceptEncodingHtml,
        filesDocumentation;
    public static String 
        acronyms,
        accessRESTFUL,
        addConstraints,
        addVarWhereAttName,
        addVarWhereAttValue,
        addVarWhere,
        additionalLinks,
        admSummary,
        admTitle,
        advl_datasetID,
        advc_accessible,
        advl_accessible,
        advl_institution,
        advc_dataStructure,
        advl_dataStructure,
        advl_cdm_data_type,
        advl_class,
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
        advancedSearchErrorHandling,
        autoRefresh,
        categoryTitleHtml,
        categoryHtml,
        category3Html,
        categoryPickAttribute,
        categorySearchHtml,
        categorySearchDifferentHtml,
        categoryClickHtml,
        categoryNotAnOption,
        caughtInterrupted,
        cdmDataTypeHelp,
        clickAccess,
        clickBackgroundInfo,
        clickERDDAP,
        clickInfo,
        clickToSubmit,
        converterWebService,
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
        convertInterpolate,
        convertInterpolateIntro,
        convertInterpolateTLLTable,
        convertInterpolateTLLTableHelp,
        convertInterpolateDatasetIDVariable,
        convertInterpolateDatasetIDVariableHelp,
        convertInterpolateNotes,
        convertInterpolateService,
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
        convertURLs,
        convertURLsIntro,
        convertURLsNotes,
        convertURLsService,
        cookiesHelp,
        daf,
        dafGridBypassTooltip,
        dafGridTooltip,
        dafTableBypassTooltip,
        dafTableTooltip,
        dasTitle,
        dataAccessNotAllowed,
        databaseUnableToConnect,
        dataProviderFormSuccess,
        dataProviderFormShortDescription,
        dataProviderFormLongDescriptionHTML,
        dataProviderFormPart1,
        dataProviderFormPart2Header,
        dataProviderFormPart2GlobalMetadata,
        dataProviderContactInfo,
        dataProviderData,
        dpf_submit,
        dpf_fixProblem,
        dpf_yourName,
        dpf_emailAddress,
        dpf_Timestamp,
        dpf_frequency,
        dpf_title,
        dpf_titleTooltip,
        dpf_summary,
        dpf_summaryTooltip,
        dpf_creatorName,
        dpf_creatorNameTooltip,
        dpf_creatorType,
        dpf_creatorTypeTooltip,
        dpf_creatorEmail,
        dpf_creatorEmailTooltip,
        dpf_institution,
        dpf_institutionTooltip,
        dpf_infoUrl,
        dpf_infoUrlTooltip,
        dpf_license,
        dpf_licenseTooltip,
        dpf_howYouStoreData,
        dpf_required,
        dpf_optional,
        dpf_provideIfAvaliable,
        dpf_acknowledgement,
        dpf_acknowledgementTooltip,
        dpf_history,
        dpf_historyTooltip,
        dpf_idTooltip,
        dpf_namingAuthority,
        dpf_namingAuthorityTooltip,
        dpf_productVersion,
        dpf_productVersionTooltip,
        dpf_references,
        dpf_referencesTooltip,
        dpf_comment,
        dpf_commentTooltip,
        dpf_dataTypeHelp,
        dpf_ioosCategory,
        dpf_ioosCategoryHelp,
        dpf_part3Header,
        dpf_variableMetadata,
        dpf_sourceName,
        dpf_sourceNameTooltip,
        dpf_destinationName,
        dpf_destinationNameTooltip,
        dpf_longName,
        dpf_longNameTooltip,
        dpf_standardName,
        dpf_standardNameTooltip,
        dpf_dataType,
        dpf_fillValue,
        dpf_fillValueTooltip,
        dpf_units,
        dpf_unitsTooltip,
        dpf_range,
        dpf_rangeTooltip,
        dpf_part4Header,
        dpf_otherComment,
        dpf_finishPart4,
        dpf_congratulation,
        disabled,
        distinctValuesTooltip,
        doWithGraphs,
        dtAccessible,
        dtAccessibleYes,
        dtAccessibleGraphs,
        dtAccessibleNo,
        dtAccessibleLogIn,
        dtLogIn,
        dtDAF,
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
        EDDTableFromEDDGridSummary,
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
        EDDTableFromHttpGetDatasetDescription,
        EDDTableFromHttpGetAuthorDescription,
        EDDTableFromHttpGetTimestampDescription,
        erddapVersionHTML,
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
        externalLink,
        externalWebSite,
        fileHelp_asc,
        fileHelp_csv,
        fileHelp_csvp,
        fileHelp_csv0,
        fileHelp_dataTable,
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
        fileHelp_jsonlCSV1,
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
        fileHelp_geotif,
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
        filesSort,
        filesWarning,
        findOutChange,
        FIPSCountryCode,
        forSOSUse,
        forWCSUse,
        forWMSUse,
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
        hpn_information,
        hpn_legalNotices,
        hpn_dataProviderForm,
        hpn_dataProviderFormP1,
        hpn_dataProviderFormP2,
        hpn_dataProviderFormP3,
        hpn_dataProviderFormP4,
        hpn_dataProviderFormDone,
        hpn_status,
        hpn_restfulWebService,
        hpn_documentation,
        hpn_help,
        hpn_files,
        hpn_SOS,
        hpn_WCS,
        hpn_slideSorter,
        hpn_add,
        hpn_list,
        hpn_validate,
        hpn_remove,
        hpn_convert,
        hpn_fipsCounty,
        hpn_OAAcronyms,
        hpn_OAVariableNames,
        hpn_keywords,
        hpn_time,
        hpn_units,
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
        interpolate,
        javaProgramsHTML,
        justGenerateAndView,
        justGenerateAndViewTooltip,
        justGenerateAndViewUrl,
        justGenerateAndViewGraphUrlTooltip,
        keywords_word,
        langCode,
        legal,
        legalNotices,
        license,
        listAll,
        listOfDatasets,
        LogIn,
        login,
        loginHTML,
        loginAttemptBlocked,
        loginDescribeCustom,
        loginDescribeEmail,
        loginDescribeGoogle,
        loginDescribeOrcid,
        loginDescribeOauth2,
        loginErddap,
        loginCanNot,
        loginAreNot,
        loginToLogIn,
        loginEmailAddress,
        loginYourEmailAddress,
        loginUserName,
        loginPassword,
        loginUserNameAndPassword,
        loginGoogleSignIn,
        loginGoogleSignIn2,
        loginOrcidSignIn,
        loginOpenID,
        loginOpenIDOr,
        loginOpenIDCreate,
        loginOpenIDFree,
        loginOpenIDSame,
        loginAs,
        loginPartwayAs,
        loginFailed,
        loginSucceeded,
        loginInvalid,
        loginNot,
        loginBack,
        loginProblemExact,
        loginProblemExpire,
        loginProblemGoogleAgain,
        loginProblemOrcidAgain,
        loginProblemOauth2Again,
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
        magGSYScaleTooltip,
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
        note,
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
        oneRequestAtATime,
        openSearchDescription,
        optional,
        options,
        orRefineSearchWith,
        orSearchWith,
        orComma,
        outOfDateKeepTrack,
        outOfDateHtml,
        patientData,
        patientYourGraph,
        percentEncode,
        pickADataset,
        protocolSearchHtml,
        protocolSearch2Html,
        protocolClick,
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

        restfulWebServices,
        restfulHTML,
        restfulHTMLContinued,
        restfulGetAllDataset,
        restfulProtocols,
        SOSDocumentation,
        WCSDocumentation,
        WMSDocumentation,
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
        searchMultipleERDDAPs,
        searchMultipleERDDAPsDescription,
        searchNotAvailable,
        searchTip,
        searchSpelling,
        searchFewerWords,
        searchWithQuery,
        seeProtocolDocumentation,
        selectNext,
        selectPrevious,
        shiftXAllTheWayLeft,
        shiftXLeft,
        shiftXRight,
        shiftXAllTheWayRight,
        sosDescriptionHtml,
        sosLongDescriptionHtml,
        sosOverview1,
        sosOverview2,


        ssUse,
        ssUsePlain,
        ssBePatient,
        ssInstructionsHtml,
        statusHtml,
        submit,
        submitTooltip,
        subscriptionRSSHTML,
        subscriptionURLHTML,
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
        subscriptionEmailOnBlacklist,
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
        time,
        timeoutOtherRequests,
        units,
        unknownDatasetID,
        unknownProtocol,
        unsupportedFileType,
        variableNames,
        viewAllDatasetsHtml,
        warning,
        wcsDescriptionHtml,
        wcsLongDescriptionHtml,
        wcsOverview1,
        wcsOverview2,
        wmsDescriptionHtml,
        WMSDocumentation1,
        WMSGetCapabilities,
        WMSGetMap,
        WMSNotes,
        wmsInstructions,
        wmsLongDescriptionHtml,
        wmsManyDatasets,
        zoomIn,
        zoomOut,
        theLongDescriptionHtml;

    public static int[] imageWidths, imageHeights, pdfWidths, pdfHeights;
    private static String[] theLongDescriptionHtml_s; //see the xxx() methods
    public static String errorFromDataSource = String2.ERROR + " from data source: ";
    //must be in the same order of translate.languageCodeList
    public static final String[] languageList = {"English (default)", "Chinese"};
    //this differs from translate.languageCodeList because the index 0 is "en"
    public static String[] fullLanguageCodeList;
    

    public static int currLang = 0;

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
        SSR.erddapVersion = erddapVersion;

        String eol = String2.lineSeparator;
        String2.log(eol + "////**** " + erdStartup + eol +
            "localTime=" + Calendar2.getCurrentISODateTimeStringLocalTZ() + eol +
            "erddapVersion=" + erddapVersion + eol + 
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


        //**** setup.xml  *************************************************************
        //This is read BEFORE messages.xml. If that is a problem for something,
        //  defer reading it in setup and add it to the messages section.
        //read static Strings from setup.xml 
        String setupFileName = contentDirectory + 
            "setup" + (developmentMode? "2" : "") + ".xml";
        errorInMethod = "ERROR while reading " + setupFileName + ": ";
        ResourceBundle2 setup = ResourceBundle2.fromXml(XML.parseXml(setupFileName, false));
        Map<String,String> ev = System.getenv();

        //logLevel may be: warning, info(default), all
        setLogLevel(getSetupEVString(setup, ev, "logLevel", DEFAULT_logLevel));
        bigParentDirectory = getSetupEVNotNothingString(setup, ev, "bigParentDirectory", ""); 
        bigParentDirectory = File2.addSlash(bigParentDirectory);
        Test.ensureTrue(File2.isDirectory(bigParentDirectory),  
            "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");
        unitTestDataDir    = getSetupEVString(setup, ev, "unitTestDataDir",    "[specify <unitTestDataDir> in setup.xml]"); 
        unitTestBigDataDir = getSetupEVString(setup, ev, "unitTestBigDataDir", "[specify <unitTestBigDataDir> in setup.xml]"); 
        unitTestDataDir    = File2.addSlash(unitTestDataDir);
        unitTestBigDataDir = File2.addSlash(unitTestBigDataDir);
        String2.unitTestDataDir    = unitTestDataDir;
        String2.unitTestBigDataDir = unitTestBigDataDir;

        //email  (do early on so email can be sent if trouble later in this method)
        emailSmtpHost          = getSetupEVString(setup, ev, "emailSmtpHost",  null);
        emailSmtpPort          = getSetupEVInt(   setup, ev, "emailSmtpPort",  25);
        emailUserName          = getSetupEVString(setup, ev, "emailUserName",  null);
        emailPassword          = getSetupEVString(setup, ev, "emailPassword",  null);
        emailProperties        = getSetupEVString(setup, ev, "emailProperties",  null);
        emailFromAddress       = getSetupEVString(setup, ev, "emailFromAddress", null);
        emailEverythingToCsv   = getSetupEVString(setup, ev, "emailEverythingTo", "");  //won't be null
        emailDailyReportToCsv  = getSetupEVString(setup, ev, "emailDailyReportTo", ""); //won't be null

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

        //*** set up directories  //all with slashes at end
        //before 2011-12-30, was fullDatasetInfoDirectory datasetInfo/; see conversion below
        fullDatasetDirectory      = bigParentDirectory + "dataset/";  
        fullFileVisitorDirectory  = fullDatasetDirectory + "_FileVisitor/";
        FileVisitorDNLS.FILE_VISITOR_DIRECTORY = fullFileVisitorDirectory;
        File2.deleteAllFiles(fullFileVisitorDirectory); //no temp file list can be active at ERDDAP restart
        fullCacheDirectory        = bigParentDirectory + "cache/";
        fullDecompressedDirectory = bigParentDirectory + "decompressed/";
        fullDecompressedGenerateDatasetsXmlDirectory
                                  = bigParentDirectory + "decompressed/GenerateDatasetsXml/";
        fullResetFlagDirectory    = bigParentDirectory + "flag/";
        fullBadFilesFlagDirectory = bigParentDirectory + "badFilesFlag/";
        fullHardFlagDirectory     = bigParentDirectory + "hardFlag/";
        fullLogsDirectory         = bigParentDirectory + "logs/";
        fullCopyDirectory         = bigParentDirectory + "copy/";
        fullLuceneDirectory       = bigParentDirectory + "lucene/";

        Test.ensureTrue(File2.isDirectory(fullPaletteDirectory),  
            "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
        errorInMethod = "ERROR while creating directories: "; //File2.makeDir throws exception if failure
        File2.makeDirectory(fullPublicDirectory);  //make it, because Git doesn't track empty dirs
        File2.makeDirectory(fullDatasetDirectory); 
        File2.makeDirectory(fullCacheDirectory);
        File2.makeDirectory(fullDecompressedDirectory);
        File2.makeDirectory(fullDecompressedGenerateDatasetsXmlDirectory);
        File2.makeDirectory(fullResetFlagDirectory);
        File2.makeDirectory(fullBadFilesFlagDirectory);
        File2.makeDirectory(fullHardFlagDirectory);
        File2.makeDirectory(fullLogsDirectory);
        File2.makeDirectory(fullCopyDirectory);
        File2.makeDirectory(fullLuceneDirectory);

        String2.log(
            "bigParentDirectory=" + bigParentDirectory + eol +
            "webInfParentDirectory=" + webInfParentDirectory);

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
        baseUrl                    = getSetupEVNotNothingString(setup, ev, "baseUrl",                    errorInMethod);
        baseHttpsUrl               = getSetupEVString(          setup, ev, "baseHttpsUrl",               "(not specified)"); //not "" (to avoid relative urls)
        categoryAttributes         = String2.split(getSetupEVNotNothingString(setup, ev, "categoryAttributes", ""), ',');
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

        String wmsActiveString     = getSetupEVString(setup, ev, "wmsActive",                  ""); 
        wmsActive                  = String2.isSomething(wmsActiveString)? String2.parseBoolean(wmsActiveString) : true;
        wmsSampleDatasetID         = getSetupEVString(setup, ev, "wmsSampleDatasetID",         wmsSampleDatasetID);
        wmsSampleVariable          = getSetupEVString(setup, ev, "wmsSampleVariable",          wmsSampleVariable);
        wmsSampleBBox110           = getSetupEVString(setup, ev, "wmsSampleBBox110",           wmsSampleBBox110);
        wmsSampleBBox130           = getSetupEVString(setup, ev, "wmsSampleBBox130",           wmsSampleBBox130);
        wmsSampleTime              = getSetupEVString(setup, ev, "wmsSampleTime",              wmsSampleTime);

        adminInstitution           = getSetupEVNotNothingString(setup, ev, "adminInstitution",           errorInMethod);
        adminInstitutionUrl        = getSetupEVNotNothingString(setup, ev, "adminInstitutionUrl",        errorInMethod);
        adminIndividualName        = getSetupEVNotNothingString(setup, ev, "adminIndividualName",        errorInMethod);
        adminPosition              = getSetupEVNotNothingString(setup, ev, "adminPosition",              errorInMethod);
        adminPhone                 = getSetupEVNotNothingString(setup, ev, "adminPhone",                 errorInMethod); 
        adminAddress               = getSetupEVNotNothingString(setup, ev, "adminAddress",               errorInMethod);
        adminCity                  = getSetupEVNotNothingString(setup, ev, "adminCity",                  errorInMethod);
        adminStateOrProvince       = getSetupEVNotNothingString(setup, ev, "adminStateOrProvince",       errorInMethod); 
        adminPostalCode            = getSetupEVNotNothingString(setup, ev, "adminPostalCode",            errorInMethod);
        adminCountry               = getSetupEVNotNothingString(setup, ev, "adminCountry",               errorInMethod);
        adminEmail                 = getSetupEVNotNothingString(setup, ev, "adminEmail",                 errorInMethod);

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
        if (!String2.isEmailAddress(adminEmail) || adminEmail.startsWith("your.")) //prohibit default adminEmail
            throw new RuntimeException("setup.xml error: invalid <adminEmail>=" + adminEmail);  

        accessConstraints          = getSetupEVNotNothingString(setup, ev, "accessConstraints",          errorInMethod); 
        accessRequiresAuthorization= getSetupEVNotNothingString(setup, ev, "accessRequiresAuthorization",errorInMethod); 
        fees                       = getSetupEVNotNothingString(setup, ev, "fees",                       errorInMethod);
        keywords                   = getSetupEVNotNothingString(setup, ev, "keywords",                   errorInMethod);

        awsS3OutputBucketUrl       = getSetupEVString(          setup, ev, "awsS3OutputBucketUrl",       null);
        if (!String2.isSomething(awsS3OutputBucketUrl))
            awsS3OutputBucketUrl = null;
        if (awsS3OutputBucketUrl != null) {
            //If something was specified, ERDDAP insists that it be valid, so set it up.
            //This code is based on https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html#list-object
            //  was v1.1 https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingObjectKeysUsingJava.html

            //ensure that it is valid
            String s3parts[] = String2.parseAwsS3Url(awsS3OutputBucketUrl);
            if (s3parts == null)
                throw new RuntimeException(
                    "The value of <awsS3OutputBucketUrl> specified in setup.xml doesn't match this regular expression: " +
                    String2.AWS_S3_REGEX);

            //ensure that the keyName (or prefix) is ""
            awsS3OutputBucket = s3parts[0]; 
            String region     = s3parts[1]; 
            String prefix     = s3parts[2]; 
            if (prefix.length() > 0) 
                throw new RuntimeException(
                    "The value of <awsS3OutputBucket> specified in setup.xml must not include an object key (AKA directory or file name).");

            //build the s3client for use with awsS3OutputBucket
            S3Client awsS3OutputClient = S3Client.builder()
//                .credentials(ProfileCredentialsProvider.create())
                .region(Region.of(region))  
                .build();               

            //note that I could set LifecycleRule(s) for the bucket via
            //awsS3OutputClient.putBucketLifecycleConfiguration
            //but LifecycleRule precision seems to be days, not e.g., minutes
            //So make my own system
        }

        units_standard             = getSetupEVString( setup, ev,          "units_standard",             "UDUNITS");

        fgdcActive                 = getSetupEVBoolean(setup, ev,          "fgdcActive",                 true); 
        iso19115Active             = getSetupEVBoolean(setup, ev,          "iso19115Active",             true); 
        jsonldActive               = getSetupEVBoolean(setup, ev,          "jsonldActive",               true); 
//until geoServicesRest is finished, it is always inactive
geoServicesRestActive      = false; //getSetupEVBoolean(setup, ev,          "geoServicesRestActive",      false); 
        filesActive                = getSetupEVBoolean(setup, ev,          "filesActive",                true); 
        defaultAccessibleViaFiles  = getSetupEVBoolean(setup, ev,          "defaultAccessibleViaFiles",  false); //false matches historical behavior 
        dataProviderFormActive     = getSetupEVBoolean(setup, ev,          "dataProviderFormActive",     true); 
        outOfDateDatasetsActive    = getSetupEVBoolean(setup, ev,          "outOfDateDatasetsActive",    true); 
        politicalBoundariesActive  = getSetupEVBoolean(setup, ev,          "politicalBoundariesActive",  true); 
        wmsClientActive            = getSetupEVBoolean(setup, ev,          "wmsClientActive",            true); 
        SgtMap.drawPoliticalBoundaries = politicalBoundariesActive;


//until SOS is finished, it is always inactive
sosActive = false;//        sosActive                  = getSetupEVBoolean(setup, ev,          "sosActive",                  false); 
        if (sosActive) {
            sosFeatureOfInterest   = getSetupEVNotNothingString(setup, ev, "sosFeatureOfInterest",       errorInMethod);
            sosStandardNamePrefix  = getSetupEVNotNothingString(setup, ev, "sosStandardNamePrefix",      errorInMethod);
            sosUrnBase             = getSetupEVNotNothingString(setup, ev, "sosUrnBase",                 errorInMethod);

            //make the sosGmlName, e.g., https://coastwatch.pfeg.noaa.gov -> gov.noaa.pfeg.coastwatch
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
wcsActive = false; //getSetupEVBoolean(setup, ev,          "wcsActive",                  false); 

        authentication             = getSetupEVString(setup, ev,           "authentication",             "");
        datasetsRegex              = getSetupEVString(setup, ev,           "datasetsRegex",              ".*");
        drawLandMask               = getSetupEVString(setup, ev,           "drawLandMask",               null);    //new name
        if (drawLandMask == null) //2014-08-28 changed defaults below to "under". It will be in v1.48
            drawLandMask           = getSetupEVString(setup, ev,           "drawLand",                   DEFAULT_drawLandMask); //old name. DEFAULT...="under"
        int tdlm = String2.indexOf(SgtMap.drawLandMask_OPTIONS, drawLandMask);
        if (tdlm < 1)
            drawLandMask = DEFAULT_drawLandMask; //"under"
        flagKeyKey                 = getSetupEVNotNothingString(setup, ev, "flagKeyKey",                 errorInMethod);
        if (flagKeyKey.toUpperCase().indexOf("CHANGE THIS") >= 0)
              //really old default: "A stitch in time saves nine. CHANGE THIS!!!"  
              //current default:    "CHANGE THIS TO YOUR FAVORITE QUOTE"
            throw new RuntimeException(
            String2.ERROR + ": You must change the <flagKeyKey> in setup.xml to a new, unique, non-default value. " +
            "NOTE that this will cause the flagKeys used by your datasets to change. " +
            "Any subscriptions using the old flagKeys will need to be redone.");
        fontFamily                 = getSetupEVString(setup, ev,           "fontFamily",                 "DejaVu Sans");
        graphBackgroundColor = new Color(String2.parseInt(
                                     getSetupEVString(setup, ev,           "graphBackgroundColor",       "" + DEFAULT_graphBackgroundColorInt)), true); //hasAlpha
        googleClientID             = getSetupEVString(setup, ev,           "googleClientID",             null);
        orcidClientID              = getSetupEVString(setup, ev,           "orcidClientID",              null);
        orcidClientSecret          = getSetupEVString(setup, ev,           "orcidClientSecret",          null);
        googleEarthLogoFile        = getSetupEVNotNothingString(setup, ev, "googleEarthLogoFile",        errorInMethod);
        highResLogoImageFile       = getSetupEVNotNothingString(setup, ev, "highResLogoImageFile",       errorInMethod);
        listPrivateDatasets        = getSetupEVBoolean(setup, ev,          "listPrivateDatasets",        false);
        logMaxSizeMB               = Math2.minMax(1, 2000, getSetupEVInt(setup, ev, "logMaxSizeMB", 20));  //2048MB=2GB

        //v2.00: these are now also in datasets.xml
        cacheMillis                = getSetupEVInt(setup, ev,              "cacheMinutes",               DEFAULT_cacheMinutes)            * 60000L; 
        loadDatasetsMinMillis      = Math.max(1,getSetupEVInt(setup, ev,   "loadDatasetsMinMinutes",     DEFAULT_loadDatasetsMinMinutes)) * 60000L;
        loadDatasetsMaxMillis      = getSetupEVInt(setup, ev,              "loadDatasetsMaxMinutes",     DEFAULT_loadDatasetsMaxMinutes)  * 60000L;
        loadDatasetsMaxMillis      = Math.max(loadDatasetsMinMillis * 2, loadDatasetsMaxMillis);
        partialRequestMaxBytes     = getSetupEVInt(setup, ev,              "partialRequestMaxBytes",     DEFAULT_partialRequestMaxBytes);
        partialRequestMaxCells     = getSetupEVInt(setup, ev,              "partialRequestMaxCells",     DEFAULT_partialRequestMaxCells);
        unusualActivity            = getSetupEVInt(setup, ev,              "unusualActivity",            DEFAULT_unusualActivity);

        lowResLogoImageFile        = getSetupEVNotNothingString(setup, ev, "lowResLogoImageFile",        errorInMethod);
        quickRestart               = getSetupEVBoolean(setup, ev,          "quickRestart",               true);      
        passwordEncoding           = getSetupEVString(setup, ev,           "passwordEncoding",           "UEPSHA256");
        searchEngine               = getSetupEVString(setup, ev,           "searchEngine",               "original");

        subscribeToRemoteErddapDataset = getSetupEVBoolean(setup, ev,      "subscribeToRemoteErddapDataset", true);
        subscriptionSystemActive   = getSetupEVBoolean(setup, ev,          "subscriptionSystemActive",       true);
        convertersActive           = getSetupEVBoolean(setup, ev,          "convertersActive",               true);
        slideSorterActive          = getSetupEVBoolean(setup, ev,          "slideSorterActive",              true);
        variablesMustHaveIoosCategory = getSetupEVBoolean(setup, ev,       "variablesMustHaveIoosCategory",  true);
        warName                    = getSetupEVString(setup, ev,           "warName",                        "erddap");

        //use Lucence?
        if (searchEngine.equals("lucene")) {
            useLuceneSearchEngine = true;
            luceneDocNToDatasetID = new ConcurrentHashMap();
        } else {
            Test.ensureEqual(searchEngine, "original", 
                "<searchEngine> must be \"original\" (the default) or \"lucene\".");
        }
        
       
        errorInMethod = "ERROR while initializing SgtGraph: ";
        sgtGraph = new SgtGraph(fontFamily);

        //ensure erddapVersion is okay
        int upo = erddapVersion.indexOf('_');
        double eVer = String2.parseDouble(upo >= 0? erddapVersion.substring(0, upo) :
            erddapVersion);
        if (upo == -1 && erddapVersion.length() == 4 && eVer > 1.8 && eVer < 10) {} //it's just a number
        else if ((upo != -1 && upo != 4) ||
            eVer <= 1.8 || eVer >=10 || Double.isNaN(eVer) || 
            erddapVersion.indexOf(' ') >= 0 ||
            !String2.isAsciiPrintable(erddapVersion))
            throw new SimpleException(
                "The format of EDStatic.erddapVersion must be d.dd[_someAsciiText]. (eVer=" + eVer + ")");            

        //ensure authentication setup is okay
        errorInMethod = "ERROR while checking authentication setup: ";
        if (authentication == null)
            authentication = "";
        authentication = authentication.trim().toLowerCase();
        if (!authentication.equals("") &&
            !authentication.equals("custom") &&
            !authentication.equals("email")  &&
            !authentication.equals("google") &&
            !authentication.equals("orcid")  &&
            !authentication.equals("oauth2") 
            )
            throw new RuntimeException(
                "setup.xml error: authentication=" + authentication + 
                " must be (nothing)|custom|email|google|orcid|oauth2."); 
        if (!authentication.equals("") && !baseHttpsUrl.startsWith("https://"))
            throw new RuntimeException(
                "setup.xml error: " + 
                ": For any <authentication> other than \"\", the baseHttpsUrl=" + baseHttpsUrl + 
                " must start with \"https://\".");
        if ((authentication.equals("google") || authentication.equals("auth2")) && 
            !String2.isSomething(googleClientID))
            throw new RuntimeException(
                "setup.xml error: " + 
                ": When authentication=google or oauth2, you must provide your <googleClientID>.");
        if ((authentication.equals("orcid") || authentication.equals("auth2")) && 
            (!String2.isSomething(orcidClientID) || !String2.isSomething(orcidClientSecret)))
            throw new RuntimeException(
                "setup.xml error: " + 
                ": When authentication=orcid or oauth2, you must provide your <orcidClientID> and <orcidClientSecret>.");
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

        //copy all <contentDirectory>images/ (and subdirectories) files to imageDir (and subdirectories)
        String tFiles[] = RegexFilenameFilter.recursiveFullNameList(
            contentDirectory + "images/", ".+", false);
        for (int i = 0; i < tFiles.length; i++) {
            int tpo = tFiles[i].indexOf("/images/");
            if (tpo < 0) tpo = tFiles[i].indexOf("\\images\\");
            if (tpo < 0) {
                String2.log("'/images/' not found in images/ file: " + tFiles[i]);
                continue;
            }
            String tName = tFiles[i].substring(tpo + 8);
            if (verbose) String2.log("  copying images/ file: " + tName);
            File2.copy(contentDirectory + "images/" + tName, imageDir + tName);
        }

        //ensure images exist and get their sizes
        Image tImage = Image2.getImage(imageDir + lowResLogoImageFile, 10000, false);
        lowResLogoImageFileWidth   = tImage.getWidth(null);
        lowResLogoImageFileHeight  = tImage.getHeight(null);
        tImage = Image2.getImage(imageDir + highResLogoImageFile, 10000, false);
        highResLogoImageFileWidth  = tImage.getWidth(null);
        highResLogoImageFileHeight = tImage.getHeight(null);
        tImage = Image2.getImage(imageDir + googleEarthLogoFile, 10000, false);
        googleEarthLogoFileWidth   = tImage.getWidth(null);
        googleEarthLogoFileHeight  = tImage.getHeight(null);

        //copy all <contentDirectory>cptfiles/ files to cptfiles
        tFiles = RegexFilenameFilter.list(contentDirectory + "cptfiles/", ".+\\.cpt"); //not recursive
        for (int i = 0; i < tFiles.length; i++) {
            if (verbose) 
                String2.log("  copying cptfiles/ file: " + tFiles[i]);
            File2.copy(contentDirectory + "cptfiles/" + tFiles[i], fullPaletteDirectory + tFiles[i]);
        }


        
        //**** messages.xml *************************************************************
        //This is read AFTER setup.xml. If that is a problem for something, defer reading it in setup and add it below.
        //Read static messages from messages(2).xml in contentDirectory.
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
        ResourceBundle2 messages0 = ResourceBundle2.fromXml(XML.parseXml(messagesFileName, false));

        fullLanguageCodeList = new String[languageList.length];
        fullLanguageCodeList[0] = "en";
        for (int i = 1; i < fullLanguageCodeList.length; i++) {
            fullLanguageCodeList[i] = translate.languageCodeList[i - 1];
        }
        ResourceBundle2[] messages = new ResourceBundle2[languageList.length];
        //messages[0] is either the custom messages.xml or the one provided by Erddap
        messages[0] = messages0;
        for (int i = 1; i < fullLanguageCodeList.length; i++) {
            String translatedFilePath = String2.getClassPath() + //WEB-INF directory
                "gov/noaa/pfel/erddap/util/translatedMessages/messages-" + 
                fullLanguageCodeList[i] + ".xml";
            messages[i] = ResourceBundle2.fromXml(XML.parseXml(translatedFilePath, false));
        }
        
        //read all the static Strings from messages.xml
        acceptEncodingHtml_s         = getMessageInAllVersions(messages, "acceptEncodingHtml",         errorInMethod);
        accessRESTFUL_s              = getMessageInAllVersions(messages, "accessRestful",              errorInMethod);
        acronyms_s                   = getMessageInAllVersions(messages, "acronyms",                   errorInMethod);
        addConstraints_s             = getMessageInAllVersions(messages, "addConstraints",             errorInMethod);
        addVarWhereAttName_s         = getMessageInAllVersions(messages, "addVarWhereAttName",         errorInMethod);
        addVarWhereAttValue_s        = getMessageInAllVersions(messages, "addVarWhereAttValue",        errorInMethod);
        addVarWhere_s                = getMessageInAllVersions(messages, "addVarWhere",                errorInMethod);
        additionalLinks_s            = getMessageInAllVersions(messages, "additionalLinks",            errorInMethod);
        admKeywords                = messages0.getNotNothingString("admKeywords",                errorInMethod);
        admSubsetVariables         = messages0.getNotNothingString("admSubsetVariables",         errorInMethod);

        admSummary_s                 = getMessageInAllVersions(messages, "admSummary",                 errorInMethod);
        admTitle_s                   = getMessageInAllVersions(messages, "admTitle",                   errorInMethod);
        advl_datasetID_s             = getMessageInAllVersions(messages, "advl_datasetID",             errorInMethod);
        advc_accessible_s            = getMessageInAllVersions(messages, "advc_accessible",            errorInMethod);
        advl_accessible_s            = getMessageInAllVersions(messages, "advl_accessible",            errorInMethod);
        advl_institution_s           = getMessageInAllVersions(messages, "advl_institution",           errorInMethod);
        advc_dataStructure_s         = getMessageInAllVersions(messages, "advc_dataStructure",         errorInMethod);
        advl_dataStructure_s         = getMessageInAllVersions(messages, "advl_dataStructure",         errorInMethod);
        //advr's are Url
        advr_dataStructure         = messages0.getNotNothingString("advr_dataStructure",         errorInMethod);
        advl_cdm_data_type_s         = getMessageInAllVersions(messages, "advl_cdm_data_type",         errorInMethod);
        advr_cdm_data_type         = messages0.getNotNothingString("advr_cdm_data_type",         errorInMethod);
        advl_class_s                 = getMessageInAllVersions(messages, "advl_class",                 errorInMethod);
        advr_class                 = messages0.getNotNothingString("advr_class",                 errorInMethod);
        advl_title_s                 = getMessageInAllVersions(messages, "advl_title",                 errorInMethod);
        advl_minLongitude_s          = getMessageInAllVersions(messages, "advl_minLongitude",          errorInMethod);
        advl_maxLongitude_s          = getMessageInAllVersions(messages, "advl_maxLongitude",          errorInMethod);
        advl_longitudeSpacing_s      = getMessageInAllVersions(messages, "advl_longitudeSpacing",      errorInMethod);
        advl_minLatitude_s           = getMessageInAllVersions(messages, "advl_minLatitude",           errorInMethod);
        advl_maxLatitude_s           = getMessageInAllVersions(messages, "advl_maxLatitude",           errorInMethod);
        advl_latitudeSpacing_s       = getMessageInAllVersions(messages, "advl_latitudeSpacing",       errorInMethod);
        advl_minAltitude_s           = getMessageInAllVersions(messages, "advl_minAltitude",           errorInMethod);
        advl_maxAltitude_s           = getMessageInAllVersions(messages, "advl_maxAltitude",           errorInMethod);
        advl_minTime_s               = getMessageInAllVersions(messages, "advl_minTime",               errorInMethod);
        advc_maxTime_s               = getMessageInAllVersions(messages, "advc_maxTime",               errorInMethod);
        advl_maxTime_s               = getMessageInAllVersions(messages, "advl_maxTime",               errorInMethod);
        advl_timeSpacing_s           = getMessageInAllVersions(messages, "advl_timeSpacing",           errorInMethod);
        advc_griddap_s               = getMessageInAllVersions(messages, "advc_griddap",               errorInMethod);
        advl_griddap_s               = getMessageInAllVersions(messages, "advl_griddap",               errorInMethod);
        advl_subset_s                = getMessageInAllVersions(messages, "advl_subset",                errorInMethod);
        advc_tabledap_s              = getMessageInAllVersions(messages, "advc_tabledap",              errorInMethod);
        advl_tabledap_s              = getMessageInAllVersions(messages, "advl_tabledap",              errorInMethod);
        advl_MakeAGraph_s            = getMessageInAllVersions(messages, "advl_MakeAGraph",            errorInMethod);
        advc_sos_s                   = getMessageInAllVersions(messages, "advc_sos",                   errorInMethod);
        advl_sos_s                   = getMessageInAllVersions(messages, "advl_sos",                   errorInMethod);
        advl_wcs_s                   = getMessageInAllVersions(messages, "advl_wcs",                   errorInMethod);
        advl_wms_s                   = getMessageInAllVersions(messages, "advl_wms",                   errorInMethod);
        advc_files_s                 = getMessageInAllVersions(messages, "advc_files",                 errorInMethod);
        advl_files_s                 = getMessageInAllVersions(messages, "advl_files",                 errorInMethod);
        advc_fgdc_s                  = getMessageInAllVersions(messages, "advc_fgdc",                  errorInMethod);
        advl_fgdc_s                  = getMessageInAllVersions(messages, "advl_fgdc",                  errorInMethod);
        advc_iso19115_s              = getMessageInAllVersions(messages, "advc_iso19115",              errorInMethod);
        advl_iso19115_s              = getMessageInAllVersions(messages, "advl_iso19115",              errorInMethod);
        advc_metadata_s              = getMessageInAllVersions(messages, "advc_metadata",              errorInMethod);
        advl_metadata_s              = getMessageInAllVersions(messages, "advl_metadata",              errorInMethod);
        advl_sourceUrl_s             = getMessageInAllVersions(messages, "advl_sourceUrl",             errorInMethod);
        advl_infoUrl_s               = getMessageInAllVersions(messages, "advl_infoUrl",               errorInMethod);
        advl_rss_s                   = getMessageInAllVersions(messages, "advl_rss",                   errorInMethod);
        advc_email_s                 = getMessageInAllVersions(messages, "advc_email",                 errorInMethod);
        advl_email_s                 = getMessageInAllVersions(messages, "advl_email",                 errorInMethod);
        advl_summary_s               = getMessageInAllVersions(messages, "advl_summary",               errorInMethod);
        advc_testOutOfDate_s         = getMessageInAllVersions(messages, "advc_testOutOfDate",         errorInMethod);
        advl_testOutOfDate_s         = getMessageInAllVersions(messages, "advl_testOutOfDate",         errorInMethod);
        advc_outOfDate_s             = getMessageInAllVersions(messages, "advc_outOfDate",             errorInMethod);
        advl_outOfDate_s             = getMessageInAllVersions(messages, "advl_outOfDate",             errorInMethod);
        advn_outOfDate_s             = getMessageInAllVersions(messages, "advn_outOfDate",             errorInMethod);
        advancedSearch_s             = getMessageInAllVersions(messages, "advancedSearch",             errorInMethod);
        advancedSearchResults_s      = getMessageInAllVersions(messages, "advancedSearchResults",      errorInMethod);
        advancedSearchDirections_s   = getMessageInAllVersions(messages, "advancedSearchDirections",   errorInMethod);
        advancedSearchTooltip_s      = getMessageInAllVersions(messages, "advancedSearchTooltip",      errorInMethod);
        advancedSearchBounds_s       = getMessageInAllVersions(messages, "advancedSearchBounds",       errorInMethod);
        advancedSearchMinLat_s       = getMessageInAllVersions(messages, "advancedSearchMinLat",       errorInMethod);
        advancedSearchMaxLat_s       = getMessageInAllVersions(messages, "advancedSearchMaxLat",       errorInMethod);
        advancedSearchMinLon_s       = getMessageInAllVersions(messages, "advancedSearchMinLon",       errorInMethod);
        advancedSearchMaxLon_s       = getMessageInAllVersions(messages, "advancedSearchMaxLon",       errorInMethod);
        advancedSearchMinMaxLon_s    = getMessageInAllVersions(messages, "advancedSearchMinMaxLon",    errorInMethod);
        advancedSearchMinTime_s      = getMessageInAllVersions(messages, "advancedSearchMinTime",      errorInMethod);
        advancedSearchMaxTime_s      = getMessageInAllVersions(messages, "advancedSearchMaxTime",      errorInMethod);
        advancedSearchClear_s        = getMessageInAllVersions(messages, "advancedSearchClear",        errorInMethod);
        advancedSearchClearHelp_s    = getMessageInAllVersions(messages, "advancedSearchClearHelp",    errorInMethod);
        advancedSearchCategoryTooltip_s=getMessageInAllVersions(messages, "advancedSearchCategoryTooltip", errorInMethod);
        advancedSearchRangeTooltip_s = getMessageInAllVersions(messages, "advancedSearchRangeTooltip", errorInMethod);
        advancedSearchMapTooltip_s   = getMessageInAllVersions(messages, "advancedSearchMapTooltip",   errorInMethod);
        advancedSearchLonTooltip_s   = getMessageInAllVersions(messages, "advancedSearchLonTooltip",   errorInMethod);
        advancedSearchTimeTooltip_s  = getMessageInAllVersions(messages, "advancedSearchTimeTooltip",  errorInMethod);
        advancedSearchWithCriteria_s = getMessageInAllVersions(messages, "advancedSearchWithCriteria", errorInMethod);
        advancedSearchFewerCriteria_s= getMessageInAllVersions(messages, "advancedSearchFewerCriteria",errorInMethod);
        advancedSearchNoCriteria_s   = getMessageInAllVersions(messages, "advancedSearchNoCriteria",   errorInMethod);
        advancedSearchErrorHandling_s= getMessageInAllVersions(messages, "advancedSearchErrorHandling",errorInMethod);
        autoRefresh_s                = getMessageInAllVersions(messages, "autoRefresh",                errorInMethod);
        blacklistMsg               = messages0.getNotNothingString("blacklistMsg",               errorInMethod);
        //care: currently these strings from other classes would remain as String, rather than String[].
        PrimitiveArray.ArrayAddN           = messages0.getNotNothingString("ArrayAddN",          errorInMethod);
        PrimitiveArray.ArrayAppendTables   = messages0.getNotNothingString("ArrayAppendTables",  errorInMethod);
        PrimitiveArray.ArrayAtInsert       = messages0.getNotNothingString("ArrayAtInsert",      errorInMethod);
        PrimitiveArray.ArrayDiff           = messages0.getNotNothingString("ArrayDiff",          errorInMethod);
        PrimitiveArray.ArrayDifferentSize  = messages0.getNotNothingString("ArrayDifferentSize", errorInMethod);
        PrimitiveArray.ArrayDifferentValue = messages0.getNotNothingString("ArrayDifferentValue",errorInMethod);
        PrimitiveArray.ArrayDiffString     = messages0.getNotNothingString("ArrayDiffString",    errorInMethod);
        PrimitiveArray.ArrayMissingValue   = messages0.getNotNothingString("ArrayMissingValue",  errorInMethod);
        PrimitiveArray.ArrayNotAscending   = messages0.getNotNothingString("ArrayNotAscending",  errorInMethod);
        PrimitiveArray.ArrayNotDescending  = messages0.getNotNothingString("ArrayNotDescending", errorInMethod);
        PrimitiveArray.ArrayNotEvenlySpaced= messages0.getNotNothingString("ArrayNotEvenlySpaced",errorInMethod);
        PrimitiveArray.ArrayRemove         = messages0.getNotNothingString("ArrayRemove",        errorInMethod);
        PrimitiveArray.ArraySubsetStart    = messages0.getNotNothingString("ArraySubsetStart",   errorInMethod);
        PrimitiveArray.ArraySubsetStride   = messages0.getNotNothingString("ArraySubsetStride",  errorInMethod);
        categoryTitleHtml_s          = getMessageInAllVersions(messages, "categoryTitleHtml",          errorInMethod);
        categoryHtml_s               = getMessageInAllVersions(messages, "categoryHtml",               errorInMethod);
        category3Html_s              = getMessageInAllVersions(messages, "category3Html",              errorInMethod);
        categoryPickAttribute_s      = getMessageInAllVersions(messages, "categoryPickAttribute",      errorInMethod);
        categorySearchHtml_s         = getMessageInAllVersions(messages, "categorySearchHtml",         errorInMethod);
        categorySearchDifferentHtml_s= getMessageInAllVersions(messages, "categorySearchDifferentHtml",errorInMethod);
        categoryClickHtml_s          = getMessageInAllVersions(messages, "categoryClickHtml",          errorInMethod);
        categoryNotAnOption_s        = getMessageInAllVersions(messages, "categoryNotAnOption",        errorInMethod);
        caughtInterrupted_s          = getMessageInAllVersions(messages, "caughtInterrupted", " ", "", errorInMethod);
        cdmDataTypeHelp_s            = getMessageInAllVersions(messages, "cdmDataTypeHelp",            errorInMethod);
        clickAccess_s                = getMessageInAllVersions(messages, "clickAccess",                errorInMethod);
        clickBackgroundInfo_s        = getMessageInAllVersions(messages, "clickBackgroundInfo",        errorInMethod);
        clickERDDAP_s                = getMessageInAllVersions(messages, "clickERDDAP",                errorInMethod);
        clickInfo_s                  = getMessageInAllVersions(messages, "clickInfo",                  errorInMethod);
        clickToSubmit_s              = getMessageInAllVersions(messages, "clickToSubmit",              errorInMethod);
        HtmlWidgets.comboBoxAlt    = messages0.getNotNothingString("comboBoxAlt",                errorInMethod);
        converterWebService_s        = getMessageInAllVersions(messages, "converterWebService",        errorInMethod);
        convertOceanicAtmosphericAcronyms_s             = getMessageInAllVersions(messages, "convertOceanicAtmosphericAcronyms",             errorInMethod);
        convertOceanicAtmosphericAcronymsIntro_s        = getMessageInAllVersions(messages, "convertOceanicAtmosphericAcronymsIntro",        errorInMethod);
        convertOceanicAtmosphericAcronymsNotes_s        = getMessageInAllVersions(messages, "convertOceanicAtmosphericAcronymsNotes",        errorInMethod);
        convertOceanicAtmosphericAcronymsService_s      = getMessageInAllVersions(messages, "convertOceanicAtmosphericAcronymsService",      errorInMethod);
        convertOceanicAtmosphericVariableNames_s        = getMessageInAllVersions(messages, "convertOceanicAtmosphericVariableNames",        errorInMethod);
        convertOceanicAtmosphericVariableNamesIntro_s   = getMessageInAllVersions(messages, "convertOceanicAtmosphericVariableNamesIntro",   errorInMethod);
        convertOceanicAtmosphericVariableNamesNotes_s   = getMessageInAllVersions(messages, "convertOceanicAtmosphericVariableNamesNotes",   errorInMethod);
        convertOceanicAtmosphericVariableNamesService_s = getMessageInAllVersions(messages, "convertOceanicAtmosphericVariableNamesService", errorInMethod);
        convertFipsCounty_s          = getMessageInAllVersions(messages, "convertFipsCounty",          errorInMethod);
        convertFipsCountyIntro_s     = getMessageInAllVersions(messages, "convertFipsCountyIntro",     errorInMethod);
        convertFipsCountyNotes_s     = getMessageInAllVersions(messages, "convertFipsCountyNotes",     errorInMethod);
        convertFipsCountyService_s   = getMessageInAllVersions(messages, "convertFipsCountyService",   errorInMethod);
        convertHtml_s                = getMessageInAllVersions(messages, "convertHtml",                errorInMethod);
        convertInterpolate_s         = getMessageInAllVersions(messages, "convertInterpolate",         errorInMethod);
        convertInterpolateIntro_s    = getMessageInAllVersions(messages, "convertInterpolateIntro",    errorInMethod);
        convertInterpolateTLLTable_s = getMessageInAllVersions(messages, "convertInterpolateTLLTable", errorInMethod);
        convertInterpolateTLLTableHelp_s
                                   = getMessageInAllVersions(messages, "convertInterpolateTLLTableHelp",              errorInMethod);
        convertInterpolateDatasetIDVariable_s 
                                   = getMessageInAllVersions(messages, "convertInterpolateDatasetIDVariable",     errorInMethod);
        convertInterpolateDatasetIDVariableHelp_s
                                   = getMessageInAllVersions(messages, "convertInterpolateDatasetIDVariableHelp", errorInMethod);
        convertInterpolateNotes_s    = getMessageInAllVersions(messages, "convertInterpolateNotes",    errorInMethod);
        convertInterpolateService_s  = getMessageInAllVersions(messages, "convertInterpolateService",  errorInMethod);
        convertKeywords_s            = getMessageInAllVersions(messages, "convertKeywords",            errorInMethod);
        convertKeywordsCfTooltip_s   = getMessageInAllVersions(messages, "convertKeywordsCfTooltip",   errorInMethod);
        convertKeywordsGcmdTooltip_s = getMessageInAllVersions(messages, "convertKeywordsGcmdTooltip", errorInMethod);
        convertKeywordsIntro_s       = getMessageInAllVersions(messages, "convertKeywordsIntro",       errorInMethod);
        convertKeywordsNotes_s       = getMessageInAllVersions(messages, "convertKeywordsNotes",       errorInMethod);
        convertKeywordsService_s     = getMessageInAllVersions(messages, "convertKeywordsService",     errorInMethod);
        convertTime_s                = getMessageInAllVersions(messages, "convertTime",                errorInMethod);
        convertTimeBypass_s          = getMessageInAllVersions(messages, "convertTimeBypass",          errorInMethod);
        convertTimeReference_s       = getMessageInAllVersions(messages, "convertTimeReference",       errorInMethod);
        convertTimeIntro_s           = getMessageInAllVersions(messages, "convertTimeIntro",           errorInMethod);
        convertTimeNotes_s           = getMessageInAllVersions(messages, "convertTimeNotes",           errorInMethod);
        convertTimeService_s         = getMessageInAllVersions(messages, "convertTimeService",         errorInMethod);
        convertTimeNumberTooltip_s   = getMessageInAllVersions(messages, "convertTimeNumberTooltip",   errorInMethod);
        convertTimeStringTimeTooltip_s=getMessageInAllVersions(messages, "convertTimeStringTimeTooltip",errorInMethod);
        convertTimeUnitsTooltip_s    = getMessageInAllVersions(messages, "convertTimeUnitsTooltip",    errorInMethod);
        convertTimeUnitsHelp_s       = getMessageInAllVersions(messages, "convertTimeUnitsHelp",       errorInMethod);
        convertTimeIsoFormatError_s  = getMessageInAllVersions(messages, "convertTimeIsoFormatError",  errorInMethod);
        convertTimeNoSinceError_s    = getMessageInAllVersions(messages, "convertTimeNoSinceError",    errorInMethod);
        convertTimeNumberError_s     = getMessageInAllVersions(messages, "convertTimeNumberError",     errorInMethod);
        convertTimeNumericTimeError_s= getMessageInAllVersions(messages, "convertTimeNumericTimeError",errorInMethod);
        convertTimeParametersError_s = getMessageInAllVersions(messages, "convertTimeParametersError", errorInMethod);
        convertTimeStringFormatError_s=getMessageInAllVersions(messages, "convertTimeStringFormatError",errorInMethod);
        convertTimeTwoTimeError_s    = getMessageInAllVersions(messages, "convertTimeTwoTimeError",    errorInMethod);
        convertTimeUnitsError_s      = getMessageInAllVersions(messages, "convertTimeUnitsError",      errorInMethod);
        convertUnits_s               = getMessageInAllVersions(messages, "convertUnits",               errorInMethod);
        convertUnitsComparison_s     = getMessageInAllVersions(messages, "convertUnitsComparison",     errorInMethod);
        convertUnitsFilter_s         = getMessageInAllVersions(messages, "convertUnitsFilter",         errorInMethod);
        convertUnitsIntro_s          = getMessageInAllVersions(messages, "convertUnitsIntro",          errorInMethod);
        convertUnitsNotes_s          = getMessageInAllVersions(messages, "convertUnitsNotes",          errorInMethod);
        convertUnitsService_s        = getMessageInAllVersions(messages, "convertUnitsService",        errorInMethod);
        convertURLs_s                = getMessageInAllVersions(messages, "convertURLs",                errorInMethod);
        convertURLsIntro_s           = getMessageInAllVersions(messages, "convertURLsIntro",           errorInMethod);
        convertURLsNotes_s           = getMessageInAllVersions(messages, "convertURLsNotes",           errorInMethod);
        convertURLsService_s         = getMessageInAllVersions(messages, "convertURLsService",         errorInMethod);
        cookiesHelp_s                = getMessageInAllVersions(messages, "cookiesHelp",                errorInMethod);
        daf_s                        = getMessageInAllVersions(messages, "daf",                        errorInMethod);
        dafGridBypassTooltip_s       = getMessageInAllVersions(messages, "dafGridBypassTooltip",       errorInMethod);
        dafGridTooltip_s             = getMessageInAllVersions(messages, "dafGridTooltip",             errorInMethod);
        dafTableBypassTooltip_s      = getMessageInAllVersions(messages, "dafTableBypassTooltip",      errorInMethod);
        dafTableTooltip_s            = getMessageInAllVersions(messages, "dafTableTooltip",            errorInMethod);
        dasTitle_s                   = getMessageInAllVersions(messages, "dasTitle",                   errorInMethod);
        dataAccessNotAllowed_s       = getMessageInAllVersions(messages, "dataAccessNotAllowed",       errorInMethod);
        databaseUnableToConnect_s    = getMessageInAllVersions(messages, "databaseUnableToConnect",    errorInMethod);
        dataProviderFormSuccess_s    = getMessageInAllVersions(messages, "dataProviderFormSuccess",    errorInMethod);
        dataProviderFormShortDescription_s = getMessageInAllVersions(messages, "dataProviderFormShortDescription", errorInMethod);
        dataProviderFormLongDescriptionHTML_s = getMessageInAllVersions(messages, "dataProviderFormLongDescriptionHTML", errorInMethod);
        disabled_s                   = getMessageInAllVersions(messages, "disabled",                   errorInMethod);
        dataProviderFormPart1_s      = getMessageInAllVersions(messages, "dataProviderFormPart1",      errorInMethod);
        dataProviderFormPart2Header_s = getMessageInAllVersions(messages, "dataProviderFormPart2Header", errorInMethod);
        dataProviderFormPart2GlobalMetadata_s = getMessageInAllVersions(messages, "dataProviderFormPart2GlobalMetadata", errorInMethod);
        dataProviderContactInfo_s    = getMessageInAllVersions(messages, "dataProviderContactInfo",    errorInMethod);
        dataProviderData_s           = getMessageInAllVersions(messages, "dataProviderData",           errorInMethod);

        dpf_submit_s                 = getMessageInAllVersions(messages, "dpf_submit",                 errorInMethod);
        dpf_fixProblem_s             = getMessageInAllVersions(messages, "dpf_fixProblem",             errorInMethod);
        dpf_yourName_s               = getMessageInAllVersions(messages, "dpf_yourName",               errorInMethod);
        dpf_emailAddress_s           = getMessageInAllVersions(messages, "dpf_emailAddress",           errorInMethod);
        dpf_Timestamp_s              = getMessageInAllVersions(messages, "dpf_Timestamp",              errorInMethod);
        dpf_frequency_s              = getMessageInAllVersions(messages, "dpf_frequency",              errorInMethod);
        dpf_title_s                  = getMessageInAllVersions(messages, "dpf_title",                  errorInMethod);
        dpf_titleTooltip_s           = getMessageInAllVersions(messages, "dpf_titleTooltip",           errorInMethod);
        dpf_summary_s                = getMessageInAllVersions(messages, "dpf_summary",                errorInMethod);
        dpf_summaryTooltip_s         = getMessageInAllVersions(messages, "dpf_summaryTooltip",         errorInMethod);
        dpf_creatorName_s            = getMessageInAllVersions(messages, "dpf_creatorName",            errorInMethod);
        dpf_creatorNameTooltip_s     = getMessageInAllVersions(messages, "dpf_creatorNameTooltip",     errorInMethod);
        dpf_creatorType_s            = getMessageInAllVersions(messages, "dpf_creatorType",            errorInMethod);
        dpf_creatorTypeTooltip_s     = getMessageInAllVersions(messages, "dpf_creatorTypeTooltip",     errorInMethod);
        dpf_creatorEmail_s           = getMessageInAllVersions(messages, "dpf_creatorEmail",           errorInMethod);
        dpf_creatorEmailTooltip_s    = getMessageInAllVersions(messages, "dpf_creatorEmailTooltip",    errorInMethod);
        dpf_institution_s            = getMessageInAllVersions(messages, "dpf_institution",            errorInMethod);
        dpf_institutionTooltip_s     = getMessageInAllVersions(messages, "dpf_institutionTooltip",     errorInMethod);
        dpf_infoUrl_s                = getMessageInAllVersions(messages, "dpf_infoUrl",                errorInMethod);
        dpf_infoUrlTooltip_s         = getMessageInAllVersions(messages, "dpf_infoUrlTooltip",         errorInMethod);
        dpf_license_s                = getMessageInAllVersions(messages, "dpf_license",                errorInMethod);
        dpf_licenseTooltip_s         = getMessageInAllVersions(messages, "dpf_licenseTooltip",         errorInMethod);
        dpf_howYouStoreData_s        = getMessageInAllVersions(messages, "dpf_howYouStoreData",        errorInMethod);
        dpf_required_s               = getMessageInAllVersions(messages, "dpf_required",               errorInMethod);
        dpf_optional_s               = getMessageInAllVersions(messages, "dpf_optional",               errorInMethod);
        dpf_provideIfAvaliable_s     = getMessageInAllVersions(messages, "dpf_provideIfAvaliable",     errorInMethod);
        dpf_acknowledgement_s        = getMessageInAllVersions(messages, "dpf_acknowledgement",        errorInMethod);
        dpf_acknowledgementTooltip_s = getMessageInAllVersions(messages, "dpf_acknowledgementTooltip", errorInMethod);
        dpf_history_s                = getMessageInAllVersions(messages, "dpf_history",                errorInMethod);
        dpf_historyTooltip_s         = getMessageInAllVersions(messages, "dpf_historyTooltip",         errorInMethod);
        dpf_idTooltip_s              = getMessageInAllVersions(messages, "dpf_idTooltip",              errorInMethod);
        dpf_namingAuthority_s        = getMessageInAllVersions(messages, "dpf_namingAuthority",        errorInMethod);
        dpf_namingAuthorityTooltip_s = getMessageInAllVersions(messages, "dpf_namingAuthorityTooltip", errorInMethod);
        dpf_productVersion_s         = getMessageInAllVersions(messages, "dpf_productVersion",         errorInMethod);
        dpf_productVersionTooltip_s  = getMessageInAllVersions(messages, "dpf_productVersionTooltip",  errorInMethod);
        dpf_references_s             = getMessageInAllVersions(messages, "dpf_references",             errorInMethod);
        dpf_referencesTooltip_s      = getMessageInAllVersions(messages, "dpf_referencesTooltip",      errorInMethod);
        dpf_comment_s                = getMessageInAllVersions(messages, "dpf_comment",                errorInMethod);
        dpf_commentTooltip_s         = getMessageInAllVersions(messages, "dpf_commentTooltip",         errorInMethod);
        dpf_dataTypeHelp_s           = getMessageInAllVersions(messages, "dpf_dataTypeHelp",           errorInMethod);
        dpf_ioosCategory_s           = getMessageInAllVersions(messages, "dpf_ioosCategory",           errorInMethod);
        dpf_ioosCategoryHelp_s       = getMessageInAllVersions(messages, "dpf_ioosCategoryHelp",       errorInMethod);
        dpf_part3Header_s            = getMessageInAllVersions(messages, "dpf_part3Header",            errorInMethod);
        dpf_variableMetadata_s       = getMessageInAllVersions(messages, "dpf_variableMetadata",       errorInMethod);
        dpf_sourceName_s             = getMessageInAllVersions(messages, "dpf_sourceName",             errorInMethod);
        dpf_sourceNameTooltip_s      = getMessageInAllVersions(messages, "dpf_sourceNameTooltip",      errorInMethod);
        dpf_destinationName_s        = getMessageInAllVersions(messages, "dpf_destinationName",        errorInMethod);
        dpf_destinationNameTooltip_s = getMessageInAllVersions(messages, "dpf_destinationNameTooltip", errorInMethod);
        dpf_longName_s               = getMessageInAllVersions(messages, "dpf_longName",               errorInMethod);
        dpf_longNameTooltip_s        = getMessageInAllVersions(messages, "dpf_longNameTooltip",        errorInMethod);
        dpf_standardName_s           = getMessageInAllVersions(messages, "dpf_standardName",           errorInMethod);
        dpf_standardNameTooltip_s    = getMessageInAllVersions(messages, "dpf_standardNameTooltip",    errorInMethod);
        dpf_dataType_s               = getMessageInAllVersions(messages, "dpf_dataType",               errorInMethod);
        dpf_fillValue_s              = getMessageInAllVersions(messages, "dpf_fillValue",              errorInMethod);
        dpf_fillValueTooltip_s       = getMessageInAllVersions(messages, "dpf_fillValueTooltip",       errorInMethod);
        dpf_units_s                  = getMessageInAllVersions(messages, "dpf_units",                  errorInMethod);
        dpf_unitsTooltip_s           = getMessageInAllVersions(messages, "dpf_unitsTooltip",           errorInMethod);
        dpf_range_s                  = getMessageInAllVersions(messages, "dpf_range",                  errorInMethod);
        dpf_rangeTooltip_s           = getMessageInAllVersions(messages, "dpf_rangeTooltip",           errorInMethod);
        dpf_part4Header_s            = getMessageInAllVersions(messages, "dpf_part4Header",            errorInMethod);
        dpf_otherComment_s           = getMessageInAllVersions(messages, "dpf_otherComment",           errorInMethod);
        dpf_finishPart4_s            = getMessageInAllVersions(messages, "dpf_finishPart4",            errorInMethod);
        dpf_congratulation_s         = getMessageInAllVersions(messages, "dpf_congratulation",         errorInMethod);


        distinctValuesTooltip_s      = getMessageInAllVersions(messages, "distinctValuesTooltip",      errorInMethod);
        doWithGraphs_s               = getMessageInAllVersions(messages, "doWithGraphs",               errorInMethod);

        dtAccessible_s               = getMessageInAllVersions(messages, "dtAccessible",               errorInMethod);
        dtAccessibleYes_s            = getMessageInAllVersions(messages, "dtAccessibleYes",            errorInMethod);
        dtAccessibleGraphs_s         = getMessageInAllVersions(messages, "dtAccessibleGraphs",         errorInMethod);
        dtAccessibleNo_s             = getMessageInAllVersions(messages, "dtAccessibleNo",             errorInMethod);
        dtAccessibleLogIn_s          = getMessageInAllVersions(messages, "dtAccessibleLogIn",          errorInMethod);
        dtLogIn_s                    = getMessageInAllVersions(messages, "dtLogIn",                    errorInMethod);
        dtDAF_s                      = getMessageInAllVersions(messages, "dtDAF",                      errorInMethod);
        dtFiles_s                    = getMessageInAllVersions(messages, "dtFiles",                    errorInMethod);
        dtMAG_s                      = getMessageInAllVersions(messages, "dtMAG",                      errorInMethod);
        dtSOS_s                      = getMessageInAllVersions(messages, "dtSOS",                      errorInMethod);
        dtSubset_s                   = getMessageInAllVersions(messages, "dtSubset",                   errorInMethod);
        dtWCS_s                      = getMessageInAllVersions(messages, "dtWCS",                      errorInMethod);
        dtWMS_s                      = getMessageInAllVersions(messages, "dtWMS",                      errorInMethod);
        
        EDDDatasetID_s               = getMessageInAllVersions(messages, "EDDDatasetID",               errorInMethod);
        EDDFgdc_s                    = getMessageInAllVersions(messages, "EDDFgdc",                    errorInMethod);
        EDDFgdcMetadata_s            = getMessageInAllVersions(messages, "EDDFgdcMetadata",            errorInMethod);
        EDDFiles_s                   = getMessageInAllVersions(messages, "EDDFiles",                   errorInMethod);
        EDDIso19115                = messages0.getNotNothingString("EDDIso19115",                errorInMethod);
        EDDIso19115Metadata_s        = getMessageInAllVersions(messages, "EDDIso19115Metadata",        errorInMethod);
        EDDMetadata_s                = getMessageInAllVersions(messages, "EDDMetadata",                errorInMethod);
        EDDBackground_s              = getMessageInAllVersions(messages, "EDDBackground",              errorInMethod);
        EDDClickOnSubmitHtml_s       = getMessageInAllVersions(messages, "EDDClickOnSubmitHtml",       errorInMethod);
        EDDInformation_s             = getMessageInAllVersions(messages, "EDDInformation",             errorInMethod);
        EDDInstitution_s             = getMessageInAllVersions(messages, "EDDInstitution",             errorInMethod);
        EDDSummary_s                 = getMessageInAllVersions(messages, "EDDSummary",                 errorInMethod);
        EDDDatasetTitle_s            = getMessageInAllVersions(messages, "EDDDatasetTitle",            errorInMethod);
        EDDDownloadData_s            = getMessageInAllVersions(messages, "EDDDownloadData",            errorInMethod);
        EDDMakeAGraph_s              = getMessageInAllVersions(messages, "EDDMakeAGraph",              errorInMethod);
        EDDMakeAMap_s                = getMessageInAllVersions(messages, "EDDMakeAMap",                errorInMethod);
        EDDFileType_s                = getMessageInAllVersions(messages, "EDDFileType",                errorInMethod);
        EDDFileTypeInformation_s     = getMessageInAllVersions(messages, "EDDFileTypeInformation",     errorInMethod);
        EDDSelectFileType_s          = getMessageInAllVersions(messages, "EDDSelectFileType",          errorInMethod);
        EDDMinimum_s                 = getMessageInAllVersions(messages, "EDDMinimum",                 errorInMethod);
        EDDMaximum_s                 = getMessageInAllVersions(messages, "EDDMaximum",                 errorInMethod);
        EDDConstraint_s              = getMessageInAllVersions(messages, "EDDConstraint",              errorInMethod);

        EDDChangedWasnt_s            = getMessageInAllVersions(messages, "EDDChangedWasnt",            errorInMethod);
        EDDChangedDifferentNVar_s    = getMessageInAllVersions(messages, "EDDChangedDifferentNVar",    errorInMethod);
        EDDChanged2Different_s       = getMessageInAllVersions(messages, "EDDChanged2Different",       errorInMethod);
        EDDChanged1Different_s       = getMessageInAllVersions(messages, "EDDChanged1Different",       errorInMethod);
        EDDChangedCGADifferent_s     = getMessageInAllVersions(messages, "EDDChangedCGADifferent",     errorInMethod);
        EDDChangedAxesDifferentNVar_s= getMessageInAllVersions(messages, "EDDChangedAxesDifferentNVar",errorInMethod);
        EDDChangedAxes2Different_s   = getMessageInAllVersions(messages, "EDDChangedAxes2Different",   errorInMethod);
        EDDChangedAxes1Different_s   = getMessageInAllVersions(messages, "EDDChangedAxes1Different",   errorInMethod);
        EDDChangedNoValue_s          = getMessageInAllVersions(messages, "EDDChangedNoValue",          errorInMethod);
        EDDChangedTableToGrid_s      = getMessageInAllVersions(messages, "EDDChangedTableToGrid",      errorInMethod);

        EDDSimilarDifferentNVar_s    = getMessageInAllVersions(messages, "EDDSimilarDifferentNVar",    errorInMethod);
        EDDSimilarDifferent_s        = getMessageInAllVersions(messages, "EDDSimilarDifferent",        errorInMethod);

        EDDGridDownloadTooltip_s     = getMessageInAllVersions(messages, "EDDGridDownloadTooltip",     errorInMethod);
        EDDGridDapDescription_s      = getMessageInAllVersions(messages, "EDDGridDapDescription",      errorInMethod);
        EDDGridDapLongDescription_s  = getMessageInAllVersions(messages, "EDDGridDapLongDescription",  errorInMethod);
        EDDGridDownloadDataTooltip_s = getMessageInAllVersions(messages, "EDDGridDownloadDataTooltip", errorInMethod);
        EDDGridDimension_s           = getMessageInAllVersions(messages, "EDDGridDimension",           errorInMethod);
        EDDGridDimensionRanges_s     = getMessageInAllVersions(messages, "EDDGridDimensionRanges",     errorInMethod);
        EDDGridFirst_s               = getMessageInAllVersions(messages, "EDDGridFirst",               errorInMethod);
        EDDGridLast_s                = getMessageInAllVersions(messages, "EDDGridLast",                errorInMethod);
        EDDGridStart_s               = getMessageInAllVersions(messages, "EDDGridStart",               errorInMethod);
        EDDGridStop_s                = getMessageInAllVersions(messages, "EDDGridStop",                errorInMethod);
        EDDGridStartStopTooltip_s    = getMessageInAllVersions(messages, "EDDGridStartStopTooltip",    errorInMethod);
        EDDGridStride_s              = getMessageInAllVersions(messages, "EDDGridStride",              errorInMethod);
        EDDGridNValues_s             = getMessageInAllVersions(messages, "EDDGridNValues",             errorInMethod);
        EDDGridNValuesHtml_s         = getMessageInAllVersions(messages, "EDDGridNValuesHtml",         errorInMethod);
        EDDGridSpacing_s             = getMessageInAllVersions(messages, "EDDGridSpacing",             errorInMethod);
        EDDGridJustOneValue_s        = getMessageInAllVersions(messages, "EDDGridJustOneValue",        errorInMethod);
        EDDGridEven_s                = getMessageInAllVersions(messages, "EDDGridEven",                errorInMethod);
        EDDGridUneven_s              = getMessageInAllVersions(messages, "EDDGridUneven",              errorInMethod);
        EDDGridDimensionTooltip_s    = getMessageInAllVersions(messages, "EDDGridDimensionTooltip",    errorInMethod);
        EDDGridDimensionFirstTooltip_s=getMessageInAllVersions(messages, "EDDGridDimensionFirstTooltip", errorInMethod);
        EDDGridDimensionLastTooltip_s =getMessageInAllVersions(messages, "EDDGridDimensionLastTooltip",  errorInMethod);
        EDDGridVarHasDimTooltip_s    = getMessageInAllVersions(messages, "EDDGridVarHasDimTooltip",    errorInMethod);
        EDDGridSSSTooltip_s          = getMessageInAllVersions(messages, "EDDGridSSSTooltip",          errorInMethod);
        EDDGridStartTooltip_s        = getMessageInAllVersions(messages, "EDDGridStartTooltip",        errorInMethod);
        EDDGridStopTooltip_s         = getMessageInAllVersions(messages, "EDDGridStopTooltip",         errorInMethod);
        EDDGridStrideTooltip_s       = getMessageInAllVersions(messages, "EDDGridStrideTooltip",       errorInMethod);
        EDDGridSpacingTooltip_s      = getMessageInAllVersions(messages, "EDDGridSpacingTooltip",      errorInMethod);
        EDDGridGridVariableHtml_s    = getMessageInAllVersions(messages, "EDDGridGridVariableHtml",    errorInMethod);

        //default EDDGrid...Example
        EDDGridErddapUrlExample    = messages0.getNotNothingString("EDDGridErddapUrlExample",    errorInMethod);
        EDDGridIdExample           = messages0.getNotNothingString("EDDGridIdExample",           errorInMethod);
        EDDGridDimensionExample    = messages0.getNotNothingString("EDDGridDimensionExample",    errorInMethod);
        EDDGridNoHyperExample      = messages0.getNotNothingString("EDDGridNoHyperExample",      errorInMethod);
        EDDGridDimNamesExample     = messages0.getNotNothingString("EDDGridDimNamesExample",     errorInMethod);
        EDDGridDataTimeExample     = messages0.getNotNothingString("EDDGridDataTimeExample",     errorInMethod);
        EDDGridDataValueExample    = messages0.getNotNothingString("EDDGridDataValueExample",    errorInMethod);
        EDDGridDataIndexExample    = messages0.getNotNothingString("EDDGridDataIndexExample",    errorInMethod);
        EDDGridGraphExample        = messages0.getNotNothingString("EDDGridGraphExample",        errorInMethod);
        EDDGridMapExample          = messages0.getNotNothingString("EDDGridMapExample",          errorInMethod);
        EDDGridMatlabPlotExample   = messages0.getNotNothingString("EDDGridMatlabPlotExample",   errorInMethod);

        //admin provides EDDGrid...Example
        EDDGridErddapUrlExample    = getSetupEVString(setup, ev, "EDDGridErddapUrlExample",    EDDGridErddapUrlExample);
        EDDGridIdExample           = getSetupEVString(setup, ev, "EDDGridIdExample",           EDDGridIdExample);
        EDDGridDimensionExample    = getSetupEVString(setup, ev, "EDDGridDimensionExample",    EDDGridDimensionExample);
        EDDGridNoHyperExample      = getSetupEVString(setup, ev, "EDDGridNoHyperExample",      EDDGridNoHyperExample);
        EDDGridDimNamesExample     = getSetupEVString(setup, ev, "EDDGridDimNamesExample",     EDDGridDimNamesExample);
        EDDGridDataIndexExample    = getSetupEVString(setup, ev, "EDDGridDataIndexExample",    EDDGridDataIndexExample);
        EDDGridDataValueExample    = getSetupEVString(setup, ev, "EDDGridDataValueExample",    EDDGridDataValueExample);
        EDDGridDataTimeExample     = getSetupEVString(setup, ev, "EDDGridDataTimeExample",     EDDGridDataTimeExample);
        EDDGridGraphExample        = getSetupEVString(setup, ev, "EDDGridGraphExample",        EDDGridGraphExample);
        EDDGridMapExample          = getSetupEVString(setup, ev, "EDDGridMapExample",          EDDGridMapExample);
        EDDGridMatlabPlotExample   = getSetupEVString(setup, ev, "EDDGridMatlabPlotExample",   EDDGridMatlabPlotExample);

        //variants encoded to be Html Examples
        EDDGridDimensionExampleHE  = XML.encodeAsHTML(EDDGridDimensionExample);
        EDDGridDataIndexExampleHE  = XML.encodeAsHTML(EDDGridDataIndexExample);
        EDDGridDataValueExampleHE  = XML.encodeAsHTML(EDDGridDataValueExample);
        EDDGridDataTimeExampleHE   = XML.encodeAsHTML(EDDGridDataTimeExample);
        EDDGridGraphExampleHE      = XML.encodeAsHTML(EDDGridGraphExample);
        EDDGridMapExampleHE        = XML.encodeAsHTML(EDDGridMapExample);

        //variants encoded to be Html Attributes
        EDDGridDimensionExampleHA  = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDimensionExample));
        EDDGridDataIndexExampleHA  = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataIndexExample));
        EDDGridDataValueExampleHA  = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataValueExample));
        EDDGridDataTimeExampleHA   = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataTimeExample));
        EDDGridGraphExampleHA      = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridGraphExample));
        EDDGridMapExampleHA        = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridMapExample)); 


        EDDTableConstraints_s             = getMessageInAllVersions(messages, "EDDTableConstraints",        errorInMethod);
        EDDTableDapDescription_s          = getMessageInAllVersions(messages, "EDDTableDapDescription",     errorInMethod);
        EDDTableDapLongDescription_s      = getMessageInAllVersions(messages, "EDDTableDapLongDescription", errorInMethod);
        EDDTableDownloadDataTooltip_s     = getMessageInAllVersions(messages, "EDDTableDownloadDataTooltip",   errorInMethod);
        EDDTableTabularDatasetTooltip_s   = getMessageInAllVersions(messages, "EDDTableTabularDatasetTooltip", errorInMethod);
        EDDTableVariable_s                = getMessageInAllVersions(messages, "EDDTableVariable",           errorInMethod);
        EDDTableCheckAll_s                = getMessageInAllVersions(messages, "EDDTableCheckAll",           errorInMethod);
        EDDTableCheckAllTooltip_s         = getMessageInAllVersions(messages, "EDDTableCheckAllTooltip",    errorInMethod);
        EDDTableUncheckAll_s              = getMessageInAllVersions(messages, "EDDTableUncheckAll",         errorInMethod);
        EDDTableUncheckAllTooltip_s       = getMessageInAllVersions(messages, "EDDTableUncheckAllTooltip",  errorInMethod);
        EDDTableMinimumTooltip_s          = getMessageInAllVersions(messages, "EDDTableMinimumTooltip",     errorInMethod);
        EDDTableMaximumTooltip_s          = getMessageInAllVersions(messages, "EDDTableMaximumTooltip",     errorInMethod);
        EDDTableCheckTheVariables_s       = getMessageInAllVersions(messages, "EDDTableCheckTheVariables",  errorInMethod);
        EDDTableSelectAnOperator_s        = getMessageInAllVersions(messages, "EDDTableSelectAnOperator",   errorInMethod);
        EDDTableFromEDDGridSummary_s      = getMessageInAllVersions(messages, "EDDTableFromEDDGridSummary", errorInMethod);
        EDDTableOptConstraint1Html_s      = getMessageInAllVersions(messages, "EDDTableOptConstraint1Html", errorInMethod);
        EDDTableOptConstraint2Html_s      = getMessageInAllVersions(messages, "EDDTableOptConstraint2Html", errorInMethod);
        EDDTableOptConstraintVar_s        = getMessageInAllVersions(messages, "EDDTableOptConstraintVar",   errorInMethod);
        EDDTableNumericConstraintTooltip_s= getMessageInAllVersions(messages, "EDDTableNumericConstraintTooltip", errorInMethod);
        EDDTableStringConstraintTooltip_s = getMessageInAllVersions(messages, "EDDTableStringConstraintTooltip",  errorInMethod);
        EDDTableTimeConstraintTooltip_s   = getMessageInAllVersions(messages, "EDDTableTimeConstraintTooltip",    errorInMethod);
        EDDTableConstraintTooltip_s       = getMessageInAllVersions(messages, "EDDTableConstraintTooltip",        errorInMethod);
        EDDTableSelectConstraintTooltip_s = getMessageInAllVersions(messages, "EDDTableSelectConstraintTooltip",  errorInMethod);

        //default EDDGrid...Example
        EDDTableErddapUrlExample   = messages0.getNotNothingString("EDDTableErddapUrlExample",   errorInMethod);
        EDDTableIdExample          = messages0.getNotNothingString("EDDTableIdExample",          errorInMethod);
        EDDTableVariablesExample   = messages0.getNotNothingString("EDDTableVariablesExample",   errorInMethod);
        EDDTableConstraintsExample = messages0.getNotNothingString("EDDTableConstraintsExample", errorInMethod);
        EDDTableDataValueExample   = messages0.getNotNothingString("EDDTableDataValueExample",   errorInMethod);
        EDDTableDataTimeExample    = messages0.getNotNothingString("EDDTableDataTimeExample",    errorInMethod);
        EDDTableGraphExample       = messages0.getNotNothingString("EDDTableGraphExample",       errorInMethod);
        EDDTableMapExample         = messages0.getNotNothingString("EDDTableMapExample",         errorInMethod);
        EDDTableMatlabPlotExample  = messages0.getNotNothingString("EDDTableMatlabPlotExample",  errorInMethod);

        EDDTableFromHttpGetDatasetDescription_s   = getMessageInAllVersions(messages, "EDDTableFromHttpGetDatasetDescription",   errorInMethod);
        EDDTableFromHttpGetAuthorDescription_s    = getMessageInAllVersions(messages, "EDDTableFromHttpGetAuthorDescription",    errorInMethod);
        EDDTableFromHttpGetTimestampDescription_s = getMessageInAllVersions(messages, "EDDTableFromHttpGetTimestampDescription", errorInMethod);

        //admin provides EDDGrid...Example
        EDDTableErddapUrlExample   = getSetupEVString(setup, ev, "EDDTableErddapUrlExample",   EDDTableErddapUrlExample);
        EDDTableIdExample          = getSetupEVString(setup, ev, "EDDTableIdExample",          EDDTableIdExample);
        EDDTableVariablesExample   = getSetupEVString(setup, ev, "EDDTableVariablesExample",   EDDTableVariablesExample);
        EDDTableConstraintsExample = getSetupEVString(setup, ev, "EDDTableConstraintsExample", EDDTableConstraintsExample);
        EDDTableDataValueExample   = getSetupEVString(setup, ev, "EDDTableDataValueExample",   EDDTableDataValueExample);
        EDDTableDataTimeExample    = getSetupEVString(setup, ev, "EDDTableDataTimeExample",    EDDTableDataTimeExample);
        EDDTableGraphExample       = getSetupEVString(setup, ev, "EDDTableGraphExample",       EDDTableGraphExample);
        EDDTableMapExample         = getSetupEVString(setup, ev, "EDDTableMapExample",         EDDTableMapExample);
        EDDTableMatlabPlotExample  = getSetupEVString(setup, ev, "EDDTableMatlabPlotExample",  EDDTableMatlabPlotExample);

        //variants encoded to be Html Examples
        EDDTableConstraintsExampleHE = XML.encodeAsHTML(EDDTableConstraintsExample);
        EDDTableDataTimeExampleHE    = XML.encodeAsHTML(EDDTableDataTimeExample);
        EDDTableDataValueExampleHE   = XML.encodeAsHTML(EDDTableDataValueExample);
        EDDTableGraphExampleHE       = XML.encodeAsHTML(EDDTableGraphExample);
        EDDTableMapExampleHE         = XML.encodeAsHTML(EDDTableMapExample);

        //variants encoded to be Html Attributes
        EDDTableConstraintsExampleHA = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableConstraintsExample));
        EDDTableDataTimeExampleHA    = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableDataTimeExample));
        EDDTableDataValueExampleHA   = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableDataValueExample));
        EDDTableGraphExampleHA       = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableGraphExample));
        EDDTableMapExampleHA         = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableMapExample));

        errorTitle_s                 = getMessageInAllVersions(messages, "errorTitle",                 errorInMethod);
        erddapVersionHTML_s          = getMessageInAllVersions(messages, "erddapVersionHTML",          errorInMethod);
        errorRequestUrl_s            = getMessageInAllVersions(messages, "errorRequestUrl",            errorInMethod);
        errorRequestQuery_s          = getMessageInAllVersions(messages, "errorRequestQuery",          errorInMethod);
        errorTheError_s              = getMessageInAllVersions(messages, "errorTheError",              errorInMethod);
        errorCopyFrom_s              = getMessageInAllVersions(messages, "errorCopyFrom",              errorInMethod);
        errorFileNotFound_s          = getMessageInAllVersions(messages, "errorFileNotFound",          errorInMethod);
        errorFileNotFoundImage_s     = getMessageInAllVersions(messages, "errorFileNotFoundImage",     errorInMethod);
        errorInternal_s              = getMessageInAllVersions(messages, "errorInternal", "", " ",     errorInMethod);
        errorJsonpFunctionName_s     = getMessageInAllVersions(messages, "errorJsonpFunctionName",     errorInMethod);
        errorJsonpNotAllowed_s       = getMessageInAllVersions(messages, "errorJsonpNotAllowed",       errorInMethod);
        errorMoreThan2GB_s           = getMessageInAllVersions(messages, "errorMoreThan2GB",           errorInMethod);
        errorNotFound_s              = getMessageInAllVersions(messages, "errorNotFound",              errorInMethod);
        errorNotFoundIn_s            = getMessageInAllVersions(messages, "errorNotFoundIn",            errorInMethod);
        errorOdvLLTGrid_s            = getMessageInAllVersions(messages, "errorOdvLLTGrid",            errorInMethod);
        errorOdvLLTTable_s           = getMessageInAllVersions(messages, "errorOdvLLTTable",           errorInMethod);
        errorOnWebPage_s             = getMessageInAllVersions(messages, "errorOnWebPage",             errorInMethod);
        HtmlWidgets.errorXWasntSpecified = messages0.getNotNothingString("errorXWasntSpecified",       errorInMethod);
        HtmlWidgets.errorXWasTooLong     = messages0.getNotNothingString("errorXWasTooLong",           errorInMethod);
        extensionsNoRangeRequests  = StringArray.arrayFromCSV(
                                     messages0.getNotNothingString("extensionsNoRangeRequests",  errorInMethod),
                                     ",", true, false); //trim, keepNothing
        externalLink_s               = getMessageInAllVersions(messages, "externalLink", " ", "",      errorInMethod);
        externalWebSite_s            = getMessageInAllVersions(messages, "externalWebSite",            errorInMethod);
        fileHelp_asc_s               = getMessageInAllVersions(messages, "fileHelp_asc",               errorInMethod);
        fileHelp_csv_s               = getMessageInAllVersions(messages, "fileHelp_csv",               errorInMethod);
        fileHelp_csvp_s              = getMessageInAllVersions(messages, "fileHelp_csvp",              errorInMethod);
        fileHelp_csv0_s              = getMessageInAllVersions(messages, "fileHelp_csv0",              errorInMethod);
        fileHelp_dataTable_s         = getMessageInAllVersions(messages, "fileHelp_dataTable",         errorInMethod);
        fileHelp_das_s               = getMessageInAllVersions(messages, "fileHelp_das",               errorInMethod);
        fileHelp_dds_s               = getMessageInAllVersions(messages, "fileHelp_dds",               errorInMethod);
        fileHelp_dods_s              = getMessageInAllVersions(messages, "fileHelp_dods",              errorInMethod);
        fileHelpGrid_esriAscii_s     = getMessageInAllVersions(messages, "fileHelpGrid_esriAscii",     errorInMethod);
        fileHelpTable_esriCsv_s      = getMessageInAllVersions(messages, "fileHelpTable_esriCsv",      errorInMethod);
        fileHelp_fgdc_s              = getMessageInAllVersions(messages, "fileHelp_fgdc",              errorInMethod);
        fileHelp_geoJson_s           = getMessageInAllVersions(messages, "fileHelp_geoJson",           errorInMethod);
        fileHelp_graph_s             = getMessageInAllVersions(messages, "fileHelp_graph",             errorInMethod);
        fileHelpGrid_help_s          = getMessageInAllVersions(messages, "fileHelpGrid_help",          errorInMethod);
        fileHelpTable_help_s         = getMessageInAllVersions(messages, "fileHelpTable_help",         errorInMethod);
        fileHelp_html_s              = getMessageInAllVersions(messages, "fileHelp_html",              errorInMethod);
        fileHelp_htmlTable_s         = getMessageInAllVersions(messages, "fileHelp_htmlTable",         errorInMethod);
        fileHelp_iso19115_s          = getMessageInAllVersions(messages, "fileHelp_iso19115",          errorInMethod);
        fileHelp_itxGrid_s           = getMessageInAllVersions(messages, "fileHelp_itxGrid",           errorInMethod);
        fileHelp_itxTable_s          = getMessageInAllVersions(messages, "fileHelp_itxTable",          errorInMethod);
        fileHelp_json_s              = getMessageInAllVersions(messages, "fileHelp_json",              errorInMethod);
        fileHelp_jsonlCSV1_s         = getMessageInAllVersions(messages, "fileHelp_jsonlCSV1",         errorInMethod);
        fileHelp_jsonlCSV_s          = getMessageInAllVersions(messages, "fileHelp_jsonlCSV",          errorInMethod);
        fileHelp_jsonlKVP_s          = getMessageInAllVersions(messages, "fileHelp_jsonlKVP",          errorInMethod);
        fileHelp_mat_s               = getMessageInAllVersions(messages, "fileHelp_mat",               errorInMethod);
        fileHelpGrid_nc3_s           = getMessageInAllVersions(messages, "fileHelpGrid_nc3",           errorInMethod);
        fileHelpGrid_nc4_s           = getMessageInAllVersions(messages, "fileHelpGrid_nc4",           errorInMethod);
        fileHelpTable_nc3_s          = getMessageInAllVersions(messages, "fileHelpTable_nc3",          errorInMethod);
        fileHelpTable_nc4_s          = getMessageInAllVersions(messages, "fileHelpTable_nc4",          errorInMethod);
        fileHelp_nc3Header_s         = getMessageInAllVersions(messages, "fileHelp_nc3Header",         errorInMethod);
        fileHelp_nc4Header_s         = getMessageInAllVersions(messages, "fileHelp_nc4Header",         errorInMethod);
        fileHelp_nccsv_s             = getMessageInAllVersions(messages, "fileHelp_nccsv",             errorInMethod);
        fileHelp_nccsvMetadata_s     = getMessageInAllVersions(messages, "fileHelp_nccsvMetadata",     errorInMethod);
        fileHelp_ncCF_s              = getMessageInAllVersions(messages, "fileHelp_ncCF",              errorInMethod);
        fileHelp_ncCFHeader_s        = getMessageInAllVersions(messages, "fileHelp_ncCFHeader",        errorInMethod);
        fileHelp_ncCFMA_s            = getMessageInAllVersions(messages, "fileHelp_ncCFMA",            errorInMethod);
        fileHelp_ncCFMAHeader_s      = getMessageInAllVersions(messages, "fileHelp_ncCFMAHeader",      errorInMethod);
        fileHelp_ncml_s              = getMessageInAllVersions(messages, "fileHelp_ncml",              errorInMethod);
        fileHelp_ncoJson_s           = getMessageInAllVersions(messages, "fileHelp_ncoJson",           errorInMethod);
        fileHelpGrid_odvTxt_s        = getMessageInAllVersions(messages, "fileHelpGrid_odvTxt",        errorInMethod);
        fileHelpTable_odvTxt_s       = getMessageInAllVersions(messages, "fileHelpTable_odvTxt",       errorInMethod);
        fileHelp_subset_s            = getMessageInAllVersions(messages, "fileHelp_subset",            errorInMethod);
        fileHelp_timeGaps_s          = getMessageInAllVersions(messages, "fileHelp_timeGaps",          errorInMethod);
        fileHelp_tsv_s               = getMessageInAllVersions(messages, "fileHelp_tsv",               errorInMethod);
        fileHelp_tsvp_s              = getMessageInAllVersions(messages, "fileHelp_tsvp",              errorInMethod);
        fileHelp_tsv0_s              = getMessageInAllVersions(messages, "fileHelp_tsv0",              errorInMethod);
        fileHelp_wav_s               = getMessageInAllVersions(messages, "fileHelp_wav",               errorInMethod);
        fileHelp_xhtml_s             = getMessageInAllVersions(messages, "fileHelp_xhtml",             errorInMethod);
        fileHelp_geotif_s            = getMessageInAllVersions(messages, "fileHelp_geotif",            errorInMethod);
        fileHelpGrid_kml_s           = getMessageInAllVersions(messages, "fileHelpGrid_kml",           errorInMethod);
        fileHelpTable_kml_s          = getMessageInAllVersions(messages, "fileHelpTable_kml",          errorInMethod);
        fileHelp_smallPdf_s          = getMessageInAllVersions(messages, "fileHelp_smallPdf",          errorInMethod);
        fileHelp_pdf_s               = getMessageInAllVersions(messages, "fileHelp_pdf",               errorInMethod);
        fileHelp_largePdf_s          = getMessageInAllVersions(messages, "fileHelp_largePdf",          errorInMethod);
        fileHelp_smallPng_s          = getMessageInAllVersions(messages, "fileHelp_smallPng",          errorInMethod);
        fileHelp_png_s               = getMessageInAllVersions(messages, "fileHelp_png",               errorInMethod);
        fileHelp_largePng_s          = getMessageInAllVersions(messages, "fileHelp_largePng",          errorInMethod);
        fileHelp_transparentPng_s    = getMessageInAllVersions(messages, "fileHelp_transparentPng",    errorInMethod);
        filesDescription_s           = getMessageInAllVersions(messages, "filesDescription",           errorInMethod);
        filesDocumentation_s         = getMessageInAllVersions(messages, "filesDocumentation",         errorInMethod);
        filesSort_s                  = getMessageInAllVersions(messages, "filesSort",                  errorInMethod);
        filesWarning_s               = getMessageInAllVersions(messages, "filesWarning",               errorInMethod);
        findOutChange_s              = getMessageInAllVersions(messages, "findOutChange",              errorInMethod);
        FIPSCountryCode_s            = getMessageInAllVersions(messages, "FIPSCountryCode",            errorInMethod);
        forSOSUse_s                  = getMessageInAllVersions(messages, "forSOSUse",                  errorInMethod);
        forWCSUse_s                  = getMessageInAllVersions(messages, "forWCSUse",                  errorInMethod);
        forWMSUse_s                  = getMessageInAllVersions(messages, "forWMSUse",                  errorInMethod);
        functions_s                  = getMessageInAllVersions(messages, "functions",                  errorInMethod);
        functionTooltip_s            = getMessageInAllVersions(messages, "functionTooltip",            errorInMethod);
        for (int i = 0; i < messages.length; i++) {
            functionTooltip_s[i]            = MessageFormat.format(functionTooltip_s[i], "distinct()");
        }
        functionDistinctCheck_s      = getMessageInAllVersions(messages, "functionDistinctCheck",      errorInMethod);
        functionDistinctTooltip_s    = getMessageInAllVersions(messages, "functionDistinctTooltip",    errorInMethod);
        for (int i = 0; i < messages.length; i++) {
            functionDistinctTooltip_s[i]    = MessageFormat.format(functionDistinctTooltip_s[i], "distinct()");
        }
        functionOrderByExtra_s       = getMessageInAllVersions(messages, "functionOrderByExtra",       errorInMethod);
        functionOrderByTooltip_s     = getMessageInAllVersions(messages, "functionOrderByTooltip",     errorInMethod);
        functionOrderBySort_s        = getMessageInAllVersions(messages, "functionOrderBySort",        errorInMethod);
        functionOrderBySort1_s       = getMessageInAllVersions(messages, "functionOrderBySort1",       errorInMethod);
        functionOrderBySort2_s       = getMessageInAllVersions(messages, "functionOrderBySort2",       errorInMethod);
        functionOrderBySort3_s       = getMessageInAllVersions(messages, "functionOrderBySort3",       errorInMethod);
        functionOrderBySort4_s       = getMessageInAllVersions(messages, "functionOrderBySort4",       errorInMethod);
        functionOrderBySortLeast_s   = getMessageInAllVersions(messages, "functionOrderBySortLeast",   errorInMethod);
        functionOrderBySortRowMax_s  = getMessageInAllVersions(messages, "functionOrderBySortRowMax",  errorInMethod);
        generatedAt_s                = getMessageInAllVersions(messages, "generatedAt",                errorInMethod);
        geoServicesDescription_s     = getMessageInAllVersions(messages, "geoServicesDescription",     errorInMethod);
        getStartedHtml_s             = getMessageInAllVersions(messages, "getStartedHtml",             errorInMethod);
        TableWriterHtmlTable.htmlTableMaxMB     = messages0.getInt("htmlTableMaxMB", TableWriterHtmlTable.htmlTableMaxMB);                                   
        htmlTableMaxMessage_s        = getMessageInAllVersions(messages, "htmlTableMaxMessage",        errorInMethod);

        hpn_information_s            = getMessageInAllVersions(messages, "hpn_information",           errorInMethod);
        hpn_legalNotices_s           = getMessageInAllVersions(messages, "hpn_legalNotices",          errorInMethod);
        hpn_dataProviderForm_s       = getMessageInAllVersions(messages, "hpn_dataProviderForm",      errorInMethod);
        hpn_dataProviderFormP1_s     = getMessageInAllVersions(messages, "hpn_dataProviderFormP1",    errorInMethod);
        hpn_dataProviderFormP2_s     = getMessageInAllVersions(messages, "hpn_dataProviderFormP2",    errorInMethod);
        hpn_dataProviderFormP3_s     = getMessageInAllVersions(messages, "hpn_dataProviderFormP3",    errorInMethod);
        hpn_dataProviderFormP4_s     = getMessageInAllVersions(messages, "hpn_dataProviderFormP4",    errorInMethod);
        hpn_dataProviderFormDone_s   = getMessageInAllVersions(messages, "hpn_dataProviderFormDone",  errorInMethod);
        hpn_status_s                 = getMessageInAllVersions(messages, "hpn_status",                errorInMethod);
        hpn_restfulWebService_s      = getMessageInAllVersions(messages, "hpn_restfulWebService",     errorInMethod);
        hpn_documentation_s          = getMessageInAllVersions(messages, "hpn_documentation",         errorInMethod);
        hpn_help_s                   = getMessageInAllVersions(messages, "hpn_help",                  errorInMethod);
        hpn_files_s                  = getMessageInAllVersions(messages, "hpn_files",                 errorInMethod);
        hpn_SOS_s                    = getMessageInAllVersions(messages, "hpn_SOS",                   errorInMethod);
        hpn_WCS_s                    = getMessageInAllVersions(messages, "hpn_WCS",                   errorInMethod);
        hpn_slideSorter_s            = getMessageInAllVersions(messages, "hpn_slideSorter",           errorInMethod);
        hpn_add_s                    = getMessageInAllVersions(messages, "hpn_add",                   errorInMethod);
        hpn_list_s                   = getMessageInAllVersions(messages, "hpn_list",                  errorInMethod);
        hpn_validate_s               = getMessageInAllVersions(messages, "hpn_validate",              errorInMethod);
        hpn_remove_s                 = getMessageInAllVersions(messages, "hpn_remove",                errorInMethod);
        hpn_convert_s                = getMessageInAllVersions(messages, "hpn_convert",               errorInMethod);
        hpn_fipsCounty_s             = getMessageInAllVersions(messages, "hpn_fipsCounty",            errorInMethod);
        hpn_OAAcronyms_s             = getMessageInAllVersions(messages, "hpn_OAAcronyms",            errorInMethod);
        hpn_OAVariableNames_s        = getMessageInAllVersions(messages, "hpn_OAVariableNames",       errorInMethod);
        hpn_keywords_s               = getMessageInAllVersions(messages, "hpn_keywords",              errorInMethod);
        hpn_time_s                   = getMessageInAllVersions(messages, "hpn_time",                  errorInMethod);
        hpn_units_s                  = getMessageInAllVersions(messages, "hpn_units",                 errorInMethod);

        imageDataCourtesyOf_s        = getMessageInAllVersions(messages, "imageDataCourtesyOf",        errorInMethod);
        imageWidths                = String2.toIntArray(String2.split(messages0.getNotNothingString("imageWidths",  errorInMethod), ','));
        imageHeights               = String2.toIntArray(String2.split(messages0.getNotNothingString("imageHeights", errorInMethod), ','));
        indexViewAll_s               = getMessageInAllVersions(messages, "indexViewAll",               errorInMethod);
        indexSearchWith_s            = getMessageInAllVersions(messages, "indexSearchWith",            errorInMethod);
        indexDevelopersSearch_s      = getMessageInAllVersions(messages, "indexDevelopersSearch",      errorInMethod);
        indexProtocol_s              = getMessageInAllVersions(messages, "indexProtocol",              errorInMethod);
        indexDescription_s           = getMessageInAllVersions(messages, "indexDescription",           errorInMethod);
        indexDatasets_s              = getMessageInAllVersions(messages, "indexDatasets",              errorInMethod);
        indexDocumentation_s         = getMessageInAllVersions(messages, "indexDocumentation",         errorInMethod);
        indexRESTfulSearch_s         = getMessageInAllVersions(messages, "indexRESTfulSearch",         errorInMethod);
        indexAllDatasetsSearch_s     = getMessageInAllVersions(messages, "indexAllDatasetsSearch",     errorInMethod);
        indexOpenSearch_s            = getMessageInAllVersions(messages, "indexOpenSearch",            errorInMethod);
        indexServices_s              = getMessageInAllVersions(messages, "indexServices",              errorInMethod);
        indexDescribeServices_s      = getMessageInAllVersions(messages, "indexDescribeServices",      errorInMethod);
        indexMetadata_s              = getMessageInAllVersions(messages, "indexMetadata",              errorInMethod);
        indexWAF1_s                  = getMessageInAllVersions(messages, "indexWAF1",                  errorInMethod);
        indexWAF2_s                  = getMessageInAllVersions(messages, "indexWAF2",                  errorInMethod);
        indexConverters_s            = getMessageInAllVersions(messages, "indexConverters",            errorInMethod);
        indexDescribeConverters_s    = getMessageInAllVersions(messages, "indexDescribeConverters",    errorInMethod);
        infoAboutFrom_s              = getMessageInAllVersions(messages, "infoAboutFrom",              errorInMethod);
        infoTableTitleHtml_s         = getMessageInAllVersions(messages, "infoTableTitleHtml",         errorInMethod);
        infoRequestForm_s            = getMessageInAllVersions(messages, "infoRequestForm",            errorInMethod);
        inotifyFix_s                 = getMessageInAllVersions(messages, "inotifyFix",                 errorInMethod);
        inotifyFixCommands         = messages0.getNotNothingString("inotifyFixCommands",         errorInMethod);
        for (int i = 0; i < messages.length; i++) {
            inotifyFix_s[i]          = MessageFormat.format(inotifyFix_s[i], inotifyFixCommands);
        }
        interpolate_s                = getMessageInAllVersions(messages, "interpolate",                errorInMethod);
        javaProgramsHTML_s           = getMessageInAllVersions(messages, "javaProgramsHTML",           errorInMethod);
        justGenerateAndView_s        = getMessageInAllVersions(messages, "justGenerateAndView",        errorInMethod);
        justGenerateAndViewTooltip_s = getMessageInAllVersions(messages, "justGenerateAndViewTooltip", errorInMethod);
        justGenerateAndViewUrl_s     = getMessageInAllVersions(messages, "justGenerateAndViewUrl",     errorInMethod);
        justGenerateAndViewGraphUrlTooltip_s = getMessageInAllVersions(messages, "justGenerateAndViewGraphUrlTooltip", errorInMethod);
        keywords_word_s              = getMessageInAllVersions(messages, "keywords",                   errorInMethod);
        langCode_s                   = getMessageInAllVersions(messages, "langCode",                   errorInMethod);
        //care
        legal_s                      = getMessageInAllVersions(messages, "legal",                      errorInMethod);
        for (int i = 0; i < messages.length; i++) {
            legal_s[i]               = getSetupEVString(setup, ev,"legal",                      legal_s[i]); //optionally in setup.xml
        }
        legalNotices_s               = getMessageInAllVersions(messages, "legalNotices",               errorInMethod);
        //These strings can be null
        legendTitle1               = messages0.getString(          "legendTitle1",               "");
        legendTitle1               =   getSetupEVString(setup, ev,"legendTitle1",               legendTitle1); //optionally in setup.xml
        legendTitle2               = messages0.getString(          "legendTitle2",               "");
        legendTitle2               =   getSetupEVString(setup, ev,"legendTitle2",               legendTitle2); //optionally in setup.xml

        license_s                    = getMessageInAllVersions(messages, "license",                    errorInMethod);
        listAll_s                    = getMessageInAllVersions(messages, "listAll",                    errorInMethod);
        listOfDatasets_s             = getMessageInAllVersions(messages, "listOfDatasets",             errorInMethod);
        LogIn_s                      = getMessageInAllVersions(messages, "LogIn",                      errorInMethod);
        login_s                      = getMessageInAllVersions(messages, "login",                      errorInMethod);
        loginHTML_s                  = getMessageInAllVersions(messages, "loginHTML",                  errorInMethod);
        loginAttemptBlocked_s        = getMessageInAllVersions(messages, "loginAttemptBlocked",        errorInMethod);
        loginDescribeCustom_s        = getMessageInAllVersions(messages, "loginDescribeCustom",        errorInMethod);
        loginDescribeEmail_s         = getMessageInAllVersions(messages, "loginDescribeEmail",         errorInMethod);
        loginDescribeGoogle_s        = getMessageInAllVersions(messages, "loginDescribeGoogle",        errorInMethod);
        loginDescribeOrcid_s         = getMessageInAllVersions(messages, "loginDescribeOrcid",         errorInMethod);
        loginDescribeOauth2_s        = getMessageInAllVersions(messages, "loginDescribeOauth2",        errorInMethod);
        loginCanNot_s                = getMessageInAllVersions(messages, "loginCanNot",                errorInMethod);
        loginAreNot_s                = getMessageInAllVersions(messages, "loginAreNot",                errorInMethod);
        loginToLogIn_s               = getMessageInAllVersions(messages, "loginToLogIn",               errorInMethod);
        loginEmailAddress_s          = getMessageInAllVersions(messages, "loginEmailAddress",          errorInMethod);
        loginYourEmailAddress_s      = getMessageInAllVersions(messages, "loginYourEmailAddress",      errorInMethod);
        loginUserName_s              = getMessageInAllVersions(messages, "loginUserName",              errorInMethod);
        loginPassword_s              = getMessageInAllVersions(messages, "loginPassword",              errorInMethod);
        loginUserNameAndPassword_s   = getMessageInAllVersions(messages, "loginUserNameAndPassword",   errorInMethod);
        loginGoogleSignIn_s          = getMessageInAllVersions(messages, "loginGoogleSignIn",          errorInMethod);
        loginGoogleSignIn2_s         = getMessageInAllVersions(messages, "loginGoogleSignIn2",         errorInMethod);
        loginOrcidSignIn_s           = getMessageInAllVersions(messages, "loginOrcidSignIn",           errorInMethod);
        loginErddap_s                = getMessageInAllVersions(messages, "loginErddap",                errorInMethod);
        loginOpenID_s                = getMessageInAllVersions(messages, "loginOpenID",                errorInMethod);
        loginOpenIDOr_s              = getMessageInAllVersions(messages, "loginOpenIDOr",              errorInMethod);
        loginOpenIDCreate_s          = getMessageInAllVersions(messages, "loginOpenIDCreate",          errorInMethod);
        loginOpenIDFree_s            = getMessageInAllVersions(messages, "loginOpenIDFree",            errorInMethod);
        loginOpenIDSame_s            = getMessageInAllVersions(messages, "loginOpenIDSame",            errorInMethod);
        loginAs_s                    = getMessageInAllVersions(messages, "loginAs",                    errorInMethod);
        loginPartwayAs_s             = getMessageInAllVersions(messages, "loginPartwayAs",             errorInMethod);
        loginFailed_s                = getMessageInAllVersions(messages, "loginFailed",                errorInMethod);
        loginSucceeded_s             = getMessageInAllVersions(messages, "loginSucceeded",             errorInMethod);
        loginInvalid_s               = getMessageInAllVersions(messages, "loginInvalid",               errorInMethod);
        loginNot_s                   = getMessageInAllVersions(messages, "loginNot",                   errorInMethod);
        loginBack_s                  = getMessageInAllVersions(messages, "loginBack",                  errorInMethod);
        loginProblemExact_s          = getMessageInAllVersions(messages, "loginProblemExact",          errorInMethod);
        loginProblemExpire_s         = getMessageInAllVersions(messages, "loginProblemExpire",         errorInMethod);
        loginProblemGoogleAgain_s    = getMessageInAllVersions(messages, "loginProblemGoogleAgain",    errorInMethod);
        loginProblemOrcidAgain_s     = getMessageInAllVersions(messages, "loginProblemOrcidAgain",     errorInMethod);
        loginProblemOauth2Again_s    = getMessageInAllVersions(messages, "loginProblemOauth2Again",    errorInMethod);
        loginProblemSameBrowser_s    = getMessageInAllVersions(messages, "loginProblemSameBrowser",    errorInMethod);
        loginProblem3Times_s         = getMessageInAllVersions(messages, "loginProblem3Times",         errorInMethod);
        loginProblems_s              = getMessageInAllVersions(messages, "loginProblems",              errorInMethod);
        loginProblemsAfter_s         = getMessageInAllVersions(messages, "loginProblemsAfter",         errorInMethod);
        loginPublicAccess_s          = getMessageInAllVersions(messages, "loginPublicAccess",          errorInMethod);
        LogOut_s                     = getMessageInAllVersions(messages, "LogOut",                     errorInMethod);
        logout_s                     = getMessageInAllVersions(messages, "logout",                     errorInMethod);
        logoutOpenID_s               = getMessageInAllVersions(messages, "logoutOpenID",               errorInMethod);
        logoutSuccess_s              = getMessageInAllVersions(messages, "logoutSuccess",              errorInMethod);
        mag_s                        = getMessageInAllVersions(messages, "mag",                        errorInMethod);
        magAxisX_s                   = getMessageInAllVersions(messages, "magAxisX",                   errorInMethod);
        magAxisY_s                   = getMessageInAllVersions(messages, "magAxisY",                   errorInMethod);
        magAxisColor_s               = getMessageInAllVersions(messages, "magAxisColor",               errorInMethod);
        magAxisStickX_s              = getMessageInAllVersions(messages, "magAxisStickX",              errorInMethod);
        magAxisStickY_s              = getMessageInAllVersions(messages, "magAxisStickY",              errorInMethod);
        magAxisVectorX_s             = getMessageInAllVersions(messages, "magAxisVectorX",             errorInMethod);
        magAxisVectorY_s             = getMessageInAllVersions(messages, "magAxisVectorY",             errorInMethod);
        magAxisHelpGraphX_s          = getMessageInAllVersions(messages, "magAxisHelpGraphX",          errorInMethod);
        magAxisHelpGraphY_s          = getMessageInAllVersions(messages, "magAxisHelpGraphY",          errorInMethod);
        magAxisHelpMarkerColor_s     = getMessageInAllVersions(messages, "magAxisHelpMarkerColor",     errorInMethod);
        magAxisHelpSurfaceColor_s    = getMessageInAllVersions(messages, "magAxisHelpSurfaceColor",    errorInMethod);
        magAxisHelpStickX_s          = getMessageInAllVersions(messages, "magAxisHelpStickX",          errorInMethod);
        magAxisHelpStickY_s          = getMessageInAllVersions(messages, "magAxisHelpStickY",          errorInMethod);
        magAxisHelpMapX_s            = getMessageInAllVersions(messages, "magAxisHelpMapX",            errorInMethod);
        magAxisHelpMapY_s            = getMessageInAllVersions(messages, "magAxisHelpMapY",            errorInMethod);
        magAxisHelpVectorX_s         = getMessageInAllVersions(messages, "magAxisHelpVectorX",         errorInMethod);
        magAxisHelpVectorY_s         = getMessageInAllVersions(messages, "magAxisHelpVectorY",         errorInMethod);
        magAxisVarHelp_s             = getMessageInAllVersions(messages, "magAxisVarHelp",             errorInMethod);
        magAxisVarHelpGrid_s         = getMessageInAllVersions(messages, "magAxisVarHelpGrid",         errorInMethod);
        magConstraintHelp_s          = getMessageInAllVersions(messages, "magConstraintHelp",          errorInMethod);
        magDocumentation_s           = getMessageInAllVersions(messages, "magDocumentation",           errorInMethod);
        magDownload_s                = getMessageInAllVersions(messages, "magDownload",                errorInMethod);
        magDownloadTooltip_s         = getMessageInAllVersions(messages, "magDownloadTooltip",         errorInMethod);
        magFileType_s                = getMessageInAllVersions(messages, "magFileType",                errorInMethod);
        magGraphType_s               = getMessageInAllVersions(messages, "magGraphType",               errorInMethod);
        magGraphTypeTooltipGrid_s    = getMessageInAllVersions(messages, "magGraphTypeTooltipGrid",    errorInMethod);
        magGraphTypeTooltipTable_s   = getMessageInAllVersions(messages, "magGraphTypeTooltipTable",   errorInMethod);
        magGS_s                      = getMessageInAllVersions(messages, "magGS",                      errorInMethod);
        magGSMarkerType_s            = getMessageInAllVersions(messages, "magGSMarkerType",            errorInMethod);
        magGSSize_s                  = getMessageInAllVersions(messages, "magGSSize",                  errorInMethod);
        magGSColor_s                 = getMessageInAllVersions(messages, "magGSColor",                 errorInMethod);
        magGSColorBar_s              = getMessageInAllVersions(messages, "magGSColorBar",              errorInMethod);
        magGSColorBarTooltip_s       = getMessageInAllVersions(messages, "magGSColorBarTooltip",       errorInMethod);
        magGSContinuity_s            = getMessageInAllVersions(messages, "magGSContinuity",            errorInMethod);
        magGSContinuityTooltip_s     = getMessageInAllVersions(messages, "magGSContinuityTooltip",     errorInMethod);
        magGSScale_s                 = getMessageInAllVersions(messages, "magGSScale",                 errorInMethod);
        magGSScaleTooltip_s          = getMessageInAllVersions(messages, "magGSScaleTooltip",          errorInMethod);
        magGSMin_s                   = getMessageInAllVersions(messages, "magGSMin",                   errorInMethod);
        magGSMinTooltip_s            = getMessageInAllVersions(messages, "magGSMinTooltip",            errorInMethod);
        magGSMax_s                   = getMessageInAllVersions(messages, "magGSMax",                   errorInMethod);
        magGSMaxTooltip_s            = getMessageInAllVersions(messages, "magGSMaxTooltip",            errorInMethod);
        magGSNSections_s             = getMessageInAllVersions(messages, "magGSNSections",             errorInMethod);
        magGSNSectionsTooltip_s      = getMessageInAllVersions(messages, "magGSNSectionsTooltip",      errorInMethod);
        magGSLandMask_s              = getMessageInAllVersions(messages, "magGSLandMask",              errorInMethod);
        magGSLandMaskTooltipGrid_s   = getMessageInAllVersions(messages, "magGSLandMaskTooltipGrid",   errorInMethod);
        magGSLandMaskTooltipTable_s  = getMessageInAllVersions(messages, "magGSLandMaskTooltipTable",  errorInMethod);
        magGSVectorStandard_s        = getMessageInAllVersions(messages, "magGSVectorStandard",        errorInMethod);
        magGSVectorStandardTooltip_s = getMessageInAllVersions(messages, "magGSVectorStandardTooltip", errorInMethod);
        magGSYAscendingTooltip_s     = getMessageInAllVersions(messages, "magGSYAscendingTooltip",     errorInMethod);
        magGSYAxisMin_s              = getMessageInAllVersions(messages, "magGSYAxisMin",              errorInMethod);
        magGSYAxisMax_s              = getMessageInAllVersions(messages, "magGSYAxisMax",              errorInMethod);
        magGSYRangeMinTooltip_s      = getMessageInAllVersions(messages, "magGSYRangeMinTooltip",      errorInMethod); 
        magGSYRangeMaxTooltip_s      = getMessageInAllVersions(messages, "magGSYRangeMaxTooltip",      errorInMethod);
        magGSYRangeTooltip_s         = getMessageInAllVersions(messages, "magGSYRangeTooltip",         errorInMethod);        
        magGSYScaleTooltip_s         = getMessageInAllVersions(messages, "magGSYScaleTooltip",         errorInMethod);        
        magItemFirst_s               = getMessageInAllVersions(messages, "magItemFirst",               errorInMethod);
        magItemPrevious_s            = getMessageInAllVersions(messages, "magItemPrevious",            errorInMethod);
        magItemNext_s                = getMessageInAllVersions(messages, "magItemNext",                errorInMethod);
        magItemLast_s                = getMessageInAllVersions(messages, "magItemLast",                errorInMethod);
        magJust1Value_s              = getMessageInAllVersions(messages, "magJust1Value",              errorInMethod);
        magRange_s                   = getMessageInAllVersions(messages, "magRange",                   errorInMethod);
        magRangeTo_s                 = getMessageInAllVersions(messages, "magRangeTo",                 errorInMethod);
        magRedraw_s                  = getMessageInAllVersions(messages, "magRedraw",                  errorInMethod);
        magRedrawTooltip_s           = getMessageInAllVersions(messages, "magRedrawTooltip",           errorInMethod);
        magTimeRange_s               = getMessageInAllVersions(messages, "magTimeRange",               errorInMethod);
        magTimeRangeFirst_s          = getMessageInAllVersions(messages, "magTimeRangeFirst",          errorInMethod);
        magTimeRangeBack_s           = getMessageInAllVersions(messages, "magTimeRangeBack",           errorInMethod);
        magTimeRangeForward_s        = getMessageInAllVersions(messages, "magTimeRangeForward",        errorInMethod);
        magTimeRangeLast_s           = getMessageInAllVersions(messages, "magTimeRangeLast",           errorInMethod);
        magTimeRangeTooltip_s        = getMessageInAllVersions(messages, "magTimeRangeTooltip",        errorInMethod);
        magTimeRangeTooltip2_s       = getMessageInAllVersions(messages, "magTimeRangeTooltip2",       errorInMethod);
        magTimesVary_s               = getMessageInAllVersions(messages, "magTimesVary",               errorInMethod);
        magViewUrl_s                 = getMessageInAllVersions(messages, "magViewUrl",                 errorInMethod);
        magZoom_s                    = getMessageInAllVersions(messages, "magZoom",                    errorInMethod);
        magZoomCenter_s              = getMessageInAllVersions(messages, "magZoomCenter",              errorInMethod);
        magZoomCenterTooltip_s       = getMessageInAllVersions(messages, "magZoomCenterTooltip",       errorInMethod);
        magZoomIn_s                  = getMessageInAllVersions(messages, "magZoomIn",                  errorInMethod);
        magZoomInTooltip_s           = getMessageInAllVersions(messages, "magZoomInTooltip",           errorInMethod);
        magZoomOut_s                 = getMessageInAllVersions(messages, "magZoomOut",                 errorInMethod);
        magZoomOutTooltip_s          = getMessageInAllVersions(messages, "magZoomOutTooltip",          errorInMethod);
        magZoomALittle_s             = getMessageInAllVersions(messages, "magZoomALittle",             errorInMethod);
        magZoomData_s                = getMessageInAllVersions(messages, "magZoomData",                errorInMethod);
        magZoomOutData_s             = getMessageInAllVersions(messages, "magZoomOutData",             errorInMethod);
        magGridTooltip_s             = getMessageInAllVersions(messages, "magGridTooltip",             errorInMethod);
        magTableTooltip_s            = getMessageInAllVersions(messages, "magTableTooltip",            errorInMethod);
        Math2.memory               = messages0.getNotNothingString("memory",                     errorInMethod);
        Math2.memoryTooMuchData    = messages0.getNotNothingString("memoryTooMuchData",          errorInMethod);
        Math2.memoryArraySize      = messages0.getNotNothingString("memoryArraySize",            errorInMethod);
      Math2.memoryThanCurrentlySafe= messages0.getNotNothingString("memoryThanCurrentlySafe",    errorInMethod);
        Math2.memoryThanSafe       = messages0.getNotNothingString("memoryThanSafe",             errorInMethod);
        metadataDownload_s           = getMessageInAllVersions(messages, "metadataDownload",           errorInMethod);
        moreInformation_s            = getMessageInAllVersions(messages, "moreInformation",            errorInMethod);
        MustBe.THERE_IS_NO_DATA    = messages0.getNotNothingString("MustBeThereIsNoData",        errorInMethod);
        MustBe.NotNull             = messages0.getNotNothingString("MustBeNotNull",              errorInMethod);
        MustBe.NotEmpty            = messages0.getNotNothingString("MustBeNotEmpty",             errorInMethod);
        MustBe.InternalError       = messages0.getNotNothingString("MustBeInternalError",        errorInMethod);
        MustBe.OutOfMemoryError    = messages0.getNotNothingString("MustBeOutOfMemoryError",     errorInMethod);
        nMatching_s                  = getMessageInAllVersions(messages, "nMatching",                  errorInMethod);
        nMatchingAlphabetical_s      = getMessageInAllVersions(messages, "nMatchingAlphabetical",      errorInMethod);
        nMatchingMostRelevant_s      = getMessageInAllVersions(messages, "nMatchingMostRelevant",      errorInMethod);
        nMatching1_s                 = getMessageInAllVersions(messages, "nMatching1",                 errorInMethod);
        nMatchingPage_s              = getMessageInAllVersions(messages, "nMatchingPage",              errorInMethod);
        nMatchingCurrent_s           = getMessageInAllVersions(messages, "nMatchingCurrent",           errorInMethod);
        noDataFixedValue_s           = getMessageInAllVersions(messages, "noDataFixedValue",           errorInMethod);
        noDataNoLL_s                 = getMessageInAllVersions(messages, "noDataNoLL",                 errorInMethod);
        noDatasetWith_s              = getMessageInAllVersions(messages, "noDatasetWith",              errorInMethod);
        noPage1_s                    = getMessageInAllVersions(messages, "noPage1",                    errorInMethod);
        noPage2_s                    = getMessageInAllVersions(messages, "noPage2",                    errorInMethod);
        notAllowed_s                 = getMessageInAllVersions(messages, "notAllowed",                 errorInMethod);
        notAuthorized_s              = getMessageInAllVersions(messages, "notAuthorized",              errorInMethod);
        notAuthorizedForData_s       = getMessageInAllVersions(messages, "notAuthorizedForData",       errorInMethod);
        notAvailable_s               = getMessageInAllVersions(messages, "notAvailable",               errorInMethod);
        note_s                       = getMessageInAllVersions(messages, "note",                       errorInMethod);
        noXxx_s                      = getMessageInAllVersions(messages, "noXxx",                      errorInMethod);
        noXxxBecause_s               = getMessageInAllVersions(messages, "noXxxBecause",               errorInMethod);
        noXxxBecause2_s              = getMessageInAllVersions(messages, "noXxxBecause2",              errorInMethod);
        noXxxNotActive_s             = getMessageInAllVersions(messages, "noXxxNotActive",             errorInMethod);
        noXxxNoAxis1_s               = getMessageInAllVersions(messages, "noXxxNoAxis1",               errorInMethod);
        noXxxNoCdmDataType_s         = getMessageInAllVersions(messages, "noXxxNoCdmDataType",         errorInMethod);
        noXxxNoColorBar_s            = getMessageInAllVersions(messages, "noXxxNoColorBar",            errorInMethod);
        noXxxNoLL_s                  = getMessageInAllVersions(messages, "noXxxNoLL",                  errorInMethod);
        noXxxNoLLEvenlySpaced_s      = getMessageInAllVersions(messages, "noXxxNoLLEvenlySpaced",      errorInMethod);
        noXxxNoLLGt1_s               = getMessageInAllVersions(messages, "noXxxNoLLGt1",               errorInMethod);
        noXxxNoLLT_s                 = getMessageInAllVersions(messages, "noXxxNoLLT",                 errorInMethod);
        noXxxNoLonIn180_s            = getMessageInAllVersions(messages, "noXxxNoLonIn180",            errorInMethod);
        noXxxNoNonString_s           = getMessageInAllVersions(messages, "noXxxNoNonString",           errorInMethod);
        noXxxNo2NonString_s          = getMessageInAllVersions(messages, "noXxxNo2NonString",          errorInMethod);
        noXxxNoStation_s             = getMessageInAllVersions(messages, "noXxxNoStation",             errorInMethod);
        noXxxNoStationID_s           = getMessageInAllVersions(messages, "noXxxNoStationID",           errorInMethod);
        noXxxNoSubsetVariables_s     = getMessageInAllVersions(messages, "noXxxNoSubsetVariables",     errorInMethod);
        noXxxNoOLLSubsetVariables_s  = getMessageInAllVersions(messages, "noXxxNoOLLSubsetVariables",  errorInMethod);
        noXxxNoMinMax_s              = getMessageInAllVersions(messages, "noXxxNoMinMax",              errorInMethod);
        noXxxItsGridded_s            = getMessageInAllVersions(messages, "noXxxItsGridded",            errorInMethod);
        noXxxItsTabular_s            = getMessageInAllVersions(messages, "noXxxItsTabular",            errorInMethod);
        oneRequestAtATime_s          = getMessageInAllVersions(messages, "oneRequestAtATime",          errorInMethod);
        openSearchDescription_s      = getMessageInAllVersions(messages, "openSearchDescription",      errorInMethod);
        optional_s                   = getMessageInAllVersions(messages, "optional",                   errorInMethod);
        options_s                    = getMessageInAllVersions(messages, "options",                    errorInMethod);
        orRefineSearchWith_s         = getMessageInAllVersions(messages, "orRefineSearchWith", "", " ",errorInMethod);
        orSearchWith_s               = getMessageInAllVersions(messages, "orSearchWith", "", " ",           errorInMethod);
        orComma_s                    = getMessageInAllVersions(messages, "orComma", "", " ",                errorInMethod);
        outOfDateKeepTrack_s         = getMessageInAllVersions(messages, "outOfDateKeepTrack",         errorInMethod);
        outOfDateHtml_s              = getMessageInAllVersions(messages, "outOfDateHtml",              errorInMethod);
        //same in all messages.
        palettes                   = String2.split(messages0.getNotNothingString("palettes",     errorInMethod), ',');
        DEFAULT_palettes = palettes; //used by LoadDatasets if palettes tag is empty
        DEFAULT_palettes_set = String2.stringArrayToSet(palettes);
        palettes0 = new String[palettes.length + 1];
        palettes0[0] = "";
        System.arraycopy(palettes, 0, palettes0, 1, palettes.length);
        patientData_s                = getMessageInAllVersions(messages, "patientData",                errorInMethod);
        patientYourGraph_s           = getMessageInAllVersions(messages, "patientYourGraph",           errorInMethod);
        pdfWidths                  = String2.toIntArray(String2.split(messages0.getNotNothingString("pdfWidths",  errorInMethod), ','));
        pdfHeights                 = String2.toIntArray(String2.split(messages0.getNotNothingString("pdfHeights", errorInMethod), ','));
        percentEncode_s              = getMessageInAllVersions(messages, "percentEncode",              errorInMethod);
        pickADataset_s               = getMessageInAllVersions(messages, "pickADataset",               errorInMethod);
        protocolSearchHtml_s         = getMessageInAllVersions(messages, "protocolSearchHtml",         errorInMethod);
        protocolSearch2Html_s        = getMessageInAllVersions(messages, "protocolSearch2Html",        errorInMethod);
        protocolClick_s              = getMessageInAllVersions(messages, "protocolClick",              errorInMethod);
        queryError                 = messages0.getNotNothingString("queryError",                     errorInMethod) + " ";
        Table.QUERY_ERROR = queryError;
        queryError180_s              = getMessageInAllVersions(messages, "queryError180",              errorInMethod);
        queryError1Value_s           = getMessageInAllVersions(messages, "queryError1Value",           errorInMethod);
        queryError1Var_s             = getMessageInAllVersions(messages, "queryError1Var",             errorInMethod);
        queryError2Var_s             = getMessageInAllVersions(messages, "queryError2Var",             errorInMethod);
        queryErrorActualRange_s      = getMessageInAllVersions(messages, "queryErrorActualRange",      errorInMethod);
        queryErrorAdjusted_s         = getMessageInAllVersions(messages, "queryErrorAdjusted",         errorInMethod);
        queryErrorAscending_s        = getMessageInAllVersions(messages, "queryErrorAscending",        errorInMethod);
        queryErrorConstraintNaN_s    = getMessageInAllVersions(messages, "queryErrorConstraintNaN",    errorInMethod);
        queryErrorEqualSpacing_s     = getMessageInAllVersions(messages, "queryErrorEqualSpacing",     errorInMethod);
        queryErrorExpectedAt_s       = getMessageInAllVersions(messages, "queryErrorExpectedAt",       errorInMethod);
        queryErrorFileType_s         = getMessageInAllVersions(messages, "queryErrorFileType",         errorInMethod);
        queryErrorInvalid_s          = getMessageInAllVersions(messages, "queryErrorInvalid",          errorInMethod);
        queryErrorLL_s               = getMessageInAllVersions(messages, "queryErrorLL",               errorInMethod);
        queryErrorLLGt1_s            = getMessageInAllVersions(messages, "queryErrorLLGt1",            errorInMethod);
        queryErrorLLT_s              = getMessageInAllVersions(messages, "queryErrorLLT",              errorInMethod);
        queryErrorNeverTrue_s        = getMessageInAllVersions(messages, "queryErrorNeverTrue",        errorInMethod);
        queryErrorNeverBothTrue_s    = getMessageInAllVersions(messages, "queryErrorNeverBothTrue",    errorInMethod);
        queryErrorNotAxis_s          = getMessageInAllVersions(messages, "queryErrorNotAxis",          errorInMethod);
        queryErrorNotExpectedAt_s    = getMessageInAllVersions(messages, "queryErrorNotExpectedAt",    errorInMethod);
        queryErrorNotFoundAfter_s    = getMessageInAllVersions(messages, "queryErrorNotFoundAfter",    errorInMethod);
        queryErrorOccursTwice_s      = getMessageInAllVersions(messages, "queryErrorOccursTwice",      errorInMethod);
        Table.ORDER_BY_CLOSEST_ERROR=messages0.getNotNothingString("queryErrorOrderByClosest",   errorInMethod);
        Table.ORDER_BY_LIMIT_ERROR = messages0.getNotNothingString("queryErrorOrderByLimit",     errorInMethod);
        Table.ORDER_BY_MEAN_ERROR  = messages0.getNotNothingString("queryErrorOrderByMean",      errorInMethod);
        queryErrorOrderByVariable_s  = getMessageInAllVersions(messages, "queryErrorOrderByVariable",  errorInMethod);
        queryErrorUnknownVariable_s  = getMessageInAllVersions(messages, "queryErrorUnknownVariable",  errorInMethod);

        queryErrorGrid1Axis_s        = getMessageInAllVersions(messages, "queryErrorGrid1Axis",        errorInMethod);
        queryErrorGridAmp_s          = getMessageInAllVersions(messages, "queryErrorGridAmp",          errorInMethod);
        queryErrorGridDiagnostic_s   = getMessageInAllVersions(messages, "queryErrorGridDiagnostic",   errorInMethod);
        queryErrorGridBetween_s      = getMessageInAllVersions(messages, "queryErrorGridBetween",      errorInMethod);
        queryErrorGridLessMin_s      = getMessageInAllVersions(messages, "queryErrorGridLessMin",      errorInMethod);
        queryErrorGridGreaterMax_s   = getMessageInAllVersions(messages, "queryErrorGridGreaterMax",   errorInMethod);
        queryErrorGridMissing_s      = getMessageInAllVersions(messages, "queryErrorGridMissing",      errorInMethod);
        queryErrorGridNoAxisVar_s    = getMessageInAllVersions(messages, "queryErrorGridNoAxisVar",    errorInMethod);
        queryErrorGridNoDataVar_s    = getMessageInAllVersions(messages, "queryErrorGridNoDataVar",    errorInMethod);
        queryErrorGridNotIdentical_s = getMessageInAllVersions(messages, "queryErrorGridNotIdentical", errorInMethod);
        queryErrorGridSLessS_s       = getMessageInAllVersions(messages, "queryErrorGridSLessS",       errorInMethod);
        queryErrorLastEndP_s         = getMessageInAllVersions(messages, "queryErrorLastEndP",         errorInMethod);
        queryErrorLastExpected_s     = getMessageInAllVersions(messages, "queryErrorLastExpected",     errorInMethod);
        queryErrorLastUnexpected_s   = getMessageInAllVersions(messages, "queryErrorLastUnexpected",   errorInMethod);
        queryErrorLastPMInvalid_s    = getMessageInAllVersions(messages, "queryErrorLastPMInvalid",    errorInMethod);
        queryErrorLastPMInteger_s    = getMessageInAllVersions(messages, "queryErrorLastPMInteger",    errorInMethod);        
        //same in all versions
        questionMarkImageFile      = messages0.getNotNothingString("questionMarkImageFile",      errorInMethod);
        questionMarkImageFile      = getSetupEVString(setup, ev,"questionMarkImageFile",      questionMarkImageFile); //optional
        rangesFromTo_s               = getMessageInAllVersions(messages, "rangesFromTo",               errorInMethod);
        requestFormatExamplesHtml_s  = getMessageInAllVersions(messages, "requestFormatExamplesHtml",  errorInMethod);
        resetTheForm_s               = getMessageInAllVersions(messages, "resetTheForm",               errorInMethod);
        resetTheFormWas_s            = getMessageInAllVersions(messages, "resetTheFormWas",            errorInMethod);
        resourceNotFound           = messages0.getNotNothingString("resourceNotFound",   errorInMethod) + " ";
        restfulWebServices_s         = getMessageInAllVersions(messages, "restfulWebServices",         errorInMethod);
        restfulHTML_s                = getMessageInAllVersions(messages, "restfulHTML",                errorInMethod);
        restfulHTMLContinued_s       = getMessageInAllVersions(messages, "restfulHTMLContinued",       errorInMethod);
        restfulGetAllDataset_s       = getMessageInAllVersions(messages, "restfulGetAllDataset",       errorInMethod);
        restfulProtocols_s           = getMessageInAllVersions(messages, "restfulProtocols",           errorInMethod);
        SOSDocumentation_s           = getMessageInAllVersions(messages, "SOSDocumentation",           errorInMethod);
        WCSDocumentation_s           = getMessageInAllVersions(messages, "WCSDocumentation",           errorInMethod);
        WMSDocumentation_s           = getMessageInAllVersions(messages, "WMSDocumentation",           errorInMethod);
        resultsFormatExamplesHtml_s  = getMessageInAllVersions(messages, "resultsFormatExamplesHtml",  errorInMethod);
        resultsOfSearchFor_s         = getMessageInAllVersions(messages, "resultsOfSearchFor",         errorInMethod);
        restfulInformationFormats_s  = getMessageInAllVersions(messages, "restfulInformationFormats",  errorInMethod);
        restfulViaService_s          = getMessageInAllVersions(messages, "restfulViaService",          errorInMethod);
        rows_s                       = getMessageInAllVersions(messages, "rows",                       errorInMethod);
        rssNo_s                      = getMessageInAllVersions(messages, "rssNo",                      errorInMethod);
        searchTitle_s                = getMessageInAllVersions(messages, "searchTitle",                errorInMethod);
        searchDoFullTextHtml_s       = getMessageInAllVersions(messages, "searchDoFullTextHtml",       errorInMethod);
        searchFullTextHtml_s         = getMessageInAllVersions(messages, "searchFullTextHtml",         errorInMethod);
        searchButton_s               = getMessageInAllVersions(messages, "searchButton",               errorInMethod);
        searchClickTip_s             = getMessageInAllVersions(messages, "searchClickTip",             errorInMethod);
        searchHintsLuceneTooltip_s   = getMessageInAllVersions(messages, "searchHintsLuceneTooltip",   errorInMethod);
        searchHintsOriginalTooltip_s = getMessageInAllVersions(messages, "searchHintsOriginalTooltip", errorInMethod);
        searchHintsTooltip_s         = getMessageInAllVersions(messages, "searchHintsTooltip",         errorInMethod);
        searchMultipleERDDAPs_s      = getMessageInAllVersions(messages, "searchMultipleERDDAPs",      errorInMethod);
        searchMultipleERDDAPsDescription_s = getMessageInAllVersions(messages, "searchMultipleERDDAPsDescription", errorInMethod);
        searchNotAvailable_s         = getMessageInAllVersions(messages, "searchNotAvailable",         errorInMethod);
        searchTip_s                  = getMessageInAllVersions(messages, "searchTip",                  errorInMethod);
        searchSpelling_s             = getMessageInAllVersions(messages, "searchSpelling",             errorInMethod);
        searchFewerWords_s           = getMessageInAllVersions(messages, "searchFewerWords",           errorInMethod);
        searchWithQuery_s            = getMessageInAllVersions(messages, "searchWithQuery",            errorInMethod);
        selectNext_s                 = getMessageInAllVersions(messages, "selectNext",                 errorInMethod);
        selectPrevious_s             = getMessageInAllVersions(messages, "selectPrevious",             errorInMethod);
        shiftXAllTheWayLeft_s        = getMessageInAllVersions(messages, "shiftXAllTheWayLeft",        errorInMethod);
        shiftXLeft_s                 = getMessageInAllVersions(messages, "shiftXLeft",                 errorInMethod);
        shiftXRight_s                = getMessageInAllVersions(messages, "shiftXRight",                errorInMethod);
        shiftXAllTheWayRight_s       = getMessageInAllVersions(messages, "shiftXAllTheWayRight",       errorInMethod);
        Attributes.signedToUnsignedAttNames = StringArray.arrayFromCSV(
                                     messages0.getNotNothingString("signedToUnsignedAttNames",   errorInMethod));
        seeProtocolDocumentation_s   = getMessageInAllVersions(messages, "seeProtocolDocumentation",   errorInMethod);
        for (int i = 0; i < messages.length; i++) {
            seeProtocolDocumentation_s[i]   = MessageFormat.format(seeProtocolDocumentation_s[i], "documentation.html");//so it isn't translated
        }
        sosDescriptionHtml_s         = getMessageInAllVersions(messages, "sosDescriptionHtml",         errorInMethod);
        sosLongDescriptionHtml_s     = getMessageInAllVersions(messages, "sosLongDescriptionHtml",     errorInMethod);
        sosOverview1_s               = getMessageInAllVersions(messages, "sosOverview1",               errorInMethod); 
        sosOverview2_s               = getMessageInAllVersions(messages, "sosOverview2",               errorInMethod);  
        sparqlP01toP02pre          = messages0.getNotNothingString("sparqlP01toP02pre",          errorInMethod); 
        sparqlP01toP02post         = messages0.getNotNothingString("sparqlP01toP02post",         errorInMethod); 
        ssUse_s                      = getMessageInAllVersions(messages, "ssUse",                      errorInMethod);
        ssUsePlain_s = new String[messages.length];
        for (int i = 0; i < messages.length; i++){
            ssUsePlain_s[i] = XML.removeHTMLTags(ssUse_s[i]);
        }
        ssBePatient_s                = getMessageInAllVersions(messages, "ssBePatient",                errorInMethod);
        ssInstructionsHtml_s         = getMessageInAllVersions(messages, "ssInstructionsHtml",         errorInMethod);

        statusHtml_s                 = getMessageInAllVersions(messages, "statusHtml",                 errorInMethod);
        submit_s                     = getMessageInAllVersions(messages, "submit",                     errorInMethod);
        submitTooltip_s              = getMessageInAllVersions(messages, "submitTooltip",              errorInMethod);
        subscriptionRSSHTML_s        = getMessageInAllVersions(messages, "subscriptionRSSHTML",        errorInMethod);
        subscriptionURLHTML_s        = getMessageInAllVersions(messages, "subscriptionURLHTML",        errorInMethod);
        subscriptionsTitle_s         = getMessageInAllVersions(messages, "subscriptionsTitle",         errorInMethod);
        subscriptionAdd_s            = getMessageInAllVersions(messages, "subscriptionAdd",            errorInMethod);
        subscriptionValidate_s       = getMessageInAllVersions(messages, "subscriptionValidate",       errorInMethod);
        subscriptionList_s           = getMessageInAllVersions(messages, "subscriptionList",           errorInMethod);
        subscriptionRemove_s         = getMessageInAllVersions(messages, "subscriptionRemove",         errorInMethod);
        subscription0Html_s          = getMessageInAllVersions(messages, "subscription0Html",          errorInMethod);
        subscription1Html_s          = getMessageInAllVersions(messages, "subscription1Html",          errorInMethod);
        subscription2Html_s          = getMessageInAllVersions(messages, "subscription2Html",          errorInMethod);
        subscriptionAbuse_s          = getMessageInAllVersions(messages, "subscriptionAbuse",          errorInMethod);
        subscriptionAddError_s       = getMessageInAllVersions(messages, "subscriptionAddError",       errorInMethod);
        subscriptionAddHtml_s        = getMessageInAllVersions(messages, "subscriptionAddHtml",        errorInMethod);
        subscriptionAdd2_s           = getMessageInAllVersions(messages, "subscriptionAdd2",           errorInMethod);
        subscriptionAddSuccess_s     = getMessageInAllVersions(messages, "subscriptionAddSuccess",     errorInMethod);
        subscriptionEmail_s          = getMessageInAllVersions(messages, "subscriptionEmail",          errorInMethod);
        subscriptionEmailOnBlacklist_s=getMessageInAllVersions(messages, "subscriptionEmailOnBlacklist",errorInMethod);
        subscriptionEmailInvalid_s   = getMessageInAllVersions(messages, "subscriptionEmailInvalid",   errorInMethod);
        subscriptionEmailTooLong_s   = getMessageInAllVersions(messages, "subscriptionEmailTooLong",   errorInMethod);
        subscriptionEmailUnspecified_s=getMessageInAllVersions(messages, "subscriptionEmailUnspecified",errorInMethod);
        subscriptionIDInvalid_s      = getMessageInAllVersions(messages, "subscriptionIDInvalid",      errorInMethod);
        subscriptionIDTooLong_s      = getMessageInAllVersions(messages, "subscriptionIDTooLong",      errorInMethod);
        subscriptionIDUnspecified_s  = getMessageInAllVersions(messages, "subscriptionIDUnspecified",  errorInMethod);
        subscriptionKeyInvalid_s     = getMessageInAllVersions(messages, "subscriptionKeyInvalid",     errorInMethod);
        subscriptionKeyUnspecified_s = getMessageInAllVersions(messages, "subscriptionKeyUnspecified", errorInMethod);
        subscriptionListError_s      = getMessageInAllVersions(messages, "subscriptionListError",      errorInMethod);
        subscriptionListHtml_s       = getMessageInAllVersions(messages, "subscriptionListHtml",       errorInMethod);
        subscriptionListSuccess_s    = getMessageInAllVersions(messages, "subscriptionListSuccess",    errorInMethod);
        subscriptionRemoveError_s    = getMessageInAllVersions(messages, "subscriptionRemoveError",    errorInMethod);
        subscriptionRemoveHtml_s     = getMessageInAllVersions(messages, "subscriptionRemoveHtml",     errorInMethod);
        subscriptionRemove2_s        = getMessageInAllVersions(messages, "subscriptionRemove2",        errorInMethod);
        subscriptionRemoveSuccess_s  = getMessageInAllVersions(messages, "subscriptionRemoveSuccess",  errorInMethod);
        subscriptionRSS_s            = getMessageInAllVersions(messages, "subscriptionRSS",            errorInMethod);
        subscriptionsNotAvailable_s  = getMessageInAllVersions(messages, "subscriptionsNotAvailable",  errorInMethod);
        subscriptionUrlHtml_s        = getMessageInAllVersions(messages, "subscriptionUrlHtml",        errorInMethod);
        subscriptionUrlInvalid_s     = getMessageInAllVersions(messages, "subscriptionUrlInvalid",     errorInMethod);
        subscriptionUrlTooLong_s     = getMessageInAllVersions(messages, "subscriptionUrlTooLong",     errorInMethod);
        subscriptionValidateError_s  = getMessageInAllVersions(messages, "subscriptionValidateError",  errorInMethod);
        subscriptionValidateHtml_s   = getMessageInAllVersions(messages, "subscriptionValidateHtml",   errorInMethod);
        subscriptionValidateSuccess_s= getMessageInAllVersions(messages, "subscriptionValidateSuccess",errorInMethod);
        subset_s                     = getMessageInAllVersions(messages, "subset",                     errorInMethod);
        subsetSelect_s               = getMessageInAllVersions(messages, "subsetSelect",               errorInMethod);
        subsetNMatching_s            = getMessageInAllVersions(messages, "subsetNMatching",            errorInMethod);
        subsetInstructions_s         = getMessageInAllVersions(messages, "subsetInstructions",         errorInMethod);
        subsetOption_s               = getMessageInAllVersions(messages, "subsetOption",               errorInMethod);
        subsetOptions_s              = getMessageInAllVersions(messages, "subsetOptions",              errorInMethod);
        subsetRefineMapDownload_s    = getMessageInAllVersions(messages, "subsetRefineMapDownload",    errorInMethod);
        subsetRefineSubsetDownload_s = getMessageInAllVersions(messages, "subsetRefineSubsetDownload", errorInMethod);
        subsetClickResetClosest_s    = getMessageInAllVersions(messages, "subsetClickResetClosest",    errorInMethod);
        subsetClickResetLL_s         = getMessageInAllVersions(messages, "subsetClickResetLL",         errorInMethod);
        subsetMetadata_s             = getMessageInAllVersions(messages, "subsetMetadata",             errorInMethod);
        subsetCount_s                = getMessageInAllVersions(messages, "subsetCount",                errorInMethod);
        subsetPercent_s              = getMessageInAllVersions(messages, "subsetPercent",              errorInMethod);
        subsetViewSelect_s           = getMessageInAllVersions(messages, "subsetViewSelect",           errorInMethod);
        subsetViewSelectDistinctCombos_s= getMessageInAllVersions(messages, "subsetViewSelectDistinctCombos",errorInMethod);
        subsetViewSelectRelatedCounts_s = getMessageInAllVersions(messages, "subsetViewSelectRelatedCounts", errorInMethod);
        subsetWhen_s                 = getMessageInAllVersions(messages, "subsetWhen",                 errorInMethod);
        subsetWhenNoConstraints_s    = getMessageInAllVersions(messages, "subsetWhenNoConstraints",    errorInMethod);
        subsetWhenCounts_s           = getMessageInAllVersions(messages, "subsetWhenCounts",           errorInMethod);
        subsetComboClickSelect_s     = getMessageInAllVersions(messages, "subsetComboClickSelect",     errorInMethod);
        subsetNVariableCombos_s      = getMessageInAllVersions(messages, "subsetNVariableCombos",      errorInMethod);
        subsetShowingAllRows_s       = getMessageInAllVersions(messages, "subsetShowingAllRows",       errorInMethod);
        subsetShowingNRows_s         = getMessageInAllVersions(messages, "subsetShowingNRows",         errorInMethod);
        subsetChangeShowing_s        = getMessageInAllVersions(messages, "subsetChangeShowing",        errorInMethod);
        subsetNRowsRelatedData_s     = getMessageInAllVersions(messages, "subsetNRowsRelatedData",     errorInMethod);
        subsetViewRelatedChange_s    = getMessageInAllVersions(messages, "subsetViewRelatedChange",    errorInMethod);
        subsetTotalCount_s           = getMessageInAllVersions(messages, "subsetTotalCount",           errorInMethod);
        subsetView_s                 = getMessageInAllVersions(messages, "subsetView",                 errorInMethod);
        subsetViewCheck_s            = getMessageInAllVersions(messages, "subsetViewCheck",            errorInMethod);
        subsetViewCheck1_s           = getMessageInAllVersions(messages, "subsetViewCheck1",           errorInMethod);
        subsetViewDistinctMap_s      = getMessageInAllVersions(messages, "subsetViewDistinctMap",      errorInMethod);
        subsetViewRelatedMap_s       = getMessageInAllVersions(messages, "subsetViewRelatedMap",       errorInMethod);
        subsetViewDistinctDataCounts_s=getMessageInAllVersions(messages, "subsetViewDistinctDataCounts",errorInMethod);
        subsetViewDistinctData_s     = getMessageInAllVersions(messages, "subsetViewDistinctData",     errorInMethod);
        subsetViewRelatedDataCounts_s= getMessageInAllVersions(messages, "subsetViewRelatedDataCounts",errorInMethod);
        subsetViewRelatedData_s      = getMessageInAllVersions(messages, "subsetViewRelatedData",      errorInMethod);
        subsetViewDistinctMapTooltip_s       = getMessageInAllVersions(messages, "subsetViewDistinctMapTooltip",       errorInMethod);
        subsetViewRelatedMapTooltip_s        = getMessageInAllVersions(messages, "subsetViewRelatedMapTooltip",        errorInMethod);
        subsetViewDistinctDataCountsTooltip_s= getMessageInAllVersions(messages, "subsetViewDistinctDataCountsTooltip",errorInMethod);
        subsetViewDistinctDataTooltip_s      = getMessageInAllVersions(messages, "subsetViewDistinctDataTooltip",      errorInMethod);
        subsetViewRelatedDataCountsTooltip_s = getMessageInAllVersions(messages, "subsetViewRelatedDataCountsTooltip", errorInMethod);
        subsetViewRelatedDataTooltip_s       = getMessageInAllVersions(messages, "subsetViewRelatedDataTooltip",       errorInMethod);
        subsetWarn_s                 = getMessageInAllVersions(messages, "subsetWarn",                 errorInMethod);                    
        subsetWarn10000_s            = getMessageInAllVersions(messages, "subsetWarn10000",            errorInMethod);
        subsetTooltip_s              = getMessageInAllVersions(messages, "subsetTooltip",              errorInMethod);
        subsetNotSetUp_s             = getMessageInAllVersions(messages, "subsetNotSetUp",             errorInMethod);
        subsetLongNotShown_s         = getMessageInAllVersions(messages, "subsetLongNotShown",         errorInMethod);

        tabledapVideoIntro_s         = getMessageInAllVersions(messages, "tabledapVideoIntro",         errorInMethod);
        theLongDescriptionHtml_s     = getMessageInAllVersions(messages, "theLongDescriptionHtml",     errorInMethod);
        time_s                       = getMessageInAllVersions(messages, "time",                       errorInMethod);
        Then_s                       = getMessageInAllVersions(messages, "Then",                       errorInMethod);
        timeoutOtherRequests_s       = getMessageInAllVersions(messages, "timeoutOtherRequests",       errorInMethod);

        units_s                      = getMessageInAllVersions(messages, "units",                      errorInMethod);
        unknownDatasetID_s           = getMessageInAllVersions(messages, "unknownDatasetID",           errorInMethod);
        unknownProtocol_s            = getMessageInAllVersions(messages, "unknownProtocol",            errorInMethod);
        unsupportedFileType_s        = getMessageInAllVersions(messages, "unsupportedFileType",        errorInMethod);
        String tStandardizeUdunits[] = String2.split(
                                     messages0.getNotNothingString("standardizeUdunits",         errorInMethod) + "\n", '\n'); // +\n\n since xml content is trimmed.
        String tUcumToUdunits[]    = String2.split(
                                     messages0.getNotNothingString("ucumToUdunits",              errorInMethod) + "\n", '\n'); // +\n\n since xml content is trimmed.
        String tUdunitsToUcum[]    = String2.split(
                                     messages0.getNotNothingString("udunitsToUcum",              errorInMethod) + "\n", '\n'); // +\n\n since xml content is trimmed.
        String tUpdateUrls[]       = String2.split(
                                     messages0.getNotNothingString("updateUrls",                 errorInMethod) + "\n", '\n'); // +\n\n since xml content is trimmed.

        updateUrlsSkipAttributes   = StringArray.arrayFromCSV(
                                     messages0.getNotNothingString("updateUrlsSkipAttributes",   errorInMethod));
        
        variableNames_s              = getMessageInAllVersions(messages, "variableNames",              errorInMethod);
        viewAllDatasetsHtml_s        = getMessageInAllVersions(messages, "viewAllDatasetsHtml",        errorInMethod);
        waitThenTryAgain           = messages0.getNotNothingString("waitThenTryAgain",           errorInMethod);
        gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.waitThenTryAgain = waitThenTryAgain;
        warning_s                    = getMessageInAllVersions(messages, "warning",                    errorInMethod);
        wcsDescriptionHtml_s         = getMessageInAllVersions(messages, "wcsDescriptionHtml",         errorInMethod);
        wcsLongDescriptionHtml_s     = getMessageInAllVersions(messages, "wcsLongDescriptionHtml",     errorInMethod);
        wcsOverview1_s               = getMessageInAllVersions(messages, "wcsOverview1",               errorInMethod);
        wcsOverview2_s               = getMessageInAllVersions(messages, "wcsOverview2",               errorInMethod);
        wmsDescriptionHtml_s         = getMessageInAllVersions(messages, "wmsDescriptionHtml",         errorInMethod);
        wmsInstructions_s            = getMessageInAllVersions(messages, "wmsInstructions",            errorInMethod); 
        wmsLongDescriptionHtml_s     = getMessageInAllVersions(messages, "wmsLongDescriptionHtml",     errorInMethod); 
        wmsManyDatasets_s            = getMessageInAllVersions(messages, "wmsManyDatasets",            errorInMethod); 
        WMSDocumentation1_s          = getMessageInAllVersions(messages, "WMSDocumentation1",          errorInMethod);
        WMSGetCapabilities_s         = getMessageInAllVersions(messages, "WMSGetCapabilities",         errorInMethod);
        WMSGetMap_s                  = getMessageInAllVersions(messages, "WMSGetMap",                  errorInMethod);
        WMSNotes_s                   = getMessageInAllVersions(messages, "WMSNotes",                   errorInMethod);

        zoomIn_s                     = getMessageInAllVersions(messages, "zoomIn",                     errorInMethod); 
        zoomOut_s                    = getMessageInAllVersions(messages, "zoomOut",                    errorInMethod); 
        
        blacklistMsg = MessageFormat.format(blacklistMsg, adminEmail);    
        
        standardShortDescriptionHtml_s = getMessageInAllVersions(messages, "standardShortDescriptionHtml",errorInMethod);
        for (int i = 0; i < messages.length; i++) {
            standardShortDescriptionHtml_s[i] = String2.replaceAll(standardShortDescriptionHtml_s[i], "&convertTimeReference;", convertersActive? convertTimeReference_s[i] : "");
            standardShortDescriptionHtml_s[i] = String2.replaceAll(standardShortDescriptionHtml_s[i], "&wmsManyDatasets;", wmsActive? wmsManyDatasets_s[i] : "");    
        }
        DEFAULT_commonStandardNames        = String2.canonical(StringArray.arrayFromCSV(
                                             messages0.getNotNothingString("DEFAULT_commonStandardNames",errorInMethod)));
                commonStandardNames        = DEFAULT_commonStandardNames;
        DEFAULT_standardLicense_s            = getMessageInAllVersions(messages, "standardLicense",            errorInMethod);
        DEFAULT_standardContact_s            = getMessageInAllVersions(messages, "standardContact",            errorInMethod);
        DEFAULT_standardDataLicenses_s       = getMessageInAllVersions(messages, "standardDataLicenses",       errorInMethod);
        DEFAULT_standardDisclaimerOfExternalLinks_s=getMessageInAllVersions(messages, "standardDisclaimerOfExternalLinks", errorInMethod);
        DEFAULT_standardDisclaimerOfEndorsement_s  =getMessageInAllVersions(messages, "standardDisclaimerOfEndorsement",   errorInMethod);
        DEFAULT_standardGeneralDisclaimer_s  = getMessageInAllVersions(messages, "standardGeneralDisclaimer",  errorInMethod);
        DEFAULT_standardPrivacyPolicy_s      = getMessageInAllVersions(messages, "standardPrivacyPolicy",      errorInMethod);
        DEFAULT_startHeadHtml_s              = getMessageInAllVersions(messages, "startHeadHtml5",           errorInMethod);
        DEFAULT_startBodyHtml_s              = getMessageInAllVersions(messages, "startBodyHtml5",           errorInMethod);
        DEFAULT_theShortDescriptionHtml_s    = getMessageInAllVersions(messages, "theShortDescriptionHtml",  errorInMethod);
        DEFAULT_endBodyHtml_s                = getMessageInAllVersions(messages, "endBodyHtml5",             errorInMethod);


        standardLicense_s = new String[messages.length];
        standardContact_s = new String[messages.length];
        standardContact_s = new String[messages.length];
        standardDataLicenses_s = new String[messages.length];
        standardDisclaimerOfExternalLinks_s = new String[messages.length];
        standardDisclaimerOfEndorsement_s = new String[messages.length];
        standardGeneralDisclaimer_s = new String[messages.length];
        standardPrivacyPolicy_s = new String[messages.length];
        startHeadHtml_s = new String[messages.length];
        startBodyHtml_s = new String[messages.length];
        theShortDescriptionHtml_s = new String[messages.length];
        endBodyHtml_s = new String[messages.length];
        ampLoginInfoPo_s = new int[messages.length];

        for (int i = 0; i < messages.length; i++) {
            standardLicense_s[i]            = getSetupEVString(setup, ev,  "standardLicense",            DEFAULT_standardLicense_s[i]);
            standardContact_s[i]            = getSetupEVString(setup, ev,  "standardContact",            DEFAULT_standardContact_s[i]);
            standardContact_s[i]            = String2.replaceAll(standardContact_s[i], "&adminEmail;", SSR.getSafeEmailAddress(adminEmail));
            standardDataLicenses_s[i]       = getSetupEVString(setup, ev,  "standardDataLicenses",       DEFAULT_standardDataLicenses_s[i]);
            standardDisclaimerOfExternalLinks_s[i]=getSetupEVString(setup, ev,  "standardDisclaimerOfExternalLinks", DEFAULT_standardDisclaimerOfExternalLinks_s[i]);
            standardDisclaimerOfEndorsement_s[i]  =getSetupEVString(setup, ev,  "standardDisclaimerOfEndorsement",   DEFAULT_standardDisclaimerOfEndorsement_s[i]);
            standardGeneralDisclaimer_s[i]  = getSetupEVString(setup, ev,  "standardGeneralDisclaimer",  DEFAULT_standardGeneralDisclaimer_s[i]);
            standardPrivacyPolicy_s[i]      = getSetupEVString(setup, ev,  "standardPrivacyPolicy",      DEFAULT_standardPrivacyPolicy_s[i]);

            DEFAULT_startHeadHtml_s[i]              = DEFAULT_startHeadHtml_s[i].replace("&langCode;", langCode_s[i]);
                startHeadHtml_s[i]              = getSetupEVString(setup, ev,  "startHeadHtml5",           DEFAULT_startHeadHtml_s[i]);
                startBodyHtml_s[i]              = getSetupEVString(setup, ev,  "startBodyHtml5",           DEFAULT_startBodyHtml_s[i]);
            ampLoginInfoPo_s[i] = startBodyHtml_s[i].indexOf(ampLoginInfo); 
        
                theShortDescriptionHtml_s[i]    = getSetupEVString(setup, ev,  "theShortDescriptionHtml",  DEFAULT_theShortDescriptionHtml_s[i]);
                theShortDescriptionHtml_s[i]    = String2.replaceAll(theShortDescriptionHtml_s[i], "[standardShortDescriptionHtml]", standardShortDescriptionHtml_s[i]);
                theShortDescriptionHtml_s[i]    = String2.replaceAll(theShortDescriptionHtml_s[i], "&requestFormatExamplesHtml;",    requestFormatExamplesHtml_s[i]);
                endBodyHtml_s[i]                = getSetupEVString(setup, ev,  "endBodyHtml5",             DEFAULT_endBodyHtml_s[i]);
                endBodyHtml_s[i]                = String2.replaceAll(endBodyHtml_s[i], "&erddapVersion;", erddapVersion);
        
        //ensure HTML5
        Test.ensureTrue(startHeadHtml_s[i].startsWith("<!DOCTYPE html>"),
            "<startHeadHtml> must start with \"<!DOCTYPE html>\".");
        }
        Test.ensureEqual(imageWidths.length,  3, "imageWidths.length must be 3.");
        Test.ensureEqual(imageHeights.length, 3, "imageHeights.length must be 3.");
        Test.ensureEqual(pdfWidths.length,    3, "pdfWidths.length must be 3.");
        Test.ensureEqual(pdfHeights.length,   3, "pdfHeights.length must be 3.");

        int nStandardizeUdunits = tStandardizeUdunits.length / 3;
        for (int i = 0; i < nStandardizeUdunits; i++) {
            int i3 = i * 3;
            Test.ensureTrue( String2.isSomething(tStandardizeUdunits[ i3]),   "standardizeUdunits line #" + (i3 + 0) + " is empty.");
            Test.ensureTrue( String2.isSomething(tStandardizeUdunits[ i3+1]), "standardizeUdunits line #" + (i3 + 1) + " is empty.");
            Test.ensureEqual(tStandardizeUdunits[i3+2].trim(), "",            "standardizeUdunits line #" + (i3 + 2) + " isn't empty.");
            Units2.standardizeUdunitsHM.put(
                String2.canonical(tStandardizeUdunits[i3].trim()), 
                String2.canonical(tStandardizeUdunits[i3 + 1].trim()));
        }       

        int nUcumToUdunits = tUcumToUdunits.length / 3;
        for (int i = 0; i < nUcumToUdunits; i++) {
            int i3 = i * 3;
            Test.ensureTrue( String2.isSomething(tUcumToUdunits[ i3]),   "ucumToUdunits line #" + (i3 + 0) + " is empty.");
            Test.ensureTrue( String2.isSomething(tUcumToUdunits[ i3+1]), "ucumToUdunits line #" + (i3 + 1) + " is empty.");
            Test.ensureEqual(tUcumToUdunits[i3+2].trim(), "",            "ucumToUdunits line #" + (i3 + 2) + " isn't empty.");
            Units2.ucumToUdunitsHM.put(
                String2.canonical(tUcumToUdunits[i3].trim()), 
                String2.canonical(tUcumToUdunits[i3 + 1].trim()));
        }       

        int nUdunitsToUcum = tUdunitsToUcum.length / 3;
        for (int i = 0; i < nUdunitsToUcum; i++) {
            int i3 = i * 3;
            Test.ensureTrue( String2.isSomething(tUdunitsToUcum[ i3]),   "udunitsToUcum line #" + (i3 + 0) + " is empty.");
            Test.ensureTrue( String2.isSomething(tUdunitsToUcum[ i3+1]), "udunitsToUcum line #" + (i3 + 1) + " is empty.");
            Test.ensureEqual(tUdunitsToUcum[i3+2].trim(), "",            "udunitsToUcum line #" + (i3 + 2) + " isn't empty.");
            Units2.udunitsToUcumHM.put(
                String2.canonical(tUdunitsToUcum[i3].trim()), 
                String2.canonical(tUdunitsToUcum[i3 + 1].trim()));
        }       


        int nUpdateUrls = tUpdateUrls.length / 3;
        updateUrlsFrom = new String[nUpdateUrls];
        updateUrlsTo   = new String[nUpdateUrls];
        for (int i = 0; i < nUpdateUrls; i++) {
            int i3 = i * 3;
            updateUrlsFrom[i] = String2.canonical(tUpdateUrls[i3].trim());
            updateUrlsTo[  i] = String2.canonical(tUpdateUrls[i3 + 1].trim());
            Test.ensureTrue( String2.isSomething(tUpdateUrls[ i3]),   "updateUrls line #" + (i3 + 0) + " is empty.");
            Test.ensureTrue( String2.isSomething(tUpdateUrls[ i3+1]), "updateUrls line #" + (i3 + 1) + " is empty.");
            Test.ensureEqual(tUpdateUrls[i3+2].trim(), "",            "updateUrls line #" + (i3 + 0) + " isn't empty.");
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
        NetcdfFormatWriter ncWriter = null; 
        try {
            NetcdfFormatWriter.Builder nc = NetcdfFormatWriter.createNewNetcdf4(
                NetcdfFileFormat.NETCDF4, testNc4Name, null); //null=default chunker
            Group.Builder rootGroup = nc.getRootGroup();
            nc.setFill(false);
        
            int nRows = 4;
            Dimension dimension = NcHelper.addDimension(rootGroup, "row", nRows);
            Variable.Builder var = NcHelper.addVariable(rootGroup, "myLongs", 
                NcHelper.getNc3DataType(PAType.LONG),
                Arrays.asList(dimension)); 

            //leave "define" mode
            ncWriter = nc.build();  //error is thrown here if netcdf-c not found

            //write the data
            Array array = Array.factory(DataType.LONG, new int[]{nRows}, new long[]{0,1,2,3});
            ncWriter.write(var.getFullName(), new int[]{0}, array);

            //if close throws Throwable, it is trouble
            ncWriter.close(); //it calls flush() and doesn't like flush called separately
            ncWriter = null;

            //success!
            accessibleViaNC4 = "";
            String2.log(".nc4 files can be created in this ERDDAP installation.");

        } catch (Throwable t) {
            accessibleViaNC4 = String2.canonical(
                MessageFormat.format(noXxxBecause2, ".nc4",
                    resourceNotFound + "netcdf-c library"));
            String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + t.toString() + "\n" + accessibleViaNC4);

        } finally {
            if (ncWriter != null) {
                try {ncWriter.abort(); } catch (Exception e9) {}
                File2.delete(testNc4Name); 
                ncWriter = null;
            }
        }
//        File2.delete(testNc4Name);
*/
    for (int i = 0; i < messages.length; i++) {
            
        searchHintsTooltip_s[i] = 
            "<div class=\"standard_max_width\">" +
            searchHintsTooltip_s[i] + "\n" +
            (useLuceneSearchEngine? searchHintsLuceneTooltip_s[i] : searchHintsOriginalTooltip_s[i]) +
            "</div>";
        advancedSearchDirections_s[i] = String2.replaceAll(advancedSearchDirections_s[i], "&searchButton;", searchButton_s[i]);

        String tEmail = SSR.getSafeEmailAddress(adminEmail);
        filesDocumentation_s[i] = String2.replaceAll(filesDocumentation_s[i], "&adminEmail;", tEmail); 

        loginProblems_s[i]      = String2.replaceAll(loginProblems_s[i],      "&cookiesHelp;",   cookiesHelp_s[i]);
        loginProblems_s[i]      = String2.replaceAll(loginProblems_s[i],      "&adminContact;",  adminContact()) + "\n\n"; 
        loginProblemsAfter_s[i] = String2.replaceAll(loginProblemsAfter_s[i], "&adminContact;",  adminContact()) + "\n\n"; 
        loginPublicAccess_s[i] += "\n"; 
        logoutSuccess_s[i] += "\n"; 

        doWithGraphs_s[i] = String2.replaceAll(doWithGraphs_s[i], "&ssUse;", slideSorterActive? ssUse_s[i] : "");

        theLongDescriptionHtml_s[i] = String2.replaceAll(theLongDescriptionHtml_s[i], "&ssUse;", slideSorterActive? ssUse_s[i] : "");
        theLongDescriptionHtml_s[i] = String2.replaceAll(theLongDescriptionHtml_s[i], "&requestFormatExamplesHtml;", requestFormatExamplesHtml_s[i]);
        theLongDescriptionHtml_s[i] = String2.replaceAll(theLongDescriptionHtml_s[i], "&resultsFormatExamplesHtml;", resultsFormatExamplesHtml_s[i]);
    }

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


    updateLangChoice(0);

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
     * This gets a string from setup.xml or environmentalVariables (preferred).
     * 
     * @param setup from setup.xml
     * @param ev from System.getenv()
     * @param paramName  If present in ev, it will be ERDDAP_paramName.
     * @param tDefault the default value
     * @return the desired value (or the default if it isn't defined anywhere)
     */
    private static String getSetupEVString(ResourceBundle2 setup, Map<String,String>ev, 
        String paramName, String tDefault) {
        String value = ev.get("ERDDAP_" + paramName);
        if (String2.isSomething(value)) {
            String2.log("got " + paramName + " from ERDDAP_" + paramName);
            return value;
        }
        return setup.getString(paramName, tDefault);
    }

        /**
     * This will read translated tags from messages.xml and all messages-langCode.xml
     * @param messages The Array of ResourceBundle2 objects of messages.xml files
     * @param tagName The tagName of the corresponding text
     * @param errorInMethod The start of an Error message
     */
    private static String[] getMessageInAllVersions(ResourceBundle2[] messages, String tagName, String errorInMethod) {
        String[] res = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            res[i] = messages[i].getNotNothingString(tagName, errorInMethod);
        }
        return res;
    }
    
    /**
     * This will read translated tags from messages.xml and all messages-langCode.xml, with some modifications
     * @param messages The Array of ResourceBundle2 objects of messages.xml files
     * @param tagName The tagName of the corresponding text
     * @param appendFront Text to add at the beginning of the message
     * @param appendEnd Text to add at the end of the message
     * @param errorInMethod The start of an Error message
     */
    private static String[] getMessageInAllVersions(ResourceBundle2[] messages, String tagName,
            String appendFront, String appendEnd,String errorInMethod) {
        String[] res = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            res[i] = appendFront + messages[i].getNotNothingString(tagName, errorInMethod) + appendEnd;
        }
        return res;
    }

    /**
     * This gets a boolean from setup.xml or environmentalVariables (preferred).
     * 
     * @param setup from setup.xml
     * @param ev from System.getenv()
     * @param paramName  If present in ev, it will be ERDDAP_paramName.
     * @param tDefault the default value
     * @return the desired value (or the default if it isn't defined anywhere)
     */
    private static boolean getSetupEVBoolean(ResourceBundle2 setup, Map<String,String>ev, 
        String paramName, boolean tDefault) {
        String value = ev.get(paramName);
        if (value != null) {
            String2.log("got " + paramName + " from ERDDAP_" + paramName);
            return String2.parseBoolean(value);
        }
        return setup.getBoolean(paramName, tDefault);
    }

    /**
     * This gets an int from setup.xml or environmentalVariables (preferred).
     * 
     * @param setup from setup.xml
     * @param ev from System.getenv()
     * @param paramName  If present in ev, it will be ERDDAP_paramName.
     * @param tDefault the default value
     * @return the desired value (or the default if it isn't defined anywhere)
     */
    private static int getSetupEVInt(ResourceBundle2 setup, Map<String,String>ev, 
        String paramName, int tDefault) {
        String value = ev.get(paramName);
        if (value != null) {
            int valuei = String2.parseInt(value);
            if (valuei < Integer.MAX_VALUE) {
                String2.log("got " + paramName + " from ERDDAP_" + paramName);
                return valuei;
            }
        }
        return setup.getInt(paramName, tDefault);
    }

    /**
     * This gets a string from setup.xml or environmentalVariables (preferred).
     * 
     * @param setup from setup.xml
     * @param ev from System.getenv()
     * @param paramName  If present in ev, it will be ERDDAP_paramName.
     * @param errorInMethod the start of an Error message
     * @return the desired value
     * @throws RuntimeException if there is no value for key
     */
    private static String getSetupEVNotNothingString(ResourceBundle2 setup, Map<String,String>ev, 
        String paramName, String errorInMethod) {
        String value = ev.get("ERDDAP_" + paramName);
        if (String2.isSomething(value)) {
            String2.log("got " + paramName + " from ERDDAP_" + paramName);
            return value;
        }
        return setup.getNotNothingString(paramName, errorInMethod);
    }


    /** 
     * 'logLevel' determines how many diagnostic messages are sent to the log.txt file.
     * It can be set to "warning" (the fewest messages), "info" (the default), or "all" (the most messages). 
     * 
     * @param logLevel invalid becomes "info"
     * @return the valid value of logLevel
     */
    public static String setLogLevel(String logLevel) {
        if (!String2.isSomething(logLevel))
            logLevel = "info";
        logLevel = logLevel.toLowerCase();  
        if (!logLevel.equals("warning") &&
            !logLevel.equals("all"))
            logLevel = "info";

        verbose = !logLevel.equals("warning");
        AxisDataAccessor.verbose = verbose;
        Boundaries.verbose = verbose;
        Calendar2.verbose = verbose;
        EDD.verbose = verbose;
        EDV.verbose = verbose;      
        Erddap.verbose = verbose;
        File2.verbose = verbose;
        FileVisitorDNLS.reallyVerbose = reallyVerbose;
        FilledMarkerRenderer.verbose = verbose;
        gov.noaa.pfel.coastwatch.griddata.Grid.verbose = verbose;
        GridDataAccessor.verbose = verbose;
        GSHHS.verbose = verbose;
        LoadDatasets.verbose = verbose;
        NcHelper.verbose = verbose;
        OutputStreamFromHttpResponse.verbose = verbose;
        PathCartesianRenderer.verbose = verbose;
        PrimitiveArray.verbose = verbose;
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
        Units2.verbose = verbose;

        reallyVerbose = logLevel.equals("all");
        AxisDataAccessor.reallyVerbose = reallyVerbose;
        Boundaries.reallyVerbose = reallyVerbose;
        Calendar2.reallyVerbose = reallyVerbose;
        EDD.reallyVerbose = reallyVerbose;
        EDV.reallyVerbose = reallyVerbose;
        Erddap.reallyVerbose = reallyVerbose;
        File2.reallyVerbose = reallyVerbose;
        FileVisitorDNLS.reallyVerbose = reallyVerbose;
        FilledMarkerRenderer.reallyVerbose = reallyVerbose;
        GridDataAccessor.reallyVerbose = reallyVerbose;
        GSHHS.reallyVerbose = reallyVerbose;
        LoadDatasets.reallyVerbose = reallyVerbose;
        NcHelper.reallyVerbose = reallyVerbose;
        //OutputStreamFromHttpResponse.reallyVerbose = reallyVerbose;  currently no such setting
        PathCartesianRenderer.reallyVerbose = reallyVerbose;
        PrimitiveArray.reallyVerbose = reallyVerbose;
        //Projects.reallyVerbose = reallyVerbose;  currently no such setting
        SgtGraph.reallyVerbose = reallyVerbose;
        SgtMap.reallyVerbose = reallyVerbose;
        SgtUtil.reallyVerbose = reallyVerbose;
        SSR.reallyVerbose = reallyVerbose;
        Subscriptions.reallyVerbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;
        //Table.debug = reallyVerbose; //for debugging
        TaskThread.reallyVerbose = reallyVerbose;
        //Units2.reallyVerbose = reallyVerbose;  currently no such setting

        String2.log("logLevel=" + logLevel + ": verbose=" + verbose + " reallyVerbose=" + reallyVerbose);
        return logLevel;
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
        
        if (currLang == 0) {
            return loggedInAs == null? erddapUrl : erddapHttpsUrl;
        } else {
            return loggedInAs == null? erddapUrl + "/" + fullLanguageCodeList[currLang] : erddapHttpsUrl + "/" + fullLanguageCodeList[currLang];
        }
        //return loggedInAs == null? erddapUrl : erddapHttpsUrl;  //works because of loggedInAsHttps
    }

    /**
     * This determines if a URL points to this server (even in development).
     *
     * @param tUrl
     */
    public static boolean urlIsThisComputer(String tUrl) {
        return 
            tUrl.startsWith(baseUrl) ||  
            tUrl.startsWith(preferredErddapUrl) ||  //will be baseHttpsUrl if active
            urlIsLocalhost(tUrl);
    }

    /**
     * This determines if a URL points to this server (even in development).
     *
     * @param tUrl
     */
    public static boolean urlIsLocalhost(String tUrl) {
        if (!tUrl.startsWith("http"))
            return false;
        return 
            tUrl.startsWith("https://localhost") ||
            tUrl.startsWith("http://localhost")  ||
            tUrl.startsWith("https://127.0.0.1") ||
            tUrl.startsWith("http://127.0.0.1");
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
     * with the warning that the link is to an external website.
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
     * This returns the html documentation for acceptEncoding.
     *
     * @param headingType  e.g., h2 or h3
     * @param tErddapUrl
     * @return the html needed to document acceptEncodig.
     */
    public static String acceptEncodingHtml(String headingType, String tErddapUrl) {
        String s = String2.replaceAll(acceptEncodingHtml, "&headingType;",      headingType);
        return     String2.replaceAll(s,                  "&externalLinkHtml;", externalLinkHtml(tErddapUrl));
    }

    /** 
     * This returns the html documentation for the /files/ system.
     *
     * @param tErddapUrl
     * @return the html needed to document acceptEncodig.
     */
    public static String filesDocumentation(String tErddapUrl) {
        return String2.replaceAll(filesDocumentation, "&acceptEncodingHtml;", acceptEncodingHtml("h3", tErddapUrl));
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
            edv.destinationDataPAType(), 
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
            edvga.destinationDataPAType(), 
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
            edv.destinationDataPAType(), 
            edv.destinationName() + allDimString, 
            edv.combinedAttributes());
    }

    /**
     * This returns the html to draw a question mark that has big html tooltip
     * with a variable's name and attributes.
     * htmlTooltipScript (see HtmlWidgets) must be already in the document.
     *
     * @param destinationDataPAType
     * @param destinationName  perhaps with axis information appended (e.g., [time][latitude][longitude]
     * @param attributes
     */
    public static String htmlTooltipImageLowEDV(String loggedInAs, PAType destinationDataPAType, 
        String destinationName, Attributes attributes) throws Throwable {

        StringBuilder sb = OpendapHelper.dasToStringBuilder(
            OpendapHelper.getAtomicType(false, destinationDataPAType) + " " + destinationName,   //strictDapMode
            destinationDataPAType,
            attributes, false, false); //htmlEncoding, strictDapMode
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
     * <br>This method logs (to log.txt) that an email was sent: to whom and the subject, 
     *   but not the content.
     * <br>This method logs the entire email to the email log, e.g., 
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

        String emailAddressesCSSV = "";
        try {
            //ensure all email addresses are valid
            StringArray emailAddressesSA = new StringArray(emailAddresses);
            BitSet keep = new BitSet(emailAddressesSA.size());  //all false
            for (int i = 0; i < emailAddressesSA.size(); i++) { 
                String addr = emailAddressesSA.get(i);
                String err = subscriptions == null? //don't use EDStatic.subscriptionSystemActive for this test -- it's a separate issue
                    String2.testEmailAddress(addr) :     //tests syntax
                    subscriptions.testEmailValid(addr);  //tests syntax and blacklist             
                if (err.length() == 0) {
                    keep.set(i);
                } else {
                    String2.log("EDStatic.email caught an invalid email address: " + err);
                }
            }
            emailAddressesSA.justKeep(keep);  //it's okay if 0 remain. email will still be written to log below.
            emailAddresses = emailAddressesSA.toArray();

            //write the email to the log
            emailAddressesCSSV = String2.toCSSVString(emailAddresses);
            String localTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
            boolean logIt = !subject.startsWith(DONT_LOG_THIS_EMAIL);
            if (!logIt) 
                subject = subject.substring(DONT_LOG_THIS_EMAIL.length());
            subject = (computerName.length() > 0? computerName + " ": "") + "ERDDAP: " + 
                String2.replaceAll(subject, '\n', ' ');

            //almost always write to emailLog
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
"\nSubject: " + subject +  
"\n   Date: " + localTime + 
"\n--------------------------------------------------------------------------------" +
(logIt?
"\n" + preferredErddapUrl + " reports:" +  
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
//??? THREAD SAFE? SYNCHRONIZED? 
//I don't think use of this needs to be synchronized. I could be wrong. I haven't tested.
            SSR.sendEmail(emailSmtpHost, emailSmtpPort, emailUserName, 
                emailPassword, emailProperties, emailFromAddress, emailAddressesCSSV, 
                subject, 
                preferredErddapUrl + " reports:\n" + content);
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
            requestBlacklist = hs; //set atomically
            String2.log("requestBlacklist is now " + String2.toCSSVString(rb));
        }
    }

    /**
     * This tests if the ipAddress is on the blacklist (and calls sendLowError if it is).
     *
     * @param ipAddress the requester's ipAddress
     * @param requestNumber The requestNumber assigned to this request by doGet().
     * @param response so the response can be sent the error
     * @return true if user is on the blacklist.
     */
    public static boolean isOnBlacklist(String ipAddress, int requestNumber, HttpServletResponse response) {
        //refuse request? e.g., to fend of a Denial of Service attack or an overzealous web robot

        //for testing:
        //  int tr = Math2.random(3);
        //  ipAddress=tr==0? "101.2.34.56" : tr==1? "1:2:3:4:56:78" : "(unknownIPAddress)";

        int periodPo1 = ipAddress.lastIndexOf('.'); //to make #.#.#.* test below for IP v4 address
        boolean hasPeriod = periodPo1 > 0;
        if (!hasPeriod)
            periodPo1 = ipAddress.lastIndexOf(':'); //to make #:#:#:#:#:#:#:* test below for IP v6 address
        String ipAddress1 = periodPo1 <= 0? null : ipAddress.substring(0, periodPo1+1) + "*";
        int periodPo2 = ipAddress1 == null? -1 :   ipAddress.substring(0, periodPo1).lastIndexOf(hasPeriod? '.' : ':');
        String ipAddress2 = periodPo2 <= 0? null : ipAddress.substring(0, periodPo2+1) + (hasPeriod? "*.*" : "*:*");
        //String2.log(">> ipAddress=" + ipAddress + " ipAddress1=" + ipAddress1 + " ipAddress2=" + ipAddress2);
        if (requestBlacklist != null &&
            (requestBlacklist.contains(ipAddress) ||
             (ipAddress1 != null && requestBlacklist.contains(ipAddress1)) ||   //#.#.#.*
             (ipAddress2 != null && requestBlacklist.contains(ipAddress2)))) {  //#.#.*.*
            //use full ipAddress, to help id user                //odd capitilization sorts better
            tally.add("Requester's IP Address (Blacklisted) (since last Major LoadDatasets)", ipAddress);
            tally.add("Requester's IP Address (Blacklisted) (since last daily report)", ipAddress);
            tally.add("Requester's IP Address (Blacklisted) (since startup)", ipAddress);
            String2.log("}}}}#" + requestNumber + " Requester is on the datasets.xml requestBlacklist.");
            lowSendError(requestNumber, response, HttpServletResponse.SC_FORBIDDEN, //a.k.a. Error 403
                blacklistMsg);
            return true;
        }
        return false;
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
        sb.append("Unique users (since startup)                            n = " + ipAddressQueue.size() + "\n");
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

        synchronized(taskList) {
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
"Major LoadDatasets Time Series: MLD    Datasets Loaded            Requests (median times in ms)              Number of Threads      MB    Open\n" +
"  timestamp                    time   nTry nFail nTotal  nSuccess (median) nFail (median) memFail tooMany  tomWait inotify other  inUse  Files\n" +
"----------------------------  -----   -----------------  ------------------------------------------------  ---------------------  -----  -----\n"
);
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
        sb.append(String2.canonicalStatistics() + "\n");
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

        //request is via https, but authentication=""?  then can't be logged in
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
            authentication.equals("google") ||
            authentication.equals("orcid") ||
            authentication.equals("oauth2")) {
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
     * For "custom" authentication, this returns true if the plaintextPassword 
     * (after passwordEncoding as specified in setup.xml) 
     * matches the stored password for user.
     *
     * @param username the user's log in name
     * @param plaintextPassword that the user entered on a log-in form
     * @return true if the plaintextPassword (after passwordEncoding as 
     *    specified in setup.xml) matches the stored password for username.
     *    If user==null or user has no password defined in datasets.xml, this returns false.
     */
    public static boolean doesPasswordMatch(String username, String plaintextPassword) {
        if (username == null || plaintextPassword == null)
            return false;

        username = username.trim();
        plaintextPassword = plaintextPassword.trim();
        if (username.length() == 0 || !username.equals(String2.justPrintable(username))) {
            String2.log("username=" + username + " doesn't match basic requirements."); 
            return false;
        }
        if (plaintextPassword.length() < minimumPasswordLength ||
            !plaintextPassword.equals(String2.justPrintable(plaintextPassword))) {
            String2.log("plaintextPassword for username=" + username + " doesn't match basic requirements."); 
            return false;
        }

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
        //String2.log("username=" + username + " plaintextPassword=" + plaintextPassword +
        //    "\nobsPassword=" + observed + "\nexpPassword=" + expected);

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
     *    If user==null, this returns null.
     *    Anyone logged in automatically gets role=anyoneLoggedIn ("[anyoneLoggedIn]"). 
     */
    public static String[] getRoles(String loggedInAs) {
        if (loggedInAs == null ||
            loggedInAs == loggedInAsHttps)
            return null;

        //???future: for authentication="basic", use tomcat-defined roles???

        //all other authentication methods
        Object oar[] = (Object[])userHashMap.get(loggedInAs);
        if (oar == null)
            return anyoneLoggedInRoles; //no <user> tag, but still gets role=[anyoneLoggedIn]
        return (String[])oar[1];
    }

    /**
     * If the user tries to access a dataset to which he doesn't have access,
     * call this to send Http UNAUTHORIZED error.
     * (was: redirectToLogin: redirect him to the login page).
     *
     * @param requestNumber The requestNumber assigned to this request by doGet().
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetID  or use "" for general login.
     * @param graphsAccessibleToPublic From edd.graphsAccessibleToPublic(). 
     *   If this is true, then this method
     *   was called because the request was for data from a dataset that
     *   allows graphics|metadata requests from the public.
     * @throws Throwable (notably ClientAbortException)
     */
    public static void sendHttpUnauthorizedError(int requestNumber, String loggedInAs, 
        HttpServletResponse response, String datasetID, 
        boolean graphsAccessibleToPublic) throws Throwable {

        String message = "The user is not authorized to make that request."; //default
        try {
            tally.add("Request refused: not authorized (since startup)", datasetID); 
            tally.add("Request refused: not authorized (since last daily report)", datasetID);
            tally.add("Request refused: not authorized (since last Major LoadDatasets)", datasetID); 

            if (datasetID != null && datasetID.length() > 0) 
                message = MessageFormat.format(
                    graphsAccessibleToPublic?
                        EDStatic.notAuthorizedForData :
                        EDStatic.notAuthorized, 
                    loggedInAsHttps.equals(loggedInAs)? "" : loggedInAs, 
                    datasetID);

            lowSendError(requestNumber, response, HttpServletResponse.SC_UNAUTHORIZED, message);

        } catch (Throwable t2) {
            EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}
            String2.log("Error in sendHttpUnauthorizedError for request #" + requestNumber + ":\n" + 
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
        return startBodyHtml(loggedInAs, "");
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
     * @param otherBody other content for the &lt;body&gt; tag, e.g., " onload=\"myFunction()\"".
     *   
     */
    public static String startBodyHtml(String loggedInAs, String otherBody) {
        String s = startBodyHtml;
        if (String2.isSomething(otherBody)) 
            s = String2.replaceAll(s, "<body>", "<body " + otherBody + ">");
        if (ampLoginInfoPo_s[currLang] >= 0) {
            s = startBodyHtml.substring(0, ampLoginInfoPo_s[currLang]) +
                getLoginHtml(loggedInAs) +
                startBodyHtml.substring(ampLoginInfoPo_s[currLang] + ampLoginInfo.length());
        }
        //String2.log(">> EDStatic startBodyHtml=" + s);
        return String2.replaceAll(s, "&erddapUrl;", erddapUrl(loggedInAs));
    }

    /**
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     */
    public static String endBodyHtml(String tErddapUrl) {
        HtmlWidgets widget = new HtmlWidgets();
        return String2.replaceAll(endBodyHtml, "&erddapUrl;", tErddapUrl)
            .replace("&HTMLselect;",""
                // Erddap supports the changing the language mode by changing the url
                // changing it with a select needs additional configuration
                // "<form name=\"lang\">\n" + //no action
                // widget.select("language", "", 1, languageList, currLang, 
                //     ")")
                // + widget.button("submit", "LangSubmit", "", "Submit", "")
                // + widget.endForm()
            );
    }
    public static String legal(String tErddapUrl) {
        StringBuilder tsb = new StringBuilder(legal);
        String2.replaceAll(tsb, "[standardContact]",                   standardContact                   + "\n\n"); 
        String2.replaceAll(tsb, "[standardDataLicenses]",              standardDataLicenses              + "\n\n"); 
        String2.replaceAll(tsb, "[standardDisclaimerOfExternalLinks]", standardDisclaimerOfExternalLinks + "\n\n"); 
        String2.replaceAll(tsb, "[standardDisclaimerOfEndorsement]",   standardDisclaimerOfEndorsement   + "\n\n"); 
        String2.replaceAll(tsb, "[standardPrivacyPolicy]",             standardPrivacyPolicy             + "\n\n"); 
        String2.replaceAll(tsb, "&erddapUrl;",                         tErddapUrl); 
        return tsb.toString();
    }

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
        String message = MustBe.throwableToShortString(t);
        return 
            "<p>&nbsp;<hr>\n" +
            "<p><span class=\"warningColor\"><strong>" + errorOnWebPage + "</strong></span>\n" +
            "<pre>" + XML.encodeAsPreHTML(message, 100) +
            "</pre><hr><p>&nbsp;<p>\n";
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
            useLuceneSearchEngine = false;
            luceneIndexSearcher = null;
            try {
                if (luceneIndexReader   != null) luceneIndexReader.close();  
            } catch (Throwable t) {}
            luceneIndexReader = null;
            luceneDocNToDatasetID = null;

            try {
                if (luceneIndexWriter   != null) 
                    //indices will be thrown away, so don't make pending changes
                    luceneIndexWriter.close(); 
            } catch (Throwable t) {}
            luceneIndexWriter = null;

        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
    }


    /** This interrupts the thread and waits up to maxSeconds for it to finish.
     * If it still isn't finished, it is stopped.
     * 
     * @return true if it had to be actively stop()'ed.
     */
    public static boolean stopThread(Thread thread, int maxSeconds) {
        boolean stopped = false;
        try {
            if (thread == null)
                return false;
            String name = thread.getName();
            if (verbose) String2.log("stopThread(" + name + ")...");
            if (!thread.isAlive()) {
                if (verbose) String2.log("thread=" + name + " was already not alive.");
                return false;
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
                stopped = true;
            } else {
                if (verbose) String2.log("thread=" + name + " noticed interrupt in " + waitSeconds + " s");
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
        return stopped;
    }

    /**
     * This checks if the task thread is running and not stalled.
     * If it is stalled, this will stop it.
     *
     * @return true if the task thread is running.
     *    If false, taskThread will be null.
     */
    public static boolean isTaskThreadRunning() {
        synchronized(taskList) {
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
     * This ensures the task thread is running if there are tasks to do.
     * This won't throw an exception.
     */
    public static void ensureTaskThreadIsRunningIfNeeded() {
        synchronized(taskList) {
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
        synchronized(taskList) {

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
        ArrayList<String> lines = String2.readLinesFromFile(
            webInfParentDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/OceanicAtmosphericAcronyms.tsv",
            String2.ISO_8859_1, 1);
        int nLines = lines.size();
        for (int i = 1; i < nLines; i++) { //1 because skip colNames
            String s = lines.get(i).trim();
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
        ArrayList<String> lines = String2.readLinesFromFile(
            webInfParentDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/OceanicAtmosphericVariableNames.tsv",
            String2.ISO_8859_1, 1);
        int nLines = lines.size();
        for (int i = 1; i < nLines; i++) {
            String s = lines.get(i).trim();
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
            webInfParentDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/FipsCounty.tsv", 
            String2.ISO_8859_1, "", "",
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
            webInfParentDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/cfStdNames.txt");
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
            webInfParentDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/gcmdScienceKeywords.txt");
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
            webInfParentDirectory + "WEB-INF/classes/gov/noaa/pfel/erddap/util/CfToGcmd.txt");
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
        //ERDDAP consciously doesn't use any stopWords (words not included in the index, e.g. a, an, the)
        //1) this matches the behaviour of the original searchEngine
        //2) this is what users expect, e.g., when searching for a phrase
        //3) the content here isn't prose, so the stop words aren't nearly as common
        luceneAnalyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);  //the set of stopWords
        //it is important that the queryParser use the same analyzer as the indexWriter
        luceneQueryParser = new QueryParser(luceneDefaultField, luceneAnalyzer);
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

            //create indexWriter
            IndexWriterConfig lucConfig = 
                new IndexWriterConfig(luceneAnalyzer);
            lucConfig.setOpenMode(firstTime?
                IndexWriterConfig.OpenMode.CREATE :
                IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            lucConfig.setInfoStream(
                verbose? new PrintStream(new String2LogOutputStream()) : null); 

            luceneIndexWriter = new IndexWriter(luceneDirectory, lucConfig);
            String2.log("  createLuceneIndexWriter finished.  time=" +
                (System.currentTimeMillis() - tTime) + "ms");
        } catch (Throwable t) {
            useLuceneSearchEngine = false;
            throw new RuntimeException(t);
        }
    }

    /** 
     * This returns the Lucene IndexSearcher (thread-safe).
     * IndexSearch uses IndexReader (also thread-safe).
     * IndexReader works on a snapshot of an index, 
     * so it is recreated if flagged at end of LoadDatasetsevery 
     * via needNewLuceneIndexReader.
     *
     * @return a luceneIndexSearcher (thread-safe) or null if trouble.
     */
    public static IndexSearcher luceneIndexSearcher() {

        //synchronize 
        synchronized(luceneIndexReaderLock) {

            //need a new indexReader?
            //(indexReader is thread-safe, but only need one)
            if (luceneIndexReader == null || needNewLuceneIndexReader ||
                luceneIndexSearcher == null) {

                luceneIndexSearcher = null;
                if (luceneIndexReader != null) {
                    try {luceneIndexReader.close(); } catch (Throwable t2) {}
                    luceneIndexReader = null;
                }
                needNewLuceneIndexReader = true;

                //create a new one
                try {
                    long rTime = System.currentTimeMillis();
                    luceneIndexReader = DirectoryReader.open(luceneDirectory); // read-only=true
                    luceneIndexSearcher = new IndexSearcher(luceneIndexReader);
                    String2.log("  new luceneIndexReader+Searcher time=" + 
                        (System.currentTimeMillis() - rTime) + "ms");

                    //create the luceneDatasetIDFieldCache
                    //save memory by sharing the canonical strings  
                    //(EDD.ensureValid makes datasetID's canonical)
                    //rTime = System.currentTimeMillis();
                    //luceneDatasetIDFieldCache = FieldCache.DEFAULT.getStrings(luceneIndexReader, 
                    //    "datasetID");
                    //int n = luceneDatasetIDFieldCache.length;
                    //for (int i = 0; i < n; i++)
                    //    luceneDatasetIDFieldCache[i] = String2.canonical(luceneDatasetIDFieldCache[i]);
                    //String2.log("  new luceneDatasetIDFieldCache time=" + 
                    //    (System.currentTimeMillis() - rTime) + "ms");

                    //if successful, we no longer needNewLuceneIndexReader
                    needNewLuceneIndexReader = false;  
                    //if successful, fall through

                } catch (Throwable t) {
                    //this may occur before indexes have initially been created
                    //so don't give up on lucene
                    if (!initialLoadDatasets()) {
                        String subject = String2.ERROR + " while creating Lucene Searcher";
                        String msg = MustBe.throwableToString(t);
                        email(emailEverythingToCsv, subject, msg);
                        String2.log(subject + "\n" + msg);            
                    }

                    //clear out old one
                    luceneIndexSearcher = null;

                    if (luceneIndexReader != null) {
                        try {luceneIndexReader.close(); } catch (Throwable t2) {}
                        luceneIndexReader = null;
                    }
                    needNewLuceneIndexReader = true;

                    //return
                    return null;
                }
            }

            return luceneIndexSearcher;
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
        synchronized(luceneQueryParser) {

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
     *   If the !String2.isJsonpNameSafe(functionName), this throws a SimpleException.
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
     *    e.g., https://coastwatch.pfeg.noaa.gov/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=temperature%20wind
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

            String url0 = "&nbsp;<a ";
            String url1 = "href=\"" + 
                          XML.encodeAsHTMLAttribute(urlWithQuery.substring(0, pageNumberPo));  // + p
            String url2 = XML.encodeAsHTMLAttribute(urlWithQuery.substring(ampPo)) + "\">";    // + p   
            String url3 = "</a>&nbsp;\n";        
            String prev = "rel=\"prev\" ";
            String next = "rel=\"next\" ";
            String bmrk = "rel=\"bookmark\" ";

            //links, e.g. if page=5 and lastPage=12: _1 ... _4  5 _6 ... _12 
            StringBuilder sb = new StringBuilder();
            if (page >= 2)            sb.append(
                url0 + (page==2? prev : bmrk) +          url1 + 1          + url2 + 1          + url3);
            if (page >= 4)            sb.append(
                "...\n");
            if (page >= 3)            sb.append(
                url0 + (page>2? prev : bmrk) +           url1 + (page - 1) + url2 + (page - 1) + url3);
            sb.append("&nbsp;" + page + "&nbsp;(" + EDStatic.nMatchingCurrent + ")&nbsp;\n"); //always show current page
            if (page <= lastPage - 2) sb.append(
                url0 + (page<lastPage-1? next : bmrk) +  url1 + (page + 1) + url2 + (page + 1) + url3);
            if (page <= lastPage - 3) sb.append(
                "...\n");
            if (page <= lastPage - 1) sb.append(
                url0 + (page==lastPage-1? next : bmrk) + url1 + lastPage   + url2 + lastPage   + url3);

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
            if (pa != null && pa.size() > 0 && pa.elementType() == PAType.STRING) {
                String oValue = pa.getString(0);        
                String value = updateUrls(oValue);
                if (!value.equals(oValue))
                    addAtts.set(names[i], value); 
            }
        }
    }


    /**
     * Create tasks to download files so a local dir mimics a remote dir.
     * <br>This won't throw an exception.
     *
     * @param maxTasks This let's you just see what would happen (0), 
     *    or just make a limited or unlimited (Integer.MAX_VALUE) number
     *    of download tasks.
     * @param tDatasetID 
     * @param the number of files that will be downloaded
     */
    public static int makeCopyFileTasks(String tClassName, int maxTasks, 
        String tDatasetID, 
        String tSourceUrl, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tLocalDir) {

        tSourceUrl = File2.addSlash(tSourceUrl);
        tLocalDir  = File2.addSlash(tLocalDir);

        if (verbose) String2.log("* " + tDatasetID + " " + 
            tClassName + ".makeCopyFileTasks  pathRegex=" + tPathRegex + "  fileNameRegex=" + tFileNameRegex + "\n" +
            "from " + tSourceUrl + "\n" +
            "to   " + tLocalDir);
        long startTime = System.currentTimeMillis();
        int nFilesToDownload = 0;
        int lastTask = -1;

        try {
            //if previous tasks are still running, return
            ensureTaskThreadIsRunningIfNeeded();  //ensure info is up-to-date
            Integer datasetLastAssignedTask = (Integer)lastAssignedTask.get(tDatasetID);
            boolean pendingTasks = datasetLastAssignedTask != null &&  
                lastFinishedTask < datasetLastAssignedTask.intValue();
            if (verbose) 
                String2.log("  " + tClassName + 
                    ".makeCopyFileTasks: lastFinishedTask=" + lastFinishedTask + 
                    " < datasetLastAssignedTask(" + tDatasetID + ")=" + datasetLastAssignedTask + 
                    "? pendingTasks=" + pendingTasks);
            if (pendingTasks) 
                return 0;

            //make sure local dir exists or can be created
            File2.makeDirectory(tLocalDir); //throws exception if unable to comply

            //get remote file info
            Table remoteFiles = FileVisitorDNLS.oneStep(   //throws IOException if "Too many open files"
                tSourceUrl, tFileNameRegex, tRecursive, tPathRegex, false); //tDirectoriesToo
            if (remoteFiles.nRows() == 0) {
                if (verbose) String2.log("  " + tClassName + ".makeCopyFileTasks: no matching source files.");
                return 0;
            }
            remoteFiles.leftToRightSort(2); //should be already
            StringArray remoteDirs    = (StringArray)remoteFiles.getColumn(FileVisitorDNLS.DIRECTORY);
            StringArray remoteNames   = (StringArray)remoteFiles.getColumn(FileVisitorDNLS.NAME);
            LongArray   remoteLastMod =   (LongArray)remoteFiles.getColumn(FileVisitorDNLS.LASTMODIFIED);
            LongArray   remoteSize    =   (LongArray)remoteFiles.getColumn(FileVisitorDNLS.SIZE);

            //get local file info
            Table localFiles = FileVisitorDNLS.oneStep(   //throws IOException if "Too many open files"
                tLocalDir, tFileNameRegex, tRecursive, tPathRegex, false); //tDirectoriesToo
            localFiles.leftToRightSort(2); //should be already
            StringArray localDirs    = (StringArray)localFiles.getColumn(FileVisitorDNLS.DIRECTORY);
            StringArray localNames   = (StringArray)localFiles.getColumn(FileVisitorDNLS.NAME);
            LongArray   localLastMod =   (LongArray)localFiles.getColumn(FileVisitorDNLS.LASTMODIFIED);
            LongArray   localSize    =   (LongArray)localFiles.getColumn(FileVisitorDNLS.SIZE);

            //make tasks to download files
            boolean remoteErrorLogged = false;  //just display 1st offender
            boolean fileErrorLogged   = false;  //just display 1st offender
            int nRemote = remoteNames.size();
            int nLocal = localNames.size();
            int localI = 0; //next to look at
            for (int remoteI = 0; remoteI < nRemote; remoteI++) {
                try {
                    String remoteRelativeDir = remoteDirs.get(remoteI).substring(tSourceUrl.length());               

                    //skip local files with DIRS which are less than the remote file's dir
                    while (localI < nLocal &&
                           localDirs.get( localI).substring(tLocalDir.length()).compareTo(remoteRelativeDir) < 0) 
                        localI++;

                    //skip local files in same dir with FILENAMES which are less than the remote file
                    while (localI < nLocal &&
                           localDirs.get( localI).substring(tLocalDir.length()).equals(remoteRelativeDir) &&  
                           localNames.get(localI).compareTo(remoteNames.get(remoteI)) < 0) 
                        localI++;

                    //same local file exists?
                    String reason = "new remote file";
                    if (localI < nLocal &&
                        localDirs.get( localI).substring(tLocalDir.length()).equals(remoteRelativeDir) &&
                        localNames.get(localI).equals(remoteNames.get(remoteI))) {
                        //same or vague lastMod and size

                        if (remoteLastMod.get(remoteI) != Long.MAX_VALUE &&
                            remoteLastMod.get(remoteI) != localLastMod.get(localI)) {
                            //remoteLastMod may be imprecise (e.g., to the minute),
                            //but local should be set to exactly match it whatever it is
                            reason = "different lastModified";
                        } else if (remoteSize.get(remoteI) != Long.MAX_VALUE &&
                                   (remoteSize.get(remoteI) < localSize.get(localI) * 0.9 ||
                                    remoteSize.get(remoteI) > localSize.get(localI) * 1.1)) {
                            //remote size may be imprecise (e.g., 1.1MB)
                            //(also, does remote mean KB=1000 or 1024?!)
                            //so this just tests match within +/-10%
                            reason = "different size";
                        } else {
                            //local is ~equivalent of remote
                            localI++;
                            continue;
                        }
                        reason = "different size";
                    }

                    //make a task to download remoteFile to localFile
                    // taskOA[1]=remoteUrl, taskOA[2]=fullFileName, taskOA[3]=lastModified (Long)
                    Object taskOA[] = new Object[7];
                    taskOA[0] = TaskThread.TASK_DOWNLOAD;  
                    taskOA[1] = remoteDirs.get(remoteI) + remoteNames.get(remoteI);
                    taskOA[2] = tLocalDir + remoteRelativeDir + remoteNames.get(remoteI);
                    taskOA[3] = new Long(remoteLastMod.get(remoteI));  //or if unknown?
                    nFilesToDownload++;
                    int tTaskNumber = nFilesToDownload <= maxTasks? 
                        (lastTask = addTask(taskOA)) : -nFilesToDownload;                        
                    if (reallyVerbose || (verbose && nFilesToDownload == 1))
                        String2.log( 
                            (tTaskNumber < 0? "% didn't create" : "% created") +
                            " task#" + Math.abs(tTaskNumber) + " TASK_DOWNLOAD reason=" + reason +
                            "\n    from=" + taskOA[1] +
                            "\n    to=" + taskOA[2]);
                } catch (Exception e) {
                    String2.log(tClassName + ".makeCopyFileTasks caught " + String2.ERROR + 
                        " while processing file #" + remoteI + "=" +  
                        remoteDirs.get(remoteI) + remoteNames.get(remoteI) + "\n" +
                        MustBe.throwableToString(e));               
                }
            }

            //create task to flag dataset to be reloaded
            if (lastTask >= 0) {
                Object taskOA[] = new Object[2];
                taskOA[0] = TaskThread.TASK_SET_FLAG;
                taskOA[1] = tDatasetID;
                lastTask = addTask(taskOA); //TASK_SET_FLAG will always be added
                if (reallyVerbose)
                    String2.log("% created task#" + lastTask + " TASK_SET_FLAG " + tDatasetID);
                lastAssignedTask.put(tDatasetID, new Integer(lastTask));
                ensureTaskThreadIsRunningIfNeeded();  //ensure info is up-to-date
            }

            if (verbose) String2.log("% " + tDatasetID + " " + 
                tClassName + ".makeCopyFileTasks finished." +
                " nFilesToDownload=" + nFilesToDownload + " maxTasks=" + maxTasks +
                " time=" + (System.currentTimeMillis() - startTime) + "ms");


        } catch (Throwable t) {
            if (verbose)
                String2.log("ERROR in " + tClassName + 
                    ".makeCopyFileTasks for datasetID=" + tDatasetID + "\n" +
                    MustBe.throwableToString(t));
        }
        return nFilesToDownload;
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

    /**
     * Set the standard DAP header information. Call this before getting outputStream.
     *
     * @param response
     * @throws Throwable if trouble
     */
    public static void standardDapHeader(HttpServletResponse response) throws Throwable {
        String rfc822date = Calendar2.getCurrentRFC822Zulu();
        response.setHeader("Date", rfc822date);             //DAP 2.0, 7.1.4.1
        response.setHeader("Last-Modified", rfc822date);    //DAP 2.0, 7.1.4.2   //this is not a good implementation
        //response.setHeader("Server", );                   //DAP 2.0, 7.1.4.3  optional
        response.setHeader("xdods-server", serverVersion);  //DAP 2.0, 7.1.7 (http header field names are case-insensitive)
        response.setHeader(programname + "-server", erddapVersion);  
    }

    /**
     * This returns the requester's ip addresses (from x-forwarded-for)
     * or "(unknownIPAddress)".
     */
    public static String getIPAddress(HttpServletRequest request) {

        //getRemoteHost(); always returns our proxy server (never changes)
        String ipAddress = request.getHeader("x-forwarded-for");  
        if (ipAddress == null) {
            ipAddress = "";
        } else {
            //if csv, get last part
            //see https://en.wikipedia.org/wiki/X-Forwarded-For
            int cPo = ipAddress.lastIndexOf(',');
            if (cPo >= 0)
                ipAddress = ipAddress.substring(cPo + 1);
        }
        ipAddress = ipAddress.trim();
        if (ipAddress.length() == 0)
            ipAddress = ipAddressUnknown; 
        return ipAddress;
    }

    /**
     * Given a throwable t, this sends an appropriate HTTP error code and a DAP-formatted dods-error response message.
     * Most users will call return in their method after calling this since the response is committed and closed.
     *
     * @param requestNumber The requestNumber assigned to this request by doGet().
     * @param request The user's request.
     * @param response The response to be written to.
     */
    public static void sendError(int requestNumber, HttpServletRequest request, 
        HttpServletResponse response, Throwable t) throws ServletException {

        //defaults
        int errorNo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; //http error 500
        String tError = "Internal server error.";

        try {
            if (isClientAbortException(t)) {
                String2.log("*** sendError for request #" + requestNumber + " caught " + String2.ERROR + "=ClientAbortException");
                return; //do nothing
            }

            //String2.log("Bob: sendErrorCode t.toString=" + t.toString());        
            tError = MustBe.getShortErrorMessage(t);
            String tRequestURI = request == null? "[unknown requestURI]" : request.getRequestURI();
            String tExt = File2.getExtension(tRequestURI);
            String tRequest = tRequestURI + (request == null? "" : questionQuery(request.getQueryString()));
            //String2.log(">> tError=" + tError);

            //log the error            
            String tErrorLC = tError.toLowerCase();
            if (tError.indexOf(resourceNotFound) >= 0 ||
                tError.indexOf(MustBe.THERE_IS_NO_DATA) >= 0) { //check this first, since may also be Query error
                errorNo = HttpServletResponse.SC_NOT_FOUND;  //http error 404  (might succeed later)
                //I wanted to use 204 No Content or 205 (similar) but browsers don't show any change for these codes

            } else if (tError.indexOf(queryError) >= 0) {
                errorNo = HttpServletResponse.SC_BAD_REQUEST; //http error 400 (won't succeed later)

            } else if (tError.indexOf(REQUESTED_RANGE_NOT_SATISFIABLE) >= 0) {
                errorNo = HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE; //http error 416

            } else if (tError.indexOf(Math2.memoryArraySize.substring(0, 25)) >= 0) {
                errorNo = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE; //http error 413 (the old name for Payload Too Large), although it could be other user's requests that are too large
                String ipAddress = EDStatic.getIPAddress(request);
                tally.add("OutOfMemory (Array Size), IP Address (since last Major LoadDatasets)",  ipAddress);
                tally.add("OutOfMemory (Array Size), IP Address (since last daily report)",        ipAddress);
                tally.add("OutOfMemory (Array Size), IP Address (since startup)",                  ipAddress);

            } else if (tError.indexOf("OutOfMemoryError") >= 0 ||  //java's words
                       tError.indexOf(Math2.memoryThanCurrentlySafe.substring(0, 25)) >= 0) { //!!! TROUBLE: but that matches memoryThanSafe (in English) too!
                errorNo = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE; //http error 413 (the old name for Payload Too Large), although it could be other user's requests that are too large
                dangerousMemoryFailures++;
                String ipAddress = EDStatic.getIPAddress(request);
                tally.add("OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)",     ipAddress);
                tally.add("OutOfMemory (Too Big), IP Address (since last daily report)",           ipAddress);
                tally.add("OutOfMemory (Too Big), IP Address (since startup)",                     ipAddress);

            } else if (tErrorLC.indexOf(Math2.memory) >= 0) {
                //catchall for remaining memory problems
                errorNo = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE; //http error 413 (the old name for Payload Too Large)
                String ipAddress = EDStatic.getIPAddress(request);
                tally.add("OutOfMemory (Way Too Big), IP Address (since last Major LoadDatasets)", ipAddress);
                tally.add("OutOfMemory (Way Too Big), IP Address (since last daily report)",       ipAddress);
                tally.add("OutOfMemory (Way Too Big), IP Address (since startup)",                 ipAddress);

            } else if (tErrorLC.indexOf("unauthorized") >= 0) {
                errorNo = HttpServletResponse.SC_UNAUTHORIZED; //http error 401

            } else if (tErrorLC.indexOf("forbidden") >= 0) {
                errorNo = HttpServletResponse.SC_FORBIDDEN; //http error 403

            } else if (tErrorLC.indexOf("timeout") >= 0 ||
                       tErrorLC.indexOf("time out") >= 0 ||
                       tErrorLC.indexOf("timed out") >= 0) { //testDescendingAxisGeotif sees this  
                errorNo = HttpServletResponse.SC_REQUEST_TIMEOUT; //http error 408

            } else {
                //everything else
                if (tError.indexOf("NullPointerException") >= 0 && emailDiagnosticsToErdData) {
                    //email stack trace for all NullPointerExceptions to erd.data@noaa.gov (i.e., ERDDAP development team)
                    email("erd.data@noaa.gov", 
                        "java.lang.NullPointerException in ERDDAP v" + erddapVersion, 
                        //I debated emailing the requestUrl, too. There are security and privacy issues. so don't do it.
                        //"request=" + 
                        //(baseHttpsUrl.startsWith("(")? baseUrl : baseHttpsUrl) + //request may actually have been to http or https (I'm too lazy to write proper code / doesn't seem necessary)
                        //(tRequest.indexOf("login.html?") >= 0? tRequestURI + "?[CONFIDENTIAL]" : tRequest) + "\n\n" + //don't show passwords, nonces, etc
                        MustBe.throwableToString(t));
                } 
                errorNo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; //http error 500
            }

            String2.log(
                "*** sendErrorCode " + errorNo + " for request #" + requestNumber + ":\n" + 
                tRequest + "\n" + //not decoded
                MustBe.throwableToString(t).trim());  //always log full stack trace

            lowSendError(requestNumber, response, errorNo, tError);

        } catch (Throwable t2) {
            //an exception occurs if response is committed
            throw new ServletException(t2);
        }

    }

    /**
     * This is the lower level version of sendError. Use this if the http errorNo
     * is known.
     *
     * @param requestNumber The requestNumber assigned to this request by doGet().
     * @param response The response to be written to.
     * @param errorNo  the HTTP status code / error number.
     *   Note that DAP 2.0 says error code is 1 digit, but doesn't provide
     *   a list of codes and meanings. I use HTTP status codes (3 digits).
     * @param msg suitable for the user (not the full diagnostic information).
     */
    public static void lowSendError(int requestNumber, HttpServletResponse response, int errorNo, String msg) {
        try {
            msg = String2.isSomething(msg)? msg.trim() : "(no details)";

            //slowDownTroubleMillis applies to all errors 
            //because any of these errors could be in a script
            //and it's good to slow the script down (prevent 100 bad requests/second)
            //and if it's a human they won't even notice a short delay
            if (EDStatic.slowDownTroubleMillis > 0)
                Math2.sleep(EDStatic.slowDownTroubleMillis);

            //put the HTTP status code name at the start of the message (from Wikipedia list
            // https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
            if      (errorNo == HttpServletResponse.SC_BAD_REQUEST)  //http error 400
                msg = "Bad Request: " + msg;    //Don't translate these (or at least keep English first) so user can look for them
            else if (errorNo == HttpServletResponse.SC_UNAUTHORIZED) //http error 401
                msg = "Unauthorized: " + msg;
            else if (errorNo == HttpServletResponse.SC_FORBIDDEN)    //http error 403
                msg = "Forbidden: " + msg;
            else if (errorNo == HttpServletResponse.SC_NOT_FOUND)    //http error 404
                msg = "Not Found: " + msg;
            else if (errorNo == HttpServletResponse.SC_REQUEST_TIMEOUT)   //http error 408
                msg = "Request Timeout: " + msg;
            else if (errorNo == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) //http error 413 (the old name for Payload Too Large)
                msg = "Payload Too Large: " + msg;
            else if (errorNo == HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE) //http error 416
                msg = "Requested Range Not Satisfiable: " + msg;
            else if (errorNo == 429) //http error 429  isn't defined in HttpServletResponse.
                msg = "Too Many Requests: " + msg;
            else if (errorNo == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) //http error 500
                msg = "Internal Server Error: " + msg;

            //always log the error
            String fullMsg = 
                "Error {\n" +
                "    code=" + errorNo + ";\n" +
                "    message=" + String2.toJson(msg, 65536, false) + ";\n" +
                "}\n";
            if (msg.indexOf(blacklistMsg) < 0)
                String2.log("*** lowSendError for request #" + requestNumber + 
                    ": isCommitted=" + (response == null || response.isCommitted()) + 
                    " fullMessage=\n" +
                    fullMsg); // + MustBe.getStackTrace());

            //if response isCommitted, nothing more can be done
            if (response == null) {
                String2.log("  response=null, so I'm not sending anything");
            } else if (!response.isCommitted()) {
                standardDapHeader(response);
                response.setStatus(errorNo);
                //set content type both ways in hopes of overwriting any previous settings
                response.setHeader("Content-Type", "text/plain; charset=UTF-8"); 
                response.setContentType("text/plain");
                response.setCharacterEncoding(String2.UTF_8);
                response.setHeader("Content-Description", "dods-error"); 
                response.setHeader("Content-Encoding", "identity");  //not e.g. deflate
                OutputStream outputStream = new BufferedOutputStream(response.getOutputStream()); //after all setHeader
                Writer writer = null;
                try {
                    writer = String2.getBufferedOutputStreamWriterUtf8(outputStream);
                    //from DAP 2.0 section 7.2.4
                    writer.write(fullMsg);

                } finally {
                    if (writer != null) 
                        writer.close();
                    else outputStream.close();
                }                
            }
        } catch (Throwable t) {
            String2.log(String2.ERROR + " in lowSendError for request #" + requestNumber + ":\n" + 
                MustBe.throwableToString(t));
        } finally {
            //last thing, try hard to close the outputstream
            try {
                //was if (!response.isCommitted()) 
                response.getOutputStream().close();
            } catch (Exception e2) {}
        }
    }


    public static void testUpdateUrls() throws Exception {
        String2.log("\n***** EDStatic.testUpdateUrls");
        Attributes source = new Attributes();
        Attributes add    = new Attributes();
        source.set("a", "http://coastwatch.pfel.noaa.gov");  //purposely out-of-date
        source.set("nine", 9.0);
        add.set(   "b", "http://www.whoi.edu"); //purposely out-of-date
        add.set(   "ten", 10.0);
        add.set(   "sourceUrl", "http://coastwatch.pfel.noaa.gov"); //purposely out-of-date
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
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ EDStatic.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testUpdateUrls();
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

    /**
     * Update the currLang variable and update all strings read from messages.xml
     * @param input the user's choice of language
     */
    public static void updateLangChoice(int input){
        if (input < 0 || input >= languageList.length) {
            return;
        } else {
            currLang = input;
        }
        try {
            
        acceptEncodingHtml = acceptEncodingHtml_s[input];
        filesDocumentation = filesDocumentation_s[input];

        standardShortDescriptionHtml = standardShortDescriptionHtml_s[input];
        DEFAULT_standardLicense = DEFAULT_standardLicense_s[input];
        DEFAULT_standardContact = DEFAULT_standardContact_s[input];
        DEFAULT_standardDataLicenses = DEFAULT_standardDataLicenses_s[input];
        DEFAULT_standardDisclaimerOfEndorsement = DEFAULT_standardDisclaimerOfEndorsement_s[input];
        DEFAULT_standardDisclaimerOfExternalLinks = DEFAULT_standardDisclaimerOfExternalLinks_s[input];
        DEFAULT_standardGeneralDisclaimer = DEFAULT_standardGeneralDisclaimer_s[input];
        DEFAULT_standardPrivacyPolicy = DEFAULT_standardPrivacyPolicy_s[input];
        DEFAULT_startHeadHtml = DEFAULT_startHeadHtml_s[input];
        DEFAULT_startBodyHtml = DEFAULT_startBodyHtml_s[input];
        DEFAULT_theShortDescriptionHtml = DEFAULT_theShortDescriptionHtml_s[input];
        DEFAULT_endBodyHtml = DEFAULT_endBodyHtml_s[input];
 
        standardLicense = standardLicense_s[input];
        standardContact = standardContact_s[input];
        standardDataLicenses = standardDataLicenses_s[input];
        standardDisclaimerOfEndorsement = standardDisclaimerOfEndorsement_s[input];
        standardDisclaimerOfExternalLinks = standardDisclaimerOfExternalLinks_s[input];
        standardGeneralDisclaimer = standardGeneralDisclaimer_s[input];
        standardPrivacyPolicy = standardPrivacyPolicy_s[input];
        startHeadHtml = startHeadHtml_s[input];
        startBodyHtml = startBodyHtml_s[input];
        theShortDescriptionHtml = theShortDescriptionHtml_s[input];
        endBodyHtml = endBodyHtml_s[input];

        acronyms = acronyms_s[input];
        accessRESTFUL = accessRESTFUL_s[input];
        addConstraints = addConstraints_s[input];
        addVarWhereAttName = addVarWhereAttName_s[input];
        addVarWhereAttValue = addVarWhereAttValue_s[input];
        addVarWhere = addVarWhere_s[input];
        additionalLinks = additionalLinks_s[input];

        admSummary = admSummary_s[input];
        admTitle = admTitle_s[input];
        advl_datasetID = advl_datasetID_s[input];
        advc_accessible = advc_accessible_s[input];
        advl_accessible = advl_accessible_s[input];
        advl_institution = advl_institution_s[input];
        advc_dataStructure = advc_dataStructure_s[input];
        advl_dataStructure = advl_dataStructure_s[input];
        
        advl_cdm_data_type = advl_cdm_data_type_s[input];
        
        advl_class = advl_class_s[input];
        
        advl_title = advl_title_s[input];
        advl_minLongitude = advl_minLongitude_s[input];
        advl_maxLongitude = advl_maxLongitude_s[input];
        advl_longitudeSpacing = advl_longitudeSpacing_s[input];
        advl_minLatitude = advl_minLatitude_s[input];
        advl_maxLatitude = advl_maxLatitude_s[input];
        advl_latitudeSpacing = advl_latitudeSpacing_s[input];
        advl_minAltitude = advl_minAltitude_s[input];
        advl_maxAltitude = advl_maxAltitude_s[input];
        advl_minTime = advl_minTime_s[input];
        advc_maxTime = advc_maxTime_s[input];
        advl_maxTime = advl_maxTime_s[input];
        advl_timeSpacing = advl_timeSpacing_s[input];
        advc_griddap = advc_griddap_s[input];
        advl_griddap = advl_griddap_s[input];
        advl_subset = advl_subset_s[input];
        advc_tabledap = advc_tabledap_s[input];
        advl_tabledap = advl_tabledap_s[input];
        advl_MakeAGraph = advl_MakeAGraph_s[input];
        advc_sos = advc_sos_s[input];
        advl_sos = advl_sos_s[input];
        advl_wcs = advl_wcs_s[input];
        advl_wms = advl_wms_s[input];
        advc_files = advc_files_s[input];
        advl_files = advl_files_s[input];
        advc_fgdc = advc_fgdc_s[input];
        advl_fgdc = advl_fgdc_s[input];
        advc_iso19115 = advc_iso19115_s[input];
        advl_iso19115 = advl_iso19115_s[input];
        advc_metadata = advc_metadata_s[input];
        advl_metadata = advl_metadata_s[input];
        advl_sourceUrl = advl_sourceUrl_s[input];
        advl_infoUrl = advl_infoUrl_s[input];
        advl_rss = advl_rss_s[input];
        advc_email = advc_email_s[input];
        advl_email = advl_email_s[input];
        advl_summary = advl_summary_s[input];
        advc_testOutOfDate = advc_testOutOfDate_s[input];
        advl_testOutOfDate = advl_testOutOfDate_s[input];
        advc_outOfDate = advc_outOfDate_s[input];
        advl_outOfDate = advl_outOfDate_s[input];
        advn_outOfDate = advn_outOfDate_s[input];
        advancedSearch = advancedSearch_s[input];
        advancedSearchResults = advancedSearchResults_s[input];
        advancedSearchDirections = advancedSearchDirections_s[input];
        advancedSearchTooltip = advancedSearchTooltip_s[input];
        advancedSearchBounds = advancedSearchBounds_s[input];
        advancedSearchMinLat = advancedSearchMinLat_s[input];
        advancedSearchMaxLat = advancedSearchMaxLat_s[input];
        advancedSearchMinLon = advancedSearchMinLon_s[input];
        advancedSearchMaxLon = advancedSearchMaxLon_s[input];
        advancedSearchMinMaxLon = advancedSearchMinMaxLon_s[input];
        advancedSearchMinTime = advancedSearchMinTime_s[input];
        advancedSearchMaxTime = advancedSearchMaxTime_s[input];
        advancedSearchClear = advancedSearchClear_s[input];
        advancedSearchClearHelp = advancedSearchClearHelp_s[input];
        advancedSearchCategoryTooltip = advancedSearchCategoryTooltip_s[input];
        advancedSearchRangeTooltip = advancedSearchRangeTooltip_s[input];
        advancedSearchMapTooltip = advancedSearchMapTooltip_s[input];
        advancedSearchLonTooltip = advancedSearchLonTooltip_s[input];
        advancedSearchTimeTooltip = advancedSearchTimeTooltip_s[input];
        advancedSearchWithCriteria = advancedSearchWithCriteria_s[input];
        advancedSearchFewerCriteria = advancedSearchFewerCriteria_s[input];
        advancedSearchNoCriteria = advancedSearchNoCriteria_s[input];
        advancedSearchErrorHandling = advancedSearchErrorHandling_s[input];
        autoRefresh = autoRefresh_s[input];

        categoryTitleHtml = categoryTitleHtml_s[input];
        categoryHtml = categoryHtml_s[input];
        category3Html = category3Html_s[input];
        categoryPickAttribute = categoryPickAttribute_s[input];
        categorySearchHtml = categorySearchHtml_s[input];
        categorySearchDifferentHtml = categorySearchDifferentHtml_s[input];
        categoryClickHtml = categoryClickHtml_s[input];
        categoryNotAnOption = categoryNotAnOption_s[input];
        caughtInterrupted = caughtInterrupted_s[input];
        cdmDataTypeHelp = cdmDataTypeHelp_s[input];
        clickAccess = clickAccess_s[input];
        clickBackgroundInfo = clickBackgroundInfo_s[input];
        clickERDDAP = clickERDDAP_s[input];
        clickInfo = clickInfo_s[input];
        clickToSubmit = clickToSubmit_s[input];
        converterWebService = converterWebService_s[input];
        convertOceanicAtmosphericAcronyms = convertOceanicAtmosphericAcronyms_s[input];
        convertOceanicAtmosphericAcronymsIntro = convertOceanicAtmosphericAcronymsIntro_s[input];
        convertOceanicAtmosphericAcronymsNotes = convertOceanicAtmosphericAcronymsNotes_s[input];
        convertOceanicAtmosphericAcronymsService = convertOceanicAtmosphericAcronymsService_s[input];
        convertOceanicAtmosphericVariableNames = convertOceanicAtmosphericVariableNames_s[input];
        convertOceanicAtmosphericVariableNamesIntro = convertOceanicAtmosphericVariableNamesIntro_s[input];
        convertOceanicAtmosphericVariableNamesNotes = convertOceanicAtmosphericVariableNamesNotes_s[input];
        convertOceanicAtmosphericVariableNamesService = convertOceanicAtmosphericVariableNamesService_s[input];
        convertFipsCounty = convertFipsCounty_s[input];
        convertFipsCountyIntro = convertFipsCountyIntro_s[input];
        convertFipsCountyNotes = convertFipsCountyNotes_s[input];
        convertFipsCountyService = convertFipsCountyService_s[input];
        convertHtml = convertHtml_s[input];
        convertInterpolate = convertInterpolate_s[input];
        convertInterpolateIntro = convertInterpolateIntro_s[input];
        convertInterpolateTLLTable = convertInterpolateTLLTable_s[input];
        convertInterpolateTLLTableHelp = convertInterpolateTLLTableHelp_s[input];
        convertInterpolateDatasetIDVariable = convertInterpolateDatasetIDVariable_s[input];
        convertInterpolateDatasetIDVariableHelp = convertInterpolateDatasetIDVariableHelp_s[input];
        convertInterpolateNotes = convertInterpolateNotes_s[input];
        convertInterpolateService = convertInterpolateService_s[input];
        convertKeywords = convertKeywords_s[input];
        convertKeywordsCfTooltip = convertKeywordsCfTooltip_s[input];
        convertKeywordsGcmdTooltip = convertKeywordsGcmdTooltip_s[input];
        convertKeywordsIntro = convertKeywordsIntro_s[input];
        convertKeywordsNotes = convertKeywordsNotes_s[input];
        convertKeywordsService = convertKeywordsService_s[input];
        convertTime = convertTime_s[input];
        convertTimeBypass = convertTimeBypass_s[input];
        convertTimeReference = convertTimeReference_s[input];
        convertTimeIntro = convertTimeIntro_s[input];
        convertTimeNotes = convertTimeNotes_s[input];
        convertTimeService = convertTimeService_s[input];
        convertTimeNumberTooltip = convertTimeNumberTooltip_s[input];
        convertTimeStringTimeTooltip = convertTimeStringTimeTooltip_s[input];
        convertTimeUnitsTooltip = convertTimeUnitsTooltip_s[input];
        convertTimeUnitsHelp = convertTimeUnitsHelp_s[input];
        convertTimeIsoFormatError = convertTimeIsoFormatError_s[input];
        convertTimeNoSinceError = convertTimeNoSinceError_s[input];
        convertTimeNumberError = convertTimeNumberError_s[input];
        convertTimeNumericTimeError = convertTimeNumericTimeError_s[input];
        convertTimeParametersError = convertTimeParametersError_s[input];
        convertTimeStringFormatError = convertTimeStringFormatError_s[input];
        convertTimeTwoTimeError = convertTimeTwoTimeError_s[input];
        convertTimeUnitsError = convertTimeUnitsError_s[input];
        convertUnits = convertUnits_s[input];
        convertUnitsComparison = convertUnitsComparison_s[input];
        convertUnitsFilter = convertUnitsFilter_s[input];
        convertUnitsIntro = convertUnitsIntro_s[input];
        convertUnitsNotes = convertUnitsNotes_s[input];
        convertUnitsService = convertUnitsService_s[input];
        convertURLs = convertURLs_s[input];
        convertURLsIntro = convertURLsIntro_s[input];
        convertURLsNotes = convertURLsNotes_s[input];
        convertURLsService = convertURLsService_s[input];
        cookiesHelp = cookiesHelp_s[input];
        daf = daf_s[input];
        dafGridBypassTooltip = dafGridBypassTooltip_s[input];
        dafGridTooltip = dafGridTooltip_s[input];
        dafTableBypassTooltip = dafTableBypassTooltip_s[input];
        dafTableTooltip = dafTableTooltip_s[input];
        dasTitle = dasTitle_s[input];
        dataAccessNotAllowed = dataAccessNotAllowed_s[input];
        databaseUnableToConnect = databaseUnableToConnect_s[input];
        dataProviderFormSuccess = dataProviderFormSuccess_s[input];
        dataProviderFormShortDescription = dataProviderFormShortDescription_s[input];
        dataProviderFormLongDescriptionHTML = dataProviderFormLongDescriptionHTML_s[input];
        dataProviderFormPart1 = dataProviderFormPart1_s[input];
        dataProviderFormPart2Header = dataProviderFormPart2Header_s[input];
        dataProviderFormPart2GlobalMetadata = dataProviderFormPart2GlobalMetadata_s[input];
        dataProviderContactInfo = dataProviderContactInfo_s[input];
        dataProviderData = dataProviderData_s[input];
        dpf_submit = dpf_submit_s[input];
        dpf_fixProblem = dpf_fixProblem_s[input];
        dpf_yourName = dpf_yourName_s[input];
        dpf_emailAddress = dpf_emailAddress_s[input];
        dpf_Timestamp = dpf_Timestamp_s[input];
        dpf_frequency = dpf_frequency_s[input];
        dpf_title = dpf_title_s[input];
        dpf_titleTooltip = dpf_titleTooltip_s[input];
        dpf_summary = dpf_summary_s[input];
        dpf_summaryTooltip = dpf_summaryTooltip_s[input];
        dpf_creatorName = dpf_creatorName_s[input];
        dpf_creatorNameTooltip = dpf_creatorNameTooltip_s[input];
        dpf_creatorType = dpf_creatorType_s[input];
        dpf_creatorTypeTooltip = dpf_creatorTypeTooltip_s[input];
        dpf_creatorEmail = dpf_creatorEmail_s[input];
        dpf_creatorEmailTooltip = dpf_creatorEmailTooltip_s[input];
        dpf_institution = dpf_institution_s[input];
        dpf_institutionTooltip = dpf_institutionTooltip_s[input];
        dpf_infoUrl = dpf_infoUrl_s[input];
        dpf_infoUrlTooltip = dpf_infoUrlTooltip_s[input];
        dpf_license = dpf_license_s[input];
        dpf_licenseTooltip = dpf_licenseTooltip_s[input];
        dpf_howYouStoreData = dpf_howYouStoreData_s[input];
        dpf_required = dpf_required_s[input];
        dpf_optional = dpf_optional_s[input];
        dpf_provideIfAvaliable = dpf_provideIfAvaliable_s[input];
        dpf_acknowledgement = dpf_acknowledgement_s[input];
        dpf_acknowledgementTooltip = dpf_acknowledgementTooltip_s[input];
        dpf_history = dpf_history_s[input];
        dpf_historyTooltip = dpf_historyTooltip_s[input];
        dpf_idTooltip = dpf_idTooltip_s[input];
        dpf_namingAuthority = dpf_namingAuthority_s[input];
        dpf_namingAuthorityTooltip = dpf_namingAuthorityTooltip_s[input];
        dpf_productVersion = dpf_productVersion_s[input];
        dpf_productVersionTooltip = dpf_productVersionTooltip_s[input];
        dpf_references = dpf_references_s[input];
        dpf_referencesTooltip = dpf_referencesTooltip_s[input];
        dpf_comment = dpf_comment_s[input];
        dpf_commentTooltip = dpf_commentTooltip_s[input];
        dpf_dataTypeHelp = dpf_dataTypeHelp_s[input];
        dpf_ioosCategory = dpf_ioosCategory_s[input];
        dpf_ioosCategoryHelp = dpf_ioosCategoryHelp_s[input];
        dpf_part3Header = dpf_part3Header_s[input];
        dpf_variableMetadata = dpf_variableMetadata_s[input];
        dpf_sourceName = dpf_sourceName_s[input];
        dpf_sourceNameTooltip = dpf_sourceNameTooltip_s[input];
        dpf_destinationName = dpf_destinationName_s[input];
        dpf_destinationNameTooltip = dpf_destinationNameTooltip_s[input];
        dpf_longName = dpf_longName_s[input];
        dpf_longNameTooltip = dpf_longNameTooltip_s[input];
        dpf_standardName = dpf_standardName_s[input];
        dpf_standardNameTooltip = dpf_standardNameTooltip_s[input];
        dpf_dataType = dpf_dataType_s[input];
        dpf_fillValue = dpf_fillValue_s[input];
        dpf_fillValueTooltip = dpf_fillValueTooltip_s[input];
        dpf_units = dpf_units_s[input];
        dpf_unitsTooltip = dpf_unitsTooltip_s[input];
        dpf_range = dpf_range_s[input];
        dpf_rangeTooltip = dpf_rangeTooltip_s[input];
        dpf_part4Header = dpf_part4Header_s[input];
        dpf_otherComment = dpf_otherComment_s[input];
        dpf_finishPart4 = dpf_finishPart4_s[input];
        dpf_congratulation = dpf_congratulation_s[input];
        disabled = disabled_s[input];
        distinctValuesTooltip = distinctValuesTooltip_s[input];
        doWithGraphs = doWithGraphs_s[input];
        dtAccessible = dtAccessible_s[input];
        dtAccessibleYes = dtAccessibleYes_s[input];
        dtAccessibleGraphs = dtAccessibleGraphs_s[input];
        dtAccessibleNo = dtAccessibleNo_s[input];
        dtAccessibleLogIn = dtAccessibleLogIn_s[input];
        dtLogIn = dtLogIn_s[input];
        dtDAF = dtDAF_s[input];
        dtFiles = dtFiles_s[input];
        dtMAG = dtMAG_s[input];
        dtSOS = dtSOS_s[input];
        dtSubset = dtSubset_s[input];
        dtWCS = dtWCS_s[input];
        dtWMS = dtWMS_s[input];
        EDDDatasetID = EDDDatasetID_s[input];
        EDDFgdc = EDDFgdc_s[input];
        EDDFgdcMetadata = EDDFgdcMetadata_s[input];
        EDDFiles = EDDFiles_s[input];
        EDDIso19115Metadata = EDDIso19115Metadata_s[input];
        EDDMetadata = EDDMetadata_s[input];
        EDDBackground = EDDBackground_s[input];
        EDDClickOnSubmitHtml = EDDClickOnSubmitHtml_s[input];
        EDDInstitution = EDDInstitution_s[input];
        EDDInformation = EDDInformation_s[input];
        EDDSummary = EDDSummary_s[input];
        EDDDatasetTitle = EDDDatasetTitle_s[input];
        EDDDownloadData = EDDDownloadData_s[input];
        EDDMakeAGraph = EDDMakeAGraph_s[input];
        EDDMakeAMap = EDDMakeAMap_s[input];
        EDDFileType = EDDFileType_s[input];
        EDDFileTypeInformation = EDDFileTypeInformation_s[input];
        EDDSelectFileType = EDDSelectFileType_s[input];
        EDDMinimum = EDDMinimum_s[input];
        EDDMaximum = EDDMaximum_s[input];
        EDDConstraint = EDDConstraint_s[input];
        EDDChangedWasnt = EDDChangedWasnt_s[input];
        EDDChangedDifferentNVar = EDDChangedDifferentNVar_s[input];
        EDDChanged2Different = EDDChanged2Different_s[input];
        EDDChanged1Different = EDDChanged1Different_s[input];
        EDDChangedCGADifferent = EDDChangedCGADifferent_s[input];
        EDDChangedAxesDifferentNVar = EDDChangedAxesDifferentNVar_s[input];
        EDDChangedAxes2Different = EDDChangedAxes2Different_s[input];
        EDDChangedAxes1Different = EDDChangedAxes1Different_s[input];
        EDDChangedNoValue = EDDChangedNoValue_s[input];
        EDDChangedTableToGrid = EDDChangedTableToGrid_s[input];
        EDDSimilarDifferentNVar = EDDSimilarDifferentNVar_s[input];
        EDDSimilarDifferent = EDDSimilarDifferent_s[input];
        EDDGridDapDescription = EDDGridDapDescription_s[input];
        EDDGridDapLongDescription = EDDGridDapLongDescription_s[input];
        EDDGridDownloadDataTooltip = EDDGridDownloadDataTooltip_s[input];
        EDDGridDimension = EDDGridDimension_s[input];
        EDDGridDimensionRanges = EDDGridDimensionRanges_s[input];
        EDDGridFirst = EDDGridFirst_s[input];
        EDDGridLast = EDDGridLast_s[input];
        EDDGridStart = EDDGridStart_s[input];
        EDDGridStop = EDDGridStop_s[input];
        EDDGridStartStopTooltip = EDDGridStartStopTooltip_s[input];
        EDDGridStride = EDDGridStride_s[input];
        EDDGridNValues = EDDGridNValues_s[input];
        EDDGridNValuesHtml = EDDGridNValuesHtml_s[input];
        EDDGridSpacing = EDDGridSpacing_s[input];
        EDDGridJustOneValue = EDDGridJustOneValue_s[input];
        EDDGridEven = EDDGridEven_s[input];
        EDDGridUneven = EDDGridUneven_s[input];
        EDDGridDimensionTooltip = EDDGridDimensionTooltip_s[input];
        EDDGridDimensionFirstTooltip = EDDGridDimensionFirstTooltip_s[input];
        EDDGridDimensionLastTooltip = EDDGridDimensionLastTooltip_s[input];
        EDDGridVarHasDimTooltip = EDDGridVarHasDimTooltip_s[input];
        EDDGridSSSTooltip = EDDGridSSSTooltip_s[input];
        EDDGridStartTooltip = EDDGridStartTooltip_s[input];
        EDDGridStopTooltip = EDDGridStopTooltip_s[input];
        EDDGridStrideTooltip = EDDGridStrideTooltip_s[input];
        EDDGridSpacingTooltip = EDDGridSpacingTooltip_s[input];
        EDDGridDownloadTooltip = EDDGridDownloadTooltip_s[input];
        EDDGridGridVariableHtml = EDDGridGridVariableHtml_s[input];
        EDDTableConstraints = EDDTableConstraints_s[input];
        EDDTableTabularDatasetTooltip = EDDTableTabularDatasetTooltip_s[input];
        EDDTableVariable = EDDTableVariable_s[input];
        EDDTableCheckAll = EDDTableCheckAll_s[input];
        EDDTableCheckAllTooltip = EDDTableCheckAllTooltip_s[input];
        EDDTableUncheckAll = EDDTableUncheckAll_s[input];
        EDDTableUncheckAllTooltip = EDDTableUncheckAllTooltip_s[input];
        EDDTableMinimumTooltip = EDDTableMinimumTooltip_s[input];
        EDDTableMaximumTooltip = EDDTableMaximumTooltip_s[input];
        EDDTableCheckTheVariables = EDDTableCheckTheVariables_s[input];
        EDDTableSelectAnOperator = EDDTableSelectAnOperator_s[input];
        EDDTableFromEDDGridSummary = EDDTableFromEDDGridSummary_s[input];
        EDDTableOptConstraint1Html = EDDTableOptConstraint1Html_s[input];
        EDDTableOptConstraint2Html = EDDTableOptConstraint2Html_s[input];
        EDDTableOptConstraintVar = EDDTableOptConstraintVar_s[input];
        EDDTableNumericConstraintTooltip = EDDTableNumericConstraintTooltip_s[input];
        EDDTableStringConstraintTooltip = EDDTableStringConstraintTooltip_s[input];
        EDDTableTimeConstraintTooltip = EDDTableTimeConstraintTooltip_s[input];
        EDDTableConstraintTooltip = EDDTableConstraintTooltip_s[input];
        EDDTableSelectConstraintTooltip = EDDTableSelectConstraintTooltip_s[input];
        EDDTableDapDescription = EDDTableDapDescription_s[input];
        EDDTableDapLongDescription = EDDTableDapLongDescription_s[input];
        EDDTableDownloadDataTooltip = EDDTableDownloadDataTooltip_s[input];

        EDDTableFromHttpGetDatasetDescription = EDDTableFromHttpGetDatasetDescription_s[input];
        EDDTableFromHttpGetAuthorDescription = EDDTableFromHttpGetAuthorDescription_s[input];
        EDDTableFromHttpGetTimestampDescription = EDDTableFromHttpGetTimestampDescription_s[input];

        erddapVersionHTML = erddapVersionHTML_s[input];
        errorTitle = errorTitle_s[input];
        errorRequestUrl = errorRequestUrl_s[input];
        errorRequestQuery = errorRequestQuery_s[input];
        errorTheError = errorTheError_s[input];
        errorCopyFrom = errorCopyFrom_s[input];
        errorFileNotFound = errorFileNotFound_s[input];
        errorFileNotFoundImage = errorFileNotFoundImage_s[input];
        errorInternal = errorInternal_s[input];
        errorJsonpFunctionName = errorJsonpFunctionName_s[input];
        errorJsonpNotAllowed = errorJsonpNotAllowed_s[input];
        errorMoreThan2GB = errorMoreThan2GB_s[input];
        errorNotFound = errorNotFound_s[input];
        errorNotFoundIn = errorNotFoundIn_s[input];
        errorOdvLLTGrid = errorOdvLLTGrid_s[input];
        errorOdvLLTTable = errorOdvLLTTable_s[input];
        errorOnWebPage = errorOnWebPage_s[input];
    
        externalLink = externalLink_s[input];
        externalWebSite = externalWebSite_s[input];
        fileHelp_asc = fileHelp_asc_s[input];
        fileHelp_csv = fileHelp_csv_s[input];
        fileHelp_csvp = fileHelp_csvp_s[input];
        fileHelp_csv0 = fileHelp_csv0_s[input];
        fileHelp_dataTable = fileHelp_dataTable_s[input];
        fileHelp_das = fileHelp_das_s[input];
        fileHelp_dds = fileHelp_dds_s[input];
        fileHelp_dods = fileHelp_dods_s[input];
        fileHelpGrid_esriAscii = fileHelpGrid_esriAscii_s[input];
        fileHelpTable_esriCsv = fileHelpTable_esriCsv_s[input];
        fileHelp_fgdc = fileHelp_fgdc_s[input];
        fileHelp_geoJson = fileHelp_geoJson_s[input];
        fileHelp_graph = fileHelp_graph_s[input];
        fileHelpGrid_help = fileHelpGrid_help_s[input];
        fileHelpTable_help = fileHelpTable_help_s[input];
        fileHelp_html = fileHelp_html_s[input];
        fileHelp_htmlTable = fileHelp_htmlTable_s[input];
        fileHelp_iso19115 = fileHelp_iso19115_s[input];
        fileHelp_itxGrid = fileHelp_itxGrid_s[input];
        fileHelp_itxTable = fileHelp_itxTable_s[input];
        fileHelp_json = fileHelp_json_s[input];
        fileHelp_jsonlCSV1 = fileHelp_jsonlCSV1_s[input];
        fileHelp_jsonlCSV = fileHelp_jsonlCSV_s[input];
        fileHelp_jsonlKVP = fileHelp_jsonlKVP_s[input];
        fileHelp_mat = fileHelp_mat_s[input];
        fileHelpGrid_nc3 = fileHelpGrid_nc3_s[input];
        fileHelpGrid_nc4 = fileHelpGrid_nc4_s[input];
        fileHelpTable_nc3 = fileHelpTable_nc3_s[input];
        fileHelpTable_nc4 = fileHelpTable_nc4_s[input];
        fileHelp_nc3Header = fileHelp_nc3Header_s[input];
        fileHelp_nc4Header = fileHelp_nc4Header_s[input];
        fileHelp_nccsv = fileHelp_nccsv_s[input];
        fileHelp_nccsvMetadata = fileHelp_nccsvMetadata_s[input];
        fileHelp_ncCF = fileHelp_ncCF_s[input];
        fileHelp_ncCFHeader = fileHelp_ncCFHeader_s[input];
        fileHelp_ncCFMA = fileHelp_ncCFMA_s[input];
        fileHelp_ncCFMAHeader = fileHelp_ncCFMAHeader_s[input];
        fileHelp_ncml = fileHelp_ncml_s[input];
        fileHelp_ncoJson = fileHelp_ncoJson_s[input];
        fileHelpGrid_odvTxt = fileHelpGrid_odvTxt_s[input];
        fileHelpTable_odvTxt = fileHelpTable_odvTxt_s[input];
        fileHelp_subset = fileHelp_subset_s[input];
        fileHelp_timeGaps = fileHelp_timeGaps_s[input];
        fileHelp_tsv = fileHelp_tsv_s[input];
        fileHelp_tsvp = fileHelp_tsvp_s[input];
        fileHelp_tsv0 = fileHelp_tsv0_s[input];
        fileHelp_wav = fileHelp_wav_s[input];
        fileHelp_xhtml = fileHelp_xhtml_s[input];
        fileHelp_geotif = fileHelp_geotif_s[input];
        fileHelpGrid_kml = fileHelpGrid_kml_s[input];
        fileHelpTable_kml = fileHelpTable_kml_s[input];
        fileHelp_smallPdf = fileHelp_smallPdf_s[input];
        fileHelp_pdf = fileHelp_pdf_s[input];
        fileHelp_largePdf = fileHelp_largePdf_s[input];
        fileHelp_smallPng = fileHelp_smallPng_s[input];
        fileHelp_png = fileHelp_png_s[input];
        fileHelp_largePng = fileHelp_largePng_s[input];
        fileHelp_transparentPng = fileHelp_transparentPng_s[input];
        filesDescription = filesDescription_s[input];
        filesSort = filesSort_s[input];
        filesWarning = filesWarning_s[input];
        findOutChange = findOutChange_s[input];
        FIPSCountryCode = FIPSCountryCode_s[input];
        forSOSUse = forSOSUse_s[input];
        forWCSUse = forWCSUse_s[input];
        forWMSUse = forWMSUse_s[input];
        functions = functions_s[input];
        functionTooltip = functionTooltip_s[input];
        functionDistinctCheck = functionDistinctCheck_s[input];
        functionDistinctTooltip = functionDistinctTooltip_s[input];
        functionOrderByExtra = functionOrderByExtra_s[input];
        functionOrderByTooltip = functionOrderByTooltip_s[input];
        functionOrderBySort = functionOrderBySort_s[input];
        functionOrderBySort1 = functionOrderBySort1_s[input];
        functionOrderBySort2 = functionOrderBySort2_s[input];
        functionOrderBySort3 = functionOrderBySort3_s[input];
        functionOrderBySort4 = functionOrderBySort4_s[input];
        functionOrderBySortLeast = functionOrderBySortLeast_s[input];
        functionOrderBySortRowMax = functionOrderBySortRowMax_s[input];
        generatedAt = generatedAt_s[input];
        geoServicesDescription = geoServicesDescription_s[input];
        getStartedHtml = getStartedHtml_s[input];
        htmlTableMaxMessage = htmlTableMaxMessage_s[input];
        hpn_information = hpn_information_s[input];
        hpn_legalNotices = hpn_legalNotices_s[input];
        hpn_dataProviderForm = hpn_dataProviderForm_s[input];
        hpn_dataProviderFormP1 = hpn_dataProviderFormP1_s[input];
        hpn_dataProviderFormP2 = hpn_dataProviderFormP2_s[input];
        hpn_dataProviderFormP3 = hpn_dataProviderFormP3_s[input];
        hpn_dataProviderFormP4 = hpn_dataProviderFormP4_s[input];
        hpn_dataProviderFormDone = hpn_dataProviderFormDone_s[input];
        hpn_status = hpn_status_s[input];
        hpn_restfulWebService = hpn_restfulWebService_s[input];
        hpn_documentation = hpn_documentation_s[input];
        hpn_help = hpn_help_s[input];
        hpn_files = hpn_files_s[input];
        hpn_SOS = hpn_SOS_s[input];
        hpn_WCS = hpn_WCS_s[input];
        hpn_slideSorter = hpn_slideSorter_s[input];
        hpn_add = hpn_add_s[input];
        hpn_list = hpn_list_s[input];
        hpn_validate = hpn_validate_s[input];
        hpn_remove = hpn_remove_s[input];
        hpn_convert = hpn_convert_s[input];
        hpn_fipsCounty = hpn_fipsCounty_s[input];
        hpn_OAAcronyms = hpn_OAAcronyms_s[input];
        hpn_OAVariableNames = hpn_OAVariableNames_s[input];
        hpn_keywords = hpn_keywords_s[input];
        hpn_time = hpn_time_s[input];
        hpn_units = hpn_units_s[input];
        imageDataCourtesyOf = imageDataCourtesyOf_s[input];
        indexViewAll = indexViewAll_s[input];
        indexSearchWith = indexSearchWith_s[input];
        indexDevelopersSearch = indexDevelopersSearch_s[input];
        indexProtocol = indexProtocol_s[input];
        indexDescription = indexDescription_s[input];
        indexDatasets = indexDatasets_s[input];
        indexDocumentation = indexDocumentation_s[input];
        indexRESTfulSearch = indexRESTfulSearch_s[input];
        indexAllDatasetsSearch = indexAllDatasetsSearch_s[input];
        indexOpenSearch = indexOpenSearch_s[input];
        indexServices = indexServices_s[input];
        indexDescribeServices = indexDescribeServices_s[input];
        indexMetadata = indexMetadata_s[input];
        indexWAF1 = indexWAF1_s[input];
        indexWAF2 = indexWAF2_s[input];
        indexConverters = indexConverters_s[input];
        indexDescribeConverters = indexDescribeConverters_s[input];
        infoAboutFrom = infoAboutFrom_s[input];
        infoTableTitleHtml = infoTableTitleHtml_s[input];
        infoRequestForm = infoRequestForm_s[input];
        inotifyFix = inotifyFix_s[input];
        interpolate = interpolate_s[input];
        javaProgramsHTML = javaProgramsHTML_s[input];
        justGenerateAndView = justGenerateAndView_s[input];
        justGenerateAndViewTooltip = justGenerateAndViewTooltip_s[input];
        justGenerateAndViewUrl = justGenerateAndViewUrl_s[input];
        justGenerateAndViewGraphUrlTooltip = justGenerateAndViewGraphUrlTooltip_s[input];
        keywords_word = keywords_word_s[input];
        langCode = langCode_s[input];
        legal = legal_s[input];
        legalNotices = legalNotices_s[input];
        license = license_s[input];
        listAll = listAll_s[input];
        listOfDatasets = listOfDatasets_s[input];
        LogIn = LogIn_s[input];
        login = login_s[input];
        loginHTML = loginHTML_s[input];
        loginAttemptBlocked = loginAttemptBlocked_s[input];
        loginDescribeCustom = loginDescribeCustom_s[input];
        loginDescribeEmail = loginDescribeEmail_s[input];
        loginDescribeGoogle = loginDescribeGoogle_s[input];
        loginDescribeOrcid = loginDescribeOrcid_s[input];
        loginDescribeOauth2 = loginDescribeOauth2_s[input];
        loginErddap = loginErddap_s[input];
        loginCanNot = loginCanNot_s[input];
        loginAreNot = loginAreNot_s[input];
        loginToLogIn = loginToLogIn_s[input];
        loginEmailAddress = loginEmailAddress_s[input];
        loginYourEmailAddress = loginYourEmailAddress_s[input];
        loginUserName = loginUserName_s[input];
        loginPassword = loginPassword_s[input];
        loginUserNameAndPassword = loginUserNameAndPassword_s[input];
        loginGoogleSignIn = loginGoogleSignIn_s[input];
        loginGoogleSignIn2 = loginGoogleSignIn2_s[input];
        loginOrcidSignIn = loginOrcidSignIn_s[input];
        loginOpenID = loginOpenID_s[input];
        loginOpenIDOr = loginOpenIDOr_s[input];
        loginOpenIDCreate = loginOpenIDCreate_s[input];
        loginOpenIDFree = loginOpenIDFree_s[input];
        loginOpenIDSame = loginOpenIDSame_s[input];
        loginAs = loginAs_s[input];
        loginPartwayAs = loginPartwayAs_s[input];
        loginFailed = loginFailed_s[input];
        loginSucceeded = loginSucceeded_s[input];
        loginInvalid = loginInvalid_s[input];
        loginNot = loginNot_s[input];
        loginBack = loginBack_s[input];
        loginProblemExact = loginProblemExact_s[input];
        loginProblemExpire = loginProblemExpire_s[input];
        loginProblemGoogleAgain = loginProblemGoogleAgain_s[input];
        loginProblemOrcidAgain = loginProblemOrcidAgain_s[input];
        loginProblemOauth2Again = loginProblemOauth2Again_s[input];
        loginProblemSameBrowser = loginProblemSameBrowser_s[input];
        loginProblem3Times = loginProblem3Times_s[input];
        loginProblems = loginProblems_s[input];
        loginProblemsAfter = loginProblemsAfter_s[input];
        loginPublicAccess = loginPublicAccess_s[input];
        LogOut = LogOut_s[input];
        logout = logout_s[input];
        logoutOpenID = logoutOpenID_s[input];
        logoutSuccess = logoutSuccess_s[input];
        mag = mag_s[input];
        magAxisX = magAxisX_s[input];
        magAxisY = magAxisY_s[input];
        magAxisColor = magAxisColor_s[input];
        magAxisStickX = magAxisStickX_s[input];
        magAxisStickY = magAxisStickY_s[input];
        magAxisVectorX = magAxisVectorX_s[input];
        magAxisVectorY = magAxisVectorY_s[input];
        magAxisHelpGraphX = magAxisHelpGraphX_s[input];
        magAxisHelpGraphY = magAxisHelpGraphY_s[input];
        magAxisHelpMarkerColor = magAxisHelpMarkerColor_s[input];
        magAxisHelpSurfaceColor = magAxisHelpSurfaceColor_s[input];
        magAxisHelpStickX = magAxisHelpStickX_s[input];
        magAxisHelpStickY = magAxisHelpStickY_s[input];
        magAxisHelpMapX = magAxisHelpMapX_s[input];
        magAxisHelpMapY = magAxisHelpMapY_s[input];
        magAxisHelpVectorX = magAxisHelpVectorX_s[input];
        magAxisHelpVectorY = magAxisHelpVectorY_s[input];
        magAxisVarHelp = magAxisVarHelp_s[input];
        magAxisVarHelpGrid = magAxisVarHelpGrid_s[input];
        magConstraintHelp = magConstraintHelp_s[input];
        magDocumentation = magDocumentation_s[input];
        magDownload = magDownload_s[input];
        magDownloadTooltip = magDownloadTooltip_s[input];
        magFileType = magFileType_s[input];
        magGraphType = magGraphType_s[input];
        magGraphTypeTooltipGrid = magGraphTypeTooltipGrid_s[input];
        magGraphTypeTooltipTable = magGraphTypeTooltipTable_s[input];
        magGS = magGS_s[input];
        magGSMarkerType = magGSMarkerType_s[input];
        magGSSize = magGSSize_s[input];
        magGSColor = magGSColor_s[input];
        magGSColorBar = magGSColorBar_s[input];
        magGSColorBarTooltip = magGSColorBarTooltip_s[input];
        magGSContinuity = magGSContinuity_s[input];
        magGSContinuityTooltip = magGSContinuityTooltip_s[input];
        magGSScale = magGSScale_s[input];
        magGSScaleTooltip = magGSScaleTooltip_s[input];
        magGSMin = magGSMin_s[input];
        magGSMinTooltip = magGSMinTooltip_s[input];
        magGSMax = magGSMax_s[input];
        magGSMaxTooltip = magGSMaxTooltip_s[input];
        magGSNSections = magGSNSections_s[input];
        magGSNSectionsTooltip = magGSNSectionsTooltip_s[input];
        magGSLandMask = magGSLandMask_s[input];
        magGSLandMaskTooltipGrid = magGSLandMaskTooltipGrid_s[input];
        magGSLandMaskTooltipTable = magGSLandMaskTooltipTable_s[input];
        magGSVectorStandard = magGSVectorStandard_s[input];
        magGSVectorStandardTooltip = magGSVectorStandardTooltip_s[input];
        magGSYAscendingTooltip = magGSYAscendingTooltip_s[input];
        magGSYAxisMin = magGSYAxisMin_s[input];
        magGSYAxisMax = magGSYAxisMax_s[input];
        magGSYRangeMinTooltip = magGSYRangeMinTooltip_s[input];
        magGSYRangeMaxTooltip = magGSYRangeMaxTooltip_s[input];
        magGSYRangeTooltip = magGSYRangeTooltip_s[input];
        magGSYScaleTooltip = magGSYScaleTooltip_s[input];
        magItemFirst = magItemFirst_s[input];
        magItemPrevious = magItemPrevious_s[input];
        magItemNext = magItemNext_s[input];
        magItemLast = magItemLast_s[input];
        magJust1Value = magJust1Value_s[input];
        magRange = magRange_s[input];
        magRangeTo = magRangeTo_s[input];
        magRedraw = magRedraw_s[input];
        magRedrawTooltip = magRedrawTooltip_s[input];
        magTimeRange = magTimeRange_s[input];
        magTimeRangeFirst = magTimeRangeFirst_s[input];
        magTimeRangeBack = magTimeRangeBack_s[input];
        magTimeRangeForward = magTimeRangeForward_s[input];
        magTimeRangeLast = magTimeRangeLast_s[input];
        magTimeRangeTooltip = magTimeRangeTooltip_s[input];
        magTimeRangeTooltip2 = magTimeRangeTooltip2_s[input];
        magTimesVary = magTimesVary_s[input];
        magViewUrl = magViewUrl_s[input];
        magZoom = magZoom_s[input];
        magZoomCenter = magZoomCenter_s[input];
        magZoomCenterTooltip = magZoomCenterTooltip_s[input];
        magZoomIn = magZoomIn_s[input];
        magZoomInTooltip = magZoomInTooltip_s[input];
        magZoomOut = magZoomOut_s[input];
        magZoomOutTooltip = magZoomOutTooltip_s[input];
        magZoomALittle = magZoomALittle_s[input];
        magZoomData = magZoomData_s[input];
        magZoomOutData = magZoomOutData_s[input];
        magGridTooltip = magGridTooltip_s[input];
        magTableTooltip = magTableTooltip_s[input];
        metadataDownload = metadataDownload_s[input];
        moreInformation = moreInformation_s[input];
        nMatching1 = nMatching1_s[input];
        nMatching = nMatching_s[input];
        nMatchingAlphabetical = nMatchingAlphabetical_s[input];
        nMatchingMostRelevant = nMatchingMostRelevant_s[input];
        nMatchingPage = nMatchingPage_s[input];
        nMatchingCurrent = nMatchingCurrent_s[input];
        noDataFixedValue = noDataFixedValue_s[input];
        noDataNoLL = noDataNoLL_s[input];
        noDatasetWith = noDatasetWith_s[input];
        noPage1 = noPage1_s[input];
        noPage2 = noPage2_s[input];
        notAllowed = notAllowed_s[input];
        notAuthorized = notAuthorized_s[input];
        notAuthorizedForData = notAuthorizedForData_s[input];
        notAvailable = notAvailable_s[input];
        note = note_s[input];
        noXxx = noXxx_s[input];
        noXxxBecause = noXxxBecause_s[input];
        noXxxBecause2 = noXxxBecause2_s[input];
        noXxxNotActive = noXxxNotActive_s[input];
        noXxxNoAxis1 = noXxxNoAxis1_s[input];
        noXxxNoColorBar = noXxxNoColorBar_s[input];
        noXxxNoCdmDataType = noXxxNoCdmDataType_s[input];
        noXxxNoLL = noXxxNoLL_s[input];
        noXxxNoLLEvenlySpaced = noXxxNoLLEvenlySpaced_s[input];
        noXxxNoLLGt1 = noXxxNoLLGt1_s[input];
        noXxxNoLLT = noXxxNoLLT_s[input];
        noXxxNoLonIn180 = noXxxNoLonIn180_s[input];
        noXxxNoNonString = noXxxNoNonString_s[input];
        noXxxNo2NonString = noXxxNo2NonString_s[input];
        noXxxNoStation = noXxxNoStation_s[input];
        noXxxNoStationID = noXxxNoStationID_s[input];
        noXxxNoSubsetVariables = noXxxNoSubsetVariables_s[input];
        noXxxNoOLLSubsetVariables = noXxxNoOLLSubsetVariables_s[input];
        noXxxNoMinMax = noXxxNoMinMax_s[input];
        noXxxItsGridded = noXxxItsGridded_s[input];
        noXxxItsTabular = noXxxItsTabular_s[input];
        oneRequestAtATime = oneRequestAtATime_s[input];
        openSearchDescription = openSearchDescription_s[input];
        optional = optional_s[input];
        options = options_s[input];
        orRefineSearchWith = orRefineSearchWith_s[input];
        orSearchWith = orSearchWith_s[input];
        orComma = orComma_s[input];
        outOfDateKeepTrack = outOfDateKeepTrack_s[input];
        outOfDateHtml = outOfDateHtml_s[input];

        patientData = patientData_s[input];
        patientYourGraph = patientYourGraph_s[input];
        percentEncode = percentEncode_s[input];
        pickADataset = pickADataset_s[input];
        protocolSearchHtml = protocolSearchHtml_s[input];
        protocolSearch2Html = protocolSearch2Html_s[input];
        protocolClick = protocolClick_s[input];
        
        queryError180 = queryError180_s[input];
        queryError1Value = queryError1Value_s[input];
        queryError1Var = queryError1Var_s[input];
        queryError2Var = queryError2Var_s[input];
        queryErrorActualRange = queryErrorActualRange_s[input];
        queryErrorAdjusted = queryErrorAdjusted_s[input];
        queryErrorAscending = queryErrorAscending_s[input];
        queryErrorConstraintNaN = queryErrorConstraintNaN_s[input];
        queryErrorEqualSpacing = queryErrorEqualSpacing_s[input];
        queryErrorExpectedAt = queryErrorExpectedAt_s[input];
        queryErrorFileType = queryErrorFileType_s[input];
        queryErrorInvalid = queryErrorInvalid_s[input];
        queryErrorLL = queryErrorLL_s[input];
        queryErrorLLGt1 = queryErrorLLGt1_s[input];
        queryErrorLLT = queryErrorLLT_s[input];
        queryErrorNeverTrue = queryErrorNeverTrue_s[input];
        queryErrorNeverBothTrue = queryErrorNeverBothTrue_s[input];
        queryErrorNotAxis = queryErrorNotAxis_s[input];
        queryErrorNotExpectedAt = queryErrorNotExpectedAt_s[input];
        queryErrorNotFoundAfter = queryErrorNotFoundAfter_s[input];
        queryErrorOccursTwice = queryErrorOccursTwice_s[input];
        queryErrorOrderByVariable = queryErrorOrderByVariable_s[input];
        queryErrorUnknownVariable = queryErrorUnknownVariable_s[input];
        queryErrorGrid1Axis = queryErrorGrid1Axis_s[input];
        queryErrorGridAmp = queryErrorGridAmp_s[input];

        queryErrorGridDiagnostic = queryErrorGridDiagnostic_s[input];
        queryErrorGridBetween = queryErrorGridBetween_s[input];
        queryErrorGridLessMin = queryErrorGridLessMin_s[input];
        queryErrorGridGreaterMax = queryErrorGridGreaterMax_s[input];
        queryErrorGridMissing = queryErrorGridMissing_s[input];
        queryErrorGridNoAxisVar = queryErrorGridNoAxisVar_s[input];
        queryErrorGridNoDataVar = queryErrorGridNoDataVar_s[input];
        queryErrorGridNotIdentical = queryErrorGridNotIdentical_s[input];
        queryErrorGridSLessS = queryErrorGridSLessS_s[input];
        queryErrorLastEndP = queryErrorLastEndP_s[input];
        queryErrorLastExpected = queryErrorLastExpected_s[input];
        queryErrorLastUnexpected = queryErrorLastUnexpected_s[input];
        queryErrorLastPMInvalid = queryErrorLastPMInvalid_s[input];
        queryErrorLastPMInteger = queryErrorLastPMInteger_s[input];
        rangesFromTo = rangesFromTo_s[input];
        resetTheForm = resetTheForm_s[input];
        resetTheFormWas = resetTheFormWas_s[input];
    
        restfulWebServices = restfulWebServices_s[input];
        restfulHTML = restfulHTML_s[input];
        restfulHTMLContinued = restfulHTMLContinued_s[input];
        restfulGetAllDataset = restfulGetAllDataset_s[input];
        restfulProtocols = restfulProtocols_s[input];
        SOSDocumentation = SOSDocumentation_s[input];
        WCSDocumentation = WCSDocumentation_s[input];
        WMSDocumentation = WMSDocumentation_s[input];
        requestFormatExamplesHtml = requestFormatExamplesHtml_s[input];
        resultsFormatExamplesHtml = resultsFormatExamplesHtml_s[input];
        resultsOfSearchFor = resultsOfSearchFor_s[input];
        restfulInformationFormats = restfulInformationFormats_s[input];
        restfulViaService = restfulViaService_s[input];
        rows = rows_s[input];
        rssNo = rssNo_s[input];
        searchTitle = searchTitle_s[input];
        searchDoFullTextHtml = searchDoFullTextHtml_s[input];
        searchFullTextHtml = searchFullTextHtml_s[input];
        searchHintsLuceneTooltip = searchHintsLuceneTooltip_s[input];
        searchHintsOriginalTooltip = searchHintsOriginalTooltip_s[input];
        searchHintsTooltip = searchHintsTooltip_s[input];
        searchButton = searchButton_s[input];
        searchClickTip = searchClickTip_s[input];
        searchMultipleERDDAPs = searchMultipleERDDAPs_s[input];
        searchMultipleERDDAPsDescription = searchMultipleERDDAPsDescription_s[input];
        searchNotAvailable = searchNotAvailable_s[input];
        searchTip = searchTip_s[input];
        searchSpelling = searchSpelling_s[input];
        searchFewerWords = searchFewerWords_s[input];
        searchWithQuery = searchWithQuery_s[input];
        seeProtocolDocumentation = seeProtocolDocumentation_s[input];
        selectNext = selectNext_s[input];
        selectPrevious = selectPrevious_s[input];
        shiftXAllTheWayLeft = shiftXAllTheWayLeft_s[input];
        shiftXLeft = shiftXLeft_s[input];
        shiftXRight = shiftXRight_s[input];
        shiftXAllTheWayRight = shiftXAllTheWayRight_s[input];
        sosDescriptionHtml = sosDescriptionHtml_s[input];
        sosLongDescriptionHtml = sosLongDescriptionHtml_s[input];
        sosOverview1 = sosOverview1_s[input];
        sosOverview2 = sosOverview2_s[input];
        //sparqlP01toP02pre = //sparqlP01toP02pre_s[input];
        //sparqlP01toP02post = //sparqlP01toP02post_s[input];
        ssUse = ssUse_s[input];
        ssUsePlain = ssUsePlain_s[input];
        ssBePatient = ssBePatient_s[input];
        ssInstructionsHtml = ssInstructionsHtml_s[input];
        statusHtml = statusHtml_s[input];
        submit = submit_s[input];
        submitTooltip = submitTooltip_s[input];
        subscriptionRSSHTML = subscriptionRSSHTML_s[input];
        subscriptionURLHTML = subscriptionURLHTML_s[input];
        subscriptionsTitle = subscriptionsTitle_s[input];
        subscriptionAdd = subscriptionAdd_s[input];
        subscriptionAddHtml = subscriptionAddHtml_s[input];
        subscriptionValidate = subscriptionValidate_s[input];
        subscriptionValidateHtml = subscriptionValidateHtml_s[input];
        subscriptionList = subscriptionList_s[input];
        subscriptionListHtml = subscriptionListHtml_s[input];
        subscriptionRemove = subscriptionRemove_s[input];
        subscriptionRemoveHtml = subscriptionRemoveHtml_s[input];
        subscriptionAbuse = subscriptionAbuse_s[input];
        subscriptionAddError = subscriptionAddError_s[input];
        subscriptionAdd2 = subscriptionAdd2_s[input];
        subscriptionAddSuccess = subscriptionAddSuccess_s[input];
        subscriptionEmail = subscriptionEmail_s[input];
        subscriptionEmailOnBlacklist = subscriptionEmailOnBlacklist_s[input];
        subscriptionEmailInvalid = subscriptionEmailInvalid_s[input];
        subscriptionEmailTooLong = subscriptionEmailTooLong_s[input];
        subscriptionEmailUnspecified = subscriptionEmailUnspecified_s[input];
        subscription0Html = subscription0Html_s[input];
        subscription1Html = subscription1Html_s[input];
        subscription2Html = subscription2Html_s[input];
        subscriptionIDInvalid = subscriptionIDInvalid_s[input];
        subscriptionIDTooLong = subscriptionIDTooLong_s[input];
        subscriptionIDUnspecified = subscriptionIDUnspecified_s[input];
        subscriptionKeyInvalid = subscriptionKeyInvalid_s[input];
        subscriptionKeyUnspecified = subscriptionKeyUnspecified_s[input];
        subscriptionListError = subscriptionListError_s[input];
        subscriptionListSuccess = subscriptionListSuccess_s[input];
        subscriptionRemoveError = subscriptionRemoveError_s[input];
        subscriptionRemove2 = subscriptionRemove2_s[input];
        subscriptionRemoveSuccess = subscriptionRemoveSuccess_s[input];
        subscriptionRSS = subscriptionRSS_s[input];
        subscriptionsNotAvailable = subscriptionsNotAvailable_s[input];
        subscriptionUrlHtml = subscriptionUrlHtml_s[input];
        subscriptionUrlInvalid = subscriptionUrlInvalid_s[input];
        subscriptionUrlTooLong = subscriptionUrlTooLong_s[input];
        subscriptionValidateError = subscriptionValidateError_s[input];
        subscriptionValidateSuccess = subscriptionValidateSuccess_s[input];
        subset = subset_s[input];
        subsetSelect = subsetSelect_s[input];
        subsetNMatching = subsetNMatching_s[input];
        subsetInstructions = subsetInstructions_s[input];
        subsetOption = subsetOption_s[input];
        subsetOptions = subsetOptions_s[input];
        subsetRefineMapDownload = subsetRefineMapDownload_s[input];
        subsetRefineSubsetDownload = subsetRefineSubsetDownload_s[input];
        subsetClickResetClosest = subsetClickResetClosest_s[input];
        subsetClickResetLL = subsetClickResetLL_s[input];
        subsetMetadata = subsetMetadata_s[input];
        subsetCount = subsetCount_s[input];
        subsetPercent = subsetPercent_s[input];
        subsetViewSelect = subsetViewSelect_s[input];
        subsetViewSelectDistinctCombos = subsetViewSelectDistinctCombos_s[input];
        subsetViewSelectRelatedCounts = subsetViewSelectRelatedCounts_s[input];
        subsetWhen = subsetWhen_s[input];
        subsetWhenNoConstraints = subsetWhenNoConstraints_s[input];
        subsetWhenCounts = subsetWhenCounts_s[input];
        subsetComboClickSelect = subsetComboClickSelect_s[input];
        subsetNVariableCombos = subsetNVariableCombos_s[input];
        subsetShowingAllRows = subsetShowingAllRows_s[input];
        subsetShowingNRows = subsetShowingNRows_s[input];
        subsetChangeShowing = subsetChangeShowing_s[input];
        subsetNRowsRelatedData = subsetNRowsRelatedData_s[input];
        subsetViewRelatedChange = subsetViewRelatedChange_s[input];
        subsetTotalCount = subsetTotalCount_s[input];
        subsetView = subsetView_s[input];
        subsetViewCheck = subsetViewCheck_s[input];
        subsetViewCheck1 = subsetViewCheck1_s[input];
        subsetViewDistinctMap = subsetViewDistinctMap_s[input];
        subsetViewRelatedMap = subsetViewRelatedMap_s[input];
        subsetViewDistinctDataCounts = subsetViewDistinctDataCounts_s[input];
        subsetViewDistinctData = subsetViewDistinctData_s[input];
        subsetViewRelatedDataCounts = subsetViewRelatedDataCounts_s[input];
        subsetViewRelatedData = subsetViewRelatedData_s[input];
        subsetViewDistinctMapTooltip = subsetViewDistinctMapTooltip_s[input];
        subsetViewRelatedMapTooltip = subsetViewRelatedMapTooltip_s[input];
        subsetViewDistinctDataCountsTooltip = subsetViewDistinctDataCountsTooltip_s[input];
        subsetViewDistinctDataTooltip = subsetViewDistinctDataTooltip_s[input];
        subsetViewRelatedDataCountsTooltip = subsetViewRelatedDataCountsTooltip_s[input];
        subsetViewRelatedDataTooltip = subsetViewRelatedDataTooltip_s[input];
        subsetWarn = subsetWarn_s[input];
        subsetWarn10000 = subsetWarn10000_s[input];
        subsetTooltip = subsetTooltip_s[input];
        subsetNotSetUp = subsetNotSetUp_s[input];
        subsetLongNotShown = subsetLongNotShown_s[input];

        tabledapVideoIntro = tabledapVideoIntro_s[input];
        theLongDescriptionHtml = theLongDescriptionHtml_s[input];
        Then = Then_s[input];
        time = time_s[input];
        timeoutOtherRequests = timeoutOtherRequests_s[input];
        units = units_s[input];
        unknownDatasetID = unknownDatasetID_s[input];
        unknownProtocol = unknownProtocol_s[input];
        unsupportedFileType = unsupportedFileType_s[input];

        variableNames = variableNames_s[input];
        viewAllDatasetsHtml = viewAllDatasetsHtml_s[input];
    
        warning = warning_s[input];

        wcsDescriptionHtml = wcsDescriptionHtml_s[input];
        wcsLongDescriptionHtml = wcsLongDescriptionHtml_s[input];
        wcsOverview1 = wcsOverview1_s[input];
        wcsOverview2 = wcsOverview2_s[input];

        wmsDescriptionHtml = wmsDescriptionHtml_s[input];
        WMSDocumentation1 = WMSDocumentation1_s[input];
        WMSGetCapabilities = WMSGetCapabilities_s[input];
        WMSGetMap = WMSGetMap_s[input];
        WMSNotes = WMSNotes_s[input];
        wmsInstructions = wmsInstructions_s[input];
        wmsLongDescriptionHtml = wmsLongDescriptionHtml_s[input];
        wmsManyDatasets = wmsManyDatasets_s[input];
        
        zoomIn = zoomIn_s[input];
        zoomOut = zoomOut_s[input];


        ampLoginInfoPo = ampLoginInfoPo_s[input];
        acceptEncodingHtml = acceptEncodingHtml_s[input];
        filesDocumentation = filesDocumentation_s[input];
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
