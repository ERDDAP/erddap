/* 
 * EDStatic Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.security.Principal;
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
     * See Changes information in /downloads/setup.html .
     * <br>0.1 started on 2007-09-17
     * <br>0.11 released on 2007-11-09
     * <br>0.12 released on 2007-12-05
     * <br>0.2 released on 2008-01-10
     * <br>0.21 was used during development of 0.22
     * <br>0.22 released on 2008-02-21
     * <br>0.23 is being used during development of 0.24
     * <br>0.24 released on 2008-03-03
     * <br>0.25 is being used during development of 0.26
     * <br>0.26 released on 2008-03-11
     * <br>0.27 is being used during development 
     * <br>0.28 released on 2008-04-14
     * <br>0.29 is being used during development 
     * <br>1.00 released on 2008-05-06
     * <br>1.01 is being used during development 
     * <br>1.02 released on 2008-05-26
     * <br>1.03 is being used during development 
     * <br>1.04 released on 2008-06-10
     * <br>1.05 is being used during development 
     * <br>1.06 released on 2008-06-20
     * <br>1.07 is being used during development 
     * <br>1.08 released on 2008-07-13
     * <br>1.09 is being used during development 
     * <br>1.10 released on 2008-10-14
     * <br>1.11 is being used during development 
     * <br>1.12 released on 2008-11-02
     * <br>1.13 is being used during development 
     * <br>1.14 released on 2009-03-17
     * <br>1.15 is being used during development 
     * <br>1.16 released on 2009-03-26
     * <br>1.17 is being used during development 
     * <br>1.18 released on 2009-04-08
     * <br>1.19 is being used during development 
     * <br>1.20 released on 2009-07-02
     * <br>1.21 is being used during development 
     * <br>1.22 released on 2009-07-05
     * <br>1.23 is being used during development 
     * <br>1.24 released on 2010-08-06
     * <br>1.25 is being used during development
     * <br>1.26 released on 2010-08-25
     * <br>1.27 is being used during development
     * <br>1.28 released on 2010-08-27
     * <br>1.29 is being used during development
     * <br>1.30 released on 2011-04-29
     * <br>1.31 is being used during development
     * <br>1.32 released on 2011-05-20
     * <br>1.33 is being used during development
     * <br>1.34 released on 2011-06-15
     * <br>1.35 is being used during development
     * <br>1.36 released on 2011-08-01
     * <br>1.37 is being used during development
     */   
    public static String erddapVersion = "1.36";  

    /** 
     * This is almost always false.  
     * During development, Bob sets this to true. No one else needs to. 
     * If true, ERDDAP uses setup2.xml, messages2.xml, and datasets2.xml. 
     */
public static boolean developmentMode = false;

    /** 
     * contentDirectory is the local directory on this computer, e.g., [tomcat]/content/erddap/ 
     * It will have a slash at the end.
     */
    public static String contentDirectory;

    public final static String INSTITUTION = "institution";

    /* contextDirectory is the local directory on this computer, e.g., [tomcat]/webapps/erddap/ */
    public static String contextDirectory = SSR.getContextDirectory();
    public final static String DOWNLOAD_DIR = "download/";
    public final static String IMAGES_DIR   = "images/";
    public final static String PUBLIC_DIR   = "public/"; 
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
    public static ConcurrentHashMap runningThreads = new ConcurrentHashMap(); 

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
    public static ConcurrentHashMap lastAssignedTask = new ConcurrentHashMap(); 
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
    public static ConcurrentHashMap convertToPublicSourceUrl = new ConcurrentHashMap();

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
        warName;
    public static String ampLoginInfo = "&loginInfo;";
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
        subscriptionSystemActive,
        sosActive, wcsActive,
        variablesMustHaveIoosCategory,
        verbose;
    public static String  categoryAttributes[];       //as it appears in metadata (and used for hashmap)
    public static String  categoryAttributesInURLs[]; //fileNameSafe (as used in URLs)
    public static boolean categoryIsGlobal[];
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
        fullDatasetInfoDirectory,
        fullCacheDirectory,
        fullLogsDirectory,
        fullCopyDirectory,
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


    /** These values are loaded from the [contentDirectory]messages.xml file. */
    public static String 
        advancedSearchDirections,
        advancedSearchHtml,
        categoryTitleHtml,
        category1Html,
        category2Html,
        category3Html,
        categorySearchHtml,
        categorySearchDifferentHtml,
        categoryClickHtml,
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
        convertTime,
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
        dataAccessNotAllowed,
        distinctValuesHtml,

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

        EDDClickOnSubmitHtml,
        EDDInstructions,
        EDDDownloadData,
        EDDMakeAGraph,
        EDDMakeAMap,
        EDDFileType,
        EDDSelectFileType,
        EDDMinimum,
        EDDMaximum,

        EDDGridDapDescription,
        EDDGridDapLongDescription,
        EDDGridDataAccessFormHtml,
        EDDGridDownloadDataHtml,
        EDDGridDimension,
        EDDGridFirst,
        EDDGridLast,
        EDDGridStart,
        EDDGridStop,
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
        EDDTableMakeANumericConstraintHtml,
        EDDTableMakeAStringConstraintHtml,
        EDDTableMakeATimeConstraintHtml,
        EDDTableConstraintHtml,
        EDDTableDapDescription,
        EDDTableDapLongDescription,
        EDDTableDataAccessFormHtml,
        EDDTableDownloadDataHtml,

        errorTitle,
        errorRequestUrl,
        errorRequestQuery,
        errorTheError,
        functionHtml,
        functionDistinctHtml,
        functionOrderByHtml,
        functionOrderByMaxHtml,
        getStartedHtml,
        infoTableTitleHtml,
        justGenerateAndView,
        justGenerateAndViewHtml,
        justGenerateAndViewUrl,
        justGenerateAndViewGraphUrlHtml,
        nDatasetsListed,
        noDatasetWith,
        palettes[],
        palettes0[],
        paletteSections[] = {
            "","2","3","4","5","6","7","8","9",
            "10","11","12","13","14","15","16","17","18","19",
            "20","21","22","23","24","25","26","27","28","29",
            "30","31","32","33","34","35","36","37","38","39", "40"},
        patientData,
        patientYourGraph,
        protocolSearchHtml,
        protocolSearch2Html,
        protocolClick,
        resetTheForm,
        requestFormatExamplesHtml,
        resultsFormatExamplesHtml,
        resultsOfSearchFor,
        searchFullTextHtml,
        searchButton,
        searchClickTip,
        searchTip,
        searchRelevantAreFirst,
        searchSpelling,
        searchFewerWords,
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
        subscriptionAbuse,
        subscriptionAddError,
        subscriptionAdd2,
        subscriptionAddSuccess,
        subscriptionEmail,
        subscriptionEmailInvalid,
        subscriptionEmailTooLong,
        subscriptionEmailUnspecified,
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
        subscriptionUrlHtml,
        subscriptionUrlInvalid,
        subscriptionUrlTooLong,        
        subscriptionValidateError,
        subscriptionValidateSuccess,
        THERE_IS_NO_DATA,
        thereIsTooMuchData,
        viewAllDatasetsHtml,
        waitThenTryAgain,

        sosDescriptionHtml,
        sosLongDescriptionHtml,
        wcsDescriptionHtml,
        wcsLongDescriptionHtml,
        wmsDescriptionHtml,
        wmsInstructions,
        wmsLongDescriptionHtml,
        yRangeHtml, 
        yRangeMinHtml,
        yRangeMaxHtml;

    public static int[] imageWidths, imageHeights, pdfWidths, pdfHeights;
    private static String 
        searchHintsHtml, 
        subscriptionHtml, subscription2Html, 
        subscriptionAddHtml, subscriptionListHtml, 
        subscriptionRemoveHtml, subscriptionValidateHtml,
        subscriptionsNotAvailable,
        theShortDescriptionHtml, theLongDescriptionHtml; //see the xxx() methods
    public static String errorFromDataSource = String2.ERROR + " from data source: ";


    /** This static block reads this class's static String values from
     * contentDirectory, which must contain messages.xml, setup.xml, and datasets.xml.
     * It may be a defined environment variable (erddapContent)
     * or a subdir of <tomcat> (e.g., usr/local/tomcat/content/erddap/)
     *   (more specifically, a sibling of 'tomcat'/webapps).
     *
     * @throws RuntimeException if trouble
     */
    static {

        //*** Set up logging systems.
        //route calls to a logger to com.cohort.util.String2Log
        String2.setupCommonsLogging(-1);

        //**** find contentDirectory
        try {
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
        } catch (Throwable t) {
            throw new RuntimeException(
                "Couldn't find 'content' directory (<tomcat>/content/erddap/) " +
                "because 'erddapContent' environment variable not found " +
                "and couldn't find '/webapps/' in classPath=" + String2.getClassPath() +
                " (and 'content/erddap' should be a sibling of <tomcat>/webapps).\n" + t.toString());
        }
        Test.ensureTrue(File2.isDirectory(contentDirectory),  
            "contentDirectory (" + contentDirectory + ") doesn't exist.");


        //**** setup.xml *************************************************************
        //read static Strings from setup.xml 
        String setupFileName = contentDirectory + 
            "setup" + (developmentMode? "2" : "") + ".xml";
        String errorInMethod = "EDStatic error while reading " + setupFileName + ": \n";
        ResourceBundle2 setup = null;
        try {
            setup = ResourceBundle2.fromXml(XML.parseXml(setupFileName, false));
        } catch (Throwable t) {
            throw new RuntimeException(errorInMethod, t);
        }

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
        PathCartesianRenderer.reallyVerbose = reallyVerbose;
        SgtGraph.reallyVerbose = reallyVerbose;
        SgtMap.reallyVerbose = reallyVerbose;
        SgtUtil.reallyVerbose = reallyVerbose;
        SSR.reallyVerbose = reallyVerbose;
        Subscriptions.reallyVerbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;
        TaskThread.reallyVerbose = reallyVerbose;

        bigParentDirectory = setup.getNotNothingString("bigParentDirectory", errorInMethod); 
        File2.addSlash(bigParentDirectory);
        Test.ensureTrue(File2.isDirectory(bigParentDirectory),  
            errorInMethod + "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");


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
        fullDatasetInfoDirectory = bigParentDirectory + "datasetInfo/";
        fullCacheDirectory       = bigParentDirectory + "cache/";
        fullResetFlagDirectory   = bigParentDirectory + "flag/";
        fullLogsDirectory        = bigParentDirectory + "logs/";
        fullCopyDirectory        = bigParentDirectory + "copy/";

        Test.ensureTrue(File2.isDirectory(fullPaletteDirectory),  
            errorInMethod + "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
        Test.ensureTrue(File2.isDirectory(fullPublicDirectory),  
            errorInMethod + "fullPublicDirectory (" + fullPublicDirectory + ") doesn't exist.");
        errorInMethod = "EDStatic error while creating directories.\n";
        File2.makeDirectory(fullDatasetInfoDirectory);
        File2.makeDirectory(fullCacheDirectory);
        File2.makeDirectory(fullResetFlagDirectory);
        File2.makeDirectory(fullLogsDirectory);
        File2.makeDirectory(fullCopyDirectory);

        //set up log (after fullLogsDirectory is known)
        errorInMethod = "EDStatic error while setting up log files: \n";
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
        try {
            //open String2 log system
            String2.setupLog(false, false, 
                fullLogsDirectory + "log.txt",
                true, true, 5000000);
        } catch (Throwable t) {
            throw new RuntimeException(errorInMethod, t);
        }
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
        //remove this code mid-2009?
        File2.deleteAllFiles(bigParentDirectory + "private", true, true); //empty it
        File2.delete(bigParentDirectory + "private"); //delete it

        //deal with cache
        //how many millis should files be left in the cache (if untouched)?
        cacheMillis = setup.getInt("cacheMinutes", 60) * 60000L; // millis/min
        displayDiagnosticInfo = setup.getBoolean("displayDiagnosticInfo", false);

        //on start up, always delete all files from fullPublicDirectory and fullCacheDirectory
        File2.deleteAllFiles(fullPublicDirectory, true, false);  
        File2.deleteAllFiles(fullCacheDirectory, true, false);

        //make some subdirectories of fullCacheDirectory
        //'_' distinguishes from dataset cache dirs
        errorInMethod = "EDStatic error while creating directories: \n";
        fullCptCacheDirectory              = fullCacheDirectory + "_cpt/";   
        fullPlainFileNcCacheDirectory      = fullCacheDirectory + "_plainFileNc/";   
        fullSgtMapTopographyCacheDirectory = fullCacheDirectory + "_SgtMapTopography/";
        fullTestCacheDirectory             = fullCacheDirectory + "_test/";
        fullWmsCacheDirectory              = fullCacheDirectory + "_wms/"; //for all-datasets WMS and subdirs for non-data layers
        File2.makeDirectory(fullCptCacheDirectory);
        File2.makeDirectory(fullPlainFileNcCacheDirectory);
        File2.makeDirectory(fullSgtMapTopographyCacheDirectory);
//2009-11-01 delete old dir name for topography; but someday remove this (only needed once)
File2.deleteAllFiles(fullCacheDirectory + "_SgtMapBathymetry", false, true);
File2.delete(fullCacheDirectory + "_SgtMapBathymetry"); 
        File2.makeDirectory(fullTestCacheDirectory);
        File2.makeDirectory(fullWmsCacheDirectory);
        File2.makeDirectory(fullWmsCacheDirectory + "Land");  //includes LandMask
        File2.makeDirectory(fullWmsCacheDirectory + "Coastlines");
        File2.makeDirectory(fullWmsCacheDirectory + "LakesAndRivers");
        File2.makeDirectory(fullWmsCacheDirectory + "Nations");
        File2.makeDirectory(fullWmsCacheDirectory + "States");

        //get other info from setup.xml
        errorInMethod = "EDStatic error while reading " + setupFileName + ": \n";
        baseUrl                    = setup.getNotNothingString("baseUrl",                    errorInMethod);
        baseHttpsUrl               = setup.getString(          "baseHttpsUrl",               "(not specified)");
        categoryAttributes         = String2.split(setup.getNotNothingString("categoryAttributes", errorInMethod), ',');
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

        EDDGridIdExample           = setup.getNotNothingString("EDDGridIdExample",           errorInMethod);
        EDDGridDimensionExample    = setup.getNotNothingString("EDDGridDimensionExample",    errorInMethod);
        EDDGridNoHyperExample      = setup.getNotNothingString("EDDGridNoHyperExample",      errorInMethod);
        EDDGridDimNamesExample     = setup.getNotNothingString("EDDGridDimNamesExample",     errorInMethod);
        EDDGridDataIndexExample    = setup.getNotNothingString("EDDGridDataIndexExample",    errorInMethod);
        EDDGridDataValueExample    = setup.getNotNothingString("EDDGridDataValueExample",    errorInMethod);
        EDDGridDataTimeExample     = setup.getNotNothingString("EDDGridDataTimeExample",     errorInMethod);
        EDDGridGraphExample        = setup.getNotNothingString("EDDGridGraphExample",        errorInMethod);
        EDDGridMapExample          = setup.getNotNothingString("EDDGridMapExample",          errorInMethod);
        EDDGridMatlabPlotExample   = setup.getNotNothingString("EDDGridMatlabPlotExample",   errorInMethod);

        EDDTableIdExample          = setup.getNotNothingString("EDDTableIdExample",          errorInMethod);
        EDDTableVariablesExample   = setup.getNotNothingString("EDDTableVariablesExample",   errorInMethod);
        EDDTableConstraintsExample = setup.getNotNothingString("EDDTableConstraintsExample", errorInMethod);
        EDDTableDataValueExample   = setup.getNotNothingString("EDDTableDataValueExample",   errorInMethod);
        EDDTableDataTimeExample    = setup.getNotNothingString("EDDTableDataTimeExample",    errorInMethod);
        EDDTableGraphExample       = setup.getNotNothingString("EDDTableGraphExample",       errorInMethod);
        EDDTableMapExample         = setup.getNotNothingString("EDDTableMapExample",         errorInMethod);
        EDDTableMatlabPlotExample  = setup.getNotNothingString("EDDTableMatlabPlotExample",  errorInMethod);

        adminInstitution           = setup.getNotNothingString("adminInstitution",           errorInMethod);
        adminIndividualName        = setup.getNotNothingString("adminIndividualName",        errorInMethod);
        adminPosition              = setup.getNotNothingString("adminPosition",              errorInMethod);
        adminPhone                 = setup.getNotNothingString("adminPhone",                 errorInMethod); 
        adminAddress               = setup.getNotNothingString("adminAddress",               errorInMethod);
        adminCity                  = setup.getNotNothingString("adminCity",                  errorInMethod);
        adminStateOrProvince       = setup.getNotNothingString("adminStateOrProvince",       errorInMethod); 
        adminPostalCode            = setup.getNotNothingString("adminPostalCode",            errorInMethod);
        adminCountry               = setup.getNotNothingString("adminCountry",               errorInMethod);
        adminEmail                 = setup.getNotNothingString("adminEmail",                 errorInMethod);

        accessConstraints          = setup.getNotNothingString("accessConstraints",          errorInMethod); 
        accessRequiresAuthorization= setup.getNotNothingString("accessRequiresAuthorization",errorInMethod); 
        fees                       = setup.getNotNothingString("fees",                       errorInMethod);
        keywords                   = setup.getNotNothingString("keywords",                   errorInMethod);
        legal                      = setup.getNotNothingString("legal",                      errorInMethod);
        units_standard             = setup.getString(          "units_standard",             "UDUNITS");

        sosActive                  = setup.getBoolean(         "sosActive",                  false); 
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
wcsActive = false;         
//        wcsActive                  = setup.getBoolean(         "wcsActive",                  false); 

        wmsSampleDatasetID         = setup.getNotNothingString("wmsSampleDatasetID",         errorInMethod);
        wmsSampleVariable          = setup.getNotNothingString("wmsSampleVariable",          errorInMethod);
        wmsSampleBBox              = setup.getNotNothingString("wmsSampleBBox",              errorInMethod);

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
        endBodyHtml                = setup.getNotNothingString("endBodyHtml",                errorInMethod);
        endBodyHtml                = String2.replaceAll(endBodyHtml, "&erddapVersion;", erddapVersion);
        flagKeyKey                 = setup.getString(          "flagKeyKey",                 "");
        if (flagKeyKey == null || flagKeyKey.length() == 0)  flagKeyKey = "flagKeyKey";
        fontFamily                 = setup.getString(          "fontFamily",                 "SansSerif");
        googleEarthLogoFile        = setup.getNotNothingString("googleEarthLogoFile",        errorInMethod);
        highResLogoImageFile       = setup.getNotNothingString("highResLogoImageFile",       errorInMethod);
        legendTitle1               = setup.getString(          "legendTitle1",               null);
        legendTitle2               = setup.getString(          "legendTitle2",               null);
        listPrivateDatasets        = setup.getBoolean(         "listPrivateDatasets",        false);
        loadDatasetsMinMillis      = Math.max(1,setup.getInt(  "loadDatasetsMinMinutes",     15)) * 60000L;
        loadDatasetsMaxMillis      = setup.getInt(             "loadDatasetsMaxMinutes",     60) * 60000L;
        loadDatasetsMaxMillis      = Math.max(loadDatasetsMinMillis * 2, loadDatasetsMaxMillis);
        lowResLogoImageFile        = setup.getNotNothingString("lowResLogoImageFile",        errorInMethod);
        partialRequestMaxBytes     = setup.getInt(             "partialRequestMaxBytes",     partialRequestMaxBytes);
        partialRequestMaxCells     = setup.getInt(             "partialRequestMaxCells",     partialRequestMaxCells);
        questionMarkImageFile      = setup.getNotNothingString("questionMarkImageFile",      errorInMethod);
        passwordEncoding           = setup.getString(          "passwordEncoding",           "UEPMD5");
        startBodyHtml              = setup.getNotNothingString("startBodyHtml",              errorInMethod);
        startHeadHtml              = setup.getNotNothingString("startHeadHtml",              errorInMethod);
        subscriptionSystemActive   = setup.getBoolean(         "subscriptionSystemActive",   true);
        theShortDescriptionHtml    = setup.getNotNothingString("theShortDescriptionHtml",    errorInMethod);
        unusualActivity            = setup.getInt(             "unusualActivity",            unusualActivity);
        variablesMustHaveIoosCategory = setup.getBoolean(      "variablesMustHaveIoosCategory", true);
        warName                    = setup.getNotNothingString("warName",                    errorInMethod);
 
        try {
            //copy all <contentDirectory>images/ (and subdirectories) files to imageDir (and subdirectories)
            errorInMethod = "EDStatic error while copying " + contentDirectory + "images/ : \n";
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
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }

        errorInMethod = "EDStatic error while initializing SgtGraph: \n";
        sgtGraph = new SgtGraph(fontFamily);

        //ensure authentication setup is okay
        errorInMethod = "EDStatic error while checking authentication setup: \n";
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
            errorInMethod = "EDStatic error while initializing Subscriptions: \n";
            try {
                //make subscriptions
                subscriptions = new Subscriptions(
                    bigParentDirectory + "subscriptionsV1.txt", 48, //maxHoursPending, 
                    erddapUrl); //always use non-https url                
            } catch (Throwable t) {
                throw new RuntimeException(MustBe.throwableToString(t));
            }
        }


        //**** messages.xml *************************************************************
        //read static Strings from messages.xml 
        String messagesFileName = contentDirectory + 
            "messages" + (developmentMode? "2" : "") + ".xml";
        errorInMethod = "messages.xml error while reading " + messagesFileName + ": \n";
        ResourceBundle2 messages = null;
        try {
            messages = ResourceBundle2.fromXml(XML.parseXml(messagesFileName, false));
        } catch (Throwable t) {
            throw new RuntimeException(errorInMethod, t);
        }

        //read all the static Strings from messages.xml
        advancedSearchDirections   = messages.getNotNothingString("advancedSearchDirections",   errorInMethod);
        advancedSearchHtml         = messages.getNotNothingString("advancedSearchHtml",         errorInMethod);
        categoryTitleHtml          = messages.getNotNothingString("categoryTitleHtml",          errorInMethod);
        category1Html              = messages.getNotNothingString("category1Html",              errorInMethod);
        category2Html              = messages.getNotNothingString("category2Html",              errorInMethod);
        category3Html              = messages.getNotNothingString("category3Html",              errorInMethod);
        categorySearchHtml         = messages.getNotNothingString("categorySearchHtml",         errorInMethod);
        categorySearchDifferentHtml= messages.getNotNothingString("categorySearchDifferentHtml",errorInMethod);
        categoryClickHtml          = messages.getNotNothingString("categoryClickHtml",          errorInMethod);
        clickAccessHtml            = messages.getNotNothingString("clickAccessHtml",            errorInMethod);
        clickAccess                = messages.getNotNothingString("clickAccess",                errorInMethod);
        clickBackgroundInfo        = messages.getNotNothingString("clickBackgroundInfo",        errorInMethod);
        clickInfo                  = messages.getNotNothingString("clickInfo",                  errorInMethod);
        clickToSubmit              = messages.getNotNothingString("clickToSubmit",              errorInMethod);
        convertFipsCounty          = messages.getNotNothingString("convertFipsCounty",          errorInMethod);
        convertFipsCountyIntro     = messages.getNotNothingString("convertFipsCountyIntro",     errorInMethod);
        convertFipsCountyNotes     = messages.getNotNothingString("convertFipsCountyNotes",     errorInMethod);
        convertFipsCountyService   = messages.getNotNothingString("convertFipsCountyService",   errorInMethod);
        convertHtml                = messages.getNotNothingString("convertHtml",                errorInMethod);
        convertTime                = messages.getNotNothingString("convertTime",                errorInMethod);
        convertTimeIntro           = messages.getNotNothingString("convertTimeIntro",           errorInMethod);
        convertTimeNotes           = messages.getNotNothingString("convertTimeNotes",           errorInMethod);
        convertTimeService         = messages.getNotNothingString("convertTimeService",         errorInMethod);
        convertTimeUnitsHelp       = messages.getNotNothingString("convertTimeUnitsHelp",       errorInMethod);
        convertUnits               = messages.getNotNothingString("convertUnits",               errorInMethod);
        convertUnitsComparison     = messages.getNotNothingString("convertUnitsComparison",     errorInMethod);
        convertUnitsFilter         = messages.getNotNothingString("convertUnitsFilter",         errorInMethod);
        convertUnitsIntro          = messages.getNotNothingString("convertUnitsIntro",          errorInMethod);
        convertUnitsNotes          = messages.getNotNothingString("convertUnitsNotes",          errorInMethod);
        convertUnitsService        = messages.getNotNothingString("convertUnitsService",        errorInMethod);
        dataAccessNotAllowed       = messages.getNotNothingString("dataAccessNotAllowed",       errorInMethod);
        distinctValuesHtml         = messages.getNotNothingString("distinctValuesHtml",         errorInMethod);

        dtAccessible               = messages.getNotNothingString("dtAccessible",               errorInMethod);
        dtAccessibleYes            = messages.getNotNothingString("dtAccessibleYes",            errorInMethod);
        dtAccessibleNo             = messages.getNotNothingString("dtAccessibleNo",             errorInMethod);
        dtAccessibleLogIn          = messages.getNotNothingString("dtAccessibleLogIn",          errorInMethod);
        dtLogIn                    = messages.getNotNothingString("dtLogIn",                    errorInMethod);
        dtDAF1                     = messages.getNotNothingString("dtDAF1",                     errorInMethod);
        dtDAF2                     = messages.getNotNothingString("dtDAF2",                     errorInMethod);
        dtMAG                      = messages.getNotNothingString("dtMAG",                      errorInMethod);
        dtSOS                      = messages.getNotNothingString("dtSOS",                      errorInMethod);
        dtSubset                   = messages.getNotNothingString("dtSubset",                   errorInMethod);
        dtWCS                      = messages.getNotNothingString("dtWCS",                      errorInMethod);
        dtWMS                      = messages.getNotNothingString("dtWMS",                      errorInMethod);
        
        EDDClickOnSubmitHtml       = messages.getNotNothingString("EDDClickOnSubmitHtml",       errorInMethod);
        EDDInstructions            = messages.getNotNothingString("EDDInstructions",            errorInMethod);
        EDDDownloadData            = messages.getNotNothingString("EDDDownloadData",            errorInMethod);
        EDDMakeAGraph              = messages.getNotNothingString("EDDMakeAGraph",              errorInMethod);
        EDDMakeAMap                = messages.getNotNothingString("EDDMakeAMap",                errorInMethod);
        EDDFileType                = messages.getNotNothingString("EDDFileType",                errorInMethod);
        EDDSelectFileType          = messages.getNotNothingString("EDDSelectFileType",          errorInMethod);
        EDDMinimum                 = messages.getNotNothingString("EDDMinimum",                 errorInMethod);
        EDDMaximum                 = messages.getNotNothingString("EDDMaximum",                 errorInMethod);

        EDDGridDownloadTooltipHtml = messages.getNotNothingString("EDDGridDownloadTooltipHtml", errorInMethod);
        EDDGridDapDescription      = messages.getNotNothingString("EDDGridDapDescription",      errorInMethod);
        EDDGridDapLongDescription  = messages.getNotNothingString("EDDGridDapLongDescription",  errorInMethod);
        EDDGridDataAccessFormHtml  = messages.getNotNothingString("EDDGridDataAccessFormHtml",  errorInMethod);
        EDDGridDownloadDataHtml    = messages.getNotNothingString("EDDGridDownloadDataHtml",    errorInMethod);
        EDDGridDimension           = messages.getNotNothingString("EDDGridDimension",           errorInMethod);
        EDDGridFirst               = messages.getNotNothingString("EDDGridFirst",               errorInMethod);
        EDDGridLast                = messages.getNotNothingString("EDDGridLast",                errorInMethod);
        EDDGridStart               = messages.getNotNothingString("EDDGridStart",               errorInMethod);
        EDDGridStop                = messages.getNotNothingString("EDDGridStop",                errorInMethod);
        EDDGridStride              = messages.getNotNothingString("EDDGridStride",              errorInMethod);
        EDDGridNValues             = messages.getNotNothingString("EDDGridNValues",             errorInMethod);
        EDDGridNValuesHtml         = messages.getNotNothingString("EDDGridNValuesHtml",         errorInMethod);
        EDDGridSpacing             = messages.getNotNothingString("EDDGridSpacing",             errorInMethod);
        EDDGridJustOneValue        = messages.getNotNothingString("EDDGridJustOneValue",        errorInMethod);
        EDDGridEven                = messages.getNotNothingString("EDDGridEven",                errorInMethod);
        EDDGridUneven              = messages.getNotNothingString("EDDGridUneven",              errorInMethod);
        EDDGridDimensionHtml       = messages.getNotNothingString("EDDGridDimensionHtml",       errorInMethod);
        EDDGridDimensionFirstHtml  = messages.getNotNothingString("EDDGridDimensionFirstHtml",  errorInMethod);
        EDDGridDimensionLastHtml   = messages.getNotNothingString("EDDGridDimensionLastHtml",   errorInMethod);
        EDDGridVarHasDimHtml       = messages.getNotNothingString("EDDGridVarHasDimHtml",       errorInMethod);
        EDDGridSSSHtml             = messages.getNotNothingString("EDDGridSSSHtml",             errorInMethod);
        EDDGridStartHtml           = messages.getNotNothingString("EDDGridStartHtml",           errorInMethod);
        EDDGridStopHtml            = messages.getNotNothingString("EDDGridStopHtml",            errorInMethod);
        EDDGridStrideHtml          = messages.getNotNothingString("EDDGridStrideHtml",          errorInMethod);
        EDDGridSpacingHtml         = messages.getNotNothingString("EDDGridSpacingHtml",         errorInMethod);
        EDDGridGridVariableHtml    = messages.getNotNothingString("EDDGridGridVariableHtml",    errorInMethod);

        EDDTableDapDescription     = messages.getNotNothingString("EDDTableDapDescription",     errorInMethod);
        EDDTableDapLongDescription = messages.getNotNothingString("EDDTableDapLongDescription", errorInMethod);
        EDDTableDataAccessFormHtml = messages.getNotNothingString("EDDTableDataAccessFormHtml", errorInMethod);
        EDDTableDownloadDataHtml   = messages.getNotNothingString("EDDTableDownloadDataHtml",   errorInMethod);
        EDDTableTabularDatasetHtml = messages.getNotNothingString("EDDTableTabularDatasetHtml", errorInMethod);
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
        EDDTableMakeANumericConstraintHtml= messages.getNotNothingString("EDDTableMakeANumericConstraintHtml",errorInMethod);
        EDDTableMakeAStringConstraintHtml=  messages.getNotNothingString("EDDTableMakeAStringConstraintHtml", errorInMethod);
        EDDTableMakeATimeConstraintHtml=    messages.getNotNothingString("EDDTableMakeATimeConstraintHtml",   errorInMethod);
        EDDTableConstraintHtml     = messages.getNotNothingString("EDDTableConstraintHtml",     errorInMethod);

        errorTitle                 = messages.getNotNothingString("errorTitle",                 errorInMethod);
        errorRequestUrl            = messages.getNotNothingString("errorRequestUrl",            errorInMethod);
        errorRequestQuery          = messages.getNotNothingString("errorRequestQuery",          errorInMethod);
        errorTheError              = messages.getNotNothingString("errorTheError",              errorInMethod);
        functionHtml               = messages.getNotNothingString("functionHtml",               errorInMethod);
        functionDistinctHtml       = messages.getNotNothingString("functionDistinctHtml",       errorInMethod);
        functionOrderByHtml        = messages.getNotNothingString("functionOrderByHtml",        errorInMethod);
        functionOrderByMaxHtml     = messages.getNotNothingString("functionOrderByMaxHtml",     errorInMethod);
        getStartedHtml             = messages.getNotNothingString("getStartedHtml",             errorInMethod);
        imageWidths                = String2.toIntArray(String2.split(messages.getNotNothingString("imageWidths",  errorInMethod), ','));
        imageHeights               = String2.toIntArray(String2.split(messages.getNotNothingString("imageHeights", errorInMethod), ','));
        infoTableTitleHtml         = messages.getNotNothingString("infoTableTitleHtml",         errorInMethod);
        justGenerateAndView        = messages.getNotNothingString("justGenerateAndView",        errorInMethod);
        justGenerateAndViewHtml    = messages.getNotNothingString("justGenerateAndViewHtml",    errorInMethod);
        justGenerateAndViewUrl     = messages.getNotNothingString("justGenerateAndViewUrl",     errorInMethod);
        justGenerateAndViewGraphUrlHtml = messages.getNotNothingString("justGenerateAndViewGraphUrlHtml", errorInMethod);
        nDatasetsListed            = messages.getNotNothingString("nDatasetsListed",            errorInMethod);
        noDatasetWith              = messages.getNotNothingString("noDatasetWith",              errorInMethod);
        palettes                   = String2.split(messages.getNotNothingString("palettes",     errorInMethod), ',');
        palettes0 = new String[palettes.length + 1];
        palettes0[0] = "";
        System.arraycopy(palettes, 0, palettes0, 1, palettes.length);
        patientData                = messages.getNotNothingString("patientData",                errorInMethod);
        patientYourGraph           = messages.getNotNothingString("patientYourGraph",           errorInMethod);
        pdfWidths                  = String2.toIntArray(String2.split(messages.getNotNothingString("pdfWidths",    errorInMethod), ','));
        pdfHeights                 = String2.toIntArray(String2.split(messages.getNotNothingString("pdfHeights",   errorInMethod), ','));
        protocolSearchHtml         = messages.getNotNothingString("protocolSearchHtml",         errorInMethod);
        protocolSearch2Html        = messages.getNotNothingString("protocolSearch2Html",        errorInMethod);
        protocolClick              = messages.getNotNothingString("protocolClick",              errorInMethod);
        requestFormatExamplesHtml  = messages.getNotNothingString("requestFormatExamplesHtml",  errorInMethod);
        resetTheForm               = messages.getNotNothingString("resetTheForm",               errorInMethod);
        resultsFormatExamplesHtml  = messages.getNotNothingString("resultsFormatExamplesHtml",  errorInMethod);
        resultsOfSearchFor         = messages.getNotNothingString("resultsOfSearchFor",         errorInMethod);
        searchFullTextHtml         = messages.getNotNothingString("searchFullTextHtml",         errorInMethod);
        searchButton               = messages.getNotNothingString("searchButton",               errorInMethod);
        searchClickTip             = messages.getNotNothingString("searchClickTip",             errorInMethod);
        searchHintsHtml            = messages.getNotNothingString("searchHintsHtml",            errorInMethod);
        searchTip                  = messages.getNotNothingString("searchTip",                  errorInMethod);
        searchRelevantAreFirst     = messages.getNotNothingString("searchRelevantAreFirst",     errorInMethod);
        searchSpelling             = messages.getNotNothingString("searchSpelling",             errorInMethod);
        searchFewerWords           = messages.getNotNothingString("searchFewerWords",           errorInMethod);
        ssBePatient                = messages.getNotNothingString("ssBePatient",                errorInMethod);
        ssInstructionsHtml         = messages.getNotNothingString("ssInstructionsHtml",         errorInMethod);
        standardShortDescriptionHtml=messages.getNotNothingString("standardShortDescriptionHtml",errorInMethod);
        standardLicense            = messages.getNotNothingString("standardLicense",            errorInMethod);
        standardContact            = messages.getNotNothingString("standardContact",            errorInMethod);
        standardDataLicenses       = messages.getNotNothingString("standardDataLicenses",       errorInMethod);
        standardDisclaimerOfExternalLinks=messages.getNotNothingString("standardDisclaimerOfExternalLinks", errorInMethod);
        standardDisclaimerOfEndorsement=messages.getNotNothingString("standardDisclaimerOfEndorsement",            errorInMethod);
        standardGeneralDisclaimer  = messages.getNotNothingString("standardGeneralDisclaimer",  errorInMethod);
        standardPrivacyPolicy      = messages.getNotNothingString("standardPrivacyPolicy",      errorInMethod);
        submit                     = messages.getNotNothingString("submit",                     errorInMethod);
        submitTooltip              = messages.getNotNothingString("submitTooltip",              errorInMethod);
        subscriptionHtml           = messages.getNotNothingString("subscriptionHtml",           errorInMethod);
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
        theLongDescriptionHtml     = messages.getNotNothingString("theLongDescriptionHtml",     errorInMethod);
        THERE_IS_NO_DATA           = messages.getNotNothingString("thereIsNoData",              errorInMethod);
        thereIsTooMuchData         = messages.getNotNothingString("thereIsTooMuchData",         errorInMethod);
        viewAllDatasetsHtml        = messages.getNotNothingString("viewAllDatasetsHtml",        errorInMethod);
        waitThenTryAgain           = messages.getNotNothingString("waitThenTryAgain",           errorInMethod);
        sosDescriptionHtml         = messages.getNotNothingString("sosDescriptionHtml",         errorInMethod);
        sosLongDescriptionHtml     = messages.getNotNothingString("sosLongDescriptionHtml",     errorInMethod); 
        wcsDescriptionHtml         = messages.getNotNothingString("wcsDescriptionHtml",         errorInMethod);
        wcsLongDescriptionHtml     = messages.getNotNothingString("wcsLongDescriptionHtml",     errorInMethod); 
        wmsDescriptionHtml         = messages.getNotNothingString("wmsDescriptionHtml",         errorInMethod);
        wmsInstructions            = messages.getNotNothingString("wmsInstructions",            errorInMethod); 
        wmsLongDescriptionHtml     = messages.getNotNothingString("wmsLongDescriptionHtml",     errorInMethod); 
        yRangeHtml                 = messages.getNotNothingString("yRangeHtml",                 errorInMethod); 
        yRangeMinHtml              = messages.getNotNothingString("yRangeMinHtml",              errorInMethod); 
        yRangeMaxHtml              = messages.getNotNothingString("yRangeMaxHtml",              errorInMethod); 

        Test.ensureEqual(imageWidths.length,  3, errorInMethod + "imageWidths.length must be 3.");
        Test.ensureEqual(imageHeights.length, 3, errorInMethod + "imageHeights.length must be 3.");
        Test.ensureEqual(pdfWidths.length,    3, errorInMethod + "pdfWidths.length must be 3.");
        Test.ensureEqual(pdfHeights.length,   3, errorInMethod + "pdfHeights.length must be 3.");

        for (int p = 0; p < palettes.length; p++) {
            String tName = fullPaletteDirectory + palettes[p] + ".cpt";
            Test.ensureTrue(File2.isFile(tName),
               "\"" + palettes[p] + 
                "\" is listed in <palettes> in messages.xml, but there is no file " + tName);
        }

        ampLoginInfoPo = startBodyHtml.indexOf(ampLoginInfo); 
        //String2.log("ampLoginInfoPo=" + ampLoginInfoPo);

        advancedSearchDirections = String2.replaceAll(advancedSearchDirections, "&searchButton;", searchButton);

        //always non-https: url
        convertFipsCountyService = String2.replaceAll(convertFipsCountyService, "&erddapUrl;",            erddapUrl) + "\n";
        convertTimeNotes         = String2.replaceAll(convertTimeNotes,         "&convertTimeUnitsHelp;", convertTimeUnitsHelp);
        convertTimeNotes         = String2.replaceAll(convertTimeNotes,         "&erddapUrl;",            erddapUrl) + "\n";
        convertTimeService       = String2.replaceAll(convertTimeService,       "&erddapUrl;",            erddapUrl) + "\n"; 
        convertUnitsFilter       = String2.replaceAll(convertUnitsFilter,       "&units_standard;",       units_standard);
        convertUnitsFilter       = String2.replaceAll(convertUnitsFilter,       "&erddapUrl;",            erddapUrl) + "\n";
        convertUnitsService      = String2.replaceAll(convertUnitsService,      "&erddapUrl;",            erddapUrl) + "\n"; 

        //standardContact is used by legal
        String tEmail = String2.replaceAll(adminEmail, "@", " at ");
        tEmail        = String2.replaceAll(tEmail,     ".", " dot ");
        standardContact = String2.replaceAll(standardContact, "&adminEmail;", tEmail);
        legal = String2.replaceAll(legal,"[standardContact]",                   standardContact                   + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDataLicenses]",              standardDataLicenses              + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDisclaimerOfExternalLinks]", standardDisclaimerOfExternalLinks + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardDisclaimerOfEndorsement]",   standardDisclaimerOfEndorsement   + "\n\n"); 
        legal = String2.replaceAll(legal,"[standardPrivacyPolicy]",             standardPrivacyPolicy             + "\n\n"); 

        PostIndex2Html = String2.replaceAll(PostIndex2Html, "&erddapUrl;",            erddapHttpsUrl);

        PostIndex3Html = String2.replaceAll(PostIndex3Html, "&PostSampleTag;",          PostSampleTag);
        PostIndex3Html = String2.replaceAll(PostIndex3Html, "&PostSurgeryDatasetID;",   PostSurgeryDatasetID);
        PostIndex3Html = String2.replaceAll(PostIndex3Html, "&PostDetectionDatasetID;", PostDetectionDatasetID);

        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&requestFormatExamplesHtml;", requestFormatExamplesHtml);
        theLongDescriptionHtml = String2.replaceAll(theLongDescriptionHtml, "&resultsFormatExamplesHtml;", resultsFormatExamplesHtml);

        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "[standardShortDescriptionHtml]", standardShortDescriptionHtml);
        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "&requestFormatExamplesHtml;",    requestFormatExamplesHtml);
        theShortDescriptionHtml = String2.replaceAll(theShortDescriptionHtml, "&resultsFormatExamplesHtml;",    resultsFormatExamplesHtml);
        postShortDescriptionActive = theShortDescriptionHtml.indexOf("[standardPostDescriptionHtml]") >= 0;

        //**************************************************************** 
        //other initialization

        String2.log("EDStatic initialization finished successfully.\n");
    }
  
    /** 
     * If loggedInAs is null, this returns erddapUrl; else erddapHttpsUrl;
     *
     * @param loggedInAs
     * @return If loggedInAs == null, this returns erddapUrl; else erddapHttpsUrl;
     */
    public static String erddapUrl(String loggedInAs) {
        return loggedInAs == null? erddapUrl : erddapHttpsUrl;
    }

    /** 
     * If publicAccess, this returns erddapUrl; else erddapHttpsUrl;
     *
     * @param publicAccess
     * @return If public access, this returns erddapUrl; else erddapHttpsUrl;
     */
    public static String publicErddapUrl(boolean publicAccess) {
        return publicAccess? erddapUrl : erddapHttpsUrl;
    }

    /** 
     * If loggedInAs is null, this returns imageDirUrl; else imageDirHttpsUrl;
     *
     * @param loggedInAs
     * @return If loggedInAs == null, this returns imageDirUrl; else imageDirHttpsUrl;
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
        String protocolUrl = tErddapUrl + "/" + protocol + "/index.html";
        return 
            "\n<h1>" + erddapHref(tErddapUrl) +
            "\n &gt; <a href=\"" + protocolUrl + "\">" + protocol + "</a>" +
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
        String protocolUrl = tErddapUrl + "/" + protocol + "/index.html";
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
        String protocolUrl = tErddapUrl + "/" + protocol + "/index.html";
        return 
            "\n<h1>" + erddapHref(tErddapUrl) + 
            "\n &gt; <a href=\"" + protocolUrl + "\">" + protocol + "</a>" +
            "\n &gt; " + datasetID + 
            "\n" + htmlTooltipImage(loggedInAs, htmlHelp) + 
            "\n</h1>\n";
    }

    /**
     * This is used by html web page generating methods to 
     * return the You Are Here html for ERDDAP/{protocol}/{attribute}/{category}.
     * 
     * @param loggedInAs
     * @param protocol e.g., categorize
     * @param attribute e.g., ioos_category
     * @param category e.g., Temperature
     * @return the You Are Here html for this EDD subclass.
     */
    public static String youAreHere(String loggedInAs, String protocol, 
        String attribute, String category) {

        String tErddapUrl = erddapUrl(loggedInAs);
        String protocolUrl  = tErddapUrl + "/" + protocol + "/index.html";
        String attributeUrl = tErddapUrl + "/" + protocol + "/" + attribute + "/index.html";
        return 
            "\n<h1>" + erddapHref(tErddapUrl) + 
            "\n &gt; <a href=\"" + protocolUrl + "\">" + protocol + "</a>" +
            "\n &gt; <a href=\"" + attributeUrl + "\">" + attribute + "</a>" +
            "\n &gt; " + category + 
            "\n</h1>\n";
    }

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
            imageDirUrl(loggedInAs) + questionMarkImageFile, html, ""); 
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
            "\n     To: " + String2.toCSVString(emailAddresses) + 
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
        
        //is the request too big under any circumstances?
        if (nBytes > Math2.maxSafeMemory) {
            tally.add("Request refused: not enough memory ever (since startup)", attributeTo);
            tally.add("Request refused: not enough memory ever (since last daily report)", attributeTo);
            throw new RuntimeException(
                "The request needs more memory (" + (nBytes / Math2.BytesPerMB) + 
                " MB) than is ever safely available in this Java setup (" + 
                (Math2.maxSafeMemory / Math2.BytesPerMB) + " MB) (" + attributeTo + ")."); 
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
            throw new RuntimeException(thereIsTooMuchData +
                " The request needs more memory (" + (nBytes / Math2.BytesPerMB) + 
                " MB) than is currently available (" + attributeTo + ")."); 
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
            throw new RuntimeException(thereIsTooMuchData +
                " The request needs an array size (" + tSize + 
                ") bigger than Java ever allows (" + Integer.MAX_VALUE + "). (" + attributeTo + ")"); 
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
            HashSet hs = new HashSet();
            for (int i = 0; i < rb.length; i++)
                hs.add(rb[i]);
            requestBlacklist = hs; //set in an instant
            String2.log("requestBlacklist is now " + String2.toCSVString(rb));
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
            EDStatic.ensureTaskThreadIsRunningIfNeeded();  //clients (like this class) are responsible for checking on it
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
        sb.append(String2.getDistributionStatistics(EDStatic.majorLoadDatasetsDistribution24)); sb.append('\n');
        sb.append("Major LoadDatasets Times Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.majorLoadDatasetsDistributionTotal)); sb.append('\n');
        sb.append('\n');

        sb.append("Minor LoadDatasets Times Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.minorLoadDatasetsDistribution24)); sb.append('\n');
        sb.append("Minor LoadDatasets Times Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.minorLoadDatasetsDistributionTotal)); sb.append('\n');
        sb.append('\n');

        if (failureTimesLoadDatasetsSB.length() > 0) {
            sb.append("Response Failed Summary (time series from between major LoadDatasets)\n");
            sb.append(failureTimesLoadDatasetsSB);
            sb.append('\n');
        }
        sb.append("Response Failed Time Distribution (since last major LoadDatasets):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.failureTimesDistributionLoadDatasets)); sb.append('\n');
        sb.append("Response Failed Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.failureTimesDistribution24)); sb.append('\n');
        sb.append("Response Failed Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.failureTimesDistributionTotal)); sb.append('\n');
        sb.append('\n');

        if (responseTimesLoadDatasetsSB.length() > 0) {
            sb.append("Response Succeeded Summary (time series from between major LoadDatasets)\n");
            sb.append(responseTimesLoadDatasetsSB);
            sb.append('\n');
        }
        sb.append("Response Succeeded Time Distribution (since last major LoadDatasets):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.responseTimesDistributionLoadDatasets)); sb.append('\n');
        sb.append("Response Succeeded Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.responseTimesDistribution24)); sb.append('\n');
        sb.append("Response Succeeded Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.responseTimesDistributionTotal)); sb.append('\n');
        sb.append('\n');

        sb.append("TaskThread Failed Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.taskThreadFailedDistribution24)); sb.append('\n');
        sb.append("TaskThread Failed Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.taskThreadFailedDistributionTotal)); sb.append('\n');
        sb.append("TaskThread Succeeded Time Distribution (since last Daily Report):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.taskThreadSucceededDistribution24)); sb.append('\n');
        sb.append("TaskThread Succeeded Time Distribution (since startup):\n");
        sb.append(String2.getDistributionStatistics(EDStatic.taskThreadSucceededDistributionTotal)); sb.append('\n');
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
                loggedInAs = (String)session.getAttribute("loggedInAs:" + EDStatic.warName);

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
        if (EDStatic.passwordEncoding.equals("MD5"))
            observed = String2.md5Hex(plaintextPassword); //it will be lowercase
        else if (EDStatic.passwordEncoding.equals("UEPMD5"))
            observed = String2.md5Hex(loggedInAs + ":ERDDAP:" + plaintextPassword); //it will be lowercase
        //only for debugging:
        //String2.log("loggedInAs=" + loggedInAs +
        //    "\nobsPassword=" + observed +
        //    "\nexpPassword=" + expected);

        boolean ok = observed.equals(expected);
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
     */
    public static void redirectToLogin(String loggedInAs, 
        HttpServletResponse response, String datasetID) {

        String message = null;
        try {
            EDStatic.tally.add("Log in Redirect (since startup)", datasetID); 
            EDStatic.tally.add("Log in Redirect (since last daily report)", datasetID);
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
                "<a href=\"" + EDStatic.erddapHttpsUrl + "/login.html\">log in</a>" :
                "<a href=\"" + EDStatic.erddapHttpsUrl + "/login.html\"><b>" + XML.encodeAsHTML(tLoggedInAs) + "</b></a> | \n" + 
                "<a href=\"" + EDStatic.erddapHttpsUrl + "/logout.html\">log out</a>";
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
    public static String searchHintsHtml(        String tErddapUrl) {return String2.replaceAll(searchHintsHtml,  "&erddapUrl;", tErddapUrl); }
    /** @param addToTitle has not yet been encodeAsHTML(addToTitle). */
    public static String startHeadHtml(          String tErddapUrl, String addToTitle) {
        String ts = startHeadHtml;
        if (addToTitle.length() > 0)
            ts = String2.replaceAll(ts, "</title>", " - " + XML.encodeAsHTML(addToTitle) + "</title>"); 
        return String2.replaceAll(ts,    "&erddapUrl;", tErddapUrl); 
        }
    public static String subscriptionHtml(         String tErddapUrl) {return String2.replaceAll(subscriptionHtml, "&erddapUrl;", tErddapUrl); }
    public static String subscription2Html(        String tErddapUrl) {return String2.replaceAll(subscription2Html, "&erddapUrl;", tErddapUrl); }
    public static String subscriptionAddHtml(      String tErddapUrl) {return String2.replaceAll(subscriptionAddHtml, "&erddapUrl;", tErddapUrl); }
    public static String subscriptionListHtml(     String tErddapUrl) {return String2.replaceAll(subscriptionListHtml, "&erddapUrl;", tErddapUrl); }
    public static String subscriptionRemoveHtml(   String tErddapUrl) {return String2.replaceAll(subscriptionRemoveHtml, "&erddapUrl;", tErddapUrl); }
    public static String subscriptionValidateHtml( String tErddapUrl) {return String2.replaceAll(subscriptionValidateHtml, "&erddapUrl;", tErddapUrl); }
    public static String subscriptionsNotAvailable(String tErddapUrl) {return String2.replaceAll(subscriptionsNotAvailable, "&erddapUrl;", tErddapUrl); }
    public static String theLongDescriptionHtml(   String tErddapUrl) {return String2.replaceAll(theLongDescriptionHtml,  "&erddapUrl;", tErddapUrl); }
    public static String theShortDescriptionHtml(  String tErddapUrl) {return String2.replaceAll(theShortDescriptionHtml, "&erddapUrl;", tErddapUrl); }
    public static String erddapHref(               String tErddapUrl) {
        return "<a title=\"The Environmental Research Division's Data Access Program\" \n" +
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
                String2.toCSVString(names));

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
                    return;
                }
                if (waitedSeconds > maxSeconds) {
                    String2.log("!!! EDStatic.destroy is done, but it had to stop() some threads.");
                    return;
                }
                Math2.sleep(2000);
                waitedSeconds += 2;
            }

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
                    email(EDStatic.emailEverythingTo, 
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
                int nPending = taskList.size() - EDStatic.nextTask;
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

    /** This adds a task to the taskList.
     * @return the task number that was assigned to the task.
     */
    public static int addTask(Object taskOA[]) {
        synchronized(taskList) { //all task-related things synch on taskList
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
     * This tests some of the methods in this class.
     * @throws an Exception if trouble.
     */
    public static void test() throws Throwable {
        String2.log("\n*** EDStatic.test");
    }

}
