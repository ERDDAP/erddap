/* 
 * TableWriterJsonl Copyright 2016, NOAA.
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

/**
 * TableWriterJsonl provides a way to write a table 
 * to JSON (https://www.json.org/) file
 * in a JSON Lines (http://jsonlines.org/) format
 * in chunks so that the whole table doesn't have to be in memory 
 * at one time.
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to write().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2016-11-15
 */
public class TableWriterJsonl extends TableWriter {

    //set by constructor
    protected String jsonp;
    protected boolean writeColNames, writeKVP;

    //set by firstTime
    protected volatile boolean isTimeStamp[];
    protected volatile String time_precision[];
    protected volatile BufferedWriter writer;

    //other
    public volatile long totalNRows = 0;

    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     * @param tWriteKVP if true, this writes colName=value. If false, this writes
     *     just the values (the "Better than CSV" example at http://jsonlines.org/examples/)
     * @param tJsonp the not-percent-encoded jsonp functionName to be prepended to the results 
     *     (or null if none).
     *     See https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/
     *     and https://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/
     *     and https://www.raymondcamden.com/2014/03/12/Reprint-What-in-the-heck-is-JSONP-and-why-would-you-use-it/ .
     *     A SimpleException will be thrown if tJsonp is not null but isn't String2.isVariableNameSafe.
     *     ! I think jsonp never makes sense for jsonl output, which isn't one json object.
     */
    public TableWriterJsonl(EDD tEdd, String tNewHistory, 
        OutputStreamSource tOutputStreamSource, boolean tWriteColNames,
        boolean tWriteKVP, String tJsonp) {

        super(tEdd, tNewHistory, tOutputStreamSource);
        writeColNames = tWriteColNames;
        writeKVP = tWriteKVP;
        jsonp = tJsonp;
        if (jsonp != null && !String2.isJsonpNameSafe(jsonp))
            throw new SimpleException(EDStatic.errorJsonpFunctionName);
    }


    /**
     * This adds the current contents of table (a chunk of data) to the OutputStream.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * @param table with destinationValues.
     *   The table should have missing values stored as destinationMissingValues
     *   or destinationFillValues.
     *   This implementation converts them to NaNs and stores them as nulls.
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
                outputStreamSource.outputStream(String2.UTF_8), String2.UTF_8));  //a requirement
            if (jsonp != null) 
                writer.write(jsonp + "(\n"); //I think this never makes sense for jsonl

            //write the column names
            if (writeColNames && !writeKVP) {
                writer.write('[');
                for (int col = 0; col < nColumns; col++) {
                    if (col > 0)
                        writer.write(", ");
                    writer.write(String2.toJson(table.getColumnName(col)));
                }
                writer.write("]\n");
            }

        }

        //*** do everyTime stuff
        table.convertToStandardMissingValues();  //to NaNs

        //avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
        int nRows = table.nRows();
        totalNRows += nRows;
        EDStatic.ensureArraySizeOkay(totalNRows, "jsonl"); 

        //write the data
        for (int row = 0; row < nRows; row++) {
            writer.write(writeKVP? '{' : '['); //beginRow
            for (int col = 0; col < nColumns; col++) {
                if (col > 0) writer.write(", "); 
                if (writeKVP) {
                    writer.write(String2.toJson(table.getColumnName(col)));
                    writer.write(':');
                }
                if (isTimeStamp[col]) {
                    double d = pas[col].getDouble(row);
                    writer.write(Double.isNaN(d)? "null" : 
                        "\"" + Calendar2.epochSecondsToLimitedIsoStringT(
                        time_precision[col], d, "") + "\"");
                } else {
                    writer.write(pas[col].getJsonString(row));
                }
            }
            writer.write(writeKVP? "}\n" : "]\n"); //endRow    //recommended: always just \n
        }       

        //ensure it gets to user right away
        if (nRows > 1) //some callers work one row at a time; avoid excessive flushing
            writer.flush(); 

    }

    
    /**
     * This writes any end-of-file info to the stream and flushes the stream.
     * If ignoreFinish=true, nothing will be done.
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        if (ignoreFinish) 
            return;

        //check for MustBe.THERE_IS_NO_DATA
        if (writer == null)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

        //end of big array
        if (jsonp != null) 
            writer.write(")");
        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("TableWriterJsonl done. TIME=" + 
                (System.currentTimeMillis() - time) + "ms\n");

    }


    
    /**
     * This is a convenience method to write an entire table in one step.
     *
     * @throws Throwable if trouble  (no columns is trouble; no rows is not trouble)
     */
    public static void writeAllAndFinish(EDD tEdd, String tNewHistory, Table table, 
        OutputStreamSource outputStreamSource, 
        boolean tWriteColNames, boolean tWriteKVP, String tJsonp)
        throws Throwable {

        TableWriterJsonl twjl = new TableWriterJsonl(tEdd, tNewHistory, 
            outputStreamSource, tWriteColNames, tWriteKVP, tJsonp);
        twjl.writeAllAndFinish(table);
    }

}



