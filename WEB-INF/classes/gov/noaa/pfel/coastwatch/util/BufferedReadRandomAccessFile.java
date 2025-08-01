package gov.noaa.pfel.coastwatch.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Wrap the RandomAccessFile with a buffer. For reads with small step sizes this can drastically
 * speed up reads. If the step size is large, this doesn't use the buffer and has effectively the
 * same performance as the regular RandomAccessFile.
 */
public class BufferedReadRandomAccessFile implements AutoCloseable {
  private static final int DEFAULT_BUFSIZE = 4096;
  private static final int MAX_BUFF_SIZE = 8192;
  private static int MAX_STEP_SIZE = 512;
  private RandomAccessFile raf;
  private byte inbuf[];
  private long startpos = -1;
  private long endpos = -1;
  private int bufsize;

  public BufferedReadRandomAccessFile(String name) throws FileNotFoundException {
    this(name, DEFAULT_BUFSIZE);
  }

  public BufferedReadRandomAccessFile(String name, int stepSize, int numSteps)
      throws FileNotFoundException {
    this(name, stepSize < MAX_STEP_SIZE ? stepSize * numSteps : 2);
  }

  public BufferedReadRandomAccessFile(String name, int b) throws FileNotFoundException {
    raf = new RandomAccessFile(name, "r");
    // Unless we think the buffer is useless (size 2), don't go lower than default size.
    if (b > 2 && b < DEFAULT_BUFSIZE) {
      b = DEFAULT_BUFSIZE;
    }
    bufsize = Math.min(b, MAX_BUFF_SIZE);
    inbuf = new byte[bufsize];
  }

  public short readShort(long pos) {
    if (pos < startpos || pos > endpos) {
      long blockstart = (pos / bufsize) * bufsize;
      int n;
      try {
        raf.seek(blockstart);
        n = raf.read(inbuf);
      } catch (IOException e) {
        return -1;
      }
      startpos = blockstart;
      endpos = blockstart + n - 1;
      if (pos < startpos || pos > endpos) return -1;
    }
    return (short)
        ((inbuf[(int) (pos - startpos) + 1] & 0xff)
            + ((inbuf[(int) (pos - startpos)] & 0xff) << 8));
  }

  @Override
  public void close() throws IOException {
    raf.close();
  }
}
