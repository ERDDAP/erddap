package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EDDTableFromErddapUnitTests {
  @Test
  void removesOnlyTrailingNccsvTransportConvention() {
    assertEquals(
        "IOOS-1.2, CF-1.6, ACDD-1.3",
        EDDTableFromErddap.removeNccsvConventionSuffix("IOOS-1.2, CF-1.6, ACDD-1.3, NCCSV-1.2"));
    assertEquals(
        "IOOS-1.2, CF-1.6",
        EDDTableFromErddap.removeNccsvConventionSuffix("IOOS-1.2, CF-1.6, NCCSV-2.10, "));
  }

  @Test
  void preservesNonTransportConventions() {
    assertEquals(
        "IOOS-1.2, NCCSV-1.2, CF-1.6",
        EDDTableFromErddap.removeNccsvConventionSuffix("IOOS-1.2, NCCSV-1.2, CF-1.6"));
    assertEquals(
        "IOOS-1.2, CF-1.6",
        EDDTableFromErddap.removeNccsvConventionSuffix(" , IOOS-1.2, CF-1.6, "));
    assertNull(EDDTableFromErddap.removeNccsvConventionSuffix(null));
  }
}
