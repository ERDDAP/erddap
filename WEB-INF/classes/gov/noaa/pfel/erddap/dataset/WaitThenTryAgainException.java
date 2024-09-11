/*
 * WaitThenTryAgainException Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.SimpleException;

/**
 * This exception should be used if an unexpected error occurs when responding to a data request,
 * where an EDD subclass's requestReloadASAP() was called right before throwing this exception.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-06
 */
public class WaitThenTryAgainException extends SimpleException {

  /** Not final, so EDStatic can change it. */
  public static String waitThenTryAgain =
      "There was a (temporary?) problem.  Wait a minute, then try again.  "
          + "(In a browser, click the Reload button.)";

  /**
   * Constructs a new runtime exception with the specified detail message. It is strongly
   * recommended that the exception message start with EDStatic.waitThenTryAgain.
   */
  WaitThenTryAgainException(String message) {
    super(message);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause. It is strongly
   * recommended that the exception message start with EDStatic.waitThenTryAgain.
   */
  WaitThenTryAgainException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new runtime exception with standard message (including the cause). When there is a
   * throwable cause, this is the recommended constructor because it is simple and throws the
   * standard message.
   */
  WaitThenTryAgainException(Throwable cause) {
    super(waitThenTryAgain, cause);
  }
}
