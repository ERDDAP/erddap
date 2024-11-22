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
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.sun.management.UnixOperatingSystemMXBean;
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
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.LoadDatasets;
import gov.noaa.pfel.erddap.RunLoadDatasets;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableFromCassandra;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamFromHttpResponse;
import gov.noaa.pfel.erddap.dataset.TableWriterHtmlTable;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.Cleaner;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.apache.lucene.store.NIOFSDirectory;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * This class holds a lot of static information set from the setup.xml and messages.xml files and
 * used by all the other ERDDAP classes.
 */
public class EDStatic {

  public static final Cleaner cleaner = Cleaner.create();

  /**
   * These are options used to control behavior for testing. They should be their default values
   * during normal operation. Better encapsulation of EDStatic initilization would mean we can get
   * rid of these.
   */
  public static boolean skipEmailThread = false;

  public static boolean forceSynchronousLoading = false;

  public static boolean usePrometheusMetrics = true;

  /** The all lowercase name for the program that appears in urls. */
  public static final String programname = "erddap";

  /** The uppercase name for the program that appears on web pages. */
  public static final String ProgramName = "ERDDAP";

  public static final String REQUESTED_RANGE_NOT_SATISFIABLE = "REQUESTED_RANGE_NOT_SATISFIABLE: ";

  /**
   * This changes with each release. <br>
   * See Changes information in /downloads/setup.html . <br>
   * 0.1 started on 2007-09-17 <br>
   * 0.11 released on 2007-11-09 <br>
   * 0.12 released on 2007-12-05 <br>
   * 0.2 released on 2008-01-10 <br>
   * From here on, odd .01 are used during development <br>
   * 0.22 released on 2008-02-21 <br>
   * 0.24 released on 2008-03-03 <br>
   * 0.26 released on 2008-03-11 <br>
   * 0.28 released on 2008-04-14 <br>
   * 1.00 released on 2008-05-06 <br>
   * 1.02 released on 2008-05-26 <br>
   * 1.04 released on 2008-06-10 <br>
   * 1.06 released on 2008-06-20 <br>
   * 1.08 released on 2008-07-13 <br>
   * 1.10 released on 2008-10-14 <br>
   * 1.12 released on 2008-11-02 <br>
   * 1.14 released on 2009-03-17 <br>
   * 1.16 released on 2009-03-26 <br>
   * 1.18 released on 2009-04-08 <br>
   * 1.20 released on 2009-07-02 <br>
   * 1.22 released on 2009-07-05 <br>
   * 1.24 released on 2010-08-06 <br>
   * 1.26 released on 2010-08-25 <br>
   * 1.28 released on 2010-08-27 <br>
   * 1.30 released on 2011-04-29 <br>
   * 1.32 released on 2011-05-20 <br>
   * 1.34 released on 2011-06-15 <br>
   * 1.36 released on 2011-08-01 <br>
   * 1.38 released on 2012-04-21 <br>
   * 1.40 released on 2012-10-25 <br>
   * 1.42 released on 2012-11-26 <br>
   * 1.44 released on 2013-05-30 <br>
   * 1.46 released on 2013-07-09 <br>
   * It's okay if .001 used for minor releases. Some code deals with it as a double, but never d.dd.
   * <br>
   * 1.48 released on 2014-09-04 <br>
   * 1.50 released on 2014-09-06 <br>
   * 1.52 released on 2014-10-03 <br>
   * 1.54 released on 2014-10-24 <br>
   * 1.56 released on 2014-12-16 <br>
   * 1.58 released on 2015-02-25 <br>
   * 1.60 released on 2015-03-12 <br>
   * 1.62 released on 2015-06-08 <br>
   * 1.64 released on 2015-08-19 <br>
   * 1.66 released on 2016-01-19 <br>
   * 1.68 released on 2016-02-08 <br>
   * 1.70 released on 2016-04-15 <br>
   * 1.72 released on 2016-05-12 <br>
   * 1.74 released on 2016-10-07 <br>
   * 1.76 released on 2017-05-12 <br>
   * 1.78 released on 2017-05-27 <br>
   * 1.80 released on 2017-08-04 <br>
   * 1.82 released on 2018-01-26 <br>
   * 2.00 released on 2019-06-26 <br>
   * 2.01 released on 2019-07-02 <br>
   * 2.02 released on 2019-08-21 <br>
   * 2.10 released on 2020-11-05 (version jump because of new PATypes) <br>
   * 2.11 released on 2020-12-04 <br>
   * 2.12 released on 2021-05-14 <br>
   * 2.13 none <br>
   * 2.14 released on 2021-07-02 <br>
   * 2.15 released on 2021-11-19 Just to coastwatch, to test translations. <br>
   * 2.16 released on 2021-12-17 <br>
   * 2.17 released on 2022-02-16 <br>
   * 2.18 released on 2022-02-23 <br>
   * 2.19 released on 2022-09-10 <br>
   * 2.20 released on 2022-09-30 <br>
   * 2.21 released on 2022-10-09 <br>
   * 2.22 released on 2022-12-08 <br>
   * 2.23 released on 2023-02-27 <br>
   * 2.24 released on 2024-06-07 <br>
   * 2.25 first RC on 2024-10-16 released on 2024-10-31 <br>
   * 2.25_1 RC 2024-11-07
   *
   * <p>For main branch releases, this will be a floating point number with 2 decimal digits, with
   * no additional text. !!! In general, people other than the main ERDDAP developer (Bob) should
   * not change the *number* below. If you need to identify a fork of ERDDAP, please append "_" +
   * other ASCII text (no spaces or control characters) to the number below, e.g., "1.82_MyFork". In
   * a few places in ERDDAP, this string is parsed as a number. The parser now disregards "_" and
   * anything following it. A request to http.../erddap/version will return just the number (as
   * text). A request to http.../erddap/version_string will return the full string.
   */
  public static final String erddapVersion = "2.25_1"; // see comment above

  /**
   * This is almost always false. During development, Bob sets this to true. No one else needs to.
   * If true, ERDDAP uses setup2.xml and datasets2.xml (and messages2.xml if it exists).
   */
  public static boolean developmentMode = false;

  /** This identifies the dods server/version that this mimics. */
  public static final String dapVersion = "DAP/2.0";

  public static final String serverVersion = "dods/3.7"; // this is what thredds replies (in 2008!)

  // drds at https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.ver replies "DODS/3.2"
  // both reply with server version, neither replies with coreVersion
  // spec says #.#.#, but Gallagher says #.# is fine.

  /**
   * contentDirectory is the local directory on this computer, e.g., [tomcat]/content/erddap/ It
   * will have a slash at the end.
   */
  public static String contentDirectory;

  public static final String INSTITUTION = "institution";
  public static final int TITLE_DOT_LENGTH = 95; // max nChar before inserting newlines

  // fgdc and iso19115XmlDirectory are used for virtual URLs.
  public static final String fgdcXmlDirectory = "metadata/fgdc/xml/"; // virtual
  public static final String iso19115XmlDirectory = "metadata/iso19115/xml/"; // virtual
  public static final String DOWNLOAD_DIR = "download/";
  public static final String IMAGES_DIR = "images/";
  public static final String PUBLIC_DIR = "public/";
  public static final String fullPaletteDirectory;
  public static final String fullPublicDirectory;
  public static final String downloadDir; // local directory on this computer
  public static final String imageDir; // local directory on this computer
  public static final Tally tally = new Tally();
  public static int emailThreadFailedDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] emailThreadFailedDistributionTotal = new int[String2.TimeDistributionSize];
  public static int emailThreadSucceededDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] emailThreadSucceededDistributionTotal = new int[String2.TimeDistributionSize];
  public static int emailThreadNEmailsDistribution24[] =
      new int[String2.CountDistributionSize]; // count, not time
  public static int[] emailThreadNEmailsDistributionTotal =
      new int[String2.CountDistributionSize]; // count, not time
  public static int failureTimesDistributionLoadDatasets[] = new int[String2.TimeDistributionSize];
  public static int failureTimesDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] failureTimesDistributionTotal = new int[String2.TimeDistributionSize];
  public static int majorLoadDatasetsDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] majorLoadDatasetsDistributionTotal = new int[String2.TimeDistributionSize];
  public static int minorLoadDatasetsDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] minorLoadDatasetsDistributionTotal = new int[String2.TimeDistributionSize];
  public static int responseTimesDistributionLoadDatasets[] = new int[String2.TimeDistributionSize];
  public static int responseTimesDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] responseTimesDistributionTotal = new int[String2.TimeDistributionSize];
  public static int taskThreadFailedDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] taskThreadFailedDistributionTotal = new int[String2.TimeDistributionSize];
  public static int taskThreadSucceededDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] taskThreadSucceededDistributionTotal = new int[String2.TimeDistributionSize];
  public static int touchThreadFailedDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] touchThreadFailedDistributionTotal = new int[String2.TimeDistributionSize];
  public static int touchThreadSucceededDistribution24[] = new int[String2.TimeDistributionSize];
  public static int[] touchThreadSucceededDistributionTotal = new int[String2.TimeDistributionSize];
  public static final AtomicInteger requestsShed =
      new AtomicInteger(0); // since last Major LoadDatasets
  public static final AtomicInteger dangerousMemoryEmails =
      new AtomicInteger(0); // since last Major LoadDatasets
  public static final AtomicInteger dangerousMemoryFailures =
      new AtomicInteger(0); // since last Major LoadDatasets
  public static final StringBuilder suggestAddFillValueCSV =
      new StringBuilder(); // EDV constructors append message here   //thread-safe but probably
  // doesn't need to be

  public static String datasetsThatFailedToLoad = "";
  public static String failedDatasetsWithErrors = "";
  public static String errorsDuringMajorReload = "";
  public static final StringBuilder majorLoadDatasetsTimeSeriesSB =
      new StringBuilder(); // thread-safe (1 thread writes but others may read)
  public static HashSet<String> requestBlacklist =
      null; // is read-only. Replacement is swapped into place.
  public static final long startupMillis = System.currentTimeMillis();
  public static final String startupLocalDateTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
  public static int nGridDatasets = 0; // as of end of last major loadDatasets
  public static int nTableDatasets = 0; // as of end of last major loadDatasets
  public static long lastMajorLoadDatasetsStartTimeMillis = System.currentTimeMillis();
  public static long lastMajorLoadDatasetsStopTimeMillis = System.currentTimeMillis() - 1;
  // Currently Loading Dataset
  public static volatile boolean cldMajor = false;
  public static volatile int cldNTry = 0; //   0=none actively loading
  public static volatile String cldDatasetID = null; // null=none actively loading
  public static volatile long cldStartMillis = 0; //   0=none actively loading

  public static final ConcurrentHashMap<String, String> activeRequests =
      new ConcurrentHashMap<>(); // request# -> 1 line info about request
  public static volatile long lastActiveRequestReportTime =
      0; // 0 means not currently in dangerousMemory inUse event

  public static final String ipAddressNotSetYet = "NotSetYet";
  public static final String ipAddressUnknown = "(unknownIPAddress)";
  public static final ConcurrentHashMap<String, IntArray> ipAddressQueue =
      new ConcurrentHashMap<>(); // ipAddress -> list of request#
  public static final int DEFAULT_ipAddressMaxRequestsActive = 2; // in datasets.xml
  public static final int DEFAULT_ipAddressMaxRequests =
      15; // in datasets.xml //more requests will see Too Many Requests error. This must be at least
  // 6 because browsers make up to 6 simultaneous requests. This can't be >1000.
  public static final String DEFAULT_ipAddressUnlimited = ", " + ipAddressUnknown;
  public static int ipAddressMaxRequestsActive =
      DEFAULT_ipAddressMaxRequestsActive; // in datasets.xml
  public static int ipAddressMaxRequests =
      DEFAULT_ipAddressMaxRequests; // in datasets.xml //more requests will see Too Many Requests
  // error. This must be at least 6 because browsers make up to 6
  // simultaneous requests.
  public static Set<String>
      ipAddressUnlimited = // in datasets.xml  //read only. New one is swapped into place. You can
          // add and remove addresses as needed.
          new HashSet<>(
              String2.toArrayList(
                  StringArray.fromCSVNoBlanks(DEFAULT_ipAddressUnlimited).toArray()));
  public static int tooManyRequests =
      0; // nRequests exceeding ipAddressMaxRequests, since last major datasets reload
  public static final String translationDisclaimer =
      // from https://cloud.google.com/translate/attribution
      "TRANSLATION DISCLAIMER"
          + "<br>&nbsp;"
          + "<br>THIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE. GOOGLE"
          + "<br>DISCLAIMS ALL WARRANTIES RELATED TO THE TRANSLATIONS, EXPRESS"
          + "<br>OR IMPLIED, INCLUDING ANY WARRANTIES OF ACCURACY, RELIABILITY,"
          + "<br>AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A"
          + "<br>PARTICULAR PURPOSE AND NONINFRINGEMENT."
          + "<br>&nbsp;"
          + "<br>The ERDDAP website has been translated for your convenience using translation"
          + "<br>software powered by Google Translate. Reasonable efforts have been made to"
          + "<br>provide an accurate translation, however, no automated translation is perfect"
          + "<br>nor is it intended to replace human translators. Translations are provided"
          + "<br>as a service to users of the ERDDAP website, and are provided \"as is.\""
          + "<br>No warranty of any kind, either expressed or implied, is made as to the"
          + "<br>accuracy, reliability, or correctness of any translations made from English"
          + "<br>into any other language. Some content (such as images, videos, etc.) may not"
          + "<br>be accurately translated due to the limitations of the translation software."
          + "<br>&nbsp;"
          + "<br>The official text is the English version of the website. Any discrepancies or"
          + "<br>differences created in the translation are not binding and have no legal"
          + "<br>effect for compliance or enforcement purposes. If any questions arise related"
          + "<br>to the accuracy of the information contained in the translated website, refer"
          + "<br>to the English version of the website which is the official version.";

  // things that can be specified in datasets.xml (often added in ERDDAP v2.00)
  public static final String DEFAULT_ANGULAR_DEGREE_UNITS =
      "angular_degree,angular_degrees,arcdeg,arcdegs,degree,"
          + "degreeE,degree_E,degree_east,degreeN,degree_N,degree_north,degrees,"
          + "degreesE,degrees_E,degrees_east,degreesN,degrees_N,degrees_north,"
          + "degreesW,degrees_W,degrees_west,degreeW,degree_W,degree_west";
  public static final String DEFAULT_ANGULAR_DEGREE_TRUE_UNITS =
      "degreesT,degrees_T,degrees_Tangular_degree,degrees_true," + "degreeT,degree_T,degree_true";
  public static Set<String> angularDegreeUnitsSet =
      new HashSet<>(
          String2.toArrayList(
              StringArray.fromCSV(DEFAULT_ANGULAR_DEGREE_UNITS).toArray())); // so canonical
  public static Set<String> angularDegreeTrueUnitsSet =
      new HashSet<>(
          String2.toArrayList(
              StringArray.fromCSV(DEFAULT_ANGULAR_DEGREE_TRUE_UNITS).toArray())); // so canonical

  public static final int DEFAULT_decompressedCacheMaxGB =
      10; // for now, 1 value applies to each dataset's decompressed dir
  public static final int DEFAULT_decompressedCacheMaxMinutesOld = 15;
  public static final int DEFAULT_nGridThreads = 1;
  public static final int DEFAULT_nTableThreads = 1;
  public static String DEFAULT_palettes[] = null; // set when messages.xml is read
  public static Set<String> DEFAULT_palettes_set = null; // set when messages.xml is read
  public static int decompressedCacheMaxGB = DEFAULT_decompressedCacheMaxGB;
  public static int decompressedCacheMaxMinutesOld = DEFAULT_decompressedCacheMaxMinutesOld;
  public static int nGridThreads = DEFAULT_nGridThreads; // will be a valid number 1+
  public static int nTableThreads = DEFAULT_nTableThreads; // will be a valid number 1+
  public static String convertInterpolateRequestCSVExample = null; // may be null or ""
  public static String convertInterpolateDatasetIDVariableList[] = new String[0]; // may be [0]

  // things that were in setup.xml (discouraged) and are now in datasets.xml (v2.00+)
  public static final int DEFAULT_cacheMinutes = 60;
  public static final String DEFAULT_drawLandMask = "under";
  public static final int DEFAULT_graphBackgroundColorInt = 0xffccccff;
  public static final int DEFAULT_loadDatasetsMinMinutes = 15;
  public static final int DEFAULT_loadDatasetsMaxMinutes = 60;
  public static final String DEFAULT_logLevel = "info"; // warning|info|all
  public static final int DEFAULT_partialRequestMaxBytes =
      490000000; // this is just below tds default <opendap><binLimit> of 500MB
  public static final int DEFAULT_partialRequestMaxCells = 10000000;
  public static final int DEFAULT_slowDownTroubleMillis = 1000;
  public static final int DEFAULT_unusualActivity = 10000;
  public static final int DEFAULT_updateMaxEvents = 10;
  public static final int DEFAULT_unusualActivityFailPercent = 25;
  public static final boolean DEFAULT_showLoadErrorsOnStatusPage = true;
  public static long cacheMillis = DEFAULT_cacheMinutes * Calendar2.MILLIS_PER_MINUTE;
  public static String drawLandMask = DEFAULT_drawLandMask;
  public static boolean emailDiagnosticsToErdData = true;
  public static Color graphBackgroundColor =
      new Color(DEFAULT_graphBackgroundColorInt, true); // hasAlpha
  public static long loadDatasetsMinMillis =
      DEFAULT_loadDatasetsMinMinutes * Calendar2.MILLIS_PER_MINUTE;
  public static long loadDatasetsMaxMillis =
      DEFAULT_loadDatasetsMaxMinutes * Calendar2.MILLIS_PER_MINUTE;
  // logLevel handled specially by setLogLevel
  public static int partialRequestMaxBytes = DEFAULT_partialRequestMaxBytes;
  public static int partialRequestMaxCells = DEFAULT_partialRequestMaxCells;
  public static int slowDownTroubleMillis = DEFAULT_slowDownTroubleMillis;
  public static int unusualActivity = DEFAULT_unusualActivity;
  public static int updateMaxEvents = DEFAULT_updateMaxEvents;
  public static int unusualActivityFailPercent = DEFAULT_unusualActivityFailPercent;
  public static boolean showLoadErrorsOnStatusPage = DEFAULT_showLoadErrorsOnStatusPage;

  // not translated
  public static final
  String // these are set by setup.xml (deprecated) and/or messages.xml and/or datasets.xml (v2.00+)
      DEFAULT_standardLicense;
  public static String standardLicense;
  public static final String DEFAULT_startHeadHtml; // see xxx() methods
  public static String startHeadHtml; // see xxx() methods

  // translated
  public static final String
          [] // these are set by setup.xml (deprecated) and/or messages.xml and/or datasets.xml
      // (v2.00+)
      DEFAULT_standardContactAr;
  public static final String[] DEFAULT_standardDataLicensesAr;
  public static final String[] DEFAULT_standardDisclaimerOfEndorsementAr;
  public static final String[] DEFAULT_standardDisclaimerOfExternalLinksAr;
  public static final String[] DEFAULT_standardGeneralDisclaimerAr;
  public static final String[] DEFAULT_standardPrivacyPolicyAr;
  public static final String[] DEFAULT_startBodyHtmlAr;
  public static final String[] DEFAULT_theShortDescriptionHtmlAr;
  public static final String[] DEFAULT_endBodyHtmlAr;
  public static final String[] standardContactAr;
  public static final String[] standardDataLicensesAr;
  public static final String[] standardDisclaimerOfEndorsementAr;
  public static final String[] standardDisclaimerOfExternalLinksAr;
  public static final String[] standardGeneralDisclaimerAr;
  public static final String[] standardPrivacyPolicyAr;
  public static final String[] startBodyHtmlAr;
  public static final String[] theShortDescriptionHtmlAr;
  public static final String[] endBodyHtmlAr;
  public static String // in messages.xml and perhaps in datasets.xml (v2.00+)
      commonStandardNames[];
  public static final String[] DEFAULT_commonStandardNames;

  // Default max of 25 copy tasks at a time, so different datasets have a chance.
  // Otherwise, some datasets could take months to do all the tasks.
  // And some file downloads are very slow (10 minutes).
  // Remember: last task is to reload the dataset, so that will get the next 25 tasks.
  public static final int DefaultMaxMakeCopyFileTasks = 25;

  /**
   * userHashMap. key=username (if email address, they are lowercased) value=[encoded password,
   * sorted roles String[]] It is empty until the first LoadDatasets is finished and puts a new
   * HashMap in place. It is private so no other code can access the information except via
   * doesPasswordMatch() and getRoles(). MD5'd and SHA'd passwords should all already be lowercase.
   * No need to be thread-safe: one thread writes it, then put here where read only.
   */
  private static Map<String, Object[]> userHashMap = new HashMap<>();

  /**
   * This is a HashMap of key=id value=thread that need to be interrupted/killed when erddap is
   * stopped in Tomcat. For example, key="taskThread", value=taskThread. The key make it easy to get
   * a specific thread (e.g., to remove it).
   */
  public static final ConcurrentHashMap<String, Thread> runningThreads =
      new ConcurrentHashMap<>(16, 0.75f, 4);

  // emailThread variables
  // Funnelling all emailThread emails through one emailThread ensures that
  //  emails that timeout don't slow down other processes
  //  and allows me to email in batches so fewer email sessions (so I won't
  //  get Too Many Login Attempts and lost emails).
  public static final ArrayList<String[]> emailList =
      new ArrayList<>(); // keep here in case EmailThread needs to be restarted
  private static EmailThread emailThread;

  // no lastAssignedEmail since not needed
  /**
   * This returns the index number of the email in emailList (-1,0..) of the last completed email
   * (successful or not). nFinishedEmails = lastFinishedEmail + 1;
   */
  public static final AtomicInteger lastFinishedEmail = new AtomicInteger(-1);

  /**
   * This returns the index number of the email in emailList (0..) that will be started when the
   * current email is finished.
   */
  public static final AtomicInteger nextEmail = new AtomicInteger(0);

  // taskThread variables
  // Funnelling all taskThread tasks through one taskThread ensures
  //  that the memory requirements, bandwidth usage, cpu usage,
  //  and stress on remote servers will be minimal
  //  (although at the cost of not doing the tasks faster / in parallel).
  // In a grid of erddaps, each will have its own taskThread, which is appropriate.
  public static final ArrayList<Object[]> taskList =
      new ArrayList<>(); // keep here in case TaskThread needs to be restarted
  private static TaskThread taskThread;

  /**
   * lastAssignedTask is used by EDDxxxCopy instances to keep track of the number of the last task
   * assigned to taskThread for a given datasetID. key=datasetID value=Integer(task#)
   */
  public static final ConcurrentHashMap<String, Integer> lastAssignedTask =
      new ConcurrentHashMap<>(16, 0.75f, 4);

  /**
   * This returns the index number of the task in taskList (-1,0..) of the last completed task
   * (successful or not). nFinishedTasks = lastFinishedTask + 1;
   */
  public static final AtomicInteger lastFinishedTask = new AtomicInteger(-1);

  /**
   * This returns the index number of the task in taskList (0..) that will be started when the
   * current task is finished.
   */
  public static final AtomicInteger nextTask = new AtomicInteger(0);

  // touchThread variables
  // Funnelling all touchThread tasks through one touchThread ensures that
  //  touches that timeout don't slow down other processes.
  public static final ArrayList<String> touchList =
      new ArrayList<>(); // keep here in case TouchThread needs to be restarted
  private static TouchThread touchThread;

  // no lastAssignedTouch since not needed
  /**
   * This returns the index number of the touch in touchList (-1,0..) of the last completed touch
   * (successful or not). nFinishedTouches = lastFinishedTouch + 1;
   */
  public static final AtomicInteger lastFinishedTouch = new AtomicInteger(-1);

  /**
   * This returns the index number of the touch in touchList (0..) that will be started when the
   * current touch is finished.
   */
  public static final AtomicInteger nextTouch = new AtomicInteger(0);

  /**
   * This recieves key=startOfLocalSourceUrl value=startOfPublicSourceUrl from LoadDatasets and is
   * used by EDD.convertToPublicSourceUrl.
   */
  public static final ConcurrentHashMap<String, String> convertToPublicSourceUrl =
      new ConcurrentHashMap<>(16, 0.75f, 4);

  /**
   * This returns the position of the "/" in if tFrom has "[something]//[something]/...", and is
   * thus a valid tFrom for convertToPublicSourceUrl.
   *
   * @return the po of the end "/" (or -1 if invalid).
   */
  public static int convertToPublicSourceUrlFromSlashPo(String tFrom) {
    if (tFrom == null) return -1;
    int spo = tFrom.indexOf("//");
    if (spo > 0) spo = tFrom.indexOf("/", spo + 2);
    return spo;
  }

  /** For Lucene. */
  // Since I recreate the index when erddap restarted, I can change anything
  //  (e.g., Directory type, Version) any time
  //  (no worries about compatibility with existing index).
  // useful documentatino
  //  https://wiki.apache.org/lucene-java/LuceneFAQ
  //  https://wiki.apache.org/lucene-java/BasicsOfPerformance
  //  http://affy.blogspot.com/2003/04/codebit-examples-for-all-of-lucenes.html
  public static final String luceneDefaultField = "text";

  // special characters to be escaped
  // see bottom of https://lucene.apache.org/java/3_5_0/queryparsersyntax.html
  public static final String luceneSpecialCharacters = "+-&|!(){}[]^\"~*?:\\";

  // made if useLuceneSearchEngine
  // there are many analyzers; this is a good starting point
  public static Analyzer luceneAnalyzer;
  private static QueryParser luceneQueryParser; // not thread-safe

  // made once by RunLoadDatasets
  public static Directory luceneDirectory;
  public static IndexWriter luceneIndexWriter; // is thread-safe

  // made/returned by luceneIndexSearcher
  private static IndexReader luceneIndexReader; // is thread-safe, but only need/want one
  private static final Object luceneIndexReaderLock = Calendar2.newGCalendarLocal();
  public static boolean needNewLuceneIndexReader = true;
  private static IndexSearcher luceneIndexSearcher; // is thread-safe, so can reuse
  public static ConcurrentHashMap<Integer, String> luceneDocNToDatasetID;

  // also see updateLucene in LoadDatasets

  public static final int defaultItemsPerPage = 1000; // 1000, for /info/index.xxx and search
  public static final String defaultPIppQuery = "page=1&itemsPerPage=" + defaultItemsPerPage;
  public static final String allPIppQuery = "page=1&itemsPerPage=1000000000";

  /** The HTML/XML encoded form */
  public static final String encodedDefaultPIppQuery =
      "page=1&#x26;itemsPerPage=" + defaultItemsPerPage;

  public static final String encodedAllPIppQuery = "page=1&#x26;itemsPerPage=1000000000";
  public static final String DONT_LOG_THIS_EMAIL = "!!! DON'T LOG THIS EMAIL: ";

  /**
   * These values are loaded from the [contentDirectory]setup.xml file. See comments in the
   * [contentDirectory]setup.xml file.
   */
  public static final String baseUrl;

  public static final String baseHttpsUrl; // won't be null, may be "(not specified)"
  public static String bigParentDirectory;
  public static final String adminInstitution;
  public static final String adminInstitutionUrl;
  public static final String adminIndividualName;
  public static final String adminPosition;
  public static final String adminPhone;
  public static final String adminAddress;
  public static final String adminCity;
  public static final String adminStateOrProvince;
  public static final String adminPostalCode;
  public static final String adminCountry;
  public static final String adminEmail;
  public static final String accessConstraints;
  public static final String accessRequiresAuthorization;
  public static final String fees;
  public static final String keywords;
  public static final String units_standard;

  public static String // the unencoded EDDGrid...Example attributes
      EDDGridErddapUrlExample;
  public static String EDDGridIdExample;
  public static String EDDGridDimensionExample;
  public static String EDDGridNoHyperExample;
  public static String EDDGridDimNamesExample;
  public static String EDDGridDataTimeExample;
  public static String EDDGridDataValueExample;
  public static String EDDGridDataIndexExample;
  public static String EDDGridGraphExample;
  public static String EDDGridMapExample;
  public static String EDDGridMatlabPlotExample;

  public static final String // variants encoded to be Html Examples
      EDDGridDimensionExampleHE;
  public static final String EDDGridDataIndexExampleHE;
  public static final String EDDGridDataValueExampleHE;
  public static final String EDDGridDataTimeExampleHE;
  public static final String EDDGridGraphExampleHE;
  public static final String EDDGridMapExampleHE;

  public static final String // variants encoded to be Html Attributes
      EDDGridDimensionExampleHA;
  public static final String EDDGridDataIndexExampleHA;
  public static final String EDDGridDataValueExampleHA;
  public static final String EDDGridDataTimeExampleHA;
  public static final String EDDGridGraphExampleHA;
  public static final String EDDGridMapExampleHA;
  public static final String EDDTableFromHttpGetDatasetDescription;
  public static final String EDDTableFromHttpGetAuthorDescription;
  public static final String EDDTableFromHttpGetTimestampDescription;

  public static String // the unencoded EDDTable...Example attributes
      EDDTableErddapUrlExample;
  public static String EDDTableIdExample;
  public static String EDDTableVariablesExample;
  public static String EDDTableConstraintsExample;
  public static String EDDTableDataTimeExample;
  public static String EDDTableDataValueExample;
  public static String EDDTableGraphExample;
  public static String EDDTableMapExample;
  public static String EDDTableMatlabPlotExample;

  public static final String // variants encoded to be Html Examples
      EDDTableConstraintsExampleHE;
  public static final String EDDTableDataTimeExampleHE;
  public static final String EDDTableDataValueExampleHE;
  public static final String EDDTableGraphExampleHE;
  public static final String EDDTableMapExampleHE;

  public static final String // variants encoded to be Html Attributes
      EDDTableConstraintsExampleHA;
  public static final String EDDTableDataTimeExampleHA;
  public static final String EDDTableDataValueExampleHA;
  public static final String EDDTableGraphExampleHA;
  public static final String EDDTableMapExampleHA;

  public static
  String /* For the wcs examples, pick one of your grid datasets that has longitude and latitude axes.
         The sample variable must be a variable in the sample grid dataset.
         The bounding box values are minx,miny,maxx,maxy.
         */ wcsSampleDatasetID = "jplMURSST41";
  public static String wcsSampleVariable = "analysed_sst";
  public static String wcsSampleBBox = "-179.98,-89.98,179.98,89.98";
  public static String wcsSampleAltitude = "0";
  public static String wcsSampleTime = "2002-06-01T09:00:00Z";

  public static String /* For the wms examples, pick one of your grid datasets that has longitude
      and latitude axes.
      The sample variable must be a variable in the sample grid dataset.
      The bounding box values are minx,miny,maxx,maxy.
      The default for wmsActive is "true".
      */
      wmsSampleDatasetID = "jplMURSST41";
  public static String wmsSampleVariable = "analysed_sst";
  public static
  String /* The bounding box values are minLongitude,minLatitude,maxLongitude,maxLatitude.
         Longitude values within -180 to 180, or 0 to 360, are now okay. */
      wmsSampleBBox110 = "-179.99,-89.99,180.0,89.99";
  public static String wmsSampleBBox130 = "-89.99,-179.99,89.99,180.0";
  public static String wmsSampleTime = "2002-06-01T09:00:00Z";
  public static String sosFeatureOfInterest;
  public static String sosUrnBase;
  public static String sosBaseGmlName;
  public static String sosStandardNamePrefix;
  public static String
      authentication; // will be one of "", "custom", "email", "google", "orcid", "oauth2". If
  public static final String // baseHttpsUrl doesn't start with https:, this will be "".
      datasetsRegex;
  public static final String emailEverythingToCsv;
  public static final String emailDailyReportToCsv;
  public static final String emailSubscriptionsFrom;
  public static final String flagKeyKey;
  public static final String fontFamily;
  public static final String
      googleClientID; // if authentication=google or oauth2, this will be something
  public static final String
      orcidClientID; // if authentication=orcid  or oauth2, this will be something
  public static final String
      orcidClientSecret; // if authentication=orcid  or oauth2, this will be something
  public static final String googleEarthLogoFile;
  public static final String highResLogoImageFile;
  public static String legendTitle1;
  public static String legendTitle2;
  public static final String lowResLogoImageFile;
  public static final String
      passwordEncoding; // will be one of "MD5", "UEPMD5", "SHA256", "UEPSHA256"
  public static String questionMarkImageFile;
  public static final String searchEngine;
  public static final String warName;

  public static final String accessibleViaNC4; // "" if accessible, else message why not
  public static final int lowResLogoImageFileWidth;
  public static final int lowResLogoImageFileHeight;
  public static final int highResLogoImageFileWidth;
  public static final int highResLogoImageFileHeight;
  public static final int googleEarthLogoFileWidth;
  public static final int googleEarthLogoFileHeight;

  /**
   * These are special because other loggedInAs must be String2.justPrintable loggedInAsHttps is for
   * using https without being logged in, but &amp;loginInfo; indicates user isn't logged in. It is
   * a reserved username -- LoadDatasets prohibits defining a user with that name. Tab is useful
   * here: LoadDatasets prohibits it as valid userName, but it won't cause big trouble when printed
   * in tally info. anyoneLoggedIn is a role given to everyone who is logged in e.g., via a specific
   * Google email address. It is a reserved username -- LoadDatasets prohibits defining a user with
   * that name.
   */
  public static final String loggedInAsHttps = "[https]"; // final so not changeable

  public static final String loggedInAsSuperuser = "\tsuperuser"; // final so not changeable
  public static final String anyoneLoggedIn = "[anyoneLoggedIn]"; // final so not changeable
  public static final ImmutableList<String> anyoneLoggedInRoles = ImmutableList.of(anyoneLoggedIn);
  public static final int minimumPasswordLength = 8;

  // these are all non-null if in awsS3Output mode, otherwise all are null
  public static String awsS3OutputBucketUrl = null; // ends in slash
  public static String awsS3OutputBucket = null; // the short name of the bucket
  public static S3TransferManager awsS3OutputTransferManager = null;
  // public static S3Client          awsS3OutputClient          = null;

  public static final boolean listPrivateDatasets;
  public static boolean reallyVerbose;
  public static final boolean subscriptionSystemActive;
  public static final boolean convertersActive;
  public static final boolean slideSorterActive;
  public static final boolean fgdcActive;
  public static final boolean iso19115Active;
  public static final boolean jsonldActive;
  public static final boolean geoServicesRestActive;
  public static final boolean filesActive;
  public static final boolean defaultAccessibleViaFiles;
  public static final boolean dataProviderFormActive;
  public static final boolean outOfDateDatasetsActive;
  public static final boolean politicalBoundariesActive;
  public static final boolean wmsClientActive;
  public static boolean sosActive;
  public static final boolean wcsActive;
  public static final boolean wmsActive;
  public static boolean quickRestart;
  public static final boolean subscribeToRemoteErddapDataset;
  public static
  boolean // if useLuceneSearchEngine=false (a setting, or after error), original search engine will
      // be
      // used
      useLuceneSearchEngine;
  public static final boolean variablesMustHaveIoosCategory;
  public static boolean verbose;
  public static boolean useSaxParser;
  public static final boolean useEddReflection;
  public static final String[]
      categoryAttributes; // as it appears in metadata (and used for hashmap)
  public static final String[] categoryAttributesInURLs; // fileNameSafe (as used in URLs)
  public static final boolean[] categoryIsGlobal;
  public static int variableNameCategoryAttributeIndex = -1;
  public static final int logMaxSizeMB;

  public static final String emailSmtpHost;
  public static final String emailUserName;
  public static final String emailFromAddress;
  public static final String emailPassword;
  public static final String emailProperties;
  public static int emailSmtpPort = 0; // <=0 means inactive
  private static String emailLogDate = "";
  private static BufferedWriter emailLogFile;
  private static boolean emailIsActive = false; // ie if actual emails will be sent

  // these are set as a consequence of setup.xml info
  public static final SgtGraph sgtGraph;
  public static final String erddapUrl; // without slash at end
  public static final String
      erddapHttpsUrl; // without slash at end   (may be useless, but won't be null)
  public static final String
      preferredErddapUrl; // without slash at end   (https if avail, else http)
  public static final String fullDatasetDirectory; // all the Directory's have slash at end
  public static final String fullFileVisitorDirectory;
  public static final String fullCacheDirectory;
  public static final String fullDecompressedDirectory;
  public static final String fullDecompressedGenerateDatasetsXmlDirectory;
  public static final String fullLogsDirectory;
  public static final String fullCopyDirectory;
  public static final String fullLuceneDirectory;
  public static final String fullResetFlagDirectory;
  public static final String fullBadFilesFlagDirectory;
  public static final String fullHardFlagDirectory;
  public static final String fullCptCacheDirectory;
  public static final String fullPlainFileNcCacheDirectory;
  public static final String fullSgtMapTopographyCacheDirectory;
  public static final String fullTestCacheDirectory;
  public static final String fullWmsCacheDirectory;
  public static String computerName; // e.g., coastwatch (or "")
  public static Subscriptions subscriptions; // null if !EDStatic.subscriptionSystemActive

  /**
   * These values are loaded from the [contentDirectory]messages.xml file (if present) or
   * .../classes/gov/noaapfel/erddap/util/messages.xml.
   */

  // NOT TRANSLATED
  public static final String admKeywords;

  public static final String admSubsetVariables;
  public static final String advl_datasetID;
  public static final String advr_cdm_data_type;
  public static final String advr_class;
  public static final String advr_dataStructure;
  public static final String EDDChangedWasnt;
  public static final String EDDChangedDifferentNVar;
  public static final String EDDChanged2Different;
  public static final String EDDChanged1Different;
  public static final String EDDChangedCGADifferent;
  public static final String EDDChangedAxesDifferentNVar;
  public static final String EDDChangedAxes2Different;
  public static final String EDDChangedAxes1Different;
  public static final String EDDChangedNoValue;
  public static final String EDDChangedTableToGrid;
  public static final String EDDFgdc;
  public static final String EDDIso19115;
  public static final String EDDSimilarDifferentNVar;
  public static final String EDDSimilarDifferent;
  public static final String[] extensionsNoRangeRequests; // an array of extensions (not translated)
  public static final String inotifyFixCommands;
  public static String legal;
  public static String palettes[]; // an array of palettes
  public static String palettes0[]; // the array of palettes with a blank [0] item inserted
  public static final ImmutableList<String> paletteSections =
      ImmutableList.of(
          "", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
          "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31",
          "32", "33", "34", "35", "36", "37", "38", "39", "40");
  public static final String sparqlP01toP02pre;
  public static final String sparqlP01toP02post;

  // TRANSLATED
  private static final String[] // private to force use via methods, e.g., acceptEncodingHtml()
      acceptEncodingHtmlAr;
  private static final String[] filesDocumentationAr;
  public static final String[] accessRESTFULAr;
  public static final String[] acronymsAr;
  public static final String[] addConstraintsAr;
  public static final String[] addVarWhereAttNameAr;
  public static final String[] addVarWhereAttValueAr;
  public static final String[] addVarWhereAr;
  public static final String[] additionalLinksAr;
  public static final String[] admSummaryAr;
  public static final String[] admTitleAr;
  public static final String[] advc_accessibleAr;
  public static final String[] advl_accessibleAr;
  public static final String[] advl_institutionAr;
  public static final String[] advc_dataStructureAr;
  public static final String[] advl_dataStructureAr;
  public static final String[] advl_cdm_data_typeAr;
  public static final String[] advl_classAr;
  public static final String[] advl_titleAr;
  public static final String[] advl_minLongitudeAr;
  public static final String[] advl_maxLongitudeAr;
  public static final String[] advl_longitudeSpacingAr;
  public static final String[] advl_minLatitudeAr;
  public static final String[] advl_maxLatitudeAr;
  public static final String[] advl_latitudeSpacingAr;
  public static final String[] advl_minAltitudeAr;
  public static final String[] advl_maxAltitudeAr;
  public static final String[] advl_minTimeAr;
  public static final String[] advc_maxTimeAr;
  public static final String[] advl_maxTimeAr;
  public static final String[] advl_timeSpacingAr;
  public static final String[] advc_griddapAr;
  public static final String[] advl_griddapAr;
  public static final String[] advl_subsetAr;
  public static final String[] advc_tabledapAr;
  public static final String[] advl_tabledapAr;
  public static final String[] advl_MakeAGraphAr;
  public static final String[] advc_sosAr;
  public static final String[] advl_sosAr;
  public static final String[] advl_wcsAr;
  public static final String[] advl_wmsAr;
  public static final String[] advc_filesAr;
  public static final String[] advl_filesAr;
  public static final String[] advc_fgdcAr;
  public static final String[] advl_fgdcAr;
  public static final String[] advc_iso19115Ar;
  public static final String[] advl_iso19115Ar;
  public static final String[] advc_metadataAr;
  public static final String[] advl_metadataAr;
  public static final String[] advl_sourceUrlAr;
  public static final String[] advl_infoUrlAr;
  public static final String[] advl_rssAr;
  public static final String[] advc_emailAr;
  public static final String[] advl_emailAr;
  public static final String[] advl_summaryAr;
  public static final String[] advc_testOutOfDateAr;
  public static final String[] advl_testOutOfDateAr;
  public static final String[] advc_outOfDateAr;
  public static final String[] advl_outOfDateAr;
  public static final String[] advn_outOfDateAr;
  public static final String[] advancedSearchAr;
  public static final String[] advancedSearchResultsAr;
  public static final String[] advancedSearchDirectionsAr;
  public static final String[] advancedSearchTooltipAr;
  public static final String[] advancedSearchBoundsAr;
  public static final String[] advancedSearchMinLatAr;
  public static final String[] advancedSearchMaxLatAr;
  public static final String[] advancedSearchMinLonAr;
  public static final String[] advancedSearchMaxLonAr;
  public static final String[] advancedSearchMinMaxLonAr;
  public static final String[] advancedSearchMinTimeAr;
  public static final String[] advancedSearchMaxTimeAr;
  public static final String[] advancedSearchClearAr;
  public static final String[] advancedSearchClearHelpAr;
  public static final String[] advancedSearchCategoryTooltipAr;
  public static final String[] advancedSearchRangeTooltipAr;
  public static final String[] advancedSearchMapTooltipAr;
  public static final String[] advancedSearchLonTooltipAr;
  public static final String[] advancedSearchTimeTooltipAr;
  public static final String[] advancedSearchWithCriteriaAr;
  public static final String[] advancedSearchFewerCriteriaAr;
  public static final String[] advancedSearchNoCriteriaAr;
  public static final String[] advancedSearchErrorHandlingAr;
  public static final String[] autoRefreshAr;
  public static final String[] blacklistMsgAr;
  public static final String[] BroughtToYouByAr;
  public static final String[] categoryTitleHtmlAr;
  public static final String[] categoryHtmlAr;
  public static final String[] category3HtmlAr;
  public static final String[] categoryPickAttributeAr;
  public static final String[] categorySearchHtmlAr;
  public static final String[] categorySearchDifferentHtmlAr;
  public static final String[] categoryClickHtmlAr;
  public static final String[] categoryNotAnOptionAr;
  public static final String[] caughtInterruptedAr;
  public static final String[] cdmDataTypeHelpAr;
  public static final String[] clickAccessAr;
  public static final String[] clickBackgroundInfoAr;
  public static final String[] clickERDDAPAr;
  public static final String[] clickInfoAr;
  public static final String[] clickToSubmitAr;
  public static final String[] convertAr;
  public static final String[] convertBypassAr;
  public static final String[] convertToAFullNameAr;
  public static final String[] convertToAnAcronymAr;
  public static final String[] convertToACountyNameAr;
  public static final String[] convertToAFIPSCodeAr;
  public static final String[] convertToGCMDAr;
  public static final String[] convertToCFStandardNamesAr;
  public static final String[] convertToNumericTimeAr;
  public static final String[] convertToStringTimeAr;
  public static final String[] convertAnyStringTimeAr;
  public static final String[] convertToProperTimeUnitsAr;
  public static final String[] convertFromUDUNITSToUCUMAr;
  public static final String[] convertFromUCUMToUDUNITSAr;
  public static final String[] convertToUCUMAr;
  public static final String[] convertToUDUNITSAr;
  public static final String[] convertStandardizeUDUNITSAr;
  public static final String[] convertToFullNameAr;
  public static final String[] convertToVariableNameAr;
  public static final String[] converterWebServiceAr;
  public static final String[] convertOAAcronymsAr;
  public static final String[] convertOAAcronymsToFromAr;
  public static final String[] convertOAAcronymsIntroAr;
  public static final String[] convertOAAcronymsNotesAr;
  public static final String[] convertOAAcronymsServiceAr;
  public static final String[] convertOAVariableNamesAr;
  public static final String[] convertOAVariableNamesToFromAr;
  public static final String[] convertOAVariableNamesIntroAr;
  public static final String[] convertOAVariableNamesNotesAr;
  public static final String[] convertOAVariableNamesServiceAr;
  public static final String[] convertFipsCountyAr;
  public static final String[] convertFipsCountyIntroAr;
  public static final String[] convertFipsCountyNotesAr;
  public static final String[] convertFipsCountyServiceAr;
  public static final String[] convertHtmlAr;
  public static final String[] convertInterpolateAr;
  public static final String[] convertInterpolateIntroAr;
  public static final String[] convertInterpolateTLLTableAr;
  public static final String[] convertInterpolateTLLTableHelpAr;
  public static final String[] convertInterpolateDatasetIDVariableAr;
  public static final String[] convertInterpolateDatasetIDVariableHelpAr;
  public static final String[] convertInterpolateNotesAr;
  public static final String[] convertInterpolateServiceAr;
  public static final String[] convertKeywordsAr;
  public static final String[] convertKeywordsCfTooltipAr;
  public static final String[] convertKeywordsGcmdTooltipAr;
  public static final String[] convertKeywordsIntroAr;
  public static final String[] convertKeywordsNotesAr;
  public static final String[] convertKeywordsServiceAr;
  public static final String[] convertTimeAr;
  public static final String[] convertTimeReferenceAr;
  public static final String[] convertTimeIntroAr;
  public static final String[] convertTimeNotesAr;
  public static final String[] convertTimeServiceAr;
  public static final String[] convertTimeNumberTooltipAr;
  public static final String[] convertTimeStringTimeTooltipAr;
  public static final String[] convertTimeUnitsTooltipAr;
  public static final String[] convertTimeUnitsHelpAr;
  public static final String[] convertTimeIsoFormatErrorAr;
  public static final String[] convertTimeNoSinceErrorAr;
  public static final String[] convertTimeNumberErrorAr;
  public static final String[] convertTimeNumericTimeErrorAr;
  public static final String[] convertTimeParametersErrorAr;
  public static final String[] convertTimeStringFormatErrorAr;
  public static final String[] convertTimeTwoTimeErrorAr;
  public static final String[] convertTimeUnitsErrorAr;
  public static final String[] convertUnitsAr;
  public static final String[] convertUnitsComparisonAr;
  public static final String[] convertUnitsFilterAr;
  public static final String[] convertUnitsIntroAr;
  public static final String[] convertUnitsNotesAr;
  public static final String[] convertUnitsServiceAr;
  public static final String[] convertURLsAr;
  public static final String[] convertURLsIntroAr;
  public static final String[] convertURLsNotesAr;
  public static final String[] convertURLsServiceAr;
  public static final String[] cookiesHelpAr;
  public static final String[] copyImageToClipboardAr;
  public static final String[] copyTextToClipboardAr;
  public static final String[] copyToClipboardNotAvailableAr;
  public static final String[] dafAr;
  public static final String[] dafGridBypassTooltipAr;
  public static final String[] dafGridTooltipAr;
  public static final String[] dafTableBypassTooltipAr;
  public static final String[] dafTableTooltipAr;
  public static final String[] dasTitleAr;
  public static final String[] dataAccessNotAllowedAr;
  public static final String[] databaseUnableToConnectAr;
  public static final String[] dataProviderFormAr;
  public static final String[] dataProviderFormP1Ar;
  public static final String[] dataProviderFormP2Ar;
  public static final String[] dataProviderFormP3Ar;
  public static final String[] dataProviderFormP4Ar;
  public static final String[] dataProviderFormDoneAr;
  public static final String[] dataProviderFormSuccessAr;
  public static final String[] dataProviderFormShortDescriptionAr;
  public static final String[] dataProviderFormLongDescriptionHTMLAr;
  public static final String[] dataProviderFormPart1Ar;
  public static final String[] dataProviderFormPart2HeaderAr;
  public static final String[] dataProviderFormPart2GlobalMetadataAr;
  public static final String[] dataProviderContactInfoAr;
  public static final String[] dataProviderDataAr;
  public static final String[] documentationAr;
  public static final String[] dpf_submitAr;
  public static final String[] dpf_fixProblemAr;
  public static final String[] dpf_yourNameAr;
  public static final String[] dpf_emailAddressAr;
  public static final String[] dpf_TimestampAr;
  public static final String[] dpf_frequencyAr;
  public static final String[] dpf_titleAr;
  public static final String[] dpf_titleTooltipAr;
  public static final String[] dpf_summaryAr;
  public static final String[] dpf_summaryTooltipAr;
  public static final String[] dpf_creatorNameAr;
  public static final String[] dpf_creatorNameTooltipAr;
  public static final String[] dpf_creatorTypeAr;
  public static final String[] dpf_creatorTypeTooltipAr;
  public static final String[] dpf_creatorEmailAr;
  public static final String[] dpf_creatorEmailTooltipAr;
  public static final String[] dpf_institutionAr;
  public static final String[] dpf_institutionTooltipAr;
  public static final String[] dpf_infoUrlAr;
  public static final String[] dpf_infoUrlTooltipAr;
  public static final String[] dpf_licenseAr;
  public static final String[] dpf_licenseTooltipAr;
  public static final String[] dpf_howYouStoreDataAr;
  public static final String[] dpf_provideIfAvailableAr;
  public static final String[] dpf_acknowledgementAr;
  public static final String[] dpf_acknowledgementTooltipAr;
  public static final String[] dpf_historyAr;
  public static final String[] dpf_historyTooltipAr;
  public static final String[] dpf_idTooltipAr;
  public static final String[] dpf_namingAuthorityAr;
  public static final String[] dpf_namingAuthorityTooltipAr;
  public static final String[] dpf_productVersionAr;
  public static final String[] dpf_productVersionTooltipAr;
  public static final String[] dpf_referencesAr;
  public static final String[] dpf_referencesTooltipAr;
  public static final String[] dpf_commentAr;
  public static final String[] dpf_commentTooltipAr;
  public static final String[] dpf_dataTypeHelpAr;
  public static final String[] dpf_ioosCategoryAr;
  public static final String[] dpf_ioosCategoryHelpAr;
  public static final String[] dpf_part3HeaderAr;
  public static final String[] dpf_variableMetadataAr;
  public static final String[] dpf_sourceNameAr;
  public static final String[] dpf_sourceNameTooltipAr;
  public static final String[] dpf_destinationNameAr;
  public static final String[] dpf_destinationNameTooltipAr;
  public static final String[] dpf_longNameAr;
  public static final String[] dpf_longNameTooltipAr;
  public static final String[] dpf_standardNameAr;
  public static final String[] dpf_standardNameTooltipAr;
  public static final String[] dpf_dataTypeAr;
  public static final String[] dpf_fillValueAr;
  public static final String[] dpf_fillValueTooltipAr;
  public static final String[] dpf_unitsAr;
  public static final String[] dpf_unitsTooltipAr;
  public static final String[] dpf_rangeAr;
  public static final String[] dpf_rangeTooltipAr;
  public static final String[] dpf_part4HeaderAr;
  public static final String[] dpf_otherCommentAr;
  public static final String[] dpf_finishPart4Ar;
  public static final String[] dpf_congratulationAr;
  public static final String[] disabledAr;
  public static final String[] distinctValuesTooltipAr;
  public static final String[] doWithGraphsAr;
  public static final String[] dtAccessibleAr;
  public static final String[] dtAccessiblePublicAr;
  public static final String[] dtAccessibleYesAr;
  public static final String[] dtAccessibleGraphsAr;
  public static final String[] dtAccessibleNoAr;
  public static final String[] dtAccessibleLogInAr;
  public static final String[] dtLogInAr;
  public static final String[] dtDAFAr;
  public static final String[] dtFilesAr;
  public static final String[] dtMAGAr;
  public static final String[] dtSOSAr;
  public static final String[] dtSubsetAr;
  public static final String[] dtWCSAr;
  public static final String[] dtWMSAr;
  public static final String[] EasierAccessToScientificDataAr;
  public static final String[] EDDDatasetIDAr;
  public static final String[] EDDFgdcMetadataAr;
  public static final String[] EDDFilesAr;
  public static final String[] EDDIso19115MetadataAr;
  public static final String[] EDDMetadataAr;
  public static final String[] EDDBackgroundAr;
  public static final String[] EDDClickOnSubmitHtmlAr;
  public static final String[] EDDInstitutionAr;
  public static final String[] EDDInformationAr;
  public static final String[] EDDSummaryAr;
  public static final String[] EDDDatasetTitleAr;
  public static final String[] EDDDownloadDataAr;
  public static final String[] EDDMakeAGraphAr;
  public static final String[] EDDMakeAMapAr;
  public static final String[] EDDFileTypeAr;
  public static final String[] EDDFileTypeInformationAr;
  public static final String[] EDDSelectFileTypeAr;
  public static final String[] EDDMinimumAr;
  public static final String[] EDDMaximumAr;
  public static final String[] EDDConstraintAr;
  public static final String[] EDDGridDapDescriptionAr;
  public static final String[] EDDGridDapLongDescriptionAr;
  public static final String[] EDDGridDownloadDataTooltipAr;
  public static final String[] EDDGridDimensionAr;
  public static final String[] EDDGridDimensionRangesAr;
  public static final String[] EDDGridFirstAr;
  public static final String[] EDDGridLastAr;
  public static final String[] EDDGridStartAr;
  public static final String[] EDDGridStopAr;
  public static final String[] EDDGridStartStopTooltipAr;
  public static final String[] EDDGridStrideAr;
  public static final String[] EDDGridNValuesAr;
  public static final String[] EDDGridNValuesHtmlAr;
  public static final String[] EDDGridSpacingAr;
  public static final String[] EDDGridJustOneValueAr;
  public static final String[] EDDGridEvenAr;
  public static final String[] EDDGridUnevenAr;
  public static final String[] EDDGridDimensionTooltipAr;
  public static final String[] EDDGridDimensionFirstTooltipAr;
  public static final String[] EDDGridDimensionLastTooltipAr;
  public static final String[] EDDGridVarHasDimTooltipAr;
  public static final String[] EDDGridSSSTooltipAr;
  public static final String[] EDDGridStartTooltipAr;
  public static final String[] EDDGridStopTooltipAr;
  public static final String[] EDDGridStrideTooltipAr;
  public static final String[] EDDGridSpacingTooltipAr;
  public static final String[] EDDGridDownloadTooltipAr;
  public static final String[] EDDGridGridVariableHtmlAr;
  public static final String[] EDDGridCheckAllAr;
  public static final String[] EDDGridCheckAllTooltipAr;
  public static final String[] EDDGridUncheckAllAr;
  public static final String[] EDDGridUncheckAllTooltipAr;
  public static final String[] EDDTableConstraintsAr;
  public static final String[] EDDTableTabularDatasetTooltipAr;
  public static final String[] EDDTableVariableAr;
  public static final String[] EDDTableCheckAllAr;
  public static final String[] EDDTableCheckAllTooltipAr;
  public static final String[] EDDTableUncheckAllAr;
  public static final String[] EDDTableUncheckAllTooltipAr;
  public static final String[] EDDTableMinimumTooltipAr;
  public static final String[] EDDTableMaximumTooltipAr;
  public static final String[] EDDTableCheckTheVariablesAr;
  public static final String[] EDDTableSelectAnOperatorAr;
  public static final String[] EDDTableFromEDDGridSummaryAr;
  public static final String[] EDDTableOptConstraint1HtmlAr;
  public static final String[] EDDTableOptConstraint2HtmlAr;
  public static final String[] EDDTableOptConstraintVarAr;
  public static final String[] EDDTableNumericConstraintTooltipAr;
  public static final String[] EDDTableStringConstraintTooltipAr;
  public static final String[] EDDTableTimeConstraintTooltipAr;
  public static final String[] EDDTableConstraintTooltipAr;
  public static final String[] EDDTableSelectConstraintTooltipAr;
  public static final String[] EDDTableDapDescriptionAr;
  public static final String[] EDDTableDapLongDescriptionAr;
  public static final String[] EDDTableDownloadDataTooltipAr;
  public static final String[] erddapIsAr;
  public static final String[] erddapVersionHTMLAr;
  public static final String[] errorTitleAr;
  public static final String[] errorRequestUrlAr;
  public static final String[] errorRequestQueryAr;
  public static final String[] errorTheErrorAr;
  public static final String[] errorCopyFromAr;
  public static final String[] errorFileNotFoundAr;
  public static final String[] errorFileNotFoundImageAr;
  public static final String[] errorInternalAr;
  public static final String[] errorJsonpFunctionNameAr;
  public static final String[] errorJsonpNotAllowedAr;
  public static final String[] errorMoreThan2GBAr;
  public static final String[] errorNotFoundAr;
  public static final String[] errorNotFoundInAr;
  public static final String[] errorOdvLLTGridAr;
  public static final String[] errorOdvLLTTableAr;
  public static final String[] errorOnWebPageAr;
  public static final String[] externalLinkAr;
  public static final String[] externalWebSiteAr;
  public static final String[] fileHelp_ascAr;
  public static final String[] fileHelp_csvAr;
  public static final String[] fileHelp_csvpAr;
  public static final String[] fileHelp_csv0Ar;
  public static final String[] fileHelp_dataTableAr;
  public static final String[] fileHelp_dasAr;
  public static final String[] fileHelp_ddsAr;
  public static final String[] fileHelp_dodsAr;
  public static final String[] fileHelpGrid_esriAsciiAr;
  public static final String[] fileHelpTable_esriCsvAr;
  public static final String[] fileHelp_fgdcAr;
  public static final String[] fileHelp_geoJsonAr;
  public static final String[] fileHelp_graphAr;
  public static final String[] fileHelpGrid_helpAr;
  public static final String[] fileHelpTable_helpAr;
  public static final String[] fileHelp_htmlAr;
  public static final String[] fileHelp_htmlTableAr;
  public static final String[] fileHelp_iso19115Ar;
  public static final String[] fileHelp_itxGridAr;
  public static final String[] fileHelp_itxTableAr;
  public static final String[] fileHelp_jsonAr;
  public static final String[] fileHelp_jsonlCSV1Ar;
  public static final String[] fileHelp_jsonlCSVAr;
  public static final String[] fileHelp_jsonlKVPAr;
  public static final String[] fileHelp_matAr;
  public static final String[] fileHelpGrid_nc3Ar;
  public static final String[] fileHelpGrid_nc4Ar;
  public static final String[] fileHelpTable_nc3Ar;
  public static final String[] fileHelpTable_nc4Ar;
  public static final String[] fileHelp_nc3HeaderAr;
  public static final String[] fileHelp_nc4HeaderAr;
  public static final String[] fileHelp_nccsvAr;
  public static final String[] fileHelp_nccsvMetadataAr;
  public static final String[] fileHelp_ncCFAr;
  public static final String[] fileHelp_ncCFHeaderAr;
  public static final String[] fileHelp_ncCFMAAr;
  public static final String[] fileHelp_ncCFMAHeaderAr;
  public static final String[] fileHelp_ncmlAr;
  public static final String[] fileHelp_ncoJsonAr;
  public static final String[] fileHelpGrid_odvTxtAr;
  public static final String[] fileHelpTable_odvTxtAr;
  public static final String[] fileHelp_parquetAr;
  public static final String[] fileHelp_parquet_with_metaAr;
  public static final String[] fileHelp_subsetAr;
  public static final String[] fileHelp_timeGapsAr;
  public static final String[] fileHelp_tsvAr;
  public static final String[] fileHelp_tsvpAr;
  public static final String[] fileHelp_tsv0Ar;
  public static final String[] fileHelp_wavAr;
  public static final String[] fileHelp_xhtmlAr;
  public static final String[] fileHelp_geotifAr; // graphical
  public static final String[] fileHelpGrid_kmlAr;
  public static final String[] fileHelpTable_kmlAr;
  public static final String[] fileHelp_smallPdfAr;
  public static final String[] fileHelp_pdfAr;
  public static final String[] fileHelp_largePdfAr;
  public static final String[] fileHelp_smallPngAr;
  public static final String[] fileHelp_pngAr;
  public static final String[] fileHelp_largePngAr;
  public static final String[] fileHelp_transparentPngAr;
  public static final String[] filesDescriptionAr;
  public static final String[] filesSortAr;
  public static final String[] filesWarningAr;
  public static final String[] findOutChangeAr;
  public static final String[] FIPSCountyCodesAr;
  public static final String[] forSOSUseAr;
  public static final String[] forWCSUseAr;
  public static final String[] forWMSUseAr;
  public static final String[] functionsAr;
  public static final String[] functionTooltipAr;
  public static final String[] functionDistinctCheckAr;
  public static final String[] functionDistinctTooltipAr;
  public static final String[] functionOrderByExtraAr;
  public static final String[] functionOrderByTooltipAr;
  public static final String[] functionOrderBySortAr;
  public static final String[] functionOrderBySort1Ar;
  public static final String[] functionOrderBySort2Ar;
  public static final String[] functionOrderBySort3Ar;
  public static final String[] functionOrderBySort4Ar;
  public static final String[] functionOrderBySortLeastAr;
  public static final String[] functionOrderBySortRowMaxAr;
  public static final String[] generatedAtAr;
  public static final String[] geoServicesDescriptionAr;
  public static final String[] getStartedHtmlAr;
  public static final String[] helpAr;
  public static final String[] htmlTableMaxMessageAr;
  public static final String[] imageDataCourtesyOfAr;
  public static final String[] imagesEmbedAr;
  public static final String[] indexViewAllAr;
  public static final String[] indexSearchWithAr;
  public static final String[] indexDevelopersSearchAr;
  public static final String[] indexProtocolAr;
  public static final String[] indexDescriptionAr;
  public static final String[] indexDatasetsAr;
  public static final String[] indexDocumentationAr;
  public static final String[] indexRESTfulSearchAr;
  public static final String[] indexAllDatasetsSearchAr;
  public static final String[] indexOpenSearchAr;
  public static final String[] indexServicesAr;
  public static final String[] indexDescribeServicesAr;
  public static final String[] indexMetadataAr;
  public static final String[] indexWAF1Ar;
  public static final String[] indexWAF2Ar;
  public static final String[] indexConvertersAr;
  public static final String[] indexDescribeConvertersAr;
  public static final String[] infoAboutFromAr;
  public static final String[] infoTableTitleHtmlAr;
  public static final String[] infoRequestFormAr;
  public static final String[] informationAr;
  public static final String[] inotifyFixAr;
  public static final String[] interpolateAr;
  public static final String[] javaProgramsHTMLAr;
  public static final String[] justGenerateAndViewAr;
  public static final String[] justGenerateAndViewTooltipAr;
  public static final String[] justGenerateAndViewUrlAr;
  public static final String[] justGenerateAndViewGraphUrlTooltipAr;
  public static final String[] keywordsAr;
  public static final String[] langCodeAr;
  public static final String[] legalNoticesAr;
  public static final String[] legalNoticesTitleAr;
  public static final String[] licenseAr;
  public static final String[] likeThisAr;
  public static final String[] listAllAr;
  public static final String[] listOfDatasetsAr;
  public static final String[] LogInAr;
  public static final String[] loginAr;
  public static final String[] loginHTMLAr;
  public static final String[] loginAttemptBlockedAr;
  public static final String[] loginDescribeCustomAr;
  public static final String[] loginDescribeEmailAr;
  public static final String[] loginDescribeGoogleAr;
  public static final String[] loginDescribeOrcidAr;
  public static final String[] loginDescribeOauth2Ar;
  public static final String[] loginErddapAr;
  public static final String[] loginCanNotAr;
  public static final String[] loginAreNotAr;
  public static final String[] loginToLogInAr;
  public static final String[] loginEmailAddressAr;
  public static final String[] loginYourEmailAddressAr;
  public static final String[] loginUserNameAr;
  public static final String[] loginPasswordAr;
  public static final String[] loginUserNameAndPasswordAr;
  public static final String[] loginGoogleSignInAr;
  public static final String[] loginOrcidSignInAr;
  public static final String[] loginOpenIDAr;
  public static final String[] loginOpenIDOrAr;
  public static final String[] loginOpenIDCreateAr;
  public static final String[] loginOpenIDFreeAr;
  public static final String[] loginOpenIDSameAr;
  public static final String[] loginAsAr;
  public static final String[] loginPartwayAsAr;
  public static final String[] loginFailedAr;
  public static final String[] loginSucceededAr;
  public static final String[] loginInvalidAr;
  public static final String[] loginNotAr;
  public static final String[] loginBackAr;
  public static final String[] loginProblemExactAr;
  public static final String[] loginProblemExpireAr;
  public static final String[] loginProblemGoogleAgainAr;
  public static final String[] loginProblemOrcidAgainAr;
  public static final String[] loginProblemOauth2AgainAr;
  public static final String[] loginProblemSameBrowserAr;
  public static final String[] loginProblem3TimesAr;
  public static final String[] loginProblemsAr;
  public static final String[] loginProblemsAfterAr;
  public static final String[] loginPublicAccessAr;
  public static final String[] LogOutAr;
  public static final String[] logoutAr;
  public static final String[] logoutOpenIDAr;
  public static final String[] logoutSuccessAr;
  public static final String[] magAr;
  public static final String[] magAxisXAr;
  public static final String[] magAxisYAr;
  public static final String[] magAxisColorAr;
  public static final String[] magAxisStickXAr;
  public static final String[] magAxisStickYAr;
  public static final String[] magAxisVectorXAr;
  public static final String[] magAxisVectorYAr;
  public static final String[] magAxisHelpGraphXAr;
  public static final String[] magAxisHelpGraphYAr;
  public static final String[] magAxisHelpMarkerColorAr;
  public static final String[] magAxisHelpSurfaceColorAr;
  public static final String[] magAxisHelpStickXAr;
  public static final String[] magAxisHelpStickYAr;
  public static final String[] magAxisHelpMapXAr;
  public static final String[] magAxisHelpMapYAr;
  public static final String[] magAxisHelpVectorXAr;
  public static final String[] magAxisHelpVectorYAr;
  public static final String[] magAxisVarHelpAr;
  public static final String[] magAxisVarHelpGridAr;
  public static final String[] magConstraintHelpAr;
  public static final String[] magDocumentationAr;
  public static final String[] magDownloadAr;
  public static final String[] magDownloadTooltipAr;
  public static final String[] magFileTypeAr;
  public static final String[] magGraphTypeAr;
  public static final String[] magGraphTypeTooltipGridAr;
  public static final String[] magGraphTypeTooltipTableAr;
  public static final String[] magGSAr;
  public static final String[] magGSMarkerTypeAr;
  public static final String[] magGSSizeAr;
  public static final String[] magGSColorAr;
  public static final String[] magGSColorBarAr;
  public static final String[] magGSColorBarTooltipAr;
  public static final String[] magGSContinuityAr;
  public static final String[] magGSContinuityTooltipAr;
  public static final String[] magGSScaleAr;
  public static final String[] magGSScaleTooltipAr;
  public static final String[] magGSMinAr;
  public static final String[] magGSMinTooltipAr;
  public static final String[] magGSMaxAr;
  public static final String[] magGSMaxTooltipAr;
  public static final String[] magGSNSectionsAr;
  public static final String[] magGSNSectionsTooltipAr;
  public static final String[] magGSLandMaskAr;
  public static final String[] magGSLandMaskTooltipGridAr;
  public static final String[] magGSLandMaskTooltipTableAr;
  public static final String[] magGSVectorStandardAr;
  public static final String[] magGSVectorStandardTooltipAr;
  public static final String[] magGSYAscendingTooltipAr;
  public static final String[] magGSYAxisMinAr;
  public static final String[] magGSYAxisMaxAr;
  public static final String[] magGSYRangeMinTooltipAr;
  public static final String[] magGSYRangeMaxTooltipAr;
  public static final String[] magGSYRangeTooltipAr;
  public static final String[] magGSYScaleTooltipAr;
  public static final String[] magItemFirstAr;
  public static final String[] magItemPreviousAr;
  public static final String[] magItemNextAr;
  public static final String[] magItemLastAr;
  public static final String[] magJust1ValueAr;
  public static final String[] magRangeAr;
  public static final String[] magRangeToAr;
  public static final String[] magRedrawAr;
  public static final String[] magRedrawTooltipAr;
  public static final String[] magTimeRangeAr;
  public static final String[] magTimeRangeFirstAr;
  public static final String[] magTimeRangeBackAr;
  public static final String[] magTimeRangeForwardAr;
  public static final String[] magTimeRangeLastAr;
  public static final String[] magTimeRangeTooltipAr;
  public static final String[] magTimeRangeTooltip2Ar;
  public static final String[] magTimesVaryAr;
  public static final String[] magViewUrlAr;
  public static final String[] magZoomAr;
  public static final String[] magZoomCenterAr;
  public static final String[] magZoomCenterTooltipAr;
  public static final String[] magZoomInAr;
  public static final String[] magZoomInTooltipAr;
  public static final String[] magZoomOutAr;
  public static final String[] magZoomOutTooltipAr;
  public static final String[] magZoomALittleAr;
  public static final String[] magZoomDataAr;
  public static final String[] magZoomOutDataAr;
  public static final String[] magGridTooltipAr;
  public static final String[] magTableTooltipAr;
  public static final String[] metadataDownloadAr;
  public static final String[] moreInformationAr;
  public static final String[] nMatching1Ar;
  public static final String[] nMatchingAr;
  public static final String[] nMatchingAlphabeticalAr;
  public static final String[] nMatchingMostRelevantAr;
  public static final String[] nMatchingPageAr;
  public static final String[] nMatchingCurrentAr;
  public static final String[] noDataFixedValueAr;
  public static final String[] noDataNoLLAr;
  public static final String[] noDatasetWithAr;
  public static final String[] noPage1Ar;
  public static final String[] noPage2Ar;
  public static final String[] notAllowedAr;
  public static final String[] notAuthorizedAr;
  public static final String[] notAuthorizedForDataAr;
  public static final String[] notAvailableAr;
  public static final String[] noteAr;
  public static final String[] noXxxAr;
  public static final String[] noXxxBecauseAr;
  public static final String[] noXxxBecause2Ar;
  public static final String[] noXxxNotActiveAr;
  public static final String[] noXxxNoAxis1Ar;
  public static final String[] noXxxNoColorBarAr;
  public static final String[] noXxxNoCdmDataTypeAr;
  public static final String[] noXxxNoLLAr;
  public static final String[] noXxxNoLLEvenlySpacedAr;
  public static final String[] noXxxNoLLGt1Ar;
  public static final String[] noXxxNoLLTAr;
  public static final String[] noXxxNoLonIn180Ar;
  public static final String[] noXxxNoNonStringAr;
  public static final String[] noXxxNo2NonStringAr;
  public static final String[] noXxxNoStationAr;
  public static final String[] noXxxNoStationIDAr;
  public static final String[] noXxxNoSubsetVariablesAr;
  public static final String[] noXxxNoOLLSubsetVariablesAr;
  public static final String[] noXxxNoMinMaxAr;
  public static final String[] noXxxItsGriddedAr;
  public static final String[] noXxxItsTabularAr;
  public static final String[] oneRequestAtATimeAr;
  public static final String[] openSearchDescriptionAr;
  public static final String[] optionalAr;
  public static final String[] optionsAr;
  public static final String[] orAListOfValuesAr;
  public static final String[] orRefineSearchWithAr;
  public static final String[] orSearchWithAr;
  public static final String[] orCommaAr;
  public static final String[] otherFeaturesAr;
  public static final String[] outOfDateDatasetsAr;
  public static final String[] outOfDateKeepTrackAr;
  public static final String[] outOfDateHtmlAr;
  public static final String[] patientDataAr;
  public static final String[] patientYourGraphAr;
  public static final String[] percentEncodeAr;
  public static final String[] pickADatasetAr;
  public static final String[] protocolSearchHtmlAr;
  public static final String[] protocolSearch2HtmlAr;
  public static final String[] protocolClickAr;
  public static final String[] queryErrorAr;
  public static final String[] queryError180Ar;
  public static final String[] queryError1ValueAr;
  public static final String[] queryError1VarAr;
  public static final String[] queryError2VarAr;
  public static final String[] queryErrorActualRangeAr;
  public static final String[] queryErrorAdjustedAr;
  public static final String[] queryErrorAscendingAr;
  public static final String[] queryErrorConstraintNaNAr;
  public static final String[] queryErrorEqualSpacingAr;
  public static final String[] queryErrorExpectedAtAr;
  public static final String[] queryErrorFileTypeAr;
  public static final String[] queryErrorInvalidAr;
  public static final String[] queryErrorLLAr;
  public static final String[] queryErrorLLGt1Ar;
  public static final String[] queryErrorLLTAr;
  public static final String[] queryErrorNeverTrueAr;
  public static final String[] queryErrorNeverBothTrueAr;
  public static final String[] queryErrorNotAxisAr;
  public static final String[] queryErrorNotExpectedAtAr;
  public static final String[] queryErrorNotFoundAfterAr;
  public static final String[] queryErrorOccursTwiceAr;
  public static final String[] queryErrorOrderByClosestAr;
  public static final String[] queryErrorOrderByLimitAr;
  public static final String[] queryErrorOrderByMeanAr;
  public static final String[] queryErrorOrderBySumAr;
  public static final String[] queryErrorOrderByVariableAr;
  public static final String[] queryErrorUnknownVariableAr;
  public static final String[] queryErrorGrid1AxisAr;
  public static final String[] queryErrorGridAmpAr;
  public static final String[] queryErrorGridDiagnosticAr;
  public static final String[] queryErrorGridBetweenAr;
  public static final String[] queryErrorGridLessMinAr;
  public static final String[] queryErrorGridGreaterMaxAr;
  public static final String[] queryErrorGridMissingAr;
  public static final String[] queryErrorGridNoAxisVarAr;
  public static final String[] queryErrorGridNoDataVarAr;
  public static final String[] queryErrorGridNotIdenticalAr;
  public static final String[] queryErrorGridSLessSAr;
  public static final String[] queryErrorLastEndPAr;
  public static final String[] queryErrorLastExpectedAr;
  public static final String[] queryErrorLastUnexpectedAr;
  public static final String[] queryErrorLastPMInvalidAr;
  public static final String[] queryErrorLastPMIntegerAr;
  public static final String[] rangesFromToAr;
  public static final String[] requiredAr;
  public static final String[] resetTheFormAr;
  public static final String[] resetTheFormWasAr;
  public static final String[] resourceNotFoundAr;
  public static final String[] restfulWebServicesAr;
  public static final String[] restfulHTMLAr;
  public static final String[] restfulHTMLContinuedAr;
  public static final String[] restfulGetAllDatasetAr;
  public static final String[] restfulProtocolsAr;
  public static final String[] SOSDocumentationAr;
  public static final String[] WCSDocumentationAr;
  public static final String[] WMSDocumentationAr;
  public static final String[] requestFormatExamplesHtmlAr;
  public static final String[] resultsFormatExamplesHtmlAr;
  public static final String[] resultsOfSearchForAr;
  public static final String[] restfulInformationFormatsAr;
  public static final String[] restfulViaServiceAr;
  public static final String[] rowsAr;
  public static final String[] rssNoAr;
  public static final String[] searchTitleAr;
  public static final String[] searchDoFullTextHtmlAr;
  public static final String[] searchFullTextHtmlAr;
  public static final String[] searchHintsLuceneTooltipAr;
  public static final String[] searchHintsOriginalTooltipAr;
  public static final String[] searchHintsTooltipAr;
  public static final String[] searchButtonAr;
  public static final String[] searchClickTipAr;
  public static final String[] searchMultipleERDDAPsAr;
  public static final String[] searchMultipleERDDAPsDescriptionAr;
  public static final String[] searchNotAvailableAr;
  public static final String[] searchTipAr;
  public static final String[] searchSpellingAr;
  public static final String[] searchFewerWordsAr;
  public static final String[] searchWithQueryAr;
  public static final String[] seeProtocolDocumentationAr;
  public static final String[] selectNextAr;
  public static final String[] selectPreviousAr;
  public static final String[] shiftXAllTheWayLeftAr;
  public static final String[] shiftXLeftAr;
  public static final String[] shiftXRightAr;
  public static final String[] shiftXAllTheWayRightAr;
  public static final String[] slideSorterAr;
  public static final String[] SOSAr;
  public static final String[] sosDescriptionHtmlAr;
  public static final String[] sosLongDescriptionHtmlAr;
  public static final String[] sosOverview1Ar;
  public static final String[] sosOverview2Ar;
  public static final String[] ssUseAr;
  public static final String[] ssUsePlainAr;
  public static final String[] ssBePatientAr;
  public static final String[] ssInstructionsHtmlAr;
  public static final String[] standardShortDescriptionHtmlAr;
  public static final String[] statusAr;
  public static final String[] statusHtmlAr;
  public static final String[] submitAr;
  public static final String[] submitTooltipAr;
  public static final String[] subscriptionOfferRssAr;
  public static final String[] subscriptionOfferUrlAr;
  public static final String[] subscriptionsTitleAr;
  public static final String[] subscriptionEmailListAr;
  public static final String[] subscriptionAddAr;
  public static final String[] subscriptionAddHtmlAr;
  public static final String[] subscriptionValidateAr;
  public static final String[] subscriptionValidateHtmlAr;
  public static final String[] subscriptionListAr;
  public static final String[] subscriptionListHtmlAr;
  public static final String[] subscriptionRemoveAr;
  public static final String[] subscriptionRemoveHtmlAr;
  public static final String[] subscriptionAbuseAr;
  public static final String[] subscriptionAddErrorAr;
  public static final String[] subscriptionAdd2Ar;
  public static final String[] subscriptionAddSuccessAr;
  public static final String[] subscriptionEmailAr;
  public static final String[] subscriptionEmailOnBlacklistAr;
  public static final String[] subscriptionEmailInvalidAr;
  public static final String[] subscriptionEmailTooLongAr;
  public static final String[] subscriptionEmailUnspecifiedAr;
  public static final String[] subscription0HtmlAr;
  public static final String[] subscription1HtmlAr;
  public static final String[] subscription2HtmlAr;
  public static final String[] subscriptionIDInvalidAr;
  public static final String[] subscriptionIDTooLongAr;
  public static final String[] subscriptionIDUnspecifiedAr;
  public static final String[] subscriptionKeyInvalidAr;
  public static final String[] subscriptionKeyUnspecifiedAr;
  public static final String[] subscriptionListErrorAr;
  public static final String[] subscriptionListSuccessAr;
  public static final String[] subscriptionRemoveErrorAr;
  public static final String[] subscriptionRemove2Ar;
  public static final String[] subscriptionRemoveSuccessAr;
  public static final String[] subscriptionRSSAr;
  public static final String[] subscriptionsNotAvailableAr;
  public static final String[] subscriptionUrlHtmlAr;
  public static final String[] subscriptionUrlInvalidAr;
  public static final String[] subscriptionUrlTooLongAr;
  public static final String[] subscriptionValidateErrorAr;
  public static final String[] subscriptionValidateSuccessAr;
  public static final String[] subsetAr;
  public static final String[] subsetSelectAr;
  public static final String[] subsetNMatchingAr;
  public static final String[] subsetInstructionsAr;
  public static final String[] subsetOptionAr;
  public static final String[] subsetOptionsAr;
  public static final String[] subsetRefineMapDownloadAr;
  public static final String[] subsetRefineSubsetDownloadAr;
  public static final String[] subsetClickResetClosestAr;
  public static final String[] subsetClickResetLLAr;
  public static final String[] subsetMetadataAr;
  public static final String[] subsetCountAr;
  public static final String[] subsetPercentAr;
  public static final String[] subsetViewSelectAr;
  public static final String[] subsetViewSelectDistinctCombosAr;
  public static final String[] subsetViewSelectRelatedCountsAr;
  public static final String[] subsetWhenAr;
  public static final String[] subsetWhenNoConstraintsAr;
  public static final String[] subsetWhenCountsAr;
  public static final String[] subsetComboClickSelectAr;
  public static final String[] subsetNVariableCombosAr;
  public static final String[] subsetShowingAllRowsAr;
  public static final String[] subsetShowingNRowsAr;
  public static final String[] subsetChangeShowingAr;
  public static final String[] subsetNRowsRelatedDataAr;
  public static final String[] subsetViewRelatedChangeAr;
  public static final String[] subsetTotalCountAr;
  public static final String[] subsetViewAr;
  public static final String[] subsetViewCheckAr;
  public static final String[] subsetViewCheck1Ar;
  public static final String[] subsetViewDistinctMapAr;
  public static final String[] subsetViewRelatedMapAr;
  public static final String[] subsetViewDistinctDataCountsAr;
  public static final String[] subsetViewDistinctDataAr;
  public static final String[] subsetViewRelatedDataCountsAr;
  public static final String[] subsetViewRelatedDataAr;
  public static final String[] subsetViewDistinctMapTooltipAr;
  public static final String[] subsetViewRelatedMapTooltipAr;
  public static final String[] subsetViewDistinctDataCountsTooltipAr;
  public static final String[] subsetViewDistinctDataTooltipAr;
  public static final String[] subsetViewRelatedDataCountsTooltipAr;
  public static final String[] subsetViewRelatedDataTooltipAr;
  public static final String[] subsetWarnAr;
  public static final String[] subsetWarn10000Ar;
  public static final String[] subsetTooltipAr;
  public static final String[] subsetNotSetUpAr;
  public static final String[] subsetLongNotShownAr;
  public static final String[] tabledapVideoIntroAr;
  public static final String[] theDatasetIDAr;
  public static final String[] theKeyAr;
  public static final String[] theSubscriptionIDAr;
  public static final String[] theUrlActionAr;
  public static final String[] ThenAr;
  public static final String[] thisParticularErddapAr;
  public static final String[] timeAr;
  public static final String[] timeoutOtherRequestsAr;
  public static final String[] unitsAr;
  public static final String[] unknownDatasetIDAr;
  public static final String[] unknownProtocolAr;
  public static final String[] unsupportedFileTypeAr;
  public static final String[] updateUrlsFrom; // not Ar. They were arrays before and now
  public static final String[] updateUrlsTo; // not Ar
  public static final String[] updateUrlsSkipAttributes; // not Ar
  public static final String[] usingGriddapAr;
  public static final String[] usingTabledapAr;
  public static final String[] variableNamesAr;
  public static final String[] viewAllDatasetsHtmlAr;
  public static final String[] waitThenTryAgainAr;
  public static final String[] warningAr;
  public static final String[] WCSAr;
  public static final String[] wcsDescriptionHtmlAr;
  public static final String[] wcsLongDescriptionHtmlAr;
  public static final String[] wcsOverview1Ar;
  public static final String[] wcsOverview2Ar;
  public static final String[] wmsDescriptionHtmlAr;
  public static final String[] WMSDocumentation1Ar;
  public static final String[] WMSGetCapabilitiesAr;
  public static final String[] WMSGetMapAr;
  public static final String[] WMSNotesAr;
  public static final String[] wmsInstructionsAr;
  public static final String[] wmsLongDescriptionHtmlAr;
  public static final String[] wmsManyDatasetsAr;
  public static final String[] yourEmailAddressAr;
  public static final String[] zoomInAr;
  public static final String[] zoomOutAr;
  public static final int[] imageWidths;
  public static final int[] imageHeights;
  public static final int[] pdfWidths;
  public static final int[] pdfHeights;
  private static final String[] theLongDescriptionHtmlAr; // see the xxx() methods
  public static final String errorFromDataSource = String2.ERROR + " from data source: ";
  public static final int nLanguages = TranslateMessages.languageList.size();

  /**
   * These are only created/used by GenerateDatasetsXml threads. See the related methods below that
   * create them.
   */
  private static Table gdxAcronymsTable;

  private static Map<String, String> gdxAcronymsHashMap, gdxVariableNamesHashMap;
  public static boolean useSharedWatchService = true;

  /**
   * This static block reads this class's static String values from contentDirectory, which must
   * contain setup.xml and datasets.xml (and may contain messages.xml). It may be a defined
   * environment variable ("erddapContentDirectory") or a subdir of <tomcat> (e.g.,
   * usr/local/tomcat/content/erddap/) (more specifically, a sibling of 'tomcat'/webapps).
   *
   * @throws RuntimeException if trouble
   */
  static {
    String erdStartup = "EDStatic Low Level Startup";
    String errorInMethod = "";
    try {
      String webInfParentDirectory = File2.getWebInfParentDirectory();

      fullPaletteDirectory = webInfParentDirectory + "WEB-INF/cptfiles/";
      fullPublicDirectory = webInfParentDirectory + PUBLIC_DIR;
      downloadDir = webInfParentDirectory + DOWNLOAD_DIR; // local directory on this computer
      imageDir = webInfParentDirectory + IMAGES_DIR; // local directory on this computer

      skipEmailThread = Boolean.parseBoolean(System.getProperty("skipEmailThread"));

      // route calls to a logger to com.cohort.util.String2Log
      String2.setupCommonsLogging(-1);
      SSR.erddapVersion = erddapVersion;

      String eol = String2.lineSeparator;
      String2.log(
          eol
              + "////**** "
              + erdStartup
              + eol
              + "localTime="
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + eol
              + "erddapVersion="
              + erddapVersion
              + eol
              + String2.standardHelpAboutMessage());

      // **** find contentDirectory
      String ecd = "erddapContentDirectory"; // the name of the environment variable
      errorInMethod =
          "Couldn't find 'content' directory ([tomcat]/content/erddap/ ?) "
              + "because '"
              + ecd
              + "' environment variable not found "
              + "and couldn't find '/webapps/' "
              + // with / separator and / at the end
              " (and 'content/erddap' should be a sibling of <tomcat>/webapps): ";
      contentDirectory = System.getProperty(ecd);
      if (contentDirectory == null) {
        // Or, it must be sibling of webapps
        // e.g., c:/programs/_tomcat/webapps/erddap/WEB-INF/classes/[these classes]
        // On windows, contentDirectory may have spaces as %20(!)
        contentDirectory = File2.getClassPath(); // access a resource folder
        int po = contentDirectory.indexOf("/webapps/");
        contentDirectory =
            contentDirectory.substring(0, po) + "/content/erddap/"; // exception if po=-1
      } else {
        contentDirectory = File2.addSlash(contentDirectory);
      }
      Test.ensureTrue(
          File2.isDirectory(contentDirectory),
          "contentDirectory (" + contentDirectory + ") doesn't exist.");

      // **** setup.xml  *************************************************************
      // This is read BEFORE messages.xml. If that is a problem for something,
      //  defer reading it in setup and add it to the messages section.
      // read static Strings from setup.xml
      String setupFileName = contentDirectory + "setup" + (developmentMode ? "2" : "") + ".xml";
      errorInMethod = "ERROR while reading " + setupFileName + ": ";
      ResourceBundle2 setup = ResourceBundle2.fromXml(XML.parseXml(setupFileName, false));
      Map<String, String> ev = System.getenv();

      // logLevel may be: warning, info(default), all
      setLogLevel(getSetupEVString(setup, ev, "logLevel", DEFAULT_logLevel));

      usePrometheusMetrics = getSetupEVBoolean(setup, ev, "usePrometheusMetrics", true);
      if (usePrometheusMetrics) {
        JvmMetrics.builder().register(); // initialize the out-of-the-box JVM metrics
      }

      bigParentDirectory = getSetupEVNotNothingString(setup, ev, "bigParentDirectory", "");
      bigParentDirectory = File2.addSlash(bigParentDirectory);
      Path bpd = Path.of(bigParentDirectory);
      if (!bpd.isAbsolute()) {
        if (!File2.isDirectory(bigParentDirectory)) {
          bigParentDirectory = File2.getWebInfParentDirectory() + bigParentDirectory;
        }
      }
      Test.ensureTrue(
          File2.isDirectory(bigParentDirectory),
          "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");

      // email  (do early on so email can be sent if trouble later in this method)
      emailSmtpHost = getSetupEVString(setup, ev, "emailSmtpHost", (String) null);
      emailSmtpPort = getSetupEVInt(setup, ev, "emailSmtpPort", 25);
      emailUserName = getSetupEVString(setup, ev, "emailUserName", (String) null);
      emailPassword = getSetupEVString(setup, ev, "emailPassword", (String) null);
      emailProperties = getSetupEVString(setup, ev, "emailProperties", (String) null);
      emailFromAddress = getSetupEVString(setup, ev, "emailFromAddress", (String) null);
      emailEverythingToCsv = getSetupEVString(setup, ev, "emailEverythingTo", ""); // won't be null
      emailDailyReportToCsv =
          getSetupEVString(setup, ev, "emailDailyReportTo", ""); // won't be null
      emailIsActive = // ie if actual emails will be sent
          String2.isSomething(emailSmtpHost)
              && emailSmtpPort > 0
              && String2.isSomething(emailUserName)
              && String2.isSomething(emailPassword)
              && String2.isEmailAddress(emailFromAddress);

      String tsar[] = String2.split(emailEverythingToCsv, ',');
      if (emailEverythingToCsv.length() > 0)
        for (String s : tsar)
          if (!String2.isEmailAddress(s)
              || s.startsWith("your.")) // prohibit the default email addresses
          throw new RuntimeException(
                "setup.xml error: invalid email address=" + s + " in <emailEverythingTo>.");
      emailSubscriptionsFrom = tsar.length > 0 ? tsar[0] : ""; // won't be null

      tsar = String2.split(emailDailyReportToCsv, ',');
      if (emailDailyReportToCsv.length() > 0) {
        for (String s : tsar)
          if (!String2.isEmailAddress(s)
              || s.startsWith("your.")) // prohibit the default email addresses
          throw new RuntimeException(
                "setup.xml error: invalid email address=" + s + " in <emailDailyReportTo>.");
      }

      if (!skipEmailThread) {
        ensureEmailThreadIsRunningIfNeeded();
      }
      ensureTouchThreadIsRunningIfNeeded();

      // test of email
      // Test.error("This is a test of emailing an error in Erddap constructor.");

      // *** set up directories  //all with slashes at end
      // before 2011-12-30, was fullDatasetInfoDirectory datasetInfo/; see conversion below
      fullDatasetDirectory = bigParentDirectory + "dataset/";
      fullFileVisitorDirectory = fullDatasetDirectory + "_FileVisitor/";
      FileVisitorDNLS.FILE_VISITOR_DIRECTORY = fullFileVisitorDirectory;
      File2.deleteAllFiles(
          fullFileVisitorDirectory); // no temp file list can be active at ERDDAP restart
      fullCacheDirectory = bigParentDirectory + "cache/";
      fullDecompressedDirectory = bigParentDirectory + "decompressed/";
      fullDecompressedGenerateDatasetsXmlDirectory =
          bigParentDirectory + "decompressed/GenerateDatasetsXml/";
      fullResetFlagDirectory = bigParentDirectory + "flag/";
      fullBadFilesFlagDirectory = bigParentDirectory + "badFilesFlag/";
      fullHardFlagDirectory = bigParentDirectory + "hardFlag/";
      fullLogsDirectory = bigParentDirectory + "logs/";
      fullCopyDirectory = bigParentDirectory + "copy/";
      fullLuceneDirectory = bigParentDirectory + "lucene/";

      Test.ensureTrue(
          File2.isDirectory(fullPaletteDirectory),
          "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
      errorInMethod =
          "ERROR while creating directories: "; // File2.makeDir throws exception if failure
      File2.makeDirectory(fullPublicDirectory); // make it, because Git doesn't track empty dirs
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
          "bigParentDirectory="
              + bigParentDirectory
              + eol
              + "webInfParentDirectory="
              + webInfParentDirectory);

      // are bufferedImages hardware accelerated?
      String2.log(SgtUtil.isBufferedImageAccelerated());

      // 2011-12-30 convert /datasetInfo/[datasetID]/ to
      //                   /dataset/[last2char]/[datasetID]/
      // to prepare for huge number of datasets
      String oldBaseDir = bigParentDirectory + "datasetInfo/"; // the old name
      if (File2.isDirectory(oldBaseDir)) {
        errorInMethod = "ERROR while converting from oldBaseDir=" + oldBaseDir + ": ";
        try {
          String2.log("[[converting datasetInfo/ to dataset/");
          String oldBaseDirList[] = new File(oldBaseDir).list();
          int oldBaseDirListSize = oldBaseDirList == null ? 0 : oldBaseDirList.length;
          for (int od = 0; od < oldBaseDirListSize; od++) {
            String odName = oldBaseDirList[od];
            if (File2.isFile(oldBaseDir + odName)) {
              // delete obsolete files
              File2.delete(oldBaseDir + odName);
              continue;
            }
            if (!File2.isDirectory(oldBaseDir + odName)) {
              // link??
              continue;
            }
            String fullNdName = EDD.datasetDir(odName);
            File2.makeDirectory(fullNdName);
            String oldFileList[] = new File(oldBaseDir + odName).list();
            int oldFileListSize = oldFileList == null ? 0 : oldFileList.length;
            for (int of = 0; of < oldFileListSize; of++) {
              String ofName = oldFileList[of];
              String fullOfName = oldBaseDir + odName + "/" + ofName;
              if (!ofName.matches(".*[0-9]{7}")) // skip temp files
              File2.copy(fullOfName, fullNdName + ofName); // dir will be created
              File2.delete(fullOfName);
            }
            File2.deleteAllFiles(oldBaseDir + odName); // should be already empty
          }
          File2.deleteAllFiles(oldBaseDir, true, true); // and delete empty subdir
          File2.delete(oldBaseDir); // hopefully empty
          String2.log("]]datasetInfo/ was successfully converted to dataset/");

        } catch (Throwable t) {
          String2.log("WARNING: " + MustBe.throwableToString(t));
        }
      }

      // make some subdirectories of fullCacheDirectory
      // '_' distinguishes from dataset cache dirs
      errorInMethod = "ERROR while creating directories: ";
      fullCptCacheDirectory = fullCacheDirectory + "_cpt/";
      fullPlainFileNcCacheDirectory = fullCacheDirectory + "_plainFileNc/";
      fullSgtMapTopographyCacheDirectory = fullCacheDirectory + "_SgtMapTopography/";
      fullTestCacheDirectory = fullCacheDirectory + "_test/";
      fullWmsCacheDirectory =
          fullCacheDirectory + "_wms/"; // for all-datasets WMS and subdirs for non-data layers
      SgtGraph.fullTestCacheDir = fullTestCacheDirectory;
      File2.makeDirectory(fullCptCacheDirectory);
      File2.makeDirectory(fullPlainFileNcCacheDirectory);
      File2.makeDirectory(fullSgtMapTopographyCacheDirectory);
      File2.makeDirectory(fullTestCacheDirectory);
      File2.makeDirectory(fullWmsCacheDirectory);
      File2.makeDirectory(fullWmsCacheDirectory + "Land"); // includes LandMask
      File2.makeDirectory(fullWmsCacheDirectory + "Coastlines");
      File2.makeDirectory(fullWmsCacheDirectory + "LakesAndRivers");
      File2.makeDirectory(fullWmsCacheDirectory + "Nations");
      File2.makeDirectory(fullWmsCacheDirectory + "States");

      // get other info from setup.xml
      errorInMethod = "ERROR while reading " + setupFileName + ": ";
      baseUrl = getSetupEVNotNothingString(setup, ev, "baseUrl", errorInMethod);
      baseHttpsUrl =
          getSetupEVString(
              setup, ev, "baseHttpsUrl", "(not specified)"); // not "" (to avoid relative urls)
      categoryAttributes =
          String2.split(getSetupEVNotNothingString(setup, ev, "categoryAttributes", ""), ',');
      int nCat = categoryAttributes.length;
      categoryAttributesInURLs = new String[nCat];
      categoryIsGlobal = new boolean[nCat]; // initially all false
      for (int cati = 0; cati < nCat; cati++) {
        String cat = categoryAttributes[cati];
        if (cat.startsWith("global:")) {
          categoryIsGlobal[cati] = true;
          cat = cat.substring(7);
          categoryAttributes[cati] = cat;
        } else if (cat.equals("institution")) { // legacy special case
          categoryIsGlobal[cati] = true;
        }
        categoryAttributesInURLs[cati] = String2.modifyToBeFileNameSafe(cat);
      }
      variableNameCategoryAttributeIndex = String2.indexOf(categoryAttributes, "variableName");

      String wmsActiveString = getSetupEVString(setup, ev, "wmsActive", "");
      wmsActive = !String2.isSomething(wmsActiveString) || String2.parseBoolean(wmsActiveString);
      wmsSampleDatasetID = getSetupEVString(setup, ev, "wmsSampleDatasetID", wmsSampleDatasetID);
      wmsSampleVariable = getSetupEVString(setup, ev, "wmsSampleVariable", wmsSampleVariable);
      wmsSampleBBox110 = getSetupEVString(setup, ev, "wmsSampleBBox110", wmsSampleBBox110);
      wmsSampleBBox130 = getSetupEVString(setup, ev, "wmsSampleBBox130", wmsSampleBBox130);
      wmsSampleTime = getSetupEVString(setup, ev, "wmsSampleTime", wmsSampleTime);

      adminInstitution = getSetupEVNotNothingString(setup, ev, "adminInstitution", errorInMethod);
      adminInstitutionUrl =
          getSetupEVNotNothingString(setup, ev, "adminInstitutionUrl", errorInMethod);
      adminIndividualName =
          getSetupEVNotNothingString(setup, ev, "adminIndividualName", errorInMethod);
      adminPosition = getSetupEVNotNothingString(setup, ev, "adminPosition", errorInMethod);
      adminPhone = getSetupEVNotNothingString(setup, ev, "adminPhone", errorInMethod);
      adminAddress = getSetupEVNotNothingString(setup, ev, "adminAddress", errorInMethod);
      adminCity = getSetupEVNotNothingString(setup, ev, "adminCity", errorInMethod);
      adminStateOrProvince =
          getSetupEVNotNothingString(setup, ev, "adminStateOrProvince", errorInMethod);
      adminPostalCode = getSetupEVNotNothingString(setup, ev, "adminPostalCode", errorInMethod);
      adminCountry = getSetupEVNotNothingString(setup, ev, "adminCountry", errorInMethod);
      adminEmail = getSetupEVNotNothingString(setup, ev, "adminEmail", errorInMethod);

      if (adminInstitution.startsWith("Your"))
        throw new RuntimeException(
            "setup.xml error: invalid <adminInstitution>=" + adminInstitution);
      if (!adminInstitutionUrl.startsWith("http") || !String2.isUrl(adminInstitutionUrl))
        throw new RuntimeException(
            "setup.xml error: invalid <adminInstitutionUrl>=" + adminInstitutionUrl);
      if (adminIndividualName.startsWith("Your"))
        throw new RuntimeException(
            "setup.xml error: invalid <adminIndividualName>=" + adminIndividualName);
      // if (adminPosition.length() == 0)
      //    throw new RuntimeException("setup.xml error: invalid <adminPosition>=" + adminPosition);
      if (adminPhone.indexOf("999-999") >= 0)
        throw new RuntimeException("setup.xml error: invalid <adminPhone>=" + adminPhone);
      if (adminAddress.equals("123 Main St."))
        throw new RuntimeException("setup.xml error: invalid <adminAddress>=" + adminAddress);
      if (adminCity.equals("Some Town"))
        throw new RuntimeException("setup.xml error: invalid <adminCity>=" + adminCity);
      // if (adminStateOrProvince.length() == 0)
      //    throw new RuntimeException("setup.xml error: invalid <adminStateOrProvince>=" +
      // adminStateOrProvince);
      if (adminPostalCode.equals("99999"))
        throw new RuntimeException("setup.xml error: invalid <adminPostalCode>=" + adminPostalCode);
      // if (adminCountry.length() == 0)
      //    throw new RuntimeException("setup.xml error: invalid <adminCountry>=" + adminCountry);
      if (!String2.isEmailAddress(adminEmail)
          || adminEmail.startsWith("your.")) // prohibit default adminEmail
      throw new RuntimeException("setup.xml error: invalid <adminEmail>=" + adminEmail);

      accessConstraints = getSetupEVNotNothingString(setup, ev, "accessConstraints", errorInMethod);
      accessRequiresAuthorization =
          getSetupEVNotNothingString(setup, ev, "accessRequiresAuthorization", errorInMethod);
      fees = getSetupEVNotNothingString(setup, ev, "fees", errorInMethod);
      keywords = getSetupEVNotNothingString(setup, ev, "keywords", errorInMethod);

      awsS3OutputBucketUrl = getSetupEVString(setup, ev, "awsS3OutputBucketUrl", (String) null);
      if (!String2.isSomething(awsS3OutputBucketUrl)) awsS3OutputBucketUrl = null;
      if (awsS3OutputBucketUrl != null) {

        // ensure that it is valid
        awsS3OutputBucketUrl = File2.addSlash(awsS3OutputBucketUrl);
        String bro[] = String2.parseAwsS3Url(awsS3OutputBucketUrl);
        if (bro == null)
          throw new RuntimeException(
              "The value of <awsS3OutputBucketUrl> specified in setup.xml doesn't match this regular expression: "
                  + String2.AWS_S3_REGEX());

        awsS3OutputBucket = bro[0];
        String region = bro[1];

        // build the awsS3OutputTransferManager
        awsS3OutputTransferManager = SSR.buildS3TransferManager(region);

        // note that I could set LifecycleRule(s) for the bucket via
        // awsS3OutputClient.putBucketLifecycleConfiguration
        // but LifecycleRule precision seems to be days, not e.g., minutes
        // So make my own system
      }

      units_standard = getSetupEVString(setup, ev, "units_standard", "UDUNITS");

      fgdcActive = getSetupEVBoolean(setup, ev, "fgdcActive", true);
      iso19115Active = getSetupEVBoolean(setup, ev, "iso19115Active", true);
      jsonldActive = getSetupEVBoolean(setup, ev, "jsonldActive", true);
      // until geoServicesRest is finished, it is always inactive
      geoServicesRestActive =
          false; // getSetupEVBoolean(setup, ev,          "geoServicesRestActive",      false);
      filesActive = getSetupEVBoolean(setup, ev, "filesActive", true);
      defaultAccessibleViaFiles =
          getSetupEVBoolean(
              setup, ev, "defaultAccessibleViaFiles", false); // false matches historical behavior
      dataProviderFormActive = getSetupEVBoolean(setup, ev, "dataProviderFormActive", true);
      outOfDateDatasetsActive = getSetupEVBoolean(setup, ev, "outOfDateDatasetsActive", true);
      politicalBoundariesActive = getSetupEVBoolean(setup, ev, "politicalBoundariesActive", true);
      wmsClientActive = getSetupEVBoolean(setup, ev, "wmsClientActive", true);
      SgtMap.drawPoliticalBoundaries = politicalBoundariesActive;

      // until SOS is finished, it is always inactive
      sosActive = false; //        sosActive                  = getSetupEVBoolean(setup, ev,
      // "sosActive",                  false);
      if (sosActive) {
        sosFeatureOfInterest =
            getSetupEVNotNothingString(setup, ev, "sosFeatureOfInterest", errorInMethod);
        sosStandardNamePrefix =
            getSetupEVNotNothingString(setup, ev, "sosStandardNamePrefix", errorInMethod);
        sosUrnBase = getSetupEVNotNothingString(setup, ev, "sosUrnBase", errorInMethod);

        // make the sosGmlName, e.g., https://coastwatch.pfeg.noaa.gov -> gov.noaa.pfeg.coastwatch
        sosBaseGmlName = baseUrl;
        int po = sosBaseGmlName.indexOf("//");
        if (po > 0) sosBaseGmlName = sosBaseGmlName.substring(po + 2);
        po = sosBaseGmlName.indexOf(":");
        if (po > 0) sosBaseGmlName = sosBaseGmlName.substring(0, po);
        StringArray sbgn = new StringArray(String2.split(sosBaseGmlName, '.'));
        sbgn.reverse();
        sosBaseGmlName = String2.toSVString(sbgn.toArray(), ".", false);
      }

      // until it is finished, it is always inactive
      wcsActive =
          false; // getSetupEVBoolean(setup, ev,          "wcsActive",                  false);

      authentication = getSetupEVString(setup, ev, "authentication", "");
      datasetsRegex = getSetupEVString(setup, ev, "datasetsRegex", ".*");
      drawLandMask = getSetupEVString(setup, ev, "drawLandMask", (String) null); // new name
      if (drawLandMask == null) // 2014-08-28 changed defaults below to "under". It will be in v1.48
      drawLandMask =
            getSetupEVString(
                setup, ev, "drawLand", DEFAULT_drawLandMask); // old name. DEFAULT...="under"
      int tdlm = SgtMap.drawLandMask_OPTIONS.indexOf(drawLandMask);
      if (tdlm < 1) drawLandMask = DEFAULT_drawLandMask; // "under"
      flagKeyKey = getSetupEVNotNothingString(setup, ev, "flagKeyKey", errorInMethod);
      if (flagKeyKey.toUpperCase().indexOf("CHANGE THIS") >= 0)
        // really old default: "A stitch in time saves nine. CHANGE THIS!!!"
        // current default:    "CHANGE THIS TO YOUR FAVORITE QUOTE"
        throw new RuntimeException(
            String2.ERROR
                + ": You must change the <flagKeyKey> in setup.xml to a new, unique, non-default value. "
                + "NOTE that this will cause the flagKeys used by your datasets to change. "
                + "Any subscriptions using the old flagKeys will need to be redone.");
      fontFamily = getSetupEVString(setup, ev, "fontFamily", "DejaVu Sans");
      graphBackgroundColor =
          new Color(
              String2.parseInt(
                  getSetupEVString(
                      setup, ev, "graphBackgroundColor", "" + DEFAULT_graphBackgroundColorInt)),
              true); // hasAlpha
      googleClientID = getSetupEVString(setup, ev, "googleClientID", (String) null);
      orcidClientID = getSetupEVString(setup, ev, "orcidClientID", (String) null);
      orcidClientSecret = getSetupEVString(setup, ev, "orcidClientSecret", (String) null);
      googleEarthLogoFile =
          getSetupEVNotNothingString(setup, ev, "googleEarthLogoFile", errorInMethod);
      highResLogoImageFile =
          getSetupEVNotNothingString(setup, ev, "highResLogoImageFile", errorInMethod);
      listPrivateDatasets = getSetupEVBoolean(setup, ev, "listPrivateDatasets", false);
      logMaxSizeMB =
          Math2.minMax(1, 2000, getSetupEVInt(setup, ev, "logMaxSizeMB", 20)); // 2048MB=2GB

      // v2.00: these are now also in datasets.xml
      cacheMillis = getSetupEVInt(setup, ev, "cacheMinutes", DEFAULT_cacheMinutes) * 60000L;
      loadDatasetsMinMillis =
          Math.max(
                  1,
                  getSetupEVInt(
                      setup, ev, "loadDatasetsMinMinutes", DEFAULT_loadDatasetsMinMinutes))
              * 60000L;
      loadDatasetsMaxMillis =
          getSetupEVInt(setup, ev, "loadDatasetsMaxMinutes", DEFAULT_loadDatasetsMaxMinutes)
              * 60000L;
      loadDatasetsMaxMillis = Math.max(loadDatasetsMinMillis * 2, loadDatasetsMaxMillis);
      partialRequestMaxBytes =
          getSetupEVInt(setup, ev, "partialRequestMaxBytes", DEFAULT_partialRequestMaxBytes);
      partialRequestMaxCells =
          getSetupEVInt(setup, ev, "partialRequestMaxCells", DEFAULT_partialRequestMaxCells);
      unusualActivity = getSetupEVInt(setup, ev, "unusualActivity", DEFAULT_unusualActivity);
      showLoadErrorsOnStatusPage =
          getSetupEVBoolean(
              setup, ev, "showLoadErrorsOnStatusPage", DEFAULT_showLoadErrorsOnStatusPage);

      lowResLogoImageFile =
          getSetupEVNotNothingString(setup, ev, "lowResLogoImageFile", errorInMethod);
      quickRestart = getSetupEVBoolean(setup, ev, "quickRestart", true);
      passwordEncoding = getSetupEVString(setup, ev, "passwordEncoding", "UEPSHA256");
      searchEngine = getSetupEVString(setup, ev, "searchEngine", "original");

      subscribeToRemoteErddapDataset =
          getSetupEVBoolean(setup, ev, "subscribeToRemoteErddapDataset", true);
      subscriptionSystemActive = getSetupEVBoolean(setup, ev, "subscriptionSystemActive", true);
      convertersActive = getSetupEVBoolean(setup, ev, "convertersActive", true);
      useSaxParser = getSetupEVBoolean(setup, ev, "useSaxParser", false);
      useEddReflection = getSetupEVBoolean(setup, ev, "useEddReflection", false);
      slideSorterActive = getSetupEVBoolean(setup, ev, "slideSorterActive", true);
      variablesMustHaveIoosCategory =
          getSetupEVBoolean(setup, ev, "variablesMustHaveIoosCategory", true);
      warName = getSetupEVString(setup, ev, "warName", "erddap");
      useSharedWatchService = getSetupEVBoolean(setup, ev, "useSharedWatchService", true);

      // use Lucence?
      if (searchEngine.equals("lucene")) {
        useLuceneSearchEngine = true;
        luceneDocNToDatasetID = new ConcurrentHashMap<>();
      } else {
        Test.ensureEqual(
            searchEngine,
            "original",
            "<searchEngine> must be \"original\" (the default) or \"lucene\".");
      }

      errorInMethod = "ERROR while initializing SgtGraph: ";
      sgtGraph = new SgtGraph(fontFamily);

      // ensure erddapVersion is okay
      int upo = erddapVersion.indexOf('_');
      double eVer = String2.parseDouble(upo >= 0 ? erddapVersion.substring(0, upo) : erddapVersion);
      if (upo == -1 && erddapVersion.length() == 4 && eVer > 1.8 && eVer < 10) {
      } // it's just a number
      else if ((upo != -1 && upo != 4)
          || eVer <= 1.8
          || eVer >= 10
          || Double.isNaN(eVer)
          || erddapVersion.indexOf(' ') >= 0
          || !String2.isAsciiPrintable(erddapVersion))
        throw new SimpleException(
            "The format of EDStatic.erddapVersion must be d.dd[_someAsciiText]. (eVer="
                + eVer
                + ")");

      // ensure authentication setup is okay
      errorInMethod = "ERROR while checking authentication setup: ";
      if (authentication == null) authentication = "";
      authentication = authentication.trim().toLowerCase();
      if (!authentication.isEmpty()
          && !authentication.equals("custom")
          && !authentication.equals("email")
          && !authentication.equals("google")
          && !authentication.equals("orcid")
          && !authentication.equals("oauth2"))
        throw new RuntimeException(
            "setup.xml error: authentication="
                + authentication
                + " must be (nothing)|custom|email|google|orcid|oauth2.");
      if (!authentication.isEmpty() && !baseHttpsUrl.startsWith("https://"))
        throw new RuntimeException(
            "setup.xml error: "
                + ": For any <authentication> other than \"\", the baseHttpsUrl="
                + baseHttpsUrl
                + " must start with \"https://\".");
      if ((authentication.equals("google") || authentication.equals("auth2"))
          && !String2.isSomething(googleClientID))
        throw new RuntimeException(
            "setup.xml error: "
                + ": When authentication=google or oauth2, you must provide your <googleClientID>.");
      if ((authentication.equals("orcid") || authentication.equals("auth2"))
          && (!String2.isSomething(orcidClientID) || !String2.isSomething(orcidClientSecret)))
        throw new RuntimeException(
            "setup.xml error: "
                + ": When authentication=orcid or oauth2, you must provide your <orcidClientID> and <orcidClientSecret>.");
      if (authentication.equals("custom")
          && (!passwordEncoding.equals("MD5")
              && !passwordEncoding.equals("UEPMD5")
              && !passwordEncoding.equals("SHA256")
              && !passwordEncoding.equals("UEPSHA256")))
        throw new RuntimeException(
            "setup.xml error: When authentication=custom, passwordEncoding="
                + passwordEncoding
                + " must be MD5|UEPMD5|SHA256|UEPSHA256.");
      // String2.log("authentication=" + authentication);

      // things set as a consequence of setup.xml
      erddapUrl = baseUrl + "/" + warName;
      erddapHttpsUrl = baseHttpsUrl + "/" + warName;
      preferredErddapUrl = baseHttpsUrl.startsWith("https://") ? erddapHttpsUrl : erddapUrl;

      if (subscriptionSystemActive) {
        subscriptions =
            new Subscriptions(
                bigParentDirectory + "subscriptionsV1.txt",
                48, // maxHoursPending,
                preferredErddapUrl); // prefer https url
      }

      // ???if logoImgTag is needed, convert to method logoImgTag(loggedInAs)
      // logoImgTag = "      <img src=\"" + imageDirUrl(loggedInAs, language) + lowResLogoImageFile
      // + "\" " +
      //    "alt=\"logo\" title=\"logo\">\n";

      // copy all <contentDirectory>images/ (and subdirectories) files to imageDir (and
      // subdirectories)
      String tFiles[] =
          RegexFilenameFilter.recursiveFullNameList(contentDirectory + "images/", ".+", false);
      for (String file : tFiles) {
        int tpo = file.indexOf("/images/");
        if (tpo < 0) tpo = file.indexOf("\\images\\");
        if (tpo < 0) {
          String2.log("'/images/' not found in images/ file: " + file);
          continue;
        }
        String tName = file.substring(tpo + 8);
        if (verbose) String2.log("  copying images/ file: " + tName);
        File2.copy(contentDirectory + "images/" + tName, imageDir + tName);
      }

      // ensure images exist and get their sizes
      Image tImage = Image2.getImage(imageDir + lowResLogoImageFile, 10000, false);
      lowResLogoImageFileWidth = tImage.getWidth(null);
      lowResLogoImageFileHeight = tImage.getHeight(null);
      tImage = Image2.getImage(imageDir + highResLogoImageFile, 10000, false);
      highResLogoImageFileWidth = tImage.getWidth(null);
      highResLogoImageFileHeight = tImage.getHeight(null);
      tImage = Image2.getImage(imageDir + googleEarthLogoFile, 10000, false);
      googleEarthLogoFileWidth = tImage.getWidth(null);
      googleEarthLogoFileHeight = tImage.getHeight(null);

      // copy all <contentDirectory>cptfiles/ files to cptfiles
      tFiles =
          RegexFilenameFilter.list(contentDirectory + "cptfiles/", ".+\\.cpt"); // not recursive
      for (String tFile : tFiles) {
        if (verbose) String2.log("  copying cptfiles/ file: " + tFile);
        File2.copy(contentDirectory + "cptfiles/" + tFile, fullPaletteDirectory + tFile);
      }

      // **** messages.xml *************************************************************
      // This is read AFTER setup.xml. If that is a problem for something, defer reading it in setup
      // and add it below.
      // Read static messages from messages(2).xml in contentDirectory.
      errorInMethod = "ERROR while reading messages.xml: ";
      ResourceBundle2[] messagesAr = new ResourceBundle2[nLanguages];
      String messagesFileName = contentDirectory + "messages.xml";
      if (File2.isFile(messagesFileName)) {
        String2.log("Using custom messages.xml from " + messagesFileName);
        // messagesAr[0] is either the custom messages.xml or the one provided by Erddap
        messagesAr[0] = ResourceBundle2.fromXml(XML.parseXml(messagesFileName, false));
      } else {
        // use default messages.xml
        String2.log("Custom messages.xml not found at " + messagesFileName);
        // use String2.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
        URL messagesResourceFile = Resources.getResource("gov/noaa/pfel/erddap/util/messages.xml");
        // messagesAr[0] is either the custom messages.xml or the one provided by Erddap
        messagesAr[0] = ResourceBundle2.fromXml(XML.parseXml(messagesResourceFile, false));
        String2.log("Using default messages.xml from  " + messagesFileName);
      }

      for (int tl = 1; tl < nLanguages; tl++) {
        String tName = "messages-" + TranslateMessages.languageCodeList.get(tl) + ".xml";
        errorInMethod = "ERROR while reading " + tName + ": ";
        URL messageFile = new URL(TranslateMessages.translatedMessagesDir + tName);
        messagesAr[tl] = ResourceBundle2.fromXml(XML.parseXml(messageFile, false));
      }

      // read all the static Strings from messages.xml
      errorInMethod = "ERROR while reading from all the messages.xml files: ";
      acceptEncodingHtmlAr = getNotNothingString(messagesAr, "acceptEncodingHtml", errorInMethod);
      accessRESTFULAr = getNotNothingString(messagesAr, "accessRestful", errorInMethod);
      acronymsAr = getNotNothingString(messagesAr, "acronyms", errorInMethod);
      addConstraintsAr = getNotNothingString(messagesAr, "addConstraints", errorInMethod);
      addVarWhereAttNameAr = getNotNothingString(messagesAr, "addVarWhereAttName", errorInMethod);
      addVarWhereAttValueAr = getNotNothingString(messagesAr, "addVarWhereAttValue", errorInMethod);
      addVarWhereAr = getNotNothingString(messagesAr, "addVarWhere", errorInMethod);
      additionalLinksAr = getNotNothingString(messagesAr, "additionalLinks", errorInMethod);
      admKeywords = messagesAr[0].getNotNothingString("admKeywords", errorInMethod);
      admSubsetVariables = messagesAr[0].getNotNothingString("admSubsetVariables", errorInMethod);
      admSummaryAr = getNotNothingString(messagesAr, "admSummary", errorInMethod);
      admTitleAr = getNotNothingString(messagesAr, "admTitle", errorInMethod);
      advl_datasetID = messagesAr[0].getNotNothingString("advl_datasetID", errorInMethod);
      advc_accessibleAr = getNotNothingString(messagesAr, "advc_accessible", errorInMethod);
      advl_accessibleAr = getNotNothingString(messagesAr, "advl_accessible", errorInMethod);
      advl_institutionAr = getNotNothingString(messagesAr, "advl_institution", errorInMethod);
      advc_dataStructureAr = getNotNothingString(messagesAr, "advc_dataStructure", errorInMethod);
      advl_dataStructureAr = getNotNothingString(messagesAr, "advl_dataStructure", errorInMethod);
      advr_dataStructure = messagesAr[0].getNotNothingString("advr_dataStructure", errorInMethod);
      advl_cdm_data_typeAr = getNotNothingString(messagesAr, "advl_cdm_data_type", errorInMethod);
      advr_cdm_data_type = messagesAr[0].getNotNothingString("advr_cdm_data_type", errorInMethod);
      advl_classAr = getNotNothingString(messagesAr, "advl_class", errorInMethod);
      advr_class = messagesAr[0].getNotNothingString("advr_class", errorInMethod);
      advl_titleAr = getNotNothingString(messagesAr, "advl_title", errorInMethod);
      advl_minLongitudeAr = getNotNothingString(messagesAr, "advl_minLongitude", errorInMethod);
      advl_maxLongitudeAr = getNotNothingString(messagesAr, "advl_maxLongitude", errorInMethod);
      advl_longitudeSpacingAr =
          getNotNothingString(messagesAr, "advl_longitudeSpacing", errorInMethod);
      advl_minLatitudeAr = getNotNothingString(messagesAr, "advl_minLatitude", errorInMethod);
      advl_maxLatitudeAr = getNotNothingString(messagesAr, "advl_maxLatitude", errorInMethod);
      advl_latitudeSpacingAr =
          getNotNothingString(messagesAr, "advl_latitudeSpacing", errorInMethod);
      advl_minAltitudeAr = getNotNothingString(messagesAr, "advl_minAltitude", errorInMethod);
      advl_maxAltitudeAr = getNotNothingString(messagesAr, "advl_maxAltitude", errorInMethod);
      advl_minTimeAr = getNotNothingString(messagesAr, "advl_minTime", errorInMethod);
      advc_maxTimeAr = getNotNothingString(messagesAr, "advc_maxTime", errorInMethod);
      advl_maxTimeAr = getNotNothingString(messagesAr, "advl_maxTime", errorInMethod);
      advl_timeSpacingAr = getNotNothingString(messagesAr, "advl_timeSpacing", errorInMethod);
      advc_griddapAr = getNotNothingString(messagesAr, "advc_griddap", errorInMethod);
      advl_griddapAr = getNotNothingString(messagesAr, "advl_griddap", errorInMethod);
      advl_subsetAr = getNotNothingString(messagesAr, "advl_subset", errorInMethod);
      advc_tabledapAr = getNotNothingString(messagesAr, "advc_tabledap", errorInMethod);
      advl_tabledapAr = getNotNothingString(messagesAr, "advl_tabledap", errorInMethod);
      advl_MakeAGraphAr = getNotNothingString(messagesAr, "advl_MakeAGraph", errorInMethod);
      advc_sosAr = getNotNothingString(messagesAr, "advc_sos", errorInMethod);
      advl_sosAr = getNotNothingString(messagesAr, "advl_sos", errorInMethod);
      advl_wcsAr = getNotNothingString(messagesAr, "advl_wcs", errorInMethod);
      advl_wmsAr = getNotNothingString(messagesAr, "advl_wms", errorInMethod);
      advc_filesAr = getNotNothingString(messagesAr, "advc_files", errorInMethod);
      advl_filesAr = getNotNothingString(messagesAr, "advl_files", errorInMethod);
      advc_fgdcAr = getNotNothingString(messagesAr, "advc_fgdc", errorInMethod);
      advl_fgdcAr = getNotNothingString(messagesAr, "advl_fgdc", errorInMethod);
      advc_iso19115Ar = getNotNothingString(messagesAr, "advc_iso19115", errorInMethod);
      advl_iso19115Ar = getNotNothingString(messagesAr, "advl_iso19115", errorInMethod);
      advc_metadataAr = getNotNothingString(messagesAr, "advc_metadata", errorInMethod);
      advl_metadataAr = getNotNothingString(messagesAr, "advl_metadata", errorInMethod);
      advl_sourceUrlAr = getNotNothingString(messagesAr, "advl_sourceUrl", errorInMethod);
      advl_infoUrlAr = getNotNothingString(messagesAr, "advl_infoUrl", errorInMethod);
      advl_rssAr = getNotNothingString(messagesAr, "advl_rss", errorInMethod);
      advc_emailAr = getNotNothingString(messagesAr, "advc_email", errorInMethod);
      advl_emailAr = getNotNothingString(messagesAr, "advl_email", errorInMethod);
      advl_summaryAr = getNotNothingString(messagesAr, "advl_summary", errorInMethod);
      advc_testOutOfDateAr = getNotNothingString(messagesAr, "advc_testOutOfDate", errorInMethod);
      advl_testOutOfDateAr = getNotNothingString(messagesAr, "advl_testOutOfDate", errorInMethod);
      advc_outOfDateAr = getNotNothingString(messagesAr, "advc_outOfDate", errorInMethod);
      advl_outOfDateAr = getNotNothingString(messagesAr, "advl_outOfDate", errorInMethod);
      advn_outOfDateAr = getNotNothingString(messagesAr, "advn_outOfDate", errorInMethod);
      advancedSearchAr = getNotNothingString(messagesAr, "advancedSearch", errorInMethod);
      advancedSearchResultsAr =
          getNotNothingString(messagesAr, "advancedSearchResults", errorInMethod);
      advancedSearchDirectionsAr =
          getNotNothingString(messagesAr, "advancedSearchDirections", errorInMethod);
      advancedSearchTooltipAr =
          getNotNothingString(messagesAr, "advancedSearchTooltip", errorInMethod);
      advancedSearchBoundsAr =
          getNotNothingString(messagesAr, "advancedSearchBounds", errorInMethod);
      advancedSearchMinLatAr =
          getNotNothingString(messagesAr, "advancedSearchMinLat", errorInMethod);
      advancedSearchMaxLatAr =
          getNotNothingString(messagesAr, "advancedSearchMaxLat", errorInMethod);
      advancedSearchMinLonAr =
          getNotNothingString(messagesAr, "advancedSearchMinLon", errorInMethod);
      advancedSearchMaxLonAr =
          getNotNothingString(messagesAr, "advancedSearchMaxLon", errorInMethod);
      advancedSearchMinMaxLonAr =
          getNotNothingString(messagesAr, "advancedSearchMinMaxLon", errorInMethod);
      advancedSearchMinTimeAr =
          getNotNothingString(messagesAr, "advancedSearchMinTime", errorInMethod);
      advancedSearchMaxTimeAr =
          getNotNothingString(messagesAr, "advancedSearchMaxTime", errorInMethod);
      advancedSearchClearAr = getNotNothingString(messagesAr, "advancedSearchClear", errorInMethod);
      advancedSearchClearHelpAr =
          getNotNothingString(messagesAr, "advancedSearchClearHelp", errorInMethod);
      advancedSearchCategoryTooltipAr =
          getNotNothingString(messagesAr, "advancedSearchCategoryTooltip", errorInMethod);
      advancedSearchRangeTooltipAr =
          getNotNothingString(messagesAr, "advancedSearchRangeTooltip", errorInMethod);
      advancedSearchMapTooltipAr =
          getNotNothingString(messagesAr, "advancedSearchMapTooltip", errorInMethod);
      advancedSearchLonTooltipAr =
          getNotNothingString(messagesAr, "advancedSearchLonTooltip", errorInMethod);
      advancedSearchTimeTooltipAr =
          getNotNothingString(messagesAr, "advancedSearchTimeTooltip", errorInMethod);
      advancedSearchWithCriteriaAr =
          getNotNothingString(messagesAr, "advancedSearchWithCriteria", errorInMethod);
      advancedSearchFewerCriteriaAr =
          getNotNothingString(messagesAr, "advancedSearchFewerCriteria", errorInMethod);
      advancedSearchNoCriteriaAr =
          getNotNothingString(messagesAr, "advancedSearchNoCriteria", errorInMethod);
      advancedSearchErrorHandlingAr =
          getNotNothingString(messagesAr, "advancedSearchErrorHandling", errorInMethod);
      PrimitiveArray.ArrayAddN = messagesAr[0].getNotNothingString("ArrayAddN", errorInMethod);
      PrimitiveArray.ArrayAppendTables =
          messagesAr[0].getNotNothingString("ArrayAppendTables", errorInMethod);
      PrimitiveArray.ArrayAtInsert =
          messagesAr[0].getNotNothingString("ArrayAtInsert", errorInMethod);
      PrimitiveArray.ArrayDiff = messagesAr[0].getNotNothingString("ArrayDiff", errorInMethod);
      PrimitiveArray.ArrayDifferentSize =
          messagesAr[0].getNotNothingString("ArrayDifferentSize", errorInMethod);
      PrimitiveArray.ArrayDifferentValue =
          messagesAr[0].getNotNothingString("ArrayDifferentValue", errorInMethod);
      PrimitiveArray.ArrayDiffString =
          messagesAr[0].getNotNothingString("ArrayDiffString", errorInMethod);
      PrimitiveArray.ArrayMissingValue =
          messagesAr[0].getNotNothingString("ArrayMissingValue", errorInMethod);
      PrimitiveArray.ArrayNotAscending =
          messagesAr[0].getNotNothingString("ArrayNotAscending", errorInMethod);
      PrimitiveArray.ArrayNotDescending =
          messagesAr[0].getNotNothingString("ArrayNotDescending", errorInMethod);
      PrimitiveArray.ArrayNotEvenlySpaced =
          messagesAr[0].getNotNothingString("ArrayNotEvenlySpaced", errorInMethod);
      PrimitiveArray.ArrayRemove = messagesAr[0].getNotNothingString("ArrayRemove", errorInMethod);
      PrimitiveArray.ArraySubsetStart =
          messagesAr[0].getNotNothingString("ArraySubsetStart", errorInMethod);
      PrimitiveArray.ArraySubsetStride =
          messagesAr[0].getNotNothingString("ArraySubsetStride", errorInMethod);
      autoRefreshAr = getNotNothingString(messagesAr, "autoRefresh", errorInMethod);
      blacklistMsgAr = getNotNothingString(messagesAr, "blacklistMsg", errorInMethod);
      BroughtToYouByAr = getNotNothingString(messagesAr, "BroughtToYouBy", errorInMethod);

      categoryTitleHtmlAr = getNotNothingString(messagesAr, "categoryTitleHtml", errorInMethod);
      categoryHtmlAr = getNotNothingString(messagesAr, "categoryHtml", errorInMethod);
      category3HtmlAr = getNotNothingString(messagesAr, "category3Html", errorInMethod);
      categoryPickAttributeAr =
          getNotNothingString(messagesAr, "categoryPickAttribute", errorInMethod);
      categorySearchHtmlAr = getNotNothingString(messagesAr, "categorySearchHtml", errorInMethod);
      categorySearchDifferentHtmlAr =
          getNotNothingString(messagesAr, "categorySearchDifferentHtml", errorInMethod);
      categoryClickHtmlAr = getNotNothingString(messagesAr, "categoryClickHtml", errorInMethod);
      categoryNotAnOptionAr = getNotNothingString(messagesAr, "categoryNotAnOption", errorInMethod);
      caughtInterruptedAr = getNotNothingString(messagesAr, "caughtInterrupted", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++)
        caughtInterruptedAr[tl] = " " + caughtInterruptedAr[tl];

      cdmDataTypeHelpAr = getNotNothingString(messagesAr, "cdmDataTypeHelp", errorInMethod);

      clickAccessAr = getNotNothingString(messagesAr, "clickAccess", errorInMethod);
      clickBackgroundInfoAr = getNotNothingString(messagesAr, "clickBackgroundInfo", errorInMethod);
      clickERDDAPAr = getNotNothingString(messagesAr, "clickERDDAP", errorInMethod);
      clickInfoAr = getNotNothingString(messagesAr, "clickInfo", errorInMethod);
      clickToSubmitAr = getNotNothingString(messagesAr, "clickToSubmit", errorInMethod);
      HtmlWidgets.comboBoxAltAr = getNotNothingString(messagesAr, "comboBoxAlt", errorInMethod);
      convertAr = getNotNothingString(messagesAr, "convert", errorInMethod);
      convertBypassAr = getNotNothingString(messagesAr, "convertBypass", errorInMethod);

      convertToAFullNameAr = getNotNothingString(messagesAr, "convertToAFullName", errorInMethod);
      convertToAnAcronymAr = getNotNothingString(messagesAr, "convertToAnAcronym", errorInMethod);
      convertToACountyNameAr =
          getNotNothingString(messagesAr, "convertToACountyName", errorInMethod);
      convertToAFIPSCodeAr = getNotNothingString(messagesAr, "convertToAFIPSCode", errorInMethod);
      convertToGCMDAr = getNotNothingString(messagesAr, "convertToGCMD", errorInMethod);
      convertToCFStandardNamesAr =
          getNotNothingString(messagesAr, "convertToCFStandardNames", errorInMethod);
      convertToNumericTimeAr =
          getNotNothingString(messagesAr, "convertToNumericTime", errorInMethod);
      convertToStringTimeAr = getNotNothingString(messagesAr, "convertToStringTime", errorInMethod);
      convertAnyStringTimeAr =
          getNotNothingString(messagesAr, "convertAnyStringTime", errorInMethod);
      convertToProperTimeUnitsAr =
          getNotNothingString(messagesAr, "convertToProperTimeUnits", errorInMethod);
      convertFromUDUNITSToUCUMAr =
          getNotNothingString(messagesAr, "convertFromUDUNITSToUCUM", errorInMethod);
      convertFromUCUMToUDUNITSAr =
          getNotNothingString(messagesAr, "convertFromUCUMToUDUNITS", errorInMethod);
      convertToUCUMAr = getNotNothingString(messagesAr, "convertToUCUM", errorInMethod);
      convertToUDUNITSAr = getNotNothingString(messagesAr, "convertToUDUNITS", errorInMethod);
      convertStandardizeUDUNITSAr =
          getNotNothingString(messagesAr, "convertStandardizeUDUNITS", errorInMethod);
      convertToFullNameAr = getNotNothingString(messagesAr, "convertToFullName", errorInMethod);
      convertToVariableNameAr =
          getNotNothingString(messagesAr, "convertToVariableName", errorInMethod);

      converterWebServiceAr = getNotNothingString(messagesAr, "converterWebService", errorInMethod);
      convertOAAcronymsAr = getNotNothingString(messagesAr, "convertOAAcronyms", errorInMethod);
      convertOAAcronymsToFromAr =
          getNotNothingString(messagesAr, "convertOAAcronymsToFrom", errorInMethod);
      convertOAAcronymsIntroAr =
          getNotNothingString(messagesAr, "convertOAAcronymsIntro", errorInMethod);
      convertOAAcronymsNotesAr =
          getNotNothingString(messagesAr, "convertOAAcronymsNotes", errorInMethod);
      convertOAAcronymsServiceAr =
          getNotNothingString(messagesAr, "convertOAAcronymsService", errorInMethod);
      convertOAVariableNamesAr =
          getNotNothingString(messagesAr, "convertOAVariableNames", errorInMethod);
      convertOAVariableNamesToFromAr =
          getNotNothingString(messagesAr, "convertOAVariableNamesToFrom", errorInMethod);
      convertOAVariableNamesIntroAr =
          getNotNothingString(messagesAr, "convertOAVariableNamesIntro", errorInMethod);
      convertOAVariableNamesNotesAr =
          getNotNothingString(messagesAr, "convertOAVariableNamesNotes", errorInMethod);
      convertOAVariableNamesServiceAr =
          getNotNothingString(messagesAr, "convertOAVariableNamesService", errorInMethod);
      convertFipsCountyAr = getNotNothingString(messagesAr, "convertFipsCounty", errorInMethod);
      convertFipsCountyIntroAr =
          getNotNothingString(messagesAr, "convertFipsCountyIntro", errorInMethod);
      convertFipsCountyNotesAr =
          getNotNothingString(messagesAr, "convertFipsCountyNotes", errorInMethod);
      convertFipsCountyServiceAr =
          getNotNothingString(messagesAr, "convertFipsCountyService", errorInMethod);
      convertHtmlAr = getNotNothingString(messagesAr, "convertHtml", errorInMethod);
      convertInterpolateAr = getNotNothingString(messagesAr, "convertInterpolate", errorInMethod);
      convertInterpolateIntroAr =
          getNotNothingString(messagesAr, "convertInterpolateIntro", errorInMethod);
      convertInterpolateTLLTableAr =
          getNotNothingString(messagesAr, "convertInterpolateTLLTable", errorInMethod);
      convertInterpolateTLLTableHelpAr =
          getNotNothingString(messagesAr, "convertInterpolateTLLTableHelp", errorInMethod);
      convertInterpolateDatasetIDVariableAr =
          getNotNothingString(messagesAr, "convertInterpolateDatasetIDVariable", errorInMethod);
      convertInterpolateDatasetIDVariableHelpAr =
          getNotNothingString(messagesAr, "convertInterpolateDatasetIDVariableHelp", errorInMethod);
      convertInterpolateNotesAr =
          getNotNothingString(messagesAr, "convertInterpolateNotes", errorInMethod);
      convertInterpolateServiceAr =
          getNotNothingString(messagesAr, "convertInterpolateService", errorInMethod);
      convertKeywordsAr = getNotNothingString(messagesAr, "convertKeywords", errorInMethod);
      convertKeywordsCfTooltipAr =
          getNotNothingString(messagesAr, "convertKeywordsCfTooltip", errorInMethod);
      convertKeywordsGcmdTooltipAr =
          getNotNothingString(messagesAr, "convertKeywordsGcmdTooltip", errorInMethod);
      convertKeywordsIntroAr =
          getNotNothingString(messagesAr, "convertKeywordsIntro", errorInMethod);
      convertKeywordsNotesAr =
          getNotNothingString(messagesAr, "convertKeywordsNotes", errorInMethod);
      convertKeywordsServiceAr =
          getNotNothingString(messagesAr, "convertKeywordsService", errorInMethod);

      convertTimeAr = getNotNothingString(messagesAr, "convertTime", errorInMethod);
      convertTimeReferenceAr =
          getNotNothingString(messagesAr, "convertTimeReference", errorInMethod);
      convertTimeIntroAr = getNotNothingString(messagesAr, "convertTimeIntro", errorInMethod);
      convertTimeNotesAr = getNotNothingString(messagesAr, "convertTimeNotes", errorInMethod);
      convertTimeServiceAr = getNotNothingString(messagesAr, "convertTimeService", errorInMethod);
      convertTimeNumberTooltipAr =
          getNotNothingString(messagesAr, "convertTimeNumberTooltip", errorInMethod);
      convertTimeStringTimeTooltipAr =
          getNotNothingString(messagesAr, "convertTimeStringTimeTooltip", errorInMethod);
      convertTimeUnitsTooltipAr =
          getNotNothingString(messagesAr, "convertTimeUnitsTooltip", errorInMethod);
      convertTimeUnitsHelpAr =
          getNotNothingString(messagesAr, "convertTimeUnitsHelp", errorInMethod);
      convertTimeIsoFormatErrorAr =
          getNotNothingString(messagesAr, "convertTimeIsoFormatError", errorInMethod);
      convertTimeNoSinceErrorAr =
          getNotNothingString(messagesAr, "convertTimeNoSinceError", errorInMethod);
      convertTimeNumberErrorAr =
          getNotNothingString(messagesAr, "convertTimeNumberError", errorInMethod);
      convertTimeNumericTimeErrorAr =
          getNotNothingString(messagesAr, "convertTimeNumericTimeError", errorInMethod);
      convertTimeParametersErrorAr =
          getNotNothingString(messagesAr, "convertTimeParametersError", errorInMethod);
      convertTimeStringFormatErrorAr =
          getNotNothingString(messagesAr, "convertTimeStringFormatError", errorInMethod);
      convertTimeTwoTimeErrorAr =
          getNotNothingString(messagesAr, "convertTimeTwoTimeError", errorInMethod);
      convertTimeUnitsErrorAr =
          getNotNothingString(messagesAr, "convertTimeUnitsError", errorInMethod);
      convertUnitsAr = getNotNothingString(messagesAr, "convertUnits", errorInMethod);
      convertUnitsComparisonAr =
          getNotNothingString(messagesAr, "convertUnitsComparison", errorInMethod);
      convertUnitsFilterAr = getNotNothingString(messagesAr, "convertUnitsFilter", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) {
        convertUnitsComparisonAr[tl] =
            convertUnitsComparisonAr[tl]
                .replaceAll(
                    "&C;",
                    "C") // these handled this way be cause you can't just avoid translating all
                // words with 'C'
                .replaceAll("&g;", "g") // "
                .replaceAll("&F;", "F") // "
                .replaceAll("&NTU;", "NTU")
                .replaceAll("&ntu;", "ntu")
                .replaceAll("&PSU;", "PSU")
                .replaceAll("&psu;", "psu");
      }

      convertUnitsIntroAr = getNotNothingString(messagesAr, "convertUnitsIntro", errorInMethod);
      convertUnitsNotesAr = getNotNothingString(messagesAr, "convertUnitsNotes", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++)
        convertUnitsNotesAr[tl] =
            convertUnitsNotesAr[tl].replace("&unitsStandard;", units_standard);
      convertUnitsServiceAr = getNotNothingString(messagesAr, "convertUnitsService", errorInMethod);
      convertURLsAr = getNotNothingString(messagesAr, "convertURLs", errorInMethod);
      convertURLsIntroAr = getNotNothingString(messagesAr, "convertURLsIntro", errorInMethod);
      convertURLsNotesAr = getNotNothingString(messagesAr, "convertURLsNotes", errorInMethod);
      convertURLsServiceAr = getNotNothingString(messagesAr, "convertURLsService", errorInMethod);
      cookiesHelpAr = getNotNothingString(messagesAr, "cookiesHelp", errorInMethod);
      copyImageToClipboardAr =
          getNotNothingString(messagesAr, "copyImageToClipboard", errorInMethod);
      copyTextToClipboardAr = getNotNothingString(messagesAr, "copyTextToClipboard", errorInMethod);
      copyToClipboardNotAvailableAr =
          getNotNothingString(messagesAr, "copyToClipboardNotAvailable", errorInMethod);

      dafAr = getNotNothingString(messagesAr, "daf", errorInMethod);
      dafGridBypassTooltipAr =
          getNotNothingString(messagesAr, "dafGridBypassTooltip", errorInMethod);
      dafGridTooltipAr = getNotNothingString(messagesAr, "dafGridTooltip", errorInMethod);
      dafTableBypassTooltipAr =
          getNotNothingString(messagesAr, "dafTableBypassTooltip", errorInMethod);
      dafTableTooltipAr = getNotNothingString(messagesAr, "dafTableTooltip", errorInMethod);
      dasTitleAr = getNotNothingString(messagesAr, "dasTitle", errorInMethod);
      dataAccessNotAllowedAr =
          getNotNothingString(messagesAr, "dataAccessNotAllowed", errorInMethod);
      databaseUnableToConnectAr =
          getNotNothingString(messagesAr, "databaseUnableToConnect", errorInMethod);
      dataProviderFormAr = getNotNothingString(messagesAr, "dataProviderForm", errorInMethod);
      dataProviderFormP1Ar = getNotNothingString(messagesAr, "dataProviderFormP1", errorInMethod);
      dataProviderFormP2Ar = getNotNothingString(messagesAr, "dataProviderFormP2", errorInMethod);
      dataProviderFormP3Ar = getNotNothingString(messagesAr, "dataProviderFormP3", errorInMethod);
      dataProviderFormP4Ar = getNotNothingString(messagesAr, "dataProviderFormP4", errorInMethod);
      dataProviderFormDoneAr =
          getNotNothingString(messagesAr, "dataProviderFormDone", errorInMethod);
      dataProviderFormSuccessAr =
          getNotNothingString(messagesAr, "dataProviderFormSuccess", errorInMethod);
      dataProviderFormShortDescriptionAr =
          getNotNothingString(messagesAr, "dataProviderFormShortDescription", errorInMethod);
      dataProviderFormLongDescriptionHTMLAr =
          getNotNothingString(messagesAr, "dataProviderFormLongDescriptionHTML", errorInMethod);
      disabledAr = getNotNothingString(messagesAr, "disabled", errorInMethod);
      dataProviderFormPart1Ar =
          getNotNothingString(messagesAr, "dataProviderFormPart1", errorInMethod);
      dataProviderFormPart2HeaderAr =
          getNotNothingString(messagesAr, "dataProviderFormPart2Header", errorInMethod);
      dataProviderFormPart2GlobalMetadataAr =
          getNotNothingString(messagesAr, "dataProviderFormPart2GlobalMetadata", errorInMethod);
      dataProviderContactInfoAr =
          getNotNothingString(messagesAr, "dataProviderContactInfo", errorInMethod);
      dataProviderDataAr = getNotNothingString(messagesAr, "dataProviderData", errorInMethod);
      documentationAr = getNotNothingString(messagesAr, "documentation", errorInMethod);

      dpf_submitAr = getNotNothingString(messagesAr, "dpf_submit", errorInMethod);
      dpf_fixProblemAr = getNotNothingString(messagesAr, "dpf_fixProblem", errorInMethod);
      dpf_yourNameAr = getNotNothingString(messagesAr, "dpf_yourName", errorInMethod);
      dpf_emailAddressAr = getNotNothingString(messagesAr, "dpf_emailAddress", errorInMethod);
      dpf_TimestampAr = getNotNothingString(messagesAr, "dpf_Timestamp", errorInMethod);
      dpf_frequencyAr = getNotNothingString(messagesAr, "dpf_frequency", errorInMethod);
      dpf_titleAr = getNotNothingString(messagesAr, "dpf_title", errorInMethod);
      dpf_titleTooltipAr = getNotNothingString(messagesAr, "dpf_titleTooltip", errorInMethod);
      dpf_summaryAr = getNotNothingString(messagesAr, "dpf_summary", errorInMethod);
      dpf_summaryTooltipAr = getNotNothingString(messagesAr, "dpf_summaryTooltip", errorInMethod);
      dpf_creatorNameAr = getNotNothingString(messagesAr, "dpf_creatorName", errorInMethod);
      dpf_creatorNameTooltipAr =
          getNotNothingString(messagesAr, "dpf_creatorNameTooltip", errorInMethod);
      dpf_creatorTypeAr = getNotNothingString(messagesAr, "dpf_creatorType", errorInMethod);
      dpf_creatorTypeTooltipAr =
          getNotNothingString(messagesAr, "dpf_creatorTypeTooltip", errorInMethod);
      dpf_creatorEmailAr = getNotNothingString(messagesAr, "dpf_creatorEmail", errorInMethod);
      dpf_creatorEmailTooltipAr =
          getNotNothingString(messagesAr, "dpf_creatorEmailTooltip", errorInMethod);
      dpf_institutionAr = getNotNothingString(messagesAr, "dpf_institution", errorInMethod);
      dpf_institutionTooltipAr =
          getNotNothingString(messagesAr, "dpf_institutionTooltip", errorInMethod);
      dpf_infoUrlAr = getNotNothingString(messagesAr, "dpf_infoUrl", errorInMethod);
      dpf_infoUrlTooltipAr = getNotNothingString(messagesAr, "dpf_infoUrlTooltip", errorInMethod);
      dpf_licenseAr = getNotNothingString(messagesAr, "dpf_license", errorInMethod);
      dpf_licenseTooltipAr = getNotNothingString(messagesAr, "dpf_licenseTooltip", errorInMethod);
      dpf_howYouStoreDataAr = getNotNothingString(messagesAr, "dpf_howYouStoreData", errorInMethod);
      dpf_provideIfAvailableAr =
          getNotNothingString(messagesAr, "dpf_provideIfAvailable", errorInMethod);
      dpf_acknowledgementAr = getNotNothingString(messagesAr, "dpf_acknowledgement", errorInMethod);
      dpf_acknowledgementTooltipAr =
          getNotNothingString(messagesAr, "dpf_acknowledgementTooltip", errorInMethod);
      dpf_historyAr = getNotNothingString(messagesAr, "dpf_history", errorInMethod);
      dpf_historyTooltipAr = getNotNothingString(messagesAr, "dpf_historyTooltip", errorInMethod);
      dpf_idTooltipAr = getNotNothingString(messagesAr, "dpf_idTooltip", errorInMethod);
      dpf_namingAuthorityAr = getNotNothingString(messagesAr, "dpf_namingAuthority", errorInMethod);
      dpf_namingAuthorityTooltipAr =
          getNotNothingString(messagesAr, "dpf_namingAuthorityTooltip", errorInMethod);
      dpf_productVersionAr = getNotNothingString(messagesAr, "dpf_productVersion", errorInMethod);
      dpf_productVersionTooltipAr =
          getNotNothingString(messagesAr, "dpf_productVersionTooltip", errorInMethod);
      dpf_referencesAr = getNotNothingString(messagesAr, "dpf_references", errorInMethod);
      dpf_referencesTooltipAr =
          getNotNothingString(messagesAr, "dpf_referencesTooltip", errorInMethod);
      dpf_commentAr = getNotNothingString(messagesAr, "dpf_comment", errorInMethod);
      dpf_commentTooltipAr = getNotNothingString(messagesAr, "dpf_commentTooltip", errorInMethod);
      dpf_dataTypeHelpAr = getNotNothingString(messagesAr, "dpf_dataTypeHelp", errorInMethod);
      dpf_ioosCategoryAr = getNotNothingString(messagesAr, "dpf_ioosCategory", errorInMethod);
      dpf_ioosCategoryHelpAr =
          getNotNothingString(messagesAr, "dpf_ioosCategoryHelp", errorInMethod);
      dpf_part3HeaderAr = getNotNothingString(messagesAr, "dpf_part3Header", errorInMethod);
      dpf_variableMetadataAr =
          getNotNothingString(messagesAr, "dpf_variableMetadata", errorInMethod);
      dpf_sourceNameAr = getNotNothingString(messagesAr, "dpf_sourceName", errorInMethod);
      dpf_sourceNameTooltipAr =
          getNotNothingString(messagesAr, "dpf_sourceNameTooltip", errorInMethod);
      dpf_destinationNameAr = getNotNothingString(messagesAr, "dpf_destinationName", errorInMethod);
      dpf_destinationNameTooltipAr =
          getNotNothingString(messagesAr, "dpf_destinationNameTooltip", errorInMethod);

      dpf_longNameAr = getNotNothingString(messagesAr, "dpf_longName", errorInMethod);
      dpf_longNameTooltipAr = getNotNothingString(messagesAr, "dpf_longNameTooltip", errorInMethod);
      dpf_standardNameAr = getNotNothingString(messagesAr, "dpf_standardName", errorInMethod);
      dpf_standardNameTooltipAr =
          getNotNothingString(messagesAr, "dpf_standardNameTooltip", errorInMethod);
      dpf_dataTypeAr = getNotNothingString(messagesAr, "dpf_dataType", errorInMethod);
      dpf_fillValueAr = getNotNothingString(messagesAr, "dpf_fillValue", errorInMethod);
      dpf_fillValueTooltipAr =
          getNotNothingString(messagesAr, "dpf_fillValueTooltip", errorInMethod);
      dpf_unitsAr = getNotNothingString(messagesAr, "dpf_units", errorInMethod);
      dpf_unitsTooltipAr = getNotNothingString(messagesAr, "dpf_unitsTooltip", errorInMethod);
      dpf_rangeAr = getNotNothingString(messagesAr, "dpf_range", errorInMethod);
      dpf_rangeTooltipAr = getNotNothingString(messagesAr, "dpf_rangeTooltip", errorInMethod);
      dpf_part4HeaderAr = getNotNothingString(messagesAr, "dpf_part4Header", errorInMethod);
      dpf_otherCommentAr = getNotNothingString(messagesAr, "dpf_otherComment", errorInMethod);
      dpf_finishPart4Ar = getNotNothingString(messagesAr, "dpf_finishPart4", errorInMethod);
      dpf_congratulationAr = getNotNothingString(messagesAr, "dpf_congratulation", errorInMethod);

      distinctValuesTooltipAr =
          getNotNothingString(messagesAr, "distinctValuesTooltip", errorInMethod);
      doWithGraphsAr = getNotNothingString(messagesAr, "doWithGraphs", errorInMethod);

      dtAccessibleAr = getNotNothingString(messagesAr, "dtAccessible", errorInMethod);
      dtAccessiblePublicAr = getNotNothingString(messagesAr, "dtAccessiblePublic", errorInMethod);
      dtAccessibleYesAr = getNotNothingString(messagesAr, "dtAccessibleYes", errorInMethod);
      dtAccessibleGraphsAr = getNotNothingString(messagesAr, "dtAccessibleGraphs", errorInMethod);
      dtAccessibleNoAr = getNotNothingString(messagesAr, "dtAccessibleNo", errorInMethod);
      dtAccessibleLogInAr = getNotNothingString(messagesAr, "dtAccessibleLogIn", errorInMethod);
      dtLogInAr = getNotNothingString(messagesAr, "dtLogIn", errorInMethod);
      dtDAFAr = getNotNothingString(messagesAr, "dtDAF", errorInMethod);
      dtFilesAr = getNotNothingString(messagesAr, "dtFiles", errorInMethod);
      dtMAGAr = getNotNothingString(messagesAr, "dtMAG", errorInMethod);
      dtSOSAr = getNotNothingString(messagesAr, "dtSOS", errorInMethod);
      dtSubsetAr = getNotNothingString(messagesAr, "dtSubset", errorInMethod);
      dtWCSAr = getNotNothingString(messagesAr, "dtWCS", errorInMethod);
      dtWMSAr = getNotNothingString(messagesAr, "dtWMS", errorInMethod);

      EasierAccessToScientificDataAr =
          getNotNothingString(messagesAr, "EasierAccessToScientificData", errorInMethod);
      EDDDatasetIDAr = getNotNothingString(messagesAr, "EDDDatasetID", errorInMethod);
      EDDFgdc = messagesAr[0].getNotNothingString("EDDFgdc", errorInMethod);
      EDDFgdcMetadataAr = getNotNothingString(messagesAr, "EDDFgdcMetadata", errorInMethod);
      EDDFilesAr = getNotNothingString(messagesAr, "EDDFiles", errorInMethod);
      EDDIso19115 = messagesAr[0].getNotNothingString("EDDIso19115", errorInMethod);
      EDDIso19115MetadataAr = getNotNothingString(messagesAr, "EDDIso19115Metadata", errorInMethod);
      EDDMetadataAr = getNotNothingString(messagesAr, "EDDMetadata", errorInMethod);
      EDDBackgroundAr = getNotNothingString(messagesAr, "EDDBackground", errorInMethod);
      EDDClickOnSubmitHtmlAr =
          getNotNothingString(messagesAr, "EDDClickOnSubmitHtml", errorInMethod);
      EDDInformationAr = getNotNothingString(messagesAr, "EDDInformation", errorInMethod);
      EDDInstitutionAr = getNotNothingString(messagesAr, "EDDInstitution", errorInMethod);
      EDDSummaryAr = getNotNothingString(messagesAr, "EDDSummary", errorInMethod);
      EDDDatasetTitleAr = getNotNothingString(messagesAr, "EDDDatasetTitle", errorInMethod);
      EDDDownloadDataAr = getNotNothingString(messagesAr, "EDDDownloadData", errorInMethod);
      EDDMakeAGraphAr = getNotNothingString(messagesAr, "EDDMakeAGraph", errorInMethod);
      EDDMakeAMapAr = getNotNothingString(messagesAr, "EDDMakeAMap", errorInMethod);
      EDDFileTypeAr = getNotNothingString(messagesAr, "EDDFileType", errorInMethod);
      EDDFileTypeInformationAr =
          getNotNothingString(messagesAr, "EDDFileTypeInformation", errorInMethod);
      EDDSelectFileTypeAr = getNotNothingString(messagesAr, "EDDSelectFileType", errorInMethod);
      EDDMinimumAr = getNotNothingString(messagesAr, "EDDMinimum", errorInMethod);
      EDDMaximumAr = getNotNothingString(messagesAr, "EDDMaximum", errorInMethod);
      EDDConstraintAr = getNotNothingString(messagesAr, "EDDConstraint", errorInMethod);

      EDDChangedWasnt = messagesAr[0].getNotNothingString("EDDChangedWasnt", errorInMethod);
      EDDChangedDifferentNVar =
          messagesAr[0].getNotNothingString("EDDChangedDifferentNVar", errorInMethod);
      EDDChanged2Different =
          messagesAr[0].getNotNothingString("EDDChanged2Different", errorInMethod);
      EDDChanged1Different =
          messagesAr[0].getNotNothingString("EDDChanged1Different", errorInMethod);
      EDDChangedCGADifferent =
          messagesAr[0].getNotNothingString("EDDChangedCGADifferent", errorInMethod);
      EDDChangedAxesDifferentNVar =
          messagesAr[0].getNotNothingString("EDDChangedAxesDifferentNVar", errorInMethod);
      EDDChangedAxes2Different =
          messagesAr[0].getNotNothingString("EDDChangedAxes2Different", errorInMethod);
      EDDChangedAxes1Different =
          messagesAr[0].getNotNothingString("EDDChangedAxes1Different", errorInMethod);
      EDDChangedNoValue = messagesAr[0].getNotNothingString("EDDChangedNoValue", errorInMethod);
      EDDChangedTableToGrid =
          messagesAr[0].getNotNothingString("EDDChangedTableToGrid", errorInMethod);

      EDDSimilarDifferentNVar =
          messagesAr[0].getNotNothingString("EDDSimilarDifferentNVar", errorInMethod);
      EDDSimilarDifferent = messagesAr[0].getNotNothingString("EDDSimilarDifferent", errorInMethod);

      EDDGridDownloadTooltipAr =
          getNotNothingString(messagesAr, "EDDGridDownloadTooltip", errorInMethod);
      EDDGridDapDescriptionAr =
          getNotNothingString(messagesAr, "EDDGridDapDescription", errorInMethod);
      EDDGridDapLongDescriptionAr =
          getNotNothingString(messagesAr, "EDDGridDapLongDescription", errorInMethod);
      EDDGridDownloadDataTooltipAr =
          getNotNothingString(messagesAr, "EDDGridDownloadDataTooltip", errorInMethod);
      EDDGridDimensionAr = getNotNothingString(messagesAr, "EDDGridDimension", errorInMethod);
      EDDGridDimensionRangesAr =
          getNotNothingString(messagesAr, "EDDGridDimensionRanges", errorInMethod);
      EDDGridFirstAr = getNotNothingString(messagesAr, "EDDGridFirst", errorInMethod);
      EDDGridLastAr = getNotNothingString(messagesAr, "EDDGridLast", errorInMethod);
      EDDGridStartAr = getNotNothingString(messagesAr, "EDDGridStart", errorInMethod);
      EDDGridStopAr = getNotNothingString(messagesAr, "EDDGridStop", errorInMethod);
      EDDGridStartStopTooltipAr =
          getNotNothingString(messagesAr, "EDDGridStartStopTooltip", errorInMethod);
      EDDGridStrideAr = getNotNothingString(messagesAr, "EDDGridStride", errorInMethod);
      EDDGridNValuesAr = getNotNothingString(messagesAr, "EDDGridNValues", errorInMethod);
      EDDGridNValuesHtmlAr = getNotNothingString(messagesAr, "EDDGridNValuesHtml", errorInMethod);
      EDDGridSpacingAr = getNotNothingString(messagesAr, "EDDGridSpacing", errorInMethod);
      EDDGridJustOneValueAr = getNotNothingString(messagesAr, "EDDGridJustOneValue", errorInMethod);
      EDDGridEvenAr = getNotNothingString(messagesAr, "EDDGridEven", errorInMethod);
      EDDGridUnevenAr = getNotNothingString(messagesAr, "EDDGridUneven", errorInMethod);
      EDDGridDimensionTooltipAr =
          getNotNothingString(messagesAr, "EDDGridDimensionTooltip", errorInMethod);
      EDDGridDimensionFirstTooltipAr =
          getNotNothingString(messagesAr, "EDDGridDimensionFirstTooltip", errorInMethod);
      EDDGridDimensionLastTooltipAr =
          getNotNothingString(messagesAr, "EDDGridDimensionLastTooltip", errorInMethod);
      EDDGridVarHasDimTooltipAr =
          getNotNothingString(messagesAr, "EDDGridVarHasDimTooltip", errorInMethod);
      EDDGridSSSTooltipAr = getNotNothingString(messagesAr, "EDDGridSSSTooltip", errorInMethod);
      EDDGridStartTooltipAr = getNotNothingString(messagesAr, "EDDGridStartTooltip", errorInMethod);
      EDDGridStopTooltipAr = getNotNothingString(messagesAr, "EDDGridStopTooltip", errorInMethod);
      EDDGridStrideTooltipAr =
          getNotNothingString(messagesAr, "EDDGridStrideTooltip", errorInMethod);
      EDDGridSpacingTooltipAr =
          getNotNothingString(messagesAr, "EDDGridSpacingTooltip", errorInMethod);
      EDDGridGridVariableHtmlAr =
          getNotNothingString(messagesAr, "EDDGridGridVariableHtml", errorInMethod);
      EDDGridCheckAllAr = getNotNothingString(messagesAr, "EDDGridCheckAll", errorInMethod);
      EDDGridCheckAllTooltipAr =
          getNotNothingString(messagesAr, "EDDGridCheckAllTooltip", errorInMethod);
      EDDGridUncheckAllAr = getNotNothingString(messagesAr, "EDDGridUncheckAll", errorInMethod);
      EDDGridUncheckAllTooltipAr =
          getNotNothingString(messagesAr, "EDDGridUncheckAllTooltip", errorInMethod);

      // default EDDGrid...Example
      EDDGridErddapUrlExample =
          messagesAr[0].getNotNothingString("EDDGridErddapUrlExample", errorInMethod);
      EDDGridIdExample = messagesAr[0].getNotNothingString("EDDGridIdExample", errorInMethod);
      EDDGridDimensionExample =
          messagesAr[0].getNotNothingString("EDDGridDimensionExample", errorInMethod);
      EDDGridNoHyperExample =
          messagesAr[0].getNotNothingString("EDDGridNoHyperExample", errorInMethod);
      EDDGridDimNamesExample =
          messagesAr[0].getNotNothingString("EDDGridDimNamesExample", errorInMethod);
      EDDGridDataTimeExample =
          messagesAr[0].getNotNothingString("EDDGridDataTimeExample", errorInMethod);
      EDDGridDataValueExample =
          messagesAr[0].getNotNothingString("EDDGridDataValueExample", errorInMethod);
      EDDGridDataIndexExample =
          messagesAr[0].getNotNothingString("EDDGridDataIndexExample", errorInMethod);
      EDDGridGraphExample = messagesAr[0].getNotNothingString("EDDGridGraphExample", errorInMethod);
      EDDGridMapExample = messagesAr[0].getNotNothingString("EDDGridMapExample", errorInMethod);
      EDDGridMatlabPlotExample =
          messagesAr[0].getNotNothingString("EDDGridMatlabPlotExample", errorInMethod);

      // admin provides EDDGrid...Example
      EDDGridErddapUrlExample =
          getSetupEVString(setup, ev, "EDDGridErddapUrlExample", EDDGridErddapUrlExample);
      EDDGridIdExample = getSetupEVString(setup, ev, "EDDGridIdExample", EDDGridIdExample);
      EDDGridDimensionExample =
          getSetupEVString(setup, ev, "EDDGridDimensionExample", EDDGridDimensionExample);
      EDDGridNoHyperExample =
          getSetupEVString(setup, ev, "EDDGridNoHyperExample", EDDGridNoHyperExample);
      EDDGridDimNamesExample =
          getSetupEVString(setup, ev, "EDDGridDimNamesExample", EDDGridDimNamesExample);
      EDDGridDataIndexExample =
          getSetupEVString(setup, ev, "EDDGridDataIndexExample", EDDGridDataIndexExample);
      EDDGridDataValueExample =
          getSetupEVString(setup, ev, "EDDGridDataValueExample", EDDGridDataValueExample);
      EDDGridDataTimeExample =
          getSetupEVString(setup, ev, "EDDGridDataTimeExample", EDDGridDataTimeExample);
      EDDGridGraphExample = getSetupEVString(setup, ev, "EDDGridGraphExample", EDDGridGraphExample);
      EDDGridMapExample = getSetupEVString(setup, ev, "EDDGridMapExample", EDDGridMapExample);
      EDDGridMatlabPlotExample =
          getSetupEVString(setup, ev, "EDDGridMatlabPlotExample", EDDGridMatlabPlotExample);

      // variants encoded to be Html Examples
      EDDGridDimensionExampleHE = XML.encodeAsHTML(EDDGridDimensionExample);
      EDDGridDataIndexExampleHE = XML.encodeAsHTML(EDDGridDataIndexExample);
      EDDGridDataValueExampleHE = XML.encodeAsHTML(EDDGridDataValueExample);
      EDDGridDataTimeExampleHE = XML.encodeAsHTML(EDDGridDataTimeExample);
      EDDGridGraphExampleHE = XML.encodeAsHTML(EDDGridGraphExample);
      EDDGridMapExampleHE = XML.encodeAsHTML(EDDGridMapExample);

      // variants encoded to be Html Attributes
      EDDGridDimensionExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDimensionExample));
      EDDGridDataIndexExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataIndexExample));
      EDDGridDataValueExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataValueExample));
      EDDGridDataTimeExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridDataTimeExample));
      EDDGridGraphExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridGraphExample));
      EDDGridMapExampleHA = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDGridMapExample));

      EDDTableConstraintsAr = getNotNothingString(messagesAr, "EDDTableConstraints", errorInMethod);
      EDDTableDapDescriptionAr =
          getNotNothingString(messagesAr, "EDDTableDapDescription", errorInMethod);
      EDDTableDapLongDescriptionAr =
          getNotNothingString(messagesAr, "EDDTableDapLongDescription", errorInMethod);
      EDDTableDownloadDataTooltipAr =
          getNotNothingString(messagesAr, "EDDTableDownloadDataTooltip", errorInMethod);
      EDDTableTabularDatasetTooltipAr =
          getNotNothingString(messagesAr, "EDDTableTabularDatasetTooltip", errorInMethod);
      EDDTableVariableAr = getNotNothingString(messagesAr, "EDDTableVariable", errorInMethod);
      EDDTableCheckAllAr = getNotNothingString(messagesAr, "EDDTableCheckAll", errorInMethod);
      EDDTableCheckAllTooltipAr =
          getNotNothingString(messagesAr, "EDDTableCheckAllTooltip", errorInMethod);
      EDDTableUncheckAllAr = getNotNothingString(messagesAr, "EDDTableUncheckAll", errorInMethod);
      EDDTableUncheckAllTooltipAr =
          getNotNothingString(messagesAr, "EDDTableUncheckAllTooltip", errorInMethod);
      EDDTableMinimumTooltipAr =
          getNotNothingString(messagesAr, "EDDTableMinimumTooltip", errorInMethod);
      EDDTableMaximumTooltipAr =
          getNotNothingString(messagesAr, "EDDTableMaximumTooltip", errorInMethod);
      EDDTableCheckTheVariablesAr =
          getNotNothingString(messagesAr, "EDDTableCheckTheVariables", errorInMethod);
      EDDTableSelectAnOperatorAr =
          getNotNothingString(messagesAr, "EDDTableSelectAnOperator", errorInMethod);
      EDDTableFromEDDGridSummaryAr =
          getNotNothingString(messagesAr, "EDDTableFromEDDGridSummary", errorInMethod);
      EDDTableOptConstraint1HtmlAr =
          getNotNothingString(messagesAr, "EDDTableOptConstraint1Html", errorInMethod);
      EDDTableOptConstraint2HtmlAr =
          getNotNothingString(messagesAr, "EDDTableOptConstraint2Html", errorInMethod);
      EDDTableOptConstraintVarAr =
          getNotNothingString(messagesAr, "EDDTableOptConstraintVar", errorInMethod);
      EDDTableNumericConstraintTooltipAr =
          getNotNothingString(messagesAr, "EDDTableNumericConstraintTooltip", errorInMethod);
      EDDTableStringConstraintTooltipAr =
          getNotNothingString(messagesAr, "EDDTableStringConstraintTooltip", errorInMethod);
      EDDTableTimeConstraintTooltipAr =
          getNotNothingString(messagesAr, "EDDTableTimeConstraintTooltip", errorInMethod);
      EDDTableConstraintTooltipAr =
          getNotNothingString(messagesAr, "EDDTableConstraintTooltip", errorInMethod);
      EDDTableSelectConstraintTooltipAr =
          getNotNothingString(messagesAr, "EDDTableSelectConstraintTooltip", errorInMethod);

      // default EDDGrid...Example
      EDDTableErddapUrlExample =
          messagesAr[0].getNotNothingString("EDDTableErddapUrlExample", errorInMethod);
      EDDTableIdExample = messagesAr[0].getNotNothingString("EDDTableIdExample", errorInMethod);
      EDDTableVariablesExample =
          messagesAr[0].getNotNothingString("EDDTableVariablesExample", errorInMethod);
      EDDTableConstraintsExample =
          messagesAr[0].getNotNothingString("EDDTableConstraintsExample", errorInMethod);
      EDDTableDataValueExample =
          messagesAr[0].getNotNothingString("EDDTableDataValueExample", errorInMethod);
      EDDTableDataTimeExample =
          messagesAr[0].getNotNothingString("EDDTableDataTimeExample", errorInMethod);
      EDDTableGraphExample =
          messagesAr[0].getNotNothingString("EDDTableGraphExample", errorInMethod);
      EDDTableMapExample = messagesAr[0].getNotNothingString("EDDTableMapExample", errorInMethod);
      EDDTableMatlabPlotExample =
          messagesAr[0].getNotNothingString("EDDTableMatlabPlotExample", errorInMethod);

      // admin provides EDDGrid...Example
      EDDTableErddapUrlExample =
          getSetupEVString(setup, ev, "EDDTableErddapUrlExample", EDDTableErddapUrlExample);
      EDDTableIdExample = getSetupEVString(setup, ev, "EDDTableIdExample", EDDTableIdExample);
      EDDTableVariablesExample =
          getSetupEVString(setup, ev, "EDDTableVariablesExample", EDDTableVariablesExample);
      EDDTableConstraintsExample =
          getSetupEVString(setup, ev, "EDDTableConstraintsExample", EDDTableConstraintsExample);
      EDDTableDataValueExample =
          getSetupEVString(setup, ev, "EDDTableDataValueExample", EDDTableDataValueExample);
      EDDTableDataTimeExample =
          getSetupEVString(setup, ev, "EDDTableDataTimeExample", EDDTableDataTimeExample);
      EDDTableGraphExample =
          getSetupEVString(setup, ev, "EDDTableGraphExample", EDDTableGraphExample);
      EDDTableMapExample = getSetupEVString(setup, ev, "EDDTableMapExample", EDDTableMapExample);
      EDDTableMatlabPlotExample =
          getSetupEVString(setup, ev, "EDDTableMatlabPlotExample", EDDTableMatlabPlotExample);

      // variants encoded to be Html Examples
      EDDTableConstraintsExampleHE = XML.encodeAsHTML(EDDTableConstraintsExample);
      EDDTableDataTimeExampleHE = XML.encodeAsHTML(EDDTableDataTimeExample);
      EDDTableDataValueExampleHE = XML.encodeAsHTML(EDDTableDataValueExample);
      EDDTableGraphExampleHE = XML.encodeAsHTML(EDDTableGraphExample);
      EDDTableMapExampleHE = XML.encodeAsHTML(EDDTableMapExample);

      // variants encoded to be Html Attributes
      EDDTableConstraintsExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableConstraintsExample));
      EDDTableDataTimeExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableDataTimeExample));
      EDDTableDataValueExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableDataValueExample));
      EDDTableGraphExampleHA =
          XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableGraphExample));
      EDDTableMapExampleHA = XML.encodeAsHTMLAttribute(SSR.pseudoPercentEncode(EDDTableMapExample));

      EDDTableFromHttpGetDatasetDescription =
          XML.decodeEntities( // because this is used as plain text
              messagesAr[0].getNotNothingString(
                  "EDDTableFromHttpGetDatasetDescription", errorInMethod));
      EDDTableFromHttpGetAuthorDescription =
          messagesAr[0].getNotNothingString("EDDTableFromHttpGetAuthorDescription", errorInMethod);
      EDDTableFromHttpGetTimestampDescription =
          messagesAr[0].getNotNothingString(
              "EDDTableFromHttpGetTimestampDescription", errorInMethod);

      errorTitleAr = getNotNothingString(messagesAr, "errorTitle", errorInMethod);
      erddapIsAr = getNotNothingString(messagesAr, "erddapIs", errorInMethod);
      erddapVersionHTMLAr = getNotNothingString(messagesAr, "erddapVersionHTML", errorInMethod);
      errorRequestUrlAr = getNotNothingString(messagesAr, "errorRequestUrl", errorInMethod);
      errorRequestQueryAr = getNotNothingString(messagesAr, "errorRequestQuery", errorInMethod);
      errorTheErrorAr = getNotNothingString(messagesAr, "errorTheError", errorInMethod);
      errorCopyFromAr = getNotNothingString(messagesAr, "errorCopyFrom", errorInMethod);
      errorFileNotFoundAr = getNotNothingString(messagesAr, "errorFileNotFound", errorInMethod);
      errorFileNotFoundImageAr =
          getNotNothingString(messagesAr, "errorFileNotFoundImage", errorInMethod);
      errorInternalAr = getNotNothingString(messagesAr, "errorInternal", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) errorInternalAr[tl] += " ";

      errorJsonpFunctionNameAr =
          getNotNothingString(messagesAr, "errorJsonpFunctionName", errorInMethod);
      errorJsonpNotAllowedAr =
          getNotNothingString(messagesAr, "errorJsonpNotAllowed", errorInMethod);
      errorMoreThan2GBAr = getNotNothingString(messagesAr, "errorMoreThan2GB", errorInMethod);
      errorNotFoundAr = getNotNothingString(messagesAr, "errorNotFound", errorInMethod);
      errorNotFoundInAr = getNotNothingString(messagesAr, "errorNotFoundIn", errorInMethod);
      errorOdvLLTGridAr = getNotNothingString(messagesAr, "errorOdvLLTGrid", errorInMethod);
      errorOdvLLTTableAr = getNotNothingString(messagesAr, "errorOdvLLTTable", errorInMethod);
      errorOnWebPageAr = getNotNothingString(messagesAr, "errorOnWebPage", errorInMethod);
      HtmlWidgets.errorXWasntSpecifiedAr =
          getNotNothingString(messagesAr, "errorXWasntSpecified", errorInMethod);
      HtmlWidgets.errorXWasTooLongAr =
          getNotNothingString(messagesAr, "errorXWasTooLong", errorInMethod);
      extensionsNoRangeRequests =
          StringArray.arrayFromCSV(
              messagesAr[0].getNotNothingString("extensionsNoRangeRequests", errorInMethod),
              ",",
              true,
              false); // trim, keepNothing

      externalLinkAr = getNotNothingString(messagesAr, "externalLink", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) externalLinkAr[tl] = " " + externalLinkAr[tl];

      externalWebSiteAr = getNotNothingString(messagesAr, "externalWebSite", errorInMethod);
      fileHelp_ascAr = getNotNothingString(messagesAr, "fileHelp_asc", errorInMethod);
      fileHelp_csvAr = getNotNothingString(messagesAr, "fileHelp_csv", errorInMethod);
      fileHelp_csvpAr = getNotNothingString(messagesAr, "fileHelp_csvp", errorInMethod);
      fileHelp_csv0Ar = getNotNothingString(messagesAr, "fileHelp_csv0", errorInMethod);
      fileHelp_dataTableAr = getNotNothingString(messagesAr, "fileHelp_dataTable", errorInMethod);
      fileHelp_dasAr = getNotNothingString(messagesAr, "fileHelp_das", errorInMethod);
      fileHelp_ddsAr = getNotNothingString(messagesAr, "fileHelp_dds", errorInMethod);
      fileHelp_dodsAr = getNotNothingString(messagesAr, "fileHelp_dods", errorInMethod);
      fileHelpGrid_esriAsciiAr =
          getNotNothingString(messagesAr, "fileHelpGrid_esriAscii", errorInMethod);
      fileHelpTable_esriCsvAr =
          getNotNothingString(messagesAr, "fileHelpTable_esriCsv", errorInMethod);
      fileHelp_fgdcAr = getNotNothingString(messagesAr, "fileHelp_fgdc", errorInMethod);
      fileHelp_geoJsonAr = getNotNothingString(messagesAr, "fileHelp_geoJson", errorInMethod);
      fileHelp_graphAr = getNotNothingString(messagesAr, "fileHelp_graph", errorInMethod);
      fileHelpGrid_helpAr = getNotNothingString(messagesAr, "fileHelpGrid_help", errorInMethod);
      fileHelpTable_helpAr = getNotNothingString(messagesAr, "fileHelpTable_help", errorInMethod);
      fileHelp_htmlAr = getNotNothingString(messagesAr, "fileHelp_html", errorInMethod);
      fileHelp_htmlTableAr = getNotNothingString(messagesAr, "fileHelp_htmlTable", errorInMethod);
      fileHelp_iso19115Ar = getNotNothingString(messagesAr, "fileHelp_iso19115", errorInMethod);
      fileHelp_itxGridAr = getNotNothingString(messagesAr, "fileHelp_itxGrid", errorInMethod);
      fileHelp_itxTableAr = getNotNothingString(messagesAr, "fileHelp_itxTable", errorInMethod);
      fileHelp_jsonAr = getNotNothingString(messagesAr, "fileHelp_json", errorInMethod);
      fileHelp_jsonlCSV1Ar = getNotNothingString(messagesAr, "fileHelp_jsonlCSV1", errorInMethod);
      fileHelp_jsonlCSVAr = getNotNothingString(messagesAr, "fileHelp_jsonlCSV", errorInMethod);
      fileHelp_jsonlKVPAr = getNotNothingString(messagesAr, "fileHelp_jsonlKVP", errorInMethod);
      fileHelp_matAr = getNotNothingString(messagesAr, "fileHelp_mat", errorInMethod);
      fileHelpGrid_nc3Ar = getNotNothingString(messagesAr, "fileHelpGrid_nc3", errorInMethod);
      fileHelpGrid_nc4Ar = getNotNothingString(messagesAr, "fileHelpGrid_nc4", errorInMethod);
      fileHelpTable_nc3Ar = getNotNothingString(messagesAr, "fileHelpTable_nc3", errorInMethod);
      fileHelpTable_nc4Ar = getNotNothingString(messagesAr, "fileHelpTable_nc4", errorInMethod);
      fileHelp_nc3HeaderAr = getNotNothingString(messagesAr, "fileHelp_nc3Header", errorInMethod);
      fileHelp_nc4HeaderAr = getNotNothingString(messagesAr, "fileHelp_nc4Header", errorInMethod);
      fileHelp_nccsvAr = getNotNothingString(messagesAr, "fileHelp_nccsv", errorInMethod);
      fileHelp_nccsvMetadataAr =
          getNotNothingString(messagesAr, "fileHelp_nccsvMetadata", errorInMethod);
      fileHelp_ncCFAr = getNotNothingString(messagesAr, "fileHelp_ncCF", errorInMethod);
      fileHelp_ncCFHeaderAr = getNotNothingString(messagesAr, "fileHelp_ncCFHeader", errorInMethod);
      fileHelp_ncCFMAAr = getNotNothingString(messagesAr, "fileHelp_ncCFMA", errorInMethod);
      fileHelp_ncCFMAHeaderAr =
          getNotNothingString(messagesAr, "fileHelp_ncCFMAHeader", errorInMethod);
      fileHelp_ncmlAr = getNotNothingString(messagesAr, "fileHelp_ncml", errorInMethod);
      fileHelp_ncoJsonAr = getNotNothingString(messagesAr, "fileHelp_ncoJson", errorInMethod);
      fileHelpGrid_odvTxtAr = getNotNothingString(messagesAr, "fileHelpGrid_odvTxt", errorInMethod);
      fileHelpTable_odvTxtAr =
          getNotNothingString(messagesAr, "fileHelpTable_odvTxt", errorInMethod);
      fileHelp_parquetAr = getNotNothingString(messagesAr, "fileHelp_parquet", errorInMethod);
      fileHelp_parquet_with_metaAr =
          getNotNothingString(messagesAr, "fileHelp_parquet_with_meta", errorInMethod);
      fileHelp_subsetAr = getNotNothingString(messagesAr, "fileHelp_subset", errorInMethod);
      fileHelp_timeGapsAr = getNotNothingString(messagesAr, "fileHelp_timeGaps", errorInMethod);
      fileHelp_tsvAr = getNotNothingString(messagesAr, "fileHelp_tsv", errorInMethod);
      fileHelp_tsvpAr = getNotNothingString(messagesAr, "fileHelp_tsvp", errorInMethod);
      fileHelp_tsv0Ar = getNotNothingString(messagesAr, "fileHelp_tsv0", errorInMethod);
      fileHelp_wavAr = getNotNothingString(messagesAr, "fileHelp_wav", errorInMethod);
      fileHelp_xhtmlAr = getNotNothingString(messagesAr, "fileHelp_xhtml", errorInMethod);
      fileHelp_geotifAr = getNotNothingString(messagesAr, "fileHelp_geotif", errorInMethod);
      fileHelpGrid_kmlAr = getNotNothingString(messagesAr, "fileHelpGrid_kml", errorInMethod);
      fileHelpTable_kmlAr = getNotNothingString(messagesAr, "fileHelpTable_kml", errorInMethod);
      fileHelp_smallPdfAr = getNotNothingString(messagesAr, "fileHelp_smallPdf", errorInMethod);
      fileHelp_pdfAr = getNotNothingString(messagesAr, "fileHelp_pdf", errorInMethod);
      fileHelp_largePdfAr = getNotNothingString(messagesAr, "fileHelp_largePdf", errorInMethod);
      fileHelp_smallPngAr = getNotNothingString(messagesAr, "fileHelp_smallPng", errorInMethod);
      fileHelp_pngAr = getNotNothingString(messagesAr, "fileHelp_png", errorInMethod);
      fileHelp_largePngAr = getNotNothingString(messagesAr, "fileHelp_largePng", errorInMethod);
      fileHelp_transparentPngAr =
          getNotNothingString(messagesAr, "fileHelp_transparentPng", errorInMethod);
      filesDescriptionAr = getNotNothingString(messagesAr, "filesDescription", errorInMethod);
      filesDocumentationAr = getNotNothingString(messagesAr, "filesDocumentation", errorInMethod);
      filesSortAr = getNotNothingString(messagesAr, "filesSort", errorInMethod);
      filesWarningAr = getNotNothingString(messagesAr, "filesWarning", errorInMethod);
      findOutChangeAr = getNotNothingString(messagesAr, "findOutChange", errorInMethod);
      FIPSCountyCodesAr = getNotNothingString(messagesAr, "FIPSCountyCodes", errorInMethod);
      forSOSUseAr = getNotNothingString(messagesAr, "forSOSUse", errorInMethod);
      forWCSUseAr = getNotNothingString(messagesAr, "forWCSUse", errorInMethod);
      forWMSUseAr = getNotNothingString(messagesAr, "forWMSUse", errorInMethod);
      functionsAr = getNotNothingString(messagesAr, "functions", errorInMethod);
      functionTooltipAr = getNotNothingString(messagesAr, "functionTooltip", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++)
        functionTooltipAr[tl] = MessageFormat.format(functionTooltipAr[tl], "distinct()");

      functionDistinctCheckAr =
          getNotNothingString(messagesAr, "functionDistinctCheck", errorInMethod);
      functionDistinctTooltipAr =
          getNotNothingString(messagesAr, "functionDistinctTooltip", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++)
        functionDistinctTooltipAr[tl] =
            MessageFormat.format(functionDistinctTooltipAr[tl], "distinct()");

      functionOrderByExtraAr =
          getNotNothingString(messagesAr, "functionOrderByExtra", errorInMethod);
      functionOrderByTooltipAr =
          getNotNothingString(messagesAr, "functionOrderByTooltip", errorInMethod);
      functionOrderBySortAr = getNotNothingString(messagesAr, "functionOrderBySort", errorInMethod);
      functionOrderBySort1Ar =
          getNotNothingString(messagesAr, "functionOrderBySort1", errorInMethod);
      functionOrderBySort2Ar =
          getNotNothingString(messagesAr, "functionOrderBySort2", errorInMethod);
      functionOrderBySort3Ar =
          getNotNothingString(messagesAr, "functionOrderBySort3", errorInMethod);
      functionOrderBySort4Ar =
          getNotNothingString(messagesAr, "functionOrderBySort4", errorInMethod);
      functionOrderBySortLeastAr =
          getNotNothingString(messagesAr, "functionOrderBySortLeast", errorInMethod);
      functionOrderBySortRowMaxAr =
          getNotNothingString(messagesAr, "functionOrderBySortRowMax", errorInMethod);
      generatedAtAr = getNotNothingString(messagesAr, "generatedAt", errorInMethod);
      geoServicesDescriptionAr =
          getNotNothingString(messagesAr, "geoServicesDescription", errorInMethod);
      getStartedHtmlAr = getNotNothingString(messagesAr, "getStartedHtml", errorInMethod);
      helpAr = getNotNothingString(messagesAr, "help", errorInMethod);
      TableWriterHtmlTable.htmlTableMaxMB =
          messagesAr[0].getInt("htmlTableMaxMB", TableWriterHtmlTable.htmlTableMaxMB);
      htmlTableMaxMessageAr = getNotNothingString(messagesAr, "htmlTableMaxMessage", errorInMethod);

      imageDataCourtesyOfAr = getNotNothingString(messagesAr, "imageDataCourtesyOf", errorInMethod);
      imageWidths =
          String2.toIntArray(
              String2.split(messagesAr[0].getNotNothingString("imageWidths", errorInMethod), ','));
      imageHeights =
          String2.toIntArray(
              String2.split(messagesAr[0].getNotNothingString("imageHeights", errorInMethod), ','));
      imagesEmbedAr = getNotNothingString(messagesAr, "imagesEmbed", errorInMethod);
      indexViewAllAr = getNotNothingString(messagesAr, "indexViewAll", errorInMethod);
      indexSearchWithAr = getNotNothingString(messagesAr, "indexSearchWith", errorInMethod);
      indexDevelopersSearchAr =
          getNotNothingString(messagesAr, "indexDevelopersSearch", errorInMethod);
      indexProtocolAr = getNotNothingString(messagesAr, "indexProtocol", errorInMethod);
      indexDescriptionAr = getNotNothingString(messagesAr, "indexDescription", errorInMethod);
      indexDatasetsAr = getNotNothingString(messagesAr, "indexDatasets", errorInMethod);
      indexDocumentationAr = getNotNothingString(messagesAr, "indexDocumentation", errorInMethod);
      indexRESTfulSearchAr = getNotNothingString(messagesAr, "indexRESTfulSearch", errorInMethod);
      indexAllDatasetsSearchAr =
          getNotNothingString(messagesAr, "indexAllDatasetsSearch", errorInMethod);
      indexOpenSearchAr = getNotNothingString(messagesAr, "indexOpenSearch", errorInMethod);
      indexServicesAr = getNotNothingString(messagesAr, "indexServices", errorInMethod);
      indexDescribeServicesAr =
          getNotNothingString(messagesAr, "indexDescribeServices", errorInMethod);
      indexMetadataAr = getNotNothingString(messagesAr, "indexMetadata", errorInMethod);
      indexWAF1Ar = getNotNothingString(messagesAr, "indexWAF1", errorInMethod);
      indexWAF2Ar = getNotNothingString(messagesAr, "indexWAF2", errorInMethod);
      indexConvertersAr = getNotNothingString(messagesAr, "indexConverters", errorInMethod);
      indexDescribeConvertersAr =
          getNotNothingString(messagesAr, "indexDescribeConverters", errorInMethod);
      infoAboutFromAr = getNotNothingString(messagesAr, "infoAboutFrom", errorInMethod);
      infoTableTitleHtmlAr = getNotNothingString(messagesAr, "infoTableTitleHtml", errorInMethod);
      infoRequestFormAr = getNotNothingString(messagesAr, "infoRequestForm", errorInMethod);
      informationAr = getNotNothingString(messagesAr, "information", errorInMethod);
      inotifyFixAr = getNotNothingString(messagesAr, "inotifyFix", errorInMethod);
      inotifyFixCommands = messagesAr[0].getNotNothingString("inotifyFixCommands", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++)
        inotifyFixAr[tl] = MessageFormat.format(inotifyFixAr[tl], inotifyFixCommands);
      interpolateAr = getNotNothingString(messagesAr, "interpolate", errorInMethod);
      javaProgramsHTMLAr = getNotNothingString(messagesAr, "javaProgramsHTML", errorInMethod);
      justGenerateAndViewAr = getNotNothingString(messagesAr, "justGenerateAndView", errorInMethod);
      justGenerateAndViewTooltipAr =
          getNotNothingString(messagesAr, "justGenerateAndViewTooltip", errorInMethod);
      justGenerateAndViewUrlAr =
          getNotNothingString(messagesAr, "justGenerateAndViewUrl", errorInMethod);
      justGenerateAndViewGraphUrlTooltipAr =
          getNotNothingString(messagesAr, "justGenerateAndViewGraphUrlTooltip", errorInMethod);
      keywordsAr = getNotNothingString(messagesAr, "keywords", errorInMethod);
      langCodeAr = getNotNothingString(messagesAr, "langCode", errorInMethod);

      legal = messagesAr[0].getNotNothingString("legal", errorInMethod);
      legal = getSetupEVString(setup, ev, "legal", legal); // optionally in setup.xml
      legalNoticesAr = getNotNothingString(messagesAr, "legalNotices", errorInMethod);
      legalNoticesTitleAr = getNotNothingString(messagesAr, "legalNoticesTitle", errorInMethod);

      legendTitle1 = messagesAr[0].getString("legendTitle1", "");
      legendTitle2 = messagesAr[0].getString("legendTitle2", "");
      legendTitle1 =
          getSetupEVString(setup, ev, "legendTitle1", legendTitle1); // optionally in setup.xml
      legendTitle2 =
          getSetupEVString(setup, ev, "legendTitle2", legendTitle2); // optionally in setup.xml

      licenseAr = getNotNothingString(messagesAr, "license", errorInMethod);
      likeThisAr = getNotNothingString(messagesAr, "likeThis", errorInMethod);
      listAllAr = getNotNothingString(messagesAr, "listAll", errorInMethod);
      listOfDatasetsAr = getNotNothingString(messagesAr, "listOfDatasets", errorInMethod);
      LogInAr = getNotNothingString(messagesAr, "LogIn", errorInMethod);
      loginAr = getNotNothingString(messagesAr, "login", errorInMethod);
      loginHTMLAr = getNotNothingString(messagesAr, "loginHTML", errorInMethod);
      loginAttemptBlockedAr = getNotNothingString(messagesAr, "loginAttemptBlocked", errorInMethod);
      loginDescribeCustomAr = getNotNothingString(messagesAr, "loginDescribeCustom", errorInMethod);
      loginDescribeEmailAr = getNotNothingString(messagesAr, "loginDescribeEmail", errorInMethod);
      loginDescribeGoogleAr = getNotNothingString(messagesAr, "loginDescribeGoogle", errorInMethod);
      loginDescribeOrcidAr = getNotNothingString(messagesAr, "loginDescribeOrcid", errorInMethod);
      loginDescribeOauth2Ar = getNotNothingString(messagesAr, "loginDescribeOauth2", errorInMethod);
      loginCanNotAr = getNotNothingString(messagesAr, "loginCanNot", errorInMethod);
      loginAreNotAr = getNotNothingString(messagesAr, "loginAreNot", errorInMethod);
      loginToLogInAr = getNotNothingString(messagesAr, "loginToLogIn", errorInMethod);
      loginEmailAddressAr = getNotNothingString(messagesAr, "loginEmailAddress", errorInMethod);
      loginYourEmailAddressAr =
          getNotNothingString(messagesAr, "loginYourEmailAddress", errorInMethod);
      loginUserNameAr = getNotNothingString(messagesAr, "loginUserName", errorInMethod);
      loginPasswordAr = getNotNothingString(messagesAr, "loginPassword", errorInMethod);
      loginUserNameAndPasswordAr =
          getNotNothingString(messagesAr, "loginUserNameAndPassword", errorInMethod);
      loginGoogleSignInAr = getNotNothingString(messagesAr, "loginGoogleSignIn", errorInMethod);
      loginOrcidSignInAr = getNotNothingString(messagesAr, "loginOrcidSignIn", errorInMethod);
      loginErddapAr = getNotNothingString(messagesAr, "loginErddap", errorInMethod);
      loginOpenIDAr = getNotNothingString(messagesAr, "loginOpenID", errorInMethod);
      loginOpenIDOrAr = getNotNothingString(messagesAr, "loginOpenIDOr", errorInMethod);
      loginOpenIDCreateAr = getNotNothingString(messagesAr, "loginOpenIDCreate", errorInMethod);
      loginOpenIDFreeAr = getNotNothingString(messagesAr, "loginOpenIDFree", errorInMethod);
      loginOpenIDSameAr = getNotNothingString(messagesAr, "loginOpenIDSame", errorInMethod);
      loginAsAr = getNotNothingString(messagesAr, "loginAs", errorInMethod);
      loginPartwayAsAr = getNotNothingString(messagesAr, "loginPartwayAs", errorInMethod);
      loginFailedAr = getNotNothingString(messagesAr, "loginFailed", errorInMethod);
      loginSucceededAr = getNotNothingString(messagesAr, "loginSucceeded", errorInMethod);
      loginInvalidAr = getNotNothingString(messagesAr, "loginInvalid", errorInMethod);
      loginNotAr = getNotNothingString(messagesAr, "loginNot", errorInMethod);
      loginBackAr = getNotNothingString(messagesAr, "loginBack", errorInMethod);
      loginProblemExactAr = getNotNothingString(messagesAr, "loginProblemExact", errorInMethod);
      loginProblemExpireAr = getNotNothingString(messagesAr, "loginProblemExpire", errorInMethod);
      loginProblemGoogleAgainAr =
          getNotNothingString(messagesAr, "loginProblemGoogleAgain", errorInMethod);
      loginProblemOrcidAgainAr =
          getNotNothingString(messagesAr, "loginProblemOrcidAgain", errorInMethod);
      loginProblemOauth2AgainAr =
          getNotNothingString(messagesAr, "loginProblemOauth2Again", errorInMethod);
      loginProblemSameBrowserAr =
          getNotNothingString(messagesAr, "loginProblemSameBrowser", errorInMethod);
      loginProblem3TimesAr = getNotNothingString(messagesAr, "loginProblem3Times", errorInMethod);
      loginProblemsAr = getNotNothingString(messagesAr, "loginProblems", errorInMethod);
      loginProblemsAfterAr = getNotNothingString(messagesAr, "loginProblemsAfter", errorInMethod);
      loginPublicAccessAr = getNotNothingString(messagesAr, "loginPublicAccess", errorInMethod);
      LogOutAr = getNotNothingString(messagesAr, "LogOut", errorInMethod);
      logoutAr = getNotNothingString(messagesAr, "logout", errorInMethod);
      logoutOpenIDAr = getNotNothingString(messagesAr, "logoutOpenID", errorInMethod);
      logoutSuccessAr = getNotNothingString(messagesAr, "logoutSuccess", errorInMethod);
      magAr = getNotNothingString(messagesAr, "mag", errorInMethod);
      magAxisXAr = getNotNothingString(messagesAr, "magAxisX", errorInMethod);
      magAxisYAr = getNotNothingString(messagesAr, "magAxisY", errorInMethod);
      magAxisColorAr = getNotNothingString(messagesAr, "magAxisColor", errorInMethod);
      magAxisStickXAr = getNotNothingString(messagesAr, "magAxisStickX", errorInMethod);
      magAxisStickYAr = getNotNothingString(messagesAr, "magAxisStickY", errorInMethod);
      magAxisVectorXAr = getNotNothingString(messagesAr, "magAxisVectorX", errorInMethod);
      magAxisVectorYAr = getNotNothingString(messagesAr, "magAxisVectorY", errorInMethod);
      magAxisHelpGraphXAr = getNotNothingString(messagesAr, "magAxisHelpGraphX", errorInMethod);
      magAxisHelpGraphYAr = getNotNothingString(messagesAr, "magAxisHelpGraphY", errorInMethod);
      magAxisHelpMarkerColorAr =
          getNotNothingString(messagesAr, "magAxisHelpMarkerColor", errorInMethod);
      magAxisHelpSurfaceColorAr =
          getNotNothingString(messagesAr, "magAxisHelpSurfaceColor", errorInMethod);
      magAxisHelpStickXAr = getNotNothingString(messagesAr, "magAxisHelpStickX", errorInMethod);
      magAxisHelpStickYAr = getNotNothingString(messagesAr, "magAxisHelpStickY", errorInMethod);
      magAxisHelpMapXAr = getNotNothingString(messagesAr, "magAxisHelpMapX", errorInMethod);
      magAxisHelpMapYAr = getNotNothingString(messagesAr, "magAxisHelpMapY", errorInMethod);
      magAxisHelpVectorXAr = getNotNothingString(messagesAr, "magAxisHelpVectorX", errorInMethod);
      magAxisHelpVectorYAr = getNotNothingString(messagesAr, "magAxisHelpVectorY", errorInMethod);
      magAxisVarHelpAr = getNotNothingString(messagesAr, "magAxisVarHelp", errorInMethod);
      magAxisVarHelpGridAr = getNotNothingString(messagesAr, "magAxisVarHelpGrid", errorInMethod);
      magConstraintHelpAr = getNotNothingString(messagesAr, "magConstraintHelp", errorInMethod);
      magDocumentationAr = getNotNothingString(messagesAr, "magDocumentation", errorInMethod);
      magDownloadAr = getNotNothingString(messagesAr, "magDownload", errorInMethod);
      magDownloadTooltipAr = getNotNothingString(messagesAr, "magDownloadTooltip", errorInMethod);
      magFileTypeAr = getNotNothingString(messagesAr, "magFileType", errorInMethod);
      magGraphTypeAr = getNotNothingString(messagesAr, "magGraphType", errorInMethod);
      magGraphTypeTooltipGridAr =
          getNotNothingString(messagesAr, "magGraphTypeTooltipGrid", errorInMethod);
      magGraphTypeTooltipTableAr =
          getNotNothingString(messagesAr, "magGraphTypeTooltipTable", errorInMethod);
      magGSAr = getNotNothingString(messagesAr, "magGS", errorInMethod);
      magGSMarkerTypeAr = getNotNothingString(messagesAr, "magGSMarkerType", errorInMethod);
      magGSSizeAr = getNotNothingString(messagesAr, "magGSSize", errorInMethod);
      magGSColorAr = getNotNothingString(messagesAr, "magGSColor", errorInMethod);
      magGSColorBarAr = getNotNothingString(messagesAr, "magGSColorBar", errorInMethod);
      magGSColorBarTooltipAr =
          getNotNothingString(messagesAr, "magGSColorBarTooltip", errorInMethod);
      magGSContinuityAr = getNotNothingString(messagesAr, "magGSContinuity", errorInMethod);
      magGSContinuityTooltipAr =
          getNotNothingString(messagesAr, "magGSContinuityTooltip", errorInMethod);
      magGSScaleAr = getNotNothingString(messagesAr, "magGSScale", errorInMethod);
      magGSScaleTooltipAr = getNotNothingString(messagesAr, "magGSScaleTooltip", errorInMethod);
      magGSMinAr = getNotNothingString(messagesAr, "magGSMin", errorInMethod);
      magGSMinTooltipAr = getNotNothingString(messagesAr, "magGSMinTooltip", errorInMethod);
      magGSMaxAr = getNotNothingString(messagesAr, "magGSMax", errorInMethod);
      magGSMaxTooltipAr = getNotNothingString(messagesAr, "magGSMaxTooltip", errorInMethod);
      magGSNSectionsAr = getNotNothingString(messagesAr, "magGSNSections", errorInMethod);
      magGSNSectionsTooltipAr =
          getNotNothingString(messagesAr, "magGSNSectionsTooltip", errorInMethod);
      magGSLandMaskAr = getNotNothingString(messagesAr, "magGSLandMask", errorInMethod);
      magGSLandMaskTooltipGridAr =
          getNotNothingString(messagesAr, "magGSLandMaskTooltipGrid", errorInMethod);
      magGSLandMaskTooltipTableAr =
          getNotNothingString(messagesAr, "magGSLandMaskTooltipTable", errorInMethod);
      magGSVectorStandardAr = getNotNothingString(messagesAr, "magGSVectorStandard", errorInMethod);
      magGSVectorStandardTooltipAr =
          getNotNothingString(messagesAr, "magGSVectorStandardTooltip", errorInMethod);
      magGSYAscendingTooltipAr =
          getNotNothingString(messagesAr, "magGSYAscendingTooltip", errorInMethod);
      magGSYAxisMinAr = getNotNothingString(messagesAr, "magGSYAxisMin", errorInMethod);
      magGSYAxisMaxAr = getNotNothingString(messagesAr, "magGSYAxisMax", errorInMethod);
      magGSYRangeMinTooltipAr =
          getNotNothingString(messagesAr, "magGSYRangeMinTooltip", errorInMethod);
      magGSYRangeMaxTooltipAr =
          getNotNothingString(messagesAr, "magGSYRangeMaxTooltip", errorInMethod);
      magGSYRangeTooltipAr = getNotNothingString(messagesAr, "magGSYRangeTooltip", errorInMethod);
      magGSYScaleTooltipAr = getNotNothingString(messagesAr, "magGSYScaleTooltip", errorInMethod);
      magItemFirstAr = getNotNothingString(messagesAr, "magItemFirst", errorInMethod);
      magItemPreviousAr = getNotNothingString(messagesAr, "magItemPrevious", errorInMethod);
      magItemNextAr = getNotNothingString(messagesAr, "magItemNext", errorInMethod);
      magItemLastAr = getNotNothingString(messagesAr, "magItemLast", errorInMethod);
      magJust1ValueAr = getNotNothingString(messagesAr, "magJust1Value", errorInMethod);
      magRangeAr = getNotNothingString(messagesAr, "magRange", errorInMethod);
      magRangeToAr = getNotNothingString(messagesAr, "magRangeTo", errorInMethod);
      magRedrawAr = getNotNothingString(messagesAr, "magRedraw", errorInMethod);
      magRedrawTooltipAr = getNotNothingString(messagesAr, "magRedrawTooltip", errorInMethod);
      magTimeRangeAr = getNotNothingString(messagesAr, "magTimeRange", errorInMethod);
      magTimeRangeFirstAr = getNotNothingString(messagesAr, "magTimeRangeFirst", errorInMethod);
      magTimeRangeBackAr = getNotNothingString(messagesAr, "magTimeRangeBack", errorInMethod);
      magTimeRangeForwardAr = getNotNothingString(messagesAr, "magTimeRangeForward", errorInMethod);
      magTimeRangeLastAr = getNotNothingString(messagesAr, "magTimeRangeLast", errorInMethod);
      magTimeRangeTooltipAr = getNotNothingString(messagesAr, "magTimeRangeTooltip", errorInMethod);
      magTimeRangeTooltip2Ar =
          getNotNothingString(messagesAr, "magTimeRangeTooltip2", errorInMethod);
      magTimesVaryAr = getNotNothingString(messagesAr, "magTimesVary", errorInMethod);
      magViewUrlAr = getNotNothingString(messagesAr, "magViewUrl", errorInMethod);
      magZoomAr = getNotNothingString(messagesAr, "magZoom", errorInMethod);
      magZoomCenterAr = getNotNothingString(messagesAr, "magZoomCenter", errorInMethod);
      magZoomCenterTooltipAr =
          getNotNothingString(messagesAr, "magZoomCenterTooltip", errorInMethod);
      magZoomInAr = getNotNothingString(messagesAr, "magZoomIn", errorInMethod);
      magZoomInTooltipAr = getNotNothingString(messagesAr, "magZoomInTooltip", errorInMethod);
      magZoomOutAr = getNotNothingString(messagesAr, "magZoomOut", errorInMethod);
      magZoomOutTooltipAr = getNotNothingString(messagesAr, "magZoomOutTooltip", errorInMethod);
      magZoomALittleAr = getNotNothingString(messagesAr, "magZoomALittle", errorInMethod);
      magZoomDataAr = getNotNothingString(messagesAr, "magZoomData", errorInMethod);
      magZoomOutDataAr = getNotNothingString(messagesAr, "magZoomOutData", errorInMethod);
      magGridTooltipAr = getNotNothingString(messagesAr, "magGridTooltip", errorInMethod);
      magTableTooltipAr = getNotNothingString(messagesAr, "magTableTooltip", errorInMethod);

      Math2.memory = messagesAr[0].getNotNothingString("memory", errorInMethod);
      Math2.memoryTooMuchData =
          messagesAr[0].getNotNothingString("memoryTooMuchData", errorInMethod);
      Math2.memoryArraySize = messagesAr[0].getNotNothingString("memoryArraySize", errorInMethod);
      Math2.memoryThanCurrentlySafe =
          messagesAr[0].getNotNothingString("memoryThanCurrentlySafe", errorInMethod);
      Math2.memoryThanSafe = messagesAr[0].getNotNothingString("memoryThanSafe", errorInMethod);

      metadataDownloadAr = getNotNothingString(messagesAr, "metadataDownload", errorInMethod);
      moreInformationAr = getNotNothingString(messagesAr, "moreInformation", errorInMethod);

      MustBe.THERE_IS_NO_DATA =
          messagesAr[0].getNotNothingString("MustBeThereIsNoData", errorInMethod);
      MustBe.NotNull = messagesAr[0].getNotNothingString("MustBeNotNull", errorInMethod);
      MustBe.NotEmpty = messagesAr[0].getNotNothingString("MustBeNotEmpty", errorInMethod);
      MustBe.InternalError =
          messagesAr[0].getNotNothingString("MustBeInternalError", errorInMethod);
      MustBe.OutOfMemoryError =
          messagesAr[0].getNotNothingString("MustBeOutOfMemoryError", errorInMethod);

      nMatching1Ar = getNotNothingString(messagesAr, "nMatching1", errorInMethod);
      nMatchingAr = getNotNothingString(messagesAr, "nMatching", errorInMethod);
      nMatchingAlphabeticalAr =
          getNotNothingString(messagesAr, "nMatchingAlphabetical", errorInMethod);
      nMatchingMostRelevantAr =
          getNotNothingString(messagesAr, "nMatchingMostRelevant", errorInMethod);
      nMatchingPageAr = getNotNothingString(messagesAr, "nMatchingPage", errorInMethod);
      nMatchingCurrentAr = getNotNothingString(messagesAr, "nMatchingCurrent", errorInMethod);
      noDataFixedValueAr = getNotNothingString(messagesAr, "noDataFixedValue", errorInMethod);
      noDataNoLLAr = getNotNothingString(messagesAr, "noDataNoLL", errorInMethod);
      noDatasetWithAr = getNotNothingString(messagesAr, "noDatasetWith", errorInMethod);
      noPage1Ar = getNotNothingString(messagesAr, "noPage1", errorInMethod);
      noPage2Ar = getNotNothingString(messagesAr, "noPage2", errorInMethod);
      notAllowedAr = getNotNothingString(messagesAr, "notAllowed", errorInMethod);
      notAuthorizedAr = getNotNothingString(messagesAr, "notAuthorized", errorInMethod);
      notAuthorizedForDataAr =
          getNotNothingString(messagesAr, "notAuthorizedForData", errorInMethod);
      notAvailableAr = getNotNothingString(messagesAr, "notAvailable", errorInMethod);
      noteAr = getNotNothingString(messagesAr, "note", errorInMethod);
      noXxxAr = getNotNothingString(messagesAr, "noXxx", errorInMethod);
      noXxxBecauseAr = getNotNothingString(messagesAr, "noXxxBecause", errorInMethod);
      noXxxBecause2Ar = getNotNothingString(messagesAr, "noXxxBecause2", errorInMethod);
      noXxxNotActiveAr = getNotNothingString(messagesAr, "noXxxNotActive", errorInMethod);
      noXxxNoAxis1Ar = getNotNothingString(messagesAr, "noXxxNoAxis1", errorInMethod);
      noXxxNoCdmDataTypeAr = getNotNothingString(messagesAr, "noXxxNoCdmDataType", errorInMethod);
      noXxxNoColorBarAr = getNotNothingString(messagesAr, "noXxxNoColorBar", errorInMethod);
      noXxxNoLLAr = getNotNothingString(messagesAr, "noXxxNoLL", errorInMethod);
      noXxxNoLLEvenlySpacedAr =
          getNotNothingString(messagesAr, "noXxxNoLLEvenlySpaced", errorInMethod);
      noXxxNoLLGt1Ar = getNotNothingString(messagesAr, "noXxxNoLLGt1", errorInMethod);
      noXxxNoLLTAr = getNotNothingString(messagesAr, "noXxxNoLLT", errorInMethod);
      noXxxNoLonIn180Ar = getNotNothingString(messagesAr, "noXxxNoLonIn180", errorInMethod);
      noXxxNoNonStringAr = getNotNothingString(messagesAr, "noXxxNoNonString", errorInMethod);
      noXxxNo2NonStringAr = getNotNothingString(messagesAr, "noXxxNo2NonString", errorInMethod);
      noXxxNoStationAr = getNotNothingString(messagesAr, "noXxxNoStation", errorInMethod);
      noXxxNoStationIDAr = getNotNothingString(messagesAr, "noXxxNoStationID", errorInMethod);
      noXxxNoSubsetVariablesAr =
          getNotNothingString(messagesAr, "noXxxNoSubsetVariables", errorInMethod);
      noXxxNoOLLSubsetVariablesAr =
          getNotNothingString(messagesAr, "noXxxNoOLLSubsetVariables", errorInMethod);
      noXxxNoMinMaxAr = getNotNothingString(messagesAr, "noXxxNoMinMax", errorInMethod);
      noXxxItsGriddedAr = getNotNothingString(messagesAr, "noXxxItsGridded", errorInMethod);
      noXxxItsTabularAr = getNotNothingString(messagesAr, "noXxxItsTabular", errorInMethod);
      oneRequestAtATimeAr = getNotNothingString(messagesAr, "oneRequestAtATime", errorInMethod);
      openSearchDescriptionAr =
          getNotNothingString(messagesAr, "openSearchDescription", errorInMethod);
      optionalAr = getNotNothingString(messagesAr, "optional", errorInMethod);
      optionsAr = getNotNothingString(messagesAr, "options", errorInMethod);
      orAListOfValuesAr = getNotNothingString(messagesAr, "orAListOfValues", errorInMethod);
      orRefineSearchWithAr = getNotNothingString(messagesAr, "orRefineSearchWith", errorInMethod);
      orSearchWithAr = getNotNothingString(messagesAr, "orSearchWith", errorInMethod);
      orCommaAr = getNotNothingString(messagesAr, "orComma", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) {
        orRefineSearchWithAr[tl] += " ";
        orSearchWithAr[tl] += " ";
        orCommaAr[tl] += " ";
      }
      otherFeaturesAr = getNotNothingString(messagesAr, "otherFeatures", errorInMethod);
      outOfDateDatasetsAr = getNotNothingString(messagesAr, "outOfDateDatasets", errorInMethod);
      outOfDateHtmlAr = getNotNothingString(messagesAr, "outOfDateHtml", errorInMethod);
      outOfDateKeepTrackAr = getNotNothingString(messagesAr, "outOfDateKeepTrack", errorInMethod);

      // just one set of palettes info (from messagesAr[0])
      palettes = String2.split(messagesAr[0].getNotNothingString("palettes", errorInMethod), ',');
      DEFAULT_palettes = palettes; // used by LoadDatasets if palettes tag is empty
      DEFAULT_palettes_set = String2.stringArrayToSet(palettes);
      palettes0 = new String[palettes.length + 1];
      palettes0[0] = "";
      System.arraycopy(palettes, 0, palettes0, 1, palettes.length);

      patientDataAr = getNotNothingString(messagesAr, "patientData", errorInMethod);
      patientYourGraphAr = getNotNothingString(messagesAr, "patientYourGraph", errorInMethod);

      pdfWidths =
          String2.toIntArray(
              String2.split(messagesAr[0].getNotNothingString("pdfWidths", errorInMethod), ','));
      pdfHeights =
          String2.toIntArray(
              String2.split(messagesAr[0].getNotNothingString("pdfHeights", errorInMethod), ','));

      percentEncodeAr = getNotNothingString(messagesAr, "percentEncode", errorInMethod);
      pickADatasetAr = getNotNothingString(messagesAr, "pickADataset", errorInMethod);
      protocolSearchHtmlAr = getNotNothingString(messagesAr, "protocolSearchHtml", errorInMethod);
      protocolSearch2HtmlAr = getNotNothingString(messagesAr, "protocolSearch2Html", errorInMethod);
      protocolClickAr = getNotNothingString(messagesAr, "protocolClick", errorInMethod);
      queryErrorAr = getNotNothingString(messagesAr, "queryError", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) queryErrorAr[tl] += " ";
      queryError180Ar = getNotNothingString(messagesAr, "queryError180", errorInMethod);
      queryError1ValueAr = getNotNothingString(messagesAr, "queryError1Value", errorInMethod);
      queryError1VarAr = getNotNothingString(messagesAr, "queryError1Var", errorInMethod);
      queryError2VarAr = getNotNothingString(messagesAr, "queryError2Var", errorInMethod);
      queryErrorActualRangeAr =
          getNotNothingString(messagesAr, "queryErrorActualRange", errorInMethod);
      queryErrorAdjustedAr = getNotNothingString(messagesAr, "queryErrorAdjusted", errorInMethod);
      queryErrorAscendingAr = getNotNothingString(messagesAr, "queryErrorAscending", errorInMethod);
      queryErrorConstraintNaNAr =
          getNotNothingString(messagesAr, "queryErrorConstraintNaN", errorInMethod);
      queryErrorEqualSpacingAr =
          getNotNothingString(messagesAr, "queryErrorEqualSpacing", errorInMethod);
      queryErrorExpectedAtAr =
          getNotNothingString(messagesAr, "queryErrorExpectedAt", errorInMethod);
      queryErrorFileTypeAr = getNotNothingString(messagesAr, "queryErrorFileType", errorInMethod);
      queryErrorInvalidAr = getNotNothingString(messagesAr, "queryErrorInvalid", errorInMethod);
      queryErrorLLAr = getNotNothingString(messagesAr, "queryErrorLL", errorInMethod);
      queryErrorLLGt1Ar = getNotNothingString(messagesAr, "queryErrorLLGt1", errorInMethod);
      queryErrorLLTAr = getNotNothingString(messagesAr, "queryErrorLLT", errorInMethod);
      queryErrorNeverTrueAr = getNotNothingString(messagesAr, "queryErrorNeverTrue", errorInMethod);
      queryErrorNeverBothTrueAr =
          getNotNothingString(messagesAr, "queryErrorNeverBothTrue", errorInMethod);
      queryErrorNotAxisAr = getNotNothingString(messagesAr, "queryErrorNotAxis", errorInMethod);
      queryErrorNotExpectedAtAr =
          getNotNothingString(messagesAr, "queryErrorNotExpectedAt", errorInMethod);
      queryErrorNotFoundAfterAr =
          getNotNothingString(messagesAr, "queryErrorNotFoundAfter", errorInMethod);
      queryErrorOccursTwiceAr =
          getNotNothingString(messagesAr, "queryErrorOccursTwice", errorInMethod);

      queryErrorOrderByClosestAr =
          getNotNothingString(messagesAr, "queryErrorOrderByClosest", errorInMethod);
      queryErrorOrderByLimitAr =
          getNotNothingString(messagesAr, "queryErrorOrderByLimit", errorInMethod);
      queryErrorOrderByMeanAr =
          getNotNothingString(messagesAr, "queryErrorOrderByMean", errorInMethod);
      queryErrorOrderBySumAr =
          getNotNothingString(messagesAr, "queryErrorOrderBySum", errorInMethod);

      queryErrorOrderByVariableAr =
          getNotNothingString(messagesAr, "queryErrorOrderByVariable", errorInMethod);
      queryErrorUnknownVariableAr =
          getNotNothingString(messagesAr, "queryErrorUnknownVariable", errorInMethod);

      queryErrorGrid1AxisAr = getNotNothingString(messagesAr, "queryErrorGrid1Axis", errorInMethod);
      queryErrorGridAmpAr = getNotNothingString(messagesAr, "queryErrorGridAmp", errorInMethod);
      queryErrorGridDiagnosticAr =
          getNotNothingString(messagesAr, "queryErrorGridDiagnostic", errorInMethod);
      queryErrorGridBetweenAr =
          getNotNothingString(messagesAr, "queryErrorGridBetween", errorInMethod);
      queryErrorGridLessMinAr =
          getNotNothingString(messagesAr, "queryErrorGridLessMin", errorInMethod);
      queryErrorGridGreaterMaxAr =
          getNotNothingString(messagesAr, "queryErrorGridGreaterMax", errorInMethod);
      queryErrorGridMissingAr =
          getNotNothingString(messagesAr, "queryErrorGridMissing", errorInMethod);
      queryErrorGridNoAxisVarAr =
          getNotNothingString(messagesAr, "queryErrorGridNoAxisVar", errorInMethod);
      queryErrorGridNoDataVarAr =
          getNotNothingString(messagesAr, "queryErrorGridNoDataVar", errorInMethod);
      queryErrorGridNotIdenticalAr =
          getNotNothingString(messagesAr, "queryErrorGridNotIdentical", errorInMethod);
      queryErrorGridSLessSAr =
          getNotNothingString(messagesAr, "queryErrorGridSLessS", errorInMethod);
      queryErrorLastEndPAr = getNotNothingString(messagesAr, "queryErrorLastEndP", errorInMethod);
      queryErrorLastExpectedAr =
          getNotNothingString(messagesAr, "queryErrorLastExpected", errorInMethod);
      queryErrorLastUnexpectedAr =
          getNotNothingString(messagesAr, "queryErrorLastUnexpected", errorInMethod);
      queryErrorLastPMInvalidAr =
          getNotNothingString(messagesAr, "queryErrorLastPMInvalid", errorInMethod);
      queryErrorLastPMIntegerAr =
          getNotNothingString(messagesAr, "queryErrorLastPMInteger", errorInMethod);

      questionMarkImageFile =
          messagesAr[0].getNotNothingString("questionMarkImageFile", errorInMethod);
      questionMarkImageFile =
          getSetupEVString(setup, ev, "questionMarkImageFile", questionMarkImageFile); // optional

      rangesFromToAr = getNotNothingString(messagesAr, "rangesFromTo", errorInMethod);
      requiredAr = getNotNothingString(messagesAr, "required", errorInMethod);
      requestFormatExamplesHtmlAr =
          getNotNothingString(messagesAr, "requestFormatExamplesHtml", errorInMethod);
      resetTheFormAr = getNotNothingString(messagesAr, "resetTheForm", errorInMethod);
      resetTheFormWasAr = getNotNothingString(messagesAr, "resetTheFormWas", errorInMethod);
      resourceNotFoundAr = getNotNothingString(messagesAr, "resourceNotFound", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) resourceNotFoundAr[tl] += " ";
      restfulWebServicesAr = getNotNothingString(messagesAr, "restfulWebServices", errorInMethod);
      restfulHTMLAr = getNotNothingString(messagesAr, "restfulHTML", errorInMethod);
      restfulHTMLContinuedAr =
          getNotNothingString(messagesAr, "restfulHTMLContinued", errorInMethod);
      restfulGetAllDatasetAr =
          getNotNothingString(messagesAr, "restfulGetAllDataset", errorInMethod);
      restfulProtocolsAr = getNotNothingString(messagesAr, "restfulProtocols", errorInMethod);
      SOSDocumentationAr = getNotNothingString(messagesAr, "SOSDocumentation", errorInMethod);
      WCSDocumentationAr = getNotNothingString(messagesAr, "WCSDocumentation", errorInMethod);
      WMSDocumentationAr = getNotNothingString(messagesAr, "WMSDocumentation", errorInMethod);
      resultsFormatExamplesHtmlAr =
          getNotNothingString(messagesAr, "resultsFormatExamplesHtml", errorInMethod);
      resultsOfSearchForAr = getNotNothingString(messagesAr, "resultsOfSearchFor", errorInMethod);
      restfulInformationFormatsAr =
          getNotNothingString(messagesAr, "restfulInformationFormats", errorInMethod);
      restfulViaServiceAr = getNotNothingString(messagesAr, "restfulViaService", errorInMethod);
      rowsAr = getNotNothingString(messagesAr, "rows", errorInMethod);
      rssNoAr = getNotNothingString(messagesAr, "rssNo", errorInMethod);
      searchTitleAr = getNotNothingString(messagesAr, "searchTitle", errorInMethod);
      searchDoFullTextHtmlAr =
          getNotNothingString(messagesAr, "searchDoFullTextHtml", errorInMethod);
      searchFullTextHtmlAr = getNotNothingString(messagesAr, "searchFullTextHtml", errorInMethod);
      searchButtonAr = getNotNothingString(messagesAr, "searchButton", errorInMethod);
      searchClickTipAr = getNotNothingString(messagesAr, "searchClickTip", errorInMethod);
      searchHintsLuceneTooltipAr =
          getNotNothingString(messagesAr, "searchHintsLuceneTooltip", errorInMethod);
      searchHintsOriginalTooltipAr =
          getNotNothingString(messagesAr, "searchHintsOriginalTooltip", errorInMethod);
      searchHintsTooltipAr = getNotNothingString(messagesAr, "searchHintsTooltip", errorInMethod);
      searchMultipleERDDAPsAr =
          getNotNothingString(messagesAr, "searchMultipleERDDAPs", errorInMethod);
      searchMultipleERDDAPsDescriptionAr =
          getNotNothingString(messagesAr, "searchMultipleERDDAPsDescription", errorInMethod);
      searchNotAvailableAr = getNotNothingString(messagesAr, "searchNotAvailable", errorInMethod);
      searchTipAr = getNotNothingString(messagesAr, "searchTip", errorInMethod);
      searchSpellingAr = getNotNothingString(messagesAr, "searchSpelling", errorInMethod);
      searchFewerWordsAr = getNotNothingString(messagesAr, "searchFewerWords", errorInMethod);
      searchWithQueryAr = getNotNothingString(messagesAr, "searchWithQuery", errorInMethod);
      selectNextAr = getNotNothingString(messagesAr, "selectNext", errorInMethod);
      selectPreviousAr = getNotNothingString(messagesAr, "selectPrevious", errorInMethod);
      shiftXAllTheWayLeftAr = getNotNothingString(messagesAr, "shiftXAllTheWayLeft", errorInMethod);
      shiftXLeftAr = getNotNothingString(messagesAr, "shiftXLeft", errorInMethod);
      shiftXRightAr = getNotNothingString(messagesAr, "shiftXRight", errorInMethod);
      shiftXAllTheWayRightAr =
          getNotNothingString(messagesAr, "shiftXAllTheWayRight", errorInMethod);

      Attributes.signedToUnsignedAttNames =
          StringArray.arrayFromCSV(
              messagesAr[0].getNotNothingString("signedToUnsignedAttNames", errorInMethod));

      seeProtocolDocumentationAr =
          getNotNothingString(messagesAr, "seeProtocolDocumentation", errorInMethod);

      slideSorterAr = getNotNothingString(messagesAr, "slideSorter", errorInMethod);
      SOSAr = getNotNothingString(messagesAr, "SOS", errorInMethod);
      sosDescriptionHtmlAr = getNotNothingString(messagesAr, "sosDescriptionHtml", errorInMethod);
      sosLongDescriptionHtmlAr =
          getNotNothingString(messagesAr, "sosLongDescriptionHtml", errorInMethod);
      sosOverview1Ar = getNotNothingString(messagesAr, "sosOverview1", errorInMethod);
      sosOverview2Ar = getNotNothingString(messagesAr, "sosOverview2", errorInMethod);
      sparqlP01toP02pre = messagesAr[0].getNotNothingString("sparqlP01toP02pre", errorInMethod);
      sparqlP01toP02post = messagesAr[0].getNotNothingString("sparqlP01toP02post", errorInMethod);
      ssUseAr = getNotNothingString(messagesAr, "ssUse", errorInMethod);
      ssUsePlainAr = getNotNothingString(messagesAr, "ssUse", errorInMethod); // start with this
      for (int tl = 0; tl < nLanguages; tl++)
        ssUsePlainAr[tl] = XML.removeHTMLTags(ssUsePlainAr[tl]);

      ssBePatientAr = getNotNothingString(messagesAr, "ssBePatient", errorInMethod);
      ssInstructionsHtmlAr = getNotNothingString(messagesAr, "ssInstructionsHtml", errorInMethod);

      statusAr = getNotNothingString(messagesAr, "status", errorInMethod);
      statusHtmlAr = getNotNothingString(messagesAr, "statusHtml", errorInMethod);
      submitAr = getNotNothingString(messagesAr, "submit", errorInMethod);
      submitTooltipAr = getNotNothingString(messagesAr, "submitTooltip", errorInMethod);
      subscriptionOfferRssAr =
          getNotNothingString(messagesAr, "subscriptionOfferRss", errorInMethod);
      subscriptionOfferUrlAr =
          getNotNothingString(messagesAr, "subscriptionOfferUrl", errorInMethod);
      subscriptionsTitleAr = getNotNothingString(messagesAr, "subscriptionsTitle", errorInMethod);
      subscriptionEmailListAr =
          getNotNothingString(messagesAr, "subscriptionEmailList", errorInMethod);
      subscriptionAddAr = getNotNothingString(messagesAr, "subscriptionAdd", errorInMethod);
      subscriptionValidateAr =
          getNotNothingString(messagesAr, "subscriptionValidate", errorInMethod);
      subscriptionListAr = getNotNothingString(messagesAr, "subscriptionList", errorInMethod);
      subscriptionRemoveAr = getNotNothingString(messagesAr, "subscriptionRemove", errorInMethod);
      subscription0HtmlAr = getNotNothingString(messagesAr, "subscription0Html", errorInMethod);
      subscription1HtmlAr = getNotNothingString(messagesAr, "subscription1Html", errorInMethod);
      subscription2HtmlAr = getNotNothingString(messagesAr, "subscription2Html", errorInMethod);
      subscriptionAbuseAr = getNotNothingString(messagesAr, "subscriptionAbuse", errorInMethod);
      subscriptionAddErrorAr =
          getNotNothingString(messagesAr, "subscriptionAddError", errorInMethod);
      subscriptionAddHtmlAr = getNotNothingString(messagesAr, "subscriptionAddHtml", errorInMethod);
      subscriptionAdd2Ar = getNotNothingString(messagesAr, "subscriptionAdd2", errorInMethod);
      subscriptionAddSuccessAr =
          getNotNothingString(messagesAr, "subscriptionAddSuccess", errorInMethod);
      subscriptionEmailAr = getNotNothingString(messagesAr, "subscriptionEmail", errorInMethod);
      subscriptionEmailOnBlacklistAr =
          getNotNothingString(messagesAr, "subscriptionEmailOnBlacklist", errorInMethod);
      subscriptionEmailInvalidAr =
          getNotNothingString(messagesAr, "subscriptionEmailInvalid", errorInMethod);
      subscriptionEmailTooLongAr =
          getNotNothingString(messagesAr, "subscriptionEmailTooLong", errorInMethod);
      subscriptionEmailUnspecifiedAr =
          getNotNothingString(messagesAr, "subscriptionEmailUnspecified", errorInMethod);
      subscriptionIDInvalidAr =
          getNotNothingString(messagesAr, "subscriptionIDInvalid", errorInMethod);
      subscriptionIDTooLongAr =
          getNotNothingString(messagesAr, "subscriptionIDTooLong", errorInMethod);
      subscriptionIDUnspecifiedAr =
          getNotNothingString(messagesAr, "subscriptionIDUnspecified", errorInMethod);
      subscriptionKeyInvalidAr =
          getNotNothingString(messagesAr, "subscriptionKeyInvalid", errorInMethod);
      subscriptionKeyUnspecifiedAr =
          getNotNothingString(messagesAr, "subscriptionKeyUnspecified", errorInMethod);
      subscriptionListErrorAr =
          getNotNothingString(messagesAr, "subscriptionListError", errorInMethod);
      subscriptionListHtmlAr =
          getNotNothingString(messagesAr, "subscriptionListHtml", errorInMethod);
      subscriptionListSuccessAr =
          getNotNothingString(messagesAr, "subscriptionListSuccess", errorInMethod);
      subscriptionRemoveErrorAr =
          getNotNothingString(messagesAr, "subscriptionRemoveError", errorInMethod);
      subscriptionRemoveHtmlAr =
          getNotNothingString(messagesAr, "subscriptionRemoveHtml", errorInMethod);
      subscriptionRemove2Ar = getNotNothingString(messagesAr, "subscriptionRemove2", errorInMethod);
      subscriptionRemoveSuccessAr =
          getNotNothingString(messagesAr, "subscriptionRemoveSuccess", errorInMethod);
      subscriptionRSSAr = getNotNothingString(messagesAr, "subscriptionRSS", errorInMethod);
      subscriptionsNotAvailableAr =
          getNotNothingString(messagesAr, "subscriptionsNotAvailable", errorInMethod);
      subscriptionUrlHtmlAr = getNotNothingString(messagesAr, "subscriptionUrlHtml", errorInMethod);
      subscriptionUrlInvalidAr =
          getNotNothingString(messagesAr, "subscriptionUrlInvalid", errorInMethod);
      subscriptionUrlTooLongAr =
          getNotNothingString(messagesAr, "subscriptionUrlTooLong", errorInMethod);
      subscriptionValidateErrorAr =
          getNotNothingString(messagesAr, "subscriptionValidateError", errorInMethod);
      subscriptionValidateHtmlAr =
          getNotNothingString(messagesAr, "subscriptionValidateHtml", errorInMethod);
      subscriptionValidateSuccessAr =
          getNotNothingString(messagesAr, "subscriptionValidateSuccess", errorInMethod);
      subsetAr = getNotNothingString(messagesAr, "subset", errorInMethod);
      subsetSelectAr = getNotNothingString(messagesAr, "subsetSelect", errorInMethod);
      subsetNMatchingAr = getNotNothingString(messagesAr, "subsetNMatching", errorInMethod);
      subsetInstructionsAr = getNotNothingString(messagesAr, "subsetInstructions", errorInMethod);
      subsetOptionAr = getNotNothingString(messagesAr, "subsetOption", errorInMethod);
      subsetOptionsAr = getNotNothingString(messagesAr, "subsetOptions", errorInMethod);
      subsetRefineMapDownloadAr =
          getNotNothingString(messagesAr, "subsetRefineMapDownload", errorInMethod);
      subsetRefineSubsetDownloadAr =
          getNotNothingString(messagesAr, "subsetRefineSubsetDownload", errorInMethod);
      subsetClickResetClosestAr =
          getNotNothingString(messagesAr, "subsetClickResetClosest", errorInMethod);
      subsetClickResetLLAr = getNotNothingString(messagesAr, "subsetClickResetLL", errorInMethod);
      subsetMetadataAr = getNotNothingString(messagesAr, "subsetMetadata", errorInMethod);
      subsetCountAr = getNotNothingString(messagesAr, "subsetCount", errorInMethod);
      subsetPercentAr = getNotNothingString(messagesAr, "subsetPercent", errorInMethod);
      subsetViewSelectAr = getNotNothingString(messagesAr, "subsetViewSelect", errorInMethod);
      subsetViewSelectDistinctCombosAr =
          getNotNothingString(messagesAr, "subsetViewSelectDistinctCombos", errorInMethod);
      subsetViewSelectRelatedCountsAr =
          getNotNothingString(messagesAr, "subsetViewSelectRelatedCounts", errorInMethod);
      subsetWhenAr = getNotNothingString(messagesAr, "subsetWhen", errorInMethod);
      subsetWhenNoConstraintsAr =
          getNotNothingString(messagesAr, "subsetWhenNoConstraints", errorInMethod);
      subsetWhenCountsAr = getNotNothingString(messagesAr, "subsetWhenCounts", errorInMethod);
      subsetComboClickSelectAr =
          getNotNothingString(messagesAr, "subsetComboClickSelect", errorInMethod);
      subsetNVariableCombosAr =
          getNotNothingString(messagesAr, "subsetNVariableCombos", errorInMethod);
      subsetShowingAllRowsAr =
          getNotNothingString(messagesAr, "subsetShowingAllRows", errorInMethod);
      subsetShowingNRowsAr = getNotNothingString(messagesAr, "subsetShowingNRows", errorInMethod);
      subsetChangeShowingAr = getNotNothingString(messagesAr, "subsetChangeShowing", errorInMethod);
      subsetNRowsRelatedDataAr =
          getNotNothingString(messagesAr, "subsetNRowsRelatedData", errorInMethod);
      subsetViewRelatedChangeAr =
          getNotNothingString(messagesAr, "subsetViewRelatedChange", errorInMethod);
      subsetTotalCountAr = getNotNothingString(messagesAr, "subsetTotalCount", errorInMethod);
      subsetViewAr = getNotNothingString(messagesAr, "subsetView", errorInMethod);
      subsetViewCheckAr = getNotNothingString(messagesAr, "subsetViewCheck", errorInMethod);
      subsetViewCheck1Ar = getNotNothingString(messagesAr, "subsetViewCheck1", errorInMethod);
      subsetViewDistinctMapAr =
          getNotNothingString(messagesAr, "subsetViewDistinctMap", errorInMethod);
      subsetViewRelatedMapAr =
          getNotNothingString(messagesAr, "subsetViewRelatedMap", errorInMethod);
      subsetViewDistinctDataCountsAr =
          getNotNothingString(messagesAr, "subsetViewDistinctDataCounts", errorInMethod);
      subsetViewDistinctDataAr =
          getNotNothingString(messagesAr, "subsetViewDistinctData", errorInMethod);
      subsetViewRelatedDataCountsAr =
          getNotNothingString(messagesAr, "subsetViewRelatedDataCounts", errorInMethod);
      subsetViewRelatedDataAr =
          getNotNothingString(messagesAr, "subsetViewRelatedData", errorInMethod);
      subsetViewDistinctMapTooltipAr =
          getNotNothingString(messagesAr, "subsetViewDistinctMapTooltip", errorInMethod);
      subsetViewRelatedMapTooltipAr =
          getNotNothingString(messagesAr, "subsetViewRelatedMapTooltip", errorInMethod);
      subsetViewDistinctDataCountsTooltipAr =
          getNotNothingString(messagesAr, "subsetViewDistinctDataCountsTooltip", errorInMethod);
      subsetViewDistinctDataTooltipAr =
          getNotNothingString(messagesAr, "subsetViewDistinctDataTooltip", errorInMethod);
      subsetViewRelatedDataCountsTooltipAr =
          getNotNothingString(messagesAr, "subsetViewRelatedDataCountsTooltip", errorInMethod);
      subsetViewRelatedDataTooltipAr =
          getNotNothingString(messagesAr, "subsetViewRelatedDataTooltip", errorInMethod);
      subsetWarnAr = getNotNothingString(messagesAr, "subsetWarn", errorInMethod);
      subsetWarn10000Ar = getNotNothingString(messagesAr, "subsetWarn10000", errorInMethod);
      subsetTooltipAr = getNotNothingString(messagesAr, "subsetTooltip", errorInMethod);
      subsetNotSetUpAr = getNotNothingString(messagesAr, "subsetNotSetUp", errorInMethod);
      subsetLongNotShownAr = getNotNothingString(messagesAr, "subsetLongNotShown", errorInMethod);

      tabledapVideoIntroAr = getNotNothingString(messagesAr, "tabledapVideoIntro", errorInMethod);
      theDatasetIDAr = getNotNothingString(messagesAr, "theDatasetID", errorInMethod);
      theKeyAr = getNotNothingString(messagesAr, "theKey", errorInMethod);
      theSubscriptionIDAr = getNotNothingString(messagesAr, "theSubscriptionID", errorInMethod);
      theUrlActionAr = getNotNothingString(messagesAr, "theUrlAction", errorInMethod);
      theLongDescriptionHtmlAr =
          getNotNothingString(messagesAr, "theLongDescriptionHtml", errorInMethod);
      timeAr = getNotNothingString(messagesAr, "time", errorInMethod);
      ThenAr = getNotNothingString(messagesAr, "Then", errorInMethod);
      thisParticularErddapAr =
          getNotNothingString(messagesAr, "thisParticularErddap", errorInMethod);
      timeoutOtherRequestsAr =
          getNotNothingString(messagesAr, "timeoutOtherRequests", errorInMethod);
      HtmlWidgets.twoClickMapDefaultTooltipAr =
          getNotNothingString(messagesAr, "twoClickMapDefaultTooltip", errorInMethod);

      unitsAr = getNotNothingString(messagesAr, "units", errorInMethod);
      unknownDatasetIDAr = getNotNothingString(messagesAr, "unknownDatasetID", errorInMethod);
      unknownProtocolAr = getNotNothingString(messagesAr, "unknownProtocol", errorInMethod);
      unsupportedFileTypeAr = getNotNothingString(messagesAr, "unsupportedFileType", errorInMethod);
      String tStandardizeUdunits[] =
          String2.split(
              messagesAr[0].getNotNothingString("standardizeUdunits", errorInMethod) + "\n",
              '\n'); // +\n\n since xml content is trimmed.
      String tUcumToUdunits[] =
          String2.split(
              messagesAr[0].getNotNothingString("ucumToUdunits", errorInMethod) + "\n",
              '\n'); // +\n\n since xml content is trimmed.
      String tUdunitsToUcum[] =
          String2.split(
              messagesAr[0].getNotNothingString("udunitsToUcum", errorInMethod) + "\n",
              '\n'); // +\n\n since xml content is trimmed.
      String tUpdateUrls[] =
          String2.split(
              messagesAr[0].getNotNothingString("updateUrls", errorInMethod) + "\n",
              '\n'); // +\n\n since xml content is trimmed.

      updateUrlsSkipAttributes =
          StringArray.arrayFromCSV(
              messagesAr[0].getNotNothingString("updateUrlsSkipAttributes", errorInMethod));

      usingGriddapAr = getNotNothingString(messagesAr, "usingGriddap", errorInMethod);
      usingTabledapAr = getNotNothingString(messagesAr, "usingTabledap", errorInMethod);
      variableNamesAr = getNotNothingString(messagesAr, "variableNames", errorInMethod);
      viewAllDatasetsHtmlAr = getNotNothingString(messagesAr, "viewAllDatasetsHtml", errorInMethod);
      waitThenTryAgainAr = getNotNothingString(messagesAr, "waitThenTryAgain", errorInMethod);
      gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.waitThenTryAgain =
          waitThenTryAgainAr[0];
      warningAr = getNotNothingString(messagesAr, "warning", errorInMethod);
      WCSAr = getNotNothingString(messagesAr, "WCS", errorInMethod);
      wcsDescriptionHtmlAr = getNotNothingString(messagesAr, "wcsDescriptionHtml", errorInMethod);
      wcsLongDescriptionHtmlAr =
          getNotNothingString(messagesAr, "wcsLongDescriptionHtml", errorInMethod);
      wcsOverview1Ar = getNotNothingString(messagesAr, "wcsOverview1", errorInMethod);
      wcsOverview2Ar = getNotNothingString(messagesAr, "wcsOverview2", errorInMethod);
      wmsDescriptionHtmlAr = getNotNothingString(messagesAr, "wmsDescriptionHtml", errorInMethod);
      wmsInstructionsAr = getNotNothingString(messagesAr, "wmsInstructions", errorInMethod);
      wmsLongDescriptionHtmlAr =
          getNotNothingString(messagesAr, "wmsLongDescriptionHtml", errorInMethod);
      wmsManyDatasetsAr = getNotNothingString(messagesAr, "wmsManyDatasets", errorInMethod);
      WMSDocumentation1Ar = getNotNothingString(messagesAr, "WMSDocumentation1", errorInMethod);
      WMSGetCapabilitiesAr = getNotNothingString(messagesAr, "WMSGetCapabilities", errorInMethod);
      WMSGetMapAr = getNotNothingString(messagesAr, "WMSGetMap", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) {
        WMSGetCapabilitiesAr[tl] =
            WMSGetCapabilitiesAr[tl] // some things should stay in English
                .replaceAll("&serviceWMS;", "service=WMS")
                .replaceAll("&version;", "version")
                .replaceAll("&requestGetCapabilities;", "request=GetCapabilities");
        WMSGetMapAr[tl] =
            WMSGetMapAr[tl] // lots of things should stay in English
                .replaceAll("&WMSSERVER;", EDD.WMS_SERVER)
                .replaceAll("&WMSSEPARATOR;", Character.toString(EDD.WMS_SEPARATOR))
                .replaceAll("&serviceWMS;", "service=WMS")
                .replaceAll("&version;", "version")
                .replaceAll("&requestGetMap;", "request=GetMap")
                .replaceAll("&TRUE;", "TRUE")
                .replaceAll("&FALSE;", "FALSE")
                .replaceAll("&layers;", "layers")
                .replaceAll("&styles;", "styles")
                .replaceAll("&width;", "width")
                .replaceAll("&height;", "height")
                .replaceAll("&format;", "format")
                .replaceAll("&transparentTRUEFALSE;", "transparent=<i>TRUE|FALSE</i>")
                .replaceAll("&bgcolor;", "bgcolor")
                .replaceAll("&exceptions;", "exceptions")
                .replaceAll("&time;", "time")
                .replaceAll("&elevation;", "elevation");
      }

      WMSNotesAr = getNotNothingString(messagesAr, "WMSNotes", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++)
        WMSNotesAr[tl] =
            WMSNotesAr[tl].replace("&WMSSEPARATOR;", Character.toString(EDD.WMS_SEPARATOR));

      yourEmailAddressAr = getNotNothingString(messagesAr, "yourEmailAddress", errorInMethod);
      zoomInAr = getNotNothingString(messagesAr, "zoomIn", errorInMethod);
      zoomOutAr = getNotNothingString(messagesAr, "zoomOut", errorInMethod);

      for (int tl = 0; tl < nLanguages; tl++) {
        blacklistMsgAr[tl] = MessageFormat.format(blacklistMsgAr[tl], adminEmail);
      }

      standardShortDescriptionHtmlAr =
          getNotNothingString(messagesAr, "standardShortDescriptionHtml", errorInMethod);
      for (int tl = 0; tl < nLanguages; tl++) {
        standardShortDescriptionHtmlAr[tl] =
            String2.replaceAll(
                standardShortDescriptionHtmlAr[tl],
                "&convertTimeReference;",
                convertersActive ? convertTimeReferenceAr[tl] : "");
        standardShortDescriptionHtmlAr[tl] =
            String2.replaceAll(
                standardShortDescriptionHtmlAr[tl],
                "&wmsManyDatasets;",
                wmsActive ? wmsManyDatasetsAr[tl] : "");
      }

      // just one
      DEFAULT_commonStandardNames =
          String2.canonical(
              StringArray.arrayFromCSV(
                  messagesAr[0].getNotNothingString("DEFAULT_commonStandardNames", errorInMethod)));
      commonStandardNames = DEFAULT_commonStandardNames;
      DEFAULT_standardLicense = messagesAr[0].getNotNothingString("standardLicense", errorInMethod);
      standardLicense = getSetupEVString(setup, ev, "standardLicense", DEFAULT_standardLicense);

      // [language]
      DEFAULT_standardContactAr = getNotNothingString(messagesAr, "standardContact", errorInMethod);
      standardContactAr = getSetupEVString(setup, ev, "standardContact", DEFAULT_standardContactAr);
      DEFAULT_standardDataLicensesAr =
          getNotNothingString(messagesAr, "standardDataLicenses", errorInMethod);
      standardDataLicensesAr =
          getSetupEVString(setup, ev, "standardDataLicenses", DEFAULT_standardDataLicensesAr);
      DEFAULT_standardDisclaimerOfExternalLinksAr =
          getNotNothingString(messagesAr, "standardDisclaimerOfExternalLinks", errorInMethod);
      standardDisclaimerOfExternalLinksAr =
          getSetupEVString(
              setup,
              ev,
              "standardDisclaimerOfExternalLinks",
              DEFAULT_standardDisclaimerOfExternalLinksAr);
      DEFAULT_standardDisclaimerOfEndorsementAr =
          getNotNothingString(messagesAr, "standardDisclaimerOfEndorsement", errorInMethod);
      standardDisclaimerOfEndorsementAr =
          getSetupEVString(
              setup,
              ev,
              "standardDisclaimerOfEndorsement",
              DEFAULT_standardDisclaimerOfEndorsementAr);
      DEFAULT_standardGeneralDisclaimerAr =
          getNotNothingString(messagesAr, "standardGeneralDisclaimer", errorInMethod);
      standardGeneralDisclaimerAr =
          getSetupEVString(
              setup, ev, "standardGeneralDisclaimer", DEFAULT_standardGeneralDisclaimerAr);
      DEFAULT_standardPrivacyPolicyAr =
          getNotNothingString(messagesAr, "standardPrivacyPolicy", errorInMethod);
      standardPrivacyPolicyAr =
          getSetupEVString(setup, ev, "standardPrivacyPolicy", DEFAULT_standardPrivacyPolicyAr);

      DEFAULT_startHeadHtml = messagesAr[0].getNotNothingString("startHeadHtml5", errorInMethod);
      startHeadHtml = getSetupEVString(setup, ev, "startHeadHtml5", DEFAULT_startHeadHtml);
      DEFAULT_startBodyHtmlAr = getNotNothingString(messagesAr, "startBodyHtml5", errorInMethod);
      startBodyHtmlAr = getSetupEVString(setup, ev, "startBodyHtml5", DEFAULT_startBodyHtmlAr);
      DEFAULT_theShortDescriptionHtmlAr =
          getNotNothingString(messagesAr, "theShortDescriptionHtml", errorInMethod);
      theShortDescriptionHtmlAr =
          getSetupEVString(setup, ev, "theShortDescriptionHtml", DEFAULT_theShortDescriptionHtmlAr);
      DEFAULT_endBodyHtmlAr = getNotNothingString(messagesAr, "endBodyHtml5", errorInMethod);
      endBodyHtmlAr = getSetupEVString(setup, ev, "endBodyHtml5", DEFAULT_endBodyHtmlAr);

      // ensure HTML5
      Test.ensureTrue(
          startHeadHtml.startsWith("<!DOCTYPE html>"),
          "<startHeadHtml5> must start with \"<!DOCTYPE html>\".");
      for (int tl = 0; tl < nLanguages; tl++) {
        DEFAULT_standardDataLicensesAr[tl] =
            String2.replaceAll(
                DEFAULT_standardDataLicensesAr[tl],
                "&license;",
                "<kbd>license</kbd>"); // so not translated
        standardDataLicensesAr[tl] =
            String2.replaceAll(standardDataLicensesAr[tl], "&license;", "<kbd>license</kbd>");
        standardContactAr[tl] =
            String2.replaceAll(
                standardContactAr[tl], "&adminEmail;", SSR.getSafeEmailAddress(adminEmail));
        startBodyHtmlAr[tl] =
            String2.replaceAll(startBodyHtmlAr[tl], "&erddapVersion;", erddapVersion);
        endBodyHtmlAr[tl] = String2.replaceAll(endBodyHtmlAr[tl], "&erddapVersion;", erddapVersion);
      }

      Test.ensureEqual(imageWidths.length, 3, "imageWidths.length must be 3.");
      Test.ensureEqual(imageHeights.length, 3, "imageHeights.length must be 3.");
      Test.ensureEqual(pdfWidths.length, 3, "pdfWidths.length must be 3.");
      Test.ensureEqual(pdfHeights.length, 3, "pdfHeights.length must be 3.");

      int nStandardizeUdunits = tStandardizeUdunits.length / 3;
      for (int i = 0; i < nStandardizeUdunits; i++) {
        int i3 = i * 3;
        Test.ensureTrue(
            String2.isSomething(tStandardizeUdunits[i3]),
            "standardizeUdunits line #" + (i3 + 0) + " is empty.");
        Test.ensureTrue(
            String2.isSomething(tStandardizeUdunits[i3 + 1]),
            "standardizeUdunits line #" + (i3 + 1) + " is empty.");
        Test.ensureEqual(
            tStandardizeUdunits[i3 + 2].trim(),
            "",
            "standardizeUdunits line #" + (i3 + 2) + " isn't empty.");
        Units2.standardizeUdunitsHM.put(
            String2.canonical(tStandardizeUdunits[i3].trim()),
            String2.canonical(tStandardizeUdunits[i3 + 1].trim()));
      }

      int nUcumToUdunits = tUcumToUdunits.length / 3;
      for (int i = 0; i < nUcumToUdunits; i++) {
        int i3 = i * 3;
        Test.ensureTrue(
            String2.isSomething(tUcumToUdunits[i3]),
            "ucumToUdunits line #" + (i3 + 0) + " is empty.");
        Test.ensureTrue(
            String2.isSomething(tUcumToUdunits[i3 + 1]),
            "ucumToUdunits line #" + (i3 + 1) + " is empty.");
        Test.ensureEqual(
            tUcumToUdunits[i3 + 2].trim(), "", "ucumToUdunits line #" + (i3 + 2) + " isn't empty.");
        Units2.ucumToUdunitsHM.put(
            String2.canonical(tUcumToUdunits[i3].trim()),
            String2.canonical(tUcumToUdunits[i3 + 1].trim()));
      }

      int nUdunitsToUcum = tUdunitsToUcum.length / 3;
      for (int i = 0; i < nUdunitsToUcum; i++) {
        int i3 = i * 3;
        Test.ensureTrue(
            String2.isSomething(tUdunitsToUcum[i3]),
            "udunitsToUcum line #" + (i3 + 0) + " is empty.");
        Test.ensureTrue(
            String2.isSomething(tUdunitsToUcum[i3 + 1]),
            "udunitsToUcum line #" + (i3 + 1) + " is empty.");
        Test.ensureEqual(
            tUdunitsToUcum[i3 + 2].trim(), "", "udunitsToUcum line #" + (i3 + 2) + " isn't empty.");
        Units2.udunitsToUcumHM.put(
            String2.canonical(tUdunitsToUcum[i3].trim()),
            String2.canonical(tUdunitsToUcum[i3 + 1].trim()));
      }

      int nUpdateUrls = tUpdateUrls.length / 3;
      updateUrlsFrom = new String[nUpdateUrls];
      updateUrlsTo = new String[nUpdateUrls];
      for (int i = 0; i < nUpdateUrls; i++) {
        int i3 = i * 3;
        updateUrlsFrom[i] = String2.canonical(tUpdateUrls[i3].trim());
        updateUrlsTo[i] = String2.canonical(tUpdateUrls[i3 + 1].trim());
        Test.ensureTrue(
            String2.isSomething(tUpdateUrls[i3]), "updateUrls line #" + (i3 + 0) + " is empty.");
        Test.ensureTrue(
            String2.isSomething(tUpdateUrls[i3 + 1]),
            "updateUrls line #" + (i3 + 1) + " is empty.");
        Test.ensureEqual(
            tUpdateUrls[i3 + 2].trim(), "", "updateUrls line #" + (i3 + 0) + " isn't empty.");
      }

      for (String palette : palettes) {
        String tName = fullPaletteDirectory + palette + ".cpt";
        Test.ensureTrue(
            File2.isFile(tName),
            "\"" + palette + "\" is listed in <palettes>, but there is no file " + tName);
      }

      // try to create an nc4 file
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

      String tEmail = SSR.getSafeEmailAddress(adminEmail);
      for (int tl = 0; tl < nLanguages; tl++) {
        searchHintsTooltipAr[tl] =
            "<div class=\"standard_max_width\">"
                + searchHintsTooltipAr[tl]
                + "\n"
                + (useLuceneSearchEngine
                    ? searchHintsLuceneTooltipAr[tl]
                    : searchHintsOriginalTooltipAr[tl])
                + "</div>";
        advancedSearchDirectionsAr[tl] =
            String2.replaceAll(
                advancedSearchDirectionsAr[tl], "&searchButton;", searchButtonAr[tl]);

        loginProblemsAr[tl] =
            String2.replaceAll(loginProblemsAr[tl], "&cookiesHelp;", cookiesHelpAr[tl]);
        loginProblemsAr[tl] =
            String2.replaceAll(loginProblemsAr[tl], "&adminContact;", adminContact()) + "\n\n";
        loginProblemsAfterAr[tl] =
            String2.replaceAll(loginProblemsAfterAr[tl], "&adminContact;", adminContact()) + "\n\n";
        loginPublicAccessAr[tl] += "\n";
        logoutSuccessAr[tl] += "\n";

        filesDocumentationAr[tl] =
            String2.replaceAll(filesDocumentationAr[tl], "&adminEmail;", tEmail);

        doWithGraphsAr[tl] =
            String2.replaceAll(doWithGraphsAr[tl], "&ssUse;", slideSorterActive ? ssUseAr[tl] : "");

        theLongDescriptionHtmlAr[tl] =
            String2.replaceAll(
                theLongDescriptionHtmlAr[tl], "&ssUse;", slideSorterActive ? ssUseAr[tl] : "");
        theLongDescriptionHtmlAr[tl] =
            String2.replaceAll(
                theLongDescriptionHtmlAr[tl],
                "&requestFormatExamplesHtml;",
                requestFormatExamplesHtmlAr[tl]);
        theLongDescriptionHtmlAr[tl] =
            String2.replaceAll(
                theLongDescriptionHtmlAr[tl],
                "&resultsFormatExamplesHtml;",
                resultsFormatExamplesHtmlAr[tl]);
      }

      try {
        computerName = System.getenv("COMPUTERNAME"); // windows
        if (computerName == null) computerName = System.getenv("HOSTNAME"); // linux
        if (computerName == null)
          computerName =
              java.net.InetAddress.getLocalHost().getHostName(); // coastwatch.pfeg.noaa.gov
        if (computerName == null) computerName = "";
        int dotPo = computerName.indexOf('.');
        if (dotPo > 0) computerName = computerName.substring(0, dotPo);
      } catch (Throwable t2) {
        computerName = "";
      }

      // ****************************************************************
      // other initialization

      // trigger CfToGcmd initialization to ensure CfToGcmd.txt file is valid.
      String testCfToGcmd[] = CfToFromGcmd.cfToGcmd("sea_water_temperature");
      Test.ensureTrue(
          testCfToGcmd.length > 0, "testCfToGcmd=" + String2.toCSSVString(testCfToGcmd));

      // successfully finished
      String2.log("*** " + erdStartup + " finished successfully." + eol);

    } catch (Throwable t) {
      errorInMethod =
          "ERROR during " + erdStartup + ":\n" + errorInMethod + "\n" + MustBe.throwableToString(t);
      System.out.println(errorInMethod);
      //        if (String2.logFileName() != null)
      //            String2.log(errorInMethod);
      //        String2.returnLoggingToSystemOut();
      throw new RuntimeException(errorInMethod);
    }
  }

  /** This does getNotNothingString for each messages[]. */
  private static String[] getNotNothingString(
      ResourceBundle2 messages[], String name, String errorInMethod) {

    int nMessages = messages.length;
    String ar[] = new String[nMessages];
    for (int i = 0; i < nMessages; i++)
      ar[i] = messages[i].getNotNothingString(name, errorInMethod + "When language=" + i + ", ");
    return ar;
  }

  /**
   * This gets a string from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private static String getSetupEVString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      return value;
    }
    return setup.getString(paramName, tDefault);
  }

  /**
   * A variant of getSetupEVString that works with an array of tDefault.
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private static String[] getSetupEVString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String tDefault[]) {
    String value = ev.get("ERDDAP_" + paramName);
    int n = tDefault.length;
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      for (int i = 0; i < n; i++) tDefault[i] = value;
      return tDefault;
    }
    for (int i = 0; i < n; i++) tDefault[i] = setup.getString(paramName, tDefault[i]);
    return tDefault;
  }

  /**
   * This gets a boolean from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private static boolean getSetupEVBoolean(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, boolean tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
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
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private static int getSetupEVInt(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, int tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
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
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param errorInMethod the start of an Error message
   * @return the desired value
   * @throws RuntimeException if there is no value for key
   */
  private static String getSetupEVNotNothingString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String errorInMethod) {
    String value = ev.get("ERDDAP_" + paramName);
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      return value;
    }
    return setup.getNotNothingString(paramName, errorInMethod);
  }

  /**
   * 'logLevel' determines how many diagnostic messages are sent to the log.txt file. It can be set
   * to "warning" (the fewest messages), "info" (the default), or "all" (the most messages).
   *
   * @param logLevel invalid becomes "info"
   * @return the valid value of logLevel
   */
  public static String setLogLevel(String logLevel) {
    if (!String2.isSomething(logLevel)) logLevel = "info";
    logLevel = logLevel.toLowerCase();
    if (!logLevel.equals("warning") && !logLevel.equals("all")) logLevel = "info";

    verbose = !logLevel.equals("warning");
    AxisDataAccessor.verbose = verbose;
    Boundaries.verbose = verbose;
    Calendar2.verbose = verbose;
    EDD.verbose = verbose;
    EDV.verbose = verbose;
    EmailThread.verbose = verbose;
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
    // ResourceBundle2.verbose = verbose;
    RunLoadDatasets.verbose = verbose;
    SgtGraph.verbose = verbose;
    SgtMap.verbose = verbose;
    SgtUtil.verbose = verbose;
    SSR.verbose = verbose;
    Subscriptions.verbose = verbose;
    Table.verbose = verbose;
    TaskThread.verbose = verbose;
    TouchThread.verbose = verbose;
    Units2.verbose = verbose;

    reallyVerbose = logLevel.equals("all");
    AxisDataAccessor.reallyVerbose = reallyVerbose;
    Boundaries.reallyVerbose = reallyVerbose;
    Calendar2.reallyVerbose = reallyVerbose;
    EDD.reallyVerbose = reallyVerbose;
    EDV.reallyVerbose = reallyVerbose;
    EmailThread.reallyVerbose = reallyVerbose;
    Erddap.reallyVerbose = reallyVerbose;
    File2.reallyVerbose = reallyVerbose;
    FileVisitorDNLS.reallyVerbose = reallyVerbose;
    FilledMarkerRenderer.reallyVerbose = reallyVerbose;
    GridDataAccessor.reallyVerbose = reallyVerbose;
    GSHHS.reallyVerbose = reallyVerbose;
    LoadDatasets.reallyVerbose = reallyVerbose;
    NcHelper.reallyVerbose = reallyVerbose;
    // OutputStreamFromHttpResponse.reallyVerbose = reallyVerbose;  currently no such setting
    PathCartesianRenderer.reallyVerbose = reallyVerbose;
    PrimitiveArray.reallyVerbose = reallyVerbose;
    // Projects.reallyVerbose = reallyVerbose;  currently no such setting
    SgtGraph.reallyVerbose = reallyVerbose;
    SgtMap.reallyVerbose = reallyVerbose;
    SgtUtil.reallyVerbose = reallyVerbose;
    SSR.reallyVerbose = reallyVerbose;
    Subscriptions.reallyVerbose = reallyVerbose;
    Table.reallyVerbose = reallyVerbose;
    // Table.debug = reallyVerbose; //for debugging
    TaskThread.reallyVerbose = reallyVerbose;
    TouchThread.reallyVerbose = reallyVerbose;
    // Units2.reallyVerbose = reallyVerbose;  currently no such setting

    String2.log(
        "logLevel=" + logLevel + ": verbose=" + verbose + " reallyVerbose=" + reallyVerbose);
    return logLevel;
  }

  /**
   * If loggedInAs is null, this returns baseUrl, else baseHttpsUrl (neither has slash at end).
   *
   * @param loggedInAs
   * @return If loggedInAs == null, this returns baseUrl, else baseHttpsUrl (neither has slash at
   *     end).
   */
  public static String baseUrl(String loggedInAs) {
    return loggedInAs == null ? baseUrl : baseHttpsUrl; // works because of loggedInAsHttps
  }

  /**
   * If loggedInAs is null, this returns erddapUrl, else erddapHttpsUrl (neither has slash at end).
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @return If loggedInAs == null, this returns erddapUrl, else erddapHttpsUrl (neither has slash
   *     at end).
   */
  public static String erddapUrl(String loggedInAs, int language) {
    return (loggedInAs == null ? erddapUrl : erddapHttpsUrl)
        + // works because of loggedInAsHttps
        (language == 0 ? "" : "/" + TranslateMessages.languageCodeList.get(language));
  }

  /**
   * If loggedInAs is null, this returns erddapUrl, else erddapHttpsUrl (neither has slash at end).
   *
   * @param language the index of the selected language
   * @return erddapHttpsUrl plus optional /languageCode. (without slash at end).
   */
  public static String erddapHttpsUrl(int language) {
    return erddapHttpsUrl
        + (language == 0 ? "" : "/" + TranslateMessages.languageCodeList.get(language));
  }

  /**
   * This determines if a URL points to this server (even in development).
   *
   * @param tUrl
   */
  public static boolean urlIsThisComputer(String tUrl) {
    return tUrl.startsWith(baseUrl)
        || tUrl.startsWith(preferredErddapUrl)
        || // will be baseHttpsUrl if active
        urlIsLocalhost(tUrl);
  }

  /**
   * This determines if a URL points to this server (even in development).
   *
   * @param tUrl
   */
  public static boolean urlIsLocalhost(String tUrl) {
    if (!tUrl.startsWith("http")) return false;
    return tUrl.startsWith("https://localhost")
        || tUrl.startsWith("http://localhost")
        || tUrl.startsWith("https://127.0.0.1")
        || tUrl.startsWith("http://127.0.0.1");
  }

  /**
   * This returns the appropriate image directory URL (with slash at end).
   *
   * @param loggedInAs
   * @param language
   * @return returns the appropriate image directory URL (with slash at end).
   */
  public static String imageDirUrl(String loggedInAs, int language) {
    return erddapUrl(loggedInAs, language) + "/" + IMAGES_DIR;
  }

  /**
   * This returns the html needed to display the external.png image with the warning that the link
   * is to an external website.
   *
   * @param language the index of the selected language
   * @param tErddapUrl
   * @return the html needed to display the external.png image and messages.
   */
  public static String externalLinkHtml(int language, String tErddapUrl) {
    return "<img\n"
        + "    src=\""
        + tErddapUrl
        + "/images/external.png\" "
        + "alt=\""
        + externalLinkAr[language]
        + "\"\n"
        + "    title=\""
        + externalWebSiteAr[language]
        + "\">";
  }

  /**
   * This returns the html documentation for acceptEncoding.
   *
   * @param language the index of the selected language
   * @param headingType e.g., h2 or h3
   * @param tErddapUrl
   * @return the html needed to document acceptEncodig.
   */
  public static String acceptEncodingHtml(int language, String headingType, String tErddapUrl) {
    String s =
        String2.replaceAll(
            acceptEncodingHtmlAr[language], "&headingType;", "<" + headingType + ">");
    s = String2.replaceAll(s, "&sheadingType;", "</" + headingType + ">");
    return String2.replaceAll(s, "&externalLinkHtml;", externalLinkHtml(language, tErddapUrl));
  }

  /**
   * This returns the html documentation for the /files/ system.
   *
   * @param language the index of the selected language
   * @param tErddapUrl
   * @return the html needed to document acceptEncodig.
   */
  public static String filesDocumentation(int language, String tErddapUrl) {
    return String2.replaceAll(
        filesDocumentationAr[language],
        "&acceptEncodingHtml;",
        acceptEncodingHtml(language, "h3", tErddapUrl));
  }

  /**
   * This is used by html web page generating methods to return the You Are Here html for ERDDAP.
   *
   * @return the You Are Here html for this EDD subclass.
   */
  public static String youAreHere() {
    return "\n<h1>" + ProgramName + "</h1>\n";
  }

  /**
   * This is used by html web page generating methods to return the You Are Here html for a
   * ERDDAP/protocol.
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @param protocol e.g., tabledap
   * @return the You Are Here html for this EDD subclass.
   */
  public static String youAreHere(int language, String loggedInAs, String protocol) {
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, erddapUrl(loggedInAs, language))
        + " &gt; "
        + protocol
        + "</h1>\n";
  }

  /**
   * This is used by html web page generating methods to return the You Are Here html for a
   * ERDDAP/protocol/sub .
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @param protocol e.g., subscriptions
   * @param protocolNameAr subscriptionsTitleAr
   * @param current
   * @return the You Are Here html for this EDD subclass.
   */
  public static String youAreHere(
      int language, String loggedInAs, String protocol, String protocolNameAr[], String current) {
    String tErddapUrl = erddapUrl(loggedInAs, language);
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, tErddapUrl)
        + " &gt; <a rel=\"bookmark\" href=\""
        + tErddapUrl
        + "/"
        + protocol
        + "\">"
        + protocolNameAr[language]
        + "</a>"
        + " &gt; "
        + current
        + "</h1>\n";
  }

  /**
   * This returns a not-yet-HTML-encoded protocol URL. You may want to encode it with
   * XML.encodeAsHTML(url)
   */
  public static String protocolUrl(String tErddapUrl, String protocol) {
    return tErddapUrl
        + "/"
        + protocol
        + (protocol.equals("files") ? "/" : "/index.html")
        + (protocol.equals("tabledap")
                || protocol.equals("griddap")
                || protocol.equals("wms")
                || protocol.equals("wcs")
                || protocol.equals("info")
                || protocol.equals("categorize")
            ? "?" + defaultPIppQuery
            : "");
  }

  /**
   * This is used by html web page generating methods to return the You Are Here html for
   * ERDDAP/protocol/datasetID.
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @param protocol e.g., tabledap (must be the same case as in the URL so the link will work)
   * @param datasetID e.g., erdGlobecBottle
   * @return the You Are Here html for this EDD subclass.
   */
  public static String youAreHere(
      int language, String loggedInAs, String protocol, String datasetID) {
    String tErddapUrl = erddapUrl(loggedInAs, language);
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, tErddapUrl)
        + "\n &gt; <a rel=\"contents\" "
        + "href=\""
        + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol))
        + "\">"
        + protocol
        + "</a>"
        + "\n &gt; "
        + datasetID
        + "</h1>\n";
  }

  /**
   * This is used by html web page generating methods to return the You Are Here html for
   * ERDDAP/protocol with a helpful information.
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @param protocol e.g., tabledap
   * @param htmlHelp
   * @return the You Are Here html for this EDD subclass.
   */
  public static String youAreHereWithHelp(
      int language, String loggedInAs, String protocol, String htmlHelp) {
    String tErddapUrl = erddapUrl(loggedInAs, language);
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, tErddapUrl)
        + "\n &gt; "
        + protocol
        + "\n"
        + htmlTooltipImage(language, loggedInAs, htmlHelp)
        + "\n</h1>\n";
  }

  /**
   * This is used by html web page generating methods to return the You Are Here html for
   * ERDDAP/protocol/datasetID with a helpful information.
   *
   * @param loggedInAs
   * @param protocol e.g., tabledap
   * @param datasetID e.g., erdGlobecBottle
   * @param htmlHelp
   * @return the You Are Here html for this EDD subclass.
   */
  public static String youAreHereWithHelp(
      int language, String loggedInAs, String protocol, String datasetID, String htmlHelp) {

    String tErddapUrl = erddapUrl(loggedInAs, language);
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, tErddapUrl)
        + "\n &gt; <a rel=\"contents\" "
        + "href=\""
        + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol))
        + "\">"
        + protocol
        + "</a>"
        + "\n &gt; "
        + datasetID
        + "\n"
        + htmlTooltipImage(language, loggedInAs, htmlHelp)
        + "\n</h1>\n";
  }

  /**
   * THIS IS NO LONGER USED This is used by html web page generating methods to return the You Are
   * Here html for ERDDAP/{protocol}/{attribute}/{category}. IF REVIVED, append current
   * ?page=x&amp;itemsPerPage=y
   *
   * @param loggedInAs
   * @param protocol e.g., categorize
   * @param attribute e.g., ioos_category
   * @param category e.g., Temperature
   * @return the You Are Here html for this EDD subclass.
   */
  /*public static String youAreHere(int language, String loggedInAs, String protocol,
      String attribute, String category) {

      String tErddapUrl = erddapUrl(loggedInAs, language);
      String attributeUrl = tErddapUrl + "/" + protocol + "/" + attribute + "/index.html"; //+?defaultPIppQuery
      return
          "\n<h1>" + erddapHref(language, tErddapUrl) +
          "\n &gt; <a href=\"" + XML.encodeAsHTMLAttribute(protocolUrl(tErddapUrl, protocol)) +
              "\">" + protocol + "</a>" +
          "\n &gt; <a href=\"" + XML.encodeAsHTMLAttribute(attributeUrl) + "\">" + attribute + "</a>" +
          "\n &gt; " + category +
          "\n</h1>\n";
  }*/

  /**
   * This returns the html to draw a question mark that has big html tooltip. htmlTooltipScript (see
   * HtmlWidgets) must be already in the document.
   *
   * @param language the language code number
   * @param html the html tooltip text, e.g., "Hi,<br>
   *     there!". It needs explicit br tags to set window width correctly. For plain text, generate
   *     html from XML.encodeAsPreHTML(plainText, 82).
   */
  public static String htmlTooltipImage(int language, String loggedInAs, String html) {
    return HtmlWidgets.htmlTooltipImage(
        imageDirUrl(loggedInAs, language) + questionMarkImageFile, "?", html, "");
  }

  /**
   * This returns the html to draw a question mark that has big html tooltip for an EDDTable EDV
   * data variable.
   *
   * @param language the language code number
   * @param edv from an EDDTable
   */
  public static String htmlTooltipImageEDV(int language, String loggedInAs, EDV edv)
      throws Throwable {

    return htmlTooltipImageLowEDV(
        language,
        loggedInAs,
        edv.destinationDataPAType(),
        edv.destinationName(),
        edv.combinedAttributes());
  }

  /**
   * This returns the html to draw a question mark that has big html tooltip for an EDDGrid EDV axis
   * variable.
   *
   * @param language the language code number
   * @param edvga
   */
  public static String htmlTooltipImageEDVGA(int language, String loggedInAs, EDVGridAxis edvga)
      throws Throwable {

    return htmlTooltipImageLowEDV(
        language,
        loggedInAs,
        edvga.destinationDataPAType(),
        edvga.destinationName() + "[" + edvga.sourceValues().size() + "]",
        edvga.combinedAttributes());
  }

  /**
   * This returns the html to draw a question mark that has big html tooltip for an EDDGrid EDV data
   * variable.
   *
   * @param language the language code number
   * @param edv for a grid variable
   * @param allDimString from eddGrid.allDimString()
   */
  public static String htmlTooltipImageEDVG(
      int language, String loggedInAs, EDV edv, String allDimString) throws Throwable {

    return htmlTooltipImageLowEDV(
        language,
        loggedInAs,
        edv.destinationDataPAType(),
        edv.destinationName() + allDimString,
        edv.combinedAttributes());
  }

  /**
   * This returns the html to draw a question mark that has big html tooltip with a variable's name
   * and attributes. htmlTooltipScript (see HtmlWidgets) must be already in the document.
   *
   * @param language the language code number
   * @param destinationDataPAType
   * @param destinationName perhaps with axis information appended (e.g.,
   *     [time][latitude][longitude]
   * @param attributes
   */
  public static String htmlTooltipImageLowEDV(
      int language,
      String loggedInAs,
      PAType destinationDataPAType,
      String destinationName,
      Attributes attributes)
      throws Throwable {

    StringBuilder sb =
        OpendapHelper.dasToStringBuilder(
            OpendapHelper.getAtomicType(false, destinationDataPAType)
                + " "
                + destinationName, // strictDapMode
            destinationDataPAType,
            attributes,
            false,
            false); // htmlEncoding, strictDapMode
    // String2.log("htmlTooltipImage sb=" + sb.toString());
    return htmlTooltipImage(
        language,
        loggedInAs,
        "<div class=\"standard_max_width\">" + XML.encodeAsPreHTML(sb.toString()) + "</div>");
  }

  /**
   * This sends the specified email to one or more emailAddresses.
   *
   * @param emailAddressCsv comma-separated list (may have ", ")
   * @return an error message ("" if no error). If emailAddress is null or "", this logs the message
   *     and returns "".
   */
  public static String email(String emailAddressCsv, String subject, String content) {
    return email(String2.split(emailAddressCsv, ','), subject, content);
  }

  /**
   * This sends one email to the emailAddresses (actually, it adds it to the emailList queue). <br>
   * This won't throw an exception if trouble. <br>
   * This method always prepends the subject and content with [erddapUrl], so that it will be clear
   * which ERDDAP this came from (in case you administer multiple ERDDAPs). <br>
   * This method logs (to log.txt) that an email was sent: to whom and the subject, but not the
   * content. <br>
   * This method logs the entire email to the email log, e.g.,
   * (bigParentDirectory)/emailLog2009-01.txt
   *
   * @param emailAddresses each e.g., john.doe@company.com
   * @param subject If error, recommended: "Error in [someClass]". If this starts with
   *     EDStatic.DONT_LOG_THIS_EMAIL, this email won't be logged (which is useful for confidential
   *     emails).
   * @param content If error, recommended: MustBe.throwableToString(t);
   * @return an error message ("" if no error). If emailAddresses is null or length==0, this logs
   *     the message and returns "".
   */
  public static String email(String emailAddresses[], String subject, String content) {

    String emailAddressesCSSV = "";
    try {
      // ensure all email addresses are valid
      StringArray emailAddressesSA = new StringArray(emailAddresses);
      BitSet keep = new BitSet(emailAddressesSA.size()); // all false
      for (int i = 0; i < emailAddressesSA.size(); i++) {
        String addr = emailAddressesSA.get(i);
        String err =
            subscriptions == null
                ? // don't use EDStatic.subscriptionSystemActive for this test -- it's a separate
                // issue
                String2.testEmailAddress(addr)
                : // tests syntax
                subscriptions.testEmailValid(addr); // tests syntax and blacklist
        if (err.length() == 0) {
          keep.set(i);
        } else {
          String2.log("EDStatic.email caught an invalid email address: " + err);
        }
      }
      emailAddressesSA.justKeep(
          keep); // it's okay if 0 remain. email will still be written to log below.
      emailAddresses = emailAddressesSA.toArray();

      // write the email to the log
      emailAddressesCSSV = String2.toCSSVString(emailAddresses);
      String localTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
      boolean logIt = !subject.startsWith(DONT_LOG_THIS_EMAIL);
      if (!logIt) subject = subject.substring(DONT_LOG_THIS_EMAIL.length());
      subject =
          (computerName.length() > 0 ? computerName + " " : "")
              + "ERDDAP: "
              + String2.replaceAll(subject, '\n', ' ');

      // almost always write to emailLog
      // Always note that email sent in regular log.
      String2.log("Emailing \"" + subject + "\" to " + emailAddressesCSSV);

      String date = localTime.substring(0, 10);
      if (!emailLogDate.equals(date) || emailLogFile == null) {
        // update emailLogDate
        // do first so other threads won't do this simultaneously
        emailLogDate = date;

        // close the previous file
        if (emailLogFile != null) {
          try {
            emailLogFile.close();
          } catch (Throwable t) {
          }
          emailLogFile = null;
        }

        // open a new file
        emailLogFile =
            File2.getBufferedWriterUtf8(
                new FileOutputStream(
                    fullLogsDirectory + "emailLog" + date + ".txt", true)); // true=append
      }

      // write the email to the log
      // do in one write encourages threads not to intermingle   (or synchronize on emailLogFile?)
      emailLogFile.write(
          "\n==== BEGIN ====================================================================="
              + "\n     To: "
              + emailAddressesCSSV
              + "\nSubject: "
              + subject
              + "\n   Date: "
              + localTime
              + "\n--------------------------------------------------------------------------------"
              + (logIt
                  ? "\n" + preferredErddapUrl + " reports:" + "\n" + content
                  : "\n[CONFIDENTIAL]")
              + "\n==== END ======================================================================="
              + "\n");
      emailLogFile.flush();

    } catch (Throwable t) {
      try {
        String2.log(MustBe.throwable("Error: Writing to emailLog failed.", t));
      } catch (Throwable t2) {
      }
      if (emailLogFile != null) {
        try {
          emailLogFile.close();
        } catch (Throwable t3) {
        }
        emailLogFile = null;
      }
    }

    // send it?
    if (!String2.isSomething(emailAddressesCSSV)) {
      String2.log("Email not sent because no To address.");

    } else if (emailIsActive) {
      // send email
      synchronized (emailList) {
        emailList.add(
            new String[] {
              emailAddressesCSSV, subject, preferredErddapUrl + " reports:\n" + content
            });
      }
    } else {
      String2.log("Email not sent because email system is inactive.");
    }

    return "";
  }

  /**
   * This sets the request blacklist of numeric ip addresses (e.g., 123.45.67.89) (e.g., to fend of
   * a Denial of Service attack or an overzealous web robot). This sets requestBlacklist to be a
   * HashSet (or null).
   *
   * @param csv the comma separated list of numeric ip addresses
   */
  public static void setRequestBlacklist(String csv) {
    if (csv == null || csv.length() == 0) {
      requestBlacklist = null;
      String2.log("requestBlacklist is now null.");
    } else {
      String rb[] = String2.split(csv, ',');
      HashSet<String> hs = new HashSet<>(Math2.roundToInt(1.4 * rb.length));
      hs.addAll(Arrays.asList(rb));
      requestBlacklist = hs; // set atomically
      String2.log("requestBlacklist is now " + String2.toCSSVString(rb));
    }
  }

  /**
   * This tests if the ipAddress is on the blacklist (and calls sendLowError if it is).
   *
   * @param language the index of the selected language
   * @param ipAddress the requester's ipAddress
   * @param requestNumber The requestNumber assigned to this request by doGet().
   * @param response so the response can be sent the error
   * @return true if user is on the blacklist.
   */
  public static boolean isOnBlacklist(
      int language, String ipAddress, int requestNumber, HttpServletResponse response) {
    // refuse request? e.g., to fend of a Denial of Service attack or an overzealous web robot

    // for testing:
    //  int tr = Math2.random(3);
    //  ipAddress=tr==0? "101.2.34.56" : tr==1? "1:2:3:4:56:78" : "(unknownIPAddress)";

    int periodPo1 = ipAddress.lastIndexOf('.'); // to make #.#.#.* test below for IP v4 address
    boolean hasPeriod = periodPo1 > 0;
    if (!hasPeriod)
      periodPo1 =
          ipAddress.lastIndexOf(':'); // to make #:#:#:#:#:#:#:* test below for IP v6 address
    String ipAddress1 = periodPo1 <= 0 ? null : ipAddress.substring(0, periodPo1 + 1) + "*";
    int periodPo2 =
        ipAddress1 == null
            ? -1
            : ipAddress.substring(0, periodPo1).lastIndexOf(hasPeriod ? '.' : ':');
    String ipAddress2 =
        periodPo2 <= 0 ? null : ipAddress.substring(0, periodPo2 + 1) + (hasPeriod ? "*.*" : "*:*");
    // String2.log(">> ipAddress=" + ipAddress + " ipAddress1=" + ipAddress1 + " ipAddress2=" +
    // ipAddress2);
    if (requestBlacklist != null
        && (requestBlacklist.contains(ipAddress)
            || (ipAddress1 != null && requestBlacklist.contains(ipAddress1))
            || // #.#.#.*
            (ipAddress2 != null && requestBlacklist.contains(ipAddress2)))) { // #.#.*.*
      // use full ipAddress, to help id user                //odd capitilization sorts better
      tally.add("Requester's IP Address (Blacklisted) (since last Major LoadDatasets)", ipAddress);
      tally.add("Requester's IP Address (Blacklisted) (since last daily report)", ipAddress);
      tally.add("Requester's IP Address (Blacklisted) (since startup)", ipAddress);
      String2.log("}}}}#" + requestNumber + " Requester is on the datasets.xml requestBlacklist.");
      lowSendError(
          requestNumber,
          response,
          HttpServletResponse.SC_FORBIDDEN, // a.k.a. Error 403
          blacklistMsgAr[language]);
      return true;
    }
    return false;
  }

  /** This adds the common, publicly accessible statistics to the StringBuilder. */
  public static void addIntroStatistics(StringBuilder sb, boolean includeErrors, Erddap erddap) {
    sb.append("Current time is " + Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n");
    sb.append("Startup was at  " + startupLocalDateTime + "\n");
    long loadTime = lastMajorLoadDatasetsStopTimeMillis - lastMajorLoadDatasetsStartTimeMillis;
    long timSinceLoad = (System.currentTimeMillis() - lastMajorLoadDatasetsStartTimeMillis) / 1000;
    sb.append(
        "Last major LoadDatasets started "
            + Calendar2.elapsedTimeString(1000l * Math2.longToInt(timSinceLoad))
            + " ago and "
            + (loadTime < 0
                ? "is still running.\n"
                : "finished after " + (loadTime / 1000) + " seconds.\n"));

    // make local copies to avoid trouble from volatile variables
    boolean tcldMajor = cldMajor;
    int tcldNTry = cldNTry; //   0=none actively loading
    String tcldDatasetID = cldDatasetID; // null=none actively loading
    long tcldStartMillis = cldStartMillis; //   0=none actively loading
    if (tcldNTry == 0 || tcldDatasetID == null || tcldStartMillis == 0) {
      sb.append("Currently, no dataset is loading.\n");
    } else {
      sb.append(
          "Currently, "
              + (tcldMajor ? "major" : "minor")
              + " LoadDatasets is loading dataset #"
              + tcldNTry
              + "="
              + tcldDatasetID
              + " ("
              + Calendar2.elapsedTimeString(
                  Math2.longToDoubleNaN(System.currentTimeMillis() - tcldStartMillis))
              + ").\n");
    }

    // make local copy of volatile variables to avoid null pointers and so sum is correct
    ConcurrentHashMap<String, EDDGrid> tGridDatasetHashMap = erddap.gridDatasetHashMap;
    ConcurrentHashMap<String, EDDTable> tTableDatasetHashMap = erddap.tableDatasetHashMap;
    int tnGridDatasets = tGridDatasetHashMap == null ? 0 : tGridDatasetHashMap.size();
    int tnTableDatasets = tTableDatasetHashMap == null ? 0 : tTableDatasetHashMap.size();
    sb.append("nGridDatasets  = " + tnGridDatasets + "\n");
    sb.append("nTableDatasets = " + tnTableDatasets + "\n");
    sb.append("nTotalDatasets = " + (tnGridDatasets + tnTableDatasets) + "\n");
    sb.append(datasetsThatFailedToLoad);
    if (includeErrors) {
      sb.append(failedDatasetsWithErrors);
    }
    sb.append(errorsDuringMajorReload);
    sb.append(
        "Unique users (since startup)                            n = "
            + ipAddressQueue.size()
            + "\n");
    sb.append("Response Failed    Time (since last major LoadDatasets) ");
    sb.append(
        String2.getBriefTimeDistributionStatistics(failureTimesDistributionLoadDatasets) + "\n");
    sb.append("Response Failed    Time (since last Daily Report)       ");
    sb.append(String2.getBriefTimeDistributionStatistics(failureTimesDistribution24) + "\n");
    sb.append("Response Failed    Time (since startup)                 ");
    sb.append(String2.getBriefTimeDistributionStatistics(failureTimesDistributionTotal) + "\n");
    sb.append("Response Succeeded Time (since last major LoadDatasets) ");
    sb.append(
        String2.getBriefTimeDistributionStatistics(responseTimesDistributionLoadDatasets) + "\n");
    sb.append("Response Succeeded Time (since last Daily Report)       ");
    sb.append(String2.getBriefTimeDistributionStatistics(responseTimesDistribution24) + "\n");
    sb.append("Response Succeeded Time (since startup)                 ");
    sb.append(String2.getBriefTimeDistributionStatistics(responseTimesDistributionTotal) + "\n");

    synchronized (taskList) {
      ensureTaskThreadIsRunningIfNeeded(); // clients (like this class) are responsible for checking
      // on it
      long tElapsedTime = taskThread == null ? -1 : taskThread.elapsedTime();
      sb.append(
          "TaskThread has finished "
              + (lastFinishedTask.get() + 1)
              + " out of "
              + taskList.size()
              + " tasks.  "
              + (tElapsedTime < 0
                  ? "Currently, no task is running.\n"
                  : "The current task has been running for "
                      + Calendar2.elapsedTimeString(tElapsedTime)
                      + ".\n"));
    }

    sb.append("TaskThread Failed    Time (since last Daily Report)     ");
    sb.append(String2.getBriefTimeDistributionStatistics(taskThreadFailedDistribution24) + "\n");
    sb.append("TaskThread Failed    Time (since startup)               ");
    sb.append(String2.getBriefTimeDistributionStatistics(taskThreadFailedDistributionTotal) + "\n");
    sb.append("TaskThread Succeeded Time (since last Daily Report)     ");
    sb.append(String2.getBriefTimeDistributionStatistics(taskThreadSucceededDistribution24) + "\n");
    sb.append("TaskThread Succeeded Time (since startup)               ");
    sb.append(
        String2.getBriefTimeDistributionStatistics(taskThreadSucceededDistributionTotal) + "\n");

    synchronized (emailList) {
      ensureEmailThreadIsRunningIfNeeded(); // clients (like this class) are responsible for
      // checking on it
      if (emailIsActive) {
        long tElapsedTime = emailThread == null ? -1 : emailThread.elapsedTime();
        sb.append(
            "EmailThread has sent "
                + (lastFinishedEmail.get() + 1)
                + " out of "
                + emailList.size()
                + " emails.  "
                + (tElapsedTime < 0
                    ? "Currently, the thread is sleeping.\n"
                    : "The current email session has been running for "
                        + Calendar2.elapsedTimeString(tElapsedTime)
                        + ".\n"));
        sb.append("EmailThread Failed    Time (since last Daily Report)    ");
        sb.append(
            String2.getBriefTimeDistributionStatistics(emailThreadFailedDistribution24) + "\n");
        sb.append("EmailThread Succeeded Time (since last Daily Report)    ");
        sb.append(
            String2.getBriefTimeDistributionStatistics(emailThreadSucceededDistribution24) + "\n");
      } else {
        sb.append("The email system is inactive.\n");
      }
    }

    synchronized (touchList) {
      ensureTouchThreadIsRunningIfNeeded(); // clients (like this class) are responsible for
      // checking on it
      long tElapsedTime = touchThread == null ? -1 : touchThread.elapsedTime();
      sb.append(
          "TouchThread has finished "
              + (lastFinishedTouch.get() + 1)
              + " out of "
              + touchList.size()
              + " touches.  "
              + (tElapsedTime < 0
                  ? "Currently, the thread is sleeping.\n"
                  : "The current touch has been running for "
                      + Calendar2.elapsedTimeString(tElapsedTime)
                      + ".\n"));
      sb.append("TouchThread Failed    Time (since last Daily Report)    ");
      sb.append(String2.getBriefTimeDistributionStatistics(touchThreadFailedDistribution24) + "\n");
      sb.append("TouchThread Succeeded Time (since last Daily Report)    ");
      sb.append(
          String2.getBriefTimeDistributionStatistics(touchThreadSucceededDistribution24) + "\n");
    }

    try {
      OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
      if (osBean instanceof UnixOperatingSystemMXBean uBean) {
        sb.append(
            "OS info: totalCPULoad="
                + Math2.doubleToFloatNaN(uBean.getCpuLoad())
                + " processCPULoad="
                + Math2.doubleToFloatNaN(uBean.getProcessCpuLoad())
                + " totalMemory="
                + (uBean.getTotalMemorySize() / Math2.BytesPerMB)
                + "MB"
                + " freeMemory="
                + (uBean.getFreeMemorySize() / Math2.BytesPerMB)
                + "MB"
                + " totalSwapSpace="
                + (uBean.getTotalSwapSpaceSize() / Math2.BytesPerMB)
                + "MB"
                + " freeSwapSpace="
                + (uBean.getFreeSwapSpaceSize() / Math2.BytesPerMB)
                + "MB\n");
      }
    } catch (Exception e) {
    }
    sb.append("Number of active requests=" + activeRequests.size() + "\n");
  }

  /** This adds the common, publicly accessible statistics to the StringBuffer. */
  public static void addCommonStatistics(StringBuilder sb) {
    if (majorLoadDatasetsTimeSeriesSB.length() > 0) {
      sb.append(
          """
                      Major LoadDatasets Time Series: MLD    Datasets Loaded               Requests (median times in ms)                Number of Threads      MB    gc   Open
                        timestamp                    time   nTry nFail nTotal  nSuccess (median) nFail (median) shed memFail tooMany  tomWait inotify other  inUse Calls Files
                      ----------------------------  -----   -----------------  -----------------------------------------------------  ---------------------  ----- ----- -----
                      """);
      sb.append(majorLoadDatasetsTimeSeriesSB);
      sb.append("\n\n");
    }

    sb.append("Major LoadDatasets Times Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(majorLoadDatasetsDistribution24));
    sb.append('\n');
    sb.append("Major LoadDatasets Times Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(majorLoadDatasetsDistributionTotal));
    sb.append('\n');
    sb.append('\n');

    sb.append("Minor LoadDatasets Times Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(minorLoadDatasetsDistribution24));
    sb.append('\n');
    sb.append("Minor LoadDatasets Times Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(minorLoadDatasetsDistributionTotal));
    sb.append('\n');
    sb.append('\n');

    sb.append("Response Failed Time Distribution (since last major LoadDatasets):\n");
    sb.append(String2.getTimeDistributionStatistics(failureTimesDistributionLoadDatasets));
    sb.append('\n');
    sb.append("Response Failed Time Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(failureTimesDistribution24));
    sb.append('\n');
    sb.append("Response Failed Time Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(failureTimesDistributionTotal));
    sb.append('\n');
    sb.append('\n');

    sb.append("Response Succeeded Time Distribution (since last major LoadDatasets):\n");
    sb.append(String2.getTimeDistributionStatistics(responseTimesDistributionLoadDatasets));
    sb.append('\n');
    sb.append("Response Succeeded Time Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(responseTimesDistribution24));
    sb.append('\n');
    sb.append("Response Succeeded Time Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(responseTimesDistributionTotal));
    sb.append('\n');
    sb.append('\n');

    if (emailIsActive) {
      sb.append("EmailThread Failed Time Distribution (since last Daily Report):\n");
      sb.append(String2.getTimeDistributionStatistics(emailThreadFailedDistribution24));
      sb.append('\n');
      sb.append("EmailThread Failed Time Distribution (since startup):\n");
      sb.append(String2.getTimeDistributionStatistics(emailThreadFailedDistributionTotal));
      sb.append('\n');
      sb.append("EmailThread Succeeded Time Distribution (since last Daily Report):\n");
      sb.append(String2.getTimeDistributionStatistics(emailThreadSucceededDistribution24));
      sb.append('\n');
      sb.append("EmailThread Succeeded Time Distribution (since startup):\n");
      sb.append(String2.getTimeDistributionStatistics(emailThreadSucceededDistributionTotal));
      sb.append('\n');
      sb.append("EmailThread nEmails/Session Distribution (since last Daily Report):\n");
      sb.append(String2.getCountDistributionStatistics(emailThreadNEmailsDistribution24));
      sb.append('\n');
      sb.append("EmailThread nEmails/Session Distribution (since startup):\n");
      sb.append(String2.getCountDistributionStatistics(emailThreadNEmailsDistributionTotal));
      sb.append('\n');
      sb.append('\n');
    }

    sb.append("TaskThread Failed Time Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(taskThreadFailedDistribution24));
    sb.append('\n');
    sb.append("TaskThread Failed Time Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(taskThreadFailedDistributionTotal));
    sb.append('\n');
    sb.append("TaskThread Succeeded Time Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(taskThreadSucceededDistribution24));
    sb.append('\n');
    sb.append("TaskThread Succeeded Time Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(taskThreadSucceededDistributionTotal));
    sb.append('\n');
    sb.append('\n');

    sb.append("TouchThread Failed Time Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(touchThreadFailedDistribution24));
    sb.append('\n');
    sb.append("TouchThread Failed Time Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(touchThreadFailedDistributionTotal));
    sb.append('\n');
    sb.append("TouchThread Succeeded Time Distribution (since last Daily Report):\n");
    sb.append(String2.getTimeDistributionStatistics(touchThreadSucceededDistribution24));
    sb.append('\n');
    sb.append("TouchThread Succeeded Time Distribution (since startup):\n");
    sb.append(String2.getTimeDistributionStatistics(touchThreadSucceededDistributionTotal));
    sb.append('\n');
    sb.append('\n');

    sb.append(tally.toString("Language (since last daily report)", 50)); // added v2.15
    sb.append(tally.toString("Language (since startup)", 50));

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
   * <p>This is safe to use this after outputStream has been written to -- this won't make a session
   * if the user doesn't have one.
   *
   * @param request
   * @return null (using http), loggedInAsHttps (using https and not logged in), or userName (using
   *     https and logged in).
   */
  public static String getLoggedInAs(HttpServletRequest request) {
    if (request == null) return null;

    // request is via http? treat as not logged in
    String fullRequestUrl = request.getRequestURL().toString(); // has proxied port#, e.g. :8080
    if (!fullRequestUrl.startsWith("https://")) return null;

    // request is via https, but authentication=""?  then can't be logged in
    if (authentication.length() == 0) return loggedInAsHttps;

    // see if user is logged in
    // NOTE: session is associated with https urls, not http urls!
    //  So user only appears logged in to https urls.
    HttpSession session = request.getSession(false); // don't make one if none already
    // String2.log("session=" + (session==null? "null" :
    // session.getServletContext().getServletContextName()));

    if (session == null) return loggedInAsHttps;

    // session != null
    String loggedInAs = null;
    if (authentication.equals("custom")
        || authentication.equals("email")
        || authentication.equals("google")
        || authentication.equals("orcid")
        || authentication.equals("oauth2")) {
      loggedInAs = (String) session.getAttribute("loggedInAs:" + warName);

      // } else if (authentication.equals("openid"))
      //    loggedInAs = OpenIdFilter.getCurrentUser(session);
    }

    // ensure printable characters only (which makes loggedInAsSuperuser special)
    return loggedInAs == null ? loggedInAsHttps : String2.justPrintable(loggedInAs);
  }

  /** This generates a nonce (a long "random" string related to basis). */
  public static String nonce(String basis) {
    return String2.passwordDigest(
        "SHA-256",
        Math2.random(Integer.MAX_VALUE)
            + "_"
            + System.currentTimeMillis()
            + "_"
            + basis
            + "_"
            + flagKeyKey);
  }

  /**
   * This allows LoadDatasets to set EDStatic.userHashMap (which is private). There is no
   * getUserHashMap (so info remains private). MD5'd and SHA256'd passwords should all already be
   * lowercase.
   */
  public static void setUserHashMap(Map<String, Object[]> tUserHashMap) {
    userHashMap = tUserHashMap;
  }

  /**
   * This uses MessageFormat.format to format the message (usually an error) in English and, if
   * language&gt;0, in another language (separated by a newline).
   *
   * @param language the index of the selected language
   * @param messageAr the message array with {0} substitution locations
   * @param sub0 the text to be substituted into the message
   */
  public static String bilingualMessageFormat(int language, String messageAr[], String sub0) {
    return MessageFormat.format(messageAr[0], sub0)
        + (language > 0 ? "\n" + MessageFormat.format(messageAr[language], sub0) : "");
  }

  /**
   * This uses MessageFormat.format to format the message (usually an error) in English and, if
   * language&gt;0, in another language (separated by a newline).
   *
   * @param language the index of the selected language
   * @param messageAr the message array with {0} and {1} substitution locations
   * @param sub0 the text to be substituted into the message
   * @param sub1 the text to be substituted into the message
   */
  public static String bilingualMessageFormat(
      int language, String messageAr[], String sub0, String sub1) {
    return MessageFormat.format(messageAr[0], sub0, sub1)
        + (language > 0 ? "\n" + MessageFormat.format(messageAr[language], sub0, sub1) : "");
  }

  /**
   * If language=0, this returns eng. If language&gt;0, this returns eng+(space if needed)+other.
   * This is mostly used so that error messages can be bilingual.
   *
   * @param language the index of the selected language
   * @param ar An EDStatic ...Ar variable
   * @return If language=0, this returns eng. If language&gt;0, this returns eng+(space if
   *     needed)+other.
   */
  public static String simpleBilingual(int language, String ar[]) {
    return language == 0 ? ar[0] : String2.periodSpaceConcat(ar[0], ar[language]);
  }

  /**
   * If language=0, this returns eng. If language&gt;0, this returns eng+newline+other. This is
   * mostly used so that error messages can be bilingual.
   *
   * @param language the index of the selected language
   * @param eng
   * @param other
   * @return If language=0, this returns eng. If language&gt;0, this returns eng+newline+other.
   */
  public static String bilingual(int language, String eng, String other) {
    return eng + (language > 0 ? "\n" + other : "");
  }

  /**
   * If language=0, this returns ar0[0]+ar1[0]. If language&gt;0, this returns
   * ar0[0]+ar1[0]+newline+ar0[language]+ar1[language]. This is mostly used so that error messages
   * can be bilingual.
   *
   * @param language the index of the selected language
   * @param ar0 one translated message array
   * @param ar1 another translated message array
   * @return If language=0, this returns ar0[0]+ar1[0]. If language&gt;0, this returns
   *     ar0[0]+ar1[0]+newline+ar0[language]+ar1[language].
   */
  public static String bilingual(int language, String ar0[], String ar1[]) {
    return ar0[0] + ar1[0] + (language > 0 ? "\n" + ar0[language] + ar1[language] : "");
  }

  /**
   * For "custom" authentication, this returns true if the plaintextPassword (after passwordEncoding
   * as specified in setup.xml) matches the stored password for user.
   *
   * @param username the user's log in name
   * @param plaintextPassword that the user entered on a log-in form
   * @return true if the plaintextPassword (after passwordEncoding as specified in setup.xml)
   *     matches the stored password for username. If user==null or user has no password defined in
   *     datasets.xml, this returns false.
   */
  public static boolean doesPasswordMatch(String username, String plaintextPassword) {
    if (username == null || plaintextPassword == null) return false;

    username = username.trim();
    plaintextPassword = plaintextPassword.trim();
    if (username.length() == 0 || !username.equals(String2.justPrintable(username))) {
      String2.log("username=" + username + " doesn't match basic requirements.");
      return false;
    }
    if (plaintextPassword.length() < minimumPasswordLength
        || !plaintextPassword.equals(String2.justPrintable(plaintextPassword))) {
      String2.log(
          "plaintextPassword for username=" + username + " doesn't match basic requirements.");
      return false;
    }

    Object oar[] = userHashMap.get(username);
    if (oar == null) {
      String2.log("username=" + username + " not found in userHashMap.");
      return false;
    }
    String expected = (String) oar[0]; // using passwordEncoding in setup.xml
    if (expected == null) return false;

    // generate observedPassword from plaintextPassword via passwordEncoding
    String observed =
        switch (passwordEncoding) {
          case "MD5" -> String2.md5Hex(plaintextPassword); // it will be lowercase
          case "UEPMD5" ->
              String2.md5Hex(username + ":ERDDAP:" + plaintextPassword); // it will be lowercase
          case "SHA256" ->
              String2.passwordDigest("SHA-256", plaintextPassword); // it will be lowercase
          case "UEPSHA256" ->
              String2.passwordDigest(
                  "SHA-256", username + ":ERDDAP:" + plaintextPassword); // it will be lowercase
          default -> throw new RuntimeException("Unexpected passwordEncoding=" + passwordEncoding);
        };
    // only for debugging:
    // String2.log("username=" + username + " plaintextPassword=" + plaintextPassword +
    //    "\nobsPassword=" + observed + "\nexpPassword=" + expected);

    boolean ok = observed.equals(expected);
    if (reallyVerbose) String2.log("username=" + username + " password matched: " + ok);
    return ok;
  }

  /**
   * This indicates if a user is on the list of potential users (i.e., there's a user tag for this
   * user in datasets.xml).
   *
   * @param userName the user's potential user name
   * @return true if a user is on the list of potential users (i.e., there's a user tag for this
   *     user in datasets.xml).
   */
  public static boolean onListOfUsers(String userName) {
    if (!String2.isSomething(userName)) return false;
    return userHashMap.get(userName) != null;
  }

  /**
   * This returns the roles for a user.
   *
   * @param loggedInAs the user's logged in name (or null if not logged in)
   * @return the roles for the user. If user==null, this returns null. Anyone logged in
   *     automatically gets role=anyoneLoggedIn ("[anyoneLoggedIn]").
   */
  public static String[] getRoles(String loggedInAs) {
    if (loggedInAs == null || loggedInAsHttps.equals(loggedInAs)) return null;

    // ???future: for authentication="basic", use tomcat-defined roles???

    // all other authentication methods
    Object oar[] = userHashMap.get(loggedInAs);
    if (oar == null) {
      String[] roles = new String[anyoneLoggedInRoles.size()];
      return anyoneLoggedInRoles.toArray(
          roles); // no <user> tag, but still gets role=[anyoneLoggedIn]
    }
    return (String[]) oar[1];
  }

  /**
   * If the user tries to access a dataset to which he doesn't have access, call this to send Http
   * UNAUTHORIZED error. (was: redirectToLogin: redirect him to the login page).
   *
   * @param language the index of the selected language
   * @param requestNumber The requestNumber assigned to this request by doGet().
   * @param loggedInAs the name of the logged in user (or null if not logged in)
   * @param datasetID or use "" for general login.
   * @param graphsAccessibleToPublic From edd.graphsAccessibleToPublic(). If this is true, then this
   *     method was called because the request was for data from a dataset that allows
   *     graphics|metadata requests from the public.
   * @throws Throwable (notably ClientAbortException)
   */
  public static void sendHttpUnauthorizedError(
      int language,
      int requestNumber,
      String loggedInAs,
      HttpServletResponse response,
      String datasetID,
      boolean graphsAccessibleToPublic)
      throws Throwable {

    String message = "The user is not authorized to make that request."; // default
    try {
      tally.add("Request refused: not authorized (since startup)", datasetID);
      tally.add("Request refused: not authorized (since last daily report)", datasetID);
      tally.add("Request refused: not authorized (since last Major LoadDatasets)", datasetID);

      if (datasetID != null && datasetID.length() > 0)
        message =
            MessageFormat.format(
                graphsAccessibleToPublic
                    ? notAuthorizedForDataAr[language]
                    : notAuthorizedAr[language],
                loggedInAsHttps.equals(loggedInAs) ? "" : loggedInAs,
                datasetID);

      lowSendError(requestNumber, response, HttpServletResponse.SC_UNAUTHORIZED, message);

    } catch (Throwable t2) {
      rethrowClientAbortException(t2); // first thing in catch{}
      String2.log(
          "Error in sendHttpUnauthorizedError for request #"
              + requestNumber
              + ":\n"
              + (message == null ? "" : message + "\n")
              + MustBe.throwableToString(t2));
    }
  }

  /**
   * This returns the session's login status html (with a link to log in/out) suitable for use at
   * the top of a web page. <br>
   * If logged in: loggedInAs | logout <br>
   * If not logged in: login
   *
   * <p>This is safe to use this after outputStream has been written to -- this won't make a session
   * if the user doesn't have one.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in) Special case:
   *     "loggedInAsHttps" is for using https without being logged in, but &amp;loginInfo; indicates
   *     user isn't logged in.
   */
  public static String getLoginHtml(int language, String loggedInAs) {
    if (authentication.isEmpty()) {
      // user can't log in
      return "";
    } else {
      String tUrl = erddapHttpsUrl(language);
      return loggedInAs == null || loggedInAsHttps.equals(loggedInAs)
          ? // ie not logged in
          // always use the erddapHttpsUrl for login/logout pages
          "<a href=\"" + tUrl + "/login.html\">" + loginAr[language] + "</a>"
          : "<a href=\""
              + tUrl
              + "/login.html\"><strong>"
              + XML.encodeAsHTML(loggedInAs)
              + "</strong></a> | \n"
              + "<a href=\""
              + tUrl
              + "/logout.html\">"
              + logoutAr[language]
              + "</a>";
    }
  }

  /**
   * This returns the startBodyHtml (with the user's login info inserted if &amp;loginInfo; is
   * present).
   *
   * <p>This is safe to use this after outputStream has been written to -- this won't make a session
   * if the user doesn't have one.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Special case:
   *     "loggedInAsHttps" is for using https without being logged in, but &amp;loginInfo; indicates
   *     user isn't logged in.
   * @param endOfRequest The part after "http.../erddap/". 2022-11-22 AVOID XSS ERROR: THIS MUST BE
   *     VALID!
   * @param queryString from request.queryString(). May be null here. Must be percent-encoded.
   *     2022-11-22 This is not used for security reasons (and it is not practical to ensure it's
   *     valid and not malicious).
   */
  public static String startBodyHtml(
      int language, String loggedInAs, String endOfRequest, String queryString) {
    return startBodyHtml(language, loggedInAs, endOfRequest, queryString, "");
  }

  /**
   * This returns the startBodyHtml (with the user's login info inserted if &amp;loginInfo; is
   * present).
   *
   * <p>This is safe to use this after outputStream has been written to -- this won't make a session
   * if the user doesn't have one.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Special case:
   *     "loggedInAsHttps" is for using https without being logged in, but &amp;loginInfo; indicates
   *     user isn't logged in.
   * @param endOfRequest The part after "http.../erddap/". 2022-11-22 AVOID XSS ERROR: THIS MUST BE
   *     VALID! Solution options: 1) percent encode endOfRequest (but I've lost confidence that I
   *     can encode my way out of trouble) 2) ensure endOfRequest and queryString are valid (the
   *     code's logic is not that strict and often goes with "I think the user wants ...") 3)
   *     provice a simple fixed endOfRequest and don't use queryString (since almost always not
   *     validated). I chose #3, so I changed all calls of this method (and getHtmlWriterUtf8() in
   *     Erddap).
   * @param queryString from request.queryString(). May be null here. Must be percent-encoded.
   *     2022-11-22 This is not used for security reasons (and it is not practical to ensure it's
   *     valid and not malicious).
   * @param otherBody other content for the &lt;body&gt; tag, e.g., " onload=\"myFunction()\"".
   */
  public static String startBodyHtml(
      int language, String loggedInAs, String endOfRequest, String queryString, String otherBody) {

    String tErddapUrl = erddapUrl(loggedInAs, language);
    String s =
        startBodyHtmlAr[
            0]; // It's hard for admins to customized this for all languages. So for now, just use
    // language=0.
    s =
        String2.replaceAll(
            s, "&EasierAccessToScientificData;", EasierAccessToScientificDataAr[language]);
    s = String2.replaceAll(s, "&BroughtToYouBy;", BroughtToYouByAr[language]);
    if (String2.isSomething(otherBody))
      s = String2.replaceAll(s, "<body>", "<body " + otherBody + ">");
    s = String2.replaceAll(s, "&loginInfo;", getLoginHtml(language, loggedInAs));
    HtmlWidgets widgets = new HtmlWidgets();
    s =
        String2.replaceAll(
            s,
            "&language;",
            String2.isSomething(endOfRequest)
                ? // isSomething should never be false, but it's safer this way
                widgets.select(
                        "language",
                        "Select the language for all web pages in ERDDAP.",
                        1,
                        TranslateMessages.languageList,
                        TranslateMessages.languageCodeList,
                        language,
                        "onchange=\"window.location.href='"
                            + baseUrl(loggedInAs)
                            + "/"
                            + warName
                            + "/' + "
                            + "(this.selectedIndex == 0? '' : this[this.selectedIndex].value + '/') + '"
                            + // e.g., de
                            XML.encodeAsHTMLAttribute(
                                endOfRequest /* + questionQuery(queryString) */)
                            + "';\"", // query string is already percent encoded.
                        false,
                        "")
                    + htmlTooltipImage(
                        language,
                        loggedInAs,
                        "<img src=\""
                            + tErddapUrl
                            + "/images/TranslatedByGoogle.png\" alt=\"Translated by Google\">"
                            + "<br>"
                            + translationDisclaimer)
                : ""); // we could also redirect to erddap/index.html but that loses the user's
    // place (and this should never happen)

    // String2.log(">> EDStatic startBodyHtml=" + s);
    return String2.replaceAll(s, "&erddapUrl;", erddapUrl(loggedInAs, language));
  }

  /**
   * The endBody HTML.
   *
   * @param language the index of the selected language
   * @param tErddapUrl from EDStatic.erddapUrl(loggedInAs, language) (erddapUrl, or erddapHttpsUrl
   *     if user is logged in)
   * @param loggedInAs
   */
  public static String endBodyHtml(int language, String tErddapUrl, String loggedInAs) {
    String s = String2.replaceAll(endBodyHtmlAr[language], "&erddapUrl;", tErddapUrl);
    if (language > 0)
      s =
          s.replace(
              "<hr>",
              "<br><img src=\""
                  + tErddapUrl
                  + "/images/TranslatedByGoogle.png\" alt=\"Translated by Google\">\n"
                  + htmlTooltipImage(language, loggedInAs, translationDisclaimer)
                  + "<hr>");
    return s;
  }

  /**
   * The content of the legal web page.
   *
   * @param language the index of the selected language
   * @param tErddapUrl from EDStatic.erddapUrl(loggedInAs, language) (erddapUrl, or erddapHttpsUrl
   *     if user is logged in)
   */
  public static String legal(int language, String tErddapUrl) {
    StringBuilder tsb = new StringBuilder(legal);
    String2.replaceAll(tsb, "[standardContact]", standardContactAr[language] + "\n\n");
    String2.replaceAll(tsb, "[standardDataLicenses]", standardDataLicensesAr[language] + "\n\n");
    String2.replaceAll(
        tsb,
        "[standardDisclaimerOfExternalLinks]",
        standardDisclaimerOfExternalLinksAr[language] + "\n\n");
    String2.replaceAll(
        tsb,
        "[standardDisclaimerOfEndorsement]",
        standardDisclaimerOfEndorsementAr[language] + "\n\n");
    String2.replaceAll(tsb, "[standardPrivacyPolicy]", standardPrivacyPolicyAr[language] + "\n\n");
    String2.replaceAll(tsb, "&erddapUrl;", tErddapUrl);
    return tsb.toString();
  }

  /**
   * The HTML for the start of the head section of each web page.
   *
   * @param language the index of the selected language
   * @param addToTitle has not yet been encodeAsHTML(addToTitle).
   */
  public static String startHeadHtml(int language, String tErddapUrl, String addToTitle) {
    String ts = startHeadHtml;

    if (addToTitle.length() > 0)
      ts = String2.replaceAll(ts, "</title>", " - " + XML.encodeAsHTML(addToTitle) + "</title>");
    ts =
        String2.replaceAll(
            ts,
            "&langCode;",
            langCodeAr[language]
                + (language == 0
                    ? ""
                    : "-x-mtfrom-en")); // see https://cloud.google.com/translate/markup

    // better if add <link> to lang=en version of this page, but hard to know enUrl (current English
    // URL)
    // see https://cloud.google.com/translate/markup
    // if (language > 0)
    //    ts += "\n<link rel=\"alternate machine-translated-from\" hreflang=\"en\" href=\"" + enUrl
    // + "\");

    return String2.replaceAll(ts, "&erddapUrl;", tErddapUrl);
  }

  public static String theLongDescriptionHtml(int language, String tErddapUrl) {
    return String2.replaceAll(theLongDescriptionHtmlAr[language], "&erddapUrl;", tErddapUrl);
  }

  public static String theShortDescriptionHtml(int language, String tErddapUrl) {
    String s =
        theShortDescriptionHtmlAr[
            0]; // from datasets.xml or messages.xml.  Always use English, but parts (most) will be
    // translated.
    s = String2.replaceAll(s, "&erddapIs;", erddapIsAr[language]);
    s = String2.replaceAll(s, "&thisParticularErddap;", thisParticularErddapAr[language]);
    s =
        String2.replaceAll(
            s, "[standardShortDescriptionHtml]", standardShortDescriptionHtmlAr[language]);
    s = String2.replaceAll(s, "&requestFormatExamplesHtml;", requestFormatExamplesHtmlAr[language]);
    s = String2.replaceAll(s, "&erddapUrl;", tErddapUrl); // do last
    return s;
  }

  public static String erddapHref(int language, String tErddapUrl) {
    return "<a title=\""
        + clickERDDAPAr[language]
        + "\" \n"
        + "rel=\"start\" "
        + "href=\""
        + tErddapUrl
        + "/index.html\">"
        + ProgramName
        + "</a>";
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
   * This appends an error message to an html page and flushes the writer. This also logs the error.
   *
   * <p>Note that the use of try/catch blocks and htmlForException is necessary because the
   * outputstream is usually gzipped, so erddap can't just catch the exception in doGet try/catch
   * and append error message since original outputstream and writer (which own/control the gzip
   * stream) aren't available.
   *
   * @param language the index of the selected language
   */
  public static String htmlForException(int language, Throwable t) {
    String message = MustBe.throwableToShortString(t);
    return "<p>&nbsp;<hr>\n"
        + "<p><span class=\"warningColor\"><strong>"
        + errorOnWebPageAr[language]
        + "</strong></span>\n"
        + "<pre>"
        + XML.encodeAsPreHTML(message, 100)
        + "</pre><hr><p>&nbsp;<p>\n";
  }

  /**
   * This interrupts/kill all of the thredds in runningThreads. Erddap.destroy calls this when
   * tomcat is stopped.
   */
  public static void destroy() {
    try {
      if (subscriptions != null) {
        subscriptions.close();
      }
      String names[] = String2.toStringArray(runningThreads.keySet().toArray());
      String2.log(
          "\nEDStatic.destroy will try to interrupt nThreads="
              + names.length
              + "\n  threadNames="
              + String2.toCSSVString(names));

      // shutdown Cassandra clusters/sessions
      EDDTableFromCassandra.shutdown();

      // interrupt all of them
      for (String name : names) {
        try {
          Thread thread = runningThreads.get(name);
          if (thread != null && thread.isAlive()) thread.interrupt();
          else runningThreads.remove(name);
        } catch (Throwable t) {
          String2.log(MustBe.throwableToString(t));
        }
      }

      // wait for threads to finish
      int waitedSeconds = 0;
      int maxSeconds = 600; // 10 minutes
      while (true) {
        boolean allDone = true;
        for (int i = 0; i < names.length; i++) {
          try {
            if (names[i] == null) continue; // it has already stopped
            Thread thread = runningThreads.get(names[i]);
            if (thread != null && thread.isAlive()) {
              allDone = false;
              if (waitedSeconds > maxSeconds) {
                String2.log("  " + names[i] + " thread is being stop()ped!!!");
                thread.stop();
                runningThreads.remove(names[i]);
                names[i] = null;
              }
            } else {
              String2.log(
                  "  " + names[i] + " thread recognized the interrupt in " + waitedSeconds + " s");
              runningThreads.remove(names[i]);
              names[i] = null;
            }
          } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            allDone = false;
          }
        }
        if (allDone) {
          String2.log(
              "EDStatic.destroy successfully interrupted all threads in " + waitedSeconds + " s");
          break;
        }
        if (waitedSeconds > maxSeconds) {
          String2.log("!!! EDStatic.destroy is done, but it had to stop() some threads.");
          break;
        }
        Math2.sleep(2000);
        waitedSeconds += 2;
      }

      // finally
      if (useLuceneSearchEngine) String2.log("stopping lucene...");
      useLuceneSearchEngine = false;
      luceneIndexSearcher = null;
      try {
        if (luceneIndexReader != null) luceneIndexReader.close();
      } catch (Throwable t) {
      }
      luceneIndexReader = null;
      luceneDocNToDatasetID = null;

      try {
        if (luceneIndexWriter != null)
          // indices will be thrown away, so don't make pending changes
          luceneIndexWriter.close();
      } catch (Throwable t) {
      }
      luceneIndexWriter = null;

    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
    }
  }

  /**
   * This interrupts the thread and waits up to maxSeconds for it to finish. If it still isn't
   * finished, it is stopped.
   *
   * @return true if it had to be actively stop()'ed.
   */
  public static boolean stopThread(Thread thread, int maxSeconds) {
    boolean stopped = false;
    try {
      if (thread == null) return false;
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
   * This checks if the email thread is live/running and not stalled. If it is stalled, this will
   * stop it.
   *
   * @return true if the email thread is live/running. If false, emailThread will be null.
   */
  public static boolean isEmailThreadRunning() {
    synchronized (emailList) {
      if (emailThread == null) return false;

      if (emailThread.isAlive()) {
        // is it stalled?
        long eTime =
            emailThread
                .elapsedTime(); // elapsed time is the session time, not 1 email.  -1 if no session
        // active
        long maxTime = 5 * Calendar2.MILLIS_PER_MINUTE; // appropriate??? user settable???
        if (eTime > maxTime) {

          // emailThread is stalled; interrupt it
          String tError =
              "%%% EmailThread: EDStatic is interrupting a stalled emailThread ("
                  + Calendar2.elapsedTimeString(eTime)
                  + " > "
                  + Calendar2.elapsedTimeString(maxTime)
                  + ") at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          email(emailEverythingToCsv, "emailThread Stalled", tError);
          String2.log(tError);

          stopThread(emailThread, 10); // short time; it is already in trouble
          // runningThreads.remove   not necessary since new one is put() in below
          emailThread = null;
          return false;
        }
        return true;
      } else {
        // it isn't alive
        String2.log(
            "%%% EmailThread: EDStatic noticed that emailThread isn't alive at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());
        emailThread = null;
        return false;
      }
    }
  }

  /**
   * This checks if the task thread is live/running and not stalled. If it is stalled, this will
   * stop it.
   *
   * @return true if the task thread is live/running. If false, taskThread will be null.
   */
  public static boolean isTaskThreadRunning() {
    synchronized (taskList) {
      if (taskThread == null) return false;

      if (taskThread.isAlive()) {
        // is it stalled?
        long eTime = taskThread.elapsedTime();
        long maxTime = 6 * Calendar2.MILLIS_PER_HOUR; // appropriate??? user settable???
        if (eTime > maxTime) {

          // taskThread is stalled; interrupt it
          String tError =
              "%%% TaskThread ERROR: EDStatic is interrupting a stalled taskThread ("
                  + Calendar2.elapsedTimeString(eTime)
                  + " > "
                  + Calendar2.elapsedTimeString(maxTime)
                  + ") at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          email(emailEverythingToCsv, "taskThread Stalled", tError);
          String2.log(tError);

          stopThread(taskThread, 10); // short time; it is already in trouble
          // runningThreads.remove   not necessary since new one is put() in below
          lastFinishedTask.set(nextTask.get() - 1);
          taskThread = null;
          return false;
        }
        return true;
      } else {
        // it isn't alive
        String2.log(
            "%%% TaskThread: EDStatic noticed that taskThread is finished at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());
        lastFinishedTask.set(nextTask.get() - 1);
        taskThread = null;
        return false;
      }
    }
  }

  /**
   * This checks if the touch thread is live/running and not stalled. If it is stalled, this will
   * stop it.
   *
   * @return true if the touch thread is live/running. If false, touchThread will be null.
   */
  public static boolean isTouchThreadRunning() {
    synchronized (touchList) {
      if (touchThread == null) return false;

      if (touchThread.isAlive()) {
        // is it stalled?
        long eTime = touchThread.elapsedTime(); // for the current touch
        long maxTime = TouchThread.TIMEOUT_MILLIS * 2L;
        if (eTime > maxTime) {

          // touchThread is stalled; interrupt it
          String tError =
              "%%% TouchThread ERROR: EDStatic is interrupting a stalled touchThread ("
                  + Calendar2.elapsedTimeString(eTime)
                  + " > "
                  + Calendar2.elapsedTimeString(maxTime)
                  + ") at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ();
          email(emailEverythingToCsv, "touchThread Stalled", tError);
          String2.log(tError);

          stopThread(touchThread, 10); // short time; it is already in trouble
          // runningThreads.remove   not necessary since new one is put() in below
          lastFinishedTouch.set(nextTouch.get() - 1);
          touchThread = null;
          return false;
        }
        return true;
      } else {
        // it isn't alive
        String2.log(
            "%%% TouchThread: EDStatic noticed that touchThread isn't alive at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());
        lastFinishedTouch.set(nextTouch.get() - 1);
        touchThread = null;
        return false;
      }
    }
  }

  /**
   * This ensures the email thread is running if email system is active. This won't throw an
   * exception.
   */
  public static void ensureEmailThreadIsRunningIfNeeded() {
    synchronized (emailList) {
      // this checks if it is running and not stalled
      if (!emailIsActive || isEmailThreadRunning()) return;

      // emailIsActive && emailThread isn't running
      // need to start a new emailThread
      emailThread = new EmailThread(nextEmail.get());
      runningThreads.put(emailThread.getName(), emailThread);
      String2.log(
          "%%% EmailThread: new emailThread started at "
              + Calendar2.getCurrentISODateTimeStringLocalTZ());
      emailThread.start();
      return;
    }
  }

  /**
   * This ensures the task thread is running if there are tasks to do. This won't throw an
   * exception.
   */
  public static void ensureTaskThreadIsRunningIfNeeded() {
    synchronized (taskList) {
      // this checks if it is running and not stalled
      if (isTaskThreadRunning()) return;

      // taskThread isn't running
      // Are there no tasks to do?
      int nPending = taskList.size() - nextTask.get();
      if (nPending <= 0) return; // no need to start it

      // need to start a new taskThread
      taskThread = new TaskThread(nextTask.get());
      runningThreads.put(taskThread.getName(), taskThread);
      String2.log(
          "%%% TaskThread: new TaskThread started at "
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + " nPendingTasks="
              + nPending);
      taskThread.start();
      return;
    }
  }

  /** This ensures the touch thread is running (always). This won't throw an exception. */
  public static void ensureTouchThreadIsRunningIfNeeded() {
    synchronized (touchList) {
      // this checks if it is running and not stalled
      if (isTouchThreadRunning()) return;

      // touchThread isn't running
      // always start a new touchThread
      touchThread = new TouchThread(nextTouch.get());
      runningThreads.put(touchThread.getName(), touchThread);
      String2.log(
          "%%% TouchThread: new touchThread started at "
              + Calendar2.getCurrentISODateTimeStringLocalTZ());
      touchThread.start();
      return;
    }
  }

  /** This returns the number of unfinished emails. */
  public static int nUnfinishedEmails() {
    return (emailList.size() - lastFinishedEmail.get()) - 1;
  }

  /** This returns the number of unfinished tasks. */
  public static int nUnfinishedTasks() {
    return (taskList.size() - lastFinishedTask.get()) - 1;
  }

  /** This returns the number of unfinished touches. */
  public static int nUnfinishedTouches() {
    return (touchList.size() - lastFinishedTouch.get()) - 1;
  }

  // addEmail is inside EDStatic.email()

  /**
   * This adds a task to the taskList if it (other than TASK_SET_FLAG) isn't already on the
   * taskList.
   *
   * @return the task number that was assigned to the task, or -1 if it was a duplicate task.
   */
  public static int addTask(Object taskOA[]) {
    synchronized (taskList) {

      // Note that all task creators check that
      //   EDStatic.lastFinishedTask >= lastAssignedTask(datasetID).  I.E., tasks are all done,
      // before again creating new tasks.
      // So no need to see if this new task duplicates an existing unfinished task.

      // add the task to the list
      taskList.add(taskOA);
      return taskList.size() - 1;
    }
  }

  /**
   * This adds a touch to the touchList.
   *
   * @return the touch number that was assigned to the touch.
   */
  public static int addTouch(String url) {
    synchronized (touchList) {
      touchList.add(url);
      return touchList.size() - 1;
    }
  }

  /**
   * This returns the Oceanic/Atmospheric Acronyms table: col 0=acronym 1=fullName. <br>
   * Acronyms are case-sensitive, sometimes with common variants included. <br>
   * The table is basically sorted by acronym, but with longer acronyms (e.g., AMSRE) before shorter
   * siblings (e.g., AMSR). <br>
   * Many of these are from https://www.nodc.noaa.gov/General/mhdj_acronyms3.html and
   * http://www.psmsl.org/train_and_info/training/manuals/acronyms.html
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
    URL resourceFile =
        Resources.getResource("gov/noaa/pfel/erddap/util/OceanicAtmosphericAcronyms.tsv");
    List<String> lines = File2.readLinesFromFile(resourceFile, File2.UTF_8, 1);
    int nLines = lines.size();
    for (int i = 1; i < nLines; i++) { // 1 because skip colNames
      String s = lines.get(i).trim();
      if (s.length() == 0 || s.startsWith("//")) continue;
      int po = s.indexOf('\t');
      if (po < 0) po = s.length();
      col1.add(s.substring(0, po).trim());
      col2.add(s.substring(po + 1).trim());
    }
    return table;
  }

  /**
   * This returns the Oceanic/Atmospheric Variable Names table: col 0=variableName 1=fullName. <br>
   * varNames are all lower-case. long_names are mostly first letter of each word capitalized. <br>
   * The table is basically sorted by varName. <br>
   * Many of these are from
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
    URL resourceFile =
        Resources.getResource("gov/noaa/pfel/erddap/util/OceanicAtmosphericVariableNames.tsv");
    List<String> lines = File2.readLinesFromFile(resourceFile, File2.UTF_8, 1);
    int nLines = lines.size();
    for (int i = 1; i < nLines; i++) {
      String s = lines.get(i).trim();
      if (s.length() == 0 || s.startsWith("//")) continue;
      int po = s.indexOf('\t');
      if (po < 0) po = s.length();
      col1.add(s.substring(0, po).trim());
      col2.add(s.substring(po + 1).trim());
    }
    return table;
  }

  /**
   * This returns the Oceanic/Atmospheric Acronyms table as a Table: col0=acronym, col1=fullName.
   * THIS IS ONLY FOR GenerateDatasetsXml THREADS -- a few common acronyms are removed.
   *
   * @return the oceanic/atmospheric variable names table with some common acronyms removed
   * @throws Exception if trouble (e.g., file not found)
   */
  public static Table gdxAcronymsTable() throws Exception {
    if (gdxAcronymsTable == null) {
      Table table = oceanicAtmosphericAcronymsTable();
      StringArray acronymSA = (StringArray) table.getColumn(0);

      // remove some really common acronyms I don't want to expand
      BitSet keep = new BitSet();
      keep.set(0, acronymSA.size());
      String common[] = { // "DOC", "DOD", "DOE", "USDOC", "USDOD", "USDOE",
        "NOAA", "NASA", "US"
      };
      for (String s : common) {
        int po = acronymSA.indexOf(s);
        if (po >= 0) keep.clear(po);
      }
      table.justKeep(keep);

      gdxAcronymsTable = table; // swap into place
    }
    return gdxAcronymsTable;
  }

  /**
   * This returns the Oceanic/Atmospheric Acronyms table as a HashMap: key=acronym, value=fullName.
   * THIS IS ONLY FOR GenerateDatasetsXml THREADS -- a few common acronyms are removed.
   *
   * @return the oceanic/atmospheric variable names table as a HashMap
   * @throws Exception if trouble (e.g., file not found)
   */
  public static Map<String, String> gdxAcronymsHashMap() throws Exception {
    if (gdxAcronymsHashMap == null) {
      Table table = gdxAcronymsTable();
      StringArray acronymSA = (StringArray) table.getColumn(0);
      StringArray fullNameSA = (StringArray) table.getColumn(1);
      int n = table.nRows();
      Map<String, String> hm = new HashMap<>();
      for (int i = 1; i < n; i++) hm.put(acronymSA.get(i), fullNameSA.get(i));
      gdxAcronymsHashMap = hm; // swap into place
    }
    return gdxAcronymsHashMap;
  }

  /**
   * This returns the Oceanic/Atmospheric Variable Names table as a HashMap: key=variableName,
   * value=fullName. THIS IS ONLY FOR GenerateDatasetsXml THREADS.
   *
   * @return the oceanic/atmospheric variable names table as a HashMap
   * @throws Exception if trouble (e.g., file not found)
   */
  public static Map<String, String> gdxVariableNamesHashMap() throws Exception {
    if (gdxVariableNamesHashMap == null) {
      Table table = oceanicAtmosphericVariableNamesTable();
      StringArray varNameSA = (StringArray) table.getColumn(0);
      StringArray fullNameSA = (StringArray) table.getColumn(1);
      int n = table.nRows();
      Map<String, String> hm = new HashMap<>();
      for (int i = 1; i < n; i++) hm.put(varNameSA.get(i), fullNameSA.get(i));
      gdxVariableNamesHashMap = hm; // swap into place
    }
    return gdxVariableNamesHashMap;
  }

  /**
   * This returns the FIPS county table: col 0=FIPS (5-digit-FIPS), 1=Name (ST, County Name). <br>
   * States are included (their last 3 digits are 000). <br>
   * The table is sorted (case insensitive) by the county column. <br>
   * The most official source is http://www.itl.nist.gov/fipspubs/fip6-4.htm
   *
   * <p>The table is modified from http://www.census.gov/datamap/fipslist/AllSt.txt . It includes
   * the Appendix A and B counties from U.S. protectorates and county-equivalent entities of the
   * freely associated atates from http://www.itl.nist.gov/fipspubs/co-codes/states.txt . I changed
   * "lsabela" PR to "Isabela".
   *
   * @return the FIPS county table
   * @throws Exception if trouble (e.g., file not found)
   */
  public static Table fipsCountyTable() throws Exception {
    URL resourceFile = Resources.getResource("gov/noaa/pfel/erddap/util/FipsCounty.tsv");
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(resourceFile.openStream(), StandardCharsets.UTF_8));
    Table table = new Table();
    table.readASCII(
        resourceFile.getFile(),
        reader,
        "",
        "",
        0,
        1,
        "",
        null,
        null,
        null,
        null,
        false); // false = don't simplify
    return table;
  }

  /**
   * This returns the complete list of CF Standard Names as a table with 1 column.
   *
   * @return the complete list of CF Standard Names as a table with 1 column
   * @throws Exception if trouble (e.g., file not found)
   */
  public static Table keywordsCfTable() throws Exception {
    URL resourceFile = Resources.getResource("gov/noaa/pfel/erddap/util/cfStdNames.txt");
    StringArray sa = StringArray.fromFileUtf8(resourceFile);
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
    URL resourceFile = Resources.getResource("gov/noaa/pfel/erddap/util/gcmdScienceKeywords.txt");
    StringArray sa = StringArray.fromFileUtf8(resourceFile);
    Table table = new Table();
    table.addColumn("GcmdScienceKeywords", sa);
    return table;
  }

  /**
   * This returns the complete CF to GCMD conversion information as a table with 1 column. The GCMD
   * to CF conversion information can be derived from this.
   *
   * @return the CF to GCMD conversion information as a table with 1 column
   * @throws Exception if trouble (e.g., file not found)
   */
  public static Table keywordsCfToGcmdTable() throws Exception {
    URL resourceFile = Resources.getResource("gov/noaa/pfel/erddap/util/CfToGcmd.txt");
    StringArray sa = StringArray.fromFileUtf8(resourceFile);
    Table table = new Table();
    table.addColumn("CfToGcmd", sa);
    return table;
  }

  /**
   * This returns true during the initial loadDatasets.
   *
   * @return true during the initial loadDatasets, else false.
   */
  public static boolean initialLoadDatasets() {
    return majorLoadDatasetsTimeSeriesSB.length() == 0;
  }

  /** This is called by the ERDDAP constructor to initialize Lucene. */
  public static void initializeLucene() {
    // ERDDAP consciously doesn't use any stopWords (words not included in the index, e.g. a, an,
    // the)
    // 1) this matches the behaviour of the original searchEngine
    // 2) this is what users expect, e.g., when searching for a phrase
    // 3) the content here isn't prose, so the stop words aren't nearly as common
    luceneAnalyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET); // the set of stopWords
    // it is important that the queryParser use the same analyzer as the indexWriter
    luceneQueryParser = new QueryParser(luceneDefaultField, luceneAnalyzer);
  }

  /**
   * This creates an IndexWriter. Normally, this is created once in RunLoadDatasets. But if trouble,
   * a new one will be created.
   *
   * @throws RuntimeException if trouble
   */
  public static void createLuceneIndexWriter(boolean firstTime) {

    try {
      String2.log("createLuceneIndexWriter(" + firstTime + ")");
      long tTime = System.currentTimeMillis();

      // if this is being called, directory shouldn't be locked
      // see javaDocs for indexWriter.close()

      // create indexWriter
      IndexWriterConfig lucConfig = new IndexWriterConfig(luceneAnalyzer);
      lucConfig.setOpenMode(
          firstTime
              ? IndexWriterConfig.OpenMode.CREATE
              : IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
      lucConfig.setInfoStream(verbose ? new PrintStream(new String2LogOutputStream()) : null);

      luceneIndexWriter = new IndexWriter(luceneDirectory, lucConfig);
      String2.log(
          "  createLuceneIndexWriter finished.  time="
              + (System.currentTimeMillis() - tTime)
              + "ms");
    } catch (Throwable t) {
      useLuceneSearchEngine = false;
      throw new RuntimeException(t);
    }
  }

  /**
   * This returns the Lucene IndexSearcher (thread-safe). IndexSearch uses IndexReader (also
   * thread-safe). IndexReader works on a snapshot of an index, so it is recreated if flagged at end
   * of LoadDatasetsevery via needNewLuceneIndexReader.
   *
   * @return a luceneIndexSearcher (thread-safe) or null if trouble.
   */
  public static IndexSearcher luceneIndexSearcher() {

    // synchronize
    synchronized (luceneIndexReaderLock) {

      // need a new indexReader?
      // (indexReader is thread-safe, but only need one)
      if (luceneIndexReader == null || needNewLuceneIndexReader || luceneIndexSearcher == null) {

        luceneIndexSearcher = null;
        if (luceneIndexReader != null) {
          try {
            luceneIndexReader.close();
          } catch (Throwable t2) {
          }
          luceneIndexReader = null;
        }
        needNewLuceneIndexReader = true;

        // create a new one
        try {
          long rTime = System.currentTimeMillis();
          luceneIndexReader = DirectoryReader.open(luceneDirectory); // read-only=true
          luceneIndexSearcher = new IndexSearcher(luceneIndexReader);
          String2.log(
              "  new luceneIndexReader+Searcher time="
                  + (System.currentTimeMillis() - rTime)
                  + "ms");

          // create the luceneDatasetIDFieldCache
          // save memory by sharing the canonical strings
          // (EDD.ensureValid makes datasetID's canonical)
          // rTime = System.currentTimeMillis();
          // luceneDatasetIDFieldCache = FieldCache.DEFAULT.getStrings(luceneIndexReader,
          //    "datasetID");
          // int n = luceneDatasetIDFieldCache.length;
          // for (int i = 0; i < n; i++)
          //    luceneDatasetIDFieldCache[i] = String2.canonical(luceneDatasetIDFieldCache[i]);
          // String2.log("  new luceneDatasetIDFieldCache time=" +
          //    (System.currentTimeMillis() - rTime) + "ms");

          // if successful, we no longer needNewLuceneIndexReader
          needNewLuceneIndexReader = false;
          // if successful, fall through

        } catch (Throwable t) {
          // this may occur before indexes have initially been created
          // so don't give up on lucene
          if (!initialLoadDatasets()) {
            String subject = String2.ERROR + " while creating Lucene Searcher";
            String msg = MustBe.throwableToString(t);
            email(emailEverythingToCsv, subject, msg);
            String2.log(subject + "\n" + msg);
          }

          // clear out old one
          luceneIndexSearcher = null;

          if (luceneIndexReader != null) {
            try {
              luceneIndexReader.close();
            } catch (Throwable t2) {
            }
            luceneIndexReader = null;
          }
          needNewLuceneIndexReader = true;

          // return
          return null;
        }
      }

      return luceneIndexSearcher;
    }
  }

  /**
   * This parses a query with luceneQueryParser (not thread-safe).
   *
   * @param searchString the user's searchString, but modified slightly for Lucene
   * @return a Query, or null if trouble
   */
  public static Query luceneParseQuery(String searchString) {

    // queryParser is not thread-safe, so re-use it in a synchronized block
    // (It is fast, so synchronizing on one parser shouldn't be a bottleneck.
    synchronized (luceneQueryParser) {
      try {
        // long qTime = System.currentTimeMillis();
        // String2.log("  luceneParseQuery finished.  time=" + (System.currentTimeMillis() - qTime)
        // + "ms"); //always 0
        return luceneQueryParser.parse(searchString);
      } catch (Throwable t) {
        String2.log(
            "Lucene failed to parse searchString="
                + searchString
                + "\n"
                + MustBe.throwableToString(t));
        return null;
      }
    }
  }

  /**
   * This gets the raw requested (or inferred) page number and itemsPerPage by checking the request
   * parameters.
   *
   * @param request
   * @return int[2] [0]=page (may be invalid, e.g., -5 or Integer.MAX_VALUE) [1]=itemsPerPage (may
   *     be invalid, e.g., -5 or Integer.MAX_VALUE)
   */
  public static int[] getRawRequestedPIpp(HttpServletRequest request) {

    return new int[] {
      String2.parseInt(request.getParameter("page")),
      String2.parseInt(request.getParameter("itemsPerPage"))
    };
  }

  /**
   * This gets the requested (or inferred) page number and itemsPerPage by checking the request
   * parameters.
   *
   * @param request
   * @return int[2] [0]=page (will be 1..., but may be too big), [1]=itemsPerPage (will be 1...),
   */
  public static int[] getRequestedPIpp(HttpServletRequest request) {

    int iar[] = getRawRequestedPIpp(request);

    // page is 1..
    if (iar[0] < 1 || iar[0] == Integer.MAX_VALUE) iar[0] = 1; // default

    // itemsPerPage
    if (iar[1] < 1 || iar[1] == Integer.MAX_VALUE) iar[1] = defaultItemsPerPage;

    return iar;
  }

  /**
   * This returns the .jsonp=[functionName] part of the request (percent encoded) or "". If not "",
   * it will have "&" at the end.
   *
   * @param language the index of the selected language
   * @param request
   * @return the .jsonp=[functionName] part of the request (percent encoded) or "". If not "", it
   *     will have "&" at the end. If the query has a syntax error, this returns "". If the
   *     !String2.isJsonpNameSafe(functionName), this throws a SimpleException.
   */
  public static String passThroughJsonpQuery(int language, HttpServletRequest request) {
    String jsonp = "";
    try {
      String parts[] =
          Table.getDapQueryParts(
              request.getQueryString()); // decoded.  Does some validity checking.
      jsonp = String2.stringStartsWith(parts, ".jsonp="); // may be null
      if (jsonp == null) return "";
      String functionName = jsonp.substring(7); // it will be because it starts with .jsonp=
      if (!String2.isJsonpNameSafe(functionName))
        throw new SimpleException(
            bilingual(language, errorJsonpFunctionNameAr[0], errorJsonpFunctionNameAr[language]));
      return ".jsonp=" + SSR.minimalPercentEncode(functionName) + "&";
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      return "";
    }
  }

  /**
   * This extracts the page= and itemsPerPage= parameters of the request (if any) and returns them
   * lightly validated and formatted for a URL (e.g., "page=1&amp;itemsPerPage=1000").
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
   * This is like passThroughPIppQuery, but the ampersand is XML encoded so it is ready to be put
   * into HTML.
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
   * This calculates the requested (or inferred) page number and itemsPerPage by checking the
   * request parameters.
   *
   * @param request
   * @param nItems e.g., total number of datasets from search results
   * @return int[4] [0]=page (will be 1..., but may be too big), [1]=itemsPerPage (will be 1...),
   *     [2]=startIndex (will be 0..., but may be too big), [3]=lastPage with items (will be 1...).
   *     Note that page may be greater than lastPage (caller should write error message to user).
   */
  public static int[] calculatePIpp(HttpServletRequest request, int nItems) {

    int pipp[] = getRequestedPIpp(request);
    int page = pipp[0];
    int itemsPerPage = pipp[1];
    int startIndex = Math2.narrowToInt((page - 1) * (long) itemsPerPage); // 0..
    int lastPage = Math.max(1, Math2.hiDiv(nItems, itemsPerPage));

    return new int[] {page, itemsPerPage, startIndex, lastPage};
  }

  /**
   * This returns the error String[2] if a search yielded no matches.
   *
   * @param language the index of the selected language
   */
  public static String[] noSearchMatch(int language, String searchFor) {
    if (searchFor == null) searchFor = "";
    return new String[] {
      MustBe.THERE_IS_NO_DATA,
      (searchFor.length() > 0 ? searchSpellingAr[language] + " " : "")
          + (searchFor.indexOf(' ') >= 0 ? searchFewerWordsAr[language] : "")
    };
  }

  /**
   * This returns the error String[2] if page &gt; lastPage.
   *
   * @param language the index of the selected language
   * @param page
   * @param lastPage
   * @return String[2] with the two Strings
   */
  public static String[] noPage(int language, int page, int lastPage) {
    return new String[] {
      MessageFormat.format(noPage1Ar[language], "" + page, "" + lastPage),
      MessageFormat.format(noPage2Ar[language], "" + page, "" + lastPage)
    };
  }

  /**
   * This returns the nMatchingDatasets HTML message, with paging options.
   *
   * @param language the index of the selected language
   * @param nMatches this must be 1 or more
   * @param page
   * @param lastPage
   * @param relevant true=most relevant first, false=sorted alphabetically
   * @param urlWithQuery percentEncoded, but not HTML/XML encoded, e.g.,
   *     https://coastwatch.pfeg.noaa.gov/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=temperature%20wind
   * @return string with HTML content
   */
  public static String nMatchingDatasetsHtml(
      int language, int nMatches, int page, int lastPage, boolean relevant, String urlWithQuery) {

    if (nMatches == 1) return nMatching1Ar[language];

    StringBuilder results =
        new StringBuilder(
            MessageFormat.format(
                    relevant
                        ? nMatchingMostRelevantAr[language]
                        : nMatchingAlphabeticalAr[language],
                    "" + nMatches)
                + "\n");

    if (lastPage > 1) {

      // figure out where page number is so replaceable
      int pagePo = urlWithQuery.indexOf("?page=");
      if (pagePo < 0) pagePo = urlWithQuery.indexOf("&page=");
      if (pagePo < 0) {
        pagePo = urlWithQuery.length();
        urlWithQuery += (urlWithQuery.indexOf('?') < 0 ? "?" : "&") + "page=" + page;
      }
      int pageNumberPo = pagePo + 6;

      int ampPo = urlWithQuery.indexOf('&', pageNumberPo);
      if (ampPo < 0) ampPo = urlWithQuery.length();

      String url0 = "&nbsp;<a ";
      String url1 =
          "href=\"" + XML.encodeAsHTMLAttribute(urlWithQuery.substring(0, pageNumberPo)); // + p
      String url2 = XML.encodeAsHTMLAttribute(urlWithQuery.substring(ampPo)) + "\">"; // + p
      String url3 = "</a>&nbsp;\n";
      String prev = "rel=\"prev\" ";
      String next = "rel=\"next\" ";
      String bmrk = "rel=\"bookmark\" ";

      // links, e.g. if page=5 and lastPage=12: _1 ... _4  5 _6 ... _12
      StringBuilder sb = new StringBuilder();
      if (page >= 2) sb.append(url0 + (page == 2 ? prev : bmrk) + url1 + 1 + url2 + 1 + url3);
      if (page >= 4) sb.append("...\n");
      if (page >= 3)
        sb.append(url0 + (page > 2 ? prev : bmrk) + url1 + (page - 1) + url2 + (page - 1) + url3);
      sb.append(
          "&nbsp;"
              + page
              + "&nbsp;("
              + nMatchingCurrentAr[language]
              + ")&nbsp;\n"); // always show current page
      if (page <= lastPage - 2)
        sb.append(
            url0
                + (page < lastPage - 1 ? next : bmrk)
                + url1
                + (page + 1)
                + url2
                + (page + 1)
                + url3);
      if (page <= lastPage - 3) sb.append("...\n");
      if (page <= lastPage - 1)
        sb.append(
            url0 + (page == lastPage - 1 ? next : bmrk) + url1 + lastPage + url2 + lastPage + url3);

      // append to results
      results.append(
          "&nbsp;&nbsp;"
              + MessageFormat.format(
                  nMatchingPageAr[language], "" + page, "" + lastPage, sb.toString())
              + "\n");
    }

    return results.toString();
  }

  /** If query is null or "", this returns ""; otherwise, this returns "?" + query. */
  public static String questionQuery(String query) {
    return query == null || query.length() == 0 ? "" : "?" + query;
  }

  /**
   * This updates out-of-date http: references to https: within a string. This is very safe won't
   * otherwise change the string (even "" or null).
   */
  public static String updateUrls(String s) {
    if (!String2.isSomething(s)) return s;

    // change some non-http things
    StringBuilder sb = new StringBuilder(s);
    String2.replaceAll(
        sb, // reversed in naming_authority
        "gov.noaa.pfel.",
        "gov.noaa.pfeg.");

    int n = updateUrlsFrom.length;
    for (int i = 0; i < n; i++) String2.replaceAll(sb, updateUrlsFrom[i], updateUrlsTo[i]);
    return sb.toString();
  }

  /**
   * This calls updateUrls for every String attribute (except EDStatic.updateUrlsSkipAttributes) and
   * writes changes to addAtts.
   *
   * @param sourceAtts may be null
   * @param addAtts mustn't be null.
   */
  public static void updateUrls(Attributes sourceAtts, Attributes addAtts) {
    // get all the attribute names
    HashSet<String> hs = new HashSet<>();
    String names[];
    if (sourceAtts != null) {
      names = sourceAtts.getNames();
      hs.addAll(Arrays.asList(names));
    }
    names = addAtts.getNames();
    hs.addAll(Arrays.asList(names));
    names = hs.toArray(new String[] {});

    // updateUrls in all attributes
    for (String name : names) {
      if (String2.indexOf(updateUrlsSkipAttributes, name) >= 0) continue;
      PrimitiveArray pa = addAtts.get(name);
      if (pa == null && sourceAtts != null) pa = sourceAtts.get(name);
      if (pa != null && pa.size() > 0 && pa.elementType() == PAType.STRING) {
        String oValue = pa.getString(0);
        String value = updateUrls(oValue);
        if (!value.equals(oValue)) addAtts.set(name, value);
      }
    }
  }

  /**
   * Create tasks to download files so a local dir mimics a remote dir. <br>
   * This won't throw an exception.
   *
   * @param maxTasks This let's you just see what would happen (0), or just make a limited or
   *     unlimited (Integer.MAX_VALUE) number of download tasks.
   * @param tDatasetID
   */
  public static int makeCopyFileTasks(
      String tClassName,
      int maxTasks,
      String tDatasetID,
      String tSourceUrl,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tLocalDir) {

    tSourceUrl = File2.addSlash(tSourceUrl);
    tLocalDir = File2.addSlash(tLocalDir);

    if (verbose)
      String2.log(
          "* "
              + tDatasetID
              + " "
              + tClassName
              + ".makeCopyFileTasks  pathRegex="
              + tPathRegex
              + "  fileNameRegex="
              + tFileNameRegex
              + "\n"
              + "from "
              + tSourceUrl
              + "\n"
              + "to   "
              + tLocalDir);
    long startTime = System.currentTimeMillis();
    int nFilesToDownload = 0;
    int lastTask = -1;

    try {
      // if previous tasks are still running, return
      ensureTaskThreadIsRunningIfNeeded(); // ensure info is up-to-date
      Integer datasetLastAssignedTask = lastAssignedTask.get(tDatasetID);
      boolean pendingTasks =
          datasetLastAssignedTask != null && lastFinishedTask.get() < datasetLastAssignedTask;
      if (verbose)
        String2.log(
            "  "
                + tClassName
                + ".makeCopyFileTasks: lastFinishedTask="
                + lastFinishedTask.get()
                + " < datasetLastAssignedTask("
                + tDatasetID
                + ")="
                + datasetLastAssignedTask
                + "? pendingTasks="
                + pendingTasks);
      if (pendingTasks) return 0;

      // make sure local dir exists or can be created
      File2.makeDirectory(tLocalDir); // throws exception if unable to comply

      // get remote file info
      Table remoteFiles =
          FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
              tSourceUrl, tFileNameRegex, tRecursive, tPathRegex, false); // tDirectoriesToo
      if (remoteFiles.nRows() == 0) {
        if (verbose)
          String2.log("  " + tClassName + ".makeCopyFileTasks: no matching source files.");
        return 0;
      }
      remoteFiles.leftToRightSort(2); // should be already
      StringArray remoteDirs = (StringArray) remoteFiles.getColumn(FileVisitorDNLS.DIRECTORY);
      StringArray remoteNames = (StringArray) remoteFiles.getColumn(FileVisitorDNLS.NAME);
      LongArray remoteLastMod = (LongArray) remoteFiles.getColumn(FileVisitorDNLS.LASTMODIFIED);
      LongArray remoteSize = (LongArray) remoteFiles.getColumn(FileVisitorDNLS.SIZE);

      // get local file info
      Table localFiles =
          FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
              tLocalDir, tFileNameRegex, tRecursive, tPathRegex, false); // tDirectoriesToo
      localFiles.leftToRightSort(2); // should be already
      StringArray localDirs = (StringArray) localFiles.getColumn(FileVisitorDNLS.DIRECTORY);
      StringArray localNames = (StringArray) localFiles.getColumn(FileVisitorDNLS.NAME);
      LongArray localLastMod = (LongArray) localFiles.getColumn(FileVisitorDNLS.LASTMODIFIED);
      LongArray localSize = (LongArray) localFiles.getColumn(FileVisitorDNLS.SIZE);

      // make tasks to download files
      int nRemote = remoteNames.size();
      int nLocal = localNames.size();
      int localI = 0; // next to look at
      for (int remoteI = 0; remoteI < nRemote; remoteI++) {
        try {
          String remoteRelativeDir = remoteDirs.get(remoteI).substring(tSourceUrl.length());

          // skip local files with DIRS which are less than the remote file's dir
          while (localI < nLocal
              && localDirs.get(localI).substring(tLocalDir.length()).compareTo(remoteRelativeDir)
                  < 0) localI++;

          // skip local files in same dir with FILENAMES which are less than the remote file
          while (localI < nLocal
              && localDirs.get(localI).substring(tLocalDir.length()).equals(remoteRelativeDir)
              && localNames.get(localI).compareTo(remoteNames.get(remoteI)) < 0) localI++;

          // same local file exists?
          String reason = "new remote file";
          if (localI < nLocal
              && localDirs.get(localI).substring(tLocalDir.length()).equals(remoteRelativeDir)
              && localNames.get(localI).equals(remoteNames.get(remoteI))) {
            // same or vague lastMod and size

            if (remoteLastMod.get(remoteI) != Long.MAX_VALUE
                && remoteLastMod.get(remoteI) != localLastMod.get(localI)) {
              // remoteLastMod may be imprecise (e.g., to the minute),
              // but local should be set to exactly match it whatever it is
              reason = "different lastModified";
            } else if (remoteSize.get(remoteI) != Long.MAX_VALUE
                && (remoteSize.get(remoteI) < localSize.get(localI) * 0.9
                    || remoteSize.get(remoteI) > localSize.get(localI) * 1.1)) {
              // remote size may be imprecise (e.g., 1.1MB)
              // (also, does remote mean KB=1000 or 1024?!)
              // so this just tests match within +/-10%
              reason = "different size";
            } else {
              // local is ~equivalent of remote
              localI++;
              continue;
            }
            reason = "different size";
          }

          // make a task to download remoteFile to localFile
          // taskOA[1]=remoteUrl, taskOA[2]=fullFileName, taskOA[3]=lastModified (Long)
          Object taskOA[] = new Object[7];
          taskOA[0] = TaskThread.TASK_DOWNLOAD;
          taskOA[1] = remoteDirs.get(remoteI) + remoteNames.get(remoteI);
          taskOA[2] = tLocalDir + remoteRelativeDir + remoteNames.get(remoteI);
          taskOA[3] = remoteLastMod.get(remoteI); // or if unknown?
          nFilesToDownload++;
          int tTaskNumber =
              nFilesToDownload <= maxTasks ? (lastTask = addTask(taskOA)) : -nFilesToDownload;
          if (reallyVerbose || (verbose && nFilesToDownload == 1))
            String2.log(
                (tTaskNumber < 0 ? "% didn't create" : "% created")
                    + " task#"
                    + Math.abs(tTaskNumber)
                    + " TASK_DOWNLOAD reason="
                    + reason
                    + "\n    from="
                    + taskOA[1]
                    + "\n    to="
                    + taskOA[2]);
        } catch (Exception e) {
          String2.log(
              tClassName
                  + ".makeCopyFileTasks caught "
                  + String2.ERROR
                  + " while processing file #"
                  + remoteI
                  + "="
                  + remoteDirs.get(remoteI)
                  + remoteNames.get(remoteI)
                  + "\n"
                  + MustBe.throwableToString(e));
        }
      }

      // create task to flag dataset to be reloaded
      if (lastTask >= 0) {
        Object taskOA[] = new Object[2];
        taskOA[0] = TaskThread.TASK_SET_FLAG;
        taskOA[1] = tDatasetID;
        lastTask = addTask(taskOA); // TASK_SET_FLAG will always be added
        if (reallyVerbose)
          String2.log("% created task#" + lastTask + " TASK_SET_FLAG " + tDatasetID);
        lastAssignedTask.put(tDatasetID, lastTask);
        ensureTaskThreadIsRunningIfNeeded(); // ensure info is up-to-date

        if (EDStatic.forceSynchronousLoading) {
          while (lastFinishedTask.get() < lastTask) {
            Thread.sleep(2000);
          }
        }
      }

      if (verbose)
        String2.log(
            "% "
                + tDatasetID
                + " "
                + tClassName
                + ".makeCopyFileTasks finished."
                + " nFilesToDownload="
                + nFilesToDownload
                + " maxTasks="
                + maxTasks
                + " time="
                + (System.currentTimeMillis() - startTime)
                + "ms");

    } catch (Throwable t) {
      if (verbose)
        String2.log(
            "ERROR in "
                + tClassName
                + ".makeCopyFileTasks for datasetID="
                + tDatasetID
                + "\n"
                + MustBe.throwableToString(t));
    }
    return nFilesToDownload;
  }

  /**
   * This checks if this request should be shed because not much free memory available. Note that
   * shedThisRequest is more eager to reject a request, than Math2.ensureMemoryAvailable is to cause
   * an in-process request to be stopped.
   *
   * @param lotsMemoryNeeded Use true if this request may require lots of memory. Use false if this
   *     request probably doesn't need much memory. If memory use is super high, all requests will
   *     be shed.
   * @return true if this send an error message to user throws InterruptedException
   */
  public static boolean shedThisRequest(
      int language, int requestNumber, HttpServletResponse response, boolean lotsMemoryNeeded)
      throws InterruptedException {
    // If shed (maybe in another thread) just called gc, memory use was recently high. Wait until
    // shortSleep is finished.
    long timeSinceGc = System.currentTimeMillis() - Math2.timeGCLastCalled;
    if (timeSinceGc < Math2.shortSleep) {
      // A problem with this is that multiple threads may wait for gc to finish,
      //  see enough memory, then all start and each use lots of memory.
      Thread.sleep(Math2.shortSleep - timeSinceGc);
      timeSinceGc = Math2.shortSleep;
    }

    // always: if >=2000ms since gc and memory use is high, call gc
    long inUse = Math2.getMemoryInUse();
    if (timeSinceGc >= 3L * Math2.shortSleep
        && inUse
            >= Math2
                .halfMemory) { // This is arbitrary. I don't want to call gc too often but I don't
      // want to shed needlessly.
      inUse =
          Math2.gcAndWait(
              "shedThisRequest"); // waits Math2.shortSleep   //in shedThisRequest   //a diagnostic
      // is always logged
    }

    // if memory use is now low enough for this request, return false
    long tLimit = lotsMemoryNeeded ? Math2.halfMemory : Math2.highMemory; // 0.5*max : .65*max
    if (inUse <= tLimit) {
      if (inUse <= Math2.maxMemory / 4)
        lastActiveRequestReportTime = 0; // the previous dangerousMemory inUse has been solved
      return false;
    }

    // if memory use is dangerously high and I haven't reported this incident, report it
    if (inUse >= Math2.dangerousMemory && lastActiveRequestReportTime == 0) {
      lastActiveRequestReportTime =
          System.currentTimeMillis(); // do first so other threads don't also report this
      dangerousMemoryEmails.incrementAndGet();
      activeRequests.remove(requestNumber + ""); // don't blame this request
      String activeRequestLines[] = activeRequests.values().toArray(new String[0]);
      Arrays.sort(activeRequestLines);
      String report =
          "Dangerously high memory use!!! inUse="
              + (inUse / Math2.BytesPerMB)
              + "MB > dangerousMemory="
              + (Math2.dangerousMemory / Math2.BytesPerMB)
              + "MB.\n"
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + ", ERDDAP version="
              + erddapVersion
              + ", url="
              + preferredErddapUrl
              + "\n"
              + "Please forward this email (after removing private information) to erd.data@noaa.gov,\n"
              + "so we can minimize this problem in future versions of ERDDAP.\n\n"
              + "Active requests:\n"
              + String2.toNewlineString(activeRequestLines);
      String2.log(report);
      email(emailEverythingToCsv, "Dangerously High Memory Use!!!", report);
    }

    // memory use is too high, so shed this request
    String2.log(
        "shedThisRequest #"
            + requestsShed.getAndIncrement()
            + // since last Major LoadDatasets
            ", request #"
            + requestNumber
            + ", lotsOfMemoryNeeded="
            + lotsMemoryNeeded
            + ", memoryInUse="
            + (inUse / Math2.BytesPerMB)
            + "MB > tLimit="
            + (tLimit / Math2.BytesPerMB)
            + "MB");
    lowSendError( // it sleeps for slowDownTroubleMillis
        requestNumber,
        response,
        503, // Service Unavailable
        waitThenTryAgainAr[language]);
    return true;
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
   * org.apache.catalina.connector.ClientAbortException is hard to catch since catalina code is
   * linked in after deployment. So this looks for the string.
   *
   * <p>Normal use: Use this first thing in catch, before throwing WaitThenTryAgainException.
   *
   * @param t the exception which will be thrown again if it is a ClientAbortException
   * @throws Throwable
   */
  public static void rethrowClientAbortException(Throwable t) throws Throwable {
    if (isClientAbortException(t)) throw t;
  }

  /**
   * Set the standard DAP header information. Call this before getting outputStream.
   *
   * @param response
   * @throws Throwable if trouble
   */
  public static void standardDapHeader(HttpServletResponse response) throws Throwable {
    String rfc822date = Calendar2.getCurrentRFC822Zulu();
    response.setHeader("Date", rfc822date); // DAP 2.0, 7.1.4.1
    response.setHeader(
        "Last-Modified", rfc822date); // DAP 2.0, 7.1.4.2   //this is not a good implementation
    // response.setHeader("Server", );                   //DAP 2.0, 7.1.4.3  optional
    response.setHeader(
        "xdods-server",
        serverVersion); // DAP 2.0, 7.1.7 (http header field names are case-insensitive)
    response.setHeader(programname + "-server", erddapVersion);
  }

  /** This returns the requester's ip addresses (from x-forwarded-for) or "(unknownIPAddress)". */
  public static String getIPAddress(HttpServletRequest request) {

    // getRemoteHost(); always returns our proxy server (never changes)
    String ipAddress = request.getHeader("True-Client-IP");
    if (ipAddress == null) {
      ipAddress = request.getHeader("x-forwarded-for");
    }
    if (ipAddress == null) {
      ipAddress = "";
    } else {
      // if csv, get last part
      // see https://en.wikipedia.org/wiki/X-Forwarded-For
      int cPo = ipAddress.lastIndexOf(',');
      if (cPo >= 0) ipAddress = ipAddress.substring(cPo + 1);
    }
    ipAddress = ipAddress.trim();
    if (ipAddress.length() == 0) ipAddress = ipAddressUnknown;
    return ipAddress;
  }

  /**
   * Given a throwable t, this sends an appropriate HTTP error code and a DAP-formatted dods-error
   * response message. Most users will call return in their method after calling this since the
   * response is committed and closed.
   *
   * <p>NOTE that these search for English words in the Throwable, so depend on the English version
   * of the text being present.
   *
   * @param requestNumber The requestNumber assigned to this request by doGet().
   * @param request The user's request.
   * @param response The response to be written to.
   */
  public static void sendError(
      int requestNumber, HttpServletRequest request, HttpServletResponse response, Throwable t)
      throws ServletException {

    // defaults
    int errorNo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // http error 500
    String tError = "Internal server error.";

    try {
      if (isClientAbortException(t)) {
        String2.log(
            "*** sendError for request #"
                + requestNumber
                + " caught "
                + String2.ERROR
                + "=ClientAbortException");
        return; // do nothing
      }

      // String2.log("Bob: sendErrorCode t.toString=" + t.toString());
      tError = MustBe.getShortErrorMessage(t);
      String tRequestURI = request == null ? "[unknown requestURI]" : request.getRequestURI();
      String tRequest =
          tRequestURI + (request == null ? "" : questionQuery(request.getQueryString()));
      // String2.log(">> tError=" + tError);

      // log the error
      String tErrorLC = tError.toLowerCase();
      if (tError.indexOf(resourceNotFoundAr[0]) >= 0
          || tError.indexOf(MustBe.THERE_IS_NO_DATA)
              >= 0) { // check this first, since may also be Query error
        errorNo = HttpServletResponse.SC_NOT_FOUND; // http error 404  (might succeed later)
        // I wanted to use 204 No Content or 205 (similar) but browsers don't show any change for
        // these codes

      } else if (tError.indexOf(queryErrorAr[0]) >= 0) {
        errorNo = HttpServletResponse.SC_BAD_REQUEST; // http error 400 (won't succeed later)

      } else if (tError.indexOf(REQUESTED_RANGE_NOT_SATISFIABLE) >= 0) {
        errorNo = HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE; // http error 416

      } else if (tError.indexOf(Math2.memoryArraySize.substring(0, 25)) >= 0) {
        errorNo =
            HttpServletResponse
                .SC_REQUEST_ENTITY_TOO_LARGE; // http error 413 (the old name for Payload Too
        // Large), although it could be other user's requests
        // that are too large
        String ipAddress = getIPAddress(request);
        tally.add(
            "OutOfMemory (Array Size), IP Address (since last Major LoadDatasets)", ipAddress);
        tally.add("OutOfMemory (Array Size), IP Address (since last daily report)", ipAddress);
        tally.add("OutOfMemory (Array Size), IP Address (since startup)", ipAddress);

      } else if (tError.indexOf("OutOfMemoryError") >= 0
          || // java's words
          tError.indexOf(Math2.memoryThanCurrentlySafe.substring(0, 25))
              >= 0) { // !!! TROUBLE: but that matches memoryThanSafe (in English) too!
        errorNo =
            HttpServletResponse
                .SC_REQUEST_ENTITY_TOO_LARGE; // http error 413 (the old name for Payload Too
        // Large), although it could be other user's requests
        // that are too large
        dangerousMemoryFailures.incrementAndGet();
        String ipAddress = getIPAddress(request);
        tally.add("OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)", ipAddress);
        tally.add("OutOfMemory (Too Big), IP Address (since last daily report)", ipAddress);
        tally.add("OutOfMemory (Too Big), IP Address (since startup)", ipAddress);

      } else if (tErrorLC.indexOf(Math2.memory) >= 0) {
        // catchall for remaining memory problems
        errorNo =
            HttpServletResponse
                .SC_REQUEST_ENTITY_TOO_LARGE; // http error 413 (the old name for Payload Too Large)
        String ipAddress = getIPAddress(request);
        tally.add(
            "OutOfMemory (Way Too Big), IP Address (since last Major LoadDatasets)", ipAddress);
        tally.add("OutOfMemory (Way Too Big), IP Address (since last daily report)", ipAddress);
        tally.add("OutOfMemory (Way Too Big), IP Address (since startup)", ipAddress);

      } else if (tErrorLC.indexOf("unauthorized") >= 0) {
        errorNo = HttpServletResponse.SC_UNAUTHORIZED; // http error 401

      } else if (tErrorLC.indexOf("forbidden") >= 0) {
        errorNo = HttpServletResponse.SC_FORBIDDEN; // http error 403

      } else if (tErrorLC.indexOf("timeout") >= 0
          || tErrorLC.indexOf("time out") >= 0
          || tErrorLC.indexOf("timed out") >= 0) { // testDescendingAxisGeotif sees this
        errorNo = HttpServletResponse.SC_REQUEST_TIMEOUT; // http error 408

      } else {
        // everything else
        if (tError.indexOf("NullPointerException") >= 0 && emailDiagnosticsToErdData) {
          // email stack trace for all NullPointerExceptions to erd.data@noaa.gov (i.e., ERDDAP
          // development team)
          email(
              "erd.data@noaa.gov",
              "java.lang.NullPointerException in ERDDAP v" + erddapVersion,
              // I debated emailing the requestUrl, too. There are security and privacy issues. so
              // don't do it.
              // "request=" +
              // (baseHttpsUrl.startsWith("(")? baseUrl : baseHttpsUrl) + //request may actually
              // have been to http or https (I'm too lazy to write proper code / doesn't seem
              // necessary)
              // (tRequest.indexOf("login.html?") >= 0? tRequestURI + "?[CONFIDENTIAL]" : tRequest)
              // + "\n\n" + //don't show passwords, nonces, etc
              MustBe.throwableToString(t));
        }
        errorNo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // http error 500
      }

      String2.log(
          "*** sendErrorCode "
              + errorNo
              + " for request #"
              + requestNumber
              + ":\n"
              + tRequest
              + "\n"
              + // not decoded
              MustBe.throwableToString(t).trim()); // always log full stack trace

      lowSendError(requestNumber, response, errorNo, tError);

    } catch (Throwable t2) {
      // an exception occurs if response is committed
      throw new ServletException(t2);
    }
  }

  /**
   * This is the lower level version of sendError. Use this if the http errorNo is known.
   *
   * @param requestNumber The requestNumber assigned to this request by doGet().
   * @param response The response to be written to.
   * @param errorNo the HTTP status code / error number. Note that DAP 2.0 says error code is 1
   *     digit, but doesn't provide a list of codes and meanings. I use HTTP status codes (3
   *     digits).
   * @param msg suitable for the user (not the full diagnostic information).
   */
  public static void lowSendError(
      int requestNumber, HttpServletResponse response, int errorNo, String msg) {
    try {
      msg = String2.isSomething(msg) ? msg.trim() : "(no details)";

      // slowDownTroubleMillis applies to all errors
      // because any of these errors could be in a script
      // and it's good to slow the script down (prevent 100 bad requests/second)
      // and if it's a human they won't even notice a short delay
      if (slowDownTroubleMillis > 0) Math2.sleep(slowDownTroubleMillis);

      // put the HTTP status code name at the start of the message (from Wikipedia list
      // https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
      if (errorNo == HttpServletResponse.SC_BAD_REQUEST) // http error 400
      msg =
            "Bad Request: "
                + msg; // Don't translate these (or at least keep English first) so user can look
      // for them
      else if (errorNo == HttpServletResponse.SC_UNAUTHORIZED) // http error 401
      msg = "Unauthorized: " + msg;
      else if (errorNo == HttpServletResponse.SC_FORBIDDEN) // http error 403
      msg = "Forbidden: " + msg;
      else if (errorNo == HttpServletResponse.SC_NOT_FOUND) // http error 404
      msg = "Not Found: " + msg;
      else if (errorNo == HttpServletResponse.SC_REQUEST_TIMEOUT) // http error 408
      msg = "Request Timeout: " + msg;
      else if (errorNo
          == HttpServletResponse
              .SC_REQUEST_ENTITY_TOO_LARGE) // http error 413 (the old name for Payload Too Large)
      msg = "Payload Too Large: " + msg;
      else if (errorNo == HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE) // http error 416
      msg = "Requested Range Not Satisfiable: " + msg;
      else if (errorNo == 429) // http error 429  isn't defined in HttpServletResponse.
      msg = "Too Many Requests: " + msg;
      else if (errorNo == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) // http error 500
      msg = "Internal Server Error: " + msg;
      else if (errorNo == HttpServletResponse.SC_SERVICE_UNAVAILABLE) // http error 503
      msg = "Service Unavailable: " + msg;

      // always log the error
      String fullMsg =
          "Error {\n"
              + "    code="
              + errorNo
              + ";\n"
              + "    message="
              + String2.toJson(msg, 65536, false)
              + ";\n"
              + "}\n";
      if (msg.indexOf(blacklistMsgAr[0]) < 0)
        String2.log(
            "*** lowSendError for request #"
                + requestNumber
                + ": isCommitted="
                + (response == null || response.isCommitted())
                + (errorNo == 503
                    ? " error #503 Service Unavailable (shedThisRequest)"
                    : " fullMessage=\n" + fullMsg)); // + MustBe.getStackTrace());

      // if response isCommitted, nothing more can be done
      if (response == null) {
        String2.log("  response=null, so I'm not sending anything");
      } else if (!response.isCommitted()) {
        standardDapHeader(response);
        response.setStatus(errorNo);
        // set content type both ways in hopes of overwriting any previous settings
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setContentType("text/plain");
        response.setCharacterEncoding(File2.UTF_8);
        response.setHeader("Content-Description", "dods-error");
        response.setHeader("Content-Encoding", "identity"); // not e.g. deflate
        OutputStream outputStream =
            new BufferedOutputStream(response.getOutputStream()); // after all setHeader
        Writer writer = null;
        try {
          writer = File2.getBufferedWriterUtf8(outputStream);
          // from DAP 2.0 section 7.2.4
          writer.write(fullMsg);

        } finally {
          if (writer != null) writer.close();
          else outputStream.close();
        }
      }
    } catch (Throwable t) {
      String2.log(
          String2.ERROR
              + " in lowSendError for request #"
              + requestNumber
              + ":\n"
              + MustBe.throwableToString(t));
    } finally {
      // last thing, try hard to close the outputstream
      try {
        // was if (!response.isCommitted())
        response.getOutputStream().close();
      } catch (Exception e2) {
      }
    }
  }

  public static void actionsAfterEveryMajorLoadDatasets() {
    EDStatic.tally.remove("Large Request, IP address (since last Major LoadDatasets)");
    EDStatic.tally.remove("OutOfMemory (Array Size), IP Address (since last Major LoadDatasets)");
    EDStatic.tally.remove("OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)");
    EDStatic.tally.remove("OutOfMemory (Way Too Big), IP Address (since last Major LoadDatasets)");
    EDStatic.tally.remove(
        "Request refused: not authorized (since last Major LoadDatasets)"); // datasetID (not IP
    // address)
    EDStatic.tally.remove("Requester's IP Address (Allowed) (since last Major LoadDatasets)");
    EDStatic.tally.remove("Requester's IP Address (Blacklisted) (since last Major LoadDatasets)");
    EDStatic.tally.remove("Requester's IP Address (Failed) (since last Major LoadDatasets)");
    EDStatic.tally.remove(
        "Requester's IP Address (Too Many Requests) (since last Major LoadDatasets)");

    EDStatic.failureTimesDistributionLoadDatasets = new int[String2.TimeDistributionSize];
    EDStatic.responseTimesDistributionLoadDatasets = new int[String2.TimeDistributionSize];
  }

  public static void clearDailyTallies() {
    EDStatic.tally.remove(".subset (since last daily report)");
    EDStatic.tally.remove(".subset DatasetID (since last daily report)");
    EDStatic.tally.remove("Advanced Search with Category Constraints (since last daily report)");
    EDStatic.tally.remove("Advanced Search with Lat Lon Constraints (since last daily report)");
    EDStatic.tally.remove("Advanced Search with Time Constraints (since last daily report)");
    EDStatic.tally.remove("Advanced Search, .fileType (since last daily report)");
    EDStatic.tally.remove("Advanced Search, Search For (since last daily report)");
    EDStatic.tally.remove("Categorize Attribute (since last daily report)");
    EDStatic.tally.remove("Categorize Attribute = Value (since last daily report)");
    EDStatic.tally.remove("Categorize File Type (since last daily report)");
    EDStatic.tally.remove("Convert (since last daily report)");
    EDStatic.tally.remove("files browse DatasetID (since last daily report)");
    EDStatic.tally.remove("files download DatasetID (since last daily report)");
    EDStatic.tally.remove("griddap DatasetID (since last daily report)");
    EDStatic.tally.remove("griddap File Type (since last daily report)");
    EDStatic.tally.remove("Home Page (since last daily report)");
    EDStatic.tally.remove("Info (since last daily report)");
    EDStatic.tally.remove("Info File Type (since last daily report)");
    EDStatic.tally.remove("Language (since last daily report)");
    EDStatic.tally.remove("Large Request, IP address (since last daily report)");
    EDStatic.tally.remove("Log in attempt blocked temporarily (since last daily report)");
    EDStatic.tally.remove("Log in failed (since last daily report)");
    EDStatic.tally.remove("Log in succeeded (since last daily report)");
    EDStatic.tally.remove("Log out (since last daily report)");
    EDStatic.tally.remove("Main Resources List (since last daily report)");
    EDStatic.tally.remove("Metadata requests (since last daily report)");
    EDStatic.tally.remove("OpenSearch For (since last daily report)");
    EDStatic.tally.remove("OutOfMemory (Array Size), IP Address (since last daily report)");
    EDStatic.tally.remove("OutOfMemory (Too Big), IP Address (since last daily report)");
    EDStatic.tally.remove("OutOfMemory (Way Too Big), IP Address (since last daily report)");
    EDStatic.tally.remove("POST (since last daily report)");
    EDStatic.tally.remove("Protocol (since last daily report)");
    EDStatic.tally.remove("Requester Is Logged In (since last daily report)");
    EDStatic.tally.remove("Request refused: not authorized (since last daily report)");
    EDStatic.tally.remove("Requester's IP Address (Allowed) (since last daily report)");
    EDStatic.tally.remove("Requester's IP Address (Blacklisted) (since last daily report)");
    EDStatic.tally.remove("Requester's IP Address (Failed) (since last daily report)");
    EDStatic.tally.remove("Requester's IP Address (Too Many Requests) (since last daily report)");
    EDStatic.tally.remove("RequestReloadASAP (since last daily report)");
    EDStatic.tally.remove("Response Failed    Time (since last daily report)");
    EDStatic.tally.remove("Response Succeeded Time (since last daily report)");
    EDStatic.tally.remove("RSS (since last daily report)");
    EDStatic.tally.remove("Search File Type (since last daily report)");
    EDStatic.tally.remove("Search For (since last daily report)");
    EDStatic.tally.remove("SetDatasetFlag (since last daily report)");
    EDStatic.tally.remove("SetDatasetFlag Failed, IP Address (since last daily report)");
    EDStatic.tally.remove("SetDatasetFlag Succeeded, IP Address (since last daily report)");
    EDStatic.tally.remove("SOS index.html (since last daily report)");
    EDStatic.tally.remove("Subscriptions (since last daily report)");
    EDStatic.tally.remove("tabledap DatasetID (since last daily report)");
    EDStatic.tally.remove("tabledap File Type (since last daily report)");
    EDStatic.tally.remove("WCS index.html (since last daily report)");
    EDStatic.tally.remove("WMS doWmsGetMap (since last daily report)");
    EDStatic.tally.remove("WMS doWmsGetCapabilities (since last daily report)");
    EDStatic.tally.remove("WMS doWmsDemo (since last daily report)");
    EDStatic.tally.remove("WMS index.html (since last daily report)");
  }

  public static void resetDailyDistributions() {
    EDStatic.emailThreadFailedDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.emailThreadSucceededDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.emailThreadNEmailsDistribution24 =
        new int[String2.CountDistributionSize]; // count, not time
    EDStatic.failureTimesDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.majorLoadDatasetsDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.minorLoadDatasetsDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.responseTimesDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.taskThreadFailedDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.taskThreadSucceededDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.touchThreadFailedDistribution24 = new int[String2.TimeDistributionSize];
    EDStatic.touchThreadSucceededDistribution24 = new int[String2.TimeDistributionSize];
  }

  public static void resetLuceneIndex() {
    try {
      // delete old index files
      // Index will be recreated, and Lucense throws exception if it tries to read from old
      // indices.
      File2.deleteAllFiles(fullLuceneDirectory);

      // Since I recreate index when erddap restarted, I can change anything
      //  (e.g., Directory type, Version) any time
      //  (no worries about compatibility with existing index).
      // ??? For now, use NIOFSDirectory,
      //  See NIOFSDirectory javadocs (I need to stop using thread.interrupt).
      luceneDirectory = new NIOFSDirectory(FileSystems.getDefault().getPath(fullLuceneDirectory));

      // At start of ERDDAP, always create a new index.  Never re-use existing index.
      // Do it here to use true and also to ensure it can be done.
      createLuceneIndexWriter(true); // throws exception if trouble
    } catch (Throwable t) {
      useLuceneSearchEngine = false;
      throw new RuntimeException(t);
    }
  }
}
