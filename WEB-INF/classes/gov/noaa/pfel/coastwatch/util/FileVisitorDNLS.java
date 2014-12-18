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
    public final static String DIR     = "dir";
    public final static String NAME    = "name";
    public final static String LASTMOD = "lastMod";
    public final static String SIZE    = "size";

    /** things set by constructor */
    public String dir;  //with \\ or / separators. With trailing slash (to match).
    private char fromSlash, toSlash;
    public String regex;
    public boolean recursive;
    static boolean OSIsWindows = String2.OSIsWindows;
    public Table table = new Table();
    /** dirs will have \\ or / like original constructor tDir, and a matching trailing slash. */
    public StringArray dirPA    = new StringArray();
    public StringArray namePA   = new StringArray();
    public LongArray lastModPA  = new LongArray();
    public LongArray sizePA     = new LongArray();


    /** 
     * The constructor.
     * Usage: see useIt().
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing /.  
     *    The resulting dirPA will contain dirs with matching slashes.
     */
    public FileVisitorDNLS(String tDir, String tRegex, boolean tRecursive) {
        super();

        dir = File2.addSlash(tDir);
        toSlash = dir.indexOf('\\') >= 0? '\\' : '/';
        fromSlash = toSlash == '/'? '\\' : '/';        
        recursive = tRecursive;
        regex = tRegex;
        table.addColumn(DIR,     dirPA);
        table.addColumn(NAME,    namePA);
        table.addColumn(LASTMOD, lastModPA);
        table.addColumn(SIZE,    sizePA);
    }

    /** Invoked before entering a directory. */
    public FileVisitResult preVisitDirectory(Path tDir, BasicFileAttributes attrs)
        throws IOException {
        
        String ttDir = String2.replaceAll(tDir.toString(), fromSlash, toSlash) + toSlash;
        return recursive || ttDir.equals(dir)? FileVisitResult.CONTINUE : 
            FileVisitResult.SKIP_SUBTREE;    
    }

    /** Invoked for a file in a directory. */
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
        throws IOException {

        String name = file.getFileName().toString();
        if (!(name.matches(regex))) 
            return FileVisitResult.CONTINUE;    

        int oSize = dirPA.size();
        try {
            //getParent returns \\ or /, without trailing /
            String ttDir = String2.replaceAll(file.getParent().toString(), fromSlash, toSlash) +
                toSlash;
            dirPA.add(    ttDir);
            namePA.add(   name);
            lastModPA.add(attrs.lastModifiedTime().toMillis());
            sizePA.add(   attrs.size());
            //for debugging only:
            //String2.log(ttDir + name + 
            //    " mod=" + attrs.lastModifiedTime().toMillis() +
            //    " size=" + attrs.size());
        } catch (Throwable t) {
            if (dirPA.size()     > oSize) dirPA.remove(oSize);
            if (namePA.size()    > oSize) namePA.remove(oSize);
            if (lastModPA.size() > oSize) lastModPA.remove(oSize);
            if (sizePA.size()    > oSize) sizePA.remove(oSize);
            String2.log(MustBe.throwableToString(t));
        }
    
        return FileVisitResult.CONTINUE;    
    }

    /** A convenience method for using this class. 
     * The results are in the global table and in the PA variables.
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     *    The resulting dirPA will contain dirs with matching slashes and trailing slash.
     * @return a table with columns with DIR, NAME, LASTMOD, and SIZE columns;
     */
    public static Table oneStep(String tDir, String tRegex, boolean tRecursive) 
        throws IOException {
        long time = System.currentTimeMillis();
        FileVisitorDNLS fv = new FileVisitorDNLS(tDir, tRegex, tRecursive);
        Files.walkFileTree(FileSystems.getDefault().getPath(tDir), fv);
        if (verbose) String2.log("FileVisitorDNLS.oneStep finished successfully. n=" + 
            fv.dirPA.size() + " time=" +
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

        table = oneStep(contextDir + "WEB-INF/fonts", ".*BI\\.ttf", false); 
        String results = table.dataToCSVString();
        String expected = 
"dir,name,lastMod,size\n" +
"C:/programs/tomcat/webapps/cwexperimental/WEB-INF/fonts/,VeraBI.ttf,1050501734000,63208\n" +
"C:/programs/tomcat/webapps/cwexperimental/WEB-INF/fonts/,VeraMoBI.ttf,1050501734000,55032\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String unexpected = 
            "\nUnexpected FileVisitorDNLS error (but /data/gtspp/temp dir has variable nFiles):\n";

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                //forward slash in huge directory
                time = System.currentTimeMillis();
                table = oneStep("/data/gtspp/temp", ".*\\.nc", false); 
                time = System.currentTimeMillis() - time;
                //2014-11-25 98436 files in 410ms
                StringArray dirPA = (StringArray)table.getColumn(DIR);
                String2.log("forward test: n=" + dirPA.size() + " time=" + time);
                if (dirPA.size() < 1000) {
                    String2.log(dirPA.size() + " files. Not a good test.");
                } else {
                    Test.ensureBetween(time / (double)dirPA.size(), 2e-3, 8e-3, 
                        "ms/file (4.1e-3 expected)");
                    String dir0 = dirPA.get(0);
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
                table = oneStep("\\data\\gtspp\\temp", ".*\\.nc", false); 
                time = System.currentTimeMillis() - time;
                //2014-11-25 98436 files in 300ms
                StringArray dirPA = (StringArray)table.getColumn(DIR);
                String2.log("backward test: n=" + dirPA.size() + " time=" + time);
                if (dirPA.size() < 1000) {
                    String2.log(dirPA.size() + " files. Not a good test.");
                } else {
                    Test.ensureBetween(time / (double)dirPA.size(), 1.5e-3, 8e-3,
                        "ms/file (3e-3 expected)");
                    String dir0 = dirPA.get(0);
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
