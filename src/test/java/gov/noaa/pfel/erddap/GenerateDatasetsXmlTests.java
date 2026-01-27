package gov.noaa.pfel.erddap;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

import java.nio.file.Path;

class GenerateDatasetsXmlTests {

    @BeforeAll
    static void init() throws Throwable {
        Initialization.edStatic();
    }

    @org.junit.jupiter.api.Test
    void testDoItEDDTableFromAsciiFiles() throws Throwable {
        String2.log("\n*** GenerateDatasetsXmlTests.testDoItEDDTableFromAsciiFiles()");
        GenerateDatasetsXml generateDatasetsXml = new GenerateDatasetsXml();

        String sampleFile = Path.of(EDDTestDataset.class.getResource("/datasets/test_chars.csv").toURI()).toString();
        String sampleDir = File2.getDirectory(sampleFile);

        // args for EDDTableFromAsciiFiles
        String args[] = new String[]{
            "EDDTableFromAsciiFiles",
            sampleDir,
            "test_chars\\.csv",
            sampleFile,
            "UTF-8",
            "1", // columnNamesRow
            "2", // firstDataRow
            ";", // columnSeparator
            "1440", // reloadEveryNMinutes
            "", // preExtractRegex
            "", // postExtractRegex
            "", // extractRegex
            "", // columnNameForExtract
            "", // sortedColumnSourceName
            "", // sortFilesBySourceNames
            "", // infoUrl
            "", // institution
            "", // summary
            "", // title
            "-1", // standardizeWhat
            ""  // cacheFromUrl
        };

        String result = generateDatasetsXml.doIt(args, false);

        Test.ensureTrue(result.contains("<dataset type=\"EDDTableFromAsciiFiles\""), "result=" + result);
        Test.ensureTrue(result.contains("<sourceName>row</sourceName>"), "result=" + result);
        Test.ensureTrue(result.contains("<sourceName>characters</sourceName>"), "result=" + result);

        // Verify out file
        String outFileName = EDStatic.config.fullLogsDirectory + "GenerateDatasetsXml.out";
        Test.ensureTrue(File2.isFile(outFileName), "outFileName=" + outFileName);

        // Verify log file
        String logFileName = EDStatic.config.fullLogsDirectory + "GenerateDatasetsXml.log";
        Test.ensureTrue(File2.isFile(logFileName), "logFileName=" + logFileName);
    }

    @org.junit.jupiter.api.Test
    void testDoItInvalidType() throws Throwable {
        String2.log("\n*** GenerateDatasetsXmlTests.testDoItInvalidType()");
        GenerateDatasetsXml generateDatasetsXml = new GenerateDatasetsXml();

        String args[] = new String[]{"InvalidType"};
        String result = generateDatasetsXml.doIt(args, false);

        // Result content comes from the file, which should be empty if no successful generation happened
        // But the log should contain the error
        String logFileName = EDStatic.config.fullLogsDirectory + "GenerateDatasetsXml.log";
        String ra[] = File2.readFromFileUtf8(logFileName);
        Test.ensureTrue(ra[1].contains("ERROR: eddType=InvalidType is not an option."), "ra[1]=" + ra[1]);
    }
}
