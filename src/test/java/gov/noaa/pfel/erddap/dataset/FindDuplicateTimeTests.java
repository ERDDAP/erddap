package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class FindDuplicateTimeTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testBasic() throws Throwable {
    String2.log("\n*** FindDuplicateTime.test()");
    String testDir = FindDuplicateTimeTests.class.getResource("/data/nc").getPath();
    String results = FindDuplicateTime.findDuplicateTime(testDir, "GL_.*\\.nc", "TIME") + "\n";
    String expected =
        "*** FindDuplicateTime directory="
            + testDir
            + "/ fileNameRegex=GL_.*\\.nc timeVarName=TIME nFilesFound=4\n"
            + "\n"
            + "error #1="
            + testDir
            + "/GL_201111_TS_DB_44761Invalid.nc\n"
            + "    java.io.IOException: java.io.EOFException: Reading "
            + testDir
            + "/GL_201111_TS_DB_44761Invalid.nc at 3720 file length = 3720\n"
            + "\n"
            + "2 files have time=2011-11-01T00:00:00Z\n"
            + testDir
            + "/GL_201111_TS_DB_44761.nc\n"
            + testDir
            + "/GL_201111_TS_DB_44761Copy.nc\n"
            + "\n"
            + "FindDuplicateTime finished successfully.  nFiles=4 nTimesWithDuplicates=1 nErrors=1\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {"-verbose", "findDuplicateTime", testDir, "GL_.*\\.nc", "TIME"},
                false); // doIt loop?
    Test.ensureEqual(results, expected, "Unexpected results from GenerateDatasetsXml.doIt.");
  }
}
