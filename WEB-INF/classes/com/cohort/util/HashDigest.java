/*
 * HashDigest Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package com.cohort.util;

import com.cohort.util.String2;
import com.cohort.util.File2;

import java.io.FileWriter;
import java.io.IOException;

/**
 * This generates a hash digest (e.g., MD5, SHA256) of a password or a file.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2016-03-09
 */
public class HashDigest {

    /** 
    HashCode is MurmurHash3 is from which is public domain code with no copyrights. From home page of
    https://github.com/aappleby/smhasher . 
    Java implementation modified from 
    https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
    */
    private static final int C1_32 = 0xcc9e2d51;
    private static final int C2_32 = 0x1b873593;
    private static final int R1_32 = 15;
    private static final int R2_32 = 13;
    private static final int M_32 = 5;
    private static final int N_32 = 0xe6546b64;
     /**
     * A default seed to use for the murmur hash algorithm.
     * Has the value {@code 104729}.
     */
    public static final int DEFAULT_SEED = 104729;


    /**
     * Generates 32-bit hash from the byte array with the given active length.
     * Modified slightly (offset is always 0. seed is always DEFAULT_SEED.)
     * from https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
     *
     * <p>This is an implementation of the 32-bit hash function {@code MurmurHash3_x86_32}
     * from from Austin Applyby's original MurmurHash3 {@code c++} code in SMHasher.</p>
     *
     * @param data The input byte array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final byte[] data, final int length) {
        int hash = DEFAULT_SEED;
        final int shift = 2; //because 4 bytes/int
        final int nblocks = length >> shift;

        // body
        for (int i = 0; i < nblocks; i++) {
            final int k = getLittleEndianInt(data, i << shift);
            hash = mix32(k, hash);
        }

        // tail
        final int index = nblocks << shift;
        int k1 = 0;
        switch (length - index) {
        case 3:
            k1 ^= (data[index + 2] & 0xff) << 16;
        case 2:
            k1 ^= (data[index + 1] & 0xff) << 8;
        case 1:
            k1 ^= (data[index] & 0xff);

            // mix functions
            k1 *= C1_32;
            k1 = Integer.rotateLeft(k1, R1_32);
            k1 *= C2_32;
            hash ^= k1;
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Generates 32-bit hash from the short array with the given active length.
     * Modified from murmur32 above.
     *
     * @param data The input short array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final short[] data, final int length) {
        int hash = DEFAULT_SEED;
        final int shift = 1; //because 2 shorts/int
        final int nblocks = length >> shift;

        // body
        for (int i = 0; i < nblocks; i++) {
            final int k = getLittleEndianInt(data, i << shift);
            hash = mix32(k, hash);
        }

        // tail
        final int index = nblocks << shift;
        if (length - index == 1) {
            int k1 = data[index] & 0xffff;

            // mix functions
            k1 *= C1_32;
            k1 = Integer.rotateLeft(k1, R1_32);
            k1 *= C2_32;
            hash ^= k1;
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Generates 32-bit hash from the char array with the given active length.
     * Modified from byte[] version above
     *
     * @param data The input char array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final char[] data, final int length) {
        int hash = DEFAULT_SEED;
        final int shift = 1; //because 2 char/int
        final int nblocks = length >> shift;

        // body
        for (int i = 0; i < nblocks; i++) {
            final int k = getLittleEndianInt(data, i << shift);
            hash = mix32(k, hash);
        }

        // tail
        final int index = nblocks << shift;
        if (length - index == 1) {
            int k1 = data[index];  //no 0xffff because already unsigned

            // mix functions
            k1 *= C1_32;
            k1 = Integer.rotateLeft(k1, R1_32);
            k1 *= C2_32;
            hash ^= k1;
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Generates 32-bit hash from the int array with the given active length.
     * Modified from murmur32 above.
     *
     * @param data The input int array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final int[] data, final int length) {
        int hash = DEFAULT_SEED;

        for (int i = 0; i < length; i++) {
            hash = mix32(data[i], hash);
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Generates 32-bit hash from the float array with the given active length.
     * Modified from murmur32 above.
     *
     * @param data The input float array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final float[] data, final int length) {
        int hash = DEFAULT_SEED;

        for (int i = 0; i < length; i++) {
            hash = mix32(Float.floatToIntBits(data[i]), hash);
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Generates 32-bit hash from the long array with the given active length.
     * Modified from murmur32 above.
     *
     * @param data The input long array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final long[] data, final int length) {
        int hash = DEFAULT_SEED;

        for (int i = 0; i < length; i++) {
            hash = mix32((int)data[i], hash);  //low int
            hash = mix32((int)(data[i] >> 16), hash); //high int
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Generates 32-bit hash from the double array with the given active length.
     * Modified from murmur32 above.
     *
     * @param data The input double array
     * @param length The active length of array
     * @return The 32-bit hash
     */
    public static int murmur32(final double[] data, final int length) {
        int hash = DEFAULT_SEED;

        for (int i = 0; i < length; i++) {
            final long tl = Double.doubleToLongBits(data[i]);
            hash = mix32((int)tl, hash);  //low int
            hash = mix32((int)(tl >> 16), hash); //high int
        }

        hash ^= length;
        return fmix32(hash);
    }

    /**
     * Gets the little-endian int from 4 bytes starting at the specified index.
     * From https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
     *
     * @param data The data
     * @param index The index
     * @return The little-endian int
     */
    private static int getLittleEndianInt(final byte[] data, final int index) {
        return ((data[index    ] & 0xff)      ) |
               ((data[index + 1] & 0xff) <<  8) |
               ((data[index + 2] & 0xff) << 16) |
               ((data[index + 3] & 0xff) << 24);
    }
    
    /**
     * Gets the little-endian int from 2 short starting at the specified index.
     * From https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
     *
     * @param data The data
     * @param index The index
     * @return The little-endian int
     */
    private static int getLittleEndianInt(final short[] data, final int index) {
        return ((data[index    ] & 0xffff)      ) |
               ((data[index + 1] & 0xffff) << 16);
    }
    
    /**
     * Gets the little-endian int from 2 chars starting at the specified index.
     * From https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
     *
     * @param data The data
     * @param index The index
     * @return The little-endian int
     */
    private static int getLittleEndianInt(final char[] data, final int index) {
        return (data[index    ]      ) |  //not & 0xffff because already unsigned
               (data[index + 1] << 16);
    }
    
    /**
     * Performs the intermediate mix step of the 32-bit hash function.
     * From https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
     *
     * @param k The data to add to the hash
     * @param hash The current hash
     * @return The new hash
     */
    private static int mix32(int k, int hash) {
        k *= C1_32;
        k = Integer.rotateLeft(k, R1_32);
        k *= C2_32;
        hash ^= k;
        return Integer.rotateLeft(hash, R2_32) * M_32 + N_32;
    }

    /**
     * Performs the final avalanche mix step of the 32-bit hash function.
     * From https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
     *
     * @param hash The current hash
     * @return The final hash
     */
    private static int fmix32(int hash) {
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        return hash;
    }

    public final static String usage = 
"Usage:\n" +
"To print a hash digest (checksum) of a password:\n" +
"  HashDigest password:myPassword type:algorithm\n" +
"To print a hash digest of a file:\n" +
"  HashDigest filename:myFileName type:algorithm\n" +
"To make a file with a hash digest of a file:\n" +
"  HashDigest filename:myFileName type:algorithm -file\n" +
"where algorithm can be MD5, SHA-1, or SHA-256.\n";

    /**
     * This tests this class.
     */
    public static void basicTest() throws Throwable {
        System.out.println("*** HashDigest.basicTest");
        String tName = String2.replaceAll(
            File2.getClassPath() + "com\\cohort\\util\\License.txt", "\\", "/");
        Test.ensureEqual(doIt(new String[]{"type:MD5"}), 
            "Neither password or filename was specified.\n" + usage, "");
        Test.ensureEqual(doIt(new String[]{"password:myPassword", "type:MD-5"}), 
            "Invalid algorithm.\n" + usage, "");
        Test.ensureEqual(doIt(new String[]{"password:myPassword", "type:MD5"}), 
            "deb1536f480475f7d593219aa1afd74c", "");
        Test.ensureEqual(doIt(new String[]{"password:myPassword", "type:SHA-1"}), 
            "5413ee24723bba2c5a6ba2d0196c78b3ee4628d1", "");
        Test.ensureEqual(doIt(new String[]{"password:myPassword", "type:SHA-256"}), 
            "76549b827ec46e705fd03831813fa52172338f0dfcbd711ed44b81a96dac51c6", "");
        Test.ensureEqual(doIt(new String[]{"filename:" + tName, "type:MD5"}), 
            "327fbb2aa6c6297d4fdd5fdf4b14e289", "");
        Test.ensureEqual(doIt(new String[]{"filename:" + tName, "type:SHA-256"}), 
            "e376c88953b2d56b00783fed071f1875e8ed94230f4e14eee5bce8bd608de5e6", "");
        Test.ensureEqual(doIt(new String[]{"filename:" + tName, "type:SHA-256", "-file"}), 
            "Created " + tName + ".sha256", "");
        Test.ensureEqual(File2.readFromFileUtf8(tName + ".sha256")[1],
            "e376c88953b2d56b00783fed071f1875e8ed94230f4e14eee5bce8bd608de5e6  License.txt\n", "");
        File2.delete(tName + ".sha256");
    }

    /**
     * This does the work.
     */
    public static String doIt(String args[]) throws Throwable {
        String algorithm = String2.stringStartsWith(args, "type:");
        if (algorithm == null) 
            return usage;
        algorithm = algorithm.substring(5);
        int whichAlgo = String2.indexOf(String2.FILE_DIGEST_OPTIONS, algorithm);
        if (whichAlgo < 0)
            return "Invalid algorithm.\n" + usage;

        String password = String2.stringStartsWith(args, "password:");
        if (password != null) 
            return String2.passwordDigest(algorithm, password.substring(9));

        String filename = String2.stringStartsWith(args, "filename:");
        if (filename == null) 
            return "Neither password or filename was specified.\n" + usage;
        filename = filename.substring(9);
        String digest = String2.fileDigest(algorithm, filename);
        if (String2.indexOf(args, "-file") >= 0) {
            String ext = String2.FILE_DIGEST_EXTENSIONS[whichAlgo];
            File2.writeToFileUtf8(filename + ext, 
                digest + "  " + File2.getNameAndExtension(filename));
            return "Created " + filename + ext; 
        } else {
            return digest;
        }
    }

    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ HashDigest.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) basicTest();
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }


    /**
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the question.
     */
    public static void main(String args[]) throws Throwable {
        System.out.println(doIt(args));
        System.exit(0);
    }

}