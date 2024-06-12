package gov.noaa.pfel.erddap;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoadDatasetsTests {
    LoadDatasets loadDatasets;
    @BeforeAll
    static void init() {
        Initialization.edStatic();
    }

    @Test
    void failedToLoadDatasetsTest() throws Throwable {
        String pathToDatasetsXml = Objects.requireNonNull(LoadDatasets.class.getResource("/datasets/failedToLoadDatasetsTest.xml")).getPath();
        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, File2.getBufferedInputStream(pathToDatasetsXml), true);
        loadDatasets.run();
        assertEquals(EDStatic.datasetsThatFailedToLoad, "n Datasets Failed To Load (in the last major LoadDatasets) = 1\n" +
                                                               "    etopo36, (end)\n");
    }

    @Test
    void duplicateDatasetsTest() throws Throwable {
        String pathToDatasetsXml = Objects.requireNonNull(LoadDatasets.class.getResource("/datasets/duplicateDatasetsTest.xml")).getPath();
        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, File2.getBufferedInputStream(pathToDatasetsXml), true);
        loadDatasets.run();
        assertEquals(EDStatic.errorsDuringMajorReload, "ERROR: Duplicate datasetIDs in datasets.xml:\n" +
                                                              "    etopo180\n");
    }

    @Test
    void numberOfGridAndTableDatasetsTest() throws Throwable {
        String pathToDatasetsXml = Objects.requireNonNull(LoadDatasets.class.getResource("/datasets/numberOfGridAndTableDatasetsTest.xml")).getPath();
        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, File2.getBufferedInputStream(pathToDatasetsXml), true);
        loadDatasets.run();
        System.out.println(EDStatic.nTableDatasets);
        System.out.println(EDStatic.nGridDatasets);
        assertEquals(EDStatic.nTableDatasets - 1, 1); // -1 because Erddap makes a table listing all the datasets
        assertEquals(EDStatic.nGridDatasets, 3);
    }
}
