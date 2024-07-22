/*
 * EDDTableFromFilesCallable Copyright 2018, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This class represents a virtual table of data by aggregating a collection of data files. <br>
 * The presumption is that the entire dataset can be read reasonable quickly (from the local files,
 * unlike remote data) and all variable's min and max info can be gathered (for each file) and
 * cached (facilitating handling constraints in data requests). <br>
 * And file data can be cached and reused because each file has a lastModified time and size which
 * can be used to detect if file is unchanged.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2018-07-26
 */
public class EDDTableFromFilesCallable implements Callable<Table> {

  /**
   * Set this to true (by calling debugMode=true in your program, not by changing the code here) if
   * you want every possible diagnostic message sent to String2.log.
   */
  public static boolean debugMode = false;

  int language;
  String identifier;
  int task;
  EDDTableFromFiles eddTableFromFiles;
  String loggedInAs;
  String requestUrl;
  String userDapQuery;
  int fileDirIndex;
  String fileDir, fileName;
  long fileLastMod;
  StringArray sourceDataNames;
  String sourceDataTypes[];
  double sortedSpacing, minSorted, maxSorted;
  StringArray sourceConVars, sourceConOps, sourceConValues;
  TableWriter tableWriter;

  public EDDTableFromFilesCallable(
      int tLanguage,
      String tIdentifier,
      EDDTableFromFiles tEDDTableFromFiles,
      String tLoggedInAs,
      String tRequestUrl,
      String tUserDapQuery,
      int tFileDirIndex,
      String tFileDir,
      String tFileName,
      long tFileLastMod,
      StringArray tSourceDataNames,
      String tSourceDataTypes[],
      double tSortedSpacing,
      double tMinSorted,
      double tMaxSorted,
      StringArray tSourceConVars,
      StringArray tSourceConOps,
      StringArray tSourceConValues)
      throws Throwable {

    language = tLanguage;
    identifier = tIdentifier;
    eddTableFromFiles = tEDDTableFromFiles;
    loggedInAs = tLoggedInAs;
    requestUrl = tRequestUrl;
    userDapQuery = tUserDapQuery;
    fileDirIndex = tFileDirIndex;
    fileDir = tFileDir;
    fileName = tFileName;
    fileLastMod = tFileLastMod;
    sourceDataNames = tSourceDataNames;
    sourceDataTypes = tSourceDataTypes;
    sortedSpacing = tSortedSpacing;
    minSorted = tMinSorted;
    maxSorted = tMaxSorted;
    sourceConVars = tSourceConVars;
    sourceConOps = tSourceConOps;
    sourceConValues = tSourceConValues;
  }

  /**
   * This gets data from one source file.
   *
   * @return a table if successfully got data, or null if no data
   * @throws Exception if trouble
   */
  @Override
  public Table call() throws Exception {
    try {
      // if (debugMode) String2.log(identifier + ": start call()");
      if (Thread.currentThread().interrupted()) // consume the interrupted status
      throw new InterruptedException();

      long startTime = System.currentTimeMillis();
      Table table;
      try {
        // file may be unavailable while being updated
        table =
            eddTableFromFiles.getSourceDataFromFile(
                fileDir,
                fileName,
                sourceDataNames,
                sourceDataTypes,
                sortedSpacing,
                minSorted,
                maxSorted,
                sourceConVars,
                sourceConOps,
                sourceConValues,
                false,
                true); // getMetadata, mustGetData  //???what about global att promoted to var?

      } catch (Throwable t2) {

        // if OutOfMemory or too much data (or some other reasons), rethrow t so request fails
        String t2String = t2.toString();
        String2.log(
            identifier + ": caught while reading file=" + fileDir + fileName + ": " + t2String);
        if (Thread.currentThread().isInterrupted()
            || t2 instanceof WaitThenTryAgainException
            || t2 instanceof InterruptedException
            || t2 instanceof TimeoutException
            || EDStatic.isClientAbortException(t2)
            || t2 instanceof OutOfMemoryError
            || t2String.indexOf(Math2.memoryTooMuchData) >= 0
            || t2String.indexOf(Math2.TooManyOpenFiles) >= 0) {
          throw t2;
        }

        // sleep and give it one more try
        Math2.sleep(1000);
        if (Thread.currentThread().interrupted()) // consume the interrupted status
        throw new InterruptedException();
        try {
          table =
              eddTableFromFiles.getSourceDataFromFile(
                  fileDir,
                  fileName,
                  sourceDataNames,
                  sourceDataTypes,
                  sortedSpacing,
                  minSorted,
                  maxSorted,
                  sourceConVars,
                  sourceConOps,
                  sourceConValues,
                  false,
                  true); // getMetadata, mustGetData  //???what about global att promoted to var?

        } catch (Throwable t3) {
          String t3String = t3.toString();
          if (debugMode) String2.log(identifier + ": caught while 2nd reading file: " + t3String);
          if (Thread.currentThread().isInterrupted()
              || t3 instanceof WaitThenTryAgainException
              || t3 instanceof InterruptedException
              || EDStatic.isClientAbortException(t3)
              || t3 instanceof OutOfMemoryError
              || t3String.indexOf(Math2.memoryTooMuchData) >= 0
              || t3String.indexOf(Math2.TooManyOpenFiles) >= 0) {
            throw t3;
          }

          if (eddTableFromFiles.filesAreLocal) {
            // mark the file as bad   and reload the dataset
            eddTableFromFiles.addBadFileToTableOnDisk(
                fileDirIndex, fileName, fileLastMod, MustBe.throwableToShortString(t2));
          }
          // an exception here will cause data request to fail (as it should)
          if (debugMode) String2.log(identifier + ": exit#2 WaitThenTryAgain: badFile.");
          throw t2 instanceof WaitThenTryAgainException
              ? t2
              : new WaitThenTryAgainException(t2); // refer to the original exception
        }
      }

      if (Thread.currentThread().interrupted()) // consume the interrupted status
      throw new InterruptedException();
      if (table.nRows() == 0) {
        if (debugMode)
          String2.log(
              identifier
                  + ": exit#3: nRows=0 after read file. time="
                  + (System.currentTimeMillis() - startTime)
                  + "ms");
        return null;
      }

      // prestandardizeResultsTable
      eddTableFromFiles.preStandardizeResultsTable(loggedInAs, table);

      // standardizeResultsTable applies all constraints
      if (table.nRows() > 0)
        eddTableFromFiles.standardizeResultsTable(language, requestUrl, userDapQuery, table);
      if (table.nRows() == 0) {
        if (debugMode)
          String2.log(
              identifier
                  + ": exit#4: nRows=0 after standardize. time="
                  + (System.currentTimeMillis() - startTime)
                  + "ms");
        return null;
      }

      if (debugMode)
        String2.log(
            identifier
                + ": SUCCESS. nRows="
                + table.nRows()
                + " time="
                + (System.currentTimeMillis() - startTime)
                + "ms");
      return table;

    } catch (Exception e) {
      throw e; // allowed
    } catch (Throwable t5) {
      throw new ExecutionException(t5); // wrap it in an Exception, which is allowed
    }
  }
}
