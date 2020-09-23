/*
 * ArchiveADataset Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.GregorianCalendar;

import ucar.nc2.NetcdfFileWriter;

/**
 * This is a command line program to run ArchiveADataset.
 * This is geared toward meeting the IOOS and BagIt recommendations 
 * for submitting data to NOAA NCEI.
 * https://sites.google.com/a/noaa.gov/ncei-ioos-archive/cookbook
 * https://sites.google.com/a/noaa.gov/ncei-ioos-archive/cookbook/data-integrity
 * https://en.wikipedia.org/wiki/BagIt
 * https://tools.ietf.org/html/draft-kunze-bagit-14  If change, change BagIt-Version below.
 *
 * Bob has Bagger (GUI program from Library of Congress) downloaded from
 *  https://github.com/LibraryOfCongress/bagger/releases/
 * To run, double click on /programs/bagger-2.7.4/bagger-2.7.4/bin/bagger.bat
 * (I specified JAVA_HOME in the .bat file.)
 * To verify a bag: File : Open existing bag 
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
            if (s == null)  //null if ^C
                throw new RuntimeException("ControlC");
        }
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            s = String2.fromJson(s); 
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
        GregorianCalendar gcZ = Calendar2.newGCalendarZulu();
        String isoTime     = Calendar2.formatAsISODateTimeTZ(gcZ);
        String compactTime = Calendar2.formatAsCompactDateTime(gcZ) + "Z";
        String aadDir = EDStatic.bigParentDirectory + "ArchiveADataset/";
        File2.makeDirectory(aadDir);
        String logFileName = aadDir + "log_" + compactTime + ".txt";
        String2.setupLog(true, false,  //toSystemOut, toSystemErr
            logFileName,
            false, String2.logFileDefaultMaxSize);  //append
        String2.log("*** Starting ArchiveADataset " + isoTime + " erddapVersion=" + EDStatic.erddapVersion + "\n" +  
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
        String bagitDigestDefault = "SHA-256"; //SHA-1, but NOAA wants SHA-256
        String digestPrompt = 
            "Which type of file digest (checksum) do you want\n" +
            "(specify one of " + String2.toCSSVString(String2.FILE_DIGEST_OPTIONS) + ")";
        String bagitDigestPrompt = digestPrompt + "\n" +
            "(BagIt spec recommends MD5 and SHA-1. NCEI prefers SHA-256.)";
        String digestType, digestExtension, digestExtension1;

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
        String manifestFullFileName = null;
        Writer manifestFileWriter = null;
        String aadSettings = null;
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
                "  Enter \"default\" (without the quotes) to get the default suggestion.\n" +
                "  Press Enter or enter \"\" (two double quotes) or the word \"nothing\"\n" +
                "    (without quotes) to specify a 0-length string.\n" +
                "  Or, you can put all the answers as parameters on the command line.\n" +
                "* Make a series of requests to the dataset and stage the netcdf-3 files in\n" +
                "  " + aadDir + "\n" +
                "  Each of those files must be <2GB.\n" +
                "* Make related files (e.g., a file with a list of data files).\n" +
                "* Make a container (e.g., .zip file) from all of the staged files.\n" +
                "  It may be any size (limited only by disk space).\n" +
                "* Make a file (e.g., .md5.txt) with the digest of the container.\n" +
                "* Delete all of the staged files.\n" +
                "\n" +
                "Diagnostic information is shown on the screen and put in\n" +
                logFileName + "\n" +
                "Press ^D or ^C to exit this program at any time.\n" +
                "For detailed information, see\n" +
                "https://coastwatch.pfeg.noaa.gov/erddap/download/setup.html#ArchiveADataset");
            
            //get bagitMode
            int whichArg = 0;
            String mode = get(args, whichArg++, "BagIt", //default
                "Which type of container (original or BagIt)\n" +
                "(NCEI prefers BagIt)");
            String modeLC = mode.toLowerCase();
            if (!modeLC.equals("original") && !modeLC.equals("bagit"))
                throw new RuntimeException("You must specify 'original' or 'BagIt'.");
            boolean bagitMode = modeLC.equals("bagit");
            String textFileEncoding = bagitMode? String2.UTF_8 : String2.ISO_8859_1;

            //compression
            String compression = get(args, whichArg++, "tar.gz", //default
                "Which type of compression (zip or tar.gz)\n" +
                "(NCEI prefers tar.gz)").toLowerCase();
            if (!compression.equals("zip") && !compression.equals("tar.gz"))
                throw new RuntimeException("You must specify 'zip' or 'tar.gz'.");
            
            //get email address
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

            tgzName           = aadDir + datasetID + "_" + compactTime + "." + compression;
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
                    "or the resulting ." + compression + " file will be too large to transmit.\n" +
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
                digestType = get(args, whichArg++, 
                    bagitMode? bagitDigestDefault : digestDefault, 
                    bagitMode? bagitDigestPrompt  : digestPrompt);
                int whichDigest = String2.indexOf(String2.FILE_DIGEST_OPTIONS, digestType);
                if (whichDigest < 0)
                    throw new RuntimeException("Invalid file digest type.");
                digestExtension  = String2.FILE_DIGEST_EXTENSIONS[whichDigest];
                digestExtension1 = digestExtension.substring(1);

                //*** write info about this archiving to archiveDir
                String2.log(
                    "\n*** Creating the files to be archived...\n" +
                      "    This may take a long time.\n");
                Math2.sleep(5000);

                if (bagitMode) {
                    manifestFullFileName = archiveDir + "manifest-" + 
                        digestExtension1 + ".txt"; //md5 or sha256
                    manifestFileWriter = String2.getBufferedOutputStreamWriterUtf8(
                        new FileOutputStream(manifestFullFileName));            

                    aadSettings = 
                        "ArchiveADataset_container_type: " + mode + "\n" +
                        "ArchiveADataset_compression: " + compression + "\n" +
                        "ArchiveADataset_contact_email: " + contactEmail + "\n" +
                        "ArchiveADataset_ERDDAP_datasetID: " + datasetID + "\n" +
                        "ArchiveADataset_data_variables: " + dataVarsCSV + "\n" +
                        "ArchiveADataset_constraints: " + constraintsString + "\n" +
                        "ArchiveADataset_digest_type: " + digestType + "\n";

                } else {
                    error = String2.writeToFile(archiveDir + "READ_ME.txt", 
                        "This archive was created by the ArchiveADataset script\n" +
                        "(which is part of ERDDAP v" + EDStatic.erddapVersion + 
                            ") starting at " + isoTime + "\n" +
                        "based on these settings:\n" +
                        "Container type=" + mode + "\n" +
                        "Compression=" + compression + "\n" +
                        "Contact email=" + contactEmail + "\n" +
                        "ERDDAP datasetID=" + datasetID + "\n" +
                        "Data variables=" + dataVarsCSV + "\n" +
                        "Constraints=" + constraintsString + "\n" +
                        "Digest type=" + digestType + "\n",
                        textFileEncoding);
                    if (error.length() > 0)
                        throw new RuntimeException(error);

                    //save .das to archiveDir
                    resultName = eddGrid.makeNewFileForDapQuery(null, null, "", 
                        archiveDir, datasetID, ".das"); 
     
                    //save .dds to archiveDir
                    resultName = eddGrid.makeNewFileForDapQuery(null, null, "", 
                        archiveDir, datasetID, ".dds"); 
                 }

                newCommandLine += 
                    String2.quoteParameterIfNeeded(mode)              + " " +
                    String2.quoteParameterIfNeeded(contactEmail)      + " " +
                    String2.quoteParameterIfNeeded(datasetID)         + " " +
                    String2.quoteParameterIfNeeded(dataVarsCSV)       + " " +
                    String2.quoteParameterIfNeeded(constraintsString) + " " +
                    String2.quoteParameterIfNeeded(digestType);

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
                            eddGrid.saveAsNc(NetcdfFileWriter.Version.netcdf3,
                                "ArchiveADataset", //pseudo ipAddress
                                baseRequestUrl + ".nc", query.toString(),                        
                                fullName, true, 0); //keepUnusedAxes, lonAdjust
                            nDataFilesCreated++;

                            //write the file digest info
                            String digest = String2.fileDigest(digestType, fullName);
                            if (bagitMode) {
                                manifestFileWriter.write(
                                    digest + "  data/" + fileName + ".nc\n");
                            } else {
                                error = String2.writeToFile(fullName + digestExtension, 
                                    digest + "  " + fileName + ".nc\n", 
                                    textFileEncoding);
                                if (error.length() > 0)
                                    throw new RuntimeException(error);
                            }

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
                def = accNcCF && subsetByCSV.length() > 0? ".ncCFMA" : ".nc"; //NCEI prefers .ncCFMA
                String fileType = "";
                while (fileType.length() == 0) {
                    fileType = get(args, whichArg, def, 
                        "Create which file type (" + fileTypeOptions.toString() + ")\n" +
                        "(NCEI prefers .ncCFMA if it is an option)");
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
                digestType = get(args, whichArg++, 
                    bagitMode? bagitDigestDefault : digestDefault, 
                    bagitMode? bagitDigestPrompt  : digestPrompt);
                int whichDigest = String2.indexOf(String2.FILE_DIGEST_OPTIONS, digestType);
                if (whichDigest < 0)
                    throw new RuntimeException("Invalid file digest type.");
                digestExtension = String2.FILE_DIGEST_EXTENSIONS[whichDigest];
                digestExtension1 = digestExtension.substring(1);

                //*** write info about this archiving to archiveDir
                String2.log(
                    "\n*** Creating the files to be archived...\n" +
                      "    This may take a long time.\n");
                Math2.sleep(5000);

                if (bagitMode) {
                    manifestFullFileName = archiveDir + "manifest-" + 
                        digestExtension1 + ".txt"; //md5 or sha256
                    manifestFileWriter = String2.getBufferedOutputStreamWriterUtf8(
                        new FileOutputStream(manifestFullFileName));            

                    aadSettings = 
                        "ArchiveADataset_container_type: " + mode + "\n" +
                        "ArchiveADataset_compression: " + compression + "\n" +
                        "ArchiveADataset_contact_email: " + contactEmail + "\n" +
                        "ArchiveADataset_ERDDAP_datasetID: " + datasetID + "\n" +
                        "ArchiveADataset_data_variables: " + dataVarsCSV + "\n" +
                        "ArchiveADataset_extra_constraints: " + extraConstraints + "\n" +
                        "ArchiveADataset_subset_by: " + subsetByCSV + "\n" +
                        "ArchiveADataset_data_file_type: " + fileType + "\n" +
                        "ArchiveADataset_digest_type: " + digestType + "\n";

                } else {
                    error = String2.writeToFile(archiveDir + "READ_ME.txt", 
                        "This archive was created by the ArchiveADataset script\n" +
                        "(which is part of ERDDAP v" + EDStatic.erddapVersion + 
                            ") starting at " + isoTime + "\n" +
                        "based on these settings:\n" +
                        "Container type=" + mode + "\n" +
                        "Compression=" + compression + "\n" +
                        "Contact email=" + contactEmail + "\n" +
                        "ERDDAP datasetID=" + datasetID + "\n" +
                        "Data variables=" + dataVarsCSV + "\n" +
                        "Extra constraints=" + extraConstraints + "\n" +
                        "Subset by=" + subsetByCSV + "\n" +
                        "Data file type=" + fileType + "\n" +
                        "Digest type=" + digestType + "\n",
                        textFileEncoding);
                    if (error.length() > 0)
                        throw new RuntimeException(error);

                    //save .das to archiveDir
                    resultName = eddTable.makeNewFileForDapQuery(null, null, "", 
                        archiveDir, datasetID, ".das"); 
     
                    //save .dds to archiveDir
                    resultName = eddTable.makeNewFileForDapQuery(null, null, "", 
                        archiveDir, datasetID, ".dds"); 
                }
 
                newCommandLine += 
                    String2.quoteParameterIfNeeded(mode)              + " " +
                    String2.quoteParameterIfNeeded(contactEmail)     + " " +
                    String2.quoteParameterIfNeeded(datasetID)        + " " +
                    String2.quoteParameterIfNeeded(dataVarsCSV)      + " " +
                    String2.quoteParameterIfNeeded(extraConstraints) + " " +
                    String2.quoteParameterIfNeeded(subsetByCSV)      + " " +
                    String2.quoteParameterIfNeeded(fileType)         + " " +
                    String2.quoteParameterIfNeeded(digestType);

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

                            //write the file digest info
                            String digest = String2.fileDigest(
                                digestType, archiveDataDir + resultName);
                            if (bagitMode) {
                                manifestFileWriter.write(
                                    digest + "  data/" + resultName + "\n");
                            } else {
                                error = String2.writeToFile(
                                    archiveDataDir + resultName + digestExtension, 
                                    digest + "  " + resultName + "\n", 
                                    textFileEncoding);
                                if (error.length() > 0)
                                    throw new RuntimeException(error);
                            }
                           
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
                    combos.readFlatNc(archiveDir + resultName, null, 0); //standardizeWhat=0
                    File2.delete(     archiveDir + resultName);
                    int nComboRows = combos.nRows();
                    int nComboCols = combos.nColumns();
                    boolean isString[] = new boolean[nComboCols];
                    for (int col = 0; col < nComboCols; col++) 
                        isString[col] = combos.getColumn(col) instanceof StringArray;

                    //write the data files to archiveDataDir
                    for (int row = 0; row < nComboRows; row++) {
                        //make directory tree from nComboCols-1 
                        StringBuilder tDir   = new StringBuilder();
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
                        String fullDir = archiveDataDir + tDir;

                        //write the file and the .md5 file
                        String2.log("writing data file for combo #" + row + "\n" +
                            "  fileName=" + fullDir + fileName + fileType + "\n" +
                            "  tQuery=" + tQuery);
                        File2.makeDirectory(fullDir);
                        if (!dryRun) {
                            try {
                                resultName = eddTable.makeNewFileForDapQuery(null, null, 
                                    tQuery.toString(), fullDir, fileName, fileType); 
                                nDataFilesCreated++;

                                //write the file digest info
                                String digest = String2.fileDigest(
                                    digestType, fullDir + resultName);
                                if (bagitMode) {
                                    manifestFileWriter.write(
                                        digest + "  data/" + tDir + resultName + "\n");
                                } else {
                                    error = String2.writeToFile(
                                        fullDir + resultName + digestExtension, 
                                        digest + "  " + resultName + "\n", 
                                        textFileEncoding);
                                    if (error.length() > 0)
                                        throw new RuntimeException(error);
                                }
                           
                            } catch (Exception e) {
                                String2.log("ERROR #" + nErrors++ + "\n" +
                                    MustBe.throwableToString(e));
                            }
                        }
                    }
                }
            }

            if (bagitMode) {
                //close manifestFileWriter
                manifestFileWriter.close(); 
                manifestFileWriter = null;

                //create required bagit.txt
                Writer tw = String2.getBufferedOutputStreamWriterUtf8(
                    new FileOutputStream(archiveDir + "bagit.txt"));            
                try {
                    tw.write(
                        "BagIt-Version: 0.97\n" +
                        "Tag-File-Character-Encoding: UTF-8\n");
                } finally {
                    tw.close();
                }

                //create optional bag-info.txt
                tw = String2.getBufferedOutputStreamWriterUtf8(
                    new FileOutputStream(archiveDir + "bag-info.txt"));            
                try {
                    tw.write(
                        "Contact-Email: " + contactEmail + "\n" +
                        "Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
                        aadSettings);
                } finally {
                    tw.close();
                }

                //create optional tagmanifest-md5.txt
                tw = String2.getBufferedOutputStreamWriterUtf8(
                    new FileOutputStream(archiveDir + "tagmanifest-" + digestExtension1 + ".txt"));            
                try {
                    tw.write(
                        String2.fileDigest(digestType, archiveDir + "bag-info.txt")                + 
                            "  bag-info.txt\n" +
                        String2.fileDigest(digestType, archiveDir + "bagit.txt")                   + 
                            "  bagit.txt\n" +
                        String2.fileDigest(digestType, archiveDir + "manifest-" + digestExtension1 + ".txt") + 
                            "  manifest-" + digestExtension1 + ".txt\n");
                } finally {
                    tw.close();
                }
            }

            //make the zip or .tgz file
            String2.log("\n*** making " + tgzName);
            Math2.sleep(3000);  //take a deep breath, let file system settle down
            if (compression.equals("zip"))
                SSR.zipADirectory(archiveDir, 30 * 60); //timeoutSeconds: 30 minutes
            else FileVisitorDNLS.makeTgz(archiveDir, ".*", true, ".*", tgzName); 

            //make the .md5.txt file of the tgzName
            String2.log("\n*** making " + tgzName + digestExtension + ".txt");
            error = String2.writeToFile(  tgzName + digestExtension + ".txt", 
                String2.fileDigest(digestType, tgzName) + 
                    "  " + File2.getNameAndExtension(tgzName) + "\n",
                textFileEncoding);
            if (error.length() > 0)
                throw new RuntimeException(error);

            //make the .listOfFiles.txt of the tgzName
            if (!bagitMode) {
                String2.log("\n*** making " + tgzName + ".listOfFiles.txt");
                error = String2.writeToFile(  tgzName + ".listOfFiles.txt", 
                    FileVisitorDNLS.oneStepToString(archiveDir, ".*", true, ".*"),
                    textFileEncoding);
                if (error.length() > 0)
                    throw new RuntimeException(error);
            }

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
                " time=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - startTime) + "\n");

        } catch (Throwable t) {

            if (manifestFileWriter != null) {
                try {
                    manifestFileWriter.close(); 
                    File2.delete(manifestFullFileName);
                } catch (Throwable mft) {
                    String2.log(
                        "ERROR while closing manifestFile=" + manifestFullFileName + ":\n" + 
                        MustBe.throwableToString(mft));
                }
            }
                    
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
                " time=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - startTime) + "\n");
            throw t;
        }
        String2.flushLog();
        String2.returnLoggingToSystemOut();
        return tgzName;
    }

    public static void testOriginalNcCF() throws Throwable {
        String2.log("*** ArchiveADataset.testOriginalNcCF()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "original",
            "tar.gz",
            "bob.simons@noaa.gov",
            "cwwcNDBCMet", 
            "default", //all data vars
            "&station=~\"3.*\"", // &station=~"3.*"
            "nothing", //should be station, but use "nothing" to test save as points
            "default", //.ncCFMA
            "MD5"});   
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String ra[] = String2.readFromFile(targzName + ".listOfFiles.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        String results = ra[1];
        String expected = 
"cwwcNDBCMet.das                                                  " + today + "T.{8}Z         1....\n" +
"cwwcNDBCMet.dds                                                  " + today + "T.{8}Z           3..\n" +
"READ_ME.txt                                                      " + today + "T.{8}Z           3..\n" +
"data/\n" +
"  cwwcNDBCMet.nc                                                 " + today + "T.{8}Z      1.......\n" +
"  cwwcNDBCMet.nc.md5                                             " + today + "T.{8}Z            49\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external ...tar.gz.md5.txt
        ra = String2.readFromFile(targzName + ".md5.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{32}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
       
       
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testBagItNcCF() throws Throwable {
        String2.log("*** ArchiveADataset.testBagItNcCF()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "BagIt",
            "default", //tar.gz
            "bob.simons@noaa.gov",
            "cwwcNDBCMet", 
            "default", //all data vars
            "&station=~\"3.*\"", // &station=~"3.*"
            "nothing", //should be station, but use "nothing" as test of ncCFMA
            ".ncCF", //default is .ncCFMA
            "MD5"});   
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        //decompress and look at contents 
        SSR.windowsDecompressTargz(targzName, false, 5); //timeout minutes
        String tempDir = targzName.substring(0, targzName.length() - 7) + "/";
        int tempDirLen = tempDir.length();
        Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", 
            true, ".*", "");
        table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
        table.removeColumn(FileVisitorDNLS.NAME);
        String results = table.dataToString();
        String expected = 
"url,size\n" +
"bag-info.txt,4..\n" +
"bagit.txt,55\n" +
"manifest-md5.txt,54\n" +
"tagmanifest-md5.txt,142\n" +
"data/cwwcNDBCMet.nc,1.......\n";  //will change periodically
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at manifest
        String ra[] = String2.readFromFile(tempDir + "manifest-md5.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{32}  data/cwwcNDBCMet.nc\n";   //2017-03-07 actual md5 verified by hand
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at bagit.txt
        ra = String2.readFromFile(tempDir + "bagit.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"BagIt-Version: 0.97\n" +
"Tag-File-Character-Encoding: UTF-8\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional bag-info.txt
        ra = String2.readFromFile(tempDir + "bag-info.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"Contact-Email: bob.simons@noaa.gov\n" +
"Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
"ArchiveADataset_container_type: BagIt\n" +
"ArchiveADataset_compression: tar.gz\n" +
"ArchiveADataset_contact_email: bob.simons@noaa.gov\n" +
"ArchiveADataset_ERDDAP_datasetID: cwwcNDBCMet\n" +
"ArchiveADataset_data_variables: \n" +
"ArchiveADataset_extra_constraints: &station=~\"3.*\"\n" +
"ArchiveADataset_subset_by: \n" +
"ArchiveADataset_data_file_type: .ncCF\n" +
"ArchiveADataset_digest_type: MD5\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional tagmanifest-md5.txt
        ra = String2.readFromFile(tempDir + "tagmanifest-md5.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected =       //2017-03-07 actual md5's verified by hand
"[0-9a-f]{32}  bag-info.txt\n" +  
"[0-9a-f]{32}  bagit.txt\n" +
"[0-9a-f]{32}  manifest-md5.txt\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external cwwcNDBCMet_20170307183959Z.tar.gz.md5.txt
        ra = String2.readFromFile(targzName + ".md5.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = //2017-03-07 actual md5 verified by hand
"[0-9a-f]{32}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        

        //String2.pressEnterToContinue("\n"); 
    }

    /** A test of NCEI-preferences */
    public static void testBagItNcCFMA() throws Throwable {
        String2.log("*** ArchiveADataset.testBagItNcCFMA()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "BagIt",
            "tar.gz", 
            "bob.simons@noaa.gov",
            "cwwcNDBCMet", 
            "default", //all data vars
            "&station=~\"3.*\"", // &station=~"3.*"
            "", //should be station, but use "nothing" as test of ncCFMA
            ".ncCFMA", 
            "SHA-256"});   
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        //decompress and look at contents 
        SSR.windowsDecompressTargz(targzName, false, 5); //timeout minutes
        String tempDir = targzName.substring(0, targzName.length() - 7) + "/";
        int tempDirLen = tempDir.length();
        Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", 
            true, ".*", "");
        table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
        table.removeColumn(FileVisitorDNLS.NAME);
        String results = table.dataToString();
        String expected = 
"url,size\n" +
"bag-info.txt,4..\n" +
"bagit.txt,55\n" +
"manifest-sha256.txt,86\n" +
"tagmanifest-sha256.txt,2..\n" +
"data/cwwcNDBCMet.nc,4.......\n";  //will change periodically
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at manifest
        String ra[] = String2.readFromFile(tempDir + "manifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  data/cwwcNDBCMet.nc\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at bagit.txt
        ra = String2.readFromFile(tempDir + "bagit.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"BagIt-Version: 0.97\n" +
"Tag-File-Character-Encoding: UTF-8\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional bag-info.txt
        ra = String2.readFromFile(tempDir + "bag-info.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"Contact-Email: bob.simons@noaa.gov\n" +
"Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
"ArchiveADataset_container_type: BagIt\n" +
"ArchiveADataset_compression: tar.gz\n" +
"ArchiveADataset_contact_email: bob.simons@noaa.gov\n" +
"ArchiveADataset_ERDDAP_datasetID: cwwcNDBCMet\n" +
"ArchiveADataset_data_variables: \n" +
"ArchiveADataset_extra_constraints: &station=~\"3.*\"\n" +
"ArchiveADataset_subset_by: \n" +
"ArchiveADataset_data_file_type: .ncCFMA\n" +
"ArchiveADataset_digest_type: SHA-256\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional tagmanifest-sha256.txt
        ra = String2.readFromFile(tempDir + "tagmanifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected =       //2017-03-07 actual sha256's verified by hand
"[0-9a-f]{64}  bag-info.txt\n" +  
"[0-9a-f]{64}  bagit.txt\n" +
"[0-9a-f]{64}  manifest-sha256.txt\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external cwwcNDBCMet_20170307183959Z.tar.gz.sha256.txt
        ra = String2.readFromFile(targzName + ".sha256.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        

        //String2.pressEnterToContinue("\n"); 
    }

    public static void testOriginalTrajectoryProfile() throws Throwable {
        String2.log("*** ArchiveADataset.testOriginalTrajectoryProfile()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            //"-verbose",  //verbose is really verbose for this test
            "original", 
            "tar.gz",
            "bob.simons@noaa.gov",
            "scrippsGliders", 
            "default", //all data vars
            // &trajectory=~"sp05.*"&time>=2015-01-01&time<=2015-01-05
            "&trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05", 
            "default", "default", //trajectory, .ncCFMA
            "default"}); //SHA-256
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String ra[] = String2.readFromFile(targzName + ".listOfFiles.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        String results = ra[1];
        String expected = 
"READ_ME.txt                                                      " + today + "T.{8}Z           4..\n" +
"scrippsGliders.das                                               " + today + "T.{8}Z         14...\n" +
"scrippsGliders.dds                                               " + today + "T.{8}Z           7..\n" +
"data/\n" +
"  sp051-20141112.nc                                              " + today + "T.{8}Z        1.....\n" +
"  sp051-20141112.nc.sha256                                       " + today + "T.{8}Z            84\n" +
"  sp052-20140814.nc                                              " + today + "T.{8}Z        4.....\n" +
"  sp052-20140814.nc.sha256                                       " + today + "T.{8}Z            84\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at external ...tar.gz.sha256.txt
        ra = String2.readFromFile(targzName + ".sha256.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
       
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testBagItTrajectoryProfile() throws Throwable {
        String2.log("*** ArchiveADataset.testBagItTrajectoryProfile()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            //"-verbose",  //verbose is really verbose for this test
            "bagit", 
            "zip",
            "bob.simons@noaa.gov",
            "scrippsGliders", 
            "default", //all data vars
            // &trajectory=~"sp05.*"&time>=2015-01-01&time<=2015-01-05
            "&trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05", 
            "default", "default", //trajectory, .ncCFMA
            "default"}); //SHA-256
        Test.ensureTrue(targzName.endsWith(".zip"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        //decompress and look at contents 
        SSR.unzipADirectory(targzName, 60, null); //timeoutSeconds
        String tempDir = targzName.substring(0, targzName.length() - 4) + "/";
        int tempDirLen = tempDir.length();
        Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", 
            true, ".*", "");
        table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
        table.removeColumn(FileVisitorDNLS.NAME);
        String results = table.dataToString();
        String expected = 
"url,size\n" +
"bag-info.txt,4..\n" +
"bagit.txt,55\n" +
"manifest-sha256.txt,178\n" +
"tagmanifest-sha256.txt,241\n" +
"data/sp051-20141112.nc,148...\n" +
"data/sp052-20140814.nc,499...\n";  //will change periodically
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at manifest
        String ra[] = String2.readFromFile(tempDir + "manifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  data/sp051-20141112.nc\n" +
"[0-9a-f]{64}  data/sp052-20140814.nc\n"; 
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at bagit.txt
        ra = String2.readFromFile(tempDir + "bagit.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"BagIt-Version: 0.97\n" +
"Tag-File-Character-Encoding: UTF-8\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional bag-info.txt
        ra = String2.readFromFile(tempDir + "bag-info.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"Contact-Email: bob.simons@noaa.gov\n" +
"Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
"ArchiveADataset_container_type: bagit\n" +
"ArchiveADataset_compression: zip\n" +
"ArchiveADataset_contact_email: bob.simons@noaa.gov\n" +
"ArchiveADataset_ERDDAP_datasetID: scrippsGliders\n" +
"ArchiveADataset_data_variables: \n" +
"ArchiveADataset_extra_constraints: &trajectory=~\"sp05.*\"&time>=2015-01-01&time<=2015-01-05\n" +
"ArchiveADataset_subset_by: trajectory\n" +
"ArchiveADataset_data_file_type: .ncCFMA\n" +
"ArchiveADataset_digest_type: SHA-256\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional tagmanifest-sha256.txt
        ra = String2.readFromFile(tempDir + "tagmanifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected =       
"[0-9a-f]{64}  bag-info.txt\n" +  
"[0-9a-f]{64}  bagit.txt\n" +
"[0-9a-f]{64}  manifest-sha256.txt\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external cwwcNDBCMet_20170307183959Z.tar.gz.sha256.txt
        ra = String2.readFromFile(targzName + ".sha256.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testOriginalGridAll() throws Throwable {
        String2.log("*** ArchiveADataset.testOriginalGridAll()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "original", 
            "tar.gz",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "default",  //constraintsString
            "SHA-256"}); //SHA-256
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String ra[] = String2.readFromFile(targzName + ".listOfFiles.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        String results = ra[1];
        String expected = 
"erdVHNchla8day.das                                               " + today + "T.{8}Z          6...\n" +
"erdVHNchla8day.dds                                               " + today + "T.{8}Z           438\n" +
"READ_ME.txt                                                      " + today + "T.{8}Z           3..\n" +
"data/\n" +
"  20150301000000Z.nc                                             " + today + "T.{8}Z     44784....\n" +
"  20150301000000Z.nc.sha256                                      " + today + "T.{8}Z            85\n" +
"  20150302000000Z.nc                                             " + today + "T.{8}Z     44784....\n" +
"  20150302000000Z.nc.sha256                                      " + today + "T.{8}Z            85\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at external ...tar.gz.sha256.txt
        ra = String2.readFromFile(targzName + ".sha256.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
       
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testBagItGridAll() throws Throwable {
        String2.log("*** ArchiveADataset.testBagItGridAll()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "BagIt", 
            "ZIP",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "default",  //constraintsString
            "SHA-256"}); //SHA-256
        Test.ensureTrue(targzName.endsWith(".zip"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        //decompress and look at contents 
        SSR.unzipADirectory(targzName, 60, null); //timeoutSeconds
        String tempDir = targzName.substring(0, targzName.length() - 4) + "/";
        int tempDirLen = tempDir.length();
        Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", 
            true, ".*", "");
        table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
        table.removeColumn(FileVisitorDNLS.NAME);
        String results = table.dataToString();
        String expected = 
"url,size\n" +
"bag-info.txt,40.\n" +
"bagit.txt,55\n" +
"manifest-sha256.txt,180\n" +
"tagmanifest-sha256.txt,241\n" +
"data/20150301000000Z.nc,447840...\n" +
"data/20150302000000Z.nc,447840...\n";  //will change periodically
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at manifest
        String ra[] = String2.readFromFile(tempDir + "manifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  data/20150301000000Z.nc\n" +
"[0-9a-f]{64}  data/20150302000000Z.nc\n";   
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at bagit.txt
        ra = String2.readFromFile(tempDir + "bagit.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"BagIt-Version: 0.97\n" +
"Tag-File-Character-Encoding: UTF-8\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional bag-info.txt
        ra = String2.readFromFile(tempDir + "bag-info.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"Contact-Email: bob.simons@noaa.gov\n" +
"Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
"ArchiveADataset_container_type: BagIt\n" +
"ArchiveADataset_compression: zip\n" +
"ArchiveADataset_contact_email: bob.simons@noaa.gov\n" +
"ArchiveADataset_ERDDAP_datasetID: erdVHNchla8day\n" +
"ArchiveADataset_data_variables: \n" +
"ArchiveADataset_constraints: \\[\\(2015-03-01T00:00:00Z\\):\\(2015-03-02T00:00:00Z\\)\\]\\[\\]\\[\\]\\[\\]\n" +
"ArchiveADataset_digest_type: SHA-256\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional tagmanifest-sha256.txt
        ra = String2.readFromFile(tempDir + "tagmanifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  bag-info.txt\n" +  
"[0-9a-f]{64}  bagit.txt\n" +
"[0-9a-f]{64}  manifest-sha256.txt\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external cwwcNDBCMet_20170307183959Z.tar.gz.sha256.txt
        ra = String2.readFromFile(targzName + ".sha256.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testOriginalGridSubset() throws Throwable {
        String2.log("*** ArchiveADataset.testOriginalGridSubset()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "original", 
            "tar.gz",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "[(2015-03-02T00:00:00Z)][][][]",  //constraintsString
            "SHA-1"});
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String ra[] = String2.readFromFile(targzName + ".listOfFiles.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        String results = ra[1];
        String expected = 
"erdVHNchla8day.das                                               " + today + "T.{8}Z          6...\n" +
"erdVHNchla8day.dds                                               " + today + "T.{8}Z           4..\n" +
"READ_ME.txt                                                      " + today + "T.{8}Z           3..\n" +
"data/\n" +
"  20150302000000Z.nc                                             " + today + "T.{8}Z     44784....\n" +
"  20150302000000Z.nc.sha1                                        " + today + "T.{8}Z            61\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at external ...tar.gz.sha1.txt
        ra = String2.readFromFile(targzName + ".sha1.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{40}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
       
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testBagItGridSubset() throws Throwable {
        String2.log("*** ArchiveADataset.testBagItGridSubset()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "BagIt", 
            "zip",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "[(2015-03-02T00:00:00Z)][][][]",  //constraintsString
            "SHA-1"});
        Test.ensureTrue(targzName.endsWith(".zip"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(5000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(5000);

        //decompress and look at contents 
        SSR.unzipADirectory(targzName, 60, null); //timeoutSeconds
        String tempDir = targzName.substring(0, targzName.length() - 4) + "/";
        int tempDirLen = tempDir.length();
        Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", 
            true, ".*", "");
        table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
        table.removeColumn(FileVisitorDNLS.NAME);
        String results = table.dataToString();
        String expected = 
"url,size\n" +
"bag-info.txt,3..\n" +
"bagit.txt,55\n" +
"manifest-sha1.txt,66\n" +
"tagmanifest-sha1.txt,167\n" +
"data/20150302000000Z.nc,447840...\n";  //will change periodically
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at manifest
        String ra[] = String2.readFromFile(tempDir + "manifest-sha1.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{40}  data/20150302000000Z.nc\n";   
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at bagit.txt
        ra = String2.readFromFile(tempDir + "bagit.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"BagIt-Version: 0.97\n" +
"Tag-File-Character-Encoding: UTF-8\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional bag-info.txt
        ra = String2.readFromFile(tempDir + "bag-info.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"Contact-Email: bob.simons@noaa.gov\n" +
"Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
"ArchiveADataset_container_type: BagIt\n" +
"ArchiveADataset_compression: zip\n" +
"ArchiveADataset_contact_email: bob.simons@noaa.gov\n" +
"ArchiveADataset_ERDDAP_datasetID: erdVHNchla8day\n" +
"ArchiveADataset_data_variables: \n" +
"ArchiveADataset_constraints: \\[\\(2015-03-02T00:00:00Z\\)\\]\\[\\]\\[\\]\\[\\]\n" +
"ArchiveADataset_digest_type: SHA-1\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional tagmanifest-sha1.txt
        ra = String2.readFromFile(tempDir + "tagmanifest-sha1.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected =       
"[0-9a-f]{40}  bag-info.txt\n" +  
"[0-9a-f]{40}  bagit.txt\n" +
"[0-9a-f]{40}  manifest-sha1.txt\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external ....tar.gz.sha1.txt
        ra = String2.readFromFile(targzName + ".sha1.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{40}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        
        //String2.pressEnterToContinue("\n"); 
    }

    public static void testBagItGridSubset2() throws Throwable {
        String2.log("*** ArchiveADataset.testBagItGridSubset2()");

        //make the targz
        String targzName = (new ArchiveADataset()).doIt(new String[]{
            "-verbose",
            "BagIt", 
            "tar.gz",
            "bob.simons@noaa.gov",
            "erdVHNchla8day", //datasetID
            "default",  //dataVarsCSV
            "[(2015-03-01T00:00:00Z):(2015-03-02T00:00:00Z)][][][]",  //constraintsString
            "SHA-256"});
        Test.ensureTrue(targzName.endsWith(".tar.gz"), "targzName=" + targzName);

        //display it (in 7zip)
        Math2.sleep(10000);
        SSR.displayInBrowser("file://" + targzName); 
        Math2.sleep(10000);

        //decompress and look at contents 
        SSR.windowsDecompressTargz(targzName, false, 20); //timeout minutes
        String tempDir = targzName.substring(0, targzName.length() - 7) + "/";
        int tempDirLen = tempDir.length();
        Table table = FileVisitorDNLS.oneStepWithUrlsNotDirs(tempDir, ".*", 
            true, ".*", "");
        table.removeColumn(FileVisitorDNLS.LASTMODIFIED);
        table.removeColumn(FileVisitorDNLS.NAME);
        String results = table.dataToString();
        String expected = 
"url,size\n" +
"bag-info.txt,4..\n" +
"bagit.txt,55\n" +
"manifest-sha256.txt,1..\n" +
"tagmanifest-sha256.txt,2..\n" +
"data/20150301000000Z.nc,447......\n" +  //will change periodically
"data/20150302000000Z.nc,447......\n";   //will change periodically
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at manifest
        String ra[] = String2.readFromFile(tempDir + "manifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  data/20150301000000Z.nc\n" +
"[0-9a-f]{64}  data/20150302000000Z.nc\n";   
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at bagit.txt
        ra = String2.readFromFile(tempDir + "bagit.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"BagIt-Version: 0.97\n" +
"Tag-File-Character-Encoding: UTF-8\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional bag-info.txt
        ra = String2.readFromFile(tempDir + "bag-info.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"Contact-Email: bob.simons@noaa.gov\n" +
"Created_By: ArchiveADataset in ERDDAP v" + EDStatic.erddapVersion + "\n" +
"ArchiveADataset_container_type: BagIt\n" +
"ArchiveADataset_compression: tar.gz\n" +
"ArchiveADataset_contact_email: bob.simons@noaa.gov\n" +
"ArchiveADataset_ERDDAP_datasetID: erdVHNchla8day\n" +
"ArchiveADataset_data_variables: \n" +
"ArchiveADataset_constraints: \\[\\(2015-03-01T00:00:00Z\\):\\(2015-03-02T00:00:00Z\\)\\]\\[\\]\\[\\]\\[\\]\n" +
"ArchiveADataset_digest_type: SHA-256\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        //look at optional tagmanifest-sha256.txt
        ra = String2.readFromFile(tempDir + "tagmanifest-sha256.txt", String2.UTF_8);
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected =       
"[0-9a-f]{64}  bag-info.txt\n" +  
"[0-9a-f]{64}  bagit.txt\n" +
"[0-9a-f]{64}  manifest-sha256.txt\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);

        //look at external ....tar.gz.sha256.txt
        ra = String2.readFromFile(targzName + ".sha256.txt");
        Test.ensureEqual(ra[0], "", "ra[0]=" + ra[0]);
        results = ra[1];
        expected = 
"[0-9a-f]{64}  " + File2.getNameAndExtension(targzName) + "\n";
        Test.repeatedlyTestLinesMatch(results, expected, "results=\n" + results);
        
        
        //String2.pressEnterToContinue("\n"); 
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
            lastTest = interactive? -1 : 9;
        String msg = "\n^^^ ArchiveADataset.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0 && doSlowTestsToo) testOriginalNcCF();
                    if (test ==  1 && doSlowTestsToo) testOriginalTrajectoryProfile();
                    if (test ==  2 && doSlowTestsToo) testOriginalGridAll();
                    if (test ==  3 && doSlowTestsToo) testOriginalGridSubset(); 

                    if (test ==  4 && doSlowTestsToo) testBagItNcCF();
                    if (test ==  5 && doSlowTestsToo) testBagItTrajectoryProfile();
                    if (test ==  6 && doSlowTestsToo) testBagItGridAll();
                    if (test ==  7 && doSlowTestsToo) testBagItGridSubset(); 
                    if (test ==  8 && doSlowTestsToo) testBagItNcCFMA();       //w NCEI preferences
                    if (test ==  9 && doSlowTestsToo) testBagItGridSubset2();  //w NCEI preferences
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