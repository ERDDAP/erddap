/* 
 * FromErddap Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;



/** 
 * This identifies that extra methods that EDDTableFromErddap and EDDGridFromErddap share.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2009-12-14
 */
public interface FromErddap { 

    /** The version of the source ERDDAP. */
    public double sourceErddapVersion();

    /**
     * This returns the source ERDDAP's local url.
     */
    public String getLocalSourceErddapUrl();

    /**
     * This returns the source ERDDAP's public url.
     */
    public String getPublicSourceErddapUrl();

}
