/* 
 * TableWriterOrderByMinMax Copyright 2012, NOAA.
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
 * TableWriterOrderByMinMax provides a way to sort the response table's rows,
 * and just keep the row where the value of the last sort variable is highest.
 * For example, you could use orderBy(\"stationID,time\") to just get the rows
 * of data with each station's minimum and maximum time value (min on one row 
 * and max on the next).
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
public class TableWriterOrderByMinMax extends TableWriterAll {


    //set by constructor
    protected TableWriter otherTableWriter;
    public String orderBy[];

    /**
     * The constructor.
     *
     * @param tDir a private cache directory for storing the intermediate files,
     *    usually cacheDirectory(datasetID)
     * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     *     A random number will be added to it for safety.
     * @param tOtherTableWriter the tableWriter that will receive the unique rows
     *   found by this tableWriter.
     * @param tOrderByCsv the names of the columns to sort by (most to least important)
     */
    public TableWriterOrderByMinMax(EDD tEdd, String tNewHistory, String tDir, 
        String tFileNameNoExt, TableWriter tOtherTableWriter, String tOrderByCsv) {

        super(tEdd, tNewHistory, tDir, tFileNameNoExt); 
        otherTableWriter = tOtherTableWriter;
        String err = EDStatic.queryError + 
            "No column names were specified for 'orderByMinMax'.";
        if (tOrderByCsv == null || tOrderByCsv.trim().length() == 0)
            throw new SimpleException(err);
        orderBy = String2.split(tOrderByCsv, ',');
        if (orderBy.length == 0)
            throw new SimpleException(err);
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

        //to save time and disk space, this just does a partial job 
        //  (remove non-min/max rows from this partial table)
        //  and leaves perfect job to finish()
        table.orderByMinMax(orderBy);

        //ensure the table's structure is the same as before
        //and write to dataOutputStreams
        super.writeSome(table);
    }

    
    /**
     * This finishes orderByMinMax and writes results to otherTableWriter
     * If ignoreFinish=true, nothing will be done.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        if (ignoreFinish) 
            return;

        super.finish();

        Table cumulativeTable = cumulativeTable();
        releaseResources();
        cumulativeTable.orderByMinMax(orderBy);
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
        if (ignoreFinish) {
            writeSome(tCumulativeTable);
            tCumulativeTable.removeAllRows();
            return;
        }
        tCumulativeTable.orderByMinMax(orderBy);
        otherTableWriter.writeAllAndFinish(tCumulativeTable);
        otherTableWriter = null;
    }



}



