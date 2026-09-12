/*
 * BufferedFileChannel Copyright 2025, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * BufferedFileChannel wraps a FileChannel and buffers small writes using an 8 KB ByteBuffer set to
 * ByteOrder.nativeOrder(). Large writes (&gt;= 8 KB) flush the buffer and write directly to the
 * underlying FileChannel.
 */
public class BufferedFileChannel implements AutoCloseable {

  public static final int BUFFER_SIZE = 8192;

  private final FileChannel channel;
  private final ByteBuffer buffer;

  /**
   * Constructs a BufferedFileChannel wrapping the specified FileChannel.
   *
   * @param channel the FileChannel to write to
   */
  public BufferedFileChannel(FileChannel channel) {
    if (channel == null) {
      throw new IllegalArgumentException(
          String2.ERROR + " in BufferedFileChannel constructor: FileChannel is null.");
    }
    this.channel = channel;
    this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
  }

  /**
   * Returns the underlying FileChannel.
   *
   * @return the underlying FileChannel
   */
  public FileChannel fileChannel() {
    try {
      flush();
    } catch (IOException e) {
      throw new RuntimeException(
          String2.ERROR + " in BufferedFileChannel.fileChannel: IOException occurred.", e);
    }
    return channel;
  }

  /**
   * Writes bytes from the source ByteBuffer to the buffered channel.
   *
   * @param src the source ByteBuffer
   * @return the number of bytes written
   * @throws IOException if an I/O error occurs
   */
  public int write(ByteBuffer src) throws IOException {
    if (src == null) {
      throw new IllegalArgumentException(
          String2.ERROR + " in BufferedFileChannel.write: src is null.");
    }
    int totalWritten = src.remaining();
    if (totalWritten == 0) {
      return 0;
    }

    // If source is large, flush pending buffer first, then write source directly
    if (src.remaining() >= BUFFER_SIZE) {
      flush();
      while (src.hasRemaining()) {
        channel.write(src);
      }
      return totalWritten;
    }

    while (src.hasRemaining()) {
      if (buffer.remaining() == 0) {
        flush();
      }
      int toCopy = Math.min(src.remaining(), buffer.remaining());
      int oldLimit = src.limit();
      src.limit(src.position() + toCopy);
      buffer.put(src);
      src.limit(oldLimit);
    }

    return totalWritten;
  }

  /**
   * Writes bytes from the specified byte array to the buffered channel.
   *
   * @param b the byte array
   * @param off the start offset in the data
   * @param len the number of bytes to write
   * @return total bytes written
   * @throws IOException if an I/O error occurs
   */
  public int write(byte[] b, int off, int len) throws IOException {
    if (b == null) {
      throw new IllegalArgumentException(
          String2.ERROR + " in BufferedFileChannel.write: byte array is null.");
    }
    if (off < 0 || len < 0 || off + len > b.length) {
      throw new IndexOutOfBoundsException(
          String2.ERROR + " in BufferedFileChannel.write: invalid offset/length.");
    }
    if (len == 0) {
      return 0;
    }

    if (len >= BUFFER_SIZE) {
      flush();
      ByteBuffer src = ByteBuffer.wrap(b, off, len);
      while (src.hasRemaining()) {
        channel.write(src);
      }
      return len;
    }

    int offset = off;
    int remaining = len;
    while (remaining > 0) {
      if (buffer.remaining() == 0) {
        flush();
      }
      int toCopy = Math.min(remaining, buffer.remaining());
      buffer.put(b, offset, toCopy);
      offset += toCopy;
      remaining -= toCopy;
    }

    return len;
  }

  /**
   * Writes all bytes from the specified byte array to the buffered channel.
   *
   * @param b the byte array
   * @return total bytes written
   * @throws IOException if an I/O error occurs
   */
  public int write(byte[] b) throws IOException {
    if (b == null) {
      throw new IllegalArgumentException(
          String2.ERROR + " in BufferedFileChannel.write: byte array is null.");
    }
    return write(b, 0, b.length);
  }

  /**
   * Flushes any unwritten buffered bytes to the underlying FileChannel.
   *
   * @throws IOException if an I/O error occurs
   */
  public void flush() throws IOException {
    if (buffer.position() > 0) {
      buffer.flip();
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      buffer.clear();
    }
  }

  /**
   * Flushes remaining bytes and closes the underlying FileChannel.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close() throws IOException {
    try {
      flush();
    } finally {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
    }
  }
}
