package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class EDVTimeStampTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This is a unit test. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    // verbose = true;

    // ***with Z
    String2.log("\n*** EDVTimeStamp.basicTest with Z");
    EDVTimeStamp eta =
        new EDVTimeStamp(
            "sampleDatasetID",
            "sourceName",
            "time",
            null,
            new Attributes()
                .add("units", "yyyy-MM-dd'T'HH:mm:ssXXX") // was Calendar2.ISO8601TZ_FORMAT with 'Z'
                .add(
                    "actual_range",
                    new StringArray(new String[] {"1970-01-01T00:00:00Z", "2007-01-01T00:00:00Z"})),
            "String"); // this constructor gets source / sets destination actual_range

    // test 'Z'
    String t1 = "2007-01-02T03:04:05Z";
    double d = eta.sourceTimeToEpochSeconds(t1);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(d), t1, "a1");
    Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t1, "a2");

    // test -01:00
    String t2 = "2007-01-02T02:04:05-01:00";
    d = eta.sourceTimeToEpochSeconds(t2);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(d), t1, "b1");
    Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t1, "b2");

    // ***with 3Z
    String2.log("\n*** EDVTimeStamp.test with 3Z");
    eta =
        new EDVTimeStamp(
            "sampleDatasetID",
            "sourceName",
            "time",
            null,
            new Attributes()
                .add("units", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                . // was Calendar2.ISO8601T3Z_FORMAT).
                add(
                    "actual_range",
                    new StringArray(
                        new String[] {"1970-01-01T00:00:00.000Z", "2007-01-01T00:00:00.000Z"})),
            "String");

    // test 'Z'
    String t13 = "2007-01-02T03:04:05.123Z";
    d = eta.sourceTimeToEpochSeconds(t13);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringT3Z(d), t13, "a1");
    Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t13, "a2");

    // test -01:00
    String t23 = "2007-01-02T02:04:05.123-01:00";
    d = eta.sourceTimeToEpochSeconds(t23);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringT3Z(d), t13, "b1");
    Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t13, "b2");

    // *** no Z
    String2.log("\n*** EDVTimeStamp.test no Z");
    eta =
        new EDVTimeStamp(
            "sampleDatasetID",
            "sourceName",
            "myTimeStamp",
            null,
            new Attributes()
                .add("units", Calendar2.ISO8601T_FORMAT)
                . // without Z
                add(
                    "actual_range",
                    new StringArray(
                        new String[] {"1970-01-01T00:00:00", "2007-01-01T00:00:00"})), // without Z
            "String");

    // test no suffix
    String t4 = "2007-01-02T03:04:05"; // without Z
    d = eta.sourceTimeToEpochSeconds(t4);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(d), t1, "b1");
    Test.ensureEqual(eta.epochSecondsToSourceTimeString(d) + "Z", t1, "b2");

    // *** 3, no Z
    String2.log("\n*** EDVTimeStamp.test 3, no Z");
    eta =
        new EDVTimeStamp(
            "sampleDatasetID",
            "sourceName",
            "myTimeStamp",
            null,
            new Attributes()
                .add("units", Calendar2.ISO8601T3_FORMAT)
                . // without Z
                add(
                    "actual_range",
                    new StringArray(
                        new String[] {
                          "1970-01-01T00:00:00.000", "2007-01-01T00:00:00.000"
                        })), // without Z
            "String");

    // test no suffix
    t4 = "2007-01-02T03:04:05.123"; // without Z
    d = eta.sourceTimeToEpochSeconds(t4);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringT3Z(d), t13, "b1");
    Test.ensureEqual(eta.epochSecondsToSourceTimeString(d) + "Z", t13, "b2");
  }
}
