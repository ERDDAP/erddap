public class File2 {
    /**
   * This is like copy(), but decompresses if the source is compressed
   *
   * @param source the full file name of the source file. If compressed, this does decompress!
   * @param destination the full file name of the destination file. If the directory doesn't exist,
   *     it will be created. It is closed at the end.
   * @return true if successful. If not successful, the destination file won't exist.
   */
  public static boolean decompress(String source, String destination) {

    if (source.equals(destination)) return false;
    InputStream in = null;
    OutputStream out = null;
    boolean success = false;
    try {
      File dir = new File(getDirectory(destination));
      if (!dir.isDirectory()) dir.mkdirs();
      in = getDecompressedBufferedInputStream(source);
      out = new BufferedOutputStream(new FileOutputStream(destination));
      success = copy(in, out, 0, -1);
    } catch (Exception e) {
      String2.log(String2.ERROR + " in File2.copy source=" + source + "\n" + e);
    }
    try {
      if (in != null) in.close();
    } catch (Exception e) {
    }
    try {
      if (out != null) out.close();
    } catch (Exception e) {
    }

    if (!success) delete(destination);

    return success;
  }

  /**
   * This returns the current directory (with the proper separator at the end).
   *
   * @return the current directory (with the proper separator at the end)
   */
  public static String getCurrentDirectory() {
    String dir = System.getProperty("user.dir");

    if (!dir.endsWith(File.separator)) dir += File.separator;

    return dir;
  }

  /**
   * Creating a buffered FileWriter this way helps me check that charset is set. (Instead of the
   * default charset used by "new FileWriter()").
   */
  public static BufferedWriter getBufferedFileWriter88591(String fullFileName) throws IOException {
    return getBufferedFileWriter(fullFileName, ISO_8859_1_CHARSET);
  }

  /**
   * This reads the specified number of bytes from the inputstream (unlike InputStream.read, which
   * may not read all of the bytes).
   *
   * @param inputStream Best if buffered.
   * @param byteArray
   * @param offset the first position of byteArray to be written to
   * @param length the number of bytes to be read
   * @throws Exception if trouble
   */
  public static void readFully(InputStream inputStream, byte[] byteArray, int offset, int length)
      throws Exception {

    int po = offset;
    int remain = length;
    while (remain > 0) {
      int read = inputStream.read(byteArray, po, remain);
      po += read;
      remain -= read;
    }
  }

  /**
   * This creates, reads, and returns a byte array of the specified length.
   *
   * @param inputStream Best if buffered.
   * @param length the number of bytes to be read
   * @throws Exception if trouble
   */
  public static byte[] readFully(InputStream inputStream, int length) throws Exception {

    byte[] byteArray = new byte[length];
    readFully(inputStream, byteArray, 0, length);
    return byteArray;
  }
}
