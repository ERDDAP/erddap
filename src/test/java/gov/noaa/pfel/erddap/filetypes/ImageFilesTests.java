package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.Test;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

/**
 * Covers .yRange's optional 5th part, the graph's height/width ratio. See
 * https://github.com/ERDDAP/erddap/issues/163
 */
class ImageFilesTests {

  private static final String TABLE_QUERY =
      "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03&.draw=markers";
  private static final String GRID_QUERY = "altitude%5B(-90.0):(-88.0)%5D%5B(-180.0):(-178.0)%5D";

  @TempDir static Path dir;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void aspectSetsHeightFromWidth() {
    Test.ensureEqual(ImageFiles.heightForAspect(400, 300, 1), 400, "a square graph");
    Test.ensureEqual(ImageFiles.heightForAspect(400, 300, 0.5), 200, "half as tall as wide");
    Test.ensureEqual(ImageFiles.heightForAspect(400, 300, 2), 800, "twice as tall as wide");
  }

  @org.junit.jupiter.api.Test
  void noAspectLeavesTheHeightAlone() {
    Test.ensureEqual(
        ImageFiles.heightForAspect(400, 300, Double.NaN), 300, "the request asked for no ratio");
  }

  @org.junit.jupiter.api.Test
  void aspectIsRoundedNotTruncated() {
    // 400 * 0.3333 = 133.32
    Test.ensureEqual(ImageFiles.heightForAspect(400, 300, 0.3333), 133, "rounds to nearest");
    // 400 * 0.334 = 133.6
    Test.ensureEqual(ImageFiles.heightForAspect(400, 300, 0.334), 134, "rounds to nearest");
  }

  @org.junit.jupiter.api.Test
  void anExtremeRatioStaysWithinAUsableImage() {
    // MAX_ASPECT on a wide image would ask for far more than WMS_MAX_HEIGHT.
    Test.ensureEqual(
        ImageFiles.heightForAspect(2000, 300, ImageFiles.MAX_ASPECT),
        EDD.WMS_MAX_HEIGHT,
        "clamped to the maximum ERDDAP will serve");
    // MIN_ASPECT on a narrow image rounds toward zero, which is not a drawable image.
    Test.ensureEqual(
        ImageFiles.heightForAspect(10, 300, ImageFiles.MIN_ASPECT), 1, "never smaller than 1");
  }

  // End to end: query parsing through to the image ERDDAP writes, on both saveAsImage paths.

  @org.junit.jupiter.api.Test
  void tableImageTakesItsShapeFromTheAspect() throws Throwable {
    EDDTable table = (EDDTable) EDDTestDataset.gettestGlobecBottle();

    assertImageSize(table, TABLE_QUERY + "&.size=400|300", "tableNoAspect", 400, 300);
    assertImageSize(table, TABLE_QUERY + "&.size=400|300&.yRange=||||0.5", "tableHalf", 400, 200);
    assertImageSize(
        table, TABLE_QUERY + "&.yRange=||||0.5&.size=400|300", "tableAspectFirst", 400, 200);
    assertImageSize(
        table, TABLE_QUERY + "&.size=400|300&.yRange=||||1000", "tableOutOfRange", 400, 300);
  }

  @org.junit.jupiter.api.Test
  void gridImageTakesItsShapeFromTheAspect() throws Throwable {
    EDDGrid grid = (EDDGrid) EDDTestDataset.getetopo180();

    assertImageSize(grid, GRID_QUERY + "&.size=400|300", "gridNoAspect", 400, 300);
    assertImageSize(grid, GRID_QUERY + "&.size=400|300&.yRange=||||2", "gridDouble", 400, 800);
    assertImageSize(grid, GRID_QUERY + "&.yRange=||||2&.size=400|300", "gridAspectFirst", 400, 800);
    assertImageSize(
        grid, GRID_QUERY + "&.size=400|300&.yRange=||||abc", "gridUnparsable", 400, 300);
  }

  private static void assertImageSize(EDD edd, String query, String baseName, int width, int height)
      throws Throwable {
    String fileName = edd.makeNewFileForDapQuery(0, null, null, query, dir + "/", baseName, ".png");
    BufferedImage image = ImageIO.read(new File(dir.toFile(), fileName));
    Test.ensureEqual(image.getWidth(), width, baseName + " width");
    Test.ensureEqual(image.getHeight(), height, baseName + " height");
  }
}
