/*
 * FindDuplicateTime Copyright 2021, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Use this to find gridded .nc files with the same time value. This is recursive. Author: Bob
 * Simons 2019-10-15
 */
public class FindDuplicateTime {

  /**
   * Use this to find gridded .nc files with the same time value. This assumes there is just 1 time
   * value per file. This standardizes the times to ISO 8601 times.
   *
   * @param directory This directory and recursive subdirectories will be searched
   * @param fileNameRegex
   * @param timeVarName usually "time"
   * @return a string with a description of the duplicates (if any) and summary statistics
   */
  public static String findDuplicateTime(String directory, String fileNameRegex, String timeVarName)
      throws Exception {

    directory = File2.addSlash(directory);
    int nErrors = 0;
    int nDup = 0;
    ArrayList<String> fileNames = new ArrayList();
    RegexFilenameFilter.recursiveFullNameList(
        fileNames, directory, fileNameRegex, false); // directoriesToo
    StringBuilder results = new StringBuilder();
    HashMap<String, StringArray> hashMap = new HashMap();

    int n = fileNames.size();
    results.append(
        "*** FindDuplicateTime directory="
            + directory
            + " fileNameRegex="
            + fileNameRegex
            + " timeVarName="
            + timeVarName
            + " nFilesFound="
            + n
            + "\n");
    for (int i = 0; i < n; i++) {
      String fileName = fileNames.get(i);
      NetcdfFile ncf = null;
      try {
        ncf =
            NcHelper.openFile(fileName); // needs to be inside try/catch, so loop continues if error
        if ((i < 1000 && i % 100 == 0) || i % 1000 == 0) String2.log("file #" + i + "=" + fileName);

        Variable var = ncf.findVariable(timeVarName);
        double rawTime = NcHelper.getPrimitiveArray(var).getDouble(0);
        String units = NcHelper.getVariableAttribute(var, "units").getString(0);
        double bf[] = Calendar2.getTimeBaseAndFactor(units);
        double epSec = Calendar2.unitsSinceToEpochSeconds(bf[0], bf[1], rawTime);
        String iso = Calendar2.epochSecondsToIsoStringTZ(epSec);

        StringArray dupNames = hashMap.get(iso);
        if (dupNames == null) {
          dupNames = new StringArray();
          dupNames.add(fileName);
          hashMap.put(iso, dupNames);
        } else {
          dupNames.add(fileName);
        }

      } catch (Throwable t) {
        nErrors++;
        results.append("\nerror #" + nErrors + "=" + fileName + "\n    " + t.toString() + "\n");
      } finally {
        try {
          if (ncf != null) ncf.close();
        } catch (Exception e9) {
        }
      }
    }

    StringArray keys = new StringArray();
    keys.addSet(hashMap.keySet());
    keys.sort();
    for (int i = 0; i < keys.size(); i++) {
      StringArray dupNames = hashMap.get(keys.get(i));
      if (dupNames.size() > 1) {
        nDup++;
        results.append("\n" + dupNames.size() + " files have time=" + keys.get(i) + "\n");
        dupNames.sortIgnoreCase();
        for (int dup = 0; dup < dupNames.size(); dup++) results.append(dupNames.get(dup) + "\n");
      }
    }

    results.append(
        "\nFindDuplicateTime finished successfully.  nFiles="
            + n
            + " nTimesWithDuplicates="
            + nDup
            + " nErrors="
            + nErrors
            + "\n");
    return results.toString();
  }

  public static void main(String[] args) throws Exception {

    if (args == null || args.length < 3)
      throw new RuntimeException(
          "Missing command line parameters: directory fileNameRegex timeVarName");
    String directory = args[0];
    String fileNameRegex = args[1];
    String timeVarName = args[2];
    String2.log(findDuplicateTime(args[0], args[1], args[2]));

    System.exit(0);
  }
}
