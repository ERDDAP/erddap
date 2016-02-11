/*
 * ArchiveADataset Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.IntArray;
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
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.variable.*;
import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.GregorianCalendar;


/**
 * This is a command line program to run ArchiveADataset.
 * This is geared toward meeting the recommendations for submitting data to NOAA NCEI.
 * https://sites.google.com/a/noaa.gov/ncei-ioos-archive/cookbook
 * https://sites.google.com/a/noaa.gov/ncei-ioos-archive/cookbook/data-integrity
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2015-12-15
 */
public class ArchiveADataset {


    /** This gets the i'th value from args, or prompts the user. 
     * @throws RuntimeException("ControlC") if user presses ^C.
     */
    private String get(String args[], int i, String def, String prompt) throws Exception {
        String2.log("\n" + prompt +
            (def.length() > 0? "\n(or enter \"default\" for \"" + def + "\")" : ""));
        String s; 
        if (args.length > i) {
            String2.log("? " + args[i]);
            s = args[i];
        } else {
            s = String2.getStringFromSystemIn("? ");
        }
        if (s == null)  //null if ^C
            throw new RuntimeException("ControlC");
        s = String2.fromJson(s.trim()); 
        if (s.equals("default") || s.equals("\"default\"")) 
            s = def;
        else if (s.equals("\"\"") || s.equals("nothing") || s.equals("\"nothing\"")) //else is important
            s = "";
        return s;
    }

    /**
     * This is used when called from within a program.
     *
     * @param args if args has values, they are used to answer the questions.
     * @return the full name of the tgz file.
     */
    public String doIt(String args[]) throws Throwable {
        GregorianCalendar gcZ = Calendar2.newGCalendarLocal();
        String isoTime     = Calendar2.formatAsISODateTimeT(gcZ) + "Z";
        String compactTime = Calendar2.formatAsCompactDateTime(gcZ) + "Z";
        String aadDir = EDStatic.bigParentDirectory + "ArchiveADataset/";
        File2.makeDirectory(aadDir);
        String logFileName = aadDir + "log_" + compactTime + ".txt";
        String2.setupLog(true, false,  //toSystemOut, toSystemErr
            logFileName,
            false, String2.logFileDefaultMaxSize);  //append
        String2.log("*** Starting ArchiveADataset " + isoTime + "\n" + 
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());
        String resultName;
        FileOutputStream fos;
        Writer writer;
        String tgzName = null;
        int nErrors = 0;
        int nDataFilesCreated = 0;
        long startTime = System.currentTimeMillis();
        String def;  //default
        String error = "";
        String digestDefault = "SHA-256";
        String digestPrompt = 
            "Which type of file digest (checksum) do you want\n" +
            "(one of " + String2.toCSSVString(String2.FILE_DIGEST_OPTIONS) + ")";
        String digestType, digestExtension;

        if (args == null) 
            args = new String[0];

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

        //look for -dryRun (and remove it)
        boolean dryRun = false;  
        int dri = String2.indexOf(args, "-dryRun");
        if (dri >= 0) {
            String2.log("dryRun=true");
            dryRun = true;
            StringArray sa = new StringArray(args);
            sa.remove(dri);
            args = sa.toArray();
        }

        String newCommandLine = 
            (String2.OSIsWindows? "ArchiveADataset " : "./ArchiveADataset.sh ") + 
            (reallyVerbose? "-verbose " : "");
        EDD.verbose = true;
        EDD.reallyVerbose = reallyVerbose;
        NcHelper.verbose = reallyVerbose;
        OpendapHelper.verbose = reallyVerbose;
        Table.verbose = reallyVerbose;
        Table.reallyVerbose = reallyVerbose;

        try {
            //intro
            String2.log(
                "\n\n************ Archive A Dataset ************\n\n" +
                "This program will:\n" +
                "* Ask you a series of questions so that you can specify which subset\n" +
                "  of which dataset you want to archive and how you want it archived.\n" + 
                "  Enter \"default\" to get the default suggestion.\n" +
                "  Or, you can put all the answers on the command line\n" +
                "    (use \"\" (two double quotes) as a placeholder for an empty string).\n" +
                "* Make a series of requests to the dataset and stage the files in\n" +
                "  " + aadDir + "\n" +
                "  Each of those files must be <2GB.\n" +
                "* Make a .tar.gz file from all of the staged files.\n" +
                "  It may be any size (limited only by disk space).\n" +
                "* Make a file (e.g., .sha256) with the digest of the .tar.gz file.\n" +
                "* Make an .listOfFiles.txt file with the list of files in the .tar.gz file.\n" +
                "* Delete all of the staged files.\n" +
                "\n" +
                "Diagnostic information is shown on the screen and put in\n" +
                logFileName + "\n" +
                "Press ^D or ^C to exit this program at any time.\n" +
                "For detailed information, see\n" +
                "http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html#ArchiveADataset");
            
            //get email address
            int whichArg = 0;
            String contactEmail = get(args, whichArg++, EDStatic.adminEmail, //default
                "What is a contact email address for this archive\n" +
                "(it will be written in the READ_ME.txt file in the archive)");
            
            //get the datasetID
            //FUTURE? allow datasetID to be a URL of a remote dataset?
            String datasetID = datasetID = get(args, whichArg++, "", //default
                "What is the datasetID of the dataset to be archived");
            if (datasetID.length() == 0)
                throw new RuntimeException("You must specify a valid datasetID.");
            String2.log("Creating the dataset...");
            EDD edd = EDD.oneFromDatasetsXml(null, datasetID); 
            EDV dataVars[] = edd.dataVariables();
            int ndv = dataVars.length;

            tgzName           = aadDir + datasetID + "_" + compactTime + ".tar.gz";
            String archiveDir = aadDir + datasetID + "_" + compactTime + "/";
            String2.log("The files to be archived will be staged in\n  " + 
                archiveDir);
            File2.delete(tgzName); //if any
            File2.makeDirectory(archiveDir);
            //delete files? yes/no
            File2.deleteAllFiles(archiveDir, true, true); //deleteEmptySubdirectories

            String archiveDataDir = archiveDir + "data/";
            File2.makeDirectory(archiveDataDir);

            if (edd instanceof EDDGrid) {

                //*** EDDGrid datasets
                EDDGrid eddGrid = (EDDGrid)edd;
                EDVGridAxis[] axisVars = eddGrid.axisVariables();
                int nav = axisVars.length;
                EDVGridAxis axis0 = axisVars[0];
                String axis0Name = axis0.destinationName();
                String baseRequestUrl = "/" + EDStatic.erddapUrl + "/griddap/" + datasetID;

                //build getEverything and get0
                StringBuilder get0SB = new StringBuilder();
                StringBuilder getEverythingSB = new StringBuilder();
                for (int av = 0; av < nav; av++) {
                    EDVGridAxis edvga = axisVars[av];
                    get0SB.append("[0]");                    
                    getEverythingSB.append(av == 0?
                        "[(" +
                        (edvga.isAscending()? edvga.destinationMinString() : 
                                              edvga.destinationMaxString()) + 
                        "):(" + 
                        (edvga.isAscending()? edvga.destinationMaxString() : 
                                              edvga.destinationMinString()) + 
                        ")]" :
                        "[]");
                }
                String get0 = get0SB.toString();
                String getEverything = getEverythingSB.toString();

                //which data variables?
                String dataVarsCSV = get(args, whichArg++, "", //default
                    datasetID + " has these data variables:\n" + 
                    String2.toCSVString(eddGrid.dataVariableDestinationNames()) + "\n" +
                    "Which data variables do you want to archive\n" +
                    "(enter a comma-separated list, or just press Enter to archive all)");          
                StringArray dataVarsSA = StringArray.fromCSV(dataVarsCSV);
                if (dataVarsSA.size() == 0)
                    dataVarsSA = new StringArray(eddGrid.dataVariableDestinationNames());
                StringArray junk = new StringArray();
                IntArray constraints = new IntArray();
                //test validity
                for (int i = 0; i < dataVarsSA.size(); i++) {
                    eddGrid.parseDataDapQuery(
                        dataVarsSA.get(0) + get0,  //not getEverything, it might trigger too-much-data error
                        junk, constraints, false); //repair
                }

                //which constraints?
                String constraintsString = get(args, whichArg++, 
                    getEverything, //default
                    "\nThe axes are [" + 
                    String2.toSVString(eddGrid.axisVariableDestinationNames(),
                        "][", false) + 
                    "].\n" +
                    "You probably won't be able to archive a large gridded dataset all at once.\n" +
                    "It is too likely that something will go wrong,\n" +
                    "or the resulting .tar.gz file will be too large to transmit.\n" +
                    "Instead, try archiving a week or month's worth.\n" +
                    "The default shown below gets everything -- change it.\n" +
                    "What subset do you want to archive");

                //parse the constraints to test validity
                eddGrid.parseDataDapQuery(
                    //pretend request is just for 1 var (I only care about the constraints)
                    dataVarsSA.get(0) + constraintsString, 
                    junk, constraints, false); //repair

                //isolate the constraints for the axes after leftmost
                int po = constraintsString.indexOf(']');
                if (po < 0)
                    throw new RuntimeException("']' not found in constraints.");
                String rightConstraints = constraintsString.substring(po + 1);

                //which type of file digest?
                digestType = get(args, whichArg++, digestDefault, digestPrompt);
                int whichDigest = String2.indexOf(String2.FILE_DIGEST_OPTIONS, digestType);
                if (whichDigest < 0)
                    throw new RuntimeException("Invalid file digest type.");
                digestExtension = String2.FILE_DIGEST_EXTENSIONS[whichDigest];

                //*** write info about this archiving to archiveDir
                String2.log(
                    "\n*** Creating the files to be archived...\n" +
                      "    This may take a long time.\n");
                Math2.sleep(5000);

                error = String2.writeToFile(archiveDir + "READ_ME.txt", 
                    "This archive was created by the ArchiveADataset script\n" +
                    "(which is part of ERDDAP) starting at " + isoTime + "\n" +
                    "based on these settings:\n" +
                    "Contact email=" + contactEmail + "\n" +
                    "ERDDAP datasetID=" + datasetID + "\n" +
                    "Data variables=" + dataVarsCSV + "\n" +
                    "Constraints=" + constraintsString + "\n" +
                    "Digest type=" + digestType + "\n",
                    "ISO-8859-1");
                if (error.length() > 0)
                    throw new RuntimeException(error);
                newCommandLine += 
                    String2.quoteParameterIfNeeded(contactEmail)      + " " +
                    String2.quoteParameterIfNeeded(datasetID)         + " " +
                    String2.quoteParameterIfNeeded(dataVarsCSV)       + " " +
                    String2.quoteParameterIfNeeded(constraintsString) + " " +
                    String2.quoteParameterIfNeeded(digestType);

                //save .das to archiveDir
                resultName = eddGrid.makeNewFileForDapQuery(null, null, "", 
                    archiveDir, datasetID, ".das"); 
 
                //save .dds to archiveDir
                resultName = eddGrid.makeNewFileForDapQuery(null, null, "", 
                    archiveDir, datasetID, ".dds"); 
 
                //write the data files to archiveDataDir
                int axis0start  = constraints.get(0);
                int axis0stride = constraints.get(1);
                int axis0stop   = constraints.get(2);
                boolean axis0IsTimeStamp = axis0 instanceof EDVTimeStampGridAxis;
                for (int axis0i = axis0start; axis0i <= axis0stop; axis0i += axis0stride) {
                    String value = axis0.destinationString(axis0i);
                    String fileName = axis0IsTimeStamp?
                        Calendar2.formatAsCompactDateTime(
                            Calendar2.epochSecondsToGc(axis0.destinationDouble(axis0i))) + "Z" :
                        String2.encodeFileNameSafe(value);
                    String2.log("writing data file for " +
                        axis0Name + "[" + axis0i + "]=" + value + 
                        "\n  " + fileName);
                    String tConstraints = "[("+ value + ")]" + rightConstraints; //userDapQuery
                    StringBuilder query = new StringBuilder(); 
                    for (int dv = 0; dv < dataVarsSA.size(); dv++) 
                        query.append(
                            (dv == 0? "" : ",") +                         
                            dataVarsSA.get(dv) + tConstraints);
                    if (dryRun) {
                        String2.log("  query=" + query);
                    } else {
                        try {
                            String fullName = archiveDataDir + fileName + ".nc";
                            eddGrid.saveAsNc(baseRequestUrl + ".nc", query.toString(),                        
                                fullName, true, 0); //keepUnusedAxes, lonAdjust
                            nDataFilesCreated++;

                            //make the file digest
                            error = String2.writeToFile(fullName + digestExtension, 
                                String2.fileDigest(digestType, fullName) + 
                                    "  " + fileName + ".nc\n",
                                "ISO-8859-1");
                            if (error.length() > 0)
                                throw new RuntimeException(error);

                        } catch (Exception e) {
                            String2.log("ERROR #" + nErrors++ + "\n" +
                                MustBe.throwableToString(e));
                        }
                    }
                }

            } else {

                //*** EDDTable datasets
                EDDTable eddTable = (EDDTable)edd;
                String baseRequestUrl = EDStatic.erddapUrl + "/tabledap/" + datasetID;
                StringBuilder sb;

                //which data variables?
                String dataVarsCSV = get(args, whichArg++, "",  //default
                    datasetID + " has these data variables:\n" + 
                    String2.toCSVString(eddTable.dataVariableDestinationNames()) + "\n" +
                    "Which data variables do you want to archive\n" +
                    "(enter a comma-separated list, or press Enter to archive all)");          
                dataVarsCSV = String2.replaceAll(dataVarsCSV, " ", ""); //remove any spaces
                StringArray dataVarsSA = StringArray.fromCSV(dataVarsCSV);
                StringArray resultVars = new StringArray();
                StringArray conVars    = new StringArray();
                StringArray conOps     = new StringArray();
                StringArray conValues  = new StringArray();
                eddTable.parseUserDapQuery(
                    dataVarsCSV, 
                    resultVars, conVars, conOps, conValues, false); //repair

                //extra constraints?
                String extraConstraints = get(args, whichArg++, "", //default
                    "For all but the largest tabular datasets, you can archive the dataset\n" +
                    "all at once.\n" +
                    "If you want to archive a subset of the dataset,\n" +
                    "enter an ERDDAP constraint expression to specify the subset,\n" +
                    "for example, &time>=2015-01-01&time<2015-02-01\n" +
                    "or press Enter for no constraints");
                //parse dataVars+constraints to ensure valid
                eddTable.parseUserDapQuery(
                    dataVarsCSV + extraConstraints, 
                    resultVars, conVars, conOps, conValues, false); //repair

                //subset by which variables?
                //default is cf_role variables
                StringArray cfRoleVars = new StringArray();
                for (int dv = 0; dv < ndv; dv++) {
                    String tRole = dataVars[dv].combinedAttributes().getString("cf_role");
                    if (tRole == null || 
                        tRole.equals("profile_id")) //put all profiles for a (trajectory) in one file
                        continue;
                    cfRoleVars.add(dataVars[dv].destinationName());
                }
                String subsetByCSV = get(args, whichArg++, cfRoleVars.toString(),  //default
                    "Separate files will be made for each unique combination of values of some\n" +
                    "variables. Each of those files must be <2GB.\n" +
                    "If you don't specify any variables, everything will be put into one file --\n" +
                    "for some datasets, this will be >2GB and will fail.\n" +
                    "Which variables will be used for this");
                subsetByCSV = String2.replaceAll(subsetByCSV, " ", ""); //remove any spaces
                StringArray subsetBySA = StringArray.fromCSV(subsetByCSV);          

                //which fileType
                boolean accNcCF = eddTable.accessibleViaNcCF().length() == 0 && 
                    cfRoleVars.size() > 0;
                StringArray fileTypeOptions = new StringArray();
                if (accNcCF) {
                    fileTypeOptions.add(".ncCF");
                    fileTypeOptions.add(".ncCFMA");
                }
                fileTypeOptions.add(".nc");
                fileTypeOptions.add(".csv");
                fileTypeOptions.add(".json");
                def = accNcCF && subsetByCSV.length() > 0? ".ncCFMA" : 
                      accNcCF?                             ".ncCF"   : ".nc";
                String fileType = "";
                while (fileType.length() == 0) {
                    fileType = get(args, whichArg, def, 
                        "Create which file type (" + fileTypeOptions.toString() + ")");
                    if (fileTypeOptions.indexOf(fileType) < 0) {
                        String msg = "fileType=" + fileType + " is not a valid option.";
                        if (args.length > whichArg) {
                            throw new RuntimeException(msg);
                        } else {
                            String2.log(msg);
                            fileType = "";
                        }
                    }
                }
                whichArg++;

                //which type of file digest?
                digestType = get(args, whichArg++, digestDefault, digestPrompt);
                int whichDigest = String2.indexOf(String2.FILE_DIGEST_OPTIONS, digestType);
                if (whichDigest < 0)
                    throw new RuntimeException("Invalid file digest type.");
                digestExtension = String2.FILE_DIGEST_EXTENSIONS[whichDigest];

                //*** write info about this archiving to archiveDir
                String2.log(
                    "\n*** Creating the files to be archived...\n" +
                      "    This may take a long time.\n");
                Math2.sleep(5000);

                error = String2.writeToFile(archiveDir + "READ_ME.txt", 
                    "This archive was created by the ArchiveADataset script\n" +
                    "(which is part of ERDDAP) starting at " + isoTime + "\n" +
                    "based on these settings:\n" +
                    "Contact email=" + contactEmail + "\n" +
                    "ERDDAP datasetID=" + datasetID + "\n" +
                    "Data variables=" + dataVarsCSV + "\n" +
                    "Extra constraints=" + extraConstraints + "\n" +
                    "Subset by=" + subsetByCSV + "\n" +
                    "Data file type=" + fileType + "\n" +
                    "Digest type=" + digestType + "\n",
                    "ISO-8859-1");
                if (error.length() > 0)
                    throw new RuntimeException(error);
                newCommandLine += 
                    String2.quoteParameterIfNeeded(contactEmail)     + " " +
                    String2.quoteParameterIfNeeded(datasetID)        + " " +
                    String2.quoteParameterIfNeeded(dataVarsCSV)      + " " +
                    String2.quoteParameterIfNeeded(extraConstraints) + " " +
                    String2.quoteParameterIfNeeded(subsetByCSV)      + " " +
                    String2.quoteParameterIfNeeded(fileType)         + " " +
                    String2.quoteParameterIfNeeded(digestType);

                //save .das to archiveDir
                resultName = eddTable.makeNewFileForDapQuery(null, null, "", 
                    archiveDir, datasetID, ".das"); 
 
                //save .dds to archiveDir
                resultName = eddTable.makeNewFileForDapQuery(null, null, "", 
                    archiveDir, datasetID, ".dds"); 
 
                if (subsetBySA.size() == 0) {
                    //deal with all in one file
                    String fileName = "allData";
                    String tQuery = dataVarsCSV + extraConstraints;
                    String2.log("writing all data to " + 
                        archiveDataDir + fileName + fileType + 
                        " tQuery=" + tQuery);
                    if (!dryRun) {
                        try {
                            resultName = eddTable.makeNewFileForDapQuery(null, null, 
                                tQuery, archiveDataDir, datasetID, fileType); 
                            nDataFilesCreated++;

                            //make the .md5file
                            error = String2.writeToFile(
                                archiveDataDir + resultName + digestExtension, 
                                String2.fileDigest(digestType, archiveDataDir + resultName) + 
                                    "  " + resultName + "\n",
                                "ISO-8859-1");
                            if (error.length() > 0)
                                throw new RuntimeException(error);

                        } catch (Exception e) {
                            String2.log("ERROR #" + nErrors++ + "\n" +
                                MustBe.throwableToString(e));
                        }
                    }

                } else {

                    //get the list of subsetBy combinations
                    resultName = eddTable.makeNewFileForDapQuery(null, null, 
                        subsetByCSV + extraConstraints + "&distinct()", 
                        archiveDir, "combos", ".nc"); 
                    Table combos = new Table(); 
                    combos.readFlatNc(archiveDir + resultName, null, 0);
                    File2.delete(     archiveDir + resultName);
                    int nComboRows = combos.nRows();
                    int nComboCols = combos.nColumns();
                    boolean isString[] = new boolean[nComboCols];
                    for (int col = 0; col < nComboCols; col++) 
                        isString[col] = combos.getColumn(col) instanceof StringArray;

                    //write the data files to archiveDataDir
                    for (int row = 0; row < nComboRows; row++) {
                        //make directory tree from nComboCols-1 
                        StringBuilder tDir   = new StringBuilder(archiveDataDir);
                        StringBuilder tQuery = new StringBuilder(dataVarsCSV + extraConstraints);
                        String fileName = null;
                        for (int col = 0; col < nComboCols; col++) {
                            String s = combos.getStringData(col, row); //times will be iso format
                            if (col == nComboCols - 1)
                                fileName = String2.encodeFileNameSafe(s);
                            else 
                                tDir.append(String2.encodeFileNameSafe(s) + "/");
                            tQuery.append("&" + combos.getColumnName(col) + "=" +
                                (isString[col]? String2.toJson(s) : s));
                        }

                        //write the file and the .md5 file
                        String2.log("writing data file for combo #" + row + "\n" +
                            "  fileName=" + tDir + fileName + fileType + "\n" +
                            "  tQuery=" + tQuery);
                        File2.makeDirectory(tDir.toString());
                        if (!dryRun) {
                            try {
                                resultName = eddTable.makeNewFileForDapQuery(null, null, 
                                    tQuery.toString(), tDir.toString(), fileName, fileType); 
                                nDataFilesCreated++;

                                //make the .md5file
                                error = String2.writeToFile(
                                    tDir.toString() + resultName + digestExtension, 
                                    String2.fileDigest(digestType, tDir.toString() + resultName) + 
                                        "  " + resultName + "\n",
                                    "ISO-8859-1");
                                if (error.length() > 0)
                                    throw new RuntimeException(error);

                            } catch (Exception e) {
                                String2.log("ERROR #" + nErrors++ + "\n" +
                                    MustBe.throwableToString(e));
                            }
                        }
                    }
                }
            }

            //make the .tgz file
            String2.log("\n*** making " + tgzName);
            FileVisitorDNLS.makeTgz(archiveDir, ".*", true, ".*", 
                tgzName); 

            //make the .md5file of the tgzName
            String2.log("\n*** making " + tgzName + digestExtension);
            error = String2.writeToFile(  tgzName + digestExtension, 
                String2.fileDigest(digestType, tgzName) + 
                    "  " + tgzName + "\n",
                "ISO-8859-1");
            if (error.length() > 0)
                throw new RuntimeException(error);

            //make the .listOfFiles.txt of the tgzName
            String2.log("\n*** making " + tgzName + ".listOfFiles.txt");
            error = String2.writeToFile(  tgzName + ".listOfFiles.txt", 
                FileVisitorDNLS.oneStepToString(archiveDir, ".*", true, ".*"),
                "ISO-8859-1");
            if (error.length() > 0)
                throw new RuntimeException(error);

            //delete the staged files
            String2.log("\n*** deleting staged files in " + archiveDir);
            File2.deleteAllFiles(archiveDir, true, true); //deleteEmptySubdirectories
            File2.delete(archiveDir); //delete the dir itself

            //success!  
            String2.log("\n\n********** ArchiveADataset succeeded!\n" +
                "The archive file is\n" +
                tgzName + "\n" +
                "A command line with all of these settings is\n" +
                newCommandLine + "\n" + 
                "nDataFilesCreated=" + nDataFilesCreated + " nErrors=" + nErrors +
                " time=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - startTime));

        } catch (Throwable t) {

            String msg = MustBe.throwableToString(t);
            if (msg.indexOf("ControlC") >= 0) {
                String2.flushLog();
                return null;
            }
            String2.log(msg);
            String2.log("\n\n********** ArchiveADataset failed.\n" +
                "A command line with all of these settings is\n" +
                newCommandLine + "\n" + 
                "nDataFilesCreated=" + nDataFilesCreated + " nErrors=" + nErrors +
                " time=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - startTime));
            throw t;
        }
        String2.flushLog();
        String2.returnLoggingToSystemOut();
        return tgzName;
    }

    public static void testNcCF() throws Throwable {
        String2.log("*** ArchiveADataset.testNcCF()");

        //make the tgz
        String tgzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "bob.simons@noaa.gov",
            "cwwcNDBCMet", 
            "default", //all data vars
            "&station=~\"3.*\"", // &station=~"3.*"
            "nothing", //should be station, but use "nothing" to test save as points
            "default", //.ncCF
            "MD5"});   

        //display it (in 7zip)
        if (tgzName != null)
            SSR.displayInBrowser("file://" + tgzName); 

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String results = String2.readFromFile(tgzName + ".listOfFiles.txt")[1];
        String expected = 
"cwwcNDBCMet.das                                                  " + today + "T.{8}Z         1462.\n" +
"cwwcNDBCMet.dds                                                  " + today + "T.{8}Z           394\n" +
"READ_ME.txt                                                      " + today + "T.{8}Z           300\n" +
"data/\n" +
"  cwwcNDBCMet.nc                                                 " + today + "T.{8}Z      12351220\n" +
"  cwwcNDBCMet.nc.md5                                             " + today + "T.{8}Z            49\n";
        Test.ensureLinesMatch(results, expected, "results=\n" + results);
        
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testTrajectoryProfile() throws Throwable {
        String2.log("*** ArchiveADataset.testTrajectoryProfile()");

        //make the tgz
        String tgzName = (new ArchiveADataset()).doIt(new String[]{
            //"-verbose",  //verbose is really verbose for this test
            "bob.simons@noaa.gov",
            "scrippsGliders", 
            "default", //all data vars
            // &trajectory=~"sp05.*"&time>=2015-01-01&time<=2015-01-05
            "&trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05", 
            "default", "default", //trajectory, .ncCFMA
            "default"}); //SHA-256

        //display it (in 7zip)
        if (tgzName != null)
            SSR.displayInBrowser("file://" + tgzName); 

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String results = String2.readFromFile(tgzName + ".listOfFiles.txt")[1];
        String expected = 
"READ_ME.txt                                                      " + today + "T.{8}Z           359\n" +
"scrippsGliders.das                                               " + today + "T.{8}Z         12528\n" +
"scrippsGliders.dds                                               " + today + "T.{8}Z           679\n" +
"data/\n" +
"  sp051-20141112.nc                                              " + today + "T.{8}Z        133832\n" +
"  sp051-20141112.nc.sha256                                       " + today + "T.{8}Z            84\n" +
"  sp052-20140814.nc                                              " + today + "T.{8}Z        451080\n" +
"  sp052-20140814.nc.sha256                                       " + today + "T.{8}Z            84\n";
        Test.ensureLinesMatch(results, expected, "results=\n" + results);
        
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testGridAll() throws Throwable {
        String2.log("*** ArchiveADataset.testGridAll()");

        //make the tgz
        String tgzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "default",  //constraintsString
            "SHA-256"}); //SHA-256
        //display it (in 7zip)
        if (tgzName != null)
            SSR.displayInBrowser("file://" + tgzName); 

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String results = String2.readFromFile(tgzName + ".listOfFiles.txt")[1];
        String expected = 
"erdVHNchla8day.das                                               " + today + "T.{8}Z          5954\n" +
"erdVHNchla8day.dds                                               " + today + "T.{8}Z           438\n" +
"READ_ME.txt                                                      " + today + "T.{8}Z           307\n" +
"data/\n" +
"  20150301000000Z.nc                                             " + today + "T.{8}Z     447840428\n" +
"  20150301000000Z.nc.sha256                                      " + today + "T.{8}Z            85\n" +
"  20150302000000Z.nc                                             " + today + "T.{8}Z     447840428\n" +
"  20150302000000Z.nc.sha256                                      " + today + "T.{8}Z            85\n";
        Test.ensureLinesMatch(results, expected, "results=\n" + results);
        
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testGridSubset() throws Throwable {
        String2.log("*** ArchiveADataset.testGridSubset()");

        //make the tgz
        String tgzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "[(2015-03-02T00:00:00Z)][][][]",  //constraintsString
            "SHA-1"});

        //display it (in 7zip)
        if (tgzName != null)
            SSR.displayInBrowser("file://" + tgzName); 

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String results = String2.readFromFile(tgzName + ".listOfFiles.txt")[1];
        String expected = 
"erdVHNchla8day.das                                               " + today + "T.{8}Z          5954\n" +
"erdVHNchla8day.dds                                               " + today + "T.{8}Z           438\n" +
"READ_ME.txt                                                      " + today + "T.{8}Z           282\n" +
"data/\n" +
"  20150302000000Z.nc                                             " + today + "T.{8}Z     447840428\n" +
"  20150302000000Z.nc.sha1                                        " + today + "T.{8}Z            61\n";
        Test.ensureLinesMatch(results, expected, "results=\n" + results);
        
        //String2.pressEnterToContinue("\n"); 
    }

    /** This tests this class. */
    public static void test() throws Throwable {
        String2.log("*** ArchiveADataset.test()");

        /* */
        testNcCF();
        testTrajectoryProfile();
        testGridAll();
        testGridSubset(); 
    }


    /**
     * This is used when called from the command line.
     * It explicitly calls System.exit(0) when done.
     *
     * @param args if args has values, they are used to answer the questions.
     */
    public static void main(String args[]) throws Throwable {
        (new ArchiveADataset()).doIt(args);
        System.exit(0);
    }

}