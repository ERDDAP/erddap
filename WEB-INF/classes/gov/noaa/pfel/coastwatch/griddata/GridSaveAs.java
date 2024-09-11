/*
 * GridSaveAs Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

/**
 * This class is designed to be a stand-alone program to convert from one type of Grid data file to
 * another, or all of the files in one directory into files of another type in another directory.
 * See Grid.davesSaveAs() for details.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-11-04
 */
public class GridSaveAs {

  /**
   * This class is designed to be a stand-alone program to convert from one type of Grid data file
   * to another. See msg below for details.
   *
   * <p>This is tested in Grid.
   *
   * @param args must contain two parameters: <in> <out> . 'in' and 'out' must be the complete
   *     directory + name + extension. To convert whole directories, don't supply the name part of
   *     the 'in' and 'out'. 'in' and 'out' may also have a .zip or .gz extension.
   */
  public static void main(String args[]) throws Exception {
    // Grid.verbose = true;
    Grid.davesSaveAs(args, new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods"));
  }
}
