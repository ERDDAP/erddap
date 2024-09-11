/* 
 * PointIndex Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Math2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.BitSet;

/** THIS CLASS WORKS BUT IS NOT ACTIVELY BEING USED, see PointDataSetStationVariables instead.
 * This class manages station and trajectory file information so that
 * subsets can be created quickly.
 * This is designed to work with datasets that are sorted by groups first
 * (e.g., station or trajectory; there doesn't have to be a column with that id), 
 * then by the longest axis (e.g., time),
 * with other associated indices (e.g., x, y, z), 
 * which may be constant within a group (e.g., a station)
 * or which may vary within a group (e.g., a trajectory).
 *
 * <p>This class takes advantage of the long index (e.g., time) within
 * each group, to quickly access desired data.
 * For stations, x,y,z can also help to quickly accepted or reject 
 * a station at a time in the 'subset' method.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-01-12
 */
public class PointIndex  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /* groups one row for each group and has columns for 
     *   "First Row", "N Finite", 
     *   then, for each index: indexName+" Min", indexName+" Max"
     */
    private Table groups = new Table();
    private IntArray firstRowIA = new IntArray();
    private IntArray nFiniteIA = new IntArray();
    private DoubleArray minDA = new DoubleArray();
    private DoubleArray maxDA = new DoubleArray();
    private String indexFileName;
    private int nDataRows;
    private int nGroups;
    private int rowsStartAt;
    private int lastFiniteSortedIndex0;
    private PAType index0ElementType;

    /**
     * A constructor which adds the first and most important index to the
     * class (usually time). The datasets needs to be sorted first by group
     * (e.g., station or trajectory, which this constructor will detect) 
     * and then by the specified index, e.g., time).
     * 
     * <p>NaN values sort high and so are treated as the end values in a group.
     * This constructor can be quite slow for large amounts of data
     * (e.g., 20 seconds for 10,000,000 rows).
     *
     * <p>Internally, this identifies groups by going through the index0PA values 
     * looking for decrease in value (indicating the start of the next group).
     * <br>It populates the first 4 columns of groups table: 
     *    firstRow, nFinite, minIndex0, maxIndex0
     *   (with a row for each group).
     * <br>It sorts all the index0PA values and stores them in indexFileName
     *   along with each value's original row number.
     * <br>Later, the subset method can identify all the rows which match
     *   specified ranges for each of the indices.
     *
     * @param indexFileName the full name of the file to be created to store the index values
     * @param indexName the name of the index (e.g., "time")
     * @param index0PA  always numeric (usually time), already sorted by group (first), then time (second).
     * @throws Exception if trouble
     */
    public PointIndex(String indexFileName, String indexName, PrimitiveArray index0PA) throws Exception {
        long time = System.currentTimeMillis();
        if (verbose) String2.log("create PointIndex for " + indexFileName + "\n  make groups table"); 

        //make the groups table
        groups.addColumn("First Row", firstRowIA);
        groups.addColumn("N Finite", nFiniteIA);
        groups.addColumn(indexName + " Min", minDA);
        groups.addColumn(indexName + " Max", maxDA);
        nDataRows = index0PA.size();
        int firstRow = 0;
        int nFinite = 0;
        double lastD, d = index0PA.getDouble(0);
        boolean lastDFinite, dFinite = Double.isFinite(d);
        if (dFinite) nFinite++;
        double min = d;
        double max = min;

        //go through the data looking for a decrease in values  
        //(i.e., the transition from one group to the next)
        for (int row = 1; row < nDataRows; row++) {
            lastD = d;
            lastDFinite = dFinite;
            d = index0PA.getDouble(row);
            dFinite = Double.isFinite(d);
            if (!dFinite) {
                //go to next row
            } else if (!lastDFinite || d < lastD) {
                //start of new group   (always finite)
                //store info for last group
                //first group is only group that may have nFinite = 0
                firstRowIA.add(firstRow);
                nFiniteIA.add(nFinite);
                minDA.add(min);
                maxDA.add(max);

                //get ready for next group
                firstRow = row;
                nFinite = 1; 
                min = d;
                max = min;
            } else {
                nFinite++;
                min = Math.min(d, min);
                max = Math.max(d, max);
            }
        }
        //store last group's info
        firstRowIA.add(firstRow);
        nFiniteIA.add(nFinite);
        minDA.add(min);
        maxDA.add(max);

        nGroups = groups.nRows();

        //make a table so the ranks can be determined
        //rank, don't sort, so index0PA unchanged
        if (verbose) String2.log("  rank index0"); 
        Table index0Table = new Table();
        index0Table.addColumn("index0", index0PA);
        int[] rank = index0Table.rank(new int[]{0}, new boolean[]{true});

        //save index0Table in index file
        if (verbose) String2.log("  save sorted index0 in index file"); 
        long tTime = System.currentTimeMillis();
        this.indexFileName = indexFileName;
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(indexFileName)));
        try {
            //write the sorted index0 values to the file
            rowsStartAt = 0;
            for (int row = 0; row < nDataRows; row++) {
                rowsStartAt += index0PA.writeDos(dos, rank[row]);
                if (!Double.isNaN(index0PA.getDouble(rank[row])))
                    lastFiniteSortedIndex0 = row;
            }
            //write the related row# in index file
            if (verbose) String2.log("  save related row# in index file"); 
            for (int row = 0; row < nDataRows; row++) 
                dos.writeInt(rank[row]);
        } finally {
            dos.close();
        }
        index0ElementType = index0PA.elementType();

        //10,000,000 takes 6.7s to write, 22.6s total
        String2.log("PointIndex nGroups=" + nGroups + " dosWriteTime=" + (System.currentTimeMillis() - tTime) +
            "ms time=" + (System.currentTimeMillis() - time) + "ms");

    }

    /**
     * This deletes the index file created by the constructor.
     *
     */
    public void close() { 
        File2.delete(indexFileName);
    }

    /**
     * This adds another index to the system (usually, x, then y, then z).
     * The indexes are done separately to conserve memory usage.
     * This method is reasonably fast for large amounts of data
     * (e.g., 0.6 seconds for 10,000,000 rows).
     *
     * @param name the columnName for the index.
     * @param pa  The PrimitiveArray with the (usually) x,y,z data (it won't be changed).
     * @throws Exception if trouble
     */
    public void addIndex(String name, PrimitiveArray pa) throws Exception {

        String errorInMethod = String2.ERROR + " in Index.addIndex:\n";
        Test.ensureEqual(pa.size(), nDataRows,
            errorInMethod + "pa.size != nDataRows for axis '" + name + "'.");
        long time = System.currentTimeMillis();

        //add indexMin and indexMax columns to groups table
        DoubleArray minArray = new DoubleArray();
        DoubleArray maxArray = new DoubleArray();
        groups.addColumn("Min " + name, minArray);
        groups.addColumn("Max " + name, maxArray);

        //find min and max of each group
        for (int group = 0; group < nGroups; group++) {
            int firstRow = firstRowIA.array[group]; 
            int lastRow =  firstRow + nFiniteIA.array[group]; //exclusive
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int row = firstRow; row < lastRow; row++) {
                double d = pa.getDouble(row);
                if (Double.isFinite(d)) {
                    min = Math.min(min, d);
                    max = Math.max(max, d);
                }
            }
            minArray.add(min);
            maxArray.add(max);
        }
        String2.log("addIndex time=" + (System.currentTimeMillis() - time) + "ms");
    }


    /**
     * This returns a BitSet with true for the row numbers which have 
     * data in the specified range.
     * For station data, the bitSet is exactly correct.
     * For trajectory data, all of the trajectory points for trajectories that
     * are at least partly in the xy range and perfectly in the time range 
     * will be returned.
     *
     * @param desiredMin The minimum acceptable values for the indices. 
     *     desiredMin.length must equal nIndices.
     * @param desiredMax The maximum acceptable values for the indices. 
     *     desiredMax.length must equal nIndices.
     * @return an okRows BitSet
     * @throws Exception if trouble
     */
    public BitSet subset(double desiredMin[], double desiredMax[]) throws Exception {

        long time = System.currentTimeMillis();
        if (verbose) String2.log("PointIndex.subset " + 
            " desiredMin=" + String2.toCSSVString(desiredMin) + 
            " desiredMax=" + String2.toCSSVString(desiredMax)); 

        //set up the bitSet
        BitSet rowOk = new BitSet(nDataRows); //initially all false
        int nIndices = (groups.nColumns() - 2) / 2;

        //go through the groups -- set groupOk[group] based on mins/maxs of indices of group
        BitSet groupOk = new BitSet(nGroups);
        groupOk.set(0, nGroups); //all groups okay
        for (int group = 0; group < nGroups; group++) {
            int groupMinColumn = 0;
            for (int index = 0; index < nIndices; index++) {
                groupMinColumn += 2;
                if (desiredMax[index] < groups.getDoubleData(groupMinColumn,     group) ||
                    desiredMin[index] > groups.getDoubleData(groupMinColumn + 1, group)) {
                    groupOk.clear(group); //reject this group
                    break;
                }
            }
        }
        //no ok groups? return all false
        if (groupOk.cardinality() == 0) 
            return rowOk;

        //search sorted index0 for first and last rows in range
        long tTime = System.currentTimeMillis();
        int firstIndex0, lastIndex0;
        RandomAccessFile raf = new RandomAccessFile(indexFileName, "r");
        try {
            firstIndex0 = (int)PrimitiveArray.rafFirstGAE(raf, index0ElementType, //safe since reading an int
                0,  //byte in file that values start at 
                0, lastFiniteSortedIndex0, PAOne.fromDouble(desiredMin[0]), 5); //precision=5
            if (firstIndex0 < 0)
                return rowOk;
            lastIndex0 = (int)PrimitiveArray.rafLastLAE(raf, index0ElementType, //safe since reading an int
                0,  //byte in file that values start at 
                firstIndex0, lastFiniteSortedIndex0, PAOne.fromDouble(desiredMax[0]), 5); //precision=5
        } finally {
            raf.close();
        }
        boolean fewTimesOk = (lastIndex0 - firstIndex0) < nDataRows / 2;
        String2.log("raf lookup firstIndex0=" + firstIndex0 +
            " lastIndex0=" + lastIndex0 + " fewTimesOk=" + fewTimesOk + 
            " time=" + (System.currentTimeMillis() - tTime) + "ms");

        //2 ways to set rowOk; to minimize lookups in indexFile 
        DataInputStream dis = new DataInputStream(
            File2.getDecompressedBufferedInputStream(indexFileName));
        try {
            if (fewTimesOk) {
                //few times are ok 
                //start with rowOk all false
                //set rows with true times to true
                int skip = rowsStartAt + firstIndex0 * 4;
                while (skip > 0) skip -= dis.skip(skip);
                for (int i = firstIndex0; i <= lastIndex0; i++) 
                    rowOk.set(dis.readInt());

                //set rows of false groups to false
                int group = groupOk.nextClearBit(0);
                while (group >= 0 && group < nGroups) {
                    int groupFirstRow = firstRowIA.array[group]; 
                    int groupLastRow  = groupFirstRow + nFiniteIA.array[group]; //exclusive
                    rowOk.clear(groupFirstRow, groupLastRow);
                    group = groupOk.nextClearBit(group + 1);
                }
            } else {
                //lots of times are ok 
                //start with rowOk all false
                //set rows of true groups to true
                int group = groupOk.nextSetBit(0);
                while (group >= 0) {
                    int groupFirstRow = firstRowIA.array[group]; 
                    int groupLastRow  = groupFirstRow + nFiniteIA.array[group]; //exclusive
                    rowOk.set(groupFirstRow, groupLastRow);
                    group = groupOk.nextSetBit(group + 1);
                }

                //set rows with false times to false
                //rows before firstIndex0 are false
                int skip = rowsStartAt;
                while (skip > 0) skip -= dis.skip(skip);
                for (int i = 0; i < firstIndex0; i++) 
                    rowOk.clear(dis.readInt());
                //rows after lastIndex0 are false
                skip = (lastIndex0 - firstIndex0 + 1) * 4;
                while (skip > 0) skip -= dis.skip(skip);
                for (int i = lastIndex0 + 1; i < nDataRows; i++) 
                    rowOk.set(dis.readInt());
            }
        } finally {
            dis.close();
        }

        String2.log("PointIndex.subset time=" + (System.currentTimeMillis() - time) + "ms");
            //+ " cumReadTime=" + cumulativeReadTime);
        return rowOk;
    }
}
