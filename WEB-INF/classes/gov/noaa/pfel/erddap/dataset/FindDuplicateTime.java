/*
 * FindDuplicateTime Copyright 2021, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;

import java.util.ArrayList;
import java.util.HashMap;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Use this to find gridded .nc files with the same time value.
 * This is recursive.
 * Author: Bob Simons  2019-10-15
 */
public class FindDuplicateTime {

    /**
     * Use this to find gridded .nc files with the same time value.
     * This assumes there is just 1 time value per file.
     * This standardizes the times to ISO 8601 times.
     *
     * @param directory This directory and recursive subdirectories will be searched
     * @param fileNameRegex 
     * @param timeVarName  usually "time"
     * @return a string with a description of the duplicates (if any) and summary statistics
     */ 
    public static String findDuplicateTime(String directory, String fileNameRegex,
        String timeVarName) throws Exception {

        directory = File2.addSlash(directory);
        int nErrors = 0;
        int nDup = 0;
        ArrayList<String> fileNames = new ArrayList();
        RegexFilenameFilter.recursiveFullNameList(fileNames, directory, fileNameRegex,
            false); //directoriesToo
        StringBuilder results = new StringBuilder();
        HashMap<String,StringArray> hashMap = new HashMap();

        int n = fileNames.size();
        results.append("*** FindDuplicateTime directory=" + directory + " fileNameRegex=" + fileNameRegex + 
            " timeVarName=" + timeVarName + " nFilesFound=" + n + "\n");           
        for (int i = 0; i < n; i++) {
            String fileName = fileNames.get(i);
            try (NetcdfFile ncf = NcHelper.openFile(fileName)) {
                if ((i < 1000 && i % 100 == 0) || i % 1000 == 0) 
                    String2.log("file #" + i + "=" + fileName);
                
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
                for (int dup = 0; dup < dupNames.size(); dup++) 
                    results.append(dupNames.get(dup) + "\n");
            }
        }               

        results.append("\nFindDuplicateTime finished successfully.  nFiles=" + n + 
            " nTimesWithDuplicates=" + nDup + " nErrors=" + nErrors + "\n");
        return results.toString();
    }

    public static void testBasic() throws Throwable {
        String2.log("\n*** FindDuplicateTime.test()");
        String results = findDuplicateTime(EDStatic.unitTestDataDir + "nc", 
            "GL_.*\\.nc", "TIME") + "\n";
        String expected = 
"*** FindDuplicateTime directory=/erddapTest/nc/ fileNameRegex=GL_.*\\.nc timeVarName=TIME nFilesFound=4\n" +
"\n" +
"error #1=/erddapTest/nc/GL_201111_TS_DB_44761Invalid.nc\n" +
"    java.io.IOException: java.io.EOFException: Reading /erddapTest/nc/GL_201111_TS_DB_44761Invalid.nc at 3720 file length = 3720\n" +
"\n" +
"2 files have time=2011-11-01T00:00:00Z\n" +
"/erddapTest/nc/GL_201111_TS_DB_44761.nc\n" +
"/erddapTest/nc/GL_201111_TS_DB_44761Copy.nc\n" +
"\n" +
"FindDuplicateTime finished successfully.  nFiles=4 nTimesWithDuplicates=1 nErrors=1\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //GenerateDatasetsXml
        results = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "findDuplicateTime",
            EDStatic.unitTestDataDir + "nc", "GL_.*\\.nc", "TIME"}, 
            false); //doIt loop?
        Test.ensureEqual(results, expected, "Unexpected results from GenerateDatasetsXml.doIt.");
    }


    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ FindDuplicateTime.test(" + interactive + ") test=";

        boolean deleteCachedDatasetInfo = true;

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testBasic();
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }

    public static void main(String[] args) throws Exception {

        if (args == null || args.length < 3)
            throw new RuntimeException("Missing command line parameters: directory fileNameRegex timeVarName");
        String directory = args[0];
        String fileNameRegex = args[1];
        String timeVarName = args[2];
        String2.log(findDuplicateTime(args[0], args[1], args[2]));

        System.exit(0);
    }
}
