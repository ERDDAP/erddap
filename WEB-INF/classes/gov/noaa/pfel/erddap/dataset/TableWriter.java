/* 
 * TableWriter Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * TableWriter provides a way to write a table to an outputStream
 * in chunks so that the whole table doesn't have to be in memory
 * at one time.
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-23
 */
public abstract class TableWriter {

    /** "ERROR" is defined here (from String2.ERROR) so that it is consistent in log files. */
    public final static String ERROR = String2.ERROR; 

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 



    //these are set by the constructor
    protected long time;
    protected OutputStreamSource outputStreamSource;

    //these are set the first time ensureCompatible is called
    protected String[] columnNames;
    protected Class[] columnTypes;
    protected Attributes[] columnAttributes;
    protected Attributes globalAttributes;


    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     */
    public TableWriter(OutputStreamSource tOutputStreamSource) {
        time = System.currentTimeMillis();
        outputStreamSource = tOutputStreamSource;
    }

    /**
     * This makes sure that the number of columns, the column names, 
     * and the types of columns are the same as before (or that this 
     * is the first time this is called).
     * 
     * @param table the table with a chunk of data
     * @throws Throwable if not compatible or other trouble
     */
    protected void ensureCompatible(Table table) throws Throwable {
        String[] tColumnNames = table.getColumnNames();
        int nColumns = tColumnNames.length;
        Class[] tColumnTypes = new Class[nColumns];
        for (int c = 0; c < nColumns; c++)
            tColumnTypes[c] = table.getColumn(c).elementClass();

        //first time this is called?
        if (columnNames == null) {
            columnNames = tColumnNames;
            columnTypes = tColumnTypes;
            columnAttributes = new Attributes[nColumns];
            for (int col = 0; col < nColumns; col++) {
                //no need to make copies (clones) off atts since standardizeResultsTable has made copies for the table
                columnAttributes[col] = table.columnAttributes(col); 
                //String2.log("\nTableWriter attributes " + columnNames[col] + "\n" + columnAttributes[col]);
            }
            globalAttributes = table.globalAttributes();
            return;
        }

        //ensure columnNames are same
        if (columnNames.length != tColumnNames.length)
            throw new RuntimeException("Internal error in TableWriter: newNColumns=" + 
                tColumnNames.length + " != oldNColumns=" + columnNames.length + ".");
        for (int c = 0; c < nColumns; c++) {
            if (!columnNames[c].equals(tColumnNames[c]))
                throw new RuntimeException("Internal error in TableWriter: for column=" + c +
                      ", newName=" + tColumnNames[c] + 
                    " != oldName=" + columnNames[c] + ".");
            if (!columnTypes[c].equals(tColumnTypes[c]))
                throw new RuntimeException("Internal error in TableWriter: for column=" + c +
                      ", newType=" + PrimitiveArray.elementClassToString(tColumnTypes[c]) + 
                    " != oldType=" + PrimitiveArray.elementClassToString(columnTypes[c]) + ".");
        }

    }

    /**
     * This adds the current contents of table (a chunk of data) to the outputSteam.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * The number of columns, the column names, and the types of columns 
     * must be the same each time this is called.
     *
     * <p>The table should have missing values stored as destinationMissingValues
     *  or destinationFillValues.
     *
     * @param table 
     * @throws Throwable if trouble
     */
    public abstract void writeSome(Table table) throws Throwable;


    /**
     * This converts the missing values to standard (e.g., NaN) missing values,
     * but doesn't erase the missing_value and _FillValue metadata so the table can be reused.
     * This is a convenience for subclasses' writeSome() and writeAllAndFinish().
     *
     * @param table a partial data table
     */
    protected void convertToStandardMissingValues(Table table) {
        int nColumns = table.nColumns();
        for (int col = 0; col < nColumns; col++) {
            Attributes colAtt = table.columnAttributes(col);
            table.getColumn(col).convertToStandardMissingValues( 
                colAtt.getDouble("_FillValue"),
                colAtt.getDouble("missing_value"));
         }
     }


    /**
     * This writes any end-of-file info to the stream and flushes the stream.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public abstract void finish() throws Throwable;

    /**
     * If caller has the entire table, use this instead of repeated writeSome() + finish().
     * Some subclasses may be able to do things more efficiently if the
     * entire table is available.
     * This default implementation just calls writeSome() and finish().
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void writeAllAndFinish(Table table) throws Throwable {
        writeSome(table);
        finish();
    }

    /**
     * This returns the number of columns.
     * Call this after finish() is called as part of getting the results.
     *
     * @return the number of columns.
     */
    public int nColumns() {return columnNames.length;}

    /**
     * This returns the internal list of columnNames, so don't change it.
     *
     * @return the column names.
     */
    public String[] columnNames() {return columnNames;}

    /**
     * This returns one of the destination column's names.
     * Call this after finish() is called as part of getting the results.
     *
     * @param col   0..
     * @return one of the destination column's names.
     */
    public String columnName(int col) {return columnNames[col];}

    /**
     * This returns one of the destination column's types.
     * Call this after finish() is called as part of getting the results.
     *
     * @param col   0..
     * @return one of the destination column's types.
     */
    public Class columnType(int col) {return columnTypes[col];}

    /**
     * This returns one of the destination column's columnAttributes.
     * Note that actual_range hasn't been modified for this data subset.
     *
     * @param col   0..
     * @return one of the destination column's Attributes.
     */
    public Attributes columnAttributes(int col) {return columnAttributes[col];}

    /**
     * This returns the destination global Attributes.
     * Note that the values (e.g., Northernmost_Northing) haven't been modified for this data subset.
     *
     * @return the global Attributes.
     */
    public Attributes globalAttributes() {return globalAttributes;}

    /** 
     * Given the information available, this makes an empty table 
     * with the appropriate columns and metadata.
     */
    public Table makeEmptyTable() {
        if (columnNames == null) 
            throw new SimpleException(EDStatic.THERE_IS_NO_DATA);

        int nColumns = columnNames.length;
        Table table = new Table();
        table.globalAttributes().set(globalAttributes);
        for (int col = 0; col < nColumns; col++) 
            table.addColumn(col, columnNames[col], 
                PrimitiveArray.factory(columnTypes[col], 1024, false), 
                columnAttributes[col]);
        return table;
    }

}



