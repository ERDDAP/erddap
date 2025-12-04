package testDataset;

import com.cohort.util.File2;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;

public class Initialization {
  public static void edStatic() {
    File2.setWebInfParentDirectory(System.getProperty("user.dir") + "/");
    System.setProperty(
        "erddapContentDirectory", System.getProperty("user.dir") + "/development/test/");
    System.setProperty("skipEmailThread", String.valueOf(true));
    EDStatic.init(System.getProperty("user.dir") + "/");
    EDD.debugMode = false;
    SgtMap.fontFamily = "SansSerif";
    EDStatic.config.useSaxParser = true;
    EDStatic.testingDontDestroy = true;

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    System.out.println("Cleaning up files on JVM shutdown.");
                    cleanup();
                  } catch (Exception ignored) {
                    System.out.println("Error deleting files");
                  }
                }));
  }

  public static void cleanup() {
    File2.delete(EDStatic.config.imageDir + "/nlogo2.gif");
    File2.delete(EDStatic.config.imageDir + "/noaa_otherName.gif");
    File2.delete(EDStatic.config.imageDir + "/noaa2000.gif");
    File2.delete(EDStatic.config.imageDir + "/QuestionMarkTest.png");
  }
}
