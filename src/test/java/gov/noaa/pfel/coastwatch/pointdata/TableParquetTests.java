package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.parquet.ParquetWriterBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import testDataset.Initialization;

class TableParquetTests {

  @TempDir Path tempDir;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testParquetBooleans() throws Exception {
    String fileName = tempDir.resolve("testBooleans.parquet").toString();

    // Define schema with boolean columns
    String schemaString =
        "message test {\n"
            + "  required binary id (UTF8);\n"
            + "  required boolean bool_col;\n"
            + "  optional boolean bool_opt;\n"
            + "}";
    MessageType schema = MessageTypeParser.parseMessageType(schemaString);

    // Write a Parquet file with boolean data using standard Parquet example writer
    Configuration conf = new Configuration();
    try (ParquetWriter<Group> writer =
        ExampleParquetWriter.builder(new LocalOutputFile(java.nio.file.Path.of(fileName)))
            .withType(schema)
            .withConf(conf)
            .build()) {
      SimpleGroupFactory factory = new SimpleGroupFactory(schema);

      // Row 0: id="r0", bool_col=true, bool_opt=false
      writer.write(
          factory.newGroup().append("id", "r0").append("bool_col", true).append("bool_opt", false));

      // Row 1: id="r1", bool_col=false, bool_opt=true
      writer.write(
          factory.newGroup().append("id", "r1").append("bool_col", false).append("bool_opt", true));

      // Row 2: id="r2", bool_col=true, bool_opt is missing (null)
      writer.write(factory.newGroup().append("id", "r2").append("bool_col", true));
    }

    // Read back with ERDDAP Table
    Table table = new Table();
    table.readParquet(fileName, null, null, true);

    // Verify results
    // ERDDAP should have converted booleans to bytes: 1=true, 0=false, missing value=127 (empty
    // string in CSV)
    String results = table.dataToString();
    String expected = "id,bool_col,bool_opt\n" + "r0,1,0\n" + "r1,0,1\n" + "r2,1,\n";
    Test.ensureEqual(results, expected, "Initial read results=\n" + results);

    // Verify column types are ByteArray
    Test.ensureTrue(table.getColumn("bool_col") instanceof ByteArray, "bool_col type");
    Test.ensureTrue(table.getColumn("bool_opt") instanceof ByteArray, "bool_opt type");

    // Verify raw values for null
    ByteArray boolOpt = (ByteArray) table.getColumn("bool_opt");
    Test.ensureEqual(boolOpt.get(2), (byte) 127, "raw value for null boolean");

    // NOW: Test writing back to Parquet using ERDDAP's CustomWriteSupport
    String roundTripFileName = tempDir.resolve("testBooleansRoundTrip.parquet").toString();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("column_names", "id,bool_col,bool_opt");
    metadata.put("column_units", ",,");

    try (ParquetWriter<List<PAOne>> writer =
        new ParquetWriterBuilder(
                schema, new LocalOutputFile(java.nio.file.Path.of(roundTripFileName)), metadata)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(new Configuration())
            .build()) {

      for (int row = 0; row < table.nRows(); row++) {
        ArrayList<PAOne> record = new ArrayList<>();
        for (int col = 0; col < table.nColumns(); col++) {
          record.add(table.getPAOneData(col, row));
        }
        writer.write(record);
      }
    }

    // VERIFY THE PARQUET FILE SCHEMA DIRECTLY
    try (ParquetFileReader reader =
        ParquetFileReader.open(new LocalInputFile(java.nio.file.Path.of(roundTripFileName)))) {
      MessageType rtSchema = reader.getFileMetaData().getSchema();
      Test.ensureEqual(
          rtSchema.getType("bool_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.BOOLEAN,
          "rtSchema bool_col");
      Test.ensureEqual(
          rtSchema.getType("bool_opt").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.BOOLEAN,
          "rtSchema bool_opt");
    }

    // Read round-trip file back
    Table table2 = new Table();
    table2.readParquet(roundTripFileName, null, null, true);
    String roundTripResults = table2.dataToString();
    Test.ensureEqual(roundTripResults, expected, "Round-trip read results=\n" + roundTripResults);

    // Final check that types are still ByteArray
    Test.ensureTrue(table2.getColumn("bool_col") instanceof ByteArray, "bool_col type round-trip");
    Test.ensureTrue(table2.getColumn("bool_opt") instanceof ByteArray, "bool_opt type round-trip");
  }

  @org.junit.jupiter.api.Test
  void testParquetTypedPrimitives() throws Exception {
    String fileName = tempDir.resolve("testTyped.parquet").toString();

    String schemaString =
        "message test {\n"
            + "  required binary id (UTF8);\n"
            + "  required int32 int_col;\n"
            + "  optional int64 long_col;\n"
            + "  required float float_col;\n"
            + "  optional double double_col;\n"
            + "  optional boolean bool_col;\n"
            + "  optional binary bin_col (UTF8);\n"
            + "}";
    MessageType schema = MessageTypeParser.parseMessageType(schemaString);

    try (ParquetWriter<Group> writer =
        ExampleParquetWriter.builder(new LocalOutputFile(java.nio.file.Path.of(fileName)))
            .withType(schema)
            .withConf(new Configuration())
            .build()) {
      SimpleGroupFactory factory = new SimpleGroupFactory(schema);

      // Row 0: all present
      writer.write(
          factory
              .newGroup()
              .append("id", "r0")
              .append("int_col", 10)
              .append("long_col", 10000000000L)
              .append("float_col", 1.5f)
              .append("double_col", 2.5)
              .append("bool_col", true)
              .append("bin_col", "s0"));

      // Row 1: all present
      writer.write(
          factory
              .newGroup()
              .append("id", "r1")
              .append("int_col", 20)
              .append("long_col", 20000000000L)
              .append("float_col", 3.5f)
              .append("double_col", 4.5)
              .append("bool_col", false)
              .append("bin_col", "s1"));

      // Row 2: some missing (long_col, double_col, bool_col, bin_col)
      writer.write(
          factory.newGroup().append("id", "r2").append("int_col", 30).append("float_col", 5.5f));
    }

    Table table = new Table();
    table.readParquet(fileName, null, null, true);
    // Types -- integer column may be simplified to ByteArray/ShortArray etc., so allow integer-like
    // PAs
    PrimitiveArray intCol = table.getColumn("int_col");
    Test.ensureTrue(
        intCol instanceof IntArray
            || intCol instanceof ByteArray
            || intCol instanceof com.cohort.array.ShortArray,
        "int_col type");
    Test.ensureTrue(table.getColumn("long_col") instanceof LongArray, "long_col type");
    // double may be simplified to float if values fit; accept either
    PrimitiveArray dblCol = table.getColumn("double_col");
    Test.ensureTrue(
        dblCol instanceof DoubleArray || dblCol instanceof FloatArray, "double_col type");
    Test.ensureTrue(table.getColumn("float_col") instanceof FloatArray, "float_col type");
    Test.ensureTrue(table.getColumn("bool_col") instanceof ByteArray, "bool_col type");
    Test.ensureTrue(table.getColumn("bin_col") instanceof StringArray, "bin_col type");

    // Values
    Test.ensureEqual(table.getColumn("int_col").getInt(0), 10, "int0");
    Test.ensureEqual(table.getColumn("int_col").getInt(1), 20, "int1");
    Test.ensureEqual(table.getColumn("int_col").getInt(2), 30, "int2");

    Test.ensureEqual(table.getColumn("long_col").getLong(0), 10000000000L, "long0");
    Test.ensureEqual(table.getColumn("long_col").getLong(1), 20000000000L, "long1");
    // missing long -> Long.MAX_VALUE sentinel
    Test.ensureEqual(
        ((LongArray) table.getColumn("long_col")).getLong(2), Long.MAX_VALUE, "long2 missing");

    Test.ensureEqual(table.getColumn("float_col").getFloat(0), 1.5f, "float0");
    Test.ensureEqual(table.getColumn("float_col").getFloat(1), 3.5f, "float1");
    Test.ensureEqual(table.getColumn("float_col").getFloat(2), 5.5f, "float2");

    Test.ensureEqual(table.getColumn("double_col").getDouble(0), 2.5, "double0");
    Test.ensureEqual(table.getColumn("double_col").getDouble(1), 4.5, "double1");
    // missing double -> Double.NaN sentinel (use Double.isNaN)
    Test.ensureTrue(Double.isNaN(table.getColumn("double_col").getDouble(2)), "double2 missing");

    // boolean stored as bytes 1|0, missing -> 127
    ByteArray bcol = (ByteArray) table.getColumn("bool_col");
    Test.ensureEqual(bcol.get(0), (byte) 1, "bool0");
    Test.ensureEqual(bcol.get(1), (byte) 0, "bool1");
    Test.ensureEqual(bcol.get(2), (byte) 127, "bool2 missing");

    Test.ensureEqual(((StringArray) table.getColumn("bin_col")).getString(0), "s0", "bin0");
    Test.ensureEqual(((StringArray) table.getColumn("bin_col")).getString(1), "s1", "bin1");
    Test.ensureEqual(((StringArray) table.getColumn("bin_col")).getString(2), "", "bin2 missing");

    // Round-trip: write back using ParquetWriterBuilder and ensure schema types preserved
    String roundTripFileName = tempDir.resolve("testTypedRoundTrip.parquet").toString();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("column_names", "id,int_col,long_col,float_col,double_col,bool_col,bin_col");
    metadata.put("column_units", ",,,,,,");

    try (ParquetWriter<List<PAOne>> writer =
        new ParquetWriterBuilder(
                schema, new LocalOutputFile(java.nio.file.Path.of(roundTripFileName)), metadata)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(new Configuration())
            .build()) {

      for (int row = 0; row < table.nRows(); row++) {
        ArrayList<PAOne> record = new ArrayList<>();
        for (int col = 0; col < table.nColumns(); col++) {
          record.add(table.getPAOneData(col, row));
        }
        writer.write(record);
      }
    }

    try (ParquetFileReader reader =
        ParquetFileReader.open(new LocalInputFile(java.nio.file.Path.of(roundTripFileName)))) {
      MessageType rtSchema = reader.getFileMetaData().getSchema();
      Test.ensureEqual(
          rtSchema.getType("int_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.INT32,
          "rtSchema int_col");
      Test.ensureEqual(
          rtSchema.getType("long_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.INT64,
          "rtSchema long_col");
      Test.ensureEqual(
          rtSchema.getType("float_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.FLOAT,
          "rtSchema float_col");
      Test.ensureEqual(
          rtSchema.getType("double_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.DOUBLE,
          "rtSchema double_col");
      Test.ensureEqual(
          rtSchema.getType("bool_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.BOOLEAN,
          "rtSchema bool_col");
      Test.ensureEqual(
          rtSchema.getType("bin_col").asPrimitiveType().getPrimitiveTypeName(),
          PrimitiveTypeName.BINARY,
          "rtSchema bin_col");
    }

    // read round-trip back
    Table table2 = new Table();
    table2.readParquet(roundTripFileName, null, null, true);

    // basic sanity: types preserved as before (allow integer-like simplifications)
    PrimitiveArray rtIntCol = table2.getColumn("int_col");
    Test.ensureTrue(
        rtIntCol instanceof IntArray
            || rtIntCol instanceof ByteArray
            || rtIntCol instanceof com.cohort.array.ShortArray,
        "int_col type rt");
    Test.ensureTrue(table2.getColumn("bool_col") instanceof ByteArray, "bool_col type rt");
  }
}
