package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;

class FileNameUtilityTests {

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    String2.log("\nFileNameUtility.testBasic");

    // test is25hour is33hour
    Test.ensureEqual(FileNameUtility.fourNameIs25Hour("t24h"), true, "");
    Test.ensureEqual(FileNameUtility.fourNameIs25Hour("u25h"), true, "");
    Test.ensureEqual(FileNameUtility.fourNameIs25Hour("t33h"), false, "");

    Test.ensureEqual(FileNameUtility.fourNameIs33Hour("t24h"), false, "");
    Test.ensureEqual(FileNameUtility.fourNameIs33Hour("u25h"), false, "");
    Test.ensureEqual(FileNameUtility.fourNameIs33Hour("t33h"), true, "");

    Test.ensureEqual(FileNameUtility.is25Hour("LGAt24h"), true, "");
    Test.ensureEqual(FileNameUtility.is25Hour("LCBu25h"), true, "");
    Test.ensureEqual(FileNameUtility.is25Hour("LGAt33h"), false, "");

    Test.ensureEqual(FileNameUtility.is33Hour("LGAt24h"), false, "");
    Test.ensureEqual(FileNameUtility.is33Hour("LCBu25h"), false, "");
    Test.ensureEqual(FileNameUtility.is33Hour("LGAt33h"), true, "");

    // validate all the dataset properties
    FileNameUtility fnu;
    // fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowserSA");
    // fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowserAK");
    // fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowserWW180");
    // fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowserWW360");
    // fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CCBrowser");
    // fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");
    fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");

    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdSpass_20010331230000_x225_X255_y22_Y50"),
        0,
        "");
    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdS1day_20010331120000_x225_X255_y22_Y50"),
        24,
        "");
    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdS25hour_20010331150000_x225_X255_y22_Y50"),
        25,
        "");
    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdS33hour_20010331150000_x225_X255_y22_Y50"),
        33,
        "");
    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdS3day_20010310120000_x225_X255_y22_Y50"),
        3 * 24,
        "");
    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdSmday_20010116120000_x225_X255_y22_Y50"),
        30 * 24,
        "");
    Test.ensureEqual(
        FileNameUtility.getTimePeriodNHours("LAHsstdS1year_200100701_x225_X255_y22_Y50"),
        365 * 24,
        "");

    // test convertDaveNameToCWBrowserName
    // composite, westus center day 3/10 => 31 + 28 + 10 = 69
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("AH2001068_2001070_sstd_westus.grd"),
        "LAHsstdS3day_20010310120000_x225_X255_y22_Y50",
        "convertDaveName a");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("AH2001068_2001070_sstd_westus"),
        "LAHsstdS3day_20010310120000_x225_X255_y22_Y50",
        "convertDaveName b");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("AH2001001_2001031_sstd_westus"),
        "LAHsstdSmday_20010116120000_x225_X255_y22_Y50",
        "convertDaveName b2");
    // pass, westsa
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("AH2005060_044800h_sstd_westsa.grd"),
        "LAHsstdSpass_20050301044800_x265_X295_y-45_Y2",
        "convertDaveName c");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("AH2005060_044800h_sstd_westsa"),
        "LAHsstdSpass_20050301044800_x265_X295_y-45_Y2",
        "convertDaveName d");
    // 25hour and no region info //note that u24h is left intact
    // dave's h time is end time, cwBrowserName is centered time
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("CM2005069_120000h_u25h.grd"),
        "LCMu25hS25hour_20050309233000",
        "convertDaveName e");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("CM2005069_120000h_u25h"),
        "LCMu25hS25hour_20050309233000",
        "convertDaveName f");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("GA2005069_120000h_t24h"),
        "LGAt24hS25hour_20050309233000",
        "convertDaveName g");
    // 33hour and no region info //note that u33h is left intact
    // dave's h time is end time, cwBrowserName is centered time
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("CM2005069_120000h_u33h.grd"),
        "LCMu33hS33hour_20050309193000",
        "convertDaveName h");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("CM2005069_120000h_u33h"),
        "LCMu33hS33hour_20050309193000",
        "convertDaveName i");
    Test.ensureEqual(
        FileNameUtility.convertDaveNameToCWBrowserName("GA2005069_120000h_t33h"),
        "LGAt33hS33hour_20050309193000",
        "convertDaveName j");
    // intentional error: 2 day composite
    try {
      FileNameUtility.convertDaveNameToCWBrowserName("GA2005069_2005070_ssta");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
    }
    // intentional error: month composite, incorrect begin date
    try {
      FileNameUtility.convertDaveNameToCWBrowserName("GA2005002_2005031_ssta");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
    }
    // intentional error: month composite, incorrect end date
    try {
      FileNameUtility.convertDaveNameToCWBrowserName("GA2005001_2005030_ssta");
      throw new Throwable("It should have failed.");
    } catch (Exception e) {
    }

    // test convertCWBrowserNameToDaveName
    // composite, westus center day 3/10 => 31 + 28 + 10 = 69
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LAHsstdS3day_20010310120000_x225_X255_y22_Y50.grd"),
        "AH2001068_2001070_sstd_westus",
        "convertCWName a");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LAHsstdS3day_20010310120000_x225_X255_y22_Y50"),
        "AH2001068_2001070_sstd_westus",
        "convertCWName b");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LAHsstdSmday_20010116120000_x225_X255_y22_Y50"),
        "AH2001001_2001031_sstd_westus",
        "convertCWName b2");
    // pass, westsa
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LAHsstdSpass_20050301044800_x265_X295_y-45_Y2.grd"),
        "AH2005060_044800h_sstd_westsa",
        "convertCWName c");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LAHsstdSpass_20050301044800_x265_X295_y-45_Y2"),
        "AH2005060_044800h_sstd_westsa",
        "convertCWName d");
    // 25hour no region info //note that u24h is left intact
    // dave's h time is end time, cwBrowserName is centered time
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LCMu25hS25hour_20050309233000.grd"),
        "CM2005069_120000h_u25h",
        "convertCWName e");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LCMu25hS25hour_20050309233000"),
        "CM2005069_120000h_u25h",
        "convertCWName f");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LGAt24hS25hour_20050309233000"),
        "GA2005069_120000h_t24h",
        "convertCWName g");
    // 33hour no region info //note that u33h is left intact
    // dave's h time is end time, cwBrowserName is centered time
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LCMu33hS33hour_20050309193000.grd"),
        "CM2005069_120000h_u33h",
        "convertCWName h");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LCMu33hS33hour_20050309193000"),
        "CM2005069_120000h_u33h",
        "convertCWName i");
    Test.ensureEqual(
        fnu.convertCWBrowserNameToDaveName("LGAt33hS33hour_20050309193000"),
        "GA2005069_120000h_t33h",
        "convertCWName j");

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
      Test.ensureEqual(FileNameUtility.get6CharName(names[i]), "ATssta", "get6CharName");
      Test.ensureEqual(FileNameUtility.getAlternateUnits(names[i]), false, "getAlternateUnits");
      Test.ensureEqual(
          FileNameUtility.getTimePeriodString(names[i]), "mday", "getTimePeriodString");
      Test.ensureEqual(
          FileNameUtility.getRawDateString(names[i]), "20030116120000", "getRawDateString");
      Test.ensureEqual(
          FileNameUtility.getCenteredCalendar(names[i]),
          Calendar2.newGCalendarZulu(2003, 1, 16, 12, 0, 0, 0),
          "getCenteredCalendar");
      Test.ensureEqual(
          FileNameUtility.getEndCalendar(names[i]),
          Calendar2.newGCalendarZulu(2003, 2, 1, 0, 0, 0, 0),
          "getEndCalendar");
      Test.ensureEqual(
          FileNameUtility.getTimePeriodNHours(names[i]), 30 * 24, "getTimePeriodNHours");
      Test.ensureEqual(
          FileNameUtility.getStartCalendar(names[i]).getTimeInMillis(),
          Calendar2.newGCalendarZulu(2003, 1, 1).getTimeInMillis(),
          "getStartCalendar");
      Test.ensureEqual(fnu.getMinX(names[i]), -135, "getMinX");
      Test.ensureEqual(fnu.getMaxX(names[i]), -105, "getMaxX");
      Test.ensureEqual(fnu.getMinY(names[i]), 22, "getMinY");
      Test.ensureEqual(fnu.getMaxY(names[i]), 51, "getMaxY");
      Test.ensureEqual(fnu.getSatellite(names[i]), "POES", "getSatellite");
      Test.ensureEqual(fnu.getSensor(names[i]), "AVHRR HRPT", "getSensor");
      Test.ensureEqual(FileNameUtility.getComposite(names[i]), "true", "getComposite");
      Test.ensureEqual(
          fnu.getBoldTitle(names[i]),
          "SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night",
          "getBoldTitle");
      Test.ensureEqual(
          fnu.getCourtesy(names[i]), "NOAA NWS Monterey and NOAA CoastWatch", "getCourtesy");
      Test.ensureEqual(fnu.getLatLonFractionDigits(names[i]), 4, "getLatLonFractionDigits");
      Test.ensureEqual(fnu.getDataFractionDigits(names[i]), 1, "getDataFractionDigits");
      Test.ensureEqual(fnu.getReadableUnits(names[i]), "degree C", "getReadableUnits");
      Test.ensureEqual(fnu.getUdUnits(names[i]), "degree_C", "getUDUnits");
      Test.ensureEqual(
          FileNameUtility.getConventions(), "COARDS, CF-1.6, ACDD-1.3, CWHDF", "getConvention");
      Test.ensureEqual(
          FileNameUtility.getMetadataConventions(),
          "COARDS, CF-1.6, ACDD-1.3, CWHDF",
          "getMetadataConvention");
      Test.ensureEqual(
          fnu.getAbstract(names[i]),
          "NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data is provided at high resolution (0.0125 degrees) for the North Pacific Ocean.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites.",
          "getAbstract");
      Test.ensureEqual(
          fnu.getKeywords(names[i]),
          "EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature",
          "getKeywords");
      Test.ensureEqual(
          FileNameUtility.getNamingAuthority(), "gov.noaa.pfeg.coastwatch", "getNamingAuthority");
      Test.ensureEqual(
          FileNameUtility.getKeywordsVocabulary(),
          "GCMD Science Keywords",
          "getKeywordsVocabulary");
      Test.ensureEqual(FileNameUtility.getCDMDataType(), "Grid", "getCDMDataType");
      String2.log("history=" + fnu.getHistory(names[i]));
      Test.ensureTrue(
          fnu.getHistory(names[i]).startsWith("NOAA NWS Monterey and NOAA CoastWatch\n20"),
          "getHistory"); // changes
      // every
      // year
      Test.ensureTrue(
          fnu.getHistory(names[i]).endsWith("NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD"),
          "getHistory");
      Test.ensureEqual(
          FileNameUtility.getDateCreated(),
          Calendar2.formatAsISODate(Calendar2.newGCalendarZulu()),
          "getDateCreated");
      Test.ensureEqual(
          FileNameUtility.getCreatorName(), "NOAA CoastWatch, West Coast Node", "getCreatorName");
      Test.ensureEqual(
          FileNameUtility.getCreatorURL(), "https://coastwatch.pfeg.noaa.gov", "getCreatorURL");
      Test.ensureEqual(FileNameUtility.getCreatorEmail(), "erd.data@noaa.gov", "getCreatorEmail");
      Test.ensureEqual(
          fnu.getInstitution(names[i]), "NOAA CoastWatch, West Coast Node", "getInstitution");
      Test.ensureEqual(
          FileNameUtility.getProject(), "CoastWatch (https://coastwatch.noaa.gov/)", "getProject");
      Test.ensureEqual(FileNameUtility.getProcessingLevel(), "3 (projected)", "getProcessingLevel");
      Test.ensureEqual(
          FileNameUtility.getAcknowledgement(),
          "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD",
          "getAcknowledgement");
      Test.ensureEqual(FileNameUtility.getLatUnits(), "degrees_north", "getLatUnits");
      Test.ensureEqual(FileNameUtility.getLonUnits(), "degrees_east", "getLonUnits");
      Test.ensureEqual(
          FileNameUtility.getStandardNameVocabulary(),
          "CF Standard Name Table v70",
          "getStandardNameVocabulary");
      Test.ensureEqual(
          FileNameUtility.getLicense(),
          "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.",
          "getLicense");
      Test.ensureEqual(
          fnu.getContributorName(names[i]), fnu.getCourtesy(names[i]), "getContributorName");
      Test.ensureEqual(
          FileNameUtility.getContributorRole(), "Source of level 2 data.", "getContributorRole");
      Test.ensureEqual(
          fnu.getSource(names[i]), "satellite observation: POES, AVHRR HRPT", "getSource");
      Test.ensureEqual(fnu.getStandardName(names[i]), "sea_surface_temperature", "getStandardName");
    }

    String desired = "LATsstaSmday";
    Test.ensureEqual(
        FileNameUtility.getID("LATsstaSmday_20030116120000_x-135_X-105_y22_Y50"), desired, "");
    Test.ensureEqual(
        FileNameUtility.getID("LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.mat"), desired, "");
    Test.ensureEqual(
        FileNameUtility.getID("LATsstaSmday_20030116120000_x-135_X-105_y22_Y50_other_stuff"),
        desired,
        "");

    names =
        new String[] {
          "LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.5", // note decimal digits on end of maxY
          "LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.5.mat",
          "LATsstaSmday_20030116120000_x-135_X-105_y22_Y50.5_other stuff"
        };
    for (int i = 0; i < names.length; i++) Test.ensureEqual(fnu.getMaxY(names[i]), 50.5, "getMaxY");

    // 8 day composite, alt units
    String name2 = "LATsstaA8day_20030110000000";
    Test.ensureEqual(FileNameUtility.getTimePeriodNHours(name2), 8 * 24, "getTimePeriodNHours 2");
    Test.ensureEqual(FileNameUtility.getComposite(name2), "true", "getComposite");
    Test.ensureEqual(fnu.getLatLonFractionDigits(name2), 4, "getLatLonFractionDigits 2");
    Test.ensureEqual(fnu.getDataFractionDigits(name2), 1, "getDataFractionDigits 2");
    Test.ensureEqual(fnu.getReadableUnits(name2), "degree F", "getReadable Units 2");
    Test.ensureEqual(
        fnu.getFGDCInfo(name2, "<metadata><idinfo><descript><abstract>"),
        "NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data is provided at high resolution (0.0125 degrees) for the North Pacific Ocean.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites.",
        "getFGDCInfo 2");
    Test.ensureEqual(
        FileNameUtility.getRawDateString(name2), "20030110000000", "getRawDateString 2");
    Test.ensureEqual(
        FileNameUtility.getCenteredCalendar(name2),
        Calendar2.newGCalendarZulu(2003, 1, 10, 0, 0, 0, 0),
        "getCenteredCalendar 2");
    Test.ensureEqual(
        FileNameUtility.getEndCalendar(name2),
        Calendar2.newGCalendarZulu(2003, 1, 14, 0, 0, 0, 0),
        "getEndCalendar 2");
    Test.ensureEqual(
        FileNameUtility.getStartCalendar(name2),
        Calendar2.newGCalendarZulu(2003, 1, 6, 0, 0, 0, 0),
        "getStartCalendar 2");
    Test.ensureEqual(
        FileNameUtility.getPassDate(name2),
        new int[] {12058, 12059, 12060, 12061, 12062, 12063, 12064, 12065},
        "getPassDate 2");
    Test.ensureEqual(
        FileNameUtility.getStartTime(name2),
        new double[] {0, 0, 0, 0, 0, 0, 0, 0},
        "getStartTime 2");

    // pass, std units
    String name3 = "LQNux10Spass_20030331123456";
    Test.ensureEqual(FileNameUtility.getTimePeriodNHours(name3), 0, "getTimePeriodNHours 3");
    Test.ensureEqual(FileNameUtility.getComposite(name3), "false", "getComposite 3");
    Test.ensureEqual(fnu.getReadableUnits(name3), "m s^-1", "getReadableUnits 3");
    Test.ensureEqual(fnu.getUdUnits(name3), "m s-1", "getUDUnits 3");
    Test.ensureEqual(
        FileNameUtility.getRawDateString(name3), "20030331123456", "getRawDateString 3");
    Test.ensureEqual(
        FileNameUtility.getCenteredCalendar(name3),
        Calendar2.newGCalendarZulu(2003, 3, 31, 12, 34, 56, 00),
        "getCenteredCalendar 3");
    Test.ensureEqual(
        FileNameUtility.getEndCalendar(name3),
        Calendar2.newGCalendarZulu(2003, 3, 31, 12, 34, 56, 00),
        "getEndCalendar 3");
    Test.ensureEqual(
        FileNameUtility.getStartCalendar(name3),
        Calendar2.newGCalendarZulu(2003, 3, 31, 12, 34, 56, 00),
        "getStartCalendar 3");
    Test.ensureEqual(FileNameUtility.getPassDate(name3), new int[] {12142}, "getPassDate 3");
    Test.ensureEqual(
        FileNameUtility.getStartTime(name3),
        new double[] {12 * 3600 + 34 * 60 + 56},
        "getStartTime 3");

    // 25hours
    String name25 = "LGAt24hS25hour_20030331083000";
    Test.ensureEqual(FileNameUtility.getTimePeriodNHours(name25), 25, "getTimePeriodNHours 25");
    Test.ensureEqual(
        FileNameUtility.getRawDateString(name25), "20030331083000", "getRawDateString 25");
    Test.ensureEqual(
        FileNameUtility.getCenteredCalendar(name25),
        Calendar2.newGCalendarZulu(2003, 3, 31, 8, 30, 0, 0),
        "getCenteredCalendar 25");
    Test.ensureEqual(
        FileNameUtility.getStartCalendar(name25),
        Calendar2.newGCalendarZulu(2003, 3, 30, 20, 0, 0, 0),
        "getStartCalendar 25");
    Test.ensureEqual(
        FileNameUtility.getEndCalendar(name25),
        Calendar2.newGCalendarZulu(2003, 3, 31, 21, 0, 0, 0),
        "getEndCalendar 25");
    Test.ensureEqual(
        FileNameUtility.getPassDate(name25), new int[] {12141, 12142}, "getPassDate 25");
    Test.ensureEqual(
        FileNameUtility.getStartTime(name25),
        new double[] {20 * 3600 + 0 * 60 + 00, 0},
        "getStartTime 25");

    // 33hours
    String name33 = "LGAt33hS33hour_20030331083000";
    Test.ensureEqual(FileNameUtility.getTimePeriodNHours(name33), 33, "getTimePeriodNHours 33");
    Test.ensureEqual(
        FileNameUtility.getRawDateString(name33), "20030331083000", "getRawDateString 33");
    Test.ensureEqual(
        FileNameUtility.getCenteredCalendar(name33),
        Calendar2.newGCalendarZulu(2003, 3, 31, 8, 30, 0, 0),
        "getCenteredCalendar 33");
    Test.ensureEqual(
        FileNameUtility.getStartCalendar(name33),
        Calendar2.newGCalendarZulu(2003, 3, 30, 16, 0, 0, 0),
        "getStartCalendar 33");
    Test.ensureEqual(
        FileNameUtility.getEndCalendar(name33),
        Calendar2.newGCalendarZulu(2003, 4, 1, 1, 0, 0, 0),
        "getEndCalendar 33");
    Test.ensureEqual(
        FileNameUtility.getPassDate(name33), new int[] {12141, 12142, 12143}, "getPassDate 33");
    Test.ensureEqual(
        FileNameUtility.getStartTime(name33),
        new double[] {16 * 3600 + 0 * 60 + 00, 0, 0},
        "getStartTime 33");

    // 1 day composite climatology
    String name4 = "LATsstaS1day_00010110120000";
    Test.ensureEqual(FileNameUtility.getTimePeriodNHours(name4), 24, "getTimePeriodNHours 4");
    Test.ensureEqual(FileNameUtility.getComposite(name4), "true", "getComposite 4");
    Test.ensureEqual(
        FileNameUtility.getRawDateString(name4), "00010110120000", "getRawDateString 4");
    Test.ensureEqual(
        FileNameUtility.getCenteredCalendar(name4),
        Calendar2.newGCalendarZulu(0001, 1, 10, 12, 00, 00, 0),
        "getEndCalendar 4");
    Test.ensureEqual(
        FileNameUtility.getEndCalendar(name4),
        Calendar2.newGCalendarZulu(0001, 1, 11, 0, 0, 0, 0),
        "getEndCalendar 4");
    Test.ensureEqual(
        FileNameUtility.getStartCalendar(name4),
        Calendar2.newGCalendarZulu(0001, 1, 10, 0, 0, 0, 0),
        "getStartCalendar 4");
    Test.ensureEqual(FileNameUtility.getPassDate(name4), new int[] {-719154}, "getPassDate 4");
    Test.ensureEqual(FileNameUtility.getStartTime(name4), new double[] {0}, "getStartTime 4");

    String2.log("All tests passed successfully.");
  }
}
