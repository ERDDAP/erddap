/* 
 * WaitThenTryAgainException Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.SimpleException;

/** 
 * This exception should only be used if a (hopefully) temporary error occurs 
 * when responding to a data request, where an EDD subclass's requestReloadASAP() was
 * called right before throwing this exception.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-06
 */
public class WaitThenTryAgainException extends SimpleException { 

    /**
     * Constructs a new runtime exception with the specified detail message.
     * It is strongly recommended that the exception message start with EDStatic.waitThenTryAgain.
     */
    WaitThenTryAgainException(String message) {
        super(message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and cause.
     * It is strongly recommended that the exception message start with EDStatic.waitThenTryAgain.
     */
    WaitThenTryAgainException(String message, Throwable cause) {
        super(message, cause);
    }

}
