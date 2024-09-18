/*
 * DasDds Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

/**
 * This is a command line program to run EDD.testDasDds.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-06-05
 */
public class DasDds {

  static String logFileName = null;
  static String outFileName = null;
  Writer outFile = null;

  public DasDds() {
    logFileName = EDStatic.fullLogsDirectory + "DasDds.log";
    outFileName = EDStatic.fullLogsDirectory + "DasDds.out";
  }

  private void printToBoth(String s) throws IOException {
    String2.log(s);
    String2.flushLog();
    outFile.write(s);
    outFile.write('\n');
    outFile.flush();
  }

  /**
   * This gets the i'th value from args, or prompts the user.
   *
   * @return the value from the user (or null if user pressed ^C)
   */
  private String get(String args[], int i, String prompt, String def) throws Throwable {
    String s;
    if (args.length > i) {
      String2.log(prompt + "? " + (s = args[i]));
    } else {
      s = String2.getStringFromSystemIn(prompt + " (default=\"" + def + "\")\n? ");
      if (s == null) // null if ^C
      return s; // different than GenerateDatasetsXml
    }
    s = s.trim();
    if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
      s = String2.fromJson(s);
    if (s.length() == 0) s = def;
    return s;
  }

  /**
   * This is used when called from within a program. If args is null or args.length is 0, this
   * loops; otherwise it returns when done.
   *
   * @param args if args has values, they are used to answer the questions.
   * @return the contents of outFileName (will be "" if trouble)
   */
  public String doIt(String args[], boolean loop) throws Throwable {
    File2.safeRename(logFileName, logFileName + ".previous");
    if (File2.isFile(outFileName)) {
      try {
        File2.rename(outFileName, outFileName + ".previous");
      } catch (Throwable t) {
        File2.delete(outFileName);
      }
    }
    String2.setupLog(
        true,
        false, // toSystemOut, toSystemErr
        logFileName,
        true,
        String2.logFileDefaultMaxSize); // append
    String2.log(
        "*** Starting DasDds "
            + Calendar2.getCurrentISODateTimeStringLocalTZ()
            + " erddapVersion="
            + EDStatic.erddapVersion
            + "\n"
            + "logFile="
            + String2.logFileName()
            + "\n"
            + String2.standardHelpAboutMessage());
    // trick EDStatic.initialLoadDatasets by making majorLoadDatasetsTimeSeriesSB not empty
    EDStatic.majorLoadDatasetsTimeSeriesSB.append("\n");
    outFile = File2.getBufferedFileWriterUtf8(outFileName);
    try {

      // delete the old log files (pre 1.48 names)
      File2.delete(EDStatic.fullLogsDirectory + "DasDdsLog.txt");
      File2.delete(EDStatic.fullLogsDirectory + "DasDdsLog.txt.previous");

      String datasetID = "";
      if (args == null) args = new String[0];

      // look for -verbose (and remove it)
      boolean verbose = false; // actually controls reallyVerbose
      int vi = String2.indexOf(args, "-verbose");
      if (vi >= 0) {
        String2.log("verbose=true");
        verbose = true;
        StringArray sa = new StringArray(args);
        sa.remove(vi);
        args = sa.toArray();
      }

      do {
        // get the EDD type
        // EDD.reallyVerbose = false;  //sometimes while testing
        datasetID =
            get(
                args,
                0,
                "\n*** DasDds ***\n"
                    + "This generates the DAS and DDS for a dataset and puts it in\n"
                    + outFileName
                    + "\n"
                    + "Press ^D or ^C to exit at any time.\n\n"
                    + "Which datasetID",
                datasetID);
        if (datasetID == null) {
          String2.flushLog();
          outFile.flush();
          outFile.close();
          outFile = null;
          return File2.readFromFileUtf8(outFileName)[1];
        }

        try {
          printToBoth(EDD.testDasDds(true, datasetID, verbose)); // clearCache
        } catch (Throwable t) {
          String2.log(
              "\n*** An error occurred while trying to load "
                  + datasetID
                  + ":\n"
                  + MustBe.throwableToString(t));
        }
        String2.flushLog();

      } while (loop && args.length == 0);

      outFile.flush();
    } finally {
      outFile.close();
    }
    String ret = File2.readFromFileUtf8(outFileName)[1];
    String2.returnLoggingToSystemOut();
    return ret;
  }

  /**
   * This is used when called from the command line. It explicitly calls System.exit(0) when done.
   *
   * @param args if args has values, they are used to answer the question.
   */
  public static void main(String args[]) throws Throwable {

    String ecd = "erddapContentDirectory";
    String contentDirectory = System.getProperty(ecd);
    if (contentDirectory == null) {
      // Or, it must be sibling of webapps
      // e.g., c:/programs/_tomcat/webapps/erddap/WEB-INF/classes/[these classes]
      // On windows, contentDirectory may have spaces as %20(!)
      contentDirectory = File2.getClassPath(); // access a resource folder
      int po = contentDirectory.indexOf("/webapps/");
      if (po == -1) {
        Path userDir = Path.of(System.getProperty("user.dir"));
        String webInfParentDir = userDir.getParent().toString() + "/";
        File2.setWebInfParentDirectory(webInfParentDir);
        System.setProperty(ecd, webInfParentDir + "/development/test/");
      }
    }

    new DasDds().doIt(args, true);
    System.exit(0);
  }
}
