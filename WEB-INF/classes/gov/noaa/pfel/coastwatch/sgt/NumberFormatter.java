/*
 * NumberFormatter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

/** This interface defines methods needed to format nubmers. */
public interface NumberFormatter {

  public String format(double d);

  public String format(long l);
}
