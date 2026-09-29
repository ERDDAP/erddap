package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.Test;
import gov.noaa.pfel.erddap.dataset.EDD;

/**
 * Covers .yRange's optional 5th part, the graph's height/width ratio. See
 * https://github.com/ERDDAP/erddap/issues/163
 */
class ImageFilesTests {

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
}
