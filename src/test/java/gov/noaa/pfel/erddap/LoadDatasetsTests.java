package gov.noaa.pfel.erddap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

public class LoadDatasetsTests {
  LoadDatasets loadDatasets;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @Test
  @SuppressWarnings("DoNotCall")
  void failedToLoadDatasetsTest() throws Throwable {
    String pathToDatasetsXml =
        Objects.requireNonNull(
                LoadDatasets.class.getResource("/datasets/failedToLoadDatasetsTest.xml"))
            .getPath();
    loadDatasets =
        new LoadDatasets(
            new Erddap(),
            EDStatic.datasetsRegex,
            File2.getBufferedInputStream(pathToDatasetsXml),
            true);
    loadDatasets.run();
    assertEquals(
        EDStatic.datasetsThatFailedToLoad,
        "n Datasets Failed To Load (in the last major LoadDatasets) = 1\n"
            + "    etopo36, (end)\n");
  }

  @Test
  @SuppressWarnings("DoNotCall")
  void duplicateDatasetsTest() throws Throwable {
    String pathToDatasetsXml =
        Objects.requireNonNull(
                LoadDatasets.class.getResource("/datasets/duplicateDatasetsTest.xml"))
            .getPath();
    loadDatasets =
        new LoadDatasets(
            new Erddap(),
            EDStatic.datasetsRegex,
            File2.getBufferedInputStream(pathToDatasetsXml),
            true);
    loadDatasets.run();
    assertEquals(
        EDStatic.errorsDuringMajorReload,
        "ERROR: Duplicate datasetIDs in datasets.xml:\n" + "    etopo180\n");
  }
}
