/* 
 * PointSubsetScaled Copyright 2005, NOAA.
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
 * This class manages x,y,z,t data so that APPROXIMATE subsets can be made quickly
 * (more suitable for .nc files, where data access is slow but not as bad as Opendap).
 * Direct search of x,y,z,t from .nc files takes a long time just to read the 
 * data for the tests in Table (about 26 s for ndbc).
 * This makes a file with x,y,z scaled to bytes and t scaled to short
 * and reads it to quickly make a subset that has all the requested rows
 * and perhaps some extras (about 5 s for ndbc). 
 * It can be thought of as a way to quickly narrow down the list of possibilities.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-01-12
 */
public class PointSubsetScaled {
    //future?
    //  store x,y,z as short?
    //  store t as int or 3 byte?

    private final static int X=0, Y=1, Z=2, T=3;  //don't change! current order has implications below
    private final static String axisNames = "XYZT";
    private final static int offset[] = {2, 3, 4, 0};
    private final static int nBytesPerRow = 5;
    //reserve MAX_VALUE for mv; regular values are scale then rounded, so .4 is best
    private final static double byteScale = 254.4, shortScale = 65534.4;
    private final static int bufferSize = 8192; //larger size doesn't help
 
    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    private String fullIndexName, altFullIndexName;
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
    public PointSubsetScaled(String fullIndexName, PrimitiveArray t) 
            throws Exception {
        this.fullIndexName = fullIndexName;

        String errorInMethod = String2.ERROR + " in PointSubsetScaled constructor:\n";
        size = t.size();
        nFiniteRows = size;

        //delete any existing index files of the same name
        for (int axis = 0; axis < 4; axis++) {
            String tName = fullIndexName + axisNames.charAt(axis);
            File2.delete(tName);
            Test.ensureEqual(File2.isFile(tName), false, 
                errorInMethod + "unable to delete " + tName);
        }
        /*
        File2.delete(fullIndexName);
        Test.ensureEqual(File2.isFile(fullIndexName), false, 
            errorInMethod + "unable to delete " + fullIndexName);

        altFullIndexName = fullIndexName + ".tmp";
        File2.delete(altFullIndexName);
        Test.ensureEqual(File2.isFile(altFullIndexName), false, 
            errorInMethod + "unable to delete " + altFullIndexName);
        */

        //store t in the index file
        store(T, t);
    }

    /**
     * This deletes the index file created by the constructors.
     *
     */
    public void close() { 
        File2.delete(fullIndexName);
    }

    /**
     * This moves the remaining old data to the beginning of the buffer and tries
     * to refill the buffer (but insists on getting at least nDesired).
     * !!This is a different system that the rest of this class.
     * !!Afterwards, set your 'offset' variable (offset of the next byte to be 
     *   processed) to 0.
     *
     * @param in
     * @param out if not null, bytes 0..oldOffset will be written to out 
     * @param buffer
     * @param oldOffset the start of the unprocessed data remaining in the buffer
     * @param oldBytesInBuffer the number of valid bytes 
     * @param nDesired the minimum number of bytes needed
     * @return newBytesInBuffer
     * @throws Exception if trouble
     */
    public static int refill(InputStream in, OutputStream out, byte buffer[], 
            int oldOffset, int oldBytesInBuffer, int nDesired) throws Exception {

        //write bytes 0..oldOffset?
        if (out != null) {
            out.write(buffer, 0, oldOffset);
        }

        //copy unprocessed bytes to start of buffer
        System.arraycopy(buffer, oldOffset, buffer, 0, oldBytesInBuffer - oldOffset);
        int newBytesInBuffer = oldBytesInBuffer - oldOffset;
        while (newBytesInBuffer < nDesired)
            newBytesInBuffer += in.read(buffer, newBytesInBuffer, buffer.length - newBytesInBuffer);
        return newBytesInBuffer;
    }

    /**
     * Read a short from the stream.
     *
     * @param buffer
     * @param offset offset of the first byte to be used
     * @return a short
     */
    public static int readShort(byte buffer[], int offset) {
        return ((buffer[offset]) << 8) | (buffer[offset + 1] & 255);
    }

    /**
     * Write a short to a buffer.
     *
     * @param buffer
     * @param offset of the first byte to be written to
     * @param s
     */
    public static void writeShort(byte buffer[], int offset, short s) {
        buffer[offset  ] = (byte)(s >> 8);
        buffer[offset+1] = (byte)s; //write() writes low 8 bits of value
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
        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(fullIndexName + axisNames.charAt(axis)), bufferSize);

        //process the rows of pa
        int lastFiniteRow = -1;  
        int po = 0;
        byte buffer[] = new byte[bufferSize];
        if (axis == T) {
            for (int row = 0; row < size; row++) {
                if (po == bufferSize) {
                    out.write(buffer);
                    po = 0;
                }
                double d = pa.getDouble(row);
                short t;
                if (Math2.isFinite(d)) {
                    lastFiniteRow = row;
                    t = isConstant[axis]? Short.MIN_VALUE : //but irrelevant for subset()
                        Math2.roundToShort(Short.MIN_VALUE + 
                            shortScale * (d - min[axis]) / (max[axis] - min[axis]));
                } else {
                    t = Short.MAX_VALUE;
                }
                buffer[po++] = (byte)(t >> 8);
                buffer[po++] = (byte)t;
            }
        } else {
            for (int row = 0; row < size; row++) {
                if (po == bufferSize) {
                    out.write(buffer);
                    po = 0;
                }
                double d = pa.getDouble(row);
                if (Math2.isFinite(d)) {
                    lastFiniteRow = row;
                    buffer[po++] = isConstant[axis]? Byte.MIN_VALUE : //but irrelevant for subset()
                        Math2.roundToByte(Byte.MIN_VALUE + 
                            byteScale * (d - min[axis]) / (max[axis] - min[axis]));
                } else {
                    buffer[po++] = Byte.MAX_VALUE;
                    //also set t to MAX_VALUE since it is checked first
                    //writeShort(buffer, bufferOffset + offset[T], Short.MAX_VALUE);  
                }
            }
        }
        //write remainder of buffer
        out.write(buffer, 0, po);
        nFiniteRows = Math.min(lastFiniteRow + 1, nFiniteRows);

        //close in and out and rename
        out.close();
        if (verbose) String2.log("PointSubsetFull.store time=" + (System.currentTimeMillis() - time));
    }

    /**
     * This stores scaled values from the x, y, z, or t axis PrimitiveArray
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
/*    private void store(int axis, PrimitiveArray pa) throws Exception {

        String errorInMethod = String2.ERROR + " in PointSubsetScaled.store:\n";
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
        if (verbose) String2.log("PointSubsetScaled.store axis=" + axis + 
            " min=" + min[axis] + " max=" + max[axis]); 

        //open the old file (in) and new file (out)
        FileInputStream in = null;
        try {
            in = new FileInputStream(fullIndexName);
        } catch (Exception e) {
            if (axis != T) 
                throw e;
        }
        FileOutputStream out = new FileOutputStream(altFullIndexName);

        //process the rows of pa
        int lastFiniteRow = -1;
        byte buffer[] = new byte[bufferSize];
        int bytesInBuffer = 0;
        int bufferOffset = -nBytesPerRow;
        for (int row = 0; row < size; row++) {
            bufferOffset += nBytesPerRow;
            if (bytesInBuffer - bufferOffset < nBytesPerRow) {
                if (in == null) {
                    out.write(buffer, 0, bufferOffset);
                    Arrays.fill(buffer, (byte)0);
                    bufferOffset = 0;
                    bytesInBuffer = buffer.length;
                } else {
                    bytesInBuffer = refill(in, out, buffer, bufferOffset, 
                        bytesInBuffer, nBytesPerRow);
                    bufferOffset = 0;
                }
            }

            //set the new values
            double d = pa.getDouble(row);
            if (axis == T) {
                short t;
                if (Math2.isFinite(d)) {
                    lastFiniteRow = row;
                    t = isConstant[axis]? Short.MIN_VALUE : //but irrelevant for subset()
                        Math2.roundToShort(Short.MIN_VALUE + 
                            shortScale * (d - min[axis]) / (max[axis] - min[axis]));
                } else {
                    t = Short.MAX_VALUE;
                }
                writeShort(buffer, bufferOffset + offset[T], t); 
            } else {
                byte b; 
                if (Math2.isFinite(d)) {
                    lastFiniteRow = row;
                    b = isConstant[axis]? Byte.MIN_VALUE : //but irrelevant for subset()
                        Math2.roundToByte(Byte.MIN_VALUE + 
                            byteScale * (d - min[axis]) / (max[axis] - min[axis]));
                } else {
                    b = Byte.MAX_VALUE;
                    //also set t to MAX_VALUE since it is checked first
                    writeShort(buffer, bufferOffset + offset[T], Short.MAX_VALUE);  
                }
                buffer[bufferOffset + offset[axis]] = b; 
            }

            //diagnostic
            //if (verbose && row < Math.min(size, 10)) 
            //    String2.log(
            //        " x=" + String2.right("" + buffer[bufferOffset + offset[X]], 3) + 
            //        " y=" + String2.right("" + buffer[bufferOffset + offset[Y]], 3) + 
            //        " z=" + String2.right("" + buffer[bufferOffset + offset[Z]], 3) + 
            //        " t=" + String2.right("" + readShort(buffer, bufferOffset), 6) +
            //        " d=" + d);
        }
        //write bytes remaining in buffer
        bufferOffset += nBytesPerRow;
        out.write(buffer, 0, bufferOffset);

        nFiniteRows = Math.min(lastFiniteRow + 1, nFiniteRows);

        //close in and out and rename
        if (in != null) in.close();
        out.close();
        File2.delete(fullIndexName);
        File2.rename(altFullIndexName, fullIndexName);
        if (verbose) String2.log("PointSubsetScaled.store time=" + (System.currentTimeMillis() - time));
    }
*/
    /*
    //a version of store that uses a randomAccessFile and a FileChannel
    //***Trouble: ByteBuffers are garbage collected!?.
    //This isn't any faster, but may be faster when other processes are running
    //  and using the disk drive's cache.
    private void store(int axis, PrimitiveArray pa) throws Exception {

        String errorInMethod = String2.ERROR + " in PointSubsetScaled.store:\n";
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
        if (verbose) String2.log("PointSubsetScaled.store axis=" + axis + 
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
            if (axis == T) {
                for (int row = 0; row < size; row++) {
                    //set the new values
                    double d = pa.getDouble(row);
                    short t;
                    if (Math2.isFinite(d)) {
                        tNFiniteRows = row + 1;
                        t = isConstant[axis]? Short.MIN_VALUE : //but irrelevant for subset()
                            Math2.roundToShort(Short.MIN_VALUE + 
                                shortScale * (d - min[axis]) / (max[axis] - min[axis]));
                    } else {
                        t = Short.MAX_VALUE;
                    }
                    //raf.seek(po); raf.writeShort(t);   //standard way
                    byteBuffer.putShort(po, t);  //bytebuffer way
                    po += nBytesPerRow;
                }
            } else { //other axes
                int axisOffset = offset[axis];
                for (int row = 0; row < size; row++) {
                    double d = pa.getDouble(row);
                    byte b; 
                    if (Math2.isFinite(d)) {
                        tNFiniteRows = row + 1;
                        b = isConstant[axis]? Byte.MIN_VALUE : //but irrelevant for subset()
                            Math2.roundToByte(Byte.MIN_VALUE + 
                                byteScale * (d - min[axis]) / (max[axis] - min[axis]));
                    } else {
                        b = Byte.MAX_VALUE;
                        //also set t to MAX_VALUE since it is checked first
                        //raf.seek(po - axisOffset); raf.writeShort(Short.MAX_VALUE); 
                        byteBuffer.putShort(po - axisOffset, Short.MAX_VALUE);
                    }
                    //raf.seek(po); raf.writeByte(b); 
                    byteBuffer.put(po, b);       //bytebuffer way
                    po += nBytesPerRow;
                }
            }
            nFiniteRows = Math.min(tNFiniteRows, nFiniteRows);

            //diagnostic
            //if (verbose && axis == Z) { 
            //    for (int row = 0; row < Math.min(size, 10); row++) {
            //        raf.seek(row * nBytesPerRow);
            //        String2.log(
            //            " t=" + String2.right("" + raf.readShort(), 6) +
            //            " x=" + String2.right("" + raf.readByte(), 3) + 
            //            " y=" + String2.right("" + raf.readByte(), 3) + 
            //            " z=" + String2.right("" + raf.readByte(), 3));
            //    }
            //}

            //close channel
            rwChannel.close();
        } catch (Exception e) {
            rwChannel.close();
            throw e;
        }

        //close the file
        if (verbose) String2.log("PointSubsetScaled.store time=" + (System.currentTimeMillis() - time));
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
/* was part of constructorZ    
        //combine the x,y,z,t index files
        long time = System.currentTimeMillis();
        DataInputStream indexX = DataStream.getDataInputStream(fullIndexName + "X");
        DataInputStream indexY = DataStream.getDataInputStream(fullIndexName + "Y");
        DataInputStream indexZ = DataStream.getDataInputStream(fullIndexName + "Z");
        DataInputStream indexT = DataStream.getDataInputStream(fullIndexName + "T");
        DataOutputStream index = DataStream.getDataOutputStream(fullIndexName);
        byte xBuffer[] = new byte[bufferSize];
        byte yBuffer[] = new byte[bufferSize];
        byte zBuffer[] = new byte[bufferSize];
        byte tBuffer[] = new byte[2 * bufferSize];
        byte rowBuffer[] = new byte[nBytesPerRow];
        int po = bufferSize; //to trigger refilling the buffers
        for (int row = 0; row < size; row++) {
            //refill the buffers
            if (po == bufferSize) {
                int nToRead = Math.min(bufferSize, size - row);
                indexX.readFully(xBuffer, 0, nToRead);
                indexY.readFully(yBuffer, 0, nToRead);
                indexZ.readFully(zBuffer, 0, nToRead);
                indexT.readFully(tBuffer, 0, 2 * nToRead);
                po = 0;
            }
            
            //fill the rowBuffer
            short tShort = 
                (xBuffer[po] == Byte.MAX_VALUE || 
                 yBuffer[po] == Byte.MAX_VALUE || 
                 zBuffer[po] == Byte.MAX_VALUE)? 
                Short.MAX_VALUE : 
                (short)readShort(tBuffer, 2 * po);
            rowBuffer[offset[X]] = xBuffer[po];
            rowBuffer[offset[Y]] = yBuffer[po];
            rowBuffer[offset[Z]] = zBuffer[po];
            writeShort(rowBuffer, offset[T], tShort);
            po++;
            //diagnostic
            //if (verbose && row < Math.min(size, 10)) 
            //    String2.log(
            //        " x=" + String2.right("" + xByte, 3) + 
            //        " y=" + String2.right("" + yByte, 3) + 
            //        " z=" + String2.right("" + zByte, 3) + 
            //        " t=" + String2.right("" + tShort, 6));

            //write the rowBuffer
            index.write(rowBuffer);
        }

        indexX.close();
        indexY.close();
        indexZ.close();
        indexY.close();
        index.close();

        File2.delete(fullIndexName + "X");
        File2.delete(fullIndexName + "Y");
        File2.delete(fullIndexName + "Z");
        File2.delete(fullIndexName + "T");
        if (verbose) String2.log("PointSubsetScaled.constructorZ time to combine = " + 
            (System.currentTimeMillis() - time));
    }
*/

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
     * @return a BitSet with true for the rows numbers which have 
     *     data in the specified range.
     * @throws Exception
     */
    public BitSet subset(double desiredMinX, double desiredMaxX, 
        double desiredMinY, double desiredMaxY,
        double desiredMinZ, double desiredMaxZ, 
        double desiredMinT, double desiredMaxT) throws Exception {

        if (verbose) String2.log("PointSubsetScaled.subset " + 
            " desiredMinX=" + desiredMinX + " desiredMaxX=" + desiredMaxX + 
            " desiredMinY=" + desiredMinY + " desiredMaxY=" + desiredMaxY + 
            " desiredMinZ=" + desiredMinZ + " desiredMaxZ=" + desiredMaxZ + 
            " desiredMinT=" + desiredMinT + " desiredMaxT=" + desiredMaxT); 

        //set up the bitSet; all bits set to false (!okay) initially, since that is most common result
        //this is relied on in serveral ways below (including rows loop to < nFiniteRows)
        long time = System.currentTimeMillis();
        BitSet bitSet = new BitSet(size); 

        //quick reject    this also handles isConstant[x|y|z|t]
        if (desiredMinX > max[X] || desiredMaxX < min[X] ||
            desiredMinY > max[Y] || desiredMaxY < min[Y] ||
            desiredMinZ > max[Z] || desiredMaxZ < min[Z] ||
            desiredMinT > max[T] || desiredMaxT < min[T]) {            
            return bitSet;
        }

        //won't find greater than actual range 
        //(this prevents overflowing target data type's range)
        desiredMinX = Math.max(desiredMinX, min[X]); desiredMaxX = Math.min(desiredMaxX, max[X]);
        desiredMinY = Math.max(desiredMinY, min[Y]); desiredMaxY = Math.min(desiredMaxY, max[Y]);
        desiredMinZ = Math.max(desiredMinZ, min[Z]); desiredMaxZ = Math.min(desiredMaxZ, max[Z]);
        desiredMinT = Math.max(desiredMinT, min[T]); desiredMaxT = Math.min(desiredMaxT, max[T]);

        //calculate target for t  (short)
        short targetMinT = isConstant[T]? Short.MIN_VALUE : 
            Math2.roundToShort(Short.MIN_VALUE + 
                shortScale * (desiredMinT - min[T]) / (max[T] - min[T]));
        short targetMaxT = isConstant[T]? Short.MIN_VALUE : 
            Math2.roundToShort(Short.MIN_VALUE + 
                shortScale * (desiredMaxT - min[T]) / (max[T] - min[T]));

        //calculate target for x,y,z  (bytes)
        byte targetMinX = isConstant[X]? Byte.MIN_VALUE : 
            Math2.roundToByte(Byte.MIN_VALUE + 
                byteScale * (desiredMinX - min[X]) / (max[X] - min[X]));
        byte targetMaxX = isConstant[X]? Byte.MIN_VALUE : 
            Math2.roundToByte(Byte.MIN_VALUE + 
                byteScale * (desiredMaxX - min[X]) / (max[X] - min[X]));

        byte targetMinY = isConstant[Y]? Byte.MIN_VALUE : 
            Math2.roundToByte(Byte.MIN_VALUE + 
                byteScale * (desiredMinY - min[Y]) / (max[Y] - min[Y]));
        byte targetMaxY = isConstant[Y]? Byte.MIN_VALUE : 
            Math2.roundToByte(Byte.MIN_VALUE + 
                byteScale * (desiredMaxY - min[Y]) / (max[Y] - min[Y]));

        byte targetMinZ = isConstant[Z]? Byte.MIN_VALUE : 
            Math2.roundToByte(Byte.MIN_VALUE + 
                byteScale * (desiredMinZ - min[Z]) / (max[Z] - min[Z]));
        byte targetMaxZ = isConstant[Z]? Byte.MIN_VALUE : 
            Math2.roundToByte(Byte.MIN_VALUE + 
                byteScale * (desiredMaxZ - min[Z]) / (max[Z] - min[Z]));

        if (verbose) String2.log("PointSubsetScaled.subset " + 
            " targetMinX=" + targetMinX + " targetMaxX=" + targetMaxX + 
            " targetMinY=" + targetMinY + " targetMaxY=" + targetMaxY + 
            " targetMinZ=" + targetMinZ + " targetMaxZ=" + targetMaxZ + 
            " targetMinT=" + targetMinT + " targetMaxT=" + targetMaxT); 


        //open the file
        //I tried using just FileInputStream and doing my own buffer,
        //but times were slower and more variable.
        //DataInputStream gives me access to readFully(buffer)
        DataInputStream inX = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "X"), bufferSize));
        DataInputStream inY = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "Y"), bufferSize));
        DataInputStream inZ = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "Z"), bufferSize));
        DataInputStream inT = new DataInputStream(new BufferedInputStream(
            new FileInputStream(fullIndexName + "T"), bufferSize));
        //or, using ByteBuffer approach... (which seems to be slower here)
        //FileChannel in = new FileInputStream(fullIndexName).getChannel();
        //ByteBuffer byteBuffer = in.map(FileChannel.MapMode.READ_ONLY, 0, (int)in.size()); 

        //go through the rows
        //This is the inner loop of the whole process.
        //Using a buffer to get all results for one row is 2x faster than 
        //  separate readByte's and readShort.
        //But using a big buffer to get lots of rows is ~ as slow
        //  as separate readByte's and readShort
        //  (because of arithmetic to calculate positions in the buffer?).
        byte xBuffer[] = new byte[bufferSize]; 
        byte yBuffer[] = new byte[bufferSize]; 
        byte zBuffer[] = new byte[bufferSize]; 
        byte tBuffer[] = new byte[2*bufferSize]; 
        long cumulativeReadTime = 0;
        int po = 0;
        int nRemain = nFiniteRows;
        for (int row = 0; row < nFiniteRows; row++) {
            //read a row's worth of data
            //cumulativeReadTime -= System.currentTimeMillis(); //this adds significant overhead
            if ((po % bufferSize) == 0) {
                int nToRead = Math.min(bufferSize, nRemain);
                nRemain -= nToRead;
                inX.readFully(xBuffer, 0, nToRead);   //about 70% of time is spend reading
                inY.readFully(yBuffer, 0, nToRead);  
                inZ.readFully(zBuffer, 0, nToRead);  
                inT.readFully(tBuffer, 0, nToRead * 2);  
                po = 0;
            }
            //or, using ByteBuffer approach... (which seems to be slower here)
            //byteBuffer.get(buffer); //Fill the array from the buffer 
            //cumulativeReadTime += System.currentTimeMillis(); //this adds significant overhead

            int t = (tBuffer[po + po] << 8) | (tBuffer[po + po + 1] & 255);

            //test (do t first since most likely to fail)
            if (t >= targetMinT && t <= targetMaxT && 
                xBuffer[po] >= targetMinX && xBuffer[po] <= targetMaxX &&
                yBuffer[po] >= targetMinY && yBuffer[po] <= targetMaxY &&
                zBuffer[po] >= targetMinZ && zBuffer[po] <= targetMaxZ) {
                //success
                bitSet.set(row);
                //if (verbose) String2.log("row=" + row + " x=" + buffer[2] + 
                //    " y=" + buffer[3] + " z=" + buffer[4] + " t=" + t + " ok=" + bitSet.get(row));
            }
            po++;
        }

        //close the file
        inX.close();
        inY.close();
        inZ.close();
        inT.close();

        String2.log("PointSubsetScaled time=" + (System.currentTimeMillis() - time));
            //+ " cumReadTime=" + cumulativeReadTime);
        return bitSet;
    }

    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {
        PointSubsetScaled.verbose = true;

        //testLittleMethods
        

        //setup for tests
        FloatArray  x = new FloatArray( new float[]{ -180, -120,   0, 120, 120, 180});
        FloatArray  y = new FloatArray( new float[]{  -20,  -45,   0,  45,  45,  90});
        FloatArray  z = new FloatArray( new float[]{    0,    0,   0,   0,   5,   0});
        DoubleArray t = new DoubleArray(new double[]{1e8,   2e8, 3e8, 4e8, 5e8, 6e8});
        String dir = File2.getSystemTempDirectory();
        String name = "PointSubsetScaledTest1";
        PointSubsetScaled pointSubsetScaled = new PointSubsetScaled(dir + name + ".index", t);
        pointSubsetScaled.constructorX(x);
        pointSubsetScaled.constructorY(y);
        pointSubsetScaled.constructorZ(z);

        //get all
        BitSet bitSet = pointSubsetScaled.subset(-180, 180, -90, 90, 0, 5, 1e8, 6e8);
        Test.ensureEqual(bitSet.cardinality(), 6, "");

        //2 x's
        bitSet = pointSubsetScaled.subset(120, 120, -90, 90, 0, 5, -1, 1e10);
        Test.ensureEqual(bitSet.cardinality(), 2, "");
        Test.ensureTrue(bitSet.get(3), "");
        Test.ensureTrue(bitSet.get(4), "");

        //1 y
        bitSet = pointSubsetScaled.subset(-180, 180, -50, -40, 0, 5, 0, 1e10);
        Test.ensureEqual(bitSet.cardinality(), 1, "");
        Test.ensureTrue(bitSet.get(1), "");

        //1 z
        bitSet = pointSubsetScaled.subset(-180, 180, -90, 90, 4, 7, 0, 1e10);
        Test.ensureEqual(bitSet.cardinality(), 1, "");
        Test.ensureTrue(bitSet.get(4), "");

        //1 t
        bitSet = pointSubsetScaled.subset(-180, 180, -90, 90, 0, 5, 0, 1.5e8);
        Test.ensureEqual(bitSet.cardinality(), 1, "");
        Test.ensureTrue(bitSet.get(0), "");

        //nothing (time)
        bitSet = pointSubsetScaled.subset(-180, 180, -90, 90, 0, 5, 0, 1);
        Test.ensureEqual(bitSet.cardinality(), 0, "");

        pointSubsetScaled.close();


        //***test no variation
        //setup for tests
        x = new FloatArray( new float[]{ 1,1});
        y = new FloatArray( new float[]{ 2,2});
        z = new FloatArray( new float[]{ 3,3});
        t = new DoubleArray(new double[]{4,4});
        name = "PointSubsetScaledTest2";
        pointSubsetScaled = new PointSubsetScaled(dir + name + ".index", t);
        pointSubsetScaled.constructorX(x);
        pointSubsetScaled.constructorY(y);
        pointSubsetScaled.constructorZ(z);

        //get all
        bitSet = pointSubsetScaled.subset(0, 5, 0, 5, 0, 5, 0, 5);
        Test.ensureEqual(bitSet.cardinality(), 2, "");

        //reject based on x, y, z, t
        bitSet = pointSubsetScaled.subset(5, 10, 0, 5, 0, 5, 0, 5);
        Test.ensureEqual(bitSet.cardinality(), 0, "");
        bitSet = pointSubsetScaled.subset(0, 5, -1, 0, 0, 5, 0, 5);
        Test.ensureEqual(bitSet.cardinality(), 0, "");
        bitSet = pointSubsetScaled.subset(0, 5, 0, 5, 5, 10, 0, 5);
        Test.ensureEqual(bitSet.cardinality(), 0, "");
        bitSet = pointSubsetScaled.subset(0, 5, 0, 5, 0, 5, -1, 0);
        Test.ensureEqual(bitSet.cardinality(), 0, "");

        pointSubsetScaled.close();

        //*** test lots of data
        //all construction takes about 12 s; subset() takes 1.6 s
        int n = 10000000;
        IntArray ia = new IntArray(0, n);
        name = "PointSubsetScaledTest3";
        pointSubsetScaled = new PointSubsetScaled(dir + name + ".index", ia);
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        pointSubsetScaled.constructorX(ia); //0, 2, 4, ...
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        pointSubsetScaled.constructorY(ia); //0, 4, 8, ...
        for (int i = 0; i <= n; i++)
            ia.array[i] *= 2;
        pointSubsetScaled.constructorZ(ia); //0, 8, 16, ...

        //reject based on x, y, z, t
        bitSet = pointSubsetScaled.subset(1,1, 2,2, 4,4, 8,8);
        String2.log("  time should be ~ 1766");
        Test.ensureEqual(bitSet.cardinality(), 77, "");

        pointSubsetScaled.close();

        //*** test .nc file
        //Table table = new Table();
        //table.readNetcdfRows(String fullIndexName, ""
        //String loadColumns[], BitSet okRows) throws Exception {
        //table.subset();

        //done
        String2.log("\n***** PointSubsetScaled.main finished successfully");
        Math2.incgc(2000); //in a test

    }


}
