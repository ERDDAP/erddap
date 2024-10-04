package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class RegexFilenameFilterTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods of RegexFilenameFilter.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n* RegexFilenameFilter.basicTest ...");
    String coastwatchDir =
        File2.getClassPath() // with / separator and / at the end
            + "gov/noaa/pfel/coastwatch/";

    // test list
    String[] sar = RegexFilenameFilter.list(coastwatchDir, "T.+\\.class");
    String[] shouldBe = {"TestAll.class", "TimePeriods.class"};
    System.out.println(coastwatchDir);
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.list");

    // test fullNameList
    sar = RegexFilenameFilter.fullNameList(coastwatchDir, "T.+\\.class");
    shouldBe = new String[] {coastwatchDir + "TestAll.class", coastwatchDir + "TimePeriods.class"};
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.fullNameList");

    // test recursiveList
    sar = RegexFilenameFilter.recursiveFullNameList(coastwatchDir, "S.+\\.class", true);
    shouldBe =
        new String[] {
          coastwatchDir + "griddata/",
          coastwatchDir + "griddata/SaveOpendap.class",
          coastwatchDir + "hdf/",
          coastwatchDir + "hdf/SdsReader.class",
          coastwatchDir + "hdf/SdsWriter.class",
          coastwatchDir + "netcheck/",
          coastwatchDir + "pointdata/",
          coastwatchDir + "pointdata/ScriptRow.class",
          coastwatchDir + "pointdata/parquet/",
          coastwatchDir + "sgt/",
          coastwatchDir + "sgt/SGTPointsVector.class",
          coastwatchDir + "sgt/SgtGraph.class",
          coastwatchDir + "sgt/SgtMap.class",
          coastwatchDir + "sgt/SgtUtil.class",
          coastwatchDir + "util/",
          coastwatchDir + "util/SSR$1.class",
          coastwatchDir + "util/SSR.class",
          coastwatchDir + "util/SharedWatchService.class",
          coastwatchDir + "util/SimpleXMLReader.class"
        };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");

    // test recursiveList no directories
    sar = RegexFilenameFilter.recursiveFullNameList(coastwatchDir, "S.+\\.class", false);
    shouldBe =
        new String[] {
          coastwatchDir + "griddata/SaveOpendap.class",
          coastwatchDir + "hdf/SdsReader.class",
          coastwatchDir + "hdf/SdsWriter.class",
          coastwatchDir + "pointdata/ScriptRow.class",
          coastwatchDir + "sgt/SGTPointsVector.class",
          coastwatchDir + "sgt/SgtGraph.class",
          coastwatchDir + "sgt/SgtMap.class",
          coastwatchDir + "sgt/SgtUtil.class",
          coastwatchDir + "util/SSR$1.class",
          coastwatchDir + "util/SSR.class",
          coastwatchDir + "util/SharedWatchService.class",
          coastwatchDir + "util/SimpleXMLReader.class"
        };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");

    // gatherInfo
    PrimitiveArray info[] = RegexFilenameFilter.gatherInfo(coastwatchDir, "Browser.*");
    int tn = info[1].size();
    StringArray lastMod = new StringArray();
    for (int i = 0; i < tn; i++)
      lastMod.add(Calendar2.safeEpochSecondsToIsoStringTZ(info[2].getLong(i) / 1000.0, "ERROR"));
    Test.ensureEqual(info[0].toString(), "griddata, hdf, netcheck, pointdata, sgt, util", "");
    Test.ensureEqual(info[1].toString(), "BrowserDefault.properties", "");
    // The below is flaky. Consider using a dedicated test resource directory
    // instead of a code directory
    // and turn these back on.
    // Test.ensureEqual(lastMod.toString(), "2007-04-23T18:24:38Z,
    // 2007-05-02T19:18:32Z", "");
    // Test.ensureEqual(info[3].toString(),
    // "155937, 359559, 89254", "");
  }
}
