/* 
 * TableWriterJson Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * TableWriterJson provides a way to write a table to JSON
 * (http://www.json.org/)
 * outputStream in chunks so that the whole table doesn't have to be in memory 
 * at one time.
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to write().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-24
 */
public class TableWriterJson extends TableWriter {

    //set by constructor
    protected String jsonp;
    protected boolean writeUnits;

    //set by firstTime
    protected boolean isString[];
    protected boolean isTimeStamp[];
    protected String time_precision[];
    protected BufferedWriter writer;

    //other
    protected boolean rowsWritten = false;
    public long totalNRows = 0;

    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     * @param tJsonp the not-percent-encoded jsonp functionName to be prepended to the results 
     *     (or null if none).
     *     See http://niryariv.wordpress.com/2009/05/05/jsonp-quickly/
     *     and http://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/
     *     and http://www.insideria.com/2009/03/what-in-the-heck-is-jsonp-and.html .
     *     A SimpleException will be thrown if tJsonp is not null but isn't String2.isVariableNameSafe.
     * @param tWriteUnits if true, the units information will be written to the file
     */
    public TableWriterJson(OutputStreamSource tOutputStreamSource, String tJsonp, 
            boolean tWriteUnits) {
        super(tOutputStreamSource);
        jsonp = tJsonp;
        if (jsonp != null && !String2.isJsonpNameSafe(jsonp))
            throw new SimpleException(EDStatic.errorJsonpFunctionName);
        writeUnits = tWriteUnits;
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

        //do firstTime stuff
        int nColumns = table.nColumns();
        if (firstTime) {
            isTimeStamp = new boolean[nColumns];
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
                }
            }

            //write the header
            writer = new BufferedWriter(new OutputStreamWriter(
                outputStreamSource.outputStream("UTF-8"), "UTF-8"));
            if (jsonp != null) 
                writer.write(jsonp + "(");

            //write the column names   
            isString = new boolean[nColumns];
            writer.write(
                "{\n" +
                "  \"table\": {\n" + //begin main structure
                "    \"columnNames\": [");
            for (int col = 0; col < nColumns; col++) {
                isString[col] = table.getColumn(col).elementClass() == String.class;
                writer.write(String2.toJson(table.getColumnName(col)));
                writer.write(col == nColumns - 1? "],\n" : ", ");
            }

            //write the types   
            writer.write("    \"columnTypes\": [");
            for (int col = 0; col < nColumns; col++) {
                String s = table.getColumn(col).elementClassString();
                if (isTimeStamp[col])
                    s = "String"; //not "double"
                writer.write(String2.toJson(s));  //nulls written as: null
                writer.write(col == nColumns - 1? "],\n" : ", ");
            }

            //write the units   
            if (writeUnits) {
                writer.write("    \"columnUnits\": [");
                for (int col = 0; col < nColumns; col++) {
                    String s = table.columnAttributes(col).getString("units");
                    if (isTimeStamp[col])
                        s = "UTC"; //no longer true: "seconds since 1970-01-01..."
                    writer.write(String2.toJson(s));  //nulls written as: null
                    writer.write(col == nColumns - 1? "],\n" : ", ");
                }
            }

            writer.write("    \"rows\": [\n");
        }

        //*** do everyTime stuff
        convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is unchanged

        //avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
        int nRows = table.nRows();
        totalNRows += nRows;
        EDStatic.ensureArraySizeOkay(totalNRows, "JSON");

        //write the data
        if (rowsWritten) writer.write(",\n"); //end previous row
        for (int row = 0; row < nRows; row++) {
            writer.write("      ["); //beginRow
            for (int col = 0; col < nColumns; col++) {
                if (col > 0) writer.write(", "); 
                if (isTimeStamp[col]) {
                    double d = table.getDoubleData(col, row);
                    writer.write(Double.isNaN(d)? "null" : 
                        "\"" + Calendar2.epochSecondsToLimitedIsoStringT(
                        time_precision[col], d, "") + "\"");
                } else if (isString[col]) {
                    String s = table.getStringData(col, row);
                    writer.write(String2.toJson(s));
                } else {
                    String s = table.getStringData(col, row);
                    //represent NaN as null? yes, that is what json library does
                    writer.write(s.length() == 0? "null" : s); 
                }
            }
            writer.write(row < nRows - 1? "],\n" : "]"); //endRow
        }       
        if (nRows > 0) rowsWritten = true;

        //ensure it gets to user right away
        if (nRows > 1) //some callers work one row at a time; avoid excessive flushing
            writer.flush(); 

    }

    
    /**
     * This writes any end-of-file info to the stream and flushes the stream.
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        //check for MustBe.THERE_IS_NO_DATA
        if (writer == null)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

        //end of big array
        writer.write(
            "\n" +
            "    ]\n" + //end of rows array
            "  }\n" + //end of table
            "}\n"); //end of main structure
        if (jsonp != null) 
            writer.write(")");
        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("TableWriterJson done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }


    
    /**
     * This is a convenience method to write an entire table in one step.
     *
     * @throws Throwable if trouble  (no columns is trouble; no rows is not trouble)
     */
    public static void writeAllAndFinish(Table table, OutputStreamSource outputStreamSource, 
        String tJsonp, boolean writeUnits) throws Throwable {

        TableWriterJson twj = new TableWriterJson(outputStreamSource, tJsonp, writeUnits);
        twj.writeAllAndFinish(table);
    }

}



