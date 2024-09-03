package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pmel.sgt.dm.SGTLine;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class BoundariesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This runs a unit test. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    // verbose = true;

    String2.log("\n*** Boundaries.basicTest");

    Boundaries nationalBoundaries = Boundaries.getNationalBoundaries();
    Boundaries stateBoundaries = Boundaries.getStateBoundaries();
    SGTLine sgtLine;

    // warmup
    sgtLine = nationalBoundaries.getSgtLine(2, -134, -105, 22, 49);

    // *** national
    // force creation of new file
    long time = System.currentTimeMillis();
    sgtLine = nationalBoundaries.getSgtLine(2, -135, -105, 22, 50);
    String2.log("create time=" + (System.currentTimeMillis() - time) + "ms (was 62)");

    // read cached version
    time = System.currentTimeMillis();
    sgtLine = nationalBoundaries.getSgtLine(2, -135, -105, 22, 50);
    time = System.currentTimeMillis() - time;
    String2.log("cache time=" + time + "ms");

    // is it the same (is SGTLine.equals a deep test? probably not)

    // test speed
    Test.ensureTrue(time < 20, "time=" + time + "ms");

    // *** state
    // force creation of new file
    sgtLine = stateBoundaries.getSgtLine(2, -135, -105, 22, 50);

    // read cached version
    time = System.currentTimeMillis();
    sgtLine = stateBoundaries.getSgtLine(2, -135, -105, 22, 50);
    time = System.currentTimeMillis() - time;

    // is it the same (is SGTLine.equals a deep test? probably not)

    // test speed
    Test.ensureTrue(time < 20, "time=" + time);

    String2.log(nationalBoundaries.statsString() + "\n" + stateBoundaries.statsString());
  }
}
