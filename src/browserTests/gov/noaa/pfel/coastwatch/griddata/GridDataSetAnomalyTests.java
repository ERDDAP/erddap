package gov.noaa.pfel.coastwatch.griddata;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import tags.TagThredds;

class GridDataSetAnomalyTests {

  @TempDir
  private static Path TEMP_DIR;

  /**
   * This tests this class.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void basicTest() throws Exception {
    String2.log("\n*** GridDataSetAnomaly.basicTest   NOTE: this requires data in C:/u00/data/QC/mday/grd/ ");
    FileNameUtility.verbose = true;
    FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");
    Grid.verbose = true;
    GridDataSet.verbose = true;
    Opendap.verbose = true;
    String dir = TEMP_DIR.toAbsolutePath().toString();
    File2.deleteAllFiles(dir);

    // make the regular, climatology and anomaly datasets
    GridDataSet gridDataSet = new GridDataSetThredds(fnu, "TQSux10",
        // was "https://oceanwatch.pfeg.noaa.gov/thredds/Satellite/aggregsatQS/ux10/",
        // //was :8081
        "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatQS/ux10/",
        "BlueWhiteRed", "Linear", "-10", "10", -1, "", null, null, "S", 1, 1, "", 1, 1);
    GridDataSetCWLocalClimatology climatologyDataSet = new GridDataSetCWLocalClimatology(
        fnu, "CQCux10", "c:/u00/data/", dir);
    GridDataSetAnomaly anomalyDataSet = new GridDataSetAnomaly(
        "AQSuxan", fnu, gridDataSet, climatologyDataSet,
        "Wind Anomaly, QuikSCAT SeaWinds, 0.25 degrees, Global, Science Quality, Zonal",
        "*", "1");

    // get data from each dataset
    String timePeriod = "1 month";
    String centeredTime = "2006-01-16 12:00:00";
    String climatologyCenteredTime = "0001-01-16 12:00:00";

    Grid gdsGrid = gridDataSet.makeGrid(timePeriod, centeredTime,
        -135, -135, 22, 51, 1, 29);
    String2.log("\ngdsGrid lon(" + gdsGrid.lon.length + ")=" + String2.toCSSVString(gdsGrid.lon) +
        "\n  lat(" + gdsGrid.lat.length + ")=" + String2.toCSSVString(gdsGrid.lat) +
        "\n  data(" + gdsGrid.data.length + ")=" + String2.toCSSVString(gdsGrid.data));

    Grid climatologyGrid = climatologyDataSet.makeGrid("monthly", climatologyCenteredTime,
        -135, -135, 22, 51, 1, 29);
    String2
        .log("\nclimatologyGrid lon(" + climatologyGrid.lon.length + ")=" + String2.toCSSVString(climatologyGrid.lon) +
            "\n  lat(" + climatologyGrid.lat.length + ")=" + String2.toCSSVString(climatologyGrid.lat) +
            "\n  data(" + climatologyGrid.data.length + ")=" + String2.toCSSVString(climatologyGrid.data));

    Grid anomalyGrid = anomalyDataSet.makeGrid(timePeriod, centeredTime,
        -135, -135, 22, 51, 1, 29);
    String2.log("\nanomalyGrid lon(" + anomalyGrid.lon.length + ")=" + String2.toCSSVString(anomalyGrid.lon) +
        "\n  lat(" + anomalyGrid.lat.length + ")=" + String2.toCSSVString(anomalyGrid.lat) +
        "\n  data(" + anomalyGrid.data.length + ")=" + String2.toCSSVString(anomalyGrid.data));

    Test.ensureEqual(anomalyGrid.lon.length, 1, "");
    Test.ensureEqual(anomalyGrid.lon[0], -135, "");
    Test.ensureEqual(gdsGrid.lon, anomalyGrid.lon, "");
    Test.ensureEqual(climatologyGrid.lon, anomalyGrid.lon, "");

    Test.ensureEqual(anomalyGrid.lat.length, 30, "");
    Test.ensureEqual(anomalyGrid.lat[0], 22, "");
    Test.ensureEqual(anomalyGrid.lat[29], 51, "");
    Test.ensureEqual(gdsGrid.lat, anomalyGrid.lat, "");
    Test.ensureEqual(climatologyGrid.lat, anomalyGrid.lat, "");

    for (int i = 0; i < 29; i++) {
      // I had some problems, so as additional test,
      // get the climatology points 1 by 1 (which avoids possible stride-related
      // problems)
      // and ensure they match
      Grid cGrid1 = climatologyDataSet.makeGrid("monthly", climatologyCenteredTime,
          -135, -135, 22 + i, 22 + i, 1, 1);
      Test.ensureEqual(climatologyGrid.data[i], cGrid1.data[0], "i=" + i);
    }

    for (int i = 0; i < 29; i++) {
      String2.log("data[" + i + "] gds=" + gdsGrid.data[i] +
          " cli=" + climatologyGrid.data[i] + " ano=" + anomalyGrid.data[i]);
      Test.ensureEqual((float) (gdsGrid.data[i] - climatologyGrid.data[i]), (float) anomalyGrid.data[i], "i=" + i);
    }

    /*
     * data[0] gds=-6.918294429779053 cli=-6.036570072174072 ano=-0.8817243576049805
     * data[28] gds=3.990844964981079 cli=1.5286099910736084 ano=2.4622349739074707
     */

    Test.ensureEqual(gdsGrid.data[0], -6.918294429779053, "");
    Test.ensureEqual(climatologyGrid.data[0], -6.036570072174072, "");
    Test.ensureEqual(anomalyGrid.data[0], -0.8817243576049805, "");

    Test.ensureEqual(gdsGrid.data[28], 3.990844964981079, "");
    Test.ensureEqual(climatologyGrid.data[28], 1.5286099910736084, "");
    Test.ensureEqual(anomalyGrid.data[28], 2.4622349739074707, "");

  }

}
