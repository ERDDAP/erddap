/* 
 * FileVisitorSubdir Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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

            /* 
            //THIS IS NOT YET WORKING and probably can't do any better than calling FileVisitorDNLS (see below)
            //Is it an S3 bucket with "files"?
            //If testing a "dir", url should have a trailing slash.
            //S3 gives precise file size and lastModified
            StringArray results = new StringArray();
            Matcher matcher = String2.AWS_S3_PATTERN.matcher(File2.addSlash(tDir)); //force trailing slash
            if (matcher.matches()) {
                //http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html
                //If files have file-system-like names, e.g., 
                //  url=http://bucketname.s3.region.amazonaws.com/  key=dir1/dir2/fileName.ext
                //  e.g., http://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_NorESM1-M_209601-209912.nc
                //  They are just object keys with internal slashes. 
                //So specify prefix in request.
                Pattern pathRegexPattern = Pattern.compile(tPathRegex);

                String bucketName = matcher.group(1); 
                String region     = matcher.group(2); 
                String prefix     = matcher.group(3); 
                String baseURL = tDir.substring(0, matcher.start(3));
                if (verbose) 
                    String2.log("FileVisitorSubdir.oneStep getting info from AWS S3 at" + 
                        "\nURL=" + tDir);
                        //"\nbucket=" + bucketName + " prefix=" + prefix);

                //This code is from https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingObjectKeysUsingJava.html
                //https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3ClientBuilder.html
                AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withRegion(region)  //com.amazonaws.regions.Regions.DEFAULT_REGION) //since I want code to be able to run from anywhere
                    .build();

                //I wanted to generate lastMod for dir based on lastMod of files
                //but it would be inconsistent for different requests (recursive, fileNameRegex).
                //so just a set of dir names.
                //https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/ListObjectsV2Request.html
                HashSet<String> dirHashSet = new HashSet(); 
                ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    //I read (not in these docs) 1000 is max. 
                    //For me, 1000 or 100000 returns ~1000 for first few responses, then ~50 for subsequent !
                    //so getting a big bucket's contents takes ~12 hours on a non-Amazon network
                    .withMaxKeys(10000) //be optimistic
                    .withDelimiter("/"); //Using it just gets files (and perhaps dirs) in this dir (not lower subdirs)
                ListObjectsV2Result result = null;
                int chunk = 0;

                try {
                    do {
                        chunk++;
                        int nTry = 0;
                        while (nTry <= 3) {
                            String msg = ">> calling s3client.listObjects chunk#" + chunk + " try#" + nTry;
                            try {
                                nTry++;
                                if (debugMode) String2.log(msg);
                                result = s3client.listObjectsV2(req);
                                break;  //success
                            } catch (Exception ev2) {
                                //try again
                                String2.log((debugMode? "" : msg + "\n") +
                                    "caught error (will try again):\n" + MustBe.throwableToString(ev2));
                                Math2.sleep(5000);
                            }
                        }

                        for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                            String keyFullName = objectSummary.getKey();
                            String keyDir = File2.getDirectory(baseURL + keyFullName);
                            String keyName = File2.getNameAndExtension(keyFullName);
                            boolean matchesPath = keyDir.startsWith(tDir) && //it should
                                ((keyDir.length() == tDir.length() ||
                                  pathRegexPattern.matcher(keyDir).matches()));
                            if (debugMode) String2.log(">> key=" + keyFullName);
                                //+ "\n>> matchesPathRegex=" + matchesPath);
                            if (matchesPath) {

                                //store this dir
                                //S3 only returns object keys. I must infer/collect directories.
                                //Store this dir and parents back to tDir.
                                String choppedKeyDir = keyDir;
                                while (choppedKeyDir.length() >= tDir.length()) {
                                    if (!dirHashSet.add(choppedKeyDir)) 
                                        break; //hash set already had this, so will already have parents

                                    //chop off last subdirectory
                                    choppedKeyDir = File2.getDirectory(
                                        choppedKeyDir.substring(0, choppedKeyDir.length() - 1)); //remove trailing /
                                }

                                //store this file's information
                                //Sometimes directories appear as files named "" with size=0.
                                //I don't store those as files.
                                if (debugMode) 
                                    String2.log(">> found dir=" + keyName);
                                if (keyName.endsWith("/")) 
                                    results.add(keyName);
                            }
                        }
                        String token = result.getNextContinuationToken();
                        req.setContinuationToken(token);

                    } while (result.isTruncated());

                } catch (AmazonServiceException ase) {
                    String msg = keepGoing + "AmazonServiceException: " + 
                        ase.getErrorType() + " ERROR, HTTP Code=" + ase.getStatusCode() + 
                        ": " + ase.getMessage();
                    String2.log(MustBe.throwable(msg, ase));
                    //Bob is testing: don't throw. Return what you have.
                    //throw new IOException(, ase);
                } catch (SdkClientException sce) {
                    String2.log(MustBe.throwable(keepGoing + sce.getMessage(), sce));
                    //Bob is testing: don't throw. Return what you have.
                    //throw new IOException(keepGoing + sce.getMessage(), sce);
                } catch (Exception e) {
                    throw new IOException(e);
                }

                //add directories to the table
                if (false) {
                    Iterator<String> it = dirHashSet.iterator();
                    while (it.hasNext()) 
                        results.add(it.next());
                }

                results.sortIgnoreCase();
                return results;

            } //end AWS S3
            /* */

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
            "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/",
            ""); 
        String results = alps.toNewlineString();
        String expected = 
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\n";
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
/* for releases, this line should have open/close comment */
        //always done        
        testLocal();
        testAWSS3();
        testWAF();

        //future: FTP?
    }


}
