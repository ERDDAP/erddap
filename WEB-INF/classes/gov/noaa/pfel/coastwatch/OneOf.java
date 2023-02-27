/* 
 * OneOf Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;


import com.cohort.array.IntArray;
import com.cohort.ema.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.hdf.SdsWriter;
import gov.noaa.pfel.coastwatch.pointdata.CacheOpendapStation;
import gov.noaa.pfel.coastwatch.pointdata.GroupVariable;
import gov.noaa.pfel.coastwatch.pointdata.PointDataSet;
import gov.noaa.pfel.coastwatch.pointdata.PointVectors;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.TableDataSet;
import gov.noaa.pfel.coastwatch.sgt.Boundaries;
import gov.noaa.pfel.coastwatch.sgt.GSHHS;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.IntObject;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.GregorianCalendar;
import java.util.Vector;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;

/**
 * This class holds things that there is just one of and which are shared 
 * throughout the CWBrowser program and by all users.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-10-31
 */
public class OneOf {

    public final static int N_DUMMY_GRID_DATASETS = 2; //because 0=(None) 1=Bathymetry
    public final static int N_DUMMY_OTHER_DATASETS = 1;  //becasue 0=(None)

    // *************** things set by oneOf constructor() 
    private boolean verbose;
    private boolean displayDiagnosticInfo;

    private String fullClassName; //fully-qualified name for .properties file
    private String shortClassName; //just the end of the fully qualified name
    private ResourceBundle2 emaRB2;
    private ResourceBundle2 classRB2;
    private ResourceBundle2 dataSetRB2;

    private int nPointScreens = 0;
    private int nTrajectoryScreens = 0;

    private String[] backgroundColors;
    private boolean lonPM180;
    private FileNameUtility fileNameUtility;
    private String url, helpLink;
    private String gridFileHelpUrl, gridFileHelpLink;
    private String pointFileHelpUrl, pointFileHelpLink;
    private String GETQueryHelpUrl, GETQueryHelpLink;
    public final static String DATA_SERVER_IS_DOWN = "The data server is not available.";
    //note that CCBrowser.properties dataSet.value mimics this string...
    public static String NO_DATA = "(None)";
    private String dataServerCatalogUrl, dataServerCatalogMustContain[];
    private String fgdcTemplate;
    private String baseUrl; 
    private String fullContextDirectory;  //a tomcat subdir
    private String bigParentDirectory;    //a directory on a drive with lots of space a many sub dir (see its use)
    private String requiredAuthorization, requiredRole;

    private String infoUrlBaseDir;
    private String infoUrlBaseUrl; 

    //a sub dir of fullContextDirectory, which is usually a symbolic link to a subdir of bigParentDirectory
    public static final String PUBLIC_DIRECTORY = "public/"; 
    private String fullPaletteDirectory;
    private String fullPublicDirectory;
    private String fullPrivateDirectory;
    private String fullResetFlagDirectory;
    private long   resetMaxMillis;
    private long   resetStalledMillis;
    private long   cacheMillis;

    private String localDataSetBaseDirectory;
    private String dataAccessNotAllowed;

    private String startHtmlHead;
    private String startHtmlBody;
    private String endHtmlBody;

    private String gridGetOptions[], gridVectorGetOptions[], gridGetTSOptions[], 
        bathymetryGetOptions[], 
        pointGetAvgOptions[], pointGetTSOptions[], 
        trajectoryGetSelectedOptions[], trajectoryGetAllOptions[];
    private String gridGetTitles[],  gridVectorGetTitles[],  gridGetTSTitles[],  
        bathymetryGetTitles[],
        pointGetAvgTitles[],  pointGetTSTitles[],
        trajectoryGetSelectedTitles[], trajectoryGetAllTitles[];

    /** DSFIXxx are the second index numbers for LXXxxxxFileInfo[]. */
    public final static int DSFIDirectory = 0, DSFIRegex = 1, DSFIAnomalyDataSet = 2;

    /** AI constants are the Anomaly field Index numbers. */
    public final static int AIGridDataSet6Name = 0, AIClimatologyDataSet6Name = 1,
        AIPaletteMax = 2, AIBoldTitle = 5; //3,4 are unused

    /** VIXxx are the second index numbers for vectorInfo[][]. */
    public final static int VIInternalName = 0, VIBoldTitle = 1, //2 is unused
        VIXDataSet = 3, VIYDataSet = 4, VISize = 5, VIUnits = 6;
    public final static int nVIIndexes = 7;

    /** PVIXxx are the second index numbers for pointVectorInfo[][]. */
    public final static int PVIInternalName = 0, PVIBoldTitle = 1, //2 is unused
        PVIXDataSet = 3, PVIYDataSet = 4, PVISize = 5, PVIUnits = 6;
    public final static int nPVIIndexes = 7;

    private String legendTitle1, legendTitle2;
    private String lowResLogoImageFile, highResLogoImageFile, googleEarthLogoFile;

    private SgtGraph sgtGraph;

    private String regionOptions[];  
    private String regionTitles[];
    private String regionInfo[][];

    private String regionsImageFileName;
    private String regionsImageFullFileName;
    private String regionsImageDefaultUrl;
    private String regionsImageTitle;
    private String regionsImageLabel;
    private String regionsImageAlt;
    private int    regionsImageWidth, regionsImageHeight,
           regionsImageMinXPixel,   regionsImageMaxXPixel,   
           regionsImageMinYPixel,   regionsImageMaxYPixel;
    private double regionsImageMinXDegrees, regionsImageMaxXDegrees,
           regionsImageMinYDegrees, regionsImageMaxYDegrees;
    private String regionCoordinatesMap;
    private String regionCoordinatesMapFormName = "";
    private String regionCoordinatesMapWithFormName;

    private double regionMinX, regionMaxX, regionMinY, regionMaxY;
    private int regionMinXPixel[], regionMaxXPixel[], 
                regionMinYPixel[], regionMaxYPixel[];
    private String regionFileInfo;

    private String fontFamily;
    private String warningTimePeriodsDisjoint;
    private String warningTimePeriodsDifferent;         

    private String imageSizeOptions[];
    private int    imageWidths[],  pdfPortraitWidths[],  pdfLandscapeWidths[];
    private int    imageHeights[], pdfPortraitHeights[], pdfLandscapeHeights[];

    private String hereIsAlt;
    private String clickOnMapToSeeTimeSeries;

    private String emailSmtpHost, emailSmtpProperties, emailSmtpPassword,
        emailUserName, emailFromAddress, emailEverythingTo, emailDailyReportTo; 
    private int emailSmtpPort;

    private String vectorInfo[][];    //[originalVectorIndex][VIXxx]
    private String vectorOptions[];   //[originalVectorIndex]

    private String pointVectorInfo[][];    //[originalPointVectorIndex][VIXxx]
    private String pointVectorOptions[];   //[originalPointVectorIndex]

    private String pointsDir;

    private long cachedDataSetsLastUpdated = 0;

    private String privateIP[];
    private String publicIP[];

    private Tally tally = new Tally();
    private int printTopNMostRequested; 

    private String errorMessage1, errorMessage2;

    //yes, static.   Shared by all apps using same public directory (e.g., cwexperimental)
    //key is gifPdfFileName  value is
    //when public dir cache is cleaned, it is cleaned, too (see cleanPublicDirectory)
    private static ConcurrentHashMap graphLocationHashMap = new ConcurrentHashMap(128, 0.75f, 4); //thread-safe

    public final static String imageExtension = ".png"; //was/could be .gif, but .gif is slower

    /** 
     * Constructor which reads properties from the resourceBundle. 
     *
     * @param tFullClassName is the full name of the class (e.g., "gov.noaa.pfel.coastwatch.CWBrowser")
     */
    public OneOf(String tFullClassName) throws Exception {

        try {
            long startTime = System.currentTimeMillis();
            fullClassName = tFullClassName;
            int scnPo = fullClassName.lastIndexOf(".");
            shortClassName = fullClassName.substring(scnPo + 1);

            String emaFullClassName = "com.cohort.ema.Ema";
            emaRB2 = new ResourceBundle2(emaFullClassName); 
            fileNameUtility = new FileNameUtility(fullClassName);
            classRB2 = fileNameUtility.classRB2(); 
            dataSetRB2 = fileNameUtility.dataSetRB2();

            String errorInMethod = String2.ERROR + " in OneOf constructor: ";

            verbose = classRB2.getBoolean("verbose", false);
            Boundaries.verbose = verbose;
            CacheOpendapStation.verbose = verbose;
            DataHelper.verbose = verbose;
            EmaAttribute.verbose = verbose;
            Grid.verbose = verbose;
            GridDataSet.verbose = verbose;
            GSHHS.verbose = verbose;
            GroupVariable.verbose = verbose;
            Opendap.verbose = verbose;
            PointDataSet.verbose = verbose;
            PointVectors.verbose = verbose;
            SdsWriter.verbose = verbose;
            SgtGraph.verbose = verbose;
            SgtMap.verbose = verbose;
            SSR.verbose = verbose;
            Table.verbose = verbose;
            TableDataSet.verbose = verbose;

            displayDiagnosticInfo = classRB2.getBoolean("displayDiagnosticInfo", false);
            if (displayDiagnosticInfo)
                Grid.doExtraErrorChecking = true;

            //email  (do early on so email can be sent if trouble later in this method)
            emailSmtpHost      = classRB2.getString("emailSmtpHost",  null);
            emailSmtpProperties= classRB2.getString("emailSmtpProperties",  null);
            emailSmtpPassword  = classRB2.getString("emailSmtpPassword",  null);
            emailSmtpPort      = classRB2.getInt(   "emailSmtpPort",  25);
            emailUserName      = classRB2.getString("emailUserName",  null);
            emailFromAddress   = classRB2.getString("emailFromAddress", null);
            emailEverythingTo  = classRB2.getString("emailEverythingTo", null); 
            emailDailyReportTo = classRB2.getString("emailDailyReportTo", null);

            //test of email
            //Test.error("This is a test of emailing an error in OneOf constructor.");

            //get absolute directories
            baseUrl = classRB2.getNotNothingString("baseUrl", errorInMethod);     
            fullContextDirectory = File2.webInfParentDirectory(); //with / separator and / at the end
            bigParentDirectory = classRB2.getNotNothingString("bigParentDirectory", errorInMethod);      
            fullContextDirectory = File2.addSlash(fullContextDirectory);
            bigParentDirectory =   File2.addSlash(bigParentDirectory);

            Test.ensureTrue(File2.isDirectory(fullContextDirectory),  
                errorInMethod + "fullContextDirectory (" + fullContextDirectory + ") doesn't exist.");
            Test.ensureTrue(File2.isDirectory(bigParentDirectory),  
                errorInMethod + "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");


            //set up log (after bigParentDirectory is known)
            String2.setupLog(false, false, 
                bigParentDirectory + "log.txt",
                true, String2.logFileDefaultMaxSize); //append?
            Math2.gcAndWait("OneOf constructor");  //before get memoryString() in OneOf constructor
            String2.log("*** Constructing OneOf for " + shortClassName + " at " + 
                Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
                "logFile=" + String2.logFileName() + "\n" +
                String2.standardHelpAboutMessage());

            //get the relative directories
            fullPaletteDirectory = classRB2.getNotNothingString("paletteDirectory", errorInMethod);
            fullPrivateDirectory = classRB2.getNotNothingString("privateDirectory", errorInMethod);      
            fullResetFlagDirectory = classRB2.getNotNothingString("resetFlagDirectory", errorInMethod);
            fullPaletteDirectory = File2.addSlash(fullPaletteDirectory);
            fullPrivateDirectory = File2.addSlash(fullPrivateDirectory);
            fullResetFlagDirectory = File2.addSlash(fullResetFlagDirectory);

            resetMaxMillis = classRB2.getInt("resetMaxMinutes", 60) * 60000; // millis/min
            resetStalledMillis = classRB2.getInt("resetStalledMinutes", 10) * 60000; // millis/min
            //how many minutes should files be left in the cache?
            cacheMillis = classRB2.getInt("cacheMinutes", 60) * 60000; // millis/min


            fullPublicDirectory  = fullContextDirectory + PUBLIC_DIRECTORY; 
            fullPaletteDirectory = fullContextDirectory + fullPaletteDirectory;
            fullPrivateDirectory = bigParentDirectory + fullPrivateDirectory;
            fullResetFlagDirectory = bigParentDirectory + fullResetFlagDirectory;

            //ensure the fullContextDirectories exist 
            Test.ensureTrue(File2.isDirectory(fullPaletteDirectory),  
                errorInMethod + "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
            Test.ensureTrue(File2.isDirectory(fullPublicDirectory),  
                errorInMethod + "fullPublicDirectory (" + fullPublicDirectory + ") doesn't exist.");

            //ensure the directories exist (or make them)  
            if (!File2.isDirectory(fullPrivateDirectory)) {
                File d = new File(fullPrivateDirectory);
                Test.ensureTrue(d.mkdirs(), errorInMethod + "unable to create " + fullPrivateDirectory);
            }    
            if (!File2.isDirectory(fullResetFlagDirectory)) {
                File d = new File(fullResetFlagDirectory);
                Test.ensureTrue(d.mkdirs(), errorInMethod + "unable to create " + fullResetFlagDirectory);
            }    
            
            String2.log(
                "fullContextDirectory=" + fullContextDirectory +
                "\nbigParentDirectory=" + bigParentDirectory +
                "\nfullPublicDirectory=" + fullPublicDirectory +
                "\nfullPaletteDirectory=" + fullPaletteDirectory +
                "\nfullPrivateDirectory=" + fullPrivateDirectory +
                "\nfullResetFlagDirectory=" + fullResetFlagDirectory);

            //load hdf native library     //currently inactive; does nothing
            //Grid.initialize(fullContextDirectory + "WEB-INF/lib/");
     
            //security stuff
            requiredAuthorization = classRB2.getString("requiredAuthorization", "");
            requiredRole = classRB2.getString("requiredRole", "");

            //on start up, always delete all files from publicDir and privateDir
            File2.deleteAllFiles(fullPublicDirectory); //no need for OneOf.publicDeleteIfOld, since nothing in hashtable
            File2.deleteAllFiles(fullPrivateDirectory);

            //set nPointScreens 
            //this looks for the last numbered "Station Data" screen, e.g., "Station Data 3"
            String tEditOptions = classRB2.getString("edit.options", null).trim();
            int tPo = tEditOptions.lastIndexOf("Station Data");
            if (tPo + 14 > tEditOptions.length())
                 nPointScreens = Integer.MAX_VALUE;
            else nPointScreens = String2.parseInt(tEditOptions.substring(tPo + 13, tPo + 14));
            if (nPointScreens == Integer.MAX_VALUE) {
                if (tPo >= 0)
                    nPointScreens = 1;
                else nPointScreens = 0;
            }
            String2.log("nPointScreens = " + nPointScreens);

            //set nTrajectoryScreens 
            //this looks for the last numbered "Trajectory Data" screen, e.g., "Trajectory Data 3"
            tPo = tEditOptions.lastIndexOf("Trajectory Data");
            if (tPo + 17 > tEditOptions.length())
                 nTrajectoryScreens = Integer.MAX_VALUE;
            else nTrajectoryScreens = String2.parseInt(tEditOptions.substring(tPo + 16, tPo + 17));
            if (nTrajectoryScreens == Integer.MAX_VALUE) {
                if (tPo >= 0)
                    nTrajectoryScreens = 1;
                else nTrajectoryScreens = 0;
            }
            String2.log("nTrajectoryScreens = " + nTrajectoryScreens);

            //url  and helpUrl
            url = classRB2.getString("url", null);
            Test.ensureNotNull(url, notInClassPropertiesFile("url"));

            //gridFileHelp   
            gridFileHelpUrl = classRB2.getString("gridFileHelpUrl", null);
            Test.ensureNotNull(gridFileHelpUrl, notInClassPropertiesFile("gridFileHelpUrl"));
            gridFileHelpLink = "        <a href=\"" + gridFileHelpUrl + 
                "\" title=\"General information about the grid data file types.\">File&nbsp;Type&nbsp;Info</a>\n";

            //pointFileHelp 
            pointFileHelpUrl = classRB2.getString("pointFileHelpUrl", null);
            Test.ensureNotNull(pointFileHelpUrl, notInClassPropertiesFile("pointFileHelpUrl"));
            pointFileHelpLink = "        <a href=\"" + pointFileHelpUrl + 
                "\" title=\"General information about the point data file types.\">File&nbsp;Type&nbsp;Info</a>\n";

            //GETQueryHelp   
            GETQueryHelpUrl = classRB2.getString("GETQueryHelpUrl", null);
            Test.ensureNotNull(GETQueryHelpUrl, notInClassPropertiesFile("GETQueryFileHelpUrl"));
            GETQueryHelpLink = "        <a href=\"" + GETQueryHelpUrl + 
                "\" title=\"General information about getting data files via HTTP GET queries.\">GET&nbsp;Queries</a>\n";

            //dataServer
            dataServerCatalogUrl = classRB2.getString("dataServerCatalogUrl", null);
            dataServerCatalogMustContain = String2.split(classRB2.getString("dataServerCatalogMustContain", null), '`');
            Test.ensureNotNull(dataServerCatalogUrl,         notInClassPropertiesFile("dataServerCatalogUrl"));
            Test.ensureNotNull(dataServerCatalogMustContain, notInClassPropertiesFile("dataServerCatalogMustContain"));

            //backgroundColors
            backgroundColors = String2.split(classRB2.getString("backgroundColors", null), '`');
            Test.ensureNotNull(backgroundColors, notInClassPropertiesFile("backgroundColors"));
            Test.ensureEqual(backgroundColors.length, 4, errorInMethod + "backgroundColors.length");

            //privateIP and publicIP
            privateIP = String2.split(classRB2.getString("privateIP", null), ',');
            publicIP = String2.split(classRB2.getString("publicIP", null), ',');

            //fgdcTemplate
            fgdcTemplate = dataSetRB2.getString("fgdcTemplate", null); //may be null
            if (fgdcTemplate != null) {
                StringBuilder sb = new StringBuilder(fgdcTemplate);
                //change &nbsp; at beginning of lines (to keep initial spaces) to " "
                String2.replaceAll(sb, "&nbsp;", " ");
                XML.removeComments(sb);
                fgdcTemplate = sb.toString();
            }
            //if (verbose)
            //    String2.log("fgdcTemplate=" + String2.annotatedString(XML.encodeAsTerminal(fgdcTemplate)));

            infoUrlBaseDir = dataSetRB2.getString("infoUrlBaseDir", null);
            infoUrlBaseUrl = dataSetRB2.getString("infoUrlBaseUrl", null); 
            Test.ensureNotNull(infoUrlBaseDir, notInDataSetPropertiesFile("infoUrlBaseDir"));
            Test.ensureNotNull(infoUrlBaseUrl, notInDataSetPropertiesFile("infoUrlBaseUrl"));

            //get 'Get' properties
            gridGetOptions         = String2.split(classRB2.getString("gridGet.options",       null), '\f');
            gridGetTSOptions       = String2.split(classRB2.getString("gridGetTS.options",     null), '\f');
            gridVectorGetOptions   = String2.split(classRB2.getString("gridVectorGet.options", null), '\f');
            bathymetryGetOptions   = String2.split(classRB2.getString("bathymetryGet.options", null), '\f');
            pointGetAvgOptions     = String2.split(classRB2.getString("pointGetAvg.options",   null), '\f');
            pointGetTSOptions      = String2.split(classRB2.getString("pointGetTS.options",    null), '\f');
            gridGetTitles          = String2.split(classRB2.getString("gridGet.title",         null), '\f');
            gridGetTSTitles        = String2.split(classRB2.getString("gridGetTS.title",       null), '\f');
            bathymetryGetTitles    = String2.split(classRB2.getString("bathymetryGet.title",   null), '\f');
            gridVectorGetTitles    = String2.split(classRB2.getString("gridVectorGet.title",   null), '\f');
            pointGetAvgTitles      = String2.split(classRB2.getString("pointGetAvg.title",     null), '\f');
            pointGetTSTitles       = String2.split(classRB2.getString("pointGetTS.title",      null), '\f');
            trajectoryGetSelectedOptions= String2.split(classRB2.getString("trajectoryGetSelected.options", null), '\f');
            trajectoryGetAllOptions     = String2.split(classRB2.getString("trajectoryGetAll.options", null), '\f');
            trajectoryGetSelectedTitles = String2.split(classRB2.getString("trajectoryGetSelected.title", null), '\f');
            trajectoryGetAllTitles      = String2.split(classRB2.getString("trajectoryGetAll.title", null), '\f');
            Test.ensureNotNull(gridGetOptions,       notInClassPropertiesFile("gridGetOptions"));
            Test.ensureNotNull(gridGetTSOptions,     notInClassPropertiesFile("gridGetTSOptions"));
            Test.ensureNotNull(gridVectorGetOptions, notInClassPropertiesFile("gridVectorGetOptions"));
            Test.ensureNotNull(bathymetryGetOptions, notInClassPropertiesFile("bathymetryGetOptions"));
            Test.ensureNotNull(pointGetAvgOptions,   notInClassPropertiesFile("pointGetAvgOptions"));
            Test.ensureNotNull(pointGetTSOptions,    notInClassPropertiesFile("pointGetTSOptions"));
            Test.ensureNotNull(gridGetTitles,        notInClassPropertiesFile("gridGetTitles"));
            Test.ensureNotNull(gridGetTSTitles,      notInClassPropertiesFile("gridGetTSTitles"));
            Test.ensureNotNull(gridVectorGetTitles,  notInClassPropertiesFile("gridVectorGetTitles"));
            Test.ensureNotNull(bathymetryGetTitles,        notInClassPropertiesFile("bathymetryGetTitles"));
            Test.ensureNotNull(pointGetAvgTitles,    notInClassPropertiesFile("pointGetAvgTitles"));
            Test.ensureNotNull(pointGetTSTitles,     notInClassPropertiesFile("pointGetTSTitles"));
            Test.ensureNotNull(trajectoryGetSelectedOptions, notInClassPropertiesFile("trajectoryGetSelectedOptions"));
            Test.ensureNotNull(trajectoryGetSelectedTitles,  notInClassPropertiesFile("trajectoryGetSelectedTitles"));
            Test.ensureNotNull(trajectoryGetAllOptions, notInClassPropertiesFile("trajectoryGetAllOptions"));
            Test.ensureNotNull(trajectoryGetAllTitles,  notInClassPropertiesFile("trajectoryGetAllTitles"));
            boolean trouble = gridGetOptions.length != gridGetTitles.length;
            if (verbose || trouble) {
                String2.log("gridGetOptions: "  + String2.toCSSVString(gridGetOptions));
                String2.log("gridGetTitles: "   + String2.toCSSVString(gridGetTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "nGridGetOptions "      + gridGetOptions.length + 
                        " != nGridGetTitles "   + gridGetTitles.length);
            }
            trouble = bathymetryGetOptions.length != bathymetryGetTitles.length;
            if (verbose || trouble) {
                String2.log("bathymetryGetOptions: "  + String2.toCSSVString(bathymetryGetOptions));
                String2.log("bathymetryGetTitles: "   + String2.toCSSVString(bathymetryGetTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "nBathymetryGetOptions "      + bathymetryGetOptions.length + 
                        " != nBathymetryGetTitles "   + bathymetryGetTitles.length);
            }
            trouble = gridGetTSOptions.length != gridGetTSTitles.length;
            if (verbose || trouble) {
                String2.log("gridGetTSOptions: "  + String2.toCSSVString(gridGetTSOptions));
                String2.log("gridGetTSTitles: "   + String2.toCSSVString(gridGetTSTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "nGridGetTSOptions "      + gridGetTSOptions.length + 
                        " != nGridGetTSTitles "   + gridGetTSTitles.length);
            }
            trouble = gridVectorGetOptions.length != gridVectorGetTitles.length;
            if (verbose || trouble) {
                String2.log("gridVectorGetOptions: "  + String2.toCSSVString(gridVectorGetOptions));
                String2.log("gridVectorGetTitles: "   + String2.toCSSVString(gridVectorGetTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "nGridVectorGetOptions "      + gridVectorGetOptions.length + 
                        " != nGridVectorGetTitles "   + gridVectorGetTitles.length);
            }
            trouble = pointGetAvgOptions.length != pointGetAvgTitles.length;
            if (verbose || trouble) {
                String2.log("pointGetAvgOptions: "  + String2.toCSSVString(pointGetAvgOptions));
                String2.log("pointGetAvgTitles: "   + String2.toCSSVString(pointGetAvgTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "n pointGetAvgOptions "      + pointGetAvgOptions.length + 
                        " != n pointGetAvgTitles "   + pointGetAvgTitles.length);
            }
            trouble = pointGetTSOptions.length != pointGetTSTitles.length;
            if (verbose || trouble) {
                String2.log("pointGetTSOptions: "  + String2.toCSSVString(pointGetTSOptions));
                String2.log("pointGetTSTitles: "   + String2.toCSSVString(pointGetTSTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "n pointGetTSOptions "      + pointGetTSOptions.length + 
                        " != n pointGetTSTitles "   + pointGetTSTitles.length);
            }
            trouble = trajectoryGetSelectedOptions.length != trajectoryGetSelectedTitles.length;
            if (verbose || trouble) {
                String2.log("trajectoryGetSelectedOptions: "  + String2.toCSSVString(trajectoryGetSelectedOptions));
                String2.log("trajectoryGetSelectedTitles: "   + String2.toCSSVString(trajectoryGetSelectedTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "n trajectoryGetSelectedOptions "      + trajectoryGetSelectedOptions.length + 
                        " != n trajectoryGetSelectedTitles "   + trajectoryGetSelectedTitles.length);
            }
            trouble = trajectoryGetAllOptions.length != trajectoryGetAllTitles.length;
            if (verbose || trouble) {
                String2.log("trajectoryGetAllOptions: "  + String2.toCSSVString(trajectoryGetAllOptions));
                String2.log("trajectoryGetAllTitles: "   + String2.toCSSVString(trajectoryGetAllTitles));
                if (trouble) 
                    Test.error(errorInMethod +
                        "n trajectoryGetAllOptions "      + trajectoryGetAllOptions.length + 
                        " != n trajectoryGetAllTitles "   + trajectoryGetAllTitles.length);
            }

            //legendTitle
            legendTitle1 = classRB2.getString("legendTitle1", ""); 
            legendTitle2 = classRB2.getString("legendTitle2", ""); 

            //logoImageFile
            highResLogoImageFile = classRB2.getString("highResLogoImageFile", null); 
            lowResLogoImageFile  = classRB2.getString("lowResLogoImageFile", null); 
            googleEarthLogoFile  = classRB2.getString("googleEarthLogoFile", null); 
            Test.ensureTrue(File2.isFile(fullContextDirectory + "images/" + highResLogoImageFile),  
                "highResLogoImageFile (" + fullContextDirectory + "images/" + highResLogoImageFile + ") not found.");
            Test.ensureTrue(File2.isFile(fullContextDirectory + "images/" + lowResLogoImageFile),  
                "lowResLogoImageFile (" + fullContextDirectory + "images/" + lowResLogoImageFile + ") not found.");
            Test.ensureTrue(File2.isFile(fullContextDirectory + "images/" + googleEarthLogoFile),  
                "googleEarthLogoFile (" + fullContextDirectory + "images/" + googleEarthLogoFile + ") not found.");

            //localDataSetBaseDirectory
            localDataSetBaseDirectory = classRB2.getString("localDataSetBaseDirectory",  null);
            Test.ensureNotNull(localDataSetBaseDirectory, 
                notInClassPropertiesFile("localDataSetBaseDirectory"));

            //dataAccessNotAllowed
            dataAccessNotAllowed = classRB2.getString("dataAccessNotAllowed",  null);
            Test.ensureNotNull(dataAccessNotAllowed, notInClassPropertiesFile("dataAccessNotAllowed"));

            //startHtmlHead
            startHtmlHead = classRB2.getString("startHtmlHead",  null);
            Test.ensureNotNull(startHtmlHead, notInClassPropertiesFile("startHtmlHead"));

            //startHtmlBody
            startHtmlBody = classRB2.getString("startHtmlBody",  null);
            Test.ensureNotNull(startHtmlBody, notInClassPropertiesFile("startHtmlBody"));

            //endHtmlBody
            endHtmlBody = classRB2.getString("endHtmlBody",  null);
            Test.ensureNotNull(endHtmlBody, notInClassPropertiesFile("endHtmlBody"));

            //get region properties
            regionOptions = String2.split(classRB2.getString("region.options",     null), '\f');
            regionTitles  = String2.split(classRB2.getString("region.title",       null), '\f');
            int nRegionOptions = regionOptions.length;
            trouble = nRegionOptions != (regionTitles.length-1); //1 extra title (main)
            if (verbose || trouble) {
                String2.log(
                    (trouble? String2.ERROR + " in OneOf.constructor:\n" : "") +
                    "regionOptions: "     + String2.toCSSVString(regionOptions) + "\n" +
                    "regionTitles: "      + String2.toNewlineString(regionTitles));
                Test.ensureEqual(nRegionOptions, regionOptions.length,    errorInMethod + "regionOptions.length"); 
                Test.ensureEqual(nRegionOptions, regionTitles.length - 1, errorInMethod + "regionTitles.length"); //0 is main title
            }

            //regionInfo: 0=rectColor,1=minX,2=maxX,3=minY,4=maxY,5=LLLabelX,6=LLLabelY,7=label
            String tRegionInfo[]  = String2.split(classRB2.getString("regionInfo",         null), '\f');
            regionInfo = new String[tRegionInfo.length][];
            for (int i = 0; i < tRegionInfo.length; i++) {  
                regionInfo[i] = String2.split(tRegionInfo[i], ',');
                trouble = regionInfo[i].length != 8;
                if (verbose || trouble) {
                    String2.log(
                        (trouble? String2.ERROR + " in OneOf.constructor:\n" : "") +
                        "tRegionInfo[" + i + "]=" + String2.toCSSVString(regionInfo[i])); 
                        Test.ensureEqual(regionInfo[i].length, 8, tRegionInfo[i]);
                }
            }
            regionsImageMinXDegrees = String2.parseDouble(regionInfo[0][1]);
            regionsImageMaxXDegrees = String2.parseDouble(regionInfo[0][2]);
            regionsImageMinYDegrees = String2.parseDouble(regionInfo[0][4]); //y's reversed: they correspond to min/maxY pixels
            regionsImageMaxYDegrees = String2.parseDouble(regionInfo[0][3]);
            if (verbose) String2.log(
                "regionsImageMinXDegrees:" + regionsImageMinXDegrees +
                " maxX:" + regionsImageMaxXDegrees +
                " minY:" + regionsImageMinYDegrees +
                " maxY:" + regionsImageMaxYDegrees);

            //this needs to be done before sgt.makeRegionsMap is called below
            fontFamily = classRB2.getString("fontFamily", SgtMap.fontFamily); 
            SgtMap.fontFamily = fontFamily;
            sgtGraph = new SgtGraph(fontFamily);

            //makePlainRegionsMap (after SGT initialized)
            regionsImageFileName = shortClassName + "Region" + imageExtension; 
            regionsImageFullFileName= fullContextDirectory + "images/" + regionsImageFileName;      
            regionsImageDefaultUrl  = "images/" + regionsImageFileName;
            regionsImageTitle       = classRB2.getString("regionsImage.title", "");
            regionsImageLabel       = classRB2.getString("regionsImage.label", "");
            regionsImageAlt         = classRB2.getString("regionsImage.alt", "");
            int regionsResult[] = SgtMap.makeRegionsMap(
                classRB2.getInt("regionMapMaxWidth",  228),
                classRB2.getInt("regionMapMaxHeight", 200),
                regionInfo, 
                fullContextDirectory + "images/", regionsImageFileName);
            //results: 0=imageWidth, 1=imageHeight, 2=graphMinX, 3=graphMaxX, 4=graphUpperY, 5=graphLowerY
            //get the name of the file with the small regions map
            regionsImageWidth       = regionsResult[0];
            regionsImageHeight      = regionsResult[1];
            regionsImageMinXPixel   = regionsResult[2];
            regionsImageMaxXPixel   = regionsResult[3];
            regionsImageMinYPixel   = regionsResult[4];
            regionsImageMaxYPixel   = regionsResult[5];
            trouble = 
                !Double.isFinite(regionsImageMinXDegrees) ||
                !Double.isFinite(regionsImageMaxXDegrees) ||
                !Double.isFinite(regionsImageMinYDegrees) ||
                !Double.isFinite(regionsImageMaxYDegrees) ||
                regionsImageMinXPixel == regionsImageMaxXPixel ||
                regionsImageMinYPixel == regionsImageMaxYPixel ||
                regionsImageMinXDegrees == regionsImageMaxXDegrees ||
                regionsImageMinYDegrees == regionsImageMaxYDegrees;
            if (verbose || trouble) {
                String2.log(
                    "regionsImageMinXPixel:" + regionsImageMinXPixel + " " + regionsImageMinXDegrees + "\n" +
                    "  maxX:" + regionsImageMaxXPixel + " " + regionsImageMaxXDegrees + "\n" +
                    "  minY:" + regionsImageMinYPixel + " " + regionsImageMinYDegrees + "\n" +
                    "  maxY:" + regionsImageMaxYPixel + " " + regionsImageMaxYDegrees);
                if (trouble)
                    Test.error(String2.ERROR + " in OneOf.constructor: regionsImageXY bad value");
            }

            //region
            regionMinX = Math.min(regionsImageMinXDegrees, regionsImageMaxXDegrees); 
            regionMaxX = Math.max(regionsImageMinXDegrees, regionsImageMaxXDegrees); 
            regionMinY = Math.min(regionsImageMinYDegrees, regionsImageMaxYDegrees);
            regionMaxY = Math.max(regionsImageMinYDegrees, regionsImageMaxYDegrees);
            regionFileInfo = FileNameUtility.makeWESNString(
                regionMinX, regionMaxX, regionMinY, regionMaxY); 
            lonPM180 = !DataHelper.lonNeedsToBe0360(regionMinX, regionMaxX); //if indeterminate, use pm180

            //create regionCoordinatesMap
            StringBuilder tsb = new StringBuilder();
            //[???define regionCoordinates for regionImage after activeRegionCoordinates known]
            //do in reverse order, so small regions detected before large regions
            //(first match found is used)
            tsb.append("    <map name=\"regionCoordinates\">\n");
            regionMinXPixel = new int[tRegionInfo.length];
            regionMaxXPixel = new int[tRegionInfo.length];
            regionMinYPixel = new int[tRegionInfo.length];
            regionMaxYPixel = new int[tRegionInfo.length];
            for (int i = tRegionInfo.length - 1; i >= 0; i--) {
                //# Pixels on the regionsImage corresponding to minX, minY, maxX, maxY  (0,0 at upper left)
                //# IE 5.2.3 for Mac OS X insists minX<maxX and minY<maxY
                regionMinXPixel[i] = Math2.roundToInt(regionsImageMinXPixel + (regionsImageMaxXPixel - regionsImageMinXPixel) * (String2.parseDouble(regionInfo[i][1]) - regionMinX)/(regionMaxX - regionMinX));         
                regionMinYPixel[i] = Math2.roundToInt(regionsImageMaxYPixel - (regionsImageMaxYPixel - regionsImageMinYPixel) * (String2.parseDouble(regionInfo[i][4]) - regionMinY)/(regionMaxY - regionMinY));           
                regionMaxXPixel[i] = Math2.roundToInt(regionsImageMinXPixel + (regionsImageMaxXPixel - regionsImageMinXPixel) * (String2.parseDouble(regionInfo[i][2]) - regionMinX)/(regionMaxX - regionMinX));         
                regionMaxYPixel[i] = Math2.roundToInt(regionsImageMaxYPixel - (regionsImageMaxYPixel - regionsImageMinYPixel) * (String2.parseDouble(regionInfo[i][3]) - regionMinY)/(regionMaxY - regionMinY));         
                tsb.append(
                    "      <area shape=\"rect\" coords=\"" + 
                        regionMinXPixel[i] + "," + regionMinYPixel[i] + "," +         
                        regionMaxXPixel[i] + "," + regionMaxYPixel[i] + "\"\n" +
                    "        title=\"" + regionTitles[i + 1] + "\"\n" + //+1 since 0 is main title
                    "        alt=\"" + regionTitles[i + 1] + "\"\n" + //+1 since 0 is main title
                    "        href=\"#\" " +   // was href=\"javascript:
                        "onClick=\"" + 
                        "pleaseWait(); document.form[0].region[" + i + 
                        "].checked=true; document.form[0].submit();\">\n");
            }
            tsb.append("    </map>\n");
            regionCoordinatesMap = tsb.toString();

            warningTimePeriodsDisjoint  = classRB2.getString("warningTimePeriodsDisjoint", null);
            warningTimePeriodsDifferent = classRB2.getString("warningTimePeriodsDifferent", null);

            //gif sizes
            imageSizeOptions = String2.split(classRB2.getString("imageSize.options",  null), '\f');
            imageWidths      = String2.toIntArray(String2.split(classRB2.getString("imageWidths",  null), '\f'));
            imageHeights     = String2.toIntArray(String2.split(classRB2.getString("imageHeights", null), '\f'));
            Test.ensureEqual(imageWidths.length,  imageSizeOptions.length, "imageSize.options length");
            Test.ensureEqual(imageWidths.length,  imageSizeOptions.length, "imageWidths.lengths");
            Test.ensureEqual(imageHeights.length, imageSizeOptions.length, "imageHeights.lengths");
            for (int i = 0; i < imageWidths.length; i++) {
                Test.ensureNotEqual(imageWidths[i],  Integer.MAX_VALUE, "imageWidths[" + i + "]");
                Test.ensureNotEqual(imageHeights[i], Integer.MAX_VALUE, "imageHeights[" + i + "]");
            }

            //pdf sizes
            pdfPortraitWidths   = String2.toIntArray(String2.split(classRB2.getString("pdfPortraitWidths",  null), '\f'));
            pdfLandscapeWidths  = String2.toIntArray(String2.split(classRB2.getString("pdfLandscapeWidths",  null), '\f'));
            pdfPortraitHeights  = String2.toIntArray(String2.split(classRB2.getString("pdfPortraitHeights", null), '\f'));
            pdfLandscapeHeights = String2.toIntArray(String2.split(classRB2.getString("pdfLandscapeHeights", null), '\f'));
            Test.ensureEqual(pdfPortraitWidths.length,  imageSizeOptions.length, "pdfPortraitWidths.lengths");
            Test.ensureEqual(pdfLandscapeWidths.length,  imageSizeOptions.length, "pdfLandscapeWidths.lengths");
            Test.ensureEqual(pdfPortraitHeights.length, imageSizeOptions.length, "pdfPortraitHeights.lengths");
            Test.ensureEqual(pdfLandscapeHeights.length, imageSizeOptions.length, "pdfLandscapeHeights.lengths");
            for (int i = 0; i < pdfPortraitWidths.length; i++) {
                Test.ensureNotEqual(pdfPortraitWidths[i],  Integer.MAX_VALUE, "pdfPortraitWidths[" + i + "]");
                Test.ensureNotEqual(pdfLandscapeWidths[i],  Integer.MAX_VALUE, "pdfLandscapeWidths[" + i + "]");
                Test.ensureNotEqual(pdfPortraitHeights[i], Integer.MAX_VALUE, "pdfPortraitHeights[" + i + "]");
                Test.ensureNotEqual(pdfLandscapeHeights[i], Integer.MAX_VALUE, "pdfLandscapeHeights[" + i + "]");
            }
 
            //hereIsAlt
            hereIsAlt = classRB2.getString("hereIsAlt",  null);
            Test.ensureNotNull(hereIsAlt, notInClassPropertiesFile("hereIsAlt"));

            //clickOnMapToSeeTimeSeries
            clickOnMapToSeeTimeSeries = classRB2.getString("clickOnMapToSeeTimeSeries", null);
            Test.ensureNotNull(clickOnMapToSeeTimeSeries, 
                notInClassPropertiesFile("clickOnMapToSeeTimeSeries"));

            //get Vector properties
            String tsar[] = String2.split(dataSetRB2.getString("vectorInfo",  null), '\f');
            Test.ensureNotNull(tsar, notInDataSetPropertiesFile("vectorInfo"));
            vectorInfo     = new String[tsar.length][]; //[][VIXxx]
            vectorOptions  = new String[tsar.length];
            for (int i = 0; i < tsar.length; i++) {
                //split the Info
                vectorInfo[i] = String2.split(tsar[i], '`');
                Test.ensureEqual(vectorInfo[i].length, nVIIndexes, errorInMethod + tsar[i]);
                String internalName = vectorInfo[i][VIInternalName];

                //get the option and title
                vectorOptions[i] = vectorInfo[i][VIBoldTitle];
                char ch = internalName.charAt(1);
                if (ch == 'O') vectorOptions[i] += "*";
                if (ch == 'T') vectorOptions[i] += "*";
            }

            //get PointVector properties
            tsar = String2.split(dataSetRB2.getString("pointVectorInfo",  null), '\f');
            Test.ensureNotNull(tsar, notInDataSetPropertiesFile("pointVectorInfo"));
            pointVectorInfo     = new String[tsar.length][]; //[][VIXxx]
            pointVectorOptions  = new String[tsar.length];
            for (int i = 0; i < tsar.length; i++) {
                //split the Info
                pointVectorInfo[i] = String2.split(tsar[i], '`');
                Test.ensureEqual(pointVectorInfo[i].length, nPVIIndexes, errorInMethod + tsar[i]);
                String internalName = pointVectorInfo[i][PVIInternalName];

                //get the option and title
                pointVectorOptions[i] = pointVectorInfo[i][PVIBoldTitle];
                char ch = internalName.charAt(1);
                if (ch == 'O') pointVectorOptions[i] += "*";
                if (ch == 'T') pointVectorOptions[i] += "*";
            }
            String2.log("pointVectorOptions=" + String2.toCSSVString(pointVectorOptions));

            pointsDir = classRB2.getString("pointsDir", null);

            errorMessage1 = classRB2.getString("errorMessage1", null);
            errorMessage2 = classRB2.getString("errorMessage2", null);
            Test.ensureNotNull(errorMessage1, notInClassPropertiesFile("errorMessage1"));
            Test.ensureNotNull(errorMessage1, notInClassPropertiesFile("errorMessage2"));
            errorMessage2 = String2.replaceAll(errorMessage2, "&emailFromAddress;", emailFromAddress);

            printTopNMostRequested = classRB2.getInt("printTopNMostRequested", 50); 

            //get the list of valid datasets from properties file and make sure related info is available
            String tDataSetList[] = String2.split(classRB2.getString("dataSetList", null), '`');
            int nDataSets = tDataSetList.length;
            boolean excessivelyStrict = false; //strict checking is done in TestAll and is only needed for GridSaveAs
            for (int i = N_DUMMY_GRID_DATASETS; i < nDataSets; i++) {  //"2" in order to skip 0=OneOf.NO_NONE and 1=BATHYMETRY
                fileNameUtility.ensureValidDataSetProperties(tDataSetList[i], excessivelyStrict);
            }            
            //checking for invalid info urls is done in TestAll

            //finished
            String2.log("OneOf constructor finished in " + 
                (System.currentTimeMillis() - startTime) + " ms.");

        } catch (Exception e) {
            String error = MustBe.throwableToString(e);
            email(emailEverythingTo, String2.ERROR + " in " + shortClassName + " oneOf constructor", error);
            Test.error(error);
        }
    }

    /** 
     * This generates a common error message used when an item is not found in 
     * the class' .properties file. 
     *
     * @param the name of the item not found
     */
    public String notInClassPropertiesFile(String name) {
        return String2.ERROR + " in OneOf constructor:\n" + name + " not found in " + 
            shortClassName + ".properties file.";
    }

    /** 
     * This generates a common error message used when an item is not found in 
     * the class' .properties file. 
     *
     * @param the name of the item not found
     */
    public String notInDataSetPropertiesFile(String name) {
        return String2.ERROR + " in OneOf constructor:\n" + name + " not found in DataSet.properties file.";
    }

    /** 
     * This converts a pixel position (0,0 at upper left) on the regionsImage
     * into a region name (or null if not in a region).
     *
     * @param x
     * @param y
     * @return the region number (or -1 if not in a region)
     */
    public String getRegionName(int x, int y) {
        //search backwards so small regions have precedence
        for (int i = regionMinXPixel.length - 1; i >= 0; i--) {
            //String2.log("x="+x+" y="+y+" i=" + i + " minx="+regionMinXPixel[i]+
            //    " maxx="+regionMaxXPixel[i]+" miny="+regionMinYPixel[i]+" maxy="+regionMaxYPixel[i]);
            if (x >= regionMinXPixel[i] && x <= regionMaxXPixel[i] &&
                y >= regionMinYPixel[i] && y <= regionMaxYPixel[i])
                return regionOptions[i];
        }
        return null;
    }

    /** The classRB2 with properties for this program. */
    public ResourceBundle2 classRB2() {return classRB2;}

    /** The dataSetRB2 with properties for all dataSets. */
    public ResourceBundle2 dataSetRB2() {return dataSetRB2;}

    /** The emaRB2 with properties for ema. */
    public ResourceBundle2 emaRB2() {return emaRB2;}

    /** The full class name. */
    public String fullClassName() {return fullClassName;}

    /** The short class name. */
    public String shortClassName() {return shortClassName;}

    /** The fileNameUtility based on the full class name. */
    public FileNameUtility fileNameUtility() {return fileNameUtility;}

    /** Indicates if the classes in this program should print extra
     diagnostic messages to String2.log. */
    public boolean verbose() {return verbose;}

    /** Indicates if diagnostic information should be included in the response. */
    public boolean displayDiagnosticInfo() {return displayDiagnosticInfo; }
   
    /** 
     * This indicates if the browser will work with longitudes +/-180 (lonPM180 = true) 
     * or 0...360 (lonPM180 = false).  
     *  Data used by the program can be +/-180 or 0...360.
     *  The data will be automatically converted to the lonPM180 specified here so
     *  that it can be plotted as desired.
     */
    public boolean lonPM180() {return lonPM180;}

    /** The number of Point Screens. */
    public int nPointScreens() {return nPointScreens;}
    
    /** The number of Trajectory Screens. */
    public int nTrajectoryScreens() {return nTrajectoryScreens;}
    
    /** Gets the specified background color (Edit, odd row, even row, submit's row). */
    public String backgroundColor(int which) {
        return backgroundColors[which];
    }

    /** Gets the HTML tag to begin a row. */
    public String getBeginRowTag(boolean odd) {
        return "<tr style=\"background-color:#" + backgroundColors[odd? 1 : 2] + "\">"; 
    }

    /** The relative url for the web page.*/
    public String url() {return url;}

    /** The url for the gridFile section of the help web page.*/
    public String gridFileHelpUrl() {return gridFileHelpUrl;}

    /** The HTML link tag for the gridFile section of the help web page.*/
    public String gridFileHelpLink() {return gridFileHelpLink;}

    /** The url for the pointFile section of the help web page.*/
    public String pointFileHelpUrl() {return pointFileHelpUrl;}

    /** The HTML link tag for the pointFile section of the help web page.*/
    public String pointFileHelpLink() {return pointFileHelpLink;}

    /** The url for the GETQuery section of the help web page.*/
    public String GETQueryHelpUrl() {return GETQueryHelpUrl;}

    /** The HTML link tag for the GETQuery section of the help web page.*/
    public String GETQueryHelpLink() {return GETQueryHelpLink;}

    /** The url for the dataServer catalog. */
    public String dataServerCatalogUrl() {return dataServerCatalogUrl;}

    /** The strings the dataServer catalog must contain. May be String[0]. */
    public String[] dataServerCatalogMustContain() {return dataServerCatalogMustContain;}

    /** 
     * This ensures that the Data Server is up.  
     *
     * @throws Exception if dataServerCatalogUrl doesn't return valid info or
     *   the info doesn't contain the dataServerCatalogMustContain strings.  
     */
    public void ensureDataServerIsUp() throws Exception {
        ensureDataServerIsUp(dataServerCatalogUrl, dataServerCatalogMustContain, verbose);
    }

    /** 
     * This ensures that the data server is up.
     *
     * @param dataServerCatalogUrl the url of the dataServerCatalog (already percentEncoded as needed) 
     *    (or "" if no test is to be performed)
     * @param dataServerCatalogMustContain the strings the response must contain. May be String[0]. 
     * @param verbose if you want extra of diagnostic messages printed to String2.log.
     * @throws Exception if dataServerCatalogUrl doesn't return valid info or
     *   the info doesn't contain the dataServerCatalogMustContain strings.  
     */
    public static void ensureDataServerIsUp(String dataServerCatalogUrl, 
            String dataServerCatalogMustContain[], boolean verbose) throws Exception {

        //do the dataServerCatalogUrl test
        if (dataServerCatalogUrl.length() > 0) {

            //this method goes to great lengths to throw very descriptive exceptions if trouble
            String error = String2.ERROR + " in OneOf.ensureDataServerUp\n  dataServerCatalogUrl=" + dataServerCatalogUrl + "\n";

            //get the urlResponse
            String catalog = null;
            try {
                catalog = SSR.getUrlResponseStringUnchanged(dataServerCatalogUrl);
            } catch (Exception e) {
                throw new Exception(error + e, e);
            }
            error += "catalog=" + catalog + "\n";
            
            //does it contain all the strings?
            for (int i = 0; i < dataServerCatalogMustContain.length; i++) {
                if (dataServerCatalogMustContain[i].length() > 0 &&
                    catalog.indexOf(dataServerCatalogMustContain[i]) < 0) {
                    Test.error(DATA_SERVER_IS_DOWN + "\n" +
                        error + "required string \"" + 
                        dataServerCatalogMustContain[i] + "\" not found in data server catalog\n" +
                        "url=" + dataServerCatalogUrl);
                }
            }
            if (verbose)
                String2.log("ensureDataServerIsUp says the data server is UP at " + 
                    Calendar2.getCurrentISODateTimeStringLocalTZ() +
                    "\n  catalogUrl=" + dataServerCatalogUrl);
            
        }

    }


    /**
     * This returns the HTML code for a link to the <internalName>InfoUrl
     * from the classRB2 file.
     * 
     * <p>This method is less strict than FileNameUtility.ensureInfoUrlExists.
     * <br>If XXXxxxxInfoUrl is "", there is no link (this returns "").
     * <br>If XXXxxxxInfoUrl starts with http:// , it must be a file on a remote server.
     * <br>Otherwise, XXXxxxxInfoUrl must be a file on this computer, in infoUrlBaseDir, 
     *   accessible to users as infoUrlBaseUrl + XXXxxxxInfoUrl
     *
     * @param internalName the internal dataset name, e.g., LATssta.
     * @return the HTML code for a link to the <internalName>InfoUrl
     * from the classRB2 file, or "" if there is no such information
     * in the classRB2 file.
     */
    public String getInternalNameInfoUrl(String internalName) {
        String infoUrl = dataSetRB2.getString(internalName + "InfoUrl", null);
        if (infoUrl == null) {
            String2.log("oneOf.getInternalNameInfoUrl: no info for " + internalName);
            return "";
        }
        if (infoUrl.equals(""))
            return "";
        if (!infoUrl.startsWith("http://"))
            infoUrl = infoUrlBaseUrl + infoUrl;
        return "        <a href=\"" + infoUrl + 
            "\" title=\"Detailed information about this data set.\">Data&nbsp;Set&nbsp;Info</a>\n";
    }

    /** The fgdcTemplate before substitutions are made. */
    public String fgdcTemplate() {return fgdcTemplate;}

    /** The base url, e.g., http://coastwatch.pfeg.noaa.gov/coastwatch/ .*/
    public String baseUrl() {return baseUrl;}

    /** The full context directory, e.g., /usr/local/jakarta-tomcat-5.5.4/webapps/coastwatch/ .*/
    public String fullContextDirectory() {return fullContextDirectory;}

    /** The bigParentDirectory, e.g., /u00/cwatch/coastwatch/ .*/
    public String bigParentDirectory() {return bigParentDirectory;}

    /** The full paletteDirectory, e.g., <fullContextDirectory>WEB-INF/cptfiles/ .*/
    public String fullPaletteDirectory() {return fullPaletteDirectory;}

    /** The full publicDirectory, e.g., <fullContextDirectory>CWBrowser/ .*/
    public String fullPublicDirectory() {return fullPublicDirectory;}

    /** The full privateDirectory, e.g., /u00/cwatch/coastwatch/private/ .*/
    public String fullPrivateDirectory() {return fullPrivateDirectory;}

    /** The full resetFlagDirectory, e.g., /u00/cwatch/coastwatch/flag/ .*/
    public String fullResetFlagDirectory() {return fullResetFlagDirectory;}

    /** The requiredAuthorization type e.g., BASIC, or "" if no https/authorization required.*/
    public String requiredAuthorization() {return requiredAuthorization;}

    /** The requiredRole type e.g., dbadmin, or "" if no role required.*/
    public String requiredRole() {return requiredRole;}

    /** The maximum millis between resets: is it time to generate new Shared? */
    public long resetMaxMillis() {return resetMaxMillis; }

    /** The maximum millis before a reset is declared stalled and the thread stopped. */
    public long resetStalledMillis() {return resetStalledMillis; }

    /** The maximum age of a file in fullPrivateDirectory or fullPublicDirectory (untouched) before it is deleted. */
    public long cacheMillis() {return cacheMillis; }

    /** The base directory for all 'relative directory' local datasets.*/
    public String localDataSetBaseDirectory() {return localDataSetBaseDirectory; }

    /** The message to be shown to users to indicate that they can't download the data.*/
    public String dataAccessNotAllowed() {return dataAccessNotAllowed; }

    /** The start of the html head code.*/
    public String startHtmlHead() {return startHtmlHead; }

    /** The start of the html body code.*/
    public String startHtmlBody() {return startHtmlBody; }

    /** The end of the html body code.*/
    public String endHtmlBody() {return endHtmlBody; }

    /** The options strings for downloading the grid data.*/
    public String[] gridGetOptions() {return gridGetOptions;}

    /** The options strings for downloading the gridTS data.*/
    public String[] gridGetTSOptions() {return gridGetTSOptions;}

    /** The options strings for downloading the grid vector data.*/
    public String[] gridVectorGetOptions() {return gridVectorGetOptions;}

    /** The options strings for downloading the bathymetry data.*/
    public String[] bathymetryGetOptions() {return bathymetryGetOptions;}

    /** The options strings for downloading the pointAvg data.*/
    public String[] pointGetAvgOptions() {return pointGetAvgOptions;}

    /** The options strings for downloading the pointTS data.*/
    public String[] pointGetTSOptions() {return pointGetTSOptions;}

    /** The options strings for downloading the trajectoryGetSelected data.*/
    public String[] trajectoryGetSelectedOptions() {return trajectoryGetSelectedOptions;}

    /** The options strings for downloading the trajectoryGetAll data.*/
    public String[] trajectoryGetAllOptions() {return trajectoryGetAllOptions;}

    /** The titles strings for downloading the grid data.*/
    public String[] gridGetTitles() {return gridGetTitles;}

    /** The titles strings for downloading the gridTS data.*/
    public String[] gridGetTSTitles() {return gridGetTSTitles;}

    /** The titles strings for downloading the grid vector data.*/
    public String[] gridVectorGetTitles() {return gridVectorGetTitles;}

    /** The titles strings for downloading the bathymetry data.*/
    public String[] bathymetryGetTitles() {return bathymetryGetTitles;}

    /** The titles strings for downloading the pointAvg data.*/
    public String[] pointGetAvgTitles() {return pointGetAvgTitles;}

    /** The titles strings for downloading the pointTS data.*/
    public String[] pointGetTSTitles() {return pointGetTSTitles;}

    /** The titles strings for downloading the trajectoryGetSelected data.*/
    public String[] trajectoryGetSelectedTitles() {return trajectoryGetSelectedTitles;}

    /** The titles strings for downloading the trajectoryGetAll data.*/
    public String[] trajectoryGetAllTitles() {return trajectoryGetAllTitles;}

    /** The text for legendTitle1. */
    public String legendTitle1() {return legendTitle1;}

    /** The text for legendTitle2. */
    public String legendTitle2() {return legendTitle2;}

    /** The name of the lowResLogoImageFile. */
    public String lowResLogoImageFile() {return lowResLogoImageFile;} 
        
    /** The name of the lowResLogoImageFile. */
    public String highResLogoImageFile() {return highResLogoImageFile;}

    /** The name of the googleEarthLogoFile. */
    public String googleEarthLogoFile() {return googleEarthLogoFile;}

    /** The sgtGraph object. */  
    public SgtGraph sgtGraph() {return sgtGraph;}

    /** The region options. */
    public String[] regionOptions() {return regionOptions;}

    /** The region titles (for the html tags). */
    public String[] regionTitles() {return regionTitles;}

    /** Info about the regions. */
    public String[][] regionInfo() {return regionInfo;}

    /** The name.extension of the regions image file. */ 
    public String regionsImageFileName() {return regionsImageFileName;}

    /** The full file name of the regions image. */
    public String regionsImageFullFileName() {return regionsImageFullFileName;}

    /** The Url for the regions image (to be put on web pages). */
    public String regionsImageDefaultUrl() {return regionsImageDefaultUrl;}

    /** The title for the regions image. */
    public String regionsImageTitle() {return regionsImageTitle;}

    /** The label for the regions image. */
    public String regionsImageLabel() {return regionsImageLabel;}

    /** The alt parameter for the regions image tag. */
    public String regionsImageAlt() {return regionsImageAlt;}

    /** The width of the regions image. */
    public int regionsImageWidth() {return regionsImageWidth;}
    
    /** The height of the regions image. */
    public int regionsImageHeight() {return regionsImageHeight;}

    /** The minX pixel of the map on the regions image. */
    public int regionsImageMinXPixel() {return regionsImageMinXPixel;}
               
    /** The maxX pixel of the map on the regions image. */
    public int regionsImageMaxXPixel() {return regionsImageMaxXPixel;}

    /** The minY pixel of the map on the regions image. */
    public int regionsImageMinYPixel() {return regionsImageMinYPixel;}
               
    /** The maxY pixel of the map on the regions image. */
    public int regionsImageMaxYPixel() {return regionsImageMaxYPixel;}

    /** The degrees which corresponds to the regionsImageMinXPixel. */
    public double regionsImageMinXDegrees() {return regionsImageMinXDegrees;}
        
    /** The degrees which corresponds to the regionsImageMaxXPixel. */
    public double regionsImageMaxXDegrees() {return regionsImageMaxXDegrees;}
           
    /** The degrees which corresponds to the regionsImageMinYPixel. */
    public double regionsImageMinYDegrees() {return regionsImageMinYDegrees;}
        
    /** The degrees which corresponds to the regionsImageMaxYPixel. */
    public double regionsImageMaxYDegrees() {return regionsImageMaxYDegrees;}
    
    /** The HTML tag for the regionsImage map tag (so a click gets converted...). 
     * Note that this refers to "form[0]". It is best if these references are
     * replaced with emaClass.getFormName().
     */
    public String regionCoordinatesMap() {return regionCoordinatesMap;}

    public synchronized String regionCoordinatesMap(String formName) {
        if (!regionCoordinatesMapFormName.equals(formName)) {
            regionCoordinatesMapWithFormName = String2.replaceAll(
                regionCoordinatesMap, "form[0]", formName);
            regionCoordinatesMapFormName = formName;
        }
        return regionCoordinatesMapWithFormName;
    }

    /** The minX for the entire max-sized region. */
    public double regionMinX() {return regionMinX;}
    
    /** The maxX for the entire max-sized region. */
    public double regionMaxX() {return regionMaxX;}
    
    /** The minY for the entire max-sized region. */
    public double regionMinY() {return regionMinY;}
    
    /** The minY for the entire max-sized region. */
    public double regionMaxY() {return regionMaxY;}
    
    /** The info about the max-sized region for file names, e.g., "_x-135_X-105_y22_Y50". */ 
    public String regionFileInfo() {return regionFileInfo;}

    /** The font family. */ 
    public String fontFamily() {return fontFamily;}

    /** The warning for disjoint time periods. May be null. */
    public String warningTimePeriodsDisjoint() {return warningTimePeriodsDisjoint;}

    /** The warning for overlapping but different time periods. May be null. */
    public String warningTimePeriodsDifferent() {return warningTimePeriodsDifferent;}
   
    /** The imageSizeOptions as strings. */
    public String[] imageSizeOptions() {return imageSizeOptions;}

    /** The gif numerical sizes. */
    public int[] imageWidths()  {return imageWidths;}
    public int[] imageHeights() {return imageHeights;}

    /** The pdf numerical sizes. */
    public int[] pdfPortraitWidths()   {return pdfPortraitWidths;}
    public int[] pdfLandscapeWidths()  {return pdfLandscapeWidths;}
    public int[] pdfPortraitHeights()  {return pdfPortraitHeights;}
    public int[] pdfLandscapeHeights() {return pdfLandscapeHeights;}

    /** The alt message for the map. */
    public String hereIsAlt() {return hereIsAlt;}

    /** The message for the map when clicking generates a time series. */
    public String clickOnMapToSeeTimeSeries() { return clickOnMapToSeeTimeSeries;}

    //None of the email strings need to be made publicly available.
    //See sendEmail() below.

    /** The info arrays [originalVectorIndex][VIXxx] for the vector data sets . */
    public String[][] vectorInfo() {return vectorInfo;}    

    /** The options strings [originalVectorIndex] for the vector data sets. */
    public String[] vectorOptions() {return vectorOptions;}   

    /** The info arrays [originalPointVectorIndex][PVIXxx] for the point vector data sets . */
    public String[][] pointVectorInfo() {return pointVectorInfo;}    

    /** The options strings [originalPointVectorIndex] for the pointVector data sets. */
    public String[] pointVectorOptions() {return pointVectorOptions;}   

    /** The directory with the ndbcMet, mbari, ... subdirectories and point data files (may be null). */
    public String pointsDir() {return pointsDir;}

    /** Return the message that there is no data in the selected region.*/
    public String noDataInRegion() {
        return "Sorry. There is no data in the currently selected region.";
    }

    /** Return the noDataAvailable message. */
    public String noDataAvailable() {return 
        "(No data is available for the current lat/lon range, Time Period and End Date.)";}

    /** Return the message that there is no fgdc info for this dataset.*/
    public String sorryNoFgdcInfo() {
        return "Sorry. There is no FGDC information available for that dataset.";
    }

    /** Return errorMessage1. */
    public String errorMessage1() {return errorMessage1;}

    /** Return errorMessage2. */
    public String errorMessage2() {return errorMessage2;}

    /** Return emailEverythingTo (may be null). */
    public String emailEverythingTo() {return emailEverythingTo;}

    /** Return emailDailyReportTo (may be null). */
    public String emailDailyReportTo() {return emailDailyReportTo;}

    /** Return the tally object. */
    public Tally tally() {return tally;}

    /** Return printTopNMostRequested. */
    public int printTopNMostRequested() {return printTopNMostRequested;}

    /** Return the System.currentTimeMillis associated with the last time the 
     * cached datasets were updated (initially 0). */
    public long getCachedDataSetsLastUpdated() {
        return cachedDataSetsLastUpdated;
    }

    /** Set the System.currentTimeMillis associated with the last time the 
     * cached datasets were updated. */
    public void setCachedDataSetsLastUpdated(long millis) {
        cachedDataSetsLastUpdated = millis;
    }


    /**
     * This determines if users are allowed to download the specified data.
     * See dataSet.daysTillDataReleased.
     *
     * @param daysTillDataAccessAllowed
     * @param endingDate  in iso format (e.g. "2001-02-05"). 
     * @return true if access is allowed
     * @throws Exception if trouble (e.g., invalid endingDate)
     */
    public static boolean dataAccessAllowed(int daysTillDataAccessAllowed, String endingDate) {
        GregorianCalendar cal = Calendar2.parseISODateTimeZulu(endingDate); //throws Exception if trouble
        cal.add(Calendar2.DATE, daysTillDataAccessAllowed);
        boolean allowed = cal.before(Calendar2.newGCalendarZulu());
        //if (verbose)   //this needs to be non-static to use verbose 
            String2.log("  dataAccessAllowed(" + daysTillDataAccessAllowed + 
                ", " +  endingDate + ") allowed=" + allowed);
        return allowed;
    }


    /**
     * This generates an fgdc file for a grid.
     * This always makes the file (even if it already exists).
     * See info about FGDC for fgdcTemplate in DataSet.properties
     *
     * @param gridDataSet the gridDataSet that created the tempGrid
     * @param tempGrid the grid with the data
     * @param timePeriodValue one of the TimePeriods.OPTIONS
     * @param timeValue an ISO formatted date/time, UTC.
     * @param altUnits 
     * @param dir the directory in which to store the fgdc file
     * @param customFileName  the name for the fgdc file (without .xml on end)
     * @throws Exception if trouble
     */
    public void makeFgdcFile(GridDataSet gridDataSet, Grid tempGrid, 
        String timePeriodValue, String timeValue, boolean altUnits, 
        String dir, String customFileName) throws Exception {

        if (fgdcTemplate == null || gridDataSet.fgdcSubstitutions == null) 
            Test.error(sorryNoFgdcInfo());

        String errorInMethod = String2.ERROR + " in OneOf.makeFgdcFile for " + 
            gridDataSet.internalName + ":\n";

        //calculate %cloud 
        int nX = tempGrid.lon.length;
        int nY = tempGrid.lat.length;
        int nXY = nX * nY;
        double minX = tempGrid.lon[0];
        double maxX = tempGrid.lon[nX - 1];
        double minY = tempGrid.lat[0];
        double maxY = tempGrid.lat[nY - 1];

        //make the bathymetry grid
        Grid bathGrid = SgtMap.createTopographyGrid(fullPrivateDirectory,
            minX, maxX, minY, maxY, nX, nY);

        //find closest bath lon and bath lat 
        int closestBathLon[] = new int[nX];
        int closestBathLat[] = new int[nY];
        for (int i = 0; i < nX; i++)
            closestBathLon[i] = Math2.binaryFindClosest(bathGrid.lon, tempGrid.lon[i]); 
        for (int i = 0; i < nY; i++)
            closestBathLat[i] = Math2.binaryFindClosest(bathGrid.lat, tempGrid.lat[i]); 

        //count mv's over water
        int nClouds = 0; 
        int nOverWater = 0;
        for (int y = 0; y < nY; y++) {
            int bathYIndex = closestBathLat[y];
            for (int x = 0; x < nX; x++) {
                int bathXIndex = closestBathLon[x];
                if (bathGrid.getData(bathXIndex, bathYIndex) < 0) {
                    nOverWater++;
                    if (Double.isNaN(tempGrid.getData(x, y)))
                        nClouds++;
                }
            }
        }
        int percentClouds = Math2.roundToInt(nClouds * 100.0 / Math.max(1, nOverWater));
        //String2.log("tempGrid.z.length=" + tempGrid.z.length + 
        //    " tempGrid.nValidPoints=" + tempGrid.nValidPoints +
        //    " percentClouds=" + percentClouds);

        //do the substitutions (comments were removed when fgdcTemplate read from .properties file)
        StringBuilder tTemplate = new StringBuilder(fgdcTemplate);
        XML.substitute(tTemplate, gridDataSet.fgdcSubstitutions); 

        //then do custom tag substitutions (after, because some subs above contain custom tags, e.g., &lonResolution;) 
        //!!!FGDC  <timeperd><timeinfo> seems to require specific year information
        //   and seems to have no provision for climatology information.
        //   So I am following the CF convention and using year 0001.

        GregorianCalendar gcZ = Calendar2.newGCalendarZulu();
        String currentZDateTime = Calendar2.formatAsCompactDateTime(gcZ);
        //String2.log("currentZDateTime=" + currentZDateTime);
        String2.replaceAll(tTemplate, "&currentZDate;", currentZDateTime.substring(0, 8)); // in the form YYYYMMDD  (in Zulu time)
        String2.replaceAll(tTemplate, "&currentZTime;", currentZDateTime.substring(8) + "Z"); // e.g., 8:15:32pm GMT would be written 201532Z 
        GregorianCalendar beginCalendar = gridDataSet.getStartCalendar(timePeriodValue, timeValue);
        GregorianCalendar endCalendar   = gridDataSet.getEndCalendar(timePeriodValue, timeValue);
        String beginCompact = Calendar2.formatAsCompactDateTime(beginCalendar);  //e.g., 20030102123700
        String endCompact   = Calendar2.formatAsCompactDateTime(endCalendar);
        if (beginCalendar.equals(endCalendar)) {
            //one datetime    
            String2.replaceAll(tTemplate, "&datasetTimeInfo;",
                "\n" +
                "           <sngdate>\n" +
                "             <caldate>" + endCompact.substring(0, 8) + "</caldate>\n" + 
                "             <time>" + endCompact.substring(8, 14) + "Z</time>\n" + 
                "           </sngdate>\n" +
                "         ");
        } else {
            //range of dates   
            String2.replaceAll(tTemplate, "&datasetTimeInfo;",
                "\n" +
                "           <rngdates>\n" + 
                "             <begdate>" + beginCompact.substring(0, 8) + "</begdate>\n" + 
                "             <begtime>" + beginCompact.substring(8, 14) + "Z</begtime>\n" + 
                "             <enddate>" + endCompact.substring(0, 8) + "</enddate>\n" + 
                "             <endtime>" + endCompact.substring(8, 14) + "Z</endtime>\n" + 
                "           </rngdates>\n" +
                "         ");
        }  

        String2.replaceAll(tTemplate, "&westBound;",  "" + minX); // western boundary;  Western lons are negative numbers
        String2.replaceAll(tTemplate, "&eastBound;",  "" + maxX); // eastern boundary;  Western lons are negative numbers
        String2.replaceAll(tTemplate, "&southBound;", "" + minY); // southern boundary; Southern lats are negative numbers
        String2.replaceAll(tTemplate, "&northBound;", "" + maxY); // northern boundary; Southern lats are negative numbers
        String2.replaceAll(tTemplate, "&timePeriod;", timePeriodValue); // e.g., 'day', '3 day', 'monthly'
        String2.replaceAll(tTemplate, "&processingZDate;", endCompact.substring(0, 8)); // date of processing in the form YYYYMMDD, in UTC time
        String2.replaceAll(tTemplate, "&percentClouds;", "" + percentClouds); // area of a data set obstructed by clouds, ... percentage of spatial extent for which data is not available
        String2.replaceAll(tTemplate, "&nRowsData;", "" + nY); // number of rows of data
        String2.replaceAll(tTemplate, "&nColsData;", "" + nX); // number of columns of data
        String2.replaceAll(tTemplate, "&latResolution;", "" + tempGrid.latSpacing); 
        String2.replaceAll(tTemplate, "&lonResolution;", "" + tempGrid.lonSpacing); 

        Test.ensureEqual("", 
            File2.writeToFileUtf8(dir + customFileName + ".xml",
                tTemplate.toString()), 
            String2.ERROR + " while writing FGDC .xml file.");
    }

    
    /**
     * This sends the specified email to the emailAddress.
     * This won't throw an exception if trouble.
     *
     * @param emailToAddresses a comma-separated list of To addresses.
     *    If all or one is null or "" or "null", it's a silent error.
     * @param subject If error, recommended: String2.ERROR + " in " + fullClassName
     * @param content If error, recommended: MustBe.throwableToString(t);
     */
    public void email(String emailToAddresses, String subject, String content) {
        try {
            SSR.sendEmail(emailSmtpHost, emailSmtpPort, emailUserName, 
                emailSmtpPassword, 
                emailSmtpProperties,
                emailFromAddress, emailToAddresses, subject, content);
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + ": Sending email failed.", e));
        }
    }

    /** 
     * Add info to the graphLocationHashMap.
     *
     * @param imageFileName the key for the hashtable.
     * @param graphLocation an IntArray returned by SgtMap.makeMap: 
     *    originX,endX,originY,endY.  The value for the hashtable.
     */
    public static void putGraphLocation(String imageFileName,
            IntArray graphLocation) {
        graphLocationHashMap.put(imageFileName, graphLocation);
    }
        
    /** 
     * Get a value from graphLocationHashMap.
     *
     * @param imageFileName  the key for the hashtable.
     * @return graphLocation: an IntArray returned by SgtMap.makeMap: 
     *    originX,endX,originY,endY, or null if imageFileName not recognized.
     *    (The value from the hashtable.)
     */
    public static IntArray getGraphLocation(String imageFileName) {
        return (IntArray)graphLocationHashMap.get(imageFileName);
    }
        
    /**
     * Remove old files from public directory, and remove related
     * entry in graphLocationHashMap (if file was a imageFileName).
     *
     *
     * @param publicDirectory
     * @param time files with a timestamp older than this will be removed
     * @return the number of files that remain
     */
    public static int publicDeleteIfOld(String publicDirectory, long time) {
        //this is based on code from File2.removeIfOld
        try {
            File file = new File(publicDirectory);

            //make sure it is an existing directory
            if (!file.isDirectory())
                return -1;

            //go through the files and delete old ones
            File files[] = file.listFiles();
            int nDeleted = 0;
            int nGraphLocationsDeleted = 0;
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && 
                    files[i].lastModified() < time) {
                    files[i].delete();
                    nDeleted++;
                    if (graphLocationHashMap.remove(files[i].getName()) != null)
                        nGraphLocationsDeleted++;
                }
            }
            String2.log("OneOf.publicDeleteIfOld nDeleted=" + nDeleted + 
                    " nRemain=" + (files.length - nDeleted) +
                "\n  nGraphLocationsDeleted=" + nGraphLocationsDeleted + 
                    " graphLocationHashMap.size=" + graphLocationHashMap.size());
            return files.length - nDeleted;
        } catch (Exception e) {
            String2.log(MustBe.throwable("OneOf.publicDeleteIfOld(" + publicDirectory + 
                ", " + time + ")", e));
            return -1;
        }


    }

    /**
     * This converts numeric ip's within urls to public-friendly urls,
     * based on privateIP and publicIP info in the BrowserDefault.properties file.
     * 
     */
    public String makePublicUrl(String url) {
        if (url == null || publicIP == null || privateIP == null) 
            return url;
        for (int i = 0; i < publicIP.length; i++) 
            url = String2.replaceAll(url, privateIP[i], publicIP[i]);
        return url;
    }



}
