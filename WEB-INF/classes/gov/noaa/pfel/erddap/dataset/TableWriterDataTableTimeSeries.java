/* 
 * TableWriterJson Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 *
 * TableWriterDataTableTimeSeries provides a way to write a table to JSON which follows the conventions of a Google Charts
 * <a href="https://developers.google.com/chart/interactive/docs/reference#dataparam">DataTable</a>
 * outputStream in chunks so that the whole table doesn't have to be in memory
 * at one time. The resulting JSON is suitable creating on
 * a <a href="https://developers.google.com/chart/interactive/docs/gallery/annotationchart">Google Annotation Chart</a> (a time series with annotaions)
 * and follows the required convention time properly formatted as the first to column.
 * There is as support library (https://developers.google.com/chart/interactive/docs/dev/dsl_intro), but it's not clear how to make it stream chunks, so
 * this implementation builds the JSON character by character. The format is
 *
 *{
 *  "cols": [
 *    {"id":"","label":"Topping","pattern":"","type":"string"},
 *    {"id":"","label":"Slices","pattern":"","type":"number"}
 *  ],
 *  "rows": [
 *    {"c":[{"v":"Mushrooms","f":null},{"v":3,"f":null}]},
 *    {"c":[{"v":"Onions","f":null},{"v":1,"f":null}]},
 *    {"c":[{"v":"Olives","f":null},{"v":1,"f":null}]},
 *    {"c":[{"v":"Zucchini","f":null},{"v":1,"f":null}]},
 *    {"c":[{"v":"Pepperoni","f":null},{"v":2,"f":null}]}
 *  ]
 *}
 *
 *
 *
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to write().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-24
 * @author Roland Schweitzer (roland.schweitzer@noaa.gov) May 2018
 */
public class TableWriterDataTableTimeSeries extends TableWriterDataTable {

    int timeColumnIndex = -1;
    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     * @param tWriteUnits if true, the units information will be written to the file
     */
    public TableWriterDataTableTimeSeries(EDD tEdd, String tNewHistory,
        OutputStreamSource tOutputStreamSource, boolean tWriteUnits) {
        super(tEdd, tNewHistory, tOutputStreamSource, tWriteUnits);
    }


    /**
     * This adds the current contents of table (a chunk of data) to the OutputStream.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * <p>The table should have missing values stored as destinationMissingValues
     * or destinationFillValues.
     * This implementation converts them to NaNs and stores them as nulls.
     *
     * @param table with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
        if (table.nRows() == 0) 
            return;

        //ensure the table's structure is the same as before
        boolean firstTime = columnNames == null;
        ensureCompatible(table);

        int nColumns = table.nColumns();
        PrimitiveArray pas[] = new PrimitiveArray[nColumns];
        for (int col = 0; col < nColumns; col++) 
            pas[col] = table.getColumn(col);

        //do firstTime stuff


        if (firstTime) {

            isTimeStamp = new boolean[nColumns];
            isCharOrString = new boolean[nColumns];
            time_precision = new String[nColumns];
            for (int col = 0; col < nColumns; col++) {
                Attributes catts = table.columnAttributes(col);
                String u = catts.getString("units");
                isTimeStamp[col] = u != null && 
                    (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
                if (isTimeStamp[col]) {
                    //just keep time_precision if it includes fractional seconds 
                    String tp = catts.getString(EDV.TIME_PRECISION);
                    if (tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) 
                        tp = null; //default
                    time_precision[col] = tp;
                    timeColumnIndex = col;
                }

                isCharOrString[col] = pas[col].elementClass() == char.class ||
                                      pas[col].elementClass() == String.class;

            }

            // Begin by adding the time column to the data table.
            if ( timeColumnIndex >= 0 ) {
                String name = table.getColumnName(timeColumnIndex);
                writer = new BufferedWriter(new OutputStreamWriter(outputStreamSource.outputStream(String2.UTF_8), String2.UTF_8));
                writer.write("{\"cols\":[");
                writer.write("{\"id\":\""+name+"\",\"label\":\""+name+"\",\"pattern\":\"\",\"type\":\"datetime\"}");
            } else {
                // Throw no time column found in requested data set.
                    throw new SimpleException("Time series DataTables must contain a time column.");
            }

            for (int col = 0; col < nColumns; col++) {
                if ( col != timeColumnIndex ) {
                    Attributes catts = table.columnAttributes(col);
                    String type = pas[col].elementClassString();
                    String name = table.getColumnName(col);
                    String u = catts.getString("units");
                    writer.write(",");
                    if (type.equals("String") ) {
                        if ( writeUnits) {
                          writer.write("{\"id\":\""+name+"\",\"label\":\""+name+" (" + u + ") " + "\",\"pattern\":\"\",\"type\":\"string\"}");
                        } else {
                          writer.write("{\"id\":\""+name+"\",\"label\":\""+name+ "\",\"pattern\":\"\",\"type\":\"string\"}");
                        }
                    } else if (type.equals("float")) {
                        if ( writeUnits ) {
                          writer.write("{\"id\":\""+name+"\",\"label\":\""+name+" (" + u + ") " + "\",\"pattern\":\"\",\"type\":\"number\"}");
                        } else {
                          writer.write("{\"id\":\""+name+"\",\"label\":\""+name+"\",\"pattern\":\"\",\"type\":\"number\"}");
                        }
                    } else if (type.equals("double")) {
                        if ( writeUnits ) {
                          writer.write("{\"id\":\""+name+"\",\"label\":\""+name+" (" + u + ") " + "\",\"pattern\":\"\",\"type\":\"number\"}");
                        } else {
                          writer.write("{\"id\":\""+name+"\",\"label\":\""+name+"\",\"pattern\":\"\",\"type\":\"number\"}");
                        }
                    } else {
                        throw new SimpleException("Column "+name+" is of unknown type.");
                    }
                }
            }
            writer.write("],\"rows\": [");
        }

        //*** do everyTime stuff
        convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is unchanged

        //avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
        int nRows = table.nRows();
        totalNRows += nRows;
        EDStatic.ensureArraySizeOkay(totalNRows, "json");

        if ( rowsWritten ) {
            // Some rows already written. Need a comma.
            writer.write(",");
        }
        // Add the new rows to the data table.

        for (int row = 0; row < nRows; row++) {
            double d = pas[timeColumnIndex].getDouble(row);

            GregorianCalendar gc = Calendar2.epochSecondsToGc(d);

            int year = gc.get(Calendar.YEAR);
            int month = gc.get(Calendar.MONTH);
            int day = gc.get(Calendar.DAY_OF_MONTH);
            int hour = gc.get(Calendar.HOUR_OF_DAY);
            int minute = gc.get(Calendar.MINUTE);
            int second = gc.get(Calendar.SECOND);
            int milli = gc.get(Calendar.MILLISECOND);

            writer.write("{\"c\":[{\"v\":\"Date("+year+", "+month+", "+day+", "+hour+", "+minute+", "+second+", "+milli+")\",\"f\":null}");

            for (int col = 0; col < nColumns; col++) {
                if (isTimeStamp[col]) {
                    // Already done, since it has to be first.
                } else if (isCharOrString[col]) {
                    writer.write(",");
                    String value = pas[col].getString(row);
                    writer.write("{\"v\":\""+value+"\",\"f\":null}");
                } else {
                    writer.write(",");
                    String s = pas[col].getString(row);
                    String eclass = pas[col].elementClassString();
                    writeNumber(s, eclass);
                }
            }
            if ( row < nRows-1 ) { 
               writer.write("]},");    
            } else {
               writer.write("]}");    
            }
        }
        if (nRows > 0) rowsWritten = true;

        //ensure it gets to user right away
        if (nRows > 1) //some callers work one row at a time; avoid excessive flushing
            writer.flush(); 

    }

}



