/*
 * PipeTo Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import java.io.InputStream;

/**
 * Implementors of the interface can capture data from an inputStream. The basic idea is from
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html, but the code here is very
 * different.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-18
 */
public abstract class PipeTo extends Thread {

  InputStream inputStream;

  /**
   * Specify the inputStream.
   *
   * @param inputStream the inputStream
   */
  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * This method grabs all the info from inputStream and stores it in an internal ArrayList. The run
   * method is the standard starting place when the thread is run.
   */
  @Override
  public abstract void run();

  /**
   * Print a message to the storage container.
   *
   * @param message
   */
  public abstract void print(String message);
}
