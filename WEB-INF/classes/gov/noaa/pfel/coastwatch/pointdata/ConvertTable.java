/*
 * ConvertTable Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.util.String2;

/**
 * This class is designed to be a stand-alone program to convert from one type of tabular data file
 * to another. See Table.convert() for details.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-02-14
 */
public class ConvertTable {

  /**
   * This class is designed to be a stand-alone program to convert from one type of tabular data
   * file to another. See msg below for details.
   *
   * <p>This is tested in Table.
   *
   * <p>A test which reads data from a 1-level opendap sequence and writes it to an .nc file:
   * ConvertTable.main(new String[]{
   * "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle?longitude,latitude,time,sal00,temperature0&amp;time=2002-08-19T08:58:00",
   * "2", "result.nc", "1", "row"});
   *
   * @param args must have 5 values: &lt;in&gt; &lt;inType&gt; &lt;out&gt; &lt;outType&gt;
   *     &lt;dimensionName&gt; <br>
   *     &lt;in&gt; must be the complete directory + name + extension or the complete url for a 1-
   *     or 2-level opendap sequence. <br>
   *     &lt;inType&gt; maybe 0 (ASCII), 1 (.nc), or 2 (opendapSequence). <br>
   *     &lt;out&gt; must be the complete directory + name + extension. <br>
   *     &lt;outType&gt; may be 0 (tabbed ASCII), 1 (flat .nc), 2 (4D .nc; the first 4 columns must
   *     be the 4 dimensions, usually 0=lon, 1=lat, 2=depth, 3=time), or 3 (.mat). <br>
   *     &lt;dimensionName&gt; e.g., "row", "observation", or "time".
   */
  public static void main(String args[]) throws Exception {
    if (args == null) args = new String[0];

    // route calls to a logger to com.cohort.util.String2Log
    String2.setupCommonsLogging(-1);

    // Table.verbose = true;

    if (args.length != 5) {
      String2.log(
          "This program converts one type of tabular data file into another type.\n\n"
              + "Usage: ConvertTable <in> <inType> <out> <outType>\n"
              + "  <in> must be the complete input file name (directory + name + extension)\n"
              + "       or the complete url for a 1- or 2-level (DAPPER-style) opendap sequence\n"
              + "       (optionally with a not yet percent-encoded query).\n"
              + "     * <in> may end in \".zip\", in which case the file will be unzipped.\n"
              + "       If it is zipped, the data file should be the only file in the\n"
              + "       .zip file and the data file's name should be <in> minus the directory\n"
              + "       and the \".zip\" at the end.\n"
              + "     * All of the data in the file will be read.\n"
              + "     * If the data is packed (e.g., scale_factor, add_offset),\n"
              + "       this will not unpack it.\n"
              + "     * ASCII files must have column names on the first line and data \n"
              + "       starting on the second line.\n"
              + "       * The item separator on each line can be tab, comma, or 1 or more spaces.\n"
              + "       * Missing values for tab- and comma-separated files can be \"\" or \".\" or \"NaN\".\n"
              + "       * Missing values for space-separated files can be \".\" or \"NaN\".\n"
              + "       * All data rows must have the same number of data items. \n"
              + "       * The data is initially read as Strings. Then columns are simplified\n"
              + "           (e.g., to doubles, ... or bytes) so they store the data compactly.\n"
              + "       * Currently, date strings are left as strings.\n"
              + "  <inType> maybe 0 (ASCII), 1 (.nc), or 2 (opendapSequence)\n"
              + "  <out> must be the complete output file name (directory + name + extension)\n"
              + "  <outType> may be 0 (tabbed ASCII), 1 (.nc),\n"
              + "     2 (4D .nc; the first 4 columns must be the 4 dimensions,\n"
              + "       usually 0=lon, 1=lat, 2=depth, 3=time), or\n"
              + "     3 (.mat).\n"
              + "  <dimensionName> For example, 'time', 'station', 'observation', or 'row'."
              + "The command line arguments were: "
              + String2.toCSSVString(args));
      return;
    }

    Table.convert(
        args[0], String2.parseInt(args[1]), args[2], String2.parseInt(args[3]), args[4], false);
  }
}
