/* 
 * TableWriterOrderByMax Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PrimitiveArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;

import java.util.BitSet;

/**
 * TableWriterOrderByMax provides a way to sort the response table's rows,
 * and just keep the row where the value of the last sort variable is highest.
 * For example, you could use orderBy(\"stationID,time\") to just get the rows
 * of data with each station's maximum time value.
 *
 * <p>This doesn't do anything to missing values and doesn't asssume they are
 * stored as NaN or fake missing values.
 *
 * <p>Unlike TableWriterAllWithMetadata, this doesn't keep track of min,max for actual_range
 * or update metadata at end. It is assumed that this is like a filter,
 * and that a subsequent TableWriter will handle that if needed.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-13
 */
public class TableWriterOrderByMax extends TableWriterAll {


    //set by constructor
    protected TableWriter otherTableWriter;
    public String orderBy[];

    /**
     * The constructor.
     *
     * @param tDir a private cache directory for storing the intermediate files,
     *    usually EDStatic.fullCacheDirectory + datasetID + "/"
     * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     *     A random number will be added to it for safety.
     * @param tOtherTableWriter the tableWriter that will receive the unique rows
     *   found by this tableWriter.
     * @param tOrderByCsv the names of the columns to sort by (most to least important)
     */
    public TableWriterOrderByMax(String tDir, String tFileNameNoExt, 
            TableWriter tOtherTableWriter, String tOrderByCsv) {

        super(tDir, tFileNameNoExt); 
        otherTableWriter = tOtherTableWriter;
        if (tOrderByCsv == null || tOrderByCsv.trim().length() == 0)
            throw new SimpleException("Query error: " +
                "No column names were specified for 'orderByMax'.");
        orderBy = String2.split(tOrderByCsv, ',');
    }


    /**
     * This adds the current contents of table (a chunk of data) to the OutputStream.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * @param table with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
        if (table.nRows() == 0) 
            return;

        //to save memory, this just does a feeble job (remove non-max rows from this partial table)
        //  and leaves perfect job to finish()
        orderByMax(table);

        //ensure the table's structure is the same as before
        //and write to dataOutputStreams
        super.writeSome(table);
    }

    
    /**
     * This finishes orderByMax and writes results to otherTableWriter
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        super.finish();

        Table cumulativeTable = cumulativeTable();
        releaseResources();
        orderByMax(cumulativeTable);
        otherTableWriter.writeAllAndFinish(cumulativeTable);

        //clean up
        otherTableWriter = null;
    }

    /**
     * If caller has the entire table, use this instead of repeated writeSome() + finish().
     * This overwrites the superclass method.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
        orderByMax(tCumulativeTable);
        otherTableWriter.writeAllAndFinish(tCumulativeTable);
        otherTableWriter = null;
    }


    /**
     * This does orderByMax for this table.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void orderByMax(Table table) throws Throwable {

        //sort based on keys
        int keys[] = new int[orderBy.length];
        boolean ascending[] = new boolean[orderBy.length];
        PrimitiveArray keysPa[] = new PrimitiveArray[orderBy.length];
        for (int ob = 0; ob < orderBy.length; ob++) {
            keys[ob] = table.findColumnNumber(orderBy[ob]);
            ascending[ob] = true;
            if (keys[ob] < 0)
                throw new SimpleException("Query error: " +
                    "'orderByMax' column=" + orderBy[ob] + " isn't in the results table.");
            keysPa[ob] = table.getColumn(keys[ob]);
        }
        table.sort(keys, ascending);

        //just save rows with unique orderBy columns (n-1 keys) with max value in last orderBy column
        int nRows = table.nRows();
        BitSet keep = new BitSet(nRows);
        for (int row = 1; row < nRows; row++) {  //keep previous row if orderBy0..n-2 are 
            boolean keepPreviousRow = false;  
            for (int ob = 0; ob < orderBy.length-1; ob++) {  //don't check last orderBy column
                if (keysPa[ob].compare(row-1, row) != 0) {
                    //if any value is different, we keep the previousRow
                    keepPreviousRow = true;
                    break;
                }
            }
            keep.set(row-1, keepPreviousRow);            
        }
        keep.set(nRows-1, true);
        table.justKeep(keep);
    }

}



