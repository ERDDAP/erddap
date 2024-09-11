/* Table Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.util.File2;
import com.cohort.util.String2;

/**
 * This class makes the data on 1 row of a table accessible to JexlScript scripts via
 * "row.<i>name</i>()" methods.
 *
 * <p>This class is Copyright 2019, NOAA.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-11-14
 */
public class ScriptRow {

  private String fullFileName = "";
  private String fileName = "";
  private Table table;
  private int row = 0;

  /**
   * The constructor.
   *
   * @param fullFileName The full name (perhaps a URL) of the current file, or "" if the source is
   *     not file-like.
   */
  public ScriptRow(String tFullFileName, Table tTable) {
    if (String2.isSomething(tFullFileName)) {
      fullFileName = tFullFileName;
      fileName = File2.getNameAndExtension(fullFileName);
    }
    table = tTable;
  }

  /**
   * This gets the full name (perhaps a URL) of the current file, or "" if the source is not
   * file-like.
   */
  public String getFullFileName() {
    return fullFileName;
  }

  /** This gets the short name of the current file, or "" if the source is not file-like. */
  public String getFileName() {
    return fileName;
  }

  /**
   * Set the current row number in the table (0..). Only the controller (ERDDAP) should call this.
   * Scripts shouldn't call this.
   */
  public void setRow(int tRow) {
    row = tRow;
  }

  /** Get the current row in the table (0..). */
  public int getRow() {
    return row;
  }

  /**
   * This gets the value from a column as an int.
   *
   * @param colName the columnName
   * @return the value as an int (or Int.MAX_VALUE if column not found or other trouble)
   */
  public int columnInt(String colName) {
    int col = table.findColumnNumber(colName);
    return col < 0 ? Integer.MAX_VALUE : table.getColumn(col).getInt(row);
  }

  /**
   * This gets the value from a column as a long.
   *
   * @param colName the columnName
   * @return the value as a long (or Long.MAX_VALUE if column not found or other trouble)
   */
  public long columnLong(String colName) {
    int col = table.findColumnNumber(colName);
    return col < 0 ? Long.MAX_VALUE : table.getColumn(col).getLong(row);
  }

  /**
   * This gets the value from a column as a float.
   *
   * @param colName the columnName
   * @return the value as a float (or NaN if column not found or other trouble)
   */
  public float columnFloat(String colName) {
    int col = table.findColumnNumber(colName);
    return col < 0 ? Float.NaN : table.getColumn(col).getFloat(row);
  }

  /**
   * This gets the value from a column as a double.
   *
   * @param colName the columnName
   * @return the value as a double (or NaN if column not found or other trouble)
   */
  public double columnDouble(String colName) {
    int col = table.findColumnNumber(colName);
    return col < 0 ? Double.NaN : table.getColumn(col).getDouble(row);
  }

  /**
   * This gets the value from a column as a String.
   *
   * @param colName the columnName
   * @return the value as a String (or "" if column not found)
   */
  public String columnString(String colName) {
    int col = table.findColumnNumber(colName);
    return col < 0 ? "" : table.getColumn(col).getString(row);
  }
}
