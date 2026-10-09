package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.EDDTestDataset;
import testDataset.Initialization;
import testSupport.WireMockLifecycle;

public class NcoJsonHeaderFilesTests extends WireMockLifecycle {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @Test
  void testTableNcoJsonHeader() throws Throwable {
    int language = 0;
    String tName, results;

    EDD edd = EDDTestDataset.gettestJsonlCSV();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncoJsonHeader");
    results = File2.directReadFromUtf8File(EDStatic.config.fullTestCacheDirectory + tName);
    // String2.log(results);

    // Validate JSON validity
    JSONObject json = new JSONObject(results);

    // Verify metadata is present
    com.cohort.util.Test.ensureTrue(json.has("attributes"), "Attributes missing");
    com.cohort.util.Test.ensureEqual(
        json.getJSONObject("attributes").getJSONObject("title").getString("data"),
        "Test of JSON Lines CSV",
        "Title attribute incorrect");

    // Verify dimensions are present and accurate
    com.cohort.util.Test.ensureTrue(json.has("dimensions"), "Dimensions missing");
    com.cohort.util.Test.ensureEqual(
        json.getJSONObject("dimensions").getInt("row"), 6, "Row dimension incorrect");
    com.cohort.util.Test.ensureEqual(
        json.getJSONObject("dimensions").getInt("ship_strlen"),
        15,
        "ship_strlen dimension incorrect");

    // Verify variables are present
    com.cohort.util.Test.ensureTrue(json.has("variables"), "Variables missing");
    JSONObject variables = json.getJSONObject("variables");
    com.cohort.util.Test.ensureTrue(variables.has("ship"), "Variable 'ship' missing");

    // Verify data is NOT present in variables
    com.cohort.util.Test.ensureTrue(
        !variables.getJSONObject("ship").has("data"), "Variable 'ship' should not have data");
    com.cohort.util.Test.ensureTrue(
        !variables.getJSONObject("time").has("data"), "Variable 'time' should not have data");
  }

  @Test
  void testGridNcoJsonHeader() throws Throwable {
    int language = 0;
    String tName, results;

    EDD edd = EDDTestDataset.gettestGriddedNcFiles();
    // Use a simple query that we can easily predict dimensions for
    // longitude is 0.125 to 359.875, resolution 0.25. 1440 points.
    // [0:2] is indices 0, 1, 2 -> 3 points.
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "y_wind[0][0][0][0:2]",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncoJsonHeader");
    results = File2.directReadFromUtf8File(EDStatic.config.fullTestCacheDirectory + tName);
    // String2.log(results);

    // Validate JSON validity
    JSONObject json = new JSONObject(results);

    // Verify metadata
    com.cohort.util.Test.ensureEqual(
        json.getJSONObject("attributes").getJSONObject("title").getString("data"),
        "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)",
        "Title incorrect");

    // Verify dimensions
    JSONObject dimensions = json.getJSONObject("dimensions");
    com.cohort.util.Test.ensureEqual(dimensions.getInt("time"), 1, "time dimension incorrect");
    com.cohort.util.Test.ensureEqual(
        dimensions.getInt("altitude"), 1, "altitude dimension incorrect");
    com.cohort.util.Test.ensureEqual(
        dimensions.getInt("latitude"), 1, "latitude dimension incorrect");
    com.cohort.util.Test.ensureEqual(
        dimensions.getInt("longitude"), 3, "longitude dimension incorrect");

    // Verify variables
    JSONObject variables = json.getJSONObject("variables");
    com.cohort.util.Test.ensureTrue(variables.has("y_wind"), "Variable 'y_wind' missing");

    // Verify no data for variables
    com.cohort.util.Test.ensureTrue(
        !variables.getJSONObject("y_wind").has("data"), "Variable 'y_wind' should not have data");
    com.cohort.util.Test.ensureTrue(
        !variables.getJSONObject("latitude").has("data"),
        "Variable 'latitude' should not have data");
  }
}
