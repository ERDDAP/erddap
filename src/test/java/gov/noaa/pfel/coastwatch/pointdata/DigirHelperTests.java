package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import com.cohort.util.Test;

class DigirHelperTests {
  /**
   * This tests the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {

    String2.log("\n***** DigirHelper.basicTest");
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // verbose = true;
    // reallyVerbose = true;

    /*
     * //one time things
     * String2.log(String2.noLongLinesAtSpace(
     * "Available genera (and number of records): " + getObisInventoryString(
     * "http://aadc-maps.aad.gov.au/digir/digir.php",
     * "argos_tracking",
     * "darwin:ScientificName"), //"darwin:Genus"),
     * 72, ""));
     * if (true) System.exit(0);
     * /*
     */

    // test parseQuery
    StringArray resultsVariables = new StringArray();
    StringArray filterVariables = new StringArray();
    StringArray filterCops = new StringArray();
    StringArray filterValues = new StringArray();
    String query = "darwin:Longitude,darwin:Latitude&darwin:Genus!=Bob" +
        "&darwin:Genus~=%rocystis&darwin:Latitude<=54&darwin:Latitude>=53" +
        "&darwin:Longitude=0&darwin:Latitude<78&darwin:Latitude>77" +
        "&darwin:Species in option1,option2,option3";
    DigirHelper.parseQuery(query, resultsVariables, filterVariables, filterCops, filterValues);
    Test.ensureEqual(resultsVariables.toString(), "darwin:Longitude, darwin:Latitude", "");
    Test.ensureEqual(filterVariables.toString(),
        "darwin:Genus, darwin:Genus, darwin:Latitude, darwin:Latitude, " +
            "darwin:Longitude, darwin:Latitude, darwin:Latitude, " +
            "darwin:Species",
        "");
    Test.ensureEqual(filterCops.toString(),
        String2.toCSSVString(DigirHelper.COP_NAMES), "");
    Test.ensureEqual(filterValues.toString(),
        "Bob, %rocystis, 54, 53, 0, 78, 77, \"option1,option2,option3\"", "");

    // test getOpendapConstraint
    filterCops.set(7, "in");
    Test.ensureEqual(DigirHelper.getOpendapConstraint(resultsVariables.toArray(),
        filterVariables.toArray(), DigirHelper.COP_NAMES, filterValues.toArray()),
        query, "");

    // This works, but takes a long time and isn't directly used by
    // the methods which get obis data, so don't test all the time.
    // testGetMetadata();

    // 2014-08-06 REMOVED dataset no longer available: testGetInventory();
    // 2014-08-06 REMOVED dataset no longer available: testObis();
    // 2014-08-06 REMOVED dataset no longer available: testOpendapStyleObis();
    DigirHelper.testBmde();

    // done
    String2.log("\n***** DigirHelper.test finished successfully");

  }
}
