package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class EDDTableFromDatabaseBugTests {

  private static Connection keepAliveConn;

  @BeforeAll
  static void init() throws Exception {
    Initialization.edStatic();
    // Keep an in-memory H2 connection open for the duration of the tests
    String url = "jdbc:h2:mem:test_bug;DB_CLOSE_DELAY=-1";
    Class.forName("org.h2.Driver");
    keepAliveConn = DriverManager.getConnection(url, "sa", "");
    try (Statement stmt = keepAliveConn.createStatement()) {
      stmt.execute("CREATE TABLE mytable (id INT PRIMARY KEY, name VARCHAR(255))");
      stmt.execute("INSERT INTO mytable VALUES (1, 'Alice')");
      stmt.execute("INSERT INTO mytable VALUES (2, 'Bob')");
    }
  }

  @AfterAll
  static void destroy() throws Exception {
    if (keepAliveConn != null) {
      keepAliveConn.close();
    }
  }

  @org.junit.jupiter.api.Test
  void testEmptySelectBug() throws Throwable {
    String2.log("\n*** EDDTableFromDatabaseBugTests.testEmptySelectBug()");
    String url = "jdbc:h2:mem:test_bug;DB_CLOSE_DELAY=-1";
    String driver = "org.h2.Driver";

    String xml =
        "<dataset type=\"EDDTableFromDatabase\" datasetID=\"h2_test\" active=\"true\">\n"
            + "    <sourceUrl>"
            + url
            + "</sourceUrl>\n"
            + "    <driverName>"
            + driver
            + "</driverName>\n"
            + "    <connectionProperty name=\"user\">sa</connectionProperty>\n"
            + "    <connectionProperty name=\"password\"></connectionProperty>\n"
            + "    <addAttributes>\n"
            + "        <att name=\"title\">H2 Test</att>\n"
            + "        <att name=\"summary\">H2 Test Summary</att>\n"
            + "        <att name=\"institution\">H2 Test Institution</att>\n"
            + "        <att name=\"infoUrl\">http://example.com</att>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "    </addAttributes>\n"
            + "    <tableName>mytable</tableName>\n"
            + "    <columnNameQuotes></columnNameQuotes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>id</sourceName>\n"
            + "        <destinationName>id</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>=123</sourceName>\n"
            + "        <destinationName>fixed_var</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>";

    EDDTable tedd = (EDDTable) EDDTableFromDatabase.oneFromXmlFragment(null, xml);
    String dir = EDStatic.config.fullTestCacheDirectory;
    int language = 0;

    // Case 1: fixed_var ONLY. handleViaFixedOrSubsetVariables will handle this and return 1 row.
    TableWriterAll twa = new TableWriterAll(language, tedd, null, dir, "testFixedVarOnly");
    tedd.getDataForDapQuery(
        language, EDStatic.loggedInAsSuperuser, "/erddap/tabledap/h2_test.csv", "fixed_var", twa);
    Table table = twa.cumulativeTable();
    String results = table.saveAsCsvASCIIString();
    String expected =
        "fixed_var\n" + "\n" + "123\n"; // handleViaFixedOrSubsetVariables returns 1 row
    Test.ensureEqual(results, expected, "results=\n" + results);

    // TEST: Constraint on id (database column) but not requested.
    // resultsVariables should contain 'id', database should be queried, filtering should apply.
    // Use a constraint on id to ensure it DOES go past handleViaFixedOrSubsetVariables if it wanted
    // to,
    // but resultsVariables will still be empty for the DB query if we don't request id.
    twa = new TableWriterAll(language, tedd, null, dir, "testConstraintOnly");
    tedd.getDataForDapQuery(
        language,
        EDStatic.loggedInAsSuperuser,
        "/erddap/tabledap/h2_test.csv",
        "fixed_var&id=2",
        twa);
    table = twa.cumulativeTable();
    results = table.saveAsCsvASCIIString();
    expected = "fixed_var\n\n123\n"; // Only 1 row (for Bob, id=2)
    Test.ensureEqual(results, expected, "results=\n" + results);

    // TEST: Negative constraint
    twa = new TableWriterAll(language, tedd, null, dir, "testNegativeConstraint");
    try {
      tedd.getDataForDapQuery(
          language,
          EDStatic.loggedInAsSuperuser,
          "/erddap/tabledap/h2_test.csv",
          "fixed_var&id=100",
          twa);
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = t.getMessage();
    }
    expected = "Your query produced no matching results. (nRows = 0)";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  @org.junit.jupiter.api.Test
  void testAggregateRowsWithEmptySelectBug() throws Throwable {
    String2.log("\n*** EDDTableFromDatabaseBugTests.testAggregateRowsWithEmptySelectBug()");
    String url = "jdbc:h2:mem:test_bug;DB_CLOSE_DELAY=-1";
    String driver = "org.h2.Driver";

    String childXml =
        "<dataset type=\"EDDTableFromDatabase\" datasetID=\"h2_child\" active=\"true\">\n"
            + "    <sourceUrl>"
            + url
            + "</sourceUrl>\n"
            + "    <driverName>"
            + driver
            + "</driverName>\n"
            + "    <connectionProperty name=\"user\">sa</connectionProperty>\n"
            + "    <connectionProperty name=\"password\"></connectionProperty>\n"
            + "    <addAttributes>\n"
            + "        <att name=\"title\">H2 Child</att>\n"
            + "        <att name=\"summary\">H2 Child Summary</att>\n"
            + "        <att name=\"institution\">H2 Child Institution</att>\n"
            + "        <att name=\"infoUrl\">http://example.com</att>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "    </addAttributes>\n"
            + "    <tableName>mytable</tableName>\n"
            + "    <columnNameQuotes></columnNameQuotes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>id</sourceName>\n"
            + "        <destinationName>id</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>=123</sourceName>\n"
            + "        <destinationName>fixed_var</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>";

    String aggXml =
        "<dataset type=\"EDDTableAggregateRows\" datasetID=\"h2_agg\" active=\"true\">\n"
            + "    <addAttributes>\n"
            + "        <att name=\"title\">H2 Agg</att>\n"
            + "        <att name=\"summary\">H2 Agg Summary</att>\n"
            + "        <att name=\"institution\">H2 Agg Institution</att>\n"
            + "        <att name=\"infoUrl\">http://example.com</att>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "    </addAttributes>\n"
            + childXml
            + "</dataset>";

    EDDTable tedd = (EDDTable) EDDTableAggregateRows.oneFromXmlFragment(null, aggXml);
    String dir = EDStatic.config.fullTestCacheDirectory;
    int language = 0;

    String query = "fixed_var&id=1";
    String tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, "testAggregateRowsWithEmptySelectBug", ".csv");
    String results = File2.directReadFrom88591File(dir + tName);
    String expected = "fixed_var\n" + "\n" + "123\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
