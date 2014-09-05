/* 
 * PointSubsetFull Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Math2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.DataStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * THIS WORKS, BUT INACTIVE -- USE PointIndex.
 * This class manages x,y,z,t data so that EXACT subsets can be made quickly
 * (more suitable for Opendap, where data access is very slow).
 * This makes a file with x,y,z,t as doubles
 * and reads it to quickly make a table with the requested x,y,z,t data
 * (so it doesn't need to be downloaded via Opendap) 
 * and a bitset indicating which rows of data these correspond to
 * (so a range of the data value's rows can be downloaded and thinned to 
 * make finish creating the table). 
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-01-12
 */
public class PointSubsetFull  {

    private final static int X=0, Y=1, Z=2, T=3;  //don't change! current order has implications below
    private final static int offset[] = {8, 16, 24, 0};
    private final static int nBytesPerRow = 32;
    private final static String axisNames = "XYZT";
    private final static int bufferSize = 8192; //larger size doesn't help
 
    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    private String fullIndexName; 
    private int size;
    private int nFiniteRows;
    private boolean isConstant[] = new boolean[4];
    private double min[] = new double[4], max[] = new double[4];

    /**
     * A constructor.
     *
     * @param fullIndexName e.g., <dir><name>.index.
     * @param t  You can pass the original time PrimitiveArray (it won't be changed).
     *    It must have at least one finite value.
     * @throws Exception if trouble
     */
    public PointSubsetFull(String fullIndexName, PrimitiveArray t) 
            throws Exception {
        this.fullIndexName = fullIndexName;

        String errorInMethod = String2.ERROR + " in PointSubsetFull constructor:\n";
        size = t.size();
        nFiniteRows = size;

        //delete any existing index files of the same name
        for (int axis = 0; axis < 4; axis++) {
            String tName = fullIndexName + axisNames.charAt(axis);
            File2.delete(tName);
            Test.ensureEqual(File2.isFile(tName), false, 
                errorInMethod + "unable to delete " + tName);
        }

        //store t in the index file
        store(T, t);
    }

    /**
     * This deletes the index file created by the constructors.
     *
     */
    public void close() { 
        for (int axis = 0; axis < 4; axis++) {
            String tName = fullIndexName + axisNames.charAt(axis);
            File2.delete(tName);
        }
    }

    /**
     * This stores values from the x, y, z, or t axis PrimitiveArray
     * in fullIndexName. 
     *
     * <p>This is relatively slow (~12 s for each axis of ndbc data).
     * Using a randomAccessFile is vastly slower (>3 minutes per axis).
     *
     * @param axis is X, Y, Z, or T
     *    !!!t should be done first because others may set some t's to mv
     * @param pa a the corresponding original primitive array (it won't be changed).
     *    It must have at least one finite value.
     * @throws Exception if trouble
     */
    private void store(int axis, PrimitiveArray pa) throws Exception {

        String errorInMethod = String2.ERROR + " in PointSubsetFull.store:\n";
        Test.ensureEqual(size, pa.size(), 
            errorInMethod + "x.size != pa.size for axis " + axis + ".");
        long time = System.currentTimeMillis();
        
        //find min and max of data
        double stats[] = pa.calculateStats();
        Test.ensureNotEqual(stats[PrimitiveArray.STATS_N], 0, 
            errorInMethod + "axis " + axis + " has no data.");
        min[axis] = stats[PrimitiveArray.STATS_MIN];
        max[axis] = stats[PrimitiveArray.STATS_MAX];
        isConstant[axis] = min[axis] == max[axis];
        if (verbose) String2.log("PointSubsetFull.store axis=" + axis + 
            " min=" + min[axis] + " max=" + max[axis]); 

        //open the output file
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(fullIndexName + axisNames.charAt(axis))));

        //process the rows of pa
        int lastFiniteRow = -1;  
        for (int row = 0; row < size; row++) {

            //set the new values
            double d = pa.getDouble(row);
            if (Math2.isFinite(d))
                lastFiniteRow = row;
            out.writeDouble(d);

        }

        nFiniteRows = Math.min(lastFiniteRow + 1, nFiniteRows);

        //close in and out and rename
        out.close();
        if (verbose) String2.log("PointSubsetFull.store time=" + (System.currentTimeMillis() - time));
    }

    /** 
    //***Trouble: ByteBuffers are garbage collected!?.
     * This stores the axis' information in fullFileName using
     * a randomAccessFile and a FileChannel.
     */
/*    private void store(int axis, PrimitiveArray pa) throws Exception {

        String errorInMethod = String2.ERROR + " in PointSubsetFull.store:\n";
        Test.ensureEqual(size, pa.size(), 
            errorInMethod + "x.size != pa.size for axis " + axis + ".");
        long time = System.currentTimeMillis();
        
        //find min and max of data
        double stats[] = pa.calculateStats();
        Test.ensureNotEqual(stats[PrimitiveArray.STATS_N], 0, 
            errorInMethod + "axis " + axis + " has no data.");
        min[axis] = stats[PrimitiveArray.STATS_MIN];
        max[axis] = stats[PrimitiveArray.STATS_MAX];
        isConstant[axis] = min[axis] == max[axis];
        if (verbose) String2.log("PointSubsetFull.store axis=" + axis + 
            " min=" + min[axis] + " max=" + max[axis]); 

        //open the randomAccessFile file 
        RandomAccessFile raf = new RandomAccessFile(fullIndexName, "rw");
        raf.seek(size * nBytesPerRow); raf.write(0); raf.seek(0);
        FileChannel rwChannel = raf.getChannel();
        try {
            ByteBuffer byteBuffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0,
                size * nBytesPerRow);

            //process the rows of pa
            int tNFiniteRows = 0;
            int po = offset[axis];
            for (int row = 0; row < size; row++) {
                //set the new values
                double d = pa.getDouble(row);
                if (Math2.isFinite(d)) {
                    tNFiniteRows = row + 1;
                } else {
                    d = Double.NaN;
                    if (axis != T) 
                        byteBuffer.putDouble(po - offset[axis], Double.NaN);
                }
                byteBuffer.putDouble(po, d);
                po += nBytesPerRow;
            }
            nFiniteRows = Math.min(tNFiniteRows, nFiniteRows);

            //diagnostic
            if (verbose && axis == Z) { 
                po = -8;
                for (int row = 0; row < Math.min(size, 10); row++) {
                    String2.log(
                        " t=" + byteBuffer.getDouble(po += 8) +
                        " x=" + byteBuffer.getDouble(po += 8) + 
                        " y=" + byteBuffer.getDouble(po += 8) + 
                        " z=" + byteBuffer.getDouble(po += 8));
                }
            }

            //close file
            rwChannel.close();
        } catch (Exception e) {
            rwChannel.close();
            throw e;
        }

        if (verbose) String2.log("PointSubsetFull.store time=" + (System.currentTimeMillis() - time));
    }
*/
    /**
     * The fourth and final part of the constructor.
     * The parts are done separately to conserve memory usage.
     *
     * @param x  You can pass the original x PrimitiveArray (it won't be changed).
     *    It must have at least one finite value.
     * @throws Exception if trouble
     */
    public void constructorX(PrimitiveArray x) throws Exception {
        store(X, x);

    }
    /**
     * The second part of the constructor.
     * The parts are done separately to conserve memory usage.
     *
     * @param y  You can pass the original y PrimitiveArray (it won't be changed).
     *    It must have at least one finite value.
     * @throws Exception if trouble
     */
    public void constructorY(PrimitiveArray y) throws Exception {
        store(Y, y);

    }

    /**
     * The third part of the constructor.
     * The parts are done separately to conserve memory usage.
     *
     * @param z  You can pass the original z PrimitiveArray (it won't be changed).
     *    It must have at least one finite value.
     * @throws Exception if trouble
     */
    public void constructorZ(PrimitiveArray z) throws Exception {
        store(Z, z);
    }

    /**
     * This returns a BitSet with true for the rows numbers which have 
     * data in the specified range.
     *
     * @param minX  all min/max must be finite values
     * @param maxX
     * @param minY
     * @param maxY
     * @param minZ
     * @param maxZ
     * @param minT
     * @param maxT
     * @return a Table with 5 columns ("longitude", "latitude", "altitude", "time") (all doubles) and "row" (int))
     * @throws Exception if trouble
     */
    public Table subset(double desiredMinX, double desiredMaxX, 
        double desiredMinY, double desiredMaxY,
        double desiredMinZ, double desiredMaxZ, 
        double desiredMinT, double desiredMaxT) throws Exception {

        if (verbose) String2.log("PointSubsetFull.subset " + 
            " desiredMinX=" + desiredMinX + " desiredMaxX=" + desiredMaxX + 
            " desiredMinY=" + desiredMinY + " desiredMaxY=" + desiredMaxY + 
            " desiredMinZ=" + desiredMinZ + " desiredMaxZ=" + desiredMaxZ + 
            " desiredMinT=" + desiredMinT + " desiredMaxT=" + desiredMaxT); 

        //set up the table
        long time = System.currentTimeMillis();
        Table table = new Table();
        DoubleArray xArray = new DoubleArray(128, false);
        DoubleArray yArray = new DoubleArray(128, false);
        DoubleArray zArray = new DoubleArray(128, false);
        DoubleArray tArray = new DoubleArray(128, false);
        IntArray rowArray  = new IntArray(128, false);
        table.addColumn("longitude", xArray);
        table.addColumn("latitude",  yArray);
        table.addColumn("altitude",  zArray);
        table.addColumn("time",      tArray);
        table.addColumn("row",       rowArray);

        //quick reject    this also handles isConstant[x|y|z|t]
        if (desiredMinX > max[X] || desiredMaxX < min[X] ||
            desiredMinY > max[Y] || desiredMaxY < min[Y] ||
            desiredMinZ > max[Z] || desiredMaxZ < min[Z] ||
            desiredMinT > max[T] || desiredMaxT < min[T]) {            
            return table;
        }

        //open the file 
        DataInputStream inX = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "X"), bufferSize));
        DataInputStream inY = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "Y"), bufferSize));
        DataInputStream inZ = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "Z"), bufferSize));
        DataInputStream inT = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "T"), bufferSize));
        int xSkip = 0, ySkip = 0, zSkip = 0, tSkip = 0;
        try {
            for (int row = 0; row < nFiniteRows; row++) {
                double t = inT.readDouble();
                if (t < desiredMinT || t > desiredMaxT || Double.isNaN(t)) {
                    xSkip += 8; //ySkip and zSkip will be incremented later
                    continue;
                }
                ySkip += xSkip; //y needs to skip as much as x; logic is a little tricky
                while (xSkip > 0) xSkip -= inX.skipBytes(xSkip);
                double x = inX.readDouble();
                if (x < desiredMinX || x > desiredMaxX || Double.isNaN(x)) {
                    ySkip += 8; //zSkip will be incremented later
                    continue;
                }
                zSkip += ySkip; //z needs to skip as much as y
                while (ySkip > 0) ySkip -= inY.skipBytes(ySkip);
                double y = inY.readDouble();
                if (y < desiredMinY || y > desiredMaxY || Double.isNaN(y)) {
                    zSkip += 8;
                    continue;
                }
                while (zSkip > 0) zSkip -= inZ.skipBytes(zSkip);
                double z = inZ.readDouble();
                if (z < desiredMinZ || z > desiredMaxZ || Double.isNaN(z)) {
                    continue;
                }

                //success
                xArray.add(x);
                yArray.add(y);
                zArray.add(z);
                tArray.add(t);
                rowArray.add(row);
                //if (verbose) String2.log("row=" + row + " x=" + x + 
                //    " y=" + y + " z=" + z + " t=" + t + " ok=true");
            }
            inX.close();
            inY.close();
            inZ.close();
            inT.close();
        } catch (Exception e) {
            inX.close();
            inY.close();
            inZ.close();
            inT.close();
            throw e;
        }

        String2.log("PointSubsetFull time=" + (System.currentTimeMillis() - time));
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
        PointSubsetFull.verbose = true;

        //testLittleMethods
        

        //setup for tests
        FloatArray  x = new FloatArray( new float[]{ -180, -120,   0, 120, 120, 180});
        FloatArray  y = new FloatArray( new float[]{  -20,  -45,   0,  45,  45,  90});
        FloatArray  z = new FloatArray( new float[]{    0,    0,   0,   0,   5,   0});
        DoubleArray t = new DoubleArray(new double[]{1e8,   2e8, 3e8, 4e8, 5e8, 6e8});
        String dir = File2.getSystemTempDirectory();
        String name = "PointSubsetFullTest1";
        PointSubsetFull pointSubsetFull = new PointSubsetFull(dir + name + ".index", t);
        pointSubsetFull.constructorX(x);
        pointSubsetFull.constructorY(y);
        pointSubsetFull.constructorZ(z);

        //get all
        Table table = pointSubsetFull.subset(-180, 180, -90, 90, 0, 5, 1e8, 6e8);
        Test.ensureEqual(table.getColumn(4).toString(), "0, 1, 2, 3, 4, 5", "");

        //2 x's
        table = pointSubsetFull.subset(120, 120, -90, 90, 0, 5, -1, 1e10);
        Test.ensureEqual(table.getColumn(4).toString(), "3, 4", "");

        //1 y
        table = pointSubsetFull.subset(-180, 180, -50, -40, 0, 5, 0, 1e10);
        Test.ensureEqual(table.getColumn(4).toString(), "1", "");

        //1 z
        table = pointSubsetFull.subset(-180, 180, -90, 90, 4, 7, 0, 1e10);
        Test.ensureEqual(table.getColumn(4).toString(), "4", "");

        //1 t
        table = pointSubsetFull.subset(-180, 180, -90, 90, 0, 5, 0, 1.5e8);
        Test.ensureEqual(table.getColumn(4).toString(), "0", "");

        //nothing (time)
        table = pointSubsetFull.subset(-180, 180, -90, 90, 0, 5, 0, 1);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");

        pointSubsetFull.close();


        //***test no variation
        //setup for tests
        x = new FloatArray( new float[]{ 1,1});
        y = new FloatArray( new float[]{ 2,2});
        z = new FloatArray( new float[]{ 3,3});
        t = new DoubleArray(new double[]{4,4});
        name = "PointSubsetFullTest2";
        pointSubsetFull = new PointSubsetFull(dir + name + ".index", t);
        pointSubsetFull.constructorX(x);
        pointSubsetFull.constructorY(y);
        pointSubsetFull.constructorZ(z);

        //get all
        table = pointSubsetFull.subset(0, 5, 0, 5, 0, 5, 0, 5);
        Test.ensureEqual(table.getColumn(4).toString(), "0, 1", "");

        //reject based on x, y, z, t
        table = pointSubsetFull.subset(5, 10, 0, 5, 0, 5, 0, 5);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = pointSubsetFull.subset(0, 5, -1, 0, 0, 5, 0, 5);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = pointSubsetFull.subset(0, 5, 0, 5, 5, 10, 0, 5);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = pointSubsetFull.subset(0, 5, 0, 5, 0, 5, -1, 0);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");

        pointSubsetFull.close();

        //*** test lots of data
        //each construction stage takes about 3 s
        int n = 10000000;
        IntArray ia = new IntArray(0, n); //0, 1, 2, ...
        name = "PointSubsetFullTest3";
        pointSubsetFull = new PointSubsetFull(dir + name + ".index", ia);
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        pointSubsetFull.constructorX(ia); //0, 2, 4, ...
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        pointSubsetFull.constructorY(ia); //0, 4, 8, ...
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        pointSubsetFull.constructorZ(ia); //0, 8, 16, ...

        //find 1 
        String2.log("times should be ~ 2422 ms");
        table = pointSubsetFull.subset(2,2, 4,4, 8,8, 1,1);
        Test.ensureEqual(table.getColumn(4).toString(), "1", "");  //row#
        table = pointSubsetFull.subset(2000,2000, 4000,4000, 8000,8000, 1000,1000);
        Test.ensureEqual(table.getColumn(4).toString(), "1000", "");  //row#
        table = pointSubsetFull.subset(2000000,2000000, 4000000,4000000, 8000000,8000000, 1000000,1000000);
        Test.ensureEqual(table.getColumn(4).toString(), "1000000", "");  //row#

        //reject based on x, y, z, t
        table = pointSubsetFull.subset(2.1,2.1, 4,4, 8,8, 1,1);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = pointSubsetFull.subset(2,2, 4.1,4.1, 8,8, 1,1);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = pointSubsetFull.subset(2,2, 4,4, 8.1,8.1, 1,1);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");
        table = pointSubsetFull.subset(2,2, 4,4, 8,8, 1.1,1.1);
        Test.ensureEqual(table.getColumn(4).toString(), "", "");

        pointSubsetFull.close();

        //done
        String2.log("\n***** PointSubsetFull.main finished successfully");
        Math2.incgc(2000);

    }


}
