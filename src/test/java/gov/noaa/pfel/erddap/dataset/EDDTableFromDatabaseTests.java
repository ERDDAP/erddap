package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagMissingDataset;
import testDataset.Initialization;

class EDDTableFromDatabaseTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
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
  @TagMissingDataset
  void testGenerateDatasetsXml() throws Throwable {

    // String2.log("\n*** EDDTableFromDatabase.testGenerateDatasetsXml");
    // testVerboseOn();
    String name, tName, gdiResults, results, tResults, expected, userDapQuery, tQuery;
    String password;
    // password = String2.getStringFromSystemIn("local Postgres password? ");
    password = "MyPassword";
    String connectionProps[] =
        new String[] {"user", EDDTableFromDatabase.testUser, "password", password};
    String dir = EDStatic.fullTestCacheDirectory;
    int language = 0;

    // list catalogs
    String2.log("* list catalogs");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            EDDTableFromDatabase.testUrl,
            EDDTableFromDatabase.testDriver,
            connectionProps,
            "!!!LIST!!!",
            "!!!LIST!!!",
            "!!!LIST!!!"); // catalog, schema, table
    expected = "TABLE_CAT\n" + "\n" + "mydatabase\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // list schemas
    String2.log("* list schemas");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            EDDTableFromDatabase.testUrl,
            EDDTableFromDatabase.testDriver,
            connectionProps,
            "",
            "!!!LIST!!!",
            "!!!LIST!!!"); // catalog, schema, table
    expected =
        "table_schem,table_catalog\n"
            + ",\n"
            + "information_schema,\n"
            + "myschema,\n"
            + "pg_catalog,\n"
            + "public,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // list tables
    String2.log("* list tables");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            EDDTableFromDatabase.testUrl,
            EDDTableFromDatabase.testDriver,
            connectionProps,
            "",
            "myschema",
            "!!!LIST!!!"); // catalog, schema, table
    expected =
        "TABLE_CAT,TABLE_SCHEM,TABLE_NAME,TABLE_TYPE,REMARKS,TYPE_CAT,TYPE_SCHEM,TYPE_NAME,SELF_REFERENCING_COL_NAME,REF_GENERATION\n"
            + ",,,,,,,,,\n"
            + ",myschema,id,INDEX,,,,,,\n"
            + ",myschema,mytable,TABLE,,,,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // getDatabaseInfo
    String2.log("* getDatabaseInfo");
    results =
        EDDTableFromDatabase.getDatabaseInfo(
            EDDTableFromDatabase.testUrl,
            EDDTableFromDatabase.testDriver,
            connectionProps,
            "",
            "myschema",
            "mytable"); // catalog, schema, table
    expected =
        "Col Key Name                    java.sql.Types Java Type Remarks\n"
            + "0   P   id                      4              int       \n"
            + "1       first                   12             String    \n"
            + "2       last                    12             String    \n"
            + "3       height_cm               4              int       \n"
            + "4       weight_kg               8              double    \n"
            + "5       birthdate               TimeStamp      double    \n"
            + "6       category                1              String    \n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromDatabase",
                  EDDTableFromDatabase.testUrl, // s1
                  EDDTableFromDatabase.testDriver, // s2
                  String2.toSVString(connectionProps, "|", false), // s3
                  "",
                  "myschema",
                  "mytable", // s4,5,6
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
            + "<dataset type=\"EDDTableFromDatabase\" datasetID=\"myschema_mytable\" active=\"true\">\n"
            + "    <sourceUrl>"
            + EDDTableFromDatabase.testUrl
            + "</sourceUrl>\n"
            + "    <driverName>"
            + EDDTableFromDatabase.testDriver
            + "</driverName>\n"
            + "    <connectionProperty name=\"user\">postgres</connectionProperty>\n"
            + "    <connectionProperty name=\"password\">"
            + password
            + "</connectionProperty>\n"
            + "    <catalogName></catalogName>\n"
            + "    <schemaName>myschema</schemaName>\n"
            + "    <tableName>mytable</tableName>\n"
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
            + "        <att name=\"keywords\">birthdate, category, center, data, erd, first, fisheries, height, height_cm, identifier, local, marine, national, nmfs, noaa, science, service, source, southwest, swfsc, time, weight, weight_kg</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local database)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">NOAA National Marine Fisheries Service (NMFS) Southwest Fisheries Science Center (SWFSC) ERD data from a local source.</att>\n"
            + "        <att name=\"title\">NOAA NMFS SWFSC ERD data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>id</sourceName>\n"
            + "        <destinationName>id</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Id</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>first</sourceName>\n"
            + "        <destinationName>first</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">First</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>last</sourceName>\n"
            + "        <destinationName>last</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Last</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>height_cm</sourceName>\n"
            + "        <destinationName>height_cm</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Height Cm</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>weight_kg</sourceName>\n"
            + "        <destinationName>weight_kg</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Weight Kg</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>birthdate</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Birthdate</att>\n"
            + "            <att name=\"source_name\">birthdate</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>category</sourceName>\n"
            + "        <destinationName>category</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Category</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // ensure it is ready-to-use by making a dataset from it
    // !!! This doesn't actually request data, so it isn't a complete test
    String tDatasetID = "myschema_mytable";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromDatabase.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "id, first, last, height_cm, weight_kg, time, category",
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
        "id,first,last,height_cm,weight_kg,time,category\n"
            + ",,,,,UTC,\n"
            + "1,Bob,Bucher,182,83.2,1966-01-31T16:16:17Z,A\n"
            + "2,Stan,Smith,177,81.1,1971-10-12T23:24:25Z,B\n"
            + "3,John,Johnson,191,88.5,1961-03-05T04:05:06Z,A\n"
            + "4,Zele,Zule,NaN,NaN,,\n"
            + "5,Betty,Bach,161,54.2,1967-07-08T09:10:11Z,B\n";
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
  @TagMissingDataset
  @ValueSource(strings = {"testMyDatabaseNo", "testMyDatabasePartial", "testMyDatabaseYes"})
  void testBasic(String tDatasetID) throws Throwable {
    // String2.log("\n*** EDDTableFromDatabase.testBasic() tDatasetID=" +
    // tDatasetID);
    // testVerboseOn();
    int language = 0;
    long eTime;
    String tQuery;
    String dir = EDStatic.fullTestCacheDirectory;
    String results, expected;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    EDDTableFromDatabase tedd =
        (EDDTableFromDatabase) EDDTableFromDatabase.oneFromDatasetsXml(null, tDatasetID);
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
            + "    Int32 actual_range 161, 191;\n"
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
            + "    Int32 _FillValue 2147483647;\n"
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
            + "[TIME]Z http://127.0.0.1:8080/cwexperimental/tabledap/"
            + tDatasetID
            + ".das\";\n"
            + "    String infoUrl \"https://www.fisheries.noaa.gov/contact/environmental-research-division-southwest-fisheries-science-center\";\n"
            + "    String institution \"NOAA NMFS SWFSC ERD\";\n"
            + "    String keywords \"birthdate, category, first, height, last, weight\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String sourceUrl \"(source database)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"category\";\n"
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
            + "    Int32 weight_lb;\n"
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
            + "A,Bob,Bucher,182,83.2,183,1966-01-31T16:16:17Z\n"
            + "A,John,Johnson,191,88.5,195,1961-03-05T04:05:06Z\n"
            + "B,Betty,Bach,161,54.2,119,1967-07-08T09:10:11Z\n"
            + "B,Stan,Smith,177,81.1,179,1971-10-12T23:24:25Z\n"
            + ",Zele,Zule,NaN,NaN,NaN,\n";
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

    // constrain numeric variable (even though knowsActualRange=false)
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "&height<162", dir, tedd.className() + "_ht162", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "category,first,last,height,weight_kg,weight_lb,time\n"
            + ",,,cm,kg,lb,UTC\n"
            + "B,Betty,Bach,161,54.2,119,1967-07-08T09:10:11Z\n";
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
            + "A,Bob,Bucher,182,83.2,183,1966-01-31T16:16:17Z\n"
            + "A,John,Johnson,191,88.5,195,1961-03-05T04:05:06Z\n"
            + "B,Betty,Bach,161,54.2,119,1967-07-08T09:10:11Z\n"
            + "B,Stan,Smith,177,81.1,179,1971-10-12T23:24:25Z\n";
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
    expected = "weight_lb\n" + "lb\n" + "119\n";
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
            + "B,Betty,Bach,161,54.2,119,1967-07-08T09:10:11Z\n";
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
            + "B,Betty,Bach,161,54.2,119,1967-07-08T09:10:11Z\n";
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
    expected = "weight_lb\n" + "lb\n" + "119\n" + "179\n" + "183\n" + "195\n" + "NaN\n";
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
        "category,weight_lb\n" + ",lb\n" + ",NaN\n" + "A,183\n" + "A,195\n" + "B,119\n" + "B,179\n";
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
            + "Betty,119\n"
            + "Bob,183\n"
            + "John,195\n"
            + "Stan,179\n"
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
    expected = "category\n" + "\n" + "\n" + "A\n" + "B\n";
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
    expected = "weight_lb\n" + "lb\n" + "119\n" + "179\n" + "183\n" + "195\n" + "NaN\n";
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
            + "A,Bucher,Bob,183\n"
            + "A,Johnson,John,195\n"
            + "B,Bach,Betty,119\n"
            + "B,Smith,Stan,179\n";
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
      Test.ensureEqual(
          msg,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "(height>1000 is outside of the variable's actual_range: 161 to 191)",
          "");
    }
  }

  /**
   * Test datasets.xml specified a non-existent variable.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testNonExistentVariable() throws Throwable {
    String2.log("\n*** EDDTableFromDatabase.testNonExistentVariable()");
    String dir = EDStatic.fullTestCacheDirectory;
    String results = "not set";
    int language = 0;
    try {
      // if there is no subsetVariables att, the dataset will be created successfully
      EDDTableFromDatabase edd =
          (EDDTableFromDatabase)
              EDDTableFromDatabase.oneFromDatasetsXml(null, "testNonExistentVariable");
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
    Test.ensureTrue(
        results.indexOf("column \"zztop\" does not exist") >= 0, "results=\n" + results);
  }

  /**
   * Test datasets.xml specified a non-existent table.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testNonExistentTable() throws Throwable {
    String2.log("\n*** EDDTableFromDatabase.testNonExistentTable()");
    String dir = EDStatic.fullTestCacheDirectory;
    String results = "not set";
    int language = 0;
    try {
      // if there is no subsetVariables att, the dataset will be created successfully
      EDDTableFromDatabase edd =
          (EDDTableFromDatabase)
              EDDTableFromDatabase.oneFromDatasetsXml(null, "testNonExistentTable");
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
        results.indexOf("relation \"myschema.zztop\" does not exist") >= 0, "results=\n" + results);
  }
}
