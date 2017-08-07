/* 
 * TableWriterAll Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * TableWriterAll provides a way to write a table to a series of 
 * DataOutputStreams (one per column) 
 * in chunks so that the whole table is available but doesn't have to be in memory 
 * at one time.
 * This is used by EDDTable.
 *
 * <p>This is different from most TableWriters in that finish() doesn't 
 * write the data anywhere (to an outputStream or to another tableWriter), 
 * it just makes all of the data available.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-23
 */
public class TableWriterAll extends TableWriter {

    public static String attributeTo = "gathering data in TableWriterAll";

    //set by constructor
    protected String dir;
    protected String fileNameNoExt;

    //set firstTime
    //POLICY: because this class may be used in more than one thread,
    //each instance makes unique temp files names by adding randomInt to name.
    protected int randomInt = Math2.random(Integer.MAX_VALUE);
    protected DataOutputStream[] columnStreams;
    protected long totalNRows = 0; 

    protected Table cumulativeTable; //set by writeAllAndFinish, if used

    /**
     * The constructor.
     * TableWriterAll will create several temporary files using the dir+name as
     * the starting point.
     * TableWriterAll will delete all of the files when garbage-collected.
     *
     * @param tDir a private cache directory for storing the intermediate files,
     *    usually cacheDirectory(datasetID)
     * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     *     A random number will be added to it for safety.
     */
    public TableWriterAll(EDD tEdd, String tNewHistory, String tDir, String tFileNameNoExt) {
        super(tEdd, tNewHistory, null);
        dir = File2.addSlash(tDir);
        //Normally, this is cacheDirectory and it already exists,
        //  but my testing environment (2+ things running) may have removed it.
        File2.makeDirectory(dir); 
        fileNameNoExt = tFileNameNoExt;
    }


    /**
     * This adds the current contents of table (a chunk of data) to the columnStreams.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., open the columnStreams).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * <p>The table should have missing values stored as destinationMissingValues
     * or destinationFillValues.
     * This implementation doesn't change them.
     *
     * @param table  with destinationValues
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
            columnStreams = new DataOutputStream[nColumns];
            for (int col = 0; col < nColumns; col++) {
                columnStreams[col] = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(
                    dir + fileNameNoExt + "." + randomInt + "." + columnNames[col])));
            }
            if (reallyVerbose) 
                String2.log("TableWriterAll col0 file=" + 
                    dir + fileNameNoExt + "." + randomInt + "." + columnNames[0]);
        }

        //avoid gathering more data than can be processed
        //(although in some cases, perhaps more could be handled)
        long newTotalNRows = totalNRows + table.nRows();
        EDStatic.ensureArraySizeOkay(newTotalNRows, attributeTo);
        EDStatic.ensureMemoryAvailable(newTotalNRows * 8, attributeTo); //to process one PA

        //do everyTime stuff
        //write the data
        for (int col = 0; col < nColumns; col++) 
            table.getColumn(col).writeDos(columnStreams[col]);
        totalNRows = newTotalNRows;
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
        if (columnStreams == null)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");
        //String2.log("TableWriterAll.finish  n columnStreams=" + columnStreams.length);
        for (int col = 0; col < columnStreams.length; col++) {
            //close the stream
            columnStreams[col].close();
            //an attempt to solve File2.delete problem on these files: it couldn't hurt
            columnStreams[col] = null;  
        }
        columnStreams = null;

        //diagnostic
        if (verbose)
            String2.log("TableWriterAll done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }

    /**
     * Call this after finish() to get a PrimitiveArray with all of the data for one of the columns.
     * Since this may be a large object, destroy this immediately when done.
     * Call this after finish() is called as part of getting the results.
     * 
     * <p>Missing values are still represented as destinationMissingValue or
     * destinationFillValue.
     * Use pa.table.convertToStandardMissingValues() if NaNs are needed.
     *
     * @param col   0..
     * @return a PrimitiveArray with all of the data for one of the columns.
     * @throws Throwable if trouble  (e.g., totalNRows > Integer.MAX_VALUE)
     */
    public PrimitiveArray column(int col) throws Throwable {
        //get it from cumulativeTable
        if (cumulativeTable != null)
            return cumulativeTable.getColumn(col);

        //get it from DOSFile
        EDStatic.ensureArraySizeOkay(totalNRows, "TableWriterAll");
        PrimitiveArray pa = PrimitiveArray.factory(columnType(col), 
            (int)totalNRows, false);  //safe since checked above
        DataInputStream dis = dataInputStream(col);
        pa.readDis(dis, (int)totalNRows); //safe since checked above
        dis.close();
        return pa;
    }

    /**
     * This returns an empty PA (of suitable type, with capacity = 1) for a column. 
     *
     * @param col   0..
     */
    public PrimitiveArray columnEmptyPA(int col) {
        return PrimitiveArray.factory(columnType(col), 1, false);  //safe since checked above
    }

    /**
     * This is like the other column(col), but just gets up to the firstNRows. 
     *
     * @param col   0..
     * @param firstNRows puts a limit on the max number of rows this will return
     * @return a PrimitiveArray with the requested data for one of the columns.
     * @throws Throwable if trouble 
     */
    public PrimitiveArray column(int col, int firstNRows) throws Throwable {
        //get it from cumulativeTable
        if (cumulativeTable != null)
            return cumulativeTable.getColumn(col);

        //get it from DOSFile
        EDStatic.ensureArraySizeOkay(totalNRows, "TableWriterAll");
        PrimitiveArray pa = PrimitiveArray.factory(columnType(col), 
            (int)totalNRows, false);  //safe since checked above
        DataInputStream dis = dataInputStream(col);
        pa.readDis(dis, Math.min(firstNRows, Math2.narrowToInt(totalNRows)));
        dis.close();
        return pa;
    }

    /**
     * Call this after finish() to get the data from a DataInputStream 
     * with all of the data for one of the columns.
     * IT IS UP TO THE CALLER TO CLOSE THE DataInputStream.
     * THIS USES ALMOST NO MEMORY.
     * 
     * <p>Missing values are still represented as destinationMissingValue or
     * destinationFillValue.
     * Use pa.table.convertToStandardMissingValues() if NaNs are needed.
     *
     * @param col   0..  the column number in the request (not the dataset)
     * @return a DataInputStream ready to have the first element read
     * @throws Throwable if trouble  (e.g., totalNRows > Integer.MAX_VALUE)
     */
    public DataInputStream dataInputStream(int col) throws Throwable {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(
            dir + fileNameNoExt + "." + randomInt + "." + columnNames[col])));
        return dis;
    }

    /**
     * This returns the total number of rows.
     * Call this after finish() is called as part of getting the results.
     *
     * @return the total number of rows.
     */
    public long nRows() {return totalNRows;}

    /** 
     * Call this after finish() to assemble the cumulative table.
     * SINCE ENTIRE TABLE IS IN MEMORY, THIS MAY TAKE TONS OF MEMORY.
     * This checks ensureMemoryAvailable.
     * THE CUMULATIVETABLE IS HELD IN MEMORY BY THIS TABLEWRITERALL AFTER THIS METHOD RETURNS!
     *
     * <p>For the TableWriterAllWithMetadata, this has the updated metadata.
     */
    public Table cumulativeTable() throws Throwable {
        //is it available from writeAllAndFinish
        if (cumulativeTable != null)
            return cumulativeTable;

        //make cumulativeTable
        Table table = makeEmptyTable();

        //ensure memory available    too bad this is after all data is gathered
        int nColumns = nColumns();
        EDStatic.ensureMemoryAvailable(nColumns * nRows() * table.estimatedBytesPerRow(), //nRows() is a long
            "TableWriterAll.cumulativeTable");

        //actually get the data
        for (int col = 0; col < nColumns; col++) 
            table.setColumn(col, column(col));
        return table;
    }

    /**
     * This deletes the columnStreams files and cumulativeTable (if any).
     * This won't throw an exception.
     *
     * <p>It isn't essential that the user call this.
     * It will be called automatically then java garbage collector calls finalize.
     * And/or the cache cleaning system will do this in ~1 hour if the caller doesn't.

     */
    public void releaseResources() {
        try {
            cumulativeTable = null;

            if (columnNames == null)
                return;
            int nColumns = nColumns();
            for (int col = 0; col < nColumns; col++) {
                //deletion isn't essential or urgent.  
                //We don't want to tie up the garbage collector thread.
                File2.simpleDelete(dir + fileNameNoExt + "." + randomInt + "." + columnNames[col]);
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
    }
    
    /** 
     * Users of this class shouldn't call this -- use releaseResources() instead.
     * Java calls this when an object is no longer used, just before garbage collection. 
     * 
     */
    protected void finalize() throws Throwable {
        releaseResources();
        super.finalize();
    }


}



