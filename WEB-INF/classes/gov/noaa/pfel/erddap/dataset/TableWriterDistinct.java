/* 
 * TableWriterDistinct Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.IntArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashSet;

/**
 * TableWriterDistinct provides a way to gather just the unique rows,
 * sort them, then write to some other TableWriter.
 * This functions like SQL's DISTINCT.
 *
 * <p>This doesn't do anything to missing values and doesn't asssume they are
 * stored as NaN or fake missing values.
 *
 * <p>Unlike TableWriterAll, this doesn't keep track of min,max for actual_range
 * or update metadata at end. It is assumed that this is like a filter,
 * and that a subsequent TableWriter will handle that if needed.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-08
 */
public class TableWriterDistinct extends TableWriterAll {


    //set by constructor
    protected TableWriter otherTableWriter;

    /**
     * The constructor.
     *
     * @param tDir a private cache directory for storing the intermediate files,
     *    usually cacheDirectory(datasetID)
     * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     *     A random number will be added to it for safety.
     * @param tOtherTableWriter the tableWriter that will receive the unique rows
     *   found by this tableWriter.
     */
    public TableWriterDistinct(EDD tEdd, String tNewHistory, String tDir, String tFileNameNoExt, 
        TableWriter tOtherTableWriter) {

        super(tEdd, tNewHistory, tDir, tFileNameNoExt); 
        otherTableWriter = tOtherTableWriter;
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

        //to save memory, this just does a feeble job (remove duplicates from this partial table)
        //  and leaves perfect job to finish()
        sortAndRemoveDuplicates(table);

        //ensure the table's structure is the same as before
        //and write to dataOutputStreams
        super.writeSome(table);
    }

    
    /**
     * This reconstructs the table, sorts and removes duplicates, and sends table to otherTableWriter.
     * If ignoreFinish=true, nothing will be done.
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        if (ignoreFinish)
            return;

        //close the dataOutputStreams
        super.finish();  //this throws Throwable if no data

        //makeCumulativeTable
        Table table = cumulativeTable();   
        releaseResources();

        //sorts and remove duplicates, and sends table to otherTableWriter.
        lowFinish(table);
    }

    /**
     * If caller has the entire table, use this instead of repeated writeSome() + finish().
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
        if (ignoreFinish) {
            writeSome(tCumulativeTable);
            tCumulativeTable.removeAllRows();
            return;
        }

        lowFinish(tCumulativeTable);
    }

    
    /** Given a cumulativeTable, this sorts it and removes duplicate rows. */
    private void lowFinish(Table cumulativeTable) throws Throwable {

        //check for MustBe.THERE_IS_NO_DATA
        if (cumulativeTable.nRows() == 0) 
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

        //sortAndRemoveDuplicates
        sortAndRemoveDuplicates(cumulativeTable);

        //write results to otherTableWriter
        otherTableWriter.writeAllAndFinish(cumulativeTable);

        //clean up
        otherTableWriter = null;
    }

    private void sortAndRemoveDuplicates(Table table) {
        //sort
        table.leftToRightSortIgnoreCase(table.nColumns()); 

        //removeDuplicates
        table.removeDuplicates();
    }
    

}



