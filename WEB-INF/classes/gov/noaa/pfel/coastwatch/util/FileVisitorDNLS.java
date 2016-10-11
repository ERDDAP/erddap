/* 
 * FileVisitorDNLS Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
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

    public final static String URL          = "url"; //in place of directory for oneStepAccessibleViaFiles

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

    /** table.dataToCSVString(); */
    public String resultsToString() {
        return table.dataToCSVString();
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
     * @return a table with columns with DIRECTORY, NAME, LASTMODIFIED, and SIZE columns.
     *    LASTMODIFIED and SIZE are LongArrays -- For directories when the values
     *    are otherwise unknown, the value will be Long.MAX_VALUE.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's file NAME will be "".
     * @throws IOException if trouble
     */
    public static Table oneStep(String tDir, String tFileNameRegex, boolean tRecursive,
        String tPathRegex, boolean tDirectoriesToo) throws IOException {
        long time = System.currentTimeMillis();

        //is tDir an http URL?
        if (tDir.matches(FileVisitorDNLS.HTTP_REGEX)) {

            //Is it an S3 bucket with "files"?
            //If testing a "dir", url should have a trailing slash.
            Matcher matcher = AWS_S3_PATTERN.matcher(File2.addSlash(tDir)); //force trailing slash
            if (matcher.matches()) {
                //http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html
                //If files have file-system-like names, e.g., 
                //  http://bucketname.s3.amazonaws.com/dir1/dir2/fileName.ext)
                //  http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_NorESM1-M_209601-209912.nc
                //  you still can't request just dir2 info because they aren't directories.
                //  They are just object keys with internal slashes. 
                //So specify prefix in request.
                Table table = makeEmptyTable();
                StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                StringArray namePA         = (StringArray)table.getColumn(NAME);
                LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                String bucketName = matcher.group(1); 
                String prefix = matcher.group(2); 
                String baseURL = tDir.substring(0, matcher.start(2));
                AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());
                try {
                    if (verbose) 
                        String2.log("FileVisitorDNLS.oneStep getting info from AWS S3 at" + 
                            "\nURL=" + tDir);
                            //"\nbucket=" + bucketName + " prefix=" + prefix);

                    //I wanted to generate lastMod for dir based on lastMod of files
                    //but it would be inconsistent for different requests (recursive, fileNameRegex).
                    //so just a set of dir names.
                    HashSet<String> dirHashSet = new HashSet(); 
                    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(prefix);
                    ObjectListing objectListing;                     
                    do {
                        objectListing = s3client.listObjects(listObjectsRequest);
                        for (S3ObjectSummary objectSummary : 
                            objectListing.getObjectSummaries()) {
                            String keyFullName = objectSummary.getKey();
                            String keyDir = File2.getDirectory(baseURL + keyFullName);
                            String keyName = File2.getNameAndExtension(keyFullName);
                            if (debugMode) String2.log("keyFullName=" + keyFullName +
                                "\nkeyDir=" + keyDir +
                                "\n  tDir=" + tDir);
                            if (keyDir.startsWith(tDir) && //it should
                                (tRecursive || keyDir.length() == tDir.length())) {

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
                                //Sometimes directories appear as files are named "" with size=0.
                                //I don't store those as files.
                                if (debugMode) String2.log("keyName=" + keyFullName +
                                          "\n tFileNameRegex=" + tFileNameRegex + " matches=" + keyName.matches(tFileNameRegex));
                                if (keyName.length() > 0 && keyName.matches(tFileNameRegex)) {
                                    directoryPA.add(keyDir);
                                    namePA.add(keyName);
                                    lastModifiedPA.add(objectSummary.getLastModified().getTime()); //epoch millis
                                    sizePA.add(objectSummary.getSize()); //long
                                }
                            }
                        }
                        listObjectsRequest.setMarker(objectListing.getNextMarker());
                    } while (objectListing.isTruncated());

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

                } catch (AmazonServiceException ase) {
                    throw new IOException("AmazonServiceException: " + 
                        ase.getErrorType() + " ERROR, HTTP Code=" + ase.getStatusCode() + 
                        ": " + ase.getMessage(), ase);
                } catch (AmazonClientException ace) {
                    throw new IOException(ace.getMessage(), ace);
                }
            }

            //HYRAX before THREDDS
            //http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/
            matcher = HYRAX_PATTERN.matcher(tDir); 
            if (matcher.matches()) {
                try {
                    Table table = makeEmptyTable();
                    StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                    StringArray namePA         = (StringArray)table.getColumn(NAME);
                    LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                    LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                    DoubleArray lastModDA = new DoubleArray();
                    addToHyraxUrlList(tDir, tFileNameRegex, tRecursive, tPathRegex,
                        tDirectoriesToo, namePA, lastModDA, sizePA);
                    lastModifiedPA.append(lastModDA);
                    int n = namePA.size();
                    for (int i = 0; i < n; i++) {
                        String fn = namePA.get(i);
                        directoryPA.add(File2.getDirectory(fn));
                        namePA.set(i, File2.getNameAndExtension(fn));
                    }

                    table.leftToRightSortIgnoreCase(2);
                    return table;
                } catch (Throwable t) {
                    throw new IOException(t.getMessage(), t);
                }
            }

            //THREDDS
            matcher = THREDDS_PATTERN.matcher(tDir); 
            if (matcher.matches()) {
                try {
                    Table table = makeEmptyTable();
                    StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                    StringArray namePA         = (StringArray)table.getColumn(NAME);
                    LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                    LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                    DoubleArray lastModDA = new DoubleArray();
                    addToThreddsUrlList(tDir, tFileNameRegex, tRecursive, tPathRegex,
                        tDirectoriesToo, namePA, lastModDA, sizePA);
                    lastModifiedPA.append(lastModDA);
                    int n = namePA.size();
                    for (int i = 0; i < n; i++) {
                        String fn = namePA.get(i);
                        directoryPA.add(File2.getDirectory(fn));
                        namePA.set(i, File2.getNameAndExtension(fn));
                    }

                    table.leftToRightSortIgnoreCase(2);
                    return table;
                } catch (Throwable t) {
                    throw new IOException(t.getMessage(), t);
                }
            }

            //default: Apache-style WAF
            try {
                Table table = makeEmptyTable();
                StringArray directorySA = (StringArray)table.getColumn(DIRECTORY);
                StringArray nameSA      = (StringArray)table.getColumn(NAME);
                LongArray   lastModLA   = (  LongArray)table.getColumn(LASTMODIFIED);
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
        if (verbose) String2.log("FileVisitorDNLS.oneStep(local) finished successfully. n=" + 
            fv.directoryPA.size() + " time=" +
            (System.currentTimeMillis() - time));
        return fv.table;
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
     *    LASTMODIFIED (double epochSeconds), and SIZE (doubles) columns.
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
                    da.add(la.get(row) / 1000.0);
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
     * This is like oneStepDouble (a convenience method for using this class)
     * but returns a url column instead of a directory column. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     * @param startOfUrl usually EDStatic.erddapUrl(loggedInAs) + "/files/" + datasetID() + "/"
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
        ".*>Name</a>.*>Last modified</a>.*>Size</a>.*>Description</a>.*");

//inport:
//<tr><td valign="top"><img src="/icons/back.gif" alt="[DIR]"></td><td>
//  <a href="/waf/NOAA/NMFS/">Parent Directory</a>       </td><td>&nbsp;</td>
//  <td align="right">  - </td><td>&nbsp;</td></tr>
//erddap:
//<img src="..." alt="[DIR]" align="absbottom"> <a href="0&#x2f;">0</a>
    public final static Pattern wafDirPattern = Pattern.compile(
        ".* alt=\"\\[DIR\\]\".*>.*<a.*>(.*?)</a>.*");

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


    public static String[] getUrlsFromWAF(String startUrl, String fileNameRegex, 
        boolean recursive, String pathRegex) throws Throwable {
        if (verbose) String2.log("getUrlsFromHyraxCatalog fileNameRegex=" + fileNameRegex);

        //call the recursive method
        boolean tDirectoriesToo = false;
        StringArray dirs = new StringArray();
        StringArray names = new StringArray();
        LongArray lastModified = new LongArray();
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
http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/
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
        InputStream is = null; 
        BufferedReader in = null;
        try {
            if (reallyVerbose) String2.log("\naddToWAFUrlList nNames=" + names.size() + 
                " url=" + url); 
            url = File2.addSlash(url);

            //for html, any charset should be fine. 
            //All non-ASCII chars should be entities.
            //But use common Linux to be consistent.
            is = SSR.getUrlInputStream(url); 
            in = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
            String s;

            //look for header line
            while ((s = in.readLine()) != null) {
                if (wafHeaderPattern.matcher(s).matches())
                    break; 
            }

            //read the lines with dir or file info
            while ((s = in.readLine()) != null) {

                //look for dirs before files (since dirs match filePattern, too)
                Matcher matcher = wafDirPattern.matcher(s); 
                if (matcher.matches()) {
                    String name = XML.decodeEntities(matcher.group(1));
                    if ("Parent Directory".equals(name))
                        continue;
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
                if (matcher.matches()) {
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
                    }
                    continue;
                } else {
                    if (debugMode) String2.log("matches=false: " + s);
                }
            }

        } catch (Exception e) {
            String2.log(String2.ERROR + " from url=" + url + " :\n" + 
                MustBe.throwableToString(e));
            completelySuccessful = false;

        } finally {
            try {
                if (in != null)
                    in.close();  //'in' will close 'is'.
                else if (is != null)
                    is.close();
            } catch (Throwable t) {
            }
        }
        return completelySuccessful;

    }



    /**
     * This tests WAF-related (Web Accessible Folder) methods.
     */
    public static void testWAF() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.testWAF()\n");
        //debugMode=true;        

        try {
        //test with trailing /
        String url = "http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/"; 
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
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,,,\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1940.nc,1262881740000,48128\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1941.nc,1262881740000,16384\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1942.nc,1262881740000,49152\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1943.nc,1262881740000,57344\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1944.nc,1262881740000,92160\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1945.nc,1262881740000,117760\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1946.nc,1262881740000,130048\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1947.nc,1262881740000,162816\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1948.nc,1262881740000,202752\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1949.nc,1262881740000,103424\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,,,\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1940.nc,1262881740000,284672\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1941.nc,1262881740000,246784\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1942.nc,1262881740000,239616\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1943.nc,1262881740000,302080\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1944.nc,1262881740000,335872\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1945.nc,1262881740000,401408\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1946.nc,1262881740000,592896\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1947.nc,1262881740000,506880\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1948.nc,1262881740000,452608\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1949.nc,1262881740000,694272\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(1262881740),
            "2010-01-07T16:29:00", "");

        //test via oneStep 
        tTable = oneStep(url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo);
        results = tTable.dataToCSVString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* test !dirsToo
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, tFileNameRegex, tRecursive, tPathRegex, false, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1940.nc,1262881740000,48128\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1941.nc,1262881740000,16384\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1942.nc,1262881740000,49152\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1943.nc,1262881740000,57344\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1944.nc,1262881740000,92160\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1945.nc,1262881740000,117760\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1946.nc,1262881740000,130048\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1947.nc,1262881740000,162816\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1948.nc,1262881740000,202752\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1949.nc,1262881740000,103424\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1940.nc,1262881740000,284672\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1941.nc,1262881740000,246784\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1942.nc,1262881740000,239616\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1943.nc,1262881740000,302080\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1944.nc,1262881740000,335872\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1945.nc,1262881740000,401408\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1946.nc,1262881740000,592896\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1947.nc,1262881740000,506880\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1948.nc,1262881740000,452608\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,1949.nc,1262881740000,694272\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url, tFileNameRegex, tRecursive, tPathRegex, false);
        results = tTable.dataToCSVString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* test subdir
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url + "3", //test no trailing /
                tFileNameRegex, tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1940.nc,1262881740000,48128\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1941.nc,1262881740000,16384\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1942.nc,1262881740000,49152\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1943.nc,1262881740000,57344\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1944.nc,1262881740000,92160\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1945.nc,1262881740000,117760\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1946.nc,1262881740000,130048\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1947.nc,1262881740000,162816\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1948.nc,1262881740000,202752\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,1949.nc,1262881740000,103424\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url + "3", tFileNameRegex, tRecursive, tPathRegex, tDirsToo);
        results = tTable.dataToCSVString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* test file regex that won't match
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, //test no trailing /
                "zztop", tRecursive, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/3/,,,\n" +
"http://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/4/,,,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url, "zztop", tRecursive, tPathRegex, tDirsToo);
        results = tTable.dataToCSVString();
        Test.ensureEqual(results, expected, "results=\n" + results);


        //* that should be the same as !recursive
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList(url, //test no trailing /
                tFileNameRegex, false, tPathRegex, tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToCSVString();
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep 
        tTable = oneStep(url, tFileNameRegex, false, tPathRegex, tDirsToo);
        results = tTable.dataToCSVString();
        Test.ensureEqual(results, expected, "results=\n" + results);

        //* Test InPort WAF        
        table.removeAllRows();
        Test.ensureTrue( //completelySuccessful
            addToWAFUrlList("https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/",
                "22...\\.xml",   
                //pre 2016-03-04 I tested NWFSC/inport/xml, but it has been empty for a month!
                true, ".*/NMFS/(|NEFSC/)(|inport/)(|xml/)", //tricky!
                true, //tDirsToo, 
                dirs, names, lastModifieds, sizes),
            "");
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/,,,\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/,,,\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,,,\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,22560.xml,1455948120000,21504\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,22561.xml,1455948120000,21504\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,22562.xml,1455948120000,19456\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,22563.xml,1455948120000,21504\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,22564.xml,1456553280000,23552\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/,22565.xml,1455948120000,25600\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\nThis changes periodically. If reasonable, just continue." +
              "\n(there no more subtests in this test).");
      }
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
        http://podaac-opendap.jpl.nasa.gov/opendap/hyrax/allData/avhrr/L4/reynolds_er/v3b/monthly/netcdf/2014/
     * @param fileNameRegex e.g.,
            "pentad.*flk\\.nc\\.gz"
     * @param recursive
     * @param pathRegex a regex to constrain which subdirs to include.
     *   This is ignored if recursive is false.
     *   null or "" is treated as .* (i.e., match everything).
     * @returns a String[] with a list of full URLs of the children (may be new String[0]) 
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
            response = SSR.getUrlResponseString(url + "contents.html");
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
        if (po < 0 ) {
            if (reallyVerbose) String2.log("ERROR: \"parent directory\" not found in Hyrax response.");
            return false;
        }
        po += 18;

        //endPre
        int endPre = responseLC.indexOf("</pre>", po); //Lower Case
        if (endPre < 0) 
            endPre = response.length();

        //go through file,dir listings
        boolean diagnosticMode = false;
        while (true) {

            //EXAMPLE http://data.nodc.noaa.gov/opendap/wod/monthly/  No longer available

            //EXAMPLE http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/M07
            //(reformatted: look for tags, not formatting
            /*   <tr>
                   <td align="left"><b><a href="month_19870701_v11l35flk.nc.gz.html">month_19870701_v11l35flk.nc.gz</a></b></td>
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

        try {
        
        String url = "http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/"; //contents.html
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
        String results = table.dataToCSVString();
        String expected = 
"URL,lastModified,size\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz,1.336863115E9,4981045\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz,1.336723222E9,5024372\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz,1.336546575E9,5006043\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz,1.336860015E9,4948285\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz,1.336835143E9,4914250\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz,1.336484405E9,4841084\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz,1.336815079E9,4837417\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz,1.336799789E9,4834242\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz,1.336676042E9,4801865\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz,1.336566352E9,4770289\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz,1.336568382E9,4769160\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz,1.336838712E9,4866335\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890101_v11l35flk.nc.gz,1.336886548E9,5003981\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890201_v11l35flk.nc.gz,1.336268373E9,5054907\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890301_v11l35flk.nc.gz,1.336605483E9,4979393\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890401_v11l35flk.nc.gz,1.336350339E9,4960865\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890501_v11l35flk.nc.gz,1.336551575E9,4868541\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890601_v11l35flk.nc.gz,1.336177278E9,4790364\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890701_v11l35flk.nc.gz,1.336685187E9,4854943\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890801_v11l35flk.nc.gz,1.336534686E9,4859216\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890901_v11l35flk.nc.gz,1.33622953E9,4838390\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891001_v11l35flk.nc.gz,1.336853599E9,4820645\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891101_v11l35flk.nc.gz,1.336882933E9,4748166\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891201_v11l35flk.nc.gz,1.336748115E9,4922858\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1990/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1991/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1992/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1993/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1994/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1995/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1996/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1997/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1998/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1999/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2000/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2001/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2002/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2003/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2004/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2005/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2006/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2007/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2008/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2009/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2010/,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2011/,,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureTrue(allOk, "");

        //test getUrlsFromHyraxCatalog
        String resultsAr[] = getUrlsFromHyraxCatalog(url, fileNameRegex, recursive,
            pathRegex);
        String expectedAr[] = new String[]{
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890101_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890201_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890301_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890401_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890501_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890601_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890701_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890801_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890901_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891001_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891101_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891201_v11l35flk.nc.gz"}; 
        Test.ensureEqual(resultsAr, expectedAr, "results=\n" + results);

        //different test of addToHyraxUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();
        LongArray fSize = new LongArray();
        url = "http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/"; //startUrl, 
        fileNameRegex = "month_[0-9]{8}_v11l35flk\\.nc\\.gz"; //fileNameRegex, 
        recursive = true;
        addToHyraxUrlList(url, fileNameRegex, recursive, pathRegex, dirsToo,
            childUrls, lastModified, fSize);

        results = childUrls.toNewlineString();
        expected = 
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870701_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870801_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870901_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871001_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871101_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871201_v11l35flk.nc.gz\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = lastModified.toString();
        expected = "NaN, 1.336609915E9, 1.336785444E9, 1.336673639E9, 1.336196561E9, 1.336881763E9, 1.336705731E9";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep -- dirs
        table = oneStep(url, fileNameRegex, recursive, pathRegex, true);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,,,\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870701_v11l35flk.nc.gz,1336609915,4807310\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870801_v11l35flk.nc.gz,1336785444,4835774\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870901_v11l35flk.nc.gz,1336673639,4809582\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871001_v11l35flk.nc.gz,1336196561,4803285\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871101_v11l35flk.nc.gz,1336881763,4787239\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871201_v11l35flk.nc.gz,1336705731,4432696\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep -- no dirs
        table = oneStep(url, fileNameRegex, recursive, pathRegex, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870701_v11l35flk.nc.gz,1336609915,4807310\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870801_v11l35flk.nc.gz,1336785444,4835774\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870901_v11l35flk.nc.gz,1336673639,4809582\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871001_v11l35flk.nc.gz,1336196561,4803285\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871101_v11l35flk.nc.gz,1336881763,4787239\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871201_v11l35flk.nc.gz,1336705731,4432696\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\n2016-02-29 results=\"\" because HTTP 503, Server Not Available." +
              "\nUnexpected error."); 
      }
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
     *    <br>http://data.nodc.noaa.gov/thredds/catalog/pathfinder/Version5.1_CloudScreened/5day/FullRes/
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
            response = SSR.getUrlResponseString(url + "catalog.html");
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
        while (true) {

/* EXAMPLE from TDS 4.2.10 at
http://data.nodc.noaa.gov/thredds/catalog/pathfinder/Version5.1_CloudScreened/5day/FullRes/1981/catalog.html
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
<td align='right'><tt>2009-11-23 17:58:53Z</tt></td>
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
            String content1 = String2.extractRegex(td1, "<tt>.*</tt>", 0);
            String content2 = String2.extractRegex(
                thisRow.substring(td2Po, td3Po), "<tt>.*</tt>", 0);
            String content3 = String2.extractRegex(
                thisRow.substring(td3Po, thisRow.length()), "<tt>.*</tt>", 0);
            content1 = content1 == null? "" : content1.substring(4, content1.length() - 5);
            content2 = content2 == null? "" : content2.substring(4, content2.length() - 5);
            content3 = content3 == null? "" : content3.substring(4, content3.length() - 5);
            if (diagnosticMode) 
                String2.log("=== <td><tt> content #1=" + content1 + " #2=" + content2 + 
                                                " #3=" + content3);
            if (content1.length() == 0) { 
                if (reallyVerbose) 
                    String2.log("WARNING: No <tt>content</tt> in first <td>...</td>.");
                completelySuccessful = false;
                po = nextRow;
                continue;
            }

            //ensure fileName matches fileNameRegex
            if (!content1.matches(fileNameRegex)) {
                po = nextRow;
                if (diagnosticMode) 
                    String2.log("=== skip this row: content1=" + content1 + " doesn't match fileNameRegex=" + fileNameRegex);
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
                "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", 0);
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
        String2.log("\n*** testThredds");
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = true;

      try {
        String url =  "http://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/monthly/"; //catalog.html
        String fileNameRegex = "sss_binned_L3_MON_SCI_V3.0_\\d{4}\\.nc";
        boolean recursive = true;
        String pathRegex = null;
        boolean dirsToo = true;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray fSize = new LongArray();

        //test error via addToThreddsUrlList  
        //(yes, logged message includes directory name)
        String2.log("\nIntentional error:");
        Test.ensureEqual(
            addToThreddsUrlList(url + "testInvalidUrl", fileNameRegex,  
                recursive, pathRegex, dirsToo, childUrls, lastModified, fSize),
            false, "");

        //test addToThreddsUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();
        fSize = new LongArray();
        addToThreddsUrlList(url, fileNameRegex,  recursive, pathRegex, 
            dirsToo, childUrls, lastModified, fSize);

        String results = childUrls.toNewlineString();
        String expected = 
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2011.nc\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2012.nc\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2013.nc\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2014.nc\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2015.nc\n";
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
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,,,\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2011.nc,1405495932,2723152\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2012.nc,1405492834,6528434\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2013.nc,1405483892,6528434\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2014.nc,1429802008,6528434\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2015.nc,1429867829,1635779\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test via oneStep -- no dirs
        table = oneStep(url, fileNameRegex, recursive, pathRegex, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2011.nc,1405495932,2723152\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2012.nc,1405492834,6528434\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2013.nc,1405483892,6528434\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2014.nc,1429802008,6528434\n" +
"http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/,sss_binned_L3_MON_SCI_V3.0_2015.nc,1429867829,1635779\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

      } catch (Throwable t) {
          String2.pressEnterToContinue(MustBe.throwableToString(t) + 
              "\nUnexpected error."); 
      }
      reallyVerbose = oReallyVerbose;
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
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"c:\\erddapTest\\fileNames\\,jplMURSST20150103090000.png,1421276044628,46482\n" +
"c:\\erddapTest\\fileNames\\,jplMURSST20150104090000.png,1420669338436,46586\n" +
"c:\\erddapTest\\fileNames\\sub\\,,1420735700318,0\n" +
"c:\\erddapTest\\fileNames\\sub\\,jplMURSST20150105090000.png,1420669304917,46549\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //recursive and !dirToo           and test // separator
        table = oneStep(String2.unitTestDataDir + "fileNames", ".*\\.png", true, tPathRegex, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n" +
String2.unitTestDataDir + "fileNames/sub/,jplMURSST20150105090000.png,1420669304917,46549\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //!recursive and dirToo
        table = oneStep(String2.unitTestDataDir + "fileNames", ".*\\.png", false, tPathRegex, true);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n" +
String2.unitTestDataDir + "fileNames/sub/,,1420735700318,0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //!recursive and !dirToo
        table = oneStep(String2.unitTestDataDir + "fileNames", ".*\\.png", false, tPathRegex, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //***
        //oneStepDouble
        table = oneStepDouble(String2.unitTestDataDir + "fileNames", ".*\\.png", true, tPathRegex, true); 
        results = table.toCSVString();
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
"row,directory,name,lastModified,size\n" +
"0," + String2.unitTestDataDir + "fileNames/,jplMURSST20150103090000.png,1.421276044628E9,46482.0\n" +
"1," + String2.unitTestDataDir + "fileNames/,jplMURSST20150104090000.png,1.420669338436E9,46586.0\n" +
"2," + String2.unitTestDataDir + "fileNames/sub/,,1.420735700318E9,0.0\n" +
"3," + String2.unitTestDataDir + "fileNames/sub/,jplMURSST20150105090000.png,1.420669304917E9,46549.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //***
        //oneStepAccessibleViaFiles
        table = oneStepDoubleWithUrlsNotDirs(String2.unitTestDataDir + "fileNames", ".*\\.png", 
            true, tPathRegex,
            "http://localhost:8080/cwexperimental/files/testFileNames/");
        results = table.toCSVString();
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
"row,url,name,lastModified,size\n" +
"0,http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,1.421276044628E9,46482.0\n" +
"1,http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150104090000.png,jplMURSST20150104090000.png,1.420669338436E9,46586.0\n" +
"2,http://localhost:8080/cwexperimental/files/testFileNames/sub/jplMURSST20150105090000.png,jplMURSST20150105090000.png,1.420669304917E9,46549.0\n";
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
                    String2.log("forward test: n=" + directoryPA.size() + " time=" + time);
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
                    String2.log("backward test: n=" + directoryPA.size() + " time=" + time);
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
     * See http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html .
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
        String parent = "http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/";
        String child = "CONUS/";
        String pathRegex = null;

        //recursive and dirToo
        table = oneStep(parent, ".*\\.nc", true, pathRegex, true); 
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/,,,\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //recursive and !dirToo
        table = oneStep(parent, ".*\\.nc", true, pathRegex, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //!recursive and dirToo
        table = oneStep(parent + child, ".*\\.nc", false, pathRegex, true);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //!recursive and !dirToo
        table = oneStep(parent + child, ".*\\.nc", false, pathRegex, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
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
     * @param doIt if true, the files are actually copied
     * @return a Table with 2 String columns (remote, local)
     *    with the full names of the files which were (or need to be)
     *    copied, and the lastModified time of the remote file.
     * @throws Exception if trouble getting remote or local file info.
     *    If doIt, exceptions while downloading files are caught, logged,
     *    and not rethrown.
     */
    public static Table sync(String remoteDir, String localDir, 
        String tFileNameRegex, boolean tRecursive, String tPathRegex,
        boolean doIt) throws Exception {

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
        //String2.log("\nremote table (max of 5)\n" + rTable.dataToCSVString(5) +
        //            "\nlocal  table (max of 5)\n" + lTable.dataToCSVString(5));

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
        if (doIt) {
            for (int i = 0; i < n; i++) {
                try {
                    if (i % 100 == 0)
                        String2.log("sync is downloading #" + i + " of " + n);
                    SSR.downloadFile(remoteSA.get(i), localSA.get(i), false); //try to compress
                    File2.setLastModified(localSA.get(i), lastModLA.get(i));
                } catch (Throwable t) {
                    downloadFailures.add(remoteSA.get(i));
                    String2.log(MustBe.throwableToString(t));
                }
            }       
        }

        String2.log("sync(" + remoteDir + ", " + localDir + ", " + tFileNameRegex + 
            ")\n  found nAlready=" + nAlready + " nToDownload=" + n +
            " nTooRecent=" + nTooRecent + " nExtraLocal=" + nExtraLocal);
        if (doIt)
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
        //pre 2016-03-04 I tested NWFSC/inport/xml, but it has been empty for a month!
        String pathRegex = ".*/NMFS/(|NEFSC/)(|inport/)(|xml/)"; //tricky!            
        boolean doIt = true;

        //get original times
        String name22560 = lDir + "/NEFSC/inport/xml/22560.xml";
        String name22565 = lDir + "/NEFSC/inport/xml/22565.xml";

        long time22560 = File2.getLastModified(name22560);
        long time22565 = File2.getLastModified(name22565);

        try {
            //For testing purposes, I put one extra file in the dir: 8083.xml renamed as 22ext.xml

            //do the sync
            sync(rDir, lDir, fileRegex, recursive, pathRegex, true);

            //current date on all these files
            //Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(1455948120),
            //    "2016-02-20T06:02:00", ""); //what the web site shows for many

            //delete 1 local file
            File2.delete(lDir + "/NEFSC/inport/xml/22563.xml");

            //make 1 local file older
            File2.setLastModified(name22560, 100);

            //make 1 local file newer
            File2.setLastModified(name22565, System.currentTimeMillis() + 100);
            Math2.sleep(500);

            //test the sync 
            Table table = sync(rDir, lDir, fileRegex, recursive, pathRegex, doIt);
            String2.pressEnterToContinue("\nCheck above to ensure these numbers:\n" +
                "\"found nAlready=3 nToDownload=2 nTooRecent=1 nExtraLocal=1\"\n");
            String results = table.dataToCSVString();
            String expected = //the lastModified values change periodically
//these are the files which were downloaded
"remote,local,lastModified\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/22560.xml," + String2.unitTestDataDir + "sync/NMFS/NEFSC/inport/xml/22560.xml,1475767320000\n" +
"https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport/xml/22563.xml," + String2.unitTestDataDir + "sync/NMFS/NEFSC/inport/xml/22563.xml,1475767380000\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //no changes, do the sync again
            table = sync(rDir, lDir, fileRegex, recursive, pathRegex, doIt);
            String2.pressEnterToContinue("\nCheck above to ensure these numbers:\n" +
                "\"found nAlready=5 nToDownload=0 nTooRecent=1 nExtraLocal=1\"\n");
            results = table.dataToCSVString();
            expected = 
    "remote,local,lastModified\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

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
        int tallyWhich, int interactiveNLines) throws Throwable {
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
                String lines[] = SSR.getUrlResponse(fullName);
                for (int linei = 0; linei < lines.length; linei++) {
                    Matcher matcher = linePattern.matcher(lines[linei]);
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
                            String2.log(String2.toNewlineString(lines));
                            String2.log("\n" + msg + ":\n");
                            for (int tl = linei; tl < Math.min(linei + interactiveNLines + 1, lines.length); tl++)
                                String2.log(lines[tl]);
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
            String2.log(tally.toString(100)); //most common n will be shown
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
            FileInputStream fis = new FileInputStream(fullName);
            while ((nBytes = fis.read(buffer)) > 0) 
                tar.write(buffer, 0, nBytes);
            fis.close();                    
            tar.closeArchiveEntry();
        }                 
        tar.close();
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
                        Calendar2.epochSecondsToIsoStringT(cTime / 1000) + "Z", 
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

            SimpleXMLReader xmlReader = null;
            try {
                String2.log("reading #" + i + ": " + dirs.get(i) + names.get(i));
                xmlReader = new SimpleXMLReader(
                    new FileInputStream(dirs.get(i) + names.get(i)));
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
                    new FileInputStream(dirs.get(i) + names.get(i)));
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
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean doBigTest) throws Throwable {
        String2.log("\n****************** FileVisitorDNLS.test() *****************\n");
/* */
        //always done        
        testLocal(doBigTest);
        testAWSS3(); 
        testHyrax();
        testThredds();
        testWAF();
        testSync();
        testMakeTgz();
        testOneStepToString();
        testPathRegex();

        //testSymbolicLinks(); //THIS TEST DOESN'T WORK on Windows, but links are followed on Linux

        //future: FTP? ftp://ftp.unidata.ucar.edu/pub/
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
            String2.log(table.dataToCSVString());
        }
        System.exit(0);
    }

}
