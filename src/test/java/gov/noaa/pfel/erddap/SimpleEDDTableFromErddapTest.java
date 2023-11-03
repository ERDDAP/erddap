package gov.noaa.pfel.erddap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.cohort.array.ByteArray;
import com.cohort.util.String2;

import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;

public class SimpleEDDTableFromErddapTest {
    @Test
    public void testEDDTableFromErddapBadConfig() throws Throwable {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            EDD.oneFromXmlFragment(null, """
                <dataset type="EDDTableFromErddap" datasetID="some_dataset" active="true">
                    <badOption>fail</badOption>
                </dataset>
            """);
        });
        assertTrue(thrown.getMessage().contains("datasets.xml error"));
        assertTrue(thrown.getMessage().contains("Unexpected tag=<erddapDatasets><dataset><badOption>"));
    }

    @Category(gov.noaa.pfel.erddap.categories.RemoteTest.class)
    @Test
    public void testRemoteDataRequest() throws Throwable {
        EDD edd = EDD.oneFromXmlFragment(null, """
            <dataset type="EDDTableFromErddap" datasetID="cwwcNDBCMet" active="true">
                <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet</sourceUrl>
            </dataset>
        """);
        assertNotNull(edd);

        String query = "latitude,longitude,time,atmp,wtmp";
        query += "&time>=2016-05-14T00:00:00Z&time<=2016-05-14T00:06:00Z";
        query += "&latitude>=30&latitude<=40";
        query += "&longitude>=-80&longitude<=-70";

        String tName = edd.makeNewFileForDapQuery(0, null, null, URLEncoder.encode(query, StandardCharsets.ISO_8859_1),
                EDStatic.fullTestCacheDirectory, edd.className() + "_" + edd.datasetID(), ".csv");
        String results = new String(ByteArray.fromFile(EDStatic.fullTestCacheDirectory + tName).toArray());
        String2.log(results);
   }
}
