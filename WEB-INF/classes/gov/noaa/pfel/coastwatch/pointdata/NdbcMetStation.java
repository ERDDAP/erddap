/* 
 * NdbcMetStation Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class had methods to generate the NdbcMet.nc file with
 * the NDBC Meteorological Station data.
 *
 * <p>Buoy Standard Meteorological file problems:
 * <ul>
 * <li> The documentation ("https://www.ndbc.noaa.gov/measdes.shtml#stdmet")
 *    says station ID's are "5-digit",  
 *    but in reality they are "4 or 5 characters".
 *    This probably is the source of problems on 
 *    https://www.ndbc.noaa.gov/historical_data.shtml for 
 *    the EBxx buoys, where the name mistakenly has an 'H' at the end
 *    and the years appear as, e.g., "970." instead of, e.g., "1970".
 *    I see that https://www.ndbc.noaa.gov/staid.shtml says "5 character"
 *    and talks about the EB exceptions.
 * <li> There is documentation for the variable PRES, but the files have BAR instead.
 * <li> There is documentation for the variable WDIR, but the files have WD instead.
 * <li> Some files have a PTDY column. Some don't.
 * <li> Some files have a TIDE column. Some don't.
 * <li> Some files have a BAR column; some have BARO.
 * <li> Variations in file naming conventions:
 *   In 2005, the monthly historic files are named e.g., 41001a2005.txt[.gz]
 *   but November is just available as a text file named 41001.txt.
 *   In 2006, Jan, July, Aug, Sep names are e.g., 4100112006.txt.gz 
 *   and Feb, Mar, Apr, May, Jun, Oct names are just <5charID>.txt.
 *   And there are stray files of the other type in some directories.
 * <li> The missing values markers are different in different columns
 *   (e.g., 99.0, 99.00, 9999, 999.0) and in different files (e.g., MM).
 * <li> Tide info is missing (not even missing value Strings, e.g., "99.00") 
 *   from initial lines (but not all lines) of 
 *   https://www.ndbc.noaa.gov/data/view_text_file.php?filename=41001h2000.txt.gz&dir=/ftp/data/historical/stdmet/ .
 *   This seems to be true for all files for 2000.
 * <li> There is no station information for 42a01, 42a02, 42a03, 46a35, 46a54, 
 *   47072, 4f887, misma
 *   (e.g., try https://www.ndbc.noaa.gov/station_page.php?station=42a01)
 *   but there is historical data for it
 *   (see https://www.ndbc.noaa.gov/historical_data.shtml).
 * <li> eb52 (see https://www.ndbc.noaa.gov/station_page.php?station=eb52)
 *   has no stationType info ("GE"?). 
 *   Similarly, skmg1, spag1, and tybg1, have no stationType info
 *   ("US Navy Tower"?).
 *   Similarly 41037.
 * <li>Having to "screen scrape" to get the information about the buoys
 *   (e.g., from https://www.ndbc.noaa.gov/station_page.php?station=32301)
 *   is made even more difficult because the data is not labeled
 *   (e.g., "Station location: 4.2 S 5.9 W")
 *   and different information is available for different stations.
 * <li> It would be great if https://www.ndbc.noaa.gov/historical_data.shtml 
 *   had links to the station page for each station. 
 * <li> It would be great if there were an ASCII table (or web page with PRE tag)
 *   which had a list of stations (rows) and information about the station as
 *   columns (e.g., lat, lon, type, ...).
 * <li> I finally (2020-02-20) got assurance that the VIS for historical and
 *   the last 45 days are both in nautical miles
 *   and that TIDE for both are measured in feet.
 *   Both data types link to the same descriptions page.
 *   I note that on the station pages, VIS is in km and TIDE is in meters!
 *   Yea!: Starting with March 2007 files, files have units:
 *   VIS is nmiles and TIDE is in feet.
 * <li> I need assurance that the VIS on the last hour page
 *   has km as the units (they are listed as "kmi" which should probably be
 *   interpreted as 1000's of miles by udunits).
 * <li> Many of the files have duplicate rows or 
 *   rows with duplicate times but different data.
 *   E.g., see 42007h1996.txt, 1996-04-21 two adjacent sets of 24 hours of data,
 *   and 1996-07-02 through 05, then 1996-07-01 on.  
 *   (Is this the time where almost all stations have duplicate data (e.g., 42019 is same)?) 
 *   In the latter case, the row I choose to use is randomly selected. 
 *   (This is terrible, but how am I to choose?)
 *   But if the row I choose to use has missing values, they are replaced by
 *   values from the row I am discarding.
 * <li> To construct a continuous time series, I had to get data from
 *   6 directories from files with different file name conventions
 *   and different columns of data:
 *   <ol>
 *   <li> The yearly historical data.
 *   <li> The monthly historical data (Jan - Oct).
 *   <li> The monthly historical data (Nov) (different file names).
 *   <li> The Real Time data (e.g., last 45 days).
 *   <li> The last hour's data.
 *   <li> Lat, Lon, and other station information was screen-scraped from each
 *      station's page.
 *   </ol>
 *   And to get updates of latest information (e.g., last hour) for all buoys,  
 *   I have to go to another web page and put in a request.
 *   Yea!: Starting with March 2007 files, files have units on the second line and
 *   years are 4 digit years.
 * <li> Data gap: on Jan 20, 2006 I note that the 2005 yearly data is not yet 
 *   available, the 2005 December monthly data is not yet available, and the
 *   last 45 day real-time data is still only for 45 days. So the data for
 *   early December 2005 is not available anywhere. This wasn't fixed until
 *   the end of January.
 *   Feb 16 this is happening again: no data available for early Jan. 
 *   (fixed ~Feb 28, when 2005 and Jan data became available)
 * <li> The script that generates the last n hours of data (at 
 *   https://www.ndbc.noaa.gov/box_search.php    for example:
 *   https://www.ndbc.noaa.gov/box_search.php?lat1=90S&lat2=90N&lon1=180W&lon2=180E&uom=M&ot=A&time=1
 *   throws errors
 *   (like "Fatal error: Allowed memory size of 8388608 bytes exhausted 
 *   (tried to allocate 44 bytes) in /var/www/html/box_search.php on line 517")
 *   if the number of hours is 5 (and sometimes 3) or greater.  
 * <li> There are lots of odd values in the files (even historic). 
 *   Just a few (from historic):
 *   <ul>
 *   <li> WindDirection actual_range is -638 to 378 (not 0 - 360?)
 *   <li> BAR has values of 0 (like outer space).
 *   <li> VIS min is -159 (negative distance?)
 *   <li> WaveDirection max is 500 (not 0 - 360?)
 *   <li> atmp, wtmp, dewpt have max of 999.9 (missing value gone awry?)
 *   </ul>
 *   So I now set odd values (&lt;minAllowed or &gt;maxAllowed) to missing values.
 * <li> The directory listing for the monthly science quality files
 *   lists several files which don't exist, e.g., in 2006: Jul/bhbm3.txt.
 * <li> It is unfortunate that many of the buoys (40%?) don't have 
 *   data files with the last 5 day's info.  Instead, I have to do hour updates
 *   via 45 days files (which is a lot of wasted downloading).
 *   (Now I get 45 day file only if 5 day file not available.)
 * <li>2008-10-09: There is a new problem with several buoys having empty 
ASCII historical data files for June 2008:
4202262008.txt
4601562008.txt
4609162008.txt
bhrc362008.txt
clkn762008.txt
hmra262008.txt
maxt262008.txt
pmaf162008.txt
rprn662008.txt
rtyc162008.txt
For example, see
The June link at 
https://www.ndbc.noaa.gov/station_history.php?station=rtyc1
then click on rtyc162008.txt 
which leads to  
https://www.ndbc.noaa.gov/view_text_file.php?filename=rtyc162008.txt.gz&dir=data/stdmet/Jun/
which appears to be an empty file.

This problem doesn't occur for data files before or after June 2008.
 * <li>2011-02-28 The yearly 42008 files have no data 
 *    42008h1980.txt.gz 42008h1981.txt.gz  42008h1982.txt.gz 
 *    42008h1983.txt.gz 42008h1984.txt.gz 
 *    But in the last download all data, they did have data!
 *    Other small files may have no data
 *    https://www.ndbc.noaa.gov/data/historical/stdmet/?C=S;O=A
 * <li>2020-02-06 For some time the data has been accessible via a THREDDS
 *    at NDBC, but it just offers 1 dataset from 1 file, e.g.,
 *    https://dods.ndbc.noaa.gov/thredds/catalog/data/stdmet/46088/catalog.html
 *    so different datasets use different units, different var names, different 
 *    missing_value.  There is no way to get one, standardized time series.
 * </ul>
 *
 * <p>The .nc files created by this class NO LONGER EXACTLY follow the 
 * Unidata Observation Dataset Convention (see
 * https://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html).
 * Observation Dataset Conventions because the Coventions have been abandoned.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-12-22
 */
public class NdbcMetStation  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    public final static String n5DayBaseUrl  = "https://www.ndbc.noaa.gov/data/5day2/";
    public final static String n45DayBaseUrl = "https://www.ndbc.noaa.gov/data/realtime2/";
    public final static String n5DaySuffix   = "_5day.txt";
    public final static String n45DaySuffix  = ".txt";

    /** An iso date time identifying the separation between historical data (quality controlled)
     * and near real time data (less quality controlled).
     * This changes every month when I get the latest historical data.
     * For the processing on the ~25th, change this to the beginning of this month.
     */
    public static String firstNearRealTimeData = "2022-09-01T00:00:00";
    /** Change current year ~Feb 15 when Jan monthly historical files become available. */
    public static String HISTORICAL_FILES_CURRENT_YEAR = "2022";  

    public final static String ID_NAME = "ID";

    /** The names of the columns in the ndbc files created by this class. */ 
    public final static String metColumnNames[] = { //after 'time' has replaced YYYY MM DD hh [mm]
  /* 0*/"LON", "LAT", "DEPTH", "TIME", ID_NAME, //use names that Lynn used in file that worked
  /* 5*/"WD", "WSPD", "GST", "WVHT", "DPD", //most common name in ndbc files
  /*10*/"APD", "MWD", "BAR", "ATMP", "WTMP", 
  /*15*/"DEWP", "VIS", "PTDY", "TIDE", "WSPU", 
  /*20*/"WSPV"};
    /** The 4 lower case characters used as internal names in the browsers. */
    public final static String internalNames[] = { 
  /* 0*/"long", "lati", "dpth", "time", "iden", 
  /* 5*/"wdir", "wspd", "wgst", "wvht", "dwpd", 
  /*10*/"awpd", "mwvd", "aprs", "atmp", "wtmp", 
  /*15*/"dewp", "visi", "ptdy", "tide", "wspu", 
  /*20*/"wspv"};
    /** The column numbers for each variable.*/
    public final static int 
  /* 0*/lonIndex = 0, latIndex = 1, depthIndex = 2, timeIndex = 3, idIndex = 4, 
  /* 5*/wdIndex = 5, wspdIndex = 6, gstIndex = 7, wvhtIndex = 8, dpdIndex = 9, 
  /*10*/apdIndex = 10, mwdIndex = 11, aprsIndex = 12, atmpIndex = 13, wtmpIndex = 14,  
  /*15*/dewpIndex = 15, visIndex = 16, ptdyIndex = 17, tideIndex = 18, wspuIndex = 19, 
  /*20*/wspvIndex = 20;

    /** ID_NAME is String. shortColumnNames are short. "TIME" is double. All others are floats. */
    public final static String shortColumnNames[] = new String[]{
        "#YY", "YY", "YYYY", "MM", "DD", "hh", "mm", "WDIR", "WD", "MWD"};    

    //Units conversions: readStationTxt converts
    //  VIS from nautical miles to km:  old*kmPerNMile;
    //  and converts TIDE feet to meters: old*meterPerFoot;
    /** The number of decimalDigits for each column. */
    public final static int decimalDigits[] = { 
  /* 0*/ 2, 2, 0, 0, 0,   
  /* 5*/ 0, 1, 1, 2, 2,   
  /*10*/ 2, 0, 1, 1, 1,   
  /*15*/ 1, 1, 1, 2, 1, 
  /*20*/ 1};
    /** The long names for each column. */
    public final static String longNames[] = { 
        //try to keep < 20 characters so boldTitle fits on legend
        //Wave Height was "Significant Wave Height"
        //Air Pressure was Sea Level Pressure
  /* 0*/"Longitude", "Latitude", "Depth", "Time", "Station Identifier", 
  /* 5*/"Wind Direction", "Wind Speed", "Wind Gust Speed", "Wave Height", "Wave Period, Dominant", 
  /*10*/"Wave Period, Average", "Wave Direction", "Air Pressure", "Air Temperature", "SST", 
  /*15*/"Dewpoint Temperature", "Station Visibility", "Pressure Tendency", "Water Level", "Wind Speed, Zonal", 
  /*20*/"Wind Speed, Meridional"};
    /** The palettes for each column. */
    public final static String palettes[] = {
  /* 0*/"BlueWhiteRed", "BlueWhiteRed", "BlueWhiteRed", "Rainbow", "Rainbow", 
  /* 5*/"BlueWhiteRed", "Rainbow", "Rainbow", "Rainbow", "Rainbow", 
  /*10*/"Rainbow", "BlueWhiteRed", "Rainbow", "Rainbow", "Rainbow", 
  /*15*/"Rainbow", "Rainbow","BlueWhiteRed", "BlueWhiteRed", "BlueWhiteRed", 
  /*20*/"BlueWhiteRed"};

//  /* 0*/"LON", "LAT", "DEPTH", "TIME", ID_NAME, //use names that Lynn used in file that worked
//  /* 5*/"WD", "WSPD", "GST", "WVHT", "DPD", //most common name in ndbc files
//  /*10*/"APD", "MWD", "BAR", "ATMP", "WTMP", 
//  /*15*/"DEWP", "VIS", "PTDY", "TIDE", "WSPU", 
//  /*20*/"WSPV"};
    /** The minAllowed and maxAllowed are my system to set nonsense values to missing value. */
    public final static double minAllowed[] = { 
  /* 0*/-180,-90,0,-2e9, 0,
  /* 5*/0,0,0,0,0,   
  /*10*/0,0,800,-200,-200, //wtmp min should be -5?  Or does this include ice?
  /*15*/-200,0,-15,-10,-98.9,
  /*20*/-98.9};    
    /** The minAllowed and maxAllowed are my system to set nonsense values to missing value. */
    public final static double maxAllowed[] = { 
  /* 0*/180,90,0,2e9, 0,
  /* 5*/359,98.9,98.9,98.9,98.9,  //98.9 to avoid erroneous mv=99
  /*10*/98.9,359,1200,50,50,  
  /*15*/50,98.9,15,10,98.9,
  /*20*/98.9};
    /** The suggested minimum value for the colorBar. */
    public final static double colorBarMin[] = {
  /* 0*/-180,-90,0,Double.NaN, Double.NaN,
  /* 5*/0,0,0,0,0, 
  /*10*/0,0,950,-10,0, 
  /*15*/0,0,-3,-5,-15,
  /*20*/-15};    
    /** The suggested maximum value for the colorBar. */
    public final static double colorBarMax[] = {
  /* 0*/180,90,0,Double.NaN,Double.NaN,
  /* 5*/360,15,30,10,20,
  /*10*/20,360,1050,40,32, 
  /*15*/40,100,3,5,15,
  /*20*/15};
    /** The udUnits for each column. */
    public final static String udUnits[] = {
  /* 0*/"degrees_east", "degrees_north", "m", Calendar2.SECONDS_SINCE_1970, null, 
  /* 5*/"degrees_true", "m s-1", "m s-1", "m", "s", 
  /*10*/"s", "degrees_true", "hPa", "degree_C", "degree_C", 
  /*15*/"degree_C", "km","hPa", "m", "m s-1", 
  /*20*/"m s-1"};
//  /* 0*/"LON", "LAT", "DEPTH", "TIME", ID_NAME, //use names that Lynn used in file that worked
//  /* 5*/"WD", "WSPD", "GST", "WVHT", "DPD", //most common name in ndbc files
//  /*10*/"APD", "MWD", "BAR", "ATMP", "WTMP", 
//  /*15*/"DEWP", "VIS", "PTDY", "TIDE", "WSPU", 
//  /*20*/"WSPV"};
    /** The comments for each column. Comments from https://www.ndbc.noaa.gov/measdes.shtml#stdmet */
    public final static String comments[] = {
  /* 0*/"The longitude of the station.", 
        "The latitude of the station.", 
        "The depth of the station, nominally 0 (see station information for details).",
        null,
        "The station identifier.",        
  /* 5*/"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD.",
        "Average wind speed (m/s).",
        "Peak 5 or 8 second gust speed (m/s).",
        "Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period.",
        "Dominant wave period (seconds) is the period with the maximum wave energy.",
  /*10*/"Average wave period (seconds) of all waves during the 20-minute period.",
        "Mean wave direction corresponding to energy of the dominant period (DOMPD).",
        "Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).",
        "Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.",
        "Sea surface temperature (Celsius). For sensor depth, see Hull Description.",
  /*15*/"Dewpoint temperature taken at the same height as the air temperature measurement.",
        "Station visibility (km, originally nautical miles in the NDBC .txt files). Note that buoy stations are limited to reports from 0 to 1.6 nmi.",
        "Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.",
        "The water level in meters (originally feet in the NDBC .txt files) above or below Mean Lower Low Water (MLLW).",
        "The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.",
  /*20*/"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed."};

        
    /** The standard names for each column from https://cfconventions.org/standard-names.html   */
    public final static String standardName[] = {
  /* 0*/"longitude", "latitude", "depth", "time", null, 
  /* 5*/"wind_from_direction", "wind_speed", "wind_speed_of_gust", "sea_surface_wave_significant_height", "sea_surface_swell_wave_period",
  /*10*/"sea_surface_swell_wave_period", "sea_surface_wave_to_direction", "air_pressure_at_sea_level", "air_temperature", "sea_surface_temperature",
  /*15*/"dew_point_temperature", "visibility_in_air", "tendency_of_air_pressure", "surface_altitude", "eastward_wind", 
  /*20*/"northward_wind"};

    /** The ioos_cateory names for each column   */
    public final static String ioosCategory[] = {
  /* 0*/"Location", "Location", "Location", "Time", "Identifier",
  /* 5*/"Wind", "Wind", "Wind", "Surface Waves", "Surface Waves",
  /*10*/"Surface Waves", "Surface Waves", "Pressure", "Temperature", "Temperature",
  /*15*/"Temperature", "Meteorology", "Pressure", "Sea Level", "Wind",
  /*20*/"Wind"};



    /** The courtesy info for map/graph legends. */
    public static final String courtesy = "NOAA NDBC and Other Station Owners/Operators";

    /** This sets of the column class of raw and finished data tables. 
     * ID_NAME is String. shortColumnNames are short. "TIME" is double. All others are floats. 
     *
     * @param table The just-read table (usually with all String columns).
     */
    public static void setColumnTypes(Table table) {
        int nCols = table.nColumns();
        for (int col = 0; col < nCols; col++) {
            String cName = table.getColumnName(col);
            PrimitiveArray pa = table.getColumn(col);
            if (cName.equals("#STN") || //in latest_obs.txt file
                cName.equals(ID_NAME)) ; //already String
            else if (cName.equals("TIME"))                   
                pa = new DoubleArray(pa);
            //"#YY", "YY", "YYYY", "MM", "DD", "hh", "mm", "WD", "MWD"
            else if (String2.indexOf(shortColumnNames, cName) >= 0) 
                pa = new ShortArray(pa);
            else pa = new FloatArray(pa);
            table.setColumn(col, pa);
        }
    }

    /**
     * This adds all the pointDataSets for all of the ndbc variables to the pointDataSet list.
     *
     * @param list the list of pointDataSets that will be added to
     * @param dir the directory with the files, with a slash at the end,
     *     e.g., c:/data/ndbc/ndbcMet/
     * @param minX the minimum acceptable station lon  (may be 0 - 360 or -180 - 180).
     * @param maxX the maximum acceptable station lon  (may be 0 - 360 or -180 - 180).
     * @param minY the minimum acceptable station lat.
     * @param maxY the maximum acceptable station lat.
     */
    public static void addPointDataSets(List list, String dir, double minX, double maxX,
        double minY, double maxY) {

        if (verbose) String2.log("\nNdbcMetStation.addPointDataSets starting...");

        //make the variableInfo strings for each variable
        //e.g., "WTMP` wtmp` Water Temperature` Rainbow`      Linear`   1`  8`  32` degree_C"},
        long time = System.currentTimeMillis();
        String variableInfo[] = new String[wspvIndex - wdIndex + 1];
        for (int col = wdIndex; col <= wspvIndex; col++) {
            variableInfo[col - wdIndex] = 
                metColumnNames[col] + "` " +  //WTMP` 
                "PNB" + internalNames[col] + "` " + //PNBwtmp` 
                longNames[col] + "` " +       //Water Temperature` 
                palettes[col] + "` " +        //Rainbow`      
                "Linear` " +                  //Linear`   
                "1` " +                       //1` 
                colorBarMin[col] + "` " +     //8`  
                colorBarMax[col] + "` " +     //32` 
                udUnits[col];                 //degree_C`
            
        }

        //make the pointDataSets
        int oldListSize = list.size();
        PointDataSetFromStationVariables.makePointDataSets(list,
            "NDBC", //String userDataSetBaseName, 
            dir, 
            ".+\\.nc",            //or all files
                //"(46088.nc)",   //or just 3 files
            variableInfo,  
            courtesy,
            minX, maxX, minY, maxY); 

        /* Now this metadata is not put in the files (so not on THREDDS)
        //remove the NDBCStation metadata 
        //it is trouble, because specific to one station, but dataset has for many stations
        for (int pdsi = oldListSize; pdsi < list.size(); pdsi++) {
            PointDataSet pds = (PointDataSet)list.get(pdsi);
            Attributes globalAtts = pds.globalAttributes;
            String[] names = globalAtts.getNames();
            for (int ni = 0; ni < names.length; ni++)
                if (names[ni].startsWith("NDBCStation"))
                    globalAtts.remove(names[ni]);
        }
        */

        if (verbose) String2.log("NdbcMetStation.addPointDataSets finished in " + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
    }


    /**
     * A variant of readStationTxtFile which reads data from an actual file.
     *
     * @param url   the url, already percentEncoded as needed
     * @param stationID the stationID which will be added to the table as a new column.
     *   For the lastest_obs.txt which already has a #STN column, this is ignored.
     * @param lon the longitude which will be added to the table as a new column.
     *   For the lastest_obs.txt which already has a LAT column, this is ignored.
     * @param lat the latitude which will be added to the table as a new column.
     *   For the lastest_obs.txt which already has a LON column, this is ignored.
     */
    public static Table readStationTxtUrl(String url, String stationID, 
            float lon, float lat) throws Exception {
        long time = System.currentTimeMillis();
        Table table = readStationTxt(url, SSR.getUrlResponseArrayList(url), stationID, lon, lat);
        String2.log("  readStationTxtUrl " + url + " finished. TIME=" +
            (System.currentTimeMillis() - time) + "ms");
        return table;
    }

    /**
     * A variant of readStationTxtFile which reads data from an actual file.
     */
    public static Table readStationTxtFile(String fullFileName, String stationID, 
            float lon, float lat) throws Exception {
        //read the file
        //String2.log("fullFileName=" + fullFileName);
        ArrayList<String> lines = File2.readLinesFromFile(fullFileName, File2.UTF_8, 1); 
        String line0 = lines.get(0);
        if (line0.length() >=2 && line0.charAt(0) == '\u001f' && line0.charAt(1) == '\u008b') {
            //common problem in 2019+ : it's a .gzip file, but with wrong extension
            String2.log("!!! " + fullFileName + " is actually a .gz file. I'll deal with it...");
            File2.rename(fullFileName, fullFileName + ".gz");
            SSR.unGzip(fullFileName + ".gz", File2.getDirectory(fullFileName), true, 20); //throws exception
            File2.delete(fullFileName + ".gz");
            lines = File2.readLinesFromFile(fullFileName, File2.UTF_8, 1); 
        }
        String shortFileName = File2.getNameAndExtension(fullFileName);
        return readStationTxt(fullFileName, lines, stationID, lon, lat);
    }

   /**
     * This reads one station file and works to standardize it 
     * (combine ymdh[m] into one column, insert missing columns).
     *
     * <p>Note that this convers VIS from nautical miles to km:  old*kmPerNMile;
     * and converts TIDE feet to meters: old*meterPerFoot
     *
     * <p>Old style example:
     * text file: https://www.ndbc.noaa.gov/data/view_text_file.php?filename=32301h1986.txt.gz&dir=/ftp/data/historical/stdmet/
     *.gz file:   https://www.ndbc.noaa.gov/data/historical/stdmet/32301h1986.txt.gz
     * <pre>
     * YY MM DD hh WD   WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS
     * 86 02 21 03 110 03.1 03.6 02.60 14.30 99.00 999 1012.0  25.0  25.7 999.0 99.0
     * 86 02 21 04 100 02.6 03.1 02.60 14.30 99.00 999 1012.5  25.1  25.7 999.0 99.0
     * 2004:
     * 0              1   2    3    4     5     6    7    8      9     10    11    12   13
     * YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
     * 2004 01 01 00  16  2.5  2.9  0.64 10.81  6.56 999 1026.9  23.1  22.9  16.9 99.0 99.00
     *
     * Starting March 2007:
     * #YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS  TIDE
     * #yr  mo dy hr mn degT m/s  m/s     m   sec   sec deg    hPa  degC  degC  degC   mi    ft
     * 
     * The latest_obs.txt file from https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt
     * already has #STN, LAT and LON, e.g., 
     * #STN     LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
     * #text    deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     ft
     * 13001  12.000  -23.000 2020 02 03 13 00  18   4.0   4.8   MM  MM   MM  MM 1015.2    MM  23.8  25.2    MM   MM     MM
     * 13002  21.000  -23.000 2020 02 03 13 00 330   5.2   6.7   MM  MM   MM  MM     MM    MM  20.3    MM    MM   MM     MM
     *
     * </pre>
     * 1hPa = 1000 bar see https://en.wikipedia.org/wiki/Bar_(unit)
     * 
     * <p>2020-01-27 This used to round time to nearest hour, now it leaves minute value as is.
     *
     * @param fileName used for diagnostic error messages
     * @param lines the StringArray with the lines from the text file
     * @param stationID 4 or (usually) 5 characters, uppercase.
     *    It will be stored as 5 char, uppercase.
     * @param lon the longitude of the station  (degrees_east)
     * @param lat the latitude of the station  (degrees_north)
     * @return a table, cleaned up: with standard columns and units, sorted, 
     *    with entirely duplicate rows removed. Missing values are 32727 in short
     *    columns and Float.NaN in float columns.
     */
    public static Table readStationTxt(String fileName, ArrayList<String> lines, 
            String stationID, float lon, float lat) throws Exception {

        String errorInMethod = String2.ERROR + " in NdbcMetStation.readStationTxt\n" +
            "fileName=" + fileName + "\n";
        //if (debugMode) {for (int i = 0; i < 4; i++) String2.log("line #" + i + ": " + lines.get(i)); }
    
        //ensure column names on line 0
        int nLines = lines.size();
        int columnNamesLine = 0;
        while (columnNamesLine < nLines && lines.get(columnNamesLine).indexOf(" MM DD hh") < 0)
            columnNamesLine++;
        Test.ensureNotEqual(columnNamesLine, nLines, 
            errorInMethod + "columnNames not found.\n" +
              "fullFileName=" + fileName + 
              (nLines > 1? "\nline 0=" + lines.get(0) : "") + 
              (nLines > 2? "\nline 1=" + lines.get(1) : "") + 
              (nLines > 3? "\nline 2=" + lines.get(2) : "") + 
              (nLines > 4? "\nline 3=" + lines.get(3) : ""));

        //ensure data starts on line 1 (before March 2007) or 2 (March 2007 and after)
        int dataStartLine = columnNamesLine + 1;
        while (dataStartLine < nLines && lines.get(dataStartLine).startsWith("#"))
            dataStartLine++;

        //replace the various mv's with NaN's
        //if (verbose) String2.log("firstLineBefore=" + lines.get(dataStartLine));
        Pattern pattern = Pattern.compile("\\S [9]+(\\.[0]+)? ");  //note just 1 space -- means column must be filled with 9's
        for (int i = dataStartLine; i < nLines; i++) {
            //https://www.ndbc.noaa.gov/measdes.shtml#stdmet says
            //"Any data field that contains "9 filled" represents missing data
            //  for that observation hour. (Example: 999.0, 99.0, 99.00, but not 9.0)"
            //trouble with Dave's script and simple replacements: BAR has legit value 999.0 and mv of 9999.0
            //  but he may be not saving BAR
            //I can't use String.replaceAll because the space at end of one pattern
            //  is the space at beginning of next pattern.
            String tline = lines.get(i) + ' '; //so pattern works in interior and at end
            Matcher matcher = pattern.matcher(tline);
            int matcherPo = 0;
            while (matcher.find(matcherPo)) {
                tline = tline.substring(0, matcher.start() + 1) + //+1: keep the non-space char at beginning of match
                    " NaN " + tline.substring(matcher.end());
                matcherPo = matcher.start() + 4;
                matcher = pattern.matcher(tline);
            }

            //newer files have MM
            tline = tline.replaceAll(" +[M]+", " NaN"); //first param is regex
            tline = tline.trim();
            lines.set(i, tline);
        }
        //if (verbose) String2.log("firstLineAfter =" + lines[dataStartLine]);

        //read the data into the table
        Table table = new Table();
        table.allowRaggedRightInReadASCII = true;
        String linesArray[] = lines.toArray(new String[0]);
        lines = null;
        String linesString = String2.toNewlineString(linesArray);
        //if (debugMode) String2.log(">> linesString=\n" + linesString.substring(0, 450) + "...");

        linesArray = null;
        table.readASCII(fileName, 
            new BufferedReader(new StringReader(linesString)),  
            "", "", columnNamesLine, dataStartLine, "", 
            null, null, null, null, false);
        linesString = null;
        setColumnTypes(table);
        int nRows = table.nRows();

        //if latest_obs.txt, move initial #STN, LAT and LON columns to end
        if (table.getColumnName(0).equals("#STN")) {
            table.setColumnName(0, ID_NAME);
            table.columnAttributes(0).remove("units");
            table.moveColumn(0, table.nColumns());
        }
        if (table.getColumnName(0).equals("LAT")) 
            table.moveColumn(0, table.nColumns());
        if (table.getColumnName(0).equals("LON")) 
            table.moveColumn(0, table.nColumns());

        //convert YY (byte) (in some files) to YYYY (short)
        PrimitiveArray yearColumn = table.getColumn(0);
        if (table.getColumnName(0).equals("#YY")) {    //March 2007 and after has #YY, but 4 digit year values
            table.setColumnName(0, "YYYY");
        }
        if (table.getColumnName(0).equals("YY")) {    //March 2007 and after has #YY, but 4 digit year values
            PrimitiveArray oYearColumn = yearColumn;
            yearColumn = new ShortArray(nRows, true);
            table.setColumnName(0, "YYYY");
            table.setColumn(0, yearColumn); 
            for (int i = 0; i < nRows; i++) {
                //there should be no missing values
                Test.ensureBetween(yearColumn.getInt(i), 0, 99, 
                    errorInMethod + "Year in YY column: row=" + i + ".");
                yearColumn.setInt(i, oYearColumn.getInt(i) + 1900); 
            }
        }

        //convert to one time column
        boolean hasMinuteColumn = table.getColumnName(4).equals("mm");
        DoubleArray timeArray = new DoubleArray(nRows, true);
        PrimitiveArray monthColumn  = table.getColumn(1);
        PrimitiveArray dayColumn    = table.getColumn(2);
        PrimitiveArray hourColumn   = table.getColumn(3);
        PrimitiveArray minuteColumn = table.getColumn(4);
        for (int i = 0; i < nRows; i++) {
            //there should be no missing values
            try {
                int ti = yearColumn.getInt(i);
                if (ti < 1800 || ti > 2200)
                    throw new RuntimeException(errorInMethod + "Invalid year in YYYY column: row#" + i + "=" + ti);
                ti = monthColumn.getInt(i);
                if (ti < 1 || ti > 12) 
                    throw new RuntimeException(errorInMethod + "Invalid month in MM column: row#" + i + "=" + ti);
                ti = dayColumn.getInt(i);
                if (ti < 1 || ti > 31) 
                    throw new RuntimeException(errorInMethod + "Invalid day in DD column: row#" + i + "=" + ti);
                ti = hourColumn.getInt(i);
                if (ti < 0 || ti > 23) 
                    throw new RuntimeException(errorInMethod + "Invalid hour in hh column: row#" + i + "=" + ti);
                if (hasMinuteColumn) {
                    ti = minuteColumn.getInt(i);
                    if (ti < 0 || ti > 59) 
                        throw new RuntimeException(errorInMethod + "Invalid minute in mm column: row#" + i + "=" + ti);
                }
                timeArray.set(i, Calendar2.isoStringToEpochSeconds( //throws exception if trouble
                    yearColumn.getString(i)  + "-" +
                    String2.zeroPad(monthColumn.getString(i), 2) + "-" +
                    String2.zeroPad(  dayColumn.getString(i), 2) + "T" +
                    String2.zeroPad( hourColumn.getString(i), 2) + ":" +
                    (hasMinuteColumn? String2.zeroPad(minuteColumn.getString(i), 2) : "")));

            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
                timeArray.set(i, Double.NaN); 
            }
        }

        //if (verbose) String2.log("time firstRow=" + timeArray.get(0) + " lastRow=" + timeArray.get(nRows-1));
        table.addColumn(0, metColumnNames[timeIndex], timeArray); //the Unidata Observation Dataset Conventions (OBSOLETE) requires 'time'
        if (hasMinuteColumn) {
            //String2.log("hasMinuteColumn\n" + String2.toCSSVString(table.getColumnNames()));
            table.removeColumn(5); //order of removal is important
        }
        table.removeColumn(4);
        table.removeColumn(3);
        table.removeColumn(2);
        table.removeColumn(1);

        //longitude
        int tCol = table.findColumnNumber("LON");
        if (tCol >= 0) {
            table.moveColumn(tCol, lonIndex);
        } else {
            //insert the longitude variable
            float[] lonFA = new float[nRows];
            Arrays.fill(lonFA, lon);
            table.addColumn(lonIndex, metColumnNames[lonIndex], new FloatArray(lonFA)); 
        }

        //latitude
        tCol = table.findColumnNumber("LAT");
        if (tCol >= 0) {
            table.moveColumn(tCol, latIndex);
        } else {
            //insert the latitude variable
            float[] latFA = new float[nRows];
            Arrays.fill(latFA, lat);
            table.addColumn(latIndex, metColumnNames[latIndex], new FloatArray(latFA));
        }

        //insert the depth variable
        float[] altitudeFA = new float[nRows];
        Arrays.fill(altitudeFA, 0);
        table.addColumn(depthIndex, metColumnNames[depthIndex], new FloatArray(altitudeFA)); 

        //stationID
        tCol = table.findColumnNumber(ID_NAME);
        if (tCol >= 0) {
            table.moveColumn(tCol, idIndex);
        } else {
            //insert the stationID column
            String[] idSA = new String[nRows];
            String stationID5 = stationID.toUpperCase();
            if (stationID5.length() == 4)
                stationID5 += "_";
            Arrays.fill(idSA, stationID5);
            //String2.log("stationID5=" + stationID5 + " nRows=" + nRows);
            table.addColumn(idIndex, ID_NAME, new StringArray(idSA));
        }

        int nColumns = table.nColumns();

        //if aprsIndex column is named BARO or PRES, rename it to BAR
        if (table.getColumnName(aprsIndex).equals("BARO") ||
            table.getColumnName(aprsIndex).equals("PRES")) //added March 2007
            table.setColumnName(aprsIndex, "BAR");

        //move columns into desired order
        table.moveColumn(table.findColumnNumber("ATMP"), atmpIndex);
        table.moveColumn(table.findColumnNumber("WTMP"), wtmpIndex);
        table.moveColumn(table.findColumnNumber("DEWP"), dewpIndex);
        
        //add VIS column if not already there
        tCol = table.findColumnNumber("VIS");
        if (tCol >= 0) {
            table.moveColumn(tCol, visIndex);
        } else {
            table.addColumn(visIndex, "VIS", PrimitiveArray.factory(PAType.FLOAT, nRows, ""));
            nColumns++;
        }

        //add ptdy column if not already there
        tCol = table.findColumnNumber("PTDY");
        if (tCol >= 0) {
            table.moveColumn(tCol, ptdyIndex);
        } else {
            table.addColumn(ptdyIndex, "PTDY", PrimitiveArray.factory(PAType.FLOAT, nRows, ""));
            nColumns++;
        }

        //add tide column if not already there
        tCol = table.findColumnNumber("TIDE");
        if (tCol >= 0) {
            table.moveColumn(tCol, tideIndex);
        } else {
            table.addColumn(tideIndex, "TIDE", PrimitiveArray.factory(PAType.FLOAT, nRows, ""));
            nColumns++;
        }
        //String2.log(">> post TIDE=" + table.dataToString(5));

        //add wspu and wspv columns
        float[] wspu = new float[nRows];
        float[] wspv = new float[nRows];
        PrimitiveArray wdColumn   = table.getColumn(wdIndex);
        if (table.getColumnName(wdIndex).equals("WDIR")) //this was added March 2007
            table.setColumnName(wdIndex, "WD");
        PrimitiveArray wspdColumn = table.getColumn(wspdIndex);
        for (int row = 0; row < nRows; row++) {
            double dir   = wdColumn.getDouble(row);
            double speed = wspdColumn.getDouble(row); 
            //explicitly check for NaN because Math2 routines change NaN to 0.
            if (Double.isNaN(dir) || Double.isNaN(speed)) {
                wspu[row] = Float.NaN;
                wspv[row] = Float.NaN;
            } else {
                //change from compass degrees to std radians
                //and +180 because ndbc dir is direction wind is coming from 
                //  (see WDIR at https://www.ndbc.noaa.gov/measdes.shtml#stdmet)
                //  whereas I want u,v to indicate where wind is going to
                dir = Math.toRadians(Math2.compassToMathDegrees(dir + 180));
                //Math.rint, not Math2.roundToInt, so they stay doubles
                wspu[row] = (float)Math2.roundTo(speed * Math.cos(dir), decimalDigits[wspuIndex]);
                wspv[row] = (float)Math2.roundTo(speed * Math.sin(dir), decimalDigits[wspvIndex]);
                //if (verbose && speed==2.5) 
                //    String2.log("dir deg=" + wdColumn.getDouble(row) + 
                //        " speed=" + speed + " u=" + wspu[row] + " v=" + wspv[row]);
            }
        }
        table.addColumn(wspuIndex, metColumnNames[wspuIndex], new FloatArray(wspu));
        table.addColumn(wspvIndex, metColumnNames[wspvIndex], new FloatArray(wspv));
        nColumns += 2;

        //ensure all expected columns present and in correct order
        String columnError = errorInMethod + "unexpected columns." +
            "\nobserved: " + String2.toCSSVString(table.getColumnNames()) +
            "\nexpected: " + String2.toCSSVString(metColumnNames);
        Test.ensureEqual(table.nColumns(), metColumnNames.length, columnError);
        for (int i = 0; i < metColumnNames.length; i++) 
            Test.ensureEqual(table.getColumnName(i), metColumnNames[i], columnError);

        //convert  VIS nautical miles to km
        //2020-02-20 email from Dawn Petraitis of NDBC says 
        //  "All visibility data is in nautical miles.
        //  Somehow, we have a slightly different header for the annual historical 
        //  standard met files than the real-time files (what we call the 45-day files)."
        PrimitiveArray oldVis = table.getColumn(visIndex);  //may be byteArray if all missing values or ints
        FloatArray newVis = new FloatArray(nRows, true); //ensure it handles floating point values
        table.setColumn(visIndex, newVis);
        for (int i = 0; i < nRows; i++)
            newVis.setDouble(i, Math2.roundTo(oldVis.getDouble(i) * Math2.kmPerNMile, decimalDigits[visIndex]));
        //if (verbose && !Double.isNaN(oldVis.getDouble(0)))
        //    String2.log("Vis convert " + oldVis.getDouble(0) + " miles into " + newVis.get(0) + " km");

        //convert TIDE feet to meters
        PrimitiveArray oldTide = table.getColumn(tideIndex);  //may be byteArray if all missing values or ints
        FloatArray newTide = new FloatArray(nRows, true); //ensure it handles floating point values
        table.setColumn(tideIndex, newTide);
        for (int i = 0; i < nRows; i++)
            newTide.setDouble(i, Math2.roundTo(oldTide.getDouble(i) * Math2.meterPerFoot, decimalDigits[tideIndex]));
        //if (verbose && !Double.isNaN(oldTide.getDouble(0)))
        //    String2.log("Tide convert " + oldTide.getDouble(0) + " feet into " + newTide.get(0) + " m");

        //convert  wd and mwd =360 to 0
        ShortArray tSA = (ShortArray)table.getColumn(wdIndex); 
        for (int i = 0; i < nRows; i++) {
            if (tSA.array[i] == 360)
                tSA.array[i] = 0;
        }
        tSA = (ShortArray)table.getColumn(mwdIndex); 
        for (int i = 0; i < nRows; i++) {
            if (tSA.array[i] == 360)
                tSA.array[i] = 0;
        }

        //set outliers to mv
        for (int col = 0; col < nColumns; col++) {
            if (col != idIndex) {
                PrimitiveArray pa = table.getColumn(col);
                int n = pa.size();
                double tMin = minAllowed[col];
                double tMax = maxAllowed[col];
                boolean msgPrinted = verbose? false : true;
                for (int row = 0; row < n; row++) {
                    double d = pa.getDouble(row);
                    if (Double.isNaN(d))
                        continue;
                    if (d < tMin || d > tMax) {
                        pa.setDouble(row, Double.NaN);
                        if (!msgPrinted) {
                            String2.log("  Outlier in " + fileName + 
                                "\n    col=" + metColumnNames[col] + 
                                " startingRow=" + row + " value=" + d);
                            msgPrinted = true;
                        }
                    }
                }
            }
        }

        //remove rows where time is NaN   (station 42361, minutes=MM, should be 30:
//2016 12 03 01 MM  MM   MM   MM    MM    MM    MM  MM 1015.2    MM    MM    MM   MM -0.4    MM
//2016 12 03 00 MM  MM   MM   MM    MM    MM    MM  MM 1015.2    MM    MM    MM   MM -0.0    MM
//2016 12 02 23 MM  MM   MM   MM    MM    MM    MM  MM 1013.9    MM    MM    MM   MM -1.7    MM
//2016 12 02 22 MM  MM   MM   MM    MM    MM    MM  MM 1015.6    MM    MM    MM   MM -0.3    MM
//2016 12 02 21 MM  MM   MM   MM    MM    MM    MM  MM 1015.2    MM    MM    MM   MM -1.4    MM
        int on = table.nRows();
        int tn = table.oneStepApplyConstraint(0, metColumnNames[timeIndex], "!=", "NaN");
        if (on != tn) { 
            String2.log("\n*** removed " + (on-tn) + " rows with time=NaN!");
            Math2.sleep(5000);
        }

        //if (verbose) String2.log("table at end of readStationText:\n" + table.toString(5));

        //ensure table has correct column in correct order with correct units
        Test.ensureEqual(table.getColumnNamesCSVString(), 
            "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV",
            errorInMethod + "Incorrect column names or order.");

        //sort
        table.sort(new int[]{idIndex, timeIndex}, new boolean[]{true, true}); //idIndex needed for latestObs file

        //removeDuplicates (after sort)
        //I shouldn't have to do this. There shouldn't be duplicates.
        int nDuplicates = table.removeDuplicates();  //entire row is duplicate!
        if (nDuplicates > 0)
            String2.log("!!! ENTIRELY DUPLICATE ROWS: " + nDuplicates + " duplicates in " + fileName);
        fancyRemoveDuplicateTimes(table);

        //return the table
        return table;
    }

    /**
     * Add metadata to the standard meteorological Table for one station.
     * If the table already has metadata, it will be updated/appended with the new attributes.
     *
     * @param table  Any existing metadata will be cleared.
     * @param stationName  4 or (usually) 5 character ID, e.g., "42362" and upperCase.
     * @param lon the longitude of the station  (degrees_east)
     * @param lat the latitude of the station  (degrees_north)
     */
    public static void addMetadata(Table table, String stationName,
            //String stationUrl, String owner, String locationName, String stationType, 
            double lon, double lat) { 
            //double firstNrtSeconds, String payload, String elevationInfo, 
            //boolean dataIsScaled) {

/*
     * @param stationUrl E.g., "https://www.ndbc.noaa.gov/station_page.php?station=42362". May be "".
     * @param owner E.g., "Owned and maintained by National Data Buoy Center". May be "".
     * @param locationName e.g., "Bligh Reef Light, AK".  May be "".
     * @param stationType e.g., "C-MAN station". May be "".
     * @param firstNrtSeconds the time (seconds since epoch) of the first
     *    NRT data (less quality controlled).
     '   Previous times are historical data (quality controlled). 
     *    Subsequent times have real time data (less quality controlled).
     * @param payload the payload type (e.g. "VEEP") (or "")
     * @param elevationInfo instrument elevation information (or "")
     * @param dataIsScaled true if the data has been scaled
     */

        //avoid trouble with unknown info  
        //if (stationUrl.length() == 0) stationUrl = "(Unknown)";
        //if (owner.length() == 0) owner = "(Unknown)";
        //if (stationType.length() == 0) stationType = "(Unknown)";
        //if (payload.length() == 0) payload = "(Unknown)";
        //if (elevationInfo.length() == 0) elevationInfo = "(Unknown)";

        //add metadata from conventions
        //see gov/noaa/pfel/coastwatch/data/MetaMetadata.txt
        Attributes ga = table.globalAttributes();
        String todaysDate = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);

        ga.set("cdm_data_type", "TimeSeries");
        ga.set("cdm_timeseries_variables", "ID, LON, LAT, DEPTH");
        ga.set("subsetVariables", "ID, LON, LAT, DEPTH");
        ga.set("contributor_name", "NOAA NDBC");
        ga.set("contributor_role", "Source of data."); 
        ga.set("Conventions", "COARDS, CF-1.6, ACDD-1.3");
        //ga.set("Metadata_Conventions", "null");
        ga.set("date_created", todaysDate); 
        ga.set("date_issued", todaysDate);
        //ga.set("id", "null");
        ga.set("infoUrl", "https://www.ndbc.noaa.gov/");
        ga.set("creator_name", "NOAA NMFS SWFSC ERD");
        ga.set("creator_type", "institution");
        ga.set("creator_email", "erd.data@noaa.gov");
        ga.set("creator_url", "https://www.pfeg.noaa.gov");
        ga.set("geospatial_vertical_positive", "down"); //since DChart wants depth 
        ga.set("publisher_name", "NOAA NMFS SWFSC ERD");
        ga.set("publisher_type", "institution");
        ga.set("publisher_email", "erd.data@noaa.gov");
        ga.set("publisher_url", "https://www.pfeg.noaa.gov");
        ga.set("history", 
"Around the 25th of each month, erd.data@noaa.gov downloads the latest yearly and monthly historical .txt.gz files " +
"from https://www.ndbc.noaa.gov/data/historical/stdmet/ and generates one historical .nc file for each station. " +
"erd.data@noaa.gov also downloads all of the 45day near real time .txt files from " +
"https://www.ndbc.noaa.gov/data/realtime2/ and generates one near real time .nc file for each station.\n" +
"Every 5 minutes, erd.data@noaa.gov downloads the list of latest data from all stations for the last 2 hours from " +
"https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt and updates the near real time .nc files.");
        ga.set("institution", "NOAA NDBC, NOAA NMFS SWFSC ERD");
        ga.set("keywords", 
"Earth Science > Atmosphere > Air Quality > Visibility,\n" +
"Earth Science > Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Earth Science > Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Earth Science > Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Earth Science > Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Earth Science > Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Earth Science > Oceans > Ocean Waves > Significant Wave Height,\n" +
"Earth Science > Oceans > Ocean Waves > Swells,\n" +
"Earth Science > Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal");
        ga.set("keywords_vocabulary", "GCMD Science Keywords");
        ga.set("license", 
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.");
        ga.set("naming_authority", "gov.noaa.pfeg.coastwatch");
        ga.set("project", "NOAA NDBC and NOAA NMFS SWFSC ERD");
        ga.set("quality", "Automated QC checks with periodic manual QC");
        ga.set("source", "station observation");
        ga.set("sourceUrl", "https://www.ndbc.noaa.gov/");
        ga.set("standard_name_vocabulary", "CF Standard Name Table v70");
        ga.set("summary", 
"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys. See\n" +
"https://www.ndbc.noaa.gov/measdes.shtml for a description of the measurements.\n" +
"\n" +
"The source data from NOAA NDBC has different column names, different units,\n" +
"and different missing values in different files, and other problems (notably,\n" +
"lots of rows with duplicate or different values for the same time point).\n" +
"This dataset is a standardized, reformatted, and lightly edited version of\n" +
"that source data, created by NOAA NMFS SWFSC ERD (email: erd.data at noaa.gov).\n" +
"Before 2020-01-29, this dataset only had the data that was closest to a given\n" +
"hour, rounded to the nearest hour. Now, this dataset has all of the data\n" +
"available from NDBC with the original time values. If there are multiple\n" +
"source rows for a given buoy for a given time, only the row with the most\n" +
"non-NaN data values is kept. If there is a gap in the data, a row of missing\n" +
"values is inserted (which causes a nice gap when the data is graphed). Also,\n" +
"some impossible data values are removed, but this data is not perfectly clean.\n" +
"This dataset is now updated every 5 minutes.\n" +
"\n" +
"This dataset has both historical data (quality controlled) and near real time\n" +
"data (less quality controlled).");
        ga.set("testOutOfDate", "now-25minutes");
        ga.set("title", "NDBC Standard Meteorological Buoy Data, 1970-present");

        //For now, don't put station specific info because THREDDS aggregates and just shows one station's info
        //tableGlobalAttributes.set("NDBCStationID", stationName);
        //tableGlobalAttributes.set("NDBCStationUrl", stationUrl);
        //tableGlobalAttributes.set("NDBCStationOwner", owner);
        //if (locationName != null && locationName.length() > 0)
        //    tableGlobalAttributes.set("NDBCStationLocation", locationName);
        //tableGlobalAttributes.set("NDBCStationType", stationType);
        //tableGlobalAttributes.set("NDBCStationPayload", payload);
        //tableGlobalAttributes.set("NDBCStationElevationInfo", elevationInfo);
        //tableGlobalAttributes.set("NDBCStationLastHistoricalTime", 
        //    Calendar2.epochSecondsToIsoStringTZ(lastHistoricalTime));
        //tableGlobalAttributes.set("id", "NdbcMeteorologicalStation" + stationName);
      
      
        table.setActualRangeAndBoundingBox(
            lonIndex, latIndex, depthIndex, -1, timeIndex, "");

        for (int col = 0; col < metColumnNames.length; col++) {
            //atts that most vars have
            Attributes colAtts = table.columnAttributes(col);
            colAtts.set("ioos_category", ioosCategory[col]);
            colAtts.set("long_name",     longNames[col]);
            if (comments[col] != null)
                colAtts.set("comment",         comments[col]);
            if (!Double.isNaN(colorBarMin[col]))
                colAtts.set("colorBarMinimum", colorBarMin[col]);
            if (!Double.isNaN(colorBarMax[col]))
                colAtts.set("colorBarMaximum", colorBarMax[col]);
            if (standardName[col] != null)
                colAtts.set("standard_name",   standardName[col]);
            if (udUnits[col] != null)
                colAtts.set("units",           udUnits[col]);

            //individual atts
            //_FillValue and missing_value are set by table.saveAs4DNc()
            if (col == idIndex) {
                colAtts.set("cf_role", "timeseries_id");

            } else if (col == lonIndex) {
                colAtts.set("_CoordinateAxisType", "Lon");
                colAtts.set("axis", "X");
 
            } else if (col == latIndex) {
                colAtts.set("_CoordinateAxisType", "Lat");
                colAtts.set("axis", "Y");
 
            } else if (col == depthIndex) {
                colAtts.set("_CoordinateAxisType", "Height");
                colAtts.set("_CoordinateZisPositive", "down");
                colAtts.set("axis", "Z");
                colAtts.set("positive", "down");
            }
        }            
    }
  
    /**
     * This updates the files with the last 45 days Info every 60 minutes.
     * The updating takes about 3 minutes, after which this sleeps.
     *
     * @param nrtNcDir the directory with the NRT .nc files (with slash at end)
     * @throws Exception
     */
    public static void updateEveryHour(String nrtNcDir, int timeOutMinutes) 
            throws Exception {
        while (true) {
            long time = System.currentTimeMillis();
            try {
                addLatestObsData(nrtNcDir, false); //2020-02-05 was addLastNDaysInfo(nrtNcDir, 5, false);
            } catch (Exception e) {
                String2.log(String2.ERROR + " in NdbcMetStation.updateEveryHour:\n" +
                    MustBe.throwableToString(e));
            }
            String2.log("sleeping for 60 minutes...");
            time = System.currentTimeMillis() - time;
            Math2.sleep(60 * Calendar2.MILLIS_PER_MINUTE - time);
        }
    }

    /**
     * Get list of files from ndbc dir.
     *
     * @param url the url, already percentEncoded as needed
     * @return a sorted StringArray with no duplicates
     */
    public static StringArray getFileList(String url, String regex) throws Exception {

        ArrayList<String> sourceSa = SSR.getUrlResponseArrayList(url);
        int nLines = sourceSa.size();
        StringArray sa = new StringArray();

        //look for Parent Directory
        int line = 0;
        while (line < nLines && (sourceSa.get(line).indexOf("Parent Directory") < 0)) 
            line++;
        if (line >= nLines)
            return sa;

        //gather the text matching regex
        while (line < nLines) {
            String s = sourceSa.get(line);
            if (s != null) {
                String ts = String2.extractRegex(s, regex, 0);
                if (ts != null) 
                    sa.add(ts);
            }
            if (s.indexOf("</table>") >= 0)
                return sa;
            line++;
        }
        return sa;
    }

    /**
     *
     * This adds the latest_obs data (up to 2 hours worth) from the "Latest Observations File" at
     * http://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt
     * (which is updated every 5 minutes) or (if needed) from the 5day or 45day NRT file.
     * The goal is to run this every 5 or 10 minutes (or run the 5day updater 
     * if there has been a gap in processing).
     *
     * <p>This is the 2020 fancy replacement for addLastNDaysInfo.
     *
     * <p>See description of data sources at NDBC
     * https://www.ndbc.noaa.gov/docs/ndbc_web_data_guide.pdf 
     * (also in my ndbcMet2Logs directory). This is Section 3.
     *
     *
     * @param testMode if true, only station 46088 and RCPT2 are updated
     */
    public static void addLatestObsData(String nrtNcDir, boolean testMode) throws Exception {
        String2.log("NdbcMetStation.addLatestObsData testMode=" + testMode + 
            "\n  nrtNcDir=" + nrtNcDir); 
        long time = System.currentTimeMillis();
        String todaysDate = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);
        long cumulativeNDayReadTime = 0;
        long stationNcReadTime = 0;
        long stationNcWriteTime = 0;
        int totalNRecordsAdded = 0;
        int nSourceIsLatestObs = 0, nSourceIs5DayTable = 0, nSourceIs45DayTable = 0;
        double maxMaxGroupTime = 0;

        //get list of current stations with station.nc files in nrtNcDir, e.g., NDBC_46088_met.nc
        String ncFileList[] = RegexFilenameFilter.list(nrtNcDir, "NDBC_.+_met\\.nc");
        for (int i = 0; i < ncFileList.length; i++)
            ncFileList[i] = ncFileList[i].substring(5, 10);

        //get the latest_obs.txt file
        Table latestObsTable = null;
        String latestObsUrl = "https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt";
        String cachedLatestObsName = "/u00/data/points/ndbcMet2Logs/latest_obs.txt";
        if (testMode && false) {  //just during development
            if (!File2.isFile(cachedLatestObsName))
                SSR.downloadFile(latestObsUrl, cachedLatestObsName, true); //tryToUseCompression
            latestObsTable = readStationTxtFile(cachedLatestObsName,  
                "*", Float.NaN, Float.NaN); //stationID, lon, lat
        } else {
            latestObsTable = readStationTxtUrl(latestObsUrl,
                "*", Float.NaN, Float.NaN); //stationID, lon, lat
        }

//#STN     LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
//#text    deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     ft
//13001  12.000  -23.000 2020 02 03 13 00  18   4.0   4.8   MM  MM   MM  MM 1015.2    MM  23.8  25.2    MM   MM     MM
//13002  21.000  -23.000 2020 02 03 13 00 330   5.2   6.7   MM  MM   MM  MM     MM    MM  20.3    MM    MM   MM     MM
//becomes
//LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV
//-23.0,12.0,0.0,1.5807348E9,13001,18,4.0,4.8,,,,,1015.2,23.8,25.2,,,,,-1.2,-3.8
//-23.0,21.0,0.0,1.5807348E9,13002,330,5.2,6.7,,,,,,20.3,,,,,,2.6,-4.5

//String2.log(latestObsTable.dataToString(5));
//latestObsTable.removeRows(0, latestObsTable.nRows() - 1);
//String2.log(latestObsTable.dataToString(1));

        //go through latestObsTable processing groups of rows which have data from various stations
        int nRows = latestObsTable.nRows();
        PrimitiveArray idPA   = latestObsTable.getColumn(idIndex);
        PrimitiveArray timePA = latestObsTable.getColumn(timeIndex);
        int firstRowOfGroup = 0;
        String oldID = idPA.getString(0);
        int nStationsUpdated = 0;
        for (int row = 1; row <= nRows; row++) {  //1 because look back.  =nRows because want to look back at last row
            String newID = row == nRows? "null" : idPA.getString(row);
            if (!oldID.equals(newID)) {
                try {
                    //process that group (for one station) of the lastObsTable
                    //is there new data?
                    double maxGroupTime = timePA.getDouble(row - 1);
                    maxMaxGroupTime = Math.max(maxGroupTime, maxMaxGroupTime);

                    //is there a file for oldID in nrt directory?
                    if ((!testMode || oldID.equals("46088") || oldID.equals("RCPT2")) &&
                        String2.indexOf(ncFileList, oldID) >= 0) {  //yes
                        //read nrt file
                        Table nrtTable = new Table();
                        stationNcReadTime -= System.currentTimeMillis();
                        //this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN 
                        nrtTable.read4DNc(nrtNcDir + "NDBC_" + oldID + "_met.nc", null, //null=it finds columns
                            -1, ID_NAME, idIndex); //standardizeWhat=-1
                        stationNcReadTime += System.currentTimeMillis();
                        int nrtTableNRows = nrtTable.nRows();
                        PrimitiveArray nrtTableTimePA = nrtTable.getColumn(timeIndex);
                        double lastNrtTime = nrtTableTimePA.getDouble(nrtTableNRows - 1);
                        String lastNrtIsoTime = Calendar2.epochSecondsToIsoStringTZ(lastNrtTime);

                        if (maxGroupTime <= lastNrtTime) {
                            String2.log("No new data in latestObs for " + oldID + 
                                ". last nrtTime=" + lastNrtIsoTime); 

                        } else {
                            float nrtLon = nrtTable.getColumn(lonIndex).getFloat(nrtTableNRows - 1);
                            float nrtLat = nrtTable.getColumn(latIndex).getFloat(nrtTableNRows - 1);

                            Table nDayTable = null; //when we have a sufficient table, this won't be null
                            cumulativeNDayReadTime -= System.currentTimeMillis();

                            if (maxGroupTime - lastNrtTime <= Calendar2.SECONDS_PER_HOUR) {
                                nSourceIsLatestObs++;
                                nDayTable = latestObsTable.subset(firstRowOfGroup, 1, row - 1); //yes, inclusive
                                //ensure lat lon in latestObsTable exactly match lat lon in nrt file
                                PrimitiveArray lonPA = nDayTable.getColumn(lonIndex);
                                PrimitiveArray latPA = nDayTable.getColumn(latIndex);
                                for (int tRow = 0; tRow < lonPA.size(); tRow++) {
                                    lonPA.setFloat(tRow, nrtLon);
                                    latPA.setFloat(tRow, nrtLat);
                                }
                            }

                            //too long since last update: try getting 5day file
                            if (maxGroupTime - lastNrtTime > Calendar2.SECONDS_PER_HOUR &&
                                maxGroupTime - lastNrtTime <= 4 * Calendar2.SECONDS_PER_DAY) {
                                try {
                                    nDayTable = readStationTxtUrl(n5DayBaseUrl + oldID + n5DaySuffix, 
                                        oldID, nrtLon, nrtLat);
                                    nSourceIs5DayTable++;
                                } catch (Exception e10) {
                                    String2.log("Warning: Failed to download 5day file for " + oldID + "\n" + 
                                        MustBe.throwableToString(e10));
                                }
                            }

                            if (nDayTable == null) {
                                //try getting 45 day file
                                //if this fails, abandon updating this station
                                nDayTable = readStationTxtUrl(n45DayBaseUrl + oldID + n45DaySuffix, 
                                    oldID, nrtLon, nrtLat);                           
                                nSourceIs45DayTable++;
                            }

                            cumulativeNDayReadTime += System.currentTimeMillis();

                            //append all the new data
                            //calculate the first and last relevant row numbers (for the rows in the nDay file) in the station .nc file
                            PrimitiveArray nDayTableTimePA = nDayTable.getColumn(timeIndex);
                            int nDayTableNRows = nDayTableTimePA.size();
                            double firstNDayTime = nDayTableTimePA.getDouble(0); 
                            double lastNDayTime  = nDayTableTimePA.getDouble(nDayTableNRows - 1); 
                            double firstNrtTime = nrtTable.getColumn(timeIndex).getDouble(0); 
                            String lastNDayIsoTime  = Calendar2.epochSecondsToIsoStringTZ(lastNDayTime);

                            //String2.log("stationLastHistoricalTime=" + stationLastHistoricalTime +
                            //    "\nfirstNDayTime=" + Calendar2.epochSecondsToIsoStringTZ(firstNDayTime) +
                            //    "\nlastNDayTime=" + lastNDayIsoTime +
                            //    "\nfirstNrtTime=" + Calendar2.epochSecondsToIsoStringTZ(firstNrtTime) +
                            //    "\nlastNrtTime=" + lastNrtIsoTime);

                            if (lastNDayTime > lastNrtTime) {
                                //use the new data!
                                //I've seen changes to data over time in these files.
                                //  And it is good to give NDBC the opportunity to make corrections over time.
                                //So always use new data in preference to old data.
                                //So remove rows at beginning of nDayTable before start of ncTable.
                                int firstKeepRow = 0;
                                while (firstKeepRow < nDayTableNRows && nDayTableTimePA.getDouble(firstKeepRow) < firstNrtTime)
                                    firstKeepRow++;
                                nDayTable.removeRows(0, firstKeepRow);
                                nDayTableNRows = nDayTable.nRows();

                                //And remove rows at end of ncTable with time >= first nDayTable time. 
                                int firstRemoveRow = 0;
                                while (firstRemoveRow < nrtTableNRows && nrtTableTimePA.getDouble(firstRemoveRow) < firstNDayTime)
                                    firstRemoveRow++;
                                //even if removing all rows, keep nrtTable because it has metadata
                                int tNRecordsAdded = -nrtTableNRows;
                                if (firstRemoveRow < nrtTableNRows)
                                    nrtTable.removeRows(firstRemoveRow, nrtTableNRows);
                                nrtTableNRows = nrtTable.nRows();

                                //Then append
                                nrtTable.append(nDayTable);
                                nrtTableNRows = nrtTable.nRows();
                                tNRecordsAdded += nrtTableNRows;
                                totalNRecordsAdded += tNRecordsAdded;

                                //ensure nrtTable has ascending time values
                                String ia = nrtTableTimePA.isAscending(); 
                                if (ia.length() > 0) 
                                    throw new RuntimeException("New time array isn't sorted! stationID=" + oldID + " (" + ia + ")");

                                //entirely duplicate and fancy duplicate rows in nDayTable were removed above
                                //but there might be a time gap between nrt data and new data
                                insertMVRowInTimeGaps(nrtTable);

                                //rewrite the station .nc file
                                stationNcWriteTime -= System.currentTimeMillis();
                                //this calls convertToFakeMissingValues, e.g., Float.NaN becomes -9999999.0f and sets the _FillValue metadata
                                nrtTable.saveAs4DNcWithStringVariable(nrtNcDir + "NDBC_" + oldID + "_met.nc", 
                                    lonIndex, latIndex, depthIndex, timeIndex, 4);
                                stationNcWriteTime += System.currentTimeMillis();
                                nStationsUpdated++;
                                String2.log(oldID + " Updated. lastNDayTime=" + lastNDayIsoTime +
                                    " > lastNrtTime=" + lastNrtIsoTime + " nRecordsAdded=" + tNRecordsAdded);
                            } else if (lastNDayTime == lastNrtTime) {
                                String2.log(oldID + " Already up-to-date: lastNDayTime=" + lastNDayIsoTime +
                                    " = lastNrtTime=" + lastNrtIsoTime);
                            } else {
                                String2.log(oldID + " UNEXPECTED: lastNDayTime=" + lastNDayIsoTime +
                                    " < lastNrtTime=" + lastNrtIsoTime + " !!!");
                            }
                        }
                    }
                } catch (Exception e2) {
                    String2.log("UNEXPECTED ERROR while trying to update " + oldID + "\n" +
                         MustBe.throwableToString(e2));
                }

                //move on to next group
                firstRowOfGroup = row;
                oldID = newID;
            }
        }

        String2.log("\nNdbcMetStation.addLatestObsData finished at " + 
            Calendar2.getCurrentISODateTimeStringLocalTZ() + ". time=" + 
            Math2.roundToInt((System.currentTimeMillis() - time) / 1000) + "s\n" +
           "  totalNRecordsAdded=" + totalNRecordsAdded + 
               " nStationsUpdated=" + nStationsUpdated +
           //2020-02-05 The lag from buoy measurement to ndbc publishing is as little as 25 minutes.
           "\n  maxMaxGroupTime=" + Calendar2.epochSecondsToIsoStringTZ(maxMaxGroupTime) +
               " minimumLagTime=" + (System.currentTimeMillis()/1000 - maxMaxGroupTime)/60.0 + "minutes" +
           "\n  source was latestObs(n=" + nSourceIsLatestObs + ") 5DayTable(n=" + 
               nSourceIs5DayTable + ") 45DayTable(n=" + nSourceIs45DayTable + ")" +
           "\n  stationNcReadTime=" + (stationNcReadTime/1000) +
               "s stationNcWriteTime=" + (stationNcWriteTime/1000) +
               "s cumulativeNDayReadTime=" + (cumulativeNDayReadTime/1000) + "s");
    }
    
    /**
     * This adds the last nDays of real time data to the individual and combined 
     * .nc files.
     *
     * <pre>example: https://www.ndbc.noaa.gov/data/5day2/AUGA2_5day.txt
     * YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS PTDY  TIDE
     * 2006 03 02 16 00 050 13.4 16.5    MM    MM    MM  MM 1013.8  -6.5    MM    MM   MM -2.0    MM
     * 2006 03 02 15 30 050 12.9 14.9    MM    MM    MM  MM 1014.2  -6.3    MM    MM   MM   MM    MM
     * 2006 03 02 15 00 050 12.4 14.4    MM    MM    MM  MM 1014.6  -6.3    MM    MM   MM -1.8    MM
     *
     * file meteorological .txt files from: https://www.ndbc.noaa.gov/data/realtime2/
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<html>
 <head>
  <title>Index of /data/realtime2</title>
 </head>
 <body>
<H1>Index of /data/realtime2</H1>
<div style="border: red solid 2px">
<p style="margin: 5px"><span style="color:Red;text-decoration:none"><font face="arial,helvetica" color="#FF00000" size="+1">New Format</font></span> - These data files implement multiple observations per hour (<a href="/mods.shtml">See description</a>).</p>
</div>
<pre><img src="/icons/blank.gif" alt="Icon " /> <a href="?C=N;O=D">Name</a>                    <a href="?C=M;O=A">Last modified</a>      <a href="?C=S;O=A">Size</a>  <a href="?C=D;O=A">Description</a><hr /><img src="/icons/back.gif" alt="[DIR]" /> <a href="/data/">Parent Directory</a>                             -   
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.adcp">42OTP.adcp</a>              31-Dec-2003 09:28   56K  Acoustic Doppler Current Profiler Data
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.cwind">42OTP.cwind</a>             28-Aug-2005 14:23   61K  Continuous Winds Data
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.data_spec">42OTP.data_spec</a>         28-Aug-2005 14:01  180K  Raw Spectral Wave Data
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.hkp">42OTP.hkp</a>               28-Aug-2005 14:01   18K  Housekeeping Data
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.ocean">42OTP.ocean</a>             03-Mar-2004 10:02   73K  Oceanographic Data
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.spec">42OTP.spec</a>              30-Jun-2004 15:09  5.9K  Spectral Wave Summary Data
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.swdir">42OTP.swdir</a>             31-Dec-2003 09:26  698   Spectral Wave Data (alpha1)
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.swdir2">42OTP.swdir2</a>            31-Dec-2003 09:27  695   Spectral Wave Data (alpha1)
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.swr1">42OTP.swr1</a>              31-Dec-2003 09:27  697   Spectral Wave Data (r1)
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.swr2">42OTP.swr2</a>              31-Dec-2003 09:28  697   Spectral Wave Data (r2)
<img src="/icons/text.gif" alt="[TXT]" /> <a href="42OTP.txt">42OTP.txt</a>               28-Aug-2005 14:23   24K  Standard Meteorological Data

     * https://www.ndbc.noaa.gov/data/realtime2/AUGA2.txt    //45 day
     * YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS PTDY  TIDE
     * 2006 03 02 16 00 050 13.4 16.5    MM    MM    MM  MM 1013.8  -6.5    MM    MM   MM -2.0    MM
     * 2006 03 02 15 30 050 12.9 14.9    MM    MM    MM  MM 1014.2  -6.3    MM    MM   MM   MM    MM
     * </pre>
     *
     *
     * @param nrtNcDir the directory with the nrt .nc files (with slash at end)
     * @param nDays the number of days (5 or 45).
     *    (Since many buoys have 45 day files, but not 5 day files, e.g., 41038,
     *    the 5 option will also look for a 45 day file if there is no 5 day file.
     *    And for good measure, the 45 option looks for 5 day file if no 45 day file.)
     * @param testMode if true, only station 46088 and RCPT2 are updated
     */
    public static void addLastNDaysInfo(String nrtNcDir, int nDays, boolean testMode) throws Exception {

        String2.log("NdbcMetStation.addLastNDaysInfo nDays=" + nDays + " testMode=" + testMode + 
            "\n  nrtNcDir=" + nrtNcDir); 
        String errorInMethod = String2.ERROR + " in NdbcMetStation.addLastNDayInfo:\n";
        long time = System.currentTimeMillis();
        if (nDays != 5 && nDays != 45) 
            Test.error(errorInMethod + "unsupported nDays: " + nDays);
        long stationNcReadTime = 0; 
        long stationNcWriteTime = 0; 
        long cumulativeNDayReadTime = 0;
        String todaysDate = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);
 
        //get list of current stations with station.nc files, e.g., NDBC_46088_met.nc
        String stationList[] = RegexFilenameFilter.list(nrtNcDir, "NDBC_.+_met\\.nc");
        for (int i = 0; i < stationList.length; i++)
            stationList[i] = stationList[i].substring(5, 10);

        //get list of nDay files from ndbc
        StringArray  n5DayFileNames = getFileList(n5DayBaseUrl,  "\\\".{5}_5day\\.txt\\\"");
        StringArray n45DayFileNames = getFileList(n45DayBaseUrl, "\\\".{5}\\.txt\\\"");

        for (int i = 0; i < n5DayFileNames.size(); i++) {
            String s = n5DayFileNames.get(i);
            n5DayFileNames.set(i, s.substring(1, 6)); //remove suffix
        }
        for (int i = 0; i < n45DayFileNames.size(); i++) {
            String s = n45DayFileNames.get(i);
            n45DayFileNames.set(i, s.substring(1, 6)); //remove suffix
        }
        if (n5DayFileNames.size() == 0)  
            String2.log("Warning: NdbcMetStation.addLastNDaysInfo: n5DayFileNames.size() == 0.");
        if (n45DayFileNames.size() == 0)  
            String2.log("Warning: NdbcMetStation.addLastNDaysInfo: n45DayFileNames.size() == 0.");
        if (verbose) String2.log(
            "n5DayFiles=" + n5DayFileNames.size() + " 5name0=" + n5DayFileNames.get(0) +
            " n45DayFiles=" + n45DayFileNames.size() + " 45name0=" + n45DayFileNames.get(0));

        int totalNRecordsAdded = 0;
        int nStationsUpdated = 0;

        //for each station
        int n5DayStations = 0;
        int n45DayStations = 0;
        for (int station = 0; station < stationList.length; station++) {

            //is this station in list of available nDay files from ndbc
            String stationID = stationList[station];
            String tBaseUrl, tSuffix;
            int tNDays = 0;  //figure out which is to be used for this file
            if (nDays == 5 && n5DayFileNames.indexOf(stationID, 0) >= 0) {  //note first test is of nDays
                tNDays = 5;
                tBaseUrl = n5DayBaseUrl;
                tSuffix = n5DaySuffix;
                n5DayStations++;
            } else if (tNDays == 0 && n45DayFileNames.indexOf(stationID, 0) >= 0) { //note first test is of tNDays
                tNDays = 45;
                tBaseUrl = n45DayBaseUrl;
                tSuffix = n45DaySuffix;
                n45DayStations++;
            } else if (tNDays == 0 && n5DayFileNames.indexOf(stationID, 0) >= 0) { //note first test is of tNDays
                tNDays = 5;
                tBaseUrl = n5DayBaseUrl;
                tSuffix = n5DaySuffix;
                n5DayStations++;
            } else continue;

            //next line is used while working on this method
            //if (testMode && stationID.compareTo("P") < 0) continue;
            if (testMode && !stationID.equals("46088") && !stationID.equals("RCPT2")) continue;
            //if (testMode && String2.isDigit(stationID.charAt(0))) continue;
            if (verbose) String2.log("updating tNDays=" + tNDays + ", station " + 
                station + "=" + stationID);

            try { //so error for one station doesn't affect next station

                //read the station's .nc file  
                //(I'm going to rewrite it completely, so just load it all.)
                Table nrtTable = new Table();
                stationNcReadTime -= System.currentTimeMillis();
                //this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN 
                nrtTable.read4DNc(nrtNcDir + "NDBC_" + stationID + "_met.nc", null, //null=it finds columns
                    0, ID_NAME, idIndex); //standardizeWhat=0
                stationNcReadTime += System.currentTimeMillis();
                Attributes tableGlobalAttributes = nrtTable.globalAttributes();
                double stationLon = tableGlobalAttributes.getDouble("geospatial_lon_min");
                double stationLat = tableGlobalAttributes.getDouble("geospatial_lat_min");

                //get the station's nDay file
                long tcTime = System.currentTimeMillis();
                Table nDayTable = readStationTxtUrl(tBaseUrl + stationID + tSuffix, 
                    stationID, (float)stationLon, (float)stationLat);
                PrimitiveArray nDayTableTimePA = nDayTable.getColumn(timeIndex);
                PrimitiveArray nrtTableTimePA = nrtTable.getColumn(timeIndex);
                cumulativeNDayReadTime += System.currentTimeMillis() - tcTime;
                int nDayTableNRows = nDayTable.nRows();
                int nrtTableNRows = nrtTable.nRows();

                //calculate the first and last relevant row numbers (for the rows in the nDay file) in the station .nc file
                double firstNDayTime = nDayTableTimePA.getDouble(0); 
                double lastNDayTime  = nDayTableTimePA.getDouble(nDayTableNRows - 1); 
                double firstNrtTime = nrtTable.getColumn(timeIndex).getDouble(0); 
                double lastNrtTime  = nrtTable.getColumn(timeIndex).getDouble(nrtTableNRows - 1);
                String lastNDayIsoTime = Calendar2.epochSecondsToIsoStringTZ(lastNDayTime);
                String lastNrtIsoTime  = Calendar2.epochSecondsToIsoStringTZ(lastNrtTime);

                //String2.log("stationLastHistoricalTime=" + stationLastHistoricalTime +
                //    "\nfirstNDayTime=" + Calendar2.epochSecondsToIsoStringTZ(firstNDayTime) +
                //    "\nlastNDayTime=" + lastNDayIsoTime +
                //    "\nfirstNrtTime=" + Calendar2.epochSecondsToIsoStringTZ(firstNrtTime) +
                //    "\nlastNrtTime=" + lastNrtIsoTime);

                if (lastNDayTime > lastNrtTime) {

                    if (verbose) String2.log("Updating: lastNDayTime=" + lastNDayIsoTime +
                        " > lastNrtTime=" + lastNrtIsoTime);

                    //I've seen changes to data over time in these files.
                    //  And it is good to give NDBC the opportunity to make corrections over time.
                    //So always use new data in preference to old data.
                    //So remove rows at beginning of nDayTable before start of ncTable.
                    int firstKeepRow = 0;
                    while (firstKeepRow < nDayTableNRows && nDayTableTimePA.getDouble(firstKeepRow) < firstNrtTime)
                        firstKeepRow++;
                    nDayTable.removeRows(0, firstKeepRow);
                    nDayTableNRows = nDayTable.nRows();

                    //And remove rows at end of ncTable with time >= first nDayTable time. 
                    int firstRemoveRow = 0;
                    while (firstRemoveRow < nrtTableNRows && nrtTableTimePA.getDouble(firstRemoveRow) < firstNDayTime)
                        firstRemoveRow++;
                    //even if removing all rows, keep nrtTable because it has metadata
                    int tNRecordsAdded = -nrtTableNRows;
                    if (firstRemoveRow < nrtTableNRows)
                        nrtTable.removeRows(firstRemoveRow, nrtTableNRows);
                    nrtTableNRows = nrtTable.nRows();

                    //Then append
                    nrtTable.append(nDayTable);
                    tNRecordsAdded += nrtTableNRows;
                    totalNRecordsAdded += tNRecordsAdded;

                    //ensure nrtTable has ascending time values
                    String ia = nrtTableTimePA.isAscending(); 
                    if (ia.length() > 0) 
                        throw new RuntimeException("New time array isn't sorted! (" + ia + ")");

                    //entirely duplicate and fancy duplicate rows in nDayTable were removed above

                    //insert mv row in time gaps
                    insertMVRowInTimeGaps(nrtTable);

                    //rewrite the station .nc file
                    nrtTable.globalAttributes().set("date_issued", todaysDate);
                    stationNcWriteTime -= System.currentTimeMillis();
                    //this calls convertToFakeMissingValues, e.g., Float.NaN becomes -9999999.0f and sets the _FillValue metadata
                    nrtTable.saveAs4DNcWithStringVariable(nrtNcDir + "NDBC_" + stationID + "_met.nc", 
                        lonIndex, latIndex, depthIndex, timeIndex, 4);
                    stationNcWriteTime += System.currentTimeMillis();
                    String2.log(stationID + " Updated. lastNDayTime=" + lastNDayIsoTime +
                        " = lastNrtTime=" + lastNrtIsoTime + " nRecordsAdded=" + tNRecordsAdded);
                } else if (lastNDayTime == lastNrtTime) {
                    String2.log(stationID + " Already up-to-date: lastNDayTime=" + lastNDayIsoTime +
                        " = lastNrtTime=" + lastNrtIsoTime);
                } else {
                    String2.log(stationID + " UNEXPECTED: lastNDayTime=" + lastNDayIsoTime +
                        " < lastNrtTime=" + lastNrtIsoTime + " !!!");
                }
            } catch (Exception e) {
                String2.log(MustBe.throwable(errorInMethod, e));
            } 

            nStationsUpdated++;
        } //end station


       String2.log("\nNdbcMet.addLastNDaysInfo finished successfully in " + //always write this
           Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + 
           "\n  totalNRecordsAdded=" + totalNRecordsAdded + 
               " n5DayStationsUpdated=" + n5DayStations +
               " n45DayStationsUpdated=" + n45DayStations +
           "\n  stationNcReadTime=" + (stationNcReadTime/1000) +
               "s stationNcWriteTime=" + (stationNcWriteTime/1000) +
               "s cumulativeNDayReadTime=" + (cumulativeNDayReadTime/1000) + "s");
    }

    /**
     * This inserts of row of mv's when there is a time gap of more than 1 hour.
     *
     * @param table the standard data table with ascending sorted time values
     */
    public static void insertMVRowInTimeGaps(Table table) {
        try {
            //insert mv row where gap of more than 1 hour
            //  2020-01-27 was: make newData columns which have data regularly spaced on-the-hour
            int oldNRows = table.nRows();
            int nColumns = table.nColumns(); 
            PrimitiveArray oldPA[] = new PrimitiveArray[nColumns];
            for (int col = 0; col < nColumns; col++) 
                oldPA[col] = table.getColumn(col);

            //quick check for gaps
            boolean hasGap = false;
            for (int oldRow = 1; oldRow < oldNRows; oldRow++) {
                if (oldPA[timeIndex].getDouble(oldRow) -
                    oldPA[timeIndex].getDouble(oldRow - 1) > Calendar2.SECONDS_PER_HOUR) {
                    hasGap = true;
                    break;
                }
            }
            if (!hasGap) 
                return;

            //there is a gap 
            PrimitiveArray newPA[] = new PrimitiveArray[nColumns];
            for (int col = 0; col < nColumns; col++) 
                newPA[col] = PrimitiveArray.factory(
                    oldPA[col].elementType(), 
                        oldNRows + 100, false); //exact number not critical here
            long lastSeconds2 = -1; //1 before previous row
            long lastSeconds = -1;  //previous row
            boolean msgPrinted = false;
            //go through old table data rows, adding rows to newPA table
            for (int oldRow = 0; oldRow < oldNRows; oldRow++) {

                //insert mv row?
                long seconds = Math2.roundToLong(oldPA[timeIndex].getDouble(oldRow));
                if (lastSeconds == -1)
                    lastSeconds = seconds - 30 * Calendar2.SECONDS_PER_MINUTE;
                if (lastSeconds2 == -1)
                    lastSeconds2 = lastSeconds - 30 * Calendar2.SECONDS_PER_MINUTE;
                if (seconds - lastSeconds > Calendar2.SECONDS_PER_HOUR) {
                    //if last row is all mv, then don't insert another gap row
                    boolean allMV = true;
                    for (int col = idIndex + 1; col < nColumns; col++) {
                        if (!Double.isNaN(oldPA[col].getDouble(oldRow - 1))) {
                            //String2.log("  col=" + table.getColumnName(col) + " data=" + oldPA[col].getDouble(oldRow - 1) + " != NaN");
                            allMV = false;
                            break;
                        }
                    }

                    if (!allMV) {                    
                        int tNRows = newPA[0].size();

                        //add x,y,z from previous row
                        for (int col = lonIndex; col <= depthIndex; col++)
                            newPA[col].addFloat(newPA[col].getFloat(tNRows - 1)); 

                        //add incremented time
                        lastSeconds += Math.min(Calendar2.SECONDS_PER_HOUR, lastSeconds - lastSeconds2); 
                        lastSeconds2 = lastSeconds;
                        newPA[timeIndex].addDouble(lastSeconds); 
                        if (!msgPrinted) {
                            String2.log("inserting first mv row in gap at " + Calendar2.epochSecondsToIsoStringTZ(lastSeconds));
                            msgPrinted = true;
                        }

                        //add id from previous row
                        newPA[idIndex].addString(newPA[idIndex].getString(tNRows - 1)); 

                        //add data mv's
                        for (int col = idIndex + 1; col < nColumns; col++) 
                            newPA[col].addDouble(Double.NaN); 
                    }
                }
                lastSeconds2 = lastSeconds;
                lastSeconds = seconds;

                //copy row of data from oldPA to newPA
                for (int col = 0; col < nColumns; col++) 
                    newPA[col].addFromPA(oldPA[col], oldRow);

            }
            //copy newPA into table (replacing the old data)
            for (int col = 0; col < nColumns; col++) 
                table.setColumn(col, newPA[col]);        
        } catch (Exception e) {
            String2.log("ERROR in insertMVRowInTimeGaps (so table is unchanged):\n" +
                MustBe.throwableToString(e));
        }
    }

    /**
     * Generate the historical/ or nrt/ stationName.nc file for one station.
     * At this point, all station names are 5 characters long.
     * The 45day file's name will deduced from the station name.
     * The file will have no empty rows at the end.
     * The file will have no missing values in the time column.
     *
     * <p>This assumes there is a historic file, which generates file names
     * (to get here) and to form cumulative file below. But that could be changed...
     *
     * @param historicalMode if true, this makes historical files. Otherwise, nrt files.
     * @param firstNrtSeconds epochSeconds of transition instant, e.g., 2020-02-01
     * @param ndbcStationHtmlDir the directory with the station .html files
     * @param ndbcHistoricalTxtDir the source directory with the .txt files
     * @param ndbc45DayTxtDir the directory with the 45 day .txt files
     * @param ndbcHistoricalNcDir the destination directory for the historical .nc file
     * @param ndbcNrtNcDir the destination directory for the nrt .nc file
     * @param historicalFiles  a String[] with the historical files to be combined.
     *    It's okay if files aren't sorted by time -- in practice, historical 
     *    month files occur before year files.
     * @throws Exception if trouble
     */
    public static void makeStationNcFile(boolean historicalMode, double firstNrtSeconds,
        String ndbcStationHtmlDir, 
        String ndbcHistoricalTxtDir, String ndbc45DayTxtDir, 
        String ndbcHistoricalNcDir, String ndbcNrtNcDir, String historicalFiles[]) 
        throws Exception {

        //String2.log("makeStationNcFile " + historicalFiles[0] + " through " + historicalFiles[historicalFiles.length - 1]);
        String stationName = historicalFiles[0].substring(0, 5);
        String stationNameLC = stationName.toLowerCase();
        String errorInMethod = String2.ERROR + " in NdbcMetStation.makeStationNcFile(historical=" + 
            historicalMode + ", " + stationName + "):\n";

        //get information about the station
        //https://www.ndbc.noaa.gov/station_page.php?station=<stationName>
        //  a section of the html is:
        //<P><strong>Owned and maintained by National Data Station Center</strong><br>   //my 'ownedLine'
        //<strong>C-MAN station</strong><br>
        //<strong>VEEP payload</strong><br>
        //<strong>60.84 N 146.88 W (60&#176;50'24" N 146&#176;52'48" W)</strong><br>
        //<br>
        //<strong>Site elevation:</strong> 0.0 m above mean sea level<br>
        //<strong>Air temp height:</strong> 21.3 m above site elevation<br>
        //<strong>Anemometer height:</strong> 21.6 m above site elevation<br>
        //<strong>Barometer elevation:</strong> 16.5 m above mean sea level<br>
        //</P>
        String officialStationName = stationName.endsWith("_")?
            stationName.substring(0, 4) : stationName;
        String lcOfficialStationName = officialStationName.toLowerCase();

        //get the lat and lon, lat lon station locations, 
        //e.g., https://www.ndbc.noaa.gov/station_page.php?station=4h362                    
        //or list of stations https://www.ndbc.noaa.gov/to_station.shtml
        //text in the form"<strong>9.9 S 105.2 W ("
        //   or sometimes "<strong>18.0 S 85.1 W</strong><br>"
        double lat = Double.NaN;
        double lon = Double.NaN;
        StringBuilder msg = new StringBuilder();
        //no station info
        //add lat and lon if not read from station file
        //I last checked all with unknown lat lon on 2020-02-07
        if        (stationNameLC.equals("32st1")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("32st2")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("41002")) { lat = 31.887; lon =  -74.921; //not right format
        } else if (stationNameLC.equals("41nt1")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("41nt2")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("41117")) { lat = 30.000; lon =  -81.080;
        } else if (stationNameLC.equals("41119")) { lat = 33.842; lon =  -78.483; 
        } else if (stationNameLC.equals("41002")) { lat = 32.309; lon =  -75.483; 
        } else if (stationNameLC.equals("42008")) { lat = 28.700; lon =  -95.300; //but all yearly files are empty
        } else if (stationNameLC.equals("42093")) { lat = 29.017; lon =  -89.832; 
        } else if (stationNameLC.equals("42095")) { lat = 24.407; lon =  -81.967; 
        } else if (stationNameLC.equals("42a01")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("42a02")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("42a03")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("42059")) { lat = 15.252; lon =  -67.483; 
        } else if (stationNameLC.equals("42097")) { lat = 25.7;   lon =  -83.65;
        } else if (stationNameLC.equals("43010")) { lat = 10.051; lon = -125.032; 
        } else if (stationNameLC.equals("43wsl")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("44009")) { lat = 38.451; lon =  -74.684; 
        } else if (stationNameLC.equals("44089")) { lat = 37.756; lon =  -75.334;
        } else if (stationNameLC.equals("44090")) { lat = 41.840; lon =  -70.329; 
        } else if (stationNameLC.equals("44091")) { lat = 39.769; lon =  -73.770; 
        } else if (stationNameLC.equals("45028")) { lat = 46.810; lon =  -91.840; 
        } else if (stationNameLC.equals("45180")) { lat = 48.019; lon =  -87.800; 
        } else if (stationNameLC.equals("45194")) { lat = 45.804; lon =  -84.792; 
        } else if (stationNameLC.equals("46067")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("46074")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("46108")) { lat = 59.760; lon = -152.090; 
        } else if (stationNameLC.equals("46252")) { lat = 33.953; lon = -119.257; 
        } else if (stationNameLC.equals("46259")) { lat = 34.732; lon = -121.664; 
        } else if (stationNameLC.equals("46a35")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("46a54")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("47072")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4conf")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f369")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f370")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f374")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f375")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f376")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f392")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4f887")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h361")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h362")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h363")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h364")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h365")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h390")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h394")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("4h902")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("51wh1")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("51wh2")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("53anf")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("53mkf")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("a002e")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("a025w")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("b040z")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("b058m")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("cwslm")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("et01z")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("f022l")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("lonfm")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("misma")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("mnmm4")) { lat = 45.096; lon =  -87.590; //bad info in some files
        } else if (stationNameLC.equals("nkla2")) { lat = 52.972; lon = -168.855; //haven't gotten these working yet
        } else if (stationNameLC.equals("ocpn7")) { lat = 33.908; lon =  -78.148; 
        } else if (stationNameLC.equals("plsfa")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("pxoc1")) { lat = 37.798; lon = -122.393; 
        } else if (stationNameLC.equals("q004w")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("sanfm")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("siswm")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("smkfm")) { return; //lat = ; lon = ; 
        } else if (stationNameLC.equals("ssbn7")) { lat = 33.848; lon =  -78.482; 
        } else if (stationNameLC.equals("tlvt2")) { lat = 27.819; lon =  -97.454; 
        } else {

            String lines[];
            String htmlFileName = ndbcStationHtmlDir + lcOfficialStationName + ".html";
            String stationUrl = "https://www.ndbc.noaa.gov/station_page.php?station=" + 
                lcOfficialStationName;
            if (File2.isFile(htmlFileName)) {
                lines = File2.readLinesFromFile(htmlFileName, File2.UTF_8, 2).toArray(new String[0]);
            } else {
                lines = SSR.getUrlResponseArrayList(stationUrl).toArray(new String[0]);
                Test.ensureEqual(File2.writeToFileUtf8(htmlFileName, String2.toNewlineString(lines)), "", "");
            }
            msg.append("  stationUrl=" + stationUrl + "  nLines=" + lines.length + "\n");

            //get location name  extracted from "<h1 align="center">Station BLIA2 - Bligh Reef Light, AK</h1> "
            //It is ok if not found.
            int locationNameLine = String2.lineContaining(lines, "</h1>");
            String locationName = String2.extractRegex(lines[locationNameLine], " - .*</h1>", 0); 
            locationName = locationName == null? "" : 
                locationName.substring(3, locationName.length() - 5);
            msg.append(//"  locationNameLine=" + lines[locationNameLine] + "\n" +
                         "  locationName=" + locationName + "\n");

            //get the owner
            int ownerLine = String2.lineContaining(lines, " maintained by");  //Buoy Center or Station Center, or University...
            if (ownerLine == -1)
                ownerLine = String2.lineContaining(lines, "Maintained by"); 
            if (ownerLine == -1)
                ownerLine = String2.lineContaining(lines, " operated by"); 
            if (ownerLine == -1)
                ownerLine = String2.lineContaining(lines, "Owned by"); 
            if (ownerLine == -1)
                ownerLine = String2.lineContaining(lines, "Information submitted by ");         
            if (ownerLine == -1)
            Test.ensureNotEqual(ownerLine, -1, errorInMethod + "'maintained by' line not found.\n" +
                "stationUrl=" + stationUrl);
            String owner = XML.removeHTMLTags(lines[ownerLine]);
            msg.append(//"  ownerLine=" + lines[ownerLine] + "\n" +
                         "  owner=" + owner + "\n");

            //if next line is "Funding..", skip it.
            if (lines[ownerLine + 1].indexOf("Funding") >= 0)
                ownerLine++;
                                         
            //get the station type   (e.g., "C-MAN station" extracted from <strong>C-MAN station</strong><br>)
            //sometimes "Developed and maintained by <...".
            String locationRegex = "<strong>[-]?[0-9]+\\.[0-9]+ [SN]\\s+[-]?[0-9]+\\.[0-9]+ [WE] \\(.*"; //tested in Test
            String stationType;
            if (lcOfficialStationName.equals("eb52")) {
                stationType = "GE"; //no stationType, but deduce it from eb61
            } else if (lcOfficialStationName.equals("skmg1") || 
                       lcOfficialStationName.equals("spag1") ||
                       lcOfficialStationName.equals("tybg1")) {
                stationType = "US Navy Tower"; //no stationType, but derive from name on web page
                ownerLine--;
            } else if (lines[ownerLine + 1].matches(locationRegex)) {
                //next line is location
                stationType = "buoy (unknown type)";
                ownerLine--;
            } else stationType = XML.removeHTMLTags(lines[ownerLine + 1]).trim();
            msg.append(//"  stationTypeLine=" + lines[ownerLine + 1] + "\n" +
                         "  stationType=" + stationType + "\n");

            //get the payload  (some files don't have this line)
            String payload = XML.removeHTMLTags(lines[ownerLine + 2]).trim();
            if (payload.equals("LCB"))
                payload = "LCB payload";
            if (payload.toLowerCase().endsWith(" payload")) {
                payload = payload.substring(0, payload.length() - 8);
            } else {
                msg.append("nonfatal " + errorInMethod + "'payload' not found:\n" + 
                        "line-1=" + lines[ownerLine+1] + 
                      "\nline  =" + lines[ownerLine+2] + 
                      "\nline+1=" + lines[ownerLine+3] + "\n");
                payload = "";
                ownerLine--; //that line isn't in the file, so adjust for subsequent info
            }
            msg.append(//"  payloadLine=" + lines[ownerLine + 2] + "\n" +
                         "  payload=" + payload + "\n");

            //2020-02-07 explicit lat= lon= section was here
            
            String location = lines[ownerLine + 3];
            int parenPo = location.indexOf('(');
            if (parenPo > 0) 
                location = location.substring(0, parenPo);
            location = XML.removeHTMLTags(location).trim();
            String latString = String2.extractRegex(location, "[-]?[0-9]+\\.[0-9]+", 0);
            lat = String2.parseDouble(latString); //better to start as double, reduce to float if needed
            //String2.log(
            //    "line-1=" + lines[ownerLine+2] + 
            //  "\nline  =" + lines[ownerLine+3] + 
            //  "\nline+1=" + lines[ownerLine+4]);
            int snPo = location.indexOf("N");
            if (snPo < 0) {
                snPo = location.indexOf("S");
                lat *= -1;
            }
            String lonString = String2.extractRegex(location, "[-]?[0-9]+\\.[0-9]+", snPo + 1);
            lon = String2.parseDouble(lonString);
            if (location.indexOf("W") >= 0)
                lon *= -1;
            msg.append("  location='" + location + "\n");
        }
        if (Double.isNaN(lon) || Double.isNaN(lat)) {
            String2.pressEnterToContinue(
                msg.toString() +
                "!!! Not processing " + stationName + " because lat=" + lat + " and lon=" + lon + " not known.");
            return;
        }

        /* //get the station elevationInfo (ok if not found):
        StringBuilder elevationInfo = new StringBuilder();
        if (stationName.equals("46108")) { elevationInfo.append("at sea level");
        } else {
            //look on web page
            int line = ownerLine + 5;
            while (line < lines.length && 
                  !lines[line].toLowerCase().trim().startsWith("</p>")) 
                elevationInfo.append(XML.removeHTMLTags(lines[line++]).trim() + "; ");
            if (elevationInfo.length() > 400) { // too long
                String2.log("nonfatal " + errorInMethod + 
                    "elevationInfo too long: " + elevationInfo.length());
            } else {
                if (elevationInfo.length() >= 2) //replace final "; " with ". "
                    elevationInfo.replace(elevationInfo.length()-2, elevationInfo.length(), ". ");
                if (verbose) {
                    if (line == ownerLine + 5) {
                        String2.log("nonfatal " + errorInMethod + "No elevationInfo found.");
                        if (ownerLine + 6 < lines.length)
                            String2.log(
                                "line-1=" + lines[ownerLine+4] + 
                              "\nline  =" + lines[ownerLine+5] + 
                              "\nline+1=" + lines[ownerLine+6]);
                        else String2.log("(end of lines)");
                    } else String2.log("  elevationInfo=" + elevationInfo);
                }
            }
        } */

        //make the Table variable that will hold everything 
        Table cumulative = null;
        
        //HISTORICAL
        if (historicalMode) {
            //append the data from all the historicalFiles
            int nDuplicates;
            for (int file = 0; file < historicalFiles.length; file++) {
                if (//historicalFiles[file].equals("4202262008.txt") ||
                    //historicalFiles[file].equals("") ||
                    //historicalFiles[file].equals("") ||
                    //historicalFiles[file].equals("") ||
                    //historicalFiles[file].equals("") ||
                    //historicalFiles[file].equals("") ||
                    //historicalFiles[file].equals("") ||
                    //historicalFiles[file].equals("") ||
                    false) { 
                    String2.log("***Skipping known bad/empty file: " + historicalFiles[file]);
                    continue;
                }

                Table tTable = readStationTxtFile(ndbcHistoricalTxtDir + historicalFiles[file], 
                    officialStationName, (float)lon, (float)lat);

                if (cumulative == null)
                    cumulative = tTable;
                else cumulative.append(tTable); //this handles upgrading cols with simpler data types
                //if (verbose) String2.log("time0=" + cumulative.getDoubleData(timeIndex, 0));
            }

            //ensure no duplicates from different source files
            cumulative.sort(new int[]{timeIndex}, new boolean[]{true});
            nDuplicates = cumulative.removeDuplicates();  //entire row is duplicate!
            if (nDuplicates > 0)
                String2.log("!!! ENTIRELY DUPLICATE ROWS: " + nDuplicates + " for " + stationName);
            fancyRemoveDuplicateTimes(cumulative);

        } else {
            //NRT Mode

            //*** get45 day Real Time data
            String realTimeFileName = ndbc45DayTxtDir + 
                    stationName.toUpperCase() + ".txt";
            boolean hasRealTimeData = File2.isFile(realTimeFileName);
            if (hasRealTimeData) {

                //get last time from historical file
                //This is better than using expected firstNrtSeconds because many
                //  stations don't have a source file for the last month's data.
                boolean hasGapAtStart = false;
                try {
                    Table histTable = new Table();
                    histTable.read4DNc(ndbcHistoricalNcDir + "NDBC_" + stationName.toUpperCase() + "_met.nc", 
                        new String[]{"WD"}, -1, ID_NAME, idIndex); //WD will get axis variables, too
                    double lastHSeconds = histTable.getColumn("TIME").getDouble(histTable.nRows() - 1) + 1;
                    if (firstNrtSeconds - lastHSeconds > Calendar2.SECONDS_PER_HOUR) {
                        String2.log("  !!! LAST MONTH'S historical source file may be missing.");
                        hasGapAtStart = true;
                    }
                    firstNrtSeconds = lastHSeconds + 1;
                } catch (Exception e9) {
                    String2.log("Caught while trying to get last historical time:\n" + 
                        MustBe.throwableToString(e9));
                    firstNrtSeconds = 0;
                }

                //read 45 day real time data (if any)
                Table realTime = readStationTxtFile(realTimeFileName, officialStationName, 
                    (float)lon, (float)lat);

                //remove all rows from beginning with <= firstNrtSeconds
                PrimitiveArray realTimeTime = realTime.getColumn(timeIndex);
                if (verbose) String2.log(
                    "  firstNrtSeconds="    + Calendar2.epochSecondsToIsoStringTZ(firstNrtSeconds) +
                    "  first realTimeTime=" + Calendar2.epochSecondsToIsoStringTZ(realTimeTime.getDouble(0))); 
                int firstToKeep = 0;
                int realTimeTimeN = realTimeTime.size();
                while (firstToKeep < realTimeTimeN &&
                    realTimeTime.getDouble(firstToKeep) < firstNrtSeconds)
                    firstToKeep++;
                if (firstToKeep == realTimeTimeN) {
                    if (verbose) String2.log("  NO DATA IN 45day FILE=" + realTimeFileName);
                    return;
                }
                for (int col = 0; col < realTime.nColumns(); col++) {
                    realTime.getColumn(col).removeRange(0, firstToKeep); 
                    if (false) {
                        double[] cumStats  = cumulative.getColumn(col).calculateStats();
                        double[] realStats = realTime.getColumn(col).calculateStats();
                        String2.log(cumulative.getColumnName(col) + 
                            " cumMin=" + cumStats[PrimitiveArray.STATS_MIN] +
                            " realMin=" + realStats[PrimitiveArray.STATS_MIN] +
                            " cumMax=" + cumStats[PrimitiveArray.STATS_MAX] + 
                            " realMax=" + realStats[PrimitiveArray.STATS_MAX]);
                    }
                }
                //if (verbose) String2.log("    remove realTime rows before firstNrtSeconds. nRowsRemoved=" + 
                //    firstToKeep + " nRemain=" + realTime.nRows());

                if (hasGapAtStart) {
                    //insert mv row at start of table to deal with gap from historical to hrt data
                    realTime.insertBlankRow(0);
                    for (int col = 0; col <= idIndex; col++) 
                        realTime.getColumn(col).copy(1, 0);
                    PrimitiveArray timePA = realTime.getColumn(timeIndex);
                    timePA.setDouble(0, timePA.getDouble(0) - Calendar2.SECONDS_PER_HOUR);
                }

                //append   (this handles if cumulative col is simpler than realTime col)
                cumulative = realTime;
                //no metadata yet, so no need to merge it
            } else {
                if (verbose) String2.log("  NO 45day FILE.");
                return;
            } 
        }

        //cumulative is already sorted by time
        //insert mv row in time gaps
        insertMVRowInTimeGaps(cumulative);

        //last thing: addMetadata
        String stdStationName = stationName.toUpperCase();
        addMetadata(cumulative, stdStationName, lon, lat);
 
        //save as UPPERCASE-name .nc file
        String id0 = cumulative.getColumn(idIndex).getString(0);
        //String2.log("\nbefore save\n" + cumulative.toString(5));
        Test.ensureEqual(id0.length(), 5, "ID length should be 5: " + id0);
        Test.ensureTrue(((StringArray)cumulative.getColumn(idIndex)).maxStringLength() <= 5, "ID maxlength should be <= 5");
        if (id0.equals("46088")) {
            cumulative.ensureValid(); //throws Exception if trouble
            //String2.pressEnterToContinue(">>! cumulative.toString()=\n" + cumulative.toString(5));
        }
        //this calls convertToFakeMissingValues, e.g., Float.NaN becomes -9999999.0f and sets the _FillValue metadata
        cumulative.saveAs4DNcWithStringVariable(
            (historicalMode? ndbcHistoricalNcDir : ndbcNrtNcDir) + "NDBC_" + stdStationName + "_met.nc", 
            lonIndex, latIndex, depthIndex, timeIndex, idIndex);
        //if (id0.equals("46088")) String2.pressEnterToContinue(">> ncdump:\n" + NcHelper.ncdump(ndbcNcDir + "NDBC_" + stdStationName + "_met.nc", "-h"));

        //for diagnostics only: validate that times values are as expected (this is modified since last used)
        //int tNRows = cumulative.nRows();
        //DoubleArray timeArray = cumulative.getColumn(timeIndex);
        //double dataStartSeconds = Calendar2.isoStringToEpochSeconds(
        //    cumulative.getGroupAttribute(rootGroup, "time_coverage_start").getDouble(0));
        //Test.ensureEqual(timeArray.array[0], dataStartSeconds, 
        //    "bad initial time for " + stdStationName);
        //for (int row = 1; row < tNRows; row++)
        //    Test.ensureEqual(timeArray.array[row], 
        //                     timeArray.array[row - 1] + Calendar2.SECONDS_PER_HOUR, 
        //        "bad time #" + row + " for " + stdStationName);

        //check the .nc file
        //if (verbose) {
        //    Table t = new Table();
        //    t.readNetCDF(ndbcNcDir + stdStationName + ".nc");
        //    String2.log("read from .nc file:\n" + t.toString(false));
        //}


    }


    /**
     * This carefully removes rows for the same station that have duplicate times.
     * 
     * @param cumulative a standard table with the standard columns, already sorted by idIndex and timeIndex
     * @param timeIndex the time column's number
     */
    public static void fancyRemoveDuplicateTimes(Table cumulative) {
        int nRows = cumulative.nRows();
        int nCols = cumulative.nColumns();
        BitSet keep = new BitSet();
        keep.set(0, nRows);
        PrimitiveArray idPA   = cumulative.getColumn(idIndex);
        PrimitiveArray timePA = cumulative.getColumn(timeIndex);

        String id = "", oID = "";
        double time = 0, oTime = 0;
        int oRow = 0, nGood = 0, onGood = 0;
        int nDeleted = 0;
        PrimitiveArray colPAs[] = new PrimitiveArray[nCols];
        for (int col = 0; col < nCols; col++) 
            colPAs[col] = cumulative.getColumn(col);

        for (int row = 0; row < nRows; row++) {
            id = idPA.getString(row);
            time = timePA.getDouble(row);
            nGood = 0;              
            for (int col = 0; col < nCols; col++) {
                if (col != idIndex && col != timeIndex && 
                    !Float.isNaN(colPAs[col].getFloat(row)))
                    nGood++;
            }

            if (row == 0 || !oID.equals(id) || time != oTime) {
                //just go to next row
                oRow = row; oID = id; oTime = time; onGood = nGood;
                continue;
            }

            double deletedTime;
            //if (onGood != nGood)
            //    String2.log(" nGood=" + nGood + " onGood=" + onGood);
            if (onGood > nGood) {
                //delete this row
                keep.clear(row);
                deletedTime = time;
                //oRow doesn't change                
            } else { //onGood <= nGood
                //delete oRow
                keep.clear(oRow);
                deletedTime = oTime;
                oRow = row; oID = id; oTime = time; onGood = nGood;
            }
            nDeleted++;
            if (nDeleted == 1) String2.log("  First deleted fancy duplicate row is for " + 
                Calendar2.safeEpochSecondsToIsoStringTZ(deletedTime, "NaN"));
        }
        if (nDeleted > 0)  {
            cumulative.justKeep(keep);
            String2.log("!!! FANCY REMOVE DUPLICATE TIMES removed " + nDeleted + " rows.");
        }
    }

    /**
     * One time: rename a group of files
     */
    public static void oneTime() {
        /*
        //rename the ?????.txt file names    
        String names[] = RegexFilenameFilter.list(ndbcHistoricalTxtDir, ".{4,5}\\.txt"); 
        for (int i = 0; i < names.length; i++)
            File2.rename(ndbcHistoricalTxtDir, names[i], 
                names[i].substring(0, names[i].length() - 4) + "b2005.txt");


        //rename the .gz file names    
        names = RegexFilenameFilter.list(ndbcHistoricalTxtDir, ".*\\.gz"); 
        for (int i = 0; i < names.length; i++)
            File2.rename(ndbcHistoricalTxtDir, names[i], 
                names[i].substring(0, names[i].length() - 3));
        */
    }

    /**
     * Make all the station .nc files from the .txt files in ndbcHistoricalTxtDir
     * and ndbc45DayTextDir.
     *
     * @param ndbcStationHtmlDir the directory with the station's html files
     * @param ndbcHistoricalTxtDir the directory with the historical .txt files
     * @param ndbc45DayTxtDir the directory with the 45 day .txt files
     * @param ndbcHistoricalNcDir the directory for the historical files to be created in
     * @param ndbcNrtNcDir the directory for the NRT files to be created in
     * @param testMode if true, just a few files are done (for test purposes)
     * @param ignoreStationsBefore is the first station to be processed (use " " for all)
     * @throws Exception if trouble
     */
    public static void makeStationNcFiles(boolean historicalMode, double firstNrtSeconds,
        String ndbcStationHtmlDir, String ndbcHistoricalTxtDir,
        String ndbc45DayTxtDir, 
        String ndbcHistoricalNcDir, String ndbcNrtNcDir,
        String ignoreStationsBefore, 
        boolean testMode) throws Exception {

        String2.log("\n*** makeStationNcFiles...");

        //if starting from start, delete all the nc files in the historical/nrt dir
        String[] files;
        String destinationDir = historicalMode? ndbcHistoricalNcDir : ndbcNrtNcDir;
        if (ignoreStationsBefore.equals(" ")) {
            files = RegexFilenameFilter.list(destinationDir, ".*\\.nc");
            for (int i = 0; i < files.length; i++) {
                File2.delete(destinationDir + files[i]);
                //make sure they are deleted
                Test.ensureEqual(File2.isFile(destinationDir + files[i]), false, 
                    String2.ERROR + " in NdbcMetStation.makeStationNcFiles:\n" +
                    "Unable to delete " + destinationDir + files[i]);
            }
        }

        //get all the file names
        String2.log("makeStationNcFiles   getting list of historicalTxt files...");
        files = RegexFilenameFilter.list(ndbcHistoricalTxtDir, ".*\\.txt");
        if (verbose) String2.log("sorting...");
        Arrays.sort(files);

        //go through the station names
        int stationNameStart = 0;    
        String stationName = files[stationNameStart].substring(0, 5);
        for (int i = stationNameStart + 1; i <= files.length; i++) {  //yes, start at +1 since looking back to see if stationName changed, end at files.length
            //new station name?
            if (i == files.length || !files[i].substring(0, 5).equals(stationName)) {

                //make the .nc file
                if (verbose) String2.log("\nmakeStationNcFile for #" + i + " " + stationName + 
                    " start=" + stationNameStart);
                String tempNames[] = new String[i - stationNameStart];
                System.arraycopy(files, stationNameStart, tempNames, 0, i - stationNameStart);
                String lcStationName = stationName.toLowerCase();
                if (lcStationName.compareTo(ignoreStationsBefore) < 0) {  
                    //or temporary for 1 station:
                    //!lcStationName.equals(ignoreStationsBefore) || 

                } else if (
                    lcStationName.equals("42008")) { //all data files are empty
                    //skip this station

                } else if (testMode &&   
                    !lcStationName.equals("46088") && 
                    !lcStationName.equals("rcpt2")) {
                    //ignore it

                } else {
                    makeStationNcFile(historicalMode, firstNrtSeconds,
                        ndbcStationHtmlDir, ndbcHistoricalTxtDir, 
                        ndbc45DayTxtDir, 
                        ndbcHistoricalNcDir, ndbcNrtNcDir, tempNames);

                }

                //start the next stationName
                stationNameStart = i;
                if (i < files.length)
                    stationName = files[i].substring(0, 5);
            }
        }

        String2.log("makeStationNcFiles finished successfully.");
    }

    /**
     * Download all the .txt Historical files from NDBC
     * that aren't already on this computer to the ndbcHistoricalTxtDir.
     * Yearly files are from: https://www.ndbc.noaa.gov/data/historical/stdmet/
     * Monthly files are from: https://www.ndbc.noaa.gov/data/stdmet/<month3Letter>/  e.g., Jan
     *
     * <p>!!!!**** Windows GUI My Computer [was this my Win 7 computer?] 
     * doesn't show all the files in the directory! 
     * Use DOS window "dir" or Linux ls instead of the GUI.
     *
     * @param ndbcHistoricalTxtDir the directory for the historical .txt files
     * @throws Exception if trouble
     */
    public static void downloadNewHistoricalTxtFiles(String ndbcHistoricalTxtDir) throws Exception {

        //the current year
        String year = HISTORICAL_FILES_CURRENT_YEAR;      

        String2.log("\n*** downloadNewHistoricalTxtFiles...");
        StringBuilder errorsSB = new StringBuilder();

        //get the names of the available YEAR standard meteorological files
        //search for e.g., "<a href="venf1h1992.txt.gz">" 
        //  and keep                "venf1h1992.txt"
        if (true) {
            String ndbcDirectoryUrl = "https://www.ndbc.noaa.gov/data/historical/stdmet/";
            //String ndbcDirectoryUrl = "https://52.84.246.225/data/historical/stdmet/"; //git bash
            //String ndbcDirectoryUrl = "https://52.84.246.27/data/historical/stdmet/";  //windows 10
            ArrayList<String> lines = SSR.getUrlResponseArrayList(ndbcDirectoryUrl);
            StringArray fileNames = new StringArray();
            int nLines = lines.size();
            for (int i = 0; i < nLines; i++) {
                String extract = String2.extractRegex(lines.get(i), 
                    "<a href=\".{4,5}h\\d{4}\\.txt\\.gz\">", 0); //some stations (EBxx) have 4 character IDs. 'h'=year's file
                if (extract != null)
                    fileNames.add(extract.substring(9, extract.length() - 5));
            }

            //go through the station names
            for (int i = 0; i < fileNames.size(); i++) {  
                String tName = fileNames.get(i);
                String destName = tName;
                if (destName.length() == 13) //deal with 4 character station IDs
                    destName = destName.substring(0, 4) + "_" + destName.substring(4);                    
                if (File2.isFile(ndbcHistoricalTxtDir + destName)) {
                    String2.log("already exists: " + tName);
                } else {
                    String2.log("downloading:    " + tName);
                    try {
                        SSR.downloadFile(
                            ndbcDirectoryUrl + tName + ".gz",
                            ndbcHistoricalTxtDir + destName + ".gz", true); //true = use compression
                        SSR.unGzip(ndbcHistoricalTxtDir + destName + ".gz", ndbcHistoricalTxtDir, 
                            true, 60); //ignoreGzDirectories, timeOutSeconds. throws Exception
                        File2.delete(ndbcHistoricalTxtDir + destName + ".gz");
                    } catch (Exception e) {
                        String2.log(MustBe.throwableToString(e));
                        try {
                            //try again
                            String2.log("  try again:    " + tName);
                            SSR.downloadFile(
                                ndbcDirectoryUrl + tName + ".gz",
                                ndbcHistoricalTxtDir + destName, true); //true = use compression
                        } catch (Exception e2) {
                            String2.log(MustBe.throwableToString(e2));
                            errorsSB.append(MustBe.throwableToString(e2) + "\n");
                        }
                    }
                }
            }
        }


        //for each month    
        for (int month = 1; month <= 12; month++) {        

            char monthChar = "123456789abc".charAt(month - 1);

            //get the names of the available MONTH standard meteorological files
            //search for e.g., "<a href="venf1.txt">"  
            //  and keep e.g.,          "venf1"
            String ndbcDirectoryUrl = "https://" +
                "www.ndbc.noaa.gov" + //this is what it should be
                //"13.225.230.79" + //2021-05-19 tried this 'a' name for first time: handshake failure
                //"13.225.230.113" + //2021-05-19 tried this 'a' name for first time: handshake failure
                //"dpxyysg1jequ8.cloudfront.net" + //2021-05-19 tried this 'c' name for the first time: "The requested URL could not be retrieved"
                //"99.84.239.111" + 
                //"13.35.125.44" +
                "/data/stdmet/" + 
                Calendar2.getMonthName3(month) + "/";
            ArrayList<String> lines = SSR.getUrlResponseArrayList(ndbcDirectoryUrl);
            //String2.log(String2.toNewlineString(lines.toArray(new String[0])));
            StringArray fileNames = new StringArray();
            int nLines = lines.size();
            for (int i = 0; i < nLines; i++) {
                String extract = String2.extractRegex(lines.get(i), 
                    //in 2006, Jan, July, Aug, Sep names are e.g., 4100112006.txt.gz 
                    // and others are just <5charID>.txt
                    //all current stations have 5 character ID
                    //this regex works for either type of file name  
                    "<a href=\".{5}(.{5}|)\\.txt(\\.gz|)", 0); //5 digit id, month letter, 4 digit year
String2.log(lines.get(i));
String2.log("extract=" + extract);

                //If it has the year, is it the wrong year?
                if (extract != null &&
                    extract.matches("<a href=\".{10}\\.txt(\\.gz|)") &&
                    !extract.substring(15, 19).equals("" + year))
                    extract = null;
                if (extract != null)
                    fileNames.add(extract.substring(9));  //file name, as advertised
            }
            String2.log("\n" + fileNames.size() + " files found in " + ndbcDirectoryUrl);
            if (fileNames.size() > 0)
                String2.log("  [0]=" + fileNames.get(0));

            //go through the station names
            for (int i = 0; i < fileNames.size(); i++) {  
                String tName = fileNames.get(i);
                //if (tName.charAt(0) < 's') continue;  //one-time bypass
                String destName = tName.substring(0, 5) + monthChar + year + ".txt";
                if (File2.isFile(ndbcHistoricalTxtDir + destName)) {
                    String2.log("already exists: " + destName);
                } else {
                    String2.log("downloading:    " + tName);
                    try {
                        SSR.downloadFile(
                            ndbcDirectoryUrl + tName, //this works even if .txt.gz
                            ndbcHistoricalTxtDir + destName, true); //true = use compression
                    } catch (Exception e) {
                        String2.log(MustBe.throwableToString(e));
                        errorsSB.append(MustBe.throwableToString(e) + "\n");
                        //String2.pressEnterToContinue();
                        continue;
                    }
                }
            }
        }
        String2.pressEnterToContinue("Cumulative errors:\n" +
            errorsSB + 
            "downloadNewHistoricalTxtFiles finished successfully.");
    }

    /**
     * Download all the 45 day near-real-time .txt data files from
     * https://www.ndbc.noaa.gov/data/realtime2/ to the ndbc45DayTxtDir,
     * whether they already exist in the local directory or not.
     *
     * @param ndbc45DayTxtDir the directory for the 45 day .txt files
     * @throws Exception if trouble
     */
    public static void download45DayTxtFiles(String ndbc45DayTxtDir) throws Exception {
        String2.log("\n*** download45DayTxtFiles...");

        File2.deleteIfOld(ndbc45DayTxtDir, 
            System.currentTimeMillis() - 65 * Calendar2.MILLIS_PER_DAY, 
            false, false); //recursive?


        //get the names of the available real time standard meteorological files
        //search for e.g., "<a href="42OTP.txt">"
        ArrayList<String> lines = SSR.getUrlResponseArrayList(n45DayBaseUrl);
        StringArray stationNames = new StringArray();
        int nLines = lines.size();
        for (int i = 0; i < nLines; i++) {
            String extract = String2.extractRegex(lines.get(i), "<a href=\".{5}\\.txt\">", 0); //all current stations have 5 character ID
            if (extract != null)
                stationNames.add(extract.substring(9, extract.length() - 6));
        }

        //go through the station names
        long time = System.currentTimeMillis();
        for (int station = 0; station < stationNames.size(); station++) {  

            String stationName = stationNames.get(station);
            String2.log("downloading " + stationName);
            try {
                SSR.downloadFile(
                    //e.g. https://www.ndbc.noaa.gov/data/realtime2/42362.txt
                    n45DayBaseUrl + stationName + n45DaySuffix,
                    ndbc45DayTxtDir + stationName + ".txt", true); //true = use compression
            } catch (Exception e) {
                String2.log("  not found:\n" + MustBe.throwableToString(e));  //all should be found
            }
        }

        String2.log("download45DayTxtFiles finished successfully.");
    }


    /**
     * Load a .nc file and display it (don't unpack it).
     *
     * @param fullFileName
     * @param showFirstNRows
     */
    public static void displayNc(String fullFileName, int showFirstNRows) throws Exception {
        Table table = new Table();
        //this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN 
        table.read4DNc(fullFileName, null, -1, ID_NAME, idIndex); //standardizeWhat=-1
        String2.log(fullFileName + "=" + table.toString(showFirstNRows));
    }

    /** This compares the data in 2 .nc files. 
     * @return true if same
     */
    public static boolean compareCommonRows(String fileName1, String fileName2) throws Exception {
        String2.log("\n*** NdbcMetStation.compareCommonRows\n  " + fileName1 + "\n  " + fileName2);
        Table table1 = new Table();
        Table table2 = new Table();
        //this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN 
        table1.read4DNc(fileName1, null, -1, ID_NAME, idIndex); //standardizeWhat=-1
        table2.read4DNc(fileName2, null, -1, ID_NAME, idIndex); //standardizeWhat=-1
        int nRows = Math.min(table1.nRows(), table2.nRows());
        int nCols = table1.nColumns();
        String2.log("nRows=" + nRows + " nCols=" + nCols);
        boolean equal = true;
        for (int col = 0; col < nCols; col++) {
            PrimitiveArray pa1 = table1.getColumn(col);
            PrimitiveArray pa2 = table2.getColumn(col);
            pa1.removeRange(nRows, pa1.size());
            pa2.removeRange(nRows, pa2.size());
            String result = pa1.testEquals(pa2);
            if (result.length() > 0) {
                String2.log("col=" + col + ": " + result);
                equal = false;
            }
        }
        return equal;
    }
}
