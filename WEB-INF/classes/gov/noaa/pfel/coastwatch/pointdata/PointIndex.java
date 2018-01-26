/* 
 * PointIndex Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Math2;
import com.cohort.util.File2;
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
 * @author Bob Simons (bob.simons@noaa.gov) 2006-01-12
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
    private Class index0ElementType;

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
        dos.close();
        index0ElementType = index0PA.elementClass();

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
        RandomAccessFile raf = new RandomAccessFile(indexFileName, "r");
        int firstIndex0 = (int)PrimitiveArray.rafFirstGAE(raf, index0ElementType, //safe since reading an int
            0,  //byte in file that values start at 
            0, lastFiniteSortedIndex0, desiredMin[0], 5); //precision=5
        if (firstIndex0 < 0)
            return rowOk;
        int lastIndex0 = (int)PrimitiveArray.rafLastLAE(raf, index0ElementType, //safe since reading an int
            0,  //byte in file that values start at 
            firstIndex0, lastFiniteSortedIndex0, desiredMax[0], 5); //precision=5
        raf.close();
        boolean fewTimesOk = (lastIndex0 - firstIndex0) < nDataRows / 2;
        String2.log("raf lookup firstIndex0=" + firstIndex0 +
            " lastIndex0=" + lastIndex0 + " fewTimesOk=" + fewTimesOk + 
            " time=" + (System.currentTimeMillis() - tTime) + "ms");

        //2 ways to set rowOk; to minimize lookups in indexFile 
        DataInputStream dis = new DataInputStream(new BufferedInputStream(
            new FileInputStream(indexFileName)));
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
        dis.close();

        String2.log("PointIndex.subset time=" + (System.currentTimeMillis() - time) + "ms");
            //+ " cumReadTime=" + cumulativeReadTime);
        return rowOk;
    }

    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {
        PointIndex.verbose = true;

        //testLittleMethods
        

        //setup for tests of one trajectory
        DoubleArray t = new DoubleArray(new double[]{1e8,   2e8, 3e8, 4e8, 5e8, 6e8});
        FloatArray  x = new FloatArray( new float[]{ -180, -120,   0, 120, 120, 180});
        FloatArray  y = new FloatArray( new float[]{  -20,  -45,   0,  45,  45,  90});
        FloatArray  z = new FloatArray( new float[]{    0,    0,   0,   0,   5,   0});
        String dir = File2.getSystemTempDirectory();
        String name = "PointIndexTest1";
        PointIndex index = new PointIndex(dir + name + ".index", "T", t);
        index.addIndex("X", x);
        index.addIndex("Y", y);
        index.addIndex("Z", z);

        //get all
        BitSet results = index.subset(
            new double[]{1e8, -180, -90, 0},
            new double[]{6e8,  180,  90, 5});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", "");

        //2 x's
        results = index.subset(
            new double[]{-1,   120, -90, 0},
            new double[]{1e10, 120,  90, 5});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", ""); //accepts entire station; should be "3, 4", "");

        //1 y
        results = index.subset(
            new double[]{  0, -180, -50, 0},
            new double[]{1e10, 180, -40, 5});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", ""); //accepts entire station; should be "1", "");

        //1 z
        results = index.subset(
            new double[]{  0, -180, -90, 4},
            new double[]{1e10, 180,  90, 7});
        Test.ensureEqual(String2.toString(results), "0, 1, 2, 3, 4, 5", ""); //accepts entire station; should be "4", "");

        //1 t
        results = index.subset(
            new double[]{  0, -180, -90, 0},
            new double[]{1.5e8,180,  90, 5});
        Test.ensureEqual(String2.toString(results), "0", "");

        //nothing (time)
        results = index.subset(
            new double[]{0, -180, -90, 0},
            new double[]{1,  180,  90, 5});
        Test.ensureEqual(String2.toString(results), "", "");

        index.close();


        //***test no variation
        //setup for tests
        t = new DoubleArray(new double[]{4,4});
        x = new FloatArray( new float[]{ 1,1});
        y = new FloatArray( new float[]{ 2,2});
        z = new FloatArray( new float[]{ 3,3});
        name = "PointIndexTest2";
        index = new PointIndex(dir + name + ".index", "T", t);
        index.addIndex("X", x);
        index.addIndex("Y", y);
        index.addIndex("Z", z);

        //get all
        results = index.subset(
            new double[]{ 0,  0,  0,  0},
            new double[]{ 5,  5,  5,  5});
        Test.ensureEqual(String2.toString(results), "0, 1", "");

        //reject based on t, x, y, z
        results = index.subset(
            new double[]{ 5,  0,  0,  0},
            new double[]{10,  5,  5,  5});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 0, -1,  0,  0},
            new double[]{ 5,  0,  5,  5});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 0,  0,  5,  0},
            new double[]{ 5,  5, 10,  5});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 0,  0,  0, -1},
            new double[]{ 5,  5,  5,  0});
        Test.ensureEqual(String2.toString(results), "", "");

        index.close();

        //*** test lots of data
        results = null;
        index = null;
        Math2.incgc(500); //in a test
        Math2.incgc(500); //in a test
        long memoryInUse = Math2.getMemoryInUse();
        int nGroups = 100;
        int nPerGroup = 10000;
        int n = nGroups * nPerGroup;
        String2.log("create PointIndex " + n + ";  Time should be < 2 s for main index"); 
        name = "PointIndexTest3";
        IntArray x2 = new IntArray(n, false);
        IntArray y2 = new IntArray(n, false);
        IntArray z2 = new IntArray(n, false);
        IntArray t2 = new IntArray(n, false);
        for (int i = 0; i < nGroups; i++) {       //1000x 0..999
            int i2 = i*2;
            int i4 = i*4;
            for (int j = 0; j < nPerGroup; j++) {
                t2.add(j);
                x2.add(i);
                y2.add(i2);
                z2.add(i4);
            }
        }
        String2.log("addIndex 1,000,000."); 
        index = new PointIndex(dir + name + ".index", "T", t2);
        //t is                   //0,1,2, ... 0,1,2, ... 0,1,2
        index.addIndex("X", x2); //0,0,0, ... 1,1,1, ... 2,2,2
        index.addIndex("Y", y2); //0,0,0, ... 2,2,2, ... 4,4,4
        index.addIndex("Z", z2); //0,0,0, ... 4,4,4, ... 8,8,8

        //find some
        String2.log("subset times should be <20 ms"); 
        results = index.subset(
            new double[]{ 1, 1,  2,  4},
            new double[]{ 1, 1,  2,  4});
        Test.ensureEqual(String2.toString(results), "10001", "1 time, 1 station");  //row#
        results = index.subset(
            new double[]{ 50, 0, 0, 0},
            new double[]{ 50, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE});
        Test.ensureEqual(results.cardinality(), nGroups, "one time, all stations");  //row#
        int next = -1;
        Test.ensureEqual(next = results.nextSetBit(next + 1),    50, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 10050, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20050, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 30050, ""); 
        results = index.subset(
            new double[]{ 0, 2, 4, 8},
            new double[]{ Integer.MAX_VALUE, 2, 4, 8});
        Test.ensureEqual(results.cardinality(), nPerGroup, "all times, one station");  //row#
        next = -1;
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20000, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20001, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20002, ""); 
        Test.ensureEqual(next = results.nextSetBit(next + 1), 20003, ""); 
        Test.ensureEqual(results.get(19999), false, ""); 
        Test.ensureEqual(results.get(30000), false, ""); 

        //reject based on x, y, z, t
        results = index.subset(
            new double[]{ 1.1,  2,  4,  8},
            new double[]{ 1.1,  2,  4,  8});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 1,  2.1,  4,  8},
            new double[]{ 1,  2.1,  4,  8});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 1,  2,  4.1,  8},
            new double[]{ 1,  2,  4.1,  8});
        Test.ensureEqual(String2.toString(results), "", "");
        results = index.subset(
            new double[]{ 1,  2,  4,  8.1},
            new double[]{ 1,  2,  4,  8.1});
        Test.ensureEqual(String2.toString(results), "", "");

        index.close();

        //done
        x2=null;
        y2=null;
        z2=null;
        t2=null;
        results = null;
        index = null;
        Math2.incgc(500); //in a test
        Math2.incgc(500); //in a test
        memoryInUse = Math2.getMemoryInUse() - memoryInUse;
        String2.log("\n***** PointIndex.main finished successfully; memory not freed = " + memoryInUse);

    }


}
