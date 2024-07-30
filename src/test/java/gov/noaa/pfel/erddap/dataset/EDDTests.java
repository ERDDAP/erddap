package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.array.UByteArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class EDDTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testSuggestInstitutionParts() {
    String2.log("\n*** EDD.testSuggestionInstituionParts");

    Test.ensureEqual(
        String2.toCSSVString(EDD.suggestInstitutionParts("http://some.site.com:8080/erddap")),
        "site, some",
        "");
    Test.ensureEqual(
        String2.toCSSVString(
            EDD.suggestInstitutionParts("https://thredds1.some.site.gov:8080/erddap")),
        "site, some",
        "");
    Test.ensureEqual(
        String2.toCSSVString(
            EDD.suggestInstitutionParts("https://the.thredds1.some.site.gov:8080/erddap")),
        "site, some",
        "");
    Test.ensureEqual(
        String2.toCSSVString(EDD.suggestInstitutionParts("ftp://www.some.site.org/erddap")),
        "site, some",
        "");
    Test.ensureEqual(
        String2.toCSSVString(EDD.suggestInstitutionParts("ftps://www.site/erddap")), "site", "");
    Test.ensureEqual(
        String2.toCSSVString(EDD.suggestInstitutionParts("sftp://site/erddap")), "site", "");

    Test.ensureEqual(
        String2.toCSSVString(EDD.suggestInstitutionParts("/some/dir1/dir2")), "dir2", "");
    Test.ensureEqual(
        String2.toCSSVString(EDD.suggestInstitutionParts("\\some\\dir1\\dir2\\")), "dir2", "");
    Test.ensureEqual(String2.toCSSVString(EDD.suggestInstitutionParts("/some/dir/")), "dir", "");
    Test.ensureEqual(String2.toCSSVString(EDD.suggestInstitutionParts("/dir/")), "dir", "");

    Test.ensureEqual(String2.toCSSVString(EDD.suggestInstitutionParts("/")), "", "");
  }

  @org.junit.jupiter.api.Test
  void testSparqlP01toP02() throws Exception {
    // String2.log("**** EDD.testSparqlP01toP02()");
    Test.ensureEqual(EDD.sparqlP01toP02("PSLTZZ01"), "PSAL", "");
    // String2.log("* Test of invalid P01 value:");
    Test.ensureEqual(EDD.sparqlP01toP02("Bob"), null, "");
  }

  @org.junit.jupiter.api.Test
  void testAddMvFvAttsIfNeeded() throws Throwable {
    String2.log("\n*** EDD.testAddMvFvAttsIfNeeded()");

    PrimitiveArray pa;
    Attributes sourceAtts, addAtts;

    // no pa -> false
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", null, sourceAtts, addAtts), false, "");

    // no addAtts -> false
    pa = new ByteArray(new byte[] {99});
    sourceAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, null), false, "");

    // StringArray -> false
    pa = new StringArray(new String[] {"99"});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), false, "");

    // ***
    // 99 is caught if observed and nothing defined
    pa = new ByteArray(new byte[] {12, 99});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), true, "");
    Test.ensureEqual(addAtts.get("_FillValue"), new ByteArray(new byte[] {99}), "");

    // -127 is caught if observed and nothing defined
    pa = new ByteArray(new byte[] {12, -127});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), true, "");
    Test.ensureEqual(addAtts.get("_FillValue"), new ByteArray(new byte[] {-127}), "");

    // -128 is caught if observed and nothing defined
    pa = new ByteArray(new byte[] {12, -128});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), true, "");
    Test.ensureEqual(addAtts.get("_FillValue"), new ByteArray(new byte[] {-128}), "");

    // 127 is caught even if no values
    pa = new ByteArray();
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), true, "");
    Test.ensureEqual(addAtts.get("_FillValue"), new ByteArray(new byte[] {127}), "");

    // 127 is caught even if no values
    pa = new ByteArray().setMaxIsMV(true);
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), true, "");
    Test.ensureEqual(addAtts.get("_FillValue"), new ByteArray(new byte[] {127}), "");

    // 127 is caught if present, even if something else is defined (duplicate)
    pa = new ByteArray(new byte[] {12, 127});
    sourceAtts = new Attributes().add("missing_value", new ByteArray(new byte[] {99}));
    addAtts = new Attributes().add("_FillValue", new ByteArray(new byte[] {99}));
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), true, "");
    Test.ensureEqual(addAtts.get("missing_value"), new ByteArray(new byte[] {127}), "");

    // ubyte 255 -> true if nothing defined
    pa = new UByteArray(new short[] {12});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("_FillValue"), new UByteArray(new short[] {255}), "");

    // short -32767 -> true if nothing defined
    pa = new ShortArray(new short[] {12, -32767, 32767});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("_FillValue"), new ShortArray(new short[] {-32767}), "");

    // int -2147483647 -> true if nothing defined
    pa = new IntArray(new int[] {12, -2147483647, 2147483647});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("_FillValue"), new IntArray(new int[] {-2147483647}), "");

    // long -9223372036854775808 -> true if nothing defined
    pa = new LongArray(new long[] {12, -9223372036854775808L});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(
        addAtts.get("_FillValue"), new LongArray(new long[] {-9223372036854775808L}), "");

    // long -9223372036854775807 -> true if nothing defined
    pa = new LongArray(new long[] {12, -9223372036854775807L});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(
        addAtts.get("_FillValue"), new LongArray(new long[] {-9223372036854775807L}), "");

    // long 9223372036854775807 -> true nothing defined
    pa = new LongArray(new long[] {12});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(
        addAtts.get("_FillValue"), new LongArray(new long[] {9223372036854775807L}), "");

    // long 9999 -> true if nothing defined
    pa = new LongArray(new long[] {12, 9999});
    sourceAtts = new Attributes();
    addAtts = new Attributes();
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("_FillValue"), new LongArray(new long[] {9999}), "");

    // ***
    // 99 not caught if something already defined
    pa = new ByteArray(new byte[] {12, 99});
    sourceAtts = new Attributes().add("missing_value", new ByteArray(new byte[] {120}));
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), false, "");

    // -127 not caught if something already defined
    pa = new ByteArray(new byte[] {12, -127});
    sourceAtts = new Attributes();
    addAtts = new Attributes().add("_FillValue", new ByteArray(new byte[] {120}));
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), false, "");

    // -128 not caught if something already defined
    pa = new ByteArray(new byte[] {12, -128});
    sourceAtts = new Attributes().add("missing_value", new ByteArray(new byte[] {120}));
    addAtts = new Attributes();
    Test.ensureEqual(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), false, "");

    // float -9223372036854775807 -> true if mv fv already defined different but
    // identical
    pa = new FloatArray(new float[] {12, -999});
    sourceAtts = new Attributes().add("missing_value", new FloatArray(new float[] {9999}));
    addAtts = new Attributes().add("_FillValue", new FloatArray(new float[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new FloatArray(new float[] {-999}), "");

    // float NaN -> true if mv fv already defined different but identical
    pa = new FloatArray(new float[] {12, Float.NaN});
    sourceAtts = new Attributes().add("missing_value", new FloatArray(new float[] {9999}));
    addAtts = new Attributes().add("_FillValue", new FloatArray(new float[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new FloatArray(new float[] {Float.NaN}), "");

    // float 1.234567e36f -> true if mv fv already defined different but identical
    pa = new FloatArray(new float[] {12, 1.234567e36f});
    sourceAtts = new Attributes().add("missing_value", new FloatArray(new float[] {9999}));
    addAtts = new Attributes().add("_FillValue", new FloatArray(new float[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new FloatArray(new float[] {1.234567e36f}), "");

    // float -99.9f -> true if mv fv already defined different but identical
    pa = new FloatArray(new float[] {12, 9999, -99.9f});
    sourceAtts = new Attributes().add("missing_value", new FloatArray(new float[] {9999}));
    addAtts = new Attributes().add("_FillValue", new FloatArray(new float[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new FloatArray(new float[] {-99.9f}), "");

    // double -9223372036854775807 -> true if mv fv already defined different but
    // identical
    pa = new DoubleArray(new double[] {12, -999});
    sourceAtts =
        new Attributes()
            .add("_FillValue", new DoubleArray(new double[] {9999}))
            .add("missing_value", new DoubleArray(new double[] {-999})); // will be ignored
    addAtts = new Attributes().add("missing_value", new DoubleArray(new double[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new DoubleArray(new double[] {-999}), "");

    // double NaN -> true if mv fv already defined different but identical
    pa = new DoubleArray(new double[] {12, Double.NaN});
    sourceAtts =
        new Attributes()
            .add("_FillValue", new DoubleArray(new double[] {9999}))
            .add("missing_value", new DoubleArray(new double[] {-999})); // will be ignored
    addAtts = new Attributes().add("missing_value", new DoubleArray(new double[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new DoubleArray(new double[] {Double.NaN}), "");

    // double 1.234567890987654e36 -> true if mv fv already defined different but
    // identical
    pa = new DoubleArray(new double[] {12, 1.234567890987654e36});
    sourceAtts =
        new Attributes()
            .add("_FillValue", new DoubleArray(new double[] {9999}))
            .add("missing_value", new DoubleArray(new double[] {-999})); // will be ignored
    addAtts = new Attributes().add("missing_value", new DoubleArray(new double[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(
        addAtts.get("missing_value"), new DoubleArray(new double[] {1.234567890987654e36}), "");

    // double 1.5e301 -> true if mv fv already defined different but identical
    pa = new DoubleArray(new double[] {12, 1.5e301});
    sourceAtts =
        new Attributes()
            .add("_FillValue", new DoubleArray(new double[] {9999}))
            .add("missing_value", new DoubleArray(new double[] {-999})); // will be ignored
    addAtts = new Attributes().add("missing_value", new DoubleArray(new double[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new DoubleArray(new double[] {1.5e301}), "");

    // double -99.9 -> true if mv fv already defined different but identical
    pa = new DoubleArray(new double[] {12, 9999, -99.9});
    sourceAtts = new Attributes().add("missing_value", new DoubleArray(new double[] {9999}));
    addAtts = new Attributes().add("_FillValue", new DoubleArray(new double[] {9999}));
    Test.ensureTrue(EDD.addMvFvAttsIfNeeded("testVar", pa, sourceAtts, addAtts), "");
    Test.ensureEqual(addAtts.get("missing_value"), new DoubleArray(new double[] {-99.9}), "");
  }

  /**
   * This tests addFillValueAttributes.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testAddFillValueAttributes() throws Throwable {
    // String2.log("\n*** EDD.testAddFillValueAttributes()");
    String dir = EDDTests.class.getResource("/data/addFillValueAttributes").getPath() + "/";

    try {
      // make temp copy of datasets.xml
      File2.copy(dir + "datasets.xml", dir + "tempDatasets.xml");

      // alter that temp copy
      // String results = addFillValueAttributes( //throws exception
      // dir + "tempDatasets.xml", dir + "addFillValueAttributes.csv");
      // String2.log(results);

      // same thing but done via generateDatasetsXml
      String results =
          new GenerateDatasetsXml()
              .doIt(
                  new String[] {
                    "-verbose",
                    "addFillValueAttributes",
                    dir + "tempDatasets.xml",
                    dir + "addFillValueAttributes.csv"
                  },
                  false); // doIt loop?

      if (results.indexOf("failed") > 0) throw new RuntimeException(results);

      // *** addFillValues finished successfully.
      // The original datasets.xml file is now named
      // /erddapTest/addFillValueAttributes/tempDatasets.xml20200915144031 .
      // The revised datasets.xml file is named
      // /erddapTest/addFillValueAttributes/tempDatasets.xml .
      // The error log file is named addFillValueAttributeErrors20200915144031.txt
      Test.ensureEqual(
          results.indexOf(
              "*** addFillValues finished successfully.\nThe original datasets.xml file is now named"),
          0,
          "");
      Test.ensureTrue(results.indexOf("The revised datasets.xml file is named") > 0, "");
      String logFile =
          String2.extractCaptureGroup(results, "The error log file is named (.*\\.txt)", 1);
      String2.log("logFile=" + logFile);
      String log = File2.readFromFileUtf8(logFile)[1];
      String expected =
          "ERROR on line #3 of addFillValueAttributes file: datasetID=\"noSuchDataset\" wasn't found in datasets.xml!\n"
              + "ERROR on line #4 of addFillValueAttributes file: for datasetID=\"dataset1\", sourceName=\"noSuchVariable\" wasn't found in datasets.xml!\n"
              + "ERROR on line #5 of addFillValueAttributes file: for datasetID=\"dataset1\" sourceName=\"var1\": <addAttributes> wasn't found in datasets.xml!\n";
      Test.ensureEqual(log, expected, "log=\n" + log);

      // check results
      results = File2.readFromFileUtf8(dir + "tempDatasets.xml")[1];
      results =
          results.replaceAll(" \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} ", " [COMPACT_TIME] ");
      expected =
          "<startOfFile (but with nonstandard tag)>\n"
              + "\n"
              + "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"dataset1\" active=\"false\">\n"
              + "    junk line 2\n"
              + "    <dataVariable>\n"
              + "        <sourceName>var1</sourceName>\n"
              + "        <!-- no addAttributes -->\n"
              + "    </dataVariable>\n"
              + "    <addAttributes>\n"
              + "    no closing tag\n"
              + "\n"
              + "</dataset>\n"
              + "\n"
              + "<!-- I removed a lot of unnecessary (for this test) info from this <dataset> definition -->\n"
              + "<dataset type=\"EDDGridFromNcFiles\" datasetID=\"nceiPH53sstd1day\" active=\"true\">\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <!-- sourceAttributes>\n"
              + "    </sourceAttributes -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"title\">AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417&deg;, 1981-present, Daytime (1 Day Composite)</att>\n"
              + "    </addAttributes>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>***replaceFromFileName,timeFormat=yyyyDDD,.*_Pathfinder-PFV5\\.3_NOAA\\d\\d_G_(\\d{7})_day.*\\.nc,1</sourceName>\n"
              + "        <destinationName>time</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"comment\">This is the centered, reference time.</att>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"long_name\">Centered Time</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T12:00:00Z</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lat</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Latitude</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lon</sourceName>\n"
              + "        <destinationName>longitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Longitude</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>sea_surface_temperature</sourceName>\n"
              + "        <destinationName>sea_surface_temperature</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"add_offset\" type=\"double\">0</att>\n"
              + "            <att name=\"ioos_category\">Temperature</att>\n"
              + "            <att name=\"units\">degree_C</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>wind_speed</sourceName>\n"
              + "        <destinationName>windSpeed</destinationName>\n"
              + "        <dataType>byte</dataType>\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"byte\">99</att> <!-- added by addFillValueAttributes at [COMPACT_TIME] -->\n"
              + "            <att name=\"ioos_category\">Wind</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>quality_level</sourceName>\n"
              + "        <destinationName>qualityLevel</destinationName>\n"
              + "        <dataType>byte</dataType>\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"byte\">127</att> <!-- added by addFillValueAttributes at [COMPACT_TIME] -->\n"
              + "            <att name=\"ioos_category\">Quality</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>\n"
              + "\n"
              + "\n"
              + "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"dataset3\">\n"
              + "    junk line 2\n"
              + "    <addAttributes>\n"
              + "    no closing tag\n"
              + "\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lat</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"units\">degrees_north</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Latitude</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>flag</sourceName>\n"
              + "        <destinationName>flag</destinationName>\n"
              + "        <dataType>byte</dataType>\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"byte\">110</att> <!-- added by addFillValueAttributes at [COMPACT_TIME] -->\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>\n"
              + "\n"
              + "<endOfFile (with nonstandard tag)>\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      String2.log("\n*** testAddFillValueAttributes finished successfully.");

    } finally {

      // delete any file other than original datasets.xml and
      // addFillValueAttributes.csv file
      File files[] = new File(dir).listFiles();
      for (int i = 0; i < files.length; i++) {
        String name = files[i].getName();
        if (name.equals("datasets.xml") || name.equals("addFillValueAttributes.csv")) {
        } else {
          File2.delete(dir + name);
        }
      }
    }
  }

  /**
   * This tests adjustNThreads.
   *
   * @throws a RuntimeException if trouble
   */
  @org.junit.jupiter.api.Test
  void testAdjustNThreads() {
    // tnThreads inUse max expected
    Test.ensureEqual(
        EDD.adjustNThreads(0, 0L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 1, "a");

    Test.ensureEqual(
        EDD.adjustNThreads(1, 0L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 1, "b");
    Test.ensureEqual(
        EDD.adjustNThreads(1, 5000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 1, "c");

    Test.ensureEqual(
        EDD.adjustNThreads(10, 0L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 10, "f"); // 0% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        10,
        "g"); // 2% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 600L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        9,
        "h"); // 12% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 1100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        8,
        "h1"); // 22% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 1900L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        7,
        "h2"); // 38% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 2100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        6,
        "i"); // 42% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 3100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        4,
        "i2"); // 62% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 4000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        3,
        "j"); // 80% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 4400L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        2,
        "j"); // 88% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 4600L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        2,
        "k"); // 92% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 5000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "l"); // 100% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(10, 6000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "m"); // 120% of
    // maxMemory/2
    // is inUse

    Test.ensureEqual(
        EDD.adjustNThreads(5, 0L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 5, "f"); // 0% of
    // maxMemory/2 is
    // inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 5, "g"); // 2% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 600L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        5,
        "h"); // 12% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 1100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        4,
        "h1"); // 22% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 1900L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        3,
        "h2"); // 38% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 2100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        3,
        "i"); // 42% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 3100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        3,
        "i2"); // 62% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 4000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        2,
        "j"); // 88% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 4400L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "j2"); // 80% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 4600L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "k"); // 92% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 5000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "l"); // 100% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(5, 6000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "m"); // 120% of
    // maxMemory/2
    // is inUse

    Test.ensureEqual(
        EDD.adjustNThreads(3, 0L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 3, "f"); // 0% of
    // maxMemory/2 is
    // inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB), 3, "g"); // 2% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 600L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        3,
        "h"); // 12% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 1100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        3,
        "h1"); // 22% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 1900L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        2,
        "h2"); // 38% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 2100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        2,
        "i"); // 42% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 3100L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        2,
        "i2"); // 62% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 4000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "j"); // 80% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 4400L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "j2"); // 88% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 4600L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "k"); // 92% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 5000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "l"); // 100% of
    // maxMemory/2
    // is inUse
    Test.ensureEqual(
        EDD.adjustNThreads(3, 6000L * Math2.BytesPerMB, 10000L * Math2.BytesPerMB),
        1,
        "m"); // 120% of
    // maxMemory/2
    // is inUse
  }
}
