/* 
 * TableWriterOrderBy Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;


/**
 * TableWriterDistinct provides a way to gather all rows,
 * sort them, then write to some other TableWriter.
 * This functions like SQL's ORDER BY.
 *
 * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
 *
 * <p>This doesn't do anything to missing values and doesn't asssume they are
 * stored as NaN or fake missing values.
 *
 * <p>Unlike TableWriterAllWithMetadata, this doesn't keep track of min,max for actual_range
 * or update metadata at end. It is assumed that this is like a filter,
 * and that a subsequent TableWriter will handle that if needed.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-10
 */
public class TableWriterOrderBy extends TableWriterAll {

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
    public TableWriterOrderBy(String tDir, String tFileNameNoExt, 
          TableWriter tOtherTableWriter, String tOrderByCsv) {

        super(tDir, tFileNameNoExt); 
        otherTableWriter = tOtherTableWriter;
        if (tOrderByCsv == null || tOrderByCsv.trim().length() == 0)
            throw new SimpleException(EDStatic.queryError +
               "No column names were specified for 'orderBy'.");
        orderBy = String2.split(tOrderByCsv, ',');
    }



    /**
     * This sorts cumulativeTable, then writes it to otherTableWriter
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        super.finish();

        Table cumulativeTable = cumulativeTable();
        releaseResources();
        sort(cumulativeTable);
        otherTableWriter.writeAllAndFinish(cumulativeTable);

        //clean up
        otherTableWriter = null;
    }

    /**
     * If caller has the entire table, use this instead of repeated writeSome() + finish().
     * This overwrites the superclass method.
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
        sort(tCumulativeTable);
        otherTableWriter.writeAllAndFinish(tCumulativeTable);
        otherTableWriter = null;
    }


    private void sort(Table table) {
        //ensure orderBy columns are present in results table
        int[] keys = new int[orderBy.length];
        boolean[] ascending = new boolean[orderBy.length];
        for (int ob = 0; ob < orderBy.length; ob++) {
            keys[ob] = table.findColumnNumber(orderBy[ob]);
            ascending[ob] = true;
            if (keys[ob] < 0)
                throw new SimpleException(EDStatic.queryError +
                    "'orderBy' column=" + orderBy[ob] + " isn't in the results table.");
        }
        table.sort(keys, ascending);
    }


}



