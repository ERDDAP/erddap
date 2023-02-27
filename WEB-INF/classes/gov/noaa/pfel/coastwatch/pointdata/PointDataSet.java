/* 
 * PointDataSet Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class represents one point dataset for CWBrowser.
 * It determine the dates for which data is available and
 * it can generate a table with the data for a specific time period.
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-01
 */
public abstract class PointDataSet implements Comparable { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false;

    /**
     * The options for timePeriod.
     */
    public final static String timePeriodOptions[] = {
        //Each of these must be in OneOf.timePeriodOptions.
        "1 observation", "1 day",
        "3 day", "8 day", "10 day", "14 day", "1 month"};
        //, "3 month", "1 year", "5 year", "10 year", "20 year", "all"};

    /**
     * The titles (tooltips) associated with each timePeriodOptions.
     */
    public static String timePeriodTitles[] = TimePeriods.getTitles(timePeriodOptions);


    /** The default min for the palette range for alternate units. */
    public double altPaletteMin;

    /** The default min for the palette range for alternate units. */
    public double altPaletteMax;

    /**
     * altScaleFactor is the scale factor to convert values in standard units 
     * to altUnits: altUnits = standardUnits * altScale + altOffset.
     * This is ignored if altUnits is "".
     * Set by the constructor.
     */
    public double altScaleFactor; 

    /**
     * altOffset is the offset to be added scale factor to convert values in standard units 
     * to altUnits: altUnits = standardUnits * altScale + altOffset.
     * This is ignored if altUnits is "".
     * Set by the constructor.
     */
    public double altOffset; 

    /** 
     * The bold title for the legend, e.g., SST.
     * Try really hard to keep less than 60 characters.
     * Set by the constructor.
     * From standard file: main variable's "long_name" (CF).
     */
    public String boldTitle; 

    /** 
     * The courtesy line for the legend, e.g., NOAA NESDIS OSDPD. 
     * Try really hard to keep less than 50 characters.
     * Set by the constructor ("" if unused).
     * From standard file: dataset's "acknowledgement" (ACDD) or
     *   "creator_name" (ACDD) or "project" (ACDD).
     */
    public String courtesy; 

    /**
     * Default units determines whether the standard units or the 
     * alternate units are the default units.
     * The value is either 'S' (for the standard SI units)
     * or 'A' (for the alternate units).
     */
    public char defaultUnits;

    /**
     * The type of data (e.g., PAType.FLOAT, PAType.DOUBLE, or PAType.STRING)
     * in a form suitable for PrimitiveArrays.
     */
    public PAType elementType;

    /** 
     * The xxxxFGDC substitution info for this dataset (null if not used). 
     * Set by the constructor.
     */
    //public String fgdcSubstitutions[]; 

    /** The first time for which data is available. */
    public GregorianCalendar firstTime; //set by the constructor

    /** The name of the data variable in the data file. */
    public String inFileVarName;

    /** 
     * The internal name for the dataset in the form P<2 letter group><4 letter id>,
     * e.g., PNBwspd. 
     * Set by the constructor.
     */ 
    public String internalName; 

    /** The last time for which data is available. */
    public GregorianCalendar lastTime; //set by the constructor

    /** 
     * The value of dayTillDataAccessAllowed, e.g., 0 or 14.
     * -1 is common (to avoid roundoff trouble).
     * Set by the constructor.
     */
    public int daysTillDataAccessAllowed;

    /** 
     * The name that will appear as a radio button option on the screen, e.g., Wind Speed (NDBC). 
     * Set by the constructor.
     */ 
    public String option; 

    /** 
     * The name of the default palette, e.g., Rainbow. 
     * Must be one of the palettes available to PointDataSets in the browser.
     * Set by the constructor.
     */
    public String palette; 

    /** The default min for the palette range for standard units. */
    public double paletteMin;

    /** The default max for the palette range for standard units. */
    public double paletteMax;

    /** 
     * The name of the default palette scale, e.g., Linear. 
     * Set by the constructor.
     */
    public String paletteScale; 

    /** 
     * The tooltip for the HTML radio button, with a description of the dataset. 
     * Set by the constructor.
     */
    public String tooltip; 

    /**
     * The units options: {udUnits} or {udUnits, altUdUnits}.
     * Set by the constructor.
     */
    public String unitsOptions[];

    /** The global attributes (raw, from the source). */
    public Attributes globalAttributes = new Attributes();
    /** The lon attributes (raw, from the source). */
    public Attributes xAttributes = new Attributes();
    /** The lat attributes (raw, from the source). */
    public Attributes yAttributes = new Attributes();
    /** The altitude attributes (raw, from the source). */
    public Attributes zAttributes = new Attributes();
    /** The time attributes (raw, from the source). */
    public Attributes tAttributes = new Attributes();
    /** The id attributes (raw, from the source). */
    public Attributes idAttributes = new Attributes(); 
    /** The data attributes (raw, from the source). */
    public Attributes dataAttributes = new Attributes();


    /**
     * This prints the most important information about this class to a string.
     *
     * @return 
     */
    public String toString() {
        return "PointDataSet " + internalName + 
            "\n    boldTitle=" + boldTitle + 
            " units=" + unitsOptions[0] +
            "\n    inFileVarName=" + inFileVarName +
            " firstTime=" + Calendar2.formatAsISODateTimeT(firstTime) + 
            " lastTime=" + Calendar2.formatAsISODateTimeT(lastTime); 
    }

    /**
     * This ensure that required values are present and constrained values
     * are valid.
     *
     * @throws Exception if trouble
     */
    public void ensureValid() throws Exception {
        String errorInMethod = String2.ERROR + " in PointDataSet.ensureValid for " + internalName + ":\n";
        
        //altXxx see below
        Test.ensureNotNull( boldTitle,                errorInMethod + "boldTitle is null!");
        Test.ensureNotEqual(boldTitle.length(), 0,    errorInMethod + "boldTitle.length is 0!");
        Test.ensureNotNull( courtesy,                 errorInMethod + "courtesy is null!");
        if (defaultUnits != 'S' && defaultUnits != 'A')
            Test.error(errorInMethod + "defaultUnits (" + defaultUnits + ") must be 'S' or 'A'!");
        //fgdcSubstitutions ... 
        Test.ensureNotNull( firstTime,                errorInMethod + "firstTime is null!");
        Test.ensureNotNull( inFileVarName,            errorInMethod + "inFileVarName is null!");
        Test.ensureNotEqual(inFileVarName.length(), 0,errorInMethod + "inFileVarName.length is 0!");
        Test.ensureNotNull( internalName,             errorInMethod + "internalName is null!");
        Test.ensureEqual(   internalName.length(), 7, errorInMethod + "internalName.length isn't 7!");
        Test.ensureNotNull( lastTime,                 errorInMethod + "lastTime is null!");
        //dayTillDataAccessAllowed allows any int
        Test.ensureNotNull( option,                   errorInMethod + "option is null!");
        Test.ensureNotEqual(option.length(), 0,       errorInMethod + "option.length is 0!");
        Test.ensureNotNull( palette,                  errorInMethod + "palette is null!");
        Test.ensureNotEqual(palette.length(), 0,      errorInMethod + "palette.length is 0!");
        Test.ensureNotNull( paletteScale,             errorInMethod + "paletteScale is null!");
        Test.ensureNotEqual(paletteScale.length(), 0, errorInMethod + "paletteScale.length is 0!");
        Test.ensureNotNull( tooltip,                  errorInMethod + "tooltip is null!");
        Test.ensureNotEqual(tooltip.length(), 0,      errorInMethod + "tooltip.length is 0!");
        Test.ensureNotNull( unitsOptions,             errorInMethod + "unitOptions is null!");
        Test.ensureBetween( unitsOptions.length, 1, 2,errorInMethod + "unitOptions.length isn't 1 or 2!");
        if (unitsOptions.length > 1) {
            Test.ensureNotEqual(altScaleFactor, Double.NaN, errorInMethod + "altScaleFactor is NaN!"); 
            Test.ensureNotEqual(altOffset,      Double.NaN, errorInMethod + "altOffset is NaN!"); 
        }

    } 

    /**
     * This gets the minTime (seconds since epoch) for one of the stations.
     *
     * @param stationID e.g., "M2" or "31201"
     * @return  the minTime (seconds since epoch) for one of the stations
     *    (or Double.NaN if stationID not found).
     */
    public abstract double getStationMinTime(String stationID);

    /**
     * Make a Table with a specific subset of the data.
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param minY the minimum acceptable latitude (degrees_north)
     * @param maxY the maximum acceptable latitude (degrees_north)
     * @param minZ the minimum acceptable altitude (meters, down is positive)
     * @param maxZ the maximum acceptable altitude (meters, down is positive)
     * @param isoMinT an ISO format date/time for the minimum ok time.
     *    isoMinT and isoMaxT are rounded to be a multiple of the frequency 
     *    of the data's collection.  For example, if the data is hourly, 
     *    they are rounded to the nearest hour.
     * @param isoMaxT an ISO format date/time for the maximum ok time
     * @return a Table with 6 columns: 
     *    <br>0) "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    <br>1) "LAT" (units=degrees_north), 
     *    <br>2) "DEPTH" (units=meters, positive=down), 
     *    <br>3) "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    <br>4) "ID" (String data), 
     *    <br>5) inFileVarName with data (unpacked, in standard units).
     *   <br>LON, LAT, DEPTH and TIME will be DoubleArrays; ID will be a StringArray; 
     *     inFileVarName will be a numeric PrimitiveArray 
     *     (not necessarily DoubleArray).  The data column will probably be elementType.
     *   <br>Rows with missing values are NOT removed.
     *   <br>The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   <br>The table will have the proper columns but may have 0 rows.
     * @throws Exception if trouble (e.g., ill-formed isoMinT, or minX > maxX)
     */
    public abstract Table makeSubset(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ,
        String isoMinT, String isoMaxT) throws Exception;

    /**
     * This is a convenience method to add one row to a subsetTable.
     */
    public static void addSubsetRow(Table table, double x, double y, double depth,
        double t, String id, double data) {
        
        table.getColumn(0).addDouble(x);
        table.getColumn(1).addDouble(y);
        table.getColumn(2).addDouble(depth);
        table.getColumn(3).addDouble(t);
        table.getColumn(4).addString(id);
        table.getColumn(5).addDouble(data);
    }

    /**
     * This calculates the average data value returned by makeSubset.
     */
    public double calculateAverage(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ,
        String isoMinT, String isoMaxT) throws Exception {

        Table table = makeSubset(minX, maxX, minY, maxY, minZ, maxZ,
            isoMinT, isoMaxT);
        PrimitiveArray pa = table.getColumn(5);
        double stats[] = pa.calculateStats();
        double n = stats[PrimitiveArray.STATS_N];
        double average = n == 0? Double.NaN : stats[PrimitiveArray.STATS_SUM]/n;
        String2.log("PointDataSet.calculateAverage" +
            " minT=" + Calendar2.epochSecondsToIsoStringTZ(table.getColumn(3).getDouble(0)) +
            " maxT=" + Calendar2.epochSecondsToIsoStringTZ(table.getColumn(3).getDouble(table.nRows() - 1)) +
            "\n  average=" + average);
        return average;
    }

    /**
     * This makes a table with the requested time period averages (e.g.,
     * daily 8-day averages centered on Jan 1, 1999 to Jan 7, 1999.
     * An average is made for each unique x,y,z,stationID combination in the 
     * relevant raw data.
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param minY the minimum acceptable latitude (degrees_north)
     * @param maxY the maximum acceptable latitude (degrees_north)
     * @param minZ the minimum acceptable altitude (meters, up is positive)
     * @param maxZ the maximum acceptable altitude (meters, up is positive)
     * @param isoMinT an ISO format date/time for the first average's centered date/time.
     * @param isoMaxT an ISO format date/time for the last average's centered date/time.
     * @param timePeriod one of the TimePeriods.timePeriodOptions (with a max length of "1 month").
     *    For timePeriod="1 observation", this method returns raw data.
     *    For nDay timePeriods, this method returns daily timePeriod-long averages.
     *    For timePeriod="1 month", this returns monthly averages of the data.
     * @return a Table with 6 columns: 
     *    <br>1) "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    <br>2) "LAT" (units=degrees_north), 
     *    <br>3) "DEPTH" (units=meters, positive=down), 
     *    <br>4) "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    <br>5) "ID" (String data), 
     *    <br>6) inFileVarName with data (unpacked, in standard units).
     *   <br>LON, LAT, DEPTH and TIME will be DoubleArrays; ID will be a StringArray; 
     *      the data column will be a numeric PrimitiveArray (not necessarily DoubleArray).
     *   <br>Rows with missing values are NOT removed.
     *   <br>The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   <br>The table will have the proper columns but may have 0 rows.
     * @throws Exception (e.g., if invalid timePeriod, isoMinT, or isoMaxT)
     */
    public Table makeAveragedTimeSeries(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ,
        String isoMinT, String isoMaxT, String timePeriod) throws Exception {

        String info = "PointDataSet.makeAveragedTimeSeries " + boldTitle + 
            "\n  mATS timePeriod=" + timePeriod + " isoMinT=" + isoMinT + " isoMaxT=" + isoMaxT;
        if (verbose) String2.log("//**** " + info);
        long time = System.currentTimeMillis();

        //validate input
        //makeSubset (below) does validation of all of input data except timePeriod

        //get timePeriodNHours
        int timePeriodNHours = TimePeriods.getNHours(timePeriod); //throws Exception if not found
        int timePeriodNSeconds = timePeriodNHours * Calendar2.SECONDS_PER_HOUR;
        int oneMonthsHours = TimePeriods.getNHours("1 month");
        Test.ensureTrue(timePeriodNHours <= oneMonthsHours, 
            String2.ERROR + " in " + info + ":\n timePeriod (" + timePeriod + ") is longer than 1 month.");
        boolean timePeriodIs1Month = timePeriodNHours == oneMonthsHours;

        //calculate the clean  isoMinT and maxT
        isoMinT = TimePeriods.getCleanCenteredTime(timePeriod, isoMinT);
        isoMaxT = TimePeriods.getCleanCenteredTime(timePeriod, isoMaxT);

        //calculate the minT and maxT (back 1 second) for getting the raw data   
        double rawMinT, rawMaxT;  //the range of data needed
        if (timePeriodIs1Month) {
            //convert start to start of min month
            GregorianCalendar gc = Calendar2.parseISODateTimeZulu(isoMinT); //throws Exception if trouble
            gc.set(Calendar.MILLISECOND, 0);
            gc.set(Calendar.SECOND, 0);
            gc.set(Calendar.MINUTE, 0);
            gc.set(Calendar.HOUR_OF_DAY, 0);
            gc.set(Calendar.DATE, 1);
            rawMinT = Calendar2.gcToEpochSeconds(gc);

            //convert end time to just before end of max month
            gc = Calendar2.parseISODateTimeZulu(isoMaxT); //throws Exception if trouble
            gc.set(Calendar.MILLISECOND, 0);
            gc.set(Calendar.SECOND, 0);
            gc.set(Calendar.MINUTE, 0);
            gc.set(Calendar.HOUR_OF_DAY, 0);
            gc.set(Calendar.DATE, 1);
            gc.add(Calendar.MONTH, 1);  //beginning of next month
            gc.add(Calendar.SECOND, -1);  //back 1 second
            rawMaxT = Calendar2.gcToEpochSeconds(gc);

        } else if (timePeriodNHours == 0) {
            //for one observation, no change to minT and maxT
            rawMinT = Calendar2.isoStringToEpochSeconds(isoMinT); //isoMinT cleaned above
            rawMaxT = Calendar2.isoStringToEpochSeconds(isoMaxT); //isoMaxT cleaned above

        } else { 
            //25hour, 33hour, and nDays  -- 1/2 time back, 1/2 time forward
            rawMinT = Calendar2.isoStringToEpochSeconds(isoMinT) - timePeriodNSeconds / 2;     //isoMinT cleaned above
            rawMaxT = Calendar2.isoStringToEpochSeconds(isoMaxT) + timePeriodNSeconds / 2 - 1; //isoMaxT cleaned above
        }
        double minT = Calendar2.isoStringToEpochSeconds(isoMinT); //isoMinT cleaned above
        double maxT = Calendar2.isoStringToEpochSeconds(isoMaxT); //isoMaxT cleaned above
        if (reallyVerbose) String2.log(
            "  mATS clean minT=" + isoMinT + " maxT=" + isoMaxT + 
            " rawMinT=" + Calendar2.epochSecondsToIsoStringTZ(rawMinT) +
            " rawMaxT=" + Calendar2.epochSecondsToIsoStringTZ(rawMaxT)); 

        //get the raw data
        Table rawTable = makeSubset(minX, maxX,
            minY, maxY, minZ, maxZ, 
            Calendar2.epochSecondsToIsoStringT(rawMinT), 
            Calendar2.epochSecondsToIsoStringT(rawMaxT));
        //!!!Note that the rawTable may have some data values beyond the end of the top time period
        //  because makeSubset rounds the times to find the boundaries. 
        //  But these extra data points are ignored by the strictly defined
        //  timePeriods.
        //String2.log("rawTable=" + rawTable);
        int rawNRows = rawTable.nRows();
        PrimitiveArray xPA    = rawTable.getColumn(0);
        PrimitiveArray yPA    = rawTable.getColumn(1);
        PrimitiveArray zPA    = rawTable.getColumn(2);
        PrimitiveArray timePA = rawTable.getColumn(3);
        PrimitiveArray idPA   = rawTable.getColumn(4);
        PrimitiveArray dataPA = rawTable.getColumn(5);

        //String2.log("PointDataSet rawTable=" + rawTable);
        //String2.log("  after makeSubset, rawTable data stats: " + dataPA.statsString());

        //for timePeriodNHours == 0, just return the raw data  (no averaging)
        if (timePeriodNHours == 0) {
            if (verbose) String2.log("\\\\**** PointDataSet.makeAveragedTimeSeries done. nRows=" + 
                rawTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "ms");
            return rawTable;
        }

        //make the resultTable  (like rawTable, but no data)
        Table resultTable = new Table();
        PrimitiveArray xResultPA    = PrimitiveArray.factory(xPA.elementType(), 4, false);
        PrimitiveArray yResultPA    = PrimitiveArray.factory(yPA.elementType(), 4, false);
        PrimitiveArray zResultPA    = PrimitiveArray.factory(zPA.elementType(), 4, false);
        PrimitiveArray tResultPA    = PrimitiveArray.factory(timePA.elementType(), 4, false);
        StringArray    idResultPA   = new StringArray();
        //but make sure data column is double or float, since it will hold floating point averages
        PAType tEt = dataPA.elementType();
        if (tEt != PAType.DOUBLE && tEt != PAType.FLOAT) 
            tEt = PAType.FLOAT; //arbitrary: should it be double? 
        PrimitiveArray dataResultPA = PrimitiveArray.factory(tEt, 4, false);
        resultTable.addColumn(rawTable.getColumnName(0), xResultPA);
        resultTable.addColumn(rawTable.getColumnName(1), yResultPA);
        resultTable.addColumn(rawTable.getColumnName(2), zResultPA);
        resultTable.addColumn(rawTable.getColumnName(3), tResultPA);
        resultTable.addColumn(rawTable.getColumnName(4), idResultPA);
        resultTable.addColumn(rawTable.getColumnName(5), dataResultPA);
        rawTable.globalAttributes().copyTo(resultTable.globalAttributes());
        for (int col = 0; col < 6; col++)
            rawTable.columnAttributes(col).copyTo(resultTable.columnAttributes(col));

        //calculate the timePeriod begin and end times
        DoubleArray tBeginTime    = new DoubleArray(); //exact begin time for data inclusion
        DoubleArray tEndTime      = new DoubleArray(); //exact end   time (1 second back) for data inclusion
        DoubleArray tCenteredTime = new DoubleArray(); //time reported to user
        if (timePeriodIs1Month) {
            //monthly        
            GregorianCalendar centerGc = Calendar2.epochSecondsToGc(minT); //center of month
            GregorianCalendar beginGc = (GregorianCalendar)centerGc.clone(); //begin of month
            beginGc.set(Calendar2.MINUTE, 0);
            beginGc.set(Calendar2.HOUR_OF_DAY, 0);
            beginGc.set(Calendar2.DATE, 1);
            while (Calendar2.gcToEpochSeconds(centerGc) <= maxT) {
                //beginTime is begin of month
                tBeginTime.add(Calendar2.gcToEpochSeconds(beginGc));
                tCenteredTime.add(Calendar2.gcToEpochSeconds(centerGc));
                beginGc.add(Calendar.MONTH, 1); //advance 1 month
                centerGc.add(Calendar.MONTH, 1); //advance 1 month
                Calendar2.centerOfMonth(centerGc);
                tEndTime.add(Calendar2.gcToEpochSeconds(beginGc) - 1); //end is 1 second before start of next month
            }
        } else {     
            //25,33hour and nDay time periods  
            double centerT = minT; 
            double timePeriodIncrementSeconds = 
                timePeriodNHours % 24 == 0?  Calendar2.SECONDS_PER_DAY : //nDays every day
                    Calendar2.SECONDS_PER_HOUR;  //25 or 33 hour every hour                
            while (centerT <= maxT) {
                tBeginTime.add(centerT - timePeriodNSeconds / 2);
                tCenteredTime.add(centerT);
                tEndTime.add(centerT + timePeriodNSeconds / 2 - 1);
                centerT += timePeriodIncrementSeconds;
            }
        }
        int nTimePeriods = tBeginTime.size();
        double timePeriodBeginTime[]    = tBeginTime.toArray();    tBeginTime = null;
        double timePeriodEndTime[]      = tEndTime.toArray();      tEndTime = null;
        double timePeriodCenteredTime[] = tCenteredTime.toArray(); tCenteredTime = null;
        if (nTimePeriods == 0) {
            if (verbose) String2.log("\\\\**** PointDataSet.makeAveragedTimeSeries done. nRows=" + 
                resultTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "ms");
            return resultTable;
        }

        //interesting tests, but don't leave on all the time
        //Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(minT), 
        //                 Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[0]), 
        //                 "minT!=center[0]");
        //Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(maxT), 
        //                 Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[nTimePeriods - 1]), 
        //                 "maxT!=center[last]");
        double timePeriodSum[]   = new double[nTimePeriods];
        int    timePeriodCount[] = new int[nTimePeriods];
        if (reallyVerbose) {
            String2.log("  mATS nTimePeriods=" + nTimePeriods);
            for (int i = 0; i < Math.min(3, nTimePeriods); i++)
                String2.log("    mATS timePeriod " + i + ": " +
                    Calendar2.epochSecondsToIsoStringTZ(timePeriodBeginTime[i]) + " to " + 
                    Calendar2.epochSecondsToIsoStringTZ(timePeriodEndTime[i]) + " center=" +
                    Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[i]));
            String2.log("    mATS timePeriod " + (nTimePeriods - 1) + ": " +
                Calendar2.epochSecondsToIsoStringTZ(timePeriodBeginTime[nTimePeriods - 1]) + " to " + 
                Calendar2.epochSecondsToIsoStringTZ(timePeriodEndTime[nTimePeriods - 1]) + " center=" +
                Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[nTimePeriods - 1]));
        }

        //make a new table with the averaged data
        int row = 0; //the next row to be read
        while (row < rawNRows) {
            //set up to read one x,y,z,id's data;  different time periods
            double stationX = xPA.getDouble(row);
            double stationY = yPA.getDouble(row);
            double stationZ = zPA.getDouble(row);
            String stationID = idPA.getString(row);
            Arrays.fill(timePeriodSum, 0);
            Arrays.fill(timePeriodCount, 0);

            //process this row's data (till next station or end of file)
            while (row < rawNRows) {
                //if x,y,z, or id is different, we're done
                if (xPA.getDouble(row) != stationX ||
                    yPA.getDouble(row) != stationY ||
                    zPA.getDouble(row) != stationZ ||
                    !idPA.getString(row).equals(stationID)) {
                    //don't advance row, so this row included in next group
                    break;
                }
                
                //is data not of interest? 
                double tValue = dataPA.getDouble(row);
                if (Double.isNaN(tValue)) {
                    row++;
                    continue;        
                }
                //String2.log("  PointDataSet.getAveragedTimeSeries datum=" + tValue);
                
                //include this data in relevant time periods
                //firstGE and lastLE identify the section of relevant time periods
                //this is tricky; draw a diagram
                double tTime = timePA.getDouble(row);
                int first = Math2.binaryFindFirstGAE(timePeriodEndTime,   tTime, 9); //first of interest
                int last  = Math2.binaryFindLastLAE( timePeriodBeginTime, tTime, 9); //last of interest
                /*
                if (first > last) 
                    String2.log(String2.ERROR + " in " + info + ":\n  firstGE or lastLE error:\n " +
                        " firstGE=" + first + 
                        " lastLE=" + last + " n=" + nTimePeriods + 
                        " time=" + tTime + "=" + Calendar2.epochSecondsToIsoStringTZ(tTime));
                        //+ "\n\nbeginTimes=" + timePeriodBeginTime +
                        //"\n\nendTimes=" + timePeriodEndTime);
                */

                for (int i = first; i <= last; i++) {
                    //time periods overlap. if time specifically ok for this timePeriod, tally it
                    if (tTime >= timePeriodBeginTime[i] &&
                        tTime <= timePeriodEndTime[i]) {
                        timePeriodSum[i] += tValue;
                        timePeriodCount[i]++;
                    }
                }
                row++;
            }

            //add a row to result table (even if count==0)
            //String2.log("PointDataSet break for next group at row=" + row + " stationID=" + stationID);
            for (int i = 0; i < nTimePeriods; i++) {
                xResultPA.addDouble(stationX);
                yResultPA.addDouble(stationY);
                zResultPA.addDouble(stationZ);
                tResultPA.addDouble(timePeriodCenteredTime[i]);
                idResultPA.addString(stationID);
                dataResultPA.addDouble(timePeriodCount[i] == 0?
                    Double.NaN :
                    timePeriodSum[i] / timePeriodCount[i]);
                //String2.log("  PointDataSet.getAveragedTimeSeries average=" + 
                //   dataResultPA.get(dataResultPA.size() - 1) + " n=" + timePeriodCount[i]);
            }        

        }

        //set Attributes    ('null' says make no changes  (don't use ""))
        resultTable.setAttributes(0, 1, 2, 3, boldTitle, 
            null, //cdmDataType,   
            null, //creatorEmail set by makeSubset //who is creating this file...
            null, //creatorName  set by makeSubset 
            null, //creatorUrl   set by makeSubset 
            null, //project      set by makeSubset
            null, //id, 
            null, //keywordsVocabulary,
            null, //keywords, 
            null, //references, 
            null, //summary, 
            courtesy,
            "Centered Time" + 
                (TimePeriods.getNHours(timePeriod) > 0? " of " + timePeriod + " Averages" : ""));
        resultTable.globalAttributes().remove("time_coverage_resolution"); //e.g., my ndbc files have this set to P1H

        //sort by ID, t, z, y, x
        resultTable.sort(new int[]{4, 3, 2, 1, 0}, //keys
            new boolean[]{true, true, true, true, true}); //ascending

        //return the resultTable
        //String2.log("PointDataSet.makeAveragedTS resultsTable=" + resultsTable);
        //String2.log("PointDataSet.makeAveragedTS resultsTable data stats=" + resultsTable.getColumn(5).statsString());
        if (verbose) String2.log("\\\\**** PointDataSet.makeAveragedTimeSeries done. nRows=" + 
            resultTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "ms\n");
        return resultTable;

    }

    /**
     * This appends data about stations 
     * (which have data within an x,y,z,t bounding box)
     * to a table of stations.
     * Future: for trajectories, this could be the x,y,z of the first
     *   matching point in the trajectory.
     *
     * <p>Typical use of this is:
     * <ol>
     * <li> Table stations = PointDataSet.getEmptyStationTable(
     *     0, 360, -90, 90, -100, 100, "1900-01-01", "3000-01-01");
     * <li> pointDataSets[i].addStationsToTable(
            0, 360, -90, 90, -100, 100, "1900-01-01", "3000-01-01", stations);
     * <li> stations.setActualRangeAndBoundingBox(0,1,2,-1,3);
     * </ol>
     *
     * @param minX  the minimum acceptable longitude (degrees_east).
     *    minX and maxX may be -180 to 180, or 0 to 360.
     * @param maxX  the maximum acceptable longitude (degrees_east)
     * @param minY  the minimum acceptable latitude (degrees_north)
     * @param maxY  the maximum acceptable latitude (degrees_north)
     * @param minZ  the minumum acceptable altitude (meters, positive=up)
     * @param maxZ  the maxumum acceptable altitude (meters, positive=up)
     * @param isoMinT an ISO format date/time for the minimum acceptable time  
     * @param isoMaxT an ISO format date/time for the maximum acceptable time  
     * @param stations a Table with 4 columns (LON, LAT, DEPTH, ID),
     *    where lat is in degrees_east adjusted to be in the minX maxX range,
     *    lon is in degrees_north, depth is in meters down,
     *    and ID is a string suitable for sorting (e.g., MBARI MO).
     * @throws Exception if trouble (e.g., isoMinT is invalid)
     */
    public abstract void addStationsToTable(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ, 
        String isoMinT, String isoMaxT, Table stations) throws Exception;

    /**
     * This returns an empty table suitable for getStations.
     *
     * @param minX  the minimum acceptable longitude (degrees_east).
     *    minX and maxX may be -180 to 180, or 0 to 360.
     * @param maxX  the maximum acceptable longitude (degrees_east)
     * @param minY  the minimum acceptable latitude (degrees_north)
     * @param maxY  the maximum acceptable latitude (degrees_north)
     * @param minZ  the minumum acceptable altitude (meters, positive=up)
     * @param maxZ  the maxumum acceptable altitude (meters, positive=up)
     * @param isoMinT an ISO format date/time for the minimum acceptable time  
     * @param isoMaxT an ISO format date/time for the maximum acceptable time  
     * @return an empty table suitable for getStations: 4 columns (LON, LAT, DEPTH, ID)
     *    where lat is in degrees_east adjusted to be in the minX maxX range,
     *    lon is in degrees_north, depth is in meters down,
     *    and ID is a string suitable for sorting (e.g., MBARI MO).
     *   and prelimary metadata. Use table.setActualRangeAndBoundingBox
     *   after adding data to add the rest of the metadata.
     */
    public static Table getEmptyStationTable(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ, 
        String isoMinT, String isoMaxT) {

        //create the table
        Table stations = new Table();
        stations.addColumn("LON", new DoubleArray());
        stations.addColumn("LAT",  new DoubleArray());
        stations.addColumn("DEPTH",  new DoubleArray());
        stations.addColumn("ID",  new StringArray());

        //add metadata from conventions
        //see gov/noaa/pfel/coastwatch/data/MetaMetadata.txt
        //see NdbcMetStation for comments about metadatastandards requirements
        Attributes lonAttributes = stations.columnAttributes(0);
        lonAttributes.set("_CoordinateAxisType", "Lon");
        lonAttributes.set("axis", "X");
        lonAttributes.set("long_name", "Longitude");
        lonAttributes.set("standard_name", "longitude");
        lonAttributes.set("units", "degrees_east");

        Attributes latAttributes = stations.columnAttributes(1);
        latAttributes.set("_CoordinateAxisType", "Lat");
        latAttributes.set("axis", "Y");
        latAttributes.set("long_name", "Latitude");
        latAttributes.set("standard_name", "latitude");
        latAttributes.set("units", "degrees_north");

        Attributes zAttributes = stations.columnAttributes(2);
        zAttributes.set("_CoordinateAxisType", "Height");
        zAttributes.set("axis", "Z");
        zAttributes.set("long_name", "Depth");
        zAttributes.set("standard_name", "depth");
        zAttributes.set("units", "m");

        Attributes idAttributes = stations.columnAttributes(3);
        idAttributes.set("long_name", "Station Identifier");
        idAttributes.set("units", DataHelper.UNITLESS);
        
        Attributes globalAttributes = stations.globalAttributes();
        globalAttributes.set("Conventions", "CF-1.6");
        String title = "Station Locations" +
            " (minX=" + minX + 
            ", maxX=" + maxX + 
            ", minY=" + minY + 
            ", maxY=" + maxY + 
            ", minZ=" + minZ + 
            ", maxZ=" + maxZ + 
            ", minT=" + isoMinT + 
            ", maxT=" + isoMaxT + ")";
        globalAttributes.set("title",  title);
        globalAttributes.set("keywords", "Oceans"); //part of line from http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
        //skip keywords vocabulary since not using it strictly
        globalAttributes.set("id", title); //2019-05-07 this is wrong but this code isn't used by ERDDAP. It should be internalName.substring(1).
        globalAttributes.set("naming_authority", "gov.noaa.pfel.coastwatch");
        globalAttributes.set("cdm_data_type", "Other");
        //skip 'history'
        String todaysDate = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);
        globalAttributes.set("date_created", todaysDate); 
        globalAttributes.set("creator_name", DataHelper.CW_CREATOR_NAME);
        globalAttributes.set("creator_url", "https://coastwatch.pfeg.noaa.gov");
        globalAttributes.set("creator_email", "erd.data@noaa.gov");
        globalAttributes.set("institution", DataHelper.CW_CREATOR_NAME);
        globalAttributes.set("project", "NOAA NESDIS CoastWatch");
        globalAttributes.set("acknowledgement", "Data is from many sources.");
        //globalAttributes.set("geospatial_lat_min",  lat);
        //globalAttributes.set("geospatial_lat_max",  lat);
        globalAttributes.set("geospatial_lat_units","degrees_north");
        //globalAttributes.set("geospatial_lon_min",  lon);
        //globalAttributes.set("geospatial_lon_max",  lon);
        globalAttributes.set("geospatial_lon_units","degrees_east");
        //globalAttributes.set("geospatial_vertical_min",  0.0); //actually, sensors are -1 ..~+5 meters
        //globalAttributes.set("geospatial_vertical_max",  0.0);
        globalAttributes.set("geospatial_vertical_units","meters");
        globalAttributes.set("geospatial_vertical_positive", "up"); //since some readings are above and some are below sea level
        //globalAttributes.set("time_coverage_start", Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(timeIndex, 0)));
        //double timeStats[] = table.getColumn(timeIndex).calculateStats();
        //globalAttributes.set("time_coverage_end",   Calendar2.epochSecondsToIsoStringTZ(timeStats[PrimitiveArray.STATS_MAX]));
        //globalAttributes.set("time_coverage_resolution", "P1H");
        globalAttributes.set("standard_name_vocabulary", "CF");
        globalAttributes.set("license", "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither NOAA, NDBC, CoastWatch, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.");
        globalAttributes.set("date_issued", todaysDate);
        //'comment' could include other information about the station, its history 
        globalAttributes.set("source", "station observation");
        //attributes unique in their opendap files
        //globalAttributes.set("quality", "Automated QC checks with periodic manual QC"); //an ndbc attribute?
        //globalAttributes.set("", ""}));
        //attributes for Google Earth
        //globalAttributes.set("Southernmost_Northing", lat);
        //globalAttributes.set("Northernmost_Northing", lat);
        //globalAttributes.set("Westernmost_Easting", lon);
        //globalAttributes.set("Easternmost_Easting", lon);

        return stations;    
    }


    /**
     * This implements Comparable so that these can be sorted (based on 
     * 'option').
     *
     * @param o another PointDataSet object
     */
    public int compareTo(Object o) {
        PointDataSet pds = (PointDataSet)o;
        return option.compareTo(pds.option);
    }


    /**
     * Find the closest time period.
     *
     * @param timePeriodValue  best if a timePeriodOption from above. But may also 
     *   be a grid-style time period (see GridDataSetCWLocal).
     * @return the index of the timePeriodOption from above for the closest
     *   time period (or "1 day"'s index if trouble).
     */
    public static int closestTimePeriod(String timePeriodValue) {
        return TimePeriods.closestTimePeriod(timePeriodValue, timePeriodOptions);
    }
    
    /**
     * This tests the methods of this class.
     */
    public static void main(String args[]) {

        //closestTimePeriod
        for (int i = 0; i < timePeriodOptions.length; i++) 
            Test.ensureTrue(TimePeriods.exactTimePeriod(timePeriodOptions[i]) >= 0, "" + i);

    }
}
