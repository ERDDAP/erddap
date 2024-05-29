/* 
 * CenterGrids Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import java.io.File;
import java.util.HashSet;

/**
 * This class is designed to be a stand-alone program to 
 * duplicate a directory and all its subdirectories
 * and .nc files (using GridSaveAs to revise the file and center the time
 * when the file is copied to the new directory). 
 * It uses info in the gov/noaa/pfel/coastwatch/DataSet.properties file.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-01-22
 */
public class DoubleCenterGrids {


    /**
     * This class is designed to be a stand-alone program to 
     * duplicate a directory and all its subdirectories
     * and .nc files (using GridSaveAs to revise the file by centering the time
     * AND converting lat, lon, altitude information to doubles 
     * when the file is copied to the new directory)
     * It uses information in the gov/noaa/pfel/coastwatch/DataSet.properties file.
     *
     * <p>A log file (CenterGrids.log) will be created in the same directory
     * as the "classes" directory. A list of files which failed to convert
     * (e.g., invalid .nc file) are printed at the end.
     * If the log gets bigger than 10 MB, it is closed and renamed to 
     * CenterGrids.log.previous, and CenterGrids.log is restarted.
     *
     * <p>There need to be two or threee parameters in this order:
     * <ul>
     * <li>'oldEndTimeDir' the directory with old, uncentered .nc files,
     *    e.g., /u00/satellite/avhrr_hrpt/ .
     *    Use the actual directory name, not the symbolic link name.
     * <li>'newCenteredTimeDir' the directory (which may or may not already exist)
     *    which will be made to mimic oldEndTimeDir
     *    (except that the .nc files will have centered times),
     *    e.g., /u00/satellite/centeredavhrr_hrpt .
     *    Use the actual directory name, not the symbolic link name.
     *    If a given .nc file already exists, this won't convert it again.
     * <li>-fast   If present, this program runs as fast as possible.
     *    If absent, the program will sleep between files and will sleep
     *    periodically for a whole minute.
     * </ul>
     *
     * @throws Exception if trouble (e.g., attempt to create a directory fails).
     *    Individual files that don't get converted (e.g., invalid .nc file)
     *    don't throw Exception and are listed at the end.
     */
    public static void main(String args[]) throws Exception {

        //setup commons logging
        String2.setupCommonsLogging(-1);

        //set up the log file
        String logDir = File2.getClassPath(); //with / separator and / at the end
        logDir = logDir.substring(0, logDir.length() - 1); //remove last "/"
        logDir = File2.getDirectory(logDir); //remove "classes"
        String2.setupLog(true, false, //tLogToSystemOut, tLogToSystemErr,
            logDir + "DoubleCenterGrids.log", 
            true, String2.logFileDefaultMaxSize);  //append
        String2.log(
            "\n**************************************************************" +
            "\nDoubleCenterGrids " + Calendar2.getCurrentISODateTimeStringLocalTZ() +
            "\nlogFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());

        //proper number of arguments?
        if (args.length < 2) 
            Test.error(
                "This method duplicates a directory and all its subdirectories\n" +
                "and .nc files (using GridSaveAs to revise the file and center the time\n" +
                "when a file is copied to the new directory). \n" +
                "It uses infomation in the gov/noaa/pfel/coastwatch/DataSet.properties file.\n" +
                "\n" +
                "It must be called with two or three parameters, in this order:\n" +
                "* 'oldEndTimeDir' is the directory with old, uncentered .nc files,\n" +
                "    e.g., /u00/satellite/avhrr_hrpt/ .\n" +
                "    Use the actual directory name, not the symbolic link name.\n" +
                "    Use the actual directory name, not the symbolic link name.\n" +
                "* 'newCenteredTimeDir' is the directory (which may or may not already exist)\n" +
                "    which will be made to mimic oldEndTimeDir\n" +
                "    (except that the .nc files will have centered times),\n" +
                "    e.g., /u00/satellite/centeredavhrr_hrpt .\n" +
                "    Use the actual directory name, not the symbolic link name.\n" +
                "    If a given .nc file already exists, this won't convert it again.\n" +
                "* '-fast' If present, this program runs as fast as possible.\n" +
                "    If absent, the program will sleep between files and will sleep\n" +
                "    periodically for a whole minute.");

        long time = System.currentTimeMillis();

        //get fileNameUtility
        FileNameUtility fileNameUtility = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");
        
        //validate oldBaseDir and newBaseDir
        String oldBaseDir = args[0];
        String newBaseDir = args[1];
        oldBaseDir = File2.addSlash(oldBaseDir);
        newBaseDir = File2.addSlash(newBaseDir);
        String2.log("(oldBaseDir=" + oldBaseDir + " newBaseDir=" + newBaseDir + ")");

        boolean fast = false;
        if (args.length >= 3 && args[2].equals("-fast"))
            fast = true;

        if (oldBaseDir.equals(newBaseDir))
            Test.error(String2.ERROR + ": oldBaseDir can't equal newBaseDir.");

        //ensure oldBaseDir exists
        if (!File2.isDirectory(oldBaseDir))
            Test.error(String2.ERROR + ": oldBaseDir doesn't exist!");
        File oldBaseDirFile = new File(oldBaseDir);

        //ensure newBaseDir exists
        File newBaseDirFile = new File(newBaseDir);
        if (!File2.isDirectory(newBaseDir)) 
            Test.ensureTrue(newBaseDirFile.mkdirs(),  //mkdirs makes parents, too, if needed
                String2.ERROR + ": Unable to create " + newBaseDir); 

        //get all the directories and file names
        String [] oldFiles = RegexFilenameFilter.recursiveFullNameList(
            oldBaseDir, ".+\\.nc", true);
        String [] newFiles = RegexFilenameFilter.recursiveFullNameList(
            newBaseDir, ".+\\.nc", true);

        //store all the new directories and file names (relative to the newBaseDir) 
        //in a hashSet 
        //so I can quickly find out if they already exist
        int oldBaseDirLength = oldBaseDir.length();
        int newBaseDirLength = newBaseDir.length();
        HashSet<String> newFilesSet = new HashSet(Math2.roundToInt(1.4 * newFiles.length));
        for (int i = 0; i < newFiles.length; i++)
            newFilesSet.add(newFiles[i].substring(newBaseDirLength));

        //ensure all the old directories and files exist in the new dir
        long cumSleep = 0;
        int nFilesAlreadyExist = 0;
        int nFilesConverted = 0;
        StringArray filesFailed = new StringArray();
        for (int i = 0; i < oldFiles.length; i++) {
            String2.log("");
            String oldName = oldFiles[i];
            String relativeName = oldName.substring(oldBaseDirLength);
            String newName = newBaseDir + relativeName;
            //String2.log("oldName=" + oldName);
            //String2.log("newName=" + newName);

            //does file or directory already exist?
            if (newFilesSet.contains(relativeName)) {
                String2.log("Already exists: " + newName);
                nFilesAlreadyExist++;
                continue;
            }

            //make the new directory
            char lastChar = oldName.charAt(oldName.length() - 1);
            if (lastChar == '\\' || lastChar == '/') {
                String2.log("Creating directory: " + newName);
                File newDir = new File(newName);
                //throw exception if can't create directory
                Test.ensureTrue(newDir.mkdirs(), 
                    String2.ERROR + ": unable to create directory " + newName); 
                continue;
            }
                
            //make the new file
            try {

                //GridSaveAs
                //String2.log("Creating file: " + newName); //davesSaveAs says this info
                long tTime = System.currentTimeMillis();
                StringArray tsa = Grid.davesSaveAs(new String[]{oldName, newName}, fileNameUtility);
                if (tsa.size() > 0)
                    filesFailed.append(tsa);
                else nFilesConverted++;

                //sleep?
                if (!fast) {
                    long tSleep = Math.max(100, System.currentTimeMillis() - tTime); 
                    Math2.sleep(tSleep);
                    cumSleep += tSleep;
                    if (cumSleep >= 60000) {
                        String2.log("Sleeping for 1 minute...");
                        cumSleep -= 60000;
                        Math2.gc("DoubleCenterGrids", 60000); //sleep for 1 minute
                    }
                }

            } catch (Exception e) {
                String2.log(String2.ERROR + " in DoubleCenterGrids while converting\n" +
                    "oldName=" + oldName + "\n" + 
                    "newName=" + newName + "\n" + 
                    MustBe.throwableToString(e)); 
                filesFailed.add(oldName);
            }

        }

        //done
        String2.log(
            "\n*** List of files that couldn't be converted (e.g., invalid .nc file):\n" +
            filesFailed.toNewlineString() + 
            "\n*** DoubleCenterGrids done. nFilesAlreadyExist=" + nFilesAlreadyExist + 
                " nFilesConverted=" + nFilesConverted + 
            "\n  nFilesFailed=" + filesFailed.size() + 
            "  TOTAL TIME=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
        String2.returnLoggingToSystemOut();
    }

}
