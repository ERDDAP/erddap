package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class SharedWatchService {
  public static final WatchEvent.Kind<Path> CREATE = StandardWatchEventKinds.ENTRY_CREATE;
  public static final WatchEvent.Kind<Path> DELETE = StandardWatchEventKinds.ENTRY_DELETE;
  public static final WatchEvent.Kind<Path> MODIFY = StandardWatchEventKinds.ENTRY_MODIFY;
  public static final WatchEvent.Kind<Object> OVERFLOW = StandardWatchEventKinds.OVERFLOW;

  private static ConcurrentHashMap<WatchKey, String> keyToDirMap = new ConcurrentHashMap<>();
  private static ConcurrentHashMap<String, WatchService> fileSystemToService =
      new ConcurrentHashMap<>();
  private static ConcurrentHashMap<WatchKey, String> keyToHandlerId = new ConcurrentHashMap<>();
  private static ConcurrentHashMap<String, WatchUpdateHandler> handlerIdToHandler =
      new ConcurrentHashMap<>();

  /**
   * Registers a directory with the shared watch service.
   *
   * @param tWatchDir the starting directory, with \\ or /, with or without trailing slash. The
   *     results will contain dirs with matching slashes.
   * @param tRecursive
   * @param tPathRegex null and "" are treated like .* (which matches all)
   * @param handler the WatchUpdateHandler that should be called for matching events.
   * @param handlerId the identifier of the dataset
   * @throws various Exceptions if trouble
   */
  public static void watchDirectory(
      String tWatchDir,
      boolean tRecursive,
      String tPathRegex,
      WatchUpdateHandler handler,
      String handlerId)
      throws IOException {
    handlerIdToHandler.put(handlerId, handler);

    String watchDir = File2.addSlash(tWatchDir);

    Path watchPath = Paths.get(watchDir);
    if (watchPath == null) throw new RuntimeException("Directory not found: " + watchDir);

    // make the WatchService
    FileSystem fs = watchPath.getFileSystem();
    if (fs == null)
      throw new RuntimeException("getFileSystem returned null for the " + watchDir + " path.");

    String fsId = SharedWatchService.systemToID(fs);

    WatchService watchService = null;
    if (fileSystemToService.containsKey(fsId)) {
      watchService = fileSystemToService.get(fsId);
    }
    if (watchService == null) {
      watchService = fs.newWatchService();
      fileSystemToService.put(fsId, watchService);
    }
    if (watchService == null)
      throw new RuntimeException("The OS doesn't support WatchService for that file system.");
    if (tRecursive) {
      StringArray alps =
          FileVisitorSubdir.oneStep( // throws IOException if "Too many open files"
              watchDir, tPathRegex); // will have matching slashes and trailing slashes
      int n = alps.size();
      for (int i = 0; i < n; i++) {
        register(watchService, alps.get(i), handlerId);
      }
    } else {
      register(watchService, String2.canonical(watchDir), handlerId);
    }
  }

  private static void register(WatchService watchService, String directory, String handler)
      throws IOException {
    WatchKey key =
        Paths.get(directory).register(watchService, new WatchEvent.Kind[] {CREATE, DELETE, MODIFY});
    keyToDirMap.put(key, directory);
    keyToHandlerId.put(key, handler);
  }

  private static String systemToID(FileSystem system) {
    String id = "";
    for (FileStore store : system.getFileStores()) {
      id += store.name() + store.type();
    }
    return id;
  }

  /**
   * Process all events for keys queued to the watcher
   *
   * @throws Throwable
   */
  public static void processEvents() throws Throwable {
    // for each file service, get watch service

    HashMap<String, StringArray> contextsByHandler = new HashMap<>();
    HashSet<String> handlerToReset = new HashSet<>();

    for (String fsId : fileSystemToService.keySet()) {
      WatchService watchService = fileSystemToService.get(fsId);
      WatchKey key = null;
      while ((key = watchService.poll()) != null) {

        String dir = keyToDirMap.get(key);
        String handlerId = keyToHandlerId.get(key);
        if (dir == null) {
          System.err.println("WatchKey not recognized!!");
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind() == SharedWatchService.OVERFLOW) {
            handlerToReset.add(handlerId);
            // If we're doing a full reset we don't need to process the events.
            break;
          }
          String context = dir + (event.context() == null ? "" : event.context());
          if (contextsByHandler.containsKey(handlerId)) {
            contextsByHandler.get(handlerId).add(context);
          } else {
            StringArray contexts = new StringArray();
            contexts.add(context);
            contextsByHandler.put(handlerId, contexts);
          }
        }

        // reset key and remove from set if directory no longer accessible
        boolean valid = key.reset();
        if (!valid) {
          keyToDirMap.remove(key);
          keyToHandlerId.remove(key);

          // all directories are inaccessible
          if (keyToDirMap.isEmpty()) {
            break;
          }
        }
      }
    }

    for (String handlerId : handlerToReset) {
      // Remove the contexts for this handler, just trigger the full reset.
      contextsByHandler.remove(handlerId);
      handlerIdToHandler.get(handlerId).doReload();
    }

    for (String handlerId : contextsByHandler.keySet()) {
      handlerIdToHandler.get(handlerId).handleUpdates(contextsByHandler.get(handlerId));
    }
  }
}
