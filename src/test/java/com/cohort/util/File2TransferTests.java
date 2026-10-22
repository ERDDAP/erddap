package com.cohort.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class File2TransferTests {

  @TempDir Path tempDir;

  /**
   * Verifies that local 64 KB buffer allocations do not cause data corruption when multiple threads
   * invoke File2.copy concurrently.
   */
  @Test
  public void testConcurrentCopyThreadLocalBufferSafety() throws Exception {
    int threads = 16;
    int fileSizeBytes =
        256 * 1024; // 256 KB (larger than the 64 KB buffer to force multiple read cycles)
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch endGate = new CountDownLatch(threads);
    AtomicBoolean corruptionDetected = new AtomicBoolean(false);

    for (int i = 0; i < threads; i++) {
      final byte threadByteValue = (byte) (i + 1);
      final Path src = tempDir.resolve("src_thread_" + i + ".dat");
      final Path dst = tempDir.resolve("dst_thread_" + i + ".dat");

      // Write unique content per thread to detect any cross-thread buffer bleeding
      byte[] expectedData = new byte[fileSizeBytes];
      Arrays.fill(expectedData, threadByteValue);
      Files.write(src, expectedData);

      pool.submit(
          () -> {
            try {
              startGate.await();
              boolean ok = File2.copy(src.toString(), dst.toString());
              if (!ok) {
                corruptionDetected.set(true);
              }

              byte[] result = Files.readAllBytes(dst);
              if (!Arrays.equals(expectedData, result)) {
                corruptionDetected.set(true);
              }
            } catch (Exception e) {
              corruptionDetected.set(true);
            } finally {
              endGate.countDown();
            }
          });
    }

    startGate.countDown();
    assertTrue(endGate.await(10, TimeUnit.SECONDS), "Concurrent copy timed out");
    pool.shutdown();

    assertFalse(corruptionDetected.get(), "Buffer corruption detected across threads!");
  }

  /**
   * Tests File2.copy(String, OutputStream, first, last) with byte slicing, verifying skipFully
   * (skipNBytes) and proper range termination.
   */
  @Test
  public void testStreamCopyRangeAndSkip() throws Exception {
    Path src = tempDir.resolve("range_test_src.dat");
    byte[] payload = new byte[100_000];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) (i % 127);
    }
    Files.write(src, payload);

    // 1. Test middle range slice (bytes 10,000 to 19,999 -> exactly 10,000 bytes)
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    boolean ok = File2.copy(src.toString(), out, 10_000, 19_999);
    assertTrue(ok);

    byte[] sliced = out.toByteArray();
    assertEquals(10_000, sliced.length);
    byte[] expected = Arrays.copyOfRange(payload, 10_000, 20_000);
    assertArrayEquals(expected, sliced);

    // 2. Test open-ended range from offset 95,000 to end
    out.reset();
    boolean okEnd = File2.copy(src.toString(), out, 95_000, -1);
    assertTrue(okEnd);
    assertEquals(5_000, out.size());
    assertArrayEquals(Arrays.copyOfRange(payload, 95_000, payload.length), out.toByteArray());
  }

  /**
   * Tests FileChannel.transferTo boundary clamping in File2.copy(String, String, first, last).
   * Asserts that specifying last > fileSize does not trigger an incomplete transfer exception.
   */
  @Test
  public void testFileChannelCopyClampedBounds() throws Exception {
    Path src = tempDir.resolve("clamped_src.dat");
    Path dst = tempDir.resolve("clamped_dst.dat");

    byte[] data = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    Files.write(src, data);

    // Request 'last' far beyond end of file (last=99999, size=36)
    boolean ok = File2.copy(src.toString(), dst.toString(), 10, 99_999);
    assertTrue(ok, "File2.copy should succeed by clamping bounds to actual file size");

    byte[] copied = Files.readAllBytes(dst);
    assertEquals(data.length - 10, copied.length);
    assertArrayEquals(Arrays.copyOfRange(data, 10, data.length), copied);
  }

  /** Tests boundary cases for File2.copy: empty files vs out-of-bounds start offsets. */
  @Test
  public void testFile2CopyBoundaryCases() throws Exception {
    Path emptySrc = tempDir.resolve("empty.dat");
    Path emptyDst = tempDir.resolve("empty_dst.dat");
    Files.write(emptySrc, new byte[0]);

    // Legitimate 0-byte file copy (first=0, size=0) -> true
    assertTrue(File2.copy(emptySrc.toString(), emptyDst.toString(), 0, -1));

    Path src = tempDir.resolve("non_empty.dat");
    Path dst = tempDir.resolve("non_empty_dst.dat");
    Files.write(src, "Hello World".getBytes()); // 11 bytes

    // Out-of-bounds start offset (first = 20, size = 11) -> false
    assertFalse(File2.copy(src.toString(), dst.toString(), 20, 30));
  }

  /**
   * Verifies that when File2.copy fails due to an out-of-bounds start offset, no destination file
   * is created on disk.
   */
  @Test
  public void testFile2CopyOutOfBoundsDoesNotCreateDestination() throws Exception {
    Path src = tempDir.resolve("src_out_of_bounds.dat");
    Path dst = tempDir.resolve("dst_out_of_bounds.dat");
    Files.write(src, "test payload".getBytes());

    boolean ok = File2.copy(src.toString(), dst.toString(), 50, 100);
    assertFalse(ok);
    assertFalse(
        Files.exists(dst), "Destination file should not exist after failed out-of-bounds copy");
  }

  /**
   * Verifies that File2.copy(InputStream, OutputStream, first, last) returns false and writes 0
   * bytes when an inverted range (first > last) is supplied.
   */
  @Test
  public void testStreamCopyInvertedRangeFails() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("hello world".getBytes());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    boolean ok = File2.copy(in, out, 10, 5);
    assertFalse(ok, "Inverted range (first > last) should return false");
    assertEquals(0, out.size());
  }

  /**
   * Verifies that File2.copy can overwrite an existing non-empty file with a 0-byte file by
   * truncating it without throwing FileAlreadyExistsException.
   */
  @Test
  public void testFile2CopyOverwriteWithZeroByteFile() throws Exception {
    Path emptySrc = tempDir.resolve("empty_src.dat");
    Path dst = tempDir.resolve("existing_dst.dat");

    Files.write(emptySrc, new byte[0]);
    Files.write(dst, "existing content to be truncated".getBytes());

    boolean ok = File2.copy(emptySrc.toString(), dst.toString(), 0, -1);
    assertTrue(ok, "Overwriting existing file with 0-byte file should succeed");
    assertEquals(0, Files.size(dst), "Destination file should be truncated to 0 bytes");
  }
}
