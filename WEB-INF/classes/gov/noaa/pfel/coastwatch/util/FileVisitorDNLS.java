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
import gov.noaa.pfel.coastwatch.pointdata.Table;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class gathers basic information about a group of files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2014-11-25
 */
public class FileVisitorDNLS extends SimpleFileVisitor<Path> {

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
    public String regex;
    public Pattern pattern; //from regex
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
     * @param tDirectoriesToo
     */
    public FileVisitorDNLS(String tDir, String tRegex, boolean tRecursive,
        boolean tDirectoriesToo) {
        super();

        dir = File2.addSlash(tDir);
        toSlash = dir.indexOf('\\') >= 0? '\\' : '/';
        fromSlash = toSlash == '/'? '\\' : '/';        
        recursive = tRecursive;
        regex = tRegex;
        pattern = Pattern.compile(regex);
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
        if (ttDir.equals(dir)) //initial dir
            return FileVisitResult.CONTINUE;

        if (directoriesToo) {
            directoryPA.add(ttDir);
            namePA.add("");
            lastModifiedPA.add(attrs.lastModifiedTime().toMillis());
            sizePA.add(0);
        }

        return recursive? FileVisitResult.CONTINUE : 
            FileVisitResult.SKIP_SUBTREE;    
    }

    /** Invoked for a file in a directory. */
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
        throws IOException {

        int oSize = directoryPA.size();
        try {
            String name = file.getFileName().toString();
            Matcher matcher = pattern.matcher(name);
            if (!matcher.matches()) 
                return FileVisitResult.CONTINUE;    

            //getParent returns \\ or /, without trailing /
            String ttDir = String2.replaceAll(file.getParent().toString(), fromSlash, toSlash) +
                toSlash;
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
     * @return a table with columns with DIRECTORY, NAME, LASTMODIFIED, and SIZE columns.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's name will be "".
     * @throws IOException if trouble
     */
    public static Table oneStep(String tDir, String tRegex, boolean tRecursive,
        boolean tDirectoriesToo) throws IOException {
        long time = System.currentTimeMillis();

        //Is it an S3 bucket with "files"?
        Pattern pattern = Pattern.compile(String2.AWS_S3_REGEX);
        Matcher matcher = pattern.matcher(File2.addSlash(tDir)); //forcing trailing slash avoids problems
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

            String bucketName = matcher.group(2); 
            String prefix = matcher.group(3); 
            String baseURL = tDir.substring(0, matcher.start(3));
            AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());
            try {
                if (verbose) 
                    String2.log("FileVisitorDNLS.oneStep getting info from AWS S3 at" + 
                        "\nURL=" + tDir);
                        //"\nbucket=" + bucketName + " prefix=" + prefix);

                //I wanted to generate lastMod for dir based on lastMod of files
                //but it would be inconsistent for different requests (recursive, regex).
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
                                      "\n tRegex=" + tRegex + " matches=" + keyName.matches(tRegex));
                            if (keyName.length() > 0 && keyName.matches(tRegex)) {
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
                    table.leftToRightSort(2);
                }

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
        if (tDir.matches("http://.+/opendap/.+")) {
            try {
                Table table = makeEmptyTable();
                StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                StringArray namePA         = (StringArray)table.getColumn(NAME);
                LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                DoubleArray lastModDA = new DoubleArray();
                addToHyraxUrlList(tDir, tRegex, tRecursive, tDirectoriesToo,
                    namePA, lastModDA, sizePA);
                lastModifiedPA.append(lastModDA);
                int n = namePA.size();
                for (int i = 0; i < n; i++) {
                    String fn = namePA.get(i);
                    directoryPA.add(File2.getDirectory(fn));
                    namePA.set(i, File2.getNameAndExtension(fn));
                }

                return table;
            } catch (Throwable t) {
                throw new IOException(t.getMessage(), t);
            }
        }

        //THREDDS
        if (tDir.matches("http://.+/thredds/catalog/.+")) {
            try {
                Table table = makeEmptyTable();
                StringArray directoryPA    = (StringArray)table.getColumn(DIRECTORY);
                StringArray namePA         = (StringArray)table.getColumn(NAME);
                LongArray   lastModifiedPA = (  LongArray)table.getColumn(LASTMODIFIED);
                LongArray   sizePA         = (  LongArray)table.getColumn(SIZE);

                DoubleArray lastModDA = new DoubleArray();
                addToThreddsUrlList(tDir, tRegex, tRecursive, tDirectoriesToo,
                    namePA, lastModDA, sizePA);
                lastModifiedPA.append(lastModDA);
                int n = namePA.size();
                for (int i = 0; i < n; i++) {
                    String fn = namePA.get(i);
                    directoryPA.add(File2.getDirectory(fn));
                    namePA.set(i, File2.getNameAndExtension(fn));
                }

                return table;
            } catch (Throwable t) {
                throw new IOException(t.getMessage(), t);
            }
        }

        //other remote files
//NOT YET DONE
//        if (tDir.startsWith("http://") || tDir.startsWith("http://")) {
                        
            //apache-style directory index? parse the html
            //Use an ERDDAP files dir as an example.
            
            //throw error
//        }

        //local files
        FileVisitorDNLS fv = new FileVisitorDNLS(tDir, tRegex, tRecursive, 
            tDirectoriesToo);
        Files.walkFileTree(FileSystems.getDefault().getPath(tDir), fv);
        if (verbose) String2.log("FileVisitorDNLS.oneStep finished successfully. n=" + 
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
     * @return a table with columns with DIRECTORY, NAME, 
     *    LASTMODIFIED (double epochSeconds), and SIZE (doubles) columns.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's name will be "".
     * @throws IOException if trouble
     */
    public static Table oneStepDouble(String tDir, String tRegex, boolean tRecursive,
        boolean tDirectoriesToo) throws IOException {

        return oneStepDouble(oneStep(tDir, tRegex, tRecursive, tDirectoriesToo));
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
    public static Table oneStepDoubleWithUrlsNotDirs(String tDir, String tRegex, boolean tRecursive,
        String startOfUrl) throws IOException {

        tDir = File2.addSlash(String2.replaceAll(tDir, "\\", "/")); //ensure forward/ and trailing/
        return oneStepDoubleWithUrlsNotDirs(
            oneStepDouble(tDir, tRegex, tRecursive, false), //tDirectoriesToo
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
     * @returns a String[] with a list of full URLs of the children (may be new String[0]) 
     * @throws Throwable if unexpected trouble. But url not responding won't throw Throwable.
     */
    public static String[] getUrlsFromHyraxCatalog(String startUrl, String fileNameRegex, 
        boolean recursive) throws Throwable {
        if (verbose) String2.log("getUrlsFromHyraxCatalog regex=" + fileNameRegex);

        //call the recursive method
        boolean tDirectoriesToo = false;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray size = new LongArray();
        addToHyraxUrlList(startUrl, fileNameRegex, recursive, tDirectoriesToo,
            childUrls, lastModified, size);

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
     * @param dirsToo  if true, directories should be collected also
     * @param childUrls  new children will be added to this
     * @param lastModified the lastModified time (secondsSinceEpoch, NaN if not available).
     *   Source times are assumed to be Zulu time zone (which is probably incorrect).
     * @param size the file's size (bytes, Long.MAX_VALUE if not available)
     * @return true if completely successful (no access errors, all URLs found)
     * @throws Throwable if unexpected trouble. But url not responding won't throw Throwable.
     */
    public static boolean addToHyraxUrlList(String url, String fileNameRegex, 
        boolean recursive, boolean dirsToo,
        StringArray childUrls, DoubleArray lastModified, LongArray size) throws Throwable {

        if (reallyVerbose) String2.log("\naddToHyraxUrlList childUrls.size=" + childUrls.size() + 
            "\n  url=" + url); 
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
                    boolean tSuccessful = addToHyraxUrlList(
                        url + thisRow.substring(quotePo + 1, conPo + 1),
                        fileNameRegex, recursive, dirsToo, childUrls, lastModified, size);
                    if (!tSuccessful)
                        completelySuccessful = false;
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
        String regex = "month_198(8|9).*flk\\.nc\\.gz";
        boolean recursive = true;
        boolean dirsToo = true;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray size = new LongArray();

        //test error via addToHyraxUrlList  
        //(yes, logged message includes directory name)
        Test.ensureEqual(
            addToHyraxUrlList(url + "testInvalidUrl", regex, recursive, dirsToo,
                childUrls, lastModified, size),
            false, "");

        //test addToHyraxUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();
        size = new LongArray();
        boolean allOk = addToHyraxUrlList(url, regex, recursive, dirsToo,
            childUrls, lastModified, size);
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
        String resultsAr[] = getUrlsFromHyraxCatalog(url, regex, recursive);
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
        regex = "month_[0-9]{8}_v11l35flk\\.nc\\.gz"; //fileNameRegex, 
        recursive = true;
        addToHyraxUrlList(url, regex, recursive, dirsToo,
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
        table = oneStep(url, regex, recursive, true);
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
        table = oneStep(url, regex, recursive, false);
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
     * @param childUrls  new children will be added to this
     * @param lastModified the lastModified time (secondsSinceEpoch, NaN if not available).
     *   Source times are assumed to be Zulu time zone (which is probably incorrect).
     * @param size the file's size (bytes, Long.MAX_VALUE if not available)
     * @return true if completely successful (no access errors, all URLs found)
     * @throws Throwable if unexpected trouble. But url not responding won't throw Throwable.
     */
    public static boolean addToThreddsUrlList(String url, String fileNameRegex, 
        boolean recursive, boolean dirsToo,
        StringArray childUrls, DoubleArray lastModified, LongArray size) throws Throwable {

        if (reallyVerbose) String2.log("\naddToThreddsUrlList childUrls.size=" + childUrls.size() + 
            "\n  url=" + url); 
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
                //Row with current dir doesn't match regex; row for subdir does.
                if (diagnosticMode) 
                    String2.log("=== folder row");
                String content1 = String2.extractRegex(td1, "href='[^']*/catalog.html'>", 0);
                if (recursive && content1 != null && content1.length() > 21) { //21 is non-.* stuff
                    content1 = content1.substring(6, content1.length() - 2);
                    completelySuccessful = addToThreddsUrlList(
                        url + content1, fileNameRegex, recursive, dirsToo,
                        childUrls, lastModified, size);
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

            //ensure fileName matches regex
            if (!content1.matches(fileNameRegex)) {
                po = nextRow;
                if (diagnosticMode) 
                    String2.log("=== skip this row: content1=" + content1 + " doesn't match regex=" + fileNameRegex);
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

      try{
        String url =  "http://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/monthly/"; //catalog.html
        String regex = "sss_binned_L3_MON_SCI_V3.0_\\d{4}\\.nc";
        boolean recursive = true;
        boolean dirsToo = true;
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray fSize = new LongArray();

        //test error via addToThreddsUrlList  
        //(yes, logged message includes directory name)
        Test.ensureEqual(
            addToThreddsUrlList(url + "testInvalidUrl", regex,  recursive, dirsToo,
                childUrls, lastModified, fSize),
            false, "");

        //test addToThreddsUrlList
        childUrls = new StringArray();
        lastModified = new DoubleArray();
        fSize = new LongArray();
        addToThreddsUrlList(url, regex,  recursive, dirsToo,
            childUrls, lastModified, fSize);

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
        Table table = oneStep(url, regex, recursive, true);
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
        table = oneStep(url, regex, recursive, false);
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
        Table table;
        long time;
        int n;
        String results, expected;

        //recursive and dirToo
        table = oneStep("c:/erddapTest/fileNames", ".*\\.png", true, true); 
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"c:/erddapTest/fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
"c:/erddapTest/fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n" +
"c:/erddapTest/fileNames/sub/,,1420735700318,0\n" +
"c:/erddapTest/fileNames/sub/,jplMURSST20150105090000.png,1420669304917,46549\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //recursive and !dirToo
        table = oneStep("c:/erddapTest/fileNames", ".*\\.png", true, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"c:/erddapTest/fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
"c:/erddapTest/fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n" +
"c:/erddapTest/fileNames/sub/,jplMURSST20150105090000.png,1420669304917,46549\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //!recursive and dirToo
        table = oneStep("c:/erddapTest/fileNames", ".*\\.png", false, true);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"c:/erddapTest/fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
"c:/erddapTest/fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n" +
"c:/erddapTest/fileNames/sub/,,1420735700318,0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //!recursive and !dirToo
        table = oneStep("c:/erddapTest/fileNames", ".*\\.png", false, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"c:/erddapTest/fileNames/,jplMURSST20150103090000.png,1421276044628,46482\n" +
"c:/erddapTest/fileNames/,jplMURSST20150104090000.png,1420669338436,46586\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //***
        //oneStepDouble
        table = oneStepDouble("c:/erddapTest/fileNames", ".*\\.png", true, true); 
        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 4 ;\n" +
"\tdirectory_strlen = 28 ;\n" +
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
"0,c:/erddapTest/fileNames/,jplMURSST20150103090000.png,1.421276044628E9,46482.0\n" +
"1,c:/erddapTest/fileNames/,jplMURSST20150104090000.png,1.420669338436E9,46586.0\n" +
"2,c:/erddapTest/fileNames/sub/,,1.420735700318E9,0.0\n" +
"3,c:/erddapTest/fileNames/sub/,jplMURSST20150105090000.png,1.420669304917E9,46549.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //***
        //oneStepAccessibleViaFiles
        table = oneStepDoubleWithUrlsNotDirs("c:/erddapTest/fileNames", ".*\\.png", true, 
            "http://127.0.0.1:8080/cwexperimental/files/testFileNames/");
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
"0,http://127.0.0.1:8080/cwexperimental/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,1.421276044628E9,46482.0\n" +
"1,http://127.0.0.1:8080/cwexperimental/files/testFileNames/jplMURSST20150104090000.png,jplMURSST20150104090000.png,1.420669338436E9,46586.0\n" +
"2,http://127.0.0.1:8080/cwexperimental/files/testFileNames/sub/jplMURSST20150105090000.png,jplMURSST20150105090000.png,1.420669304917E9,46549.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //*** huge dir
        String unexpected = 
            "\nUnexpected FileVisitorDNLS error (but /data/gtspp/temp dir has variable nFiles):\n";

        if (doBigTest) {
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    //forward slash in huge directory
                    time = System.currentTimeMillis();
                    table = oneStep("/data/gtspp/temp", ".*\\.nc", false, false); 
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
                    table = oneStep("\\data\\gtspp\\temp", ".*\\.nc", false, false); 
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

        //recursive and dirToo
        table = oneStep(parent, ".*\\.nc", true, true); 
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/,,,\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_200601-201012.nc,1382457840000,1368327466\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201101-201512.nc,1382457577000,1369233982\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201601-202012.nc,1382260226000,1368563375\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //recursive and !dirToo
        table = oneStep(parent, ".*\\.nc", true, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_200601-201012.nc,1382457840000,1368327466\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201101-201512.nc,1382457577000,1369233982\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201601-202012.nc,1382260226000,1368563375\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //!recursive and dirToo
        table = oneStep(parent + child, ".*\\.nc", false, true);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_200601-201012.nc,1382457840000,1368327466\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201101-201512.nc,1382457577000,1369233982\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201601-202012.nc,1382260226000,1368563375\n";
        if (expected.length() > results.length()) 
            String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //!recursive and !dirToo
        table = oneStep(parent + child, ".*\\.nc", false, false);
        results = table.dataToCSVString();
        expected = 
"directory,name,lastModified,size\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_200601-201012.nc,1382457840000,1368327466\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201101-201512.nc,1382457577000,1369233982\n" +
"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_BNU-ESM_201601-202012.nc,1382260226000,1368563375\n";
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
    }

}
