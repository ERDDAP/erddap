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

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Vector;

/**
 * Get netcdf-X.X.XX.jar from
 * http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/index.html
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * 2013-02-21 new netcdfAll uses Java logging, not slf4j.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/**
 * This class had methods to generate the NdbcMet.nc file with
 * the NDBC Meteorological Station data.
 *
 * <p>Buoy Standard Meteorological file problems:
 * <ul>
 * <li> The documentation ("http://www.ndbc.noaa.gov/measdes.shtml#stdmet")
 *    says station ID's are "5-digit",  
 *    but in reality they are "4 or 5 characters".
 *    This probably is the source of problems on 
 *    http://www.ndbc.noaa.gov/historical_data.shtml for 
 *    the EBxx buoys, where the name mistakenly has an 'H' at the end
 *    and the years appear as, e.g., "970." instead of, e.g., "1970".
 *    I see that http://www.ndbc.noaa.gov/staid.shtml says "5 character"
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
 *   http://www.ndbc.noaa.gov/data/view_text_file.php?filename=41001h2000.txt.gz&dir=/ftp/data/historical/stdmet/ .
 *   This seems to be true for all files for 2000.
 * <li> There is no station information for 42a01, 42a02, 42a03, 46a35, 46a54, 
 *   47072, 4f887, misma
 *   (e.g., try http://www.ndbc.noaa.gov/station_page.php?station=42a01)
 *   but there is historical data for it
 *   (see http://www.ndbc.noaa.gov/historical_data.shtml).
 * <li> eb52 (see http://www.ndbc.noaa.gov/station_page.php?station=eb52)
 *   has no stationType info ("GE"?). 
 *   Similarly, skmg1, spag1, and tybg1, have no stationType info
 *   ("US Navy Tower"?).
 *   Similarly 41037.
 * <li>Having to "screen scrape" to get the information about the buoys
 *   (e.g., from http://www.ndbc.noaa.gov/station_page.php?station=32301)
 *   is made even more difficult because the data is not labeled
 *   (e.g., "Station location: 4.2 S 5.9 W")
 *   and different information is available for different stations.
 * <li> It would be great if http://www.ndbc.noaa.gov/historical_data.shtml 
 *   had links to the station page for each station. 
 * <li> It would be great if there were an ASCII table (or web page with PRE tag)
 *   which had a list of stations (rows) and information about the station as
 *   columns (e.g., lat, lon, type, ...).
 * <li> I need assurance that the VIS for historical and
 *   the last 45 days are both in statute miles
 *   and that TIDE for both are measured in feet.
 *   Both data types link to the same descriptions page.
 *   I note that on the station pages, VIS is in km and TIDE is in meters!
 *   Yea!: Starting with March 2007 files, files have units:
 *   VIS is miles and TIDE is in feet.
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
 * <li> The script that generates the last n hours of data 
 *   (at http://www.ndbc.noaa.gov/box_search.php?lat1=90s&lat2=90n&lon1=180w&lon2=180E&uom=M&ot=A&time=1) 
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
http://www.ndbc.noaa.gov/station_history.php?station=rtyc1
then click on rtyc162008.txt 
which leads to  
http://www.ndbc.noaa.gov/view_text_file.php?filename=rtyc162008.txt.gz&dir=data/stdmet/Jun/
which appears to be an empty file.

This problem doesn't occur for data files before or after June 2008.
 * <li>2011-02-28 The yearly 42008 files have no data 
 *    42008h1980.txt.gz 42008h1981.txt.gz  42008h1982.txt.gz 
 *    42008h1983.txt.gz 42008h1984.txt.gz 
 *    But in the last download all data, they did have data!
 *    Other small files may have no data
 *    http://www.ndbc.noaa.gov/data/historical/stdmet/?C=S;O=A
 * </ul>
 *
 * <p>The .nc files created by this class NO LONGER EXACTLY follow the 
 * Unidata Observation Dataset Convention (see
 * http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html).
 * Observation Dataset Conventions
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-12-22
 */
public class NdbcMetStation  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /** An iso date time identifying the separation between historical data (quality controlled)
     * and near real time data (less quality controlled).
     * This changes every month when I get the latest historical data.
     */
    public static String firstNearRealTimeData = "2016-09-01T00:00:00";
    /** Change current year ~Feb 28 when Jan historical files become available. */
    public static String HISTORICAL_FILES_CURRENT_YEAR = "2016";  

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
    //Units conversions: readStationTxt converts
    //  VIS from statute miles to km:  old*kmPerMile;
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
    /** The suggested minimum value for the colorbar. */
    public final static double paletteMin[] = {
  /* 0*/-180,-90,0,-2e9, 0,
  /* 5*/0,0,0,0,0, 
  /*10*/0,0,970,-10,8, 
  /*15*/0,0,-3,-5,-15,
  /*20*/-15};    
    /** The suggested maximum value for the colorbar. */
    public final static double paletteMax[] = {
  /* 0*/180,90,0,2e9,1,
  /* 5*/360,15,30,10,20,
  /*10*/20,360,1030,40,32, 
  /*15*/40,100,3,5,15,
  /*20*/15};
    /** The udUnits for each column. */
    public final static String udUnits[] = {
  /* 0*/"degrees_east", "degrees_north", "m", Calendar2.SECONDS_SINCE_1970, DataHelper.UNITLESS, 
  /* 5*/"degrees_true", "m s-1", "m s-1", "m", "s", 
  /*10*/"s", "degrees_true", "hPa", "degree_C", "degree_C", 
  /*15*/"degree_C", "km","hPa", "m", "m s-1", 
  /*20*/"m s-1"};
//  /* 0*/"LON", "LAT", "DEPTH", "TIME", ID_NAME, //use names that Lynn used in file that worked
//  /* 5*/"WD", "WSPD", "GST", "WVHT", "DPD", //most common name in ndbc files
//  /*10*/"APD", "MWD", "BAR", "ATMP", "WTMP", 
//  /*15*/"DEWP", "VIS", "PTDY", "TIDE", "WSPU", 
//  /*20*/"WSPV"};
    /** The comments for each column. Comments from http://www.ndbc.noaa.gov/measdes.shtml#stdmet */
    public final static String comments[] = {
  /* 0*/"The longitude of the station.", 
        "The latitude of the station.", 
        "The depth of the station, nominally 0 (see station information for details).",
        "Time in " + Calendar2.SECONDS_SINCE_1970 + ". The original times are rounded to the nearest hour.",
        "The station identifier.",        
  /* 5*/"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.",
        "Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.",
        "Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or two-minute period. The 5 or 8 second period can be determined by payload, See the Sensor Reporting, Sampling, and Accuracy section.",
        "Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period. See the Wave Measurements section.",
        "Dominant wave period (seconds) is the period with the maximum wave energy. See the Wave Measurements section.",
  /*10*/"Average wave period (seconds) of all waves during the 20-minute period. See the Wave Measurements section.",
        "Mean wave direction corresponding to energy of the dominant period (DOMPD). The units are degrees from true North just like wind direction. See the Wave Measurements section.",
        "Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).",
        "Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.",
        "Sea surface temperature (Celsius). For sensor depth, see Hull Description.",
  /*15*/"Dewpoint temperature taken at the same height as the air temperature measurement.",
        "Station visibility (km, originally statute miles). Note that buoy stations are limited to reports from 0 to 1.9 miles.",
        "Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.",
        "The water level in meters (originally feet) above or below Mean Lower Low Water (MLLW).",
        "The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.",
  /*20*/"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed."};

        
    /** The standard names for each column from http://cfconventions.org/Data/cf-standard-names/27/build/cf-standard-name-table.html   */
    public final static String standardName[] = {
  /* 0*/"longitude", "latitude", "depth", "time", "station_id", 
  /* 5*/"wind_from_direction", "wind_speed", "wind_speed_of_gust", "sea_surface_wave_significant_height", "sea_surface_swell_wave_period",
  /*10*/"sea_surface_swell_wave_period", "sea_surface_wave_to_direction", "air_pressure_at_sea_level", "air_temperature", "sea_surface_temperature",
  /*15*/"dew_point_temperature", "visibility_in_air", "tendency_of_air_pressure", "surface_altitude", "eastward_wind", 
  /*20*/"northward_wind"};

    /** The courtesy info for map/graph legends. */
    public static final String courtesy = "NOAA NDBC and Other Station Owners/Operators";

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
                paletteMin[col] + "` " +      //8`  
                paletteMax[col] + "` " +      //32` 
                udUnits[col];                 //degree_C`
            
        }

        //make the pointDataSets
        int oldListSize = list.size();
        PointDataSetFromStationVariables.makePointDataSets(list,
            "NDBC", //String userDataSetBaseName, 
            dir, 
            ".+\\.nc",                        //or all files
                //"(31201.nc|46088.nc|TAML1.nc)",   //or just 3 files
            variableInfo,  
            courtesy,
            minX, maxX, minY, maxY); 

        /* Now this meta data is not put in the files (so not on THREDDS)
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
     * @param url   the url, already percentEncoded as needed
     */
    public static Table readStationTxtUrl(String url, String stationID, 
            float lon, float lat) throws Exception {
        return readStationTxt(url, SSR.getUrlResponse(url), stationID, lon, lat);
    }

    /**
     * A variant of readStationTxtFile which reads data from an actual file.
     */
    public static Table readStationTxtFile(String fullFileName, String stationID, 
            float lon, float lat) throws Exception {
        //read the file
        String lines[] = String2.readLinesFromFile(fullFileName, null, 2); 
        String shortFileName = File2.getNameAndExtension(fullFileName);
        return readStationTxt(shortFileName, lines, stationID, lon, lat);
    }

   /**
     * This reads one station file and works to standardize it 
     * (combine ymdh[m] into one column, insert missing columns).
     *
     * <p>Note that this convers VIS from statute miles to km:  old*kmPerMile;
     * and converts TIDE feet to meters: old*meterPerFoot
     *
     * <p>Old style example:
     * text file: http://www.ndbc.noaa.gov/data/view_text_file.php?filename=32301h1986.txt.gz&dir=/ftp/data/historical/stdmet/
     *.gz file:   http://www.ndbc.noaa.gov/data/historical/stdmet/32301h1986.txt.gz
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
     * </pre>
     * 1hPa = 1000 bar see https://en.wikipedia.org/wiki/Bar_(unit)
     *
     * @param fileName used for diagnostic error messages
     * @param lines the String[] with the lines from the text file
     * @param stationID 4 or (usually) 5 characters, uppercase.
     *    It will be stored as 5 char, uppercase.
     * @param lon the longitude of the station  (degrees_east)
     * @param lat the latitude of the station  (degrees_north)
     * @return a table, cleaned up   with packed data values
     */
    public static Table readStationTxt(String fileName, String lines[], 
            String stationID, float lon, float lat) throws Exception {

        String errorInMethod = String2.ERROR + " in NdbcMetStation.readStationTxt\n" +
            "fileName=" + fileName + "\n";
 
         //ensure column names on line 0
        int columnNamesLine = String2.lineContaining(lines, " MM DD hh");
        Test.ensureNotEqual(columnNamesLine, -1, 
            errorInMethod + "columnNames not found.\n" +
              "fullFileName=" + fileName + 
              (lines.length > 1? "\nline 0=" + lines[0] : "") + 
              (lines.length > 2? "\nline 1=" + lines[1] : "") + 
              (lines.length > 3? "\nline 2=" + lines[2] : "") + 
              (lines.length > 4? "\nline 3=" + lines[3] : ""));

        //ensure data starts on line 1 (before March 2007) or 2 (March 2007 and after)
        int dataStartLine = columnNamesLine + 1;
        while (lines[dataStartLine].startsWith("#"))
            dataStartLine++;

        //replace the various mv's with NaN's
        //if (verbose) String2.log("firstLineBefore=" + lines[dataStartLine]);
        Pattern pattern = Pattern.compile("\\S [9]+(\\.[0]+)? ");
        for (int i = dataStartLine; i < lines.length; i++) {
            //http://www.ndbc.noaa.gov/measdes.shtml#stdmet says
            //"Any data field that contains "9 filled" represents missing data
            //  for that observation hour. (Example: 999.0, 99.0, 99.00)"
            //trouble with Dave's script and simple replacements: BAR has legit value 999.0 and mv of 9999.0
            //  but he may be not saving BAR
            //I can't use String.replaceAll because the space at end of one pattern
            //  is the space at beginning of next pattern.
            lines[i] += ' '; //so pattern works in interior and at end
            Matcher matcher = pattern.matcher(lines[i]);
            int matcherPo = 0;
            while (matcher.find(matcherPo)) {
                lines[i] = lines[i].substring(0, matcher.start() + 1) + //+1: keep the non-space char at beginning of match
                    " NaN " + lines[i].substring(matcher.end());
                matcherPo = matcher.start() + 4;
                matcher = pattern.matcher(lines[i]);
            }
            lines[i] = lines[i].replaceAll(" [M]+", " NaN"); //first param is regex
            lines[i] = lines[i].trim();
        }
        //if (verbose) String2.log("firstLineAfter =" + lines[dataStartLine]);

        //read the data into the table
        Table table = new Table();
        table.allowRaggedRightInReadASCII = true;
        table.readASCII(fileName, lines, columnNamesLine, dataStartLine, null, null, null, null, true);
        int nRows = table.nRows();

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
                Test.ensureBetween(yearColumn.getInt(i), 1800, 2200,
                    errorInMethod + "Year in YYYY column: row=" + i + ".");
                Test.ensureBetween(monthColumn.getInt(i), 1, 12, 
                    errorInMethod + "Month in MM column: row=" + i + ".");
                Test.ensureBetween(dayColumn.getInt(i), 1, 31, 
                    errorInMethod + "Day in DD column: row=" + i + ".");
                Test.ensureBetween(hourColumn.getInt(i), 0, 23, 
                    errorInMethod + "Hour in hh column: row=" + i + ".");
                if (hasMinuteColumn) {
                    int mm = minuteColumn.getInt(i);
                    Test.ensureBetween(mm, 0, 59,
                        errorInMethod + "Minute in mm column: row=" + i + ".");
                    //round to nearest hour
                    minuteColumn.setInt(i, mm < 30? 0 : 60);
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

        //insert the longitude, latitude, and altitude variables
        float[] lonFA = new float[nRows];
        Arrays.fill(lonFA, lon);
        table.addColumn(lonIndex, metColumnNames[lonIndex], new FloatArray(lonFA)); 

        float[] latFA = new float[nRows];
        Arrays.fill(latFA, lat);
        table.addColumn(latIndex, metColumnNames[latIndex], new FloatArray(latFA));

        float[] altitudeFA = new float[nRows];
        Arrays.fill(altitudeFA, 0);
        table.addColumn(depthIndex, metColumnNames[depthIndex], new FloatArray(altitudeFA)); 

        //insert the stationID column
        String[] idSA = new String[nRows];
        String stationID5 = stationID.toUpperCase();
        if (stationID5.length() == 4)
            stationID5 += "_";
        Arrays.fill(idSA, stationID5);
        //String2.log("stationID5=" + stationID5 + " nRows=" + nRows);
        table.addColumn(idIndex, metColumnNames[idIndex], new StringArray(idSA));

        //if aprsIndex column is named BARO or PRES, rename it to BAR
        if (table.getColumnName(aprsIndex).equals("BARO") ||
            table.getColumnName(aprsIndex).equals("PRES")) //added March 2007
            table.setColumnName(aprsIndex, "BAR");

        //add ptdy column at end (if not already there)
        int nColumns = table.nColumns();
        Test.ensureEqual(table.getColumnName(ptdyIndex - 1), "VIS", 
            errorInMethod + "Unexpected columnName #" + (ptdyIndex - 1) + "." +
            String2.toCSSVString(table.getColumnNames()));
        if (nColumns <= ptdyIndex || !table.getColumnName(ptdyIndex).equals("PTDY")) {
            float[] ptdyFA = new float[nRows];
            Arrays.fill(ptdyFA, Float.NaN);
            table.addColumn(ptdyIndex, "PTDY", new FloatArray(ptdyFA));
            nColumns++;
        }

        //add tide column at end (if not already there)
        if (nColumns <= tideIndex || !table.getColumnName(tideIndex).equals("TIDE")) {
            float[] tideFA = new float[nRows];
            Arrays.fill(tideFA, Float.NaN);
            table.addColumn(tideIndex, "TIDE", new FloatArray(tideFA));
            nColumns++;
        }

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
                //  (see WDIR at http://www.ndbc.noaa.gov/measdes.shtml#stdmet)
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
            "\nexpected: " + String2.toCSSVString(metColumnNames) +
            "\nobserved: " + String2.toCSSVString(table.getColumnNames());
        Test.ensureEqual(table.nColumns(), metColumnNames.length, columnError);
        for (int i = 0; i < metColumnNames.length; i++) 
            Test.ensureEqual(table.getColumnName(i), metColumnNames[i], columnError);

        //convert  VIS statute miles to km
        PrimitiveArray oldVis = table.getColumn(visIndex);  //may be byteArray if all missing values or ints
        FloatArray newVis = new FloatArray(nRows, true); //ensure it handles floating point values
        table.setColumn(visIndex, newVis);
        for (int i = 0; i < nRows; i++)
            newVis.setDouble(i, Math2.roundTo(oldVis.getDouble(i) * Math2.kmPerMile, decimalDigits[visIndex]));
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

        //force all columns after idIndex to be float (or short if wdIndex or mwdIndex)
        //41009 GST is currently StringArray (probably stray text)
        for (int col = idIndex + 1; col < table.nColumns(); col++) {
            PrimitiveArray pa = table.getColumn(col);
            if (col == wdIndex || col == mwdIndex) {
                if (!(pa instanceof ShortArray)) {
                    table.setColumn(col, new ShortArray(pa));
                }
            } else if (!(pa instanceof FloatArray)) {
                table.setColumn(col, new FloatArray(pa));
            }
        }

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

        //if (verbose) String2.log("table at end of readStationText:\n" + table.toString("row", 5));

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
            //double lastHistoricalTime, String payload, String elevationInfo, 
            //boolean dataIsScaled) {

/*
     * @param stationUrl E.g., "http://www.ndbc.noaa.gov/station_page.php?station=42362". May be "".
     * @param owner E.g., "Owned and maintained by National Data Buoy Center". May be "".
     * @param locationName e.g., "Bligh Reef Light, AK".  May be "".
     * @param stationType e.g., "C-MAN station". May be "".
     * @param lastHistoricalTime the time (seconds since epoch) of the last
     *    historical time in the file (data is quality controlled). 
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
        Attributes tableGlobalAttributes = table.globalAttributes();
        String tConventions = "COARDS, CF-1.6, ACDD-1.3";
        tableGlobalAttributes.set("Conventions", tConventions);      
        String title = "NOAA NDBC Standard Meteorological";
            //For now, don't put station specific info because THREDDS aggregates and just shows one station's info
            //Data from " + stationType + " " + stationName;
        tableGlobalAttributes.set("title",  title);
        tableGlobalAttributes.set("summary", 
            //Bob wrote:
            "The National Data Buoy Center (NDBC) distributes meteorological data from " + 
            "moored buoys maintained by NDBC and others. " +
            //from http://www.ndbc.noaa.gov/mooredbuoy.shtml
            "Moored buoys are the weather sentinels of the sea. They are deployed in the coastal and " +
            "offshore waters from the western Atlantic to the Pacific Ocean around Hawaii, and from " +
            "the Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit barometric " +
            "pressure; wind direction, speed, and gust; air and sea temperature; and wave energy spectra " +
            "from which significant wave height, dominant wave period, and average wave period are derived. " +
            "Even the direction of wave propagation is measured on many moored buoys. " +
            "\n\nThis dataset has both historical data (quality controlled, before " + firstNearRealTimeData + ") " +
            "and near real time data (less quality controlled, from " + firstNearRealTimeData + " on)."
            //For now, don't put station specific info because THREDDS or the CWBrowsers aggregate 
            //and just shows one station's info.
            //, " +
            //"with a switch over date and time as noted by NDBCStationLastHistoricalTime. " +
            //(locationName == null || locationName.length() == 0 ? "" : "Location: " + locationName + ". ") + 
            //(payload.length() > 0? payload + " payload. " : "") +
            //elevationInfo
            ); 
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
        //    Calendar2.epochSecondsToIsoStringT(lastHistoricalTime) + "Z");
        //tableGlobalAttributes.set("id", "NdbcMeteorologicalStation" + stationName);
      
        tableGlobalAttributes.set("NDBCMeasurementDescriptionUrl",  
            "http://www.ndbc.noaa.gov/measdes.shtml");
        tableGlobalAttributes.set("keywords", "Oceans"); //part of line from http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
        //skip keywords vocabulary since not using it strictly
        tableGlobalAttributes.set("naming_authority", "gov.noaa.pfeg.coastwatch");
        tableGlobalAttributes.set("cdm_data_type", "Station");
        //skip 'history'// this is called by each addNDays, so don't add to history repeatedly
        String todaysDate = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10) + "Z";
        tableGlobalAttributes.set("date_created", todaysDate); 
        //by reorganizing the data, we are the creators of these files
        //but the institution (courtesy info) is NDBC
        tableGlobalAttributes.set("creator_name", "NOAA CoastWatch, West Coast Node");
        tableGlobalAttributes.set("creator_url", "http://coastwatch.pfeg.noaa.gov");
        tableGlobalAttributes.set("creator_email", "dave.foley@noaa.gov");
        //tableGlobalAttributes.set("creator_name", "NOAA National Data Buoy Center");
        //tableGlobalAttributes.set("creator_url", "http://www.ndbc.noaa.gov");
        //tableGlobalAttributes.set("creator_email", "webmaster.ndbc@noaa.gov");
        tableGlobalAttributes.set("institution", "NOAA National Data Buoy Center and Participators in Data Assembly Center.");
        tableGlobalAttributes.set("history", "NOAA NDBC");
        tableGlobalAttributes.set("project", "NOAA NDBC and NOAA CoastWatch (West Coast Node)");
        tableGlobalAttributes.set("acknowledgement",  "NOAA NDBC and NOAA CoastWatch (West Coast Node)");
        tableGlobalAttributes.set("contributor_name", "NOAA NDBC and NOAA CoastWatch (West Coast Node)");
        tableGlobalAttributes.set("contributor_role", "Source of data."); 

        tableGlobalAttributes.set("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
        tableGlobalAttributes.set("license", "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither NOAA, NDBC, CoastWatch, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.");
        tableGlobalAttributes.set("date_issued", todaysDate);
        //'comment' could include other information about the station, its history 
        tableGlobalAttributes.set("source", "station observation");
        //attributes unique in their opendap files
        tableGlobalAttributes.set("quality", "Automated QC checks with periodic manual QC");
        //tableGlobalAttributes.set("", ""}));

        table.columnAttributes(depthIndex).set("positive", "down");
        table.columnAttributes(timeIndex).set("point_spacing", "even");
       
        //setActualRangeAndBoundingBox     this does a lot:
        //  e.g., table.columnAttributes(lonIndex  ).set("axis", "X");
        //  e.g., tableGlobalAttributes.set("Southernmost_Northing", lat);
        //  e.g., tableGlobalAttributes.set("geospatial_lat_min",  lat);
        //  e.g., tableGlobalAttributes.set("time_coverage_start", Calendar2.epochSecondsToIsoStringT(table.getDoubleData(timeIndex, 0)) + "Z");
        //  e.g., table.columnAttributes(lonIndex  ).set("_CoordinateAxisType", "Lon");
        //but it doesn't set
        tableGlobalAttributes.set("time_coverage_resolution", "P1H");
        tableGlobalAttributes.set("geospatial_vertical_positive", "down"); //since DChart wants depth 
        table.setActualRangeAndBoundingBox(
            lonIndex, latIndex, depthIndex, -1, timeIndex, "Centered Time of 1 Hour Averages");

        for (int col = 0; col < metColumnNames.length; col++) {

            Attributes tableColumnAttributes = table.columnAttributes(col);
            tableColumnAttributes.set("long_name", longNames[col]);
            tableColumnAttributes.set("units",     udUnits[col]);
            tableColumnAttributes.set("comment",   comments[col]);
            if (standardName[col] != null)
                tableColumnAttributes.set("standard_name", standardName[col]);

            //if (col > timeIndex) 
            //    tableColumnAttributes.set("coordinates", 
            //        metColumnNames[latIndex] + " " + //yes, lat first   Unidata CF-1 method [huh? which standard? where?]
            //        metColumnNames[lonIndex] + " " +
            //        metColumnNames[depthIndex] + " " +
            //        metColumnNames[timeIndex]);

        }
            
    }
  
    /**
     * This updates the files with the last 45 days Info every 60 minutes.
     * The updating takes about 3 minutes, after which this sleeps.
     *
     * @param ncDir the directory with the individual .nc files (with slash at end)
     * @throws Exception
     */
    public static void updateEveryHour(String ncDir, int timeOutMinutes) 
            throws Exception {
        while (true) {
            long time = System.currentTimeMillis();
            try {
                addLastNDaysInfo(ncDir, 5, false);
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
        String[] sar = SSR.getUrlResponse(url);
        int nLines = sar.length;
        StringArray sa = new StringArray();

        //look for pre
        int line = 0;
        while (line < nLines && (sar[line] == null || sar[line].indexOf("<pre>") < 0)) 
            line++;
        if (line >= nLines)
            return sa;

        //gather the text matching regex
        while (line < nLines) {
            String s = sar[line];
            if (s != null) 
                s = String2.extractRegex(s, regex, 0);
            if (s != null) 
                sa.add(s);
            if (sar[line] != null && sar[line].indexOf("</pre>") >= 0)
                return sa;
            line++;
        }
        return sa;
    }

    
    /**
     * This adds the last nDays of real time data to the individual and combined 
     * .nc files.
     *
     * <pre>example: http://www.ndbc.noaa.gov/data/5day2/AUGA2_5day.txt
     * YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS PTDY  TIDE
     * 2006 03 02 16 00 050 13.4 16.5    MM    MM    MM  MM 1013.8  -6.5    MM    MM   MM -2.0    MM
     * 2006 03 02 15 30 050 12.9 14.9    MM    MM    MM  MM 1014.2  -6.3    MM    MM   MM   MM    MM
     * 2006 03 02 15 00 050 12.4 14.4    MM    MM    MM  MM 1014.6  -6.3    MM    MM   MM -1.8    MM
     *
     * file meteorological .txt files from: http://www.ndbc.noaa.gov/data/realtime2/
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

     * http://www.ndbc.noaa.gov/data/realtime2/AUGA2.txt    //45 day
     * YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS PTDY  TIDE
     * 2006 03 02 16 00 050 13.4 16.5    MM    MM    MM  MM 1013.8  -6.5    MM    MM   MM -2.0    MM
     * 2006 03 02 15 30 050 12.9 14.9    MM    MM    MM  MM 1014.2  -6.3    MM    MM   MM   MM    MM
     * </pre>
     *
     *
     * @param ncDir the directory with the individual and combined .nc files (with slash at end)
     * @param nDays the number of days (5 or 45).
     *    (Since many buoys have 45 day files, but not 5 day files, e.g., 41038,
     *    the 5 option will also look for a 45 day file if there is no 5 day file.
     *    And for good measure, the 45 option looks for 5 day file if no 45 day file.)
     * @param testMode if true, only station 46088 is done
     */
    public static void addLastNDaysInfo(String ncDir, int nDays, boolean testMode) throws Exception {

        String2.log("NdbcMetStation.addLastNDaysInfo nDays=" + nDays + " testMode=" + testMode + 
            "\n  ncDir=" + ncDir); 
        String errorInMethod = String2.ERROR + " in NdbcMetStation.addLastNDayInfo:\n";
        long time = System.currentTimeMillis();
        if (nDays != 5 && nDays != 45) 
            Test.error(errorInMethod + "unsupported nDays: " + nDays);
        long stationNcReadTime = 0; 
        long stationNcWriteTime = 0; 
        long cumulativeNDayReadTime = 0;
 
        //get list of current stations with station.nc files, e.g., NDBC_46088_met.nc
        String stationList[] = RegexFilenameFilter.list(ncDir, "NDBC_.+_met\\.nc");
        for (int i = 0; i < stationList.length; i++)
            stationList[i] = stationList[i].substring(5, 10);

        //get list of nDay files from ndbc
        String n5DayBaseUrl = "http://www.ndbc.noaa.gov/data/5day2/";
        String n45DayBaseUrl = "http://www.ndbc.noaa.gov/data/realtime2/";
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

        int nRecordsAdded = 0;
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
                tBaseUrl = "http://www.ndbc.noaa.gov/data/5day2/";
                tSuffix = "_5day.txt";
                n5DayStations++;
            } else if (tNDays == 0 && n45DayFileNames.indexOf(stationID, 0) >= 0) { //note first test is of tNDays
                tNDays = 45;
                tBaseUrl = "http://www.ndbc.noaa.gov/data/realtime2/";
                tSuffix = ".txt";
                n45DayStations++;
            } else if (tNDays == 0 && n5DayFileNames.indexOf(stationID, 0) >= 0) { //note first test is of tNDays
                tNDays = 5;
                tBaseUrl = "http://www.ndbc.noaa.gov/data/5day2/";
                tSuffix = "_5day.txt";
                n5DayStations++;
            } else continue;

            //next line is used while working on this method
            //if (testMode && stationID.compareTo("P") < 0) continue;
            if (testMode && !stationID.equals("46088")) continue;
            //if (testMode && String2.isDigit(stationID.charAt(0))) continue;
            if (verbose) String2.log("updating tNDays=" + tNDays + ", station " + 
                station + "=" + stationID);
            int tNRecordsAdded = 0;                      

            try { //so error for one station doesn't affect next station

                //read the station's .nc file  
                //(I'm going to rewrite it completely, so just load it all.)
                Table table = new Table();
                stationNcReadTime -= System.currentTimeMillis();
                table.read4DNc(ncDir + "NDBC_" + stationID + "_met.nc", null, 0, ID_NAME, 4); //null=it finds columns 0=nothing to unpack
                stationNcReadTime += System.currentTimeMillis();
                Attributes tableGlobalAttributes = table.globalAttributes();
                //String stationName = tableGlobalAttributes.getString("NDBCStationID");
                //String stationUrl = tableGlobalAttributes.getString("NDBCStationUrl");
                //String stationOwner = tableGlobalAttributes.getString("NDBCStationOwner");
                //String stationLocation = tableGlobalAttributes.getString("NDBCStationLocation");
                //String stationType = tableGlobalAttributes.getString("NDBCStationType");
                double stationLon = tableGlobalAttributes.getDouble("geospatial_lon_min");
                double stationLat = tableGlobalAttributes.getDouble("geospatial_lat_min");
                //String stationLastHistoricalTime = tableGlobalAttributes.getString("NDBCStationLastHistoricalTime");
                //Test.ensureNotNull(stationLastHistoricalTime, 
                //    errorInMethod + "stationLastHistoricalTime is null.");
                //double doubleStationLastHistoricalTime = Calendar2.isoStringToEpochSeconds(  //catch exception?
                //    stationLastHistoricalTime);
                //String stationPayload = tableGlobalAttributes.getString("NDBCStationPayload");
                //String stationElevationInfo = tableGlobalAttributes.getString("NDBCStationElevationInfo");
                //String2.log("rewriting station file: " + 
                //    "\n stationName=" + stationName +
                //    "\n stationUrl=" + stationUrl +
                //    "\n stationOwner=" + stationOwner +
                //    "\n stationLocation=" + stationLocation +
                //    "\n stationType=" + stationType +
                //    "\n stationLon=" + stationLon +
                //    "\n stationLat=" + stationLat +
                //    "\n stationLastHistoricalTime=" + stationLastHistoricalTime +
                //    "\n stationPayload=" + stationPayload +
                //    "\n stationElevationInfo=" + stationElevationInfo);

                //get the station's nDay file
                long tcTime = System.currentTimeMillis();
                Table nDayTable = readStationTxtUrl(tBaseUrl + stationID + tSuffix, 
                    stationID, (float)stationLon, (float)stationLat);
                cumulativeNDayReadTime += System.currentTimeMillis() - tcTime;
                int nDayTableNRows = nDayTable.nRows();

                //round all nDayTable times to nearest hour
                if (true) {   //in braces so timeColumn will be gc'd
                    DoubleArray timeColumn = (DoubleArray)nDayTable.getColumn(timeIndex);
                    for (int i = 0; i < nDayTableNRows; i++)
                        timeColumn.set(i, 
                            Math2.roundToInt(timeColumn.get(i) / Calendar2.SECONDS_PER_HOUR) * 
                                Calendar2.SECONDS_PER_HOUR);
                }


                //calculate the first and last relevant row numbers (for the rows in the nDay file) in the station .nc file
                double firstNDayTime = nDayTable.getDoubleData(timeIndex, nDayTableNRows - 1); //oldest time is last in file
                double lastNDayTime  = nDayTable.getDoubleData(timeIndex, 0); //newest time is first in file
                double ncFirstTime = table.getColumn(timeIndex).getDouble(0); 
                double ncLastTime  = table.getColumn(timeIndex).getDouble(table.nRows() - 1);
                //String2.log("stationLastHistoricalTime=" + stationLastHistoricalTime +
                //    "\nfirstNDayTime=" + Calendar2.epochSecondsToIsoStringT(firstNDayTime) +
                //    "\nlastNDayTime=" + Calendar2.epochSecondsToIsoStringT(lastNDayTime) +
                //    "\nncFirstTime=" + Calendar2.epochSecondsToIsoStringT(ncFirstTime) +
                //    "\nncLastTime=" + Calendar2.epochSecondsToIsoStringT(ncLastTime));

                double lastNDayHour = Math2.roundToInt(lastNDayTime / Calendar2.SECONDS_PER_HOUR) * Calendar2.SECONDS_PER_HOUR;
                if (lastNDayHour > ncLastTime) {

                    //but not before lastHistoricalTime
                    firstNDayTime = Math.max(firstNDayTime, ncLastTime); //doubleStationLastHistoricalTime);
                    lastNDayTime = Math.max(firstNDayTime, lastNDayTime);

                    int firstNcRow = Math2.roundToInt((firstNDayTime - ncFirstTime) / Calendar2.SECONDS_PER_HOUR);
                    int lastNcRow  = Math2.roundToInt((lastNDayTime  - ncFirstTime) / Calendar2.SECONDS_PER_HOUR);
                    firstNcRow = Math.max(0, firstNcRow);
                    //if (verbose) String2.log("firstNcRow=" + firstNcRow + " lastNcRow=" + lastNcRow);

                    //calculate rowNumber in station .nc related to each nDayTable row
                    IntArray relatedStationRowNumber = new IntArray(nDayTableNRows, false);
                    PrimitiveArray nDayTableTimes = nDayTable.getColumn(timeIndex);
                    for (int i = 0; i < nDayTableNRows; i++) 
                        relatedStationRowNumber.add(Math2.roundToInt(
                            (nDayTableTimes.getDouble(i) - ncFirstTime) / Calendar2.SECONDS_PER_HOUR));

                    //column by column
                    for (int col = 0; col < metColumnNames.length; col++) {
                        Test.ensureEqual(table.getColumnName(col), nDayTable.getColumnName(col), 
                            errorInMethod + "The column names are different.");

                        //get the table column
                        PrimitiveArray nDayTableColumn = nDayTable.getColumn(col);

                        //read the packed data from the .nc file
                        PrimitiveArray stationColumn = table.getColumn(col);

                        //row-by-row of nDayTable ...
                        for (int row = 0; row < nDayTableNRows; row++) {

                            int tRelatedStationRowNumber = relatedStationRowNumber.get(row);
                            //if (col == timeIndex) String2.log("tRelatedRowNumber=" + tRelatedStationRowNumber + 
                            //    " firstNcRow=" + firstNcRow + " lastNcRow=" + lastNcRow);
                            if (tRelatedStationRowNumber < firstNcRow || 
                                tRelatedStationRowNumber > lastNcRow)
                                continue;

                            //add rows to the stationColumn if needed
                            while (stationColumn.size() <= tRelatedStationRowNumber) {
                                //add rows to the stationColumn
                                if (col == idIndex) {
                                    //id column
                                    stationColumn.addString(stationID);
                                } else if (col < timeIndex) {
                                    //lon lat depth
                                    stationColumn.addDouble(
                                        stationColumn.getDouble(stationColumn.size() - 1));
                                } else if (col == timeIndex) {
                                    //time column
                                    tNRecordsAdded++;
                                    stationColumn.addDouble(
                                        stationColumn.getDouble(stationColumn.size() - 1) +
                                        Calendar2.SECONDS_PER_HOUR); //increment time
                                } else { 
                                    //all other columns
                                    stationColumn.addDouble(Double.NaN);
                                }
                            }
                            
                            //transfer the data
                            double nDayTableValue = nDayTableColumn.getDouble(row);
                            if (col == timeIndex) {
                                ncLastTime = Math.max(ncLastTime, nDayTableValue);
                            } else if (col > idIndex) {
                                double oldValue = stationColumn.getDouble(tRelatedStationRowNumber);
                                //if (metColumnNames[col].equals("DPD"))
                                //    String2.log(" col=" + metColumnNames[col] + 
                                //          " oldValue=" + oldValue + " nDayTableValue=" + nDayTableValue);
                                if (Double.isNaN(oldValue) && !Double.isNaN(nDayTableValue)) {
                                    stationColumn.setDouble(tRelatedStationRowNumber,  //setDouble will round to float or short if needed
                                        nDayTableValue); //it is already packed (by readStationTxt
                                }
                            }
                        }
                    }

                    //rewrite the station .nc file
                    addMetadata(table, stationID, 
                        //stationUrl, stationOwner,  stationLocation, stationType, 
                        stationLon, stationLat); 
                        //doubleStationLastHistoricalTime, stationPayload, 
                        //stationElevationInfo, 
                        //false); //false=dataIsScaled
                    stationNcWriteTime -= System.currentTimeMillis();
                    table.saveAs4DNcWithStringVariable(ncDir + "NDBC_" + stationID + "_met.nc", 
                        lonIndex, latIndex, depthIndex, timeIndex, 4);
                    stationNcWriteTime += System.currentTimeMillis();
                } else if (lastNDayHour == ncLastTime) {
                    String2.log("  lastNDayHour(" + lastNDayHour + ") = ncLastTime(" + ncLastTime + "=" +
                        Calendar2.epochSecondsToIsoStringT(ncLastTime) + ")");
                } else {
                    String2.log("  lastNDayHour(" + lastNDayHour + ") < ncLastTime(" + ncLastTime + "=" +
                        Calendar2.epochSecondsToIsoStringT(ncLastTime) + ") !!!");
                }
            } catch (Exception e) {
                String2.log(MustBe.throwable(errorInMethod, e));
            } 

            if (verbose) String2.log("  tNRecordsAdded=" + tNRecordsAdded); 
            nRecordsAdded += tNRecordsAdded;
            nStationsUpdated++;
        } //end station


       String2.log("NdbcMet.addLastNDaysInfo finished successfully in " + //always write this
           Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + 
           "\n  nRecordsAdded=" + nRecordsAdded + 
           " n5DayStationsUpdated=" + n5DayStations +
           " n45DayStationsUpdated=" + n45DayStations +
           "\n  stationNcReadTime=" + (stationNcReadTime/1000) +
           "s stationNcWriteTime=" + (stationNcWriteTime/1000) +
           "s cumulativeNDayReadTime=" + (cumulativeNDayReadTime/1000) + "s");
    }

    /**
     * Generate the <ndbcNcDir>stationName.nc file for one station.
     * At this point, all station names are 5 characters long.
     * The 45day file's name will deduced from the station name.
     * The file will have no empty rows at the end.
     * The file will have no missing values in the time column.
     *
     * <p>This assumes there is a historic file, which generates file names
     * (to get here) and to form cumulative file below. But that could be changed...
     *
     * <p>The resulting file will have a row for every hour from the 
     * start time to the end time.  Data for minutes 1 - 59 will be discarded.
     * Rows with missing values (for the data) will be inserted as needed.
     *
     * @param ndbcStationHtmlDir the directory with the station .html files
     * @param ndbcHistoricalTxtDir the source directory with the .txt files
     * @param ndbc45DayTxtDir the directory with the 45 day .txt files
     * @param ndbcNcDir the destination directory for the .nc file
     * @param historicalFiles  a String[] with the historical files to be combined.
     * @throws Exception if trouble
     */
    public static void makeStationNcFile(String ndbcStationHtmlDir, 
        String ndbcHistoricalTxtDir, String ndbc45DayTxtDir, 
        String ndbcNcDir, String historicalFiles[]) 
        throws Exception {

        String stationName = historicalFiles[0].substring(0, 5);
        String errorInMethod = String2.ERROR + " in NdbcMetStation.makeStationNcFile(" + stationName + "):\n";
        if (verbose) String2.log("station=" + stationName + " getting station info..."); 

        //get information about the station
        //http://www.ndbc.noaa.gov/station_page.php?station=<stationName>
        //  a section of the html is:
        //<P><b>Owned and maintained by National Data Station Center</b><br>   //my 'ownedLine'
        //<b>C-MAN station</b><br>
        //<b>VEEP payload</b><br>
        //<b>60.84 N 146.88 W (60&#176;50'24" N 146&#176;52'48" W)</b><br>
        //<br>
        //<b>Site elevation:</b> 0.0 m above mean sea level<br>
        //<b>Air temp height:</b> 21.3 m above site elevation<br>
        //<b>Anemometer height:</b> 21.6 m above site elevation<br>
        //<b>Barometer elevation:</b> 16.5 m above mean sea level<br>
        //</P>
        String officialStationName = stationName.endsWith("_")?
            stationName.substring(0, 4) : stationName;
        String lcOfficialStationName = officialStationName.toLowerCase();
        String lines[];
        String htmlFileName = ndbcStationHtmlDir + lcOfficialStationName + ".html";
        String stationUrl = "http://www.ndbc.noaa.gov/station_page.php?station=" + 
            lcOfficialStationName;
        if (File2.isFile(htmlFileName)) {
            lines = String2.readLinesFromFile(htmlFileName, null, 2);
        } else {
            lines = SSR.getUrlResponse(stationUrl);
            Test.ensureEqual(String2.writeToFile(htmlFileName, String2.toNewlineString(lines)), "", "");
        }
        if (verbose) 
            String2.log("  stationUrl=" + stationUrl +
                      "\n  nLines=" + lines.length);

        //get location name  extracted from "<h1 align="center">Station BLIA2 - Bligh Reef Light, AK</h1> "
        //It is ok if not found.
        int locationNameLine = String2.lineContaining(lines, "</h1>");
        String locationName = String2.extractRegex(lines[locationNameLine], " - .*</h1>", 0); 
        locationName = locationName == null? "" : 
            locationName.substring(3, locationName.length() - 5);
        if (verbose) String2.log(//"  locationNameLine=" + lines[locationNameLine] + "\n" +
                                 "  locationName=" + locationName);

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
        if (verbose) String2.log(//"  ownerLine=" + lines[ownerLine] + "\n" +
                                 "  owner=" + owner);

        //if next line is "Funding..", skip it.
        if (lines[ownerLine + 1].indexOf("Funding") >= 0)
            ownerLine++;
                                     
        //get the station type   (e.g., "C-MAN station" extracted from <b>C-MAN station</b><br>)
        //sometimes "Developed and maintained by <...".
        String locationRegex = "<b>[-]?[0-9]+\\.[0-9]+ [SN]\\s+[-]?[0-9]+\\.[0-9]+ [WE] \\(.*"; //tested in Test
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
        /*} else if (lcOfficialStationName.equals("32487") || 
                   lcOfficialStationName.equals("32488") || 
                   lcOfficialStationName.equals("41037") || 
                   lcOfficialStationName.equals("41038") ||
                   lcOfficialStationName.equals("42067") ||
                   lcOfficialStationName.equals("46235") ||
                   lcOfficialStationName.equals("alia2") || 
                   lcOfficialStationName.equals("alxn6") || 
                   lcOfficialStationName.equals("atka2") || 
                   lcOfficialStationName.equals("elfa2") || 
                   lcOfficialStationName.equals("iloh1") || 
                   lcOfficialStationName.equals("nwwh1") || 
                   lcOfficialStationName.equals("obla1") || 
                   lcOfficialStationName.equals("ocim2") || 
                   lcOfficialStationName.equals("ocpn7") || 
                   lcOfficialStationName.equals("omhc1") || 
                   lcOfficialStationName.equals("ovia2") || 
                   lcOfficialStationName.equals("pnlm6") || 
                   lcOfficialStationName.equals("ulam6") || 
                   //lcOfficialStationName.equals("") || 
                   //lcOfficialStationName.equals("") || 
                   //lcOfficialStationName.equals("") || 
                   //lcOfficialStationName.equals("") || 
                   false) {
            stationType = "buoy (unknown type)";
            ownerLine--; */
        } else stationType = XML.removeHTMLTags(lines[ownerLine + 1]).trim();
        if (verbose) String2.log(//"  stationTypeLine=" + lines[ownerLine + 1] + "\n" +
                                 "  stationType=" + stationType);

        //get the payload  (some files don't have this line)
        String payload = XML.removeHTMLTags(lines[ownerLine + 2]).trim();
        if (payload.equals("LCB"))
            payload = "LCB payload";
        if (payload.toLowerCase().endsWith(" payload")) {
            payload = payload.substring(0, payload.length() - 8);
        } else {
            if (verbose)
                String2.log("nonfatal " + errorInMethod + "'payload' not found:\n" + 
                    "line-1=" + lines[ownerLine+1] + 
                  "\nline  =" + lines[ownerLine+2] + 
                  "\nline+1=" + lines[ownerLine+3]);
            payload = "";
            ownerLine--; //that line isn't in the file, so adjust for subsequent info
        }
        if (verbose) String2.log(//"  payloadLine=" + lines[ownerLine + 2] + "\n" +
                                 "  payload=" + payload);

        //get the lat and lon
        //text in the form"<b>9.9 S 105.2 W ("
        //   or sometimes "<b>18.0 S 85.1 W</b><br>"
        double lat = Double.NaN;
        double lon = Double.NaN;
        String location = lines[ownerLine + 3];
        if (stationName.equals("46108"))        { lat = 59.760; lon = -152.090; 
        } else if (stationName.equals("44089")) { lat = 37.756; lon =  -75.334;
        } else if (stationName.equals("44090")) { lat = 41.840; lon =  -70.329; 
        } else if (stationName.equals("44091")) { lat = 39.769; lon =  -73.770; 
        } else if (stationName.equals("45028")) { lat = 46.810; lon =  -91.840; 
        } else if (stationName.equals("46252")) { lat = 33.953; lon = -119.257; 
        } else if (stationName.equals("46259")) { lat = 34.732; lon = -121.664; 
        } else if (stationName.equals("ocpn7")) { lat = 33.908; lon =  -78.148; 
        } else if (stationName.equals("pxoc1")) { lat = 37.798; lon = -122.393; 
        } else if (stationName.equals("ssbn7")) { lat = 33.848; lon =  -78.482; 
        } else {
            //look on web page
            int parenPo = location.indexOf('(');
            if (parenPo > 0) 
                location = location.substring(0, parenPo);
            location = XML.removeHTMLTags(location).trim();
            String latString = String2.extractRegex(location, "[-]?[0-9]+\\.[0-9]+", 0);
            lat = String2.parseDouble(latString); //better to start as double, reduce to float if needed
            Test.ensureNotEqual(lat, Double.NaN, 
                errorInMethod + "lat('" + latString + "')=NaN.  location=" + location + "\n" +
                "line-1=" + lines[ownerLine+2] + 
              "\nline  =" + lines[ownerLine+3] + 
              "\nline+1=" + lines[ownerLine+4]);
            int snPo = location.indexOf("N");
            if (snPo < 0) {
                snPo = location.indexOf("S");
                lat *= -1;
            }
            String lonString = String2.extractRegex(location, "[-]?[0-9]+\\.[0-9]+", snPo + 1);
            lon = String2.parseDouble(lonString);
            if (location.indexOf("W") >= 0)
                lon *= -1;
            Test.ensureNotEqual(lon, Double.NaN, errorInMethod + "lon is null.  location=" + location);
        }
        if (verbose) String2.log(//"  locationLine=" + lines[ownerLine + 3] + "\n" +
                                 "  location='" + location + "' lon=" + lon + " lat=" + lat);

        //get the station elevationInfo (ok if not found):
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
        }

        //make the Table variable that will hold everything 
        Table cumulative = null;
        
        //append the data from all the historicalFiles
        int nDuplicates;
        for (int file = 0; file < historicalFiles.length; file++) {
            if (historicalFiles[file].equals("4202262008.txt") ||
                historicalFiles[file].equals("4601562008.txt") ||
                historicalFiles[file].equals("4609162008.txt") ||
                historicalFiles[file].equals("bhrc362008.txt") ||
                historicalFiles[file].equals("clkn762008.txt") ||
                historicalFiles[file].equals("hmra262008.txt") ||
                historicalFiles[file].equals("maxt262008.txt") ||
                historicalFiles[file].equals("pmaf162008.txt") ||
                historicalFiles[file].equals("rprn662008.txt") ||
                historicalFiles[file].equals("rtyc162008.txt") ||
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

            if (verbose) String2.log("  reading " + historicalFiles[file]);
            Table tTable = readStationTxtFile(ndbcHistoricalTxtDir + historicalFiles[file], 
                officialStationName, (float)lon, (float)lat);

            //look for duplicates within a file; I shouldn't have to do this
            tTable.sort(new int[]{timeIndex}, new boolean[]{true});
            nDuplicates = tTable.removeDuplicates();
            if (nDuplicates > 0)
                String2.log("DUPLICATE ROWS: " + nDuplicates + " duplicates found in " + historicalFiles[file]);

            if (cumulative == null)
                cumulative = tTable;
            else cumulative.append(tTable); //this handles upgrading cols with simpler data types
            //if (verbose) String2.log("time0=" + cumulative.getDoubleData(timeIndex, 0));
        }

        //sort, since file names not in time-ascending order
        cumulative.sort(new int[]{timeIndex}, new boolean[]{true});

        //removeDuplicates; I shouldn't have to do this
        nDuplicates = cumulative.removeDuplicates();
        if (nDuplicates > 0)
            String2.log("DUPLICATE ROWS: " + nDuplicates + " duplicates found between historic files for " + stationName);

        //*** add 45 day Real Time data
        String realTimeFileName = ndbc45DayTxtDir + 
                stationName.toUpperCase() + ".txt";
        double lastHistoricalTime = cumulative.getDoubleData(timeIndex, cumulative.nRows() - 1);
        boolean hasRealTimeData = File2.isFile(realTimeFileName);
        if (hasRealTimeData) {
            //read 45 day real time data (if any)
            if (verbose) String2.log("  reading 45 day realTime file");
            Table realTime = readStationTxtFile(realTimeFileName, officialStationName, 
                (float)lon, (float)lat);

            //sort it
            realTime.sort(new int[]{timeIndex}, new boolean[]{true});

            //remove all rows from beginning with <= lastHistoricalTime
            PrimitiveArray realTimeTime = realTime.getColumn(timeIndex);
            if (verbose) String2.log(
                "    lastHistoricalTime=" + Calendar2.epochSecondsToIsoStringT(lastHistoricalTime) +
              "\n    first realTimeTime=" + Calendar2.epochSecondsToIsoStringT(realTimeTime.getDouble(0))); 
            int firstToKeep = 0;
            int realTimeTimeN = realTimeTime.size();
            while (firstToKeep < realTimeTimeN &&
                realTimeTime.getDouble(firstToKeep) <= lastHistoricalTime)
                firstToKeep++;
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
            if (verbose) String2.log("    nRowsRemoved=" + firstToKeep + " nRemain=" + realTime.nRows());

            //append   (this handles if cumulative col is simpler than realTime col)
            cumulative.append(realTime);
            //no metadata yet, so no need to merge it
        } else {
            if (verbose) String2.log("  NO REALTIME FILE.");
        } 
        //at this point, cumulative is sorted by time; and all times rounded to nearest hour

        //make newData columns which have data regularly spaced on-the-hour
        int oldNRows = cumulative.nRows();
        int nColumns = cumulative.nColumns(); 
        PrimitiveArray newData[] = new PrimitiveArray[nColumns];
        for (int col = 0; col < nColumns; col++) 
            newData[col] = PrimitiveArray.factory(
                cumulative.getColumn(col).elementClass(), 
                    oldNRows + 2500, false); //exact number not critical here
        long lastSeconds = -1;
        //go through cumulative data rows, adding rows evenly spaced in time to newData
        for (int oldRow = 0; oldRow < oldNRows; oldRow++) {

            //if time goes backwards (or stays same), merge this row with previous row
            long seconds = Math2.roundToLong(cumulative.getDoubleData(timeIndex, oldRow));
            Test.ensureTrue(seconds != Long.MAX_VALUE, "seconds=" + cumulative.getStringData(timeIndex, oldRow));
            Test.ensureTrue(seconds >= lastSeconds, "seconds < lastSeconds");
            if (seconds == lastSeconds) {
                //replace mv's in previous row with real data
                int tNewDataRows = newData[0].size();
                for (int col = idIndex + 1; col < nColumns; col++) {
                    if (Double.isNaN(newData[col].getDouble(tNewDataRows - 1)))
                        newData[col].setDouble(tNewDataRows - 1, 
                            cumulative.getColumn(col).getDouble(oldRow));
                }
                continue;
            }

            //insert mv rows
            if (lastSeconds == -1)
                lastSeconds = seconds - Calendar2.SECONDS_PER_HOUR;
            int nToInsert = (int)((seconds - lastSeconds) / Calendar2.SECONDS_PER_HOUR) - 1; //safe
            if (nToInsert > 1000) 
                if (verbose) String2.log("nToInsert=" + nToInsert + " lastSeconds=" +
                    Calendar2.epochSecondsToIsoStringT(lastSeconds) +
                    " seconds=" + Calendar2.epochSecondsToIsoStringT(seconds));
            for (int i = 0; i < nToInsert; i++) {
                int tNRows = newData[0].size();

                //add x,y,z from previous row
                for (int col = lonIndex; col <= depthIndex; col++)
                    newData[col].addDouble(newData[col].getDouble(tNRows - 1)); 

                //add id from previous row
                newData[idIndex].addString(newData[idIndex].getString(tNRows - 1)); 

                //add incremented time
                lastSeconds += Calendar2.SECONDS_PER_HOUR; 
                newData[timeIndex].addDouble(lastSeconds); 

                //add data mv's
                for (int col = timeIndex + 1; col < nColumns; col++) 
                    newData[col].addDouble(Double.NaN); 
            }
            lastSeconds = seconds;

            //copy row of data from cumulative to newData
            for (int col = 0; col < nColumns; col++) {
                if (col == idIndex) newData[col].addString(cumulative.getStringData(col, oldRow));
                else                newData[col].addDouble(cumulative.getDoubleData(col, oldRow));
            }

        }
        //ensure time increments in a regular way
        int nNewRows = newData[timeIndex].size();
        for (int i = 1; i < nNewRows; i++) {
            Test.ensureEqual(newData[timeIndex].getDouble(i) - newData[timeIndex].getDouble(i - 1),
                Calendar2.SECONDS_PER_HOUR, "time increment incorrect at i=" + i);
        }
        //String2.log("2 save station .nc maxLength=" + ((StringArray)cumulative.getColumn(0)).maxStringLength());

        //copy newData into cumulative (replacing the old data)
        for (int col = 0; col < nColumns; col++) 
            cumulative.setColumn(col, newData[col]);        

        //last thing (so missing_value data type is correct): addMetadata
        String stdStationName = stationName.toUpperCase();
        addMetadata(cumulative, stdStationName, 
            //stationUrl, owner, locationName, stationType, 
            lon, lat);
            //lastHistoricalTime,
            //payload, elevationInfo.toString(), false);  //true=data is scaled
 
        //save as UPPERCASE-name .nc file
        String id0 = cumulative.getColumn(idIndex).getString(0);
        //String2.log("\nbefore save\n" + cumulative.toString("row", 5));
        Test.ensureEqual(id0.length(), 5, "ID length should be 5: " + id0);
        Test.ensureTrue(((StringArray)cumulative.getColumn(idIndex)).maxStringLength() <= 5, "ID maxlength should be <= 5");
        cumulative.saveAs4DNcWithStringVariable(ndbcNcDir + "NDBC_" + stdStationName + "_met.nc", 
            lonIndex, latIndex, depthIndex, timeIndex, 4);

        //for diagnostics only: validate that times values are as expected (this is modified since last used)
        //int tNRows = cumulative.nRows();
        //DoubleArray timeArray = cumulative.getColumn(timeIndex);
        //double dataStartSeconds = Calendar2.isoStringToEpochSeconds(
        //    cumulative.getGlobalAttribute("time_coverage_start").getDouble(0));
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

        //do some tests on the files
        if (stdStationName.equals("31201")) test31201Nc(ndbcNcDir);
        if (stdStationName.equals("41009")) test41009Nc(ndbcNcDir);
        if (stdStationName.equals("41015")) test41015Nc(ndbcNcDir);
        if (stdStationName.equals("46088")) test46088Nc(ndbcNcDir);
        if (stdStationName.equals("TAML1")) testTAML1Nc(ndbcNcDir);
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
     * @param ndbcNcDir the directory for the nc files to be created in
     * @param testMode if true, just a few files are done (for test purposes)
     * @param ignoreStationsBefore is the first station to be processed (use " " for all)
     * @throws Exception if trouble
     */
    public static void makeSeparateNcFiles(String ndbcStationHtmlDir,
        String ndbcHistoricalTxtDir,
        String ndbc45DayTxtDir, String ndbcNcDir, String ignoreStationsBefore, 
        boolean testMode) throws Exception {

        String2.log("\n*** makeSeparateNcFiles...");

        //if starting from start, delete all the nc files in the ndbcNcDir
        String[] files;
        if (ignoreStationsBefore.equals(" ")) {
            files = RegexFilenameFilter.list(ndbcNcDir, ".*\\.nc");
            for (int i = 0; i < files.length; i++) {
                File2.delete(ndbcNcDir + files[i]);
                //make sure they are deleted
                Test.ensureEqual(File2.isFile(ndbcNcDir + files[i]), false, 
                    String2.ERROR + " in NdbcMetStation.makeSeparateNcFiles:\n" +
                    "Unable to delete " + ndbcNcDir + files[i]);
            }
        }

        //get all the file names
        String2.log("makeSeparateNcFiles   getting list of historicalTxt files...");
        files = RegexFilenameFilter.list(ndbcHistoricalTxtDir, ".*\\.txt");
        if (verbose) String2.log("sorting...");
        Arrays.sort(files);

        //go through the station names
        int stationNameStart = 0;    
        String stationName = files[stationNameStart].substring(0, 5);
        for (int i = stationNameStart + 1; i <= files.length; i++) {  //yes, start at +1, end at files.length
            //new station name?
            if (i == files.length || !files[i].substring(0, 5).equals(stationName)) {

                //make the .nc file
                if (verbose) String2.log("\nmakeStationNcFile for #" + i + " " + stationName + 
                    " start=" + stationNameStart);
                String tempNames[] = new String[i - stationNameStart];
                System.arraycopy(files, stationNameStart, tempNames, 0, i - stationNameStart);
                String lcStationName = stationName.toLowerCase();
                if (

                    lcStationName.compareTo(ignoreStationsBefore) < 0 ||  
                    //or temporary for 1 station:
                    //!lcStationName.equals(ignoreStationsBefore) || 

                    //this following lines are standard stations to avoid 
                    //unless otherwise marked: no station info  (checked 2012-02-26)
                    //e.g., http://www.ndbc.noaa.gov/station_page.php?station=4h362                    
                    lcStationName.equals("32st1") || 
                    lcStationName.equals("32st2") || 
                    lcStationName.equals("41nt1") || 
                    lcStationName.equals("41nt2") || 
                    lcStationName.equals("42008") || //all yearly files are empty
                    lcStationName.equals("42a01") || 
                    lcStationName.equals("42a02") ||
                    lcStationName.equals("42a03") ||
                    lcStationName.equals("46a35") ||
                    lcStationName.equals("46a54") ||
                    lcStationName.equals("47072") ||
                    lcStationName.equals("4f369") ||
                    lcStationName.equals("4f370") ||
                    lcStationName.equals("4f374") ||
                    lcStationName.equals("4f375") ||
                    lcStationName.equals("4f376") ||
                    lcStationName.equals("4f392") ||
                    lcStationName.equals("4f887") ||
                    lcStationName.equals("4h361") ||
                    lcStationName.equals("4h362") ||
                    lcStationName.equals("4h363") ||
                    lcStationName.equals("4h364") ||
                    lcStationName.equals("4h365") ||
                    lcStationName.equals("4h390") ||
                    lcStationName.equals("4h394") ||
                    lcStationName.equals("4h902") ||
                    lcStationName.equals("51wh1") || 
                    lcStationName.equals("51wh2") ||
                    lcStationName.equals("misma") ||
                    lcStationName.equals("plsfa") ||
                    lcStationName.equals("mnmm4")) { //bad info in some files
                    //no station information, so no lat lon, so skip these
                } else if (testMode &&   
                    !lcStationName.equals("31201") &&    //I have detailed tests for these
                    !lcStationName.equals("41009") &&
                    !lcStationName.equals("41015") &&
                    !lcStationName.equals("46012") &&   //4601x are off west coast of US
                    !lcStationName.equals("46013") &&
                    !lcStationName.equals("46014") &&
                    !lcStationName.equals("46015") &&
                    !lcStationName.equals("46088") &&
                    !lcStationName.equals("taml1")) {  
                    //ignore it
                } else makeStationNcFile(ndbcStationHtmlDir, ndbcHistoricalTxtDir, 
                    ndbc45DayTxtDir, ndbcNcDir, tempNames);

                //start the next stationName
                stationNameStart = i;
                if (i < files.length)
                    stationName = files[i].substring(0, 5);
            }
        }

        String2.log("makeSeparateNcFiles finished successfully.");
    }

    /**
     * Download all the .txt Historical files from NDBC
     * that aren't already on this computer to the ndbcHistoricalTxtDir.
     * Yearly files are from: http://www.ndbc.noaa.gov/data/historical/stdmet/
     * Monthly files are from: http://www.ndbc.noaa.gov/data/stdmet/<month3Letter>/  e.g., Jan
     *
     * <p>!!!!**** Windows GUI My Computer doesn't show all the files in the directory! 
     * Use DOS window "dir" or Linux ls instead of the GUI.
     *
     * @param ndbcHistoricalTxtDir the directory for the historical .txt files
     * @throws Exception if trouble
     */
    public static void downloadNewHistoricalTxtFiles(String ndbcHistoricalTxtDir) throws Exception {

        //the current year
        String year = HISTORICAL_FILES_CURRENT_YEAR;      

        String2.log("\n*** downloadNewHistoricalTxtFiles...");

        //get the names of the available YEAR standard meteorological files
        //search for e.g., "<a href="venf1h1992.txt.gz">" 
        //  and keep                "venf1h1992.txt"
        if (true) {
            String ndbcDirectoryUrl = "http://www.ndbc.noaa.gov/data/historical/stdmet/";
            String[] lines = SSR.getUrlResponse(ndbcDirectoryUrl);
            StringArray fileNames = new StringArray();
            for (int i = 0; i < lines.length; i++) {
                String extract = String2.extractRegex(lines[i], 
                    "<a href=\".{4,5}h\\d{4}\\.txt\\.gz\">", 0); //some stations (EBxx) have 4 character IDs
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
                            ndbcHistoricalTxtDir + destName, true); //true = use compression
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
            String ndbcDirectoryUrl = "http://www.ndbc.noaa.gov/data/stdmet/" + 
                Calendar2.getMonthName3(month) + "/";
            String[] lines = SSR.getUrlResponse(ndbcDirectoryUrl);
            StringArray fileNames = new StringArray();
            for (int i = 0; i < lines.length; i++) {
                String extract = String2.extractRegex(lines[i], 
                    //in 2006, Jan, July, Aug, Sep names are e.g., 4100112006.txt.gz 
                    // and others are just <5charID>.txt
                    //all current stations have 5 character ID
                    //this regex works for either type of file name  
                    "<a href=\".{5}(.{5}|)\\.txt(\\.gz|)", 0); 

                //If it has the year, is it the wrong year?
                if (extract != null &&
                    extract.matches("<a href=\".{10}\\.txt(\\.gz|)") &&
                    !extract.substring(15, 19).equals("" + year))
                    extract = null;
                if (extract != null)
                    fileNames.add(extract.substring(9));
            }
            String2.log("\n" + fileNames.size() + " files found in " + ndbcDirectoryUrl);

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
                        //String2.pressEnterToContinue();
                        continue;
                    }
                }
            }
        }

        String2.log("downloadNewHistoricalTxtFiles finished successfully.");
    }

    /**
     * Download all the 45 day near-real-time .txt data files from
     * http://www.ndbc.noaa.gov/data/realtime2/ to the ndbc45DayTxtDir,
     * whether they already exist in the local directory or not.
     *
     * @param ndbc45DayTxtDir the directory for the 45 day .txt files
     * @throws Exception if trouble
     */
    public static void download45DayTxtFiles(String ndbc45DayTxtDir) throws Exception {
        String ndbcDirectoryUrl = "http://www.ndbc.noaa.gov/data/realtime2/";

        String2.log("\n*** download45DayTxtFiles...");

        //get the names of the available real time standard meteorological files
        //search for e.g., "<a href="42OTP.txt">"
        String[] lines = SSR.getUrlResponse(ndbcDirectoryUrl);
        StringArray stationNames = new StringArray();
        for (int i = 0; i < lines.length; i++) {
            String extract = String2.extractRegex(lines[i], "<a href=\".{5}\\.txt\">", 0); //all current stations have 5 character ID
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
                    //e.g. http://www.ndbc.noaa.gov/data/realtime2/42362.txt
                    ndbcDirectoryUrl + stationName + ".txt",
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
        table.read4DNc(fullFileName, null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking
        String2.log(fullFileName + "=" + table.toString("row", showFirstNRows));
    }


    /**
     * These tests a time primitiveArray to make sure the values 
     * increment in 1 hour increments.
     *
     * @param stationID which is just used to identify the station in the error message
     * @param timePA
     * @param requireAllValid If true, all values must be valid.
     *    If false, only the values until the first NaN must be valid.
     * @throws Exception if they don't increment correctly
     */
    public static void testTime(String stationID, PrimitiveArray timePA,
        boolean requireAllValid) {

        //ensure time increments in a regular way
        //PrimitiveArray timeColumn = table.getColumn(timeIndex);
        int nRows = timePA.size();
        double time = timePA.getDouble(0);
        double oTime;
        for (int i = 1; i < nRows; i++) {
            oTime = time;
            time = timePA.getDouble(i);
            if (!requireAllValid && Double.isNaN(time))
                return;
            if (time - oTime != Calendar2.SECONDS_PER_HOUR)
                //do pre-test above to avoid pre-generating the very long error strings
                Test.ensureEqual(time - oTime, Calendar2.SECONDS_PER_HOUR, 
                    String2.ERROR + " in NdbcMetStation.testTime:\n" + 
                    "time increment incorrect at i=" + i + " station=" + stationID + 
                    "\n oTime=" + Calendar2.epochSecondsToIsoStringT(oTime) +
                    " time=" + Calendar2.epochSecondsToIsoStringT(time) +
                    "\n" + timePA.toString());
        }
    }

    /**
     * This reads the 31201.nc file and makes sure it has the right info.
     */
    public static void test31201Nc(String ndbcNcDir) throws Exception {
        String2.log("\ndoing test31201Nc");
        //String2.log(DataHelper.ncDumpString(ndbcNcDir + "31201.nc", false));

        Table table = new Table();
        table.read4DNc(ndbcNcDir + "NDBC_31201_met.nc", null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking
        test31201(table);
        testTime("31201", table.getColumn(timeIndex), true); //true=requireAllValid
    }

    /**
     * This tests the 31201 info in the table.
     */
    public static void test31201(Table table) throws Exception {
        String2.log("\ndoing test31201  nRows=" + table.nRows());
        //String2.log(table.toString("obs", 2));
        //Note that VIS converted from statute miles to km:  old*kmPerMile;
        //and TIDE converted from feet to meters: old*Math2.meterPerFoot

        //test global attributes
        Test.ensureEqual(table.globalAttributes().getString("creator_name"), "NOAA CoastWatch, West Coast Node", "");
        Test.ensureEqual(table.globalAttributes().getString("creator_url"), "http://coastwatch.pfeg.noaa.gov", "");

        //test variable attributes
        int nCols = table.nColumns();
        for (int col = 0; col < nCols; col++) {
            Test.ensureEqual(table.columnAttributes(col).getString("long_name"), longNames[col], "");
            Test.ensureEqual(table.columnAttributes(col).getString("units"),     udUnits[col],   "");
        }
 
        //first available line from 31201h2005.txt
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 
        double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), -27.705f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -48.134f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), 1.4f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex,  row), 9.00f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 24.4f, "");

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        seconds = Calendar2.isoStringToEpochSeconds("2005-04-25T18");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), -27.705f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -48.134f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), 3.9f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex,  row), 8.00f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 23.9f, "");

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 08 19 15 00 999 99.0 99.0  1.50  8.00 99.00 999 9999.0 999.0  18.2 999.0 99.0 99.00 last available
        seconds = Calendar2.isoStringToEpochSeconds("2005-08-19T15");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), -27.705f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -48.134f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), 1.5f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex,  row), 8.00f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 18.2f, "");
            
        String2.log("test31201 was successful");

    }

    /**
     * This reads the 41009.nc file and makes sure it has the right info.
     */
    public static void test41009Nc(String ndbcNcDir) throws Exception {
        String2.log("\ndoing test41009Nc");
        //String2.log(DataHelper.ncDumpString(ndbcNcDir + "41009.nc", false));

        Table table = new Table();
        table.read4DNc(ndbcNcDir + "NDBC_41009_met.nc", null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking
        test41009(table);
        testTime("41009", table.getColumn(timeIndex), true); //true=requireAllValid
    }

    /**
     * This tests the 41009 info in the table.
     */
    public static void test41009(Table table) throws Exception {
        String2.log("\ndoing test41009  nRows=" + table.nRows());
        //String2.log(table.toString("obs", 2));

        //test columnNames and Types
        int nCols = table.nColumns();
        for (int col = 0; col < nCols; col++) {
            Test.ensureEqual(table.getColumnName(col), metColumnNames[col], "col=" + col);
            PrimitiveArray pa = table.getColumn(col);
            String msg = "colName=" + table.getColumnName(col) + " type=" + pa.elementClass().toString();
            if (col == timeIndex) {
                Test.ensureTrue(pa instanceof DoubleArray, msg);
            } else if (col == idIndex) {
                Test.ensureTrue(pa instanceof StringArray, msg);
            } else if (col == wdIndex || col == mwdIndex) {
                Test.ensureTrue(pa instanceof ShortArray, msg);
                PrimitiveArray range = table.columnAttributes(col).get("actual_range");
                Test.ensureEqual(range.getInt(0),   0, "min not 0.");
                Test.ensureEqual(range.getInt(1), 359, "max not 359.");
            } else { 
                Test.ensureTrue(pa instanceof FloatArray, msg);
            }
        }

    }

    /**
     * This reads the 41015.nc file and makes sure it has the right info.
     */
    public static void test41015Nc(String ndbcNcDir) throws Exception {
        String2.log("\ndoing test41015Nc");
        Table table = new Table();
        table.read4DNc(ndbcNcDir + "NDBC_41015_met.nc", null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking
        test41015(table);
        testTime("41015", table.getColumn(timeIndex), true); //true=requireAllValid
    }

    /**
     * This tests the 41015 info in the table.
     */
    public static void test41015(Table table) throws Exception {
        String2.log("\ndoing test41015  nRows=" + table.nRows());
        //String2.log(table.toString("obs", 3));
        //Note that VIS converted from statute miles to km:  old*kmPerMile;
        //and TIDE converted from feet to meters: old*Math2.meterPerFoot

        //test global attributes

        //test variable attributes
        int nCols = table.nColumns();
        for (int col = 0; col < nCols; col++) {
            Test.ensureEqual(table.columnAttributes(col).getString("long_name"), longNames[col], "");
            Test.ensureEqual(table.columnAttributes(col).getString("units"),     udUnits[col],     "");
        }

        //row of data from 41015h1993.txt
        //YY MM DD hh WD   WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS
        //93 05 23 18 303 00.1 00.6 99.00 99.00 99.00 999 1021.1  19.9  18.4 999.0 99.0  //first available
        double seconds = Calendar2.isoStringToEpochSeconds("1993-05-23T18");
        int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "41015", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 35.4f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -75.3f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 303, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), .1f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), .6f, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1021.1f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 19.9f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 18.4f, "");
        Test.ensureEqual(table.getDoubleData(dewpIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");

        //YY MM DD hh WD   WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS
        //93 05 24 11 194 02.5 02.8 00.70 04.20 04.90 185 1021.2  17.3  16.4 999.0 18.5
        seconds = Calendar2.isoStringToEpochSeconds("1993-05-24T11");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "41015", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 35.4f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -75.3f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 194, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 2.5f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 2.8f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), .7f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), 4.2f, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), 4.9f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), 185f, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1021.2f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 17.3f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 16.4f, "");
        Test.ensureEqual(table.getDoubleData(dewpIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), (float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(wspuIndex, row),  0.6f, ""); //.604 calc by hand
        Test.ensureEqual(table.getFloatData(wspvIndex, row), 2.4f, ""); //2.4257 calc by hand

        //YY MM DD hh WD   WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS
        //93 07 24 17 205 02.8 03.5 00.90 09.10 04.70 109 1019.0  28.1  27.3 999.0 10.9  //last avail 
        seconds = Calendar2.isoStringToEpochSeconds("1993-07-24T17");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "41015", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 35.4f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -75.3f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 205, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 2.8f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 3.5f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), .9f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), 9.1f, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), 4.7f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), 109f, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1019f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 28.1f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 27.3f, "");
        Test.ensureEqual(table.getDoubleData(dewpIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), (float)Math2.roundTo(10.9 * Math2.kmPerMile, decimalDigits[visIndex]), "");

        String2.log("test41015 was successful");
    }

    /**
     * This reads the 46088.nc file and makes sure it has the right info.
     */
    public static void test46088Nc(String ndbcNcDir) throws Exception {
        String2.log("\ndoing test46088Nc");
        //String2.log(DataHelper.ncDumpString(ndbcNcDir + "46088.nc", false));

        Table table = new Table();
        table.read4DNc(ndbcNcDir + "NDBC_46088_met.nc", null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking
        test46088(table);
        testTime("46088", table.getColumn(timeIndex), true); //true=requireAllValid
    }

    /**
     * This tests the 46088 info in the table.
     */
    public static void test46088(Table table) throws Exception {
        String2.log("\ndoing test46088  nRows=" + table.nRows());
        //String2.log(table.toString("obs", 3));

        //Note that VIS converted from statute miles to km:  old*kmPerMile;
        //and TIDE converted from feet to meters: old*Math2.meterPerFoot
        //String2.log(table.toString(5));

        //first row, first year of historical data from 46088h2004.txt
        //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2004 07 01 22 228  5.7  6.5  0.44  5.00  3.46 270 9999.0 999.0 999.0  11.0 99.0 99.00
        double seconds = Calendar2.isoStringToEpochSeconds("2004-07-01T22");
        int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 228, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 5.7f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 6.5f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), .44f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), 5f, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), 3.46f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), 270f, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 11.0f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), ""); 
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");


        //last row, first year of historical data from 46088h2004.txt
        //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2004 12 31 23 354  7.1  8.5  0.50  3.13  2.88 332 1000.5   5.7   8.6   1.7 99.0 99.00
        seconds = Calendar2.isoStringToEpochSeconds("2004-12-31T23");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 354, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 7.1f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 8.5f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), .5f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), 3.13f, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), 2.88f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), 332f, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1000.5f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 5.7f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 8.6f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 1.7f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //first row, last year of historical data from 46088h2005.txt
        //YYYY MM DD hh mm  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2005 01 01 00 00 359  6.4  7.4  0.42  3.13  2.87 331 1000.4   6.1   8.6   1.8 99.0 99.00
        seconds = Calendar2.isoStringToEpochSeconds("2005-01-01T00");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 359, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 6.4f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 7.4f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), .42f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), 3.13f, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), 2.87f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), 331f, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1000.4f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 6.1f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 8.6f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 1.8f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //last row, last year of historical data from 46088h2005.txt
        //YYYY MM DD hh mm  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2005 12 31 23 30  11  1.9  2.2 99.00 99.00 99.00 999  987.7   8.5   8.6   7.4 99.0 99.00
        seconds = Calendar2.isoStringToEpochSeconds("2006-01-01T00"); //30 minutes rounds to next hour
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 11, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 1.9f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 2.2f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 987.7f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 8.5f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 8.6f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 7.4f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //first row, first monthly data from 4608812006.txt
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2006 01 01 00 30 272  1.3  1.7 99.00 99.00 99.00 999  989.0   8.5   8.6   7.4 99.0 99.00
        seconds = Calendar2.isoStringToEpochSeconds("2006-01-01T01"); //30 min rounds to next hour
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 272, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 1.3f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 1.7f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 989f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 8.5f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 8.6f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 7.4f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //last row, first monthly data from 4608812006.txt
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2006 01 31 23 30 135 19.2 23.6 99.00 99.00 99.00 999  994.4   5.9   8.6   3.3 99.0 99.00
        seconds = Calendar2.isoStringToEpochSeconds("2006-02-01T00"); //30 min rounds to next hour
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 135, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 19.2f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 23.6f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 994.4f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 5.9f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 8.6f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 3.3f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //UPDATE_EACH_MONTH
        //first post-historical rows from 45day file 46088.txt    
        //  http://www.ndbc.noaa.gov/data/realtime2/46088.txt    //45 day   //top line has precedence
        //#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
        //#yr  mo dy hr mn degT m/s  m/s     m   sec   sec degT   hPa  degC  degC  degC   mi  hPa    ft
        //2016 09 01 01 20 170  3.0  3.0   0.1    MM   3.9  MM 1013.5  13.0  11.0  12.3   MM   MM    MM
        //2016 09 01 00 50 160  3.0  3.0   0.1    MM   4.1  MM 1013.6  13.4  11.0  12.5   MM -0.0    MM
        seconds = Calendar2.isoStringToEpochSeconds("2016-09-01T01"); //50 min rounds to next hour; usually test 01T01
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 170, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 3f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 3f, "");    
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), .1f, ""); 
        Test.ensureEqual(table.getFloatData(dpdIndex, row), Float.NaN, ""); 
        Test.ensureEqual(table.getFloatData(apdIndex, row), 3.9f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), Float.NaN, "");  
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1013.5f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 13f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 11f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 12.3f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(ptdyIndex, row), 0f, "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        String2.log("test46088 was successful");
    }

    /**
     * This reads the 46088.nc file and makes sure it was updated correctly
     * by addLastNDays.
     */
    public static void test46088AddLastNDaysNc(String ndbcNcDir) throws Exception {
        String2.log("\ndoing test46088AddLastNDaysNc");
        Table table = new Table();
        table.read4DNc(ndbcNcDir + "NDBC_46088_met.nc", null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking

        test46088AddLastNDays(table);
        testTime("46088AddLastNCDaysNc", table.getColumn(timeIndex), true); //true=requireAllValid
    }

    /**
     * This reads the 46088.nc file and makes sure it was updated correctly
     * by addLastNDays.
     */
    public static void test46088AddLastNDays(Table table) throws Exception {

        //Note that VIS converted from statute miles to km:  old*kmPerMile;
        //and TIDE converted from feet to meters: old*Math2.meterPerFoot

        //!!!***SPECIAL UPDATE EACH MONTH --after separateFiles made (later dated record has priority - encountered first)
        //tests are from (downloaded by hand) 
        //  http://www.ndbc.noaa.gov/data/realtime2/46088.txt    //45 day   //upper line has precedence
        //a time point in the 5 day file AFTER the last 45 day time
        //top row has precedence, but not if file already had lower row of data
        //#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
        //#yr  mo dy hr mn degT m/s  m/s     m   sec   sec degT   hPa  degC  degC  degC   mi  hPa    ft
        //2016 09 27 14 20 210  2.0  3.0   0.2    MM   3.8  MM 1021.4  11.0  10.6  10.9   MM   MM    MM
        //2016 09 27 13 50 200  2.0  3.0   0.2    MM   3.7  MM 1021.2  11.0  10.6  10.8   MM +1.3    MM
        double seconds = Calendar2.isoStringToEpochSeconds("2016-09-27T14"); //rounded
        int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureTrue(row >= 0, "row=" + row);
        Test.ensureEqual(table.getStringData(idIndex, row), "46088", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 48.333f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -123.167f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getFloatData(wdIndex, row), 210, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 2f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 3f, "");
        Test.ensureEqual(table.getFloatData(wvhtIndex, row), 0.2f, "");
        Test.ensureEqual(table.getFloatData(dpdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(apdIndex, row), 3.8f, "");
        Test.ensureEqual(table.getFloatData(mwdIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1021.4f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 11f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 10.6f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 10.9f, "");
        Test.ensureEqual(table.getFloatData(visIndex, row), Float.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(ptdyIndex, row), 1.3f, "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, "");//(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        String2.log("test46088AddLastNDays was successful");
    }

    /**
     * This reads the taml1.nc file and makes sure it has the right info.
     */
    public static void testTAML1Nc(String ndbcNcDir) throws Exception {
        String2.log("\ndoing testTAML1Nc");
        Table table = new Table();
        table.read4DNc(ndbcNcDir + "NDBC_TAML1_met.nc", null, 0, ID_NAME, 4); //0 to force looking at what is actually there, without unpacking
        testTAML1(table);
        testTime("TAML1", table.getColumn(timeIndex), true); //true=requireAllValid
    }


    /**
     * This tests the TAML1 info in the table.
     */
    public static void testTAML1(Table table) throws Exception {
        String2.log("\ndoing testTAML1  nRows=" + table.nRows());
        //Note that VIS converted from statute miles to km:  old*kmPerMile;
        //and TIDE converted from feet to meters: old*Math2.meterPerFoot

        //row of data from taml1h2004.txt   //first year
        //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2004 01 01 01  70  4.6 99.0 99.00 99.00 99.00 999 1026.3  15.2  13.7  13.4 99.0  0.62  //first avail
        double seconds = Calendar2.isoStringToEpochSeconds("2004-01-01T01");
        int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 70, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 4.6f, "");
        Test.ensureEqual(table.getDoubleData(gstIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1026.3f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 15.2f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row),13.7f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 13.4f, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), (float)Math2.roundTo(0.62 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //row of data from taml1h2004.txt
        //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2004 01 01 08 100  3.6 99.0 99.00 99.00 99.00 999 1025.5  15.1  13.8  14.3 99.0  0.06
        seconds = Calendar2.isoStringToEpochSeconds("2004-01-01T08");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 100, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 3.6f, "");
        Test.ensureEqual(table.getDoubleData(gstIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1025.5f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 15.1f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row),13.8f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 14.3f, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), (float)Math2.roundTo(0.06 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");
        Test.ensureEqual(table.getFloatData(wspuIndex, row), -3.5f, ""); //-3.545307911 calc by hand
        Test.ensureEqual(table.getFloatData(wspvIndex, row), .6f, ""); //.625133439 calc by hand

        //row of data from taml1h2004.txt
        //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2004 12 31 23  90  5.1  6.2 99.00 99.00 99.00 999 1022.9  16.3  13.7  15.2 99.0 -0.04  //last row of first year
        seconds = Calendar2.isoStringToEpochSeconds("2004-12-31T23");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 90, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 5.1f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 6.2f, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1022.9f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 16.3f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row),13.7f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 15.2f, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), (float)Math2.roundTo(-0.04 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //row of data from taml1h2005.txt
        //YYYY MM DD hh mm  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2005 01 01 01 00  90  6.7  8.2 99.00 99.00 99.00 999 1023.3  15.6  13.5  15.1 99.0  0.27 //first row, 2nd year
        seconds = Calendar2.isoStringToEpochSeconds("2005-01-01T01");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 90, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 6.7f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 8.2f, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1023.3f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 15.6f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row),13.5f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 15.1f, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), (float)Math2.roundTo(0.27 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //row of data from taml1h2005.txt
        //YYYY MM DD hh mm WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2005 12 31 23 00 100  1.0  1.5 99.00 99.00 99.00 999 1012.5  20.5  19.0  19.4 99.0 99.00 //last row, 2nd year
        seconds = Calendar2.isoStringToEpochSeconds("2005-12-31T23");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 100, "");
        Test.ensureEqual(table.getDoubleData(wspdIndex, row), 1, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 1.5f, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1012.5f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 20.5f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), 19, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 19.4f, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getDoubleData(tideIndex, row), Double.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //row of data from taml112006.txt
        //YYYY MM DD hh mm WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2006 01 01 02 00 330 22.1 99.0 99.00 99.00 99.00 999 1027.5 999.0  18.5 999.0 99.0 99.00 //first row, monthly file
        seconds = Calendar2.isoStringToEpochSeconds("2006-01-01T02");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 330, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 22.1f, "");
        Test.ensureEqual(table.getDoubleData(gstIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1027.5f, "");
        Test.ensureEqual(table.getDoubleData(atmpIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row),18.5f, "");
        Test.ensureEqual(table.getDoubleData(dewpIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getDoubleData(tideIndex, row), Double.NaN, ""); //(float)Math2.roundTo(3.0 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        //row of data from taml112006.txt
        //YYYY MM DD hh mm WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2006 01 31 23 00 140  4.6  6.2 99.00 99.00 99.00 999 1018.2  15.4  16.7   4.4 99.0  1.31  //last row, monthly file
        seconds = Calendar2.isoStringToEpochSeconds("2006-01-31T23");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 140, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 4.6f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 6.2f, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1018.2f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 15.4f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row),16.7f, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), 4.4f, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), (float)Math2.roundTo(1.31 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");

        /* 2011-11-27 TAML1 no longer active???
        //UPDATE_EACH_MONTH
        //row of data from TAML1.txt   //first non-historical row from 45 day file
        //  http://www.ndbc.noaa.gov/data/realtime2/TAML1.txt    //45 day
        //#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
        //#yr  mo dy hr mn degT m/s  m/s     m   sec   sec degT   hPa  degC  degC  degC   mi  hPa    ft
        //2011 10 03 15 00  60  6.2  9.3    MM    MM    MM  MM 1025.8   6.3    MM    MM   MM   MM    MM
        seconds = Calendar2.isoStringToEpochSeconds("2011-10-03T15");
        row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        Test.ensureEqual(table.getStringData(idIndex, row), "TAML1", "");
        Test.ensureEqual(table.getFloatData(latIndex, row), 29.188f, "");
        Test.ensureEqual(table.getFloatData(lonIndex, row), -90.665f, "");
        Test.ensureEqual(table.getDoubleData(depthIndex, row), 0, "");
        Test.ensureEqual(table.getDoubleData(wdIndex, row), 60, "");
        Test.ensureEqual(table.getFloatData(wspdIndex, row), 6.2f, "");
        Test.ensureEqual(table.getFloatData(gstIndex, row), 9.3f, "");
        Test.ensureEqual(table.getDoubleData(wvhtIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(dpdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(apdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getDoubleData(mwdIndex, row), Double.NaN, "");
        Test.ensureEqual(table.getFloatData(aprsIndex, row), 1025.8f, "");
        Test.ensureEqual(table.getFloatData(atmpIndex, row), 6.3f, "");
        Test.ensureEqual(table.getFloatData(wtmpIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(dewpIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getDoubleData(visIndex, row), Double.NaN, ""); //(float)Math2.roundTo(18.5 * Math2.kmPerMile, decimalDigits[visIndex]), "");
        Test.ensureEqual(table.getFloatData(ptdyIndex, row), Float.NaN, "");
        Test.ensureEqual(table.getFloatData(tideIndex, row), Float.NaN, ""); //(float)Math2.roundTo(2.22 * Math2.meterPerFoot, decimalDigits[tideIndex]), "");
        */

        String2.log("testTAML1 was successful");
    }


    /**
     * This is used for maintenance and testing of this class.
     */
    public static void main(String args[]) throws Exception {

        String observationDir = "c:/data/ndbc/";  
        String ndbcNcDir = observationDir + "ndbcMet/";  
        String logDir = observationDir + "logs/";  
        String ndbcStationHtmlDir = observationDir + "ndbcMetStationHtml/";
        String ndbcHistoricalTxtDir = observationDir + "ndbcMetHistoricalTxt/"; 
        String ndbc45DayTxtDir = observationDir + "ndbcMet45DayTxt/";
        
        verbose = true;
        //Table.verbose = true;
        //Table.reallyVerbose = true;
        //oneTime();

        //open a log file
        String dateTime = Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarLocal());
        String2.setupLog(true, false, logDir + "log." + dateTime,
            true, String2.logFileDefaultMaxSize); //append
        String2.log("*** Starting NdbcMetStation " + 
            Calendar2.getCurrentISODateTimeStringLocal() + "\n" +
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());  
        long time = System.currentTimeMillis();

        //MONTHLY UPDATE PROCEDURE (done on/after 25th of month).
        // 1) *** CHANGE firstNearRealTimeData STRING AT TOP OF FILE!

        // 2) *** get new historical files
        //historical yearly files are from: http://www.ndbc.noaa.gov/data/historical/stdmet/
        //  (Once a year ~March 1, the new yearly files appear. 
        //   copy last year's monthly files
        //     cd \data\ndbc
        //     md ndbcMetHistoricalTxt2015  (last year)
        //     copy ndbcMetHistoricalTxt\*2015.txt ndbcMetHistoricalTxt2015
        //     del  ndbcMetHistoricalTxt\*2015.txt
        //   change HISTORICAL_FILES_CURRENT_YEAR at top of file to the current year,
        //   then follow normal update procedure.)
        //2011-02-28 I re-downloaded ALL of the files (since previous years had been modified).
        //   I also re-downloaded ALL of the station html files (by renaming previous 
        //     ndbcMetStationHtml dir to Retired and making a new dir).
        //historical monthly files are from: http://www.ndbc.noaa.gov/data/stdmet/<month3Letter>/  e.g., Jan
        //!!!!**** Windows GUI My Computer doesn't show all the files in the directory! 
        //  Use DOS window "dir" or Linux ls instead of the GUI.
        //downloadNewHistoricalTxtFiles(ndbcHistoricalTxtDir); //time varies, last done 2016-09-26

        // 3) *** get latest 45 day files
        //DON'T download45DayTextFiles after 45 days after last historicalTxt date.
        //download45DayTxtFiles(ndbc45DayTxtDir);  //15-30 minutes, last done 2016-09-26

        // 4) *** Make the nc files
        //!!!!**** EACH MONTH, SOME TESTS NEED UPDATING: SEE "UPDATE_EACH_MONTH"
        //no station info for a station?  search for "no station info" above
        // or lat lon available? search for "get the lat and lon" above
        boolean testMode = false;  //used to: always run 'true' then run 'false'    
        String ignoreStationsBefore = " "; //use " " to process all stations   or lowercase characters to start in middle
        //makeSeparateNcFiles(ndbcStationHtmlDir, ndbcHistoricalTxtDir, ndbc45DayTxtDir, 
        //    ndbcNcDir, ignoreStationsBefore, testMode); //M4700 ~1 hr, was ~3 hrs  //last done 2016-09-26
        test31201Nc(ndbcNcDir);
        test41009Nc(ndbcNcDir);
        test41015Nc(ndbcNcDir);
        test46088Nc(ndbcNcDir);
        testTAML1Nc(ndbcNcDir);  

        // 5) *** make a copy of the c:/data/ndbc/ndbcMet directory, e.g., ndbcMet20070425o,
        // and *don't* ftp it to coastwatch's 

        // 6) *** addLastNDaysInfo   
        //Doing this isn't (strictly-speaking) needed for the monthly reprocessing of the ndbc data,
        //  but it tests if CWBrowserSA will be able to do it on coastwatch
        //  (which isn't obvious given the changes to NDBC datafile formats).
        //5day does 5day if possible (or 45 if not), so I usually just do 5day now)
        //(5 days takes 12 minutes)
        //but 45 is more likely to get more information (if needed and if available)
        //(45 days takes 25 minutes)
        testMode = false; //always run 'true' then run 'false'    
        //addLastNDaysInfo(ndbcNcDir, 5, testMode);  //usually 5
        //!!!!**** EACH MONTH, THIS TEST NEED UPDATING
        test46088AddLastNDaysNc(ndbcNcDir); 

        /* 7) *** On laptop: 
                rename ndbcMet ndbcMett
                cd \data\ndbc, use git bash:  tar zcvf ndbcMett.tgz ndbcMett
            * ftp ndbcMett.tgz to coastwatch's /u00/data/points
cd /u00/data/points
tar zxvf ndbcMett.tgz
as su
  chown -R tomcat:erddap ndbcMett
  chmod -R a+rw ndbcMett
rename ndbcMet ndbcMetR20150224 ndbcMet
rename ndbcMett ndbcMet ndbcMett
rm ndbcMett.tgz
        */

        // 8) *** In datasetsFEDCW.xml and datasets2.xml, 
        //   change the 2 'historic' dates in summary attribute for datasetID=cwwcNDBCMet 
        //   to reflect new transition date.
        // * On laptop, use updateDatasetsXml.py  (EditPlus Python tools #2)
        // * Copy datasetsFEDCW.xml to coastwatch and rename to put in place
        // * Set cwwcNDBCMet flag.

        // 9) *** test cwwcNDBCMet     sometimes:
        // * Copy data from new dir, e.g., c:/data/ndbc/ndbcMet20101025 to c:/u00/data/points/ndbcMet20101025
        //   Then rename into place.
        // * Run TestAll:   String2.log(EDD.testDasDds("cwwcNDBCMet"));         
        //   to see if trouble.

        String2.log("\n*** NdbcMetStation.main finished successfully in " + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
        String2.returnLoggingToSystemOut();

    }


}
