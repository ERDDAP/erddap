package gov.noaa.pfel.erddap.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BufferedFileChannelTests {

  @Test
  void testConstructorNull() {
    assertThrows(IllegalArgumentException.class, () -> new BufferedFileChannel(null));
  }

  @Test
  void testWriteSmallAndFlush(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_small.bin");
    try (FileChannel fc =
            FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        BufferedFileChannel bfc = new BufferedFileChannel(fc)) {
      assertEquals(fc, bfc.fileChannel());

      ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
      buf.putDouble(123.45);
      buf.putDouble(678.90);
      buf.flip();

      int written = bfc.write(buf);
      assertEquals(16, written);

      // Before flush, FileChannel size should be 0 because 16 bytes fit in 8 KB buffer
      assertEquals(0, fc.size());

      bfc.flush();
      assertEquals(16, fc.size());
    }

    // Read back and verify
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer readBuf = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
      fc.read(readBuf);
      readBuf.flip();
      assertEquals(123.45, readBuf.getDouble(), 1e-6);
      assertEquals(678.90, readBuf.getDouble(), 1e-6);
    }
  }

  @Test
  void testWriteLargeDirect(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_large.bin");
    int size = 16384; // 16 KB > 8 KB
    try (FileChannel fc =
            FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        BufferedFileChannel bfc = new BufferedFileChannel(fc)) {

      ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
      for (int i = 0; i < size / 4; i++) {
        buf.putInt(i);
      }
      buf.flip();

      int written = bfc.write(buf);
      assertEquals(size, written);
      assertEquals(size, fc.size());
    }

    // Verify content
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer readBuf = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
      fc.read(readBuf);
      readBuf.flip();
      for (int i = 0; i < size / 4; i++) {
        assertEquals(i, readBuf.getInt());
      }
    }
  }

  @Test
  void testNullWriteBuffer(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_null.bin");
    try (FileChannel fc =
            FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        BufferedFileChannel bfc = new BufferedFileChannel(fc)) {
      assertThrows(IllegalArgumentException.class, () -> bfc.write((ByteBuffer) null));
      assertThrows(IllegalArgumentException.class, () -> bfc.write((byte[]) null));
      assertThrows(IllegalArgumentException.class, () -> bfc.write((byte[]) null, 0, 10));
    }
  }

  @Test
  void testWriteByteArraySmallAndLarge(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_byte_array.bin");
    byte[] dataSmall = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    byte[] dataLarge = new byte[10000]; // 10 KB > 8 KB
    for (int i = 0; i < dataLarge.length; i++) {
      dataLarge[i] = (byte) (i & 0xFF);
    }

    try (FileChannel fc =
            FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        BufferedFileChannel bfc = new BufferedFileChannel(fc)) {
      // Small write
      int writtenSmall = bfc.write(dataSmall);
      assertEquals(dataSmall.length, writtenSmall);
      assertEquals(0, fc.size()); // buffered

      // Large write triggers flush and direct write
      int writtenLarge = bfc.write(dataLarge, 0, dataLarge.length);
      assertEquals(dataLarge.length, writtenLarge);
      assertEquals(dataSmall.length + dataLarge.length, fc.size());
    }

    // Read back and verify
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer readBuf = ByteBuffer.allocate(dataSmall.length + dataLarge.length);
      fc.read(readBuf);
      readBuf.flip();

      byte[] readSmall = new byte[dataSmall.length];
      readBuf.get(readSmall);
      for (int i = 0; i < dataSmall.length; i++) {
        assertEquals(dataSmall[i], readSmall[i]);
      }

      byte[] readLarge = new byte[dataLarge.length];
      readBuf.get(readLarge);
      for (int i = 0; i < dataLarge.length; i++) {
        assertEquals(dataLarge[i], readLarge[i]);
      }
    }
  }
}
