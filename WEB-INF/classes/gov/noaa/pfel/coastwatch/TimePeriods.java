/* 
 * TimePeriods Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;


import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class holds the master TimePeriods.OPTIONS list and related information
 * and methods.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-02-28
 */
public class TimePeriods {

    public final static String _25HOUR_OPTION = "25 hour";
    public final static String _33HOUR_OPTION = "33 hour";
    public final static String MONTHLY_OPTION = "monthly";

    /**
     * All possible time period OPTIONS used within CWBrowser,
     * in order of duration.
     * Note that other options may be added in the future.
     */
    public final static String OPTIONS[] = {
        //If these change, make similar changes in closestTimePeriod below.
        "pass", "pass", "1 observation", "hourly",
        "1 day", _25HOUR_OPTION, _33HOUR_OPTION,
        "3 day", "4 day", "5 day", "7 day", "8 day", "10 day", 
        "14 day", "1 month", MONTHLY_OPTION, "3 month", 
        "1 year", "5 year", "10 year", 
        "20 year", "all"};

    /** The OPTIONs index for "1 day". */
    public final static int _1DAY_INDEX = 4;
    /** The OPTIONs index for "25 hour". */
    public final static int _25HOUR_INDEX = 5;
    /** The OPTIONs index for "33 hour". */
    public final static int _33HOUR_INDEX = 6;

    /**
     * All possible time period OPTIONS used within CWBrowser-style file names,
     * in order of duration.
     * Note that other options may be added in the future.
     */
    public final static String IN_FILE_NAMES[] = {
        //If these change, make similar changes in closestTimePeriod below.
        "pass", "hday", "1obs", "hourly",
        "1day", "25hour", "33hour",
        "3day", "4day", "5day", "7day", "8day", "10day", 
        "14day", "mday", "mday", "3month", 
        "1year", "5year", "10year", 
        "20year", "all"};

    /**
     * The titles related to the OPTIONS.
     */
    public final static String TITLES[] = {
        "Get the data from one observation.",  
        "Get the data from one observation.", 
        "Get the data from one observation.",  
        "Get the data from one hourly observation.",  
        "Get the mean of 1 days' data.",           
        "Get the mean of 25 hours' data.", 
        "Get the mean of 33 hours' data.", 
        "Get the mean of 3 days' data.",           
        "Get the mean of 4 days' data.",           
        "Get the mean of 5 days' data.",   
        "Get the mean of 7 days' data.",   
        "Get the mean of 8 days' data.",   
        "Get the mean of 10 days' data.",
        "Get the mean of 14 days' data.",          
        "Get the mean of 1 months' data.", 
        "Get the mean of 1 months' data.", 
        "Get the mean of 3 months' data.",
        "Get the mean of 1 years' data.",          
        "Get the mean of 5 years' data.",  
        "Get the mean of 10 years' data.",
        "Get the mean of 20 years' data.",         
        "Get the mean of all available data."};

    /** The value of N_HOURS for timePeriod = "all". */
    public final static int N_HOURS_ALL = Integer.MAX_VALUE;
    public final static int N_HOURS_MONTHLY = 30 * 24;

    /**
     * The nominal number of hours related to an OPTIONS.
     * Since the number of hours in a time period varies (e.g., Nov vs Dec), these
     * are fixed numbers which represent idealized values
     * e.g., 'month' options are represented as multiples of 30*24 hours,
     * and 'year' options are represented as multiples of 365*24 hours.
     * "all" time is represented is Integer.MAX_VALUE.
     */
    public final static int N_HOURS[] = {
        0, 0, 0, 0,
        24, 25, 33,
        3*24, 4*24, 5*24, 7*24, 8*24, 10*24, 
        14*24, N_HOURS_MONTHLY, 30*24, 90*24, 
        365*24, 5*365*24, 10*365*24, 
        20*365*24, N_HOURS_ALL};


    /**
     * pickFrom indicates the level of resolution needed to pick a data from given
     * timePeriod (related to OPTIONS) with EmaDateTimeText2.
     */
    public final static String PICK_FROM[] = {
        //pass and 1 observation timePeriods every hour; YMDhms is appropriate for current data sets, but others?
        "YMDhms",  "YMDhms", "YMDhms", "YMDhms",
        "YMDhms", "YMDhms", "YMDhms", //nDay  start every day
        "YMDhms", "YMDhms", "YMDhms", "YMDhms", "YMDhms", "YMDhms",
        "YMDhms", "YMDhms", "YMDhms", "YMDhms",   //nMonth start every month
        "YMD", "Y", "Y",                  //1Year starts every month; nYear start every year
        "Y", "Y"}; //"Y" for "all" is irrelevant, 

    /**
     * increment indicates the Calendar field that +,- buttons (if visible)
     *    affect (used by emaDateTimeText2.setIncrement).
     * sec=13, min=12, hr=10, date=5, mon=2, yr=1
     */
    public final static int INCREMENT[] = {
        Calendar.HOUR, Calendar.HOUR, Calendar.HOUR, Calendar.HOUR,
        Calendar.DATE, Calendar.HOUR, Calendar.HOUR, 
        Calendar.DATE, Calendar.DATE, Calendar.DATE, Calendar.DATE, Calendar.DATE, Calendar.DATE,
        Calendar.DATE, Calendar.MONTH, Calendar.MONTH, Calendar.MONTH, 
        Calendar.YEAR, Calendar.YEAR, Calendar.YEAR, 
        Calendar.YEAR, Calendar.YEAR}; 

    /**
     * Find the index of a time period option in OPTIONS.
     * 
     * @param option usually one of the OPTIONS.
     * @return the index in OPTIONS for value,
     *   or -1 if not found.
     */
    public static int exactTimePeriod(String option) { 
        return String2.indexOf(OPTIONS, option);
    }

    /**
     * Given an option from OPTIONS, this returns the corresponding N_HOURS.
     *
     * @param option an option from OPTIONS
     * @return the corresponding N_HOURS
     * @throws exception if option not found in OPTIONS
     */
    public static int getNHours(String option) {
        int index = exactTimePeriod(option);
        Test.ensureNotEqual(index, -1, String2.ERROR + " in TimePeriods.getNHours: '" + option + 
            "' not in TimePeriods.OPTIONS.");
        return N_HOURS[index];
    }

    /**
     * Given an option from OPTIONS, this returns the corresponding IN_FILE_NAME.
     *
     * @param option an option from OPTIONS
     * @return the corresponding IN_FILE_NAME
     * @throws exception if option not found in OPTIONS
     */
    public static String getInFileName(String option) {
        int index = exactTimePeriod(option);
        Test.ensureNotEqual(index, -1, String2.ERROR + " in TimePeriods.getInFileName: '" + option + 
            "' not in TimePeriods.OPTIONS.");
        return IN_FILE_NAMES[index];
    }

    /**
     * Make the list of titles related to a list of OPTIONS.
     *
     * @param options is a String[] with the valid options, all of which
     *   must be in options, except "" which generates "" for a title.
     * @return the corresponding titles (with a generic one inserted at the beginning
     *   because that is what EmaSelect wants)
     */
    public static String[] getTitles(String[] options) {
        int n = options.length;
        String titles[] = new String[n + 1];
        titles[0] = "Specify the length of time in which you are interested.";
        for (int i = 0; i < n; i++)
            titles[i + 1] = options[i].length() == 0? "" : TITLES[exactTimePeriod(options[i])];
        return titles;
    }



    /**
     * Find the closest time period in a list.
     *
     * @param value  The desired value, which should be in OPTIONS, 
     *   but doesn't have to be in 'list'.
     * @param list is a String[] with the valid options, all of which
     *   must be in options.
     * @return index in list for the closest time period.
     *   If value is null, "" or not in the list, this returns 
     *   the index of the option in list closest to "1 day".
     * @throws Exception if trouble
     */
    public static int closestTimePeriod(String value, String[] list) {
        //make sure it is one of standard timePeriods
        int valueIndex = exactTimePeriod(value);
        if (valueIndex < 0)
            valueIndex = _1DAY_INDEX; 
        value = OPTIONS[valueIndex];
        int valueHours = N_HOURS[valueIndex];

        //find exact
        int index = String2.indexOf(list, value);
        if (index >= 0)
            return index;
      
        //find closest
        int diff = Integer.MAX_VALUE;
        index = -1;
        for (int i = 0; i < list.length; i++) {
            int tHours = getNHours(list[i]);
            if (tHours == N_HOURS_ALL)
                tHours = 30 * 365 * 24;
            int tDiff = Math.abs(tHours - valueHours);
            //String2.log(i + " TimePeriods.closestTimePeriod value=" + value + 
            //    " tHours=" + tHours + " tDiff=" + tDiff + " index=" + index);
            if (tDiff < diff) {
                index = i;
                diff = tDiff;
            }
        }
        return index;
    }

    /**
     * Find the closest time period in a list.
     *
     * @param nHours  
     * @param list is a String[] with the valid options, all of which
     *   must be in OPTIONS.
     * @return index in list for the closest time period.
     *   If value is NaN, this returns 
     *   the index of the option in list closest to "1 day".
     */
    public static int closestTimePeriod(int nHours, String[] list) {
        if (nHours == Integer.MAX_VALUE)
            return closestTimePeriod(OPTIONS[_1DAY_INDEX], list);

        //find closest
        int diff = Integer.MAX_VALUE;
        int closestI = -1;
        for (int i = 0; i < list.length; i++) {
            int tNHours = getNHours(list[i]);
            if (tNHours == N_HOURS_ALL)
                tNHours = 30 * 365 * 24;
            int tDiff = Math.abs(nHours - tNHours);
            //String2.log(i + " TimePeriods.closestTimePeriod tNHours=" + tNHours + 
            //    " nHours=" + nHours + " tDiff=" + tDiff + " index=" + index);
            if (tDiff < diff) {
                closestI = i;
                diff = tDiff;
            }
        }
       
        return closestI;
    }

    /**
     * This cleans a centeredTime.
     * For example, if the Station Data raw centered time is 2006-02-12 and 
     * timePeriod is changed to 1 year (which is to nearest month), 
     * the clean centered time is changed to start of month (e.g., 2006-02-01).
     * 
     * @param timePeriod  must be one of the standard OPTIONS
     * @param isoCenteredTime the exact centered ISO datetime String 
     *     (thus this won't work for climatology: e.g., "July")
     * @return the cleaned centerTime as isoDateTimeSpace.
     *   <ul>
     *   <li>If timePeriod is pass, the time is unchanged.
     *   <li>If timePeriod is nHour, min=0, sec=0.
     *   <li>If timePeriod is even nDays, hr=0, min=0, sec=0.
     *   <li>If timePeriod is odd nDays, hr=12, min=0, sec=0.
     *   <li>If timePeriod is even nMonths, date=1, hr=0, min=0, sec=0.
     *   <li>If timePeriod is odd nMonths, date=middle, hr=0 or 12, min=0, sec=0.
     *   <li>If timePeriod is 1 year (to nearest month), date=1, hr=0, min=0, sec=0.
     *   <li>If timePeriod is even nYears, month=0, date=1, hr=0, min=0, sec=0.
     *   <li>If timePeriod is odd nYears, month=6, date=1, hr=0, min=0, sec=0.
     *   <li>If timePeriod is "all", the time is unchanged.
     *   </ul>
     * @throws Exception if trouble  (e.g., if isoCenteredTime is "")
     */
    public static String getCleanCenteredTime(String timePeriod, 
            String isoCenteredTime) throws Exception {

        int timePeriodHours = getNHours(timePeriod);

        if (timePeriodHours == 0 ||
            timePeriodHours == N_HOURS_ALL) {  
            //0 hours or all time, no change
            return isoCenteredTime;
        }

        GregorianCalendar cal = Calendar2.parseISODateTimeZulu(isoCenteredTime); //throws Exception if trouble
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (timePeriodHours == 1) {
            //1 hour
            //Dave says: no further change needed
        } else if (timePeriodHours % 24 != 0) {
            //25_HOUR, and 33_HOUR
           cal.set(Calendar.MINUTE, 30); 
        } else if (timePeriodHours == 365 * 24) {  
            //1 year (center to nearest month)    
            cal.set(Calendar.DATE, 1); 
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0); 
        } else if ((timePeriodHours % (365 * 24)) == 0) {  
            //nYears    center to nearest month 
            int nYears = timePeriodHours / (365 * 24);  //may be odd number
            cal.set(Calendar.MONTH, Math2.odd(nYears)? 6: 0);  //0 based
            cal.set(Calendar.DATE, 1); 
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0); 
        } else if ((timePeriodHours % (30 * 24)) == 0) {  //check for month AFTER check for years
            //nMonths
            int nMonths = timePeriodHours / (30 * 24);
            if (Math2.odd(nMonths)) { //e.g., odd:1..16.5..31   even:1..16..30
                Calendar2.centerOfMonth(cal);
            } else {
                //isEven
                cal.set(Calendar.DATE, 1); 
                cal.set(Calendar.MINUTE, 0); 
            }
        } else if (timePeriodHours % 24 == 0) { 
            //n days (e.g., 3) 
            int nDays = timePeriodHours / 24;
            cal.set(Calendar.HOUR_OF_DAY, Math2.odd(nDays)? 12 : 0);
            cal.set(Calendar.MINUTE, 0); 
        } else Test.error(String2.ERROR + ": Unexpected timePeriod=" + timePeriod);

        return Calendar2.formatAsISODateTimeSpace(cal);
    }


    /**
     * This calculates the start time of a time period.
     * 
     * @param timePeriod  must be one of the standard OPTIONS
     * @param isoCenteredTime the centered iso datetime string.
     *     !!! It will be cleaned by getCleanCenteredTime
     *     so it won't work for climatology.  [still true?]
     * @param dataMinT the start time of "all" of the data (an ISO date[time] String).
     *    If this is null, 1900-01-01 is used.
     * @return The start time of the time period.
     *   <ul>
     *   <li>If timePeriod is pass or 1 hour (assumed to be already centered), 
     *        the start time equals the centered time.
     *   <li>If timePeriod is nHours (25 or 33), the start time is nHours/2 back.
     *   <li>If timePeriod is nDays, the start time is the nDays/2 back.
     *   <li>If timePeriod is nMonths (multiple of 30*24), the start time is nMonths/2 back.
     *   <li>If timePeriod is nYears, the start time 12*n/2 months back.
     *   <li>If timePeriod is "all", the start time is dataMinT.
     *   </ul>
     * @throws Exception if trouble, e.g., if isoCenteredTime is ""
     */
    public static GregorianCalendar getStartCalendar(String timePeriod, 
            String isoCenteredTime, String dataMinT) throws Exception {
        int timePeriodHours = getNHours(timePeriod);
        isoCenteredTime = getCleanCenteredTime(timePeriod, isoCenteredTime);
        //String2.log("centeredTime=" + isoCenteredTime);
        GregorianCalendar cal = Calendar2.parseISODateTimeZulu(isoCenteredTime); //throws Exception if trouble
        if (timePeriodHours <= 1) { 
            //0 or 1 hours, Dave says: no change
        } else if (timePeriodHours == N_HOURS_ALL) {  
            //all time, use dataMinT
            if (dataMinT == null || dataMinT.length() == 0)
               dataMinT = "1900-01-01"; 
            cal = Calendar2.parseISODateTimeZulu(dataMinT); //throws Exception if trouble
        } else if (timePeriodHours % 24 != 0) {
            //25hour and 33
            cal.add(Calendar.MINUTE, (-timePeriodHours * 60) / 2); //nMinutes is always even
        } else if ((timePeriodHours % (365 * 24)) == 0) {  
            //nYears    
            int nMonths = 12 * (timePeriodHours / (365 * 24));  
            cal.add(Calendar.MONTH, -nMonths / 2); 
        } else if ((timePeriodHours % (30 * 24)) == 0) {  //check for month AFTER check for years
            //nMonths
            int nMonths = timePeriodHours / (30 * 24);
            cal.set(Calendar.MINUTE, 0); 
            cal.set(Calendar.HOUR_OF_DAY, 0); 
            cal.set(Calendar.DATE, 1); 
            cal.add(Calendar.MONTH, -nMonths / 2); //integer division
        } else if (timePeriodHours % 24 == 0) { 
            //nDays (e.g., 3) 
            cal.add(Calendar.HOUR_OF_DAY, -timePeriodHours / 2);
        } else Test.error(String2.ERROR + 
            ": Unexpected timePeriod=" + timePeriod);
        return cal;
    }

    /**
     * This calculates the end time of a time period 
     *   (for composites: this is the exact end instant -- not 1 second back).
     * 
     * @param timePeriod  must be one of the standard OPTIONS
     * @param isoCenteredTime the centered iso datetime string.
     *     !!! It will be cleaned a cleaned by getCleanCenteredTime.
     *     (thus this won't work for climatology: e.g., "July")
     * @param dataMaxT the end time of "all" of the data (an ISO Date[time] string).
     *    If this is null, the current date/time is used.
     * @return The end time of the time period  
     *   (for composites: this is the exact end instand -- not 1 second back).
     *   <ul>
     *   <li>If timePeriod is pass or 1 hour (assumed to be already centered), 
     *       the end time equals the centered time.
     *   <li>If timePeriod is nHours (25 or 33), the end time is nHours/2 forward.
     *   <li>If timePeriod is nDays, the end time is nDays/2 forward.
     *   <li>If timePeriod is nMonths, the end time is nMonths/2 forward.
     *   <li>If timePeriod is nYears, the end time is 12*n/2 months forward.
     *   <li>If timePeriod is "all", the end time is dataMaxT 
     *     (or now+24 hours if dataMaxT is null or "").
     *   </ul>
     * @throws Exception if trouble, e.g., if isoCenteredTime is ""
     */
    public static GregorianCalendar getEndCalendar(String timePeriod, 
            String isoCenteredTime, String dataMaxT) throws Exception {
        int timePeriodHours = getNHours(timePeriod);
        isoCenteredTime = getCleanCenteredTime(timePeriod, isoCenteredTime);
        GregorianCalendar cal = Calendar2.parseISODateTimeZulu(isoCenteredTime); //throws Exception if trouble
        if (timePeriodHours <= 1) { 
            //0 or 1 hours, Dave says: no change
        } else if (timePeriodHours == N_HOURS_ALL) {  
            //all time, use dataMinT
            if (dataMaxT == null || dataMaxT.length() == 0) {
               cal = Calendar2.newGCalendarZulu();
               cal.add(Calendar.HOUR_OF_DAY, 24);
            } else cal = Calendar2.parseISODateTimeZulu(dataMaxT); //throws Exception if trouble
        } else if (timePeriodHours % 24 != 0) {
            //25hour and 33
            cal.add(Calendar.MINUTE, (timePeriodHours * 60) / 2); //nMinutes is always even
        } else if ((timePeriodHours % (365 * 24)) == 0) {  
            //nYears    
            int nMonths = 12 * (timePeriodHours / (365 * 24));  
            cal.add(Calendar.MONTH, nMonths / 2); 
        } else if ((timePeriodHours % (30 * 24)) == 0) {  //check for month AFTER check for years
            //nMonths
            int nMonths = timePeriodHours / (30 * 24);
            cal.set(Calendar.MINUTE, 0); 
            cal.set(Calendar.HOUR_OF_DAY, 0); 
            cal.set(Calendar.DATE, 1); 
            cal.add(Calendar.MONTH, (nMonths + 1) / 2); //integer division
        } else if (timePeriodHours % 24 == 0) { 
            //nDays (e.g., 3) 
            cal.add(Calendar.HOUR_OF_DAY, timePeriodHours / 2);
        } else Test.error(String2.ERROR + 
            ": Unexpected timePeriod=" + timePeriod);

        return cal;
    }

    /**
     * This converts an End Calendar into
     * a centered time (centered to the nearest second).
     *
     * <p>WARNING: Old-style (pre Dec 2006) 25 and 33 hour files were stored as
     * e.g., 00:00 (Dave says that is correct to nearest second),
     * so use before calling this:
     * <pre>
         //fix old-style 25 and 33 hour end times  so 59:59
         if (timePeriodNHours &gt; 1 &amp;&amp; timePeriodNHours % 24 != 0)
             cal.add(Calendar2.SECOND, -1);
     * </pre>
     *
     * @param timePeriodNHours one of the TimePeriod.NHOURS options
     * @param cal initially, the end time GregorianCalendar usually from
     *    getEndCalendar.
     *    This will be modified so that it holds the centered time when finished.
     * @param errorInMethod the start of the error message
     * @throws Exception if trouble
     */
    public static void endCalendarToCenteredTime(int timePeriodNHours, 
            GregorianCalendar cal, String errorInMethod) throws Exception {
        if (timePeriodNHours <= 1) {
            //Dave says: do nothing
        } else if (timePeriodNHours % 24 != 0) { 
            //covers 25, 33 hours
            if (cal.get(Calendar2.MINUTE) != 0 ||
                cal.get(Calendar2.SECOND) != 0) {
                Test.error(errorInMethod + "timePeriodNHours=" + timePeriodNHours + 
                    " Time=" + Calendar2.formatAsISODateTimeT(cal) + 
                    " doesn't end in 00:00."); 
            }
            cal.add(Calendar2.MINUTE, -timePeriodNHours * 60 / 2); 
        } else if (timePeriodNHours < 30 * 24) {
            //nDays        
            if (cal.get(Calendar2.HOUR_OF_DAY) != 0 ||
                cal.get(Calendar2.MINUTE) != 0 ||
                cal.get(Calendar2.SECOND) != 0) {
                Test.error(errorInMethod + "timePeriodNHours=" + timePeriodNHours + 
                    " Time=" + Calendar2.formatAsISODateTimeT(cal) + 
                    " doesn't end in 00:00:00."); 
            }
            cal.add(Calendar2.HOUR_OF_DAY, -timePeriodNHours / 2);  //these nHours always even
        } else if (timePeriodNHours == 30 * 24) {
            //1 month
            if (cal.get(Calendar2.HOUR_OF_DAY) != 0 ||
                cal.get(Calendar2.MINUTE) != 0 ||
                cal.get(Calendar2.SECOND) != 0) {
                Test.error(errorInMethod + "timePeriodNHours=" + timePeriodNHours + 
                    "Time=" + Calendar2.formatAsISODateTimeT(cal) + 
                    " doesn't end in 00:00:00."); 
            }
            cal.add(Calendar2.SECOND, -1);  //-1 changes to 23:59:59 on last day of previous month
            Calendar2.centerOfMonth(cal); 
        } else {
            //there are longer time periods, but no data sets used them
            Test.error(errorInMethod + "Unexpected timePeriodNHours=" + timePeriodNHours);
        }
    }

    /**
     * This converts a centered time string into an old-style end time option string
     * (For compatibility with old-style options in CWBrowser).
     *
     * @param timePeriodNHours one of the TimePeriod.NHOURS options, e.g., 24
     * @param centeredTime e.g., 2006-08-07 12:00:00
     * @return old style end time e.g., 2006-08-07 (last day, inclusive)
     * @throws Exception if trouble
     */
    public static String centeredTimeToOldStyleEndOption(int timePeriodNHours, String centeredTime) {
        if (timePeriodNHours <= 1) 
            return  centeredTime; //Dave says: do nothing   

        GregorianCalendar cal = Calendar2.parseISODateTimeZulu(centeredTime); //throws Exception if trouble 

        //covers 25, 33 hours
        if (timePeriodNHours % 24 != 0) { 
            cal.add(Calendar2.MINUTE, timePeriodNHours * 60 / 2); 
            return Calendar2.formatAsISODateTimeSpace(cal); //yes, yyyy-mm-dd hh:mm:ss
        } 
        
        //nDays        
        if (timePeriodNHours < 30 * 24) {
            cal.add(Calendar2.HOUR_OF_DAY, timePeriodNHours / 2);  //these nHours always even
            cal.add(Calendar2.SECOND, -1);  
            return Calendar2.formatAsISODate(cal); //yes, yyyy-mm-dd
        } 
        
        //1 month
        if (timePeriodNHours == 30 * 24) {
            cal.set(Calendar2.DATE, cal.getActualMaximum(Calendar2.DATE));  
            return Calendar2.formatAsISODate(cal); //yes, yyyy-mm-dd
        }

        //there are longer time periods, but no data sets used them
        Test.error(String2.ERROR + ": Unexpected timePeriodNHours=" + timePeriodNHours);
        return "";
    }

    /**
     * This converts an old-style end time option string into a centered time string
     * (for compatibility with old-style options in CWBrowser).
     *
     * @param timePeriodNHours one of the TimePeriod.NHOURS options, e.g., 24
     * @param endOption old style end time option e.g., 2006-08-07  (last day, inclusive)
     * @return centeredTime e.g., 2006-08-07 12:00:00
     * @throws Exception if trouble
     */
    public static String oldStyleEndOptionToCenteredTime(int timePeriodNHours, 
            String endOption) throws Exception {

        if (timePeriodNHours <= 1) 
            return endOption; //Dave says: do nothing      

        GregorianCalendar cal = Calendar2.parseISODateTimeZulu(endOption); //throws Exception if trouble

        //covers 25, 33 hours   //old style end was exact last second  (00:00)
        if (timePeriodNHours % 24 != 0) { 
            cal.add(Calendar2.MINUTE, -timePeriodNHours * 60 / 2); 
            return Calendar2.formatAsISODateTimeSpace(cal); //yes, yyyy-mm-dd hh:mm:ss
        } 
        
        //nDays    old style end was last date, inclusive
        if (timePeriodNHours < 30 * 24) {
            cal.add(Calendar2.HOUR_OF_DAY, 24 - timePeriodNHours / 2);  //24=fwd to end second. then back to center
            return Calendar2.formatAsISODateTimeSpace(cal); //yes, yyyy-mm-dd hh:mm:ss
        } 
        
        //1 month  old style end was last date, inclusive
        if (timePeriodNHours == 30 * 24) {
            Calendar2.centerOfMonth(cal);  
            return Calendar2.formatAsISODateTimeSpace(cal); //yes, yyyy-mm-dd hh:mm:ss
        }

        //there are longer time periods, but no data sets used them
        Test.error(String2.ERROR + ": Unexpected timePeriodNHours=" + timePeriodNHours);
        return "";
    }

    /**
     * This creates the legend dateTime string.
     *
     * @param timePeriod must be one of the OPTIONs
     * @param centeredTime centered time in iso format, e.g., 2003-01-05 with optional time.
     *   It will be cleaned with getCleanCenteredTime.
     * @return the dateTime string for the legend:
     *   either a date time if timePeriod is pass, _25HOUR or _33HOUR
     *   (e.g., "YYYY-MM-DD HH:MM:SSZ"), or
     *   a range of dates for other time periods (e.g., "YYYY-MM-DD through YYYY-MM-DD").
     * @throws Exception if trouble
     */
    public static String getLegendTime(String timePeriod, String centeredTime) 
        throws Exception {

        int timePeriodHours = getNHours(timePeriod);
        if (timePeriodHours == N_HOURS_ALL) 
            return "All available data";

        centeredTime = getCleanCenteredTime(timePeriod, centeredTime);
        GregorianCalendar center = Calendar2.parseISODateTimeZulu(centeredTime); //throws Exception if trouble     
        GregorianCalendar start = getStartCalendar(timePeriod, centeredTime, null);
        GregorianCalendar end   = getEndCalendar(  timePeriod, centeredTime, null);
        end.add(Calendar2.DATE, -1); //so last day, inclusive 

        //String2.log("getLegendTime start=" + Calendar2.formatAsISODateTimeT(start) + 
        //             " end=" + Calendar2.formatAsISODateTimeT(end) +
        //             " nHours=" + timePeriodHours);

        //end time; return as date + time string
        if (timePeriodHours < 24)
            return Calendar2.formatAsISODateTimeTZ(center);

        if (timePeriodHours == 25) //25 hours
            return Calendar2.formatAsISODateTimeTZ(center) + " (center of 25 hours)";

        if (timePeriodHours == 33) //33 hours
            return Calendar2.formatAsISODateTimeTZ(center) + " (center of 33 hours)";

        if (timePeriodHours == 24) //1 day
            return Calendar2.formatAsISODate(start);

        if (timePeriodHours < 30*24) //n days
            return Calendar2.formatAsISODate(start) + " through " +
                   Calendar2.formatAsISODate(end);

        if (timePeriodHours == 30*24) //1 month
            return Calendar2.formatAsISODate(start).substring(0, 7);

        if (timePeriodHours <= 365*24) //n months    or 1 year (month to month)
            return Calendar2.formatAsISODate(start).substring(0, 7) + " through " +
                   Calendar2.formatAsISODate(end).substring(0, 7);
        
        //nYears
        return Calendar2.formatAsISODate(start).substring(0, 4) + " through " +
               Calendar2.formatAsISODate(end).substring(0, 4);
        
    }


    /**
     * This ensures that the beginTime (begin centered time) 
     * (used as the start time for a station time series) is valid. 
     * If beginTime is invalid or after centeredTime, this suggests a beginTime.
     *
     * @param beginTimeValue an iso formatted date time string (may be invalid)
     * @param centeredTimeValue an iso formatted date time string (must be valid)
     * @param timePeriodValue one of the TimePerio.OPTIONS (must be valid)
     * @return the original beginTimeValue. 
     *    Or if beginTimeValue is invalid or after centeredTimeValue, 
     *    this returns a suggested beginTimeValue (in ISO Date Time Space format).
     *    If timePeriod is 1 month or longer, this suggests 1 year before centeredTime,
     *    otherwise it suggests 1 month before centeredTime.
     * @throws Exception if centeredTimeValue or timePeriodValue is invalid
     */
    public static String validateBeginTime(String beginTimeValue, 
        String centeredTimeValue, String timePeriodValue) throws Exception {

        double tBeginTimeSeconds = Double.NaN;
        try {
            beginTimeValue = getCleanCenteredTime(timePeriodValue, beginTimeValue);
            tBeginTimeSeconds = Calendar2.isoStringToEpochSeconds(beginTimeValue); //throws exception if trouble
        } catch (Exception e) {
            tBeginTimeSeconds = Double.NaN;
        }
        double tEndTimeSeconds = Calendar2.isoStringToEpochSeconds(centeredTimeValue); //throws exception if trouble
        Test.ensureNotEqual(tEndTimeSeconds, Double.NaN, 
            String2.ERROR + " in TimePeriods.suggestBeginTime:\ninvalid centeredTime: " + centeredTimeValue);
        int timePeriodNHours = getNHours(timePeriodValue);

        if (Double.isNaN(tBeginTimeSeconds) || 
            tBeginTimeSeconds >= tEndTimeSeconds - timePeriodNHours * Calendar2.SECONDS_PER_HOUR) 
            beginTimeValue = Calendar2.formatAsISODateTimeSpace(
                timePeriodNHours < 24?      Calendar2.isoDateTimeAdd(centeredTimeValue, -1, Calendar2.DATE) :  // x Hours
                timePeriodNHours < 24 * 30? Calendar2.isoDateTimeAdd(centeredTimeValue, -1, Calendar2.MONTH) : // x Days
                                            Calendar2.isoDateTimeAdd(centeredTimeValue, -1, Calendar2.YEAR));  // x months or years

        return beginTimeValue;
    }


    private static void testStartEndCalendar(String timePeriod, String centeredTime, 
            String expectedStart, String expectedEnd, String id) throws Exception {
        String minT = "1971-02-03 01:00:00";
        String maxT = "2006-04-05 02:00:00";
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(
            getStartCalendar(timePeriod, centeredTime, minT)),  expectedStart, 
            "id=" + id + " badStart timePeriod=" + timePeriod + " centered=" + centeredTime + 
            " expected=" + expectedStart);
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(
            getEndCalendar(timePeriod, centeredTime, maxT)),  expectedEnd, 
            "id=" + id + " badEnd timePeriod=" + timePeriod + " centered=" + centeredTime + 
            " expected=" + expectedStart);
    }

    /**
     * This tests the methods in this class.
     */
    public static void test() throws Exception {
        String2.log("\nTesting TimePeriods...");

        Test.ensureEqual(OPTIONS.length, TITLES.length, "");
        Test.ensureEqual(OPTIONS.length, N_HOURS.length, ""); 
        Test.ensureEqual(OPTIONS.length, PICK_FROM.length, ""); 

        //test getCleanCenteredTime
        //If timePeriod is pass, the time is unchanged.
        //If timePeriod is nHour, min=0, sec=0.
        //If timePeriod is even nDays, hr=0, min=0, sec=0.
        //If timePeriod is odd nDays, hr=12, min=0, sec=0.
        //If timePeriod is even nMonths, date=1, hr=0, min=0, sec=0.
        //If timePeriod is odd nMonths, date=middle, hr=0 or 12, min=0, sec=0.
        //If timePeriod is 1 year (to nearest month), date=1, hr=0, min=0, sec=0.
        //If timePeriod is even nYears, month=0, date=1, hr=0, min=0, sec=0.
        //If timePeriod is odd nYears, month=6, date=1, hr=0, min=0, sec=0.
        //If timePeriod is "all", the time is unchanged.
        Test.ensureEqual(getCleanCenteredTime("pass",         "2005-06-10 14:13:11"), "2005-06-10 14:13:11", "a");
        Test.ensureEqual(getCleanCenteredTime(_25HOUR_OPTION, "2005-06-10 14:13:11"), "2005-06-10 14:30:00", "b");
        Test.ensureEqual(getCleanCenteredTime(_33HOUR_OPTION, "2005-06-10 14:13:11"), "2005-06-10 14:30:00", "b33");
        Test.ensureEqual(getCleanCenteredTime("1 day",        "2005-06-10 14:13:11"), "2005-06-10 12:00:00", "c");
        Test.ensureEqual(getCleanCenteredTime("3 day",        "2005-06-10 14:13:11"), "2005-06-10 12:00:00", "d");
        Test.ensureEqual(getCleanCenteredTime("4 day",        "2005-06-10 14:13:11"), "2005-06-10 00:00:00", "e");
        Test.ensureEqual(getCleanCenteredTime("8 day",        "2005-06-10 14:13:11"), "2005-06-10 00:00:00", "f");
        Test.ensureEqual(getCleanCenteredTime("1 month",      "2005-06-10 14:13:11"), "2005-06-16 00:00:00", "g");
        Test.ensureEqual(getCleanCenteredTime("1 month",      "2005-01-10 14:13:11"), "2005-01-16 12:00:00", "g");
        Test.ensureEqual(getCleanCenteredTime("1 month",      "2005-05-09 14:13:11"), "2005-05-16 12:00:00", "h");
        Test.ensureEqual(getCleanCenteredTime("3 month",      "2005-06-10 14:13:11"), "2005-06-16 00:00:00", "i");
        Test.ensureEqual(getCleanCenteredTime("1 year",       "2005-06-10 14:13:11"), "2005-06-01 00:00:00", "j");
        Test.ensureEqual(getCleanCenteredTime("5 year",       "2005-06-10 14:13:11"), "2005-07-01 00:00:00", "k");
        Test.ensureEqual(getCleanCenteredTime("10 year",      "2005-06-10 14:13:11"), "2005-01-01 00:00:00", "l");
        Test.ensureEqual(getCleanCenteredTime("all",          "2005-06-10 14:13:11"), "2005-06-10 14:13:11", "m");

        //test getCleanCenteredTime for climatology
        Test.ensureEqual(getCleanCenteredTime("pass",         "0001-06-10 14:13:11"), "0001-06-10 14:13:11", "a");
        Test.ensureEqual(getCleanCenteredTime(_25HOUR_OPTION, "0001-06-10 14:13:11"), "0001-06-10 14:30:00", "b");
        Test.ensureEqual(getCleanCenteredTime(_33HOUR_OPTION, "0001-06-10 14:13:11"), "0001-06-10 14:30:00", "b33");
        Test.ensureEqual(getCleanCenteredTime("1 day",        "0001-06-10 14:13:11"), "0001-06-10 12:00:00", "c");
        Test.ensureEqual(getCleanCenteredTime("3 day",        "0001-06-10 14:13:11"), "0001-06-10 12:00:00", "d");
        Test.ensureEqual(getCleanCenteredTime("4 day",        "0001-06-10 14:13:11"), "0001-06-10 00:00:00", "e");
        Test.ensureEqual(getCleanCenteredTime("8 day",        "0001-06-10 14:13:11"), "0001-06-10 00:00:00", "f");
        Test.ensureEqual(getCleanCenteredTime("1 month",      "0001-06-10 14:13:11"), "0001-06-16 00:00:00", "g");
        Test.ensureEqual(getCleanCenteredTime("1 month",      "0001-01-10 14:13:11"), "0001-01-16 12:00:00", "g");
        Test.ensureEqual(getCleanCenteredTime("1 month",      "0001-05-09 14:13:11"), "0001-05-16 12:00:00", "h");
        //Test.ensureEqual(getCleanCenteredTime("3 month",      "0001-06-10 14:13:11"), "0001-06-16 00:00:00", "i");
        //Test.ensureEqual(getCleanCenteredTime("1 year",       "0001-06-10 14:13:11"), "0001-06-01 00:00:00", "j");
        //Test.ensureEqual(getCleanCenteredTime("5 year",       "0001-06-10 14:13:11"), "0001-07-01 00:00:00", "k");
        //Test.ensureEqual(getCleanCenteredTime("10 year",      "0001-06-10 14:13:11"), "0001-01-01 00:00:00", "l");
        //Test.ensureEqual(getCleanCenteredTime("all",          "0001-06-10 14:13:11"), "0001-06-10 14:13:11", "m");

        //test getStartCalendar getEndCalendar
        String minT = "1971-02-03 01:00:00";
        String maxT = "2006-04-05 02:00:00";
        testStartEndCalendar("pass",          "2005-06-16 14:13:11", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "a");
        testStartEndCalendar("1 observation", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "b");
        testStartEndCalendar(_25HOUR_OPTION,  "2005-06-16 10:30:00", "2005-06-15 22:00:00", "2005-06-16 23:00:00", "c");
        testStartEndCalendar(_33HOUR_OPTION,  "2005-06-16 10:30:00", "2005-06-15 18:00:00", "2005-06-17 03:00:00", "d");
        testStartEndCalendar("1 day",         "2005-06-16 12:00:00", "2005-06-16 00:00:00", "2005-06-17 00:00:00", "e");
        testStartEndCalendar("3 day",         "2005-06-16 12:00:00", "2005-06-15 00:00:00", "2005-06-18 00:00:00", "f");
        testStartEndCalendar("5 day",         "2005-06-16 12:00:00", "2005-06-14 00:00:00", "2005-06-19 00:00:00", "h");
        testStartEndCalendar("4 day",         "2005-06-16 00:00:00", "2005-06-14 00:00:00", "2005-06-18 00:00:00", "g");
        testStartEndCalendar("8 day",         "2005-06-16 00:00:00", "2005-06-12 00:00:00", "2005-06-20 00:00:00", "i");
        testStartEndCalendar("10 day",        "2005-06-16 00:00:00", "2005-06-11 00:00:00", "2005-06-21 00:00:00", "j");
        testStartEndCalendar("1 month",       "2005-01-16 12:00:00", "2005-01-01 00:00:00", "2005-02-01 00:00:00", "k");
        testStartEndCalendar("1 month",       "2005-06-16 00:00:00", "2005-06-01 00:00:00", "2005-07-01 00:00:00", "k");
        testStartEndCalendar("3 month",       "2005-06-16 00:00:00", "2005-05-01 00:00:00", "2005-08-01 00:00:00", "m");
        testStartEndCalendar("1 year",        "2005-06-16 00:00:00", "2004-12-01 00:00:00", "2005-12-01 00:00:00", "n");
        testStartEndCalendar("5 year",        "2005-07-01 00:00:00", "2003-01-01 00:00:00", "2008-01-01 00:00:00", "o");
        testStartEndCalendar("10 year",       "2005-01-01 00:00:00", "2000-01-01 00:00:00", "2010-01-01 00:00:00", "p");
        testStartEndCalendar("all",           "2005-06-16 14:13:11", minT,                  maxT,                  "q");
        
        //test getStartCalendar getEndCalendar  -- for climatology
        testStartEndCalendar("pass",          "0001-06-16 14:13:11", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "a");
        testStartEndCalendar("1 observation", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "b");
        testStartEndCalendar(_25HOUR_OPTION,  "0001-06-16 10:30:00", "0001-06-15 22:00:00", "0001-06-16 23:00:00", "c");
        testStartEndCalendar(_33HOUR_OPTION,  "0001-06-16 10:30:00", "0001-06-15 18:00:00", "0001-06-17 03:00:00", "d");
        testStartEndCalendar("1 day",         "0001-06-16 12:00:00", "0001-06-16 00:00:00", "0001-06-17 00:00:00", "e");
        testStartEndCalendar("3 day",         "0001-06-16 12:00:00", "0001-06-15 00:00:00", "0001-06-18 00:00:00", "f");
        testStartEndCalendar("5 day",         "0001-06-16 12:00:00", "0001-06-14 00:00:00", "0001-06-19 00:00:00", "h");
        testStartEndCalendar("4 day",         "0001-06-16 00:00:00", "0001-06-14 00:00:00", "0001-06-18 00:00:00", "g");
        testStartEndCalendar("8 day",         "0001-06-16 00:00:00", "0001-06-12 00:00:00", "0001-06-20 00:00:00", "i");
        testStartEndCalendar("10 day",        "0001-06-16 00:00:00", "0001-06-11 00:00:00", "0001-06-21 00:00:00", "j");
        testStartEndCalendar("1 month",       "0001-01-16 12:00:00", "0001-01-01 00:00:00", "0001-02-01 00:00:00", "k");
        testStartEndCalendar("1 month",       "0001-06-16 00:00:00", "0001-06-01 00:00:00", "0001-07-01 00:00:00", "k");
        //testStartEndCalendar("3 month",       "0001-06-16 00:00:00", "0001-05-01 00:00:00", "0001-08-01 00:00:00", "m");
        //testStartEndCalendar("1 year",        "0001-06-16 00:00:00", "2004-12-01 00:00:00", "0001-12-01 00:00:00", "n");
        //testStartEndCalendar("5 year",        "0001-07-01 00:00:00", "2003-01-01 00:00:00", "2008-01-01 00:00:00", "o");
        //testStartEndCalendar("10 year",       "0001-01-01 00:00:00", "2000-01-01 00:00:00", "2010-01-01 00:00:00", "p");
        //testStartEndCalendar("all",           "0001-06-16 14:13:11", minT,                  maxT,                  "q");
        
        //getLegendTime
        Test.ensureEqual(getLegendTime("pass",         "2005-06-10 14:13:11"), "2005-06-10T14:13:11Z", "a");
        Test.ensureEqual(getLegendTime("1 observation","2005-06-10 14:13:11"), "2005-06-10T14:13:11Z", "a2");
        Test.ensureEqual(getLegendTime(_25HOUR_OPTION, "2005-06-10 14:13:11"), "2005-06-10T14:30:00Z (center of 25 hours)", "b");
        Test.ensureEqual(getLegendTime(_33HOUR_OPTION, "2005-06-10 14:13:11"), "2005-06-10T14:30:00Z (center of 33 hours)", "b2");
        Test.ensureEqual(getLegendTime("1 day",        "2005-06-10 14:13:11"), "2005-06-10", "c");
        Test.ensureEqual(getLegendTime("3 day",        "2005-06-10 14:13:11"), "2005-06-09 through 2005-06-11", "d");
        Test.ensureEqual(getLegendTime("4 day",        "2005-06-10 14:13:11"), "2005-06-08 through 2005-06-11", "e");
        Test.ensureEqual(getLegendTime("8 day",        "2005-06-10 14:13:11"), "2005-06-06 through 2005-06-13", "f");
        Test.ensureEqual(getLegendTime("1 month",      "2005-06-10 14:13:11"), "2005-06", "g");
        Test.ensureEqual(getLegendTime("1 month",      "2005-05-09 14:13:11"), "2005-05", "h");
        Test.ensureEqual(getLegendTime("3 month",      "2005-06-10 14:13:11"), "2005-05 through 2005-07", "i");
        Test.ensureEqual(getLegendTime("1 year",       "2005-06-10 14:13:11"), "2004-12 through 2005-11", "j");
        Test.ensureEqual(getLegendTime("5 year",       "2005-06-10 14:13:11"), "2003 through 2007", "k");
        Test.ensureEqual(getLegendTime("10 year",      "2005-06-10 14:13:11"), "2000 through 2009", "l");
        Test.ensureEqual(getLegendTime("all",          "2005-06-10 14:13:11"), "All available data", "m");

        //getLegendTime - for climatology
        Test.ensureEqual(getLegendTime("pass",         "0001-06-10 14:13:11"), "0001-06-10T14:13:11Z", "a");
        Test.ensureEqual(getLegendTime("1 observation","0001-06-10 14:13:11"), "0001-06-10T14:13:11Z", "a2");
        Test.ensureEqual(getLegendTime(_25HOUR_OPTION, "0001-06-10 14:13:11"), "0001-06-10T14:30:00Z (center of 25 hours)", "b");
        Test.ensureEqual(getLegendTime(_33HOUR_OPTION, "0001-06-10 14:13:11"), "0001-06-10T14:30:00Z (center of 33 hours)", "b2");
        Test.ensureEqual(getLegendTime("1 day",        "0001-06-10 14:13:11"), "0001-06-10", "c");
        Test.ensureEqual(getLegendTime("3 day",        "0001-06-10 14:13:11"), "0001-06-09 through 0001-06-11", "d");
        Test.ensureEqual(getLegendTime("4 day",        "0001-06-10 14:13:11"), "0001-06-08 through 0001-06-11", "e");
        Test.ensureEqual(getLegendTime("8 day",        "0001-06-10 14:13:11"), "0001-06-06 through 0001-06-13", "f");
        Test.ensureEqual(getLegendTime("1 month",      "0001-06-10 14:13:11"), "0001-06", "g");
        Test.ensureEqual(getLegendTime("1 month",      "0001-05-09 14:13:11"), "0001-05", "h");
        //Test.ensureEqual(getLegendTime("3 month",      "0001-06-10 14:13:11"), "0001-05 through 0001-07", "i");
        //Test.ensureEqual(getLegendTime("1 year",       "0001-06-10 14:13:11"), "2004-12 through 0001-11", "j");
        //Test.ensureEqual(getLegendTime("5 year",       "0001-06-10 14:13:11"), "2003 through 2007", "k");
        //Test.ensureEqual(getLegendTime("10 year",      "2005-06-10 14:13:11"), "2000 through 2009", "l");
        //Test.ensureEqual(getLegendTime("all",          "2005-06-10 14:13:11"), "All available data", "m");

        //getLegendTime
        /* NOT FINISHED
        Test.ensureEqual(getLegendTime("pass",         "2005-06-10 14:13:11"), "1 pass at 2005-06-10 14:13:11Z", "a");
        Test.ensureEqual(getLegendTime("1 observation","2005-06-10 14:13:11"), "1 observation at 2005-06-10 14:13:11Z", "a");
        Test.ensureEqual(getLegendTime(_25HOUR_OPTION, "2005-06-10 14:13:11"), "25 hours, centered at 2005-06-10 14:00:00Z", "b");
        Test.ensureEqual(getLegendTime("1 day",        "2005-06-10 14:13:11"), "1 day, centered at 2005-06-10 12:00:00Z", "c");
        Test.ensureEqual(getLegendTime("3 day",        "2005-06-10 14:13:11"), "3 days, centered at 2005-06-10 12:00:00Z", "d");
        Test.ensureEqual(getLegendTime("4 day",        "2005-06-10 14:13:11"), "4 days, centered at 2005-06-10 00:00:00Z", "e");
        Test.ensureEqual(getLegendTime("8 day",        "2005-06-10 14:13:11"), "8 days, centered at 2005-06-10 00:00:00Z", "f");
        Test.ensureEqual(getLegendTime("1 month",      "2005-06-10 14:13:11"), "1 month, centered at 2005-06-16 00:00:00Z", "g");
        Test.ensureEqual(getLegendTime("1 moth",       "2005-05-09 14:13:11"), "1 month, centered at 2005-05-09 12:00:00Z", "h");
        Test.ensureEqual(getLegendTime("3 month",      "2005-06-10 14:13:11"), "3 months, centered at 2005-06-16 00:00:00Z", "i");
        Test.ensureEqual(getLegendTime("1 year",       "2005-06-10 14:13:11"), "1 year, centered at 2005-06-01 00:00:00Z", "j");
        Test.ensureEqual(getLegendTime("5 year",       "2005-06-10 14:13:11"), "5 years, centered at 2005-07-01 00:00:00Z", "k");
        Test.ensureEqual(getLegendTime("10 year",      "2005-06-10 14:13:11"), "10 years, centered at 2005-01-01 00:00:00Z", "l");
        Test.ensureEqual(getLegendTime("all",          "2005-06-10 14:13:11"), "All available", "m");
        */

        //getLegendTime
        /*  never finished
        String center = "2005-06-16 14:13:11";
        Test.ensureEqual(getLegendTime("pass",          center, minT, maxT), "2005-06-16 14:13:11Z", "");
        Test.ensureEqual(getLegendTime("1 observation", center, minT, maxT), "2005-06-16 14:13:11Z", "");
        Test.ensureEqual(getLegendTime(_25HOUR_OPTION,  center, minT, maxT), "2005-06-15 21:30:00Z to 2005-06-16 22:30:00Z", "c");
        Test.ensureEqual(getLegendTime(_33HOUR_OPTION,  center, minT, maxT), "2005-06-15 17:30:00Z to 2005-06-17 02:30:00Z", "d");
        Test.ensureEqual(getLegendTime("1 day",         center, minT, maxT), "2005-06-16 00:00:00Z to 2005-06-17 00:00:00Z", "e");
        Test.ensureEqual(getLegendTime("3 day",         center, minT, maxT), "2005-06-15 00:00:00Z to 2005-06-18 00:00:00Z", "f");
        Test.ensureEqual(getLegendTime("5 day",         center, minT, maxT), "2005-06-14 00:00:00Z to 2005-06-19 00:00:00Z", "h");
        Test.ensureEqual(getLegendTime("4 day",         center, minT, maxT), "2005-06-14 00:00:00Z to 2005-06-18 00:00:00Z", "g");
        Test.ensureEqual(getLegendTime("8 day",         center, minT, maxT), "2005-06-12 00:00:00Z to 2005-06-20 00:00:00Z", "i");
        Test.ensureEqual(getLegendTime("10 day",        center, minT, maxT), "2005-06-11 00:00:00Z to 2005-06-21 00:00:00Z", "j");
        Test.ensureEqual(getLegendTime("1 month",       center, minT, maxT), "2005-06-01 00:00:00Z to 2005-07-01 00:00:00Z", "k");
        Test.ensureEqual(getLegendTime("monthly",       center, minT, maxT), "2005-06-01 00:00:00Z to 2005-07-01 00:00:00Z", "l");
        Test.ensureEqual(getLegendTime("3 month",       center, minT, maxT), "2005-05-01 00:00:00Z to 2005-08-01 00:00:00Z", "m");
        Test.ensureEqual(getLegendTime("1 year",        center, minT, maxT), "2005-01-01 00:00:00Z to 2006-01-01 00:00:00Z", "n");
        Test.ensureEqual(getLegendTime("5 year",        center, minT, maxT), "2003-01-01 00:00:00Z to 2008-01-01 00:00:00Z", "o");
        Test.ensureEqual(getLegendTime("10 year",       center, minT, maxT), "2000-01-01 00:00:00Z to 2010-01-01 00:00:00Z", "p");
        Test.ensureEqual(getLegendTime("all",           center, minT, maxT), "1971-02-03 01:00:00Z to 2006-04-05 02:00:00Z", "q");
        */

        //closestTimePeriod
        String tOptions[] = {"pass", "1 day", "3 day", "10 day", 
        "14 day", "1 month", "3 month", "1 year", "10 year", "all"};
        Test.ensureEqual(tOptions[closestTimePeriod("pass",         tOptions)], "pass",    "");
        Test.ensureEqual(tOptions[closestTimePeriod("1 observation",tOptions)], "pass",    "");
        Test.ensureEqual(tOptions[closestTimePeriod(_25HOUR_OPTION, tOptions)], "1 day",   "");
        Test.ensureEqual(tOptions[closestTimePeriod("8 day",        tOptions)], "10 day",  "");
        Test.ensureEqual(tOptions[closestTimePeriod("monthly",      tOptions)], "1 month", "");
        Test.ensureEqual(tOptions[closestTimePeriod("5 year",       tOptions)], "1 year",  "");
        Test.ensureEqual(tOptions[closestTimePeriod("20 year",      tOptions)], "10 year", "");
        Test.ensureEqual(tOptions[closestTimePeriod("Nate",         tOptions)], "1 day",   ""); //not an option

        //closestTimePeriod
        tOptions = new String[] {"pass", "1 day", "3 day", "10 day", 
        "14 day", "1 month", "3 month", "1 year", "10 year", "all"};
        Test.ensureEqual(tOptions[closestTimePeriod(0,         tOptions)], "pass",    "");
        Test.ensureEqual(tOptions[closestTimePeriod(24,        tOptions)], "1 day",    "");
        Test.ensureEqual(tOptions[closestTimePeriod(25,        tOptions)], "1 day",   "");
        Test.ensureEqual(tOptions[closestTimePeriod(8*24,      tOptions)], "10 day",  "");
        Test.ensureEqual(tOptions[closestTimePeriod(31*24,     tOptions)], "1 month", "");
        Test.ensureEqual(tOptions[closestTimePeriod(366*24,    tOptions)], "1 year",  "");
        Test.ensureEqual(tOptions[closestTimePeriod(10*365*24, tOptions)], "10 year", "");
        Test.ensureEqual(tOptions[closestTimePeriod(25*365*24, tOptions)], "all", "");
        Test.ensureEqual(tOptions[closestTimePeriod("Nate",    tOptions)], "1 day",   ""); //not an option

        //getTitles
        String tOptions2[] = {"pass", "3 day"};
        Test.ensureEqual(getTitles(tOptions2), 
            new String[]{"Specify the length of time in which you are interested.", //first one is generic
                "Get the data from one observation.", "Get the mean of 3 days' data."}, "");

        //validateBeginDate
        Test.ensureEqual(validateBeginTime("2003-01-05", "2004-02-03", "1 day"),   "2003-01-05 12:00:00", ""); //already valid
        Test.ensureEqual(validateBeginTime("2005-01-05", "2004-02-03", "3 day"),   "2004-01-03 00:00:00", ""); //one month back
        Test.ensureEqual(validateBeginTime("",           "2004-02-03", "1 month"), "2003-02-03 00:00:00", ""); //one year back

        //endCalendarToCenteredTime(int timePeriodNHours, GregorianCalendar cal, String errorInMethod) 
        GregorianCalendar cal;
        cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
        endCalendarToCenteredTime(0, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 11:00:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
        endCalendarToCenteredTime(1, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 11:00:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
        endCalendarToCenteredTime(25, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 22:30:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
        endCalendarToCenteredTime(33, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 18:30:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-09-11 00:00:00");
        endCalendarToCenteredTime(24, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 12:00:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-09-11 00:00:00");
        endCalendarToCenteredTime(3*24, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 12:00:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-09-11 00:00:00");
        endCalendarToCenteredTime(8*24, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-07 00:00:00", "");

        cal = Calendar2.parseISODateTimeZulu("2006-11-01 00:00:00"); 
        endCalendarToCenteredTime(30*24, cal, "");        
        Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-10-16 12:00:00", "");

 
        //centeredTimeToOldStyleEndOption(int timePeriodNHours, String centeredTime) {
        //comments about (don't) include hms are based on CWBrowser before change to centered time
        Test.ensureEqual(centeredTimeToOldStyleEndOption(    0, "2006-09-10 11:00:00"), "2006-09-10 11:00:00", ""); //yes, include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption(    1, "2006-09-10 11:00:00"), "2006-09-10 11:00:00", ""); //yes, include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption(   25, "2006-09-10 08:30:00"), "2006-09-10 21:00:00", ""); //yes, include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption(   33, "2006-09-10 08:30:00"), "2006-09-11 01:00:00", ""); //yes, include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption(   24, "2006-09-10 12:00:00"), "2006-09-10", ""); //last day, inclusive; no, don't include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption( 3*24, "2006-09-10 12:00:00"), "2006-09-11", ""); //last day, inclusive; no, don't include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption( 8*24, "2006-09-10 00:00:00"), "2006-09-13", ""); //last day, inclusive; no, don't include hms
        Test.ensureEqual(centeredTimeToOldStyleEndOption(30*24, "2006-09-16 00:00:00"), "2006-09-30", ""); //last day, inclusive; no, don't include hms

        //oldStyleEndOptionToCenteredTime(int timePeriodNHours, String centeredTime) {
        //comments about (don't) include hms are based on CWBrowser before change to centered time
        Test.ensureEqual(oldStyleEndOptionToCenteredTime(    0, "2006-09-10 11:00:00"), "2006-09-10 11:00:00", ""); //yes, include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime(    1, "2006-09-10 11:00:00"), "2006-09-10 11:00:00", ""); //yes, include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime(   25, "2006-09-10 21:00:00"), "2006-09-10 08:30:00", ""); //yes, include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime(   33, "2006-09-11 01:00:00"), "2006-09-10 08:30:00", ""); //yes, include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime(   24, "2006-09-10"),          "2006-09-10 12:00:00", ""); //last day, inclusive; no, don't include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime( 3*24, "2006-09-11"),          "2006-09-10 12:00:00", ""); //last day, inclusive; no, don't include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime( 8*24, "2006-09-13"),          "2006-09-10 00:00:00", ""); //last day, inclusive; no, don't include hms
        Test.ensureEqual(oldStyleEndOptionToCenteredTime(30*24, "2006-09-30"),          "2006-09-16 00:00:00", ""); //last day, inclusive; no, don't include hms


    }
}
