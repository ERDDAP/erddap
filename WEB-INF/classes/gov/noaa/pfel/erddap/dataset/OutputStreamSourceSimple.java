/*
 * OutputStreamSourceSimple Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import java.io.OutputStream;

/**
 * OutputStreamSourceSimple provides an OutputStream upon request.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-01
 */
public class OutputStreamSourceSimple implements OutputStreamSource {

  private OutputStream outputStream;

  /**
   * The constructor.
   *
   * @param os This should be already buffered.
   */
  public OutputStreamSourceSimple(OutputStream os) {
    outputStream = os;
  }

  /** A variant of outputStream() for when the contentLength isn't known. */
  @Override
  public OutputStream outputStream(String characterEncoding) throws Throwable {
    return outputStream;
  }

  /**
   * This returns an OutputStream. If called repeatedly, this returns the same outputStream.
   *
   * @param characterEncoding e.g., "" (for none specified), String2.UTF_8, or "" (for DAP). This
   *     parameter only matters the first time this method is called. This only matters for some
   *     subclasses.
   * @param contentLength the number of bytes that will be sent (or -1 if not known). (In this
   *     subclass it is ignored.)
   * @return a outputStream. A buffered outputStream. If outputStream has already been created, the
   *     same one is returned.
   * @throws Throwable if trouble
   */
  @Override
  public OutputStream outputStream(String characterEncoding, long contentLength) throws Throwable {

    return outputStream;
  }

  /**
   * After ouputStream() has been called, this indicates the encoding (compression) being used for
   * an OutputStreamFromHttpResponse (gzip, deflate) or "identity" if no compression. !!!
   * OutputStreamSourceSimple always returns "identity", which may not be correct!!!
   */
  @Override
  public String usingCompression() {
    return "identity";
  }

  /** This returns the outputStream if it has already been created (else null). */
  @Override
  public OutputStream existingOutputStream() {
    return outputStream;
  }

  /**
   * This is useful for OutputStream types that support fileName if you want to change the download
   * fileName before the call to getOutputStream().
   */
  @Override
  public void setFileName(String tFileName) {
    // do nothing because no fileName
  }
}
