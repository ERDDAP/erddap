package gov.noaa.pfel.coastwatch.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tags.TagSlowTests;

class SharedWatchServiceTests {

  private static class EventHandler implements WatchUpdateHandler {

    public boolean reloadRequested = false;
    public StringArray contexts = new StringArray();

    public void resetTest() {
      reloadRequested = false;
      contexts = new StringArray();
    }

    @Override
    public void doReload() {
      reloadRequested = true;
    }

    @Override
    public void handleUpdates(StringArray contexts) throws Throwable {
      this.contexts = contexts;
    }
  }

  /** This tests this class. */
  @Test
  @TagSlowTests
  void basicTest() throws Throwable {
    EventHandler eventHandler = new EventHandler();

    StringArray contexts = new StringArray();
    String testDataDir =
        Path.of(WatchDirectoryTests.class.getResource("/data/").toURI())
            .toString()
            .replace('\\', '/');
    String sourceDir = testDataDir;
    String watchDir = testDataDir + "/watchService";
    String subDirNS = testDataDir + "/watchService/watchSub";
    String file1 = "/columnarAsciiWithComments.txt";
    String file2 = "/csvAscii.txt";
    String results;
    int sleep = 2000;

    // delete all files in watchDir and subdir
    try {
      RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    } catch (Exception e) {

    }
    Math2.sleep(sleep);
    SharedWatchService.watchDirectory(
        watchDir, false, null, eventHandler, "basicTest"); // recursive, pathRegex

    // programmatic test: copy files into dirs
    File2.copy(sourceDir + file1, watchDir + file1);
    File2.copy(sourceDir + file2, subDirNS + file2); // won't notice
    Math2.sleep(sleep);
    SharedWatchService.processEvents();
    contexts = eventHandler.contexts;
    for (int i = 0; i < contexts.size(); i++) {
      results = contexts.get(i);
      // String2.log("results i=" + i + "=\n" + results);
      assertTrue(
          results.equals(watchDir + file1) || results.equals(subDirNS), // !
          "");
    }
    assertFalse(eventHandler.reloadRequested);
    assertTrue(
        contexts.size() >= 1 && contexts.size() <= 4); // sometimes the dir event isn't caught
    eventHandler.resetTest();
    // programmatic test: delete files
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    SharedWatchService.processEvents();
    contexts = eventHandler.contexts;
    for (int i = 0; i < contexts.size(); i++) {
      results = contexts.get(i);
      // String2.log("results i=" + i + "=\n" + results);
      assertTrue(results.equals(watchDir + file1) || results.equals(subDirNS));
    }
    assertFalse(eventHandler.reloadRequested);
    assertTrue(
        contexts.size() >= 1 && contexts.size() <= 3,
        "*** KNOWN PROBLEM: Sometimes nEvents=0 because the dir events weren't caught (because computer busy?).");
    eventHandler.resetTest();

    // *** test recursive
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    SharedWatchService.watchDirectory(
        watchDir, true, "", eventHandler, "basicTest"); // recursive, pathRegex

    // programmatic test: copy files into dirs
    File2.copy(sourceDir + file1, watchDir + file1);
    File2.copy(sourceDir + file2, subDirNS + file2);
    Math2.sleep(sleep);
    SharedWatchService.processEvents();
    contexts = eventHandler.contexts;
    for (int i = 0; i < contexts.size(); i++) {
      results = contexts.get(i);
      // String2.log("results i=" + i + "=\n" + results);
      assertTrue(
          results.equals(watchDir + file1)
              || results.equals(subDirNS + file2)
              || results.equals(subDirNS));
    }
    assertFalse(eventHandler.reloadRequested);
    assertTrue(
        contexts.size() >= 4 && contexts.size() <= 5); // sometimes the dir event isn't caught
    eventHandler.resetTest();

    // programmatic test: delete files
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    SharedWatchService.processEvents();
    contexts = eventHandler.contexts;
    for (int i = 0; i < contexts.size(); i++) {
      results = contexts.get(i);
      // String2.log("results i=" + i + "=\n" + results);
      assertTrue(
          results.equals(watchDir + file1)
              || results.equals(subDirNS + file2)
              || results.equals(subDirNS));
    }
    // on linux modify events are not created during this delete
    // also a subdir deletion was previously checked for here,
    // but RegexFilenameFilter.regexDelete explicitly excludes directories
    // from matching (directoriesToo is false), so only two events are possible here
    // (the two file DELETEs)
    assertFalse(eventHandler.reloadRequested);
    assertTrue(contexts.size() >= 2 && contexts.size() <= 5);
  }
}
