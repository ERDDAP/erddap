package gov.noaa.pfel.erddap;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagMissingDataset;
import testDataset.Initialization;

class ArchiveADatasetTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset //  All of these require the datasets to be defined in the loaded datasets.xml
  void testOriginalNcCF() throws Throwable {
    String2.log("*** ArchiveADataset.testOriginalNcCF()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "original",
                  "tar.gz",
                  "erd.data@noaa.gov",
                  "cwwcNDBCMet",
                  "default", // all data vars
                  "&station=~\"3.*\"", // &station=~"3.*"
                  "nothing", // should be station, but use "nothing" to test save as points
                  "default", // .ncCFMA
                  "MD5"
                });
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String ra[] = File2.readFromFileUtf8(targzName + ".listOfFiles.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    String results = ra[1];
    String expected =
        "cwwcNDBCMet.das                                                  "
            + today
            + "T.{8}Z         1....\n"
            + "cwwcNDBCMet.dds                                                  "
            + today
            + "T.{8}Z           3..\n"
            + "READ_ME.txt                                                      "
            + today
            + "T.{8}Z           3..\n"
            + "data/\n"
            + "  cwwcNDBCMet.nc                                                 "
            + today
            + "T.{8}Z      1.......\n"
            + "  cwwcNDBCMet.nc.md5                                             "
            + today
            + "T.{8}Z            49\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external ...tar.gz.md5.txt
    ra = File2.readFromFileUtf8(targzName + ".md5.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{32}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBagItNcCF() throws Throwable {
    String2.log("*** ArchiveADataset.testBagItNcCF()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "BagIt",
                  "default", // tar.gz
                  "erd.data@noaa.gov",
                  "cwwcNDBCMet",
                  "default", // all data vars
                  "&station=~\"3.*\"", // &station=~"3.*"
                  "nothing", // should be station, but use "nothing" as test of ncCFMA
                  ".ncCF", // default is .ncCFMA
                  "MD5"
                });
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    // decompress and look at contents
    SSR.windowsDecompressTargz(targzName, false, 5); // timeout minutes
    String tempDir = targzName.substring(0, targzName.length() - 7) + "/";
    int tempDirLen = tempDir.length();
    Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", true, ".*", "");
    table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
    table.removeColumn(FileVisitorDNLS.NAME);
    String results = table.dataToString();
    String expected =
        "url,size\n"
            + "bag-info.txt,4..\n"
            + "bagit.txt,55\n"
            + "manifest-md5.txt,54\n"
            + "tagmanifest-md5.txt,142\n"
            + "data/cwwcNDBCMet.nc,1.......\n"; // will change periodically
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at manifest
    String ra[] = File2.readFromFile(tempDir + "manifest-md5.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{32}  data/cwwcNDBCMet.nc\n"; // 2017-03-07 actual md5 verified by hand
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at bagit.txt
    ra = File2.readFromFile(tempDir + "bagit.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "BagIt-Version: 0.97\n" + "Tag-File-Character-Encoding: UTF-8\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional bag-info.txt
    ra = File2.readFromFile(tempDir + "bag-info.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "Contact-Email: erd.data@noaa.gov\n"
            + "Created_By: ArchiveADataset in ERDDAP v"
            + EDStatic.erddapVersion
            + "\n"
            + "ArchiveADataset_container_type: BagIt\n"
            + "ArchiveADataset_compression: tar.gz\n"
            + "ArchiveADataset_contact_email: erd.data@noaa.gov\n"
            + "ArchiveADataset_ERDDAP_datasetID: cwwcNDBCMet\n"
            + "ArchiveADataset_data_variables: \n"
            + "ArchiveADataset_extra_constraints: &station=~\"3.*\"\n"
            + "ArchiveADataset_subset_by: \n"
            + "ArchiveADataset_data_file_type: .ncCF\n"
            + "ArchiveADataset_digest_type: MD5\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional tagmanifest-md5.txt
    ra = File2.readFromFile(tempDir + "tagmanifest-md5.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = // 2017-03-07 actual md5's verified by hand
        "[0-9a-f]{32}  bag-info.txt\n"
            + "[0-9a-f]{32}  bagit.txt\n"
            + "[0-9a-f]{32}  manifest-md5.txt\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external cwwcNDBCMet_20170307183959Z.tar.gz.md5.txt
    ra = File2.readFromFileUtf8(targzName + ".md5.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = // 2017-03-07 actual md5 verified by hand
        "[0-9a-f]{32}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  /** A test of NCEI-preferences */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBagItNcCFMA() throws Throwable {
    String2.log("*** ArchiveADataset.testBagItNcCFMA()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "BagIt",
                  "tar.gz",
                  "erd.data@noaa.gov",
                  "cwwcNDBCMet",
                  "default", // all data vars
                  "&station=~\"3.*\"", // &station=~"3.*"
                  "", // should be station, but use "nothing" as test of ncCFMA
                  ".ncCFMA",
                  "SHA-256"
                });
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    // decompress and look at contents
    SSR.windowsDecompressTargz(targzName, false, 5); // timeout minutes
    String tempDir = targzName.substring(0, targzName.length() - 7) + "/";
    int tempDirLen = tempDir.length();
    Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", true, ".*", "");
    table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
    table.removeColumn(FileVisitorDNLS.NAME);
    String results = table.dataToString();
    String expected =
        "url,size\n"
            + "bag-info.txt,4..\n"
            + "bagit.txt,55\n"
            + "manifest-sha256.txt,86\n"
            + "tagmanifest-sha256.txt,2..\n"
            + "data/cwwcNDBCMet.nc,4.......\n"; // will change periodically
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at manifest
    String ra[] = File2.readFromFile(tempDir + "manifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  data/cwwcNDBCMet.nc\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at bagit.txt
    ra = File2.readFromFile(tempDir + "bagit.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "BagIt-Version: 0.97\n" + "Tag-File-Character-Encoding: UTF-8\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional bag-info.txt
    ra = File2.readFromFile(tempDir + "bag-info.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "Contact-Email: erd.data@noaa.gov\n"
            + "Created_By: ArchiveADataset in ERDDAP v"
            + EDStatic.erddapVersion
            + "\n"
            + "ArchiveADataset_container_type: BagIt\n"
            + "ArchiveADataset_compression: tar.gz\n"
            + "ArchiveADataset_contact_email: erd.data@noaa.gov\n"
            + "ArchiveADataset_ERDDAP_datasetID: cwwcNDBCMet\n"
            + "ArchiveADataset_data_variables: \n"
            + "ArchiveADataset_extra_constraints: &station=~\"3.*\"\n"
            + "ArchiveADataset_subset_by: \n"
            + "ArchiveADataset_data_file_type: .ncCFMA\n"
            + "ArchiveADataset_digest_type: SHA-256\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional tagmanifest-sha256.txt
    ra = File2.readFromFile(tempDir + "tagmanifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = // 2017-03-07 actual sha256's verified by hand
        "[0-9a-f]{64}  bag-info.txt\n"
            + "[0-9a-f]{64}  bagit.txt\n"
            + "[0-9a-f]{64}  manifest-sha256.txt\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external cwwcNDBCMet_20170307183959Z.tar.gz.sha256.txt
    ra = File2.readFromFileUtf8(targzName + ".sha256.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testOriginalTrajectoryProfile() throws Throwable {
    String2.log("*** ArchiveADataset.testOriginalTrajectoryProfile()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  // "-verbose",  //verbose is really verbose for this test
                  "original",
                  "tar.gz",
                  "erd.data@noaa.gov",
                  "scrippsGliders",
                  "default", // all data vars
                  // &trajectory=~"sp05.*"&time>=2015-01-01&time<=2015-01-05
                  "&trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05",
                  "default",
                  "default", // trajectory, .ncCFMA
                  "default"
                }); // SHA-256
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String ra[] = File2.readFromFileUtf8(targzName + ".listOfFiles.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    String results = ra[1];
    String expected =
        "READ_ME.txt                                                      "
            + today
            + "T.{8}Z           4..\n"
            + "scrippsGliders.das                                               "
            + today
            + "T.{8}Z         1....\n"
            + "scrippsGliders.dds                                               "
            + today
            + "T.{8}Z           7..\n"
            + "data/\n"
            + "  sp051-20141112.nc                                              "
            + today
            + "T.{8}Z        1.....\n"
            + "  sp051-20141112.nc.sha256                                       "
            + today
            + "T.{8}Z            84\n"
            + "  sp052-20140814.nc                                              "
            + today
            + "T.{8}Z        4.....\n"
            + "  sp052-20140814.nc.sha256                                       "
            + today
            + "T.{8}Z            84\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external ...tar.gz.sha256.txt
    ra = File2.readFromFileUtf8(targzName + ".sha256.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBagItTrajectoryProfile() throws Throwable {
    String2.log("*** ArchiveADataset.testBagItTrajectoryProfile()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  // "-verbose",  //verbose is really verbose for this test
                  "bagit",
                  "zip",
                  "erd.data@noaa.gov",
                  "scrippsGliders",
                  "default", // all data vars
                  // &trajectory=~"sp05.*"&time>=2015-01-01&time<=2015-01-05
                  "&trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05",
                  "default",
                  "default", // trajectory, .ncCFMA
                  "default"
                }); // SHA-256
    Test.ensureTrue(targzName.endsWith(".zip"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    // decompress and look at contents
    SSR.unzipADirectory(targzName, 60, null); // timeoutSeconds
    String tempDir = targzName.substring(0, targzName.length() - 4) + "/";
    int tempDirLen = tempDir.length();
    Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", true, ".*", "");
    table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
    table.removeColumn(FileVisitorDNLS.NAME);
    String results = table.dataToString();
    String expected =
        "url,size\n"
            + "bag-info.txt,4..\n"
            + "bagit.txt,55\n"
            + "manifest-sha256.txt,178\n"
            + "tagmanifest-sha256.txt,241\n"
            + "data/sp051-20141112.nc,148...\n"
            + "data/sp052-20140814.nc,499...\n"; // will change periodically
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at manifest
    String ra[] = File2.readFromFile(tempDir + "manifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  data/sp051-20141112.nc\n" + "[0-9a-f]{64}  data/sp052-20140814.nc\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at bagit.txt
    ra = File2.readFromFile(tempDir + "bagit.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "BagIt-Version: 0.97\n" + "Tag-File-Character-Encoding: UTF-8\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional bag-info.txt
    ra = File2.readFromFile(tempDir + "bag-info.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "Contact-Email: erd.data@noaa.gov\n"
            + "Created_By: ArchiveADataset in ERDDAP v"
            + EDStatic.erddapVersion
            + "\n"
            + "ArchiveADataset_container_type: bagit\n"
            + "ArchiveADataset_compression: zip\n"
            + "ArchiveADataset_contact_email: erd.data@noaa.gov\n"
            + "ArchiveADataset_ERDDAP_datasetID: scrippsGliders\n"
            + "ArchiveADataset_data_variables: \n"
            + "ArchiveADataset_extra_constraints: &trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05\n"
            + "ArchiveADataset_subset_by: trajectory\n"
            + "ArchiveADataset_data_file_type: .ncCFMA\n"
            + "ArchiveADataset_digest_type: SHA-256\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional tagmanifest-sha256.txt
    ra = File2.readFromFile(tempDir + "tagmanifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "[0-9a-f]{64}  bag-info.txt\n"
            + "[0-9a-f]{64}  bagit.txt\n"
            + "[0-9a-f]{64}  manifest-sha256.txt\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external cwwcNDBCMet_20170307183959Z.tar.gz.sha256.txt
    ra = File2.readFromFileUtf8(targzName + ".sha256.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testOriginalGridAll() throws Throwable {
    String2.log("*** ArchiveADataset.testOriginalGridAll()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "original",
                  "tar.gz",
                  "erd.data@noaa.gov",
                  "erdVHNchla8day", // datasetID
                  "default", // dataVarsCSV
                  "default", // constraintsString
                  "SHA-256"
                }); // SHA-256
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String ra[] = File2.readFromFileUtf8(targzName + ".listOfFiles.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    String results = ra[1];
    String expected =
        "erdVHNchla8day.das                                               "
            + today
            + "T.{8}Z          6...\n"
            + "erdVHNchla8day.dds                                               "
            + today
            + "T.{8}Z           438\n"
            + "READ_ME.txt                                                      "
            + today
            + "T.{8}Z           3..\n"
            + "data/\n"
            + "  20150301000000Z.nc                                             "
            + today
            + "T.{8}Z     44784....\n"
            + "  20150301000000Z.nc.sha256                                      "
            + today
            + "T.{8}Z            85\n"
            + "  20150302000000Z.nc                                             "
            + today
            + "T.{8}Z     44784....\n"
            + "  20150302000000Z.nc.sha256                                      "
            + today
            + "T.{8}Z            85\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external ...tar.gz.sha256.txt
    ra = File2.readFromFileUtf8(targzName + ".sha256.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBagItGridAll() throws Throwable {
    String2.log("*** ArchiveADataset.testBagItGridAll()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "BagIt",
                  "ZIP",
                  "erd.data@noaa.gov",
                  "erdVHNchla8day", // datasetID
                  "default", // dataVarsCSV
                  "default", // constraintsString
                  "SHA-256"
                }); // SHA-256
    Test.ensureTrue(targzName.endsWith(".zip"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    // decompress and look at contents
    SSR.unzipADirectory(targzName, 60, null); // timeoutSeconds
    String tempDir = targzName.substring(0, targzName.length() - 4) + "/";
    int tempDirLen = tempDir.length();
    Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", true, ".*", "");
    table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
    table.removeColumn(FileVisitorDNLS.NAME);
    String results = table.dataToString();
    String expected =
        "url,size\n"
            + "bag-info.txt,(39.|40.)\n"
            + "bagit.txt,55\n"
            + "manifest-sha256.txt,180\n"
            + "tagmanifest-sha256.txt,241\n"
            + "data/20150301000000Z.nc,447840...\n"
            + "data/20150302000000Z.nc,447840...\n"; // will change periodically
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at manifest
    String ra[] = File2.readFromFile(tempDir + "manifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "[0-9a-f]{64}  data/20150301000000Z.nc\n" + "[0-9a-f]{64}  data/20150302000000Z.nc\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at bagit.txt
    ra = File2.readFromFile(tempDir + "bagit.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "BagIt-Version: 0.97\n" + "Tag-File-Character-Encoding: UTF-8\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional bag-info.txt
    ra = File2.readFromFile(tempDir + "bag-info.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "Contact-Email: erd.data@noaa.gov\n"
            + "Created_By: ArchiveADataset in ERDDAP v"
            + EDStatic.erddapVersion
            + "\n"
            + "ArchiveADataset_container_type: BagIt\n"
            + "ArchiveADataset_compression: zip\n"
            + "ArchiveADataset_contact_email: erd.data@noaa.gov\n"
            + "ArchiveADataset_ERDDAP_datasetID: erdVHNchla8day\n"
            + "ArchiveADataset_data_variables: \n"
            + "ArchiveADataset_constraints: \\[\\(2015-03-01T00:00:00Z\\):\\(2015-03-02T00:00:00Z\\)\\]\\[\\]\\[\\]\\[\\]\n"
            + "ArchiveADataset_digest_type: SHA-256\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional tagmanifest-sha256.txt
    ra = File2.readFromFile(tempDir + "tagmanifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "[0-9a-f]{64}  bag-info.txt\n"
            + "[0-9a-f]{64}  bagit.txt\n"
            + "[0-9a-f]{64}  manifest-sha256.txt\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external cwwcNDBCMet_20170307183959Z.tar.gz.sha256.txt
    ra = File2.readFromFileUtf8(targzName + ".sha256.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testOriginalGridSubset() throws Throwable {
    String2.log("*** ArchiveADataset.testOriginalGridSubset()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "original",
                  "tar.gz",
                  "erd.data@noaa.gov",
                  "erdVHNchla8day", // datasetID
                  "default", // dataVarsCSV
                  "[(2015-03-02T00:00:00Z)][][][]", // constraintsString
                  "SHA-1"
                });
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String ra[] = File2.readFromFileUtf8(targzName + ".listOfFiles.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    String results = ra[1];
    String expected =
        "erdVHNchla8day.das                                               "
            + today
            + "T.{8}Z          6...\n"
            + "erdVHNchla8day.dds                                               "
            + today
            + "T.{8}Z           4..\n"
            + "READ_ME.txt                                                      "
            + today
            + "T.{8}Z           3..\n"
            + "data/\n"
            + "  20150302000000Z.nc                                             "
            + today
            + "T.{8}Z     44784....\n"
            + "  20150302000000Z.nc.sha1                                        "
            + today
            + "T.{8}Z            61\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external ...tar.gz.sha1.txt
    ra = File2.readFromFileUtf8(targzName + ".sha1.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{40}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBagItGridSubset() throws Throwable {
    String2.log("*** ArchiveADataset.testBagItGridSubset()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "BagIt",
                  "zip",
                  "erd.data@noaa.gov",
                  "erdVHNchla8day", // datasetID
                  "default", // dataVarsCSV
                  "[(2015-03-02T00:00:00Z)][][][]", // constraintsString
                  "SHA-1"
                });
    Test.ensureTrue(targzName.endsWith(".zip"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(5000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(5000);

    // decompress and look at contents
    SSR.unzipADirectory(targzName, 60, null); // timeoutSeconds
    String tempDir = targzName.substring(0, targzName.length() - 4) + "/";
    int tempDirLen = tempDir.length();
    Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", true, ".*", "");
    table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
    table.removeColumn(FileVisitorDNLS.NAME);
    String results = table.dataToString();
    String expected =
        "url,size\n"
            + "bag-info.txt,3..\n"
            + "bagit.txt,55\n"
            + "manifest-sha1.txt,66\n"
            + "tagmanifest-sha1.txt,167\n"
            + "data/20150302000000Z.nc,447840...\n"; // will change periodically
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at manifest
    String ra[] = File2.readFromFile(tempDir + "manifest-sha1.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{40}  data/20150302000000Z.nc\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at bagit.txt
    ra = File2.readFromFile(tempDir + "bagit.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "BagIt-Version: 0.97\n" + "Tag-File-Character-Encoding: UTF-8\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional bag-info.txt
    ra = File2.readFromFile(tempDir + "bag-info.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "Contact-Email: erd.data@noaa.gov\n"
            + "Created_By: ArchiveADataset in ERDDAP v"
            + EDStatic.erddapVersion
            + "\n"
            + "ArchiveADataset_container_type: BagIt\n"
            + "ArchiveADataset_compression: zip\n"
            + "ArchiveADataset_contact_email: erd.data@noaa.gov\n"
            + "ArchiveADataset_ERDDAP_datasetID: erdVHNchla8day\n"
            + "ArchiveADataset_data_variables: \n"
            + "ArchiveADataset_constraints: \\[\\(2015-03-02T00:00:00Z\\)\\]\\[\\]\\[\\]\\[\\]\n"
            + "ArchiveADataset_digest_type: SHA-1\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional tagmanifest-sha1.txt
    ra = File2.readFromFile(tempDir + "tagmanifest-sha1.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "[0-9a-f]{40}  bag-info.txt\n"
            + "[0-9a-f]{40}  bagit.txt\n"
            + "[0-9a-f]{40}  manifest-sha1.txt\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external ....tar.gz.sha1.txt
    ra = File2.readFromFileUtf8(targzName + ".sha1.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{40}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBagItGridSubset2() throws Throwable {
    String2.log("*** ArchiveADataset.testBagItGridSubset2()");
    int language = 0;

    // make the targz
    String targzName =
        new ArchiveADataset()
            .doIt(
                language,
                new String[] {
                  "-verbose",
                  "BagIt",
                  "tar.gz",
                  "erd.data@noaa.gov",
                  "erdVHNchla8day", // datasetID
                  "default", // dataVarsCSV
                  "[(2015-03-01T00:00:00Z):(2015-03-02T00:00:00Z)][][][]", // constraintsString
                  "SHA-256"
                });
    Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

    // display it (in 7zip)
    Math2.sleep(10000);
    // Test.displayInBrowser("file://" + targzName);
    // Math2.sleep(10000);

    // decompress and look at contents
    SSR.windowsDecompressTargz(targzName, false, 20); // timeout minutes
    String tempDir = targzName.substring(0, targzName.length() - 7) + "/";
    int tempDirLen = tempDir.length();
    Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", true, ".*", "");
    table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
    table.removeColumn(FileVisitorDNLS.NAME);
    String results = table.dataToString();
    String expected =
        "url,size\n"
            + "bag-info.txt,4..\n"
            + "bagit.txt,55\n"
            + "manifest-sha256.txt,1..\n"
            + "tagmanifest-sha256.txt,2..\n"
            + "data/20150301000000Z.nc,447......\n"
            + // will change periodically
            "data/20150302000000Z.nc,447......\n"; // will change periodically
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at manifest
    String ra[] = File2.readFromFile(tempDir + "manifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "[0-9a-f]{64}  data/20150301000000Z.nc\n" + "[0-9a-f]{64}  data/20150302000000Z.nc\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at bagit.txt
    ra = File2.readFromFile(tempDir + "bagit.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "BagIt-Version: 0.97\n" + "Tag-File-Character-Encoding: UTF-8\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional bag-info.txt
    ra = File2.readFromFile(tempDir + "bag-info.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "Contact-Email: erd.data@noaa.gov\n"
            + "Created_By: ArchiveADataset in ERDDAP v"
            + EDStatic.erddapVersion
            + "\n"
            + "ArchiveADataset_container_type: BagIt\n"
            + "ArchiveADataset_compression: tar.gz\n"
            + "ArchiveADataset_contact_email: erd.data@noaa.gov\n"
            + "ArchiveADataset_ERDDAP_datasetID: erdVHNchla8day\n"
            + "ArchiveADataset_data_variables: \n"
            + "ArchiveADataset_constraints: \\[\\(2015-03-01T00:00:00Z\\):\\(2015-03-02T00:00:00Z\\)\\]\\[\\]\\[\\]\\[\\]\n"
            + "ArchiveADataset_digest_type: SHA-256\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at optional tagmanifest-sha256.txt
    ra = File2.readFromFile(tempDir + "tagmanifest-sha256.txt", File2.UTF_8);
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected =
        "[0-9a-f]{64}  bag-info.txt\n"
            + "[0-9a-f]{64}  bagit.txt\n"
            + "[0-9a-f]{64}  manifest-sha256.txt\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // look at external ....tar.gz.sha256.txt
    ra = File2.readFromFileUtf8(targzName + ".sha256.txt");
    Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
    results = ra[1];
    expected = "[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // String2.pressEnterToContinue("\n");
  }
}
