package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.array.UByteArray;
import com.cohort.array.UIntArray;
import com.cohort.array.ULongArray;
import com.cohort.array.UShortArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.BitSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagExternalERDDAP;
import tags.TagExternalOther;
import tags.TagIncompleteTest;
import tags.TagLargeFiles;
import tags.TagMissingFile;
import tags.TagPassword;
import tags.TagSlowTests;
import testDataset.Initialization;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class TableTests {

  @TempDir private static Path TEMP_DIR;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This tests the little methods. */
  @org.junit.jupiter.api.Test
  void testLittleMethods() {
    // String2.log("\n*** Table.testLittleMethods...");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // isValid and findColumnNumber and subset
    Table table = getTestTable(true, true);
    table.ensureValid(); // throws Exception if not
    Test.ensureEqual(table.findColumnNumber("Time"), 0, "");
    Test.ensureEqual(table.findColumnNumber("String Data"), 9, "");
    Test.ensureEqual(table.findColumnNumber("zz"), -1, "");

    // toString
    table = getTestTable(true, true);
    // String2.log("toString: " + table.toString());

    // ensureEqual
    Table table2 = getTestTable(true, true);
    Test.ensureTrue(table.equals(table2), "test equals a");

    String2.log("intentional error:\n");
    table2.getColumn(2).setDouble(1, 100);
    Test.ensureEqual(table.equals(table2), false, "intentional notEqual b");

    String2.log("intentional error:\n");
    table2 = getTestTable(true, true);
    table2.getColumn(0).setDouble(2, 55);
    Test.ensureEqual(table.equals(table2), false, "intentional notEqual c");

    // getSubset
    /*
     * table = getTestTable();
     * table.getSubset(new int[]{table.secondsColumn},
     * new double[]{stringToSeconds("2005-08-31T16:01:01")},
     * new double[]{stringToSeconds("2005-08-31T16:01:03")});
     * Test.ensureEqual(table.nRows(), 1, "getSubset a");
     * Test.ensureEqual(table.nColumns(), 5, "getSubset b");
     * Test.ensureEqual(table.getColumn(table.secondsColumn), new
     * double[]{stringToSeconds("2005-08-31T16:01:02")}, "getSubset c");
     * Test.ensureEqual(table.getColumn(table.lonColumn), new double[]{-2},
     * "getSubset d");
     * Test.ensureEqual(table.getColumn(table.latColumn), new double[]{1.5},
     * "getSubset e");
     * Test.ensureEqual(table.getColumn(3)[0], 7, "getSubset f");
     * Test.ensureEqual(table.getColumn(4)[0], 8, "getSubset g");
     *
     * table = getTestTable();
     * table.getSubset(new int[]{table.latColumn},
     * new double[]{1.9},
     * new double[]{2.1});
     * Test.ensureEqual(table.nRows(), 1, "getSubset b");
     * Test.ensureEqual(table.getColumn(table.latColumn), new double[]{2},
     * "getSubset j");
     */

    // calculateStats look at array via constants and as array
    table = getTestTable(true, true);
    Test.ensureEqual(table.getColumnName(1), "Longitude", "columnNames a");
    table.setColumnName(1, "Test");
    Test.ensureEqual(table.getColumnName(1), "Test", "columnNames b");
    table.setColumnName(1, "Longitude");
    Test.ensureEqual(table.getColumnName(1), "Longitude", "columnNames c");

    double stats[] = table.getColumn(1).calculateStats();
    Test.ensureEqual(stats[PrimitiveArray.STATS_N], 3, "calculateStats n");
    Test.ensureEqual(stats[PrimitiveArray.STATS_MIN], -3, "calculateStats min");
    Test.ensureEqual(stats[PrimitiveArray.STATS_MAX], -1, "calculateStats max");

    // forceLonPM180(boolean pm180)
    table = getTestTable(true, true);
    PrimitiveArray lonAr = table.getColumn(1);
    Table.forceLonPM180(lonAr, false);
    Test.ensureEqual(lonAr.toString(), "357, 358, 359, 2147483647", "forceLonPM180f");
    Table.forceLonPM180(lonAr, true);
    Test.ensureEqual(lonAr.toString(), "-3, -2, -1, 2147483647", "forceLonPM180t");

    // clear
    table = getTestTable(true, true);
    table.clear();
    Test.ensureEqual(table.nRows(), 0, "clear a");
    Test.ensureEqual(table.nColumns(), 0, "clear b");

    // getXxxAttribute
    table = getTestTable(true, true);
    int col = table.findColumnNumber("Double Data");
    Test.ensureEqual(col, 3, "");
    Test.ensureEqual(table.globalAttributes().getString("global_att1"), "a string", "");
    Test.ensureEqual(table.globalAttributes().getString("test"), null, "");
    table.globalAttributes().set("global_att1", "new");
    Test.ensureEqual(table.globalAttributes().getString("global_att1"), "new", "");
    table.globalAttributes().remove("global_att1");
    Test.ensureEqual(table.globalAttributes().getString("global_att1"), null, "");

    Test.ensureEqual(table.columnAttributes(3).getString("units"), "doubles", "");
    table.columnAttributes(3).set("units", "new");
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "new", "");
    table.columnAttributes(3).remove("units");
    Test.ensureEqual(table.columnAttributes(3).getString("units"), null, "");
    Test.ensureEqual(table.getDoubleData(3, 1), 3.123, "");
    Test.ensureEqual(table.getStringData(3, 1), "3.123", "");
    table.setColumnName(3, "new3");
    Test.ensureEqual(table.getColumnName(3), "new3", "");

    // sort
    table.sort(new int[] {3}, new boolean[] {false});
    Test.ensureEqual(table.getColumn(2).toString(), "NaN, 2.0, 1.5, 1.0", "");
    Test.ensureEqual(table.getColumn(3).toString(), "NaN, 1.0E300, 3.123, -1.0E300", "");

    // removeColumn
    table.removeColumn(3);
    Test.ensureEqual(
        table.getColumn(3).toString(),
        "9223372036854775807, 2000000000000000, 2, -2000000000000000",
        "");
    Test.ensureEqual(table.getColumnName(3), "Long Data", "");
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "longs", "");

    // addColumn
    table.addColumn(3, "test3", new IntArray(new int[] {10, 20, 30, Integer.MAX_VALUE}));
    Test.ensureEqual(table.getColumn(3).toString(), "10, 20, 30, 2147483647", "");
    Test.ensureEqual(table.getColumnName(3), "test3", "");
    Test.ensureEqual(
        table.getColumn(4).toString(),
        "9223372036854775807, 2000000000000000, 2, -2000000000000000",
        "");
    Test.ensureEqual(table.getColumnName(4), "Long Data", "");
    Test.ensureEqual(table.columnAttributes(4).getString("units"), "longs", "");

    // append
    table.append(table);
    Test.ensureEqual(
        table.getColumn(4).toString(),
        "9223372036854775807, 2000000000000000, 2, -2000000000000000, 9223372036854775807, 2000000000000000, 2, -2000000000000000",
        "");
    Test.ensureEqual(table.getColumnName(4), "Long Data", "");
    Test.ensureEqual(table.columnAttributes(4).getString("units"), "longs", "");

    // average
    table = new Table();
    DoubleArray da = new DoubleArray(new double[] {10, 20, 30, 40, 50});
    table.addColumn("a", da);
    da = new DoubleArray(new double[] {0, 0, 1, 2, 2});
    table.addColumn("b", da);
    table.average(new int[] {1});
    Test.ensureEqual(table.getColumn(0).toString(), "15.0, 30.0, 45.0", "");
    Test.ensureEqual(table.getColumn(1).toString(), "0.0, 1.0, 2.0", "");
  }

  @org.junit.jupiter.api.Test
  void testReorderColumns() throws Exception {
    // String2.log("\n*** Table.testReorderColumns");
    Table table = new Table();
    table.addColumn("ints", new IntArray());
    table.addColumn("floats", new FloatArray());
    table.addColumn("doubles", new DoubleArray());

    StringArray newOrder = new StringArray(new String[] {"hubert", "floats", "doubles", "zztop"});
    table.reorderColumns(newOrder, false);
    String results = String2.toCSSVString(table.getColumnNames());
    String expected = "floats, doubles, ints";
    Test.ensureEqual(results, expected, "results=" + results);

    newOrder = new StringArray(new String[] {"ints", "doubles"});
    table.reorderColumns(newOrder, true); // discard columns not in list
    results = String2.toCSSVString(table.getColumnNames());
    expected = "ints, doubles";
    Test.ensureEqual(results, expected, "results=" + results);
  }

  @org.junit.jupiter.api.Test
  void testLastRowWithData() throws Exception {
    // String2.log("\n*** Table.testLastRowWithData");
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    Table table = new Table();
    String results, expected;
    Attributes iAtts = new Attributes();
    iAtts.add("missing_value", 99);
    iAtts.add("_FillValue", 999);
    Attributes fAtts = new Attributes();
    fAtts.add("_FillValue", -99f);
    Attributes dAtts = new Attributes();
    dAtts.add("_FillValue", -99.0);
    Attributes sAtts = new Attributes();
    sAtts.add("_FillValue", "99");

    IntArray ia =
        (IntArray) new IntArray(new int[] {1, 99, 999, Integer.MAX_VALUE}).setMaxIsMV(true);
    FloatArray fa = new FloatArray(new float[] {2, -99, -99, Float.NaN});
    DoubleArray da = new DoubleArray(new double[] {3, -99, -99, Double.NaN});
    StringArray sa = new StringArray(new String[] {"4", null, "99", ""});

    table.clear();
    table.addColumn(0, "i", ia, iAtts);
    Test.ensureEqual(table.lastRowWithData(), 0, "");

    table.clear();
    table.addColumn(0, "f", fa, fAtts);
    Test.ensureEqual(table.lastRowWithData(), 0, "");

    table.clear();
    table.addColumn(0, "d", da, dAtts);
    Test.ensureEqual(table.lastRowWithData(), 0, "");

    table.clear();
    table.addColumn(0, "s", sa, sAtts);
    Test.ensureEqual(table.lastRowWithData(), 0, "");

    table.clear();
    table.addColumn(0, "i", ia, iAtts);
    table.addColumn(1, "f", fa, fAtts);
    table.addColumn(2, "d", da, dAtts);
    table.addColumn(3, "s", sa, sAtts);
    Test.ensureEqual(table.lastRowWithData(), 0, "");

    // ***
    // String2.log("\n*** Table.testRemoveRowsWithoutData");
    table.clear();
    table.addColumn(0, "i", ia, iAtts);
    Test.ensureEqual(table.removeRowsWithoutData(), 1, "");

    table.clear();
    table.addColumn(0, "f", fa, fAtts);
    Test.ensureEqual(table.removeRowsWithoutData(), 1, "");

    table.clear();
    table.addColumn(0, "d", da, dAtts);
    Test.ensureEqual(table.removeRowsWithoutData(), 1, "");

    table.clear();
    table.addColumn(0, "s", sa, sAtts);
    Test.ensureEqual(table.removeRowsWithoutData(), 1, "");

    table.clear();
    table.addColumn(0, "i", ia, iAtts);
    table.addColumn(1, "f", fa, fAtts);
    table.addColumn(2, "d", da, dAtts);
    table.addColumn(3, "s", sa, sAtts);
    // good: original row +
    // hi 5 there
    ia.add(99);
    ia.add(999);
    ia.add(Integer.MAX_VALUE);
    ia.add(99);
    ia.add(999);
    fa.add(-99);
    fa.add(-99);
    fa.add(Float.NaN);
    fa.add(-99);
    fa.add(-99);
    da.add(-99);
    da.add(-99);
    da.add(Double.NaN);
    da.add(5);
    da.add(-99);
    sa.add("hi");
    sa.add("99");
    sa.add("");
    sa.add("");
    sa.add("there");
    table.removeRowsWithoutData();
    results = table.dataToString();
    expected =
        "i,f,d,s\n"
            + "1,2.0,3.0,4\n"
            + "99,-99.0,-99.0,hi\n"
            + "99,-99.0,5.0,\n"
            + "999,-99.0,-99.0,there\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // Table.debugMode = oDebug;

  }

  /**
   * This tests reading data from a ncCF multidimensional dataset where the dimensions are in the
   * discouraged order (e.g., var[time][station]). Very large test file has
   * discharge[time=758001][station=18] 4vars[station=18], so when flattened 758001 * 18 * 5vars *
   * 8bytes =~545MB + 2 indexColumns: 758001 * 18 * 2vars * 4bytes =~109MB
   *
   * @param readAsNcCF if true, this reads the file via readNcCF. If false, this reads the file via
   *     readMultidimNc
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagSlowTests
  void testReadNcCFMATimeSeriesReversed(boolean readAsNcCF) throws Exception {
    // String2.log("\n*** Table.testReadNcCFMATimeSeriesReversed readAsNcCF=" +
    // readAsNcCF);
    // time is days since 2006-01-01 00:00:00. file has 2007-10-01T04 through
    // 2013-11-14T17:06
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    Table table = new Table();
    String results, expected;
    long time;
    // was "/data/hunter/USGS_DISCHARGE_STATIONS_SUBSET.nc"
    String fileName =
        TableTests.class.getResource("/largeFiles/nccf/MATimeSeriesReversedDim.nc").getPath();
    // String2.log(NcHelper.ncdump(fileName, "-h"));

    /* */
    // read all vars when obs is constrained
    // String2.log("\n* read all vars when obs is constrained");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          null, // all vars
          0, // standardizeWhat=0
          StringArray.fromCSV("time"),
          StringArray.fromCSV(">"),
          StringArray.fromCSV("3426.69"));
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          null,
          null,
          null, // read default dimensions
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          StringArray.fromCSV("time"),
          StringArray.fromCSV(">"),
          StringArray.fromCSV("3426.69"));
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        // EEK! I don't think they should be different.
        // I think readMultidimNc is correct, because the -99999.0 values are from
        // rows with earlier time than valid data rows from same station at later time.
        // I think readNcCF is too aggressive at removing MV rows.
        "discharge,station,time,longitude,latitude\n"
            + (readAsNcCF ? "" : "-99999.0,1327750.0,3426.691666666651,-73.5958333,43.26944444\n")
            + "80.98618241999999,1327750.0,3426.6979166667443,-73.5958333,43.26944444\n"
            + // same station,
            // later time
            (readAsNcCF ? "" : "-99999.0,1357500.0,3426.691666666651,-73.7075,42.78527778\n")
            + "181.2278208,1357500.0,3426.6979166667443,-73.7075,42.78527778\n"
            + // same station, later time
            "183.77633703,1357500.0,3426.708333333372,-73.7075,42.78527778\n"
            + "-56.06735706,1484085.0,3426.691666666651,-75.3976111,39.05830556\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // just read station vars (all stations) no constraints
    // String2.log("\n* just read station vars (all stations) no constraints");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station,latitude,longitude"),
          0, // standardizeWhat=0
          null,
          null,
          null);
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          StringArray.fromCSV("station,latitude,longitude"),
          null, // dimensions
          null, // treatDimensionsAs
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          null,
          null,
          null);
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        "station,latitude,longitude\n"
            + "1463500.0,40.22166667,-74.7780556\n"
            + "1389500.0,40.8847222,-74.2261111\n"
            + "1327750.0,43.26944444,-73.5958333\n"
            + "1357500.0,42.78527778,-73.7075\n"
            + "1403060.0,40.5511111,-74.5483333\n"
            + "1474500.0,39.9678905,-75.1885123\n"
            + "1477050.0,39.83677934,-75.36630199\n"
            + "1480120.0,39.7362245,-75.540172\n"
            + "1481500.0,39.7695,-75.5766944\n"
            + "1480065.0,39.71063889,-75.6087222\n"
            + "1480015.0,39.71575,-75.6399444\n"
            + "1482170.0,39.65680556,-75.562\n"
            + "1482800.0,39.5009454,-75.5682589\n"
            + "1413038.0,39.3836111,-75.35027778\n"
            + "1412150.0,39.23166667,-75.0330556\n"
            + "1411435.0,39.16166667,-74.8319444\n"
            + "1484085.0,39.05830556,-75.3976111\n"
            + "1484080.0,39.01061275,-75.45794828\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // just read station vars (a few stations because lat is constrained)
    // String2.log("\n* just read station vars (a few stations because lat is
    // constrained)");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station,latitude,longitude"),
          0, // standardizeWhat=0
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("<"),
          StringArray.fromCSV("39.1"));
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          StringArray.fromCSV("station,latitude,longitude"),
          null,
          null, // dimensions, treatDimensionsAs
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("<"),
          StringArray.fromCSV("39.1"));
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        "station,latitude,longitude\n"
            + "1484085.0,39.05830556,-75.3976111\n"
            + "1484080.0,39.01061275,-75.45794828\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // just read obs vars (obs is constrained)
    // String2.log("\n* just read obs vars (obs is constrained)");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          StringArray.fromCSV("time,discharge"),
          0, // standardizeWhat=0
          StringArray.fromCSV("discharge"),
          StringArray.fromCSV(">"),
          StringArray.fromCSV("5400"));
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          StringArray.fromCSV("time,discharge"),
          null,
          null, // dimensions, treatDimensionsAs
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          StringArray.fromCSV("discharge"),
          StringArray.fromCSV(">"),
          StringArray.fromCSV("5400"));
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        "time,discharge\n"
            + "2076.5,5408.517777\n"
            + "2076.510416666628,5465.151471\n"
            + "2076.5208333332557,5493.468318\n"
            + "2076.53125,5521.785165\n"
            + "2076.541666666628,5521.785165\n"
            + "2076.5520833332557,5521.785165\n"
            + "2076.5625,5521.785165\n"
            + "2076.572916666628,5521.785165\n"
            + "2076.5833333332557,5493.468318\n"
            + "2076.59375,5465.151471\n"
            + "2076.604166666628,5436.834624\n"
            + "2076.6145833332557,5408.517777\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read all vars when station is constrained,
    // String2.log("\n* read all vars when station is constrained");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station,latitude,longitude,time,discharge"),
          0, // standardizeWhat=0
          StringArray.fromCSV("station"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("1463500.0"));
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          StringArray.fromCSV("station,latitude,longitude,time,discharge"),
          null,
          null, // dimensions, treatDimensionsAs
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          StringArray.fromCSV("station"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("1463500.0"));
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        // EEK! Again, readMultidimNc has additional rows with discharge=MV.
        // I think readMultidimNc is correct.
        "station,latitude,longitude,time,discharge\n"
            + "1463500.0,40.22166667,-74.7780556,638.1666666666279,92.02975275\n"
            + "1463500.0,40.22166667,-74.7780556,638.1770833332557,92.02975275\n"
            + "1463500.0,40.22166667,-74.7780556,638.1875,92.02975275\n"
            + "1463500.0,40.22166667,-74.7780556,638.1979166666279,92.87925815999999\n"
            + "1463500.0,40.22166667,-74.7780556,638.2083333332557,93.72876357\n"
            + (readAsNcCF
                ? ""
                : "1463500.0,40.22166667,-74.7780556,638.2083333333721,-99999.0\n"
                    + "1463500.0,40.22166667,-74.7780556,638.2125000000233,-99999.0\n"
                    + "1463500.0,40.22166667,-74.7780556,638.2166666666744,-99999.0\n")
            + "1463500.0,40.22166667,-74.7780556,638.21875,93.72876357\n"
            + (readAsNcCF
                ? ""
                : "1463500.0,40.22166667,-74.7780556,638.2208333333256,-99999.0\n"
                    + "1463500.0,40.22166667,-74.7780556,638.2250000000931,-99999.0\n")
            + "1463500.0,40.22166667,-74.7780556,638.2291666666279,94.86143745\n"
            + (readAsNcCF
                ? ""
                : "1463500.0,40.22166667,-74.7780556,638.2291666667443,-99999.0\n"
                    + "1463500.0,40.22166667,-74.7780556,638.2333333333954,-99999.0\n"
                    + "1463500.0,40.22166667,-74.7780556,638.2375000000466,-99999.0\n")
            + "1463500.0,40.22166667,-74.7780556,638.2395833332557,95.71094286\n"; // stop there
    results = results.substring(0, expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), readAsNcCF ? 256610 : 757994, "wrong nRows");

    // read all data
    // String2.log("\n* read all data");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName, null, 0, // standardizeWhat=0
          null, null, null);
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName, null, null, null, // read all dimensions
          true, 0, true, // getMetadata, standardizeWhat, removeMVRows,
          null, null, null);
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString(10);
    expected =
        readAsNcCF
            ? "discharge,station,time,longitude,latitude\n"
                + "92.02975275,1463500.0,638.1666666666279,-74.7780556,40.22166667\n"
                + "92.02975275,1463500.0,638.1770833332557,-74.7780556,40.22166667\n"
                + "92.02975275,1463500.0,638.1875,-74.7780556,40.22166667\n"
                + "92.87925815999999,1463500.0,638.1979166666279,-74.7780556,40.22166667\n"
                + "93.72876357,1463500.0,638.2083333332557,-74.7780556,40.22166667\n"
                + "93.72876357,1463500.0,638.21875,-74.7780556,40.22166667\n"
                + "94.86143745,1463500.0,638.2291666666279,-74.7780556,40.22166667\n"
                + "95.71094286,1463500.0,638.2395833332557,-74.7780556,40.22166667\n"
                + "95.71094286,1463500.0,638.25,-74.7780556,40.22166667\n"
                + "95.71094286,1463500.0,638.2604166666279,-74.7780556,40.22166667\n"
                + "...\n"
            : "discharge,station,time,longitude,latitude\n"
                + "92.02975275,1463500.0,638.1666666666279,-74.7780556,40.22166667\n"
                + "92.02975275,1463500.0,638.1770833332557,-74.7780556,40.22166667\n"
                + "92.02975275,1463500.0,638.1875,-74.7780556,40.22166667\n"
                + "92.87925815999999,1463500.0,638.1979166666279,-74.7780556,40.22166667\n"
                + "93.72876357,1463500.0,638.2083333332557,-74.7780556,40.22166667\n"
                + "-99999.0,1463500.0,638.2083333333721,-74.7780556,40.22166667\n"
                + "-99999.0,1463500.0,638.2125000000233,-74.7780556,40.22166667\n"
                + "-99999.0,1463500.0,638.2166666666744,-74.7780556,40.22166667\n"
                + "93.72876357,1463500.0,638.21875,-74.7780556,40.22166667\n"
                + "-99999.0,1463500.0,638.2208333333256,-74.7780556,40.22166667\n"
                + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), readAsNcCF ? 2315617 : 7539127, "wrong nRows");

    // read all vars when obs is constrained,
    // String2.log("\n* read all vars when obs is constrained");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station,latitude,longitude,time,discharge"),
          0, // standardizeWhat=0
          StringArray.fromCSV("discharge"),
          StringArray.fromCSV(">"),
          StringArray.fromCSV("5400"));
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          StringArray.fromCSV("station,latitude,longitude,time,discharge"),
          null,
          null, // dimensions, treatDimensionsAs
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          StringArray.fromCSV("discharge"),
          StringArray.fromCSV(">"),
          StringArray.fromCSV("5400"));
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        "station,latitude,longitude,time,discharge\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5,5408.517777\n"
            + "1463500.0,40.22166667,-74.7780556,2076.510416666628,5465.151471\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5208333332557,5493.468318\n"
            + "1463500.0,40.22166667,-74.7780556,2076.53125,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.541666666628,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5520833332557,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5625,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.572916666628,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5833333332557,5493.468318\n"
            + "1463500.0,40.22166667,-74.7780556,2076.59375,5465.151471\n"
            + "1463500.0,40.22166667,-74.7780556,2076.604166666628,5436.834624\n"
            + "1463500.0,40.22166667,-74.7780556,2076.6145833332557,5408.517777\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read all vars when station and obs are constrained,
    // String2.log("\n* read all vars when station and obs are constrained");
    time = System.currentTimeMillis();
    if (readAsNcCF)
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station,latitude,longitude,time,discharge"),
          0, // standardizeWhat=0
          StringArray.fromCSV("station,discharge"),
          StringArray.fromCSV("=,>"),
          StringArray.fromCSV("1463500.0,5400"));
    else {
      TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
      reader.readMultidimNc(
          fileName,
          StringArray.fromCSV("station,latitude,longitude,time,discharge"),
          null,
          null, // dimensions, treatDimensionsAs
          true,
          0,
          true, // getMetadata, standardizeWhat, removeMVRows,
          StringArray.fromCSV("station,discharge"),
          StringArray.fromCSV("=,>"),
          StringArray.fromCSV("1463500.0,5400"));
    }
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms");
    results = table.dataToString();
    expected =
        "station,latitude,longitude,time,discharge\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5,5408.517777\n"
            + "1463500.0,40.22166667,-74.7780556,2076.510416666628,5465.151471\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5208333332557,5493.468318\n"
            + "1463500.0,40.22166667,-74.7780556,2076.53125,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.541666666628,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5520833332557,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5625,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.572916666628,5521.785165\n"
            + "1463500.0,40.22166667,-74.7780556,2076.5833333332557,5493.468318\n"
            + "1463500.0,40.22166667,-74.7780556,2076.59375,5465.151471\n"
            + "1463500.0,40.22166667,-74.7780556,2076.604166666628,5436.834624\n"
            + "1463500.0,40.22166667,-74.7780556,2076.6145833332557,5408.517777\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Table.debugMode = oDebug;

  }

  /** This tests sortColumnsByName (and reorderColumns). */
  @org.junit.jupiter.api.Test
  void testSortColumnsByName() {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // String2.log("\n***** Table.testSortColumnsByName");
    Table table = getTestTable(true, true);
    table.setColumnName(2, "latitude"); // to test case-insensitive

    table.sortColumnsByName();

    // byte
    Test.ensureEqual(table.getColumnName(0), "Byte Data", "");
    Test.ensureEqual(table.columnAttributes(0).getString("units"), "bytes", "");

    // char
    Test.ensureEqual(table.getColumnName(1), "Char Data", "");
    Test.ensureEqual(table.columnAttributes(1).getString("units"), "chars", "");

    // double
    Test.ensureEqual(table.getColumnName(2), "Double Data", "");
    Test.ensureEqual(table.columnAttributes(2).getString("units"), "doubles", "");

    // int
    Test.ensureEqual(table.getColumnName(3), "Int Data", "");
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "ints", "");

    // Lat
    Test.ensureEqual(table.getColumnName(4), "latitude", "");
    Test.ensureEqual(table.columnAttributes(4).getString("units"), "degrees_north", "");

    // long
    Test.ensureEqual(table.getColumnName(5), "Long Data", "");
    Test.ensureEqual(table.columnAttributes(5).getString("units"), "longs", "");

    // Lon
    Test.ensureEqual(table.getColumnName(6), "Longitude", "");
    Test.ensureEqual(table.columnAttributes(6).getString("units"), "degrees_east", "");

    // short
    Test.ensureEqual(table.getColumnName(7), "Short Data", "");
    Test.ensureEqual(table.columnAttributes(7).getString("units"), "shorts", "");

    // String
    Test.ensureEqual(table.getColumnName(8), "String Data", "");
    Test.ensureEqual(table.columnAttributes(8).getString("units"), "Strings", "");

    // Time
    Test.ensureEqual(table.getColumnName(9), "Time", "");
    Test.ensureEqual(
        table.columnAttributes(9).getString("units"), Calendar2.SECONDS_SINCE_1970, "");
  }

  /** This tests saveAsEnhancedFlatNcFile and readEnhancedFlatNcFile. */
  @org.junit.jupiter.api.Test
  void testEnhancedFlatNcFile() throws Exception {

    // String2.log("\n*** Table.testEnhancedFlatNcFile()");
    String results, expected;
    String fileName = File2.getSystemTempDirectory() + "enhancedFlatNcFile.nc";

    Table table = makeToughTestTable();
    expected = String2.annotatedString(table.toString());
    // String2.log("expected=\n" + expected);

    table.saveAsEnhancedFlatNc(fileName);
    results = String2.annotatedString(table.toString());
    Test.ensureEqual(results, expected, "a"); // saveAsEnhancedFlatNc didn't change anything
    table.clear();

    table.readEnhancedFlatNc(fileName, null);
    table.globalAttributes().remove("id");
    results = String2.annotatedString(table.toString());
    expected =
        "{[10]\n"
            + "dimensions:[10]\n"
            + "[9]row = 5 ;[10]\n"
            + "[9]aString_strlen = 9 ;[10]\n"
            + "variables:[10]\n"
            + "[9]char aString(row, aString_strlen) ;[10]\n"
            + "[9][9]aString:test = \"a[252]b[10]\n"
            + "c\\td[8364]e\" ;[10]\n"
            + "[9]char aChar(row) ;[10]\n"
            + "[9][9]aChar:test = \"[252]\" ;[10]\n"
            + "[9]byte aByte(row) ;[10]\n"
            + "[9][9]aByte:_FillValue = 99 ;[10]\n"
            + "[9][9]aByte:test = -128, 127 ;[10]\n"
            + "[9]ubyte aUByte(row) ;[10]\n"
            + "[9][9]aUByte:_FillValue = 99 ;[10]\n"
            + "[9][9]aUByte:test = 0, 255 ;[10]\n"
            + "[9]short aShort(row) ;[10]\n"
            + "[9][9]aShort:_FillValue = 9999 ;[10]\n"
            + "[9][9]aShort:test = -32768, 32767 ;[10]\n"
            + "[9]ushort aUShort(row) ;[10]\n"
            + "[9][9]aUShort:_FillValue = 9999 ;[10]\n"
            + "[9][9]aUShort:test = 0, 65535 ;[10]\n"
            + "[9]int anInt(row) ;[10]\n"
            + "[9][9]anInt:_FillValue = 999999999 ;[10]\n"
            + "[9][9]anInt:test = -2147483648, 2147483647 ;[10]\n"
            + "[9]uint aUInt(row) ;[10]\n"
            + "[9][9]aUInt:_FillValue = 999999999 ;[10]\n"
            + "[9][9]aUInt:test = 0, 4294967295 ;[10]\n"
            + "[9]long aLong(row) ;[10]\n"
            + "[9][9]aLong:_FillValue = 999999999999 ;[10]\n"
            + "[9][9]aLong:test = -9223372036854775808, 9223372036854775807 ;[10]\n"
            + "[9]ulong aULong(row) ;[10]\n"
            + "[9][9]aULong:_FillValue = 999999999999 ;[10]\n"
            + "[9][9]aULong:test = 0, 18446744073709551615 ;[10]\n"
            + "[9]float aFloat(row) ;[10]\n"
            + "[9][9]aFloat:_FillValue = 1.0E36f ;[10]\n"
            + "[9][9]aFloat:test = -3.4028235E38f, NaNf ;[10]\n"
            + "[9]double aDouble(row) ;[10]\n"
            + "[9][9]aDouble:_FillValue = 1.0E300 ;[10]\n"
            + "[9][9]aDouble:test = -1.7976931348623157E308, NaN ;[10]\n"
            + "[10]\n"
            + "// global attributes:[10]\n"
            + "[9][9]:testc = \"[252]\" ;[10]\n"
            + "[9][9]:testi = -2147483648, 2147483647 ;[10]\n"
            + "[9][9]:testl = -9223372036854775808, 9223372036854775807 ;[10]\n"
            + "[9][9]:tests = \"a[252]b[10]\n"
            + "c\\td[8364]z\" ;[10]\n"
            + "[9][9]:testub = 0, 255 ;[10]\n"
            + "[9][9]:testui = 0, 4294967295 ;[10]\n"
            + "[9][9]:testul = 0, 9223372036854775807, 18446744073709551615 ;[10]\n"
            + "[9][9]:testus = 0, 65535 ;[10]\n"
            + "}[10]\n"
            + "aString,aChar,aByte,aUByte,aShort,aUShort,anInt,aUInt,aLong,aULong,aFloat,aDouble[10]\n"
            + "a\\u00fcb\\nc\\td\\u20ace,\\u00fc,-128,0,-32768,0,-2147483648,0,-9223372036854775808,0,-3.4028235E38,-1.7976931348623157E308[10]\n"
            + "ab,\\u0000,0,127,0,32767,0,7,0,1,2.2,3.3[10]\n"
            + ",A,99,99,9999,9999,999999999,2147483647,8,9223372036854775807,1.4E-45,4.9E-324[10]\n"
            + "cd,\\t,126,254,32766,65534,2147483646,4294967294,9223372036854775806,18446744073709551614,3.4028235E38,1.7976931348623157E308[10]\n"
            + ",\\u20ac,,,,,,,,,,[10]\n"
            + "[end]";
    Test.ensureEqual(results, expected, "b");
  }

  /** The tests readJsonLinesCsv. */
  @org.junit.jupiter.api.Test
  void testJsonlCSV() throws Exception {
    // String2.log("\n* String2.testReadJsonlCSV()\n" +
    // "The WARNING's below are expected...");

    String source =
        "[\"a\",\"\",\"ccc\",\"dddd\"]\n"
            + "[2,1.5,\" c3\u00b5\u20acz\\\\q\\f\\n\\r\\t\\u00b5\\u20ac \",true]\n"
            + "[\"BAD ROW WITH WRONG NUMBER OF ITEMS\"]\n"
            + "[\"BAD ROW with unterminated string ]\n"
            + "BAD ROW WITHOUT JSON ARRAY.\n"
            + " [null, null, \"1\", null] \n"
            + "        ";

    // don't simplify
    StringReader sr = new StringReader(source);
    Table table = new Table();
    table.readJsonlCSV(sr, "[StringReader]", null, null, false);
    String results = table.dataToString();
    Test.ensureEqual(
        results,
        "a,,ccc,dddd\n"
            + "2,1.5,\"\"\" c3\\u00b5\\u20acz\\\\\\\\q\\\\f\\\\n\\\\r\\\\t\\\\u00b5\\\\u20ac \"\"\",true\n"
            + // ?!
            // not
            // sure
            // if
            // correct
            ",,\"\"\"1\"\"\",\n",
        "results=\n" + results);

    // simplify
    sr = new StringReader(source);
    table = new Table();
    table.readJsonlCSV(sr, "[StringReader]", null, null, true);
    results = table.dataToString();
    Test.ensureEqual(
        results,
        "a,,ccc,dddd\n"
            + "2,1.5,\" c3\\u00b5\\u20acz\\\\q\\f\\n\\r\\t\\u00b5\\u20ac \",1\n"
            + ",,1,\n",
        "results=\n" + results);

    // specify colNames and simplify
    sr = new StringReader(source);
    table = new Table();
    table.readJsonlCSV(sr, "[StringReader]", StringArray.fromCSV("ccc,a,dddd"), null, true);
    results = table.dataToString();
    Test.ensureEqual(
        results,
        "ccc,a,dddd\n" + "\" c3\\u00b5\\u20acz\\\\q\\f\\n\\r\\t\\u00b5\\u20ac \",2,1\n" + "1,,\n",
        "results=\n" + results);
    Test.ensureEqual(table.getColumn(0).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(1).elementTypeString(), "byte", "");
    Test.ensureEqual(table.getColumn(2).elementTypeString(), "byte", ""); // boolean -> byte

    // specify colNames and types
    sr = new StringReader(source);
    table = new Table();
    table.readJsonlCSV(
        sr,
        "[StringReader]",
        StringArray.fromCSV("ccc,a,dddd"),
        new String[] {"String", "int", "boolean"},
        true);
    results = table.dataToString();
    Test.ensureEqual(
        results,
        "ccc,a,dddd\n" + "\" c3\\u00b5\\u20acz\\\\q\\f\\n\\r\\t\\u00b5\\u20ac \",2,1\n" + "1,,\n",
        "results=\n" + results);
    Test.ensureEqual(table.getColumn(0).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(1).elementTypeString(), "int", "");
    Test.ensureEqual(table.getColumn(2).elementTypeString(), "byte", ""); // boolean -> byte

    // tough test table
    String fullName = TEMP_DIR.toAbsolutePath().toString() + "/testJsonCSV.jsonl";
    File2.delete(fullName);
    table = makeToughTestTable();
    results = table.dataToString();
    String expected =
        "aString,aChar,aByte,aUByte,aShort,aUShort,anInt,aUInt,aLong,aULong,aFloat,aDouble\n"
            + "a\\u00fcb\\nc\\td\\u20ace,\\u00fc,-128,0,-32768,0,-2147483648,0,-9223372036854775808,0,-3.4028235E38,-1.7976931348623157E308\n"
            + "ab,\\u0000,0,127,0,32767,0,7,0,1,2.2,3.3\n"
            + ",A,99,99,9999,9999,999999999,2147483647,8,9223372036854775807,1.4E-45,4.9E-324\n"
            + "cd,\\t,126,254,32766,65534,2147483646,4294967294,9223372036854775806,18446744073709551614,3.4028235E38,1.7976931348623157E308\n"
            + ",\\u20ac,,,,,,,,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    table.writeJsonlCSV(fullName);
    table.clear(); // doubly sure
    table.readJsonlCSV(fullName, null, null, true); // simplify
    results = table.dataToString();
    expected = // differs in that aULong is now a double column (because simplify doesn't catch
        // unsigned types)
        "aString,aChar,aByte,aUByte,aShort,aUShort,anInt,aUInt,aLong,aULong,aFloat,aDouble\n"
            + "a\\u00fcb\\nc\\td\\u20ace,\\u00fc,-128,0,-32768,0,-2147483648,0,-9223372036854775808,0.0,-3.4028235E38,-1.7976931348623157E308\n"
            + "ab,\\u0000,0,127,0,32767,0,7,0,1.0,2.2,3.3\n"
            + ",A,99,99,9999,9999,999999999,2147483647,8,9.223372036854776E18,1.4E-45,4.9E-324\n"
            + "cd,\\t,126,254,32766,65534,2147483646,4294967294,9223372036854775806,1.8446744073709552E19,3.4028235E38,1.7976931348623157E308\n"
            + ",\\u20ac,,,,,,,,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    PAType tTypes[] = {
      PAType.STRING, PAType.STRING, PAType.BYTE, PAType.SHORT,
          PAType.SHORT, // char->String, unsigned ->
      // larger
      // signed
      PAType.INT, PAType.INT, PAType.LONG, PAType.LONG, PAType.DOUBLE, // ULong -> Double
      PAType.DOUBLE, PAType.DOUBLE
    }; // float->double because lots of decimal digits
    for (int col = 0; col < 12; col++)
      Test.ensureEqual(table.getColumn(col).elementType(), tTypes[col], "col=" + col);

    // another hard test
    fullName = TableTests.class.getResource("/data/jsonl/sampleCSV.jsonl").getPath();
    table.readJsonlCSV(fullName, null, null, true); // simpify
    results = table.dataToString();
    Test.ensureEqual(
        results,
        "ship,time,lat,lon,status,testLong,sst\n"
            + "Bell M. Shimada,2017-03-23T00:45:00Z,28.0002,-130.2576,A,-9223372036854775808,10.9\n"
            + "Bell M. Shimada,2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,-1234567890123456,\n"
            + "Bell M. Shimada,2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,0,10.7\n"
            + "Bell M. Shimada,2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",1234567890123456,99.0\n"
            + "\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,9223372036854775806,10.0\n"
            + ",,,,,,\n",
        "results=\n" + results);

    // *** write new file
    table = makeToughTestTable();
    fullName = File2.getSystemTempDirectory() + "testJsonlCSV.json";
    table.writeJsonlCSV(fullName);
    results = File2.directReadFromUtf8File(fullName);
    Test.ensureEqual(
        results,
        "[\"aString\",\"aChar\",\"aByte\",\"aUByte\",\"aShort\",\"aUShort\",\"anInt\",\"aUInt\",\"aLong\",\"aULong\",\"aFloat\",\"aDouble\"]\n"
            + "[\"a\\u00fcb\\nc\\td\\u20ace\",\"\\u00fc\",-128,0,-32768,0,-2147483648,0,-9223372036854775808,0,-3.4028235E38,-1.7976931348623157E308]\n"
            + "[\"ab\",\"\\u0000\",0,127,0,32767,0,7,0,1,2.2,3.3]\n"
            + "[\"\",\"A\",99,99,9999,9999,999999999,2147483647,8,9223372036854775807,1.4E-45,4.9E-324]\n"
            + "[\"cd\",\"\\t\",126,254,32766,65534,2147483646,4294967294,9223372036854775806,18446744073709551614,3.4028235E38,1.7976931348623157E308]\n"
            + "[\"\",\"\\u20ac\",null,null,null,null,null,null,null,null,null,null]\n",
        "results=\n" + results);

    // *** write/append new file
    table = makeToughTestTable();
    File2.delete(fullName);
    table.writeJsonlCSV(fullName, true);
    results = File2.directReadFromUtf8File(fullName);
    Test.ensureEqual(
        results,
        "[\"aString\",\"aChar\",\"aByte\",\"aUByte\",\"aShort\",\"aUShort\",\"anInt\",\"aUInt\",\"aLong\",\"aULong\",\"aFloat\",\"aDouble\"]\n"
            + "[\"a\\u00fcb\\nc\\td\\u20ace\",\"\\u00fc\",-128,0,-32768,0,-2147483648,0,-9223372036854775808,0,-3.4028235E38,-1.7976931348623157E308]\n"
            + "[\"ab\",\"\\u0000\",0,127,0,32767,0,7,0,1,2.2,3.3]\n"
            + "[\"\",\"A\",99,99,9999,9999,999999999,2147483647,8,9223372036854775807,1.4E-45,4.9E-324]\n"
            + "[\"cd\",\"\\t\",126,254,32766,65534,2147483646,4294967294,9223372036854775806,18446744073709551614,3.4028235E38,1.7976931348623157E308]\n"
            + "[\"\",\"\\u20ac\",null,null,null,null,null,null,null,null,null,null]\n",
        "results=\n" + results);

    // then append
    table.writeJsonlCSV(fullName, true);
    results = File2.directReadFromUtf8File(fullName);
    Test.ensureEqual(
        results,
        "[\"aString\",\"aChar\",\"aByte\",\"aUByte\",\"aShort\",\"aUShort\",\"anInt\",\"aUInt\",\"aLong\",\"aULong\",\"aFloat\",\"aDouble\"]\n"
            + "[\"a\\u00fcb\\nc\\td\\u20ace\",\"\\u00fc\",-128,0,-32768,0,-2147483648,0,-9223372036854775808,0,-3.4028235E38,-1.7976931348623157E308]\n"
            + "[\"ab\",\"\\u0000\",0,127,0,32767,0,7,0,1,2.2,3.3]\n"
            + "[\"\",\"A\",99,99,9999,9999,999999999,2147483647,8,9223372036854775807,1.4E-45,4.9E-324]\n"
            + "[\"cd\",\"\\t\",126,254,32766,65534,2147483646,4294967294,9223372036854775806,18446744073709551614,3.4028235E38,1.7976931348623157E308]\n"
            + "[\"\",\"\\u20ac\",null,null,null,null,null,null,null,null,null,null]\n"
            + "[\"a\\u00fcb\\nc\\td\\u20ace\",\"\\u00fc\",-128,0,-32768,0,-2147483648,0,-9223372036854775808,0,-3.4028235E38,-1.7976931348623157E308]\n"
            + "[\"ab\",\"\\u0000\",0,127,0,32767,0,7,0,1,2.2,3.3]\n"
            + "[\"\",\"A\",99,99,9999,9999,999999999,2147483647,8,9223372036854775807,1.4E-45,4.9E-324]\n"
            + "[\"cd\",\"\\t\",126,254,32766,65534,2147483646,4294967294,9223372036854775806,18446744073709551614,3.4028235E38,1.7976931348623157E308]\n"
            + "[\"\",\"\\u20ac\",null,null,null,null,null,null,null,null,null,null]\n",
        "results=\n" + results);
  }

  /** Test convert. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testConvert() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    String url, fileName;
    Table table = new Table();

    // /*
    // the original test from Roy
    // This is used as an example in various documentation.
    // If url changes, do search and replace to change all references to it.
    url = "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"";
    // String2.log("\ntesting Table.convert \n url=" + url);
    fileName = TEMP_DIR.toAbsolutePath().toString() + "/convertOriginal.nc";
    Table.convert(url, Table.READ_OPENDAP_SEQUENCE, fileName, Table.SAVE_AS_FLAT_NC, "row", false);
    table.readFlatNc(fileName, null, 0); // standardizeWhat=0, should be already unpacked.
    // String2.log(table.toString(3));
    Test.ensureEqual(table.nColumns(), 2, "");
    Test.ensureEqual(table.nRows(), 190, "");
    Test.ensureEqual(table.getColumnName(0), "t0", "");
    Test.ensureEqual(table.getColumnName(1), "oxygen", "");
    Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Temperature T0", "");
    Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Oxygen", "");
    Test.ensureEqual(table.getFloatData(0, 0), 12.1185f, "");
    Test.ensureEqual(table.getFloatData(0, 1), 12.1977f, "");
    Test.ensureEqual(table.getFloatData(1, 0), 6.56105f, "");
    Test.ensureEqual(table.getFloatData(1, 1), 6.95252f, "");
    File2.delete(fileName);
    // */

    // /*
    // The 8/16/06 test from osu
    // Test values below from html ascii request (same as url below, but with .ascii
    // before "?")
    // adcp95.yearday, adcp95.depth, adcp95.x, adcp95.y, adcp95.eastv
    // row0: 184.45120239257812, 22, -123.8656005859375, 48.287899017333984,
    // -0.31200000643730164
    // last row: 185.99949645996094, 22, -125.98069763183594, 42.844200134277344,
    // -0.15399999916553497
    url =
        "http://nwioos.coas.oregonstate.edu:8080/dods/drds/1995%20Hake%20Survey%20ADCP?ADCP95.yearday,ADCP95.Z,ADCP95.x,ADCP95.y,ADCP95.EV&ADCP95.yearday<186&ADCP95.Z<25";
    // String2.log("\ntesting Table.convert \n url=" + url);
    fileName = TEMP_DIR.toAbsolutePath().toString() + "/convertOSU.nc";
    Table.convert(url, Table.READ_OPENDAP_SEQUENCE, fileName, Table.SAVE_AS_FLAT_NC, "row", false);
    table.readFlatNc(fileName, null, 0); // standardizeWhat=0, should be already unpacked.
    // String2.log(table.toString(3));
    Test.ensureEqual(table.nColumns(), 5, "");
    Test.ensureEqual(table.nRows(), 446, "");
    Test.ensureEqual(table.getColumnName(0), "yearday", "");
    Test.ensureEqual(table.getColumnName(1), "Z", "");
    Test.ensureEqual(table.getColumnName(2), "x", "");
    Test.ensureEqual(table.getColumnName(3), "y", "");
    Test.ensureEqual(table.getColumnName(4), "EV", "");
    // no attributes
    Test.ensureEqual(table.getDoubleData(0, 0), 184.45120239257812, "");
    Test.ensureEqual(table.getDoubleData(1, 0), 22, "");
    Test.ensureEqual(table.getDoubleData(2, 0), -123.8656005859375, "");
    Test.ensureEqual(table.getDoubleData(3, 0), 48.287899017333984, "");
    Test.ensureEqual(table.getDoubleData(4, 0), -0.31200000643730164, "");
    Test.ensureEqual(table.getDoubleData(0, 445), 185.99949645996094, "");
    Test.ensureEqual(table.getDoubleData(1, 445), 22, "");
    Test.ensureEqual(table.getDoubleData(2, 445), -125.98069763183594, "");
    Test.ensureEqual(table.getDoubleData(3, 445), 42.844200134277344, "");
    Test.ensureEqual(table.getDoubleData(4, 445), -0.15399999916553497, "");
    File2.delete(fileName);
    // */

    // /*
    // The 8/17/06 test from cimt
    // Test values below from html ascii request (same as url below, but with .ascii
    // before "?")
    // vCTD.latitude, vCTD.longitude, vCTD.station, vCTD.depth, vCTD.salinity
    // first: 36.895, -122.082, "T101", 1.0, 33.9202
    // last: 36.609, -121.989, "T702", 4.0, 33.4914
    url =
        "http://cimt.dyndns.org:8080/dods/drds/vCTD?vCTD.latitude,vCTD.longitude,vCTD.station,vCTD.depth,vCTD.salinity&vCTD.depth<5";
    // String2.log("\ntesting Table.convert \n url=" + url);
    fileName = TEMP_DIR.toAbsolutePath().toString() + "/convertCIMT.nc";
    Table.convert(url, Table.READ_OPENDAP_SEQUENCE, fileName, Table.SAVE_AS_FLAT_NC, "row", false);
    table.readFlatNc(fileName, null, 0); // standardizeWhat=0, should be already unpacked.
    // String2.log(table.toString(3));
    Test.ensureEqual(table.nColumns(), 5, "");
    // Test.ensureEqual(table.nRows(), 1407, ""); //this changes; file is growing
    Test.ensureEqual(table.getColumnName(0), "latitude", "");
    Test.ensureEqual(table.getColumnName(1), "longitude", "");
    Test.ensureEqual(table.getColumnName(2), "station", "");
    Test.ensureEqual(table.getColumnName(3), "depth", "");
    Test.ensureEqual(table.getColumnName(4), "salinity", "");
    // no attributes
    Test.ensureEqual(table.getFloatData(0, 0), 36.895f, "");
    Test.ensureEqual(table.getFloatData(1, 0), -122.082f, "");
    Test.ensureEqual(table.getStringData(2, 0), "T101", "");
    Test.ensureEqual(table.getFloatData(3, 0), 1.0f, "");
    Test.ensureEqual(table.getFloatData(4, 0), 33.9202f, "");
    Test.ensureEqual(table.getFloatData(0, 1406), 36.609f, "");
    Test.ensureEqual(table.getFloatData(1, 1406), -121.989f, "");
    Test.ensureEqual(table.getStringData(2, 1406), "T702", "");
    Test.ensureEqual(table.getFloatData(3, 1406), 4.0f, "");
    Test.ensureEqual(table.getFloatData(4, 1406), 33.4914f, "");
    File2.delete(fileName);
    // */
  }

  /**
   * Test the readASCII and saveAsASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testASCII() throws Exception {
    // *** test read all
    // String2.log("\n***** Table.testASCII read all");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    Table table = getTestTable(true, true);
    table.removeRow(3); // remove the empty row at the end, since readASCII will remove it

    Table table1 = getTestTable(true, true);
    table1.removeRow(3); // remove the empty row at the end, since readASCII will remove it

    // write it to a file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/tempTable.asc";
    table.saveAsTabbedASCII(fileName);
    // String2.log(fileName + "=\n" + File2.directReadFrom88591File(fileName));

    // read it from the file
    Table table2 = new Table();
    // // Table.debugMode = true;
    table2.readASCII(fileName);
    // // Table.debugMode = false;

    // check units on 1st data row
    Test.ensureEqual(table2.getStringData(1, 0), "degrees_east", "");
    Test.ensureEqual(table2.getStringData(2, 0), "degrees_north", "");

    // remove units row
    table2.removeRow(0);
    table2.simplify();

    // are they the same (but column types may be different)?
    Test.ensureTrue(
        table1.equals(table2, false),
        "\ntable1=" + table1.toString() + "\ntable2=" + table2.toString());

    // test simplification: see if column types are the same as original table
    int n = table.nColumns();
    for (int col = 2;
        col < n;
        col++) // skip first 2 columns which are intentionally initially stored in bigger
      // type
      if (col != 4 && // LongArray -> StringArray
          col != 8) // CharArray -> StringArray
      Test.ensureEqual(
            table.columns.get(col).elementType(),
            table2.getColumn(col).elementType(),
            "test type of col#" + col);

    // *** test read subset
    // String2.log("\n***** Table.testASCII read subset");

    // read 2nd row from the file
    table2 = new Table();
    table2.readASCII(
        fileName,
        File2.ISO_8859_1,
        "",
        "",
        0,
        1,
        "",
        new String[] {"Int Data"},
        new double[] {0},
        new double[] {4},
        new String[] {"Short Data", "String Data"},
        true);
    Test.ensureEqual(table2.nColumns(), 2, "");
    Test.ensureEqual(table2.nRows(), 1, "");
    Test.ensureEqual(table2.getColumnName(0), "Short Data", "");
    Test.ensureEqual(table2.getColumnName(1), "String Data", "");
    Test.ensureEqual(table2.getDoubleData(0, 0), 7, "");
    Test.ensureEqual(table2.getStringData(1, 0), "bb", "");

    // *** test read subset with no column names (otherwise same as test above)
    // String2.log("\n***** Table.testASCII read subset with no column names");
    // read 3rd row from the file
    table2 = new Table();
    table2.readASCII(
        fileName,
        File2.ISO_8859_1,
        "",
        "",
        -1,
        1,
        "", // -1=no column names
        new String[] {"Column#5"},
        new double[] {0},
        new double[] {4},
        new String[] {"Column#6", "Column#8", "Column#9"},
        true);
    Test.ensureEqual(table2.nColumns(), 3, "");
    Test.ensureEqual(table2.nRows(), 1, "");
    Test.ensureEqual(table2.getColumnName(0), "Column#6", "");
    Test.ensureEqual(table2.getColumnName(1), "Column#8", "");
    Test.ensureEqual(table2.getColumnName(2), "Column#9", "");
    Test.ensureEqual(table2.getDoubleData(0, 0), 7, "");
    Test.ensureEqual(table2.getStringData(1, 0), "\"", "");
    Test.ensureEqual(table2.getStringData(2, 0), "bb", "");

    // ** finally
    File2.delete(fileName);
  }

  /**
   * Test readStandardTabbedASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testReadStandardTabbedASCII() throws Exception {
    // *** test read all
    // String2.log("\n***** Table.testReadStandardTabbedASCII");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    String lines =
        "colA\tcolB\tcolC\n"
            + "1a\t1b\t1c\n"
            + "2\n"
            + "a\t2\n"
            + "b\t2c\n"
            + "3a\t3b\t3c"; // no terminal \n
    Table table = new Table();

    // read it from lines
    table.readStandardTabbedASCII(
        "tFileName", new BufferedReader(new StringReader(lines)), null, true);
    // String2.log("nRows=" + table.nRows() + " nCols=" + table.nColumns());
    Test.ensureEqual(
        table.dataToString(),
        "colA,colB,colC\n" + "1a,1b,1c\n" + "2\\na,2\\nb,2c\n" + "3a,3b,3c\n",
        "tFileName toCSVString=\n" + table.dataToString());

    // write it to a file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/tempTable.asc";
    File2.writeToFile88591(fileName, lines);

    // read all columns from the file
    Table table2 = new Table();
    table2.readStandardTabbedASCII(fileName, null, true);
    // String2.log("nRows=" + table2.nRows() + " nCols=" + table2.nColumns());
    Test.ensureEqual(
        table2.dataToString(),
        "colA,colB,colC\n" + "1a,1b,1c\n" + "2\\na,2\\nb,2c\n" + "3a,3b,3c\n",
        "table2 toCSVString=\n" + table2.dataToString());

    // just read cols B and C from the file
    table2 = new Table();
    table2.readStandardTabbedASCII(fileName, new String[] {"colB", "colC"}, true);
    // String2.log("nRows=" + table2.nRows() + " nCols=" + table2.nColumns());
    Test.ensureEqual(
        table2.dataToString(),
        "colB,colC\n" + "1b,1c\n" + "2\\nb,2c\n" + "3b,3c\n",
        "table2 toCSVString=\n" + table2.dataToString());

    // ** finally
    File2.delete(fileName);
  }

  /**
   * Test the saveAsHtml.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testHtml() throws Exception {
    // String2.log("\n***** Table.testHtml");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    Table table = getTestTable(true, true);

    // write it to html file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/tempTable.html";
    table.saveAsHtml(
        fileName,
        "preTextHtml\n<br>\n",
        "postTextHtml\n<br>",
        null,
        Table.BGCOLOR,
        1,
        true,
        0,
        true, // needEncodingAsHtml
        false);
    // String2.log(fileName + "=\n" + File2.directReadFromUtf8File(fileName));
    // Test.displayInBrowser("file://" + fileName); // .html

    // read it from the file
    String results = File2.directReadFromUtf8File(fileName);
    Test.ensureEqual(
        results,
        "<!DOCTYPE HTML>\n"
            + "<html lang=\"en-US\">\n"
            + "<head>\n"
            + "  <title>tempTable</title>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <link href=\"https://coastwatch.pfeg.noaa.gov/erddap/images/erddap2.css\" rel=\"stylesheet\" type=\"text/css\">\n"
            + "</head>\n"
            + "<body>\n"
            + "preTextHtml\n"
            + "<br>\n"
            + "<table class=\"erd nowrap\" style=\"background-color:#ffffcc;\" >\n"
            + "<tr>\n"
            + "<th>Time\n"
            + "<th>Longitude\n"
            + "<th>Latitude\n"
            + "<th>Double Data\n"
            + "<th>Long Data\n"
            + "<th>Int Data\n"
            + "<th>Short Data\n"
            + "<th>Byte Data\n"
            + "<th>Char Data\n"
            + "<th>String Data\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>UTC\n"
            + "<th>degrees_east\n"
            + "<th>degrees_north\n"
            + "<th>doubles\n"
            + "<th>longs\n"
            + "<th>ints\n"
            + "<th>shorts\n"
            + "<th>bytes\n"
            + "<th>chars\n"
            + "<th>Strings\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>1970-01-01T00:00:00Z\n"
            + "<td>-3\n"
            + "<td>1.0\n"
            + "<td>-1.0E300\n"
            + "<td>-2000000000000000\n"
            + "<td>-2000000000\n"
            + "<td>-32000\n"
            + "<td>-120\n"
            + "<td>,\n"
            + "<td>a\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2005-08-31T16:01:02Z\n"
            + "<td>-2\n"
            + "<td>1.5\n"
            + "<td>3.123\n"
            + "<td>2\n"
            + "<td>2\n"
            + "<td>7\n"
            + "<td>8\n"
            + "<td>&quot;\n"
            + "<td>bb\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2005-11-02T18:04:09Z\n"
            + "<td>-1\n"
            + "<td>2.0\n"
            + "<td>1.0E300\n"
            + "<td>2000000000000000\n"
            + "<td>2000000000\n"
            + "<td>32000\n"
            + "<td>120\n"
            + "<td>&#x20ac;\n"
            + "<td>ccc\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>\n"
            + "<td>&nbsp;\n"
            + "</tr>\n"
            + "</table>\n"
            + "postTextHtml\n"
            + "<br></body>\n"
            + "</html>\n",
        results);

    // test readHtml - treat 2nd row as data
    Table table2 = new Table();
    table2.readHtml(fileName, results, 0, false, true); // secondRowHasUnits, simplify
    String csv = String2.annotatedString(table2.dataToString());
    Test.ensureEqual(
        csv, // so units appear here as a row of data
        "Time,Longitude,Latitude,Double Data,Long Data,Int Data,Short Data,Byte Data,Char Data,String Data[10]\n"
            + "UTC,degrees_east,degrees_north,doubles,longs,ints,shorts,bytes,chars,Strings[10]\n"
            + "1970-01-01T00:00:00Z,-3,1.0,-1.0E300,-2000000000000000,-2000000000,-32000,-120,\",\",a[10]\n"
            + "2005-08-31T16:01:02Z,-2,1.5,3.123,2,2,7,8,\"\"\"\",bb[10]\n"
            + "2005-11-02T18:04:09Z,-1,2.0,1.0E300,2000000000000000,2000000000,32000,120,\\u20ac,ccc[10]\n"
            + ",,,,,,,,,[10]\n"
            + "[end]",
        csv);

    // test readHtml - treat 2nd row as units
    table2 = new Table();
    table2.readHtml(fileName, results, 0, true, true); // secondRowHasUnits, simplify
    csv = String2.annotatedString(table2.dataToString());
    Test.ensureEqual(
        csv, // so units correctly stored as units
        "Time,Longitude,Latitude,Double Data,Long Data,Int Data,Short Data,Byte Data,Char Data,String Data[10]\n"
            + "1970-01-01T00:00:00Z,-3,1.0,-1.0E300,-2000000000000000,-2000000000,-32000,-120,\",\",a[10]\n"
            + "2005-08-31T16:01:02Z,-2,1.5,3.123,2,2,7,8,\"\"\"\",bb[10]\n"
            + "2005-11-02T18:04:09Z,-1,2.0,1.0E300,2000000000000000,2000000000,32000,120,\\u20ac,ccc[10]\n"
            + ",,,,,,,,,[10]\n"
            + "[end]",
        csv);
    Test.ensureEqual(table2.columnAttributes(0).getString("units"), "UTC", "");
    Test.ensureEqual(table2.columnAttributes(1).getString("units"), "degrees_east", "");
    Test.ensureEqual(table2.columnAttributes(8).getString("units"), "chars", "");
    Test.ensureEqual(table2.columnAttributes(9).getString("units"), "Strings", "");

    // ** finally
    Math2.gc(
        "Table (between tests)",
        10000); // in a test. Do something useful while browser gets going to display
    // the
    // file.
    File2.delete(fileName);
  }

  /**
   * This is a test of readFlatNc and saveAsFlatNc.
   *
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testFlatNc() throws Exception {

    // ********** test reading all data
    // String2.log("\n*** Table.testFlatNc write and then read all");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    Table table = getTestTable(false, true); // falses=.nc doesn't seem to take longs
    // String2.log("*******table=" + table.toString());

    // write it to a file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/tempTable.nc";
    table.saveAsFlatNc(fileName, "time");

    // read it from the file
    Table table2 = new Table();
    table2.readFlatNc(fileName, null, 0); // standardizeWhat=0
    // String2.log("*********table2=" + table2.toString());

    // replace ' ' with '_' in column names
    for (int i = 0; i < table.columnNames.size(); i++)
      table.columnNames.set(i, String2.replaceAll(table.columnNames.get(i), " ", "_"));

    // do the test that the tables are equal
    // String2.log("testFlatNc table.nColAtt=" + table.columnAttributes.size() + //
    // ? why columnAtt?
    // " table2.nColAtt=" + table2.columnAttributes.size());
    // except char \\u20ac becomes "?" in nc file, so reset it
    Test.ensureEqual(table2.columns.get(7).getString(2), "?", "");
    table2.columns.get(7).setString(2, "\u20ac");
    if (table2.columns.get(7).getString(3).equals("?")) table2.columns.get(7).setString(3, "");
    String t1 = table.dataToString();
    String t2 = table.dataToString();
    // String2.log("\nt1=\n" + t1 + "\nt2=\n" + t2);
    Test.ensureEqual(t1, t2, "");
    Test.ensureTrue(table.equals(table2), "Test table equality");

    // test if data types are the same
    int n = table.nColumns();
    for (int col = 0; col < n; col++)
      Test.ensureEqual(
          table.columns.get(col).elementType(),
          table2.columns.get(col).elementType(),
          "test type of col#" + col);

    // clean up
    table2.clear();
    File2.delete(fileName);

    // ***test unpack options (and global and variable attributes)
    // String2.log("\n*** Table.testFlatNc test unpack");
    // row of data from 41015h1993.txt
    // YY MM DD hh WD WSPD GST WVHT DPD APD MWD BAR ATMP WTMP DEWP VIS
    // 93 05 24 11 194 02.5 02.8 00.70 04.20 04.90 185 1021.2 17.3 16.4 999.0 18.5
    double seconds = Calendar2.isoStringToEpochSeconds("1993-05-24T11");
    // String2.log("seconds=" + seconds);
    int[] testColumns = {0};
    double testMin[] = {seconds};
    double testMax[] = {seconds};

    // don't unpack
    table.readFlatNc(
        TEMP_DIR.toAbsolutePath().toString() + "/41015.nc",
        new String[] {"time", "BAR"},
        0); // standardizeWhat=0
    // String2.log(table.toString(100));
    table.subset(testColumns, testMin, testMax);
    Test.ensureEqual(table.nColumns(), 2, "");
    Test.ensureEqual(table.nRows(), 1, "");
    Test.ensureEqual(table.getColumnName(0), "time", "");
    Test.ensureEqual(table.getColumnName(1), "BAR", "");
    Test.ensureEqual(table.getColumn(1).elementType(), PAType.SHORT, ""); // short
    Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
    Test.ensureEqual(table.getDoubleData(1, 0), 10212, "");

    // test global and variable attributes
    Test.ensureEqual(
        table.globalAttributes().getString("creator_name"), "NOAA National Data Buoy Center", "");
    Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Sea Level Pressure", "");

    // unpack, to float if that is recommended
    table.readFlatNc(
        TEMP_DIR.toAbsolutePath().toString() + "/41015.nc",
        new String[] {"time", "BAR"},
        1); // standardizeWhat=1
    table.subset(testColumns, testMin, testMax);
    Test.ensureEqual(table.nColumns(), 2, "");
    Test.ensureEqual(table.nRows(), 1, "");
    Test.ensureEqual(table.getColumnName(0), "time", "");
    Test.ensureEqual(table.getColumnName(1), "BAR", "");
    Test.ensureEqual(table.getColumn(1).elementType(), PAType.FLOAT, ""); // float
    Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
    Test.ensureEqual(table.getFloatData(1, 0), 1021.2f, "");

    // ********** test reading subset of data via bitset (which uses read via
    // firstrow/lastrow)
    // String2.log("\n*** Table.testFlatNc read subset");
    table.clear();
    NetcdfFile netcdfFile = NcHelper.openFile(TEMP_DIR.toAbsolutePath().toString() + "/41015.nc");
    try {
      Variable loadVariables[] = NcHelper.findVariables(netcdfFile, new String[] {"time", "BAR"});
      Variable testVariables[] = NcHelper.findVariables(netcdfFile, new String[] {"time"});
      BitSet okRows = NcHelper.testRows(testVariables, testMin, testMax);
      table.appendNcRows(loadVariables, okRows);
      Test.ensureEqual(okRows.cardinality(), 1, "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    Test.ensureEqual(table.nColumns(), 2, "");
    Test.ensureEqual(table.nRows(), 1, "");
    Test.ensureEqual(table.getColumnName(0), "time", "");
    Test.ensureEqual(table.getColumnName(1), "BAR", "");
    Test.ensureEqual(table.getColumn(1).elementType(), PAType.SHORT, ""); // short
    Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
    Test.ensureEqual(table.getDoubleData(1, 0), 10212, ""); // still packed
  }

  /**
   * This is a test of read4DNc and saveAs4DNc.
   *
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  void test4DNc() throws Exception {

    // ********** test reading all data
    // String2.log("\n*** Table.test4DNc write and then read all");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    Table table = new Table();
    DoubleArray xCol = new DoubleArray();
    DoubleArray yCol = new DoubleArray();
    DoubleArray zCol = new DoubleArray();
    DoubleArray tCol = new DoubleArray();
    IntArray data1Col = new IntArray();
    DoubleArray data2Col = new DoubleArray();
    StringArray data3Col = new StringArray();
    table.addColumn("X", xCol);
    table.addColumn("Y", yCol);
    table.addColumn("Z", zCol);
    table.addColumn("T", tCol);
    table.addColumn("data1", data1Col);
    table.addColumn("data2", data2Col);
    table.addColumn("data3", data3Col);
    for (int t = 0; t < 2; t++) {
      for (int z = 0; z < 3; z++) {
        for (int y = 0; y < 3; y++) {
          for (int x = 0; x < 4; x++) {
            xCol.add(x + 1);
            yCol.add(y + 1);
            zCol.add(z + 1);
            tCol.add(t + 1);
            int fac = (x + 1) * (y + 1) * (z + 1) * (t + 1);
            data1Col.add(fac);
            data2Col.add(100 + (x + 1) * (y + 1) * (z + 1) * (t + 1));
            data3Col.add("" + fac);
          }
        }
      }
    }
    table.ensureValid(); // throws Exception if not
    // String2.log(table.toString("obs", 10));

    // write it to a file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/temp4DTable.nc";
    Attributes idAttributes = new Attributes();
    idAttributes.set("long_name", "The station's name.");
    idAttributes.set("units", DataHelper.UNITLESS);
    String stringVariableValue = "My Id Value";
    table.saveAs4DNc(fileName, 0, 1, 2, 3, "ID", stringVariableValue, idAttributes);

    // then insert col 4 filled with "My Id Value"
    String sar[] = new String[table.nRows()];
    Arrays.fill(sar, stringVariableValue);
    table.addColumn(4, "ID", new StringArray(sar), idAttributes);

    // get the header
    // String2.log("table=" + String2.log(NcHelper.ncdump(fileName, "-h")));

    // read from file
    Table table2 = new Table();
    table2.read4DNc(fileName, null, 1, "ID", 4); // standarizeWhat=1
    // String2.log("col6=" + table2.getColumn(6));
    // String2.log("\ntable2 after read4DNc: " + table2.toString("obs", 1000000));

    // test equality
    Test.ensureTrue(table.equals(table2), "test4DNc tables not equal!");

    File2.delete(fileName);
  }

  /** Test the speed of readASCII */
  @org.junit.jupiter.api.Test
  void testReadASCIISpeed() throws Exception {

    String fileName =
        TableTests.class.getResource("/data/points/ndbcMet2HistoricalTxt/41009h1990.txt").getPath();
    long time = 0;

    for (int attempt = 0; attempt < 4; attempt++) {
      // String2.log("\n*** Table.testReadASCIISpeed attempt #" + attempt + "\n");
      Math2.gcAndWait("Table (between tests)"); // in a test
      Math2.sleep(5000);
      // time it
      long fileLength = File2.length(fileName); // was 1335204
      Test.ensureTrue(fileLength > 1335000, "fileName=" + fileName + " length=" + fileLength);
      time = System.currentTimeMillis();
      Table table = new Table();
      table.readASCII(fileName);
      time = System.currentTimeMillis() - time;

      String results = table.dataToString(3);
      String expected =
          "YY,MM,DD,hh,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS\n"
              + "90,01,01,00,161,08.6,10.7,01.50,05.00,04.80,999,1017.2,22.7,22.0,999.0,99.0\n"
              + "90,01,01,01,163,09.3,11.3,01.50,05.00,04.90,999,1017.3,22.7,22.0,999.0,99.0\n"
              + "90,01,01,01,164,09.2,10.6,01.60,04.80,04.90,999,1017.3,22.7,22.0,999.0,99.0\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      Test.ensureEqual(table.nColumns(), 16, "nColumns=" + table.nColumns());
      Test.ensureEqual(table.nRows(), 17117, "nRows=" + table.nRows());

      String2.log(
          "********** attempt #"
              + attempt
              + " Done.\n"
              + "cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + " (usual=2560 with StringHolder. With String, was 2711 Java 1.7M4700, was 648)"
              + "\ntime="
              + time
              + "ms (good=106ms, but slower when computer is busy.\n"
              + "  (was 101 Java 1.7M4700, was 422, java 1.5 was 719)");
      if (time <= 130) break;
    }
    // TODO get a better system for time based performance tests
    // if (time > 130)
    //     throw new SimpleException(
    //             "readASCII took too long (time=" + time + "ms > 130ms) (but often does when
    // computer is busy).");
  }

  /** Test the speed of readJson */
  @org.junit.jupiter.api.Test
  void testReadJsonSpeed() throws Exception {

    // warmup
    String fileName = TableTests.class.getResource("/data/cPostDet3.files.json.gz").getPath();
    long time = 0;
    String msg = "";
    String expected =
        "dirIndex,fileName,lastMod,sortedSpacing,unique_tag_id_min_,unique_tag_id_max_,PI_min_,PI_max_,longitude_min_,longitude_max_,latitude_min_,latitude_max_,time_min_,time_max_,bottom_depth_min_,bottom_depth_max_,common_name_min_,common_name_max_,date_public_min_,date_public_max_,line_min_,line_max_,position_on_subarray_min_,position_on_subarray_max_,project_min_,project_max_,riser_height_min_,riser_height_max_,role_min_,role_max_,scientific_name_min_,scientific_name_max_,serial_number_min_,serial_number_max_,stock_min_,stock_max_,surgery_time_min_,surgery_time_max_,surgery_location_min_,surgery_location_max_,tagger_min_,tagger_max_\n"
            + "0,52038_A69-1303_1059305.nc,1.284567715046E12,0.0,52038_A69-1303_1059305,52038_A69-1303_1059305,BARBARA BLOCK,BARBARA BLOCK,-146.36933,-146.1137,60.6426,60.7172,1.2192849E9,1.238062751E9,13.4146341463415,130.487804878049,SALMON SHARK,SALMON SHARK,1.273271649385E9,1.273271649385E9,,PORT GRAVINA,,9,HOPKINS MARINE STATION,HOPKINS MARINE STATION,,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,LAMNA DITROPIS,1059305,1059305,N/A,N/A,1.2192156E9,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\"PORT GRAVINA, PRINCE WILLIAM SOUND\",,\n"
            + "1,16955_A69-1303_8685G.nc,1.284567719796E12,-1.0,16955_A69-1303_8685G,16955_A69-1303_8685G,BARRY BEREJIKIAN,BARRY BEREJIKIAN,-122.78316,-122.78316,47.65223,47.65223,1.1466882E9,1.1466882E9,,,STEELHEAD,STEELHEAD,1.222730955645E9,1.222730955645E9,,,,,NOAA|NOAA FISHERIES,NOAA|NOAA FISHERIES,,,BEREJIKIAN_BARRY_ONCORHYNCHUS_MYKISS_BIGBEEFCREEK,BEREJIKIAN_BARRY_ONCORHYNCHUS_MYKISS_BIGBEEFCREEK,ONCORHYNCHUS MYKISS,ONCORHYNCHUS MYKISS,8685G,8685G,BIG BEEF CREEK,BIG BEEF CREEK,1.146528E9,1.146528E9,BIG BEEF CREEK,BIG BEEF CREEK,SKIP TEZAK,SKIP TEZAK\n"
            + "1,16956_A69-1303_8686G.nc,1.284567723515E12,-1.0,16956_A69-1303_8686G,16956_A69-1303_8686G,BARRY BEREJIKIAN,BARRY BEREJIKIAN,-122.78316,-122.78316,47.65223,47.65223,1.1466882E9,1.1466882E9,,,STEELHEAD,STEELHEAD,1.222730955653E9,1.222730955653E9,,,,,NOAA|NOAA FISHERIES,NOAA|NOAA FISHERIES,,,BEREJIKIAN_BARRY_ONCORHYNCHUS_MYKISS_BIGBEEFCREEK,BEREJIKIAN_BARRY_ONCORHYNCHUS_MYKISS_BIGBEEFCREEK,ONCORHYNCHUS MYKISS,ONCORHYNCHUS MYKISS,8686G,8686G,BIG BEEF CREEK,BIG BEEF CREEK,1.146528E9,1.146528E9,BIG BEEF CREEK,BIG BEEF CREEK,SKIP TEZAK,SKIP TEZAK\n"
            + "...\n";

    for (int attempt = 0; attempt < 3; attempt++) {
      // String2.log("\n*** Table.testReadJsonSpeed attempt#" + attempt + "\n");

      // time it
      time = System.currentTimeMillis();
      long fileLength = File2.length(fileName); // before gz was 10,166KB, now 574572
      Test.ensureTrue(fileLength > 574000, "fileName=" + fileName + " length=" + fileLength);
      Table table = new Table();
      table.readJson(fileName);

      String results = table.dataToString(3);
      Test.ensureEqual(results, expected, "results=" + results);
      Test.ensureTrue(results.indexOf("unique_tag_id_max") > 0, "test 1");
      Test.ensureTrue(results.indexOf("surgery_time_min") > 0, "test 2");
      Test.ensureTrue(table.nColumns() > 40, "nColumns=" + table.nColumns()); // was 42
      Test.ensureTrue(table.nRows() > 15000, "nRows=" + table.nRows()); // was 15024

      time = System.currentTimeMillis() - time;
      msg =
          "*** Done. cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + " (usual=2881 Java 1.7M4700, was 747)"
              + "\ntime="
              + time
              + "ms (usual=300, java 8 was 219, Java 1.7M4700, was 844, java 1.5 was 1687)";
      String2.log(msg);
      if (time <= 400) break;
    }
    // TODO get a better way to check for time performance
    // Test.ensureTrue(time < 400, msg + "\nreadJson took too long.");
  }

  /** Test the speed of readNDNc */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testReadNDNcSpeed() throws Exception {

    String fileName =
        TableTests.class
            .getResource("/veryLarge/points/ndbcMet2/historical/NDBC_41004_met.nc")
            .getPath();
    Table table = new Table();
    long time = 0;
    // String2.log(NcHelper.ncdump(fileName, "-h"));

    for (int attempt = 0; attempt < 3; attempt++) {
      // String2.log("\n*** Table.testReadNDNcSpeed attempt+" + attempt + "\n");
      Math2.gcAndWait("Table (between tests)"); // in a test

      // time it
      time = System.currentTimeMillis();
      long fileLength = File2.length(fileName); // was 20580000
      Test.ensureTrue(fileLength > 20570000, "fileName=" + fileName + " length=" + fileLength);
      table = new Table();
      table.readNDNc(fileName, null, 0, null, 0, 0); // standardizeWhat=0

      String results = table.dataToString(3);
      String expected = // before 2011-06-14 was 32.31, -75.35,
          "TIME,DEPTH,LAT,LON,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV,ID\n"
              + "2.678004E8,0.0,32.501,-79.099,255,1.3,-9999999.0,-9999999.0,-9999999.0,-9999999.0,32767,1020.5,27.2,27.4,-9999999.0,-9999999.0,-9999999.0,-9999999.0,1.3,0.3,41004\n"
              + "2.67804E8,0.0,32.501,-79.099,247,6.6,-9999999.0,-9999999.0,-9999999.0,-9999999.0,32767,1020.6,26.8,27.4,-9999999.0,-9999999.0,-9999999.0,-9999999.0,6.1,2.6,41004\n"
              + "2.678076E8,0.0,32.501,-79.099,249,7.0,-9999999.0,-9999999.0,-9999999.0,-9999999.0,32767,1020.4,26.8,27.4,-9999999.0,-9999999.0,-9999999.0,-9999999.0,6.5,2.5,41004\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      Test.ensureEqual(table.nColumns(), 21, "nColumns=" + table.nColumns());
      Test.ensureTrue(table.nRows() >= 351509, "nRows=" + table.nRows());

      time = System.currentTimeMillis() - time;
      String2.log(
          "********** Done. cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + " (usual=31414 Java 1.7M4700, was 9679)"
              + "\ntime="
              + time
              + "ms (usual=556 Lenovo, was 226 Java 1.7M4700, was 640, java 1.5 was 828, but varies a lot)");
      if (time <= 650) break;
    }
    // TODO get a better way to check for time performance
    // if (time > 650)
    //     throw new SimpleException("readNDNc took too long (time=" + time + "ms (556ms
    // expected))");
  }

  /** Test the speed of readOpendapSequence */
  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testReadOpendapSequenceSpeed() throws Exception {

    String url =
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet?"
            + "&time%3E=1999-01-01&time%3C=1999-04-01&station=%2241009%22";
    long time = 0;

    for (int attempt = 0; attempt < 3; attempt++) {
      // String2.log("\n*** Table.testReadOpendapSequenceSpeed\n");
      Math2.gcAndWait("Table (between tests)"); // in a test

      // time it
      time = System.currentTimeMillis();
      Table table = new Table();
      table.readOpendapSequence(url);
      String results = table.dataToString(3);
      String expected = // before 2011-06-14 was -80.17, 28.5
          // "station,longitude,latitude,time,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,wtmp,dewp,vis,ptdy,tide,wspu,wspv\n"
          // +
          // "41009,-80.166,28.519,9.151488E8,0,1.9,2.7,1.02,11.11,6.49,32767,1021.0,20.4,24.2,-9999999.0,-9999999.0,-9999999.0,-9999999.0,0.0,-1.9\n"
          // +
          // "41009,-80.166,28.519,9.151524E8,53,1.5,2.8,0.99,11.11,6.67,32767,1021.0,20.6,24.5,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-1.2,-0.9\n"
          // +
          // "41009,-80.166,28.519,9.15156E8,154,1.0,2.2,1.06,11.11,6.86,32767,1021.2,20.6,24.6,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-0.4,0.9\n"
          // +

          // source ASCII file has: (note 2 rows for ever hour)
          // YYYY MM DD hh WD WSPD GST WVHT DPD APD MWD BAR ATMP WTMP DEWP VIS [in
          // EditPlus, the first row of data is here, to right of col names -- screwy line
          // endings?!]
          // 1999 01 01 00 360 1.9 2.7 1.02 11.11 6.49 999 1021.0 20.4 24.2 999.0 99.0
          // 1999 01 01 00 21 1.4 3.4 1.10 11.11 6.82 999 1020.9 20.4 24.5 999.0 99.0
          // 1999 01 01 01 53 1.5 2.8 .99 11.11 6.67 999 1021.0 20.6 24.5 999.0 99.0
          // 1999 01 01 01 53 1.5 2.6 1.10 11.11 6.97 999 1021.1 20.6 24.5 999.0 99.0
          // 1999 01 01 02 154 1.0 2.2 1.06 11.11 6.86 999 1021.2 20.6 24.6 999.0 99.0
          // 1999 01 01 02 73 2.5 3.8 1.09 11.11 6.87 999 1021.2 20.7 24.6 999.0 99.0

          // 2020-03-03 this data changed significantly after big changes to processing
          // system/ dealing with duplicate lines (prefer newer data/later in file)
          "station,longitude,latitude,time,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,wtmp,dewp,vis,ptdy,tide,wspu,wspv\n"
              + "41009,-80.166,28.519,9.151488E8,21,1.4,3.4,1.1,11.11,6.82,32767,1020.9,20.4,24.5,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-0.5,-1.3\n"
              + // 1999-01-01T00:00
              "41009,-80.166,28.519,9.151524E8,53,1.5,2.6,1.1,11.11,6.97,32767,1021.1,20.6,24.5,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-1.2,-0.9\n"
              + // 1999-01-01T01:00
              "41009,-80.166,28.519,9.15156E8,73,2.5,3.8,1.09,11.11,6.87,32767,1021.2,20.7,24.6,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-2.4,-0.7\n"
              + // 1999-01-01T02:00
              "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Test.ensureTrue(table.nRows() > 2100, "nRows=" + table.nRows());
      time = System.currentTimeMillis() - time;
      String2.log(
          "********** Done. cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + " (usual(https)=33, was(http) 337 Java 1.7M4700, was 106)"
              + "\ntime="
              + time
              + "ms (usual(https)=1285, was http=600 since remote, was 128 Java 1.7M4700, was 406, java 1.5 was 562)");
      if (time <= 2000) break;
    }
    // TODO get a better way to check for time performance
    // if (time > 2000)
    //     throw new SimpleException("readOpendapSequence took too long (time=" + time + "ms).");
  }

  /** Test the speed of saveAs speed */
  @org.junit.jupiter.api.Test
  void testSaveAsSpeed() throws Exception {

    // warmup
    // String2.log("\n*** Table.testSaveAsSpeed\n");
    String sourceName =
        TableTests.class.getResource("/data/points/ndbcMet2HistoricalTxt/41009h1990.txt").getPath();
    String destName = File2.getSystemTempDirectory() + "testSaveAsSpeed";
    Table table = new Table();
    table.readASCII(sourceName);
    Test.ensureEqual(table.nColumns(), 16, "nColumns=" + table.nColumns());
    Test.ensureEqual(table.nRows(), 17117, "nRows=" + table.nRows());
    table.saveAsCsvASCII(destName + ".csv");
    table.saveAsJson(destName + ".json", table.findColumnNumber("time"), true); // writeUnits
    table.saveAsFlatNc(destName + ".nc", "row");
    long time = 0;

    for (int attempt = 0; attempt < 3; attempt++) {
      // time it
      // String2.log("\ntime it\n");

      // saveAsCsvASCII
      time = System.currentTimeMillis();
      table.saveAsCsvASCII(destName + ".csv");
      time = System.currentTimeMillis() - time;
      String2.log(
          "saveAsCsvASCII attempt#"
              + attempt
              + " done. cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + // 796
              "\ntime="
              + time
              + "ms  (expected=344, was 532 for Java 1.5 Dell)");
      File2.delete(destName + ".csv");
      if (time <= 550) break;
    }
    if (time > 550)
      throw new SimpleException(
          "saveAsCsvASCII  (time=" + time + "ms). Expected=~344 for 17117 rows.");

    for (int attempt = 0; attempt < 3; attempt++) {
      // saveAsJson
      time = System.currentTimeMillis();
      table.saveAsJson(destName + ".json", table.findColumnNumber("time"), true); // writeUnits
      time = System.currentTimeMillis() - time;
      String2.log(
          "saveAsJson attempt#"
              + attempt
              + " done. cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + // 974
              "\ntime="
              + time
              + "ms  (expect=281, was 515 for Java 1.5 Dell)");
      File2.delete(destName + ".json");
      if (time <= 450) break;
    }
    if (time >= 450)
      throw new SimpleException(
          "saveAsJson took too long (time=" + time + "ms). Expected=~281 for 17117 rows.");

    // saveAsFlatNc
    for (int attempt = 0; attempt < 3; attempt++) {
      time = System.currentTimeMillis();
      table.saveAsFlatNc(destName + ".nc", "row");
      time = System.currentTimeMillis() - time;
      String2.log(
          "saveAsFlatNc attempt#"
              + attempt
              + " done. cells/ms="
              + (table.nColumns() * table.nRows() / time)
              + // 2190
              "\ntime="
              + time
              + "ms  (expected=125, was 172 for Java 1.5 Dell)");
      File2.delete(destName + ".nc");
      if (time <= 200) break;
    }
    if (time > 200)
      throw new SimpleException(
          "saveAsFlatNc took too long (time=" + time + "ms). Expected=~125 for 17117 rows.");
  }

  /**
   * This is a test of readOpendap.
   *
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testOpendap() throws Exception {
    // *************
    // String2.log("\n*** Table.testOpendap");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // opendap, even sequence data, can be read via .nc
    // but constraints are not supported
    Table table = new Table();
    int nRows = 3779;
    table.readFlatNc(
        // read all via ascii:
        // "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?abund_m3,lat,long",
        // null);
        // or
        // "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?MOC1.abund_m3,MOC1.lat,MOC1.long",
        // null);
        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1",
        new String[] {"MOC1.abund_m3", "MOC1.lat", "MOC1.long"}, // but "MOC1." is required here
        0); // standardizeWhat=0
    // 2018-05-12 was unpack to doubles, so these tests will change
    // String2.log(table.toString(5));

    Test.ensureEqual(table.nColumns(), 3, "");
    Test.ensureEqual(table.nRows(), nRows, "");

    Test.ensureEqual(table.getColumnName(0), "abund_m3", "");
    Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Abundance m3", "");
    Test.ensureEqual(table.getDoubleData(0, 0), 0.242688983, "");
    Test.ensureEqual(table.getDoubleData(0, nRows - 1), 0.248962652, "");

    Test.ensureEqual(table.getColumnName(1), "lat", "");
    Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Latitude", "");
    Test.ensureEqual(table.getDoubleData(1, 0), 44.6517, "");
    Test.ensureEqual(table.getDoubleData(1, nRows - 1), 44.6517, "");

    Test.ensureEqual(table.getColumnName(2), "long", "");
    Test.ensureEqual(table.columnAttributes(2).getString("long_name"), "Longitude", "");
    Test.ensureEqual(table.getDoubleData(2, 0), -124.175, "");
    Test.ensureEqual(table.getDoubleData(2, nRows - 1), -124.65, "");

    // can it read with list of variables?
    table.readFlatNc(
        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long",
        null, // read all variables
        0); // standardizeWhat=0
    // 2018-05-12 was unpack to doubles, so these tests will change
    // String2.log(table.toString(5));
    Test.ensureEqual(table.nColumns(), 3, "");
    Test.ensureEqual(table.nRows(), nRows, "");

    // !!!HEY, the results are an unexpected order!!!
    Test.ensureEqual(table.getColumnName(0), "lat", "");
    Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Latitude", "");
    Test.ensureEqual(table.getDoubleData(0, 0), 44.6517, "");
    Test.ensureEqual(table.getDoubleData(0, nRows - 1), 44.6517, "");

    Test.ensureEqual(table.getColumnName(1), "long", "");
    Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Longitude", "");
    Test.ensureEqual(table.getDoubleData(1, 0), -124.175, "");
    Test.ensureEqual(table.getDoubleData(1, nRows - 1), -124.65, "");

    Test.ensureEqual(table.getColumnName(2), "abund_m3", "");
    Test.ensureEqual(table.columnAttributes(2).getString("long_name"), "Abundance m3", "");
    Test.ensureEqual(table.getDoubleData(2, 0), 0.242688983, "");
    Test.ensureEqual(table.getDoubleData(2, nRows - 1), 0.248962652, "");
  }

  /** Test join(). * */
  @org.junit.jupiter.api.Test
  void testJoin() {

    // *** testJoin 1
    // String2.log("\n*** Table.testJoin 1 column");
    Table table = new Table();
    table.addColumn("zero", PrimitiveArray.csvFactory(PAType.STRING, "a,b,c,d,,e"));
    table.addColumn("one", PrimitiveArray.csvFactory(PAType.INT, "40,10,12,30,,20"));
    table.addColumn("two", PrimitiveArray.csvFactory(PAType.STRING, "aa,bb,cc,dd,,ee"));
    table.columnAttributes(0).add("long_name", "hey zero");
    table.columnAttributes(1).add("missing_value", -99999);
    table.columnAttributes(2).add("long_name", "hey two");

    Table lut = new Table();
    lut.addColumn("aa", PrimitiveArray.csvFactory(PAType.INT, "10,20,30,40"));
    lut.addColumn("bb", PrimitiveArray.csvFactory(PAType.STRING, "11,22,33,44"));
    lut.addColumn("cc", PrimitiveArray.csvFactory(PAType.LONG, "111,222,333,444"));
    lut.columnAttributes(0).add("missing_value", -99999);
    lut.columnAttributes(1).add("long_name", "hey bb");
    lut.columnAttributes(2).add("missing_value", -9999999L);

    // test lut before join
    String results = lut.toString();
    String expectedLut =
        "{\n"
            + "dimensions:\n"
            + "\trow = 4 ;\n"
            + "\tbb_strlen = 2 ;\n"
            + "variables:\n"
            + "\tint aa(row) ;\n"
            + "\t\taa:missing_value = -99999 ;\n"
            + "\tchar bb(row, bb_strlen) ;\n"
            + "\t\tbb:long_name = \"hey bb\" ;\n"
            + "\tlong cc(row) ;\n"
            + "\t\tcc:missing_value = -9999999 ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "aa,bb,cc\n"
            + "10,11,111\n"
            + "20,22,222\n"
            + "30,33,333\n"
            + "40,44,444\n";
    Test.ensureEqual(results, expectedLut, "lut results=\n" + results);

    // do the join
    table.join(1, 1, "10", lut);

    results = table.toString();
    String expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 6 ;\n"
            + "\tzero_strlen = 1 ;\n"
            + "\tbb_strlen = 2 ;\n"
            + "\ttwo_strlen = 2 ;\n"
            + "variables:\n"
            + "\tchar zero(row, zero_strlen) ;\n"
            + "\t\tzero:long_name = \"hey zero\" ;\n"
            + "\tint one(row) ;\n"
            + "\t\tone:missing_value = -99999 ;\n"
            + "\tchar bb(row, bb_strlen) ;\n"
            + "\t\tbb:long_name = \"hey bb\" ;\n"
            + "\tlong cc(row) ;\n"
            + "\t\tcc:missing_value = -9999999 ;\n"
            + "\tchar two(row, two_strlen) ;\n"
            + "\t\ttwo:long_name = \"hey two\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "zero,one,bb,cc,two\n"
            + "a,40,44,444,aa\n"
            + "b,10,11,111,bb\n"
            + "c,12,,-9999999,cc\n"
            + "d,30,33,333,dd\n"
            + ",,11,111,\n"
            + "e,20,22,222,ee\n";
    Test.ensureEqual(results, expected, "join 1 results=\n" + results);

    // ensure lut unchanged
    results = lut.toString();
    Test.ensureEqual(results, expectedLut, "lut 1 results=\n" + results);

    // *** testJoin 2 columns
    // String2.log("\n*** Table.testJoin 2 columns");
    table = new Table();
    table.addColumn("zero", PrimitiveArray.csvFactory(PAType.STRING, "a,b,c,d,,e"));
    table.addColumn("one", PrimitiveArray.csvFactory(PAType.INT, "40,10,12,30,,20"));
    table.addColumn("two", PrimitiveArray.csvFactory(PAType.STRING, "44,bad,1212,33,,22"));
    table.addColumn("three", PrimitiveArray.csvFactory(PAType.STRING, "aaa,bbb,ccc,ddd,,eee"));
    table.columnAttributes(0).add("long_name", "hey zero");
    table.columnAttributes(1).add("missing_value", -99999);
    table.columnAttributes(2).add("long_name", "hey two");
    table.columnAttributes(3).add("long_name", "hey three");

    // do the join
    table.join(2, 1, "10\t11", lut);

    results = table.toString();
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 6 ;\n"
            + "\tzero_strlen = 1 ;\n"
            + "\ttwo_strlen = 4 ;\n"
            + "\tthree_strlen = 3 ;\n"
            + "variables:\n"
            + "\tchar zero(row, zero_strlen) ;\n"
            + "\t\tzero:long_name = \"hey zero\" ;\n"
            + "\tint one(row) ;\n"
            + "\t\tone:missing_value = -99999 ;\n"
            + "\tchar two(row, two_strlen) ;\n"
            + "\t\ttwo:long_name = \"hey two\" ;\n"
            + "\tlong cc(row) ;\n"
            + "\t\tcc:missing_value = -9999999 ;\n"
            + "\tchar three(row, three_strlen) ;\n"
            + "\t\tthree:long_name = \"hey three\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "zero,one,two,cc,three\n"
            + "a,40,44,444,aaa\n"
            + "b,10,bad,-9999999,bbb\n"
            + "c,12,1212,-9999999,ccc\n"
            + "d,30,33,333,ddd\n"
            + ",,,111,\n"
            + "e,20,22,222,eee\n";
    Test.ensureEqual(results, expected, "join 2 results=\n" + results);

    // ensure lut unchanged
    results = lut.toString();
    Test.ensureEqual(results, expectedLut, "lut 2 results=\n" + results);
  }

  /** test update() */
  @org.junit.jupiter.api.Test
  void testUpdate() throws Exception {
    Table table = new Table();
    table.addColumn("zero", PrimitiveArray.csvFactory(PAType.STRING, "a,    b,  c,  d,   ,  e"));
    table.addColumn("one", PrimitiveArray.csvFactory(PAType.INT, "10,  20, 30, 40,   , 50"));
    table.addColumn("two", PrimitiveArray.csvFactory(PAType.INT, "111,222,333,444,-99,555"));
    table.addColumn("three", PrimitiveArray.csvFactory(PAType.DOUBLE, "1.1,2.2,3.3,4.4,4.6,5.5"));
    table.columnAttributes(2).add("missing_value", -99);

    // otherTable rows: matches, matches, partial match, new
    // otherTable cols: keys, matches (but different type), doesn't match
    Table otherTable = new Table();
    otherTable.addColumn("one", PrimitiveArray.csvFactory(PAType.INT, " 50,   , 11,  5"));
    otherTable.addColumn("zero", PrimitiveArray.csvFactory(PAType.STRING, "  e,   ,  a,  f"));
    otherTable.addColumn("three", PrimitiveArray.csvFactory(PAType.INT, " 11, 22, 33, 44"));
    otherTable.addColumn("five", PrimitiveArray.csvFactory(PAType.INT, "  1,  2,  3,  4"));

    int nMatched = table.update(new String[] {"zero", "one"}, otherTable);
    String results = table.dataToString();
    String expected =
        "zero,one,two,three\n"
            + "a,10,111,1.1\n"
            + "b,20,222,2.2\n"
            + "c,30,333,3.3\n"
            + "d,40,444,4.4\n"
            + ",,-99,22.0\n"
            + "e,50,555,11.0\n"
            + "a,11,-99,33.0\n"
            + // -99 is from missing_value
            "f,5,-99,44.0\n"; // -99 is from missing_value
    Test.ensureEqual(results, expected, "update results=\n" + results);
    Test.ensureEqual(nMatched, 2, "nMatched");
  }

  /** This tests orderByMax, orderByMin, orderByMinMax */
  @org.junit.jupiter.api.Test
  void testOrderByMinMax() throws Exception {

    for (int proc = 0; proc < 3; proc++) {
      // String2.log("Table.testOrderBy" +
      // (proc == 1 ? "" : "Max") +
      // (proc == 0 ? "" : "Min"));

      // *** test #1
      PrimitiveArray substation =
          PrimitiveArray.csvFactory(PAType.INT, "10, 20, 20, 30, 10, 10, 10");
      PrimitiveArray station =
          PrimitiveArray.csvFactory(PAType.STRING, " a,  a,  a,  b,  c,  c,  c");
      PrimitiveArray time = PrimitiveArray.csvFactory(PAType.INT, " 1,  1,  2,  1,  1,  2,  3");
      PrimitiveArray other = PrimitiveArray.csvFactory(PAType.INT, "99, 95, 92, 91, 93, 98, 90");
      Table table = new Table();
      table.addColumn("substation", substation);
      table.addColumn("station", station);
      table.addColumn("time", time);
      table.addColumn("other", other);

      // unsort by sorting on 'other'
      table.ascendingSort(new int[] {3});

      // orderBy...
      String vars[] = new String[] {"station", "substation", "time"};
      if (proc == 0) table.orderByMax(vars);
      if (proc == 1) table.orderByMin(vars);
      if (proc == 2) table.orderByMinMax(vars);
      String results = table.dataToString();
      String expected[] =
          new String[] {
            "substation,station,time,other\n"
                + "10,a,1,99\n"
                + "20,a,2,92\n"
                + "30,b,1,91\n"
                + "10,c,3,90\n",
            "substation,station,time,other\n"
                + "10,a,1,99\n"
                + "20,a,1,95\n"
                + "30,b,1,91\n"
                + "10,c,1,93\n",
            "substation,station,time,other\n"
                + "10,a,1,99\n"
                + // note duplicate of 1 row of data
                "10,a,1,99\n"
                + "20,a,1,95\n"
                + "20,a,2,92\n"
                + "30,b,1,91\n"
                + // note duplicate
                "30,b,1,91\n"
                + "10,c,1,93\n"
                + "10,c,3,90\n"
          };
      Test.ensureEqual(results, expected[proc], "proc=" + proc + " results=\n" + results);

      // *** test #2
      PrimitiveArray time2 = PrimitiveArray.csvFactory(PAType.INT, " 1,  1,  2,  1,  1,  2,  3");
      PrimitiveArray other2 = PrimitiveArray.csvFactory(PAType.INT, "99, 95, 92, 91, 93, 98, 90");
      table = new Table();
      table.addColumn("time", time2);
      table.addColumn("other", other2);

      // unsort by sorting on 'other'
      table.ascendingSort(new int[] {1});

      // orderBy...
      vars = new String[] {"time"};
      if (proc == 0) table.orderByMax(vars);
      if (proc == 1) table.orderByMin(vars);
      if (proc == 2) table.orderByMinMax(vars);
      results = table.dataToString();
      expected =
          new String[] {
            "time,other\n" + "3,90\n",
            "time,other\n"
                + "1,91\n", // for ties, the one that is picked isn't specified. unsort above causes
            // 91 to
            // be first.
            "time,other\n"
                + "1,91\n"
                + // for ties, the one that is picked isn't specified. unsort above causes 91 to
                // be first.
                "3,90\n"
          };

      Test.ensureEqual(results, expected[proc], "proc=" + proc + " results=\n" + results);

      // *** test #3
      PrimitiveArray time3 = PrimitiveArray.csvFactory(PAType.INT, " 1");
      PrimitiveArray other3 = PrimitiveArray.csvFactory(PAType.INT, "99");
      table = new Table();
      table.addColumn("time", time3);
      table.addColumn("other", other3);

      // unsort by sorting on 'other'
      table.ascendingSort(new int[] {1});

      // orderBy...
      vars = new String[] {"time"};
      if (proc == 0) table.orderByMax(vars);
      if (proc == 1) table.orderByMin(vars);
      if (proc == 2) table.orderByMinMax(vars);
      results = table.dataToString();
      expected =
          new String[] {
            "time,other\n" + "1,99\n",
            "time,other\n" + "1,99\n",
            "time,other\n" + "1,99\n" + "1,99\n"
          };
      Test.ensureEqual(results, expected[proc], "proc=" + proc + " results=\n" + results);

      // *** test #4
      PrimitiveArray time4 = new IntArray();
      PrimitiveArray other4 = new IntArray();
      table = new Table();
      table.addColumn("time", time4);
      table.addColumn("other", other4);

      // orderBy...
      vars = new String[] {"time"};
      if (proc == 0) table.orderByMax(vars);
      if (proc == 1) table.orderByMin(vars);
      if (proc == 2) table.orderByMinMax(vars);
      results = table.dataToString();
      String expected4 = "time,other\n";
      Test.ensureEqual(results, expected4, "proc=" + proc + " results=\n" + results);
    }
  }

  /** This tests orderByClosest. */
  @org.junit.jupiter.api.Test
  void testOrderByClosest() throws Exception {
    // String2.log("\n*** Table.testOrderByClosest()");

    // regular: 2 minutes
    // String2.log("\nTest 2 minutes");
    StringArray sar =
        new StringArray(new String[] {"b", "b", "b", "b", "b", "b", "c", "a", "d", "a"});
    DoubleArray dar =
        new DoubleArray(new double[] {-121, -100, Double.NaN, 110, 132, -2, 1e30, 132, 1e30, 125});
    IntArray iar = new IntArray(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    Table table = new Table();
    table.addColumn("iar", iar);
    table.addColumn("sar", sar);
    table.addColumn("dar", dar);
    table.orderByClosest("sar, dar, 2 minutes");
    String results = table.dataToString();
    String expected =
        "iar,sar,dar\n"
            + "9,a,125.0\n"
            + "0,b,-121.0\n"
            + "5,b,-2.0\n"
            + "3,b,110.0\n"
            + "6,c,1.0E30\n"
            + "8,d,1.0E30\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 2 months:
    // note that Jan is the 0th month: so 2 months rounds to Jan 1, Mar 1, May 1
    // String2.log("\nTest 2 months");
    sar = new StringArray(new String[] {"b", "b", "b", "b", "b", "b", "c", "a", "d", "a"});
    String sa[] = {
      "-0002-08-28", // 0 b -121
      "-0002-09-28", // 1 b -100
      "", // 2 b NaN
      "2014-06-25", // 3 b 110,
      "2014-07-25", // 4 b 132,
      "-912345-12-28", // 5 b -2,
      "2010-04-05", // 6 c 82,
      "2016-09-25", // 7 a 132,
      "2010-04-05", // 8 d 82,
      "2016-09-10"
    }; // 9 a 125});
    dar = new DoubleArray();
    for (int i = 0; i < 10; i++) dar.add(Calendar2.safeIsoStringToEpochSeconds(sa[i]));
    iar = new IntArray(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    table.clear();
    table.addColumn("iar", iar);
    table.addColumn("sar", sar);
    table.addColumn("dar", dar);
    table.orderByClosest("sar, dar, 2 months");
    StringArray sar2 = new StringArray();
    for (int i = 0; i < dar.size(); i++)
      sar2.add(Calendar2.safeEpochSecondsToIsoStringTZ(dar.get(i), ""));
    table.setColumn(2, sar2);
    results = table.dataToString();
    expected =
        "iar,sar,dar\n"
            + "9,a,2016-09-10T00:00:00Z\n"
            + "5,b,-912345-12-28T00:00:00Z\n"
            + "0,b,-0002-08-28T00:00:00Z\n"
            + "3,b,2014-06-25T00:00:00Z\n"
            + "6,c,2010-04-05T00:00:00Z\n"
            + "8,d,2010-04-05T00:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 10 years: beware BC AD transition, see Calendar2.getYear
    // String2.log("\nTest 10 years");
    sar = new StringArray(new String[] {"b", "b", "b", "b", "b", "b", "c", "a", "d", "a"});
    sa =
        new String[] {
          "-0002-12-30", // 0 b -121
          "0004-01-14", // 1 b -100
          "", // 2 b NaN
          "2018-06-25", // 3 b 110,
          "2024-07-25", // 4 b 132,
          "-912345-12-28", // 5 b -2,
          "0211-04-05", // 6 c 82,
          "2024-09-25", // 7 a 132,
          "0211-04-05", // 8 d 82,
          "2023-09-10"
        }; // 9 a 125});
    dar = new DoubleArray();
    for (int i = 0; i < 10; i++) dar.add(Calendar2.safeIsoStringToEpochSeconds(sa[i]));
    iar = new IntArray(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    table.clear();
    table.addColumn("iar", iar);
    table.addColumn("sar", sar);
    table.addColumn("dar", dar);
    table.orderByClosest("sar, dar, 10 years");
    sar2 = new StringArray();
    for (int i = 0; i < dar.size(); i++)
      sar2.add(Calendar2.safeEpochSecondsToIsoStringTZ(dar.get(i), ""));
    table.setColumn(2, sar2);
    results = table.dataToString();
    expected =
        "iar,sar,dar\n"
            + "9,a,2023-09-10T00:00:00Z\n"
            + "5,b,-912345-12-28T00:00:00Z\n"
            + "0,b,-0002-12-30T00:00:00Z\n"
            + "3,b,2018-06-25T00:00:00Z\n"
            + "6,c,0211-04-05T00:00:00Z\n"
            + "8,d,0211-04-05T00:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests orderByCount. */
  @org.junit.jupiter.api.Test
  void testOrderByCount() throws Exception {
    // String2.log("\n*** Table.testOrderByCount()");

    // test 2 orderyBy variables
    ShortArray shar =
        new ShortArray(new short[] {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 2, 2, 2});
    StringArray sar =
        new StringArray(
            new String[] {"b", "b", "b", "b", "b", "b", "c", "a", "d", "a", "c", "a", ""});
    DoubleArray dar =
        new DoubleArray(
            new double[] {
              -121, -100, Double.NaN, 110, 132, -2, 1e30, 132, 1e30, 125, 1.1, 1.2, 1.3
            });
    ByteArray bar =
        (ByteArray)
            new ByteArray(
                    new byte[] { // 127=NaN
                      0, 127, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
                    })
                .setMaxIsMV(true);
    Table table = new Table();
    table.addColumn("shar", shar);
    table.addColumn("bar", bar);
    table.addColumn("sar", sar);
    table.addColumn("dar", dar);
    table.orderByCount(new String[] {"shar", "sar"});
    String results = table.toString();
    String expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 7 ;\n"
            + "\tsar_strlen = 1 ;\n"
            + "variables:\n"
            + "\tshort shar(row) ;\n"
            + "\tint bar(row) ;\n"
            + "\t\tbar:units = \"count\" ;\n"
            + "\tchar sar(row, sar_strlen) ;\n"
            + "\tint dar(row) ;\n"
            + "\t\tdar:units = \"count\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "shar,bar,sar,dar\n"
            + "2,1,,1\n"
            + "2,1,a,1\n"
            + "2,1,c,1\n"
            + "100,2,a,2\n"
            + "100,5,b,5\n"
            + "100,1,c,1\n"
            + "100,1,d,1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test 0 orderBy variables
    sar =
        new StringArray(
            new String[] {"b", "b", "b", "b", "b", "b", "c", "a", "d", "a", "c", "a", ""});
    dar =
        new DoubleArray(
            new double[] {
              -121, -100, Double.NaN, 110, 132, -2, 1e30, 132, 1e30, 125, 1.1, 1.2, 1.3
            });
    bar =
        (ByteArray)
            new ByteArray(
                    new byte[] { // 127=NaN
                      0, 127, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
                    })
                .setMaxIsMV(true);
    table = new Table();
    table.addColumn("bar", bar);
    table.addColumn("sar", sar);
    table.addColumn("dar", dar);
    table.orderByCount(new String[] {});
    results = table.toString();
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 1 ;\n"
            + "variables:\n"
            + "\tint bar(row) ;\n"
            + "\t\tbar:units = \"count\" ;\n"
            + "\tint sar(row) ;\n"
            + "\t\tsar:units = \"count\" ;\n"
            + "\tint dar(row) ;\n"
            + "\t\tdar:units = \"count\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "bar,sar,dar\n"
            + "12,12,12\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests orderByLimit. */
  @org.junit.jupiter.api.Test
  void testOrderByLimit() throws Exception {
    // String2.log("\n*** Table.testOrderByLimit()");

    StringArray sar =
        new StringArray(new String[] {"b", "b", "b", "b", "b", "b", "c", "a", "d", "a"});
    IntArray iar = new IntArray(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    Table table = new Table();
    table.addColumn("iar", iar);
    table.addColumn("sar", sar);
    table.orderByLimit("sar, 2");
    String results = table.dataToString();
    String expected = "iar,sar\n" + "7,a\n" + "9,a\n" + "0,b\n" + "1,b\n" + "6,c\n" + "8,d\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests getDapQueryParts. */
  @org.junit.jupiter.api.Test
  void testGetDapQueryParts() throws Exception {
    // String2.log("\n*** Table.testGetDapQueryParts");

    // test Table.getDapQueryParts
    Test.ensureEqual(Table.getDapQueryParts(null), new String[] {""}, "");
    Test.ensureEqual(Table.getDapQueryParts(""), new String[] {""}, "");
    Test.ensureEqual(Table.getDapQueryParts("  "), new String[] {"  "}, "");
    Test.ensureEqual(Table.getDapQueryParts("ab%3dc"), new String[] {"ab=c"}, "");
    Test.ensureEqual(Table.getDapQueryParts("a&b&c"), new String[] {"a", "b", "c"}, "");
    Test.ensureEqual(Table.getDapQueryParts("&&"), new String[] {"", "", ""}, "");
    Test.ensureEqual(
        Table.getDapQueryParts("a&b=R%26D"), new String[] {"a", "b=R&D"}, ""); // & visible
    Test.ensureEqual(
        Table.getDapQueryParts("a%26b=\"R%26D\""),
        new String[] {"a", "b=\"R&D\""},
        ""); // & encoded
    Test.ensureEqual(
        Table.getDapQueryParts("a%26b=\"R%26D\"%26c"),
        new String[] {"a", "b=\"R&D\"", "c"},
        ""); // &
    // encoded
    Test.ensureEqual(Table.getDapQueryParts("a%26b%3dR-D"), new String[] {"a", "b=R-D"}, "");

    // *** test getDapQueryParts (decoded) with invalid queries
    String error = "";
    try {
      Table.getDapQueryParts("a%26b=\"R%26D"); // decoded. unclosed "

    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }

    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: A closing doublequote is missing.",
        "error=" + error);

    // String2.log("\n*** Table.testGetDapQueryParts succeeded");
  }

  /** This tests subsetViaDapQuery. */
  @org.junit.jupiter.api.Test
  void testSubsetViaDapQuery() throws Exception {
    // String2.log("\n*** Table.testSubsetViaDapQuery");
    String results, expected;
    Table table;

    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("");
    results = table.dataToString();
    expected =
        "Time,Longitude,Latitude,Double Data,Int Data,Short Data,Byte Data,Char Data,String Data\n"
            + "0.0,-3,1.0,-1.0E300,-2000000000,-32000,-120,\",\",a\n"
            + "1.125504062E9,-2,1.5,3.123,2,7,8,\"\"\"\",bb\n"
            + "1.130954649E9,-1,2.0,1.0E300,2000000000,32000,120,\\u20ac,ccc\n"
            + ",,,,,,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !=NaN
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("&Latitude!=NaN");
    results = table.dataToString();
    expected =
        "Time,Longitude,Latitude,Double Data,Int Data,Short Data,Byte Data,Char Data,String Data\n"
            + "0.0,-3,1.0,-1.0E300,-2000000000,-32000,-120,\",\",a\n"
            + "1.125504062E9,-2,1.5,3.123,2,7,8,\"\"\"\",bb\n"
            + "1.130954649E9,-1,2.0,1.0E300,2000000000,32000,120,\\u20ac,ccc\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // =NaN
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("&Latitude=NaN");
    results = table.dataToString();
    expected =
        "Time,Longitude,Latitude,Double Data,Int Data,Short Data,Byte Data,Char Data,String Data\n"
            + ",,,,,,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 1125504062 seconds since 1970-01-01T00:00:00Z = 2005-08-31T16:01:02Z
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("&Time>2005-08-31T16:01:02Z");
    results = table.dataToString();
    expected =
        "Time,Longitude,Latitude,Double Data,Int Data,Short Data,Byte Data,Char Data,String Data\n"
            + "1.130954649E9,-1,2.0,1.0E300,2000000000,32000,120,\\u20ac,ccc\n";
    // mv row removed because tests related to NaN (except NaN=NaN) return false.
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 1125504062 seconds since 1970-01-01T00:00:00Z = 2005-08-31T16:01:02Z
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("String Data,Time&Time>=2005-08-31T16:01:02Z");
    results = table.dataToString();
    expected = "String Data,Time\n" + "bb,1.125504062E9\n" + "ccc,1.130954649E9\n";
    // mv row removed because tests related to NaN (except NaN=NaN) return false.
    Test.ensureEqual(results, expected, "results=\n" + results);

    // constraint var needn't be in resultsVars
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("String Data&Time>=2005-08-31T16:01:02Z");
    results = table.dataToString();
    expected = "String Data\n" + "bb\n" + "ccc\n";
    // mv row removed because tests related to NaN (except NaN=NaN) return false.
    Test.ensureEqual(results, expected, "results=\n" + results);

    // return 0 rows
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("Longitude,Time&Time=2005-08-31");
    results = table.dataToString();
    expected = "Longitude,Time\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // string
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("String Data,Time&Time>1970-01-01&String Data=\"bb\"");
    results = table.dataToString();
    expected = "String Data,Time\n" + "bb,1.125504062E9\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // regex
    table = getTestTable(false, true); // includeLongs, Strings
    table.subsetViaDapQuery("String Data,Time&String Data=~\"b{1,5}\"");
    results = table.dataToString();
    expected = "String Data,Time\n" + "bb,1.125504062E9\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // String2.log("\n*** Table.testSubsetViaDapQuery finished successfully");
  }

  /** This tests parseDapQuery. */
  @org.junit.jupiter.api.Test
  void testParseDapQuery() throws Exception {
    // String2.log("\n*** Table.testParseDapQuery");

    Table table =
        Table.makeEmptyTable(
            new String[] {"myDouble", "myString", "time", "myInt"},
            new String[] {"double", "String", "double", "int"});
    table.columnAttributes("time").add("units", Calendar2.SECONDS_SINCE_1970);
    StringArray rVar = new StringArray();
    StringArray cVar = new StringArray();
    StringArray cOp = new StringArray();
    StringArray cVal = new StringArray();
    String results;

    // *** test valid queries
    table.parseDapQuery("", rVar, cVar, cOp, cVal, false); // fix
    Test.ensureEqual(rVar.toString(), "myDouble, myString, time, myInt", "");
    Test.ensureEqual(cVar.toString(), "", "");
    Test.ensureEqual(cOp.toString(), "", "");
    Test.ensureEqual(cVal.toString(), "", "");

    table.parseDapQuery("myString,myDouble", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myString, myDouble", "");
    Test.ensureEqual(cVar.toString(), "", "");
    Test.ensureEqual(cOp.toString(), "", "");
    Test.ensureEqual(cVal.toString(), "", "");

    table.parseDapQuery("myDouble&myString=\"something\"", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myDouble", "");
    Test.ensureEqual(cVar.toString(), "myString", "");
    Test.ensureEqual(cOp.toString(), "=", "");
    Test.ensureEqual(cVal.toString(), "something", "");

    table.parseDapQuery("myDouble&myInt=~\"regex\"", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myDouble", "");
    Test.ensureEqual(cVar.toString(), "myInt", "");
    Test.ensureEqual(cOp.toString(), "=~", "");
    Test.ensureEqual(cVal.toString(), "regex", "");

    table.parseDapQuery("myDouble&time>=1.4229216E9", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myDouble", "");
    Test.ensureEqual(cVar.toString(), "time", "");
    Test.ensureEqual(cOp.toString(), ">=", "");
    Test.ensureEqual(cVal.toString(), "1.4229216E9", "");

    table.parseDapQuery("myDouble&time>=2015-02-03", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myDouble", "");
    Test.ensureEqual(cVar.toString(), "time", "");
    Test.ensureEqual(cOp.toString(), ">=", "");
    Test.ensureEqual(cVal.toString(), "1.4229216E9", "");

    table.parseDapQuery("myDouble&myInt=NaN", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myDouble", "");
    Test.ensureEqual(cVar.toString(), "myInt", "");
    Test.ensureEqual(cOp.toString(), "=", "");
    Test.ensureEqual(cVal.toString(), "NaN", "");

    table.parseDapQuery(
        "&time!=2015-02-03&myInt<5.1&myString=\"Nate\"&orderBy(\"myString,myInt\")&distinct()",
        rVar,
        cVar,
        cOp,
        cVal,
        false);
    Test.ensureEqual(rVar.toString(), "myDouble, myString, time, myInt", "");
    Test.ensureEqual(cVar.toString(), "time, myInt, myString", "");
    Test.ensureEqual(cOp.toString(), "!=, <, =", "");
    Test.ensureEqual(cVal.toString(), "1.4229216E9, 5.1, Nate", "");

    // s
    table.parseDapQuery("s", rVar, cVar, cOp, cVal, false); // fix
    Test.ensureEqual(rVar.toString(), "myDouble, myString, time, myInt", "");
    Test.ensureEqual(cVar.toString(), "", "");
    Test.ensureEqual(cOp.toString(), "", "");
    Test.ensureEqual(cVal.toString(), "", "");

    table.parseDapQuery("s.myDouble&s.time>=2015-02-03", rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "myDouble", "");
    Test.ensureEqual(cVar.toString(), "time", "");
    Test.ensureEqual(cOp.toString(), ">=", "");
    Test.ensureEqual(cVal.toString(), "1.4229216E9", "");

    table.parseDapQuery(
        "&s.time!=2015-02-03&s.myInt<5.1&s.myString=\"Nate\"&orderBy(\"s.myString,s.myInt\")&distinct()",
        rVar,
        cVar,
        cOp,
        cVal,
        false);
    Test.ensureEqual(rVar.toString(), "myDouble, myString, time, myInt", "");
    Test.ensureEqual(cVar.toString(), "time, myInt, myString", "");
    Test.ensureEqual(cOp.toString(), "!=, <, =", "");
    Test.ensureEqual(cVal.toString(), "1.4229216E9, 5.1, Nate", "");

    // Tests of time related to "now"
    double now = System.currentTimeMillis() / 1000.0;
    // String2.log("epochSecondsNow=" + now);
    table.parseDapQuery(
        "time&time>now&time>now%2Bsecond&time>now-minute&time>now-44days",
        rVar, cVar, cOp, cVal, false);
    Test.ensureEqual(rVar.toString(), "time", "");
    Test.ensureEqual(cVar.toString(), "time, time, time, time", "");
    Test.ensureEqual(cOp.toString(), ">, >, >, >", "");
    Test.ensureTrue(Math.abs(cVal.getDouble(0) - now) < 20, cVal.get(0));
    Test.ensureTrue(Math.abs(cVal.getDouble(1) - (now + 1)) < 20, cVal.get(1));
    Test.ensureTrue(Math.abs(cVal.getDouble(2) - (now - 60)) < 20, cVal.get(2));
    Test.ensureTrue(Math.abs(cVal.getDouble(3) - (now - 44 * 86400)) < 20, cVal.get(3));

    // ********* tests that fail
    results = "invalid request var";
    try {
      table.parseDapQuery("time,MyInt", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Unrecognized variable=\"MyInt\".",
        "");

    results = "listed twice";
    try {
      table.parseDapQuery("time,myInt,time", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: variable=time is listed twice in the results variables list.",
        "");

    results = "invalid constraint var";
    try {
      table.parseDapQuery("time&MyDouble>5", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Unrecognized constraint variable=\"MyDouble\".",
        "");

    results = "missing &";
    try {
      table.parseDapQuery("myInt>5", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: All constraints (including \"myInt>...\") must be preceded by '&'.",
        "");

    results = "missing & with date with double col";
    try {
      table.parseDapQuery("myDouble>2014-01-01T00:00:00Z", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: All constraints (including \"myDouble>...\") must be preceded by '&'.",
        "");

    results = "invalid op";
    try {
      table.parseDapQuery("&myInt==5", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Use '=' instead of '==' in constraints.",
        "");

    results = "invalid NAN value";
    try {
      table.parseDapQuery("&myInt=NAN", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Numeric tests of NaN must use \"NaN\", not value=\"NAN\".",
        "");

    results = "invalid NaN value, something";
    try {
      table.parseDapQuery("&myInt=something", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Numeric tests of NaN must use \"NaN\", not value=\"something\".",
        "");

    results = "invalid NaN value";
    try {
      table.parseDapQuery("&myInt=1e1000", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Numeric tests of NaN must use \"NaN\", not value=\"1e1000\".",
        "");

    results = "invalid regex op";
    try {
      table.parseDapQuery("&myInt~=\"regex\"", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Use '=~' instead of '~=' in constraints.",
        "");

    results = "missing \"\" for string";
    try {
      table.parseDapQuery("&myString=something", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: For constraints of String variables, the right-hand-side value must be surrounded by double quotes.\n"
            + "Bad constraint: myString=something",
        "results=" + results);

    results = "missing \"\" for regex";
    try {
      table.parseDapQuery("&myInt=~something", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: For =~ constraints of numeric variables, the right-hand-side value must be surrounded by double quotes.\n"
            + "Bad constraint: myInt=~something",
        "results=" + results);

    results = "invalid now units";
    try {
      table.parseDapQuery("&time>now-2dayss", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Invalid \"now\" constraint: \"now-2dayss\". "
            + "Timestamp constraints with \"now\" must be in the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).",
        "results=" + results);

    results = "invalid now option -";
    try {
      table.parseDapQuery("&time>now-", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Invalid \"now\" constraint: \"now-\". "
            + "Timestamp constraints with \"now\" must be in the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).",
        "results=" + results);

    results = "invalid now option -+";
    try {
      table.parseDapQuery("&time>now-%2B4seconds", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Invalid \"now\" constraint: \"now-+4seconds\". "
            + "Timestamp constraints with \"now\" must be in the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).",
        "results=" + results);

    results = "invalid now option +-";
    try {
      table.parseDapQuery("&time>now%2B-4seconds", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Invalid \"now\" constraint: \"now+-4seconds\". "
            + "Timestamp constraints with \"now\" must be in the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).",
        "results=" + results);

    results = "invalid now option";
    try {
      table.parseDapQuery("&time>now=2days", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: Unrecognized constraint variable=\"time>now\".",
        "results=" + results);

    results = "unknown filter";
    try {
      table.parseDapQuery("&Distinct()", rVar, cVar, cOp, cVal, false);
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureEqual(
        results,
        "com.cohort.util.SimpleException: Query error: No operator found in constraint=\"Distinct()\".",
        "");

    // String2.log("\n*** Table.testParseDapQuery finished successfully.");
  }

  @org.junit.jupiter.api.Test
  void testAddIndexColumns() throws Exception {
    Table table = new Table();
    table.addIndexColumns(new int[] {3, 2, 4});
    String results = table.dataToString();
    String expected =
        "_index_0,_index_1,_index_2\n"
            + "0,0,0\n"
            + "0,0,1\n"
            + "0,0,2\n"
            + "0,0,3\n"
            + "0,1,0\n"
            + "0,1,1\n"
            + "0,1,2\n"
            + "0,1,3\n"
            + "1,0,0\n"
            + "1,0,1\n"
            + "1,0,2\n"
            + "1,0,3\n"
            + "1,1,0\n"
            + "1,1,1\n"
            + "1,1,2\n"
            + "1,1,3\n"
            + "2,0,0\n"
            + "2,0,1\n"
            + "2,0,2\n"
            + "2,0,3\n"
            + "2,1,0\n"
            + "2,1,1\n"
            + "2,1,2\n"
            + "2,1,3\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests readMultidimNc by reading an Argo Profile file. */
  @org.junit.jupiter.api.Test
  void testReadMultidimNc() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebugMode = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadMultidimNc");
    Table table = new Table();
    // ftp://ftp.ifremer.fr/ifremer/argo/dac/csio/2901175/2901175_prof.nc
    String fiName = TableTests.class.getResource("/data/nc/2901175_prof.nc").getPath();
    // String2.log(NcHelper.ncdump(fiName, "-h"));
    String results, expectedStart, expectedEnd;
    /* */

    // ** don't specify varNames or dimNames -- it find vars with most dims
    TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
    reader.readMultidimNc(
        fiName,
        new StringArray(),
        new StringArray(),
        null,
        true,
        0,
        false, // readMetadata, standardizeWhat=0, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart =
        // static vars and vars like char SCIENTIFIC_CALIB_COEFFICIENT(N_PROF=254,
        // N_CALIB=1, N_PARAM=3, STRING256=256);
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,STATION_PARAMETERS,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PARAMETER,SCIENTIFIC_CALIB_EQUATION,SCIENTIFIC_CALIB_COEFFICIENT,SCIENTIFIC_CALIB_COMMENT,SCIENTIFIC_CALIB_DATE\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PRES,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PRES,PRES_ADJUSTED = PRES - dP,dP =  0.1 dbar.,Pressures adjusted by using pressure offset at the sea surface. The quoted error is manufacturer specified accuracy in dbar.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,TEMP,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,TEMP,none,none,The quoted error is manufacturer specified accuracy with respect to ITS-90 at time of laboratory calibration.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PSAL,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PSAL,\"PSAL_ADJUSTED = sw_salt( sw_cndr(PSAL,TEMP,PRES), TEMP, PRES_ADJUSTED ); PSAL_ADJ corrects conductivity cell therm mass (CTM), Johnson et al, 2007, JAOT;\",\"same as for PRES_ADJUSTED; CTL: alpha=0.0267, tau=18.6;\",No significant salinity drift detected; SBE sensor accuracy,20110628060155\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 762, "nRows"); // 254*3

    // * same but quick reject based on constraint
    reader.readMultidimNc(
        fiName,
        new StringArray(),
        new StringArray(),
        null,
        true,
        0,
        false, // readMetadata, standardizeWhat=0, removeMVRows
        StringArray.fromCSV("FORMAT_VERSION,FORMAT_VERSION"), // conVars
        StringArray.fromCSV("=,="), // conOps
        StringArray.fromCSV("3.1,3.2")); // conVals
    Test.ensureEqual(table.nRows(), 0, "nRows");

    // * test don't removeMVRows
    reader.readMultidimNc(
        fiName,
        null,
        StringArray.fromCSV("ZZTOP, N_PROF, N_LEVELS"),
        null,
        true,
        0,
        false, // readMetadata, standardizeWhat=0, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.toString(3);
    expectedStart =
        "{\n"
            + "dimensions:\n"
            + "\trow = 18034 ;\n"
            + "\tDATA_TYPE_strlen = 12 ;\n"
            + "\tFORMAT_VERSION_strlen = 3 ;\n"
            + "\tHANDBOOK_VERSION_strlen = 3 ;\n"
            + "\tREFERENCE_DATE_TIME_strlen = 14 ;\n"
            + "\tDATE_CREATION_strlen = 14 ;\n"
            + "\tDATE_UPDATE_strlen = 14 ;\n"
            + "\tPLATFORM_NUMBER_strlen = 7 ;\n"
            + "\tPROJECT_NAME_strlen = 18 ;\n"
            + "\tPI_NAME_strlen = 11 ;\n"
            + "\tDATA_CENTRE_strlen = 2 ;\n"
            + "\tDC_REFERENCE_strlen = 14 ;\n"
            + "\tDATA_STATE_INDICATOR_strlen = 2 ;\n"
            + "\tPLATFORM_TYPE_strlen = 4 ;\n"
            + "\tFLOAT_SERIAL_NO_strlen = 13 ;\n"
            + "\tFIRMWARE_VERSION_strlen = 6 ;\n"
            + "\tWMO_INST_TYPE_strlen = 3 ;\n"
            + "\tPOSITIONING_SYSTEM_strlen = 5 ;\n"
            + "\tVERTICAL_SAMPLING_SCHEME_strlen = 26 ;\n"
            + "variables:\n"
            + "\tchar DATA_TYPE(row, DATA_TYPE_strlen) ;\n"
            + "\t\tDATA_TYPE:conventions = \"Argo reference table 1\" ;\n"
            + "\t\tDATA_TYPE:long_name = \"Data type\" ;\n"
            + "\tchar FORMAT_VERSION(row, FORMAT_VERSION_strlen) ;\n"
            + "\t\tFORMAT_VERSION:long_name = \"File format version\" ;\n"
            + "\tchar HANDBOOK_VERSION(row, HANDBOOK_VERSION_strlen) ;\n"
            + "\t\tHANDBOOK_VERSION:long_name = \"Data handbook version\" ;\n"
            + "\tchar REFERENCE_DATE_TIME(row, REFERENCE_DATE_TIME_strlen) ;\n"
            + "\t\tREFERENCE_DATE_TIME:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tREFERENCE_DATE_TIME:long_name = \"Date of reference for Julian days\" ;\n"
            + "\tchar DATE_CREATION(row, DATE_CREATION_strlen) ;\n"
            + "\t\tDATE_CREATION:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tDATE_CREATION:long_name = \"Date of file creation\" ;\n"
            + "\tchar DATE_UPDATE(row, DATE_UPDATE_strlen) ;\n"
            + "\t\tDATE_UPDATE:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tDATE_UPDATE:long_name = \"Date of update of this file\" ;\n"
            + "\tchar PLATFORM_NUMBER(row, PLATFORM_NUMBER_strlen) ;\n"
            + "\t\tPLATFORM_NUMBER:conventions = \"WMO float identifier : A9IIIII\" ;\n"
            + "\t\tPLATFORM_NUMBER:long_name = \"Float unique identifier\" ;\n"
            + "\tchar PROJECT_NAME(row, PROJECT_NAME_strlen) ;\n"
            + "\t\tPROJECT_NAME:long_name = \"Name of the project\" ;\n"
            + "\tchar PI_NAME(row, PI_NAME_strlen) ;\n"
            + "\t\tPI_NAME:long_name = \"Name of the principal investigator\" ;\n"
            + "\tint CYCLE_NUMBER(row) ;\n"
            + "\t\tCYCLE_NUMBER:_FillValue = 99999 ;\n"
            + "\t\tCYCLE_NUMBER:conventions = \"0...N, 0 : launch cycle (if exists), 1 : first complete cycle\" ;\n"
            + "\t\tCYCLE_NUMBER:long_name = \"Float cycle number\" ;\n"
            + "\tchar DIRECTION(row) ;\n"
            + "\t\tDIRECTION:conventions = \"A: ascending profiles, D: descending profiles\" ;\n"
            + "\t\tDIRECTION:long_name = \"Direction of the station profiles\" ;\n"
            + "\tchar DATA_CENTRE(row, DATA_CENTRE_strlen) ;\n"
            + "\t\tDATA_CENTRE:conventions = \"Argo reference table 4\" ;\n"
            + "\t\tDATA_CENTRE:long_name = \"Data centre in charge of float data processing\" ;\n"
            + "\tchar DC_REFERENCE(row, DC_REFERENCE_strlen) ;\n"
            + "\t\tDC_REFERENCE:conventions = \"Data centre convention\" ;\n"
            + "\t\tDC_REFERENCE:long_name = \"Station unique identifier in data centre\" ;\n"
            + "\tchar DATA_STATE_INDICATOR(row, DATA_STATE_INDICATOR_strlen) ;\n"
            + "\t\tDATA_STATE_INDICATOR:conventions = \"Argo reference table 6\" ;\n"
            + "\t\tDATA_STATE_INDICATOR:long_name = \"Degree of processing the data have passed through\" ;\n"
            + "\tchar DATA_MODE(row) ;\n"
            + "\t\tDATA_MODE:conventions = \"R : real time; D : delayed mode; A : real time with adjustment\" ;\n"
            + "\t\tDATA_MODE:long_name = \"Delayed mode or real time data\" ;\n"
            + "\tchar PLATFORM_TYPE(row, PLATFORM_TYPE_strlen) ;\n"
            + "\t\tPLATFORM_TYPE:conventions = \"Argo reference table 23\" ;\n"
            + "\t\tPLATFORM_TYPE:long_name = \"Type of float\" ;\n"
            + "\tchar FLOAT_SERIAL_NO(row, FLOAT_SERIAL_NO_strlen) ;\n"
            + "\t\tFLOAT_SERIAL_NO:long_name = \"Serial number of the float\" ;\n"
            + "\tchar FIRMWARE_VERSION(row, FIRMWARE_VERSION_strlen) ;\n"
            + "\t\tFIRMWARE_VERSION:long_name = \"Instrument firmware version\" ;\n"
            + "\tchar WMO_INST_TYPE(row, WMO_INST_TYPE_strlen) ;\n"
            + "\t\tWMO_INST_TYPE:conventions = \"Argo reference table 8\" ;\n"
            + "\t\tWMO_INST_TYPE:long_name = \"Coded instrument type\" ;\n"
            + "\tdouble JULD(row) ;\n"
            + "\t\tJULD:_FillValue = 999999.0 ;\n"
            + "\t\tJULD:axis = \"T\" ;\n"
            + "\t\tJULD:conventions = \"Relative julian days with decimal part (as parts of day)\" ;\n"
            + "\t\tJULD:long_name = \"Julian day (UTC) of the station relative to REFERENCE_DATE_TIME\" ;\n"
            + "\t\tJULD:resolution = 0.0 ;\n"
            + "\t\tJULD:standard_name = \"time\" ;\n"
            + "\t\tJULD:units = \"days since 1950-01-01 00:00:00 UTC\" ;\n"
            + "\tchar JULD_QC(row) ;\n"
            + "\t\tJULD_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tJULD_QC:long_name = \"Quality on date and time\" ;\n"
            + "\tdouble JULD_LOCATION(row) ;\n"
            + "\t\tJULD_LOCATION:_FillValue = 999999.0 ;\n"
            + "\t\tJULD_LOCATION:conventions = \"Relative julian days with decimal part (as parts of day)\" ;\n"
            + "\t\tJULD_LOCATION:long_name = \"Julian day (UTC) of the location relative to REFERENCE_DATE_TIME\" ;\n"
            + "\t\tJULD_LOCATION:resolution = 0.0 ;\n"
            + "\t\tJULD_LOCATION:units = \"days since 1950-01-01 00:00:00 UTC\" ;\n"
            + "\tdouble LATITUDE(row) ;\n"
            + "\t\tLATITUDE:_FillValue = 99999.0 ;\n"
            + "\t\tLATITUDE:axis = \"Y\" ;\n"
            + "\t\tLATITUDE:long_name = \"Latitude of the station, best estimate\" ;\n"
            + "\t\tLATITUDE:standard_name = \"latitude\" ;\n"
            + "\t\tLATITUDE:units = \"degree_north\" ;\n"
            + "\t\tLATITUDE:valid_max = 90.0 ;\n"
            + "\t\tLATITUDE:valid_min = -90.0 ;\n"
            + "\tdouble LONGITUDE(row) ;\n"
            + "\t\tLONGITUDE:_FillValue = 99999.0 ;\n"
            + "\t\tLONGITUDE:axis = \"X\" ;\n"
            + "\t\tLONGITUDE:long_name = \"Longitude of the station, best estimate\" ;\n"
            + "\t\tLONGITUDE:standard_name = \"longitude\" ;\n"
            + "\t\tLONGITUDE:units = \"degree_east\" ;\n"
            + "\t\tLONGITUDE:valid_max = 180.0 ;\n"
            + "\t\tLONGITUDE:valid_min = -180.0 ;\n"
            + "\tchar POSITION_QC(row) ;\n"
            + "\t\tPOSITION_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPOSITION_QC:long_name = \"Quality on position (latitude and longitude)\" ;\n"
            + "\tchar POSITIONING_SYSTEM(row, POSITIONING_SYSTEM_strlen) ;\n"
            + "\t\tPOSITIONING_SYSTEM:long_name = \"Positioning system\" ;\n"
            + "\tchar PROFILE_PRES_QC(row) ;\n"
            + "\t\tPROFILE_PRES_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_PRES_QC:long_name = \"Global quality flag of PRES profile\" ;\n"
            + "\tchar PROFILE_TEMP_QC(row) ;\n"
            + "\t\tPROFILE_TEMP_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_TEMP_QC:long_name = \"Global quality flag of TEMP profile\" ;\n"
            + "\tchar PROFILE_PSAL_QC(row) ;\n"
            + "\t\tPROFILE_PSAL_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_PSAL_QC:long_name = \"Global quality flag of PSAL profile\" ;\n"
            + "\tchar VERTICAL_SAMPLING_SCHEME(row, VERTICAL_SAMPLING_SCHEME_strlen) ;\n"
            + "\t\tVERTICAL_SAMPLING_SCHEME:conventions = \"Argo reference table 16\" ;\n"
            + "\t\tVERTICAL_SAMPLING_SCHEME:long_name = \"Vertical sampling scheme\" ;\n"
            + "\tint CONFIG_MISSION_NUMBER(row) ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:_FillValue = 99999 ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:conventions = \"1...N, 1 : first complete mission\" ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:long_name = \"Unique number denoting the missions performed by the float\" ;\n"
            + "\tfloat PRES(row) ;\n"
            + "\t\tPRES:_FillValue = 99999.0f ;\n"
            + "\t\tPRES:axis = \"Z\" ;\n"
            + "\t\tPRES:C_format = \"%7.1f\" ;\n"
            + "\t\tPRES:FORTRAN_format = \"F7.1\" ;\n"
            + "\t\tPRES:long_name = \"Sea water pressure, equals 0 at sea-level\" ;\n"
            + "\t\tPRES:resolution = 1.0f ;\n"
            + "\t\tPRES:standard_name = \"sea_water_pressure\" ;\n"
            + "\t\tPRES:units = \"decibar\" ;\n"
            + "\t\tPRES:valid_max = 12000.0f ;\n"
            + "\t\tPRES:valid_min = 0.0f ;\n"
            + "\tchar PRES_QC(row) ;\n"
            + "\t\tPRES_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPRES_QC:long_name = \"quality flag\" ;\n"
            + "\tfloat PRES_ADJUSTED(row) ;\n"
            + "\t\tPRES_ADJUSTED:_FillValue = 99999.0f ;\n"
            + "\t\tPRES_ADJUSTED:axis = \"Z\" ;\n"
            + "\t\tPRES_ADJUSTED:C_format = \"%7.1f\" ;\n"
            + "\t\tPRES_ADJUSTED:FORTRAN_format = \"F7.1\" ;\n"
            + "\t\tPRES_ADJUSTED:long_name = \"Sea water pressure, equals 0 at sea-level\" ;\n"
            + "\t\tPRES_ADJUSTED:resolution = 1.0f ;\n"
            + "\t\tPRES_ADJUSTED:standard_name = \"sea_water_pressure\" ;\n"
            + "\t\tPRES_ADJUSTED:units = \"decibar\" ;\n"
            + "\t\tPRES_ADJUSTED:valid_max = 12000.0f ;\n"
            + "\t\tPRES_ADJUSTED:valid_min = 0.0f ;\n"
            + "\tchar PRES_ADJUSTED_QC(row) ;\n"
            + "\t\tPRES_ADJUSTED_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPRES_ADJUSTED_QC:long_name = \"quality flag\" ;\n"
            + "\tfloat PRES_ADJUSTED_ERROR(row) ;\n"
            + "\t\tPRES_ADJUSTED_ERROR:_FillValue = 99999.0f ;\n"
            + "\t\tPRES_ADJUSTED_ERROR:C_format = \"%7.1f\" ;\n"
            + "\t\tPRES_ADJUSTED_ERROR:FORTRAN_format = \"F7.1\" ;\n"
            + "\t\tPRES_ADJUSTED_ERROR:long_name = \"Contains the error on the adjusted values as determined by the delayed mode QC process\" ;\n"
            + "\t\tPRES_ADJUSTED_ERROR:resolution = 1.0f ;\n"
            + "\t\tPRES_ADJUSTED_ERROR:units = \"decibar\" ;\n"
            + "\tfloat TEMP(row) ;\n"
            + "\t\tTEMP:_FillValue = 99999.0f ;\n"
            + "\t\tTEMP:C_format = \"%9.3f\" ;\n"
            + "\t\tTEMP:FORTRAN_format = \"F9.3\" ;\n"
            + "\t\tTEMP:long_name = \"Sea temperature in-situ ITS-90 scale\" ;\n"
            + "\t\tTEMP:resolution = 0.001f ;\n"
            + "\t\tTEMP:standard_name = \"sea_water_temperature\" ;\n"
            + "\t\tTEMP:units = \"degree_Celsius\" ;\n"
            + "\t\tTEMP:valid_max = 40.0f ;\n"
            + "\t\tTEMP:valid_min = -2.5f ;\n"
            + "\tchar TEMP_QC(row) ;\n"
            + "\t\tTEMP_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tTEMP_QC:long_name = \"quality flag\" ;\n"
            + "\tfloat TEMP_ADJUSTED(row) ;\n"
            + "\t\tTEMP_ADJUSTED:_FillValue = 99999.0f ;\n"
            + "\t\tTEMP_ADJUSTED:C_format = \"%9.3f\" ;\n"
            + "\t\tTEMP_ADJUSTED:FORTRAN_format = \"F9.3\" ;\n"
            + "\t\tTEMP_ADJUSTED:long_name = \"Sea temperature in-situ ITS-90 scale\" ;\n"
            + "\t\tTEMP_ADJUSTED:resolution = 0.001f ;\n"
            + "\t\tTEMP_ADJUSTED:standard_name = \"sea_water_temperature\" ;\n"
            + "\t\tTEMP_ADJUSTED:units = \"degree_Celsius\" ;\n"
            + "\t\tTEMP_ADJUSTED:valid_max = 40.0f ;\n"
            + "\t\tTEMP_ADJUSTED:valid_min = -2.5f ;\n"
            + "\tchar TEMP_ADJUSTED_QC(row) ;\n"
            + "\t\tTEMP_ADJUSTED_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tTEMP_ADJUSTED_QC:long_name = \"quality flag\" ;\n"
            + "\tfloat TEMP_ADJUSTED_ERROR(row) ;\n"
            + "\t\tTEMP_ADJUSTED_ERROR:_FillValue = 99999.0f ;\n"
            + "\t\tTEMP_ADJUSTED_ERROR:C_format = \"%9.3f\" ;\n"
            + "\t\tTEMP_ADJUSTED_ERROR:FORTRAN_format = \"F9.3\" ;\n"
            + "\t\tTEMP_ADJUSTED_ERROR:long_name = \"Contains the error on the adjusted values as determined by the delayed mode QC process\" ;\n"
            + "\t\tTEMP_ADJUSTED_ERROR:resolution = 0.001f ;\n"
            + "\t\tTEMP_ADJUSTED_ERROR:units = \"degree_Celsius\" ;\n"
            + "\tfloat PSAL(row) ;\n"
            + "\t\tPSAL:_FillValue = 99999.0f ;\n"
            + "\t\tPSAL:C_format = \"%9.3f\" ;\n"
            + "\t\tPSAL:FORTRAN_format = \"F9.3\" ;\n"
            + "\t\tPSAL:long_name = \"Practical salinity\" ;\n"
            + "\t\tPSAL:resolution = 0.001f ;\n"
            + "\t\tPSAL:standard_name = \"sea_water_salinity\" ;\n"
            + "\t\tPSAL:units = \"psu\" ;\n"
            + "\t\tPSAL:valid_max = 41.0f ;\n"
            + "\t\tPSAL:valid_min = 2.0f ;\n"
            + "\tchar PSAL_QC(row) ;\n"
            + "\t\tPSAL_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPSAL_QC:long_name = \"quality flag\" ;\n"
            + "\tfloat PSAL_ADJUSTED(row) ;\n"
            + "\t\tPSAL_ADJUSTED:_FillValue = 99999.0f ;\n"
            + "\t\tPSAL_ADJUSTED:C_format = \"%9.3f\" ;\n"
            + "\t\tPSAL_ADJUSTED:FORTRAN_format = \"F9.3\" ;\n"
            + "\t\tPSAL_ADJUSTED:long_name = \"Practical salinity\" ;\n"
            + "\t\tPSAL_ADJUSTED:resolution = 0.001f ;\n"
            + "\t\tPSAL_ADJUSTED:standard_name = \"sea_water_salinity\" ;\n"
            + "\t\tPSAL_ADJUSTED:units = \"psu\" ;\n"
            + "\t\tPSAL_ADJUSTED:valid_max = 41.0f ;\n"
            + "\t\tPSAL_ADJUSTED:valid_min = 2.0f ;\n"
            + "\tchar PSAL_ADJUSTED_QC(row) ;\n"
            + "\t\tPSAL_ADJUSTED_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPSAL_ADJUSTED_QC:long_name = \"quality flag\" ;\n"
            + "\tfloat PSAL_ADJUSTED_ERROR(row) ;\n"
            + "\t\tPSAL_ADJUSTED_ERROR:_FillValue = 99999.0f ;\n"
            + "\t\tPSAL_ADJUSTED_ERROR:C_format = \"%9.3f\" ;\n"
            + "\t\tPSAL_ADJUSTED_ERROR:FORTRAN_format = \"F9.3\" ;\n"
            + "\t\tPSAL_ADJUSTED_ERROR:long_name = \"Contains the error on the adjusted values as determined by the delayed mode QC process\" ;\n"
            + "\t\tPSAL_ADJUSTED_ERROR:resolution = 0.001f ;\n"
            + "\t\tPSAL_ADJUSTED_ERROR:units = \"psu\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:Conventions = \"Argo-3.1 CF-1.6\" ;\n"
            + "\t\t:featureType = \"trajectoryProfile\" ;\n"
            + "\t\t:history = \"2016-04-15T20:47:22Z creation\" ;\n"
            + "\t\t:institution = \"Coriolis GDAC\" ;\n"
            + "\t\t:references = \"http://www.argodatamgt.org/Documentation\" ;\n"
            + "\t\t:source = \"Argo float\" ;\n"
            + "\t\t:title = \"Argo float vertical profile\" ;\n"
            + "\t\t:user_manual_version = \"3.1\" ;\n"
            + "}\n";
    Test.ensureEqual(
        results.substring(0, expectedStart.length()), expectedStart, "results=\n" + results);

    results = table.dataToString(3);
    expectedStart =
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,5.9,1,5.8,1,2.4,24.989,1,24.989,1,0.002,34.555,1,34.55511,1,0.01\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,10.0,1,9.9,1,2.4,24.99,1,24.99,1,0.002,34.554,1,34.55505,1,0.01\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,20.1,1,20.0,1,2.4,24.69,1,24.69,1,0.002,34.56,1,34.56191,1,0.01\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 18034, "nRows"); // 254*71

    // and the end of that table
    table.removeRows(0, table.nRows() - 3);
    results = table.dataToString(5);
    expectedEnd =
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1899.9,1,1899.3,1,99999.0,2.055,1,2.055,1,99999.0,34.612,1,34.612,1,99999.0\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1950.0,1,1949.4,1,99999.0,2.014,1,2.014,1,99999.0,34.617,1,34.617,1,99999.0\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,99999.0,\" \",99999.0,\" \",99999.0,99999.0,\" \",99999.0,\" \",99999.0,99999.0,\" \",99999.0,\" \",99999.0\n";
    Test.ensureEqual(results, expectedEnd, "results=\n" + results);

    // * test do removeMVRows
    reader.readMultidimNc(
        fiName,
        null,
        StringArray.fromCSV("ZZTOP, N_PROF, N_LEVELS"),
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 17266, "nRows");

    // and test data at the end of that table
    table.removeRows(0, table.nRows() - 3);
    results = table.dataToString(5);
    expectedEnd =
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1850.0,1,1849.4,1,99999.0,2.106,1,2.106,1,99999.0,34.604,1,34.604,1,99999.0\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1899.9,1,1899.3,1,99999.0,2.055,1,2.055,1,99999.0,34.612,1,34.612,1,99999.0\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1950.0,1,1949.4,1,99999.0,2.014,1,2.014,1,99999.0,34.617,1,34.617,1,99999.0\n";
    Test.ensureEqual(results, expectedEnd, "results=\n" + results);

    // * same but quick reject based on constraint LAT,LON 26.587,154.853
    // *** this takes 9ms while test above takes 99ms!
    reader.readMultidimNc(
        fiName,
        null,
        StringArray.fromCSV("ZZTOP, N_PROF, N_LEVELS"),
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        StringArray.fromCSV("LATITUDE"), // conVars
        StringArray.fromCSV("="), // conOps
        StringArray.fromCSV("45")); // conVals
    Test.ensureEqual(table.nRows(), 0, "nRows");

    // * test different dim order (should be rearranged so the same)
    reader.readMultidimNc(
        fiName,
        null,
        StringArray.fromCSV("N_LEVELS, ZZTOP, N_PROF"),
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 17266, "nRows");

    // and test data at the end of that table
    table.removeRows(0, table.nRows() - 3);
    results = table.dataToString(5);
    Test.ensureEqual(results, expectedEnd, "results=\n" + results);

    // * test read all and constrain PLATFORM_NUMBER
    // Roland reported this problem 2016-06-21: returned 0 rows, saying:
    // "Returning an empty table because var=PLATFORM_NUMBER failed its constraints,
    // including =2901175. time=0"

    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV(
            "DATA_TYPE, FORMAT_VERSION, HANDBOOK_VERSION, REFERENCE_DATE_TIME, DATE_CREATION, "
                + "DATE_UPDATE, PLATFORM_NUMBER, PROJECT_NAME, PI_NAME, CYCLE_NUMBER, DIRECTION, "
                + "DATA_CENTRE, DC_REFERENCE, DATA_STATE_INDICATOR, DATA_MODE, PLATFORM_TYPE, "
                + "FLOAT_SERIAL_NO, FIRMWARE_VERSION, WMO_INST_TYPE, JULD, JULD_QC, JULD_LOCATION, "
                + "LATITUDE, LONGITUDE, POSITION_QC, POSITIONING_SYSTEM, PROFILE_PRES_QC, "
                + "PROFILE_TEMP_QC, PROFILE_PSAL_QC, VERTICAL_SAMPLING_SCHEME, "
                + "CONFIG_MISSION_NUMBER, PRES, PRES_QC, PRES_ADJUSTED, PRES_ADJUSTED_QC, "
                + "PRES_ADJUSTED_ERROR, TEMP, TEMP_QC, TEMP_ADJUSTED, TEMP_ADJUSTED_QC, "
                + "TEMP_ADJUSTED_ERROR, PSAL, PSAL_QC, PSAL_ADJUSTED, PSAL_ADJUSTED_QC, "
                + "PSAL_ADJUSTED_ERROR"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        StringArray.fromCSV("PLATFORM_NUMBER"), // conVars, conOps, conVals
        StringArray.fromCSV("="),
        StringArray.fromCSV("2901175"));
    results = table.dataToString(3);
    expectedStart =
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,"
            + "DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,"
            + "DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,"
            + "FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,"
            + "POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,"
            + "VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,"
            + "PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,"
            + "TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,"
            + "CHINA ARGO PROJECT,JIANPING XU,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,"
            + "21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,"
            + "ARGOS,A,A,A,,1,5.9,1,5.8,1,2.4,24.989,1,24.989,1,0.002,34.555,1,34.55511,"
            + "1,0.01\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,"
            + "CHINA ARGO PROJECT,JIANPING XU,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,"
            + "21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,"
            + "ARGOS,A,A,A,,1,10.0,1,9.9,1,2.4,24.99,1,24.99,1,0.002,34.554,1,34.55505,"
            + "1,0.01\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,"
            + "CHINA ARGO PROJECT,JIANPING XU,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,"
            + "21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,"
            + "ARGOS,A,A,A,,1,20.1,1,20.0,1,2.4,24.69,1,24.69,1,0.002,34.56,1,34.56191,"
            + "1,0.01\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(
        table.nRows(), 17266, "nRows"); // same as when all variables were explicitly loaded

    // * test different varNames
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV(
            "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 17266, "nRows");

    // and test data at the end of that table
    table.removeRows(0, table.nRows() - 3);
    results = table.dataToString(5);
    Test.ensureEqual(results, expectedEnd, "results=\n" + results);

    // * test do removeMVRows when loadVariables is limited (to ensure all are
    // loaded for the test)
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV("LONGITUDE,PRES,PSAL_ADJUSTED_ERROR"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart =
        "LONGITUDE,PRES,PSAL_ADJUSTED_ERROR\n"
            + "123.36499786376953,5.9,0.01\n"
            + "123.36499786376953,10.0,0.01\n"
            + "123.36499786376953,20.1,0.01\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(
        table.nRows(), 17266, "nRows"); // same as when all variables were explicitly loaded

    // and test data at the end of that table
    table.removeRows(0, table.nRows() - 3);
    results = table.dataToString(5);
    expectedEnd =
        "LONGITUDE,PRES,PSAL_ADJUSTED_ERROR\n"
            + "154.853,1850.0,99999.0\n"
            + // these rows were're removed because other full-dim vars had values
            "154.853,1899.9,99999.0\n"
            + "154.853,1950.0,99999.0\n";
    Test.ensureEqual(results, expectedEnd, "results=\n" + results);

    // * test read JULD
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV("JULD"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart =
        "JULD\n"
            + "21660.34238425926\n"
            + "21670.351828703704\n"
            + "21680.386898148146\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(
        table.nRows(), 254, "nRows"); // same as when all variables were explicitly loaded

    table.removeRows(0, 251);
    results = table.dataToString(1000);
    expectedStart =
        "JULD\n" + "24190.451828703703\n" + "24200.381412037037\n" + "24210.44662037037\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);

    // * test read JULD && PRES
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV("JULD,PRES"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart =
        "JULD,PRES\n"
            + "21660.34238425926,5.9\n"
            + // JULD is correctly JOINed
            "21660.34238425926,10.0\n"
            + "21660.34238425926,20.1\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(
        table.nRows(), 17266, "nRows"); // same as when all variables were explicitly loaded

    table.removeRows(0, 17263);
    results = table.dataToString(1000);
    expectedStart =
        "JULD,PRES\n"
            + "24210.44662037037,1850.0\n"
            + // JULD is correctly JOINed
            "24210.44662037037,1899.9\n"
            + "24210.44662037037,1950.0\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);

    // * test read just static vars, in a different order
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV("HANDBOOK_VERSION,FORMAT_VERSION,DATA_TYPE"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart = "HANDBOOK_VERSION,FORMAT_VERSION,DATA_TYPE\n" + "1.2,3.1,Argo profile\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);

    // * test read 0 dim variable -> empty table
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV("HISTORY_INSTITUTION"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // * test read non-existent dim -> just scalar vars
    reader.readMultidimNc(
        fiName,
        null,
        StringArray.fromCSV("ZZTOP"),
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart =
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);

    // * test read non-existent Var -> empty table
    reader.readMultidimNc(
        fiName,
        StringArray.fromCSV("ZZTOP"),
        null,
        null,
        true,
        0,
        true, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // done
    /* */
    // Table.debugMode = oDebugMode;
  }

  /**
   * Test readASCII with csv file.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testReadAsciiCsvFile() throws Exception {

    // String2.log("\nTable.testReadAsciiCsvASCIIFile");
    String results, expected;
    StringArray sa = new StringArray();
    String fileName = TableTests.class.getResource("/data/csvAscii.txt").getPath();
    String skipHeaderToRegex = "\\*\\*\\* END OF HEADER.*";
    String skipLinesRegex = "#.*";

    Table table;

    // public void readASCII(String fullFileName, int columnNamesLine, int
    // dataStartLine,
    // String testColumns[], double testMin[], double testMax[],
    // String loadColumns[], boolean simplify) throws Exception {

    // read as Strings
    table = new Table();
    table.allowRaggedRightInReadASCII = true;
    table.readASCII(
        fileName,
        File2.ISO_8859_1,
        skipHeaderToRegex,
        skipLinesRegex,
        0,
        1,
        "",
        null,
        null,
        null,
        null,
        false);
    results = table.dataToString();
    expected =
        "aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n"
            + "\" b,d \",Ab,t,24,24000,24000000,240000000000,2.4,2.412345678987654\n"
            + "needs,1comma:,,,,,,,\n"
            + "fg,F,true,11,12001,1200000,12000000000,1.21,1e200\n"
            + "h,H,1,12,12002,120000,1200000000,1.22,2e200\n"
            + "i,I,TRUE,13,12003,12000,120000000,1.23,3e200\n"
            + "j,J,f,14,12004,1200,12000000,1.24,4e200\n"
            + "k,K,false,15,12005,120,1200000,1.25,5e200\n"
            + "\"BAD LINE: UNCLOSED QUOTE,K,false,15,12005,120,1200000,1.25,   5.5e200\",,,,,,,,\n"
            + "l,L,0,16,12006,12,120000,1.26,6e200\n"
            + "m,M,FALSE,17,12007,121,12000,1.27,7e200\n"
            + "n,N,8,18,12008,122,1200,1.28,8e200\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "String, String, String, String, String, String, String, String, String";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test simplify
    table = new Table();
    table.allowRaggedRightInReadASCII = false;
    table.readASCII(
        fileName,
        File2.ISO_8859_1,
        skipHeaderToRegex,
        skipLinesRegex,
        0,
        1,
        "",
        null,
        null,
        null,
        null,
        true);
    results = table.dataToString();
    expected = // bad lines are removed
        "aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n"
            + "\" b,d \",Ab,t,24,24000,24000000,240000000000,2.4,2.412345678987654\n"
            + "fg,F,true,11,12001,1200000,12000000000,1.21,1.0E200\n"
            + "h,H,1,12,12002,120000,1200000000,1.22,2.0E200\n"
            + "i,I,TRUE,13,12003,12000,120000000,1.23,3.0E200\n"
            + "j,J,f,14,12004,1200,12000000,1.24,4.0E200\n"
            + "k,K,false,15,12005,120,1200000,1.25,5.0E200\n"
            + "l,L,0,16,12006,12,120000,1.26,6.0E200\n"
            + "m,M,FALSE,17,12007,121,12000,1.27,7.0E200\n"
            + "n,N,8,18,12008,122,1200,1.28,8.0E200\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "String, String, String, byte, short, int, long, float, double";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read subset
    table = new Table();
    table.allowRaggedRightInReadASCII = false;
    table.readASCII(
        fileName,
        File2.ISO_8859_1,
        skipHeaderToRegex,
        skipLinesRegex,
        0,
        1,
        "",
        new String[] {"aByte"},
        new double[] {14},
        new double[] {16},
        new String[] {"aDouble", "aString", "aByte"},
        true); // load cols
    results = table.dataToString();
    expected = // bad lines are removed
        "aDouble,aString,aByte\n" + "4.0E200,j,14\n" + "5.0E200,k,15\n" + "6.0E200,l,16\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * Test readASCII with ssv file.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testReadAsciiSsvFile() throws Exception {

    // String2.log("\nTable.testReadAsciiSsvASCIIFile");
    String results, expected;
    StringArray sa = new StringArray();
    String fileName = TableTests.class.getResource("/data/ssvAscii.txt").getPath();
    Table table;

    // public void readASCII(String fullFileName, int columnNamesLine, int
    // dataStartLine,
    // String testColumns[], double testMin[], double testMax[],
    // String loadColumns[], boolean simplify) throws Exception {

    // read as Strings
    table = new Table();
    table.allowRaggedRightInReadASCII = true;
    table.readASCII(fileName, File2.ISO_8859_1, "", "", 0, 1, "", null, null, null, null, false);
    results = table.dataToString();
    expected =
        "aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n"
            + "\" b d \",Ab,t,24,24000,24000000,240000000000,2.4,2.412345678987654\n"
            + "needs1space,E,,,,,,,\n"
            + "fg,F,true,11,12001,1200000,12000000000,1.21,1e200\n"
            + "h,H,1,12,12002,120000,1200000000,1.22,2e200\n"
            + "i,I,TRUE,13,12003,12000,120000000,1.23,3e200\n"
            + "j,J,f,14,12004,1200,12000000,1.24,4e200\n"
            + "k,K,false,15,12005,120,1200000,1.25,5e200\n"
            + "l,L,0,16,12006,12,120000,1.26,6e200\n"
            + "m,M,FALSE,17,12007,121,12000,1.27,7e200\n"
            + "n,N,8,18,12008,122,1200,1.28,8e200\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "String, String, String, String, String, String, String, String, String";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test simplify
    table = new Table();
    table.allowRaggedRightInReadASCII = true;
    table.readASCII(fileName, File2.ISO_8859_1, "", "", 0, 1, "", null, null, null, null, true);
    results = table.dataToString();
    expected =
        "aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n"
            + "\" b d \",Ab,t,24,24000,24000000,240000000000,2.4,2.412345678987654\n"
            + "needs1space,E,,,,,,,\n"
            + "fg,F,true,11,12001,1200000,12000000000,1.21,1.0E200\n"
            + "h,H,1,12,12002,120000,1200000000,1.22,2.0E200\n"
            + "i,I,TRUE,13,12003,12000,120000000,1.23,3.0E200\n"
            + "j,J,f,14,12004,1200,12000000,1.24,4.0E200\n"
            + "k,K,false,15,12005,120,1200000,1.25,5.0E200\n"
            + "l,L,0,16,12006,12,120000,1.26,6.0E200\n"
            + "m,M,FALSE,17,12007,121,12000,1.27,7.0E200\n"
            + "n,N,8,18,12008,122,1200,1.28,8.0E200\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "String, String, String, byte, short, int, long, float, double";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read subset
    table = new Table();
    table.allowRaggedRightInReadASCII = true;
    table.readASCII(
        fileName,
        File2.ISO_8859_1,
        "",
        "",
        0,
        1,
        "",
        new String[] {"aByte"},
        new double[] {14},
        new double[] {16},
        new String[] {"aDouble", "aString", "aByte"},
        true); // load cols
    results = table.dataToString();
    expected =
        "aDouble,aString,aByte\n"
            + ",needs1space,\n"
            + "4.0E200,j,14\n"
            + "5.0E200,k,15\n"
            + "6.0E200,l,16\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * Test readColumnarASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testReadColumnarASCIIFile() throws Exception {

    // String2.log("\nTable.testReadColumnarASCIIFile");
    String results, expected;
    StringArray sa = new StringArray();
    String fullFileName =
        TableTests.class.getResource("/data/columnarAsciiWithComments.txt").getPath();
    String skipHeaderToRegex = "END OF HEADER.*";
    String skipLinesRegex = "%.*";
    String colNames[] = {
      "aDouble", "aString", "aChar", "aBoolean", "aByte", "aShort", "anInt", "aLong", "aFloat"
    };
    int start[] = {66, 0, 9, 15, 24, 30, 37, 45, 57};
    int end[] = {86, 9, 15, 24, 30, 37, 45, 57, 66};

    // 012345678911234567892123456789312345678941234567895123456789612345678971234567898123456
    // aString aChar aBoolean aByte aShort anInt aLong aFloat aDouble
    // abc a true 24 24000 240000002400000000002.4 2.412345678987654

    // read as Strings
    PAType colPAType[] = new PAType[9];
    Arrays.fill(colPAType, PAType.STRING);
    Table table = new Table();
    table.readColumnarASCIIFile(
        fullFileName, "", skipHeaderToRegex, skipLinesRegex, 3, colNames, start, end, colPAType);
    results = table.dataToString();
    expected =
        "aDouble,aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat\n"
            + "2.412345678987654,abcdef,Ab,t,24,24000,24000000,240000000000,2.4\n"
            + ",short:,,,,,,,\n"
            + "1e200,fg,F,true,11,12001,1200000,12000000000,1.21\n"
            + "2e200,h,H,1,12,12002,120000,1200000000,1.22\n"
            + "3e200,i,I,TRUE,13,12003,12000,120000000,1.23\n"
            + "4e200,j,J,f,14,12004,1200,12000000,1.24\n"
            + "5e200,k,K,false,15,12005,120,1200000,1.25\n"
            + "6e200,l,L,0,16,12006,12,120000,1.26\n"
            + "7e200,m,M,FALSE,17,12007,121,12000,1.27\n"
            + "8e200,n,N,8,18,12008,122,1200,1.28\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "String, String, String, String, String, String, String, String, String";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // simplify
    table.readColumnarASCIIFile(
        fullFileName, "", skipHeaderToRegex, skipLinesRegex, 3, colNames, start, end, null);
    results = table.dataToString();
    expected =
        "aDouble,aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat\n"
            + "2.412345678987654,abcdef,Ab,t,24,24000,24000000,240000000000,2.4\n"
            + ",short:,,,,,,,\n"
            + "1.0E200,fg,F,true,11,12001,1200000,12000000000,1.21\n"
            + "2.0E200,h,H,1,12,12002,120000,1200000000,1.22\n"
            + "3.0E200,i,I,TRUE,13,12003,12000,120000000,1.23\n"
            + "4.0E200,j,J,f,14,12004,1200,12000000,1.24\n"
            + "5.0E200,k,K,false,15,12005,120,1200000,1.25\n"
            + "6.0E200,l,L,0,16,12006,12,120000,1.26\n"
            + "7.0E200,m,M,FALSE,17,12007,121,12000,1.27\n"
            + "8.0E200,n,N,8,18,12008,122,1200,1.28\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "double, String, String, String, byte, short, int, long, float";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read as types
    colPAType =
        new PAType[] {
          PAType.DOUBLE,
          PAType.STRING,
          PAType.CHAR,
          PAType.BOOLEAN,
          PAType.BYTE,
          PAType.SHORT,
          PAType.INT,
          PAType.LONG,
          PAType.FLOAT
        };
    table.readColumnarASCIIFile(
        fullFileName, "", skipHeaderToRegex, skipLinesRegex, 3, colNames, start, end, colPAType);
    results = table.dataToString();
    expected =
        "aDouble,aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat\n"
            + "2.412345678987654,abcdef,A,1,24,24000,24000000,240000000000,2.4\n"
            + ",short:,,,,,,,\n"
            + "1.0E200,fg,F,1,11,12001,1200000,12000000000,1.21\n"
            + "2.0E200,h,H,1,12,12002,120000,1200000000,1.22\n"
            + "3.0E200,i,I,1,13,12003,12000,120000000,1.23\n"
            + "4.0E200,j,J,0,14,12004,1200,12000000,1.24\n"
            + "5.0E200,k,K,0,15,12005,120,1200000,1.25\n"
            + "6.0E200,l,L,0,16,12006,12,120000,1.26\n"
            + "7.0E200,m,M,0,17,12007,121,12000,1.27\n"
            + "8.0E200,n,N,1,18,12008,122,1200,1.28\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // test types
    sa.clear();
    for (int col = 0; col < table.nColumns(); col++)
      sa.add(table.getColumn(col).elementTypeString());
    results = sa.toString();
    expected = "double, String, char, byte, byte, short, int, long, float";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests readNccsv(), readNccsvMetadata(), */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testNccsv() throws Exception {
    // String2.log("\n**** Table.testNccsv()\n");
    String dir = TableTests.class.getResource("/data/nccsv/").getPath();

    // read/write scalar
    String fileName = dir + "testScalar_1.1.csv";
    Table table = new Table();
    table.readNccsv(fileName);
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    String results = table.saveAsNccsv();
    String expected1 =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,featureType,trajectory\n"
            + "*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"NOAA, sea, ship, sst, surface, temperature, trajectory\"\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,ship\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "ship,*SCALAR*,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "ship,cf_role,trajectory_id\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,standard_name,time\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "lat,*DATA_TYPE*,double\n"
            + "lat,units,degrees_north\n"
            + "lon,*DATA_TYPE*,double\n"
            + "lon,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,0.17f,23.58f\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "time,lat,lon,status,testByte,testUByte,testLong,testULong,sst\n"
            + "2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n"
            + "2017-03-23T01:45:00Z,28.0003,-130.3472,\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n"
            + "2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n"
            + "2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n"
            + "2017-03-23T21:45:00Z,28.0003,-132.0014,\u00fc,,,,,\n"
            + "2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n"
            + "*END_DATA*\n";
    Test.ensureEqual(results, expected1, "results=\n" + results);

    results = table.saveAsNccsv(false, true, 0, Integer.MAX_VALUE); // don't catch scalar
    String expected2 =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,featureType,trajectory\n"
            + "*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"NOAA, sea, ship, sst, surface, temperature, trajectory\"\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,ship\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,standard_name,time\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "lat,*DATA_TYPE*,double\n"
            + "lat,units,degrees_north\n"
            + "lon,*DATA_TYPE*,double\n"
            + "lon,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,0.17f,23.58f\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "ship,time,lat,lon,status,testByte,testUByte,testLong,testULong,sst\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\u00fc,,,,,\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n"
            + "*END_DATA*\n";
    Test.ensureEqual(results, expected2, "results=\n" + results);

    // non scalar
    fileName = dir + "sample_1.1.csv";
    table.readNccsv(fileName);
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    // String2.log(table.toString());
    Attributes atts = table.columnAttributes("sst");
    PrimitiveArray pa = atts.get("testChars");
    // String2.log(">> sample_1.1.csv testChars isCharArray?" + (pa instanceof
    // CharArray) + " isStringArray?"
    // + (pa instanceof StringArray));
    results = table.saveAsNccsv(false, true, 0, Integer.MAX_VALUE); // don't catch scalars
    expected1 =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,featureType,trajectory\n"
            + "*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"NOAA, sea, ship, sst, surface, temperature, trajectory\"\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,ship\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,standard_name,time\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "lat,*DATA_TYPE*,double\n"
            + "lat,units,degrees_north\n"
            + "lon,*DATA_TYPE*,double\n"
            + "lon,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,0.17f,23.58f\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,0L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "ship,time,lat,lon,status,testByte,testUByte,testLong,testULong,sst\n"
            + "Bell M. Shimada,2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n"
            + "Bell M. Shimada,2017-03-23T01:45:00Z,28.0003,-130.3472,\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n"
            + "Bell M. Shimada,2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n"
            + "Bell M. Shimada,2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n"
            + "Bell M. Shimada,2017-03-23T21:45:00Z,28.0003,-132.0014,\u00fc,,,,,\n"
            + "Bell M. Shimada,2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n"
            + "*END_DATA*\n";
    Test.ensureEqual(results, expected1, "results=\n" + results);

    // non scalar
    fileName = dir + "sample_1.2.csv";
    table.readNccsv(fileName);
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    // String2.log(table.toString());
    atts = table.columnAttributes("sst");
    pa = atts.get("testChars");
    String2.log(
        ">> sample_1.2.csv testChars isCharArray?"
            + (pa instanceof CharArray)
            + " isStringArray?"
            + (pa instanceof StringArray));
    results = table.saveAsNccsv(false, true, 0, Integer.MAX_VALUE); // don't catch scalars
    // we expect the same results
    Test.ensureEqual(results, expected1, "results=\n" + results);

    // just metadata
    fileName = dir + "sampleMetadata_1.1.csv";
    table = new Table();
    table.readNccsv(fileName, false); // readData?
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    results = table.saveAsNccsv(true, true, 0, 0); // catch scalar, writeMetadata, don't write data
    String expected3 =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,featureType,trajectory\n"
            + "*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"NOAA, sea, ship, sst, surface, temperature, trajectory\"\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,ship\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "ship,*SCALAR*,Bell M. Shimada\n"
            + "ship,cf_role,trajectory_id\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,standard_name,time\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "lat,*DATA_TYPE*,double\n"
            + "lat,units,degrees_north\n"
            + "lon,*DATA_TYPE*,double\n"
            + "lon,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,0.17f,23.58f\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,0L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n";
    Test.ensureEqual(results, expected3, "results=\n" + results);

    // just metadata
    fileName = dir + "sampleMetadata_1.2.csv";
    table = new Table();
    table.readNccsv(fileName, false); // readData?
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    results = table.saveAsNccsv(true, true, 0, 0); // catch scalar, writeMetadata, don't write data
    Test.ensureEqual(results, expected3, "results=\n" + results);
  }

  /**
   * This tests saveAsMatlab().
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsMatlab() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // see gov.noaa.pfel.coastwatch.griddata.Matlab class for summary of Matlab
    // commands

    // String2.log("\n***** Table.testSaveAsMatlab");
    Table table = new Table();
    table.addColumn("ints", new IntArray(new int[] {1, 2, 3}));
    table.addColumn("floats", new FloatArray(new float[] {1.1f, 2.2f, 3.3f}));
    table.addColumn("Strings", new StringArray(new String[] {"a", "bb", "ccc"}));
    table.addColumn("doubles", new DoubleArray(new double[] {1.111, 2.222, 3.333}));
    String dir = TEMP_DIR.toAbsolutePath().toString() + "/";
    File2.delete(dir + "temp.mat");
    table.saveAsMatlab(dir + "temp.mat", "sst"); // names of length 3,4,5 were a challenge
    String mhd = File2.hexDump(dir + "temp.mat", 1000);
    // String2.log(mhd);
    // String2.log("\nsst.mat=\n" + File2.hexDump(dir + "sst.mat", 1000));
    Test.ensureEqual(
        mhd.substring(0, 71 * 4) + mhd.substring(71 * 7), // remove the creation dateTime
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            +
            // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 4d 6f , Created on: Mo |\n" +
            // "6e 20 46 65 62 20 31 31 20 30 39 3a 31 31 3a 30 n Feb 11 09:11:0 |\n" +
            // "30 20 32 30 30 38 20 20 20 20 20 20 20 20 20 20 0 2008 |\n" +
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 01 e8   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 03 00 01 73 73 74 00               sst  |\n"
            + "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 80                    |\n"
            + "69 6e 74 73 00 00 00 00   00 00 00 00 00 00 00 00   ints             |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "66 6c 6f 61 74 73 00 00   00 00 00 00 00 00 00 00   floats           |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "53 74 72 69 6e 67 73 00   00 00 00 00 00 00 00 00   Strings          |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "64 6f 75 62 6c 65 73 00   00 00 00 00 00 00 00 00   doubles          |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 0e 00 00 00 40   00 00 00 06 00 00 00 08          @         |\n"
            + "00 00 00 0c 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 0c   00 00 00 01 00 00 00 02                    |\n"
            + "00 00 00 03 00 00 00 00   00 00 00 0e 00 00 00 40                  @ |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 08   00 00 00 03 00 00 00 01                    |\n"
            + "00 00 00 01 00 00 00 00   00 00 00 07 00 00 00 0c                    |\n"
            + "3f 8c cc cd 40 0c cc cd   40 53 33 33 00 00 00 00   ?   @   @S33     |\n"
            + "00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n"
            + "00 00 00 04 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 03 00 00 00 03   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 04 00 00 00 12   00 61 00 62 00 63 00 20            a b c   |\n"
            + "00 62 00 63 00 20 00 20   00 63 00 00 00 00 00 00    b c     c       |\n"
            + "00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 09 00 00 00 18   3f f1 c6 a7 ef 9d b2 2d           ?      - |\n"
            + "40 01 c6 a7 ef 9d b2 2d   40 0a a9 fb e7 6c 8b 44   @      -@    l D |\n",
        "mhd=" + mhd);
    // File2.delete(dir + "temp.mat");
  }

  /**
   * Test readAwsXmlFile. Automatic Weather Station
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testReadAwsXmlFile() throws Exception {
    // String2.log("\nTable.testReadAwsXmlFile");
    Table table = new Table();
    table.readAwsXmlFile(
        TableTests.class.getResource("/data/aws/xml/SNFLS-2012-11-03T20_30_01Z.xml").getPath());
    String results = table.toString();
    String expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 1 ;\n"
            + "\tob-date_strlen = 11 ;\n"
            + "\tstation-id_strlen = 5 ;\n"
            + "\tstation_strlen = 13 ;\n"
            + "\tcity-state-zip_strlen = 5 ;\n"
            + "\tcity-state_strlen = 17 ;\n"
            + "\tsite-url_strlen = 0 ;\n"
            + "\taux-temp_strlen = 2 ;\n"
            + "\taux-temp-rate_strlen = 1 ;\n"
            + "\tdew-point_strlen = 2 ;\n"
            + "\televation_strlen = 1 ;\n"
            + "\tfeels-like_strlen = 2 ;\n"
            + "\tgust-time_strlen = 11 ;\n"
            + "\tgust-direction_strlen = 1 ;\n"
            + "\tgust-speed_strlen = 1 ;\n"
            + "\thumidity_strlen = 2 ;\n"
            + "\thumidity-high_strlen = 3 ;\n"
            + "\thumidity-low_strlen = 2 ;\n"
            + "\thumidity-rate_strlen = 2 ;\n"
            + "\tindoor-temp_strlen = 2 ;\n"
            + "\tindoor-temp-rate_strlen = 4 ;\n"
            + "\tlight_strlen = 4 ;\n"
            + "\tlight-rate_strlen = 4 ;\n"
            + "\tmoon-phase-moon-phase-img_strlen = 12 ;\n"
            + "\tmoon-phase_strlen = 2 ;\n"
            + "\tpressure_strlen = 4 ;\n"
            + "\tpressure-high_strlen = 5 ;\n"
            + "\tpressure-low_strlen = 5 ;\n"
            + "\tpressure-rate_strlen = 5 ;\n"
            + "\train-month_strlen = 4 ;\n"
            + "\train-rate_strlen = 1 ;\n"
            + "\train-rate-max_strlen = 1 ;\n"
            + "\train-today_strlen = 1 ;\n"
            + "\train-year_strlen = 4 ;\n"
            + "\ttemp_strlen = 4 ;\n"
            + "\ttemp-high_strlen = 2 ;\n"
            + "\ttemp-low_strlen = 2 ;\n"
            + "\ttemp-rate_strlen = 3 ;\n"
            + "\tsunrise_strlen = 13 ;\n"
            + "\tsunset_strlen = 13 ;\n"
            + "\twet-bulb_strlen = 6 ;\n"
            + "\twind-speed_strlen = 1 ;\n"
            + "\twind-speed-avg_strlen = 1 ;\n"
            + "\twind-direction_strlen = 3 ;\n"
            + "\twind-direction-avg_strlen = 1 ;\n"
            + "variables:\n"
            + "\tchar ob-date(row, ob-date_strlen) ;\n"
            + "\t\tob-date:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tchar station-id(row, station-id_strlen) ;\n"
            + "\tchar station(row, station_strlen) ;\n"
            + "\tchar city-state-zip(row, city-state-zip_strlen) ;\n"
            + "\tchar city-state(row, city-state_strlen) ;\n"
            + "\tchar site-url(row, site-url_strlen) ;\n"
            + "\tchar aux-temp(row, aux-temp_strlen) ;\n"
            + "\t\taux-temp:units = \"degree_F\" ;\n"
            + "\tchar aux-temp-rate(row, aux-temp-rate_strlen) ;\n"
            + "\t\taux-temp-rate:units = \"degree_F\" ;\n"
            + "\tchar dew-point(row, dew-point_strlen) ;\n"
            + "\t\tdew-point:units = \"degree_F\" ;\n"
            + "\tchar elevation(row, elevation_strlen) ;\n"
            + "\t\televation:units = \"ft\" ;\n"
            + "\tchar feels-like(row, feels-like_strlen) ;\n"
            + "\t\tfeels-like:units = \"degree_F\" ;\n"
            + "\tchar gust-time(row, gust-time_strlen) ;\n"
            + "\t\tgust-time:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tchar gust-direction(row, gust-direction_strlen) ;\n"
            + "\tchar gust-speed(row, gust-speed_strlen) ;\n"
            + "\t\tgust-speed:units = \"mph\" ;\n"
            + "\tchar humidity(row, humidity_strlen) ;\n"
            + "\t\thumidity:units = \"%\" ;\n"
            + "\tchar humidity-high(row, humidity-high_strlen) ;\n"
            + "\t\thumidity-high:units = \"%\" ;\n"
            + "\tchar humidity-low(row, humidity-low_strlen) ;\n"
            + "\t\thumidity-low:units = \"%\" ;\n"
            + "\tchar humidity-rate(row, humidity-rate_strlen) ;\n"
            + "\tchar indoor-temp(row, indoor-temp_strlen) ;\n"
            + "\t\tindoor-temp:units = \"degree_F\" ;\n"
            + "\tchar indoor-temp-rate(row, indoor-temp-rate_strlen) ;\n"
            + "\t\tindoor-temp-rate:units = \"degree_F\" ;\n"
            + "\tchar light(row, light_strlen) ;\n"
            + "\tchar light-rate(row, light-rate_strlen) ;\n"
            + "\tchar moon-phase-moon-phase-img(row, moon-phase-moon-phase-img_strlen) ;\n"
            + "\tchar moon-phase(row, moon-phase_strlen) ;\n"
            + "\tchar pressure(row, pressure_strlen) ;\n"
            + "\t\tpressure:units = \"inch_Hg\" ;\n"
            + "\tchar pressure-high(row, pressure-high_strlen) ;\n"
            + "\t\tpressure-high:units = \"inch_Hg\" ;\n"
            + "\tchar pressure-low(row, pressure-low_strlen) ;\n"
            + "\t\tpressure-low:units = \"inch_Hg\" ;\n"
            + "\tchar pressure-rate(row, pressure-rate_strlen) ;\n"
            + "\t\tpressure-rate:units = \"inch_Hg/h\" ;\n"
            + "\tchar rain-month(row, rain-month_strlen) ;\n"
            + "\t\train-month:units = \"inches\" ;\n"
            + "\tchar rain-rate(row, rain-rate_strlen) ;\n"
            + "\t\train-rate:units = \"inches/h\" ;\n"
            + "\tchar rain-rate-max(row, rain-rate-max_strlen) ;\n"
            + "\t\train-rate-max:units = \"inches/h\" ;\n"
            + "\tchar rain-today(row, rain-today_strlen) ;\n"
            + "\t\train-today:units = \"inches\" ;\n"
            + "\tchar rain-year(row, rain-year_strlen) ;\n"
            + "\t\train-year:units = \"inches\" ;\n"
            + "\tchar temp(row, temp_strlen) ;\n"
            + "\t\ttemp:units = \"degree_F\" ;\n"
            + "\tchar temp-high(row, temp-high_strlen) ;\n"
            + "\t\ttemp-high:units = \"degree_F\" ;\n"
            + "\tchar temp-low(row, temp-low_strlen) ;\n"
            + "\t\ttemp-low:units = \"degree_F\" ;\n"
            + "\tchar temp-rate(row, temp-rate_strlen) ;\n"
            + "\t\ttemp-rate:units = \"degree_F\" ;\n"
            + "\tchar sunrise(row, sunrise_strlen) ;\n"
            + "\t\tsunrise:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tchar sunset(row, sunset_strlen) ;\n"
            + "\t\tsunset:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tchar wet-bulb(row, wet-bulb_strlen) ;\n"
            + "\t\twet-bulb:units = \"degree_F\" ;\n"
            + "\tchar wind-speed(row, wind-speed_strlen) ;\n"
            + "\t\twind-speed:units = \"mph\" ;\n"
            + "\tchar wind-speed-avg(row, wind-speed-avg_strlen) ;\n"
            + "\t\twind-speed-avg:units = \"mph\" ;\n"
            + "\tchar wind-direction(row, wind-direction_strlen) ;\n"
            + "\tchar wind-direction-avg(row, wind-direction-avg_strlen) ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "ob-date,station-id,station,city-state-zip,city-state,site-url,aux-temp,aux-temp-rate,dew-point,elevation,feels-like,gust-time,gust-direction,gust-speed,humidity,humidity-high,humidity-low,humidity-rate,indoor-temp,indoor-temp-rate,light,light-rate,moon-phase-moon-phase-img,moon-phase,pressure,pressure-high,pressure-low,pressure-rate,rain-month,rain-rate,rain-rate-max,rain-today,rain-year,temp,temp-high,temp-low,temp-rate,sunrise,sunset,wet-bulb,wind-speed,wind-speed-avg,wind-direction,wind-direction-avg\n"
            + "1.3519746E9,SNFLS,Exploratorium,94123,\"San Francisco, CA\",,32,0,54,0,67,1.3519746E9,E,8,63,100,63,-6,90,+4.6,67.9,-0.3,mphase16.gif,82,30.1,30.14,30.06,-0.01,0.21,0,0,0,1.76,66.9,67,52,3.8,1.351953497E9,1.351991286E9,59.162,0,2,ENE,E\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.3519746E9), "2012-11-03T20:30:00Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.3519746E9), "2012-11-03T20:30:00Z", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(1.351953497E9), "2012-11-03T14:38:17Z", "");
    Test.ensureEqual(
        Calendar2.epochSecondsToIsoStringTZ(1.351991286E9), "2012-11-04T01:08:06Z", "");
  }

  /** This tests readNDNC. */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testReadNDNc() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // String2.log("\n*** Table.testReadNDNc");
    Table table = new Table();
    String results, expected;

    // test no vars specified, 4D, only 2nd dim has >1 value, getMetadata
    String fiName =
        TableTests.class
            .getResource("/data/points/erdCalcofiSubsurface/1950/subsurface_19500106_69_144.nc")
            .getPath();
    table.readNDNc(
        fiName, null, 0, // standardizeWhat=0
        null, 0, 0);
    results = table.toString();
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 16 ;\n"
            + "variables:\n"
            + "\tdouble time(row) ;\n"
            + "\t\ttime:long_name = \"time\" ;\n"
            + "\t\ttime:units = \"seconds since 1948-1-1\" ;\n"
            + "\tfloat depth(row) ;\n"
            + "\t\tdepth:long_name = \"depth index\" ;\n"
            + "\tfloat lat(row) ;\n"
            + "\t\tlat:_FillValue = -999.0f ;\n"
            + "\t\tlat:long_name = \"latitude\" ;\n"
            + "\t\tlat:missing_value = -999.0f ;\n"
            + "\t\tlat:units = \"degrees_north\" ;\n"
            + "\tfloat lon(row) ;\n"
            + "\t\tlon:_FillValue = -999.0f ;\n"
            + "\t\tlon:long_name = \"longitude\" ;\n"
            + "\t\tlon:missing_value = -999.0f ;\n"
            + "\t\tlon:units = \"degrees_east\" ;\n"
            + "\tint stationyear(row) ;\n"
            + "\t\tstationyear:_FillValue = -9999 ;\n"
            + "\t\tstationyear:long_name = \"Station Year\" ;\n"
            + "\t\tstationyear:missing_value = -9999 ;\n"
            + "\t\tstationyear:units = \"years\" ;\n"
            + "\tint stationmonth(row) ;\n"
            + "\t\tstationmonth:_FillValue = -9999 ;\n"
            + "\t\tstationmonth:long_name = \"Station Month\" ;\n"
            + "\t\tstationmonth:missing_value = -9999 ;\n"
            + "\t\tstationmonth:units = \"months\" ;\n"
            + "\tint stationday(row) ;\n"
            + "\t\tstationday:_FillValue = -9999 ;\n"
            + "\t\tstationday:long_name = \"Station Day\" ;\n"
            + "\t\tstationday:missing_value = -9999 ;\n"
            + "\t\tstationday:units = \"days\" ;\n"
            + "\tint stime(row) ;\n"
            + "\t\tstime:_FillValue = -9999 ;\n"
            + "\t\tstime:long_name = \"Cast Time (GMT)\" ;\n"
            + "\t\tstime:missing_value = -9999 ;\n"
            + "\t\tstime:units = \"GMT\" ;\n"
            + "\tfloat stationline(row) ;\n"
            + "\t\tstationline:_FillValue = -999.0f ;\n"
            + "\t\tstationline:long_name = \"CALCOFI Line Number\" ;\n"
            + "\t\tstationline:missing_value = -999.0f ;\n"
            + "\t\tstationline:units = \"number\" ;\n"
            + "\tfloat stationnum(row) ;\n"
            + "\t\tstationnum:_FillValue = -999.0f ;\n"
            + "\t\tstationnum:long_name = \"CALCOFI Station Number\" ;\n"
            + "\t\tstationnum:missing_value = -999.0f ;\n"
            + "\t\tstationnum:units = \"number\" ;\n"
            + "\tfloat temperature(row) ;\n"
            + "\t\ttemperature:_FillValue = -999.0f ;\n"
            + "\t\ttemperature:has_data = 1 ;\n"
            + "\t\ttemperature:long_name = \"Temperature\" ;\n"
            + "\t\ttemperature:missing_value = -999.0f ;\n"
            + "\t\ttemperature:units = \"degC\" ;\n"
            + "\tfloat salinity(row) ;\n"
            + "\t\tsalinity:_FillValue = -999.0f ;\n"
            + "\t\tsalinity:has_data = 1 ;\n"
            + "\t\tsalinity:long_name = \"Salinity\" ;\n"
            + "\t\tsalinity:missing_value = -999.0f ;\n"
            + "\t\tsalinity:units = \"PSU\" ;\n"
            + "\tfloat pressure(row) ;\n"
            + "\t\tpressure:_FillValue = -999.0f ;\n"
            + "\t\tpressure:has_data = 0 ;\n"
            + "\t\tpressure:long_name = \"Pressure\" ;\n"
            + "\t\tpressure:missing_value = -999.0f ;\n"
            + "\t\tpressure:units = \"decibars\" ;\n"
            + "\tfloat oxygen(row) ;\n"
            + "\t\toxygen:_FillValue = -999.0f ;\n"
            + "\t\toxygen:has_data = 1 ;\n"
            + "\t\toxygen:long_name = \"Oxygen\" ;\n"
            + "\t\toxygen:missing_value = -999.0f ;\n"
            + "\t\toxygen:units = \"milliliters/liter\" ;\n"
            + "\tfloat po4(row) ;\n"
            + "\t\tpo4:_FillValue = -999.0f ;\n"
            + "\t\tpo4:has_data = 1 ;\n"
            + "\t\tpo4:long_name = \"Phosphate\" ;\n"
            + "\t\tpo4:missing_value = -999.0f ;\n"
            + "\t\tpo4:units = \"ugram-atoms/liter\" ;\n"
            + "\tfloat silicate(row) ;\n"
            + "\t\tsilicate:_FillValue = -999.0f ;\n"
            + "\t\tsilicate:has_data = 0 ;\n"
            + "\t\tsilicate:long_name = \"Silicate\" ;\n"
            + "\t\tsilicate:missing_value = -999.0f ;\n"
            + "\t\tsilicate:units = \"ugram-atoms/liter\" ;\n"
            + "\tfloat no2(row) ;\n"
            + "\t\tno2:_FillValue = -999.0f ;\n"
            + "\t\tno2:has_data = 0 ;\n"
            + "\t\tno2:long_name = \"Nitrite\" ;\n"
            + "\t\tno2:missing_value = -999.0f ;\n"
            + "\t\tno2:units = \"ugram-atoms/liter\" ;\n"
            + "\tfloat no3(row) ;\n"
            + "\t\tno3:_FillValue = -999.0f ;\n"
            + "\t\tno3:has_data = 0 ;\n"
            + "\t\tno3:long_name = \"Nitrate\" ;\n"
            + "\t\tno3:missing_value = -999.0f ;\n"
            + "\t\tno3:units = \"ugram-atoms/liter\" ;\n"
            + "\tfloat nh3(row) ;\n"
            + "\t\tnh3:_FillValue = -999.0f ;\n"
            + "\t\tnh3:has_data = 0 ;\n"
            + "\t\tnh3:long_name = \"Ammonia\" ;\n"
            + "\t\tnh3:missing_value = -999.0f ;\n"
            + "\t\tnh3:units = \"ugram-atoms/liter\" ;\n"
            + "\tfloat chl(row) ;\n"
            + "\t\tchl:_FillValue = -999.0f ;\n"
            + "\t\tchl:has_data = 0 ;\n"
            + "\t\tchl:long_name = \"Chlorophyll-a\" ;\n"
            + "\t\tchl:missing_value = -999.0f ;\n"
            + "\t\tchl:units = \"milligrams/meter**3\" ;\n"
            + "\tfloat dark(row) ;\n"
            + "\t\tdark:_FillValue = -999.0f ;\n"
            + "\t\tdark:has_data = 0 ;\n"
            + "\t\tdark:long_name = \"Dark Bottle C14 Assimilation\" ;\n"
            + "\t\tdark:missing_value = -999.0f ;\n"
            + "\t\tdark:units = \"milligrams/meter**3/experiment\" ;\n"
            + "\tfloat primprod(row) ;\n"
            + "\t\tprimprod:_FillValue = -999.0f ;\n"
            + "\t\tprimprod:has_data = 0 ;\n"
            + "\t\tprimprod:long_name = \"Mean Primary Production (C14 Assimilation)\" ;\n"
            + "\t\tprimprod:missing_value = -999.0f ;\n"
            + "\t\tprimprod:units = \"milligrams/meter**3/experiment\" ;\n"
            + "\tfloat lightpercent(row) ;\n"
            + "\t\tlightpercent:_FillValue = -999.0f ;\n"
            + "\t\tlightpercent:has_data = 0 ;\n"
            + "\t\tlightpercent:long_name = \"Percent Light (for incubations)\" ;\n"
            + "\t\tlightpercent:missing_value = -999.0f ;\n"
            + "\t\tlightpercent:units = \"milligrams/meter**3/experiment\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:history = \"created by ERD from Matlab database created by Andrew Leising  from the CalCOFI Physical data\" ;\n"
            + "\t\t:title = \"CalCOFI Physical Observations, 1949-2001\" ;\n"
            + "}\n"
            + "time,depth,lat,lon,stationyear,stationmonth,stationday,stime,stationline,stationnum,temperature,salinity,pressure,oxygen,po4,silicate,no2,no3,nh3,chl,dark,primprod,lightpercent\n"
            + "6.3612E7,0.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.19,33.6,-999.0,5.3,0.42,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,22.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.18,33.6,-999.0,5.26,0.38,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,49.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.2,33.6,-999.0,5.3,0.36,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,72.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,14.95,33.58,-999.0,5.51,0.37,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,98.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,13.02,33.35,-999.0,5.35,0.45,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,147.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,11.45,33.36,-999.0,4.99,0.81,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,194.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,9.32,33.55,-999.0,4.47,1.19,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,241.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,8.51,33.85,-999.0,4.02,1.51,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,287.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,7.74,33.95,-999.0,3.48,1.76,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,384.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,6.42,33.97,-999.0,2.55,2.15,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,477.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,5.35,34.04,-999.0,1.29,2.48,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,576.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.83,34.14,-999.0,0.73,2.73,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,673.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.44,34.22,-999.0,0.48,2.9,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,768.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.15,34.31,-999.0,0.37,2.87,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,969.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,3.67,34.43,-999.0,0.49,2.8,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n"
            + "6.3612E7,1167.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,3.3,34.49,-999.0,0.66,2.7,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test specify vars (including out-of-order axis var, and nonsense var),
    // !getMetadata
    table.readNDNc(
        fiName,
        new String[] {"temperature", "lat", "salinity", "junk"},
        0, // standardizeWhat=0
        "depth",
        100,
        200);
    results = table.dataToString();
    expected =
        "time,depth,lat,lon,temperature,salinity\n"
            + "6.3612E7,147.0,33.31667,-128.53333,11.45,33.36\n"
            + "6.3612E7,194.0,33.31667,-128.53333,9.32,33.55\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test String vars
    fiName =
        "c:/erddapBPD/copy/cPostDet3/BARBARAx20BLOCK/LAMNAx20DITROPIS/Nx2fA/52038_A69-1303_1059305.nc";
    table.readNDNc(
        fiName, null, 0, // standardizeWhat=0
        null, 0, 0);
    results = table.dataToString(4);
    expected =
        "row,unique_tag_id,PI,longitude,latitude,time,bottom_depth,common_name,date_public,line,position_on_subarray,project,riser_height,role,scientific_name,serial_number,stock,surgery_time,surgery_location,tagger\n"
            + "0,52038_A69-1303_1059305,BARBARA BLOCK,-146.1137,60.7172,1.2192849E9,,SALMON SHARK,1.273271649385E9,,,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n"
            + "1,52038_A69-1303_1059305,BARBARA BLOCK,-146.32355,60.66713,1.233325298E9,127.743902439024,SALMON SHARK,1.273271649385E9,PORT GRAVINA,6,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n"
            + "2,52038_A69-1303_1059305,BARBARA BLOCK,-146.32355,60.66713,1.233325733E9,127.743902439024,SALMON SHARK,1.273271649385E9,PORT GRAVINA,6,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n"
            + "3,52038_A69-1303_1059305,BARBARA BLOCK,-146.32355,60.66713,1.233325998E9,127.743902439024,SALMON SHARK,1.273271649385E9,PORT GRAVINA,6,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test 4D but request axis vars only, with constraints
    fiName =
        TableTests.class
            .getResource("/data/points/ndbcMet2/historical/NDBC_51001_met.nc")
            .getPath(); // implied
    // c:
    table.readNDNc(
        fiName,
        new String[] {"LON", "LAT", "TIME"},
        0, // standardizeWhat=0
        "TIME",
        1.2051936e9,
        1.20528e9);
    results = table.dataToString(4);
    expected =
        "LON,LAT,TIME\n"
            + "-162.279,23.445,1.2051894E9\n"
            + "-162.279,23.445,1.205193E9\n"
            + "-162.279,23.445,1.2051966E9\n"
            + "-162.279,23.445,1.2052002E9\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests readNDNC. */
  @org.junit.jupiter.api.Test
  void testReadNDNc2() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // String2.log("\n*** Table.testReadNDNc2");
    String fiName = TableTests.class.getResource("/data/wodSample/wod_008015632O.nc").getPath();
    Table table = new Table();
    String results, expected;

    // test no vars specified
    table.readNDNc(
        fiName, null, 0, // standardizeWhat=0
        null, 0, 0);
    results = table.toString();
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 11 ;\n"
            + "\tWOD_cruise_identifier_strlen = 8 ;\n"
            + "\tProject_strlen = 49 ;\n"
            + "\tdataset_strlen = 14 ;\n"
            + "variables:\n"
            + "\tfloat z(row) ;\n"
            + "\t\tz:long_name = \"depth_below_sea_level\" ;\n"
            + "\t\tz:positive = \"down\" ;\n"
            + "\t\tz:standard_name = \"altitude\" ;\n"
            + "\t\tz:units = \"m\" ;\n"
            + "\tfloat Temperature(row) ;\n"
            + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
            + "\t\tTemperature:flag_definitions = \"WODfp\" ;\n"
            + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
            + "\t\tTemperature:Instrument_(WOD_code) = \"UNDERWAY: MK3 data recording tag (Wildlife Computers) mounted on elephant seal\" ;\n"
            + "\t\tTemperature:long_name = \"Temperature\" ;\n"
            + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
            + "\t\tTemperature:units = \"degree_C\" ;\n"
            + "\t\tTemperature:WODprofile_flag = 9 ;\n"
            + "\tint Temperature_sigfigs(row) ;\n"
            + "\tint Temperature_WODflag(row) ;\n"
            + "\t\tTemperature_WODflag:flag_definitions = \"WODf\" ;\n"
            + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
            + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
            + "\t\tWOD_cruise_identifier:country = \"UNITED STATES\" ;\n"
            + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
            + "\tint wod_unique_cast(row) ;\n"
            + "\tfloat lat(row) ;\n"
            + "\t\tlat:axis = \"Y\" ;\n"
            + "\t\tlat:long_name = \"latitude\" ;\n"
            + "\t\tlat:standard_name = \"latitude\" ;\n"
            + "\t\tlat:units = \"degrees_north\" ;\n"
            + "\tfloat lon(row) ;\n"
            + "\t\tlon:axis = \"X\" ;\n"
            + "\t\tlon:long_name = \"longitude\" ;\n"
            + "\t\tlon:standard_name = \"longitude\" ;\n"
            + "\t\tlon:units = \"degrees_east\" ;\n"
            + "\tdouble time(row) ;\n"
            + "\t\ttime:axis = \"T\" ;\n"
            + "\t\ttime:long_name = \"time\" ;\n"
            + "\t\ttime:standard_name = \"time\" ;\n"
            + "\t\ttime:units = \"days since 1770-01-01 00:00:00\" ;\n"
            + "\tint date(row) ;\n"
            + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
            + "\t\tdate:long_name = \"date\" ;\n"
            + "\tfloat GMT_time(row) ;\n"
            + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
            + "\tint Access_no(row) ;\n"
            + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
            + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
            + "\t\tAccess_no:units = \"NODC_code\" ;\n"
            + "\tchar Project(row, Project_strlen) ;\n"
            + "\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n"
            + "\t\tProject:long_name = \"Project_name\" ;\n"
            + "\tchar dataset(row, dataset_strlen) ;\n"
            + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
            + "\tfloat ARGOS_last_fix(row) ;\n"
            + "\t\tARGOS_last_fix:units = \"hours\" ;\n"
            + "\tfloat ARGOS_next_fix(row) ;\n"
            + "\t\tARGOS_next_fix:units = \"hours\" ;\n"
            + "\tint crs(row) ;\n"
            + "\t\tcrs:epsg_code = \"EPSG:4326\" ;\n"
            + "\t\tcrs:grid_mapping_name = \"latitude_longitude\" ;\n"
            + "\t\tcrs:inverse_flattening = 298.25723f ;\n"
            + "\t\tcrs:longitude_of_prime_meridian = 0.0f ;\n"
            + "\t\tcrs:semi_major_axis = 6378137.0f ;\n"
            + "\tint profile(row) ;\n"
            + "\tint WODf(row) ;\n"
            + "\t\tWODf:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
            + "\t\tWODf:flag_values = \"0 1 2 3 4 5 6 7 8 9\" ;\n"
            + "\t\tWODf:long_name = \"WOD_observation_flag\" ;\n"
            + "\tint WODfp(row) ;\n"
            + "\t\tWODfp:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
            + "\t\tWODfp:flag_values = \"0 1 2 3 4 5 6 7 8 9\" ;\n"
            + "\t\tWODfp:long_name = \"WOD_profile_flag\" ;\n"
            + "\tint WODfd(row) ;\n"
            + "\t\tWODfd:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
            + "\t\tWODfd:flag_values = \"0 1 2\" ;\n"
            + "\t\tWODfd:long_name = \"WOD_depth_level_\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:cdm_data_type = \"Profile\" ;\n"
            + "\t\t:cf_role = \"profile_id\" ;\n"
            + "\t\t:Conventions = \"CF-1.5\" ;\n"
            + "\t\t:featureType = \"profile\" ;\n"
            + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
            + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
            + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
            + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
            + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
            + "\t\t:Metadata_Conventions = \"Unidata Dataset Discovery v1.0\" ;\n"
            + "\t\t:standard_name_vocabulary = \"CF-1.5\" ;\n"
            + "}\n"
            + "z,Temperature,Temperature_sigfigs,Temperature_WODflag,WOD_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,dataset,ARGOS_last_fix,ARGOS_next_fix,crs,profile,WODf,WODfp,WODfd\n"
            + "0.0,7.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "10.0,7.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "42.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "76.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "120.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "166.0,7.5,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "212.0,7.0,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "260.0,6.5,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "308.0,5.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "354.0,5.2,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n"
            + "402.0,4.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test specify 0D and 1D data vars (out-of-order, implied axis var, and
    // nonsense var), !getMetadata
    table.readNDNc(
        fiName,
        new String[] {"lon", "lat", "time", "Temperature", "WOD_cruise_identifier", "junk"},
        0, // standardizeWhat=0
        "z",
        100,
        200);
    results = table.dataToString();
    expected =
        "z,Temperature,WOD_cruise_identifier,lat,lon,time\n"
            + "120.0,7.8,US025547,45.28,-142.24,83369.90625\n"
            + "166.0,7.5,US025547,45.28,-142.24,83369.90625\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // request axis vars only, with constraints
    table.readNDNc(
        fiName,
        new String[] {"z"},
        0, // standardizeWhat=0
        "z",
        100,
        200);
    results = table.dataToString();
    expected = "z\n" + "120.0\n" + "166.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // request 0D vars only, with constraints (ignored)
    table.readNDNc(
        fiName,
        new String[] {"WOD_cruise_identifier", "Project", "junk", "lon", "lat"},
        0, // standardizeWhat=0
        "z",
        100,
        200);
    results = table.dataToString();
    expected =
        "WOD_cruise_identifier,lat,lon,Project\n"
            + "US025547,45.28,-142.24,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES)\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // request axis var and 0D vars only, with constraints
    table.readNDNc(
        fiName,
        new String[] {"WOD_cruise_identifier", "Project", "z", "junk", "lon", "lat"},
        0, // standardizeWhat=0
        "z",
        100,
        200);
    results = table.dataToString();
    expected =
        "z,WOD_cruise_identifier,lat,lon,Project\n"
            + "120.0,US025547,45.28,-142.24,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES)\n"
            + "166.0,US025547,45.28,-142.24,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES)\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** Test readMultidimNc with treatDimensionsAs specified. */
  @org.junit.jupiter.api.Test
  void testHardReadMultidimNc() throws Exception {
    String fileName =
        TableTests.class.getResource("/data/nc/GLsubdir/GL_201207_TS_DB_44761.nc").getPath();
    // String2.log("\n*** Table.testHardReadMultidimNc(" + fileName + ")");
    // Table.debugMode = true;
    Table table = new Table();
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
    reader.readMultidimNc(
        fileName,
        StringArray.fromCSV("TIME,LATITUDE,LONGITUDE,DEPTH,TEMP,TEMP_DM"), // loadVarNames,
        null, // loadDimNames,
        new String[][] {{"LATITUDE", "LONGITUDE", "TIME"}}, // treatDimensionsAs
        true, // getMetadata,
        0, // standardizeWhat,
        true, // removeMVRows,
        null,
        null,
        null); // conVars, conOps, conVals
    // String2.log(table.toString(5));
    String results = table.dataToString(5);
    String expected =
        "TIME,LATITUDE,LONGITUDE,DEPTH,TEMP,TEMP_DM\n"
            + "22837.541666666668,48.309,-44.112,-99999.0,12.6,R\n"
            + "22837.583333333332,48.313,-44.111,-99999.0,12.6,R\n"
            + "22837.625,48.317,-44.107,-99999.0,12.7,R\n"
            + "22837.666666666668,48.318,-44.103,-99999.0,13.0,R\n"
            + "22837.708333333332,48.315,-44.097,-99999.0,13.1,R\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // time
    Test.ensureEqual(table.getColumnName(0), "TIME", "");
    Test.ensureEqual(
        table.columnAttributes(0).getString("units"),
        "days since 1950-01-01T00:00:00Z",
        "results=\n" + results);
    Test.ensureEqual(table.columnAttributes(0).getDouble("valid_min"), 0.0, "results=\n" + results);
    Test.ensureEqual(
        table.columnAttributes(0).getDouble("valid_max"), 90000.0, "results=\n" + results);
    // depth
    Test.ensureEqual(table.getColumnName(3), "DEPTH", "");
    Test.ensureEqual(
        table.columnAttributes(3).getFloat("_FillValue"), -99999f, "results=\n" + results);
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "meter", "results=\n" + results);
    // temp
    Test.ensureEqual(table.getColumnName(4), "TEMP", "");
    Test.ensureEqual(
        table.columnAttributes(4).getFloat("_FillValue"), 9.96921E36f, "results=\n" + results);
    Test.ensureEqual(
        table.columnAttributes(4).getString("units"), "degree_Celsius", "results=\n" + results);

    // standardizeWhat
    table.clear();
    reader.readMultidimNc(
        fileName,
        StringArray.fromCSV("TIME,LATITUDE,LONGITUDE,DEPTH,TEMP,TEMP_DM"), // loadVarNames,
        null, // loadDimNames,
        new String[][] {{"LATITUDE", "LONGITUDE", "TIME"}}, // treatDimensionsAs
        true, // getMetadata,
        1 + 2 + 4096, // standardizeWhat,
        true, // removeMVRows,
        null,
        null,
        null); // conVars, conOps, conVals
    // String2.log(table.toString(5));
    results = table.dataToString(5);
    expected =
        "TIME,LATITUDE,LONGITUDE,DEPTH,TEMP,TEMP_DM\n"
            + "1.3420116E9,48.309,-44.112,,12.6,R\n"
            + "1.3420152E9,48.313,-44.111,,12.6,R\n"
            + "1.3420188E9,48.317,-44.107,,12.7,R\n"
            + "1.3420224E9,48.318,-44.103,,13.0,R\n"
            + "1.342026E9,48.315,-44.097,,13.1,R\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // time
    Test.ensureEqual(table.getColumnName(0), "TIME", "");
    Test.ensureEqual(
        table.columnAttributes(0).getString("units"),
        "seconds since 1970-01-01T00:00:00Z",
        "results=\n" + results);
    Test.ensureEqual(
        table.columnAttributes(0).getDouble("valid_min"), -6.31152E8, "results=\n" + results);
    Test.ensureEqual(
        table.columnAttributes(0).getDouble("valid_max"), 7.144848E9, "results=\n" + results);
    // depth
    Test.ensureEqual(table.getColumnName(3), "DEPTH", "");
    Test.ensureEqual(
        table.columnAttributes(3).getFloat("_FillValue"), Float.NaN, "results=\n" + results);
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "m", "results=\n" + results);
    // temp
    Test.ensureEqual(table.getColumnName(4), "TEMP", "");
    Test.ensureEqual(
        table.columnAttributes(4).getFloat("_FillValue"), Float.NaN, "results=\n" + results);
    Test.ensureEqual(
        table.columnAttributes(4).getString("units"), "degree_C", "results=\n" + results);

    // Table.debugMode = false;
  }

  /** This tests unpack by reading an Argo Profile file. */
  @org.junit.jupiter.api.Test
  void testUnpack() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebugMode = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testUnpack");
    Table table = new Table();
    // ftp://ftp.ifremer.fr/ifremer/argo/dac/csio/2901175/2901175_prof.nc
    String fiName = TableTests.class.getResource("/data/nc/2901175_prof.nc").getPath();
    String results, expected;

    // ** test the original packed format for comparison
    TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
    reader.readMultidimNc(
        fiName,
        new StringArray(),
        new StringArray(),
        null,
        true,
        0,
        false, // readMetadata, unpack, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.toString(3);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 762 ;\n"
            + "\tDATA_TYPE_strlen = 12 ;\n"
            + "\tFORMAT_VERSION_strlen = 3 ;\n"
            + "\tHANDBOOK_VERSION_strlen = 3 ;\n"
            + "\tREFERENCE_DATE_TIME_strlen = 14 ;\n"
            + "\tDATE_CREATION_strlen = 14 ;\n"
            + "\tDATE_UPDATE_strlen = 14 ;\n"
            + "\tPLATFORM_NUMBER_strlen = 7 ;\n"
            + "\tPROJECT_NAME_strlen = 18 ;\n"
            + "\tPI_NAME_strlen = 11 ;\n"
            + "\tSTATION_PARAMETERS_strlen = 4 ;\n"
            + "\tDATA_CENTRE_strlen = 2 ;\n"
            + "\tDC_REFERENCE_strlen = 14 ;\n"
            + "\tDATA_STATE_INDICATOR_strlen = 2 ;\n"
            + "\tPLATFORM_TYPE_strlen = 4 ;\n"
            + "\tFLOAT_SERIAL_NO_strlen = 13 ;\n"
            + "\tFIRMWARE_VERSION_strlen = 6 ;\n"
            + "\tWMO_INST_TYPE_strlen = 3 ;\n"
            + "\tPOSITIONING_SYSTEM_strlen = 5 ;\n"
            + "\tVERTICAL_SAMPLING_SCHEME_strlen = 26 ;\n"
            + "\tPARAMETER_strlen = 4 ;\n"
            + "\tSCIENTIFIC_CALIB_EQUATION_strlen = 153 ;\n"
            + "\tSCIENTIFIC_CALIB_COEFFICIENT_strlen = 55 ;\n"
            + "\tSCIENTIFIC_CALIB_COMMENT_strlen = 124 ;\n"
            + "\tSCIENTIFIC_CALIB_DATE_strlen = 14 ;\n"
            + "variables:\n"
            + "\tchar DATA_TYPE(row, DATA_TYPE_strlen) ;\n"
            + "\t\tDATA_TYPE:conventions = \"Argo reference table 1\" ;\n"
            + "\t\tDATA_TYPE:long_name = \"Data type\" ;\n"
            + "\tchar FORMAT_VERSION(row, FORMAT_VERSION_strlen) ;\n"
            + "\t\tFORMAT_VERSION:long_name = \"File format version\" ;\n"
            + "\tchar HANDBOOK_VERSION(row, HANDBOOK_VERSION_strlen) ;\n"
            + "\t\tHANDBOOK_VERSION:long_name = \"Data handbook version\" ;\n"
            + "\tchar REFERENCE_DATE_TIME(row, REFERENCE_DATE_TIME_strlen) ;\n"
            + "\t\tREFERENCE_DATE_TIME:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tREFERENCE_DATE_TIME:long_name = \"Date of reference for Julian days\" ;\n"
            + "\tchar DATE_CREATION(row, DATE_CREATION_strlen) ;\n"
            + "\t\tDATE_CREATION:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tDATE_CREATION:long_name = \"Date of file creation\" ;\n"
            + "\tchar DATE_UPDATE(row, DATE_UPDATE_strlen) ;\n"
            + "\t\tDATE_UPDATE:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tDATE_UPDATE:long_name = \"Date of update of this file\" ;\n"
            + "\tchar PLATFORM_NUMBER(row, PLATFORM_NUMBER_strlen) ;\n"
            + "\t\tPLATFORM_NUMBER:conventions = \"WMO float identifier : A9IIIII\" ;\n"
            + "\t\tPLATFORM_NUMBER:long_name = \"Float unique identifier\" ;\n"
            + "\tchar PROJECT_NAME(row, PROJECT_NAME_strlen) ;\n"
            + "\t\tPROJECT_NAME:long_name = \"Name of the project\" ;\n"
            + "\tchar PI_NAME(row, PI_NAME_strlen) ;\n"
            + "\t\tPI_NAME:long_name = \"Name of the principal investigator\" ;\n"
            + "\tchar STATION_PARAMETERS(row, STATION_PARAMETERS_strlen) ;\n"
            + "\t\tSTATION_PARAMETERS:conventions = \"Argo reference table 3\" ;\n"
            + "\t\tSTATION_PARAMETERS:long_name = \"List of available parameters for the station\" ;\n"
            + "\tint CYCLE_NUMBER(row) ;\n"
            + "\t\tCYCLE_NUMBER:_FillValue = 99999 ;\n"
            + "\t\tCYCLE_NUMBER:conventions = \"0...N, 0 : launch cycle (if exists), 1 : first complete cycle\" ;\n"
            + "\t\tCYCLE_NUMBER:long_name = \"Float cycle number\" ;\n"
            + "\tchar DIRECTION(row) ;\n"
            + "\t\tDIRECTION:conventions = \"A: ascending profiles, D: descending profiles\" ;\n"
            + "\t\tDIRECTION:long_name = \"Direction of the station profiles\" ;\n"
            + "\tchar DATA_CENTRE(row, DATA_CENTRE_strlen) ;\n"
            + "\t\tDATA_CENTRE:conventions = \"Argo reference table 4\" ;\n"
            + "\t\tDATA_CENTRE:long_name = \"Data centre in charge of float data processing\" ;\n"
            + "\tchar DC_REFERENCE(row, DC_REFERENCE_strlen) ;\n"
            + "\t\tDC_REFERENCE:conventions = \"Data centre convention\" ;\n"
            + "\t\tDC_REFERENCE:long_name = \"Station unique identifier in data centre\" ;\n"
            + "\tchar DATA_STATE_INDICATOR(row, DATA_STATE_INDICATOR_strlen) ;\n"
            + "\t\tDATA_STATE_INDICATOR:conventions = \"Argo reference table 6\" ;\n"
            + "\t\tDATA_STATE_INDICATOR:long_name = \"Degree of processing the data have passed through\" ;\n"
            + "\tchar DATA_MODE(row) ;\n"
            + "\t\tDATA_MODE:conventions = \"R : real time; D : delayed mode; A : real time with adjustment\" ;\n"
            + "\t\tDATA_MODE:long_name = \"Delayed mode or real time data\" ;\n"
            + "\tchar PLATFORM_TYPE(row, PLATFORM_TYPE_strlen) ;\n"
            + "\t\tPLATFORM_TYPE:conventions = \"Argo reference table 23\" ;\n"
            + "\t\tPLATFORM_TYPE:long_name = \"Type of float\" ;\n"
            + "\tchar FLOAT_SERIAL_NO(row, FLOAT_SERIAL_NO_strlen) ;\n"
            + "\t\tFLOAT_SERIAL_NO:long_name = \"Serial number of the float\" ;\n"
            + "\tchar FIRMWARE_VERSION(row, FIRMWARE_VERSION_strlen) ;\n"
            + "\t\tFIRMWARE_VERSION:long_name = \"Instrument firmware version\" ;\n"
            + "\tchar WMO_INST_TYPE(row, WMO_INST_TYPE_strlen) ;\n"
            + "\t\tWMO_INST_TYPE:conventions = \"Argo reference table 8\" ;\n"
            + "\t\tWMO_INST_TYPE:long_name = \"Coded instrument type\" ;\n"
            + "\tdouble JULD(row) ;\n"
            + "\t\tJULD:_FillValue = 999999.0 ;\n"
            + "\t\tJULD:axis = \"T\" ;\n"
            + "\t\tJULD:conventions = \"Relative julian days with decimal part (as parts of day)\" ;\n"
            + "\t\tJULD:long_name = \"Julian day (UTC) of the station relative to REFERENCE_DATE_TIME\" ;\n"
            + "\t\tJULD:resolution = 0.0 ;\n"
            + "\t\tJULD:standard_name = \"time\" ;\n"
            + "\t\tJULD:units = \"days since 1950-01-01 00:00:00 UTC\" ;\n"
            + "\tchar JULD_QC(row) ;\n"
            + "\t\tJULD_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tJULD_QC:long_name = \"Quality on date and time\" ;\n"
            + "\tdouble JULD_LOCATION(row) ;\n"
            + "\t\tJULD_LOCATION:_FillValue = 999999.0 ;\n"
            + "\t\tJULD_LOCATION:conventions = \"Relative julian days with decimal part (as parts of day)\" ;\n"
            + "\t\tJULD_LOCATION:long_name = \"Julian day (UTC) of the location relative to REFERENCE_DATE_TIME\" ;\n"
            + "\t\tJULD_LOCATION:resolution = 0.0 ;\n"
            + "\t\tJULD_LOCATION:units = \"days since 1950-01-01 00:00:00 UTC\" ;\n"
            + "\tdouble LATITUDE(row) ;\n"
            + "\t\tLATITUDE:_FillValue = 99999.0 ;\n"
            + "\t\tLATITUDE:axis = \"Y\" ;\n"
            + "\t\tLATITUDE:long_name = \"Latitude of the station, best estimate\" ;\n"
            + "\t\tLATITUDE:standard_name = \"latitude\" ;\n"
            + "\t\tLATITUDE:units = \"degree_north\" ;\n"
            + "\t\tLATITUDE:valid_max = 90.0 ;\n"
            + "\t\tLATITUDE:valid_min = -90.0 ;\n"
            + "\tdouble LONGITUDE(row) ;\n"
            + "\t\tLONGITUDE:_FillValue = 99999.0 ;\n"
            + "\t\tLONGITUDE:axis = \"X\" ;\n"
            + "\t\tLONGITUDE:long_name = \"Longitude of the station, best estimate\" ;\n"
            + "\t\tLONGITUDE:standard_name = \"longitude\" ;\n"
            + "\t\tLONGITUDE:units = \"degree_east\" ;\n"
            + "\t\tLONGITUDE:valid_max = 180.0 ;\n"
            + "\t\tLONGITUDE:valid_min = -180.0 ;\n"
            + "\tchar POSITION_QC(row) ;\n"
            + "\t\tPOSITION_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPOSITION_QC:long_name = \"Quality on position (latitude and longitude)\" ;\n"
            + "\tchar POSITIONING_SYSTEM(row, POSITIONING_SYSTEM_strlen) ;\n"
            + "\t\tPOSITIONING_SYSTEM:long_name = \"Positioning system\" ;\n"
            + "\tchar PROFILE_PRES_QC(row) ;\n"
            + "\t\tPROFILE_PRES_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_PRES_QC:long_name = \"Global quality flag of PRES profile\" ;\n"
            + "\tchar PROFILE_TEMP_QC(row) ;\n"
            + "\t\tPROFILE_TEMP_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_TEMP_QC:long_name = \"Global quality flag of TEMP profile\" ;\n"
            + "\tchar PROFILE_PSAL_QC(row) ;\n"
            + "\t\tPROFILE_PSAL_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_PSAL_QC:long_name = \"Global quality flag of PSAL profile\" ;\n"
            + "\tchar VERTICAL_SAMPLING_SCHEME(row, VERTICAL_SAMPLING_SCHEME_strlen) ;\n"
            + "\t\tVERTICAL_SAMPLING_SCHEME:conventions = \"Argo reference table 16\" ;\n"
            + "\t\tVERTICAL_SAMPLING_SCHEME:long_name = \"Vertical sampling scheme\" ;\n"
            + "\tint CONFIG_MISSION_NUMBER(row) ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:_FillValue = 99999 ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:conventions = \"1...N, 1 : first complete mission\" ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:long_name = \"Unique number denoting the missions performed by the float\" ;\n"
            + "\tchar PARAMETER(row, PARAMETER_strlen) ;\n"
            + "\t\tPARAMETER:conventions = \"Argo reference table 3\" ;\n"
            + "\t\tPARAMETER:long_name = \"List of parameters with calibration information\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_EQUATION(row, SCIENTIFIC_CALIB_EQUATION_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_EQUATION:long_name = \"Calibration equation for this parameter\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_COEFFICIENT(row, SCIENTIFIC_CALIB_COEFFICIENT_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_COEFFICIENT:long_name = \"Calibration coefficients for this equation\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_COMMENT(row, SCIENTIFIC_CALIB_COMMENT_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_COMMENT:long_name = \"Comment applying to this parameter calibration\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_DATE(row, SCIENTIFIC_CALIB_DATE_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_DATE:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tSCIENTIFIC_CALIB_DATE:long_name = \"Date of calibration\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:Conventions = \"Argo-3.1 CF-1.6\" ;\n"
            + "\t\t:featureType = \"trajectoryProfile\" ;\n"
            + "\t\t:history = \"2016-04-15T20:47:22Z creation\" ;\n"
            + "\t\t:institution = \"Coriolis GDAC\" ;\n"
            + "\t\t:references = \"http://www.argodatamgt.org/Documentation\" ;\n"
            + "\t\t:source = \"Argo float\" ;\n"
            + "\t\t:title = \"Argo float vertical profile\" ;\n"
            + "\t\t:user_manual_version = \"3.1\" ;\n"
            + "}\n"
            + "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,STATION_PARAMETERS,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PARAMETER,SCIENTIFIC_CALIB_EQUATION,SCIENTIFIC_CALIB_COEFFICIENT,SCIENTIFIC_CALIB_COMMENT,SCIENTIFIC_CALIB_DATE\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PRES,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PRES,PRES_ADJUSTED = PRES - dP,dP =  0.1 dbar.,Pressures adjusted by using pressure offset at the sea surface. The quoted error is manufacturer specified accuracy in dbar.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,TEMP,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,TEMP,none,none,The quoted error is manufacturer specified accuracy with respect to ITS-90 at time of laboratory calibration.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PSAL,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PSAL,\"PSAL_ADJUSTED = sw_salt( sw_cndr(PSAL,TEMP,PRES), TEMP, PRES_ADJUSTED ); PSAL_ADJ corrects conductivity cell therm mass (CTM), Johnson et al, 2007, JAOT;\",\"same as for PRES_ADJUSTED; CTL: alpha=0.0267, tau=18.6;\",No significant salinity drift detected; SBE sensor accuracy,20110628060155\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ** don't specify varNames or dimNames -- it find vars with most dims
    reader.readMultidimNc(
        fiName,
        new StringArray(),
        new StringArray(),
        null,
        true,
        3,
        false, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.toString(3);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 762 ;\n"
            + "\tDATA_TYPE_strlen = 12 ;\n"
            + "\tFORMAT_VERSION_strlen = 3 ;\n"
            + "\tHANDBOOK_VERSION_strlen = 3 ;\n"
            + "\tREFERENCE_DATE_TIME_strlen = 14 ;\n"
            + "\tDATE_CREATION_strlen = 14 ;\n"
            + "\tDATE_UPDATE_strlen = 14 ;\n"
            + "\tPLATFORM_NUMBER_strlen = 7 ;\n"
            + "\tPROJECT_NAME_strlen = 18 ;\n"
            + "\tPI_NAME_strlen = 11 ;\n"
            + "\tSTATION_PARAMETERS_strlen = 4 ;\n"
            + "\tDATA_CENTRE_strlen = 2 ;\n"
            + "\tDC_REFERENCE_strlen = 14 ;\n"
            + "\tDATA_STATE_INDICATOR_strlen = 2 ;\n"
            + "\tPLATFORM_TYPE_strlen = 4 ;\n"
            + "\tFLOAT_SERIAL_NO_strlen = 13 ;\n"
            + "\tFIRMWARE_VERSION_strlen = 6 ;\n"
            + "\tWMO_INST_TYPE_strlen = 3 ;\n"
            + "\tPOSITIONING_SYSTEM_strlen = 5 ;\n"
            + "\tVERTICAL_SAMPLING_SCHEME_strlen = 26 ;\n"
            + "\tPARAMETER_strlen = 4 ;\n"
            + "\tSCIENTIFIC_CALIB_EQUATION_strlen = 153 ;\n"
            + "\tSCIENTIFIC_CALIB_COEFFICIENT_strlen = 55 ;\n"
            + "\tSCIENTIFIC_CALIB_COMMENT_strlen = 124 ;\n"
            + "\tSCIENTIFIC_CALIB_DATE_strlen = 14 ;\n"
            + "variables:\n"
            + "\tchar DATA_TYPE(row, DATA_TYPE_strlen) ;\n"
            + "\t\tDATA_TYPE:conventions = \"Argo reference table 1\" ;\n"
            + "\t\tDATA_TYPE:long_name = \"Data type\" ;\n"
            + "\tchar FORMAT_VERSION(row, FORMAT_VERSION_strlen) ;\n"
            + "\t\tFORMAT_VERSION:long_name = \"File format version\" ;\n"
            + "\tchar HANDBOOK_VERSION(row, HANDBOOK_VERSION_strlen) ;\n"
            + "\t\tHANDBOOK_VERSION:long_name = \"Data handbook version\" ;\n"
            + "\tchar REFERENCE_DATE_TIME(row, REFERENCE_DATE_TIME_strlen) ;\n"
            + "\t\tREFERENCE_DATE_TIME:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tREFERENCE_DATE_TIME:long_name = \"Date of reference for Julian days\" ;\n"
            + "\tchar DATE_CREATION(row, DATE_CREATION_strlen) ;\n"
            + "\t\tDATE_CREATION:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tDATE_CREATION:long_name = \"Date of file creation\" ;\n"
            + "\tchar DATE_UPDATE(row, DATE_UPDATE_strlen) ;\n"
            + "\t\tDATE_UPDATE:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tDATE_UPDATE:long_name = \"Date of update of this file\" ;\n"
            + "\tchar PLATFORM_NUMBER(row, PLATFORM_NUMBER_strlen) ;\n"
            + "\t\tPLATFORM_NUMBER:conventions = \"WMO float identifier : A9IIIII\" ;\n"
            + "\t\tPLATFORM_NUMBER:long_name = \"Float unique identifier\" ;\n"
            + "\tchar PROJECT_NAME(row, PROJECT_NAME_strlen) ;\n"
            + "\t\tPROJECT_NAME:long_name = \"Name of the project\" ;\n"
            + "\tchar PI_NAME(row, PI_NAME_strlen) ;\n"
            + "\t\tPI_NAME:long_name = \"Name of the principal investigator\" ;\n"
            + "\tchar STATION_PARAMETERS(row, STATION_PARAMETERS_strlen) ;\n"
            + "\t\tSTATION_PARAMETERS:conventions = \"Argo reference table 3\" ;\n"
            + "\t\tSTATION_PARAMETERS:long_name = \"List of available parameters for the station\" ;\n"
            + "\tint CYCLE_NUMBER(row) ;\n"
            + "\t\tCYCLE_NUMBER:_FillValue = 2147483647 ;\n"
            + "\t\tCYCLE_NUMBER:conventions = \"0...N, 0 : launch cycle (if exists), 1 : first complete cycle\" ;\n"
            + "\t\tCYCLE_NUMBER:long_name = \"Float cycle number\" ;\n"
            + "\tchar DIRECTION(row) ;\n"
            + "\t\tDIRECTION:conventions = \"A: ascending profiles, D: descending profiles\" ;\n"
            + "\t\tDIRECTION:long_name = \"Direction of the station profiles\" ;\n"
            + "\tchar DATA_CENTRE(row, DATA_CENTRE_strlen) ;\n"
            + "\t\tDATA_CENTRE:conventions = \"Argo reference table 4\" ;\n"
            + "\t\tDATA_CENTRE:long_name = \"Data centre in charge of float data processing\" ;\n"
            + "\tchar DC_REFERENCE(row, DC_REFERENCE_strlen) ;\n"
            + "\t\tDC_REFERENCE:conventions = \"Data centre convention\" ;\n"
            + "\t\tDC_REFERENCE:long_name = \"Station unique identifier in data centre\" ;\n"
            + "\tchar DATA_STATE_INDICATOR(row, DATA_STATE_INDICATOR_strlen) ;\n"
            + "\t\tDATA_STATE_INDICATOR:conventions = \"Argo reference table 6\" ;\n"
            + "\t\tDATA_STATE_INDICATOR:long_name = \"Degree of processing the data have passed through\" ;\n"
            + "\tchar DATA_MODE(row) ;\n"
            + "\t\tDATA_MODE:conventions = \"R : real time; D : delayed mode; A : real time with adjustment\" ;\n"
            + "\t\tDATA_MODE:long_name = \"Delayed mode or real time data\" ;\n"
            + "\tchar PLATFORM_TYPE(row, PLATFORM_TYPE_strlen) ;\n"
            + "\t\tPLATFORM_TYPE:conventions = \"Argo reference table 23\" ;\n"
            + "\t\tPLATFORM_TYPE:long_name = \"Type of float\" ;\n"
            + "\tchar FLOAT_SERIAL_NO(row, FLOAT_SERIAL_NO_strlen) ;\n"
            + "\t\tFLOAT_SERIAL_NO:long_name = \"Serial number of the float\" ;\n"
            + "\tchar FIRMWARE_VERSION(row, FIRMWARE_VERSION_strlen) ;\n"
            + "\t\tFIRMWARE_VERSION:long_name = \"Instrument firmware version\" ;\n"
            + "\tchar WMO_INST_TYPE(row, WMO_INST_TYPE_strlen) ;\n"
            + "\t\tWMO_INST_TYPE:conventions = \"Argo reference table 8\" ;\n"
            + "\t\tWMO_INST_TYPE:long_name = \"Coded instrument type\" ;\n"
            + "\tdouble JULD(row) ;\n"
            + "\t\tJULD:_FillValue = NaN ;\n"
            + "\t\tJULD:axis = \"T\" ;\n"
            + "\t\tJULD:conventions = \"Relative julian days with decimal part (as parts of day)\" ;\n"
            + "\t\tJULD:long_name = \"Julian day (UTC) of the station relative to REFERENCE_DATE_TIME\" ;\n"
            + "\t\tJULD:resolution = 0.0 ;\n"
            + "\t\tJULD:standard_name = \"time\" ;\n"
            + "\t\tJULD:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tchar JULD_QC(row) ;\n"
            + "\t\tJULD_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tJULD_QC:long_name = \"Quality on date and time\" ;\n"
            + "\tdouble JULD_LOCATION(row) ;\n"
            + "\t\tJULD_LOCATION:_FillValue = NaN ;\n"
            + "\t\tJULD_LOCATION:conventions = \"Relative julian days with decimal part (as parts of day)\" ;\n"
            + "\t\tJULD_LOCATION:long_name = \"Julian day (UTC) of the location relative to REFERENCE_DATE_TIME\" ;\n"
            + "\t\tJULD_LOCATION:resolution = 0.0 ;\n"
            + "\t\tJULD_LOCATION:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tdouble LATITUDE(row) ;\n"
            + "\t\tLATITUDE:_FillValue = NaN ;\n"
            + "\t\tLATITUDE:axis = \"Y\" ;\n"
            + "\t\tLATITUDE:long_name = \"Latitude of the station, best estimate\" ;\n"
            + "\t\tLATITUDE:standard_name = \"latitude\" ;\n"
            + "\t\tLATITUDE:units = \"degree_north\" ;\n"
            + "\t\tLATITUDE:valid_max = 90.0 ;\n"
            + "\t\tLATITUDE:valid_min = -90.0 ;\n"
            + "\tdouble LONGITUDE(row) ;\n"
            + "\t\tLONGITUDE:_FillValue = NaN ;\n"
            + "\t\tLONGITUDE:axis = \"X\" ;\n"
            + "\t\tLONGITUDE:long_name = \"Longitude of the station, best estimate\" ;\n"
            + "\t\tLONGITUDE:standard_name = \"longitude\" ;\n"
            + "\t\tLONGITUDE:units = \"degree_east\" ;\n"
            + "\t\tLONGITUDE:valid_max = 180.0 ;\n"
            + "\t\tLONGITUDE:valid_min = -180.0 ;\n"
            + "\tchar POSITION_QC(row) ;\n"
            + "\t\tPOSITION_QC:conventions = \"Argo reference table 2\" ;\n"
            + "\t\tPOSITION_QC:long_name = \"Quality on position (latitude and longitude)\" ;\n"
            + "\tchar POSITIONING_SYSTEM(row, POSITIONING_SYSTEM_strlen) ;\n"
            + "\t\tPOSITIONING_SYSTEM:long_name = \"Positioning system\" ;\n"
            + "\tchar PROFILE_PRES_QC(row) ;\n"
            + "\t\tPROFILE_PRES_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_PRES_QC:long_name = \"Global quality flag of PRES profile\" ;\n"
            + "\tchar PROFILE_TEMP_QC(row) ;\n"
            + "\t\tPROFILE_TEMP_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_TEMP_QC:long_name = \"Global quality flag of TEMP profile\" ;\n"
            + "\tchar PROFILE_PSAL_QC(row) ;\n"
            + "\t\tPROFILE_PSAL_QC:conventions = \"Argo reference table 2a\" ;\n"
            + "\t\tPROFILE_PSAL_QC:long_name = \"Global quality flag of PSAL profile\" ;\n"
            + "\tchar VERTICAL_SAMPLING_SCHEME(row, VERTICAL_SAMPLING_SCHEME_strlen) ;\n"
            + "\t\tVERTICAL_SAMPLING_SCHEME:conventions = \"Argo reference table 16\" ;\n"
            + "\t\tVERTICAL_SAMPLING_SCHEME:long_name = \"Vertical sampling scheme\" ;\n"
            + "\tint CONFIG_MISSION_NUMBER(row) ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:_FillValue = 2147483647 ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:conventions = \"1...N, 1 : first complete mission\" ;\n"
            + "\t\tCONFIG_MISSION_NUMBER:long_name = \"Unique number denoting the missions performed by the float\" ;\n"
            + "\tchar PARAMETER(row, PARAMETER_strlen) ;\n"
            + "\t\tPARAMETER:conventions = \"Argo reference table 3\" ;\n"
            + "\t\tPARAMETER:long_name = \"List of parameters with calibration information\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_EQUATION(row, SCIENTIFIC_CALIB_EQUATION_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_EQUATION:long_name = \"Calibration equation for this parameter\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_COEFFICIENT(row, SCIENTIFIC_CALIB_COEFFICIENT_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_COEFFICIENT:long_name = \"Calibration coefficients for this equation\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_COMMENT(row, SCIENTIFIC_CALIB_COMMENT_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_COMMENT:long_name = \"Comment applying to this parameter calibration\" ;\n"
            + "\tchar SCIENTIFIC_CALIB_DATE(row, SCIENTIFIC_CALIB_DATE_strlen) ;\n"
            + "\t\tSCIENTIFIC_CALIB_DATE:conventions = \"YYYYMMDDHHMISS\" ;\n"
            + "\t\tSCIENTIFIC_CALIB_DATE:long_name = \"Date of calibration\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:Conventions = \"Argo-3.1 CF-1.6\" ;\n"
            + "\t\t:featureType = \"trajectoryProfile\" ;\n"
            + "\t\t:history = \"2016-04-15T20:47:22Z creation\" ;\n"
            + "\t\t:institution = \"Coriolis GDAC\" ;\n"
            + "\t\t:references = \"http://www.argodatamgt.org/Documentation\" ;\n"
            + "\t\t:source = \"Argo float\" ;\n"
            + "\t\t:title = \"Argo float vertical profile\" ;\n"
            + "\t\t:user_manual_version = \"3.1\" ;\n"
            + "}\n"
            + "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,STATION_PARAMETERS,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PARAMETER,SCIENTIFIC_CALIB_EQUATION,SCIENTIFIC_CALIB_COEFFICIENT,SCIENTIFIC_CALIB_COMMENT,SCIENTIFIC_CALIB_DATE\n"
            +
            // from raw file dif JULD_LOCATION dif JULD_LOCATION
            // "Argo
            // profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA
            // ARGO PROJECT,JIANPING
            // XU,PRES,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PRES,PRES_ADJUSTED
            // = PRES - dP,dP = 0.1 dbar.,Pressures adjusted by using pressure offset at the
            // sea surface. The quoted error is manufacturer specified accuracy in
            // dbar.,20110628060155\n" +
            "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PRES,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,1.240301582E9,1,1.240301812E9,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PRES,PRES_ADJUSTED = PRES - dP,dP =  0.1 dbar.,Pressures adjusted by using pressure offset at the sea surface. The quoted error is manufacturer specified accuracy in dbar.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,TEMP,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,1.240301582E9,1,1.240301812E9,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,TEMP,none,none,The quoted error is manufacturer specified accuracy with respect to ITS-90 at time of laboratory calibration.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PSAL,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,1.240301582E9,1,1.240301812E9,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PSAL,\"PSAL_ADJUSTED = sw_salt( sw_cndr(PSAL,TEMP,PRES), TEMP, PRES_ADJUSTED ); PSAL_ADJ corrects conductivity cell therm mass (CTM), Johnson et al, 2007, JAOT;\",\"same as for PRES_ADJUSTED; CTL: alpha=0.0267, tau=18.6;\",No significant salinity drift detected; SBE sensor accuracy,20110628060155\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // done
    /* */
    // Table.debugMode = oDebugMode;
  }

  /** This tests readVlenNc. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // illegal heap address when reading file
  void testReadVlenNc() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // boolean oDebugMode = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadVlenNc");
    Table table = new Table();
    String fiName =
        TableTests.class.getResource("/largeFiles/nccf/vlen/rr2_vlen_test.nc").getPath();
    // String2.log(NcHelper.ncdump(fiName, "-h"));
    String results, expectedStart, expectedEnd;
    /* */

    // ** don't specify varNames or dimNames -- it find vars with most dims
    TableFromMultidimNcFile reader = new TableFromMultidimNcFile(table);
    reader.readMultidimNc(
        fiName,
        new StringArray(),
        new StringArray(),
        null,
        true,
        0,
        false, // readMetadata, standardizeWhat, removeMVRows
        null,
        null,
        null); // conVars, conOps, conVals
    results = table.dataToString(3);
    expectedStart =
        // static vars and vars like char SCIENTIFIC_CALIB_COEFFICIENT(N_PROF=254,
        // N_CALIB=1, N_PARAM=3, STRING256=256);
        "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,STATION_PARAMETERS,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PARAMETER,SCIENTIFIC_CALIB_EQUATION,SCIENTIFIC_CALIB_COEFFICIENT,SCIENTIFIC_CALIB_COMMENT,SCIENTIFIC_CALIB_DATE\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PRES,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PRES,PRES_ADJUSTED = PRES - dP,dP =  0.1 dbar.,Pressures adjusted by using pressure offset at the sea surface. The quoted error is manufacturer specified accuracy in dbar.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,TEMP,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,TEMP,none,none,The quoted error is manufacturer specified accuracy with respect to ITS-90 at time of laboratory calibration.,20110628060155\n"
            + "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PSAL,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PSAL,\"PSAL_ADJUSTED = sw_salt( sw_cndr(PSAL,TEMP,PRES), TEMP, PRES_ADJUSTED ); PSAL_ADJ corrects conductivity cell therm mass (CTM), Johnson et al, 2007, JAOT;\",\"same as for PRES_ADJUSTED; CTL: alpha=0.0267, tau=18.6;\",No significant salinity drift detected; SBE sensor accuracy,20110628060155\n"
            + "...\n";
    Test.ensureEqual(results, expectedStart, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 762, "nRows"); // 254*3
    /*
     * //* same but quick reject based on constraint
     * table.readVlenNc(fiName, new StringArray(), new StringArray(),
     * true, false, //readMetadata, removeMVRows
     * StringArray.fromCSV("FORMAT_VERSION,FORMAT_VERSION"), //conVars
     * StringArray.fromCSV("=,="), //conOps
     * StringArray.fromCSV("3.1,3.2")); //conVals
     * Test.ensureEqual(table.nRows(), 0, "nRows");
     *
     *
     *
     * //* test do removeMVRows
     * table.readVlenNc(fiName, null,
     * StringArray.fromCSV("ZZTOP, N_PROF, N_LEVELS"),
     * true, false, //readMetadata, removeMVRows
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3, true);
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     * Test.ensureEqual(table.nRows(), 17266, "nRows");
     *
     * //and test data at the end of that table
     * table.removeRows(0, table.nRows() - 3);
     * results = table.dataToString(5);
     * expectedEnd =
     * "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR\n"
     * +
     * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1850.0,1,1849.4,1,99999.0,2.106,1,2.106,1,99999.0,34.604,1,34.604,1,99999.0\n"
     * +
     * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1899.9,1,1899.3,1,99999.0,2.055,1,2.055,1,99999.0,34.612,1,34.612,1,99999.0\n"
     * +
     * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,24210.44662037037,1,24210.44662037037,26.587,154.853,1,ARGOS,A,A,A,Primary sampling: discrete,1,1950.0,1,1949.4,1,99999.0,2.014,1,2.014,1,99999.0,34.617,1,34.617,1,99999.0\n"
     * ;
     * Test.ensureEqual(results, expectedEnd, "results=\n" + results);
     *
     * //* same but quick reject based on constraint LAT,LON 26.587,154.853
     * //*** this takes 9ms while test above takes 99ms!
     * table.readVlenNc(fiName, null,
     * StringArray.fromCSV("ZZTOP, N_PROF, N_LEVELS"),
     * true, //readMetadata,
     * StringArray.fromCSV("LATITUDE"), //conVars
     * StringArray.fromCSV("="), //conOps
     * StringArray.fromCSV("45")); //conVals
     * Test.ensureEqual(table.nRows(), 0, "nRows");
     *
     * //* test different dim order (should be rearranged so the same)
     * table.readVlenNc(fiName, null,
     * StringArray.fromCSV("N_LEVELS, ZZTOP, N_PROF"),
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3, true);
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     * Test.ensureEqual(table.nRows(), 17266, "nRows");
     *
     * //and test data at the end of that table
     * table.removeRows(0, table.nRows() - 3);
     * results = table.dataToString(5, true);
     * Test.ensureEqual(results, expectedEnd, "results=\n" + results);
     *
     *
     * //* test different varNames
     * table.readVlenNc(fiName,
     * StringArray.fromCSV(
     * "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PRES,PRES_QC,PRES_ADJUSTED,PRES_ADJUSTED_QC,PRES_ADJUSTED_ERROR,TEMP,TEMP_QC,TEMP_ADJUSTED,TEMP_ADJUSTED_QC,TEMP_ADJUSTED_ERROR,PSAL,PSAL_QC,PSAL_ADJUSTED,PSAL_ADJUSTED_QC,PSAL_ADJUSTED_ERROR"
     * ),
     * null,
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3, true);
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     * Test.ensureEqual(table.nRows(), 17266, "nRows");
     *
     * //and test data at the end of that table
     * table.removeRows(0, table.nRows() - 3);
     * results = table.dataToString(5, true);
     * Test.ensureEqual(results, expectedEnd, "results=\n" + results);
     *
     *
     * //* test read JULD
     * table.readVlenNc(fiName, StringArray.fromCSV("JULD"), null,
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3);
     * expectedStart =
     * "JULD\n" +
     * "21660.34238425926\n" +
     * "21670.351828703704\n" +
     * "21680.386898148146\n" +
     * "...\n";
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     * Test.ensureEqual(table.nRows(), 254, "nRows"); //same as when all variables
     * were explicitly loaded
     *
     * table.removeRows(0, 251);
     * results = table.dataToString(1000);
     * expectedStart =
     * "JULD\n" +
     * "24190.451828703703\n" +
     * "24200.381412037037\n" +
     * "24210.44662037037\n";
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     *
     *
     * //* test read JULD && PRES
     * table.readVlenNc(fiName, StringArray.fromCSV("JULD,PRES"), null,
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3);
     * expectedStart =
     * "JULD,PRES\n" +
     * "21660.34238425926,5.9\n" + //JULD is correctly JOINed
     * "21660.34238425926,10.0\n" +
     * "21660.34238425926,20.1\n" +
     * "...\n";
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     * Test.ensureEqual(table.nRows(), 17266, "nRows"); //same as when all variables
     * were explicitly loaded
     *
     * table.removeRows(0, 17263);
     * results = table.dataToString(1000);
     * expectedStart =
     * "JULD,PRES\n" +
     * "24210.44662037037,1850.0\n" + //JULD is correctly JOINed
     * "24210.44662037037,1899.9\n" +
     * "24210.44662037037,1950.0\n";
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     *
     *
     * //* test read just static vars, in a different order
     * table.readVlenNc(fiName,
     * StringArray.fromCSV("HANDBOOK_VERSION,FORMAT_VERSION,DATA_TYPE"),
     * null,
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3);
     * expectedStart =
     * "HANDBOOK_VERSION,FORMAT_VERSION,DATA_TYPE\n" +
     * "1.2,3.1,Argo profile\n";
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     *
     *
     * //* test read 0 dim variable -> empty table
     * table.readVlenNc(fiName,
     * StringArray.fromCSV("HISTORY_INSTITUTION"),
     * null,
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * Test.ensureEqual(table.nRows(), 0, "");
     * Test.ensureEqual(table.nColumns(), 0, "");
     *
     * //* test read non-existent dim -> just scalar vars
     * table.readVlenNc(fiName,
     * null,
     * StringArray.fromCSV("ZZTOP"),
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * results = table.dataToString(3);
     * expectedStart =
     * "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE\n"
     * +
     * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722\n";
     * Test.ensureEqual(results, expectedStart, "results=\n" + results);
     *
     * //* test read non-existent Var -> empty table
     * table.readVlenNc(fiName,
     * StringArray.fromCSV("ZZTOP"),
     * null,
     * true, //readMetadata,
     * null, null, null); //conVars, conOps, conVals
     * Test.ensureEqual(table.nRows(), 0, "");
     * Test.ensureEqual(table.nColumns(), 0, "");
     *
     *
     * //done
     * /*
     */
    // Table.debugMode = oDebugMode;
  }

  /** NOT FINISHED. This tests readNcSequence. */
  /*
   * public static void testReadNcSequence() throws Exception {
   * // Table.verbose = true;
   * // Table.reallyVerbose = true;
   * boolean oDebugMode = Table.debugMode;
   * // Table.debugMode = true;
   * String2.log("\n*** Table.testReadNcSequence");
   * Table table = new Table();
   * String fiName = "/data/andy/pilot_20210818202736_IUPA50_EGRR_181930.bufr";
   * //String2.unitTestDataDir + "";
   * String2.log(NcHelper.ncdump(fiName, ""));
   * String results, expectedStart, expectedEnd;
   *
   * //** don't specify varNames or dimNames -- it find vars with most dims
   * table.readNcSequence(fiName, new StringArray(), new StringArray(),
   * true, 0, //readMetadata, standardizeWhat=0
   * null, null, null); //conVars, conOps, conVals
   * results = table.dataToString(3);
   * expectedStart =
   * //static vars and vars like char SCIENTIFIC_CALIB_COEFFICIENT(N_PROF=254,
   * N_CALIB=1, N_PARAM=3, STRING256=256);
   * "DATA_TYPE,FORMAT_VERSION,HANDBOOK_VERSION,REFERENCE_DATE_TIME,DATE_CREATION,DATE_UPDATE,PLATFORM_NUMBER,PROJECT_NAME,PI_NAME,STATION_PARAMETERS,CYCLE_NUMBER,DIRECTION,DATA_CENTRE,DC_REFERENCE,DATA_STATE_INDICATOR,DATA_MODE,PLATFORM_TYPE,FLOAT_SERIAL_NO,FIRMWARE_VERSION,WMO_INST_TYPE,JULD,JULD_QC,JULD_LOCATION,LATITUDE,LONGITUDE,POSITION_QC,POSITIONING_SYSTEM,PROFILE_PRES_QC,PROFILE_TEMP_QC,PROFILE_PSAL_QC,VERTICAL_SAMPLING_SCHEME,CONFIG_MISSION_NUMBER,PARAMETER,SCIENTIFIC_CALIB_EQUATION,SCIENTIFIC_CALIB_COEFFICIENT,SCIENTIFIC_CALIB_COMMENT,SCIENTIFIC_CALIB_DATE\n"
   * +
   * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PRES,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PRES,PRES_ADJUSTED = PRES - dP,dP =  0.1 dbar.,Pressures adjusted by using pressure offset at the sea surface. The quoted error is manufacturer specified accuracy in dbar.,20110628060155\n"
   * +
   * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,TEMP,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,TEMP,none,none,The quoted error is manufacturer specified accuracy with respect to ITS-90 at time of laboratory calibration.,20110628060155\n"
   * +
   * "Argo profile,3.1,1.2,19500101000000,20090422121913,20160415204722,2901175,CHINA ARGO PROJECT,JIANPING XU,PSAL,1,A,HZ,0066_80617_001,2C,D,,APEX_SBE_4136,,846,21660.34238425926,1,21660.345046296297,21.513999938964844,123.36499786376953,1,ARGOS,A,A,A,,1,PSAL,\"PSAL_ADJUSTED = sw_salt( sw_cndr(PSAL,TEMP,PRES), TEMP, PRES_ADJUSTED ); PSAL_ADJ corrects conductivity cell therm mass (CTM), Johnson et al, 2007, JAOT;\",\"same as for PRES_ADJUSTED; CTL: alpha=0.0267, tau=18.6;\",No significant salinity drift detected; SBE sensor accuracy,20110628060155\n"
   * +
   * "...\n";
   * Test.ensureEqual(results, expectedStart, "results=\n" + results);
   * Test.ensureEqual(table.nRows(), 762, "nRows"); //254*3
   *
   * //* same but quick reject based on constraint
   * table.readMultidimNc(fiName, new StringArray(), new StringArray(), null,
   * true, 0, false, //readMetadata, standardizeWhat=0, removeMVRows
   * StringArray.fromCSV("FORMAT_VERSION,FORMAT_VERSION"), //conVars
   * StringArray.fromCSV("=,="), //conOps
   * StringArray.fromCSV("3.1,3.2")); //conVals
   * Test.ensureEqual(table.nRows(), 0, "nRows");
   *
   * //done
   * // Table.debugMode = oDebugMode;
   * }
   */

  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testReadInvalidCRA() throws Exception {
    // String2.log("\n*** Table.testReadInvalidCRA()");
    StringArray colNames, conNames, conOps, conVals;
    Table table = new Table();
    table.debugMode = true;
    String dir = TableTests.class.getResource("/veryLarge/nccf/wod/").getPath();
    String drbDir = TableTests.class.getResource("/largeFiles/nccf/wod/").getPath();
    String fullName, results, expected;
    // String doAllString = String2.getStringFromSystemIn(
    // "Do all the tests, including ones that are slow and take lots of memory
    // (y/n)?");
    boolean doAll = true; // doAllString.startsWith("y");
    /* */
    // read all
    fullName = dir + "wod_apb_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 19223385 ;\n"
              + "\tcountry_strlen = 13 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 15 ;\n"
              + "\tProject_strlen = 60 ;\n"
              + "\tPlatform_strlen = 19 ;\n"
              + "\tdataset_strlen = 14 ;\n"
              + "\tOcean_Vehicle_strlen = 13 ;\n"
              + "\tTemperature_Instrument_strlen = 73 ;\n"
              + "\tSalinity_Instrument_strlen = 73 ;\n"
              + "\tPrimary_Investigator_strlen = 20 ;\n"
              + "\tPrimary_Investigator_VAR_strlen = 13 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Project(row, Project_strlen) ;\n"
              + "\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n"
              + "\t\tProject:long_name = \"Project_name\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar Ocean_Vehicle(row, Ocean_Vehicle_strlen) ;\n"
              + "\t\tOcean_Vehicle:comment = \"Ocean_vehicle\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tint Temperature_uncalibrated(row) ;\n"
              + "\t\tTemperature_uncalibrated:_FillValue = -99999 ;\n"
              + "\t\tTemperature_uncalibrated:comment = \"set if measurements have not been calibrated\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Salinity_Instrument(row, Salinity_Instrument_strlen) ;\n"
              + "\t\tSalinity_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tSalinity_Instrument:long_name = \"Instrument\" ;\n"
              + "\tchar Primary_Investigator(row, Primary_Investigator_strlen) ;\n"
              + "\tchar Primary_Investigator_VAR(row, Primary_Investigator_VAR_strlen) ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Pressure(row) ;\n"
              + "\t\tPressure:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure:ancillary_variables = \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag\" ;\n"
              + "\t\tPressure:coordinates = \"time lat lon z\" ;\n"
              + "\t\tPressure:grid_mapping = \"crs\" ;\n"
              + "\t\tPressure:long_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:standard_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:units = \"dbar\" ;\n"
              + "\tbyte Pressure_sigfigs(row) ;\n"
              + "\t\tPressure_sigfigs:long_name = \"sea_water_pressure significant_figures\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Project, Platform, Orig_Stat_Num, dataset, Ocean_Vehicle, Temperature_WODprofileflag, Temperature_Instrument, Temperature_uncalibrated, Salinity_WODprofileflag, Salinity_Instrument, Primary_Investigator, Primary_Investigator_VAR\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-24\" ;\n"
              + "\t\t:date_modified = \"2018-03-24\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 66.6276f ;\n"
              + "\t\t:geospatial_lat_min = -71.2137f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = 8.102386f ;\n"
              + "\t\t:geospatial_lon_min = 55.2659f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 1975.4133f ;\n"
              + "\t\t:geospatial_vertical_min = 0.0f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_apb_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Orig_Stat_Num,dataset,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
              + "UNITED STATES,US033376,2004017,14009590,42.875145,-139.0886,85832.01060199086,20050101,0.2544478,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,12888,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,0.0,0,1,12.3,3,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033376,2004017,14009590,42.875145,-139.0886,85832.01060199086,20050101,0.2544478,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,12888,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,1.0,0,2,12.35,4,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033376,2004017,14009590,42.875145,-139.0886,85832.01060199086,20050101,0.2544478,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,12888,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,8.5,0,2,12.35,4,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033376,2004017,14009590,42.875145,-139.0886,85832.01060199086,20050101,0.2544478,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,12888,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,22.5,0,3,12.35,4,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033376,2004017,14009590,42.875145,-139.0886,85832.01060199086,20050101,0.2544478,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,12888,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,36.5,0,3,12.35,4,0,-1.0E10,,-1.0E10,,\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 10000000);
      results = table.dataToString(5);
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Orig_Stat_Num,dataset,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
              + "UNITED STATES,US033495,2005057,14148754,48.738785,-133.2403,86025.06550899148,20050713,1.5722158,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,2738,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,55.0,0,3,8.2,2,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033495,2005057,14148754,48.738785,-133.2403,86025.06550899148,20050713,1.5722158,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,2738,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,66.0,0,3,8.1,2,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033495,2005057,14148754,48.738785,-133.2403,86025.06550899148,20050713,1.5722158,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,2738,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,77.5,0,3,7.75,3,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033495,2005057,14148754,48.738785,-133.2403,86025.06550899148,20050713,1.5722158,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,2738,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,89.5,0,3,7.6,2,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033495,2005057,14148754,48.738785,-133.2403,86025.06550899148,20050713,1.5722158,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,2738,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,101.0,0,4,7.35,3,0,-1.0E10,,-1.0E10,,\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 9223380);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Orig_Stat_Num,dataset,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
              + "UNITED STATES,US033460,2005059,14331034,31.686825,-120.0401,86196.99254602194,20051231,23.821104,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,13046,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,550.0,0,4,5.95,3,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033460,2005059,14331034,31.686825,-120.0401,86196.99254602194,20051231,23.821104,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,13046,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,557.5,0,4,5.85,3,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033460,2005059,14331034,31.686825,-120.0401,86196.99254602194,20051231,23.821104,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,13046,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,564.0,0,4,5.85,3,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033460,2005059,14331034,31.686825,-120.0401,86196.99254602194,20051231,23.821104,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,13046,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,570.5,0,4,5.8,2,0,-1.0E10,,-1.0E10,,\n"
              + "UNITED STATES,US033460,2005059,14331034,31.686825,-120.0401,86196.99254602194,20051231,23.821104,77805,TAGGING OF PACIFIC PREDATORS (TOPP),,13046,animal mounted,Elephant Seal,0,ANIMAL MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR. DANIEL P.,all_variables,579.0,0,4,5.8,2,0,-1.0E10,,-1.0E10,,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // *** just read outer col
    table.readInvalidCRA(
        fullName,
        new StringArray(new String[] {"wod_unique_cast", "time"}),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time\n"
            + "14009590,85832.01060199086\n"
            + "14009591,85832.03254599497\n"
            + "14009592,85832.01828699\n"
            + "14009593,85832.0382870026\n"
            + "14009594,85832.01134299766\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 346231, "");

    table.removeRows(0, 346226);
    results = table.dataToString();
    expected =
        "wod_unique_cast,time\n"
            + "14331030,86196.96106499434\n"
            + "14331031,86196.97773098946\n"
            + "14331032,86196.99402803183\n"
            + "14331033,86196.97189801931\n"
            + "14331034,86196.99254602194\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // just read outer col with constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(new String[] {"wod_unique_cast", "time"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"14148754"}));
    results = table.dataToString(5);
    // from above
    // "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Orig_Stat_Num,dataset,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
    // +
    // "UNITED
    // STATES,US033495,2005057,14148754,48.738785,-133.2403,86025.06550899148,20050713,1.5722158,77805,TAGGING
    // OF PACIFIC PREDATORS (TOPP),,2738,animal mounted,Elephant Seal,0,ANIMAL
    // MOUNTED: TDR (Time-Depth Recorder) Tag (Wildlife Computers),1,0,,COSTA; DR.
    // DANIEL P.,all_variables,55.0,0,3,8.2,2,0,-1.0E10,,-1.0E10,,\n" +
    expected = "wod_unique_cast,time\n" + "14148754,86025.06550899148\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** just read inner col
    table.readInvalidCRA(
        fullName,
        new StringArray(new String[] {"z", "Temperature", "Temperature_sigfigs"}),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    // from above
    // ...z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
    // +
    // ...0.0,0,1,12.3,3,0,-1.0E10,,-1.0E10,,\n" +
    // ...1.0,0,2,12.35,4,0,-1.0E10,,-1.0E10,,\n" +
    // ...8.5,0,2,12.35,4,0,-1.0E10,,-1.0E10,,\n" +
    // ...22.5,0,3,12.35,4,0,-1.0E10,,-1.0E10,,\n" +
    // ...36.5,0,3,12.35,4,0,-1.0E10,,-1.0E10,,\n" +
    expected =
        "z,Temperature,Temperature_sigfigs\n"
            + "0.0,12.3,3\n"
            + "1.0,12.35,4\n"
            + "8.5,12.35,4\n"
            + "22.5,12.35,4\n"
            + "36.5,12.35,4\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 19223385, "");

    table.removeRows(0, 19223380);
    results = table.dataToString();
    // from above
    // ...z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
    // +
    // ...550.0,0,4,5.95,3,0,-1.0E10,,-1.0E10,,\n" +
    // ...557.5,0,4,5.85,3,0,-1.0E10,,-1.0E10,,\n" +
    // ...564.0,0,4,5.85,3,0,-1.0E10,,-1.0E10,,\n" +
    // ...570.5,0,4,5.8,2,0,-1.0E10,,-1.0E10,,\n" +
    // ...579.0,0,4,5.8,2,0,-1.0E10,,-1.0E10,,\n";
    expected =
        "z,Temperature,Temperature_sigfigs\n"
            + "550.0,5.95,3\n"
            + "557.5,5.85,3\n"
            + "564.0,5.85,3\n"
            + "570.5,5.8,2\n"
            + "579.0,5.8,2\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // just read inner col with constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(new String[] {"z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"z", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"570.5", "5.8"}));
    results = table.dataToString(5);
    expected =
        "z,Temperature,Temperature_WODflag\n"
            + "570.5,5.8,0\n"
            + "570.5,5.8,0\n"
            + "570.5,5.8,0\n"
            + "570.5,5.8,0\n"
            + "570.5,5.8,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 22, "");

    // read outer and inner col with outer constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"14148754"}));
    results = table.dataToString(10);
    // from above
    // ...wod_unique_cast,...z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Pressure,Pressure_sigfigs,Salinity,Salinity_sigfigs,Salinity_WODflag\n"
    // +
    // ...14148754,... 55.0,0,3,8.2,2,0,-1.0E10,,-1.0E10,,\n" +
    // ...14148754,... 66.0,0,3,8.1,2,0,-1.0E10,,-1.0E10,,\n" +
    // ...14148754,... 77.5,0,3,7.75,3,0,-1.0E10,,-1.0E10,,\n" +
    // ...14148754,... 89.5,0,3,7.6,2,0,-1.0E10,,-1.0E10,,\n" +
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "14148754,86025.06550899148,0.0,13.85,0\n"
            + "14148754,86025.06550899148,0.5,13.85,0\n"
            + "14148754,86025.06550899148,3.0,13.85,0\n"
            + "14148754,86025.06550899148,15.0,13.55,0\n"
            + "14148754,86025.06550899148,30.0,10.05,0\n"
            + "14148754,86025.06550899148,43.5,8.4,0\n"
            + "14148754,86025.06550899148,55.0,8.2,0\n"
            + "14148754,86025.06550899148,66.0,8.1,0\n"
            + "14148754,86025.06550899148,77.5,7.75,0\n"
            + "14148754,86025.06550899148,89.5,7.6,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 90, "");

    // read outer and inner col with outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"14148754", "13.55"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "14148754,86025.06550899148,15.0,13.55,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** list the non-"" PI's
    table.readInvalidCRA(
        fullName,
        new StringArray(new String[] {"Primary_Investigator"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"Primary_Investigator"}),
        new StringArray(new String[] {"!="}),
        new StringArray(new String[] {""}));
    results = table.dataToString(5);
    expected =
        "Primary_Investigator\n"
            + "COSTA; DR. DANIEL P.\n"
            + "COSTA; DR. DANIEL P.\n"
            + "COSTA; DR. DANIEL P.\n"
            + "COSTA; DR. DANIEL P.\n"
            + "COSTA; DR. DANIEL P.\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 323499, "");

    // *** read all ctd !!! This fails with -Xmx6000m of memory. Succeeds with
    // -Xmx8000m
    fullName = dir + "wod_ctd_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 14843110 ;\n"
              + "\tcountry_strlen = 18 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 15 ;\n"
              + "\toriginators_station_identifier_strlen = 9 ;\n"
              + "\tProject_strlen = 71 ;\n"
              + "\tPlatform_strlen = 80 ;\n"
              + "\tInstitute_strlen = 80 ;\n"
              + "\tCast_Direction_strlen = 4 ;\n"
              + "\tWater_Color_strlen = 54 ;\n"
              + "\tWave_Direction_strlen = 25 ;\n"
              + "\tWave_Height_strlen = 9 ;\n"
              + "\tSea_State_strlen = 42 ;\n"
              + "\tWave_Period_strlen = 17 ;\n"
              + "\tWind_Direction_strlen = 25 ;\n"
              + "\tWeather_Condition_strlen = 169 ;\n"
              + "\tCloud_Type_strlen = 18 ;\n"
              + "\tCloud_Cover_strlen = 65 ;\n"
              + "\tdataset_strlen = 4 ;\n"
              + "\tRecorder_strlen = 47 ;\n"
              + "\tdepth_eq_strlen = 8 ;\n"
              + "\tVisibility_strlen = 24 ;\n"
              + "\tneeds_z_fix_strlen = 16 ;\n"
              + "\treal_time_strlen = 14 ;\n"
              + "\tdbase_orig_strlen = 59 ;\n"
              + "\torigflagset_strlen = 5 ;\n"
              + "\tTemperature_Scale_strlen = 19 ;\n"
              + "\tTemperature_Instrument_strlen = 74 ;\n"
              + "\tSalinity_Scale_strlen = 15 ;\n"
              + "\tSalinity_Instrument_strlen = 74 ;\n"
              + "\tOxygen_Instrument_strlen = 103 ;\n"
              + "\tOxygen_Method_strlen = 0 ;\n"
              + "\tOxygen_Original_units_strlen = 60 ;\n"
              + "\tPressure_Instrument_strlen = 65 ;\n"
              + "\tChlorophyll_Instrument_strlen = 59 ;\n"
              + "\tChlorophyll_Original_units_strlen = 32 ;\n"
              + "\tPrimary_Investigator_strlen = 26 ;\n"
              + "\tPrimary_Investigator_VAR_strlen = 13 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tchar originators_station_identifier(row, originators_station_identifier_strlen) ;\n"
              + "\t\toriginators_station_identifier:long_name = \"originators_station_identifier\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Project(row, Project_strlen) ;\n"
              + "\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n"
              + "\t\tProject:long_name = \"Project_name\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tchar Institute(row, Institute_strlen) ;\n"
              + "\t\tInstitute:comment = \"name of institute which collected data\" ;\n"
              + "\t\tInstitute:long_name = \"Responsible_institute\" ;\n"
              + "\tint Cast_Tow_number(row) ;\n"
              + "\t\tCast_Tow_number:_FillValue = -99999 ;\n"
              + "\t\tCast_Tow_number:comment = \"originator assigned sequential cast or tow_no\" ;\n"
              + "\t\tCast_Tow_number:long_name = \"Cast_or_Tow_number\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tfloat Bottom_Depth(row) ;\n"
              + "\t\tBottom_Depth:_FillValue = -1.0E10f ;\n"
              + "\t\tBottom_Depth:standard_name = \"sea_floor_depth_below_sea_surface\" ;\n"
              + "\t\tBottom_Depth:units = \"meters\" ;\n"
              + "\tfloat Cast_Duration(row) ;\n"
              + "\t\tCast_Duration:_FillValue = -1.0E10f ;\n"
              + "\t\tCast_Duration:long_name = \"Cast_Duration\" ;\n"
              + "\t\tCast_Duration:units = \"hours\" ;\n"
              + "\tchar Cast_Direction(row, Cast_Direction_strlen) ;\n"
              + "\t\tCast_Direction:long_name = \"Cast_Direction\" ;\n"
              + "\tint High_res_pair(row) ;\n"
              + "\t\tHigh_res_pair:_FillValue = -99999 ;\n"
              + "\t\tHigh_res_pair:comment = \"WOD unique cast number for bottle/CTD from same rosette\" ;\n"
              + "\t\tHigh_res_pair:long_name = \"WOD_high_resolution_pair_number\" ;\n"
              + "\tchar Water_Color(row, Water_Color_strlen) ;\n"
              + "\t\tWater_Color:long_name = \"Water_Color\" ;\n"
              + "\t\tWater_Color:units_wod = \"Forel-Ule scale (00 to 21)\" ;\n"
              + "\tfloat Water_Transpar(row) ;\n"
              + "\t\tWater_Transpar:_FillValue = -1.0E10f ;\n"
              + "\t\tWater_Transpar:comment = \"Secchi disk depth\" ;\n"
              + "\t\tWater_Transpar:long_name = \"Water_Transparency\" ;\n"
              + "\t\tWater_Transpar:units = \"meters\" ;\n"
              + "\tchar Wave_Direction(row, Wave_Direction_strlen) ;\n"
              + "\t\tWave_Direction:long_name = \"Wave_Direction\" ;\n"
              + "\t\tWave_Direction:units_wod = \"WMO 0877 or NODC 0110\" ;\n"
              + "\tchar Wave_Height(row, Wave_Height_strlen) ;\n"
              + "\t\tWave_Height:long_name = \"Wave_Height\" ;\n"
              + "\t\tWave_Height:units_wod = \"WMO 1555 or NODC 0104\" ;\n"
              + "\tchar Sea_State(row, Sea_State_strlen) ;\n"
              + "\t\tSea_State:long_name = \"Sea_State\" ;\n"
              + "\t\tSea_State:units_wod = \"WMO 3700 or NODC 0109\" ;\n"
              + "\tchar Wave_Period(row, Wave_Period_strlen) ;\n"
              + "\t\tWave_Period:long_name = \"Wave_Period\" ;\n"
              + "\t\tWave_Period:units_wod = \"WMO 3155 or NODC 0378\" ;\n"
              + "\tchar Wind_Direction(row, Wind_Direction_strlen) ;\n"
              + "\t\tWind_Direction:long_name = \"Wind_Direction\" ;\n"
              + "\t\tWind_Direction:units_wod = \"WMO 0877 or NODC 0110\" ;\n"
              + "\tfloat Wind_Speed(row) ;\n"
              + "\t\tWind_Speed:_FillValue = -1.0E10f ;\n"
              + "\t\tWind_Speed:standard_name = \"wind_speed\" ;\n"
              + "\t\tWind_Speed:units = \"knots\" ;\n"
              + "\tfloat Barometric_Pres(row) ;\n"
              + "\t\tBarometric_Pres:_FillValue = -1.0E10f ;\n"
              + "\t\tBarometric_Pres:long_name = \"Barometric_Pressure\" ;\n"
              + "\t\tBarometric_Pres:units = \"millibars\" ;\n"
              + "\tfloat Dry_Bulb_Temp(row) ;\n"
              + "\t\tDry_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tDry_Bulb_Temp:long_name = \"Dry_Bulb_Air_Temperature\" ;\n"
              + "\t\tDry_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tfloat Wet_Bulb_Temp(row) ;\n"
              + "\t\tWet_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tWet_Bulb_Temp:long_name = \"Wet_Bulb_Air_Temperature\" ;\n"
              + "\t\tWet_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tchar Weather_Condition(row, Weather_Condition_strlen) ;\n"
              + "\t\tWeather_Condition:comment = \"Weather conditions at time of measurements\" ;\n"
              + "\t\tWeather_Condition:long_name = \"Weather_Condition\" ;\n"
              + "\tchar Cloud_Type(row, Cloud_Type_strlen) ;\n"
              + "\t\tCloud_Type:long_name = \"Cloud_Type\" ;\n"
              + "\t\tCloud_Type:units_wod = \"WMO 0500 or NODC 0053\" ;\n"
              + "\tchar Cloud_Cover(row, Cloud_Cover_strlen) ;\n"
              + "\t\tCloud_Cover:long_name = \"Cloud_Cover\" ;\n"
              + "\t\tCloud_Cover:units_wod = \"WMO 2700 or NODC 0105\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar Recorder(row, Recorder_strlen) ;\n"
              + "\t\tRecorder:comment = \"Device which recorded measurements\" ;\n"
              + "\t\tRecorder:long_name = \"Recorder\" ;\n"
              + "\t\tRecorder:units_wod = \"WMO code 4770\" ;\n"
              + "\tchar depth_eq(row, depth_eq_strlen) ;\n"
              + "\t\tdepth_eq:comment = \"which drop rate equation was used\" ;\n"
              + "\t\tdepth_eq:long_name = \"depth_equation_used\" ;\n"
              + "\tbyte Bottom_Hit(row) ;\n"
              + "\t\tBottom_Hit:_FillValue = -9 ;\n"
              + "\t\tBottom_Hit:comment = \"set to one if instrument hit bottom\" ;\n"
              + "\t\tBottom_Hit:long_name = \"Bottom_Hit\" ;\n"
              + "\tchar Visibility(row, Visibility_strlen) ;\n"
              + "\t\tVisibility:long_name = \"Horizontal_visibility\" ;\n"
              + "\t\tVisibility:units_wod = \"WMO Code 4300\" ;\n"
              + "\tfloat Ref_Surf_Temp(row) ;\n"
              + "\t\tRef_Surf_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tRef_Surf_Temp:comment = \"Reference_or_Sea_Surface_Temperature\" ;\n"
              + "\t\tRef_Surf_Temp:units = \"degree_C\" ;\n"
              + "\tchar needs_z_fix(row, needs_z_fix_strlen) ;\n"
              + "\t\tneeds_z_fix:comment = \"instruction for fixing depths\" ;\n"
              + "\t\tneeds_z_fix:long_name = \"z_fix_instructions\" ;\n"
              + "\t\tneeds_z_fix:units_wod = \"WOD_code\" ;\n"
              + "\tchar real_time(row, real_time_strlen) ;\n"
              + "\t\treal_time:comment = \"timeliness and quality status\" ;\n"
              + "\t\treal_time:long_name = \"real_time_data\" ;\n"
              + "\tchar dbase_orig(row, dbase_orig_strlen) ;\n"
              + "\t\tdbase_orig:comment = \"Database from which data were extracted\" ;\n"
              + "\t\tdbase_orig:long_name = \"database_origin\" ;\n"
              + "\tchar origflagset(row, origflagset_strlen) ;\n"
              + "\t\torigflagset:comment = \"set of originators flag codes to use\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Scale(row, Temperature_Scale_strlen) ;\n"
              + "\t\tTemperature_Scale:long_name = \"Scale upon which values were measured\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tint Temperature_uncalibrated(row) ;\n"
              + "\t\tTemperature_uncalibrated:_FillValue = -99999 ;\n"
              + "\t\tTemperature_uncalibrated:comment = \"set if measurements have not been calibrated\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Salinity_Scale(row, Salinity_Scale_strlen) ;\n"
              + "\t\tSalinity_Scale:long_name = \"Scale upon which values were measured\" ;\n"
              + "\tchar Salinity_Instrument(row, Salinity_Instrument_strlen) ;\n"
              + "\t\tSalinity_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tSalinity_Instrument:long_name = \"Instrument\" ;\n"
              + "\tint Salinity_uncalibrated(row) ;\n"
              + "\t\tSalinity_uncalibrated:_FillValue = -99999 ;\n"
              + "\t\tSalinity_uncalibrated:comment = \"set if measurements have not been calibrated\" ;\n"
              + "\tbyte Oxygen_WODprofileflag(row) ;\n"
              + "\t\tOxygen_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tOxygen_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tOxygen_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Oxygen_Instrument(row, Oxygen_Instrument_strlen) ;\n"
              + "\t\tOxygen_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tOxygen_Instrument:long_name = \"Instrument\" ;\n"
              + "\tchar Oxygen_Method(row, Oxygen_Method_strlen) ;\n"
              + "\t\tOxygen_Method:comment = \"Method\" ;\n"
              + "\tchar Oxygen_Original_units(row, Oxygen_Original_units_strlen) ;\n"
              + "\t\tOxygen_Original_units:comment = \"Units originally used: coverted to standard units\" ;\n"
              + "\tint Oxygen_uncalibrated(row) ;\n"
              + "\t\tOxygen_uncalibrated:_FillValue = -99999 ;\n"
              + "\t\tOxygen_uncalibrated:comment = \"set if measurements have not been calibrated\" ;\n"
              + "\tchar Pressure_Instrument(row, Pressure_Instrument_strlen) ;\n"
              + "\t\tPressure_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tPressure_Instrument:long_name = \"Instrument\" ;\n"
              + "\tbyte Chlorophyll_WODprofileflag(row) ;\n"
              + "\t\tChlorophyll_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tChlorophyll_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tChlorophyll_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Chlorophyll_Instrument(row, Chlorophyll_Instrument_strlen) ;\n"
              + "\t\tChlorophyll_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tChlorophyll_Instrument:long_name = \"Instrument\" ;\n"
              + "\tchar Chlorophyll_Original_units(row, Chlorophyll_Original_units_strlen) ;\n"
              + "\t\tChlorophyll_Original_units:comment = \"Units originally used: coverted to standard units\" ;\n"
              + "\tint Chlorophyll_uncalibrated(row) ;\n"
              + "\t\tChlorophyll_uncalibrated:_FillValue = -99999 ;\n"
              + "\t\tChlorophyll_uncalibrated:comment = \"set if measurements have not been calibrated\" ;\n"
              + "\tchar Primary_Investigator(row, Primary_Investigator_strlen) ;\n"
              + "\tchar Primary_Investigator_VAR(row, Primary_Investigator_VAR_strlen) ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag z_origflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_origflag(row) ;\n"
              + "\t\tz_origflag:_FillValue = -9 ;\n"
              + "\t\tz_origflag:comment = \"Originator flags are dependent on origflagset\" ;\n"
              + "\t\tz_origflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag Temperature_origflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tbyte Temperature_origflag(row) ;\n"
              + "\t\tTemperature_origflag:_FillValue = -9 ;\n"
              + "\t\tTemperature_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tTemperature_origflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag Salinity_origflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tbyte Salinity_origflag(row) ;\n"
              + "\t\tSalinity_origflag:_FillValue = -9 ;\n"
              + "\t\tSalinity_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tSalinity_origflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tfloat Oxygen(row) ;\n"
              + "\t\tOxygen:_FillValue = -1.0E10f ;\n"
              + "\t\tOxygen:ancillary_variables = \"Oxygen_sigfigs Oxygen_WODflag Oxygen_WODprofileflag Oxygen_origflag\" ;\n"
              + "\t\tOxygen:coordinates = \"time lat lon z\" ;\n"
              + "\t\tOxygen:grid_mapping = \"crs\" ;\n"
              + "\t\tOxygen:long_name = \"volume_fraction_of_oxygen_in_sea_water\" ;\n"
              + "\t\tOxygen:standard_name = \"volume_fraction_of_oxygen_in_sea_water\" ;\n"
              + "\t\tOxygen:units = \"ml/l\" ;\n"
              + "\tbyte Oxygen_sigfigs(row) ;\n"
              + "\t\tOxygen_sigfigs:long_name = \"volume_fraction_of_oxygen_in_sea_water significant_figures\" ;\n"
              + "\tbyte Oxygen_WODflag(row) ;\n"
              + "\t\tOxygen_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tOxygen_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tOxygen_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tOxygen_WODflag:standard_name = \"volume_fraction_of_oxygen_in_sea_water status_flag\" ;\n"
              + "\tbyte Oxygen_origflag(row) ;\n"
              + "\t\tOxygen_origflag:_FillValue = -9 ;\n"
              + "\t\tOxygen_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tOxygen_origflag:standard_name = \"volume_fraction_of_oxygen_in_sea_water status_flag\" ;\n"
              + "\tfloat Pressure(row) ;\n"
              + "\t\tPressure:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure:ancillary_variables = \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag Pressure_origflag\" ;\n"
              + "\t\tPressure:coordinates = \"time lat lon z\" ;\n"
              + "\t\tPressure:grid_mapping = \"crs\" ;\n"
              + "\t\tPressure:long_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:standard_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:units = \"dbar\" ;\n"
              + "\tbyte Pressure_sigfigs(row) ;\n"
              + "\t\tPressure_sigfigs:long_name = \"sea_water_pressure significant_figures\" ;\n"
              + "\tbyte Pressure_origflag(row) ;\n"
              + "\t\tPressure_origflag:_FillValue = -9 ;\n"
              + "\t\tPressure_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tPressure_origflag:standard_name = \"sea_water_pressure status_flag\" ;\n"
              + "\tfloat Chlorophyll(row) ;\n"
              + "\t\tChlorophyll:_FillValue = -1.0E10f ;\n"
              + "\t\tChlorophyll:ancillary_variables = \"Chlorophyll_sigfigs Chlorophyll_WODflag Chlorophyll_WODprofileflag Chlorophyll_origflag\" ;\n"
              + "\t\tChlorophyll:coordinates = \"time lat lon z\" ;\n"
              + "\t\tChlorophyll:grid_mapping = \"crs\" ;\n"
              + "\t\tChlorophyll:long_name = \"mass_concentration_of_chlorophyll_in_sea_water\" ;\n"
              + "\t\tChlorophyll:standard_name = \"mass_concentration_of_chlorophyll_in_sea_water\" ;\n"
              + "\t\tChlorophyll:units = \"ugram/l\" ;\n"
              + "\tbyte Chlorophyll_sigfigs(row) ;\n"
              + "\t\tChlorophyll_sigfigs:long_name = \"mass_concentration_of_chlorophyll_in_sea_water significant_figures\" ;\n"
              + "\tbyte Chlorophyll_WODflag(row) ;\n"
              + "\t\tChlorophyll_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tChlorophyll_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tChlorophyll_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tChlorophyll_WODflag:standard_name = \"mass_concentration_of_chlorophyll_in_sea_water status_flag\" ;\n"
              + "\tbyte Chlorophyll_origflag(row) ;\n"
              + "\t\tChlorophyll_origflag:_FillValue = -9 ;\n"
              + "\t\tChlorophyll_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tChlorophyll_origflag:standard_name = \"mass_concentration_of_chlorophyll_in_sea_water status_flag\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, originators_station_identifier, latitude, longitude, time, date, GMT_time, Access_no, Project, Platform, Institute, Cast_Tow_number, Orig_Stat_Num, Bottom_Depth, Cast_Duration, Cast_Direction, High_res_pair, Water_Color, Water_Transpar, Wave_Direction, Wave_Height, Sea_State, Wave_Period, Wind_Direction, Wind_Speed, Barometric_Pres, Dry_Bulb_Temp, Wet_Bulb_Temp, Weather_Condition, Cloud_Type, Cloud_Cover, dataset, Recorder, depth_eq, Bottom_Hit, Visibility, Ref_Surf_Temp, needs_z_fix, real_time, dbase_orig, origflagset, Temperature_WODprofileflag, Temperature_Scale, Temperature_Instrument, Temperature_uncalibrated, Salinity_WODprofileflag, Salinity_Scale, Salinity_Instrument, Salinity_uncalibrated, Oxygen_WODprofileflag, Oxygen_Instrument, Oxygen_Method, Oxygen_Original_units, Oxygen_uncalibrated, Pressure_Instrument, Chlorophyll_WODprofileflag, Chlorophyll_Instrument, Chlorophyll_Original_units, Chlorophyll_uncalibrated, Primary_Investigator, Primary_Investigator_VAR\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-24\" ;\n"
              + "\t\t:date_modified = \"2018-03-24\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 89.986336f ;\n"
              + "\t\t:geospatial_lat_min = -78.579f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = 180.0f ;\n"
              + "\t\t:geospatial_lon_min = -180.0f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 6473.251f ;\n"
              + "\t\t:geospatial_vertical_min = 0.0f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_ctd_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,originators_station_identifier,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Cast_Tow_number,Orig_Stat_Num,Bottom_Depth,Cast_Duration,Cast_Direction,High_res_pair,Water_Color,Water_Transpar,Wave_Direction,Wave_Height,Sea_State,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Type,Cloud_Cover,dataset,Recorder,depth_eq,Bottom_Hit,Visibility,Ref_Surf_Temp,needs_z_fix,real_time,dbase_orig,origflagset,Temperature_WODprofileflag,Temperature_Scale,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Scale,Salinity_Instrument,Salinity_uncalibrated,Oxygen_WODprofileflag,Oxygen_Instrument,Oxygen_Method,Oxygen_Original_units,Oxygen_uncalibrated,Pressure_Instrument,Chlorophyll_WODprofileflag,Chlorophyll_Instrument,Chlorophyll_Original_units,Chlorophyll_uncalibrated,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Oxygen,Oxygen_sigfigs,Oxygen_WODflag,Oxygen_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Chlorophyll,Chlorophyll_sigfigs,Chlorophyll_WODflag,Chlorophyll_origflag\n"
              + "NEW ZEALAND,NZ001156,tan0501,15076949,,-43.427,177.9535,85832.04166666791,20050101,1.0,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,24,332.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,0.0,0,-9,1,12.792,5,0,-9,34.702,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001156,tan0501,15076949,,-43.427,177.9535,85832.04166666791,20050101,1.0,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,24,332.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,1.0,0,-9,1,13.085,5,0,-9,34.573,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001156,tan0501,15076949,,-43.427,177.9535,85832.04166666791,20050101,1.0,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,24,332.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,2.0,0,-9,1,12.729,5,0,-9,34.562,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001156,tan0501,15076949,,-43.427,177.9535,85832.04166666791,20050101,1.0,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,24,332.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,3.0,0,-9,1,12.778,5,0,-9,34.585,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001156,tan0501,15076949,,-43.427,177.9535,85832.04166666791,20050101,1.0,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,24,332.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,7.0,0,-9,1,12.538,5,0,-9,34.629,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 14843110 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,originators_station_identifier,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Cast_Tow_number,Orig_Stat_Num,Bottom_Depth,Cast_Duration,Cast_Direction,High_res_pair,Water_Color,Water_Transpar,Wave_Direction,Wave_Height,Sea_State,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Type,Cloud_Cover,dataset,Recorder,depth_eq,Bottom_Hit,Visibility,Ref_Surf_Temp,needs_z_fix,real_time,dbase_orig,origflagset,Temperature_WODprofileflag,Temperature_Scale,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Scale,Salinity_Instrument,Salinity_uncalibrated,Oxygen_WODprofileflag,Oxygen_Instrument,Oxygen_Method,Oxygen_Original_units,Oxygen_uncalibrated,Pressure_Instrument,Chlorophyll_WODprofileflag,Chlorophyll_Instrument,Chlorophyll_Original_units,Chlorophyll_uncalibrated,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Oxygen,Oxygen_sigfigs,Oxygen_WODflag,Oxygen_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Chlorophyll,Chlorophyll_sigfigs,Chlorophyll_WODflag,Chlorophyll_origflag\n"
              + "NEW ZEALAND,NZ001163,tan0601,15077333,,-43.9471,-179.09016,86196.95902776718,20051231,23.016666,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,25,382.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,374.0,0,-9,3,8.695,4,0,-9,34.571,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001163,tan0601,15077333,,-43.9471,-179.09016,86196.95902776718,20051231,23.016666,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,25,382.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,375.0,0,-9,3,8.692,4,0,-9,34.571,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001163,tan0601,15077333,,-43.9471,-179.09016,86196.95902776718,20051231,23.016666,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,25,382.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,376.0,0,-9,3,8.686,4,0,-9,34.571,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001163,tan0601,15077333,,-43.9471,-179.09016,86196.95902776718,20051231,23.016666,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,25,382.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,377.0,0,-9,3,8.682,4,0,-9,34.57,4,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
              + "NEW ZEALAND,NZ001163,tan0601,15077333,,-43.9471,-179.09016,86196.95902776718,20051231,23.016666,74288,,TANGAROA (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER & ATMOSPHERIC RESEARCH LTD (NIWA); AUCKLAND,-99999,25,382.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,378.0,0,-9,3,8.675,4,0,-9,34.57,4,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    // from above
    // "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,originators_station_identifier,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Cast_Tow_number,Orig_Stat_Num,Bottom_Depth,Cast_Duration,Cast_Direction,High_res_pair,Water_Color,Water_Transpar,Wave_Direction,Wave_Height,Sea_State,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Type,Cloud_Cover,dataset,Recorder,depth_eq,Bottom_Hit,Visibility,Ref_Surf_Temp,needs_z_fix,real_time,dbase_orig,origflagset,Temperature_WODprofileflag,Temperature_Scale,Temperature_Instrument,Temperature_uncalibrated,Salinity_WODprofileflag,Salinity_Scale,Salinity_Instrument,Salinity_uncalibrated,Oxygen_WODprofileflag,Oxygen_Instrument,Oxygen_Method,Oxygen_Original_units,Oxygen_uncalibrated,Pressure_Instrument,Chlorophyll_WODprofileflag,Chlorophyll_Instrument,Chlorophyll_Original_units,Chlorophyll_uncalibrated,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Oxygen,Oxygen_sigfigs,Oxygen_WODflag,Oxygen_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Chlorophyll,Chlorophyll_sigfigs,Chlorophyll_WODflag,Chlorophyll_origflag\n"
    // +
    // "NEW
    // ZEALAND,NZ001163,tan0601,15077333,,-43.9471,-179.09016,86196.95902776718,20051231,23.016666,74288,,TANGAROA
    // (R/V; call sign ZMFR; built 1991; IMO9011571),NATIONAL INSTITUTE OF WATER &
    // ATMOSPHERIC RESEARCH LTD (NIWA);
    // AUCKLAND,-99999,25,382.0,-1.0E10,,-99999,,-1.0E10,,,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,CTD,,,-9,,-1.0E10,,,GODAR
    // Project,,0,,,-99999,0,,,-99999,0,,,,-99999,,0,,,-99999,,,374.0,0,-9,3,8.695,4,0,-9,34.571,5,0,-9,-1.0E10,,,-9,-1.0E10,,-9,-1.0E10,,,-9\n"
    // +
    fullName = dir + "wod_ctd_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"15077333"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "15077333,86196.95902776718,0.0,15.537,0\n"
            + "15077333,86196.95902776718,1.0,15.544,0\n"
            + "15077333,86196.95902776718,2.0,15.533,0\n"
            + "15077333,86196.95902776718,3.0,15.516,0\n"
            + "15077333,86196.95902776718,5.0,15.501,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"15077333", "15.533"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "15077333,86196.95902776718,2.0,15.533,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all drb
    fullName = drbDir + "wod_drb_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 846878 ;\n"
              + "\tcountry_strlen = 13 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 5 ;\n"
              + "\tPlatform_strlen = 17 ;\n"
              + "\tInstitute_strlen = 62 ;\n"
              + "\tdataset_strlen = 13 ;\n"
              + "\tOcean_Vehicle_strlen = 38 ;\n"
              + "\tTemperature_Instrument_strlen = 50 ;\n"
              + "\tSalinity_Instrument_strlen = 50 ;\n"
              + "\tPrimary_Investigator_strlen = 14 ;\n"
              + "\tPrimary_Investigator_VAR_strlen = 13 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tchar Institute(row, Institute_strlen) ;\n"
              + "\t\tInstitute:comment = \"name of institute which collected data\" ;\n"
              + "\t\tInstitute:long_name = \"Responsible_institute\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar Ocean_Vehicle(row, Ocean_Vehicle_strlen) ;\n"
              + "\t\tOcean_Vehicle:comment = \"Ocean_vehicle\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Salinity_Instrument(row, Salinity_Instrument_strlen) ;\n"
              + "\t\tSalinity_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tSalinity_Instrument:long_name = \"Instrument\" ;\n"
              + "\tchar Primary_Investigator(row, Primary_Investigator_strlen) ;\n"
              + "\tchar Primary_Investigator_VAR(row, Primary_Investigator_VAR_strlen) ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tfloat Pressure(row) ;\n"
              + "\t\tPressure:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure:ancillary_variables = \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag\" ;\n"
              + "\t\tPressure:coordinates = \"time lat lon z\" ;\n"
              + "\t\tPressure:grid_mapping = \"crs\" ;\n"
              + "\t\tPressure:long_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:standard_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:units = \"dbar\" ;\n"
              + "\tbyte Pressure_sigfigs(row) ;\n"
              + "\t\tPressure_sigfigs:long_name = \"sea_water_pressure significant_figures\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Platform, Institute, Orig_Stat_Num, dataset, Ocean_Vehicle, Temperature_WODprofileflag, Temperature_Instrument, Salinity_WODprofileflag, Salinity_Instrument, Primary_Investigator, Primary_Investigator_VAR\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-25\" ;\n"
              + "\t\t:date_modified = \"2018-03-25\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 89.625f ;\n"
              + "\t\t:geospatial_lat_min = 76.025f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = 180.0f ;\n"
              + "\t\t:geospatial_lon_min = -180.0f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 780.2189f ;\n"
              + "\t\t:geospatial_vertical_min = 8.113341f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_drb_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Platform,Institute,Orig_Stat_Num,dataset,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Pressure,Pressure_sigfigs\n"
              + "JAPAN,JP033439,,10899854,88.1995,-15.9725,85832.0,20050101,0.0,14672,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),3006,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,25.348,0,6,-1.738,5,0,31.7095,6,0,-1.0E10,\n"
              + "JAPAN,JP033439,,10899854,88.1995,-15.9725,85832.0,20050101,0.0,14672,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),3006,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,50.694,0,6,-1.738,5,0,31.7078,6,0,-1.0E10,\n"
              + "JAPAN,JP033439,,10899854,88.1995,-15.9725,85832.0,20050101,0.0,14672,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),3006,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,81.104,0,6,-1.68,5,0,33.4528,6,0,-1.0E10,\n"
              + "JAPAN,JP033439,,10899854,88.1995,-15.9725,85832.0,20050101,0.0,14672,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),3006,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,121.808,0,7,-1.286,5,0,34.1678,6,0,-1.0E10,\n"
              + "JAPAN,JP033439,,10899854,88.1995,-15.9725,85832.0,20050101,0.0,14672,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),3006,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,203.193,0,7,0.432,4,0,34.7061,6,0,-1.0E10,\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 846878 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Platform,Institute,Orig_Stat_Num,dataset,Ocean_Vehicle,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Pressure,Pressure_sigfigs\n"
              + "JAPAN,JP033440,,10909132,76.025,-11.6514,86196.95833331347,20051231,23.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504843054,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,106.845,0,7,-1.474,5,0,33.5642,6,0,-1.0E10,\n"
              + "JAPAN,JP033440,,10909132,76.025,-11.6514,86196.95833331347,20051231,23.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504843054,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,148.032,0,7,-0.74,4,0,34.2258,6,0,-1.0E10,\n"
              + "JAPAN,JP033440,,10909132,76.025,-11.6514,86196.95833331347,20051231,23.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504843054,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,194.357,0,7,-0.436,4,0,31.0866,6,0,-1.0E10,\n"
              + "JAPAN,JP033441,,10909133,87.4024,-112.5225,86196.95833331347,20051231,23.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504846154,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,50.447,0,6,-1.6169,5,0,30.988,6,0,-1.0E10,\n"
              + "JAPAN,JP033441,,10909133,87.4024,-112.5225,86196.95833331347,20051231,23.0,-504834352,,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),-504846154,drifting buoy,J-CAD (JAMSTEC Compact Arctic Drifter),0,,0,,,,121.051,0,7,-1.5004,5,0,34.0442,6,0,-1.0E10,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    fullName = drbDir + "wod_drb_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"10909132"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10909132,86196.95833331347,22.264,-1.818,0\n"
            + "10909132,86196.95833331347,44.526,-1.816,0\n"
            + "10909132,86196.95833331347,71.236,-1.798,0\n"
            + "10909132,86196.95833331347,106.845,-1.474,0\n"
            + "10909132,86196.95833331347,148.032,-0.74,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"10909132", "-1.798"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10909132,86196.95833331347,71.236,-1.798,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all gld
    fullName = dir + "wod_gld_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 3398337 ;\n"
              + "\tcountry_strlen = 13 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 15 ;\n"
              + "\tPlatform_strlen = 80 ;\n"
              + "\tInstitute_strlen = 79 ;\n"
              + "\tCast_Direction_strlen = 2 ;\n"
              + "\tdataset_strlen = 6 ;\n"
              + "\tOcean_Vehicle_strlen = 9 ;\n"
              + "\torigflagset_strlen = 5 ;\n"
              + "\tTemperature_Instrument_strlen = 70 ;\n"
              + "\tSalinity_Instrument_strlen = 70 ;\n"
              + "\tPrimary_Investigator_strlen = 33 ;\n"
              + "\tPrimary_Investigator_VAR_strlen = 27 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tchar Institute(row, Institute_strlen) ;\n"
              + "\t\tInstitute:comment = \"name of institute which collected data\" ;\n"
              + "\t\tInstitute:long_name = \"Responsible_institute\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tchar Cast_Direction(row, Cast_Direction_strlen) ;\n"
              + "\t\tCast_Direction:long_name = \"Cast_Direction\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar Ocean_Vehicle(row, Ocean_Vehicle_strlen) ;\n"
              + "\t\tOcean_Vehicle:comment = \"Ocean_vehicle\" ;\n"
              + "\tint WMO_ID(row) ;\n"
              + "\t\tWMO_ID:_FillValue = -99999 ;\n"
              + "\t\tWMO_ID:long_name = \"WMO_identification_code\" ;\n"
              + "\tchar origflagset(row, origflagset_strlen) ;\n"
              + "\t\torigflagset:comment = \"set of originators flag codes to use\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Salinity_Instrument(row, Salinity_Instrument_strlen) ;\n"
              + "\t\tSalinity_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tSalinity_Instrument:long_name = \"Instrument\" ;\n"
              + "\tbyte Chlorophyll_WODprofileflag(row) ;\n"
              + "\t\tChlorophyll_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tChlorophyll_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tChlorophyll_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Primary_Investigator(row, Primary_Investigator_strlen) ;\n"
              + "\tchar Primary_Investigator_VAR(row, Primary_Investigator_VAR_strlen) ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag z_origflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_origflag(row) ;\n"
              + "\t\tz_origflag:_FillValue = -9 ;\n"
              + "\t\tz_origflag:comment = \"Originator flags are dependent on origflagset\" ;\n"
              + "\t\tz_origflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag Temperature_origflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tbyte Temperature_origflag(row) ;\n"
              + "\t\tTemperature_origflag:_FillValue = -9 ;\n"
              + "\t\tTemperature_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tTemperature_origflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag Salinity_origflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tbyte Salinity_origflag(row) ;\n"
              + "\t\tSalinity_origflag:_FillValue = -9 ;\n"
              + "\t\tSalinity_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tSalinity_origflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tfloat Pressure(row) ;\n"
              + "\t\tPressure:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure:ancillary_variables = \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag Pressure_origflag\" ;\n"
              + "\t\tPressure:coordinates = \"time lat lon z\" ;\n"
              + "\t\tPressure:grid_mapping = \"crs\" ;\n"
              + "\t\tPressure:long_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:standard_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:units = \"dbar\" ;\n"
              + "\tbyte Pressure_sigfigs(row) ;\n"
              + "\t\tPressure_sigfigs:long_name = \"sea_water_pressure significant_figures\" ;\n"
              + "\tbyte Pressure_origflag(row) ;\n"
              + "\t\tPressure_origflag:_FillValue = -9 ;\n"
              + "\t\tPressure_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tPressure_origflag:standard_name = \"sea_water_pressure status_flag\" ;\n"
              + "\tfloat Latitude(row) ;\n"
              + "\t\tLatitude:_FillValue = -1.0E10f ;\n"
              + "\t\tLatitude:ancillary_variables = \"Latitude_sigfigs Latitude_WODflag Latitude_WODprofileflag Latitude_origflag\" ;\n"
              + "\t\tLatitude:coordinates = \"time lat lon z\" ;\n"
              + "\t\tLatitude:grid_mapping = \"crs\" ;\n"
              + "\t\tLatitude:long_name = \"latitude\" ;\n"
              + "\t\tLatitude:standard_name = \"latitude\" ;\n"
              + "\t\tLatitude:units = \"degrees_north\" ;\n"
              + "\tbyte Latitude_sigfigs(row) ;\n"
              + "\t\tLatitude_sigfigs:long_name = \"latitude significant_figures\" ;\n"
              + "\tbyte Latitude_WODflag(row) ;\n"
              + "\t\tLatitude_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tLatitude_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tLatitude_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tLatitude_WODflag:standard_name = \"latitude status_flag\" ;\n"
              + "\tbyte Latitude_origflag(row) ;\n"
              + "\t\tLatitude_origflag:_FillValue = -9 ;\n"
              + "\t\tLatitude_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tLatitude_origflag:standard_name = \"latitude status_flag\" ;\n"
              + "\tfloat Longitude(row) ;\n"
              + "\t\tLongitude:_FillValue = -1.0E10f ;\n"
              + "\t\tLongitude:ancillary_variables = \"Longitude_sigfigs Longitude_WODflag Longitude_WODprofileflag Longitude_origflag\" ;\n"
              + "\t\tLongitude:coordinates = \"time lat lon z\" ;\n"
              + "\t\tLongitude:grid_mapping = \"crs\" ;\n"
              + "\t\tLongitude:long_name = \"longitude\" ;\n"
              + "\t\tLongitude:standard_name = \"longitude\" ;\n"
              + "\t\tLongitude:units = \"degrees_east\" ;\n"
              + "\tbyte Longitude_sigfigs(row) ;\n"
              + "\t\tLongitude_sigfigs:long_name = \"longitude significant_figures\" ;\n"
              + "\tbyte Longitude_WODflag(row) ;\n"
              + "\t\tLongitude_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tLongitude_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tLongitude_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tLongitude_WODflag:standard_name = \"longitude status_flag\" ;\n"
              + "\tbyte Longitude_origflag(row) ;\n"
              + "\t\tLongitude_origflag:_FillValue = -9 ;\n"
              + "\t\tLongitude_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tLongitude_origflag:standard_name = \"longitude status_flag\" ;\n"
              + "\tfloat JulianDay(row) ;\n"
              + "\t\tJulianDay:_FillValue = -1.0E10f ;\n"
              + "\t\tJulianDay:ancillary_variables = \"JulianDay_sigfigs JulianDay_WODflag JulianDay_WODprofileflag JulianDay_origflag\" ;\n"
              + "\t\tJulianDay:coordinates = \"time lat lon z\" ;\n"
              + "\t\tJulianDay:grid_mapping = \"crs\" ;\n"
              + "\t\tJulianDay:long_name = \"JulianDay\" ;\n"
              + "\tbyte JulianDay_sigfigs(row) ;\n"
              + "\t\tJulianDay_sigfigs:long_name = \"JulianDay significant_figures\" ;\n"
              + "\tbyte JulianDay_WODflag(row) ;\n"
              + "\t\tJulianDay_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tJulianDay_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tJulianDay_WODflag:long_name = \"JulianDay st\" ;\n"
              + "\tbyte JulianDay_origflag(row) ;\n"
              + "\t\tJulianDay_origflag:_FillValue = -9 ;\n"
              + "\t\tJulianDay_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tJulianDay_origflag:long_name = \"JulianDay st\" ;\n"
              + "\tfloat Chlorophyll(row) ;\n"
              + "\t\tChlorophyll:_FillValue = -1.0E10f ;\n"
              + "\t\tChlorophyll:ancillary_variables = \"Chlorophyll_sigfigs Chlorophyll_WODflag Chlorophyll_WODprofileflag Chlorophyll_origflag\" ;\n"
              + "\t\tChlorophyll:coordinates = \"time lat lon z\" ;\n"
              + "\t\tChlorophyll:grid_mapping = \"crs\" ;\n"
              + "\t\tChlorophyll:long_name = \"mass_concentration_of_chlorophyll_in_sea_water\" ;\n"
              + "\t\tChlorophyll:standard_name = \"mass_concentration_of_chlorophyll_in_sea_water\" ;\n"
              + "\t\tChlorophyll:units = \"ugram/l\" ;\n"
              + "\tbyte Chlorophyll_sigfigs(row) ;\n"
              + "\t\tChlorophyll_sigfigs:long_name = \"mass_concentration_of_chlorophyll_in_sea_water significant_figures\" ;\n"
              + "\tbyte Chlorophyll_WODflag(row) ;\n"
              + "\t\tChlorophyll_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tChlorophyll_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tChlorophyll_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tChlorophyll_WODflag:standard_name = \"mass_concentration_of_chlorophyll_in_sea_water status_flag\" ;\n"
              + "\tbyte Chlorophyll_origflag(row) ;\n"
              + "\t\tChlorophyll_origflag:_FillValue = -9 ;\n"
              + "\t\tChlorophyll_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tChlorophyll_origflag:standard_name = \"mass_concentration_of_chlorophyll_in_sea_water status_flag\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Platform, Institute, Orig_Stat_Num, Cast_Direction, dataset, Ocean_Vehicle, WMO_ID, origflagset, Temperature_WODprofileflag, Temperature_Instrument, Salinity_WODprofileflag, Salinity_Instrument, Chlorophyll_WODprofileflag, Primary_Investigator, Primary_Investigator_VAR\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-25\" ;\n"
              + "\t\t:date_modified = \"2018-03-25\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 63.9972f ;\n"
              + "\t\t:geospatial_lat_min = 22.481083f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = 16.887665f ;\n"
              + "\t\t:geospatial_lon_min = -158.30486f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 1026.7106f ;\n"
              + "\t\t:geospatial_vertical_min = -0.29778513f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_gld_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Platform,Institute,Orig_Stat_Num,Cast_Direction,dataset,Ocean_Vehicle,WMO_ID,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,Chlorophyll_WODprofileflag,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Latitude,Latitude_sigfigs,Latitude_WODflag,Latitude_origflag,Longitude,Longitude_sigfigs,Longitude_WODflag,Longitude_origflag,JulianDay,JulianDay_sigfigs,JulianDay_WODflag,JulianDay_origflag,Chlorophyll,Chlorophyll_sigfigs,Chlorophyll_WODflag,Chlorophyll_origflag\n"
              + "UNITED STATES,US035083,p015,15727905,56.54242,-55.34162,85832.0146483751,20050101,0.351561,111844,SG015 (AUV;Seaglider #015;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),377,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,0.89629334,0,1,2,3.2594981,4,0,1,-1.0E10,-127,-127,-9,0.9058805,3,1,56.54242,7,0,1,-55.34162,7,0,1,0.19433588,5,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US035083,p015,15727905,56.54242,-55.34162,85832.0146483751,20050101,0.351561,111844,SG015 (AUV;Seaglider #015;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),377,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,0.8763757,0,1,2,3.5394983,4,0,1,-1.0E10,-127,-127,-9,0.8857498,3,1,56.542416,7,0,1,-55.34162,7,0,1,0.19433588,5,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US035083,p015,15727905,56.54242,-55.34162,85832.0146483751,20050101,0.351561,111844,SG015 (AUV;Seaglider #015;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),377,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,1.1153866,0,1,3,3.6717756,4,0,1,-1.0E10,-127,-127,-9,1.1273179,4,1,56.542416,7,0,1,-55.34162,7,0,1,0.19433588,5,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US035083,p015,15727905,56.54242,-55.34162,85832.0146483751,20050101,0.351561,111844,SG015 (AUV;Seaglider #015;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),377,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,1.4639436,0,1,3,3.678846,4,0,1,34.7831,5,0,1,1.4796048,4,1,56.542416,7,0,1,-55.34162,7,0,1,0.19433588,5,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US035083,p015,15727905,56.54242,-55.34162,85832.0146483751,20050101,0.351561,111844,SG015 (AUV;Seaglider #015;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),377,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,1.7925826,0,1,3,3.6769755,4,0,1,34.789787,5,0,1,1.811761,4,1,56.542416,7,0,1,-55.341618,7,0,1,0.19433588,5,0,1,-1.0E10,,,-9\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 3398337 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Platform,Institute,Orig_Stat_Num,Cast_Direction,dataset,Ocean_Vehicle,WMO_ID,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,Chlorophyll_WODprofileflag,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Latitude,Latitude_sigfigs,Latitude_WODflag,Latitude_origflag,Longitude,Longitude_sigfigs,Longitude_WODflag,Longitude_origflag,JulianDay,JulianDay_sigfigs,JulianDay_WODflag,JulianDay_origflag,Chlorophyll,Chlorophyll_sigfigs,Chlorophyll_WODflag,Chlorophyll_origflag\n"
              + "UNITED STATES,US037856,0156003p012,17842328,47.00655,-127.19153,86196.91210919619,20051231,21.890621,156003,SG012 (AUV;Seaglider #012;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),3,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,976.9241,0,1,5,3.5159547,4,0,1,34.39147,5,0,1,989.7248,6,1,47.007023,7,0,1,-127.15596,8,0,1,365.91602,8,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US037856,0156003p012,17842328,47.00655,-127.19153,86196.91210919619,20051231,21.890621,156003,SG012 (AUV;Seaglider #012;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),3,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,980.71027,0,1,5,3.512552,4,0,1,34.39222,5,0,1,993.56976,6,1,47.007023,7,0,1,-127.15583,8,0,1,365.91406,8,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US037856,0156003p012,17842328,47.00655,-127.19153,86196.91210919619,20051231,21.890621,156003,SG012 (AUV;Seaglider #012;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),3,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,984.67487,0,1,5,3.5072713,4,0,1,-1.0E10,-127,-127,-9,997.5959,6,1,47.00701,7,0,1,-127.15576,8,0,1,365.91406,8,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US037856,0156003p012,17842328,47.00655,-127.19153,86196.91210919619,20051231,21.890621,156003,SG012 (AUV;Seaglider #012;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),3,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,987.4005,0,1,5,3.5013642,4,0,1,-1.0E10,-127,-127,-9,1000.36383,7,1,47.006985,7,0,1,-127.15577,8,0,1,365.9121,8,0,1,-1.0E10,,,-9\n"
              + "UNITED STATES,US037856,0156003p012,17842328,47.00655,-127.19153,86196.91210919619,20051231,21.890621,156003,SG012 (AUV;Seaglider #012;owned by Applied Phys.Lab.Univ.of Washington),UNIVERSITY OF WASHINGTON; APPLIED PHYSICS LABORATORY (APL),3,UP,glider,Seaglider,-99999,GTSPP,0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,CTD: SBE 41/41CP (Sea-Bird CTD Module for Autonomous Profiling Floats),0,RHINES; PETER B./ERIKSEN; CHARLES,all_variables/all_variables,987.97534,0,1,5,3.5010133,4,0,1,-1.0E10,-127,-127,-9,1000.94763,7,1,47.00696,7,0,1,-127.15578,8,0,1,365.9121,8,0,1,-1.0E10,,,-9\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    fullName = dir + "wod_gld_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"17842328"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "17842328,86196.91210919619,0.8763757,10.643924,0\n"
            + "17842328,86196.91210919619,1.1950568,10.658873,0\n"
            + "17842328,86196.91210919619,2.0614686,10.660649,0\n"
            + "17842328,86196.91210919619,2.8780832,10.663277,0\n"
            + "17842328,86196.91210919619,3.6449013,10.660924,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"17842328", "10.660649"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "17842328,86196.91210919619,2.0614686,10.660649,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all mrb
    fullName = dir + "wod_mrb_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 1184438 ;\n"
              + "\tcountry_strlen = 13 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 5 ;\n"
              + "\tProject_strlen = 80 ;\n"
              + "\tPlatform_strlen = 14 ;\n"
              + "\tInstitute_strlen = 62 ;\n"
              + "\tWind_Direction_strlen = 25 ;\n"
              + "\tdataset_strlen = 11 ;\n"
              + "\treal_time_strlen = 14 ;\n"
              + "\tdbase_orig_strlen = 24 ;\n"
              + "\torigflagset_strlen = 24 ;\n"
              + "\tTemperature_Instrument_strlen = 52 ;\n"
              + "\tSalinity_Instrument_strlen = 52 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Project(row, Project_strlen) ;\n"
              + "\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n"
              + "\t\tProject:long_name = \"Project_name\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tchar Institute(row, Institute_strlen) ;\n"
              + "\t\tInstitute:comment = \"name of institute which collected data\" ;\n"
              + "\t\tInstitute:long_name = \"Responsible_institute\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tchar Wind_Direction(row, Wind_Direction_strlen) ;\n"
              + "\t\tWind_Direction:long_name = \"Wind_Direction\" ;\n"
              + "\t\tWind_Direction:units_wod = \"WMO 0877 or NODC 0110\" ;\n"
              + "\tfloat Wind_Speed(row) ;\n"
              + "\t\tWind_Speed:_FillValue = -1.0E10f ;\n"
              + "\t\tWind_Speed:standard_name = \"wind_speed\" ;\n"
              + "\t\tWind_Speed:units = \"knots\" ;\n"
              + "\tfloat Dry_Bulb_Temp(row) ;\n"
              + "\t\tDry_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tDry_Bulb_Temp:long_name = \"Dry_Bulb_Air_Temperature\" ;\n"
              + "\t\tDry_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar real_time(row, real_time_strlen) ;\n"
              + "\t\treal_time:comment = \"timeliness and quality status\" ;\n"
              + "\t\treal_time:long_name = \"real_time_data\" ;\n"
              + "\tchar dbase_orig(row, dbase_orig_strlen) ;\n"
              + "\t\tdbase_orig:comment = \"Database from which data were extracted\" ;\n"
              + "\t\tdbase_orig:long_name = \"database_origin\" ;\n"
              + "\tint WMO_ID(row) ;\n"
              + "\t\tWMO_ID:_FillValue = -99999 ;\n"
              + "\t\tWMO_ID:long_name = \"WMO_identification_code\" ;\n"
              + "\tchar origflagset(row, origflagset_strlen) ;\n"
              + "\t\torigflagset:comment = \"set of originators flag codes to use\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Salinity_Instrument(row, Salinity_Instrument_strlen) ;\n"
              + "\t\tSalinity_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tSalinity_Instrument:long_name = \"Instrument\" ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag z_origflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_origflag(row) ;\n"
              + "\t\tz_origflag:_FillValue = -9 ;\n"
              + "\t\tz_origflag:comment = \"Originator flags are dependent on origflagset\" ;\n"
              + "\t\tz_origflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag Temperature_origflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tbyte Temperature_origflag(row) ;\n"
              + "\t\tTemperature_origflag:_FillValue = -9 ;\n"
              + "\t\tTemperature_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tTemperature_origflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag Salinity_origflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tbyte Salinity_origflag(row) ;\n"
              + "\t\tSalinity_origflag:_FillValue = -9 ;\n"
              + "\t\tSalinity_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tSalinity_origflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Project, Platform, Institute, Orig_Stat_Num, Wind_Direction, Wind_Speed, Dry_Bulb_Temp, dataset, real_time, dbase_orig, WMO_ID, origflagset, Temperature_WODprofileflag, Temperature_Instrument, Salinity_WODprofileflag, Salinity_Instrument\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-24\" ;\n"
              + "\t\t:date_modified = \"2018-03-24\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 80.5578f ;\n"
              + "\t\t:geospatial_lat_min = -18.89f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = -0.02999878f ;\n"
              + "\t\t:geospatial_lon_min = 80.48f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 4309.0f ;\n"
              + "\t\t:geospatial_vertical_min = 1.0f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_mrb_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Orig_Stat_Num,Wind_Direction,Wind_Speed,Dry_Bulb_Temp,dataset,real_time,dbase_orig,WMO_ID,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag\n"
              + "UNITED STATES,US029953,,12631835,80.5578,-68.9076,85832.0,20050101,0.0,63240,ARCTIC/SUBARCTIC OCEAN FLUXES (ASOF),,UNIVERSITY OF DELAWARE;SCHOOL OF MARINE SCIENCE AND POLICY,-99999,,-1.0E10,-1.0E10,moored buoy,,,-99999,,0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",28.294939,0,-9,5,-1.782,4,0,-9,32.662,5,0,-9\n"
              + "UNITED STATES,US029953,,12631835,80.5578,-68.9076,85832.0,20050101,0.0,63240,ARCTIC/SUBARCTIC OCEAN FLUXES (ASOF),,UNIVERSITY OF DELAWARE;SCHOOL OF MARINE SCIENCE AND POLICY,-99999,,-1.0E10,-1.0E10,moored buoy,,,-99999,,0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",77.65329,0,-9,5,-1.755,4,0,-9,32.706,5,0,-9\n"
              + "UNITED STATES,US029953,,12631835,80.5578,-68.9076,85832.0,20050101,0.0,63240,ARCTIC/SUBARCTIC OCEAN FLUXES (ASOF),,UNIVERSITY OF DELAWARE;SCHOOL OF MARINE SCIENCE AND POLICY,-99999,,-1.0E10,-1.0E10,moored buoy,,,-99999,,0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",126.99971,0,-9,6,-1.235,4,0,-9,33.404,5,0,-9\n"
              + "UNITED STATES,US029953,,12631835,80.5578,-68.9076,85832.0,20050101,0.0,63240,ARCTIC/SUBARCTIC OCEAN FLUXES (ASOF),,UNIVERSITY OF DELAWARE;SCHOOL OF MARINE SCIENCE AND POLICY,-99999,,-1.0E10,-1.0E10,moored buoy,,,-99999,,0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",196.10425,0,-9,6,-0.396,3,0,-9,34.347,5,0,-9\n"
              + "UNITED STATES,US029953,,12631836,80.5578,-68.9076,85832.01000976562,20050101,0.24023438,63240,ARCTIC/SUBARCTIC OCEAN FLUXES (ASOF),,UNIVERSITY OF DELAWARE;SCHOOL OF MARINE SCIENCE AND POLICY,-99999,,-1.0E10,-1.0E10,moored buoy,,,-99999,,0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",0,\"CTD: SBE 37-IM MicroCAT (Sea-Bird Electronics, Inc.)\",27.602451,0,-9,5,-1.782,4,0,-9,32.66,5,0,-9\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 1184438 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Orig_Stat_Num,Wind_Direction,Wind_Speed,Dry_Bulb_Temp,dataset,real_time,dbase_orig,WMO_ID,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Salinity_WODprofileflag,Salinity_Instrument,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag\n"
              + "BRAZIL,BR001182,,10688847,4.09,-37.98,86196.0,20051231,9.96921E36,78936,PIRATA BUOY ARRAY,FIXED PLATFORM,,-99999,85 DEGREES - 94 DEGREES,8.3592005,27.0,moored buoy,,PMEL TAO/PIRATA database,31002,PMEL TAO/PIRATA database,0,,0,,100.0,0,2,3,26.48,5,0,1,-1.0E10,-127,-127,-9\n"
              + "BRAZIL,BR001182,,10688847,4.09,-37.98,86196.0,20051231,9.96921E36,78936,PIRATA BUOY ARRAY,FIXED PLATFORM,,-99999,85 DEGREES - 94 DEGREES,8.3592005,27.0,moored buoy,,PMEL TAO/PIRATA database,31002,PMEL TAO/PIRATA database,0,,0,,140.0,0,2,3,15.74,5,0,2,-1.0E10,-127,-127,-9\n"
              + "BRAZIL,BR001182,,10688847,4.09,-37.98,86196.0,20051231,9.96921E36,78936,PIRATA BUOY ARRAY,FIXED PLATFORM,,-99999,85 DEGREES - 94 DEGREES,8.3592005,27.0,moored buoy,,PMEL TAO/PIRATA database,31002,PMEL TAO/PIRATA database,0,,0,,180.0,0,2,3,13.85,5,0,1,-1.0E10,-127,-127,-9\n"
              + "BRAZIL,BR001182,,10688847,4.09,-37.98,86196.0,20051231,9.96921E36,78936,PIRATA BUOY ARRAY,FIXED PLATFORM,,-99999,85 DEGREES - 94 DEGREES,8.3592005,27.0,moored buoy,,PMEL TAO/PIRATA database,31002,PMEL TAO/PIRATA database,0,,0,,300.0,0,2,3,11.67,5,0,1,-1.0E10,-127,-127,-9\n"
              + "BRAZIL,BR001182,,10688847,4.09,-37.98,86196.0,20051231,9.96921E36,78936,PIRATA BUOY ARRAY,FIXED PLATFORM,,-99999,85 DEGREES - 94 DEGREES,8.3592005,27.0,moored buoy,,PMEL TAO/PIRATA database,31002,PMEL TAO/PIRATA database,0,,0,,500.0,0,2,3,8.16,4,0,1,-1.0E10,-127,-127,-9\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    fullName = dir + "wod_mrb_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"10688847"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10688847,86196.0,1.0,27.98,0\n"
            + "10688847,86196.0,20.0,27.98,0\n"
            + "10688847,86196.0,40.0,27.99,0\n"
            + "10688847,86196.0,60.0,28.0,0\n"
            + "10688847,86196.0,80.0,27.97,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"10688847", "27.99"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10688847,86196.0,40.0,27.99,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all osd -- THIS FILE FAILS BECAUSE IT HAS DataType=Structure!
    // fullName = dir + "wod_osd_2005.nc";
    // table.readInvalidCRA(fullName, null, null, null, null);
    // results = table.toString(5);
    // expected = "zztop\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // table.removeRows(0, 146042 - 5);
    // results = table.dataToString();
    // expected = "zztop\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all
    fullName = dir + "wod_pfl_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 4420114 ;\n"
              + "\tcountry_strlen = 18 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 8 ;\n"
              + "\tProject_strlen = 74 ;\n"
              + "\tPlatform_strlen = 15 ;\n"
              + "\tInstitute_strlen = 79 ;\n"
              + "\tCast_Direction_strlen = 22 ;\n"
              + "\tdataset_strlen = 15 ;\n"
              + "\tRecorder_strlen = 44 ;\n"
              + "\treal_time_strlen = 36 ;\n"
              + "\tOcean_Vehicle_strlen = 68 ;\n"
              + "\tdbase_orig_strlen = 22 ;\n"
              + "\torigflagset_strlen = 21 ;\n"
              + "\tTemperature_Instrument_strlen = 40 ;\n"
              + "\tSalinity_Instrument_strlen = 40 ;\n"
              + "\tSalinity_Method_strlen = 0 ;\n"
              + "\tOxygen_Original_units_strlen = 7 ;\n"
              + "\tPrimary_Investigator_strlen = 120 ;\n"
              + "\tPrimary_Investigator_VAR_strlen = 97 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Project(row, Project_strlen) ;\n"
              + "\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n"
              + "\t\tProject:long_name = \"Project_name\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tchar Institute(row, Institute_strlen) ;\n"
              + "\t\tInstitute:comment = \"name of institute which collected data\" ;\n"
              + "\t\tInstitute:long_name = \"Responsible_institute\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tchar Cast_Direction(row, Cast_Direction_strlen) ;\n"
              + "\t\tCast_Direction:long_name = \"Cast_Direction\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar Recorder(row, Recorder_strlen) ;\n"
              + "\t\tRecorder:comment = \"Device which recorded measurements\" ;\n"
              + "\t\tRecorder:long_name = \"Recorder\" ;\n"
              + "\t\tRecorder:units_wod = \"WMO code 4770\" ;\n"
              + "\tchar real_time(row, real_time_strlen) ;\n"
              + "\t\treal_time:comment = \"timeliness and quality status\" ;\n"
              + "\t\treal_time:long_name = \"real_time_data\" ;\n"
              + "\tchar Ocean_Vehicle(row, Ocean_Vehicle_strlen) ;\n"
              + "\t\tOcean_Vehicle:comment = \"Ocean_vehicle\" ;\n"
              + "\tchar dbase_orig(row, dbase_orig_strlen) ;\n"
              + "\t\tdbase_orig:comment = \"Database from which data were extracted\" ;\n"
              + "\t\tdbase_orig:long_name = \"database_origin\" ;\n"
              + "\tint WMO_ID(row) ;\n"
              + "\t\tWMO_ID:_FillValue = -99999 ;\n"
              + "\t\tWMO_ID:long_name = \"WMO_identification_code\" ;\n"
              + "\tchar origflagset(row, origflagset_strlen) ;\n"
              + "\t\torigflagset:comment = \"set of originators flag codes to use\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tfloat Temperature_Adjustment(row) ;\n"
              + "\t\tTemperature_Adjustment:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature_Adjustment:comment = \"Adjustment made to original measurement values and measurement units\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Salinity_Instrument(row, Salinity_Instrument_strlen) ;\n"
              + "\t\tSalinity_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tSalinity_Instrument:long_name = \"Instrument\" ;\n"
              + "\tchar Salinity_Method(row, Salinity_Method_strlen) ;\n"
              + "\t\tSalinity_Method:comment = \"Method\" ;\n"
              + "\tfloat Salinity_Adjustment(row) ;\n"
              + "\t\tSalinity_Adjustment:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity_Adjustment:comment = \"Adjustment made to original measurement values and measurement units\" ;\n"
              + "\tint Salinity_(row) ;\n"
              + "\t\tSalinity_:_FillValue = -99999 ;\n"
              + "\tfloat Pressure_Adjustment(row) ;\n"
              + "\t\tPressure_Adjustment:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure_Adjustment:comment = \"Adjustment made to original measurement values and measurement units\" ;\n"
              + "\tbyte Oxygen_WODprofileflag(row) ;\n"
              + "\t\tOxygen_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tOxygen_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tOxygen_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Oxygen_Original_units(row, Oxygen_Original_units_strlen) ;\n"
              + "\t\tOxygen_Original_units:comment = \"Units originally used: coverted to standard units\" ;\n"
              + "\tfloat Oxygen_Adjustment(row) ;\n"
              + "\t\tOxygen_Adjustment:_FillValue = -1.0E10f ;\n"
              + "\t\tOxygen_Adjustment:comment = \"Adjustment made to original measurement values and measurement units\" ;\n"
              + "\tchar Primary_Investigator(row, Primary_Investigator_strlen) ;\n"
              + "\tchar Primary_Investigator_VAR(row, Primary_Investigator_VAR_strlen) ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag z_origflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_origflag(row) ;\n"
              + "\t\tz_origflag:_FillValue = -9 ;\n"
              + "\t\tz_origflag:comment = \"Originator flags are dependent on origflagset\" ;\n"
              + "\t\tz_origflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag Temperature_origflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tbyte Temperature_origflag(row) ;\n"
              + "\t\tTemperature_origflag:_FillValue = -9 ;\n"
              + "\t\tTemperature_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tTemperature_origflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag Salinity_origflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tbyte Salinity_origflag(row) ;\n"
              + "\t\tSalinity_origflag:_FillValue = -9 ;\n"
              + "\t\tSalinity_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tSalinity_origflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tfloat Pressure(row) ;\n"
              + "\t\tPressure:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure:ancillary_variables = \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag Pressure_origflag\" ;\n"
              + "\t\tPressure:coordinates = \"time lat lon z\" ;\n"
              + "\t\tPressure:grid_mapping = \"crs\" ;\n"
              + "\t\tPressure:long_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:standard_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:units = \"dbar\" ;\n"
              + "\tbyte Pressure_sigfigs(row) ;\n"
              + "\t\tPressure_sigfigs:long_name = \"sea_water_pressure significant_figures\" ;\n"
              + "\tbyte Pressure_origflag(row) ;\n"
              + "\t\tPressure_origflag:_FillValue = -9 ;\n"
              + "\t\tPressure_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tPressure_origflag:standard_name = \"sea_water_pressure status_flag\" ;\n"
              + "\tfloat Oxygen(row) ;\n"
              + "\t\tOxygen:_FillValue = -1.0E10f ;\n"
              + "\t\tOxygen:ancillary_variables = \"Oxygen_sigfigs Oxygen_WODflag Oxygen_WODprofileflag Oxygen_origflag\" ;\n"
              + "\t\tOxygen:coordinates = \"time lat lon z\" ;\n"
              + "\t\tOxygen:grid_mapping = \"crs\" ;\n"
              + "\t\tOxygen:long_name = \"volume_fraction_of_oxygen_in_sea_water\" ;\n"
              + "\t\tOxygen:standard_name = \"volume_fraction_of_oxygen_in_sea_water\" ;\n"
              + "\t\tOxygen:units = \"ml/l\" ;\n"
              + "\tbyte Oxygen_sigfigs(row) ;\n"
              + "\t\tOxygen_sigfigs:long_name = \"volume_fraction_of_oxygen_in_sea_water significant_figures\" ;\n"
              + "\tbyte Oxygen_WODflag(row) ;\n"
              + "\t\tOxygen_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tOxygen_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tOxygen_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tOxygen_WODflag:standard_name = \"volume_fraction_of_oxygen_in_sea_water status_flag\" ;\n"
              + "\tbyte Oxygen_origflag(row) ;\n"
              + "\t\tOxygen_origflag:_FillValue = -9 ;\n"
              + "\t\tOxygen_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tOxygen_origflag:standard_name = \"volume_fraction_of_oxygen_in_sea_water status_flag\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Project, Platform, Institute, Orig_Stat_Num, Cast_Direction, dataset, Recorder, real_time, Ocean_Vehicle, dbase_orig, WMO_ID, origflagset, Temperature_WODprofileflag, Temperature_Instrument, Temperature_Adjustment, Salinity_WODprofileflag, Salinity_Instrument, Salinity_Method, Salinity_Adjustment, Salinity_, Pressure_Adjustment, Oxygen_WODprofileflag, Oxygen_Original_units, Oxygen_Adjustment, Primary_Investigator, Primary_Investigator_VAR\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-24\" ;\n"
              + "\t\t:date_modified = \"2018-03-24\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 77.427f ;\n"
              + "\t\t:geospatial_lat_min = -72.811f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = 180.0f ;\n"
              + "\t\t:geospatial_lon_min = -180.0f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 6420.1216f ;\n"
              + "\t\t:geospatial_vertical_min = -1.0f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_pfl_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Orig_Stat_Num,Cast_Direction,dataset,Recorder,real_time,Ocean_Vehicle,dbase_orig,WMO_ID,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Temperature_Adjustment,Salinity_WODprofileflag,Salinity_Instrument,Salinity_Method,Salinity_Adjustment,Salinity_,Pressure_Adjustment,Oxygen_WODprofileflag,Oxygen_Original_units,Oxygen_Adjustment,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Oxygen,Oxygen_sigfigs,Oxygen_WODflag,Oxygen_origflag\n"
              + "JAPAN,JP031068,Q1900475,10405561,-13.641,69.829,85832.02929583378,20050101,0.7031,2352,J-ARGO (JAPAN ARGO),,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),2,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),1900475,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,CTD: TYPE UNKNOWN,,0.0,-99999,0.0,0,,-1.0E10,TAKEUCHI; KENSUKE,all_variables,4.4739165,0,1,3,27.817,5,0,1,34.939,5,0,1,4.5,2,1,-1.0E10,,,-9\n"
              + "JAPAN,JP031068,Q1900475,10405561,-13.641,69.829,85832.02929583378,20050101,0.7031,2352,J-ARGO (JAPAN ARGO),,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),2,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),1900475,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,CTD: TYPE UNKNOWN,,0.0,-99999,0.0,0,,-1.0E10,TAKEUCHI; KENSUKE,all_variables,9.44482,0,1,3,27.745,5,0,1,34.942,5,0,1,9.5,2,1,-1.0E10,,,-9\n"
              + "JAPAN,JP031068,Q1900475,10405561,-13.641,69.829,85832.02929583378,20050101,0.7031,2352,J-ARGO (JAPAN ARGO),,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),2,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),1900475,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,CTD: TYPE UNKNOWN,,0.0,-99999,0.0,0,,-1.0E10,TAKEUCHI; KENSUKE,all_variables,13.91853,0,1,4,27.408,5,0,1,34.98,5,0,1,14.0,3,1,-1.0E10,,,-9\n"
              + "JAPAN,JP031068,Q1900475,10405561,-13.641,69.829,85832.02929583378,20050101,0.7031,2352,J-ARGO (JAPAN ARGO),,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),2,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),1900475,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,CTD: TYPE UNKNOWN,,0.0,-99999,0.0,0,,-1.0E10,TAKEUCHI; KENSUKE,all_variables,18.889206,0,1,4,27.286,5,0,1,34.997,5,0,1,19.0,3,1,-1.0E10,,,-9\n"
              + "JAPAN,JP031068,Q1900475,10405561,-13.641,69.829,85832.02929583378,20050101,0.7031,2352,J-ARGO (JAPAN ARGO),,JAPAN AGENCY FOR MARINE-EARTH SCIENCE AND TECHNOLOGY (JAMSTEC),2,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),1900475,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,CTD: TYPE UNKNOWN,,0.0,-99999,0.0,0,,-1.0E10,TAKEUCHI; KENSUKE,all_variables,24.257399,0,1,4,27.233,5,0,1,35.001,5,0,1,24.4,3,1,-1.0E10,,,-9\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 4420114 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Orig_Stat_Num,Cast_Direction,dataset,Recorder,real_time,Ocean_Vehicle,dbase_orig,WMO_ID,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Temperature_Adjustment,Salinity_WODprofileflag,Salinity_Instrument,Salinity_Method,Salinity_Adjustment,Salinity_,Pressure_Adjustment,Oxygen_WODprofileflag,Oxygen_Original_units,Oxygen_Adjustment,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Salinity_origflag,Pressure,Pressure_sigfigs,Pressure_origflag,Oxygen,Oxygen_sigfigs,Oxygen_WODflag,Oxygen_origflag\n"
              + "UNKNOWN,99005892,2900451,11898775,39.42,132.58,86196.9589958787,20051231,23.015902,42682,,,,45,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),2900451,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,\"CTD: Sea-Bird Electronics, MODEL UNKNOWN\",,0.0,-99999,0.0,0,,-1.0E10,SUK; MOON-SIK,all_variables,613.0759,0,1,5,0.474,3,0,1,34.071,5,0,2,618.7,6,1,-1.0E10,,,-9\n"
              + "UNKNOWN,99005892,2900451,11898775,39.42,132.58,86196.9589958787,20051231,23.015902,42682,,,,45,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),2900451,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,\"CTD: Sea-Bird Electronics, MODEL UNKNOWN\",,0.0,-99999,0.0,0,,-1.0E10,SUK; MOON-SIK,all_variables,632.8637,0,1,5,0.463,3,0,1,34.071,5,0,2,638.7,6,1,-1.0E10,,,-9\n"
              + "UNKNOWN,99005892,2900451,11898775,39.42,132.58,86196.9589958787,20051231,23.015902,42682,,,,45,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),2900451,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,\"CTD: Sea-Bird Electronics, MODEL UNKNOWN\",,0.0,-99999,0.0,0,,-1.0E10,SUK; MOON-SIK,all_variables,652.84753,0,1,5,0.448,3,0,1,34.07,5,0,2,658.9,6,1,-1.0E10,,,-9\n"
              + "UNKNOWN,99005892,2900451,11898775,39.42,132.58,86196.9589958787,20051231,23.015902,42682,,,,45,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),2900451,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,\"CTD: Sea-Bird Electronics, MODEL UNKNOWN\",,0.0,-99999,0.0,0,,-1.0E10,SUK; MOON-SIK,all_variables,672.5326,0,1,5,0.438,3,0,1,34.07,5,0,2,678.8,6,1,-1.0E10,,,-9\n"
              + "UNKNOWN,99005892,2900451,11898775,39.42,132.58,86196.9589958787,20051231,23.015902,42682,,,,45,UP,profiling float,,delayed_mode quality controlled data,\"APEX (Autonomous Profiling Explorer, Webb Research Corporation)\",US GODAE server (Argo),2900451,ARGO profiling floats,0,CTD: TYPE UNKNOWN,0.0,0,\"CTD: Sea-Bird Electronics, MODEL UNKNOWN\",,0.0,-99999,0.0,0,,-1.0E10,SUK; MOON-SIK,all_variables,693.007,0,1,5,0.428,3,0,1,34.07,5,0,2,699.5,6,1,-1.0E10,,,-9\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    fullName = dir + "wod_pfl_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"11898775"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "11898775,86196.9589958787,4.5649443,7.71,0\n"
            + "11898775,86196.9589958787,13.892994,7.696,0\n"
            + "11898775,86196.9589958787,24.014437,7.491,0\n"
            + "11898775,86196.9589958787,33.83771,7.302,0\n"
            + "11898775,86196.9589958787,43.958168,7.241,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"11898775", "7.491"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "11898775,86196.9589958787,24.014437,7.491,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all uor
    fullName = dir + "wod_uor_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 1169159 ;\n"
              + "\tcountry_strlen = 6 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 9 ;\n"
              + "\tPlatform_strlen = 73 ;\n"
              + "\tWave_Height_strlen = 10 ;\n"
              + "\tWave_Period_strlen = 17 ;\n"
              + "\tWind_Direction_strlen = 25 ;\n"
              + "\tWeather_Condition_strlen = 169 ;\n"
              + "\tCloud_Cover_strlen = 65 ;\n"
              + "\tdataset_strlen = 9 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tfloat Bottom_Depth(row) ;\n"
              + "\t\tBottom_Depth:_FillValue = -1.0E10f ;\n"
              + "\t\tBottom_Depth:standard_name = \"sea_floor_depth_below_sea_surface\" ;\n"
              + "\t\tBottom_Depth:units = \"meters\" ;\n"
              + "\tchar Wave_Height(row, Wave_Height_strlen) ;\n"
              + "\t\tWave_Height:long_name = \"Wave_Height\" ;\n"
              + "\t\tWave_Height:units_wod = \"WMO 1555 or NODC 0104\" ;\n"
              + "\tchar Wave_Period(row, Wave_Period_strlen) ;\n"
              + "\t\tWave_Period:long_name = \"Wave_Period\" ;\n"
              + "\t\tWave_Period:units_wod = \"WMO 3155 or NODC 0378\" ;\n"
              + "\tchar Wind_Direction(row, Wind_Direction_strlen) ;\n"
              + "\t\tWind_Direction:long_name = \"Wind_Direction\" ;\n"
              + "\t\tWind_Direction:units_wod = \"WMO 0877 or NODC 0110\" ;\n"
              + "\tfloat Wind_Speed(row) ;\n"
              + "\t\tWind_Speed:_FillValue = -1.0E10f ;\n"
              + "\t\tWind_Speed:standard_name = \"wind_speed\" ;\n"
              + "\t\tWind_Speed:units = \"knots\" ;\n"
              + "\tfloat Barometric_Pres(row) ;\n"
              + "\t\tBarometric_Pres:_FillValue = -1.0E10f ;\n"
              + "\t\tBarometric_Pres:long_name = \"Barometric_Pressure\" ;\n"
              + "\t\tBarometric_Pres:units = \"millibars\" ;\n"
              + "\tfloat Dry_Bulb_Temp(row) ;\n"
              + "\t\tDry_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tDry_Bulb_Temp:long_name = \"Dry_Bulb_Air_Temperature\" ;\n"
              + "\t\tDry_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tfloat Wet_Bulb_Temp(row) ;\n"
              + "\t\tWet_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tWet_Bulb_Temp:long_name = \"Wet_Bulb_Air_Temperature\" ;\n"
              + "\t\tWet_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tchar Weather_Condition(row, Weather_Condition_strlen) ;\n"
              + "\t\tWeather_Condition:comment = \"Weather conditions at time of measurements\" ;\n"
              + "\t\tWeather_Condition:long_name = \"Weather_Condition\" ;\n"
              + "\tchar Cloud_Cover(row, Cloud_Cover_strlen) ;\n"
              + "\t\tCloud_Cover:long_name = \"Cloud_Cover\" ;\n"
              + "\t\tCloud_Cover:units_wod = \"WMO 2700 or NODC 0105\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tbyte Salinity_WODprofileflag(row) ;\n"
              + "\t\tSalinity_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tSalinity_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tfloat Salinity(row) ;\n"
              + "\t\tSalinity:_FillValue = -1.0E10f ;\n"
              + "\t\tSalinity:ancillary_variables = \"Salinity_sigfigs Salinity_WODflag Salinity_WODprofileflag\" ;\n"
              + "\t\tSalinity:coordinates = \"time lat lon z\" ;\n"
              + "\t\tSalinity:grid_mapping = \"crs\" ;\n"
              + "\t\tSalinity:long_name = \"sea_water_salinity\" ;\n"
              + "\t\tSalinity:standard_name = \"sea_water_salinity\" ;\n"
              + "\tbyte Salinity_sigfigs(row) ;\n"
              + "\t\tSalinity_sigfigs:long_name = \"sea_water_salinity significant_figures\" ;\n"
              + "\tbyte Salinity_WODflag(row) ;\n"
              + "\t\tSalinity_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tSalinity_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tSalinity_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tSalinity_WODflag:standard_name = \"sea_water_salinity status_flag\" ;\n"
              + "\tfloat Pressure(row) ;\n"
              + "\t\tPressure:_FillValue = -1.0E10f ;\n"
              + "\t\tPressure:ancillary_variables = \"Pressure_sigfigs Pressure_WODflag Pressure_WODprofileflag\" ;\n"
              + "\t\tPressure:coordinates = \"time lat lon z\" ;\n"
              + "\t\tPressure:grid_mapping = \"crs\" ;\n"
              + "\t\tPressure:long_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:standard_name = \"sea_water_pressure\" ;\n"
              + "\t\tPressure:units = \"dbar\" ;\n"
              + "\tbyte Pressure_sigfigs(row) ;\n"
              + "\t\tPressure_sigfigs:long_name = \"sea_water_pressure significant_figures\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, latitude, longitude, time, date, GMT_time, Access_no, Platform, Bottom_Depth, Wave_Height, Wave_Period, Wind_Direction, Wind_Speed, Barometric_Pres, Dry_Bulb_Temp, Wet_Bulb_Temp, Weather_Condition, Cloud_Cover, dataset, Temperature_WODprofileflag, Salinity_WODprofileflag\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-25\" ;\n"
              + "\t\t:date_modified = \"2018-03-25\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 62.485f ;\n"
              + "\t\t:geospatial_lat_min = 42.67f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = -46.9317f ;\n"
              + "\t\t:geospatial_lon_min = -65.84f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 1443.402f ;\n"
              + "\t\t:geospatial_vertical_min = 0.0f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_uor_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-17\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-08\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Platform,Bottom_Depth,Wave_Height,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Cover,dataset,Temperature_WODprofileflag,Salinity_WODprofileflag,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Pressure,Pressure_sigfigs\n"
              + "CANADA,CA155645,18TL05542,10742153,49.435,-50.725,85839.57291668653,20050108,13.75,98794,TELEOST (CGGS;R/V;c.s.CGCB;b.1988;ex.Atlantic Champion 1995;IMO8714346),326.0,2.5 METER,6 OR 7 SECONDS,285 DEGREES - 294 DEGREES,24.8832,1023.0,0.0,-0.1,CLOUDS GENERALLY DISSOLVING OF BECOMING LESS DEVELOPED-CHAR.  CHANGE OF STATE OF SKY DURING PAST HR.,6 OKTAS     7/10-8/10,towed CTD,0,0,0.0,0,1,1.321,4,0,33.597,5,0,0.0,1\n"
              + "CANADA,CA155645,18TL05542,10742153,49.435,-50.725,85839.57291668653,20050108,13.75,98794,TELEOST (CGGS;R/V;c.s.CGCB;b.1988;ex.Atlantic Champion 1995;IMO8714346),326.0,2.5 METER,6 OR 7 SECONDS,285 DEGREES - 294 DEGREES,24.8832,1023.0,0.0,-0.1,CLOUDS GENERALLY DISSOLVING OF BECOMING LESS DEVELOPED-CHAR.  CHANGE OF STATE OF SKY DURING PAST HR.,6 OKTAS     7/10-8/10,towed CTD,0,0,0.099147804,0,1,1.321,4,0,33.598,5,0,0.1,1\n"
              + "CANADA,CA155645,18TL05542,10742153,49.435,-50.725,85839.57291668653,20050108,13.75,98794,TELEOST (CGGS;R/V;c.s.CGCB;b.1988;ex.Atlantic Champion 1995;IMO8714346),326.0,2.5 METER,6 OR 7 SECONDS,285 DEGREES - 294 DEGREES,24.8832,1023.0,0.0,-0.1,CLOUDS GENERALLY DISSOLVING OF BECOMING LESS DEVELOPED-CHAR.  CHANGE OF STATE OF SKY DURING PAST HR.,6 OKTAS     7/10-8/10,towed CTD,0,0,0.19829556,0,1,1.321,4,0,33.601,5,0,0.2,1\n"
              + "CANADA,CA155645,18TL05542,10742153,49.435,-50.725,85839.57291668653,20050108,13.75,98794,TELEOST (CGGS;R/V;c.s.CGCB;b.1988;ex.Atlantic Champion 1995;IMO8714346),326.0,2.5 METER,6 OR 7 SECONDS,285 DEGREES - 294 DEGREES,24.8832,1023.0,0.0,-0.1,CLOUDS GENERALLY DISSOLVING OF BECOMING LESS DEVELOPED-CHAR.  CHANGE OF STATE OF SKY DURING PAST HR.,6 OKTAS     7/10-8/10,towed CTD,0,0,0.79318106,0,1,1.321,4,0,33.603,5,0,0.8,1\n"
              + "CANADA,CA155645,18TL05542,10742153,49.435,-50.725,85839.57291668653,20050108,13.75,98794,TELEOST (CGGS;R/V;c.s.CGCB;b.1988;ex.Atlantic Champion 1995;IMO8714346),326.0,2.5 METER,6 OR 7 SECONDS,285 DEGREES - 294 DEGREES,24.8832,1023.0,0.0,-0.1,CLOUDS GENERALLY DISSOLVING OF BECOMING LESS DEVELOPED-CHAR.  CHANGE OF STATE OF SKY DURING PAST HR.,6 OKTAS     7/10-8/10,towed CTD,0,0,1.487212,0,2,1.321,4,0,33.604,5,0,1.5,2\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 1169159 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Platform,Bottom_Depth,Wave_Height,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Cover,dataset,Temperature_WODprofileflag,Salinity_WODprofileflag,z,z_WODflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Salinity,Salinity_sigfigs,Salinity_WODflag,Pressure,Pressure_sigfigs\n"
              + "CANADA,CA019465,181C05632,10749307,51.2133,-53.85,86182.78125,20051217,18.75,2026318698,WILFRED TEMPLEMAN (CCGS;F/R/V;call sign CGDV; built 1981;IMO7907099),206.0,1.5 METER,5 SECONDS OR LESS,95 DEGREES - 104 DEGREES,19.8288,1032.0,-1.0E10,-1.0E10,CONTINUOUS FALL OF SNOW FLAKES-MODERATE AT TIME OF OBSERVATION,6 OKTAS     7/10-8/10,towed CTD,0,0,197.47443,0,4,2.176,4,0,34.101,5,0,199.3,4\n"
              + "CANADA,CA019465,181C05632,10749307,51.2133,-53.85,86182.78125,20051217,18.75,2026318698,WILFRED TEMPLEMAN (CCGS;F/R/V;call sign CGDV; built 1981;IMO7907099),206.0,1.5 METER,5 SECONDS OR LESS,95 DEGREES - 104 DEGREES,19.8288,1032.0,-1.0E10,-1.0E10,CONTINUOUS FALL OF SNOW FLAKES-MODERATE AT TIME OF OBSERVATION,6 OKTAS     7/10-8/10,towed CTD,0,0,197.57347,0,4,2.176,4,0,34.101,5,0,199.4,4\n"
              + "CANADA,CA019465,181C05632,10749307,51.2133,-53.85,86182.78125,20051217,18.75,2026318698,WILFRED TEMPLEMAN (CCGS;F/R/V;call sign CGDV; built 1981;IMO7907099),206.0,1.5 METER,5 SECONDS OR LESS,95 DEGREES - 104 DEGREES,19.8288,1032.0,-1.0E10,-1.0E10,CONTINUOUS FALL OF SNOW FLAKES-MODERATE AT TIME OF OBSERVATION,6 OKTAS     7/10-8/10,towed CTD,0,0,197.6725,0,4,2.174,4,0,34.102,5,0,199.5,4\n"
              + "CANADA,CA019465,181C05632,10749307,51.2133,-53.85,86182.78125,20051217,18.75,2026318698,WILFRED TEMPLEMAN (CCGS;F/R/V;call sign CGDV; built 1981;IMO7907099),206.0,1.5 METER,5 SECONDS OR LESS,95 DEGREES - 104 DEGREES,19.8288,1032.0,-1.0E10,-1.0E10,CONTINUOUS FALL OF SNOW FLAKES-MODERATE AT TIME OF OBSERVATION,6 OKTAS     7/10-8/10,towed CTD,0,0,197.77153,0,4,2.175,4,0,34.102,5,0,199.6,4\n"
              + "CANADA,CA019465,181C05632,10749307,51.2133,-53.85,86182.78125,20051217,18.75,2026318698,WILFRED TEMPLEMAN (CCGS;F/R/V;call sign CGDV; built 1981;IMO7907099),206.0,1.5 METER,5 SECONDS OR LESS,95 DEGREES - 104 DEGREES,19.8288,1032.0,-1.0E10,-1.0E10,CONTINUOUS FALL OF SNOW FLAKES-MODERATE AT TIME OF OBSERVATION,6 OKTAS     7/10-8/10,towed CTD,0,0,197.87054,0,4,2.178,4,0,34.102,5,0,199.7,4\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    fullName = dir + "wod_uor_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"10749307"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10749307,86182.78125,0.0,0.751,0\n"
            + "10749307,86182.78125,0.099131815,0.776,0\n"
            + "10749307,86182.78125,0.19826359,0.751,0\n"
            + "10749307,86182.78125,0.29739532,0.777,0\n"
            + "10749307,86182.78125,0.49565855,0.775,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"10749307", "0.751"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10749307,86182.78125,0.0,0.751,0\n"
            + "10749307,86182.78125,0.19826359,0.751,0\n"
            + "10749307,86182.78125,2.4782808,0.751,0\n"
            + "10749307,86182.78125,4.0643644,0.751,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** read all xbt
    fullName = dir + "wod_xbt_2005.nc";
    if (doAll) {
      table.readInvalidCRA(
          fullName, null, 0, // standardizeWhat=0
          null, null, null);
      results = table.toString(5);
      expected =
          "{\n"
              + "dimensions:\n"
              + "\trow = 25164853 ;\n"
              + "\tcountry_strlen = 14 ;\n"
              + "\tWOD_cruise_identifier_strlen = 8 ;\n"
              + "\toriginators_cruise_identifier_strlen = 15 ;\n"
              + "\toriginators_station_identifier_strlen = 10 ;\n"
              + "\tProject_strlen = 59 ;\n"
              + "\tPlatform_strlen = 80 ;\n"
              + "\tInstitute_strlen = 79 ;\n"
              + "\tWater_Color_strlen = 37 ;\n"
              + "\tWave_Height_strlen = 9 ;\n"
              + "\tWave_Period_strlen = 17 ;\n"
              + "\tWind_Direction_strlen = 25 ;\n"
              + "\tWeather_Condition_strlen = 169 ;\n"
              + "\tCloud_Type_strlen = 17 ;\n"
              + "\tCloud_Cover_strlen = 65 ;\n"
              + "\tdataset_strlen = 3 ;\n"
              + "\tRecorder_strlen = 47 ;\n"
              + "\tdepth_eq_strlen = 9 ;\n"
              + "\tRef_Type_strlen = 6 ;\n"
              + "\tVisibility_strlen = 24 ;\n"
              + "\tneeds_z_fix_strlen = 39 ;\n"
              + "\treal_time_strlen = 14 ;\n"
              + "\tdbase_orig_strlen = 13 ;\n"
              + "\torigflagset_strlen = 4 ;\n"
              + "\tTemperature_Instrument_strlen = 34 ;\n"
              + "\tPrimary_Investigator_strlen = 28 ;\n"
              + "\tPrimary_Investigator_VAR_strlen = 13 ;\n"
              + "variables:\n"
              + "\tchar country(row, country_strlen) ;\n"
              + "\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n"
              + "\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n"
              + "\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n"
              + "\tchar originators_cruise_identifier(row, originators_cruise_identifier_strlen) ;\n"
              + "\tint wod_unique_cast(row) ;\n"
              + "\t\twod_unique_cast:cf_role = \"profile_id\" ;\n"
              + "\tchar originators_station_identifier(row, originators_station_identifier_strlen) ;\n"
              + "\t\toriginators_station_identifier:long_name = \"originators_station_identifier\" ;\n"
              + "\tfloat lat(row) ;\n"
              + "\t\tlat:long_name = \"latitude\" ;\n"
              + "\t\tlat:standard_name = \"latitude\" ;\n"
              + "\t\tlat:units = \"degrees_north\" ;\n"
              + "\tfloat lon(row) ;\n"
              + "\t\tlon:long_name = \"longitude\" ;\n"
              + "\t\tlon:standard_name = \"longitude\" ;\n"
              + "\t\tlon:units = \"degrees_east\" ;\n"
              + "\tdouble time(row) ;\n"
              + "\t\ttime:long_name = \"time\" ;\n"
              + "\t\ttime:standard_name = \"time\" ;\n"
              + "\t\ttime:units = \"days since 1770-01-01 00:00:00 UTC\" ;\n"
              + "\tint date(row) ;\n"
              + "\t\tdate:comment = \"YYYYMMDD\" ;\n"
              + "\t\tdate:long_name = \"date\" ;\n"
              + "\tfloat GMT_time(row) ;\n"
              + "\t\tGMT_time:long_name = \"GMT_time\" ;\n"
              + "\t\tGMT_time:units = \"hours\" ;\n"
              + "\tint Access_no(row) ;\n"
              + "\t\tAccess_no:_FillValue = -99999 ;\n"
              + "\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n"
              + "\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n"
              + "\t\tAccess_no:units_wod = \"NODC_code\" ;\n"
              + "\tchar Project(row, Project_strlen) ;\n"
              + "\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n"
              + "\t\tProject:long_name = \"Project_name\" ;\n"
              + "\tchar Platform(row, Platform_strlen) ;\n"
              + "\t\tPlatform:comment = \"name of platform from which measurements were taken\" ;\n"
              + "\t\tPlatform:long_name = \"Platform_name\" ;\n"
              + "\tchar Institute(row, Institute_strlen) ;\n"
              + "\t\tInstitute:comment = \"name of institute which collected data\" ;\n"
              + "\t\tInstitute:long_name = \"Responsible_institute\" ;\n"
              + "\tint Orig_Stat_Num(row) ;\n"
              + "\t\tOrig_Stat_Num:_FillValue = -99999 ;\n"
              + "\t\tOrig_Stat_Num:comment = \"number assigned to a given station by data originator\" ;\n"
              + "\t\tOrig_Stat_Num:long_name = \"Originators_Station_Number\" ;\n"
              + "\tfloat Bottom_Depth(row) ;\n"
              + "\t\tBottom_Depth:_FillValue = -1.0E10f ;\n"
              + "\t\tBottom_Depth:standard_name = \"sea_floor_depth_below_sea_surface\" ;\n"
              + "\t\tBottom_Depth:units = \"meters\" ;\n"
              + "\tchar Water_Color(row, Water_Color_strlen) ;\n"
              + "\t\tWater_Color:long_name = \"Water_Color\" ;\n"
              + "\t\tWater_Color:units_wod = \"Forel-Ule scale (00 to 21)\" ;\n"
              + "\tfloat Water_Transpar(row) ;\n"
              + "\t\tWater_Transpar:_FillValue = -1.0E10f ;\n"
              + "\t\tWater_Transpar:comment = \"Secchi disk depth\" ;\n"
              + "\t\tWater_Transpar:long_name = \"Water_Transparency\" ;\n"
              + "\t\tWater_Transpar:units = \"meters\" ;\n"
              + "\tchar Wave_Height(row, Wave_Height_strlen) ;\n"
              + "\t\tWave_Height:long_name = \"Wave_Height\" ;\n"
              + "\t\tWave_Height:units_wod = \"WMO 1555 or NODC 0104\" ;\n"
              + "\tchar Wave_Period(row, Wave_Period_strlen) ;\n"
              + "\t\tWave_Period:long_name = \"Wave_Period\" ;\n"
              + "\t\tWave_Period:units_wod = \"WMO 3155 or NODC 0378\" ;\n"
              + "\tchar Wind_Direction(row, Wind_Direction_strlen) ;\n"
              + "\t\tWind_Direction:long_name = \"Wind_Direction\" ;\n"
              + "\t\tWind_Direction:units_wod = \"WMO 0877 or NODC 0110\" ;\n"
              + "\tfloat Wind_Speed(row) ;\n"
              + "\t\tWind_Speed:_FillValue = -1.0E10f ;\n"
              + "\t\tWind_Speed:standard_name = \"wind_speed\" ;\n"
              + "\t\tWind_Speed:units = \"knots\" ;\n"
              + "\tfloat Barometric_Pres(row) ;\n"
              + "\t\tBarometric_Pres:_FillValue = -1.0E10f ;\n"
              + "\t\tBarometric_Pres:long_name = \"Barometric_Pressure\" ;\n"
              + "\t\tBarometric_Pres:units = \"millibars\" ;\n"
              + "\tfloat Dry_Bulb_Temp(row) ;\n"
              + "\t\tDry_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tDry_Bulb_Temp:long_name = \"Dry_Bulb_Air_Temperature\" ;\n"
              + "\t\tDry_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tfloat Wet_Bulb_Temp(row) ;\n"
              + "\t\tWet_Bulb_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tWet_Bulb_Temp:long_name = \"Wet_Bulb_Air_Temperature\" ;\n"
              + "\t\tWet_Bulb_Temp:units = \"degree_C\" ;\n"
              + "\tchar Weather_Condition(row, Weather_Condition_strlen) ;\n"
              + "\t\tWeather_Condition:comment = \"Weather conditions at time of measurements\" ;\n"
              + "\t\tWeather_Condition:long_name = \"Weather_Condition\" ;\n"
              + "\tchar Cloud_Type(row, Cloud_Type_strlen) ;\n"
              + "\t\tCloud_Type:long_name = \"Cloud_Type\" ;\n"
              + "\t\tCloud_Type:units_wod = \"WMO 0500 or NODC 0053\" ;\n"
              + "\tchar Cloud_Cover(row, Cloud_Cover_strlen) ;\n"
              + "\t\tCloud_Cover:long_name = \"Cloud_Cover\" ;\n"
              + "\t\tCloud_Cover:units_wod = \"WMO 2700 or NODC 0105\" ;\n"
              + "\tchar dataset(row, dataset_strlen) ;\n"
              + "\t\tdataset:long_name = \"WOD_dataset\" ;\n"
              + "\tchar Recorder(row, Recorder_strlen) ;\n"
              + "\t\tRecorder:comment = \"Device which recorded measurements\" ;\n"
              + "\t\tRecorder:long_name = \"Recorder\" ;\n"
              + "\t\tRecorder:units_wod = \"WMO code 4770\" ;\n"
              + "\tchar depth_eq(row, depth_eq_strlen) ;\n"
              + "\t\tdepth_eq:comment = \"which drop rate equation was used\" ;\n"
              + "\t\tdepth_eq:long_name = \"depth_equation_used\" ;\n"
              + "\tbyte Bottom_Hit(row) ;\n"
              + "\t\tBottom_Hit:_FillValue = -9 ;\n"
              + "\t\tBottom_Hit:comment = \"set to one if instrument hit bottom\" ;\n"
              + "\t\tBottom_Hit:long_name = \"Bottom_Hit\" ;\n"
              + "\tchar Ref_Type(row, Ref_Type_strlen) ;\n"
              + "\t\tRef_Type:comment = \"Instrument for reference temperature\" ;\n"
              + "\t\tRef_Type:long_name = \"Reference_Instrument\" ;\n"
              + "\tchar Visibility(row, Visibility_strlen) ;\n"
              + "\t\tVisibility:long_name = \"Horizontal_visibility\" ;\n"
              + "\t\tVisibility:units_wod = \"WMO Code 4300\" ;\n"
              + "\tfloat Ref_Surf_Temp(row) ;\n"
              + "\t\tRef_Surf_Temp:_FillValue = -1.0E10f ;\n"
              + "\t\tRef_Surf_Temp:comment = \"Reference_or_Sea_Surface_Temperature\" ;\n"
              + "\t\tRef_Surf_Temp:units = \"degree_C\" ;\n"
              + "\tchar needs_z_fix(row, needs_z_fix_strlen) ;\n"
              + "\t\tneeds_z_fix:comment = \"instruction for fixing depths\" ;\n"
              + "\t\tneeds_z_fix:long_name = \"z_fix_instructions\" ;\n"
              + "\t\tneeds_z_fix:units_wod = \"WOD_code\" ;\n"
              + "\tchar real_time(row, real_time_strlen) ;\n"
              + "\t\treal_time:comment = \"timeliness and quality status\" ;\n"
              + "\t\treal_time:long_name = \"real_time_data\" ;\n"
              + "\tfloat Launch_height(row) ;\n"
              + "\t\tLaunch_height:_FillValue = -1.0E10f ;\n"
              + "\t\tLaunch_height:long_name = \"Height_of_instrument_launch\" ;\n"
              + "\t\tLaunch_height:units = \"meters\" ;\n"
              + "\tfloat sensor_depth(row) ;\n"
              + "\t\tsensor_depth:_FillValue = -1.0E10f ;\n"
              + "\t\tsensor_depth:long_name = \"Depth_of_sea_surface_sensor\" ;\n"
              + "\t\tsensor_depth:units = \"meters\" ;\n"
              + "\tchar dbase_orig(row, dbase_orig_strlen) ;\n"
              + "\t\tdbase_orig:comment = \"Database from which data were extracted\" ;\n"
              + "\t\tdbase_orig:long_name = \"database_origin\" ;\n"
              + "\tchar origflagset(row, origflagset_strlen) ;\n"
              + "\t\torigflagset:comment = \"set of originators flag codes to use\" ;\n"
              + "\tbyte Temperature_WODprofileflag(row) ;\n"
              + "\t\tTemperature_WODprofileflag:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n"
              + "\t\tTemperature_WODprofileflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODprofileflag:long_name = \"WOD_profile_flag\" ;\n"
              + "\tchar Temperature_Instrument(row, Temperature_Instrument_strlen) ;\n"
              + "\t\tTemperature_Instrument:comment = \"Device used for measurement\" ;\n"
              + "\t\tTemperature_Instrument:long_name = \"Instrument\" ;\n"
              + "\tchar Primary_Investigator(row, Primary_Investigator_strlen) ;\n"
              + "\tchar Primary_Investigator_VAR(row, Primary_Investigator_VAR_strlen) ;\n"
              + "\tfloat z(row) ;\n"
              + "\t\tz:ancillary_variables = \"z_sigfigs z_WODflag z_origflag\" ;\n"
              + "\t\tz:long_name = \"depth_below_sea_surface\" ;\n"
              + "\t\tz:positive = \"down\" ;\n"
              + "\t\tz:standard_name = \"depth\" ;\n"
              + "\t\tz:units = \"m\" ;\n"
              + "\tbyte z_WODflag(row) ;\n"
              + "\t\tz_WODflag:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n"
              + "\t\tz_WODflag:flag_values = 0, 1, 2 ;\n"
              + "\t\tz_WODflag:long_name = \"WOD_depth_level_flag\" ;\n"
              + "\t\tz_WODflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_origflag(row) ;\n"
              + "\t\tz_origflag:_FillValue = -9 ;\n"
              + "\t\tz_origflag:comment = \"Originator flags are dependent on origflagset\" ;\n"
              + "\t\tz_origflag:standard_name = \"depth status_flag\" ;\n"
              + "\tbyte z_sigfigs(row) ;\n"
              + "\t\tz_sigfigs:long_name = \"depth significant figures   \" ;\n"
              + "\tfloat Temperature(row) ;\n"
              + "\t\tTemperature:_FillValue = -1.0E10f ;\n"
              + "\t\tTemperature:ancillary_variables = \"Temperature_sigfigs Temperature_WODflag Temperature_WODprofileflag Temperature_origflag\" ;\n"
              + "\t\tTemperature:coordinates = \"time lat lon z\" ;\n"
              + "\t\tTemperature:grid_mapping = \"crs\" ;\n"
              + "\t\tTemperature:long_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n"
              + "\t\tTemperature:units = \"degree_C\" ;\n"
              + "\tbyte Temperature_sigfigs(row) ;\n"
              + "\t\tTemperature_sigfigs:long_name = \"sea_water_temperature significant_figures\" ;\n"
              + "\tbyte Temperature_WODflag(row) ;\n"
              + "\t\tTemperature_WODflag:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n"
              + "\t\tTemperature_WODflag:flag_values = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ;\n"
              + "\t\tTemperature_WODflag:long_name = \"WOD_observation_flag\" ;\n"
              + "\t\tTemperature_WODflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\tbyte Temperature_origflag(row) ;\n"
              + "\t\tTemperature_origflag:_FillValue = -9 ;\n"
              + "\t\tTemperature_origflag:flag_definitions = \"flag definitions dependent on origflagset\" ;\n"
              + "\t\tTemperature_origflag:standard_name = \"sea_water_temperature status_flag\" ;\n"
              + "\n"
              + "// global attributes:\n"
              + "\t\t:cdm_data_type = \"Profile\" ;\n"
              + "\t\t:cdm_profile_variables = \"country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, originators_station_identifier, latitude, longitude, time, date, GMT_time, Access_no, Project, Platform, Institute, Orig_Stat_Num, Bottom_Depth, Water_Color, Water_Transpar, Wave_Height, Wave_Period, Wind_Direction, Wind_Speed, Barometric_Pres, Dry_Bulb_Temp, Wet_Bulb_Temp, Weather_Condition, Cloud_Type, Cloud_Cover, dataset, Recorder, depth_eq, Bottom_Hit, Ref_Type, Visibility, Ref_Surf_Temp, needs_z_fix, real_time, Launch_height, sensor_depth, dbase_orig, origflagset, Temperature_WODprofileflag, Temperature_Instrument, Primary_Investigator, Primary_Investigator_VAR\" ;\n"
              + "\t\t:Conventions = \"CF-1.6\" ;\n"
              + "\t\t:creator_email = \"OCLhelp@noaa.gov\" ;\n"
              + "\t\t:creator_name = \"Ocean Climate Lab/NCEI\" ;\n"
              + "\t\t:creator_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:crs_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:crs_grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:crs_inverse_flattening = 298.25723f ;\n"
              + "\t\t:crs_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:crs_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:date_created = \"2018-03-24\" ;\n"
              + "\t\t:date_modified = \"2018-03-24\" ;\n"
              + "\t\t:featureType = \"Profile\" ;\n"
              + "\t\t:geospatial_lat_max = 79.0612f ;\n"
              + "\t\t:geospatial_lat_min = -77.556f ;\n"
              + "\t\t:geospatial_lat_resolution = \"point\" ;\n"
              + "\t\t:geospatial_lon_max = -0.011505127f ;\n"
              + "\t\t:geospatial_lon_min = 180.0f ;\n"
              + "\t\t:geospatial_lon_resolution = \"point\" ;\n"
              + "\t\t:geospatial_vertical_max = 3124.0f ;\n"
              + "\t\t:geospatial_vertical_min = 0.0f ;\n"
              + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
              + "\t\t:geospatial_vertical_units = \"meters\" ;\n"
              + "\t\t:grid_mapping_epsg_code = \"EPSG:4326\" ;\n"
              + "\t\t:grid_mapping_inverse_flattening = 298.25723f ;\n"
              + "\t\t:grid_mapping_longitude_of_prime_meridian = 0.0f ;\n"
              + "\t\t:grid_mapping_name = \"latitude_longitude\" ;\n"
              + "\t\t:grid_mapping_semi_major_axis = 6378137.0f ;\n"
              + "\t\t:id = \"/nodc/data/oc5.clim.0/wod_update_nc/2005/wod_xbt_2005.nc\" ;\n"
              + "\t\t:institution = \"National Centers for Environmental Information (NCEI), NOAA\" ;\n"
              + "\t\t:naming_authority = \"gov.noaa.nodc\" ;\n"
              + "\t\t:project = \"World Ocean Database\" ;\n"
              + "\t\t:publisher_email = \"NODC.Services@noaa.gov\" ;\n"
              + "\t\t:publisher_name = \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION\" ;\n"
              + "\t\t:publisher_url = \"http://www.nodc.noaa.gov\" ;\n"
              + "\t\t:references = \"World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\" ;\n"
              + "\t\t:source = \"World Ocean Database\" ;\n"
              + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v41\" ;\n"
              + "\t\t:summary = \"Data for multiple casts from the World Ocean Database\" ;\n"
              + "\t\t:time_coverage_end = \"2005-12-31\" ;\n"
              + "\t\t:time_coverage_start = \"2005-01-01\" ;\n"
              + "\t\t:title = \"World Ocean Database - Multi-cast file\" ;\n"
              + "}\n"
              + "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,originators_station_identifier,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Orig_Stat_Num,Bottom_Depth,Water_Color,Water_Transpar,Wave_Height,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Type,Cloud_Cover,dataset,Recorder,depth_eq,Bottom_Hit,Ref_Type,Visibility,Ref_Surf_Temp,needs_z_fix,real_time,Launch_height,sensor_depth,dbase_orig,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag\n"
              + "UNITED STATES,US155628,WCY7054,10216693,,57.51,-147.63,85832.02569444478,20050101,0.6166667,1959,,HMI CAPE LOOKOUT SHOALS (Call Sign WCY7054),,2477204,-1.0E10,,-1.0E10,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,XBT,SIPPICAN MK-12,original,-9,,,-1.0E10,needs Hanawa et al.; 1994 applied (XBT),real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: DEEP BLUE (SIPPICAN),,,1.0,0,-9,2,5.0,4,0,-9\n"
              + "UNITED STATES,US155628,WCY7054,10216693,,57.51,-147.63,85832.02569444478,20050101,0.6166667,1959,,HMI CAPE LOOKOUT SHOALS (Call Sign WCY7054),,2477204,-1.0E10,,-1.0E10,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,XBT,SIPPICAN MK-12,original,-9,,,-1.0E10,needs Hanawa et al.; 1994 applied (XBT),real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: DEEP BLUE (SIPPICAN),,,5.0,0,-9,2,5.0,4,0,-9\n"
              + "UNITED STATES,US155628,WCY7054,10216693,,57.51,-147.63,85832.02569444478,20050101,0.6166667,1959,,HMI CAPE LOOKOUT SHOALS (Call Sign WCY7054),,2477204,-1.0E10,,-1.0E10,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,XBT,SIPPICAN MK-12,original,-9,,,-1.0E10,needs Hanawa et al.; 1994 applied (XBT),real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: DEEP BLUE (SIPPICAN),,,73.0,0,-9,3,5.1,4,0,-9\n"
              + "UNITED STATES,US155628,WCY7054,10216693,,57.51,-147.63,85832.02569444478,20050101,0.6166667,1959,,HMI CAPE LOOKOUT SHOALS (Call Sign WCY7054),,2477204,-1.0E10,,-1.0E10,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,XBT,SIPPICAN MK-12,original,-9,,,-1.0E10,needs Hanawa et al.; 1994 applied (XBT),real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: DEEP BLUE (SIPPICAN),,,77.0,0,-9,3,4.9,4,0,-9\n"
              + "UNITED STATES,US155628,WCY7054,10216693,,57.51,-147.63,85832.02569444478,20050101,0.6166667,1959,,HMI CAPE LOOKOUT SHOALS (Call Sign WCY7054),,2477204,-1.0E10,,-1.0E10,,,,-1.0E10,-1.0E10,-1.0E10,-1.0E10,,,,XBT,SIPPICAN MK-12,original,-9,,,-1.0E10,needs Hanawa et al.; 1994 applied (XBT),real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: DEEP BLUE (SIPPICAN),,,89.0,0,-9,3,5.0,4,0,-9\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      table.removeRows(0, 25164853 - 5);
      results = table.dataToString();
      expected =
          "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,originators_station_identifier,lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Orig_Stat_Num,Bottom_Depth,Water_Color,Water_Transpar,Wave_Height,Wave_Period,Wind_Direction,Wind_Speed,Barometric_Pres,Dry_Bulb_Temp,Wet_Bulb_Temp,Weather_Condition,Cloud_Type,Cloud_Cover,dataset,Recorder,depth_eq,Bottom_Hit,Ref_Type,Visibility,Ref_Surf_Temp,needs_z_fix,real_time,Launch_height,sensor_depth,dbase_orig,origflagset,Temperature_WODprofileflag,Temperature_Instrument,Primary_Investigator,Primary_Investigator_VAR,z,z_WODflag,z_origflag,z_sigfigs,Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag\n"
              + "UNKNOWN,,,10731585,,36.55,13.6667,86196.0,20051231,9.96921E36,2516,,,,3028589,-1.0E10,,-1.0E10,,,165 DEGREES - 174 DEGREES,27.216,-1.0E10,-1.0E10,-1.0E10,,,,XBT,,,-9,,,-1.0E10,insufficient information,real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: TYPE UNKNOWN,,,88.0,0,-9,3,15.3,5,0,-9\n"
              + "UNKNOWN,,,10731585,,36.55,13.6667,86196.0,20051231,9.96921E36,2516,,,,3028589,-1.0E10,,-1.0E10,,,165 DEGREES - 174 DEGREES,27.216,-1.0E10,-1.0E10,-1.0E10,,,,XBT,,,-9,,,-1.0E10,insufficient information,real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: TYPE UNKNOWN,,,98.0,0,-9,3,15.1,5,0,-9\n"
              + "UNKNOWN,,,10731585,,36.55,13.6667,86196.0,20051231,9.96921E36,2516,,,,3028589,-1.0E10,,-1.0E10,,,165 DEGREES - 174 DEGREES,27.216,-1.0E10,-1.0E10,-1.0E10,,,,XBT,,,-9,,,-1.0E10,insufficient information,real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: TYPE UNKNOWN,,,102.0,0,-9,4,14.7,5,0,-9\n"
              + "UNKNOWN,,,10731585,,36.55,13.6667,86196.0,20051231,9.96921E36,2516,,,,3028589,-1.0E10,,-1.0E10,,,165 DEGREES - 174 DEGREES,27.216,-1.0E10,-1.0E10,-1.0E10,,,,XBT,,,-9,,,-1.0E10,insufficient information,real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: TYPE UNKNOWN,,,167.0,0,-9,4,14.7,5,0,-9\n"
              + "UNKNOWN,,,10731585,,36.55,13.6667,86196.0,20051231,9.96921E36,2516,,,,3028589,-1.0E10,,-1.0E10,,,165 DEGREES - 174 DEGREES,27.216,-1.0E10,-1.0E10,-1.0E10,,,,XBT,,,-9,,,-1.0E10,insufficient information,real-time data,-1.0E10,-1.0E10,GTSP Project,,0,XBT: TYPE UNKNOWN,,,200.0,0,-9,4,14.5,5,0,-9\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // read outer and inner col with outer constraint
    fullName = dir + "wod_xbt_2005.nc";
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast"}),
        new StringArray(new String[] {"="}),
        new StringArray(new String[] {"10731585"}));
    results = table.dataToString(5);
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10731585,86196.0,0.0,15.8,0\n"
            + "10731585,86196.0,23.0,15.7,0\n"
            + "10731585,86196.0,47.0,15.3,0\n"
            + "10731585,86196.0,50.0,15.4,0\n"
            + "10731585,86196.0,65.0,15.2,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // outer and inner constraint
    table.readInvalidCRA(
        fullName,
        new StringArray(
            new String[] {"wod_unique_cast", "time", "z", "Temperature", "Temperature_WODflag"}),
        0, // standardizeWhat=0
        new StringArray(new String[] {"wod_unique_cast", "Temperature"}),
        new StringArray(new String[] {"=", "="}),
        new StringArray(new String[] {"10731585", "15.3"}));
    results = table.dataToString();
    expected =
        "wod_unique_cast,time,z,Temperature,Temperature_WODflag\n"
            + "10731585,86196.0,47.0,15.3,0\n"
            + "10731585,86196.0,88.0,15.3,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    /* */

    table.debugMode = false;
  }

  /** This tests reading an ncCF Contiguous Ragged Array file with 7(!) sample_dimension's. */
  @org.junit.jupiter.api.Test
  void testReadNcCF7SampleDims() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCF7SampleDims");
    Table table = new Table();
    String results, expected;
    // From Ajay Krishnan, NCEI/NODC, from
    // https://data.nodc.noaa.gov/thredds/catalog/testdata/wod_ragged/05052016/catalog.html?dataset=testdata/wod_ragged/05052016/ind199105_ctd.nc
    String fileName = TableTests.class.getResource("/data/nccf/ncei/ind199105_ctd.nc").getPath();
    Attributes gatts;
    String scalarVars = ",crs,WODf,WODfd";

    // String2.log("\n\n** Testing " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    // String2.pressEnterToContinue(NcHelper.ncdump(fileName, "-v crs;WODf;WODfd"));
    // String2.pressEnterToContinue(NcHelper.ncdump(fileName,
    // "-v
    // Temperature_row_size;Salinity_row_size;Oxygen_row_size;Pressure_row_size;Chlorophyll_row_size"));

    // test reading just specified legit "z_obs" outer and inner variables
    table.readNcCF(
        fileName,
        StringArray.fromCSV("zztop,wod_unique_cast,lat,lon,time,z,z_WODflag" + scalarVars),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected =
        // with netcdf-java 4.6.5 and before, the last 3 vars had 0's.
        // with netcdf-java 4.6.6 and after, they are the default cf missing values
        // Unidata says it is not a bug. 4.6.6 is correct. see email from Sean Arms June
        // 15, 2016
        // I notified source of files: Ajay Krisnan, but he never replied.
        // so I'm going with 4.6.6 and odd values
        // was
        // "wod_unique_cast,lat,lon,time,z,z_WODflag,crs,WODf,WODfd\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,2.9759612,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,3.967939,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,5.9518795,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,7.9358006,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,9.919703,0,0,0,0\n" +
        // "...\n";
        "wod_unique_cast,lat,lon,time,z,z_WODflag,crs,WODf,WODfd\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,2.9759612,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,3.967939,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,5.9518795,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,7.9358006,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,9.919703,0,-2147483647,-32767,-32767\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test reading just specified legit "Temperature_obs" outer and inner variables
    table.readNcCF(
        fileName,
        StringArray.fromCSV(
            "zztop,wod_unique_cast,lat,lon,time,Temperature,Temperature_WODflag" + scalarVars),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected =
        // was
        // "wod_unique_cast,lat,lon,time,Temperature,Temperature_WODflag,crs,WODf,WODfd\n"
        // +
        // "3390296,-43.7802,67.3953,80838.31180554628,14.519,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,14.526,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,14.537,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,14.533,0,0,0,0\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,14.532,0,0,0,0\n" +
        // "...\n";
        "wod_unique_cast,lat,lon,time,Temperature,Temperature_WODflag,crs,WODf,WODfd\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,14.519,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,14.526,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,14.537,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,14.533,0,-2147483647,-32767,-32767\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,14.532,0,-2147483647,-32767,-32767\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test reading just specified legit outer variables
    table.readNcCF(
        fileName,
        StringArray.fromCSV("zztop,wod_unique_cast,lat,lon,time" + scalarVars),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected =
        // was
        // "wod_unique_cast,lat,lon,time,crs,WODf,WODfd\n" +
        // "3390296,-43.7802,67.3953,80838.31180554628,0,0,0\n" +
        // "3390301,-44.5177,67.9403,80838.68055558205,0,0,0\n" +
        // "3390310,-45.2592,68.3755,80839.08888889104,0,0,0\n" +
        // "3390318,-46.0113,68.7568,80839.39652776718,0,0,0\n" +
        // "3390328,-47.0115,69.431,80839.87291669846,0,0,0\n" +
        // "...\n";
        "wod_unique_cast,lat,lon,time,crs,WODf,WODfd\n"
            + "3390296,-43.7802,67.3953,80838.31180554628,-2147483647,-32767,-32767\n"
            + "3390301,-44.5177,67.9403,80838.68055558205,-2147483647,-32767,-32767\n"
            + "3390310,-45.2592,68.3755,80839.08888889104,-2147483647,-32767,-32767\n"
            + "3390318,-46.0113,68.7568,80839.39652776718,-2147483647,-32767,-32767\n"
            + "3390328,-47.0115,69.431,80839.87291669846,-2147483647,-32767,-32767\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test reading just specified legit "Temperature_obs" inner variables
    table.readNcCF(
        fileName,
        StringArray.fromCSV("zztop,Temperature,Temperature_WODflag" + scalarVars),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected =
        // was
        // "Temperature,Temperature_WODflag,crs,WODf,WODfd\n" +
        // "14.519,0,0,0,0\n" +
        // "14.526,0,0,0,0\n" +
        // "14.537,0,0,0,0\n" +
        // "14.533,0,0,0,0\n" +
        // "14.532,0,0,0,0\n" +
        // "...\n";
        "Temperature,Temperature_WODflag,crs,WODf,WODfd\n"
            + "14.519,0,-2147483647,-32767,-32767\n"
            + "14.526,0,-2147483647,-32767,-32767\n"
            + "14.537,0,-2147483647,-32767,-32767\n"
            + "14.533,0,-2147483647,-32767,-32767\n"
            + "14.532,0,-2147483647,-32767,-32767\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test reading just specified legit scalar variables
    table.readNcCF(
        fileName,
        StringArray.fromCSV("zztop,WODf,crs"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected =
        // was
        // "WODf,crs\n" +
        // "0,0\n";
        "WODf,crs\n" + "-32767,-2147483647\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test reading WHOLE file (should just catch z_obs dimension)
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(3);
    expected =
        // note it catches z_obs dimension (z, z_WODflag, z_sigfig), not others.
        // Temperature_row_size, and Temperature_WODprofileflag are [casts], and don't
        // use Temperature_obs.
        "country,WOD_cruise_identifier,originators_cruise_identifier,wod_unique_cast,"
            + "lat,lon,time,date,GMT_time,Access_no,Project,Platform,Institute,Cast_Tow_number,"
            + "Orig_Stat_Num,Bottom_Depth,Cast_Duration,Cast_Direction,High_res_pair,dataset,"
            + "dbase_orig,origflagset,z,z_WODflag,z_sigfig,Temperature_row_size,"
            + "Temperature_WODprofileflag,Temperature_Scale,Temperature_Instrument,"
            + "Salinity_row_size,Salinity_WODprofileflag,Salinity_Scale,Salinity_Instrument,"
            + "Oxygen_row_size,Oxygen_WODprofileflag,Oxygen_Instrument,Oxygen_Original_units,"
            + "Pressure_row_size,Chlorophyll_row_size,Chlorophyll_WODprofileflag,"
            + "Chlorophyll_Instrument,Chlorophyll_uncalibrated,Conductivit_row_size,crs,WODf,WODfp,WODfd\n"
            + "FRANCE,FR008787,35MF68SUZIL,3390296,-43.7802,67.3953,80838.31180554628,"
            + "19910501,7.483333,841,WORLD OCEAN CIRCULATION EXPERIMENT (WOCE),"
            + "MARION DUFRESNE (C.s.FNGB;built 1972;decomm-d 1995;renamed Fres;IMO7208388),"
            + "NATIONAL MUSEUM OF NATURAL HISTORY (PARIS),1,37.0,4438.0,9.96921E36,,7498735,"
            + "CTD,WHO/CCHDO,WOCE,2.9759612,0,3,2204,0,,CTD: NEIL BROWN MARK IIIB,2204,0,,"
            + "CTD: NEIL BROWN MARK IIIB,2204,5,CTD: NEIL BROWN MARK IIIB,umol/kg,2204,0,0,,"
            +
            // was "-2147483647,0,0,0,0,0\n" +
            "-2147483647,0,-2147483647,-32767,-32767,-32767\n"
            + "FRANCE,FR008787,35MF68SUZIL,3390296,-43.7802,67.3953,80838.31180554628,"
            + "19910501,7.483333,841,WORLD OCEAN CIRCULATION EXPERIMENT (WOCE),"
            + "MARION DUFRESNE (C.s.FNGB;built 1972;decomm-d 1995;renamed Fres;IMO7208388),"
            + "NATIONAL MUSEUM OF NATURAL HISTORY (PARIS),1,37.0,4438.0,9.96921E36,,7498735,"
            + "CTD,WHO/CCHDO,WOCE,3.967939,0,3,2204,0,,CTD: NEIL BROWN MARK IIIB,2204,0,,"
            + "CTD: NEIL BROWN MARK IIIB,2204,5,CTD: NEIL BROWN MARK IIIB,umol/kg,2204,0,0,,"
            +
            // was "-2147483647,0,0,0,0,0\n" +
            "-2147483647,0,-2147483647,-32767,-32767,-32767\n"
            + "FRANCE,FR008787,35MF68SUZIL,3390296,-43.7802,67.3953,80838.31180554628,"
            + "19910501,7.483333,841,WORLD OCEAN CIRCULATION EXPERIMENT (WOCE),"
            + "MARION DUFRESNE (C.s.FNGB;built 1972;decomm-d 1995;renamed Fres;IMO7208388),"
            + "NATIONAL MUSEUM OF NATURAL HISTORY (PARIS),1,37.0,4438.0,9.96921E36,,7498735,"
            + "CTD,WHO/CCHDO,WOCE,5.9518795,0,3,2204,0,,CTD: NEIL BROWN MARK IIIB,2204,0,,"
            + "CTD: NEIL BROWN MARK IIIB,2204,5,CTD: NEIL BROWN MARK IIIB,umol/kg,2204,0,0,,"
            +
            // was "-2147483647,0,0,0,0,0\n" +
            "-2147483647,0,-2147483647,-32767,-32767,-32767\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.globalAttributes().getString("cdm_data_type");
    expected = "Profile";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.globalAttributes().getString("subsetVariables");
    expected =
        "country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, "
            + "lat, lon, time, date, GMT_time, Access_no, Project, Platform, Institute, "
            + "Cast_Tow_number, Orig_Stat_Num, Bottom_Depth, Cast_Duration, Cast_Direction, "
            + "High_res_pair, dataset, dbase_orig, origflagset, Temperature_row_size, "
            + "Temperature_WODprofileflag, Temperature_Scale, Temperature_Instrument, "
            + "Salinity_row_size, Salinity_WODprofileflag, Salinity_Scale, Salinity_Instrument, "
            + "Oxygen_row_size, Oxygen_WODprofileflag, Oxygen_Instrument, Oxygen_Original_units, "
            + "Pressure_row_size, "
            + "Chlorophyll_row_size, Chlorophyll_WODprofileflag, Chlorophyll_Instrument, "
            + "Chlorophyll_uncalibrated, Conductivit_row_size, crs, WODf, WODfp, WODfd";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.globalAttributes().getString("cdm_profile_variables");
    // same expected
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test reading row_size vars -- are they actually read?
    table.readNcCF(
        fileName,
        StringArray.fromCSV(
            "zztop,z_row_size,Temperature_row_size,Salinity_row_size,Oxygen_row_size,"
                + "Pressure_row_size,Chlorophyll_row_size,Conductivity_row_size"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(7);
    expected = // verified with dumpString above
        "Temperature_row_size,Salinity_row_size,Oxygen_row_size,Pressure_row_size,Chlorophyll_row_size\n"
            + "2204,2204,2204,2204,0\n"
            + "1844,1844,1844,1844,0\n"
            + "1684,1684,1684,1684,0\n"
            + "1587,1587,1587,1587,0\n"
            + "357,357,357,357,0\n"
            + "34,34,0,34,34\n"
            + "34,34,0,34,34\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test request for vars with 2 different sample_dimensions
    // second sample_dim throws error
    results = "shouldn't happen";
    try {
      table.readNcCF(
          fileName,
          StringArray.fromCSV(
              "zztop,wod_unique_cast,lat,lon,time,z,z_WODflag,Temperature,Temperature_WODflag"
                  + scalarVars),
          0, // standardizeWhat=0
          null,
          null,
          null);
      results = table.dataToString(5);

    } catch (Throwable t2) {
      results = t2.toString();
    }
    expected =
        "Invalid request: loadVariables includes variables that use two different sample_dimension's (z_obs and Temperature_obs).";
    int po = Math.max(0, results.indexOf(expected));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);
    // Table.debugMode = oDebug;
  }

  /** This tests readNcCF reading point files. */
  @org.junit.jupiter.api.Test
  void testReadNcCFPoint() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCFPoint");
    Table table = new Table();
    String results, expected;
    String fileName =
        TableTests.class
            .getResource("/data/CFPointConventions/point/point-H.1/point-H.1.nc")
            .getPath();
    Attributes gatts;

    /* */
    // *************** point
    // String2.log("\n\n** Testing " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));

    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    // String2.log(table.toCSVString());
    results = table.dataToString(5);
    expected =
        "obs,lat,lon,alt,time,temperature,humidity\n"
            + "0,41.0,112.0,7.745540487338979,573,26.225288,11.245576\n"
            + "1,179.0,68.0,3.0855444414144264,2248,12.695349,67.73824\n"
            + "2,10.0,11.0,3.254759157455159,71,21.193731,48.589462\n"
            + "3,106.0,22.0,4.549437636401848,1714,35.339344,39.594116\n"
            + "4,75.0,16.0,6.061720687265453,1209,22.593496,28.170149\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    table.readNcCF(
        fileName,
        StringArray.fromCSV("row,obs,lat,lon,alt,time,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV(""),
        StringArray.fromCSV(""),
        StringArray.fromCSV(""));
    results = table.dataToString(5);
    // expected is same
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.columnAttributes(6).toString();
    expected =
        "    coordinates=time lat lon alt\n"
            + "    long_name=Humidity\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=specific_humidity\n"
            + "    units=Percent\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.globalAttributes().toString();
    expected = "    Conventions=CF-1.6\n" + "    featureType=point\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs,lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("obs"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("2"));
    results = table.dataToString();
    expected = "obs,lat,time,temperature\n" + "2,10.0,71,21.193731\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs,lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("71"));
    results = table.dataToString();
    expected = "obs,lat,time,temperature\n" + "2,10.0,71,21.193731\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs,lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("21.193731"));
    results = table.dataToString();
    expected = "obs,lat,time,temperature\n" + "2,10.0,71,21.193731\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // this is important test of just getting dimension values
    // (when there is no corresponding variable)
    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs"),
        0, // standardizeWhat=0
        StringArray.fromCSV("obs"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("2"));
    results = table.dataToString();
    expected = "obs\n" + "2\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // and test that it gets the global attributes from the file.
    results = table.globalAttributes().toString();
    expected = "    Conventions=CF-1.6\n" + "    featureType=point\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("21.193731"));
    results = table.dataToString();
    expected = "temperature\n" + "21.193731\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs,lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("obs"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs,lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs,lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("obs"),
        0, // standardizeWhat=0
        StringArray.fromCSV("obs"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");

    // String2.log("\n*** Table.testReadNcCFPoint finished successfully");
    // Table.debugMode = oDebug;
  }

  /** This tests readNcCF nLevels=1. */
  @org.junit.jupiter.api.Test
  void testReadNcCF1() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCF1");
    Table table = new Table();
    String results, expected;
    String profileFileName = TableTests.class.getResource("/data/nccf/Profile.nc").getPath();
    Attributes gatts;

    /* */
    // *************** contiguous profileFileName
    // String2.log("\n\n** Testing profile contiguousRagged\n" +
    // " " + profileFileName);
    // String2.log(NcHelper.ncdump(profileFileName, "-h"));

    // String2.log("\n\n** Test nLevels=1/contiguousRagged no loadVars, no
    // constraints");
    table.readNcCF(
        profileFileName,
        null,
        0, // standardizeWhat=0
        null,
        null,
        null);
    // String2.log(table.toCSVString());
    results = table.dataToString(5);
    expected =
        "id,longitude,latitude,time,altitude,chlorophyll,chlorophyll_qc,oxygen,oxygen_qc,pressure,pressure_qc,salinity,salinity_qc,temperature,temperature_qc\n"
            + "465958,163.08,39.0,1.107754559E9,-2.0,,,,,,,,,10.1,0.0\n"
            + "465958,163.08,39.0,1.107754559E9,-58.0,,,,,,,,,9.9,0.0\n"
            + "465958,163.08,39.0,1.107754559E9,-96.0,,,,,,,,,9.2,0.0\n"
            + "465958,163.08,39.0,1.107754559E9,-138.0,,,,,,,,,8.8,0.0\n"
            + "465958,163.08,39.0,1.107754559E9,-158.0,,,,,,,,,8.1,0.0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "");
    Test.ensureEqual(table.nRows(), 118, table.toString());
    results = table.columnAttributes(0).toString();
    expected =
        "    actual_range=465958i,848984i\n"
            + "    cf_role=profile_id\n"
            + "    colorBarMaximum=1000000.0d\n"
            + "    colorBarMinimum=0.0d\n"
            + "    ioos_category=Identifier\n"
            + "    long_name=Sequence ID\n"
            + "    missing_value=2147483647i\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    gatts = table.globalAttributes();
    Test.ensureEqual(gatts.getString("cdm_data_type"), "Profile", gatts.toString());
    Test.ensureEqual(
        gatts.getString("cdm_profile_variables"),
        "id, longitude, latitude, time",
        gatts.toString());
    Test.ensureEqual(
        gatts.getString("subsetVariables"), "id, longitude, latitude, time", gatts.toString());

    //
    // String2.log("\n\n** Test 1 non-existent loadVar test:ncCFcc.set(27)");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV(""),
        StringArray.fromCSV(""),
        StringArray.fromCSV(""));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    // String2.log("\n\n** Test nLevels=1/contiguousRagged " +
    // "many loadVars, constraints, NO_DATA");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("id,longitude,latitude,time,altitude,salinity,salinity_qc,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("id"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("zztop"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("id,longitude,latitude,time,altitude,salinity,salinity_qc,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("id"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("zztop"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("id,longitude,latitude,time,altitude,salinity,salinity_qc,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("longitude"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("id,longitude,latitude,time,altitude,salinity,salinity_qc,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("id,longitude,latitude,time,altitude,salinity,salinity_qc,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    // String2.log("\n\n** Test nLevels=1/contiguousRagged " +
    // "just outerTable loadVars, no constraints");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,zztop,id"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "longitude,latitude,time,id\n"
            + "163.08,39.0,1.107754559E9,465958\n"
            + "225.84,16.05,1.1077371E9,580888\n"
            + "225.4,15.11,1.107751391E9,580889\n"
            + "225.0,14.25,1.107764388E9,580890\n"
            + "224.49,13.23,1.10778048E9,580891\n"
            + "214.66,54.8,1.107759959E9,848984\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    gatts = table.globalAttributes();
    Test.ensureEqual(gatts.getString("cdm_data_type"), "Profile", gatts.toString());
    Test.ensureEqual(
        gatts.getString("cdm_profile_variables"),
        "id, longitude, latitude, time",
        gatts.toString());
    Test.ensureEqual(
        gatts.getString("subsetVariables"), "id, longitude, latitude, time", gatts.toString());

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,zztop,id"),
        0, // standardizeWhat=0
        StringArray.fromCSV("id"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,zztop,id"),
        0, // standardizeWhat=0
        StringArray.fromCSV("latitude"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,zztop,id"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    // String2.log("\n\n** Test nLevels=1/contiguousRagged " +
    // "just outerTable loadVars, constraints");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,zztop,id"),
        0, // standardizeWhat=0
        StringArray.fromCSV("id"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("848984"));
    results = table.dataToString();
    expected = "longitude,latitude,time,id\n" + "214.66,54.8,1.107759959E9,848984\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    gatts = table.globalAttributes();
    Test.ensureEqual(gatts.getString("cdm_data_type"), "Profile", gatts.toString());
    Test.ensureEqual(
        gatts.getString("cdm_profile_variables"),
        "id, longitude, latitude, time",
        gatts.toString());
    Test.ensureEqual(
        gatts.getString("subsetVariables"), "id, longitude, latitude, time", gatts.toString());

    // String2.log("\n\n** Test nLevels=1/contiguousRagged " +
    // "just outerTable loadVars, constraints, NO_DATA");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,zztop,id"),
        0, // standardizeWhat=0
        StringArray.fromCSV("id"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("zztop"));
    Test.ensureEqual(table.nColumns(), 0, "");
    Test.ensureEqual(table.nRows(), 0, "");

    // String2.log("\n\n** Test nLevels=1/contiguousRagged, specific loadVars,
    // constraints");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("longitude,latitude,time,altitude,temperature,temperature_qc,zztop,id"),
        0, // standardizeWhat=0
        StringArray.fromCSV("id,temperature"),
        StringArray.fromCSV("=,>="),
        StringArray.fromCSV("848984,5"));
    results = table.dataToString();
    expected =
        "longitude,latitude,time,altitude,temperature,temperature_qc,id\n"
            + "214.66,54.8,1.107759959E9,-2.0,5.8,0.0,848984\n"
            + "214.66,54.8,1.107759959E9,-95.0,5.6,0.0,848984\n"
            + "214.66,54.8,1.107759959E9,-101.0,5.4,0.0,848984\n"
            + "214.66,54.8,1.107759959E9,-107.0,5.0,0.0,848984\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    gatts = table.globalAttributes();
    Test.ensureEqual(gatts.getString("cdm_data_type"), "Profile", gatts.toString());
    Test.ensureEqual(
        gatts.getString("cdm_profile_variables"),
        "id, longitude, latitude, time",
        gatts.toString());
    Test.ensureEqual(
        gatts.getString("subsetVariables"), "id, longitude, latitude, time", gatts.toString());

    // String2.log("\n\n** Test nLevels=1/contiguousRagged just obs loadVars, no
    // constraints");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("salinity,temperature,zztop"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "salinity,temperature\n"
            + ",10.1\n"
            + ",9.9\n"
            + ",9.2\n"
            + ",8.8\n"
            + ",8.1\n"
            + ",8.1\n"
            + ",7.4\n"
            + ",6.7\n"
            + ",6.0\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 118, table.dataToString());
    gatts = table.globalAttributes();
    Test.ensureEqual(gatts.getString("cdm_data_type"), "Profile", gatts.toString());
    Test.ensureEqual(gatts.getString("cdm_profile_variables"), null, gatts.toString());
    Test.ensureEqual(gatts.getString("subsetVariables"), null, gatts.toString());

    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("salinity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("salinity"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    // String2.log("\n\n** Test nLevels=1/contiguousRagged just obs loadVars,
    // constraints");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">"),
        StringArray.fromCSV("24.5"));
    results = table.dataToString();
    expected = "temperature\n" + "24.8\n" + "24.7\n" + "25.0\n" + "24.9\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    gatts = table.globalAttributes();
    Test.ensureEqual(gatts.getString("cdm_data_type"), "Profile", gatts.toString());
    Test.ensureEqual(gatts.getString("cdm_profile_variables"), null, gatts.toString());
    Test.ensureEqual(gatts.getString("subsetVariables"), null, gatts.toString());

    // String2.log("\n\n** Test nLevels=1/contiguousRagged " +
    // "just obs loadVars, constraints, NO_DATA");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-195"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // test quick reject
    // if (constraintVar!=NaN or constraintVar=(finite)) and var not in file,
    // String2.log("\n\n** Test #1m quick reject var=(nonNumber) if var not in file:
    // NO_DATA");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("temperature,zzStation"),
        0, // standardizeWhat=0
        StringArray.fromCSV("zzStation"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("WXURP"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // String2.log("\n\n** Test #1n quick reject var!=\"\" if var not in file:
    // NO_DATA");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("temperature,zzStation"),
        0, // standardizeWhat=0
        StringArray.fromCSV("zzStation"),
        StringArray.fromCSV("!="),
        new StringArray(new String[] {""}));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // but this can't be quickly rejected
    // String2.log("\n\n** Test #1o can't quick reject var=99 if var not in file\n"
    // +
    // "(since 99 might be defined as missing_value)");
    table.readNcCF(
        profileFileName,
        StringArray.fromCSV("temperature,zzStation"),
        0, // standardizeWhat=0
        StringArray.fromCSV("zzStation"),
        StringArray.fromCSV("="),
        new StringArray(new String[] {"99"}));
    Test.ensureEqual(table.nRows(), 118, "");
    Test.ensureEqual(table.nColumns(), 1, "");

    /* */
    // *************** nLevels=1 TimeSeries
    for (int type = 0; type < 2; type++) {
      // ncCF1b and ncCFMA1b have same data, so tests are the same!
      String fileType = type == 0 ? "contiguous" : "multidimensional";
      // from EDDTableFromNcFiles.testNcCF1b() and testNcCFMA1b();
      String fileName =
          TableTests.class
              .getResource("/data/nccf/" + (type == 0 ? "ncCF1b.nc" : "ncCFMA1b.nc"))
              .getPath();

      // String2.log("\n\n** Testing nLevels=1/" + fileType + "\n" +
      // " " + fileName);
      // String2.log(NcHelper.ncdump(fileName, "-h"));

      // String2.log("\n\n** Test nLevels=1/" + fileType + " no loadVars, no
      // constraints");
      table.readNcCF(
          fileName, null, 0, // standardizeWhat=0
          null, null, null);
      // String2.log(table.toString());
      results = table.dataToString(5);
      expected =
          "line_station,longitude,latitude,altitude,time,obsScientific,obsValue,obsUnits\n"
              + "076.7_100,-124.32333,33.388332,-214.1,1.10064E9,Argyropelecus sladeni,2,number of larvae\n"
              + "076.7_100,-124.32333,33.388332,-214.1,1.10064E9,Chauliodus macouni,3,number of larvae\n"
              + "076.7_100,-124.32333,33.388332,-214.1,1.10064E9,Danaphos oculatus,4,number of larvae\n"
              + "076.7_100,-124.32333,33.388332,-214.1,1.10064E9,Diogenichthys atlanticus,3,number of larvae\n"
              + "076.7_100,-124.32333,33.388332,-214.1,1.10064E9,Idiacanthus antrostomus,3,number of larvae\n"
              + "...\n";
      Test.ensureEqual(results, expected, "");
      Test.ensureEqual(table.nRows(), 23, table.toString());
      results = table.columnAttributes(0).toString();
      expected =
          "    cf_role=timeseries_id\n"
              + "    ioos_category=Identifier\n"
              + "    long_name=CalCOFI Line + Station\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(gatts.getString("cdm_data_type"), "TimeSeries", gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_timeseries_variables"), "line_station", gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), "line_station", gatts.toString());

      table.readNcCF(
          fileName,
          StringArray.fromCSV(""),
          0, // standardizeWhat=0
          StringArray.fromCSV("line_station"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV(""),
          0, // standardizeWhat=0
          StringArray.fromCSV("longitude"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV(""),
          0, // standardizeWhat=0
          StringArray.fromCSV("time"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV(""),
          0, // standardizeWhat=0
          StringArray.fromCSV("obsValue"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      // String2.log("\n\n** Test nLevels=1/" + fileType + " " +
      // "just outerTable loadVars, no constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("line_station,zztop"),
          0, // standardizeWhat=0
          null,
          null,
          null);
      results = table.dataToString();
      expected =
          "line_station\n"
              + "076.7_100\n"
              + "080_100\n"
              + "083.3_100\n"; // 4 row with all mv was removed
      Test.ensureEqual(results, expected, "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(gatts.getString("cdm_data_type"), "TimeSeries", gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_timeseries_variables"), "line_station", gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), "line_station", gatts.toString());

      table.readNcCF(
          fileName,
          StringArray.fromCSV("line_station"),
          0, // standardizeWhat=0
          StringArray.fromCSV("line_station"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      // String2.log("\n\n** Test nLevels=1/" + fileType + " " +
      // "just outerTable loadVars, constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("line_station,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("line_station"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("083.3_100"));
      results = table.dataToString();
      expected = "line_station\n" + "083.3_100\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(gatts.getString("cdm_data_type"), "TimeSeries", gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_timeseries_variables"), "line_station", gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), "line_station", gatts.toString());

      // String2.log("\n\n** Test nLevels=1/" + fileType + " " +
      // "just outerTable loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("line_station,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("line_station"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("zztop"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=1/" + fileType + ", specific loadVars,
      // constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV(
              "longitude,latitude,altitude,time,line_station,zztop,obsScientific,obsValue,obsUnits"),
          0, // standardizeWhat=0
          StringArray.fromCSV("line_station,obsValue"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("083.3_100,1"));
      results = table.dataToString();
      expected =
          "longitude,latitude,altitude,time,line_station,obsScientific,obsValue,obsUnits\n"
              + "-123.49333,32.245,-211.5,1.10027676E9,083.3_100,Argyropelecus sladeni,1,number of larvae\n"
              + "-123.49333,32.245,-211.5,1.10027676E9,083.3_100,Diogenichthys atlanticus,1,number of larvae\n"
              + "-123.49333,32.245,-211.5,1.10027676E9,083.3_100,Idiacanthus antrostomus,1,number of larvae\n"
              + "-123.49333,32.245,-211.5,1.10027676E9,083.3_100,Tetragonurus cuvieri,1,number of larvae\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(gatts.getString("cdm_data_type"), "TimeSeries", gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_timeseries_variables"), "line_station", gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), "line_station", gatts.toString());

      // String2.log("\n\n** Test nLevels=1/" + fileType +
      // ", specific loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV(
              "longitude,latitude,altitude,time,line_station,zztop,obsScientific,obsValue,obsUnits"),
          0, // standardizeWhat=0
          StringArray.fromCSV("line_station,obsValue"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("083.3_100,-9"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=1/" + fileType + " just obs loadVars, no
      // constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("obsScientific,obsValue,obsUnits,zztop"),
          0, // standardizeWhat=0
          null,
          null,
          null);
      results = table.dataToString();
      expected =
          "obsScientific,obsValue,obsUnits\n"
              + "Argyropelecus sladeni,2,number of larvae\n"
              + "Chauliodus macouni,3,number of larvae\n"
              + "Danaphos oculatus,4,number of larvae\n"
              + "Diogenichthys atlanticus,3,number of larvae\n"
              + "Idiacanthus antrostomus,3,number of larvae\n"
              + "Lestidiops ringens,1,number of larvae\n"
              + "Melamphaes lugubris,1,number of larvae\n"
              + "Protomyctophum crockeri,4,number of larvae\n"
              + "Stenobrachius leucopsarus,1,number of larvae\n"
              + "Total Fish Larvae,22,number of larvae\n"
              + "Argyropelecus affinis,1,number of larvae\n"
              + "Argyropelecus sladeni,2,number of larvae\n"
              + "Danaphos oculatus,1,number of larvae\n"
              + "Idiacanthus antrostomus,2,number of larvae\n"
              + "Melamphaes parvus,1,number of larvae\n"
              + "Nannobrachium spp,2,number of larvae\n"
              + "Protomyctophum crockeri,1,number of larvae\n"
              + "Total Fish Larvae,10,number of larvae\n"
              + "Argyropelecus sladeni,1,number of larvae\n"
              + "Diogenichthys atlanticus,1,number of larvae\n"
              + "Idiacanthus antrostomus,1,number of larvae\n"
              + "Tetragonurus cuvieri,1,number of larvae\n"
              + "Total Fish Larvae,4,number of larvae\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(gatts.getString("cdm_data_type"), "TimeSeries", gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_timeseries_variables"), null, gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), null, gatts.toString());

      table.readNcCF(
          fileName,
          StringArray.fromCSV("obsScientific"),
          0, // standardizeWhat=0
          StringArray.fromCSV("obsScientific"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      // String2.log("\n\n** Test nLevels=1/" + fileType + " just obs loadVars,
      // constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("obsScientific,obsValue,obsUnits,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("obsValue"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("4"));
      results = table.dataToString();
      expected =
          "obsScientific,obsValue,obsUnits\n"
              + "Danaphos oculatus,4,number of larvae\n"
              + "Protomyctophum crockeri,4,number of larvae\n"
              + "Total Fish Larvae,4,number of larvae\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(gatts.getString("cdm_data_type"), "TimeSeries", gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_timeseries_variables"), null, gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), null, gatts.toString());

      // String2.log("\n\n** Test nLevels=1/" + fileType +
      // " just obs loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("obsScientific,obsValue,obsUnits,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("obsValue"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-99"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");
    } // end nLevels=1 type loop

    /* */
    // ********* expected errors
    // String2.log("\n** Expected errors");
    try {
      table.readNcCF(
          TableTests.class.getResource("/data/nccf/badIndexed1To6.nc").getPath(),
          null,
          0, // standardizeWhat=0
          null,
          null,
          null);
      throw new SimpleException("Shouldn't get here.");

    } catch (Exception e) {
      if (e.toString()
              .indexOf("Invalid file: The index values must be 0 - 5, but parentIndex[88]=6.")
          < 0) throw e;
    }

    // ******* another test file, from Kevin O'Brien
    try {
      table.readNcCF(
          TableTests.class
              .getResource("/largeFiles/kevin/interpolated_gld.20120620_045152_meta_2.nc")
              .getPath(),
          null,
          0, // standardizeWhat=0
          null,
          null,
          null);
      throw new SimpleException("Shouldn't get here.");

    } catch (Exception e) {
      if (e.toString()
              .indexOf(
                  "Invalid contiguous ragged file: The sum of the values in the rowSizes "
                      + "variable (rowSize sum=1024368) is greater than the size of the "
                      + "observationDimension (obs size=1022528).")
          < 0) throw e;
    }

    /* */
    // Table.debugMode = oDebug;
  }

  /** This tests readNcCF nLevels=1. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Invalid contiguous ragged file
  void testReadNcCF1Kevin() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCF1Kevin");
    Table table = new Table();
    String results, expected;
    String fileName =
        TableTests.class
            .getResource("/largeFiles/kevin/interpolated_gld.20120620_045152_meta_2.nc")
            .getPath(); // from Kevin O'Brien
    Attributes gatts;

    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    // String2.log(table.toString());
    // Table.debugMode = oDebug;
  }

  /** This tests reading the gocd nccf files. */
  @org.junit.jupiter.api.Test
  void testReadGocdNcCF() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadGocdNcCF");
    Table table = new Table();
    String results, expected;
    String fileName;
    int po;
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    /*
     * //*************** non-standard timeSeries file -- TOO WEIRD, don't support it
     * //some var var[time=7022][z=1], depth[z=1]
     * //and other station vars (latitude, longitude) with no dimensions
     * fileName = "/data/gocd/gocdNcCF/gocd_v3_cmetr.nc";
     * String2.log("\n\n** Testing " + fileName);
     * String2.log(NcHelper.ncdump(fileName, "-h"));
     * // dimensions:
     * // z = 1;
     * // time = 7022;
     * // float sampling_interval; // :_FillValue = 9999.9f; // float
     * // float seafloor_depth;
     * // float latitude;
     * // float longitude;
     * // int latitude_quality_flag;
     * // int longitude_quality_flag;
     * // int crs;
     *
     * // float depth(z=1);
     * // int depth_quality_flag(z=1);
     *
     * // double time(time=7022);
     * // int time_quality_flag(time=7022);
     *
     * // float u(time=7022, z=1);
     * // int u_quality_flag(time=7022, z=1);
     * // float v(time=7022, z=1);
     * // int v_quality_flag(time=7022, z=1);
     * // float current_speed(time=7022, z=1);
     * // int current_speed_quality_flag(time=7022, z=1);
     * // float current_direction(time=7022, z=1);
     * // int current_direction_quality_flag(time=7022, z=1);
     *
     * // global attributes:
     * // :gocd_id = "gocd_a0084999_tr1162.nc";
     * // :id = "0093183";
     * // :featureType = "timeSeries";
     * // :cdm_data_type = "Station";
     * // :instrument_type = "";
     * table.readNcCF(fileName, null, 0, //standardizeWhat=0
     * null, null, null);
     * //String2.log(table.toCSVString());
     * results = table.dataToString(5);
     * expected =
     * "zztop\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     * /*
     */

    // *************** trajectory profile
    // This is important test of Table.readNcCF
    // if multidimensional file, 2 levels, and outerDim=ScalarDim
    // this tests if var[scalarDim][obs] was read (e.g., depth[obs]
    // File dims are outerDim=[scalarDim], inner=[time], obs=[obs]
    table = new Table();
    fileName = TableTests.class.getResource("/data/gocdNcCF/gocd_v3_sadcp.nc").getPath();
    // String2.log("\n\n** Testing " + fileName);
    results = NcHelper.ncdump(fileName, "-h");
    // String2.log(results);
    // just the structure, rearranged into groups
    // dimensions:
    // time = 501;
    // z = 70;

    // float sampling_interval;
    // int crs;
    //
    // float seafloor_depth(time=501);
    // float latitude(time=501);
    // float longitude(time=501);
    // double time(time=501);
    // int latitude_quality_flag(time=501);
    // int longitude_quality_flag(time=501);
    // int time_quality_flag(time=501);
    //
    // float depth(z=70);
    // int depth_quality_flag(z=70);
    //
    // float u(time=501, z=70);
    // int u_quality_flag(time=501, z=70);
    // float v(time=501, z=70);
    // int v_quality_flag(time=501, z=70);
    // float current_speed(time=501, z=70);
    // int current_speed_quality_flag(time=501, z=70);
    // float current_direction(time=501, z=70);
    // int current_direction_quality_flag(time=501, z=70);

    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    os.reset();
    table.saveAsDAS(os, Table.SEQUENCE_NAME);
    results = os.toString();
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  sampling_interval {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String long_name \"Sampling Interval\";\n"
            + "    String units \"minutes\";\n"
            + "  }\n"
            + "  seafloor_depth {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String long_name \"Seafloor Depth\";\n"
            + "    String postive \"down\";\n"
            + "    String units \"meters\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String ancillary_variables \"latitude_quality_flag\";\n"
            + "    String axis \"Y\";\n"
            + "    Float64 data_max 21.0958;\n"
            + "    Float64 data_min -14.3883;\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String long_name \"latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float32 valid_max 90.0;\n"
            + "    Float32 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String ancillary_variables \"longitude_quality_flag\";\n"
            + "    String axis \"X\";\n"
            + "    Float64 data_max -158.3554;\n"
            + "    Float64 data_min -176.8126;\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String long_name \"longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float32 valid_max 180.0;\n"
            + "    Float32 valid_min -180.0;\n"
            + "  }\n"
            + "  latitude_quality_flag {\n"
            + "    Int32 _FillValue -9;\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Latitude Quality Flag\";\n"
            + "  }\n"
            + "  longitude_quality_flag {\n"
            + "    Int32 _FillValue -9;\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Longitude Quality Flag\";\n"
            + "  }\n"
            + "  depth {\n"
            + "    String C_format \"%7.2f\";\n"
            + "    Float64 data_max 720.0;\n"
            + "    Float64 data_min 30.0;\n"
            + "    String FORTRAN_format \"F7.2\";\n"
            + "    String long_name \"Depth\";\n"
            + "    String postive \"down\";\n"
            + "    String units \"meters\";\n"
            + "    Float32 valid_max 15000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  depth_quality_flag {\n"
            + "    Int32 _FillValue -9;\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Depth QC Flags\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String ancillary_variables \"time_quality_flag\";\n"
            + "    String axis \"T\";\n"
            + "    String C_format \"%9.4f\";\n"
            + "    Float64 data_max 38751.8319444442;\n"
            + "    Float64 data_min 38730.9986111112;\n"
            + "    String FORTRAN_format \"F9.4\";\n"
            + "    String long_name \"time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String units \"days since 1900-01-01 00:00:00Z\";\n"
            + "  }\n"
            + "  time_quality_flag {\n"
            + "    Int32 _FillValue -9;\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Time Quality Flag\";\n"
            + "  }\n"
            + "  u {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String ancillary_variables \"u_quality_flag\";\n"
            + "    String C_format \"%7.4f\";\n"
            + "    String cell_methods \"time:point z:point\";\n"
            + "    String coordinates \"time z\";\n"
            + "    Float64 data_max 0.883000030517578;\n"
            + "    Float64 data_min -1.25;\n"
            + "    String FORTRAN_format \"F7.4\";\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String long_name \"Eastward Velocity Component\";\n"
            + "    String standard_name \"eastward_sea_water_velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "    Float64 valid_max 5.0;\n"
            + "    Float64 valid_min -5.0;\n"
            + "  }\n"
            + "  u_quality_flag {\n"
            + "    Int32 _FillValue -9;\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Eastward Velocity component QC Flags\";\n"
            + "  }\n"
            + "  v {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String ancillary_variables \"v_quality_flag\";\n"
            + "    String C_format \"%7.4f\";\n"
            + "    String cell_methods \"time:point z:point\";\n"
            + "    String coordinates \"time z\";\n"
            + "    Float64 data_max 0.733;\n"
            + "    Float64 data_min -0.695;\n"
            + "    String FORTRAN_format \"F7.4\";\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String long_name \"Northward Velocity Component\";\n"
            + "    String standard_name \"northward_sea_water_velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "    Float64 valid_max 5.0;\n"
            + "    Float64 valid_min -5.0;\n"
            + "  }\n"
            + "  v_quality_flag {\n"
            + "    Int32 _FillValue -9;\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Northward Velocity component QC Flags\";\n"
            + "  }\n"
            + "  current_speed {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String ancillary_variables \"current_direction_quality_flag\";\n"
            + "    String C_format \"%7.4f\";\n"
            + "    String cell_methods \"time:point z:point\";\n"
            + "    String coordinates \"time z\";\n"
            + "    Float64 data_max 141.42;\n"
            + "    Float64 data_min 0.0;\n"
            + "    String FORTRAN_format \"F7.4\";\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String long_name \"Current_Speed\";\n"
            + "    String units \"m s-1\";\n"
            + "    Float64 valid_max 7.0711;\n"
            + "    Float64 valid_min 0.0;\n"
            + "  }\n"
            + "  current_speed_quality_flag {\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Current Speed QC Flags\";\n"
            + "  }\n"
            + "  current_direction {\n"
            + "    Float32 _FillValue 9999.9;\n"
            + "    String ancillary_variables \"current_direction_quality_flag\";\n"
            + "    String C_format \"%5.1f\";\n"
            + "    String cell_methods \"time:point z:point\";\n"
            + "    String comment \"True Direction toward which current is flowing\";\n"
            + "    String coordinates \"time z\";\n"
            + "    Float64 data_max 360.0;\n"
            + "    Float64 data_min 0.3;\n"
            + "    String FORTRAN_format \"F5.1\";\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String long_name \"Current Direction\";\n"
            + "    String units \"degrees\";\n"
            + "    Float64 valid_max 360.0;\n"
            + "    Float64 valid_min 0.0;\n"
            + "  }\n"
            + "  current_direction_quality_flag {\n"
            + "    String flag_meanings \"good_value probably_good probably_bad bad_value modified_value not_used not_used not_used missing_value\";\n"
            + "    Int32 flag_values 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String long_name \"Current Direction QC Flags\";\n"
            + "  }\n"
            + "  crs {\n"
            + "    String epsg_code \"EPSG:4326\";\n"
            + "    String grid_mapping_name \"latitude_longitude\";\n"
            + "    String inverse_flattening \"298.257223563\";\n"
            + "    String long_name \"Coordinate Reference System\";\n"
            + "    String longitude_of_prime_meridian \"0.0f\";\n"
            + "    String semi_major_axis \"6378137.0\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String acknowledgment \"These data were acquired from the US NOAA National Centers for Environmental Information (NCEI) on [DATE] from http://www.nodc.noaa.gov/gocd/.\";\n"
            + "    String cdm_data_type \"TrajectoryProfile\";\n"
            + "    String cdm_profile_variables \"seafloor_depth, latitude, longitude, latitude_quality_flag, longitude_quality_flag, time, time_quality_flag\";\n"
            + "    String cdm_trajectory_variables \"sampling_interval, crs\";\n"
            + "    String contributor \"University of Hawaii and NOAA/NMFS\";\n"
            + "    String Conventions \"CF-1.6\";\n"
            + "    String creator_email \"Charles.Sun@noaa.gov\";\n"
            + "    String creator_name \"Charles Sun\";\n"
            + "    String creator_url \"http://www.nodc.noaa.gov\";\n"
            + "    String date_created \"2014-12-15T20:20:04Z\";\n"
            + "    String date_issued \"2016-01-18T04:39:10Z\";\n"
            + "    String date_modified \"2016-01-18T04:39:10Z\";\n"
            + "    String featureType \"trajectoryProfile\";\n"
            + "    Float32 geospatial_lat_max 21.0958;\n"
            + "    Float32 geospatial_lat_min -14.3883;\n"
            + "    String geospatial_lat_resolution \"point\";\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float32 geospatial_lon_max -158.3554;\n"
            + "    Float32 geospatial_lon_min -176.8126;\n"
            + "    String geospatial_lon_resolution \"point\";\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float32 geospatial_vertical_max 720.0;\n"
            + "    Float32 geospatial_vertical_min 30.0;\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_resolution \"point\";\n"
            + "    String geospatial_vertical_units \"meters\";\n"
            + "    String gocd_format_version \"GOCD-3.0\";\n"
            + "    String gocd_id \"gocd_a0067774_01192v3.nc\";\n"
            + "    String grid_mapping_epsg_code \"EPSG:4326\";\n"
            + "    String grid_mapping_inverse_flattening \"298.257223563\";\n"
            + "    String grid_mapping_long_name \"Coordinate Reference System\";\n"
            + "    String grid_mapping_longitude_of_prime_meridian \"0.0f\";\n"
            + "    String grid_mapping_name \"latitude_longitude\";\n"
            + "    String grid_mapping_semi_major_axis \"6378137.0\";\n"
            + "    String history \"Wed Feb 10 18:31:43 2016: ncrename -d depth,z test.nc\n"
            + "2016-01-18T04:39:10Z csun updateOCD.R Version 2.0\n"
            + "Thu Jan  7 15:59:35 2016: ncatted -a valid_max,seafloor_depth,d,, ../V3/a0067774/gocd_a0067774_01192v3.nc\n"
            + "Thu Jan  7 15:59:35 2016: ncatted -a valid_min,seafloor_depth,d,, ../V3/a0067774/gocd_a0067774_01192v3.nc\n"
            + "Thu Jan  7 15:59:35 2016: ncatted -a missing_value,seafloor_depth,d,, ../V3/a0067774/gocd_a0067774_01192v3.nc\n"
            + "Thu Jan  7 15:59:35 2016: ncatted -a missing_value,sampling_interval,d,, ../V3/a0067774/gocd_a0067774_01192v3.nc\n"
            + "2016-01-07T14:01:54Z csun updateGOCD.R Version 1.0\n"
            + "2014-12-15T20:20:04Z csun convJASADCP.f90 Version 1.0\";\n"
            + "    String id \"0093183\";\n"
            + "    String institution \"NOAA National Centers for Environmental Information\";\n"
            + "    String instrument_type \"Ocean Surveyor OS75\";\n"
            + "    String keywords \"EARTH SCIENCE,OCEANS,OCEAN CIRCULATION,OCEAN CURRENTS\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"These data are openly available to the public Please acknowledge the use of these data with the text given in the acknowledgment attribute.\";\n"
            + "    String Metadata_Conventions \"Unidata Dataset Discovery v1.0\";\n"
            + "    String naming_authority \"gov.noaa.nodc\";\n"
            + "    String principal_invesigator \"E.Firing,J.Hummon,R.Brainard\";\n"
            + "    String project_name \"Coral Reef Ecosystem Investigations\";\n"
            + "    String publisher_email \"NODC.Services@noaa.gov\";\n"
            + "    String publisher_name \"US DOC; NESDIS; NATIONAL CENTERS FOR ENVIRONMENTAL INFORMATION - IN295\";\n"
            + "    String publisher_url \"http://www.nodc.noaa.gov/\";\n"
            + "    String QC_indicator \"Contact Principle Investigaror(s)\";\n"
            + // sic
            "    String QC_Manual \"Contact Principle Investigaror(s)\";\n"
            + "    String QC_Software \"Contact Principle Investigaror(s)\";\n"
            + "    String QC_test_codes \"Contact Principle Investigaror(s)\";\n"
            + "    String QC_test_names \"Contact Principle Investigaror(s)\";\n"
            + "    String QC_test_results \"Contact Principle Investigaror(s)\";\n"
            + "    String references \"http://www.nodc.noaa.gov/\";\n"
            + "    String source \"global ocean currents in the NCEI archive holdings\";\n"
            + "    String standard_name_vocabulary \"CF-1.6\";\n"
            + "    String subsetVariables \"sampling_interval, crs, seafloor_depth, latitude, longitude, latitude_quality_flag, longitude_quality_flag, time, time_quality_flag\";\n"
            + "    String summary \"global ocean currents in the NCEI archive holdings\";\n"
            + "    String time_coverage_duration \"P0Y020DT20H00M00S\";\n"
            + "    String time_coverage_end \"2006-02-05T19:58:00Z\";\n"
            + "    String time_coverage_resolution \"R000501/2006-01-15T23:58:00Z/P0Y020DT20H00M00\";\n"
            + "    String time_coverage_start \"2006-01-15T23:58:00Z\";\n"
            + "    String title \"Global Ocean Currents Database  - gocd_a0067774_01192v3.nc\";\n"
            + "    String uuid \"26f4a163-4c81-437b-9ad7-796822d1ce49\";\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    os.reset();
    table.saveAsDDS(os, Table.SEQUENCE_NAME);
    results = os.toString();
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 sampling_interval;\n"
            + "    Float32 seafloor_depth;\n"
            + "    Float32 latitude;\n"
            + "    Float32 longitude;\n"
            + "    Int32 latitude_quality_flag;\n"
            + "    Int32 longitude_quality_flag;\n"
            + "    Float32 depth;\n"
            + "    Int32 depth_quality_flag;\n"
            + "    Float64 time;\n"
            + "    Int32 time_quality_flag;\n"
            + "    Float32 u;\n"
            + "    Int32 u_quality_flag;\n"
            + "    Float32 v;\n"
            + "    Int32 v_quality_flag;\n"
            + "    Float32 current_speed;\n"
            + "    Int32 current_speed_quality_flag;\n"
            + "    Float32 current_direction;\n"
            + "    Int32 current_direction_quality_flag;\n"
            + "    Int32 crs;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // String2.log(table.toCSVString());
    results = table.dataToString(5);
    expected =
        "sampling_interval,seafloor_depth,latitude,longitude,latitude_quality_flag,longitude_quality_flag,"
            + "depth,depth_quality_flag,time,time_quality_flag,u,u_quality_flag,v,v_quality_flag,"
            + "current_speed,current_speed_quality_flag,current_direction,"
            + "current_direction_quality_flag,crs\n"
            +
            // sapmInt,sfDpth lat lon depth time u v cspeed cdir crs
            "9999.9,9999.9,21.0958,-158.3554,1,1,30.0,1,38730.9986,1,-0.004,1,0.174,1,0.174,1,358.7,1,0\n"
            + "9999.9,9999.9,21.0958,-158.3554,1,1,40.0,1,38730.9986,1,-0.008,1,0.169,1,0.1692,1,357.3,1,0\n"
            + "9999.9,9999.9,21.0958,-158.3554,1,1,50.0,1,38730.9986,1,-0.01,1,0.165,1,0.1653,1,356.5,1,0\n"
            + "9999.9,9999.9,21.0958,-158.3554,1,1,60.0,1,38730.9986,1,-0.009,1,0.163,1,0.1632,1,356.8,1,0\n"
            + "9999.9,9999.9,21.0958,-158.3554,1,1,70.0,1,38730.9986,1,-0.012,1,0.173,1,0.1734,1,356.0,1,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.dataToString(); // no row numbers
    expected = // why are cspeed and cdir known, but u,v not?
        // sampInt sfDpth lat lon depth time u v cspeed cdir crs
        "9999.9,9999.9,-14.2758,-170.6805,1,1,680.0,1,38751.8319,1,9999.9,-9,9999.9,-9,141.42,1,45.0,1,0\n"
            + "9999.9,9999.9,-14.2758,-170.6805,1,1,690.0,1,38751.8319,1,9999.9,-9,9999.9,-9,141.42,1,45.0,1,0\n"
            + "9999.9,9999.9,-14.2758,-170.6805,1,1,700.0,1,38751.8319,1,9999.9,-9,9999.9,-9,141.42,1,45.0,1,0\n"
            + "9999.9,9999.9,-14.2758,-170.6805,1,1,710.0,1,38751.8319,1,9999.9,-9,9999.9,-9,141.42,1,45.0,1,0\n"
            + "9999.9,9999.9,-14.2758,-170.6805,1,1,720.0,1,38751.8319,1,9999.9,-9,9999.9,-9,141.42,1,45.0,1,0\n";
    po = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results.substring(po));
    /* */

    // *************** a similar test
    // 2 levels, and outerDim=ScalarDim
    // this tests if var[scalarDim][obs] was read (e.g., depth[obs]
    // File dims are outerDim=[scalarDim], inner=[time], obs=[obs]
    table = new Table();
    fileName = TableTests.class.getResource("/data/gocd/gocd_v3_madcp.nc").getPath();
    // String2.log("\n\n** Testing " + fileName);
    results = NcHelper.ncdump(fileName, "-h");
    // String2.log(results);
    // just the structure, rearranged into groups
    // z = 14;
    // time = 3188;
    // int crs;
    // float sampling_interval;
    // float seafloor_depth;
    // float latitude;
    // float longitude;
    // int latitude_quality_flag;
    // int longitude_quality_flag;

    // float depth(z=14);
    // int depth_quality_flag(z=14);

    // double time(time=3188);
    // int time_quality_flag(time=3188);

    // float u(time=3188, z=14);
    // int u_quality_flag(time=3188, z=14);
    // float v(time=3188, z=14);
    // int v_quality_flag(time=3188, z=14);
    // float current_speed(time=3188, z=14);
    // int current_speed_quality_flag(time=3188, z=14);
    // float current_direction(time=3188, z=14);
    // int current_direction_quality_flag(time=3188, z=14);

    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    os.reset();
    table.saveAsDDS(os, Table.SEQUENCE_NAME);
    results = os.toString();
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 sampling_interval;\n"
            + "    Float32 seafloor_depth;\n"
            + "    Float32 latitude;\n"
            + "    Float32 longitude;\n"
            + "    Int32 latitude_quality_flag;\n"
            + "    Int32 longitude_quality_flag;\n"
            + "    Float32 depth;\n"
            + "    Int32 depth_quality_flag;\n"
            + "    Float64 time;\n"
            + "    Int32 time_quality_flag;\n"
            + "    Float32 u;\n"
            + "    Int32 u_quality_flag;\n"
            + "    Float32 v;\n"
            + "    Int32 v_quality_flag;\n"
            + "    Float32 current_speed;\n"
            + "    Int32 current_speed_quality_flag;\n"
            + "    Float32 current_direction;\n"
            + "    Int32 current_direction_quality_flag;\n"
            + "    Int32 crs;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // String2.log(table.toCSVString());
    results = table.dataToString(5);
    expected =
        "sampling_interval,seafloor_depth,latitude,longitude,"
            + "latitude_quality_flag,longitude_quality_flag,depth,depth_quality_flag,time,time_quality_flag,u,u_quality_flag,v,v_quality_flag,"
            + "current_speed,current_speed_quality_flag,current_direction,"
            + "current_direction_quality_flag,crs\n"
            +
            // si sDepth lat lon q q depth q time q u q v q cspeed q cDir q crs
            "60.0,32.6386,42.37859,-70.78094,1,1,26.34,1,38621.7448,1,0.0066,1,0.0072,1,0.0111,1,42.8,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,24.34,1,38621.7448,1,0.0279,1,-0.008,1,0.0292,1,106.0,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,22.34,1,38621.7448,1,0.0325,1,2.0E-4,1,0.033,1,89.7,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,20.34,1,38621.7448,1,-0.0094,1,0.0011,1,0.0121,1,277.0,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,18.34,1,38621.7448,1,-0.0383,1,-0.0367,1,0.0532,1,226.2,1,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = table.dataToString(); // no row numbers
    // String2.log(results);
    expected = // why are cspeed and cdir known, but u,v not?
        // si sDepth lat lon q q depth q time q u q v q cspeed q cDir q crs
        "60.0,32.6386,42.37859,-70.78094,1,1,8.34,1,38754.5365,1,-0.0177,1,-0.0152,1,0.0257,1,229.2,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,6.34,1,38754.5365,1,-0.0186,1,-0.0145,1,0.0251,1,232.0,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,4.34,1,38754.5365,1,-0.0149,1,-0.0181,1,0.03,1,219.4,1,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,2.34,1,38754.5365,1,-0.0026,2,-0.0202,2,0.0293,1,187.4,2,0\n"
            + "60.0,32.6386,42.37859,-70.78094,1,1,0.34,1,38754.5365,1,9999.9,9,9999.9,9,9999.9,9,45.0,9,0\n";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results.substring(po));
    /* */

    // ************* v4 file #1 timeSeries has
    // featureType=timeSeriesProfile cdm_data_type=profile
    // outerdim=scalar var[time=6952][z=1] time[time=6952] depth[z=1]
    table = new Table();
    fileName = TableTests.class.getResource("/data/gocd/gocd_a0000841_rcm00566_v4.nc").getPath();
    // String2.log("\n\n** Testing " + fileName);
    results = NcHelper.ncdump(fileName, "-h");
    // String2.log(results);
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    // String2.log(table.dataToString());
    results = table.dataToString(5);
    expected =
        "sampling_interval,seafloor_depth,latitude,longitude,latitude_quality_flag,"
            + "longitude_quality_flag,depth,depth_quality_flag,time,time_quality_flag,"
            + "u,u_quality_flag,v,v_quality_flag,current_speed,current_speed_quality_flag,"
            + "current_direction,current_direction_quality_flag,crs\n"
            +
            // si sd lat lon q q depth q time q u q v q cSpeed p dir q crs
            "60.0,2640.0,62.894997,-35.857998,1,1,1980.0,1,31662.424999999814,1,0.015,1,0.0865,1,0.0878,1,9.8,1,0\n"
            + "60.0,2640.0,62.894997,-35.857998,1,1,1980.0,1,31662.46666666679,1,-0.0045,1,0.0933,1,0.0934,1,357.2,1,0\n"
            + "60.0,2640.0,62.894997,-35.857998,1,1,1980.0,1,31662.508333333302,1,-0.0072,1,0.0856,1,0.0859,1,355.2,1,0\n"
            + "60.0,2640.0,62.894997,-35.857998,1,1,1980.0,1,31662.549999999814,1,-0.0065999995,1,0.1025,1,0.1027,1,356.3,1,0\n"
            + "60.0,2640.0,62.894997,-35.857998,1,1,1980.0,1,31662.59166666679,1,-0.0036,1,0.11200001,1,0.1121,1,358.2,1,0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    /* */

    // ********************* v4 file #2 has
    /*
     * // featureType=timeSeriesProfile cdm_data_type=profile
     * // latitude=scalar var[time=126][z=12] time[time=126] depth[z=12]
     * table = new Table();
     * fileName = "/data/gocd/gocd_a0060062_4381adc-a_v4.nc";
     * String2.log("\n\n** Testing " + fileName);
     * results = NcHelper.ncdump(fileName, "-h");
     * String2.log(results);
     * table.readNcCF(fileName, null, 0, //standardizeWhat=0
     * null, null, null);
     * String2.log(table.dataToString());
     * results = table.dataToString(5);
     * expected =
     * "zz\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     * /*
     */

    // String2.log("\n*** Table.testReadGocdNcCF finished successfully");
    // Table.debugMode = oDebug;

  }

  /** This tests readNcCF nLevels=2. */
  @org.junit.jupiter.api.Test
  void testReadNcCF2() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCF2");
    Table table = new Table();
    String results, expected;
    Attributes gatts;

    // *************** nLevels=2 multidimensional
    for (int type = 0; type < 2; type++) {
      // ncCF2b and ncCFMA2b have same data, so tests are the same!
      String fileType = type == 0 ? "ragged" : "multidimensional";
      // from EDDTableFromNcFiles.testNcCF2b() and testNcCFMA2b();
      String fileName =
          TableTests.class
              .getResource("/data/nccf/" + (type == 0 ? "ncCF2b.nc" : "ncCFMA2b.nc"))
              .getPath();
      String msg = "ERROR when type=" + type + ": ";

      // String2.log("\n\n** Testing type=" + type + " nLevels=2/" + fileType + "\n" +
      // " " + fileName);
      // String2.log(NcHelper.ncdump(fileName, "-h"));
      /* */
      // String2.log("\n\n** Test nLevels=2/" + fileType + " no loadVars, no
      // constraints");
      table.readNcCF(
          fileName, null, 0, // standardizeWhat=0
          null, null, null);
      // String2.log(table.toString());
      results = table.dataToString(5);
      expected =
          "platform,cruise,org,type,station_id,longitude,latitude,time,depth,temperature,salinity\n"
              + "33P2,Q990046312,ME,TE,13968849,176.64,-75.45,1.3351446E9,4.0,-1.84,35.64\n"
              + "33P2,Q990046312,ME,TE,13968849,176.64,-75.45,1.3351446E9,10.0,-1.84,35.64\n"
              + "33P2,Q990046312,ME,TE,13968849,176.64,-75.45,1.3351446E9,20.0,-1.83,35.64\n"
              + "33P2,Q990046312,ME,TE,13968849,176.64,-75.45,1.3351446E9,30.0,-1.83,35.64\n"
              + "33P2,Q990046312,ME,TE,13968849,176.64,-75.45,1.3351446E9,49.0,-1.83,35.64\n"
              + "...\n";
      Test.ensureEqual(results, expected, msg);
      Test.ensureEqual(table.nRows(), 53, msg + table.toString());
      results = table.columnAttributes(0).toString();
      expected =
          "    comment=See the list of platform codes (sorted in various ways) at http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html\n"
              + "    ioos_category=Identifier\n"
              + "    long_name=GTSPP Platform Code\n"
              + "    references=http://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_trajectory_variables"), "platform, cruise", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"),
          "org, type, station_id, longitude, latitude, time",
          msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"),
          "platform, cruise, org, type, station_id, longitude, latitude, time",
          msg + gatts.toString());

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("cruise,latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("cruise"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      table.readNcCF(
          fileName,
          StringArray.fromCSV("cruise,latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      table.readNcCF(
          fileName,
          StringArray.fromCSV("cruise,latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("depth"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      table.readNcCF(
          fileName,
          StringArray.fromCSV("cruise,latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("temperature"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("depth"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("temperature"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("depth"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, msg);
      Test.ensureEqual(table.nColumns(), 0, msg);

      table.readNcCF(
          fileName,
          StringArray.fromCSV("depth,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("temperature"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("temperature"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("cruise"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("depth,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("depth"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("depth,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("cruise"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("temperature,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("temperature"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("temperature,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("cruise"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      table.readNcCF(
          fileName,
          StringArray.fromCSV("cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("cruise"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude"),
          0, // standardizeWhat=0
          StringArray.fromCSV("latitude"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("temperature"),
          0, // standardizeWhat=0
          StringArray.fromCSV("temperature"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      table.readNcCF(
          fileName,
          StringArray.fromCSV("depth"),
          0, // standardizeWhat=0
          StringArray.fromCSV("depth"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-12345"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      //
      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable loadVars, constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("platform,cruise,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("platform"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("33P2"));
      results = table.dataToString();
      expected = "platform,cruise\n" + "33P2,Q990046312\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_trajectory_variables"), "platform, cruise", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_profile_variables"), null, msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"), "platform, cruise", msg + gatts.toString());

      //
      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("platform,cruise,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("platform"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("zztop"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just innerTable loadVars, no constraints");
      // String2.log("ncdump of " + fileName + "\n" + NcHelper.ncdump(fileName, "-v
      // station_id;type"));
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station_id,zztop,type"),
          0, // standardizeWhat=0
          null,
          null,
          null);
      results = table.dataToString();
      expected =
          // before: 4th row with mv's is removed
          // 2020-08-03 now it isn't removed for type=1 because station_id is int with no
          // defined _FillValue or missing_value and [3]=2147483647!
          // In other words: this is a flaw in the data file.
          // note type0=ragged, type1=multidimensional
          "station_id,type\n"
              + "13968849,TE\n"
              + "13968850,TE\n"
              + "13933177,BA\n"
              + (type == 0 ? "" : "2147483647,\n");
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_trajectory_variables"), null, msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"), "type, station_id", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"), "type, station_id", msg + gatts.toString());

      //
      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just innerTable loadVars, constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station_id,zztop,type"),
          0, // standardizeWhat=0
          StringArray.fromCSV("station_id"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("13933177"));
      results = table.dataToString();
      expected = "station_id,type\n" + "13933177,BA\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_trajectory_variables"), null, msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"), "type, station_id", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"), "type, station_id", msg + gatts.toString());

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just innerTable loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("station_id,zztop,type"),
          0, // standardizeWhat=0
          StringArray.fromCSV("station_id"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("zztop"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable and innerTable loadVars, no constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("cruise,org,type,station_id,longitude,latitude,time,zztop,platform"),
          0, // standardizeWhat=0
          null,
          null,
          null);
      results = table.dataToString();
      expected =
          "cruise,org,type,station_id,longitude,latitude,time,platform\n"
              + "Q990046312,ME,TE,13968849,176.64,-75.45,1.3351446E9,33P2\n"
              + "Q990046312,ME,TE,13968850,176.64,-75.43,1.335216E9,33P2\n"
              + "SHIP    12,ME,BA,13933177,173.54,-34.58,1.33514142E9,9999\n"
              + (type == 0
                  ? ""
                  : "SHIP    12,,,2147483647,,,,9999\n"); // 2020-08-03 this also appeared. See
      // 2020-08-03
      // comments above
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_trajectory_variables"), "platform, cruise", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"),
          "org, type, station_id, longitude, latitude, time",
          msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"),
          "platform, cruise, org, type, station_id, longitude, latitude, time",
          msg + gatts.toString());

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable and innerTable loadVars, constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV(
              "row,cruise,org,type,station_id,longitude,latitude,time,zztop,platform"),
          0, // standardizeWhat=0
          StringArray.fromCSV("platform,station_id"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("33P2,13968850"));
      results = table.dataToString();
      expected =
          "cruise,org,type,station_id,longitude,latitude,time,platform\n"
              + "Q990046312,ME,TE,13968850,176.64,-75.43,1.335216E9,33P2\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_trajectory_variables"), "platform, cruise", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"),
          "org, type, station_id, longitude, latitude, time",
          msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"),
          "platform, cruise, org, type, station_id, longitude, latitude, time",
          msg + gatts.toString());

      //
      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable and innerTable loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV(
              "row,cruise,org,type,station_id,longitude,latitude,time,zztop,platform"),
          0, // standardizeWhat=0
          StringArray.fromCSV("platform,station_id"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("33P2,zztop"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable and obs loadVars, constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("salinity,platform,zztop,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("platform,salinity"),
          StringArray.fromCSV("=,>="),
          StringArray.fromCSV("33P2,35.98"));
      results = table.dataToString();
      expected =
          "salinity,platform,cruise\n"
              + "35.99,33P2,Q990046312\n"
              + "36.0,33P2,Q990046312\n"
              + "35.98,33P2,Q990046312\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_trajectory_variables"), "platform, cruise", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_profile_variables"), null, msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"), "platform, cruise", msg + gatts.toString());

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just outerTable and obs loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("salinity,platform,zztop,cruise"),
          0, // standardizeWhat=0
          StringArray.fromCSV("platform,salinity"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("33P2,-100"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just innerTable and obs loadVars, no constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,longitude,time,zztop,salinity"),
          0, // standardizeWhat=0
          null,
          null,
          null);
      results = table.dataToString();
      expected =
          "latitude,longitude,time,salinity\n"
              + "-75.45,176.64,1.3351446E9,35.64\n"
              + "-75.45,176.64,1.3351446E9,35.64\n"
              + "-75.45,176.64,1.3351446E9,35.64\n"
              + "-75.45,176.64,1.3351446E9,35.64\n"
              + "-75.45,176.64,1.3351446E9,35.64\n"
              + "-75.45,176.64,1.3351446E9,35.63\n"
              + "-75.45,176.64,1.3351446E9,35.59\n"
              + "-75.45,176.64,1.3351446E9,35.52\n"
              + "-75.45,176.64,1.3351446E9,35.56\n"
              + "-75.45,176.64,1.3351446E9,35.77\n"
              + "-75.45,176.64,1.3351446E9,35.81\n"
              + "-75.45,176.64,1.3351446E9,35.82\n"
              + "-75.45,176.64,1.3351446E9,35.88\n"
              + "-75.45,176.64,1.3351446E9,35.94\n"
              + "-75.45,176.64,1.3351446E9,35.99\n"
              + "-75.45,176.64,1.3351446E9,36.0\n"
              + "-75.43,176.64,1.335216E9,35.64\n"
              + "-75.43,176.64,1.335216E9,35.64\n"
              + "-75.43,176.64,1.335216E9,35.64\n"
              + "-75.43,176.64,1.335216E9,35.64\n"
              + "-75.43,176.64,1.335216E9,35.63\n"
              + "-75.43,176.64,1.335216E9,35.63\n"
              + "-75.43,176.64,1.335216E9,35.61\n"
              + "-75.43,176.64,1.335216E9,35.58\n"
              + "-75.43,176.64,1.335216E9,35.5\n"
              + "-75.43,176.64,1.335216E9,35.77\n"
              + "-75.43,176.64,1.335216E9,35.81\n"
              + "-75.43,176.64,1.335216E9,35.86\n"
              + "-75.43,176.64,1.335216E9,35.9\n"
              + "-75.43,176.64,1.335216E9,35.93\n"
              + "-75.43,176.64,1.335216E9,35.96\n"
              + "-75.43,176.64,1.335216E9,35.98\n";

      // multidimensional deletes these rows because all read obs columns have mv's
      // This is not ideal, but I don't want to read all obs columns in file to
      // determine which rows to delete.
      if (fileType.equals("ragged"))
        expected +=
            "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n"
                + "-34.58,173.54,1.33514142E9,\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_trajectory_variables"), null, msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"),
          "longitude, latitude, time",
          msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"), "longitude, latitude, time", msg + gatts.toString());

      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just innerTable and obs loadVars, constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,longitude,time,zztop,salinity"),
          0, // standardizeWhat=0
          StringArray.fromCSV("time,salinity"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("1.335216E9,35.77"));
      results = table.dataToString();
      expected = "latitude,longitude,time,salinity\n" + "-75.43,176.64,1.335216E9,35.77\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_trajectory_variables"), null, msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("cdm_profile_variables"),
          "longitude, latitude, time",
          msg + gatts.toString());
      Test.ensureEqual(
          gatts.getString("subsetVariables"), "longitude, latitude, time", msg + gatts.toString());

      //
      // String2.log("\n\n** Test nLevels=2/" + fileType + " " +
      // "just innerTable and obs loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("latitude,longitude,time,zztop,salinity"),
          0, // standardizeWhat=0
          StringArray.fromCSV("time,salinity"),
          StringArray.fromCSV("=,="),
          StringArray.fromCSV("1.335216E9,-1000"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");

      // String2.log("\n\n** Test nLevels=2/" + fileType + " just obs loadVars,
      // constraints");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("temperature,salinity,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("salinity"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("35.77"));
      results = table.dataToString();
      expected = "temperature,salinity\n" + "-1.1,35.77\n" + "-1.12,35.77\n";
      Test.ensureEqual(results, expected, msg + "results=\n" + results);
      gatts = table.globalAttributes();
      Test.ensureEqual(
          gatts.getString("cdm_data_type"), "TrajectoryProfile", msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_trajectory_variables"), null, msg + gatts.toString());
      Test.ensureEqual(gatts.getString("cdm_profile_variables"), null, msg + gatts.toString());
      Test.ensureEqual(gatts.getString("subsetVariables"), null, msg + gatts.toString());

      //
      // String2.log("\n\n** Test nLevels=2/" + fileType +
      // " just obs loadVars, constraints, NO_DATA");
      table.readNcCF(
          fileName,
          StringArray.fromCSV("temperature,salinity,zztop"),
          0, // standardizeWhat=0
          StringArray.fromCSV("salinity"),
          StringArray.fromCSV("="),
          StringArray.fromCSV("-1000"));
      Test.ensureEqual(table.nRows(), 0, "");
      Test.ensureEqual(table.nColumns(), 0, "");
    } // end nLevels=2 type loop

    // Table.debugMode = oDebug;
  }

  /**
   * This tests readNcCF profile files from ASA via
   * https://github.com/asascience-open/CFPointConventions stored in
   * unitTestDataDir/CFPointConventions.
   */
  @org.junit.jupiter.api.Test
  void testReadNcCFASAProfile() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCFASAProfile");
    Table table = new Table();
    String results, expected, fileName;

    /* */
    // *************** profile contiguous
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/profile/"
                    + "profile-Contiguous-Ragged-MultipleProfiles-H.3.4/"
                    + "profile-Contiguous-Ragged-MultipleProfiles-H.3.4.nc")
            .getPath();
    // String2.log("\n\n** Testing contiguous file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(18);
    expected =
        /*
         * pre 2012-10-02 was
         * "lat,lon,profile,time,z,temperature,humidity\n" +
         * "137.0,30.0,0,0,8.055641,32.962334,14.262256\n" +
         * "137.0,30.0,0,0,8.9350815,25.433783,41.722572\n" +
         * "137.0,30.0,0,0,5.1047354,4.0391192,44.34395\n" +
         * "137.0,30.0,0,0,4.2890472,6.0850625,13.220096\n" +
         * "137.0,30.0,0,0,1.9311341,32.794086,32.313293\n";
         */
        // z seems to be made up numbers. not in order (for a given profile) as one
        // would expect.
        "lat,lon,profile,time,z,temperature,humidity\n"
            + "34.0,115.0,0,0,9.913809,30.592709,32.71529\n"
            + "34.0,115.0,0,0,5.699307,17.442251,76.26051\n"
            + "34.0,115.0,0,0,0.617254,14.230382,13.789284\n"
            + "34.0,115.0,0,0,2.6114788,38.859676,21.792738\n"
            + "34.0,115.0,0,0,6.519849,28.003593,33.264217\n"
            + "34.0,115.0,0,0,8.975919,10.699942,61.52172\n"
            + "34.0,115.0,0,0,9.912431,32.747574,85.96188\n"
            + "34.0,115.0,0,0,7.5545244,18.109398,41.733406\n"
            + "34.0,115.0,0,0,7.568512,10.165248,84.50128\n"
            + "34.0,115.0,0,0,3.376015,0.48572874,5.2108083\n"
            + "11.0,95.0,1,3600,0.16332848,1.193263,87.431725\n"
            + "11.0,95.0,1,3600,4.9485574,31.53037,65.04175\n"
            + "11.0,95.0,1,3600,6.424919,11.956788,54.758873\n"
            + "11.0,95.0,1,3600,4.7111635,36.69692,50.6536\n"
            + "11.0,95.0,1,3600,6.854408,21.065716,83.941765\n"
            + "11.0,95.0,1,3600,9.321201,31.395382,17.139112\n"
            + "176.0,17.0,2,7200,7.2918577,17.65049,66.33111\n"
            + "176.0,17.0,2,7200,3.270435,35.854877,17.296724\n"
            + "...\n";

    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 58, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // test get outer+obs variables with outer constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,lon,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("11"));
    results = table.dataToString();
    expected =
        "profile,lat,lon,temperature\n"
            + "1,11.0,95.0,1.193263\n"
            + "1,11.0,95.0,31.53037\n"
            + "1,11.0,95.0,11.956788\n"
            + "1,11.0,95.0,36.69692\n"
            + "1,11.0,95.0,21.065716\n"
            + "1,11.0,95.0,31.395382\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test get outer+obs variables with obs constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,lon,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("11.956788"));
    results = table.dataToString();
    expected = "profile,lat,lon,temperature\n" + "1,11.0,95.0,11.956788\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test get obs variables with outer constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("11"));
    results = table.dataToString();
    expected =
        "lat,temperature\n"
            + "11.0,1.193263\n"
            + "11.0,31.53037\n"
            + "11.0,11.956788\n"
            + "11.0,36.69692\n"
            + "11.0,21.065716\n"
            + "11.0,31.395382\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** profile incomplete multidimensional ---
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/profile/"
                    + "profile-Incomplete-MultiDimensional-MultipleProfiles-H.3.2/"
                    + "profile-Incomplete-MultiDimensional-MultipleProfiles-H.3.2.nc")
            .getPath();
    // String2.log("\n\n** Testing incomplete\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(45);
    expected =
        "lat,lon,profile,time,alt,temperature,humidity,wind_speed\n"
            + "171.0,119.0,0,0,3.6755686,17.65279,-999.9,51.078896\n"
            + "171.0,119.0,0,0,0.26343155,6.2052555,-999.9,64.199974\n"
            + "171.0,119.0,0,0,3.175112,12.011641,-999.9,6.9850345\n"
            + "171.0,119.0,0,0,4.208357,21.89748,-999.9,58.273148\n"
            + "171.0,119.0,0,0,2.6554945,21.416033,-999.9,71.660774\n"
            + "171.0,119.0,0,0,4.9972143,1.4952343,-999.9,31.470207\n"
            + "171.0,119.0,0,0,1.9827757,21.466,-999.9,11.440447\n"
            + "171.0,119.0,0,0,4.1058283,14.191161,-999.9,21.072964\n"
            + "171.0,119.0,0,0,5.648934,7.727216,-999.9,20.63561\n"
            + "171.0,119.0,0,0,1.2512851,21.434706,-999.9,60.469204\n"
            + "171.0,119.0,0,0,9.600934,2.1928697,-999.9,71.77351\n"
            + "171.0,119.0,0,0,1.9799258,16.0188,-999.9,55.211063\n"
            + "171.0,119.0,0,0,1.2364764,3.242274,-999.9,11.2599\n"
            + "171.0,119.0,0,0,2.834809,39.97538,-999.9,84.81159\n"
            + "171.0,119.0,0,0,3.950956,19.135057,-999.9,29.651375\n"
            + "171.0,119.0,0,0,8.663035,36.685486,-999.9,16.686064\n"
            + "171.0,119.0,0,0,1.8081368,31.313751,-999.9,55.862072\n"
            + "171.0,119.0,0,0,7.7147174,22.89713,-999.9,55.927597\n"
            + "171.0,119.0,0,0,9.629576,18.616583,-999.9,68.66041\n"
            + "171.0,119.0,0,0,6.9754705,7.9321976,-999.9,60.648094\n"
            + "171.0,119.0,0,0,2.7991323,11.907311,-999.9,67.411575\n"
            + "171.0,119.0,0,0,1.5943866,29.448673,-999.9,79.15605\n"
            + "171.0,119.0,0,0,0.9762172,3.3020692,-999.9,85.00339\n"
            + "171.0,119.0,0,0,5.5088353,12.813819,-999.9,77.104706\n"
            + "171.0,119.0,0,0,7.2601357,38.730194,-999.9,18.446539\n"
            + "171.0,119.0,0,0,8.384121,19.790619,-999.9,74.80566\n"
            + "171.0,119.0,0,0,6.4686337,23.498947,-999.9,76.68345\n"
            + "171.0,119.0,0,0,2.0993211,21.344112,-999.9,28.282118\n"
            + "171.0,119.0,0,0,0.8403456,17.045395,-999.9,88.80201\n"
            + "171.0,119.0,0,0,9.251101,15.639243,-999.9,70.71877\n"
            + "171.0,119.0,0,0,1.3482393,9.54115,-999.9,59.91356\n"
            + "171.0,119.0,0,0,3.6940877,30.967232,-999.9,35.620453\n"
            + "171.0,119.0,0,0,6.3351345,6.0343504,-999.9,44.98056\n"
            + "171.0,119.0,0,0,6.3332343,20.940767,-999.9,76.89658\n"
            + "171.0,119.0,0,0,0.053762503,20.765089,-999.9,12.856414\n"
            + "171.0,119.0,0,0,1.0131614,12.508157,-999.9,69.99224\n"
            + "171.0,119.0,0,0,4.424666,37.28969,-999.9,24.69326\n"
            + "171.0,119.0,0,0,1.5825375,17.199543,-999.9,63.037647\n"
            + "171.0,119.0,0,0,3.072151,13.194056,-999.9,33.561863\n"
            + "171.0,119.0,0,0,5.897976,6.350154,-999.9,9.787908\n"
            + "171.0,119.0,0,0,1.6135278,22.95996,-999.9,85.10665\n"
            + "171.0,119.0,0,0,6.9384937,7.619196,-999.9,33.569344\n"
            + "155.0,158.0,1,3600,2.1733663,4.981018,-999.9,41.24567\n"
            + "155.0,158.0,1,3600,2.189715,16.313164,-999.9,8.15441\n"
            + "155.0,158.0,1,3600,9.445334,18.173727,-999.9,52.259445\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // 42 * 142 = 5964 obs spaces, so it is incomplete
    Test.ensureEqual(table.nRows(), 5754, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** profile indexed IMPORTANT - I didn't have sample of indexed
    // try {
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/profile/"
                    + "profile-Indexed-Ragged-MultipleProfiles-H.3.5/"
                    + "profile-Indexed-Ragged-MultipleProfiles-H.3.5.nc")
            .getPath();
    // String2.log("\n\n** Testing indexed file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    // String2.log(table.dataToString());
    results = table.dataToString(20);
    expected =
        "lat,lon,profile,time,z,temperature,humidity\n"
            + "93.0,71.0,0,0,0.38200212,23.69535,52.60904\n"
            + "93.0,71.0,0,0,1.200709,29.121883,12.060117\n"
            + "93.0,71.0,0,0,2.6969194,23.355228,9.943134\n"
            + "93.0,71.0,0,0,3.4035592,21.57062,75.10006\n"
            + "93.0,71.0,0,0,5.829337,2.9969826,17.760695\n"
            + "93.0,71.0,0,0,5.8626857,37.635395,86.32262\n"
            + "93.0,71.0,0,0,6.5773344,2.3481517,85.33706\n"
            + "93.0,71.0,0,0,7.7204447,5.337912,54.993973\n"
            + "93.0,71.0,0,0,8.301987,32.431896,88.71708\n"
            + "93.0,71.0,0,0,9.088309,30.518106,44.74581\n"
            + "45.0,151.0,1,3600,0.47979552,28.567852,65.933014\n"
            + "45.0,151.0,1,3600,0.594338,7.940218,79.38502\n"
            + "45.0,151.0,1,3600,4.0314445,20.808128,13.365513\n"
            + "45.0,151.0,1,3600,6.101271,4.62561,8.945877\n"
            + "45.0,151.0,1,3600,6.1228404,13.251722,50.431633\n"
            + "45.0,151.0,1,3600,8.454789,17.803867,4.852586\n"
            + "169.0,145.0,2,7200,1.9213479,7.1473145,11.227387\n"
            + "169.0,145.0,2,7200,3.328237,27.21546,29.352453\n"
            + "112.0,9.0,3,10800,0.009190708,6.3910594,56.909916\n"
            + "112.0,9.0,3,10800,0.013856917,13.634793,63.741573\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 100, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // "19,112.0,9.0,3,10800,0.013856917,13.634793,63.741573\n";
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile,temperature"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("3,13.634793"));
    results = table.dataToString();
    expected = "profile,temperature\n" + "3,13.634793\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // "19,112.0,9.0,3,10800,0.013856917,13.634793,63.741573\n";
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile,z"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("3,0.013856917"));
    results = table.dataToString();
    expected = "profile,z\n" + "3,0.013856917\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // String2.log("\n\n** Testing indexed file NO_DATA - inner");
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,lon,profile,time,z,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-10000"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // String2.log("\n\n** Testing indexed file NO_DATA - odd combo");
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,lon,profile,time,z,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat,z"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("93,0.594338")); // actual values, but never in this combination
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // String2.log("\n\n** Testing indexed file NO_DATA - outer");
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,lon,profile,time,z,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("humidity"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-10000"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");
    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e));
    // }

    // *************** profile orthogonal
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/profile/"
                    + "profile-Orthogonal-MultiDimensional-MultipleProfiles-H.3.1/"
                    + "profile-Orthogonal-MultiDimensional-MultipleProfiles-H.3.1.nc")
            .getPath();
    // String2.log("\n\n** Testing orthogonal file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(55);
    expected = // z[obs] is in the innerTable
        "lat,lon,profile,time,z,temperature,humidity\n"
            + "19.0,116.0,0,0,1.6315197,15.477672,45.439682\n"
            + "19.0,116.0,0,0,7.0598154,31.758614,35.987625\n"
            + "19.0,116.0,0,0,0.36953768,2.4893014,65.79051\n"
            + "19.0,116.0,0,0,7.5342026,26.857018,48.042828\n"
            + "19.0,116.0,0,0,7.404938,19.151163,35.629215\n"
            + "19.0,116.0,0,0,1.4442251,24.565704,37.29833\n"
            + "19.0,116.0,0,0,9.80883,15.455084,23.763685\n"
            + "19.0,116.0,0,0,8.060886,26.090511,14.579169\n"
            + "19.0,116.0,0,0,1.965906,8.010671,69.79476\n"
            + "19.0,116.0,0,0,9.60608,26.692741,78.83376\n"
            + "19.0,116.0,0,0,9.839138,39.378746,37.22304\n"
            + "19.0,116.0,0,0,6.2004266,14.685706,39.81143\n"
            + "19.0,116.0,0,0,6.9113455,7.344667,18.64804\n"
            + "19.0,116.0,0,0,8.798231,7.1495833,25.831097\n"
            + "19.0,116.0,0,0,2.3565977,0.25708458,32.442547\n"
            + "19.0,116.0,0,0,8.742956,34.86492,49.41099\n"
            + "19.0,116.0,0,0,8.557564,35.413876,66.573906\n"
            + "19.0,116.0,0,0,9.6161375,37.28068,4.6605506\n"
            + "19.0,116.0,0,0,6.610992,5.4654717,60.635574\n"
            + "19.0,116.0,0,0,1.936887,33.513893,82.823166\n"
            + "19.0,116.0,0,0,3.0184858,31.41321,75.51568\n"
            + "19.0,116.0,0,0,2.5581324,15.092895,79.2067\n"
            + "19.0,116.0,0,0,7.1288857,20.573462,27.601343\n"
            + "19.0,116.0,0,0,1.5220404,0.5649648,3.6447735\n"
            + "19.0,116.0,0,0,3.276416,27.345316,62.10269\n"
            + "19.0,116.0,0,0,0.40930283,27.671362,79.762955\n"
            + "19.0,116.0,0,0,2.4845016,31.252121,61.57929\n"
            + "19.0,116.0,0,0,9.366717,9.342631,78.63049\n"
            + "19.0,116.0,0,0,0.3365049,20.81806,29.236477\n"
            + "19.0,116.0,0,0,7.646478,3.1961684,7.8138685\n"
            + "19.0,116.0,0,0,5.075439,36.427265,20.879707\n"
            + "19.0,116.0,0,0,5.1594234,18.314194,6.4109855\n"
            + "19.0,116.0,0,0,2.1663764,10.056105,5.798549\n"
            + "19.0,116.0,0,0,9.028424,5.7192965,56.243206\n"
            + "19.0,116.0,0,0,9.031402,13.884695,36.763905\n"
            + "19.0,116.0,0,0,5.26929,3.5693107,84.04594\n"
            + "19.0,116.0,0,0,2.6247969,8.933488,28.76576\n"
            + "19.0,116.0,0,0,9.745737,24.357897,76.431816\n"
            + "19.0,116.0,0,0,3.722143,17.96677,18.759092\n"
            + "19.0,116.0,0,0,1.9264901,28.71267,52.148735\n"
            + "19.0,116.0,0,0,3.9815784,35.91171,33.082714\n"
            + "19.0,116.0,0,0,4.657818,31.10753,65.25383\n"
            + "109.0,178.0,1,3600,1.6315197,26.582031,10.312429\n"
            + "109.0,178.0,1,3600,7.0598154,4.909754,50.415916\n"
            + "109.0,178.0,1,3600,0.36953768,30.069138,36.845417\n"
            + "109.0,178.0,1,3600,7.5342026,3.341837,52.53064\n"
            + "109.0,178.0,1,3600,7.404938,36.832874,81.62572\n"
            + "109.0,178.0,1,3600,1.4442251,21.88992,78.833565\n"
            + "109.0,178.0,1,3600,9.80883,25.902088,50.43351\n"
            + "109.0,178.0,1,3600,8.060886,30.653927,81.53324\n"
            + "109.0,178.0,1,3600,1.965906,0.8834069,86.67266\n"
            + "109.0,178.0,1,3600,9.60608,27.2307,74.25348\n"
            + "109.0,178.0,1,3600,9.839138,15.706074,86.22133\n"
            + "109.0,178.0,1,3600,6.2004266,34.751484,79.71265\n"
            + "109.0,178.0,1,3600,6.9113455,16.43026,30.387852\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 5964, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("profile,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    /* */
    // Table.debugMode = oDebug;
  }

  /**
   * This tests readNcCF timeseries files from ASA via
   * https://github.com/asascience-open/CFPointConventions stored in
   * unitTestDataDir/CFPointConventions.
   */
  @org.junit.jupiter.api.Test
  void testReadNcCFASATimeSeries() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCFASATimeseries");
    Table table = new Table();
    String results, expected, fileName;

    // *************** timeseries orthogonal
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeries/"
                    + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/"
                    + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.nc")
            .getPath();
    // String2.log("\n\n** Testing orthogonal file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    // !!! obs vars are temperature[time=100][station=10]
    // so outer=time and inner is station!
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(12);
    expected =
        "lat,lon,station_name,alt,time,temperature,humidity\n"
            + "8.0,146.0,Station-0,0.8488673,0,18.618036,27.177536\n"
            + "4.0,53.0,Station-1,1.8478156,0,13.216496,83.71079\n"
            + "90.0,159.0,Station-2,3.4614673,0,39.300182,44.69293\n"
            + "55.0,25.0,Station-3,4.8902116,0,17.008652,2.3659434\n"
            + "115.0,30.0,Station-4,9.45969,0,24.951536,7.1026664\n"
            + "165.0,125.0,Station-5,0.17808062,0,35.995247,41.411594\n"
            + "143.0,175.0,Station-6,8.85507,0,24.334364,39.776123\n"
            + "157.0,175.0,Station-7,0.47320434,0,33.077255,1.1665242\n"
            + "101.0,80.0,Station-8,7.470208,0,6.9397545,72.75068\n"
            + "167.0,57.0,Station-9,0.6709764,0,28.991974,71.65753\n"
            + "8.0,146.0,Station-0,0.8488673,3600,3.0675685,53.43748\n"
            + "4.0,53.0,Station-1,1.8478156,3600,37.31892,46.79294\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 1000, "");
    results = table.columnAttributes(5).toString(); // temperature
    expected =
        "    coordinates=lat lon alt\n"
            + // this is what source has. But it should include time.
            "    long_name=Air Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=air_temperature\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // read just inner [station] vars
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "station_name,lat,lon\n"
            + "Station-0,8.0,146.0\n"
            + "Station-1,4.0,53.0\n"
            + "Station-2,90.0,159.0\n"
            + "Station-3,55.0,25.0\n"
            + "Station-4,115.0,30.0\n"
            + "Station-5,165.0,125.0\n"
            + "Station-6,143.0,175.0\n"
            + "Station-7,157.0,175.0\n"
            + "Station-8,101.0,80.0\n"
            + "Station-9,167.0,57.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just inner [station] vars, with constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV(">"),
        StringArray.fromCSV("150"));
    results = table.dataToString();
    expected =
        "station_name,lat,lon\n"
            + "Station-5,165.0,125.0\n"
            + "Station-7,157.0,175.0\n"
            + "Station-9,167.0,57.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer [time] vars
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(5);
    expected = "time\n" + "0\n" + "3600\n" + "7200\n" + "10800\n" + "14400\n" + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 100, "");

    // read just outer [time] vars, with constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,time"),
        StringArray.fromCSV(">,<"),
        StringArray.fromCSV("7000,11000"));
    results = table.dataToString();
    expected = "time\n" + "7200\n" + "10800\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer+inner [time][station] vars
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon,time"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "station_name,lat,lon,time\n"
            + "Station-0,8.0,146.0,0\n"
            + "Station-1,4.0,53.0,0\n"
            + "Station-2,90.0,159.0,0\n"
            + "Station-3,55.0,25.0,0\n"
            + "Station-4,115.0,30.0,0\n"
            + "Station-5,165.0,125.0,0\n"
            + "Station-6,143.0,175.0,0\n"
            + "Station-7,157.0,175.0,0\n"
            + "Station-8,101.0,80.0,0\n"
            + "Station-9,167.0,57.0,0\n"
            + "Station-0,8.0,146.0,3600\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 10 * 100, "");

    expected =
        "Station-0,8.0,146.0,345600\n"
            + "Station-1,4.0,53.0,345600\n"
            + "Station-2,90.0,159.0,345600\n"
            + "Station-3,55.0,25.0,345600\n"
            + "Station-4,115.0,30.0,345600\n"
            + "Station-5,165.0,125.0,345600\n"
            + "Station-6,143.0,175.0,345600\n"
            + "Station-7,157.0,175.0,345600\n"
            + "Station-8,101.0,80.0,345600\n"
            + "Station-9,167.0,57.0,345600\n";
    int po = results.indexOf(expected);
    Test.ensureTrue(po > 0, "po=" + po);
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // read just outer+inner [time][station] vars, with outer [time] constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("345600"));
    results = table.dataToString();
    expected =
        "station_name,lat,lon,time\n"
            + "Station-0,8.0,146.0,345600\n"
            + "Station-1,4.0,53.0,345600\n"
            + "Station-2,90.0,159.0,345600\n"
            + "Station-3,55.0,25.0,345600\n"
            + "Station-4,115.0,30.0,345600\n"
            + "Station-5,165.0,125.0,345600\n"
            + "Station-6,143.0,175.0,345600\n"
            + "Station-7,157.0,175.0,345600\n"
            + "Station-8,101.0,80.0,345600\n"
            + "Station-9,167.0,57.0,345600\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer+inner [time][station] vars, with inner [station] constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("165"));
    results = table.dataToString();
    expected =
        "station_name,lat,lon,time\n"
            + "Station-5,165.0,125.0,0\n"
            + "Station-5,165.0,125.0,3600\n"
            + "Station-5,165.0,125.0,7200\n"
            + "Station-5,165.0,125.0,10800\n"
            + "Station-5,165.0,125.0,14400\n"
            + "Station-5,165.0,125.0,18000\n"
            + "Station-5,165.0,125.0,21600\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 100, "");

    // read just outer+inner [time][station] vars, with outer and inner constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,lat"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("345600,165"));
    results = table.dataToString();
    expected = "station_name,lat,lon,time\n" + "Station-5,165.0,125.0,345600\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer+inner+obs vars, with outer and inner constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV(""),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,lat"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("345600,165"));
    results = table.dataToString();
    expected =
        "lat,lon,station_name,alt,time,temperature,humidity\n"
            + "165.0,125.0,Station-5,0.17808062,345600,38.457962,28.075706\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer+obs vars, with outer and inner constraint
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,lat"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("345600,165"));
    results = table.dataToString();
    expected = "lat,time,temperature,humidity\n" + "165.0,345600,38.457962,28.075706\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just inner+obs vars, with outer and inner constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,lon,station_name,time,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,lat"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("345600,165"));
    results = table.dataToString();
    expected =
        "lat,lon,station_name,time,temperature\n" + "165.0,125.0,Station-5,345600,38.457962\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just obs vars, with outer and inner constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,lat"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("345600,165"));
    results = table.dataToString();
    expected = "time,lat,temperature\n" + "345600,165.0,38.457962\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** timeseries incomplete multidimensional ---
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeries/"
                    + "timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2/"
                    + "timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.nc")
            .getPath();
    // String2.log("\n\n** Testing incomplete file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(24);
    expected =
        "lat,lon,station_elevation,station_info,station_name,alt,time,temperature,humidity\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,0,17.0,56.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,3600,7.0,49.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,7200,19.0,86.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,10800,5.0,81.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,14400,0.0,55.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,18000,10.0,9.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,21600,32.0,57.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,25200,39.0,39.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,28800,39.0,68.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,32400,29.0,6.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,36000,26.0,12.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,39600,24.0,72.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,43200,14.0,80.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,46800,38.0,52.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,50400,35.0,46.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,54000,33.0,48.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,57600,34.0,85.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,61200,27.0,3.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,64800,37.0,61.0\n"
            + "121.0,81.0,4.859895,0,Station-0,1.9358816,68400,0.0,0.0\n"
            + "150.0,73.0,2.6002314,1,Station-1,4.052759,0,25.0,73.0\n"
            + "150.0,73.0,2.6002314,1,Station-1,4.052759,3600,29.0,74.0\n"
            + "150.0,73.0,2.6002314,1,Station-1,4.052759,7200,33.0,88.0\n"
            + "150.0,73.0,2.6002314,1,Station-1,4.052759,10800,25.0,3.0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 200, "");
    results = table.columnAttributes(7).toString(); // temperature
    expected =
        "    coordinates=time lat lon alt\n"
            + "    long_name=Air Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=air_temperature\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // read just outer [station] vars
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "station_name,lat,lon\n"
            + "Station-0,121.0,81.0\n"
            + "Station-1,150.0,73.0\n"
            + "Station-2,107.0,152.0\n"
            + "Station-3,117.0,143.0\n"
            + "Station-4,107.0,11.0\n"
            + "Station-5,161.0,100.0\n"
            + "Station-6,150.0,169.0\n"
            + "Station-7,176.0,85.0\n"
            + "Station-8,83.0,126.0\n"
            + "Station-9,84.0,170.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer [station] vars, with constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,lon"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV(">"),
        StringArray.fromCSV("155"));
    results = table.dataToString();
    expected = "station_name,lat,lon\n" + "Station-5,161.0,100.0\n" + "Station-7,176.0,85.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // There are no just inner[obs] variables

    // read just outer+obs vars, with outer constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("150"));
    results = table.dataToString();
    expected =
        // metadata mv=-999.9 for temp and humidity, so 9e36 below are "valid" values
        "lat,time,temperature,humidity\n"
            + "150.0,0,25.0,73.0\n"
            + "150.0,3600,29.0,74.0\n"
            + "150.0,7200,33.0,88.0\n"
            + "150.0,10800,25.0,3.0\n"
            + "150.0,14400,2.0,70.0\n"
            + "150.0,18000,9.0,37.0\n"
            + "150.0,21600,10.0,45.0\n"
            + "150.0,25200,33.0,24.0\n"
            + "150.0,28800,1.0,43.0\n"
            + "150.0,32400,26.0,0.0\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,-2147483647,9.96921E36,9.96921E36\n"
            + "150.0,0,18.0,4.0\n"
            + "150.0,3600,18.0,83.0\n"
            + "150.0,7200,27.0,10.0\n"
            + "150.0,10800,33.0,43.0\n"
            + "150.0,14400,34.0,31.0\n"
            + "150.0,18000,0.0,69.0\n"
            + "150.0,21600,27.0,34.0\n"
            + "150.0,25200,3.0,41.0\n"
            + "150.0,28800,38.0,14.0\n"
            + "150.0,32400,20.0,5.0\n"
            + "150.0,36000,26.0,48.0\n"
            + "150.0,39600,11.0,29.0\n"
            + "150.0,43200,22.0,60.0\n"
            + "150.0,46800,39.0,63.0\n"
            + "150.0,50400,27.0,18.0\n"
            + "150.0,54000,26.0,84.0\n"
            + "150.0,57600,26.0,71.0\n"
            + "150.0,61200,33.0,25.0\n"
            + "150.0,64800,27.0,17.0\n"
            + "150.0,68400,17.0,79.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer+obs vars, with outer + obs constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,time,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat,humidity"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("150,43"));
    results = table.dataToString();
    expected =
        "lat,time,temperature,humidity\n" + "150.0,28800,1.0,43.0\n" + "150.0,10800,33.0,43.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just outer+obs vars,
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,temperature,humidity"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,temperature"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("7200,33"));
    results = table.dataToString();
    expected =
        // "22,150.0,73.0,2.6002314,1,Station-1,4.052759,7200,33.0,88.0\n" + from above
        "time,temperature,humidity\n" + "7200,33.0,88.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just obs vars, with outer and obs constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,lon,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,lon"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("7200,73"));
    results = table.dataToString();
    expected = "time,lon,alt,temperature\n" + "7200,73.0,4.052759,33.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    /* */
    // read just obs vars, with outer and obs constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,lon,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lon,temperature"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("73,33"));
    results = table.dataToString();
    expected =
        "time,lon,alt,temperature\n" + "7200,73.0,4.052759,33.0\n" + "25200,73.0,4.052759,33.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read just obs vars, with outer and obs constraint
    table.readNcCF(
        fileName,
        StringArray.fromCSV("time,lon,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time,temperature"),
        StringArray.fromCSV("=,="),
        StringArray.fromCSV("7200,33"));
    results = table.dataToString();
    expected = "time,lon,alt,temperature\n" + "7200,73.0,4.052759,33.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Table.debugMode = oDebug;
  }

  /**
   * This tests readNcCF trajectory files from ASA via
   * https://github.com/asascience-open/CFPointConventions stored in
   * unitTestDataDir/CFPointConventions.
   */
  @org.junit.jupiter.api.Test
  void testReadNcCFASATrajectory() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCFASATrajectory");
    Table table = new Table();
    String results, expected, fileName;

    /* */
    // *************** trajectory contiguous
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/trajectory/"
                    + "trajectory-Contiguous-Ragged-MultipleTrajectories-H.4.3/"
                    + "trajectory-Contiguous-Ragged-MultipleTrajectories-H.4.3.nc")
            .getPath();
    // String2.log("\n\n** Testing contiguous file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(20);
    expected =
        "lat,lon,trajectory_info,trajectory_name,time,z,temperature,humidity\n"
            + "16.937433,-35.901237,0,Trajectory0,0,0.0,18.559397,46.487503\n"
            + "32.011345,-8.81588,0,Trajectory0,3600,1.0,34.649773,16.22458\n"
            + "3.137092,-64.15942,0,Trajectory0,7200,2.0,35.318504,77.41457\n"
            + "10.783036,-11.503419,0,Trajectory0,10800,3.0,19.39111,56.601\n"
            + "4.6016994,-6.416601,0,Trajectory0,14400,4.0,5.4162874,62.606712\n"
            + "25.337688,-69.37197,0,Trajectory0,18000,5.0,2.604784,16.390015\n"
            + "30.219189,-71.78619,0,Trajectory0,21600,6.0,22.968603,62.276855\n"
            + "5.3421707,-29.245968,0,Trajectory0,25200,7.0,8.609019,14.976101\n"
            + "25.687958,-57.089973,0,Trajectory0,28800,8.0,9.202528,79.17113\n"
            + "31.82367,-58.56237,0,Trajectory0,32400,9.0,1.5670301,26.49425\n"
            + "23.310976,-3.997997,0,Trajectory0,36000,10.0,23.187065,64.34719\n"
            + "43.486816,-62.39688,0,Trajectory0,39600,11.0,37.44155,29.570276\n"
            + "44.56024,-54.139122,0,Trajectory0,43200,12.0,11.75348,72.36402\n"
            + "42.48622,-42.518707,1,Trajectory1,0,0.0,20.665886,67.27393\n"
            + "32.187572,-73.20317,1,Trajectory1,3600,1.0,26.498121,79.754486\n"
            + "6.4802227,-72.74957,1,Trajectory1,7200,2.0,17.64227,70.126625\n"
            + "38.596996,-67.64374,1,Trajectory1,10800,3.0,23.615097,59.626125\n"
            + "24.085066,-63.833694,1,Trajectory1,14400,4.0,30.743101,35.862038\n"
            + "24.221394,-57.373817,1,Trajectory1,18000,5.0,39.391495,28.661589\n"
            + "22.637892,-47.858807,1,Trajectory1,21600,6.0,1.2310536,55.708595\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 163, "");
    results = table.columnAttributes(5).toString(); // temperature
    expected =
        "    axis=Z\n"
            + "    long_name=height above mean sea level\n"
            + "    missing_value=-999.0f\n"
            + "    positive=up\n"
            + "    standard_name=altitude\n"
            + "    units=m\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** trajectory single multidimensional ---
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/trajectory/"
                    + "trajectory-Incomplete-Multidimensional-SingleTrajectory-H.4.2/"
                    + "trajectory-Incomplete-Multidimensional-SingleTrajectory-H.4.2.nc")
            .getPath();
    // String2.log("\n\n** Testing single multidimensional file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(5);
    expected =
        "lat,lon,trajectory_info,trajectory_name,time,z,temperature,humidity\n"
            + "42.003387,-7.9335957,0,Trajectory1,0,0.0,12.522581,35.668747\n"
            + "8.972063,-46.335754,0,Trajectory1,3600,1.0,25.658121,1.0647067\n"
            + "25.841967,-49.1959,0,Trajectory1,7200,2.0,35.43442,13.059927\n"
            + "35.699753,-40.790943,0,Trajectory1,10800,3.0,35.752117,48.576355\n"
            + "11.132234,-25.553247,0,Trajectory1,14400,4.0,6.082586,64.91749\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 100, "");
    results = table.columnAttributes(table.findColumnNumber("temperature")).toString();
    expected =
        "    coordinates=time lat lon z\n"
            + "    long_name=Air Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=air_temperature\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** trajectory indexed ---
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/trajectory/"
                    + "trajectory-Indexed-Ragged-MultipleTrajectories-H.4.4/"
                    + "trajectory-Indexed-Ragged-MultipleTrajectories-H.4.4.nc")
            .getPath();
    // String2.log("\n\n** Testing indexed file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(5);
    expected =
        "lat,lon,trajectory_info,trajectory_name,time,z,temperature,humidity\n"
            + "11.256147,-5.989336,8,Trajectory8,72000,12.518082,14.902713,37.237553\n"
            + "26.104128,-2.6626983,3,Trajectory3,111600,7.372036,24.243849,12.862466\n"
            + "22.414213,-23.53803,4,Trajectory4,68400,5.7999315,1.4940661,20.668322\n"
            + "22.181162,-34.355854,4,Trajectory4,122400,20.127024,6.8310843,55.93755\n"
            + "2.177301,-58.388607,5,Trajectory5,162000,1.764841,27.893003,28.2276\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 213, "");
    results = table.columnAttributes(6).toString(); // temperature
    expected =
        "    coordinates=time lat lon z\n"
            + "    long_name=Air Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=air_temperature\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    /* */
    // Table.debugMode = oDebug;
  }

  /**
   * This tests readNcCF timeSeriesProfile files from ASA via
   * https://github.com/asascience-open/CFPointConventions stored in
   * unitTestDataDir/CFPointConventions.
   */
  @org.junit.jupiter.api.Test
  void testReadNcCFASATimeSeriesProfile() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCFASATimeSeriesProfile");
    Table table = new Table();
    String results, expected, fileName;
    String orthoMultiDimH51FileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeriesProfile/"
                    + "timeSeriesProfile-Orthogonal-Multidimensional-MultipeStations-H.5.1/"
                    + "timeSeriesProfile-Orthogonal-Multidimensional-MultipeStations-H.5.1.nc")
            .getPath();
    String raggedSingleStationFileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeriesProfile/"
                    + "timeSeriesProfile-Ragged-SingleStation-H.5.3/"
                    + "timeSeriesProfile-Ragged-SingleStation-H.5.3.nc")
            .getPath();

    // *************** timeSeriesProfile multidimensional ---
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeriesProfile/"
                    + "timeSeriesProfile-Multidimensional-MultipeStations-H.5.1/"
                    + "timeSeriesProfile-Multidimensional-MultipeStations-H.5.1.nc")
            .getPath();
    // String2.log("\n\n** Testing incomplete file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(5);
    expected =
        "lat,lon,station_info,station_name,alt,time,temperature\n"
            + "37.5,-76.5,0,Station1,0.0,0,0.0\n"
            + "37.5,-76.5,0,Station1,2.5,0,0.1\n"
            + "37.5,-76.5,0,Station1,5.0,0,0.2\n"
            + "37.5,-76.5,0,Station1,7.5,0,0.3\n"
            + "37.5,-76.5,0,Station1,10.0,0,0.4\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 240, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** timeSeriesProfile multidimensional single station
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeriesProfile/"
                    + "timeSeriesProfile-Multidimensional-SingleStation-H.5.2/"
                    + "timeSeriesProfile-Multidimensional-SingleStation-H.5.2.nc")
            .getPath();
    // String2.log("\n\n** Testing contiguous file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString();
    expected =
        "lat,lon,station_info,station_name,alt,time,temperature\n"
            + "37.5,-76.5,0,Station1,0.0,0,0.0\n"
            + "37.5,-76.5,0,Station1,2.5,0,0.1\n"
            + "37.5,-76.5,0,Station1,5.0,0,0.2\n"
            + "37.5,-76.5,0,Station1,7.5,0,0.3\n"
            + "37.5,-76.5,0,Station1,10.0,0,0.4\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected =
        "37.5,-76.5,0,Station1,287.5,10800,11.5\n"
            + "37.5,-76.5,0,Station1,290.0,10800,11.6\n"
            + "37.5,-76.5,0,Station1,292.5,10800,11.7\n"
            + "37.5,-76.5,0,Station1,295.0,10800,11.8\n"
            + "37.5,-76.5,0,Station1,297.5,10800,11.9\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 120, "");
    results = table.columnAttributes(table.findColumnNumber("alt")).toString();
    expected = "    axis=Z\n" + "    positive=up\n" + "    units=m\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = table.columnAttributes(table.findColumnNumber("temperature")).toString();
    expected =
        "    coordinates=time lat lon alt\n"
            + "    long_name=Water Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** timeSeriesProfile orthogonal multidimensional ---
    // !!! IMPORTANT/ONLY test of file with variable[innerDim] for innerTable
    // and variable[time][z][station] which is allowed, ordering
    fileName = orthoMultiDimH51FileName;
    // String2.log("\n\n** Testing incomplete file\n" + " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(12);
    expected =
        "lat,lon,alt,station_info,station_name,time,temperature,humidity\n"
            + "37.5,-76.5,0.0,0,Station1,0,15.698009,89.70879\n"
            + "32.5,-78.3,0.0,1,Station2,0,8.11997,33.637585\n"
            + "37.5,-76.5,10.0,0,Station1,0,10.9166565,55.78947\n"
            + "32.5,-78.3,10.0,1,Station2,0,39.356647,65.43795\n"
            + "37.5,-76.5,20.0,0,Station1,0,15.666663,50.176994\n"
            + "32.5,-78.3,20.0,1,Station2,0,33.733116,58.14976\n"
            + "37.5,-76.5,30.0,0,Station1,0,1.1587523,36.855045\n"
            + "32.5,-78.3,30.0,1,Station2,0,4.65479,63.862186\n"
            + "37.5,-76.5,0.0,0,Station1,3600,31.059647,65.01694\n"
            + "32.5,-78.3,0.0,1,Station2,3600,33.374344,22.771135\n"
            + "37.5,-76.5,10.0,0,Station1,3600,5.680936,35.675472\n"
            + "32.5,-78.3,10.0,1,Station2,3600,17.763374,38.54674\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 800, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    fileName = orthoMultiDimH51FileName;
    // String2.log("\n\n** Testing incomplete file with constraints\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(orthoMultiDimH51FileName, "-h"));
    table.readNcCF(
        fileName,
        null,
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("Station1"));
    results = table.dataToString(12);
    expected =
        "lat,lon,alt,station_info,station_name,time,temperature,humidity\n"
            + "37.5,-76.5,0.0,0,Station1,0,15.698009,89.70879\n"
            + "37.5,-76.5,10.0,0,Station1,0,10.9166565,55.78947\n"
            + "37.5,-76.5,20.0,0,Station1,0,15.666663,50.176994\n"
            + "37.5,-76.5,30.0,0,Station1,0,1.1587523,36.855045\n"
            + "37.5,-76.5,0.0,0,Station1,3600,31.059647,65.01694\n"
            + "37.5,-76.5,10.0,0,Station1,3600,5.680936,35.675472\n"
            + "37.5,-76.5,20.0,0,Station1,3600,24.156359,45.77856\n"
            + "37.5,-76.5,30.0,0,Station1,3600,25.934822,35.178967\n"
            + "37.5,-76.5,0.0,0,Station1,7200,6.518481,12.735875\n"
            + "37.5,-76.5,10.0,0,Station1,7200,4.463567,47.44697\n"
            + "37.5,-76.5,20.0,0,Station1,7200,29.448772,20.438272\n"
            + "37.5,-76.5,30.0,0,Station1,7200,37.245636,62.655357\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 400, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // test just variable[obs] (interiorTable)
    // String2.log("\n\n** Testing incomplete file\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("lat,lon,station_name,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("Station1"));
    results = table.dataToString();
    expected = "lat,lon,station_name\n" + "37.5,-76.5,Station1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 1, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ********** NO_DATA
    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA,
    // outer/inner/obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51FileName just outer/inner/obsVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("time,zztop,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // outerVar
    // String2.log("\n\n** Testing orthoMultiDimH51FileName just outerVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // innerVar
    // String2.log("\n\n** Testing orthoMultiDimH51FileName just innerVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("alt,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10000"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just obsVar, NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // outerVar* and innerVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer*/innerVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("zztop,time,alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // outerVar and innerVar*
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer/innerVar*,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("zztop,time,alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10000"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // outerVar* and obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer*/obsVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("time,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // outerVar and obsVar*
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer/obsVar*,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("time,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // innerVar* and obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just inner*/obsVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("alt,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10000"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile orthoMultiDimH51FileName --- NO_DATA, just
    // innerVar and obsVar*
    // String2.log("\n\n** Testing orthoMultiDimH51File just inner/obsVar*,
    // NO_DATA\n" +
    // " " + orthoMultiDimH51FileName);
    table.readNcCF(
        orthoMultiDimH51FileName,
        StringArray.fromCSV("alt,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *******
    // *************** timeSeriesProfile ragged ---
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/timeSeriesProfile/"
                    + "timeSeriesProfile-Ragged-MultipeStations-H.5.3/"
                    + "timeSeriesProfile-Ragged-MultipeStations-H.5.3.nc")
            .getPath();
    // String2.log("\n\n** Testing timeSeriesProfile ragged file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString();
    expected =
        // file has:
        // lat, lon, station_info, station_name, profile, time, station_index, row_size,
        // height, temperature
        "lat,lon,station_info,station_name,profile,time,height,temperature\n"
            + "37.5,-76.5,0,Station1,0,0,0.5,6.7\n"
            + "37.5,-76.5,0,Station1,0,0,1.5,6.9\n"
            + "32.5,-78.3,1,Station2,1,3600,0.8,7.6\n"
            + "32.5,-78.3,1,Station2,1,3600,1.8,7.7\n"
            + "32.5,-78.3,1,Station2,1,3600,2.8,7.9\n"
            + "32.5,-78.3,1,Station2,1,3600,3.8,8.0\n"
            + "37.5,-76.5,0,Station1,2,7200,0.5,6.7\n"
            + "37.5,-76.5,0,Station1,2,7200,1.5,7.0\n"
            + "32.5,-78.3,1,Station2,3,10800,0.8,8.2\n"
            + "32.5,-78.3,1,Station2,3,10800,1.8,7.7\n"
            + "32.5,-78.3,1,Station2,3,10800,2.8,7.8\n"
            + "32.5,-78.3,1,Station2,3,10800,3.8,9.1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 12, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** timeSeriesProfile ragged single station ---
    // This is ONLY TEST FILE of nLevels=2 single station (outerTable are scalar
    // vars)!!!!!!!!
    // Note also the lack of a stationIndex var with
    // instance_dimension="someDimension"
    // since there is no instance_dimension
    fileName = raggedSingleStationFileName;
    // String2.log("\n\n** Testing raggedSingleStationFile\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString();
    expected =
        "lat,lon,station_info,station_name,profile,time,height,temperature\n"
            + "37.5,-76.5,0,Station1,0,0,0.5,6.7\n"
            + "37.5,-76.5,0,Station1,0,0,1.5,6.9\n"
            + "37.5,-76.5,0,Station1,1,3600,0.5,6.8\n"
            + "37.5,-76.5,0,Station1,1,3600,1.5,7.9\n"
            + "37.5,-76.5,0,Station1,2,7200,0.5,6.8\n"
            + "37.5,-76.5,0,Station1,2,7200,1.5,7.9\n"
            + "37.5,-76.5,0,Station1,2,7200,2.5,8.4\n"
            + "37.5,-76.5,0,Station1,3,10800,0.5,5.7\n"
            + "37.5,-76.5,0,Station1,3,10800,1.5,9.2\n"
            + "37.5,-76.5,0,Station1,3,10800,2.5,8.3\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 10, "");
    results = table.columnAttributes(table.findColumnNumber("profile")).toString();
    expected = "    cf_role=profile_id\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name,lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("station_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("height"),
        0, // standardizeWhat=0
        StringArray.fromCSV("height"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** timeSeriesProfile ragged single station --- just outerVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outerVar\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("zztop,lat,lon,station_info,station_name"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected = "lat,lon,station_info,station_name\n" + "37.5,-76.5,0,Station1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 1, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station latitude\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** timeSeriesProfile ragged single station --- just innerVar
    // String2.log("\n\n** Testing raggedSingleStationFile just innerVar\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("time,zztop,profile"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected = "time,profile\n" + "0,0\n" + "3600,1\n" + "7200,2\n" + "10800,3\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 4, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=time\n"
            + "    missing_value=-999i\n"
            + "    standard_name=time\n"
            + "    units=seconds since 1990-01-01 00:00:00\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** timeSeriesProfile ragged single station --- just outerVar and
    // innerVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outerVar and
    // innerVar\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("station_info,station_name,lon,lat,time,profile,"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "station_info,station_name,lon,lat,time,profile\n"
            + "0,Station1,-76.5,37.5,0,0\n"
            + "0,Station1,-76.5,37.5,3600,1\n"
            + "0,Station1,-76.5,37.5,7200,2\n"
            + "0,Station1,-76.5,37.5,10800,3\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 4, "");
    results = table.columnAttributes(0).toString();
    expected = "    long_name=station info\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** timeSeriesProfile ragged single station --- just outerVar and
    // obsVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outerVar and
    // obsVar\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("station_info,temperature,zztop,station_name,"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("8"));
    results = table.dataToString();
    expected =
        "station_info,temperature,station_name\n"
            + "0,8.4,Station1\n"
            + "0,9.2,Station1\n"
            + "0,8.3,Station1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 3, "");
    results = table.columnAttributes(0).toString();
    expected = "    long_name=station info\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** timeSeriesProfile ragged single station --- just innerVar and
    // obsVar
    // String2.log("\n\n** Testing raggedSingleStationFile just innerVar and
    // obsVar\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("temperature,zztop,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("8"));
    results = table.dataToString();
    expected = "temperature,time\n" + "8.4,7200\n" + "9.2,10800\n" + "8.3,10800\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 3, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    coordinates=time lat lon height\n"
            + "    long_name=Water Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=sea_water_temperature\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ********** NO_DATA
    // ********** timeSeriesProfile ragged single station --- NO_DATA,
    // outer/inner/obsVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outer/inner/obsVar,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("station_info,zztop,profile,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just outerVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outerVar, NO_DATA\n"
    // +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("zztop,station_info"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_info"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just innerVar
    // String2.log("\n\n** Testing raggedSingleStationFile just innerVar, NO_DATA\n"
    // +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("profile,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just obsVar
    // String2.log("\n\n** Testing raggedSingleStationFile just obsVar, NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just
    // outerVar* and innerVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outer*/innerVar,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("zztop,station_info,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_info"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just outerVar
    // and innerVar*
    // String2.log("\n\n** Testing raggedSingleStationFile just outer/innerVar*,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("zztop,station_info,profile"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just
    // outerVar* and obsVar
    // String2.log("\n\n** Testing raggedSingleStationFile just outer*/obsVar,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("temperature,station_info,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("station_info"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just outerVar
    // and obsVar*
    // String2.log("\n\n** Testing raggedSingleStationFile just outer/obsVar*,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("temperature,station_info,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just
    // innerVar* and obsVar
    // String2.log("\n\n** Testing raggedSingleStationFile just inner*/obsVar,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("temperature,profile,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("profile"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-1"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** timeSeriesProfile ragged single station --- NO_DATA, just innerVar
    // and obsVar*
    // String2.log("\n\n** Testing raggedSingleStationFile just inner/obsVar*,
    // NO_DATA\n" +
    // " " + raggedSingleStationFileName);
    table.readNcCF(
        raggedSingleStationFileName,
        StringArray.fromCSV("temperature,profile,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    /* */
    // Table.debugMode = oDebug;
  }

  /**
   * This tests readNcCF trajectoryProfile files from ASA via
   * https://github.com/asascience-open/CFPointConventions stored in
   * unitTestDataDir/CFPointConventions.
   */
  @org.junit.jupiter.api.Test
  void testReadNcCFASATrajectoryProfile() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    boolean oDebug = Table.debugMode;
    // Table.debugMode = true;
    // String2.log("\n*** Table.testReadNcCFASATrajectoryProfile");
    Table table = new Table();
    String results, expected, fileName;
    String orthoMultiDimH61FileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/trajectoryProfile/"
                    + "trajectoryProfile-Multidimensional-MultipleTrajectories-H.6.1/"
                    + "trajectoryProfile-Multidimensional-MultipleTrajectories-H.6.1.nc")
            .getPath();
    String raggedMultipleStationFileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/trajectoryProfile/"
                    + "trajectoryProfile-Ragged-MultipleTrajectories-H.6.3/"
                    + "trajectoryProfile-Ragged-MultipleTrajectories-H.6.3.nc")
            .getPath();

    /* */
    // *************** trajectoryProfile multidimensional single station
    // try {
    fileName =
        TableTests.class
            .getResource(
                "/data/CFPointConventions/trajectoryProfile/"
                    + "trajectoryProfile-Multidimensional-SingleTrajectory-H.6.2/"
                    + "trajectoryProfile-Multidimensional-SingleTrajectory-H.6.2.nc")
            .getPath();
    // String2.log("\n\n** Testing contiguous file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, "-h"));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString();
    expected =
        "lat,lon,trajectory,alt,time,temperature,salinity\n"
            + "4.9986253,-35.718536,0,0.0,0,3.2502668,79.006065\n"
            + "4.9986253,-35.718536,0,1.0,0,30.566391,37.089394\n"
            + "4.9986253,-35.718536,0,2.0,0,0.8428718,20.478456\n"
            + "1.4658529,-25.19569,0,0.0,3600,27.982288,17.57356\n"
            + "1.4658529,-25.19569,0,1.0,3600,15.750698,11.152985\n"
            + "1.4658529,-25.19569,0,2.0,3600,39.36954,31.84863\n"
            + "1.4658529,-25.19569,0,3.0,3600,8.506618,77.88138\n"
            + "1.4658529,-25.19569,0,4.0,3600,39.580475,77.575645\n"
            + "38.299484,-55.72639,0,0.0,7200,36.646107,77.1104\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 20, "");
    results = table.columnAttributes(table.findColumnNumber("lat")).toString();
    expected =
        "    long_name=Latitude\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = table.columnAttributes(table.findColumnNumber("alt")).toString();
    expected =
        "    axis=Z\n"
            + "    long_name=height below mean sea level\n"
            + "    missing_value=-999.9f\n"
            + "    positive=down\n"
            + "    standard_name=altitude\n"
            + "    units=m\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e));
    // }

    // *************** trajectoryProfile orthogonal multidimensional ---
    // try {
    fileName = orthoMultiDimH61FileName;
    // String2.log("\n\n** Testing orthogonal multidim file\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(14);
    expected =
        "lat,lon,trajectory,alt,time,temperature,salinity\n"
            + "18.736742,-28.520071,0,0.0,0,8.067447,62.835354\n"
            + "18.736742,-28.520071,0,1.0,0,15.871663,27.454027\n"
            + "18.736742,-28.520071,0,2.0,0,32.306496,61.80094\n"
            + "18.736742,-28.520071,0,3.0,0,27.631369,74.84051\n"
            + "18.736742,-28.520071,0,4.0,0,22.757963,73.378914\n"
            + "39.43245,-57.711514,1,0.0,0,36.39878,77.23479\n"
            + "39.43245,-57.711514,1,1.0,0,14.957566,7.621207\n"
            + "39.43245,-57.711514,1,2.0,0,5.405648,56.557266\n"
            + "39.43245,-57.711514,1,3.0,0,4.9267964,75.427795\n"
            + "39.43245,-57.711514,1,4.0,0,7.806849,42.65483\n"
            + "39.43245,-57.711514,1,5.0,0,28.784224,13.940006\n"
            + "39.43245,-57.711514,1,6.0,0,19.139135,53.46242\n"
            + "25.034857,-62.39183,1,0.0,3600,24.302265,62.551056\n"
            + "25.034857,-62.39183,1,1.0,3600,11.195762,4.3670874\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 65, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=Latitude\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("alt"),
        0, // standardizeWhat=0
        StringArray.fromCSV("alt"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // String2.log("\n\n** Testing incomplete file with constraints\n" +
    // " " + orthoMultiDimH61FileName);
    // String2.log(NcHelper.ncdump(orthoMultiDimH61FileName, "-h"));
    table.readNcCF(
        orthoMultiDimH61FileName,
        null,
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("1"));
    results = table.dataToString();
    expected =
        "lat,lon,trajectory,alt,time,temperature,salinity\n"
            + "39.43245,-57.711514,1,0.0,0,36.39878,77.23479\n"
            + "39.43245,-57.711514,1,1.0,0,14.957566,7.621207\n"
            + "39.43245,-57.711514,1,2.0,0,5.405648,56.557266\n"
            + "39.43245,-57.711514,1,3.0,0,4.9267964,75.427795\n"
            + "39.43245,-57.711514,1,4.0,0,7.806849,42.65483\n"
            + "39.43245,-57.711514,1,5.0,0,28.784224,13.940006\n"
            + "39.43245,-57.711514,1,6.0,0,19.139135,53.46242\n"
            + "25.034857,-62.39183,1,0.0,3600,24.302265,62.551056\n"
            + "25.034857,-62.39183,1,1.0,3600,11.195762,4.3670874\n"
            + "25.034857,-62.39183,1,2.0,3600,20.055767,62.966892\n"
            + "25.034857,-62.39183,1,3.0,3600,38.838474,49.101334\n"
            + "25.034857,-62.39183,1,4.0,3600,16.995031,19.580278\n"
            + "25.034857,-62.39183,1,5.0,3600,5.6433516,62.23398\n"
            + "25.034857,-62.39183,1,6.0,3600,3.8319156,3.1172054\n"
            + "27.619982,-46.87254,1,0.0,7200,26.57189,22.247057\n"
            + "27.619982,-46.87254,1,1.0,7200,6.7928324,61.507736\n"
            + "27.619982,-46.87254,1,2.0,7200,37.85058,49.765\n"
            + "27.619982,-46.87254,1,3.0,7200,31.926285,65.68376\n"
            + "27.619982,-46.87254,1,4.0,7200,36.00912,39.43354\n"
            + "27.619982,-46.87254,1,5.0,7200,1.9108725,22.057114\n"
            + "27.619982,-46.87254,1,6.0,7200,23.234598,62.765938\n"
            + "16.401363,-38.747154,1,0.0,10800,39.38336,41.227074\n"
            + "16.401363,-38.747154,1,1.0,10800,22.487091,60.7646\n"
            + "16.401363,-38.747154,1,2.0,10800,16.574474,5.8886514\n"
            + "16.401363,-38.747154,1,3.0,10800,3.2033331,48.783085\n"
            + "16.401363,-38.747154,1,4.0,10800,32.13747,12.481885\n"
            + "16.401363,-38.747154,1,5.0,10800,5.927774,46.63955\n"
            + "16.401363,-38.747154,1,6.0,10800,9.936579,44.746056\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 28, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=Latitude\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test just variable[obs] (interiorTable)
    // String2.log("\n\n** Testing incomplete file\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("lat,lon,trajectory,time,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("2"));
    results = table.dataToString();
    expected =
        "lat,lon,trajectory,time\n" + "22.20038,-74.5625,2,0\n" + "39.905518,-15.35749,2,3600\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=Latitude\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=latitude\n"
            + "    units=degrees_north\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e));
    // }

    // ********** NO_DATA
    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA,
    // outer/inner/obsVar
    // String2.log("\n\n** Testing orthoMultiDimH61FileName just outer/inner/obsVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("trajectory,lat,zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // outerVar
    // String2.log("\n\n** Testing orthoMultiDimH61FileName just outerVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("zztop,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // innerVar
    // String2.log("\n\n** Testing orthoMultiDimH61FileName just innerVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("lat,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just obsVar, NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // outerVar* and innerVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer* /innerVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("zztop,trajectory,lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // outerVar and innerVar*
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer/innerVar*,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("zztop,trajectory,lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // outerVar* and obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer* /obsVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("trajectory,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // outerVar and obsVar*
    // String2.log("\n\n** Testing orthoMultiDimH51File just outer/obsVar*,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("trajectory,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // innerVar* and obsVar
    // String2.log("\n\n** Testing orthoMultiDimH51File just inner* /obsVar,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("lat,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile orthoMultiDimH61FileName --- NO_DATA, just
    // innerVar and obsVar*
    // String2.log("\n\n** Testing orthoMultiDimH51File just inner/obsVar*,
    // NO_DATA\n" +
    // " " + orthoMultiDimH61FileName);
    table.readNcCF(
        orthoMultiDimH61FileName,
        StringArray.fromCSV("lat,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *******
    // *************** trajectoryProfile ragged multiple station ---
    // try {
    fileName = raggedMultipleStationFileName;
    // String2.log("\n\n** Testing raggedMultipleStationFile\n" +
    // " " + fileName);
    // String2.log(NcHelper.ncdump(fileName, ""));
    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString();
    expected =
        "trajectory,lat,lon,time,z,temperature,humidity\n"
            + "0,49.0,-60.0,176400,0.0,39.174652,78.30777\n"
            + "0,49.0,-60.0,176400,1.0,30.924078,58.185684\n"
            + "0,49.0,-60.0,176400,2.0,19.258087,47.014008\n"
            + "0,49.0,-60.0,176400,3.0,33.512325,77.832664\n"
            + "0,49.0,-60.0,176400,4.0,35.435345,55.649605\n"
            + "0,49.0,-60.0,176400,5.0,17.33441,60.725643\n"
            + "0,49.0,-60.0,176400,6.0,6.3611813,40.747665\n"
            + "0,49.0,-60.0,176400,7.0,37.18272,32.312344\n"
            + "0,49.0,-60.0,176400,8.0,31.59126,30.934\n"
            + "0,49.0,-60.0,176400,9.0,36.08196,48.639427\n"
            + "0,49.0,-60.0,176400,10.0,22.3596,16.427603\n"
            + "0,49.0,-60.0,176400,11.0,6.2299767,16.590557\n"
            + "0,53.0,-60.0,104400,0.0,13.244916,28.135975\n"
            + "0,53.0,-60.0,104400,1.0,17.160164,41.951385\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected =
        "4,47.0,-44.0,136800,33.0,8.7725,74.930565\n"
            + "4,47.0,-44.0,136800,34.0,29.10568,31.136621\n"
            + "4,47.0,-44.0,136800,35.0,4.7999196,18.904522\n"
            + "4,47.0,-44.0,136800,36.0,1.4633778,47.546745\n"
            + "4,47.0,-44.0,136800,37.0,24.846626,77.9313\n"
            + "4,47.0,-44.0,136800,38.0,16.368006,8.343946\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length(), results.length()),
        expected,
        "results=\n" + results);
    Test.ensureEqual(table.nRows(), 594, "");
    results = table.columnAttributes(0).toString();
    expected = "    cf_role=trajectory_id\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // *************** trajectoryProfile ragged multiple station --- just outerVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just outerVar\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("zztop,trajectory"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString(20);
    expected = "trajectory\n" + "0\n" + "1\n" + "2\n" + "3\n" + "4\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 5, "");
    results = table.columnAttributes(0).toString();
    expected = "    cf_role=trajectory_id\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** trajectoryProfile ragged multiple station --- just innerVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just innerVar\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("time,zztop,trajectory"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "time,trajectory\n"
            + "176400,0\n"
            + "104400,0\n"
            + "140400,0\n"
            + "151200,0\n"
            + "111600,1\n"
            + "36000,1\n"
            + "64800,1\n"
            + "46800,1\n"
            + "0,2\n"
            + "162000,2\n"
            + "180000,2\n"
            + "129600,2\n"
            + "154800,3\n"
            + "3600,3\n"
            + "122400,3\n"
            + "32400,3\n"
            + "136800,4\n"
            + "140400,4\n"
            + "140400,4\n"
            + "136800,4\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 20, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=time of measurement\n"
            + "    missing_value=-999i\n"
            + "    standard_name=time\n"
            + "    units=seconds since 1990-01-01 00:00:00\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** trajectoryProfile ragged multiple station --- just outerVar
    // and innerVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just outerVar and
    // innerVar\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("lon,lat,time,trajectory,"),
        0, // standardizeWhat=0
        null,
        null,
        null);
    results = table.dataToString();
    expected =
        "lon,lat,time,trajectory\n"
            + "-60.0,49.0,176400,0\n"
            + "-60.0,53.0,104400,0\n"
            + "-63.0,45.0,140400,0\n"
            + "-62.0,45.0,151200,0\n"
            + "-56.0,54.0,111600,1\n"
            + "-68.0,53.0,36000,1\n"
            + "-66.0,43.0,64800,1\n"
            + "-47.0,58.0,46800,1\n"
            + "-52.0,47.0,0,2\n"
            + "-50.0,48.0,162000,2\n"
            + "-60.0,59.0,180000,2\n"
            + "-71.0,42.0,129600,2\n"
            + "-66.0,53.0,154800,3\n"
            + "-53.0,50.0,3600,3\n"
            + "-67.0,56.0,122400,3\n"
            + "-73.0,48.0,32400,3\n"
            + "-67.0,47.0,136800,4\n"
            + "-45.0,43.0,140400,4\n"
            + "-40.0,54.0,140400,4\n"
            + "-44.0,47.0,136800,4\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 20, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    long_name=station longitude\n"
            + "    standard_name=longitude\n"
            + "    units=degrees_east\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** trajectoryProfile ragged multiple station --- just outerVar
    // and obsVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just outerVar and
    // obsVar\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("trajectory,temperature,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("39"));
    results = table.dataToString();
    expected =
        "trajectory,temperature\n"
            + "0,39.174652\n"
            + "0,39.157566\n"
            + "0,39.915646\n"
            + "0,39.041203\n"
            + "1,39.104313\n"
            + "1,39.74818\n"
            + "1,39.077923\n"
            + "1,39.406986\n"
            + "2,39.85588\n"
            + "2,39.213017\n"
            + "2,39.89851\n"
            + "2,39.404697\n"
            + "2,39.012913\n"
            + "3,39.276768\n"
            + "3,39.832867\n"
            + "3,39.119217\n"
            + "3,39.335064\n"
            + "3,39.261826\n"
            + "3,39.501484\n"
            + "4,39.015987\n"
            + "4,39.5769\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 21, "");
    results = table.columnAttributes(0).toString();
    expected = "    cf_role=trajectory_id\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *************** trajectoryProfile ragged multiple station --- just innerVar
    // and obsVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just innerVar and
    // obsVar\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("temperature,zztop,time"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("39"));
    results = table.dataToString();
    expected =
        "temperature,time\n"
            + "39.174652,176400\n"
            + "39.157566,104400\n"
            + "39.915646,104400\n"
            + "39.041203,104400\n"
            + "39.104313,111600\n"
            + "39.74818,36000\n"
            + "39.077923,64800\n"
            + "39.406986,64800\n"
            + "39.85588,0\n"
            + "39.213017,180000\n"
            + "39.89851,180000\n"
            + "39.404697,180000\n"
            + "39.012913,180000\n"
            + "39.276768,154800\n"
            + "39.832867,3600\n"
            + "39.119217,122400\n"
            + "39.335064,122400\n"
            + "39.261826,122400\n"
            + "39.501484,122400\n"
            + "39.015987,140400\n"
            + "39.5769,136800\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 21, "");
    results = table.columnAttributes(0).toString();
    expected =
        "    coordinates=time lat lon z\n"
            + "    long_name=Air Temperature\n"
            + "    missing_value=-999.9f\n"
            + "    standard_name=air_temperature\n"
            + "    units=Celsius\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ********** NO_DATA
    // ********** trajectoryProfile ragged multiple station --- NO_DATA,
    // outer/inner/obsVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just
    // outer/inner/obsVar, NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("trajectory,zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // outerVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just outerVar,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("zztop,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("10"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // innerVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just innerVar,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("time,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("time"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just obsVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just obsVar, NO_DATA\n"
    // +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("zztop,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // outerVar* and innerVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just outer*/innerVar,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("zztop,z,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // outerVar and innerVar*
    // String2.log("\n\n** Testing raggedMultipleStationFile just outer/innerVar*,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("zztop,z,trajectory"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-5"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // outerVar* and obsVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just outer*/obsVar,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("temperature,trajectory,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-10"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // outerVar and obsVar*
    // String2.log("\n\n** Testing raggedMultipleStationFile just outer/obsVar*,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("temperature,trajectory,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // innerVar* and obsVar
    // String2.log("\n\n** Testing raggedMultipleStationFile just inner*/obsVar,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("temperature,z,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-1"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    // ********** trajectoryProfile ragged multiple station --- NO_DATA, just
    // innerVar and obsVar*
    // String2.log("\n\n** Testing raggedMultipleStationFile just inner/obsVar*,
    // NO_DATA\n" +
    // " " + raggedMultipleStationFileName);
    table.readNcCF(
        raggedMultipleStationFileName,
        StringArray.fromCSV("temperature,z,zztop"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV(">="),
        StringArray.fromCSV("100"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");
    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e));
    // }
    /* */
    // Table.debugMode = oDebug;
  }

  /**
   * This tests reading a very large TSV file: 764MB before .gz, 52MB after .gz, 33 columns, 3503266
   * rows. I switched to StringHolder in StringArray after complaints this file took ~10GB to load.
   * I also made several other significant memory saving changes. Now, reading the file into
   * StringArray ~1600MB is the high point. While parsing the lines, it shrinks to ~671MB (lots of
   * small strings). After convert to binary data types: ~420MB. Total time to read and ingest=~33s
   * from gz file (was ~420s before).
   */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testBigAscii() throws Exception {
    // PrimitiveArray.reallyVerbose = true;
    Math2.gcAndWait("Table (between tests)");
    Math2.gcAndWait("Table (between tests)"); // in a test
    // String2.log("\n*** Table.testBigAscii(): " + Math2.memoryString());

    Table table = new Table();
    long time = System.currentTimeMillis();
    table.readASCII(
        TableTests.class.getResource("/largeFiles/biddle/3937_v1_CTD_Profiles.tsv.gz").getPath());
    time = System.currentTimeMillis() - time;
    Math2.gcAndWait("Table (between tests)");
    Math2.gcAndWait("Table (between tests)"); // in a test
    // String2.log(" done. " + Math2.memoryString() + "\n" +
    // String2.canonicalStatistics()); // in a test
    String results = table.dataToString(4);
    // String2.log(results);
    String expected =
        "cruise_name,station,cast,ISO_DateTime,Year,Month,Day,timeutc,lon,lat,depth_max,pres_max,Date,timecode,HOT_summary_file_name,parameters,num_bottles,section,nav_code,depth_hgt,EXPOCODE,Ship,comments,CTDPRS,CTDTMP,CTDSAL,CTDOXY,XMISS,CHLPIG,NUMBER,NITRATE,FLUOR,QUALT1\n"
            + "001,2,1,1988-10-30T21:34:00,1988,10,30,2134,-157.9967,22.7483,4750,238,103088,BE,cruise.summaries/hot1.sum,NaN,11,PRS2,GPS,4514,32MW001_1,32MW001/1,NaN,0.0,26.2412,35.2615,183.2,4.99,-0.0126,0,,,666666\n"
            + "001,2,1,1988-10-30T21:34:00,1988,10,30,2134,-157.9967,22.7483,4750,238,103088,BE,cruise.summaries/hot1.sum,NaN,11,PRS2,GPS,4514,32MW001_1,32MW001/1,NaN,2.0,26.2412,35.2615,183.2,4.99,-0.0126,36,,,222322\n"
            + "001,2,1,1988-10-30T21:34:00,1988,10,30,2134,-157.9967,22.7483,4750,238,103088,BE,cruise.summaries/hot1.sum,NaN,11,PRS2,GPS,4514,32MW001_1,32MW001/1,NaN,4.0,26.2554,35.2530,185.5,4.08,0.0026,72,,,223322\n"
            + "001,2,1,1988-10-30T21:34:00,1988,10,30,2134,-157.9967,22.7483,4750,238,103088,BE,cruise.summaries/hot1.sum,NaN,11,PRS2,GPS,4514,32MW001_1,32MW001/1,NaN,6.0,26.2377,35.2455,204.8,3.05,0.0167,108,,,222122\n"
            + "...\n";
    Test.ensureEqual(results, expected, "");

    Test.ensureEqual(table.nRows(), 3503266, "");
    Test.ensureEqual(table.nColumns(), 33, "");

    table.removeRows(0, 3503261);
    results =
        table.saveAsNccsv(
            false, true, 0, Integer.MAX_VALUE); // catchScalars, writeMetadata, firstDataRow,
    // lastDataRow

    expected = // many vars are scalar because they're constant in last 6 rows
        "*GLOBAL*,Conventions,\"COARDS, CF-1.10, ACDD-1.3, NCCSV-1.2\"\n"
            + "cruise_name,*DATA_TYPE*,String\n"
            + "station,*DATA_TYPE*,byte\n"
            + "cast,*DATA_TYPE*,byte\n"
            + "ISO_DateTime,*DATA_TYPE*,String\n"
            + "Year,*DATA_TYPE*,short\n"
            + "Month,*DATA_TYPE*,byte\n"
            + "Day,*DATA_TYPE*,String\n"
            + "timeutc,*DATA_TYPE*,String\n"
            + "lon,*DATA_TYPE*,float\n"
            + "lat,*DATA_TYPE*,float\n"
            + "depth_max,*DATA_TYPE*,short\n"
            + "pres_max,*DATA_TYPE*,short\n"
            + "Date,*DATA_TYPE*,String\n"
            + "timecode,*DATA_TYPE*,String\n"
            + "HOT_summary_file_name,*DATA_TYPE*,String\n"
            + "parameters,*DATA_TYPE*,String\n"
            + "num_bottles,*DATA_TYPE*,byte\n"
            + "section,*DATA_TYPE*,String\n"
            + "nav_code,*DATA_TYPE*,String\n"
            + "depth_hgt,*DATA_TYPE*,short\n"
            + "EXPOCODE,*DATA_TYPE*,String\n"
            + "Ship,*DATA_TYPE*,String\n"
            + "comments,*DATA_TYPE*,String\n"
            + "CTDPRS,*DATA_TYPE*,float\n"
            + "CTDTMP,*DATA_TYPE*,float\n"
            + "CTDSAL,*DATA_TYPE*,String\n"
            + "CTDOXY,*DATA_TYPE*,String\n"
            + "XMISS,*DATA_TYPE*,float\n"
            + "CHLPIG,*DATA_TYPE*,String\n"
            + "NUMBER,*DATA_TYPE*,String\n"
            + "NITRATE,*DATA_TYPE*,float\n"
            + "FLUOR,*DATA_TYPE*,double\n"
            + "QUALT1,*DATA_TYPE*,String\n"
            + "\n"
            + "*END_METADATA*\n"
            + "cruise_name,station,cast,ISO_DateTime,Year,Month,Day,timeutc,lon,lat,depth_max,pres_max,Date,timecode,HOT_summary_file_name,parameters,num_bottles,section,nav_code,depth_hgt,EXPOCODE,Ship,comments,CTDPRS,CTDTMP,CTDSAL,CTDOXY,XMISS,CHLPIG,NUMBER,NITRATE,FLUOR,QUALT1\n"
            + "288,50,1,2016-11-28T17:51:00,2016,11,28,1751,-157.9373,22.7703,4705,202,112816,BE,cruise.summaries/hot288.sum,NaN,22,PRS2,GPS,4504,33KB288_1,33KB288/1,Dual T; C sensors,194.0,18.2746,34.9098,203.9,,0.0254,144,,,222192\n"
            + "288,50,1,2016-11-28T17:51:00,2016,11,28,1751,-157.9373,22.7703,4705,202,112816,BE,cruise.summaries/hot288.sum,NaN,22,PRS2,GPS,4504,33KB288_1,33KB288/1,Dual T; C sensors,196.0,18.159,34.9027,203.8,,0.0257,108,,,222192\n"
            + "288,50,1,2016-11-28T17:51:00,2016,11,28,1751,-157.9373,22.7703,4705,202,112816,BE,cruise.summaries/hot288.sum,NaN,22,PRS2,GPS,4504,33KB288_1,33KB288/1,Dual T; C sensors,198.0,17.9686,34.8727,203.8,,0.0257,156,,,222192\n"
            + "288,50,1,2016-11-28T17:51:00,2016,11,28,1751,-157.9373,22.7703,4705,202,112816,BE,cruise.summaries/hot288.sum,NaN,22,PRS2,GPS,4504,33KB288_1,33KB288/1,Dual T; C sensors,200.0,17.8751,34.8506,201.8,,0.0248,108,,,222192\n"
            + "288,50,1,2016-11-28T17:51:00,2016,11,28,1751,-157.9373,22.7703,4705,202,112816,BE,cruise.summaries/hot288.sum,NaN,22,PRS2,GPS,4504,33KB288_1,33KB288/1,Dual T; C sensors,202.0,17.7435,34.8246,202.1,,0.0248,60,,,222192\n"
            + "*END_DATA*\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    Math2.gcAndWait("Table (between tests)"); // in a test
    // String msg = Math2.memoryString() + "\n" +
    //         String2.canonicalStatistics() + "\n" +
    //         "testBigAscii time=" + time +
    //         "ms. file read time should be ~60 - 90s in java 17 (but I think it should be faster
    // -- too much gc) (but longer when computer is busy) (Java 8 was 45s. was 36s before v2.10)";
    // String2.log(msg);
    // TODO get a better performance check
    // Test.ensureTrue(time < 60000, "Too slow! " + msg);
  }

  /**
   * This tests if nc files are closed when "try with resources" approach is taken. 2022-09-09 With
   * tryWithResources, ERDDAP crashes every 2 days (like THREDDS)! So go back to: finally
   * {nc.close()}
   */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testNcCloseTryWithResources() throws Throwable {
    // String2.log("\n*** Table.testNcCloseTryWithResources()");

    long time = System.currentTimeMillis();
    String fileName =
        Path.of(
                TableTests.class
                    .getResource(
                        "/data/points/erdCalcofiSubsurface/1950/subsurface_19500106_69_144.nc")
                    .toURI())
            .toString();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      try (NetcdfFile ncfile = NcHelper.openFile(fileName)) {
        if (i % 10000 == 0) String2.log("" + i);
        // } catch (Exception e) {
        // String2.log("i=" + i + " " + e.toString());
      }
    }
    String2.log(
        "Opened the same file "
            + n
            + " times and auto-closed them. time="
            + (System.currentTimeMillis() - time)
            + " (exp=35534ms)");

    // open all of the 10,000 scripps glider files in batch14 after manually: gunzip
    // *.gz
    time = System.currentTimeMillis();
    Table table =
        FileVisitorDNLS.oneStep(
            Path.of(TableTests.class.getResource("/data/points/scrippsGliders/batch14/").toURI())
                .toString(),
            ".*\\.nc",
            true,
            "",
            false);
    PrimitiveArray dirs = table.getColumn(0);
    PrimitiveArray names = table.getColumn(1);
    n = dirs.size();
    // String2.log("nFiles found=" + n);
    for (int i = 0; i < n; i++) {
      try (NetcdfFile ncfile = NcHelper.openFile(dirs.getString(i) + names.getString(i))) {
        if (i % 1000 == 0) String2.log("" + i);
      }
    }
    String2.log(
        "Opened "
            + n
            + " files and auto-closed them. time="
            + (System.currentTimeMillis() - time)
            + " (exp=4530ms)");
  }

  /** This tests readXml. */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testXml() throws Exception {
    // String2.log("\n*** Table.testXml()");
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // TableXmlHandler.verbose = true;
    Table table = null;
    String xml, results, expected;

    // *** WFS
    table = new Table();
    BufferedReader reader =
        File2.getDecompressedBufferedFileReaderUtf8("c:/programs/mapserver/WVBoreholeResponse.xml");
    try {
      table.readXml(
          reader,
          false, // no validate since no .dtd
          "/wfs:FeatureCollection/wfs:member", // default tRowElementXPath,
          // was "/wfs:FeatureCollection/gml:featureMember",
          null,
          false); // row attributes, simplify
    } finally {
      reader.close();
    }
    results = table.dataToString(3);
    expected =
        "aasg:BoreholeTemperature/aasg:ObservationURI,aasg:BoreholeTemperature/aasg:WellName,aasg:BoreholeTemperature/aasg:APINo,aasg:BoreholeTemperature/aasg:HeaderURI,aasg:BoreholeTemperature/aasg:OtherName,aasg:BoreholeTemperature/aasg:Label,aasg:BoreholeTemperature/aasg:Operator,aasg:BoreholeTemperature/aasg:SpudDate,aasg:BoreholeTemperature/aasg:EndedDrillingDate,aasg:BoreholeTemperature/aasg:WellType,aasg:BoreholeTemperature/aasg:StatusDate,aasg:BoreholeTemperature/aasg:ReleaseDate,aasg:BoreholeTemperature/aasg:Field,aasg:BoreholeTemperature/aasg:County,aasg:BoreholeTemperature/aasg:State,aasg:BoreholeTemperature/aasg:UTM_E,aasg:BoreholeTemperature/aasg:UTM_N,aasg:BoreholeTemperature/aasg:LatDegree,aasg:BoreholeTemperature/aasg:LongDegree,aasg:BoreholeTemperature/aasg:SRS,aasg:BoreholeTemperature/aasg:LocationUncertaintyStatement,aasg:BoreholeTemperature/aasg:LocationUncertaintyRadius,aasg:BoreholeTemperature/aasg:DrillerTotalDepth,aasg:BoreholeTemperature/aasg:DepthReferencePoint,aasg:BoreholeTemperature/aasg:LengthUnits,aasg:BoreholeTemperature/aasg:WellBoreShape,aasg:BoreholeTemperature/aasg:TrueVerticalDepth,aasg:BoreholeTemperature/aasg:ElevationKB,aasg:BoreholeTemperature/aasg:ElevationDF,aasg:BoreholeTemperature/aasg:ElevationGL,aasg:BoreholeTemperature/aasg:FormationTD,aasg:BoreholeTemperature/aasg:BitDiameterTD,aasg:BoreholeTemperature/aasg:MaximumRecordedTemperature,aasg:BoreholeTemperature/aasg:MeasuredTemperature,aasg:BoreholeTemperature/aasg:CorrectedTemperature,aasg:BoreholeTemperature/aasg:TemperatureUnits,aasg:BoreholeTemperature/aasg:CirculationDuration,aasg:BoreholeTemperature/aasg:MeasurementProcedure,aasg:BoreholeTemperature/aasg:DepthOfMeasurement,aasg:BoreholeTemperature/aasg:MeasurementDateTime,aasg:BoreholeTemperature/aasg:MeasurementFormation,aasg:BoreholeTemperature/aasg:MeasurementSource,aasg:BoreholeTemperature/aasg:RelatedResource,aasg:BoreholeTemperature/aasg:CasingBottomDepthDriller,aasg:BoreholeTemperature/aasg:CasingTopDepth,aasg:BoreholeTemperature/aasg:CasingPipeDiameter,aasg:BoreholeTemperature/aasg:CasingWeight,aasg:BoreholeTemperature/aasg:CasingThickness,aasg:BoreholeTemperature/aasg:pH,aasg:BoreholeTemperature/aasg:InformationSource,aasg:BoreholeTemperature/aasg:Shape/gml:Point/latitude,aasg:BoreholeTemperature/aasg:Shape/gml:Point/longitude,aasg:BoreholeTemperature/aasg:LeaseName,aasg:BoreholeTemperature/aasg:LeaseOwner,aasg:BoreholeTemperature/aasg:LeaseNo,aasg:BoreholeTemperature/aasg:TimeSinceCirculation,aasg:BoreholeTemperature/aasg:Status,aasg:BoreholeTemperature/aasg:CommodityOfInterest,aasg:BoreholeTemperature/aasg:Function,aasg:BoreholeTemperature/aasg:Production,aasg:BoreholeTemperature/aasg:ProducingInterval,aasg:BoreholeTemperature/aasg:Notes\n"
            + "http://resources.usgin.org/uri-gin/wvges/bhtemp/4705500185/,Dominion Appalachian Development Co.  Gilbert Bailey 1,4705500185,http://resources.usgin.org/uri-gin/wvges/well/api:4705500185/,Gilbert Bailey,4705500185,Dominion Appalachian Development Co.,1998-04-02T00:00:00,1998-04-13T00:00:00,Gas,1900-01-01T00:00:00,1900-01-01T00:00:00,Stovall Ridge,Mercer,West Virginia,0.0,0.0,37.4834349990001,-81.1519399999999,EPSG:4326,Location recorded as received from official permit application converted to NAD83 if required,0.0,4854,G.L.,ft,vertical,4854,0.0,0.0,2563,Up Devonian undiff:Berea to Lo Huron,0.0,0.0,87.8,0.0,F,0.0,Temperature log evaluated by WVGES staff for deepest stable log segment to extract data otherwise used given bottom hole temperature on log header if available,3550,1900-01-01T00:00:00,Big Lime,Well Temperature Log,TL | GR | DEN | NEL | IL | CAL | SL,0.0,0.0,0.0,0.0,0.0,0.0,West Virginia Geological and Economic Survey 2013,37.48343499900005,-81.15193999999991,,,,,,,,,,\n"
            + "http://resources.usgin.org/uri-gin/wvges/bhtemp/4705500113/,\"Stonewall Gas Co., Inc.  State Conserv Comm 2\",4705500113,http://resources.usgin.org/uri-gin/wvges/well/api:4705500113/,Blue Jay Lumber Co,4705500113,\"Stonewall Gas Co., Inc.\",1991-09-20T00:00:00,1991-09-27T00:00:00,Gas,1900-01-01T00:00:00,1900-01-01T00:00:00,Rhodell,Mercer,West Virginia,0.0,0.0,37.530177999,-81.1708569999999,EPSG:4326,Location recorded as received from official permit application converted to NAD83 if required,0.0,4100,G.L.,ft,vertical,4100,0.0,0.0,2442,Price Fm & equivs,0.0,0.0,90,0.0,F,0.0,Temperature log evaluated by WVGES staff for deepest stable log segment to extract data otherwise used given bottom hole temperature on log header if available,3940,1900-01-01T00:00:00,,Well Temperature Log,TL | GR | DEN | NEL | IL | CAL | SL,0.0,0.0,0.0,0.0,0.0,0.0,West Virginia Geological and Economic Survey 2013,37.53017799900005,-81.1708569999999,,,,,,,,,,\n"
            + "http://resources.usgin.org/uri-gin/wvges/bhtemp/4705500115/,\"Stonewall Gas Co., Inc.  State Conserv Comm 4\",4705500115,http://resources.usgin.org/uri-gin/wvges/well/api:4705500115/,Blue Jay Lumber Co 4,4705500115,\"Stonewall Gas Co., Inc.\",1992-12-01T00:00:00,1992-12-08T00:00:00,Gas,1900-01-01T00:00:00,1900-01-01T00:00:00,Rhodell,Mercer,West Virginia,0.0,0.0,37.533804999,-81.1585159999999,EPSG:4326,Location recorded as received from official permit application converted to NAD83 if required,0.0,4097,G.L.,ft,vertical,4097,0.0,0.0,2463,Price Fm & equivs,0.0,0.0,98,0.0,F,0.0,Temperature log evaluated by WVGES staff for deepest stable log segment to extract data otherwise used given bottom hole temperature on log header if available,3920,1900-01-01T00:00:00,,Well Temperature Log,TL | GR | DEN | NEL | IL | CAL | SL,0.0,0.0,0.0,0.0,0.0,0.0,West Virginia Geological and Economic Survey 2013,37.53380499900004,-81.15851599999991,,,,,,,,,,\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** darwin core
    // Lines are commented out to test some aspects of readXml.
    xml =
        "<?xml version='1.0' encoding='utf-8' ?>\n"
            + "<response\n"
            + "  xmlns=\"http://digir.net/schema/protocol/2003/1.0\" \n"
            + "  xmlns:xsd=\"https://www.w3.org/2001/XMLSchema\" \n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n"
            + "  xmlns:darwin=\"http://digir.net/schema/conceptual/darwin/2003/1.0\" \n"
            + "  xmlns:obis=\"http://www.iobis.org/obis\" >\n"
            + "<header>\n"
            + "<version>$Revision: 1.12 $</version>\n"
            + "</header>\n"
            + "<content><record>\n"
            +
            // "<darwin:InstitutionCode>Marine Fish Division, Fisheries and Oceans
            // Canada</darwin:InstitutionCode>\n" +
            "<darwin:CollectionCode>Gwaii Haanas Marine Algae</darwin:CollectionCode>\n"
            + "<darwin:CatalogNumber>100-MACRINT</darwin:CatalogNumber>\n"
            + "<darwin:ScientificName>Macrocystis integrifolia</darwin:ScientificName>\n"
            + "<darwin:ScientificName>sciName2</darwin:ScientificName>\n"
            + "<darwin:Latitude>52.65172</darwin:Latitude>\n"
            + "<darwin:Longitude>-131.66368</darwin:Longitude>\n"
            + "<obis:Temperature xsi:nil='true'/>\n"
            + "<parent>\n"
            + "  <child1>child1data</child1>\n"
            + "  <child2>child2data</child2>\n"
            + "  <child2>child22data</child2>\n"
            + "</parent>\n"
            + "</record><record><darwin:InstitutionCode>BIO</darwin:InstitutionCode>\n"
            + "<darwin:CollectionCode>GHMP</darwin:CollectionCode>\n"
            + "<darwin:CatalogNumber>100-MACRINT</darwin:CatalogNumber>\n"
            + "<darwin:ScientificName>Macrocystis integrifolia</darwin:ScientificName>\n"
            + "<darwin:Latitude>52.65172</darwin:Latitude>\n"
            + "<darwin:Longitude>-131.66368</darwin:Longitude>\n"
            + "<obis:Temperature xsi:nil='true'/>\n"
            + "</record><record>\n"
            +
            // "<darwin:InstitutionCode>Marine Fish Division, Fisheries and Oceans
            // Canada</darwin:InstitutionCode>\n" +
            "<darwin:CollectionCode>Gwaii Haanas Marine Algae</darwin:CollectionCode>\n"
            + "<darwin:CatalogNumber>10036-MACRINT</darwin:CatalogNumber>\n"
            + "<darwin:ScientificName>Macrocystis integrifolia</darwin:ScientificName>\n"
            + "<darwin:Latitude>53.292</darwin:Latitude>\n"
            + "<darwin:Longitude>-132.4223</darwin:Longitude>\n"
            + "<obis:Temperature xsi:nil='true'/>\n"
            + "</record></content>\n"
            + "<diagnostics>\n"
            + "</diagnostics></response>\n";

    // this doesn't test heirarchical elements
    table = new Table();
    reader = new BufferedReader(new StringReader(xml));
    try {
      table.readXml(
          reader,
          false, // no validate since no .dtd
          "/response/content/record",
          null,
          true);
    } finally {
      reader.close();
    }
    table.ensureValid(); // throws Exception if not
    Test.ensureEqual(table.nRows(), 3, "");
    Test.ensureEqual(table.nColumns(), 10, "");
    Test.ensureEqual(table.getColumnName(0), "darwin:CollectionCode", "");
    Test.ensureEqual(table.getColumnName(1), "darwin:CatalogNumber", "");
    Test.ensureEqual(table.getColumnName(2), "darwin:ScientificName", "");
    Test.ensureEqual(table.getColumnName(3), "darwin:ScientificName2", "");
    Test.ensureEqual(table.getColumnName(4), "darwin:Latitude", "");
    Test.ensureEqual(table.getColumnName(5), "darwin:Longitude", "");
    // Test.ensureEqual(table.getColumnName(5), "obis:Temperature", ""); //no data,
    // so no column
    Test.ensureEqual(table.getColumnName(6), "parent/child1", "");
    Test.ensureEqual(table.getColumnName(7), "parent/child2", "");
    Test.ensureEqual(table.getColumnName(8), "parent/child22", "");
    Test.ensureEqual(table.getColumnName(9), "darwin:InstitutionCode", "");
    Test.ensureEqual(table.getColumn(0).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(1).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(2).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(3).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(4).elementTypeString(), "float", "");
    Test.ensureEqual(table.getColumn(5).elementTypeString(), "double", "");
    Test.ensureEqual(table.getColumn(6).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(7).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(8).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(9).elementTypeString(), "String", "");
    Test.ensureEqual(table.getStringData(0, 2), "Gwaii Haanas Marine Algae", "");
    Test.ensureEqual(table.getStringData(1, 2), "10036-MACRINT", "");
    Test.ensureEqual(table.getStringData(2, 2), "Macrocystis integrifolia", "");
    Test.ensureEqual(table.getStringData(3, 0), "sciName2", "");
    Test.ensureEqual(table.getFloatData(4, 2), 53.292f, "");
    Test.ensureEqual(table.getFloatData(5, 2), -132.4223f, "");
    Test.ensureEqual(table.getStringData(6, 0), "child1data", "");
    Test.ensureEqual(table.getStringData(7, 0), "child2data", "");
    Test.ensureEqual(table.getStringData(8, 0), "child22data", "");
    Test.ensureEqual(table.getStringData(9, 0), "", "");
    Test.ensureEqual(table.getStringData(9, 1), "BIO", "");
    Test.ensureEqual(table.getStringData(9, 2), "", "");

    // a subset of https://opendap.co-ops.nos.noaa.gov/stations/stationsXML.jsp
    String stationsXml =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
            + "<stations xmlns=\"https://opendap.co-ops.nos.noaa.gov/stations/\" \n"
            + "xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n"
            + "xsi:schemaLocation=\"https://opendap.co-ops.nos.noaa.gov/stations/   xml_schemas/stations.xsd\"> \n"
            + "<station name=\"DART BUOY 46419\" ID=\"1600013\" >\n"
            + "<metadata>\n"
            + "<location>\n"
            + "<lat> 48 28.7 N </lat>\n"
            + "<long> 129 21.5 W </long>\n"
            + "<state>   </state>\n"
            + "</location>\n"
            + "<date_established> 2003-01-01 </date_established>\n"
            + "</metadata>\n"
            + "</station>\n"
            + "<station name=\"DART BUOY 46410\" ID=\"1600014\" >\n"
            + "<metadata>\n"
            + "<location>\n"
            + "<lat> 57 29.9 N </lat>\n"
            + "<long> 144 0.06 W </long>\n"
            + "<state>   </state>\n"
            + "</location>\n"
            + "<date_established> 2001-01-01 </date_established>\n"
            + "</metadata>\n"
            + "</station>\n"
            + "<station name=\"Magueyes Island\" ID=\"9759110\" >\n"
            + "<metadata>\n"
            + "<location>\n"
            + "<lat> 17 58.3 N </lat>\n"
            + "<long> 67 2.8 W </long>\n"
            + "<state> PR </state>\n"
            + "</location>\n"
            + "<date_established> 1954-12-01 </date_established>\n"
            + "</metadata>\n"
            + "<parameter name=\"Water Level\" sensorID=\"A1\" DCP=\"1\" status=\"1\" />\n"
            + "<parameter name=\"Winds\" sensorID=\"C1\" DCP=\"1\" status=\"1\" />\n"
            + "<parameter name=\"Air Temp\" sensorID=\"D1\" DCP=\"1\" status=\"1\" />\n"
            + "<parameter name=\"Water Temp\" sensorID=\"E1\" DCP=\"1\" status=\"1\" />\n"
            + "<parameter name=\"Air Pressure\" sensorID=\"F1\" DCP=\"1\" status=\"1\" />\n"
            + "</station>\n"
            + "</stations>\n";
    table.clear();
    reader = new BufferedReader(new StringReader(stationsXml));
    try {
      table.readXml(
          reader,
          false, // no validate since no .dtd
          "/stations/station",
          new String[] {"name", "ID"},
          true);
    } finally {
      reader.close();
    }
    table.ensureValid(); // throws Exception if not
    // String2.log(table.toString());
    // Row name ID metadata/locat metadata/locat metadata/date_ metadata/locat
    // 0 DART BUOY 4641 1600013 48 28.7 N 129 21.5 W 2003-01-01
    // 1 DART BUOY 4641 1600014 57 29.9 N 144 0.06 W 2001-01-01
    // 2 Magueyes Islan 9759110 17 58.3 N 67 2.8 W 1954-12-01 PR
    Test.ensureEqual(table.nRows(), 3, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    Test.ensureEqual(table.getColumnName(0), "name", "");
    Test.ensureEqual(table.getColumnName(1), "ID", "");
    Test.ensureEqual(table.getColumnName(2), "metadata/location/lat", "");
    Test.ensureEqual(table.getColumnName(3), "metadata/location/long", "");
    Test.ensureEqual(table.getColumnName(4), "metadata/date_established", "");
    Test.ensureEqual(table.getColumnName(5), "metadata/location/state", "");
    Test.ensureEqual(table.getColumn(0).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(1).elementTypeString(), "int", "");
    Test.ensureEqual(table.getColumn(2).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(3).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(4).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(5).elementTypeString(), "String", "");
    Test.ensureEqual(table.getStringData(0, 0), "DART BUOY 46419", "");
    Test.ensureEqual(table.getStringData(1, 0), "1600013", "");
    Test.ensureEqual(table.getStringData(2, 0), "48 28.7 N", "");
    Test.ensureEqual(table.getStringData(3, 0), "129 21.5 W", "");
    Test.ensureEqual(table.getStringData(4, 0), "2003-01-01", "");
    Test.ensureEqual(table.getStringData(5, 0), "", "");
    Test.ensureEqual(table.getStringData(0, 2), "Magueyes Island", "");
    Test.ensureEqual(table.getStringData(1, 2), "9759110", "");
    Test.ensureEqual(table.getStringData(2, 2), "17 58.3 N", "");
    Test.ensureEqual(table.getStringData(3, 2), "67 2.8 W", "");
    Test.ensureEqual(table.getStringData(4, 2), "1954-12-01", "");
    Test.ensureEqual(table.getStringData(5, 2), "PR", "");

    TableXmlHandler.verbose = false;
  }

  /**
   * This tests the readSql and saveAsSql methods.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagPassword
  void testSql() throws Exception {
    // String2.log("\n*** Table.testSql");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // load the sql driver (the actual driver .jar must be in the classpath)
    Class.forName("org.postgresql.Driver");

    // set up connection and query
    String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest"; // database name
    String user = "postadmin";
    String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
    if (password.length() == 0) {
      String2.log("No password, so skipping the test.");
      return;
    }
    long tTime = System.currentTimeMillis();
    Connection con = DriverManager.getConnection(url, user, password);
    // String2.log("getConnection time=" + (System.currentTimeMillis() - tTime) +
    // "ms"); // often 9s !

    DatabaseMetaData dm = con.getMetaData();
    // String2.log("getMaxRowSize=" + dm.getMaxRowSize()); // 1GB

    // get catalog info -- has one col with name(s) of databases for this user
    // ...

    // test getSqlSchemas
    StringArray schemas = Table.getSqlSchemas(con);
    Test.ensureTrue(schemas.indexOf("public") >= 0, "schemas=" + schemas.toString());

    // sometimes: make names table
    if (false) {
      Table namesTable = new Table();
      namesTable.addColumn("id", PrimitiveArray.factory(new int[] {1, 2, 3}));
      namesTable.addColumn(
          "first_name", PrimitiveArray.factory(new String[] {"Bob", "Nate", "Nancy"}));
      namesTable.addColumn(
          "last_name", PrimitiveArray.factory(new String[] {"Smith", "Smith", "Jones"}));
      namesTable.saveAsSql(
          con, true, // 'true' tests dropSqlTable, too
          "names", 0, null, null, null, 2);
    }

    // test getSqlTableNames
    StringArray tableNames = Table.getSqlTableNames(con, "public", new String[] {"TABLE"});
    // String2.log("tableNames=" + tableNames);
    Test.ensureTrue(tableNames.indexOf("names") >= 0, "tableNames=" + tableNames.toString());
    Test.ensureTrue(
        tableNames.indexOf("zztop") < 0, "tableNames=" + tableNames.toString()); // doesn't exist

    // test getSqlTableType
    Test.ensureEqual(Table.getSqlTableType(con, "public", "names"), "TABLE", "");
    Test.ensureEqual(Table.getSqlTableType(con, "public", "zztop"), null, ""); // doesn't exist

    // *** test saveAsSql (create a table) (this tests dropSqlTable, too)
    if (true) {
      String tempTableName = "TempTest";
      String dates[] = {"1960-01-02", "1971-01-02", null, "2020-12-31"};
      double dateDoubles[] = new double[4];
      String timestamps[] = {
        "1960-01-02 01:02:03", "1971-01-02 07:08:09", null, "2020-12-31 23:59:59"
      };
      double timestampDoubles[] = new double[4];
      String times[] = {"01:02:03", "07:08:09", null, "23:59:59"};
      for (int i = 0; i < 4; i++) {
        dateDoubles[i] =
            dates[i] == null ? Double.NaN : Calendar2.isoStringToEpochSeconds(dates[i]);
        timestampDoubles[i] =
            timestamps[i] == null ? Double.NaN : Calendar2.isoStringToEpochSeconds(timestamps[i]);
      }
      Table tempTable = new Table();
      tempTable.addColumn("uid", PrimitiveArray.factory(new int[] {1, 2, 3, 4}));
      tempTable.addColumn(
          "short", PrimitiveArray.factory(new short[] {-10, 0, Short.MAX_VALUE, 10}));
      // Math2.random makes this test different every time. ensures old table is
      // dropped and new one created.
      tempTable.addColumn(
          "int", PrimitiveArray.factory(new int[] {Math2.random(1000), 0, Integer.MAX_VALUE, 20}));
      tempTable.addColumn("long", PrimitiveArray.factory(new long[] {-30, 0, Long.MAX_VALUE, 30}));
      tempTable.addColumn(
          "float", PrimitiveArray.factory(new float[] {-44.4f, 0f, Float.NaN, 44.4f}));
      tempTable.addColumn(
          "double", PrimitiveArray.factory(new double[] {-55.5, 0, Double.NaN, 55.5}));
      tempTable.addColumn(
          "string", PrimitiveArray.factory(new String[] {"ab", "", null, "longer"}));
      tempTable.addColumn("date", PrimitiveArray.factory(dateDoubles));
      tempTable.addColumn("timestamp", PrimitiveArray.factory(timestampDoubles));
      tempTable.addColumn("time", PrimitiveArray.factory(times));
      tempTable.saveAsSql(
          con,
          true, // 'true' tests dropSqlTable, too
          tempTableName,
          0,
          new int[] {7},
          new int[] {8},
          new int[] {9},
          1.5);

      // test readSql (read a table)
      Table tempTable2 = new Table();
      tempTable2.readSql(con, "SELECT * FROM " + tempTableName);
      Test.ensureEqual(tempTable, tempTable2, "");

      // *** test rollback: add data that causes database to throw exception
      tempTable2.setIntData(0, 0, 5); // ok
      tempTable2.setIntData(0, 1, 6); // ok
      tempTable2.setIntData(0, 2, 7); // ok
      tempTable2.setIntData(0, 3, 1); // not ok because not unique
      try { // try to add new tempTable2 to database table
        tempTable2.saveAsSql(
            con,
            false, // false, so added to previous data
            tempTableName,
            0,
            new int[] {7},
            new int[] {8},
            new int[] {9},
            1.5);
        String2.log("Shouldn't get here.");
        Math2.sleep(60000);
      } catch (Exception e) {
        // this error is expected
        // make sure it has both parts of the error message
        String2.log("\nEXPECTED " + String2.ERROR + ":\n" + MustBe.throwableToString(e));
        Test.ensureTrue(
            e.toString()
                    .indexOf(
                        "PSQLException: "
                            + String2.ERROR
                            + ": duplicate key violates unique constraint \"temptest_pkey\"")
                >= 0,
            "(A) The error was: " + e.toString());
        Test.ensureTrue(
            e.toString()
                    .indexOf("java.sql.BatchUpdateException: Batch entry 3 INSERT INTO TempTest (")
                >= 0,
            "(B)The error was: " + e.toString());
      }

      // and ensure database was rolled back to previous state
      tempTable2.readSql(con, "SELECT * FROM " + tempTableName);
      Test.ensureEqual(tempTable, tempTable2, "");

      // *** test pre-execute errors: add data that causes saveAsSql to throw
      // exception
      tempTable2.setIntData(0, 0, 5); // ok, so new rows can be added
      tempTable2.setIntData(0, 1, 6); // ok
      tempTable2.setIntData(0, 2, 7); // ok
      tempTable2.setIntData(0, 3, 8); // ok
      int timeCol = tempTable2.findColumnNumber("time");
      // invalid date will be caught before statement is fully prepared
      tempTable2.setStringData(timeCol, 3, "20.1/30"); // first 3 rows succeed, this should fail
      try { // try to add new tempTable2 to database table
        tempTable2.saveAsSql(
            con,
            false, // false, so added to previous data
            tempTableName,
            0,
            new int[] {7},
            new int[] {8},
            new int[] {9},
            1.5);
        String2.log("Shouldn't get here.");
        Math2.sleep(60000);
      } catch (Exception e) {
        // this error is expected
        // make sure it is the right error
        String2.log("\nEXPECTED " + String2.ERROR + ":\n" + MustBe.throwableToString(e));
        Test.ensureTrue(
            e.toString()
                    .indexOf(
                        "java.lang.RuntimeException: ERROR in Table.saveAsSql(TempTest):\n"
                            + "Time format must be "
                            + "HH:MM:SS. Bad value=20.1/30 in row=3 col=9")
                >= 0,
            "error=" + e.toString());
      }

      // and ensure it rolls back to previous state
      tempTable2.readSql(con, "SELECT * FROM " + tempTableName);
      Test.ensureEqual(tempTable, tempTable2, "");

      // *** test successfully add data (and ensure previous rollbacks worked
      // assign row numbers so new rows can be added (and so different from possibly
      // added 5,6,7,8)
      tempTable2.setIntData(0, 0, 9); // ok,
      tempTable2.setIntData(0, 1, 10); // ok
      tempTable2.setIntData(0, 2, 11); // ok
      tempTable2.setIntData(0, 3, 12); // ok
      tempTable2.saveAsSql(
          con,
          false, // false, so added to previous data
          tempTableName,
          0,
          new int[] {7},
          new int[] {8},
          new int[] {9},
          1.5);

      // and ensure result has 8 rows
      tempTable2.readSql(con, "SELECT uid, string FROM " + tempTableName);
      Test.ensureEqual(tempTable2.getColumn(0).toString(), "1, 2, 3, 4, 9, 10, 11, 12", "");
      Test.ensureEqual(
          tempTable2.getColumn(1).toString(), "ab, , [null], longer, ab, , [null], longer", "");
    }

    // don't drop the table, so I can view it in phpPgAdmin

    // how read just column names and types? query that returns no rows???

  }

  /** This tests readIObis. */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testIobis() throws Exception {
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // String2.log("\n*** Table.testIobis");
    String testName = "c:/programs/digir/Macrocyctis.nc";
    Table table = new Table();
    if (true) {
      table.readIobis(
          Table.IOBIS_URL,
          "Macrocystis",
          "", // String genus, String species,
          "",
          "",
          "53",
          "54", // String west, String east, String south, String north,
          "",
          "", // String minDepth, String maxDepth,
          "1970-01-01",
          "", // String iso startDate, String iso endDate,
          new String[] {
            "Institutioncode", "Collectioncode", "Scientificname", "Temperature"
          }); // String
      // loadColumns[])

      // table.saveAsFlatNc(testName, "row");
    } else {
      table.readFlatNc(testName, null, 1); // standardizeWhat
    }
    // String2.log(table.toString());
    TableTests.testObis5354Table(table);
  }

  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  // This test relies on setting bits on a static member during all of the
  // existing tests.
  // We can probably use normal coverage tooling instead of properly implemnting
  // this test.
  void testReadNcCFCodeCoverage() {
    Table.ncCFcc.flip(0, 100); // there are currently 99 code coverage tests
    Test.ensureEqual(Table.ncCFcc.toString(), "{}", "Table.readNcCF code coverage");
    Table.ncCFcc = null; // turn off test of readNcCF code coverage
  }

  /**
   * Make a test Table.
   *
   * @param includeLongs set this to true if you want a column with longs
   * @param includeStrings set this to true if you want a column with Strings
   * @return Table
   */
  public static Table getTestTable(boolean includeLongs, boolean includeStrings) {

    String testTimes[] = {"1970-01-01T00:00:00", "2005-08-31T16:01:02", "2005-11-02T18:04:09", ""};
    Table table = new Table();
    int nRows = testTimes.length;

    // global attributes
    table.globalAttributes().set("global_att1", "a string");
    table.globalAttributes().set("global_att2", new IntArray(new int[] {1, 100}));

    // add the data variables (and their attributes)

    // 0=seconds
    double[] ad = new double[nRows];
    for (int i = 0; i < nRows; i++) ad[i] = Calendar2.safeIsoStringToEpochSeconds(testTimes[i]);
    int col = table.addColumn("Time", new DoubleArray(ad));
    table.columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
    table.columnAttributes(col).set("time_att2", new DoubleArray(new double[] {-1e300, 1e300}));

    // 1=lon
    int[] ai = {-3, -2, -1, Integer.MAX_VALUE};
    col = table.addColumn("Longitude", new IntArray(ai).setMaxIsMV(true));
    table.columnAttributes(col).set("units", "degrees_east");
    table.columnAttributes(col).set("lon_att2", new IntArray(new int[] {-2000000000, 2000000000}));

    // 2=lat
    float[] af = {1, 1.5f, 2, Float.NaN};
    col = table.addColumn("Latitude", new FloatArray(af));
    table.columnAttributes(col).set("units", "degrees_north");
    table.columnAttributes(col).set("lat_att2", new FloatArray(new float[] {-1e30f, 1e30f}));

    // 3=double
    ad = new double[] {-1e300, 3.123, 1e300, Double.NaN};
    col = table.addColumn("Double Data", new DoubleArray(ad));
    table.columnAttributes(col).set("units", "doubles");
    table.columnAttributes(col).set("double_att2", new DoubleArray(new double[] {-1e300, 1e300}));

    // 4=long
    if (includeLongs) {
      long[] al = {-2000000000000000L, 2, 2000000000000000L, Long.MAX_VALUE};
      col = table.addColumn("Long Data", new LongArray(al).setMaxIsMV(true));
      table.columnAttributes(col).set("units", new StringArray(new String[] {"longs"}));
      table
          .columnAttributes(col)
          .set("long_att2", new LongArray(new long[] {-2000000000000000L, 2000000000000000L}));
    }

    // 5=int
    ai = new int[] {-2000000000, 2, 2000000000, Integer.MAX_VALUE};
    col = table.addColumn("Int Data", new IntArray(ai).setMaxIsMV(true));
    table.columnAttributes(col).set("units", new StringArray(new String[] {"ints"}));
    table.columnAttributes(col).set("int_att2", new IntArray(new int[] {-2000000000, 2000000000}));

    // 6=short
    short[] as = {(short) -32000, (short) 7, (short) 32000, Short.MAX_VALUE};
    col = table.addColumn("Short Data", new ShortArray(as).setMaxIsMV(true));
    table.columnAttributes(col).set("units", new StringArray(new String[] {"shorts"}));
    table
        .columnAttributes(col)
        .set("short_att2", new ShortArray(new short[] {(short) -30000, (short) 30000}));

    // 7=byte
    byte[] ab = {(byte) -120, (byte) 8, (byte) 120, Byte.MAX_VALUE};
    col = table.addColumn("Byte Data", new ByteArray(ab).setMaxIsMV(true));
    table.columnAttributes(col).set("units", "bytes");
    table
        .columnAttributes(col)
        .set("byte_att2", new ByteArray(new byte[] {(byte) -120, (byte) 120}));

    // 8=String
    if (includeStrings) {
      char[] ac = {',', '"', '\u20ac', '\uffff'};
      col = table.addColumn("Char Data", new CharArray(ac).setMaxIsMV(true));
      table.columnAttributes(col).set("units", "chars");
      table
          .columnAttributes(col)
          .set("char_att2", new CharArray(new char[] {',', '"', '\u00fc', '\u20ac'}));

      String[] aS = {"a", "bb", "ccc", ""};
      col = table.addColumn("String Data", new StringArray(aS));
      table.columnAttributes(col).set("units", "Strings");
      table.columnAttributes(col).set("String_att2", new StringArray(new String[] {"a string"}));
    }

    table.ensureValid(); // throws Exception if not
    return table;
  }

  /** This makes a tough test table. */
  private static Table makeToughTestTable() {

    Table table = new Table();

    Attributes gatts = table.globalAttributes();
    gatts.add("tests", "a\u00fcb\nc\td\u20acz");
    gatts.add("testc", '\u00fc');
    gatts.add("testub", UByteArray.fromCSV("0, 255"));
    gatts.add("testus", UShortArray.fromCSV("0, 65535"));
    gatts.add("testui", UIntArray.fromCSV("0, 4294967295"));
    gatts.add("testl", new LongArray(new long[] {Long.MIN_VALUE, Long.MAX_VALUE}));
    gatts.add("testul", ULongArray.fromCSV("0, 9223372036854775807, 18446744073709551615"));
    gatts.add("testi", new IntArray(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}));

    table.addColumn(
        0,
        "aString",
        new StringArray(new String[] {"a\u00fcb\nc\td\u20ace", "ab", "", "cd", ""}),
        new Attributes().add("test", "a\u00fcb\nc\td\u20ace"));

    table.addColumn(
        1,
        "aChar",
        new CharArray(new char[] {'\u00fc', (char) 0, 'A', '\t', '\u20ac'}),
        new Attributes().add("test", '\u00fc'));

    table.addColumn(
        2,
        "aByte", // min, med, fv, max-1, max
        new ByteArray(new byte[] {Byte.MIN_VALUE, 0, 99, 126, Byte.MAX_VALUE}).setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", ByteArray.fromCSV("99"))
            .add("test", ByteArray.fromCSV("-128, 127")));
    table.addColumn(
        3,
        "aUByte",
        new UByteArray(new short[] {0, 127, 99, 254, 255}).setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", UByteArray.fromCSV("99"))
            .add("test", UByteArray.fromCSV("0, 255")));
    table.addColumn(
        4,
        "aShort",
        new ShortArray(new short[] {Short.MIN_VALUE, 0, 9999, 32766, Short.MAX_VALUE})
            .setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", ShortArray.fromCSV("9999"))
            .add("test", ShortArray.fromCSV("-32768, 32767")));
    table.addColumn(
        5,
        "aUShort",
        UShortArray.fromCSV("0, 32767, 9999, 65534, 65535").setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", UShortArray.fromCSV("9999"))
            .add("test", UShortArray.fromCSV("0, 65535")));
    table.addColumn(
        6,
        "anInt",
        new IntArray(
                new int[] {
                  Integer.MIN_VALUE, 0, 999999999, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
                })
            .setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", IntArray.fromCSV("999999999"))
            .add("test", IntArray.fromCSV("-2147483648, 2147483647")));
    table.addColumn(
        7,
        "aUInt",
        new UIntArray(
                new long[] {
                  0, 7, Integer.MAX_VALUE, Math2.UINT_MAX_VALUE - 1, Math2.UINT_MAX_VALUE
                })
            .setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", UIntArray.fromCSV("999999999"))
            .add("test", UIntArray.fromCSV("0, 4294967295")));
    table.addColumn(
        8,
        "aLong",
        new LongArray(new long[] {Long.MIN_VALUE, 0, 8, Long.MAX_VALUE - 1, Long.MAX_VALUE})
            .setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", LongArray.fromCSV("999999999999"))
            .add("test", LongArray.fromCSV("-9223372036854775808, 9223372036854775807")));
    table.addColumn(
        9,
        "aULong",
        ULongArray.fromCSV("0,  1, 9223372036854775807, 18446744073709551614, 18446744073709551615")
            .setMaxIsMV(true),
        new Attributes()
            .add("_FillValue", ULongArray.fromCSV("999999999999"))
            .add("test", ULongArray.fromCSV("0, 18446744073709551615")));
    table.addColumn(
        10,
        "aFloat",
        new FloatArray(
            new float[] {-Float.MAX_VALUE, 2.2f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN}),
        new Attributes()
            .add("_FillValue", 1e36f)
            .add("test", new FloatArray(new float[] {-Float.MAX_VALUE, Float.NaN})));
    table.addColumn(
        11,
        "aDouble",
        new DoubleArray(
            new double[] {-Double.MAX_VALUE, 3.3, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN}),
        new Attributes()
            .add("_FillValue", 1e300)
            .add("test", new DoubleArray(new double[] {-Double.MAX_VALUE, Double.NaN})));
    return table;
  }

  /**
   * This tests that the values in this table are the expected results from the typical obis
   * "Macrocystis", time 1970+, lat 53.. 54 request.
   */
  public static void testObis5354Table(Table table) {
    String2.log("\n*** Table.testObis5354Table...");
    table.leftToRightSort(5);

    Test.ensureTrue(table.nRows() >= 30, "nRows=" + table.nRows());
    Test.ensureEqual(table.nColumns(), 9, "");
    if (String2.toCSSVString(table.getColumnNames())
        .equals(
            "LON, LAT, DEPTH, TIME, ID, "
                + "darwin:InstitutionCode, darwin:CollectionCode, "
                + "darwin:ScientificName, obis:Temperature")) {
    } else if (String2.toCSSVString(table.getColumnNames())
        .equals(
            "LON, LAT, DEPTH, TIME, ID, "
                + "Institutioncode, Collectioncode, "
                + "Scientificname, Temperature")) {
    } else
      throw new RuntimeException(
          "Unexpected col names: " + String2.toCSSVString(table.getColumnNames()));

    // !!!note that from GHMP request, rows of data are in pairs of almost
    // duplicates
    // and CollectionCode includes 2 sources -- 1 I requested and another one (both
    // served by GHMP?)
    // and Lat and Lon can be slightly different (e.g., row 60/61 lat)
    DoubleArray latCol = (DoubleArray) table.getColumn(1);
    double stats[] = latCol.calculateStats();
    Test.ensureTrue(
        stats[PrimitiveArray.STATS_MIN] >= 53, "min=" + stats[PrimitiveArray.STATS_MIN]);
    Test.ensureTrue(
        stats[PrimitiveArray.STATS_MAX] <= 54, "max=" + stats[PrimitiveArray.STATS_MAX]);
    Test.ensureEqual(stats[PrimitiveArray.STATS_N], table.nRows(), "");

    // test time > 0 (1970-01-01)
    DoubleArray timeCol = (DoubleArray) table.getColumn(3);
    stats = timeCol.calculateStats();
    Test.ensureTrue(stats[PrimitiveArray.STATS_MIN] >= 0, "min=" + stats[PrimitiveArray.STATS_MIN]);
    Test.ensureEqual(stats[PrimitiveArray.STATS_N], table.nRows(), "");

    DoubleArray lonCol = (DoubleArray) table.getColumn(0); // ==0
    int row = lonCol.indexOf("-132.4223");
    Test.ensureEqual(table.getDoubleData(0, row), -132.4223, "");
    Test.ensureEqual(table.getDoubleData(1, row), 53.292, "");
    Test.ensureEqual(table.getDoubleData(2, row), Double.NaN, "");
    Test.ensureEqual(table.getDoubleData(3, row), 347155200, "");
    Test.ensureEqual(table.getStringData(4, row), "BIO:GHMP:10036-MACRINT", "");
    Test.ensureEqual(table.getStringData(5, row), "BIO", "");
    Test.ensureEqual(table.getStringData(6, row), "GHMP", "");
    Test.ensureEqual(table.getStringData(7, row), "Macrocystis integrifolia", "");
    Test.ensureEqual(table.getDoubleData(8, row), Double.NaN, "");
    /*
     * duplicates (described above) disappeared 2007-09-04
     * row++;
     * Test.ensureEqual(table.getDoubleData(0, row), -132.4223, "");
     * Test.ensureEqual(table.getDoubleData(1, row), 53.292, "");
     * Test.ensureEqual(table.getDoubleData(2, row), Double.NaN, "");
     * Test.ensureEqual(table.getDoubleData(3, row), 347155200, "");
     * Test.ensureEqual(table.getStringData(4, row),
     * "Marine Fish Division, Fisheries and Oceans Canada:Gwaii Haanas Marine Algae:10036-MACRINT"
     * , "");
     * Test.ensureEqual(table.getStringData(5, row),
     * "Marine Fish Division, Fisheries and Oceans Canada", "");
     * Test.ensureEqual(table.getStringData(6, row), "Gwaii Haanas Marine Algae",
     * "");
     * Test.ensureEqual(table.getStringData(7, row), "Macrocystis integrifolia",
     * "");
     * Test.ensureEqual(table.getDoubleData(8, row), Double.NaN, "");
     */
    row = lonCol.indexOf("-132.08171");
    Test.ensureEqual(table.getDoubleData(0, row), -132.08171, "");
    Test.ensureEqual(table.getDoubleData(1, row), 53.22519, "");
    Test.ensureEqual(table.getDoubleData(2, row), Double.NaN, "");
    Test.ensureEqual(table.getDoubleData(3, row), 63072000, "");
    Test.ensureEqual(table.getStringData(4, row), "BIO:GHMP:198-MACRINT", "");
    Test.ensureEqual(table.getStringData(5, row), "BIO", "");
    Test.ensureEqual(table.getStringData(6, row), "GHMP", "");
    Test.ensureEqual(table.getStringData(7, row), "Macrocystis integrifolia", "");
    Test.ensureEqual(table.getDoubleData(8, row), Double.NaN, "");
    /*
     * row++;
     * Test.ensureEqual(table.getDoubleData(0, row), -132.08171, "");
     * Test.ensureEqual(table.getDoubleData(1, row), 53.225193, "");
     * Test.ensureEqual(table.getDoubleData(2, row), Double.NaN, "");
     * Test.ensureEqual(table.getDoubleData(3, row), 63072000, "");
     * Test.ensureEqual(table.getStringData(4, row),
     * "Marine Fish Division, Fisheries and Oceans Canada:Gwaii Haanas Marine Algae:198-MACRINT"
     * , "");
     * Test.ensureEqual(table.getStringData(5, row),
     * "Marine Fish Division, Fisheries and Oceans Canada", "");
     * Test.ensureEqual(table.getStringData(6, row), "Gwaii Haanas Marine Algae",
     * "");
     * Test.ensureEqual(table.getStringData(7, row), "Macrocystis integrifolia",
     * "");
     * Test.ensureEqual(table.getDoubleData(8, row), Double.NaN, "");
     */

    String2.log("Table.testObis5354Table finished successfully.");
  }

  @org.junit.jupiter.api.Test
  void testNccsvInteractive() throws Exception {

    String dir = TableTests.class.getResource("/data/nccsv/").getPath();
    boolean haveExcel = false; // as of ~2020, I no longer have excel

    // *** test 1.1 file
    // test round trip to spreadsheet and back
    // make a copy of sampleScalar
    String fileName = dir + "sampleExcel_1.1.csv";
    File2.writeToFile88591(fileName, File2.directReadFrom88591File(dir + "testScalar_1.1.csv"));

    // if (haveExcel) {
    // Test.displayInBrowser("file://" + fileName); //.csv
    // String2.pressEnterToContinue("\nIn Excel, use File : Save As : CSV : as
    // sampleExcel_1.1.csv : yes : yes.");
    // }
    Table table = new Table();
    table.readNccsv(fileName);
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    String results = table.saveAsNccsv();
    String expected = // it gets converted to nccsv 1.2
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,featureType,trajectory\n"
            + "*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"NOAA, sea, ship, sst, surface, temperature, trajectory\"\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,ship\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "ship,*SCALAR*,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "ship,cf_role,trajectory_id\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,standard_name,time\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "lat,*DATA_TYPE*,double\n"
            + "lat,units,degrees_north\n"
            + "lon,*DATA_TYPE*,double\n"
            + "lon,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,0.17f,23.58f\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "time,lat,lon,status,testByte,testUByte,testLong,testULong,sst\n"
            + "2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n"
            + "2017-03-23T01:45:00Z,28.0003,-130.3472,\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n"
            + "2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n"
            + "2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n"
            + (haveExcel ? "BAD ROW: TOO FEW ITEMS,,,?,,\n" : "")
            + // this disappears if I don't actually do
            // Excel Save
            // As
            "2017-03-23T21:45:00Z,28.0003,-132.0014,\u00fc,,,,,\n"
            + "2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n"
            + "*END_DATA*\n";
    // try {
    Test.ensureEqual(results, expected, "results=\n" + results);
    // } catch (Exception e) {
    // Test.knownProblem(
    // "1.1: How to keep integer in string att as a string?!",
    // "If I don't actually do Excel 'Save As', the BAD ROW disappears.", e);
    // }

    // *** test 1.2 file
    // test round trip to spreadsheet and back
    // make a copy of sampleScalar
    fileName = dir + "sampleExcel_1.2.csv";
    File2.writeToFileUtf8(fileName, File2.directReadFromUtf8File(dir + "testScalar_1.1.csv"));
    // if (haveExcel) {
    // Test.displayInBrowser("file://" + fileName); //.csv
    // String2.pressEnterToContinue("\nIn Excel, use File : Save As : CSV : as
    // sampleExcel_1.2.csv : yes : yes.");
    // }
    table = new Table();
    table.readNccsv(fileName);
    for (int c = 0; c < table.nColumns(); c++) {
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_SCALAR) == null, "col=" + c);
      Test.ensureTrue(table.columnAttributes(c).get(String2.NCCSV_DATATYPE) == null, "col=" + c);
    }
    results = table.saveAsNccsv();

    // try {
    Test.ensureEqual(results, expected, "results=\n" + results);
    // } catch (Exception e) {
    // Test.knownProblem(
    // "1.2: How to keep integer in string att as a string?!",
    // "If I don't actually do Excel 'Save As', the BAD ROW disappears.", e);
    // }

  }

  /**
   * This tests readAudioFile with stereo 16 bit data. This is a destructive test: it removes the
   * first second of the data.
   *
   * @param fullName e.g., String2.unitTestDataDir + "audio/M1F1-int16-AFsp.wav" or my test re-write
   *     of it: from http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples.html
   */
  void testReadShortAudioFile(String fullName) throws Exception {
    String2.log("* testReadShortAudioFile(" + fullName + ")");
    Table table = new Table();
    String results, expected;

    // without elapsed time
    table.readAudioFile(fullName, true, false); // readData, addElapsedTime
    results = table.toString(10);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 23493 ;\n"
            + "variables:\n"
            + "\tshort channel_1(row) ;\n"
            + "\t\tchannel_1:long_name = \"Channel 1\" ;\n"
            + "\tshort channel_2(row) ;\n"
            + "\t\tchannel_2:long_name = \"Channel 2\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:audioBigEndian = \"false\" ;\n"
            + "\t\t:audioChannels = 2 ;\n"
            + "\t\t:audioEncoding = \"PCM_SIGNED\" ;\n"
            + "\t\t:audioFrameRate = 8000.0f ;\n"
            + "\t\t:audioFrameSize = 4 ;\n"
            + "\t\t:audioSampleRate = 8000.0f ;\n"
            + "\t\t:audioSampleSizeInBits = 16 ;\n"
            + "}\n"
            + "channel_1,channel_2\n"
            + "0,0\n"
            + "0,0\n"
            + "1,2\n"
            + "-3,0\n"
            + "0,-4\n"
            + "6,4\n"
            + "-2,1\n"
            + "-2,-2\n"
            + "1,-4\n"
            + "-1,-4\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get elapsed time
    table.readAudioFile(fullName, true, true); // readData, addElapsedTime
    results = table.toString(10);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 23493 ;\n"
            + "variables:\n"
            + "\tdouble elapsedTime(row) ;\n"
            + "\t\telapsedTime:long_name = \"Elapsed Time\" ;\n"
            + "\t\telapsedTime:units = \"seconds\" ;\n"
            + "\tshort channel_1(row) ;\n"
            + "\t\tchannel_1:long_name = \"Channel 1\" ;\n"
            + "\tshort channel_2(row) ;\n"
            + "\t\tchannel_2:long_name = \"Channel 2\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:audioBigEndian = \"false\" ;\n"
            + "\t\t:audioChannels = 2 ;\n"
            + "\t\t:audioEncoding = \"PCM_SIGNED\" ;\n"
            + "\t\t:audioFrameRate = 8000.0f ;\n"
            + "\t\t:audioFrameSize = 4 ;\n"
            + "\t\t:audioSampleRate = 8000.0f ;\n"
            + "\t\t:audioSampleSizeInBits = 16 ;\n"
            + "}\n"
            + "elapsedTime,channel_1,channel_2\n"
            + "0.0,0,0\n"
            + "1.25E-4,0,0\n"
            + "2.5E-4,1,2\n"
            + "3.75E-4,-3,0\n"
            + "5.0E-4,0,-4\n"
            + "6.25E-4,6,4\n"
            + "7.5E-4,-2,1\n"
            + "8.75E-4,-2,-2\n"
            + "0.001,1,-4\n"
            + "0.001125,-1,-4\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Test.displayInBrowser("file://" + fullName); //.wav
    // String2.pressEnterToContinue("Close the audio player if file is okay.");
  }

  /**
   * This tests reading a wav file with stereo float data.
   *
   * @param fullName String2.unitTestDataDir + "audio/M1F1-float32-AFsp.wav" or my test re-write of
   *     it: from http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples.html
   */
  void testReadFloatAudioFile(String fullName) throws Exception {
    String2.log("* testReadFloatAudioFile(" + fullName + ")");
    Table table = new Table();
    String results, expected;

    table.readAudioFile(fullName, true, false); // readData, addElapsedTime
    results = table.toString(10);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 23493 ;\n"
            + "variables:\n"
            + "\tfloat channel_1(row) ;\n"
            + "\t\tchannel_1:long_name = \"Channel 1\" ;\n"
            + "\tfloat channel_2(row) ;\n"
            + "\t\tchannel_2:long_name = \"Channel 2\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:audioBigEndian = \"false\" ;\n"
            + "\t\t:audioChannels = 2 ;\n"
            + "\t\t:audioEncoding = \"PCM_FLOAT\" ;\n"
            + "\t\t:audioFrameRate = 8000.0f ;\n"
            + "\t\t:audioFrameSize = 8 ;\n"
            + "\t\t:audioSampleRate = 8000.0f ;\n"
            + "\t\t:audioSampleSizeInBits = 32 ;\n"
            + "}\n"
            + "channel_1,channel_2\n"
            + "0.0,0.0\n"
            + "0.0,0.0\n"
            + "3.0517578E-5,6.1035156E-5\n"
            + "-9.1552734E-5,0.0\n"
            + "0.0,-1.2207031E-4\n"
            + "1.8310547E-4,1.2207031E-4\n"
            + "-6.1035156E-5,3.0517578E-5\n"
            + "-6.1035156E-5,-6.1035156E-5\n"
            + "3.0517578E-5,-1.2207031E-4\n"
            + "-3.0517578E-5,-1.2207031E-4\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.displayInBrowser("file://" + fullName); //audio
    // String2.pressEnterToContinue("Close the audio player if file is okay.");
  }

  /**
   * This tests reading audio files and and writing WAVE files.
   *
   * @param stop use a big number will be converted to the max available
   */
  @org.junit.jupiter.api.Test
  void testReadAudioWriteWaveFiles() throws Exception {
    int start = 0;
    int stop = 1000;
    String dir = TableTests.class.getResource("/largeFiles/audio/").getPath();
    String names[] = {
      // sample files from
      // http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Samples.html
      "M1F1-int16-AFsp.wav", "M1F1-int32-AFsp.wav",
      "M1F1-uint8-AFsp.wav", "M1F1-int24-AFsp.wav",
      "M1F1-float32-AFsp.wav", "M1F1-mulaw-AFsp.wav",
      // "M1F1-Alaw-AFsp.wav", "M1F1-int12-AFsp.wav", //12bits is not supported
      "addf8-Alaw-GW.wav", "addf8-mulaw-GW.wav",
      // sample files from http://www.class-connection.com/8bit-ulaw.htm
      "Cuckoo_Clock.wav", "ballgame1.wav", // [10]
      // sample files from https://en.wikipedia.org/wiki/WAV
      "8k8bitpcm.wav", "8k16bitpcm.wav", // [12]
      "8kulaw.wav", "11k8bitpcm.wav",
      "11k16bitpcm.wav", "11kulaw.wav",
      // sample files from
      // http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/AU/Samples.html
      "M1F1-Alaw-AFsp.au", "M1F1-mulaw-AFsp.au", // [18]
      "M1F1-int8-AFsp.au", "M1F1-int16-AFsp.au",
      "M1F1-int24-AFsp.au", "M1F1-int32-AFsp.au",
      // "M1F1-float32-AFsp.au", "M1F1-float64-AFsp.au", // float types FAIL: "file is
      // not a supported file type"
      // sample files from
      // http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/AIFF/Samples.html
      // CURRENTLY, ALL AIF SAMPLES FAIL
      // "M1F1-AlawC-AFsp.aif", "M1F1-mulawC-AFsp.aif", // [26]
      // "M1F1-int8-AFsp.aif", "M1F1-int8C-AFsp.aif",
      // "M1F1-int12-AFsp.aif", "M1F1-int12C-AFsp.aif", //12 bits is not supported
      // "M1F1-int16-AFsp.aif", "M1F1-int16C-AFsp.aif",
      // "M1F1-int24-AFsp.aif", "M1F1-int24C-AFsp.aif",
      // "M1F1-int32-AFsp.aif", "M1F1-int32C-AFsp.aif",
      // "M1F1-float32C-AFsp.aif", "M1F1-float64C-AFsp.aif",
      // "M1F1-int16s-AFsp.aif", "" };
    };

    stop = Math.min(stop, names.length - 1);

    Table table = new Table();
    for (int i = start; i <= stop; i++) {
      // try {
      String name = names[i];
      if (name.length() == 0) continue;
      String2.log("\n* Table.testReadWriteWaveFiles #" + i + "=" + dir + name);
      String outName =
          File2.getSystemTempDirectory() + "write" + File2.forceExtension(name, ".wav");
      File2.delete(outName);
      table.readAudioFile(dir + name, true, false); // readData, addElapsedTime
      String2.log(table.dataToString(16));

      table.writeWaveFile(outName);
      table.readAudioFile(outName, true, false); // readData, addElapsedTime
      String2.log(table.dataToString(16));
      // Test.displayInBrowser("file://" + outName); //audio

      // } catch (Exception e) {
      // String2.log(MustBe.throwableToString(e));
      // }
      // String2.pressEnterToContinue("Listen to the audio file. Then ");
    }
  }

  /** This tests writing a WAVE file with float stereo data. */
  @org.junit.jupiter.api.Test
  void testReadWriteFloatWaveFile() throws Exception {
    // String2.log("* testReadWriteFloatWaveFile");
    String fullName = File2.getSystemTempDirectory() + "testFloat.wav";
    File2.delete(fullName);
    Table table = new Table();
    table.readAudioFile(
        TableTests.class.getResource("/largeFiles/audio/M1F1-float32-AFsp.wav").getPath(),
        true,
        false); // readData,
    // addElapsedTime
    table.writeWaveFile(fullName);

    // read it
    // DEAL WITH JAVA 8 BUG
    // boolean java8 = System.getProperty("java.version").startsWith("1.8.");
    // if (java8)
    // Test.displayInBrowser("file://" + fullName); //.wav
    // else
    this.testReadFloatAudioFile(fullName);
    // String2.pressEnterToContinue("Close the audio player if file is okay.");
  }

  @org.junit.jupiter.api.Test
  void testInteractiveNcCFMA() throws Exception {
    boolean pauseAfterEach = false;
    // *************** trajectory multiple multidimensional ---
    // try {

    Table table = new Table();
    String results, expected;
    String pauseMessage = "\nOK?";
    String fileName =
        Path.of(
                TableTests.class
                    .getResource(
                        "/data/CFPointConventions/trajectory/"
                            + "trajectory-Incomplete-Multidimensional-MultipleTrajectories-H.4.1/"
                            + "trajectory-Incomplete-Multidimensional-MultipleTrajectories-H.4.1.nc")
                    .toURI())
            .toString();
    String2.log("\n\n** Testing multiple multidimensional file\n" + "  " + fileName);
    String2.log(NcHelper.ncdump(fileName, "-h"));

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name,lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature,trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    //
    table.readNcCF(
        fileName,
        StringArray.fromCSV("trajectory_name"),
        0, // standardizeWhat=0
        StringArray.fromCSV("trajectory_name"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("lat"),
        0, // standardizeWhat=0
        StringArray.fromCSV("lat"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("temperature"),
        0, // standardizeWhat=0
        StringArray.fromCSV("temperature"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName,
        StringArray.fromCSV("z"),
        0, // standardizeWhat=0
        StringArray.fromCSV("z"),
        StringArray.fromCSV("="),
        StringArray.fromCSV("-12345"));
    Test.ensureEqual(table.nRows(), 0, "");
    Test.ensureEqual(table.nColumns(), 0, "");

    table.readNcCF(
        fileName, null, 0, // standardizeWhat=0
        null, null, null);
    results = table.dataToString(55);
    expected =
        // rows that a human thinks should be rejected are kept
        // probably because trajectory_info and trajectory_name aren't missing values
        // AND because lat,lon have missing values but there is no missing_value
        // attribute.
        "lat,lon,trajectory_info,trajectory_name,time,z,temperature,humidity\n"
            + "2.152863,-35.078842,0,Trajectory0,0,0.0,13.466983,65.38418\n"
            + "33.60481,-44.12696,0,Trajectory0,3600,1.0,23.050304,7.0401154\n"
            + "22.562508,-18.115444,0,Trajectory0,7200,2.0,15.112072,45.15019\n"
            + "13.432817,-21.772585,0,Trajectory0,10800,3.0,22.97767,12.618799\n"
            + "43.011986,-28.655304,0,Trajectory0,14400,4.0,21.318092,4.788235\n"
            + "18.84832,-25.418892,0,Trajectory0,18000,5.0,27.496708,66.337166\n"
            + "18.040411,-30.469133,0,Trajectory0,21600,6.0,30.678926,31.57974\n"
            + "32.34516,-75.79432,0,Trajectory0,25200,7.0,12.096431,11.228316\n"
            + "8.652234,-69.01581,0,Trajectory0,28800,8.0,12.523737,47.003998\n"
            + "18.905367,-18.362652,0,Trajectory0,32400,9.0,22.805552,8.789174\n"
            + "12.184539,-42.194824,0,Trajectory0,36000,10.0,17.411797,40.25377\n"
            + "16.498188,-74.44906,0,Trajectory0,39600,11.0,27.783548,20.712833\n"
            + "1.5479256,-53.522717,0,Trajectory0,43200,12.0,11.809888,5.6157913\n"
            + "22.033587,-28.557417,0,Trajectory0,46800,13.0,13.730549,2.8293543\n"
            + "5.997217,-35.043163,0,Trajectory0,50400,14.0,6.549969,30.482803\n"
            + "8.580469,-45.364418,0,Trajectory0,54000,15.0,11.789269,2.303839\n"
            + "6.253441,-9.302229,0,Trajectory0,57600,16.0,24.03656,56.802467\n"
            + "12.948677,-20.07699,0,Trajectory0,61200,17.0,17.980707,66.24162\n"
            + "41.49208,-19.628315,0,Trajectory0,64800,18.0,0.44739303,25.76894\n"
            + "25.784758,-65.65333,0,Trajectory0,68400,19.0,13.147206,1.4286463\n"
            + "25.884523,-64.92309,0,Trajectory0,72000,20.0,21.278152,72.43937\n"
            + "7.5993505,-33.58001,0,Trajectory0,75600,21.0,14.465093,74.04942\n"
            + "23.801714,-8.210893,0,Trajectory0,79200,22.0,17.250273,43.468597\n"
            + "24.086273,-16.376455,0,Trajectory0,82800,23.0,36.73325,56.15435\n"
            + "8.838917,-65.32871,0,Trajectory0,86400,24.0,21.714993,32.324383\n"
            + "3.049409,-50.187355,0,Trajectory0,90000,25.0,17.755543,7.7604437\n"
            + "32.699135,-13.603052,0,Trajectory0,93600,26.0,21.764454,68.36558\n"
            + "28.82149,-4.238066,0,Trajectory0,97200,27.0,4.18221,75.262665\n"
            + "4.573595,-15.691054,0,Trajectory0,100800,28.0,36.230297,74.156654\n"
            + "30.231867,-29.110548,0,Trajectory0,104400,29.0,10.372004,8.0368805\n"
            + "26.295082,-24.224209,0,Trajectory0,108000,30.0,7.0729938,31.468176\n"
            + "26.146648,-35.461746,0,Trajectory0,111600,31.0,12.3075285,71.35397\n"
            + "18.875525,-11.409157,0,Trajectory0,115200,32.0,30.241188,45.14291\n"
            + "44.57873,-29.37942,0,Trajectory0,118800,33.0,21.847982,61.776512\n"
            + "40.911667,-31.65526,0,Trajectory0,122400,34.0,30.369759,29.810774\n"
            + "9.5415745,-57.1067,0,Trajectory0,126000,35.0,15.864324,33.90924\n"
            +
            // these aren't rejected because
            // * trajectory_info has the attribute missing_value=-999,
            // but the data above has trajectory_info=0, which isn't a missing value.
            // * trajectory_name is a string, so my reader doesn't expect a
            // missing_value attribute, but my reader does treat a data
            // value of "" (nothing) as a missing value. Unfortunately, this
            // file has other values (e.g., "Trajectory0"), so my reader
            // treats those as not missing values.
            // * lat and lon have data values of -999.9 which is probably
            // intended to be a missing value marker, but those variables
            // have no missing_value attributes, so my program treats them as
            // valid values.
            // I reported to Kyle 2012-10-03 and hope he'll make changes

            "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "-999.9,-999.9,0,Trajectory0,-999,-999.9,-999.9,-999.9\n"
            + "13.633951,-61.779095,1,Trajectory1,0,0.0,31.081379,7.8967414\n"
            + "41.583824,-52.047775,1,Trajectory1,3600,1.0,23.026224,25.034555\n"
            + "14.176689,-15.26571,1,Trajectory1,7200,2.0,21.750353,61.35738\n"
            + "23.482225,-69.246284,1,Trajectory1,10800,3.0,25.214777,70.63098\n"
            + "8.910114,-12.47847,1,Trajectory1,14400,4.0,29.234118,53.387733\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    Test.ensureEqual(table.nRows(), 200, "");
    results = table.columnAttributes(5).toString(); // temperature
    expected =
        "    axis=Z\n"
            + "    long_name=height above mean sea level\n"
            + "    missing_value=-999.9f\n"
            + "    positive=up\n"
            + "    standard_name=altitude\n"
            + "    units=m\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    if (pauseAfterEach) String2.pressEnterToContinue("#8b " + pauseMessage);

    // } catch (Exception e) {
    // //String2.pressEnterToContinue(
    // Test.knownProblem(
    // "KYLE WILCOX'S DSG TEST FILE.",
    // MustBe.throwableToString(e) +
    // "\nI reported this problem to Kyle 2012-10-03" +
    // "\n2013-10-30 Since Kyle changed jobs, it is unlikely he will ever fix
    // this.");
    // }
  }

  /**
   * DOESN'T WORK. Driver gives error: Exception in thread "main" java.sql.SQLException:
   * [Microsoft][ODBC Microsoft Access Driver]Optional feature not implemented at
   * sun.jdbc.odbc.JdbcOdbc.createSQLException(Unknown Source) ...
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // wasn't run before, also: ClassNotFound sun.jdbc.odbc.JdbcOdbcDriver
  void testMdb() throws Exception {
    String fileName =
        TableTests.class
            .getResource("/notIncludedFiles/calcofi2012/calcofi8102012.accdb")
            .getPath();
    // "c:/fishbase/COUNTRY.mdb";
    Connection con = Table.getConnectionToMdb(fileName, "", ""); // user, password
    // String2.log(getSqlSchemas(con).toString());
    // String schema = "";
    // String2.log(getSqlTableNames(con, schema, null).toString()); //null for all
    // types
    String tableName = "EGG_COUNTS"; // "ACTIVITIES";
    // query = "SELECT * FROM names WHERE id = 3"
    String query = "SELECT * FROM " + tableName;
    Table table = new Table();
    table.readSql(con, query);
    String2.log(table.dataToString(5));
  }
}
