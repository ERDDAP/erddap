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
    public static void test() throws Throwable {
        System.out.println("*** Test HashDigest");
        String tName = String2.replaceAll(
            String2.getClassPath() + "com\\cohort\\util\\License.txt", "\\", "/");
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
        Test.ensureEqual(String2.readFromFile(tName + ".sha256")[1],
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
            String2.writeToFile(filename + ext, 
                digest + "  " + File2.getNameAndExtension(filename));
            return "Created " + filename + ext; 
        } else {
            return digest;
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