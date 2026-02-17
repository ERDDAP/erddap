package testDataset;

import com.cohort.util.File2;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import testSupport.WireMockStarter;

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
    EDStatic.messages.sparqlP01toP02pre =
        "http://localhost:"
            + WireMockStarter.port()
            + "/sparql/sparql?query=prefix+skos%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E+prefix+rdf%3A%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+prefix+owl%3A%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+prefix+dc%3A%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E+%0D%0A+%0D%0Aselect+distinct+%28%3Fdci+as+%3FIdentifier%29+%28%3Fpl+as+%3FPrefLabel%29+%28%3Fdefx+as+%3FDefinition%29+%28%3Fver+as+%3FVersion%29+%28%3Fsr+as+%3Frelated%29+%3FDate+%28%3Fdt+as+%3FUrl%29+where+%7B%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FP01%2Fcurrent%2F%3E++skos%3Amember+%3Fdt+.%0D%0AFILTER%28regex%28str%28%3Fdt%29%2C%22";

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
