package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import tags.TagExternalOther;
import tags.TagSlowTests;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

class DigirHelperTests {
  /**
   * This tests the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagSlowTests
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
    String query =
        "darwin:Longitude,darwin:Latitude&darwin:Genus!=Bob"
            + "&darwin:Genus~=%rocystis&darwin:Latitude<=54&darwin:Latitude>=53"
            + "&darwin:Longitude=0&darwin:Latitude<78&darwin:Latitude>77"
            + "&darwin:Species in option1,option2,option3";
    DigirHelper.parseQuery(query, resultsVariables, filterVariables, filterCops, filterValues);
    Test.ensureEqual(resultsVariables.toString(), "darwin:Longitude, darwin:Latitude", "");
    Test.ensureEqual(
        filterVariables.toString(),
        "darwin:Genus, darwin:Genus, darwin:Latitude, darwin:Latitude, "
            + "darwin:Longitude, darwin:Latitude, darwin:Latitude, "
            + "darwin:Species",
        "");
    Test.ensureEqual(filterCops.toString(), String2.toCSSVString(DigirHelper.COP_NAMES), "");
    Test.ensureEqual(
        filterValues.toString(),
        "Bob, %rocystis, 54, 53, 0, 78, 77, \"option1,option2,option3\"",
        "");

    // test getOpendapConstraint
    filterCops.set(7, "in");
    Test.ensureEqual(
        DigirHelper.getOpendapConstraint(
            resultsVariables.toArray(),
            filterVariables.toArray(),
            DigirHelper.COP_NAMES,
            filterValues.toArray()),
        query,
        "");

    // This works, but takes a long time and isn't directly used by
    // the methods which get obis data, so don't test all the time.
    // testGetMetadata();

    // 2014-08-06 REMOVED dataset no longer available: testGetInventory();
    // 2014-08-06 REMOVED dataset no longer available: testObis();
    // 2014-08-06 REMOVED dataset no longer available: testOpendapStyleObis();
    testBmde();

    // done
    String2.log("\n***** DigirHelper.test finished successfully");
  }

  /**
   * This tests getMetadata.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther // error in response (meta tag not closed)
  void testGetMetadata() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // String2.log("\n*** DigirHelper.testGetMetadata");
    Table table;

    // test rutgers_obis
    if (false) {
      // this used to work and probably still does; but I have stopped testing rutgers
      // because it is often down.
      table = DigirHelper.getMetadataTable(DigirHelper.RUTGERS_OBIS_URL, DigirHelper.OBIS_VERSION);
      String2.log("metadata table=" + table.toString(10));
      Test.ensureTrue(table.nRows() >= 142, "nRows=" + table.nRows());
      Test.ensureEqual(table.getColumnName(0), "name", "");
      Test.ensureEqual(table.getColumnName(1), "code", "");
      Test.ensureEqual(table.getColumnName(2), "relatedInformation", "");
      Test.ensureEqual(table.getColumnName(3), "contact/emailAddress", "");
      Test.ensureEqual(table.getStringData(0, 0), "EPA'S EMAP Database", "");
      Test.ensureEqual(table.getStringData(1, 0), "EMAP", "");
      Test.ensureEqual(table.getStringData(1, 1), "HMAP", "");
      Test.ensureEqual(table.getStringData(1, 2), "NODC", "");
      Test.ensureEqual(
          table.getStringData(2, 0), "URL that provides more information about this resource", "");
      Test.ensureEqual(table.getStringData(3, 0), "hale.stephen@epa.gov", "");
      Test.ensureEqual(table.getStringData(4, 0), "401-782-3048", "");

      // test getting code column (mentioned in getMetadataTable docs
      String codes[] = ((StringArray) table.findColumn("code")).toArray();
      String2.log("codes=" + String2.toCSSVString(codes));
      Test.ensureTrue(codes.length >= 142, "codes.length=" + codes.length);
      Test.ensureTrue(String2.indexOf(codes, "GHMP") >= 0, "GHMP not found.");
      Test.ensureTrue(String2.indexOf(codes, "EMAP") >= 0, "EMAP not found.");
      Test.ensureTrue(String2.indexOf(codes, "iziko Fish") >= 0, "iziko Fish not found.");
    }

    // test ind_obis
    if (false) {
      // Row name code contact/name contact/title contact/emailA contact/phone
      // contact2/name contact2/title contact2/email contact2/phone
      // abstract keywords citation conceptualSche recordIdentifi numberOfRecord
      // dateLastUpdate minQueryTermLe maxSearchRespo maxInventoryRe
      // 0 IndOBIS, India indobis Vishwas Chavan Scientist vs.chavan@ncl. 91 20 2590
      // 248 Asavari Navlak Technical Offi ar.navlakhe@nc 91 20 2590 248 IndOBIS
      // (India Indian Ocean, Chavan, VIshwa http://digir.n sciname 41880
      // 2007-06-21T02: 3 100 10000
      // 1 Biological Col NIOCOLLECTION Achuthankutty, Coordinator, B achu@nio.org
      // http://digir.n sciname 803 2006-11-03 3 10000 10000
      table = DigirHelper.getMetadataTable(DigirHelper.IND_OBIS_URL, DigirHelper.OBIS_VERSION);
      String2.log("metadata table=" + table.toString(10));
      Test.ensureTrue(table.nRows() >= 2, "nRows=" + table.nRows());
      Test.ensureEqual(table.getColumnName(0), "name", "");
      Test.ensureEqual(table.getColumnName(1), "code", "");
      Test.ensureEqual(table.getColumnName(2), "contact/name", "");
      Test.ensureEqual(table.getColumnName(3), "contact/title", "");
      Test.ensureEqual(table.getStringData(0, 0), "IndOBIS, Indian Ocean Node of OBIS", "");
      Test.ensureEqual(table.getStringData(1, 0), "indobis", "");
      Test.ensureEqual(table.getStringData(2, 0), "Vishwas Chavan", "");
      Test.ensureEqual(table.getStringData(1, 1), "NIOCOLLECTION", "");
    }

    // test flanders
    if (true) {
      // Row name code relatedInforma contact/name contact/title contact/emailA
      // contact/phone contact2/name contact2/title contact2/email contact2/phone
      // abstract citation useRestriction conceptualSche conceptualSche recordIdentifi
      // recordBasis numberOfRecord dateLastUpdate minQueryTermLe maxSearchRespo
      // maxInventoryRe contact3/name contact3/email keywords conceptualSche
      // contact3/title contact3/phone
      // 0 Taxonomic Info tisbe http://www.vli Edward Vanden Manager VMDC
      // wardvdb@vliz.b +32 59 342130 Bart Vanhoorne IT Staff Membe bartv_at_vliz_+32
      // 59 342130 Biogeographica Vanden Berghe, Data are freel http://www.iob
      // http://digir.n Tisbe O 24622 2007-06-12 10: 0 1000 10000
      // 1 Benthic fauna pechorasea http://www.mar Dahle, Salve Data owner
      // sd@akvaplan.ni +47-(0)77-75 0 Cochrane, Sabi Coordinator Bi
      // sc@akvaplan.ni+47-777-50327 Quantitative s Release with p http://www.iob
      // http://digir.n PechoraSea O 1324 2004-09-02 18: 0 1000 10000 Denisenko, Sta
      // dest@unitel.sp Benthic fauna
      // 2 N3 data of Kie n3data http://www.mar Rumohr, Heye hrumohr@ifm-ge
      // +49-(0)431-600 Release with p http://www.iob http://digir.n N3Data O 8944
      // 2005-11-22 17: 0 1000 10000 Benthic fauna,
      table = DigirHelper.getMetadataTable(DigirHelper.FLANDERS_OBIS_URL, DigirHelper.OBIS_VERSION);
      String2.log("metadata table=" + table.toString(10));
      Test.ensureTrue(table.nRows() >= 37, "nRows=" + table.nRows());
      Test.ensureEqual(table.getColumnName(0), "name", "");
      Test.ensureEqual(table.getColumnName(1), "code", "");
      Test.ensureEqual(table.getColumnName(2), "relatedInformation", "");
      Test.ensureEqual(table.getColumnName(3), "contact/name", "");
      Test.ensureEqual(
          table.getStringData(0, 0),
          "Taxonomic Information Sytem for the Belgian coastal area",
          "");
      Test.ensureEqual(table.getStringData(1, 0), "tisbe", "");
      Test.ensureEqual(table.getStringData(2, 0), "http://www.vliz.be/vmdcdata/tisbe", "");
      Test.ensureEqual(table.getStringData(1, 1), "pechorasea", "");
    }
  }

  /**
   * This tests getInventory.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther // error in response (meta tag not closed)
  void testGetInventory() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    Table table;

    // Test that "diagnostic" exception is thrown if error in request
    if (false) {
      // this worked, but I have stopped testing rutgers_obis and
      // stopped doing tests of things that don't work -- don't stress the servers
      // String2.log("\n*** DigirHelper.testGetInventory ensure diagnostic error is
      // caught");
      try {
        String xml =
            DigirHelper.getInventoryXml(
                DigirHelper.OBIS_VERSION,
                DigirHelper.OBIS_PREFIXES,
                DigirHelper.OBIS_XMLNSES,
                DigirHelper.OBIS_XSDES,
                "NONE", // !!! not available
                DigirHelper.RUTGERS_OBIS_URL,
                new String[] {"darwin:Genus"},
                new String[] {"equals"},
                new String[] {"Carcharodon"},
                "darwin:ScientificName");
        String2.log(xml);
        String2.log("Shouldn't get here.");
        Math2.sleep(60000);
      } catch (Exception e) {
        String2.log(e.toString());
        if (e.toString().indexOf("<diagnostic") < 0) throw e;
        String2.log("\n*** diagnostic error (above) correctly caught\n");
      }
    }

    if (false) {
      // this worked, but I have stopped testing rutgers_obis and
      // stopped doing tests of things that don't work -- don't stress the servers
      // String2.log("\n*** DigirHelper.testGetInventory valid test...");
      table =
          DigirHelper.getInventoryTable(
              DigirHelper.OBIS_VERSION,
              DigirHelper.OBIS_PREFIXES,
              DigirHelper.OBIS_XMLNSES,
              DigirHelper.OBIS_XSDES,
              // "GHMP", //or
              // "aims_biotech",
              // "IMCS", not available
              new String[] {"iziko Fish"},
              DigirHelper.RUTGERS_OBIS_URL,
              new String[] {"darwin:Genus"},
              new String[] {"equals"},
              new String[] {
                // "Macrocystis"},
                "Carcharodon"
              }, // spp=carcharias
              "darwin:ScientificName");
      String2.log("inventory table=" + table);
      Test.ensureTrue(table.nRows() >= 1, "nRows=" + table.nRows());
      String colNames = String2.toCSSVString(table.getColumnNames());
      Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
      Test.ensureEqual(table.getStringData(0, 0), "iziko Fish", "");
      Test.ensureEqual(table.getStringData(1, 0), "Carcharodon carcharias", "");
      Test.ensureEqual(table.getStringData(2, 0), "18", "");
    }

    if (false) {
      // experiment
      // String2.log("\n*** DigirHelper.testGetInventory experiment...");
      table =
          DigirHelper.getInventoryTable(
              DigirHelper.OBIS_VERSION,
              DigirHelper.OBIS_PREFIXES,
              DigirHelper.OBIS_XMLNSES,
              DigirHelper.OBIS_XSDES,
              new String[] {"OBIS-SEAMAP"},
              DigirHelper.RUTGERS_OBIS_URL,
              new String[] {"darwin:YearCollected"},
              new String[] {"equals"},
              new String[] {"2005"},
              "darwin:ScientificName");
      String2.log("inventory table=" + table);
      Test.ensureTrue(table.nRows() >= 1, "nRows=" + table.nRows());
      String colNames = String2.toCSSVString(table.getColumnNames());
      Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
      Test.ensureEqual(table.getStringData(0, 0), "OBIS-SEAMAP", "");
      // Test.ensureEqual(table.getStringData(1,0), "Carcharodon carcharias", "");
      // Test.ensureEqual(table.getStringData(2,0), "18", "");
    }

    if (false) {
      // String2.log("\n*** DigirHelper.testGetInventory valid test of indobis...");
      table =
          DigirHelper.getInventoryTable(
              DigirHelper.OBIS_VERSION,
              DigirHelper.OBIS_PREFIXES,
              DigirHelper.OBIS_XMLNSES,
              DigirHelper.OBIS_XSDES,
              new String[] {"indobis"},
              DigirHelper.IND_OBIS_URL,
              new String[] {"darwin:Genus"},
              new String[] {"equals"},
              new String[] {
                // "Macrocystis"},
                "Carcharodon"
              }, // spp=carcharias
              "darwin:ScientificName");
      String2.log("inventory table=" + table);
      Test.ensureTrue(table.nRows() >= 1, "nRows=" + table.nRows());
      String colNames = String2.toCSSVString(table.getColumnNames());
      Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
      Test.ensureEqual(table.getStringData(0, 0), "indobis", "");
      Test.ensureEqual(table.getStringData(1, 0), "Carcharodon carcharias", "");
      Test.ensureEqual(table.getStringData(2, 0), "7", "");
    }

    // This is the test I normally run because Flanders is reliable.
    if (true) {
      // String2.log("\n*** DigirHelper.testGetInventory valid test of flanders...");
      table =
          DigirHelper.getInventoryTable(
              DigirHelper.OBIS_VERSION,
              DigirHelper.OBIS_PREFIXES,
              DigirHelper.OBIS_XMLNSES,
              DigirHelper.OBIS_XSDES,
              new String[] {"tisbe"},
              DigirHelper.FLANDERS_OBIS_URL,
              new String[] {"darwin:Genus"},
              new String[] {"equals"},
              new String[] {"Abietinaria"},
              // "darwin:Genus");
              "darwin:ScientificName");
      String2.log("inventory table=" + table);
      // 11 Abietinaria abietina
      // 3 Abietinaria filicula
      String2.log("darwin:ScientificNames: " + table.getColumn(1).toString());
      Test.ensureTrue(table.nRows() >= 2, "nRows=" + table.nRows());
      String colNames = String2.toCSSVString(table.getColumnNames());
      Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
      Test.ensureEqual(table.getStringData(0, 0), "tisbe", "");
      Test.ensureEqual(table.getStringData(1, 0), "Abietinaria abietina", "");
      // pre 2009-02-12 was 11; then 10; post 2009-03-12 is 11; post 2010-07-19 is 10
      Test.ensureEqual(table.getIntData(2, 0), 10, "");
      Test.ensureEqual(table.getStringData(0, 1), "tisbe", "");
      Test.ensureEqual(table.getStringData(1, 1), "Abietinaria filicula", "");
      // pre 2009-02-12 was 3; then 2; post 2009-03-12 is 3; post 2010-07-19 is 2
      Test.ensureEqual(table.getIntData(2, 1), 2, "");
    }
  }

  /** This tests searchBmde. */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testBmde() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    try {

      Table table =
          DigirHelper.searchBmde(
              new String[] {"prbo05"},
              "http://digir.prbo.org/digir/DiGIR.php",
              new String[] {
                "bmde:Family", "bmde:Genus", "bmde:ObservationDate", "bmde:ObservationDate"
              }, // "bmde:Class",
              new String[] {"equals", "equals", "greaterThan", "lessThan"}, // "equals"
              new String[] {"Laridae", "Uria", "2007-06-01", "2007-06-05"}, // "Aves",
              new String[] {
                "bmde:DecimalLongitude",
                "bmde:DecimalLatitude",
                "bmde:ObservationDate",
                "bmde:GlobalUniqueIdentifier",
                "bmde:Genus",
                "bmde:ScientificName"
              });
      String fileName = "c:/temp/DigirHelperTestBmde.csv";
      table.saveAsCsvASCII(fileName);
      String results = File2.readFromFile88591(fileName)[1];
      String expected =
          "\"bmde:DecimalLongitude\",\"bmde:DecimalLatitude\",\"bmde:ObservationDate\",\"bmde:GlobalUniqueIdentifier\",\"bmde:Genus\",\"bmde:ScientificName\"\n"
              + "\"degrees_east\",\"degrees_north\",\"\",\"\",\"\",\"\"\n"
              + "-123.002737,37.698771,\"2007-06-01T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1171.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-02T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1172.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-03T17:49:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1173.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-04T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1174.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-01T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1191.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-02T17:55:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1192.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-03T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1193.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-04T17:25:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1194.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-01T17:10:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1616.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-02T17:49:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1617.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-03T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1618.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-04T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1619.1\",\"Uria\",\"Uria aalge\"\n"
              + "-123.002737,37.698771,\"2007-06-03T17:49:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17167.SHORE1.COMUSubcolonyCount.1184.1\",\"Uria\",\"Uria aalge\"\n";
      Test.ensureEqual(
          results.substring(0, Math.min(results.length(), expected.length())),
          expected,
          "results=" + results);

      // expected error didn't occur!
      String2.pressEnterToContinue(
          "\n" + MustBe.getStackTrace() + "An expected error didn't occur at the above location.");

    } catch (Exception e) {
      String2.log(
          "THIS STOPPED WORKING ~JAN 2009: "
              + MustBe.throwableToString(e)
              + "\nI think Digir is dead.");
      Math2.gc("DigirHelper (between tests)", 5000); // in a test, after displaying a message
    }
  }

  /** This tests searchObis. */
  @org.junit.jupiter.api.Test
  @TagExternalOther // error in response (meta tag not closed)
  void testObis() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    try {
      String url = DigirHelper.RUTGERS_OBIS_URL;
      // String url = CSIRO_OBIS_URL;

      // test ghmp for Macrocystis -- this used to work
      Table table;
      if (false) {
        // THIS WORKED ON 2007-05-03
        table = new Table();
        DigirHelper.searchObis(
            // IMCS is the code for "Institute of Marine and Coastal Sciences, Rutgers
            // University"
            // It is listed for the "host", not as a resouce.
            // but url response is it isn't available.
            new String[] {"GHMP"}, // has lots
            // "aims_biotech","IndOBIS","EMAP"}, //"EMAP" has no data
            url,
            new String[] {"darwin:Genus"},
            new String[] {"equals"},
            new String[] {
              // "Pterosiphonia"},
              "Macrocystis"
            },
            // "Carcharodon"}, // sp=carcharias
            table,
            false,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:CatalogNumber",
              "darwin:ScientificName",
              "darwin:Longitude",
              "darwin:Latitude",
              "obis:Temperature"
            });
        String2.log("\nresulting table is: " + table);
        Test.ensureEqual(table.nRows(), 248, "");
      }

      if (false) {
        // THIS WORKED ON 2007-05-03
        table = new Table();
        DigirHelper.searchObis(
            new String[] {"GHMP"},
            url,
            new String[] {"darwin:Genus", "darwin:Latitude"}, // , "darwin:Latitude"},
            new String[] {"equals", "greaterThan"}, // , "lessThan"},
            new String[] {"Macrocystis", "53"}, // , "54"},
            table,
            false,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:CatalogNumber",
              "darwin:ScientificName",
              "darwin:Longitude",
              "darwin:Latitude",
              "obis:Temperature"
            });
        String2.log("\nresulting table is: " + table);
        Test.ensureEqual(table.nRows(), 72, "");
      }

      if (false) {
        // this worked on 6/25/07, but stop doing tests that don't work
        String2.log("\n*** DigirHelper.testObis test of invalid filter variable");
        try {
          table = new Table();
          DigirHelper.searchObis(
              new String[] {"GHMP"},
              url,
              new String[] {"darwin:Genus", "darwin:XYZ"},
              new String[] {"equals", "greaterThan"},
              new String[] {"Macrocystis", "53"},
              table,
              false,
              new String[] {"darwin:InstitutionCode"});
          String2.log("Shouldn't get here.");
          Math2.sleep(60000);
        } catch (Exception e) {
          String2.log(e.toString());
          if (e.toString().indexOf("darwin:XYZ") < 0) throw e;
          String2.log("\n*** diagnostic error (above) correctly caught\n");
        }

        String2.log("\n*** DigirHelper.testObis test of invalid COP");
        try {
          table = new Table();
          DigirHelper.searchObis(
              new String[] {"GHMP"},
              url,
              new String[] {"darwin:Genus", "darwin:Latitude"},
              new String[] {"equals", "greaterTheen"},
              new String[] {"Macrocystis", "53"},
              table,
              false,
              new String[] {"darwin:InstitutionCode"});
          String2.log("Shouldn't get here.");
          Math2.sleep(60000);
        } catch (Exception e) {
          String2.log(e.toString());
          if (e.toString().indexOf("greaterTheen") < 0) throw e;
          String2.log("\n*** diagnostic error (above) correctly caught\n");
        }

        String2.log("\n*** DigirHelper.testObis test of invalid resultsVariable");
        try {
          table = new Table();
          DigirHelper.searchObis(
              new String[] {"GHMP"},
              url,
              new String[] {"darwin:Genus", "darwin:Latitude"},
              new String[] {"equals", "greaterThan"},
              new String[] {"Macrocystis", "53"},
              table,
              false,
              new String[] {"darwin:InstitutionCoode"});
          String2.log("Shouldn't get here.");
          Math2.sleep(60000);
        } catch (Exception e) {
          String2.log(e.toString());
          if (e.toString().indexOf("darwin:InstitutionCoode") < 0) throw e;
          String2.log("\n*** diagnostic error (above) correctly caught\n");
        }

        String2.log("\n*** DigirHelper.testObis test of invalid resource");
        try {
          table = new Table();
          DigirHelper.searchObis(
              new String[] {"NONE"},
              url,
              new String[] {"darwin:Genus", "darwin:Latitude"},
              new String[] {"equals", "greaterThan"},
              new String[] {"Macrocystis", "53"},
              table,
              false,
              new String[] {"darwin:InstitutionCode"});
          String2.log("Shouldn't get here.");
          Math2.sleep(60000);
        } catch (Exception e) {
          String2.log(e.toString());
          if (e.toString().indexOf("<diagnostic") < 0) throw e;
          String2.log("\n*** diagnostic error (above) correctly caught\n");
        }
      }

      if (false) {
        String2.log("\n*** DigirHelper.testObis test of includeXYZT=false");
        // THIS WORKED ON 2007-05-03 but doesn't anymore because of
        // failure on obis server.
        table = new Table();
        DigirHelper.searchObis(
            new String[] {
              "GHMP", // note that requests for "IMCS" (which I hoped would mean "all") fail
              "NONE"
            }, // "NONE" tests not failing if one resource fails
            url,
            new String[] {
              "darwin:Genus",
              "darwin:Latitude",
              "darwin:Latitude",
              "darwin:YearCollected",
              "darwin:YearCollected"
            },
            new String[] {
              "equals", "greaterThan", "lessThan", "greaterThanOrEquals", "lessThanOrEquals"
            },
            new String[] {"Macrocystis", "53", "54", "1970", "2100"},
            table,
            false,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:CatalogNumber",
              "darwin:ScientificName",
              "darwin:Longitude",
              "darwin:Latitude",
              "obis:Temperature"
            });
        String2.log("\nresulting table is: " + table);
        Test.ensureTrue(table.nRows() >= 60, "nRows=" + table.nRows());
        Test.ensureEqual(table.nColumns(), 7, "");
        Test.ensureEqual(
            String2.toCSSVString(table.getColumnNames()),
            "darwin:InstitutionCode, darwin:CollectionCode, darwin:CatalogNumber, "
                + "darwin:ScientificName, darwin:Longitude, darwin:Latitude, "
                + "obis:Temperature",
            "");
        // !!!note that rows of data are in pairs of almost duplicates
        // and CollectionCode includes 2 sources -- 1 I requested and another one (both
        // served by GHMP?)
        // and Lat and Lon can be slightly different (e.g., row 60/61 lat)
        DoubleArray latCol = (DoubleArray) table.getColumn(5);
        double stats[] = latCol.calculateStats();
        Test.ensureTrue(
            stats[PrimitiveArray.STATS_MIN] >= 53, "min=" + stats[PrimitiveArray.STATS_MIN]);
        Test.ensureTrue(
            stats[PrimitiveArray.STATS_MAX] <= 54, "max=" + stats[PrimitiveArray.STATS_MAX]);
        Test.ensureEqual(stats[PrimitiveArray.STATS_N], table.nRows(), "");

        // CollectionYear not in results
        // DoubleArray timeCol = (DoubleArray)table.getColumn(3);
        // stats = timeCol.calculateStats();
        // Test.ensureTrue(stats[PrimitiveArray.STATS_MIN] >= 0, "min=" +
        // stats[PrimitiveArray.STATS_MIN]);

        StringArray catCol = (StringArray) table.getColumn(2);
        int row = catCol.indexOf("10036-MACRINT"); // ==0
        Test.ensureEqual(
            table.getStringData(0, row), "Marine Fish Division, Fisheries and Oceans Canada", "");
        Test.ensureEqual(table.getStringData(1, row), "Gwaii Haanas Marine Algae", "");
        Test.ensureEqual(table.getStringData(2, row), "10036-MACRINT", "");
        Test.ensureEqual(table.getStringData(3, row), "Macrocystis integrifolia", "");
        Test.ensureEqual(table.getDoubleData(4, row), -132.4223, "");
        Test.ensureEqual(table.getDoubleData(5, row), 53.292, "");
        Test.ensureEqual(table.getDoubleData(6, row), Double.NaN, "");

        Test.ensureEqual(table.getStringData(0, row + 1), "BIO", "");
        Test.ensureEqual(table.getStringData(1, row + 1), "GHMP", "");
        Test.ensureEqual(table.getStringData(2, row + 1), "10036-MACRINT", "");
        Test.ensureEqual(table.getStringData(3, row + 1), "Macrocystis integrifolia", "");
        Test.ensureEqual(table.getDoubleData(4, row + 1), -132.4223, "");
        Test.ensureEqual(table.getDoubleData(5, row + 1), 53.292, "");
        Test.ensureEqual(table.getDoubleData(6, row + 1), Double.NaN, "");

        row = catCol.indexOf("198-MACRINT");
        Test.ensureEqual(
            table.getStringData(0, row), "Marine Fish Division, Fisheries and Oceans Canada", "");
        Test.ensureEqual(table.getStringData(1, row), "Gwaii Haanas Marine Algae", "");
        Test.ensureEqual(table.getStringData(2, row), "198-MACRINT", "");
        Test.ensureEqual(table.getStringData(3, row), "Macrocystis integrifolia", "");
        Test.ensureEqual(table.getDoubleData(4, row), -132.08171, "");
        Test.ensureEqual(table.getDoubleData(5, row), 53.225193, "");
        Test.ensureEqual(table.getDoubleData(6, row), Double.NaN, "");

        Test.ensureEqual(table.getStringData(0, row + 1), "BIO", "");
        Test.ensureEqual(table.getStringData(1, row + 1), "GHMP", "");
        Test.ensureEqual(table.getStringData(2, row + 1), "198-MACRINT", "");
        Test.ensureEqual(table.getStringData(3, row + 1), "Macrocystis integrifolia", "");
        Test.ensureEqual(table.getDoubleData(4, row + 1), -132.08171, "");
        Test.ensureEqual(table.getDoubleData(5, row + 1), 53.22519, "");
        Test.ensureEqual(table.getDoubleData(6, row + 1), Double.NaN, "");
      }

      if (false) {
        String2.log("\n*** DigirHelper.testObis test of includeXYZT=true");
        // THIS WORKED ON 2007-05-04 but has failed most of the time since:
        // <diagnostic code="Unknown PHP Error [2]"
        // severity="DIAG_WARNING">odbc_pconnect()
        // : SQL error: [Microsoft][ODBC Driver Manager] Data source name not found and
        // no
        // default driver specified, SQL state IM002 in SQLConnect
        // (D:\DiGIR_Phoebe\DiGIRpr
        // ov\lib\adodb\drivers\adodb-odbc.inc.php:173)</diagnostic>
        // <diagnostic code="INTERNAL_DATABASE_ERROR" severity="fatal">A connection to
        // the
        // database could not be created.</diagnostic>
        // </diagnostics></response>
        String testName = "c:/temp/ObisMac5354.nc";
        table = new Table();
        DigirHelper.searchObis(
            new String[] {"GHMP"},
            // note that requests for "IMCS" (which I hoped would mean "all") fail
            // ARC throws no error, but returns 0 records
            // not avail: "Gwaii Haanas Marine Algae"
            url,
            new String[] {
              "darwin:Genus",
              "darwin:Latitude",
              "darwin:Latitude",
              "darwin:YearCollected",
              "darwin:YearCollected"
            },
            new String[] {
              "equals", "greaterThan", "lessThan", "greaterThanOrEquals", "lessThanOrEquals"
            },
            new String[] {"Macrocystis", "53", "54", "1970", "2100"},
            table,
            true,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:ScientificName",
              "obis:Temperature"
            });
        String2.log("\nresulting table is: " + table);

        // test contents
        TableTests.testObis5354Table(table);

        // test reading as NetcdfDataset
        table.saveAsFlatNc(testName, "row");
        NetcdfDataset ncd = NetcdfDatasets.openDataset(testName); // 2021: 's' is new API
        try {
          Object o[] = ncd.getCoordinateAxes().toArray();
          String so = String2.toCSSVString(o);
          String2.log("axes=" + so);
          Test.ensureEqual(o.length, 4, "");
          Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Lon\"") >= 0, "");
          Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Lat\"") >= 0, "");
          Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Height\"") >= 0, "");
          Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Time\"") >= 0, "");

        } finally {
          ncd.close();
        }
      }

      // test iziko Fish for Carcharodon
      if (false) {
        table = new Table();
        DigirHelper.searchObis(
            // IMCS is the code for "Institute of Marine and Coastal Sciences, Rutgers
            // University"
            // It is listed for the "host", not as a resouce.
            // but url response is it isn't available.
            new String[] {"iziko Fish"},
            url,
            new String[] {"darwin:Genus"},
            new String[] {"equals"},
            new String[] {
              // "Pterosiphonia"},
              // "Macrocystis"},
              "Carcharodon"
            }, // sp=carcharias
            table,
            false,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:CatalogNumber",
              "darwin:ScientificName",
              "darwin:Longitude",
              "darwin:Latitude",
              "obis:Temperature"
            });
        String2.log("\nresulting table is: " + table);
        Test.ensureEqual(table.nRows(), 248, "");
      }

      // indobis is more reliable, but doesn't support numeric tests of lat lon
      if (false) {
        url = DigirHelper.IND_OBIS_URL;
        table = new Table();
        DigirHelper.searchObis(
            new String[] {"indobis"},
            url,
            new String[] {"darwin:Genus"},
            new String[] {"equals"},
            new String[] {"Carcharodon"}, // sp=carcharias
            table,
            false,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:CatalogNumber",
              "darwin:ScientificName",
              "darwin:Longitude",
              "darwin:Latitude",
              "obis:Temperature"
            });
        String2.log("\nresulting table is: " + table);
        // Row darwin:Institu darwin:Collect darwin:Catalog darwin:Scienti
        // darwin:Longitu darwin:Latitud obis:Temperatu
        // 0 NCL INDOBIS-DATASE 101652 Carcharodon ca 55.666667 -4.583333 NaN
        // 1 NCL INDOBIS-DATASE 101652 Carcharodon ca 18.583334 -34.133335 NaN
        // 2 NCL INDOBIS-DATASE 101652 Carcharodon ca 0 0 NaN
        Test.ensureEqual(table.nColumns(), 7, "");
        Test.ensureEqual(table.nRows(), 7, "");
        Test.ensureEqual(
            String2.toCSSVString(table.getColumnNames()),
            "darwin:InstitutionCode, darwin:CollectionCode, darwin:CatalogNumber, "
                + "darwin:ScientificName, darwin:Longitude, darwin:Latitude, obis:Temperature",
            "");
        Test.ensureEqual(table.getStringData(0, 0), "NCL", "");
        Test.ensureEqual(table.getStringData(1, 0), "INDOBIS-DATASET1", "");
        Test.ensureEqual(table.getStringData(2, 0), "101652", "");
        Test.ensureEqual(table.getStringData(3, 0), "Carcharodon carcharias", "");
        Test.ensureEqual(table.getFloatData(4, 0), 55.666667f, "");
        Test.ensureEqual(table.getFloatData(5, 0), -4.583333f, "");
        Test.ensureEqual(table.getFloatData(6, 0), Float.NaN, "");
        Test.ensureEqual(table.getStringData(0, 1), "NCL", "");
        Test.ensureEqual(table.getStringData(1, 1), "INDOBIS-DATASET1", "");
        Test.ensureEqual(table.getStringData(2, 1), "101652", "");
        Test.ensureEqual(table.getStringData(3, 1), "Carcharodon carcharias", "");
        Test.ensureEqual(table.getFloatData(4, 1), 18.583334f, "");
        Test.ensureEqual(table.getFloatData(5, 1), -34.133335f, "");
        Test.ensureEqual(table.getFloatData(6, 1), Float.NaN, "");
        Test.ensureEqual(table.getStringData(0, 2), "NCL", "");
        Test.ensureEqual(table.getStringData(1, 2), "INDOBIS-DATASET1", "");
        Test.ensureEqual(table.getStringData(2, 2), "101652", "");
        Test.ensureEqual(table.getStringData(3, 2), "Carcharodon carcharias", "");
        Test.ensureEqual(table.getFloatData(4, 2), 0f, ""); // missing value?!
        Test.ensureEqual(table.getFloatData(5, 2), 0f, "");
        Test.ensureEqual(table.getFloatData(6, 2), Float.NaN, "");
      }

      // flanders -- This works reliably. It is my main test.
      if (true) {
        url = DigirHelper.FLANDERS_OBIS_URL;
        table = new Table();
        DigirHelper.searchObis(
            new String[] {"tisbe"},
            url,
            new String[] {"darwin:ScientificName", "darwin:Longitude"},
            new String[] {"equals", "lessThan"},
            new String[] {"Abietinaria abietina", "2"},
            table,
            false,
            new String[] {
              "darwin:InstitutionCode",
              "darwin:CollectionCode",
              "darwin:CatalogNumber",
              "darwin:ScientificName",
              "darwin:Longitude",
              "darwin:Latitude",
              "obis:Temperature"
            });
        // pre 2010-07-27 was 4 rows:
        // Row darwin:Institu darwin:Collect darwin:Catalog darwin:Scienti
        // darwin:Longitu darwin:Latitud obis:Temperatu
        // 0 VLIZ Tisbe 405003 Abietinaria ab 1.57 50.849998 NaN
        // 1 VLIZ Tisbe 405183 Abietinaria ab -20 40 NaN
        // 2 VLIZ Tisbe 415428 Abietinaria ab 1.95 51.23 NaN
        // 3 VLIZ Tisbe 562956 Abietinaria ab 1.62 50.77 NaN

        String results = table.dataToString();
        String expected =
            "darwin:InstitutionCode,darwin:CollectionCode,darwin:CatalogNumber,darwin:ScientificName,darwin:Longitude,darwin:Latitude,obis:Temperature\n"
                + "VLIZ,Tisbe,405183,Abietinaria abietina,-20.0,46.0,\n"
                + "VLIZ,Tisbe,415428,Abietinaria abietina,1.95,51.229,\n"
                + "VLIZ,Tisbe,562956,Abietinaria abietina,1.615055,50.77295,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
      }

    } catch (Exception e) {
      throw e;
      // String2.pressEnterToContinue(MustBe.throwableToString(e) +
      // "\nUNEXPECTED DigirHelper " + String2.ERROR);

    }
  }

  /** This tests searchOpendapStyleObis(). */
  @org.junit.jupiter.api.Test
  @TagExternalOther // error in response (meta tag not closed)
  void testOpendapStyleObis() throws Exception {
    Table table = new Table();
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // verbose = true;
    // reallyVerbose = true;

    // these invalid queries are caught locally
    String2.log("\n*** DigirHelper.testOpendapStyleObis test of unknown op");
    try {
      DigirHelper.searchObisOpendapStyle(
          new String[] {"GHMP"},
          DigirHelper.RUTGERS_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&darwin:Genus!Macrocystis&darwin:Latitude>53&darwin:Latitude<54",
          table);
      String2.log("Shouldn't get here.");
      Math2.sleep(60000);
    } catch (Exception e) {
      String2.log(e.toString());
      String2.log("\n*** diagnostic error (above) correctly caught\n");
    }

    String2.log("\n*** DigirHelper.testOpendapStyleObis test of empty filter at beginning");
    try {
      DigirHelper.searchObisOpendapStyle(
          new String[] {"GHMP"},
          DigirHelper.RUTGERS_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Latitude<54",
          table);
      String2.log("Shouldn't get here.");
      Math2.sleep(60000);
    } catch (Exception e) {
      String2.log(e.toString());
      String2.log("\n*** diagnostic error (above) correctly caught\n");
    }

    String2.log("\n*** DigirHelper.testOpendapStyleObis test of empty filter at end");
    try {
      DigirHelper.searchObisOpendapStyle(
          new String[] {"GHMP"},
          DigirHelper.RUTGERS_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Latitude<54&",
          table);
      String2.log("Shouldn't get here.");
      Math2.sleep(60000);
    } catch (Exception e) {
      String2.log(e.toString());
      String2.log("\n*** diagnostic error (above) correctly caught\n");
    }

    String2.log("\n*** DigirHelper.testOpendapStyleObis test of invalid var name");
    try {
      DigirHelper.searchObisOpendapStyle(
          new String[] {"GHMP"},
          DigirHelper.RUTGERS_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Laatitude<54",
          table);
      String2.log("Shouldn't get here.");
      Math2.sleep(60000);
    } catch (Exception e) {
      String2.log(e.toString());
      String2.log("\n*** diagnostic error (above) correctly caught\n");
    }

    // experiment (not normally run)
    if (false) {
      table.clear();
      String2.log("\n*** DigirHelper.testOpendapStyleObis test of experiment");
      DigirHelper.searchObisOpendapStyle(
          new String[] {"OBIS-SEAMAP"},
          DigirHelper.RUTGERS_OBIS_URL,
          "obis:Temperature,darwin:ScientificName"
              + "&darwin:ScientificName=Caretta caretta"
              + "&darwin:YearCollected=2005",
          table);

      String2.log("\nresulting table is: " + table);
      TableTests.testObis5354Table(table);
    }

    // valid request, standard test
    // but I have stopped testing rutgers because it is down so often
    if (false) {
      table.clear();
      String2.log("\n*** DigirHelper.testOpendapStyleObis test of valid request");
      DigirHelper.searchObisOpendapStyle(
          new String[] {"GHMP"},
          DigirHelper.RUTGERS_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Latitude<54"
              + "&darwin:YearCollected>=1970&darwin:YearCollected<=2100",
          table);

      String2.log("\nresulting table is: " + table);
      TableTests.testObis5354Table(table);
    }

    // valid request of indobis but indobis treats lat lon queries as strings!
    if (false) {
      table.clear();
      String2.log("\n*** DigirHelper.testOpendapStyleObis test of valid request");
      DigirHelper.searchObisOpendapStyle(
          new String[] {"indobis"},
          DigirHelper.IND_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&darwin:Genus=Carcharodon"
              + "&darwin:Latitude>=-10&darwin:Latitude<=0" // ERROR: QUERY_TERM_TOO_SHORT
          ,
          table);

      String2.log("\nresulting table is: " + table);
      // testObisCarcharodonTable(table);
    }

    // test flanders This is a reliable test that I normally use.
    try {
      table.clear();
      String2.log("\n*** DigirHelper.testOpendapStyleObis test flanders");
      DigirHelper.searchObisOpendapStyle(
          new String[] {"tisbe"},
          DigirHelper.FLANDERS_OBIS_URL,
          "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature"
              + "&darwin:Genus=Abietinaria&darwin:Longitude<2",
          table);

      testObisAbietinariaTable(table);
    } catch (Exception e) {
      throw e;
      // String2.pressEnterToContinue(MustBe.throwableToString(e) +
      // "\nUnexpected DigirHelper error");

    }
  }

  /**
   * This ensures that the table is the standard result from indobis request for genus=Carcharodon
   * from lat -10 to 0.
   *
   * @throws Exception if unexpected value found in table.
   */
  private static void testObisAbietinariaTable(Table table) {
    String2.log("\nresulting table is: " + table);
    Test.ensureEqual(
        String2.toCSSVString(table.getColumnNames()),
        "LON, LAT, DEPTH, TIME, ID, darwin:InstitutionCode, darwin:CollectionCode, "
            + "darwin:ScientificName, obis:Temperature",
        "");

    String results = table.dataToString();
    String expected =
        // pre 2010-07-27 was 7 rows
        "LON,LAT,DEPTH,TIME,ID,darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature\n"
            + "-20.0,46.0,,,VLIZ:Tisbe:405183,VLIZ,Tisbe,Abietinaria abietina,\n"
            + "1.606355,50.73067,,,VLIZ:Tisbe:407878,VLIZ,Tisbe,Abietinaria filicula,\n"
            + "-4.54935,54.2399,,,VLIZ:Tisbe:411870,VLIZ,Tisbe,Abietinaria filicula,\n"
            + "1.95,51.229,,,VLIZ:Tisbe:415428,VLIZ,Tisbe,Abietinaria abietina,\n"
            + "1.615055,50.77295,,,VLIZ:Tisbe:562956,VLIZ,Tisbe,Abietinaria abietina,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
