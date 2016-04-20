/*
 * GenerateDatasetsXml Copyright 2009, NOAA.
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

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.GregorianCalendar;

/**
 * This is a command line program to run GenerateDatasetsXml for the 
 * various EDD subclasses.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-04
 */
public class GenerateDatasetsXml {

    OutputStreamWriter outFile = null;
    static String logFileName = EDStatic.fullLogsDirectory + "GenerateDatasetsXml.log";
    static String outFileName = EDStatic.fullLogsDirectory + "GenerateDatasetsXml.out";

    private void printToBoth(String s) throws IOException {
        String2.log(s);
        String2.flushLog();
        outFile.write(s);
        outFile.write('\n');
        outFile.flush();
    }

    /** This gets the i'th value from args, or prompts the user. 
     * @throws RuntimeException("ControlC") if user presses ^C.
     */
    private String get(String args[], int i, String def, String prompt) throws Throwable {
        String s; 
        if (args.length > i) {
            String2.log(prompt + "? " + (s = args[i]));
        } else {
            s = String2.getStringFromSystemIn(prompt + " (default=\"" + def + "\")\n? ");
            if (s == null)  //null if ^C
                throw new RuntimeException("ControlC");
        }
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            s = String2.fromJson(s); 
        if (s.length() == 0 || s.equals("default") || s.equals("\"default\"")) 
            s = def;
        else if (s.equals("nothing") || s.equals("\"nothing\"")) //else is important
            s = "";
        return s;
    }

    /**
     * This is used when called from within a program.
     * If args.length is 0, this loops; otherwise it returns when done.
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
            false, String2.logFileDefaultMaxSize);  //append
        GregorianCalendar gcLocal = Calendar2.newGCalendarLocal();
        String localIsoTime     = Calendar2.formatAsISODateTimeT(gcLocal);
        String localCompactTime = Calendar2.formatAsCompactDateTime(gcLocal);
        String2.log("*** Starting GenerateDatasetsXml " + localIsoTime + "\n" + 
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());
        outFile = new OutputStreamWriter(
            new FileOutputStream(outFileName), "ISO-8859-1"); //charset to match datasets.xml

        //delete the old-system log files (pre 1.48 names)
        File2.delete(EDStatic.fullLogsDirectory + "GenerateDatasetsXmlLog.txt");
        File2.delete(EDStatic.fullLogsDirectory + "GenerateDatasetsXmlLog.txt.previous");

        if (args == null) 
            args = new String[0];
        String eddType = "EDDGridFromDap";
        String s1 = "", s2 = "", s3 = "", s4 = "", s5 = "", s6 = "", s7 = "", 
            s8 = "", s9 = "", s10 = "", s11 = "", s12 = "", s13 = "", s14 = "",
            s15 = "", s16 = "", s17 = "";
        String reloadEveryNMinutesMessage = "ReloadEveryNMinutes (e.g., " + 
            EDD.DEFAULT_RELOAD_EVERY_N_MINUTES + ")";
        String sampleFileNamePrompt = "Full file name of one file (or leave empty to use first matching fileName)";                  
        String sampleFileUrlPrompt  = "Full URL of one file (or leave empty to use first matching fileName)";                  

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

        //look for -I (testmode) or -i
        int ii = String2.lineStartsWith(args, "-I");
        if (ii < 0)
            ii = String2.lineStartsWith(args, "-i");
        String insert = null;
        if (ii >= 0) {
            insert = args[ii];
            String2.log(insert);
            StringArray sa = new StringArray(args);
            sa.remove(ii);
            args = sa.toArray();
        }

        EDD.verbose = true;
        EDD.reallyVerbose = reallyVerbose;
        NcHelper.verbose = reallyVerbose;
        OpendapHelper.verbose = reallyVerbose;
        Table.verbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;
        String eddTypes[] = {
            "EDDGridAggregateExistingDimension",
            "EDDGridFromDap",
            "EDDGridFromEDDTable",
            "EDDGridFromErddap",
            "EDDGridFromMergeIRFiles",
            "EDDGridFromNcFiles",
            "EDDGridFromNcFilesUnpacked",
            "EDDGridFromThreddsCatalog",
            "EDDGridLonPM180FromErddapCatalog",
            "EDDTableFromAsciiFiles",
            "EDDTableFromAwsXmlFiles",
            "EDDTableFromCassandra",
            "EDDTableFromColumnarAsciiFiles",
            "EDDTableFromDapSequence",
            "EDDTableFromDatabase",
            "EDDTableFromErddap",
            "EDDTableFromFileNames",
            "EDDTableFromIoosSOS",
            "EDDTableFromNcFiles",
            "EDDTableFromNcCFFiles",
            "EDDTableFromOBIS",
            "EDDTableFromSOS",
            "EDDTableFromThreddsFiles",
            "EDDTableFromWFSFiles",
            "EDDsFromFiles"};
        StringBuilder sb = new StringBuilder();
        int net = eddTypes.length;
        int net2 = Math2.hiDiv(net, 2);
        for (int i = 0; i < net2; i++)
            sb.append(String2.left(eddTypes[i], 36) +
                (net2 + i >= net? "" : eddTypes[net2 + i]) + "\n");
        String eddTypesString = sb.toString();
        sb = null;

        do {
            try {
                //get the EDD type
                eddType = get(args, 0, eddType,
                    "\n*** GenerateDatasetsXml ***\n" +
                    "Press Enter or type the word \"default\" (but without the quotes)\n" +
                    "  to get the default value.\n" +
                    "Type the word \"nothing\" (but without quotes) or \"\" (2 double quotes)\n" +
                    "  to change from a non-nothing default back to nothing (a 0-length string).\n" +
                    "Press ^D or ^C to exit this program at any time.\n" +
                    "Or, you can put all the answers as parameters on the command line.\n" +
                    "Results are shown on the screen and put in\n" +
                    outFileName + "\n" +
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
                    eddTypesString + "\n" +
                    "Which EDDType");

                //EDDGrid
                if (eddType.equals("EDDGridAggregateExistingDimension")) {
                    s1  = get(args,  1 , s1, "Server type (hyrax, thredds, or WAF)");          
                    s2  = get(args,  2 , s2, "Parent URL (e.g., for hyrax, ending in \"contents.html\"; " +
                        " for thredds, ending in \"catalog.xml\"; for WAF, ending in '/')");          
                    s3  = get(args,  3,  s3, "File name regex (e.g., \".*\\.nc\")");
                    s4  = get(args,  4,  s4, reloadEveryNMinutesMessage);
                    String2.log("working...");
                    printToBoth(EDDGridAggregateExistingDimension.generateDatasetsXml(
                        s1, s2, s3, 
                        String2.parseInt(s4, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES)));

                } else if (eddType.equals("EDDGridFromDap")) {
                    s1  = get(args,  1,  s1, "URL (without trailing .dds or .html)");         
                    s2  = get(args,  2,  s2, "ReloadEveryNMinutes (e.g., " + 
                        EDD.DEFAULT_RELOAD_EVERY_N_MINUTES + ", recommended: -1 generates suggested values)");
                    String2.log("working...");
                    printToBoth(EDDGridFromDap.generateDatasetsXml(true, //writeInstructions
                        s1, null, null, null, 
                        String2.parseInt(s2, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), null));

                } else if (eddType.equals("EDDGridFromEDDTable")) {
                    s1  = get(args,  1,  s1, "datasetID of underlying EDDTable");         
                    s2  = get(args,  2,  s2, "ReloadEveryNMinutes (e.g., " + 
                        EDD.DEFAULT_RELOAD_EVERY_N_MINUTES);
                    String2.log("working...");
                    printToBoth(EDDGridFromEDDTable.generateDatasetsXml(s1, 
                        String2.parseInt(s2, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), null));

                } else if (eddType.equals("EDDGridFromErddap")) {
                    s1 = get(args, 1, s1, "URL of remote ERDDAP (ending in (\"/erddap\")");  
                    s2 = get(args, 2, s2, "Keep original datasetIDs (true|false)");
                    String2.log("working...");
                    printToBoth(EDDGridFromErddap.generateDatasetsXml(s1, String2.parseBoolean(s2)));

                } else if (eddType.equals("EDDGridFromMergeIRFiles")) {
                    s1  = get(args,  1,  s1, "Parent directory");
                    s2  = get(args,  2,  "merg_[0-9]{10}_4km-pixel\\.gz", 
                        "File name regex (merg_[0-9]{10}_4km-pixel\\.gz)");              
                    s3  = get(args,  3,  Integer.toString(EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        reloadEveryNMinutesMessage);
                    String2.log("working...");
                    printToBoth(EDDGridFromMergeIRFiles.generateDatasetsXml(s1, s2,
                        String2.parseInt(s3, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES)));

                } else if (eddType.equals("EDDGridFromNcFiles")) {
                    s1  = get(args,  1,  s1, "Parent directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                  
                    s4  = get(args,  4,  s4, reloadEveryNMinutesMessage);
                    String2.log("working...");
                    printToBoth(EDDGridFromNcFiles.generateDatasetsXml(s1, 
                        s2.length() == 0? ".*\\.nc" : s2, 
                        s3, String2.parseInt(s4, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), null));

                } else if (eddType.equals("EDDGridFromNcFilesUnpacked")) {
                    s1  = get(args,  1,  s1, "Parent directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                  
                    s4  = get(args,  4,  s4, reloadEveryNMinutesMessage);
                    String2.log("working...");
                    printToBoth(EDDGridFromNcFilesUnpacked.generateDatasetsXml(s1, 
                        s2.length() == 0? ".*\\.nc" : s2, 
                        s3, String2.parseInt(s4, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), null));

                } else if (eddType.equals("EDDGridFromThreddsCatalog")) {
                    s1  = get(args,  1,  s1, "URL (usually ending in \"/catalog.xml\")");         
                    s2  = get(args,  2,  s2, "Dataset name regex (e.g., \".*\")");  //for now, always .*
                    s3  = get(args,  3,  s3, "ReloadEveryNMinutes (e.g., " + 
                        EDD.DEFAULT_RELOAD_EVERY_N_MINUTES + ", recommended: -1 generates suggested values)");
                    String2.log("working...");
                    String tempDir = SSR.getTempDirectory();
                    File2.makeDirectory(tempDir);
                    String resultsFileName = tempDir + "EDDGridFromThreddsCatalog.xml";
                    EDDGridFromDap.generateDatasetsXmlFromThreddsCatalog(
                        resultsFileName, s1, s2, 
                        String2.parseInt(s3, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES));
                    printToBoth(String2.readFromFile(resultsFileName)[1]);

                } else if (eddType.equals("EDDGridLonPM180FromErddapCatalog")) {
                    s1  = get(args,  1,  s1, "URL (ending in \"/erddap/\")");         
                    s2  = get(args,  2,  s2, "Dataset name regex (usually \".*\")");  
                    String2.log("working...");
                    printToBoth(EDDGridLonPM180.generateDatasetsXmlFromErddapCatalog(
                        s1, s2));

                //EDDTable
                } else if (eddType.equals("EDDTableFromAsciiFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.asc\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                       
                    s4  = get(args,  4,  s4, "Charset (e.g., ISO-8859-1 (default) or UTF-8)");                     
                    s5  = get(args,  5,  s5, "Column names row (e.g., 1)");                     
                    s6  = get(args,  6,  s6, "First data row (e.g., 2)");                          
                    s7  = get(args,  7,  s7, reloadEveryNMinutesMessage);
                    s8  = get(args,  8,  s8, "PreExtractRegex");
                    s9  = get(args,  9,  s9, "PostExtractRegex");
                    s10 = get(args, 10, s10, "ExtractRegex");
                    s11 = get(args, 11, s11, "Column name for extract");
                    s12 = get(args, 12, s12, "Sorted column source name");
                    s13 = get(args, 13, s13, "Sort files by sourceName");
                    s14 = get(args, 14, s14, "infoUrl");
                    s15 = get(args, 15, s15, "institution");
                    s16 = get(args, 16, s16, "summary");
                    s17 = get(args, 17, s17, "title");
                    String2.log("working...");
                    printToBoth(EDDTableFromAsciiFiles.generateDatasetsXml(
                        s1, s2, s3, s4, String2.parseInt(s5, 1), String2.parseInt(s6, 2), 
                        String2.parseInt(s7, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, null));

                } else if (eddType.equals("EDDTableFromAwsXmlFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.xml\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                       
                    s4  = "1"; //get(args,  4,  s4, "Column names row (e.g., 1)");                     
                    s5  = "2"; //get(args,  5,  s5, "First data row (e.g., 2)");                          
                    s6  = get(args,  6,  s6, reloadEveryNMinutesMessage);
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
                    String2.log("working...");
                    printToBoth(EDDTableFromAwsXmlFiles.generateDatasetsXml(
                        s1, s2, s3, String2.parseInt(s4, 1), String2.parseInt(s5, 2), 
                        String2.parseInt(s6, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, null));

                //currently no EDDTableFromBMDE  //it is inactive

                } else if (eddType.equals("EDDTableFromCassandra")) {
                    s1 = get(args,  1,  s1, "URL (without port number, e.g., localhost or 127.0.0.1)");
                    s2 = get(args,  2,  s2, "Connection properties (format: name1|value1|name2|value2)");
                    s3 = get(args,  3,  s3, "Keyspace (or '!!!LIST!!!')");
                    s4 = get(args,  4,  s4, "Table name (or '!!!LIST!!!')");
                    s5 = get(args,  5,  s5, reloadEveryNMinutesMessage);
                    s6 = get(args,  6,  s6, "infoUrl");
                    s7 = get(args,  7,  s7, "institution");
                    s8 = get(args,  8,  s8, "summary");
                    s9 = get(args,  9,  s9, "title");
                    String sa2[] = s2.length() == 0? new String[0] : String2.split(s2, '|');
                    String2.log("working...");
                    printToBoth(EDDTableFromCassandra.generateDatasetsXml(
                        s1, sa2, s3, s4, String2.parseInt(s5), s6, s7, s8, s9, null));

                } else if (eddType.equals("EDDTableFromColumnarAsciiFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.asc\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                       
                    s4  = get(args,  4,  s4, "Charset (e.g., ISO-8859-1 (default) or UTF-8)");                     
                    s5  = get(args,  5,  s5, "Column names row (e.g., 1)");                     
                    s6  = get(args,  6,  s6, "First data row (e.g., 2)");                          
                    s7  = get(args,  7,  s7, reloadEveryNMinutesMessage);
                    s8  = get(args,  8,  s8, "PreExtractRegex");
                    s9  = get(args,  9,  s9, "PostExtractRegex");
                    s10 = get(args, 10, s10, "ExtractRegex");
                    s11 = get(args, 11, s11, "Column name for extract");
                    //s12 = get(args, 12, s12, "Sorted column source name");
                    s12 = get(args, 12, s12, "Sort files by sourceName");
                    s13 = get(args, 13, s13, "infoUrl");
                    s14 = get(args, 14, s14, "institution");
                    s15 = get(args, 15, s15, "summary");
                    s16 = get(args, 16, s16, "title");
                    String2.log("working...");
                    printToBoth(EDDTableFromColumnarAsciiFiles.generateDatasetsXml(
                        s1, s2, s3, s4, String2.parseInt(s5, 1), String2.parseInt(s6, 2), 
                        String2.parseInt(s7, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s8, s9, s10, s11, s12, s13, s14, s15, s16, null));

                } else if (eddType.equals("EDDTableFromDapSequence")) {
                    s1  = get(args,  1,  s1, "URL (without trailing .dds or .html)"); 
                    s2  = get(args,  2,  s2, reloadEveryNMinutesMessage);
                    String2.log("working...");
                    printToBoth(EDDTableFromDapSequence.generateDatasetsXml(
                        s1, String2.parseInt(s2, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), null));

                } else if (eddType.equals("EDDTableFromDatabase")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "Driver name");
                    s3  = get(args,  3,  s3, "Connection properties (format: name1|value1|name2|value2)");
                    s4  = get(args,  4,  s4, "Catalog name"); 
                    s5  = get(args,  5,  s5, "Schema name");
                    s6  = get(args,  6,  s6, "Table name");
                    s7  = get(args,  7,  s7, "OrderBy (CSV list of sourceNames)");
                    s8  = get(args,  8,  s8, reloadEveryNMinutesMessage);
                    s9  = get(args,  9,  s9, "infoUrl");
                    s10 = get(args, 10, s10, "institution");
                    s11 = get(args, 11, s11, "summary");
                    s12 = get(args, 12, s12, "title");
                    String sa3[] = s3.length() == 0? new String[0] : String2.split(s3, '|');
                    String2.log("working...");
                    printToBoth(EDDTableFromDatabase.generateDatasetsXml(
                        s1, s2, sa3, s4, s5, s6, s7, 
                        String2.parseInt(s8, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES),
                        s9, s10, s11, s12, null));

                } else if (eddType.equals("EDDTableFromErddap")) {
                    s1 = get(args, 1, s1, "URL of remote ERDDAP (ending in (\"/erddap\")");
                    s2 = get(args, 2, s2, "Keep original datasetIDs (true|false)");
                    String2.log("working...");
                    printToBoth(EDDTableFromErddap.generateDatasetsXml(s1, String2.parseBoolean(s2)));

                } else if (eddType.equals("EDDTableFromFileNames")) {
                    s1 = get(args,  1,  s1, "Starting directory");
                    s2 = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3 = get(args,  3,  s3, "Recursive (true|false)");                       
                    s4 = get(args,  4,  s4, reloadEveryNMinutesMessage);
                    s5 = get(args,  5,  s5, "infoUrl");
                    s6 = get(args,  6,  s6, "institution");
                    s7 = get(args,  7,  s7, "summary");
                    s8 = get(args,  8,  s8, "title");
                    String2.log("working...");
                    printToBoth(EDDTableFromFileNames.generateDatasetsXml(
                        s1, s2, String2.parseBoolean(s3), 
                        String2.parseInt(s4, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s5, s6, s7, s8, null));


                //INACTIVE: "EDDTableFromHyraxFiles"

                } else if (eddType.equals("EDDTableFromNcFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                       
                    s4  = get(args,  4,  s4, "DimensionsCSV (or \"\" for default)");                       
                    s5  = get(args,  5,  s5, reloadEveryNMinutesMessage);
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
                    String2.log("working...");
                    printToBoth(EDDTableFromNcFiles.generateDatasetsXml(
                        s1, s2, s3, s4, String2.parseInt(s5, 
                        EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, null));

                } else if (eddType.equals("EDDTableFromNcCFFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, sampleFileNamePrompt);                       
                    s4  = get(args,  4,  s4, reloadEveryNMinutesMessage);
                    s5  = get(args,  5,  s5, "PreExtractRegex");
                    s6  = get(args,  6,  s6, "PostExtractRegex");
                    s7  = get(args,  7,  s7, "ExtractRegex");
                    s8  = get(args,  8,  s8, "Column name for extract");
                    s9  = get(args,  9,  s9, "Sort files by sourceName");
                    s10 = get(args, 10, s10, "infoUrl");
                    s11 = get(args, 11, s11, "institution");
                    s12 = get(args, 12, s12, "summary");
                    s13 = get(args, 13, s13, "title");
                    String2.log("working...");
                    printToBoth(EDDTableFromNcCFFiles.generateDatasetsXml(
                        s1, s2, s3, String2.parseInt(s4, 
                        EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s5, s6, s7, s8, s9, s10, s11, s12, s13, null));

                } else if (eddType.equals("EDDTableFromOBIS")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "Source Code");
                    s3  = get(args,  3,  s3, reloadEveryNMinutesMessage);
                    s4  = get(args,  4,  s4, "CreatorEmail");
                    String2.log("working...");
                    printToBoth(EDDTableFromOBIS.generateDatasetsXml(s1, s2,
                        String2.parseInt(s3, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), s4, null));

                } else if (eddType.equals("EDDTableFromIoosSOS")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "SOS version (e.g., 1.0.0)");
                    s3  = get(args,  3,  s3, "SOS server type (52N, IOOS_NDBC, IOOS_NOS)");
                    String2.log("working...");
                    printToBoth(EDDTableFromSOS.generateDatasetsXmlFromIOOS(
                        false, s1, s2, s3)); //use cached getCapabilities                        

                } else if (eddType.equals("EDDTableFromSOS")) {
                    s1  = get(args,  1,  s1, "URL");
                    s2  = get(args,  2,  s2, "SOS version (e.g., 1.0.0)");
                    s3  = get(args,  3,  s3, "SOS server type (IOOS_52N, IOOS_NDBC, IOOS_NOS, OOSTethys, or WHOI)");
                    String2.log("working...");
                    printToBoth(EDDTableFromSOS.generateDatasetsXml(
                        false, s1, s2, s3)); //use cached getCapabilities

                } else if (eddType.equals("EDDTableFromThreddsFiles")) {
                    s1  = get(args,  1,  s1, "Starting catalog.xml URL");
                    s2  = get(args,  2,  s2, "File name regex (e.g., \".*\\.nc\")");
                    s3  = get(args,  3,  s3, sampleFileUrlPrompt);                       
                    s4  = get(args,  4,  s4, reloadEveryNMinutesMessage);
                    s5  = get(args,  5,  s5, "PreExtractRegex");
                    s6  = get(args,  6,  s6, "PostExtractRegex");
                    s7  = get(args,  7,  s7, "ExtractRegex");
                    s8  = get(args,  8,  s8, "Column name for extract");
                    s9  = get(args,  9,  s9, "Sorted column source name");
                    s10 = get(args, 10, s10, "Sort files by sourceName");
                    String2.log("working...");
                    printToBoth(EDDTableFromThreddsFiles.generateDatasetsXml(
                        s1, s2, s3, 
                        String2.parseInt(s4, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s5, s6, s7, s8, s9, s10, null));

                } else if (eddType.equals("EDDTableFromWFSFiles")) {
                    s1  = get(args,  1,  s1, "Percent-encoded sourceUrl");
                    s2  = get(args,  2,  s2, "rowElementXPath (space=default)");
                    s3  = get(args,  3,  s3, reloadEveryNMinutesMessage);
                    s4  = get(args,  4,  s4, "infoUrl");
                    s5  = get(args,  5,  s5, "institution");
                    s6  = get(args,  6,  s6, "summary");
                    s7  = get(args,  7,  s7, "title");
                    String2.log("working...");
                    s2 = s2.trim(); //space becomes ""
                    printToBoth(EDDTableFromWFSFiles.generateDatasetsXml(
                        s1, s2, 
                        String2.parseInt(s3, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES), 
                        s4, s5, s6, s7, null));

                } else if (eddType.equals("EDDsFromFiles")) {
                    s1  = get(args,  1,  s1, "Starting directory");
                    String2.log("working...");
                    printToBoth(EDD.generateDatasetsXmlFromFiles(s1));

                } else {
                    String2.log("ERROR: eddType=" + eddType + " is not an option.");
                }
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t);
                if (msg.indexOf("ControlC") >= 0) {
                    String2.flushLog();
                    outFile.flush();
                    outFile.close();
                    return String2.readFromFile(outFileName)[1];
                }
                String2.log(msg);
            }
            String2.flushLog();

        } while (loop && args.length == 0);

        outFile.close();
        String ret = String2.readFromFile(outFileName)[1]; 

        //insert switch:  -idatasetsXmlName#tagName  
        //(or -I for testmode: no overwrite datasets.xml)
        //This looks for lines in datasetsXmlName that have
        //<!-- Begin GenerateDatasetsXml #tagName 
        //<!-- End GenerateDatasetsXml #tagName 
        //and replaces everything in between those lines with the new content.
        //The default datasetsXmlName is this installation's [tomcat]/content/erddap/datasets.xml .
        //If Begin or End lines are not found, then those lines and the new content
        //  are inserted right before </erddapDatasets>.
        //Don't run this in two processes at once. At best, only one's changes 
        //  will be kept and there may be trouble.
        if (insert != null) {
            BufferedReader inFile = null;
            BufferedWriter outFile = null;
            String tempName = null;

            String2.log("\nprocessing " + insert);
            String first2 = insert.substring(0, 2);
            String abandoningI = "Abandoning " + first2 + " processing: ";
            if (ret.length() < 80)
                throw new RuntimeException(abandoningI + "GenerateDatasetsXml output only " + ret.length() + " bytes");
            int npo = insert.indexOf('#');
            if (npo <= 1)
                throw new RuntimeException(abandoningI + 
                    "'#' not found. Usage: " + first2 + "datasetsXmlName#tagName");
            if (npo >= insert.length() - 1) 
                throw new RuntimeException(abandoningI + 
                    "no tagName after '#'. Usage: " + first2 + "datasetsXmlName#tagName");
            String datasetsXmlName = insert.substring(2, npo);
            if (datasetsXmlName.length() == 0) {
                datasetsXmlName = EDStatic.contentDirectory + "datasets" + 
                    (EDStatic.developmentMode? "2" : "") + ".xml";
                String2.log("datasetsXmlName not specified, so using " + datasetsXmlName);
            }
            if (!File2.isFile(datasetsXmlName))
                throw new RuntimeException(abandoningI + 
                    "datasetsXmlName=" + datasetsXmlName + " file not found.");
            String tagName = insert.substring(npo + 1);
            if (reallyVerbose)
                String2.log("datasetsXmlName=" + datasetsXmlName + "\n" +
                    "tagName=" + tagName);
            String beginLine = "<!-- Begin GenerateDatasetsXml #" + tagName + " ";
            String endLine =   "<!-- End GenerateDatasetsXml #" + tagName + " ";
            String timeEol = localIsoTime + " -->\n"; 
            String endTag = "</erddapDatasets>";

            //copy datasets.xml line-by-line to new file, 
            tempName = datasetsXmlName + localCompactTime;
            inFile = new BufferedReader(new InputStreamReader(
                new FileInputStream(datasetsXmlName), "ISO-8859-1")); //charset to match datasets.xml
            outFile = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(tempName), "ISO-8859-1")); //charset to match datasets.xml
            
            //look for the beginLine  
            String line = inFile.readLine();
            while (line != null && line.indexOf(endTag) < 0 &&
                line.indexOf(beginLine) < 0) {
                //write if not stray endLine
                if (line.indexOf(endLine) < 0)
                    outFile.write(line + "\n");
                line = inFile.readLine();
            }

            //unexpected end of file?
            if (line == null) {
                inFile.close();  inFile = null; 
                outFile.close(); outFile = null; File2.delete(tempName);
                throw new RuntimeException(abandoningI + "\"" + beginLine + 
                    "\" and \"" + endTag + "\" not found in " + datasetsXmlName);
            }

            //found end of file
            if (line.indexOf(endTag) >= 0) {
                //found endTag </erddapDatasets>.  Write new stuff just before endTag.
                if (reallyVerbose)
                    String2.log("found endTag=" + endTag + " so writing info at end of file.");
                outFile.write(beginLine + timeEol);
                outFile.write(ret);
                outFile.write(endLine + timeEol);
                outFile.write(line + "\n");  //line with endTag
            } else {
                //found beginLine, so now look for the endLine (discard lines in between)
                if (reallyVerbose)
                    String2.log("found beginLine: " + beginLine);
                while (line != null && line.indexOf(endTag) < 0 &&
                    line.indexOf(endLine) < 0) 
                    line = inFile.readLine();
                if (line == null || line.indexOf(endTag) >= 0) {
                    inFile.close();  inFile = null;
                    outFile.close(); outFile = null; File2.delete(tempName);
                    throw new RuntimeException(abandoningI + "\"" + beginLine + 
                        "\" found, but \"" + endLine + "\" not found in " + 
                        datasetsXmlName);
                }
                //found endLine.  finish up.
                if (reallyVerbose)
                    String2.log("found endLine: " + endLine);
                outFile.write(beginLine + timeEol);
                outFile.write(ret);
                outFile.write(endLine + timeEol);
                line = inFile.readLine();
                while (line != null) {
                    outFile.write(line + "\n");
                    line = inFile.readLine();
                }
            }
            inFile.close();  inFile = null;
            outFile.close(); outFile = null;

            //rename temp file into place
            if (insert.startsWith("-i")) { 
                File2.rename(datasetsXmlName, tempName + "Retired");
                File2.rename(tempName, datasetsXmlName);
                //too risky to delete. If delete and trouble, people will be mad.
                //File2.delete(tempName + "Retired"); //only if everything was successful
                String2.log(insert + " finished successfully!");

            } else { //testmode
                String2.log(insert + " successfully created " + tempName);
            }

        }

        String2.returnLoggingToSystemOut();
        return ret;
    }

    /**
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the questions.
     */
    public static void main(String args[]) throws Throwable {
        (new GenerateDatasetsXml()).doIt(args, true);
        System.exit(0);
    }

}