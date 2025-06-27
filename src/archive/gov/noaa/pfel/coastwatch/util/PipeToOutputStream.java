/*
 * PipeToOutputStream Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import java.io.OutputStream;

/**
 * This class is used by SSR.cShell and dosShell to create a separate thread to grab info from the
 * out or err inputStream from runtime.exec() and send it to an outputStream. The basic idea is from
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html, but the code here is very
 * different.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-03-11
 */
public class PipeToOutputStream extends PipeTo {
  private OutputStream outputStream;

  /**
   * A constructor
   *
   * @param outputStream the outputStream
   */
  public PipeToOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  /**
   * This method grabs all the info from inputStream and sends it to outputStream. The run method is
   * the standard starting place when the thread is run.
   */
  @Override
  public void run() {
    try {
      byte[] buf = new byte[4096];
      int len;
      while ((len = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, len);
      }
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
    }
  }

  /**
   * Print a message to the storage container.
   *
   * @param message
   */
  @Override
  public void print(String message) {
    try {
      outputStream.write(String2.toByteArray(message));
    } catch (Exception e) {
      String2.log(String2.ERROR + ": " + MustBe.throwable("PipeToOutputStream.print", e));
    }
  }
}
