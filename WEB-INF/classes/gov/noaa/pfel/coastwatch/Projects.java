/* 
 * Projects Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.*;
import com.cohort.util.*;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.hdf.*;
import gov.noaa.pfel.coastwatch.pointdata.ScriptRow;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.*;
import gov.noaa.pfel.erddap.dataset.EDD;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.util.TimeZone;

import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.*;

import org.apache.commons.codec.digest.DigestUtils;  //in netcdf-all.jar
//import org.codehaus.janino.ExpressionEvaluator;

import java.time.*;
import java.time.format.*;

/**
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;



/**
 * This class has static methods special, one-time projects.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-12-11
 */
public class Projects  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;


    public final static String stationColumnName = "ID";


    /** A special project for Roy. 
     * This extracts/converts the Channel Islands data in
     * f:/programs/kushner/KFM_Temperature.txt
     * and SiteLocations.txt into separate 4D .nc files with metadata.
     * 12/11/06
     * I put the results in \\Xserve\pfel_share\PERSON_PERSON\BobSimons\ChannelIslands
     * for Roy. 12/13/06
     * Email from David_Kushner@nps.gov 12/12/06
<pre>
1) I note from the email below that you don't want the exact lat/lon and
depth locations made public.  OK.  Is it sufficient for me to simply use
the degree and minute values, and ignore the seconds values?

YES

2) Given that the lat and lon will be crude, can I use the Depth as is? YES
Or, how should I obscure it? NO NEED TO IF.

3) It seems that many of the minute values for the SiteLocations are 0.
More than one would normally expect by chance.  Are the minute values
correct?  Or can you provide me with an updated SiteLocations file (.mdb
or ASCII files are fine)?

YES, IT IS A FORMAT ISSUE IN OUR DATA SET, THAT I SHOULD FIX.  0 = 00 AND 2
EQUALS 02.  LET ME KNOW IF THAT DOES NOT MAKE SENSE.

4) Are the times recorded in the file UTC times? Or are they local
times? If they are local times, did you use clock time (so that the
times may be standard time or daylight saving time)?

THEY ARE PACIFIC STANDARD TIME SET TO THE COMPUTER I USE TO SET THE
LOGGERS.  WE RETRIEVE THEM ONCE PER YEAR SO I ASSUME THEY ARE CORRECTED FOR
DAYLIGHT SAVING TIME, BUT I NEVER REALLY THOUGHT ABOUT THIS.

[in another email he says]
Wow, good question.  They are local PST.  However, I will just presume that
they are daylight saving time, but never thought about it.  We retrieved
the data only once per year in the summer so I never thought to check that.

[Bob interprets this as: all times are Pacific Daylight Saving Time, 
e.g., 7 hours earlier than Zulu,
since he used his computer's clock in the summer to set the logger's clocks.]

5) Is it okay if the courtesy information appears as "Channel Islands
National Park, National Park Service"?

I WILL CHECK ON THIS, BUT I THINK IT SHOULD HAVE BOTH:  :COURTESY OF THE
NATIONAL PARK SERVICE, CHANNEL ISLANDS NATIONAL PARK.

[in the 12/12/2006 email he says:]
We have been usuing Onset computer Corp. temperature loggers.  However, we
have been usuing these since the company began selling them and the models
have changed three times.  In the early years we used their HoboTemp tm
loggers, these had little memory which is why we recorded temperature every
4-5 hours.  Then they developed the StowAway loggers with more memory and
went to one hour intervals.  There was another version of StowAway before
we then switched to the newer Tidbit (these are completely waterproof,
yeah!) loggers several years ago.  It often took sevaral years to swap out
models since we only visit the sites once per year so it is hard to get an
exact date for each site of which model was used when, though I do have
this info on raw data sheets.  All the temperature loggers are supposed to
be accurate to +- 0.2C.  For many of the past 10 years I have installed two
loggers in the same underwater housing at most of the sites and have
compared the data.  In nearly all cases the loggers have been within
specifications, so I do have much confidence in the loggers to +-0.2 C.  In
the early years I was even testing them periodically in an ice bath.

</pre>
     */ 
    public static void channelIslands() throws Exception {
        String2.log("\n*** Projects.channelIslands");

        //read SiteLocations.txt 
        //IslandName\tSiteName\tLatDegrees\tLatMinutes\tLatSeconds\tLatDir\tLongDegrees\tLongMinutes\tLongSeconds\tLongDir\tDepth (meters)
        //Anacapa\tAdmiral's Reef\t34\t00\t200\tN\t119\t25\t520\tW\t16
        //Anacapa\tBlack Sea Bass Reef\t34\t00\t756\tN\t119\t23\t351\tW\t17
        Table site = new Table();
        StringArray siteID = new StringArray();
        FloatArray siteLon = new FloatArray();
        FloatArray siteLat = new FloatArray();
        IntArray siteDepth = new IntArray();
        site.addColumn("ID", siteID);
        site.addColumn("LON", siteLon);
        site.addColumn("LAT", siteLat);
        site.addColumn("DEPTH", siteDepth);
        int siteNRows;
        {
            //read the site location info
            Table tSite = new Table();
            tSite.readASCII("c:/programs/kushner/Site_Locations.txt");

            //test that first row's data is as expected
            Test.ensureEqual(tSite.getStringData(0, 0), "Anacapa", "");
            Test.ensureEqual(tSite.getStringData(1, 0), "Admiral's Reef", "");
            Test.ensureEqual(tSite.getDoubleData(2, 0), 34, "");  //lat
            Test.ensureEqual(tSite.getDoubleData(3, 0), 0, "");
            Test.ensureEqual(tSite.getDoubleData(4, 0), 200, "");
            Test.ensureEqual(tSite.getStringData(5, 0), "N", "");
            Test.ensureEqual(tSite.getDoubleData(6, 0), 119, ""); //lon
            Test.ensureEqual(tSite.getDoubleData(7, 0), 25, "");
            Test.ensureEqual(tSite.getDoubleData(8, 0), 520, "");
            Test.ensureEqual(tSite.getStringData(9, 0), "W", "");
            Test.ensureEqual(tSite.getDoubleData(10, 0), 16, "");

            //fill siteID, siteLat, siteLon, siteDepth  in new site table
            siteNRows = tSite.nRows();
            for (int row = 0; row < siteNRows; row++) {
                siteID.add(tSite.getStringData(0, row) + " (" + tSite.getStringData(1, row) + ")");

                //lat
                double d = tSite.getDoubleData(2, row) + 
                    tSite.getDoubleData(3, row) / 60.0;
                    //12/01/2006 email: Kushner says don't make exact location public
                    //so I say: ignore "seconds" column (which is actually milli-minutes;
                    //his 12/12/2006 email says "The .351[sic 351] is .351 X 60 (seconds) = 21.06 seconds.").
                    //+ tSite.getDoubleData(4, row) / 60000.0
                siteLat.addDouble(d);

                //lon
                d = tSite.getDoubleData(6, row) + 
                    tSite.getDoubleData(7, row) / 60.0;
                    //+ tSite.getDoubleData(8, row) / 60000.0 
                if (tSite.getStringData(9, 0).equals("W"))
                    d = -d;                 
                siteLon.addDouble(d);

                //but I am using depth value as is
                siteDepth.add(tSite.getIntData(10, row));
            }

            //sort by ID
            site.leftToRightSort(1);
            String2.log("site table=\n" + site.toString(siteNRows));
        }

        //read f:/programs/kushner/KFM_Temperature.txt which has all the temperature data
        //IslandName\tSiteName\tDate\tTemperatureC
        //Anacapa\tAdmiral's Reef\t8/26/1995 14:49:00\t18.47
        //Anacapa\tAdmiral's Reef\t8/25/1995 14:49:00\t18.79
        Table temp = new Table();
        temp.allowRaggedRightInReadASCII = true; //it has some missing temp values
        temp.readASCII("c:/programs/kushner/KFM_Temperature.txt");

        //ensure that first row's data is as expected
        Test.ensureEqual(temp.getStringData(0, 0), "Anacapa", "");
        Test.ensureEqual(temp.getStringData(1, 0), "Admiral's Reef", "");
        Test.ensureEqual(temp.getStringData(2, 0), "8/26/1995 14:49:00", "");
        Test.ensureEqual(temp.getFloatData( 3, 0), 18.47f, "");
        int tempNRows = temp.nRows();

        //convert temp table's dateTime to epochSeconds
        {
            StringArray oldTimePA = (StringArray)temp.getColumn(2);
            DoubleArray newTimePA = new DoubleArray();
            for (int row = 0; row < tempNRows; row++) {
                double sec = Calendar2.gcToEpochSeconds(
                    Calendar2.parseUSSlash24Zulu(temp.getStringData(2, row))); //throws Exception
                if (sec < 100 || sec > 2e9)
                    String2.log("row=" + row + " sec=" + sec + " Unexpected time=" + temp.getStringData(2, row));
                newTimePA.add(sec);
            }
            temp.setColumn(2, newTimePA);
        }

        //sort by  island, site, time
        String2.log("pre-sort n=" + temp.nRows() + " temp table=\n" + temp.toString(5));
        temp.leftToRightSort(3);

        //go through rows of temp, saving separate station files
        String oldIsland, oldSite, oldID, newIsland = "", newSite = "", newID = "";
        Attributes attrib;
        int oldPo, newPo = -1;
        int nStationsCreated = 0;
        Table station = new Table();   //x,y,z,t,id,temperature
        station.addColumn(0, "LON", new FloatArray()); //x
        station.addColumn(1, "LAT", new FloatArray()); //y
        station.addColumn(2, "DEPTH", new IntArray()); //z
        station.addColumn(3, "TIME", new DoubleArray()); //t

        StringArray stationPA = new StringArray();
        Attributes stationAttributes = new Attributes();
        station.addColumn(4, stationColumnName, stationPA, stationAttributes); //id
        stationAttributes.set("long_name", "Station Identifier");
        //stationAttributes.set("units", DataHelper.UNITLESS);

        station.addColumn(5, "Temperature", new FloatArray()); //temperature
        attrib = station.columnAttributes(5);
        attrib.set("long_name", "Sea Temperature");
        attrib.set("standard_name", "sea_water_temperature");
        attrib.set("units", "degree_C");

        for (int row = 0; row <= tempNRows; row++) { //yes, =n since comparing current to previous row
            oldIsland = newIsland;
            oldSite = newSite;
            oldID = newID;
            oldPo = newPo;
            if (row < tempNRows) {
                newIsland = temp.getStringData(0, row);
                newSite = temp.getStringData(1, row);             
                newID = newIsland + " (" + newSite + ")";
                if (newID.equals(oldID)) {
                    newPo = oldPo;
                } else {
                    newPo = siteID.indexOf(newID, 0);
                    Test.ensureNotEqual(newPo, -1, "tID not found: " + newID);
                    String2.log("newID=" + newID);
                }
            }

            //save the station file
            if (row == tempNRows || 
                (row > 0 && !newID.equals(oldID))) {
                String tOldIsland = String2.replaceAll(oldIsland, " ", "");
                String tOldSite = String2.replaceAll(oldSite, " ", "");
                tOldSite = String2.replaceAll(tOldSite, "'", "");
                String tempID = "KFMTemperature_" + tOldIsland + "_" + tOldSite;

                //setAttributes
                station.setAttributes(0, 1, 2, 3, //x,y,z,t
                    "Sea Temperature (Channel Islands, " + oldIsland + ", " + oldSite + ")", //boldTitle
                    "Station", //cdmDataType
                    DataHelper.ERD_CREATOR_EMAIL, //"Roy.Mendelssohn@noaa.gov", //creatorEmail
                    DataHelper.ERD_CREATOR_NAME,  //"NOAA NMFS SWFSC ERD",  //creatorName
                    DataHelper.ERD_CREATOR_URL,   //"https://www.pfeg.noaa.gov", //creatorUrl
                    DataHelper.ERD_PROJECT,       
                    tempID, //id
                    "GCMD Science Keywords", //keywordsVocabulary,
                    "Oceans > Ocean Temperature > Water Temperature", //keywords

                    //references   from 2006-12-19 email from Kushner
                    "Channel Islands National Parks Inventory and Monitoring information: " +
                        "http://nature.nps.gov/im/units/medn . " +
                        "Kelp Forest Monitoring Protocols: " +
                        "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                    //summary  from 2006-12-19 email from Kushner
                    "The subtidal temperature data taken at Channel Islands National " +
                        "Park's Kelp Forest Monitoring Programs permanent monitoring sites.  Since " +
                        "1993, remote temperature loggers manufactured by Onset Computer Corporation " +
                        "were deployed at each site approximately 10-20 cm from the bottom in a " +
                        "underwater housing.  Since 1993, three models of temperature loggers " +
                        "(HoboTemp (tm), StowAway (R) and Tidbit(R) )were used to collect " +
                        "temperature data every 1-5 hours depending on the model used.",
                    //my old summary
                    //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                    //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                    //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                    //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                    //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                    "Channel Islands National Park, National Park Service", 
                    null); //timeLongName     use default long name) {

                //add the National Park Service disclaimer from 2006-12-19 email
                String license = station.globalAttributes().getString("license");
                license += "  National Park Service Disclaimer: " +
                    "The National Park Service shall not be held liable for " +
                    "improper or incorrect use of the data described and/or contained " +
                    "herein. These data and related graphics are not legal documents and " +
                    "are not intended to be used as such. The information contained in " +
                    "these data is dynamic and may change over time. The data are not " +
                    "better than the original sources from which they were derived. It is " +
                    "the responsibility of the data user to use the data appropriately and " +
                    "consistent within the limitation of geospatial data in general and " +
                    "these data in particular. The related graphics are intended to aid " +
                    "the data user in acquiring relevant data; it is not appropriate to " +
                    "use the related graphics as data. The National Park Service gives no " +
                    "warranty, expressed or implied, as to the accuracy, reliability, or " +
                    "completeness of these data. It is strongly recommended that these " +
                    "data are directly acquired from an NPS server and not indirectly " +
                    "through other sources which may have changed the data in some way. " +
                    "Although these data have been processed successfully on computer " +
                    "systems at the National Park Service, no warranty expressed or " +
                    "implied is made regarding the utility of the data on other systems " +
                    "for general or scientific purposes, nor shall the act of distribution " +
                    "constitute any such warranty. This disclaimer applies both to " +
                    "individual use of the data and aggregate use with other data.";
                station.globalAttributes().set("license", license);

                //save the station table
                station.saveAs4DNcWithStringVariable("c:/programs/kushner/" + tempID + ".nc", 
                    0, 1, 2, 3, 4);  //x,y,z,t, stringVariable=ID
                nStationsCreated++;

                //see what results are like 
                if (nStationsCreated == 1) {
                    Table tTable = new Table();
                    tTable.read4DNc("c:/programs/kushner/" + tempID + ".nc", null, 0, //standardizeWhat=0
                        stationColumnName, 4); 
                    String2.log("\nstation0=\n" + tTable.toString(3));
                    //from site ascii file
                    //Anacapa	Admiral's Reef	34	00	200	N	119	25	520	W	16
                    //data from original ascii file
                    //Anacapa	Admiral's Reef	8/26/1995 14:49:00	18.47 //first Admiral's Reef in file
                    //Anacapa	Admiral's Reef	8/25/1995 17:49:00	18.95 //random
                    //Anacapa	Admiral's Reef	5/18/1998 11:58:00	14.27 //last  in file
                    DoubleArray ttimepa = (DoubleArray)tTable.getColumn(3);
                    double adjustTime = 7 * Calendar2.SECONDS_PER_HOUR; 
                    int trow;   
                    trow = ttimepa.indexOf("" + (Calendar2.isoStringToEpochSeconds("1995-08-26 14:49:00") + adjustTime), 0);
                    Test.ensureNotEqual(trow, -1, "");
                    Test.ensureEqual(tTable.getFloatData(0, trow), -119f - 25f/60f, "");
                    Test.ensureEqual(tTable.getFloatData(1, trow), 34f, "");
                    Test.ensureEqual(tTable.getFloatData(2, trow), 16f, "");
                    Test.ensureEqual(tTable.getStringData(4, trow), "Anacapa (Admiral's Reef)", "");
                    Test.ensureEqual(tTable.getFloatData(5, trow), 18.47f, "");

                    trow = ttimepa.indexOf("" + (Calendar2.isoStringToEpochSeconds("1995-08-25 17:49:00") + adjustTime), 0);
                    Test.ensureNotEqual(trow, -1, "");
                    Test.ensureEqual(tTable.getFloatData(0, trow), -119f - 25f/60f, "");
                    Test.ensureEqual(tTable.getFloatData(1, trow), 34f, "");
                    Test.ensureEqual(tTable.getFloatData(2, trow), 16f, "");
                    Test.ensureEqual(tTable.getStringData(4, trow), "Anacapa (Admiral's Reef)", "");
                    Test.ensureEqual(tTable.getFloatData(5, trow), 18.95f, "");

                    trow = ttimepa.indexOf("" + (Calendar2.isoStringToEpochSeconds("1998-05-18 11:58:00") + adjustTime), 0);
                    Test.ensureNotEqual(trow, -1, "");
                    Test.ensureEqual(tTable.getFloatData(0, trow), -119f - 25f/60f, "");
                    Test.ensureEqual(tTable.getFloatData(1, trow), 34f, "");
                    Test.ensureEqual(tTable.getFloatData(2, trow), 16f, "");
                    Test.ensureEqual(tTable.getStringData(4, trow), "Anacapa (Admiral's Reef)", "");
                    Test.ensureEqual(tTable.getFloatData(5, trow), 14.27f, "");
                }

                //set station table to 0 rows
                station.removeRows(0, station.nRows());
            }

            //add a row of data to station table
            if (row < tempNRows) {
                station.addFloatData(0, siteLon.get(newPo)); //x
                station.addFloatData(1, siteLat.get(newPo)); //y
                station.addIntData(2, siteDepth.get(newPo)); //z
                //t is adjusted from Pacific Daylight Saving Time to UTC (they are 7 hours ahead of PDST)
                //see metadata "summary" regarding this conversion.
                station.addDoubleData(3, temp.getDoubleData(2, row) + 7 * Calendar2.SECONDS_PER_HOUR); //t
                station.addStringData(4, newID); //id
                station.addFloatData(5, temp.getFloatData(3, row)); //temperature
            }
        }
        Test.ensureEqual(nStationsCreated, siteNRows, "nStationsCreated != siteNRows");

        String2.log("\n*** Projects.channelIslands finished successfully.");
    }

    /** 
     * Makes f:/programs/kfm200801/KFMSpeciesName.tab which has
     * a table of species number, species name in original files, species name in my files.
     * I remove the blank second line (no units) manually.
     * I will put the results in otter /u00/bob/kfm2008/ which mimics my f:/programs/kfm200801 .
     * for Lynn. 
     */ 
    public static void kfmSpeciesNameConversion200801() throws Exception {
        String2.log("\n*** Projects.kfmSpeciesNameConversion200801");

        //*** read the site location info
        //test that first row's data is as expected
        Table spp = new Table();
        spp.readASCII("c:/programs/kfm200801/KFMSpeciesName.tab");
        Test.ensureEqual(spp.getColumnName(0), "Species", "");
        Test.ensureEqual(spp.getColumnName(1), "Species Name", "");
        spp.removeColumns(2, spp.nColumns());
        StringArray oldNames = (StringArray)spp.getColumn(1);
        StringArray newNames = new StringArray();
        spp.addColumn(2, "New Name", newNames);

        int nRows = spp.nRows();
        for (int row = 0; row < nRows; row++) 
            newNames.add(convertSpeciesName(oldNames.get(row)));
        spp.saveAsTabbedASCII("c:/programs/kfm200801/KFMSpeciesNewName.tab");
     }

    
    /** 
     * This is the Jan 2008 revision of channelIslands().
     * This extracts/converts the Channel Islands data in
     * f:/programs/kfm200801/KFMHourlyTemperature.tab
     * and f:/programs/kfm200801/KFM_Site_Info.tsv into separate 4D .nc files with metadata.
     * 2008-01-15
     * I will put the results in otter /u00/bob/kfm2008/ which mimics my f:/programs/kfm200801 .
     */ 
    public static void kfmTemperature200801() throws Exception {
        String2.log("\n*** Projects.kfmTemperature200801");
        File2.deleteAllFiles("c:/programs/kfm200801/KFMTemperature/");

        //*** read the site location info
        //test that first row's data is as expected
        //Island	SiteName	Lat	Lon	Depth (meters)
        //San Miguel	Wyckoff Ledge	34.0166666666667	-120.383333333333	13
        Table site = new Table();
        site.readASCII("c:/programs/kfm200801/KFM_Site_Info.tsv"); //copied from last year
        Test.ensureEqual(site.getColumnName(0), "Island", "");
        Test.ensureEqual(site.getColumnName(1), "SiteName", "");
        Test.ensureEqual(site.getColumnName(2), "Lat", "");
        Test.ensureEqual(site.getColumnName(3), "Lon", "");
        Test.ensureEqual(site.getColumnName(4), "Depth (meters)", "");
        Test.ensureEqual(site.getStringData(0, 0), "San Miguel", "");
        Test.ensureEqual(site.getStringData(1, 0), "Wyckoff Ledge", "");
        Test.ensureEqual(site.getFloatData(2, 0), 34.016666666f, "");  //lat   to nearest minute
        Test.ensureEqual(site.getFloatData(3, 0), -120.38333333f, "");       //to nearest minute  
        Test.ensureEqual(site.getDoubleData(4, 0), 13, "");
        int siteNRows = site.nRows();
        String2.log(site.toString());

        //*** read f:/programs/kfm200801/KFMHourlyTemperature.tab which has all the temperature data
        //test that first row's data is as expected
        //SiteNumber	IslandName	SiteName	Date	Time	TemperatureC
        //11	Anacapa	Admiral's Reef	8/26/1993 0:00:00	12/30/1899 10:35:00	18.00
        Table temp = new Table();
        temp.allowRaggedRightInReadASCII = true; //it has some missing temp values
        temp.readASCII("c:/programs/kfm200801/KFMHourlyTemperature.tab");
        temp.removeColumn(0); //now columns shifted down one
        //Test.ensureEqual(temp.getColumnName(0), "SiteNumber", "");
        Test.ensureEqual(temp.getColumnName(0), "IslandName", "");
        Test.ensureEqual(temp.getColumnName(1), "SiteName", "");
        Test.ensureEqual(temp.getColumnName(2), "Date", "");
        Test.ensureEqual(temp.getColumnName(3), "Time", "");
        Test.ensureEqual(temp.getColumnName(4), "TemperatureC", "");
        //Test.ensureEqual(temp.getFloatData(0, 0), 11, "");
        Test.ensureEqual(temp.getStringData(0, 0), "Anacapa", "");
        Test.ensureEqual(temp.getStringData(1, 0), "Admiral's Reef", "");
        Test.ensureEqual(temp.getStringData(2, 0), "8/26/1993 0:00:00", "");
        Test.ensureEqual(temp.getStringData(3, 0), "12/30/1899 10:35:00", "");
        Test.ensureEqual(temp.getFloatData( 4, 0), 18f, "");
        int tempNRows = temp.nRows();

        //convert temp table's IslandName+SiteName->ID; date+time->epochSeconds
        {
            StringArray oldIslandName = (StringArray)temp.getColumn(0);
            StringArray oldSiteName   = (StringArray)temp.getColumn(1);
            StringArray oldDatePA     = (StringArray)temp.getColumn(2);
            StringArray oldTimePA     = (StringArray)temp.getColumn(3);
            StringArray newID = new StringArray();
            DoubleArray newTimePA = new DoubleArray();
            for (int row = 0; row < tempNRows; row++) {
                String tID = oldIslandName.get(row) + " (" + oldSiteName.get(row) + ")";
                newID.add(tID);

                String tDate = oldDatePA.get(row);
                String tTime = oldTimePA.get(row);
                int po1 = tDate.indexOf(' ');
                int po2 = tTime.indexOf(' ');
                String dt = tDate.substring(0, po1) + tTime.substring(po2);
                double sec = Calendar2.gcToEpochSeconds(
                    Calendar2.parseUSSlash24Zulu(dt)); //throws Exception
                if (sec < 100 || sec > 2e9)
                    String2.log("row=" + row + " Unexpected sec=" + sec + " from date=" + tDate + " time=" + tTime);
                newTimePA.add(sec);
            }
            temp.removeColumn(3);
            temp.removeColumn(2);
            temp.removeColumn(1);
            temp.removeColumn(0);
            temp.addColumn(0, "Time", newTimePA, new Attributes());
            temp.addColumn(1, stationColumnName, newID, new Attributes());
            //col 2 is temperature
        }

        //sort by  ID, Time
        String2.log("pre-sort n=" + temp.nRows() + " temp table=\n" + temp.toString(5));
        temp.sort(new int[]{1, 0}, new boolean[] {true, true});

        //go through rows of temp, saving separate station files
        int nStationsCreated = 0;
        Table station = new Table();   //x,y,z,t,id,temperature
        station.addColumn(0, "LON", new FloatArray()); //x
        station.addColumn(1, "LAT", new FloatArray()); //y
        station.addColumn(2, "DEPTH", new IntArray()); //z

        DoubleArray stationTime = new DoubleArray();
        station.addColumn(3, "TIME", stationTime); 

        StringArray stationID = new StringArray();
        Attributes stationAttributes = new Attributes();
        stationAttributes.set("long_name", "Station Identifier");
        //stationAttributes.set("units", DataHelper.UNITLESS);
        station.addColumn(4, stationColumnName, stationID, stationAttributes); 

        FloatArray stationTemperature = new FloatArray();
        station.addColumn(5, "Temperature", stationTemperature); 
        Attributes attrib = station.columnAttributes(5);
        attrib.set("long_name", "Sea Temperature");
        attrib.set("standard_name", "sea_water_temperature");
        attrib.set("units", "degree_C");

        int first = 0;
        String oldID, newID = null;
        for (int row = 0; row <= tempNRows; row++) { //yes, =n since comparing current to previous row
            oldID = newID;
            if (row < tempNRows) 
                newID = temp.getStringData(1, row);

            //save the station file?
            if (row == tempNRows || 
                (row > 0 && !newID.equals(oldID))) { //station ID has changed

                //find the matching site
                String oldIsland = null, oldSite = null;
                int siteRow = 0;
                while (siteRow < siteNRows) {
                    oldIsland = site.getStringData(0, siteRow);
                    oldSite = site.getStringData(1, siteRow);
                    String tID = oldIsland + " (" + oldSite + ")";
                    if (tID.equals(oldID))
                        break;
                    siteRow++;
                }
                Test.ensureNotEqual(siteRow, siteNRows, "");
                String tempID = "KFMTemperature_" + oldIsland + "_" + oldSite;
                tempID = String2.replaceAll(tempID, "'", "");
                tempID = String2.replaceAll(tempID, " ", "_");

                //add multiple copies of lon, lat, depth
                int tn = stationTime.size();
                String2.log("tn=" + tn);
                ((FloatArray)station.getColumn(0)).addN(tn, site.getFloatData(3, siteRow)); //x
                ((FloatArray)station.getColumn(1)).addN(tn, site.getFloatData(2, siteRow)); //y
                ((IntArray)  station.getColumn(2)).addN(tn, site.getIntData(  4, siteRow)); //z

                //setAttributes
                station.setAttributes(0, 1, 2, 3, //x,y,z,t
                    "Sea Temperature (Kelp Forest Monitoring, Channel Islands)", //boldTitle
                    "Station", //cdmDataType
                    DataHelper.ERD_CREATOR_EMAIL, //"Roy.Mendelssohn@noaa.gov", //creatorEmail
                    DataHelper.ERD_CREATOR_NAME,  //"NOAA NMFS SWFSC ERD",  //creatorName
                    DataHelper.ERD_CREATOR_URL,   //"https://www.pfeg.noaa.gov", //creatorUrl
                    DataHelper.ERD_PROJECT,       
                    tempID, //id
                    "GCMD Science Keywords", //keywordsVocabulary,
                    "Oceans > Ocean Temperature > Water Temperature", //keywords

                    //references   from 2006-12-19 email from Kushner
                    "Channel Islands National Parks Inventory and Monitoring information: " +
                        //was "http://www.nature.nps.gov/im/units/chis/ " +
                        "http://nature.nps.gov/im/units/medn . " +
                        "Kelp Forest Monitoring Protocols: " +
                        "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                    //summary  from 2006-12-19 email from Kushner
                    "The subtidal temperature data taken at Channel Islands National " +
                        "Park's Kelp Forest Monitoring Programs permanent monitoring sites.  Since " +
                        "1993, remote temperature loggers manufactured by Onset Computer Corporation " +
                        "were deployed at each site approximately 10-20 cm from the bottom in a " +
                        "underwater housing.  Since 1993, three models of temperature loggers " +
                        "(HoboTemp (tm), StowAway (R) and Tidbit(R) )were used to collect " +
                        "temperature data every 1-5 hours depending on the model used.",
                    //my old summary
                    //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                    //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                    //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                    //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                    //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                    "Channel Islands National Park, National Park Service", 
                    null); //timeLongName     use default long name) {

                //add the National Park Service disclaimer from 2006-12-19 email
                String license = station.globalAttributes().getString("license");
                license += "  National Park Service Disclaimer: " +
                    "The National Park Service shall not be held liable for " +
                    "improper or incorrect use of the data described and/or contained " +
                    "herein. These data and related graphics are not legal documents and " +
                    "are not intended to be used as such. The information contained in " +
                    "these data is dynamic and may change over time. The data are not " +
                    "better than the original sources from which they were derived. It is " +
                    "the responsibility of the data user to use the data appropriately and " +
                    "consistent within the limitation of geospatial data in general and " +
                    "these data in particular. The related graphics are intended to aid " +
                    "the data user in acquiring relevant data; it is not appropriate to " +
                    "use the related graphics as data. The National Park Service gives no " +
                    "warranty, expressed or implied, as to the accuracy, reliability, or " +
                    "completeness of these data. It is strongly recommended that these " +
                    "data are directly acquired from an NPS server and not indirectly " +
                    "through other sources which may have changed the data in some way. " +
                    "Although these data have been processed successfully on computer " +
                    "systems at the National Park Service, no warranty expressed or " +
                    "implied is made regarding the utility of the data on other systems " +
                    "for general or scientific purposes, nor shall the act of distribution " +
                    "constitute any such warranty. This disclaimer applies both to " +
                    "individual use of the data and aggregate use with other data.";
                station.globalAttributes().set("license", license);

                //save the station table
                station.ensureValid();
                station.saveAs4DNcWithStringVariable("c:/programs/kfm200801/KFMTemperature/" + tempID + ".nc", 
                    0, 1, 2, 3, 4);  //x,y,z,t, stringVariable=ID
                nStationsCreated++;

                //see what results are like 
                if (nStationsCreated == 1) {
                    Table tTable = new Table();
                    tTable.read4DNc("c:/programs/kfm200801/KFMTemperature/" + tempID + ".nc",
                        null, -1, stationColumnName, 4); //standardizeWhat=0
                    String2.log("\nstation0=\n" + tTable.toString(3));
                    //from site ascii file
                    //Anacapa	Admiral's Reef	34	00	200	N	119	25	520	W	16
                    //data from original ascii file
                    //11	Anacapa	Admiral's Reef	8/26/1993 0:00:00	12/30/1899 10:35:00	18.00 //first line
                    //11	Anacapa	Admiral's Reef	11/3/2001 0:00:00	12/30/1899 16:12:00	15.88 //random
                    //11	Anacapa	Admiral's Reef	7/13/2007 0:00:00	12/30/1899 11:25:00	16.89 //last line
                    DoubleArray ttimepa = (DoubleArray)tTable.getColumn(3);
                    double adjustTime = 7 * Calendar2.SECONDS_PER_HOUR; 
                    int trow;   
                    trow = ttimepa.indexOf("" + (Calendar2.isoStringToEpochSeconds("1993-08-26 10:35:00") + adjustTime), 0);
                    Test.ensureNotEqual(trow, -1, "");
                    Test.ensureEqual(tTable.getFloatData(0, trow), -119f - 25f/60f, "");
                    Test.ensureEqual(tTable.getFloatData(1, trow), 34f, "");
                    Test.ensureEqual(tTable.getFloatData(2, trow), 16f, "");
                    Test.ensureEqual(tTable.getStringData(4, trow), "Anacapa (Admiral's Reef)", "");
                    Test.ensureEqual(tTable.getFloatData(5, trow), 18f, "");

                    trow = ttimepa.indexOf("" + (Calendar2.isoStringToEpochSeconds("2001-11-03 16:12:00") + adjustTime), 0);
                    Test.ensureNotEqual(trow, -1, "");
                    Test.ensureEqual(tTable.getFloatData(0, trow), -119f - 25f/60f, "");
                    Test.ensureEqual(tTable.getFloatData(1, trow), 34f, "");
                    Test.ensureEqual(tTable.getFloatData(2, trow), 16f, "");
                    Test.ensureEqual(tTable.getStringData(4, trow), "Anacapa (Admiral's Reef)", "");
                    Test.ensureEqual(tTable.getFloatData(5, trow), 15.88f, "");

                    trow = ttimepa.indexOf("" + (Calendar2.isoStringToEpochSeconds("2007-07-13 11:25:00") + adjustTime), 0);
                    Test.ensureNotEqual(trow, -1, "");
                    Test.ensureEqual(tTable.getFloatData(0, trow), -119f - 25f/60f, "");
                    Test.ensureEqual(tTable.getFloatData(1, trow), 34f, "");
                    Test.ensureEqual(tTable.getFloatData(2, trow), 16f, "");
                    Test.ensureEqual(tTable.getStringData(4, trow), "Anacapa (Admiral's Reef)", "");
                    Test.ensureEqual(tTable.getFloatData(5, trow), 16.89f, "");
                }

                //set station table to 0 rows
                station.removeRows(0, station.nRows());
            }

            //add a row of data to station table
            if (row < tempNRows) {
                //t is adjusted from Pacific Daylight Saving Time to UTC (they are 7 hours ahead of PDST)
                //see metadata "summary" regarding this conversion.
                stationTime.add(temp.getDoubleData(0, row) + 7 * Calendar2.SECONDS_PER_HOUR); //t
                stationID.add(newID);
                stationTemperature.add(temp.getFloatData(2, row));
            }
        }
        String2.log("siteNRows=" + siteNRows);
        String2.log("nStationsCreated=" + nStationsCreated);
        String2.log("\n*** Projects.kfmTemperature200801 finished successfully.");
    }

    public static String convertSpeciesName(String oldName) {
        String newName = oldName;
        int sp2Po = newName.indexOf("  ");
        if (sp2Po > 0) newName = newName.substring(0, sp2Po);
        int pPo = newName.indexOf("(");
        if (pPo > 0)   newName = newName.substring(0, pPo);
        newName = String2.replaceAll(newName, ".", "");
        newName = String2.replaceAll(newName, ",", "");
        newName = String2.replaceAll(newName, ' ', '_');
        return newName;
    }

    /**
     * KFM - This is the second group of files from Kushner (see f:/programs/kfm, 
     * processed starting 2007-03-27). This is biological data.
     * <ul>
     * <li>I converted the .mdb file to separate tab-separated value files,
     *    one per table, in MS Access (see instructions below --
     *    or with &lt;65,000 rows just use clipboard to move data) with extension .tab.
     *   <p>In the future: to export Access tables to a format we like (tab separated ASCII files):
     *   <ul>
     *   <li>In Access, select the specified table
     *     <ul>
     *     <li>Choose "File : Export"
     *     <li>Pick the table
     *     <li>Choose "Save as type: Text Files"
     *     <li>Change the file's extension to .tab
     *     <li>Don't check "Save formatted"
     *     <li>Click "Export All"
     *     </ul>
     *   <li>Check "Delimited"
     *   <li>Click "Next"
     *   <li>Check "Tab"
     *   <li>Check "Include Field Names on First Row"
     *   <li>Choose "Text Qualifier: none"
     *   <li>Click "Next"
     *   <li>Click "Finish"
     *   </ul>
     * <li>In CoStat, I removed extraneous columns from .tab files and save as .tsv so 
     *   <br> data .tsv files are: col 1)siteName, 2) year, 3+) data columns
     *   <br> site .tsv file is: col 1)island, 2) siteName, 
     *       3) lat(deg. N), 4) lon(deg. E), 5) depth(m)
     *   <br>(lat and lon are just degree and minute info -- as before, at Kushner's
     *     request, no seconds data.
     * <li> In this method, load site table, and process a data file
     *    (convert siteName to Lon, Lat, Depth) and save as .nc file with metadata.
     * </ul>
     *
     * <p>In 2007-03-29 email, David Kushner said in response to my questions:
     * <pre>

I'm working on converting the latest KFM data files to another format
and adding metadata. It's going pretty well. I have a few questions...

***
I presume that I should obscure the exact locations of the stations by
just releasing the degrees and minutes (not seconds) for the lat and lon
values, as before.  If that has changed, please let me know.
      YES, THAT WORKS WELL FOR NOW AND APPRECIATE YOUR FORESIGHT ON THIS
***
Where does the Species number come from?  Is this your internal
numbering system -- a number that you have assigned to each species? Or,
are they standardized numbers from some standard numbering system?
      THEY ARE OUR INTERNAL NUMBER SYSTEM.  THEY DON'T CHANGE BUT THE
SPECIES NAMES DO WAY TOO OFTEN.  I ONLY INCLUDED THESE THINKING THEY MAY BE
USEFUL FOR YOU TO ORGANIZE THE DATA, BUT IF NOT NO WORRIES...  THEY MAY
COME IN HANDY TO HAVE LINKED IF WE UPDATE THE DATA EVERY YEAR AND THE NAMES
CHANGE.  PERHAPS I SHOULD SEND YOU A TABLE THAT HAS THE SPECIES CODE,
SPECIES NAME, AND COMMON NAME AND YOU CAN LINK THEM THIS WAY WHICH IS WHAT
WE DO.  WHATEVER YOU THINK IS BEST.

***
Is the reference information the same as for the temperature data?  If
not, please let me know how you want it to appear. The information
currently is:  YES, THE SAME.  WE ARE WORKING ON THE NPS WEB SITE NOW AND
THOSE LINKS MAY CHANGE, BUT I WILL NOTIFY YOU IF THEY DO.


Channel Islands National Parks Inventory and Monitoring information:
http://www.nature.nps.gov/im/units/chis/
(changing in early 2007 to http://nature.nps.gov/im/units/medn ).
Kelp Forest Monitoring Protocols:
http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf

.

***
Is the "courtesy" information still:
"Channel Islands National Park, National Park Service"?

[I take his no-comment as tacit acceptance.]
***
Should the license/disclaimer still be:
"National Park Service Disclaimer: " +
"The National Park Service shall not be held liable for " +
"improper or incorrect use of the data described and/or contained " +
"herein. These data and related graphics are not legal documents and " +
"are not intended to be used as such. The information contained in " +
"these data is dynamic and may change over time. The data are not " +
"better than the original sources from which they were derived. It is "  +
"the responsibility of the data user to use the data appropriately and " +
"consistent within the limitation of geospatial data in general and " +
"these data in particular. The related graphics are intended to aid " +
"the data user in acquiring relevant data; it is not appropriate to " +
"use the related graphics as data. The National Park Service gives no " +
"warranty, expressed or implied, as to the accuracy, reliability, or " +
"completeness of these data. It is strongly recommended that these " +
"data are directly acquired from an NPS server and not indirectly " +
"through other sources which may have changed the data in some way. " +
"Although these data have been processed successfully on computer " +
"systems at the National Park Service, no warranty expressed or " +
"implied is made regarding the utility of the data on other systems " +
"for general or scientific purposes, nor shall the act of distribution "  +
"constitute any such warranty. This disclaimer applies both to " +
"individual use of the data and aggregate use with other data.";

[I take his no-comment as tacit acceptance.]
</pre>
     * <p>I added the following info to the KFM_Site_Info.tsv file based
     *   on info in emails from Kushner:
     *   <br> San Miguel, Miracle Mile, 34.0166666666667, -120.4, 10
     *   <br>That station didn't have a temperature logger, so there wasn't
     *   an entry in the station file, but there is
     *   biological data for the station.
     * @throws Exception if trouble
     */ 
    public static void kfmBiological() throws Exception {
        String2.log("\n*** Projects.kfmBiological");

        //'_' in tsvNames will be converted to ' ' for tempID below
        //Station .nc files will be stored in subdirectories named tsvName.
        String tsvDir = "c:/programs/kfm/";
        //order not important
        String tsvNames[] = {"KFM_5m_Quadrat", "KFM_Quadrat", "KFM_Random_Point", "KFM_Transect"};

        //read KFM_Site_Info.tsv: col 0)island e.g., "Anacapa", 1=siteID (e.g., Admiral's Reef)", 
        //  2) lat(deg. N), 3) lon(deg. E), 4) depth(m)
        Table site = new Table();
        site.readASCII(tsvDir + "KFM_Site_Info.tsv");
        StringArray sitePa = (StringArray)site.getColumn(1);
        Test.ensureEqual(site.getStringData(0, 0), "San Miguel", "");
        Test.ensureEqual(sitePa.get(0), "Wyckoff Ledge", "");
        Test.ensureEqual(site.getFloatData(2, 0), 34.0166666666667f, ""); //lat, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(3, 0), -120.383333333333f, "");  //lon, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(4, 0), 13, "");   //depth

        //go through the source tab-separated-value files
        StringArray missingSites = new StringArray();
        for (int tsvI = 0; tsvI < tsvNames.length; tsvI++) {
            String tsvName = tsvNames[tsvI];
            String2.log("processing " + tsvDir + tsvName + ".tsv");

            //empty the results directory
            File2.deleteAllFiles(tsvDir + tsvName + "/");

            //read datafile into a table
            //col 0)siteName, e.g., "Admiral's Reef", 1) year 2+) data columns
            Table data = new Table();
            data.readASCII(tsvDir + tsvName + ".tsv"); 
            int dataNRows = data.nRows();
           
            //create x,y,z,t,id columns  (numeric coordinate columns are all doubles)
            DoubleArray xPa = new DoubleArray(dataNRows, false);
            DoubleArray yPa = new DoubleArray(dataNRows, false);
            DoubleArray zPa = new DoubleArray(dataNRows, false);
            DoubleArray tPa = new DoubleArray(dataNRows, false);
            StringArray idPa = new StringArray(dataNRows, false);
            for (int row = 0; row < dataNRows; row++) {
                String tSiteName = data.getStringData(0, row);
                int siteRow = sitePa.indexOf(tSiteName);
                if (siteRow == -1) {
                    int tpo = missingSites.indexOf(tSiteName);
                    if (tpo == -1) missingSites.add(tSiteName);
                    siteRow = 0; //clumsy, but lets me collect all the missing sites
                }

                xPa.add(site.getNiceDoubleData(3, siteRow));
                yPa.add(site.getNiceDoubleData(2, siteRow));
                zPa.add(site.getNiceDoubleData(4, siteRow));
                //they are just year #'s. no time zone issues.
                //Times are vague (may to oct), so assign to July 1 (middle of year).
                String tYear = data.getStringData(1, row);
                Test.ensureEqual(tYear.length(), 4, "Unexpected year=" + tYear + " on row=" + row);
                tPa.add(Calendar2.isoStringToEpochSeconds(tYear + "-07-01"));  
                idPa.add(site.getStringData(0, siteRow) + " (" + tSiteName + ")");
            }

            //put x,y,z,t,id columns in place
            data.removeColumn(0);
            data.addColumn(0, "LON",   xPa, new Attributes());
            data.addColumn(1, "LAT",   yPa, new Attributes());
            data.addColumn(2, "DEPTH", zPa, new Attributes());
            data.addColumn(3, "TIME",  tPa, new Attributes());
            data.addColumn(4, "ID",    idPa, new Attributes());
            data.columnAttributes(4).set("long_name", "Station Identifier");
            //data.columnAttributes(4).set("units", DataHelper.UNITLESS);

            //remove the year column
            Test.ensureEqual(data.getColumnName(5), "Year", "Unexpected col 5 name.");
            data.removeColumn(5);

            //add metadata for data columns
            //standardNames from https://cfconventions.org/standard-names.html
            //none seem relevant here
            for (int col = 5; col < data.nColumns(); col++) {
                String pm2 = " per square meter";  //must be all lowercase
                String colName = data.getColumnName(col);
                if (colName.toLowerCase().endsWith(pm2)) {
                    colName = colName.substring(0, colName.length() - pm2.length());
                    data.setColumnName(col, colName); 
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "m-2");
                } else if (colName.equals("Species")) {
                    data.columnAttributes(col).set("long_name", "Species");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("SpeciesName")) {
                    data.columnAttributes(col).set("long_name", "Species Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("CommonName")) {
                    data.columnAttributes(col).set("long_name", "Common Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Percent Cover")) {
                    data.columnAttributes(col).set("long_name", colName);
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else {
                    Test.error("Unexpected column name=" + colName);
                }
                //data.columnAttributes(col).set("long_name", "Sea Temperature");
                //data.columnAttributes(col).set("standard_name", "sea_water_temperature");
            }

            //summaries are verbatim (except for the first few words) 
            //from c:\content\kushner\NOAA Web page KFM protocol descriptions.doc
            //from Kushner's 2007-04-11 email.
            String summary = null; 
            if (tsvName.equals("KFM_5m_Quadrat")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance of selected rare, clumped, sedentary indicator species. " + 
    "The summary data presented here represents the mean density per square meter. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The original measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else if (tsvName.equals("KFM_Quadrat")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance (density) of relatively abundant selected sedentary indicator species. " + 
    "The summary data presented here represents the mean density per square meter. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The actual measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else if (tsvName.equals("KFM_Random_Point")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has estimates of substrate composition and percent cover of selected algal and invertebrate taxa. " + 
    "The summary data presented here represents the mean percent cover of the indicator species at the site. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The actual measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else if (tsvName.equals("KFM_Transect")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance and distribution of rare and clumped organisms not adequately sampled by quadrats. " + 
    "The summary data presented here represents the mean density per square meter. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The actual measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else Test.error("Unexpected tsvName=" + tsvName);

            //sort by id, x,y,z,t
            data.sort(new int[]{4,0,1,2,3}, new boolean[]{true, true, true, true, true});
            int sppCol = 6;
String2.log("sppCol name = " + data.getColumnName(sppCol));
            int commonCol = 7;
String2.log("commonCol name = " + data.getColumnName(commonCol));
            int dataCol = 8;
String2.log("dataCol name = " + data.getColumnName(dataCol));

            //find unique spp
            IntArray sppIndices = new IntArray();
            StringArray uniqueSpp = (StringArray)data.getColumn(sppCol).makeIndices(sppIndices);
            int nUniqueSpp = uniqueSpp.size();
            for (int sp = 0; sp < nUniqueSpp; sp++) {
                String newName = convertSpeciesName(uniqueSpp.get(sp));
                newName = String2.replaceAll(newName + " " + data.getColumnName(dataCol), ' ', '_');
                uniqueSpp.set(sp, newName);
            }
String2.log("uniqueSpp = " + uniqueSpp);

            //find unique years
            IntArray yearIndices = new IntArray();
            PrimitiveArray uniqueYear = data.getColumn(3).makeIndices(yearIndices);
            int nUniqueYear = uniqueYear.size();
String2.log("uniqueYear = " + uniqueYear);
            
            //make a separate file for each station
            int startRow = 0;
            for (int row = 1; row <= dataNRows; row++) {  //yes 1..=
                //id changed?
                if (row == dataNRows || //test this first
                    !data.getStringData(4, startRow).equals(data.getStringData(4, row))) {
                    
                    //make stationTable x,y,z(constant), t, col for each sp
                    Table stationTable = new Table();
                    data.globalAttributes().copyTo(stationTable.globalAttributes());
                    for (int col = 0; col < 5; col++) {
                        stationTable.addColumn(col, data.getColumnName(col), 
                            PrimitiveArray.factory(data.getColumn(col).elementType(), dataCol, false), 
                            (Attributes)data.columnAttributes(col).clone());
                    }
                    for (int col = 0; col < nUniqueSpp; col++) {
                        stationTable.addColumn(5 + col, uniqueSpp.get(col), 
                            new FloatArray(), 
                            (Attributes)data.columnAttributes(dataCol).clone());
                        stationTable.columnAttributes(5 + col).set("long_name",
                            String2.replaceAll(uniqueSpp.get(col), '_', ' '));
                        int rowWithThisSpp = sppIndices.indexOf("" + col);
                        stationTable.columnAttributes(5 + col).set("comment",
                            "Common name: " + data.getStringData(commonCol, rowWithThisSpp));
                    }

                    //fill the stationTable with axis info and blanks
                    for (int tRow = 0; tRow < nUniqueYear; tRow++) {
                        //x,y,z,t,id
                        stationTable.getColumn(0).addDouble(data.getDoubleData(0, startRow));
                        stationTable.getColumn(1).addDouble(data.getDoubleData(1, startRow));
                        stationTable.getColumn(2).addDouble(data.getDoubleData(2, startRow));
                        stationTable.getColumn(3).addDouble(uniqueYear.getDouble(tRow));
                        stationTable.getColumn(4).addString(data.getStringData(4, startRow));
                        //spp
                        for (int col = 0; col < nUniqueSpp; col++) 
                            stationTable.getColumn(5 + col).addFloat(Float.NaN);
                    }

                    //fill the stationTable with data
                    for (int tRow = startRow; tRow < row; tRow++) {
                        float d = data.getFloatData(dataCol, tRow);
                        if (d < -9000)
                            Test.error("d=" + d + " is < -9000.");
                        stationTable.setFloatData(5 + sppIndices.get(tRow), 
                            yearIndices.get(tRow), d);
                    }

                    //setAttributes
                    String id = data.getStringData(4, startRow); //e.g., "San Miguel (Wyckoff Ledge)"
                    int pPo = id.indexOf('(');
                    Test.ensureNotEqual(pPo, -1, "'(' in id=" + id);
                    String island = id.substring(0, pPo - 1);
                    String station = id.substring(pPo + 1, id.length() - 1);
                    stationTable.setAttributes(0, 1, 2, 3, //x,y,z,t
                        String2.replaceAll(tsvName, '_', ' ') + " (Channel Islands)", //bold title
                            //don't make specific to this station; when aggregated, just one boldTitle will be used
                            //", " + island + ", " + station + ")", 
                        "Station", //cdmDataType
                        DataHelper.ERD_CREATOR_EMAIL,
                        DataHelper.ERD_CREATOR_NAME, 
                        DataHelper.ERD_CREATOR_URL,   
                        DataHelper.ERD_PROJECT,       
                        tsvName, //id    //don't make specific to this station; when aggregated, just one id will be used
                        "GCMD Science Keywords", //keywordsVocabulary,
                        //see http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
                        //there are plands and invertebrates, so habitat seems closest keyword
                        "Oceans > Marine Biology > Marine Habitat", //keywords

                        //references   from 2006-12-19 email from Kushner
                        "Channel Islands National Parks Inventory and Monitoring information: " +
                            "http://nature.nps.gov/im/units/medn . " +
                            "Kelp Forest Monitoring Protocols: " +
                            "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                        //summary  from 2006-12-19 email from Kushner
                        summary,
                        //my old summary
                        //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                        //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                        //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                        //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                        //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                        "Channel Islands National Park, National Park Service", 
                        null); //timeLongName     use default long name

                    //add the National Park Service disclaimer from 2006-12-19 email
                    String license = stationTable.globalAttributes().getString("license") +
                        "  National Park Service Disclaimer: " +
                        "The National Park Service shall not be held liable for " +
                        "improper or incorrect use of the data described and/or contained " +
                        "herein. These data and related graphics are not legal documents and " +
                        "are not intended to be used as such. The information contained in " +
                        "these data is dynamic and may change over time. The data are not " +
                        "better than the original sources from which they were derived. It is " +
                        "the responsibility of the data user to use the data appropriately and " +
                        "consistent within the limitation of geospatial data in general and " +
                        "these data in particular. The related graphics are intended to aid " +
                        "the data user in acquiring relevant data; it is not appropriate to " +
                        "use the related graphics as data. The National Park Service gives no " +
                        "warranty, expressed or implied, as to the accuracy, reliability, or " +
                        "completeness of these data. It is strongly recommended that these " +
                        "data are directly acquired from an NPS server and not indirectly " +
                        "through other sources which may have changed the data in some way. " +
                        "Although these data have been processed successfully on computer " +
                        "systems at the National Park Service, no warranty expressed or " +
                        "implied is made regarding the utility of the data on other systems " +
                        "for general or scientific purposes, nor shall the act of distribution " +
                        "constitute any such warranty. This disclaimer applies both to " +
                        "individual use of the data and aggregate use with other data.";
                    stationTable.globalAttributes().set("license", license);

                    //fix up the attributes
                    stationTable.globalAttributes().set("acknowledgement",
                        stationTable.globalAttributes().getString("acknowledgement") + ", " +
                        "Channel Islands National Park, National Park Service");

                    //review the table
                    if (tsvI == 0 && (startRow == 0 || row == dataNRows)) {
                        String2.log(stationTable.toString(100));
                        String2.pressEnterToContinue("Check if the file (above) is ok, then...");
                    }
                    String2.log("  startRow=" + startRow + " end=" + (row-1) + " island=" + island + " station=" + station); 

                    //save the data table    
                    String tFileName = tsvDir + tsvName + "/" + tsvName + "_" + 
                        String2.replaceAll(island, " ", "") + "_" + 
                        String2.replaceAll(station, " ", "") + ".nc";
                    tFileName = String2.replaceAll(tFileName, ' ', '_');
                    tFileName = String2.replaceAll(tFileName, "'", "");
                    stationTable.saveAs4DNcWithStringVariable(tFileName,0,1,2,3,4); 

                    startRow = row;
                }
            }
        }
        if (missingSites.size() > 0) {
            String2.log("\n*** Projects.kfmBiological FAILED. Missing sites=" + missingSites);
        } else {
            String2.log("\n*** Projects.kfmBiological finished successfully.");
        }
    }

    /**
     * This is the 4th processing (Jan 2008) of data from Kushner/KFM project.
     *
     * <p>This year I didn't convert .tab to .tsv in CoStat and rearrange columns.
     * I just rearranged the columns below.
     *
     * <p>In KFM1mQuadrat.tab line 2158, I replaced the missing stdDev and stdErr with NaNs.
     * <br>and line 3719: 8	Santa Cruz	Pelican Bay	11007.00	Parastichopus parvimensis	Warty sea cucumber	1983	NaN	NaN	NaN
     *
     * I will put the results in otter /u00/bob/kfm2008/ which mimics my f:/programs/kfm200801 .
     */
    public static void kfmBiological200801() throws Exception {
        String2.log("\n*** Projects.kfmBiological200801");

        //'_' in tabNames will be converted to ' ' for tempID below
        //Station .nc files will be stored in subdirectories named tabName.
        String tabDir = "c:/programs/kfm200801/";
        //order not important
        String tabNames[] = {"KFM5mQuadrat", "KFM1mQuadrat", "KFMRandomPointContact", "KFMBandTransect"};
        String titles[] = {"Survey, 5m Quadrat", "Survey, 1m Quadrat", "Survey, Random Point Contact", "Survey, Band Transect"};

        //read KFM_Site_Info.tsv: col 0)island e.g., "Anacapa", 1=siteID (e.g., Admiral's Reef)", 
        //  2) lat(deg. N), 3) lon(deg. E), 4) depth(m)
        Table site = new Table();
        site.readASCII(tabDir + "KFM_Site_Info.tsv");    //read siteInfo
        StringArray sitePa = (StringArray)site.getColumn(1);
        Test.ensureEqual(site.getStringData(0, 0), "San Miguel", "");
        Test.ensureEqual(sitePa.get(0), "Wyckoff Ledge", "");
        Test.ensureEqual(site.getFloatData(2, 0), 34.0166666666667f, ""); //lat, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(3, 0), -120.383333333333f, "");  //lon, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(4, 0), 13, "");   //depth

        //go through the source tab-separated-value files
        StringArray missingSites = new StringArray();
//        for (int tabI = 1; tabI <= 1; tabI++) {
        for (int tabI = 0; tabI < tabNames.length; tabI++) {
            String tabName = tabNames[tabI];
            String title = titles[tabI];
            String2.log("processing " + tabDir + tabName + ".tab");

            //empty the results directory
            File2.deleteAllFiles(tabDir + tabName + "/");

            //read datafile into a table
            //initially 0=SiteNumber 1=Island 2=SiteName 3=Species 4=SpeciesName
            //5=CommonName 6=Year 7=...Per Square Meter [8=StdDev] [9=Std Error]
            Table data = new Table();
            data.readASCII(tabDir + tabName + ".tab"); 
            int dataNRows = data.nRows();
           
            //create x,y,z,t,id columns  (numeric coordinate columns are all doubles)
            DoubleArray xPa = new DoubleArray(dataNRows, false);
            DoubleArray yPa = new DoubleArray(dataNRows, false);
            DoubleArray zPa = new DoubleArray(dataNRows, false);
            DoubleArray tPa = new DoubleArray(dataNRows, false);
            StringArray idPa = new StringArray(dataNRows, false);
            for (int row = 0; row < dataNRows; row++) {
                String tSiteName = data.getStringData(2, row);
                int siteRow = sitePa.indexOf(tSiteName);
                if (siteRow == -1) {
                    int tpo = missingSites.indexOf(tSiteName);
                    if (tpo == -1) missingSites.add(tSiteName);
                    siteRow = 0; //clumsy, but lets me collect all the missing sites
                }

                xPa.add(site.getNiceDoubleData(3, siteRow));
                yPa.add(site.getNiceDoubleData(2, siteRow));
                zPa.add(site.getNiceDoubleData(4, siteRow));
                //they are just year #'s. no time zone issues.
                //Times are vague (may to oct), so assign to July 1 (middle of year).
                String tYear = data.getStringData(6, row);
                Test.ensureEqual(tYear.length(), 4, "Unexpected year=" + tYear + " on row=" + row);
                tPa.add(Calendar2.isoStringToEpochSeconds(tYear + "-07-01"));  
                idPa.add(site.getStringData(0, siteRow) + " (" + tSiteName + ")");
            }

            //put x,y,z,t,id columns in place
            data.removeColumn(6); //Year
            data.removeColumn(2); //SiteName
            data.removeColumn(1); //Island
            data.removeColumn(0); //SiteNumber
            data.addColumn(0, "LON",   xPa, new Attributes());
            data.addColumn(1, "LAT",   yPa, new Attributes());
            data.addColumn(2, "DEPTH", zPa, new Attributes());
            data.addColumn(3, "TIME",  tPa, new Attributes());
            data.addColumn(4, "ID",    idPa, new Attributes());
            data.columnAttributes(4).set("long_name", "Station Identifier");
            //data.columnAttributes(4).set("units", DataHelper.UNITLESS);
            Test.ensureEqual(data.getColumnName(5), "Species", "");
            Test.ensureEqual(data.getColumnName(6), "SpeciesName", "");
            Test.ensureEqual(data.getColumnName(7), "CommonName", "");
            String newDataName = null, newDataUnits = null;
            String ts = data.getColumnName(8);
            if (ts.endsWith(" Per Square Meter")) {
                newDataName = ts.substring(0, ts.length() - " Per Square Meter".length());
                newDataUnits = "m-2";
            } else if (ts.endsWith(" Percent Cover")) {
                newDataName = ts.substring(0, ts.length() - " Percent Cover".length());
                newDataUnits = "percent cover";
            } else 
                Test.error(ts);
            Test.ensureEqual(data.getColumnName(9), "StdDev", "");
            Test.ensureEqual(data.getColumnName(10), "Std Error", "");
            int sppCol = 6;       
            int commonCol = 7;    
            int dataCol = 8;      
            int sdCol = 9;        
            int seCol = 10;       
            String2.log("newDataName=" + newDataName + " newDataUnits=" + newDataUnits);

            //add metadata for data columns
            //standardNames from https://cfconventions.org/standard-names.html
            //none seem relevant here
            for (int col = 5; col < data.nColumns(); col++) {
                String pm2 = " per square meter";  //must be all lowercase
                String colName = data.getColumnName(col);
                data.setColumnName(col, colName);
                if (colName.toLowerCase().endsWith(pm2)) {
                    colName = colName.substring(0, colName.length() - pm2.length());
                    data.setColumnName(col, colName); 
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "m-2");
                } else if (colName.equals("Species")) {
                    data.columnAttributes(col).set("long_name", "Species");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("SpeciesName")) {
                    data.columnAttributes(col).set("long_name", "Species Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("CommonName")) {
                    data.columnAttributes(col).set("long_name", "Common Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Mean Percent Cover")) {
                    data.setColumnName(col, "Mean");
                    data.columnAttributes(col).set("long_name", "Mean");
                    data.columnAttributes(col).set("units", "percent cover");
                } else if (colName.equals("StdDev")) {
                    data.columnAttributes(col).set("long_name", "Standard Deviation");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Std Error")) {
                    data.setColumnName(col, "StdErr");
                    data.columnAttributes(col).set("long_name", "Standard Error");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else {
                    Test.error("Unexpected column name=" + colName);
                }
            }

            //summaries are verbatim (except for the first few words) 
            //from c:\content\kushner\NOAA Web page KFM protocol descriptions.doc
            //from Kushner's 2007-04-11 email.
            String summary = null; 
            if (tabName.equals("KFM5mQuadrat")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance of selected rare, clumped, sedentary indicator species. " + 
    "The summary data presented here represents the mean density per square meter. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The original measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else if (tabName.equals("KFM1mQuadrat")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance (density) of relatively abundant selected sedentary indicator species. " + 
    "The summary data presented here represents the mean density per square meter. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The actual measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else if (tabName.equals("KFMRandomPointContact")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has estimates of substrate composition and percent cover of selected algal and invertebrate taxa. " + 
    "The summary data presented here represents the mean percent cover of the indicator species at the site. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The actual measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else if (tabName.equals("KFMBandTransect")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance and distribution of rare and clumped organisms not adequately sampled by quadrats. " + 
    "The summary data presented here represents the mean density per square meter. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The actual measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else Test.error("Unexpected tabName=" + tabName);

            //sort by id, x,y,z,t
            data.sort(new int[]{4,0,1,2,3}, new boolean[]{true, true, true, true, true});

            //find unique spp
            IntArray sppIndices = new IntArray();
            StringArray uniqueSpp = (StringArray)data.getColumn(sppCol).makeIndices(sppIndices);
            int nUniqueSpp = uniqueSpp.size();
            for (int sp = 0; sp < nUniqueSpp; sp++) 
                uniqueSpp.set(sp, convertSpeciesName(uniqueSpp.get(sp)));
String2.log("uniqueSpp = " + uniqueSpp);

            //find unique years
            IntArray yearIndices = new IntArray();
            PrimitiveArray uniqueYear = data.getColumn(3).makeIndices(yearIndices);
            int nUniqueYear = uniqueYear.size();
String2.log("uniqueYear = " + uniqueYear);
            
            //make a separate file for each station
            int startRow = 0;
            for (int row = 1; row <= dataNRows; row++) {  //yes 1..=
                //id changed?
                if (row == dataNRows || //test this first
                    !data.getStringData(4, startRow).equals(data.getStringData(4, row))) {
                    
                    //make stationTable x,y,z(constant), t, col for each sp
                    Table stationTable = new Table();
                    data.globalAttributes().copyTo(stationTable.globalAttributes());
                    for (int col = 0; col < 5; col++) {
                        stationTable.addColumn(col, data.getColumnName(col), 
                            PrimitiveArray.factory(data.getColumn(col).elementType(), dataCol, false), 
                            (Attributes)data.columnAttributes(col).clone());
                    }
                    //add data columns
                    for (int col = 0; col < nUniqueSpp; col++) {

                        String ulDataName = String2.replaceAll(newDataName, ' ', '_');
                        stationTable.addColumn(5 + col * 3 + 0, uniqueSpp.get(col) + "_" + ulDataName, 
                            new FloatArray(), 
                            (Attributes)data.columnAttributes(dataCol).clone());
                        stationTable.addColumn(5 + col * 3 + 1, uniqueSpp.get(col) + "_StdDev", 
                            new FloatArray(), 
                            (Attributes)data.columnAttributes(sdCol).clone());
                        stationTable.addColumn(5 + col * 3 + 2, uniqueSpp.get(col) + "_StdErr", 
                            new FloatArray(), 
                            (Attributes)data.columnAttributes(seCol).clone());

                        stationTable.columnAttributes(5 + col * 3 + 0).set("long_name",
                            String2.replaceAll(uniqueSpp.get(col) + "_" + newDataName, '_', ' '));
                        stationTable.columnAttributes(5 + col * 3 + 1).set("long_name",
                            String2.replaceAll(uniqueSpp.get(col) + "_StdDev", '_', ' '));
                        stationTable.columnAttributes(5 + col * 3 + 2).set("long_name",
                            String2.replaceAll(uniqueSpp.get(col) + "_StdErr", '_', ' '));

                        int rowWithThisSpp = sppIndices.indexOf("" + col);
                        stationTable.columnAttributes(5 + col * 3 + 0).set("comment",
                            "Common name: " + data.getStringData(commonCol, rowWithThisSpp));

                    }

                    //fill the stationTable with axis info and blanks
                    for (int tRow = 0; tRow < nUniqueYear; tRow++) {
                        //x,y,z,t,id
                        stationTable.getColumn(0).addDouble(data.getDoubleData(0, startRow));
                        stationTable.getColumn(1).addDouble(data.getDoubleData(1, startRow));
                        stationTable.getColumn(2).addDouble(data.getDoubleData(2, startRow));
                        stationTable.getColumn(3).addDouble(uniqueYear.getDouble(tRow));
                        stationTable.getColumn(4).addString(data.getStringData(4, startRow));
                        //spp
                        for (int col = 0; col < nUniqueSpp; col++) {
                            stationTable.getColumn(5 + col * 3 + 0).addFloat(Float.NaN);
                            stationTable.getColumn(5 + col * 3 + 1).addFloat(Float.NaN);
                            stationTable.getColumn(5 + col * 3 + 2).addFloat(Float.NaN);
                        }
                    }

                    //fill the stationTable with data
                    for (int tRow = startRow; tRow < row; tRow++) {
                        float tm = data.getFloatData(dataCol, tRow);
                        float td = data.getFloatData(sdCol, tRow);
                        float te = data.getFloatData(seCol, tRow);
                        if (tm < -9000)
                            Test.error("tm=" + tm + " is < -9000.");
                        stationTable.setFloatData(5 + sppIndices.get(tRow) * 3 + 0, yearIndices.get(tRow), tm);
                        stationTable.setFloatData(5 + sppIndices.get(tRow) * 3 + 1, yearIndices.get(tRow), td);
                        stationTable.setFloatData(5 + sppIndices.get(tRow) * 3 + 2, yearIndices.get(tRow), te);
                    }

                    //setAttributes
                    String id = data.getStringData(4, startRow); //e.g., "San Miguel (Wyckoff Ledge)"
                    int pPo = id.indexOf('(');
                    Test.ensureNotEqual(pPo, -1, "'(' in id=" + id);
                    String island = id.substring(0, pPo - 1);
                    String station = id.substring(pPo + 1, id.length() - 1);
                    stationTable.setAttributes(0, 1, 2, 3, //x,y,z,t
                        title + " (Kelp Forest Monitoring, Channel Islands)", //bold title
                            //don't make specific to this station; when aggregated, just one boldTitle will be used
                            //", " + island + ", " + station + ")", 
                        "Station", //cdmDataType
                        DataHelper.ERD_CREATOR_EMAIL,
                        DataHelper.ERD_CREATOR_NAME, 
                        DataHelper.ERD_CREATOR_URL,   
                        DataHelper.ERD_PROJECT,       
                        tabName, //id    //don't make specific to this station; when aggregated, just one id will be used
                        "GCMD Science Keywords", //keywordsVocabulary,
                        //see http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
                        //there are plands and invertebrates, so habitat seems closest keyword
                        "Oceans > Marine Biology > Marine Habitat", //keywords

                        //references   from 2006-12-19 email from Kushner
                        "Channel Islands National Parks Inventory and Monitoring information: " +
                            "http://nature.nps.gov/im/units/medn . " +
                            "Kelp Forest Monitoring Protocols: " +
                            "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                        //summary  from 2006-12-19 email from Kushner
                        summary,
                        //my old summary
                        //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                        //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                        //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                        //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                        //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                        "Channel Islands National Park, National Park Service", 
                        null); //timeLongName     use default long name

                    //add the National Park Service disclaimer from 2006-12-19 email
                    String license = stationTable.globalAttributes().getString("license") +
                        "  National Park Service Disclaimer: " +
                        "The National Park Service shall not be held liable for " +
                        "improper or incorrect use of the data described and/or contained " +
                        "herein. These data and related graphics are not legal documents and " +
                        "are not intended to be used as such. The information contained in " +
                        "these data is dynamic and may change over time. The data are not " +
                        "better than the original sources from which they were derived. It is " +
                        "the responsibility of the data user to use the data appropriately and " +
                        "consistent within the limitation of geospatial data in general and " +
                        "these data in particular. The related graphics are intended to aid " +
                        "the data user in acquiring relevant data; it is not appropriate to " +
                        "use the related graphics as data. The National Park Service gives no " +
                        "warranty, expressed or implied, as to the accuracy, reliability, or " +
                        "completeness of these data. It is strongly recommended that these " +
                        "data are directly acquired from an NPS server and not indirectly " +
                        "through other sources which may have changed the data in some way. " +
                        "Although these data have been processed successfully on computer " +
                        "systems at the National Park Service, no warranty expressed or " +
                        "implied is made regarding the utility of the data on other systems " +
                        "for general or scientific purposes, nor shall the act of distribution " +
                        "constitute any such warranty. This disclaimer applies both to " +
                        "individual use of the data and aggregate use with other data.";
                    stationTable.globalAttributes().set("license", license);

                    //fix up the attributes
                    stationTable.globalAttributes().set("acknowledgement",
                        stationTable.globalAttributes().getString("acknowledgement") + ", " +
                        "Channel Islands National Park, National Park Service");

                    //review the table
                    if (tabI == 0 && (startRow == 0 || row == dataNRows)) {
                        String2.log(stationTable.toString(100));
                        String2.pressEnterToContinue("Check if the file (above) is ok, then...");
                    }
                    String2.log("  startRow=" + startRow + " end=" + (row-1) + " island=" + island + " station=" + station); 

                    //save the data table    
                    String tFileName = tabDir + tabName + "/" + tabName + "_" + 
                        String2.replaceAll(island, " ", "") + "_" + 
                        String2.replaceAll(station, " ", "") + ".nc";
                    tFileName = String2.replaceAll(tFileName, ' ', '_');
                    tFileName = String2.replaceAll(tFileName, "'", "");

                    //change ><= in columnNames to GT LT EQ
                    for (int col = 0; col < stationTable.nColumns(); col++) {
                        String colName = stationTable.getColumnName(col);
                        colName = String2.replaceAll(colName, ">", "GT");
                        colName = String2.replaceAll(colName, "<", "LT");
                        colName = String2.replaceAll(colName, "=", "EQ");
                        stationTable.setColumnName(col, colName);
                    }
                    
                    stationTable.saveAs4DNcWithStringVariable(tFileName,0,1,2,3,4); 

                    startRow = row;
                }
            }
        }
        if (missingSites.size() > 0) {
            String2.log("\n*** Projects.kfmBiological200801 FAILED. Missing sites=" + missingSites);
        } else {
            String2.log("\n*** Projects.kfmBiological200801 finished successfully.");
        }
    }

    /**
     * This is the 4th processing (Jan 2008) of data from Kushner/KFM project.
     *
     * I will put the results in otter /u00/bob/kfm2008/ which mimics my f:/programs/kfm200801 .
     */
    public static void kfmFishTransect200801() throws Exception {
        String2.log("\n*** Projects.kfmFishTransect200801");

        //Station .nc files will be stored in subdirectories named tabName.
        String tabDir = "c:/programs/kfm200801/";
        //order not important
        String tabNames[] = {"KFMFishTransect"};
        String boldTitles[] = {"Fish Survey, Transect"};

        //read KFM_Site_Info.tsv: col 0)island e.g., "Anacapa", 1=siteID (e.g., Admiral's Reef)", 
        //  2) lat(deg. N), 3) lon(deg. E), 4) depth(m)
        Table site = new Table();
        site.readASCII(tabDir + "KFM_Site_Info.tsv");    //read siteInfo
        StringArray sitePa = (StringArray)site.getColumn(1);
        Test.ensureEqual(site.getStringData(0, 0), "San Miguel", "");
        Test.ensureEqual(sitePa.get(0), "Wyckoff Ledge", "");
        Test.ensureEqual(site.getFloatData(2, 0), 34.0166666666667f, ""); //lat, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(3, 0), -120.383333333333f, "");  //lon, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(4, 0), 13, "");   //depth

        //go through the source tab-separated-value files
        StringArray missingSites = new StringArray();
        for (int tabI = 0; tabI < tabNames.length; tabI++) {
            String tabName = tabNames[tabI];
            String2.log("processing " + tabDir + tabName + ".tab");

            //empty the results directory
            File2.deleteAllFiles(tabDir + tabName + "/");

            //read datafile into a table
            Table data = new Table();
            data.readASCII(tabDir + tabName + ".tab"); 
            int dataNRows = data.nRows();
            Test.ensureEqual(
                String2.toCSSVString(data.getColumnNames()),
                "Year, IslandName, SiteName, Date, " +                            //0,1,2,3
                    "Species, Species Name, Adult/Juvenile/sex, " +               //4,5,6
                    "CommonName, Transect, Number fish per 100mX2mX30m transect", //7,8,9
                "");
          
            //create x,y,z,t,id columns  (numeric coordinate columns are all doubles)
            DoubleArray xPa = new DoubleArray(dataNRows, false);
            DoubleArray yPa = new DoubleArray(dataNRows, false);
            DoubleArray zPa = new DoubleArray(dataNRows, false);
            DoubleArray tPa = new DoubleArray(dataNRows, false);
            StringArray idPa = new StringArray(dataNRows, false);
            for (int row = 0; row < dataNRows; row++) {
                String tSiteName = data.getStringData(2, row);
                int siteRow = sitePa.indexOf(tSiteName);
                if (siteRow == -1) {
                    int tpo = missingSites.indexOf(tSiteName);
                    if (tpo == -1) missingSites.add(tSiteName);
                    siteRow = 0; //clumsy, but lets me collect all the missing sites
                }

                xPa.add(site.getNiceDoubleData(3, siteRow));
                yPa.add(site.getNiceDoubleData(2, siteRow));
                zPa.add(site.getNiceDoubleData(4, siteRow));
                double sec = Calendar2.gcToEpochSeconds(
                    Calendar2.parseUSSlash24Zulu(data.getStringData(3, row))); //throws Exception
                if (sec < 100 || sec > 2e9)
                    String2.log("row=" + row + " sec=" + sec + " Unexpected time=" + data.getStringData(3, row));
                tPa.add(sec);
                idPa.add(site.getStringData(0, siteRow) + " (" + tSiteName + ")");

                //combine SpeciesName, Adult/Juvenile/sex
                String tsp = data.getStringData(5, row);
                int sp2Po = tsp.indexOf("  "); //precedes description I don't keep
                if (sp2Po > 0) tsp = tsp.substring(0, sp2Po);
                int pPo = tsp.indexOf("("); //precedes description I don't keep
                if (pPo > 0)   tsp = tsp.substring(0, pPo); 
                tsp += "_" + data.getStringData(6, row); 
                tsp = String2.replaceAll(tsp, ".", "");
                tsp = String2.replaceAll(tsp, ",", "");
                tsp = String2.replaceAll(tsp, ' ', '_');
                data.setStringData(5, row, tsp);

                //ensure transect always 1
                Test.ensureEqual(data.getDoubleData(8, row), 1, "");
            }

            //put x,y,z,t,id columns in place
            //    "Year, IslandName, SiteName, Date, " +                        //0,1,2,3
            //    "Species, Species Name, Adult/Juvenile/sex, " +               //4,5,6
            //    "CommonName, Transect, Number fish per 100mX2mX30m transect", //7,8,9
            data.removeColumn(8); //Transect (always 1)
            data.removeColumn(6); //Adult
            data.removeColumn(4); //Species
            data.removeColumn(3); //Date
            data.removeColumn(2); //SiteName
            data.removeColumn(1); //Island
            data.removeColumn(0); //Year
            data.addColumn(0, "LON",   xPa, new Attributes());
            data.addColumn(1, "LAT",   yPa, new Attributes());
            data.addColumn(2, "DEPTH", zPa, new Attributes());
            data.addColumn(3, "TIME",  tPa, new Attributes());
            data.addColumn(4, "ID",    idPa, new Attributes());
            data.columnAttributes(4).set("long_name", "Station Identifier");
            //data.columnAttributes(4).set("units", DataHelper.UNITLESS);
            //now
            //    "LON, LAT, DEPTH, TIME, ID, " +                     //0,1,2,3,4
            //    "Species Name + Adult/Juvenile/sex, " +             //5
            //    CommonName, Number fish per 100mX2mX30m transect",  //6,7

            //add metadata for data columns
            //standardNames from https://cfconventions.org/standard-names.html
            //none seem relevant here
//Year	IslandName	SiteName	Date	Species	Species Name	Adult/Juvenile/sex	CommonName	Transect	Number fish per 100mX2mX30m transect
//1985	Anacapa	Admiral's Reef	8/30/1985 0:00:00	14001.00	Chromis punctipinnis	 Adult	Blacksmith Adult	1	224
            for (int col = 5; col < data.nColumns(); col++) {
                String colName = data.getColumnName(col);
                if (colName.equals("Species Name")) {
                    data.setColumnName(col, "SpeciesName");
                    data.columnAttributes(col).set("long_name", "Species Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("CommonName")) {
                    //no need for metadata; just used as comment
                } else if (colName.equals("Number fish per 100mX2mX30m transect")) {
                    data.setColumnName(col, "NumberOfFish");
                    data.columnAttributes(col).set("long_name", "Number of fish per 100mX2mX30m transect");
                    data.columnAttributes(col).set("units", "per 100mX2mX30m transect");
                } else {
                    Test.error("Unexpected column name=" + colName);
                }
            }

            //summaries are verbatim (except for the first few words) 
            //from c:\content\kushner\NOAA Web page KFM protocol descriptions.doc
            //from Kushner's 2007-04-11 email.
            String summary = null; 
            if (tabName.equals("KFMFishTransect")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the abundance of fish species. " + 
    "The original measurements were taken at various depths, " +
    "so the Depth data in this file is the depth of the station's temperature logger, which is a typical depth." ;
            else Test.error("Unexpected tabName=" + tabName);

            //sort by id, x,y,z,t
            data.sort(new int[]{4,0,1,2,3}, new boolean[]{true, true, true, true, true});
            int sppCol = 5;
String2.log("sppCol name = " + data.getColumnName(sppCol));
            int comCol = 6;
String2.log("comCol name = " + data.getColumnName(comCol));
            int dataCol = 7;
String2.log("dataCol name = " + data.getColumnName(dataCol));

            //find unique spp
            IntArray sppIndices = new IntArray();
            StringArray uniqueSpp = (StringArray)data.getColumn(sppCol).makeIndices(sppIndices);
            int nUniqueSpp = uniqueSpp.size();
String2.log("uniqueSpp = " + uniqueSpp);
            
            //make a separate file for each station
            int startRow = 0;
            DoubleArray uniqueTimes = new DoubleArray();
            for (int row = 0; row <= dataNRows; row++) {  //yes 0..=
                //id changed?
                if (row == dataNRows || //test this first
                    !data.getStringData(4, startRow).equals(data.getStringData(4, row))) {
                    
                    //make stationTable x,y,z(constant), t, col for each sp
                    Table stationTable = new Table();
                    data.globalAttributes().copyTo(stationTable.globalAttributes());
                    for (int col = 0; col < 5; col++) {
                        stationTable.addColumn(col, data.getColumnName(col), 
                            PrimitiveArray.factory(data.getColumn(col).elementType(), dataCol, false), 
                            (Attributes)data.columnAttributes(col).clone());
                    }
                    for (int col = 0; col < nUniqueSpp; col++) {
                        stationTable.addColumn(5 + col, uniqueSpp.get(col), 
                            new IntArray(), 
                            (Attributes)data.columnAttributes(dataCol).clone());
                        stationTable.columnAttributes(5 + col).set("long_name",
                            "Number of " + String2.replaceAll(uniqueSpp.get(col), '_', ' '));
                        int rowWithThisSpp = sppIndices.indexOf("" + col);
                        stationTable.columnAttributes(5 + col).set("comment",
                            "Common name: " + data.getStringData(comCol, rowWithThisSpp));
                    }

                    //fill the stationTable with axis info and blanks
                    int nUniqueTimes = uniqueTimes.size();
                    uniqueTimes.sort();
                    for (int tRow = 0; tRow < nUniqueTimes; tRow++) {
                        //x,y,z,t,id
                        stationTable.getColumn(0).addDouble(data.getDoubleData(0, startRow));
                        stationTable.getColumn(1).addDouble(data.getDoubleData(1, startRow));
                        stationTable.getColumn(2).addDouble(data.getDoubleData(2, startRow));
                        stationTable.getColumn(3).addDouble(uniqueTimes.get(tRow));
                        stationTable.getColumn(4).addString(data.getStringData(4, startRow));
                        //spp
                        for (int col = 0; col < nUniqueSpp; col++) 
                            stationTable.getColumn(5 + col).addInt(Integer.MAX_VALUE);
                    }

                    //fill the stationTable with data
                    for (int tRow = startRow; tRow < row; tRow++) {
                        int stationRow = uniqueTimes.indexOf(tPa.get(tRow), 0);
                        int d = data.getIntData(dataCol, tRow);
                        if (d < 0)
                            Test.error("d=" + d + " is < 0.");
                        stationTable.setIntData(5 + sppIndices.get(tRow), stationRow, d);
                    }

                    //setAttributes
                    String id = data.getStringData(4, startRow); //e.g., "San Miguel (Wyckoff Ledge)"
                    int pPo = id.indexOf('(');
                    Test.ensureNotEqual(pPo, -1, "'(' in id=" + id);
                    String island = id.substring(0, pPo - 1);
                    String station = id.substring(pPo + 1, id.length() - 1);
                    stationTable.setAttributes(0, 1, 2, 3, //x,y,z,t
                        boldTitles[tabI] + " (Channel Islands)", //bold title
                            //don't make specific to this station; when aggregated, just one boldTitle will be used
                            //", " + island + ", " + station + ")", 
                        "Station", //cdmDataType
                        DataHelper.ERD_CREATOR_EMAIL,
                        DataHelper.ERD_CREATOR_NAME, 
                        DataHelper.ERD_CREATOR_URL,   
                        DataHelper.ERD_PROJECT,       
                        tabName, //id    //don't make specific to this station; when aggregated, just one id will be used
                        "GCMD Science Keywords", //keywordsVocabulary,
                        //see http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
                        //there are plands and invertebrates, so habitat seems closest keyword
                        "Oceans > Marine Biology > Marine Habitat", //keywords

                        //references   from 2006-12-19 email from Kushner
                        "Channel Islands National Parks Inventory and Monitoring information: " +
                            "http://nature.nps.gov/im/units/medn . " +
                            "Kelp Forest Monitoring Protocols: " +
                            "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                        //summary  from 2006-12-19 email from Kushner
                        summary,
                        //my old summary
                        //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                        //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                        //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                        //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                        //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                        "Channel Islands National Park, National Park Service", 
                        null); //timeLongName     use default long name
 
                    //add the National Park Service disclaimer from 2006-12-19 email
                    String license = stationTable.globalAttributes().getString("license") +
                        "  National Park Service Disclaimer: " +
                        "The National Park Service shall not be held liable for " +
                        "improper or incorrect use of the data described and/or contained " +
                        "herein. These data and related graphics are not legal documents and " +
                        "are not intended to be used as such. The information contained in " +
                        "these data is dynamic and may change over time. The data are not " +
                        "better than the original sources from which they were derived. It is " +
                        "the responsibility of the data user to use the data appropriately and " +
                        "consistent within the limitation of geospatial data in general and " +
                        "these data in particular. The related graphics are intended to aid " +
                        "the data user in acquiring relevant data; it is not appropriate to " +
                        "use the related graphics as data. The National Park Service gives no " +
                        "warranty, expressed or implied, as to the accuracy, reliability, or " +
                        "completeness of these data. It is strongly recommended that these " +
                        "data are directly acquired from an NPS server and not indirectly " +
                        "through other sources which may have changed the data in some way. " +
                        "Although these data have been processed successfully on computer " +
                        "systems at the National Park Service, no warranty expressed or " +
                        "implied is made regarding the utility of the data on other systems " +
                        "for general or scientific purposes, nor shall the act of distribution " +
                        "constitute any such warranty. This disclaimer applies both to " +
                        "individual use of the data and aggregate use with other data.";
                    stationTable.globalAttributes().set("license", license);

                    //fix up the attributes
                    stationTable.globalAttributes().set("acknowledgement",
                        stationTable.globalAttributes().getString("acknowledgement") + ", " +
                        "Channel Islands National Park, National Park Service");

                    //review the table
                    if (tabI == 0 && (startRow == 0 || row == dataNRows)) {
                        String2.log(stationTable.toString(100));
                        String2.pressEnterToContinue("Check if the file (above) is ok, then...");
                    }
                    String2.log("  startRow=" + startRow + " end=" + (row-1) + " island=" + island + " station=" + station); 

                    //do tests that look for known data
//1985	Anacapa	Admiral's Reef	8/30/1985 0:00:00	14003.00	Oxyjulis californica	 Adult	Señorita Adult	1	15
//2005	Anacapa	Admiral's Reef	8/22/2005 0:00:00	14003.00	Oxyjulis californica	 Adult	Señorita Adult	1	38
                    if (island.equals("Anacapa") && station.equals("Admiral's Reef")) {
                        int nRows = stationTable.nRows();
                        int testCol = stationTable.findColumnNumber("Oxyjulis_californica_Adult");
                        Test.ensureNotEqual(testCol, -1, "testCol");
                        double testTime = Calendar2.isoStringToEpochSeconds("1985-08-30");
                        int testRow = uniqueTimes.indexOf(testTime, 0);
                        Test.ensureNotEqual(testRow, -1, "testRow");
                        Test.ensureEqual(stationTable.getIntData(testCol, testRow), 15, "");

                        testTime = Calendar2.isoStringToEpochSeconds("2005-08-22");
                        testRow = uniqueTimes.indexOf(testTime, 0);
                        Test.ensureEqual(stationTable.getIntData(testCol, testRow), 38, "");
                        String2.log("passed Anacapa Admiral's Reef tests");
                    }

//2002	San Miguel	Wyckoff Ledge	9/26/2002 0:00:00	14005.00	Sebastes mystinus	 Adult	Blue rockfish Adult	1	8
//2003	San Miguel	Wyckoff Ledge	9/9/2003 0:00:00	14005.00	Sebastes mystinus	 Adult	Blue rockfish Adult	1	1
                    if (island.equals("San Miguel") && station.equals("Wyckoff Ledge")) {
                        int nRows = stationTable.nRows();
                        int testCol = stationTable.findColumnNumber("Sebastes_mystinus_Adult");
                        Test.ensureNotEqual(testCol, -1, "testCol");
                        double testTime = Calendar2.isoStringToEpochSeconds("2002-09-26");
                        int testRow = uniqueTimes.indexOf(testTime, 0);
                        Test.ensureNotEqual(testRow, -1, "testRow");
                        Test.ensureEqual(stationTable.getIntData(testCol, testRow), 8, "");

                        testTime = Calendar2.isoStringToEpochSeconds("2003-09-09");
                        testRow = uniqueTimes.indexOf(testTime, 0);
                        Test.ensureEqual(stationTable.getIntData(testCol, testRow), 1, "");
                        String2.log("passed San Miguel Syckoff Ledge tests");
                    }


                    //save the data table    
                    String tFileName = tabDir + tabName + "/" + tabName + "_" + 
                        String2.replaceAll(island, " ", "") + "_" + 
                        String2.replaceAll(station, " ", "") + ".nc";
                    tFileName = String2.replaceAll(tFileName, ' ', '_');
                    tFileName = String2.replaceAll(tFileName, "'", "");
                    stationTable.saveAs4DNcWithStringVariable(tFileName,0,1,2,3,4); 

                    startRow = row;

                    //clear uniqueTimes
                    uniqueTimes.clear();
                }
            
                //add time to uniqueTimes (for this station)?
                if (row < dataNRows) {
                    int po = uniqueTimes.indexOf(tPa.get(row), 0);
                    if (po < 0) 
                        uniqueTimes.add(tPa.get(row));
                }

            }
        }
        if (missingSites.size() > 0) {
            String2.log("\n*** Projects.kfmFishTransect200801 FAILED. Missing sites=" + missingSites);
        } else {
            String2.log("\n*** Projects.kfmFishTransect200801 finished successfully.");
        }
    }

    /**
     * KFM3 - This is the third group of files from Kushner (see f:/programs/kfm, 
     * processed starting 2007-06-26). This is biological data.
     *
     * Same procedure outline as kfmBiological, above.
     *
     * @throws Exception if trouble
     */ 
    public static void kfm3() throws Exception {
        String2.log("\n*** Projects.kfm3");

        //'_' in tsvNames will be converted to ' ' for tempID below
        //Station .nc files will be stored in subdirectories named tsvName.
        String tsvDir = "c:/programs/kfm3/";
        //order not important
        String tsvNames[] = {"KFM_SizeFrequencyGorgoniansAndStylaster", 
            "KFM_SizeFrequencyMacrocystis", "KFM_SizeFrequencyNaturalHabitat"};

        //read KFM_Site_Info.tsv: col 0)island e.g., "Anacapa", 1=siteID (e.g., Admiral's Reef)", 
        //  2) lat(deg. N), 3) lon(deg. E), 4) depth(m)
        Table site = new Table();
        site.readASCII("c:/programs/kfm/KFM_Site_Info.tsv");
        StringArray sitePa = (StringArray)site.getColumn(1);
        Test.ensureEqual(site.getStringData(0, 0), "San Miguel", "");
        Test.ensureEqual(sitePa.get(0), "Wyckoff Ledge", "");
        Test.ensureEqual(site.getFloatData(2, 0), 34.0166666666667f, ""); //lat, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(3, 0), -120.383333333333f, "");  //lon, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(4, 0), 13, "");   //depth

        //go through the source tab-separated-value files
        StringArray missingSites = new StringArray();
        for (int tsvI = 0; tsvI < tsvNames.length; tsvI++) {
            String tsvName = tsvNames[tsvI];
            String2.log("processing " + tsvDir + tsvName + ".tsv");

            //empty the results directory
            File2.deleteAllFiles(tsvDir + tsvName + "/");

            //read datafile into a table
            //col 0)siteName, e.g., "Admiral's Reef", 1) year 2+) data columns
            Table data = new Table();
            data.allowRaggedRightInReadASCII = true;  //macrocystis file has last col missing
            data.readASCII(tsvDir + tsvName + ".tsv"); 
            int dataNRows = data.nRows();
           
            //create x,y,z,t,id columns  (numeric coordinate columns are all doubles)
            DoubleArray xPa = new DoubleArray(dataNRows, false);
            DoubleArray yPa = new DoubleArray(dataNRows, false);
            DoubleArray zPa = new DoubleArray(dataNRows, false);
            DoubleArray tPa = new DoubleArray(dataNRows, false);
            StringArray idPa = new StringArray(dataNRows, false);
            for (int row = 0; row < dataNRows; row++) {
                String tSiteName = data.getStringData(0, row);
                int siteRow = sitePa.indexOf(tSiteName);
                if (siteRow == -1) {
                    int tpo = missingSites.indexOf(tSiteName);
                    if (tpo == -1) missingSites.add(tSiteName);
                    siteRow = 0; //clumsy, but lets me collect all the missing sites
                }

                xPa.add(site.getNiceDoubleData(3, siteRow));
                yPa.add(site.getNiceDoubleData(2, siteRow));
                zPa.add(site.getNiceDoubleData(4, siteRow));
                //they are just year #'s. no time zone issues.
                //Times are vague (may to oct), so assign to July 1 (middle of year).
                String tYear = data.getStringData(1, row);
                Test.ensureEqual(tYear.length(), 4, "Unexpected year=" + tYear + " on row=" + row);
                tPa.add(Calendar2.isoStringToEpochSeconds(tYear + "-07-01"));  
                idPa.add(site.getStringData(0, siteRow) + " (" + tSiteName + ")");
            }

            //put x,y,z,t,id columns in place
            data.removeColumn(0);
            data.addColumn(0, "LON",   xPa, new Attributes());
            data.addColumn(1, "LAT",   yPa, new Attributes());
            data.addColumn(2, "DEPTH", zPa, new Attributes());
            data.addColumn(3, "TIME",  tPa, new Attributes());
            data.addColumn(4, "ID",    idPa, new Attributes());
            data.columnAttributes(4).set("long_name", "Station Identifier");
            //data.columnAttributes(4).set("units", DataHelper.UNITLESS);

            //remove the year column
            Test.ensureEqual(data.getColumnName(5), "Year", "Unexpected col 5 name.");
            data.removeColumn(5);

            //add metadata for data columns
            //standardNames from https://cfconventions.org/standard-names.html
            //none seem relevant here
            for (int col = 5; col < data.nColumns(); col++) {
                String colName = data.getColumnName(col);
                if (colName.equals("Species")) {
                    data.columnAttributes(col).set("long_name", "Species");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("SpeciesName")) {
                    data.columnAttributes(col).set("long_name", "Species Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("CommonName")) {
                    data.columnAttributes(col).set("long_name", "Common Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Marker")) {
                    data.columnAttributes(col).set("long_name", colName);
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Stipes")) {
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "count");
                } else if (colName.equals("Height") || //in Macrocystis and Gorg&Styl
                           colName.equals("Width")) {
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "cm");
                } else if (colName.equals("Size")) {  //in NaturalHabitat
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "mm");
                } else {
                    Test.error("Unexpected column name=" + colName);
                }
                //data.columnAttributes(col).set("long_name", "Sea Temperature");
                //data.columnAttributes(col).set("standard_name", "sea_water_temperature");
            }

            //summaries are verbatim (except for the first few words) 
            //from c:\content\kushner\NOAA Web page KFM protocol descriptions.doc
            //from Kushner's 2007-04-11 email.
            String summary = null; 
            if (tsvName.equals("KFM_SizeFrequencyGorgoniansAndStylaster")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the size of corals at selected locations in the Channel Islands National Park. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The size frequency measurements were taken within 10 meters of the " +  //depth description from Kushner's 6/29/2007 email
    "transect line at each site.  Depths at the site vary some, but we describe " +
    "the depth of the site along the transect line where that station's " +
    "temperature logger is located, a typical depth for the site.";
            else if (tsvName.equals("KFM_SizeFrequencyMacrocystis")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the size of kelp at selected locations in the Channel Islands National Park. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The size frequency measurements were taken within 10 meters of the " +  //depth description from Kushner's 6/29/2007 email
    "transect line at each site.  Depths at the site vary some, but we describe " +
    "the depth of the site along the transect line where that station's " +
    "temperature logger is located, a typical depth for the site.";
            else if (tsvName.equals("KFM_SizeFrequencyNaturalHabitat")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the size of selected animal species at selected locations in the Channel Islands National Park. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The size frequency measurements were taken within 10 meters of the " +  //depth description from Kushner's 6/29/2007 email
    "transect line at each site.  Depths at the site vary some, but we describe " +
    "the depth of the site along the transect line where that station's " +
    "temperature logger is located, a typical depth for the site.";
            else Test.error("Unexpected tsvName=" + tsvName);

            //sort by id, x,y,z,t, spp#
            data.sort(new int[]{4,0,1,2,3,5}, new boolean[]{true, true, true, true, true, true});
            int sppCol = 6;
String2.log("sppCol name = " + data.getColumnName(sppCol));

            //make a separate file for each station
            int startRow = 0;
            for (int row = 1; row <= dataNRows; row++) {  //yes 1..=
                //id changed?
                if (row == dataNRows || //test this first
                    !data.getStringData(4, startRow).equals(data.getStringData(4, row))) {
                    
                    //make stationTable x,y,z(constant), t, col for each sp
                    int tnRows = row - startRow;
                    Table stationTable = new Table();
                    data.globalAttributes().copyTo(stationTable.globalAttributes());
                    for (int col = 0; col < data.nColumns(); col++) {
                        PrimitiveArray oldColumn = data.getColumn(col);
                        PAType elementPAType = oldColumn.elementType();
                        PrimitiveArray newColumn = 
                            PrimitiveArray.factory(elementPAType, tnRows, false);
                        stationTable.addColumn(col, data.getColumnName(col), 
                            newColumn, 
                            (Attributes)data.columnAttributes(col).clone());

                        //fill the stationTable with data
                        boolean isString = elementPAType == PAType.STRING;
                        for (int tRow = startRow; tRow < row; tRow++) {
                            if (isString)
                                newColumn.addString(oldColumn.getString(tRow));
                            else newColumn.addDouble(oldColumn.getDouble(tRow));
                        }
                    }

                    //setAttributes
                    String id = data.getStringData(4, startRow); //e.g., "San Miguel (Wyckoff Ledge)"
                    int pPo = id.indexOf('(');
                    Test.ensureNotEqual(pPo, -1, "'(' in id=" + id);
                    String island = id.substring(0, pPo - 1);
                    String station = id.substring(pPo + 1, id.length() - 1);
                    stationTable.setAttributes(0, 1, 2, 3, //x,y,z,t
                        String2.replaceAll(tsvName, '_', ' ') + " (Channel Islands)", //bold title
                            //don't make specific to this station; when aggregated, just one boldTitle will be used
                            //", " + island + ", " + station + ")", 
                        "Station", //cdmDataType
                        DataHelper.ERD_CREATOR_EMAIL,
                        DataHelper.ERD_CREATOR_NAME, 
                        DataHelper.ERD_CREATOR_URL,   
                        DataHelper.ERD_PROJECT,       
                        tsvName, //id    //don't make specific to this station; when aggregated, just one id will be used
                        "GCMD Science Keywords", //keywordsVocabulary,
                        //see http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
                        //there are plands and invertebrates, so habitat seems closest keyword
                        "Oceans > Marine Biology > Marine Habitat", //keywords

                        //references   from 2006-12-19 email from Kushner
                        "Channel Islands National Parks Inventory and Monitoring information: " +
                            "http://nature.nps.gov/im/units/medn . " +
                            "Kelp Forest Monitoring Protocols: " +
                            "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                        //summary  from 2006-12-19 email from Kushner
                        summary,
                        //my old summary
                        //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                        //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                        //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                        //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                        //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                        "Channel Islands National Park, National Park Service", 
                        null); //timeLongName     use default long name

                    //add the National Park Service disclaimer from 2006-12-19 email
                    String license = stationTable.globalAttributes().getString("license") +
                        "  National Park Service Disclaimer: " +
                        "The National Park Service shall not be held liable for " +
                        "improper or incorrect use of the data described and/or contained " +
                        "herein. These data and related graphics are not legal documents and " +
                        "are not intended to be used as such. The information contained in " +
                        "these data is dynamic and may change over time. The data are not " +
                        "better than the original sources from which they were derived. It is " +
                        "the responsibility of the data user to use the data appropriately and " +
                        "consistent within the limitation of geospatial data in general and " +
                        "these data in particular. The related graphics are intended to aid " +
                        "the data user in acquiring relevant data; it is not appropriate to " +
                        "use the related graphics as data. The National Park Service gives no " +
                        "warranty, expressed or implied, as to the accuracy, reliability, or " +
                        "completeness of these data. It is strongly recommended that these " +
                        "data are directly acquired from an NPS server and not indirectly " +
                        "through other sources which may have changed the data in some way. " +
                        "Although these data have been processed successfully on computer " +
                        "systems at the National Park Service, no warranty expressed or " +
                        "implied is made regarding the utility of the data on other systems " +
                        "for general or scientific purposes, nor shall the act of distribution " +
                        "constitute any such warranty. This disclaimer applies both to " +
                        "individual use of the data and aggregate use with other data.";
                    stationTable.globalAttributes().set("license", license);

                    //fix up the attributes
                    stationTable.globalAttributes().set("acknowledgement",
                        stationTable.globalAttributes().getString("acknowledgement") + ", " +
                        "Channel Islands National Park, National Park Service");

                    //review the table
                    String2.log("  startRow=" + startRow + " end=" + (row-1) + 
                        " island=" + island + " station=" + station); 

                    //save the data table    
                    String tFileName = tsvDir + tsvName + "/" + tsvName + "_" + 
                        String2.replaceAll(island, " ", "") + "_" + 
                        String2.replaceAll(station, " ", "") + ".nc";
                    tFileName = String2.replaceAll(tFileName, ' ', '_');
                    tFileName = String2.replaceAll(tFileName, "'", "");
                    stationTable.saveAsFlatNc(tFileName, "row"); 
                    //if (startRow == 0 || row == data.nRows()) {
                    //    String2.log("\n  table:" + tFileName + "\n" + stationTable);
                    //    String2.pressEnterToContinue(                            
                    //        "Check if the file (above) is ok, then...");
                    //}

                    startRow = row;
                }
            }
        }
        if (missingSites.size() > 0) {
            String2.log("\n*** Projects.kfm3 FAILED. Missing sites=" + missingSites);
        } else {
            String2.log("\n*** Projects.kfm3 finished successfully.");
        }
    }

    /**
     * KFM3 - This is the third group of files from Kushner (see f:/programs/kfm, 
     * processed starting 2007-06-26). This is biological data.
     *
     * Same procedure outline as kfmBiological, above.
     *
     * I will put the results in otter /u00/bob/kfm2008/ which mimics my f:/programs/kfm200801 .
     *
     * @throws Exception if trouble
     */ 
    public static void kfmSizeFrequency200801() throws Exception {
        String2.log("\n*** Projects.kfmSizeFrequency200801");

        //'_' in tabNames will be converted to ' ' for tempID below
        //Station .nc files will be stored in subdirectories named tabName.
        String tabDir = "c:/programs/kfm200801/";
        //order not important
        String tabNames[] = {
            //"KFMSizeFrequencyGorgoniansAndStylaster", 
            //"KFMSizeFrequencyMacrocystis", 
            "KFMSizeFrequencyNaturalHabitat"};
        String boldTitles[] = {
            //"Size and Frequency of Gorgonians And Stylaster", 
            //"Size and Frequency of Macrocystis", 
            "Size and Frequency, Natural Habitat"};

        //read KFM_Site_Info.tsv: col 0)island e.g., "Anacapa", 1=siteID (e.g., Admiral's Reef)", 
        //  2) lat(deg. N), 3) lon(deg. E), 4) depth(m)
        //Island	SiteName	Lat	Lon	Depth (meters)
        //San Miguel	Wyckoff Ledge	34.0166666666667	-120.383333333333	13
        Table site = new Table();
        site.readASCII(tabDir + "KFM_Site_Info.tsv"); //copied from last year
        StringArray sitePa = (StringArray)site.getColumn(1);
        Test.ensureEqual(site.getStringData(0, 0), "San Miguel", "");
        Test.ensureEqual(sitePa.get(0), "Wyckoff Ledge", "");
        Test.ensureEqual(site.getFloatData(2, 0), 34.0166666666667f, ""); //lat, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(3, 0), -120.383333333333f, "");  //lon, !!! already rounded to nearest minute at Kushner's request
        Test.ensureEqual(site.getFloatData(4, 0), 13, "");   //depth

        //go through the source tab-separated-value files
        StringArray missingSites = new StringArray();
        for (int tabI = 0; tabI < tabNames.length; tabI++) {
            String tabName = tabNames[tabI];
            String boldTitle = boldTitles[tabI];
            String2.log("processing " + tabDir + tabName + ".tab");

            //empty the results directory
            File2.deleteAllFiles(tabDir + tabName + "/");

            //read datafile into a table
            //0=SiteNumber	1=IslandName	2=SiteName	3=Year	4=Species	5=Species Name	6=CommonName	7=Size mm
            //1	San Miguel	Wyckoff Ledge	1985	9002.00	Haliotis rufescens	Red abalone	210
            Table data = new Table();
            data.allowRaggedRightInReadASCII = true;  //macrocystis file has last col missing
            data.readASCII(tabDir + tabName + ".tab"); 
            int dataNRows = data.nRows();
            Test.ensureEqual(data.getColumnName(0), "SiteNumber", "");
            Test.ensureEqual(data.getColumnName(1), "IslandName", "");
            Test.ensureEqual(data.getColumnName(2), "SiteName", "");
            Test.ensureEqual(data.getColumnName(3), "Year", "");
            Test.ensureEqual(data.getColumnName(4), "Species", "");
            Test.ensureEqual(data.getColumnName(5), "Species Name", "");
            Test.ensureEqual(data.getColumnName(6), "CommonName", "");
            Test.ensureEqual(data.getColumnName(7), "Size mm", "");
           
            //create x,y,z,t,id columns  (numeric coordinate columns are all doubles)
            DoubleArray xPa = new DoubleArray(dataNRows, false);
            DoubleArray yPa = new DoubleArray(dataNRows, false);
            DoubleArray zPa = new DoubleArray(dataNRows, false);
            DoubleArray tPa = new DoubleArray(dataNRows, false);
            StringArray idPa = new StringArray(dataNRows, false);
            for (int row = 0; row < dataNRows; row++) {
                String tSiteName = data.getStringData(2, row);
                int siteRow = sitePa.indexOf(tSiteName);
                if (siteRow == -1) {
                    int tpo = missingSites.indexOf(tSiteName);
                    if (tpo == -1) missingSites.add(tSiteName);
                    siteRow = 0; //clumsy, but lets me collect all the missing sites
                }

                xPa.add(site.getNiceDoubleData(3, siteRow));
                yPa.add(site.getNiceDoubleData(2, siteRow));
                zPa.add(site.getNiceDoubleData(4, siteRow));
                //they are just year #'s. no time zone issues.
                //Times are vague (may to oct), so assign to July 1 (middle of year).
                String tYear = data.getStringData(3, row);
                Test.ensureEqual(tYear.length(), 4, "Unexpected year=" + tYear + " on row=" + row);
                tPa.add(Calendar2.isoStringToEpochSeconds(tYear + "-07-01"));  
                idPa.add(site.getStringData(0, siteRow) + " (" + tSiteName + ")");
            }

            //remove the year, siteName, islandName, site# column
            data.removeColumn(3);
            data.removeColumn(2);
            data.removeColumn(1);
            data.removeColumn(0);

            //put x,y,z,t,id columns in place
            data.removeColumn(0);
            data.addColumn(0, "LON",   xPa, new Attributes());
            data.addColumn(1, "LAT",   yPa, new Attributes());
            data.addColumn(2, "DEPTH", zPa, new Attributes());
            data.addColumn(3, "TIME",  tPa, new Attributes());
            data.addColumn(4, "ID",    idPa, new Attributes());
            data.columnAttributes(4).set("long_name", "Station Identifier");
            //data.columnAttributes(4).set("units", DataHelper.UNITLESS);

            //add metadata for data columns
            //standardNames from https://cfconventions.org/standard-names.html
            //none seem relevant here
            for (int col = 5; col < data.nColumns(); col++) {
                String colName = data.getColumnName(col);
                if (colName.equals("Species")) {
                    data.columnAttributes(col).set("long_name", "Species");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Species Name")) {
                    data.columnAttributes(col).set("long_name", "Species Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("CommonName")) {
                    data.columnAttributes(col).set("long_name", "Common Name");
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Marker")) {
                    data.columnAttributes(col).set("long_name", colName);
                    //data.columnAttributes(col).set("units", DataHelper.UNITLESS);
                } else if (colName.equals("Stipes")) {
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "count");
                } else if (colName.equals("Height") || //in Macrocystis and Gorg&Styl
                           colName.equals("Width")) {
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "cm");
                } else if (colName.equals("Size mm")) {  //in NaturalHabitat
                    data.setColumnName(col, "Size");
                    data.columnAttributes(col).set("long_name", colName);
                    data.columnAttributes(col).set("units", "mm");
                } else {
                    Test.error("Unexpected column name=" + colName);
                }
                //data.columnAttributes(col).set("long_name", "Sea Temperature");
                //data.columnAttributes(col).set("standard_name", "sea_water_temperature");
            }

            //summaries are verbatim (except for the first few words) 
            //from c:\content\kushner\NOAA Web page KFM protocol descriptions.doc
            //from Kushner's 2007-04-11 email.
            String summary = null; 
            if (tabName.equals("KFMSizeFrequencyGorgoniansAndStylaster")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the size of corals at selected locations. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The size frequency measurements were taken within 10 meters of the " +  //depth description from Kushner's 6/29/2007 email
    "transect line at each site.  Depths at the site vary some, but we describe " +
    "the depth of the site along the transect line where that station's " +
    "temperature logger is located, a typical depth for the site.";
            else if (tabName.equals("KFMSizeFrequencyMacrocystis")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the size of kelp at selected locations. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The size frequency measurements were taken within 10 meters of the " +  //depth description from Kushner's 6/29/2007 email
    "transect line at each site.  Depths at the site vary some, but we describe " +
    "the depth of the site along the transect line where that station's " +
    "temperature logger is located, a typical depth for the site.";
            else if (tabName.equals("KFMSizeFrequencyNaturalHabitat")) summary = 
    "This dataset from the Channel Islands National Park's Kelp Forest Monitoring Program has measurements of the size of selected animal species at selected locations. " + 
    "Sampling is conducted annually between the months of May-October, " +
    "so the Time data in this file is July 1 of each year (a nominal value). " +
    "The size frequency measurements were taken within 10 meters of the " +  //depth description from Kushner's 6/29/2007 email
    "transect line at each site.  Depths at the site vary some, but we describe " +
    "the depth of the site along the transect line where that station's " +
    "temperature logger is located, a typical depth for the site.";
            else Test.error("Unexpected tabName=" + tabName);

            //sort by id, x,y,z,t, spp#
            data.sort(new int[]{4,0,1,2,3,5}, new boolean[]{true, true, true, true, true, true});
            int sppCol = 6;
String2.log("sppCol name = " + data.getColumnName(sppCol));

            //make a separate file for each station
            int startRow = 0;
            for (int row = 1; row <= dataNRows; row++) {  //yes 1..=
                //id changed?
                if (row == dataNRows || //test this first
                    !data.getStringData(4, startRow).equals(data.getStringData(4, row))) {
                    
                    //make stationTable x,y,z(constant), t, col for each sp
                    int tnRows = row - startRow;
                    Table stationTable = new Table();
                    data.globalAttributes().copyTo(stationTable.globalAttributes());
                    for (int col = 0; col < data.nColumns(); col++) {
                        PrimitiveArray oldColumn = data.getColumn(col);
                        PAType elementPAType = oldColumn.elementType();
                        PrimitiveArray newColumn = 
                            PrimitiveArray.factory(elementPAType, tnRows, false);
                        stationTable.addColumn(col, data.getColumnName(col), 
                            newColumn, 
                            (Attributes)data.columnAttributes(col).clone());

                        //fill the stationTable with data
                        boolean isString = elementPAType == PAType.STRING;
                        for (int tRow = startRow; tRow < row; tRow++) {
                            if (isString)
                                newColumn.addString(oldColumn.getString(tRow));
                            else newColumn.addDouble(oldColumn.getDouble(tRow));
                        }
                    }

                    //setAttributes
                    String id = data.getStringData(4, startRow); //e.g., "San Miguel (Wyckoff Ledge)"
                    int pPo = id.indexOf('(');
                    Test.ensureNotEqual(pPo, -1, "'(' in id=" + id);
                    String island = id.substring(0, pPo - 1);
                    String station = id.substring(pPo + 1, id.length() - 1);
                    stationTable.setAttributes(0, 1, 2, 3, //x,y,z,t
                        boldTitle + " (Kelp Forest Monitoring, Channel Islands)", //bold title
                            //don't make specific to this station; when aggregated, just one boldTitle will be used
                            //", " + island + ", " + station + ")", 
                        "Station", //cdmDataType
                        DataHelper.ERD_CREATOR_EMAIL,
                        DataHelper.ERD_CREATOR_NAME, 
                        DataHelper.ERD_CREATOR_URL,   
                        DataHelper.ERD_PROJECT,       
                        tabName, //id    //don't make specific to this station; when aggregated, just one id will be used
                        "GCMD Science Keywords", //keywordsVocabulary,
                        //see http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
                        //there are plands and invertebrates, so habitat seems closest keyword
                        "Oceans > Marine Biology > Marine Habitat", //keywords

                        //references   from 2006-12-19 email from Kushner
                        "Channel Islands National Parks Inventory and Monitoring information: " +
                            "http://nature.nps.gov/im/units/medn . " +
                            "Kelp Forest Monitoring Protocols: " +
                            "http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .",

                        //summary  from 2006-12-19 email from Kushner
                        summary,
                        //my old summary
                        //"Temperatures were recorded by David Kushner (David_Kushner@nps.gov) " + 
                        //    "using Onset Computer Corp. temperature loggers, accurate to +/- 0.2 C. The raw time values " +
                        //    "(Pacific Daylight Saving Time) were converted to Zulu time by adding 7 hours and then stored in this file. " +
                        //    "LAT and LON values were stored without seconds values to obscure the station's exact location.",

                        //courtesy, see 2006-12-12 email, but order reversed to current in 2006-12-19 email from Kushner
                        "Channel Islands National Park, National Park Service", 
                        null); //timeLongName     use default long name

                    //add the National Park Service disclaimer from 2006-12-19 email
                    String license = stationTable.globalAttributes().getString("license") +
                        "  National Park Service Disclaimer: " +
                        "The National Park Service shall not be held liable for " +
                        "improper or incorrect use of the data described and/or contained " +
                        "herein. These data and related graphics are not legal documents and " +
                        "are not intended to be used as such. The information contained in " +
                        "these data is dynamic and may change over time. The data are not " +
                        "better than the original sources from which they were derived. It is " +
                        "the responsibility of the data user to use the data appropriately and " +
                        "consistent within the limitation of geospatial data in general and " +
                        "these data in particular. The related graphics are intended to aid " +
                        "the data user in acquiring relevant data; it is not appropriate to " +
                        "use the related graphics as data. The National Park Service gives no " +
                        "warranty, expressed or implied, as to the accuracy, reliability, or " +
                        "completeness of these data. It is strongly recommended that these " +
                        "data are directly acquired from an NPS server and not indirectly " +
                        "through other sources which may have changed the data in some way. " +
                        "Although these data have been processed successfully on computer " +
                        "systems at the National Park Service, no warranty expressed or " +
                        "implied is made regarding the utility of the data on other systems " +
                        "for general or scientific purposes, nor shall the act of distribution " +
                        "constitute any such warranty. This disclaimer applies both to " +
                        "individual use of the data and aggregate use with other data.";
                    stationTable.globalAttributes().set("license", license);

                    //fix up the attributes
                    stationTable.globalAttributes().set("acknowledgement",
                        stationTable.globalAttributes().getString("acknowledgement") + ", " +
                        "Channel Islands National Park, National Park Service");

                    //review the table
                    String2.log("  startRow=" + startRow + " end=" + (row-1) + 
                        " island=" + island + " station=" + station); 

                    //save the data table    
                    String tFileName = tabDir + tabName + "/" + tabName + "_" + 
                        String2.replaceAll(island, " ", "") + "_" + 
                        String2.replaceAll(station, " ", "") + ".nc";
                    tFileName = String2.replaceAll(tFileName, ' ', '_');
                    tFileName = String2.replaceAll(tFileName, "'", "");
                    stationTable.saveAsFlatNc(tFileName, "row"); 
                    //if (startRow == 0 || row == data.nRows()) {
                    //    String2.log("\n  table:" + tFileName + "\n" + stationTable);
                    //    String2.pressEnterToContinue(                            
                    //        "Check if the file (above) is ok, then...");
                    //}

                    startRow = row;
                }
            }
        }
        if (missingSites.size() > 0) {
            String2.log("\n*** Projects.kfmSizeFrequency200801 FAILED. Missing sites=" + missingSites);
        } else {
            String2.log("\n*** Projects.kfmSizeFrequency200801 finished successfully.");
        }
    }

    /** 
     * A special project for David Kushner. 
     * This extracts data from 5 of my standard ndbc buoy files in the format Kushner wants.
     * 12/06
     */ 
    public static void kushner() throws Exception {
        int id[] = {46023, 46025, 46053, 46054, 46063, 46069};
        int nId = id.length;
        for (int idi = 0; idi < nId; idi++) { 
            String2.log("\nID=" + id[idi]);
            //original names
              ///* 0*/"LON", "LAT", "DEPTH", "TIME", "ID", //use names that Lynn used in file that worked
              ///* 5*/"WD", "WSPD", "GST", "WVHT", "DPD", //most common name in ndbc files
              ///*10*/"APD", "MWD", "BAR", "ATMP", "WTMP", 
              ///*15*/"DEWP", "VIS", "PTDY", "TIDE", "WSPU", 
              ///*20*/"WSPV"};
            ///* 0*/"degrees_east", "degrees_north", "m", Calendar2.SECONDS_SINCE_1970, DataHelper.UNITLESS, 
            ///* 5*/"degrees_true", "m s-1", "m s-1", "m", "s", 
            ///*10*/"s", "degrees_true", "hPa", "degree_C", "degree_C", 
            ///*15*/"degree_C", "km","hPa", "m", "m s-1", 
            ///*20*/"m s-1"};
            String desiredColumns[] = {"TIME", "WD", "WSPD", "GST",  "WVHT", "DPD",  "APD",  "ATMP", "WTMP"};
            Table table = new Table();
            table.read4DNc("c:/programs/kushner/NDBC_" + id[idi] + "_met.nc", null, 
                1, stationColumnName, 4); //standardizeWhat=1
            String2.log("colNames=" + String2.toCSSVString(table.getColumnNames()));

            //pluck out desired columns
            //wants:
            //TIME\tWDIR\tWSPD\tGST\tWVHT\tDPD\tAPD\tATMP\tWTMP
            //12/1/2006\t15:00:00\t260\t9.7\t11.7\t2.3\t16\t4.4\t60.4\t61
            Table tTable = new Table();
            for (int col = 0; col < desiredColumns.length; col++) 
                tTable.addColumn(col, desiredColumns[col], table.getColumn(desiredColumns[col]));
            table = tTable;
            Test.ensureEqual(table.getColumnNames(), desiredColumns, "");
          
            //populate newTime
            StringArray newTime = new StringArray();        
            DoubleArray oldTime = (DoubleArray)table.getColumn(0);
            int nRows = oldTime.size();
            for (int row = 0; row < nRows; row++) {
                //US slash-style date, 24 hour time
                double d = oldTime.get(row);
                Test.ensureNotEqual(d, Double.NaN, "");
                GregorianCalendar gc = Calendar2.epochSecondsToGc(d);
                newTime.add(Calendar2.formatAsUSSlash24(gc));
            }

            //insert the new time column
            table.setColumn(0, newTime);

            //write out the file
            table.saveAsTabbedASCII("c:/programs/kushner/NDBC_" + id[idi] + "_met.asc"); 
            String2.log(table.toString(5));

        }
    }

    /**
     * This is the controlling program for finding all the .nc files
     * in subdirectories or url and writing catalog.xml-style info to fileName.
     *
     * @throws Exception if trouble
     */
    public static void ssc() throws Exception {
        String url = "http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/dapnav_main.py/WCOS/nmsp/wcos/"; 
        String fileName = "c:/temp/ssc.xml";

        Writer writer = new BufferedWriter(new FileWriter(fileName));
        try {
            oneSsc(writer,
                "http://biloxi-bay.ssc.hpc.msstate.edu",
                "/dods-bin/dapnav_main.py/WCOS/nmsp/wcos/"); // ANO001/
        } finally {
            writer.close();
        }
        String2.log(String2.readFromFile(fileName)[1]);
        String2.log("fileName=" + fileName);

    }
    /**
     * This is called by ssc to process the ssc information at the baseUrl+url:
     * either handling the referenced .nc files or
     * calling oneSsc recursively to handle subdirectories.
     *
     * @param writer 
     * @param baseUrl e.g., http://biloxi-bay.ssc.hpc.msstate.edu
     * @param url e.g., /dods-bin/dapnav_main.py/WCOS/nmsp/wcos/ANO001/2005/  
     *    (already percentEncoded as needed)
     */
    public static void oneSsc(Writer writer, String baseUrl, String url) throws Exception { 
        String2.log("oneSsc entering " + url);
        ArrayList<String> sa = SSR.getUrlResponseArrayList(baseUrl + url);
        int line = 0;
        //read to "parent directory" line
        while (line < sa.size() && sa.get(line).indexOf("parent directory") < 0)
            line++;
        line++;
        if (line == sa.size())
            throw new Exception("No 'parent directory' found\n" + baseUrl + url + 
                "\n" + String2.toNewlineString(sa.toArray(new String[0])));

        String aStart = "<a href=\"";
        while (true) {

            //are we done with this file?
            if (line >= sa.size() || sa.get(line).startsWith("</pre>")) {
                String2.log("  oneSsc leaving " + url);
                return;
            }

            //is it a file?    sample is one line...
            //ANO001_021MTBD000R00_20050617.nc        
            //[<a href="/dods-bin/dapnav_main.py/WCOS/nmsp/wcos/ANO001/2005/
            //ANO001_021MTBD000R00_20050617.nc">File information</a>] 
            //[<a href="http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/pyt.sh/WCOS/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc">OPeNDAP direct</a>] 
            //Thu Feb 23 13:48:30 2006          627120 
            String s = sa.get(line);
            //String2.log("  line#" + line + "=" + s.substring(0, Math.min(70, s.length())));
            int po;
            int po2 = s.indexOf("\">OPeNDAP direct</a>");
            if (po2 > 0) {
                po = s.lastIndexOf(aStart, po2);
                s = s.substring(po + aStart.length(), po2);
                String2.log("  opendap=" + s);
                /*
                //get nTimes
                //isn't there a better way to get nTimes?
                String dds = SSR.getURLResponseStringUnchanged(s + ".dds");
                po = dds.indexOf("Time = ");
                po2 = dds.indexOf("]", po);
                int nTimes = String2.parseInt(dds.substring(po + 7, po2));
                if (nTimes == Integer.MAX_VALUE)
                    throw new Exception("Unexpected Time from dds=\n" + dds);
                DConnect dConnect = new DConnect(s, true, 1, 1);
                double beginTime = OpendapHelper.getDoubleArray(dConnect, "?Time[0]")[0]; 
                double endTime   = OpendapHelper.getDoubleArray(dConnect, "?Time[" + (nTimes-1) + "]")[0]; 
                String2.log("  Time n=" + nTimes + " begin=" + beginTime + " end=" + endTime);
                */
                writeSsc(writer, s);
                //return;  //to just see first file, during development 

            } else if (s.startsWith(aStart)) {
                //is it a dir?
                //from http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/dapnav_main.py/WCOS/nmsp/wcos/ANO001/2005/
                //<a href="/dods-bin/dapnav_main.py/WCOS/nmsp/wcos/ANO001/2005/">2005/  </a>  Sat May 27 22:19:30 2006            4096 
                po = 0;
                po2 = s.indexOf("\">");
                s = s.substring(po + aStart.length(), po2);
                oneSsc(writer, baseUrl, s);
            } else {
                throw new Exception("Unexpected line:\n" + s);
            }

            line++;
        }
    }

    public static String lastNcName = null;

    /**
     * This is called by oneSsc to write the catalog.xml-style xml related
     * to ncUrl to the writer.
     *
     * @param writer
     * @param ncUrl e.g., http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/dapnav_main.py/WCOS/nmsp/wcos/ANO001/2005/
     *     ANO001_021MTBD000R00_20050617.nc
     * @throws Exception if trouble
     */
    public static void writeSsc(Writer writer, String ncUrl) throws Exception {
        String nameExt = File2.getNameAndExtension(ncUrl);
        if (nameExt.length() != 32 || !nameExt.endsWith(".nc") || nameExt.charAt(17) != 'R' ||
            nameExt.charAt(6) != '_')
            throw new Exception("Improper format for ncUrl=" + ncUrl);
        String name6 = nameExt.substring(0, 6);
        int    depth = String2.parseInt(nameExt.substring(14, 17));
        String year  = nameExt.substring(21, 25);

        String lastName6 = lastNcName == null? "" : lastNcName.substring(0, 6);
        int    lastDepth = lastNcName == null? -99999 : String2.parseInt(lastNcName.substring(14, 17));
        String lastYear  = lastNcName == null? "" : lastNcName.substring(21, 25);

        //end previous inner dataset?
        if (!name6.equals(lastName6) || !year.equals(lastYear) || depth != lastDepth) {
            //end this dataset
            if (lastNcName != null)
                writer.write(
                "        </aggregation>\n" +
                "      </netcdf>\n" +
                "    </dataset>\n" +
                "\n");
        }

        //outer dataset  (change in name6 or year)
        if (!name6.equals(lastName6) || !year.equals(lastYear)) {
            //end previous outer dataset
            if (lastNcName != null)
                writer.write(
                "  </dataset>\n" +
                "\n");

            //start new outer dataset
            writer.write(
                "  <dataset name=\"WCOS [?Point Ano Nuevo CA] (" + name6 + ") " + year + "\">\n");
        }

        //start the inner dataset?
        if (!name6.equals(lastName6) || !year.equals(lastYear) || depth != lastDepth) {
                writer.write(
                "    <dataset name=\"Measurement at " + depth + "m\" " +
                    "ID=\"WCOS/temp/" + year + "_" + name6 + "_" + depth + "m\" " +
                    "urlPath=\"WCOS/temp/" + year + "_" + name6 + "_" + depth + "m\">\n" +
                "      <serviceName>ncdods</serviceName>\n" +
                "      <!-- <serviceName>wcs</serviceName> -->\n" +
                "      <netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n" +
                "        <variable name=\"Time\" shape=\"Time\" type=\"double\">\n" +
                "          <attribute name=\"units\" value=\"seconds since 1970-01-01 00:00:00\"/>\n" +
                "          <attribute name=\"_CoordinateAxisType\" value=\"Time\" />\n" +
                "        </variable>\n" +
                "        <aggregation dimName=\"Time\" type=\"joinExisting\">\n" +
                "          <variableAgg name=\"T\" />\n");
        }

        //add the file to the aggregation
        writer.write(
                "          <netcdf location=\"" + ncUrl + "\" />\n");


        lastNcName = nameExt;
    }


    /**
     * This makes ncml files for soda (3.3.1).
     *
     * @param iceOcean "ice" or "ocean"
     * @throws Exception if trouble
     */
    public static void makeSoda331Ncml(String iceOcean, int startYear, int stopYear)
        throws Exception {

        String dir = "/u00/soda3/soda3_" + iceOcean + "/ncml/";
        String preName =  "soda3.3.1_mn_" + iceOcean + "_reg_";
        for (int year = startYear; year <= stopYear; year++) {
            String name = preName + year;
            String2.log("writing " + dir + name + ".ncml");
            Writer writer = new BufferedWriter(new FileWriter(dir + name + ".ncml"));
            try {
                StringBuilder values = new StringBuilder();
                for (int i = 1; i <= 12; i++)
                    values.append((Calendar2.isoStringToEpochSeconds(year + "-" + String2.zeroPad(""+i, 2) + "-16") / 
                        Calendar2.SECONDS_PER_DAY) + " ");
                writer.write(
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'\n" +
"  location=\"" + name + ".nc\">\n" +
"  <variable name=\"time\">\n" +
"    <attribute name='units' value='days since 1970-01-01' />\n" +
"    <values>" + values+ "</values>\n" +
"  </variable>\n" +
"</netcdf>\n");
            } finally {
                writer.close();
            }
        }
    }

    /**
     * This adds metadata and a time dimension to SODA data files.
     * See https://www.atmos.umd.edu/~ocean/
     * ncdump of sample source file
<pre>
netcdf SODA_1.4.2_200112 {
dimensions:
        lon = 720 ;
        lat = 330 ;
        depth = 40 ;
        time = UNLIMITED ; // (1 currently)
variables:
        float temp(depth, lat, lon) ;
                temp:long_name = "TEMPERATURE" ;
                temp:units = "deg. C" ;
                temp:_FillValue = -9.99e+033f ;
        float salt(depth, lat, lon) ;
                salt:long_name = "SALINITY(ppt)" ;
                salt:units = "frac. by wt. less" ;
                salt:_FillValue = -9.99e+033f ;
        float u(depth, lat, lon) ;
                u:long_name = "ZONAL VELOCITY" ;
                u:units = "cm/sec" ;
                u:_FillValue = -9.99e+033f ;
        float v(depth, lat, lon) ;
                v:long_name = "MERIDIONAL VELOCITY" ;
                v:units = "cm/sec" ;
                v:_FillValue = -9.99e+033f ;
        float taux(lat, lon) ;
                taux:long_name = "TAU X" ;
                taux:units = "dynes/cm^2" ;
                taux:_FillValue = -9.99e+033f ;
        float tauy(lat, lon) ;
                tauy:long_name = "TAU Y" ;
                tauy:units = "dynes/cm^2" ;
                tauy:_FillValue = -9.99e+033f ;
        float ssh(lat, lon) ;
                ssh:long_name = "SEA LEVEL HEIGHT" ;
                ssh:units = "cm" ;
                ssh:_FillValue = -9.99e+033f ;
        double lon(lon) ;
                lon:units = "degrees_east" ;
        double lat(lat) ;
                lat:units = "degrees_north" ;
        double depth(depth) ;
                depth:units = "meters" ;
                depth:positive = "down" ;
        float time(time) ;
                time:units = "months" ;

// global attributes:
                :title = "SODA - POP 1.4.2 Assimilation TAMU/UMD" ;
}
</pre>
     *
     * @param sodaVersion e.g., 1.4.2
     * @param oldDir e.g., F:/SODA_1.4.2/
     * @param newDir e.g., F:/SODA_1.4.2nc/
     * @throws Exception if trouble
     */
    public static void soda(String sodaVersion, String oldDir, String newDir) throws Exception {

        //get a list of files
        String tempDir = "c:/programs/";
        String[] fileNames = RegexFilenameFilter.list(oldDir, "(.+cdf|.+cdf.gz)");
        NetcdfFile oldFile = null;
        NetcdfFileWriter newFile = null;
        try {

            //for each file
            for (int fn = 0; fn < fileNames.length; fn++) { 
            //for (int fn = 0; fn < 1; fn++) { 
                String2.log("converting " + fileNames[fn]); //e.g., SODA_1.4.2_195806.cdf
    
                int po = fileNames[fn].lastIndexOf('_');
                int year  = String2.parseInt(fileNames[fn].substring(po + 1, po + 5));
                int month = String2.parseInt(fileNames[fn].substring(po + 5, po + 7)); //1..
                int months = (year - 1950) * 12 + month - 1;
//if (year < 2007) continue;
//if (year == 2007 && month < 2) continue;
                String2.log("  year=" + year + " month=" + month + " monthsSinceJan1950=" + months);

                //if .gz, make a temp file
                String cdfDir = oldDir;
                String cdfName = oldDir + fileNames[fn];
                boolean gzipped = cdfName.endsWith(".gz");
                if (gzipped) {
                    SSR.unGzip(oldDir + fileNames[fn], tempDir, true, 90);
                    cdfDir = tempDir;
                    cdfName = fileNames[fn].substring(0, fileNames[fn].length() - 3);
                }                    

                if (fn == 0) String2.log("\noldFile=" + NcHelper.ncdump(cdfDir + cdfName, "-h") + "\n");

                //open the old file
                String newName = cdfName.substring(0, cdfName.length() - 3) + "nc";
                oldFile = NcHelper.openFile(cdfDir + cdfName);
                try {

                    //open the new file
                    newFile = NetcdfFileWriter.createNew(
                        NetcdfFileWriter.Version.netcdf3, newDir + newName);
                    try {
                        boolean nc3Mode = true;
                        Group rootGroup = newFile.addGroup(null, "");

                        //find old dimensions
                        Dimension oldTimeDimension  = oldFile.findDimension("time");
                        Dimension oldDepthDimension = oldFile.findDimension("depth");
                        Dimension oldLatDimension   = oldFile.findDimension("lat");
                        Dimension oldLonDimension   = oldFile.findDimension("lon");

                        //find variables
                        List<Variable> oldVars = oldFile.getVariables();

                        //create the dimensions
                        Dimension timeDimension  = newFile.addDimension(rootGroup, "time", 1);
                        Dimension depthDimension = newFile.addDimension(rootGroup, "depth", oldDepthDimension.getLength());
                        Dimension latDimension   = newFile.addDimension(rootGroup, "lat",   oldLatDimension.getLength());
                        Dimension lonDimension   = newFile.addDimension(rootGroup, "lon",   oldLonDimension.getLength());

                        //define each variable
                        double minLon = Double.NaN, maxLon = Double.NaN, lonSpacing = Double.NaN;
                        double minLat = Double.NaN, maxLat = Double.NaN, latSpacing = Double.NaN;
                        double minDepth = Double.NaN, maxDepth = Double.NaN;

                        Variable newVars[] = new Variable[oldVars.size()];
                        for (int v = 0; v < oldVars.size(); v++) {
                            Variable oldVar = oldVars.get(v);
                            String varName = oldVar.getName();
                            Attributes atts = new Attributes(); 
                            NcHelper.getVariableAttributes(oldVar, atts);
                            ArrayList<Dimension> dimensions = new ArrayList();
                            DataType dataType = oldVar.getDataType();

                            //if lon 
                            if (varName.equals("lon")) {
                                dimensions.add(oldVar.getDimension(0));

                                PrimitiveArray pa = NcHelper.getPrimitiveArray(oldVar);
                                minLon = pa.getDouble(0);
                                maxLon = pa.getDouble(pa.size() - 1);
                                if (pa.isEvenlySpaced().length() == 0)
                                    lonSpacing = (maxLon - minLon) / (pa.size() - 1);

                                atts.add("_CoordinateAxisType", "Lon");
                                atts.add("actual_range", new DoubleArray(new double[]{minLon, maxLon}));
                                atts.add("axis", "X");
                                atts.add("coordsys", "geographic");
                                atts.add("long_name", "Longitude");
                                atts.add("standard_name", "longitude");
                                atts.add("units", "degrees_east");

                            //if lat 
                            } else if (varName.equals("lat")) {
                                dimensions.add(oldVar.getDimension(0));

                                PrimitiveArray pa = NcHelper.getPrimitiveArray(oldVar);
                                minLat = pa.getDouble(0);
                                maxLat = pa.getDouble(pa.size() - 1);
                                if (pa.isEvenlySpaced().length() == 0)
                                    latSpacing = (maxLat - minLat) / (pa.size() - 1);

                                atts.add("_CoordinateAxisType", "Lat");
                                atts.add("actual_range", new DoubleArray(new double[]{minLat, maxLat}));
                                atts.add("axis", "Y");
                                atts.add("coordsys", "geographic");
                                atts.add("long_name", "Latitude");
                                atts.add("standard_name", "latitude");
                                atts.add("units", "degrees_north");                    

                            //if depth
                            } else if (varName.equals("depth")) {
                                dimensions.add(oldVar.getDimension(0));

                                PrimitiveArray pa = NcHelper.getPrimitiveArray(oldVar);
                                minDepth = pa.getDouble(0);
                                maxDepth = pa.getDouble(pa.size() - 1);

                                atts.add("_CoordinateAxisType", "Height");
                                atts.add("_CoordinateZisPositive", "down");
                                atts.add("actual_range", new DoubleArray(new double[]{minDepth, maxDepth}));
                                atts.add("axis", "Z");
                                atts.add("long_name", "Depth");
                                atts.add("positive", "down");
                                atts.add("standard_name", "depth");
                                atts.add("units", "m");

                            //if time
                            } else if (varName.equals("time")) {
                                dimensions.add(timeDimension);

                                dataType = DataType.INT; //the only oldVar that changes dataType

                                //ensure time size == 1;
                                PrimitiveArray pa = NcHelper.getPrimitiveArray(oldVar);
                                if (pa.size() != 1)
                                    throw new Exception("time size=" + pa.size() + "\n" + pa);

                                atts.add("_CoordinateAxisType", "Time");
                                atts.add("axis", "T");
                                atts.add("long_name", "Time");
                                atts.add("standard_name", "time");
                                atts.add("time_origin", "15-JAN-1950 00:00:00");
                                atts.add("units", "months since 1950-01-15T00:00:00Z");                    

                            //other variables
                            } else {

                                //add time dimension
                                int rank = oldVar.getRank();
                                dimensions.add(timeDimension);
                                for (int r = 0; r < rank; r++)
                                    dimensions.add(oldVar.getDimension(r));

                                atts.add("missing_value", atts.getFloat("_FillValue"));
                                if (varName.equals("temp")) {
                                    Test.ensureEqual(atts.getString("units"), "deg. C", "");
                                    atts.add("long_name", "Sea Water Temperature");
                                    atts.add("standard_name", "sea_water_temperature");
                                    atts.add("units", "degree_C");

                                } else if (varName.equals("salt")) {
                                    if (atts.getString("units").equals("frac. by wt. less")) {
                                        //atts.add("units", "frac. by wt. less"); //???
                                    } else if (atts.getString("units").equals("g/kg")) {
                                        atts.add("units", "g kg-1"); //???
                                    } else {
                                        Test.error("Unexpected salt units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Salinity");
                                    atts.add("standard_name", "sea_water_salinity");

                                } else if (varName.equals("u")) {
                                    if (atts.getString("units").equals("cm/sec")) {
                                        atts.add("units", "cm s-1");
                                    } else if (atts.getString("units").equals("m/sec")) {
                                        atts.add("units", "m s-1");
                                    } else {
                                        Test.error("Unexpected u units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Zonal Velocity");
                                    atts.add("standard_name", "sea_water_x_velocity");

                                } else if (varName.equals("v")) {
                                    if (atts.getString("units").equals("cm/sec")) {
                                        atts.add("units", "cm s-1");
                                    } else if (atts.getString("units").equals("m/sec")) {
                                        atts.add("units", "m s-1");
                                    } else {
                                        Test.error("Unexpected v units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Meridional Velocity");
                                    atts.add("standard_name", "sea_water_y_velocity");

                                } else if (varName.equals("w")) {
                                    if (atts.getString("units").equals("cm/sec")) {
                                        atts.add("units", "cm s-1");
                                    } else if (atts.getString("units").equals("m/sec")) {
                                        atts.add("units", "m s-1");
                                    } else {
                                        Test.error("Unexpected w units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Vertical Velocity");
                                    atts.add("standard_name", "sea_water_z_velocity"); //not offical standard name, but direct extension of x and y


                                } else if (varName.equals("utrans")) {
                                    if (atts.getString("units").equals("degC/sec")) {
                                        atts.add("units", "degree_C s-1");
                                    } else {
                                        Test.error("Unexpected utrans units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Zonal Temperature Transport");  //TEMP-> Temperature???
                                    atts.add("standard_name", "eastward_ocean_heat_transport"); //???

                                } else if (varName.equals("vtrans")) {
                                    if (atts.getString("units").equals("degC/sec")) {
                                        atts.add("units", "degree_C s-1");
                                    } else {
                                        Test.error("Unexpected vtrans units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Meridional Temperature Transport"); //TEMP-> Temperature???
                                    atts.add("standard_name", "northward_ocean_heat_transport"); //???

                                } else if (varName.equals("hflx")) {
                                    if (atts.getString("units").equals("watts/m^2")) {
                                        atts.add("units", "watt m-2");
                                    } else {
                                        Test.error("Unexpected hflx units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Surface Heat Flux");  
                                    atts.add("standard_name", "surface_downward_heat_flux_in_sea_water"); //???

                                } else if (varName.equals("wflx")) {
                                    if (atts.getString("units").equals("m/year")) {
                                        atts.add("units", "m year-1");
                                    } else {
                                        Test.error("Unexpected wflx units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Surface Water Flux");
                                    atts.add("standard_name", "surface_downward_water_flux"); //???

                                } else if (varName.equals("CFC11")) {
                                    if (atts.getString("units").equals("mmol/m**3")) {
                                        atts.add("units", "mmole m-3");
                                    } else {
                                        Test.error("Unexpected vtrans units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "CFC11 Concentration"); 
                                    atts.add("standard_name", "mole_concentration"); //not standard!!! but close

                                } else if (varName.equals("taux")) {
                                    if (atts.getString("units").equals("dynes/cm^2")) {
                                        atts.add("units", "dynes cm-2");  //convert to Pa???
                                    } else if (atts.getString("units").equals("N/m^2")) {
                                        atts.add("units", "N m-2");
                                    } else {
                                        Test.error("Unexpected taux units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Zonal Wind Stress"); //???
                                    atts.add("standard_name", "surface_downward_eastward_stress"); //???

                                } else if (varName.equals("tauy")) {
                                    if (atts.getString("units").equals("dynes/cm^2")) {
                                        atts.add("units", "dynes cm-2");  //convert to Pa???
                                    } else if (atts.getString("units").equals("N/m^2")) {
                                        atts.add("units", "N m-2");
                                    } else {
                                        Test.error("Unexpected tauy units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Meridional Wind Stress"); //???
                                    atts.add("standard_name", "surface_downward_northward_stress"); //???

                                } else if (varName.equals("ssh")) {
                                    if (atts.getString("units").equals("cm")) {
                                    } else if (atts.getString("units").equals("m")) {
                                    } else {
                                        Test.error("Unexpected ssh units=" + atts.getString("units"));
                                    }
                                    atts.add("long_name", "Sea Surface Height");
                                    atts.add("standard_name", "sea_surface_height_above_geoid"); //???
                                } else {
                                    throw new Exception("Unexpected varName=" + varName);
                                }
                            }

                            //define newVar in new file
                            newVars[v] = newFile.addVariable(rootGroup, varName, dataType, dimensions); 
                            NcHelper.setAttributes(nc3Mode, newVars[v], atts, NcHelper.isUnsigned(dataType));
                        }

                        //define GLOBAL metadata 
                        Attributes gatts = new Attributes();
                        String cDate = Calendar2.getCurrentISODateStringZulu();
                        NcHelper.getGlobalAttributes(oldFile, gatts);
                        gatts.add("acknowledgement", "NSF, NASA, NOAA"); //from https://www.atmos.umd.edu/~ocean/reanalysis.pdf
                        gatts.add("cdm_data_type", "Grid");
                        gatts.add("composite", "true");
                        gatts.add("contributor_name", "World Ocean Atlas, Expendable Bathythermograph Archive, " +
                            "TOGA-TAO thermistor array, Soviet SECTIONS tropical program, " +
                            "and Satellite altimetry from Geosat, ERS/1 and TOPEX/Poseidon.");
                        gatts.add("contributor_role", "source data");
                        gatts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
                        gatts.add("creator_email", "carton@umd.edu");
                        gatts.add("creator_name", "SODA");
                        gatts.add("creator_url", "https://www.atmos.umd.edu/~ocean/");
                        gatts.add("date_created", cDate);
                        gatts.add("date_issued", cDate);
                        gatts.add("Easternmost_Easting", maxLon);
                        gatts.add("geospatial_lat_max", maxLat);
                        gatts.add("geospatial_lat_min", minLat);
                        if (!Double.isNaN(latSpacing)) gatts.add("geospatial_lat_resolution", latSpacing);
                        gatts.add("geospatial_lat_units", "degrees_north");
                        gatts.add("geospatial_lon_max", maxLon);
                        gatts.add("geospatial_lon_min", minLon);
                        if (!Double.isNaN(lonSpacing)) gatts.add("geospatial_lon_resolution", lonSpacing);
                        gatts.add("geospatial_lon_units", "degrees_east");
                        gatts.add("geospatial_vertical_max", maxDepth);
                        gatts.add("geospatial_vertical_min", minDepth);
                        gatts.add("geospatial_vertical_positive", "down");
                        gatts.add("geospatial_vertical_units", "m");
                        gatts.add("history", "http://dsrs.atmos.umd.edu/\n" +
                            cDate + " NOAA SWFSC ERD added metadata and time dimension");
                        gatts.add("infoUrl", "https://www.atmos.umd.edu/~ocean/");
                        gatts.add("institution", "TAMU/UMD"); //from title
                        gatts.add("keywords", "Oceans > Ocean Temperature > Water Temperature");
                        gatts.add("keywords_vocabulary", "GCMD Science Keywords");
                        gatts.add("license", "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Creator, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.");
                        gatts.add("naming_authority", "SODA");
                        gatts.add("Northernmost_Northing", maxLat);
                        //gatts.add("origin", "TAMU/UMD"); cwhdf attribute, see institution instead
                        gatts.add("processing_level", "4 (model)");
                        gatts.add("project", "SODA (https://www.atmos.umd.edu/~ocean/)");
                        gatts.add("projection", "geographic");
                        gatts.add("projection_type", "mapped");
                        gatts.add("references", //from http://www.met.rdg.ac.uk/~swr02ldc/SODA.html
                            "Carton, J. A., Chepurin, G., Cao, X. H. and Giese, B. (2000). " +
                            "A Simple Ocean Data Assimilation analysis of the global upper ocean 1950-95. " +
                            "Part I: Methodology. Journal of Physical Oceanography, 30, 2, pp294-309. " +
                            "Carton, J. A., Chepurin, G. and Cao, X. H. (2000). A Simple Ocean Data " +
                            "Assimilation analysis of the global upper ocean 1950-95. Part II: Results. " +
                            "Journal of Physical Oceanography, 30, 2, pp311-326. " +
                            "See also https://www.atmos.umd.edu/~ocean/reanalysis.pdf .");
                        //gatts.add("satellite", "POES");   cwhdf attribute, not appropriate here
                        //gatts.add("sensor", "AVHRR GAC"); cwhdf attribute, not appropriate here
                        gatts.add("source", "model; SODA " + sodaVersion);
                        gatts.add("Southernmost_Northing", minLat);
                        gatts.add("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
                        gatts.add("summary", 
                            "Simple Ocean Data Assimilation (SODA) version " + sodaVersion + " - A reanalysis of ocean climate. " +
                            //from http://www.met.rdg.ac.uk/~swr02ldc/SODA.html
                            "SODA uses the GFDL modular ocean model version 2.2. The model is forced by observed " +
                            "surface wind stresses from the COADS data set (from 1958 to 1992) and from NCEP (after 1992). " +
                            "Note that the wind stresses were detrended before use due to inconsistencies with " +
                            "observed sea level pressure trends. The model is also constrained by constant assimilation " +
                            "of observed temperatures, salinities, and altimetry using an optimal data assimilation " +
                            "technique. The observed data comes from: " +
                            "1) The World Ocean Atlas 1994 which contains ocean temperatures and salinities from " +
                            "mechanical bathythermographs, expendable bathythermographs and conductivity-temperature-depth probes. " +
                            "2) The expendable bathythermograph archive " +
                            "3) The TOGA-TAO thermistor array " +
                            "4) The Soviet SECTIONS tropical program " +
                            "5) Satellite altimetry from Geosat, ERS/1 and TOPEX/Poseidon. \n" +
                            //from https://www.atmos.umd.edu/~ocean/history.html
                            "We are now exploring an eddy-permitting reanalysis based on the Parallel Ocean Program " +
                            "POP-1.4 model with 40 levels in the vertical and a 0.4x0.25 degree displaced pole grid " +
                            "(25 km resolution in the western North Atlantic).  The first version of this we will release " +
                            "is SODA1.2, a reanalysis driven by ERA-40 winds covering the period 1958-2001 (extended " +
                            "to the current year using available altimetry). ");
                        //has title
                        gatts.add("Westernmost_Easting", minLon);
                        //set the globalAttributes
                        NcHelper.setAttributes(nc3Mode, rootGroup, gatts);
                    
                        //leave define mode
                        newFile.create();

                        //write data for each variable
                        for (int v = 0; v < oldVars.size(); v++) {
                            Variable oldVar = oldVars.get(v);
                            String name = oldVar.getName();

                            //if lon, lat, depth
                            if (name.equals("lon") || name.equals("lat") || name.equals("depth")) {
                                //just read it and write it unchanged
                                newFile.write(newVars[v], oldVar.read());

                            //if time
                            } else if (name.equals("time")) {
                                //just read it and write it unchanged
                                newFile.write(newVars[v], NcHelper.get1DArray(new int[]{months}, false)); 

                            //if other variables
                            } else {
                                //read it
                                Array array = oldVar.read();
                                //add time dimension
                                int oldShape[] = array.getShape();
                                int newShape[] = new int[oldShape.length + 1];
                                newShape[0] = 1;
                                System.arraycopy(oldShape, 0, newShape, 1, oldShape.length);
                                array = array.reshape(newShape);
                                //write it
                                newFile.write(newVars[v], array);
                            }
                        }
                        newFile.close();
                        newFile = null;

                    } finally {
                        try {oldFile.close(); } catch (Exception e) {}
                        oldFile = null;
                    }
                } finally {
                    try { if (newFile != null) newFile.abort(); } catch (Exception e) {}
                    newFile = null;
                }

                if (gzipped) 
                    File2.delete(tempDir + cdfName);

                String2.log("newFile=" + NcHelper.ncdump(newDir + newName, "-h"));

            } //end file loop

            String2.log("Projects.soda finished converting " + oldDir + " successfully.");

        } catch (Exception e) {
            try {oldFile.close();} catch (Exception e2) {}
            try {newFile.close();} catch (Exception e2) {}
            String2.log(MustBe.throwableToString(e));
        }
    }

    /** Can netcdf-java write longs (64bit integers) into a nc3 file? 
     * 2017-02-08:
java.lang.IllegalArgumentException: illegal dataType: long not supported in netcdf-3
 at ucar.nc2.NetcdfFileWriter.addVariable(NetcdfFileWriter.java:538)
 at ucar.nc2.NetcdfFileWriter.addVariable(NetcdfFileWriter.java:518)
 at gov.noaa.pfel.coastwatch.Projects.testLongInNc3(Projects.java:3237)    +5
 at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:442)
     */
    public static void testLongInNc3() throws Exception {
        String2.log("\n*** Projects.testLongInNc3");

        //get a list of files
        String dirFileName = "/data/ethan/testLongInNc3.nc";
        NetcdfFileWriter newFile = newFile = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf3, dirFileName);
        try {
            
            boolean nc3Mode = true;
            Group rootGroup = newFile.addGroup(null, "");

            //create the dimensions
            Dimension dim = newFile.addDimension(rootGroup, "row", 5);
            ArrayList<Dimension> dims = new ArrayList();
            dims.add(dim);

            ArrayList<Dimension> dimensions = new ArrayList();
            DataType dataType = DataType.LONG;

            Attributes atts = new Attributes();
            atts.set("units", "count");

            Variable var = newFile.addVariable(rootGroup, "longs", dataType, dims); 
            NcHelper.setAttributes(nc3Mode, var, atts, NcHelper.isUnsigned(dataType));

            //define GLOBAL metadata 
            Attributes gatts = new Attributes();
            gatts.set("title", "test of 64bit integers");
            NcHelper.setAttributes(nc3Mode, rootGroup, gatts);
        
            //leave define mode
            newFile.create();

            ArrayLong.D1 array = new ArrayLong.D1(5, false); //isUnsigned
            array.set(0, Long.MIN_VALUE);
            array.set(0, -999);
            array.set(0, 0);
            array.set(0, 999);
            array.set(0, Long.MAX_VALUE);

            newFile.write(var, array);

            newFile.close(); newFile = null;

            String2.log("newFile=" + NcHelper.ncdump(dirFileName, ""));

            String2.log("\n*** Projects.testLongInNc3 finished successfully.");

        } catch (Exception e) {
            try {if (newFile != null) newFile.abort();} catch (Exception e2) {}
            String2.log(MustBe.throwableToString(e));
        }
    }


    /**
     * This is a test of reading a coastwatch .hdf file (as they distribute)
     * with the new netcdf-java 4.0 library.
     * With the hope of making a thredds iosp for these files (see 
     * https://www.unidata.ucar.edu/software/netcdf-java/tutorial/IOSPoverview.html ).
     *
     * @throws Exception if trouble
     */
    public static void testHdf4() throws Exception {
        /*  
        //one time - change ucar to ucar4
        String[] list = RegexFilenameFilter.recursiveFullNameList(
            "c:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/ucar4", ".*\\.java", false);
        for (int i = 0; i < list.length; i++) {
            String content[] = String2.readFromFile(list[i]);
            if (content[0].length() == 0) {
                String2.log("processing " + list[i]);
                content[1] = String2.replaceAll(content[1], "ucar.", "ucar4.");
                String2.writeToFile(list[i], content[1]);
            } else {
                String2.log(content[0]);
            } 
        }
        // */


        /* 
        //ensure compiled:
        ucar4.nc2.NetcdfFile nc4; //changed to encourage compilation of iosp classes
        ucar4.nc2.iosp.netcdf3.SPFactory spf;

        String fileName = "c:/temp/2008_062_0118_n15_ax.hdf";
        //String fileName = "c:/temp/MODSCW_P2008063_C3_1750_1755_1930_CB05_closest_chlora.hdf";

        //trying to read with my code fails 
        //SdsReader.verbose = true;
        //SdsReader sr = new SdsReader(fileName);

        //the netcdf-java way
        ucar4.nc2.NetcdfFile nc = ucar4.nc2.NetcdfFile.open(fileName);
        try {
            String2.log(nc.toString());
            ucar4.nc2.Variable v = nc.findVariable("avhrr_ch1");
            ucar4.ma2.Array a = v.read("0:100:10,0:100:10");   //start:end:stride,start:end:stride
            String2.log(a.toString());
        } finally {
            nc.close();
        }
        // */

        // /* 
        //the netcdf-java way
        String fileName = "c:/data/seawifs/L3bin/S20010012001008.L3b_8D_CHL.main";
        NetcdfFile nc = NetcdfFile.open(fileName);
        try {
            String2.log(nc.toString());
            //ucar4.nc2.Variable v = nc.findVariable("avhrr_ch1");
            //ucar4.ma2.Array a = v.read("0:100:10,0:100:10");   //start:end:stride,start:end:stride
            //String2.log(a.toString());
        } finally {
            nc.close();
        }
        // */

    }

    /** A test of Janino.
     *
     * Compile the expression once; relatively slow.
     * <li><a href="http://www.janino.net/">Janino</a> is a Java compiler
     *    which is useful for compiling and then evaluating expressions at runtime
     *    (Copyright (c) 2001-2007, Arno Unkrig, All rights reserved; license: 
     *    <a href="http://www.janino.net/javadoc/org/codehaus/janino/doc-files/new_bsd_license.txt">BSD</a>).
     */
/* This works, but janino.jar not currently in classpath or in /lib
public static void testJanino() throws Exception {
        if (true) {
            //Janino
            ExpressionEvaluator ee = new ExpressionEvaluator(
                "c > d ? c : d",                     // expression
                PAType.INT,                           // expressionType
                new String[] { "c", "d" },           // parameterNames
                new PAType[] { PAType.INT, PAType.INT } // parameterTypes
            );
            Integer res = (Integer) ee.evaluate(
                new Object[] {          // parameterValues
                    new Integer(10),
                    new Integer(11),
                }
            );
            System.out.println("res = " + res);
        }
        if (true) {
            ExpressionEvaluator ee = new ExpressionEvaluator(
                "import com.cohort.util.Calendar2; Calendar2.isoStringToEpochSeconds(a[0] + \"T\" + a[1])",    // expression
                PAType.DOUBLE,                 // expressionType
                new String[] {"a" },          // array of parameterNames
                new PAType[] {String[].class } // array of parameterTypes, e.g., PAType.INT, or String[].class
            );

            // Evaluate it with varying parameter values; very fast.
            String2.log("result=" + 
                (Double)ee.evaluate(new Object[] {   // parameterValues
                    new String[]{"1970-01-02", "12:00:00"}})
                );
        }
    }
*/
    /**
     * A test of getting Grids from .nc files.
     *
     */
    public static void testGetNcGrids() throws Exception {
        if (true) { 
            //getGrids test
            //NetcdfDataset ncd = NetcdfDataset.openDataset("c:/temp/cwsamples/MODSCW_P2008045_P2008105_D61_GM05_closest_chlora.nc");
            NetcdfDataset ncd = NetcdfDataset.openDataset("c:/temp/cwsamples/MODSCW_P2008073_2010_D61_P2007351_P2008046_GM03_closest_R667ANOMALY.nc");
            try {
                List list = ncd.getCoordinateSystems();
                System.out.println("nCoordSystems=" + list.size());
                System.out.println("coordinateSystem0=" + list.get(0));
                ucar.nc2.dt.grid.GridDataset gd = new ucar.nc2.dt.grid.GridDataset(ncd);
                try {
                    List gdList = gd.getGrids();
                    System.out.println("nGrids=" + gdList.size());
                    System.out.println("grid=" + gdList.get(0));
                } finally {
                    gd.close();
                }
            } finally {
                ncd.close();
            }
        }
        //Grid.testHDF(new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser"), false);
        //"C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/gov/noaa/pfel/coastwatch/griddata/OQNux10S1day_20050712_x-135_X-105_y22_Y50Test.hdf";
        //File2.copy("c:/TestDapperBackendService.nc", "c:/zztop/testFileCopy.nc");
        //String2.log(" -5%4=" + (-5%4) + " 5%-4=" + (5%-4) + " -5%-4=" + (-5%-4));
        if (false) {
            //ucar.nc2.util.DebugFlags.set("DODS/serverCall", true);
            //NetcdfFile nc = NetcdfDataset.openFile("http://apdrc.soest.hawaii.edu/dapper/godae/argo_all.cdp", null);
            //NetcdfFile nc = NetcdfDataset.openFile("https://coastwatch.pfeg.noaa.gov/erddap2/tabledap/cwwcNDBCMet", null);
            NetcdfFile nc = NetcdfDataset.openFile("http://localhost/cwexperimental/tabledap/cwwcNDBCMet", null);
            //NetcdfFile nc = NetcdfDataset.openFile("https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/cwtest/aqua/modis/chlora/D1", null);
            String2.log(nc.toString());
            nc.close();
        }
    }

    /** Test wobbly lon and lat values in AGssta. */
    public static void testWobblyLonLat() throws Exception {
        //test of "wobbly" lat and lon values in AGssta 14day
        Grid grid = new Grid();
        //grid.readGrd("c:/programs/roy/AG2006009_2006022_ssta_westus.grd",
        grid.readNetCDF("c:/programs/roy/AG2006009_2006022_ssta.nc", null);
        String2.log("lon=" + String2.toCSSVString(grid.lon) + "\nlat=" + String2.toCSSVString(grid.lat));
        int nLon = grid.lon.length;
        double maxLonDif = 0;
        double dLon[] = new double[nLon];
        for (int i = 0; i < nLon; i++) {
            dLon[i] = (float)grid.lon[i];
            if (i > 0)
                maxLonDif = Math.max(maxLonDif, Math.abs(dLon[i]-dLon[i-1] -.1));
        }
        int nLat = grid.lat.length;
        double maxLatDif = 0;
        for (int i = 1; i < nLat; i++) 
            maxLatDif = Math.max(maxLatDif, Math.abs(grid.lat[i]-grid.lat[i-1]));
        String2.log("maxLonDif=" + maxLonDif + " maxLatDif = " + maxLatDif);
            //+ "\ndLon=" + String2.toCSSVString(dLon));
    }

    /** A tunnel test for ERDDAP. */
    public static void erddapTunnelTest() throws Exception {
        String url = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdCMsfc";
        String varName = "eastCurrent";
        SSR.genericTunnelTest(1000, url + ".csv", varName); 
        SSR.genericTunnelTest(1000, url + ".nc",  varName); 
        SSR.genericTunnelTest(1000, url + ".csv", varName); 
        SSR.genericTunnelTest(1000, url + ".nc",  varName); 
    }

    /** A test of Opendap availability. 
     *
     * @param maxSeconds specifies the delay between tests: a random number between 0 and maxSeconds.
     * @param thredds if true, the requests include lon and lat.
     *     If false, they include longitude and latitude.
     */
    public static void testOpendapAvailability(String opendapBaseUrl, String varName,
        int nIterations, int maxSeconds, boolean thredds) throws Exception {

        boolean tVerbose = true;
        String2.log("testOpendapAvailablity nIterations=" + nIterations + ", maxSeconds=" + maxSeconds +
            "\nopendapBaseUrl=" + opendapBaseUrl);
        int nFailures = 0;
        long cumTime = 0;
        long clockTime = System.currentTimeMillis();
        int distribution[] = new int[String2.DistributionSize];

        for (int i = 0; i < nIterations; i++) {
            //get dataset info
            String response1="", response2="", response3="", response4="", response5="", response6="";
            long time = System.currentTimeMillis(), 
                tTime1=-1, tTime2=-1, tTime3=-1, tTime4=-1, tTime5=-1, tTime6=-1;
            try {
                tTime1 = System.currentTimeMillis();
                response1 = SSR.getUrlResponseStringUnchanged(opendapBaseUrl + ".das");
                tTime1 = System.currentTimeMillis() - tTime1;

                tTime2 = System.currentTimeMillis();
                response2 = SSR.getUrlResponseStringUnchanged(opendapBaseUrl + ".dds");
                tTime2 = System.currentTimeMillis() - tTime2;

                tTime3 = System.currentTimeMillis();
                response3 = SSR.getUrlResponseStringUnchanged(opendapBaseUrl + ".asc?time");      
                tTime3 = System.currentTimeMillis() - tTime3;

                tTime4 = System.currentTimeMillis();
                response4 = SSR.getUrlResponseStringUnchanged(opendapBaseUrl + ".asc?lat" + (thredds? "" : "itude"));      
                tTime4 = System.currentTimeMillis() - tTime4;

                tTime5 = System.currentTimeMillis();
                response5 = SSR.getUrlResponseStringUnchanged(opendapBaseUrl + ".asc?lon" + (thredds? "" : "gitude"));      
                tTime5 = System.currentTimeMillis() - tTime5;

                tTime6 = System.currentTimeMillis();
                response6 = SSR.getUrlResponseStringUnchanged(opendapBaseUrl + ".asc?" + varName + 
                    "[" + Math2.random(100) + "][0]" +
                    "[" + Math2.random(20) + "]" +
                    "[" + Math2.random(20) + "]");      
                tTime6 = System.currentTimeMillis() - tTime6;

                time = System.currentTimeMillis() - time;
                cumTime += time;
                String2.distribute(time, distribution);
            } catch (Exception e) {
                time = -1;
                nFailures++;
                String2.log(MustBe.throwableToString(e));
            }

            //outside of timings 
            if (i == 0) String2.log("\n***r1\n" + response1 + "\n***r2\n" + response2 + 
                "\n***r3\n" + response3 + "\n***r4\n" + response4 + "\n***r5\n" + response5 + 
                "\n***r6\n" + response6);

            //wait 0 - maxMinutes
            int sec = Math2.random(maxSeconds);
            if (i== 0) String2.log("\niter   response ms   .das   .dds   time    lat    lon  datum   sleep sec");
            String2.log(
                String2.right("" + i, 4) + 
                String2.right("" + time, 14) + 
                String2.right("" + tTime1, 7) + 
                String2.right("" + tTime2, 7) + 
                String2.right("" + tTime3, 7) + 
                String2.right("" + tTime4, 7) + 
                String2.right("" + tTime5, 7) + 
                String2.right("" + tTime6, 7) + 
                String2.right("" + sec, 12));
            Math2.sleep(sec * 1000);
        }
        String2.log("\ntestOpendapAvailablity finished nIterations=" + nIterations + ", maxSeconds=" + maxSeconds +
            "\nopendapBaseUrl=" + opendapBaseUrl +
            "\nclockTime=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - clockTime) +
            "\ncumulativeTime=" + (cumTime/1000) + "sec, nFailures=" + nFailures + 
            "\nresponseMsDistribution:\n" +
            String2.getDistributionStatistics(distribution));
    }


    /**
     * This does a bunch of downloads from ERDDAP to files with chunks of the leftmost dimension.
     */    
    public static void downloadFilesFromErddap() throws Exception {
         //setup
         String erddapUrl = "http://localhost:8080/cwexperimental/griddap/";
         String datasetID = "usgsCeSrtm30v6c";
         String varName = "topo"; //presumably only one
         int n = 21600;
         int inc = 2000;
         String baseDir = "c:/u00/data"; 
         boolean compress = false; //I think no need since erddap is on this computer, transmission is fast

         //do it
         String baseUrl =  erddapUrl + datasetID + ".nc?" + varName;
         File2.makeDirectory(baseDir + datasetID);
         for (int i = 0; i < n; i += inc) {
             String url  = baseUrl + "[" + i + ":" + Math.min(i + inc - 1, n - 1) + "][]"; //add [] if needed
             String file = baseDir + datasetID + "/" + datasetID + "_" + i + ".nc";
             String2.log("\n url=" + url + "\nfile=" + file);
             long time = System.currentTimeMillis();
             SSR.downloadFile(url, file, compress); //throws Exception
             String2.log("time=" + (System.currentTimeMillis() - time)/1000 + "s"); 
         }
    }
        
    /** 
     * This was used by Bob to convert the source NewportCTD .csv data into .nc files
     * suitable for ERDDAP EDDTableFromNcFiles.
     * <br>Lynn made the .csv files 2009-12-31.
     *    ftp://192.168.31.10/outgoing/ldewitt/Simons/northwest/
     * <br>Source URL http://192.168.31.13/cgi-bin/ERDserver/northwest.sql.das .
     */
    public static void convertNewportCTD() throws Exception {
        String2.log("\n*** EDDTableFromNcFiles.convertNewportCTD");
        String sourceDir = "c:/data/rawSource/newportCTD2009-12/";
        String sourceCsv = "CTD_NH.csv";
        String sourceLatLon = "NH_Target_LatLong.csv";
        String destDir = "c:/u00/data/points/erdNewportCtd/";
        float mv = -9999;
        int factor = 10000;

        String dataColNames[] = String2.split(
            "station_code, date,         station,      local_time,   depth_or_pressure, " + 
            "temperature,  salinity,     density,      fluorescence, project, " +
            "transect", ',');
        PAType dataColTypes[] = {
            PAType.STRING,  PAType.STRING, PAType.STRING, PAType.STRING, PAType.FLOAT,
            PAType.FLOAT,   PAType.FLOAT,  PAType.FLOAT,  PAType.FLOAT,  PAType.STRING,
            PAType.STRING};
        String dataUnits[] = { //date will be ...
            null,          "seconds since 1970-01-01T00:00:00Z", null, null, "meters",
            "degree_C",    "1e-3",       "sigma",      "volts",      null, //1e-3 replaces PSU in CF std names 25
            null};
      
        Test.ensureEqual(dataColNames.length, dataColTypes.length, "dataColNames.length != dataColTypes.length");

        String latLonColNames[] = String2.split(
            "line,        station,      latitude,    longitude,   transect", ',');
        //schema has PAType.DOUBLE for lat, lon, but I think not necessary
        PAType latLonColTypes[] = {
            PAType.STRING, PAType.STRING, PAType.FLOAT, PAType.FLOAT, PAType.STRING};

        //find timezone   America/Los_Angeles
        //String2.log(String2.toCSSVString(ZoneId.getAvailableZoneIDs().toArray()));
        //Test.ensureTrue(false, "");

        //recursively delete any files in destDir 
        File2.deleteAllFiles(destDir, true, true);
         
        //read the data source file
        String2.log("\nreading the data source file");
        Table dataTable = new Table();
        dataTable.readASCII(sourceDir + sourceCsv, String2.ISO_8859_1, 
            "", "", //skipHeaderToRegex, skipLinesRegex,
            -1, 0, "", 
            null, null, null, null, false); //don't simplify
        Test.ensureEqual(dataTable.nColumns(), dataColNames.length, "dataTable.nColumns() != dataColNames.length");
        String2.log("");

        //find bad rows?   there is 1, 48358, remove it
        PrimitiveArray depthPa = dataTable.getColumn(4);
        for (int row = 0; row < depthPa.size(); row++) {
            if (depthPa.getString(row).length() == 0) {
                String2.log("!!! row=" + row + " has no depth_or_pressure.  Removing it...");
                dataTable.removeRow(row);
            }
        }
        String2.log("");

        for (int col = 0; col < dataColNames.length; col++) {
            //set the column name
            dataTable.setColumnName(col, dataColNames[col]);

            //set the units 
            if (dataUnits[col] != null)
                dataTable.columnAttributes(col).set("units", dataUnits[col]);

            //change the columnType
            if (dataColTypes[col] != PAType.STRING) {
                PrimitiveArray pa = PrimitiveArray.factory(dataColTypes[col], 1, false);
                pa.append(dataTable.getColumn(col));
                dataTable.setColumn(col, pa);
            }

            //set data mv to mv (-9999)
            if (col >= 4 && col <= 8) {
                PrimitiveArray pa = dataTable.getColumn(col);
                String2.log(pa.switchFromTo("", "" + mv) + //the mv is packed, too
                    " " + dataColNames[col] + " values converted from '' to " + mv);
                if (col == 8) {
                    //fluorescence has mv of -999999 and ""  
                    //  and bruised double values (obviously originally floats)
                    String2.log(pa.switchFromTo("-999999", "" + mv) + 
                        " fluorescence values converted from -999999 to " + mv);
                }
                pa.scaleAddOffset(factor, 0);
                pa = new IntArray(pa);
                dataTable.setColumn(col, pa);
                dataTable.columnAttributes(col).set("missing_value", 
                    Math2.roundToInt(mv * factor));  //missing_value is packed, too
                dataTable.columnAttributes(col).set("scale_factor", 1/(float)factor);  //float
            }

            //convert "Ship of Opportu" 
            if (col == 9) {
                PrimitiveArray pa = dataTable.getColumn(col);
                String2.log(pa.switchFromTo("Ship of Opportu", "Ship of Opportunity") + 
                    " project values converted from \"Ship of Opportu\".");
            }

            //convert transect "" to "Newport Hydrographic" ???
            if (col == 10) {
                PrimitiveArray pa = dataTable.getColumn(col);
                String2.log(pa.switchFromTo("", "Newport Hydrographic") + 
                    " transect values converted from \"\" to \"Newport Hydrographic\".");
            }


        }

        //sort   (so all data for a given stationCode will be stored together)
        String2.log("\nsorting\n");
        dataTable.sort(new int[]{0,4}, new boolean[]{true, true}); //stationCode, depth


        //make time (Z) from date and local_time "04/20/2007 12:00:00 AM,NH125,12/30/1899 12:04:00 AM"
        StringArray datePa = (StringArray)dataTable.findColumn("date");
        StringArray localTimePa = (StringArray)dataTable.findColumn("local_time");
        DoubleArray timePa = new DoubleArray();
        String dateTimeFormat = "MM/dd/yyyy hh:mm:ss a";
        String timeZoneString = "America/Los_Angeles";
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
        DateTimeFormatter dateTimeFormatter = 
            Calendar2.makeDateTimeFormatter(dateTimeFormat, timeZoneString);
        for (int row = 0; row < datePa.size(); row++) {
            String tDate = datePa.get(row);
            if (tDate.length() == 0) {
                timePa.add(Double.NaN);
                continue;
            }
            Test.ensureEqual(tDate.substring(10), " 12:00:00 AM", 
                "Unexpected date on row=" + row);
            String tLocal = localTimePa.get(row);
            if (tLocal.length() > 0) {
                Test.ensureEqual(tLocal.substring(0, 11), "12/30/1899 ", 
                    "Unexpected local_time date on row=" + row);
                tDate = tDate.substring(0, 10) + tLocal.substring(10);
            }
            //Newport, OR is same time zone as Pacific Grove. so just use default local time zone.
            double sec = Calendar2.parseToEpochSeconds(tDate, dateTimeFormat, timeZone);
            if (row == 0 || row == 6053)
                String2.log("time example: row=" + row + " \"" + tDate + "\" was converted to " + 
                    Calendar2.safeEpochSecondsToIsoStringTZ(sec, ""));
            timePa.add(sec);
        }
        dataTable.setColumn(1, timePa);
        dataTable.setColumnName(1, "time");
        //remove local_time
        dataTable.removeColumn("local_time");


        //read the latLon file
        String2.log("\nreading the latLon source file");
        Table latLonTable = new Table();
        latLonTable.readASCII(sourceDir + sourceLatLon, String2.ISO_8859_1,
            "", "", -1, 0, "", null, null, null, null, true);
        Test.ensureEqual(latLonTable.nColumns(), latLonColNames.length, "latLonTable.nColumns() != latLonColNames.length");
        for (int col = 0; col < latLonColNames.length; col++) {
            //set the column name
            latLonTable.setColumnName(col, latLonColNames[col]);

            //change the columnType
            if (latLonColTypes[col] != PAType.STRING) {
                PrimitiveArray pa = PrimitiveArray.factory(latLonColTypes[col], 1, false);
                pa.append(latLonTable.getColumn(col));
                latLonTable.setColumn(col, pa);
            }
        }

        //make/insert lon lat columns
        String2.log("\nmake/insert lon lat columns");
        StringArray llLinePa    = (StringArray)latLonTable.findColumn("line");
        StringArray llStationPa = (StringArray)latLonTable.findColumn("station");
        PrimitiveArray lla = latLonTable.findColumn("latitude");
        lla.scaleAddOffset(factor, 0);
        IntArray  llLatPa       = new IntArray(lla);
        lla = latLonTable.findColumn("longitude");
        lla.scaleAddOffset(factor, 0);
        IntArray  llLonPa       = new IntArray(lla);

        //add some missing stations   
        //(location calculated by interpolation - Roy said number is distance in km)
        for (int i = 0; i < 4; i++) {
            llLinePa.add("NH");
            llLatPa.add(446517);
        }
        llStationPa.add("NH02");  llLonPa.add(-1241150);
        llStationPa.add("NH12");  llLonPa.add(-1243416);
        llStationPa.add("NH30");  llLonPa.add(-1247667);
        llStationPa.add("NH75");  llLonPa.add(-1258250);


        StringArray newPlainStationPa = new StringArray();
        StringArray newLinePa         = new StringArray();
        StringArray oldStationPa      = (StringArray)dataTable.findColumn("station");
        IntArray  newLatPa            = new IntArray();
        IntArray  newLonPa            = new IntArray();

        String oPlainStation = "";
        for (int row = 0; row < oldStationPa.size(); row++) {
            String plainStation = oldStationPa.getString(row);
            //remove suffix letter
            while (String2.isLetter(plainStation.charAt(plainStation.length() - 1)))
                plainStation = plainStation.substring(0, plainStation.length() - 1);
            newPlainStationPa.add(plainStation);
            int po = llStationPa.indexOf(plainStation);
            Test.ensureTrue(po >= 0, "plainStation=" + plainStation + " not found starting on row=" + row);
            newLinePa.add(po < 0? "": llLinePa.get(po));
            newLatPa.add( po < 0? Math2.roundToInt(mv*factor): llLatPa.get(po));
            newLonPa.add( po < 0? Math2.roundToInt(mv*factor): llLonPa.get(po));
            oPlainStation = plainStation;
        }
        dataTable.addColumn(3, "plain_station",  newPlainStationPa, 
            new Attributes().add("description", "The station without the suffix."));
        dataTable.addColumn(0, "line",     newLinePa, new Attributes());
        dataTable.addColumn(1, "longitude", newLonPa, 
            (new Attributes()).add("units", "degrees_east").add("scale_factor", 1/(float)factor));
        dataTable.addColumn(2, "latitude", newLatPa,  
            (new Attributes()).add("units", "degrees_north").add("scale_factor", 1/(float)factor));

        String2.log("\ncolumnNames=" + String2.toCSSVString(dataTable.getColumnNames()) + "\n");

        //save in files
        StringArray oldStationCodePa = (StringArray)dataTable.findColumn("station_code");
        String lastStationCode = oldStationCodePa.get(0);
        int startRow = 0;
        int nRows = oldStationCodePa.size();
        for (int row = 0; row < nRows; row++) {
            if (row == nRows - 1 || !oldStationCodePa.get(row).equals(lastStationCode)) {
                int lastRow = row == nRows - 1? row : row - 1;
                Test.ensureTrue(oldStationPa.get(    row).length() > 0, "row=" + row + " station=''"); 
                Test.ensureTrue(oldStationCodePa.get(row).length() > 0, "row=" + row + " oldStation=''"); 
                String eStation     = String2.encodeFileNameSafe(oldStationPa.get(row));
                String eStationCode = String2.encodeFileNameSafe(oldStationCodePa.get(row));                
                String fullName = destDir + eStation + "/" + eStationCode + ".nc";
                File2.makeDirectory(destDir + eStation + "/");

                Table table = new Table();
                for (int col = 0; col < dataTable.nColumns(); col++) {
                    PrimitiveArray oldPa = dataTable.getColumn(col);
                    PrimitiveArray newPa = PrimitiveArray.factory(oldPa.elementType(), 
                        lastRow - startRow + 1, false);
                    for (int tRow = startRow; tRow <= lastRow; tRow++) 
                        newPa.addString(oldPa.getString(tRow));
                    table.addColumn(col, dataTable.getColumnName(col), newPa, 
                        (Attributes)(dataTable.columnAttributes(col).clone()));
                }
                table.saveAsFlatNc(fullName, "row", false);

                if (startRow < 100 || row == nRows - 1) 
                    String2.log(table.toString());
                //if (startRow > 100) Test.ensureTrue(false, "Evaluate the tables.");

                lastStationCode = oldStationCodePa.get(row);
                startRow = lastRow + 1;
            }
        }
        String2.log("Finished!");

    }

    /** 
     * This was used by Bob to convert the source Calcatch .csv data into .nc files
     * suitable for ERDDAP EDDTableFromNcFiles.
     * <br>Lynn made the .csv files 2009-12-31.
     *    ftp://192.168.31.10/outgoing/ldewitt/Simons/calcatch/
     * <br>This data is different from Jan's similar data.
     */
    public static void convertFedCalLandings() throws Exception {
        String2.log("\n*** EDDTableFromNcFiles.convertFedCalLandings");
        String sourceDir = "c:/data/rawSource/fedCalLandings2010-01-05/";
        String sourceCsv = "dbo_block_summary.csv";
        String sourceMarCat = "dbo_market_categories.csv";
        String destDir = "c:/u00/data/points/fedCalLandings/";
        int mv = -9999;
        double timeNaN = -1e30;

        String dataColNames[] = String2.split(
/* 	region			Int8 (4), 
	year			Int8 (4), 
	mark_cat		Int8 (4), 
	month			Int8 (4), 
	block			Int8 (4), 
	pounds			Char (76),       really!!! why?
	area			Char (50), 
	imported		Char (2), 
	region_caught	Int8 (4) */
            "region,       year,         market_category, month,          block, " + 
            "pounds,       area,         imported,        region_caught", ',');
        PAType dataColTypes[] = { //month and region_caught could be byte, but mv=-9999 wouldn't fit
            PAType.SHORT,   PAType.SHORT,  PAType.SHORT,     PAType.SHORT,     PAType.SHORT,
            PAType.INT,     PAType.STRING, PAType.STRING,    PAType.SHORT};
        String dataUnits[] = { //date will be ...
            null,          null,         null,            null,           null,
            "pounds",      null,         null,            null};
      
        Test.ensureEqual(dataColNames.length, dataColTypes.length, "dataColNames.length != dataColTypes.length");

        //recursively delete any files in destDir 
        File2.deleteAllFiles(destDir, true, true);
         
        //read the data source file
        String2.log("\nreading the data source file");
        Table dataTable = new Table();
        dataTable.readASCII(sourceDir + sourceCsv, String2.ISO_8859_1,  
            "", "", //skipHeaderToRegex, skipLinesRegex,
            -1, 0, "", null, null, null, null, false);  //don't simplify
        Test.ensureEqual(dataTable.nColumns(), dataColNames.length, "dataTable.nColumns() != dataColNames.length");
        String2.log("");

        //find bad rows (no market category)?
        PrimitiveArray tPa = dataTable.getColumn(2);
        for (int row = 0; row < tPa.size(); row++) {
            if (tPa.getString(row).length() == 0) {
                String2.log("!!! data row=" + row + " has no marCat.  Removing it...");
                dataTable.removeRow(row);
            }
        }
        String2.log("");

        for (int col = 0; col < dataColNames.length; col++) {
            //set the column name
            dataTable.setColumnName(col, dataColNames[col]);

            //set the units 
            if (dataUnits[col] != null)
                dataTable.columnAttributes(col).set("units", dataUnits[col]);

            //change the columnType
            if (dataColTypes[col] != PAType.STRING) {
                PrimitiveArray pa = PrimitiveArray.factory(dataColTypes[col], 1, false);
                PrimitiveArray opa = dataTable.getColumn(col);
                //ensure dataColType is appropriate
                int n = opa.size();
                int max = dataColTypes[col] == PAType.SHORT? Short.MAX_VALUE :
                          dataColTypes[col] == PAType.INT? Integer.MAX_VALUE :
                          -1;
                Test.ensureTrue(max != -1, "Unrecognized dataColType for col=" + col);
                for (int row = 0; row < n; row++) {
                    String s = opa.getString(row);
                    int ti = String2.parseInt(s);
                    if ((s.length() > 0 && ti == Integer.MAX_VALUE) ||  //e.g., a word
                        (ti < Integer.MAX_VALUE && ti >= max))
                        Test.ensureTrue(false, "invalid value: col=" + col + " row=" + row + 
                            " s=" + s + " ti=" + ti + " max=" + max);
                }
                pa.append(opa);
                dataTable.setColumn(col, pa);

                //set missing value to mv
                String2.log(pa.switchFromTo("", "" + mv) + 
                    " " + dataColNames[col] + " values converted from '' to " + mv);
                dataTable.columnAttributes(col).set("missing_value", mv);  
            }

            //convert "Ship of Opportu" 
            //if (col == 9) {
            //    PrimitiveArray pa = dataTable.getColumn(col);
            //    String2.log(pa.switchFromTo("Ship of Opportu", "Ship of Opportunity") + 
            //        " project values converted from \"Ship of Opportu\".");
            //}

        }

        //sort   (so all data for a given stationCode will be stored together)
        String2.log("\nsorting dataTable\n");
        dataTable.leftToRightSort(5);  //region, year, month, market_category, block

        //make time (Z) from year and month
        PrimitiveArray yearPa  = dataTable.findColumn("year");
        PrimitiveArray monthPa = dataTable.findColumn("month");
        DoubleArray timePa = new DoubleArray();
        int nTimeNaN = 0;
        for (int row = 0; row < yearPa.size(); row++) {
            int year = yearPa.getInt(row);
            int month = monthPa.getInt(row);
            if (year == mv || month == mv) {
                timePa.add(timeNaN);
                nTimeNaN++;
                continue;
            }
            GregorianCalendar gc = Calendar2.newGCalendarZulu(year, month, 15);
            timePa.add(Math2.roundToDouble(gc.getTimeInMillis() / 1000.0));
            if (row % 10000 == 0)
                String2.log("row=" + row + " year=" + year + " month=" + month + " time=" + 
                    Calendar2.epochSecondsToIsoStringTZ(timePa.get(row)));
        }
        String2.log("\nnTimeNaN=" +nTimeNaN + "\n");
        Test.ensureEqual(timePa.size(), yearPa.size(), "timePa size is incorrect.");
        dataTable.addColumn(dataTable.nColumns(), "time", timePa, 
            (new Attributes()).add("units", Calendar2.SECONDS_SINCE_1970).add("missing_value", timeNaN));

        //read the marCat file
        String marCatColNames[] = String2.split(
/*  market_category		Int8 (4), 
	description			Char (200), 
	nominal_species		Char (8), 
	species_group		Char (20), 
	comments			Char (240) */
            "market_category, description, nominal_species, species_group, comments", ',');
        //schema has PAType.DOUBLE for lat, lon, but I think not necessary
        PAType marCatColTypes[] = {
            PAType.SHORT, PAType.STRING, PAType.STRING, PAType.STRING, PAType.STRING};

        String2.log("\nreading the marCat source file");
        Table marCatTable = new Table();

        marCatTable.readASCII(sourceDir + sourceMarCat, String2.ISO_8859_1,
            "", "", -1, 0, "", null, null, null, null, true);
        Test.ensureEqual(marCatTable.nColumns(), marCatColNames.length, "marCatTable.nColumns() != marCatColNames.length");
        for (int col = 0; col < marCatColNames.length; col++) {
            //set the column name
            marCatTable.setColumnName(col, marCatColNames[col]);

            //change the columnType
            if (marCatColTypes[col] != PAType.STRING) {
                PrimitiveArray pa = PrimitiveArray.factory(marCatColTypes[col], 1, false);
                pa.append(marCatTable.getColumn(col));
                marCatTable.setColumn(col, pa);
            }
        }

        //sort marCatTable so can use binarySearch
        String2.log("\nsorting marCatTable\n");
        marCatTable.leftToRightSort(1); //marCat
String2.log(marCatTable.toString());

        //generate the marCat columns for the dataTable
        String2.log("\ngenerate marCat columns");
        PrimitiveArray marCatPa     = marCatTable.findColumn("market_category");
        PrimitiveArray dataMarCatPa = dataTable.findColumn("market_category");

        StringArray saa[] = new StringArray[5]; //0 isn't used
        for (int c = 1; c < 5; c++) {
            saa[c] = new StringArray();
            dataTable.addColumn(dataTable.nColumns(), marCatColNames[c], saa[c], new Attributes());
        }

        int marCatPaSize = marCatPa.size();
        int nRows = dataMarCatPa.size();
        for (int row = 0; row < nRows; row++) {
            int dataMarCat = dataMarCatPa.getInt(row);
            int po = marCatPa.binarySearch(0, marCatPaSize - 1, PAOne.fromInt(dataMarCat));
            if (po < 0) {
//marCat=7 isn't defined
                String2.log("dataMarCat=" + dataMarCat + " not in marCatPa");
                for (int c = 1; c < 5; c++)
                    saa[c].add("");
            } else {
                for (int c = 1; c < 5; c++)
                    saa[c].add(marCatTable.getStringData(c, po));
            }
        }

        //save in files
        PrimitiveArray regionPa = dataTable.findColumn("region");
        //already have yearPa
        int lastRegion = regionPa.getInt(0);
        int lastYear   = yearPa.getInt(0); 
        int startRow = 0;
        nRows = regionPa.size();
        for (int row = 0; row < nRows; row++) {
            int tRegion = regionPa.getInt(row);
            int tYear = yearPa.getInt(row);
            if (row == nRows - 1 || tRegion != lastRegion || tYear != lastYear) {
                int lastRow = row == nRows - 1? row : row - 1;
                String fullName   = destDir + tRegion + "/" + tYear + ".nc";
                File2.makeDirectory(destDir + tRegion + "/");

                Table table = new Table();
                for (int col = 0; col < dataTable.nColumns(); col++) {
                    PrimitiveArray oldPa = dataTable.getColumn(col);
                    PrimitiveArray newPa = PrimitiveArray.factory(oldPa.elementType(), 
                        lastRow - startRow + 1, false);
                    for (int tRow = startRow; tRow <= lastRow; tRow++) 
                        newPa.addString(oldPa.getString(tRow));
                    table.addColumn(col, dataTable.getColumnName(col), newPa, 
                        (Attributes)(dataTable.columnAttributes(col).clone()));
                }
                table.saveAsFlatNc(fullName, "row", false);

                if (startRow < 100 || row == nRows - 1) 
                    String2.log(table.toString());
                //if (startRow > 100) Test.ensureTrue(false, "Evaluate the tables.");

                lastRegion = tRegion;
                lastYear = tYear;
                startRow = lastRow + 1;
            }
        }
        String2.log("Finished!");

    }

    /** 
     * This was used by Bob to convert the slightly modified .csv files 
     * (from the source .xls files from
     * http://data.prbo.org/cadc2/index.php?page=colony-data)
     * into .nc files suitable for ERDDAP EDDTableFromNcFiles.
     */
    public static void convertPrbo201001() throws Exception {
        String2.log("\n*** convertPrbo201001");
        String sourceDir = "c:/data/rawSource/prbo2010-01-07/";
        String sourceNames[] = new String[]{  //.csv
           "prboSefiDiet", "prboSefiPhen", "prboSefiPop", "prboSefiProd"};
        String destDir = "C:/u00/data/points/";

        for (int f = 0; f < 4; f++) {
            Table table = new Table();
            table.readASCII(sourceDir + sourceNames[f] + ".csv");
            
            File2.makeDirectory(destDir + sourceNames[f]);
            table.saveAsFlatNc(destDir + sourceNames[f] + "/" + sourceNames[f] + ".nc", "row");
        }
        String2.log("finished.\n");
    }

    /**
     * THIS IS INACTIVE. SEE calcofiSub() INSTEAD.
     * This makes subdirectories erdCalcofiSur and distributes 
     * files to the directories.
     * Note that there are the files from the calcofiSurface.tar file in sourceDir
     * (from Lynn who made the .tar and put it in 
     * ftp://192.168.31.10/outgoing/ldewitt/Simons/calcofi/ )
     * and also the 4 renamed files (e.g., surface_19501113_125_4500000.nc)
     * which I copied into the destDir by hand.
     *
     */
    public static void distributeCalcofiSur() throws Exception {
        if (true) System.exit(0);

        String2.log("\n*** distributeCalcofiSur");
        String sourceDir = "c:/data/rawSource/calcofiSurface2010-01-05/";
        String destDir = "c:/u00/data/points/erdCalcofiSurface/";
        File2.makeDirectory(destDir);
        
        //list all of the files
        String files[] = RegexFilenameFilter.list(sourceDir, ".*\\.nc");
        int nFiles = files.length;
        String2.log("nFiles=" + nFiles); //25996
        int totalCopied = 0;
        for (int year = 1949; year <= 1998; year++) {
            String2.log("\nyear=" + year);
            File2.makeDirectory(destDir + year);
            int yearCopied = 0;
            for (int f = 0; f < nFiles; f++) {
                String fName = files[f];
                if (fName.startsWith("surface_" + year)) {
                    if (File2.copy(
                        sourceDir + fName,
                        destDir + year + "/" + fName)) {
                        yearCopied++;
                        totalCopied++;
                    } else {
                        String2.log("fail: " + fName);
                    }
                }
            }
            String2.log("nFiles copied for " + year + ": " + yearCopied);
        }
        String2.log("Finished.  nFiles=" + nFiles + " totalCopied=" + totalCopied);
    }

    /**
     * THIS IS INACTIVE. SEE calcofiSub() INSTEAD.
     * This makes subdirectories erdCalcofiSub and distributes 
     * files to the directories.
     * Note that there are the files from the calcofiSubsurface.tar file in sourceDir
     * (from Lynn who made the .tar and put it in 
     * ftp://192.168.31.10/outgoing/ldewitt/Simons/calcofi/ )
     * and also the 4 renamed files (e.g., subsurface_19501113_125_4500000.nc)
     * which I copied into the destDir by hand.
     *
     */
    public static void distributeCalcofiSub() throws Exception {
        if (true) System.exit(0);

        String2.log("\n*** distributeCalcofiSub");
        String sourceDir = "c:/data/rawSource/calcofiSurface2010-01-05/";
        String destDir = "c:/u00/data/points/erdCalcofiSubsurface/";
        File2.makeDirectory(destDir);
        File2.deleteAllFiles(destDir);
        
        //list all of the files
        String files[] = RegexFilenameFilter.list(sourceDir, ".*\\.nc");
        int nFiles = files.length;
        String2.log("nFiles=" + nFiles);  //25996
        int totalCopied = 0;
        for (int year = 1949; year <= 1998; year++) {
            String2.log("\nyear=" + year);
            File2.makeDirectory(destDir + year);
            int yearCopied = 0;
            for (int f = 0; f < nFiles; f++) {
                String fName = files[f];
                if (fName.startsWith("subsurface_" + year)) {
                    if (File2.copy(
                        sourceDir + fName,
                        destDir + year + "/" + fName)) {
                        yearCopied++;
                        totalCopied++;
                    } else {
                        String2.log("fail: " + fName);
                    }
                }
            }
            String2.log("nFiles copied for " + year + ": " + yearCopied);
        }
        String2.log("Finished.  nFiles=" + nFiles + " totalCopied=" + totalCopied);
    }


    public static String md5(String password) {
        return DigestUtils.md5Hex(password).toLowerCase();     
    }

    /**
     * This returns the crc32 (a 33 bit value) of a string.
     * This is less likely to have collisions that s.hashCode(),
     * but more likely than MD5 (128 bits).
     *
     * @return a positive value, 10 decimal digits or fewer
     */
    public static long crc32(String s) {
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(String2.stringToUtf8Bytes(s));
        return crc32.getValue();
    }

    private static String hash(int method, String s) {
        if (method == 0) return Math2.reduceHashCode(s.hashCode());
        if (method == 1) return md5(s);
        if (method == 2) return "" + crc32(s);
        if (method == 3) return String2.md5Hex12(s); //md5(s).substring(20);
        return null;
    }


    /**
     * This tests different hash functions.
     * <p>hashCode does poorly.     
     * There are lots of collisions of pairs of chars. Pattern is easily seen from these pairs: 
     * 10 and 0O, 11 and 0P, 12 and 0Q and
     * KB and Ja, KC and Jb.
     * but no pair of numbers collides with another pair.
     *
     * <p>
     * <br>  #failures   /100000  /1000000 (short time) /10000000
     * <br> hashCode          ~1      ~120       6219       11512   ;fails 6116 pairs of digits!
     * <br> md5                0         0      10109
     * <br> crc32             ~1      ~120       7079
     * <br> md512              0         0      11391
     * <br>md512 still had 0 as of 7e6

     */
    public static void testHashFunctions() throws Exception {
        //ensure that old and new md5 methods are equivalent
        String quote = "Now is the time for all good men to come to the aid of their country.";
        Test.ensureEqual(md5(quote), String2.md5Hex(quote), "md5's aren't the same.");

        for (int method = 3; method <= 3; method++) {
            String2.log("\n*** Projects.testHashFunctions " +
                (method == 0? "hashCode" :
                 method == 1? "md5" :     
                 method == 2? "crc32"    :
                "md5Hex12") +                
                "   sample=" + 
                hash(method, quote));

            HashMap hm = new HashMap();
            //single chars
            for (int i = 32; i < 127; i++) {
                String s = "" + (char)i;
                hm.put(hash(method, s), s);
            }

            //pairs of digits
            for (int i = 0; i < 100; i++) {
                String s1 = String2.zeroPad("" + i, 2);
                String s2 = (String)hm.put(hash(method, s1), s1);
                if (s2 != null)
                    throw new Exception("Pairs of digits collide:\n" +
                       "hash(" + s1 + ")=" + hash(method, s1) + "\n" +
                       "hash(" + s2 + ")=" + hash(method, s2));
            }

            //pairs of characters
            int count = 0;
            for (int i1 = 32; i1 < 127; i1++) {
                for (int i2 = 32; i2 < 127; i2++) {
                    String s1 = "" + (char)i1 + (char)i2;
                    String s2 = (String)hm.put(hash(method, s1), s1);
                    if (s2 != null && !s1.equals(s2)) {
                        //String2.log("SAME: " +
                        //   "hash(" + s1 + ")=" + hash(method, s1) + "  " +
                        //   "hash(" + s2 + ")=" + hash(method, s2));
                        count++;
                    }
                }
            }
            String2.log("n=" + count + " pairs of chars collided");

            //test for short string collisions
            String2.log("testing hash(randomShortStrings)");
            int nSame = 0;
            int nextTest = 10;
            int nTest = 1000000;  //usually 1000000, sometimes 10000000
            long time = System.currentTimeMillis();
            for (int i1 = 0; i1 <= nTest; i1++) {
                if (i1 == nextTest) {
                    String2.log("nSame=" + nSame + " out of " + i1);
                    nextTest = nextTest >= 1000000? nextTest + 1000000 : nextTest * 10;
                }
                StringBuilder sb = new StringBuilder();
                for (int i2 = 0; i2 < 10; i2++) 
                    sb.append((char)(33 + Math2.random(95)));
                String s1 = sb.toString();
                //Test.ensureEqual(md5(s1), String2.md5Hex(s1), "md5's aren't the same.");
                String hs1 = hash(method, s1);
                String s2 = (String)hm.put(hs1, s1);
                if (s2 != null && !s1.equals(s2)) {
                    //String2.log(
                    //   "1 hash(" + s1 + ")=" + hash(method, s1) + "\n" +
                    //   "2 hash(" + s2 + ")=" + hash(method, s2));
                    nSame++;
                }
            } 
            String2.log("time=" + (System.currentTimeMillis() - time));

            //test for long string collisions
            hm = new HashMap();
            String2.log("testing hash(randomLongStrings)");
            nSame = 0;
            nextTest = 10;
            nTest = 1000000;
            time = System.currentTimeMillis();
            for (int i1 = 0; i1 <= nTest; i1++) {
                if (i1 == nextTest) {
                    String2.log("nSame=" + nSame + " out of " + i1);
                    nextTest *= 10;
                }
                StringBuilder sb = new StringBuilder();
                for (int i2 = 0; i2 < 65; i2++) 
                    sb.append((char)(33 + Math2.random(95)));
                String s1 = sb.toString();
                String hs1 = hash(method, s1);
                String s2 = (String)hm.put(hs1, s1);
                if (s2 != null && !s1.equals(s2)) {
                    //String2.log(
                    //   "1 hash(" + s1 + ")=" + hash(method, s1) + "\n" +
                    //   "2 hash(" + s2 + ")=" + hash(method, s2));
                    nSame++;
                }
            } 
            String2.log("time=" + (System.currentTimeMillis() - time));
}
    }


    /** This touches (pings) several URLs */
    public static void touchUrls() throws Exception {
String2.log("\n***Projects.touchUrls");
//for example, copy all of the flag urls for erddap to lines here like  
//(or use Alt 8 from first url line?)
//SSR.touchUrl("url", 10000);

String2.log("Projects.touchUrls is finished.");
    }


    /**
     * Get CA market catch (Short List) data into 2 nc file (Monthly and Yearly).
     */
    public static void getCAMarCatShort() throws Exception {
        String2.log("\n*** Projects.getCAMarCatShort");

        String url = "http://las.pfeg.noaa.gov/thredds/dodsC/CA_market_catch/ca_fish_grouped_short.nc";

        DConnect dConnect = new DConnect(url, true, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        String name;

        //Global
        Table table = new Table();
        OpendapHelper.getAttributes(das, "GLOBAL", table.globalAttributes());

        name = "time_series";
        Attributes time_seriesAtts = new Attributes();
        OpendapHelper.getAttributes(das, name, time_seriesAtts);
        DoubleArray time_series = (DoubleArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + time_series);
        int nTime_series = time_series.size();

        //stations are just 1..6
        name = "stations";
        IntArray stations = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + stations);
        int nStations = stations.size();
        for (int s = 0; s < nStations; s++) 
            Test.ensureEqual(stations.get(s), s + 1, "s+1=" + (s+1));

        //fish are just 1..57
        name = "fish";
        IntArray fish = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + fish);
        int nFish = fish.size();
        Test.ensureEqual(nFish, 57, "nFish");
        for (int f = 0; f < nFish; f++) 
            Test.ensureEqual(fish.get(f), f + 1, "f+1=" + (f+1));

        //bob changed all caps to titleCase
        //bob changed groped -> grouped, littlencks -> littlenecks, speckeled -> speckled, 
        String fishnames[] = new String[] {
//0:  18, 22, 16, 11, 16, 8, 15, 9, 11, 16, 
"Anchovy, Northern",
"Barracuda, California",
"Bass, Giant Sea",
"Bass, Rock",
"Bonito, Pacific",
"Cabezon",
"Croaker, White",
"Flounder",
"Flyingfish",
"Greenling, Kelp",
//10:  11, 20, 17, 8, 15, 25, 22, 6, 9, 10,
"Grenadiers",
"Halibut, California",
"Herring, Pacific",
"Lingcod",
"Mackerel, Jack",
"Mackerel, Pacific (Chub)",
"Mackerel, Unspecified",
"Perch",
"Rockfish",
"Sablefish",
//20:  7, 8, 17, 25, 6, 22, 6, 6, 5, 10,
"Salmon",
"Sanddab",
"Sardine, Pacific",
"Scorpionfish, California",
"Shark",
"Sheephead, California",
"Skate",
"Smelt",
"Sole",
"Swordfish",
//30:  11, 14, 14, 15, 16, 7, 17, 17, 11, 22,
"Thornyhead",
"Tuna, Albacore",
"Tuna, Bluefin",
"Tuna, Skipjack",
"Tuna, Yellowfin",
"Turbot",
"Whitefish, Ocean",
"Whiting, Pacific",
"Yellowtail",
"Blackfish, Sacramento",
//40:  5, 8, 9, 23, 11, 16, 11, 26, 6, 12,
"Carp",
"Catfish",
"Hardhead",
"Pikeminnow, Sacramento",
"Split-tail",
"Crab, Dungeness",
"Crab, Rock",
"Lobster, California Spiny",
"Prawn",
"Shrimp, Bay",
//50:  22, 14, 7, 8, 6, 8, 14
"Shrimp, Pacific Ocean",
"Cucumber, Sea",
"Urchin",
"Abalone",
"Clams",
"Octopus",
"Squid, Market"};
        Test.ensureEqual(fishnames.length, nFish, "fishnames.length");

        //fishOrder only used for documentation
        ArrayList list = new ArrayList();
        list.add(new StringArray(fishnames));
        int fishOrder[] = PrimitiveArray.rank(list, new int[]{0}, new boolean[]{true});
        for (int f = 0; f < nFish; f++)
            String2.log(f + " " + fishnames[fishOrder[f]]);


        String fishdescr[] = new String[] {
//"All fish landed as anchovy EXCEPT deepbody anchovy and slough anchovyAll fish landed as barracudaAll fish landed as black seabass (1928-1960) or giant seabass (1961)Group name includes kelp bass, barred sand bass, and spotted sand bassAll fish landed as bonito or bonito tunaAll fish landed as cabezonAll fish landed as king fish or croaker or as queenfish, which was included with white croaker through 1971All fish landed as flounder or as starry flounder, but NOT arrowtooth flounder which was included with sole before 1951All fish landed as flyingfishAll fish landed as kelp greenling or as greenlingAll fish landed as grenadiers after 1971, they were part of the miscellaneous trawl category through 1971Fish landed as California halibut, possible mixing of Pacific with California halibut, see Fish Bulletin 74, p75. 1947 All fish landed as Pacific herring or as herring roe (meaning herring containing roe), not round herringAll fish landed as lingcod, called Pacific cultus before 1947.All fish landed as jack mackerel, plus proportion based on sampling of unspecified mackerel 1977-85. All fish landed as Pacific mackerel, plus proportion based on sampling of unspecified mackerel 1977-85All fish landed as mackerel from 1978-80, plus later mackerel landings if not identified by species on fish receipt or by samplingMixed species group containing just surfperches in northern Calif, but in southern Calif. also including blacksmith, halfmoon, opaleye and zebraperch - separable after 1976Mixed species group containing all species of rockfish (plus thornyheads until 1977)All fish landed as sablefish or blackcodAll fish landed as salmon including, king or chinook, silver or coho, pink, and chum - separable after 1976All fish landed as sanddabs, including Pacific, longfin and speckeledAll fish landed in California as sardine, some were mixed with mackerel schools and calculated from sampling 1981All fish landed as scorpionfish or as sculpin used as common name for scorpionfish until 1978All fish landed under various shark names separable from 1977 onAll fish landed as California sheepheadAll fish landed as skate, California skate, big skateAll fish landed as smelt, whitebait, or silversides, including topsmelt and jacksmelt.All landed as combined sole until 1954, primarily English, petrale, and rex before 1948, then Dover as well. Includes other soles and some turbot and small Calif.halibut as well. All fish landed as swordfish or broadbill swordfishIncluded as part of the rockfish catch until 1978, all fish landed as longspine, shortspine or combined thornyhead.All albacore from California watersAll bluefin tuna from California watersAll skipjack and black skipjack tuna from California waters.All yellowfin tuna from California watersAll fish landed as turbot, curlfin, diamond, C-O, and sharpridge or hornyhead.  Some included in soles before 1931.All fish landed as ocean whitefish, or whitefish.All fish landed as Pacific hake or Pacific whiting.All fish landed as yellowtail (a member of the jack family).Freshwater fish Included with hardhead until 1969, also called greaser Orthodon microlepidotus (Fish Bulletin 74, p47.).  All carp landed until 1971, some large quantities remove from Clear Lake 1931-36, Cyprinus carpio.All freshwater catfish landed, includes Forktail or channel catfish (Ictalurus catus) and and square-tail,Sacramento catfish or bullhead (Ameiurus nebulosus)All freshwater fish landed as hard head Mylopharodon conocephalus, includes blackfish in most years.All freshwater fish landed as pike Ptychocheilus grandis (not true pike) (1928-1951)All freshwater fish landed as split-tail, Pogonichtys macrolepidotus 1928-67)All crabs landed as market crab or Dungeness crab, includes some rock crabs from Santa Barbara south.All crabs landed in any of the rock crab categories (not differentiated until 1995).  All lobster called either California spiny or Pacific spiny lobster.All types of prawns landedAlmost all shrimp before 1952 were bay shrimp from SF Bay, separate category after 1951Fishery on Pacific Ocean shrimp began in 1952.California and warty sea cucumbers. Sea cucumber fishery  began 1978. All types of sea urchins but primarioy red sea urchin.All types of abalone. Just red abalone until 1943, but all were groped together until 1972. See California Living Marine Resources, p91.All types of clams, included pismo clams until 1947, also cockles or littlencks (Chione sps.)., gaper, razor, washington, jackknife, and softshell.All types of octopus.All market squid (Loligo opalescens)"
//I add periods at end if none.
//69, 28, 68, 70, 40, 26, 107, 119, 29, 49, 
"All fish landed as anchovy EXCEPT deepbody anchovy and slough anchovy.",
"All fish landed as barracuda.",
"All fish landed as black seabass (1928-1960) or giant seabass (1961).",
"Group name includes kelp bass, barred sand bass, and spotted sand bass.",
"All fish landed as bonito or bonito tuna.",
"All fish landed as cabezon.",
"All fish landed as king fish or croaker or as queenfish, which was included with white croaker through 1971.",
"All fish landed as flounder or as starry flounder, but NOT arrowtooth flounder which was included with sole before 1951.",
"All fish landed as flyingfish.",
"All fish landed as kelp greenling or as greenling.",
//105, 119, 104, 62, 101, 102, 130, 172, 84, 40, 
"All fish landed as grenadiers after 1971, they were part of the miscellaneous trawl category through 1971.",
"Fish landed as California halibut, possible mixing of Pacific with California halibut, see Fish Bulletin 74, p75. 1947.",
"All fish landed as Pacific herring or as herring roe (meaning herring containing roe), not round herring.",
"All fish landed as lingcod, called Pacific cultus before 1947.",
"All fish landed as jack mackerel, plus proportion based on sampling of unspecified mackerel 1977-85.",
"All fish landed as Pacific mackerel, plus proportion based on sampling of unspecified mackerel 1977-85.",
"All fish landed as mackerel from 1978-80, plus later mackerel landings if not identified by species on fish receipt or by sampling.",
"Mixed species group containing just surfperches in northern Calif, but in southern Calif. also including blacksmith, halfmoon, opaleye and zebraperch - separable after 1976.",
"Mixed species group containing all species of rockfish (plus thornyheads until 1977).",
"All fish landed as sablefish or blackcod.",
//107, 69, 113, 93, 64, 39, 53, 86, 179, 51,
"All fish landed as salmon including, king or chinook, silver or coho, pink, and chum - separable after 1976.",
"All fish landed as sanddabs, including Pacific, longfin and speckled.",
"All fish landed in California as sardine, some were mixed with mackerel schools and calculated from sampling 1981.",
"All fish landed as scorpionfish or as sculpin used as common name for scorpionfish until 1978.",
"All fish landed under various shark names separable from 1977 on.",
"All fish landed as California sheephead.",
"All fish landed as skate, California skate, big skate.",
"All fish landed as smelt, whitebait, or silversides, including topsmelt and jacksmelt.",
"All landed as combined sole until 1954, primarily English, petrale, and rex before 1948, then Dover as well. Includes other soles and some turbot and small Calif. halibut as well.",
"All fish landed as swordfish or broadbill swordfish.",
//115, 35, 39, 60, 41, 115, 49, 51, 60, 122,
"Included as part of the rockfish catch until 1978, all fish landed as longspine, shortspine or combined thornyhead.",
"All albacore from California waters.",
"All bluefin tuna from California waters.",
"All skipjack and black skipjack tuna from California waters.",
"All yellowfin tuna from California waters.",
"All fish landed as turbot, curlfin, diamond, C-O, and sharpridge or hornyhead.  Some included in soles before 1931.",
"All fish landed as ocean whitefish, or whitefish.",
"All fish landed as Pacific hake or Pacific whiting.",
"All fish landed as yellowtail (a member of the jack family).",
"Freshwater fish Included with hardhead until 1969, also called greaser Orthodon microlepidotus (Fish Bulletin 74, p47).",
//98, 157, 100, 84, 77, 101, 86, 68, 26, 87,
"All carp landed until 1971, some large quantities remove from Clear Lake 1931-36, Cyprinus carpio.",
"All freshwater catfish landed, includes Forktail or channel catfish (Ictalurus catus) and square-tail, Sacramento catfish or bullhead (Ameiurus nebulosus).",
"All freshwater fish landed as hard head Mylopharodon conocephalus, includes blackfish in most years.",
"All freshwater fish landed as pike Ptychocheilus grandis (not true pike) (1928-1951).",
"All freshwater fish landed as split-tail, Pogonichtys macrolepidotus 1928-67).",
"All crabs landed as market crab or Dungeness crab, includes some rock crabs from Santa Barbara south.",
"All crabs landed in any of the rock crab categories (not differentiated until 1995).",
"All lobster called either California spiny or Pacific spiny lobster.",
"All types of prawns landed.",
"Almost all shrimp before 1952 were bay shrimp from SF Bay, separate category after 1951.",
//46, 70, 54, 136, 147, 21, 36
"Fishery on Pacific Ocean shrimp began in 1952.",
"California and warty sea cucumbers. Sea cucumber fishery  began 1978.",
"All types of sea urchins but primarily red sea urchin.",
"All types of abalone. Just red abalone until 1943, but all were grouped together until 1972. See California Living Marine Resources, p91.",
"All types of clams, included pismo clams until 1947, also cockles or littlenecks (Chione spp.), gaper, razor, washington, jackknife, and softshell.",
"All types of octopus.",
"All market squid (Loligo opalescens)."};
        Test.ensureEqual(fishdescr.length, nFish, "fishdescr.size");

        StringBuilder descriptions = new StringBuilder();
        for (int f = 0; f < nFish; f++) 
            descriptions.append("* " + fishnames[fishOrder[f]] + " - " + fishdescr[fishOrder[f]] + "\n");
        Attributes fishAtts = (new Attributes())
            .add("long_name", "Fish Name")
            .add("description", descriptions.toString());

        //"San Diego    Los Angeles  Santa BarbaraMonterey     San FranciscoEureka       "
        StringArray stationnames = (StringArray)PrimitiveArray.csvFactory(PAType.STRING,
            "San Diego, Los Angeles, Santa Barbara, Monterey, San Francisco, Eureka");
        Test.ensureEqual(nStations, stationnames.size(), "nStations != nStationnames");

        name = "landings";
        Attributes landingsAtts = new Attributes();
        OpendapHelper.getAttributes(das, name, landingsAtts);
        landingsAtts.set("units", "pounds");
        IntArray landings = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, 
            "?landings.landings");
        Test.ensureEqual(landings.size(), nTime_series * nStations * nFish, "landings.size");

        //make the table  0=time 1=year 2=station 3=fish 4=landings
        DoubleArray timePA = new DoubleArray();
        ShortArray yearPA = new ShortArray();
        StringArray stationPA = new StringArray();
        StringArray fishPA = new StringArray();
        IntArray landingsPA = new IntArray();
        table.addColumn(0, "time", timePA, time_seriesAtts.add("units", Calendar2.SECONDS_SINCE_1970));
        table.addColumn(1, "year", yearPA, new Attributes());
        table.addColumn(2, "fish", fishPA, fishAtts);
        table.addColumn(3, "port", stationPA, (new Attributes()).add("long_name", "Port"));
        table.addColumn(4, "landings", landingsPA, landingsAtts);

        //add the data
        double tbf[] = Calendar2.getTimeBaseAndFactor("hours since 1900-01-01T00:00:00Z");
        
        for (int t = 0; t < nTime_series; t++) {
            double tt = time_series.get(t);
            double epSec = Calendar2.unitsSinceToEpochSeconds(tbf[0], tbf[1], tt);
            int tYear = String2.parseInt(Calendar2.epochSecondsToIsoDateString(epSec).substring(0, 4));

            int landingsAll[] = new int[nFish];  //0's
            for (int s = 0; s < nStations; s++) {
                int base = t * nStations * nFish +  s * nFish;

                for (int f = 0; f < nFish; f++) {
                    timePA.add(epSec);
                    yearPA.addInt(tYear);
                    fishPA.add(fishnames[f]);
                    stationPA.add(stationnames.get(s));
                    int tLandings = landings.get(base + f);
                    landingsPA.add(tLandings);
                    if (tLandings > 0) landingsAll[f] += tLandings; // >0 handles mv
                }

            }

            //add the data for the All Station
            for (int f = 0; f < nFish; f++) {
                timePA.add(epSec);
                yearPA.addInt(tYear);
                fishPA.add(fishnames[f]);
                stationPA.add("All");
                landingsPA.add(landingsAll[f]);
            }
        }
        stationnames.add("All");
        nStations++;

        //sort by fish,port,time
        table.sort(new int[]{2,3,0}, new boolean[]{true, true, true});

        //save in baseDir/station/fish.nc
        String monthlyDir = "C:/u00/data/points/erdCAMarCatSM/";
        String yearlyDir  = "C:/u00/data/points/erdCAMarCatSY/";
        File2.deleteAllFiles(monthlyDir, true, true); //recursive, deleteEmptySubdirectories
        File2.deleteAllFiles(yearlyDir,  true, true); //recursive, deleteEmptySubdirectories
        int nRows = table.nRows();
        for (int s = 0; s < nStations; s++) {
            //make subdirectory
            String tStation = stationnames.get(s);
            String tMonthlyDir = monthlyDir + String2.encodeFileNameSafe(tStation) + "/";
            String tYearlyDir  = yearlyDir  + String2.encodeFileNameSafe(tStation) + "/";
            File2.makeDirectory(tMonthlyDir);
            File2.makeDirectory(tYearlyDir);
            for (int f = 0; f < nFish; f++) {
                String tFish = fishnames[f];
                String2.log(tStation + " " + tFish);
                BitSet bitset = new BitSet(nRows);
                for (int row = 0; row < nRows; row++) 
                    bitset.set(row, fishPA.get(row).equals(tFish) && stationPA.get(row).equals(tStation));

                //* make the monthly table  0=time 1=year 2=station 3=fish 4=landings
                DoubleArray tTimePA    = (DoubleArray)timePA.clone();    tTimePA.justKeep(bitset);
                ShortArray tYearPA     = (ShortArray)yearPA.clone();     tYearPA.justKeep(bitset);
                StringArray tStationPA = (StringArray)stationPA.clone(); tStationPA.justKeep(bitset);
                StringArray tFishPA    = (StringArray)fishPA.clone();    tFishPA.justKeep(bitset);
                IntArray tLandingsPA   = (IntArray)landingsPA.clone();   tLandingsPA.justKeep(bitset);
                Table tTable = new Table();
                tTable.addColumn(0, "time", tTimePA,        (Attributes)table.columnAttributes(0).clone());
                tTable.addColumn(1, "year", tYearPA,        (Attributes)table.columnAttributes(1).clone());
                tTable.addColumn(2, "fish", tFishPA,        (Attributes)table.columnAttributes(2).clone());
                tTable.addColumn(3, "port", tStationPA,     (Attributes)table.columnAttributes(3).clone());
                tTable.addColumn(4, "landings", tLandingsPA,(Attributes)table.columnAttributes(4).clone());

                //save the monthly file
                tTable.saveAsFlatNc(tMonthlyDir + String2.encodeFileNameSafe(tFish) + ".nc", "row", false);     

                //* condense to yearly data
                int readRow = 0, writeRow = 0, tnRows = tTable.nRows();
                while (readRow < tnRows) {
                    short tYear = tYearPA.get(readRow);
                    int sum = tLandingsPA.get(readRow);
                    int nextRow = readRow + 1;
                    while (nextRow < nRows && tYear == yearPA.get(nextRow)) {
                        sum += tLandingsPA.get(nextRow);
                        nextRow++;
                    }
                    //store the data on writeRow;  change time to be tYear-07-01
                    tTimePA.set(writeRow, Calendar2.isoStringToEpochSeconds(tYear + "-07-01"));
                    tYearPA.set(writeRow, tYear);
                    tStationPA.set(writeRow, tStation);
                    tFishPA.set(writeRow, tFish);
                    tLandingsPA.set(writeRow, sum);
                    writeRow++;

                    readRow = nextRow;
                }            
                tTable.removeRows(writeRow, tnRows);

                //save the yearly file
                tTable.saveAsFlatNc(tYearlyDir + String2.encodeFileNameSafe(tFish) + ".nc", "row", false);     
            }
        }

        String2.log("\n*** Projects.getCAMarCatShort successfully created");
        
    }


    /**
     * Get CA market catch (Long List) data into 2 nc file (Monthly and Yearly).
     */
    public static void getCAMarCatLong() throws Exception {
        String2.log("\n*** Projects.getCAMarCatLong");

        String url = "http://las.pfeg.noaa.gov/thredds/dodsC/CA_market_catch/ca_fish_grouped.nc";

        DConnect dConnect = new DConnect(url, true, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        String name;

        //Global
        Table table = new Table();
        OpendapHelper.getAttributes(das, "GLOBAL", table.globalAttributes());

        name = "time_series";
        Attributes time_seriesAtts = new Attributes();
        OpendapHelper.getAttributes(das, name, time_seriesAtts);
        DoubleArray time_series = (DoubleArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + time_series);
        int nTime_series = time_series.size();

        //stations are just 1..6
        name = "stations";
        IntArray stations = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + stations);
        int nStations = stations.size();
        for (int s = 0; s < nStations; s++) 
            Test.ensureEqual(stations.get(s), s + 1, "s+1=" + (s+1));

        //fish are just 1..
        name = "fish";
        IntArray fish = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + fish);
        int nFish = fish.size();
        Test.ensureEqual(nFish, 341, "nFish");
        for (int f = 0; f < nFish; f++) 
            Test.ensureEqual(fish.get(f), f + 1, "f+1=" + (f+1));

        //fish name nchar
        name = "nchar";
        IntArray nchar = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + nchar);
        Test.ensureEqual(nchar.size(), nFish, "nchar.length");

        //fishnames
        String fishNamesString = 
"ANCHOVY, NORTHERNANCHOVY, DEEPBODYBARRACUDA, CALIFORNIABASS, BARRED SAND BASS, GIANT SEA BASS, KELP BASS, ROCK BASS, SPOTTED SAND BASS, STRIPED BLACKSMITHBONEFISH BONITO, PACIFIC BUTTERFISH, PACIFIC CABEZON CABRILLA, SPOTTEDCOD, PACIFIC CORBINA, CALIFORNIA CORVINA, SHORTFIN CROAKER, BLACK CROAKER, VARIOUS SPECIES CROAKER, WHITE CROAKER, YELLOWFIN CUSK-EEL, SPOTTED DOLPHINFISH EEL EEL, BLENNY EEL, CALIFORNIA MORAY ESCOLAR EULACHON FLOUNDER, ARROWTOOTH FLOUNDER, STARRY FLOUNDER, UNCLASSIFIED FLYINGFISH GARIBALDI GOBY, BLUEBANDED GOBY, YELLOWFIN GREENLING, KELP GRENADIERS GROUPER GROUPER, BROOMTAIL GRUNION, CALIFORNIA GUITARFISH, SHOVELNOSE HAGFISH HALFMOON HALIBUT, CALIFORNIA HALIBUT, PACIFICHALIBUT, UNSPECIFIEDHERRING ROE ON KELP HERRING, PACIFIC HERRING, WHOLE FISH WITH ROE HERRING, ROUND JACK CREVALLE JACKS KAHAWAI KELPFISHES KILLIFISH, CALIFORN LINGCOD LIZARDFISH, CALIFORNIA LOUVAR MACKEREL, BULLET MACKEREL, JACK MACKEREL, PACIFIC MACKEREL, UNSPECIFIED MARLIN, STRIPED MIDSHIPMAN, PLAINFIN MOLA, COMMON MONKEYFACE EEL MUDSUCKER, LONGJAW MULLET, STRIPED NEEDLEFISH, CALIFORNIA OILFISH OPAH OPALEYE PERCH PERCH, PACIFIC OCEAN POMFRET, PACIFICQUEENFISH RATFISH RAY, BAT RAY, PACIFIC ELECTRIC RAY ROCKFISH, AURORA ROCKFISH, BANK ROCKFISH, BLACKGILL ROCKFISH, BLACK-AND-YELLOW ROCKFISH, BLACK ROCKFISH, BLUE ROCKFISH, BOCACCIO ROCKFISH, BRONZESPOTTED ROCKFISH, BROWN ROCKFISH, CALICO ROCKFISH, NOMINAL CANARY ROCKFISH, CHAMELEON ROCKFISH, CHILIPEPPER ROCKFISH, CHINA ROCKFISH, COPPER ROCKFISH, COWCOD ROCKFISH, DARKBLOTCHED ROCKFISH, FLAG ROCKFISH, GROUP BLUE/BLACK ROCKFISH, GROUP BOCACCIO/CHILIRROCKFISH, GROUP BOLINA ROCKFISH, GROUP CANARY/VERMILLROCKROCKFISH, GROUP DEEP RED ROCKFISH, GROUP GOPHER ROCKFISH, GROUP RED ROCKFISH, GROUP ROSEFISH ROCKFISH, GROUP SMALL RED ROCKFISH, GOPHER ROCKFISH, GRASS ROCKFISH, GREENBLOTCHED ROCKFISH, GREENSTRIPED ROCKFISH, GREENSPOTTED ROCKFISH, HONEYCOMB ROCKFISH, KELP ROCKFISH, MEXICAN ROCKFISH, OLIVE ROCKFISH, PINK ROCKFISH, PINKROSE ROCKFISH, QUILLBACK ROCKFISH, REDBANDED ROCKFISH, ROSETHORN ROCKFISH, ROSY ROCKFISH, SHORTBELLY ROCKFISH, SPLITNOSE ROCKFISH, SPECKLED ROCKFISH, SQUARESPOT ROCKFISH, STRIPETAIL ROCKFISH, STARRY ROCKFISH, SWORDSPINE ROCKFISH, TREEFISH ROCKFISH,UNSPECIFIED ROCKFISH,UNSPECIFIED NEARSHORERROCKFISH,UNSPECIFIED SHELF ROCKFISH,UNSPECIFIED SLOPE ROCKFISH, VERMILLION ROCKFISH, WIDOW ROCKFISH, YELLOWEYE ROCKFISH, YELLOWTAIL SABLEFISH SAILFISH SALEMA SALMON SALMON, CHUM SALMON, KING SALMON ROE, KING SALMON, PINK SALMON, SILVER SANDDAB SANDDAB, LONGFIN SANDDAB, PACIFIC SANDDAB, SPECKLED SARDINE, PACIFIC SARGO SAURY, PACIFIC SCORPIONFISH, CALIFORNIA  SCULPIN, PACIFIC STAGHORN SCULPIN, YELLOWCHIN SEABASS, WHITE SENORITA SHAD, AMERICAN SHAD, THREADFIN SHARK SHARK, PACIFIC ANGEL SHARK, BLUE SHARK, BONITO (MAKO) SHARK, BROWN SMOOTH SHARK, BASKING SHARK, BLACK TIP SHARK, COMMON THRESHER SHARK, COW SHARK, SPINY DOGFISH SHARK, DUSKY SHARK, SMOOTH HAMMERHEAD SHARK, HORN SHARK, LEOPARD SHARK, SALMON SHARK, SEVENGILL SHARK, SIXGILL SHARK, GRAY SMOOTHHOUND SHARK, SOUPFIN SHARK, SWELL SHARK, BIGEYE THRESHER SHARK, PELAGIC THRESHER SHARK, WHITE SHEEPHEAD, CALIFORNIA SIERRA SKATE SKATE, BIG SKATE, CALIFORNIA SKATE, THORNY SMELT SMELT, ATHERINID SMELT, JACKSMELT SMELT, NIGHT SMELT, SURF SMELT, TOPSMELT SMELT, TRUE SMELT, WHITEBAIT SNAPPER -MEXICO- SOLE, BIGMOUTH SOLE, BUTTER SOLE, C-O SOLE, CURLFIN SOLE, DOVER SOLE, ENGLISH SOLE, FANTAIL SOLE, PETRALE SOLE, ROCK SOLE, REX SOLE, SAND SOLE, SLENDER SOLE, UNCLASSIFIED SURFPERCH SURFPERCH, BARRED SURFPERCH, BLACK SURFPERCH, CALICO SURFPERCH, DWARF SURFPERCH, PILE SURFPERCH, PINK SURFPERCH, RAINBOW SURFPERCH, REDTAIL SURFPERCH, RUBBERLIP SURFPERCH, SHINER SURFPERCH, WALLEYE SURFPERCH, WHITE STICKLEBACK, THREESPINE STINGRAY STURGEON SWORDFISH THORNYHEAD THORNYHEAD, LONGSPINE THORNYHEAD, SHORTSPINE TOMCOD, PACIFIC TRIGGERFISH TONGUEFISH, CALIFORNIA  TUNA, ALBACORE TUNA, BIGEYE TUNA, BLACKFIN TUNA, BLUEFIN TUNA, SKIPJACK TUNA, SKIPJACK, BLACK TUNA, YELLOWFIN TUNA, UNSPECIFIED TURBOT, DIAMOND TURBOT, HORNYHEAD TURBOT, SPOTTED TURBOT, UNSPECIFIED WAHOO WHITEFISH, OCEAN WHITING, PACIFIC WOLF-EEL WRASSE, ROCK YELLOWTAIL ZEBRAPERCH UNSPECIFIED TRAWLED FISH UNSPECIFIED FISH BLACKFISH, SACRAMENTO CARP CATFISH HARDHEAD HITCH PIKEMINNOW, SACRAMENTO SPLIT-TAIL SUCKER, WESTERN BARNACLE BOX CRAB CRAB, CLAWS CRAB, DUNGENESS CRAB, KING CRAB, PELAGIC RED CRAB, ROCK CRAB, BROWN ROCK CRAB, RED ROCK CRAB, YELLOW ROCK CRAB, SAND CRAB, SHORE CRAB, SPIDER CRAB, TANNER CRAYFISH LOBSTER, CALIFORNIA SPINY PRAWN, GOLDEN PRAWN, RIDGEBACK PRAWN, SPOTTED PRAWN, UNSPECIFIED SHRIMP, BAY SHRIMP, BRINE SHRIMP, COONSTRIPE SHRIMP, GHOST SHRIMP, PACIFIC OCEAN SHRIMP, RED ROCK SHRIMP, UNSPECIFIED CRUSTACEAN, UNSPECIFIED CUCUMBER, SEA ECHINOD, UNSPECIFIED SEA STARS URCHIN, LYTECHINUS URCHIN, PURPLE SEA URCHIN, RED SEA URCHIN, UNSPECIFIED ABALONE, BLACK ABALONE, FLAT ABALONE, GREEN ABALONE, PINK ABALONE, WHITE ABALONE, THREADED ABALONE, PINTO ABALONE, RED ABALONE, UNSPECIFIED CHITON CLAM, GAPER CLAM, CALIFORNIA JACKKNIFE CLAM, NATIVE LITTLENECK CLAM, NOTHERN RAZOR CLAM, PURPLE CLAM, PISMO CLAM, ROSY RAZOR CLAM, SOFT SHELLED CLAM, COMMON WASHINGTON CLAM, UNSPECIFIED LIMPET, UNSPECIFIED MOLLUSK, UNSPECIFIED MUSSEL OCTOPUS, UNSPECIFIED OYSTER, CALIFORNIA OYSTER, EASTERN OYSTER, EUROPEAN FLAT OYSTER, GIANT PACIFIC OYSTER, UNSPECIFIED SCALLOP, UNSPECIFIED SEA HARES SEA SLUG SNAIL, SEA SQUID, JUMBO SQUID, MARKET WHELK FROGS INVERTEBRATES, COLONIAL WORMS, MARINE TURTLES, FRESHWATER TURTLES, MARINE ";
        String fishnames[] = new String[nFish];
        int po = 0;
        for (int f = 0; f < nFish; f++) {
            fishnames[f] = String2.toTitleCase(fishNamesString.substring(po, po + nchar.get(f)).trim());
            if (fishnames[f].equals("Killifish, Californ")) fishnames[f] = "Killifish, California";
            fishnames[f] = String2.replaceAll(fishnames[f], "Rockfish,Un", "Rockfish, Un");
            if (fishnames[f].indexOf("Littlencks") >= 0 ||
                fishnames[f].indexOf("Speckeled") >= 0 ||
                fishnames[f].indexOf("Groped") >= 0) {
                String2.log("trouble: #" + f + "=" + fishnames[f]);
            }
            String2.log(f + " " + fishnames[f]);
            po += nchar.get(f);
        }

        //String2.toTitleCase()
        Test.ensureEqual(fishnames.length, nFish, "fishnames.length");

        //fishOrder only used for documentation
        ArrayList list = new ArrayList();
        list.add(new StringArray(fishnames));
        int fishOrder[] = PrimitiveArray.rank(list, new int[]{0}, new boolean[]{true});
        //for (int f = 0; f < nFish; f++)
        //    String2.log(f + " " + fishnames[fishOrder[f]]);

        //fish description ndescr
        name = "ndescr";
        IntArray ndescr = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, "?" + name);
        String2.log(name + "=" + ndescr);
        Test.ensureEqual(ndescr.size(), nFish, "ndescr.length");

        //fishnames
        String fishDescrString = 
"Norhtern anchovy (Engraulis mordax) all anchovy landed EXCEPT deepbody anchovy, all yearsDeepbody anchovy (Anchoa compressa) mostly in bays and estuaries, 1978+California barracuda (Sphyraena argentea) all yearsBarred sand bass (Paralabrax nebulifer) 1978+, included in BASS, ROCK category before 1978Giant or black sea bass (Sterolepis gigas) all years Kelp bass (Paralabrax clathratus) 1978+, included in BASS, ROCK category before 1978Group name includes kelp bass, barred sand bass, and spotted sand bass, 1928-53 then sport only after 1953, some years 1978+Spotted sand bass (Paralabrax maculatofasciatus), small component of BASS, ROCK category before 1978Striped bass (Roccus saxatilis), Sport only after 1935Blacksmith (Chromis punctipinnis) included in Perch category before 1963Bonefisn (Albula vulpes), 1983 onlyPacific Bonito or bonito tuna (Sarda chiliensis) all yearsPacific butterfish also called California pompano  (Peprilus simillimus) all yearsCabezon (Scorpaenichthys marmoratus) all years ,,,,\"Spotted Cabrilla (Epinephelus analogus) a sea bass, 1930, 1983+1977+ Pacific cod (Gadus macrocephalus) 1977+California corbina (Menticirrhus undulatus), a croaker 1981+(1928-41, 1995+)Shortfin corvina or shortfin seabass (Cynoscion parvipinnis), a croaker caught and sometimes landed mixed with white croakers 1932-41 and 1995+Black croaker (Cheilotrema saturnum) 1979+ Croakers of various unidentified species 1978+, probably inclded in CROAKER, WHITE before 1978White croaker also called kingfish (Genyonemus lineatus) may include other croakers especially queenfish (Seriphus politus)Yellowfin croaker (Umbrina roncador) 1995+Spotted cusk-eel (Chilara taylori) 1994+Dolfpninfish (Coryphaena hippururs), Fish NOT Mammals, occasional 1930-62, yearly 1972+Various unspecified eels including moray, wolfeel, blennies and pricklebacks.Blennies, unidentified, 1931 only, probably pricklebacksCalifornia Moray (Gymnothorax mordax) 1931-32, 1981+ Escolar (Lepidocybium flavobrunneum) 1994+Eulachon (Thaleichthys pacificus) 1987+Arrowtooth flounder (Atheresthes stomias) 1955+, included with sole before 1951Starry flounder (Platichthys stellatus), in unclassified flounder some years or ports, combine with FLOUNDER, UNCLASSIFIED 1930-46 and 1978+Flounders, almost entirely starry flounder (Platichthys stellatus), combine with FLOUNDER, STARRY.  most yearsProbably mostly California Flyingfish (Cypselurus californicus), all yearsGaribaldi (Hypsypops rubicundus), Not a commercial species, small amounts 1979+Bluebanded goby, (Lythrypnus dalli) 1985+Yellowfin goby (Acanthogobius flavimanus), 1990+Kelp greenling (Hexagrammos decagrammus)  most yearsVarious grenadiers, Pacific grenadier (Coryaphaenoides acrolepis) and giant grenadier (Albatrossia pectoralis) and possibly others, 1972+Various species of grouper (Mycteroperca)  1931, 1978+Broomtail grouper (Mycteroperca xenarcha) 1985+California Grunion (Leuresthes tenuis), not a commercial species, some 1978+Shovelnose guitarfish (Rhinobatos productus), 1977+Hagfish (Eptatretus spp.) 1983+Halfmoon (Medialuna californicus), included in Perch category before 1954California halibut (Paralicthys californicus), may include some Pacific halibut landings, all yearsPacific halibut (Hippoglossus stenolepis), some landings may have been recorded as California halibut, most yearsUnspecified halibut, may be either California halibut (Paralicthys californicus), or Pacific halibut (Hippoglossus stenolepis) in Northern California 1978+Pacific herring roe laid on kelp, 1987+Pacific herring (Clupea pallasi) all years, combine with HERRING, WHOLE FISH WITH ROEPacific herring (Clupea pallasi) containing roe, 1993+, combine with HERRING, PACIFIC Round herring (Etrumeus teres), may be some mislabled Pacific herring included 1962, 1985, 1992+Crevalle jack (Caranx hippos), 1992+Various species of jacks, family Carangidae 1992+Kahawai or Australian Salmon (Arripis spp), 1989 + 1999Giant kelpfish (Heterostichus robustus) and other kelpfishes 1979+California killifish (Funduous parvipinnis) 1993Lingcod (Ophiodon elongatus) called Pacific cultus before 1946,  all yearsCalifornia lizardfish (Synodus lucioceps) 1980+Louvar (Luvarus imperialis) 1984+Bullet mackerel (Auxis fochei) 1976+Jack mackerel (Trachurus symmetricus) a jack, called Horse mackerel before 1945, see unspecified mackerel 1977-85. Pacific mackerel (Scomber japonicus), see unspecified mackerel 1977-85.Mixture of jack mackerel and Pacific mackerel landed as mixed mackerel 1977+, for breakdown see Californias Living Marine Resources, p312-313.Striped marlin (Tetrapturus audax), not commercial after 1938Plainfin midshipman (Porichthys notatus) 1954, 1983+Mola or Ocean sunfish (Mola mola) 1982+Monkyface-eel or monkeyface prickleback (Cebidichthys violaceus) 1981+Longjaw mudsucker (Gilllichthys mirabilis) used for bait, 1971+Mullet (Mugil cephalus), some landings are from the Salton Sea in 1940s.Needlefish (Strongylura exilis) 1954+Oilfish (Ruvettus pretiosus) 1996+Opah (Lampris regius) 1976+Opaleye (Girella nigricans) included in perch category before 1954Market category includes surfperch in northern Calif. and other perch-like species in Soutnern Calif. included opaleye, halfmoon and sargo until 1954, blacksmith until 1962.  see Fish Bulletin 149 table 17Rockfish landed as market category Pacific Ocean Perch (Sebastes alutus) 1977+, species identification required 1984+Fish landed as Pacific Pomfret (Brama japonica)Queenfish (Seraphus politus) a croaker, included with white croaker before 1972.Ratfish (Hydrolagus colliei), occasional yearsBat ray (Myliobatis californica) 1977+, may be included with skates in earlier years.Pacific electric ray, (Torpedo californica) 1977+Rays not identified to species 1977+Rockfish landed as market category aurora (Sebastes aurora) may include other species, 1993+Rockfish landed as market category bank (Sebastes rufus) may include other species, 1980+Rockfish landed as market category blackgill (Sebastes melanostomus)  may include other species, 1980+Rockfish landed as market category black-and-yellow (Sebastes chrysomelas)  may include other species, 1979+Rockfish landed as market category black (Sebastes melanops)  may include other species, 1977+Rockfish landed as market category blue (Sebastes mystinus)  may include other species, 1977+Bocaccio rockfish (Sebastes paucispinus),1977+, species identification required from 1991+Rockfish landed as market category bronzespotted (Sebastes gilli)  may include other species, 1991+Rockfish landed as market category brown includes widow (Sebastes entomelas) and copper (S. caurinus) as well as brown (S. auriculatus) 1977+.  Also see BolinaRockfish landed as market category calico (Sebastes dallii)  may include other species. Rockfish landed as market category canary (Sebastes pinniger) may include other species before 1998, Canary Rockfish also in ROCKFISH, GROUP CANARY/VERMILLION 1994+Rockfish landed as market category chameleon (Sebastes phillipsi) may include other species, 1985+Rockfish landed as market category chilipepper (Sebastes goodei) may include other species, 1979+Rockfish landed as market category china (Sebastes nebulosa) may include other species, 1979+Rockfish landed as market categories copper (Sebastes caurinus) and whitebelly (S. vexillarius) may include other species, 1977+Rockfish landed as market category cowcod (Sebastes levis)  may include other species, 1979+Rockfish landed as market category darkblotched (Sebastes crameri) may include other species, 1996+Rockfish landed as market category flag (Sebastes rubrivinctus) may include other species, 1979+Rockfish landed as market category blue/black includes (Sebastes mystinus and S. melanops) may include other species, 1994+Rockfish landed as market category bocaccio/chillipepper (Sebastes paucispinis and S. goodei) may include other species 1980-93Rockfish landed as market category bolina includes brown (Sebastes auriculatus), copper (S. caurinus) and other species 1980+Rockfish landed as market category canary/vermillion 1994+.   may include other species.  See also ROCKFISH, NOMINAL CANARY and ROCKFISH, VERMILLION.Rockfish landed as market category deep reds may include various species of rockfish 1982+Rockfish landed as market category gopher group includes gopher (Sebastes carnatus) and other species 1985+, see also ROCKFISH, GOPHERRockfish landed as market category group red may include various species of rockfish 1980+Rockfish landed as market category group rosefish, primarily splitnose (Sebastes diploproa) and aurora (S. aurora) 1982+Rockfish landed as market category group smallred may include various species of small rockfish 1980+Rockfish landed as market category gopher may contain black-and-yellow (Sebastes chrysomelas), kelp (S. atrovirens) and others as well as gopher (Sebastes carnatus) 1979+Rockfish landed as market category grass (Sebastes rastrelliger)  may include other species, 1979+Rockfish landed as market category greenblotched (Sebastes rosenblatti) may include other species, 1990+Rockfish landed as market category greenstriped (Sebastes elongatus) may include other species, 1978+Rockfish landed as market category greenspotted (Sebastes chlorostictus)probably single species, 1979+Rockfish landed as market category honeycomb (Sebastes umbrosus) may include other species, 1993Rockfish landed as market category kelp rockfish (Sebastes atrovirens) may include other species, 1980+ see ROCKFISH, GOPHERRockfish landed as market category Mexican Rockfish 2002+Rockfish landed as market category olive (Sebastes serranoides) may include other species, 1979+Rockfish landed as market category pink (Sebastes eos) may include other species, 1987+Rockfish landed as market category pinkrose (Sebastes simulator) may include other species, 1987+Rockfish landed as market category quillback (Sebastes maliger) may include other species, 1994+Rockfish landed as market category redbanded (Sebastes nigrocinctus) may include other species, 1989+Rockfish landed as market category rosethorn (Sebastes helvomaculatus) may include other small species, 1991+Rockfish landed as market category rosy (Sebastes rosaceus) may include other small species, 1979+Rockfish landed as market category shortbelly (Sebastes jordani) may include other species, 1979+Rockfish landed as market category splitnose (Sebastes diploproa) may include other species,1977+Rockfish landed as market category speckled (Sebastes ovalis) may include other species,1979+Rockfish landed as market category squarespot (Sebastes hopkinsi) may include other species,1994+Rockfish landed as market category stripetail (Sebastes saxicola) may include other speciesRockfish landed as market category starry (Sebastes constellatus) may include other species,1977+Rockfish landed as market category swordspine (Sebastes ensifer) may include other small species,1996+Rockfish landed as market category treefish (Sebastes serriceps) may include other species,1980+Rockfish landed as market category unspecified rockfish, includes various species,1928+Rockfish landed as market category unspecified rockfish nearshore, from 2000Rockfish landed as market category unspecified rockfish shelf, from 2000Rockfish landed as market category unspecified rockfish slope, from 2000Rockfish landed as market category vermillion (Sebastes miniatus) may include other species,1977+, see also ROCKFISH, GROUP CANARY/VERMILLION,1977+Widow rockfish (Sebastes entomelas),1977+, identification required from 1983+Rockfish landed as market category yelloweye (Sebastes ruberrimus) may include other species,1977+Yellowtail rockfish (Sebastes flavidus) identification required from 1994+Sablefish or blackcod (Anoplopoma fimbria) all yearsSailfish (Istiophorus platypterus) 1982 onlySalema (Xenistius californiensis) 1971 and 1992 onlyAll species of salmon were recorded together before 1977, some unspecified landings 1978-85.Chum salmon (Oncorhycus keta) 1977+, see  before 1977King or chinook salmon (Oncorhycus tshawytscha) 1977+, see  before 1977The roe from king or chinook salmon. 1994+Pink salmon (Oncorhycus gorbuscha)  1977+, see  before 1977Silver or coho salmon (Oncorhycus kisutch) 1977+,  see  before 1977All species of sanddabs before 1990, and most of sanddab landings after 1990Longfin sanddab, (Citharichthys xanthostigma) 1999+Pacific sanddab (Citharichthys sordidus) 1990+Speckled sanddab (Citharichthys stigmaeus) 1997+Pacific sardine also called Pacific pilchard (Sardinops sagax) all years Sargo (Anisotremus davidsonii),1954+, included in perch category until 1953.Pacific saury (Cololabis saria) occasional landings.Spotted scorpionfish commonly called sculpin (Scorpeaena guttata), all yearsPacific staghorn sculpin (Leptocottus armatus) 1935, 1950s, 1978+Yellowchin sculpin (Icelinus quadriseriatus) 1994White seabass (Atractoscion noblis), all years Seniorita (Oxyjulis californica) 1973, 1994+American shad (Alosa sapidissima) Sactamento River delta fishery 1928-51, small amounts from 1986+Threadfin shad, (Dorosoma petenense) San Francisco Bay, 1987+ All shark landings until 1976, unspecified sharks, 1977+Pacific angel shark (Squatina californica), 1977+Blue shark (Prionace glauca), 1977+Bonito or short fin mako shark (Isurus oxyrinchus), 1977+Brown smoothound shark (Mustelus henlei), 1977+Basking shark (Cetorhinus maximus), 1991+Black tip shark presumably (Carcharhinus limbatus), 1984, 1993, 1999Common thresher shark (Alopias vulpinus), 1977+Cow sharks group sixgill (Hexanchus griseus) and sevengill (Notorynchus maculatus), see also Sevengill shark, 1977+Spiny dogfish (Squalus acanthias), 1977+Dusky shark (Carcharhinus obscurus), 1977+Smooth hammerhead shark (Sphyrna zygaena), 1977+Horn shark (Heterodontus francisci), 1977+Leopard shark (Triakis semifasciata), 1977+Salmon shark (Lamna ditropsis), 1977+Sevengill shark (Notorynchus maculatus) see also Cow shark, 1977+Sixgill shark (Hexanchus griseus) see also Cow sharks,1979+ Gray smoothound shark (Mustelus californicus), 1977+Soupfin shark (Galeorhinus zyopterus), in general shark category prior to 1977 include high landings 1936-44 for vitamin A in livers, separate category 1928-29, 1977+Swell shark (Cephaloscyllium ventriosum), 1978+Big eye thresher shark (Alopias superciliosus), 1980+Pelagic thresher shark (Alopias pelagicus) sometimes confused with common thresher shark (Alopias vulpinus), 1977+White shark (Carcharodon carcharias), 1979+California Sheephead (Semicossyphus pulcher), all yearsSierra (Scomberomorus sierra), 1987 onlySkates unspecified, may include rays also, all yearsBig skate (Raja binoculata), 1983+California skate (Raja inornata), 1978+Thorny skate, probably Thornback (Platyrhinoidis triseriata)Included all silversides and all smelts except those landed as whitebait until 1976, dominated by jacksmelt.Group called silversides, includes Jacksmelt (Atherinopsis californiensis) and Topsmelt (Atherinops affinis) 1978+, overlaps with individual species landingsJacksmelt (Atherinopsis californiensis) a silverside 1986+ Majority in Smelt before 1977, in SMELT, ATHERINID 1976-85+Night smelt (Spirinchus starski), may include surf smelt as well 1976+, see also SMELT, TRUESurf smelt (Hypomesus pretiosus) may include night smelt as well 1976+, see also SMELT, TRUETopsmelt (Atherinops affinis) a silverside. 1986+ Included in Smelt before 1977, in SMELT, ATHERINID 1976-85+Combined market categoty of surf smelt (Hypomesus pretiosus) and Night smelt (Spirinchus starski) 1976+,  overlaps with individual species landingsCombined market categoty of Whitebait smelt (Allosmerus elongatus) plus Night smelt (Spirinchus starski)  F&G Fish Bulletin 74 p142Unknwn species landed as snapper, 1994 onlyBig mouth sole (Hippoglossina stomata), 1979+Butter sole (Inopsetta isolepis), 1977+C-O sole or C-O turbot (Pleuronichthys coenosus) Curfin sole or curfin turbot (Pleuronichthys decurrens), 1994+.  May have been in either sole or turbot before 1994.Dover sole (Microstomus pacificus) included with sole unspecified 1948-1954.English sole (Parophrys vetulus) dominated the sole unspecified category before 1955.  See F&G Bulletin 149, p65.Fantail sole (Xystreurys liolepis), 1977+Petrale sole (Eopsetta jordani) 1954+, about 20% of sole unspecified before 1950.  See F&G Bulletin 149, p65.Rock sole (Lepidopsetta bilineata), 1977+Rex sole (Glyptocephalus zachirus) 1954+, less than 10% of unspecified soles before 1954.  See F&G Bulletin 149, p65.Sand sole (Psettichthys melanostictus), 1954+Slender sole (Lyopsetta exilis), 1979+All soles, dominated by English sole through 1948. Dover sole dominant 1949-53. Only unspecified soles after 1954.  See F&G Bulletin 149, p65. Unclassified surfperch 1977+.  Included in perch category before 1977.Barred surfperch (Amphistichus argenteus) 1977+.  Included in perch category before 1977.Black surfperch (Embiotoca jacksoni) 1977+.  Included in perch category before 1977 Calico surfperch (Amphistichus koelzi) 1980+.  Included in perch category before 1977 Dwarf surfperch (Micrometrus minimus) 1983, 1996.  Included in perch category before 1977Pile surfperch (Damalicthys vacca) 1979+.  Included in perch category before 1977Pink surfperch (Zalembius rosaceus) Rainbow surfperch (Hypsurus caryi) 1980+.  Included in perch category before 1977Redtail Surfperch (Amphistichus rhodoterus), 1979+.  Included in perch category before 1977Rubberlip surfperch (Rhacochilus toxotes), 1979+.  Included in perch category before 1977Shiner surfperch (Cymatogaster aggregata) 1979+.  Included in perch category before 1977Walleye surfperch (Hyperprosopon argenteum) 1977+.  Included in perch category before 1977White surfperch (Phanerodon furcatus) 1977+.  Included in perch category before 1977Threespine stickleback (Gasterosteus auleatus), San Francisco Bay, 1976+Unspecified rays, 1928 and 1977+White sturgeon (Acipenser transmontanus) not a commercial species, 1978+Swordfish (Xiphias gladius), all years.  Also called broadbill swordfish, landings are recorded as dressed weights, multiply by 1.45 to convert to weight of whole fish.Market categories thornyhead and thornyhead rockfish (Sebastolobus spp.), may include other species, 1977+, included in rockfish before 1977.Longspine thornyhead (Sebastolobus altivelis) 1995+, in Thornyhead 1978-95, in rockfish before 1977.Shortspine thornyhead (Sebastolobus alascanus) 1995+, in Thornyhead 1978-95, in rockfish before 1977.Pacific tomcod (Microgadus proximus) many yearsTriggerfish - possibly any of these triggerfish: finescale (Balistes polylepis), black (Mellichthys niger) or redtail (Xanthicthys mento) 1980+California toungefish (Symphurus atricauda) 1982+, included in sole unspecified before 1982.Albacore tuna (Thunnus alalunga) only landings from California waters catch, all yearsBigeye tuna (Thunnus obesus) only landings from California waters catch, 1977+Tuna landed as Blackfin tuna, presumably from common name (Thunnus atlanticus) which is a Western Atlantic species, 1985-93Bluefin tuna (Thunnus thynnus) only landings from California waters catch, all yearsSkipjack tuna (Euthynnus pelamis) only landings from California waters catch, most years.Black skipjack (Euthynnus lineatus) only landings from California waters catch,1972+Yellowfin tuna (Thunnus albacares) only landings from California waters catch, most years.Any species of tuna unspecified, presumably landings from California waters catch, 1978+Diamond turbot (Hypsopsetta guttulata) 1992Hornyhead turbot (Pleuronichthys verticalis) 1995+Spotted turbot (Pleuronichthys ritteri) noneAll species of turbot before 1992, unclassified turbot after 1991Wahoo (Acanthocybium solanderi) only landings from California waters catch, 1975+Ocean whitefish (Caulolatilus princeps)  all yearsPacific whiting, also called Pacific Hake (Merluccius productus) all yearsWolf-eel (Anarrhichthys ocellatus) 1977+, included in the category eels befoe 1977.Rock wrasse (Halichoeres semicinctus) 1984+Yellowtail (Seriola dorsalis)  only landings from California waters catch, all years.Zebraperch (Hermosilla azurea) 1977+, included in perch category before 1977Unspecified trawl caught groundfish, in the 1960s, this was sold for animal food to fur breeders. Unspecified marine fish for which identification was not made or in question, and those totaling small amounts before 1980. Freshwater fish Included with hardhead until 1969, also called greaser (Orthodon microlepidotus) (Fish Bulletin 74, p47.). Freshwater catch not in database after 1971  All carp landed until 1971, some large quantities remove from Clear Lake 1931-36 (Cyprinus carpio). Freshwater catch not in database after 1971(1928-52)All freshwater catfish landed, includes Forktail or channel catfish (Ictalurus catus) and square-tail,Sacramento catfish or bullhead (Ameiurus nebulosus).  Freshwater catch not in database after 1971All freshwater fish landed as hard head (Mylopharodon conocephalus), includes SACRAMENTO BLACKFISH in most years. Freshwater catch not in database after 1971All fish landed as hitch (Lavinia exilicauda) 1967-71. Freshwater catch not in database after 1971All freshwater fish landed as pike or Sacramento squawfish (Ptychocheilus grandis) (not true pike) 1928-1951. Freshwater catch not in database after 1971All freshwater fish landed as split-tail (Pogonichtys macrolepidotus) 1928-67. Freshwater catch not in database after 1971All freshwater fish landed as Sacramento or Western sucker (Catostomus occidentalis and other species) 1928-63. Freshwater catch not in database after 1971Barnacles 1980s.  presumably goose barnaclesBox crab 1981+Claws from rock crabs  before 1991 and sheep or spider crabs (Loxorhynchus grandis), 1986+Dungeness or market crab (Cancer magister) all years, may include some rock crabs from Santa Barbara south especially in 1940sKing crab, presumably Paralithodes species, 1977+Pelagic red crabs.  1974-88, 1999Rock crabs, unspecified, all years, however some may have been recorded as market crab before 1950. Brown Rock crabs (Cancer antennarius) 1997+Red rock crabs (Cancer productus) 1996+Yellow Rock crabs (Cancer anthonyi) 1995+Sand crabs (Emerita analoga ) harvested for live bait, not in database before 1979Shore crabs  (Hemigrapsus oregonensis or Pachygrapsus crassipes) harvested for live bait, not in database before 1978Sheep or spider crabs (Loxorhynchus grandis) 1968+Crab landed as tanner crab, presumably Chionocetes species 1983+Crayfish caught in rivers and Delta (Pacifastacus leniusculus or Procambarus clarkii) 1969-71, Freshwater catch not in database after 1971California Spiny Lobster (Panulirus interruptus)  all yearsPrawns landed as Golden prawns, 1999+  Ridgeback prawn (Sicyonia ingentis) fishery first developed in 1965 Spot prawn (Pandalus platyceros) taken in the ocean 1952+. Probably Spot prawn (Pandalus platyceros) taken in the ocean at least until 1960s, then maybe mixed species. Bay Shrimp (Crago franciscorum and C. nigricauda) caught in San Francisco Bay, recorded in shrimp unspecified until 1952Brine shrimp (Artemia salina) taken in bays Coonstripe or dock shrimp (Pandanlus danae) 1986+Ghost shrimp (Callianassa californiensis) harvested for bait from bays 1978+Pacific ocean shrimp (Pandulus jordani) 1952+Red rock shrimp (Lysmata californica) for live bait fishery developed in late 1950s but not in database until 1979.Bay Shrimp (Crago franciscorum and C. nigricauda) until 1952, may be various unspecified species after.Assorted unspecifid crustaceans. 1969+,Sea cucumbers giant (Parastichopus californicus) and warty (P.parvimensis) 1978+,Unspecified echinoderms 1971-1993,Sea stars unspecified  1971+,Sea Urchin landed as lytechinus 1983+,Purple sea urchin (Strongylocentrotus purperatus) 1985+,Red sea urchin (Strongylocentrotus franciscanus) 1986+ ,See Unspecified sea urchins 1971-85Unspecified sea urchins, probably red urchins.  1971-1985\",Black abalone (Haliotis cracherodii), 1972+\",Flat abalone (Haliotis wallensis), 1972+ \",Green abalone (Haliotis fulgens), 1972+\",Pink abalone (Haliotis corrugata), 1972+\",White abalone (Haliotis sorenseni), 1972+\",Threadded abalone (Haliotis assimilis), 1972+. now believed to be a southern sub-species of pinto (H. kamtschatkana) Calif.Living Mar.Res.p89\",Pinto abalone (Haliotis kamtschatkana), 1972+\",Red abalone (Haliotis ruffescens), 1972+\",All abalone (Haliotis spp.) until 1972, all red abalone until 1950, red and pink 1950-1970, unspecified abalone after 1971. Calif.Living Mar.Res.p89Unspecified chitons 1983+.Pacific gaper clam (Tresus nuttalli) and fat gaper (T. capax). 1931+ Jackknife clam (Tagelus californianus) 1931+, used for bait F&G Fish Bulletin 74 p161Littleneck clams, cockles or chiones  (Chione fluctifraga, and C. undullata., Protothaca  staminia, and Tapes philippinarum) Pacific razor clam (Siliqua patula), sport only after 1949.Purple clam, 1983 and 1994Pismo clam (Tivela stultorum) sport only after 1947, F&G Fish Bulletin 74 p165Rosy rasor clams, 1987+Soft shelled clams (Mya arenaria) before 1950Washington clams (Saxidomus nuttalli) until 1983.Unspecified clams Limpet unspecified, 1980+Unspecified mollusks 1954+Mussels Californian (Mytilus californianus), Mediterrranean  (M. galloprovincialis), and Bay (Mytelus edulis) primarily for bait 1928-1978, Octopus unspecified (Polypus spp), all yearsCalifornia native oyster (Ostreola lurida or O. conchaphila)Eastern oyster (Crassostrea virginica) maricultured.European flat oyster, maricultured.(Crassostrea gigas and C.sikamea) maricultured 1933+Oysters unspecified  1995+Scallops unspecified specied.Sea haresSea slugSea snails unspecifiedJumbo squid (Dosididicus gigas) 1978+Market squid (Loligo opalescens) all yearsWelks, probably Kellets Welk, 1930s and 1979+Freshwater frogs 1934-35 and 1971Colonial Invertbrates, sponges and tunicatesMarine worms unspecified, 1986+Fresh or brackish water turtle (or terrapin) species 1928-1931Primarily green sea turtles, 1928";
        String fishdescr[] = new String[nFish];
        po = 0;
        for (int f = 0; f < nFish; f++) {
            fishdescr[f] = fishDescrString.substring(po, po + ndescr.get(f));

            //remove junk at start
            int start = 0;
            while ("\"., ".indexOf(fishdescr[f].charAt(start)) >= 0)
                start++;
            if (start > 0) fishdescr[f] = fishdescr[f].substring(start);

            //remove junk at end
            int sLen = fishdescr[f].length();
            while ("\"., ".indexOf("" + fishdescr[f].charAt(sLen - 1)) >= 0) 
                sLen--;
            fishdescr[f] = fishdescr[f].substring(0, sLen) + ".";

            //other fixups
            if (fishdescr[f].startsWith("1977+ "))
                fishdescr[f] = fishdescr[f].substring(6);
            if (fishdescr[f].startsWith("(1928-41, 1995+)"))
                fishdescr[f] = fishdescr[f].substring(16);            
            //if (fishdescr[f].equals("Killifish, Californ")) fishdescr[f] = "Killifish, California";
            fishdescr[f] = String2.replaceAll(fishdescr[f], "Nortern", "Northern");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "inclded", "included");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "Dolfpnin", "Dolphin");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "Monkyface", "Monkeyface");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "Soutnern", "Southern");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "   ", " "); //few
            fishdescr[f] = String2.replaceAll(fishdescr[f], "  ", " "); //many
            fishdescr[f] = String2.replaceAll(fishdescr[f], ")probably", ") probably");
            fishdescr[f] = String2.replaceAll(fishdescr[f], ",1", ", 1");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "toungefish", "tonguefish");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "tail,Sac", "tail, Sac");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "remove from", "removed from");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "analoga )", "analoga)");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "1986+ ,See ", "1986+. See ");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "(Crassostrea gigas and C.sikamea)", "Crassostrea gigas and C. sikamea");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "unspecified specied.", "unspecified species.");
            fishdescr[f] = String2.replaceAll(fishdescr[f], ". m", ". M");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "1980s. presumably", "1980s. Presumably");
            fishdescr[f] = String2.replaceAll(fishdescr[f], "1972+. now", "1972+. Now");
            //fishdescr[f] = String2.replaceAll(fishdescr[f], "", "");
            if (fishdescr[f].indexOf("and and") >= 0 ||
                fishdescr[f].indexOf("primarioy") >= 0 ||
                fishdescr[f].indexOf("sps") >= 0) {
                String2.log("trouble: #" + f + "=" + fishdescr[f]);
            }
            String2.log(f + " " + fishdescr[f]);
            po += ndescr.get(f);
        }
        //if (true) System.exit(0);

        StringBuilder descriptions = new StringBuilder();
        for (int f = 0; f < nFish; f++) 
            descriptions.append("* " + fishnames[fishOrder[f]] + " - " + fishdescr[fishOrder[f]] + "\n");
        Attributes fishAtts = (new Attributes())
            .add("long_name", "Fish Name")
            .add("description", descriptions.toString());

        StringArray stationnames = (StringArray)PrimitiveArray.csvFactory(PAType.STRING,
            "San Diego, Los Angeles, Santa Barbara, Monterey, San Francisco, Eureka");
        Test.ensureEqual(nStations, stationnames.size(), "nStations != nStationnames");

        name = "landings";
        Attributes landingsAtts = new Attributes();
        OpendapHelper.getAttributes(das, name, landingsAtts);
        landingsAtts.set("units", "pounds");
        IntArray landings = (IntArray)OpendapHelper.getPrimitiveArray(dConnect, 
            "?landings.landings");
        Test.ensureEqual(landings.size(), nTime_series * nStations * nFish, "landings.size");

        //make the table  0=time 1=year 2=station 3=fish 4=landings
        DoubleArray timePA = new DoubleArray();
        ShortArray yearPA = new ShortArray();
        StringArray stationPA = new StringArray();
        StringArray fishPA = new StringArray();
        IntArray landingsPA = new IntArray();
        table.addColumn(0, "time", timePA, time_seriesAtts.add("units", Calendar2.SECONDS_SINCE_1970));
        table.addColumn(1, "year", yearPA, new Attributes());
        table.addColumn(2, "fish", fishPA, fishAtts);
        table.addColumn(3, "port", stationPA, (new Attributes()).add("long_name", "Port"));
        table.addColumn(4, "landings", landingsPA, landingsAtts);

        //add the data
        double tbf[] = Calendar2.getTimeBaseAndFactor("hours since 1900-01-01T00:00:00Z");
        
        for (int t = 0; t < nTime_series; t++) {
            double tt = time_series.get(t);
            double epSec = Calendar2.unitsSinceToEpochSeconds(tbf[0], tbf[1], tt);
            int tYear = String2.parseInt(Calendar2.epochSecondsToIsoDateString(epSec).substring(0, 4));

            int landingsAll[] = new int[nFish];  //0's
            for (int s = 0; s < nStations; s++) {
                int base = t * nStations * nFish +  s * nFish;

                for (int f = 0; f < nFish; f++) {
                    timePA.add(epSec);
                    yearPA.addInt(tYear);
                    fishPA.add(fishnames[f]);
                    stationPA.add(stationnames.get(s));
                    int tLandings = landings.get(base + f);
                    landingsPA.add(tLandings);
                    if (tLandings > 0) landingsAll[f] += tLandings; // >0 handles mv
                }

            }

            //add the data for the All Station
            for (int f = 0; f < nFish; f++) {
                timePA.add(epSec);
                yearPA.addInt(tYear);
                fishPA.add(fishnames[f]);
                stationPA.add("All");
                landingsPA.add(landingsAll[f]);
            }
        }
        stationnames.add("All");
        nStations++;

        //sort by fish,port,time
        table.sort(new int[]{2,3,0}, new boolean[]{true, true, true});

        //save in baseDir/station/fish.nc
        String monthlyDir = "C:/u00/data/points/erdCAMarCatLM/";
        String yearlyDir  = "C:/u00/data/points/erdCAMarCatLY/";
        File2.deleteAllFiles(monthlyDir, true, true); //recursive, deleteEmptySubdirectories
        File2.deleteAllFiles(yearlyDir,  true, true); //recursive, deleteEmptySubdirectories
        int nRows = table.nRows();
        for (int s = 0; s < nStations; s++) {
            //make subdirectory
            String tStation = stationnames.get(s);
            String tMonthlyDir = monthlyDir + String2.encodeFileNameSafe(tStation) + "/";
            String tYearlyDir  = yearlyDir  + String2.encodeFileNameSafe(tStation) + "/";
            File2.makeDirectory(tMonthlyDir);
            File2.makeDirectory(tYearlyDir);
            for (int f = 0; f < nFish; f++) {
                String tFish = fishnames[f];
                String2.log(tStation + " " + tFish);
                BitSet bitset = new BitSet(nRows);
                for (int row = 0; row < nRows; row++) 
                    bitset.set(row, fishPA.get(row).equals(tFish) && stationPA.get(row).equals(tStation));

                //* make the monthly table  0=time 1=year 2=station 3=fish 4=landings
                DoubleArray tTimePA    = (DoubleArray)timePA.clone();    tTimePA.justKeep(bitset);
                ShortArray tYearPA     = (ShortArray)yearPA.clone();     tYearPA.justKeep(bitset);
                StringArray tStationPA = (StringArray)stationPA.clone(); tStationPA.justKeep(bitset);
                StringArray tFishPA    = (StringArray)fishPA.clone();    tFishPA.justKeep(bitset);
                IntArray tLandingsPA   = (IntArray)landingsPA.clone();   tLandingsPA.justKeep(bitset);
                Table tTable = new Table();
                tTable.addColumn(0, "time", tTimePA,        (Attributes)table.columnAttributes(0).clone());
                tTable.addColumn(1, "year", tYearPA,        (Attributes)table.columnAttributes(1).clone());
                tTable.addColumn(2, "fish", tFishPA,        (Attributes)table.columnAttributes(2).clone());
                tTable.addColumn(3, "port", tStationPA,     (Attributes)table.columnAttributes(3).clone());
                tTable.addColumn(4, "landings", tLandingsPA,(Attributes)table.columnAttributes(4).clone());

                //save the monthly file
                tTable.saveAsFlatNc(tMonthlyDir + String2.encodeFileNameSafe(tFish) + ".nc", "row", false);     

                //* condense to yearly data
                int readRow = 0, writeRow = 0, tnRows = tTable.nRows();
                while (readRow < tnRows) {
                    short tYear = tYearPA.get(readRow);
                    int sum = tLandingsPA.get(readRow);
                    int nextRow = readRow + 1;
                    while (nextRow < nRows && tYear == yearPA.get(nextRow)) {
                        sum += tLandingsPA.get(nextRow);
                        nextRow++;
                    }
                    //store the data on writeRow;  change time to be tYear-07-01
                    tTimePA.set(writeRow, Calendar2.isoStringToEpochSeconds(tYear + "-07-01"));
                    tYearPA.set(writeRow, tYear);
                    tStationPA.set(writeRow, tStation);
                    tFishPA.set(writeRow, tFish);
                    tLandingsPA.set(writeRow, sum);
                    writeRow++;

                    readRow = nextRow;
                }            
                tTable.removeRows(writeRow, tnRows);

                //save the yearly file
                tTable.saveAsFlatNc(tYearlyDir + String2.encodeFileNameSafe(tFish) + ".nc", "row", false);     
            }
        }

        String2.log("\n*** Projects.getCAMarCatLong successfully created");
    }

    /** Test various hashmap implementations. */
    public static void testHashMaps() {
        String2.log("test regular HashMap");
        long time = System.currentTimeMillis();
        HashMap hashmap = new HashMap();
        for (int i = 0; i < 1000; i++) {
            String s = "" + i;
            hashmap.put(s, s);
        }
        String max = "";
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                String s = (String)hashmap.get("" + j);
                max = s.length() > max.length()? s : max;
            }
        }   
        String2.log("test regular hashMap finished longest=" + max + " time=" + 
            (System.currentTimeMillis() - time) + "ms");      //282     

        //***
        String2.log("test synchronized HashMap");
        time = System.currentTimeMillis();
        Map smap = Collections.synchronizedMap(new HashMap());
        for (int i = 0; i < 1000; i++) {
            String s = "" + i;
            smap.put(s, s);
        }
        max = "";
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                String s = (String)smap.get("" + j);
                max = s.length() > max.length()? s : max;
            }
        }   
        String2.log("test synchronized hashMap finished longest=" + max + " time=" + 
            (System.currentTimeMillis() - time) + "ms");      //343     

        //***
        String2.log("testConcurrentHashMap");
        time = System.currentTimeMillis();
        ConcurrentHashMap map = new ConcurrentHashMap(16, 0.75f, 4); //intentionally low initCapacity
        for (int i = 0; i < 1000; i++) {
            String s = "" + i;
            map.put(s, s);
        }
        max = "";
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                String s = (String)map.get("" + j);
                max = s.length() > max.length()? s : max;
            }
        }   
        String2.log("testConcurrentHashMap finished longest=" + max + " time=" + 
            (System.currentTimeMillis() - time) + "ms");   //282
    }


    /**
     * This processes Christina Show's SWFSC data from .tsv into .nc 
     * and does many table.join's to denormalize the data.
     *
     */
    public static void getChristinaShowsData() throws Throwable {
        String tsvDir = "c:/data/christina/tsv/";
        String ncDir  = "c:/data/christina/nc/";
        File2.makeDirectory(ncDir);
        File2.deleteAllFiles(ncDir);

        StringArray sa, sa1, sa2;
        Attributes atts;
        int nRows, po;
        String today = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);

        //*** global attributes
        Attributes globatts = new Attributes();
        globatts.add("cdm_data_type", "Trajectory");
        globatts.add("creator_name", "Christina Show and Kevin Hill");
        globatts.add("creator_email", "christina.show@noaa.gov"); //and kevin.hill@noaa.gov
        globatts.add("history", 
            "2010-10-25 Christina Show <christina.show at noaa.gov> saved the database data as an .mdb file.\n" +
            today + " Bob Simons <bob.simons at noaa.gov> joined/converted/reformatted as .nc files and added metadata.");
        globatts.add("infoUrl", "http://calcofi.net/"); 
        globatts.add("institution", "CalCOFI, NOAA SWFSC");
        globatts.add("license", "[standard]");
        globatts.add("references", "The data are from the CalCOFI Data Reports, 1 - 37.\n" +
"Additional information:\n" +
"\n" +
"Radovich, John, 1952.  Report on the young sardines, Sardinops caerulea,\n" +
"survey in California and Mexican waters, 1950 and 1951. Calif. Dept. Fish and\n" +
"Game, Fish Bull. 87, p. 31-63.\n" +
"\n" +
"Radovich, John, 1961.  Pelagic fish sea surveys (methodology).  Proceedings of\n" +
"the First National Coastal and Shallow Water Research Conference, p. 610-614.\n" +
"\n" +
"Radovich, John, and Earl d. Gibbs, 1954.  The use of a blanket net in sampling\n" +
"fish populations. Calif. Fish and Game, 48(4): 353-365.\n" +
"\n" +
"Sprague, Lucian M., and Andrew M. Vrooman, 1962. A racial analysis of the\n" +
"Pacific sardine (Sardinops caerulea) based on studies of erythrocyte antigens.\n" +
"New York Acad. Sci., Ann. 97: 131-138.\n" +
"\n" +
"Vrooman, Andrew M.,  1964.  Serologically differentiated subpopulations of the\n" +
"Pacific sardine, Sardinops caerulea.  Fish.  Res. Bd. Canada, Jour. 21(4):\n" +
"691 – 701.\n");
        globatts.add("sourceUrl", "(database data, converted to local files)");
        //customize subsetVariables, summary, and title (CalCOFI Data Reports, ...) below
        String project = 
"The California Department of Fish and Game has annually conducted a series of\n" +
"pelagic Fish survey cruises since 1950 as a part of the California Cooperative\n" +
"Oceanic Fisheries Investigations (CalCOFI) program.\n" +
"\n" +            
"In 1966 the surveys were changed completely both in scope and methodology.\n" +
"These new surveys are supported by the Federal Aid to Commercial Fisheries\n" +
"Research and Development funds, the project titled\n" +
"\"Fisheries Resources Sea Survey\".";

        //from QA-1.doc
        Attributes depthBTAtts = (new Attributes())
            .add("description", 
"Temperature readings derived from readings of depth at inflection\n" +
"points on the original traces of a bathythermograph cast.")
            .add("long_name", "Depth at which Temperature was Measured")
            .add("units", "m");
        Attributes diameterAtts = (new Attributes())
            .add("description",
"The calculated surface area of all anchovy schools on a station in square meters.\n" +
"Each school is considered round shaped.  Diameter is considered as the horizontal\n" +
"range from the near to far side of the school, as measured from sonargram\n" +
"recording paper.")
            .add("long_name", "Diameter of School")
            .add("units", "m"); //?
        Attributes speciesCodeAtts = (new Attributes())
            .add("long_name",     "Species Code")
            .add("ioos_category", "Fish Species");
        Attributes tempAtts = (new Attributes())
            .add("colorBarMinimum", 0.0)
            .add("colorBarMaximum", 32.0)
            .add("long_name",     "Sea Water Temperature")
            .add("standard_name", "sea_water_temperature")
            .add("units", "degree_C");
        Attributes yearClassAtts = (new Attributes())
            .add("long_name",     "Year Class")
            .add("description",   "This is the year that the fish spawned and hatched. " +
"Thus, it identifies a class of fish.")
            .add("ioos_category", "Biology");


        //*** LookUpTables
        //activity09
        Table activity09 = new Table();
        activity09.readASCII(tsvDir + "Activity09.tsv");
        activity09.setColumnName(0,                         "ActivityCode");  //will be removed
        activity09.setColumnName(1,                         "Activity");
        activity09.columnAttributes(1)
            .add("long_name",     "Activity")
            .add("ioos_category", "Other")
            .add("description",   "'Other' includes gill netting, beach seining, round, etc.");
        sa = (StringArray)activity09.getColumn(1);
        sa.intraReplaceAll("-blanket", "- blanket");
        sa.intraReplaceAll("Other, includes gill netting, beach seining, round",
                           "Other");
        String2.log("\n*** activity09=\n" + activity09.toString());

        //areaCode09
        Table areaCode09 = new Table();
        areaCode09.readASCII(tsvDir + "AreaCode09.tsv");
        areaCode09.setColumnName(0,                         "AreaCode");  //will be removed
        areaCode09.removeColumn(1); //alphaCode col never needed
        areaCode09.setColumnName(1,                         "Area");
        areaCode09.columnAttributes(1)
            .add("long_name",     "Area")
            .add("ioos_category", "Location");
        sa = (StringArray)areaCode09.getColumn(1);
        sa.intraReplaceAllIgnoreCase("\"Punta Baha - 28 \"\" 30.0' N.\"", "Punta Baha - 28.5N");
        sa.intraReplaceAllIgnoreCase("\"28\"\" 30.0' - punta Eugenia\"", "Punta Eugenia - 28.5N");
        String2.log("\n*** areaCode09=\n" + areaCode09.toString());

        //idMethod09
        Table idMethod09 = new Table();
        idMethod09.readASCII(tsvDir + "IdMethod09.tsv");
        idMethod09.setColumnName(0,                         "IdMethodCode"); //will be removed
        idMethod09.setColumnName(1,                         "IdMethod");
        idMethod09.columnAttributes(1)
            .add("long_name",     "ID Method")
            .add("ioos_category", "Fish Species")
            .add("description",
                "'Echo characteristic' - shape, size, intensity of echogram peculiar to certain species.");
        sa = (StringArray)idMethod09.getColumn(1);
        sa.intraReplaceAll("Echo characteristic (shape, size, intensity of echogram peculiar to certain species)",
            "Echo characteristic");
        sa.intraReplaceAll("detetions", "detections");  //ok???
        sa.intraReplaceAll(".", "");
        String2.log("\n*** idMethod09=\n" + idMethod09.toString());

        //lengthUnit09
        Table lengthUnit09 = new Table();
        lengthUnit09.readASCII(tsvDir + "LengthUnit09.tsv");
        lengthUnit09.setColumnName(0, "LengthUnitCode");
        lengthUnit09.columnAttributes(0)
            .add("long_name",     "Length Unit Code")
            .add("ioos_category", "Other");
        lengthUnit09.removeColumn(1); //alphaCode col never needed
        lengthUnit09.setColumnName(1, "LengthUnit");
        lengthUnit09.columnAttributes(1)
            .add("long_name",     "Length Unit")
            .add("ioos_category", "Other");
        sa = (StringArray)lengthUnit09.getColumn(1);
        sa.intraReplaceAllIgnoreCase("centimeters", "cm");
        sa.intraReplaceAllIgnoreCase("Inches", "inches");
        sa.intraReplaceAllIgnoreCase("millimeters", "mm");
        sa.intraReplaceAll("length,squid measurement", "length (squid)");
        sa.intraReplaceAll("length.", "length");
        String2.log("\n*** lengthUnit09=\n" + lengthUnit09.toString());

        //methodCapt09   
        Table methodCapt09 = new Table();
        methodCapt09.readASCII(tsvDir + "methodCapt09.tsv");
        String2.log("\n*** original methodCapt09=\n" + methodCapt09.dataToString());
        methodCapt09.setColumnName(0, "MethodLocatingCode"); //will be removed
        methodCapt09.removeColumn(2); //Description moved to metadata
        methodCapt09.setColumn(1, StringArray.fromCSV(
", MWT30, MWT50, Blanket, DipNet, Trolling, HookLine, BeachSeine, RoundHaul" +
", OtterTrawl, GillNet, Other, Dynamite, 60Q, 60L, 60H, 30H, COT, MWT"));
        methodCapt09.ensureValid(); //throws Exception if not
        methodCapt09.setColumnName(1,                         "MethodLocating");
        methodCapt09.columnAttributes(1)
            .add("long_name",     "Method Locating")
            .add("ioos_category", "Other")
            .add("description",
        //bob modified the Alpha Codes since they weren't in original data and I'm using them
        //and changed: additoin to addition, added ')' at end of other, 
        //    measureing to measuring, selcom to seldom,
        //    "( no designation )" to "(no designation)"
        //    COT  "35 feet" to "35 foot-square"  ok???
        //and many tiny syntax changes
"  METHOD     DESCRIPTION\n" +                            
"*            Missing data\n" +  //0
"* MWT30      Midwater trawl, 30 foot-square mouth\n" +
"* MWT50      Midwater trawl, 50 foot-square mouth\n" +
"* Blanket    Blanket net\n" +
"* DipNet     Dip net (under a night light)\n" +
"* Trolling   Trolling\n" + //5
"* HookLine   Hook and line\n" +
"* BeachSeine Beach seine\n" +
"* RoundHaul  Round haul net (lampara or purse seine)\n" +
"* OtterTrawl Otter trawl\n" +
"* GillNet    Gill net\n" + //10
"* Other      Other (includes traps, poison, dredges, spear, etc)\n" +
"* Dynamite   Dynamite\n" +
"* 60Q        60 foot-square mouth opening with four flat otter boards measuring 4x3 feet.  One door was attached to each of the four corners of the trawl.\n" +
"* 60L        60 foot-square mouth opening same as 60Q with addition of two 3x5 foot conventional bottom trawl otter doors at juncture of bridles and towing warps.\n" +
"* 60H        60 foot-square mouth opening same as 60Q with different doors.  Two 5x8-foot hydrofoil doors were attached to the upper corners of the trawl and two 4x4 foot hydrofoil doors attached to the bottom corners.  The headrope was bouyed by floats & chain.\n" +
"* 30H        30 foot-square opening trawl which was spread in same manner as above but with smaller hydrofoil doors.\n" +
"* COT        A seldom-used early model cotton twine trawl.  Mouth opening was approximately 35 foot-square.\n" +
"* MWT        Midwater trawl (no designation)\n");  //18
        String2.log("\n*** methodCapt09=\n" + methodCapt09.toString());

        //MethodLocating09    (missing value is code="0")
//Code	AlphaCode	Description
//0		No attempts were made for hook-and-line or oceanographic stations, etc.
//1	V	Visual
        Table methodLocating09 = new Table();
        methodLocating09.readASCII(tsvDir + "methodLocating09.tsv");
        methodLocating09.removeColumn(1);  //get rid of AlphaCode
        methodLocating09.setColumnName(1, "MethodLocating");
        methodLocating09.columnAttributes(1)
            .add("Description",   "The method used to locate the fish. For method=\"No attempt\",\n" +
                                  "no attempts were made for hook-and-line or oceanographic stations, etc.")
            .add("long_name",     "Method Locating")
            .add("ioos_category", "Identifier");
        methodLocating09.setStringData(1, 1, "No attempt");
        String2.log("\n*** methodLocating09=\n" + methodLocating09.toString());

        //speCode09
        Table speCode09 = new Table();
        speCode09.readASCII(tsvDir + "SpeCode09.tsv");
        speCode09.setColumnName(0, "SpeciesNumber");  //will be kept
        speCode09.setColumnName(1, "CommonName");
        speCode09.setColumnName(2, "ScientificName");
        speCode09.columnAttributes(0)
            .add("long_name", "Species Number")
            .add("ioos_category", "Fish Species");
        speCode09.columnAttributes(1)
            .add("long_name", "Common Name")
            .add("ioos_category", "Fish Species");
        speCode09.columnAttributes(2)
            .add("long_name", "Scientific Name")
            .add("ioos_category", "Fish Species");
        sa1 = (StringArray)speCode09.getColumn(1);
        sa2 = (StringArray)speCode09.getColumn(2);
        nRows = sa1.size();
        for (int row = 0; row < nRows; row++) {
            sa1.set(row, String2.toTitleCase(sa1.get(row))); 
            sa2.set(row, String2.toSentenceCase(sa2.get(row)));
            if (sa2.get(row).endsWith(" sp"))
                sa2.set(row, sa2.get(row) + ".");
            sa2.switchFromTo("Unknow", "Unknown");

        }
        String2.log("\n*** speCode09=\n" + speCode09.toString(20));

        //stType09
        Table stType09 = new Table();
        stType09.readASCII(tsvDir + "StType09.tsv");
        stType09.setColumnName(0,                         "StationTypeCode");  //will be removed
        stType09.setColumnName(1,                         "StationType");
        stType09.columnAttributes(1)
            .add("long_name",     "Station Type")
            .add("ioos_category", "Identifier")
            .add("description",   "'Other' includes gill netting, beach seining, round, etc.");
        sa = (StringArray)stType09.getColumn(1);
        sa.intraReplaceAll("data.", "data");
        sa.intraReplaceAll("scouting.", "scouting");
        sa.intraReplaceAll("Unknown.", "Unknown");
        sa.intraReplaceAll(",round", ", round");
        sa.intraReplaceAll("-blanket", "- blanket");
        sa.intraReplaceAll("Other, includes gill netting, beach seining, round", "Other");
        String2.log("\n*** stType09=\n" + stType09.toString());

        //vesselName   (I put the info in the comments for CuiseNo)
        Table vesselName = new Table();
        vesselName.readASCII(tsvDir + "VesselName.tsv");
        vesselName.setColumnName(0, "VesselCode");  
        vesselName.columnAttributes(1)
            .add("long_name",     "Vessel Name")
            .add("ioos_category", "Identifier");
        String2.log("\n*** vesselName09=\n" + vesselName.toString());


        //*** stationData09        
        Table stationData09 = new Table();
        stationData09.readASCII(tsvDir + "stationData09.tsv");
        stationData09.columnAttributes(0)
            .add("long_name",     "Cruise Number")
            .add("ioos_category", "Identifier")
            .add("description",   
               "The cruise number is generated from 2DigitYear + VesselCode + Line#\n" +
               "The VesselCodes are A=Alaska, P=Paolina T, S=N.B.Scofield, X=Charter Vessels, Y=Yellowfin.");
        stationData09.columnAttributes(1)
            .add("long_name",     "Station Number")
            .add("ioos_category", "Identifier")
            .add("description",   "The station number is generated from ???");  //???
        //join StType
        po = stationData09.findColumnNumber("StType");
        stationData09.join(1, po, "-1", stType09); 
        //stationData09.removeColumn(po); //keep for now since it 
        //join AreaCode
        po = stationData09.findColumnNumber("AreaCode");
        stationData09.join(1, po, "0", areaCode09); 
        stationData09.removeColumn(po);
        //make lon, lat, lon2, lat2, time, 
        int latDPo  = stationData09.findColumnNumber("LatD");
        int latMPo  = stationData09.findColumnNumber("LatM");
        int lonDPo  = stationData09.findColumnNumber("LongD");
        int lonMPo  = stationData09.findColumnNumber("LongM");
        int latD2Po = stationData09.findColumnNumber("LatD2");
        int latM2Po = stationData09.findColumnNumber("LatM2");
        int lonD2Po = stationData09.findColumnNumber("LongD2");
        int lonM2Po = stationData09.findColumnNumber("LongM2");
        int monthPo = stationData09.findColumnNumber("Month");
        int dayPo   = stationData09.findColumnNumber("Day");
        int yearPo  = stationData09.findColumnNumber("Year");
        int hourPo  = stationData09.findColumnNumber("Hour");   
        int minutePo= stationData09.findColumnNumber("Minute");
        nRows = stationData09.nRows();
        FloatArray lonPA   = new FloatArray( nRows, false);
        FloatArray latPA   = new FloatArray( nRows, false);
        FloatArray lon2PA  = new FloatArray( nRows, false);
        FloatArray lat2PA  = new FloatArray( nRows, false);
        DoubleArray timePA = new DoubleArray(nRows, false);
        for (int row = 0; row < nRows; row++) {
            lonPA.add( stationData09.getIntData(lonDPo,  row) + stationData09.getIntData(lonMPo,  row) / 60f);
            latPA.add( stationData09.getIntData(latDPo,  row) + stationData09.getIntData(latMPo,  row) / 60f);
            lon2PA.add(stationData09.getIntData(lonD2Po, row) + stationData09.getIntData(lonM2Po, row) / 60f);
            lat2PA.add(stationData09.getIntData(latD2Po, row) + stationData09.getIntData(latM2Po, row) / 60f);
            timePA.add(Calendar2.gcToEpochSeconds(
                Calendar2.newGCalendarLocal(            //???local or zulu???  daylight saving time?
                    stationData09.getIntData(yearPo,   row),
                    stationData09.getIntData(monthPo,  row),
                    stationData09.getIntData(dayPo,    row),
                    stationData09.getIntData(hourPo,   row),
                    stationData09.getIntData(minutePo, row), 0, 0)));
        }
        stationData09.removeColumns(latDPo, minutePo+1);
        stationData09.addColumn(latDPo + 0, "longitude",  lonPA,  (new Attributes())
            .add("description",   "The starting (or the only) longitude where measurements were taken."));
        stationData09.addColumn(latDPo + 1, "latitude",   latPA,  (new Attributes())
            .add("description",   "The starting (or the only) latitude where measurements were taken."));
        stationData09.addColumn(latDPo + 2, "longitude2", lon2PA, (new Attributes())
            .add("description",   "The ending longitude where measurements were taken.")
            .add("ioos_category", "Location")
            .add("long_name",     "End Longitude")    //???
            .add("standard_name", "longitude")   
            .add("units",         "degrees_east"));
        stationData09.addColumn(latDPo + 3, "latitude2",  lat2PA, (new Attributes())
            .add("description",   "The ending latitude where measurements were taken.")
            .add("ioos_category", "Location")
            .add("long_name",     "End Latitude")    //???
            .add("standard_name", "latitude")   
            .add("units",         "degrees_north"));
        stationData09.addColumn(latDPo + 4, "time",       timePA, (new Attributes())
            .add("description",   "The starting (or the only) time when measurements were taken.")); //???
        //join MethodLocating
        po = stationData09.findColumnNumber("MethodLocating");
        stationData09.join(1, po, "0", methodLocating09); 
        stationData09.removeColumn(po);
        //join MethodCapt
        po = stationData09.findColumnNumber("MethodCapt");
        stationData09.join(1, po, "0", methodCapt09); 
        stationData09.removeColumn(po);
        //join MethodCapt2
        po = stationData09.findColumnNumber("MethodCapt2");
        stationData09.join(1, po, "0", methodCapt09); 
        stationData09.removeColumn(po);
        stationData09.setColumnName(po, "MethodCapture2");
        stationData09.columnAttributes(po)
            .add("description", "The second capture method (if any). \n" +
                stationData09.columnAttributes(po).getString("description"));
//join SchoolType     //SchoolType needs work first
        //po = stationData09.findColumnNumber("SchoolType");
        //stationData09.join(1, po, ""?, schoolType09); 
        //stationData09.removeColumn(po);
        //join IdMethod
        po = stationData09.findColumnNumber("IdMethod");
        stationData09.join(1, po, "0", idMethod09); 
        stationData09.removeColumn(po);
        stationData09.columnAttributes(stationData09.findColumnNumber("Duration"))
            .add("units",         "minutes");
        stationData09.columnAttributes(stationData09.findColumnNumber("OffDist"))
            .add("long_name",     "Minimum Distance Offshore")
            .add("units",         "nautical_miles");
        po = stationData09.findColumnNumber("Offdist2");
        stationData09.setColumnName(po, "Offdist2");              //!!! Christine should fix this.
        stationData09.columnAttributes(po)
            .add("long_name",     "Maximum Distance Offshore")
            .add("units",         "nautical_miles");
        stationData09.columnAttributes(stationData09.findColumnNumber("Depth"))
            .add("description",   "Minimum (or only) estimate of ocean depth.")
            .add("units",         "fathoms");
        stationData09.columnAttributes(stationData09.findColumnNumber("Depth2"))
            .add("description",   "Maximum estimate of ocean depth.")
            .add("units",         "fathoms");
        stationData09.columnAttributes(stationData09.findColumnNumber("SSTemp"))
            .add("colorBarMinimum", 0.0)
            .add("colorBarMaximum", 32.0)
            .add("long_name",     "Sea Surface Temperature")
            .add("standard_name", "sea_surface_temperature")
            .add("units",         "degree_C");
        po = stationData09.findColumnNumber("BTData");
        stationData09.setColumnName(po, "BTCast");   
        stationData09.columnAttributes(po)
            .add("description",   "Indicates if a bathythermograph cast was made: 0=No, 1=Yes.");
        stationData09.columnAttributes(stationData09.findColumnNumber("MilesSounded"))
            .add("description",   "For acoustic stations, this is the number of miles sounded.")
            .add("units",         "nautical_miles");
        po = stationData09.findColumnNumber("MilesScounted");
        stationData09.setColumnName(po, "MilesScouted");          //!!!Christine should fix this.
        stationData09.columnAttributes(po)
            .add("description",   "The distance during which a visual watch was maintained for surface schools.")
            .add("units",         "nautical_miles");
        stationData09.columnAttributes(stationData09.findColumnNumber(""))
            .add("description",   "");
        stationData09.columnAttributes(stationData09.findColumnNumber(""))
            .add("description",   "");
        stationData09.columnAttributes(stationData09.findColumnNumber(""))
            .add("description",   "");
        stationData09.columnAttributes(stationData09.findColumnNumber(""))
            .add("description",   "");
        stationData09.columnAttributes(stationData09.findColumnNumber(""))
            .add("description",   "");

        String2.log("\n*** stationData09=\n" + stationData09.toString(20));
        for (int col = 0; col < stationData09.nColumns(); col++) {
            if (!(stationData09.getColumn(col) instanceof StringArray)) {
                String2.log(String2.left(stationData09.getColumnName(col),14) + " " + 
                    stationData09.getColumn(col).statsString());
            }
        }

        //common SubsetVariables
        String cruiseSSVars = "CruiseNo, StationNo, StType, AreaCode";
        String speciesSSVars = "SpeciesCode, CommonName, ScientificName";

        //*** RESULTS TABLES
        //AFData09
        Table afData09 = new Table();
        afData09.readASCII(tsvDir + "AFData09.tsv");
        afData09.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Age Frequency Data")
            .add("summary", 
"This dataset has length and year-class information for the listed species.\n" +
"The length of each fish whose age (year-class) was determined and the 4 digits\n" +  //was aged
"of its year class are listed for each station.  The length measurement units\n" +
"vary, and are indicated in the lenghUnit field; these are the units in which\n" +
"the fish were originally measured.\n\n" +
project)
            .add("subsetVariables", cruiseSSVars + ", " + speciesSSVars);
        afData09.columnAttributes(6)
            .add(yearClassAtts);
        //join StationData
        po = afData09.findColumnNumber("CruiseNo");
        afData09.join(3, po, "", stationData09); 
        //keep the keyColumns 
        //join SpeCode
        po = afData09.findColumnNumber("SpeCode");
        afData09.join(1, po, "0", speCode09); 
        afData09.setColumnName(po, "SpeciesCode"); //don't remove SpeCode
        afData09.columnAttributes(po)
            .add(speciesCodeAtts); 
        //join LengthUnit
        po = afData09.findColumnNumber("LengthUnit");
        afData09.join(1, po, "0", lengthUnit09); 
        afData09.removeColumn(po);
        afData09.columnAttributes(afData09.findColumnNumber("Length"))
            .add("description",   "See the LengthUnit column for what was measured and for the units.")
            .add("ioos_category", "Biology");
        afData09.columnAttributes(afData09.findColumnNumber("YearClass"))
            .add(yearClassAtts);
        String2.log("\n*** afData09=\n" + afData09.toString(20));

        //AnchSchoolData
        Table anchSchoolData = new Table();
        anchSchoolData.readASCII(tsvDir + "AnchSchoolData.tsv");
        anchSchoolData.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Anchove School Data")
            .add("summary", 
"This dataset presents results of traversing transect lines with an echo sounder\n" +
"and sonar and counting schools of Anchovy detected.  A station usually consists\n" +
"of 1 to 2 hours of sounding at a vessel speed of 7-10 knots.  Transects\n" +
"generally run perpendicular to the coastline and extend between the 10 and\n" +
"1,000 fathom depth contour intervals.  In areas where deep water is found close\n" +
"to shore, the seaward extent is 35 miles.  Spacing of transect lines varies\n" +
"between 6 and 30 miles.  All sounding was done during daylight hours.  A 38 kHz\n" +
"echo sounder coupled to an Ocean Sonics Precision Depth Recorder was used.\n" +
"A 29 kHz sonar was fixed in a horizontal plane 75-90 degrees from the vessel's\n" +
"heading.  The diameters listed are the lower limits of the class intervals,\n" +
"where the width of each class interval is 5 meters 5 – 9, 10 – 14, etc.  In\n" +
"addition, only for years between 1974 – 1979, inclusive collected this type of\n" +
"data.\n\n" + project)
            .add("subsetVariables", cruiseSSVars);
        anchSchoolData.columnAttributes(3)  //Diameter
            .add("description",   
"The calculated surface area of all anchovy schools on a station in square\n" +
"meters.  Each school considered round shaped.  Diameter considered horizontal\n" +
"range from near to far side of school as measured from sonargram recording\n" +
"paper.")
            .add("ioos_category", "Biology")
            .add("units",         "m^2");
        anchSchoolData.columnAttributes(4)  //Frequencies
            .add("description", "The number of anchovy schools seen from sonargram at a station.")
            .add("ioos_category", "Fish Abundance")
            .add("units",         "count");
        //join StationData
        po = anchSchoolData.findColumnNumber("CruiseNo");
        anchSchoolData.join(3, po, "", stationData09); 
        //keep the keyColumns 
        anchSchoolData.columnAttributes(afData09.findColumnNumber("Frequencies"))        
            .add("description", "Number of anchovy schools seen from the sonargram at a station.")
            .add("units",       "count");
        String2.log("\n*** anchSchoolData=\n" + anchSchoolData.toString(20));

        //BTData09
        Table btData09 = new Table();
        btData09.readASCII(tsvDir + "BTData09.tsv");
        btData09.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Bathythermograph Data")
            .add("summary", 
"This dataset contains temperatures in degrees Celsius at selected depths for\n" +
"each station where a bathythermograph cast was made.  The entries are derived\n" +
"from readings of depth and temperatures at inflection points on the original\n" +
"traces; these are converted to meters and degrees Celsius where necessary.\n" +
"The table entries are then calculated by interpolation between inflection\n" +
"points.\n\n" + project)
            .add("subsetVariables", cruiseSSVars);
        btData09.columnAttributes(2)
            .add("units",         "nautical_mile"); 
        btData09.columnAttributes(3)
            .add(depthBTAtts);
        btData09.columnAttributes(4)
            .add(tempAtts);
        //join StationData
        po = btData09.findColumnNumber("CruiseNo");
        btData09.join(3, po, "", stationData09); 
        //keep the keyColumns 
        String2.log("\n*** btData09=\n" + btData09.toString(20));

        //LFData09
        Table lfData09 = new Table();
        lfData09.readASCII(tsvDir + "LFData09.tsv");
        lfData09.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Length Frequency Data")
            .add("summary", 
"This dataset summarizes the length frequencies of the following species:\n" +
"Pacific sardine, Northern anchovy, Pacific saury, Pacific hake, Jack mackerel,\n" +
"Pacific mackerel, and Market squid.  The data are the number of fish by size\n" +
"groups at each station.  The size group is indicated by the midpoint of the\n" +
"group interval.  The length measurement units are indicated by the unitLength\n" +
"field.\n\n" + project)
            .add("subsetVariables", cruiseSSVars + ", " + speciesSSVars);
        lfData09.columnAttributes(3)
            .add("long_name",     "Species Code")
            .add("ioos_category", "Fish Species");
        lfData09.columnAttributes(5)
//            .add("description",   "...
            .add("long_name",     "Midpoint of this Size Group")
            .add("ioos_category", "Biology");
        lfData09.columnAttributes(6)
            .add("long_name",     "Frequency")
            .add("ioos_category", "Fish Abundance")
            .add("units",         "???");           //??? 
        //join StationData
        po = lfData09.findColumnNumber("CruiseNo");
        lfData09.join(3, po, "", stationData09); 
        //keep the keyColumns 
        //join SpeCode
        po = lfData09.findColumnNumber("SpeCode");
        lfData09.join(1, po, "0", speCode09); 
        lfData09.setColumnName(po, "SpeciesCode"); //don't remove SpeCode
        lfData09.columnAttributes(po).add(speciesCodeAtts); 
        //join LengthUnit
        po = lfData09.findColumnNumber("LengthUnit");
        lfData09.join(1, po, "0", lengthUnit09); 
        lfData09.removeColumn(po);
        String2.log("\n*** lfData09=\n" + lfData09.toString(20));


        //NSData09
        Table nsData09 = new Table();
        nsData09.readASCII(tsvDir + "NSData09.tsv");
        nsData09.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Night Scouting Data")
            .add("summary", 
"This dataset lists the miles scouted and the number of schools seen in each area\n" +
"covered by a cruise (the area descriptions are given under AreaCode. Data exit\n" + //exist for?
"during the years 1950 – 1965.\n" +
"\n" +
"The number of sardine, anchovy, mackerel, miscellaneous, and unidentified\n" +
"schools is given.\n" +
"\"Sardine\" includes Pacific sardines only;\n" +
"\"Anchovies\" includes northern anchovies and unidentified anchovies;\n" +
"\"Mackerel\" includes Pacific mackerel, jack mackerel, and unidentified mackerel;\n" +
"\"Miscellaneous\" includes all other identified schools;\n" +
"\"unidentified\" includes schools that were not identified.\n\n" + project)
            .add("subsetVariables", "CruiseNo, AreaCode");
        nsData09.columnAttributes(0)
            .add("long_name",     "Cruise Number")
            .add("ioos_category", "Identifier");
        nsData09.columnAttributes(3)
            .add("ioos_category", "Fish Abundance")
            .add("units",         "???");           //??? 
        nsData09.columnAttributes(4)
            .add("ioos_category", "Fish Abundance")
            .add("units",         "???");           //??? 
        nsData09.columnAttributes(5)
            .add("ioos_category", "Fish Abundance")
            .add("units",         "???");           //??? 
        nsData09.columnAttributes(6)
            .add("long_name",     "Miscellaneous")
            .add("ioos_category", "Fish Abundance")
            .add("units",         "???");           //??? 
        nsData09.setColumnName(   7, "Unidentified");
        nsData09.columnAttributes(7)
            .add("ioos_category", "Fish Abundance")
            .add("units",         "???");           //??? 
        //join areaCode09
        po = nsData09.findColumnNumber("AreaCode");
        nsData09.join(1, po, "0", areaCode09); 
        nsData09.removeColumn(po);
        String2.log("\n*** nsData09=\n" + nsData09.toString(20));

        //speciesData09
        Table speciesData09 = new Table();
        speciesData09.readASCII(tsvDir + "SpeciesData09.tsv");
        speciesData09.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Species Data")
            .add("summary", 
"This dataset contains species caught, observed, or identified for each\n" +
"station, cruise and stations information, indicator whether the captured\n" +
"fish were measured or aged and the maximum and minimum fish lengths when\n" +
"measurements were taken.  The species were identified by the 4 digit CalCOFI\n" +
"species codes.\n\n" + project)
            .add("subsetVariables", cruiseSSVars + ", " + speciesSSVars);
        speciesData09.columnAttributes( 3)
            .add("long_name",     "Species Code")
            .add("ioos_category", "Fish Species");
        speciesData09.columnAttributes( 4)
            .add("long_name",     "Number Captured") //???
            .add("ioos_category", "Fish Abundance");
        speciesData09.join(1, 5, "0", methodCapt09); 
        speciesData09.removeColumn(5);
        speciesData09.columnAttributes( 6)
            .add("long_name",     "Minimum Length")
            .add("ioos_category", "Biology");
        speciesData09.columnAttributes( 7)
            .add("long_name",     "Maximum Length")
            .add("ioos_category", "Biology");
        speciesData09.columnAttributes( 9)
            .add("long_name",     "Length")
            .add("ioos_category", "Biology");
        speciesData09.columnAttributes(10)
            .add("ioos_category", "Biology");
        //join StationData
        po = speciesData09.findColumnNumber("CruiseNo");
        speciesData09.join(3, po, "", stationData09); 
        //keep the keyColumns 
        //join SpeCode
        po = speciesData09.findColumnNumber("SpeCode");
        speciesData09.join(1, po, "0", speCode09); 
        speciesData09.setColumnName(po, "SpeciesCode"); //don't remove SpeCode
        speciesData09.columnAttributes(po).add(speciesCodeAtts);
        //join LengthUnit
        po = speciesData09.findColumnNumber("LenUnit");
        speciesData09.join(1, po, "0", lengthUnit09); 
        speciesData09.removeColumn(po);
        String2.log("\n*** speciesData09=\n" + speciesData09.toString(20));

        //speciesTally09
        Table speciesTally09 = new Table();
        speciesTally09.readASCII(tsvDir + "SpeciesTally09.tsv");
        speciesTally09.globalAttributes()
            .add(globatts)
            .add("title", "CalCOFI Data Reports, Species Tally Data")
            .add("summary", 
"This dataset gives a list of species code numbers of all species seen or\n" +
"captured at all stations.\n" +
"\n" +
"The numbers of schools detected by echo sounder or sonar station type are\n" +
"listed under NoSchool field.  A zero or blank school number under NoSchool\n" +
"field indicates a non-schooling behavior.\n" +
"\n" +
"Station numbers with non-zero decimal digit denote echo sounding stations\n" +
"such as 1.02, 1.03, etc.  Station numbers without decimal digit are midwater,\n" +
"or engaging in activities other than using echo sounder or sonar.\n\n" + project)
            .add("subsetVariables", cruiseSSVars + ", Activity, " + speciesSSVars);
        speciesTally09.columnAttributes(5)
            .add("long_name",     "Number of Schools")
            .add("ioos_category", "Fish Abundance");
        //join StationData
        po = speciesTally09.findColumnNumber("CruiseNo");
        speciesTally09.join(3, po, "", stationData09); 
        //keep the keyColumns 
        //join activity09
        po = speciesTally09.findColumnNumber("Activity");
        speciesTally09.join(1, po, "-1", activity09); 
        speciesTally09.removeColumn(po);
        //join SpeCode
        po = speciesTally09.findColumnNumber("SpeCode");
        speciesTally09.join(1, po, "0", speCode09); 
        speciesTally09.setColumnName(po, "SpeciesCode"); //don't remove SpeCode
        speciesTally09.columnAttributes(po).add(speciesCodeAtts);
        String2.log("\n*** speciesTally09=\n" + speciesTally09.toString(20));


        //done
        String2.log("\n*** Projects.getChristinaShowsData() finished successfully.");
    }

    /** Reorganize the calcofiBio data in f:/data/calcofiBio for erddap. 
     * 
     * 2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons 
     *   ftp://192.168.31.10/outgoing/ldewitt/Simons/calcofi/
     * 2010-12-29 Bob Simons reorganized the files with Projects.calcofiBio().
     */
    public static void calcofiBio() throws Exception {
        String2.log("\n*** Projects.calcofiBio");
        long elapsedTime = System.currentTimeMillis();
        String oldDir = "c:/data/calcofiBio/";
        String newDir = "c:/u00/data/points/erdCalcofiBio/";
        File2.deleteAllFiles(newDir);        
        String today = Calendar2.getCurrentISODateStringLocal();

        //quick look at one file
        if (false) {
            String2.log("Quick look at one file");
            Table table = new Table();
            table.readNDNc(oldDir + "calcofiBio_19840211_66.7_65_NH16.nc",
                null, 0,  //standardizeWhat=0
                null, Double.NaN, Double.NaN);
            String2.log(table.toString());
            System.exit(0);
        }

        String timeUnits= "minutes since 1948-1-1";
        //depth:long_name = "depth at start of tow" ;
        //"Order of Occupancy"

        StringArray lineStationPA  = new StringArray();
        FloatArray  linePA         = new FloatArray();
        FloatArray  stationPA      = new FloatArray();
        FloatArray  lonPA          = new FloatArray();
        FloatArray  latPA          = new FloatArray();
        DoubleArray timePA         = new DoubleArray();
        FloatArray  depthPA        = new FloatArray();
        IntArray    occupyPA       = new IntArray();

        IntArray    cruiseNumberPA = new IntArray();
        StringArray shipNamePA     = new StringArray();
        StringArray shipCodePA     = new StringArray();

        StringArray obsCommonPA    = new StringArray();
        StringArray obsScientificPA= new StringArray();
        IntArray    valuePA        = new IntArray();
        StringArray unitsPA        = new StringArray();

        Table outTable = new Table();
        outTable.addColumn( 0, "lineStation",    lineStationPA, new Attributes());
        outTable.addColumn( 1, "line",           linePA, new Attributes());
        outTable.addColumn( 2, "station",        stationPA, new Attributes());
        outTable.addColumn( 3, "longitude",      lonPA, new Attributes());
        outTable.addColumn( 4, "latitude",       latPA, new Attributes());
        outTable.addColumn( 5, "depth",          depthPA, new Attributes().add("long_name", "Depth at Start of Tow"));
        outTable.addColumn( 6, "time",           timePA, new Attributes().add("units", timeUnits));

        outTable.addColumn( 7, "cruiseNumber",   cruiseNumberPA, new Attributes());
        outTable.addColumn( 8, "shipName",       shipNamePA, new Attributes());
        outTable.addColumn( 9, "shipCode",       shipCodePA, new Attributes());
        outTable.addColumn(10, "occupy",         occupyPA, new Attributes().add("long_name", "Order of Occupancy"));

        outTable.addColumn(11, "obsCommon",      obsCommonPA, new Attributes());
        outTable.addColumn(12, "obsScientific",  obsScientificPA, new Attributes());
        outTable.addColumn(13, "obsValue",       valuePA, new Attributes());
        outTable.addColumn(14, "obsUnits",       unitsPA, new Attributes());

        outTable.globalAttributes().set("history",
            "Data originally from CalCOFI project.\n" +
            "At ERD, Roy Mendelssohn processed the data into .nc files.\n" +
            "2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons.\n" +
            today + " Bob Simons reorganized the files with Projects.calcofiBio().");

        String fileName[] = 
            RegexFilenameFilter.list(oldDir, ".*\\.nc");
            //new String[]{"calcofiBio_19840211_66.7_65_NH16.nc"};  //for testing
        int nFiles = 
            fileName.length; 
            //Math.min(5, fileName.length);  //for testing
        for (int filei = 0; filei < nFiles; filei++) {
            Table inTable = new Table();
            inTable.readNDNc(oldDir + fileName[filei],
                null, 0,  //standardizeWhat=0
                null, Double.NaN, Double.NaN);
            if (inTable.nRows() != 1)
                throw new RuntimeException(fileName[filei] + " has nRows=" + inTable.nRows());
            double time = -999f;
            float depth = -999f;
            float lat = -999f;
            float lon = -999f;
            float line = -999f;
            float station = -999f;
            int occupy = -9999;
            int nColumns = inTable.nColumns();
            for (int col = 0; col < nColumns; col++) {
                String colName = inTable.getColumnName(col);
                if (colName.equals("time")) {
                    String tUnits = inTable.columnAttributes(col).getString("units");
                    if (!timeUnits.equals(tUnits))
                        throw new RuntimeException(fileName[filei] + " has time units=" + tUnits);
                    time = inTable.getDoubleData(col, 0);

                } else if (colName.equals("depth")) {
                    depth = inTable.getFloatData(col, 0);

                } else if (colName.equals("lat")) {
                    lat = inTable.getFloatData(col, 0);

                } else if (colName.equals("lon")) {
                    lon = inTable.getFloatData(col, 0);
                
                } else if (colName.equals("Line")) {
                    line = inTable.getFloatData(col, 0);

                } else if (colName.equals("Station")) {
                    station = inTable.getFloatData(col, 0);

                } else if (colName.equals("Occupy")) {
                    occupy = inTable.getIntData(col, 0);

                } else {
                    //a data var
                    String long_name = inTable.columnAttributes(col).getString("long_name");
                    String units     = inTable.columnAttributes(col).getString("units");
                    String type      = inTable.getColumn(col).elementTypeString();
                    if (!type.equals("int"))
                        throw new RuntimeException(fileName[filei] + " long_name=" + long_name +
                            " has type=" + type);
                    int value        = inTable.getIntData(col, 0);
                    if (value < 0) {
                        String2.log(fileName[filei] + " long_name=" + long_name +
                            " has invalid value=" + value);
                        continue;
                    }

                    //Pacific_argentine_LarvaeCount
                    String common = colName;
                    if (common.endsWith("_LarvaeCount"))
                        common = String2.replaceAll(common.substring(0, common.length() - 12), '_', ' ');
                    if (common.equals("TotalFishEggs"))        common = "Total Fish Eggs";
                    if (common.equals("TotalFishLarvae"))      common = "Total Fish Larvae";
                    if (common.equals("TotalPlanktonVolume"))  common = "Total Plankton Volume";
                    if (common.equals("SmallPlanktonVolume"))  common = "Small Plankton Volume";
                    if (common.equals("Unidentified_EggCount"))common = "Unidentified Egg Count";

                    //long_name = "Argentina sialis Larvae Count" 
                    if (long_name.endsWith(" Larvae Count")) 
                        long_name = long_name.substring(0, long_name.length() - 13);

                    //all okay
                    obsCommonPA.add(common);
                    obsScientificPA.add(long_name);
                    valuePA.add(value);
                    unitsPA.add(units);
                }
            }

            // global attributes:
            int nToAdd = unitsPA.size() - cruiseNumberPA.size();
            int i = inTable.globalAttributes().getInt("CRUISE_NUM");
            cruiseNumberPA.addN(nToAdd, i);
            String s = inTable.globalAttributes().getString("SHIP_NAME");
            shipNamePA.addN(nToAdd, s == null? "" : s);
            s = inTable.globalAttributes().getString("SHIPCODE");
            shipCodePA.addN(nToAdd, s == null? "" : s);

            timePA.addN(nToAdd, time);
            depthPA.addN(nToAdd, depth);
            latPA.addN(nToAdd, lat);
            lonPA.addN(nToAdd, lon);
            linePA.addN(nToAdd, line);
            stationPA.addN(nToAdd, station);
            lineStationPA.addN(nToAdd, calcofiLineStation("" + line, "" + station));
            occupyPA.addN(nToAdd, occupy);
        }

        //nRows=77429
        //Memory: currently using     314 MB 
        int nRows = lineStationPA.size();
        String2.log("Done gathering data. nRows=" + nRows + "\n" +
            Math2.memoryString() + "\n" +
            "sorting...");

        //sort by obsCommon, lineStation, time
        outTable.sort(
            new int[]{
                outTable.findColumnNumber("obsCommon"),
                outTable.findColumnNumber("lineStation"), 
                outTable.findColumnNumber("time")}, 
            new boolean[]{true, true, true});

        //write to lineStation files
        int startRow = 0;
        int nStations = 0; 
        for (int row = 0; row < nRows; row++) {
            if (row == nRows - 1 ||
                obsCommonPA.compare(row, row + 1) != 0) {  //look ahead

                //save this station's data
                nStations++;
                Table tTable = outTable.subset(startRow, 1, row);
                tTable.saveAsFlatNc(newDir + 
                    String2.modifyToBeFileNameSafe(obsCommonPA.get(row)) + ".nc", 
                    "row", false); //convertToFakeMissingValues
                if (startRow == 0)
                    String2.log(tTable.toString());
                startRow = row + 1;
            }
        }
        String2.log("Done nOriginalFiles=" + nFiles + " (6407?)" +            
                " outTable.nRows=" + nRows + " (77363?)\n" + //~11 datums per oldFile
            "  " + Math2.memoryString() + "\n" +
            "  nNewFiles=" + nStations + " (322?)" +
                " elapsedTime=" + (System.currentTimeMillis() - elapsedTime) + 
                " (Java 1.6 20s(in cache) - 86s (not))");
    }

    /** This makes the formatted line_station for CalCOFI datasets. */
    public static String calcofiLineStation(String lineS, String statS) {
     
        //all are .0;   some negative e.g, -1, -2, -999
        if (lineS.endsWith(".0")) lineS = lineS.substring(0, lineS.length() - 2);
        if (statS.endsWith(".0")) statS = statS.substring(0, statS.length() - 2);
        if (lineS.charAt(0) == '-') lineS = "-" + String2.zeroPad(lineS.substring(1), 3);
        else                        lineS =       String2.zeroPad(lineS, 3);
        if (statS.charAt(0) == '-') statS = "-" + String2.zeroPad(statS.substring(1), 3);
        else                        statS =       String2.zeroPad(statS, 3);
        if (lineS.equals("-999")) lineS = "";
        if (statS.equals("-999")) statS = "";
        return lineS + "_" + statS;
    }


    /** Reorganize the calcofiSub data in f:/data/calcofiSub for erddap. 
     * 
     * 2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons 
     *   ftp://192.168.31.10/outgoing/ldewitt/Simons/calcofi/
     * 2010-12-30 Bob Simons reorganized the files with Projects.calcofiSub().
     */
    public static void calcofiSub() throws Exception {
        String2.log("\n*** Projects.calcofiSub");
        long elapsedTime = System.currentTimeMillis();
        String oldDir = "c:/data/calcofiSub/";
        String newDir = "c:/u00/data/points/erdCalcofiSub/";
        File2.deleteAllFiles(newDir);        

        //quick look at a file
        if (false) {
            Table table = new Table();
            table.readNDNc(oldDir + "subsurface_19490228_92_39.nc",
                null, 0,  //standardizeWhat=0
                null, Double.NaN, Double.NaN);
            String2.log(table.toString());
            System.exit(0);
        }

        String timeUnits= "seconds since 1948-1-1";

        //get the file names
        String fileName[] = 
            RegexFilenameFilter.list(oldDir, ".*\\.nc");
        int nFiles = 
            fileName.length; 
            //Math.min(5, fileName.length);  //for testing

        //use the first file as the seed for all others
        Table outTable = new Table();
        outTable.readNDNc(oldDir + fileName[0],
            null, 0,  //standardizeWhat=0
            null, Double.NaN, Double.NaN);
            String2.log(outTable.toString());
        String outColNames[] = outTable.getColumnNames();
        String today = Calendar2.getCurrentISODateStringLocal();
        outTable.globalAttributes().set("history",
            outTable.globalAttributes().getString("history") + "\n" +
            "At ERD, Roy Mendelssohn processed the data into .nc files.\n" +
            "2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons.\n" +
            today + " Bob Simons reorganized the files with Projects.calcofiSub().");


        //append all of the other files
        for (int filei = 1; filei < nFiles; filei++) {
            Table inTable = new Table();
            inTable.readNDNc(oldDir + fileName[filei],
                null, 0,  //standardizeWhat=0
                null, Double.NaN, Double.NaN);
            String inColNames[] = inTable.getColumnNames();
            Test.ensureEqual(outColNames, inColNames, "outColNames doesn't equal inColNames");            
           
            outTable.append(inTable);
        }

        int nRows = outTable.nRows();
        String2.log("Done gathering data. nRows=" + nRows + "\n" +
            Math2.memoryString() + "\n" +
            "sorting...");

        //sort by line, station, lon, lat, time, depth
        //There are 25696 Distinct combinations, but only 25211 distinct times.
        //  Include lon and lat in sort just to be doubly safe.
        int lineCol    = outTable.findColumnNumber("stationline");
        int stationCol = outTable.findColumnNumber("stationnum");
        outTable.sort(
            new int[]{
                lineCol,
                stationCol, 
                outTable.findColumnNumber("lon"), 
                outTable.findColumnNumber("lat"), 
                outTable.findColumnNumber("time"), 
                outTable.findColumnNumber("depth")}, 
            new boolean[]{true, true, true, true, true, true});
        PrimitiveArray linePA    = outTable.getColumn(lineCol);
        PrimitiveArray stationPA = outTable.getColumn(stationCol);

        //make lineStationPA
        StringArray lineStationPA = new StringArray(nRows, false);
        outTable.addColumn(0, "lineStation", lineStationPA, new Attributes());
        for (int row = 0; row < nRows; row++) 
            lineStationPA.add(calcofiLineStation(linePA.getString(row), stationPA.getString(row)));       
        //String2.log(outTable.toCSSVString());

        //write to lineStation files
        int startRow = 0;
        int nStations = 0; 
        for (int row = 0; row < nRows; row++) {
            if (row == nRows - 1 ||
                lineStationPA.compare(row, row + 1) != 0) {  //look ahead

                //save this station's data
                nStations++;
                Table tTable = outTable.subset(startRow, 1, row);
                tTable.saveAsFlatNc(newDir + 
                    String2.modifyToBeFileNameSafe(lineStationPA.get(row)) + ".nc", 
                    "row", false); //convertToFakeMissingValues
                if (startRow == 0)
                    String2.log(tTable.toString());
                startRow = row + 1;
            }
        }
        String2.log("Done nOriginalFiles=" + nFiles + " (was 26000)" +            
                " outTable.nRows=" + nRows + " (was 387499)\n" + //~16 datums per oldFile
            "  " + Math2.memoryString() + "\n" + //202 MB, varies
            "  nNewFiles=" + nStations + " (was 1927)" +
                " elapsedTime=" + (System.currentTimeMillis() - elapsedTime) + 
                " (Java 1.6 138 - 363s, varies)");

    }

    /** Reorganized the calcofiSur data in f:/data/calcofiSur for erddap. 
     * 
     * 2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons 
     *   ftp://192.168.31.10/outgoing/ldewitt/Simons/calcofi/
     * 2011-01-02 Bob Simons reorganized the files with Projects.calcofiSur().
     */
    public static void calcofiSur() throws Exception {
        String2.log("\n*** Projects.calcofiSur");
        long elapsedTime = System.currentTimeMillis();
        String oldDir = "c:/data/calcofiSur/";
        String newDir = "c:/u00/data/points/erdCalcofiSur/";
        File2.deleteAllFiles(newDir);        

        //quick look at a file
        if (true) {
            Table table = new Table();
            table.readNDNc(oldDir + "surface_19490228_92_39.nc",
                null, 0,  //standardizeWhat=0
                null, Double.NaN, Double.NaN);
            String2.log(table.toString());
            //System.exit(0);
        }

        String timeUnits= "seconds since 1948-1-1";

        //get the file names
        String fileName[] = 
            RegexFilenameFilter.list(oldDir, ".*\\.nc");
        int nFiles = 
            fileName.length; 
            //Math.min(5, fileName.length);  //for testing

        //use the first file as the seed for all others
        Table outTable = new Table();
        outTable.readNDNc(oldDir + fileName[0],
            null, 0,  //standardizeWhat=0
            null, Double.NaN, Double.NaN);
            String2.log(outTable.toString());
        String outColNames[] = outTable.getColumnNames();
        String today = Calendar2.getCurrentISODateStringLocal();
        outTable.globalAttributes().set("history",
            outTable.globalAttributes().getString("history") + "\n" +
            "At ERD, Roy Mendelssohn processed the data into .nc files.\n" +
            "2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons.\n" +
            today + " Bob Simons reorganized the files with Projects.calcofiSur().");


        //append all of the other files
        for (int filei = 1; filei < nFiles; filei++) {
            Table inTable = new Table();
            inTable.readNDNc(oldDir + fileName[filei],
                null, 0,  //standardizeWhat=0
                null, Double.NaN, Double.NaN);
            String inColNames[] = inTable.getColumnNames();
            Test.ensureEqual(outColNames, inColNames, "outColNames doesn't equal inColNames");            
           
            outTable.append(inTable);
        }

        int nRows = outTable.nRows();
        String2.log("Done gathering data. nRows=" + nRows + "\n" +
            Math2.memoryString() + "\n" +
            "sorting...");

        //sort by line, station, lon, lat, time, depth
        //There are 25696 Distinct combinations, but only 25211 distinct times.
        //  Include lon and lat in sort just to be doubly safe.
        int lineCol    = outTable.findColumnNumber("stationline");
        int stationCol = outTable.findColumnNumber("stationnum");
        outTable.sort(
            new int[]{
                lineCol,
                stationCol, 
                outTable.findColumnNumber("lon"), 
                outTable.findColumnNumber("lat"), 
                outTable.findColumnNumber("time"), 
                outTable.findColumnNumber("depth")}, 
            new boolean[]{true, true, true, true, true, true});
        PrimitiveArray linePA    = outTable.getColumn(lineCol);
        PrimitiveArray stationPA = outTable.getColumn(stationCol);

        //make lineStationPA
        StringArray lineStationPA = new StringArray(nRows, false);
        outTable.addColumn(0, "line_station", lineStationPA, new Attributes());
        for (int row = 0; row < nRows; row++) 
            lineStationPA.add(calcofiLineStation(linePA.getString(row), stationPA.getString(row)));       
        //String2.log(outTable.toCSSVString());

        //write to line_station files
        int startRow = 0;
        int nStations = 0; 
        for (int row = 0; row < nRows; row++) {
            if (row == nRows - 1 ||
                lineStationPA.compare(row, row + 1) != 0) {  //look ahead

                //save this station's data
                nStations++;
                Table tTable = outTable.subset(startRow, 1, row);
                tTable.saveAsFlatNc(newDir + 
                    String2.modifyToBeFileNameSafe(lineStationPA.get(row)) + ".nc", 
                    "row", false); //convertToFakeMissingValues
                if (startRow == 0)
                    String2.log(tTable.toString());
                startRow = row + 1;
            }
        }
        String2.log("Done nOriginalFiles=" + nFiles + " (was 26000)" +            
                " outTable.nRows=" + nRows + " (was 26000)\n" + //1 datum per oldFile
            "  " + Math2.memoryString() + "\n" + //133 MB, varies
            "  nNewFiles=" + nStations + " (was 1927)" +
                " elapsedTime=" + (System.currentTimeMillis() - elapsedTime) + 
                " (Java 1.6 143 - ?s, varies)");

    }

    /** This processes NODC PJJU data files from source to make consistent for ERDDAP.
     * 2011-04-24
     */
    public static void nodcPJJU(
            String inDir, String outDir) throws Throwable {
        String[] fileNames = RegexFilenameFilter.list(inDir, ".*\\.nc");
        int col;
        Attributes atts;
        PrimitiveArray pa;
        String units;
        int count;
        for (int f = 0; f < fileNames.length; f++) {
            //*** read file into Table
            String fileName = fileNames[f];
            String2.log("fileName=" + fileName);
            Table table = new Table();
            //1=unpack -- solves problem:
            // intp  first 4 files have add_offset NaN.  remainder have 273.15
            table.readFlatNc(inDir + fileName, null, 1); //standardizeWhat=1
            int nRows = table.nRows();

            //*** ext: some degree_Celsius, some Kelvin (convert to degree_C)
            col = table.findColumnNumber("ext");
            atts = table.columnAttributes(col);
            units = atts.getString("units");
            pa = table.getColumn(col);
            pa.switchFromTo("9.96921E36", ""); 
            if (!"degree_Celsius".equals(units)) {               
                if ("degrees_Celsius".equals(units)) {
                } else if ("Kelvin".equals(units)) {
                    pa = table.getColumn(col);
                    pa.scaleAddOffset(1, Math2.kelvinToC); 
                } else {
                    throw new Exception("Unexpected units for ext: " + units);
                }
                atts.set("units", "degree_C");
            }
            pa.switchFromTo("" + Math2.kelvinToC, ""); 
            //count n<-100 degrees
            pa = table.getColumn(col);
            count = 0;
            for (int i = 0; i < nRows; i++)
                if (pa.getFloat(i) < -100) count++;
            if (count > 0)
                throw new RuntimeException("count of ext values <-100 = " + count + "\n" +
                    atts.toString());

            //*** intp: some degree_Celsius, some Kelvin (convert to degree_C)
            col = table.findColumnNumber("intp");
            atts = table.columnAttributes(col);
            units = atts.getString("units");
            if (!"degree_Celsius".equals(units)) {               
                if ("degrees_Celsius".equals(units)) {
                } else if ("Kelvin".equals(units)) {
                    pa = table.getColumn(col);
                    pa.scaleAddOffset(1, Math2.kelvinToC); 

                } else {
                    throw new Exception("Unexpected units for intp: " + units);
                }
                atts.set("units", "degree_Celsius");
            }
            //count n>100 degrees
            pa = table.getColumn(col);
            count = 0;
            for (int i = 0; i < nRows; i++)
                if (pa.getFloat(i) >= 100) count++;
            if (count > 0)
                throw new RuntimeException("count of intp values >100 = " + count + "\n" +
                    atts.toString());

            //*** sal: 
            col = table.findColumnNumber("sal");
            atts = table.columnAttributes(col);
            units = atts.getString("units");
            Test.ensureEqual(units, "1e-3", "Unexpected units for sal");

            //*** sst: some degrees_Celsius, some Kelvin (convert to degree_C)
            col = table.findColumnNumber("sst");
            atts = table.columnAttributes(col);
            units = atts.getString("units");
            pa = table.getColumn(col);
            pa.switchFromTo("9.96921E36", ""); 
            if (!"degree_Celsius".equals(units)) {               
                if ("degrees_Celsius".equals(units)) {
                } else if ("Kelvin".equals(units)) {
                    pa = table.getColumn(col);
                    pa.scaleAddOffset(1, Math2.kelvinToC); 
                } else {
                    throw new Exception("Unexpected units for sst: " + units);
                }
                atts.set("units", "degree_C");
            }

            //*** time
            //read time variable
            col = table.findColumnNumber("time");
            atts = table.columnAttributes(col);
            double[] bf = Calendar2.getTimeBaseAndFactor(atts.getString("units"));          
            Test.ensureEqual(bf[1], 1, "Unexpected time factor."); //ensure seconds
            pa = table.getColumn(col);

            //convert time to DoubleArray
            pa = new DoubleArray(pa);
            table.setColumn(col, pa);
            double oldSec = Calendar2.unitsSinceToEpochSeconds(bf[0], 1, pa.getDouble(0));
            String2.log("  old time[0]=" + pa.getDouble(0) + " " + oldSec);

            //convert time to epochSeconds
            pa.scaleAddOffset(1, bf[0]); //add time offset; now epoch seconds
            double newSec = pa.getDouble(0);
            String2.log("  new time[0]=" + pa.getDouble(0) + " " + newSec);
            Test.ensureEqual(oldSec, newSec, "Incorrect conversion.");
            atts.set("units", Calendar2.SECONDS_SINCE_1970);

            //*** flag_a
            /*
            for (int flag = 97; flag <= 108; flag++) {
                col = table.findColumnNumber("flag_" + (char)flag);
                atts = table.columnAttributes(col);
                pa = table.getColumn(col);
                for (int i = 0; i < nRows; i++) {
                    if (pa.getInt(i) >1) 
                        throw new RuntimeException("flag_" + (char)flag + 
                            " value#" + i + " > 1: " + pa.getInt(i) + "\n" +
                            atts.toString());
                }
            }
            */

            //***save file
            table.saveAsFlatNc(outDir + fileName, "row");
        }
    }


    /** 
     * Convert CCHDO (WOCE) bottle .csv files to .nc files.
     * See WHP-Exchange Description (referred to as WED below) 
     * version 4/22/2008 from "Data in 'Exchange' format" 
     * from http://cchdo.ucsd.edu/format.html.
     * WED selected over .nc because 
     * <br>1) some files available in exchange format and not in .nc, 
     * <br>3) need to process files anyway, to combine date+time.
     * <br>2) .csv is closer to what ERDDAP wants.
     * Created 2011-05-01.
     * 2014-04-08 Changed PSU to 1e-3 (used in cf std names 25)
     */
//2019-07-26 needs to be converted to new readASCII
/*    public static void convertCchdoBottle() throws Exception {
        String inDir  = "c:/data/cchdo/botcsv/";
        String outDir = "c:/u00/data/points/cchdoBot/";
        String logFile = "c:/data/cchdo/convertCchdoBottle.log";
        Attributes colInfo = new Attributes();  //colName -> units|type

        String2.setupLog(true, false, logFile, false, 1000000000);
        String2.log("*** Projects.convertCchdoBottle " + 
            Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
              " inDir=" + inDir +
            "\noutDir=" + outDir +
            "\nlogFile=" + logFile + "\n" +
            String2.standardHelpAboutMessage());
        long eTime = System.currentTimeMillis();
        HashSet ipts68Columns = new HashSet();
        File2.deleteAllFiles(outDir);

        
        //for each .csv file
        String[] fileNames = RegexFilenameFilter.list(inDir, ".*\\.csv");
        String2.log("nFiles=" + fileNames.length);
        for (int f = 0; f < fileNames.length; f++) {
            try {
                String2.log("\n#" + f + " " + fileNames[f]);
                ArrayList<String> lines = String2.readLinesFromFile(inDir + fileNames[f], null, 2);

                //BOTTLE,20030711WHPSIODMB
                //#code : jjward hyd_to_exchange.pl 
                //#original files copied from HTML directory: 20030711
                //#original HYD file: ar24_ahy.txt   Fri Jul 11 12:18:51 2003
                //#original SUM file: ar24_asu.txt   Fri Jul 11 12:34:19 2003
                //EXPOCODE,SECT_ID,STNNBR,CASTNO,SAMPNO,BTLNBR,BTLNBR_FLAG_W,DATE,TIME,LATITUDE,LONGITUDE,DEPTH,CTDPRS,CTDTMP,CTDSAL,SALNTY,SALNTY_FLAG_W,CTDOXY,OXYGEN,OXYGEN_FLAG_W,SILCAT,SILCAT_FLAG_W,NITRAT,NITRAT_FLAG_W,NITRIT,NITRIT_FLAG_W,PHSPHT,PHSPHT_FLAG_W,ALKALI,ALKALI_FLAG_W,CTDRAW,THETA,TCO2,TCO2_FLAG_W
                //,,,,,,,,,,,,DBARS,IPTS-68,PSS-78,PSS-78,,UMOL/KG,UMOL/KG,,UMOL/KG,,UMOL/KG,,UMOL/KG,,UMOL/KG,,UMOL/KG,,,IPTS-68,UMOL/KG,
                //     316N147_2,  AR24,     1,  1,     24, WHF040,2,19961102,1949, 39.1362, -27.3817, 1194,      5.0,  20.7443,  36.1300,  36.1316,2,   -999.0,    225.9,2,     0.49,2,     0.08,2,     0.00,2,     0.05,2,   2362.6,2,  157.5, 20.7433,  2053.1,2

                //extract last chars of first line: division, institute, person (see WED Table 1)            
                //BOTTLE,20081117PRINUNIVRMK
                Table table = new Table();
                Attributes gatts = table.globalAttributes();
                String s = lines.get(0);
                gatts.add("processed", s.substring(6, 14));
                gatts.add("DivInsWho", s.substring(14)); //DivisionInstituteWho

                //skip # lines  (It's history, but would be lost when files aggregated.)
                int colNamesLine = 1;
                while (lines.get(colNamesLine).startsWith("#"))
                    colNamesLine++;
                //String2.log("  colNamesLine=" + colNamesLine);

                //remove END_DATA and other rows at end
                int endDataLine = lines.size() - 1; //index 0.. of endDataLine
                while (!lines.get(endDataLine).startsWith("END_DATA")) {
                    lines.remove(endDataLine);
                    endDataLine--;
                }
                String[] units = String2.split(lines.get(colNamesLine + 1), ',');

                //read ASCII info into a table
                table.readASCII(inDir + fileNames[f], 
                    lines,  //WARNING: individual lines in lines will be set to null 
                    colNamesLine, colNamesLine + 2, "", 
                    null, null, null, null, false); //false=simplify
                lines = null;
                int nRows = table.nRows();

                //clean up
                for (int col = 0; col < units.length; col++) {
                    //make column names safe for .nc
                    String colName = table.getColumnName(col).toLowerCase();
                    colName = String2.replaceAll(colName, '-', '_');
                    colName = String2.replaceAll(colName, '+', '_');
                    colName = String2.replaceAll(colName, '/', '_');
                    if        (colName.equals("ammoni")) {
                        String2.log("change colName=ammoni to ammonia");
                        colName = "ammonia";
                    } else if (colName.equals("ammoni_flag_w")) {
                        String2.log("change colName=ammoni_flag_w to ammonia_flag_w");
                        colName = "ammonia_flag_w";
                    } else if (colName.equals("bltnbr")) {
                        String2.log("change colName=bltnbr to btlnbr");
                        colName = "btlnbr";
                    } else if (colName.equals("bltnbr_flag_w")) {
                        String2.log("change colName=bltnbr_flag_w to btlnbr_flag_w");
                        colName = "btlnbr_flag_w";
                    } else if (colName.equals("cfc11")) {
                        String2.log("change colName=cfc11 to cfc_11");
                        colName = "cfc_11";
                    } else if (colName.equals("ctdfluoro")) {
                        String2.log("change colName=ctdfluoro to ctdfluor");
                        colName = "ctdfluor";
                    } else if (colName.equals("ctdfluoro_flag_w")) {
                        String2.log("change colName=ctdfluoro_flag_w to ctdfluor_flag_w");
                        colName = "ctdfluor_flag_w";
                    } else if (colName.equals("fco2tmp")) {
                        String2.log("change colName=fco2tmp to fco2_tmp");
                        colName = "fco2_tmp";
                    } else if (colName.equals("fco220c")) {
                        String2.log("change colName=fco220c to fco2");
                        colName = "fco2";
                    } else if (colName.equals("fco220c_flag_w")) {
                        String2.log("change colName=fco220c_flag_w to fco2_flag_w");
                        colName = "fco2_flag_w";
                    } else if (colName.equals("gala")) {
                        String2.log("change colName=gala to gal");
                        colName = "gal";
                    } else if (colName.equals("gala_flag_w")) {
                        String2.log("change colName=gala_flag_w to gal_flag_w");
                        colName = "gal_flag_w";
                    } else if (colName.equals("gluc")) {
                        String2.log("change colName=gluc to glu");
                        colName = "glu";
                    } else if (colName.equals("gluc_flag_w")) {
                        String2.log("change colName=gluc_flag_w to glu_flag_w");
                        colName = "glu_flag_w";
                    } else if (colName.equals("neone")) {
                        String2.log("change colName=neone to neoner");
                        colName = "neoner";
                    } else if (colName.equals("o18_o16")) {
                        String2.log("change colName=o18_o16 to o18o16");
                        colName = "o18o16";
                    } else if (colName.equals("o18_o16_flag_w")) {
                        String2.log("change colName=o18_o16_flag_w to o18o16_flag_w");
                        colName = "o18o16_flag_w";
                    } else if (colName.equals("ph_temp")) {
                        String2.log("change colName=ph_temp to ph_tmp");
                        colName = "ph_tmp";
                    } else if (colName.equals("phtemp")) {
                        String2.log("change colName=phtemp to ph_tmp");
                        colName = "ph_tmp";
                    } else if (colName.equals("poc_flag")) {
                        String2.log("change colName=poc_flag to poc_flag_w");
                        colName = "poc_flag_w";
                    } else if (colName.equals("reftemp")) {
                        String2.log("change colName=reftemp to reftmp");
                        colName = "reftmp";
                    } else if (colName.equals("reftemp_flag_w")) {
                        String2.log("change colName=reftemp_flag_w to reftmp_flag_w");
                        colName = "reftmp_flag_w";
                    } else if (colName.equals("sect_id")) {
                        String2.log("change colName=sect_id to sect");
                        colName = "sect";
                    }
                    table.setColumnName(col, colName);

                    //get units
                    String tUnits = units[col];
                    if (tUnits.equals("PSU"))
                        tUnits = "1e-3"; //used in CF std names 25
                    else 
                        tUnits = tUnits.toLowerCase();
                    //String2.log("orig colName=" + colName + " tUnits=" + tUnits);

                    if        (tUnits.equals("%") || 
                               tUnits.equals("percnt") ||
                               tUnits.equals("percent")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to % (g/100g?)");
                        tUnits = "g/100g";
                    } else if (tUnits.equals("dbars") || 
                               tUnits.equals("dbr") || 
                               tUnits.equals("dba")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to dbar");
                        tUnits = "dbar";
                    } else if (tUnits.equals("degc") || 
                               tUnits.equals("deg_c") || 
                               tUnits.equals("deg-c") || 
                               tUnits.equals("dec c") || 
                               tUnits.equals("deg c")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to degree_C");
                        tUnits = "degree_C";
                    } else if (colName.equals("theta") && 
                               (tUnits.equals("deg") ||
                                tUnits.equals("dec c"))) { //typo?
                        String2.log("change colName=" + colName + " units=" + tUnits + " to degree_C");
                        tUnits = "degree_C";
                    } else if (colName.equals("ph") &&
                        (tUnits.equals("sw@25") || 
                         tUnits.equals("sw") || 
                         tUnits.equals("@25_deg") || 
                         tUnits.equals("25_deg"))) {
                        //ph temp should be reported in PH_TMP column
                        String2.log("change colName=" + colName + " units=" + tUnits + " to \"\"");
                        tUnits = "";
                    } else if ((tUnits.equals("uatm@20") || 
                                tUnits.equals("uatm@4c")) && 
                               colName.equals("pco2")) {
                        //pco2 temp should be reported in PCO2_TMP column
                        String2.log("change colName=" + colName + " units=" + tUnits + " to uatm");
                        tUnits = "uatm";
                    } else if (tUnits.equals("mille") ||
                               tUnits.equals("/mille") ||
                               tUnits.equals("\\mille") ||
                               tUnits.equals("o/oo")) {
                        //http://www.es.flinders.edu.au/~mattom/IntroOc/lecture03.html says
                        //The symbol o/oo stands for "parts per thousand" or "per mil"; 
                        //a salt content of 3.5% is equivalent to 35 o/oo, 
                        //or 35 grams of salt per kilogram of sea water.
                        //https://en.wikipedia.org/wiki/Per_mil says
                        //A per mil or per mille (also spelled permil, permille,
                        //per mill or promille) (Latin, literally meaning 'for (every) thousand') 
                        //is a tenth of a percent or one part per thousand. 
                        //It is written with the sign .. which looks like a 
                        //percent sign (%) with an extra zero at the end.
                        String2.log("change colName=" + colName + " units=" + tUnits + " to o/oo (g/kg?)");
                        tUnits = "g/kg";
                    } else if (colName.equals("c14err") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to o/oo (g/kg?) ?!");
                        tUnits = "g/kg"; 
                    } else if (tUnits.equals("its-90") || 
                               tUnits.equals("its90")  ||
                               tUnits.equals("ipts-90")  ||
                               tUnits.equals("itst90")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to degree_C");
                        tUnits = "degree_C";
                        table.columnAttributes(col).add("description", "ITS-90");
                    } else if (tUnits.equals("ipts-68") || 
                               tUnits.equals("ipts68") || 
                               tUnits.equals("its-68") ||
                               tUnits.equals("r ipts-6")) { 
                        //http://www.code10.info/index.php?option=com_content&view=article&id=83:conversions-among-international-temperature-scales&catid=60:temperature&Itemid=83
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to degree_C and multipy by 0.99976");
                        tUnits = "degree_C";
                        table.getColumn(col).scaleAddOffset(0.99976, 1); 
                        table.columnAttributes(col).add("description", "Converted from IPTS-68 to ITS-90.");
                        ipts68Columns.add(String2.canonical(colName));
                    } else if (tUnits.equals("g umol/k") ||
                               tUnits.equals("umol/k") ||
                               tUnits.equals("8 umol/k")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to umol/kg");
                        tUnits="umol/kg";
                    }

                    //unwanted units
                    if (       (colName.equals("btlnbr") ||
                                colName.equals("btlnbr_flag_w") ||
                                colName.equals("castno") ||
                                colName.equals("expocode") ||
                                colName.equals("sampno") ||
                                colName.equals("sect") ||
                                colName.equals("stnnbr")) && 
                               !tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to \"\"");
                        tUnits = "";
                    }

                    //missing units
                    if (colName.equals("ctdraw") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to dbar ?!");
                        tUnits = "dbar";
                    } else if (colName.equals("ctdoxy") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to umol/kg ?!");
                        tUnits = "umol/kg";
                    } else if (colName.equals("ctdprs") && 
                               (tUnits.equals(""))) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to dbar ?!");
                        tUnits = "dbar";
                    } else if (colName.equals("ctdtmp") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to degree_C ?!");
                        tUnits = "degree_C";
                    } else if (colName.equals("delher") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to % (g/100g?) ?!");
                        tUnits = "g/100g";
                    } else if (colName.equals("depth") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to meters ?!");
                        tUnits = "meters";
                    } else if (colName.equals("helier") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to nmol/kg ?!");
                        tUnits = "nmol/kg";
                    } else if (colName.equals("neoner") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to nmol/kg ?!");
                        tUnits = "nmol/kg";
                    } else if (colName.equals("poc") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to ug/kg ?!");
                        tUnits = "ug/kg";
                    } else if (colName.equals("revtmp") && tUnits.equals("")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + "\"\" to degree_C ?!");
                        tUnits = "degree_C";
                    }

                    //clean up units
                    if (colName.equals("depth") && tUnits.equals("m")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to meters");
                        tUnits = "meters";
                    } else if (colName.equals("latitude") && 
                               (tUnits.equals("") || tUnits.equals("deg"))) {
                        tUnits = "degrees_north";
                    } else if (colName.equals("longitude") && 
                               (tUnits.equals("") || tUnits.equals("deg"))) {
                        tUnits = "degrees_east";
                    } else if (tUnits.equals("pss78") ||
                               tUnits.equals("0  pss-7") ||
                               tUnits.equals("8  pss-7") ||
                               tUnits.equals("iss78") || 
                               tUnits.equals("pss-78") ||
                               tUnits.equals("c  pss-7")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to 1e-3");
                        tUnits = "1e-3"; //PSU changed to 1e-3 in CF std names 25
                    } else if (colName.equals("cfc_12") &&
                               tUnits.equals("pm/kg")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to pmol/kg");
                        tUnits = "pmol/kg";
                    } else if (tUnits.equals("g pmol/k") ||
                               tUnits.equals("pmool/kg") ||
                               tUnits.equals("pmol?" + (char)8 + "/k")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to pmol/kg");
                        tUnits = "pmol/kg";
                    } else if (colName.equals("phaeo") &&
                               tUnits.equals("mg/m3")) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to ug/l");
                        tUnits = "ug/l"; //  1:1 conversion
                    } else if (colName.equals("fluor") &&
                               (tUnits.equals("ug/l") ||  //1:1 conversion
                                tUnits.equals("mg/m^3"))) {
                        String2.log("change colName=" + colName + " units=" + tUnits + " to ug/l");
                        tUnits = "mg/m3"; //  1:1 conversion
                    }

                    //scale
                    if (       (tUnits.equals("pmol/kg") ||
                                tUnits.equals("pm/kg")) &&
                               (colName.equals("cfc_11") ||
                                colName.equals("phspht"))) {
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to umol/kg and multiply by 1000000");
                        tUnits = "umol/kg";
                        PrimitiveArray pa = table.getColumn(col);
                        if (!(pa instanceof FloatArray))
                            table.setColumn(col, pa = new FloatArray(pa));
                        pa.scaleAddOffset(1000000, 1); 
                    } else if ((tUnits.equals("umol/kg") ||
                                tUnits.equals("um/kg")) && 
                               (colName.equals("barium") ||
                                colName.equals("helium") ||
                                colName.equals("helier"))) {
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to nmol/kg and multiply by 1000");
                        tUnits = "nmol/kg";
                        PrimitiveArray pa = table.getColumn(col);
                        if (!(pa instanceof FloatArray))
                            table.setColumn(col, pa = new FloatArray(pa));
                        pa.scaleAddOffset(1000, 1); 
                    } else if ((tUnits.equals("g/100g")) && 
                        (colName.equals("c14err"))) {
                        String2.log("change colName=" + colName + " units=%" + 
                            " to o/oo (g/kg?) and multiply by 10");
                        tUnits = "g/kg";
                        PrimitiveArray pa = table.getColumn(col);
                        if (!(pa instanceof FloatArray))
                            table.setColumn(col, pa = new FloatArray(pa));
                        pa.scaleAddOffset(10, 1); 
                    }                    

                    //set column to NaN
                    if ((tUnits.equals("pmol/l") ||
                                tUnits.equals("mg/m3")) && 
                               (colName.equals("ccl4") || 
                                colName.equals("cfc113") || 
                                colName.equals("cfc_12"))) {
                        //convert cfc in wrong units to NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to pmol/kg and set all to -999");
                        tUnits = "pmol/kg";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("e8/l")) && 
                               (colName.equals("bact"))) {
                        //convert bact in wrong units to NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to cells/ml and set all to -999");
                        tUnits = "cells/ml";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("pmol/l") ||
                                tUnits.equals("umol/l") ||
                                tUnits.equals("ml/l")) && 
                               (colName.equals("cfc_11") ||
                                colName.equals("ctdoxy") ||
                                colName.equals("no2_no3") ||
                                colName.equals("oxygen") ||
                                colName.equals("phspht") ||
                                colName.equals("silcat") ||
                                colName.equals("toc"))) {
                        //convert umol/l to NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to umol/kg and set all to -999");
                        tUnits = "umol/kg";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("umol/kg")) && 
                               colName.equals("tritum")) {
                        //convert tritum in umol/kg to NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to TU and set all to -999");
                        tUnits = "tu";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("ppm") || 
                                tUnits.equals("ppm@eq")) && 
                               colName.equals("pco2")) {
                        //convert pco2 in ppm to NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to uatm and set all to -999");
                        tUnits = "uatm";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("ug/l") ||
                                tUnits.equals("mg/m3") ||
                                tUnits.equals("mg/m**3")) && 
                               (colName.equals("chlora") ||
                                colName.equals("pphytn"))) {
                        //convert chlora in ug/l to NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to ug/kg and set all to -999");
                        tUnits = "ug/kg";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("")) && 
                               colName.equals("c13err")) {
                        //convert c13err to g/kg and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to o/oo (g/kg?) and set all to -999");
                        tUnits = "g/kg";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("")) && 
                               colName.equals("phaeo")) {
                        //convert phaeo to ug/l and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to ug/l and set all to -999");
                        tUnits = "ug/l";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("")) && 
                               colName.equals("revprs")) {
                        //convert revprs to dbar and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to dbar and set all to -999");
                        tUnits = "dbar";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("pctmod")) && 
                               colName.equals("delhe3")) {
                        //convert delhe3 to g/100g and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to % (g/100g?) and set all to -999");
                        tUnits = "g/100g";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("degree_C")) && 
                        colName.equals("ctdprs")) {
                        //convert ctdpers to dbar and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to dbar and set all to -999");
                        tUnits = "dbar";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if ((tUnits.equals("degree_C") ||
                         tUnits.equals("umol/kg")) && 
                        (colName.equals("ctdsal") ||
                         colName.equals("salnty"))) {
                        //convert ctdpers to dbar and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to 1e-3 and set all to -999");
                        tUnits = "1e-3"; //PSU changed to 1e-3 in CF std names 25
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } else if (tUnits.equals("1e-3") &&  //?  was PSU
                        colName.equals("ctdtmp")) {
                        //convert ctdtmp to degree_C and NaNs
                        String2.log("change colName=" + colName + " units=" + tUnits + 
                            " to degree_C and set all to -999");
                        tUnits = "degree_C";
                        table.setColumn(col, PrimitiveArray.factory(PAType.FLOAT, nRows, "-999"));
                    } 


                    table.columnAttributes(col).add("units", tUnits);

                    //diagnostic: print min and max
                    //if (colName.equals("btlnbr_flag_w")) {
                    //    double stats[] = table.getColumn(col).calculateStats();
                    //    String2.log("  btlnbr_flag_w min=" + stats[PrimitiveArray.STATS_MIN] + 
                    //        " max=" + stats[PrimitiveArray.STATS_MAX]);
                    //}

                    //make sure some cols are stored as String
                    if ("|expocode|ph_scale|sect|stnnbr|sampno|btlnbr|date|time|".indexOf(
                        "|" + colName + "|") >= 0) {
                        //leave as String
                    } else if (colName.equals("castno") || 
                               colName.equals("depth") || 
                               colName.indexOf("_flag_") > 0) {
                        //convert to short
                        table.setColumn(col, new ShortArray(table.getColumn(col)));
                    } else if (colName.equals("id")) {
                        //convert to int
                        table.setColumn(col, new IntArray(table.getColumn(col)));
                    } else if (("|14c_doc|alkali|bact|barium|c13err|c14err|ccl4|" +
                        "cfc_11|cfc_12|cfc113|chlora|" +
                        "ctdoxy|ctdprs|ctdraw|ctdsal|ctdtmp|" +
                        "delc13|delc14|delhe3|delsi30|density|doc|dtn|fuco|" +
                        "gala|gluc|helier|helium|man|nitrat|nitrit|no2_no3|o18o16|oxygen|" +
                        "pco2|pco2_tmp|ph|ph_tmp|phaeo|" +
                        "phspht|pphytn|ra_8_6|ra_8_6e|revprs|rham|" +
                        "salnty|silcat|tcarbn|tdn|toc|tritum|"
                        ).indexOf("|" + colName + "|") >= 0) {
                        //convert to float
                        table.setColumn(col, new FloatArray(table.getColumn(col)));
                    } else {
                        //simplify
                        table.simplify(col);
                        PrimitiveArray pa = table.getColumn(col);

                        //convert doubles to floats
                        if (pa instanceof DoubleArray)
                            table.setColumn(col, pa = new FloatArray(pa));
                    }

                    //add missing_value attribute to non-String columns
                    PrimitiveArray pa = table.getColumn(col);
                    if (!(pa instanceof StringArray))
                        table.columnAttributes(col).add("missing_value", 
                            PrimitiveArray.factory(pa.elementType(), 1, "-999"));

                }

                //combine date,time
                int dateCol = table.findColumnNumber("date");  //YYYYMMDD
                int timeCol = table.findColumnNumber("time");  //HHMM  (UT)
                Test.ensureTrue(dateCol >= 0, "date column not found.\n" + 
                    table.getColumnNamesCSSVString());
                PrimitiveArray datePA = table.getColumn(dateCol);
                StringArray dateTimePA = new StringArray(nRows, false);
                if (timeCol >= 0) {
                    //combine date,time
                    PrimitiveArray timePA = table.getColumn(timeCol);
                    for (int row = 0; row < nRows; row++) 
                        dateTimePA.add(datePA.getString(row) + String2.zeroPad(timePA.getString(row), 4));
                    table.setColumn(timeCol, dateTimePA);
                    table.columnAttributes(timeCol).set("units", "yyyyMMddHHmm");
                    table.removeColumn(dateCol); //do last
                } else {
                    //convert date to String "time"
                    for (int row = 0; row < nRows; row++) 
                        dateTimePA.add(datePA.getString(row) + "0000");
                    table.setColumn(dateCol, dateTimePA);
                    table.setColumnName(dateCol, "time");
                    table.columnAttributes(dateCol).set("units", "yyyyMMddHHmm");
                }

                //save all colInfo: Name -> String[] 0=units, 1=type
                for (int col = 0; col < table.nColumns(); col++) {
                    String tUnits = table.columnAttributes(col).getString("units");
                    String colName = table.getColumnName(col);
                    String newInfo = (tUnits == null? "" : tUnits) + 
                        "|" +
                        table.getColumn(col).elementTypeString();
                    //if (colName.equals("ph_sws_flag_w") && 
                    //    fileNames[f].equals("06AQ19960712_hy1.csv")) {
                    //    throw new Exception("  col=" + colName + " newInfo=" + newInfo + "\n" +
                    //        table.getColumn(col));
                    //}
                    PrimitiveArray oldInfoPA = colInfo.get(colName);
                    //check for different units or type!
                    if (oldInfoPA != null) {
                        String oldInfo = oldInfoPA.getString(0);
                        if (!newInfo.equals(oldInfo))
                            throw new RuntimeException("Incompatible: col=" + colName +
                                "\n  oldInfo=" + oldInfo + "  " + String2.annotatedString(oldInfo) +
                                "\n  newInfo=" + newInfo + "  " + String2.annotatedString(newInfo));
                    }
                    //all is well, set it
                    colInfo.set(colName, newInfo);
                }

                //write to .nc file
                table.saveAsFlatNc(outDir + 
                    fileNames[f].substring(0, fileNames[f].length() - 4) + ".nc", 
                    "row", false);            

            } catch (Throwable t) {
                String2.pressEnterToContinue(MustBe.throwableToString(t));                
            }
        }

        //write colInfo
        String2.log("Done.  time=" + (System.currentTimeMillis() - eTime)/1000 + "s");

        String2.log("* columns with PSU should say \"Most values determined by PSS-78.\"");

        String2.log("* columns that had IPTS-68 originally (so metadata should say\n" +
            "\"Some data converted from IPTS-68 to ITS-90 via ITS90 = IPTS68 * 0.99976\"):");
        Object oar[] = ipts68Columns.toArray();
        Arrays.sort(oar);
        String2.log(String2.toNewlineString(oar));

        String2.log("colInfo:");
        String2.log(colInfo.toString());
        String2.returnLoggingToSystemOut();
    } */

    /** 
     * This makes the NetCheck tests for all of an ERDDAP's datasets. 
     * 
     * @param erddapUrl the erddap URL, ending in a slash
     * @return a string with the tests. The results are also printed and
     *   put on the clipboard.
     */
    public static String makeNetcheckErddapTests(String erddapUrl) throws Throwable {
        Table table = new Table();
        erddapUrl += "tabledap/allDatasets.csv0?datasetID";
        ArrayList<String> rows = SSR.getUrlResponseArrayList(erddapUrl); //default is String2.ISO_8859_1
        int nRows = rows.size();
        rows.sort(String2.STRING_COMPARATOR_IGNORE_CASE);
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < nRows; row++) 
            sb.append("    <responseMustInclude>" + rows.get(row) + "</responseMustInclude>\n");
        String s = sb.toString();
        sb = null;
        String2.setClipboardString(s);
        String2.log(s);
        String2.log("nDatasets=" + nRows);
        String2.log("The results are on the clipboard.");
        return s;
    }


    /* *
     * Export all tables in .mdb file to .tsv
     * http://sourceforge.net/projects/jackcess/
     * http://jackcess.sourceforge.net/apidocs/index.html
     *
     * @param mdbFileName
     * @param destinationDir
     */
    /*public static void exportMdbToTsv(String mdbFileName, String destDir) throws Exception {
        String2.log("Project.exportMdbToTsv " + mdbFileName + " to " + destDir);
        com.healthmarketscience.jackcess.ExportUtil.exportAll(
            com.healthmarketscience.jackcess.Database.open(new java.io.File(mdbFileName), true), //read only
            new java.io.File(destDir), "tsv", 
            true,   //boolean header, 
            "\t",   //String delim, 
            '\"',   //char quote, 
            new com.healthmarketscience.jackcess.SimpleExportFilter());  //ExportFilter filter) 
    }*/
 
    /**
     * 2011-09-21 - recently I've made keywords newline separated. 
     * It should be CSV (but ignore newlines).
     * This fixes the datasets.xml files.
     */
    public static void fixKeywords(String fileName) throws Exception {
        String2.log("fixKeywords " + fileName);
        String charset = String2.ISO_8859_1;
        String attKeywords = "<att name=\"keywords\">";
        int attKeywordsLength = attKeywords.length();
        StringArray lines = StringArray.fromFile(fileName, charset);
        int linesSize = lines.size();
        for (int i = 0; i < linesSize; i++) {
            //start keywords?
            String s = lines.get(i);
            int kpo = s.indexOf(attKeywords);
            if (kpo >= 0) {
                int ekpo = s.lastIndexOf("</att>");
                if (ekpo > kpo)
                    continue;

                //first line may not need comma
                if (s.trim().endsWith(attKeywords))
                    i++;

                //add comma to subsequent lines
                while (lines.get(i).indexOf("</att>") < 0) {
                    if (!lines.get(i).trim().endsWith(",") &&
                        !lines.get(i+1).trim().startsWith("</att>")) {
                        lines.set(i, lines.get(i) + ",");
                    }
                    i++;
                }
            }
        }
        lines.toFile(fileName);
    }

    /**
     * Get data from a common type of Opendap grid request and save in .nc file.
     *
     * @param dapUrl       the base DAP URL
     * @param vars         the var names (which much share the same dimensions)
     *   If a var isn't present, it is skipped.
     * @param projection   e.g., [17][0][0:179][0:359]  (or null or "" for all) 
     * @param fullFileName the complete name
     * @param jplMode use for requests for jplG1SST data (to enable lat chunking)
     *   when the projection is [0][0:15999][0:35999]
     */
/* See OpendapHelper.dapToNc

  public static void dapToNc(String dapUrl, String vars[], String projection, 
        String fullFileName, boolean jplMode) throws Throwable {

        //constants for jpl
        int jplLonSize = 36000;
        int jplLatSize = 16000;
        int jplLatChunk = 2000;
        int jplNChunks = jplLatSize / jplLatChunk;
        int jplLatDim = 1;  //[time][lat][lon]
        FloatArray jplLatPa = new FloatArray(jplLatSize, true);
        for (int i = 0; i < jplLatSize; i++)
            jplLatPa.setDouble(i, -79.995 + i * 0.01);
        int jplChunkShape[] = {1, jplLatChunk, jplLonSize};

        String beginError = "Projects.dapToNc url=" + dapUrl + 
            "\n  vars=" + String2.toCSSVString(vars) + "  projection=" + projection;
        String2.log(beginError + 
               " jplMode=" + jplMode +
            "\n  fileName=" + fullFileName); 
        beginError = String2.ERROR + " in " + beginError; 
        long time = System.currentTimeMillis();

        //delete any existing file
        File2.delete(fullFileName);

        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //get dConnect.  If this fails, no clean up needed.
        DConnect dConnect = new DConnect(dapUrl, true, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        //figure out the projection if projection is null or ""
        if (projection == null || projection.length() == 0) {
            StringBuilder sb = new StringBuilder();
            BUILD_PROJECTION:
            for (int v = 0; v < vars.length; v++) {
                String tName = vars[v];
                BaseType baseType;
                try {
                    baseType = dds.getVariable(tName);
                } catch (Throwable t) {
                    if (verbose) String2.log("  for var=" + tName + ": " + t.toString());
                    continue;
                }
                if (baseType instanceof DGrid) {
                    //dGrid has main dArray + dimensions
                    DGrid dGrid = (DGrid)baseType;
                    int nEl = dGrid.elementCount(true);
                    for (int el = 1; el < nEl; el++) { //1..
                        BaseType bt2 = dGrid.getVar(el);
                        if (bt2 instanceof DVector) 
                            sb.append("[0:" + (((DVector)bt2).getLength() - 1) + "]");
                        else throw new RuntimeException(beginError + 
                            "var=" + tName + " element#" + el +
                            " has unexpected baseType=" + bt2.getClass().getName());
                    }
                    break;
                } else if (baseType instanceof DArray) {
                    //dArray is usually 1 dim, but may be multidimensional
                    DArray dArray = (DArray)baseType;
                    int nDim = dArray.numDimensions();
                    if (nDim == 0) 
                        throw new RuntimeException(beginError + 
                            "var=" + tName + " is a DArray with 0 dimensions.");
                    for (int d = 0; d < nDim; d++) {//0..
                        sb.append("[0:" + (dArray.getDimension(d).getSize() - 1) + "]");
                    }
                    break;
                } else {
                    throw new RuntimeException(beginError + 
                        "var=" + tName + " has unexpected baseType=" + 
                        baseType.getClass().getName());
                }
            }

            if (sb.length() == 0) 
                throw new RuntimeException(beginError + 
                    "File not created! None of the requested vars were found.");
        }

        //read the first var to get the dimension info  (pas[] has main + dimensions)
        String tProjection = projection;
        if (jplMode) tProjection = String2.replaceAll(tProjection, 
            ":" + (jplLatSize - 1) + "]", ":" + (jplLatChunk - 1) + "]");
        PrimitiveArray[] pas = OpendapHelper.getPrimitiveArrays(
            dConnect, "?" + vars[0] + tProjection); 
        int nDims = pas.length - 1;
        Dimension dims[] = new Dimension[nDims];
        int shape[] = new int[nDims];

        //*Then* make ncOut.    If this fails, no clean up needed.
        NetcdfFileWriter ncOut = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, fullFileName + randomInt);
        boolean nc3Mode = true;
        try {
            Group rootGroup = ncOut.addGroup(null, "");
            ncOut.setFill(false);

            //define the data variables in ncOut
            int nVars = vars.length;
            Variable newDimVars[] = new Variable[nDims];
            Variable newVars[] = new Variable[nVars];
            for (int v = 0; v < nVars; v++) {
                //String2.log("  create var=" + vars[v]);
                BaseType baseType = dds.getVariable(vars[v]);
                if (baseType instanceof DGrid) {
                    //dGrid has main dArray + dimensions
                    DGrid dGrid = (DGrid)baseType;

                    if (v == 0) {
                        //make the dimensions
                        for (int d = 0; d < nDims; d++) {
                            String tName = dGrid.getVar(d + 1).getName();
                            int tSize = pas[d + 1].size();
                            if (jplMode && d == jplLatDim)
                                tSize = jplLatSize;
                            shape[d] = tSize;
                            //String2.log("    dim#" + d + "=" + tName + " size=" + tSize);
                            dims[d] = ncOut.addDimension(rootGroup, tName, tSize, true, false, false);
                            newDimVars[d] = ncOut.addVariable(rootGroup, tName, 
                                NcHelper.getNc3DataType(pas[d + 1].elementType()), 
                                Arrays.asList(dims[d])); 
                        }
                    }

                    PrimitiveVector pv = ((DArray)dGrid.getVar(0)).getPrimitiveVector(); 
                    PAType tType = OpendapHelper.getElementPAType(pv);
                    //String2.log("pv=" + pv.toString() + " tType=" + tType);
                    newVars[v] = ncOut.addVariable(rootGroup, vars[v], 
                        NcHelper.getNc3DataType(tType), dims);

                } else {
                   throw new RuntimeException(beginError + 
                       "var=" + vars[v] + " baseType=" + baseType.getClass().getName() +
                       " isn't a DGrid.");
                }
            }

            //write global attributes in ncOut
            Attributes tAtts = new Attributes();
            OpendapHelper.getAttributes(das, "GLOBAL", tAtts);
            NcHelper.setAttributes(nc3Mode, rootGroup, tAtts);

            //write dimension attributes in ncOut
            for (int dim = 0; dim < nDims; dim++) {
                String dimName = dims[dim].getName();               
                tAtts.clear();
                OpendapHelper.getAttributes(das, dimName, tAtts);
                NcHelper.setAttributes(nc3Mode, newDimVars[v], tAtts);
            }

            //write data attributes in ncOut
            for (int v = 0; v < nVars; v++) {
                tAtts.clear();
                OpendapHelper.getAttributes(das, vars[v], tAtts);
                NcHelper.setAttributes(nc3Mode, newVars[v], tAtts);
            }

            //leave "define" mode in ncOut
            ncOut.create();


            //read/write the data variables
            for (int v = 0; v < nVars; v++) {
                long vTime = System.currentTimeMillis();

                //write the dimensions
                if (v == 0) {
                    //pas already has the data
                    for (int d = 0; d < nDims; d++) {
                        PrimitiveArray tpa = jplMode && d == jplLatDim? 
                            jplLatPa : pas[d + 1];
                        ncOut.write(newDimVars[d], Array.factory(tpa.toObjectArray()));
                    }
                }

                if (jplMode) {
                    //chunk 0 was read above
                    int origin[] = {0, 0, 0};
                    pas[0].trimToSize(); //so underlying array is exact size
                    ncOut.write(newVars[v], origin,
                        Array.factory(NcHelper.getNc3DataType(pas[0].elementType()),
                            jplChunkShape, pas[0].toObjectArray()));

                    //read other chunks
                    for (int chunk = 1; chunk < jplNChunks; chunk++) {
                        int base = chunk * jplLatChunk;
                        origin[1] = base;
                        tProjection = String2.replaceAll(projection, 
                            "[0:" + (jplLatSize - 1) + "]", 
                            "[" + base + ":" + (base + jplLatChunk - 1) + "]");
                        pas = OpendapHelper.getPrimitiveArrays(dConnect, "?" + vars[v] + tProjection); 
                        pas[0].trimToSize(); //so underlying array is exact size
                        //String2.log("pas[0]=" + pas[0].toString());
                        ncOut.write(newVars[v], origin,
                            Array.factory(NcHelper.getNc3DataType(pas[0].elementType()), 
                                jplChunkShape, pas[0].toObjectArray()));
                    }
                } else {
                    if (v > 0)  //read it
                        pas = OpendapHelper.getPrimitiveArrays(dConnect, "?" + vars[v] + projection); 
                    pas[0].trimToSize(); //so underlying array is exact size
                    //String2.log("pas[0]=" + pas[0].toString());
                    ncOut.write(newVars[v], 
                        Array.factory(NcHelper.getNc3DataType(pas[0].elementType()), 
                            shape, pas[0].toObjectArray()));
                }

                if (verbose) String2.log("  v#" + v + "=" + vars[v] + " finished. time=" + 
                    Calendar2.elapsedTimeString(System.currentTimeMillis() - vTime));
            }

            //if close throws Throwable, it is trouble
            ncOut.close(); //it calls flush() and doesn't like flush called separately
            ncOut = null;

            //rename the file to the specified name
            File2.rename(fullFileName + randomInt, fullFileName);

            //diagnostic
            if (verbose) String2.log("  Projects.dapToNc finished.  TIME=" + 
                Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + "\n");
            //String2.log(NcHelper.ncdump(fullFileName, "-h"));

        } catch (Throwable t) {
            //try to close the file
            try {  if (ncOut != null) ncOut.abort(); 
            } catch (Throwable t2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullFileName + randomInt);

            throw t;
        }
    }
*/

    /**
     * 2012-07-20 Convert .csv globec files to .nc.  
     * Files are from lynn from oceanwatch database
     * /Volumes/PFEL_Shared_Space/PFEL_Share/Lynn2Bob/oceanwatch_databases/PRD_ctd.csv 
     * stored locally as e.g., local:F:\data\globec\PRD_ctd.csv )
     */
    public static void convertGlobecCsvToNc() throws Exception {
        String2.log("\n*** convertGlobecCsvToNc");
        String inDir = "c:/data/globec/";
        String outDir = "c:/u00/data/points/globec/";
        String fileNames[] = {
            //"Globec_birds",     
            //"Globec_bottle_data_2002" /*, 
            //"Globec_cetaceans", 
            //"Globec_moc1", 
            //"Globec_vpt",       
            //"PRD_ctd",
            "rockfish0",
            "rockfish1",
            "rockfish2",
            "rockfish3",
            "rockfish4",
            "rockfish5",
            "rockfish6",
            "rockfish7",
            "rockfish8",
            "rockfish9"  
            /* */};
        for (int i = 0; i < fileNames.length; i++) {
            Table table = new Table();
            table.readASCII(inDir + fileNames[i] + ".csv");
            boolean isGlobec = fileNames[i].equals("Globec_bottle_data_2002");
            //String2.log("isGlobec=" + isGlobec + "\ncolNames=" + table.getColumnNamesCSSVString());
            for (int col = 0; col < table.nColumns(); col++) { 
                String colName = table.getColumnName(col);
                boolean isCastNo = colName.equals("cast_no");
                //String2.log("colName=" + colName + " " + isCastNo);

                if (colName.toLowerCase().indexOf("date") < 0) {
                    PrimitiveArray pa = table.getColumn(col);
                    if (colName.equals("cruise")) {
                        //ensure cruise is saved as String, not short
                        table.setColumn(col, new StringArray(pa));

                    } else if (colName.equals("ctd_index")) {
                        //ensure ctd_index is saved as short, not byte
                        table.setColumn(col, new ShortArray(pa));

                    } else if (pa instanceof DoubleArray) {
                        //save science data columns (not date values) as floats, not doubles
                        table.setColumn(col, new FloatArray(pa));

                    } else if (isGlobec && isCastNo) {
                        //globecBottle cast_no has valid data values = 127 
                        String2.log("converting cast_no to ShortArray");
                        pa = new ShortArray(pa);
                        pa.switchFromTo("NaN", "127");
                        double[] stats = pa.calculateStats();
                        String2.log("cast_no stats=" + String2.toCSSVString(stats));
                        table.setColumn(col, pa);
                    }
                }
            }
                    
            String2.log(table.getNCHeader("row"));
            table.saveAsFlatNc(outDir + fileNames[i] + ".nc", "row");
        }
    }

    /** This splits rockfish_view into 9 chunks of more manageable size. */
    public static void splitRockfish() throws Exception {
        String2.log("\n*** splitRockfish");
        String fileDir = "c:/data/globec/";
        StringArray sa = StringArray.fromFile(fileDir + "rockfish_view.csv");
        int size = sa.size();

        //this file has \N for missing values.  change to ""
        for (int i = 1; i < size; i++)
            sa.set(i, String2.replaceAll(sa.get(i), "\\N", ""));

        String colNames = sa.get(0);
        int chunk = 0;
        StringArray subset = new StringArray();
        while (size > 1) { 
            int stop = Math.min(100000, size - 1);
            sa.subset(subset, 0, 1, stop); //stop is inclusive
            subset.toFile(fileDir + "rockfish" + (chunk++) + ".csv");
            sa.removeRange(1, stop + 1); //end is exclusive
            size = sa.size();
        }
    }

    /** 
     * This converts f:/data/calcofi2012/*.txt files to *.nc files with some metadata.
     *
     * <p>2012-08-15 The calcofi8102012.mdb file is from ???.
     * It is actually a .accdb file (Access 2007 or 2010).  
     * I can't read it in my Access 2003.
     * Roy read it via Access 2010 on his computer.
     * He used File, Export, delimited, tab to make .txt file for each table.  
     */
    public static void processCalcofi2012() throws Exception {
        String dir = "c:/data/calcofi2012/";
        String tableName;
        Table table = new Table();

        //I'm converting true to their generous typing:
        //  int->int, smallint->short,
        //  real->float, decimal->double, numeric->double
/*
table = processOneCalcofi2012(
dir, tableName = "CC_?", new String[]{
//colNames             types  units  long_name
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", "",
"",         "String", "", ""});
*/
  

//***** Tables that are Look Up Tables 
/*

Table speciesCodesLUT = processOneCalcofi2012(
dir, tableName = "CC_SPECIES_CODES", new String[]{
//colNames             types  units  long_name
"speciesCode",        "int",    "", "CalCOFI Species Code",
"ITISTsn",            "int",    "", "Taxonomic Serial # from Integrated Taxonomic Information System",
"CCHSpeciesCode",     "String", "", "CalCOFI Hierarchical Species Code",
"scientificName",     "String", "", "Scientific Name",
"scientificNameItalic","String","", "Scientific Name Ready to Italicize",
"order",              "double", "", "Numbered Phylogenetic Order",
"CFGCode",            "double", "", "California Fish & Game Internal Code",
"RaceBaseCode",       "double", "", "RaceBase (AFSC) Code",
"NODCCode",           "double", "", "NODC Taxonomic Code",
"CFGSeaSurveyCode",   "double", "", "California Fish & Game Sea Survey Code",
"commonName",         "String", "", "Common Name"},
"");
speciesCodesLUT.reorderColumns(StringArray.fromCSV(
    "speciesCode, scientificName, commonName"), 
    true);  //discard others


table = processOneCalcofi2012(
dir, tableName = "CC_TOWTYPES", new String[]{
//colNames             types  units  long_name
"towTypeCode",        "String", "", "Short Tow Type Code",
"longTowType",        "String", "", "Long Tow Type",
"traditional",        "String", "", "Traditional Description",
"longDescription",    "String", "", "Long Description"},
"");

String towTypesDescription =   //hand picked and alphabetized; added to towTypeCode vars below
"BT=BOTTOM TRAWL\n" +
"C1=CalCOFI One Meter Oblique Tow\n" +
"C2=CalCOFI Two Meter Oblique Tow\n" +
"CB=CalCOFI Oblique Bongo Tow\n" +
"CV=CalCOFI Vertical Egg Tow\n" +
"DC=DEEP BONGO\n" +
"HR=HEAD ROPE\n" +
"M1=MOCNESS 1M (10)\n" +
"M2=MOCNESS 10M (6)\n" +
"MB=MARBOBL\n" +  //?
"MT=Twin Winged Continuous-Flow Surface Tow\n" +
"NE=NEUSTON\n" +
"PV=Paired Tows of CalCOFI Vertical Egg Net";

Table shipCodesLUT = processOneCalcofi2012(
dir, tableName = "CC_SHIPCODES", new String[]{
//colNames             types  units  long_name
"shipCode",           "String", "", "Ship Code",
"shipName",           "String", "", "Ship Name",
"NODCShipCode",       "String", "", "NODC Ship Code"},
towTypesDescription);
shipCodesLUT.reorderColumns(StringArray.fromCSV(
    "shipCode, shipName"), 
    true);  //discard others
*/
Table stationsLUT = processOneCalcofi2012(
dir, tableName = "CC_STATIONS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"line",               "double", "", "Line",
"station",            "double", "", "Station",
"arriveDate",         "String", "yyMMdd", "Arrive Date",
"arriveTime",         "int",    "HHmm",   "Arrive Time",   //should have been 0-padded string!
"departDate",         "String", "yyMMdd", "Depart Date",
"departTime",         "int",    "HHmm",   "Depart Time",   //should have been 0-padded string!
"latitude",           "int",    "degrees_north", "Latitude",
"latitudeMinutes",    "double", "", "MM.M Latitude Minutes",  //MM.M
"latitudeLocation",   "String", "", "Latitude Location",  //???!!! .pdf incorrectly has "W or E"
"longitude",          "int",    "degrees_east", "Longitude",
"longitudeMinutes",   "double", "", "MM.M Longitude Minutes",
"longitudeLocation",  "String", "", "Longitude Location",  //???!!! .pdf incorrectly has "N or S"
"bottomDepth",        "String", "", "Bottom Depth (meters/fathoms)",  //???
"region",             "int",    "", "Region (1 - 23)",
"navigator",          "String", "", "Navigator's Initials",
"remarks",            "String", "", "Remarks",
"hydrocastDate",      "String", "yyMMdd", "Hydrocast Date",
"timeZone",           "int",    "", "Time Zone (0 - 10)",  //e.g., 8 (is it ever not 7 or 8?)
"observer",           "String", "", "Station Observer's Initials"},
"");


/*

//****** The other tables (usually joined with the Look Up Tables from above)
table = processOneCalcofi2012(
dir, tableName = "CC_ACTIVITIES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"activity",           "String", "", "Activity",
"activityStatusCode", "String", "", "Activity Status Code"},
towTypesDescription);
  
table = processOneCalcofi2012(
dir, tableName = "CC_ANGLES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"wireOut",            "int",    "m","Wire Out",
"angle",              "short",  "degrees", "Angle"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_CRUISES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_CRUISETYPES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"towTypeCode",        "String", "", "Short Tow Type Code",
"beginDate",          "String", "yyMMdd", "Begin Date",   //? no documentation for if yyMMdd
"endDate",            "String", "yyMMdd", "End Date",     //? no documentation for if yyMMdd
"cruiseID",           "String", "", "Cruise Identifier",
"remarks",            "String", "", "Remarks"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_CRUISETYPE_CODES", new String[]{
//colNames             types  units  long_name
"numberingCode",      "int",    "", "Sequential Numbering Code",
"typeCode",           "String", "", "Cruise Type Code",
"traditional",        "String", "", "Traditional Description"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_EGG_COUNTS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"eggCount",           "double", "", "Egg Count (Raw)",
"strata",             "float",  "", "Strata",
"weightFactor",       "double", "", "Weight Factor"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_EGG_STAGES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"eggStage",           "double", "", "Egg Stage",
"eggDay",             "String", "", "Egg Day",
"eggStageCount",      "double", "", "Egg Stage Count (Raw)"},
towTypesDescription);


table = processOneCalcofi2012(
dir, tableName = "CC_FISH_COUNTS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"fishCount",          "double", "", "Fish Count (Raw)"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_FISH_SIZES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"fishSize",           "double", "mm","Fish Size",
"fishTally",          "double", "",  "Fish Tally"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_FLOWMETERS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"flowMeterNumber",    "String", "", "Flow Meter Number",
"calibDateBefore",    "String", "yyMMdd", "Calibration Before Date",
"calibDateAfter",     "String", "yyMMdd", "Calibration After Date",
"slope",              "double", "", "Slope",
"intercept",          "double", "", "Intercept",
"lineCode",           "String", "", "Line Code ('C'ommon/'S'ingle)"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_HYDROS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"castType",           "String", "", "Cast Type ('B'ucket, 'C'TD, 'H'ydrocast, 10'M'eterBottle)",
"standardDepth",      "double", "m",                    "Standard Depth",
"temperature",        "double", "degree_C",             "Temperature",
"salinity",           "double", "parts per thousand",   "Salinity",
"waterDensity",       "double", "1000 * (g/liter - 1)", "Water Density",
"oxygen",             "double", "ml/liter",             "Oxygen Content",
"dynHeightAnom",      "double", "dynamic meters",       "Dynamic Height Anomaly",
"oxygenSaturation",   "double", "percent",              "Oxygen Saturation"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_LARVAE_COUNTS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"larvaeCount",        "double", "", "Larvae Count (Raw)",
"measureDate",        "String", "yyMMdd", "Measure Date",
"initials",           "String", "", "Measurer's Initials",
"disintegrated",      "double", "", "Number of Disintegrated Larvae in Sample"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_LARVAE_SIZES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"larvaeSize",         "double", "mm", "Larvae Size",
"larvaeTally",        "double", "",   "Larvae Tally"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_LARVAE_STAGES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"speciesCode",        "int",    "", "CalCOFI Species Code",
"larvaeStage",        "String", "", "Larvae Stage",
"larvaeTally",        "double", "", "Larvae Tally"},
towTypesDescription);

//2012-08-15 Roy(?) reversed the names of CC_MOCNESSES and CC_NETS. Bob unreversed them.
table = processOneCalcofi2012(
dir, tableName = "CC_MOCNESSES", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"oxygenMean",         "double", "?",         "Oxygen Mean",
"temperatureMean",    "double", "degree_C",  "Temperature Mean",
"salinityMean",       "double", "?",         "Salinity Mean",
"temperatureMin",     "double", "degree_C",  "Temperature Minimum",
"temperatureMax",     "double", "degree_C",  "Temperature Maximum",
"salinityMin",        "double", "?",         "Salinity Minimum",
"salinityMax",        "double", "?",         "Salinity Maximum",
"oxygenMin",          "double", "?",         "Oxygen Minimum",
"oxygenMax",          "double", "?",         "Oxygen Maximum"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_NETS", new String[]{
//colNames             types  units  long_name
"netNumber",          "String", "", "Net Number",
"mesh",               "double", "microns", "Mesh"},
towTypesDescription);

table = processOneCalcofi2012(
dir, tableName = "CC_TOWS", new String[]{
//colNames             types  units  long_name
"cruiseYYYYMM",       "String", "yyMM", "CruiseYYYYMM (predominant month)",
"shipCode",           "String", "", "Ship Code",
"order",              "int",    "", "Order Each Station Was Occupied",
"towTypeCode",        "String", "", "Short Tow Type Code",
"towNumber",          "short",  "", "Sequential Tow Number",
"PortStarboard",      "String", "", "'P'ort or 'S'tarboard Net Location",
"towDate",            "String", "yyMMdd", "Begin Tow Date",  //was towDate
"towTime",            "String", "HHmm",   "Time Begin Tow",  //was timeBeginTow
"stdHaulFactor",      "double", "", "Standard Haul Factor",
"volumeStrained",     "double", "m^3", "Volume of Water Strained",
"fractionSorted",     "double", "fraction", "Fraction Sorted (0 - 1)",  //they said percent, .5=50%
"flowMeterNumber",    "String", "", "Flow Meter Number",
"netNumber",          "String", "", "Net Number",
"beginDepth",         "double", "m","Begin Depth of Tow",
"collector",          "String", "", "Collector's Initials",
"dateEndSort",        "String", "yyMMdd", "Date End Sort",
"dateStartSort",      "String", "yyMMdd", "Date Start Sort",
"errorCode",          "double", "", "Error Code (null, 0, 5, 6, 9, 10, or 19)",
"identifier",         "String", "", "Identifier's Initials",
"observer",           "String", "", "Observer's Initials",
"preservative",       "double", "", "Preservative (null, 0 - 4)",
"totalEggs",          "double", "", "Total Eggs (Raw Count)",
"sortedPlanktonVolume", "double", "ml/1000 cubic meters of water strained", "Sorted Plankton Volume",
"sampleQuality",      "double", "", "Sample Quality (0 - 5)",
"smallPlanktonVolume","double", "ml/1000 cubic meters of water strained", "Small Plankton Volume",
"sorter",             "String", "", "Sorter's Initials",
"volumner",           "String", "", "Volumer's Initials",
"wireOut",            "double", "", "Total Wire Out (0 - 365)",
"revolutions",        "double", "", "Total Revolutions",
"totalPlanktonVolume","double", "ml/1000 cubic meters of water strained", "Total Plankton Volumes",
"timeNetTowing",      "double", "minutes", "Time Net Towing",
"timeNetSinking",     "double", "minutes", "Time Net Sinking",
"totalLarvae",        "double", "", "Total Larvae (Raw Count)",
"timeEndTow",         "int",    "", "HHMM Time End Tow",  //!!! I need to connect this to towDate above
"sortingHours",       "double", "hours", "Sorting Hours",
"netCondition",       "String", "", "Net Condition",
"juvenileAdult",      "double", "", "Juvenile/Adults (Raw Count)",
"endDepth",           "double", "m", "End Depth of Tow",
"dateVolumed",        "String", "yyMMdd", "Date Volumed",
"dateID",             "String", "yyMMdd", "Date Identification Completed"},
towTypesDescription);

// * Plankton volume in cubic centimeters (excluding specimens larger than 5 cc)
//[NOTE: When counts are standardized: sum/10 meters squared of sea surface
//area not per volume]


*/
    }

    /**
     * Processes one CalCOFI2012 tsv file into a .nc file.
     * 
     * <p>The var sequence of latitude, latitudeMinutes, latitudeLocation
     * will be converted to 1 latitude column (decimal degrees).
     * Similar with longitude.
     *
     * <p>If units are yyMM, var will be converted to yyyyMM and units="".
     *
     * <p>If units are yyMMdd, var will be converted to yyyyMMdd, then
     * to "seconds since 1970-01-01T00:00:00Z" (assuming Pacific time zone) 
     * and sets attribute: time_precision=1970-01-01.
     *
     * <p>If units of variable after yyMMdd are HHmm, the date and time will
     * be combined into one "seconds since 1970-01-01T00:00:00Z" variable.
     *
     * <p>If variableName="timeZone", this checks to see if arriveDate value 
     * timeZone offset is as expected.
     *
     * @param dir
     * @param tableName  e.g., CC_TOWS
     * @param info  with 4 strings (name, type, units, long_name) for each variable 
     * @param towTypesDescription will be added as description=[towTypesDescrption]
     *   when variableName=towTypeCode.
     * @return the table, as saved in the .nc file.
     */
    public static Table processOneCalcofi2012(String dir, String tableName, 
        String info[], String towTypesDescription) throws Exception {

        String rowName = "row";
        int cutoffYear = 20;  // fourDigitYear += twoDigitYear < cutoffYear? 2000 : 1900;

/*  make tsv into ERDDAP-style .json table
//{
//  "table": {
//    "columnNames": ["longitude", "latitude", "time", "sea_surface_temperature"],
//    "columnTypes": ["float", "float", "String", "float"],
//    "columnUnits": ["degrees_east", "degrees_north", "UTC", "degree_C"],
//    "rows": [
//      [180.099, 0.032, "2007-10-04T12:00:00Z", 27.66],
//      [180.099, 0.032, null, null],
//      [189.971, -7.98, "2007-10-04T12:00:00Z", 29.08]
//    ]
//}
*/
        String inFile = dir + tableName + ".txt";
        String2.log("\n* processOneCalcofi2012 " + inFile);
        StringArray names  = new StringArray();
        StringArray types  = new StringArray();
        StringArray units  = new StringArray();
        StringArray lNames = new StringArray();
        Test.ensureEqual(info.length % 4, 0, "info.length=" + info.length);
        int nVars = info.length / 4;
        int n = 0;
        for (int v = 0; v < nVars; v++) {
            names.add( info[n++]);
            types.add( info[n++]);
            units.add( info[n++]);
            lNames.add(info[n++]);
        }

        String fromFile[] = String2.readFromFile(inFile);
        if (fromFile[0].length() > 0)
            throw new RuntimeException(fromFile[0]);
        String lines = String2.replaceAll(fromFile[1], "\t", ",");
        lines = String2.replaceAll(lines, "\n", "],\n[");
        lines = 
            "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [" + names.toJsonCsvString() + "],\n" +
            "    \"columnTypes\": [" + types.toJsonCsvString() + "],\n" +
            "    \"columnUnits\": [" + units.toJsonCsvString() + "],\n" +
            "    \"rows\": [\n" +
            "[" + lines.substring(0, lines.length() - 3) + "\n" +
            "]\n" +
            "}\n";
        String2.log(lines.substring(0, Math.min(lines.length(), 1500)));
        Table table = new Table();
        table.readJson(inFile, lines);
        String2.log("Before adjustments:\n" + String2.annotatedString(table.dataToString(5)));  
        int nRows = table.nRows();
        int nErrors = 0;

        //things where nColumns won't change
        for (int v = 0; v < table.nColumns(); v++) {  
            PrimitiveArray pa = table.getColumn(v);
            String vName = table.getColumnName(v);
            Attributes atts = table.columnAttributes(v);

            //longNames
            atts.add("long_name", lNames.get(v));  //relies on original var lists

            //timeZone
            nErrors = 0;
            if ("timeZone".equals(vName)) {
                PrimitiveArray arrivePA = table.getColumn("arriveDate"); //still yyMMdd
                GregorianCalendar gc = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"));
                Calendar2.clearSmallerFields(gc, Calendar2.DATE);
                for (int row = 0; row < nRows; row++) {
                    String val = arrivePA.getString(row);
                    if (val.matches("[0-9]{6}")) {
                        int year = String2.parseInt(val.substring(0, 2));
                        gc.set(Calendar2.YEAR, year < cutoffYear? 2000 : 1900);
                        gc.set(Calendar2.MONTH, String2.parseInt(val.substring(2, 4)) - 1); //0..
                        gc.set(Calendar2.DATE,  String2.parseInt(val.substring(4, 6)));
                        int fileHas = pa.getInt(row);
                        int calculated =  
                            -gc.get(Calendar.ZONE_OFFSET)/(60*60*1000) + //in hours
                            -gc.get(Calendar.DST_OFFSET) /(60*60*1000);  //in hours
                        if (fileHas != calculated) {
                            nErrors++;
                            if (nErrors <= 5) String2.log("ERROR: col=" + vName + 
                                " row=" + row + " timeZone=" + fileHas + 
                                " != calculated(" + val + ")=" + calculated);
                        }
                    }
                }
                if (nErrors > 0)
                    String2.pressEnterToContinue(
                        "Bob: check US daylight saving time rules. On/near these dates?\n" +
                        "https://en.wikipedia.org/wiki/History_of_time_in_the_United_States\n" +
                        "nErrors=" + nErrors);
            }

        }

        //things where nColumns may change
        for (int v = 0; v < table.nColumns(); v++) {  
            PrimitiveArray pa = table.getColumn(v);
            String vName = table.getColumnName(v);
            Attributes atts = table.columnAttributes(v);
            String vUnits = atts.getString("units");
            Attributes nextAtts = v < table.nColumns() - 1? 
                table.columnAttributes(v+1) : new Attributes();

            //towTypeCode
            if ("towTypeCode".equals(vName))
                atts.add("description", towTypesDescription);

            //latitude
            nErrors = 0;
            if ("latitude".equals(vName) &&
                "latitudeMinutes".equals(table.getColumnName(v + 1)) &&
                "latitudeLocation".equals(table.getColumnName(v + 2))) {
                FloatArray fa = new FloatArray(nRows, false); //float avoids, e.g., xx.33333333333333
                PrimitiveArray pa1 = table.getColumn(v + 1);
                PrimitiveArray pa2 = table.getColumn(v + 2);
                for (int row = 0; row < nRows; row++) {
                    String loc = pa2.getString(row);
                    String combo = pa.getString(row) + " " + pa1.getString(row) + 
                        " " + loc;
                    float f = //will be NaN if trouble
                        (pa.getFloat(row) + (pa1.getFloat(row) / 60)) *
                        (loc.equals("N")? 1 : loc.equals("S")? -1 : Float.NaN);
                    fa.atInsert(row, f);
                    if (combo.length() > 2 && //2 spaces
                        (f < -90 || f > 90 || Float.isNaN(f))) {
                        nErrors++;
                        if (nErrors <= 5) String2.log("ERROR: col=" + vName + 
                            " row=" + row + " lat=" + combo + " -> " + f);
                    }
                }
                table.setColumn(v, fa);
                table.removeColumn(v + 2);
                table.removeColumn(v + 1);
            }
            if (nErrors > 0) 
                String2.pressEnterToContinue("nErrors=" + nErrors);

            //longitude
            nErrors = 0;
            if ("longitude".equals(vName) &&
                "longitudeMinutes".equals(table.getColumnName(v + 1)) &&
                "longitudeLocation".equals(table.getColumnName(v + 2))) {
                FloatArray fa = new FloatArray(nRows, false); //float avoids, e.g., xx.33333333333333
                PrimitiveArray pa1 = table.getColumn(v + 1);
                PrimitiveArray pa2 = table.getColumn(v + 2);
                for (int row = 0; row < nRows; row++) {
                    String loc = pa2.getString(row);
                    String combo = pa.getString(row) + " " + pa1.getString(row) + 
                        " " + loc;
                    float f = //will be NaN if trouble
                        (pa.getFloat(row) + (pa1.getFloat(row) / 60)) *
                        (loc.equals("E")? 1 : loc.equals("W")? -1 : Float.NaN);
                    fa.atInsert(row, f);
                    if (combo.length() > 2 && //2 spaces
                        (f < -180 || f > 180 || Float.isNaN(f))) {
                        nErrors++;
                        if (nErrors <= 5) String2.log("ERROR: col=" + vName + 
                            " row=" + row + " lat=" + combo + " -> " + f);
                    }
                }
                table.setColumn(v, fa);
                table.removeColumn(v + 2);
                table.removeColumn(v + 1);
            }
            if (nErrors > 0) 
                String2.pressEnterToContinue("nErrors=" + nErrors);

            //yyMM  add century to cruiseYYMM
            nErrors = 0;
            if ("yyMM".equals(vUnits)) {
                for (int row = 0; row < nRows; row++) {
                    String val = pa.getString(row);
                    if (val.matches("[0-9]{4}")) {
                        int year = String2.parseInt(val.substring(0, 2));
                        pa.setString(row, (year < cutoffYear? "20" : "19") + val);
                    } else {
                        if (val.length() != 0) {
                            nErrors++;
                            if (nErrors <= 5) String2.log("ERROR: col=" + vName + 
                                " row=" + row + " yyMM=" + val);
                        }
                        pa.setString(row, "");  //set to MV
                    }
                }
                atts.set("units", "");  //leave it as a String identifier
            }
            if (nErrors > 0) 
                String2.pressEnterToContinue("nErrors=" + nErrors);

            //yyMMdd
            nErrors = 0;
            if ("yyMMdd".equals(vUnits)) {
                String nextUnits = nextAtts.getString("units");
                boolean nextHasMinutes = "HHmm".equals(nextUnits);
                if (nextHasMinutes)
                    String2.log("combining yyMMdd and next column (HHmm)");
                String nextVName = nextHasMinutes? table.getColumnName(v + 1) : "";
                DoubleArray datePA = new DoubleArray(nRows, false);
                PrimitiveArray minutesPA = nextHasMinutes? 
                    table.getColumn(v + 1) : (PrimitiveArray)null;
                //??!! Use Zulu or local, or time_zone data (in one table only)?
                GregorianCalendar gc = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"));
                Calendar2.clearSmallerFields(gc, Calendar2.DATE);
                for (int row = 0; row < nRows; row++) {
                    String val = pa.getString(row);
                    if (val.matches("[0-9]{6}")) {
                        int year = String2.parseInt(val.substring(0, 2));
                        gc.set(Calendar2.YEAR, year < cutoffYear? 2000 : 1900);
                        gc.set(Calendar2.MONTH, String2.parseInt(val.substring(2, 4)) - 1); //0..
                        gc.set(Calendar2.DATE,  String2.parseInt(val.substring(4, 6)));
                        if (nextHasMinutes) {
                            Calendar2.clearSmallerFields(gc, Calendar2.DATE);
                            int HHmm = minutesPA.getInt(row);
                            if (HHmm < 0 || HHmm > 2359) {
                                nErrors++;
                                if (nErrors <= 5) String2.log("ERROR: col=" + nextVName + 
                                    " row=" + row + " HHmm=" + minutesPA.getString(row));
                            } else {
                                gc.set(Calendar2.HOUR_OF_DAY, HHmm / 100); 
                                gc.set(Calendar2.MINUTE,      HHmm % 100);
                            }
                        }
                        datePA.add(Calendar2.gcToEpochSeconds(gc));
                    } else {
                        if (val.length() != 0) {
                            nErrors++;
                            if (nErrors <= 5) String2.log("ERROR: col=" + vName + 
                                " row=" + row + " yyMMdd=" + val);
                        }
                        datePA.add(Double.NaN);
                    }
                }
                table.setColumn(v, datePA);
                atts.set("units", Calendar2.SECONDS_SINCE_1970);
                atts.set("time_precision", //AKA EDV.TIME_PRECISION 
                    nextHasMinutes? "1970-01-01T00:00" : "1970-01-01");
                if (nextHasMinutes) 
                    table.removeColumn(v + 1);
            }
            if (nErrors > 0) 
                String2.pressEnterToContinue("nErrors=" + nErrors);

        }
        
        //save as .nc
        String2.log("After adjustments:\n" + String2.annotatedString(table.dataToString(5)));  
        table.saveAsFlatNc(dir + tableName + ".nc", rowName, false);
        return table;
    }

    /**
     * Given a whole or partial datasets.xml file, this extracts all of the unique sourceUrls,
     * then tallys the total number of datasets and number of unaggregated datasets
     * (i.e., ferret-aggregated) per domain.
     * The ferret-aggregated dataset sourceUrls are converted to their presumed original URL,
     * for easier comparison.
     * This only finds sourceUrl if it is on one line by itself.
     *
     * @param datasetsXmlFileName
     */
    public static void tallyUafAggregations(String datasetsXmlFileName) throws Exception {

        String fromTo[] = {
//alternate ferret url and sourceUrl
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/20thC_ReanV2/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/20thC_ReanV2/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/NARR",  //several variants 
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/NARR",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/cpc_us_hour_precip/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/cpc_us_hour_precip/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/cpc_us_precip/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/cpc_us_precip/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/cru/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/cru/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/godas/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/godas/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/gpcc/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/gpcc/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/interp_OLR/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/interp_OLR/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/msu/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/msu/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/ncep.reanalysis.derived/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.derived/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/ncep.reanalysis2.dailyavgs/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis2.dailyavgs/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/ncep.reanalysis2/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis2/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/noaa.ersst/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/noaa.ersst/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/snowcover/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/snowcover/",
"https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/udel.airt.precip/",
"https://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/udel.airt.precip/"
};

        //extract unique sourceUrls
        HashSet<String> ferretAggregations = new HashSet();  //hashsets automatically avoid duplicates
        HashSet<String> ferretAggregationSources = new HashSet();
        HashSet<String> others = new HashSet();
        String source;

        BufferedReader in = File2.getDecompressedBufferedFileReader(datasetsXmlFileName, String2.ISO_8859_1); 
        try {
            while ((source = in.readLine()) != null) {
                source = source.trim();
                if (source.startsWith("<sourceUrl>") &&
                    source.endsWith(  "</sourceUrl>")) {
                    source = source.substring(11, source.length() - 12);
                    String unferret = null;
                    if (source.startsWith("https://ferret.pmel.noaa.gov/geoide/dodsC/Datasets/")) {
                        for (int ft = 0; ft < fromTo.length; ft +=2) {
                            if (source.startsWith(fromTo[ft])) {
                                unferret = fromTo[ft + 1] + source.substring(fromTo[ft].length());
                                break;
                            }
                        }
                    }
                    if (unferret == null) {
                        others.add(source);
                    } else {
                        if (unferret.endsWith("_aggregation")) {
                            ferretAggregations.add(source);
                            ferretAggregationSources.add(unferret.substring(0, unferret.length() - 12));
                        } else {
                            others.add(unferret);
                        }
                    }
                }
            }
        } finally {
            in.close();
        }

        String2.log("***** ferretAggregations");
        String sar[] = ferretAggregations.toArray(new String[0]);
        Arrays.sort(sar);
        String2.log(String2.toNewlineString(sar));

        String2.log("***** ferretAggregationSources");
        sar = ferretAggregationSources.toArray(new String[0]);
        Arrays.sort(sar);
        String2.log(String2.toNewlineString(sar));
        Tally tally = new Tally();
        for (int i = 0; i < sar.length; i++)
            tally.add("ferretAggregationSources", File2.getProtocolDomain(sar[i]));

        String2.log("\n***** others");
        sar = others.toArray(new String[0]);
        Arrays.sort(sar);
        String2.log(String2.toNewlineString(sar));
        for (int i = 0; i < sar.length; i++)
            tally.add("otherDatasets", File2.getProtocolDomain(sar[i]));

        String2.log("\n***** Tally Info");
        String2.log(tally.toString());

    }

    /** create lat lon file for viirs */
    public static void viirsLatLon(boolean create) throws Exception {
        String dir = "/u00/data/viirs/MappedDaily4km/";
        String fileName = "LatLon.nc";  //"fakeDim01.nc";
        String latName = "latitude";    //"fakeDim0"; 
        int nLat = 4320;
        double lat0 = 89.97916666666666666666666666;
        String lonName = "longitude";   //"fakeDim1"; 
        int nLon = 8640;
        double lon0 = -179.97916666666666666666666666;
        double inc = 0.041666666666666666666666666;

        if (create) {
            NetcdfFileWriter nc = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf3, dir + fileName);
            boolean nc3Mode = true;
            try {
                Group rootGroup = nc.addGroup(null, "");
                nc.setFill(false);

                Attributes atts = new Attributes();

                //lat
                atts.add("units", "degrees_north");
                Dimension latDim = nc.addDimension(rootGroup, latName, nLat);
                Variable latVar = nc.addVariable(rootGroup, latName, 
                    NcHelper.getNc3DataType(PAType.DOUBLE), Arrays.asList(latDim)); 
                NcHelper.setAttributes(nc3Mode, latVar, atts, false);  //isUnsigned

                //lon
                atts.add("units", "degrees_east");
                Dimension lonDim = nc.addDimension(rootGroup, lonName, nLon);
                Variable lonVar = nc.addVariable(rootGroup, lonName, 
                    NcHelper.getNc3DataType(PAType.DOUBLE), Arrays.asList(lonDim)); 
                NcHelper.setAttributes(nc3Mode, lonVar, atts, false); //isUnsigned

                //write global attributes
                //NcHelper.setAttributes(nc3Mode, nc, "NC_GLOBAL", ada.globalAttributes());

                //leave "define" mode
                nc.create();

                //write the lat values  (top to bottom!)
                DoubleArray da = new DoubleArray();
                for (int i = 0; i < nLat; i++)
                    da.add(lat0 - i * inc);
                nc.write(latVar, NcHelper.get1DArray(da));

                //write the lon values
                da = new DoubleArray();
                for (int i = 0; i < nLon; i++)
                    da.add(lon0 + i * inc);
                nc.write(lonVar, NcHelper.get1DArray(da));

                //if close throws Throwable, it is trouble
                nc.close(); //it calls flush() and doesn't like flush called separately
                nc = null;

                //diagnostic
                String2.log("  createViirsLatLon finished successfully\n");

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            } finally {
                if (nc != null) nc.abort();
            }
        }

        //read the file
        NetcdfFile nc = NcHelper.openFile(dir + fileName);
        try {
            Variable v = nc.findVariable(latName);
            PrimitiveArray pa = NcHelper.getPrimitiveArray(v);
            String2.log(latName + 
                " [0]=" + pa.getString(0) +
                " [1]=" + pa.getString(1) +
                " [" + (pa.size()-1) + "]=" + pa.getString(pa.size()-1));

            v = nc.findVariable(lonName);
            pa = NcHelper.getPrimitiveArray(v);
            String2.log(lonName + 
                " [0]=" + pa.getString(0) +
                " [1]=" + pa.getString(1) +
                " [" + (pa.size()-1) + "]=" + pa.getString(pa.size()-1));


        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        } finally {
            nc.close();
        }

    }

    /** This prints time, lat, and lon values from an .ncml dataset. */
    public static String dumpTimeLatLon(String ncmlName) throws Exception {

        String latName = "latitude";   
        String lonName = "longitude";  
        StringBuilder sb = new StringBuilder();
        sb.append("ncmlName=" + ncmlName + "\n"); 
        NetcdfFile nc = NcHelper.openFile(ncmlName);
        try {
            Variable v = nc.findVariable(latName);
            PrimitiveArray pa = NcHelper.getPrimitiveArray(v);
            sb.append(latName + 
                " [0]=" + pa.getString(0) +
                " [1]=" + pa.getString(1) +
                " [" + (pa.size()-1) + "]=" + pa.getString(pa.size()-1) + "\n");

            v = nc.findVariable(lonName);
            pa = NcHelper.getPrimitiveArray(v);
            sb.append(lonName + 
                " [0]=" + pa.getString(0) +
                " [1]=" + pa.getString(1) +
                " [" + (pa.size()-1) + "]=" + pa.getString(pa.size()-1) + "\n");

            v = nc.findVariable("time");
            pa = NcHelper.getPrimitiveArray(v);
            sb.append("time" +
                " [0]=" + pa.getString(0) +
                " [1]=" + pa.getString(1) +
                " [" + (pa.size()-1) + "]=" + pa.getString(pa.size()-1) + "\n");

        } catch (Throwable t) {
            String2.log(sb.toString());
            String2.log(MustBe.throwableToString(t));
        } finally {
            nc.close();
        }
        return sb.toString();
    }

    /** 
     * Generate ncml which assigns "days since 1970-01-01" coordValue to a file for a range of times. 
     * E.g., &lt;netcdf location="V2013074.L3m_DAY_NPP_CHL_chlor_a_4km" coordValue="15779"/&gt;
     * The string is displayed and put on the clipboard.
     * 
     * @param location  with '*' where YYYYDDD goes.
     * @param startIso
     * @param stopIso   (inclusive)
     * @param increment e.g., 1, 7, or 3
     * @param field  e.g. Calendar2.DAY_OF_YEAR, Calendar2.MONTH
     * @return ncml which assigns "days since 1970-01-01" coordValue to a file for a range of times. 
     */
    public static String makeNcmlCoordValues(String location, String startIso, String stopIso, 
        int increment, int field) throws Exception {

        StringBuilder sb = new StringBuilder();
        double baseFactor[] = Calendar2.getTimeBaseAndFactor("days since 1970-01-01");
        GregorianCalendar gc     = Calendar2.parseISODateTimeZulu(startIso);
        GregorianCalendar stopGc = Calendar2.parseISODateTimeZulu(stopIso);
        long stopMillis = stopGc.getTimeInMillis();
        while (gc.getTimeInMillis() <= stopMillis) {
            int daysSince = Math2.roundToInt(Calendar2.epochSecondsToUnitsSince(
                baseFactor[0], baseFactor[1], Calendar2.gcToEpochSeconds(gc)));
            sb.append("<netcdf location=\"" + 
                String2.replaceAll(location, "*", Calendar2.formatAsYYYYDDD(gc)) +
                "\" coordValue=\"" + daysSince + "\"/>\n");
            gc.add(field, increment);
        }
        String s = sb.toString();
        String2.setClipboardString(s);
        String2.log(s);
        return s;
    }
    
    /** 
     * Convert FED Rockfish CTD to .nc.
     * I do the .xls to tsv by hand in Excel -- use time format yyyy-mm-dd h:mm (24 hour h, no am/pm). 
     * Then use macro in EditPlus to fix the hour data (regex search "-[0-9]{2} [0-9]:")
     *   and insert 0 so it is Thh (2 digits). 
     * Then copy rockfish_casts_yyyy.nc and rockfish_header_yyyy.nc to rockfish20130409
     *  so they are part of erdFed

     * From Lynn 2013-03-28 from /Volumes/PFEL_Shared_Space/PFEL_Share/Lynn2Bob/Rockfish_CTD.tar.gz).
     * From Lynn 2017-02-03 from xserve /home/ldewitt/FED_rockfish_CTD
     *
     * @param lastYear year of processing e.g., 2013, 2015
     */
    public static void convertRockfish(int lastYear) throws Throwable {
        String2.log("\nProjects.convertRockfish()");
        String dir = null, outerName = null, innerName = null, fileExtension = null;
        if (lastYear == 2011) {
            dir = "/u00/data/points/rockfish20130328/"; 
            outerName = "ERD_CTD_HEADER_2008_to_2011";  //.csv -> .nc
            innerName = "ERD_CTD_CAST_2008_to_2011";    //.csv -> .nc
            fileExtension = ".csv";

        } else if (lastYear == 2015) {
            dir = "/data/rockfish/"; 
            outerName = "NOAA NMFS SWFSC Santa Cruz CTD_HEADER 2012-2015bob";  //tsv -> .nc
            innerName = "NOAA NMFS SWFSC Santa Cruz CTD_CAST 2012-2015";    //tsv -> .nc
            fileExtension = ".txt";

        } else {
            throw new RuntimeException("unsupported year");
        }

        //read the outer files
        Table outer = new Table();
        outer.readASCII(dir + outerName + fileExtension, String2.ISO_8859_1,
            "", "", 0, 1, "\t", 
            null, null, null, null, false); //simplify
        Test.ensureEqual(outer.getColumnNamesCSVString(),
            "CRUISE,CTD_INDEX,CTD_NO,STATION,CTD_DATE,CTD_LAT,CTD_LONG,CTD_BOTTOM_DEPTH,BUCKET_TEMP,BUCKET_SAL,TS_TEMP,TS_SAL",
            "Unexpected outer column names");
        for (int coli = 0; coli < outer.nColumns(); coli++) 
            outer.setColumnName(coli, outer.getColumnName(coli).toLowerCase());
        String2.log("outer (5 rows) as read:\n" + outer.dataToString(5));

        //convert to short 
        String colNames[] = {"ctd_index","ctd_no","station","ctd_bottom_depth"};
        for (int coli = 0; coli < colNames.length; coli++) {
            int col = outer.findColumnNumber(colNames[coli]);
            outer.setColumn(col, new ShortArray(outer.getColumn(col)));
            if (colNames[coli].equals("ctd_bottom_depth"))
                outer.setColumnName(col, "bottom_depth");
        }

        //convert to floats 
        colNames = new String[]{"bucket_temp","bucket_sal","ts_temp","ts_sal"};
        String newColNames[] = new String[]{"bucket_temperature","bucket_salinity",
            "ts_temperature","ts_salinity"};
        for (int coli = 0; coli < colNames.length; coli++) {
            int col = outer.findColumnNumber(colNames[coli]);
            outer.setColumn(col, new FloatArray(outer.getColumn(col)));
            outer.setColumnName(col, newColNames[coli]);
        }

        //convert date time e.g., "2008-05-05 12:10" to epoch seconds
        String dateTimeFormat = "yyyy-MM-dd HH:mm";
        String timeZoneString = "America/Los_Angeles";
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
        //was DateTimeFormatter dtf = Calendar2.makeDateTimeFormatter(dateTimeFormat, timeZoneString); 
        //GMT: erddap/convert/time.html says "5/5/2008 19:10" = 1.2100146E9
        //  if 7 hours different in summer...
        Test.ensureEqual(Calendar2.parseToEpochSeconds("2008-05-05 12:10", dateTimeFormat, timeZone), 
            1.2100146E9, //erddap/convert/time.html
            "trouble with DateTimeFormat");
        int nOuter = outer.nRows();
        {
            int col = outer.findColumnNumber("ctd_date");
            PrimitiveArray oldTimePA = outer.getColumn(col);
            DoubleArray newTimePA = new DoubleArray();
            for (int row = 0; row < nOuter; row++) 
                newTimePA.add(Calendar2.parseToEpochSeconds(oldTimePA.getString(row), dateTimeFormat, timeZone));
            outer.setColumnName(col, "time");
            outer.setColumn(col, newTimePA);
            outer.columnAttributes(col).set("units", "seconds since 1970-01-01T00:00:00Z");
        }

        //convert lat and lon from dddmm.mmmm to decimal degrees
        colNames = new String[]{"ctd_lat","ctd_long"};
        for (int coli = 0; coli < colNames.length; coli++) {
            int col = outer.findColumnNumber(colNames[coli]);
            PrimitiveArray pa = outer.getColumn(col);
            FloatArray fa = new FloatArray();
            Test.ensureEqual(Math.floor(1234.5 / 100.0) + (1234.5 % 100.0) / 60.0, 12.575, "");
            float scale = coli == 0? 1 : -1; //lon are originally degrees_west!
            for (int row = 0; row < nOuter; row++) {
                double d = pa.getDouble(row);
                if (d < 0) throw new SimpleException("d<0 requires more testing");
                fa.add(scale * Math2.doubleToFloatNaN(Math.floor(d / 100.0) + (d % 100.0) / 60.0));
            }
            outer.setColumnName(col, coli == 0? "latitude": "longitude");
            outer.setColumn(col, fa);
        }
        
        //save the outer as .nc
        String2.log("outer (5 rows) before save:\n" + outer.toString(5));
        outer.saveAsFlatNc(dir + "rockfish_header_" + lastYear + ".nc", "row", false); //convertToFakeMissingValues

        //just keep the outer columns needed for inner table
        StringArray desired = StringArray.fromCSV("cruise,ctd_index,ctd_no,station,time,latitude,longitude");
        Test.ensureEqual(outer.reorderColumns(desired, true), desired.size(), 
            "Not all desired columns were found.");

        //read inner table
        Table inner = new Table();
        inner.readASCII(dir + innerName + fileExtension, String2.ISO_8859_1,
            "", "", 0, 1, "\t", 
            null, null, null, null, false); //simplify
        for (int coli = 0; coli < inner.nColumns(); coli++) 
            inner.setColumnName(coli, inner.getColumnName(coli).toLowerCase());
        Test.ensureEqual(inner.getColumnNamesCSVString(),
            "cruise,ctd_index,ctd_depth,temperature,salinity,density,dyn_hgt,irrad,fluor_volt,transmissivity,chlorophyll,oxygen_volt,oxygen",
            "Unexpected inner column names");

        //convert to short 
        colNames = new String[]{"ctd_index","ctd_depth"};
        for (int coli = 0; coli < colNames.length; coli++) {
            int col = inner.findColumnNumber(colNames[coli]);
            inner.setColumnName(col, coli == 0? "ctd_index": "depth");
            inner.setColumn(col, new ShortArray(inner.getColumn(col)));
        }

        //convert to floats 
        colNames = new String[]{"temperature","salinity","density","dyn_hgt","irrad","fluor_volt","transmissivity","chlorophyll","oxygen_volt","oxygen"};
        for (int coli = 0; coli < colNames.length; coli++) {
            int col = inner.findColumnNumber(colNames[coli]);
            inner.setColumn(col, new FloatArray(inner.getColumn(col)));
            if (colNames[coli].equals("irrad"))
                inner.setColumnName(col, "irradiance");
        }

        //add outer info to inner table
        inner.join(2, 0, "", outer); //nKeys, keyCol, String mvKey, Table lookUpTable

        //save inner table
        String2.log("inner (5 rows) before save:\n" + inner.toString(5));
        inner.saveAsFlatNc(dir + "rockfish_casts_" + lastYear + ".nc", "row", false); //convertToFakeMissingValues

        String2.log("\n*** Projects.convertRockfish() finished successfully");
    }

    /** Convert FED Rockfish CTD .csv data files to .nc (from Lynn 2013-04-09
     * from /Volumes/PFEL_Shared_Space/PFEL_Share/Lynn2Bob/Rockfish_CTD.tar.gz).
     */
    public static void convertRockfish20130409(boolean headerMode) throws Throwable {
        String2.log("\nProjects.convertRockfish20130409(headerMode=" + headerMode + ")");
        String dir = "C:/u00/data/points/rockfish20130409/"; 
        String regex = "rockfish_" + (headerMode? "header" : "casts") + 
            "_[0-9]{4}\\.csv";  //.csv -> .nc

        //read the outer .csv files
        String tFileNames[] = RegexFilenameFilter.fullNameList(dir, regex);
        for (int f = 0; f < tFileNames.length; f++) {

            Table table = new Table();
            table.readASCII(tFileNames[f], String2.ISO_8859_1,
                "", "", 0, 2, "", null, null, null, null, false); //simplify
            Test.ensureEqual(table.getColumnNamesCSVString(),
                headerMode?
                    "cruise,ctd_index,ctd_no,station,time,longitude,latitude,bottom_depth," +
                        "bucket_temperature,bucket_salinity,ts_temperature,ts_salinity" :
                    "cruise,ctd_index,ctd_no,station,time,longitude,latitude,depth,temperature," +
                        "salinity,density,dyn_hgt,irradiance,fluor_volt,transmissivity," +
                        "chlorophyll,oxygen_volt,oxygen",
                "Unexpected column names");
            if (f == 0)
                String2.log("table (5 rows) as read:\n" + table.dataToString(5));

            //convert to short 
            String colNames[] = headerMode? 
                new String[]{"ctd_index","ctd_no","station","bottom_depth"} :
                new String[]{"ctd_index","ctd_no","station","depth"};
            for (int coli = 0; coli < colNames.length; coli++) {
                int col = table.findColumnNumber(colNames[coli]);
                table.setColumn(col, new ShortArray(table.getColumn(col)));
            }

            //convert to floats 
            colNames = headerMode?
                new String[]{"longitude","latitude","bucket_temperature","bucket_salinity","ts_temperature","ts_salinity"} :
                new String[]{"longitude","latitude","temperature","salinity","density","dyn_hgt","irradiance",
                             "fluor_volt","transmissivity","chlorophyll","oxygen_volt","oxygen"};
            for (int coli = 0; coli < colNames.length; coli++) {
                int col = table.findColumnNumber(colNames[coli]);
                table.setColumn(col, new FloatArray(table.getColumn(col)));
            }

            //convert iso date time to epochSeconds time   
            int ntable = table.nRows();
            {
                int col = table.findColumnNumber("time");
                PrimitiveArray oldTimePA = table.getColumn(col);
                DoubleArray newTimePA = new DoubleArray();
                for (int row = 0; row < ntable; row++) 
                    newTimePA.add(Calendar2.isoStringToEpochSeconds(oldTimePA.getString(row)));
                table.setColumn(col, newTimePA);
                table.columnAttributes(col).set("units", "seconds since 1970-01-01T00:00:00Z");
            }

            //save as .nc
            String2.log("f=" + f + " finished.");
            if (f == 0) 
                String2.log("table (5 rows) before save:\n" + table.toString(5));
            table.saveAsFlatNc(dir + File2.getNameNoExtension(tFileNames[f]) + ".nc", 
                "row", false); //convertToFakeMissingValues
        }

        String2.log("\n*** Projects.convertRockfish20130409(headerMode=" + headerMode + 
            ") finished successfully");
    }

    /**
     * This queries a list of coastwatch ERDDAP datasets and makes a table with
     * the last time point for each dataset.
     *
     * @param griddapUrl e.g., "https://coastwatch.pfeg.noaa.gov/erddap/griddap/"
     * @param datasetIDs a list of datasetIDs
     */
    public static void lastTime(String griddapUrl, StringArray datasetIDs) throws Exception {
        String2.log("\n*** Projects.lastTime(" + griddapUrl + ")");
        int n = datasetIDs.size();
        StringArray datasetInfo = new StringArray(n, false);
        for (int i = 0; i < n; i++) {
            String result = "";
            try {
                //request time[last] and get the date from the 3rd line   2007-04-12T14:00:00Z
                ArrayList<String> response = SSR.getUrlResponseArrayList(
                    griddapUrl + datasetIDs.get(i) + ".csv?time[last]");
                if (response.size() >= 3)
                    result = response.get(2);
                else if (response.size() >= 1)
                    result = response.get(0);
                else result = "[Response = \"\"]";
            } catch (Throwable t) {
                result = t.toString();
            }
            if (result.length() > 64)
                result = result.substring(0, 64);
            datasetInfo.add(String2.left(datasetIDs.get(i), 21) + " " + result);
            String2.log(datasetInfo.get(i));
        }
        String2.log("\nLast Times (as of " + Calendar2.getCurrentISODateTimeStringLocalTZ() + 
            ")\n" + datasetInfo.toNewlineString());
    }

    /**
     * This makes regularly-spaced (alternate 3, 4 days) .ncml files for the CRW datasets.
     * 
     * @param isoStartDate Exact Date for first .ncml file, e.g., "2000-12-09" 
     * @param firstIncrement 3 or 4 days to second .ncml file
     * @param isoEndDate doesn't have to fall on valid date
     * @param varName   e.g., sst, anomaly, dhw, hotspot, baa
     */
    public static void makeCRWNcml34(String isoStartDate, int firstIncrement, String isoEndDate,
        String varName) {

        int secondsPerDay = Calendar2.SECONDS_PER_DAY;
        double startSeconds = Calendar2.isoStringToEpochSeconds(isoStartDate);
        double endSeconds   = Calendar2.isoStringToEpochSeconds(isoEndDate);
        double seconds = startSeconds;
        int increment = firstIncrement;
        String dir = "/u00/satellite/CRW/" + varName + "/ncml/";
        String varNameInFileName = varName.equals("sst")? "night" : varName;
        String varNameInFile = 
            varName.equals("sst")?     "CRW_SST"        : 
            varName.equals("anomaly")? "CRW_SSTANOMALY" :
            varName.equals("dhw")?     "CRW_DHW"        :
            varName.equals("hotspot")? "CRW_HOTSPOT"    :   
            varName.equals("baa")?     "CRW_BAA"        : "error";
        if (varNameInFile.equals("error"))
            throw new RuntimeException("Unknown varName=" + varName);
        //String2.log("dir=" + dir);

        while (seconds <= endSeconds) {
            String yyyymmdd = String2.replaceAll(
                Calendar2.epochSecondsToIsoDateString(seconds), "-", "");
            int daysSince = Math2.roundToInt(seconds / secondsPerDay);
            int n = yyyymmdd.compareTo("20090604") >= 0? 19 :
                    yyyymmdd.compareTo("20050822") >= 0? 18 :
                    yyyymmdd.compareTo("20010223") >= 0? 16 : 14;
            String fileName = "sst." + varNameInFileName + ".field.50km.n" + n + "." + yyyymmdd;
            String2.log(daysSince + " " + fileName);

            String contents = 
"<netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">\n" +
"  <variable name=\"time\" type=\"int\" shape=\"time\">\n" +
"    <attribute name=\"units\" value=\"days since 1970-01-01T00:00:00Z\"/>\n" +
"    <attribute name=\"_CoordinateAxisType\" value=\"Time\" />\n" +
"    <attribute name=\"axis\" value=\"T\" />\n" +
"  </variable>\n" +
"  <aggregation dimName=\"time\" type=\"joinNew\">\n" +
"    <variableAgg name=\"" + varNameInFile + "\"/>\n" +
"    <variableAgg name=\"surface_flag\"/>\n" +
"    <netcdf location=\"" + fileName + ".hdf\" coordValue=\"" + daysSince + "\"/>\n" +
"  </aggregation>\n" +
"</netcdf>\n";
            String2.writeToFile(dir + fileName + ".ncml", contents);

            //increment seconds
            seconds += increment * Calendar2.SECONDS_PER_DAY;
            increment = increment == 3? 4 : 3;
        }
    }

    /** 
     * This was needed to make the initial, irregularly-spaced ncml files
     * corresponding to the .hdf files because the .hdf files often
     * break the 3/4 day pattern.
     *
     * @param varName   e.g., sst, anomaly, dhw, hotspot, baa
     */
    public static void makeCRWToMatch(String varName) {
        String dataDir = "/u00/satellite/CRW/" + varName + "/";
        String ncmlDir = dataDir + "ncml/";

        //get the list of .hdf files
        String[] fileNames = RegexFilenameFilter.list(dataDir, ".*\\.hdf");

        //make an ncml file for each
        int n = fileNames.length;
        for (int i = 0; i < n; i++) {
            //String2.log(fileNames[i]);
            String nameNoExt = fileNames[i].substring(0, fileNames[i].length() - 4);
            String compactDate = nameNoExt.substring(nameNoExt.length() - 8);
            String isoDate = compactDate.substring(0, 4) + "-" + 
                             compactDate.substring(4, 6) + "-" + 
                             compactDate.substring(6); 
            makeCRWNcml34(isoDate, 3, isoDate, varName);
        }
    }

    /**
     * Convert Isaac's North Pacific High .csv into .nc (and clean up).
     */
    public static void makeIsaacNPH() throws Throwable {
        Table table = new Table();
        table.readASCII("/u00/data/points/isaac/NPH_IDS_Feb2014.csv");
        //Year,Month,Lon Center,Lat Center,Area,Max SLP
        Test.ensureEqual(table.getColumnNamesCSVString(), 
            "Year,Month,Lon Center,Lat Center,Area,Max SLP", "");

        table.setColumnName(0, "year");
        table.columnAttributes(0).set("long_name", "Year");

        table.setColumnName(1, "month");
        table.columnAttributes(1).set("long_name", "Month (1 - 12)");

        table.setColumnName(2, "longitude");
        table.columnAttributes(2).set("units", "degrees_east");
        table.columnAttributes(2).set("long_name", "Longitude of the Center of the NPH");

        table.setColumnName(3, "latitude");
        table.columnAttributes(3).set("units", "degrees_north");
        table.columnAttributes(3).set("long_name", "Latitude of the Center of the NPH");

        table.setColumnName(4, "area");
        table.columnAttributes(4).set("long_name", "Areal Extent of the 1020 hPa Contour");
        table.columnAttributes(4).set("units", "km2");

        table.setColumnName(5, "maxSLP");
        table.columnAttributes(5).set("long_name", "Maximum Sea Level Pressure");
        table.columnAttributes(5).set("units", "hPa");

        //make seconds since 1970-01-01  
        PrimitiveArray year = table.getColumn(0);
        PrimitiveArray mon  = table.getColumn(1);
        DoubleArray time = new DoubleArray();
        int nRows = table.nRows();
        for (int i = 0; i < nRows; i++) {
            GregorianCalendar gc = Calendar2.newGCalendarZulu(year.getInt(i), mon.getInt(i), 16);  //centered
            time.add(Calendar2.gcToEpochSeconds(gc));
        }
        table.addColumn(0, "time", time, new Attributes());
        table.setColumnName(0, "time");
        table.columnAttributes(0).set("long_name", "Centered Time");
        table.columnAttributes(0).set("units", Calendar2.SECONDS_SINCE_1970);

        String2.log(table.toString(3));

        table.saveAsFlatNc("/u00/data/points/isaac/NPH_IDS.nc", "time", false);  //convertToFakeMV=false
    }

    /**
     * Make simpleTest.nc
     */
    public static void makeSimpleTestNc() throws Throwable {
        Table table = new Table();

        ByteArray timeStamps = new ByteArray(new byte[]{});
                ByteArray arByte2     = new ByteArray(   new byte[]  {5,   15,  50,   25});

        table.addColumn(0, "days", 
            new ByteArray(new byte[]  {1,2,3,4}), 
            new Attributes());
        table.addColumn(1, "hours", 
            new ByteArray(new byte[]  {5,6,7,8}), 
            new Attributes());
        table.addColumn(2, "minutes", 
            new ByteArray(new byte[]  {9,10,11,12}), 
            new Attributes());
        table.addColumn(3, "seconds", 
            new ByteArray(new byte[]  {20,21,22,23}), 
            new Attributes());
        table.addColumn(4, "millis", 
            new ByteArray(new byte[]  {30,31,32,33}), 
            new Attributes());
        table.addColumn(5, "bytes", 
            new ByteArray(new byte[]  {40,41,42,43}), 
            new Attributes());
        table.addColumn(6, "shorts", 
            new ShortArray(new short[]  {10000,10001,10002,10004}), 
            new Attributes());
        table.addColumn(7, "ints", 
            new IntArray(new int[]  {1000000,1000001,1000002,1000004}), 
            new Attributes());
        table.addColumn(8, "longs", 
            new LongArray(new long[]  {10000000000L,10000000001L,10000000002L,10000000004L}), 
            new Attributes());
        table.addColumn(9, "floats", 
            new FloatArray(new float[]  {0,1.1f,2.2f,4.4f}), 
            new Attributes());
        double d = 1e12;
        table.addColumn(10, "doubles", 
            new DoubleArray(new double[]  {d, d+0.1, d+0.2, d+0.3}), 
            new Attributes());
        table.addColumn(11, "Strings", 
            new StringArray(new String[]  {"0","10","20","30"}), 
            new Attributes());

        table.columnAttributes(0).set("units", "days since 1970-01-01");
        table.columnAttributes(1).set("units", "hours since 1980-01-01");
        table.columnAttributes(2).set("units", "minutes since 1990-01-01");
        table.columnAttributes(3).set("units", "seconds since 2000-01-01");
        table.columnAttributes(4).set("units", "millis since 2010-01-01");

        table.saveAsFlatNc(String2.unitTestDataDir + "simpleTest.nc", "row", false);  //convertToFakeMV=false
    }

    /**
     * Convert Isaac's PCUI .csv into .nc (and clean up).
     * I removed 2014 row by hand: 2014,-9999,-9999,-9999,-9999,-9999,-9999
     */
    public static void makeIsaacPCUI() throws Throwable {
        Table table = new Table();
        table.readASCII("/u00/data/points/isaac/PCUI_IDS_Feb2014-1.csv");
        //Year,Month,Lon Center,Lat Center,Area,Max SLP
        Test.ensureEqual(table.getColumnNamesCSVString(), 
            "Year,33N,36N,39N,42N,45N,48N", "");
        String pcui = "PCUI at ";


        table.setColumnName(0, "time");
        table.columnAttributes(0).set("long_name", "Centered Time");
        table.columnAttributes(0).set("units", Calendar2.SECONDS_SINCE_1970);

        table.setColumnName(1, "pcui33N");
        table.columnAttributes(1).set("long_name", pcui + "33N");
        table.columnAttributes(1).set("units", "m^3 s^-1 100m^-1");

        table.setColumnName(2, "pcui36N");
        table.columnAttributes(2).set("long_name", pcui + "36N");
        table.columnAttributes(2).set("units", "m^3 s^-1 100m^-1");

        table.setColumnName(3, "pcui39N");
        table.columnAttributes(3).set("long_name", pcui + "39N");
        table.columnAttributes(3).set("units", "m^3 s^-1 100m^-1");

        table.setColumnName(4, "pcui42N");
        table.columnAttributes(4).set("long_name", pcui + "42N");
        table.columnAttributes(4).set("units", "m^3 s^-1 100m^-1");

        table.setColumnName(5, "pcui45N");
        table.columnAttributes(5).set("long_name", pcui + "45N");
        table.columnAttributes(5).set("units", "m^3 s^-1 100m^-1");

        table.setColumnName(6, "pcui48N");
        table.columnAttributes(6).set("long_name", pcui + "48N");
        table.columnAttributes(6).set("units", "m^3 s^-1 100m^-1");

        for (int col = 1; col < 7; col++)
            table.setColumn(col, new FloatArray(table.getColumn(col)));

        //make seconds since 1970-01-01  
        PrimitiveArray year = table.getColumn(0);
        DoubleArray time = new DoubleArray();
        int nRows = table.nRows();
        for (int i = 0; i < nRows; i++) {
            GregorianCalendar gc = Calendar2.newGCalendarZulu(year.getInt(i), 7, 1); //centered
            time.add(Calendar2.gcToEpochSeconds(gc));
        }
        table.setColumn(0, time);
        String2.log(table.toString(3));

        table.saveAsFlatNc("/u00/data/points/isaac/PCUI_IDS.nc", "time", false);  //convertToFakeMV=false
    }

    /**
     * This gets a list and a count of varNames and data types in a group of files.
     */
    public static void getTabularFileVarNamesAndTypes(String dir, String nameRegex) 
          throws Throwable {
        String fileNames[] = (new File(dir)).list(); 
        int nMatching = 0;
        Table table = new Table();
        Tally tally = new Tally();
        for (int f = 0; f < fileNames.length; f++) {
            String fileName = fileNames[f];

            if (fileName != null && fileName.matches(nameRegex)) {
                table.clear();
                table.readNDNc(dir + fileName, null, 0,  //standardizeWhat=0
                    null, 0, 0);
                int nCols = table.nColumns();
                PrimitiveArray pa = table.globalAttributes().get("wmo_platform_code");
                tally.add("wmo_platform_code",
                    pa == null? "null" : pa.elementTypeString());

                for (int c = 0; c < nCols; c++)
                    tally.add(table.getColumnName(c), 
                        table.getColumn(c).elementTypeString());
            }
        }
        String2.log("\n*** Projects.lookAtFiles finished successfully. nMatchingFiles=" + nMatching + "\n" +
            dir + ", " + nameRegex + "\n" +
            tally);
    }

    /** Make VH 1day .ncml files. */
    public static void makeVH1dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int jDate = 1; jDate <= nDays; jDate++) {
                String yj = year + String2.zeroPad("" + jDate, 3);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        Calendar2.newGCalendarZulu(year, jDate).getTimeInMillis()/1000));
                String2.log(yj + " " + daysSince);      
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VHncml/" +
                        varDirNames[var] + "/ncml1day/V" + yj + ".ncml"));
                    w.write(
/* C:/content/scripts/VHncml/chla/ncml1day/V2014365.ncml is
<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V2014365.L3m_DAY_NPP_CHL_chlor_a_4km' coordValue='16435'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='l3m_data'/>\n" +
"    <netcdf location='V" + yj + ".L3m_DAY_NPP_" + jplNames[var] + "_4km' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH 8day .ncml files. 
     * coordValue is firstDay. First 3 of 2012 are 15340, 15348, 15356. 
     * Every year, start again Jan 1-8, 9-16, ...
     * End of year, 
     */
    public static void makeVH8dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int day1 = 1; day1 <= nDays; day1 += 8) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, day1);

                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, 
                    Math.min(nDays, day1+7));
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                if (day1+7 > nDays)
                    daysSince--; //imperfect
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VHncml/" +
                        varDirNames[var] + "/ncml8day/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VHncml/chla/ncml8day/V20120012012008.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012008.L3m_8D_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='l3m_data'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_8D_NPP_" + jplNames[var] + "_4km' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH mday .ncml files. 
     * coordValue is firstDay of the month. First 3 of 2012 are 15340, 15371, 15400. 
     */
    public static void makeVHmdayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};

        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, month, 1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, month+1, 0);
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VHncml/" +
                        varDirNames[var] + "/ncmlmon/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VHncml/chla/ncmlmon/V20120012012031.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf>
 */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='l3m_data'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_MO_NPP_" + jplNames[var] + "_4km' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH2 1day .ncml files. */
    public static void makeVH21dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int jDate = 1; jDate <= nDays; jDate++) {
                String yj = year + String2.zeroPad("" + jDate, 3);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        Calendar2.newGCalendarZulu(year, jDate).getTimeInMillis()/1000));
                String2.log(yj + " " + daysSince);      
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH2ncml/" +
                        varDirNames[var] + "/ncml1day/V" + yj + ".ncml"));
                    w.write(
/* C:/content/scripts/VH2ncml/chla/ncml1day/V2014365.ncml is
<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V2014365.L3m_DAY_NPP_CHL_chlor_a_4km' coordValue='16435'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj + ".L3m_DAY_NPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH2 8day .ncml files. 
     * coordValue is firstDay. First 3 of 2012 are 15340, 15348, 15356. 
     * Every year, start again Jan 1-8, 9-16, ...
     * End of year, 
     */
    public static void makeVH28dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int day1 = 1; day1 <= nDays; day1 += 8) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, day1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, 
                    Math.min(nDays, day1+7));
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                if (day1+7 > nDays)
                    daysSince--; //imperfect
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH2ncml/" +
                        varDirNames[var] + "/ncml8day/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VH2ncml/chla/ncml8day/V20120012012008.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012008.L3m_8D_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_8D_NPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH2 mday .ncml files. 
     * coordValue is firstDay of the month. First 3 of 2012 are 15340, 15371, 15400. 
     */
    public static void makeVH2mdayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, month, 1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, month+1, 0);
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH2ncml/" +
                        varDirNames[var] + "/ncmlmon/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VH2ncml/chla/ncmlmon/V20120012012031.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf>
 */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_MO_NPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH3 1day .ncml files. */
    public static void makeVH31dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int jDate = 1; jDate <= nDays; jDate++) {
                String yj = year + String2.zeroPad("" + jDate, 3);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        Calendar2.newGCalendarZulu(year, jDate).getTimeInMillis()/1000));
                String2.log(yj + " " + daysSince);      
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH3ncml/" +
                        varDirNames[var] + "/ncml1day/V" + yj + ".ncml"));
                    w.write(
/* C:/content/scripts/VH3ncml/chla/ncml1day/V2014365.ncml is
<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V2014365.L3m_DAY_NPP_CHL_chlor_a_4km' coordValue='16435'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj + ".L3m_DAY_SNPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH3 8day .ncml files. 
     * coordValue is firstDay. First 3 of 2012 are 15340, 15348, 15356. 
     * Every year, start again Jan 1-8, 9-16, ...
     * End of year, 
     */
    public static void makeVH38dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int day1 = 1; day1 <= nDays; day1 += 8) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, day1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, 
                    Math.min(nDays, day1+7));
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                if (day1+7 > nDays)
                    daysSince--; //imperfect
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH3ncml/" +
                        varDirNames[var] + "/ncml8day/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VH3ncml/chla/ncml8day/V20120012012008.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012008.L3m_8D_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_8D_SNPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }
    
    /** Make VH3 mday .ncml files. 
     * coordValue is firstDay of the month. First 3 of 2012 are 15340, 15371, 15400. 
     */
    public static void makeVH3mdayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, month, 1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, month+1, 0);
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH3ncml/" +
                        varDirNames[var] + "/ncmlmon/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VH3ncml/chla/ncmlmon/V20120012012031.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf>
 */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_MO_SNPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH2018 1day .ncml files. */
    public static void makeVH20181dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int jDate = 1; jDate <= nDays; jDate++) {
                String yj = year + String2.zeroPad("" + jDate, 3);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        Calendar2.newGCalendarZulu(year, jDate).getTimeInMillis()/1000));
                String2.log(yj + " " + daysSince);      
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH2018ncml/" +
                        varDirNames[var] + "/ncml1day/V" + yj + ".ncml"));
                    w.write(
/* C:/content/scripts/VH2018ncml/chla/ncml1day/V2014365.ncml is
<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V2014365.L3m_DAY_NPP_CHL_chlor_a_4km' coordValue='16435'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj + ".L3m_DAY_SNPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }

    /** Make VH2018 8day .ncml files. 
     * coordValue is firstDay. First 3 of 2012 are 15340, 15348, 15356. 
     * Every year, start again Jan 1-8, 9-16, ...
     * End of year, 
     */
    public static void makeVH20188dayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            int nDays = year % 4 == 0? 366 : 365;
            for (int day1 = 1; day1 <= nDays; day1 += 8) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, day1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, 
                    Math.min(nDays, day1+7));
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                if (day1+7 > nDays)
                    daysSince--; //imperfect
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH2018ncml/" +
                        varDirNames[var] + "/ncml8day/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VH2018ncml/chla/ncml8day/V20120012012008.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012008.L3m_8D_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf> */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_8D_SNPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }
    
    /** Make VH2018 mday .ncml files. 
     * coordValue is firstDay of the month. First 3 of 2012 are 15340, 15371, 15400. 
     */
    public static void makeVH2018mdayNcmlFiles(int startYear, int endYear) throws Throwable {
        String varDirNames[] = new String[]{
            "chla",       "k490",        "r671",       "par",    "pic",    "poc"};
        String jplFileNames[]    = new String[]{
            "CHL_chlor_a","KD490_Kd_490","RRS_Rrs_671","PAR_par","PIC_pic","POC_poc"};
        String jplVarNames[]    = new String[]{
            "chlor_a",    "Kd_490",      "Rrs_671",    "par",    "pic",    "poc"};

        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                GregorianCalendar firstDay = Calendar2.newGCalendarZulu(year, month, 1);
                GregorianCalendar lastDay  = Calendar2.newGCalendarZulu(year, month+1, 0);
                String yj1 = Calendar2.formatAsYYYYDDD(firstDay);
                String yj2 = Calendar2.formatAsYYYYDDD(lastDay);
                int daysSince = Math2.roundToInt(
                    Calendar2.epochSecondsToUnitsSince(0, Calendar2.SECONDS_PER_DAY,
                        firstDay.getTimeInMillis()/1000));
                String2.log(yj1 + " " + yj2 + " " + daysSince);
                for (int var = 0; var < varDirNames.length; var++) {
                    Writer w = new BufferedWriter(new FileWriter("/content/scripts/VH2018ncml/" +
                        varDirNames[var] + "/ncmlmon/V" + yj1 + yj2 + ".ncml"));
                    w.write(
/* C:/content/scripts/VH2018ncml/chla/ncmlmon/V20120012012031.ncml is
 <netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>
   <variable name='time' type='int' shape='time' />
   <aggregation dimName='time' type='joinNew'>
     <variableAgg name='l3m_data'/>
     <netcdf location='V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km' coordValue='15340'/>
   </aggregation>
 </netcdf>
 */
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <variable name='time' type='int' shape='time' />\n" +
"  <aggregation dimName='time' type='joinNew'>\n" +
"    <variableAgg name='" + jplVarNames[var] + "'/>\n" +
"    <netcdf location='V" + yj1 + yj2 + ".L3m_MO_SNPP_" + jplFileNames[var] + "_4km.nc' " +
            "coordValue='" + daysSince + "'/>\n" +
"  </aggregation>\n" +
"</netcdf>\n");
                    w.close();
                }
            }
        }
    }


    /** Extract sonar initial lat,lon values.
     * 1) original csv file (from xls)
     * 2) In local ERDDAP, save all of nceiNmfsSonarAscii as nceiNmfsSonarOriginal.nc
     * 3) This creates latitude and longitude columns with initial point from shape var.
     * 4) This saves as nceiNmfsSonar.nc
     */
    public static void extractSonarLatLon() throws Throwable {
        String2.log("\n*** Projects.extractSonarLatLon");
        String dir = "/u00/data/points/sonar/";
        String fiName = "nceiNmfsSonarOriginal.nc";
        String foName = "nceiNmfsSonar.nc";

        Table table = new Table();
        table.readFlatNc(dir + fiName, null, 0); //standardizeWhat=0
        int nRows = table.nRows();
        String2.log("original nRows=" + nRows + " colNames=" + table.getColumnNamesCSSVString());

        //get shape. make lat, lon.
        int shapeCol = table.findColumnNumber("shape");
        StringArray shape = (StringArray)table.getColumn(shapeCol);
        DoubleArray lat = new DoubleArray();
        DoubleArray lon = new DoubleArray();
        table.addColumn(shapeCol, "latitude", lat, 
            (new Attributes()).add("units", "degrees_north").add("long_name", "Initial Latitude"));
        table.addColumn(shapeCol, "longitude", lon, 
            (new Attributes()).add("units", "degrees_east").add("long_name", "Initial Longitude"));

        //convert fileName "D20130716-T034047.raw" to more precise epochSeconds time
        //(Original time is just the date.)
        //??? IS THAT RIGHT??? Won't there be conflicts from different cruises???
        Pattern fileNamePattern = Pattern.compile("D(\\d{8})-T(\\d{6})\\.raw");
        PrimitiveArray fileNamePA = table.findColumn("file_name");
        int dateCol               = table.findColumnNumber("time");
        table.setColumnName(dateCol, "date");
        PrimitiveArray datePA     = table.getColumn(dateCol);
        PrimitiveArray timePA     = (DoubleArray)datePA.clone();
        table.addColumn(dateCol, "time", timePA, 
            (new Attributes()).add("units", "seconds since 1970-01-01T00:00:00Z").add("long_name", "Start Time"));

        for (int row = 0; row < nRows; row++) {
            //generate lon and lat
            String[] shapeAr = StringArray.arrayFromCSV(shape.get(row));
            lon.add(shapeAr.length >= 2? 
                Math2.roundTo(String2.parseDouble(shapeAr[0]), 7) : Double.NaN);
            lat.add(shapeAr.length >= 2? 
                Math2.roundTo(String2.parseDouble(shapeAr[1]), 7) : Double.NaN);

            //convert fileName "D20130716-T034047.raw" to a more precise time
            //  if it matches the regex
            String tfn = fileNamePA.getString(row);
            try {
                Matcher matcher = fileNamePattern.matcher(tfn);
                if (matcher.matches()) {
                    timePA.setDouble(row, 
                        Calendar2.gcToEpochSeconds(Calendar2.parseCompactDateTimeZulu(
                            matcher.group(1) + matcher.group(2))));
                } else {
                    String2.log("unmatched fileName["+row+"]=" + tfn);
                }
            } catch (Throwable t) {
                String2.log("Exception for fileName["+row+"]=" + tfn + "\n" +
                    t.toString());
            }
        }
        table.saveAsFlatNc(dir + foName, "row");
        String2.log("new colNames=" + table.getColumnNamesCSSVString());
        String2.log("\n*** Projects.extractSonarLatLon finished successfully. Created:\n" +
            dir + foName);
    }

    /**
     * This makes ncml files for PH2.
     *
     * @param sstdn  sstd or sstn
     * @throws Exception if trouble
     */
    public static void makePH2Ncml(String sstdn)
        throws Exception {
        String2.log("*** Projects.makePH2Ncml(" + sstdn + ")");

        //get list of filenames (without dir)
        //19811101145206-NODC-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.2_NOAA07_G_1981305_day-v02.0-fv01.0.nc
        String dir = "/u00/satellite/PH2/" + sstdn + "/1day/ncml/";
        String regex = "\\d{14}-NODC.*\\.nc";

        //String names[] = RegexFilenameFilter.list(dir, regex);
        ArrayList<String> names = String2.readLinesFromFile(
            "/u00/satellite/PH2/" + sstdn + "/names.txt", null, 1);

        //for each file
        int nNames = names.size();
        for (int i = 0; i < nNames; i++) {

            //extract date yyyyddd
            String tName = names.get(i);
//19811101145206-NODC-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.2_NOAA07_G_1981305_day-v02.0-fv01.0.nc
            double epochSeconds = 
                (Calendar2.parseYYYYDDDZulu(tName.substring(72, 79)).getTimeInMillis() /
                1000.0) + 12 * Calendar2.SECONDS_PER_HOUR; //center on noon of that day

            String2.log("writing " + dir + tName + ".ncml " + epochSeconds);
            Writer writer = new BufferedWriter(new FileWriter(dir + tName + ".ncml"));
            StringBuilder values = new StringBuilder();
            writer.write(
"<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'\n" +
"  location=\"" + tName + "\">\n" +
"  <variable name=\"time\">\n" +
"    <attribute name='units' value='seconds since 1970-01-01T00:00:00Z' />\n" +
"    <values>" + epochSeconds + "</values>\n" +
"  </variable>\n" +
"</netcdf>\n");
            writer.close();
        }
    }

    /**
     * This makes ncml files for PH53.
     *
     * @param dayMode
     * @throws Exception if trouble
     */
    public static void makePH53Ncml()
        throws Exception {
        String2.log("*** Projects.makePH53Ncml()");
        String dir = "/u00/satellite/PH53/";
        String dayNight[] = {"day", "night"};
        String ncmlDir = "/u00/satellite/PH53Ncml/";
        File2.makeDirectory(ncmlDir);

        //get list of 1day filenames:  
        //  cd /u00/satellite/PH53/1day/2019
        //  find data -name *day*.nc > daynames2019.txt  
        //  find data -name *night*.nc > nightnames2019.txt  
        //copy to laptop /u00/satellite/PH53Ncml
        //
        //source is ftp://ftp.nodc.noaa.gov/pub/data.nodc/pathfinder/Version5.3/L3C/2018/data/
        //2019-02-21: there is no 2018081_day file, or 2018023_night file

        for (int year = 2019; year <= 2019; year++) {
            for (int daynight = 0; daynight < 2; daynight++) {

                ArrayList<String> names = String2.readLinesFromFile(
                    ncmlDir + dayNight[daynight] + "names" + year + ".txt", null, 1);
                HashMap<String,String> hm = new HashMap();
                //for each file
                //19811101145206-NODC-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.2_NOAA07_G_1981305_day-v02.0-fv01.0.nc
                int nNames = names.size();
                for (int i = 0; i < nNames; i++) {

                    //extract date yyyyddd
                    String tName = File2.getNameAndExtension(names.get(i));
                    String date = String2.extractCaptureGroup(tName, "_(\\d{7})_", 1);
                    if (date == null)
                        continue;
        //19810825023019-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA07_G_1981237_night-v02.0-fv01.0.nc
                    int epochSeconds = Math2.roundToInt(
                        (Calendar2.parseYYYYDDDZulu(date).getTimeInMillis() /
                        1000.0) + 12 * Calendar2.SECONDS_PER_HOUR); //center on noon of that day
                    String oldName = hm.put("" + epochSeconds, tName);
                    if (oldName != null) 
                        throw new RuntimeException("duplicate epSec=" +epochSeconds + 
                           "\n" + oldName +
                           "\n" + tName);

                    String2.log("writing " + ncmlDir + dayNight[daynight] + "Ncml/" +tName + ".ncml " + epochSeconds);
                    Writer writer = new BufferedWriter(new FileWriter(ncmlDir + dayNight[daynight] + "Ncml/" +tName + ".ncml"));
                    try {
                        writer.write(
        "<netcdf xmlns='https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'\n" +
        "  location=\"" + tName + "\">\n" +
        "  <variable name=\"time\">\n" +
        "    <attribute name='units' value='seconds since 1970-01-01T00:00:00Z' />\n" +
        "    <values>" + epochSeconds + "</values>\n" +
        "  </variable>\n" +
        "</netcdf>\n");
                    } finally {
                        writer.close();
                    }
                }
            }
        }
    }

    public static void tallyGridValues(String fileName, String varName,
        double scale) throws Exception {

        String2.log("\n*** Projects.tallyGridValues(\n" + 
            fileName + "\n" + 
            varName + " scale=" + scale);
        NetcdfFile file = NcHelper.openFile(fileName);
        Variable var = file.findVariable(varName);
        PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
        int n = pa.size();
        Tally tally = new Tally();
        for (int i = 0; i < n; i++)
            tally.add("value", "" + Math2.roundToInt(pa.getDouble(i) * scale));
        String2.log(tally.toString());
    }

    /** 
     * This un-gzips a bunch of files that don't have the extension .gz but should.
     */
    public static void unGz(String dirName, String fileNameRegex) throws Exception {

        String2.log("\n*** Projects.unGz(" + dirName + ", " + fileNameRegex + ")");

        //get the list of file names
        String[] fileNames = RegexFilenameFilter.list(dirName, fileNameRegex);

        //process each file
        int nFail = 0;
        for (int i = 0; i < fileNames.length; i++) {
            String tName = fileNames[i];
            String2.log(tName);
            InputStream is  = null;
            OutputStream os = null;
            try {
                //rename to .gz
                File2.rename(dirName, tName, tName + ".gz");

                //unGz
                is = File2.getDecompressedBufferedInputStream(     dirName + tName + ".gz");
                os = new BufferedOutputStream(new FileOutputStream(dirName + tName));
                File2.copy(is, os);
                is.close();
                os.close();
                File2.delete(dirName + tName + ".gz");
            } catch (Exception e) {
                nFail++;
                String2.log("* Exception for #" + i + "=" + dirName + tName + "\n" +
                    MustBe.throwableToString(e));
                try {
                    if (is != null) is.close(); 
                } catch (Exception e2) {
                    String2.log(MustBe.throwableToString(e2));
                }
                try {
                    if (os != null) os.close(); 
                } catch (Exception e2) {
                    String2.log(MustBe.throwableToString(e2));
                }
                try {
                    File2.rename(dirName, tName + ".gz", tName);
                } catch (Exception e2) {
                    String2.log(MustBe.throwableToString(e2));
                }
            }
        }                
        String2.log("\n*** Projects.unGz finished. nTry=" + fileNames.length + " nFail=" + nFail);
    }

    /** This is a 2020-04-10 test that NetcdfFileWriter.abort() succeeds and closes the file. */
    public static void testNcAbort() throws Exception {

        String fullName = "/downloads/testNcAbort.nc";

        //delete file if it already exists
        File file = new File(fullName);
        if (file.exists()) {
            System.out.println("result of initial file.delete()=" + file.delete());  
            System.out.println("file.exists=" + file.exists());  
        }
        file = null; //so this isn't excuse for file not being deleted later

        //open the file (before 'try'); if it fails, no temp file to delete
        System.out.println("createNew file=" + fullName);  
        NetcdfFileWriter nc = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, fullName);
        
        try {
            Group rootGroup = nc.addGroup(null, "");
            nc.setFill(false);

            //define the dimensions
            int nX = 3;
            Dimension aDimension  = nc.addDimension(rootGroup, "x", nX);

            //add the variables
            Variable var = nc.addVariable(rootGroup, "var0", DataType.BYTE, Arrays.asList(aDimension)); 

            rootGroup.addAttribute(new ucar.nc2.Attribute("testAttribute", 
                Array.factory(DataType.UBYTE, new int[]{4}, new byte[]{2,4,6,8})));

            nc.create();

            Array ar = Array.factory(DataType.UBYTE, new int[]{nX}, new byte[]{1,2,3});
            nc.write(var, ar);

            //if close throws exception, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately
            nc = null;

            System.out.println("shouldn't get here");

        } catch (Exception e) {
            System.out.println("caught Exception e (before abort):");
            e.printStackTrace();
            try {
                if (nc != null) { 
                    nc.abort(); 
                    nc = null; //so this isn't excuse for file not being deleted later
                }
            } catch (Exception e2) {
                System.out.println("caught Exception e2 (caused by abort):");
                e2.printStackTrace();
            }

            //delete the partial file
            file = new File(fullName);
            if (file.exists())
                System.out.println("result of file.delete()=" + file.delete());  

            System.out.println("file still exists=" + file.exists());  

            throw e;

        }
    }



    /**
     * This converts a NOAA Passive Acoustic Spectrum csv file into a .nc file.
     */
    public static void acousticCsvToNc(String sourceFullName, String destFullName) throws Exception {

        //read the csv
        Table table = new Table();
        table.readASCII(sourceFullName, String2.ISO_8859_1,
            "", "", 0, 1, ",",
            null, null, null, null, true);  //simplify
        String2.log("acousticCsvToNc " + sourceFullName + "\n" +
            table.toString(3));

/*
        int po = fileNames[fn].lastIndexOf('_');
        int year  = String2.parseInt(fileNames[fn].substring(po + 1, po + 5));
        int month = String2.parseInt(fileNames[fn].substring(po + 5, po + 7)); //1..
        int months = (year - 1950) * 12 + month - 1;
//if (year < 2007) continue;
//if (year == 2007 && month < 2) continue;
        String2.log("  year=" + year + " month=" + month + " monthsSinceJan1950=" + months);
*/

        //open the new file
        NetcdfFileWriter newFile = NetcdfFileWriter.createNew(
            NetcdfFileWriter.Version.netcdf3, destFullName);
        try {
            boolean nc3Mode = true;
            DataType dataType = DataType.DOUBLE;
            Group rootGroup = newFile.addGroup(null, "");
            String cDate = Calendar2.getCurrentISODateStringZulu();

            int nRows = table.nRows();
            int nCols = table.nColumns();
            int nCols1 = nCols - 1;
            ArrayList<Dimension> dimensions = new ArrayList();
            Attributes atts; 
            Variable newVars[] = new Variable[3];
            Array array[] = new Array[3];

            //create the time dimension and variable
            //??? what is the meaning of the column name, e.g., 1213?
            Dimension timeDimension = newFile.addDimension(rootGroup, "timeStamp", nRows);
            dimensions.clear();
            dimensions.add(timeDimension);
            PrimitiveArray pa = table.getColumn(0);
            array[0] = NcHelper.get1DArray(pa);
            atts = new Attributes(); 
            //atts.add("_CoordinateAxisType", "Lon");
            atts.add("actual_range", new DoubleArray(new double[]{pa.getDouble(0), pa.getDouble(nRows-1)}));
            atts.add("axis", "T");
            atts.add("long_name", "Time Stamp");
            atts.add("standard_name", "time");
            atts.add("units", "seconds");
            newVars[0] = newFile.addVariable(rootGroup, "timeStamp", dataType, dimensions); 
            NcHelper.setAttributes(nc3Mode, newVars[0], atts, NcHelper.isUnsigned(dataType));

            //create the freq dimension
            Dimension freqDimension = newFile.addDimension(rootGroup, "frequency", nCols1);
            dimensions.clear();
            dimensions.add(freqDimension);
            DoubleArray da = new DoubleArray();
            for (int col = 1; col < nCols; col++)
                da.addDouble(String2.parseDouble(table.getColumnName(col)));           
            array[1] = NcHelper.get1DArray(da);
            atts = new Attributes(); 
            //atts.add("_CoordinateAxisType", "Lon");
            atts.add("actual_range", new DoubleArray(new double[]{da.get(0), da.get(da.size() - 1)}));
            atts.add("comment", "The frequency associated with a 1/3 octave band.");
            atts.add("long_name", "frequency");
            atts.add("standard_name", "sound_frequency");
            atts.add("units", "s-1");
            newVars[1] = newFile.addVariable(rootGroup, "frequency", dataType, dimensions); 
            NcHelper.setAttributes(nc3Mode, newVars[1], atts, NcHelper.isUnsigned(dataType));

            //create the acoustic variable
            dimensions.clear();
            dimensions.add(timeDimension);
            dimensions.add(freqDimension);
            da = new DoubleArray(nCols1 * nRows, false);
            for (int col = 1; col < nCols; col++)
                for (int row = 0; row < nRows; row++)
                    da.add(table.getDoubleData(col, row));
            array[2] = NcHelper.get1DArray(da);
            array[2] = array[2].reshape(new int[]{nRows, nCols1});
            da = null;
            atts = new Attributes(); 
            atts.add("comment", "This is the acoustic energy in this 1/3 octave band.");
            atts.add("long_name", "Acoustic Amplitude");
            atts.add("standard_name", "sound_intensity_level_in_water");
            atts.add("units", "dB");
            newVars[2] = newFile.addVariable(rootGroup, "acoustic", dataType, dimensions); 
            NcHelper.setAttributes(nc3Mode, newVars[2], atts, NcHelper.isUnsigned(dataType));

            //define newVar in new file

            //define GLOBAL metadata 
            Attributes gatts = new Attributes();
            gatts.add("acknowledgement", "This project received funding from the U.S. Navy."); 
            gatts.add("cdm_data_type", "Grid");
            gatts.add("comment", "This metadata is a placeholder for future data submissions. " +
                "This information is not 100&#37; accurate and will change.");
            gatts.add("contributor_name", "U.S. Navy");
            gatts.add("contributor_role", "funding");
            gatts.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
            gatts.add("creator_email", "carrie.wall@noaa.gov");
            gatts.add("creator_name", "Carrie Wall");
            gatts.add("creator_type", "person");
            gatts.add("creator_url", "https://www.ngdc.noaa.gov/mgg/pad/");
            gatts.add("date_created", cDate);
            gatts.add("date_issued", cDate);
            gatts.add("history", "Raw acoustic data files are retrieved from stationary acoustic " +
                "recorders every 6 months and then processed in the respective NOAA NMFS lab. " +
                "The raw acoustic data and processed data files are sent to NCEI for archive. " +
                "The processed data files are then sent to NOAA NMFS SWFSC ERD for enhanced access.\n" +
                cDate + " NOAA NMFS SWFSC ERD (erd.data@noaa.gov) converted from .csv to .nc and added metadata and time dimension");
            gatts.add("id", "NOAA-NAVY-StetsonSP1"); 
            gatts.add("infoUrl", "https://www.ngdc.noaa.gov/mgg/pad/");
            gatts.add("institution", "NOAA NCEI"); 
            gatts.add("keywords", "acoustic, agreement, centers, data, environmental, " +
                "information, monitory, national, navy, ncei, nesdis, noaa, noaa-navy, passive, " +
                "settlement, surtass, " +
                "Earth Science &gt; Oceans &gt; Ocean Acoustics &gt; Acoustic Frequency, " +
                "Earth Science &gt; Oceans &gt; Ocean Acoustics &gt; Acoustic Attenuation/Transmission");
            gatts.add("keywords_vocabulary", "GCMD Science Keywords");
            gatts.add("license", "The data may be used and redistributed for free " +
                "but is not intended for legal use, since it may contain inaccuracies. " +
                "Neither the data Creator, NOAA, nor the United States Government, " +
                "nor any of their employees or contractors, makes any warranty, express or implied, " +
                "including warranties of merchantability and fitness for a particular purpose, " +
                "or assumes any legal liability for the accuracy, completeness, or usefulness, " +
                "of this information.");
            gatts.add("naming_authority", "NOAA-NAVY");
            gatts.add("product_version", "2019");
            gatts.add("project", "NOAA Passive Acoustic Data");
            //gatts.add("references", "...");
            //gatts.add("platform", "");
            gatts.add("sensor", "hydrophone"); 
            //gatts.add("source", "");
            gatts.add("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
            gatts.add("summary", 
                "Passive acoustic data collected over 5 years across numerous national marine sanctuaries and monuments.");
            gatts.add("title", 
                "NOAA-Navy Surtass Settlement Agreement Passive Acoustic Monitory"); //?surtass misleading?
            //set the globalAttributes
            NcHelper.setAttributes(nc3Mode, rootGroup, gatts);
        
            //leave define mode
            newFile.create();

            //write data for each variable
            for (int v = 0; v < 3; v++) {
                Variable var = newVars[v];
                newFile.write(newVars[v], array[v]);
            }
            newFile.close();
            newFile = null;

        } finally {
            try {if (newFile != null) newFile.abort(); } catch (Exception e) {}
        }
    }


    /** 
     * If the String is surrounded by ', this removes them,
     *  else if the String is surrounded by ", this returns fromJson(s), else it returns s.
     */
    public static String unquoteYaml(String s) {
        if (s == null || s.length() < 2) 
            return s;
        if (s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"')
            return String2.fromJson(s);
        if (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'')
            return s.substring(1, s.length() - 1);
        return s;
    } 

    /** This generates ERDDAP datasets from all the dataset descriptions in 
     * /c/git/open-data-registry/datasets/ .
     * Cloned from https://github.com/awslabs/open-data-registry/
     * Last cloned on 2019-08-27
     *
     * @throws Exception if trouble
     */
    public static String makeAwsS3FilesDatasets(String fileNameRegex) throws Exception {

        String today = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);

        Table table = FileVisitorDNLS.oneStep("/git/open-data-registry/datasets/", 
            ".*.yaml", false, ".*", false); //fileNameRegex, tRecursive, pathRegex, dirToo
        StringArray dirs  = (StringArray)table.getColumn(0);
        StringArray names = (StringArray)table.getColumn(1);
        StringBuilder sb     = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        HashSet ignoredTags = new HashSet();
        int nFiles = dirs.size();
        HashSet bucketsAlreadyDone = new HashSet();
        String skipBuckets[] = new String[]{
            //skip because too many files in initial directory
            "aws-earth-mo-atmospheric-ukv-prd",
            "aws-earth-mo-atmospheric-global-prd", 
            "aws-earth-mo-atmospheric-mogreps-uk-prd",
            "goesingest",
            "irs-form-990", 
            "mogreps-g", 
            "mogreps-uk", 
            "ngi-igenomes",
            "nrel-pds-hsds", 
            //not reported: I'm not sure where to report  (owner?)
            "data.geo.admin.ch",   //2019-08-28  access denied  403
            "gcgrid",              //2019-08-28  access denied  403
            "hcp-openaccess",      //2019-08-28  access denied  403
            "icgc",                //2019-08-28  access denied  403
            "mimic-iii-physionet", //2019-08-28  access denied  403
            "tcga",                //2019-08-28  access denied  403
            "physionet-pds",       //2019-08-28  no content

            "zinc3d"};             //RequesterPays (not noted in names)
        for (int f = 0; f < nFiles; f++) {
            if (!names.get(f).matches(fileNameRegex))
                continue;
            String2.log(dirs.get(f) + "   " + names.get(f));
            ArrayList<String> lines = String2.readLinesFromFile(dirs.get(f) + names.get(f), String2.UTF_8, 1);
            int nLines = lines.size();
            String name = null, deprecated = null, description = null, documentation = null, contact = null,
                managedBy = null, updateFrequency = null, license = null,
                name2 = null, bucket = null, region = null;
            HashSet keywords = new HashSet();
            EDD.chopUpCsvAndAdd("AWS, bucket, data, file, lastModified, names, S3", 
                keywords);
            int line = 0; //next line to be read
            while (line < nLines) {
                String tl = lines.get(line++);
                //get rid of bom marker #65279 in some utf files.
                if (line == 1 && tl.length() > 0 && tl.charAt(0) == '\uFEFF')
                    tl = tl.substring(1);
                //String2.log(">> tl =" + String2.annotatedString(tl));
                if      (tl.startsWith("#"))
                    continue;
                else if (tl.startsWith("Name: ")) {          name            = unquoteYaml(tl.substring( 6).trim());
                    EDD.chopUpAndAdd(name, keywords);
                }
                else if (tl.startsWith("Description: ")) {   description     = tl.substring(13).trim();
                    if (description.length() == 1 && (description.equals("|") || description.equals(">"))) {
                        String spacer = " "; 
                        description = "";
                        tl = line >= nLines? "" : lines.get(line++);
                        while (tl.startsWith("  ")) {
                            description += (description.length() == 0? "" : spacer) + tl.trim();
                            tl = line >= nLines? "" : lines.get(line++);
                        }
                        if (line < nLines) line--; //back up a line
                    }
                    description = unquoteYaml(description);
                }
                else if (tl.startsWith("Deprecated: "))      deprecated      = tl.substring(12).trim();
                else if (tl.startsWith("Documentation: "))   documentation   = unquoteYaml(tl.substring(15).trim());
                else if (tl.startsWith("Contact: "))         contact         = unquoteYaml(tl.substring( 9).trim());
                else if (tl.startsWith("ManagedBy: "))       managedBy       = unquoteYaml(tl.substring(11).trim());
                else if (tl.startsWith("UpdateFrequency: ")) updateFrequency = unquoteYaml(tl.substring(17).trim());
                else if (tl.startsWith("Tags:")) {
                    tl = line >= nLines? "" : lines.get(line++);
                    while (tl.startsWith("  - ")) {
                        EDD.addAllAndParts(tl.substring(4), keywords);
                        tl = line >= nLines? "" : lines.get(line++);
                    }
                    if (line < nLines) line--; //back up a line
                }
                else if (tl.startsWith("License: ")) {       license         = tl.substring( 9).trim();
                    if (license.length() == 1 && (license.equals("|") || license.equals(">"))) {
                        String spacer = " "; 
                        license = "";
                        tl = line >= nLines? "" : lines.get(line++);
                        while (tl.startsWith("  ")) {
                            license += (license.length() == 0? "" : spacer) + tl.trim();
                            tl = line >= nLines? "" : lines.get(line++);
                        }
                        if (line < nLines) line--; //back up a line
                    }
                    license = unquoteYaml(license);
                }
                else if (tl.startsWith("DataAtWork:")) { //skip all content
                    tl = line >= nLines? "" : lines.get(line++);
                    while (tl.startsWith("  ")) {
                        tl = line >= nLines? "" : lines.get(line++);
                    }
                    if (line < nLines) line--; //back up a line
                }
                else if (tl.startsWith("Resources:")) {
                    EDD.cleanSuggestedKeywords(keywords);
                    tl = line >= nLines? "" : lines.get(line++);
                    while (tl.startsWith("  ")) {
                        //String2.log(">> tl2=" + tl);
                        if      (tl.startsWith("  - Description: ")) {name2  = tl.substring(17).trim();
                            if (name2.length() == 1 && (name2.equals("|") || name2.equals(">"))) {
                                String spacer = " "; 
                                name2 = "";
                                tl = line >= nLines? "" : lines.get(line++);
                                while (tl.startsWith("  ")) {
                                    name2 += (name2.length() == 0? "" : spacer) + tl.trim();
                                    tl = line >= nLines? "" : lines.get(line++);
                                }
                                if (line < nLines) line--; //back up a line
                            }
                            name2 = unquoteYaml(name2);
                        }
                        else if (tl.startsWith("    ARN: "))          bucket = tl.substring( 9).trim();
                        else if (tl.startsWith("    Region: "))       region = tl.substring(12).trim();
                        else if (tl.startsWith("    RequesterPays: ")) {} //occurs after Type line. [Requester Pays] is caught via name2 below.
                        else if (tl.startsWith("    Type: S3 Bucket")) {
try {
                            int po = bucket.lastIndexOf(':');
                            if (po > 0)
                                bucket = bucket.substring(po + 1);

                            //skipBucket?             (bucket+prefix)
                            if (String2.indexOf(skipBuckets, bucket) >= 0) {
                                sb.append("<!-- skipping bucket+prefix because on skip list: " + bucket + " -->\n\n"); 
                                tl = line >= nLines? "" : lines.get(line++);
                                continue;
                            }

                            //bucketsAlreadyDone        (bucket+prefix)
                            if (!bucketsAlreadyDone.add(bucket)) {
                                sb.append("<!-- skipping bucket+prefix because already done: " + bucket + " -->\n\n"); 
                                tl = line >= nLines? "" : lines.get(line++);
                                continue;
                            }

                            //prefix?
                            String prefix = "";
                            po = bucket.indexOf('/');
                            if (po > 0) {
                                prefix = File2.addSlash(bucket).substring(po + 1); //ensure it ends in /
                                bucket = bucket.substring(0, po);
                            }

                            //I asked owner to fix error in yaml, which has us-east-1
                            if (bucket.equals("giab") ||
                                bucket.equals("human-pangenomics"))
                                region = "us-west-2";

String2.log(">> name=" + name + " name2=" + name2);
if ( name.indexOf("Requester Pays") >= 0 ||
    name2.indexOf("Requester Pays") >= 0)
    sb.append("<!-- " + XML.encodeAsXML("No dataset for " + names.get(f) + 
        " bucket=" + bucket + " prefix=" + prefix + 
        "\nname2=" + String2.toJson(name2) + " because [Requester Pays].") + " -->\n\n");

else sb.append(
"<dataset type=\"EDDTableFromFileNames\" datasetID=\"" + 
    String2.modifyToBeVariableNameSafe("awsS3Files_" + bucket + (prefix.length() == 0? "" : "_" + prefix)) + 
    "\" active=\"true\">\n" +
"    <fileDir>***fromOnTheFly, https://" + bucket + ".s3." + region + ".amazonaws.com/" + prefix + "</fileDir>\n" +
"    <fileNameRegex>.*</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <reloadEveryNMinutes>" + (10080 + Math2.random(1000)) + "</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
(contact == null? "" : 
"        <att name=\"contact\">" + XML.encodeAsXML(contact) + "</att>\n") +
"        <att name=\"creator_email\">" +
    (String2.isEmailAddress(contact)? XML.encodeAsXML(contact) : "null") +
    "</att>\n" +
"        <att name=\"creator_name\">null</att>\n" +
"        <att name=\"creator_url\">null</att>\n" +
"        <att name=\"history\">" + today + " erd.data@noaa.gov created ERDDAP metadata from " + names.get(f) + "</att>\n" +
"        <att name=\"infoUrl\">https://registry.opendata.aws/" + File2.getNameNoExtension(names.get(f)) + "/</att>\n" +
"        <att name=\"institution\">Amazon Web Services</att>\n" +
"        <att name=\"keywords\">" + XML.encodeAsXML(String2.toCSSVString(keywords)) + "</att>\n" +
"        <att name=\"license\">" +
    (license == null? "[standard]" : XML.encodeAsXML(license)) +
    "</att>\n" +
"        <att name=\"sourceUrl\">https://" + bucket + ".s3." + region + ".amazonaws.com/</att>\n" + //no prefix
"        <att name=\"summary\">This dataset has file information from the AWS S3 " + 
XML.encodeAsXML(
bucket + " bucket at https://" + bucket + ".s3." + region + ".amazonaws.com/" + 
(prefix.length() > 0? " with prefix=" + prefix : "") + 
" . " +
"Use ERDDAP's \"files\" system for this dataset to browse and download the files. " +
"The \"files\" information for this dataset is always perfectly up-to-date because ERDDAP gets it on-the-fly. " +
"AWS S3 doesn't offer a simple way to browse the files in their public, Open Data buckets. This dataset is a solution to that problem for this bucket.\n" +
"\n" +
(name            == null? "" : "Name: "            + name            + "\n") +
(name2           == null? "" : "Name2: "           + name2           + "\n") + 
(deprecated      == null? "" : "Deprecated: "      + deprecated      + "\n") + 
(description     == null? "" : "Description: "     + description     + "\n\n") +
(documentation   == null? "" : "Documentation: "   + documentation   + "\n") +
(contact         == null? "" : "Contact: "         + contact         + "\n") +
(managedBy       == null? "" : "ManagedBy: "       + managedBy       + "\n") +
(updateFrequency == null? "" : "UpdateFrequency: " + updateFrequency + "\n")) + // ) is end of XML.encodeAsXML(
"</att>\n" +
"        <att name=\"title\">File Names from the AWS S3 " + bucket + " Bucket" +
XML.encodeAsXML(
    (prefix.length() > 0? " with prefix=" + prefix : "") +
    (name  == null? "" : ": " + name) +
    (name2 == null || String2.isRemote(name2)? "" : ": " + name2)) +
    "</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>url</sourceName>\n" +
"        <destinationName>url</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">URL</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>name</sourceName>\n" +
"        <destinationName>name</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">File Name</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lastModified</sourceName>\n" +
"        <destinationName>lastModified</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Last Modified</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>size</sourceName>\n" +
"        <destinationName>size</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"            <att name=\"long_name\">Size</att>\n" +
"            <att name=\"units\">bytes</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>fileType</sourceName>\n" +
"        <destinationName>fileType</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">.*(\\..+?)</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">File Type</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n");
} catch (Exception e) {
    errors.append("ERROR for " + names.get(f) + ":\n" + MustBe.throwableToString(e) + "\n");
}
                        } //end of S3 bucket chunk
                        tl = line >= nLines? "" : lines.get(line++);
                        } //end of "  " lines loop
                    if (line < nLines) line--; //back up a line
                    }  //end of resource lines loop
                else { 
                  int po = tl.indexOf(':');
                  if (po > 0 && tl.charAt(0) != ' ')
                      ignoredTags.add(tl.substring(0, po+1));
                  }
                } //end of for line loop
            } //end of for file loop
            String2.log(sb.toString());
            String2.log("\nmakeAwsS3FilesDatasets finished nFiles=" + nFiles + ".\n" +
                "ERRORS: " + errors.toString() + "\n" +
                "IgnoredTags: " + String2.toCSSVString(ignoredTags) + "\n");

            return sb.toString();

        }


}

