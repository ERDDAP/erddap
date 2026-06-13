package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromDatabaseTests {

  private static String testUser = "sa";
  private static String testUrl = "jdbc:h2:mem:legacy_db;DB_CLOSE_DELAY=-1";
  private static String testDriver = "org.h2.Driver";

  private static Connection keepAliveConn;

  @BeforeAll
  static void init() throws Exception {
    Initialization.edStatic();
    String url = "jdbc:h2:mem:legacy_db;DB_CLOSE_DELAY=-1";
    Class.forName("org.h2.Driver");
    keepAliveConn = DriverManager.getConnection(url, "sa", "");
    ensureLegacyDatabase();
  }

  private static void ensureLegacyDatabase() {
    String2.log("starting ensureLegacyDatabase");
    try (Statement stmt = keepAliveConn.createStatement()) {
      String2.log("have db connection");
      // stmt.execute("DROP TABLE IF EXISTS legacy_table");
      stmt.execute(
          "CREATE TABLE legacy_table ("
              + "id INT PRIMARY KEY, "
              + "first VARCHAR(255), "
              + "last VARCHAR(255), "
              + "height INT, "
              + "weight_kg DOUBLE, "
              + "weight_lb DOUBLE, "
              + "birthdate TIMESTAMP, "
              + "category VARCHAR(1))");
      String2.log("table created");
      stmt.execute(
          "INSERT INTO legacy_table VALUES (1, 'Bob', 'Bucher', 182, 83.2, 183, '1966-01-31T16:16:17Z', 'A')");
      stmt.execute(
          "INSERT INTO legacy_table VALUES (2, 'Stan', 'Smith', 177, 81.1, 179, '1971-10-12T23:24:25Z', 'B')");
      stmt.execute(
          "INSERT INTO legacy_table VALUES (3, 'John', 'Johnson', 191, 88.5, 195, '1961-03-05T04:05:06Z', 'A')");
      stmt.execute(
          "INSERT INTO legacy_table VALUES (4, 'Zele', 'Zule', NULL, NULL, NULL, NULL, NULL)");
      stmt.execute(
          "INSERT INTO legacy_table VALUES (5, 'Betty', 'Bach', 161, 54.2, 119, '1967-07-08T09:10:11Z', 'B')");
      String2.log("data inserted");
    } catch (Exception e) {
      throw new RuntimeException("Unable to initialize legacy H2 test database", e);
    }
  }

  @AfterAll
  static void destroy() throws Exception {
    if (keepAliveConn != null && !keepAliveConn.isClosed()) {
      keepAliveConn.close();
    }
  }

  /**
   * This tests generateDatasetsXml. 2014-01-14 Bob downloaded postgresql-9.3.2-1-windows-x64.exe
   * Installed in /programs/postgresql93 <br>
   * To work as admin: cd \programs\PostgreSQL93\bin pgAdmin3 <br>
   * Log in: Double click on "Servers : PostgreSQL 9.3 (localhost:5432) <br>
   * Work in database=postgres schema=public <br>
   * Create table, columns, PrimaryKey: Edit : Create : ... <br>
   * table=mytable
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {

    // String2.log("\n*** EDDTableFromDatabase.testGenerateDatasetsXml");
    // testVerboseOn();
    String tName, results, expected;
    String password;
    password = "";
    String connectionProps[] = new String[] {"user", testUser, "password", password};
    String dir = EDStatic.config.fullTestCacheDirectory;
    int language = 0;

    // list catalogs
    String2.log("* list catalogs");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            testUrl,
            testDriver,
            connectionProps,
            "!!!LIST!!!",
            "!!!LIST!!!",
            "!!!LIST!!!"); // catalog, schema, table
    expected = "TABLE_CAT\n" + "\n" + "LEGACY_DB\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // list schemas
    String2.log("* list schemas");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            testUrl,
            testDriver,
            connectionProps,
            "",
            "!!!LIST!!!",
            "!!!LIST!!!"); // catalog, schema, table
    expected =
        "TABLE_SCHEM,TABLE_CATALOG\n"
            + ",\n"
            + "INFORMATION_SCHEMA,LEGACY_DB\n"
            + "PUBLIC,LEGACY_DB\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // list tables
    String2.log("* list tables");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            testUrl,
            testDriver,
            connectionProps,
            "LEGACY_DB",
            "PUBLIC",
            "!!!LIST!!!"); // catalog, schema, table
    expected =
        "TABLE_CAT,TABLE_SCHEM,TABLE_NAME,TABLE_TYPE,REMARKS,TYPE_CAT,TYPE_SCHEM,TYPE_NAME,SELF_REFERENCING_COL_NAME,REF_GENERATION\n"
            + ",,,,,,,,,\n"
            + "LEGACY_DB,PUBLIC,LEGACY_TABLE,BASE TABLE,,,,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // getDatabaseInfo
    String2.log("* getDatabaseInfo");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            testUrl,
            testDriver,
            connectionProps,
            "LEGACY_DB",
            "PUBLIC",
            "LEGACY_TABLE"); // catalog, schema, table
    expected =
        "Col Key Name                    java.sql.Types Java Type Remarks\n"
            + "0   P   ID                      4              int       \n"
            + "1       FIRST                   12             String    \n"
            + "2       LAST                    12             String    \n"
            + "3       HEIGHT                  4              int       \n"
            + "4       WEIGHT_KG               8              double    \n"
            + "5       WEIGHT_LB               8              double    \n"
            + "6       BIRTHDATE               TimeStamp      double    \n"
            + "7       CATEGORY                12             String    \n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromDatabase",
                  testUrl, // s1
                  testDriver, // s2
                  String2.toSVString(connectionProps, "|", false), // s3
                  "LEGACY_DB",
                  "PUBLIC",
                  "LEGACY_TABLE",
                  "", // s7 orderBy csv
                  "99", // s8 reloadEveryNMinute
                  "http://www.pfeg.noaa.gov", // s9 infoUrl
                  "NOAA NMFS SWFSC ERD", // s10 institution
                  "", // s11 summary
                  "", // s12 title
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?

    expected =
        "<!-- NOTE! Since database tables don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromDatabase\" datasetID=\"LEGACY_DB_PUBLIC_LEGACY_TABLE\" active=\"true\">\n"
            + "    <sourceUrl>"
            + testUrl
            + "</sourceUrl>\n"
            + "    <driverName>"
            + testDriver
            + "</driverName>\n"
            + "    <connectionProperty name=\"user\">"
            + testUser
            + "</connectionProperty>\n"
            + "    <connectionProperty name=\"password\">"
            + password
            + "</connectionProperty>\n"
            + "    <catalogName>LEGACY_DB</catalogName>\n"
            + "    <schemaName>PUBLIC</schemaName>\n"
            + "    <tableName>LEGACY_TABLE</tableName>\n"
            + "    <reloadEveryNMinutes>99</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n"
            + "        <att name=\"infoUrl\">https://www.pfeg.noaa.gov</att>\n"
            + "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"keywords\">birthdate, category, center, data, erd, first, fisheries, height, identifier, local, marine, national, nmfs, noaa, science, service, source, southwest, swfsc, time, weight, weight_kg, weight_lb</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local database)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">NOAA National Marine Fisheries Service (NMFS) Southwest Fisheries Science Center (SWFSC) ERD data from a local source.</att>\n"
            + "        <att name=\"title\">NOAA NMFS SWFSC ERD data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ID</sourceName>\n"
            + "        <destinationName>id</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">ID</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FIRST</sourceName>\n"
            + "        <destinationName>first</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">FIRST</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LAST</sourceName>\n"
            + "        <destinationName>last</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">LAST</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>HEIGHT</sourceName>\n"
            + "        <destinationName>height</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">HEIGHT</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WEIGHT_KG</sourceName>\n"
            + "        <destinationName>weight_kg</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">WEIGHT KG</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WEIGHT_LB</sourceName>\n"
            + "        <destinationName>weight_lb</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">WEIGHT LB</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>BIRTHDATE</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">BIRTHDATE</att>\n"
            + "            <att name=\"source_name\">BIRTHDATE</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>CATEGORY</sourceName>\n"
            + "        <destinationName>category</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">CATEGORY</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // ensure it is ready-to-use by making a dataset from it
    // !!! This doesn't actually request data, so it isn't a complete test
    String tDatasetID = "LEGACY_DB_PUBLIC_LEGACY_TABLE";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromDatabase.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "id, first, last, height, weight_kg, weight_lb, time, category",
        "");

    // !!! This does request data, so it is a complete test
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&orderBy(\"id\")",
            dir,
            edd.className() + "_" + tDatasetID + "_getCSV",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "id,first,last,height,weight_kg,weight_lb,time,category\n"
            + ",,,,,,UTC,\n"
            + "1,Bob,Bucher,182,83.2,183.0,1966-01-31T16:16:17Z,A\n"
            + "2,Stan,Smith,177,81.1,179.0,1971-10-12T23:24:25Z,B\n"
            + "3,John,Johnson,191,88.5,195.0,1961-03-05T04:05:06Z,A\n"
            + "4,Zele,Zule,NaN,NaN,NaN,,\n"
            + "5,Betty,Bach,161,54.2,119.0,1967-07-08T09:10:11Z,B\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This performs basic tests of the local postgres database.
   *
   * @param tDatasetID testMyDatabaseNo or testMyDatabasePartial or testMyDatabaseYes, to test
   *     sourceCanOrderBy=x and sourceCanDoDistinct=x. tDatasetID=testMyDabaseYes tests
   *     sourceCanOrderBy=yes and sourceCanDoDistinct=yes.
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(strings = {"testMyDatabaseNo", "testMyDatabasePartial", "testMyDatabaseYes"})
  void testBasic(String tDatasetID) throws Throwable {
    // testVerboseOn();
    int language = 0;
    long eTime;
    String dir = EDStatic.config.fullTestCacheDirectory;
    String results, expected;

    EDDTableFromDatabase tedd;
    switch (tDatasetID) {
      case "testMyDatabaseNo":
        tedd = (EDDTableFromDatabase) EDDTestDataset.gettestMyDatabaseNo();
        break;
      case "testMyDatabasePartial":
        tedd = (EDDTableFromDatabase) EDDTestDataset.gettestMyDatabasePartial();
        break;
      case "testMyDatabaseYes":
        tedd = (EDDTableFromDatabase) EDDTestDataset.gettestMyDatabaseYes();
        break;
      default:
        throw new RuntimeException("unexpected tDatasetID=" + tDatasetID);
    }

    String tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_Basic", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("2\\d{3}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "[TIME]");
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  category {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Category\";\n"
            + "  }\n"
            + "  first {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"First Name\";\n"
            + "  }\n"
            + "  last {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Last Name\";\n"
            + "  }\n"
            + "  height {\n"
            + "    Int32 _FillValue 2147483647;\n"
            + "    String ioos_category \"Biology\";\n"
            + "    String long_name \"Height\";\n"
            + "    String units \"cm\";\n"
            + "  }\n"
            + "  weight_kg {\n"
            + "    String ioos_category \"Biology\";\n"
            + "    String long_name \"Weight\";\n"
            + "    String units \"kg\";\n"
            + "  }\n"
            + "  weight_lb {\n"
            + "    Float64 _FillValue 2.147483647e+9;\n"
            + "    String ioos_category \"Biology\";\n"
            + "    String long_name \"Weight\";\n"
            + "    String units \"lb\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Birthdate\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Other\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String history \"[TIME]Z (source database)\n"
            + "[TIME]Z http://localhost:8080/erddap/tabledap/"
            + tDatasetID
            + ".das\";\n"
            + "    String infoUrl \"https://www.fisheries.noaa.gov/contact/environmental-research-division-southwest-fisheries-science-center\";\n"
            + "    String institution \"NOAA NMFS SWFSC ERD\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies.\";\n"
            + "    String sourceUrl \"(source database)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"This is Bob's test for reading from a database table.\";\n"
            + "    String title \"mydatabase myschema mytable\";\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dds
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_peb_Data", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String category;\n"
            + "    String first;\n"
            + "    String last;\n"
            + "    Int32 height;\n"
            + "    Float64 weight_kg;\n"
            + "    Float64 weight_lb;\n"
            + "    Float64 time;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    /* */

    // all check dataset's orderBy
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,first,last,height,weight_kg,weight_lb,time\n"
            + ",,,cm,kg,lb,UTC\n"
            + "A,Bob,Bucher,182,83.2,183.0,1966-01-31T16:16:17Z\n"
            + "B,Stan,Smith,177,81.1,179.0,1971-10-12T23:24:25Z\n"
            + "A,John,Johnson,191,88.5,195.0,1961-03-05T04:05:06Z\n"
            + ",Zele,Zule,NaN,NaN,NaN,\n"
            + "B,Betty,Bach,161,54.2,119.0,1967-07-08T09:10:11Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  all time=" + (System.currentTimeMillis() - eTime) + "ms");

    // subset
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time,last&time=1967-07-08T09:10:11Z",
            dir,
            tedd.className() + "_subset",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "time,last\n" + "UTC,\n" + "1967-07-08T09:10:11Z,Bach\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  subset time=" + (System.currentTimeMillis() - eTime) + "ms");

    // constrain numeric variable
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "&height<162", dir, tedd.className() + "_ht162", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,first,last,height,weight_kg,weight_lb,time\n"
            + ",,,cm,kg,lb,UTC\n"
            + "B,Betty,Bach,161,54.2,119.0,1967-07-08T09:10:11Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test height=NaN
    results = "unexpected";
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "&height=NaN", dir, tedd.className() + "_htEqNaN", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
    } catch (Exception e) {
      results = e.getMessage();
    }
    expected = "Your query produced no matching results. (nRows = 0)";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test height!=NaN
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "&height!=NaN", dir, tedd.className() + "_htNeNaN", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,first,last,height,weight_kg,weight_lb,time\n"
            + ",,,cm,kg,lb,UTC\n"
            + "A,Bob,Bucher,182,83.2,183.0,1966-01-31T16:16:17Z\n"
            + "B,Stan,Smith,177,81.1,179.0,1971-10-12T23:24:25Z\n"
            + "A,John,Johnson,191,88.5,195.0,1961-03-05T04:05:06Z\n"
            + "B,Betty,Bach,161,54.2,119.0,1967-07-08T09:10:11Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // just script variable
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "weight_lb&time=1967-07-08T09:10:11Z",
            dir,
            tedd.className() + "_script2",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "weight_lb\n" + "lb\n" + "119.0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  subset time=" + (System.currentTimeMillis() - eTime) + "ms");

    // constrain just script variable
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "&weight_lb=119", dir, tedd.className() + "_script3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,first,last,height,weight_kg,weight_lb,time\n"
            + ",,,cm,kg,lb,UTC\n"
            + "B,Betty,Bach,161,54.2,119.0,1967-07-08T09:10:11Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  subset time=" + (System.currentTimeMillis() - eTime) + "ms");

    // constrain script variable (first, not passed to database) and non-script
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&weight_lb=119&category=\"B\"",
            dir,
            tedd.className() + "_script4",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,first,last,height,weight_kg,weight_lb,time\n"
            + ",,,cm,kg,lb,UTC\n"
            + "B,Betty,Bach,161,54.2,119.0,1967-07-08T09:10:11Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  subset time=" + (System.currentTimeMillis() - eTime) + "ms");

    // distinct() subsetVariables
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "category&distinct()", dir, tedd.className() + "_subset", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "category\n" + "\n" + "\n" + "A\n" + "B\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  distinct time=" + (System.currentTimeMillis() - eTime) + "ms");

    // distinct() subsetVariables
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "weight_lb&distinct()",
            dir,
            tedd.className() + "_subset2",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "weight_lb\n" + "lb\n" + "119.0\n" + "179.0\n" + "183.0\n" + "195.0\n" + "NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  distinct time=" + (System.currentTimeMillis() - eTime) + "ms");

    // distinct() 2 vars (one of which is script)
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category,weight_lb&distinct()",
            dir,
            tedd.className() + "_distinct1",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = // tDatasetID.equals("testMyDatabaseNo") ||
        // tDatasetID.equals("testMyDatabasePartial")?
        // ERDDAP sorts category="" at top.
        // 2019-12-10 now ERDDAP always does distinct (even if database does, too)
        "category,weight_lb\n"
            + ",lb\n"
            + ",NaN\n"
            + "A,183.0\n"
            + "A,195.0\n"
            + "B,119.0\n"
            + "B,179.0\n";
    /*
     * :
     * //Postgres sorts "" at bottom
     * "category,first\n" +
     * ",\n" +
     * "A,Bob\n" +
     * "A,John\n" +
     * "B,Betty\n" +
     * "B,Stan\n" +
     * ",Zele\n";
     */
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  distinct time=" + (System.currentTimeMillis() - eTime) + "ms");

    // distinct() 2 vars, different order
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "first,weight_lb&distinct()",
            dir,
            tedd.className() + "_distinct2",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "first,weight_lb\n"
            + ",lb\n"
            + "Betty,119.0\n"
            + "Bob,183.0\n"
            + "John,195.0\n"
            + "Stan,179.0\n"
            + "Zele,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  distinct time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderBy() subsetVars
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category&orderBy(\"category\")",
            dir,
            tedd.className() + "_orderBy1",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "category\n\n\n" + "A\n" + "A\n" + "B\n" + "B\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderBy subsetVars time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderBy() subsetVars
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "weight_lb&orderBy(\"weight_lb\")",
            dir,
            tedd.className() + "_orderBy1a",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "weight_lb\n" + "lb\n" + "119.0\n" + "179.0\n" + "183.0\n" + "195.0\n" + "NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderBy subsetVars time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderBy()
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category,last,first&orderBy(\"last,category\")",
            dir,
            tedd.className() + "_orderBy2",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,last,first\n"
            + ",,\n"
            + "B,Bach,Betty\n"
            + "A,Bucher,Bob\n"
            + "A,Johnson,John\n"
            + "B,Smith,Stan\n"
            + ",Zule,Zele\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderBy time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderBy()
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category,last,first,weight_lb&orderBy(\"category,weight_lb,last\")",
            dir,
            tedd.className() + "_orderBy3",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = // tDatasetID.equals("testMyDatabaseNo") ||
        // tDatasetID.equals("testMyDatabasePartial")?
        // 2019-12-12 now ERDDAP always does orderBy (even if database does, too)
        // ERDDAP sorts category="" at top.
        "category,last,first,weight_lb\n"
            + ",,,lb\n"
            + ",Zule,Zele,NaN\n"
            + "A,Bucher,Bob,183.0\n"
            + "A,Johnson,John,195.0\n"
            + "B,Bach,Betty,119.0\n"
            + "B,Smith,Stan,179.0\n";
    /*
     * :
     * //Postgres sorts "" at bottom
     * "category,last,first\n" +
     * ",,\n" + //units
     * "A,Bucher,Bob\n" +
     * "A,Johnson,John\n" +
     * "B,Bach,Betty\n" +
     * "B,Smith,Stan\n" +
     * ",Zule,Zele\n";
     */

    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderBy time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderBy() and distinct()
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category,last,first&orderBy(\"category,last\")&distinct()",
            dir,
            tedd.className() + "_orderBy4",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        // ERDDAP's distinct() is always done and it sorts "" at top.
        "category,last,first\n"
            + ",,\n"
            + // units
            ",Zule,Zele\n"
            + "A,Bucher,Bob\n"
            + "A,Johnson,John\n"
            + "B,Bach,Betty\n"
            + "B,Smith,Stan\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderBy + distinct time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderByMax() and distinct()
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category,last,first&orderByMax(\"category,last\")&distinct()",
            dir,
            tedd.className() + "_orderBy5",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // ERDDAP sorts "" at top and it is ERDDAP's orderByMax that is done last
    expected =
        "category,last,first\n"
            + ",,\n"
            + // units
            ",Zule,Zele\n"
            + "A,Johnson,John\n"
            + "B,Smith,Stan\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderByMax + distinct time=" + (System.currentTimeMillis() - eTime) + "ms");

    // orderByMax() and orderBy()
    eTime = System.currentTimeMillis();
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "category,last,first&orderByMax(\"category,last\")&orderBy(\"first\")",
            dir,
            tedd.className() + "_orderBy6",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,last,first\n"
            + ",,\n"
            + // units
            "A,Johnson,John\n"
            + "B,Smith,Stan\n"
            + ",Zule,Zele\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("  orderByMax + orderBy time=" + (System.currentTimeMillis() - eTime) + "ms");

    // no matching data (database determined)
    eTime = System.currentTimeMillis();
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "last,height&height=170",
              dir,
              tedd.className() + "_subset",
              ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected = "Shouldn't get here";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    } catch (Throwable t) {
      String msg = MustBe.throwableToString(t);
      String2.log(msg + "  no matching data time=" + (System.currentTimeMillis() - eTime) + "ms");
      if (msg.indexOf("Your query produced no matching results.") < 0)
        throw new RuntimeException("Unexpected error.", t);
    }

    // quick reject -> orderBy var not in results vars
    // orderBy()
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "category,last&orderBy(\"category,last,first\")",
              dir,
              tedd.className() + "_qr1",
              ".csv");
      throw new SimpleException("Shouldn't get here");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      results = t.toString();
      expected =
          "com.cohort.util.SimpleException: Query error: orderBy "
              + "variable=first isn't in the list of results variables.";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    // quick reject -> no matching data
    eTime = System.currentTimeMillis();
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "last,height&height>1000",
              dir,
              tedd.className() + "_qr2",
              ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected = "Shouldn't get here";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    } catch (Throwable t) {
      String msg = t.toString();
      String2.log(msg + "  quick reject time=" + (System.currentTimeMillis() - eTime) + "ms");
      Test.ensureTrue(
          msg.contains("com.cohort.util.SimpleException: Your query produced no matching results."),
          "");
    }
  }

  /**
   * Test datasets.xml specified a non-existent variable.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testNonExistentVariable() throws Throwable {
    String dir = EDStatic.config.fullTestCacheDirectory;
    String results = "not set";
    int language = 0;
    try {
      // if there is no subsetVariables att, the dataset will be created successfully
      EDDTableFromDatabase edd = (EDDTableFromDatabase) EDDTestDataset.gettestNonExistentVariable();
      results = "shouldn't get here";
      edd.getDataForDapQuery(
          language,
          null,
          "",
          "", // should throw error
          new TableWriterAll(language, edd, "", dir, "testNonExistentVariable"));
      results = "really shouldn't get here";
    } catch (Throwable t) {
      results = MustBe.throwableToString(t);
    }
    Test.ensureTrue(results.indexOf("Column \"zztop\" not found") >= 0, "results=\n" + results);
  }

  /**
   * Test datasets.xml specified a non-existent table.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testNonExistentTable() throws Throwable {
    String dir = EDStatic.config.fullTestCacheDirectory;
    String results = "not set";
    int language = 0;
    try {
      // if there is no subsetVariables att, the dataset will be created successfully
      EDDTableFromDatabase edd = (EDDTableFromDatabase) EDDTestDataset.gettestNonExistentTable();
      results = "shouldn't get here";
      edd.getDataForDapQuery(
          language,
          null,
          "",
          "", // should throw error
          new TableWriterAll(language, edd, "", dir, "testNonExistentTable"));
      results = "really shouldn't get here";
    } catch (Throwable t) {
      results = MustBe.throwableToString(t);
    }
    Test.ensureTrue(
        results.indexOf(" Table \"MISSING_TABLE\" not found;") >= 0, "results=\n" + results);
  }
}
