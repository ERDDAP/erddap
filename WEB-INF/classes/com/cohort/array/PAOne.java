/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;


/**
 * This class holds a PrimitiveArray which always has 1 value. 
 * This it is like Java's Number, but for PrimitiveArrays.
 * The constructors set a specific PAType, which PAOne uses for its entire life.
 */
public class PAOne {

    private PrimitiveArray pa;
    private int elementSize;

    /**
     * This constructs a paOne (with a value of 0) of the specified type.
     */
    public PAOne(PAType paType) {
        pa = PrimitiveArray.factory(paType, 1, true);
        elementSize = pa.elementSize();
    }

    /**
     * This constructs a paOne (with a value of 0) of the same type as the PrimitiveArray.
     */
    public PAOne(PrimitiveArray otherPA) {
        pa = PrimitiveArray.factory(otherPA.elementType(), 1, true);
        elementSize = pa.elementSize();
    }

    /**
     * This constructs a paOne with the specified value.
     */
    public PAOne(PrimitiveArray otherPA, int index) {
        pa = PrimitiveArray.factory(otherPA.elementType(), 1, true);
        pa.setFromPA(0, otherPA, index);
        elementSize = pa.elementSize();
    }

    /**
     * This constructs a paOne with the specified value.
     */
    public static PAOne fromDouble(double d) {
        PAOne paOne = new PAOne(PAType.DOUBLE);
        paOne.setDouble(d);
        return paOne;
    }

    /**
     * This constructs a paOne with the specified value.
     */
    public static PAOne fromFloat(float f) {
        PAOne paOne = new PAOne(PAType.FLOAT);
        paOne.setFloat(f);
        return paOne;
    }

    /**
     * This constructs a paOne with the specified value.
     */
    public static PAOne fromInt(int i) {
        PAOne paOne = new PAOne(PAType.INT);
        paOne.setInt(i);
        return paOne;
    }

    /**
     * This returns the PAType of this PAOne.
     */
    public PAType paType() {
        return pa.elementType();
    }

    /**
     * This sets this PAOne's value.
     *
     * @param s a String
     * @param this PAOne, for convenience
     */
    public PAOne setString(String s) {
        pa.setString(0, s);
        return this;
    }
    /**
     * Return a value from the array as a String (and the cohort missing value
     * appears as "", not a value).
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns (String.valueOf(ar[index])) 
     *   or "" (for missing value).
     *   If this PA is unsigned, this method retuns the unsigned value.
     */
    public String getString(int index) {
        return pa.getString(index);
    }

    /**
     * Return a value from the array as a String (and the cohort missing value
     * appears as a value, not "").
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns (String.valueOf(ar[index])).
     *   If this PA is unsigned, this method retuns the unsigned value.
     */
    public String getSimpleString(int index) {
        return pa.getSimpleString(index);
    }

    /**
     * This sets this PAOne's value.
     *
     * @param d a double
     * @param this PAOne, for convenience
     */
    public PAOne setDouble(double d) {
        pa.setDouble(0, d);
        return this;
    }

    /**
     * This gets this PAOne's value as a double.
     *
     * @return this PAOne's value as a double.
     */
    public double getDouble() {
        return pa.getDouble(0);
    }

    /**
     * This sets this PAOne's value.
     *
     * @param f a float
     * @param this PAOne, for convenience
     */
    public PAOne setFloat(float f) {
        pa.setFloat(0, f);
        return this;
    }

    /**
     * This gets this PAOne's value as a float.
     *
     * @return this PAOne's value as a float.
     */
    public float getFloat() {
        return pa.getFloat(0);
    }

    /**
     * This sets this PAOne's value.
     *
     * @param i an int
     * @param this PAOne, for convenience
     */
    public PAOne setInt(int i) {
        pa.setInt(0, i);
        return this;
    }

    /**
     * This gets this PAOne's value as an int.
     *
     * @return this PAOne's value as an int.
     */
    public float getInt() {
        return pa.getInt(0);
    }

    /**
     * This sets this PAOne's value from an element in otherPA.
     *
     * @param otherPA the source PAOne which must be of the same (or smaller) PAType.
     * @param index the source index in PAOne.
     * @param this PAOne, for convenience
     */
    public PAOne readFrom(PrimitiveArray otherPA, int index) {
        pa.setFromPA(0, otherPA, index);
        return this;
    }

    /**
     * This sets an element of otherPA from the value in this PAOne.
     *
     * @param otherPA the destination PAOne which must be of the same (or smaller) PAType.
     * @param index the destination index in PAOne.
     */
    public void writeTo(PrimitiveArray otherPA, int index) {
        otherPA.setFromPA(index, pa, 0);
    }

    /**
     * This adds this PAOne's value to the otherPA.
     *
     * @param otherPA the destination PAOne which must be of the same (or smaller) PAType.
     */
    public void addTo(PrimitiveArray otherPA) {
        otherPA.addFromPA(pa, 0);
    }

    
    /**
     * This compares this object's value to the specified object's value.
     *
     * @param otherPA the other PA which must be of the same (or smaller) PAType. 
     * @param index the index in otherPA
     * @return a negative integer (if this is less than Other), zero (if this is
     *   same as Other), or a positive integer (if this is greater than Other).
     *   Think "this - other".
     */
    public int compareTo(PrimitiveArray otherPA, int index) {
        return pa.compare(0, otherPA, index); 
    }

    /**
     * This compares this object's value to the other object's value.
     *
     * @param otherPA the other PA which must be of the same (or smaller) PAType. 
     * @param index the index in otherPA
     * @return a negative integer (if this is less than Other), zero (if this is
     *   same as Other), or a positive integer (if this is greater than Other).
     *   Think "this - other".
     */
    public int compareTo(PAOne otherPAOne) {
        return pa.compare(0, otherPAOne.pa, 0); 
    }

    /**
     * This test if this value equals the otherPA object's value.
     *
     * @param otherPA the other PA which must be of the same (or smaller) PAType. 
     * @param index the index in otherPA
     * @return true if they are equal.
     */
    public boolean equals(PrimitiveArray otherPA, int index) {
        return compareTo(otherPA, index) == 0; 
    }

    /**
     * This returns true if the values are equal.
     *
     * @param otherPA the other PA which must be of the same (or smaller) PAType. 
     * @return true if the values are equal.
     */
    public boolean equals(PAOne otherPAOne) {
        return compareTo(otherPAOne) == 0; 
    }

    /**
     * This returns true of the values are almost equal.
     *
     * @param precision 
     * @param otherPA the other PA which must be of the same (or smaller) PAType. 
     * @return true if the values are almost equal. 
     *   Only FLOAT and DOUBLE test 'almost'. Other classes do pure ==.
     */
    public boolean almostEqual(int precision, PAOne otherPAOne) {
        PAType type1 = pa.elementType();
        PAType type2 = otherPAOne.paType();
        if (type1 == PAType.DOUBLE || type2 == PAType.DOUBLE)
            return Math2.almostEqual(precision, pa.getDouble(0), otherPAOne.pa.getDouble(0));
        else if (type1 == PAType.FLOAT || type2 == PAType.FLOAT)
            return Math2.almostEqual(precision, pa.getFloat(0), otherPAOne.pa.getFloat(0));
        else return compareTo(otherPAOne) == 0;
    }

    /**
     * This returns a string representation of the value.
     */
    public String toString() {
        return pa.toString();
    }

    /**
     * This reads one number from the current position in the randomAccessFile.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile, which MUST have data of the same PAType as this PAOne.
     * @throws Exception if trouble
     */
    public void readFromRAF(RandomAccessFile raf) throws Exception {
        pa.clear();
        pa.readFromRAF(raf);
    }

    /**
     * This reads one number from a randomAccessFile.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile, which MUST have data of the same PAType as this PAOne.
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the desired value (0..)
     * @return this (for convenience)
     * @throws Exception if trouble
     */
    public PAOne readFromRAF(RandomAccessFile raf, long start, long index) throws Exception {
        raf.seek(start + elementSize * index);
        pa.clear();
        pa.readFromRAF(raf);
        return this;
    }

    /**
     * This writes the one number to a randomAccessFile at the current position.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile
     * @throws Exception if trouble
     */
    public void writeToRAF(RandomAccessFile raf) throws Exception {
        pa.writeToRAF(raf, 0);
    }

    /**
     * This writes the one number to a randomAccessFile.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the value (0..)
     * @throws Exception if trouble
     */
    public void writeToRAF(RandomAccessFile raf, long start, long index) throws Exception {
        raf.seek(start + elementSize * index);
        pa.writeToRAF(raf, 0);
    }

    /**
     * This writes the one number to a DataOutputStream.
     * This doesn't yet support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param dos the DataOutputStream
     * @throws Exception if trouble
     */
    public void writeToDOS(DataOutputStream dos) throws Exception {
        pa.writeDos(dos, 0);
    }


    /** This tests the methods in this class.
     */
    public static void test() throws Exception {
        String2.log("\n*** PAOne.test()");

        ByteArray  ba = new ByteArray( new byte[]{-128, 0, 127});
        ShortArray sa = new ShortArray(new short[]{-32768, 0, 32767});
        PAOne bo = new PAOne(sa, 0);   
        Test.ensureEqual(bo.toString(), "-32768", "");
        Test.ensureEqual(bo.compareTo(ba, 0), -1, "");
        bo.readFrom(sa, 1);              
        Test.ensureEqual(bo.toString(), "0", "");
        Test.ensureEqual(bo.compareTo(ba, 0), 1, "");
        Test.ensureEqual(bo.compareTo(sa, 0), 1, "");
        Test.ensureEqual(bo.compareTo(sa, 1), 0, "");
        Test.ensureEqual(bo.compareTo(sa, 2), -1, "");

        bo.addTo(sa);
        Test.ensureEqual(sa.toString(), "-32768, 0, 32767, 0", "");

        //raf test
        String raf2Name = File2.getSystemTempDirectory() + "PAOneTest.bin";
        String2.log("rafName=" + raf2Name);
        File2.delete(raf2Name);
        Test.ensureEqual(File2.isFile(raf2Name), false, "");

        RandomAccessFile raf2 = new RandomAccessFile(raf2Name, "rw");
        PAOne paOne = new PAOne(ba);
        long bStart = raf2.getFilePointer();
        for (int i = 0; i < 3; i++) {
            paOne.readFrom(ba, i);
            paOne.writeToRAF(raf2); //at current position
        }

        paOne = new PAOne(sa);
        long sStart = raf2.getFilePointer();
        for (int i = 0; i < 4; i++) {
            paOne.readFrom(sa, i);
            paOne.writeToRAF(raf2, sStart, i); //at specified position
        }

        paOne = new PAOne(sa);
        raf2.seek(sStart);
        for (int i = 0; i < 4; i++) {
            paOne.readFromRAF(raf2);  //at current position
            Test.ensureEqual(paOne, "" + sa.get(i), "i=" + i);
        }

        paOne = new PAOne(ba);
        raf2.seek(bStart);
        for (int i = 0; i < 3; i++) {
            paOne.readFromRAF(raf2);  //at current position
            Test.ensureEqual(paOne, "" + ba.get(i), "i=" + i);
        }

        paOne = new PAOne(ba);
        for (int i = 0; i < 3; i++) {
            paOne.readFromRAF(raf2, bStart, i);  //at specified position
            Test.ensureEqual(paOne, "" + ba.get(i), "i=" + i);
        }

        //test almostEqual
        
    }

}

