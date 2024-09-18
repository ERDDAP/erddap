package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.awt.Color;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class CompoundColorMapTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    // verbose = true;
    String basePaletteDir =
        EDStatic.getWebInfParentDirectory()
            + // with / separator and / at the end
            "WEB-INF/cptfiles/";
    String tempDir = SSR.getTempDirectory();
    File2.deleteAllFiles(tempDir);
    CompoundColorMap ccm;

    // **** make a realistic continuous ccm
    {
      String2.log("\n* Test CompoundColorMap continuous palette");
      boolean continuous = true;
      String newPalette =
          CompoundColorMap.makeCPT(
              basePaletteDir, "Rainbow", "Linear", 8, 32, 8, continuous, tempDir);
      ccm = new CompoundColorMap(newPalette);

      Test.ensureEqual(Integer.toHexString(ccm.getColor(8).getRGB()), "ff800080", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(32).getRGB()), "ff800000", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(8.5).getRGB()), "ff950095", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(31.5).getRGB()), "ff950000", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(7).getRGB()), "ff800080", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(33).getRGB()), "ff800000", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(Double.NaN).getRGB()), "ff808080", "");

      // speed test
      int n = 1000 * 1000; // bigger than typical map size (convenient for seeing how fast)
      Color c = Color.RED;
      Math2.random(5); // sets up Math2.random

      // warm up loop
      for (int i = 0; i < n; i++) {
        c = ccm.getColor(10 + 10 * Math2.random.nextDouble());
      }

      // timed loop
      long time = System.currentTimeMillis();
      ccm.cumulativeLookupTime = -1; // -1 indicate not used
      ccm.cumulativeTotalTime = -1; // -1 indicate not used
      ccm.cumulativeCount = 0; // 0 indicates not used
      for (int i = 0; i < n; i++) {
        c = ccm.getColor(10 + 10 * Math2.random.nextDouble());
      }
      // See TESTING ON/OFF above
      // Times I see in SgtMap are >2X slower (e.g., 1/4 the count takes 1/2 the time)
      // than times I see here.
      // I think it is because this test all fits in on-chip cache,
      // but real world use doesn't.
      String2.log(
          "  "
              + n
              + " getColors  last="
              + Integer.toHexString(c.getRGB())
              + "\n  cumLookupTime="
              + ccm.cumulativeLookupTime
              + "[was 60 if on, -1=off]"
              + "\n  cumTotalTime="
              + ccm.cumulativeTotalTime
              + "[was 434 if on, -1=off]"
              + // varies, but testTime more
              // consistent
              "\n  testTime="
              + (System.currentTimeMillis() - time)
              + "[was 703 if on, 500 if off]\n");
      // so removing all the tracking code makes times 7/9 of timed speed

      // delete the palette
      File2.delete(newPalette);
    }
    /* */

    // **** make a realistic !continuous ccm
    {
      String2.log("\n* Test CompoundColorMap not continuous palette");
      boolean continuous = false;
      String newPalette =
          CompoundColorMap.makeCPT(
              basePaletteDir, "Rainbow", "Linear", 8, 32, 8, continuous, tempDir);
      ccm = new CompoundColorMap(newPalette);

      Test.ensureEqual(Integer.toHexString(ccm.getColor(8).getRGB()), "ffbf00bf", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(32).getRGB()), "ffbf0000", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(8.5).getRGB()), "ffbf00bf", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(31.5).getRGB()), "ffbf0000", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(7).getRGB()), "ffbf00bf", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(33).getRGB()), "ffbf0000", "");
      Test.ensureEqual(Integer.toHexString(ccm.getColor(Double.NaN).getRGB()), "ff808080", "");

      // speed test
      int n = 1000 * 1000; // bigger than typical map size (convenient for seeing how fast)
      Color c = Color.RED;
      Math2.random(5); // sets up Math2.random

      // warm up loop
      for (int i = 0; i < n; i++) {
        c = ccm.getColor(10 + 10 * Math2.random.nextDouble());
      }

      // timed loop
      long time = System.currentTimeMillis();
      ccm.cumulativeLookupTime = -1; // -1 indicate not used
      ccm.cumulativeTotalTime = -1; // -1 indicate not used
      ccm.cumulativeCount = 0; // 0 indicates not used
      for (int i = 0; i < n; i++) {
        c = ccm.getColor(10 + 10 * Math2.random.nextDouble());
      }
      // See TESTING ON/OFF above
      // Times I see in SgtMap are >2X slower (e.g., 1/4 the count takes 1/2 the time)
      // than times I see here.
      // I think it is because this test all fits in on-chip cache,
      // but real world use doesn't.
      String2.log(
          "  "
              + n
              + " getColors  last="
              + Integer.toHexString(c.getRGB())
              + "\n  cumLookupTime="
              + ccm.cumulativeLookupTime
              + "[was 156 if on, -1=off]"
              + // varies, but testTime more
              // consistent
              "\n  cumTotalTime="
              + ccm.cumulativeTotalTime
              + "[was -1=on, -1=off]"
              + "\n  testTime="
              + (System.currentTimeMillis() - time)
              + "[was 375 if on, 234 if off]\n");
      // so removing all the tracking code makes times 7/9 of timed speed

      // delete the palette
      File2.delete(newPalette);
    }
    /* */

    // test date time
    {
      boolean continuous = false;
      boolean dataIsMillis = false;
      int atLeastNPieces = 5;

      // 90 years -> 9 years
      String2.log("\n* Test CompoundColorMap time = 90 years");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("1906-10-05T12:13:14"),
              Calendar2.isoStringToEpochSeconds("1996-01-15T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(String2.toCSSVString(ccm.leftLabel), "1900, 1920, 1940, 1960, 1980", "");
      Test.ensureEqual(ccm.lastLabel, "2000", "");

      // 30 years -> 5 years
      String2.log("\n* Test CompoundColorMap time = 30 years");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("1971-01-05T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2001-06-15T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel), "1970, 1975, 1980, 1985, 1990, 1995, 2000", "");
      Test.ensureEqual(ccm.lastLabel, "2005", "");

      // 12 years -> 2 years
      String2.log("\n* Test CompoundColorMap time = 12 years");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("1978-09-05T12:13:14"),
              Calendar2.isoStringToEpochSeconds("1990-02-15T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel), "1978, 1980, 1982, 1984, 1986, 1988, 1990", "");
      Test.ensureEqual(ccm.lastLabel, "1992", "");

      // 5 years -> 1 year
      String2.log("\n* Test CompoundColorMap time = 5 years");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-12-05T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2004-03-17T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(String2.toCSSVString(ccm.leftLabel), "2000, 2001, 2002, 2003, 2004", "");
      Test.ensureEqual(ccm.lastLabel, "2005", "");

      // 4 years -> 6 months
      String2.log("\n* Test CompoundColorMap time = 4 years");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-10-05T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2003-04-17T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "Jul<br>2000, Jan<br>2001, Jul, Jan<br>2002, Jul, Jan<br>2003",
          "");
      Test.ensureEqual(ccm.lastLabel, "Jul", "");

      // 11 months -> 2 months
      String2.log("\n* Test CompoundColorMap time = 11 months");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-10-05T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2001-08-17T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel), "Sep<br>2000, Nov, Jan<br>2001, Mar, May, Jul", "");
      Test.ensureEqual(ccm.lastLabel, "Sep", "");

      // 3 months -> 16 days
      String2.log("\n* Test CompoundColorMap time = 3 months");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-09T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2000-09-27T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "01<br>2000-07, 17, 01<br>2000-08, 17, 01<br>2000-09, 17",
          ""); // note no 31
      Test.ensureEqual(ccm.lastLabel, "01<br>2000-10", "");

      // 2 months -> 10 days
      String2.log("\n* Test CompoundColorMap time = 2 months");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-14T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2000-08-30T20:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "11<br>2000-07, 21, 01<br>2000-08, 11, 21",
          ""); // note no 31
      Test.ensureEqual(ccm.lastLabel, "01<br>2000-09", "");

      // 28 days -> 5 days
      String2.log("\n* Test CompoundColorMap time = 28 days");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-09T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2000-08-05T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "06<br>2000-07, 11, 16, 21, 26, 01<br>2000-08",
          ""); // note no 31
      Test.ensureEqual(ccm.lastLabel, "06", "");

      // 10 days -> 2 days
      String2.log("\n* Test CompoundColorMap time = 10 days");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-26T12:13:14"),
              Calendar2.isoStringToEpochSeconds("2000-08-05T08:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "25<br>2000-07, 27, 29, 01<br>2000-08, 03, 05",
          ""); // note no 31
      Test.ensureEqual(ccm.lastLabel, "07", "");

      // 60 hr -> 12 hours
      String2.log("\n* Test CompoundColorMap time = 60 hours");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-09T01:13:14"),
              Calendar2.isoStringToEpochSeconds("2000-07-11T15:09:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "00<br>2000-07-09, 12, 00<br>2000-07-10, 12, 00<br>2000-07-11, 12",
          "");
      Test.ensureEqual(ccm.lastLabel, "00<br>2000-07-12", "");

      // 2 hr -> 15 min
      String2.log("\n* Test CompoundColorMap time = 2 hours");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-11-09T13:14:15"),
              Calendar2.isoStringToEpochSeconds("2000-11-09T14:29:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "00<br>2000-11-09T13, 15, 30, 45, 00<br>2000-11-09T14, 15",
          "");
      Test.ensureEqual(ccm.lastLabel, "30", "");

      // 6 min -> 1 min
      String2.log("\n* Test CompoundColorMap time = 20 min");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-09T01:58:14"),
              Calendar2.isoStringToEpochSeconds("2000-07-09T02:03:10"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "58<br>2000-07-09T01, 59, 00<br>2000-07-09T02, 01, 02, 03",
          "");
      Test.ensureEqual(ccm.lastLabel, "04", "");

      // 58 sec -> 10 sec
      String2.log("\n* Test CompoundColorMap time = 58 sec");
      ccm =
          new CompoundColorMap(
              basePaletteDir,
              "Rainbow",
              dataIsMillis,
              Calendar2.isoStringToEpochSeconds("2000-07-09T01:16:14"),
              Calendar2.isoStringToEpochSeconds("2000-07-09T01:17:12"),
              atLeastNPieces,
              continuous,
              tempDir);
      Test.ensureEqual(
          String2.toCSSVString(ccm.leftLabel),
          "10<br>2000-07-09T01:16, 20, 30, 40, 50, 00<br>2000-07-09T01:17, 10",
          "");
      Test.ensureEqual(ccm.lastLabel, "20", "");

      // 0 sec threw exception
      String2.log("\n* Test range=0");
      ccm =
          new CompoundColorMap(
              basePaletteDir, "Rainbow", false, 1.1806992E9, 1.1806992E9, -1, true, tempDir);
    }
  }
}
