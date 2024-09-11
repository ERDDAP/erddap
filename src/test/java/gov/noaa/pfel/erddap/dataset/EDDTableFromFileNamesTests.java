package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagAWS;
import tags.TagLocalERDDAP;
import tags.TagSlowTests;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromFileNamesTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXml()");
    // testVerboseOn();

    String tDir =
        Path.of(EDDTableFromFileNamesTests.class.getResource("/data/fileNames/").toURI())
            .toString();
    tDir = tDir.replace('\\', '/');
    String tRegex = ".*\\.png";
    boolean tRecursive = true;
    String tInfoUrl = "http://mur.jpl.nasa.gov/";
    String tInstitution = "NASA JPL";
    String tSummary = "Images from JPL MUR SST Daily.";
    String tTitle = "JPL MUR SST Images";
    // datasetID changes with different unitTestDataDir
    String tDatasetID =
        EDDTableFromFileNames.suggestDatasetID(tDir + "/" + tRegex + "(EDDTableFromFileNames)");

    String expected =
        "<dataset type=\"EDDTableFromFileNames\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <fileDir>"
            + tDir
            + "/</fileDir>\n"
            + "    <fileNameRegex>.*\\.png</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">null</att>\n"
            + "        <att name=\"creator_name\">null</att>\n"
            + "        <att name=\"creator_url\">null</att>\n"
            + "        <att name=\"history\">null</att>\n"
            + "        <att name=\"infoUrl\">"
            + tInfoUrl
            + "</att>\n"
            + "        <att name=\"institution\">"
            + tInstitution
            + "</att>\n"
            + "        <att name=\"keywords\">data, file, high, identifier, images, jet, jpl, laboratory, lastModified, modified, multi, multi-scale, mur, name, nasa, propulsion, resolution, scale, sea, size, sst, surface, temperature, time, ultra, ultra-high</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"subsetVariables\">fileType</att>\n"
            + "        <att name=\"summary\">"
            + tSummary
            + "</att>\n"
            + "        <att name=\"title\">"
            + tTitle
            + "</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>url</sourceName>\n"
            + "        <destinationName>url</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">URL</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>name</sourceName>\n"
            + "        <destinationName>name</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Name</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lastModified</sourceName>\n"
            + "        <destinationName>lastModified</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Last Modified</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>size</sourceName>\n"
            + "        <destinationName>size</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "            <att name=\"long_name\">Size</att>\n"
            + "            <att name=\"units\">bytes</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>fileType</sourceName>\n"
            + "        <destinationName>fileType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">.*(\\..+?)</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <!-- You can create other variables which are derived from extracts\n"
            + "         from the file names.  Use an extractRegex attribute to specify a\n"
            + "         regular expression with a capturing group (in parentheses). The\n"
            + "         part of the file name which matches the specified capturing group\n"
            + "         (usually group #1) will be extracted to make the new data variable.\n"
            + "         fileType above shows how to extract a String. Below are examples\n"
            + "         showing how to extract a date, and how to extract an integer.\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"units\">yyyyMMddHHmmss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>day</sourceName>\n"
            + "        <destinationName>day</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    -->\n"
            + "</dataset>\n\n\n";
    String results =
        EDDTableFromFileNames.generateDatasetsXml(
                tDir, tRegex, tRecursive, -1, tInfoUrl, tInstitution, tSummary, tTitle, null)
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromFileNames",
                  tDir,
                  tRegex,
                  "" + tRecursive,
                  "-1",
                  tInfoUrl,
                  tInstitution,
                  tSummary,
                  tTitle,
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    // ensure it is ready-to-use by making a dataset from it
    String2.log("results=\n" + results);
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromFileNames.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), tTitle, "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "url, name, lastModified, size, fileType",
        "");
    String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXml() finished successfully.");
  }

  /**
   * testGenerateDatasetsXmlAwsS3 Your S3 credentials must be in <br>
   * ~/.aws/credentials on Linux, OS X, or Unix <br>
   * C:\Users\USERNAME\.aws\credentials on Windows See
   * https://docs.aws.amazon.com/sdk-for-java/?id=docs_gateway#aws-sdk-for-java,-version-1 .
   */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testGenerateDatasetsXmlAwsS3() throws Throwable {
    // String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXmlAwsS3()");
    // testVerboseOn();

    String tDir =
        "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS";
    // tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc
    String tRegex = ".*_CESM1-CAM5_.*\\.nc";
    boolean tRecursive = true;
    String tInfoUrl = "https://nex.nasa.gov/nex/";
    String tInstitution = "NASA Earth Exchange";
    String tSummary = "My great summary";
    String tTitle = "My Great Title";
    String tDatasetID =
        EDDTableFromFileNames.suggestDatasetID(tDir + "/" + tRegex + "(EDDTableFromFileNames)");
    String expected =
        "<dataset type=\"EDDTableFromFileNames\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <fileDir>"
            + tDir
            + "/</fileDir>\n"
            + "    <fileNameRegex>"
            + tRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <reloadEveryNMinutes>120</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">null</att>\n"
            + "        <att name=\"creator_name\">null</att>\n"
            + "        <att name=\"creator_url\">null</att>\n"
            + "        <att name=\"history\">null</att>\n"
            + "        <att name=\"infoUrl\">"
            + tInfoUrl
            + "</att>\n"
            + "        <att name=\"institution\">"
            + tInstitution
            + "</att>\n"
            + "        <att name=\"keywords\">data, earth, exchange, file, great, identifier, lastModified, modified, name, nasa, size, time, title</att>\n"
            + "        <att name=\"sourceUrl\">(remote files)</att>\n"
            + "        <att name=\"subsetVariables\">fileType</att>\n"
            + "        <att name=\"summary\">"
            + tSummary
            + "</att>\n"
            + "        <att name=\"title\">"
            + tTitle
            + "</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>url</sourceName>\n"
            + "        <destinationName>url</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">URL</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>name</sourceName>\n"
            + "        <destinationName>name</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Name</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lastModified</sourceName>\n"
            + "        <destinationName>lastModified</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Last Modified</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>size</sourceName>\n"
            + "        <destinationName>size</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "            <att name=\"long_name\">Size</att>\n"
            + "            <att name=\"units\">bytes</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>fileType</sourceName>\n"
            + "        <destinationName>fileType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">.*(\\..+?)</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <!-- You can create other variables which are derived from extracts\n"
            + "         from the file names.  Use an extractRegex attribute to specify a\n"
            + "         regular expression with a capturing group (in parentheses). The\n"
            + "         part of the file name which matches the specified capturing group\n"
            + "         (usually group #1) will be extracted to make the new data variable.\n"
            + "         fileType above shows how to extract a String. Below are examples\n"
            + "         showing how to extract a date, and how to extract an integer.\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"units\">yyyyMMddHHmmss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>day</sourceName>\n"
            + "        <destinationName>day</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    -->\n"
            + "</dataset>\n\n\n";
    String results =
        EDDTableFromFileNames.generateDatasetsXml(
                tDir, tRegex, tRecursive, -1, tInfoUrl, tInstitution, tSummary, tTitle, null)
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromFileNames",
                  tDir,
                  tRegex,
                  "" + tRecursive,
                  "-1",
                  tInfoUrl,
                  tInstitution,
                  tSummary,
                  tTitle,
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    // ensure it is ready-to-use by making a dataset from it
    String2.log("results=\n" + results);
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromFileNames.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), tTitle, "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "url, name, lastModified, size, fileType",
        "");

    String2.log(
        "\n*** EDDTableFromFileNames.testGenerateDatasetsXmlAwsS3() finished successfully.");
  }

  /** testGenerateDatasetsXmlFromFiles */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testGenerateDatasetsXmlFromFiles() throws Throwable {
    // String2.log("\n***
    // EDDTableFromFileNames.testGenerateDatasetsXmlFromFiles()");
    // testVerboseOn();

    String
        tDir = // ***fromFiles,fromFilesFileType,fromFilesFileDir,fromFilesFileNameRegex,fromFilesRealDir
        "***fromFiles, jsonlCSV, "
                + Path.of(
                        EDDTestDataset.class
                            .getResource("/largePoints/awsS3NoaaGoes17partial/")
                            .toURI())
                    .toString()
                + "/, awsS3NoaaGoes17_....\\.jsonlCSV(|.gz), "
                + "https://noaa-goes17.s3.us-east-1.amazonaws.com/";
    String tRegex = ".*\\.nc"; // for testing. would be .*
    boolean tRecursive = true;
    String tInfoUrl = "https://en.wikipedia.org/wiki/GOES-17";
    String tInstitution = "NOAA";
    String tSummary = "My great summary";
    String tTitle = "My Great Title";
    String tDatasetID =
        EDDTableFromFileNames.suggestDatasetID(tDir + tRegex + "(EDDTableFromFileNames)");
    String results =
        EDDTableFromFileNames.generateDatasetsXml(
                tDir, tRegex, tRecursive, -1, tInfoUrl, tInstitution, tSummary, tTitle, null)
            + "\n";
    String expected =
        "<dataset type=\"EDDTableFromFileNames\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <fileDir>"
            + tDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + tRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <reloadEveryNMinutes>120</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">null</att>\n"
            + "        <att name=\"creator_name\">null</att>\n"
            + "        <att name=\"creator_url\">null</att>\n"
            + "        <att name=\"history\">null</att>\n"
            + "        <att name=\"infoUrl\">"
            + tInfoUrl
            + "</att>\n"
            + "        <att name=\"institution\">"
            + tInstitution
            + "</att>\n"
            + "        <att name=\"keywords\">data, file, great, identifier, lastModified, modified, name, noaa, size, time, title</att>\n"
            + "        <att name=\"sourceUrl\">https://noaa-goes17.s3.us-east-1.amazonaws.com/</att>\n"
            + "        <att name=\"subsetVariables\">fileType</att>\n"
            + "        <att name=\"summary\">"
            + tSummary
            + "</att>\n"
            + "        <att name=\"title\">"
            + tTitle
            + "</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>url</sourceName>\n"
            + "        <destinationName>url</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">URL</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>name</sourceName>\n"
            + "        <destinationName>name</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Name</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lastModified</sourceName>\n"
            + "        <destinationName>lastModified</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Last Modified</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>size</sourceName>\n"
            + "        <destinationName>size</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "            <att name=\"long_name\">Size</att>\n"
            + "            <att name=\"units\">bytes</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>fileType</sourceName>\n"
            + "        <destinationName>fileType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">.*(\\..+?)</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <!-- You can create other variables which are derived from extracts\n"
            + "         from the file names.  Use an extractRegex attribute to specify a\n"
            + "         regular expression with a capturing group (in parentheses). The\n"
            + "         part of the file name which matches the specified capturing group\n"
            + "         (usually group #1) will be extracted to make the new data variable.\n"
            + "         fileType above shows how to extract a String. Below are examples\n"
            + "         showing how to extract a date, and how to extract an integer.\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"units\">yyyyMMddHHmmss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>day</sourceName>\n"
            + "        <destinationName>day</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    -->\n"
            + "</dataset>\n\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromFileNames",
                  tDir,
                  tRegex,
                  "" + tRecursive,
                  "-1",
                  tInfoUrl,
                  tInstitution,
                  tSummary,
                  tTitle,
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    // ensure it is ready-to-use by making a dataset from it
    String2.log("results=\n" + results);
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromFileNames.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), tTitle, "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "url, name, lastModified, size, fileType",
        "");

    String2.log(
        "\n*** EDDTableFromFileNames.testGenerateDatasetsXmlFromFiles() finished successfully.");
  }

  /** Do tests of local file system. */
  @org.junit.jupiter.api.Test
  void testLocal() throws Throwable {
    // String2.log("\n*** EDDTableFromFileNames.testLocal\n");
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    // testVerboseOn();
    int language = 0;
    String dir = EDStatic.fullTestCacheDirectory;
    String results, expected, query, tName;

    EDDTable tedd = (EDDTable) EDDTestDataset.gettestFileNames();

    // .dds
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_all", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        // fvEmptyString wasn't allowed before v2.10
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 five;\n"
            + "    String url;\n"
            + "    String name;\n"
            + "    Float64 time;\n"
            + "    Int32 day;\n"
            + "    Float64 lastModified;\n"
            + "    Float64 size;\n"
            + "    String fileType;\n"
            + "    Float64 fixedTime;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String mySpecialString;\n"
            + "    String fvEmptyString;\n"
            + "    String fromScript;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .das
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_all", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("2\\d{3}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "[TIME]");
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  five {\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Five\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  url {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"URL\";\n"
            + "  }\n"
            + "  name {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Name\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  day {\n"
            + "    Int32 _FillValue 2147483647;\n"
            + "    String ioos_category \"Time\";\n"
            + "  }\n"
            + "  lastModified {\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Last Modified\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  size {\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Size\";\n"
            + "    String units \"bytes\";\n"
            + "  }\n"
            + "  fileType {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Type\";\n"
            + "  }\n"
            + "  fixedTime {\n"
            + "    Float64 actual_range 9.466848e+8, 9.783072e+8;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Fixed Time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 20.0, 40.0;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 0.0, 45.0;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  mySpecialString {\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  fvEmptyString {\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  fromScript {\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Other\";\n"
            + "    Float64 Easternmost_Easting 45.0;\n"
            + "    Float64 geospatial_lat_max 40.0;\n"
            + "    Float64 geospatial_lat_min 20.0;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 45.0;\n"
            + "    Float64 geospatial_lon_min 0.0;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"[TIME]Z (local files)\n"
            + "[TIME]Z http://localhost:8080/erddap/tabledap/testFileNames.das\";\n"
            + "    String infoUrl \"https://www.pfeg.noaa.gov/\";\n"
            + "    String institution \"NASA JPL\";\n"
            + "    String keywords \"file, images, jpl, modified, mur, name, nasa, size, sst, time, URL\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 40.0;\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 20.0;\n"
            + "    String subsetVariables \"fileType\";\n"
            + "    String summary \"Images from JPL MUR SST Daily.\";\n"
            + "    String title \"JPL MUR SST Images\";\n"
            + "    Float64 Westernmost_Easting 0.0;\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get all as .csv
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "five,url,name,time,day,size,fileType,fixedTime,latitude,longitude,mySpecialString,fvEmptyString",
            dir,
            tedd.className() + "_all",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "five,url,name,time,day,size,fileType,fixedTime,latitude,longitude,mySpecialString,fvEmptyString\n"
            + "m,,,UTC,,bytes,,UTC,degrees_north,degrees_east,,\n"
            + "5.0,http://localhost:8080/erddap/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,2015-01-03T09:00:00Z,3,46482.0,.png,,NaN,NaN,\"My \"\"Special\"\" String\",\n"
            + "5.0,http://localhost:8080/erddap/files/testFileNames/jplMURSST20150104090000.png,jplMURSST20150104090000.png,2015-01-04T09:00:00Z,4,46586.0,.png,,NaN,NaN,\"My \"\"Special\"\" String\",\n"
            + "5.0,http://localhost:8080/erddap/files/testFileNames/sub/jplMURSST20150105090000.png,jplMURSST20150105090000.png,2015-01-05T09:00:00Z,5,46549.0,.png,,NaN,NaN,\"My \"\"Special\"\" String\",\n";
    Test.ensureEqual(results, expected, "results=\n" + String2.annotatedString(results));

    // test that min and max are being set by the constructor
    EDV edv = tedd.findVariableByDestinationName("time");
    Test.ensureEqual(edv.destinationMinString(), "2015-01-03T09:00:00Z", "min");
    Test.ensureEqual(edv.destinationMaxString(), "2015-01-05T09:00:00Z", "max");

    edv = tedd.findVariableByDestinationName("day");
    Test.ensureEqual(edv.destinationMinDouble(), 3, "min");
    Test.ensureEqual(edv.destinationMaxDouble(), 5, "max");

    // Don't check lastModified, it has hard to control behavior - especially across computers.
    // edv = tedd.findVariableByDestinationName("lastModified");
    // Test.ensureEqual(edv.destinationMinString(), "2015-01-07T21:21:44Z", "min"); // 2018-08-09
    // these changed by 1 hr
    //                                                                              // with switch
    // to lenovo
    // Test.ensureEqual(edv.destinationMaxString(), "2015-01-14T21:54:04Z", "max");

    edv = tedd.findVariableByDestinationName("size");
    Test.ensureEqual(edv.destinationMinDouble(), 46482, "min");
    Test.ensureEqual(edv.destinationMaxDouble(), 46586, "max");

    /*
     * actual_range and =NaN fixedValue variables:
     * Technically, if a variable has a fixedValue, then the actual_range should be
     * determined
     * from that fixedValue. However, it is sometimes useful (notably with
     * EDDTableFromFileNames)
     * to have dummy variable(s) (e.g., latitude, longitude, time) with fixed values
     * of NaN,
     * but a valid actual_range (as set by the attribute).
     * Then, in Advanced Search a user can search for datasets
     * which have data in a specific latitude, longitude, time range and this
     * dataset
     * will be able to say it does have the data (although all the actual rows of
     * data
     * will show NaN).
     */
    edv = tedd.findVariableByDestinationName("fixedTime");
    // Test.ensureEqual(edv.destinationMinDouble(), 946684800, "min");
    // Test.ensureEqual(edv.destinationMaxDouble(), 978307200, "max");

    edv = tedd.findVariableByDestinationName("latitude");
    Test.ensureEqual(edv.destinationMinDouble(), 20, "min");
    Test.ensureEqual(edv.destinationMaxDouble(), 40, "max");

    edv = tedd.findVariableByDestinationName("longitude");
    Test.ensureEqual(edv.destinationMinDouble(), 0, "min");
    Test.ensureEqual(edv.destinationMaxDouble(), 45, "max");

    // a constraint on an extracted variable, and fewer results variables
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "name,day,size&day=4", dir, tedd.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "name,day,size\n" + ",,bytes\n" + "jplMURSST20150104090000.png,4,46586.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // just request fixed values
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "five,fixedTime,latitude,longitude,mySpecialString,fvEmptyString",
            dir,
            tedd.className() + "_fixed",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log("dir+tName=" + dir + tName);
    expected =
        "five,fixedTime,latitude,longitude,mySpecialString,fvEmptyString\n"
            + "m,UTC,degrees_north,degrees_east,,\n"
            + "5.0,,NaN,NaN,\"My \"\"Special\"\" String\",\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n EDDTableFromFileNames.testLocal finished successfully");
    // debugMode = oDebugMode;
  }

  /**
   * Do tests of an Amazon AWS S3 file system. Your S3 credentials must be in <br>
   * ~/.aws/credentials on Linux, OS X, or Unix <br>
   * C:\Users\USERNAME\.aws\credentials on Windows See
   * https://docs.aws.amazon.com/sdk-for-java/?id=docs_gateway#aws-sdk-for-java,-version-1 .
   */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testAwsS3() throws Throwable {
    // String2.log("\n*** EDDTableFromFileNames.testAwsS3\n");
    // testVerboseOn();
    String dir = EDStatic.fullTestCacheDirectory;
    String results, expected, query, tName;
    int language = 0;

    EDDTable tedd = (EDDTable) EDDTestDataset.gettestFileNamesAwsS3();

    // .dds
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_all", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 five;\n"
            + "    String url;\n"
            + "    String name;\n"
            + "    Float64 startMonth;\n"
            + "    Float64 endMonth;\n"
            + "    Float64 lastModified;\n"
            + "    Float64 size;\n"
            + "    String fileType;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .das
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_all", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("2\\d{3}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "[TIME]");
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  five {\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Five\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  url {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"URL\";\n"
            + "  }\n"
            + "  name {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Name\";\n"
            + "  }\n"
            + "  startMonth {\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Start Month\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  endMonth {\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"End Month\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  lastModified {\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Last Modified\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  size {\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Size\";\n"
            + "    String units \"bytes\";\n"
            + "  }\n"
            + "  fileType {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Type\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Other\";\n"
            + "    String creator_name \"NASA Earth Exchange\";\n"
            + "    String creator_url \"https://nex.nasa.gov/nex/\";\n"
            + "    String history \"[TIME]Z (remote files)\n"
            + "[TIME]Z http://localhost:8080/erddap/tabledap/testFileNamesAwsS3.das\";\n"
            + "    String infoUrl \"https://nex.nasa.gov/nex/\";\n"
            + "    String institution \"NASA Earth Exchange\";\n"
            + "    String keywords \"data, earth, exchange, file, great, identifier, lastModified, modified, name, nasa, size, time, title\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String sourceUrl \"(remote files)\";\n"
            + "    String subsetVariables \"fileType\";\n"
            + "    String summary \"File Names from https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\";\n"
            + "    String title \"File Names from Amazon AWS S3 NASA NEX tasmin Files\";\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get all as .csv
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "five,url,name,startMonth,endMonth,lastModified,size,fileType\n"
            + "m,,,UTC,UTC,UTC,bytes,\n"
            + "5.0,http://localhost:8080/erddap/files/testFileNamesAwsS3/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc,2006-01-01T00:00:00Z,2010-12-01T00:00:00Z,2013-10-25T20:46:53Z,1.372730447E9,.nc\n"
            + "5.0,http://localhost:8080/erddap/files/testFileNamesAwsS3/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201101-201512.nc,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201101-201512.nc,2011-01-01T00:00:00Z,2015-12-01T00:00:00Z,2013-10-25T20:47:18Z,1.373728987E9,.nc\n"
            + "5.0,http://localhost:8080/erddap/files/testFileNamesAwsS3/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201601-202012.nc,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201601-202012.nc,2016-01-01T00:00:00Z,2020-12-01T00:00:00Z,2013-10-25T20:51:23Z,1.373747344E9,.nc\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // test that min and max are being set by the constructor
    EDV edv = tedd.findVariableByDestinationName("startMonth");
    Test.ensureEqual(edv.destinationMinString(), "2006-01-01T00:00:00Z", "min");
    Test.ensureEqual(edv.destinationMaxString(), "2096-01-01T00:00:00Z", "max");

    edv = tedd.findVariableByDestinationName("endMonth");
    Test.ensureEqual(edv.destinationMinString(), "2010-12-01T00:00:00Z", "min");
    Test.ensureEqual(edv.destinationMaxString(), "2099-12-01T00:00:00Z", "max");

    edv = tedd.findVariableByDestinationName("lastModified");
    Test.ensureEqual(edv.destinationMinString(), "2013-10-25T20:45:24Z", "min");
    Test.ensureEqual(edv.destinationMaxString(), "2013-10-25T20:54:20Z", "max");

    edv = tedd.findVariableByDestinationName("size");
    Test.ensureEqual("" + edv.destinationMinDouble(), "1.098815646E9", "min"); // exact test
    Test.ensureEqual("" + edv.destinationMaxDouble(), "1.373941204E9", "max");

    // a constraint on an extracted variable, and fewer results variables
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "name,startMonth,size&size=1098815646",
            dir,
            tedd.className() + "_subset",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "name,startMonth,size\n"
            + ",UTC,bytes\n"
            + "tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_209601-209912.nc,2096-01-01T00:00:00Z,1.098815646E9\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n EDDTableFromFileNames.testAwsS3 finished successfully");
  }

  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testgoes17all() throws Throwable {
    testAccessibleViaFilesFileTable(false, true);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagSlowTests
  void testgoes17partial(boolean deleteCachedInfo) throws Throwable {
    testAccessibleViaFilesFileTable(deleteCachedInfo, false);
  }

  /** Test accessibleViaFilesFileTable fromFiles */
  private static void testAccessibleViaFilesFileTable(boolean deleteCachedInfo, boolean bigTest)
      throws Throwable {

    // String2.log("\n***
    // EDDTableFromFileNames.testAccessibleViaFilesFileTable(deleteCachedInfo=" +
    // deleteCachedInfo + ")");
    int language = 0;
    String id = "awsS3NoaaGoes17" + (bigTest ? "all" : "partial");
    if (deleteCachedInfo) {
      String2.log("This test will be slow.");
      EDD.deleteCachedDatasetInfo(id);
      File2.simpleDelete(EDD.datasetDir(id)); // delete the dir. (complete test)
      EDD.deleteCachedDatasetInfo(id + "_child");
    }
    long time = System.currentTimeMillis();
    EDDTableFromFileNames edd =
        (EDDTableFromFileNames)
            (bigTest
                ? EDDTestDataset.getawsS3NoaaGoes17all()
                : EDDTestDataset.getawsS3NoaaGoes17partial());
    time = (System.currentTimeMillis() - time) / 1000; // s
    // TODO handle time based performance tests better
    // long expTime = (bigTest ? 20 : 1) * (deleteCachedInfo ? 20 : 14);
    // String2.log("loadDataset time=" + time + "s (expected=" + expTime + "s)");
    // Test.ensureTrue(time < expTime * 1.5, "too slow. time=" + time + "s > " + expTime + "s");
    Object o2[];
    Table fileTable;
    StringArray subDirs;
    int fileTableNRows;
    String results, expected;

    if (true) {
      // root dir
      time = System.currentTimeMillis();
      o2 = edd.accessibleViaFilesFileTable(language, ""); // what's in root dir?
      time = System.currentTimeMillis() - time;
      String2.log("root time=" + time + "ms");
      fileTable = (Table) o2[0];
      subDirs = new StringArray((String[]) o2[1]);
      fileTableNRows = fileTable.nRows();
      Test.ensureTrue(fileTableNRows + subDirs.size() > 0, "No results!");
      results = fileTable.dataToString(5);
      expected = "Name,Last modified,Size,Description\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      results = subDirs.toString();
      expected =
          bigTest
              ? "ABI-L1b-RadC, ABI-L1b-RadF, ABI-L1b-RadM, ABI-L2-CMIPC, ABI-L2-CMIPF, ABI-L2-CMIPM, ABI-L2-FDCC, ABI-L2-FDCF, ABI-L2-MCMIPC, ABI-L2-MCMIPF, ABI-L2-MCMIPM, GLM-L2-LCFA"
              : "ABI-L1b-RadC, ABI-L1b-RadF";
      Test.ensureEqual(results, expected, "");
      // TODO handle time based performance tests better
      //   expTime = 100; // ms
      //   String msg = "get root dir time=" + time + "ms (expected=" + expTime + "ms)";
      //   String2.log(msg);
      //   Test.ensureTrue(time < expTime * 1.5, msg);
    }

    if (true) {
      // subdir was 6s
      time = System.currentTimeMillis();
      o2 = edd.accessibleViaFilesFileTable(language, "ABI-L1b-RadC/");
      time = System.currentTimeMillis() - time;
      String2.log("ABI-L1b-RadC/ subdir time=" + time + "ms");
      fileTable = (Table) o2[0];
      subDirs = new StringArray((String[]) o2[1]);
      fileTableNRows = fileTable.nRows();
      Test.ensureTrue(fileTableNRows + subDirs.size() > 0, "No results!");
      results = fileTable.dataToString(5);
      expected = "Name,Last modified,Size,Description\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      results = subDirs.toString();
      expected = "2018, 2019";
      Test.ensureEqual(results, expected, "");
      // TODO handle time based performance tests better
      //   expTime = 100; // ms
      //   String2.log("get ABI-L1b-RadC/ dir time=" + time + "ms (expected=" + expTime + "ms)");
      //   Test.ensureTrue(time < expTime * 1.5, "");
    }

    if (true) {
      // subdir 712ms
      time = System.currentTimeMillis();
      o2 = edd.accessibleViaFilesFileTable(language, "ABI-L1b-RadC/2018/360/10/");
      time = System.currentTimeMillis() - time;
      String2.log("ABI-L1b-RadC/2018/360/10/ dir with files time=" + time + "ms");
      fileTable = (Table) o2[0];
      subDirs = new StringArray((String[]) o2[1]);
      fileTableNRows = fileTable.nRows();
      Test.ensureTrue(fileTableNRows + subDirs.size() > 0, "No results!");
      results = fileTable.dataToString(5);
      expected =
          "Name,Last modified,Size,Description\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601002189_e20183601004562_c20183601004596.nc,1545818719000,456238,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601007189_e20183601009562_c20183601009596.nc,1545819029000,544207,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601012189_e20183601014502_c20183601014536.nc,1545819316000,485764,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601017189_e20183601019562_c20183601019596.nc,1545819621000,489321,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601022189_e20183601024562_c20183601024597.nc,1545819917000,539104,\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      results = subDirs.toString();
      expected = "";
      Test.ensureEqual(results, expected, "");
      // TODO handle time based performance tests better
      //   expTime = 1300; // ms
      //   String2.log("get ABI-L1b-RadC/2018/360/10/ dir time=" + time + "ms (expected=" + expTime
      // + "ms)");
      //   Test.ensureTrue(time < expTime * 1.5, "TOO SLOW!!! time=" + time + "ms (exp=1300ms)");
    }
  }

  /** testGenerateDatasetsXmlFromOnTheFly */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testGenerateDatasetsXmlFromOnTheFly() throws Throwable {
    // String2.log("\n***
    // EDDTableFromFileNames.testGenerateDatasetsXmlFromOnTheFly()");
    // testVerboseOn();
    String tDir = // ***fromOnTheFly,urlDir
        "***fromOnTheFly, https://noaa-goes17.s3.us-east-1.amazonaws.com/";
    String tRegex = ".*\\.nc"; // for testing. would be .*
    boolean tRecursive = true;
    String tInfoUrl = "https://en.wikipedia.org/wiki/GOES-17";
    String tInstitution = "NOAA";
    String tSummary = ""; // test the auto-generated summary
    String tTitle = ""; // test the auto-generated title
    String tDatasetID =
        EDDTableFromFileNames.suggestDatasetID(tDir + "/" + tRegex + "(EDDTableFromFileNames)");
    String results =
        EDDTableFromFileNames.generateDatasetsXml(
                tDir, tRegex, tRecursive, -1, tInfoUrl, tInstitution, tSummary, tTitle, null)
            + "\n";
    String expected =
        "<dataset type=\"EDDTableFromFileNames\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <fileDir>"
            + tDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + tRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">null</att>\n"
            + "        <att name=\"creator_name\">null</att>\n"
            + "        <att name=\"creator_url\">null</att>\n"
            + "        <att name=\"history\">null</att>\n"
            + "        <att name=\"infoUrl\">"
            + tInfoUrl
            + "</att>\n"
            + "        <att name=\"institution\">"
            + tInstitution
            + "</att>\n"
            + "        <att name=\"keywords\">aws, bucket, data, file, goes17, identifier, lastModified, modified, name, names, noaa, noaa-goes17, size, time</att>\n"
            + "        <att name=\"sourceUrl\">https://noaa-goes17.s3.us-east-1.amazonaws.com/</att>\n"
            + "        <att name=\"summary\">This dataset has file information from the AWS S3 noaa-goes17 bucket at "
            + "https://noaa-goes17.s3.us-east-1.amazonaws.com/ . "
            + "Use ERDDAP&#39;s &quot;files&quot; system for this dataset to browse and download the files. "
            + "The &quot;files&quot; information for this dataset is always perfectly up-to-date because ERDDAP gets it on-the-fly. "
            + "AWS S3 doesn&#39;t offer a simple way to browser the files in buckets. "
            + "This dataset is a solution to that problem for this bucket.</att>\n"
            + "        <att name=\"title\">File Names from the AWS S3 noaa-goes17 Bucket</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>url</sourceName>\n"
            + "        <destinationName>url</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">URL</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>name</sourceName>\n"
            + "        <destinationName>name</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Name</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lastModified</sourceName>\n"
            + "        <destinationName>lastModified</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Last Modified</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>size</sourceName>\n"
            + "        <destinationName>size</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"ioos_category\">Other</att>\n"
            + "            <att name=\"long_name\">Size</att>\n"
            + "            <att name=\"units\">bytes</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>fileType</sourceName>\n"
            + "        <destinationName>fileType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">.*(\\..+?)</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <!-- You can create other variables which are derived from extracts\n"
            + "         from the file names.  Use an extractRegex attribute to specify a\n"
            + "         regular expression with a capturing group (in parentheses). The\n"
            + "         part of the file name which matches the specified capturing group\n"
            + "         (usually group #1) will be extracted to make the new data variable.\n"
            + "         fileType above shows how to extract a String. Below are examples\n"
            + "         showing how to extract a date, and how to extract an integer.\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"units\">yyyyMMddHHmmss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>day</sourceName>\n"
            + "        <destinationName>day</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n"
            + "            <att name=\"extractGroup\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    -->\n"
            + "</dataset>\n\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromFileNames",
                  tDir,
                  tRegex,
                  "" + tRecursive,
                  "-1",
                  tInfoUrl,
                  tInstitution,
                  tSummary,
                  tTitle,
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    // ensure it is ready-to-use by making a dataset from it
    String2.log("results=\n" + results);
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromFileNames.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "File Names from the AWS S3 noaa-goes17 Bucket", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "url, name, lastModified, size, fileType",
        "");

    String2.log(
        "\n*** EDDTableFromFileNames.testGenerateDatasetsXmlFromFiles() finished successfully.");
  }

  /** Test an AWS S3 dataset in localhost ERDDAP. */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testAwsS3local() throws Throwable {
    // String2.log("\n*** EDDTableFromFileNames.testAwsS3b");
    String results, expected;
    String url = "http://localhost:8080/cwexperimental/files/awsS3Files_1000genomes/";

    // base url dir
    results = SSR.getUrlResponseStringNewline(url + ".csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "1000G_2504_high_coverage/,NaN,NaN,\n"
            + "alignment_indices/,NaN,NaN,\n"
            + "changelog_details/,NaN,NaN,\n"
            + "complete_genomics_indices/,NaN,NaN,\n"
            + "data/,NaN,NaN,\n"
            + "hgsv_sv_discovery/,NaN,NaN,\n"
            + "phase1/,NaN,NaN,\n"
            + "phase3/,NaN,NaN,\n"
            + "pilot_data/,NaN,NaN,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // file in base url dir
    results = SSR.getUrlResponseStringNewline(url + "20131219.superpopulations.tsv");
    expected =
        "Description\tPopulation Code\n"
            + "East Asian\tEAS\n"
            + "South Asian\tSAS\n"
            + "African\tAFR\n"
            + "European\tEUR\n"
            + "American\tAMR\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subdir
    results = SSR.getUrlResponseStringNewline(url + "changelog_details/.csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "changelog_detail_20100621_new_bams,1409641614000,108000,\n"
            + "changelog_details_20081217,1337355796000,882798,\n"
            + "changelog_details_20081219,1337355796000,160,\n"
            + "changelog_details_20081222,1337355796000,176513,\n"
            + "changelog_details_20090105,1337355796000,855,\n"
            + "changelog_details_20090108,1337355796000,80901,\n"
            + "changelog_details_20090127,1337355795000,11357,\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // file in subdir
    results = SSR.getUrlResponseStringNewline(url + "changelog_details/changelog_details_20081219");
    expected =
        "OLD\tNEW\n"
            + "simulations\ttechnical/simulations\n"
            + "technical/SOLiD\ttechnical/method_development/SOLiD\n"
            + "technical/recalibration\ttechnical/method_development/recalibration\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** Test a ***fromOnTheFile dataset */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testOnTheFly() throws Throwable {
    // String2.log("\n*** EDDTableFromFileNames.testAccessibleViaFilesFileTable");
    int language = 0;
    String id = "awsS3NoaaGoes17";
    long time = System.currentTimeMillis();
    EDDTableFromFileNames edd = (EDDTableFromFileNames) EDDTestDataset.getawsS3NoaaGoes17();
    time = System.currentTimeMillis() - time;
    long expTime = 4000;
    String msg = "loadDataset time=" + time + "ms (expected=" + expTime + "ms)";
    String2.log(msg);
    Test.ensureTrue(time < expTime * 2, "Too slow: " + msg);
    Object o2[];
    String dir = EDStatic.fullTestCacheDirectory;
    Table fileTable;
    StringArray subDirs;
    int fileTableNRows;
    String results, expected, tName;

    // sleep before these timing tests
    Math2.gc("EDDTableFromFileNames (between tests)", 5000);
    Math2.gc("EDDTableFromFileNames (between tests)", 5000);

    if (true) {
      // root dir
      time = System.currentTimeMillis();
      o2 = edd.accessibleViaFilesFileTable(language, ""); // what's in root dir?
      time = System.currentTimeMillis() - time;
      String2.log("root time=" + time + "ms");
      fileTable = (Table) o2[0];
      subDirs = new StringArray((String[]) o2[1]);
      fileTableNRows = fileTable.nRows();
      Test.ensureTrue(fileTableNRows + subDirs.size() > 0, "No results!");
      results = fileTable.dataToString(5);
      expected =
          "Name,Last modified,Size,Description\n"
              + "index.html,1632772095000,32357,\n"; // last modified is millis (stored as long),
      // changes sometimes
      Test.ensureEqual(
          results,
          expected,
          "results=\n"
              + results
              + "\nlastModified and size change sometimes. If so, change the test.");
      results = subDirs.toString();
      expected =
          "ABI-L1b-RadC, ABI-L1b-RadF, ABI-L1b-RadM, ABI-L2-ACHAC, ABI-L2-ACHAF, ABI-L2-ACHAM, ABI-L2-ACHTF, ABI-L2-ACHTM, ABI-L2-ACMC, ABI-L2-ACMF, ABI-L2-ACMM, ABI-L2-ACTPC, ABI-L2-ACTPF, ABI-L2-ACTPM, ABI-L2-ADPC, ABI-L2-ADPF, ABI-L2-ADPM, ABI-L2-AICEF, ABI-L2-AITAF, ABI-L2-AODC, ABI-L2-AODF, ABI-L2-BRFC, ABI-L2-BRFF, ABI-L2-BRFM, ABI-L2-CMIPC, ABI-L2-CMIPF, ABI-L2-CMIPM, ABI-L2-CODC, ABI-L2-CODF, ABI-L2-CPSC, ABI-L2-CPSF, ABI-L2-CPSM, ABI-L2-CTPC, ABI-L2-CTPF, ABI-L2-DMWC, ABI-L2-DMWF, ABI-L2-DMWM, ABI-L2-DMWVC, ABI-L2-DMWVF, ABI-L2-DMWVM, ABI-L2-DSIC, ABI-L2-DSIF, ABI-L2-DSIM, ABI-L2-DSRC, ABI-L2-DSRF, ABI-L2-DSRM, ABI-L2-FDCC, ABI-L2-FDCF, ABI-L2-FDCM, ABI-L2-LSAC, ABI-L2-LSAF, ABI-L2-LSAM, ABI-L2-LST2KMF, ABI-L2-LSTC, ABI-L2-LSTF, ABI-L2-LSTM, ABI-L2-LVMPC, ABI-L2-LVMPF, ABI-L2-LVMPM, ABI-L2-LVTPC, ABI-L2-LVTPF, ABI-L2-LVTPM, ABI-L2-MCMIPC, ABI-L2-MCMIPF, ABI-L2-MCMIPM, ABI-L2-RRQPEF, ABI-L2-RSRC, ABI-L2-RSRF, ABI-L2-SSTF, ABI-L2-TPWC, ABI-L2-TPWF, ABI-L2-TPWM, ABI-L2-VAAF, EXIS-L1b-SFEU, EXIS-L1b-SFXR, GLM-L2-LCFA, MAG-L1b-GEOF, SEIS-L1b-EHIS, SEIS-L1b-MPSH, SEIS-L1b-MPSL, SEIS-L1b-SGPS, SUVI-L1b-Fe093, SUVI-L1b-Fe131, SUVI-L1b-Fe171, SUVI-L1b-Fe195, SUVI-L1b-Fe284, SUVI-L1b-He303"; // changes
      // sometimes
      Test.ensureEqual(results, expected, "");
      expTime = 459; // ms
      Test.ensureTrue(
          time < expTime * 2,
          "Too slow! (common if computer is busy).\n"
              + "get root dir time="
              + time
              + "ms (expected="
              + expTime
              + "ms)");
    }

    if (true) {
      // subdir
      time = System.currentTimeMillis();
      o2 = edd.accessibleViaFilesFileTable(language, "ABI-L1b-RadC/");
      time = System.currentTimeMillis() - time;
      String2.log("ABI-L1b-RadC/ subdir time=" + time + "ms");
      fileTable = (Table) o2[0];
      subDirs = new StringArray((String[]) o2[1]);
      fileTableNRows = fileTable.nRows();
      Test.ensureTrue(fileTableNRows + subDirs.size() > 0, "No results!");
      results = fileTable.dataToString(5);
      expected = "Name,Last modified,Size,Description\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      results = subDirs.toString();
      expected = "2018, 2019, 2020, 2021, 2022, 2023";
      Test.ensureEqual(results, expected, "");
      expTime = 549; // ms
      msg = "get ABI-L1b-RadC/ dir time=" + time + "ms (expected=" + expTime + "ms)";
      String2.log(msg);
      Test.ensureTrue(time < expTime * 2, "too slow: " + msg);
    }

    if (true) {
      // subdir
      time = System.currentTimeMillis();
      o2 = edd.accessibleViaFilesFileTable(language, "ABI-L1b-RadC/2018/360/10/");
      time = System.currentTimeMillis() - time;
      String2.log("ABI-L1b-RadC/2018/360/10/ dir with files time=" + time + "ms");
      fileTable = (Table) o2[0];
      subDirs = new StringArray((String[]) o2[1]);
      fileTableNRows = fileTable.nRows();
      Test.ensureTrue(fileTableNRows + subDirs.size() > 0, "No results!");
      results = fileTable.dataToString(5);
      expected =
          "Name,Last modified,Size,Description\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601002189_e20183601004562_c20183601004596.nc,1545818719000,456238,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601007189_e20183601009562_c20183601009596.nc,1545819029000,544207,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601012189_e20183601014502_c20183601014536.nc,1545819316000,485764,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601017189_e20183601019562_c20183601019596.nc,1545819621000,489321,\n"
              + "OR_ABI-L1b-RadC-M3C01_G17_s20183601022189_e20183601024562_c20183601024597.nc,1545819917000,539104,\n"
              + "...\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
      results = subDirs.toString();
      expected = "";
      Test.ensureEqual(results, expected, "");
      expTime = 693; // ms
      msg = "get ABI-L1b-RadC/2018/360/10/ dir time=" + time + "ms (expected=" + expTime + "ms)";
      String2.log(msg);
      Test.ensureTrue(time < expTime * 2, "too slow: " + msg);
    }

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "", // entire dataset
            dir,
            edd.className() + "_all",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "url,name,lastModified,size,fileType\n"
            + ",,UTC,bytes,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/index.html,index.html,2021-09-27T19:48:15Z,32357.0,.html\n"
            + // changes sometimes
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L1b-RadC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L1b-RadF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L1b-RadM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACHAC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACHAF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACHAM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACHTF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACHTM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACMC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACMF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACMM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACTPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACTPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ACTPM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ADPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ADPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-ADPM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-AICEF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-AITAF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-AODC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-AODF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-BRFC/,,,NaN,\n"
            + // appeared 2021-08-31
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-BRFF/,,,NaN,\n"
            + // appeared 2021-08-31
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-BRFM/,,,NaN,\n"
            + // appeared 2021-08-31
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CMIPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CMIPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CMIPM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CODC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CODF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CPSC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CPSF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CPSM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CTPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-CTPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DMWC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DMWF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DMWM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DMWVC/,,,NaN,\n"
            + // disappeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DMWVF/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DMWVM/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DSIC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DSIF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DSIM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DSRC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DSRF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-DSRM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-FDCC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-FDCF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-FDCM/,,,NaN,\n"
            + // appeared 2021-06-24
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LSAC/,,,NaN,\n"
            + // appeared 2021-08-31
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LSAF/,,,NaN,\n"
            + // appeared 2021-08-31
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LSAM/,,,NaN,\n"
            + // appeared 2021-08-31
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LST2KMF/,,,NaN,\n"
            + // appeared 2021-11-16
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LSTC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LSTF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LSTM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LVMPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LVMPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LVMPM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LVTPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LVTPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-LVTPM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-MCMIPC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-MCMIPF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-MCMIPM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-RRQPEF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-RSRC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-RSRF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-SSTF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-TPWC/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-TPWF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-TPWM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L2-VAAF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/EXIS-L1b-SFEU/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/EXIS-L1b-SFXR/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/GLM-L2-LCFA/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/MAG-L1b-GEOF/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-EHIS/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-MPSH/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-MPSL/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-SGPS/,,,NaN,\n"
            + // appeared 2021-05-03
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe093/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe131/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe171/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe195/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe284/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-He303/,,,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "&url=~\".*-L1b-.*\"", dir, edd.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "url,name,lastModified,size,fileType\n"
            + ",,UTC,bytes,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L1b-RadC/,,,NaN,\n"
            + // 2021-05-03 many added below...
            "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L1b-RadF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/ABI-L1b-RadM/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/EXIS-L1b-SFEU/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/EXIS-L1b-SFXR/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/MAG-L1b-GEOF/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-EHIS/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-MPSH/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-MPSL/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SEIS-L1b-SGPS/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe093/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe131/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe171/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe195/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-Fe284/,,,NaN,\n"
            + "http://localhost:8080/erddap/files/awsS3NoaaGoes17/SUVI-L1b-He303/,,,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
