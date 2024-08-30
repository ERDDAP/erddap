/*
 * Touch Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.array.StringArray;
import com.cohort.util.*;
import gov.noaa.pfel.coastwatch.util.SSR;

/**
 * This class has static methods which call various urls to ensure they respond.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-02-08
 */
public class Touch {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * This ensures that the url response includes "Dataset {".
   *
   * @param urlEnd the end of the url (already percentEncoded as needed)
   */
  private static void tThredds(String urlEnd) throws Exception {
    String base = "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/";
    String find = "Dataset {";
    String2.log("  touching " + base + urlEnd);
    String response = SSR.getUrlResponseStringUnchanged(base + urlEnd);
    Test.ensureTrue(
        response.indexOf(find) >= 0,
        String2.ERROR
            + " in Touch.thredds while reading "
            + base
            + urlEnd
            + ":\nfind=\""
            + find
            + "\" not found in response=\n"
            + response);
  }

  /**
   * This touches many of the oceanwatch THREDDS datasets to ensure they are available and
   * information about the files is in the thredds cache. (this took 49 minutes to run)
   *
   * <p>I'm not convinced that thredds caches remote datasets (like PISCO) the same way as local
   * data sets. If I visit one of these pages (it's very slow), wait a minute then go back (it's
   * fast), wait 20 minutes and go back (it's slow).
   *
   * @throws Exception if trouble
   */
  public static void thredds() throws Exception {
    String2.log("\n*** Touch.thredds");
    long time = System.currentTimeMillis();

    tThredds("PISCO/adcp/2004_BAY_15m.html");
    tThredds("PISCO/adcp/2005_BAY_15m.html");
    tThredds("PISCO/adcp/2006_BAY_15m.html");
    tThredds("PISCO/adcp/2005_BEA_15m.html");
    tThredds("PISCO/adcp/2006_BEA_15m.html");
    tThredds("PISCO/adcp/2004_PEL_15m.html");
    tThredds("PISCO/adcp/2005_PEL_15m.html");
    tThredds("PISCO/adcp/2006_PEL_15m.html");
    tThredds("PISCO/adcp/2004_SMS_15m.html");
    tThredds("PISCO/adcp/2005_SMS_15m.html");
    tThredds("PISCO/adcp/2006_SMS_15m.html");
    tThredds("PISCO/adcp/2005_SRS_15m.html");
    tThredds("PISCO/adcp/2006_SRS_15m.html");
    tThredds("PISCO/adcp/2004_VAL_15m.html");
    tThredds("PISCO/adcp/2005_VAL_15m.html");
    tThredds("PISCO/adcp/2006_VAL_15m.html");
    tThredds("PISCO/temp/2005_ANO001_0m.html");
    tThredds("PISCO/temp/2005_ANO001_4m.html");
    tThredds("PISCO/temp/2005_ANO001_12m.html");
    tThredds("PISCO/temp/2005_ANO001_20m.html");
    tThredds("PISCO/temp/2006_ANO001_4m.html");
    tThredds("PISCO/temp/2006_ANO001_12m.html");
    tThredds("PISCO/temp/2006_ANO001_20m.html");
    tThredds("PISCO/temp/2005_BAY_3m.html");
    tThredds("PISCO/temp/2005_BAY_9m.html");
    tThredds("PISCO/temp/2005_BAY_14m.html");
    tThredds("PISCO/temp/2006_BAY_3m.html");
    tThredds("PISCO/temp/2006_BAY_9m.html");
    tThredds("PISCO/temp/2006_BAY_14m.html");
    tThredds("PISCO/temp/2005_BEA_3m.html");
    tThredds("PISCO/temp/2005_BEA_9m.html");
    tThredds("PISCO/temp/2005_BEA_14m.html");
    tThredds("PISCO/temp/2006_BEA_3m.html");
    tThredds("PISCO/temp/2006_BEA_9m.html");
    tThredds("PISCO/temp/2006_BEA_14m.html");
    tThredds("PISCO/temp/2005_BIG001_0m.html");
    tThredds("PISCO/temp/2005_BIG001_4m.html");
    tThredds("PISCO/temp/2005_BIG001_15m.html");
    tThredds("PISCO/temp/2005_BIG001_25m.html");
    tThredds("PISCO/temp/2006_BIG001_0m.html");
    tThredds("PISCO/temp/2006_BIG001_4m.html");
    tThredds("PISCO/temp/2006_BIG001_15m.html");
    tThredds("PISCO/temp/2006_BIG001_25m.html");
    tThredds("PISCO/temp/2005_ESA001_0m.html");
    tThredds("PISCO/temp/2005_ESA001_6m.html");
    tThredds("PISCO/temp/2005_ESA001_15m.html");
    tThredds("PISCO/temp/2005_ESA001_24m.html");
    tThredds("PISCO/temp/2006_ESA001_0m.html");
    tThredds("PISCO/temp/2006_ESA001_6m.html");
    tThredds("PISCO/temp/2006_ESA001_15m.html");
    tThredds("PISCO/temp/2006_ESA001_24m.html");
    tThredds("PISCO/temp/2005_JOE001_0m.html");
    tThredds("PISCO/temp/2005_JOE001_5m.html");
    tThredds("PISCO/temp/2005_JOE001_14m.html");
    tThredds("PISCO/temp/2005_JOE001_22m.html");
    tThredds("PISCO/temp/2006_JOE001_0m.html");
    tThredds("PISCO/temp/2006_JOE001_5m.html");
    tThredds("PISCO/temp/2006_JOE001_14m.html");
    tThredds("PISCO/temp/2006_JOE001_22m.html");
    tThredds("PISCO/temp/2005_LAC001_0m.html");
    tThredds("PISCO/temp/2005_LAC001_4m.html");
    tThredds("PISCO/temp/2005_LAC001_13m.html");
    tThredds("PISCO/temp/2005_LAC001_23m.html");
    tThredds("PISCO/temp/2006_LAC001_0m.html");
    tThredds("PISCO/temp/2006_LAC001_4m.html");
    tThredds("PISCO/temp/2006_LAC001_13m.html");
    tThredds("PISCO/temp/2006_LAC001_23m.html");
    tThredds("PISCO/temp/2005_LOP001_0m.html");
    tThredds("PISCO/temp/2005_LOP001_4m.html");
    tThredds("PISCO/temp/2005_LOP001_15m.html");
    tThredds("PISCO/temp/2005_LOP001_25m.html");
    tThredds("PISCO/temp/2006_LOP001_0m.html");
    tThredds("PISCO/temp/2006_LOP001_4m.html");
    tThredds("PISCO/temp/2006_LOP001_15m.html");
    tThredds("PISCO/temp/2006_LOP001_25m.html");
    tThredds("PISCO/temp/2004_PEL_3m.html");
    tThredds("PISCO/temp/2004_PEL_9m.html");
    tThredds("PISCO/temp/2004_PEL_14m.html");
    tThredds("PISCO/temp/2005_PEL_3m.html");
    tThredds("PISCO/temp/2005_PEL_9m.html");
    tThredds("PISCO/temp/2005_PEL_14m.html");
    tThredds("PISCO/temp/2006_PEL_3m.html");
    tThredds("PISCO/temp/2006_PEL_9m.html");
    tThredds("PISCO/temp/2006_PEL_14m.html");
    tThredds("PISCO/temp/2005_PIG001_0m.html");
    tThredds("PISCO/temp/2005_PIG001_5m.html");
    tThredds("PISCO/temp/2005_PIG001_13m.html");
    tThredds("PISCO/temp/2005_PIG001_20m.html");
    tThredds("PISCO/temp/2006_PIG001_0m.html");
    tThredds("PISCO/temp/2006_PIG001_5m.html");
    tThredds("PISCO/temp/2006_PIG001_13m.html");
    tThredds("PISCO/temp/2006_PIG001_20m.html");
    tThredds("PISCO/temp/2004_SEF001_4m.html");
    tThredds("PISCO/temp/2004_SEF001_13m.html");
    tThredds("PISCO/temp/2004_SMS_3m.html");
    tThredds("PISCO/temp/2004_SMS_9m.html");
    tThredds("PISCO/temp/2004_SMS_14m.html");
    tThredds("PISCO/temp/2005_SMS_3m.html");
    tThredds("PISCO/temp/2005_SMS_9m.html");
    tThredds("PISCO/temp/2005_SMS_14m.html");
    tThredds("PISCO/temp/2006_SMS_3m.html");
    tThredds("PISCO/temp/2006_SMS_9m.html");
    tThredds("PISCO/temp/2006_SMS_14m.html");
    tThredds("PISCO/temp/2004_SRS_3m.html");
    tThredds("PISCO/temp/2004_SRS_9m.html");
    tThredds("PISCO/temp/2004_SRS_14m.html");
    tThredds("PISCO/temp/2005_SRS_3m.html");
    tThredds("PISCO/temp/2005_SRS_9m.html");
    tThredds("PISCO/temp/2005_SRS_14m.html");
    tThredds("PISCO/temp/2006_SRS_3m.html");
    tThredds("PISCO/temp/2006_SRS_9m.html");
    tThredds("PISCO/temp/2006_SRS_14m.html");
    tThredds("PISCO/temp/2005_SUN001_0m.html");
    tThredds("PISCO/temp/2005_SUN001_5m.html");
    tThredds("PISCO/temp/2005_SUN001_15m.html");
    tThredds("PISCO/temp/2005_SUN001_25m.html");
    tThredds("PISCO/temp/2006_SUN001_0m.html");
    tThredds("PISCO/temp/2006_SUN001_5m.html");
    tThredds("PISCO/temp/2006_SUN001_15m.html");
    tThredds("PISCO/temp/2006_SUN001_25m.html");
    tThredds("PISCO/temp/2003_TPT001_16m.html");
    tThredds("PISCO/temp/2005_TPT007_0m.html");
    tThredds("PISCO/temp/2005_TPT007_5m.html");
    tThredds("PISCO/temp/2005_TPT007_10m.html");
    tThredds("PISCO/temp/2005_TPT007_20m.html");
    tThredds("PISCO/temp/2005_TPT007_40m.html");
    tThredds("PISCO/temp/2005_TPT007_60m.html");
    tThredds("PISCO/temp/2006_TPT007_0m.html");
    tThredds("PISCO/temp/2006_TPT007_5m.html");
    tThredds("PISCO/temp/2006_TPT007_10m.html");
    tThredds("PISCO/temp/2006_TPT007_20m.html");
    tThredds("PISCO/temp/2006_TPT007_40m.html");
    tThredds("PISCO/temp/2006_TPT007_60m.html");
    tThredds("PISCO/temp/2005_TPT008_0m.html");
    tThredds("PISCO/temp/2005_TPT008_6m.html");
    tThredds("PISCO/temp/2005_TPT008_10m.html");
    tThredds("PISCO/temp/2005_TPT008_20m.html");
    tThredds("PISCO/temp/2005_TPT008_40m.html");
    tThredds("PISCO/temp/2005_TPT008_60m.html");
    tThredds("PISCO/temp/2005_TPT008_99m.html");
    tThredds("PISCO/temp/2006_TPT008_0m.html");
    tThredds("PISCO/temp/2006_TPT008_6m.html");
    tThredds("PISCO/temp/2006_TPT008_10m.html");
    tThredds("PISCO/temp/2006_TPT008_20m.html");
    tThredds("PISCO/temp/2006_TPT008_40m.html");
    tThredds("PISCO/temp/2006_TPT008_60m.html");
    tThredds("PISCO/temp/2006_TPT008_99m.html");
    tThredds("PISCO/temp/2004_VAL_3m.html");
    tThredds("PISCO/temp/2004_VAL_9m.html");
    tThredds("PISCO/temp/2004_VAL_14m.html");
    tThredds("PISCO/temp/2005_VAL_3m.html");
    tThredds("PISCO/temp/2005_VAL_9m.html");
    tThredds("PISCO/temp/2005_VAL_14m.html");
    tThredds("PISCO/temp/2006_VAL_3m.html");
    tThredds("PISCO/temp/2006_VAL_9m.html");
    tThredds("PISCO/temp/2006_VAL_14m.html");
    tThredds("PISCO/temp/2005_WES001_0m.html");
    tThredds("PISCO/temp/2005_WES001_11m.html");
    tThredds("PISCO/temp/2005_WES001_29m.html");
    tThredds("PISCO/temp/2006_WES001_0m.html");
    tThredds("PISCO/temp/2006_WES001_5m.html");
    tThredds("PISCO/temp/2006_WES001_11m.html");
    tThredds("PISCO/temp/2006_WES001_21m.html");
    tThredds("PISCO/temp/2006_WES001_29m.html");

    String2.log(
        "Touch.thredds done. TIME="
            + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
            + "ms");
  }

  /** One time use: This generates a list of pisco urls. */
  public static void getPiscoUrls() throws Exception {
    // get the main catlog
    String cat =
        SSR.getUrlResponseStringUnchanged("https://oceanwatch.pfeg.noaa.gov/thredds/catalog.html");
    // String2.log(cat);

    // extract all pisco urls
    String second[] = String2.extractAllRegexes(cat, "http:.+?PISCO.+?xml");
    String2.log("second.length=" + second.length);

    // for each of those
    StringArray sa = new StringArray();
    for (int i = 0; i < second.length; i++) {
      String s = second[i];
      // String2.log("s=" + s);

      // print all ...urls
      cat = SSR.getUrlResponseStringUnchanged(second[i]);
      // String2.log(cat);

      String third[] = String2.extractAllRegexes(cat, "ID=\".+?\"");
      sa.append(new StringArray(third));
    }
    String2.log(sa.toNewlineString());
  }
}
