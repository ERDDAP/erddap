/*
 * DasDds Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * This is a command line program to run EDD.testDasDds.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-05
 */
public class DasDds {

    private static void println(String s) {
        String2.setClipboardString(s);
        String2.log(s);
    }

    /** This gets the i'th value from args, or prompts the user. */
    private static String get(String args[], int i, String prompt, String def) throws Throwable {
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
     */
    public static void doIt(String args[], boolean loop) throws Throwable {
        EDStatic.returnLoggingToSystemOut(); //also, this forces instantiation when run TestAll

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
                "This generates the DAS and DDS for a dataset and puts it on the clipboard.\n" +
                "Press ^C to exit at any time.\n\n" +
                "Which datasetID",
                datasetID);
            if (datasetID == null) return;

            //delete the datasetInfo files for this datasetID (in case incorrect info)       
            try {
                String dir = EDD.datasetDir(datasetID);
                println("dataset dir=" + dir + "\n" +
                    "dataset n files not deleted = " +
                    RegexFilenameFilter.regexDelete(dir, ".*", false));

            } catch (Throwable t) {
                println("\n*** An error occurred while trying to load the " + datasetID + " dataset:\n" +
                    MustBe.throwableToString(t));
            }

            try {
                println(EDD.testDasDds(datasetID, verbose));
            } catch (Throwable t) {
                println("\n*** An error occurred while trying to load the " + datasetID + " dataset:\n" +
                    MustBe.throwableToString(t));
            }
        } while (loop && args.length == 0);
    }

    /**
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the question.
     */
    public static void main(String args[]) throws Throwable {
        doIt(args, true);
        System.exit(0);
    }

}