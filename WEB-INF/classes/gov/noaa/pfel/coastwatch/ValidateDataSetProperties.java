/*
 * ValidateDataSetPropterties Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.io.Resources;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

/**
 * This class is designed to be a stand-alone program to validate that the DataSet.properties file
 * contains valid information for all datasets listed by validDataSets in DataSet.properties. Don't
 * run this on the coastwatch computer.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-10-06
 */
public class ValidateDataSetProperties {

  public static final int N_DUMMY_GRID_DATASETS = 2;

  /**
   * This class is designed to be a stand-alone program to validate that the DataSet.properties file
   * contains valid information for all datasets listed by validDataSets in DataSet.properties.
   * Don't run this on the coastwatch computer.
   *
   * @param args is ignored
   */
  public static void main(String args[]) throws Exception {

    doIt();
  }

  public static void doIt() throws Exception {

    String2.log("ValidateDataSetProperties (testing DataSet.properties validDataSets");

    // find a browser properties file (e.g., CWBrowser.properties)
    String resourcePath = Resources.getResource("gov/noaa/pfel/coastwatch/").toString();
    String contextDirectory = File2.accessResourceFile(resourcePath);
    String[] propList = RegexFilenameFilter.list(contextDirectory, ".+\\.properties");
    int which = -1;
    for (int i = 0; i < propList.length; i++) {
      if (!propList[i].equals("DataSet.properties")) {
        which = i;
        break;
      }
    }
    Test.ensureNotEqual(
        which,
        -1,
        String2.ERROR
            + ": No non-DataSet.properties properties files found in\n"
            + "WEB-INF/classes/gov/noaa/pfel/coastwatch/.\n"
            + ".properties files="
            + String2.toCSSVString(propList));

    FileNameUtility fnu =
        new FileNameUtility(
            "gov.noaa.pfel.coastwatch." + File2.getNameNoExtension(propList[which]));
    ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");
    String infoUrlBaseUrl = dataSetRB2.getString("infoUrlBaseUrl", null);
    String tDataSetList[] = String2.split(dataSetRB2.getString("validDataSets", null), '`');
    int nDataSets = tDataSetList.length;
    // Test.ensureEqual(nDataSets, 228, "nDataSets"); //useful to me, but Dave can't add dataset
    // without recompiling this
    String2.log("  testing " + nDataSets + " data sets");
    boolean excessivelyStrict = true;
    for (int i = ValidateDataSetProperties.N_DUMMY_GRID_DATASETS;
        i < nDataSets;
        i++) { // "2" in order to skip 0=OneOf.NO_DATA, 1=BATHYMETRY
      String seven = tDataSetList[i];
      Test.ensureTrue(seven != null && seven.length() > 0, "  tDataSetList[" + i + "] is ''.");
      fnu.ensureValidDataSetProperties(seven, excessivelyStrict);
      String infoUrl = dataSetRB2.getString(seven + "InfoUrl", null);
      Test.ensureNotNull(infoUrl, seven + "InfoUrl is null.");
      SSR.getUrlResponseArrayList(
          infoUrlBaseUrl
              + infoUrl); // on all computers except coastwatch, all are accessible as urls
    }
    String2.log(
        "  ValidateDataSetProperties successfully tested n="
            + nDataSets
            + " last="
            + tDataSetList[nDataSets - 1]);
  }

  public static void ensureDatasetInDataSetProperties() throws Exception {
    // ensure all of the datasets used in each browser are in DataSet.properties validDataSets.
    String propNames[] = {
      "CWBrowser", "CWBrowserAK", "CWBrowserSA", "CWBrowserWW180", "CWBrowserWW360", "CWBrowserHAB"
    };
    StringArray validDataSets = null;
    for (int pni = 0; pni < propNames.length; pni++) {
      String2.log("\nTesting " + propNames[pni]);
      FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch." + propNames[pni]);
      String tDataSetList[] = String2.split(fnu.classRB2().getString("dataSetList", null), '`');
      int nDataSets = tDataSetList.length;
      if (validDataSets == null) {
        String ts = fnu.dataSetRB2().getString("validDataSets", null);
        String[] tsa = String2.split(ts, '`');
        validDataSets = new StringArray(tsa);
      }
      for (int i = ValidateDataSetProperties.N_DUMMY_GRID_DATASETS;
          i < nDataSets;
          i++) { // "2" in order to skip 0=OneOf.NO_DATA and 1=BATHYMETRY
        if (validDataSets.indexOf(tDataSetList[i], 0) == -1) {
          Test.error(
              "In "
                  + propNames[pni]
                  + ".properties, ["
                  + i
                  + "]="
                  + tDataSetList[i]
                  + " not found in DataSet.properties validDataSets:\n"
                  + validDataSets);
        }
      }
    }
  }

  /**
   * This runs all of the interactive or not interactive tests for this class.
   *
   * @param errorSB all caught exceptions are logged to this.
   * @param interactive If true, this runs all of the interactive tests; otherwise, this runs all of
   *     the non-interactive tests.
   * @param doSlowTestsToo If true, this runs the slow tests, too.
   * @param firstTest The first test to be run (0...). Test numbers may change.
   * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). Test
   *     numbers may change.
   */
  public static void test(
      StringBuilder errorSB,
      boolean interactive,
      boolean doSlowTestsToo,
      int firstTest,
      int lastTest) {
    if (lastTest < 0) lastTest = interactive ? -1 : 1;
    String msg = "\n^^^ ValidateDatasetProperties.test(" + interactive + ") test=";

    for (int test = firstTest; test <= lastTest; test++) {
      try {
        long time = System.currentTimeMillis();
        String2.log(msg + test);

        if (interactive) {
          // if (test ==  0) ...;

        } else {
          if (test == 0 && doSlowTestsToo) doIt();
          if (test == 1) ensureDatasetInDataSetProperties();
        }

        String2.log(
            msg
                + test
                + " finished successfully in "
                + (System.currentTimeMillis() - time)
                + " ms.");
      } catch (Throwable testThrowable) {
        String eMsg = msg + test + " caught throwable:\n" + MustBe.throwableToString(testThrowable);
        errorSB.append(eMsg);
        String2.log(eMsg);
        if (interactive) String2.pressEnterToContinue("");
      }
    }
  }
}
