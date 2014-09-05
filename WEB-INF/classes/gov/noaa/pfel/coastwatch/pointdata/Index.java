/* 
 * Index Copyright 2005, NOAA.
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
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * This class manages columns of numeric data so that EXACT subsets can be made quickly
 * (including the row numbers that they come from).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-01-12
 */
public class Index  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    private static int bufferSize = 8192;

    private String baseIndexName; 
    private int nIndices = 0;
    private int nRows;
    private int lastFiniteRow;
    private StringArray names = new StringArray();
    private StringArray compactNames = new StringArray();
    private DoubleArray min = new DoubleArray();
    private DoubleArray max = new DoubleArray();

    /**
     * A constructor.
     *
     * @param fullIndexName e.g., <dir><name>.index.
     */
    public Index(String baseIndexName) 
            throws Exception {
        this.baseIndexName = baseIndexName;
    }

    /**
     * This deletes the index file created by the constructors.
     *
     */
    public void close() { 
        for (int i = 0; i < nIndices; i++) 
            File2.delete(baseIndexName + compactNames.get(i));
    }

    /**
     * The parts are done separately to conserve memory usage.
     *
     * @param name the columnName for the index.
     * @param pa  The PrimitiveArray with the data (it won't be changed).
     *    It must have at least one finite value.
     * @throws Exception if trouble
     */
    public void addIndex(String name, PrimitiveArray pa) throws Exception {

        nIndices++;
        if (nIndices == 1) {
            nRows = pa.size();
            lastFiniteRow = nRows - 1;
        }
        
        String errorInMethod = String2.ERROR + " in Index.addIndex:\n";
        Test.ensureEqual(pa.size(), nRows,
            errorInMethod + "pa.size != size for axis '" + name + "'.");
        long time = System.currentTimeMillis();

        //store the name
        names.add(name);
        String compactName = String2.replaceAll(name, " ", "");
        compactNames.add(compactName);

        //find min and max of data
        double stats[] = pa.calculateStats();
        Test.ensureNotEqual(stats[PrimitiveArray.STATS_N], 0, 
            errorInMethod + "index '" + name + "; has no data.");
        min.add(stats[PrimitiveArray.STATS_MIN]);
        max.add(stats[PrimitiveArray.STATS_MAX]);

        //delete any existing index file of the same name
        File2.delete(baseIndexName + compactName);
        Test.ensureEqual(File2.isFile(baseIndexName + compactName), false, 
            errorInMethod + "unable to delete " + baseIndexName + compactName);

        //open the output file
        FileChannel out = (new FileOutputStream(baseIndexName + compactName)).getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        byteBuffer.rewind();

        //process the rows of pa
        int tLastFiniteRow = -1;  
        int bufferPo = -8;
        for (int row = 0; row < nRows; row++) {

            //need to empty the buffer first?
            bufferPo += 8;
            if (bufferPo == bufferSize) {
                byteBuffer.flip(); //makes written bytes ready for writing
                Test.ensureEqual(out.write(byteBuffer), bufferSize, 
                    String2.ERROR + " in Index.addIndex: not all of buffer was written.");
                byteBuffer.rewind();
                bufferPo = 0;
            }

            //set the new values
            double d = pa.getDouble(row);
            if (Math2.isFinite(d))
                tLastFiniteRow = row;
            byteBuffer.putDouble(d); //use internal po
        }
        bufferPo += 8;
        byteBuffer.flip(); //makes written bytes ready for writing
        Test.ensureEqual(out.write(byteBuffer), //it knows po (bytes to write) internally
            bufferPo, 
            String2.ERROR + " in Index.addIndex: not all of buffer was written.");

        lastFiniteRow = Math.min(tLastFiniteRow, lastFiniteRow);

        //close out
        out.close();
        if (verbose) String2.log("Index.store time=" + (System.currentTimeMillis() - time));
    }


    /**
     * This returns a BitSet with true for the rows numbers which have 
     * data in the specified range.
     *
     * @param desiredMin The minimum acceptable values for the indices. 
     *     desiredMin.length must equal nIndices.
     * @param desiredMax The maximum acceptable values for the indices. 
     *     desiredMax.length must equal nIndices.
     * @return a Table with columns for each of the indices (all doubles)
     *     and a "row" column (int), indicating the row that this data came from.
     * @throws Exception if trouble
     */
    public Table subset(double desiredMin[], double desiredMax[]) throws Exception {

        if (verbose) String2.log("Index.subset " + 
            " desiredMin=" + String2.toCSSVString(desiredMin) + 
            " desiredMax=" + String2.toCSSVString(desiredMax)); 

        //set up the table
        long time = System.currentTimeMillis();
        Table table = new Table();
        DoubleArray doubleArray[] = new DoubleArray[nIndices];
        for (int i = 0; i < nIndices; i++) {
            doubleArray[i] = new DoubleArray(128, false);
            table.addColumn(names.get(i), doubleArray[i]);
        }
        IntArray rowArray  = new IntArray(128, false);
        table.addColumn("observation", rowArray);

        //quick reject    this also handles isConstant
        for (int i = 0; i < nIndices; i++) {
            if (desiredMin[i] > max.array[i] || desiredMax[i] < min.array[i])
                return table;
        }

        //open the files
        FileChannel in[] = new FileChannel[nIndices];
        ByteBuffer byteBuffer[] = new ByteBuffer[nIndices];
        for (int i = 0; i < nIndices; i++) {
            in[i] = (new FileInputStream(baseIndexName + compactNames.get(i))).getChannel();
            byteBuffer[i] = ByteBuffer.allocate(bufferSize);
        }

        //go through the data
        double rowValues[] = new double[nIndices];
        int filePo = 0;
        int bufferPo = -8;
        ROW_LOOP:
        for (int row = 0; row <= lastFiniteRow; row++) {
            //refill buffers?
            bufferPo += 8;  //position in bytes
            if ((bufferPo % bufferSize) == 0) {
                int desiredBytes = Math.min(bufferSize, nRows * 8 - filePo);
                for (int index = 0; index < nIndices; index++) {
                    byteBuffer[index].rewind();
                    //it would be best to keep track of nBytes read for each,
                    //  but I need to keep them in synch to simplify reading from them
                    Test.ensureEqual(in[index].read(byteBuffer[index], filePo), desiredBytes, 
                        String2.ERROR + " in Index.subset while filling buffers.");
                }
                filePo += bufferSize;
                bufferPo = 0;
            }

            //check if valid row
            for (int index = 0; index < nIndices; index++) {
                double d = byteBuffer[index].getDouble(bufferPo); 
                if (d < desiredMin[index] || d > desiredMax[index] || Double.isNaN(d)) {
                    continue ROW_LOOP;
                }
            rowValues[index] = d;
            }
            //success
            for (int index = 0; index < nIndices; index++) 
                doubleArray[index].add(rowValues[index]);
            rowArray.add(row);
            //if (verbose) String2.log("row=" + row + " values=" + String2.toCSSVString(values));
        }
            
        for (int index = 0; index < nIndices; index++) 
            in[index].close();

        String2.log("Index.subset time=" + (System.currentTimeMillis() - time));
            //+ " cumReadTime=" + cumulativeReadTime);
        return table;
    }

    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {
        Index.verbose = true;

        //testLittleMethods
        

        //setup for tests
        FloatArray  x = new FloatArray( new float[]{ -180, -120,   0, 120, 120, 180});
        FloatArray  y = new FloatArray( new float[]{  -20,  -45,   0,  45,  45,  90});
        FloatArray  z = new FloatArray( new float[]{    0,    0,   0,   0,   5,   0});
        DoubleArray t = new DoubleArray(new double[]{1e8,   2e8, 3e8, 4e8, 5e8, 6e8});
        String dir = File2.getSystemTempDirectory();
        String name = "IndexTest1";
        Index index = new Index(dir + name + ".index");
        index.addIndex("X", x);
        index.addIndex("Y", y);
        index.addIndex("Z", z);
        index.addIndex("T", t);

        //get all
        Table table = index.subset(
            new double[]{-180, -90, 0, 1e8},
            new double[]{ 180,  90, 5, 6e8});
        Test.ensureEqual(table.getColumn(4).toString(), "0, 1, 2, 3, 4, 5", "");

        //2 x's
        table = index.subset(
            new double[]{ 120, -90, 0, -1},
            new double[]{ 120,  90, 5, 1e10});
        Test.ensureEqual(table.getColumn(4).toString(), "3, 4", "");

        //1 y
        table = index.subset(
            new double[]{-180, -50, 0, 0},
            new double[]{ 180, -40, 5, 1e10});
        Test.ensureEqual(table.getColumn(4).toString(), "1", "");

        //1 z
        table = index.subset(
            new double[]{-180, -90, 4, 0},
            new double[]{ 180,  90, 7, 1e10});
        Test.ensureEqual(table.getColumn(4).toString(), "4", "");

        //1 t
        table = index.subset(
            new double[]{-180, -90, 0, 0},
            new double[]{ 180,  90, 5, 1.5e8});
        Test.ensureEqual(table.getColumn(4).toString(), "0", "");

        //nothing (time)
        table = index.subset(
            new double[]{-180, -90, 0, 0},
            new double[]{ 180,  90, 5, 1});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");

        index.close();


        //***test no variation
        //setup for tests
        x = new FloatArray( new float[]{ 1,1});
        y = new FloatArray( new float[]{ 2,2});
        z = new FloatArray( new float[]{ 3,3});
        t = new DoubleArray(new double[]{4,4});
        name = "IndexTest2";
        index = new Index(dir + name + ".index");
        index.addIndex("X", x);
        index.addIndex("Y", y);
        index.addIndex("Z", z);
        index.addIndex("T", t);

        //get all
        table = index.subset(
            new double[]{ 0,  0,  0,  0},
            new double[]{ 5,  5,  5,  5});
        Test.ensureEqual(table.getColumn(4).toString(), "0, 1", "");

        //reject based on x, y, z, t
        table = index.subset(
            new double[]{ 5,  0,  0,  0},
            new double[]{10,  5,  5,  5});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = index.subset(
            new double[]{ 0, -1,  0,  0},
            new double[]{ 5,  0,  5,  5});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = index.subset(
            new double[]{ 0,  0,  5,  0},
            new double[]{ 5,  5, 10,  5});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = index.subset(
            new double[]{ 0,  0,  0, -1},
            new double[]{ 5,  5,  5,  0});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");

        index.close();

        //*** test lots of data
        table = null;
        index = null;
        Math2.incgc(500);
        Math2.incgc(500);
        long memoryInUse = Math2.getMemoryInUse();
        String2.log("addIndex times should be ~2.5 seconds"); //with dataOutputStream ~7500
        int n = 10000000;
        name = "IndexTest3";
        index = new Index(dir + name + ".index");
        IntArray ia = new IntArray(0, n); //0, 1, 2, ...
        index.addIndex("X", ia);
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        index.addIndex("Y", ia); //0, 2, 4, ...
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        index.addIndex("Z", ia); //0, 4, 8, ...
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        index.addIndex("T", ia); //0, 8, 16, ...

        //find 1 
        String2.log("subset times should be ~ 1800 ms"); //with dataInputStream ~3600
        table = index.subset(
            new double[]{ 1,  2,  4,  8},
            new double[]{ 1,  2,  4,  8});
        Test.ensureEqual(table.getColumn(4).toString(), "1", "");  //row#
        table = index.subset(
            new double[]{ 1000,  2000,  4000,  8000},
            new double[]{ 1000,  2000,  4000,  8000});
        Test.ensureEqual(table.getColumn(4).toString(), "1000", "");  //row#
        table = index.subset(
            new double[]{ 1000000,  2000000,  4000000,  8000000},
            new double[]{ 1000000,  2000000,  4000000,  8000000});
        Test.ensureEqual(table.getColumn(4).toString(), "1000000", "");  //row#

        //reject based on x, y, z, t
        table = index.subset(
            new double[]{ 1.1,  2,  4,  8},
            new double[]{ 1.1,  2,  4,  8});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = index.subset(
            new double[]{ 1,  2.1,  4,  8},
            new double[]{ 1,  2.1,  4,  8});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = index.subset(
            new double[]{ 1,  2,  4.1,  8},
            new double[]{ 1,  2,  4.1,  8});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = index.subset(
            new double[]{ 1,  2,  4,  8.1},
            new double[]{ 1,  2,  4,  8.1});
        Test.ensureEqual(table.getColumn(4).toString(), "", "");

        index.close();

        //done
        ia = null;
        table = null;
        index = null;
        Math2.incgc(500);
        Math2.incgc(500);
        memoryInUse = Math2.getMemoryInUse() - memoryInUse;
        String2.log("\n***** Index.main finished successfully; memory not freed = " + memoryInUse);

    }


}
