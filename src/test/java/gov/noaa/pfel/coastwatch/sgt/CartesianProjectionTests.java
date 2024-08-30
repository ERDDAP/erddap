package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.DoubleObject;
import com.cohort.util.String2;
import com.cohort.util.Test;

class CartesianProjectionTests {
  /** This tests this class. */
  @org.junit.jupiter.api.Test
  void basicTest() {
    CartesianProjection cp =
        new CartesianProjection(100, 200, 10, 20, 30, 50, 120, 80, false, false);
    String2.log("\nTest CartesianProjection\n" + cp);
    DoubleObject dox = new DoubleObject(0);
    DoubleObject doy = new DoubleObject(0);
    cp.graphToDevice(100, 10, dox, doy);
    Test.ensureEqual(dox.d, 30, "");
    Test.ensureEqual(doy.d, 120, "");
    cp.graphToDevice(200, 20, dox, doy);
    Test.ensureEqual(dox.d, 50, "");
    Test.ensureEqual(doy.d, 80, "");
    cp.graphToDevice(125, 10, dox, doy);
    Test.ensureEqual(dox.d, 35, "");
    Test.ensureEqual(doy.d, 120, "");
    cp.graphToDevice(100, 12.5, dox, doy);
    Test.ensureEqual(dox.d, 30, "");
    Test.ensureEqual(doy.d, 110, "");

    Test.ensureEqual(cp.graphToDeviceXDistance(25), 5, "");
    Test.ensureEqual(cp.graphToDeviceYDistance(2.5), 10, "");
  }
}
