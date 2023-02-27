/* 
 * CWDataBrowserReset Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.ema.*;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import java.util.Arrays;
import java.util.Vector;

/**
 * This gathers all the information needed for resetting the CWDataBrowser.
 * It can be run in a different thread. When finished, the data can 
 * be copied to CWDataBrowser quickly.
 *
 * <p>!!!!BEFORE DEPLOYMENT, CWDataBrowser.properties "other" needs
 * to be set to "baddog", "coastwatch", (or ?), so that proper 
 * dataDirectory and dataServer info are used.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 *
 * <p>Changes:
 * <ul>
 * </ul>
 * 
 */
public class CWDataBrowserReset extends EmaClass implements Runnable {

    boolean verbose;

    public String runError = ""; //set by run() if trouble
    public StringBuilder runInfo = new StringBuilder();

    String baseDataDirectory;
    String dataServer;
    boolean lookForAllUnusedDataFiles;
    String dataSetOptions[];
    String dataSetTitles[];
    String dataSetDirectories[];
    String dataSetRegexs[];
    int    dataSetRequests[];
    String activeDataSetOptions[];    
    String activeDataSetTitles[];    
    Vector activeDataSetContents; 
    String timePeriodOptions[];
    String timePeriodTitles[];
    String timePeriodDirectories[];
    int    timePeriodRequests[];
    String regionOptions[];
    String regionTitles[];
    String regionRegexs[];
    String regionCoordinates[];
    String getOptions[];
    String getTitles[];
    String getDirectories[];
    String getRegexs[];
    String getExtensions[];
    int fileCount[];

    
    /**
     * Constructor
     */
    public CWDataBrowserReset() {
        super("gov.noaa.pfel.coastwatch.CWDataBrowser"); //base name for properties file
        runInfo.append("\n" + String2.makeString('*', 80) +  
            "\nCWDataBrowserReset.constructor " + 
            Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n");
      
        //one time things
        verbose = classRB2.getBoolean("verbose", false);

        //get baseDataDirectory (for access within this program)
        baseDataDirectory = classRB2.getString("dataDirectory", null);

        //get dataServer (for web-based access to the data)
        dataServer = classRB2.getString("dataServer", null);

        //lookForAllUnusedDataFiles
        lookForAllUnusedDataFiles = classRB2.getBoolean("lookForAllUnusedDataFiles", false); 

        //get dataSet properties
        dataSetOptions     = classRB2.getString("dataSet.options", null).split("\f");
        dataSetTitles      = classRB2.getString("dataSet.title" , null).split("\f");
        dataSetDirectories = classRB2.getString("dataSet.directories", null).split("\f");
        dataSetRegexs      = classRB2.getString("dataSet.regexs", null).split("\f");
        if (dataSetRequests == null)
            dataSetRequests = new int[dataSetOptions.length];
        boolean trouble =
            dataSetOptions.length != (dataSetTitles.length-1) || //1 extra title (main)
            dataSetOptions.length != dataSetDirectories.length ||
            dataSetOptions.length != dataSetRegexs.length;
        if (verbose || trouble) {
            runInfo.append("baseDataDirectory: " + baseDataDirectory + "\n");
            runInfo.append("dataSetOptions: " + String2.toCSSVString(dataSetOptions) + "\n");
            runInfo.append("dataSetTitles: " + String2.toNewlineString(dataSetTitles) + "\n");
            runInfo.append("dataSetDirectories: " + String2.toCSSVString(dataSetDirectories) + "\n");
            runInfo.append("dataSetRegexs: " + String2.toCSSVString(dataSetRegexs) + "\n");
            if (trouble) 
                throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset " +
                    "nDataSetOptions " + dataSetOptions.length + 
                    " != nDataSetTitles " + dataSetTitles.length + 
                    " != nDataSetDirectories " + dataSetDirectories.length + 
                    " != nDataSetRegexs " + dataSetRegexs.length);
        }

        //get region properties
        regionOptions      = classRB2.getString("region.options", null).split("\f");
        regionTitles       = classRB2.getString("region.title", null).split("\f");
        regionRegexs       = classRB2.getString("region.regexs", null).split("\f");
        regionCoordinates  = classRB2.getString("region.coordinates", null).split("\f");
        trouble = 
            regionOptions.length != (regionTitles.length-1) || //1 extra title (main)
            regionOptions.length != regionRegexs.length ||
            regionOptions.length != regionCoordinates.length;
        if (verbose || trouble) {
            runInfo.append("regionOptions: " + String2.toCSSVString(regionOptions) + "\n");
            runInfo.append("regionTitles: " + String2.toNewlineString(regionTitles) + "\n");
            runInfo.append("regionRegexs: " + String2.toCSSVString(regionRegexs) + "\n");
            if (trouble) 
                throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset " +
                    "nRegionOptions " + regionOptions.length + 
                    " != nRegionTitles " + regionTitles.length + 
                    " != nRegionRegexs " + regionRegexs.length +
                    " != nRegionCoordinates " + regionCoordinates.length);
        }

        //get timePeriod properties
        timePeriodOptions     = classRB2.getString("timePeriod.options", null).split("\f");
        timePeriodTitles      = classRB2.getString("timePeriod.title", null).split("\f");
        timePeriodDirectories = classRB2.getString("timePeriod.directories", null).split("\f");
        timePeriodRequests    = new int[timePeriodOptions.length];
        trouble = 
            timePeriodOptions.length != (timePeriodTitles.length-1) || //1 extra title (main)
            timePeriodOptions.length != timePeriodDirectories.length;
        if (verbose || trouble) {
            runInfo.append("timePeriodOptions: " + String2.toCSSVString(timePeriodOptions) + "\n");
            runInfo.append("timePeriodTitles: " + String2.toNewlineString(timePeriodTitles) + "\n");
            runInfo.append("timePeriodDirectories: " + String2.toCSSVString(timePeriodDirectories) + "\n");
            if (trouble) 
                throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset " +
                    "nTimePeriodOptions " + timePeriodOptions.length + 
                    " != nTimePeriodTitles " + timePeriodTitles.length + " " +
                    " != nTimePeriodDirectories " + timePeriodDirectories.length);
        }

        //get 'get' properties
        //the first 'get' option must be .gif; a .gif file must be present before looking for others
        getOptions     = classRB2.getString("get.options", null).split("\f");
        getTitles      = classRB2.getString("get.title", null).split("\f");
        getDirectories = classRB2.getString("get.directories", null).split("\f");
        getRegexs      = classRB2.getString("get.regexs", null).split("\f");
        getExtensions  = classRB2.getString("get.extensions", null).split("\f");
        trouble = 
            getOptions.length != (getTitles.length-1) || //1 extra title (main)
            getOptions.length != getDirectories.length || 
            getOptions.length != getRegexs.length ||
            getOptions.length != getExtensions.length;
        if (verbose || trouble) {
            runInfo.append("getOptions: " + String2.toCSSVString(getOptions) + "\n");
            runInfo.append("getDirectories: " + String2.toCSSVString(getDirectories) + "\n");
            runInfo.append("getExtensions: " + String2.toCSSVString(getExtensions) + "\n");
            runInfo.append("getRegexs: " + String2.toCSSVString(getRegexs) + "\n");
            if (trouble) 
                throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset \n" +
                    "nGetOptions " + getOptions.length + 
                    " != nGetTitles " + getTitles.length + 
                    " != nGetDirectories " + getDirectories.length + 
                    " != nGetRegexs " + getRegexs.length +
                    " != nGetExtensions " + getExtensions.length);
        }
    }

    /**
     * Reset the file lists and other things.
     * When this is done, check if resetError.length() > 0, indicating
     * that the procedure failed and the results are not reliable.
     * Internal errors throw exceptions which are caught here and used to set resetError.
     */
    public void run() {
        try {
            long gifDirTime = 0;
            long otherDirTime = 0;
            long matchTime = 0;
            long tTime;
            long startTime = System.currentTimeMillis();
            runInfo.append("\n" + String2.makeString('*', 80) +  
                "\nCWDataBrowserReset.run " + 
                Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n");

            //find out what files are available and store in ActiveXxx vectors
            //for each dataset
            fileCount = new int[getOptions.length];
            int notMatched = 0;
            Vector tActiveDataSetOptions  = new Vector(); //holds active dataSet options
            Vector tActiveDataSetTitles   = new Vector(); //holds active dataSet titles
            Vector tActiveDataSetContents = new Vector(); //holds []'s with activeTimePeriodOptions+Contains for each dataset
            tActiveDataSetTitles.add(dataSetTitles[0]);  //always add the main title
            int nDataSets = dataSetOptions.length;
            for (int dataSetI = 0; dataSetI < nDataSets; dataSetI++) {
                Vector tActiveTimePeriodOptions  = new Vector();//holds active timePeriodOptions (for a dataset)
                Vector tActiveTimePeriodTitles   = new Vector();//holds active timePeriodTitles (for a dataset)
                Vector tActiveTimePeriodContents = new Vector();//holds []'s with activeRegionOptions+Contains for each timePeriod
                tActiveTimePeriodTitles.add(timePeriodTitles[0]); //always add the main title

                //for each timePeriod
                int nTimePeriods = timePeriodOptions.length;
                for (int timePeriodI = 0; timePeriodI < nTimePeriods; timePeriodI++) {
                    String dir = baseDataDirectory + dataSetDirectories[dataSetI] + "/" + 
                        timePeriodDirectories[timePeriodI];
                    Vector tActiveRegionOptions     = new Vector(); //holds active region options (for a dataSet and timePeriod)
                    Vector tActiveRegionTitles      = new Vector(); //holds active region titles (for a dataSet and timePeriod)
                    Vector tActiveRegionCoordinates = new Vector(); //holds active region coordinates (for a dataSet and timePeriod)
                    Vector tActiveRegionContents    = new Vector(); //holds []'s with gifs[]+getBits[] for each region
                    tActiveRegionTitles.add(regionTitles[0]); //always add the main title

                    //for each region
                    int nRegions = regionOptions.length;
                    for (int regionI = 0; regionI < nRegions; regionI++) {
                        String regex = dataSetRegexs[dataSetI] + regionRegexs[regionI];

                        //get the sorted list of gif files and remove the .gif extension
                        tTime = System.currentTimeMillis();
                        String tDir = dir + "/" + getDirectories[0];
                        String gifs[] = RegexFilenameFilter.list(tDir, regex + getRegexs[0]);
                        if (gifs == null) {
                            String2.log(String2.ERROR + " in CWDataBrowserReset #1: trouble with tDir=" + 
                                tDir + " and regex=" + regex + getRegexs[0] + ".");
                            gifs = new String[0];
                        }
                        String dates[] = new String[gifs.length];
                        int nGifs = gifs.length;
                        fileCount[0] += nGifs;
                        for (int gifI = 0; gifI < nGifs; gifI++) {
                            try {
                                gifs[gifI] = gifs[gifI].substring(0, gifs[gifI].length() - 4);
                                //yyyydddToIsoDate can throw exception
                                if (gifs[gifI].charAt(16) == 'h') 
                                    dates[gifI] = Calendar2.yyyydddToIsoDate(
                                        gifs[gifI].substring(2, 9)) + //date; 
                                        " " + gifs[gifI].substring(10, 12) + //hr
                                        ":" + gifs[gifI].substring(12, 14) + //mn
                                        ":" + gifs[gifI].substring(14, 16);  //sec
                                else //2nd date is end date
                                    dates[gifI] = Calendar2.yyyydddToIsoDate(
                                        gifs[gifI].substring(10, 17)); 
                            } catch (Exception e) {
                                gifs[gifI] = "";
                                dates[gifI] = "";
                            }
                            
                        }
                        gifDirTime += System.currentTimeMillis() - tTime;
                        //runInfo.append(("gifs found in " + tDir + "  " + regex + ": " + 
                        //    String2.toNewlineString(gifs) + "\n");

                        //if gifs exist, look for matching files of other types
                        //lookForAllUnusedDataFiles mode runs slower, but detects some files with no matching .gifs
                        if (lookForAllUnusedDataFiles || nGifs > 0) { 
                            //make the getBits array
                            int getBits[] = new int[gifs.length];
                            Arrays.fill(getBits, 1); //indicated that a gif version is available

                            //for each getOption (file type)
                            int nGetOptions = getOptions.length; //this is the unchanging list
                            for (int getI = 1; getI < nGetOptions; getI++) { //yes 1.., .gif already done
                                int bitValue = Math2.Two[getI];

                                //get the sorted list of files in the directory
                                tTime = System.currentTimeMillis();
                                tDir = dir + "/" + getDirectories[getI];
                                String files[] = RegexFilenameFilter.list(tDir, 
                                    regex + getRegexs[getI]);
                                if (files == null) {
                                    String2.log(String2.ERROR + " in CWDataBrowserReset #2: trouble with tDir=" + 
                                        tDir + " and regex=" + regex + getRegexs[getI] + ".");
                                    files = new String[0];
                                }
                                int nFiles = files.length;
                                otherDirTime += System.currentTimeMillis() - tTime;

                                //remove extension
                                for (int fileI = 0; fileI < nFiles; fileI++) {
                                    String s = files[fileI];
                                    files[fileI] = s.substring(0, s.indexOf('.'));
                                }
                                //runInfo.append(("files found in " + tDir + "  " + regex + getRegexs[getI] + "\n" +
                                //    String2.toNewlineString(files) + "\n");

                                //Look for non-gif files which have matching gif files.
                                //Since gif list and non-gif list are sorted,
                                //I can look for common files sort-of-like a merge sort.
                                tTime = System.currentTimeMillis();
                                int nMatched = 0;
                                int gifI = 0, fileI = 0;  
                                while (gifI < nGifs || fileI < nFiles) {
                                    String cGifName = gifI < nGifs? gifs[gifI] : "\uFFFF";
                                    String cFileName = fileI < nFiles? files[fileI] : "\uFFFF";
                                    int diff = cGifName.compareTo(cFileName);
                                    if (diff == 0) { //a match!
                                        getBits[gifI] |= bitValue;
                                        nMatched ++;
                                        gifI++;
                                        fileI++;
                                    } else if (diff < 0) { //no match for this gif
                                        gifI++;
                                    } else { //no match for this file
                                        runInfo.append("  No .gif file to match: " + 
                                            files[fileI] + getExtensions[getI] + "\n");
                                        fileI++;
                                    } 
                                }
                                fileCount[getI] += nMatched;
                                if (nMatched != nFiles) {
                                    notMatched += nFiles - nMatched;
                                    runInfo.append(String2.ERROR + ": " + 
                                        nFiles + " files match \"" + 
                                            tDir + "/" + regex + getRegexs[getI] + "\"\n" + 
                                        "  but only " + nMatched + 
                                            " matched a .gif file (see list above).\n");
                                }
                                matchTime += System.currentTimeMillis() - tTime;
                            }

                            //save region name, array of gifs, and parallel array of getBits
                            //later usage: activeRegions.get(i)
                            //  -> a region name and a list of its active gifs (and related files)
                            if (nGifs > 0) {
                                tActiveRegionOptions.add(regionOptions[regionI]);
                                tActiveRegionTitles.add(regionTitles[regionI + 1]); //+1 since 0=mainTitle
                                tActiveRegionCoordinates.add(regionCoordinates[regionI]);
                                tActiveRegionContents.add(new Object[]{gifs, dates, getBits});
                            }
                        } //end of if nGif > 0  

                    } //end of regions loop

                    //save timePeriod name and the regionVector
                    //later usage: activeTimePeriods.get(i) 
                    //  -> aTimePeriodName + a list of its activeRegions + 
                    if (!tActiveRegionOptions.isEmpty()) {
                        tActiveTimePeriodOptions.add(timePeriodOptions[timePeriodI]);
                        tActiveTimePeriodTitles.add(timePeriodTitles[timePeriodI + 1]); //+1 since 0=mainTitle
                        tActiveTimePeriodContents.add(new Object[] {
                            String2.toStringArray(tActiveRegionOptions.toArray()), 
                            String2.toStringArray(tActiveRegionTitles.toArray()), 
                            String2.toStringArray(tActiveRegionCoordinates.toArray()), 
                            tActiveRegionContents,
                            timePeriodDirectories[timePeriodI]});
                    }

                } //end of timePeriods loop

                //if there were activeTimePeriods, save 
                //later usage: activeDatasets.get(i) 
                //  -> a dataSet name and a list of its activeTimePeriods 
                if (!tActiveTimePeriodOptions.isEmpty()) {
                    tActiveDataSetOptions.add(dataSetOptions[dataSetI]);
                    tActiveDataSetTitles.add(dataSetTitles[dataSetI + 1]); //+1 since 0=mainTitle
                    tActiveDataSetContents.add(new Object[]{
                        String2.toStringArray(tActiveTimePeriodOptions.toArray()), 
                        String2.toStringArray(tActiveTimePeriodTitles.toArray()), 
                        tActiveTimePeriodContents, 
                        dataSetDirectories[dataSetI]});
                }
            } //end of dataSets loop
            if (notMatched > 0)
                runInfo.append(String2.ERROR + ": In total, " + notMatched + 
                    " files were found for which there was no matching .gif.\n");
            if (tActiveDataSetOptions.size() == 0)
                throw new RuntimeException(
                    String2.ERROR + ": no valid .gifs so no active DataSets!");
            activeDataSetOptions  = String2.toStringArray(tActiveDataSetOptions.toArray());
            activeDataSetTitles   = String2.toStringArray(tActiveDataSetTitles.toArray());
            activeDataSetContents = tActiveDataSetContents; 

            //print lots of useful information
            runInfo.append("Data files found which have matching .gif files:\n");
            for (int getI = 0; getI < getOptions.length; getI++)
                runInfo.append(String2.right("" + fileCount[getI], 8) + " " + 
                    getOptions[getI] + " files are available.\n");
            runInfo.append("  gifDirTIME=" + gifDirTime + ", otherDirTIME=" + otherDirTime +
                ", matchGifTIME=" + matchTime + "\n");
            runInfo.append("    dirTimes broken down: getTime = " + RegexFilenameFilter.getTime + 
                ", matchRegexTime = " + RegexFilenameFilter.matchTime + 
                ", sortTime = " + RegexFilenameFilter.sortTime + "\n");
            runInfo.append("  CWDataBrowserReset done. TOTAL TIME=" + (System.currentTimeMillis() - startTime) + " ms.\n");
        } catch (Exception e) {
            runError = MustBe.throwable("CWDataBrowserReset.run [" + String2.ERROR + "]", e);
            String2.log(runError);
        }
    }


}
