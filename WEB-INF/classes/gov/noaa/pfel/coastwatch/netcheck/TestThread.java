/*
 * TestThread Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

// import com.cohort.util.String2;
// import com.cohort.util.Test;

/**
 * This allows NetCheck to run a test in a separate thread (which can be killed if the test doesn't
 * respond fast enough).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-08
 */
public class TestThread extends Thread {

  private NetCheckTest test;
  private String result = null;

  /**
   * The constructor.
   *
   * @param test the test to be run
   * @throws Exception if trouble
   */
  public TestThread(NetCheckTest test) {
    this.test = test;
  }

  /** This is called when the thread is run. */
  @Override
  public void run() {
    result = test.test();
  }

  /**
   * This gets the result String from the test.
   *
   * @return the result string from the test (or null if not yet available).
   */
  public String getResult() {
    return result;
  }
}
