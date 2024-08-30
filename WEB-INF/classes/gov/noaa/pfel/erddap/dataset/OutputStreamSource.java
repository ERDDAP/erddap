/*
 * OutputStreamSource Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import java.io.OutputStream;

/**
 * OutputStreamSource provides an OutputStream upon request. This is used by Erddap to delay calling
 * response.getOutputStream as long as possible.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-01
 */
public interface OutputStreamSource {

  /** A variant of outputStream() for when the contentLength isn't known. */
  public OutputStream outputStream(String characterEncoding) throws Throwable;

  /** This returns the outputStream if it has already been created (else null). */
  public OutputStream existingOutputStream();

  /**
   * This is useful for OutputStream types that support fileName if you want to change the download
   * fileName before the call to getOutputStream().
   */
  public void setFileName(String tFileName);

  /**
   * This returns an OutputStream. If called repeatedly, this returns the same outputStream.
   *
   * @param characterEncoding e.g., "" (for none specified), File2.UTF_8, or "" (for DAP). This
   *     parameter only matters the first time this method is called. This only matters for some
   *     subclasses.
   * @param contentLength the number of bytes that will be sent (or -1 if not known). Currently,
   *     this causes Firefox to freeze, so this method ignores it.
   * @return outputStream A buffered outputStream. If outputStream has already been created, the
   *     same one is returned.
   * @throws Throwable if trouble
   */
  public OutputStream outputStream(String characterEncoding, long contentLength) throws Throwable;

  /**
   * After ouputStream() has been called, this indicates the encoding (compression) being used for
   * an OutputStreamFromHttpResponse (gzip, deflate) or "identity" if no compression.
   */
  public String usingCompression();
}
