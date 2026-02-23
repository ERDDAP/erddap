package gov.noaa.pfel.erddap;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class DasDdsTests {

  @BeforeAll
  static void init() throws Throwable {
    Initialization.edStatic();
    EDDTestDataset.generateDatasetsXml();
  }

  @org.junit.jupiter.api.Test
  void testDoIt() throws Throwable {
    String2.log("\n*** DasDdsTests.testDoIt()");
    DasDds dasDds = new DasDds();

    // test_chars_e886_d14c_7d71 is a dataset defined in EDDTestDataset.xmlFragment_test_chars
    String datasetID = "test_chars_e886_d14c_7d71";
    String args[] = new String[] {datasetID};

    String result = dasDds.doIt(args, false);

    Test.ensureTrue(result.contains("Attributes {"), "result=" + result);
    Test.ensureTrue(result.contains("row {"), "result=" + result);
    Test.ensureTrue(result.contains("characters {"), "result=" + result);
    Test.ensureTrue(result.contains("String long_name \"Characters\""), "result=" + result);

    // Verify out file
    String outFileName = EDStatic.config.fullLogsDirectory + "DasDds.out";
    Test.ensureTrue(File2.isFile(outFileName), "outFileName=" + outFileName);
    String ra[] = File2.readFromFileUtf8(outFileName);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    Test.ensureTrue(ra[1].contains(datasetID), "ra[1]=" + ra[1]);

    // Verify log file
    String logFileName = EDStatic.config.fullLogsDirectory + "DasDds.log";
    Test.ensureTrue(File2.isFile(logFileName), "logFileName=" + logFileName);
    ra = File2.readFromFileUtf8(logFileName);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    Test.ensureTrue(ra[1].contains("Starting DasDds"), "ra[1]=" + ra[1]);
  }

  @org.junit.jupiter.api.Test
  void testDoItVerbose() throws Throwable {
    String2.log("\n*** DasDdsTests.testDoItVerbose()");
    DasDds dasDds = new DasDds();

    String datasetID = "test_chars_e886_d14c_7d71";
    String args[] = new String[] {"-verbose", datasetID};

    String result = dasDds.doIt(args, false);
    Test.ensureTrue(result.contains("Attributes {"), "result=" + result);

    String logFileName = EDStatic.config.fullLogsDirectory + "DasDds.log";
    String ra[] = File2.readFromFileUtf8(logFileName);
    Test.ensureTrue(ra[1].contains("verbose=true"), "ra[1]=" + ra[1]);
  }

  @org.junit.jupiter.api.Test
  void testDoItLoopWithArgs() throws Throwable {
    String2.log("\n*** DasDdsTests.testDoItLoopWithArgs()");
    DasDds dasDds = new DasDds();

    String datasetID = "test_chars_e886_d14c_7d71";
    String args[] = new String[] {datasetID};

    // loop=true but args.length > 0, so it should NOT loop
    String result = dasDds.doIt(args, true);
    Test.ensureTrue(result.contains("Attributes {"), "result=" + result);
  }

  @org.junit.jupiter.api.Test
  void testDoItNonExistentDataset() throws Throwable {
    String2.log("\n*** DasDdsTests.testDoItNonExistentDataset()");
    DasDds dasDds = new DasDds();

    String datasetID = "non_existent_dataset";
    String args[] = new String[] {datasetID};

    String result = dasDds.doIt(args, false);
    // It returns the contents of the out file, which should be empty if it failed to find the
    // dataset
    // and didn't print anything to it.
    // Actually, printToBoth is only called if successful.
    Test.ensureEqual(result, "", "result=" + result);

    String logFileName = EDStatic.config.fullLogsDirectory + "DasDds.log";
    String ra[] = File2.readFromFileUtf8(logFileName);
    Test.ensureTrue(
        ra[1].contains("An error occurred while trying to load non_existent_dataset"),
        "ra[1]=" + ra[1]);
  }
}
