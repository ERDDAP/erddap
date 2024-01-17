package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

class RegexFilenameFilterTests {

  /**
   * This tests the methods of RegexFilenameFilter.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n* RegexFilenameFilter.basicTest ...");
    String coastwatchDir = File2.getClassPath() // with / separator and / at the end
        + "gov/noaa/pfel/coastwatch/";

    // test list
    String[] sar = RegexFilenameFilter.list(coastwatchDir, "S.+\\.java");
    String[] shouldBe = {
        "Screen.java",
        "Shared.java" };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.list");

    // test fullNameList
    sar = RegexFilenameFilter.fullNameList(coastwatchDir, "S.+\\.java");
    shouldBe = new String[] {
        coastwatchDir + "Screen.java",
        coastwatchDir + "Shared.java"
    };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.fullNameList");

    // test recursiveList
    sar = RegexFilenameFilter.recursiveFullNameList(coastwatchDir, "S.+\\.java", true);
    shouldBe = new String[] {
        coastwatchDir + "Screen.java",
        coastwatchDir + "Shared.java",
        coastwatchDir + "griddata/",
        coastwatchDir + "griddata/SaveOpendap.java",
        coastwatchDir + "hdf/",
        coastwatchDir + "hdf/SdsReader.java",
        coastwatchDir + "hdf/SdsWriter.java",
        coastwatchDir + "netcheck/",
        coastwatchDir + "pointdata/",
        coastwatchDir + "pointdata/ScriptRow.java",
        coastwatchDir + "pointdata/StationVariableNc4D.java",
        coastwatchDir + "pointdata/StoredIndex.java",
        coastwatchDir + "sgt/",
        coastwatchDir + "sgt/SGTPointsVector.java",
        coastwatchDir + "sgt/SgtGraph.java",
        coastwatchDir + "sgt/SgtMap.java",
        coastwatchDir + "sgt/SgtUtil.java",
        coastwatchDir + "util/",
        coastwatchDir + "util/SSR.java",
        coastwatchDir + "util/SimpleXMLReader.java",
        coastwatchDir + "util/StringObject.java"
    };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");

    // test recursiveList no directories
    sar = RegexFilenameFilter.recursiveFullNameList(coastwatchDir, "S.+\\.java", false);
    shouldBe = new String[] {
        coastwatchDir + "Screen.java",
        coastwatchDir + "Shared.java",
        coastwatchDir + "griddata/SaveOpendap.java",
        coastwatchDir + "hdf/SdsReader.java",
        coastwatchDir + "hdf/SdsWriter.java",
        coastwatchDir + "pointdata/ScriptRow.java",
        coastwatchDir + "pointdata/StationVariableNc4D.java",
        coastwatchDir + "pointdata/StoredIndex.java",
        coastwatchDir + "sgt/SGTPointsVector.java",
        coastwatchDir + "sgt/SgtGraph.java",
        coastwatchDir + "sgt/SgtMap.java",
        coastwatchDir + "sgt/SgtUtil.java",
        coastwatchDir + "util/SSR.java",
        coastwatchDir + "util/SimpleXMLReader.java",
        coastwatchDir + "util/StringObject.java"
    };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");

    // gatherInfo
    PrimitiveArray info[] = RegexFilenameFilter.gatherInfo(coastwatchDir, "Browser.*");
    int tn = info[1].size();
    StringArray lastMod = new StringArray();
    for (int i = 0; i < tn; i++)
      lastMod.add(Calendar2.safeEpochSecondsToIsoStringTZ(info[2].getLong(i) / 1000.0, "ERROR"));
    Test.ensureEqual(info[0].toString(),
        "griddata, hdf, netcheck, pointdata, sgt, util", "");
    Test.ensureEqual(info[1].toString(),
        "Browser.class, Browser.java, BrowserDefault.properties", "");
    // The below is flaky. Consider using a dedicated test resource directory instead of a code directory
    // and turn these back on.
    // Test.ensureEqual(lastMod.toString(), "2007-04-23T18:24:38Z,
    // 2007-05-02T19:18:32Z", "");
    // Test.ensureEqual(info[3].toString(),
    //     "155937, 359559, 89254", "");
  }

}
