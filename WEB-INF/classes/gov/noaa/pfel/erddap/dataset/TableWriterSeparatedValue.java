/* 
 * TableWriterSeparatedValue Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * TableWriterSeparatedValue provides a way to write a table to comma or
 * tab separated value ASCII 
 * outputStream in chunks so that the whole table doesn't have to be in memory 
 * at one time.
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-24
 */
public class TableWriterSeparatedValue extends TableWriter {

    //set by constructor
    protected String separator;
    protected boolean quoted, writeColumnNames;
    protected char writeUnits;
    protected String nanString; 

    //set by firstTime
    protected boolean isString[];
    protected boolean isTimeStamp[];
    protected String time_precision[];
    protected BufferedWriter writer;

    public long totalNRows = 0;

    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     * @param tSeparator  usually a tab or a comma (without space.  space causes problems with Excel)
     * @param tQuoted if true, if a String value starts or ends with a space or has a double quote or comma, 
     *    the value will be written in double quotes
     *    and internal double quotes become two double quotes.
     *    In any case, newline characters are replaced by char #166 (pipe with gap).
     * @param tWriteColumnNames if false, data starts on line 0.
     * @param tWriteUnits  '0'=no, 
     *    '('=on the first line as "variableName (units)" (if present),
     *    2=on the second line.
     * @param tNanString the String to use for missing numeric values (e.g., "NaN" or "").
     *    ERDDAP .csv and .tsv use "NaN" because they did since the beginning and
     *    because it is easier/safer to replace "NaN" with "" than replace nothing with "NaN".
     */
    public TableWriterSeparatedValue(EDD tEdd, String tNewHistory, 
        OutputStreamSource tOutputStreamSource,
        String tSeparator, boolean tQuoted, boolean tWriteColumnNames, 
        char tWriteUnits, String tNanString) {

        super(tEdd, tNewHistory, tOutputStreamSource);
        separator = tSeparator;
        quoted = tQuoted;
        writeColumnNames = tWriteColumnNames;
        writeUnits = tWriteUnits;
        nanString = tNanString;
        if ("0(2".indexOf(writeUnits) < 0)
            throw new SimpleException("Internal error: TableWriterSeparatedValue writeUnits=" + 
                writeUnits + " must be 0, (, or 2.");
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
     * This implementation converts them to NaNs.
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

            //write the column names   
            isString = new boolean[nColumns];
            for (int col = 0; col < nColumns; col++) {
                isString[col] = table.getColumn(col).elementClass() == String.class;
                
                if (writeColumnNames) {
                    String units = "";
                    if (writeUnits == '[' ||
                        writeUnits == '(') {
                        units = table.columnAttributes(col).getString("units");
                        if (units == null) units = "";
                        if (isTimeStamp[col])
                            units = "UTC"; //"seconds since 1970-01-01..." is no longer true
                        if (units.length() > 0) {
                            if (writeUnits == '[') 
                                 units = " [" + units + "]";
                            else units = " (" + units + ")";
                        }
                    }

                    //quoteIfNeeded converts carriageReturns/newlines to (char)166; //'¦'  (#166)
                    writer.write(String2.quoteIfNeeded(quoted, table.getColumnName(col) + units));
                    writer.write(col == nColumns - 1? "\n" : separator);
                }
            }

            //write the units on 2nd line of header  
            if (writeColumnNames && writeUnits == '2') {
                for (int col = 0; col < nColumns; col++) {
                    String units = table.columnAttributes(col).getString("units");
                    if (units == null) units = "";
                    if (isTimeStamp[col])
                        units = "UTC"; //no longer true: "seconds since 1970-01-01..."
                    writer.write(String2.quoteIfNeeded(quoted, units));
                    writer.write(col == nColumns - 1? "\n" : separator);
                }
            }
        }

        //*** do everyTime stuff
        convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is unchanged

        //avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
        int nRows = table.nRows();
        totalNRows += nRows;
        EDStatic.ensureArraySizeOkay(totalNRows, "Separated Value");

        //write the data
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nColumns; col++) {
                if (isTimeStamp[col]) {
                    writer.write(Calendar2.epochSecondsToLimitedIsoStringT(
                        time_precision[col], table.getDoubleData(col, row), ""));
                } else if (isString[col]) {
                    writer.write(String2.quoteIfNeeded(quoted, table.getStringData(col, row)));
                } else {
                    String s = table.getStringData(col, row);
                    writer.write(s.length() == 0? nanString : s); 
                }
                writer.write(col == nColumns -1? "\n" : separator);
            }
        }       

        //ensure it gets to user right away
        if (nRows > 1) //some callers work one row at a time; avoid excessive flushing
            writer.flush(); 

    }

    
    /**
     * This writes any end-of-file info to the stream and flush the stream.
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

        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("TableWriterSeparatedValue done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }


    /**
     * This is a convenience method to write an entire table in one step.
     *
     * @throws Throwable if trouble  (no columns is trouble; no rows is not trouble)
     */
    public static void writeAllAndFinish(EDD tEdd, String tNewHistory, Table table, 
        OutputStreamSource tOutputStreamSource, String tSeparator, boolean tQuoted, 
        boolean tWriteColumnNames, char tWriteUnits, String tNanString) throws Throwable {

        TableWriterSeparatedValue twsv = new TableWriterSeparatedValue(tEdd, 
            tNewHistory, tOutputStreamSource, tSeparator,tQuoted, 
            tWriteColumnNames, tWriteUnits, tNanString);
        twsv.writeAllAndFinish(table);
    }

}



