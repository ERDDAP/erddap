package gov.noaa.pfel.coastwatch;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.SSR;
import tags.TagIncompleteTest;

class BrowserTests {
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // url 404s
  void interactiveTest() throws Exception {
    String2.log("\n*** Browser.basicTest()");
    String testDir = File2.getSystemTempDirectory() + "/browser/";

    String tName = "coverage.kml";
    File2.delete(testDir + tName);
    String url = "https://coastwatch.pfeg.noaa.gov/coastwatch/CWBrowserWW360.jsp?" +
        "get=gridData&dataSet=TMBchla&timePeriod=8day&centeredTime=2006-01-23T00:00:00" +
        "&maxLat=50&minLon=220&maxLon=250&minLat=20&fileType=GoogleEarth";
    SSR.downloadFile(url, testDir + tName, true);
    Test.displayInBrowser("file://" + testDir + tName); // .kml
    // String2.pressEnterToContinue(
    // "Is GoogleEarth showing a coverage? \n" +
    // "Close it, then...");

    // 2020-03-03 This test of CWBrowsers stopped working after I removed NDBCMet
    // data from public browsers
    /*
     * String tName = "station.kml";
     * File2.delete(testDir + tName);
     * SSR.downloadFile(
     * "https://coastwatch.pfeg.noaa.gov/coastwatch/CWBrowserWW360.jsp?" +
     * "get=stationData&dataSet=PNBwtmp&timePeriod=8day&beginTime=2008-08-26T22:00:00"
     * +
     * "&endTime=2008-09-26T22:00:00&minLon=220.0&maxLon=250.0&minLat=20.0&maxLat=50.0"
     * +
     * "&minDepth=0&maxDepth=0&fileType=GoogleEarth",
     * testDir + tName, true);
     * Test.displayInBrowser("file://" + testDir + tName); //.kml
     * String2.pressEnterToContinue(
     * "Is GoogleEarth showing stations? \n" +
     * "Close it, then...");
     * }
     */

  }
}
