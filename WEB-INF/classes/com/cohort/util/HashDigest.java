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
    public static void basicTest() throws Throwable {
        System.out.println("*** HashDigest.basicTest");
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