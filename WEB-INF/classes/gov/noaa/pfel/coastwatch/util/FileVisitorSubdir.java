/* 
 * FileVisitorSubdir Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class gathers a list of subdir names (including initial dir).
 * This follows Linux symbolic links, but not Windows .lnk's 
 *   (see FileVistorDNLS.testSymbolicLinks() below).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2014-12-29
 */
public class FileVisitorSubdir extends SimpleFileVisitor<Path> {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 

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


    /** Invoked for a file that could not be visited. */
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        //2015-03-10 I added this method to override the superclass
        //which apparently throws the exception and stops the parent
        //SimpleFileVisitor. This class just ignores the error.
        //A one time test that this change solves the problem: call
        //    new FileVisitorSubdir("/") 
        //  on my Windows computer with message below enabled.
        //Always show message here. It is useful information.     (message is just filename)
        String2.log("WARNING: FileVisitorSubdir.visitFileFailed: " + exc.getMessage());
        return FileVisitResult.CONTINUE;    
    }

    /** 
     * A convenience method for using this class. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     * @return a StringArray with dir and subdir names 
     *   (with same slashes as tDir and with with the OS's slashes -- \\ for Windows!).
     */
    public static StringArray oneStep(String tDir, String tPathRegex) 
        throws IOException {
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
            fv.results.size() + " time=" + (System.currentTimeMillis() - time));
        return fv.results;
    }

    /** 
     * This tests a local file system. 
     */
    public static void testLocal() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.testLocal");
        verbose = true;
        String contextDir = SSR.getContextDirectory(); //with / separator and / at the end
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
     * See http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html .
     */
    public static void testAWSS3() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.testAWSS3");
        try {

        verbose = true;
        StringArray alps;
        long time;

        alps = oneStep(
            "http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/",
            ""); 
        String results = alps.toNewlineString();
        String expected = 
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** FileVisitorSubdir.testAWSS3 finished.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error.  (Did you create your AWS S3 credentials file?)"); 
        }
    }

    /** 
     * This tests a WAF and pathRegex.
     */
    public static void testWAF() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.testWAF");
        try {

        verbose = true;
        StringArray alps;
        long time;

        alps = oneStep("https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/",
            ".*/NMFS/(|SWFSC/|NWFSC/)(|inport/)(|xml/)"); //tricky!
        String results = alps.toNewlineString();
        String expected = 
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/inport/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/inport/xml/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/inport/\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/inport/xml/\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** FileVisitorSubdir.testWAF finished.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** FileVisitorSubdir.test() *****************\n");
/* */
        //always done        
        testLocal();
        testAWSS3();
        testWAF();

        //future: FTP?
    }


}
