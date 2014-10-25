/*
 * DasDds Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.FileWriter;
import java.io.IOException;

/**
 * This is a command line program to run EDD.testDasDds.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-05
 */
public class DasDds {

    static String logFileName = EDStatic.fullLogsDirectory + "DasDds.log";
    static String outFileName = EDStatic.fullLogsDirectory + "DasDds.out";
    FileWriter outFile = null;

    private void printToBoth(String s) throws IOException {
        String2.log(s);
        String2.flushLog();
        outFile.write(s);
        outFile.write('\n');
        outFile.flush();
    }

    /** This gets the i'th value from args, or prompts the user. */
    private String get(String args[], int i, String prompt, String def) throws Throwable {
        if (args.length > i) {
            String2.log(prompt + "? " + args[i]);
            return args[i];
        }
        String s = String2.getStringFromSystemIn(prompt + " (default=\"" + def + "\")? ");
        if (s == null)  //null if ^C
            return s;
        if (s.equals("\"\"")) 
            s = "";
        else if (s.length() == 0) 
            s = def;
        return s;
    }
    
    /**
     * This is used when called from within a program.
     * If args is null or args.length is 0, this loops; otherwise it returns when done.
     *
     * @param args if args has values, they are used to answer the questions.
     * @returns the contents of outFileName (will be "" if trouble)
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
        String2.setupLog(true, false,  //toSystemOut, toSystemErr
            logFileName,
            false, //logToStringBuffer
            true, 20000000);  //append
        String2.log("*** Starting DasDds " + 
            Calendar2.getCurrentISODateTimeStringLocal() + "\n" +        
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());  
        outFile = new FileWriter(outFileName); //default charset

        //delete the old log files (pre 1.48 names)
        File2.delete(EDStatic.fullLogsDirectory + "DasDdsLog.txt");
        File2.delete(EDStatic.fullLogsDirectory + "DasDdsLog.txt.previous");

        String datasetID = "";
        if (args == null) 
            args = new String[0];

        //look for -verbose (and remove it)
        boolean verbose = false;  //actually controls reallyVerbose
        int vi = String2.indexOf(args, "-verbose");
        if (vi >= 0) {
            String2.log("verbose=true");
            verbose = true;
            StringArray sa = new StringArray(args);
            sa.remove(vi);
            args = sa.toArray();
        }

        do {
            //get the EDD type
            //EDD.reallyVerbose = false;  //sometimes while testing
            datasetID = get(args, 0, 
                "\n*** DasDds ***\n" +
                "This generates the DAS and DDS for a dataset and puts it in\n" +
                outFileName + "\n" +
                "Press ^D or ^C to exit at any time.\n\n" +
                "Which datasetID",
                datasetID);
            if (datasetID == null) {
                String2.flushLog();
                outFile.flush();
                outFile.close();
                return String2.readFromFile(outFileName)[1];
            }

            //delete the datasetInfo files for this datasetID (in case incorrect info)       
            try {
                String dir = EDD.datasetDir(datasetID);
                String2.log("dataset dir=" + dir + "\n" +
                    "dataset n files not deleted = " +
                    RegexFilenameFilter.regexDelete(dir, ".*", false));

            } catch (Throwable t) {
                String2.log("\n*** An error occurred while deleting the old info for " + datasetID + ":\n" +
                    MustBe.throwableToString(t));
            }

            try {
                printToBoth(EDD.testDasDds(datasetID, verbose));
            } catch (Throwable t) {
                String2.log(
                    "\n*** An error occurred while trying to load " + datasetID + ":\n" +
                    MustBe.throwableToString(t));
            }
            String2.flushLog();

        } while (loop && args.length == 0);

        outFile.flush();
        outFile.close();
        String ret = String2.readFromFile(outFileName)[1];
        String2.returnLoggingToSystemOut();
        return ret;
    }

    /**
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the question.
     */
    public static void main(String args[]) throws Throwable {
        (new DasDds()).doIt(args, true);
        System.exit(0);
    }

}