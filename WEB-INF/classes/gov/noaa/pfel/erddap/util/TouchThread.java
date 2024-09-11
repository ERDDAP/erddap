/*
 * TouchThread Copyright 2022, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.SSR;

/**
 * This does a series of touches.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2022-08-27
 */
public class TouchThread extends Thread {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  public static int TIMEOUT_MILLIS = 60 * 1000;
  public static int sleepMillis = 500;

  // set while running
  private long lastStartTime = -1; // for 1 touch

  /** The constructor. TouchThread uses touch variables in EDStatic. */
  public TouchThread(int tNextTouch) {
    EDStatic.nextTouch.set(tNextTouch);
    EDStatic.lastFinishedTouch = tNextTouch - 1;
    setName("TouchThread");
  }

  /** This returns elapsed time for the current touch (or -1 if no touch is running). */
  public long elapsedTime() {
    return lastStartTime == -1 ? -1 : System.currentTimeMillis() - lastStartTime;
  }

  /** This sleeps, then does any pending touches. */
  @Override
  public void run() {
    while (true) {

      // sleep
      Math2.sleep(sleepMillis);

      // check isInterrupted
      if (isInterrupted()) {
        String2.log(
            "%%% TouchThread was interrupted at " + Calendar2.getCurrentISODateTimeStringLocalTZ());
        return; // only return (stop thread) if interrupted
      }

      while (EDStatic.nextTouch.get() < EDStatic.touchList.size()) {
        String url = null;
        try {
          // check isInterrupted
          if (isInterrupted()) {
            String2.log(
                "%%% TouchThread was interrupted at "
                    + Calendar2.getCurrentISODateTimeStringLocalTZ());
            return; // only return (stop thread) if interrupted
          }

          // start to do the touch
          // do these things quickly to keep internal consistency
          EDStatic.nextTouch.incrementAndGet();
          url = EDStatic.touchList.get(EDStatic.nextTouch.get() - 1);
          lastStartTime = System.currentTimeMillis();
          String2.log(
              "%%% TouchThread started touch #"
                  + EDStatic.nextTouch.get()
                  + " of "
                  + (EDStatic.touchList.size() - 1)
                  + " at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ()
                  + " url="
                  + url);

          // do the touch
          SSR.touchUrl(url, TIMEOUT_MILLIS, true); // handleS3ViaSDK=false

          // touch finished successfully
          long tElapsedTime = elapsedTime();
          String2.log(
              "%%% TouchThread touch #"
                  + (EDStatic.nextTouch.get() - 1)
                  + " of "
                  + (EDStatic.touchList.size() - 1)
                  + " succeeded.  elapsedTime="
                  + tElapsedTime
                  + "ms"
                  + (tElapsedTime > 10000 ? " (>10s!)" : ""));
          String2.distributeTime(tElapsedTime, EDStatic.touchThreadSucceededDistribution24);
          String2.distributeTime(tElapsedTime, EDStatic.touchThreadSucceededDistributionTotal);

        } catch (InterruptedException e) {
          String2.log("%%% TouchThread was interrupted.");
          return; // only return (stop thread) if interrupted

        } catch (Exception e) {
          long tElapsedTime = elapsedTime();
          String2.log(
              "%%% TouchThread error: touch #"
                  + (EDStatic.nextTouch.get() - 1)
                  + " failed after "
                  + tElapsedTime
                  + "ms"
                  + (tElapsedTime > 10000 ? " (>10s!)" : "")
                  + " url="
                  + url
                  + "\n"
                  + MustBe.throwableToString(e));
          String2.distributeTime(tElapsedTime, EDStatic.touchThreadFailedDistribution24);
          String2.distributeTime(tElapsedTime, EDStatic.touchThreadFailedDistributionTotal);

        } finally {
          // whether succeeded or failed
          lastStartTime = -1;
          synchronized (EDStatic.touchList) {
            EDStatic.lastFinishedTouch = (EDStatic.nextTouch.get() - 1);
            EDStatic.touchList.set(
                (EDStatic.nextTouch.get() - 1), null); // throw away the touch info (gc)
          }
        }
      }
    }
  }
}
