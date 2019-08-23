/* 
 * FileVisitorDNLS Copyright 2014, NOAA.
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

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.Tally;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

/**
 * This class gathers basic information about a group of files.
 * This follows Linux symbolic links, but not Windows .lnk's (see testSymbolicLinks() below).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2014-11-25
 */
public class FileVisitorDNLS extends SimpleFileVisitor<Path> {

    /** 
     * These are thread-safe ways to recognize different types of servers. 
     * Use them in this order.
     */
    public final static String  AWS_S3_REGEX    = String2.AWS_S3_REGEX;
    /** If testing a "dir", url should have a trailing slash.*/
    public final static Pattern AWS_S3_PATTERN  = String2.AWS_S3_PATTERN;
    //test hyrax before tds because some hyrax offer a tds-like catalog
    public final static String  HYRAX_REGEX     = "https?://.+/opendap/.+";
    public final static Pattern HYRAX_PATTERN   = Pattern.compile(HYRAX_REGEX);
    public final static String  THREDDS_REGEX   = "https?://.+/thredds/catalog/.+";
    public final static Pattern THREDDS_PATTERN = Pattern.compile(THREDDS_REGEX);
    //works for any http service (e.g., WAF), so test last
    //FileVisitorSubdir also uses this.
    public final static String  HTTP_REGEX       = "https?://.+";
    public final static Pattern HTTP_PATTERN     = Pattern.compile(HTTP_REGEX);
    //not that . doesn't match line terminator characters!
    public final static String  TT_REGEX         = "<tt>.*</tt>";

    public static String keepGoing = "FileVisitorDNLS caught an Exception but is continuing and returning the info it has: ";

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 
    public static boolean debugMode = false; 

    /** The names of the columns in the table. */
    public final static String DIRECTORY    = "directory";
    public final static String NAME         = "name";
    public final static String LASTMODIFIED = "lastModified";
    public final static String SIZE         = "size";
    public final static String DNLS_COLUMN_NAMES[]      = {DIRECTORY, NAME,     LASTMODIFIED, SIZE};
    public final static String DNLS_COLUMN_TYPES_SSLL[] = {"String",  "String", "long",       "long"};

    public final static String URL          = "url"; //in place of directory for oneStepAccessibleViaFiles

    /** For working files. Set here but reset by EDStatic to be bigParentDir/dataset/_FileVisitorDNLS/ . */
    public static String FILE_VISITOR_DIRECTORY = null;
    static {
        try {
            FILE_VISITOR_DIRECTORY = File2.getSystemTempDirectory() + "_FileVisitor/";
        } catch (Exception e) {
            String2.log(String2.ERROR + " while creating _FileVisitor directory:\n" + 
                MustBe.throwableToString(e));
        }
    }

    /** pruneCache is a mini system to keep track of the total size of files in a cache
      and periodically prune the cache (remove old files) until total size is 
      some fraction (e.g. 0.75) of the max allowed size.
      It is a little loose/imprecise, but that's okay.
      E.g., it won't ever prune files that where lastMod time is less than PRUNE_CACHE_SAFE_MILLIS ago.

      Files with lastMod in the last n milliseconds are not removed from the cache. */
    public static int PRUNE_CACHE_SAFE_MILLIS = 20 * 1000; //20 seconds = 2*10 (10 seconds to decompress jplMur files)
    /** When threshold size is reached, prune the cache to fraction*threshold. 
        Normally, you won't change this, but will change the fraction in code calling pruneCache(). */
    public static double PRUNE_CACHE_DEFAULT_FRACTION = 0.75; 
    /** ConcurrentHashMap handles multi-threaded access well. */
    public static ConcurrentHashMap<String,Long> pruneCacheDirSize = new ConcurrentHashMap(); /* dirName, bytes */

    /** things set by constructor */
    public String dir;  //with \\ or / separators. With trailing slash (to match).
    private char fromSlash, toSlash;
    public String fileNameRegex;
    public String pathRegex;  //will be null if equivalent of .*
    public Pattern fileNamePattern; //from fileNameRegex
    public Pattern pathPattern;     //will be null if pathRegex is null
    public boolean recursive, directoriesToo;
    static boolean OSIsWindows = String2.OSIsWindows;
    public Table table;
    /** dirs will have \\ or / like original constructor tDir, and a matching trailing slash. */
    public StringArray directoryPA;
    public StringArray namePA;
    public LongArray   lastModifiedPA;
    public LongArray   sizePA;


    /** 
     * The constructor.
     * Usage: see useIt().
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing /.  
     *    The resulting dirPA will contain dirs with matching slashes.
     * @param pathRegex This is a regex to constrain which subdirectories to include.
     *   null or "" is treated as .* (i.e., match everything).
     * @param tDirectoriesToo
     */
    public FileVisitorDNLS(String tDir, String tFileNameRegex, boolean tRecursive,
        String tPathRegex, boolean tDirectoriesToo) {
        super();

        dir = File2.addSlash(tDir);
        toSlash = dir.indexOf('\\') >= 0? '\\' : '/';
        fromSlash = toSlash == '/'? '\\' : '/';        
        fileNameRegex = tFileNameRegex;
        fileNamePattern = Pattern.compile(fileNameRegex);
        recursive = tRecursive;
        pathRegex = tPathRegex == null || tPathRegex.length() == 0 || tPathRegex.equals(".*")?
            null : tPathRegex;
        pathPattern = pathRegex == null? null : Pattern.compile(pathRegex);
        directoriesToo = tDirectoriesToo;
        table = makeEmptyTable();
        directoryPA    = (StringArray)table.getColumn(DIRECTORY);
        namePA         = (StringArray)table.getColumn(NAME);
        lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
        sizePA         = (  LongArray)table.getColumn(SIZE);
    }

    /** Invoked before entering a directory. */
    public FileVisitResult preVisitDirectory(Path tDir, BasicFileAttributes attrs)
        throws IOException {

        String ttDir = String2.replaceAll(tDir.toString(), fromSlash, toSlash) + toSlash;
        if (ttDir.equals(dir)) {
            if (debugMode) String2.log(">> initial dir");
            return FileVisitResult.CONTINUE;
        }

        //skip because it doesn't match pathRegex?
        if (pathPattern != null && !pathPattern.matcher(ttDir).matches()) {
            if (debugMode) String2.log(">> doesn't match pathRegex: " + ttDir + " regex=" + pathRegex);
            return FileVisitResult.SKIP_SUBTREE;    
        }

        if (directoriesToo) {
            directoryPA.add(ttDir);
            namePA.add("");
            lastModifiedPA.add(attrs.lastModifiedTime().toMillis());
            sizePA.add(0);
        }

        if (debugMode) String2.log(">> recursive=" + recursive + " dir=" + ttDir);
        return recursive?
            FileVisitResult.CONTINUE : 
            FileVisitResult.SKIP_SUBTREE;    
    }

    /** Invoked for a file in a directory. */
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
        throws IOException {

        int oSize = directoryPA.size();
        try {
            String name = file.getFileName().toString();
            if (!fileNamePattern.matcher(name).matches()) {
                if (debugMode) String2.log(">> fileName doesn't match: name=" + name + " regex=" + fileNameRegex);
                return FileVisitResult.CONTINUE;    
            }

            //getParent returns \\ or /, without trailing /
            String ttDir = String2.replaceAll(file.getParent().toString(), fromSlash, toSlash) +
                toSlash;
            if (debugMode) String2.log(">> add fileName: " + ttDir + name);
            directoryPA.add(ttDir);
            namePA.add(name);
            lastModifiedPA.add(attrs.lastModifiedTime().toMillis());
            sizePA.add(attrs.size());
            //for debugging only:
            //String2.log(ttDir + name + 
            //    " mod=" + attrs.lastModifiedTime().toMillis() +
            //    " size=" + attrs.size());
        } catch (Throwable t) {
            if (directoryPA.size()    > oSize) directoryPA.remove(oSize);
            if (namePA.size()         > oSize) namePA.remove(oSize);
            if (lastModifiedPA.size() > oSize) lastModifiedPA.remove(oSize);
            if (sizePA.size()         > oSize) sizePA.remove(oSize);
            String2.log(MustBe.throwableToString(t));
        }
    
        return FileVisitResult.CONTINUE;    
    }

    /** Invoked for a file that could not be visited. */
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        //2015-03-10 I added this method to override the superclass
        //which apparently throws the exception and stops the parent
        //SimpleFileVisitor. This class just ignores the error.
        //Partial test that this change solves the problem: call
        //    new FileVisitorSubdir("/") 
        //  on my Windows computer with message analogous to below enabled.
        //  It shows several files where visitFileFailed.
        //Always show message here. It is useful information.     (message is just filename)
        String2.log("WARNING: FileVisitorDNLS.visitFileFailed: " + exc.getMessage());
        return FileVisitResult.CONTINUE;    
    }

    /** table.dataToString(); */
    public String resultsToString() {
        return table.dataToString();
    }

    /**
     * This returns an empty table with columns suitable for the instance table or oneStep.
     */
    public static Table makeEmptyTable() {
        Table table = new Table();
        table.addColumn(DIRECTORY,    new StringArray());
        table.addColumn(NAME,         new StringArray());
        table.addColumn(LASTMODIFIED, new LongArray());
        table.addColumn(SIZE,         new LongArray());
        return table;
    }

    /**
     * This makes an empty table with urls (not names) and doubles and metadata. 
     */
    public static Table makeEmptyTableWithUrlsAndDoubles() {
        //"url,name,lastModified,size\n" +
        //"http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,1.421272444E9,46482.0\n" +
        Table sourceTable = new Table();
        sourceTable.addColumn(0,  FileVisitorDNLS.URL,          new StringArray(), new Attributes()
            .add("ioos_category", "Identifier")
            .add("long_name",     "URL"));
        sourceTable.addColumn(1,  FileVisitorDNLS.NAME,         new StringArray(), new Attributes()
            .add("ioos_category", "Identifier")
            .add("long_name",     "File Name"));
        sourceTable.addColumn(2,  FileVisitorDNLS.LASTMODIFIED, new DoubleArray(), new Attributes()
            .add("ioos_category", "Time")
            .add("long_name",     "Last Modified")
            .add("units",         "seconds since 1970-01-01T00:00:00Z"));
        sourceTable.addColumn(3,  FileVisitorDNLS.SIZE,         new DoubleArray(), new Attributes()
            .add("ioos_category", "Other")
            .add("long_name",     "Size")
            .add("units",         "bytes"));
        return sourceTable;
    }


    /**
     * This is a convenience method for using this class. 
     * <p>This works with Amazon AWS S3 bucket URLs. Internal /'s in the keys will be
     * treated as folder separators. If there aren't any /'s, all the keys will 
     * be in the root directory.
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     *    The resulting directoryPA will contain dirs with matching slashes and trailing slash.
     * @param tPathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @param tDirectoriesToo if true, each directory name will get its own rows 
     *   in the results.
     * @return a table with columns with DIRECTORY, NAME,
     *    LASTMODIFIED (long, epochMilliseconds), and SIZE (long, bytes) columns.
     *    For LASTMODIFIED and SIZE, for directories or when the values
     *    are otherwise unknown, the value will be Long.MAX_VALUE.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's file NAME will be "".
     * @throws IOException if trouble
     */
    public static Table oneStep(String tDir, String tFileNameRegex, boolean tRecursive,
        String tPathRegex, boolean tDirectoriesToo) throws IOException {
        long time = System.currentTimeMillis();
        if (!String2.isSomething(tPathRegex))
            tPathRegex = ".*";

        //is tDir an http URL?
        if (tDir.matches(FileVisitorDNLS.HTTP_REGEX)) {

            //Is it an S3 bucket with "files"?
            //If testing a "dir", url should have a trailing slash.
            //S3 gives precise file size and lastModified
            Matcher matcher = AWS_S3_PATTERN.matcher(File2.addSlash(tDir)); //force trailing slash
            if (matcher.matches()) {
                //it matches with /, so actually add it (if not already there)
                tDir = File2.addSlash(tDir);

                //http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html
                //If files have file-system-like names, e.g., 
                //  url=http://bucketname.s3.region.amazonaws.com/  key=dir1/dir2/fileName.ext
                //  e.g., http://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_NorESM1-M_209601-209912.nc
                //  They are just object keys with internal slashes. 
                //So specify prefix in request.
                Pattern fileNameRegexPattern = Pattern.compile(tFileNameRegex);
                Pattern pathRegexPattern = Pattern.compile(tPathRegex);
                Table table = makeEmptyTable();
                StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                StringArray namePA         = (StringArray)table.getColumn(NAME);
                LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                //results may be slow (>12 hours) and huge (>10GB).
                //So, if >10000 files, accumulate results to a temporary jsonlCSV file 
                //(not in memory) and read when done.
                String dnlsFileName = FILE_VISITOR_DIRECTORY +
                    String2.modifyToBeFileNameSafe(tDir) + 
                    Calendar2.getCompactCurrentISODateTimeStringLocal() + "_" + 
                    Math2.random(1000) + ".jsonlCsv";
                boolean writtenToFile = false;

                String bucketName = matcher.group(1); 
                String region     = matcher.group(2); 
                String prefix     = matcher.group(3); 
                String baseURL = tDir.substring(0, matcher.start(3));
                if (verbose) 
                    String2.log("FileVisitorDNLS.oneStep getting info from AWS S3 at" + 
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
                    //For me, 1000 or 100000 returns ~1000 for first few responses, then ~100 for subsequent !
                    //so getting a big bucket's contents takes ~12 hours on a non-Amazon network
                    .withMaxKeys(10000); //be optimistic

                if (!tRecursive)
                    req = req.withDelimiter("/"); //Using it just gets files in this dir (not subdir)

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

                        //get common prefixes
                        if (tDirectoriesToo) {
                            List<String> list = result.getCommonPrefixes();
                            int tn = list.size();
                            for (int i = 0; i < tn; i++) {
                                String td2 = baseURL + list.get(i); //list.get(i)= e.g., BCSD/
                                dirHashSet.add(td2);
                                //String2.log(">> add dir=" + td2);
                            }                          
                        }

                        for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                            String keyFullName = objectSummary.getKey();
                            String keyDir = File2.getDirectory(baseURL + keyFullName);
                            String keyName = File2.getNameAndExtension(keyFullName);
                            boolean matchesPath = keyDir.startsWith(tDir) && //it should
                                ((keyDir.length() == tDir.length() ||
                                  tRecursive && pathRegexPattern.matcher(keyDir).matches()));
                            if (debugMode) String2.log(">> key=" + keyFullName);
                                //+ "\n>> matchesPathRegex=" + matchesPath);
                            if (matchesPath) {

                                //store this dir
                                if (tDirectoriesToo) {
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
                                }

                                //store this file's information
                                //Sometimes directories appear as files named "" with size=0.
                                //I don't store those as files.
                                boolean matches = keyName.length() > 0 && fileNameRegexPattern.matcher(keyName).matches();
                                //if (debugMode) String2.log(">> matchesFileNameRegex=(" + tFileNameRegex + ")=" + matches);
                                if (matches) {
                                    directoryPA.add(keyDir);
                                    namePA.add(keyName);
                                    lastModifiedPA.add(objectSummary.getLastModified().getTime()); //epoch millis
                                    sizePA.add(objectSummary.getSize()); //long
                                }
                            }
                        }
                        String token = result.getNextContinuationToken();
                        req.setContinuationToken(token);

                        //write a chunk to file?
                        if ((result.isTruncated() && table.nRows() > 10000) ||  //write a chunk
                            (!result.isTruncated() && writtenToFile)) {         //write final chunk
                            if (!writtenToFile)
                                File2.makeDirectory(File2.getDirectory(dnlsFileName));  //ensure dir exists
                            table.writeJsonlCSV(dnlsFileName, writtenToFile? true : false); //append
                            table.removeAllRows();
                            writtenToFile = true;
                        }

                    } while (result.isTruncated());

                    //read the file
                    if (writtenToFile) {
                        table = new Table();
                        table.readJsonlCSV(dnlsFileName, null, null, false); //simplify
                        int col = table.findColumnNumber(LASTMODIFIED);
                        table.setColumn(col, new LongArray(table.getColumn(col)));
                        col = table.findColumnNumber(SIZE);
                        table.setColumn(col, new LongArray(table.getColumn(col)));
                        //if no error:
                        File2.delete(dnlsFileName);
                    } //else use table as is

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
                if (tDirectoriesToo) {
                    Iterator<String> it = dirHashSet.iterator();
                    while (it.hasNext()) {
                        directoryPA.add(it.next());
                        namePA.add("");
                        lastModifiedPA.add(Long.MAX_VALUE); 
                        sizePA.add(Long.MAX_VALUE);
                    }
                }

                table.leftToRightSortIgnoreCase(2);
                return table;

            }

            //HYRAX before THREDDS
            //http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/
            //hyrax displays precise size and lastModified
            matcher = HYRAX_PATTERN.matcher(tDir); 
            if (matcher.matches()) {
                try {
                    if (verbose) 
                        String2.log("FileVisitorDNLS.oneStep getting info from Hyrax at" + 
                            "\nURL=" + tDir);
                    Table table = makeEmptyTable();
                    StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                    StringArray namePA         = (StringArray)table.getColumn(NAME);
                    LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                    LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                    DoubleArray lastModDA = new DoubleArray();
                    addToHyraxUrlList(tDir, tFileNameRegex, tRecursive, tPathRegex,
                        tDirectoriesToo, namePA, lastModDA, sizePA);
                    int n = namePA.size();
                    for (int i = 0; i < n; i++) {
                        String fn = namePA.get(i);
                        directoryPA.add(File2.getDirectory(fn));
                        namePA.set(i, File2.getNameAndExtension(fn));
                        lastModifiedPA.add(Math2.roundToLong(lastModDA.get(i) * 1000.0)); //to long epochMillis
                    }

                    table.leftToRightSortIgnoreCase(2);
                    return table;
                } catch (Throwable t) {
                    throw new IOException(t.getMessage(), t);
                }
            }

            //THREDDS
            //https://thredds.jpl.nasa.gov/thredds/catalog/ncml_aggregation/Chlorophyll/modis/catalog.html
            //thredds displays sizes as e.g., 24.10 Kbytes
            matcher = THREDDS_PATTERN.matcher(tDir); 
            if (matcher.matches()) {
                try {
                    if (verbose) 
                        String2.log("FileVisitorDNLS.oneStep getting info from THREDDS at" + 
                            "\nURL=" + tDir);
                    Table table = makeEmptyTable();
                    StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                    StringArray namePA         = (StringArray)table.getColumn(NAME);
                    LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                    LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                    DoubleArray lastModDA = new DoubleArray();
                    addToThreddsUrlList(tDir, tFileNameRegex, tRecursive, tPathRegex,
                        tDirectoriesToo, namePA, lastModDA, sizePA);
                    int n = namePA.size();
                    for (int i = 0; i < n; i++) {
                        String fn = namePA.get(i);
                        directoryPA.add(File2.getDirectory(fn));
                        namePA.set(i, File2.getNameAndExtension(fn));
                        lastModifiedPA.add(Math2.roundToLong(lastModDA.get(i) * 1000.0));  //to long epochMillis
                    }

                    table.leftToRightSortIgnoreCase(2);
                    return table;
                } catch (Throwable t) {
                    throw new IOException(t.getMessage(), t);
                }
            }

            //default: Apache-style WAF
            //WAF displays sizes as e.g., 24.10 Kbytes or precise
            //Similarly, time may be precise or to nearest minute
            try {
                if (verbose) 
                    String2.log("FileVisitorDNLS.oneStep getting info from Apache-style WAF at" + 
                        "\nURL=" + tDir);
                Table table = makeEmptyTable();
                StringArray directorySA = (StringArray)table.getColumn(DIRECTORY);
                StringArray nameSA      = (StringArray)table.getColumn(NAME);
                LongArray   lastModLA   = (  LongArray)table.getColumn(LASTMODIFIED); //epochMillis
                LongArray   sizeLA      = (  LongArray)table.getColumn(SIZE);

                addToWAFUrlList(tDir, tFileNameRegex, tRecursive, tPathRegex,
                    tDirectoriesToo, directorySA, nameSA, lastModLA, sizeLA);
                table.leftToRightSortIgnoreCase(2);
                return table;
            } catch (Throwable t) {
                throw new IOException(t.getMessage(), t);
            }
        }

        //local files
        //follow symbolic links: https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileVisitor.html
        //But this doesn't follow Windows symbolic link .lnk's:
        //  http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4237760
        FileVisitorDNLS fv = new FileVisitorDNLS(tDir, tFileNameRegex, tRecursive, 
            tPathRegex, tDirectoriesToo);
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(FileSystems.getDefault().getPath(tDir), 
            opts,               //follow symbolic links
            Integer.MAX_VALUE,  //maxDepth
            fv);
        fv.table.leftToRightSortIgnoreCase(2);
        if (verbose) String2.log("FileVisitorDNLS.oneStep(" + tDir + ") finished successfully. n=" + 
            fv.directoryPA.size() + " time=" +
            (System.currentTimeMillis() - time) + "ms" 
            //+ (debugMode? "\n" + MustBe.stackTrace() : "")
            );
        return fv.table;
    }

    /**
     * This is a variant of oneStep that makes it look like all the files
     * in the cacheFromUrl are actually in tDir.
     * 
     * @param cacheFromUrl remote dir ending in slash
     * @param tDir local dir ending in slash
     * @param tPathRegex must work for local and remote dir names, so will start with
     *    ".* /someDir/" (ha! without space in middle which is needed here to not close this javadoc).
     * @return a DNLS table as if local dir had all the files.
     * @throws Exception if trouble (e.g., unable to connect to Url). 
     *   (0 files is not error)
     */
    public static Table oneStepCache(String cacheFromUrl, 
        String tDir, String tFileNameRegex, boolean tRecursive,
        String tPathRegex, boolean tDirectoriesToo) throws IOException {

        //get remote info from cacheFromUrl
        cacheFromUrl = File2.addSlash(cacheFromUrl);
        tDir         = File2.addSlash(tDir);
        Table table = oneStep(cacheFromUrl, tFileNameRegex, tRecursive, 
            tPathRegex, tDirectoriesToo); //throws IOException

        //convert to local dir 
        StringArray pa = (StringArray)table.getColumn(DIRECTORY);
        int size = pa.size();
        int cacheFromUrlLength = cacheFromUrl.length();
        for (int i = 0; i < size; i++) 
            pa.set(i, tDir + pa.get(i).substring(cacheFromUrlLength));
        return table;
    }

    /**
     * If the specified local file isn't in the cache, download it.
     * This is thread-safe -- if 2 threads want the same file, one will get it 
     * while the other waits then sees that it already exists.
     *
     * <p>This calls incrementPruneCacheDirSize.
     * 
     * @param cacheFromUrl remote dir ending in slash
     * @param localDir local dir ending in slash
     * @param localFullName must start with localDir.
     * @return 0 if already in localDir or the file's size if this downloads it. 
     *    In either case, the local files lastMod time will be set to 'now'.
     */
    public static long ensureInCache(String cacheFromUrl, String localDir, 
        String localFullName) throws Exception {

        //synchronize on canonical localFullName -- so only 1 thread works on this file
        localFullName = String2.canonical(localFullName);
        synchronized (localFullName) {
            //localFullName may have been downloaded by another thread while this thread
            //waited for synch lock.
            if (RegexFilenameFilter.touchFileAndRelated(localFullName)) { //returns true if localFullName exists
                if (debugMode) String2.log("ensureInCache: local file ALREADY exists: " + localFullName);
                return 0;
            } 
            String remoteFullName = cacheFromUrl + localFullName.substring(localDir.length());
            SSR.downloadFile("ensureInCache", remoteFullName, localFullName, true); //tryToUseCompression, throws Exception
            //mark it as recently used 
            //Windows gives it a time of 10 minutes ago(!), which causes problems! 
            File2.touch(localFullName); //no point in trying to touchFileAndRelated -- the related don't exist yet
            long fl = File2.length(localFullName);
            incrementPruneCacheDirSize(localDir, fl); 
            return fl;
        }
    }


    /** 
     * If sourceFullName isDecompressible, this decompresses it in 
     * cacheDir (if not already there) and returns cacheFullName.
     *
     * <p>This calls pruneCache if needed. 
     * This calls incrementPruneCacheDirSize to add bytes if file is decompressed into cacheDir.
     *
     * @param sourceFullName the full name of the possibly compressed source file
     * @param sourceBaseDir the start of the sourceFullName which will be replaced by the cacheDir.
     *    It must have a slash at the end.
     * @param cacheDir the dir (for ERDDAP, the edd.decompressedDirectory()) 
     *    in which to store decompressed file.      
     *    It must have a slash at the end.
     * @param pruneCacheWhenGB is the number of GB in cache that triggers pruning
     * @param reuseExisting If true, this reuses an existing decompressed file of the same name.
     *    This is usually true, but false for generateDatasetsXml.
     * @return cacheFullName (if decompressed there) or sourceFullName (if source not compressed).
     *    This is smart and uses same subdirectory system.
     * @throws Exception of various types if trouble
     */
    public static String decompressIfNeeded(String sourceFullName, 
        String sourceBaseDir, String cacheDir,
        int pruneCacheWhenGB, boolean reuseExisting) throws Exception {
        //        !isDecompressible?  then nothing needs to be done
        if (!File2.isDecompressible(sourceFullName))
            return sourceFullName;

        //make destination cacheFullName
        if (!sourceFullName.startsWith(sourceBaseDir))
            throw new SimpleException(
                "When decompressing, sourceFullName=" + sourceFullName + 
                " must start with sourceBaseDir=" + sourceBaseDir + " .");
        String cacheFullName = cacheDir + sourceFullName.substring(sourceBaseDir.length()); 
        int cfnl = cacheFullName.length();
        if (cacheFullName.endsWith(".tar.gz"))
            cacheFullName = cacheFullName.substring(0, cfnl - 7);
        else if (cacheFullName.endsWith(".tar.gzip"))
            cacheFullName = cacheFullName.substring(0, cfnl - 9);
        else cacheFullName = File2.removeExtension(cacheFullName); //remove simple extension
        
        //decompressed file already exists? 
        if (!reuseExisting)
            File2.delete(cacheFullName);
        else if (File2.isFile(cacheFullName)) {
            File2.touch(cacheFullName); //ignore whether successful or not
            return cacheFullName; 
        }

        //pruneCache?     safer to prune ahead of time        
        pruneCache(cacheDir, pruneCacheWhenGB * Math2.BytesPerGB, PRUNE_CACHE_DEFAULT_FRACTION);

        //decompress the file
        cacheFullName = String2.canonical(cacheFullName); //so different threads will synch on same object
        synchronized (cacheFullName) { 
            //check again (since waiting for synch lock): decompressed file already exists? 
            if (File2.isFile(cacheFullName)) {
                File2.touch(cacheFullName); //ignore whether successful or not
                return cacheFullName; 
            }

            //make dir and decompressed file
            long time = System.currentTimeMillis();
            File2.makeDirectory(File2.getDirectory(cacheFullName));
            InputStream is = File2.getDecompressedBufferedInputStream(sourceFullName);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(cacheFullName));
                if (!File2.copy(is, os))
                    throw new IOException("Unable to decompress " + sourceFullName);
                if (verbose) String2.log("  decompressed " + sourceFullName + "  time=" + 
                    (System.currentTimeMillis() - time) + "ms");
            } finally {
                try { if (os != null) os.close(); } catch (Exception e2) {}
                try { if (is != null) is.close(); } catch (Exception e2) {}
            }
            long cs = incrementPruneCacheDirSize(cacheDir, Math.max(0, File2.length(cacheFullName)));
            if (reallyVerbose) String2.log("  decompressIfNeeded finished. time=" + 
                (System.currentTimeMillis() - time) + "ms cacheSize=" + (cs/Math2.BytesPerMB) + "MB" + 
                "\n    from " + sourceFullName + 
                "\n      to " + cacheFullName); 
        }
        return cacheFullName;
    }

    /**
     * This gets a directory's size in the pruneCacheDirSize hashmap.
     *
     * @param dir  It should already have slash at end
     * @return sizeB total size of files, in bytes, or -1 if unknown
     */
    public static long getPruneCacheDirSize(String dir) {
        Long tl = pruneCacheDirSize.get(dir);
        return tl == null? -1 : tl.longValue();
    }

    /**
     * This sets a directory's size in the pruneCacheDirSize hashmap.
     *
     * @param dir It should already have slash at end.
     * @param sizeB total size of files, in bytes
     */
    public static void setPruneCacheDirSize(String dir, long sizeB) {
        pruneCacheDirSize.put(dir, new Long(sizeB));
    }

    /**
     * This increments a directory's size in the pruneCacheDirSize hashmap.
     *
     * @param dir If this isn't yet in pruneCacheDirSize, this method will add it (and assume initial size=0). 
     *    It should already have slash at end.
     * @param addB total size of files, in bytes
     */
    public static long incrementPruneCacheDirSize(String dir, long addB) {
        Long tl = pruneCacheDirSize.get(dir);
        long tll = (tl == null? 0 : tl.longValue()) + addB;
        pruneCacheDirSize.put(dir, new Long(tll));
        return tll;
    }

    /** 
     * If currentCacheSizeB &gt;= thresholdCacheSizeB, this delete files (starting 
     * with oldest lastMod) until new currentCacheSizeB &lt; fraction*thresholdCacheSizeB.
     * This gets currentCacheSizeB from this class' hashmap.
     * This won't delete a file &lt; PRUNE_CACHE_SAFE_SECONDS (10?) old.
     * This won't throw an Exception.
     *
     * <p>currentCacheSizeB (the current sum of all file sizes, in bytes) 
     *   is gotten from getPruneCacheDirSize.
     *   You can get actual currentCacheSizeB by calling this method with 
     *     &lt;0 (e.g., -1).
     *   If &lt;0 initially or if pruning done, this calculates the actual size 
     *   and saves size via setPruneCacheDirSize.
     *
     * @param cacheDir  The local cache directory. It should already have trailing slash.
     *   If it doesn't exist, nothing is done and this returns 0.
     * @param thresholdCacheSizeB the threshold value of currentCacheSizeB that
     *   triggers pruning until it is &lt; fraction*thresholdCacheSizeB.
     * @param fraction  e.g., 0.75
     * @return the new currentCacheSizeB (as freshly recalculated so no risk of drift)
     */
    public static long pruneCache(String cacheDir, long thresholdCacheSizeB, double fraction) {
        
        if (!File2.isDirectory(cacheDir)) 
            return 0;
        long currentCacheSizeB = getPruneCacheDirSize(cacheDir); //-1 if not known
        if (currentCacheSizeB >= 0 && currentCacheSizeB < thresholdCacheSizeB)
            return currentCacheSizeB;

        //synchronize on canonical cacheDir -- so only 1 thread works on this dir
        cacheDir = String2.canonical(cacheDir);
        synchronized (cacheDir) {
            try {
                //never delete if <PRUNE_CACHE_SAFE_SECONDS old
                long goal = Math2.roundToLong(fraction * thresholdCacheSizeB);
                if (reallyVerbose) String2.log(
                    "& pruneCache " + cacheDir + 
                    " originalCacheSizeMB=" + currentCacheSizeB/Math2.BytesPerMB + 
                    " goalMB=" + goal/Math2.BytesPerMB);
                Table table = oneStep(cacheDir, ".*", true, ".*", false); //recursive=true, tDirectoriesToo=false, throws Exception
                //note time as of right after oneStep
                long originalTimeSafe = System.currentTimeMillis() - PRUNE_CACHE_SAFE_MILLIS; 
                int nRows = table.nRows();
                table.sort(new String[]{LASTMODIFIED}, new boolean[]{false}); //ascending=false
                StringArray dirSA  = (StringArray)table.getColumn(DIRECTORY);
                StringArray nameSA = (StringArray)table.getColumn(NAME);
                long lastModAr[]   = ((LongArray)table.getColumn(LASTMODIFIED)).array;
                long sizeAr[]      = ((LongArray)table.getColumn(SIZE)).array;

                //calcuate correct currentCacheSizeB
                currentCacheSizeB = 0;  
                for (int row = 0; row < nRows; row++)
                    currentCacheSizeB += sizeAr[row];
                if (currentCacheSizeB < thresholdCacheSizeB) {
                    if (reallyVerbose) String2.log(
                        "& pruneCache exiting after RECALCULATED currentSizeMB=" + 
                        currentCacheSizeB/Math2.BytesPerMB +
                        " < thresholdMB=" + thresholdCacheSizeB/Math2.BytesPerMB);
                    setPruneCacheDirSize(cacheDir, currentCacheSizeB);
                    return currentCacheSizeB;
                }

                //remove old files as needed
                int row = nRows - 1;
                if (row >= 0 && reallyVerbose) String2.log(
                    "& pruneCache nRows=" + nRows + 
                    " lastMod[0]=" + Calendar2.millisToIsoStringTZ(lastModAr[0]) + 
                    " lastMod[last]=" + Calendar2.millisToIsoStringTZ(lastModAr[row]) + 
                    " originalTimeSafe=" + Calendar2.millisToIsoStringTZ(originalTimeSafe) +
                    " currentSizeMB=" + currentCacheSizeB/Math2.BytesPerMB +
                    " goalMB=" + goal/Math2.BytesPerMB);
                while (row > 0 &&                            //not the last file
                       lastModAr[row] < originalTimeSafe &&  //file was old (when checked above)
                       currentCacheSizeB > goal) {           //still using too much space
                    if (debugMode) String2.log("& pruneCache look at row=" + row);

                    //be as thread-safe as reasonably possible
                    String localFullName = String2.canonical(dirSA.get(row) + nameSA.get(row));
                    synchronized (localFullName) {
                        long lastMod = File2.getLastModified(localFullName);  //in case changed very recently
                        long currentTimeSafe = System.currentTimeMillis() - PRUNE_CACHE_SAFE_MILLIS; //up-to-date
                        if (lastMod < currentTimeSafe) {  //file is old
                            if (debugMode) String2.log("& pruneCache DELETING " + localFullName);
                            if (File2.simpleDelete(localFullName)) //simple because may be in use!
                                currentCacheSizeB -= sizeAr[row];
                        }
                    }
                    row--;
                }
                boolean reachedGoal = currentCacheSizeB <= goal;
                if (reallyVerbose || !reachedGoal) String2.log(
                    "& pruneCache " + cacheDir + 
                    (reachedGoal? " FINISHED SUCCESSFULLY" : " WARNING: DIDN'T REACH GOAL") +
                    " goalMB=" + goal/Math2.BytesPerMB +
                    " currentMB=" + currentCacheSizeB/Math2.BytesPerMB);
            } catch (Exception e) {
                String2.log("Caught " + String2.ERROR + " in FileVisitorDNLS.pruneCache(" +
                    cacheDir + "):\n" + 
                    MustBe.throwableToString(e));
            }
            setPruneCacheDirSize(cacheDir, currentCacheSizeB);
            return currentCacheSizeB;
        }
    }


    /** 
     * This is a variant of oneStep (a convenience method for using this class)
     * that returns lastModified as double epoch seconds and size as doubles (bytes)
     * with some additional metadata. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     *    The resulting directoryPA will contain dirs with matching slashes and trailing slash.
     * @param pathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @return a table with columns with DIRECTORY, NAME, 
     *    LASTMODIFIED (doubles, epochSeconds), and SIZE (doubles, bytes) columns.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's name will be "".
     * @throws IOException if trouble
     */
    public static Table oneStepDouble(String tDir, String tFileNameRegex, 
        boolean tRecursive, String tPathRegex, boolean tDirectoriesToo) throws IOException {

        return oneStepDouble(oneStep(tDir, tFileNameRegex, tRecursive, 
            tPathRegex, tDirectoriesToo));
    }


    /**
     * This is a variant of oneStepDouble() that uses an existing table from oneStep().
     * 
     * @throws IOException if trouble
     */
    public static Table oneStepDouble(Table tTable) throws IOException {
        int nCols = tTable.nColumns();
        int nRows = tTable.nRows();

        for (int col = 0; col < nCols; col++) {
            String colName = tTable.getColumnName(col);
            Attributes atts = tTable.columnAttributes(col);

            if (colName.equals(DIRECTORY)) {
                atts.set("ioos_category", "Identifier");
                atts.set("long_name", "Directory");

            } else if (colName.equals(NAME)) {
                atts.set("ioos_category", "Identifier");
                atts.set("long_name", "File Name");

            } else if (colName.equals(LASTMODIFIED)) {
                atts.set("ioos_category", "Time");
                atts.set("long_name", "Last Modified");
                atts.set("units", Calendar2.SECONDS_SINCE_1970);
                LongArray la = (LongArray)tTable.getColumn(col);
                DoubleArray da = new DoubleArray(nRows, false);
                for (int row = 0; row < nRows; row++) 
                    da.add(la.get(row) / 1000.0); //to epochSeconds
                tTable.setColumn(col, da);

            } else if (colName.equals(SIZE)) {
                atts.set("ioos_category", "Other");
                atts.set("long_name", "Size");
                atts.set("units", "bytes");
                tTable.setColumn(col, new DoubleArray(tTable.getColumn(col)));
            }
        }

        return tTable;
    }

    /** 
     * This is like oneStep (a convenience method for using this class)
     * but returns a url column instead of a directory column. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash,
     *    which will be removed.
     * @param startOfUrl usually EDStatic.erddapUrl(loggedInAs) + "/files/" + datasetID() + "/"
     *    which will be prepended.
     * @return a table with columns with DIRECTORY (always "/"), NAME,
     *    LASTMODIFIED (long epochMilliseconds), and SIZE (long) columns.
     */
    public static Table oneStepWithUrlsNotDirs(String tDir, 
        String tFileNameRegex, boolean tRecursive, String tPathRegex, String startOfUrl) 
        throws IOException {

        tDir = File2.addSlash(String2.replaceAll(tDir, "\\", "/")); //ensure forward/ and trailing/
        return oneStepDoubleWithUrlsNotDirs(
            oneStep(tDir, tFileNameRegex, tRecursive, tPathRegex, false), //tDirectoriesToo
            tDir,
            startOfUrl);
    }
        
    /** 
     * This is like oneStepDouble (a convenience method for using this class)
     * but returns a url column instead of a directory column. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash,
     *    which will be removed.
     * @param startOfUrl usually EDStatic.erddapUrl(loggedInAs) + "/files/" + datasetID() + "/"
     *    which will be prepended.
     * @return a table with columns with DIRECTORY (always "/"), NAME,
     *    LASTMODIFIED (double epochSeconds), and SIZE (doubles) columns.
     */
    public static Table oneStepDoubleWithUrlsNotDirs(String tDir, 
        String tFileNameRegex, boolean tRecursive, String tPathRegex, String startOfUrl) 
        throws IOException {

        tDir = File2.addSlash(String2.replaceAll(tDir, "\\", "/")); //ensure forward/ and trailing/
        return oneStepDoubleWithUrlsNotDirs(
            oneStepDouble(tDir, tFileNameRegex, tRecursive, tPathRegex, false), //tDirectoriesToo
            tDir,
            startOfUrl);
    }
        

    /**
     * This is a variant of oneStepDoubleWithUrlsNotDirs() that uses an existing 
     * table from oneStepDouble(tDirToo=false).
     * 
     * @param tTable an existing table from oneStepDouble(tDirToo=false)
     * @throws IOException if trouble
     */
    public static Table oneStepDoubleWithUrlsNotDirs(Table tTable, String tDir, 
        String startOfUrl) throws IOException {

        int nCols = tTable.nColumns();
        int nRows = tTable.nRows();

        //replace directory with virtual url
        int diri = tTable.findColumnNumber(DIRECTORY);
        tTable.setColumnName(diri, URL); 
        tTable.columnAttributes(diri).set("long_name", "URL");
        if (nRows == 0)
            return tTable;

        StringArray dirPA = (StringArray)tTable.getColumn(diri);
        StringArray namePA = (StringArray)tTable.getColumn(NAME);
        int fromLength = tDir.length();
        //ensure returned dir is exactly as expected
        String from = dirPA.get(0).substring(0, fromLength);
        if (nRows > 0 && !from.equals(tDir)) {
            String2.log("Observed dir=" + from + "\n" +
                        "Expected dir=" + tDir);                            
            throw new SimpleException(MustBe.InternalError + " unexpected directory name."); 
        }
        for (int row = 0; row < nRows; row++) {
            //replace from with to 
            dirPA.set(row, 
                startOfUrl + dirPA.get(row).substring(fromLength) + namePA.get(row));               
        }

        return tTable;
    }        

    /** 
     * As a convenience to generateDatasetsXml methods, this returns the last
     * matching fullFileName.
     *
     */
    public static String getSampleFileName(String tFileDir, String tFileNameRegex,
        boolean tRecursive, String tPathRegex) throws Exception {
        Table fileTable = oneStep(tFileDir, tFileNameRegex, tRecursive, tPathRegex,
            false); //dirNamesToo
        int nRows = fileTable.nRows();
        if (nRows == 0)
            throw new RuntimeException(
                "ERROR in getSampleFileName: No matching files found for\n" +
                "  dir=" + tFileDir + " fileNameRegex=" + tFileNameRegex + 
                " recursive=" + tRecursive + " pathRegex=" + tPathRegex);
        return fileTable.getColumn(DIRECTORY).getString(nRows - 1) +
               fileTable.getColumn(NAME).getString(nRows - 1);
    }

//Patterns are thread safe.
//inport:
//<table><tr><th><img src="/icons/blank.gif" alt="[ICO]"></th><th>
//  <a href="?C=N;O=D">Name</a></th><th><a href="?C=M;O=A">Last modified</a></th>
//  <th><a href="?C=S;O=A">Size</a></th><th><a href="?C=D;O=A">Description</a></th>
//  </tr><tr><th colspan="5"><hr></th></tr>
//erddap:
//<pre><img src="..." alt="Icon "> <a href="?C=N;O=D">Name</a>
//  <a href="?C=M;O=A">Last modified</a>      <a href="?C=S;O=A">Size</a>  
//  <a href="?C=D;O=A">Description</a>
//This line is reliable. Not all WAFs have "Parent Directory".
    public final static Pattern wafHeaderPattern = Pattern.compile(
        ".*>Name</a>.*>Last modified</a>.*>Size</a>.*>Description</a>.*"); //older tomcat
//https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/iso19115/xml/
    public final static Pattern wafFileNamePattern = Pattern.compile(
        ".*>File Name<.*");  //info separate lines  tomcat 8.5.24

//inport:
//<tr><td valign="top"><img src="/icons/back.gif" alt="[DIR]"></td><td>
//  <a href="/waf/NOAA/NMFS/">Parent Directory</a>       </td><td>&nbsp;</td>
//  <td align="right">  - </td><td>&nbsp;</td></tr>
//erddap:
//<img src="..." alt="[DIR]" align="absbottom"> <a href="0&#x2f;">0</a>
    public final static Pattern wafDirPattern = Pattern.compile(
        ".* alt=\"\\[DIR\\]\".*>.*<a.*>(.*?)</a>.*");
//https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/
//<tr><td valign="top">&nbsp;</td><td><a href="doc/">doc/</a></td>
//  <td align="right">14-Mar-2017 08:34  </td><td align="right">  - </td><td>&nbsp;</td></tr>
    public final static Pattern wafDirPattern2 = Pattern.compile(
        ".*href=\"(.*?/)\">" +
        ".*>\\d{2}-[a-zA-Z]{3}-\\d{4} \\d{2}:\\d{2}(|:\\d{2})" + //date, note internal ()
        ".*");

//inport:
//<tr><td valign="top"><img src="/icons/unknown.gif" alt="[   ]"></td><td>
// <a href="16734.xml">16734.xml</a> </td>
// <td align="right">30-Jul-2015 13:05  </td><td align="right"> 10K</td>
// <td>&nbsp;</td></tr>
//erddap:
//<img src="..." alt="[BIN]" align="absbottom"> <a href="1932&#x2e;nc">1932&#x2e;nc</a>
//  07-Jan-2010 16:29  236K  
    public final static Pattern wafFilePattern = Pattern.compile(
        ".* alt=\"\\[.*?\\]\".*>.*<a.*>(.*?)</a>" + //name
        ".*(\\d{2}-[a-zA-Z]{3}-\\d{4} \\d{2}:\\d{2}(|:\\d{2}))" + //date, note internal ()
        ".*\\W(\\d{1,15}\\.?\\d{0,10}[KMGTP]?).*"); //size
//https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/
//<tr><td valign="top">&nbsp;</td><td><a href="gpcp_1dd_v1.2_p1d.199610">gpcp_1dd_v1.2_p1d.199610</a>
//  </td><td align="right">18-Sep-2012 12:55  </td><td align="right">7.7M</td><td>&nbsp;</td></tr>
    public final static Pattern wafFilePattern2 = Pattern.compile(
        ".* href=\"(.*?)\">" + //name
        ".*>(\\d{2}-[a-zA-Z]{3}-\\d{4} \\d{2}:\\d{2}(|:\\d{2}))" + //date, note internal ()
        ".*>(\\d{1,15}\\.?\\d{0,10}[KMGTP]?)</td>.*"); //size
//
//https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/
    public final static Pattern waf2FilenamePattern = Pattern.compile(
        ".*<strong>Filename</strong>.*");    
    public final static Pattern waf2HrefPattern = Pattern.compile(
        ".* href=\"(.*?)\">.*"); //name
    public final static Pattern waf2SizePattern = Pattern.compile(
        ".*><tt>(\\d{1,15}\\.?\\d{0,10} [kmgtp]?b)</tt></td>.*"); //size   //&nbsp;
    public final static Pattern waf2LastModPattern = Pattern.compile(
        //<td align="right"><tt>Sat, 04 Aug 2018 11:14:44 GMT</tt></td>
        ".*><tt>(" + Calendar2.RFC822_GMT_REGEX + ")</tt></td>.*");


    public static String[] getUrlsFromWAF(String startUrl, String fileNameRegex, 
        boolean recursive, String pathRegex) throws Throwable {
        if (verbose) String2.log("getUrlsFromHyraxCatalog fileNameRegex=" + fileNameRegex);

        //call the recursive method
        boolean tDirectoriesToo = false;
        StringArray dirs = new StringArray();
        StringArray names = new StringArray();
        LongArray lastModified = new LongArray(); //epochMillis
        LongArray size = new LongArray();
        addToWAFUrlList(startUrl, fileNameRegex, recursive, pathRegex, 
            tDirectoriesToo, dirs, names, lastModified, size);

        int n = dirs.size();
        String urls[] = new String[n];
        for (int i = 0; i < n; i++)
            urls[i] = dirs.get(i) + names.get(i);

        return urls;
    }

    /**
     * This gets file information from a 
     *   WAF (an Apache-style Web Accessible Folder) URL.
     * This calls itself recursively, adding into to the PrimitiveArrays as info 
     * for a URLs are found. 
     * dirs, names, lastModified and size are like a table with one row per item.
     *
     * @param url the url of a directory, e.g.,  
https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/
https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/
     * @param fileNameRegex e.g., "pentad.*flk\\.nc\\.gz"
     * @param recursive
     * @param pathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @param dirsToo  if true, subdirectories should be collected also.
     *   But the original url won't be included.
     * @param dirs directory names will be added here (they'll have / at end).
     * @param names file names will be added here (if dirsToo, "" for a dir).
     * @param lastModified the lastModified time (epochMillis, MAX_VALUE if not available).
     *   Source times are assumed to be Zulu time zone (which is probably incorrect).
     * @param size the file's size (bytes, Long.MAX_VALUE if not available).
     *   Usually, this is approximate (e.g., 10K vs 11K).
     * @return true if completely successful (no exceptions of any type)
     */
    public static boolean addToWAFUrlList(String url, String fileNameRegex, 
        boolean recursive, String pathRegex, boolean dirsToo,
        StringArray dirs, StringArray names, LongArray lastModified, LongArray size) {

        boolean completelySuccessful = true;  //but any child can set it to false
        if (pathRegex == null || pathRegex.length() == 0)
            pathRegex = ".*";
        BufferedReader in = null;
        try {
            if (reallyVerbose) String2.log("\naddToWAFUrlList nNames=" + names.size() + 
                " url=" + url); 
            url = File2.addSlash(url);

            //for html, any charset should be fine. 
            //All non-ASCII chars should be entities.
            //But use common Linux to be consistent.
            in = SSR.getBufferedUrlReader(url);
            String s;

            //look for header line
            int mode = 0;
            while ((s = in.readLine()) != null) {
                if (wafHeaderPattern.matcher(s).matches()) {
                    mode = 1;
                    break; 
                }
                if (waf2FilenamePattern.matcher(s).matches()) {
                    mode = 2;
                    break; 
                }
            }
            if (debugMode)
                String2.log(">> mode=" + mode);

            //read the lines with dir or file info
            if (mode == 1) {
                while ((s = in.readLine()) != null) {

                    //look for dirs before files (since dirs match filePattern, too)
                    if (s.indexOf("Parent Directory") >= 0)
                        continue;
                    Matcher matcher = wafDirPattern.matcher(s); 
                    boolean matched = matcher.matches();
                    if (!matched) {
                        matcher = wafDirPattern2.matcher(s);
                        matched = matcher.matches();
                    }
                    if (matched) {
                        String name = XML.decodeEntities(matcher.group(1));
                        String tUrl = File2.addSlash(url + name);
                        if (tUrl.matches(pathRegex)) {
                            if (dirsToo) {
                                dirs.add(tUrl);
                                names.add("");
                                lastModified.add(Long.MAX_VALUE);
                                size.add(Long.MAX_VALUE);
                            }
                            if (recursive) {
                                if (!addToWAFUrlList(
                                    tUrl, fileNameRegex, recursive, pathRegex, 
                                    dirsToo, dirs, names, lastModified, size))
                                    completelySuccessful = false;
                            }
                        }
                        continue;
                    }

                    //look for files 
                    matcher = wafFilePattern.matcher(s); 
                    matched = matcher.matches();
                    if (!matched) {
                        matcher = wafFilePattern2.matcher(s);
                        matched = matcher.matches();
                    }
                    if (matched) {
                        String name = XML.decodeEntities(matcher.group(1));
                        if (name.matches(fileNameRegex)) {

                            //interpret last modified
                            long millis = Long.MAX_VALUE;
                            try {
                                millis = Calendar2.parseDDMonYYYYZulu(matcher.group(2)).getTimeInMillis();
                            } catch (Throwable t2) {
                                String2.log(t2.getMessage());
                            }

                            //convert size to bytes
                            String tSize = matcher.group(4);
                            char lastCh = tSize.charAt(tSize.length() - 1);
                            long times = 1;
                            if (!String2.isDigit(lastCh)) {
                                tSize = tSize.substring(0, tSize.length() - 1);
                                times = lastCh == 'K'? Math2.BytesPerKB :
                                        lastCh == 'M'? Math2.BytesPerMB :
                                        lastCh == 'G'? Math2.BytesPerGB :
                                        lastCh == 'T'? Math2.BytesPerTB :
                                        lastCh == 'P'? Math2.BytesPerPB : 1;
                            }
                            long lSize = Math2.roundToLong(String2.parseDouble(tSize) * times);

                            dirs.add(url);
                            names.add(name);
                            lastModified.add(millis);
                            size.add(lSize);
                        } else if (debugMode) {
                            String2.log("name matches=false: " + name);
                        }
                        continue;
                    } else {
                        if (debugMode) String2.log(">> matches=false: " + s);
                    }
                }
            } else if (mode == 2) {
                //read the lines with dir or file info
                String domain = File2.getProtocolDomain(url);
                
                while ((s = in.readLine()) != null) {

                    //look for href 
                    Matcher matcher = waf2HrefPattern.matcher(s); 
                    if (matcher.matches()) {
                        if (debugMode)
                            String2.log(">> matched href line=" + s);

                        String name = XML.decodeEntities(matcher.group(1));
                        if (name.startsWith("/"))
                            name = domain + name;

                        //a directory?
                        if (name.endsWith("/")) {
                            if (name.matches(pathRegex)) {
                                if (dirsToo) {
                                    dirs.add(name);
                                    names.add("");
                                    lastModified.add(Long.MAX_VALUE);
                                    size.add(Long.MAX_VALUE);
                                }
                                if (recursive) {
                                    if (!addToWAFUrlList(
                                        name, fileNameRegex, recursive, pathRegex, 
                                        dirsToo, dirs, names, lastModified, size))
                                        completelySuccessful = false;
                                }
                            }
                            continue;
                        }

                        //it's a file name
                        if (!File2.getNameAndExtension(name).matches(fileNameRegex))
                            continue;

                        //next line is size (or &nbsp; for dir)
                        s = in.readLine();
                        matcher = waf2SizePattern.matcher(s == null? "" : s); 
                        long lSize = Long.MAX_VALUE;
                        if (matcher.matches()) {
                            //convert size to bytes
                            String tSize = XML.decodeEntities(matcher.group(1));
                            char lastCh = Character.toLowerCase(tSize.charAt(tSize.length() - 2)); //kmgtp
                            long times = 1;
                            if ("kmgtp".indexOf(lastCh) >= 0) {
                                tSize = tSize.substring(0, tSize.length() - 2);
                                times = lastCh == 'k'? Math2.BytesPerKB :
                                        lastCh == 'm'? Math2.BytesPerMB :
                                        lastCh == 'g'? Math2.BytesPerGB :
                                        lastCh == 't'? Math2.BytesPerTB :
                                        lastCh == 'p'? Math2.BytesPerPB : 1;
                            } else {
                                tSize = tSize.substring(0, tSize.length() - 1);  //exclude 'b'
                            }
                            lSize = Math2.roundToLong(String2.parseDouble(tSize) * times);
                        }

                        //next line is lastMod in RFC822 format
                        s = in.readLine();
                        matcher = waf2LastModPattern.matcher(s == null? "" : s); 
                        long millis = Long.MAX_VALUE;
                        if (matcher.matches()) {
                            try {
                                millis = Calendar2.parseDateTimeZulu(
                                    XML.decodeEntities(matcher.group(1)), 
                                    Calendar2.RFC822_GMT_FORMAT).getTimeInMillis();
                            } catch (Throwable t2) {
                                String2.log(t2.getMessage());
                            }
                        }

                        dirs.add(File2.getDirectory(name));
                        names.add(File2.getNameAndExtension(name));
                        lastModified.add(millis);
                        size.add(lSize);
                    } else {
                        if (debugMode) String2.log(">> matches=false: " + s);
                    }
                }
            }

        } catch (Exception e) {
            String2.log(String2.ERROR + " from url=" + url + " :\n" + 
                MustBe.throwableToString(e));
            completelySuccessful = false;

        } finally {
            try {
                if (in != null)
                    in.close();  
            } catch (Throwable t) {
            }
        }
        return completelySuccessful;

    }



    /**
     * This tests a WAF-related (Web Accessible Folder) methods on an ERDDAP "files" directory.
     */
    public static void testErddapFilesWAF() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testErddapFilesWAF()\n");
        boolean oDebugMode = debugMode;
        //debugMode=true;        

        try {
        //test with trailing /
        //This also tests redirect to https!
        String url = "https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/"; 
        String tFileNameRegex = "194\\d\\.nc";
        boolean tRecursive = true;
        String tPathRegex = ".*/(3|4)/.*";   
        boolean tDirsToo = true;
        Table table = makeEmptyTable();
        StringArray dirs        = (StringArray)table.getColumn(0);
        StringArray names       = (StringArray)table.getColumn(1);
        LongArray lastModifieds = (LongArray)table.getColumn(2);
        LongArray sizes         = (LongArray)table.getColumn(3);
        String results, expected;
        Table tTable;

        //* test all features
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,,,\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1940.nc,1262881740000,14916\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1941.nc,1262881740000,17380\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1942.nc,1262881740000,20548\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1943.nc,1262881740000,17280\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1944.nc,1262881740000,12748\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1945.nc,1262881740000,15692\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1946.nc,1262881740000,17028\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1947.nc,1262881740000,11576\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1948.nc,1262881740000,12876\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1949.nc,1262881740000,15268\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,,,\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1940.nc,1262881740000,285940\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1941.nc,1262881740000,337768\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1942.nc,1262881740000,298608\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1943.nc,1262881740000,175940\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1944.nc,1262881740000,215864\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1945.nc,1262881740000,195056\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1946.nc,1262881740000,239444\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1947.nc,1262881740000,190272\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1948.nc,1262881740000,263084\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1949.nc,1262881740000,352240\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1262881740),
            "2010-01-07T16:29:00Z", "");

        //test via oneStep 
        tTable = oneStep(url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo);
        results = tTable.dataToString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* test !dirsToo
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, tFileNameRegex, tRecursive, tPathRegex, false, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1940.nc,1262881740000,14916\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1941.nc,1262881740000,17380\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1942.nc,1262881740000,20548\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1943.nc,1262881740000,17280\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1944.nc,1262881740000,12748\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1945.nc,1262881740000,15692\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1946.nc,1262881740000,17028\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1947.nc,1262881740000,11576\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1948.nc,1262881740000,12876\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1949.nc,1262881740000,15268\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1940.nc,1262881740000,285940\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1941.nc,1262881740000,337768\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1942.nc,1262881740000,298608\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1943.nc,1262881740000,175940\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1944.nc,1262881740000,215864\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1945.nc,1262881740000,195056\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1946.nc,1262881740000,239444\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1947.nc,1262881740000,190272\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1948.nc,1262881740000,263084\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1949.nc,1262881740000,352240\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url, tFileNameRegex, tRecursive, tPathRegex, false);
        results = tTable.dataToString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* test subdir
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url + "3", //test no trailing /
                tFileNameRegex, tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1940.nc,1262881740000,14916\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1941.nc,1262881740000,17380\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1942.nc,1262881740000,20548\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1943.nc,1262881740000,17280\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1944.nc,1262881740000,12748\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1945.nc,1262881740000,15692\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1946.nc,1262881740000,17028\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1947.nc,1262881740000,11576\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1948.nc,1262881740000,12876\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1949.nc,1262881740000,15268\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url + "3", tFileNameRegex, tRecursive, tPathRegex, tDirsToo);
        results = tTable.dataToString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* test file regex that won't match
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, //test no trailing /
                "zztop", tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = //just dirs
"directory,name,lastModified,size\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,,,\n" +
"https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,,,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url, "zztop", tRecursive, tPathRegex, tDirsToo);
        results = tTable.dataToString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* that should be the same as !recursive
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, //test no trailing /
                tFileNameRegex, false, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url, tFileNameRegex, false, tPathRegex, tDirsToo);
        results = tTable.dataToString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //*** test localhost
        String2.log("\nThis test requires erdMWchla1day in localhost erddap.");
        url = "http://localhost:8080/cwexperimental/files/erdMWchla1day/"; 
        tFileNameRegex = "MW200219.*\\.nc(|\\.gz)";
        tRecursive = true;
        tPathRegex = ".*";   
        tDirsToo = true;
        table.removeAllRows();

        //* test all features
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002190_2002190_chla.nc.gz,1535062380000,3541709\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002191_2002191_chla.nc.gz,1535062380000,2661568\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002192_2002192_chla.nc.gz,1535062380000,2680618\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002193_2002193_chla.nc.gz,1535062380000,2392851\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002194_2002194_chla.nc.gz,1535062380000,2209197\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002195_2002195_chla.nc.gz,1535062380000,2246841\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002196_2002196_chla.nc.gz,1535062380000,1543949\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002197_2002197_chla.nc.gz,1535062380000,1846579\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002198_2002198_chla.nc.gz,1535062380000,2252800\n" +
"http://localhost:8080/cwexperimental/files/erdMWchla1day/,MW2002199_2002199_chla.nc.gz,1535062380000,2547736\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //** Test InPort WAF        
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList("https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/",
                "22...\\.xml",   
                //pre 2016-03-04 I tested NWFSC/inport/xml, but it has been empty for a month!
                true, ".*/NMFS/(|NEFSC/(|inport-xml/(|xml/)))", //tricky! 
                true, //tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/,,,\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/,,,\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,,,\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22561.xml,1554803918000,214016\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22562.xml,1554803940000,211046\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22563.xml,1554803962000,213504\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22564.xml,1554803984000,213094\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22565.xml,1554804006000,214323\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22560.xml,1557568922000,213709\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //* Test ncei WAF        
        table.removeAllRows();
        tTable = oneStep(
            "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/",
            "gpcp_1dd_v1\\.2_p1d\\.1997[0-9]{2}",   
            true, ".*", true); //tDirsToo, 
        results = tTable.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1996/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1997/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1998/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1999/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2001/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2002/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2003/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2004/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2005/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2006/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2007/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2008/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2009/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2010/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2011/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2012/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2013/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2014/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2015/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2016/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2017/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2018/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2019/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/doc/,,,\n" +
"https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/src/,,,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //ersst
        table.removeAllRows();
        url = "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/";
        tFileNameRegex = "ersst.v4.19660.*\\.nc";
        tRecursive = false;
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196601.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196602.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196603.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196604.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196605.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196606.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196607.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196608.nc,1479909540000,135168\n" +
"https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196609.nc,1479909540000,135168\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\nThis changes periodically. If reasonable, just continue." +
              "\n(there no more subtests in this test).");
      }
      debugMode = oDebugMode;
    }



    /**
     * This gets the file names from Hyrax catalog directory URL.
     * This only finds info for DAP URLs. 
     * This doesn't find other types of files (although they may be listed)
     * This is used by EDDGridFromDap and EDDTableFromHyraxFiles.
     *
     * @param startUrl the url of the current web directory to which "contents.html" 
     *  can be added to see a hyrax catalog) e.g.,
        http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/
        or
        https://opendap.jpl.nasa.gov/opendap/hyrax/allData/avhrr/L4/reynolds_er/v3b/monthly/netcdf/2014/
     * @param fileNameRegex e.g.,
            "pentad.*flk\\.nc\\.gz"
     * @param recursive
     * @param pathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @return a String[] with a list of full URLs of the children (may be new String[0]) 
     * @throws Throwable if unexpected trouble. But url not responding won't throw Throwable.
     */
    public static String[] getUrlsFromHyraxCatalog(String startUrl, String fileNameRegex, 
        boolean recursive, String pathRegex) throws Throwable {
        if (verbose) String2.log("getUrlsFromHyraxCatalog fileNameRegex=" + fileNameRegex);

        //call the recursive method
        boolean tDirectoriesToo = false;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray size = new LongArray();
        addToHyraxUrlList(startUrl, fileNameRegex, recursive, pathRegex, 
            tDirectoriesToo, childUrls, lastModified, size);

        return childUrls.toArray();
    }

    /**
     * This does the work for getUrlsFromHyraxCatalogs.
     * This calls itself recursively, adding into to the PrimitiveArrays as info 
     * for a DAP URL is found. 
     * This doesn't find other types of files (although they may be listed)
     *
     * @param url the url of the directory to which contents.html can be added
     *    to see a hyrax catalog, e.g.,
            http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/
     *   (If url has a file name, it must be "contents.html".)
     * @param fileNameRegex e.g., "pentad.*flk\\.nc\\.gz"
     * @param recursive
     * @param pathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @param dirsToo  if true, directories should be collected also
     * @param childUrls  new children will be added to this
     * @param lastModified the lastModified time (secondsSinceEpoch, NaN if not available).
     *   Source times are assumed to be Zulu time zone (which is probably incorrect).
     * @param size the file's size (bytes, Long.MAX_VALUE if not available)
     * @return true if completely successful (no access errors, all URLs found)
     * @throws Throwable if unexpected trouble. But url not responding won't throw Throwable.
     */
    public static boolean addToHyraxUrlList(String url, String fileNameRegex, 
        boolean recursive, String pathRegex, boolean dirsToo,
        StringArray childUrls, DoubleArray lastModified, LongArray size) throws Throwable {

        if (reallyVerbose) String2.log("\naddToHyraxUrlList childUrls.size=" + childUrls.size() + 
            "\n  url=" + url); 
        if (pathRegex == null || pathRegex.length() == 0)
            pathRegex = ".*";
        boolean completelySuccessful = true;  //but any child can set it to false
        String response;
        try {
            if (url.endsWith("/contents.html"))
                url = File2.getDirectory(url);
            else url = File2.addSlash(url); //otherwise, assume url is missing final slash
            response = SSR.getUrlResponseStringUnchanged(url + "contents.html");
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return false;
        }
        String responseLC = response.toLowerCase();
        if (dirsToo) {
            childUrls.add(url);
            lastModified.add(Double.NaN);
            size.add(Long.MAX_VALUE);
        }

        //skip header line and parent directory
        int po = responseLC.indexOf("parent directory");  //Lower Case
        if (po >= 0) {
            po += 18;
        } else {
            if (verbose) String2.log("WARNING: \"parent directory\" not found in Hyrax response.");
            po = responseLC.indexOf("<table");  //Lower Case
            if (po < 0) {
                if (verbose) String2.log("ERROR: \"<table\" not found in Hyrax response.\n" +
                    "No dir info found for " + url);
                return false;
            } else {
                po += 6;
                if (verbose) String2.log("Using \"<table\" as starting point instead.");
            }
        }

        //endPre
        int endPre = responseLC.indexOf("</pre>", po); //Lower Case
        if (endPre < 0) 
            endPre = response.length();

        //go through file,dir listings
        boolean diagnosticMode = false;
        while (true) {

            //EXAMPLE https://data.nodc.noaa.gov/opendap/wod/monthly/  No longer available

            //EXAMPLE https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/M07
            //(reformatted: look for tags, not formatting
            /*   <tr>
                   <td align="left"><strong><a href="month_19870701_v11l35flk.nc.gz.html">month_19870701_v11l35flk.nc.gz</a></strong></td>
                   <td align="center" nowrap="nowrap">2007-04-04T07:00:00</td>
                   <td align="right">4807310</td>
                   <td align="center">
                      <table>
                      <tr>
                        <td><a href="month_19870701_v11l35flk.nc.gz.ddx">ddx</a>&nbsp;</td>
                        <td><a href="month_19870701_v11l35flk.nc.gz.dds">dds</a>&nbsp;</td>
                      </table>  //will exist if <table> exists
                   </td>
                   <td align="center"><a href="/opendap/webstart/viewers?dapService=/opendap/hyrax&amp;datasetID=/allData/ccmp/L3.5a/monthly/flk/1987/M07/month_19870701_v11l35flk.nc.gz">viewers</a></td>
                 </tr>  //may or may not exist
                 <tr>   //may or may not exist
                   //the next row...
               </table>
            */ 

            //find beginRow and nextRow
            int beginRow = responseLC.indexOf("<tr", po);      //Lower Case
            if (beginRow < 0 || beginRow > endPre)
                return completelySuccessful;
            int endRow = responseLC.indexOf("<tr", beginRow + 3);      //Lower Case
            if (endRow < 0 || endRow > endPre)
                endRow = endPre;

            //if <table> in the middle, skip table 
            int tablePo = responseLC.indexOf("<table", beginRow + 3);
            if (tablePo > 0 && tablePo < endRow) {
                int endTablePo = responseLC.indexOf("</table", tablePo + 6);
                if (endTablePo < 0 || endTablePo > endPre)
                    endTablePo = endPre;

                //find <tr after </table>
                endRow = responseLC.indexOf("<tr", endTablePo + 7);      //Lower Case
                if (endRow < 0 || endRow > endPre)
                    endRow = endPre;
            }
            String thisRow   = response.substring(beginRow, endRow);
            String thisRowLC = responseLC.substring(beginRow, endRow);
            if (diagnosticMode) 
                String2.log("<<<beginRow=" + beginRow + " endRow=" + endRow + "\n" + 
                    thisRow + "\n>>>");

            //look for .das   href="wod_013459339O.nc.das">das<     
            int dasPo = thisRowLC.indexOf(".das\">das<");
            if (diagnosticMode) 
                String2.log("    .das " + (dasPo < 0? "not " : "") + "found");
            if (dasPo > 0) {
                int quotePo = thisRow.lastIndexOf('"', dasPo);
                if (quotePo < 0) {
                    String2.log("ERROR: invalid .das reference:\n  " + thisRow);
                    po = endRow;
                    continue;
                }
                String fileName = thisRow.substring(quotePo + 1, dasPo);
                if (diagnosticMode) 
                    String2.log("    filename=" + fileName + 
                        (fileName.matches(fileNameRegex)? " does" : " doesn't") + 
                        " match " + fileNameRegex);
                if (fileName.matches(fileNameRegex)) {

                    //get lastModified time   >2011-06-30T04:43:09<
                    String stime = String2.extractRegex(thisRow,
                        ">\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}<", 0);
                    double dtime = Calendar2.safeIsoStringToEpochSeconds(
                        stime == null? "" : stime.substring(1, stime.length() - 1));

                    //get size   e.g., 119K
                    String sSize = String2.extractRegex(thisRow,
                        ">(\\d|\\.)+(|K|M|G|T|P)</td>", 0);
                    long lSize = Long.MAX_VALUE;
                    if (sSize != null) {
                        sSize = sSize.substring(1, sSize.length() - 5);
                        char lastCh = sSize.charAt(sSize.length() - 1);
                        long times = 1;
                        if (!String2.isDigit(lastCh)) {
                            //this is done, but my samples just have nBytes
                            sSize = sSize.substring(0, sSize.length() - 1);
                            times = lastCh == 'K'? Math2.BytesPerKB :
                                    lastCh == 'M'? Math2.BytesPerMB :
                                    lastCh == 'G'? Math2.BytesPerGB :
                                    lastCh == 'T'? Math2.BytesPerTB :
                                    lastCh == 'P'? Math2.BytesPerPB : 1;
                        }
                        lSize = Math2.roundToLong(String2.parseDouble(sSize) * times);
                    }

                    //then add to PrimitiveArrays
                    childUrls.add(url + fileName);
                    lastModified.add(dtime);
                    size.add(lSize);
                    //String2.log("  file=" + fileName + "   " + stime);
                    po = endRow;
                    continue;
                }
            } 

            if (recursive) {
                //look for   href="199703-199705/contents.html"     
                int conPo = thisRowLC.indexOf("/contents.html\"");
                if (conPo > 0) {
                    int quotePo = thisRow.lastIndexOf('"', conPo);
                    if (quotePo < 0) {
                        String2.log("ERROR: invalid contents.html reference:\n  " + thisRow);
                        po = endRow;
                        continue;
                    }
                    String tUrl = url + thisRow.substring(quotePo + 1, conPo + 1);
                    if (tUrl.matches(pathRegex)) {
                        boolean tSuccessful = addToHyraxUrlList(
                            tUrl, fileNameRegex, recursive, pathRegex, 
                            dirsToo, childUrls, lastModified, size);
                        if (!tSuccessful)
                            completelySuccessful = false;
                    }
                    po = endRow;
                    continue;
                }
            }
            po = endRow;
        }
    }



    /**
     * This tests Hyrax-related methods.
     */
    public static void testHyrax() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testHyrax()\n");
        //reallyVerbose = true;
        //debugMode=true;

        try {
        //before 2018-08-17 podaac-opendap caused  
        //  "javax.net.ssl.SSLProtocolException: handshake alert: unrecognized_name" error
        //String url = "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/"; //contents.html
        //so I used domain name shown on digital certificate: opendap.jpl.nasa.gov for tests below
        //But now podaac-opendap works.
        String url = "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/"; //contents.html
        String fileNameRegex = "month_198(8|9).*flk\\.nc\\.gz";
        boolean recursive = true;
        String pathRegex = null;
        boolean dirsToo = true;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray size = new LongArray();

        //test error via addToHyraxUrlList  
        //(yes, logged message includes directory name)
        String2.log("\nIntentional error:");
        Test.ensureEqual(
            addToHyraxUrlList(url + "testInvalidUrl", fileNameRegex, 
                recursive, pathRegex, dirsToo, childUrls, lastModified, size),
            false, "");

        //test addToHyraxUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();
        size = new LongArray();
        boolean allOk = addToHyraxUrlList(url, fileNameRegex, recursive, 
            pathRegex, dirsToo, childUrls, lastModified, size);
        Table table = new Table();
        table.addColumn("URL", childUrls);
        table.addColumn("lastModified", lastModified);
        table.addColumn("size", size);
        String results = table.dataToString();
        String expected = 
"URL,lastModified,size\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz,1.336863115E9,4981045\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz,1.336723222E9,5024372\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz,1.336546575E9,5006043\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz,1.336860015E9,4948285\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz,1.336835143E9,4914250\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz,1.336484405E9,4841084\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz,1.336815079E9,4837417\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz,1.336799789E9,4834242\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz,1.336676042E9,4801865\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz,1.336566352E9,4770289\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz,1.336568382E9,4769160\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz,1.336838712E9,4866335\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890101_v11l35flk.nc.gz,1.336886548E9,5003981\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890201_v11l35flk.nc.gz,1.336268373E9,5054907\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890301_v11l35flk.nc.gz,1.336605483E9,4979393\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890401_v11l35flk.nc.gz,1.336350339E9,4960865\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890501_v11l35flk.nc.gz,1.336551575E9,4868541\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890601_v11l35flk.nc.gz,1.336177278E9,4790364\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890701_v11l35flk.nc.gz,1.336685187E9,4854943\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890801_v11l35flk.nc.gz,1.336534686E9,4859216\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890901_v11l35flk.nc.gz,1.33622953E9,4838390\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891001_v11l35flk.nc.gz,1.336853599E9,4820645\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891101_v11l35flk.nc.gz,1.336882933E9,4748166\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891201_v11l35flk.nc.gz,1.336748115E9,4922858\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1990/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1991/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1992/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1993/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1994/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1995/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1996/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1997/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1998/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1999/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2000/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2001/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2002/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2003/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2004/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2005/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2006/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2007/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2008/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2009/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2010/,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2011/,,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureTrue(allOk, "");

        //test getUrlsFromHyraxCatalog
        String resultsAr[] = getUrlsFromHyraxCatalog(url, fileNameRegex, recursive,
            pathRegex);
        String expectedAr[] = new String[]{
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890101_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890201_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890301_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890401_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890501_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890601_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890701_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890801_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890901_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891001_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891101_v11l35flk.nc.gz",
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891201_v11l35flk.nc.gz"}; 
        Test.ensureEqual(resultsAr, expectedAr, "results=\n" + results);

        //different test of addToHyraxUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();
        LongArray fSize = new LongArray();
        url = "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/"; //startUrl, 
        fileNameRegex = "month_[0-9]{8}_v11l35flk\\.nc\\.gz"; //fileNameRegex, 
        recursive = true;
        addToHyraxUrlList(url, fileNameRegex, recursive, pathRegex, dirsToo,
            childUrls, lastModified, fSize);

        results = childUrls.toNewlineString();
        expected = 
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870701_v11l35flk.nc.gz\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870801_v11l35flk.nc.gz\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870901_v11l35flk.nc.gz\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871001_v11l35flk.nc.gz\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871101_v11l35flk.nc.gz\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871201_v11l35flk.nc.gz\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = lastModified.toString();
        expected = "NaN, 1.336609915E9, 1.336785444E9, 1.336673639E9, 1.336196561E9, 1.336881763E9, 1.336705731E9";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep -- dirs
        table = oneStep(url, fileNameRegex, recursive, pathRegex, true);
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,,,\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870701_v11l35flk.nc.gz,1336609915000,4807310\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870801_v11l35flk.nc.gz,1336785444000,4835774\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870901_v11l35flk.nc.gz,1336673639000,4809582\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871001_v11l35flk.nc.gz,1336196561000,4803285\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871101_v11l35flk.nc.gz,1336881763000,4787239\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871201_v11l35flk.nc.gz,1336705731000,4432696\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep -- no dirs
        table = oneStep(url, fileNameRegex, recursive, pathRegex, false);
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870701_v11l35flk.nc.gz,1336609915000,4807310\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870801_v11l35flk.nc.gz,1336785444000,4835774\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870901_v11l35flk.nc.gz,1336673639000,4809582\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871001_v11l35flk.nc.gz,1336196561000,4803285\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871101_v11l35flk.nc.gz,1336881763000,4787239\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871201_v11l35flk.nc.gz,1336705731000,4432696\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\nUnexpected error."); 
      }
    }

    /**
     * This tests Hyrax-related methods with the JPL MUR dataset.
     */
    public static void testHyraxMUR() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testHyraxMUR()\n");
reallyVerbose = true;
debugMode=true;

        try {
        String url = "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/"; //contents.html
        String fileNameRegex = "[0-9]{14}-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02\\.0-fv04\\.1\\.nc";
        boolean recursive = true;
        String pathRegex = ".*/v4\\.1/2018/(|01./)";  //for test, just get 01x dirs/files. Read regex: "(|2018/(|/[0-9]{3}/))";
        boolean dirsToo = false;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray size = new LongArray();

        //test via oneStep -- no dirs
        Table table = oneStep(url, fileNameRegex, recursive, pathRegex, false);
        String results = table.dataToString();
        String expected = 
"directory,name,lastModified,size\n" +   //lastMod is epochMillis
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/010/,20180110090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526396428000,400940089\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/011/,20180111090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526400024000,402342953\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/012/,20180112090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526403626000,407791965\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/013/,20180113090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526407232000,410202577\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/014/,20180114090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526410828000,412787416\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/015/,20180115090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526414431000,408049023\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/016/,20180116090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526418038000,398060630\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/017/,20180117090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526421637000,388221460\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/018/,20180118090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526425240000,381433871\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/019/,20180119090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526428852000,390892954\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\nUnexpected error."); 
      }
debugMode=false;
    }



    /**
     * This parses /thredds/catalog/.../catalog.html files to extract file URLs 
     * (/thredds/fileServer/.../name.ext), lastModified, and size info.
     * This calls itself recursively, adding into to the PrimitiveArrays as info 
     * for a file URL is found. 
     * This doesn't find other types of files (although they may be listed)
     *
     * @param url the url of the current Thredds directory 
     *   (which usually includes /thredds/catalog/)
     *   to which catalog.html will be added, e.g.,
     *    <br>https://data.nodc.noaa.gov/thredds/catalog/pathfinder/Version5.1_CloudScreened/5day/FullRes/
     *   (If url has a file name, it must be catalog.html or catalog.xml.)
     * @param fileNameRegex e.g., ".*\\.hdf"
     * @param recursive
     * @param pathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @param childUrls  new children will be added to this
     * @param lastModified the lastModified time (secondsSinceEpoch, NaN if not available).
     *   Source times are assumed to be Zulu time zone (which is probably incorrect).
     * @param size the file's size (bytes, Long.MAX_VALUE if not available)
     * @return true if completely successful (no access errors, all URLs found)
     * @throws Throwable if unexpected trouble. But url not responding won't throw Throwable.
     */
    public static boolean addToThreddsUrlList(String url, String fileNameRegex, 
        boolean recursive, String pathRegex, boolean dirsToo,
        StringArray childUrls, DoubleArray lastModified, LongArray size) throws Throwable {

        if (reallyVerbose) String2.log("\naddToThreddsUrlList childUrls.size=" + childUrls.size() + 
            "\n  url=" + url); 
        if (pathRegex == null || pathRegex.length() == 0)
            pathRegex = ".*";
        boolean completelySuccessful = true;  //but any child can set it to false
        String response;
        try {
            if (url.endsWith("/catalog.html") || url.endsWith("/catalog.xml"))
                url = File2.getDirectory(url);
            else url = File2.addSlash(url); //otherwise, assume url is missing final slash
            response = SSR.getUrlResponseStringUnchanged(url + "catalog.html");
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return false;
        }
        String fileServerDir = String2.replaceAll(url,
            "/thredds/catalog/", "/thredds/fileServer/");
        if (dirsToo) {
            childUrls.add(fileServerDir);
            lastModified.add(Double.NaN);
            size.add(Long.MAX_VALUE);
        }

        //skip header line and parent directory
        int po = response.indexOf("<table");  //Lower Case
        if (po < 0 ) {
            String2.log("ERROR: \"<table\" not found in Thredds response.");
            return false;
        }
        po += 6;

        //endTable
        int endTable = response.indexOf("</table>", po); //Lower Case
        if (endTable < 0) {
            if (reallyVerbose) String2.log("WARNING: </table> not found!");
            completelySuccessful = false;
            endTable = response.length();
        }

        //go through file,dir listings
        boolean diagnosticMode = false;
        int row = 0;
        while (true) {
            row++;

/* EXAMPLE from TDS 4.2.10 at
https://data.nodc.noaa.gov/thredds/catalog/pathfinder/Version5.1_CloudScreened/5day/FullRes/1981/catalog.html
...<table ...
...<tr>
<th align='left'><font size='+1'>Dataset</font></th>
<th align='center'><font size='+1'>Size</font></th>
<th align='right'><font size='+1'>Last Modified</font></th>
</tr><tr>
<td align='left'>&nbsp;&nbsp;&nbsp;&nbsp;
<img src='/thredds/folder.gif' alt='Folder'> &nbsp;<a href='catalog.html?dataset=pathfinder/Version5.1_CloudScreened/5day/FullRes/1981'><tt>1981</tt></a></td>
<td align='right'><tt>&nbsp;</tt></td>
<td align='right'><tt>--</tt></td>
</tr>
<tr bgcolor='#eeeeee'>
<td align='left'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<a href='catalog.html?dataset=pathfinder/Version5.1_CloudScreened/5day/FullRes/1981/1981236-1981240.pfv51sst_5day.hdf'><tt>1981236-1981240.pfv51sst_5day.hdf</tt></a></td>
<td align='right'><tt>70.03 Mbytes</tt></td>
<td align='right'><tt>2009-11-23 17:58:53Z</tt></td>             or now T in middle
</tr>
*/ 
            //find beginRow and nextRow
            int beginRow = response.indexOf("<tr", po);  
            if (beginRow < 0 || beginRow > endTable)
                return completelySuccessful;
            int nextRow = response.indexOf("<tr", beginRow + 3); 
            if (nextRow < 0 || nextRow > endTable)
                nextRow = endTable;

            String thisRow = response.substring(beginRow, nextRow);
            if (diagnosticMode) 
                String2.log("=== thisRow=" + thisRow);

            //look for <td>     
            int td1Po = thisRow.indexOf("<td");
            int td2Po = td1Po >= 0? thisRow.indexOf("<td", td1Po + 3) : -1;
            int td3Po = td2Po >= 0? thisRow.indexOf("<td", td2Po + 3) : -1;
            if (diagnosticMode) 
                String2.log("=== td1Po=" + td1Po + " td2Po=" + td2Po + " td3Po=" + td3Po);

            //are there no <td's  (e.g., row with <th>'s)
            if (td1Po < 0) {
                po = nextRow;
                continue;
            }

            //There is at least 1 <td>.  Is it a catalog?
            String td1 = thisRow.substring(td1Po + 3, td2Po >= 0? td2Po : thisRow.length());
            if (td1.indexOf("'/thredds/folder.gif'") > 0) {
                //This is a folder row.
                //Row with current dir doesn't match fileNameRegex; row for subdir does.
                if (diagnosticMode) 
                    String2.log("=== folder row");
                String content1 = String2.extractRegex(td1, "href='[^']*/catalog.html'>", 0);
                if (recursive && content1 != null && content1.length() > 21) { //21 is non-.* stuff
                    content1 = content1.substring(6, content1.length() - 2);
                    completelySuccessful = addToThreddsUrlList(
                        url + content1, fileNameRegex, recursive, pathRegex,
                        dirsToo, childUrls, lastModified, size);
                }
                po = nextRow;
                continue;
            }

            //file info row will have 3 <td>'s.   So if not, skip this row.
            if (td3Po < 0) {
                po = nextRow;
                if (diagnosticMode) 
                    String2.log("=== td3Po<0 so go to next row");
                continue;
            }

            //look for <tt>content</tt> in the 3 <td>'s
            //note that String2.extractCaptureGroup fails if the string has line terminators
            String content1 = String2.extractRegex(td1, TT_REGEX, 0);
            String content2 = String2.extractRegex(
                thisRow.substring(td2Po, td3Po), TT_REGEX, 0);
            String content3 = String2.extractRegex(
                thisRow.substring(td3Po, thisRow.length()), TT_REGEX, 0);
            if (diagnosticMode) 
                String2.log("=== <td><tt> content #1=" + content1 + " #2=" + content2 + 
                                                " #3=" + content3);
            content1 = content1 == null? "" : content1.substring(4, content1.length() - 5);
            content2 = content2 == null? "" : content2.substring(4, content2.length() - 5);
            content3 = content3 == null? "" : content3.substring(4, content3.length() - 5);
            if (content1.length() == 0) { 
                if (reallyVerbose) 
                    String2.log("WARNING: No <tt>content</tt> in first <td>...</td>" + 
                        (row < 3? ":" + String2.annotatedString(td1) : ""));
                completelySuccessful = false;
                po = nextRow;
                continue;
            }

            //ensure fileName matches fileNameRegex
            if (!content1.matches(fileNameRegex)) {
                po = nextRow;
                if (diagnosticMode) 
                    String2.log("=== skip this row: content1=" + content1 + 
                        " doesn't match fileNameRegex=" + fileNameRegex);
                continue;
            }

            //extract approximate size,   e.g., 70.03 Mbytes
            String sSize = String2.extractRegex(content2,
                "(\\d|\\.)+ (|K|M|G|T|P)bytes", 0);
            long lSize = Long.MAX_VALUE;
            if (sSize != null) {
                int spacePo = sSize.indexOf(' ');
                char ch = sSize.charAt(spacePo + 1);
                long times = ch == 'K'? Math2.BytesPerKB :
                             ch == 'M'? Math2.BytesPerMB :
                             ch == 'G'? Math2.BytesPerGB :
                             ch == 'T'? Math2.BytesPerTB :
                             ch == 'P'? Math2.BytesPerPB : 1;
                lSize = Math2.roundToLong(
                    String2.parseDouble(sSize.substring(0, spacePo)) * times);
            }

            //extract lastModified, e.g.,  2011-06-30 04:43:09Z
            String stime = String2.extractRegex(content3,
                "\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}", 0);  //space or T joining
            double dtime = Calendar2.safeIsoStringToEpochSeconds(stime);

            //add info to PrimitiveArrays
            childUrls.add(fileServerDir + content1); 
            lastModified.add(dtime);
            size.add(lSize);
            //String2.log("  file=" + fileName + "   " + stime);
            po = nextRow;
        }
    }

    /** 
     * This tests THREDDS-related methods.
     */
    public static void testThredds() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testThredds");
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = true;

      try {
        String url =  "https://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/monthly/"; //catalog.html
        String fileNameRegex = "sss_binned_L3_MON_SCI_V3.0_\\d{4}\\.nc";
        boolean recursive = true;
        String pathRegex = null;
        boolean dirsToo = true;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray fSize = new LongArray();

        //test TT_REGEX
        //note that String2.extractCaptureGroup fails if the string has line terminators
        Test.ensureEqual(String2.extractRegex("q\r\na<tt>b</tt>c\r\nz", TT_REGEX, 0),
            "<tt>b</tt>", "");

        //test error via addToThreddsUrlList  
        //(yes, logged message includes directory name)
        String2.log("\nIntentional error:");
        Test.ensureEqual(
            addToThreddsUrlList(url + "testInvalidUrl", fileNameRegex,  
                recursive, pathRegex, dirsToo, childUrls, lastModified, fSize),
            false, "");

        //test addToThreddsUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();  //epochSeconds
        fSize = new LongArray();
        addToThreddsUrlList(url, fileNameRegex,  recursive, pathRegex, 
            dirsToo, childUrls, lastModified, fSize);

        String results = childUrls.toNewlineString();
        String expected = 
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2011.nc\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2012.nc\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2013.nc\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2014.nc\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2015.nc\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = lastModified.toString();
        expected = 
"NaN, 1.405495932E9, 1.405492834E9, 1.405483892E9, 1.429802008E9, 1.429867829E9";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = fSize.toString();
        expected = 
"9223372036854775807, 2723152, 6528434, 6528434, 6528434, 1635779";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //test via oneStep -- dirs
        Table table = oneStep(url, fileNameRegex, recursive, pathRegex, true);
        results = table.dataToString();
        expected =    //lastMod is longs, epochMilliseconds
"directory,name,lastModified,size\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,,,\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2011.nc,1405495932000,2723152\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2012.nc,1405492834000,6528434\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2013.nc,1405483892000,6528434\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2014.nc,1429802008000,6528434\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2015.nc,1429867829000,1635779\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep -- no dirs
        table = oneStep(url, fileNameRegex, recursive, pathRegex, false);
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2011.nc,1405495932000,2723152\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2012.nc,1405492834000,6528434\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2013.nc,1405483892000,6528434\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2014.nc,1429802008000,6528434\n" +
"https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2015.nc,1429867829000,1635779\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\nUnexpected error."); 
      }
        reallyVerbose = oReallyVerbose;
        String2.log("\n*** FileVisitorDNLS.testThredds finished successfully");
    }


    /** 
     * This tests this class with the local file system. 
     */
    public static void testLocal(boolean doBigTest) throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testLocal");
        verbose = true;
        String contextDir = SSR.getContextDirectory(); //with / separator and / at the end
        String tPathRegex = null;
        Table table;
        long time;
        int n;
        String results, expected;

        //recursive and dirToo             and test \\ separator
        table = oneStep("c:\\erddapTest\\fileNames", ".*\\.png", true, tPathRegex, true); 
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"c:\\\\erddapTest\\\\fileNames\\\\,jplMURSST20150103090000.png,1421272444000,46482\n" +
"c:\\\\erddapTest\\\\fileNames\\\\,jplMURSST20150104090000.png,1420665738000,46586\n" +
"c:\\\\erddapTest\\\\fileNames\\\\sub\\\\,,1420732100000,0\n" +
"c:\\\\erddapTest\\\\fileNames\\\\sub\\\\,jplMURSST20150105090000.png,1420665704000,46549\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //recursive and !dirToo           and test // separator
        table = oneStep(String2.unitTestDataDir + "fileNames", ".*\\.png", true, tPathRegex, false);
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1421272444000,46482\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1420665738000,46586\n" +
String2.unitTestDataDir + "fileNames/sub/,jplMURSST20150105090000.png,1420665704000,46549\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //!recursive and dirToo
        table = oneStep(String2.unitTestDataDir + "fileNames", ".*\\.png", false, tPathRegex, true);
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1421272444000,46482\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1420665738000,46586\n" +
String2.unitTestDataDir + "fileNames/sub/,,1420732100000,0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //!recursive and !dirToo
        table = oneStep(String2.unitTestDataDir + "fileNames", ".*\\.png", false, tPathRegex, false);
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1421272444000,46482\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1420665738000,46586\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //***
        //oneStepDouble
        table = oneStepDouble(String2.unitTestDataDir + "fileNames", ".*\\.png", true, tPathRegex, true); 
        results = table.toString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 4 ;\n" +
"\tdirectory_strlen = 26 ;\n" +
"\tname_strlen = 27 ;\n" +
"variables:\n" +
"\tchar directory(row, directory_strlen) ;\n" +
"\t\tdirectory:ioos_category = \"Identifier\" ;\n" +
"\t\tdirectory:long_name = \"Directory\" ;\n" +
"\tchar name(row, name_strlen) ;\n" +
"\t\tname:ioos_category = \"Identifier\" ;\n" +
"\t\tname:long_name = \"File Name\" ;\n" +
"\tdouble lastModified(row) ;\n" +
"\t\tlastModified:ioos_category = \"Time\" ;\n" +
"\t\tlastModified:long_name = \"Last Modified\" ;\n" +
"\t\tlastModified:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n" +
"\tdouble size(row) ;\n" +
"\t\tsize:ioos_category = \"Other\" ;\n" +
"\t\tsize:long_name = \"Size\" ;\n" +
"\t\tsize:units = \"bytes\" ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1.421272444E9,46482.0\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1.420665738E9,46586.0\n" +
String2.unitTestDataDir + "fileNames/sub/,,1.4207321E9,0.0\n" +
String2.unitTestDataDir + "fileNames/sub/,jplMURSST20150105090000.png,1.420665704E9,46549.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //***
        //oneStepAccessibleViaFiles
        table = oneStepDoubleWithUrlsNotDirs(String2.unitTestDataDir + "fileNames", ".*\\.png", 
            true, tPathRegex,
            "http://localhost:8080/cwexperimental/files/testFileNames/");
        results = table.toString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 3 ;\n" +
"\turl_strlen = 88 ;\n" +
"\tname_strlen = 27 ;\n" +
"variables:\n" +
"\tchar url(row, url_strlen) ;\n" +
"\t\turl:ioos_category = \"Identifier\" ;\n" +
"\t\turl:long_name = \"URL\" ;\n" +
"\tchar name(row, name_strlen) ;\n" +
"\t\tname:ioos_category = \"Identifier\" ;\n" +
"\t\tname:long_name = \"File Name\" ;\n" +
"\tdouble lastModified(row) ;\n" +
"\t\tlastModified:ioos_category = \"Time\" ;\n" +
"\t\tlastModified:long_name = \"Last Modified\" ;\n" +
"\t\tlastModified:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n" +
"\tdouble size(row) ;\n" +
"\t\tsize:ioos_category = \"Other\" ;\n" +
"\t\tsize:long_name = \"Size\" ;\n" +
"\t\tsize:units = \"bytes\" ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"url,name,lastModified,size\n" +
"http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,1.421272444E9,46482.0\n" +
"http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150104090000.png,jplMURSST20150104090000.png,1.420665738E9,46586.0\n" +
"http://localhost:8080/cwexperimental/files/testFileNames/sub/jplMURSST20150105090000.png,jplMURSST20150105090000.png,1.420665704E9,46549.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //*** huge dir
        String unexpected = 
            "\nUnexpected FileVisitorDNLS error (but /data/gtspp/temp dir has variable nFiles):\n";

        if (doBigTest) {
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    //forward slash in huge directory
                    time = System.currentTimeMillis();
                    table = oneStep("/data/gtspp/temp", ".*\\.nc", false, tPathRegex, false); 
                    time = System.currentTimeMillis() - time;
                    //2014-11-25 98436 files in 410ms
                    StringArray directoryPA = (StringArray)table.getColumn(DIRECTORY);
                    String2.log("forward test: n=" + directoryPA.size() + " time=" + time + "ms");
                    if (directoryPA.size() < 1000) {
                        String2.log(directoryPA.size() + " files. Not a good test.");
                    } else {
                        Test.ensureBetween(time / (double)directoryPA.size(), 2e-3, 8e-3, 
                            "ms/file (4.1e-3 expected)");
                        String dir0 = directoryPA.get(0);
                        String2.log("forward slash test: dir0=" + dir0);
                        Test.ensureTrue(dir0.indexOf('\\') < 0, "");
                        Test.ensureTrue(dir0.endsWith("/"), "");
                    }
                } catch (Throwable t) {
                    String2.pressEnterToContinue(unexpected +
                        MustBe.throwableToString(t)); 
                }
            }

            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    //backward slash in huge directory
                    time = System.currentTimeMillis();
                    table = oneStep("\\data\\gtspp\\temp", ".*\\.nc", false, tPathRegex, false); 
                    time = System.currentTimeMillis() - time;
                    //2014-11-25 98436 files in 300ms
                    StringArray directoryPA = (StringArray)table.getColumn(DIRECTORY);
                    String2.log("backward test: n=" + directoryPA.size() + " time=" + time + "ms");
                    if (directoryPA.size() < 1000) {
                        String2.log(directoryPA.size() + " files. Not a good test.");
                    } else {
                        Test.ensureBetween(time / (double)directoryPA.size(), 1e-3, 8e-3,
                            "ms/file (3e-3 expected)");
                        String dir0 = directoryPA.get(0);
                        String2.log("backward slash test: dir0=" + dir0);
                        Test.ensureTrue(dir0.indexOf('/') < 0, "");
                        Test.ensureTrue(dir0.endsWith("\\"), "");
                    }
                } catch (Throwable t) {
                    String2.pressEnterToContinue(unexpected +
                        MustBe.throwableToString(t)); 
                }
            }
        }
        String2.log("\n*** FileVisitorDNLS.testLocal finished.");
    }

    /** 
     * This tests this class with Amazon AWS S3 file system. 
     * Your S3 credentials must be in 
     * <br> ~/.aws/credentials on Linux, OS X, or Unix
     * <br> C:\Users\USERNAME\.aws\credentials on Windows
     * See https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
     * See https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html
     * See https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html
     * See http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html .
     * https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-file-format
     */
    public static void testAWSS3() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testAWSS3");
        try {

        verbose = true;
        String contextDir = SSR.getContextDirectory(); //with / separator and / at the end
        Table table;
        long time;
        int n;
        String results, expected;
        //this works in browser: http://nasanex.s3.us-west-2.amazonaws.com
        //the full parent here doesn't work in a browser.
        //But ERDDAP knows that "nasanex" is the bucket name and 
        //  "NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/" is the prefix.
        //See https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html
        String parent = "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/";
        String child = "CONUS/";
        String pathRegex = null;

/* for releases, this line should have open/close comment */
        {

            //!recursive and dirToo
            table = oneStep(
                "https://nasanex.s3.us-west-2.amazonaws.com", 
                ".*", false, ".*", true); //fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
            results = table.dataToString();
            expected = 
"directory,name,lastModified,size\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/AVHRR/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/CMIP5/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/Landsat/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/LOCA/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/MAIAC/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/MODIS/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NAIP/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-GDDP/,,,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //!recursive and dirToo
            table = oneStep(
                "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/", 
                ".*", false, ".*", true); //fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
            results = table.dataToString();
            expected = 
"directory,name,lastModified,size\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,doi.txt,1380418295000,35\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,nex-dcp30-s3-files.json,1473288687000,2717227\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/CONTRIB/,,,\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/NEX-quartile/,,,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //!recursive and !dirToo
            table = oneStep(
                "https://nasanex.s3.us-west-2.amazonaws.com", 
                ".*", false, ".*", false); //fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
            results = table.dataToString();
            expected = 
"directory,name,lastModified,size\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //!recursive and !dirToo
            table = oneStep(
                "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/", 
                ".*", false, ".*", false); //fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
            results = table.dataToString();
            expected = 
"directory,name,lastModified,size\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,doi.txt,1380418295000,35\n" +
"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,nex-dcp30-s3-files.json,1473288687000,2717227\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //recursive and dirToo
            table = oneStep(parent, ".*\\.nc", true, pathRegex, true); 
            results = table.dataToString();
            expected = 
    "directory,name,lastModified,size\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/,,,\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
            if (expected.length() > results.length()) 
                String2.log("results=\n" + results);
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //recursive and !dirToo
            table = oneStep(parent, ".*\\.nc", true, pathRegex, false);
            results = table.dataToString();
            expected = 
    "directory,name,lastModified,size\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
            if (expected.length() > results.length()) 
                String2.log("results=\n" + results);
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //!recursive and dirToo
            table = oneStep(parent + child, ".*\\.nc", false, pathRegex, true);
            results = table.dataToString();
            expected = 
    "directory,name,lastModified,size\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
            if (expected.length() > results.length()) 
                String2.log("results=\n" + results);
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //!recursive and !dirToo
            table = oneStep(parent + child, ".*\\.nc", false, pathRegex, false);
            results = table.dataToString();
            expected = 
    "directory,name,lastModified,size\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
    "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
            if (expected.length() > results.length()) 
                String2.log("results=\n" + results);
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        } /* */

        //recursive and dirToo
        //reallyVerbose = true;
        //debugMode = true;
        parent = "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/";
        pathRegex = ".*"; 
        table = oneStep(parent, ".*\\.nc", true, pathRegex, true); 
        results = table.dataToString();
        expected = 
"directory,name,lastModified,size\n" +
"https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/,,,\n" +
"https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,,,\n" +
"https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381637189_e20183381639562_c20183381639596.nc,1543941659000,9269699\n" +
"https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381642189_e20183381644502_c20183381644536.nc,1543942123000,9585452\n" +
"https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381647189_e20183381649562_c20183381649596.nc,1543942279000,9894495\n" +
"https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381652189_e20183381654562_c20183381654595.nc,1543942520000,10195765\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);


        String2.log("\n*** FileVisitorDNLS.testAWSS3 finished.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error.  (Did you create your AWS S3 credentials file?)"); 
        }
    }

    /** 
     * This makes sure a local directory has the same contents as 
     * a remote directory, based on the lastModified time (not size,
     * since size of some remote sources is approximate).
     * If a file is in the localDir but not the remoteDir, it isn't deleted.
     * If a file in the localDir is newer than in remoteDir, it isn't changed.
     *
     * @param localDir need not currently exist. If doIt=true, it will be created,
     *    as will all needed subdir.
     * @param tPathRegex if tRecursive=true, this restricts which remoteDir 
     *    paths will be followed.
     *    Use null, "", or ".*" to get the default ".*".
     *    Creating these is very tricky. In general, it must start with .* and
     *    then a specific directory using forward slashes. 
     *    Each subsequent dir must be a capturing group
     *    with the option of nothing, e.g., "(|someDir/)". 
     *    It must start with the last part of the remoteDir and localDir, which must be the same.
     *    Use "(|[^/]+/)" to match anything at a given level.  Example:
     *    ".* /NMFS/(|NEFSC/)(|inport/)(|xml/)" (but remove the internal space which
     *    lets this javadoc comment continue).
     * @param fullSync if true, this does a fullSync. If not, it ensures there
     *    is at least 1 matching file. 
     * @return a Table with 2 String columns (remote, local)
     *    with the full names of the files which were (or need to be)
     *    copied, and the lastModified time of the remote file.
     * @throws Exception if trouble getting remote or local file info.
     *    Exceptions while downloading files are caught, logged,
     *    and not rethrown.
     */
    public static Table sync(String remoteDir, String localDir, 
        String tFileNameRegex, boolean tRecursive, String tPathRegex,
        boolean fullSync) throws Exception {

        String2.log("sync remote=" + remoteDir + " local=" + localDir + 
            " regex=" + tFileNameRegex + " recursive=" + tRecursive +
            " pathRegex=" + tPathRegex + " fullSync=" + fullSync);

        //use forward slashes and final slash so I can work consistently with the two dirs
        remoteDir = File2.addSlash(String2.replaceAll(remoteDir, '\\', '/'));
        localDir  = File2.addSlash(String2.replaceAll(localDir,  '\\', '/'));
        int rDirLen = remoteDir.length();
        int lDirLen = localDir.length();

        //get the remote and local file information
        Table rTable = oneStep(remoteDir, tFileNameRegex, tRecursive,
            tPathRegex, false); //dir too
        Table lTable = oneStep(localDir, tFileNameRegex, tRecursive,
            tPathRegex, false); //dir too
        rTable.leftToRightSort(2); //lexical sort, so can walk through below
        lTable.leftToRightSort(2); //lexical sort, so can walk through below
        //String2.log("\nremote table (max of 10)\n" + rTable.dataToString(10) +
        //            "\nlocal  table (max of 10)\n" + lTable.dataToString(10));

        StringArray rDir     = (StringArray)rTable.getColumn(DIRECTORY);
        StringArray lDir     = (StringArray)lTable.getColumn(DIRECTORY);
        StringArray rName    = (StringArray)rTable.getColumn(NAME);
        StringArray lName    = (StringArray)lTable.getColumn(NAME);
        LongArray   rLastMod = (  LongArray)rTable.getColumn(LASTMODIFIED);
        LongArray   lLastMod = (  LongArray)lTable.getColumn(LASTMODIFIED);

        //make a table for the results
        StringArray remoteSA  = new StringArray();
        StringArray localSA   = new StringArray();
        LongArray   lastModLA = new LongArray();
        Table outTable = new Table();
        outTable.addColumn("remote",     remoteSA);
        outTable.addColumn("local",      localSA);
        outTable.addColumn(LASTMODIFIED, lastModLA);

        //walk through both tables, dealing with nextr and nextl items
        int nr = rDir.size();
        int nl = lDir.size();
        int nextr = 0;
        int nextl = 0;
        int nTooRecent = 0, nExtraLocal = 0, nAlready = 0;
        while (nextr < nr || nextl < nl) {
            if (nextr >= nr) {
                //no more remote files
                //delete this local file that doesn't exist at the source?
                String2.log("  WARNING: localFile=" + lDir.get(nextl) + lName.get(nextl) +
                    " has no remote counterpart at " + remoteDir);
                nExtraLocal++;
                nextl++;
                continue;
            }
            String rFullName = rDir.get(nextr) + rName.get(nextr);
            if (nextl >= nl) {
                //no more local files: so download this remote file
                remoteSA.add(rFullName);
                localSA.add(localDir + rFullName.substring(rDirLen));
                lastModLA.add(rLastMod.get(nextr));
                nextr++;
                continue;
            }
            String lFullName = lDir.get(nextl) + lName.get(nextl);

            //find which of nextLocal or nextRemote has lower relativeDir+name
            int compare = rFullName.substring(rDirLen).compareTo(
                lFullName.substring(lDirLen));
            if (compare == 0 && rLastMod.get(nextr) < lLastMod.get(nextl)) { 
                //newer local version!
                String2.log("  WARNING: localFile=" + 
                    lFullName + "(" + lLastMod.get(nextl) +
                    ") is newer than comparable remoteFile=" + 
                    rFullName + "(" + rLastMod.get(nextr) + ")");
                nTooRecent++;
                nextr++;
                nextl++;
            } else if (compare < 0 || //new remote file
                (compare == 0 && rLastMod.get(nextr) > lLastMod.get(nextl))) { //newer remote version
                //new remote file: so download it
                remoteSA.add(rFullName);
                localSA.add(localDir + rFullName.substring(rDirLen));
                lastModLA.add(rLastMod.get(nextr));
                nextr++;
                if (compare == 0)
                    nextl++;
            } else if (compare == 0) {
                //times are same, so do nothing
                nAlready++;
                nextr++;
                nextl++;
            } else { //compare > 0: local file is unexpected. Delete it?
                String2.log("  WARNING: localFile=" + lFullName +
                    " has no remote counterpart at " + remoteDir);
                nExtraLocal++;
                nextl++;
            }
        }
        int n = remoteSA.size();

        StringArray downloadFailures = new StringArray();
        int nTry = fullSync? n : 
            nAlready > 0? 0 : Math.min(1, n) ;
        int nSuccess = 0;
        for (int i = 0; i < nTry; i++) {
            try {
                String2.log("sync is downloading #" + (i+1) + " of nTry=" + nTry);
                SSR.downloadFile("sync", remoteSA.get(i), localSA.get(i), true); //try to compress
                File2.setLastModified(localSA.get(i), lastModLA.get(i));
                nSuccess++;
            } catch (Throwable t) {
                downloadFailures.add(remoteSA.get(i));
                String2.log(MustBe.throwableToString(t));
            }
        }       

        String2.log("finished sync: found nAlready=" + nAlready + " nAvailDownload=" + n +
            " nTooRecent=" + nTooRecent + " nExtraLocal=" + nExtraLocal + 
            " nTry=" + nTry + " nSuccess=" + nSuccess);
        if (fullSync)
            String2.log("nDownloadFailures=" + downloadFailures.size() + ":\n" +
                downloadFailures.toNewlineString());

        return outTable;
    }
        
    /**
     * This tests sync().
     */
    public static void testSync() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testSync");
        String rDir = "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/";
        String lDir = String2.unitTestDataDir + "sync/NMFS";  //test without trailing slash 
        String fileRegex = "22...\\.xml";
        boolean recursive = true;
        //first part of pathRegex must be the last part of the rDir and the lDir, e.g., NMFS
        //pre 2016-03-04 I tested NWFSC/inport-xml/xml, but it has been empty for a month!
        String pathRegex = ".*/NMFS/(|NEFSC/)(|inport-xml/)(|xml/)"; //tricky!            
        boolean doIt = true;

        //get original times
        String name22560 = lDir + "/NEFSC/inport-xml/xml/22560.xml";
        String name22565 = lDir + "/NEFSC/inport-xml/xml/22565.xml";

        long time22560 = File2.getLastModified(name22560);
        long time22565 = File2.getLastModified(name22565);

        try {
            //For testing purposes, I put one extra file in the dir: 22561.xml renamed as 22ext.xml

            //do the sync
            sync(rDir, lDir, fileRegex, recursive, pathRegex, true);

            //current date on all these files
            //Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1455948120),
            //    "2016-02-20T06:02:00Z", ""); //what the website shows for many

            //delete 1 local file
            File2.delete(lDir + "/NEFSC/inport-xml/xml/22563.xml");

            //make 1 local file older
            File2.setLastModified(name22560, 100);

            //make 1 local file newer
            File2.setLastModified(name22565, System.currentTimeMillis() + 100);
            Math2.sleep(500);

            //test the sync 
            Table table = sync(rDir, lDir, fileRegex, recursive, pathRegex, doIt);
            String2.pressEnterToContinue("\nCheck above to ensure these numbers:\n" +
                "\"found nAlready=3 nToDownload=2 nTooRecent=1 nExtraLocal=1\"\n");
            String results = table.dataToString();
            String expected = //the lastModified values change periodically
//these are the files which were downloaded
"remote,local,lastModified\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/22560.xml,/erddapTest/sync/NMFS/NEFSC/inport-xml/xml/22560.xml,1557568922000\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/22563.xml,/erddapTest/sync/NMFS/NEFSC/inport-xml/xml/22563.xml,1554803962000\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //no changes, do the sync again
            table = sync(rDir, lDir, fileRegex, recursive, pathRegex, doIt);
            String2.pressEnterToContinue("\nCheck above to ensure these numbers:\n" +
                "\"found nAlready=5 nToDownload=0 nTooRecent=1 nExtraLocal=1\"\n");
            results = table.dataToString();
            expected = 
    "remote,local,lastModified\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) +
                "Often, there are minor differences."); 

        } finally {
            File2.setLastModified(name22560, time22560);
            File2.setLastModified(name22565, time22565);
        }

    }

    /** 
     * This looks in all the specified files until a file with a line that
     * matches lineRegex is found.
     *
     * @param tallyWhich if &gt;= 0, this tabulates the values of the
     *   tallyWhich-th capture group in the lineRegex.
     * @param interactiveNLines if &gt;0, this shows the file, 
     *   the matching line and nLines thereafter, and calls pressEnterToContinue().
     *   If false, this returns the name of the first file matching lineRegex.
     * @return this returns the first matching fileName, or null if none
     */
    public static String findFileWith(String tDir, String tFileNameRegex, 
        boolean tRecursive, String tPathRegex, String lineRegex, 
        int tallyWhich, int interactiveNLines, int showTopN) throws Throwable {
        String2.log("\n*** findFileWith(\"" + lineRegex + "\")");
        Table table = oneStep(tDir, tFileNameRegex, tRecursive,
            tPathRegex, false);
        StringArray dirs  = (StringArray)table.getColumn(DIRECTORY);
        StringArray names = (StringArray)table.getColumn(NAME);
        Tally tally = tallyWhich >= 0? new Tally() : null;
        int nFiles = names.size();
        Pattern linePattern = Pattern.compile(lineRegex);
        String firstFullName = null;
        int lastFileiMatch = -1;
        int nFilesMatched = 0;
        int nLinesMatched = 0;
        for (int filei = 0; filei < nFiles; filei++) {
            if (filei % 100 == 0)
                String2.log("file#" + filei + " nFilesMatched=" + nFilesMatched + 
                    " nLinesMatched=" + nLinesMatched);
            try {
                String fullName = dirs.get(filei) + names.get(filei);
                ArrayList<String> lines = SSR.getUrlResponseArrayList(fullName);
                int nLines = lines.size();
                for (int linei = 0; linei < nLines; linei++) {
                    Matcher matcher = linePattern.matcher(lines.get(linei));
                    if (matcher.matches()) {
                        nLinesMatched++;
                        if (lastFileiMatch != filei) {
                            lastFileiMatch = filei;
                            nFilesMatched++;
                        }
                        if (firstFullName == null)
                            firstFullName = fullName;
                        if (tallyWhich >= 0)
                            tally.add(lineRegex, matcher.group(tallyWhich));
                        if (interactiveNLines > 0) {
                            String msg = "\n***** Found match in fileName#" + filei + 
                                "=" + fullName + " on line#" + linei;
                            String2.log(msg + ". File contents="); 
                            String2.log(String2.toNewlineString(lines.toArray(new String[0])));
                            String2.log("\n" + msg + ":\n");
                            for (int tl = linei; tl < Math.min(linei + interactiveNLines + 1, nLines); tl++)
                                String2.log(lines.get(tl));
                            String2.pressEnterToContinue();
                        } else if (tallyWhich < 0) {
                            return firstFullName;
                        } 
                    }
                }
            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }
        }
        String2.log("\nfindFileWhich finished successfully. nFiles=" + nFiles +
            " nFilesMatched=" + nFilesMatched + " nLinesMatched=" + nLinesMatched);
        if (tallyWhich >= 0)
            String2.log(tally.toString(showTopN)); //most common n will be shown
        return firstFullName;
    }


    /** 
     * This makes a .tgz or .tar.gz file.
     *
     * @param tResultName is the full result file name, usually 
     *   the name of the dir being archived, and ending in .tgz or .tar.gz.
     */
    public static void makeTgz(String tDir, String tFileNameRegex, 
        boolean tRecursive, String tPathRegex, String tResultName) throws Exception {
        TarArchiveOutputStream tar = null;
        String outerDir = File2.getDirectory(tDir.substring(0, tDir.length() - 1));
        tar = new TarArchiveOutputStream(new GZIPOutputStream(
            new BufferedOutputStream(new FileOutputStream(tResultName))));
        try {

            // Add data to out and flush stream
            Table filesTable = oneStep(tDir, tFileNameRegex, tRecursive, 
                tPathRegex, false); //tDirectoriesToo
            StringArray directoryPA    = (StringArray)filesTable.getColumn(DIRECTORY);
            StringArray namePA         = (StringArray)filesTable.getColumn(NAME);
            LongArray   lastModifiedPA = (  LongArray)filesTable.getColumn(LASTMODIFIED);
            LongArray   sizePA         = (  LongArray)filesTable.getColumn(SIZE);
            byte buffer[] = new byte[32768];
            int nBytes;
            for (int fi = 0; fi < namePA.size(); fi++) {
                String fullName = directoryPA.get(fi) + namePA.get(fi);
                TarArchiveEntry entry = new TarArchiveEntry(
                    new File(fullName.substring(outerDir.length())));
                entry.setSize(sizePA.get(fi));
                entry.setModTime(lastModifiedPA.get(fi));
                tar.putArchiveEntry(entry);
                InputStream fis = new BufferedInputStream(new FileInputStream(fullName)); //not File2.getDecompressedBufferedInputStream(). Read file as is.
                try {
                    while ((nBytes = fis.read(buffer)) > 0) 
                        tar.write(buffer, 0, nBytes);
                } finally {
                    fis.close();                    
                }
                tar.closeArchiveEntry();
            }                 
        } finally {
            tar.close();
        }
    }

    /** 
     * This tests makeTgz.
     */
    public static void testMakeTgz() throws Exception {
        String2.log("\n*** FileVisitorDNLS.testMakeTgz");
        
        String tgzName = String2.unitTestDataDir + "testMakeTgz.tar.gz";
        try {
            makeTgz(String2.unitTestDataDir + "fileNames/", ".*", true, ".*",
                tgzName);
            SSR.displayInBrowser("file://" + tgzName);  //works with .tar.gz, not .tgz
            String2.pressEnterToContinue("Are the contents of the .tar.gz file okay?");
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t));
        }
        File2.delete(tgzName);
    }

    /**
     * This makes a String representation of the directories, fileNames, sizes,
     * and lastModified times.
     *
     * @param relativeDirectoryNames if true, the directory names shown will 
     *    be relative to tDir.
     * @return a String representation of the directories, fileNames, sizes,
     *   and lastModified times.  Most lines will have 99 characters.
     */
    public static String oneStepToString(String tDir, String tFileNameRegex, 
        boolean tRecursive, String tPathRegex) throws IOException {

        StringBuilder sb = new StringBuilder();
        tDir = File2.addSlash(tDir);
        String slash = tDir.indexOf('/') >= 0? "/" : "\\";

        Table table = oneStep(tDir, tFileNameRegex, tRecursive,
            tPathRegex, true); //directoriesToo
        StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
        StringArray namePA         = (StringArray)table.getColumn(NAME);
        LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
        LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);
        int nRows = table.nRows();
        StringBuilder spaces = new StringBuilder();
        for (int row = 0; row < nRows; row++) {
            String cDir  = directoryPA.get(row);
            String cName = namePA.get(row);
            long   cTime = lastModifiedPA.get(row);
            long   cSize = sizePA.get(row);

            String relDir = cDir.substring(Math.min(tDir.length(), cDir.length()));
            int nSpaces = String2.countAll(relDir, slash) * 2;
            if (spaces.length() > nSpaces) {
                spaces.setLength(nSpaces);
            } else while (spaces.length() < nSpaces) {
                spaces.append(' ');
            }

            if (cName.length() == 0) {
                //a directory
                sb.append(spaces.substring(0, Math.max(0, nSpaces-2)) + relDir + "\n");
            } else {
                sb.append(String2.left(spaces + cName, 64)); //64
                sb.append(' '); //1                   
                sb.append(String2.left(
                    cTime == Long.MAX_VALUE? "" :
                        Calendar2.epochSecondsToIsoStringTZ(cTime / 1000), 
                    21));  //21
                sb.append(' ');  //1
                sb.append(String2.right(
                    cSize == Long.MAX_VALUE? "" : "" + cSize, 12)); //just <1TB //12
                sb.append('\n');
            }   
        }

        return sb.toString();
    }

    /**
     * This tests oneStepToString().
     */
    public static void testOneStepToString() throws Throwable {
        
        String2.log("\n*** FileVisitorDNLS.testOneStepToString()");

        String results = oneStepToString(
            String2.unitTestDataDir + "CFPointConventions/timeSeries/", ".*", true, ".*");
        String expected = 
//this tests that all files are before all dirs
"timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.rb 2012-11-01T18:16:26Z          5173\n" +
"timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.rb 2012-11-01T18:16:26Z          2979\n" +
"timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2/\n" +
"  README                                                         2012-11-01T18:16:26Z            87\n" +
"  timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.cdl 2012-11-01T18:16:26Z          1793\n" +
"  timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.nc 2012-11-01T18:16:26Z          4796\n" +
"  timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.ncml 2012-11-01T18:16:26Z          3026\n" +
"timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/\n" +
"  README                                                         2012-11-01T18:16:26Z           538\n" +
"  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.cdl 2012-11-01T18:16:26Z          1515\n" +
"  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.nc 2012-11-01T18:16:26Z         10436\n" +
"  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.ncml 2012-11-01T18:16:26Z          2625\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }
        
    /**
     * This tests pathRegex().
     */
    public static void testPathRegex() throws Throwable {
        
        String2.log("\n*** FileVisitorDNLS.testPathRegex()");

        String results = oneStepToString(    
            String2.unitTestDataDir + "CFPointConventions/timeSeries/", 
                ".*", true,         //all files
                ".*H\\.2\\.1.*");   //but only H.2.1 dirs
        String expected = 
"timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.rb 2012-11-01T18:16:26Z          5173\n" +
"timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.rb 2012-11-01T18:16:26Z          2979\n" +
"timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/\n" +
"  README                                                         2012-11-01T18:16:26Z           538\n" +
"  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.cdl 2012-11-01T18:16:26Z          1515\n" +
"  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.nc 2012-11-01T18:16:26Z         10436\n" +
"  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.ncml 2012-11-01T18:16:26Z          2625\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }
        
    /**
     * This tests following symbolic links / soft links.
     * THIS DOESN'T WORK on Windows, because Java doesn't follow Windows .lnk's.
     *  Windows links are not easily parsed files. It would be hard to add support
     *  for .lnk's to this class.
     * This class now works as expected with symbolic links on Linux.
     *   I tested manually on coastwatch Linux with FileVisitorDNLS.sh and main() below and
     *   ./FileVisitorDNLS.sh /u00/satellite/MUR41/anom/1day/ ....0401.\* 
     */
    public static void testSymbolicLinks() throws Throwable {
        
        String2.log("\n*** FileVisitorDNLS.testSymbolicLinks()");
        boolean oDebugMode = debugMode;
        debugMode = true;

        String results = oneStepToString(
            "/u00/satellite/MUR41/anom/1day/", ".*", true, ".*");
        String expected = 
//2002 are files. 2003 is a shortcut to files
"zztop\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        debugMode = oDebugMode;
    }
        
    /**
     * This tallys the contents of the specified XML findTags in the specified files
     * and returns the Tally object.
     */
    public static Tally tallyXml(String dir, String fileNameRegex, boolean recursive,
        String findTags[]) throws Exception {

        Table table = oneStep(dir, fileNameRegex, recursive,
            ".*", false); //tRecursive, tPathRegex, tDirectoriesToo
        StringArray dirs  = (StringArray)table.getColumn(FileVisitorDNLS.DIRECTORY);
        StringArray names = (StringArray)table.getColumn(FileVisitorDNLS.NAME);
        Tally tally = new Tally();
        int nErrors = 0;
        int nFindTags = findTags.length;
        for (int i = 0; i < names.size(); i++) {

            String2.log("reading #" + i + ": " + dirs.get(i) + names.get(i));
            SimpleXMLReader xmlReader = null;
            try {
                xmlReader = new SimpleXMLReader(
                    File2.getDecompressedBufferedInputStream(dirs.get(i) + names.get(i))); 
                while (true) {
                    xmlReader.nextTag();
                    String tags = xmlReader.allTags();
                    if (tags.length() == 0) 
                        break;
                    for (int t = 0; t < nFindTags; t++) {
                        if (tags.equals(findTags[t]))
                            tally.add(findTags[t], xmlReader.content());
                    }
                }
                xmlReader.close();
                xmlReader = null;

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
                nErrors++;
                if (xmlReader != null)
                    xmlReader.close();
            }
        }
        String2.log("\n*** tallyXml() finished. nErrors=" + nErrors);
        return tally;
    }

    /**
     * This finds the file names where an xml findTags matches a regex.
     */
    public static void findMatchingContentInXml(String dir, String fileNameRegex, boolean recursive,
        String findTag, String matchRegex) throws Exception {

        Table table = oneStep(dir, fileNameRegex, recursive,
            ".*", false); //tRecursive, tPathRegex, tDirectoriesToo
        StringArray dirs  = (StringArray)table.getColumn(FileVisitorDNLS.DIRECTORY);
        StringArray names = (StringArray)table.getColumn(FileVisitorDNLS.NAME);
        Tally tally = new Tally();
        int nErrors = 0;
        for (int i = 0; i < names.size(); i++) {

            SimpleXMLReader xmlReader = null;
            try {
                String2.log("reading #" + i + ": " + dirs.get(i) + names.get(i));
                xmlReader = new SimpleXMLReader(
                    File2.getDecompressedBufferedInputStream(dirs.get(i) + names.get(i)));
                while (true) {
                    xmlReader.nextTag();
                    String tags = xmlReader.allTags();
                    if (tags.length() == 0) 
                        break;
                    if (tags.equals(findTag) &&
                        xmlReader.content().matches(matchRegex))
                        String2.log(dirs.get(i) + names.get(i) + " has \"" + xmlReader.content() + "\"");
                }
                xmlReader.close();

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
                nErrors++;
                if (xmlReader != null)
                    xmlReader.close();
            }
        }
        String2.log("\n*** findMatchingContentInXml() finished. nErrors=" + nErrors);
    }


    /** 
     * A variant of reduceDnlsTableToOneDir that doesn't use subdirHash
     * and returns the sorted subDir short names as a String[].
     */
    public static String[] reduceDnlsTableToOneDir(Table dnlsTable, String oneDir) {
        HashSet<String> subdirHash = new HashSet();
        reduceDnlsTableToOneDir(dnlsTable, oneDir, subdirHash);

        String subDirs[] = (String[])(subdirHash.toArray(new String[0]));
        Arrays.sort(subDirs, String2.STRING_COMPARATOR_IGNORE_CASE);
        return subDirs;
    }

    /** 
     * This prunes a DNLS table so that it just includes file entries from one directory
     * and returns subDirs (the short names, e.g., mySubDir) .
     * This just looks at and works with col#0=directory and col#1=name.
     * This doesn't look at or use lastModified or size, so they can be doubles or longs (or anything).
     * 
     * @param dnlsTable Before, it will have file entries (and optionally, dir entries).
     *   Afterwards, the dnlsTable will have just 1 value in directory column (oneDir)
     *   and will be sorted by the name column.
     * @param oneDir the full path name (ending in slash) of the directory to be matched 
     * @param subdirHash to collect (additional) subsir (short) names. 
     */
    public static void reduceDnlsTableToOneDir(Table dnlsTable, String oneDir, HashSet<String> subdirHash) {
        int nRows = dnlsTable.nRows();
        if (nRows == 0)
            return;
        char separator = oneDir.indexOf('\\') >= 0? '\\' : '/';
        StringArray dirSA  = (StringArray)dnlsTable.getColumn(0);
        StringArray nameSA = (StringArray)dnlsTable.getColumn(1);
        int oneDirLength = oneDir.length();
        BitSet keep = new BitSet(nRows);  //all false
        for (int row = 0; row < nRows; row++) {
            String tDir = dirSA.get(row);
            if (tDir.startsWith(oneDir)) {
                if (tDir.length() == oneDirLength) {
                    if (nameSA.get(row).length() > 0) 
                        keep.set(row);
                } else { //tDir startsWith(oneDir) but is longer
                    //add next-level directory name
                    subdirHash.add(tDir.substring(oneDirLength, tDir.indexOf(separator, oneDirLength))); 
                }
            }
        }
        dnlsTable.justKeep(keep);
        dnlsTable.sortIgnoreCase(new int[]{1}, new boolean[]{true});
        //String2.log(">> reduceDnlsTabletoOneDir nRows=" + dnlsTable.nRows() + " nSubdir=" + subdirHash.size());
    }
   
    /** 
     * This tests reduceDnlsTableToOneDir(). 
     */
    public static void testReduceDnlsTableToOneDir() throws Exception {
        String2.log("\n*** FileVisitorDNLS.testReduceDnlsTableToOneDir\n");
        String tableString = 
            "directory, name, lastModified, size\n" +
            "/u00/, , , \n" +
            "/u00/, nothing, 60, 1\n" +
            "/u00/a/, , , \n" +
            "/u00/a/, AA, 60, 2\n" +
            "/u00/a/, A, 60, 3\n" +
            "/u00/a/q/,  ,   , \n" +
            "/u00/a/q/, D, 60, 4\n" +
            "/u00/a/b/, B, 60, 5\n" +
            "/u00/a/b/c/, C, 60, 6\n";
        Table table = new Table();
        table.readASCII("testReduceDnlsTableToOneDir", 
            new BufferedReader(new StringReader(tableString)),
            0, 1, ",", null, null, null, null, true);
        String subDirs[] = reduceDnlsTableToOneDir(table, "/u00/a/");

        String results = table.dataToString();
        String expected = 
"directory,name,lastModified,size\n" +
"/u00/a/,A,60,3\n" +   //files are sorted, dirs are removed
"/u00/a/,AA,60,2\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = String2.toCSSVString(subDirs);
        expected = "b, q";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }

   /**
     * This is used for testing this class.
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the question.
     */
    public static void main(String args[]) throws Throwable {
        verbose = true; 
        reallyVerbose = true;
        debugMode = true;
        if (args == null || args.length < 2) {
            String2.log("Usage: FileVisitorDNLS startingDirectory fileNameRegex");
        } else {
            Table table = oneStep(args[0], args[1], 
                true, ".*", true); //tRecursive, tPathRegex, tDirectoriesToo
            String2.log(table.dataToString());
        }
        System.exit(0);
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean doBigTest) throws Throwable {
        String2.log("\n****************** FileVisitorDNLS.test() *****************\n");
/* for releases, this line should have open/close comment */
        //always done        
        testLocal(doBigTest);
        testAWSS3(); 
        testHyrax();
        testHyraxMUR();
        testThredds();
        testErddapFilesWAF();  
        testSync();
        testMakeTgz();
        testOneStepToString();
        testPathRegex();
        testReduceDnlsTableToOneDir();
        /* */

        //testSymbolicLinks(); //THIS TEST DOESN'T WORK on Windows, but links are followed on Linux

        //future: FTP? ftp://ftp.unidata.ucar.edu/pub/
    }


}
