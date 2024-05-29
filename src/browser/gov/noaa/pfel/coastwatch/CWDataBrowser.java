/* 
 * CWDataBrowser Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.ema.*;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.File;
import java.text.DecimalFormat; 
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This creates an HTML page which lets users pick a CoastWatch 
 * data file from categories and lists of existing files.
 *
 * <p>Note that "/" is always used as the file.separator, even on Windows 
 * computers. (Java supports this.)
 *
 * <p>This creates a series of log files in the same directory as this
 *  .java file, named CWDataBrowser.log.x, where x is 0 - 9.
 *
 * <p>The statistics on the most requested .gif files are
 * misleading because they include .gifs selected on the way to selecting 
 * the desired data. For example, the default settings will probably be
 * the most requested .gif but perhaps not the most frequent end point.
 *
 * <p>!!!!BEFORE DEPLOYMENT, set the proper dataDirectory and dataServer
 * in CWDataBrowser.properties.
 *
 * <p>!!!!Never change the .properties file in the directory of a running
 * version of this program without immediately restarting this program.
 * Otherwise, the data gathered by the periodic reset thread may
 * be inconsistent with this class's properties info.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 *
 * <p>Changes:
 * <ul>
 * </ul>
 * 
 */
public class CWDataBrowser extends EmaClass  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose;

    String contextDirectory;

    //these are not null after reset is request, but before it is done and handled
    private CWDataBrowserReset cwDataBrowserReset;
    private Thread resetThread;

    EmaSelect dataSet, region, timePeriod, date;
    EmaHidden formSubmitted;
    EmaButton submitForm;

    String constructorDateTime;
    long   nRequestsInitiated = 0;
    long   nRequestsCompleted = 0;
    String baseDataDirectory;
    String dataServer;
    long   resetEveryNMillis;
    long   timeOfLastReset;
    int    printTopNMostRequested;
    boolean lookForAllUnusedDataFiles;
    ConcurrentHashMap requestedFilesMap; //thread-safe (not needed?) and has keys() method 
    String regionsImage;
    String regionsImageTitle;
    String regionsImageAlt;
    String title;
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
    int    regionRequests[];
    String getOptions[];
    String getTitles[];
    String getDirectories[];
    String getRegexs[];
    String getExtensions[];
    String hereIs;
    String hereIsAlt;
    protected String beginRowArray[] = {"<tr style=\"background-color:#FFCC99\">", "<tr style=\"background-color:#FFFFCC\">"};

    
    /**
     * Constructor
     */
    public CWDataBrowser() throws Exception {
        super("gov.noaa.pfel.coastwatch.CWDataBrowser"); 
        constructorDateTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
      
        //addAttribute(new EmaLabel(this, "instructions"));
        addAttribute(dataSet      = new EmaSelect(this, "dataSet"));
        addAttribute(region       = new EmaSelect(this, "region"));
        addAttribute(timePeriod   = new EmaSelect(this, "timePeriod"));
        addAttribute(date         = new EmaSelect(this, "date"));
        addAttribute(formSubmitted= new EmaHidden(this, "formSubmitted"));
        addAttribute(submitForm   = new EmaButton(this, "submitForm"));

        //one time things
        verbose = classRB2.getBoolean("verbose", false);

        //contextDirectory and logs (file will be in same dir as this class's source code, named log.txt)
        contextDirectory = classRB2.getString("contextDirectory", "");
        int logPo = fullClassName.lastIndexOf('.');
        String logDir = String2.replaceAll(fullClassName.substring(0, logPo + 1), ".", "/");
        String2.setupLog(false, false, 
            File2.getClassPath() + logDir + "log.txt", //with / separator
            true, String2.logFileDefaultMaxSize); //append?
        String2.log("\n" + String2.makeString('*', 80) +  
            "\nCWDataBrowser.constructor " + constructorDateTime +
            "\nlogFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());

        //get baseDataDirectory (for access within this program)
        baseDataDirectory = classRB2.getString("dataDirectory", null);
        Test.ensureNotNull(baseDataDirectory, "dataDirectory");

        //get dataServer (for web-based access to the data)
        dataServer = classRB2.getString("dataServer", null);
        Test.ensureNotNull(dataServer, "dataServer");

        //how often should reset be called?
        resetEveryNMillis = classRB2.getInt("resetEveryNMinutes", 10) * 60000; // millis/min

        //printTopNMostRequested
        printTopNMostRequested = classRB2.getInt("printTopNMostRequested", 50); 

        //lookForAllUnusedDataFiles
        lookForAllUnusedDataFiles = classRB2.getBoolean("lookForAllUnusedDataFiles", false); 

        //get the name of the file with the small regions map
        regionsImage      = classRB2.getString("regionsImage", "");
        regionsImageTitle = classRB2.getString("regionsImage.title", "");
        regionsImageAlt   = classRB2.getString("regionsImage.alt", "");

        //get title
        title = classRB2.getString("title.value", "");

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
        if (verbose || trouble)
            String2.log(
                "baseDataDirectory: " + baseDataDirectory + "\n" +
                "dataSetOptions: " + String2.toCSSVString(dataSetOptions) + "\n" +
                "dataSetTitles: " + String2.toNewlineString(dataSetTitles) + "\n" +
                "dataSetDirectories: " + String2.toCSSVString(dataSetDirectories) + "\n" +
                "dataSetRegexs: " + String2.toCSSVString(dataSetRegexs));
        if (trouble) 
            throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset " +
                "nDataSetOptions " + dataSetOptions.length + 
                " != nDataSetTitles " + dataSetTitles.length + 
                " != nDataSetDirectories " + dataSetDirectories.length + 
                " != nDataSetRegexs " + dataSetRegexs.length);

        //get region properties
        regionOptions      = classRB2.getString("region.options", null).split("\f");
        regionTitles       = classRB2.getString("region.title", null).split("\f");
        regionRegexs       = classRB2.getString("region.regexs", null).split("\f");
        regionCoordinates  = classRB2.getString("region.coordinates", null).split("\f");
        regionRequests     = new int[regionOptions.length];
        trouble = 
            regionOptions.length != (regionTitles.length-1) || //1 extra title (main)
            regionOptions.length != regionRegexs.length ||
            regionOptions.length != regionCoordinates.length;
        if (verbose || trouble)
            String2.log(
                "regionOptions: " + String2.toCSSVString(regionOptions) + "\n" +
                "regionTitles: " + String2.toNewlineString(regionTitles) + "\n" +
                "regionRegexs: " + String2.toCSSVString(regionRegexs));
        if (trouble) 
            throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset " +
                "nRegionOptions " + regionOptions.length + 
                " != nRegionTitles " + regionTitles.length + 
                " != nRegionRegexs " + regionRegexs.length +
                " != nRegionCoordinates " + regionCoordinates.length);

        //get timePeriod properties
        timePeriodOptions     = classRB2.getString("timePeriod.options", null).split("\f");
        timePeriodTitles      = classRB2.getString("timePeriod.title", null).split("\f");
        timePeriodDirectories = classRB2.getString("timePeriod.directories", null).split("\f");
        timePeriodRequests    = new int[timePeriodOptions.length];
        trouble = 
            timePeriodOptions.length != (timePeriodTitles.length-1) || //1 extra title (main)
            timePeriodOptions.length != timePeriodDirectories.length;
        if (verbose || trouble)
            String2.log(
                "timePeriodOptions: " + String2.toCSSVString(timePeriodOptions) + "\n" +
                "timePeriodTitles: " + String2.toNewlineString(timePeriodTitles) + "\n" +
                "timePeriodDirectories: " + String2.toCSSVString(timePeriodDirectories));
        if (trouble) 
            throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset " +
                "nTimePeriodOptions " + timePeriodOptions.length + 
                " != nTimePeriodTitles " + timePeriodTitles.length + " " +
                " != nTimePeriodDirectories " + timePeriodDirectories.length);

        //get 'get' properties
        //the first 'get' option must be .gif; a .gif file must be present before looking for others
        getOptions     = classRB2.getString("get.options", null).split("\f");
        getTitles      = classRB2.getString("get.title", null).split("\f");
        getDirectories = classRB2.getString("get.directories", null).split("\f");
        getRegexs      = classRB2.getString("get.regexs", null).split("\f");
        getExtensions  = classRB2.getString("get.extensions", null).split("\f");
        hereIs         = classRB2.getString("hereIs", null);
        hereIsAlt      = classRB2.getString("hereIsAlt", null);
        trouble = 
            getOptions.length != (getTitles.length-1) || //1 extra title (main)
            getOptions.length != getDirectories.length || 
            getOptions.length != getRegexs.length ||
            getOptions.length != getExtensions.length;
        if (verbose || trouble)
            String2.log(
                "getOptions: " + String2.toCSSVString(getOptions) + "\n" +
                "getDirectories: " + String2.toCSSVString(getDirectories) + "\n" +
                "getExtensions: " + String2.toCSSVString(getExtensions) + "\n" +
                "getRegexs: " + String2.toCSSVString(getRegexs));
        if (trouble) 
            throw new RuntimeException(String2.ERROR + ": CWDataBrowser.reset \n" +
                "nGetOptions " + getOptions.length + 
                " != nGetTitles " + getTitles.length + 
                " != nGetDirectories " + getDirectories.length + 
                " != nGetRegexs " + getRegexs.length +
                " != nGetExtensions " + getExtensions.length);

        requestedFilesMap = new ConcurrentHashMap(128, 0.75f, 4); //thread-safe
        //EmaAttribute.verbose= true;

        //reset after attributes have been created
        runCWDataBrowserReset();
        resetThread.join();
        boolean ok = getDataFromReset(); //it should succeed first try
        if (!ok)
            throw new RuntimeException(String2.ERROR + " in CWDataBrowserReset.");
    }

    /**
     * Run CWDataBrowserReset
     */
    void runCWDataBrowserReset() {
        //is there already a reset thread?
        if (resetThread != null) {
            String2.log(String2.ERROR + " in runCWDataBrowserReset: resetThread!=null");
            return;
        }

        //create a lower priority thread and run cwDataBrowserReset
        timeOfLastReset = System.currentTimeMillis();
        cwDataBrowserReset = new CWDataBrowserReset();
        resetThread = new Thread(cwDataBrowserReset);
        resetThread.setPriority(Thread.currentThread().getPriority() - 2); 
        //-2 because some adjacent priorities map to the same OS priority
        resetThread.start();
    }

    /**
     * This gets the data from CWDataBrowserReset (if any is available)
     *
     * @return true if reset info was successfully retrieved
     */
    public boolean getDataFromReset() {
        //is there no resetThread?
        if (resetThread == null)
            return false;

        //is the resetThread still running?
        if (resetThread.isAlive())
            return false;

        //did the resetThread throw an exception?
        String2.log(cwDataBrowserReset.runInfo.toString());
        if (cwDataBrowserReset.runError.length() > 0) {
            //error already printed to Tomcat's log
            //there is nothing more to be done
            //keep using old data
            //send email to Bob Simons?
            cwDataBrowserReset = null;
            resetThread = null;
            return false; 
        }

        //store dataSet info
        activeDataSetOptions  = cwDataBrowserReset.activeDataSetOptions;
        activeDataSetTitles   = cwDataBrowserReset.activeDataSetTitles;
        activeDataSetContents = cwDataBrowserReset.activeDataSetContents; 
        dataSet.setOptions(activeDataSetOptions); //1 time only; doesn't change till next reset()
        dataSet.setTitles(activeDataSetTitles);   //1 time only; doesn't change till next reset()
        if (verbose)
            String2.log("activeDataSetTitles: " + String2.toNewlineString(activeDataSetTitles));

        //print lots of useful information
        String2.log("\n" + String2.makeString('*', 80) +  
            "\nCWDataBrowser.getDataFromReset " + 
            Calendar2.getCurrentISODateTimeStringLocalTZ());
        String2.log("CWDataBrowser construction was at " + constructorDateTime + ".  Since then...\n");
        String2.log("Data files found which have matching .gif files:");
        String2.log(getUsageStatistics()); 
        String2.log("  number of page requests initiated since construction: " + nRequestsInitiated);
        String2.log("  number of page requests completed since construction: " + nRequestsCompleted);
        if (nRequestsInitiated > 0) {
            DecimalFormat percent = new DecimalFormat("##0.000");
            double nRequests = nRequestsInitiated / 100.0;
            String2.log("Data Set Usage:");
            for (int i = 0; i < dataSetOptions.length; i++)
                String2.log(String2.right(percent.format(dataSetRequests[i]/nRequests), 9) + 
                    "% were for " + dataSetOptions[i]);
            String2.log("Time Period Usage:");
            for (int i = 0; i < timePeriodOptions.length; i++)
                String2.log(String2.right(percent.format(timePeriodRequests[i]/nRequests), 9) + 
                    "% were for " + timePeriodOptions[i]);
            String2.log("Region Usage:");
            for (int i = 0; i < regionOptions.length; i++)
                String2.log(String2.right(percent.format(regionRequests[i]/nRequests), 9) + 
                    "% were for " + regionOptions[i]);
        }

        String2.log(SSR.getTopN(printTopNMostRequested, 
            " Most Requested .gif files", requestedFilesMap));

        //data was successfully gathered from CWDataBrowserReset
        resetThread = null;
        cwDataBrowserReset = null;
        return true;
    }

    /**
     * Call reset() if needed.
     *
     * @return true if reset was called
     */
    public boolean resetIfNeeded() {   
        //getDataFromReset if available
        getDataFromReset();

        //should runCWDataBrowserReset() be called?
        long sinceLastReset = System.currentTimeMillis() - timeOfLastReset;
        if (sinceLastReset > resetEveryNMillis) {
            runCWDataBrowserReset();
            return true;
        } else return false;

    }

    /**
     * This creates the POST HTML form with the EmaAttributes.
     *
     * @param request is a request from a user
     * @param displayErrorMessage if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the HTML code for the form.
     *    The line separator is the newline character.
     */
    public String getHTMLForm(HttpServletRequest request, boolean displayErrorMessages) {
        long startTime = System.currentTimeMillis();
        HttpSession session = request.getSession();
        if (verbose)
            String2.log("\ngetHTMLForm session isNew=" + session.isNew() + " id=" + session.getId());
        nRequestsInitiated++;
        int rowNumber = 0;
        StringBuilder sb = new StringBuilder();
        
        sb.append(getStartOfHTMLForm());
        displayErrorMessages = false;//always
     
        //create the rows of the table with the attributes

        //title
        sb.append(
            "    <tr>\n" + 
            "      <td colspan=\"2\">" + title + "</td>\n" +
            "      <td rowspan=\"6\"><img style=\"border:0px; width:228px; height:208px;\" \n" +
            "        src=\"" + regionsImage + "\"\n" +
            "        alt=\"" + regionsImageAlt + "\" title=\"" + regionsImageTitle + "\"\n" +
            "        usemap=#regionCoordinates>\n" +
            "        <br><div style=\"text-align:center;\"><small>" + regionsImageTitle + "</small></div></td>\n" +
            "    </tr>\n");

        //dataset
        String dataSetValue = dataSet.getValue(session);
        int whichDataSet = String2.indexOf(activeDataSetOptions, dataSetValue);
        if (whichDataSet < 0) {
            whichDataSet = 0;
            dataSetValue = activeDataSetOptions[0];
            dataSet.setValue(session, dataSetValue);
        }
        dataSetRequests[String2.indexOf(dataSetOptions, dataSetValue)]++;
        if (verbose)
            String2.log("dataSetValue = " + dataSetValue);
        setBeginRow(beginRowArray[Math2.odd(rowNumber++)? 1 : 0]);
        sb.append(dataSet.getTableEntry(dataSetValue, displayErrorMessages));

        //timePeriod
        Object[] object = (Object[])activeDataSetContents.get(whichDataSet);
        String[] activeTimePeriodOptions  = (String[])object[0];
        String[] activeTimePeriodTitles   = (String[])object[1];
        Vector   activeTimePeriodContents = (Vector)object[2]; 
        String   dataSetDirectory         = (String)object[3];
        if (verbose)
            String2.log("dataSetDirectory = " + dataSetDirectory);
        timePeriod.setOptions(activeTimePeriodOptions);
        timePeriod.setTitles(activeTimePeriodTitles);
        String timePeriodValue = timePeriod.getValue(session);
        int whichTimePeriod = String2.indexOf(activeTimePeriodOptions, timePeriodValue);
        if (whichTimePeriod < 0) {
            whichTimePeriod = 0;
            timePeriodValue = activeTimePeriodOptions[0];
            timePeriod.setValue(session, timePeriodValue);
        }
        timePeriodRequests[String2.indexOf(timePeriodOptions, timePeriodValue)]++;
        if (verbose)
            String2.log("timePeriodValue = " + timePeriodValue);
        setBeginRow(beginRowArray[Math2.odd(rowNumber++)? 1 : 0]);
        sb.append(timePeriod.getTableEntry(timePeriodValue, displayErrorMessages));

        //region
        object = (Object[])activeTimePeriodContents.get(whichTimePeriod);
        String[] activeRegionOptions     = (String[])object[0];
        String[] activeRegionTitles      = (String[])object[1];
        String[] activeRegionCoordinates = (String[])object[2];
        Vector   activeRegionContents    = (Vector)object[3]; 
        String   timePeriodDirectory     = (String)object[4];
        region.setOptions(activeRegionOptions);
        region.setTitles(activeRegionTitles);
        String regionValue = region.getValue(session);
        int whichRegion = String2.indexOf(activeRegionOptions, regionValue);
        if (whichRegion < 0) {
            whichRegion = 0;
            regionValue = activeRegionOptions[0];
            region.setValue(session, regionValue);
        }
        regionRequests[String2.indexOf(regionOptions, regionValue)]++;
        if (verbose)
            String2.log("regionValue = " + regionValue);
        setBeginRow(beginRowArray[Math2.odd(rowNumber++)? 1 : 0]);
        sb.append(region.getTableEntry(regionValue, displayErrorMessages));

        //define regionCoordinates for regionImage after activeRegionCoordinates known
        //do in reverse order, so small regions detected before large regions
        //(first match found is used)
        sb.append("    <map name=\"regionCoordinates\">\n");
        for (int i = activeRegionCoordinates.length - 1; i >= 0; i--)
//        for (int i = 0; i < activeRegionCoordinates.length; i++)
            sb.append(
                "      <area shape=\"rect\" coords=\"" + activeRegionCoordinates[i] + "\"\n" +
                "        title=\"" + activeRegionTitles[i + 1] + "\"\n" + //+1 since 0 is main title
                "        href=\"#\" " +   // was href=\"javascript:
                    "onClick=\"" + 
                    "document.forms[0].region[" + i + 
                    "].checked=true; document.forms[0].submit();\">\n");
        sb.append("    </map>\n");

        //formSubmitted
        sb.append(formSubmitted.getControl("true"));

        //date
        object = (Object[])activeRegionContents.get(whichRegion);
        String[] gifs              = (String[])object[0];
        String[] activeTimeOptions = (String[])object[1];
        int[]    getBits           = (int[])object[2]; 
        //if (verbose)
        //    String2.log("activeTimeOptions = " + String2.toCSSVString(activeTimeOptions));
        date.setOptions(activeTimeOptions);
        String timeValue = date.getValue(session);
        //find exact date match or one past (activeTimeOptions are sorted)
        //date will be off first time, and if user changes above settings and same date not available
        int whichDate = activeTimeOptions.length - 1; //last one
        if (timeValue != null) {
            for (int i = 0; i < activeTimeOptions.length; i++) {
                if (timeValue.compareTo(activeTimeOptions[i]) <= 0) {
                    whichDate = i;
                    break;
                }
            }
        }
        timeValue = activeTimeOptions[whichDate];
        date.setValue(session, timeValue);
        if (verbose)
            String2.log("timeValue = " + timeValue);
        setBeginRow(beginRowArray[Math2.odd(rowNumber++)? 1 : 0]);
        sb.append(date.getTableEntry(timeValue, displayErrorMessages));

        //submitForm  
        //may or may not be visible; so always a unique color (light red)
        setBeginRow("<tr style=\"background-color:#FFCCCC\">"); 
        sb.append("    <noscript>\n" + 
            submitForm.getTableEntry(submitForm.getValue(session), displayErrorMessages));
        sb.append("    </noscript>\n");

        //get
        String gifName = gifs[whichDate];
        int bits = getBits[whichDate];
        String currentFileDir = dataServer + 
            dataSetDirectory + "/" +    //for example, "QS"
            timePeriodDirectory + "/";  //for example, "1day"
        setBeginRow(beginRowArray[Math2.odd(rowNumber++)? 1 : 0]);
        sb.append(
            "    " + getBeginRow() + "\n" + 
            "      <td>" + classRB2.getString("get.label", "") + "&nbsp;</td>\n" +
            "      <td>");
        int nGetOptions = getOptions.length;
        for (int getI = 0; getI < nGetOptions; getI++) 
            if ((bits & Math2.Two[getI]) != 0)
                sb.append( 
                    "<a href=\"" + currentFileDir + 
                    getDirectories[getI] + "/" + gifName + 
                    getExtensions[getI] + "\"\n        title=\"" + 
                    getTitles[getI + 1] + "\">" + //+1: title 0 is main title
                    getOptions[getI] + "</a>\n        "); 
        sb.append(  
//                     "<br><small>" + hereIs + "</small>\n" +
             "      </td>\n" +
             "    </tr>\n");

        //image
        String currentGifName = 
            currentFileDir + 
            getDirectories[0] + "/" + gifName + getExtensions[0];
            //"QN2005001_2005001_curl_westus.gif";
        sb.append(
//            "    <tr><td>&nbsp;</td></tr>\n" +  //row 6
//            "    <tr><td colspan=\"2\">" + hereIs + "</td></tr>\n" + //row 7
            "    <tr>\n" + //standard
            "      <td colspan=\"3\"><img style=\"border:0px; width:650px; height:502px;\" \n" +
            "        src=\"" + currentGifName + "\"\n" + //row 8
            "        title=\"" + hereIs + " " + currentGifName + "\"\n" +
            "        alt=\"" + hereIsAlt + " " + currentGifName + "\">\n" +
            "      </td>\n" +
            "    </tr>\n"); 

        //update requestedFilesMap
        Integer I = (Integer)requestedFilesMap.get(gifName);
        requestedFilesMap.put(gifName, Integer.valueOf(I == null? 1 : I.intValue() + 1)); 

        //end of table, end of form
        sb.append(getEndOfHTMLForm(startTime, ""));
        nRequestsCompleted++;

        sb.append(
            "<p>DISCLAIMER OF ENDORSEMENT\n" +
            "<br>Any reference obtained from this server to a specific commercial product,\n" +
            "process, or service does not constitute or imply an endorsement by CoastWatch,\n" +
            "NOAA, or the United States Government of the product, process, or service, or \n" +
            "its producer or provider. The views and opinions expressed in any referenced \n" +
            "document do not necessarily state or reflect those of CoastWatch,\n" +
            "NOAA, or the United States Government.\n" +
            "\n" +
            "<p>DISCLAIMER FOR EXTERNAL LINKS\n" +
            "<br>The appearance of external links on this World Wide Web site does not\n" +
            "constitute endorsement by the\n" +
            "<a href=\"http://www.commerce.gov\">Department of Commerce</a>/<a href=\"https://www.noaa.gov\">National\n" +
            "Oceanic and Atmospheric Administration</a> of external Web sites or the information,\n" +
            "products or services contained\n" +
            "therein. For other than authorized activities the Department of Commerce/NOAA does not\n" +
            "exercise any editorial control over the information you may find at these locations. These\n" +
            "links are provided consistent with the stated purpose of this Department of Commerce/NOAA\n" +
            "Web site.\n" +
            "\n" +
            "<p>DISCLAIMER OF LIABILITY\n" +
            "<br>Neither the data Contributors, CoastWatch, NOAA, nor the United States Government, \n" +
            "nor any of their employees or contractors, makes any warranty, express or implied, \n" +
            "including warranties of merchantability and fitness for a particular purpose, \n" +
            "or assumes any legal liability for the accuracy, completeness, or usefulness,\n" +
            "of any information at this site.\n" +
            "\n" +
            "<p><small>Please email questions, comments, or\n" +
            "suggestions regarding this web page to erd.data at noaa.gov .</small>");

        return sb.toString();
    }




}
