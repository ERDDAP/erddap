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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.TableTests;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableCopy;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import org.eclipse.jetty.ee10.webapp.WebAppContext;

import tags.TagImageComparison;
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

    Thread.sleep(15 * 60 * 1000);
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
        // "info/index.html?page=1&itemsPerPage=1000", // Descriptions of datasets may contain char patterns
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
   * This is used by Bob to do simple tests of the basic Erddap services
   * from the ERDDAP at EDStatic.erddapUrl. It assumes Bob's test datasets are
   * available.
   *
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testErddapBasic() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testBasic");
    int po;
    int language = 0;
    EDStatic.sosActive = false; // currently, never true because sos is unfinished //some other tests may have
                                // left this as true

    // home page
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl); // redirects to index.html
    expected = "The small effort to set up ERDDAP brings many benefits.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/"); // redirects to index.html
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // test version info (opendap spec section 7.2.5)
    // "version" instead of datasetID
    expected = "Core Version: DAP/2.0\n" +
        "Server Version: dods/3.7\n" +
        "ERDDAP_version: " + EDStatic.erddapVersion + "\n";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // "version.txt"
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version.txt");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version.txt");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ".ver"
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/etopo180.ver");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.ver");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // help
    expected = "griddap to Request Data and Graphs from Gridded Datasets";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    expected = "tabledap to Request Data and Graphs from Tabular Datasets";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // error 404
    results = "";
    try {
      SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/gibberish");
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureTrue(results.indexOf("java.io.FileNotFoundException") >= 0, "results=\n" + results);

    // info list all datasets
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Argo float vertical profiles from Coriolis Global Data Assembly Centres") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.csv?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") < 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Argo float vertical profiles from Coriolis Global Data Assembly Centres") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.tsv");
    Test.ensureTrue(results.indexOf("\t") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    // search
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Do a Full Text Search for Datasets") >= 0, "results=\n" + results);
    // index.otherFileType must have ?searchFor=...

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">Global Temperature and Salinity Profile Programme (GTSPP) Data, 1985-present\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">GLOBEC NEP Rosette Bottle Data (2002)") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.htmlTable?" +
        EDStatic.defaultPIppQuery + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Global Temperature and Salinity Profile Programme (GTSPP) Data, 1985-present") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\n") > 0,
        "results=\n" + results);

    // .json
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel+sst");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
        +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
        +
        "    \"rows\": [\n" +
        "" +
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                        "ST_6025 (Sea Surface Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n" + //
                        "      [\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                        "ST_6025 (Sea Surface Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"],\n" + //
                        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                        "QST_5071 (Sigma-Theta Quality)\\n" + //
                        "SST_6071 (Sigma-Theta Source)\\n" + //
                        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"],\n" + //
                        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                        "QST_5071 (Sigma-Theta Quality)\\n" + //
                        "SST_6071 (Sigma-Theta Source)\\n" + //
                        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n"
        +
        "    ]\n" +
        "  }\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .json with jsonp
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel+sst&.jsonp=fnName");
    expected = "fnName({\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
        +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
        +
        "    \"rows\": [\n" +
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                        "ST_6025 (Sea Surface Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n" + //
                        "      [\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                        "ST_6025 (Sea Surface Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"],\n" + //
                        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                        "QST_5071 (Sigma-Theta Quality)\\n" + //
                        "SST_6071 (Sigma-Theta Source)\\n" + //
                        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"],\n" + //
                        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                        "QST_5071 (Sigma-Theta Quality)\\n" + //
                        "SST_6071 (Sigma-Theta Source)\\n" + //
                        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n" + //
        "    ]\n" +
        "  }\n" +
        "}\n" +
        ")";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // and read the header to see the mime type
    results = String2.toNewlineString(
        SSR.dosOrCShell("curl -i \"" + EDStatic.erddapUrl + "/search/index.json?" +
            EDStatic.defaultPIppQuery + "&searchFor=tao+pmel&.jsonp=fnName\"",
            120).toArray());
    po = results.indexOf("HTTP");
    results = results.substring(po);
    po = results.indexOf("chunked");
    results = results.substring(0, po + 7);
    expected = "HTTP/1.1 200 OK\n" +
        "Server: Jetty(12.0.8)\n" +
        "Date: Today\n" +
        "Content-Type: application/javascript;charset=utf-8\n" +
        "Content-Encoding: identity\n" +
        "Transfer-Encoding: chunked";
    results = results.replaceAll("Date: ..., .. [a-zA-Z]+ .... ..:..:.. ...\n", "Date: Today\n");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV1
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV1?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "[\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"]\n"
        +
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAdcp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP\", \"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "u_1205 (Eastward Sea Water Velocity, cm/s)\\n" + //
                        "QU_5205 (Eastward Sea Water Velocity Quality)\\n" + //
                        "v_1206 (Northward Sea Water Velocity, cm/s)\\n" + //
                        "QV_5206 (Northward Sea Water Velocity Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAdcp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAdcp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAdcp/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAdcp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAdcp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAdcp\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyCur/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents\", \"This dataset has daily Currents data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "U_320 (Eastward Sea Water Velocity, cm s-1)\\n" + //
                        "V_321 (Northward Sea Water Velocity, cm s-1)\\n" + //
                        "CS_300 (Sea Water Velocity, cm s-1)\\n" + //
                        "CD_310 (Direction of Sea Water Velocity, degrees_true)\\n" + //
                        "QCS_5300 (Current Speed Quality)\\n" + //
                        "QCD_5310 (Current Direction Quality)\\n" + //
                        "SCS_6300 (Current Speed Source)\\n" + //
                        "CIC_7300 (Current Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyCur_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyCur_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyCur/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyCur.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyCur&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyCur\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyT/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Temperature\", \"This dataset has daily Temperature data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_20 (Sea Water Temperature, degree_C)\\n" + //
                        "QT_5020 (Temperature Quality)\\n" + //
                        "ST_6020 (Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyT_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyT_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyT/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyT.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyT&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyT\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyW/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind\", \"This dataset has daily Wind data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "WU_422 (Zonal Wind, m s-1)\\n" + //
                        "WV_423 (Meridional Wind, m s-1)\\n" + //
                        "WS_401 (Wind Speed, m s-1)\\n" + //
                        "QWS_5401 (Wind Speed Quality)\\n" + //
                        "SWS_6401 (Wind Speed Source)\\n" + //
                        "WD_410 (Wind Direction, degrees_true)\\n" + //
                        "QWD_5410 (Wind Direction Quality)\\n" + //
                        "SWD_6410 (Wind Direction Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyW_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyW_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyW/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyW.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyW&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyW\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Position\", \"This dataset has daily Position data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "LON_502 (Precise Longitude, degree_east)\\n" + //
                        "QX_5502 (Longitude Quality)\\n" + //
                        "LAT_500 (Precise Latitude, degree_north)\\n" + //
                        "QY_5500 (Latitude Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyPos/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyPos\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyS/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Salinity\", \"This dataset has daily Salinity data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
                        "QS_5041 (Salinity Quality)\\n" + //
                        "SS_6041 (Salinity Source)\\n" + //
                        "SIC_8041 (Salinity Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyS_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyS_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyS/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyS.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyS&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyS\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEvap/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Evaporation\", \"This dataset has daily Evaporation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "E_250 (Evaporation, MM/HR)\\n" + //
                        "QE_5250 (Evaporation Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEvap_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEvap_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEvap/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEvap.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEvap&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEvap\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyTau/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress\", \"This dataset has daily Wind Stress data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "TX_442 (Zonal Wind Stress, N/m2)\\n" + //
                        "TY_443 (Meridional Wind Stress, N/m2)\\n" + //
                        "TAU_440 (Wind Stress, N/m2)\\n" + //
                        "TD_445 (Wind Stress Direction, degrees_true)\\n" + //
                        "QTAU_5440 (Wind Stress Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyTau_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyTau_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyTau/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyTau.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyTau&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyTau\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                        "QST_5071 (Sigma-Theta Quality)\\n" + //
                        "SST_6071 (Sigma-Theta Source)\\n" + //
                        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"This dataset has monthly Position data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "LON_502 (Precise Longitude, degree_east)\\n" + //
                        "QX_5502 (Longitude Quality)\\n" + //
                        "LAT_500 (Precise Latitude, degree_north)\\n" + //
                        "QY_5500 (Latitude Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoMonPos\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyIso/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, 20C Isotherm Depth\", \"This dataset has daily 20C Isotherm Depth data (the depth at which the ocean temperature is 20C) from the \\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "ISO_6 (20C Isotherm Depth, m)\\n" + //
                        "QI_5006 (20C Depth Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyIso_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyIso_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyIso/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyIso.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyIso&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyIso\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"This dataset has daily Air Temperature data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "AT_21 (Air Temperature, degree_C)\\n" + //
                        "QAT_5021 (Air Temperature Quality)\\n" + //
                        "SAT_6021 (Air Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAirt\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyDyn/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height\", \"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "DYN_13 (Dynamic Height, dyn-cm)\\n" + //
                        "QD_5013 (Dynamic Height Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyDyn_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyDyn_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyDyn/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyDyn.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyDyn&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyDyn\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyHeat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Heat Content\", \"This dataset has daily Heat Content data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "HTC_130 (Heat Content, 10**10 J m-2)\\n" + //
                        "HTC_5130 (Heat Content Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyHeat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyHeat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyHeat/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyHeat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyHeat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyHeat\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQlat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Latent Heat Flux\", \"This dataset has daily Latent Heat Flux data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "QL_137 (Latent Heat Flux, W m-2)\\n" + //
                        "QQL_5137 (Latent Heat Flux Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQlat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQlat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQlat/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQlat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQlat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQlat\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRh/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Relative Humidity\", \"This dataset has daily Relative Humidity data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "RH_910 (Relative Humidity, percent)\\n" + //
                        "QRH_5910 (Relative Humidity Quality)\\n" + //
                        "SRH_6910 (Relative Humidity Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRh_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRh_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRh/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRh.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRh&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRh\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQsen/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Sensible Heat Flux\", \"This dataset has daily Sensible Heat Flux data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "QS_138 (Sensible Heat Flux, W m-2)\\n" + //
                        "QQS_5138 (Sensible Heat Flux Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQsen_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQsen_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQsen/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQsen.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQsen&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQsen\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySss/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity\", \"This dataset has daily Sea Surface Salinity data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
                        "QS_5041 (Salinity Quality)\\n" + //
                        "SS_6041 (Salinity Source)\\n" + //
                        "SIC_7041 (Salinity Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySss_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySss_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySss/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySss.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySss&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySss\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Heat Flux Due To Rain\", \"This dataset has daily Heat Flux Due To Rain data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "QR_139 (Heat Flux Due To Rain, W m-2)\\n" + //
                        "QQR_5139 (Heat Flux Due To Rain Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRf/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRf\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRain/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation\", \"This dataset has daily Precipitation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "RN_485 (Precipitation, MM/HR)\\n" + //
                        "QRN_5485 (Precipitation Quality)\\n" + //
                        "SRN_6485 (Precipitation Source)\\n" + //
                        "RNS_486 (Precipitation Standard Deviation, MM/HR)\\n" + //
                        "RNP_487 (Percent Time Raining, percent)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRain_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRain_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRain/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRain.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRain&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRain\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Buoyancy Flux\", \"This dataset has daily Buoyancy Flux data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "BF_191 (Buoyancy Flux, 10**6 kg m-2 s-1)\\n" + //
                        "QBF_5191 (Buoyancy Flux Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBf/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBf\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLw/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Longwave Radiation\", \"This dataset has daily Incoming Longwave Radiation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "Ql_136 (Incoming Longwave Radiation, W m-2)\\n" + //
                        "QLW_5136 (Longwave Radiation Quality)\\n" + //
                        "SLW_6136 (Longwave Radiation Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLw_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLw_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLw/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLw.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLw&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLw\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Total Heat Flux\", \"This dataset has daily Total Heat Flux data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "QT_210 (Total Heat Flux, W/M**2)\\n" + //
                        "QQ0_5210 (Total Heat Flux Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQnet/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQnet\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                        "ST_6025 (Sea Surface Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                        "ST_6025 (Sea Surface Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                        "QST_5071 (Sigma-Theta Quality)\\n" + //
                        "SST_6071 (Sigma-Theta Source)\\n" + //
                        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRad/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Downgoing Shortwave Radiation\", \"This dataset has daily Downgoing Shortwave Radiation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "RD_495 (Downgoing Shortwave Radiation, W/M**2)\\n" + //
                        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
                        "SSW_6495 (Shortwave Radiation Source)\\n" + //
                        "RDS_496 (Shortwave Radiation Standard Deviation, W/M**2)\\n" + //
                        "RDP_497 (Shortwave Radiation Peak, W/M**2)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRad_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRad_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRad/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRad.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRad&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRad\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Net Shortwave Radiation\", \"This dataset has daily Net Shortwave Radiation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "SWN_1495 (Net Shortwave Radiation, W/M**2)\\n" + //
                        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySwnet/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySwnet\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEmp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Evaporation Minus Precipitation\", \"This dataset has daily Evaporation Minus Precipitation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "EMP_251 (Evaporation Minus Precipitation, mm/hr)\\n" + //
                        "QEMP_5251 (Evaporation Minus Precipitation Quality)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEmp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEmp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEmp/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEmp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEmp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEmp\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1998-present, Barometric (Air) Pressure\", \"This dataset has daily Barometric (Air) Pressure data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "BP_915 (Barometric (Air) Pressure, hPa)\\n" + //
                        "QBP_5915 (Barometric (Air) Pressure Quality)\\n" + //
                        "SBP_6915 (Barometric (Air) Pressure Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBp/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBp\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation\", \"This dataset has daily Net Longwave Radiation data from the\\n" + //
                        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "LWN_1136 (Net Longwave Radiation, W m-2)\\n" + //
                        "QLW_5136 (Longwave Radiation Quality)\\n" + //
                        "SLW_6136 (Longwave Radiation Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLwnet/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLwnet\"]\n" + //
                        "[\"\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"\", \"http://localhost:8080/erddap/files/testTableWithDepth/\", \"This is EDDTableWithDepth\", \"This is the summary\\n" + //
                        "\\n" + //
                        "cdm_data_type = TimeSeries\\n" + //
                        "VARIABLES:\\n" + //
                        "array\\n" + //
                        "station\\n" + //
                        "wmo_platform_code\\n" + //
                        "longitude (Nominal Longitude, degrees_east)\\n" + //
                        "latitude (Nominal Latitude, degrees_north)\\n" + //
                        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                        "depth (m)\\n" + //
                        "AT_21 (Air Temperature, degree_C)\\n" + //
                        "QAT_5021 (Air Temperature Quality)\\n" + //
                        "SAT_6021 (Air Temperature Source)\\n" + //
                        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"testTableWithDepth\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAdcp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP\", \"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "u_1205 (Eastward Sea Water Velocity, cm/s)\\n" + //
                "QU_5205 (Eastward Sea Water Velocity Quality)\\n" + //
                "v_1206 (Northward Sea Water Velocity, cm/s)\\n" + //
                "QV_5206 (Northward Sea Water Velocity Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAdcp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAdcp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAdcp/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAdcp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAdcp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAdcp\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyCur/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents\", \"This dataset has daily Currents data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "U_320 (Eastward Sea Water Velocity, cm s-1)\\n" + //
                "V_321 (Northward Sea Water Velocity, cm s-1)\\n" + //
                "CS_300 (Sea Water Velocity, cm s-1)\\n" + //
                "CD_310 (Direction of Sea Water Velocity, degrees_true)\\n" + //
                "QCS_5300 (Current Speed Quality)\\n" + //
                "QCD_5310 (Current Direction Quality)\\n" + //
                "SCS_6300 (Current Speed Source)\\n" + //
                "CIC_7300 (Current Instrument Code)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyCur_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyCur_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyCur/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyCur.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyCur&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyCur\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyT/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Temperature\", \"This dataset has daily Temperature data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "T_20 (Sea Water Temperature, degree_C)\\n" + //
                "QT_5020 (Temperature Quality)\\n" + //
                "ST_6020 (Temperature Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyT_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyT_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyT/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyT.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyT&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyT\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyW/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind\", \"This dataset has daily Wind data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "WU_422 (Zonal Wind, m s-1)\\n" + //
                "WV_423 (Meridional Wind, m s-1)\\n" + //
                "WS_401 (Wind Speed, m s-1)\\n" + //
                "QWS_5401 (Wind Speed Quality)\\n" + //
                "SWS_6401 (Wind Speed Source)\\n" + //
                "WD_410 (Wind Direction, degrees_true)\\n" + //
                "QWD_5410 (Wind Direction Quality)\\n" + //
                "SWD_6410 (Wind Direction Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyW_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyW_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyW/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyW.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyW&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyW\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Position\", \"This dataset has daily Position data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "LON_502 (Precise Longitude, degree_east)\\n" + //
                "QX_5502 (Longitude Quality)\\n" + //
                "LAT_500 (Precise Latitude, degree_north)\\n" + //
                "QY_5500 (Latitude Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyPos/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyPos\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyS/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Salinity\", \"This dataset has daily Salinity data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
                "QS_5041 (Salinity Quality)\\n" + //
                "SS_6041 (Salinity Source)\\n" + //
                "SIC_8041 (Salinity Instrument Code)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyS_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyS_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyS/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyS.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyS&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyS\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEvap/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Evaporation\", \"This dataset has daily Evaporation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "E_250 (Evaporation, MM/HR)\\n" + //
                "QE_5250 (Evaporation Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEvap_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEvap_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEvap/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEvap.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEvap&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEvap\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyTau/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress\", \"This dataset has daily Wind Stress data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "TX_442 (Zonal Wind Stress, N/m2)\\n" + //
                "TY_443 (Meridional Wind Stress, N/m2)\\n" + //
                "TAU_440 (Wind Stress, N/m2)\\n" + //
                "TD_445 (Wind Stress Direction, degrees_true)\\n" + //
                "QTAU_5440 (Wind Stress Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyTau_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyTau_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyTau/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyTau.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyTau&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyTau\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                "QST_5071 (Sigma-Theta Quality)\\n" + //
                "SST_6071 (Sigma-Theta Source)\\n" + //
                "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"This dataset has monthly Position data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "LON_502 (Precise Longitude, degree_east)\\n" + //
                "QX_5502 (Longitude Quality)\\n" + //
                "LAT_500 (Precise Latitude, degree_north)\\n" + //
                "QY_5500 (Latitude Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoMonPos\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyIso/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, 20C Isotherm Depth\", \"This dataset has daily 20C Isotherm Depth data (the depth at which the ocean temperature is 20C) from the \\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "ISO_6 (20C Isotherm Depth, m)\\n" + //
                "QI_5006 (20C Depth Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyIso_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyIso_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyIso/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyIso.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyIso&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyIso\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"This dataset has daily Air Temperature data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "AT_21 (Air Temperature, degree_C)\\n" + //
                "QAT_5021 (Air Temperature Quality)\\n" + //
                "SAT_6021 (Air Temperature Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAirt\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyDyn/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height\", \"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "DYN_13 (Dynamic Height, dyn-cm)\\n" + //
                "QD_5013 (Dynamic Height Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyDyn_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyDyn_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyDyn/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyDyn.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyDyn&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyDyn\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyHeat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Heat Content\", \"This dataset has daily Heat Content data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "HTC_130 (Heat Content, 10**10 J m-2)\\n" + //
                "HTC_5130 (Heat Content Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyHeat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyHeat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyHeat/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyHeat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyHeat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyHeat\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQlat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Latent Heat Flux\", \"This dataset has daily Latent Heat Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QL_137 (Latent Heat Flux, W m-2)\\n" + //
                "QQL_5137 (Latent Heat Flux Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQlat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQlat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQlat/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQlat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQlat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQlat\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRh/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Relative Humidity\", \"This dataset has daily Relative Humidity data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "RH_910 (Relative Humidity, percent)\\n" + //
                "QRH_5910 (Relative Humidity Quality)\\n" + //
                "SRH_6910 (Relative Humidity Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRh_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRh_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRh/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRh.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRh&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRh\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQsen/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Sensible Heat Flux\", \"This dataset has daily Sensible Heat Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QS_138 (Sensible Heat Flux, W m-2)\\n" + //
                "QQS_5138 (Sensible Heat Flux Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQsen_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQsen_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQsen/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQsen.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQsen&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQsen\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySss/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity\", \"This dataset has daily Sea Surface Salinity data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
                "QS_5041 (Salinity Quality)\\n" + //
                "SS_6041 (Salinity Source)\\n" + //
                "SIC_7041 (Salinity Instrument Code)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySss_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySss_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySss/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySss.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySss&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySss\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Heat Flux Due To Rain\", \"This dataset has daily Heat Flux Due To Rain data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QR_139 (Heat Flux Due To Rain, W m-2)\\n" + //
                "QQR_5139 (Heat Flux Due To Rain Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRf/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRf\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRain/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation\", \"This dataset has daily Precipitation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "RN_485 (Precipitation, MM/HR)\\n" + //
                "QRN_5485 (Precipitation Quality)\\n" + //
                "SRN_6485 (Precipitation Source)\\n" + //
                "RNS_486 (Precipitation Standard Deviation, MM/HR)\\n" + //
                "RNP_487 (Percent Time Raining, percent)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRain_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRain_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRain/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRain.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRain&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRain\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Buoyancy Flux\", \"This dataset has daily Buoyancy Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "BF_191 (Buoyancy Flux, 10**6 kg m-2 s-1)\\n" + //
                "QBF_5191 (Buoyancy Flux Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBf/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBf\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLw/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Longwave Radiation\", \"This dataset has daily Incoming Longwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "Ql_136 (Incoming Longwave Radiation, W m-2)\\n" + //
                "QLW_5136 (Longwave Radiation Quality)\\n" + //
                "SLW_6136 (Longwave Radiation Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLw_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLw_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLw/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLw.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLw&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLw\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Total Heat Flux\", \"This dataset has daily Total Heat Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QT_210 (Total Heat Flux, W/M**2)\\n" + //
                "QQ0_5210 (Total Heat Flux Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQnet/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQnet\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                "ST_6025 (Sea Surface Temperature Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                "ST_6025 (Sea Surface Temperature Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                "QST_5071 (Sigma-Theta Quality)\\n" + //
                "SST_6071 (Sigma-Theta Source)\\n" + //
                "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRad/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Downgoing Shortwave Radiation\", \"This dataset has daily Downgoing Shortwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "RD_495 (Downgoing Shortwave Radiation, W/M**2)\\n" + //
                "QSW_5495 (Shortwave Radiation Quality)\\n" + //
                "SSW_6495 (Shortwave Radiation Source)\\n" + //
                "RDS_496 (Shortwave Radiation Standard Deviation, W/M**2)\\n" + //
                "RDP_497 (Shortwave Radiation Peak, W/M**2)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRad_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRad_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRad/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRad.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRad&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRad\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Net Shortwave Radiation\", \"This dataset has daily Net Shortwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "SWN_1495 (Net Shortwave Radiation, W/M**2)\\n" + //
                "QSW_5495 (Shortwave Radiation Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySwnet/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySwnet\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEmp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Evaporation Minus Precipitation\", \"This dataset has daily Evaporation Minus Precipitation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "EMP_251 (Evaporation Minus Precipitation, mm/hr)\\n" + //
                "QEMP_5251 (Evaporation Minus Precipitation Quality)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEmp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEmp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEmp/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEmp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEmp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEmp\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1998-present, Barometric (Air) Pressure\", \"This dataset has daily Barometric (Air) Pressure data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "BP_915 (Barometric (Air) Pressure, hPa)\\n" + //
                "QBP_5915 (Barometric (Air) Pressure Quality)\\n" + //
                "SBP_6915 (Barometric (Air) Pressure Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBp/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBp\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation\", \"This dataset has daily Net Longwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "LWN_1136 (Net Longwave Radiation, W m-2)\\n" + //
                "QLW_5136 (Longwave Radiation Quality)\\n" + //
                "SLW_6136 (Longwave Radiation Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLwnet/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLwnet\"]\n" + //
                "[\"\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"\", \"http://localhost:8080/erddap/files/testTableWithDepth/\", \"This is EDDTableWithDepth\", \"This is the summary\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "AT_21 (Air Temperature, degree_C)\\n" + //
                "QAT_5021 (Air Temperature Quality)\\n" + //
                "SAT_6021 (Air Temperature Source)\\n" + //
                "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"testTableWithDepth\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlKVP
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlKVP?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyAdcp/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP\", \"Summary\":\"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "u_1205 (Eastward Sea Water Velocity, cm/s)\\n" + //
                "QU_5205 (Eastward Sea Water Velocity Quality)\\n" + //
                "v_1206 (Northward Sea Water Velocity, cm/s)\\n" + //
                "QV_5206 (Northward Sea Water Velocity Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAdcp_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAdcp_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyAdcp/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyAdcp.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAdcp&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyAdcp\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyCur\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyCur/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents\", \"Summary\":\"This dataset has daily Currents data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "U_320 (Eastward Sea Water Velocity, cm s-1)\\n" + //
                "V_321 (Northward Sea Water Velocity, cm s-1)\\n" + //
                "CS_300 (Sea Water Velocity, cm s-1)\\n" + //
                "CD_310 (Direction of Sea Water Velocity, degrees_true)\\n" + //
                "QCS_5300 (Current Speed Quality)\\n" + //
                "QCD_5310 (Current Direction Quality)\\n" + //
                "SCS_6300 (Current Speed Source)\\n" + //
                "CIC_7300 (Current Instrument Code)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyCur_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyCur_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyCur/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyCur.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyCur&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyCur\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyT.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyT\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyT.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyT/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Temperature\", \"Summary\":\"This dataset has daily Temperature data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "T_20 (Sea Water Temperature, degree_C)\\n" + //
                "QT_5020 (Temperature Quality)\\n" + //
                "ST_6020 (Temperature Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyT_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyT_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyT/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyT.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyT&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyT\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyW.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyW\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyW.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyW/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind\", \"Summary\":\"This dataset has daily Wind data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "WU_422 (Zonal Wind, m s-1)\\n" + //
                "WV_423 (Meridional Wind, m s-1)\\n" + //
                "WS_401 (Wind Speed, m s-1)\\n" + //
                "QWS_5401 (Wind Speed Quality)\\n" + //
                "SWS_6401 (Wind Speed Source)\\n" + //
                "WD_410 (Wind Direction, degrees_true)\\n" + //
                "QWD_5410 (Wind Direction Quality)\\n" + //
                "SWD_6410 (Wind Direction Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyW_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyW_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyW/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyW.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyW&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyW\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyPos\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyPos/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Position\", \"Summary\":\"This dataset has daily Position data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "LON_502 (Precise Longitude, degree_east)\\n" + //
                "QX_5502 (Longitude Quality)\\n" + //
                "LAT_500 (Precise Latitude, degree_north)\\n" + //
                "QY_5500 (Latitude Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyPos_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyPos_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyPos/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyPos.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyPos&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyPos\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyS.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyS\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyS.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyS/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Salinity\", \"Summary\":\"This dataset has daily Salinity data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
                "QS_5041 (Salinity Quality)\\n" + //
                "SS_6041 (Salinity Source)\\n" + //
                "SIC_8041 (Salinity Instrument Code)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyS_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyS_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyS/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyS.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyS&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyS\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyEvap/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Evaporation\", \"Summary\":\"This dataset has daily Evaporation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "E_250 (Evaporation, MM/HR)\\n" + //
                "QE_5250 (Evaporation Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEvap_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEvap_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyEvap/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyEvap.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEvap&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyEvap\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyTau\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyTau/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress\", \"Summary\":\"This dataset has daily Wind Stress data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "TX_442 (Zonal Wind Stress, N/m2)\\n" + //
                "TY_443 (Meridional Wind Stress, N/m2)\\n" + //
                "TAU_440 (Wind Stress, N/m2)\\n" + //
                "TD_445 (Wind Stress Direction, degrees_true)\\n" + //
                "QTAU_5440 (Wind Stress Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyTau_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyTau_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyTau/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyTau.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyTau&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyTau\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"Summary\":\"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                "QST_5071 (Sigma-Theta Quality)\\n" + //
                "SST_6071 (Sigma-Theta Source)\\n" + //
                "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySsd/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySsd\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"Summary\":\"This dataset has monthly Position data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "LON_502 (Precise Longitude, degree_east)\\n" + //
                "QX_5502 (Longitude Quality)\\n" + //
                "LAT_500 (Precise Latitude, degree_north)\\n" + //
                "QY_5500 (Latitude Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoMonPos\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyIso\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyIso/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, 20C Isotherm Depth\", \"Summary\":\"This dataset has daily 20C Isotherm Depth data (the depth at which the ocean temperature is 20C) from the \\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "ISO_6 (20C Isotherm Depth, m)\\n" + //
                "QI_5006 (20C Depth Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyIso_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyIso_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyIso/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyIso.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyIso&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyIso\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"Summary\":\"This dataset has daily Air Temperature data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "AT_21 (Air Temperature, degree_C)\\n" + //
                "QAT_5021 (Air Temperature Quality)\\n" + //
                "SAT_6021 (Air Temperature Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyAirt\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyDyn/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height\", \"Summary\":\"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "DYN_13 (Dynamic Height, dyn-cm)\\n" + //
                "QD_5013 (Dynamic Height Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyDyn_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyDyn_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyDyn/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyDyn.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyDyn&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyDyn\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyHeat/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Heat Content\", \"Summary\":\"This dataset has daily Heat Content data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "HTC_130 (Heat Content, 10**10 J m-2)\\n" + //
                "HTC_5130 (Heat Content Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyHeat_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyHeat_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyHeat/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyHeat.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyHeat&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyHeat\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyQlat/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Latent Heat Flux\", \"Summary\":\"This dataset has daily Latent Heat Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QL_137 (Latent Heat Flux, W m-2)\\n" + //
                "QQL_5137 (Latent Heat Flux Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQlat_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQlat_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyQlat/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyQlat.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQlat&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyQlat\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRh\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRh/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Relative Humidity\", \"Summary\":\"This dataset has daily Relative Humidity data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "RH_910 (Relative Humidity, percent)\\n" + //
                "QRH_5910 (Relative Humidity Quality)\\n" + //
                "SRH_6910 (Relative Humidity Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRh_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRh_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRh/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRh.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRh&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRh\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyQsen/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Sensible Heat Flux\", \"Summary\":\"This dataset has daily Sensible Heat Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QS_138 (Sensible Heat Flux, W m-2)\\n" + //
                "QQS_5138 (Sensible Heat Flux Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQsen_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQsen_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyQsen/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyQsen.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQsen&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyQsen\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySss.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySss\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySss.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySss/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity\", \"Summary\":\"This dataset has daily Sea Surface Salinity data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
                "QS_5041 (Salinity Quality)\\n" + //
                "SS_6041 (Salinity Source)\\n" + //
                "SIC_7041 (Salinity Instrument Code)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySss_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySss_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySss/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySss.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySss&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySss\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRf\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRf/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Heat Flux Due To Rain\", \"Summary\":\"This dataset has daily Heat Flux Due To Rain data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QR_139 (Heat Flux Due To Rain, W m-2)\\n" + //
                "QQR_5139 (Heat Flux Due To Rain Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRf_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRf_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRf/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRf.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRf&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRf\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRain\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRain/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation\", \"Summary\":\"This dataset has daily Precipitation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "RN_485 (Precipitation, MM/HR)\\n" + //
                "QRN_5485 (Precipitation Quality)\\n" + //
                "SRN_6485 (Precipitation Source)\\n" + //
                "RNS_486 (Precipitation Standard Deviation, MM/HR)\\n" + //
                "RNP_487 (Percent Time Raining, percent)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRain_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRain_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRain/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRain.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRain&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRain\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBf\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyBf/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Buoyancy Flux\", \"Summary\":\"This dataset has daily Buoyancy Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "BF_191 (Buoyancy Flux, 10**6 kg m-2 s-1)\\n" + //
                "QBF_5191 (Buoyancy Flux Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBf_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBf_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyBf/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyBf.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBf&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyBf\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLw\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyLw/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Longwave Radiation\", \"Summary\":\"This dataset has daily Incoming Longwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "Ql_136 (Incoming Longwave Radiation, W m-2)\\n" + //
                "QLW_5136 (Longwave Radiation Quality)\\n" + //
                "SLW_6136 (Longwave Radiation Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLw_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLw_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyLw/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyLw.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLw&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyLw\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyQnet/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Total Heat Flux\", \"Summary\":\"This dataset has daily Total Heat Flux data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "QT_210 (Total Heat Flux, W/M**2)\\n" + //
                "QQ0_5210 (Total Heat Flux Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQnet_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQnet_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyQnet/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyQnet.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQnet&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyQnet\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                "ST_6025 (Sea Surface Temperature Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySst\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "T_25 (Sea Surface Temperature, degree_C)\\n" + //
                "QT_5025 (Sea Surface Temperature Quality)\\n" + //
                "ST_6025 (Sea Surface Temperature Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rlPmelTaoDySst\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"Summary\":\"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "STH_71 (Sigma-Theta, kg m-3)\\n" + //
                "QST_5071 (Sigma-Theta Quality)\\n" + //
                "SST_6071 (Sigma-Theta Source)\\n" + //
                "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyD/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyD\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRad\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRad/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Downgoing Shortwave Radiation\", \"Summary\":\"This dataset has daily Downgoing Shortwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "RD_495 (Downgoing Shortwave Radiation, W/M**2)\\n" + //
                "QSW_5495 (Shortwave Radiation Quality)\\n" + //
                "SSW_6495 (Shortwave Radiation Source)\\n" + //
                "RDS_496 (Shortwave Radiation Standard Deviation, W/M**2)\\n" + //
                "RDP_497 (Shortwave Radiation Peak, W/M**2)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRad_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRad_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRad/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRad.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRad&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRad\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySwnet/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Net Shortwave Radiation\", \"Summary\":\"This dataset has daily Net Shortwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "SWN_1495 (Net Shortwave Radiation, W/M**2)\\n" + //
                "QSW_5495 (Shortwave Radiation Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySwnet_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySwnet_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySwnet/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySwnet.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySwnet&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySwnet\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyEmp/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Evaporation Minus Precipitation\", \"Summary\":\"This dataset has daily Evaporation Minus Precipitation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "EMP_251 (Evaporation Minus Precipitation, mm/hr)\\n" + //
                "QEMP_5251 (Evaporation Minus Precipitation Quality)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEmp_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEmp_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyEmp/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyEmp.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEmp&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyEmp\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBp\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyBp/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1998-present, Barometric (Air) Pressure\", \"Summary\":\"This dataset has daily Barometric (Air) Pressure data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "BP_915 (Barometric (Air) Pressure, hPa)\\n" + //
                "QBP_5915 (Barometric (Air) Pressure Quality)\\n" + //
                "SBP_6915 (Barometric (Air) Pressure Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBp_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBp_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyBp/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyBp.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBp&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyBp\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyLwnet/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation\", \"Summary\":\"This dataset has daily Net Longwave Radiation data from the\\n" + //
                "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
                "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
                "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
                "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n" + //
                "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "LWN_1136 (Net Longwave Radiation, W m-2)\\n" + //
                "QLW_5136 (Longwave Radiation Quality)\\n" + //
                "SLW_6136 (Longwave Radiation Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLwnet_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLwnet_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyLwnet/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyLwnet.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLwnet&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyLwnet\"}\n" + //
                "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/testTableWithDepth/\", \"Title\":\"This is EDDTableWithDepth\", \"Summary\":\"This is the summary\\n" + //
                "\\n" + //
                "cdm_data_type = TimeSeries\\n" + //
                "VARIABLES:\\n" + //
                "array\\n" + //
                "station\\n" + //
                "wmo_platform_code\\n" + //
                "longitude (Nominal Longitude, degrees_east)\\n" + //
                "latitude (Nominal Latitude, degrees_north)\\n" + //
                "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
                "depth (m)\\n" + //
                "AT_21 (Air Temperature, degree_C)\\n" + //
                "QAT_5021 (Air Temperature Quality)\\n" + //
                "SAT_6021 (Air Temperature Source)\\n" + //
                "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"testTableWithDepth\"}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.tsv?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    Test.ensureTrue(
        results.indexOf("\t\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\t") > 0,
        "results=\n" + results);

    // categorize
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">standard_name\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.json");
    Test.ensureEqual(results,
        "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
            "    \"columnTypes\": [\"String\", \"String\"],\n" +
            "    \"rows\": [\n" +
            "      [\"cdm_data_type\", \"http://localhost:" + PORT + "/erddap/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"institution\", \"http://localhost:" + PORT + "/erddap/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"ioos_category\", \"http://localhost:" + PORT + "/erddap/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"keywords\", \"http://localhost:" + PORT + "/erddap/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"long_name\", \"http://localhost:" + PORT + "/erddap/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"standard_name\", \"http://localhost:" + PORT + "/erddap/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"variableName\", \"http://localhost:" + PORT + "/erddap/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            +
            "    ]\n" +
            "  }\n" +
            "}\n",
        "results=\n" + results);

    // json with jsonp
    String jsonp = "myFunctionName";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.json?.jsonp=" + SSR.percentEncode(jsonp));
    Test.ensureEqual(results,
        jsonp + "(" +
            "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
            "    \"columnTypes\": [\"String\", \"String\"],\n" +
            "    \"rows\": [\n" +
            "      [\"cdm_data_type\", \"http://localhost:" + PORT + "/erddap/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"institution\", \"http://localhost:" + PORT + "/erddap/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"ioos_category\", \"http://localhost:" + PORT + "/erddap/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"keywords\", \"http://localhost:" + PORT + "/erddap/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"long_name\", \"http://localhost:" + PORT + "/erddap/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"standard_name\", \"http://localhost:" + PORT + "/erddap/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"variableName\", \"http://localhost:" + PORT + "/erddap/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            +
            "    ]\n" +
            "  }\n" +
            "}\n" +
            ")",
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">sea_water_temperature\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/index.json");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"sea_water_temperature\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/institution/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">ioos_category\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">noaa_coastwatch_west_coast_node\n") >= 0,
        "results=\n" + results);

    results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/institution/index.tsv"));
    Test.ensureTrue(results.indexOf("Category[9]URL[10]") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "noaa_coastwatch_west_coast_node[9]http://localhost:" + PORT + "/erddap/categorize/institution/noaa_coastwatch_west_coast_node/index.tsv?page=1&itemsPerPage=1000[10]") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/sea_water_temperature/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">erdGlobecBottle\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/sea_water_temperature/index.json");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", " +
        (EDStatic.sosActive ? "\"sos\", " : "") +
        (EDStatic.wcsActive ? "\"wcs\", " : "") +
        (EDStatic.wmsActive ? "\"wms\", " : "") +
        (EDStatic.filesActive ? "\"files\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"Accessible\", " : "") +
        "\"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", " +
        (EDStatic.subscriptionSystemActive ? "\"Email\", " : "") +
        "\"Institution\", \"Dataset ID\"],\n" +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", " +
        (EDStatic.sosActive ? "\"String\", " : "") +
        (EDStatic.wcsActive ? "\"String\", " : "") +
        (EDStatic.wmsActive ? "\"String\", " : "") +
        (EDStatic.filesActive ? "\"String\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"String\", " : "") +
        "\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", " +
        (EDStatic.subscriptionSystemActive ? "\"String\", " : "") +
        "\"String\", \"String\"],\n" +
        "    \"rows\": [\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected = "http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle.subset\", " +
        "\"http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle\", " +
        "\"http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle.graph\", " +
        (EDStatic.sosActive ? "\"\", " : "") + // currently, it isn't made available via sos
        (EDStatic.wcsActive ? "\"\", " : "") +
        (EDStatic.wmsActive ? "\"\", " : "") +
        (EDStatic.filesActive ? "\"http://localhost:" + PORT + "/erddap/files/erdGlobecBottle/\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"public\", " : "") +
        "\"GLOBEC NEP Rosette Bottle Data (2002)\", \"GLOBEC (GLOBal " +
        "Ocean ECosystems Dynamics) NEP (Northeast Pacific)\\nRosette Bottle Data from " +
        "New Horizon Cruise (NH0207: 1-19 August 2002).\\nNotes:\\nPhysical data " +
        "processed by Jane Fleischbein (OSU).\\nChlorophyll readings done by " +
        "Leah Feinberg (OSU).\\nNutrient analysis done by Burke Hales (OSU).\\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\\n" +
        "secondary sensor pair was used in final processing of CTD data for\\n" +
        "most stations because the primary had more noise and spikes. The\\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\\n" +
        "multiple spikes or offsets in the secondary pair.\\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\\n" +
        "data developed by Burke Hales (OSU).\\n" +
        "Operation Detection Limits for Nutrient Concentrations\\n" +
        "Nutrient  Range         Mean    Variable         Units\\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\\n" +
        "Dates and Times are UTC.\\n\\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\\n\\n" +
        // was "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\\n\\n"
        // +
        "Inquiries about how to access this data should be directed to\\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\\n\\n" +
        "cdm_data_type = TrajectoryProfile\\n" +
        "VARIABLES:\\ncruise_id\\n... (24 more variables)\\n\", " +
        "\"http://localhost:" + PORT + "/erddap/metadata/fgdc/xml/erdGlobecBottle_fgdc.xml\", " +
        "\"http://localhost:" + PORT + "/erddap/metadata/iso19115/xml/erdGlobecBottle_iso19115.xml\", " +
        "\"http://localhost:" + PORT + "/erddap/info/erdGlobecBottle/index.json\", " +
        "\"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\", " + // was "\"http://www.globec.org/\", " +
        "\"http://localhost:" + PORT + "/erddap/rss/erdGlobecBottle.rss\", " +
        (EDStatic.subscriptionSystemActive
            ? "\"http://localhost:" + PORT + "/erddap/subscriptions/add.html?datasetID=erdGlobecBottle&showErrors=false&email=\", "
            : "")
        +
        "\"GLOBEC\", \"erdGlobecBottle\"],";
    po = results.indexOf("http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle");
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // griddap
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of griddap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">Daily MUR SST, Interim near-real-time (nrt) product") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">jplMURSST41\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.json?" +
        EDStatic.defaultPIppQuery + "");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Daily MUR SST, Interim near-real-time (nrt) product") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"jplMURSST41\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/jplMURSST41.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Sea Ice Area Fraction") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/jplMURSST41.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Sea Ice Area Fraction") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // tabledap
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of tabledap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdGlobecBottle\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.json?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"GLOBEC NEP Rosette Bottle Data (2002)\"") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"erdGlobecBottle\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Filled Square") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // files
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/");
    Test.ensureTrue(
        results.indexOf(
            "ERDDAP's \"files\" system lets you browse a virtual file system and download source data files.") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("cwwcNDBCMet") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directories") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/cwwcNDBCMet/nrt/");
    Test.ensureTrue(results.indexOf("NDBC Standard Meteorological Buoy Data") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make a graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NDBC&#x5f;41008&#x5f;met&#x2e;nc") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    String localName = EDStatic.fullTestCacheDirectory + "NDBC_41008_met.nc";
    File2.delete(localName);
    SSR.downloadFile( // throws Exception if trouble
        EDStatic.erddapUrl + "/files/cwwcNDBCMet/nrt/NDBC_41008_met.nc",
        localName, true); // tryToUseCompression
    Test.ensureTrue(File2.isFile(localName),
        "/files download failed. Not found: localName=" + localName);
    File2.delete(localName);

    // sos
    if (EDStatic.sosActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of SOS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">NDBC Standard Meteorological Buoy Data") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">cwwcNDBCMet") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"NDBC Standard Meteorological Buoy Data\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"cwwcNDBCMet\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "available via ERDDAP's Sensor Observation Service (SOS) web service.") >= 0,
          "results=\n" + results);

      String sosUrl = EDStatic.erddapUrl + "/sos/cwwcNDBCMet/" + EDDTable.sosServer;
      results = SSR.getUrlResponseStringUnchanged(sosUrl + "?service=SOS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<ows:ServiceIdentification>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("<ows:Get xlink:href=\"" + sosUrl + "?\"/>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("</Capabilities>") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/sos/index.html?page=1&itemsPerPage=1000\n"
          +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: The \\\"SOS\\\" system has been disabled on this ERDDAP.\";\n" +
          "})\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected,
          "results=\n" + results);
    }

    // wcs
    if (EDStatic.wcsActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Datasets Which Can Be Accessed via WCS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(">Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)</td>") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMHchla8day<") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("\"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.") >= 0,
          "results=\n" + results);

      String wcsUrl = EDStatic.erddapUrl + "/wcs/erdMHchla8day/" + EDDGrid.wcsServer;
      results = SSR.getUrlResponseStringUnchanged(wcsUrl + "?service=WCS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<CoverageOfferingBrief>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("<lonLatEnvelope srsName") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("</WCS_Capabilities>") >= 0, "results=\n" + results);
    } else {
      // wcs is inactive
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf(
          "java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/wcs/index.html?page=1&itemsPerPage=1000") >= 0,
          "results=\n" + results);
    }

    // wms
    if (EDStatic.wmsActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of WMS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results
              .indexOf(">Chlorophyll-a, Aqua MODIS, NPP, 0.0125&deg;, West US, EXPERIMENTAL, 2002-present (1 Day Composite)\n") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMH1chla1day\n") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
              "\"Chlorophyll-a, Aqua MODIS, NPP, 0.0125\\u00b0, West US, EXPERIMENTAL, 2002-present (1 Day Composite)\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMH1chla1day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("display of registered and superimposed map-like views") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/erdMH1chla1day/index.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("ERDDAP - Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite) - WMS") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("on-the-fly by ERDDAP's") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("longitude") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);
    }

    // results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
    // "/categorize/standard_name/index.html");
    // Test.ensureTrue(results.indexOf(">sea_water_temperature<") >= 0,
    // "results=\n" + results);

    // validate the various GetCapabilities documents
    /*
     * NOT ACTIVE
     * String s = https://xmlvalidation.com/ ".../xml/validate/?lang=en" +
     * "&url=" + EDStatic.erddapUrl + "/wms/" + EDD.WMS_SERVER + "?service=WMS&" +
     * "request=GetCapabilities&version=";
     * Test.displayInBrowser(s + "1.1.0");
     * Test.displayInBrowser(s + "1.1.1");
     * Test.displayInBrowser(s + "1.3.0");
     */

    // more information
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/information.html");
    Test.ensureTrue(results.indexOf(
        "ERDDAP a solution to everyone's data distribution / data access problems?") >= 0,
        "results=\n" + results);

    // subscriptions
    if (EDStatic.subscriptionSystemActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/index.html");
      Test.ensureTrue(results.indexOf("Add a new subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Validate a subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List your subscriptions") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Remove a subscription") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/add.html");
      Test.ensureTrue(results.indexOf(
          "To add a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/validate.html");
      Test.ensureTrue(results.indexOf(
          "To validate a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/list.html");
      Test.ensureTrue(results.indexOf(
          "To request an email with a list of your subscriptions, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/remove.html");
      Test.ensureTrue(results.indexOf(
          "To remove a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
            "/subscriptions/index.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);
    }

    // slideSorter
    if (EDStatic.slideSorterActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/slidesorter.html");
      Test.ensureTrue(results.indexOf(
          "Your slides will be lost when you close this browser window, unless you:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
            "/slidesorter.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);
    }

    // embed a graph (always at coastwatch)
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/images/embed.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Embed a Graph in a Web Page") >= 0,
        "results=\n" + results);

    // Computer Programs
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/rest.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "ERDDAP's RESTful Web Services") >= 0,
        "results=\n" + results);

    // list of services
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.csv");
    expected = "Resource,URL\n" +
        "info,http://localhost:" + PORT + "/erddap/info/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "search,http://localhost:" + PORT + "/erddap/search/index.csv?" + EDStatic.defaultPIppQuery + "&searchFor=\n" +
        "categorize,http://localhost:" + PORT + "/erddap/categorize/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "griddap,http://localhost:" + PORT + "/erddap/griddap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "tabledap,http://localhost:" + PORT + "/erddap/tabledap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        (EDStatic.sosActive
            ? "sos,http://localhost:" + PORT + "/erddap/sos/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "wcs,http://localhost:" + PORT + "/erddap/wcs/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "wms,http://localhost:" + PORT + "/erddap/wms/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "");
    // subscriptions?
    // converters?
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.htmlTable?" +
        EDStatic.defaultPIppQuery);
    expected = EDStatic.startHeadHtml(0, EDStatic.erddapUrl((String) null, language), "Resources") + "\n" +
        "</head>\n" +
        EDStatic.startBodyHtml(0, null, "index.html", EDStatic.defaultPIppQuery) + // 2022-11-22 .htmlTable converted to
                                                                                   // .html to avoid user requesting all
                                                                                   // data in a dataset if they change
                                                                                   // language
        "&nbsp;<br>\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>Resource\n" +
        "<th>URL\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>info\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;info&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:" + PORT + "/erddap/info/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>search\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;search&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000&#x26;searchFor&#x3d;\">http://localhost:" + PORT + "/erddap/search/index.htmlTable?page=1&amp;itemsPerPage=1000&amp;searchFor=</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>categorize\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;categorize&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:" + PORT + "/erddap/categorize/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>griddap\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;griddap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:" + PORT + "/erddap/griddap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>tabledap\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;tabledap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:" + PORT + "/erddap/tabledap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        (EDStatic.sosActive ? "<tr>\n" +
            "<td>sos\n" +
            "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;sos&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:" + PORT + "/erddap/sos/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            +
            "</tr>\n" : "")
        +
        "<tr>\n" +
        "<td>wms\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;wms&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:" + PORT + "/erddap/wms/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "</table>\n" +
        EDStatic.endBodyHtml(0, EDStatic.erddapUrl((String) null, language), (String) null) + "\n" +
        "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.json");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"Resource\", \"URL\"],\n" +
        "    \"columnTypes\": [\"String\", \"String\"],\n" +
        "    \"rows\": [\n" +
        "      [\"info\", \"http://localhost:" + PORT + "/erddap/info/index.json?page=1&itemsPerPage=1000\"],\n" +
        "      [\"search\", \"http://localhost:" + PORT + "/erddap/search/index.json?page=1&itemsPerPage=1000&searchFor=\"],\n"
        +
        "      [\"categorize\", \"http://localhost:" + PORT + "/erddap/categorize/index.json?page=1&itemsPerPage=1000\"],\n"
        +
        "      [\"griddap\", \"http://localhost:" + PORT + "/erddap/griddap/index.json?page=1&itemsPerPage=1000\"],\n" +
        "      [\"tabledap\", \"http://localhost:" + PORT + "/erddap/tabledap/index.json?page=1&itemsPerPage=1000\"]"
        + (EDStatic.sosActive || EDStatic.wcsActive || EDStatic.wmsActive ? "," : "") + "\n" +
        (EDStatic.sosActive
            ? "      [\"sos\", \"http://localhost:" + PORT + "/erddap/sos/index.json?page=1&itemsPerPage=1000\"]"
                + (EDStatic.wcsActive || EDStatic.wmsActive ? "," : "") + "\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "      [\"wcs\", \"http://localhost:" + PORT + "/erddap/wcs/index.json?page=1&itemsPerPage=1000\"]"
                + (EDStatic.wmsActive ? "," : "") + "\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "      [\"wms\", \"http://localhost:" + PORT + "/erddap/wms/index.json?page=1&itemsPerPage=1000\"]\n"
            : "")
        +
        // subscriptions?
        "    ]\n" +
        "  }\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.tsv"));
    expected = "Resource[9]URL[10]\n" +
        "info[9]http://localhost:" + PORT + "/erddap/info/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "search[9]http://localhost:" + PORT + "/erddap/search/index.tsv?page=1&itemsPerPage=1000&searchFor=[10]\n" +
        "categorize[9]http://localhost:" + PORT + "/erddap/categorize/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "griddap[9]http://localhost:" + PORT + "/erddap/griddap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "tabledap[9]http://localhost:" + PORT + "/erddap/tabledap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        (EDStatic.sosActive ? "sos[9]http://localhost:" + PORT + "/erddap/sos/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        (EDStatic.wcsActive ? "wcs[9]http://localhost:" + PORT + "/erddap/wcs/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        (EDStatic.wmsActive ? "wms[9]http://localhost:" + PORT + "/erddap/wms/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        "[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.xhtml");
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
        "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
        "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
        "  <title>Resources</title>\n" +
        "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:" + PORT + "/erddap/images/erddap2.css\" />\n"
        +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>Resource</th>\n" +
        "<th>URL</th>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>info</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/info/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>search</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/search/index.xhtml?page=1&amp;itemsPerPage=1000&amp;searchFor=</td>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>categorize</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/categorize/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>griddap</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/griddap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>tabledap</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/tabledap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        (EDStatic.sosActive ? "<tr>\n" +
            "<td>sos</td>\n" +
            "<td>http://localhost:" + PORT + "/erddap/sos/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        (EDStatic.wcsActive ? "<tr>\n" +
            "<td>wcs</td>\n" +
            "<td>http://localhost:" + PORT + "/erddap/wcs/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        (EDStatic.wmsActive ? "<tr>\n" +
            "<td>wms</td>\n" +
            "<td>http://localhost:" + PORT + "/erddap/wms/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        "</table>\n" +
        "</body>\n" +
        "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

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
        "time_coverage_end=yyyy-mm-ddT09:00:00Z\\n" +
        "time_coverage_start=yyyy-mm-ddT09:00:00Z\\n" +
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
        "      \"maxValue\": \"yyyy-mm-ddT09:00:00Z\",\n" + // changes
        "      \"minValue\": \"yyyy-mm-ddT09:00:00Z\",\n" +
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
        "          \"value\": \"ice fraction is a dimensionless quantity between 0 and 1; it has been interpolated by a nearest neighbor approach; EUMETSAT OSI-SAF files used: ice_conc_nh_polstere-100_multi_yyyymmdd1200.nc, ice_conc_sh_polstere-100_multi_yyyymmdd1200.nc.\"\n" +
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
        "  \"dateCreated\": \"yyyy-mm-ddThh:mm:ssZ\",\n" + // changes
        "  \"identifier\": \"org.ghrsst/MUR-JPL-L4-GLOB-v04.1\",\n" +
        "  \"version\": \"04.1\",\n" +
        "  \"temporalCoverage\": \"yyyy-mm-ddT09:00:00Z/yyyy-mm-ddT09:00:00Z\",\n" + // end date changes
        "  \"spatialCoverage\": {\n" +
        "    \"@type\": \"Place\",\n" +
        "    \"geo\": {\n" +
        "      \"@type\": \"GeoShape\",\n" +
        "      \"box\": \"-89.99 -179.99 89.99 180\"\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "</script>\n";

    results = results.replaceAll(
        "time_coverage_end=....-..-..T09:00:00Z",
        "time_coverage_end=yyyy-mm-ddT09:00:00Z");
    results = results.replaceAll("....-..-..T09:00:00Z", "yyyy-mm-ddT09:00:00Z");
        results = results.replaceAll("dateCreated\\\": \\\"....-..-..T..:..:..Z", "dateCreated\\\": \\\"yyyy-mm-ddThh:mm:ssZ");
    results = results.replaceAll("100_multi_........1200", "100_multi_yyyymmdd1200");
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

  /** EDDGridFromDap */

  /**
   * This tests a depth axis variable. This requires
   * hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagImageComparison
  void testGridWithDepth2_LonPM180() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testGridWithDepth2_LonPM180");
    String results, expected, tName;
    int po;
    int language = 0;

    // test generateDatasetsXml -- It should catch z variable and convert to
    // altitude.
    // !!! I don't have a test dataset with real altitude data that isn't already
    // called altitude!
    /*
     * String url =
     * "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/19921014.bodas_ts.nc";
     * results = generateDatasetsXml(true, url,
     * null, null, null, 10080, null);
     * po = results.indexOf("<sourceName>z</sourceName>");
     * Test.ensureTrue(po >= 0, "results=\n" + results);
     * expected =
     * "<sourceName>z</sourceName>\n" +
     * "        <destinationName>depth</destinationName>\n" +
     * "        <!-- sourceAttributes>\n" +
     * "            <att name=\"cartesian_axis\">Z</att>\n" +
     * "            <att name=\"long_name\">Depth</att>\n" +
     * "            <att name=\"positive\">down</att>\n" +
     * "            <att name=\"units\">m</att>\n" +
     * "        </sourceAttributes -->\n" +
     * "        <addAttributes>\n" +
     * "            <att name=\"ioos_category\">Location</att>\n" +
     * "            <att name=\"standard_name\">depth</att>\n" +
     * "        </addAttributes>\n" +
     * "    </axisVariable>";
     * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
     * "results=\n" + results);
     */

    // Test that constructor of EDVDepthGridAxis added proper metadata for depth
    // variable.
    // EDDGrid gridDataset = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null,
    // "hawaii_d90f_20ee_c4cb_LonPM180");
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.gethawaii_d90f_20ee_c4cb_LonPM180();

    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
        EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth2", ".das");
    results = File2.directReadFrom88591File(
        EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("depth {");
    Test.ensureTrue(po >= 0, "results=\n" + results);
    expected = "depth {\n" +
        "    String _CoordinateAxisType \"Height\";\n" +
        "    String _CoordinateZisPositive \"down\";\n" +
        "    Float64 actual_range 5.01, 5375.0;\n" + // 2014-01-17 was 5.0, 5374.0
        "    String axis \"Z\";\n" +
        "    String ioos_category \"Location\";\n" +
        "    String long_name \"Depth\";\n" +
        "    String positive \"down\";\n" +
        "    String standard_name \"depth\";\n" +
        "    String units \"m\";\n" +
        "  }";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // FGDC should deal with depth correctly
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
        EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth2", ".fgdc");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("<vertdef>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<vertdef>\n" +
        "      <depthsys>\n" +
        "        <depthdn>Unknown</depthdn>\n" +
        "        <depthres>Unknown</depthres>\n" +
        "        <depthdu>meters</depthdu>\n" +
        "        <depthem>Explicit depth coordinate included with horizontal coordinates</depthem>\n"
        +
        "      </depthsys>\n" +
        "    </vertdef>\n" +
        "  </spref>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // ISO 19115 should deal with depth correctly
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
        EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth2", ".iso19115");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);

    po = results.indexOf(
        "codeListValue=\"vertical\">");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n" +
        "          </gmd:dimensionName>\n" +
        "          <gmd:dimensionSize>\n" +
        "            <gco:Integer>40</gco:Integer>\n" +
        "          </gmd:dimensionSize>\n" +
        "          <gmd:resolution>\n" +
        "            <gco:Measure uom=\"m\">137.69205128205127</gco:Measure>\n" + // 2014-01-17
                                                                                  // was
                                                                                  // 137.66666666666666
        "          </gmd:resolution>\n" +
        "        </gmd:MD_Dimension>\n" +
        "      </gmd:axisDimensionProperties>\n";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    po = results.indexOf(
        "<gmd:EX_VerticalExtent>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<gmd:EX_VerticalExtent>\n" +
        "              <gmd:minimumValue><gco:Real>-5375.0</gco:Real></gmd:minimumValue>\n" +
        "              <gmd:maximumValue><gco:Real>-5.01</gco:Real></gmd:maximumValue>\n" +
        "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
        "            </gmd:EX_VerticalExtent>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // test WMS 1.1.0 service getCapabilities from localhost erddap
    String2.log("\nTest WMS 1.1.0 getCapabilities\n" +
        "!!! This test requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP!!!");
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "service=WMS&request=GetCapabilities&version=1.1.0");
    po = results.indexOf("</Layer>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "</Layer>\n" +
        "      <Layer>\n" +
        "        <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180</Title>\n"
        +
        "        <SRS>EPSG:4326</SRS>\n" +
        "        <LatLonBoundingBox minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" />\n"
        +
        "        <BoundingBox SRS=\"EPSG:4326\" minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" resx=\"0.5\" resy=\"0.5\" />\n"
        +
        "        <Dimension name=\"time\" units=\"ISO8601\" />\n" +
        "        <Dimension name=\"elevation\" units=\"EPSG:5030\" />\n" +
        // 2014-01-24 default was 2008-12-15
        "        <Extent name=\"time\" default=\"2010-12-15T00:00:00Z\" >1871-01-15T00:00:00Z,1871-02-15T00:00:00Z,1871-03-15T00:00:00Z,";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    po = results.indexOf("<Extent name=\"elevation\"");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<Extent name=\"elevation\" default=\"-5375.0\" >-5.01,-15.07,-25.28,-35.76,-46.61,-57.98,-70.02,-82.92,-96.92,-112.32,-129.49,-148.96,-171.4,-197.79,-229.48,-268.46,-317.65,-381.39,-465.91,-579.31,-729.35,-918.37,-1139.15,-1378.57,-1625.7,-1875.11,-2125.01,-2375.0,-2625.0,-2875.0,-3125.0,-3375.0,-3625.0,-3875.0,-4125.0,-4375.0,-4625.0,-4875.0,-5125.0,-5375.0</Extent>\n"
        +
        "        <Attribution>\n" +
        "          <Title>TAMU/UMD</Title>\n" +
        "          <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n" +
        "            xlink:type=\"simple\"\n" +
        "            xlink:href=\"https://www.atmos.umd.edu/~ocean/\" />\n" +
        "        </Attribution>\n" +
        "        <Layer opaque=\"1\" >\n" +
        "          <Name>hawaii_d90f_20ee_c4cb_LonPM180:temp</Name>\n" +
        "          <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180 - temp</Title>\n"
        +
        "        </Layer>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // WMS 1.1.0 elevation=-5
    String baseName = "EDDGridLonPM180_TestGridWithDepth2110e5";
    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // WMS 1.1.0 default elevation
    baseName = "EDDGridLonPM180_TestGridWithDepth2110edef";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080" +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // test WMS 1.3.0 service getCapabilities from localhost erddap
    String2.log("\nTest WMS 1.3.0 getCapabilities\n" +
        "!!! This test requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP!!!");
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "service=WMS&request=GetCapabilities&version=1.3.0");

    po = results.indexOf("</Layer>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "</Layer>\n" +
        "      <Layer>\n" +
        "        <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180</Title>\n"
        +
        "        <CRS>CRS:84</CRS>\n" +
        "        <CRS>EPSG:4326</CRS>\n" +
        "        <EX_GeographicBoundingBox>\n" +
        "          <westBoundLongitude>-179.75</westBoundLongitude>\n" +
        "          <eastBoundLongitude>179.75</eastBoundLongitude>\n" +
        "          <southBoundLatitude>-75.25</southBoundLatitude>\n" +
        "          <northBoundLatitude>89.25</northBoundLatitude>\n" +
        "        </EX_GeographicBoundingBox>\n" +
        "        <BoundingBox CRS=\"EPSG:4326\" minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" resx=\"0.5\" resy=\"0.5\" />\n"
        +
        "        <Dimension name=\"time\" units=\"ISO8601\" multipleValues=\"0\" nearestValue=\"1\" default=\"2010-12-15T00:00:00Z\" >1871-01-15T00:00:00Z,1871-02-15T00:00:00Z,";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    po = results.indexOf("<Dimension name=\"elevation\"");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<Dimension name=\"elevation\" units=\"CRS:88\" unitSymbol=\"m\" multipleValues=\"0\" nearestValue=\"1\" default=\"-5375.0\" >-5.01,-15.07,-25.28,-35.76,-46.61,-57.98,-70.02,-82.92,-96.92,-112.32,-129.49,-148.96,-171.4,-197.79,-229.48,-268.46,-317.65,-381.39,-465.91,-579.31,-729.35,-918.37,-1139.15,-1378.57,-1625.7,-1875.11,-2125.01,-2375.0,-2625.0,-2875.0,-3125.0,-3375.0,-3625.0,-3875.0,-4125.0,-4375.0,-4625.0,-4875.0,-5125.0,-5375.0</Dimension>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // WMS 1.3.0 elevation=-5
    // 2022-07-07 trouble with wms png request, so test underlying data request
    // first
    tName = gridDataset.makeNewFileForDapQuery(language, null, null,
        "temp%5B(2008-11-15)%5D%5B(5)%5D%5B(-75):100:(75)%5D%5B(-90):100:(63.6)%5D",
        EDStatic.fullTestCacheDirectory,
        "EDDGridLonPM180_testGridWithDepthPreWMS", ".csv");
    results = File2.directReadFrom88591File(
        EDStatic.fullTestCacheDirectory + tName);
    expected = "time,depth,latitude,longitude,temp\n" +
        "UTC,m,degrees_north,degrees_east,degree_C\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,-89.75,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,-39.75,-1.9969722\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,10.25,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,60.25,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,-89.75,19.052225\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,-39.75,22.358824\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,10.25,17.43544\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,60.25,23.83485\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,-89.75,26.235065\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,-39.75,25.840372\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,10.25,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,60.25,27.425127\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,-89.75,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,-39.75,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,10.25,4.0587144\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,60.25,-0.4989917\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // (see section above) now request troubling wms png
    baseName = "EDDGridLonPM180_TestGridWithDepth2130e5";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-75,-90,75,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // WMS 1.1.0 default elevation
    baseName = "EDDGridLonPM180_TestGridWithDepth2130edef";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080" +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-75,-90,75,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // test lat beyond dataset range (changed from -75:75 above to -80:80 here)
    baseName = "EDDGridLonPM180_BeyondRange";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");
  }

  /** EDDGridFromEtopo */

  /**
   * This tests the /files/ "files" system.
   * This requires etopo180 or etopo360 in the localhost ERDDAP.
   *
   * @param tDatasetID etopo180 or etopo360
   */
  @ParameterizedTest
  @ValueSource(strings = { "etopo180", "etopo360" })
  @TagJetty
  void testEtopoGridFiles(String tDatasetID) throws Throwable {

    String2.log("\n*** EDDGridFromEtopo.testFiles(" + tDatasetID + ")\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/.csv");
    expected = "Name,Last modified,Size,Description\n" +
        "etopo1_ice_g_i2.bin,1642733858000,466624802,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/");
    Test.ensureTrue(results.indexOf("etopo1&#x5f;ice&#x5f;g&#x5f;i2&#x2e;bin") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">466624802<") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv

    // download a file in root
    String2.log("This test takes ~30 seconds.");
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/etopo1_ice_g_i2.bin");
    results = String2.annotatedString(results.substring(0, 50));
    expected = "|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239][end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // download a file in subdir

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/"
        + tDatasetID + "/gibberish/\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/"
        + tDatasetID + "/gibberish.csv\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
  }

  /** ErddapTests */

  /**
   * This is used by Bob to do simple tests of Search.
   * This requires a running local ERDDAP with erdMHchla8day and rMHchla8day
   * (among others which will be not matched).
   * This can be used with searchEngine=original or lucene.
   *
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testSearch() throws Throwable {
    // Erddap.verbose = true;
    // Erddap.reallyVerbose = true;
    // EDD.testVerboseOn();
    String htmlUrl = EDStatic.erddapUrl + "/search/index.html?page=1&itemsPerPage=1000";
    String csvUrl = EDStatic.erddapUrl + "/search/index.csv?page=1&itemsPerPage=1000";
    String expected = "pmelTaoDySst";
    String expected2, query, results;
    int count;
    String2.log("\n*** Erddap.testSearch\n" +
        "This assumes localhost ERDDAP is running with erdMHchla8day and rMHchla8day (among others which will be not matched).");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
        // "&searchFor=erdMHchla8day",
        "&searchFor=" + SSR.minimalPercentEncode("tao daily sea surface temperature -air"),
        "&searchFor=pmelTaoDySst", // lucene fail?
        "&searchFor=DySst", // lucene fail?
        // "&searchFor=" + SSR.minimalPercentEncode(
        //     "\"sourceUrl=https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\n\"")
    };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      count = String2.countAll(results, "NOAA PMEL");
      Test.ensureEqual(count, 4, "results=\n" + results + "i=" + i); // one in help, plus one per dataset

      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      count = String2.countAll(results, "NOAA PMEL");
      Test.ensureEqual(count, 2, "results=\n" + results + "i=" + i); // one per dataset
    }

    // query with no matches: valid for .html but error for .csv: protocol
    query = "&searchFor=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    expected = "Your query produced no matching results.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: Your query produced no matching results. Check the spelling of the word(s) you searched for.\";\n"
        +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

  }

  /** EDDGridCopy */
  /**
   * This tests the /files/ "files" system.
   * This requires testGridCopy in the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGridFiles() throws Throwable {
    // String2.log("\n*** EDDGridCopy.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/.csv");
    expected = "Name,lastModTime,Size,Description\n" +
        "subfolder/,NaN,NaN,\n" +
        "erdQSwind1day_20080101_03.nc.gz,lastModTime,10478645,\n" +
        "erdQSwind1day_20080104_07.nc,lastModTime,49790172,\n";
    results = results.replaceAll(",.............,", ",lastModTime,");
    if (results.length() < expected.length()) {
        throw new Exception(results);
    }
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/");
    Test.ensureTrue(results.indexOf("erdQSwind1day&#x5f;20080104&#x5f;07&#x2e;nc") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">49790172<") > 0, "results=\n" + results);

    // download a file in root
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/erdQSwind1day_20080104_07.nc").substring(0, 50));
    expected = "CDF[1][0][0][0][0][0][0][0][10]\n" +
        "[0][0][0][4][0][0][0][4]time[0][0][0][4][0][0][0][8]altitude[0][0][0][1][0][0][0][8]la[end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** EDDGridLon0360 */

  /**
   * This tests a dataset that is initially all LT0.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagImageComparison
  void testLT0() throws Throwable {
    // String2.log("\n****************** EDDGridLon0360.testLT0()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettest_erdVHNchlamday_Lon0360();

    tName = eddGrid.makeNewFileForDapQuery(language, null, null, "", dir,
        eddGrid.className() + "_LT0_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("time = \\d{2,3}", "time = ddd");
    expected = "Dataset {\n" +
        "  Float64 time[time = ddd];\n" +
        "  Float64 altitude[altitude = 1];\n" +
        "  Float64 latitude[latitude = 11985];\n" +
        "  Float64 longitude[longitude = 9333];\n" +
        "  GRID {\n" +
        "    ARRAY:\n" +
        "      Float32 chla[time = ddd][altitude = 1][latitude = 11985][longitude = 9333];\n" +
        "    MAPS:\n" +
        "      Float64 time[time = ddd];\n" +
        "      Float64 altitude[altitude = 1];\n" +
        "      Float64 latitude[latitude = 11985];\n" +
        "      Float64 longitude[longitude = 9333];\n" +
        "  } chla;\n" +
        "} test_erdVHNchlamday_Lon0360;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName = eddGrid.makeNewFileForDapQuery(language, null, null, "", dir,
        eddGrid.className() + "_LT0_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "longitude {\n" +
        "    String _CoordinateAxisType \"Lon\";\n" +
        "    Float64 actual_range 180.00375, 249.99375;\n" +
        "    String axis \"X\";\n" +
        "    String coordsys \"geographic\";\n" +
        "    String ioos_category \"Location\";\n" +
        "    String long_name \"Longitude\";\n" +
        "    String point_spacing \"even\";\n" +
        "    String standard_name \"longitude\";\n" +
        "    String units \"degrees_east\";\n" +
        "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    expected = "    Float64 geospatial_lon_max 249.99375;\n" +
        "    Float64 geospatial_lon_min 180.00375;\n" +
        "    Float64 geospatial_lon_resolution 0.007500000000000001;\n" +
        "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    expected = "    Float64 Westernmost_Easting 180.00375;\n" +
        "  }\n" +
        "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // lon values
    userDapQuery = "longitude[0:1000:9332]";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        eddGrid.className() + "_LT0_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 180.00375, 187.50375, 195.00375, 202.50375, 210.00375, 217.50375, 225.00375, 232.50375, 240.00375, 247.50375, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire lon range
    userDapQuery = "chla[(2015-03-16)][][(89.77152):5500:(-0.10875)][(180.00375):4500:(249.99375)]"; // [-179.99625:4500:-110.00625]
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        eddGrid.className() + "_LT0_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // from same request from cw erddap: erdVHNchlamday
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(89.77125):5500:(-0.10875)%5D%5B(-179.99625):4500:(-110.00625)%5D
        // but add 360 to the lon values to get the values below
        "time,altitude,latitude,longitude,chla\n" +
            "UTC,m,degrees_north,degrees_east,mg m^-3\n" +
            "2015-03-16T00:00:00Z,0.0,89.77125,180.00375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,89.77125,213.75375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,89.77125,247.50375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,48.52125,180.00375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,48.52125,213.75375,0.29340988\n" +
            "2015-03-16T00:00:00Z,0.0,48.52125,247.50375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,7.27125,180.00375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,7.27125,213.75375,0.08209898\n" +
            "2015-03-16T00:00:00Z,0.0,7.27125,247.50375,0.12582141\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon subset
    userDapQuery = "chla[(2015-03-16)][][(10):500:(0)][(200):2000:(240)]";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        eddGrid.className() + "_LT0_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(10):500:(0)%5D%5B(-160):2000:(-120)%5D
        // but add 360 to get lon values below
        "time,altitude,latitude,longitude,chla\n" +
            "UTC,m,degrees_north,degrees_east,mg m^-3\n" +
            "2015-03-16T00:00:00Z,0.0,10.00125,199.99875,0.03348998\n" +
            "2015-03-16T00:00:00Z,0.0,10.00125,214.99875,0.06315609\n" +
            "2015-03-16T00:00:00Z,0.0,10.00125,229.99875,0.06712352\n" +
            "2015-03-16T00:00:00Z,0.0,6.25125,199.99875,0.089674756\n" +
            "2015-03-16T00:00:00Z,0.0,6.25125,214.99875,0.12793668\n" +
            "2015-03-16T00:00:00Z,0.0,6.25125,229.99875,0.15159287\n" +
            "2015-03-16T00:00:00Z,0.0,2.50125,199.99875,0.16047283\n" +
            "2015-03-16T00:00:00Z,0.0,2.50125,214.99875,0.15874723\n" +
            "2015-03-16T00:00:00Z,0.0,2.50125,229.99875,0.12635218\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "chla[(2015-03-16)][][][]&.land=under";
    String baseName = eddGrid.className() + "_NEPacificNowLon0360";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery,
        Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR), baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // test of /files/ system for fromErddap in local host dataset
    String2.log("\n*** The following test requires test_erdVHNchlamday_Lon0360 in the localhost ERDDAP.");
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/files/test_erdVHNchlamday_Lon0360/");
    expected = "VHN2015060&#x5f;2015090&#x5f;chla&#x2e;nc";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // String2.log("\n*** EDDGridLon0360.testLT0() finished.");
  }

  /** EDDTableCopy */

  /**
   * The basic tests of this class (erdGlobecBottle).
   * 
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagImageComparison
  void testTableCopyBasic() throws Throwable {
    // testVerboseOn();
    int language = 0;

    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String tDir = EDStatic.fullTestCacheDirectory;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&time>=2002-08-03";
    userDapQuery = "longitude,NO3,time,ship&latitude%3E0&time%3E=2002-08-03";

    EDDTable edd = (EDDTableCopy) EDDTestDataset.gettestTableCopy();

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDTableCopy.test das dds for entire dataset\n");
    tName = edd.makeNewFileForDapQuery(language, null, null, "",
        tDir, edd.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = // see OpendapHelper.EOL for comments
        "Attributes {\n" +
            " s {\n" +
            "  cruise_id {\n" +
            "    String cf_role \"trajectory_id\";\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Cruise ID\";\n" +
            "  }\n" +
            "  ship {\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Ship\";\n" +
            "  }\n" +
            "  cast {\n" +
            "    Int16 _FillValue 32767;\n" +
            "    Int16 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 140.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Cast Number\";\n" +
            "    Int16 missing_value 32767;\n" +
            "  }\n" +
            "  longitude {\n" +
            "    String _CoordinateAxisType \"Lon\";\n" +
            "    Float32 _FillValue 327.67;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    String axis \"X\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Longitude\";\n" +
            "    Float32 missing_value 327.67;\n" +
            "    String standard_name \"longitude\";\n" +
            "    String units \"degrees_east\";\n" +
            "  }\n" +
            "  latitude {\n" +
            "    String _CoordinateAxisType \"Lat\";\n" +
            "    Float32 _FillValue 327.67;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    String axis \"Y\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Latitude\";\n" +
            "    Float32 missing_value 327.67;\n" +
            "    String standard_name \"latitude\";\n" +
            "    String units \"degrees_north\";\n" +
            "  }\n" +
            "  time {\n" +
            "    String _CoordinateAxisType \"Time\";\n" +
            "    Float64 actual_range MIN, MAX;\n" +
            "    String axis \"T\";\n" +
            "    String cf_role \"profile_id\";\n" +
            "    String ioos_category \"Time\";\n" +
            "    String long_name \"Time\";\n" +
            "    String standard_name \"time\";\n" +
            "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
            "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "  }\n" +
            "  bottle_posn {\n" +
            "    String _CoordinateAxisType \"Height\";\n" +
            "    Byte _FillValue 127;\n" +
            "    String _Unsigned \"false\";\n" +
            "    Byte actual_range 0, 12;\n" +
            "    String axis \"Z\";\n" +
            "    Float64 colorBarMaximum 12.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Bottle Number\";\n" +
            "    Byte missing_value -128;\n" +
            "  }\n" +
            "  chl_a_total {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Chlorophyll-a\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  chl_a_10um {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Chlorophyll-a after passing 10um screen\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  phaeo_total {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Total Phaeopigments\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  phaeo_10um {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Phaeopigments 10um\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  sal00 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 37.0;\n" +
            "    Float64 colorBarMinimum 32.0;\n" +
            "    String ioos_category \"Salinity\";\n" +
            "    String long_name \"Practical Salinity from T0 and C0 Sensors\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_practical_salinity\";\n" +
            "    String units \"PSU\";\n" +
            "  }\n" +
            "  sal11 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 37.0;\n" +
            "    Float64 colorBarMinimum 32.0;\n" +
            "    String ioos_category \"Salinity\";\n" +
            "    String long_name \"Practical Salinity from T1 and C1 Sensors\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_practical_salinity\";\n" +
            "    String units \"PSU\";\n" +
            "  }\n" +
            "  temperature0 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 32.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Temperature\";\n" +
            "    String long_name \"Sea Water Temperature from T0 Sensor\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_temperature\";\n" +
            "    String units \"degree_C\";\n" +
            "  }\n" +
            "  temperature1 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 32.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Temperature\";\n" +
            "    String long_name \"Sea Water Temperature from T1 Sensor\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_temperature\";\n" +
            "    String units \"degree_C\";\n" +
            "  }\n" +
            "  fluor_v {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 5.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Fluorescence Voltage\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"volts\";\n" +
            "  }\n" +
            "  xmiss_v {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 5.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Optical Properties\";\n" +
            "    String long_name \"Transmissivity Voltage\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"volts\";\n" +
            "  }\n" +
            "  PO4 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 4.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Phosphate\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_phosphate_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  N_N {\n" +
            "    Float32 _FillValue -99.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 50.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Nitrate plus Nitrite\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  NO3 {\n" +
            "    Float32 _FillValue -99.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 50.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Nitrate\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_nitrate_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  Si {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 50.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Silicate\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_silicate_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  NO2 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 1.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Nitrite\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_nitrite_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  NH4 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 5.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Ammonium\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_ammonium_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  oxygen {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 10.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved O2\";\n" +
            "    String long_name \"Oxygen\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n" +
            "    String units \"mL L-1\";\n" +
            "  }\n" +
            "  par {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 3.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Photosynthetically Active Radiation\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"volts\";\n" +
            "  }\n" +
            " }\n" +
            "  NC_GLOBAL {\n" +
            "    String cdm_altitude_proxy \"bottle_posn\";\n" +
            "    String cdm_data_type \"TrajectoryProfile\";\n" +
            "    String cdm_profile_variables \"cast, longitude, latitude, time\";\n" +
            "    String cdm_trajectory_variables \"cruise_id, ship\";\n" +
            "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            "    Float64 Easternmost_Easting -124.1;\n" +
            "    String featureType \"TrajectoryProfile\";\n" +
            "    Float64 geospatial_lat_max 44.65;\n" +
            "    Float64 geospatial_lat_min 41.9;\n" +
            "    String geospatial_lat_units \"degrees_north\";\n" +
            "    Float64 geospatial_lon_max -124.1;\n" +
            "    Float64 geospatial_lon_min -126.2;\n" +
            "    String geospatial_lon_units \"degrees_east\";\n";
    // " String history \"" + today + " 2012-07-29T19:11:09Z (local files; contact
    // erd.data@noaa.gov)\n"; //date is from last created file, so varies sometimes
    // today + "
    // https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle.das"; //\n"
    // +
    // today + " https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
    // today + "
    // http://localhost:8080/cwexperimental/tabledap/rGlobecBottle.das\";\n" +
    expected2 = "    String infoUrl \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n" +
        "    String institution \"GLOBEC\";\n" +
        "    String keywords \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
        +
        "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
        "    String license \"The data may be used and redistributed for free but is not intended\n" +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "    Float64 Northernmost_Northing 44.65;\n" +
        "    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
        "    Float64 Southernmost_Northing 41.9;\n" +
        "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
        "    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
        "    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
        "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
        "Notes:\n" +
        "Physical data processed by Jane Fleischbein (OSU).\n" +
        "Chlorophyll readings done by Leah Feinberg (OSU).\n" +
        "Nutrient analysis done by Burke Hales (OSU).\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
        "secondary sensor pair was used in final processing of CTD data for\n" +
        "most stations because the primary had more noise and spikes. The\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
        "multiple spikes or offsets in the secondary pair.\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\n" +
        "data developed by Burke Hales (OSU).\n" +
        "Operation Detection Limits for Nutrient Concentrations\n" +
        "Nutrient  Range         Mean    Variable         Units\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
        "Dates and Times are UTC.\n" +
        "\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\n" +
        "\n" +
        "Inquiries about how to access this data should be directed to\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
        "    String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ\";\n" +
        "    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
        "    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
        "    Float64 Westernmost_Easting -126.2;\n" +
        "  }\n" +
        "}\n";

    results = results.replaceAll("Int16 actual_range [0-9]+, [0-9]+;", "Int16 actual_range MIN, MAX;");
    results = results.replaceAll("Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;", "Float32 actual_range MIN, MAX;");
    results = results.replaceAll("Float64 actual_range -?[0-9].[0-9]+e[+][0-9], -?[0-9].[0-9]+e[+][0-9];", "Float64 actual_range MIN, MAX;");
    results = results.replaceAll("String time_coverage_end \\\"....-..-..T..:..:..Z", "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf("    String infoUrl ");
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName = edd.makeNewFileForDapQuery(language, null, null, "", tDir,
        edd.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    String cruise_id;\n" +
        "    String ship;\n" +
        "    Int16 cast;\n" +
        "    Float32 longitude;\n" +
        "    Float32 latitude;\n" +
        "    Float64 time;\n" +
        "    Byte bottle_posn;\n" +
        "    Float32 chl_a_total;\n" +
        "    Float32 chl_a_10um;\n" +
        "    Float32 phaeo_total;\n" +
        "    Float32 phaeo_10um;\n" +
        "    Float32 sal00;\n" +
        "    Float32 sal11;\n" +
        "    Float32 temperature0;\n" +
        "    Float32 temperature1;\n" +
        "    Float32 fluor_v;\n" +
        "    Float32 xmiss_v;\n" +
        "    Float32 PO4;\n" +
        "    Float32 N_N;\n" +
        "    Float32 NO3;\n" +
        "    Float32 Si;\n" +
        "    Float32 NO2;\n" +
        "    Float32 NH4;\n" +
        "    Float32 oxygen;\n" +
        "    Float32 par;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName = edd.makeNewFileForDapQuery(language, null, null, "", tDir,
        edd.className() + "_Entire", ".html");
    results = File2.directReadFromUtf8File(tDir + tName);
    expected = "<option>.png - View a standard, medium-sized .png image file with a graph or map.";
    expected2 = "    String _CoordinateAxisType &quot;Lon&quot;;";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    // Test.displayInBrowser("file://" + tDir + tName);

    // *** test make data files
    String2.log("\n****************** EDDTableCopy.test make DATA FILES\n");

    // .asc
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
        edd.className() + "_Data", ".asc");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    Float32 longitude;\n" +
        "    Float32 NO3;\n" +
        "    Float64 time;\n" +
        "    String ship;\n" +
        "  } s;\n" +
        "} s;\n" +
        "---------------------------------------------\n" +
        "s.longitude, s.NO3, s.time, s.ship\n" +
        "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"\n";
    expected2 = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"\n"; // row with missing value has source missing
                                                                    // value
    expected3 = "-124.57, 19.31, 1.02939792E9, \"New_Horizon\"\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected3) > 0, "\nresults=\n" + results); // last row in erdGlobedBottle, not
                                                                               // last
                                                                               // here

    // .csv
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
        edd.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    expected2 = "-124.8,NaN,2002-08-03T07:17:00Z,New_Horizon\n"; // row with missing value has source missing value
    expected3 = "-124.57,19.31,2002-08-15T07:52:00Z,New_Horizon\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected3) > 0, "\nresults=\n" + results); // last row in erdGlobedBottle, not
                                                                               // last
                                                                               // here

    // .dds
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
        edd.className() + "_Data", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    Float32 longitude;\n" +
        "    Float32 NO3;\n" +
        "    Float64 time;\n" +
        "    String ship;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dods
    // tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
    // edd.className() + "_Data", ".dods");
    // Test.displayInBrowser("file://" + tDir + tName);
    String2.log("\ndo .dods test");
    String tUrl = EDStatic.erddapUrl + // in tests, always use non-https url
        "/tabledap/" + edd.datasetID();
    // for diagnosing during development:
    // String2.log(String2.annotatedString(SSR.getUrlResponseStringUnchanged(
    // "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
    // String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseStringUnchanged(tUrl +
    // ".das?" + userDapQuery));
    // String2.log("\nDODS RESPONSE=" +
    // String2.annotatedString(SSR.getUrlResponseStringUnchanged(tUrl + ".dods?" +
    // userDapQuery)));

    // test if table.readOpendapSequence works with Erddap opendap server
    // !!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in
    // tests, always use non-https url
    // !!!THIS IS NOT JUST A LOCAL TEST!!!
    Table tTable = new Table();
    tTable.readOpendapSequence(tUrl + "?" + userDapQuery, false);
    Test.ensureEqual(tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
    Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
    Test.ensureEqual(tTable.getColumnNames(), new String[] { "longitude", "NO3", "time", "ship" }, "");
    Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
    Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
    Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
    Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
    String2.log("  .dods test succeeded");

    // test .png
    String baseName = edd.className() + "_GraphM";
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery,
        Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR), baseName, ".png");
    // Test.displayInBrowser("file://" + tDir + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

  } // end of testBasic

  /**
   * This tests the /files/ "files" system.
   * This requires testTableCopy in the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testTableCopyFiles() throws Throwable {

    String2.log("\n*** EDDTableCopy.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/.csv");
    expected = "Name,Last modified,Size,Description\n" +
        "nh0207/,NaN,NaN,\n" +
        "w0205/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/");
    Test.ensureTrue(results.indexOf("nh0207&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("nh0207/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("w0205&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("w0205/") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/.csv");
    expected = "Name,lastModTime,Size,Description\n" +
        "1.nc,lastModTime,14384,\n" +
        "10.nc,lastModTime,15040,\n" +
        "100.nc,lastModTime,14712,\n";
    results = results.replaceAll(",.............,", ",lastModTime,");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // download a file in root

    // download a file in subdir
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/100.nc").substring(0, 50));
    expected = "CDF[1][0][0][0][0][0][0][0][10]\n" +
        "[0][0][0][3][0][0][0][3]row[0][0][0][0][6][0][0][0][16]cruise_id_strlen[0][0][end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
