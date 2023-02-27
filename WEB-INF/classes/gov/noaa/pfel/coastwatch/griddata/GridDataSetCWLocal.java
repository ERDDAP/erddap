/* 
 * GridDataSetCWLocal Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.OneOf;
import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Vector;

// from netcdfAll-x.jar
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents a gridDataSet based on locally stored data files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-01
 */
public class GridDataSetCWLocal extends GridDataSetCW { 


    /** The active time period fileNames (String[]) related to an activeTimePeriodOption. 
     * The names are just the actual names (no directory info), 
     * so no point in making canonical with String2.canonical().
     */
    protected Vector activeTimePeriodFileNames = new Vector(); //set by the constructor
    protected String[] activeTimePeriodDirectories;
    
    protected String directory;      
    protected String fileNameRegex;  //e.g., AG.{15}_ssta   was dataSetFileInfo[DSFIRegex]
    protected String cacheDir;  //e.g., AG.{15}_ssta   was dataSetFileInfo[DSFIRegex]

    //the constructor also sets
    public long grdDirTime = 0;
    public int  localGrdFileCount = 0;

    /**
     * Don't use this -- it just fulfulls a Java requirement for extending this class.
     *
     * @throws Exception if trouble, e.g., no data
     */
    public GridDataSetCWLocal() throws Exception {  
    }

    /**
     * The constructor searches for available data and sets
     * activeTimePeriodOptions, activeTimePeriodTitles, activeTimePeriodTimes,
     * and activeTimePeriodFileNames.
     * This prints diagnostic info to String2.log.
     *
     * @param fileNameUtility
     * @param internalName  e.g., "LATssta"
     * @param localDataSetBaseDirectory
     * @param cacheDirectory a directory that can be used to cache files
     * @throws Exception if trouble, e.g., no data
     */
    public GridDataSetCWLocal(FileNameUtility fileNameUtility, String internalName,
            String localDataSetBaseDirectory, String cacheDirectory) 
        throws Exception {

        cacheDir = cacheDirectory;

        //set info from DataSet.properties
        dataSetPropertiesSetup(fileNameUtility, internalName);
        String dataSetFileInfo[] = String2.split(fileNameUtility.dataSetRB2().getString(
            internalName + "FileInfo",  null), '`');
        Test.ensureEqual(dataSetFileInfo.length, 3, 
            String2.ERROR + ": " + 
            internalName + "FileInfo in DataSet.properties must have 3 items.");
        directory     = dataSetFileInfo[OneOf.DSFIDirectory];  
        //(usually) add directory to localDataSetBaseDirectory
        if (directory.charAt(0) != '/') 
            directory = localDataSetBaseDirectory + directory;
        fileNameRegex = dataSetFileInfo[OneOf.DSFIRegex];      
        anomalyDataSet = dataSetFileInfo[OneOf.DSFIAnomalyDataSet];

        //set original timePeriod values
        String[] originalTimePeriodOptions = { //these must all be in TimePeriods.OPTIONS
            "1 observation", "1 day", 
            TimePeriods._25HOUR_OPTION, TimePeriods._33HOUR_OPTION,
            "3 day", "4 day", "5 day", "8 day", "10 day", "14 day", "monthly"};
        String[] originalTimePeriodDirectories = {
            "hday", "1day", "lday", "l33h", "3day", "4day", "5day", "8day", "10day", "14day", "mday"};  //note lday for 25hour
        //these match the shortNames in TimePeriods
        //note 25hour (to distinguish 25hour from 33 hour in customFileNames) 
        //String[] originalTimePeriodShortNames = {
        //    "1obs", "1day", "25hour", "33hour", "3day", "4day", "5day", "8day", "10day", "14day", "mday"};  
        //these match the system in TimePeriods
        //25hour is often a special case;  30 is translated to 1 month   
        int[] originalTimePeriodNHours = {
            0, 24, 25, 33, 3*24, 4*24, 5*24, 8*24, 10*24, 14*24, 30*24}; 
        String[] originalTimePeriodTitles = {
            "", //dummy main title
            singlePassTitle(),
            compositeTitle("a 1 day"),
            compositeTitle("a 25 hour"),
            compositeTitle("a 33 hour"),
            compositeTitle("a 3 day"),
            compositeTitle("a 4 day"),
            compositeTitle("a 5 day"),
            compositeTitle("an 8 day"),
            compositeTitle("a 10 day"),
            compositeTitle("a 14 day"),
            compositeTitle("a 1 month")};

        //create vectors to hold info for available data
        Vector tActiveTimePeriodOptions     = new Vector();//holds the active timePeriodOptions for this dataset
        Vector tActiveTimePeriodTitles      = new Vector();//holds the active timePeriodTitles for this dataset
        Vector tActiveTimePeriodNHours      = new Vector();//holds the active timePeriodNHours for this dataset
        Vector tActiveTimePeriodDirectories = new Vector();//holds the active timePeriodDirectories for this dataset
        Vector tActiveTimePeriodOpendapUrls = new Vector();//holds the active timePeriodOpendapUrls for this dataset
        tActiveTimePeriodTitles.add(""); //always add a blank main title

        //for each timePeriod
        long tTime = System.currentTimeMillis();
        int nTimePeriods = originalTimePeriodOptions.length;
        for (int timePeriodI = 0; timePeriodI < nTimePeriods; timePeriodI++) {
            String timePeriodInFileName = originalTimePeriodDirectories[timePeriodI];
            String dir = directory + timePeriodInFileName + "/grd";
            int expectedNHours = TimePeriods.getNHours(originalTimePeriodOptions[timePeriodI]);

            //get the sorted list of the grd.zip files
            String regex = fileNameRegex + ".*\\.grd\\.zip";
            String grds[] = RegexFilenameFilter.list(dir, regex);
            if (grds == null) {
                String2.log(String2.ERROR + " in GridDataSetCWLocal: trouble with dir=" + 
                    dir + " and regex=" + regex + ".");
                grds = new String[0];
            }
            int nGrds = grds.length;
            if (verbose)
                String2.log("\n* GridDataSetCWLocal constructor dir=" + dir + 
                    "\n  regex=" + fileNameRegex + 
                    ".*\\.grd\\.zip nfiles=" + nGrds +
                    (nGrds > 0? "\n  last grds=" + grds[nGrds-1] : ""));
            StringArray keepGrds = new StringArray();
            StringArray keepDates = new StringArray();
            for (int grdI = 0; grdI < nGrds; grdI++) {
                String tOption = "";
                try {
                    if (grds[grdI].charAt(16) == 'h') {
                        //hourly data AT2005060_044800h_sstn_westus.grd.zip
                        String yyyyddd = grds[grdI].substring(2, 9);
                        String isoDate = Calendar2.yyyydddToIsoDate(yyyyddd); //throws Exception if trouble
                        tOption = isoDate + //date
                            " " + grds[grdI].substring(10, 12) + //hr
                            ":" + grds[grdI].substring(12, 14) + //mn
                            ":" + grds[grdI].substring(14, 16);  //sec

                        //sanity check: it should be an 'h' date, shouldn't it?
                        if (expectedNHours == 0) {   
                            //tOption doesn't need to be modified
                        } else if (expectedNHours % 24 != 1) {
                            //handle 25hour and 33hour: fileName is endTime; center it
                            //file name has end hour (which is correct to nearest second (says Dave))
                            tOption = Calendar2.formatAsISODateTimeSpace(
                                Calendar2.isoDateTimeAdd(tOption, 
                                    -expectedNHours * 60 / 2, Calendar2.MINUTE)); 
                        } else {
                            Test.error(String2.ERROR + 
                                ": It shouldn't have been an 'h' type file name."); //FILE NAME SHOWN BELOW
                        }
                    } else { 
                        //2nd date is end date   e.g. AG2005152_2005181_tanm_westus.grd.zip
                        String endDateString = Calendar2.yyyydddToIsoDate(  //throws Exception if trouble
                            grds[grdI].substring(10, 17)); 

                        //do sanity checking: is the time period correct?
                        String beginTimeString = Calendar2.yyyydddToIsoDate(  //throws Exception if trouble
                            grds[grdI].substring(2, 9)); 

                        if (expectedNHours == 0 || expectedNHours % 24 != 0) {   //this handles 25hour and 33hour correctly
                            Test.error(String2.ERROR + 
                                ": It should have been an 'h' type file name."); //FILE NAME SHOWN BELOW
                        } else if (expectedNHours == 30*24) {
                            //monthly
                            GregorianCalendar tgc = Calendar2.parseISODateTimeZulu(endDateString); //throws Exception if trouble
                            if (!beginTimeString.substring(0, 8).equals(endDateString.substring(0,8))) {
                                Test.error(String2.ERROR + 
                                    ": monthly begin=" + beginTimeString + 
                                    " end=" + endDateString + " not same month"); //FILE NAME SHOWN BELOW
                            } else if (!beginTimeString.substring(8, 10).equals("01")) {
                                Test.error(String2.ERROR + " monthly begin date=" + 
                                    beginTimeString + " not '01'."); //FILE NAME SHOWN BELOW
                            } else if (tgc.getActualMaximum(Calendar2.DATE) != 
                                    tgc.get(Calendar2.DATE)) {
                                Test.error(String2.ERROR + " monthly end date=" + 
                                    endDateString + " not end of month."); //FILE NAME SHOWN BELOW
                            }
                        } else if (expectedNHours % 24 == 0 && expectedNHours >= 24 && 
                                expectedNHours < 30*24) {
                            //expectedNHours is >1 day but <month
                            int beginHours = Calendar2.isoStringToEpochHours(beginTimeString); //should be clean from above
                            int endHours   = Calendar2.isoStringToEpochHours(endDateString);   //should be clean from above
                            int nDays = (endHours - beginHours)/24 + 1;
                            if (nDays != expectedNHours/24) {
                                Test.error(String2.ERROR + " unexpected nDays (" + nDays + ")."); //FILE NAME SHOWN BELOW
                            }
                        } else {
                            Test.error(String2.ERROR + " in GridDataSetCWLocal constructor\n" +
                                "unexpected expectedNHours=" + expectedNHours + "."); //FILE NAME SHOWN BELOW
                        }

                        tOption = DataHelper.centerOfStartDateAndInclusiveEndDate(
                            beginTimeString, endDateString);
                    }

                } catch (Exception e) {
                    tOption = "";  //should be already
                    String2.log(e.toString());
                }


                //important for vector common dates: just keep valid dates
                if (tOption.length() > 0) {
                    //make sure it isn't a duplicate date (when 2 files for same date match regex)!
                    String previousOption = keepDates.size() > 0? keepDates.get(keepDates.size() - 1) : "";
                    if (tOption.equals(previousOption)) {
                        String msg = String2.ERROR + " in GridDataSetCWLocal constructor\n" +
                            "  (dir = " + dir + " regex=" + fileNameRegex + ")\n" +
                            "  Duplicate file entries:" + grds[grdI] + 
                            " and " + keepGrds.get(keepGrds.size() - 1);
                        String2.log(msg);
                    } else {
                        //keep this grd
                        keepGrds.add(String2.canonical(grds[grdI])); //canonical saves space if file in >1 browser
                        keepDates.add(tOption);  //GridDataSet.makeTimeOptionsCanonical will call String2.canonical()
                    }
                } else {
                    String2.log(String2.ERROR + " in GridDataSetCWLocal constructor\n" +
                        "  (dir = " + dir + " regex=" + fileNameRegex + ")\n" +
                        "  Trouble with grds[" + grdI + "]: date=" + grds[grdI]);
                }

            }

            //save the info
            if (keepDates.size() > 0) {

                //save the time period's info
                localGrdFileCount += nGrds;
                tActiveTimePeriodOptions.add(originalTimePeriodOptions[timePeriodI]);
                tActiveTimePeriodTitles.add(originalTimePeriodTitles[timePeriodI + 1]); //+1 since 0=mainTitle
                tActiveTimePeriodDirectories.add(originalTimePeriodDirectories[timePeriodI]);
                tActiveTimePeriodNHours.add("" + expectedNHours);
                tActiveTimePeriodOpendapUrls.add(null);
                activeTimePeriodTimes.add(keepDates.toArray());
                activeTimePeriodFileNames.add(keepGrds.toArray());
                if (verbose)
                    String2.log("  last date=" + keepDates.get(keepDates.size() - 1));

            }
        } //end of timePeriods loop

        grdDirTime += System.currentTimeMillis() - tTime;
        
        //convert vectors to string[]'s
        activeTimePeriodOptions     = String2.toStringArray(tActiveTimePeriodOptions.toArray());
        activeTimePeriodTitles      = String2.toStringArray(tActiveTimePeriodTitles.toArray());
        activeTimePeriodNHours      = String2.toIntArray(tActiveTimePeriodNHours.toArray());
        activeTimePeriodDirectories = String2.toStringArray(tActiveTimePeriodDirectories.toArray());
        activeTimePeriodOpendapUrls = String2.toStringArray(tActiveTimePeriodOpendapUrls.toArray());

        //check validity
        checkValidity();
        if (verbose) String2.log(
            "  Options: "     + String2.toCSSVString(activeTimePeriodOptions) + "\n" +
            "  Titles: "      + String2.toCSSVString(activeTimePeriodTitles) + "\n" +
            "  NHours: "      + String2.toCSSVString(activeTimePeriodNHours) + "\n" +
            "  Directories: " + String2.toCSSVString(activeTimePeriodDirectories) + "\n" +
            "  GridDataSetCWLocal constructor done. TIME=" + grdDirTime);
    
    }        

    /**
     * This creates the legendTime string from a local file's grd(Zip)Name.
     *
     * @param grdZipName
     * @return the dateTime string for the legend
     */
    /*public static String getDateTime(String grdZipName) {
        String dateTime = Calendar2.formatAsISODate(
            Calendar2.getGCalendar(  //start date
                String2.parseInt(grdZipName.substring(2, 6)),
                String2.parseInt(grdZipName.substring(6, 9))));
        if (grdZipName.charAt(16) == 'h') { //2nd 'date' is a time
            dateTime += 
                " " + grdZipName.substring(10, 12) + //hr
                ":" + grdZipName.substring(12, 14) + //mn
                //":" +   grdZipName.substring(14, 16) + //sec
                " UTC";
        } else { //2nd date is end date
            dateTime += " to " + Calendar2.formatAsISODate(
                Calendar2.getGCalendar( 
                    String2.parseInt(grdZipName.substring(10, 14)),
                    String2.parseInt(grdZipName.substring(14, 17))));
        }           
        return dateTime;
    }*/

    /**
     * This makes the specified grid as best it can.
     * See the superclass' documentation.
     */
    public Grid makeGrid(String timePeriodValue, String timeValue, 
        double minX, double maxX, 
        double minY, double maxY,
        int desiredNWide, int desiredNHigh)  throws Exception {

        long time = System.currentTimeMillis();
        String msg = "GridDataSetCWLocal.makeGrid(timePeriod=" + timePeriodValue +
               " date=" + timeValue +
            "\n  minX=" + minX +
               " maxX=" + maxX +
               " minY=" + minY +
               " maxY=" + maxY +
               " nWide=" + desiredNWide +
               " nHigh=" + desiredNHigh + ")";
        if (verbose) String2.log("/* " + msg); 
        String errorInMethod = String2.ERROR + " in " + msg + ":\n";

        //get indexes
        int timePeriodIndex = String2.indexOf(activeTimePeriodOptions, timePeriodValue);
        Test.ensureNotEqual(timePeriodIndex, -1, 
            errorInMethod + "timePeriod not found: " + timePeriodValue +
            "\ntimePeriodOptions=" + String2.toCSSVString(activeTimePeriodOptions));
        String tActiveTimePeriodTimes[] = (String[])activeTimePeriodTimes.get(timePeriodIndex);
        int timeIndex = String2.indexOf(tActiveTimePeriodTimes, timeValue);
        Test.ensureNotEqual(timeIndex, -1, 
            errorInMethod + "time (" + timeValue + ") must be one of\n" + 
            String2.toCSSVString(tActiveTimePeriodTimes));
        if (verbose) String2.log( 
            "  activeTimePeriodDir=" + activeTimePeriodDirectories[timePeriodIndex]);

        //if file is .zip'd, unzip and store in cacheDir
        String atpfn[] = (String[])activeTimePeriodFileNames.get(timePeriodIndex);
        String sourceDir = directory + activeTimePeriodDirectories[timePeriodIndex] + "/grd/";
        String sourceName = atpfn[timeIndex]; 
        String cacheName = sourceName.substring(0, sourceName.length() - 4); //remove ".zip" extension
        if (!File2.touch(cacheDir + cacheName)) {
            //unzip it
            long unzipTime = System.currentTimeMillis();
            SSR.unzipRename(sourceDir, sourceName, cacheDir, cacheName, 30);  
            if (verbose) String2.log("  unzip time=" + (System.currentTimeMillis() - unzipTime));
        }

        //readGrd   (note that it calls makeLonPM180() and subset())
        long readGrdTime = System.currentTimeMillis();
        Grid grid = new Grid();
        grid.readGrd(cacheDir + cacheName, 
            minX, maxX, minY, maxY, 
            desiredNWide, desiredNHigh);

        if (verbose) String2.log( 
            "\\* GridDataSetCWLocal.makeGrid " + internalName + 
            " done.  readGrdTime=" + (System.currentTimeMillis() - readGrdTime) + 
            " TIME=" + (System.currentTimeMillis() - time) + "\n");

        return grid;
    }


}
