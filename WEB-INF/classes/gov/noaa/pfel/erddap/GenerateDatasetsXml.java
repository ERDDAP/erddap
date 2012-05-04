/*
 * GenerateDatasetsXml Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * This is a command line program to run GenerateDatasetsXml for the 
 * various EDD subclasses.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-04
 */
public class GenerateDatasetsXml {

    private static void println(String s) {
        String2.setClipboardString(s);
        String2.log(s);
    }

    /** This gets the i'th value from args, or prompts the user. 
     * @throws RuntimeException("ControlC") if user presses ^C.
     */
    private static String get(String args[], int i, String def, String prompt) throws Throwable {
        if (args.length > i) {
            String2.log(prompt + "? " + args[i]);
            return args[i];
        }
        String s = String2.getStringFromSystemIn(prompt + " (default=\"" + def + "\")\n? ");
        if (s == null)  //null if ^C
            throw new RuntimeException("ControlC");
        if (s.equals("\"\"")) 
            s = "";
        else if (s.length() == 0) 
            s = def;
        return s;
    }

    /**
     * This is used when called from within a program.
     * If args.length is 0, this loops; otherwise it returns when done.
     *
     * @param args if args has values, they are used to answer the questions.
     */
    public static void doIt(String args[], boolean loop) throws Throwable {
        String logFileName = EDStatic.fullLogsDirectory + "log.txt";

        if (args == null) 
            args = new String[0];
        String eddType = "EDDGridFromDap";
        String s1 = "", s2 = "", s3 = "", s4 = "", s5 = "", s6 = "", s7 = "", 
            s8 = "", s9 = "", s10 = "", s11 = "", s12 = "", s13 = "", s14 = "",
            s15 = "", s16 = "";

        //look for -verbose (and remove it)
        boolean reallyVerbose = false;  
        int vi = String2.indexOf(args, "-verbose");
        if (vi >= 0) {
            String2.log("verbose=true");
            reallyVerbose = true;
            StringArray sa = new StringArray(args);
            sa.remove(vi);
            args = sa.toArray();
        }
        EDD.verbose = true;
        EDD.reallyVerbose = reallyVerbose;
        NcHelper.verbose = reallyVerbose;
        OpendapHelper.verbose = reallyVerbose;
        Table.verbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;

        do {
            //get the EDD type
            eddType = get(args, 0, eddType,
                "\n*** GenerateDatasetsXml ***\n" +
                "Results are shown on the screen, put on the clipboard and put in\n" +
                logFileName + "\n" +
                "Press ^C to exit at any time.\n" +
                "Type \"\" to change from a non-nothing default back to nothing.\n" +
                "DISCLAIMER:\n" +
                "  The chunk of datasets.xml made by GenerateDatasetsXml isn't perfect.\n" +
                "  YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
                "  GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
                "  correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
                "  THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
                "For detailed information, see\n" +
                "http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html\n" +
                "\n" +
                "The EDDType options are:\n" +
                "  EDDGridAggregateExistingDimension\n" +
                "  EDDGridFromDap\n" +
                "  EDDGridFromErddap\n" +
                "  EDDGridFromNcFiles\n" +
                "  EDDGridFromThreddsCatalog\n" +
                "  EDDTableFromAsciiFiles\n" +                
                "  EDDTableFromDapSequence\n" +
                "  EDDTableFromDatabase\n" +
                "  EDDTableFromErddap\n" +
                "  EDDTableFromIoosSOS\n" +
                "  EDDTableFromNcFiles\n" +
                "  EDDTableFromOBIS\n" +
                "  EDDTableFromSOS\n" +           
                "  EDDTableFromThreddsFiles\n" +
                "Which EDDType");
            if (eddType == null) return;

            try {

                //EDDGrid
                if (eddType.equals("EDDGridAggregateExistingDimension")) {
                    s1  = get(args,  1 , s1, "Server type (hyrax or thredds)");          
                    s2  = get(args,  2 , s2, "Parent URL (e.g., for hyrax, ending in \"contents.html\"; " +
                                                             " for thredds, ending in \"catalog.xml\")");          
                    s3  = get(args,  3,  s3, "File name regex (e.g., \".*\\.nc\")");
                    s4  = get(args,  4,  s4, "ReloadEveryNMinutes (e.g., 10080)");
                    System.out.println("working...");
                    println(EDDGridAggregateExistingDimension.generateDatasetsXml(
                        s1, s2, s3, true, String2.parseInt(s4, 10080)));

                } else if (eddType.equals("EDDGridFromDap")) {
                    s1  = get(args,  1,  s1, "URL (without trailing .dds or .html)");         
                    s2  = get(args,  2,  s2, "ReloadEveryNMinutes (e.g., 10080)");
                    System.out.println("working...");
                    println(EDDGridFromDap.generateDatasetsXml(true, //writeInstructions
                        s1, null, null, null, String2.parseInt(s2, 10080), null));

                } else if (eddType.equals("EDDGridFromErddap")) {
                    s1  = get(args,  1,  s1, "URL of remote ERDDAP (ending in (\"/erddap\")");  
                    System.out.println("working...");
                    println(EDDGridFromErddap.generateDatasetsXml(s1));

                } else if (eddType.equals("EDDGridFromNcFiles")) {
                    s1  = get(args,  1,  s1, "Parent directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, "Full file name of one file");                  
                    s4  = get(args,  4,  s4, "ReloadEveryNMinutes (e.g., 10080)");
                    System.out.println("working...");
                    println(EDDGridFromNcFiles.generateDatasetsXml(s1, 
                        s2.length() == 0? ".*\\.nc" : s2, 
                        s3, String2.parseInt(s4, 10080), null));

                } else if (eddType.equals("EDDGridFromThreddsCatalog")) {
                    s1  = get(args,  1,  s1, "URL (usually ending in /catalog.xml)");         
                    s2  = get(args,  2,  s2, "Dataset name regex (e.g., \".*\")");  //for now, always .*
                    s3  = get(args,  3,  s3, "ReloadEveryNMinutes (e.g., 10080)");
                    System.out.println("working...");
                    String tempDir = SSR.getTempDirectory();
                    File2.makeDirectory(tempDir);
                    String resultsFileName = tempDir + "EDDGridFromThreddsCatalog.xml";
                    EDDGridFromDap.generateDatasetsXmlFromThreddsCatalog(
                        resultsFileName, s1, s2, String2.parseInt(s3, 10080));
                    println(String2.readFromFile(resultsFileName)[1]);

                //EDDTable
                } else if (eddType.equals("EDDTableFromAsciiFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.asc\")");
                    s3  = get(args,  3,  s3, "A sample full file name");                       
                    s4  = get(args,  4,  s4, "Column names row (e.g., 1)");                     
                    s5  = get(args,  5,  s5, "First data row (e.g., 2)");                          
                    s6  = get(args,  6,  s6, "ReloadEveryNMinutes (e.g., 10080)");
                    s7  = get(args,  7,  s7, "PreExtractRegex");
                    s8  = get(args,  8,  s8, "PostExtractRegex");
                    s9  = get(args,  9,  s9, "ExtractRegex");
                    s10 = get(args, 10, s10, "Column name for extract");
                    s11 = get(args, 11, s11, "Sorted column source name");
                    s12 = get(args, 12, s12, "Sort files by sourceName");
                    s13 = get(args, 13, s13, "infoUrl");
                    s14 = get(args, 14, s14, "institution");
                    s15 = get(args, 15, s15, "summary");
                    s16 = get(args, 16, s16, "title");
                    System.out.println("working...");
                    println(EDDTableFromAsciiFiles.generateDatasetsXml(
                        s1, s2, s3, String2.parseInt(s4, 1), String2.parseInt(s5, 2), 
                        String2.parseInt(s6, 10080), s7, s8, s9, s10,
                        s11, s12, s13, s14, s15, s16, null));

                //currently no EDDTableFromBMDE  //it is inactive

                } else if (eddType.equals("EDDTableFromDapSequence")) {
                    s1  = get(args,  1,  s1, "URL (without trailing .dds or .html)"); 
                    s2  = get(args,  2,  s2, "ReloadEveryNMinutes (e.g., 10080)");
                    System.out.println("working...");
                    println(EDDTableFromDapSequence.generateDatasetsXml(
                        s1, String2.parseInt(s2, 10080), null));

                } else if (eddType.equals("EDDTableFromDatabase")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "Driver name");
                    s3  = get(args,  3,  s3, "Connection properties (format: name1|value1|name2|value2)");
                    s4  = get(args,  4,  s4, "Catalog name"); 
                    s5  = get(args,  5,  s5, "Schema name");
                    s6  = get(args,  6,  s6, "Table name");
                    s7  = get(args,  7,  s7, "OrderBy (CSV list of sourceNames)");
                    s8  = get(args,  8,  s8, "ReloadEveryNMinutes (e.g., 10080)");
                    s9  = get(args,  9,  s9, "infoUrl");
                    s10 = get(args, 10, s10, "institution");
                    s11 = get(args, 11, s11, "summary");
                    s12 = get(args, 12, s12, "title");
                    String sa3[] = s3.length() == 0? new String[0] : String2.split(s3, '|');
                    System.out.println("working...");
                    println(EDDTableFromDatabase.generateDatasetsXml(
                        s1, s2, sa3, s4, s5, s6, s7, String2.parseInt(s8, 10080),
                        s9, s10, s11, s12, null));

                } else if (eddType.equals("EDDTableFromErddap")) {
                    s1 = get(args, 1, s1, "URL of remote ERDDAP (ending in (\"/erddap\")");
                    System.out.println("working...");
                    println(EDDTableFromErddap.generateDatasetsXml(s1));

                //INACTIVE: "EDDTableFromHyraxFiles"

                } else if (eddType.equals("EDDTableFromNcFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, "A sample full file name");                       
                    s4  = get(args,  4,  s4, "DimensionsCSV (or \"\" for default)");                       
                    s5  = get(args,  5,  s5, "ReloadEveryNMinutes (e.g., 10080)");
                    s6  = get(args,  6,  s6, "PreExtractRegex");
                    s7  = get(args,  7,  s7, "PostExtractRegex");
                    s8  = get(args,  8,  s8, "ExtractRegex");
                    s9  = get(args,  9,  s9, "Column name for extract");
                    s10 = get(args, 10, s10, "Sorted column source name");
                    s11 = get(args, 11, s11, "Sort files by sourceName");
                    s12 = get(args, 12, s12, "infoUrl");
                    s13 = get(args, 13, s13, "institution");
                    s14 = get(args, 14, s14, "summary");
                    s15 = get(args, 15, s15, "title");
                    System.out.println("working...");
                    println(EDDTableFromNcFiles.generateDatasetsXml(
                        s1, s2, s3, s4, String2.parseInt(s5, 10080), s6, s7, s8,
                        s9, s10, s11, s12, s13, s14, s15, null));

                } else if (eddType.equals("EDDTableFromOBIS")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "Source Code");
                    s3  = get(args,  3,  s3, "ReloadEveryNMinutes (e.g., 10080)");
                    s4  = get(args,  4,  s4, "CreatorEmail");
                    System.out.println("working...");
                    println(EDDTableFromOBIS.generateDatasetsXml(s1, s2,
                        String2.parseInt(s3, 10080), s4, null));

                } else if (eddType.equals("EDDTableFromIoosSOS")) {
                    s1  = get(args,  1,  s1, "URL");
                    System.out.println("working...");
                    println(EDDTableFromSOS.generateDatasetsXmlFromIOOS(s1));

                } else if (eddType.equals("EDDTableFromSOS")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "SOS version (or \"\" for default)");
                    System.out.println("working...");
                    println(EDDTableFromSOS.generateDatasetsXml(s1, s2));

                } else if (eddType.equals("EDDTableFromThreddsFiles")) {
                    s1  = get(args,  1,  s1, "Starting catalog.xml URL");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, "A sample file URL");                       
                    s4  = get(args,  4,  s4, "ReloadEveryNMinutes (e.g., 10080)");
                    s5  = get(args,  5,  s5, "PreExtractRegex");
                    s6  = get(args,  6,  s6, "PostExtractRegex");
                    s7  = get(args,  7,  s7, "ExtractRegex");
                    s8  = get(args,  8,  s8, "Column name for extract");
                    s9  = get(args,  9,  s9, "Sorted column source name");
                    s10 = get(args, 10, s10, "Sort files by sourceName");
                    System.out.println("working...");
                    println(EDDTableFromThreddsFiles.generateDatasetsXml(
                        s1, s2, s3, String2.parseInt(s4, 10080), s5, s6, s7, s8,
                        s9, s10, null));

                } else {
                    println("ERROR: eddType=" + eddType + " is not an option.");
                }
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t);
                if (msg.indexOf("ControlC") >= 0)
                    return;
                println("\n*** An error occurred while trying to generate the information for datasets.xml:\n" +
                    msg);
            }
        } while (loop && args.length == 0);
    }

    /**
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the questions.
     */
    public static void main(String args[]) throws Throwable {
        doIt(args, true);
        System.exit(0);
    }

}