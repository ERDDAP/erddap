/* 
 * WatchDirectory Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an easy way to use a WatchService.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2014-12-30
 */
public class WatchDirectory {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 
    public final static WatchEvent.Kind CREATE   = StandardWatchEventKinds.ENTRY_CREATE;
    public final static WatchEvent.Kind DELETE   = StandardWatchEventKinds.ENTRY_DELETE;
    public final static WatchEvent.Kind MODIFY   = StandardWatchEventKinds.ENTRY_MODIFY;
    public final static WatchEvent.Kind OVERFLOW = StandardWatchEventKinds.OVERFLOW;

    /** things set by constructor */
    private String watchDir;
    private boolean recursive;
    private WatchService watchService;
    private ConcurrentHashMap keyToDirMap = new ConcurrentHashMap(); 


    /** 
     * A convenience method to construct a WatchDirectory for all events 
     * (CREATE, DELETE, and MODIFY).  (OVERFLOW is automatically included.)
     */
    public static WatchDirectory watchDirectoryAll(String dir, boolean recursive) 
        throws IOException {
        return new WatchDirectory(dir, recursive, new WatchEvent.Kind[]{
            CREATE, DELETE, MODIFY});
    }

    /** 
     * The constructor.
     *
     * @param tWatchDir the starting directory, with \\ or /, with or without trailing slash.  
     *    The results will contain dirs with matching slashes.
     * @param tRecursive
     * @param events some combination of CREATE, DELETE, MODIFY, e.g.,
     *   new WatchEvent.Kind[]{CREATE, DELETE, MODIFY}
     *   OVERFLOW events are always automatically included -- don't specify them here.
     * @throws various Exceptions if trouble
     */
    public WatchDirectory(String tWatchDir, boolean tRecursive, WatchEvent.Kind events[]) 
        throws IOException {

        watchDir = File2.addSlash(tWatchDir);
        recursive = tRecursive;
        char toSlash = watchDir.indexOf('\\') >= 0? '\\' : '/';
        char fromSlash = toSlash == '/'? '\\' : '/';        

        Path watchPath = Paths.get(watchDir);
        if (watchPath == null) 
            throw new RuntimeException("Directory not found: " + watchDir);

        //make the WatchService 
        watchService = watchPath.getFileSystem().newWatchService();
        if (watchService == null)
            throw new RuntimeException(
                "The OS doesn't support WatchService for that file system.");
        if (recursive) {
            ArrayList<Path> alps = FileVisitorSubdir.oneStep(watchDir); 
            int n = alps.size();
            for (int i = 0; i < n; i++) {
                WatchKey key = alps.get(i).register(watchService, events);
                keyToDirMap.put(key, String2.canonical(File2.addSlash(
                    String2.replaceAll(alps.get(i).toString(), fromSlash, toSlash))));
            }
        } else {
            WatchKey key = watchPath.register(watchService, events);
            keyToDirMap.put(key, String2.canonical(File2.addSlash(
                String2.replaceAll(watchDir, fromSlash, toSlash))));
        }
    }

    /**
     * This gets all the queued events.
     * Events from a given directory are grouped together, 
     * but the order in the group is not specified.
     * On Bob's Dell M4700, the queue can hold 500 events per directory, 
     * but not much more before it switches to just OVERFLOW 
     * (and all other events for that directory are lost).
     *
     * @param eventKinds This will receive the eventKind for each event.
     *   You can test with '==' for CREATE, DELETE, MODIFY, and OVERFLOW.
     * @param contexts This will receive the full context for each event.
     *    They always start with the full path, with slashes (forward or back)
     *    matching the slashes in the originally specified watchDir.
     *    The fileName part may be a fileName, a subdirctory name (without trailing slash),
     *    or "" for OVERFLOW.
     *    Note that new subdirectories aren't watched. 
     *    You have to create a new WatchDirectory.
     * @return the size of eventKinds and contexts
     */
    public int getEvents(ArrayList<WatchEvent.Kind> eventKinds, StringArray contexts) {
        eventKinds.clear();
        contexts.clear();
        WatchKey key = watchService.poll(); 
        while (key != null) {
            String tDir = (String)keyToDirMap.get(key);
            for (WatchEvent event : key.pollEvents()) {
                eventKinds.add(event.kind());
                contexts.add(tDir +
                    (event.context() == null? "" : event.context()));
            }
            key.reset();
            key = watchService.poll(); 
        }
        return eventKinds.size();
    }


    /** 
     * This tests this class. 
     */
    public static void test(boolean doInteractiveTest) throws Throwable {
        String2.log("\n*** WatchDirectory.test");
        verbose = true;
        ArrayList<WatchEvent.Kind> eventKinds = new ArrayList();
        StringArray contexts  = new StringArray();
        String sourceDir = "/erddapTest/";
        String watchDir  = "/erddapTest/watchService/";
        String subDir    = "/erddapTest/watchService/watchSub/";
        String subDirNS  = "/erddapTest/watchService/watchSub";
        String file1     = "columnarAscii.txt";
        String file2     = "csvAscii.txt";
        String results;
        int n;
        //On Bob's M4700, even 2000 isn't sufficient to reliably catch all events
        int sleep = 2000; 

        //*** test not recursive
        String2.log("test not recursive " + CREATE);
        //delete all files in watchDir and subdir  
        RegexFilenameFilter.regexDelete(watchDir, ".*", true);       
        Math2.sleep(sleep);
        WatchDirectory wd = watchDirectoryAll(watchDir, false);

        //programmatic test: copy files into dirs
        File2.copy(sourceDir + file1, watchDir + file1);
        File2.copy(sourceDir + file2, subDir   + file2); //won't notice
        Math2.sleep(sleep);
        n = wd.getEvents(eventKinds, contexts);
        for (int i = 0; i < n; i++) {
            WatchEvent.Kind kind = eventKinds.get(i);
            results = kind + " " + contexts.get(i);
            String2.log("  " + results);
            Test.ensureTrue(
                results.equals(CREATE + " " + watchDir + file1) ||
                results.equals(MODIFY + " " + watchDir + file1) ||
                results.equals(MODIFY + " " + subDirNS), // !
                "");
            //ensure testing via '==' works
            Test.ensureTrue(kind == CREATE || kind == MODIFY || kind == DELETE, 
                "kind=" + kind);
        }
        Test.ensureBetween(n, 2, 3, ""); //sometimes the dir event isn't caught

        //programmatic test: delete files 
        String2.log("test not recursive " + DELETE);
        RegexFilenameFilter.regexDelete(watchDir, ".*", true);       
        Math2.sleep(sleep);
        n = wd.getEvents(eventKinds, contexts);
        for (int i = 0; i < n; i++) {
            WatchEvent.Kind kind = eventKinds.get(i);
            results = kind + " " + contexts.get(i);
            String2.log("  " + results);
            Test.ensureTrue(
                results.equals(DELETE + " " + watchDir + file1) ||
                results.equals(MODIFY + " " + watchDir + file1) ||
                results.equals(MODIFY + " " + subDirNS),
                "");
            //ensure testing via '==' works
            Test.ensureTrue(kind == CREATE || kind == MODIFY || kind == DELETE, 
                "kind=" + kind);
        }
        Test.ensureBetween(n, 2, 3, ""); //sometimes the dir event isn't caught


        //*** test recursive
        String2.log("test recursive " + CREATE);
        RegexFilenameFilter.regexDelete(watchDir, ".*", true);       
        Math2.sleep(sleep);
        wd = watchDirectoryAll(watchDir, true);

        //programmatic test: copy files into dirs
        File2.copy(sourceDir + file1, watchDir + file1);
        File2.copy(sourceDir + file2, subDir   + file2);
        Math2.sleep(sleep);
        n = wd.getEvents(eventKinds, contexts);
        for (int i = 0; i < n; i++) {
            results = eventKinds.get(i) + " " + contexts.get(i);
            String2.log("  " + results);
            Test.ensureTrue(
                results.equals(CREATE + " " + watchDir + file1) ||
                results.equals(MODIFY + " " + watchDir + file1) ||
                results.equals(CREATE + " " + subDir   + file2) ||
                results.equals(MODIFY + " " + subDir   + file2) ||
                results.equals(MODIFY + " " + subDirNS),
                "");
        }
        Test.ensureBetween(n, 4, 5, ""); //sometimes the dir event isn't caught

        //programmatic test: delete files 
        String2.log("test recursive " + DELETE);
        RegexFilenameFilter.regexDelete(watchDir, ".*", true);       
        Math2.sleep(sleep);
        n = wd.getEvents(eventKinds, contexts);
        for (int i = 0; i < n; i++) {
            results = eventKinds.get(i) + " " + contexts.get(i);
            String2.log("  " + results);
            Test.ensureTrue(
                results.equals(DELETE + " " + watchDir + file1) ||
                results.equals(MODIFY + " " + watchDir + file1) ||
                results.equals(DELETE + " " + subDir   + file2) ||
                results.equals(MODIFY + " " + subDir   + file2) ||
                results.equals(MODIFY + " " + subDirNS),
                "");
        }
        Test.ensureBetween(n, 4, 5, ""); //sometimes the dir event isn't caught

        //*** test creating a huge number 
        //This is allowed on Windows. It doesn't appear to have max number.
// ADVICE TO ADMINS: 2015-03-10
//  On Linux computers, 
//  if you are using <updateEveryNMillis> with EDDGridFromFiles or EDDTableFromFiles
//  classes, you may see a problem where a dataset fails to load with the 
//  error message:
//  "IOException: User limit of inotify instances reached or too many open files".
//  If so, you can fix this problem by calling (as root):
//    echo 10000 > /proc/sys/fs/inotify/max_user_watches
//    echo 10000 > /proc/sys/fs/inotify/max_user_instances
//  Or, use higher numbers if the problem persists.
//  The default for watches is 8192. The default for instances is 128.
        //VERY SLOW! NOT USUALLY DONE:
        //String2.log("test all dirs on this computer: watchDirectoryAll(\"/\", true)");
        //wd = watchDirectoryAll("/", true);

        //never finished this test since Windows allows large number of watches.
        //n = 10000;
        //String2.log("test huge number=" + n);
        //WatchDirectory wdar[] = new WatchDirectory[n];
        //for (int i = 0; i < n; i++) {
//            if (i % 10 == 0) Math2.gc(100); //does gc make the error go away?
        //    wdar[i] = watchDirectoryAll(watchDir, true);
        //}         


        //*** interactive test
        RegexFilenameFilter.regexDelete(watchDir, ".*", true);       
        Math2.sleep(sleep);
        wd = watchDirectoryAll(watchDir, true);
        while (doInteractiveTest) {  
            String s = String2.getStringFromSystemIn(
                "WatchDirectory interactive test (recursive):\n" +
                "  Enter '' to see events in " + watchDir + 
                ", 'exit' to exit this loop, or ^C..."); 
            if ("exit".equals(s)) break;

            n = wd.getEvents(eventKinds, contexts);
            for (int i = 0; i < n; i++) {
                WatchEvent.Kind kind = eventKinds.get(i);
                String2.log("  " + kind + " " + contexts.get(i));
                //ensure testing via '==' works
                Test.ensureTrue(kind == CREATE || kind == MODIFY || kind == DELETE ||
                    kind == OVERFLOW, "kind=" + kind);
            }
        }
        RegexFilenameFilter.regexDelete(watchDir, ".*", true);       

        String2.log("\n*** WatchDirectory.test finished.");
    }

}
