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
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
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
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import org.semver4j.Semver;

/**
 * This class holds a lot of static information set from the setup.xml and messages.xml files and
 * used by all the other ERDDAP classes.
 */
public class EDStatic {

  public static Cleaner cleaner = Cleaner.create();

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
   * 2.25_1 RC 2024-11-07 <br>
   * 2.26 RC on 2025-03-11 <br>
   * The below is kept for historical reference. As of 2.27.0 ERDDAP has transitioned to using
   * Semantic Versioning.
   *
   * <p>For main branch releases, this will be a floating point number with 2 decimal digits, with
   * no additional text. !!! In general, people other than the main ERDDAP developer (Bob) should
   * not change the *number* below. If you need to identify a fork of ERDDAP, please append "_" +
   * other ASCII text (no spaces or control characters) to the number below, e.g., "1.82_MyFork". In
   * a few places in ERDDAP, this string is parsed as a number. The parser now disregards "_" and
   * anything following it. A request to http.../erddap/version will return just the number (as
   * text). A request to http.../erddap/version_string will return the full string.
   */
  public static final Semver erddapVersion = new Semver("2.26.0");

  /** This identifies the dods server/version that this mimics. */
  public static final String dapVersion = "DAP/2.0";

  public static final String serverVersion = "dods/3.7"; // this is what thredds replies (in 2008!)

  // drds at https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.ver replies "DODS/3.2"
  // both reply with server version, neither replies with coreVersion
  // spec says #.#.#, but Gallagher says #.# is fine.

  public static final String INSTITUTION = "institution";
  public static final int TITLE_DOT_LENGTH = 95; // max nChar before inserting newlines

  public static EDConfig config;
  public static Metrics metrics;
  public static final Tally tally = new Tally();
  public static int[] emailThreadFailedDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] emailThreadSucceededDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] emailThreadFailedDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] emailThreadSucceededDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] emailThreadNEmailsDistribution24 =
      new int[String2.CountDistributionSize]; // count, not time
  public static int[] emailThreadNEmailsDistributionTotal =
      new int[String2.CountDistributionSize]; // count, not time
  public static int[] majorLoadDatasetsDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] minorLoadDatasetsDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] majorLoadDatasetsDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] minorLoadDatasetsDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] taskThreadFailedDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] taskThreadSucceededDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] taskThreadFailedDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] taskThreadSucceededDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] touchThreadFailedDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] touchThreadSucceededDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] touchThreadFailedDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] touchThreadSucceededDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] responseTimesDistributionLoadDatasets = new int[String2.TimeDistributionSize];
  public static int[] failureTimesDistributionLoadDatasets = new int[String2.TimeDistributionSize];
  public static int[] responseTimesDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] failureTimesDistribution24 = new int[String2.TimeDistributionSize];
  public static int[] responseTimesDistributionTotal = new int[String2.TimeDistributionSize];
  public static int[] failureTimesDistributionTotal = new int[String2.TimeDistributionSize];
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
  public static int decompressedCacheMaxGB = DEFAULT_decompressedCacheMaxGB;
  public static int decompressedCacheMaxMinutesOld = DEFAULT_decompressedCacheMaxMinutesOld;
  public static int nGridThreads = DEFAULT_nGridThreads; // will be a valid number 1+
  public static int nTableThreads = DEFAULT_nTableThreads; // will be a valid number 1+
  public static String convertInterpolateRequestCSVExample = null; // may be null or ""
  public static String convertInterpolateDatasetIDVariableList[] = new String[0]; // may be [0]

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
  public static final RequestQueue<String> touchList =
      new RequestQueue<>(); // keep here in case TouchThread needs to be restarted
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

  public static String[] DEFAULT_displayAttributeAr = {"summary", "license"};
  public static String[] DEFAULT_displayInfoArray = {"Summary", "License"};
  public static LocalizedHolder<String[]> DEFAULT_displayInfoAr =
      new LocalizedHolder<String[]>(DEFAULT_displayInfoArray);
  public static String[] displayAttributeAr = DEFAULT_displayAttributeAr;
  public static LocalizedHolder<String[]> displayInfoAr = DEFAULT_displayInfoAr;

  private static String emailLogDate = "";
  private static BufferedWriter emailLogFile;

  // these are set as a consequence of setup.xml info
  public static SgtGraph sgtGraph;
  public static String erddapUrl; // without slash at end
  public static String erddapHttpsUrl; // without slash at end   (may be useless, but won't be null)
  public static String preferredErddapUrl; // without slash at end   (https if avail, else http)
  public static String computerName; // e.g., coastwatch (or "")
  public static Subscriptions subscriptions; // null if !EDStatic.config.subscriptionSystemActive

  public static boolean reallyVerbose;
  public static boolean verbose;

  /**
   * These values are loaded from the [contentDirectory]messages.xml file (if present) or
   * .../classes/gov/noaapfel/erddap/util/messages.xml.
   */
  public static EDMessages messages;

  public static final ImmutableList<String> paletteSections =
      ImmutableList.of(
          "", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
          "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31",
          "32", "33", "34", "35", "36", "37", "38", "39", "40");

  /**
   * These are only created/used by GenerateDatasetsXml threads. See the related methods below that
   * create them.
   */
  private static Table gdxAcronymsTable;

  private static Map<String, String> gdxAcronymsHashMap, gdxVariableNamesHashMap;

  private static boolean initialized = false;

  // When doing JettyTests, the servlet gets torn down and so it calls EDStatic.destroy.
  // However we aren't actually done testing at that point, so we don't want to call destroy
  // during testing. This should only be set to tru during testing.
  public static boolean testingDontDestroy = false;

  /**
   * This static block reads this class's static String values from contentDirectory, which must
   * contain setup.xml and datasets.xml (and may contain messages.xml). It may be a defined
   * environment variable ("erddapContentDirectory") or a subdir of <tomcat> (e.g.,
   * usr/local/tomcat/content/erddap/) (more specifically, a sibling of 'tomcat'/webapps).
   *
   * @throws RuntimeException if trouble
   */
  static {
    String webInfParentDirectory = File2.getWebInfParentDirectory();
    // route calls to a logger to com.cohort.util.String2Log
    String2.setupCommonsLogging(-1);
    init(webInfParentDirectory);
  }

  public static void init(String webInfParentDirectory) {
    if (initialized) {
      return;
    }
    initialized = true;
    String erdStartup = "EDStatic Low Level Startup";
    String errorInMethod = "";
    try {
      SSR.erddapVersion = erddapVersion.getVersion();

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
              + erddapVersion.getVersion()
              + eol
              + String2.standardHelpAboutMessage());

      config = new EDConfig(webInfParentDirectory);
      metrics = new Metrics();

      if (!config.skipEmailThread) {
        ensureEmailThreadIsRunningIfNeeded();
      }
      ensureTouchThreadIsRunningIfNeeded();

      // test of email
      // Test.error("This is a test of emailing an error in Erddap constructor.");

      // are bufferedImages hardware accelerated?
      String2.log(SgtUtil.isBufferedImageAccelerated());

      // 2011-12-30 convert /datasetInfo/[datasetID]/ to
      //                   /dataset/[last2char]/[datasetID]/
      // to prepare for huge number of datasets
      String oldBaseDir = config.bigParentDirectory + "datasetInfo/"; // the old name
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
      // use Lucence?
      if (config.searchEngine.equals("lucene")) {
        config.useLuceneSearchEngine = true;
        luceneDocNToDatasetID = new ConcurrentHashMap<>();
      } else {
        Test.ensureEqual(
            config.searchEngine,
            "original",
            "<searchEngine> must be \"original\" (the default) or \"lucene\".");
      }

      errorInMethod = "ERROR while initializing SgtGraph: ";
      sgtGraph = new SgtGraph(config.fontFamily);

      // ensure erddapVersion is okay
      String versionCheck = erddapVersion.getVersion();
      if (versionCheck.indexOf(' ') >= 0 || !String2.isAsciiPrintable(versionCheck))
        throw new SimpleException(
            "Invalid ERDDAP version format. Must be a valid Semantic Version and ascii printable"
                + versionCheck);

      // ensure authentication setup is okay
      errorInMethod = "ERROR while checking authentication setup: ";
      String authentication = config.authentication;
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
      if (!authentication.isEmpty() && !config.baseHttpsUrl.startsWith("https://"))
        throw new RuntimeException(
            "setup.xml error: "
                + ": For any <authentication> other than \"\", the baseHttpsUrl="
                + config.baseHttpsUrl
                + " must start with \"https://\".");
      if ((authentication.equals("google") || authentication.equals("auth2"))
          && !String2.isSomething(config.googleClientID))
        throw new RuntimeException(
            "setup.xml error: "
                + ": When authentication=google or oauth2, you must provide your <googleClientID>.");
      if ((authentication.equals("orcid") || authentication.equals("auth2"))
          && (!String2.isSomething(config.orcidClientID)
              || !String2.isSomething(config.orcidClientSecret)))
        throw new RuntimeException(
            "setup.xml error: "
                + ": When authentication=orcid or oauth2, you must provide your <orcidClientID> and <orcidClientSecret>.");
      if (authentication.equals("custom")
          && (!config.passwordEncoding.equals("MD5")
              && !config.passwordEncoding.equals("UEPMD5")
              && !config.passwordEncoding.equals("SHA256")
              && !config.passwordEncoding.equals("UEPSHA256")))
        throw new RuntimeException(
            "setup.xml error: When authentication=custom, passwordEncoding="
                + config.passwordEncoding
                + " must be MD5|UEPMD5|SHA256|UEPSHA256.");
      // String2.log("authentication=" + authentication);

      // things set as a consequence of setup.xml
      erddapUrl = config.baseUrl + "/" + config.warName;
      erddapHttpsUrl = config.baseHttpsUrl + "/" + config.warName;
      preferredErddapUrl = config.baseHttpsUrl.startsWith("https://") ? erddapHttpsUrl : erddapUrl;

      if (config.subscriptionSystemActive) {
        subscriptions =
            new Subscriptions(
                config.bigParentDirectory + "subscriptionsV1.txt",
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
          RegexFilenameFilter.recursiveFullNameList(
              config.contentDirectory + "images/", ".+", false);
      for (String file : tFiles) {
        int tpo = file.indexOf("/images/");
        if (tpo < 0) tpo = file.indexOf("\\images\\");
        if (tpo < 0) {
          String2.log("'/images/' not found in images/ file: " + file);
          continue;
        }
        String tName = file.substring(tpo + 8);
        if (verbose) String2.log("  copying images/ file: " + tName);
        File2.copy(config.contentDirectory + "images/" + tName, config.imageDir + tName);
      }
      // copy all <contentDirectory>cptfiles/ files to cptfiles
      tFiles =
          RegexFilenameFilter.list(
              config.contentDirectory + "cptfiles/", ".+\\.cpt"); // not recursive
      for (String tFile : tFiles) {
        if (verbose) String2.log("  copying cptfiles/ file: " + tFile);
        File2.copy(
            config.contentDirectory + "cptfiles/" + tFile, config.fullPaletteDirectory + tFile);
      }

      // try to create an nc4 file
      config.accessibleViaNC4 = ".nc4 is not yet supported.";
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
      metrics.initialize(config.usePrometheusMetrics);
      messages = new EDMessages(config.contentDirectory);
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
    return loggedInAs == null
        ? config.baseUrl
        : config.baseHttpsUrl; // works because of loggedInAsHttps
  }

  /**
   * Return the ERDDAP URL prefix, including scheme, host (and port if specified), and WAR context
   * path (/erddap), with no trailing slash.
   *
   * @param request the request
   * @param loggedInAs the logged in user
   * @return the ERDDAP URL prefix (example: http://erddap.yourdomain.com/erddap)
   */
  protected static String getErddapUrlPrefix(HttpServletRequest request, String loggedInAs) {
    if (EDStatic.config.useHeadersForUrl && request != null && request.getHeader("Host") != null) {
      return request.getScheme() + "://" + request.getHeader("Host") + "/" + config.warName;
    }
    return loggedInAs == null ? erddapUrl : erddapHttpsUrl;
  }

  /**
   * If loggedInAs is null, this returns erddapUrl, else erddapHttpsUrl (neither has slash at end).
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @return If loggedInAs == null, this returns erddapUrl, else erddapHttpsUrl (neither has slash
   *     at end).
   */
  public static String erddapUrl(HttpServletRequest request, String loggedInAs, int language) {
    return getErddapUrlPrefix(request, loggedInAs)
        + (language == 0 ? "" : "/" + TranslateMessages.languageCodeList.get(language));
  }

  /**
   * Returns https ERDDAP url plus optional /languageCode, without slash at end. If useHeaderForUrl
   * is enabled, the https url is built from the request Host header, as long as either the request
   * scheme is https or the host doesn't contain a port (i.e. http requests without a port in the
   * host will result in https urls with the same host). Otherwise erddapHttpsUrl is used.
   *
   * @param language the index of the selected language
   * @return erddapHttpsUrl plus optional /languageCode. (without slash at end).
   */
  public static String erddapHttpsUrl(HttpServletRequest request, int language) {
    String httpsUrl = erddapHttpsUrl;
    if (EDStatic.config.useHeadersForUrl
        && request != null
        && request.getHeader("Host") != null
        && (request.getScheme() == "https" || !request.getHeader("Host").contains(":"))) {
      httpsUrl = "https://" + request.getHeader("Host") + "/" + config.warName;
    }
    return httpsUrl + (language == 0 ? "" : "/" + TranslateMessages.languageCodeList.get(language));
  }

  /**
   * This determines if a URL points to this server (even in development).
   *
   * @param tUrl
   */
  public static boolean urlIsThisComputer(String tUrl) {
    return tUrl.startsWith(config.baseUrl)
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
  public static String imageDirUrl(HttpServletRequest request, String loggedInAs, int language) {
    return erddapUrl(request, loggedInAs, language) + "/" + EDConfig.IMAGES_DIR;
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
  public static String youAreHere(
      HttpServletRequest request, int language, String loggedInAs, String protocol) {
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, erddapUrl(request, loggedInAs, language))
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
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String protocol,
      String protocolNameAr[],
      String current) {
    String tErddapUrl = erddapUrl(request, loggedInAs, language);
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
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String protocol,
      String datasetID) {
    String tErddapUrl = erddapUrl(request, loggedInAs, language);
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
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String protocol,
      String htmlHelp) {
    String tErddapUrl = erddapUrl(request, loggedInAs, language);
    return "\n<h1 class=\"nowrap\">"
        + erddapHref(language, tErddapUrl)
        + "\n &gt; "
        + protocol
        + "\n"
        + htmlTooltipImage(request, language, loggedInAs, htmlHelp)
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
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String protocol,
      String datasetID,
      String htmlHelp) {

    String tErddapUrl = erddapUrl(request, loggedInAs, language);
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
        + htmlTooltipImage(request, language, loggedInAs, htmlHelp)
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

      String tErddapUrl = erddapUrl(request, loggedInAs, language);
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
  public static String htmlTooltipImage(
      HttpServletRequest request, int language, String loggedInAs, String html) {
    return HtmlWidgets.htmlTooltipImage(
        imageDirUrl(request, loggedInAs, language) + messages.questionMarkImageFile, "?", html, "");
  }

  /**
   * This returns the html to draw a question mark that has big html tooltip for an EDDTable EDV
   * data variable.
   *
   * @param language the language code number
   * @param edv from an EDDTable
   */
  public static String htmlTooltipImageEDV(
      HttpServletRequest request, int language, String loggedInAs, EDV edv) throws Throwable {

    return htmlTooltipImageLowEDV(
        request,
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
  public static String htmlTooltipImageEDVGA(
      HttpServletRequest request, int language, String loggedInAs, EDVGridAxis edvga)
      throws Throwable {

    return htmlTooltipImageLowEDV(
        request,
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
      HttpServletRequest request, int language, String loggedInAs, EDV edv, String allDimString)
      throws Throwable {

    return htmlTooltipImageLowEDV(
        request,
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
   * @param request the servlet request
   * @param language the language code number
   * @param loggedInAs
   * @param destinationDataPAType
   * @param destinationName perhaps with axis information appended (e.g.,
   *     [time][latitude][longitude]
   * @param attributes
   */
  public static String htmlTooltipImageLowEDV(
      HttpServletRequest request,
      int language,
      String loggedInAs,
      PAType destinationDataPAType,
      String destinationName,
      LocalizedAttributes attributes)
      throws Throwable {

    StringBuilder sb =
        OpendapHelper.dasToStringBuilder(
            OpendapHelper.getAtomicType(false, destinationDataPAType)
                + " "
                + destinationName, // strictDapMode
            destinationDataPAType,
            attributes.toAttributes(language),
            false,
            false); // htmlEncoding, strictDapMode
    // String2.log("htmlTooltipImage sb=" + sb.toString());
    return htmlTooltipImage(
        request,
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
                ? // don't use EDStatic.config.subscriptionSystemActive for this test -- it's a
                // separate
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
                    config.fullLogsDirectory + "emailLog" + date + ".txt", true)); // true=append
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

    } else if (config.emailIsActive) {
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
          messages.blacklistMsgAr[language]);
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
      if (config.emailIsActive) {
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

    if (config.emailIsActive) {
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
   * <p>This relies on EDStatic.config.authentication
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
    if (config.authentication.length() == 0) return loggedInAsHttps;

    // see if user is logged in
    // NOTE: session is associated with https urls, not http urls!
    //  So user only appears logged in to https urls.
    HttpSession session = request.getSession(false); // don't make one if none already
    // String2.log("session=" + (session==null? "null" :
    // session.getServletContext().getServletContextName()));

    if (session == null) return loggedInAsHttps;

    // session != null
    String loggedInAs = null;
    if (config.authentication.equals("custom")
        || config.authentication.equals("email")
        || config.authentication.equals("google")
        || config.authentication.equals("orcid")
        || config.authentication.equals("oauth2")) {
      loggedInAs = (String) session.getAttribute("loggedInAs:" + config.warName);

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
            + config.flagKeyKey);
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
        switch (config.passwordEncoding) {
          case "MD5" -> String2.md5Hex(plaintextPassword); // it will be lowercase
          case "UEPMD5" ->
              String2.md5Hex(username + ":ERDDAP:" + plaintextPassword); // it will be lowercase
          case "SHA256" ->
              String2.passwordDigest("SHA-256", plaintextPassword); // it will be lowercase
          case "UEPSHA256" ->
              String2.passwordDigest(
                  "SHA-256", username + ":ERDDAP:" + plaintextPassword); // it will be lowercase
          default ->
              throw new RuntimeException("Unexpected passwordEncoding=" + config.passwordEncoding);
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
                    ? messages.notAuthorizedForDataAr[language]
                    : messages.notAuthorizedAr[language],
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
   * @param request the request
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in) Special case:
   *     "loggedInAsHttps" is for using https without being logged in, but &amp;loginInfo; indicates
   *     user isn't logged in.
   */
  public static String getLoginHtml(HttpServletRequest request, int language, String loggedInAs) {
    if (config.authentication.isEmpty()) {
      // user can't log in
      return "";
    } else {
      String tUrl = erddapHttpsUrl(request, language);
      return loggedInAs == null || loggedInAsHttps.equals(loggedInAs)
          ? // ie not logged in
          // always use the erddapHttpsUrl for login/logout pages
          "<a href=\"" + tUrl + "/login.html\">" + messages.loginAr[language] + "</a>"
          : "<a href=\""
              + tUrl
              + "/login.html\"><strong>"
              + XML.encodeAsHTML(loggedInAs)
              + "</strong></a> | \n"
              + "<a href=\""
              + tUrl
              + "/logout.html\">"
              + messages.logoutAr[language]
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
   * @param request the request
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
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String endOfRequest,
      String queryString) {
    return startBodyHtml(request, language, loggedInAs, endOfRequest, queryString, "");
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
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String endOfRequest,
      String queryString,
      String otherBody) {

    String tErddapUrl = erddapUrl(request, loggedInAs, language);
    String s =
        messages
            .startBodyHtmlAr[
            0]; // It's hard for admins to customized this for all languages. So for now, just use
    // language=0.
    s =
        String2.replaceAll(
            s, "&EasierAccessToScientificData;", messages.EasierAccessToScientificDataAr[language]);
    s = String2.replaceAll(s, "&BroughtToYouBy;", messages.BroughtToYouByAr[language]);
    if (String2.isSomething(otherBody))
      s = String2.replaceAll(s, "<body>", "<body " + otherBody + ">");
    s = String2.replaceAll(s, "&loginInfo;", getLoginHtml(request, language, loggedInAs));
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
                            + config.warName
                            + "/' + "
                            + "(this.selectedIndex == 0? '' : this[this.selectedIndex].value + '/') + '"
                            + // e.g., de
                            XML.encodeAsHTMLAttribute(
                                endOfRequest /* + questionQuery(queryString) */)
                            + "';\"", // query string is already percent encoded.
                        false,
                        "")
                    + htmlTooltipImage(
                        request,
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
    return String2.replaceAll(s, "&erddapUrl;", erddapUrl(request, loggedInAs, language));
  }

  /**
   * The endBody HTML.
   *
   * @param language the index of the selected language
   * @param tErddapUrl from EDStatic.erddapUrl(request, loggedInAs, language) (erddapUrl, or
   *     erddapHttpsUrl if user is logged in)
   * @param loggedInAs
   */
  public static String endBodyHtml(
      HttpServletRequest request, int language, String tErddapUrl, String loggedInAs) {
    String s = String2.replaceAll(messages.endBodyHtmlAr[language], "&erddapUrl;", tErddapUrl);
    if (language > 0)
      s =
          s.replace(
              "<hr>",
              "<br><img src=\""
                  + tErddapUrl
                  + "/images/TranslatedByGoogle.png\" alt=\"Translated by Google\">\n"
                  + htmlTooltipImage(request, language, loggedInAs, translationDisclaimer)
                  + "<hr>");
    return s;
  }

  /**
   * The content of the legal web page.
   *
   * @param language the index of the selected language
   * @param tErddapUrl from EDStatic.erddapUrl(request, loggedInAs, language) (erddapUrl, or
   *     erddapHttpsUrl if user is logged in)
   */
  public static String legal(int language, String tErddapUrl) {
    StringBuilder tsb = new StringBuilder(messages.legal);
    String2.replaceAll(tsb, "[standardContact]", messages.standardContactAr[language] + "\n\n");
    String2.replaceAll(
        tsb, "[standardDataLicenses]", messages.standardDataLicensesAr[language] + "\n\n");
    String2.replaceAll(
        tsb,
        "[standardDisclaimerOfExternalLinks]",
        messages.standardDisclaimerOfExternalLinksAr[language] + "\n\n");
    String2.replaceAll(
        tsb,
        "[standardDisclaimerOfEndorsement]",
        messages.standardDisclaimerOfEndorsementAr[language] + "\n\n");
    String2.replaceAll(
        tsb, "[standardPrivacyPolicy]", messages.standardPrivacyPolicyAr[language] + "\n\n");
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
    String ts = messages.startHeadHtml;

    if (addToTitle.length() > 0)
      ts = String2.replaceAll(ts, "</title>", " - " + XML.encodeAsHTML(addToTitle) + "</title>");
    ts =
        String2.replaceAll(
            ts,
            "&langCode;",
            messages.langCodeAr[language]
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

  public static String erddapHref(int language, String tErddapUrl) {
    return "<a title=\""
        + messages.clickERDDAPAr[language]
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
    String ae = String2.replaceAll(config.adminEmail, "@", " at ");
    ae = String2.replaceAll(ae, ".", " dot ");
    return config.adminIndividualName + " (email: " + ae + ")";
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
        + messages.errorOnWebPageAr[language]
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
    if (testingDontDestroy) {
      return;
    }
    try {
      if (subscriptions != null) {
        subscriptions.close();
        subscriptions = null;
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
      if (config.useLuceneSearchEngine) String2.log("stopping lucene...");
      config.useLuceneSearchEngine = false;
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

      if (touchThread != null) {
        touchThread.interrupt();
        touchThread = null;
      }

      if (taskThread != null) {
        taskThread.interrupt();
        taskThread = null;
      }

      if (emailThread != null) {
        emailThread.interrupt();
        emailThread = null;
      }

    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
    } finally {
      initialized = false;
      cleaner = null;
      messages = null;
      config = null;
      metrics = null;
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
          email(config.emailEverythingToCsv, "emailThread Stalled", tError);
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
          email(config.emailEverythingToCsv, "taskThread Stalled", tError);
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
          email(config.emailEverythingToCsv, "touchThread Stalled", tError);
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
      if (!config.emailIsActive || isEmailThreadRunning()) return;

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
      config.useLuceneSearchEngine = false;
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
            email(config.emailEverythingToCsv, subject, msg);
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

  public static String getJsonpFromQuery(int language, String userDapQuery) throws Exception {
    String parts[] = Table.getDapQueryParts(userDapQuery); // decoded
    String jsonp = String2.stringStartsWith(parts, ".jsonp="); // may be null
    if (jsonp != null) {
      jsonp = jsonp.substring(7);
      if (!String2.isJsonpNameSafe(jsonp)) {
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0] + EDStatic.messages.errorJsonpFunctionNameAr[0],
                EDStatic.messages.queryErrorAr[language]
                    + EDStatic.messages.errorJsonpFunctionNameAr[language]));
      }
    }
    return jsonp;
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
      jsonp = EDStatic.getJsonpFromQuery(language, request.getQueryString());
      if (jsonp == null) return "";
      return ".jsonp=" + SSR.minimalPercentEncode(jsonp) + "&";
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
      (searchFor.length() > 0 ? messages.searchSpellingAr[language] + " " : "")
          + (searchFor.indexOf(' ') >= 0 ? messages.searchFewerWordsAr[language] : "")
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
      MessageFormat.format(messages.noPage1Ar[language], "" + page, "" + lastPage),
      MessageFormat.format(messages.noPage2Ar[language], "" + page, "" + lastPage)
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

    if (nMatches == 1) return messages.nMatching1Ar[language];

    StringBuilder results =
        new StringBuilder(
            MessageFormat.format(
                    relevant
                        ? messages.nMatchingMostRelevantAr[language]
                        : messages.nMatchingAlphabeticalAr[language],
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
              + messages.nMatchingCurrentAr[language]
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
                  messages.nMatchingPageAr[language], "" + page, "" + lastPage, sb.toString())
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

    int n = messages.updateUrlsFrom.length;
    for (int i = 0; i < n; i++)
      String2.replaceAll(sb, messages.updateUrlsFrom[i], messages.updateUrlsTo[i]);
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
      if (String2.indexOf(messages.updateUrlsSkipAttributes, name) >= 0) continue;
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

        if (config.forceSynchronousLoading) {
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
      metrics.dangerousMemoryEmails.inc();
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
      email(config.emailEverythingToCsv, "Dangerously High Memory Use!!!", report);
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
        messages.waitThenTryAgainAr[language]);
    metrics.shedRequests.inc();
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
    response.setHeader(programname + "-server", erddapVersion.getVersion());
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
      if (tError.indexOf(messages.resourceNotFoundAr[0]) >= 0
          || tError.indexOf(MustBe.THERE_IS_NO_DATA)
              >= 0) { // check this first, since may also be Query error
        errorNo = HttpServletResponse.SC_NOT_FOUND; // http error 404  (might succeed later)
        // I wanted to use 204 No Content or 205 (similar) but browsers don't show any change for
        // these codes

      } else if (tError.indexOf(messages.queryErrorAr[0]) >= 0) {
        errorNo = HttpServletResponse.SC_BAD_REQUEST; // http error 400 (won't succeed later)

      } else if (tError.indexOf(REQUESTED_RANGE_NOT_SATISFIABLE) >= 0) {
        errorNo = HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE; // http error 416

      } else if (tError.indexOf(Math2.memoryArraySize.substring(0, 25)) >= 0) {
        errorNo =
            HttpServletResponse
                .SC_REQUEST_ENTITY_TOO_LARGE; // http error 413 (the old name for Payload Too
        // Large), although it could be other user's requests
        // that are too large
        metrics.dangerousMemoryFailures.inc();
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
        metrics.dangerousMemoryFailures.inc();
        String ipAddress = getIPAddress(request);
        tally.add("OutOfMemory (Too Big), IP Address (since last Major LoadDatasets)", ipAddress);
        tally.add("OutOfMemory (Too Big), IP Address (since last daily report)", ipAddress);
        tally.add("OutOfMemory (Too Big), IP Address (since startup)", ipAddress);

      } else if (tErrorLC.indexOf(Math2.memory) >= 0) {
        // catchall for remaining memory problems
        errorNo =
            HttpServletResponse
                .SC_REQUEST_ENTITY_TOO_LARGE; // http error 413 (the old name for Payload Too Large)
        metrics.dangerousMemoryFailures.inc();
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
        if (tError.indexOf("NullPointerException") >= 0 && config.emailDiagnosticsToErdData) {
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
      if (config.slowDownTroubleMillis > 0) Math2.sleep(config.slowDownTroubleMillis);

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
      if (msg.indexOf(messages.blacklistMsgAr[0]) < 0)
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
      File2.deleteAllFiles(config.fullLuceneDirectory);

      // Since I recreate index when erddap restarted, I can change anything
      //  (e.g., Directory type, Version) any time
      //  (no worries about compatibility with existing index).
      // ??? For now, use NIOFSDirectory,
      //  See NIOFSDirectory javadocs (I need to stop using thread.interrupt).
      luceneDirectory =
          new NIOFSDirectory(FileSystems.getDefault().getPath(config.fullLuceneDirectory));

      // At start of ERDDAP, always create a new index.  Never re-use existing index.
      // Do it here to use true and also to ensure it can be done.
      createLuceneIndexWriter(true); // throws exception if trouble
    } catch (Throwable t) {
      config.useLuceneSearchEngine = false;
      throw new RuntimeException(t);
    }
  }

  public static Semver getSemver(String version) {
    Semver semver = Semver.coerce(version);
    if (semver == null) {
      semver = Semver.coerce(version + ".0");
    }
    if (semver == null) {
      throw new SimpleException("**SEMVER_ERROR** Could not get semver from version: " + version);
    }
    return semver;
  }
}
