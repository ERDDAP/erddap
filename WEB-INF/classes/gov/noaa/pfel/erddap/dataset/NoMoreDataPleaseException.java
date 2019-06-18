/* 
 * NoMoreDataPleaseException Copyright 2018, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.SimpleException;

/** 
 * This exception may be used when a TableWriter says NoMoreDataPlease.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2018-07-26
 */
public class NoMoreDataPleaseException extends SimpleException { 

    /**
     * Constructs a new exception.
     */
    NoMoreDataPleaseException() {
        super("NoMoreDataPlease");
    }


}
