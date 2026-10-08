package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.variable.AxisVariableInfo;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import testDataset.EDDTestDataset;
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
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
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
      assertEquals(
          "Test Zarr Dataset Title", dataset.combinedGlobalAttributes().getString(0, "title"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testFromXml() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_xml_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<dataset type=\"EDDGridFromZarr\" datasetID=\"zarr_xml_id\">\n"
              + "    <zarrStorePath>"
              + tempDir.toString().replace('\\', '/')
              + "</zarrStorePath>\n"
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

      SimpleXMLReader xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "dataset");

      EDDGridFromZarr dataset = EDDGridFromZarr.fromXml(null, xmlReader);

      assertNotNull(dataset);
      assertEquals("zarr_xml_id", dataset.datasetID());
      assertEquals("XML Zarr Dataset", dataset.combinedGlobalAttributes().getString(0, "title"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testZarrV2VsV3Compatibility() throws Throwable {
    Initialization.edStatic();
    Path tempDirV3 = Files.createTempDirectory("zarr_v3_test");
    Path tempDirV2 = Files.createTempDirectory("zarr_v2_test");

    try {
      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // 1. Zarr v3 Store with native dimensionNames
      dev.zarr.zarrjava.store.FilesystemStore storeV3 =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDirV3);
      dev.zarr.zarrjava.v3.Group.create(storeV3.resolve());

      dev.zarr.zarrjava.v3.Array.create(
              storeV3.resolve("lat"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("lat"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {10.0, 20.0}));

      dev.zarr.zarrjava.v3.Array.create(
              storeV3.resolve("lon"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("lon"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {100.0, 110.0}));

      dev.zarr.zarrjava.v3.Array.create(
              storeV3.resolve("temp"),
              mb -> mb.withShape(2, 2).withDataType(float64).withDimensionNames("lat", "lon"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2, 2}, new double[] {1.0, 2.0, 3.0, 4.0}));

      String xmlV3 = EDDGridFromZarr.generateDatasetsXml(tempDirV3.toString(), "");
      assertTrue(xmlV3.contains("<dataset type=\"EDDGridFromZarr\""));
      assertTrue(xmlV3.contains("lat"));
      assertTrue(xmlV3.contains("lon"));
      assertTrue(xmlV3.contains("temp"));

      // 2. Zarr v2 Store with _ARRAY_DIMENSIONS attributes
      dev.zarr.zarrjava.store.FilesystemStore storeV2 =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDirV2);
      dev.zarr.zarrjava.v3.Group.create(storeV2.resolve());

      dev.zarr.zarrjava.v3.Array latV2 =
          dev.zarr.zarrjava.v3.Array.create(
              storeV2.resolve("latitude"), mb -> mb.withShape(2).withDataType(float64), true);
      dev.zarr.zarrjava.core.Attributes latAtts = new dev.zarr.zarrjava.core.Attributes();
      latAtts.set("_ARRAY_DIMENSIONS", new String[] {"latitude"});
      latV2.setAttributes(latAtts);
      latV2.write(
          ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {0.0, 5.0}));

      dev.zarr.zarrjava.v3.Array lonV2 =
          dev.zarr.zarrjava.v3.Array.create(
              storeV2.resolve("longitude"), mb -> mb.withShape(2).withDataType(float64), true);
      dev.zarr.zarrjava.core.Attributes lonAtts = new dev.zarr.zarrjava.core.Attributes();
      lonAtts.set("_ARRAY_DIMENSIONS", new String[] {"longitude"});
      lonV2.setAttributes(lonAtts);
      lonV2.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {-180.0, -170.0}));

      dev.zarr.zarrjava.v3.Array sstV2 =
          dev.zarr.zarrjava.v3.Array.create(
              storeV2.resolve("sst"), mb -> mb.withShape(2, 2).withDataType(float64), true);
      dev.zarr.zarrjava.core.Attributes sstAtts = new dev.zarr.zarrjava.core.Attributes();
      sstAtts.set("_ARRAY_DIMENSIONS", new String[] {"latitude", "longitude"});
      sstAtts.set("ioos_category", "Temperature");
      sstV2.setAttributes(sstAtts);
      sstV2.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2, 2}, new double[] {15.0, 16.0, 17.0, 18.0}));

      String xmlV2 = EDDGridFromZarr.generateDatasetsXml(tempDirV2.toString(), "");
      assertTrue(xmlV2.contains("<dataset type=\"EDDGridFromZarr\""));
      assertTrue(xmlV2.contains("latitude"));
      assertTrue(xmlV2.contains("longitude"));
      assertTrue(xmlV2.contains("sst"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDirV3.toString(), true, true);
      com.cohort.util.File2.deleteAllFiles(tempDirV2.toString(), true, true);
    }
  }

  @Test
  void testCoordinateAxisAndAttributeVerification() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_axes_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group g = dev.zarr.zarrjava.v3.Group.create(store.resolve());
      dev.zarr.zarrjava.core.Attributes gAtts = new dev.zarr.zarrjava.core.Attributes();
      gAtts.set("title", "Global Axes Test");
      gAtts.set("institution", "NOAA PMEL");
      g.setAttributes(gAtts);

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array timeArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("time"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("time"),
              true);
      dev.zarr.zarrjava.core.Attributes timeAtts = new dev.zarr.zarrjava.core.Attributes();
      timeAtts.set("units", "seconds since 1970-01-01T00:00:00Z");
      timeArray.setAttributes(timeAtts);
      timeArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {1000.0, 2000.0}));

      dev.zarr.zarrjava.v3.Array depthArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("depth"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("depth"),
              true);
      dev.zarr.zarrjava.core.Attributes depthAtts = new dev.zarr.zarrjava.core.Attributes();
      depthAtts.set("units", "m");
      depthAtts.set("positive", "down");
      depthArray.setAttributes(depthAtts);
      depthArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {0.0, 10.0}));

      dev.zarr.zarrjava.v3.Array latArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("latitude"),
              true);
      latArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {-10.0, 10.0}));

      dev.zarr.zarrjava.v3.Array lonArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("longitude"),
              true);
      lonArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {120.0, 130.0}));

      dev.zarr.zarrjava.v3.Array tempArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temperature"),
              mb ->
                  mb.withShape(2, 2, 2, 2)
                      .withDataType(float64)
                      .withDimensionNames("time", "depth", "latitude", "longitude"),
              true);
      dev.zarr.zarrjava.core.Attributes tempAtts = new dev.zarr.zarrjava.core.Attributes();
      tempAtts.set("ioos_category", "Temperature");
      tempAtts.set("units", "degree_C");
      tempArray.setAttributes(tempAtts);

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "summary", "Axes Summary");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "zarr_axes_id",
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
      assertEquals(4, dataset.axisVariables().length);
      assertEquals("time", dataset.axisVariables()[0].sourceName());
      assertEquals("depth", dataset.axisVariables()[1].sourceName());
      assertEquals("latitude", dataset.axisVariables()[2].sourceName());
      assertEquals("longitude", dataset.axisVariables()[3].sourceName());

      assertEquals(1000.0, dataset.getAxisData(0).getDouble(0), 1e-6);
      assertEquals(2000.0, dataset.getAxisData(0).getDouble(1), 1e-6);
      assertEquals(0.0, dataset.getAxisData(1).getDouble(0), 1e-6);
      assertEquals(10.0, dataset.getAxisData(1).getDouble(1), 1e-6);

      assertEquals(1, dataset.dataVariables().length);
      assertEquals("temperature", dataset.dataVariables()[0].sourceName());

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testDataSlicingAndStridedExtraction() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_slicing_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array latArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(4).withDataType(float64).withDimensionNames("latitude"),
              true);
      latArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {4}, new double[] {0, 10, 20, 30}));

      dev.zarr.zarrjava.v3.Array lonArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(6).withDataType(float64).withDimensionNames("longitude"),
              true);
      lonArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE,
              new int[] {6},
              new double[] {100, 101, 102, 103, 104, 105}));

      dev.zarr.zarrjava.v3.Array dataArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("salinity"),
              mb ->
                  mb.withShape(4, 6)
                      .withChunkShape(2, 2)
                      .withDataType(float64)
                      .withDimensionNames("latitude", "longitude"),
              true);

      dev.zarr.zarrjava.core.Attributes salAtts = new dev.zarr.zarrjava.core.Attributes();
      salAtts.set("ioos_category", "Salinity");
      dataArray.setAttributes(salAtts);

      double[] vals = new double[24];
      for (int i = 0; i < 24; i++) vals[i] = 30.0 + i;
      dataArray.write(ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[] {4, 6}, vals));

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Slicing Test");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "zarr_slicing_id",
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

      com.cohort.array.PrimitiveArray res =
          dataset.getSourceDataFromFile(
              dataset.dataVariables()[0], new int[] {1, 0}, new int[] {2, 3}, new int[] {3, 5});

      assertNotNull(res);
      assertEquals(4, res.size());
      assertEquals(30.0 + (1 * 6 + 0), res.getDouble(0), 1e-6);
      assertEquals(30.0 + (1 * 6 + 3), res.getDouble(1), 1e-6);
      assertEquals(30.0 + (3 * 6 + 0), res.getDouble(2), 1e-6);
      assertEquals(30.0 + (3 * 6 + 3), res.getDouble(3), 1e-6);

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testMissingOrSparseChunkResilience() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_sparse_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array latArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(4).withDataType(float64).withDimensionNames("latitude"),
              true);
      latArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {4}, new double[] {0, 10, 20, 30}));

      dev.zarr.zarrjava.v3.Array lonArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(4).withDataType(float64).withDimensionNames("longitude"),
              true);
      lonArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {4}, new double[] {100, 101, 102, 103}));

      dev.zarr.zarrjava.v3.Array sparseArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("sparse_data"),
              mb ->
                  mb.withShape(4, 4)
                      .withChunkShape(2, 2)
                      .withDataType(float64)
                      .withDimensionNames("latitude", "longitude"),
              true);

      dev.zarr.zarrjava.core.Attributes sparseAtts = new dev.zarr.zarrjava.core.Attributes();
      sparseAtts.set("ioos_category", "Unknown");
      sparseArray.setAttributes(sparseAtts);

      sparseArray.writeChunk(
          new long[] {0, 0},
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2, 2}, new double[] {1.0, 2.0, 3.0, 4.0}));
      sparseArray.writeChunk(
          new long[] {1, 1},
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2, 2}, new double[] {5.0, 6.0, 7.0, 8.0}));

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Sparse Chunk Test");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      EDDGridFromZarr dataset =
          new EDDGridFromZarr(
              "zarr_sparse_id",
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

      com.cohort.array.PrimitiveArray res =
          dataset.getSourceDataFromFile(
              dataset.dataVariables()[0], new int[] {0, 0}, new int[] {1, 1}, new int[] {3, 3});

      assertNotNull(res);
      assertEquals(16, res.size());

      assertEquals(1.0, res.getDouble(0), 1e-6);
      assertEquals(2.0, res.getDouble(1), 1e-6);
      assertEquals(3.0, res.getDouble(4), 1e-6);
      assertEquals(4.0, res.getDouble(5), 1e-6);

      assertTrue(Double.isNaN(res.getDouble(2)));
      assertTrue(Double.isNaN(res.getDouble(3)));
      assertTrue(Double.isNaN(res.getDouble(6)));
      assertTrue(Double.isNaN(res.getDouble(7)));

      assertTrue(Double.isNaN(res.getDouble(8)));
      assertTrue(Double.isNaN(res.getDouble(9)));
      assertTrue(Double.isNaN(res.getDouble(12)));
      assertTrue(Double.isNaN(res.getDouble(13)));

      assertEquals(5.0, res.getDouble(10), 1e-6);
      assertEquals(6.0, res.getDouble(11), 1e-6);
      assertEquals(7.0, res.getDouble(14), 1e-6);
      assertEquals(8.0, res.getDouble(15), 1e-6);

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testUnpackingAndMissingValues() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_unpack_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;
      dev.zarr.zarrjava.v3.DataType int16 = dev.zarr.zarrjava.v3.DataType.INT16;

      dev.zarr.zarrjava.v3.Array latArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("latitude"),
              true);
      latArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {0.0, 10.0}));

      dev.zarr.zarrjava.v3.Array lonArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("longitude"),
              true);
      lonArray.write(
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {100.0, 110.0}));

      dev.zarr.zarrjava.v3.Array packedArray =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("packed_temp"),
              mb ->
                  mb.withShape(2, 2)
                      .withChunkShape(2, 2)
                      .withDataType(int16)
                      .withDimensionNames("latitude", "longitude"),
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

      com.cohort.array.PrimitiveArray res =
          dataset.getSourceDataFromFile(
              dataset.dataVariables()[0], new int[] {0, 0}, new int[] {1, 1}, new int[] {1, 1});

      assertNotNull(res);
      assertEquals(4, res.size());

      assertEquals(10.0, res.getDouble(0), 1e-6);
      assertEquals(11.0, res.getDouble(1), 1e-6);
      assertTrue(Double.isNaN(res.getDouble(2)));
      assertEquals(15.0, res.getDouble(3), 1e-6);

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testGenerateDatasetsXmlAndCLI() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_gen_xml_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group g = dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.core.Attributes gAtts = new dev.zarr.zarrjava.core.Attributes();
      gAtts.set("title", "Generated XML Test Store");
      g.setAttributes(gAtts);

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("lat"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("lat"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {10.0, 20.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("lon"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("lon"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {100.0, 110.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("sst"),
              mb -> mb.withShape(2, 2).withDataType(float64).withDimensionNames("lat", "lon"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE,
                  new int[] {2, 2},
                  new double[] {20.0, 21.0, 22.0, 23.0}));

      String xml =
          EDDGridFromZarr.generateDatasetsXml(
              tempDir.toString(), "", "gen_xml_prefix", 720, "https://example.org/cache");

      assertNotNull(xml);
      assertTrue(xml.contains("type=\"EDDGridFromZarr\""));
      assertTrue(xml.contains("datasetID=\"gen_xml_prefix_"));
      assertTrue(xml.contains("<reloadEveryNMinutes>720</reloadEveryNMinutes>"));
      assertTrue(xml.contains("<cacheFromUrl>https://example.org/cache</cacheFromUrl>"));
      assertTrue(xml.contains("<sourceName>lat</sourceName>"));
      assertTrue(xml.contains("<sourceName>lon</sourceName>"));
      assertTrue(xml.contains("<sourceName>sst</sourceName>"));

      GenerateDatasetsXml gdx = new GenerateDatasetsXml();
      gdx.doGridFromZarr(
          new String[] {"EDDGridFromZarr", tempDir.toString(), "", "cli_prefix", "60", ""});

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testWebEndpointFileGeneration() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_web_endpoint_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group g = dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.core.Attributes gAtts = new dev.zarr.zarrjava.core.Attributes();
      gAtts.set("title", "Web Endpoint Zarr Test");
      g.setAttributes(gAtts);

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("latitude"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {-10.0, 10.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("longitude"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {100.0, 110.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temperature"),
              mb ->
                  mb.withShape(2, 2)
                      .withDataType(float64)
                      .withDimensionNames("latitude", "longitude"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE,
                  new int[] {2, 2},
                  new double[] {12.5, 14.0, 15.5, 17.0}));

      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<dataset type=\"EDDGridFromZarr\" datasetID=\"zarr_web_id\">\n"
              + "    <zarrStorePath>"
              + tempDir.toString().replace('\\', '/')
              + "</zarrStorePath>\n"
              + "    <zarrGroupName></zarrGroupName>\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <addAttributes>\n"
              + "        <att name=\"title\">Web Endpoint Zarr Test</att>\n"
              + "        <att name=\"summary\">Web Endpoint Test Summary</att>\n"
              + "        <att name=\"institution\">NOAA</att>\n"
              + "        <att name=\"infoUrl\">https://example.org</att>\n"
              + "    </addAttributes>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>latitude</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>longitude</sourceName>\n"
              + "        <destinationName>longitude</destinationName>\n"
              + "    </axisVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>temperature</sourceName>\n"
              + "        <destinationName>temperature</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Temperature</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>";

      SimpleXMLReader xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "dataset");
      EDDGridFromZarr dataset = EDDGridFromZarr.fromXml(null, xmlReader);

      String testDir = gov.noaa.pfel.erddap.util.EDStatic.config.fullTestCacheDirectory;

      // 1. Verify .das metadata format
      String dasFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", testDir, "zarr_test_endpoint", ".das");
      Path dasPath = Paths.get(testDir, dasFileName);
      assertTrue(Files.exists(dasPath));
      String dasContent = Files.readString(dasPath);
      assertTrue(dasContent.contains("Attributes {"));
      assertTrue(dasContent.contains("temperature {"));

      // 2. Verify .dds data descriptor structure format
      String ddsFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", testDir, "zarr_test_endpoint", ".dds");
      Path ddsPath = Paths.get(testDir, ddsFileName);
      assertTrue(Files.exists(ddsPath));
      String ddsContent = Files.readString(ddsPath);
      assertTrue(ddsContent.contains("Dataset {"));
      assertTrue(ddsContent.contains("temperature"));

      // 3. Verify .htmlTable format output
      String htmlTableFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "temperature[0:1][0:1]", testDir, "zarr_test_endpoint", ".htmlTable");
      Path htmlTablePath = Paths.get(testDir, htmlTableFileName);
      assertTrue(Files.exists(htmlTablePath));
      String htmlContent = Files.readString(htmlTablePath);
      assertTrue(
          htmlContent.contains("<table")
              || htmlContent.contains("<TABLE")
              || htmlContent.contains("temperature"));

      // 4. Verify .csv file format output
      String csvFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "temperature[0:1][0:1]", testDir, "zarr_test_endpoint", ".csv");
      Path csvPath = Paths.get(testDir, csvFileName);
      assertTrue(Files.exists(csvPath));
      String csvContent = Files.readString(csvPath);
      assertTrue(
          csvContent.contains("latitude")
              && csvContent.contains("longitude")
              && csvContent.contains("temperature"));

      // 5. Verify .nc NetCDF output
      String ncFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "temperature[0:1][0:1]", testDir, "zarr_test_endpoint", ".nc");
      Path ncPath = Paths.get(testDir, ncFileName);
      assertTrue(Files.exists(ncPath));
      assertTrue(Files.size(ncPath) > 0);

      // 6. Verify .png plot/image generation
      String pngFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "temperature[0:1][0:1]", testDir, "zarr_test_endpoint", ".png");
      Path pngPath = Paths.get(testDir, pngFileName);
      assertTrue(Files.exists(pngPath));
      assertTrue(Files.size(pngPath) > 0);

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testDatasetReloadMechanism() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_reload_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group g = dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.core.Attributes gAtts = new dev.zarr.zarrjava.core.Attributes();
      gAtts.set("title", "Initial Zarr Store Title");
      g.setAttributes(gAtts);

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("latitude"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {0.0, 5.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temperature"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("latitude"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {20.0, 22.0}));

      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<dataset type=\"EDDGridFromZarr\" datasetID=\"zarr_reload_id\">\n"
              + "    <zarrStorePath>"
              + tempDir.toString().replace('\\', '/')
              + "</zarrStorePath>\n"
              + "    <zarrGroupName></zarrGroupName>\n"
              + "    <reloadEveryNMinutes>1</reloadEveryNMinutes>\n"
              + "    <addAttributes>\n"
              + "        <att name=\"summary\">Reload Test Summary</att>\n"
              + "        <att name=\"institution\">NOAA</att>\n"
              + "        <att name=\"infoUrl\">https://example.org</att>\n"
              + "    </addAttributes>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>latitude</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "    </axisVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>temperature</sourceName>\n"
              + "        <destinationName>temperature</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Temperature</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>";

      SimpleXMLReader xmlReader1 =
          new SimpleXMLReader(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "dataset");
      EDDGridFromZarr dataset1 = EDDGridFromZarr.fromXml(null, xmlReader1);
      assertEquals(
          "Initial Zarr Store Title", dataset1.combinedGlobalAttributes().getString(0, "title"));

      // Update source Zarr store attributes to simulate underlying dataset reload/change
      gAtts.set("title", "Updated Zarr Store Title");
      g.setAttributes(gAtts);

      // Re-initialize dataset instance (simulating ERDDAP dataset reload mechanism)
      SimpleXMLReader xmlReader2 =
          new SimpleXMLReader(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "dataset");
      EDDGridFromZarr dataset2 = EDDGridFromZarr.fromXml(null, xmlReader2);

      assertEquals(
          "Updated Zarr Store Title", dataset2.combinedGlobalAttributes().getString(0, "title"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testGridCompressedData_ZarrJava() throws Throwable {
    Initialization.edStatic();
    EDDGridFromZarr dataset =
        (EDDGridFromZarr) EDDTestDataset.getTestZarr_gridCompressedData_ZarrJava();
    assertNotNull(dataset);
    assertEquals("zarr_gridCompressedData_ZarrJava", dataset.datasetID());

    Path testDir = Files.createTempDirectory("zarr_compressed_test");
    try {
      String dir = testDir.toString() + "/";
      String dasFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_compressed", ".das");
      String dasContent = Files.readString(Paths.get(dir, dasFileName));
      assertTrue(dasContent.contains("null_compressor"));

      String ddsFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_compressed", ".dds");
      String ddsContent = Files.readString(Paths.get(dir, ddsFileName));
      assertTrue(ddsContent.contains("GRID {"));
      assertTrue(ddsContent.contains("null_compressor"));
      assertTrue(ddsContent.contains("compressed_deflate1"));
    } finally {
      com.cohort.util.File2.deleteAllFiles(testDir.toString(), true, true);
    }

    int[] start = new int[] {0, 0};
    int[] stride = new int[] {1, 1};
    int[] stop = new int[] {0, 1};

    EDV nullComp = dataset.findDataVariableByDestinationName("null_compressor");
    assertNotNull(nullComp);
    com.cohort.array.PrimitiveArray paNull =
        dataset.getSourceDataFromFile(nullComp, start, stride, stop);
    assertTrue(Double.isNaN(paNull.getDouble(0)));
    assertEquals(1.0, paNull.getDouble(1), 1e-4);

    EDV def1 = dataset.findDataVariableByDestinationName("compressed_deflate1");
    assertNotNull(def1);
    com.cohort.array.PrimitiveArray paDef1 =
        dataset.getSourceDataFromFile(def1, start, stride, stop);
    assertEquals(0, paDef1.getInt(0));
    assertEquals(1, paDef1.getInt(1));

    EDV def9 = dataset.findDataVariableByDestinationName("compressed_deflate9");
    assertNotNull(def9);
    com.cohort.array.PrimitiveArray paDef9 =
        dataset.getSourceDataFromFile(def9, start, stride, stop);
    assertEquals(0, paDef9.getInt(0));
    assertEquals(1, paDef9.getInt(1));

    // Unsupported codec variables were gracefully skipped during initialization
    String[] activeDVs = dataset.dataVariableDestinationNames();
    assertTrue(String2.indexOf(activeDVs, "comp_filt_Adler_shuffle_deflate") < 0);
    assertTrue(String2.indexOf(activeDVs, "comp_filt_shuffle_deflate") < 0);
    assertTrue(String2.indexOf(activeDVs, "compressed_adler32") < 0);
    assertTrue(String2.indexOf(activeDVs, "compressed_crc32") < 0);
    assertTrue(String2.indexOf(activeDVs, "compressed_scaleOffset") < 0);
    assertTrue(String2.indexOf(activeDVs, "compressed_shuffle") < 0);
    assertTrue(String2.indexOf(activeDVs, "filtered_adler32") < 0);
    assertTrue(String2.indexOf(activeDVs, "filtered_adler_shuffle") < 0);
  }

  @Test
  void testGridFillValues_ZarrJava() throws Throwable {
    Initialization.edStatic();
    EDDGridFromZarr dataset =
        (EDDGridFromZarr) EDDTestDataset.getTestZarr_gridFillValues_ZarrJava();
    assertNotNull(dataset);
    assertEquals("zarr_gridFillValues_ZarrJava", dataset.datasetID());

    Path testDir = Files.createTempDirectory("zarr_fill_test");
    try {
      String dir = testDir.toString() + "/";
      String dasFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_fill", ".das");
      String dasContent = Files.readString(Paths.get(dir, dasFileName));
      assertTrue(dasContent.contains("double_nan {"));

      String ddsFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_fill", ".dds");
      String ddsContent = Files.readString(Paths.get(dir, ddsFileName));
      assertTrue(ddsContent.contains("double_nan"));
      assertTrue(ddsContent.contains("float_nan"));
    } finally {
      com.cohort.util.File2.deleteAllFiles(testDir.toString(), true, true);
    }

    int[] start = new int[] {0, 0};
    int[] stride = new int[] {1, 1};
    int[] stop = new int[] {0, 0};

    for (EDV edv : dataset.dataVariables()) {
      com.cohort.array.PrimitiveArray pa = dataset.getSourceDataFromFile(edv, start, stride, stop);
      assertNotNull(pa);
      String name = edv.destinationName();
      if ("double_nan".equals(name)) {
        assertTrue(Double.isNaN(pa.getDouble(0)));
      } else if ("float_nan".equals(name)) {
        assertTrue(Float.isNaN(pa.getFloat(0)));
      }
    }
  }

  @Test
  void testGriddTypes_ZarrJava() throws Throwable {
    Initialization.edStatic();
    EDDGridFromZarr dataset = (EDDGridFromZarr) EDDTestDataset.getTestZarr_griddTypes_ZarrJava();
    assertNotNull(dataset);
    assertEquals("zarr_griddTypes_ZarrJava", dataset.datasetID());

    Path testDir = Files.createTempDirectory("zarr_dtypes_test");
    try {
      String dir = testDir.toString() + "/";
      String dasFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_dtypes", ".das");
      String dasContent = Files.readString(Paths.get(dir, dasFileName));
      assertTrue(dasContent.contains("byte_ordered_group_big_endian_double_data {"));

      String ddsFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_dtypes", ".dds");
      String ddsContent = Files.readString(Paths.get(dir, ddsFileName));
      assertTrue(ddsContent.contains("byte_ordered_group_big_endian_double_data"));
      assertTrue(ddsContent.contains("byte_ordered_group_little_endian_ulong_data"));
    } finally {
      com.cohort.util.File2.deleteAllFiles(testDir.toString(), true, true);
    }

    int[] start = new int[] {0, 0};
    int[] stride = new int[] {1, 1};
    int[] stop = new int[] {0, 1};

    EDV bigDouble =
        dataset.findDataVariableByDestinationName("byte_ordered_group_big_endian_double_data");
    EDV littleDouble =
        dataset.findDataVariableByDestinationName("byte_ordered_group_little_endian_double_data");
    EDV bigLong =
        dataset.findDataVariableByDestinationName("byte_ordered_group_big_endian_long_data");
    EDV littleLong =
        dataset.findDataVariableByDestinationName("byte_ordered_group_little_endian_long_data");
    EDV bigUlong =
        dataset.findDataVariableByDestinationName("byte_ordered_group_big_endian_ulong_data");
    EDV littleUlong =
        dataset.findDataVariableByDestinationName("byte_ordered_group_little_endian_ulong_data");

    assertNotNull(bigDouble);
    assertNotNull(littleDouble);
    assertNotNull(bigLong);
    assertNotNull(littleLong);
    assertNotNull(bigUlong);
    assertNotNull(littleUlong);

    com.cohort.array.PrimitiveArray paBigDouble =
        dataset.getSourceDataFromFile(bigDouble, start, stride, stop);
    com.cohort.array.PrimitiveArray paLittleDouble =
        dataset.getSourceDataFromFile(littleDouble, start, stride, stop);
    for (int i = 0; i < paBigDouble.size(); i++) {
      assertEquals(paBigDouble.getDouble(i), paLittleDouble.getDouble(i), 1e-6);
    }

    com.cohort.array.PrimitiveArray paBigLong =
        dataset.getSourceDataFromFile(bigLong, start, stride, stop);
    com.cohort.array.PrimitiveArray paLittleLong =
        dataset.getSourceDataFromFile(littleLong, start, stride, stop);
    for (int i = 0; i < paBigLong.size(); i++) {
      assertEquals(paBigLong.getLong(i), paLittleLong.getLong(i));
    }

    com.cohort.array.PrimitiveArray paBigUlong =
        dataset.getSourceDataFromFile(bigUlong, start, stride, stop);
    com.cohort.array.PrimitiveArray paLittleUlong =
        dataset.getSourceDataFromFile(littleUlong, start, stride, stop);
    assertEquals(PAType.ULONG, paBigUlong.elementType());
    assertEquals(PAType.ULONG, paLittleUlong.elementType());
    for (int i = 0; i < paBigUlong.size(); i++) {
      assertEquals(paBigUlong.getString(i), paLittleUlong.getString(i));
    }
  }

  @Test
  void testUnsupportedOrMissingAxisFailsFast() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_missing_axis_test");

    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("valid_lat"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("valid_lat"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {10.0, 20.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temp"),
              mb -> mb.withShape(2).withDataType(float64).withDimensionNames("valid_lat"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {2}, new double[] {1.0, 2.0}));

      // Test 1: Missing axis variable that is not in store and not a dimension
      List<AxisVariableInfo> missingAxisList = new ArrayList<>();
      missingAxisList.add(
          new AxisVariableInfo(
              "missing_axis_name", "missing_axis_name", new LocalizedAttributes(), null));

      LocalizedAttributes addGlobalAtts = new LocalizedAttributes();
      addGlobalAtts.set(0, "title", "Missing Axis Test");
      addGlobalAtts.set(0, "summary", "Test Summary");
      addGlobalAtts.set(0, "institution", "NOAA");
      addGlobalAtts.set(0, "infoUrl", "https://example.org");

      Exception missingEx =
          assertThrows(
              Exception.class,
              () ->
                  new EDDGridFromZarr(
                      "zarr_missing_axis_id",
                      null,
                      null,
                      true,
                      new StringArray(),
                      null,
                      null,
                      null,
                      null,
                      addGlobalAtts,
                      missingAxisList,
                      new ArrayList<>(),
                      10080,
                      0,
                      tempDir.toString(),
                      "",
                      -1,
                      -1,
                      true));

      assertTrue(
          missingEx
              .getMessage()
              .contains("Axis variable 'missing_axis_name' not found in Zarr store"));

    } finally {
      com.cohort.util.File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testGridTestData_ZarrJava() throws Throwable {
    Initialization.edStatic();
    EDDGridFromZarr dataset = (EDDGridFromZarr) EDDTestDataset.getTestZarr_gridTestData_ZarrJava();
    assertNotNull(dataset);
    assertEquals("zarr_gridTestData_ZarrJava", dataset.datasetID());

    Path testDir = Files.createTempDirectory("zarr_testdata_test");
    try {
      String dir = testDir.toString() + "/";
      String dasFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_testdata", ".das");
      String dasContent = Files.readString(Paths.get(dir, dasFileName));
      assertTrue(dasContent.contains("group_with_dims_var4D {"));

      String ddsFileName =
          dataset.makeNewFileForDapQuery(0, null, null, "", dir, "zarr_testdata", ".dds");
      String ddsContent = Files.readString(Paths.get(dir, ddsFileName));
      assertTrue(ddsContent.contains("group_with_dims_var4D"));
    } finally {
      com.cohort.util.File2.deleteAllFiles(testDir.toString(), true, true);
    }

    int[] start = new int[] {0, 0, 0, 0};
    int[] stride = new int[] {1, 1, 1, 1};
    int[] stop = new int[] {0, 0, 0, 1};

    EDV var4D = dataset.findDataVariableByDestinationName("group_with_dims_var4D");
    assertNotNull(var4D);
    com.cohort.array.PrimitiveArray pa = dataset.getSourceDataFromFile(var4D, start, stride, stop);
    assertNotNull(pa);
    assertEquals(2, pa.size());
  }
}
