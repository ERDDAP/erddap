package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testDataset.Initialization;

public class TableWriterAllTests {

  @org.junit.jupiter.api.BeforeAll
  static void beforeAll() {
    Initialization.edStatic();
  }

  @Test
  void testSanitizePath(@TempDir Path tempDir) throws Exception {
    String baseDir = tempDir.toString();

    // Valid path inside baseDir
    String validPath = tempDir.resolve("sub/test.txt").toString();
    new File(tempDir.resolve("sub").toString()).mkdirs();
    new File(validPath).createNewFile();

    String sanitized = TableWriterAll.sanitizePath(validPath, baseDir);
    assertEquals(new File(validPath).getCanonicalPath(), sanitized);

    // Invalid path traversal outside baseDir
    String traversalPath = tempDir.resolve("../outside.txt").toString();
    assertThrows(
        SecurityException.class, () -> TableWriterAll.sanitizePath(traversalPath, baseDir));
  }

  @Test
  void testTableWriterAllWriteAndReadChunks(@TempDir Path tempDir) throws Throwable {
    String dir = tempDir.toString();
    TableWriterAll twa = new TableWriterAll(0, null, null, dir, "testTableWriterAll");

    Table table = new Table();
    table.addColumn("col0", new DoubleArray(new double[] {10.0, 20.0, 30.0, 40.0, 50.0}));
    table.addColumn("col1", new IntArray(new int[] {1, 2, 3, 4, 5}));
    table.addColumn("col2", new StringArray(new String[] {"a", "bb", "ccc", "dddd", "eeeee"}));

    twa.writeSome(table);

    // Write another chunk of rows
    Table table2 = new Table();
    table2.addColumn("col0", new DoubleArray(new double[] {60.0, 70.0}));
    table2.addColumn("col1", new IntArray(new int[] {6, 7}));
    table2.addColumn("col2", new StringArray(new String[] {"ffffff", "ggggggg"}));

    twa.writeSome(table2);
    twa.finish();

    assertEquals(7, twa.nRows());

    // Test column(0) reading full array
    DoubleArray col0 = (DoubleArray) twa.column(0);
    assertEquals(7, col0.size());
    assertEquals(10.0, col0.get(0));
    assertEquals(70.0, col0.get(6));

    // Test StringArray column(2) reading full array
    StringArray col2 = (StringArray) twa.column(2);
    assertEquals(7, col2.size());
    assertEquals("a", col2.get(0));
    assertEquals("ggggggg", col2.get(6));

    // Test readColumnChunk for col0 starting at row 2 with maxRows 3
    DoubleArray chunk0 = new DoubleArray();
    String file0 = dir + "/" + twa.columnFileName(0);
    try (FileChannel fc = FileChannel.open(Path.of(file0), StandardOpenOption.READ)) {
      TableWriterAll.readColumnChunk(0, fc, chunk0, 2, 3);
    }
    assertEquals(3, chunk0.size());
    assertEquals(30.0, chunk0.get(0));
    assertEquals(40.0, chunk0.get(1));
    assertEquals(50.0, chunk0.get(2));

    // Test readColumnChunk for StringArray col2 starting at row 3 with maxRows 2
    StringArray chunk2 = new StringArray();
    String file2 = dir + "/" + twa.columnFileName(2);
    try (FileChannel fc = FileChannel.open(Path.of(file2), StandardOpenOption.READ)) {
      TableWriterAll.readColumnChunk(2, fc, chunk2, 3, 2);
    }
    assertEquals(2, chunk2.size());
    assertEquals("dddd", chunk2.get(0));
    assertEquals("eeeee", chunk2.get(1));

    twa.close();
  }
}
