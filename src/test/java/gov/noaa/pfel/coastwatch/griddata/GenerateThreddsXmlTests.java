package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.StringArray;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import tags.TagDisabledMissingFile;

class GenerateThreddsXmlTests {
  /**
   * This tests shortening all the boldTitles in DataSet.properties, which is only useful while
   * testing shortenBoldTitles.
   */
  @org.junit.jupiter.api.Test
  void testShortenBoldTitles() {
    String2.log("\n*** GenerateThreddsXml.testShortenBoldTitles");
    ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");
    String validDataSets[] = String2.split(dataSetRB2.getString("validDataSets", null), '`');
    // String2.log("validDataSets n=" + validDataSets.length);
    for (int i = 0; i < validDataSets.length; i++)
      GenerateThreddsXml.shortenBoldTitle(
          dataSetRB2.getString(validDataSets[i].substring(1) + "BoldTitle", null));
    // String2.log(GenerateThreddsXml.shortenBoldTitle(dataSetRB2.getString(
    //     validDataSets[i].substring(1) + "BoldTitle", null)));
  }

  /** This does a little test of this class -- specific to Bob's computer. */
  @org.junit.jupiter.api.Test
  @TagDisabledMissingFile
  void basicTest() throws Exception {
    // verbose = true;
    StringArray sa =
        GenerateThreddsXml.generateThreddsXml(
            "c:/u00/",
            "satellite/",
            "C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/incompleteMainCatalog.xml",
            "c:/u00/xml/");
    String2.log("first catalog.xml=" + sa.get(0));
    //        TestUtil.displayInBrowser("file://" + sa.get(0));  //.xml
    //        TestUtil.displayInBrowser("file://f:/u00/xml/catalog.xml");
  }

  @org.junit.jupiter.api.Test
  void testFormerFileUtilHelpers() throws Throwable {
    // test is25hour is33hour
    Test.ensureEqual(GenerateThreddsXml.fourNameIs25Hour("t24h"), true, "");
    Test.ensureEqual(GenerateThreddsXml.fourNameIs25Hour("u25h"), true, "");
    Test.ensureEqual(GenerateThreddsXml.fourNameIs25Hour("t33h"), false, "");

    Test.ensureEqual(GenerateThreddsXml.fourNameIs33Hour("t24h"), false, "");
    Test.ensureEqual(GenerateThreddsXml.fourNameIs33Hour("u25h"), false, "");
    Test.ensureEqual(GenerateThreddsXml.fourNameIs33Hour("t33h"), true, "");

    // test convertDaveNameToCWBrowserName
    // composite, westus center day 3/10 => 31 + 28 + 10 = 69
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("AH2001068_2001070_sstd_westus.grd"),
        "LAHsstdS3day_20010310120000_x225_X255_y22_Y50",
        "convertDaveName a");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("AH2001068_2001070_sstd_westus"),
        "LAHsstdS3day_20010310120000_x225_X255_y22_Y50",
        "convertDaveName b");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("AH2001001_2001031_sstd_westus"),
        "LAHsstdSmday_20010116120000_x225_X255_y22_Y50",
        "convertDaveName b2");
    // pass, westsa
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("AH2005060_044800h_sstd_westsa.grd"),
        "LAHsstdSpass_20050301044800_x265_X295_y-45_Y2",
        "convertDaveName c");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("AH2005060_044800h_sstd_westsa"),
        "LAHsstdSpass_20050301044800_x265_X295_y-45_Y2",
        "convertDaveName d");
    // 25hour and no region info //note that u24h is left intact
    // dave's h time is end time, cwBrowserName is centered time
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("CM2005069_120000h_u25h.grd"),
        "LCMu25hS25hour_20050309233000",
        "convertDaveName e");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("CM2005069_120000h_u25h"),
        "LCMu25hS25hour_20050309233000",
        "convertDaveName f");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("GA2005069_120000h_t24h"),
        "LGAt24hS25hour_20050309233000",
        "convertDaveName g");
    // 33hour and no region info //note that u33h is left intact
    // dave's h time is end time, cwBrowserName is centered time
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("CM2005069_120000h_u33h.grd"),
        "LCMu33hS33hour_20050309193000",
        "convertDaveName h");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("CM2005069_120000h_u33h"),
        "LCMu33hS33hour_20050309193000",
        "convertDaveName i");
    Test.ensureEqual(
        GenerateThreddsXml.convertDaveNameToCWBrowserName("GA2005069_120000h_t33h"),
        "LGAt33hS33hour_20050309193000",
        "convertDaveName j");
    // intentional error: 2 day composite
    try {
      GenerateThreddsXml.convertDaveNameToCWBrowserName("GA2005069_2005070_ssta");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
    }
    // intentional error: month composite, incorrect begin date
    try {
      GenerateThreddsXml.convertDaveNameToCWBrowserName("GA2005002_2005031_ssta");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
    }
    // intentional error: month composite, incorrect end date
    try {
      GenerateThreddsXml.convertDaveNameToCWBrowserName("GA2005001_2005030_ssta");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
    }

    // monthly composite, standard units
    String names[] = {
      "LATsstaSmday_20030116120000",
      "LATsstaSmday_20030116120000.hdf",
      "LATsstaSmday_20030116120000_x-135_X-105_y22_Y51", // note decimal digits on end of maxY
      "LATsstaSmday_20030116120000_x-135_X-105_y22_Y51.mat",
      "LATsstaSmday_20030116120000_x-135_X-105_y22_Y51_other stuff"
    };
    for (int i = 0; i < names.length; i++) {
      String2.log("i=" + i);
      Test.ensureEqual(
          GenerateThreddsXml.getRawDateString(names[i]), "20030116120000", "getRawDateString");
      Test.ensureEqual(
          GenerateThreddsXml.getCenteredCalendar(names[i]),
          ZonedDateTime.of(2003, 1, 16, 12, 0, 0, 0, ZoneOffset.UTC),
          "getCenteredCalendar");
    }

    names =
        new String[] {
          "LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.5", // note decimal digits on end of maxY
          "LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.5.mat",
          "LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.5_other stuff"
        };

    // 8 day composite, alt units
    String name2 = "LATsstaA8day_20030110000000";
    Test.ensureEqual(
        GenerateThreddsXml.getRawDateString(name2), "20030110000000", "getRawDateString 2");
    Test.ensureEqual(
        GenerateThreddsXml.getCenteredCalendar(name2),
        ZonedDateTime.of(2003, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC),
        "getCenteredCalendar 2");

    // pass, std units
    String name3 = "LQNux10Spass_20030331123456";
    Test.ensureEqual(
        GenerateThreddsXml.getRawDateString(name3), "20030331123456", "getRawDateString 3");
    Test.ensureEqual(
        GenerateThreddsXml.getCenteredCalendar(name3),
        ZonedDateTime.of(2003, 3, 31, 12, 34, 56, 00, ZoneOffset.UTC),
        "getCenteredCalendar 3");

    // 25hours
    String name25 = "LGAt24hS25hour_20030331083000";
    Test.ensureEqual(
        GenerateThreddsXml.getRawDateString(name25), "20030331083000", "getRawDateString 25");
    Test.ensureEqual(
        GenerateThreddsXml.getCenteredCalendar(name25),
        ZonedDateTime.of(2003, 3, 31, 8, 30, 0, 0, ZoneOffset.UTC),
        "getCenteredCalendar 25");

    // 33hours
    String name33 = "LGAt33hS33hour_20030331083000";
    Test.ensureEqual(
        GenerateThreddsXml.getRawDateString(name33), "20030331083000", "getRawDateString 33");
    Test.ensureEqual(
        GenerateThreddsXml.getCenteredCalendar(name33),
        ZonedDateTime.of(2003, 3, 31, 8, 30, 0, 0, ZoneOffset.UTC),
        "getCenteredCalendar 33");

    // 1 day composite climatology
    String name4 = "LATsstaS1day_00010110120000";
    Test.ensureEqual(
        GenerateThreddsXml.getRawDateString(name4), "00010110120000", "getRawDateString 4");
    Test.ensureEqual(
        GenerateThreddsXml.getCenteredCalendar(name4),
        ZonedDateTime.of(0001, 1, 10, 12, 00, 00, 0, ZoneOffset.UTC),
        "getEndCalendar 4");
  }
}
