/*
 * TableWriterEsriCsv Copyright 2010, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedWriter;
import java.util.HashSet;

/**
 * TableWriterEsriCsv provides a way to write an ESRI ArcGIS compatible comma separated value ASCII
 * outputStream in chunks so that the whole table doesn't have to be in memory at one time. This is
 * used by EDDTable. The outputStream isn't obtained until the first call to writeSome(). See
 * https://support.esri.com/technical-article/000012745 which specifically refers to csv files as
 * coming from Excel. So the output here is very much like a spreadsheet csv file (but with further
 * restrictions).
 *
 * <p>Column names are truncated at 9 characters. (B, C, ... are appended to avoid duplicate names)
 * <br>
 * "longitude" is renamed "X". "latitude" is renamed "Y". <br>
 * Missing numeric values are all written as -9999. <br>
 * Double quotes in strings are replaced by 2 double quotes. <br>
 * Timestamp columns are separated into date and time columns. <br>
 * This doesn't yet truncate strings to 255 chars. (Hopefully ArcGIS does this.) <br>
 * This doesn't deal with non-ASCII chars (about which ArcGIS is vague).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-07-20
 */
public class TableWriterEsriCsv extends TableWriter {

  // set by constructor
  protected String separator = ",";
  // search for "default esri" in
  // http://www.stata-journal.com/sjpdf.html?articlenum=dm0014
  // or
  // http://docs.google.com/viewer?a=v&q=cache:iwbmxvE9DvIJ:www.stata-journal.com/sjpdf.html%3Farticlenum%3Ddm0014+esri+default+missing+value&hl=en&gl=us&pid=bl&srcid=ADGEESjrRrmh7PoDFjjsViKg06rNXFvycmSNw4U_D9Y2ZpwrS_M6D1KFokTG6UFPQmi1MbkvDtwSRw4f60ui0QXp4Tf0kIL2lVD4ykiei66-N4gZPyq7Jr4FW0N4cZH85iboEUI30AEW&sig=AHIEtbTtN5tHnXlQcP_6Nu8HZXlFHzH56w
  // Int and Float are treated differently, so ArcGIS can determine the type of data.
  protected String nanIString = "-9999";
  protected String nanFString = "-9999.0";

  // set by firstTime
  protected boolean isTimeStamp[];
  protected boolean isCharOrString[];
  protected boolean isFloat[];
  protected HashSet<String> uniqueColNames = new HashSet();
  protected BufferedWriter writer;

  public long totalNRows = 0;

  /**
   * The constructor.
   *
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   */
  public TableWriterEsriCsv(
      int tLanguage, EDD tEdd, String tNewHistory, OutputStreamSource tOutputStreamSource) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation converts them to
   *     NaNs and then to -9999 or -9999.0.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // ensure the table's structure is the same as before
    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    int nColumns = table.nColumns();
    PrimitiveArray pas[] = new PrimitiveArray[nColumns];
    for (int col = 0; col < nColumns; col++) pas[col] = table.getColumn(col);

    // do firstTime stuff
    if (firstTime) {
      // write the header
      writer = File2.getBufferedWriter88591(outputStreamSource.outputStream(File2.ISO_8859_1));

      // write the column names
      isFloat = new boolean[nColumns];
      isCharOrString = new boolean[nColumns];
      isTimeStamp = new boolean[nColumns];
      for (int col = 0; col < nColumns; col++) {
        PAType elementPAType = pas[col].elementType();
        isFloat[col] = (elementPAType == PAType.FLOAT) || (elementPAType == PAType.DOUBLE);
        isCharOrString[col] = elementPAType == PAType.CHAR || elementPAType == PAType.STRING;
        String u = table.columnAttributes(col).getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));

        // shapefile colNames limited to 10 char
        // colName shortening here is crude -- may sometimes result in columns with same names!
        String colName = table.getColumnName(col);
        if (isTimeStamp[col]) {
          // split into date column and time column
          if (colName.equals(EDV.TIME_NAME)) {
            writer.write(makeUnique("date") + separator + makeUnique(EDV.TIME_NAME));
          } else {
            // make 8char+(D|T), then makeUnique
            if (colName.length() > 8) colName = colName.substring(0, 8);
            writer.write(makeUnique(colName + "D") + separator + makeUnique(colName + "T"));
          }
        } else {
          if (colName.equals(EDV.LON_NAME)) colName = "X";
          if (colName.equals(EDV.LAT_NAME)) colName = "Y";
          writer.write(makeUnique(colName));
        }
        writer.write(col == nColumns - 1 ? "\n" : separator);
      }
    }

    // *** do everyTime stuff
    table.convertToStandardMissingValues(); // to NaNs

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    int nRows = table.nRows();
    boolean flushAfterward = totalNRows == 0; // flush initial chunk so info gets to user quickly
    totalNRows += nRows;
    Math2.ensureArraySizeOkay(totalNRows, "ESRI CSV");

    // write the data
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        if (isTimeStamp[col]) {
          // split into date column 2010-07-20 and time column 07:45:00
          double d = pas[col].getDouble(row);
          String iso = Calendar2.safeEpochSecondsToIsoStringTZ(d, "");

          if (iso.length() >= 19) {
            // "1/20/2006 9:00:00 pm"
            String usStyle = Calendar2.formatAsUSSlashAmPm(Calendar2.epochSecondsToGc(d));
            int spo = usStyle.indexOf(' ');
            writer.write(
                iso.substring(0, 10)
                    + separator
                    + (spo < 0 ? "" : usStyle.substring(spo + 1))); // spo < 0 shouldn't ever happen
          } else {
            // missing value
            writer.write(separator); // mv on either side
          }
        } else if (isCharOrString[col]) {
          writer.write(String2.toNccsv127DataString(pas[col].getString(row)));
        } else {
          // numeric
          String s = pas[col].getString(row);
          writer.write(s.length() == 0 ? (isFloat[col] ? nanFString : nanIString) : s);
        }
        writer.write(col == nColumns - 1 ? "\n" : separator);
      }
    }

    if (flushAfterward) writer.flush();
  }

  protected String makeUnique(String colName) {
    if (colName.length() > 10) colName = colName.substring(0, 10);
    if (uniqueColNames.add(colName)) return colName;

    if (colName.length() > 9) colName = colName.substring(0, 9);
    int ch = 65;
    while (!uniqueColNames.add(colName + (char) ch)) ch++;
    return colName + (char) ch;
  }

  /**
   * This writes any end-of-file info to the stream and flush the stream. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    // check for MustBe.THERE_IS_NO_DATA
    if (writer == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log("TableWriterEsriCsv done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * This is a convenience method to write an entire table in one step.
   *
   * @throws Throwable if trouble (no columns is trouble; no rows is not trouble)
   */
  public static void writeAllAndFinish(
      int language,
      EDD tEdd,
      String tNewHistory,
      Table table,
      OutputStreamSource tOutputStreamSource)
      throws Throwable {

    TableWriterEsriCsv twsv =
        new TableWriterEsriCsv(language, tEdd, tNewHistory, tOutputStreamSource);
    twsv.writeAllAndFinish(table);
  }
}
