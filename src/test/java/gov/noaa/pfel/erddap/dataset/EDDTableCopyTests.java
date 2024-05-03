package gov.noaa.pfel.erddap.dataset;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.erddap.util.EDStatic;
import tags.TagMissingDataset;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableCopyTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * The tests testRepPostDet.
   * 
   */
  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  @TagMissingDataset
  void testRepPostDet(boolean tCheckSourceData) throws Throwable {
    String2.log("\n****************** EDDTableCopy.testRepPostDet(tCheckSourceData=" +
        tCheckSourceData + ") *****************\n");
    // testVerboseOn();
    int language = 0;
    // defaultCheckSourceData = tCheckSourceData;
    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    long eTime;
    EDDTable edd = null;

    // maxChunks = 400;

    try {
      edd = (EDDTableCopy) EDDTestDataset.getrepPostDet();
    } catch (Throwable t2) {
      // it will fail if no files have been copied
      String2.log(MustBe.throwableToString(t2));
    }
    if (tCheckSourceData && EDStatic.nUnfinishedTasks() > 0) {
      while (EDStatic.nUnfinishedTasks() > 0) {
        String2.log("nUnfinishedTasks=" + EDStatic.nUnfinishedTasks());
        Math2.sleep(10000);
      }
      // recreate edd to see new copied data files
      edd = (EDDTableCopy) EDDTestDataset.getrepPostDet();
    }
    // reallyVerbose=false;

    // .dds
    eTime = System.currentTimeMillis();
    tName = edd.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory,
        edd.className() + "_postDet", ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    Float64 longitude;\n" +
        "    Float64 latitude;\n" +
        "    Float64 time;\n" +
        "    String common_name;\n" +
        "    String pi;\n" +
        "    String project;\n" +
        "    Int32 surgery_id;\n" +
        "    String tag_id_code;\n" +
        "    String tag_sn;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .das
    eTime = System.currentTimeMillis();
    tName = edd.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory,
        edd.className() + "_postDet", ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "Attributes {\n" +
        " s {\n" +
        "  longitude {\n" +
        "    String _CoordinateAxisType \"Lon\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "    String infoUrl \"http://www.postprogram.org/\";\n" +
        "    String institution \"POST\";\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "  surgery_id {\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // 1var
    eTime = System.currentTimeMillis();
    tName = edd.makeNewFileForDapQuery(language, null, null,
        "pi&distinct()",
        EDStatic.fullTestCacheDirectory, edd.className() + "_postDet1Var", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected = "pi\n" +
        "\n" +
        "BARRY BEREJIKIAN\n" +
        "CEDAR CHITTENDEN\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    String2.log("*** 1var elapsed time=" + (System.currentTimeMillis() - eTime) +
        "ms (vs 148,000 or 286,000 ms for POST).");

    // 2var
    eTime = System.currentTimeMillis();
    tName = edd.makeNewFileForDapQuery(language, null, null,
        "pi,common_name&distinct()",
        EDStatic.fullTestCacheDirectory, edd.className() + "_postDet2var", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // this will change
        "pi,common_name\n" +
            ",\n" +
            "BARRY BEREJIKIAN,STEELHEAD\n" +
            "CEDAR CHITTENDEN,COHO\n" +
            "CHRIS WOOD,\"SOCKEYE,KOKANEE\"\n" +
            "CHUCK BOGGS,COHO\n" +
            "DAVID WELCH,CHINOOK\n" +
            "DAVID WELCH,COHO\n" +
            "DAVID WELCH,DOLLY VARDEN\n" +
            "DAVID WELCH,\"SOCKEYE,KOKANEE\"\n" +
            "DAVID WELCH,STEELHEAD\n" +
            "FRED GOETZ,CHINOOK\n" +
            "FRED GOETZ,CUTTHROAT\n" +
            "JACK TIPPING,STEELHEAD\n" +
            "JEFF MARLIAVE,BLACK ROCKFISH\n" +
            "JOHN PAYNE,SQUID\n" +
            "LYSE GODBOUT,\"SOCKEYE,KOKANEE\"\n" +
            "MIKE MELNYCHUK,COHO\n" +
            "MIKE MELNYCHUK,\"SOCKEYE,KOKANEE\"\n" +
            "MIKE MELNYCHUK,STEELHEAD\n" +
            "ROBERT BISON,STEELHEAD\n" +
            "SCOTT STELTZNER,CHINOOK\n" +
            "SCOTT STELTZNER,COHO\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log(results);
    String2.log("*** 2var elapsed time=" + (System.currentTimeMillis() - eTime) +
        "ms (vs 192,000 ms for POST).");

    // 3var
    eTime = System.currentTimeMillis();
    tName = edd.makeNewFileForDapQuery(language, null, null,
        "pi,common_name,surgery_id&distinct()",
        EDStatic.fullTestCacheDirectory, edd.className() + "_postDet3var", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "pi,common_name,surgery_id\n" +
        ",,\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "BARRY BEREJIKIAN,STEELHEAD,2846\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    String lines[] = String2.split(results, '\n');
    Test.ensureEqual(lines.length, 4317 + 3, "\nresults=\n" + results);
    lines = null;
    String2.log("*** 3var elapsed time=" + (System.currentTimeMillis() - eTime) +
        "ms (vs 152,000ms for POST).");

    // 1tag
    eTime = System.currentTimeMillis();
    tName = edd.makeNewFileForDapQuery(language, null, null,
        "&pi=\"BARRY BEREJIKIAN\"&common_name=\"STEELHEAD\"&surgery_id=2846",
        EDStatic.fullTestCacheDirectory, edd.className() + "_postDet1tag", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
        "degrees_east,degrees_north,UTC,,,,,,\n" +
        "-127.34393,50.67973,2004-05-30T06:08:40Z,STEELHEAD,BARRY BEREJIKIAN,NOAA|NOAA FISHERIES,2846,3985,1031916\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    String2.log("*** 1tag elapsed time=" + (System.currentTimeMillis() - eTime) +
        "ms (vs 5,700ms for POST).");

    // constraint
    eTime = System.currentTimeMillis();
    tQuery = "&pi=\"DAVID WELCH\"&common_name=\"CHINOOK\"&latitude>50" +
        "&surgery_id>=1201&surgery_id<1202&time>=2007-05-01T08&time<2007-05-01T09";
    tName = edd.makeNewFileForDapQuery(language, null, null, tQuery, EDStatic.fullTestCacheDirectory,
        edd.className() + "_peb_constrained", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = "longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
        "degrees_east,degrees_north,UTC,,,,,,\n" +
        "-127.48843,50.78142,2007-05-01T08:43:33Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
        "-127.48843,50.78142,2007-05-01T08:48:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
        "-127.48843,50.78142,2007-05-01T08:51:14Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
        "-127.48843,50.78142,2007-05-01T08:53:18Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
        "-127.48843,50.78142,2007-05-01T08:56:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
        "-127.48843,50.78142,2007-05-01T08:59:27Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n";
    Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())),
        expected, "\nresults=\n" + results);
    String2.log("*** constraint elapsed time=" + (System.currentTimeMillis() - eTime) +
        "ms (usually 31).");

    // done
    // String2.pressEnterToContinue("EDDTableCopy.testRepPostDet done.");

    // defaultCheckSourceData = true;

  } // end of testRepPostDet
}
