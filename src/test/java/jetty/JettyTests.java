package jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.TableTests;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;

import org.eclipse.jetty.ee10.webapp.WebAppContext;

import tags.TagJetty;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class JettyTests {

  @TempDir
  private static Path TEMP_DIR;

  private static Server server;
  private static Integer PORT = 8080;

  @BeforeAll
  public static void setUp() throws Throwable {
    Initialization.edStatic();
    EDDTestDataset.generateDatasetsXml();

    server = new Server(PORT);

    WebAppContext context = new WebAppContext();
    ResourceFactory resourceFactory = ResourceFactory.of(context);
    Resource baseResource = resourceFactory
        .newResource(Path.of(System.getProperty("user.dir")).toAbsolutePath().toUri());
    context.setBaseResource(baseResource);
    context.setContextPath("/");
    context.setParentLoaderPriority(true);
    server.setHandler(context);

    server.start();
    // Make a request of the server to make sure it starts loading the datasets
    SSR.getUrlResponseStringUnchanged("http://localhost:" + PORT + "/erddap");

    Thread.sleep(5 * 60 * 1000);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    server.stop();
  }

  /* TableTests */

  /**
   * Test saveAsJson and readJson.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testJson() throws Exception {
    // String2.log("\n***** Table.testJson");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    Table table = TableTests.getTestTable(true, true);

    // write it to a file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/tempTable.json";
    table.saveAsJson(fileName, 0, true);
    // String2.log(fileName + "=\n" + File2.readFromFile(fileName)[1]);
    // Test.displayInBrowser("file://" + fileName); //.json

    // read it from the file
    String results = File2.directReadFromUtf8File(fileName);
    Test.ensureEqual(results,
        "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Time\", \"Longitude\", \"Latitude\", \"Double Data\", \"Long Data\", \"Int Data\", \"Short Data\", \"Byte Data\", \"Char Data\", \"String Data\"],\n"
            +
            "    \"columnTypes\": [\"String\", \"int\", \"float\", \"double\", \"long\", \"int\", \"short\", \"byte\", \"char\", \"String\"],\n"
            +
            "    \"columnUnits\": [\"UTC\", \"degrees_east\", \"degrees_north\", \"doubles\", \"longs\", \"ints\", \"shorts\", \"bytes\", \"chars\", \"Strings\"],\n"
            +
            "    \"rows\": [\n" +
            "      [\"1970-01-01T00:00:00Z\", -3, 1.0, -1.0E300, -2000000000000000, -2000000000, -32000, -120, \",\", \"a\"],\n"
            +
            "      [\"2005-08-31T16:01:02Z\", -2, 1.5, 3.123, 2, 2, 7, 8, \"\\\"\", \"bb\"],\n" +
            "      [\"2005-11-02T18:04:09Z\", -1, 2.0, 1.0E300, 2000000000000000, 2000000000, 32000, 120, \"\\u20ac\", \"ccc\"],\n"
            +
            "      [null, null, null, null, null, null, null, null, \"\", \"\"]\n" +
            "    ]\n" +
            "  }\n" +
            "}\n",
        results);

    // read it
    Table table2 = new Table();
    table2.readJson(fileName);
    Test.ensureTrue(table.equals(table2), "");

    // finally
    File2.delete(fileName);

    // ******************* test readErddapInfo
    // String tUrl = "https://coastwatch.pfeg.noaa.gov/erddap2";
    // http://localhost:" + PORT + "/erddap/info/pmelTaoDySst/index.json
    String tUrl = "http://localhost:" + PORT + "/erddap";
    table.readErddapInfo(tUrl + "/info/pmelTaoDySst/index.json");
    String ncHeader = table.getNCHeader("row");
    Test.ensureEqual(table.globalAttributes().getString("cdm_data_type"), "TimeSeries", ncHeader);
    Test.ensureEqual(table.globalAttributes().getString("title"),
        "TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature",
        ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").size(), 3, ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").getString(0),
        "This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.",
        ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").getString(1),
        "This dataset is a product of the TAO Project Office at NOAA/PMEL.",
        ncHeader);
    Test.ensureLinesMatch(table.globalAttributes().get("history").getString(2),
        "20\\d{2}-\\d{2}-\\d{2} Bob Simons at NOAA/NMFS/SWFSC/ERD \\(bob.simons@noaa.gov\\) " +
            "fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  "
            +
            "Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.",
        ncHeader);
    Test.ensureEqual(table.nColumns(), 10, ncHeader);
    Test.ensureEqual(table.findColumnNumber("longitude"), 3, ncHeader);
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "degrees_east", ncHeader);
    int t25Col = table.findColumnNumber("T_25");
    Test.ensureTrue(t25Col > 0, ncHeader);
    Test.ensureEqual(table.columnAttributes(t25Col).getString("ioos_category"), "Temperature", ncHeader);
    Test.ensureEqual(table.columnAttributes(t25Col).getString("units"), "degree_C", ncHeader);
  }

  /* FileVisitorDNLS */

  /**
   * This tests a WAF-related (Web Accessible Folder) methods on an ERDDAP "files"
   * directory.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testErddap1FilesWAF2() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testErddapFilesWAF2()\n");

    // *** test localhost
    String2.log("\nThis test requires erdMWchla1day in localhost erddap.");
    String url = "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/";
    String tFileNameRegex = "MW200219.*\\.nc(|\\.gz)";
    boolean tRecursive = true;
    String tPathRegex = ".*";
    boolean tDirsToo = true;
    Table table = FileVisitorDNLS.makeEmptyTable();
    StringArray dirs = (StringArray) table.getColumn(0);
    StringArray names = (StringArray) table.getColumn(1);
    LongArray lastModifieds = (LongArray) table.getColumn(2);
    LongArray sizes = (LongArray) table.getColumn(3);

    // * test all features
    String results = FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
        url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo,
        dirs, names, lastModifieds, sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    table.removeColumn("lastModified");
    table.removeColumn("size");
    results = table.dataToString();
    String expected = "directory,name\n" +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002190_2002190_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002191_2002191_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002192_2002192_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002193_2002193_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002194_2002194_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002195_2002195_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002196_2002196_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002197_2002197_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002198_2002198_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002199_2002199_chla.nc\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /* TestSSR */

  /**
   * Test posting info and getting response.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testPostFormGetResponseString() throws Exception {
    String s = SSR.postFormGetResponseString(
        "https://coastwatch.pfeg.noaa.gov/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=jplmursst41");
    String2.log("\nSSR.testPostFormGetResponseString() result:\n" + s);
    Test.ensureTrue(s.indexOf("Do a Full Text Search for Datasets:") >= 0, "");
    Test.ensureTrue(s.indexOf("Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global") >= 0,
        "");
    Test.ensureTrue(s.indexOf("ERDDAP, Version") >= 0, "");

    // 2018-10-24 I verified that
    // * This request appears as a POST (not GET) in tomcat's
    // localhost_access_lot[date].txt
    // * The parameters don't appear in that file (whereas they do for GET requests)
    // * The parameters don't appear in ERDDAP log (whereas they do for GET
    // requests),
    // * and it is labelled as a POST request.
    s = SSR.postFormGetResponseString(
        "http://localhost:" + PORT + "/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=jplmursst41");
    String2.log("\nSSR.testPostFormGetResponseString() result:\n" + s);
    Test.ensureTrue(s.indexOf("Do a Full Text Search for Datasets:") >= 0, "");
    Test.ensureTrue(s.indexOf("Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global") >= 0,
        "This test requires MUR 4.1 in the local host ERDDAP.");
    Test.ensureTrue(s.indexOf("ERDDAP, Version") >= 0, "");
  }

  /* TranslateMessages */

  /**
   * This checks lots of webpages on localhost ERDDAP for uncaught special text
   * (&amp;term; or ZtermZ).
   * This REQUIRES localhost ERDDAP be running with at least
   * <datasetsRegex>(etopo.*|jplMURSST41|cwwcNDBCMet)</datasetsRegex>.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void checkForUncaughtSpecialText() throws Exception {
    String2.log("\n*** TranslateMessages.checkForUncaughtSpecialText()\n" +
        "THIS REQUIRES localhost ERDDAP with at least (etopo.*|jplMURSST41|cwwcNDBCMet)");
    String tErddapUrl = "http://localhost:" + PORT + "/erddap/de/";
    String pages[] = {
        "index.html",
        "categorize/cdm_data_type/grid/index.html?page=1&itemsPerPage=1000",
        "convert/index.html",
        "convert/oceanicAtmosphericAcronyms.html",
        "convert/fipscounty.html",
        "convert/keywords.html",
        "convert/time.html",
        // "convert/units.html", // Disable this check because &C is in translations wrong (this is a minor mistake not an incorrect translation)
        "convert/urls.html",
        "convert/oceanicAtmosphericVariableNames.html",
        "dataProviderForm.html",
        "dataProviderForm1.html",
        "dataProviderForm2.html",
        "dataProviderForm3.html",
        "dataProviderForm4.html",
        // "download/AccessToPrivateDatasets.html",
        // "download/changes.html",
        // "download/EDDTableFromEML.html",
        // "download/grids.html",
        // "download/NCCSV.html",
        // "download/NCCSV_1.00.html",
        // "download/setup.html",
        // "download/setupDatasetsXml.html",
        "files/",
        "files/cwwcNDBCMet/",
        "files/documentation.html",
        "griddap/documentation.html",
        "griddap/jplMURSST41.graph",
        "griddap/jplMURSST41.html",
        "info/index.html?page=1&itemsPerPage=1000",
        "info/cwwcNDBCMet/index.html",
        "information.html",
        "opensearch1.1/index.html",
        "rest.html",
        "search/index.html?page=1&itemsPerPage=1000&searchFor=sst",
        "slidesorter.html",
        "subscriptions/index.html",
        "subscriptions/add.html",
        "subscriptions/validate.html",
        "subscriptions/list.html",
        "subscriptions/remove.html",
        "tabledap/documentation.html",
        "tabledap/cwwcNDBCMet.graph",
        "tabledap/cwwcNDBCMet.html",
        "tabledap/cwwcNDBCMet.subset",
        "wms/documentation.html",
        "wms/jplMURSST41/index.html" };
    StringBuilder results = new StringBuilder();
    for (int i = 0; i < pages.length; i++) {
      String content;
      String sa[];
      HashSet<String> hs;
      try {
        content = SSR.getUrlResponseStringUnchanged(tErddapUrl + pages[i]);
      } catch (Exception e) {
        results.append("\n* Trouble: " + e.toString() + "\n");
        continue;
      }

      // Look for ZsomethingZ
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(Z[a-zA-Z0-9]Z)", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " ZtermsZ :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for &something; that are placeholders that should have been replaced by
      // replaceAll().
      // There are some legit uses in changes.html, setup.html, and
      // setupDatasetsXml.html.
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(&amp;[a-zA-Z]+?;)", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " &entities; :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for {0}, {1}, etc that should have been replaced by replaceAll().
      // There are some legit values on setupDatasetsXml.html in regexes ({nChar}:
      // 12,14,4,6,7,8).
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(\\{\\d+\\})", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " {#} :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }
    }
    if (results.length() > 0)
      throw new RuntimeException(results.toString());
  }

  /* Erddap */

  /**
   * This checks that the links on the specified web page return error 200.
   *
   * @param tUrl the web page to be checked
   * @throws RuntimeException for
   */
  void testForBrokenLinks(String tUrl) throws Exception {

    String2.log("\nSSR.testForBrokenLinks(" + tUrl + ")");
    String regex = "\"(https?://.+?)\"";
    Pattern pattern = Pattern.compile(regex);
    String lines[] = SSR.getUrlResponseLines(tUrl);
    StringBuilder log = new StringBuilder();
    int errorCount = 0;
    HashSet<String> tried = new HashSet();
    String skip[] = new String[] {
        "http://",
        "http://127.0.0.1:8080/manager/html/", // will always fail this test
        "http://127.0.0.1:8080/erddap/status.html", // will always fail this test
        "https://127.0.0.1:8443/cwexperimental/login.html", // the links to log in (upper right of most web pages) will
                                                            // fail on my test computer
        "https://192.168.31.18/",
        "https://basin.ceoe.udel.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building
                                                         // failed:
                                                         // sun.security.provider.certpath.SunCertPathBuilderException:
                                                         // unable to find valid certification path to requested target
        "http://coastwatch.pfeg.noaa.gov:8080/", // java.net.SocketTimeoutException: Connect timed out
        "https://coastwatch.pfeg.noaa.gov/erddap/files/cwwcNDBCMet/nrt/NDBC_{41008,41009,41010}_met.nc", // intended for
                                                                                                         // curl
                                                                                                         // (globbing)
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/pmelTaoDySst.csv?longitude,latitude,time,station,wmo_platform_code,T_25&time>=2015-05-23T12:00:00Z&time<=2015-05-31T12:00:00Z", // always
                                                                                                                                                                                          // fails
                                                                                                                                                                                          // because
                                                                                                                                                                                          // of
                                                                                                                                                                                          // invalid
                                                                                                                                                                                          // character
        "https://dev.mysql.com", // fails here, but works in browser
        "https://myhsts.org", // javax.net.ssl.SSLHandshakeException: No subject alternative DNS name matching
                              // myhsts.org found.
        "https://gcoos5.geos.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building
                                                          // failed:
                                                          // sun.security.provider.certpath.SunCertPathBuilderException:
                                                          // unable to find valid certification path to requested target
        "https://gcoos4.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building failed:
                                                     // sun.security.provider.certpath.SunCertPathBuilderException:
                                                     // unable to find valid certification path to requested target
        "https://github.com/ERDDAP/", // fails here, but works in browser
        "http://localhost:8080/manager/html/", // will always fail this test
        "https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core", // it's clever: no follow
        "https://mvnrepository.com/artifact/com.codahale.metrics/metrics-core/3.0.2", // it's clever: no follow
        "https://mvnrepository.com/artifact/org.postgresql/postgresql", // it's clever: no follow
        "https://mvnrepository.com/", // it's clever: no follow
        "https://noaa-goes17.s3.us-east-1.amazonaws.com", // will always fail
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/", // will always fail
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2019/235/22/OR_ABI-L1b-RadC-M6C01_G17_s20192352201196_e20192352203569_c20192352204013.nc", // always
                                                                                                                                                                // fails
        "https://whatismyipaddress.com/ip-lookup", // it's clever: no follow
        "https://www.adobe.com", // fails here, but works in browser
        "https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html#time-zone-leap-seconds", // failes in test, works in brownser
        "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day.timeGaps", // dataset not found
    };
    // https://unitsofmeasure.org/ucum.html fails in tests because of certificate,
    // but succeeds in my browser. Others are like this, too.
    for (int linei = 0; linei < lines.length; linei++) {
      String urls[] = String2.extractAllCaptureGroupsAsHashSet(lines[linei], pattern, 1).toArray(new String[0]);
      for (int urli = 0; urli < urls.length; urli++) {
        // just try a given url once
        if (tried.contains(urls[urli]))
          continue;
        tried.add(urls[urli]);

        String ttUrl = XML.decodeEntities(urls[urli]);
        if (String2.indexOf(skip, ttUrl) >= 0)
          continue;
        String msg = null;
        try {
          Object[] o3 = SSR.getUrlConnBufferedInputStream(ttUrl,
              20000, false, true); // timeOutMillis, requestCompression, touchMode
          if (o3[0] == null) {
            ((InputStream) o3[1]).close();
            throw new IOException("The URL for SSR.testForBrokenLinks can't be an AWS S3 URL.");
          }
          HttpURLConnection conn = (HttpURLConnection) (o3[0]);
          int code = conn.getResponseCode();
          if (code != 200)
            msg = " code=" + code + " " + ttUrl;
        } catch (Exception e) {
          msg = " code=ERR " + ttUrl + " error=\n" +
              e.toString() + "\n";
        }
        if (msg != null) {
          String fullMsg = "#" + ++errorCount + " line=" + String2.left("" + (linei + 1), 4) + msg;
          String2.log(fullMsg);
          log.append(fullMsg + "\n");
        }
      }
    }
    if (log.length() > 0)
      throw new RuntimeException(
          "\nSSR.testForBrokenLinks(" + tUrl + ") found:\n" +
              log.toString());
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testForBrokenLinks() throws Exception {
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/oceanicAtmosphericAcronyms.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/fipscounty.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/keywords.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/time.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/units.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/urls.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/oceanicAtmosphericVariableNames.html");

    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/AccessToPrivateDatasets.html");
    //this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/changes.html"); // todo re-enable, a couple links seem to be broken, needs more investigation
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/EDDTableFromEML.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/grids.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/NCCSV.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/NCCSV_1.00.html");
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/setup.html"); // todo re-enable, a number of links are broken
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/setupDatasetsXml.html"); // todo re-enable, a number of links are broken

    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/information.html"); // todo  rtech link breaks, but its already commented out, remove fully?
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/rest.html");
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/griddap/documentation.html"); // todo re-enable multiple links error
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/tabledap/documentation.html"); // todo re-enable, several broken links
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/files/documentation.html"); // todo re-enable, adobe link failes to load
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/wms/documentation.html"); // todo re-enable esri link is broken (and others)
    //this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/images/erddapTalk/TablesAndGrids.html"); // doesn't exist?
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/images/erddapTalk/erdData.html"); // doesn't exist
  }

  /**
   * This test the json-ld responses from the ERDDAP at EDStatic.erddapUrl.
   * It assumes jplMURSST41 datasets is available.
   *
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testJsonld() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testJsonld");
    int po;

    // info list all datasets
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
        EDStatic.defaultPIppQuery);

    // json-ld all datasets
    expected = "<script type=\"application/ld+json\">\n" +
        "{\n" +
        "  \"@context\": \"http://schema.org\",\n" +
        "  \"@type\": \"DataCatalog\",\n" +
        "  \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n" +
        "  \"url\": \"http://localhost:" + PORT + "/erddap\",\n" +
        "  \"publisher\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"ERDDAP Jetty Install\",\n" +
        "    \"address\": {\n" +
        "      \"@type\": \"PostalAddress\",\n" +
        "      \"addressCountry\": \"USA\",\n" +
        "      \"addressLocality\": \"123 Irrelevant St., Nowhere\",\n" +
        "      \"addressRegion\": \"AK\",\n" +
        "      \"postalCode\": \"99504\"\n" +
        "    },\n" +
        "    \"telephone\": \"555-555-5555\",\n" +
        "    \"email\": \"nobody@example.com\",\n" +
        "    \"sameAs\": \"http://example.com\"\n"
        +
        "  },\n" +
        "  \"fileFormat\": [\n" +
        "    \"application/geo+json\",\n" +
        "    \"application/json\",\n" +
        "    \"text/csv\"\n" +
        "  ],\n" +
        "  \"isAccessibleForFree\": \"True\",\n" +
        "  \"dataset\": [\n" +
        "    {\n" +
        "      \"@type\": \"Dataset\",\n" +
        "      \"name\": \"";
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    {\n" +
        "      \"@type\": \"Dataset\",\n" +
        "      \"name\": \"Fluorescence Line Height, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)\",\n" +
        "      \"sameAs\": \"http://localhost:" + PORT + "/erddap/info/erdMH1cflh1day/index.html\"\n" +
        "    }";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // json-ld 1 dataset
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/jplMURSST41/index.html");
    expected = "<script type=\"application/ld+json\">\n" +
        "{\n" +
        "  \"@context\": \"http://schema.org\",\n" +
        "  \"@type\": \"Dataset\",\n" +
        "  \"name\": \"Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global, 0.01°, 2002-present, Daily\",\n"
        +
        "  \"headline\": \"jplMURSST41\",\n" +
        "  \"description\": \"This is a merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL). This daily, global, Multi-scale, Ultra-high Resolution (MUR) Sea Surface Temperature (SST) 1-km data set, Version 4.1, is produced at JPL under the NASA MEaSUREs program. For details, see https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1 . This dataset is part of the Group for High-Resolution Sea Surface Temperature (GHRSST) project. The data for the most recent 7 days is usually revised everyday.  The data for other days is sometimes revised.\\n" +
        "_NCProperties=version=2,netcdf=4.7.4,hdf5=1.8.12\\n" +
        "acknowledgement=Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.\\n"
        +
        "cdm_data_type=Grid\\n" +
        "comment=MUR = \\\"Multi-scale Ultra-high Resolution\\\"\\n" +
        "Conventions=CF-1.6, COARDS, ACDD-1.3\\n" +
        "Easternmost_Easting=180.0\\n" +
        "file_quality_level=3\\n" +
        "gds_version_id=2.0\\n" +
        "geospatial_lat_max=89.99\\n" +
        "geospatial_lat_min=-89.99\\n" +
        "geospatial_lat_resolution=0.01\\n" +
        "geospatial_lat_units=degrees_north\\n" +
        "geospatial_lon_max=180.0\\n" +
        "geospatial_lon_min=-179.99\\n" +
        "geospatial_lon_resolution=0.01\\n" +
        "geospatial_lon_units=degrees_east\\n" +
        "history=created at nominal 4-day latency; replaced nrt (1-day latency) version.\\n" +
        "Data is downloaded daily from https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/ to NOAA NMFS SWFSC ERD by erd.data@noaa.gov .\\n"
        +
        "The data for the most recent 7 days is usually revised everyday. The data for other days is sometimes revised.\\n"
        +
        "id=MUR-JPL-L4-GLOB-v04.1\\n" +
        "infoUrl=https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\\n" +
        "institution=NASA JPL\\n" +
        "keywords_vocabulary=GCMD Science Keywords\\n" +
        "naming_authority=org.ghrsst\\n" +
        "netcdf_version_id=4.1\\n" +
        "Northernmost_Northing=89.99\\n" +
        "platform=Terra, Aqua, GCOM-W, MetOp-B, Buoys/Ships\\n" +
        "processing_level=L4\\n" +
        "project=NASA Making Earth Science Data Records for Use in Research Environments (MEaSUREs) Program\\n" +
        "references=https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\\n" +
        "sensor=MODIS, AMSR2, AVHRR, in-situ\\n" +
        "source=MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRRMTB_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\\n" +
        "sourceUrl=(local files)\\n" +
        "Southernmost_Northing=-89.99\\n" +
        "spatial_resolution=0.01 degrees\\n" +
        "standard_name_vocabulary=CF Standard Name Table v70\\n" +
        "testOutOfDate=now-3days\\n" +
        "time_coverage_end=2024-04-23T09:00:00Z\\n" +
        "time_coverage_start=2002-06-01T09:00:00Z\\n" +
        "Westernmost_Easting=-179.99\",\n" +
        "  \"url\": \"http://localhost:" + PORT + "/erddap/griddap/jplMURSST41.html\",\n" +
        "  \"includedInDataCatalog\": {\n" +
        "    \"@type\": \"DataCatalog\",\n" +
        "    \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n" +
        "    \"sameAs\": \"http://localhost:" + PORT + "/erddap\"\n" +
        "  },\n" +
        "  \"keywords\": [\n" +
        "    \"analysed\",\n" +
        "    \"analysed_sst\",\n" +
        "    \"analysis\",\n" +
        "    \"analysis_error\",\n" +
        "    \"area\",\n" +
        "    \"binary\",\n" +
        "    \"composite\",\n" +
        "    \"daily\",\n" +
        "    \"data\",\n" +
        "    \"day\",\n" +
        "    \"deviation\",\n" +
        "    \"distribution\",\n" +
        "    \"Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature\",\n" +
        "    \"error\",\n" +
        "    \"estimated\",\n" +
        "    \"field\",\n" +
        "    \"final\",\n" +
        "    \"foundation\",\n" +
        "    \"fraction\",\n" +
        "    \"ghrsst\",\n" +
        "    \"high\",\n" +
        "    \"ice\",\n" +
        "    \"ice distribution\",\n" +
        "    \"identifier\",\n" +
        "    \"jet\",\n" +
        "    \"jpl\",\n" +
        "    \"laboratory\",\n" +
        "    \"land\",\n" +
        "    \"land_binary_mask\",\n" +
        "    \"mask\",\n" +
        "    \"multi\",\n" +
        "    \"multi-scale\",\n" +
        "    \"mur\",\n" +
        "    \"nasa\",\n" +
        "    \"ocean\",\n" +
        "    \"oceans\",\n" +
        "    \"product\",\n" +
        "    \"propulsion\",\n" +
        "    \"resolution\",\n" +
        "    \"scale\",\n" +
        "    \"sea\",\n" +
        "    \"sea ice area fraction\",\n" +
        "    \"sea/land\",\n" +
        "    \"sea_ice_fraction\",\n" +
        "    \"sea_surface_foundation_temperature\",\n" +
        "    \"sst\",\n" +
        "    \"standard\",\n" +
        "    \"statistics\",\n" +
        "    \"surface\",\n" +
        "    \"temperature\",\n" +
        "    \"time\",\n" +
        "    \"ultra\",\n" +
        "    \"ultra-high\"\n" +
        "  ],\n" +
        "  \"license\": \"These data are available free of charge under the JPL PO.DAAC data policy.\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\",\n"
        +
        "  \"variableMeasured\": [\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"time\",\n" +
        "      \"alternateName\": \"reference time of sst field\",\n" +
        "      \"description\": \"reference time of sst field\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"T\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"Nominal time of analyzed fields\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"reference time of sst field\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"time_origin\",\n" +
        "          \"value\": \"01-JAN-1970 00:00:00\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": \"2024-04-23T09:00:00Z\",\n" + // changes
        "      \"minValue\": \"2002-06-01T09:00:00Z\",\n" +
        "      \"propertyID\": \"time\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"latitude\",\n" +
        "      \"alternateName\": \"Latitude\",\n" +
        "      \"description\": \"Latitude\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Lat\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"Y\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Location\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Latitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"latitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 90\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -90\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": 89.99,\n" +
        "      \"minValue\": -89.99,\n" +
        "      \"propertyID\": \"latitude\",\n" +
        "      \"unitText\": \"degrees_north\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"longitude\",\n" +
        "      \"alternateName\": \"Longitude\",\n" +
        "      \"description\": \"Longitude\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Lon\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"X\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Location\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Longitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"longitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 180\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -180\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": 180,\n" +
        "      \"minValue\": -179.99,\n" +
        "      \"propertyID\": \"longitude\",\n" +
        "      \"unitText\": \"degrees_east\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"analysed_sst\",\n" +
        "      \"alternateName\": \"Analysed Sea Surface Temperature\",\n" +
        "      \"description\": \"Analysed Sea Surface Temperature\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -7.768000000000001\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 32\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"\\\"Final\\\" version using Multi-Resolution Variational Analysis (MRVA) method for interpolation\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Analysed Sea Surface Temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRRMTB_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"sea_surface_foundation_temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 57.767\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -7.767000000000003\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"sea_surface_foundation_temperature\",\n" +
        "      \"unitText\": \"degree_C\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"analysis_error\",\n" +
        "      \"alternateName\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
        "      \"description\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -327.68\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 5\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Statistics\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Estimated Error Standard Deviation of analysed_sst\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 327.67\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 0\n" +
        "        }\n" +
        "      ],\n" +
        "      \"unitText\": \"degree_C\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"mask\",\n" +
        "      \"alternateName\": \"Sea/Land Field Composite Mask\",\n" +
        "      \"description\": \"Sea/Land Field Composite Mask\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -128\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 20\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"mask can be used to further filter the data.\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_masks\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_meanings\",\n" +
        "          \"value\": \"open_sea land open_lake open_sea_with_ice_in_the_grid open_lake_with_ice_in_the_grid\"\n"
        +
        "        },\n" +
        // "        {\n" +
        // "          \"@type\": \"PropertyValue\",\n" +
        // "          \"name\": \"flag_values\",\n" +
        // "          \"value\": 1\n" +
        // "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Identifier\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Sea/Land Field Composite Mask\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"GMT \\\"grdlandmask\\\", ice flag from sea_ice_fraction data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"land_binary_mask\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 31\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 1\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"land_binary_mask\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"sea_ice_fraction\",\n" +
        "      \"alternateName\": \"Sea Ice Area Fraction\",\n" +
        "      \"description\": \"Sea Ice Area Fraction\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -1.28\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"ice fraction is a dimensionless quantity between 0 and 1; it has been interpolated by a nearest neighbor approach; EUMETSAT OSI-SAF files used: ice_conc_nh_polstere-100_multi_202404161200.nc, ice_conc_sh_polstere-100_multi_202404161200.nc.\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Ice Distribution\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Sea Ice Area Fraction\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"EUMETSAT OSI-SAF, copyright EUMETSAT\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"sea_ice_area_fraction\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 0\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"sea_ice_area_fraction\",\n" +
        "      \"unitText\": \"1\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"creator\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"JPL MUR SST project\",\n" +
        "    \"email\": \"ghrsst@podaac.jpl.nasa.gov\",\n" +
        "    \"sameAs\": \"https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\"\n" +
        "  },\n" +
        "  \"publisher\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"GHRSST Project Office\",\n" +
        "    \"email\": \"ghrsst-po@nceo.ac.uk\",\n" +
        "    \"sameAs\": \"https://www.ghrsst.org\"\n" +
        "  },\n" +
        "  \"dateCreated\": \"2024-04-24T07:35:03Z\",\n" + // changes
        "  \"identifier\": \"org.ghrsst/MUR-JPL-L4-GLOB-v04.1\",\n" +
        "  \"version\": \"04.1\",\n" +
        "  \"temporalCoverage\": \"2002-06-01T09:00:00Z/2024-04-23T09:00:00Z\",\n" + // end date changes
        "  \"spatialCoverage\": {\n" +
        "    \"@type\": \"Place\",\n" +
        "    \"geo\": {\n" +
        "      \"@type\": \"GeoShape\",\n" +
        "      \"box\": \"-89.99 -179.99 89.99 180\"\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "</script>\n";
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(results.substring(po, Math.min(results.length(), po + expected.length())),
        expected, "results=\n" + results);
  }

  /**
   * This is used by Bob to do simple tests of Advanced Search.
   * 
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testAdvancedSearch() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String htmlUrl = EDStatic.erddapUrl + "/search/advanced.html?page=1&itemsPerPage=1000";
    String csvUrl = EDStatic.erddapUrl + "/search/advanced.csv?page=1&itemsPerPage=1000";
    String expected = "cwwcNDBCMet";
    String expected2, query, results;
    String2.log("\n*** Erddap.testAdvancedSearch\n" +
        "This assumes localhost ERDDAP is running with at least cwwcNDBCMet.");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
        "&searchFor=CWWCndbc",
        "&protocol=TAbleDAp",
        "&standard_name=sea_surface_WAVE_significant_height",
        "&minLat=0&maxLat=45",
        "&minLon=-135&maxLon=-120",
        "&minTime=now-20years&maxTime=now-19years" };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
    }

    // valid for .html but error for .csv: protocol
    query = "&searchFor=CWWCndbc&protocol=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Your query produced no matching results. (protocol=gibberish)\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: standard_name
    query = "&searchFor=CWWCndbc&standard_name=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Your query produced no matching results. (standard_name=gibberish)\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minLat > &maxLat
    query = "&searchFor=CWWCndbc&minLat=45&maxLat=0";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: minLat=45.0 > maxLat=0.0\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minTime > &maxTime
    query = "&searchFor=CWWCndbc&minTime=now-10years&maxTime=now-11years";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: minTime=now-10years > maxTime=now-11years\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

  }

  /* EDDTableFromNccsvFiles */

  /**
   * This tests actual_range in .nccsvMetadata and .nccsv responses.
   * This requires pmelTaoDySst and rPmelTaoDySst in localhost erddap.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testActualRange() throws Throwable {
    // String2.log("\n****************** EDDTableFromNccsv.testActualRange\n" +
    // "!!!This test requires pmelTaoDySst and rlPmelTaoDySst in localhost
    // ERDDAP.\n");
    // testVerboseOn();
    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String baseUrl = "http://localhost:" + PORT + "/erddap/tabledap/pmelTaoDySst";
    String rbaseUrl = "http://localhost:" + PORT + "/erddap/tabledap/rlPmelTaoDySst";

    // *** test getting .nccsvMetadata for entire dataset
    tQuery = ".nccsvMetadata";
    expected = "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n" +
        "*GLOBAL*,cdm_data_type,TimeSeries\n" +
        "*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n" +
        "*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,creator_type,group\n" +
        "*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n" +
        "*GLOBAL*,Easternmost_Easting,357.0d\n" +
        "*GLOBAL*,featureType,TimeSeries\n" +
        "*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,geospatial_lat_max,21.0d\n" +
        "*GLOBAL*,geospatial_lat_min,-25.0d\n" +
        "*GLOBAL*,geospatial_lat_units,degrees_north\n" +
        "*GLOBAL*,geospatial_lon_max,357.0d\n" +
        "*GLOBAL*,geospatial_lon_min,0.0d\n" +
        "*GLOBAL*,geospatial_lon_units,degrees_east\n" +
        "*GLOBAL*,geospatial_vertical_max,15.0d\n" +
        "*GLOBAL*,geospatial_vertical_min,1.0d\n" +
        "*GLOBAL*,geospatial_vertical_positive,down\n" +
        "*GLOBAL*,geospatial_vertical_units,m\n" +
        "*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n"
        +
        "dddd-dd-dd Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.\"\n"
        +
        "*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n"
        +
        "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
        "*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n"
        +
        "*GLOBAL*,Northernmost_Northing,21.0d\n" +
        "*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n"
        +
        "*GLOBAL*,sourceUrl,(local files)\n" +
        "*GLOBAL*,Southernmost_Northing,-25.0d\n" +
        "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
        "*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n" +
        "*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n"
        +
        "*GLOBAL*,testOutOfDate,now-3days\n" +
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n" +
        "*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n" +
        "*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n" +
        "*GLOBAL*,Westernmost_Easting,0.0d\n" +
        "array,*DATA_TYPE*,String\n" +
        "array,ioos_category,Identifier\n" +
        "array,long_name,Array\n" +
        "station,*DATA_TYPE*,String\n" +
        "station,cf_role,timeseries_id\n" +
        "station,ioos_category,Identifier\n" +
        "station,long_name,Station\n" +
        "wmo_platform_code,*DATA_TYPE*,int\n" +
        "wmo_platform_code,actual_range,13001i,2147483647i\n" +
        "wmo_platform_code,ioos_category,Identifier\n" +
        "wmo_platform_code,long_name,WMO Platform Code\n" +
        "wmo_platform_code,missing_value,2147483647i\n" +
        "longitude,*DATA_TYPE*,float\n" +
        "longitude,_CoordinateAxisType,Lon\n" +
        "longitude,actual_range,0.0f,357.0f\n" +
        "longitude,axis,X\n" +
        "longitude,epic_code,502i\n" +
        "longitude,ioos_category,Location\n" +
        "longitude,long_name,Nominal Longitude\n" +
        "longitude,missing_value,1.0E35f\n" +
        "longitude,standard_name,longitude\n" +
        "longitude,type,EVEN\n" +
        "longitude,units,degrees_east\n" +
        "latitude,*DATA_TYPE*,float\n" +
        "latitude,_CoordinateAxisType,Lat\n" +
        "latitude,actual_range,-25.0f,21.0f\n" +
        "latitude,axis,Y\n" +
        "latitude,epic_code,500i\n" +
        "latitude,ioos_category,Location\n" +
        "latitude,long_name,Nominal Latitude\n" +
        "latitude,missing_value,1.0E35f\n" +
        "latitude,standard_name,latitude\n" +
        "latitude,type,EVEN\n" +
        "latitude,units,degrees_north\n" +
        "time,*DATA_TYPE*,String\n" +
        "time,_CoordinateAxisType,Time\n" +
        "time,actual_range,1977-11-03T12:00:00Z\\ndddd-dd-ddT12:00:00Z\n" + // stop time changes
        "time,axis,T\n" +
        "time,ioos_category,Time\n" +
        "time,long_name,Centered Time\n" +
        "time,point_spacing,even\n" +
        "time,standard_name,time\n" +
        "time,time_origin,01-JAN-1970 00:00:00\n" +
        "time,type,EVEN\n" +
        "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
        "depth,*DATA_TYPE*,float\n" +
        "depth,_CoordinateAxisType,Height\n" +
        "depth,_CoordinateZisPositive,down\n" +
        "depth,actual_range,1.0f,15.0f\n" +
        "depth,axis,Z\n" +
        "depth,epic_code,3i\n" +
        "depth,ioos_category,Location\n" +
        "depth,long_name,Depth\n" +
        "depth,missing_value,1.0E35f\n" +
        "depth,positive,down\n" +
        "depth,standard_name,depth\n" +
        "depth,type,EVEN\n" +
        "depth,units,m\n" +
        "T_25,*DATA_TYPE*,float\n" +
        "T_25,_FillValue,1.0E35f\n" +
        "T_25,actual_range,17.12f,35.4621f\n" +
        "T_25,colorBarMaximum,32.0d\n" +
        "T_25,colorBarMinimum,0.0d\n" +
        "T_25,epic_code,25i\n" +
        "T_25,generic_name,temp\n" +
        "T_25,ioos_category,Temperature\n" +
        "T_25,long_name,Sea Surface Temperature\n" +
        "T_25,missing_value,1.0E35f\n" +
        "T_25,name,T\n" +
        "T_25,standard_name,sea_surface_temperature\n" +
        "T_25,units,degree_C\n" +
        "QT_5025,*DATA_TYPE*,float\n" +
        "QT_5025,_FillValue,1.0E35f\n" +
        "QT_5025,actual_range,0.0f,5.0f\n" +
        "QT_5025,colorBarContinuous,false\n" +
        "QT_5025,colorBarMaximum,6.0d\n" +
        "QT_5025,colorBarMinimum,0.0d\n" +
        "QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n"
        +
        "QT_5025,epic_code,5025i\n" +
        "QT_5025,generic_name,qt\n" +
        "QT_5025,ioos_category,Quality\n" +
        "QT_5025,long_name,Sea Surface Temperature Quality\n" +
        "QT_5025,missing_value,1.0E35f\n" +
        "QT_5025,name,QT\n" +
        "ST_6025,*DATA_TYPE*,float\n" +
        "ST_6025,_FillValue,1.0E35f\n" +
        "ST_6025,actual_range,0.0f,5.0f\n" +
        "ST_6025,colorBarContinuous,false\n" +
        "ST_6025,colorBarMaximum,8.0d\n" +
        "ST_6025,colorBarMinimum,0.0d\n" +
        "ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n"
        +
        "ST_6025,epic_code,6025i\n" +
        "ST_6025,generic_name,st\n" +
        "ST_6025,ioos_category,Other\n" +
        "ST_6025,long_name,Sea Surface Temperature Source\n" +
        "ST_6025,missing_value,1.0E35f\n" +
        "ST_6025,name,ST\n" +
        "\n" +
        "*END_METADATA*\n";

    // note that there is actual_range info
    results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "\\*GLOBAL\\*,time_coverage_end,....-..-..T12:00:00Z\n",
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results = results.replaceAll(
        "time,actual_range,1977-11-03T12:00:00Z\\\\n....-..-..T12:00:00Z\n",
        "time,actual_range,1977-11-03T12:00:00Z\\\\ndddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "\\*GLOBAL\\*,time_coverage_end,....-..-..T12:00:00Z\n",
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results = results.replaceAll(
        "time,actual_range,1977-11-03T12:00:00Z\\\\n....-..-..T12:00:00Z\n",
        "time,actual_range,1977-11-03T12:00:00Z\\\\ndddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test getting .nccsv
    tQuery = ".nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05";
    expected = "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n" +
        "*GLOBAL*,cdm_data_type,TimeSeries\n" +
        "*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n" +
        "*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,creator_type,group\n" +
        "*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n" +
        "*GLOBAL*,Easternmost_Easting,357.0d\n" +
        "*GLOBAL*,featureType,TimeSeries\n" +
        "*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,geospatial_lat_max,21.0d\n" +
        "*GLOBAL*,geospatial_lat_min,-25.0d\n" +
        "*GLOBAL*,geospatial_lat_units,degrees_north\n" +
        "*GLOBAL*,geospatial_lon_max,357.0d\n" +
        "*GLOBAL*,geospatial_lon_min,0.0d\n" +
        "*GLOBAL*,geospatial_lon_units,degrees_east\n" +
        "*GLOBAL*,geospatial_vertical_max,15.0d\n" +
        "*GLOBAL*,geospatial_vertical_min,1.0d\n" +
        "*GLOBAL*,geospatial_vertical_positive,down\n" +
        "*GLOBAL*,geospatial_vertical_units,m\n" +
        "*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n";
        // +
        // "dddd-dd-dd Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.\\n";
    // "2017-05-26T18:30:46Z (local files)\\n" +
    // "2017-05-26T18:30:46Z
    expected2 = "http://localhost:" + PORT + "/erddap/tabledap/pmelTaoDySst.nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05\"\n" +
        "*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n"
        +
        "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
        "*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n"
        +
        "*GLOBAL*,Northernmost_Northing,21.0d\n" +
        "*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n"
        +
        "*GLOBAL*,sourceUrl,(local files)\n" +
        "*GLOBAL*,Southernmost_Northing,-25.0d\n" +
        "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
        "*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n" +
        "*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n"
        +
        "*GLOBAL*,testOutOfDate,now-3days\n" +
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n" +
        "*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n" +
        "*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n" +
        "*GLOBAL*,Westernmost_Easting,0.0d\n" +
        "array,*DATA_TYPE*,String\n" +
        "array,ioos_category,Identifier\n" +
        "array,long_name,Array\n" +
        "station,*DATA_TYPE*,String\n" +
        "station,cf_role,timeseries_id\n" +
        "station,ioos_category,Identifier\n" +
        "station,long_name,Station\n" +
        "wmo_platform_code,*DATA_TYPE*,int\n" +
        "wmo_platform_code,ioos_category,Identifier\n" +
        "wmo_platform_code,long_name,WMO Platform Code\n" +
        "wmo_platform_code,missing_value,2147483647i\n" +
        "longitude,*DATA_TYPE*,float\n" +
        "longitude,_CoordinateAxisType,Lon\n" +
        "longitude,axis,X\n" +
        "longitude,epic_code,502i\n" +
        "longitude,ioos_category,Location\n" +
        "longitude,long_name,Nominal Longitude\n" +
        "longitude,missing_value,1.0E35f\n" +
        "longitude,standard_name,longitude\n" +
        "longitude,type,EVEN\n" +
        "longitude,units,degrees_east\n" +
        "latitude,*DATA_TYPE*,float\n" +
        "latitude,_CoordinateAxisType,Lat\n" +
        "latitude,axis,Y\n" +
        "latitude,epic_code,500i\n" +
        "latitude,ioos_category,Location\n" +
        "latitude,long_name,Nominal Latitude\n" +
        "latitude,missing_value,1.0E35f\n" +
        "latitude,standard_name,latitude\n" +
        "latitude,type,EVEN\n" +
        "latitude,units,degrees_north\n" +
        "time,*DATA_TYPE*,String\n" +
        "time,_CoordinateAxisType,Time\n" +
        "time,axis,T\n" +
        "time,ioos_category,Time\n" +
        "time,long_name,Centered Time\n" +
        "time,point_spacing,even\n" +
        "time,standard_name,time\n" +
        "time,time_origin,01-JAN-1970 00:00:00\n" +
        "time,type,EVEN\n" +
        "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
        "depth,*DATA_TYPE*,float\n" +
        "depth,_CoordinateAxisType,Height\n" +
        "depth,_CoordinateZisPositive,down\n" +
        "depth,axis,Z\n" +
        "depth,epic_code,3i\n" +
        "depth,ioos_category,Location\n" +
        "depth,long_name,Depth\n" +
        "depth,missing_value,1.0E35f\n" +
        "depth,positive,down\n" +
        "depth,standard_name,depth\n" +
        "depth,type,EVEN\n" +
        "depth,units,m\n" +
        "T_25,*DATA_TYPE*,float\n" +
        "T_25,_FillValue,1.0E35f\n" +
        "T_25,colorBarMaximum,32.0d\n" +
        "T_25,colorBarMinimum,0.0d\n" +
        "T_25,epic_code,25i\n" +
        "T_25,generic_name,temp\n" +
        "T_25,ioos_category,Temperature\n" +
        "T_25,long_name,Sea Surface Temperature\n" +
        "T_25,missing_value,1.0E35f\n" +
        "T_25,name,T\n" +
        "T_25,standard_name,sea_surface_temperature\n" +
        "T_25,units,degree_C\n" +
        "QT_5025,*DATA_TYPE*,float\n" +
        "QT_5025,_FillValue,1.0E35f\n" +
        "QT_5025,colorBarContinuous,false\n" +
        "QT_5025,colorBarMaximum,6.0d\n" +
        "QT_5025,colorBarMinimum,0.0d\n" +
        "QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n"
        +
        "QT_5025,epic_code,5025i\n" +
        "QT_5025,generic_name,qt\n" +
        "QT_5025,ioos_category,Quality\n" +
        "QT_5025,long_name,Sea Surface Temperature Quality\n" +
        "QT_5025,missing_value,1.0E35f\n" +
        "QT_5025,name,QT\n" +
        "ST_6025,*DATA_TYPE*,float\n" +
        "ST_6025,_FillValue,1.0E35f\n" +
        "ST_6025,colorBarContinuous,false\n" +
        "ST_6025,colorBarMaximum,8.0d\n" +
        "ST_6025,colorBarMinimum,0.0d\n" +
        "ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n"
        +
        "ST_6025,epic_code,6025i\n" +
        "ST_6025,generic_name,st\n" +
        "ST_6025,ioos_category,Other\n" +
        "ST_6025,long_name,Sea Surface Temperature Source\n" +
        "ST_6025,missing_value,1.0E35f\n" +
        "ST_6025,name,ST\n" +
        "\n" +
        "*END_METADATA*\n" +
        "array,station,wmo_platform_code,longitude,latitude,time,depth,T_25,QT_5025,ST_6025\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-01T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-02T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-03T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-04T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "*END_DATA*\n";

    // note no actual_range info
    results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "time_coverage_end,....-..-..T12:00:00Z\n",
        "time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf(expected2.substring(0, 100));
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery); // difference: 'r'baseUrl
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "time_coverage_end,....-..-..T12:00:00Z\n",
        "time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf(expected2.substring(0, 100));
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

  }
}
