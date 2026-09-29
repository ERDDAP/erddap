package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.variable.AxisVariableInfo;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

class EDDGridFromZarrTests {

  @Test
  void testZarrAttributeConversion() {
    dev.zarr.zarrjava.core.Attributes zattrs = new dev.zarr.zarrjava.core.Attributes();
    zattrs.set("title", "Test Zarr Dataset");
    zattrs.set("history", "Created by test");
    zattrs.set("value", 42.5);
    zattrs.set("count", 100);
    zattrs.set("isActive", true);

    Attributes erddapAtts = new Attributes();
    EDDGridFromZarr.populateAttributesFromZarr(zattrs, erddapAtts);

    assertEquals("Test Zarr Dataset", erddapAtts.getString("title"));
    assertEquals("Created by test", erddapAtts.getString("history"));
    assertEquals(42.5, erddapAtts.getDouble("value"), 1e-6);
    assertEquals(100, erddapAtts.getInt("count"));
    assertEquals("true", erddapAtts.getString("isActive"));
  }

  @Test
  void testConstructorInitializationWithLocalStore() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_test_store");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      String datasetID = "test_zarr_dataset";
      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Test Zarr Dataset Title");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      LocalizedAttributes varAtts = new LocalizedAttributes();
      varAtts.set(0, "ioos_category", "Temperature");

      List<AxisVariableInfo> axisVars = new ArrayList<>();
      List<DataVariableInfo> dataVars = new ArrayList<>();
      dataVars.add(new DataVariableInfo("temperature", "temperature", varAtts, "double"));

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              datasetID,
              null,
              null,
              true,
              new StringArray(),
              null,
              null,
              null,
              null,
              addGlobalAtts,
              axisVars,
              dataVars,
              10080,
              0,
              tempDir.toString(),
              "",
              -1,
              -1,
              true);

      assertEquals("test_zarr_dataset", dataset.datasetID());
      assertEquals("EDDGridFromZarr", dataset.className());
      assertEquals("Test Zarr Dataset Title", dataset.combinedGlobalAttributes().getString(0, "title"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testFromXml() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_xml_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<dataset type=\"EDDGridFromZarr\" datasetID=\"zarr_xml_id\">\n"
              + "    <zarrStorePath>" + tempDir.toString().replace('\\', '/') + "</zarrStorePath>\n"
              + "    <zarrGroupName></zarrGroupName>\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <addAttributes>\n"
              + "        <att name=\"title\">XML Zarr Dataset</att>\n"
              + "        <att name=\"summary\">XML Summary</att>\n"
              + "        <att name=\"institution\">NOAA</att>\n"
              + "        <att name=\"infoUrl\">https://example.org</att>\n"
              + "    </addAttributes>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>temp</sourceName>\n"
              + "        <destinationName>temperature</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Temperature</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>";

      SimpleXMLReader xmlReader = new SimpleXMLReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "dataset");

      EDDGridFromZarr dataset = EDDGridFromZarr.fromXml(null, xmlReader);

      assertNotNull(dataset);
      assertEquals("zarr_xml_id", dataset.datasetID());
      assertEquals("XML Zarr Dataset", dataset.combinedGlobalAttributes().getString(0, "title"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testStubsThrowUnsupportedOperationException() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_stub_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Test Stubs");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      LocalizedAttributes varAtts = new LocalizedAttributes();
      varAtts.set(0, "ioos_category", "Temperature");

      List<DataVariableInfo> dataVars = new ArrayList<>();
      dataVars.add(new DataVariableInfo("temp", "temp", varAtts, "double"));

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "stub_test",
              null,
              null,
              true,
              new StringArray(),
              null,
              null,
              null,
              null,
              addGlobalAtts,
              new ArrayList<>(),
              dataVars,
              10080,
              0,
              tempDir.toString(),
              "",
              -1,
              -1,
              true);

      assertThrows(UnsupportedOperationException.class, () -> dataset.getAxisData(0));
      assertThrows(UnsupportedOperationException.class, () -> dataset.getSourceData(0, null, null, null, null));
      assertThrows(UnsupportedOperationException.class, () -> EDDGridFromZarr.generateDatasetsXml(tempDir.toString(), ""));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }
}
