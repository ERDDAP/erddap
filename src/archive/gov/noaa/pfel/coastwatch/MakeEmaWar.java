/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.ema.TestEmaJsp;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.util.Arrays;
import java.util.ArrayList;


/**
 * This is a command line application which makes a .war file so that
 * EMA can be distributed.
 *
 * @author Bob Simons (CoHortSoftware@gmail.com) 2005-11-28
 */
public class MakeEmaWar  {

    //make sure EMA files are compiled
    TestEmaJsp testEmaJsp = new TestEmaJsp(); 

    /**
     * This runs MakeEmaWar.
     *
     * @param args is ignored
     */
    public static void main(String args[]) throws Exception {

        String2.log("\n*** MakeEmaWar");
        String errorInMethod = String2.ERROR + " while generating Ema JavaDocs ";

        //identify directories
        String classPath = File2.getClassPath(); //with / separator and / at the end
        int po = classPath.indexOf("WEB-INF/classes/");
        Test.ensureTrue(po >= 0, "test po >= 0");
        String baseDir = classPath.substring(0, po);
        String coastWatchDir = baseDir + "WEB-INF/classes/gov/noaa/pfel/coastwatch/";

        //make the javadoc commands
        String commandLine0 = "C:\\programs\\jdk8u292-b10\\bin\\javadoc" +
            " -sourcepath " + classPath + //root directory of the classes
            " -d "; //directory to hold results
        String commandLine2 = " -subpackages com.cohort";   //the packages to be doc'd

        //generate the JavaDocs for the online use file
        String tDir = baseDir + "EMADoc"; //dir to hold results
        SSR.dosShell("del /s /q " + //delete (/s=recursive /q=quiet) previous results
            String2.replaceAll(tDir, "/", "\\"), 20); 
        Test.ensureTrue(!File2.isFile(tDir + "/index.html"), errorInMethod + tDir + "/index.html not deleted.");
        Test.ensureTrue(!File2.isFile(tDir + "/com/cohort/array/DoubleArray.html"), errorInMethod + tDir + "/com/cohort/array/DoubleArray.html not deleted.");
        try {
            String2.log(String2.toNewlineString(SSR.dosShell(
                commandLine0 + tDir + commandLine2, 120).toArray()));  
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + 
                " (expected) while generating EMA JavaDocs:", e));
        }
        Test.ensureTrue(File2.isFile(tDir + "/index.html"), errorInMethod + tDir + "/index.html not found.");
        Test.ensureTrue(File2.isFile(tDir + "/com/cohort/array/DoubleArray.html"), errorInMethod + tDir + "/com/cohort/array/DoubleArray.html not found.");

        //generate the JavaDocs for the .war file
        tDir = baseDir + "WEB-INF/docs/EMADoc"; //dir to hold results
        SSR.dosShell("del /s /q " + //delete (/s=recursive /q=quiet) previous results
            String2.replaceAll(tDir, "/", "\\"), 20); 
        Test.ensureTrue(!File2.isFile(tDir + "/index.html"), errorInMethod + tDir + "/index.html not deleted.");
        Test.ensureTrue(!File2.isFile(tDir + "/com/cohort/array/DoubleArray.html"), errorInMethod + tDir + "/com/cohort/array/DoubleArray.html not deleted.");
        try {
            String2.log(String2.toNewlineString(SSR.dosShell(
                commandLine0 + tDir + commandLine2, 120).toArray()));  
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + 
                " (expected) while generating EMA JavaDocs:", e));
        }
        Test.ensureTrue(File2.isFile(tDir + "/index.html"), errorInMethod + tDir + "/index.html not found.");
        Test.ensureTrue(File2.isFile(tDir + "/com/cohort/array/DoubleArray.html"), errorInMethod + tDir + "/com/cohort/array/DoubleArray.html not found.");

        //accumulate the files for the .war file
        ArrayList dirNames = new ArrayList();
        dirNames.add(baseDir + "TestEmaJsp.jsp");
        dirNames.add(baseDir + "EMA.html");
        String cohortDir = baseDir + "WEB-INF/classes/com/cohort/";
        String2.add(dirNames, RegexFilenameFilter.recursiveFullNameList(cohortDir, ".+", false));
        String2.add(dirNames, RegexFilenameFilter.recursiveFullNameList(baseDir + "WEB-INF/docs/EMADoc", ".+", false));

        //convert to sorted String array
        String dirNameArray[] = String2.toStringArray(dirNames.toArray());
        Arrays.sort(dirNameArray);
        //String2.log(String2.toNewlineString(dirNameArray));

        //make the zip file
        String zipName = File2.webInfParentDirectory() + //with / separator and / at the end
            "EMA.war";
        String2.log("MakeEmaWar is making " + zipName + ".");
        SSR.zip(zipName, dirNameArray, 60, baseDir);
        String2.log("\nMakeEmaWar successfully finished making " + 
            zipName + ".\nnFiles=" + dirNames.size());

    }


}
