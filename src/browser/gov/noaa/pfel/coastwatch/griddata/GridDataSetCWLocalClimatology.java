/* 
 * GridDataSetCWLocalClimatology Copyright 2006, NOAA.
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
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

// from netcdfAll-x.jar
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents a gridDataSet based on locally stored climatology data files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-11-06
 */
public class GridDataSetCWLocalClimatology extends GridDataSetCWLocal { 


    /** Dates associated with months. */
    public final static String monthDateStrings[] = {
        "0001001_0001031", "0001032_0001059",  
        "0001060_0001090", "0001091_0001120", 
        "0001121_0001151", "0001152_0001181", 
        "0001182_0001212", "0001213_0001243", 
        "0001244_0001273", "0001274_0001304", 
        "0001305_0001334", "0001335_0001365"};
    public final static String monthOptions[] = { 
        "0001-01-16 12:00:00", 
        "0001-02-15 00:00:00", 
        "0001-03-16 12:00:00", 
        "0001-04-16 00:00:00", 
        "0001-05-16 12:00:00", 
        "0001-06-16 00:00:00", 
        "0001-07-16 12:00:00", 
        "0001-08-16 12:00:00", 
        "0001-09-16 00:00:00", 
        "0001-10-16 12:00:00", 
        "0001-11-16 00:00:00", 
        "0001-12-16 12:00:00"};

    /**
     * The constructor searches for available climatology data and sets
     * activeTimePeriodOptions, activeTimePeriodTitles, activeTimePeriodTimes,
     * and activeTimePeriodFileNames.
     * This prints diagnostic info to String2.log.
     *
     * @param fileNameUtility
     * @param internalName
     * @param localDataSetBaseDirectory
     * @param cacheDirectory a directory that can be used to cache files
     * @throws Exception if trouble, e.g., no data
     */
    public GridDataSetCWLocalClimatology(FileNameUtility fileNameUtility, String internalName,
            String localDataSetBaseDirectory, String cacheDirectory) 
        throws Exception {

        //this is a climatology  
        isClimatology = true;
        cacheDir = cacheDirectory;

        //set info from DataSet.properties
        dataSetPropertiesSetup(fileNameUtility, internalName);
        String dataSetFileInfo[] = String2.split(fileNameUtility.dataSetRB2().getString(
            internalName + "FileInfo",  null), '`');
        Test.ensureEqual(dataSetFileInfo.length, 3, 
            String2.ERROR + ": " + internalName + "FileInfo in DataSet.properties must have 3 items.");
        directory     = dataSetFileInfo[OneOf.DSFIDirectory]; 
        //add localDataSetBaseDirectory to relative directory datasets
        if (directory.charAt(0) != '/') 
            directory = localDataSetBaseDirectory + directory;
        fileNameRegex = dataSetFileInfo[OneOf.DSFIRegex];     
        //climatologies never have associated climatologies

        //set original timePeriod values
        String[] originalTimePeriodOptions = { //these must all be in TimePeriods.OPTIONS
            "1 day", "3 day", "4 day", "5 day", "8 day", "10 day", "14 day", 
            TimePeriods.MONTHLY_OPTION};
        String[] originalTimePeriodDirectories = {
            "1day", "3day", "4day", "5day", "8day", "10day", "14day", "mday"};  //note lday for 25hour
        ////these match the shortNames in TimePeriods
        //String[] originalTimePeriodShortNames = {
        //    "1day", "3day", "4day", "5day", "8day", "10day", "14day", "mday"};  
        //these match the system in TimePeriods   //30 is special case for mday
        int[] originalTimePeriodNHours = {
            24, 25, 33, 3*24, 4*24, 5*24, 8*24, 10*24, 14*24, 30*24}; 
        String[] originalTimePeriodTitles = {
            "", //dummy main title
            compositeTitle("a 1 day"),
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
            //*** FOR NOW, JUST DO MONTHLY TIME PERIOD
            //the program isn't set up to handle other time periods -- 
            //I don't want it to crash if data for other time periods appears.
            if (TimePeriods.getNHours(originalTimePeriodOptions[timePeriodI]) != 30 * 24)
                continue;

            String timePeriodInFileName = originalTimePeriodDirectories[timePeriodI];
            String dir = directory + timePeriodInFileName + "/grd";
            int expectedNHours = TimePeriods.getNHours(originalTimePeriodOptions[timePeriodI]);

            //get the sorted list of the grd.zip files
            String regex = fileNameRegex + ".*\\.grd\\.zip";
            String grds[] = RegexFilenameFilter.list(dir, regex);
            if (grds == null) {
                String2.log(String2.ERROR + " in GridDataSetCWLocalClimatology: trouble with dir=" + 
                    dir + " and regex=" + regex + ".");
                grds = new String[0];
            }
            int nGrds = grds.length;
            if (verbose)
                String2.log("\n* GridDataSetCWLocalClimatology constructor dir=" + dir + 
                    "\n  regex=" + fileNameRegex + 
                    ".*\\.grd\\.zip nfiles=" + nGrds +
                    (nGrds > 0? "\n  last grds=" + grds[nGrds-1] : ""));
            StringArray keepGrds = new StringArray();
            StringArray keepDates = new StringArray();
            for (int grdI = 0; grdI < nGrds; grdI++) {
                String tOption = ""; //indicating trouble
                try {
                    if (grds[grdI].charAt(16) == 'h') {

                        //sanity check: it should be an 'h' date, shouldn't it?
                        if (expectedNHours == 0 || expectedNHours % 24 != 0) {   //this handles 25hour and 33hour correctly
                            //all is well
                            //hourly data AT2005060_044800h_sstn_westus.grd.zip
                            String yyyyddd = grds[grdI].substring(2, 9);
                            tOption = Calendar2.yyyydddToIsoDate(yyyyddd) + //throws exception if trouble
                                " " + grds[grdI].substring(10, 12) + //hr
                                ":" + grds[grdI].substring(12, 14) + //mn
                                ":" + grds[grdI].substring(14, 16);  //sec
                        } else {
                            //trouble
                            Test.error(String2.ERROR + 
                                ": It shouldn't have been an 'h'-type file name."); //FILE NAME SHOWN BELOW
                        }
                    } else { 
                        //2nd date is end date   
                        //  currently year 0 e.g. PH0000091_0000120_ssta.grd.zip
                        //  will be year 1   e.g. PH0001091_0001120_ssta.grd.zip
                        String endDateString = Calendar2.yyyydddToIsoDate(  //throws exception if trouble
                            grds[grdI].substring(10, 17)); 

                        //do sanity checking: is the time period correct?
                        String beginTimeString = Calendar2.yyyydddToIsoDate(  //throws exception if trouble
                            grds[grdI].substring(2, 9)); 
                        if (expectedNHours == 0 || expectedNHours % 24 != 0) {   //this handles 25hour and 33hour correctly
                            Test.error(String2.ERROR + 
                                ": It should have been an 'h'-type file name."); //FILE NAME SHOWN BELOW
                        } else if (expectedNHours == 30*24) {
                            //monthly       e.g., PC0001091_0001120_ssta.grd.zip
                            String tDates = grds[grdI].substring(2, 17);
                            for (int i = 0; i < monthDateStrings.length; i++) {
                                if (tDates.equals(monthDateStrings[i])) {
                                    tOption = monthOptions[i];
                                    break;
                                }
                            }
                            if (tOption.length() == 0) 
                                Test.error(String2.ERROR + 
                                    ": Unexpected month.");  //FILE NAME SHOWN BELOW
                        } else if (expectedNHours % 24 == 0 && expectedNHours >= 24 && expectedNHours < 30*24) {
                            //expectedNHours is >1 day but <month
                            //NOT FINISHED
                            int beginHours = Calendar2.isoStringToEpochHours(beginTimeString); //throws Exception if trouble
                            int endHours   = Calendar2.isoStringToEpochHours(endDateString);   //throws Exception if trouble
                            int nDays = (endHours - beginHours)/24 + 1;
                            if (nDays == expectedNHours / 24) {
                                tOption = DataHelper.centerOfStartDateAndInclusiveEndDate(beginTimeString, endDateString);
                            } else {
                                Test.error(String2.ERROR + 
                                    ": Unexpected nDays (" + nDays + ")."); //FILE NAME SHOWN BELOW
                            }
                        } else {
                            Test.error(String2.ERROR + 
                                ": Unexpected expectedNHours=" + expectedNHours + ".");//FILE NAME SHOWN BELOW
                        }
                    }
                } catch (Exception e) {
                    tOption = ""; //should be already
                    String2.log(e.toString());
                }

                //important for vector common dates: just keep valid dates
                if (tOption.length() > 0) {
                    //make sure it isn't a duplicate date (when 2 files for same date match regex)!
                    String previousDate = keepDates.size() > 0? 
                        keepDates.get(keepDates.size() - 1) : "";
                    if (tOption.equals(previousDate)) {
                        String2.log(String2.ERROR + ": Duplicate file entries:" + grds[grdI] + 
                            " and " + previousDate);
                        tOption = "";
                    } else {
                        //keep this grd
                        keepGrds.add(String2.canonical(grds[grdI])); //canonical saves space if data used in >1 browser
                        keepDates.add(tOption);  //GridDataSet.makeTimeOptionsCanonical will call String2.canonical()
                    }
                }
                
                //need to display rest of error message?
                if (tOption.length() == 0) {
                    String2.log(String2.ERROR + " in GridDataSetCWLocalClimatology constructor\n" +
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
            "  GridDataSetCWLocalClimatology constructor " + internalName + 
                " done. TIME=" + grdDirTime);
    }        

    /**
     * This throws Exception, since time series of climatology is not allowed.
     */
    public Table getTimeSeries(String newDir, double x, double y,
        String isoMinDate, String isoMaxDate, String timePeriod) throws Exception {

        Test.error(String2.ERROR + ": GridDataSetCWLocalClimatology doesn't support getTimeSeries.");
        return null;

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



    // ******* This uses GridDataSetCWLocal's makeGrid(). *****************

    
    
    /**
     * This adds some features to GridDataSetCW.setAttributes.
     *
     * @param grid
     * @param fileName A CWBrowser-style file name (so that fileNameUtility
     *    can generate the information.
     */
    public void setAttributes(Grid grid, String fileName) throws Exception {
        //set standard attributes
        super.setAttributes(grid, fileName);

        //then modify
        //see CNetCDF Climate and Forecast (CF) Metadata Conventions, 
        //  sections 4.4 Time coordinate and 4.4.1 Calendar
        //  So I'm using e.g., "3 months since 0001-01-01" and calendar="none"
        //    This is imperfect because UDUnits "months" are each a 
        //      year(3.15569259747e7 second) / 12.
        //    UDUnits has no "calendarYear" where the month lengths vary.
        //    One could use days, but leap and non-leap year months have 
        //      different number of days.
        //  It is possible that calendar=360_day is better, but I find no good 
        //    information about it.  UDUnits doesn't support other calendars.
        //grid.globalAttributes().remove("pass_date");
        //grid.globalAttributes().remove("start_time");
        String timePeriodInFileName = FileNameUtility.getTimePeriodString(fileName);
        String centeredTime = FileNameUtility.getRawDateString(fileName);
        if (timePeriodInFileName.equals("mday")) {
            int monthIndex = String2.caseInsensitiveIndexOf(monthOptions, centeredTime);
            grid.globalAttributes().set("time_coverage_start", monthIndex + " months " + Grid.SINCE_111);
            grid.globalAttributes().set("time_coverage_end", (monthIndex + 1) + " months " + Grid.SINCE_111);
        } else {
            Test.error("not supported yet");
        }

    }
     

    /** 
     * This returns the start GregorianCalendar for the specified timePeriodValue and centeredTimeValue.
     * 
     * @param timePeriodValue
     * @param centeredTimeValue
     * @return the start GregorianCalendar for the specified timePeriodValue and centeredTimeValue.
     */
/*    public GregorianCalendar getStartCalendar(String timePeriodValue, String centeredTimeValue) {

        //for months, return start of e.g., "July"
        if (timePeriodValue.equals(TimePeriods.MONTHLY_OPTION)) {
            int monthIndex = String2.indexOf(monthOptions, centeredTimeValue);
            Test.ensureNotEqual(monthIndex, -1, String2.ERROR + "Unexpected centeredTimeValue=" + centeredTimeValue);
            return Calendar2.newGCalendarZulu(0001, monthIndex + 1, 1); 
        }

        //for nDay groups  
        Test.error("not implemented yet");       
        return TimePeriods.getStartCalendar(timePeriodValue, centeredTimeValue, null);
    } */

    /** 
     * This returns the end GregorianCalendar for the specified timePeriodValue and centeredTimeValue.
     *
     * @param timePeriodValue
     * @param centeredTimeValue
     * @return the end GregorianCalendar for the specified timePeriodValue and centeredTimeValue.
     */
/*    public GregorianCalendar getEndCalendar(String timePeriodValue, String centeredTimeValue) {

        //for months, return start of next month
        if (timePeriodValue.equals(TimePeriods.MONTHLY_OPTION)) {
            int monthIndex = String2.indexOf(monthOptions, centeredTimeValue);  //0..
            Test.ensureNotEqual(monthIndex, -1, String2.ERROR + "Unexpected centeredTimeValue=" + centeredTimeValue);
            GregorianCalendar gc = Calendar2.newGCalendarZulu(0001, monthIndex + 1, 1); 
            gc.add(Calendar2.MONTH, 1);
            return gc;
        }

        //for nDay groups  
        Test.error("not implemented yet");       
        return TimePeriods.getEndCalendar(timePeriodValue, centeredTimeValue, null);
    } */

    /**
     * Find the case-insensitive match for timeValue in timeOptions (sorted ascending).
     * "Closest" may be one before or one after insertion point for 
     * non-perfect match, e.g. going from pass data (2005-06-02 14:00), 
     * should go to composite for 2005-06-02, not 2005-06-03.
     *
     * @param timeOptions  an array of valid isoTimes for this dataset.  
     * @param timeValue This can be a modern (e.g., 2006-10-16 00:00:00)
     *    or a climatology (e.g., 0001-10-16 00:00:00) value.
     * @return the index (in timeOptions) of the best match for timeValue.
     *   If timeValue is null or length < 4, this returns timeOptions.length-1.
     */
    public int binaryFindClosestTime(String timeOptions[], String timeValue) {
        //ensure timeValue is a climatology 
        if (timeValue == null || timeValue.length() < 4)
            return timeOptions.length - 1;
        timeValue = "0001" + timeValue.substring(4);

        //find closest climatologyCenteredTime 
        return Calendar2.binaryFindClosest(timeOptions, timeValue);

    } 


    /**
     * This determines if users are allowed to download the specified data.
     * Currently, all climatology data is always available.
     *
     * @param timePeriod one of the TimePeriod.OPTIONs
     * @param centeredDate 
     * @return true if access is allowed
     * @throws RuntimeException if trouble (e.g., invalid endingDate)
     */
    public boolean dataAccessAllowedCentered(String timePeriod, String centeredDate) {
        //currently, climatologies are always available
        return true;
    }

}
