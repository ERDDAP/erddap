/*
 * HashDigest Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package com.cohort.util;

/**
 * This generates a hash digest (e.g., MD5, SHA256) of a password or a file.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2016-03-09
 */
public class HashDigest {

  /**
   * HashCode is MurmurHash3 is from which is public domain code with no copyrights. From home page
   * of https://github.com/aappleby/smhasher . Java implementation modified from
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
   */
  private static final int C1_32 = 0xcc9e2d51;

  private static final int C2_32 = 0x1b873593;
  private static final int R1_32 = 15;
  private static final int R2_32 = 13;
  private static final int M_32 = 5;
  private static final int N_32 = 0xe6546b64;

  /** A default seed to use for the murmur hash algorithm. Has the value {@code 104729}. */
  public static final int DEFAULT_SEED = 104729;

  /**
   * Generates 32-bit hash from the byte array with the given active length. Modified slightly
   * (offset is always 0. seed is always DEFAULT_SEED.) from
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
   *
   * <p>This is an implementation of the 32-bit hash function {@code MurmurHash3_x86_32} from from
   * Austin Applyby's original MurmurHash3 {@code c++} code in SMHasher.
   *
   * @param data The input byte array
   * @param length The active length of array
   * @return The 32-bit hash
   */
  public static int murmur32(final byte[] data, final int length) {
    int hash = DEFAULT_SEED;
    final int shift = 2; // because 4 bytes/int
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
        // fall through
      case 2:
        k1 ^= (data[index + 1] & 0xff) << 8;
        // fall through
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
   * Generates 32-bit hash from the short array with the given active length. Modified from murmur32
   * above.
   *
   * @param data The input short array
   * @param length The active length of array
   * @return The 32-bit hash
   */
  public static int murmur32(final short[] data, final int length) {
    int hash = DEFAULT_SEED;
    final int shift = 1; // because 2 shorts/int
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
   * Generates 32-bit hash from the char array with the given active length. Modified from byte[]
   * version above
   *
   * @param data The input char array
   * @param length The active length of array
   * @return The 32-bit hash
   */
  public static int murmur32(final char[] data, final int length) {
    int hash = DEFAULT_SEED;
    final int shift = 1; // because 2 char/int
    final int nblocks = length >> shift;

    // body
    for (int i = 0; i < nblocks; i++) {
      final int k = getLittleEndianInt(data, i << shift);
      hash = mix32(k, hash);
    }

    // tail
    final int index = nblocks << shift;
    if (length - index == 1) {
      int k1 = data[index]; // no 0xffff because already unsigned

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
   * Generates 32-bit hash from the int array with the given active length. Modified from murmur32
   * above.
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
   * Generates 32-bit hash from the float array with the given active length. Modified from murmur32
   * above.
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
   * Generates 32-bit hash from the long array with the given active length. Modified from murmur32
   * above.
   *
   * @param data The input long array
   * @param length The active length of array
   * @return The 32-bit hash
   */
  public static int murmur32(final long[] data, final int length) {
    int hash = DEFAULT_SEED;

    for (int i = 0; i < length; i++) {
      hash = mix32((int) data[i], hash); // low int
      hash = mix32((int) (data[i] >> 16), hash); // high int
    }

    hash ^= length;
    return fmix32(hash);
  }

  /**
   * Generates 32-bit hash from the double array with the given active length. Modified from
   * murmur32 above.
   *
   * @param data The input double array
   * @param length The active length of array
   * @return The 32-bit hash
   */
  public static int murmur32(final double[] data, final int length) {
    int hash = DEFAULT_SEED;

    for (int i = 0; i < length; i++) {
      final long tl = Double.doubleToLongBits(data[i]);
      hash = mix32((int) tl, hash); // low int
      hash = mix32((int) (tl >> 16), hash); // high int
    }

    hash ^= length;
    return fmix32(hash);
  }

  /**
   * Gets the little-endian int from 4 bytes starting at the specified index. From
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
   *
   * @param data The data
   * @param index The index
   * @return The little-endian int
   */
  private static int getLittleEndianInt(final byte[] data, final int index) {
    return (data[index] & 0xff)
        | ((data[index + 1] & 0xff) << 8)
        | ((data[index + 2] & 0xff) << 16)
        | ((data[index + 3] & 0xff) << 24);
  }

  /**
   * Gets the little-endian int from 2 short starting at the specified index. From
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
   *
   * @param data The data
   * @param index The index
   * @return The little-endian int
   */
  private static int getLittleEndianInt(final short[] data, final int index) {
    return (data[index] & 0xffff) | ((data[index + 1] & 0xffff) << 16);
  }

  /**
   * Gets the little-endian int from 2 chars starting at the specified index. From
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
   *
   * @param data The data
   * @param index The index
   * @return The little-endian int
   */
  private static int getLittleEndianInt(final char[] data, final int index) {
    return data[index]
        | // not & 0xffff because already unsigned
        (data[index + 1] << 16);
  }

  /**
   * Performs the intermediate mix step of the 32-bit hash function. From
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
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
   * Performs the final avalanche mix step of the 32-bit hash function. From
   * https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash3.java.html
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

  public static final String usage =
      "Usage:\n"
          + "To print a hash digest (checksum) of a password:\n"
          + "  HashDigest password:myPassword type:algorithm\n"
          + "To print a hash digest of a file:\n"
          + "  HashDigest filename:myFileName type:algorithm\n"
          + "To make a file with a hash digest of a file:\n"
          + "  HashDigest filename:myFileName type:algorithm -file\n"
          + "where algorithm can be MD5, SHA-1, or SHA-256.\n";

  /** This does the work. */
  public static String doIt(String args[]) throws Throwable {
    String algorithm = String2.stringStartsWith(args, "type:");
    if (algorithm == null) return usage;
    algorithm = algorithm.substring(5);
    int whichAlgo = String2.indexOf(String2.FILE_DIGEST_OPTIONS, algorithm);
    if (whichAlgo < 0) return "Invalid algorithm.\n" + usage;

    String password = String2.stringStartsWith(args, "password:");
    if (password != null) return String2.passwordDigest(algorithm, password.substring(9));

    String filename = String2.stringStartsWith(args, "filename:");
    if (filename == null) return "Neither password or filename was specified.\n" + usage;
    filename = filename.substring(9);
    String digest = String2.fileDigest(algorithm, filename);
    if (String2.indexOf(args, "-file") >= 0) {
      String ext = String2.FILE_DIGEST_EXTENSIONS[whichAlgo];
      File2.writeToFileUtf8(filename + ext, digest + "  " + File2.getNameAndExtension(filename));
      return "Created " + filename + ext;
    } else {
      return digest;
    }
  }

  /**
   * This is used when called from the command line. It explicitly calls System.exit(0) when done.
   *
   * @param args if args has values, they are used to answer the question.
   */
  public static void main(String args[]) throws Throwable {
    System.out.println(doIt(args));
    System.exit(0);
  }
}
