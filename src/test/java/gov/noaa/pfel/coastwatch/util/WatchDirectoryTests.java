package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import tags.TagIncompleteTest;
import tags.TagSlowTests;

class WatchDirectoryTests {
  /** This tests this class. */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void basicTest() throws Throwable {
    // String2.log("\n*** WatchDirectory.basicTest");
    // verbose = true;
    ArrayList<WatchEvent.Kind> eventKinds = new ArrayList();
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
    int n;
    // On Bob's M4700, even 2000 isn't sufficient to reliably catch all events
    int sleep = 2000;

    // *** test not recursive
    String2.log("test not recursive " + WatchDirectory.CREATE);
    // delete all files in watchDir and subdir
    try {
      RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    } catch (Exception e) {

    }
    Math2.sleep(sleep);
    WatchDirectory wd =
        WatchDirectory.watchDirectoryAll(watchDir, false, null); // recursive, pathRegex

    // programmatic test: copy files into dirs
    File2.copy(sourceDir + file1, watchDir + file1);
    File2.copy(sourceDir + file2, subDirNS + file2); // won't notice
    Math2.sleep(sleep);
    n = wd.getEvents(eventKinds, contexts);
    for (int i = 0; i < n; i++) {
      WatchEvent.Kind kind = eventKinds.get(i);
      results = kind + " " + contexts.get(i);
      String2.log("results i=" + i + "=\n" + results);
      Test.ensureTrue(
          results.equals(WatchDirectory.CREATE + " " + watchDir + file1)
              || results.equals(WatchDirectory.MODIFY + " " + watchDir + file1)
              || results.equals(WatchDirectory.DELETE + " " + watchDir + file1)
              || results.equals(WatchDirectory.MODIFY + " " + subDirNS), // !
          "");
      // ensure testing via '==' works
      Test.ensureTrue(
          kind == WatchDirectory.CREATE
              || kind == WatchDirectory.MODIFY
              || kind == WatchDirectory.DELETE,
          "kind=" + kind);
    }
    Test.ensureBetween(n, 1, 4, ""); // sometimes the dir event isn't caught

    // programmatic test: delete files
    String2.log("test not recursive " + WatchDirectory.DELETE);
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    n = wd.getEvents(eventKinds, contexts);
    for (int i = 0; i < n; i++) {
      WatchEvent.Kind kind = eventKinds.get(i);
      results = kind + " " + contexts.get(i);
      String2.log("results i=" + i + "=\n" + results);
      Test.ensureTrue(
          results.equals(WatchDirectory.DELETE + " " + watchDir + file1)
              || results.equals(WatchDirectory.MODIFY + " " + watchDir + file1)
              || results.equals(WatchDirectory.MODIFY + " " + subDirNS),
          "");
      // ensure testing via '==' works
      Test.ensureTrue(
          kind == WatchDirectory.CREATE
              || kind == WatchDirectory.MODIFY
              || kind == WatchDirectory.DELETE,
          "kind=" + kind);
    }
    Test.ensureBetween(
        n,
        1,
        3,
        "*** KNOWN PROBLEM: Sometimes nEvents=0 because the dir events weren't caught (because computer busy?).");

    // *** test recursive
    String2.log("test recursive " + WatchDirectory.CREATE);
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    wd = WatchDirectory.watchDirectoryAll(watchDir, true, ""); // recursive, pathRegex

    // programmatic test: copy files into dirs
    File2.copy(sourceDir + file1, watchDir + file1);
    File2.copy(sourceDir + file2, subDirNS + file2);
    Math2.sleep(sleep);
    n = wd.getEvents(eventKinds, contexts);
    for (int i = 0; i < n; i++) {
      results = eventKinds.get(i) + " " + contexts.get(i);
      String2.log("results i=" + i + "=\n" + results);
      Test.ensureTrue(
          results.equals(WatchDirectory.CREATE + " " + watchDir + file1)
              || results.equals(WatchDirectory.MODIFY + " " + watchDir + file1)
              || results.equals(WatchDirectory.DELETE + " " + watchDir + file1)
              || results.equals(WatchDirectory.CREATE + " " + subDirNS + file2)
              || results.equals(WatchDirectory.MODIFY + " " + subDirNS + file2)
              || results.equals(WatchDirectory.MODIFY + " " + subDirNS),
          "");
    }
    Test.ensureBetween(n, 4, 5, ""); // sometimes the dir event isn't caught

    // programmatic test: delete files
    String2.log("test recursive " + WatchDirectory.DELETE);
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    n = wd.getEvents(eventKinds, contexts);
    for (int i = 0; i < n; i++) {
      results = eventKinds.get(i) + " " + contexts.get(i);
      String2.log("results i=" + i + "=\n" + results);
      Test.ensureTrue(
          results.equals(WatchDirectory.DELETE + " " + watchDir + file1)
              || results.equals(WatchDirectory.MODIFY + " " + watchDir + file1)
              || results.equals(WatchDirectory.DELETE + " " + subDirNS + file2)
              || results.equals(WatchDirectory.MODIFY + " " + subDirNS + file2)
              || results.equals(WatchDirectory.MODIFY + " " + subDirNS),
          "");
    }
    // on linux modify events are not created during this delete
    // also a subdir deletion was previously checked for here,
    // but RegexFilenameFilter.regexDelete explicitly excludes directories
    // from matching (directoriesToo is false), so only two events are possible here
    // (the two file DELETEs)
    Test.ensureBetween(n, 2, 5, "");

    // *** test creating a huge number
    // This is allowed on Windows. It doesn't appear to have max number.
    // ADVICE TO ADMINS: 2015-03-10 [revised 2015-09-01]
    // On Linux computers,
    // if you are using <updateEveryNMillis> with EDDGridFromFiles or
    // EDDTableFromFiles
    // classes, you may see a problem where a dataset fails to load with the
    // error message:
    // "IOException: User limit of inotify instances reached or too many open
    // files".
    // see messages.xml: <inotifyFix>
    // You can fix this problem by calling (as root):
    // echo fs.inotify.max_user_watches=524288 | tee -a /etc/sysctl.conf
    // echo fs.inotify.max_user_instances=2048 | tee -a /etc/sysctl.conf
    // sysctl -p
    // Or, use higher numbers if the problem persists.
    // The default for watches is 8192. The default for instances is 128.
    // ***
    // see current limit
    // cat /proc/sys/fs/inotify/max_user_watches
    // cat /proc/sys/fs/inotify/max_user_instances
    // from
    // https://github.com/guard/listen/wiki/Increasing-the-amount-of-inotify-watchers
    // VERY SLOW! NOT USUALLY DONE:
    // String2.log("test all dirs on this computer: watchDirectoryAll(\"/\",
    // true)");
    // wd = watchDirectoryAll("/", true);

    // never finished this test since Windows allows large number of watches.
    // n = 10000;
    // String2.log("test huge number=" + n);
    // WatchDirectory wdar[] = new WatchDirectory[n];
    // for (int i = 0; i < n; i++) {
    // if (i % 10 == 0) Math2.gc(100); //does gc make the error go away?
    // wdar[i] = watchDirectoryAll(watchDir, true);
    // }
  }

  /** This tests this class. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void interactiveTest() throws Throwable {
    // String2.log("\n*** WatchDirectory.interactiveTest");
    // verbose = true;
    ArrayList<WatchEvent.Kind> eventKinds = new ArrayList();
    StringArray contexts = new StringArray();
    String sourceDir = Path.of(WatchDirectoryTests.class.getResource("/data/").toURI()).toString();
    String watchDir = sourceDir + "/watchService/";
    // String subDir = String2.unitTestDataDir + "watchService/watchSub/";
    // String subDirNS = String2.unitTestDataDir + "watchService/watchSub";
    // String file1 = "columnarAscii.txt";
    // String file2 = "csvAscii.txt";
    // String results;
    int n;
    // On Bob's M4700, even 2000 isn't sufficient to reliably catch all events
    int sleep = 2000;

    // *** interactive test
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);
    Math2.sleep(sleep);
    WatchDirectory wd =
        WatchDirectory.watchDirectoryAll(watchDir, true, ""); // recursive, pathRegex
    while (true) {
      String s =
          String2.getStringFromSystemIn(
              "WatchDirectory interactive test (recursive):\n"
                  + "  Enter '' to see events in "
                  + watchDir
                  + ", 'exit' to exit this loop, or ^C...");
      if ("exit".equals(s)) break;

      n = wd.getEvents(eventKinds, contexts);
      for (int i = 0; i < n; i++) {
        WatchEvent.Kind kind = eventKinds.get(i);
        String2.log("results i=" + i + "=\n" + kind + " " + contexts.get(i));
        // ensure testing via '==' works
        Test.ensureTrue(
            kind == WatchDirectory.CREATE
                || kind == WatchDirectory.MODIFY
                || kind == WatchDirectory.DELETE
                || kind == WatchDirectory.OVERFLOW,
            "kind=" + kind);
      }
    }
    RegexFilenameFilter.regexDelete(watchDir, ".*", true);

    String2.log("\n*** WatchDirectory.interactiveTest finished.");
  }
}
