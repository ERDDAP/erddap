package testDataset;

import com.cohort.util.File2;

import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.erddap.dataset.EDD;

public class Initialization {
  public static void edStatic() {
    File2.setWebInfParentDirectory();
    System.setProperty("erddapContentDirectory", System.getProperty("user.dir") + "/development/jetty/config/");
    System.setProperty("skipEmailThread", String.valueOf(true));
    System.setProperty("allowDeferedLoading", String.valueOf(false));
    EDD.debugMode = true;
    SgtMap.fontFamily = "SansSerif";
  }
}
