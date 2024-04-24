package gov.noaa.pfel.coastwatch.pointdata;

import java.nio.file.Path;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import com.cohort.util.File2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.SSR;

import org.eclipse.jetty.ee10.webapp.WebAppContext;

import tags.TagJetty;
import testDataset.Initialization;

class TableJettyTests {

    @TempDir
    private static Path TEMP_DIR;

    private static Server server;
    private static Integer PORT = 8080;

    @BeforeAll
    public static void setUp() throws Throwable {
        Initialization.edStatic();   
        
        server = new Server(PORT);

        WebAppContext context = new WebAppContext();
        ResourceFactory resourceFactory = ResourceFactory.of(context);
        Resource baseResource = resourceFactory.newResource(Path.of(System.getProperty("user.dir")).toAbsolutePath().toUri());
        context.setBaseResource(baseResource);
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        server.setHandler(context);

        server.start();
        // Make a request of the server to make sure it starts loading the datasets
        String request = "http://localhost:" + PORT + "/erddap";
        System.out.println("request url: " + request);
        SSR.getUrlResponseStringUnchanged(request);

        Thread.sleep(5*60*1000);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
    }

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
        // http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.json
        String tUrl = "http://localhost:" + PORT + "/erddap";
        table.readErddapInfo(tUrl + "/info/pmelTaoDySst/index.json");
        String ncHeader = table.getNCHeader("row");
        Test.ensureEqual(table.globalAttributes.getString("cdm_data_type"), "TimeSeries", ncHeader);
        Test.ensureEqual(table.globalAttributes.getString("title"),
                "TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature",
                ncHeader);
        Test.ensureEqual(table.globalAttributes.get("history").size(), 3, ncHeader);
        Test.ensureEqual(table.globalAttributes.get("history").getString(0),
                "This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.",
                ncHeader);
        Test.ensureEqual(table.globalAttributes.get("history").getString(1),
                "This dataset is a product of the TAO Project Office at NOAA/PMEL.",
                ncHeader);
        Test.ensureLinesMatch(table.globalAttributes.get("history").getString(2),
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
    
}
