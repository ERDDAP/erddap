package gov.noaa.pfel.coastwatch.util;

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
          coastwatchDir + "netcheck/",
          coastwatchDir + "pointdata/",
          coastwatchDir + "pointdata/parquet/",
          coastwatchDir + "sgt/",
          coastwatchDir + "sgt/SgtGraph.class",
          coastwatchDir + "sgt/SgtMap.class",
          coastwatchDir + "sgt/SgtUtil$PDFPageSize.class",
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
          coastwatchDir + "sgt/SgtGraph.class",
          coastwatchDir + "sgt/SgtMap.class",
          coastwatchDir + "sgt/SgtUtil$PDFPageSize.class",
          coastwatchDir + "sgt/SgtUtil.class",
          coastwatchDir + "util/SSR$1.class",
          coastwatchDir + "util/SSR.class",
          coastwatchDir + "util/SharedWatchService.class",
          coastwatchDir + "util/SimpleXMLReader.class"
        };
    Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");
  }
}
