package gov.noaa.pmel.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SoTPointTests {

  @Test
  void testBasic() {
    SoTValue xVal = new SoTValue.Double(1.0);
    SoTValue yVal = new SoTValue.Double(2.0);
    SoTPoint point = new SoTPoint(xVal, yVal);

    assertEquals(xVal, point.x());
    assertEquals(yVal, point.y());
  }

  @Test
  void testEqualsAndHashCode() {
    SoTValue x1 = new SoTValue.Double(1.0);
    SoTValue y1 = new SoTValue.Double(2.0);
    SoTPoint p1 = new SoTPoint(x1, y1);

    SoTValue x2 = new SoTValue.Double(1.0);
    SoTValue y2 = new SoTValue.Double(2.0);
    SoTPoint p2 = new SoTPoint(x2, y2);

    SoTValue x3 = new SoTValue.Double(3.0);
    SoTValue y3 = new SoTValue.Double(4.0);
    SoTPoint p3 = new SoTPoint(x3, y3);

    assertEquals(p1, p2);
    assertNotEquals(p1, p3);
    assertEquals(p1.hashCode(), p2.hashCode());
  }
}
