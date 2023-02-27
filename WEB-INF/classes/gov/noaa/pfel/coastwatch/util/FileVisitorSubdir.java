/* 
 * FileVisitorSubdir Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class gathers a list of subdir names (including initial dir).
 * This follows Linux symbolic links, but not Windows .lnk's 
 *   (see FileVistorDNLS.testSymbolicLinks() below).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2014-12-29
 */
public class FileVisitorSubdir extends SimpleFileVisitor<Path> {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 
    public static boolean debugMode = false; 

    public static String keepGoing = "FileVisitorSubdir caught an Exception but is continuing and returning the info it has: ";

    /** things set by constructor */
    public String dir;  //with \\ or / separators. With trailing slash (to match).
    private char fromSlash, toSlash;
    public String pathRegex;     //will be null if equivalent of .*
    public Pattern pathPattern;  //will be null if pathRegex is null

    public StringArray results = new StringArray();


    /** 
     * The constructor.
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     */
    public FileVisitorSubdir(String tDir, String tPathRegex) {
        super();
        dir = File2.addSlash(tDir);
        toSlash = dir.indexOf('\\') >= 0? '\\' : '/';
        fromSlash = toSlash == '/'? '\\' : '/';        

        pathRegex = tPathRegex == null || tPathRegex.length() == 0 || tPathRegex.equals(".*")?
            null : tPathRegex;
        pathPattern = pathRegex == null? null : Pattern.compile(pathRegex);
    }

    /** Invoked before entering a directory. */
    public FileVisitResult preVisitDirectory(Path tPath, BasicFileAttributes attrs)
        throws IOException {
        
        String ttDir = String2.replaceAll(tPath.toString(), fromSlash, toSlash) + toSlash;

        //skip because it doesn't match pathRegex?
        if (pathRegex != null && !pathPattern.matcher(ttDir).matches()) 
            return FileVisitResult.SKIP_SUBTREE;    

        results.add(ttDir);
        return FileVisitResult.CONTINUE;    
    }


    /** Invoked for a file that could not be visited. 
     *
     * @throws IOException if "Too many open files". 
     */
    public FileVisitResult visitFileFailed(Path file, IOException exc) 
        throws IOException {
        //2015-03-10 I added this method to override the superclass
        //  which apparently throws the exception and stops the parent
        //  SimpleFileVisitor. This class just ignores the error (unless "Too many open files").
        //  A one time test that this change solves the problem: call
        //    new FileVisitorSubdir("/") 
        //  on my Windows computer with message below enabled.
        //  Always show message here. It is useful information.     (message is just filename, or "Too many open files")
        //2021-02-16 I revised to throw exception if it is "Too many open files"
        String msg = exc.getMessage();
        String2.log("WARNING: FileVisitorSubdir.visitFileFailed: " + msg);
        if (msg.indexOf(Math2.TooManyOpenFiles) >= 0)
            throw exc;            
        return FileVisitResult.CONTINUE;    
    }

    /** 
     * A convenience method for using this class. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     * @return a StringArray with dir and subdir names 
     *   (with same slashes as tDir and with with the OS's slashes -- \\ for Windows!).
     * @throws IOException notably, if "Too many open files". 
     */
    public static StringArray oneStep(String tDir, String tPathRegex) throws IOException {
        long time = System.currentTimeMillis();

        tPathRegex = tPathRegex == null || tPathRegex.length() == 0? ".*": tPathRegex;

        //is this tDir an http url?  then use get info via FileVisitorDNLS
        if (tDir.matches(FileVisitorDNLS.HTTP_REGEX)) {

            Table table = FileVisitorDNLS.oneStep(tDir, 
                "/", //a regex that won't match any fileNames, but isn't null or ""
                true, tPathRegex, true); //tRecursive, tPathRegex, tDirectoriesToo
            StringArray dirs = (StringArray)table.getColumn(FileVisitorDNLS.DIRECTORY);
            return dirs;
        }            

        //do local file system
        //follow symbolic links: https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileVisitor.html
        //But this doesn't follow Windows symbolic link .lnk's:
        //  http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4237760
        FileVisitorSubdir fv = new FileVisitorSubdir(tDir, tPathRegex);
        Files.walkFileTree(FileSystems.getDefault().getPath(tDir), 
            EnumSet.of(FileVisitOption.FOLLOW_LINKS), //follow symbolic links
            Integer.MAX_VALUE,                        //maxDepth
            fv);
        if (verbose) String2.log("FileVisitorSubdir.oneStep finished successfully. n=" + 
            fv.results.size() + " time=" + (System.currentTimeMillis() - time) + "ms");
        return fv.results;
    }

    /** 
     * This tests a local file system. 
     */
    public static void testLocal() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.testLocal");
        verbose = true;
        String contextDir = File2.webInfParentDirectory(); //with / separator and / at the end
        StringArray alps;
        long time;

        //test forward slashes
        alps = oneStep(contextDir + "WEB-INF/classes/com/cohort", null);   //without trailing slash
        String results = alps.toNewlineString();
        String expected = 
"C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/com/cohort/\n" +
"C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/com/cohort/array/\n" +
"C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/com/cohort/ema/\n" +
"C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/com/cohort/util/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test backslashes 
        alps = oneStep(
            String2.replaceAll(contextDir + "WEB-INF/classes/com/cohort/", '/', '\\'), //with trailing slash
            null); 
        results = alps.toNewlineString();
        expected = String2.replaceAll(expected, '/', '\\');
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** FileVisitorSubdir.testLocal finished.");
    }

    /** 
     * This tests an Amazon AWS S3 file system.
     * Your S3 credentials must be in 
     * <br> ~/.aws/credentials on Linux, OS X, or Unix
     * <br> C:\Users\USERNAME\.aws\credentials on Windows
     * See https://docs.aws.amazon.com/sdk-for-java/?id=docs_gateway#aws-sdk-for-java,-version-1 .
     */
    public static void testAWSS3() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.testAWSS3");

        verbose = true;
        StringArray alps;
        long time;

        alps = oneStep(
            "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/",
            ""); 
        String results = alps.toNewlineString();
        String expected = 
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** FileVisitorSubdir.testAWSS3 finished.");

    }

    /** 
     * This tests a WAF and pathRegex.
     */
    public static void testWAF() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.testWAF");

        verbose = true;
        StringArray alps;
        long time;
        try {

        alps = oneStep("https://www.fisheries.noaa.gov/inportserve/waf/", //after 2020-08-03
            //"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/", //pre 2020-08-03
            ".*/NMFS/(|SWFSC/|NWFSC/)(|inport-xml/)(|xml/)"); //tricky!
        String results = alps.toNewlineString();
        String expected = 
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/inport-xml/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/inport-xml/xml/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/inport-xml/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/inport-xml/xml/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** FileVisitorSubdir.testWAF finished.");
        } catch (Exception e) {
            Test.knownProblem("2020-08-03 New 'directory' in new InPort system isn't a directory but a web page with other info.", e);
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
            lastTest = interactive? -1 : 2;
        String msg = "\n^^^ FileVisitorSubdir.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testLocal();
                    if (test ==  1) testAWSS3();
                    if (test ==  2) testWAF();

                    //FUTURE: FTP?
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


}
