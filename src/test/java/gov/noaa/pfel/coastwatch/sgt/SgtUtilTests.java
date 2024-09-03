package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

class SgtUtilTests {
  /** This tests SgtUtil. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    // test splitLine
    String2.log("\n*** SgtUtil.basicTest");
    StringArray sa = new StringArray();

    // wide
    sa.clear();
    SgtUtil.splitLine(38, sa, "This is a test of splitline.");
    Test.ensureEqual(sa.size(), 1, "");
    Test.ensureEqual(sa.get(0), "This is a test of splitline.", "");

    // narrow
    sa.clear();
    SgtUtil.splitLine(12, sa, "This is a test of splitline.");
    Test.ensureEqual(sa.size(), 3, "");
    Test.ensureEqual(sa.get(0), "This is a ", "");
    Test.ensureEqual(sa.get(1), "test of ", "");
    Test.ensureEqual(sa.get(2), "splitline.", "");

    // narrow and can't split, so chop at limit
    sa.clear();
    SgtUtil.splitLine(12, sa, "This1is2a3test4of5splitline.");
    Test.ensureEqual(sa.size(), 3, "");
    Test.ensureEqual(sa.get(0), "This1is2a3t", "");
    Test.ensureEqual(sa.get(1), "est4of5split", "");
    Test.ensureEqual(sa.get(2), "line.", "");

    // caps
    sa.clear();
    SgtUtil.splitLine(12, sa, "THESE ARE a a REALLY WIDE.");
    Test.ensureEqual(sa.size(), 3, "");
    Test.ensureEqual(sa.get(0), "THESE ARE ", "");
    Test.ensureEqual(sa.get(1), "a a REALLY ", "");
    Test.ensureEqual(sa.get(2), "WIDE.", "");

    // test SgtUtil.suggestPaletteRange
    Test.ensureEqual(
        SgtUtil.suggestPaletteRange(.3, 8.9), new double[] {0, 10}, ""); // typical Rainbow Linear
    Test.ensureEqual(
        SgtUtil.suggestPaletteRange(.11, 890), new double[] {.1, 1000}, ""); // typical Rainbow Log
    Test.ensureEqual(
        SgtUtil.suggestPaletteRange(-7, 8),
        new double[] {-10, 10},
        ""); // typical BlueWhiteRed Linear
    // symmetric

    // test SgtUtil.suggestPalette
    Test.ensureEqual(
        SgtUtil.suggestPalette(.3, 8.9), "WhiteRedBlack", ""); // small positive, large positive
    Test.ensureEqual(SgtUtil.suggestPalette(300, 890), "Rainbow", ""); // typical Rainbow Log
    Test.ensureEqual(
        SgtUtil.suggestPalette(-7, 8), "BlueWhiteRed", ""); // typical BlueWhiteRed Linear symmetric

    // test SgtUtil.suggestPaletteScale
    Test.ensureEqual(SgtUtil.suggestPaletteScale(.3, 8.9), "Linear", ""); // typical Rainbow Linear
    Test.ensureEqual(SgtUtil.suggestPaletteScale(.11, 890), "Log", ""); // typical Rainbow Log
    Test.ensureEqual(
        SgtUtil.suggestPaletteScale(-7, 8), "Linear", ""); // typical BlueWhiteRed Linear symmetric

    BufferedImage bi =
        ImageIO.read(
            new File(SgtUtilTests.class.getResource("/data/graphs/erdBAssta5day.png").getPath()));
    long time = System.currentTimeMillis();
    Test.ensureEqual(SgtUtil.findGraph(bi), new int[] {24, 334, 150, 21}, "");
    String2.log("findGraph time=" + (System.currentTimeMillis() - time) + "ms");
  }
}
