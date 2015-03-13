/* 
 * FileVisitorSubdir Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;


/**
 * This class gathers a list of subdir names (including initial dir).
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

    public ArrayList<Path> results = new ArrayList();


    /** 
     * The constructor.
     * Usage: see useIt().
     *
     * @param tDir The starting directory, with \\ or /, with or without trailing slash.  
     */
    public FileVisitorSubdir(String tDir) {
        super();
        dir = File2.addSlash(tDir);
    }

    /** Invoked before entering a directory. */
    public FileVisitResult preVisitDirectory(Path tPath, BasicFileAttributes attrs)
        throws IOException {
        
        results.add(tPath);
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
     * @return an ArrayList&lt;Path&gt; with the dir and subdir paths.
     *   Note that path.toString() returns the full dir name with the OS's slashes
     *   (\\ for Windows!).
     */
    public static ArrayList<Path> oneStep(String tDir) 
        throws IOException {
        long time = System.currentTimeMillis();
        FileVisitorSubdir fv = new FileVisitorSubdir(tDir);
        Files.walkFileTree(FileSystems.getDefault().getPath(tDir), fv);
        if (verbose) String2.log("FileVisitorSubdir.oneStep finished successfully. n=" + 
            fv.results.size() + " time=" + (System.currentTimeMillis() - time));
        return fv.results;
    }

    /** 
     * This tests this class. 
     */
    public static void test() throws Throwable {
        String2.log("\n*** FileVisitorSubdir.test");
        verbose = true;
        String contextDir = SSR.getContextDirectory(); //with / separator and / at the end
        ArrayList<Path> alps;
        long time;

        alps = oneStep(contextDir + "WEB-INF/classes/com/cohort"); 
        String results = String2.toNewlineString(alps.toArray());
        String expected = 
"C:\\programs\\tomcat\\webapps\\cwexperimental\\WEB-INF\\classes\\com\\cohort\n" +
"C:\\programs\\tomcat\\webapps\\cwexperimental\\WEB-INF\\classes\\com\\cohort\\array\n" +
"C:\\programs\\tomcat\\webapps\\cwexperimental\\WEB-INF\\classes\\com\\cohort\\ema\n" +
"C:\\programs\\tomcat\\webapps\\cwexperimental\\WEB-INF\\classes\\com\\cohort\\util\n";
        Test.ensureEqual(results, expected, "results=\\n" + results);

        alps = oneStep(String2.replaceAll(contextDir + "WEB-INF/classes/com/cohort/", '/', '\\')); 
        results = String2.toNewlineString(alps.toArray());
        Test.ensureEqual(results, expected, "results=\\n" + results);

        String2.log("\n*** FileVisitorSubdir.test finished.");
    }


}
