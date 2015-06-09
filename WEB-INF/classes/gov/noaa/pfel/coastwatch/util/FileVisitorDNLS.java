/* 
 * FileVisitorDNLS Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
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

    public final static String URL          = "url"; //in place of directory for oneStepAccessibleViaFiles

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

    /** table.dataToCSVString(); */
    public String resultsToString() {
        return table.dataToCSVString();
    }

    /**
     * This is a convenience method for using this class. 
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

    /** 
     * This is a variant of oneStep (a convenience method for using this class)
     * that returns lastModified as double epoch seconds and size as doubles (bytes)
     * with some additional metadata. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     *    The resulting directoryPA will contain dirs with matching slashes and trailing slash.
     * @return a table with columns with DIRECTORY, NAME, LASTMODIFIED, and SIZE columns.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's name will be "".
     */
    public static Table oneStepDouble(String tDir, String tRegex, boolean tRecursive,
        boolean tDirectoriesToo) throws IOException {

        Table tTable = oneStep(tDir, tRegex, tRecursive, tDirectoriesToo);
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
     * This is a variant of oneStepDouble (a convenience method for using this class)
     * that returns a url column instead of a directory column. 
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     * @param startOfUrl usually EDStatic.erddapUrl(loggedInAs) + "/files/" + datasetID() + "/"
     * @return a table with columns with DIRECTORY, NAME, LASTMODIFIED, and SIZE columns.
     *    If directoriesToo=true, the original dir won't be included and any 
     *    directory's name will be "".
     */
    public static Table oneStepAccessibleViaFiles(String tDir, String tRegex, boolean tRecursive,
        String startOfUrl) throws IOException {

        tDir = File2.addSlash(String2.replaceAll(tDir, "\\", "/")); //ensure forward/ and trailing/
        Table tTable = oneStepDouble(tDir, tRegex, tRecursive, false); //tDirectoriesToo
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
        table = oneStepAccessibleViaFiles("c:/erddapTest/fileNames", ".*\\.png", true, 
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
        String2.log("\n*** FileVisitorDNLS.test finished.");
    }


}
