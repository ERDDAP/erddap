package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

public class EDDGridFromFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @Test
  void testAccessibleViaFilesLocal() throws Throwable {
    EDDGridFromFiles edd = (EDDGridFromFiles) EDDTestDataset.getnceiPH53sstn1day();
    String file =
        edd.accessibleViaFilesGetLocal(
            0,
            "1994/data/19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc");
    file = file.replace('\\', '/');
    String path =
        Path.of(
                EDDGridFromFilesTests.class
                    .getResource(
                        "/largeSatellite/PH53/1day/1994/data/19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc")
                    .toURI())
            .toString()
            .replace('\\', '/');
    assertEquals(path, file);
  }
}
