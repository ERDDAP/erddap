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
  void testZarrArrayMetadataDiscoveryAndGridAxes() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_discovery_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // Create latitude 1D coordinate array
      dev.zarr.zarrjava.v3.Array latArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("latitude"),
          mb -> mb.withShape(3).withDataType(float64).withDimensionNames("latitude"),
          true);
      double[] latVals = new double[] {10.0, 20.0, 30.0};
      latArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {3}, latVals));

      // Create longitude 1D coordinate array
      dev.zarr.zarrjava.v3.Array lonArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("longitude"),
          mb -> mb.withShape(4).withDataType(float64).withDimensionNames("longitude"),
          true);
      double[] lonVals = new double[] {-100.0, -90.0, -80.0, -70.0};
      lonArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {4}, lonVals));

      // Create 2D data array (latitude, longitude)
      dev.zarr.zarrjava.v3.Array tempArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("sst"),
          mb -> mb.withShape(3, 4).withDataType(float64).withDimensionNames("latitude", "longitude"),
          true);

      dev.zarr.zarrjava.core.Attributes sstZattrs = new dev.zarr.zarrjava.core.Attributes();
      sstZattrs.set("ioos_category", "Temperature");
      tempArray.setAttributes(sstZattrs);

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Zarr Discovery Test");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "zarr_discovery_id",
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
              new ArrayList<>(),
              10080,
              0,
              tempDir.toString(),
              "",
              -1,
              -1,
              true);

      assertNotNull(dataset);
      assertEquals("zarr_discovery_id", dataset.datasetID());

      // Verify axis variables
      assertEquals(2, dataset.axisVariables().length);
      assertEquals("latitude", dataset.axisVariables()[0].sourceName());
      assertEquals("longitude", dataset.axisVariables()[1].sourceName());

      // Verify coordinate values loaded into memory
      com.cohort.array.PrimitiveArray latPA = dataset.getAxisData(0);
      assertNotNull(latPA);
      assertEquals(3, latPA.size());
      assertEquals(10.0, latPA.getDouble(0), 1e-6);
      assertEquals(30.0, latPA.getDouble(2), 1e-6);

      // Verify data variables
      assertEquals(1, dataset.dataVariables().length);
      assertEquals("sst", dataset.dataVariables()[0].sourceName());

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testDataFetchingAndChunkSlicing() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_fetch_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // Create latitude 1D coordinate array (size 3)
      dev.zarr.zarrjava.v3.Array latArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("latitude"),
          mb -> mb.withShape(3).withDataType(float64).withDimensionNames("latitude"),
          true);
      double[] latVals = new double[] {10.0, 20.0, 30.0};
      latArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {3}, latVals));

      // Create longitude 1D coordinate array (size 4)
      dev.zarr.zarrjava.v3.Array lonArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("longitude"),
          mb -> mb.withShape(4).withDataType(float64).withDimensionNames("longitude"),
          true);
      double[] lonVals = new double[] {-100.0, -90.0, -80.0, -70.0};
      lonArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {4}, lonVals));

      // Create 2D data array "sst" shape [3, 4] with chunkShape [2, 2]
      dev.zarr.zarrjava.v3.Array sstArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("sst"),
          mb -> mb.withShape(3, 4).withChunkShape(2, 2).withDataType(float64).withDimensionNames("latitude", "longitude"),
          true);

      dev.zarr.zarrjava.core.Attributes sstZattrs = new dev.zarr.zarrjava.core.Attributes();
      sstZattrs.set("ioos_category", "Temperature");
      sstArray.setAttributes(sstZattrs);

      double[] sstVals = new double[] {
          1.0,  2.0,  3.0,  4.0,
          5.0,  6.0,  7.0,  8.0,
          9.0, 10.0, 11.0, 12.0
      };
      sstArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {3, 4}, sstVals));

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Zarr Fetch Test");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "zarr_fetch_id",
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
              new ArrayList<>(),
              10080,
              0,
              tempDir.toString(),
              "",
              -1,
              -1,
              true);

      // 1. Test full contiguous extraction
      com.cohort.array.PrimitiveArray fullSst = dataset.getSourceDataFromFile(
          dataset.dataVariables()[0],
          new int[] {0, 0},
          new int[] {1, 1},
          new int[] {2, 3});

      assertNotNull(fullSst);
      assertEquals(12, fullSst.size());
      for (int i = 0; i < 12; i++) {
        assertEquals(i + 1.0, fullSst.getDouble(i), 1e-6);
      }

      // 2. Test strided extraction (stride 2 along lat and lon)
      com.cohort.array.PrimitiveArray stridedSst = dataset.getSourceDataFromFile(
          dataset.dataVariables()[0],
          new int[] {0, 0},
          new int[] {2, 2},
          new int[] {2, 3});

      assertNotNull(stridedSst);
      assertEquals(4, stridedSst.size());
      // Selected indices: (0,0)=1.0, (0,2)=3.0, (2,0)=9.0, (2,2)=11.0
      assertEquals(1.0, stridedSst.getDouble(0), 1e-6);
      assertEquals(3.0, stridedSst.getDouble(1), 1e-6);
      assertEquals(9.0, stridedSst.getDouble(2), 1e-6);
      assertEquals(11.0, stridedSst.getDouble(3), 1e-6);

      // 3. Test getSourceData full query
      com.cohort.array.IntArray constraints = new com.cohort.array.IntArray(
          new int[] {0, 2, 2, 0, 2, 3});
      com.cohort.array.PrimitiveArray[] res = dataset.getSourceData(0, null, null, dataset.dataVariables(), constraints);

      assertNotNull(res);
      assertEquals(3, res.length); // lat, lon, sst

      // lat axis subset [0, 2, 2] -> 10.0, 30.0
      assertEquals(2, res[0].size());
      assertEquals(10.0, res[0].getDouble(0), 1e-6);
      assertEquals(30.0, res[0].getDouble(1), 1e-6);

      // lon axis subset [0, 2, 3] -> -100.0, -80.0
      assertEquals(2, res[1].size());
      assertEquals(-100.0, res[1].getDouble(0), 1e-6);
      assertEquals(-80.0, res[1].getDouble(1), 1e-6);

      // sst data variable
      assertEquals(4, res[2].size());
      assertEquals(1.0, res[2].getDouble(0), 1e-6);
      assertEquals(3.0, res[2].getDouble(1), 1e-6);

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testUnpackingAndMissingChunkHandling() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_unpack_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;
      dev.zarr.zarrjava.v3.DataType int16 = dev.zarr.zarrjava.v3.DataType.INT16;

      // Coordinate axes
      dev.zarr.zarrjava.v3.Array latArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("latitude"),
          mb -> mb.withShape(2).withDataType(float64).withDimensionNames("latitude"),
          true);
      latArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {0.0, 10.0}));

      dev.zarr.zarrjava.v3.Array lonArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("longitude"),
          mb -> mb.withShape(2).withDataType(float64).withDimensionNames("longitude"),
          true);
      lonArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {100.0, 110.0}));

      // Array with scale_factor and add_offset attributes
      dev.zarr.zarrjava.v3.Array packedArray = dev.zarr.zarrjava.v3.Array.create(
          store.resolve("packed_temp"),
          mb -> mb.withShape(2, 2).withChunkShape(2, 2).withDataType(int16).withDimensionNames("latitude", "longitude"),
          true);

      dev.zarr.zarrjava.core.Attributes atts = new dev.zarr.zarrjava.core.Attributes();
      atts.set("scale_factor", 0.1);
      atts.set("add_offset", 10.0);
      atts.set("_FillValue", -999);
      atts.set("ioos_category", "Temperature");
      packedArray.setAttributes(atts);

      short[] rawVals = new short[] {0, 10, -999, 50};
      packedArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.SHORT, new int[] {2, 2}, rawVals));

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Zarr Unpack Test");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "zarr_unpack_id",
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
              new ArrayList<>(),
              10080,
              0,
              tempDir.toString(),
              "",
              -1,
              -1,
              true);

      // Fetch packed data with auto unpacking
      com.cohort.array.PrimitiveArray res = dataset.getSourceDataFromFile(
          dataset.dataVariables()[0],
          new int[] {0, 0},
          new int[] {1, 1},
          new int[] {1, 1});

      assertNotNull(res);
      assertEquals(4, res.size());

      // raw 0 -> 0 * 0.1 + 10 = 10.0
      assertEquals(10.0, res.getDouble(0), 1e-6);
      // raw 10 -> 10 * 0.1 + 10 = 11.0
      assertEquals(11.0, res.getDouble(1), 1e-6);
      // raw -999 (_FillValue) -> NaN
      assertTrue(Double.isNaN(res.getDouble(2)));
      // raw 50 -> 50 * 0.1 + 10 = 15.0
      assertEquals(15.0, res.getDouble(3), 1e-6);

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testRemainingStubsThrowUnsupportedOperationException() throws Throwable {
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

      LocalizedAttributes sstAtts = new LocalizedAttributes();
      sstAtts.set(0, "ioos_category", "Temperature");
      List<DataVariableInfo> dataVars = new ArrayList<>();
      dataVars.add(new DataVariableInfo("sst", "sst", sstAtts, "double"));

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

      assertThrows(UnsupportedOperationException.class, () -> EDDGridFromZarr.generateDatasetsXml(tempDir.toString(), ""));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }
}
