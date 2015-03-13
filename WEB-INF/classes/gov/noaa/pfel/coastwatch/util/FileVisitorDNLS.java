/* 
 * FileVisitorDNLS Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.LongArray;
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
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
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

    /** The names of the columns in the table. */
    public final static String DIRECTORY    = "directory";
    public final static String NAME         = "name";
    public final static String LASTMODIFIED = "lastModified";
    public final static String SIZE         = "size";

    /** things set by constructor */
    public String dir;  //with \\ or / separators. With trailing slash (to match).
    private char fromSlash, toSlash;
    public String regex;
    public Pattern pattern; //from regex
    public boolean recursive, directoriesToo;
    static boolean OSIsWindows = String2.OSIsWindows;
    public Table table = new Table();
    /** dirs will have \\ or / like original constructor tDir, and a matching trailing slash. */
    public StringArray directoryPA    = new StringArray();
    public StringArray namePA         = new StringArray();
    public LongArray   lastModifiedPA = new LongArray();
    public LongArray   sizePA         = new LongArray();


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
        table.addColumn(DIRECTORY,    directoryPA);
        table.addColumn(NAME,         namePA);
        table.addColumn(LASTMODIFIED, lastModifiedPA);
        table.addColumn(SIZE,         sizePA);
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

    /** A convenience method for using this class. 
     * The results are in the global table and in the PA variables.
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     *    The resulting directoryPA will contain dirs with matching slashes and trailing slash.
     * @return a table with columns with DIRECTORY, NAME, LASTMODIFIED, and SIZE columns.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's name will be "".
     */
    public static Table oneStep(String tDir, String tRegex, boolean tRecursive,
        boolean tDirectoriesToo) throws IOException {
        long time = System.currentTimeMillis();
        FileVisitorDNLS fv = new FileVisitorDNLS(tDir, tRegex, tRecursive, 
            tDirectoriesToo);
        Files.walkFileTree(FileSystems.getDefault().getPath(tDir), fv);
        if (verbose) String2.log("FileVisitorDNLS.oneStep finished successfully. n=" + 
            fv.directoryPA.size() + " time=" +
            (System.currentTimeMillis() - time));
        return fv.table;
    }

    /** table.dataToCSVString(); */
    public String resultsToString() {
        return table.dataToCSVString();
    }

    /** 
     * This tests this class. 
     */
    public static void test() throws Throwable {
        String2.log("\n*** FileVisitorDNLS.test");
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

        //huge dir
        String unexpected = 
            "\nUnexpected FileVisitorDNLS error (but /data/gtspp/temp dir has variable nFiles):\n";

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
                String2.getStringFromSystemIn(unexpected +
                    MustBe.throwableToString(t) + "\n" +
                    "Press ^C to stop or Enter to continue..."); 
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
                String2.getStringFromSystemIn(unexpected +
                    MustBe.throwableToString(t) + "\n" +
                    "Press ^C to stop or Enter to continue..."); 
            }
        }
        String2.log("\n*** FileVisitorDNLS.test finished.");
    }


}
