/*
 * FromErddap Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

/**
 * This identifies that extra methods that EDDTableFromErddap and EDDGridFromErddap share.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-12-14
 */
public interface FromErddap {

  /** The version of the source ERDDAP. */
  public double sourceErddapVersion(); // e.g., 1.76

  public int intSourceErddapVersion(); // e.g., 176

  /** This returns the source ERDDAP's local URL. */
  public String getLocalSourceErddapUrl();

  /** This returns the source ERDDAP's public URL. */
  public String getPublicSourceErddapUrl();

  /** This indicates whether user requests should be redirected. */
  public boolean redirect();
}
