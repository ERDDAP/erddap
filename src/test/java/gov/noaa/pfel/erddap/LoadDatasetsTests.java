package gov.noaa.pfel.erddap;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoadDatasetsTests {
    LoadDatasets loadDatasets;
    @BeforeAll
    static void init() {
        Initialization.edStatic();
    }

    @Test
    void failedToLoadDatasetsTest() throws Throwable {
        InputStream inputStream = new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                        "<erddapDatasets>\n" +
                        "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo180\" />\n" +
                        "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo360\" />\n"+
                        "</erddapDatasets>\n" +
                        ""));

        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, inputStream, true);
        loadDatasets.run();
        assertEquals(EDStatic.datasetsThatFailedToLoad, "n Datasets Failed To Load (in the last major LoadDatasets) = 0\n");

        inputStream = new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                        "<erddapDatasets>\n" +
                        "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo180\" />\n" +
                        "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo36\" />\n"+ //Made a typo in the ID here so the dataset fails to load
                        "</erddapDatasets>\n" +
                        ""));

        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, inputStream, true);
        loadDatasets.run();
        assertEquals(EDStatic.datasetsThatFailedToLoad, "n Datasets Failed To Load (in the last major LoadDatasets) = 1\n" +
                                                               "    etopo36, (end)\n");
    }

    @Test
    void duplicateDatasetsTest() throws Throwable {
        InputStream inputStream = new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                        "<erddapDatasets>\n" +
                        "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo180\" />\n" +
                        "<dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo180\" />\n"+
                        "</erddapDatasets>\n" +
                        ""));

        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, inputStream, true);
        loadDatasets.run();
        assertEquals(EDStatic.errorsDuringMajorReload, "ERROR: Duplicate datasetIDs in datasets.xml:\n" +
                                                              "    etopo180\n");
    }

//    @Test
//    void numberOfGridAndTableDatasetsTest() throws Throwable {
//        InputStream inputStream = new ByteArrayInputStream(String2.toByteArray(
//                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
//                        "<erddapDatasets>\n" +
//                        "</erddapDatasets>\n"
//                        ));
//
//        loadDatasets = new LoadDatasets(new Erddap(), EDStatic.datasetsRegex, inputStream, true);
//        loadDatasets.run();
//        System.out.println(EDStatic.nTableDatasets);
//        System.out.println(EDStatic.nGridDatasets);
//        assertEquals(1 + 1, 2);
//
//    }
}
