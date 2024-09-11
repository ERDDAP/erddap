package gov.noaa.pfel.erddap.util;

import com.cohort.array.Attributes;
import com.cohort.util.String2;
import com.cohort.util.Test;

class EDStaticTests {
  @org.junit.jupiter.api.Test
  void testUpdateUrls() throws Exception {
    String2.log("\n***** EDStatic.testUpdateUrls");
    Attributes source = new Attributes();
    Attributes add = new Attributes();
    source.set("a", "http://coastwatch.pfel.noaa.gov"); // purposely out-of-date
    source.set("nine", 9.0);
    add.set("b", "http://www.whoi.edu"); // purposely out-of-date
    add.set("ten", 10.0);
    add.set("sourceUrl", "http://coastwatch.pfel.noaa.gov"); // purposely out-of-date
    EDStatic.updateUrls(source, add);
    String results = add.toString();
    String expected =
        "    a=https://coastwatch.pfeg.noaa.gov\n"
            + "    b=https://www.whoi.edu\n"
            + "    sourceUrl=http://coastwatch.pfel.noaa.gov\n"
            + // unchanged
            "    ten=10.0d\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    source = new Attributes();
    add = new Attributes();
    add.set("a", "http://coastwatch.pfel.noaa.gov");
    add.set("b", "http://www.whoi.edu");
    add.set("nine", 9.0);
    add.set("sourceUrl", "http://coastwatch.pfel.noaa.gov");
    EDStatic.updateUrls(null, add);
    results = add.toString();
    expected =
        "    a=https://coastwatch.pfeg.noaa.gov\n"
            + "    b=https://www.whoi.edu\n"
            + "    nine=9.0d\n"
            + "    sourceUrl=http://coastwatch.pfel.noaa.gov\n"; // unchanged
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
