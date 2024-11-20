/*
 * WatchDirectory Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an easy way to use a WatchService.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2014-12-30
 */
public class WatchDirectory implements AutoCloseable {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;
  public static final WatchEvent.Kind<Path> CREATE = StandardWatchEventKinds.ENTRY_CREATE;
  public static final WatchEvent.Kind<Path> DELETE = StandardWatchEventKinds.ENTRY_DELETE;
  public static final WatchEvent.Kind<Path> MODIFY = StandardWatchEventKinds.ENTRY_MODIFY;
  public static final WatchEvent.Kind<Object> OVERFLOW = StandardWatchEventKinds.OVERFLOW;

  private WatchService watchService;
  private ConcurrentHashMap<WatchKey, String> keyToDirMap = new ConcurrentHashMap<>();

  /**
   * A convenience method to construct a WatchDirectory for all events (CREATE, DELETE, and MODIFY).
   * (OVERFLOW is automatically included.)
   */
  public static WatchDirectory watchDirectoryAll(String dir, boolean recursive, String pathRegex)
      throws IOException {
    return new WatchDirectory(
        dir, recursive, pathRegex, new WatchEvent.Kind[] {CREATE, DELETE, MODIFY});
  }

  /**
   * The constructor.
   *
   * @param tWatchDir the starting directory, with \\ or /, with or without trailing slash. The
   *     results will contain dirs with matching slashes.
   * @param tRecursive
   * @param tPathRegex null and "" are treated like .* (which matches all)
   * @param events some combination of CREATE, DELETE, MODIFY, e.g., new WatchEvent.Kind[]{CREATE,
   *     DELETE, MODIFY} OVERFLOW events are always automatically included -- don't specify them
   *     here.
   * @throws various Exceptions if trouble
   */
  public WatchDirectory(
      String tWatchDir, boolean tRecursive, String tPathRegex, WatchEvent.Kind<?> events[])
      throws IOException {

    /** things set by constructor */
    String watchDir = File2.addSlash(tWatchDir);

    Path watchPath = Paths.get(watchDir);
    if (watchPath == null) throw new RuntimeException("Directory not found: " + watchDir);

    // make the WatchService
    FileSystem fs = watchPath.getFileSystem();
    if (fs == null)
      throw new RuntimeException("getFileSystem returned null for the " + watchDir + " path.");
    watchService = fs.newWatchService();
    if (watchService == null)
      throw new RuntimeException("The OS doesn't support WatchService for that file system.");
    if (tRecursive) {
      StringArray alps =
          FileVisitorSubdir.oneStep( // throws IOException if "Too many open files"
              watchDir, tPathRegex); // will have matching slashes and trailing slashes
      int n = alps.size();
      for (int i = 0; i < n; i++) {
        WatchKey key = Paths.get(alps.get(i)).register(watchService, events);
        keyToDirMap.put(key, alps.get(i));
      }
    } else {
      WatchKey key = watchPath.register(watchService, events);
      keyToDirMap.put(key, String2.canonical(watchDir));
    }

    EDStatic.cleaner.register(this, new CleanupWatchService(watchService));
  }

  private static class CleanupWatchService implements Runnable {

    private final WatchService watchService;

    private CleanupWatchService(WatchService watchService) {
      this.watchService = watchService;
    }

    @Override
    public void run() {
      try {
        if (watchService != null) watchService.close();
      } catch (Throwable t1) {
        // do nothing, so nothing can go wrong.
      }
    }
  }

  /**
   * This gets all the queued events. Events from a given directory are grouped together, but the
   * order in the group is not specified. On Bob's Dell M4700, the queue can hold 500 events per
   * directory, but not much more before it switches to just OVERFLOW (and all other events for that
   * directory are lost).
   *
   * @param eventKinds This will receive the eventKind for each event. You can test with '==' for
   *     CREATE, DELETE, MODIFY, and OVERFLOW.
   * @param contexts This will receive the full context for each event. They always start with the
   *     full path, with slashes (forward or back) matching the slashes in the originally specified
   *     watchDir. The fileName part may be a fileName, a subdirctory name (without trailing slash),
   *     or "" for OVERFLOW. Note that new subdirectories aren't watched. You have to create a new
   *     WatchDirectory.
   * @return the size of eventKinds and contexts
   */
  public int getEvents(List<WatchEvent.Kind<?>> eventKinds, StringArray contexts) {
    eventKinds.clear();
    contexts.clear();
    if (watchService == null) // perhaps close() was called
    return 0;
    WatchKey key = watchService.poll();
    while (key != null) {
      String tDir = keyToDirMap.get(key);
      for (WatchEvent<?> event : key.pollEvents()) {
        eventKinds.add(event.kind());
        contexts.add(tDir + (event.context() == null ? "" : event.context()));
      }
      key.reset();
      key = watchService.poll();
    }
    return eventKinds.size();
  }

  /**
   * This explicitly closes this WatchDirectory and frees resources (threads). This won't throw an
   * exception.
   */
  @Override
  public void close() {
    try {
      if (watchService != null) watchService.close();
    } catch (Throwable t1) {
      // do nothing, so nothing can go wrong.
    }

    watchService = null; // safe, and encourages gc
    keyToDirMap = null; // safe, and encourages gc
  }
}
