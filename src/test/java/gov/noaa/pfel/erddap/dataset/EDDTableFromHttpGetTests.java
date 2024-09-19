package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import tags.TagFlaky;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromHttpGetTests {

  @TempDir private static Path TEMP_DIR;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testStaticMethods() throws Throwable {
    testStatic(-1);
  }

  /**
   * This tests the static methods in this class.
   *
   * @param hammer If hammer&lt;0, this just runs the static tests 1 time. If hammer&gt;0, this
   *     doesn't delete existing files and makes 10000 insertions with data values =hammer. If
   *     hammer==0, this prints the hammer'd data file -- raw, no processing. If hammer==3,
   *     repeatedly checks the hammer'd data file to ensure it ends with ']'.
   */
  private static void testStatic(int hammer) throws Throwable {
    String results, expected;
    int language = 0;

    // test parseDirectoryStructure
    StringArray dsColumnName = new StringArray();
    IntArray dsN = new IntArray();
    IntArray dsCalendar = new IntArray();
    EDDTableFromHttpGet.parseHttpGetDirectoryStructure(
        "5years/3MonTH/4DayS/5hr/6min/7sec/100millis/stationID", dsColumnName, dsN, dsCalendar);
    Test.ensureEqual(dsColumnName.toString(), ", , , , , , , stationID", "");
    Test.ensureEqual(dsN.toString(), "5, 3, 4, 5, 6, 7, 100, -1", "");
    Test.ensureEqual(
        dsCalendar.toString(),
        String2.toCSSVString(
            new int[] {
              Calendar.YEAR,
              Calendar.MONTH,
              Calendar.DATE,
              Calendar.HOUR_OF_DAY,
              Calendar.MINUTE,
              Calendar.SECOND,
              Calendar.MILLISECOND,
              -1
            }),
        "");

    // test whichFile
    Test.ensureEqual(
        EDDTableFromHttpGet.whichFile(
            "/ab/",
            dsColumnName,
            dsN,
            dsCalendar,
            new String[] {"junk", "stationID"}, // sourceNames
            // row0 row1
            new PrimitiveArray[] {
              new StringArray(new String[] {"junk0", "junk1"}), // junk sourceValues
              new StringArray(new String[] {"12345", "46088"})
            }, // stationID sourceValues
            1, // row
            Calendar2.isoStringToEpochSeconds("2016-06-21T14:15:16.789")),
        "/ab/2015/2016-04/2016-06-20/2016-06-21T10/2016-06-21T14-12/"
            + "2016-06-21T14-15-14/2016-06-21T14-15-16.700/"
            + "2015_2016-04_2016-06-20_2016-06-21T10_2016-06-21T14-12_"
            + "2016-06-21T14-15-14_2016-06-21T14-15-16.700_46088.jsonl",
        "");

    // set up
    String startDir = TEMP_DIR.toAbsolutePath().toString() + "/";
    if (hammer < 0)
      File2.deleteAllFiles(startDir, true, true); // recursive, deleteEmptySubdirectories
    EDDTableFromHttpGet.parseHttpGetDirectoryStructure(
        "stationID/2months", dsColumnName, dsN, dsCalendar);
    HashSet<String> keys = new HashSet();
    keys.add("bsimons_aSecret");
    Attributes tGlobalAttributes =
        new Attributes()
            .add("Conventions", "CF-1.6, COARDS, ACDD-1.3")
            .add("creator_name", "Bob Simons")
            .add("title", "Test EDDTableFromHttpGet");
    String columnNames[] = {
      "stationID",
      "time",
      "aByte",
      "aChar",
      "aShort",
      "anInt",
      "aFloat",
      "aDouble",
      "aString",
      "=123",
      EDDTableFromHttpGet.TIMESTAMP,
      EDDTableFromHttpGet.AUTHOR,
      EDDTableFromHttpGet.COMMAND
    };
    StringArray columnNamesSA = new StringArray(columnNames);
    String columnUnits[] = {
      "",
      "minutes since 1980-01-01",
      "",
      "",
      "m",
      "days since 1985-01-01",
      "degree_C",
      EDV.TIME_UNITS,
      null,
      "m.s-1",
      Calendar2.SECONDS_SINCE_1970,
      null,
      null
    };
    PAType columnPATypes[] = {
      PAType.STRING,
      PAType.DOUBLE,
      PAType.BYTE,
      PAType.CHAR,
      PAType.SHORT,
      PAType.INT,
      PAType.FLOAT,
      PAType.DOUBLE,
      PAType.STRING,
      PAType.INT,
      PAType.DOUBLE,
      PAType.STRING,
      PAType.BYTE
    };
    int nCol = columnNames.length;
    PrimitiveArray columnMvFv[] = new PrimitiveArray[nCol];
    String columnTypes[] = new String[nCol];
    for (int col = 0; col < nCol; col++)
      columnTypes[col] = PAType.toCohortString(columnPATypes[col]);
    String requiredVariableNames[] = {"stationID", "time"};
    String requiredVariableTypes[] = {"String", "double"};

    Table table;

    // *** read the hammer data
    if (hammer == 0) {
      // test the read time
      String name = startDir + "46088/46088_1980-01.jsonl";
      table =
          EDDTableFromHttpGet.readFile(
              name,
              columnNamesSA,
              columnTypes,
              requiredVariableNames,
              requiredVariableTypes,
              true,
              Double.NaN);

      File2.copy(name, name + ".txt");
      Test.displayInBrowser("file://" + name + ".txt");
      return;
    }

    // *** read the hammer data file repeatedly for 10 seconds to ensure last char
    // is ']'
    if (hammer == 3) {

      // test the read time
      String name = startDir + "46088/46088_1980-01.jsonl";
      long start = System.currentTimeMillis();
      int count = 0;
      while (System.currentTimeMillis() - start < 20000) {
        count++;
        String text = File2.directReadFromUtf8File(name);
        text = text.substring(Math.max(0, text.length() - 80)).trim();
        if (text.endsWith("]")) String2.log("The file ends with ']' as expected.");
        else throw new RuntimeException("Unexpected end of file:\n..." + text);
      }
      String2.log("All " + count + " tests passed.");
      return;
    }

    // *** hammer the system with inserts
    if (hammer > 0) {
      long time = System.currentTimeMillis();
      int n = 20000;
      String tHammer = "" + hammer;
      if (hammer >= 10)
        tHammer = "[" + PrimitiveArray.factory(PAType.INT, hammer, "" + hammer).toCSVString() + "]";
      String2.log(">> tHammer=" + tHammer);
      for (int i = 0; i < n; i++) {
        // error will throw exception
        results =
            EDDTableFromHttpGet.insertOrDelete(
                language,
                startDir,
                dsColumnName,
                dsN,
                dsCalendar,
                keys,
                tGlobalAttributes,
                columnNames,
                columnUnits,
                columnPATypes,
                columnMvFv,
                requiredVariableNames,
                EDDTableFromHttpGet.INSERT_COMMAND,
                "stationID=\"46088\"&time="
                    + hammer
                    + "&aByte="
                    + tHammer
                    + "&aChar="
                    + (char) 65
                    + "&aShort="
                    + tHammer
                    + "&anInt="
                    + tHammer
                    + "&aFloat="
                    + tHammer
                    + "&aDouble="
                    + tHammer
                    + "&aString="
                    + tHammer
                    + "&author=bsimons_aSecret",
                null,
                null); // Table dirTable, Table fileTable
        if (i == 0) String2.log(">> results=" + results);
      }
      String2.log(
          "\n*** hammer("
              + hammer
              + ") n="
              + n
              + " finished successfully. Avg time="
              + ((System.currentTimeMillis() - time) / (n + 0.0))
              + "ms");
      return;
    }

    // ****** from here on are the non-hammer, static tests
    // *** test insertOrDelete
    String2.log("\n>> insertOrDelete #1: insert 1 row");
    results =
        EDDTableFromHttpGet.insertOrDelete(
            language,
            startDir,
            dsColumnName,
            dsN,
            dsCalendar,
            keys,
            tGlobalAttributes,
            columnNames,
            columnUnits,
            columnPATypes,
            columnMvFv,
            requiredVariableNames,
            EDDTableFromHttpGet.INSERT_COMMAND,
            "stationID=\"46088\"&time=3.3&aByte=17.1&aChar=g"
                + "&aShort=30000.1&anInt=2&aFloat=1.23"
                + "&aDouble=1.2345678901234&aString=\"abcdefghijkl\""
                + // string is nBytes long
                "&author=bsimons_aSecret",
            null,
            null); // Table dirTable, Table fileTable
    Test.ensureLinesMatch(results, EDDTableFromHttpGet.resultsRegex(1), "results=" + results);
    double timestamp1 = EDDTableFromHttpGet.extractTimestamp(results);
    String2.log(">> results=" + results + ">> timestamp1=" + timestamp1);

    // read the data
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            Double.NaN);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl,"
            + timestamp1
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    Math2.sleep(1000);

    // *** add 2 rows via array
    String2.log("\n>> insertOrDelete #2: insert 2 rows via array");
    results =
        EDDTableFromHttpGet.insertOrDelete(
            language,
            startDir,
            dsColumnName,
            dsN,
            dsCalendar,
            keys,
            tGlobalAttributes,
            columnNames,
            columnUnits,
            columnPATypes,
            columnMvFv,
            requiredVariableNames,
            EDDTableFromHttpGet.INSERT_COMMAND,
            "stationID=\"46088\"&time=[4.4,5.5]&aByte=[18.2,18.8]&aChar=[\"\u20AC\",\" \"]"
                + // unicode char
                "&aShort=[30002.2,30003.3]&anInt=3&aFloat=[1.45,1.67]"
                + "&aDouble=[1.3,1.4]&aString=[\" s\n\tÃ\u20AC123\",\" \\n\\u20AC \"]"
                + // string is nBytes long, unicode
                // char
                "&author=bsimons_aSecret",
            null,
            null); // Table dirTable, Table fileTable
    Test.ensureLinesMatch(results, EDDTableFromHttpGet.resultsRegex(2), "results=" + results);
    double timestamp2 = EDDTableFromHttpGet.extractTimestamp(results);
    String2.log(">> results=" + results + ">> timestamp2=" + timestamp2);

    // read the data
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            System.currentTimeMillis() / 1000.0);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl,"
            + timestamp1
            + ",bsimons,0\n"
            + "46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\","
            + timestamp2
            + ",bsimons,0\n"
            + "46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \","
            + timestamp2
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // read the data with timestamp from first insert
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            timestamp1);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl,"
            + timestamp1
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // *** change all values in a row
    String2.log("\n>> insertOrDelete #3: change all values in a row");
    results =
        EDDTableFromHttpGet.insertOrDelete(
            language,
            startDir,
            dsColumnName,
            dsN,
            dsCalendar,
            keys,
            tGlobalAttributes,
            columnNames,
            columnUnits,
            columnPATypes,
            columnMvFv,
            requiredVariableNames,
            EDDTableFromHttpGet.INSERT_COMMAND,
            "stationID=46088&time=3.3&aByte=19.9&aChar=¼"
                + // stationID not in quotes //the character itself
                "&aShort=30009.9&anInt=9&aFloat=1.99"
                + "&aDouble=1.999&aString=\"\""
                + // empty string
                "&author=\"bsimons_aSecret\"", // author in quotes
            null,
            null); // Table dirTable, Table fileTable
    Test.ensureLinesMatch(results, EDDTableFromHttpGet.resultsRegex(1), "results=" + results);
    double timestamp3 = EDDTableFromHttpGet.extractTimestamp(results);
    String2.log(">> results=" + results + ">> timestamp3=" + timestamp3);

    // read the data
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            System.currentTimeMillis() / 1000.0);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,3.3,20,\\u00bc,30010,9,1.99,1.999,,"
            + timestamp3
            + ",bsimons,0\n"
            + "46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\","
            + timestamp2
            + ",bsimons,0\n"
            + "46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \","
            + timestamp2
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // *** change values in a row but only specify a few
    String2.log("\n>> insertOrDelete #4: change a few values in a row");
    results =
        EDDTableFromHttpGet.insertOrDelete(
            language,
            startDir,
            dsColumnName,
            dsN,
            dsCalendar,
            keys,
            tGlobalAttributes,
            columnNames,
            columnUnits,
            columnPATypes,
            columnMvFv,
            requiredVariableNames,
            EDDTableFromHttpGet.INSERT_COMMAND,
            "stationID=\"46088\"&time=3.3&aByte=29.9&aChar=\" \"" + "&author=bsimons_aSecret",
            null,
            null); // Table dirTable, Table fileTable
    Test.ensureLinesMatch(results, EDDTableFromHttpGet.resultsRegex(1), "results=" + results);
    double timestamp4 = EDDTableFromHttpGet.extractTimestamp(results);
    String2.log(">> results=" + results + ">> timestamp3=" + timestamp4);

    // read the data
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            System.currentTimeMillis() / 1000.0);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,3.3,30,\" \",,,,,,"
            + timestamp4
            + ",bsimons,0\n"
            + // only low byt kept
            "46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\","
            + timestamp2
            + ",bsimons,0\n"
            + "46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \","
            + timestamp2
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // *** delete a row
    String2.log("\n>> EDDTableFromHttpGet.insertOrDelete #4: delete a row");
    results =
        EDDTableFromHttpGet.insertOrDelete(
            language,
            startDir,
            dsColumnName,
            dsN,
            dsCalendar,
            keys,
            tGlobalAttributes,
            columnNames,
            columnUnits,
            columnPATypes,
            columnMvFv,
            requiredVariableNames,
            EDDTableFromHttpGet.DELETE_COMMAND,
            "stationID=\"46088\"&time=3.3" + "&author=bsimons_aSecret",
            null,
            null); // Table dirTable, Table fileTable
    Test.ensureLinesMatch(results, EDDTableFromHttpGet.resultsRegex(1), "results=" + results);
    double timestamp5 = EDDTableFromHttpGet.extractTimestamp(results);
    String2.log(">> results=" + results + ">> timestamp3=" + timestamp4);

    // read the data
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            System.currentTimeMillis() / 1000.0);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\","
            + timestamp2
            + ",bsimons,0\n"
            + "46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \","
            + timestamp2
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // read the data with timestamp from first insert
    table =
        EDDTableFromHttpGet.readFile(
            startDir + "46088/46088_1980-01.jsonl",
            columnNamesSA,
            columnTypes,
            requiredVariableNames,
            requiredVariableTypes,
            true,
            timestamp1);
    results = table.dataToString();
    expected =
        "stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n"
            + "46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl,"
            + timestamp1
            + ",bsimons,0\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // *** test errors
    String2.log("\n>> insertOrDelete #5: expected errors");

    results = "invalid author";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A"
                  + "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&aDouble=1.999&aString=\"a\""
                  + "&author=zzsimons_aSecret", // zz
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results, "com.cohort.util.SimpleException: Query error: Invalid author_key.", "");

    results = "author not last";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A"
                  + "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&author=bsimons_aSecret"
                  + "&aDouble=1.999&aString=\"a\"",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: author= must be the last parameter.",
        "");

    results = "invalid secret";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A"
                  + "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecretzz", // zz
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results, "com.cohort.util.SimpleException: Query error: Invalid author_key.", "");

    results = "invalid var name";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A"
                  + "&aShort=30009.9&anInt=9&aFloatzz=1.99"
                  + // zz
                  "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecret",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Unknown variable name=aFloatzz",
        "");

    results = "missing required var";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "time=3.3&aByte=19.9&aChar=A"
                  + // no stationID
                  "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecret",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: requiredVariableName=stationID wasn't specified.",
        "");

    results = "invalid required var value";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"\"&time=3.3&aByte=19.9&aChar=A"
                  + // ""
                  "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecret",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: requiredVariable=stationID must have a valid value.",
        "");

    results = "invalid time value";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=1e14&aByte=19.9&aChar=A"
                  + // time is invalid
                  "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecret",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: "
            + "ERROR in httpGetDirectoryStructure part#1: invalid time value (timeEpSec=6.0000003155328E15)!",
        "");

    results = "different array sizes";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A"
                  + "&aShort=30009.9&anInt=[9,10]&aFloat=[1.99,2.99,3.99]"
                  + // 2 and 3
                  "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecret",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Different parameters with arrays have different sizes: 2!=3.",
        "");

    results = "2 vars with same name";
    try {
      results =
          EDDTableFromHttpGet.insertOrDelete(
              language,
              startDir,
              dsColumnName,
              dsN,
              dsCalendar,
              keys,
              tGlobalAttributes,
              columnNames,
              columnUnits,
              columnPATypes,
              columnMvFv,
              requiredVariableNames,
              EDDTableFromHttpGet.INSERT_COMMAND,
              "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A"
                  + "&aShort=30009.9&anInt=9&aFloat=1.99"
                  + "&aShort=30009.9"
                  + // duplicate
                  "&aDouble=1.999&aString=\"a\""
                  + "&author=bsimons_aSecret",
              null,
              null); // Table dirTable, Table fileTable
      results = "shouldn't get here";
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: There are two parameters with variable name=aShort.",
        "");
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String dataDir =
        Path.of(EDDTestDataset.class.getResource("/data/points/testFromHttpGet/").toURI())
                .toString()
            + "/";
    String sampleFile = dataDir + "testFromHttpGet.jsonl";

    String results =
        EDDTableFromHttpGet.generateDatasetsXml(
                language,
                dataDir,
                sampleFile,
                "stationID, time",
                "stationID/2months",
                "JohnSmith_JohnSmithKey, HOBOLogger_HOBOLoggerKey, QCScript59_QCScript59Key",
                "https://erddap.github.io/setupDatasetsXml.html",
                "NOAA NMFS SWFSC ERD",
                "This is my great summary.",
                "My Great Title",
                null)
            + "\n";

    // String2.log(results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromHttpGet",
                  dataDir,
                  sampleFile,
                  "stationID, time",
                  "stationID/2months",
                  "JohnSmith_JohnSmithKey, HOBOLogger_HOBOLoggerKey, QCScript59_QCScript59Key",
                  "https://erddap.github.io/setupDatasetsXml.html",
                  "NOAA NMFS SWFSC ERD",
                  "This is my great summary.",
                  "My Great Title"
                },
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String tDatasetID =
        EDDTableFromHttpGet.suggestDatasetID(dataDir + ".*|.jsonl_EDDTableFromHttpGet");
    String expected =
        "<!-- NOTE! Since JSON Lines CSV files have no metadata, you MUST edit the chunk\n"
            + "  of datasets.xml below to add all of the metadata (especially \"units\"). -->\n"
            + "<dataset type=\"EDDTableFromHttpGet\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
            + "    <fileDir>"
            + dataDir
            + "</fileDir>\n"
            + "    <fileNameRegex>.*\\.jsonl</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <sortedColumnSourceName></sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>stationID, time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
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
            + "        <att name=\"httpGetDirectoryStructure\">stationID/2months</att>\n"
            + "        <att name=\"httpGetKeys\">JohnSmith_JohnSmithKey, HOBOLogger_HOBOLoggerKey, QCScript59_QCScript59Key</att>\n"
            + "        <att name=\"httpGetRequiredVariables\">stationID, time</att>\n"
            + "        <att name=\"infoUrl\">https://erddap.github.io/setupDatasetsXml.html</att>\n"
            + "        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"keywords\">air, airTemp, author, center, command, data, erd, fisheries, great, identifier, marine, national, nmfs, noaa, science, service, southwest, station, stationID, swfsc, temperature, time, timestamp, title, water, waterTemp</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">stationID</att>\n"
            + "        <att name=\"summary\">This is my great summary. NOAA National Marine Fisheries Service (NMFS) Southwest Fisheries Science Center (SWFSC) ERD data from a local source."
            + "\n\nNOTE! This is an unusual dataset in that the data files are actually log files. Normally, when you request data from this dataset, ERDDAP processes the insert (command=0) and delete (command=1) commands in the log files to return data from the current version of this dataset. However, if you make a request which includes &amp;timestamp&lt;= , then ERDDAP will return the dataset as it was at that point in time. Or, if you make a request which includes &amp;timestamp&gt; (or &gt;= or =), e.g., &amp;timestamp&gt;0, then ERDDAP will return the raw data from the log files.</att>\n"
            + "        <att name=\"testOutOfDate\">now-1day</att>\n"
            + "        <att name=\"title\">My Great Title</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>stationID</sourceName>\n"
            + "        <destinationName>stationID</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Station ID</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n"
            + "            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss&#39;Z&#39;</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>airTemp</sourceName>\n"
            + "        <destinationName>airTemp</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Air Temp</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">NaN</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>waterTemp</sourceName>\n"
            + "        <destinationName>waterTemp</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Water Temp</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">NaN</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>timestamp</sourceName>\n"
            + "        <destinationName>timestamp</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"comment\">The values in this column are added by ERDDAP to identify when each row of data was added to the data file. NOTE! This is an unusual dataset in that the data files are actually log files. Normally, when you request data from this dataset, ERDDAP processes the insert (command=0) and delete (command=1) commands in the log files to return data from the current version of this dataset. However, if you make a request which includes &amp;timestamp&lt;= , then ERDDAP will return the dataset as it was at that point in time. Or, if you make a request which includes &amp;timestamp&gt; (or &gt;= or =), e.g., &amp;timestamp&gt;0, then ERDDAP will return the raw data from the log files.</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Timestamp</att>\n"
            + "            <att name=\"missing_value\" type=\"double\">NaN</att>\n"
            + "            <att name=\"time_precision\">1970-01-01T00:00:00.000Z</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>author</sourceName>\n"
            + "        <destinationName>author</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"comment\">The values in this column identify the author who added each row of data to the dataset.</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Author</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>command</sourceName>\n"
            + "        <destinationName>command</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"comment\">This is an unusual dataset in that the data files are actually log files. Normally, when you request data from this dataset, ERDDAP processes the insert (command=0) and delete (command=1) commands in the log files to return data from the current version of this dataset. However, if you make a request which includes &amp;timestamp&lt;= , then ERDDAP will return the dataset as it was at that point in time. Or, if you make a request which includes &amp;timestamp&gt; (or &gt;= or =), e.g., &amp;timestamp&gt;0, then ERDDAP will return the raw data from the log files.</att>\n"
            + "            <att name=\"flag_meanings\">insert delete</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Command</att>\n"
            + "            <att name=\"missing_value\" type=\"byte\">127</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // String tDatasetID = "testFromHttpGet_dc0c_4e18_f4ef";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    // delete the data files (but not the seed data file)
    File2.deleteAllFiles(dataDir + "station1", true, true);
    File2.deleteAllFiles(dataDir + "station2", true, true);

    EDD edd = EDDTableFromHttpGet.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "My Great Title", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "stationID, time, airTemp, waterTemp, timestamp, author, command",
        "");
  }

  /**
   * This does basic tests of this class. Note that ü in utf-8 is \xC3\xBC or [195][188] Note that
   * Euro is \\u20ac (and low byte is #172 is \\u00ac -- I worked to encode as '?')
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagFlaky
  void testBasic() throws Throwable {
    // String2.log("\n****************** EDDTableFromHttpGet.testBasic()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dataDir =
        Path.of(EDDTestDataset.class.getResource("/data/points/testFromHttpGet/").toURI())
            .toString();
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    // boolean oReallyVerbose = reallyVerbose;
    // reallyVerbose = true;

    String id = "testFromHttpGet";
    EDDTableFromHttpGet.deleteCachedDatasetInfo(id);
    // delete the data files (but not the seed data file)
    File2.deleteAllFiles(dataDir + "station1", true, true);
    File2.deleteAllFiles(dataDir + "station2", true, true);
    File2.delete(dataDir + "station1");
    File2.delete(dataDir + "station2");
    // String2.pressEnterToContinue();

    EDDTableFromHttpGet eddTable = (EDDTableFromHttpGet) EDDTestDataset.gettestFromHttpGet();

    // *** test getting das for entire dataset
    String2.log("\n*** EDDTableFromHttpGet.testBasic  test das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  stationID {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.461888e+9, 1.529946e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String time_precision \"1970-01-01T00:00:00Z\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 10.2, 10.2;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    Float64 missing_value NaN;\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -150.3, -150.3;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    Float64 missing_value NaN;\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  airTemp {\n"
            + "    Float32 actual_range 10.2, 22.1;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Air Temp\";\n"
            + "    Float32 missing_value NaN;\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  waterTemp {\n"
            + "    Float32 actual_range 11.2, 23.1;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Water Temp\";\n"
            + "    Float32 missing_value NaN;\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  timestamp {\n"
            + "    Float64 actual_range 0.0, 1.531153327766e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Timestamp\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String time_precision \"1970-01-01T00:00:00.000Z\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  author {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Author\";\n"
            + "  }\n"
            + "  command {\n"
            + "    String _Unsigned \"false\";\n"
            + "    Byte actual_range 0, 0;\n"
            + "    String flag_meanings \"insert delete\";\n"
            + "    Byte flag_values 0, 1;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Command\";\n"
            + "    Byte missing_value 127;\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"stationID, latitude, longitude\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"erd.data@noaa.gov\";\n"
            + "    String creator_name \"NOAA NMFS SWFSC ERD\";\n"
            + "    String creator_type \"institution\";\n"
            + "    String creator_url \"https://www.pfeg.noaa.gov\";\n"
            + "    Float64 Easternmost_Easting -150.3;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 10.2;\n"
            + "    Float64 geospatial_lat_min 10.2;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -150.3;\n"
            + "    Float64 geospatial_lon_min -150.3;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "    String httpGetDirectoryStructure \"stationID/2months\";\n"
            + "    String httpGetRequiredVariables \"stationID, time\";\n"
            + "    String infoUrl \"https://erddap.github.io/setupDatasetsXml.html\";\n"
            + "    String institution \"NOAA NMFS SWFSC ERD\";\n"
            + "    String keywords \"air, airTemp, author, center, command, data, erd, fisheries, great, identifier, latitude, longitude, marine, national, nmfs, noaa, science, service, southwest, station, stationID, swfsc, temperature, time, timestamp, title, water, waterTemp\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 10.2;\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 10.2;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"stationID, longitude, latitude\";\n"
            + "    String summary \"This is my great summary. NOAA National Marine Fisheries Service (NMFS) Southwest Fisheries Science Center (SWFSC) ERD data from a local source.\";\n"
            + "    String testOutOfDate \"now-1day\";\n"
            + "    String time_coverage_end \"2018-06-25T17:00:00Z\";\n"
            + "    String time_coverage_start \"2016-04-29T00:00:00Z\";\n"
            + "    String title \"My Great Title\";\n"
            + "    Float64 Westernmost_Easting -150.3;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String stationID;\n"
            + "    Float64 time;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    Float32 airTemp;\n"
            + "    Float32 waterTemp;\n"
            + "    Float64 timestamp;\n"
            + "    String author;\n"
            + "    Byte command;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n*** EDDTableFromHttpGet.testBasic make DATA FILES\n");

    // .csv all data (just the seed data)
    userDapQuery = "stationID,time,latitude,longitude,airTemp,waterTemp,author,command";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "stationID,time,latitude,longitude,airTemp,waterTemp,author,command\n"
            + ",UTC,degrees_north,degrees_east,degree_C,degree_C,,\n"
            + "myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,me,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,JohnSmith,0\n"
            + //
            "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,JohnSmith,0\n"
            + //
            "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,JohnSmith,0\n"
            + //
            "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,JohnSmith,0\n"
            + //
            "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,JohnSmith,0\n"
            + //
            "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,JohnSmith,0\n"
            + //
            "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,JohnSmith,0\n"
            + //
            "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,JohnSmith,0\n"
            + //
            "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,JohnSmith,0\n"
            + //
            "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,JohnSmith,0\n"
            + //
            "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,JohnSmith,0\n"
            + //
            "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,JohnSmith,0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // initial file table
    Table tFileTable =
        eddTable.tryToLoadDirFileTable(
            EDDTableFromHttpGet.datasetDir(id) + EDDTableFromHttpGet.FILE_TABLE_FILENAME);
    tFileTable.removeColumn("lastMod");
    results = tFileTable.dataToString();
    String2.log(results);
    expected =
        "dirIndex,fileName,size,sortedSpacing,stationID_min_,stationID_max_,stationID_hasNaN_,time_min_,time_max_,time_hasNaN_,x3d10x2e2_min_,x3d10x2e2_max_,x3d10x2e2_hasNaN_,x3dx2d150x2e3_min_,x3dx2d150x2e3_max_,x3dx2d150x2e3_hasNaN_,airTemp_min_,airTemp_max_,airTemp_hasNaN_,waterTemp_min_,waterTemp_max_,waterTemp_hasNaN_,timestamp_min_,timestamp_max_,timestamp_hasNaN_,author_min_,author_max_,author_hasNaN_,command_min_,command_max_,command_hasNaN_\n"
            + "0,testFromHttpGet.jsonl,144,-1.0,myStation,myStation,0,2018-06-25T17:00:00Z,2018-06-25T17:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.2,14.2,0,12.2,12.2,0,0.0,0.0,0,me,me,0,0,0,0\n"
            + "1,station1_2016-03.jsonl,528,-1.0,station1,station1,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,10.2,20.1,0,11.2,21.1,0,1.531153318778E9,1.531153326823E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "1,station1_2016-05.jsonl,528,-1.0,station1,station1,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.2,22.1,0,15.2,23.1,0,1.531153320277E9,1.531153327766E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "2,station2_2016-03.jsonl,527,-1.0,station2,station2,0,2016-04-29T02:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,12.2,12.3,0,13.2,13.3,0,1.53115331948E9,1.53115332259E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "2,station2_2016-05.jsonl,532,-1.0,station2,station2,0,2016-05-29T02:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,16.2,16.3,0,17.2,17.3,0,1.531153321058E9,1.531153324151E9,0,JohnSmith,JohnSmith,0,0,0,0\n";
    Test.ensureLinesMatch(results, expected, "");

    EDStatic.developmentMode = true;
    // push a bunch of data into the dataset: 2 stations, 2 time periods
    for (int i = 0; i < 4; i++) {
      // apr
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station1&time=2016-04-29T0"
                  + i
                  + ":00:00Z"
                  + "&airTemp=10."
                  + i
                  + "&waterTemp=11."
                  + i
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_insert1_" + i,
              ".insert");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station2&time=2016-04-29T0"
                  + i
                  + ":00:00Z"
                  + "&airTemp=12."
                  + i
                  + "&waterTemp=13."
                  + i
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_insert2_" + i,
              ".insert");

      // may
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station1&time=2016-05-29T0"
                  + i
                  + ":00:00Z"
                  + "&airTemp=14."
                  + i
                  + "&waterTemp=15."
                  + i
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_insert3_" + i,
              ".insert");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station2&time=2016-05-29T0"
                  + i
                  + ":00:00Z"
                  + "&airTemp=16."
                  + i
                  + "&waterTemp=17."
                  + i
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_insert4_" + i,
              ".insert");
    }
    EDStatic.developmentMode = false;

    // look at last response file
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        "\\{\n"
            + "\"status\":\"success\",\n"
            + "\"nRowsReceived\":1,\n"
            + "\"stringTimestamp\":\""
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z\",\n"
            + "\"numericTimestamp\":1\\.\\d{10,12}+E9\n"
            + // possible but unlikely that millis=0 by chance; if so, try again
            "\\}\n";
    Test.ensureLinesMatch(
        results, expected,
        ""); // sometimes fails if right at change of hour (to "today" is previous
    // hour)

    tFileTable =
        eddTable.tryToLoadDirFileTable(
            EDDTableFromHttpGet.datasetDir(id) + EDDTableFromHttpGet.FILE_TABLE_FILENAME);
    results = tFileTable.dataToString();
    String2.log(results);
    expected =
        "dirIndex,fileName,lastMod,size,sortedSpacing,stationID_min_,stationID_max_,stationID_hasNaN_,time_min_,time_max_,time_hasNaN_,x3d10x2e2_min_,x3d10x2e2_max_,x3d10x2e2_hasNaN_,x3dx2d150x2e3_min_,x3dx2d150x2e3_max_,x3dx2d150x2e3_hasNaN_,airTemp_min_,airTemp_max_,airTemp_hasNaN_,waterTemp_min_,waterTemp_max_,waterTemp_hasNaN_,timestamp_min_,timestamp_max_,timestamp_hasNaN_,author_min_,author_max_,author_hasNaN_,command_min_,command_max_,command_hasNaN_\n"
            +
            // "0,testFromHttpGet.jsonl,15\\d+,144,-1.0,myStation,myStation,0,2018-06-25T17:00:00Z,2018-06-25T17:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.2,14.2,0,12.2,12.2,0,0.0,0.0,0,me,me,0,0,0,0\n"
            // +
            // "1,station1_2016-03.jsonl,1\\d+,37\\d,1.0,station1,station1,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,10.0,10.3,0,11.0,11.3,0,1.6\\d+E9,1.6\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            // +
            // "2,station2_2016-03.jsonl,1\\d+,37\\d,1.0,station2,station2,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,12.0,12.3,0,13.0,13.3,0,1.6\\d+E9,1.6\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            // +
            // "1,station1_2016-05.jsonl,1\\d+,37\\d,1.0,station1,station1,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.0,14.3,0,15.0,15.3,0,1.6\\d+E9,1.6\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            // +
            // "2,station2_2016-05.jsonl,1\\d+,37\\d,1.0,station2,station2,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,16.0,16.3,0,17.0,17.3,0,1.6\\d+E9,1.6\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            // +
            "0,testFromHttpGet.jsonl,15\\d+,144,-1.0,myStation,myStation,0,2018-06-25T17:00:00Z,2018-06-25T17:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.2,14.2,0,12.2,12.2,0,0.0,0.0,0,me,me,0,0,0,0\n"
            + //
            "1,station1_2016-03.jsonl,15\\d+,528,-1.0,station1,station1,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,10.2,20.1,0,11.2,21.1,0,1.531153318778E9,1.531153326823E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "1,station1_2016-05.jsonl,15\\d+,528,-1.0,station1,station1,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.2,22.1,0,15.2,23.1,0,1.531153320277E9,1.531153327766E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "2,station2_2016-03.jsonl,15\\d+,527,-1.0,station2,station2,0,2016-04-29T02:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,12.2,12.3,0,13.2,13.3,0,1.53115331948E9,1.53115332259E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "2,station2_2016-05.jsonl,15\\d+,532,-1.0,station2,station2,0,2016-05-29T02:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,16.2,16.3,0,17.2,17.3,0,1.531153321058E9,1.531153324151E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "3,station1_2016-03.jsonl,17\\d+,8\\d+,1.0,station1,station1,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,10.0,10.3,0,11.0,11.3,0,1.7\\d+E9,1.7\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "4,station2_2016-03.jsonl,17\\d+,8\\d+,1.0,station2,station2,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,12.0,12.3,0,13.0,13.3,0,1.7\\d+E9,1.7\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "3,station1_2016-05.jsonl,17\\d+,8\\d+,1.0,station1,station1,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.0,14.3,0,15.0,15.3,0,1.7\\d+E9,1.7\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n"
            + //
            "4,station2_2016-05.jsonl,17\\d+,8\\d+,1.0,station2,station2,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,16.0,16.3,0,17.0,17.3,0,1.7\\d+E9,1.7\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n";
    Test.ensureLinesMatch(results, expected, "");

    // .csv all data
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    long versioningTime = System.currentTimeMillis();
    Math2.sleep(1);
    String versioningExpected =
        "stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n"
            + ",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n"
            + "myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z,me,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n"
            + "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,"
            + today
            + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n";
    Test.ensureLinesMatch(results, versioningExpected, "\nresults=\n" + results);

    EDStatic.developmentMode = true;
    // overwrite and delete a bunch of data
    for (int i = 0; i < 2; i++) {
      // overwrite the first 2
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station1&time=2016-04-29T0"
                  + i
                  + ":00:00Z"
                  + "&airTemp=20."
                  + i
                  + "&waterTemp=21."
                  + i
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_insert5_" + i,
              ".insert");
      // delete the first 2
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station2&time=2016-04-29T0"
                  + i
                  + ":00:00Z"
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_delete6_" + i,
              ".delete");

      // overwrite the first 2
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station1&time=2016-05-29T0"
                  + i
                  + ":00:00Z"
                  + "&airTemp=22."
                  + i
                  + "&waterTemp=23."
                  + i
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_insert7_" + i,
              ".insert");
      // delete the first 2
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "stationID=station2&time=2016-05-29T0"
                  + i
                  + ":00:00Z"
                  + "&author=JohnSmith_JohnSmithKey",
              dir,
              eddTable.className() + "_delete8_" + i,
              ".delete");
    }
    EDStatic.developmentMode = false;

    // .csv all data (with processing)
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n"
            + ",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n"
            + "myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z,me,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n"
            + // changed
            "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n"
            + // changed
            "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n"
            + // hours 00 01 deleted
            "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n"
            + // hours 00 01 deleted
            "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n"
            + // changed
            "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n"
            + // hours 00 01 deleted
            "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n"
            + // changed
            "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n"
            + // hours 00 01 deleted
            "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n";
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // similar: versioning as of now
    userDapQuery = "&timestamp<=" + (System.currentTimeMillis() / 1000.0); // as millis
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all3b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // similar: versioning as of now
    userDapQuery =
        "&timestamp<="
            + Calendar2.epochSecondsToIsoStringT3Z(System.currentTimeMillis() / 1000.0); // as ISO
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all3c", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // .csv all data (without processing)
    userDapQuery = "&timestamp>-1";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log("dataset without processing:\n" + results);
    /*
     * without neutered timestamps (neutered because they change each time):
     * stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command
     * ,UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,
     * myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z
     * ,me,0
     * station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,2020-10-09T19:12:43.819Z,
     * JohnSmith,0
     * station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,2020-10-09T19:12:46.325Z,
     * JohnSmith,0
     * station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,2020-10-09T19:12:48.633Z,
     * JohnSmith,0
     * station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,2020-10-09T19:12:51.001Z,
     * JohnSmith,0
     * station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,2020-10-09T19:12:53.427Z,
     * JohnSmith,0
     * station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,2020-10-09T19:12:55.785Z,
     * JohnSmith,0
     * station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,2020-10-09T19:12:44.485Z,
     * JohnSmith,0
     * station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,2020-10-09T19:12:46.880Z,
     * JohnSmith,0
     * station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,2020-10-09T19:12:49.216Z,
     * JohnSmith,0
     * station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,2020-10-09T19:12:51.552Z,
     * JohnSmith,0
     * station2,2016-04-29T00:00:00Z,10.2,-150.3,NaN,NaN,2020-10-09T19:12:54.052Z,
     * JohnSmith,1
     * station2,2016-04-29T01:00:00Z,10.2,-150.3,NaN,NaN,2020-10-09T19:12:56.389Z,
     * JohnSmith,1
     * station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,2020-10-09T19:12:45.134Z,
     * JohnSmith,0
     * station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,2020-10-09T19:12:47.474Z,
     * JohnSmith,0
     * station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,2020-10-09T19:12:49.828Z,
     * JohnSmith,0
     * station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,2020-10-09T19:12:52.146Z,
     * JohnSmith,0
     * station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,2020-10-09T19:12:54.606Z,
     * JohnSmith,0
     * station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,2020-10-09T19:12:56.984Z,
     * JohnSmith,0
     * station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,2020-10-09T19:12:45.765Z,
     * JohnSmith,0
     * station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,2020-10-09T19:12:48.068Z,
     * JohnSmith,0
     * station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,2020-10-09T19:12:50.439Z,
     * JohnSmith,0
     * station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,2020-10-09T19:12:52.743Z,
     * JohnSmith,0
     * station2,2016-05-29T00:00:00Z,10.2,-150.3,NaN,NaN,2020-10-09T19:12:55.178Z,
     * JohnSmith,1
     * station2,2016-05-29T01:00:00Z,10.2,-150.3,NaN,NaN,2020-10-09T19:12:57.577Z,
     * JohnSmith,1
     */
    expected =
        "stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n"
            + ",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n"
            + "myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z,me,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n"
            + "station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n"
            + "station2,2016-04-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-04-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n"
            + "station2,2016-05-29T00:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n"
            + "station2,2016-05-29T01:00:00Z,10.2,-150.3,NaN,NaN,.{24},JohnSmith,1\n";
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // direct read a data file
    results =
        File2.directReadFromUtf8File(
            Path.of(
                    EDDTestDataset.class
                        .getResource("/data/points/testFromHttpGet/station2/station2_2016-05.jsonl")
                        .toURI())
                .toString());
    // String2.log(results);
    expected =
        "\\[\"stationID\",\"time\",\"airTemp\",\"waterTemp\",\"timestamp\",\"author\",\"command\"\\]\n"
            + "\\[\"station2\",\"2016-05-29T00:00:00Z\",16,17,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T01:00:00Z\",16.1,17.1,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T02:00:00Z\",16.2,17.2,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T03:00:00Z\",16.3,17.3,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T00:00:00Z\",null,null,1.\\d+E9,\"JohnSmith\",1\\]\n"
            + "\\[\"station2\",\"2016-05-29T01:00:00Z\",null,null,1.\\d+E9,\"JohnSmith\",1\\]\n"
            + "\\[\"station2\",\"2016-05-29T00:00:00Z\",16,17,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T01:00:00Z\",16.1,17.1,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T02:00:00Z\",16.2,17.2,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T03:00:00Z\",16.3,17.3,1.\\d+E9,\"JohnSmith\",0\\]\n"
            + "\\[\"station2\",\"2016-05-29T00:00:00Z\",null,null,1.\\d+E9,\"JohnSmith\",1\\]\n"
            + "\\[\"station2\",\"2016-05-29T01:00:00Z\",null,null,1.\\d+E9,\"JohnSmith\",1\\]\n";
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // .csv (versioning: as of previous time)
    userDapQuery = "&timestamp<=" + (versioningTime / 1000.0);
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all5", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    Test.ensureLinesMatch(results, versioningExpected, "\nresults=\n" + results);

    // 2020-10-09 There were errors related to a request not including some required
    // variables, so test that
    userDapQuery = "airTemp";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_withProcessing",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "airTemp\n"
            + "degree_C\n"
            + "14.2\n"
            + "20.0\n"
            + "20.1\n"
            + "10.2\n"
            + "10.3\n"
            + "22.0\n"
            + "22.1\n"
            + // 22.1 is visible and previous 14.1 for same station and time
            // 2016-05-29T01:00:00Z isn't visible
            "14.2\n"
            + "14.3\n"
            + "12.2\n"
            + "12.3\n"
            + "16.2\n"
            + "16.3\n"
            + "20.0\n"
            + "20.1\n"
            + "10.2\n"
            + "10.3\n"
            + "12.2\n"
            + "12.3\n"
            + "22.0\n"
            + "22.1\n"
            + // 22.1 is visible and previous 14.1 for same station and time
            // 2016-05-29T01:00:00Z isn't visible
            "14.2\n"
            + "14.3\n"
            + "16.2\n"
            + "16.3\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // view the raw data for the station1 and time=2016-05-29T01:00:00Z 'row'
    userDapQuery = "&stationID=\"station1\"&time=2016-05-29T01:00:00Z&timestamp>-1";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1RawRow", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n"
            + ",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1,.{24},JohnSmith,0\n"
            + "station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n"; // so 22.1
    // is
    // visible
    // above,
    // not 14.1
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // e.g., max(timestamp)-0.001 -> 2020-10-09T19:29:31.667Z
    userDapQuery = "airTemp&timestamp<max(timestamp)-0.001"; // without the final change above
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_miniRollback",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "airTemp\n"
            + "degree_C\n"
            + "14.2\n"
            + "20.0\n"
            + "20.1\n"
            + "10.2\n"
            + "10.3\n"
            + "22.0\n"
            + "22.1\n"
            + "14.2\n"
            + "14.3\n"
            + "12.2\n"
            + "12.3\n"
            + "16.1\n"
            + // final change to dataset deleted this row 2016-05-29T01:00:00Z, so now it is
            // visible again
            "16.2\n"
            + "16.3\n"
            + "20.0\n"
            + "20.1\n"
            + "10.2\n"
            + "10.3\n"
            + "12.2\n"
            + "12.3\n"
            + "22.0\n"
            + "22.1\n"
            + "14.2\n"
            + "14.3\n"
            + "16.1\n"
            + // final change to dataset deleted this row 2016-05-29T01:00:00Z, so now it is
            // visible again
            "16.2\n"
            + "16.3\n";

    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    userDapQuery = "airTemp&timestamp>-1";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_withoutProcessing",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "airTemp\n"
            + "degree_C\n"
            + "14.2\n"
            + "10.0\n"
            + "10.1\n"
            + "10.2\n"
            + "10.3\n"
            + "20.0\n"
            + "20.1\n"
            + "10.0\n"
            + "10.1\n"
            + "10.2\n"
            + "10.3\n"
            + "20.0\n"
            + "20.1\n"
            + "14.0\n"
            + "14.1\n"
            + "14.2\n"
            + "14.3\n"
            + "22.0\n"
            + "22.1\n"
            + "14.0\n"
            + "14.1\n"
            + "14.2\n"
            + "14.3\n"
            + "22.0\n"
            + "22.1\n"
            + "12.0\n"
            + "12.1\n"
            + "12.2\n"
            + "12.3\n"
            + "NaN\n"
            + "NaN\n"
            + "12.0\n"
            + "12.1\n"
            + "12.2\n"
            + "12.3\n"
            + "NaN\n"
            + "NaN\n"
            + "16.0\n"
            + "16.1\n"
            + "16.2\n"
            + "16.3\n"
            + "NaN\n"
            + "NaN\n"
            + "16.0\n"
            + "16.1\n"
            + "16.2\n"
            + "16.3\n"
            + "NaN\n"
            + "NaN\n"
            + "10.0\n"
            + "10.1\n"
            + "10.2\n"
            + "10.3\n"
            + "20.0\n"
            + "20.1\n"
            + "10.0\n"
            + "10.1\n"
            + "10.2\n"
            + "10.3\n"
            + "20.0\n"
            + "20.1\n"
            + "12.0\n"
            + "12.1\n"
            + "12.2\n"
            + "12.3\n"
            + "NaN\n"
            + "NaN\n"
            + "12.0\n"
            + "12.1\n"
            + "12.2\n"
            + "12.3\n"
            + "NaN\n"
            + "NaN\n"
            + "14.0\n"
            + "14.1\n"
            + "14.2\n"
            + "14.3\n"
            + "22.0\n"
            + "22.1\n"
            + "14.0\n"
            + "14.1\n"
            + "14.2\n"
            + "14.3\n"
            + "22.0\n"
            + "22.1\n"
            + "16.0\n"
            + "16.1\n"
            + "16.2\n"
            + "16.3\n"
            + "NaN\n"
            + "NaN\n"
            + "16.0\n"
            + "16.1\n"
            + "16.2\n"
            + "16.3\n"
            + "NaN\n"
            + "NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    /* */
    // reallyVerbose = oReallyVerbose;

  }

  /**
   * Run TestAll (set to call this) a few times simultaneously and enter 1, 2, 3, 4. '3' repeatedly
   * tests that the file is valid. Wait till they are done, then run this with 0 to see resulting
   * file from multithreaded hammer test.
   */
  @TagIncompleteTest
  @org.junit.jupiter.api.Test
  void testMultithreadedReadWrite() throws Throwable {
    // int value = String2.parseInt(String2.getStringFromSystemIn("What integer (1,
    // 2, 4, ..., or 3 to test file validity, or 0 to see results)?"));
    testStatic(1);
    testStatic(2);
    testStatic(4);
    testStatic(3);
    testStatic(0);
  }
}
