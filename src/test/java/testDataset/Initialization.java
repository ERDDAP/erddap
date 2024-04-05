package testDataset;

import com.cohort.util.File2;

import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.erddap.dataset.EDD;

public class Initialization {
  public static void edStatic() {
    File2.setWebInfParentDirectory();
    System.setProperty("erddapContentDirectory", System.getProperty("user.dir") + "\\content\\erddap");
    System.setProperty("doSetupValidation", String.valueOf(false));
    EDD.debugMode = true;
    
  }

  public static void withSetFonts() {
    edStatic();
    System.setProperty("useSansSerifFont", String.valueOf(true));
    SgtMap.fontFamily = "SansSerif";
  }
}
