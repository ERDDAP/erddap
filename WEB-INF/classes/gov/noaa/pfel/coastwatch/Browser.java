/* 
 * Browser Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.ema.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.pointdata.PointDataSet;
import gov.noaa.pfel.coastwatch.pointdata.PointDataSetFromStationVariables;
import gov.noaa.pfel.coastwatch.pointdata.PointVectors;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.TableDataSet;
import gov.noaa.pfel.coastwatch.sgt.*;
import gov.noaa.pfel.coastwatch.util.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


/**
 * This JSP web app generates custom maps (and associated
 * data files) on-the-fly based on a ...US+Mexico.grd file. 
 * Thus it replaces the subsampling and the generation of the other types of 
 * data files that were a big chunk of Dave Foley's scripts and the processing on GoodDog.
 * It also allows for any region to be sampled (not just pre-defined regions).
 * It also allows bathymetry contour lines to be plotted on the map.
 * It also allows contour data to be plotted on the map.
 * It also allows vector data to be plotted on the map.
 * All of this is served to the user with a browser via one easy-to-use
 * web page.
 *
 * <p>!!!!BEFORE DEPLOYMENT, change things listed at top of 
 * the 'fullClassName'.properties file.
 *
 * <p>Validate that data files are made correctly:
 * <ul>
 * <li> .asc - Rich Cosgrove read them into ArcView (future: I can do it.)
 * <li> .grd - GMT can read them
 * <li> .hdf - CDAT can read them (e.g., proper metadata)
 * <li> .mat - Luke has Matlab and can view them
 * <li> .nc  - ncdump or my MapViewer
 * <li> .tif - [currently not working]
 * <li> .xyz - look at the data (.xyz is not for a specific purpose/program)
 * </ul>
 *
 * <p>The base file name format is e.g.,    LATsstaS1day_20030304.
 * <br>The custom file name format is e.g., LATsstaS1day_20030304_x-135_X-113_y30_Y50.
 * <br>Letter 0 is 'L'ocal or 'O'pendap.
 * <br>Letter 7 is 'S'tandard (e.g., the units in the source file) or 'A'lternate units
 * <br>Code that is sensitive to the file name format has "//FILE_NAME_RELATED_CODE" in a comment,
 *   e.g., externally, Luke's grd2Hdf script is relies on a specific file name format
 *   for files generated from CWBrowser.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 */
public abstract class Browser extends HttpServlet {
  
    public static boolean drawLandAsMask = true;  //This was Dave's request  2006-03-29.

    public String fullClassName;
    public ConcurrentHashMap userHashMap = new ConcurrentHashMap(16, 0.75f, 4); //thread-safe
    public String[] dontTallyIPAddresses;
    public String numberOfRequestsByUsersDomain;
    public long    constructorMillis;

    //the things that don't change
    public OneOf oneOf;

    //the current Shared
    public volatile Shared shared;

    //nextShared is not null after calling createNextShared, but before getDataFromNextShared
    public volatile Shared nextShared;

    public volatile long timeOfLastReset;

    //for statistics
    public String lastReportDate = Calendar2.formatAsISODate(Calendar2.newGCalendarLocal()); //so no report on first date
    public volatile int nUsers = 0;
    public volatile long nRequestsInitiated = 0,
         nRequestsFailed    = 0,
         nRequestsCompleted = 0,
         idleTime, cumulativeIdleTime, 
         activeTime, cumulativeActiveTime,
         cumulativeResetTime, 
         timeStartedGetHTMLForm,
         timeFinishedGetHTMLForm; 
    public volatile int resetCount;
    public int resetTimesDistribution[]  = new int[String2.TimeDistributionSize];
    public int activeTimesDistribution[] = new int[String2.TimeDistributionSize];
    public int idleTimesDistribution[]   = new int[String2.TimeDistributionSize];

    public String javaScript;

    /**
     * This is called by the subclasses to set everything up.
     *
     * @param fullClassName the full class name (used to find the .properties file)
     *       public String fullClassName;
     * @throws Exception if trouble
     */
    public Browser(String fullClassName) throws Exception {
        this.fullClassName = fullClassName;

        //The information gathered here and data structures created here 
        //are valid for as long as this program runs.
        constructorMillis = System.currentTimeMillis();

        //route calls to a logger to com.cohort.util.String2Log
        String2.setupCommonsLogging(-1);

        //get oneOf 
        oneOf = new OneOf(fullClassName); //does most of the initialization

        String errorInMethod = String2.ERROR + " in " + oneOf.shortClassName() + " constructor:\n";

        javaScript = EmaClass.includeJavaScript + 
            oneOf.classRB2().getString("additionalJavaScript", 
                oneOf.emaRB2().getString("additionalJavaScript", ""));

        //dontTallyIPAddresses
        dontTallyIPAddresses = String2.split(oneOf.classRB2().getString("dontTallyIPAddresses", ""), ',');
        String2.log("dontTallyIPAddresses=" + String2.toCSSVString(dontTallyIPAddresses));
        numberOfRequestsByUsersDomain = 
            "Number of Requests by User's Domain:\n" +
            "    (\"(numeric)\" doesn't include " + String2.toCSSVString(dontTallyIPAddresses) + ")";

        //specialThings
        specialThings();

        //created shared object 
        Test.ensureEqual(createNextShared(), true, "createNextShared");
        nextShared.join();
        String error = getDataFromNextShared();
        if (error.length() == 0)
            String2.log(oneOf.shortClassName() + ".constructor: nextShare.join successful.");
        else 
            throw new RuntimeException(error);

    }

    /** 
     * This returns the JavaScript for the HTML header. 
     *
     * @return the javaScript needed by the EMA components which
     *   needs to be in the HTML head section.
     */
    public String getJavaScript() {
        return javaScript;
    }

    /** 
     * The returns the number of page requests for this application
     * made in this session.
     * This is identical to the same-named method in EmaClass.
     *
     * @param session
     * @return the number of page requests for this application
     * made in this session (0 for a newbie).
     */
    public int getNRequestsThisSession(HttpSession session) {
        return String2.parseInt((String)session.getAttribute(
            oneOf.shortClassName() + "." + EmaClass.N_REQUESTS_THIS_SESSION), -1);
    }


    /**
     * Create nextShared (i.e., create nextShared and run it to look for 
     * available data files).
     *
     * @return true if the reset was started successfully (i.e., if it wasn't 
     *    already running).
     */
    public boolean createNextShared() {
        //is there already a nextShared?
        if (nextShared != null) {
            String2.log(String2.ERROR + " in createNextShared: nextShared!=null");           
            return false;
        }

        try {
            //create a lower priority thread and run Shared
            timeOfLastReset = System.currentTimeMillis();
            nextShared = new Shared(oneOf);
            nextShared.setPriority(Thread.currentThread().getPriority() - 2); 
            //-2 because some adjacent priorities map to the same OS priority
            nextShared.start();

            //delete all of the flag files (whether or not they triggered the reset)
            File rfDir = new File(oneOf.fullResetFlagDirectory());
            String files[] = rfDir.list(); //doesn't return link to parent
            for (int i = 0; i < files.length; i++) {
                //String2.log("file in resetFlagDirectory: " + files[i]);
                File2.delete(oneOf.fullResetFlagDirectory() + files[i]);
            }

            return true;
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + " in createNextShared", e));
            return false;
        }
    }

    /**
     * destroy() is called by Tomcat whenever the servlet is removed from service.
     * See example at http://classes.eclab.byu.edu/462/demos/PrimeSearcher.java
     *
     * <p>Browser doesn't overwrite HttpServlet.init(servletConfig), but it could if need be. 
     * nextShared is created periodically by createNextShared.
     */
    public void destroy() {
        //shutdown nextShared
        if (nextShared == null) {
            String2.log("\nBrowser.destroy notes that nextShared was already null.");
        } else {
            try {
                nextShared.interrupt();

                //wait up to 2 minutes for !nextShared.isAlive
                int waitedSeconds = 0;
                while (waitedSeconds < 2*60 && nextShared.isAlive()) {
                    Math2.sleep(1000);
                    waitedSeconds++;
                }

                //call nextShared.stop()!
                if (nextShared.isAlive()) {
                    String2.log("\n!!! Browser.destroy is calling nextShared.stop()!!!");
                    nextShared.stop();
                } else {
                    String2.log("\nBrowser.destroy successfully used interrupt() to stop nextShared.");
                }

            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }
        }
    }


    /**
     * This gets the data from nextShared.
     *
     * @return an error String ("" if no error)
     */
    private String getDataFromNextShared() {

        String methodName = oneOf.shortClassName() + " browser.getDataFromNextShared: ";
        String error =  "";

        //is there no nextShared
        //This will happen if this thread blocked while another thread was in this 
        //method. When other thread is finished, it sets nextShared to null.
        if (nextShared == null)
            return methodName + "nextShared is null!";

        //is it still running
        if (nextShared.isAlive())
            return methodName + "nextShared is still alive!";

        //did nextShared throw an exception?  (e.g., THREDDS is down)
        if (nextShared.runError().length() > 0) {
            //set fake timeOfLastReset so another reset is tried in 10 minutes
            timeOfLastReset = System.currentTimeMillis() - 
                (oneOf.resetMaxMillis() - 10 * Calendar2.MILLIS_PER_MINUTE);

            //keep using old data
            error = nextShared.runError();
            String2.log(String2.ERROR + " in " + methodName + "\n" + error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + methodName, error);
            nextShared = null;
            return error; 
        }

        try {
            resetCount++;
            String2.log("\n" + String2.makeString('*', 80) +  
                "\n" + methodName + " " + 
                Calendar2.getCurrentISODateTimeStringLocalTZ());

            //kludge: for CWBrowserAK, to adjust palette suggested min and max to colder temps for SST data.
            if (oneOf.shortClassName().equals("CWBrowserAK")) {
                if (oneOf.verbose()) 
                    String2.log("adjusting SST dataset's min max for CWBrowserAK...");
                Vector gridDataSets = nextShared.activeGridDataSets();
                for (int i = OneOf.N_DUMMY_GRID_DATASETS; i < gridDataSets.size(); i++) {  //start at 2, since 0=None, 1=Bathymetry
                    GridDataSet gds = (GridDataSet)gridDataSets.get(i);
                    if ((gds.paletteMin.equals("-2") || gds.paletteMin.equals("8")) &&
                        (gds.paletteMax.equals("32"))) {
                        gds.paletteMin = "0";
                        gds.paletteMax = "18";
                        gds.altPaletteMin = "32";
                        gds.altPaletteMax = "64"; 
                    }
                }

                Vector pointDataSets = nextShared.activePointDataSets();
                for (int i = OneOf.N_DUMMY_OTHER_DATASETS; i < pointDataSets.size(); i++) {  //start at 1, since 0=None
                    PointDataSet pds = (PointDataSet)pointDataSets.get(i);
                    if (pds.internalName.equals("PNBwtmp")) {
                        pds.paletteMin = 0;
                        pds.paletteMax = 18;
                    }
                }
            }

            //How do I safely remove old files while other threads are working?
            //Answer: don't worry. It is almost impossible: it would only happen if the 
            //user is idle for 60 minutes then the file is checked for existence
            //and then deleted in the 1 ms after it is checked for existence but
            //before it is 'touched'. Even if that did happen, there would just be an
            //error message for the user.

            //remove old files from publicDir and privateDir
            long cacheMillis = oneOf.cacheMillis();
            int nPublicFiles = OneOf.publicDeleteIfOld(oneOf.fullPublicDirectory(), 
                System.currentTimeMillis() - cacheMillis); 
            int nPrivateFiles = File2.deleteIfOld(oneOf.fullPrivateDirectory(), 
                System.currentTimeMillis() - cacheMillis, true, true); 
            if (Math.max(nPublicFiles, nPrivateFiles) > 10000)
                oneOf.email(oneOf.emailEverythingTo(), 
                    "WARNING from " + oneOf.shortClassName(), 
                    nPublicFiles + " files remain in " + oneOf.fullPublicDirectory() + "\n" +
                    nPrivateFiles + " files remain in " + oneOf.fullPrivateDirectory());

            //remove old users from userHashMap
            //ConcurrentHashMap iterator is thread-safe
            //make a list of users to be removed
            Set entrySet = userHashMap.entrySet();
            Iterator entrySetIterator = entrySet.iterator();
            while (entrySetIterator.hasNext()) {
                Entry mapEntry = (Entry)entrySetIterator.next();
                User tUser = (User)mapEntry.getValue();
                if (tUser.lastAccessTime() < System.currentTimeMillis() - cacheMillis) {
                    entrySetIterator.remove();
                }
            }
            entrySetIterator = null;

            //generate report of active datasets in nextShared 
            Math2.gcAndWait("Browser"); Math2.gcAndWait("Browser"); //for more accurate memoryString in report below
            StringBuilder activeDataSets = new StringBuilder();
            activeDataSets.append(
                "Report from " + oneOf.shortClassName() + " at " + 
                    Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n\n" +
                Math2.memoryString() + "\n" + 
                nPublicFiles + " files remain in public directory\n  " + 
                  oneOf.fullPublicDirectory() + "\n" +
                nPrivateFiles + " files remain in private cache directory\n  " + 
                  oneOf.fullPrivateDirectory() + "\n" +
                GSHHS.statsString() + "\n" +
                SgtMap.nationalBoundaries.statsString() + "\n" +
                SgtMap.stateBoundaries.statsString() + "\n\n");                

            Test.ensureNotNull(nextShared.activeGridDataSetOptions(), 
                "nextShared.activeGridDataSetOptions() is null.");

            //add gridDataSet counts to top of report
            activeDataSets.append( 
                (nextShared.activeGridDataSetOptions().length - 1) +
                    " Active Grid Datasets\n");
            int timePeriodCount = 0;
            int nFileCount = 0;
            for (int dataSetI = OneOf.N_DUMMY_GRID_DATASETS; dataSetI < nextShared.activeGridDataSetOptions().length; dataSetI++) { //skip 0=None, 1=Bath
                GridDataSet tGridDataSet = (GridDataSet)nextShared.activeGridDataSets().get(dataSetI);
                for (int timePeriod = 0; timePeriod < tGridDataSet.activeTimePeriodOptions.length; timePeriod++) {
                    String[] tDates = (String[])tGridDataSet.activeTimePeriodTimes.get(timePeriod);
                    timePeriodCount++;
                    nFileCount += tDates.length;
                }
            }
            activeDataSets.append(
                "Total number of Grid Dataset Time Periods (all Datasets) = " + timePeriodCount + "\n" +
                "Total number of Grid files = " + nFileCount + "\n\n");


            //add list of gridDataSets 
            activeDataSets.append( 
                "Dataset\n" +
                "    Time Period    n Files  First Centered Time  Last Centered Time\n" +
                "    -------------  -------  -------------------  -------------------\n");
            for (int dataSetI = OneOf.N_DUMMY_GRID_DATASETS; dataSetI < nextShared.activeGridDataSetOptions().length; dataSetI++) { //skip 0=None, 1=Bath
                GridDataSet tGridDataSet = (GridDataSet)nextShared.activeGridDataSets().get(dataSetI);
                activeDataSets.append(tGridDataSet.option + 
                    "\n" + tGridDataSet.internalName + "\n");
                for (int timePeriod = 0; timePeriod < tGridDataSet.activeTimePeriodOptions.length; timePeriod++) {
                    String[] tDates = (String[])tGridDataSet.activeTimePeriodTimes.get(timePeriod);
                    activeDataSets.append("    " + String2.left(tGridDataSet.activeTimePeriodOptions[timePeriod], 15) +
                        String2.right("" + tDates.length, 7) +
                        "  " + String2.left(tDates[0], 21) +
                        tDates[tDates.length - 1] + "\n");
                }
                activeDataSets.append('\n');
            }
            activeDataSets.append('\n');

            //add active vector datasets
            activeDataSets.append((nextShared.activeVectorOptions().length - 1) + 
                " Active Vector Datasets in "  + oneOf.shortClassName() + "\n" +
                "as of " + Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
                "\n" +
                "   X        Y     Name\n" +
                "-------  -------  ---------------------------------------------\n");
            for (int dataSetI = OneOf.N_DUMMY_OTHER_DATASETS; dataSetI < nextShared.activeVectorOptions().length; dataSetI++) { //skip 0=None
                activeDataSets.append(
                           ((GridDataSet)(nextShared.activeGridDataSets().get(nextShared.activeVectorXDataSetIndexes()[dataSetI]))).internalName + 
                    "  " + ((GridDataSet)(nextShared.activeGridDataSets().get(nextShared.activeVectorYDataSetIndexes()[dataSetI]))).internalName + 
                    "  " + nextShared.activeVectorOptions()[dataSetI] + "\n");
            }
            activeDataSets.append("\n\n");

            //add active point datasets
            activeDataSets.append( 
                (nextShared.activePointDataSetOptions().length - 1) +
                " Active Point Datasets in " + oneOf.shortClassName() + "\n" +
                "as of " + Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
                "\n" +
                "Dataset\n" +
                "    Internal First Time           Last Time\n" +
                "    -------  -------------------  -------------------\n");
            for (int dataSetI = OneOf.N_DUMMY_OTHER_DATASETS; dataSetI < nextShared.activePointDataSetOptions().length; dataSetI++) { //skip 0=None
                PointDataSet tPointDataSet = (PointDataSet)nextShared.activePointDataSets().get(dataSetI);
                activeDataSets.append(tPointDataSet.option + "\n" + 
                    "    " + tPointDataSet.internalName + 
                    "  " + Calendar2.formatAsISODateTimeT(tPointDataSet.firstTime) + 
                    "  " + Calendar2.formatAsISODateTimeT(tPointDataSet.lastTime) + "\n"); 
            }
            activeDataSets.append("\n\n");

            //add active point vector datasets
            activeDataSets.append((nextShared.activePointVectorOptions().length - 1) + 
                " Active Point Vector Datasets in "  + oneOf.shortClassName() + "\n" +
                "as of " + Calendar2.getCurrentISODateTimeStringLocalTZ() + " \n" +
                "\n" +
                "   X        Y     Name\n" +
                "-------  -------  ---------------------------------------------\n");
            for (int dataSetI = OneOf.N_DUMMY_OTHER_DATASETS; dataSetI < nextShared.activePointVectorOptions().length; dataSetI++) { //skip 0=None
                activeDataSets.append(
                           ((PointDataSet)(nextShared.activePointDataSets().get(nextShared.activePointVectorXDataSetIndexes()[dataSetI]))).internalName + 
                    "  " + ((PointDataSet)(nextShared.activePointDataSets().get(nextShared.activePointVectorYDataSetIndexes()[dataSetI]))).internalName + 
                    "  " + nextShared.activePointVectorOptions()[dataSetI] + "\n");
            }         
            activeDataSets.append("\n\n");
 
            //write activeDataSets info to log
            String2.log(activeDataSets.toString());

            //get and print usage information
            String usageInfo = getUsageInfo();
            String2.log(usageInfo);

            //and email it once a day, after 7 am, so in my inbox when I arrive
            GregorianCalendar reportCalendar = Calendar2.newGCalendarLocal();
            String reportDate = Calendar2.formatAsISODate(reportCalendar);
            int hour = reportCalendar.get(Calendar2.HOUR_OF_DAY);
            if (!reportDate.equals(lastReportDate) && hour >= 7 && 
                !oneOf.displayDiagnosticInfo()) { //so browsers in development don't generate emails
                lastReportDate = reportDate;
                oneOf.email(oneOf.emailEverythingTo() + "," + oneOf.emailDailyReportTo(), 
                    "Daily Report from " + oneOf.shortClassName(), 
                    activeDataSets + usageInfo);
            }

            //log this resetTime
            String2.distributeTime(nextShared.resetTime(), resetTimesDistribution);
            cumulativeResetTime += nextShared.resetTime();

            String2.log(methodName + "finished successfully.\n");
        } catch (Exception t) {
            error = MustBe.throwable(String2.ERROR + " in " + methodName + "\n", t);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + methodName, error);
        }

        //data was successfully gathered in nextShared
        if (nextShared != null)  //extra insurance, although it should happen now that resetIfNeeded is synchronized
            shared = nextShared;
        nextShared = null; //last thing, since it's existence indicates not yet finished
        return error;
    }

    /**
     * This returns a String with usage info.
     */
    public String getUsageInfo() {
        StringBuilder htmlSB = new StringBuilder();
        if (nRequestsInitiated > 0) {
            long time = System.currentTimeMillis() - constructorMillis;
            htmlSB.append("Usage statistics for " + oneOf.shortClassName() + " (" + 
                    Calendar2.getCurrentISODateTimeStringLocalTZ() + "):\n" +
                "  time since instantiation = " + 
                    Calendar2.elapsedTimeString(time) + "\n" +
                "  number of user sessions created since instantiation = " + nUsers + "\n" +
                "    (average = " + ((nUsers * 86400000L) / time) + "/day).\n" +
                "\n");       

            DecimalFormat percent = new DecimalFormat("##0.000");
            double nRequests = nRequestsInitiated / 100.0;

            //print the tallied things
            htmlSB.append(oneOf.tally().toString(oneOf.printTopNMostRequested()));

        }

        //activeAndIdleTimeStats
        htmlSB.append(getActiveAndIdleTimeStats() + "\n\nDone.\n\n");


        return htmlSB.toString();
    }


    /**
     * See if createNextShared() needs to be called.
     * This is synchronized so only one thread can do this 
     * (especially getDataFromNextShared) at once.
     *
     * @return true if reset was called
     */
    public synchronized boolean resetIfNeeded() {   
        //calculate idleTime (if not first time this is called)
        if (timeFinishedGetHTMLForm > 0) {
            idleTime = System.currentTimeMillis() - timeFinishedGetHTMLForm; 
            String2.distributeTime(idleTime, idleTimesDistribution);
            cumulativeIdleTime += idleTime;      
        }

        //getDataFromNextShared if available
        getDataFromNextShared();

        //is reset stalled?
        long sinceLastReset = System.currentTimeMillis() - timeOfLastReset;
        //String2.log("Browser.resetIfNeeded  sinceLastReset=" + sinceLastReset +
        //    " resetStalledMillis=" + oneOf.resetStalledMillis() +
        //    " resetMaxMillis=" + oneOf.resetMaxMillis());
        if (sinceLastReset > oneOf.resetStalledMillis() && nextShared != null) {
            //stop nextShared
            String msg = "nextShared was stalled in " + oneOf.shortClassName() + "\n" +
                MustBe.getStackTrace(nextShared);
            try {
                if (nextShared.isAlive()) nextShared.stop();
            } catch (Exception e) {
            }
            nextShared = null;
            oneOf.email(oneOf.emailEverythingTo(), msg, msg);

            //force reset
            timeOfLastReset = 0;  
            sinceLastReset = System.currentTimeMillis();
        }

        //should reset() be called
        boolean doReset = false;
        if (sinceLastReset > oneOf.resetMaxMillis()) {
            //String2.log("reset? yes, > max");
            doReset = true;
        } else {
            File rfDir = new File(oneOf.fullResetFlagDirectory());
            int nFiles = rfDir.list().length; 
            doReset =  nFiles > 0;
            if (doReset)
                String2.log("Browser.resetIfNeeded reset? " + doReset + 
                    " (nFiles in directory=" + 
                    oneOf.fullResetFlagDirectory() + "=" + nFiles);
        }
        if (doReset) 
            createNextShared();

        timeStartedGetHTMLForm = System.currentTimeMillis();
        return doReset;

    }

    /**
     * This handles a "request" from a user, storing incoming attributes
     * as session values.
     * This updates totalNRequests, totalProcessingTime, maxProcessingTime.
     *
     * @param request 
     * @return true if all the values on the form are valid
     * @throws ServletException if trouble
     */
    public boolean processRequest(HttpServletRequest request) throws ServletException {   
        try {

            return getUser(request).processRequest(request);

        } catch (Exception e) {

            //send email
            String subject = String2.ERROR + " in " + oneOf.shortClassName() + ".processRequest";
            String msg = MustBe.throwableToString(e);
            oneOf.email(oneOf.emailEverythingTo(), subject, msg);

            //repackage and rethrow the exception (doGet wants either IOException of ServletException)
            throw new ServletException(subject + "\n" + msg);
        }
    }

    /** 
     * This gets the user (making one if needed) associated with a request.
     *
     * @param request
     * @return the User associated with the request (possibly a new user)
     */
    public abstract User getUser(HttpServletRequest request);

    /**
     * This returns the startHtmlBody, htmlForm, and endHtmlBody.
     *
     * @param request is a request from a user
     * @param displayErrorMessage if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the body HTML code. 
     *    The line separator is the newline character.
     */
    public String getStartHtmlHead() {
        return oneOf.startHtmlHead();
    }

    /**
     * This returns the startHtmlBody, htmlForm, and endHtmlBody.
     *
     * @param request is a request from a user
     * @param displayErrorMessage if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the body HTML code. 
     *    The line separator is the newline character.
     */
    public String getHtmlBody(HttpServletRequest request, 
            boolean displayErrorMessages) {
        return oneOf.startHtmlBody() + "\n" +
            getHtmlForm(request, displayErrorMessages) + "\n" +
            oneOf.endHtmlBody();
    }

    /**
     * This checks if the user is on the dontTallyIPAddresses list.
     *
     * @param remoteHost  the user's numeric ip address (e.g., 192.28.105.00)
     *     usually gathered from request.getHeader("x-forwarded-for").
     *     getRemoteHost() isn't what I want because it returns the ip 
     *     address of the last computer to touch the message, in this case, 
     *     our proxy server (which never changes).
     * @return true if the user's statistics should be tallied
     */
    public boolean getDoTally(String remoteHost) {
        if (remoteHost == null) {
            String2.log("  getDoTally=false  remoteHost=null");
            return false;
        }

        //should this users info be tallied?
        for (int i = 0; i < dontTallyIPAddresses.length; i++) {
            if (remoteHost.startsWith(dontTallyIPAddresses[i])) {
                String2.log("  getDoTally=false  remoteHost=" + remoteHost + " startsWith " + dontTallyIPAddresses[i]);
                return false;
            }
        }
        String2.log("  getDoTally=true  remoteHost=" + remoteHost);
        return true;
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
    public String getHtmlForm(HttpServletRequest request, boolean displayErrorMessages) {

       
        //String2.log("request.headerNames:");
        //String attNames[] = g2.toStringArray(String2.toArrayList(request.getHeaderNames()).toArray());
        //for (int i = 0; i < attNames.length; i++)
        //    String2.log("  " + attNames[i] + "=" + request.getHeader(attNames[i]));

        //categorize user's domains
        User user = getUser(request);
        boolean doTally = user.doTally;
        //x-forwarded-for returns requester's address
        //getRemoteHost(); returns our proxy server (never changes)
        String ipAddress = request.getHeader("x-forwarded-for");  
        if (doTally) {
            //tally ip addresses
            //but not full individual IP address; remove last of 4 numbers
            if (ipAddress == null || ipAddress.length() == 0)
                ipAddress = "(unknown)";
            int dotPo = ipAddress.lastIndexOf('.'); 
            if (dotPo > 0)
                ipAddress = ipAddress.substring(0, dotPo + 1);
            oneOf.tally().add(numberOfRequestsByUsersDomain, ipAddress);

            //count nRequests
            nRequestsInitiated++;
            nRequestsFailed++; //decrement later if successful
        }
        if (oneOf.verbose()) 
            String2.log("\n" + oneOf.shortClassName() + ".getHTMLForm" + 
                (doTally? "#" + nRequestsInitiated: "") + 
                "  " + Calendar2.getCurrentISODateTimeStringLocalTZ() + " doTally=" + doTally +
                " ipAddress=" + ipAddress);
            
        //most of the work (validation, html generation) is done in user.getHTMLForm()
        //It has its own try/catch.
        StringBuilder htmlSB = new StringBuilder();
        if (user.getHTMLForm(request, htmlSB, timeStartedGetHTMLForm)) {
            if (doTally) {
                nRequestsCompleted++;
                nRequestsFailed--; 
            }
        } else {

            //need to reindex?
            if (htmlSB.indexOf(Opendap.WAIT_THEN_TRY_AGAIN) >= 0)
                //If this fails because reset already in progress, 
                //hopefully the reset will catch the changes.
                //If not, this will be triggered by next failure.
                createNextShared();
        }

        //calculate activeTime
        timeFinishedGetHTMLForm = System.currentTimeMillis();
        activeTime = timeFinishedGetHTMLForm - timeStartedGetHTMLForm; 
        String2.distributeTime(activeTime, activeTimesDistribution);
        cumulativeActiveTime += activeTime;      

        if (oneOf.verbose()) 
            String2.log(
                "This html response in " + activeTime + " ms.");

        //for testing exceptions              
        //if (true) throw new RuntimeException("Fake Exception.");

        //test usage reports
        //htmlSB.append("<pre>" + getUsageInfo() + "</pre>\n");

        //pumping data through sgt uses lots of memory - be agressive with gc
        //System.gc();  //not Math2.incgc -- don't wait for it.  Commented out 2013-12-05. Let Java handle memory.
        return htmlSB.toString();
    }


    /**
     * Generate all of the active and idleTime statistics.
     * @return the statistics
     */
    public String getActiveAndIdleTimeStats() {
        return
            "Cumulative Reset Time  = " + Calendar2.elapsedTimeString(cumulativeResetTime) + "\n" +
            "  Reset Count: " + resetCount + "\n" +
            "  Reset Times Distribution:\n" +
            String2.getTimeDistributionStatistics(resetTimesDistribution) + "\n" +
            "Cumulative Idle Time   = " + Calendar2.elapsedTimeString(cumulativeIdleTime) + "\n" +
            "  Idle Times Distribution:\n" +
            String2.getTimeDistributionStatistics(idleTimesDistribution) + "\n" +
            "Cumulative Active Time = " + Calendar2.elapsedTimeString(cumulativeActiveTime) + "\n" +
            "  Number of page requests initiated since instantiation: " + nRequestsInitiated + "\n" +
            "  Number of page requests completed since instantiation: " + nRequestsCompleted + "\n" +
            "    (" + nRequestsFailed + " threw an exception)\n" +
            "  Active Times Distribution:\n" +
            String2.getTimeDistributionStatistics(activeTimesDistribution) + "\n";
    }



    /**
     * This is an experimental area
     */
    private void specialThings() {

        //one time 
        //get file names
        /* String2.log("one time");
        //handle AH....grd file in AH.....grd.zip -> AT in AT  
        for (int i = 1999; i <= 2003; i++) {
            String tDir = "c:/u00/data/AT/14day/grd/";
            String[] names = RegexFilenameFilter.list(tDir, "AH" + i + ".+zip");
            for (int j = 0; j < names.length; j++) {
                try {
                    String2.log("doing " + names[j]);
                    SSR.unzip(tDir + names[j], tDir, true, null); //true=ignoreZipDirectories
                    String ahName = names[j].substring(0, names[j].length() - 4); //remove .zip
                    String atName = "AT" + ahName.substring(2);
                    File2.rename(tDir, ahName, atName);
                    SSR.zip(tDir + atName + ".zip", new String[]{tDir + atName});
                    File2.delete(tDir + atName);
                    File2.delete(tDir + names[j]); //the AH zip file
                } catch (Exception e) {
                    String2.log(MustBe.throwable(String2.ERROR + " while processing " + names[j], e));
                }
            }
        } 
        */
        /* //handle AH....grd file in AT.....grd.zip, (unzip rename rezip)
        for (int i = 1999; i <= 2003; i++) {
            String tDir = "c:/u00/data/AT/8day/grd/";
            String[] names = RegexFilenameFilter.list(tDir, "AT" + i + ".+zip");
            for (int j = 0; j < names.length; j++) {
                try {
                    String2.log("doing " + names[j]);
                    SSR.unzip(tDir + names[j], tDir, true, null); //true=ignoreZipDirectories
                    String atName = names[j].substring(0, names[j].length() - 4); //remove .zip
                    String ahName = "AH" + atName.substring(2);
                    File2.rename(tDir, ahName, atName);
                    SSR.zip(tDir + atName + ".zip", new String[]{tDir + atName});
                    File2.delete(tDir + atName);
                } catch (Exception e) {
                    String2.log(MustBe.throwable(String2.ERROR + " while processing " + names[j], e));
                }
            }
        } */

/*
//start of test
        String2.log("****** TEST COMPRESSION");
        try {
            long time = System.currentTimeMillis();
            //String urlText = "http://www.google.com";   //doesn't compress
            //String urlText = "http://www.thejspbook.com"; //doesn't compress
            //String urlText = "http://www.it.kth.se/~jj/curl.gz"; //always compressed   
            String urlText = "http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/qscat/QNuy103day.nc.ascii?lon";    
            //String urlText = "http://www.webperformance.org/compression/"; //does compress
            String2.log(String2.getURLResponseStringUnchanged(urlText));
            String2.log("EndCompressionTest TIME=" + 
                (System.currentTimeMillis() - time) + " ms (3923 ms if uncompressed)");
        } catch (Exception e) {
            String2.log(MustBe.throwable("URLConnection experiment", e));
        }

 //end of compression test


//start of java lib test
        String2.log("****** JAVA LIB Test");
        try {
            long time = System.currentTimeMillis();
            String urlText = "http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/qscat/QNuy103day.nc";    
            DODSNetcdfFile dods = new DODSNetcdfFile(urlText);
            try {

                DODSVariable dodsVariable = dods.findVariableByShortName("lon");
                ArrayFloat arrayFloat = (ArrayFloat)dodsVariable.read();
                float[] af = (float[])arrayFloat.copyTo1DJavaArray();
                int n = af.length;
                String2.log("n=" + n + " af[0]=" + af[0] + " af[n-1]=" + af[n - 1]);

                //Object structures[] = dods.getStructures().toArray();
                //String2.log(structures.length + " structures: " + String2.toCSSVString(structures));
                //DODSStructure structure = dods.findStructureByShortName("uy10");
                dodsVariable = dods.findVariableByShortName("uy10");
                arrayFloat = (ArrayFloat)dodsVariable.read();
                Array ar = arrayFloat.section(new int[]{0,10,0}, new int[]{1,1,n});
                af = (float[])arrayFloat.copyTo1DJavaArray();
                String2.log("uy10[0]=" + af[0] + " uy10[n-1]=" + af[af.length - 1]);
            } finally {
                dods.close();
            }
          
         
            //public DODSStructure findStructure(String name); // full name
            //public DODSStructure findStructureByShortName(String shortName);
            //public DODSVariable findVariableByShortName(String shortName);

            String2.log("EndJavaLibExperiment  TIME=" + 
                (System.currentTimeMillis() - time) + "(22 - 45 sec uncompressed)\n" +
                "****** End JAVA LIB TEST");
        } catch (Exception e) {
            String2.log(MustBe.throwable("java lib experiment", e));
        }

 //end of java lib test
 // */      
    }

    /**
     * This responds to a "post" request from the user by extending HttpServlet's doPost.
     *
     * @param request 
     * @param response
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        doGet(request, response);
    }

    /** 
     * This responds to a "get" request from the user by extending HttpServlet's doGet.
     *
     * @param request
     * @param response
     * @throws ServletException, IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        //very first thing, always: if this is a secure browser, ensure access is authorized
        //(this is the authorization type, e.g., see Java Servlet and JSP Cookbook, ch 15)
        //???Should I force both/neither to be specified (not just one)???
        if ((oneOf.requiredAuthorization().length() > 0 &&
            !oneOf.requiredAuthorization().equals(request.getAuthType())) ||
            (oneOf.requiredRole().length() > 0 &&
            !request.isUserInRole(oneOf.requiredRole()))) {
            //send not-authorized response
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            //standard html response
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println(getStartHtmlHead());
            out.println("</head>");
            out.println("<body>");
            out.println(oneOf.url() + " is only accessible to authorized users via https.");
            out.println("</body>");
            out.println("</html>");
            return;
        }

        //is there an HTTP GET query?
        //it is important that these not generate a user:
        //   the nature of Get requests is that user request won't support cookies.
        //   So if a person requests 1000's of files, they would generate
        //   1000's of users (wasteful of time and memory).
        //It is internally synchronized.
        //Also, important to process these before setContentType text/html,
        //   so content can be of another type, e.g., application/download.
        try {
            if (doQuery(request, response))
                return;
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,  //a.k.a. Error 500
                MustBe.throwableToString(e)); 
        }

        //standard html response
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        //allow only one response per user to be processed at once
        //see Java Servlet Programming 2nd Ed., pg 38
        User user = getUser(request);
        ReentrantLock lock = String2.canonicalLock(user);
        try {
            if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
                throw new TimeoutException("Timeout waiting for user's lock.");
            try {

                //reset (look for new data) if needed
                resetIfNeeded();

                //process the form so user settings are noted (don't care if valid)
                processRequest(request);

                //for testing purposes, uncomment this to force reset to defaults
                //addDefaultsToSession(request.getSession()); 
                out.println(getStartHtmlHead());
                out.println(getJavaScript());
                out.println("</head>");

                out.println(getHtmlBody(request, false));
            } finally {
                lock.unlock();
            } 
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,  //a.k.a. Error 500
                MustBe.throwableToString(e)); 
        }

        out.println("</html>");
    }

    /**
     * This returns the kml code for the screenOverlay.
     *
     * @return the kml code for the screenOverlay.
     */
    public String getIconScreenOverlay() {
        return 
            "  <ScreenOverlay id=\"Logo\">\n" + //generic id
            "    <description>" + oneOf.baseUrl() + oneOf.url() + "</description>\n" +
            "    <name>Logo</name>\n" + //generic name
            "    <Icon>" +
                  "<href>" + oneOf.baseUrl() + "images/" + oneOf.googleEarthLogoFile() + "</href>" +
                "</Icon>\n" +
            "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
            "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
            "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" + //0=original size
            "  </ScreenOverlay>\n";
    }

    /**
     * This method responds to opendap-like HTTP GET queries to return 
     * data files.
     * A query is appended to the url for an instance of a CoastWatch browser.
     * Here is a sample url and query: 
<tt>https://coastwatch.pfeg.noaa.gov/coastwatch/CWBrowser.jsp?
get=gridData&dataSet=SST,NOAAGOESImager,DayandNight,0.05degrees,WestCoastofUS&timePeriod=1observation&centeredTime=2006-04-11T00:00:00&
minLon=-135&maxLon=-105&minLat=22&maxLat=50&nLon=400&nLat=200&fileType=.nc</tt>

     * The query is the "?" and subsequent text.
     * Any spaces in the above sample should be removed.
     * To see detailed information on forming a query, use the query "?get".
     *
     * <p>The intended end user is a human being writing a program or a script
     * to automatically get some data from this server. Thus the
     * communications must be both people- and computer-friendly.
     * If you want a primarily people-friendly interface, use CWBrowser.
     *
     * <p>Compared to Opendap:
     * <ul>
     * <li> This system is not meant to replace Opendap. Opendap provides
     *    access to any data structure in a file. This system
     *    is intended to provide easier access to space and time-oriented data.
     * <li> This system uses user-style information: actual lon and lat values,
     *    not indices in an array.  How long does it take the average
     *    user to calculate the indices to get a desired subset? 
     *    How often do they miscalculate it and get not the desired data?
     *    Also, if the data is updated between getting the time index values and
     *    getting data, the time index values will be out-of-date and user will
     *    get data other than desired.
     * <li> This system has a better GUI back up: users can go to CWBrowser to 
     *    see options, formulate a query, and get the data.
     * <li> This system returns standard file types 
     *    (.asc, .grd, .hdf, .mat, .nc, .xyz). Opendap defines its own 
     *    standard and requires a custom client (for non-ASCII requests).
     *    (Opendaps approach is interesting. It says no standard file type
     *    is sufficient or should be annointed THE standard, so we'll create
     *    our own new format and say it is THE standard.  But I do see the
     *    advantage of having one standard -- at least you only have to 
     *    write one client and one converter to the file format you prefer.)
     * <li> This system compresses all data transmissions (if the requester 
     *    accepts compression).
     *    Opendap's ASCII requests are not compressed for transmission,
     *    so the transmission is 3X to 10X slower than it needs to be.
     * <li> This system uses http for transactions; it doesn't create  
     *    its own transport protocol (as Opendap does).
     * </ul>
     * Yes, I could probably make this an opendap-compatible system.
     * It is unclear how big a project that would be.
     * But this new system is the type of service that John Caron said would be nice
     * if OPeNDAP would support in the future -- sort of a geo-spatially
     * aware version of OPeNDAP.
     *
     * @param request
     * @param response
     * @return true if the request was handled by this method 
     *    (so doGet doesn't need to handle it)
     * @throws Exception if trouble
     */
    public boolean doQuery(HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        long queryTime = System.currentTimeMillis();
        String query = request.getQueryString();
        if (query == null) 
            return false;
        query = SSR.percentDecode(query);
        String2.log("////**** GET Query\n  query=" + query);
        String lcQuery = query.toLowerCase();
        if (!lcQuery.startsWith("get"))
            return false;

        //process the query
        String error = null;  //eventually a complete html error message

        //parse the query
        Shared localShared = shared; //local copy, in case another thread changes the reference
        String url = oneOf.url();
        String fullUrl = oneOf.baseUrl() + oneOf.url();
        String parts[] = query.split("&");
        ArrayList alternate = new ArrayList();
        for (int i = 0; i < parts.length; i++) {
            int po = parts[i].indexOf('=');
            if (po > 0) {
                alternate.add(parts[i].substring(0, po).trim().toLowerCase()); //all attNames are made lowercase
                alternate.add(parts[i].substring(po + 1).trim());
            }
        }
        String thankYou = 
            "<p><strong>Thank you for using <a href=\"" + url + "?get\">" + fullUrl + "?get</a> .</strong>\n" +
            "<br>This system allows computer programs (or humans with a browser) to use an HTTP GET\n" +
            "request to get space and time-oriented data in various common file formats.\n" +
            "<br>For a human-friendly graphical-user-interface to (mostly) the same data, see\n" +
            "<a href=\"" + url + "\">" + File2.getNameNoExtension(url) + "</a>.\n";

        //get
        String getValue = (String)String2.alternateGetValue(alternate, "get");
        String cleanGetValue = null;
        boolean getGridData = false;
        boolean getGridTimeSeries = false;
        boolean getGridVectorData = false;
        boolean getBathymetryData = false;
        boolean getStationData = false;
        boolean getStationVectorData = false;
        boolean getTrajectoryData = false;
        boolean getOpendapSequence = false;
        if (error == null) {
            String cleanGets[] = {"gridData", "gridTimeSeries", "gridVectorData", "bathymetryData",
                "stationData", "stationVectorData", "trajectoryData"};
            int getIndex = String2.caseInsensitiveIndexOf(cleanGets, getValue);
            if (getIndex >= 0) {
                cleanGetValue = cleanGets[getIndex];
                if (getIndex == 0) getGridData = true;
                else if (getIndex == 1) getGridTimeSeries = true;
                else if (getIndex == 2) getGridVectorData = true;
                else if (getIndex == 3) getBathymetryData = true;
                else if (getIndex == 4) getStationData = true;
                else if (getIndex == 5) getStationVectorData = true;
                else if (getIndex == 6) getTrajectoryData = true;
            } else {
                error = 
                    thankYou + 
                    "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                    "<p><strong>Queries must begin with one of these options:</strong>\n" + 
                    "<ul><li><a href=\"" + url + "?get=gridData\"><kbd>?get=gridData</kbd></a>\n" +
                       " - to download lat lon gridded data (for example, satellite SST data).\n" + 
                    "<li><a href=\"" + url + "?get=gridTimeSeries\"><kbd>?get=gridTimeSeries</kbd></a>\n" +
                       " - to download a time series for one lat lon point from gridded data (for example, satellite SST data).\n" + 
                    "<li><a href=\"" + url + "?get=gridVectorData\"><kbd>?get=gridVectorData</kbd></a>\n" +
                       " - to download lat lon gridded vector data (for example, satellite-derived wind data).\n" + 
                    "<li><a href=\"" + url + "?get=bathymetryData\"><kbd>?get=bathymetryData</kbd></a>\n" +
                       " - to download lat lon gridded bathymetry data (ETOPO2v2).\n" + 
                    "<li><a href=\"" + url + "?get=stationData\"><kbd>?get=stationData</kbd></a>\n" + 
                       " - to download (averaged) time series data from stations (for example, buoy water temperature data).\n" + 
                    "<li><a href=\"" + url + "?get=stationVectorData\"><kbd>?get=stationVectorData</kbd></a>\n" + 
                       " - to download (averaged) time series vector data from stations (for example, buoy wind data).\n" + 
                    "<li><a href=\"" + url + "?get=trajectoryData\"><kbd>?get=trajectoryData</kbd></a>\n" + 
                       " - to download trajectory data (for example, from tagged animals).\n" + 
                    "</ul>\n"; 
            }
        } //close off 'if'
        String2.log("  get=" + getValue + " error=" + error);

        //********************************************************************
        //handle (currently all) get options
        if (error == null && (getGridData || getGridTimeSeries || getGridVectorData || 
                getBathymetryData ||
                getStationData || getStationVectorData || getTrajectoryData)) { 
            try {
                String cleanQuery = "?get=" + getValue + "&amp;dataSet=";

                //dataSet
                String dataSetValue = null;
                GridDataSet gridDataSet = null; 
                GridDataSet xGridDataSet = null; 
                GridDataSet yGridDataSet = null; 
                String gridVectorInternalName = null;
                PointDataSetFromStationVariables pointDataSet = null;
                PointDataSetFromStationVariables xPointDataSet = null;
                PointDataSetFromStationVariables yPointDataSet = null;
                String pointVectorInternalName = null;
                String pointVectorOption = null;
                TableDataSet tableDataSet = null;
                if (error == null && (!getBathymetryData)) {                
                    dataSetValue = (String)String2.alternateGetValue(alternate, "dataset");  //must be lowercase attName
                    //test 7char names 
                    String cleanDataSets[] =
                        getGridData || getGridTimeSeries? clean(localShared.activeGridDataSet7Names()) : 
                        getGridVectorData? clean(localShared.activeVector7Names()) : 
                        getStationData? clean(localShared.activePointDataSet7Names()) :
                        getStationVectorData? clean(localShared.activePointVector7Names()) :
                        getTrajectoryData? clean(localShared.activeTrajectoryDataSet7Names()) :
                        null; 
                    int dataSetIndex = String2.caseInsensitiveIndexOf(cleanDataSets, dataSetValue);
                    //test long names
                    String cleanDataSets2[] = null;
                    int nDummy = getGridData || getGridTimeSeries? OneOf.N_DUMMY_GRID_DATASETS : OneOf.N_DUMMY_OTHER_DATASETS; // 0=None   (gridData 1=Bathy)
                    if (dataSetIndex < nDummy) { //not matched
                        cleanDataSets2 = 
                            getGridData || getGridTimeSeries? clean(localShared.activeGridDataSetOptions()) : 
                            getGridVectorData? clean(localShared.activeVectorOptions()) : 
                            getStationData? clean(localShared.activePointDataSetOptions()) :
                            getStationVectorData? clean(localShared.activePointVectorOptions()) :
                            getTrajectoryData? clean(localShared.activeTrajectoryDataSetOptions()) :
                            null;
                        dataSetIndex = String2.caseInsensitiveIndexOf(cleanDataSets2, dataSetValue);
                    }
                    String next = getTrajectoryData? "individuals" : "timePeriod";
                    if (dataSetIndex < nDummy) { //not matched 
                        //remove 0=None 1=Bath before displaying
                        String tCleanDataSets[]  = new String[cleanDataSets.length - nDummy]; 
                        String tCleanDataSets2[] = new String[cleanDataSets2.length - nDummy]; 
                        System.arraycopy(cleanDataSets,  nDummy, tCleanDataSets,  0, cleanDataSets.length - nDummy);
                        System.arraycopy(cleanDataSets2, nDummy, tCleanDataSets2, 0, cleanDataSets2.length - nDummy);
                        error = listError("dataSet", dataSetValue, cleanQuery, true, 
                            tCleanDataSets, tCleanDataSets2, next);
                    } else {
                        if (getGridData || getGridTimeSeries || getGridVectorData ||
                            getStationData || getStationVectorData || getTrajectoryData) 
                            cleanQuery += dataSetValue + "&amp;" + next + "=";
                        if (getGridData || getGridTimeSeries) {
                            gridDataSet = (GridDataSet)localShared.activeGridDataSets().get(dataSetIndex); 
                        } else if (getGridVectorData) {
                            gridVectorInternalName = localShared.activeVector7Names()[dataSetIndex];
                            xGridDataSet = (GridDataSet)localShared.activeGridDataSets().get(
                                localShared.activeVectorXDataSetIndexes()[dataSetIndex]); 
                            yGridDataSet = (GridDataSet)localShared.activeGridDataSets().get(
                                localShared.activeVectorYDataSetIndexes()[dataSetIndex]); 
                        } else if (getStationData) {
                            pointDataSet = (PointDataSetFromStationVariables)localShared.activePointDataSets().get(dataSetIndex); 
                        } else if (getStationVectorData) {
                            pointVectorInternalName = localShared.activePointVector7Names()[dataSetIndex];
                            pointVectorOption = localShared.activePointVectorOptions()[dataSetIndex];
                            xPointDataSet = (PointDataSetFromStationVariables)localShared.activePointDataSets().get(
                                localShared.activePointVectorXDataSetIndexes()[dataSetIndex]); 
                            yPointDataSet = (PointDataSetFromStationVariables)localShared.activePointDataSets().get(
                                localShared.activePointVectorYDataSetIndexes()[dataSetIndex]); 
                        } else if (getTrajectoryData) {
                            tableDataSet = (TableDataSet)localShared.activeTrajectoryDataSets().get(dataSetIndex);
                        }
                        String2.log("  GET after dataset, cleanQuery=" + cleanQuery);
                    }
                }
    
                //timePeriod 
                String timePeriodValue = null;         
                String standardTimePeriodValue = null; 
                int timePeriodIndex = -1;
                if (error == null && (getGridData || getGridTimeSeries || getGridVectorData || 
                        getStationData || getStationVectorData)) { 
                    timePeriodValue = (String)String2.alternateGetValue(alternate, "timeperiod");  //must be lowercase attName
                    String cleanTimePeriods[] = 
                        getGridData || getGridTimeSeries? clean(gridDataSet.activeTimePeriodOptions) :
                        getGridVectorData? clean(xGridDataSet.activeTimePeriodOptions) :
                        getStationData || getStationVectorData? clean(PointDataSet.timePeriodOptions) :
                        null;
                    timePeriodIndex = String2.caseInsensitiveIndexOf(cleanTimePeriods, timePeriodValue);
                    String next = getGridData || getGridVectorData? "centeredTime" : 
                        getGridTimeSeries || getStationData || getStationVectorData? "beginTime" : null;  
                    if (timePeriodIndex < 0) 
                        error = listError("timePeriod", timePeriodValue, cleanQuery, true, 
                            cleanTimePeriods, null, 
                            next + (getStationData || getStationVectorData? "=null" : "")); //kludge to show begin/centeredTime entry form below
                    else {
                        cleanQuery += timePeriodValue + "&amp;" + next + "=";
                        standardTimePeriodValue = 
                            getGridData || getGridTimeSeries? gridDataSet.activeTimePeriodOptions[timePeriodIndex] :
                            getGridVectorData? xGridDataSet.activeTimePeriodOptions[timePeriodIndex] :
                            getStationData || getStationVectorData? PointDataSet.timePeriodOptions[timePeriodIndex] :
                            null;                            
                    }
                    if (error == null) String2.log("  GET after timePeriod, cleanQuery=" + cleanQuery);
                }
                
                //individuals (allow 1+)
                String individualsValue[] = null;
                if (error == null && (getTrajectoryData)) { 
                    String next = getTrajectoryData? "dataVariables" : null;  
                    String requested = (String)String2.alternateGetValue(alternate, "individuals");  //must be lowercase attName
                    String requestedArray[] = String2.split(requested, ','); //assumes no "," in individual's names
                    String allIndividuals[] = getTrajectoryData? tableDataSet.individuals() :
                        null;
                    String cleanIndividuals[] = clean(allIndividuals);
                    StringArray sa = new StringArray();
                    if (requested == null || requestedArray.length == 0) {
                        error = listError("individuals", null, cleanQuery, true, 
                            cleanIndividuals, null, 
                            next + (getTrajectoryData? "=null" : "")); //kludge to show dataVariables list below 
                    } else {
                        for (int i = 0; i < requestedArray.length; i++) {
                            //find each of the requested individuals
                            int which = String2.caseInsensitiveIndexOf(cleanIndividuals, requestedArray[i]);
                            if (which >= 0) {
                                sa.add(allIndividuals[which]);
                            } else {
                                error = listError("individuals", requestedArray[i], cleanQuery, true, 
                                    cleanIndividuals, null, 
                                    next + (getTrajectoryData? "=null" : "")); //kludge to show dataVariables list below  
                                break;
                            }
                        }
                        if (error == null) {
                            individualsValue = sa.toArray();
                            cleanQuery += requested + "&amp;" + next + "=";
                            String2.log("  GET after individuals, cleanQuery=" + cleanQuery);
                        }
                    }
                }
                
                //dataVariables (allow null and 0 values)
                String dataVariablesValue[] = null;
                if (error == null && (getTrajectoryData)) { 
                    String next = getTrajectoryData? "fileType" : null;  
                    String requested = (String)String2.alternateGetValue(alternate, "datavariables");  //must be lowercase attName
                    String requestedArray[] = String2.split(requested, ','); //assumes no "," in variable names
                    String allDataVariables[] = getTrajectoryData? tableDataSet.dataVariableNames() :
                        null;
                    String cleanDataVariables[] = clean(allDataVariables);
                    StringArray sa = new StringArray();
                    if (requested == null) {  
                        //if "dataVariables=" not specified, all dataVariables will be selected
                        for (int i = 0; i < allDataVariables.length; i++) 
                            sa.add(allDataVariables[i]);
                    } else if (requested.equals("")) {
                        //0 variables selected
                    } else { //0 or more values
                        for (int i = 0; i < requestedArray.length; i++) {
                            //find each of the requested dataVariables
                            int which = String2.caseInsensitiveIndexOf(cleanDataVariables, requestedArray[i]);
                            if (which >= 0) {
                                sa.add(allDataVariables[which]);
                            } else {
                                error = listError("dataVariables", requestedArray[i], cleanQuery, true, 
                                    cleanDataVariables, null, next); 
                                break;
                            }
                        }
                    }
                    if (error == null) {
                        dataVariablesValue = sa.toArray();
                        cleanQuery += requested + "&amp;" + next + "=";
                        String2.log("  GET after dataVariables, cleanQuery=" + cleanQuery);
                    }
                }
                
                //beginTime and endTime (for getGridTimeSeries || getStationData || getStationVectorData) 
                //and centeredTime (for getGridData || getGridVectorData)
                String TBeginTimeValue = null; //with 'T' connector
                String TEndTimeValue = null; //with 'T' connector
                String TCenteredTimeValue = null; //with 'T' connector
                String spaceBeginTimeValue = null;    //with ' ' connector
                String spaceEndTimeValue = null;    //with ' ' connector
                String spaceCenteredTimeValue = null;    //with ' ' connector
                if (error == null) {

                    //getGridData || getGridVectorData   gets centeredTime from a list of options
                    if (getGridData || getGridVectorData) {
                        TCenteredTimeValue = (String)String2.alternateGetValue(alternate, "centeredtime");  //must be lowercase attName
                        
                        //did user supply old-style endDate instead of centeredTime?  convert to centered time
                        if (TCenteredTimeValue == null) {
                            String ed = (String)String2.alternateGetValue(alternate, "enddate");  //must be lowercase attName
                            if (ed != null) {
                                if (ed.toLowerCase().equals("latest")) {
                                    TCenteredTimeValue = ed;
                                } else {
                                    String pre = ed.length() > 0 && ed.charAt(0) == '~'? "~" : "";
                                    if (pre.equals("~")) 
                                        ed = ed.substring(1);
                                    int timePeriodNHours = TimePeriods.getNHours(standardTimePeriodValue);
                                    TCenteredTimeValue = TimePeriods.oldStyleEndOptionToCenteredTime(
                                        timePeriodNHours, ed);
                                    TCenteredTimeValue = pre + String2.replaceAll(TCenteredTimeValue, ' ', 'T');
                                }
                            }
                        }

                        //centeredTime is handled a little differently because there may be many options,
                        //  so I avoid manipulating the options.
                        String originalCenteredTimes[] = 
                            getGridData? (String[])gridDataSet.activeTimePeriodTimes.get(timePeriodIndex) :
                            getGridVectorData? (String[])xGridDataSet.activeTimePeriodTimes.get(timePeriodIndex) :
                            null;
                        boolean approximateCenteredTime = false;
                        if (TCenteredTimeValue != null) {
                            if (TCenteredTimeValue.toLowerCase().equals("latest")) {
                                spaceCenteredTimeValue = originalCenteredTimes[originalCenteredTimes.length - 1];
                                TCenteredTimeValue = String2.replaceAll(spaceCenteredTimeValue, ' ', 'T');
                            } else {
                                //hopefully, it is an iso date/time
                                if (TCenteredTimeValue.startsWith("~")) {
                                    approximateCenteredTime = true;
                                    TCenteredTimeValue = TCenteredTimeValue.substring(1); //so it doesn't have ~
                                }
                                spaceCenteredTimeValue = String2.replaceAll(TCenteredTimeValue, 'T', ' '); //so connector is ' '
                            }
                        }

                        //always do binary search (fast)       
                        int centeredTimeIndex = spaceCenteredTimeValue == null? -1 : 
                            getGridData? gridDataSet.binaryFindClosestTime(originalCenteredTimes, spaceCenteredTimeValue) :
                            getGridVectorData? xGridDataSet.binaryFindClosestTime(originalCenteredTimes, spaceCenteredTimeValue) :
                            -1;
                        //String2.log("T=" + TCenteredTimeValue + " spaceCenteredTimeValue=" + spaceCenteredTimeValue + " index=" + centeredTimeIndex);

                        if (centeredTimeIndex >= 0) {
                            if (approximateCenteredTime) {
                                //the closest available centeredTime is assigned to spaceCenteredTimeValue
                                spaceCenteredTimeValue = originalCenteredTimes[centeredTimeIndex];                                 
                            } else if (!originalCenteredTimes[centeredTimeIndex].equals(spaceCenteredTimeValue)) {
                                //but if not approximateCenteredTime, insist on exact match
                                centeredTimeIndex = -1;
                            }
                        }
                        if (centeredTimeIndex < 0) {
                            //I avoid making cleanCenteredTimes because there can be a large number of Strings
                            String cleanCenteredTimes[] = new String[originalCenteredTimes.length];
                            for (int i = 0; i < originalCenteredTimes.length; i++)
                                cleanCenteredTimes[i] = String2.replaceAll(originalCenteredTimes[i], ' ', 'T'); 
                            error = listError("centeredTime", TCenteredTimeValue, cleanQuery, true, 
                                cleanCenteredTimes, null, "minLon=null");  //kludge to show min/max/lon/lat entry form below
                        } else cleanQuery += TCenteredTimeValue + "&amp;minLon=";

                    //getGridTimeSeries || getStationData || getStationVectorData   
                    //  get Begin and End from iso formatted strings (any time)
                    } else if (getGridTimeSeries || getStationData || getStationVectorData) {
                        GregorianCalendar firstTime = null;
                        GregorianCalendar lastTime = null;
                        if (getGridTimeSeries) {
                            String[] activeTimes = (String[])gridDataSet.activeTimePeriodTimes.get(timePeriodIndex);   
                            try {
                                firstTime = Calendar2.parseISODateTimeZulu(activeTimes[0]);//throws Exception if trouble
                            } catch (Exception e) {
                            }
                            try {
                                lastTime  = Calendar2.parseISODateTimeZulu(activeTimes[activeTimes.length - 1]); //throws Exception if trouble
                            } catch (Exception e) {
                            }
                        } else if (getStationData) { 
                            firstTime = (GregorianCalendar)pointDataSet.firstTime.clone();
                            lastTime = (GregorianCalendar)pointDataSet.lastTime.clone();
                        } else if (getStationVectorData) { 
                            firstTime = (GregorianCalendar)xPointDataSet.firstTime.clone();
                            lastTime = (GregorianCalendar)xPointDataSet.lastTime.clone();
                        }
                        GregorianCalendar suggestBeginGc = (GregorianCalendar)lastTime.clone();
                        int nHours = TimePeriods.getNHours(standardTimePeriodValue);
                        if (nHours < 24)           suggestBeginGc.add(Calendar2.DATE, -1); //one day back
                        else if (nHours < 30 * 24) suggestBeginGc.add(Calendar2.MONTH, -1); //one month back
                        else                       suggestBeginGc.add(Calendar2.YEAR, -1); //1 year back
                        TBeginTimeValue = (String)String2.alternateGetValue(alternate, "begintime"); //must be lowercase attName
                        TEndTimeValue   = (String)String2.alternateGetValue(alternate, "endtime");   //must be lowercase attName
                        if (TBeginTimeValue == null) 
                            TBeginTimeValue = Calendar2.formatAsISODateTimeT(suggestBeginGc);
                        else if (TBeginTimeValue.toLowerCase().equals("latest")) 
                            TBeginTimeValue = Calendar2.formatAsISODateTimeT(lastTime);
                        if (TEndTimeValue == null || TEndTimeValue.toLowerCase().equals("latest")) 
                            TEndTimeValue = Calendar2.formatAsISODateTimeT(lastTime);
                        spaceBeginTimeValue = String2.replaceAll(TBeginTimeValue, 'T', ' '); //so connector is ' '
                        spaceEndTimeValue   = String2.replaceAll(TEndTimeValue,   'T', ' '); //so connector is ' '
                        GregorianCalendar beginGc = null;
                        try {
                            beginGc = Calendar2.parseISODateTimeZulu(TBeginTimeValue); //throws Exception if trouble
                        } catch (Exception e) {
                        }
                        GregorianCalendar endGc = null;
                        try { 
                            endGc = Calendar2.parseISODateTimeZulu(TEndTimeValue);   //throws Exception if trouble
                        } catch (Exception e) {
                        }

                        //are user's Begin and endTime acceptable?
                        //allow before dataSet's firstTime and after dataSet's lastTime
                        if (TBeginTimeValue.equals("null")) //kludge to show Begin EndDate form below
                            error = "";
                        else if (beginGc == null)  //parse result may be null
                            error = "<kbd>beginTime</kbd> is not a properly formatted dateTime.\n<br>";
                        else if (endGc == null)  //parse result may be null
                            error = "<kbd>endTime</kbd> is not a properly formatted dateTime.\n<br>";
                        else if (beginGc.after(endGc))   
                            error = "<kbd>beginTime</kbd> must not be after <kbd>endTime</kbd>.\n<br>";
                        else if (endGc.before(firstTime)) //will find no data
                            error = "<kbd>endTime</kbd> must not be before the first available data (" + 
                                Calendar2.formatAsISODateTimeT(firstTime) + ").\n<br>";
                        else if (beginGc.after(lastTime)) //will find no data
                            error = "<kbd>beginTime</kbd> must not be after the last available data (" + 
                                Calendar2.formatAsISODateTimeT(lastTime) + ").\n<br>";
                        
                        if (error == null) {
                            //okay. ensure clean format
                            TBeginTimeValue     = Calendar2.formatAsISODateTimeT(beginGc);
                            TEndTimeValue       = Calendar2.formatAsISODateTimeT(endGc);
                            spaceBeginTimeValue = Calendar2.formatAsISODateTimeSpace(beginGc);
                            spaceEndTimeValue   = Calendar2.formatAsISODateTimeSpace(endGc);
                            cleanQuery += TBeginTimeValue + "&amp;endTime=" + TEndTimeValue + 
                                (getGridTimeSeries? "&amp;lon=" : "&amp;minLon=");
                        } else {
                            //error message is a form for user to select a Begin and EndTime
                            error = 
                                error + 
                                "Please specify the desired Begin Time and End Time (Zulu time zone):\n" +
                                "<form method=\"GET\" action=\"" + url + "\">\n" +
                                "  <input type=\"hidden\" name=\"get\" value=\"" + getValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"dataSet\" value=\"" + dataSetValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"timePeriod\" value=\"" + timePeriodValue + "\">\n" +
                                "  <table class=\"erd\">\n" + //padding=0
                                "    <tr>\n" +
                                "      <td>&nbsp;&nbsp;&nbsp;&nbsp;Begin Time: </td>\n" +
                                "      <td><input type=\"text\" name=\"beginTime\" value=\"" + 
                                    Calendar2.formatAsISODateTimeT(suggestBeginGc) + "\"\n" +
                                "        title=\"Enter the Begin Time in the form YYYY-MM-DDThh:mm:ss .\" \n" + 
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        size=\"20\" maxlength=\"40\" ></td>\n" +
                                "    </tr>\n" +
                                "    <tr>\n" +
                                "      <td>&nbsp;&nbsp;&nbsp;&nbsp;End Time: </td>\n" +
                                "      <td><input type=\"text\" name=\"endTime\" value=\"" + 
                                    Calendar2.formatAsISODateTimeT(lastTime) + "\"\n" +
                                "        title=\"Enter the End Time in the form YYYY-MM-DDThh:mm:ss .\" \n" + 
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        size=\"20\" maxlength=\"40\" ></td>\n" +
                                "    </tr>\n" +
                                "  </table>\n" +
                                "  &nbsp;&nbsp;&nbsp;&nbsp;<input type=\"submit\" value=\"Submit\"\n" +
                                "    title=\"Submit the Begin and End Time information.\">\n" +
                                //'null' is kludge to trigger min/max/lon/lat entry form below
                                "  <input type=\"hidden\" name=\"" + 
                                    (getGridTimeSeries? "lon" : "minLon") +
                                    "\" value=\"null\">\n" + 
                                "</form>\n";
                        }
                    }
                    if (error == null) String2.log("  GET after times, cleanQuery=" + cleanQuery);
                }

                if (error == null) {

                    //check if data access is allowed
                    boolean dataAccessAllowed =
                        getGridData? 
                            gridDataSet.dataAccessAllowedCentered(standardTimePeriodValue, TCenteredTimeValue) : 
                        getGridTimeSeries? 
                            gridDataSet.dataAccessAllowedCentered(standardTimePeriodValue, 
                                TEndTimeValue) : //TEndTimeValue is the last centeredTime
                        getGridVectorData? 
                            xGridDataSet.dataAccessAllowedCentered(standardTimePeriodValue, TCenteredTimeValue) : 
                        getBathymetryData? true :
                        getStationData? 
                            OneOf.dataAccessAllowed(pointDataSet.daysTillDataAccessAllowed, TEndTimeValue) :
                        getStationVectorData? 
                            OneOf.dataAccessAllowed(xPointDataSet.daysTillDataAccessAllowed, TEndTimeValue) :
                        false;
                    if (getTrajectoryData) {
                        dataAccessAllowed = true; //but any individual can make this false...
                        for (int i = 0; i < individualsValue.length; i++) 
                            if (!OneOf.dataAccessAllowed(
                                tableDataSet.daysTillDataAccessAllowed(individualsValue[i]), 
                                Calendar2.getCurrentISODateStringZulu()))
                                dataAccessAllowed = false;
                    }

                    if (!dataAccessAllowed) 
                        error = oneOf.dataAccessNotAllowed();
                }

                //lon lat
                double minLon = String2.parseDouble((String)String2.alternateGetValue(alternate, "minlon"));   //must be lowercase attName
                double maxLon = String2.parseDouble((String)String2.alternateGetValue(alternate, "maxlon"));
                double minLat = String2.parseDouble((String)String2.alternateGetValue(alternate, "minlat"));
                double maxLat = String2.parseDouble((String)String2.alternateGetValue(alternate, "maxlat"));

                //getGridTimeSeries 
                if (getGridTimeSeries) {
                    minLon = String2.parseDouble((String)String2.alternateGetValue(alternate, "lon"));   //must be lowercase attName
                    minLat = String2.parseDouble((String)String2.alternateGetValue(alternate, "lat"));
                    maxLon = minLon;
                    maxLat = minLat;
                }

                //lon lat  
                //getGridTimeSeries, getStationData, getStationVectorData, getTrajectoryData just ignore nLon and nLat
                //ok if null -> Integer.MAX_VALUE
                int nLon = String2.parseInt((String)String2.alternateGetValue(alternate, "nlon")); //must be lowercase attName
                int nLat = String2.parseInt((String)String2.alternateGetValue(alternate, "nlat"));
                if (error == null) {
                    if (nLon < Integer.MAX_VALUE) {
                        int po = cleanQuery.lastIndexOf("&amp;");
                        cleanQuery = cleanQuery.substring(0, po) + "&amp;nLon=" + nLon + cleanQuery.substring(po);
                        //String2.log("nLon=" + nLon);
                    }
                    if (nLat < Integer.MAX_VALUE) {
                        int po = cleanQuery.lastIndexOf("&amp;");
                        cleanQuery = cleanQuery.substring(0, po) + "&amp;nLat=" + nLat + cleanQuery.substring(po);
                        //String2.log("nLat=" + nLat);
                    }
                    if (getTrajectoryData) {
                        //do nothing
                    } else if (getGridTimeSeries) {
                        //get lon and lat (which ARE required)
                        if (Double.isNaN(minLon) || Double.isNaN(minLat)) {
                            //calculate suggestions if needed
                            if (Double.isNaN(minLon))
                                minLon = ((oneOf.regionMinX() + oneOf.regionMaxX()) / 2); //center
                            if (Double.isNaN(minLat))
                                minLat = ((oneOf.regionMinY() + oneOf.regionMaxY()) / 2); //center
                            error = 
                                "Please specify the desired longitude and latitude:\n" +
                                "<form method=\"GET\" action=\"" + url + "\">\n" +
                                "  <input type=\"hidden\" name=\"get\" value=\"" + getValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"dataSet\" value=\"" + dataSetValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"timePeriod\" value=\"" + timePeriodValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"beginTime\" value=\"" + TBeginTimeValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"endTime\" value=\"" + TEndTimeValue + "\">\n" +
                                "  <table class=\"erd\">\n" + //padding=0
                                "    <tr>\n" +
                                "      <td>&nbsp;&nbsp;&nbsp;Lon:&nbsp;</td>\n" +
                                "      <td><input type=\"text\" name=\"lon\" value=\"" + minLon + "\"\n" + 
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        title=\"Enter the longitude (in decimal degrees East).\"\n" +
                                "        size=\"6\" maxlength=\"48\" ></td>\n" +
                                "    </tr>\n" +
                                "    <tr>\n" +
                                "      <td>&nbsp;&nbsp;&nbsp;Lat:&nbsp;</td>\n" +
                                "      <td><input type=\"text\" name=\"lat\" value=\"" + minLat + "\"\n" + 
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        title=\"Enter the latitude (in decimal degrees North).\"\n" +
                                "        size=\"6\" maxlength=\"48\" ></td>\n" +
                                "    </tr>\n" +
                                "    <tr>\n" +
                                "      <td colspan=\"2\">\n" +
                                "        &nbsp;&nbsp;&nbsp;<input type=\"submit\" value=\"Submit\"\n" +
                                "          title=\"Submit the longitude and latitude information.\"></td>\n" +
                                "    </tr>\n" +
                                "  </table>\n" +
                                "</form/\n";
                        } else {
                            cleanQuery += minLon + "&amp;lat=" + minLat + "&amp;fileType=";
                        }
                    } else { 
                        //for all other getXxx, get min/max lon/lat  (which are NOT required)
                        //hence, used for getGridData || getGridVectorData || getBathymetryData || getStationData || getStationVectorData
                        String tMinLon = (String)String2.alternateGetValue(alternate, "minlon");
                        if (tMinLon != null && tMinLon.equals("null")) { //kludge from above to show min/max/lon/lat entry form
                            error = 
                                "Please specify the desired longitude and latitude range:\n" +
                                "<form method=\"GET\" action=\"" + url + "\">\n" +
                                "  <input type=\"hidden\" name=\"get\" value=\"" + getValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"dataSet\" value=\"" + dataSetValue + "\">\n" +
                                "  <input type=\"hidden\" name=\"timePeriod\" value=\"" + timePeriodValue + "\">\n" +
                                (getGridData || getGridVectorData? 
                                    "  <input type=\"hidden\" name=\"centeredTime\" value=\"" + TCenteredTimeValue + "\">\n" : 
                                   ("  <input type=\"hidden\" name=\"beginTime\" value=\"" + TBeginTimeValue + "\">\n" +
                                    "  <input type=\"hidden\" name=\"endTime\" value=\"" + TEndTimeValue + "\">\n")) +
                                (getStationData || getStationVectorData? 
                                    "  <input type=\"hidden\" name=\"minDepth\" value=\"null\">\n" : //force getting depth
                                    "") +
                                "  <table class=\"erd\">\n" + //padding=0
                                "    <tr>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;maxLat:&nbsp;</td>\n" +     
                                //oneOf.regionMin/Max/X/Y more appropriate defaults for form than -180,180 and -90,90
                                "      <td><input type=\"text\" name=\"maxLat\" value=\"" + oneOf.regionMaxY() + "\"\n" +
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        title=\"Enter the maximum latitude (in decimal degrees North).\"\n" +
                                "        size=\"6\" maxlength=\"48\" ></td>\n" +
                                "    <tr>\n" +
                                "      <td>minLon:&nbsp;</td>\n" +
                                "      <td><input type=\"text\" name=\"minLon\" value=\"" + oneOf.regionMinX() + "\"\n" +
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        title=\"Enter the minimum longitude (in decimal degrees East).\"\n" +
                                "        size=\"6\" maxlength=\"48\" ></td>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;maxLon:&nbsp;</td>\n" +
                                "      <td><input type=\"text\" name=\"maxLon\" value=\"" + oneOf.regionMaxX() + "\"\n" +
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        title=\"Enter the maximum longitude (in decimal degrees East).\"\n" +
                                "        size=\"6\" maxlength=\"48\" ></td>\n" +
                                "    </tr>\n" +
                                "    <tr>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;minLat:&nbsp;</td>\n" +
                                "      <td><input type=\"text\" name=\"minLat\" value=\"" + oneOf.regionMinY() + "\"\n" +
                                "        onkeypress=\"return !enter(event);\"\n" +
                                "        title=\"Enter the minimum latitude (in decimal degrees North).\"\n" +
                                "        size=\"6\" maxlength=\"48\" ></td>\n" +
                                "    </tr>\n" +
                                "    <tr>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "    </tr>\n" +
                                "    <tr>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td>&nbsp;</td>\n" +
                                "      <td colspan=\"2\" style=\"text-align:center;\">\n" +
                                "        <input type=\"submit\" value=\"Submit\"\n" +
                                "          title=\"Submit the longitude and latitude information.\"></td>\n" +
                                "    </tr>\n" +
                                "  </table>\n" +
                                "</form/\n";
                        } else {
                            //use defaults
                            if (Double.isNaN(minLon)) minLon = oneOf.regionMinX();
                            if (Double.isNaN(maxLon)) maxLon = oneOf.regionMaxX();
                            if (Double.isNaN(minLat)) minLat = oneOf.regionMinY();
                            if (Double.isNaN(maxLat)) maxLat = oneOf.regionMaxY();
                            if (nLon <= 0) nLon = Integer.MAX_VALUE;
                            if (nLat <= 0) nLat = Integer.MAX_VALUE;

                            //ensure valid
                            if (minLon > maxLon) 
                                error = "minLon (" + minLon + ") must be less than or equal to maxLon (" + maxLon + ").";
                            if (minLat > maxLat) 
                                error = "minLat (" + minLat + ") must be less than or equal to maxLat (" + maxLat + ").";
                            String next = getStationData || getStationVectorData? "minDepth=" : 
                                "fileType=";
                            cleanQuery += minLon + "&amp;maxLon=" + maxLon + 
                                "&amp;minLat=" + minLat + "&amp;maxLat=" + maxLat + "&amp;" + next;
                        }
                    }                     
                    if (error == null) String2.log("  GET after latLon, cleanQuery=" + cleanQuery);
                }

                //min/maxDepth
                String minDepthValue = null;         
                String maxDepthValue = null;         
                if (error == null && (getStationData || getStationVectorData)) { 
                    String cleanDepths[] = 
                        getStationData? clean(pointDataSet.depthLevels()) :
                        getStationVectorData? clean(xPointDataSet.depthLevels()) :
                        null;

                    //minDepth
                    minDepthValue = (String)String2.alternateGetValue(alternate, "mindepth");  //must be lowercase attName
                    if (minDepthValue == null) minDepthValue = cleanDepths[0]; //default value
                    else if (minDepthValue.equals("null") && cleanDepths.length == 1)
                        minDepthValue = cleanDepths[0];
                    String next = "maxDepth="; 
                    //Any double value is valid.  But if invalid, suggest from depths list.
                    if (Double.isFinite(String2.parseDouble(minDepthValue))) 
                         cleanQuery += minDepthValue + "&amp;" + next;
                    else error = listError("minDepth", minDepthValue, cleanQuery, true, 
                            cleanDepths, null, next + "null"); //=null forces user to see maxDepth options

                    //maxDepth
                    maxDepthValue = (String)String2.alternateGetValue(alternate, "maxdepth");  //must be lowercase attName
                    if (maxDepthValue == null) maxDepthValue = cleanDepths[cleanDepths.length - 1]; //default value
                    else if (maxDepthValue.equals("null") && cleanDepths.length == 1)
                        maxDepthValue = cleanDepths[0];
                    int maxDepthIndex = String2.caseInsensitiveIndexOf(cleanDepths, maxDepthValue);
                    next = "fileType";
                    //Any double value is valid.  But if invalid, suggest from depths list.
                    if (Double.isFinite(String2.parseDouble(maxDepthValue))) {
                        cleanQuery += maxDepthValue + "&amp;" + next + "=";
                        String2.log("  GET after depth, cleanQuery=" + cleanQuery);
                    } else {
                        error = listError("maxDepth", maxDepthValue, cleanQuery, true, 
                            cleanDepths, null, next);
                    }
                }
                

                //fileType
                String extension = null;
                String fileExtension = null;
                if (error == null) {
                    extension = (String)String2.alternateGetValue(alternate, "filetype");   //must be lowercase attName
                    String cleanFileTypes[] = clean(
                        getGridData? oneOf.gridGetOptions() :
                        getGridTimeSeries? oneOf.gridGetTSOptions() :
                        getGridVectorData? oneOf.gridVectorGetOptions() :
                        getBathymetryData? oneOf.bathymetryGetOptions() :
                        getStationData || getStationVectorData? oneOf.pointGetAvgOptions() :
                        getTrajectoryData? oneOf.trajectoryGetAllOptions() :
                        null);                    
                    int index = String2.caseInsensitiveIndexOf(cleanFileTypes, extension);
                    if (index < 0) 
                        error = listError("fileType", extension, cleanQuery, true, 
                            cleanFileTypes, null, null);
                    else {
                        cleanQuery += extension; 
                        extension = cleanFileTypes[index]; //standardized extension
                        fileExtension = extension.equals("FGDC")? ".xml" : 
                            extension.equals("ESRI.asc")? ".asc" : 
                            extension.equals("GoogleEarth")? ".kml" : 
                            extension.equals("transparent.png")? ".png" : 
                            extension;
                        String2.log("  GET after fileType, cleanQuery=" + cleanQuery);
                    }
                }

                if (error == null) {
                    //Actual file work is synchronized because different users could request
                    //same file, so there would be two simultaneous attempts to create it.
                    //(Think of Dave demoing the program to a class with 20 users on 20 computers.)
                    cleanQuery = String2.canonical(cleanQuery);
                    ReentrantLock lock = String2.canonicalLock(cleanQuery);
                    if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
                        throw new TimeoutException("Timeout waiting for lock on cleanQuery.");
                    try {

                        //create the file
                        String2.log("  GET query is clean. create the .nc file...");
                        String dir = oneOf.fullPrivateDirectory();
                        String name = null;
                        int whichImage = 
                            extension.equals("small.png")? 0 :
                            extension.equals("medium.png")? 1 :
                            extension.equals("large.png")? 2 :
                            -1;
                        if (whichImage >= 0)
                            fileExtension = ".png";

                        try {
                            //***************************************************************************
                            //*** getGridData
                            if (getGridData) {

                                //if png, set nLon to imageWidth and nLat to imageHeight
                                //makeMap uses this info for imageWidth and imageHeight
                                if (whichImage >= 0) {
                                    nLon = oneOf.imageWidths()[whichImage];  
                                    nLat = nLon + //only need room for small legend, so don't use imageHeights
                                        (whichImage == 0? 115 : 60); //but smallest has narrow&higher legend
                                }
                                if (extension.equals("GoogleEarth") || 
                                    extension.equals("transparent.png")) {
                                    //temporary limitation to avoid insufficient Java Heap Space/out of memory exception
                                    nLon = Math.min(nLon, 3601); //.1 degree, if global
                                    nLat = Math.min(nLat, 1801); //.1 degree, if global
                                }

                                //generate the data file name based on the request spec.      
                                //standard baseName e.g., LATsstaS1day_20030304   ...
                                //set name first in case of error in makeGrid
                                name = 
                                    FileNameUtility.makeBaseName(
                                        gridDataSet.internalName, 'S', //S=always standard units
                                        standardTimePeriodValue, spaceCenteredTimeValue) +
                                    FileNameUtility.makeWESNString( 
                                        minLon, maxLon, minLat, maxLat) +
                                    FileNameUtility.makeNLonNLatString(
                                        nLon, nLat);  //no extension
                                
                                //does the .nc file already exist in cache?
                                Grid grid;
                                if (File2.touch(dir + name + ".nc")) {
                                    //reuse
                                    String2.log("  GET using cached grid: " + name + ".nc");
                                    grid = new Grid();
                                    grid.readNetCDF(dir + name + ".nc", null);
                                } else {
                                    //get the data
                                    //if no data in range, makeGrid will throw exception
                                    grid = gridDataSet.makeGrid(standardTimePeriodValue, 
                                        spaceCenteredTimeValue, 
                                        minLon, maxLon, minLat, maxLat,
                                        nLon, nLat);

                                    //setAttributes
                                    gridDataSet.setAttributes(grid, name);

                                    //if large and from thredds, 
                                    //  cache it in .nc file (with attributes, useful for .ncHeader)
                                    //Do test with actual grid.lat/lon.length, not nLat nLon (often MAX_VALUE)
                                    if (grid.lon.length * grid.lat.length > 600 * 600 && 
                                        gridDataSet instanceof GridDataSetThredds) {
                                        //grid.saveAs() sets attributes
                                        grid.saveAs(dir, name, FileNameUtility.get6CharName(name), 
                                            Grid.SAVE_AS_NETCDF, false); //false=zipIt
                                    }
                                }
    
                                if (extension.equals("ESRI.asc"))
                                    name += "_ESRI";
                                else if (whichImage == 0) name += "_small"; 
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //file already exists?
                                //note that GlobalAttributes.id doesn't include nLon nLat info
                                if (File2.touch(dir + name + fileExtension)) {
                                    //reuse existing file
                                    String2.log("  GET reusing result file: " + name + fileExtension);

                                } else if (extension.equals("FGDC")) { //always standardized here
                                    oneOf.makeFgdcFile(gridDataSet, grid, 
                                        standardTimePeriodValue, spaceCenteredTimeValue, false, //not alt units 
                                        dir, name);

                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    //since not all files cached, save .nc if it doesn't exist
                                    if (!File2.touch(dir + name + ".nc")) 
                                        grid.saveAs(dir, name, FileNameUtility.get6CharName(name), Grid.SAVE_AS_NETCDF, 
                                            false); //false=zipIt

                                    //save .ncHeader
                                    File2.writeToFileUtf8(dir + name + extension,
                                        NcHelper.ncdump(dir + name + ".nc", "-h"));

                                } else if (extension.equals(".nc")) { //always standardized here
                                    //since not all files cached, save .nc if it doesn't exist
                                    if (!File2.touch(dir + name + ".nc")) 
                                        grid.saveAs(dir, name, FileNameUtility.get6CharName(name), Grid.SAVE_AS_NETCDF, 
                                            false); //false=zipIt

                                } else if (extension.equals("GoogleEarth")) { //always standardized here
                                    //Google Earth .kml
                                   
                                    //based on quirky example (but lots of useful info):
                                    //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
                                    //kml docs: https://developers.google.com/kml/documentation/kmlreference
                                    String west  = String2.genEFormat10(grid.lon[0]);
                                    String east  = String2.genEFormat10(grid.lon[grid.lon.length - 1]);
                                    String south = String2.genEFormat10(grid.lat[0]);
                                    String north = String2.genEFormat10(grid.lat[grid.lat.length - 1]);
                                    String compositeString = 
                                        TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " composite"; 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                                        "<Document>\n" +
                                        //human-friendly, but descriptive, <name>
                                        //name is used as link title -- leads to <description> 
                                        "  <name>" + XML.encodeAsXML(gridDataSet.boldTitle) + ", " + 
                                            standardTimePeriodValue + 
                                            compositeString + ", " + spaceCenteredTimeValue + "</name>\n" +
                                        //<description appears in help balloon
                                        "  <description><![CDATA[" + 
                                            //<br /> is what kml/description documentation recommends
                                        "    Time Period: " + standardTimePeriodValue + compositeString + "\n" + 
                                        "<br />    Centered Time: " + spaceCenteredTimeValue + "\n" + 
                                        "<br />    Data courtesy of: " + XML.encodeAsXML(gridDataSet.courtesy) + "\n" +
                                        //link to download data
                                        "<br />    <a href=\"" + fullUrl + "?" +
                                            //XML.encodeAsXML isn't ok
                                            "edit=Grid+Data" +
                                            "&amp;gridDataSet="      + SSR.minimalPercentEncode(gridDataSet.internalName) + 
                                            "&amp;gridTimePeriod="   + SSR.minimalPercentEncode(standardTimePeriodValue) +
                                            "&amp;gridCenteredTime=" + SSR.minimalPercentEncode(spaceCenteredTimeValue) + 
                                            "\">Download the data</a>\n" +
                                        "    ]]></description>\n");

                                    //did user supply beginTime?
                                    String tBeginTime = (String)String2.alternateGetValue(alternate, "begintime");  //must be lowercase attName
                                    if (tBeginTime == null) {
                                        //no timeline in Google Earth
                                        sb.append(
                                            //the kml link to the data 
                                            "  <GroundOverlay>\n" +
                                            "    <name>" + name + "</name>\n" +
                                            "    <Icon>\n" +
                                            "      <href>" + fullUrl + "?" +
                                                //XML.encodeAsXML isn't ok
                                                "get=gridData" +
                                                "&amp;dataSet="      + SSR.minimalPercentEncode(gridDataSet.internalName) +
                                                "&amp;timePeriod="   + SSR.minimalPercentEncode(timePeriodValue) +
                                                "&amp;centeredTime=" + SSR.minimalPercentEncode(TCenteredTimeValue) +
                                                "&amp;minLon=" + west +
                                                "&amp;maxLon=" + east +
                                                "&amp;minLat=" + south +
                                                "&amp;maxLat=" + north +
                                                "&amp;fileType=transparent.png" +
                                            "</href>\n" +
                                            "    </Icon>\n" +
                                            "    <LatLonBox>\n" +
                                            "      <west>" + west + "</west>\n" +
                                            "      <east>" + east + "</east>\n" +
                                            "      <south>" + south + "</south>\n" +
                                            "      <north>" + north + "</north>\n" +
                                            "    </LatLonBox>\n" +
                                            "    <visibility>1</visibility>\n" +
                                            "  </GroundOverlay>\n");
                                    } else { 
                                        //user supplied tBeginTime, so make a timeline in Google Earth
                                        name += "_timeline"; //distinguish from otherwise identical non-timeline file
                                        String originalCenteredTimes[] = 
                                            (String[])gridDataSet.activeTimePeriodTimes.get(timePeriodIndex);
                                        int nOCTimes = originalCenteredTimes.length;
                                        int tNHours = TimePeriods.getNHours(standardTimePeriodValue);
                                        boolean isMonthly = tNHours == TimePeriods.N_HOURS_MONTHLY;
                                        int timeI2 = gridDataSet.binaryFindClosestTime(originalCenteredTimes, spaceCenteredTimeValue);
                                        int timeI1 = gridDataSet.binaryFindClosestTime(originalCenteredTimes, tBeginTime);
                                        timeI1 = Math.min(timeI1, timeI2);
                                        timeI1 = Math.max(timeI1, timeI2 - 499); //limitation: no more than 500 time points
                                        timeI1 = Math.max(timeI1, 0); //ensure valid
                                        oneOf.tally().add("HTTP GET queries: get=gridData, Google Earth timeline, nImages", "" + (timeI2 - timeI1 + 1));
                                        for (int timeI = timeI1; timeI <= timeI2; timeI++) {
                                            String cTimeStringM1 = originalCenteredTimes[Math.max(0, timeI - 1)];
                                            String cTimeString0  = String2.replaceAll(originalCenteredTimes[timeI], ' ', 'T');
                                            String cTimeStringP1 = originalCenteredTimes[Math.min(nOCTimes - 1, timeI + 1)];
                                            double endTimeM1   = Calendar2.gcToEpochSeconds(TimePeriods.getEndCalendar(  standardTimePeriodValue, cTimeStringM1, null)); 
                                            double startTime0  = Calendar2.gcToEpochSeconds(TimePeriods.getStartCalendar(standardTimePeriodValue, cTimeString0,  null));
                                            double endTime0    = Calendar2.gcToEpochSeconds(TimePeriods.getEndCalendar(  standardTimePeriodValue, cTimeString0,  null));
                                            double startTimeP1 = Calendar2.gcToEpochSeconds(TimePeriods.getStartCalendar(standardTimePeriodValue, cTimeStringP1, null));
                                            if (startTime0 == endTime0) { //make TimePeriod at least 1 hr
                                                endTimeM1   += Calendar2.SECONDS_PER_HOUR / 2;
                                                startTime0  -= Calendar2.SECONDS_PER_HOUR / 2;
                                                endTime0    += Calendar2.SECONDS_PER_HOUR / 2;
                                                startTimeP1 -= Calendar2.SECONDS_PER_HOUR / 2;
                                            }
                                            double tBeginSpan = isMonthly || timeI == 0 || endTimeM1 < startTime0 ? 
                                                startTime0 : //no overlap, use beginning of current time period
                                                (endTimeM1 + startTime0)/2; //overlap? use 1/2 way between
                                            double tEndSpan = isMonthly || timeI == nOCTimes - 1 || endTime0 < startTimeP1? 
                                                 endTime0 : //no overlap, use end of current time period
                                                (endTime0 + startTimeP1) / 2; //overlap? use 1/2 way between
                                            sb.append(
                                                //the kml link to the data 
                                                "  <GroundOverlay>\n" +
                                                "    <name>" + cTimeString0 + "Z" + "</name>\n" +
                                                "    <Icon>\n" +
                                                "      <href>" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    "get=gridData" +
                                                    "&amp;dataSet="      + SSR.minimalPercentEncode(gridDataSet.internalName) +
                                                    "&amp;timePeriod="   + SSR.minimalPercentEncode(timePeriodValue) +
                                                    "&amp;centeredTime=" + SSR.minimalPercentEncode(cTimeString0) +
                                                    "&amp;minLon=" + west +
                                                    "&amp;maxLon=" + east +
                                                    "&amp;minLat=" + south +
                                                    "&amp;maxLat=" + north +
                                                    "&amp;fileType=transparent.png" + 
                                                "</href>\n" +
                                                "    </Icon>\n" +
                                                "    <LatLonBox>\n" +
                                                "      <west>" + west + "</west>\n" +
                                                "      <east>" + east + "</east>\n" +
                                                "      <south>" + south + "</south>\n" +
                                                "      <north>" + north + "</north>\n" +
                                                "    </LatLonBox>\n" +
                                                "    <TimeSpan>\n" +
                                                "      <begin>" + Calendar2.epochSecondsToIsoStringTZ(tBeginSpan) + "</begin>\n" +
                                                "      <end>"   + Calendar2.epochSecondsToIsoStringTZ(tEndSpan) + "</end>\n" +
                                                "    </TimeSpan>\n" +
                                                "    <visibility>1</visibility>\n" +
                                                "  </GroundOverlay>\n");
                                        }
                                    }
                                    sb.append(
                                        getIconScreenOverlay() +
                                        "</Document>\n" +
                                        "</kml>\n");
                                    File2.writeToFileUtf8(dir + name + fileExtension, sb.toString());

                                } else if (extension.equals("transparent.png")) {
                                    //note: I considered making image options part of Grid.saveAs
                                    //But then distributing gridSave as would require
                                    //distributing Sgt package and coastline (huge!) and political outline files!
                                    //For now, keep separate.

                                    //make the cpt and other needed info
                                    String fullCptName = CompoundColorMap.makeCPT(
                                        oneOf.fullPaletteDirectory(), 
                                        gridDataSet.palette, gridDataSet.paletteScale, 
                                        String2.parseDouble(gridDataSet.paletteMin), //for std units
                                        String2.parseDouble(gridDataSet.paletteMax), //for std units
                                        -1, true, dir);

                                    //make the image
                                    nLon = grid.lon.length;
                                    nLat = grid.lat.length;
                                    BufferedImage bufferedImage = 
                                        SgtUtil.getBufferedImage(nLon, nLat);
                                    Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();
                                   
                                    //fill with 128,128,128 --> later convert to transparent
                                    //Not a great approach to the problem.
                                    //Gray isn't used by any palette (except gray scale)
                                    //and no data set has gray scale as default 
                                    Color gray = SgtMap.oceanColor;
                                    g2D.setColor(gray);
                                    g2D.fillRect(0, 0, nLon, nLat);

                                    //draw the map
                                    SgtMap.makeCleanMap(
                                        grid.lon[0], grid.lon[grid.lon.length - 1],
                                        grid.lat[0], grid.lat[grid.lat.length - 1],
                                        false,

                                        grid, 
                                        1, //scaleFactor,
                                        1, //altScaleFactor,
                                        0, //altOffset,
                                        fullCptName, 

                                        false, false, SgtMap.NO_LAKES_AND_RIVERS, false, false,
                                        g2D, nLon, nLat,  
                                        0, 0, nLon, nLat); 

                                    //saveAsTransparentPng
                                    name += "_transparent"; //important for file transfer below
                                    SgtUtil.saveAsTransparentPng(bufferedImage, gray, dir + name);

                                } else if (whichImage >= 0) {
                                    //save as png
                                    gridSaveAsPng(gridDataSet, grid, dir, name, standardTimePeriodValue,
                                        spaceCenteredTimeValue, nLon, nLat);

                                } else {
                                    //normal File SaveAs
                                    gridDataSet.setAttributes(grid, name);
                                    int saveAsType = String2.indexOf(Grid.SAVE_AS_EXTENSIONS, fileExtension); 
                                    if (extension.equals(".asc")) //catch plain .asc
                                        saveAsType = Grid.SAVE_AS_ASCII;
                                    grid.saveAs(dir, name, FileNameUtility.get6CharName(name), saveAsType, false); //false=zipIt
                                    //insurance
                                    Test.ensureTrue(File2.isFile(dir + name + fileExtension), 
                                        name + fileExtension + " wasn't created."); //will be caught below
                                }

                            //***************************************************************************
                            //*** getGridVectorData
                            } else if (getGridVectorData) {

                                int absoluteVectorIndex = -1;
                                String vectorInfo[][] = oneOf.vectorInfo();
                                for (int i = OneOf.N_DUMMY_OTHER_DATASETS; i < vectorInfo.length; i++) {
                                    if (vectorInfo[i][OneOf.VIInternalName].equals(gridVectorInternalName)) {
                                        absoluteVectorIndex = i;
                                        break;
                                    }
                                }
                                Test.ensureNotEqual(absoluteVectorIndex, -1, 
                                    "gridVectorInternalName=" + gridVectorInternalName + " wasn't matched.");

                                //if png, set nLon to imageWidth and nLat to imageHeight
                                //makeMap uses this info for imageWidth and imageHeight
                                if (whichImage >= 0) {
                                    nLon = oneOf.imageWidths()[whichImage];  
                                    nLat = nLon + //only need room for small legend, so don't use imageHeights
                                        (whichImage == 0? 115 : 60); //but smallest has narrow&higher legend
                                }

                                //generate the data file names
                                //standard baseName e.g., LATsstaS1day_20030304   FILE_NAME_RELATED_CODE
                                String xBaseName = FileNameUtility.makeBaseName(
                                    xGridDataSet.internalName, 'S', //S=always standard units
                                    standardTimePeriodValue, spaceCenteredTimeValue); 
                                String yBaseName = FileNameUtility.makeBaseName(
                                    yGridDataSet.internalName, 'S', //S=always standard units
                                    standardTimePeriodValue, spaceCenteredTimeValue); 
                                //set 'name' in case next lines fail and name needed for error message
                                name = FileNameUtility.makeBaseName(
                                    gridVectorInternalName, 'S', //S=always standard units
                                    standardTimePeriodValue, spaceCenteredTimeValue); 
                                //if no data in range, makeGrid will throw exception
                                Grid xGrid = xGridDataSet.makeGrid(standardTimePeriodValue, 
                                    spaceCenteredTimeValue, 
                                    minLon, maxLon, minLat, maxLat,
                                    nLon, nLat);
                                Grid yGrid = yGridDataSet.makeGrid(standardTimePeriodValue, 
                                    spaceCenteredTimeValue, 
                                    minLon, maxLon, minLat, maxLat,
                                    nLon, nLat);
                                name = name +
                                    FileNameUtility.makeWESNString( 
                                        xGrid.lon[0], xGrid.lon[xGrid.lon.length - 1],
                                        xGrid.lat[0], xGrid.lat[xGrid.lat.length - 1]) +
                                    FileNameUtility.makeNLonNLatString(xGrid.lon.length, xGrid.lat.length);
                                xBaseName = xBaseName +
                                    FileNameUtility.makeWESNString(
                                        xGrid.lon[0], xGrid.lon[xGrid.lon.length - 1],
                                        xGrid.lat[0], xGrid.lat[xGrid.lat.length - 1]) +
                                    FileNameUtility.makeNLonNLatString(xGrid.lon.length, xGrid.lat.length);  
                                yBaseName = yBaseName +
                                    FileNameUtility.makeWESNString( 
                                        yGrid.lon[0], yGrid.lon[yGrid.lon.length - 1],
                                        yGrid.lat[0], yGrid.lat[yGrid.lat.length - 1]) +
                                    FileNameUtility.makeNLonNLatString(yGrid.lon.length, yGrid.lat.length);  
                                xGridDataSet.setAttributes(xGrid, xBaseName);
                                yGridDataSet.setAttributes(yGrid, yBaseName);
                                if      (whichImage == 0) name += "_small"; 
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //file already exists?
                                //note that GlobalAttributes.id doesn't include nLon nLat info
                                if (File2.touch(dir + name + fileExtension)) {
                                    //reuse existing file
                                    String2.log("  GET doQuery reusing " + name + fileExtension);

                                //} else if (extension.equals("FGDC")) { //always standardized here
                                //    oneOf.makeFgdcFile(gridDataSet, grid, 
                                //        standardTimePeriodValue, spaceCenteredTimeValue, false, //not alt units 
                                //        dir, name);

                                } else if (extension.equals(".mat")) { //always standardized here
                                    //no attributes needed
                                    TwoGrids.saveAsMatlab(xGrid, yGrid, dir, name, 
                                        xGridDataSet.internalName.substring(1),
                                        yGridDataSet.internalName.substring(1)); 

                                } else if (extension.equals(".nc")) { //always standardized here
                                    xGridDataSet.setAttributes(xGrid, xBaseName);
                                    yGridDataSet.setAttributes(yGrid, yBaseName);
                                    TwoGrids.saveAsNetCDF(xGrid, yGrid, dir, name, 
                                        xGridDataSet.internalName.substring(1),
                                        yGridDataSet.internalName.substring(1)); 

                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    //.ncHeader
                                    //first normal File SaveAs .nc
                                    xGridDataSet.setAttributes(xGrid, xBaseName);
                                    yGridDataSet.setAttributes(yGrid, yBaseName);
                                    TwoGrids.saveAsNetCDF(xGrid, yGrid, dir, name, 
                                        xGridDataSet.internalName.substring(1),
                                        yGridDataSet.internalName.substring(1)); 

                                    //then save .ncHeader
                                    File2.writeToFileUtf8(dir + name + extension,
                                        NcHelper.ncdump(dir + name + ".nc", "-h"));

                                } else if (extension.equals(".xyz")) { //always standardized here
                                    //no attributes needed
                                    TwoGrids.saveAsXyz(xGrid, yGrid, dir, name, "NaN"); 

                                } else if (whichImage >= 0) {

                                    //save as png
                                    gridVectorSaveAsPng(absoluteVectorIndex,
                                        xGridDataSet, yGridDataSet, xGrid, yGrid, 
                                        dir, name, standardTimePeriodValue,
                                        spaceCenteredTimeValue, nLon, nLat);

                                }

                            //***************************************************************************
                            //*** getGridTimeSeries
                            } else if (getGridTimeSeries) {
                                //generate the data file name   
                                String tDataName = FileNameUtility.makeAveragedGridTimeSeriesName(
                                    gridDataSet.internalName,
                                    'S', //!!!currently assumes Standard units...
                                    minLon, minLat, spaceBeginTimeValue, spaceEndTimeValue, 
                                    standardTimePeriodValue);
                                name = tDataName;
                                if      (whichImage == 0) name += "_small"; //since imageWidth imageHeight not in name
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //does the .nc file already exist?
                                //If data is expensive to get, this ensures it is only read once.
                                //.nc is starting point for 
                                Table table = null; //table created here if convenient; otherwise not
                                if (File2.touch(dir + tDataName + ".nc")) {
                                    //reuse existing file
                                    String2.log("  GET doQuery reusing .nc " + tDataName + ".nc");
                                } else {
                                    table = gridDataSet.getTimeSeries(dir, minLon, minLat, 
                                        spaceBeginTimeValue, spaceEndTimeValue, standardTimePeriodValue);
                                    //save the file
                                    String dimensionName = "row";
                                    table.saveAsFlatNc(dir + tDataName + ".nc", dimensionName);
                                }

                                //final file already exists?
                                if (error != null) {
                                    //fall through
                                } else if (File2.touch(dir + name + fileExtension)) {
                                    //reuse existing final file
                                } else if (extension.equals(".asc")) {
                                    //make table
                                    if (table == null) {
                                        table = new Table();
                                        table.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }
                                    table.saveAsTabbedASCII(dir + name + extension); 

                                } else if (whichImage >= 0) {
                                    //make table
                                    if (table == null) {
                                        table = new Table();
                                        table.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }

                                    //save as png
                                    gridTimeSeriesSaveAsPng(gridDataSet,  
                                        table, dir, name, standardTimePeriodValue,
                                        spaceCenteredTimeValue, minLon, minLat, whichImage);

                                } else if (extension.equals(".html")) {
                                    //make table
                                    if (table == null) {
                                        table = new Table();
                                        table.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }

                                    //make query for medium.png to be embedded in html file
                                    //It needs to be query (dynamic) since image file in cache may be deleted.
                                    int tpo = cleanQuery.indexOf("fileType=");
                                    Test.ensureNotEqual(tpo, -1, "'fileType=' not found in cleanQuery=" + cleanQuery + ".");
                                    String tCleanQuery = cleanQuery.substring(0, tpo + 9) + "medium.png"; 

                                    String preTableHtml = 
                                        "<img src=\"" + fullUrl + tCleanQuery + "\" alt=\"Time Series Graph\">" +
                                        //"\n<p><strong>Time Series from Lat Lon Gridded Data</strong>" +
                                        //"\n<br><strong>Data set:</strong> " + 
                                        //    XML.encodeAsHTML(gridDataSet.internalName + " = " + gridDataSet.boldTitle) + 
                                        //"\n<br><strong>Time period:</strong> " + standardTimePeriodValue + 
                                        //    (TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " composite") +
                                        //(gridDataSet.courtesy.length() == 0? "" :
                                        //    "\n<br><strong>Data courtesy of:</strong> " + 
                                        //    XML.encodeAsHTML(gridDataSet.courtesy)) + 
                                        "\n<p>";

                                    //save as .html
                                    table.saveAsHtml(dir + name + extension, preTableHtml, "", 
                                        "", Table.BGCOLOR, 1, true, 3, true, false); 

                                } else if (extension.equals(".mat")) {
                                    //make table
                                    if (table == null) {
                                        table = new Table();
                                        table.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }
                                    table.saveAsMatlab(dir + name + extension, 
                                        gridDataSet.internalName.substring(1)); 

                                } else if (extension.equals(".nc")) {
                                    //already done
                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    File2.writeToFileUtf8(dir + name + fileExtension,
                                        NcHelper.ncdump(dir + tDataName + ".nc", "-h"));
                                } else {
                                    Test.error("internal error Browser.doQuery:\n" +
                                        "unexpected gridTimeSeries extension: " + extension);
                                }

                                //insurance
                                if (error == null)
                                    Test.ensureTrue(File2.isFile(dir + name + fileExtension), 
                                        name + fileExtension + " wasn't created."); //will be caught below


                            //***************************************************************************
                            //*** getBathymetryData
                            } else if (getBathymetryData) {

                                //if png, set nLon to imageWidth and nLat to imageHeight
                                //makeMap uses this info for imageWidth and imageHeight
                                if (whichImage >= 0) {
                                    nLon = oneOf.imageWidths()[whichImage];  
                                    nLat = nLon + //only need room for small legend, so don't use imageHeights
                                        (whichImage == 0? 115 : 60); //but smallest has narrow&higher legend
                                }
                                //if (extension.equals("transparent.png")) {
                                {
                                    //temporary limitation to avoid insufficient Java Heap Space/out of memory exception
                                    //  or excessive requests
                                    nLon = Math.min(nLon, 3601); //.1 degree, if global
                                    nLat = Math.min(nLat, 1801); //.1 degree, if global
                                }

                                //generate the data file name based on the request spec.      
                                //standard baseName e.g., LBAthym_W-128   ...
                                //set name first in case of error in makeGrid
                                name = 
                                    SgtMap.BATHYMETRY_7NAME +
                                    FileNameUtility.makeWESNString( 
                                        minLon, maxLon, minLat, maxLat) +
                                    FileNameUtility.makeNLonNLatString(
                                        nLon, nLat);  //no extension
                                
                                //getBathymetryGrid does caching
                                Grid grid = SgtMap.createTopographyGrid(
                                    oneOf.fullPrivateDirectory(),
                                    minLon, maxLon, minLat, maxLat, nLon, nLat);
                                SgtMap.setBathymetryAttributes(grid, false); //!saveMVAsDouble
   
                                if (extension.equals("ESRI.asc"))
                                    name += "_ESRI";
                                else if (whichImage == 0) name += "_small"; 
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //file already exists?
                                //note that GlobalAttributes.id doesn't include nLon nLat info
                                if (File2.touch(dir + name + fileExtension)) {
                                    //reuse existing file
                                    String2.log("  GET reusing result file: " + name + fileExtension);

                                //currently no FGDC option; I'd have to make separate mechanism for it

                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    //save .nc if it doesn't exist
                                    if (!File2.touch(dir + name + ".nc")) 
                                        grid.saveAs(dir, name, SgtMap.BATHYMETRY_7NAME.substring(1), 
                                            Grid.SAVE_AS_NETCDF, false); //false=zipIt

                                    //save .ncHeader
                                    File2.writeToFileUtf8(dir + name + extension,
                                        NcHelper.ncdump(dir + name + ".nc", "-h"));

                                } else if (extension.equals(".nc")) { //always standardized here
                                    grid.saveAs(dir, name, SgtMap.BATHYMETRY_7NAME.substring(1), 
                                        Grid.SAVE_AS_NETCDF, false); //false=zipIt

                                } else if (extension.equals("GoogleEarth")) { //always standardized here
                                    //Google Earth .kml

                                    //based on quirky example (but lots of useful info):
                                    //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
                                    //kml docs: https://developers.google.com/kml/documentation/kmlreference
                                    String west  = String2.genEFormat10(grid.lon[0]);
                                    String east  = String2.genEFormat10(grid.lon[grid.lon.length - 1]);
                                    String south = String2.genEFormat10(grid.lat[0]);
                                    String north = String2.genEFormat10(grid.lat[grid.lat.length - 1]);
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                                        "<Document>\n" +
                                        //human-friendly, but descriptive, <name>
                                        //name is used as link title -- leads to <description> 
                                        "  <name>" + XML.encodeAsXML(SgtMap.BATHYMETRY_BOLD_TITLE) + "</name>\n" +
                                        //<description appears in help balloon
                                        "  <description><![CDATA[" + 
                                            //<br /> is what kml/description documentation recommends
                                        "   Data courtesy of: " + XML.encodeAsXML(SgtMap.BATHYMETRY_COURTESY) + "\n" +
                                        //link to download data
                                        "<br />    <a href=\"" + fullUrl + "?" +
                                            //XML.encodeAsXML isn't ok
                                            "edit=Grid+Data" +  //SSR.minimalPercentEncode
                                            "&amp;gridDataSet=" + SgtMap.BATHYMETRY_7NAME + 
                                        "\">Download the data</a>\n" +
                                        "    ]]></description>\n");
                                    sb.append(
                                        //the kml link to the data 
                                        "  <GroundOverlay>\n" +
                                        "    <name>" + name + "</name>\n" +
                                        "    <Icon>\n" +
                                        "      <href>" + fullUrl + "?" +
                                            //was SSR.minimalPercentEncode  XML.encodeAsXML isn't ok
                                            "get=bathymetryData" +
                                            "&amp;dataSet=" + SgtMap.BATHYMETRY_7NAME +
                                            "&amp;minLon=" + west +
                                            "&amp;maxLon=" + east +
                                            "&amp;minLat=" + south +
                                            "&amp;maxLat=" + north +
                                            "&amp;fileType=transparent.png" +
                                        "</href>\n" +
                                        "    </Icon>\n" +
                                        "    <LatLonBox>\n" +
                                        "      <west>" + west + "</west>\n" +
                                        "      <east>" + east + "</east>\n" +
                                        "      <south>" + south + "</south>\n" +
                                        "      <north>" + north + "</north>\n" +
                                        "    </LatLonBox>\n" +
                                        "    <visibility>1</visibility>\n" +
                                        "  </GroundOverlay>\n");
                                    sb.append(
                                        getIconScreenOverlay() +
                                        "</Document>\n" +
                                        "</kml>\n");
                                    File2.writeToFileUtf8(dir + name + fileExtension, sb.toString());

                                } else if (extension.equals("transparent.png")) {
                                    //note: I considered making image options part of Grid.saveAs
                                    //But then distributing gridSave as would require
                                    //distributing Sgt package and coastline (huge!) and political outline files!
                                    //For now, keep separate.

                                    //make the image
                                    nLon = grid.lon.length;
                                    nLat = grid.lat.length;
                                    BufferedImage bufferedImage = 
                                        SgtUtil.getBufferedImage(nLon, nLat);
                                    Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();
                                   
                                    //fill with 128,128,128 --> later convert to transparent
                                    //Not a great approach to the problem.
                                    //Gray isn't used by any palette (except gray scale)
                                    //and no data set has gray scale as default 
                                    Color gray = SgtMap.oceanColor;
                                    g2D.setColor(gray);
                                    g2D.fillRect(0, 0, nLon, nLat);

                                    //draw the map
                                    SgtMap.makeCleanMap(
                                        grid.lon[0], grid.lon[grid.lon.length - 1],
                                        grid.lat[0], grid.lat[grid.lat.length - 1],
                                        false, 

                                        grid, 
                                        1, //scaleFactor,
                                        1, //altScaleFactor,
                                        0, //altOffset,
                                        SgtMap.bathymetryCptTrueFullName, //use true so land is transparent

                                        false, false, SgtMap.NO_LAKES_AND_RIVERS, false, false,
                                        g2D, nLon, nLat,
                                        0, 0, nLon, nLat); 

                                    //saveAsTransparentPng
                                    name += "_transparent"; //important for file transfer below
                                    SgtUtil.saveAsTransparentPng(bufferedImage, gray, dir + name);

                                } else if (whichImage >= 0) {
                                    //save as png
                                    bathymetrySaveAsPng(grid, dir, name, nLon, nLat);

                                } else {
                                    //normal File SaveAs
                                    int saveAsType = String2.indexOf(Grid.SAVE_AS_EXTENSIONS, fileExtension); 
                                    if (extension.equals(".asc")) //catch plain .asc
                                        saveAsType = Grid.SAVE_AS_ASCII;
                                    grid.saveAs(dir, name, SgtMap.BATHYMETRY_7NAME.substring(1),
                                        saveAsType, false); //false=zipIt
                                    //insurance
                                    Test.ensureTrue(File2.isFile(dir + name + fileExtension), 
                                        name + fileExtension + " wasn't created."); //will be caught below
                                }

                            //***************************************************************************
                            //*** getStationData
                            } else if (getStationData) {
                                //generate the data file name       
                                String tDataName = FileNameUtility.makeAveragedPointTimeSeriesName(
                                    pointDataSet.internalName, 
                                    'S', //S=always standard units
                                    minLon, maxLon, minLat, maxLat, 
                                    String2.parseDouble(minDepthValue), 
                                    String2.parseDouble(maxDepthValue),
                                    spaceBeginTimeValue, spaceEndTimeValue,
                                    standardTimePeriodValue);
                                name = tDataName;
                                if      (whichImage == 0) name += "_small"; //since imageWidth imageHeight not in name
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //create the table
                                long tTime = System.currentTimeMillis();
                                Table subsetTable = null; 
                                if (File2.touch(dir + tDataName + ".nc")) {
                                    //reuse existing file
                                    String2.log("  GET doQuery reusing datafile " + tDataName + ".nc");
                                    subsetTable = new Table();
                                    subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                } else {
                                    //make table: x,y,z,t,id,data
                                    subsetTable = pointDataSet.makeAveragedTimeSeries(
                                        minLon, maxLon, minLat, maxLat, 
                                        String2.parseDouble(minDepthValue), 
                                        String2.parseDouble(maxDepthValue), 
                                        TBeginTimeValue, TEndTimeValue, standardTimePeriodValue);
                                    String2.log("  GET subsetTable: creating new file: " + tDataName + ".nc");
                                    //String2.log("  columnNames=" + String2.toCSSVString(subsetTable.getColumnNames()));

                                    //only standard units, so no convert to alt units

                                    //save in .nc file
                                    String dimensionName = "row";       
                                    subsetTable.saveAsFlatNc(dir + tDataName + ".nc", dimensionName); 
                                }


                                if (extension.equals(".asc")) {
                                    subsetTable.saveAsTabbedASCII(dir + name + extension); 

                                } else if (extension.equals("GoogleEarth")) { //always standardized here
                                    //Google Earth .kml

                                    //make table, e.g., LON   LAT DEPTH   TIME    ID  WTMP
                                    //remember this may be many stations one time, or one station many times, or many/many
                                    //sort table by lat, lon, depth, then time
                                    subsetTable.leftToRightSort(4);

                                    //based on kmz example from http://www.coriolis.eu.org/cdc/google_earth.htm
                                    //see copy in bob's c:/programs/kml/SE-LATEST-MONTH-STA.kml
                                    //kml docs: https://developers.google.com/kml/documentation/kmlreference
                                    StringBuilder sb = new StringBuilder();
                                    //kml/description docs recommend \n<br />
                                    String averagesString = 
                                        TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " averages";
                                    String nbrCourtesy = pointDataSet.courtesy.length() == 0? "" : 
                                        "\n<br />Data courtesy of: " + pointDataSet.courtesy;
                                    sb.append(
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                                        "<Document>\n" +
                                        //human-friendly, but descriptive, <name>
                                        //name is used as link title -- leads to <description> 
                                        "  <name>" + XML.encodeAsXML(pointDataSet.boldTitle) + ", " + standardTimePeriodValue + 
                                            averagesString + "</name>\n" +
                                        //<description> appears in balloon
                                        "  <description><![CDATA[" +
                                        "    Variable: " + subsetTable.getColumnName(5) + " = " + 
                                            XML.encodeAsXML(pointDataSet.boldTitle) + 
                                        "\n<br />    Time Period: " + standardTimePeriodValue + averagesString +
                                        "\n<br />    Minimum Depth: " + minDepthValue + " m" +
                                        "\n<br />    Maximum Depth: " + maxDepthValue + " m" +
                                        nbrCourtesy + 
                                        //link to download data
                                        "\n<br />    <a href=\"" + fullUrl + "?" +
                                            //XML.encodeAsXML isn't ok
                                            "edit=Station+Data+1" +
                                            "&amp;pointDataSet1="    + SSR.minimalPercentEncode(pointDataSet.internalName) +
                                            "&amp;pointDepth1="      + SSR.minimalPercentEncode(minDepthValue) +
                                            "&amp;pointTimePeriod1=" + SSR.minimalPercentEncode(standardTimePeriodValue) +
                                            "&amp;pointCenteredTime1=latest" + 
                                        "\">Download the data</a>\n" +
                                        "    ]]></description>\n" +
                                        "  <open>1</open>\n" +
                                        "  <Style id=\"BUOY ON\">\n" +
                                        "    <IconStyle>\n" +
                                        "      <color>ff0099ff</color>\n" + //abgr   orange
                                        //"      <scale>0.40</scale>\n" +
                                        "      <Icon>\n" +
                                        "        <href>https://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href>\n" +
                                        "      </Icon>\n" +
                                        "    </IconStyle>\n" +
                                        "  </Style>\n" +
                                        "  <Style id=\"BUOY OUT\">\n" +
                                        "    <IconStyle>\n" +
                                        "      <color>ff0099ff</color>\n" +
                                        //"      <scale>0.40</scale>\n" +
                                        "      <Icon>\n" +
                                        "        <href>https://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href>\n" +
                                        "      </Icon>\n" +
                                        "    </IconStyle>\n" +
                                        "    <LabelStyle><scale>0</scale></LabelStyle>\n" +
                                        "  </Style>\n" +
                                        "  <StyleMap id=\"BUOY\">\n" +
                                        "    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n" +
                                        "    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n" +
                                        "  </StyleMap>\n");
                                    
                                    //just one link for each station (same lat,lon/depth):
                                    //LON   LAT DEPTH   TIME    ID  WTMP
                                    //-130.36   42.58   0.0 1.1652336E9 NDBC 46002 met  13.458333174387613
                                    int nRows = subsetTable.nRows();  //there must be at least 1 row
                                    int startRow = 0;
                                    double startLon = subsetTable.getDoubleData(0, startRow);
                                    double startLat = subsetTable.getDoubleData(1, startRow);
                                    double startDepth = subsetTable.getDoubleData(2, startRow);
                                    String averageString = TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " average";
                                    for (int row = 1; row <= nRows; row++) { //yes, 1...n, since looking at previous row
                                        //look for a change in lastLon/Lat/Depth
                                        //???I don't think it should look for change in depth -- on station per latLon
                                        if (row == nRows || 
                                            startLon != subsetTable.getDoubleData(0, row) ||
                                            startLat != subsetTable.getDoubleData(1, row) ||
                                            startDepth != subsetTable.getDoubleData(2, row)) {

                                            //make a placemark for this station
                                            String stationName = subsetTable.getStringData(4, row - 1);
                                            double startLon180 = Math2.anglePM180(startLon);                                            
                                            sb.append(
                                                "  <Placemark>\n" +
                                                "    <name>" + XML.encodeAsXML(stationName) + "</name>\n" +
                                                "    <description><![CDATA[" + 
                                                    //kml/description docs recommend \n<br />
                                                "      Variable: " + subsetTable.getColumnName(5) + //e.g., "WTMP"
                                                        " = " + XML.encodeAsXML(pointDataSet.boldTitle) + 
                                                "\n<br />      Longitude: " + String2.genEFormat10(startLon180) +
                                                "\n<br />      Latitude: " + String2.genEFormat10(startLat) +
                                                "\n<br />      Depth: " + String2.genEFormat6(startDepth) + " m" +
                                                "\n<br />      Time Period: " + standardTimePeriodValue + averagesString +
                                                nbrCourtesy); 

                                            //generic link for this station
                                            String startLink0 = 
                                            //don't use \n for the following lines
                                                "get=" + cleanGetValue + 
                                                "&amp;dataSet="    + SSR.minimalPercentEncode(dataSetValue) +
                                                "&amp;timePeriod=" + SSR.minimalPercentEncode(timePeriodValue) +
                                                "&amp;beginTime=";
                                            String startLink1 =
                                                "&amp;endTime=latest" +
                                                "&amp;minLon=" + startLon180 +
                                                "&amp;maxLon=" + startLon180 +
                                                "&amp;minLat=" + startLat +
                                                "&amp;maxLat=" + startLat +
                                                "&amp;minDepth=" + startDepth +
                                                "&amp;maxDepth=" + startDepth +
                                                "&amp;fileType="; //e.g., medium.png

                                            //add link for last week's data
                                            //too bad tgc is ~ hard coded; better if "latest - 1 month"
                                            String currentIso = Calendar2.getCurrentISODateTimeStringZulu();
                                            GregorianCalendar tgc = Calendar2.isoDateTimeAdd(currentIso, -7, Calendar2.DATE);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">Last week's data</a>");
                                                //+
                                                //" or " + 
                                                //"<a href=\"" + fullUrl + "?" +
                                                //startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                //startLink1 + "medium.png" +
                                                //"\">graph</a>");
                                            //add link for last month's data
                                            tgc = Calendar2.isoDateTimeAdd(currentIso, -1, Calendar2.MONTH);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">Last month's data</a>");
                                            //add link for last year's data
                                            tgc = Calendar2.isoDateTimeAdd(currentIso, -1, Calendar2.YEAR);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">Last year's data</a>");
                                            //add link for all time's data
                                            tgc = Calendar2.isoDateTimeAdd(currentIso, -100, Calendar2.YEAR);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">All data</a>");
                                            sb.append("\n      ]]></description>\n" +
                                                "    <styleUrl>#BUOY</styleUrl>\n" +
                                                "    <Point>\n" +
                                                "      <coordinates>" + 
                                                           startLon180 + "," +
                                                           startLat + "," +
                                                           -startDepth + //- to make it altitude
                                                        "</coordinates>\n" +
                                                "    </Point>\n" +
                                                "  </Placemark>\n");

                                            //so it doesn't zoom in forever on the 1 point
                                            if (startRow == 0 && row == nRows)
                                                sb.append(
                                                    "  <LookAt>\n" +
                                                    "    <longitude>" + startLon180 + "</longitude>\n" +
                                                    "    <latitude>" + startLat + "</latitude>\n" +
                                                    "    <range>2000000</range>\n" + //meters  ~1000 miles
                                                    "  </LookAt>\n");

                                            //reset startRow...
                                            startRow = row;
                                            if (startRow < nRows) {
                                                startLon = subsetTable.getDoubleData(0, startRow);
                                                startLat = subsetTable.getDoubleData(1, startRow);
                                                startDepth = subsetTable.getDoubleData(2, startRow);
                                            }
                                        } //end processing change in lon, lat, or depth
                                    } //end row loop

                                    //end of kml file
                                    sb.append(
                                        getIconScreenOverlay() +
                                        "  </Document>\n" +
                                        "</kml>\n");
                                    File2.writeToFileUtf8(dir + name + fileExtension, sb.toString());

                                } else if (extension.equals(".html")) {

                                    //make query for medium.png to be embedded in html file
                                    //It needs to be query (dynamic) since image file in cache may be deleted.
                                    int tpo = cleanQuery.indexOf("fileType=");
                                    Test.ensureNotEqual(tpo, -1, "'fileType=' not found in cleanQuery=" + cleanQuery + ".");
                                    String tCleanQuery = cleanQuery.substring(0, tpo + 9) + "medium.png"; 

                                    String preTableHtml = 
                                        "<img src=\"" + fullUrl + tCleanQuery + "\" alt=\"Station Data Image\">" +
                                        //"<strong>Station Data</strong>" +
                                        //"\n<br><strong>Data set:</strong> " + 
                                        //    XML.encodeAsXML(pointDataSet.internalName + " = " + pointDataSet.boldTitle) + 
                                        //"\n<br><strong>Time period:</strong> " + standardTimePeriodValue + 
                                        //    (TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " averages") +
                                        //(pointDataSet.courtesy.length() == 0? "" :
                                        //    "\n<br><strong>Data courtesy of:</strong> " + 
                                        //    XML.encodeAsXML(pointDataSet.courtesy)) + 
                                        "\n<p>";

                                    //save as .html
                                    subsetTable.saveAsHtml(dir + name + extension, preTableHtml, "", 
                                        "", Table.BGCOLOR, 1, true, 3, true, false); 

                                } else if (extension.equals(".mat")) {
                                    subsetTable.saveAsMatlab(dir + name + extension, 
                                        pointDataSet.internalName.substring(1)); 
                                } else if (extension.equals(".nc")) {
                                    //save as .nc done above
                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    //save as .nc done above
                                    File2.writeToFileUtf8(dir + name + fileExtension,
                                        NcHelper.ncdump(dir + tDataName + ".nc", "-h"));
                                } else if (whichImage >= 0) {
                                    stationSaveAsPng(pointDataSet, subsetTable, dir, name, 
                                        minLon, maxLon, minLat, maxLat, minDepthValue, maxDepthValue,
                                        spaceBeginTimeValue, spaceEndTimeValue, standardTimePeriodValue,
                                        whichImage);

                                }  else {
                                    Test.error("internal error Browser.doQuery:\n" +
                                        "unexpected stationData extension: " + extension);
                                }

                            //***************************************************************************
                            //*** getStationVectorData
                            } else if (getStationVectorData) {

                                int absolutePointVectorIndex = -1;
                                String pointVectorInfo[][] = oneOf.pointVectorInfo();
                                for (int i = OneOf.N_DUMMY_OTHER_DATASETS; i < pointVectorInfo.length; i++) {
                                    if (pointVectorInfo[i][OneOf.PVIInternalName].equals(pointVectorInternalName)) {
                                        absolutePointVectorIndex = i;
                                        break;
                                    }
                                }
                                Test.ensureNotEqual(absolutePointVectorIndex, -1, 
                                    "pointVectorInternalName=" + pointVectorInternalName + " wasn't matched.");

                                //generate the data file name       
                                String tDataName = FileNameUtility.makeAveragedPointTimeSeriesName(
                                    xPointDataSet.internalName, 
                                    'S', //S=always standard units
                                    minLon, maxLon, minLat, maxLat, 
                                    String2.parseDouble(minDepthValue), 
                                    String2.parseDouble(maxDepthValue),
                                    spaceBeginTimeValue, spaceEndTimeValue,
                                    standardTimePeriodValue);
                                name = tDataName;
                                if      (whichImage == 0) name += "_small"; //since imageWidth imageHeight not in name
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //ensure the data .nc file exists
                                long tTime = System.currentTimeMillis();
                                Table subsetTable = null;  //table created here if convenient; otherwise not
                                if (File2.touch(dir + tDataName + ".nc")) {
                                    //reuse existing file
                                    String2.log("  GET doQuery reusing datafile " + tDataName + ".nc");
                                } else {
                                    //make table: x,y,z,t,id,udata,vdata
                                    subsetTable = PointVectors.makeAveragedTimeSeries(
                                        xPointDataSet, yPointDataSet, 
                                        minLon, maxLon, minLat, maxLat, 
                                        String2.parseDouble(minDepthValue), 
                                        String2.parseDouble(maxDepthValue), 
                                        TBeginTimeValue, TEndTimeValue, standardTimePeriodValue);
                                    String2.log("  GET subsetTable: creating new file: " + tDataName + ".nc");
                                    //String2.log("  columnNames=" + String2.toCSSVString(subsetTable.getColumnNames()));

                                    //only standard units, so no convert to alt units

                                    //save in .nc file
                                    String dimensionName = "row";       
                                    subsetTable.saveAsFlatNc(dir + tDataName + ".nc", dimensionName); 
                                }


                                //save the data in a file
                                if (error != null) {   
                                    //let it fall through
                                } else if (File2.touch(dir + name + fileExtension)) {
                                    //reuse existing file
                                    String2.log("  GET doQuery reusing datafile " + name + ".nc");
                                } else if (extension.equals(".asc")) {
                                    //make table
                                    if (subsetTable == null) {
                                        subsetTable = new Table();
                                        subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }
                                    subsetTable.saveAsTabbedASCII(dir + name + extension); 
                                } else if (extension.equals("GoogleEarth")) { //always standardized here
                                    //Google Earth .kml

                                    //make table, e.g., LON   LAT DEPTH   TIME    ID  udata vdata
                                    //remember this may be many stations one time, or one station many times, or many/many
                                    if (subsetTable == null) {
                                        subsetTable = new Table();
                                        subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }

                                    //sort by lat, lon, depth, then time
                                    subsetTable.leftToRightSort(4);

                                    //based on kmz example from http://www.coriolis.eu.org/cdc/google_earth.htm
                                    //see copy in bob's c:/programs/kml/SE-LATEST-MONTH-STA.kml
                                    //kml docs: https://developers.google.com/kml/documentation/kmlreference
                                    StringBuilder sb = new StringBuilder();
                                    //kml/description docs recommend \n<br />
                                    String averagesString = TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : 
                                        " averages";
                                    String nbrCourtesy = xPointDataSet.courtesy.length() == 0? "" : 
                                        "\n<br />Data courtesy of: " + xPointDataSet.courtesy;
                                    sb.append(
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                                        "<Document>\n" +
                                        //human-friendly, but descriptive, <name>
                                        //name is used as link title -- leads to <description> 
                                        "  <name>" + XML.encodeAsXML(pointVectorOption) + ", " + standardTimePeriodValue + 
                                            averagesString + "</name>\n" +
                                        //<description> appears in help balloon
                                        "  <description><![CDATA[" +
                                        "Variables: " + 
                                        "\n<br />    &nbsp; " + subsetTable.getColumnName(5) + 
                                            " = " + XML.encodeAsXML(xPointDataSet.boldTitle) +  
                                        "\n<br />    &nbsp; " + subsetTable.getColumnName(6) + 
                                            " = " + XML.encodeAsXML(yPointDataSet.boldTitle) +
                                        "\n<br />    Time Period: " + standardTimePeriodValue + averagesString +
                                        "\n<br />    Minimum Depth: " + minDepthValue + " m" +
                                        "\n<br />    Maximum Depth: " + maxDepthValue + " m" +
                                        nbrCourtesy + 
                                        //link to download data
                                        "\n<br />    <a href=\"" + fullUrl + "?" +
                                            //XML.encodeAsXML isn't ok
                                            "edit=Station+Vector+Data" +
                                            "&amp;pointVectorDataSet="    + SSR.minimalPercentEncode(pointVectorInternalName) +
                                            "&amp;pointVectorDepth="      + SSR.minimalPercentEncode(minDepthValue) +
                                            "&amp;pointVectorTimePeriod=" + SSR.minimalPercentEncode(standardTimePeriodValue) +
                                            "&amp;pointVectorCenteredTime=latest" + 
                                        "\">Download the data</a>\n" +
                                        "\n    ]]></description>\n" +
                                        "  <open>1</open>\n" +
                                        "  <Style id=\"BUOY ON\">\n" +
                                        "    <IconStyle>\n" +
                                        "      <color>ff0099ff</color>\n" + //abgr   orange
                                        //"      <scale>0.40</scale>\n" +
                                        "      <Icon>\n" +
                                        "        <href>https://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href>\n" +
                                        "      </Icon>\n" +
                                        "    </IconStyle>\n" +
                                        "  </Style>\n" +
                                        "  <Style id=\"BUOY OUT\">\n" +
                                        "    <IconStyle>\n" +
                                        "      <color>ff0099ff</color>\n" +
                                        //"      <scale>0.40</scale>\n" +
                                        "      <Icon>\n" +
                                        "        <href>https://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href>\n" +
                                        "      </Icon>\n" +
                                        "    </IconStyle>\n" +
                                        "    <LabelStyle><scale>0</scale></LabelStyle>\n" +
                                        "  </Style>\n" +
                                        "  <StyleMap id=\"BUOY\">\n" +
                                        "    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n" +
                                        "    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n" +
                                        "  </StyleMap>\n");
                                    
                                    //just one link for each station (same lat,lon/depth):
                                    //LON   LAT DEPTH   TIME    ID  udata vdata
                                    //-130.36   42.58   0.0 1.1652336E9 NDBC 46002 met  13.458333174387613
                                    int nRows = subsetTable.nRows();  //there must be at least 1 row
                                    int startRow = 0;
                                    double startLon = subsetTable.getDoubleData(0, startRow);
                                    double startLat = subsetTable.getDoubleData(1, startRow);
                                    double startDepth = subsetTable.getDoubleData(2, startRow);
                                    String averageString = TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " average";
                                    for (int row = 1; row <= nRows; row++) { //yes, 1...n, since looking at previous row
                                        //look for a change in lastLon/Lat/Depth
                                        //???I don't think it should look for change in depth -- on station per latLon
                                        if (row == nRows || 
                                            startLon != subsetTable.getDoubleData(0, row) ||
                                            startLat != subsetTable.getDoubleData(1, row) ||
                                            startDepth != subsetTable.getDoubleData(2, row)) {

                                            //make a placemark for this station
                                            String stationName = subsetTable.getStringData(4, row - 1);
                                            double startLon180 = Math2.anglePM180(startLon);                                            
                                            sb.append(
                                                "  <Placemark>\n" +
                                                "    <name>" + XML.encodeAsXML(stationName) + "</name>\n" +
                                                "    <description><![CDATA[" + 
                                                    //kml/description docs recommend \n<br />
                                                "Variables: " + 
                                                "\n<br />      &nbsp; " + subsetTable.getColumnName(5) + " = " + XML.encodeAsXML(xPointDataSet.boldTitle) +  
                                                "\n<br />      &nbsp; " + subsetTable.getColumnName(6) + " = " + XML.encodeAsXML(yPointDataSet.boldTitle) +
                                                "\n<br />      Longitude: " + String2.genEFormat10(startLon180) +
                                                "\n<br />      Latitude: " + String2.genEFormat10(startLat) +
                                                "\n<br />      Depth: " + String2.genEFormat6(startDepth) + " m" +
                                                "\n<br />      Time Period: " + standardTimePeriodValue + averagesString +
                                                nbrCourtesy); 

                                            //generic link for this station
                                            String startLink0 = 
                                                "get="             + SSR.minimalPercentEncode(cleanGetValue) + 
                                                "&amp;dataSet="    + SSR.minimalPercentEncode(dataSetValue) +
                                                "&amp;timePeriod=" + SSR.minimalPercentEncode(timePeriodValue) + 
                                                "&amp;beginTime=";
                                            String startLink1 =
                                                "&amp;endTime=latest" +
                                                "&amp;minLon=" + startLon180 +
                                                "&amp;maxLon=" + startLon180 +
                                                "&amp;minLat=" + startLat +
                                                "&amp;maxLat=" + startLat +
                                                "&amp;minDepth=" + startDepth +
                                                "&amp;maxDepth=" + startDepth +
                                                "&amp;fileType="; //e.g., .html

                                            //add link for last week's data
                                            //too bad tgc is ~ hard coded; better if "latest - 1 month"
                                            String currentIso = Calendar2.getCurrentISODateTimeStringZulu();
                                            GregorianCalendar tgc = Calendar2.isoDateTimeAdd(currentIso, -7, Calendar2.DATE);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                     //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">Last week's data</a>");
                                            //add link for last month's data
                                            tgc = Calendar2.isoDateTimeAdd(currentIso, -1, Calendar2.MONTH);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">Last month's data</a>");
                                            //add link for last year's data
                                            tgc = Calendar2.isoDateTimeAdd(currentIso, -1, Calendar2.YEAR);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">Last year's data</a>");
                                            //add link for all time's data
                                            tgc = Calendar2.isoDateTimeAdd(currentIso, -100, Calendar2.YEAR);
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" +
                                                    //XML.encodeAsXML isn't ok
                                                    startLink0 + Calendar2.formatAsISODateTimeT(tgc) + 
                                                    startLink1 + ".html" +
                                                "\">All data</a>");
                                            sb.append("\n      ]]></description>\n" +
                                                "    <styleUrl>#BUOY</styleUrl>\n" +
                                                "    <Point>\n" +
                                                "      <coordinates>" + 
                                                           startLon180 + "," +
                                                           startLat + "," +
                                                           -startDepth + //- to make it altitude
                                                        "</coordinates>\n" +
                                                "    </Point>\n" +
                                                "  </Placemark>\n");

                                            //so it doesn't zoom in forever on the 1 point
                                            if (startRow == 0 && row == nRows)
                                                sb.append(
                                                    "  <LookAt>\n" +
                                                    "    <longitude>" + startLon180 + "</longitude>\n" +
                                                    "    <latitude>" + startLat + "</latitude>\n" +
                                                    "    <range>2000000</range>\n" + //meters  ~1000 miles
                                                    "  </LookAt>\n");
                                            
                                            //reset startRow...
                                            startRow = row;
                                            if (startRow < nRows) {
                                                startLon = subsetTable.getDoubleData(0, startRow);
                                                startLat = subsetTable.getDoubleData(1, startRow);
                                                startDepth = subsetTable.getDoubleData(2, startRow);
                                            }
                                        } //end processing change in lon, lat, or depth
                                    } //end row loop

                                    //end of kml file
                                    sb.append(
                                        getIconScreenOverlay() +
                                        "  </Document>\n" +
                                        "</kml>\n");
                                    File2.writeToFileUtf8(dir + name + fileExtension, sb.toString());

                                } else if (extension.equals(".html")) {
                                    //make table
                                    if (subsetTable == null) {
                                        subsetTable = new Table();
                                        subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }

                                    //make query for medium.png to be embedded in html file
                                    //It needs to be query (dynamic) since image file in cache may be deleted.
                                    int tpo = cleanQuery.indexOf("fileType=");
                                    Test.ensureNotEqual(tpo, -1, "'fileType=' not found in cleanQuery=" + cleanQuery + ".");
                                    String tCleanQuery = cleanQuery.substring(0, tpo + 9) + "medium.png"; 

                                    //String tOption = pointVectorOption;
                                    //if (tOption.endsWith("*"))
                                    //    tOption = tOption.substring(0, tOption.length() - 1);
                                    String preTableHtml = 
                                        "<img src=\"" + fullUrl + tCleanQuery + "\" alt=\"Station Vector Image\">" +
                                        //"<strong>Station Vector Data</strong>" +
                                        //"\n<br><strong>Data set:</strong> " + XML.encodeAsHTML(tOption) + 
                                        //"\n<br>&nbsp; &nbsp; " + 
                                        //    XML.encodeAsHTML(xPointDataSet.internalName + " = " + xPointDataSet.boldTitle) +
                                        //"\n<br>&nbsp; &nbsp; " + 
                                        //    XML.encodeAsHTML(yPointDataSet.internalName + " = " + yPointDataSet.boldTitle) +
                                        //"\n<br><strong>Time period:</strong> " + standardTimePeriodValue + 
                                        //    (TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " averages") +
                                        //(xPointDataSet.courtesy.length() == 0? "" :
                                        //    "\n<br><strong>Data courtesy of:</strong> " + 
                                        //    XML.encodeAsHTML(xPointDataSet.courtesy)) + 
                                        "\n<p>";

                                    //save in .html file
                                    subsetTable.saveAsHtml(dir + name + extension, preTableHtml, "", 
                                        "", Table.BGCOLOR, 1, true, 3, true, false); 

                                } else if (extension.equals(".mat")) {
                                    //make table
                                    if (subsetTable == null) {
                                        subsetTable = new Table();
                                        subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }
                                    subsetTable.saveAsMatlab(dir + name + extension, 
                                        xPointDataSet.internalName.substring(1)); 
                                } else if (extension.equals(".nc")) {
                                    //already done above
                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    //first save as .nc
                                    File2.writeToFileUtf8(dir + name + fileExtension,
                                        NcHelper.ncdump(dir + tDataName + ".nc", "-h"));
                                } else if (whichImage >= 0) {
                                    //make table
                                    if (subsetTable == null) {
                                        subsetTable = new Table();
                                        subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                    }

                                    //save as png
                                    stationVectorSaveAsPng(pointVectorInfo, 
                                        absolutePointVectorIndex, xPointDataSet,
                                        subsetTable, dir, name, 
                                        minLon, maxLon, minLat, maxLat, minDepthValue, maxDepthValue,
                                        spaceBeginTimeValue, spaceEndTimeValue, standardTimePeriodValue,
                                        whichImage);

                                }  else {
                                    Test.error("internal error Browser.doQuery:\n" +
                                        "unexpected stationData extension: " + extension);
                                }

                            //***************************************************************************
                            //*** getTrajectoryData
                            } else if (getTrajectoryData) {
                                //generate the data file name       
                                String tDataName = FileNameUtility.makeTrajectoryName(
                                    tableDataSet.internalName(), 
                                    individualsValue, dataVariablesValue);
                                name = tDataName;
                                if      (whichImage == 0) name += "_small"; //since imageWidth imageHeight not in name
                                else if (whichImage == 1) name += "_medium";
                                else if (whichImage == 2) name += "_large";

                                //create the table
                                long tTime = System.currentTimeMillis();
                                Table subsetTable = null; 
                                if (File2.touch(dir + tDataName + ".nc")) {
                                    //reuse existing file
                                    String2.log("  GET doQuery reusing datafile " + tDataName + ".nc");
                                    subsetTable = new Table();
                                    subsetTable.readFlatNc(dir + tDataName + ".nc", null, 1); //standardizeWhat=1
                                } else {
                                    //make table: x,y,z,t,id,dataVariables
                                    subsetTable = tableDataSet.makeSubset(null, null,
                                        individualsValue, dataVariablesValue);
                                    String2.log("  GET subsetTable: creating new file: " + tDataName + ".nc");
                                    //String2.log("  columnNames=" + String2.toCSSVString(subsetTable.getColumnNames()));

                                    //only standard units, so no convert to alt units

                                    //save in .nc file
                                    String dimensionName = "row";       
                                    subsetTable.saveAsFlatNc(dir + tDataName + ".nc", dimensionName); 
                                }


                                if (extension.equals(".asc")) {
                                    subsetTable.saveAsTabbedASCII(dir + name + extension); 

                                } else if (extension.equals("GoogleEarth")) { //always standardized here
                                    //Google Earth .kml

                                    //make table, e.g., LON   LAT DEPTH   TIME    ID  WTMP
                                    //remember this may be one or many trajectories

                                    //based on kmz example from http://www.coriolis.eu.org/cdc/google_earth.htm
                                    //see copy in bob's c:/programs/kml/SE-LATEST-MONTH-STA.kml
                                    //kml docs: https://developers.google.com/kml/documentation/kmlreference
                                    StringBuilder sb = new StringBuilder();
                                    //kml/description docs recommend \n<br />
                                    String nbrCourtesy = tableDataSet.courtesy().length() == 0? "" : 
                                        "\n<br />Data courtesy of: " + tableDataSet.courtesy();
                                    sb.append(
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                                        "<Document>\n" +
                                        //human-friendly, but descriptive, <name>
                                        //name is used as link title -- leads to <description> 
                                        "  <name>" + XML.encodeAsXML(tableDataSet.datasetName()) + "</name>\n" +
                                        //<description> appears in balloon
                                        "  <description><![CDATA[" +
                                        "\n<br />    Individuals: " + XML.encodeAsXML(String2.toCSSVString(individualsValue)) +
                                        nbrCourtesy + 
                                        //link to download data
                                        "\n<br />    <a href=\"" + fullUrl + "?" +
                                            //XML.encodeAsXML isn't ok
                                            "edit=Trajectory+Data+1" +
                                            "&amp;trajectoryDataSet1="     + SSR.minimalPercentEncode(tableDataSet.internalName()) +
                                            "&amp;trajectoryIndividuals1=" + SSR.minimalPercentEncode(individualsValue[0]) +
                                        "\">Download the data</a>\n" +
                                        "    ]]></description>\n" +
                                        "  <open>1</open>\n" +
                                        "  <Style id=\"BUOY ON\">\n" +
                                        "    <IconStyle>\n" +
                                        "      <color>ff0099ff</color>\n" + //abgr   orange
                                        //"      <scale>0.40</scale>\n" +
                                        "      <Icon>\n" +
                                        "        <href>https://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href>\n" +
                                        "      </Icon>\n" +
                                        "    </IconStyle>\n" +
                                        "  </Style>\n" +
                                        "  <Style id=\"BUOY OUT\">\n" +
                                        "    <IconStyle>\n" +
                                        "      <color>ff0099ff</color>\n" +
                                        //"        <scale>0.40</scale>\n" +
                                        "      <Icon>\n" +
                                        "        <href>https://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href>\n" +
                                        "      </Icon>\n" +
                                        "    </IconStyle>\n" +
                                        "    <LabelStyle><scale>0</scale></LabelStyle>\n" +
                                        "  </Style>\n" +
                                        "  <StyleMap id=\"BUOY\">\n" +
                                        "    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n" +
                                        "    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n" +
                                        "  </StyleMap>\n");
                                    

                                    //It should be necessary to look for adjacent lon,lat,depth unchanging,
                                    //   but it doesn't hurt.
                                    //a link for each point:
                                    //LON   LAT DEPTH   TIME    ID  WTMP
                                    //-130.36   42.58   0.0 1.1652336E9 NDBC 46002 met  13.458333174387613
                                    int nRows = subsetTable.nRows();  //there must be at least 1 row
                                    int startRow = 0;
                                    double startLon = subsetTable.getDoubleData(0, startRow);
                                    double startLat = subsetTable.getDoubleData(1, startRow);
                                    double startDepth = subsetTable.getDoubleData(2, startRow);
                                    for (int row = 1; row <= nRows; row++) { //yes, 1...n, since looking at previous row
                                        //look for a change in lastLon/Lat/Depth
                                        //???I don't think it should look for change in depth -- on station per latLon
                                        if (row == nRows || 
                                            startLon != subsetTable.getDoubleData(0, row) ||
                                            startLat != subsetTable.getDoubleData(1, row) ||
                                            startDepth != subsetTable.getDoubleData(2, row)) {

                                            //make a placemark for this station
                                            String sourceID = subsetTable.getStringData(4, row - 1);
                                            double startLon180 = Math2.anglePM180(startLon);                                            
                                            sb.append(
                                                "  <Placemark>\n" +
                                                "    <name>" + sourceID + "</name>\n" +
                                                "    <description><![CDATA[" + 
                                                    //kml/description docs recommend \n<br />
                                                "      DataSet: " + XML.encodeAsXML(tableDataSet.datasetName()) + 
                                                "\n<br />      Individual: " + XML.encodeAsXML(sourceID) +
                                                "\n<br />      Longitude: " + String2.genEFormat10(startLon180) +
                                                "\n<br />      Latitude: " + String2.genEFormat10(startLat) +
                                                "\n<br />      Depth: " + String2.genEFormat6(startDepth) + " m" +
                                                nbrCourtesy); 

                                            //add link for data
                                            sb.append("\n<br />      " + 
                                                "<a href=\"" + fullUrl + "?" + //don't use \n for the following lines
                                                    //XML.encodeAsXML isn't ok
                                                    "get="              + SSR.minimalPercentEncode(cleanGetValue) + 
                                                    "&amp;dataSet="     + SSR.minimalPercentEncode(dataSetValue) +
                                                    "&amp;individuals=" + SSR.minimalPercentEncode(String2.replaceAll(
                                                        String2.toCSSVString(individualsValue), " ", "")) +
                                                    "&amp;dataVariables=" + //none specified -> all
                                                    "&amp;fileType=.html" +
                                                ">All data</a>\n");
                                            sb.append("\n      ]]></description>\n" +
                                                "    <styleUrl>#BUOY</styleUrl>\n" +
                                                "    <Point>\n" +
                                                "      <coordinates>" + 
                                                           startLon180 + "," +
                                                           startLat + "," +
                                                           -startDepth + //- to make it altitude
                                                        "</coordinates>\n" +
                                                "    </Point>\n" +
                                                "  </Placemark>\n");

                                            //so it doesn't zoom in forever on the 1 point
                                            if (startRow == 0 && row == nRows)
                                                sb.append(
                                                    "  <LookAt>\n" +
                                                    "    <longitude>" + startLon180 + "</longitude>\n" +
                                                    "    <latitude>" + startLat + "</latitude>\n" +
                                                    "    <range>2000000</range>\n" + //meters  ~1000 miles
                                                    "  </LookAt>\n");

                                            //reset startRow...
                                            startRow = row;
                                            if (startRow < nRows) {
                                                startLon = subsetTable.getDoubleData(0, startRow);
                                                startLat = subsetTable.getDoubleData(1, startRow);
                                                startDepth = subsetTable.getDoubleData(2, startRow);
                                            }
                                        } //end processing change in lon, lat, or depth
                                    } //end row loop

                                    //end of kml file
                                    sb.append(
                                        getIconScreenOverlay() +
                                        "  </Document>\n" +
                                        "</kml>\n");
                                    File2.writeToFileUtf8(dir + name + fileExtension, sb.toString());

                                } else if (extension.equals(".html")) {

                                    //make query for medium.png to be embedded in html file
                                    //It needs to be query (dynamic) since image file in cache may be deleted.
                                    int tpo = cleanQuery.indexOf("fileType=");
                                    Test.ensureNotEqual(tpo, -1, "'fileType=' not found in cleanQuery=" + cleanQuery + ".");
                                    String tCleanQuery = cleanQuery.substring(0, tpo + 9) + "medium.png"; 

                                    String preTableHtml = 
                                        "<img src=\"" + fullUrl + tCleanQuery + "\" alt=\"Trajectory Data Image\">" +
                                        //"<strong>Station Data</strong>" +
                                        //"\n<br><strong>Data set:</strong> " + 
                                        //    XML.encodeAsHTML(pointDataSet.internalName + " = " + pointDataSet.boldTitle) + 
                                        //"\n<br><strong>Time period:</strong> " + standardTimePeriodValue + 
                                        //    (TimePeriods.getNHours(standardTimePeriodValue) == 0? "" : " averages") +
                                        //(pointDataSet.courtesy.length() == 0? "" :
                                        //    "\n<br><strong>Data courtesy of:</strong> " + 
                                        //    XML.encodeAsHTML(pointDataSet.courtesy)) + 
                                        "\n<p>";

                                    //save as .html
                                    subsetTable.saveAsHtml(dir + name + extension, preTableHtml, "", 
                                        "", Table.BGCOLOR, 1, true, 3, true, false); 

                                } else if (extension.equals(".mat")) {
                                    subsetTable.saveAsMatlab(dir + name + extension, 
                                        individualsValue[0]); 
                                } else if (extension.equals(".nc")) {
                                    //save as .nc done above
                                } else if (extension.equals(".ncHeader")) { //always standardized here
                                    //save as .nc done above
                                    File2.writeToFileUtf8(dir + name + fileExtension,
                                        NcHelper.ncdump(dir + tDataName + ".nc", "-h"));
                                } else if (whichImage >= 0) {
                                    trajectorySaveAsPng(tableDataSet, subsetTable, dir, name, 
                                        individualsValue, whichImage);

                                }  else {
                                    Test.error("internal error Browser.doQuery:\n" +
                                        "unexpected trajectoryData extension: " + extension);
                                }
                            }

                        } catch (Exception e) {
                            error = MustBe.throwableToString(e);
                            if (error.indexOf(MustBe.THERE_IS_NO_DATA) >= 0)
                                error = "<strong>" + MustBe.THERE_IS_NO_DATA + "</strong>";
                            else if (error.indexOf(oneOf.sorryNoFgdcInfo()) >= 0)
                                error = "<strong>" + oneOf.sorryNoFgdcInfo() + "</strong>";
                            else error = String2.replaceAll(
                                "Error while creating the file \"" + 
                                    name + fileExtension + "\":\n" + error,
                                "\n", "\n<br>");
                        }

                        //***************************************************************************
                        //file creation was successful, we're going to send the data
                        //content/mime types, see: 
                        //http://www.webmaster-toolkit.com/mime-types.shtml (several zip options)
                        //common types http://html.megalink.com/programmer/pltut/plMimeTypes.html 
                        //    (recommends application/zip)
                        if (error == null) {
                            //setContentType(mimeType)
                            //User won't want hassle of saving file (changing fileType, ...).
                            String2.log("  GET file creation successful. setContentType...");
                            if (fileExtension.equals(".gif")) {
                                response.setContentType("image/gif");                                 
                            } else if (fileExtension.equals(".png")) {
                                response.setContentType("image/png");                                 
                            } else if (//extension.equals(".asc") || //
                                extension.equals(".ncHeader") //always standardized here
                                // || extension.equals(".xyz")
                                ) {
                                response.setContentType("text/plain"); 
                            } else if (extension.equals("FGDC")) { //always standardized here
                                response.setContentType("text/xml"); //"text/xml"); //text/plain allows viewing in browser
                            } else if (extension.equals("GoogleEarth")) {
                                //see https://developers.google.com/kml/documentation/kml_tut
                                //which lists both of these content types (in different places)
                                //application/keyhole is used by the pydap example that works
                                //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
                                //response.setContentType("application/vnd.google-earth.kml+xml"); 
                                //Opera says handling program is "Opera"!  But I added the mime type to Opera manually.
                                response.setContentType("application/vnd.google-earth.kml+xml");
                            } else if (extension.equals(".html")) { //always standardized here
                                response.setContentType("text/html"); 
                            } else { 
                                response.setContentType("application/x-download");  //or "application/octet" ?
                                //how specify file name in popup window that user is shown? 
                                //see http://forum.java.sun.com/thread.jspa?threadID=696263&messageID=4043287
                            }

                            if (!extension.equals("FGDC") && 
                                !extension.equals(".ncHeader") &&
                                !extension.equals(".html")) {
                                response.setHeader("Content-Disposition","attachment;filename=" + 
                                    name + fileExtension);
                            }

                            //transfer the file to response.getOutputStream
                            //Compress the output stream if user request says it is allowed.
                            //See http://www.websiteoptimization.com/speed/tweak/compress/
                            //This makes sending raw file (even ASCII) as efficient as sending a zipped file
                            //   and user doesn't have to unzip the file.
                            // /* not tested yet. test after other things are working. test them individually
                            //Accept-Encoding should be a csv list of acceptable encodings.
                            String encoding = request.getHeader("Accept-Encoding");  //case insensitive
                            encoding = encoding == null? "" : encoding.toLowerCase();
                            OutputStream out = new BufferedOutputStream(response.getOutputStream());
                            if (fileExtension.equals(".gif") || fileExtension.equals(".png")) {
                                //no compression  (gifs and pngs are already compressed)
                            //ZipOutputStream too finicky.  outputStream.closeEntry() MUST be called at end or it fails
                            //} else if (encoding.indexOf("compress") >= 0) {
                            //    String2.log("  using encoding=compress"); 
                            //    response.setHeader("Content-Encoding", "compress");
                            //    out = new ZipOutputStream(out);
                            //    ((ZipOutputStream)out).putNextEntry(new ZipEntry(name + fileExtension));
                            } else if (encoding.indexOf("gzip") >= 0) {
                                String2.log("    using encoding=gzip"); 
                                response.setHeader("Content-Encoding", "gzip");
                                out = new GZIPOutputStream(out);
                            } else if (encoding.indexOf("deflate") >= 0) {
                                String2.log("    using encoding=deflate"); 
                                response.setHeader("Content-Encoding", "deflate");
                                out = new DeflaterOutputStream(out);
                            } else { //no compression
                            }
                            try {
                                //transfer the file to response.getOutputStream
                                String2.log("  GET copying file to outputStream. file=" + 
                                    dir + name + fileExtension);
                                if (!File2.copy(dir + name + fileExtension, out)) {
                                    //outputStream contentType already set, so can't
                                    //go back to html and display error message
                                    String errorInMethod = String2.ERROR + " in " + 
                                        oneOf.shortClassName() + ".doQuery(\n" +
                                        query + "):\n";
                                    //note than the message is thrown if user cancels the transmission; so don't email to me
                                    String2.log(errorInMethod + "error while transmitting " + name + fileExtension);
                                }
                                
                                //essential
                                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                            } finally {
                                out.close(); 
                            }

                            //collect statistics
                            oneOf.tally().add("HTTP GET queries: get=?", cleanGetValue);
                            if (getGridData) {
                                oneOf.tally().add("HTTP GET queries: get=gridData dataSet=?", dataSetValue);
                                oneOf.tally().add("HTTP GET queries: get=gridData fileType=?", extension);
                            } else if (getGridTimeSeries) {
                                oneOf.tally().add("HTTP GET queries: get=gridTimeSeries dataSet=?", dataSetValue);
                                oneOf.tally().add("HTTP GET queries: get=gridTimeSeries fileType=?", extension);
                            } else if (getStationData) {
                                oneOf.tally().add("HTTP GET queries: get=stationData dataSet=?", dataSetValue);
                                oneOf.tally().add("HTTP GET queries: get=stationData fileType=?", extension);
                            }

                            String2.log("\\\\\\\\**** GET Query successfully finished. TOTAL TIME=" + 
                                (System.currentTimeMillis() - queryTime) + "\n");
                            return true;
                        }

                    } finally {
                        lock.unlock(); //essential                        
                    }
                } 
            } catch (Exception e) {
                String subject = "Unexpected " + String2.ERROR + " in " + oneOf.shortClassName() + "?get"; 
                error = "query: " + query + "\n" + 
                    MustBe.throwableToString(e);
                String2.log(subject + "\n" + error);
                if (error.indexOf("ClientAbortException") < 0) //don't send some errors
                    oneOf.email(oneOf.emailEverythingTo(), subject,
                        String2.replaceAll(error, "\n", "\n<br>"));

            }

            //find the name of the example dataSet on this browser 
            String dataSet7Example = null;
            String dataSetFullExample = null;
            {
                //look for preferred datasets 
                int po = -1;
                if (getGridData || getGridTimeSeries) {
                    po =             String2.indexOf(localShared.activeGridDataSet7Names(), "LGAssta");
                    if (po < 0) po = String2.indexOf(localShared.activeGridDataSet7Names(), "TGAssta");
                } else if (getGridVectorData) {
                    po =             String2.indexOf(localShared.activeVector7Names(), "VLQSu10");
                    if (po < 0) po = String2.indexOf(localShared.activeVector7Names(), "VTQSu10");
                } else if (getStationData) {
                    po = String2.indexOf(localShared.activePointDataSet7Names(), "PNBwtmp");
                } else if (getStationVectorData) {
                    po = String2.indexOf(localShared.activePointVector7Names(), "PVPNBwsp");
                }

                if (po < 0) po = 1;

                //set dataSet7Example and FullExample 
                dataSet7Example = 
                    getGridData || getGridTimeSeries? localShared.activeGridDataSet7Names()[po] :
                    getGridVectorData? localShared.activeVector7Names()[po] :
                    getStationData? localShared.activePointDataSet7Names()[po] :
                    getStationVectorData? localShared.activePointVector7Names()[po] :
                    null;
                dataSetFullExample = 
                    getGridData || getGridTimeSeries? localShared.activeGridDataSetOptions()[po] :
                    getGridVectorData? localShared.activeVectorOptions()[po] :
                    getStationData? localShared.activePointDataSetOptions()[po] :
                    getStationVectorData? localShared.activePointVectorOptions()[po] :
                    null;
            }

            //******************************************************************************
            //flesh out the get=gridData and get=gridVectorData error message
            if (error != null && (getGridData || getGridVectorData)) {            

                String GridOrVector = getGridData? "Grid" : 
                    getGridVectorData? "Vector" :
                    null;

                error = thankYou +
                "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                "<p><strong>There was an error in your <kbd>get=" + cleanGetValue + "</kbd> query:</strong>\n" +
                "<br>" + error + "\n" +
                "<p><strong>General Information for <kbd>get=" + cleanGetValue + "</kbd> Queries</strong>\n";

                error +=
                    getGridData?           "    <br><kbd>get=gridData</kbd> queries allow you to download lat lon gridded data (for example, satellite SST data).\n" :
                    getGridVectorData?     "    <br><kbd>get=gridVectorData</kbd> queries allow you to download lat lon gridded vector data (for example, satellite-derived wind data).\n" :
                    ""; 


                error +=
                "    <br>The format of a <kbd>get=" + cleanGetValue + "</kbd> query is<kbd>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;?get=" + cleanGetValue + "&amp;dataSet=<i>dataSetValue</i>&amp;timePeriod=<i>timePeriodValue</i>&amp;centeredTime=<i>centeredTimeValue</i>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;[&amp;minLon=<i>minLonValue</i>][&amp;maxLon=<i>maxLonValue</i>][&amp;minLat=<i>minLatValue</i>][&amp;maxLat=<i>maxLatValue</i>]\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;[&amp;nLon=<i>nLonValue</i>][&amp;nLat=<i>nLatValue</i>]\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;&amp;fileType=<i>fileTypeValue</i></kbd>\n" +
                "\n" +
                "<p>Notes:<ul>\n" +
                "<li>The easiest way to build a correct query url is to start at\n" +
                "   <a href=\"" + url + "?get\">" + fullUrl + "?get</a> and repeatedly choose\n" +
                "   from the options on the error message web pages.\n" +
                "<li>Queries must not have any internal spaces.\n"+
                "<li>Queries are not case sensitive.\n" +
                "<li>[ ] is notation to denote an optional part of the query.\n" + //sp is needed for CencoosCurrents Browser's font
                "<li><kbd><i>italics</i></kbd> is notation to denote a value specific to your query.\n" +
                "<li><kbd><i>dataSetValue</i></kbd> is the name of a data set. \n" +
                "   <br>To see a list of options, use <kbd>dataSet=</kbd> at the end of a query.\n" +
                "   <br>There are two sets of options: the 7 character \"internal\" data set names\n" +
                "     (for example, <kbd>" + dataSet7Example + "</kbd>) and the Data Set options in the \n" +
                "     <a href=\"" + url + "?edit=" + (getGridVectorData? "Vector" : "Grid") + "%20Data\">\n" +  //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + GridOrVector + " Data</kbd></a> section of the CoastWatch Browser, but with spaces removed\n" +
                "     (for example, <kbd>" + clean(dataSetFullExample) + "</kbd>).\n" +
                "     The 7 character \"internal\" names are preferred, since they are unlikely to ever change.\n" +
                "<li><kbd><i>timePeriodValue</i></kbd> is the name of a time period.\n" +
                "     For data files which represent composites of several day's worth of data,\n" +
                "     the <kbd>timePeriod</kbd> indicates the length of the composite.\n" +
                "     For example, an <kbd>8day</kbd> composite has the average of all data observed\n" +
                "     in an 8 day time period.\n" +
                "   <br>To see a list of options, use <kbd>timePeriod=</kbd> at the end of a query.\n" +
                "   <br>The options are the same as the Time Period options specific to a given Data Set in the \n" +
                "     <a href=\"" + url + "?edit=" + GridOrVector + "%20Data\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + GridOrVector + " Data</kbd></a> section of the CoastWatch Browser, but with spaces removed.\n" +
                "<li><kbd><i>centeredTimeValue</i></kbd> is the centered date/time for the Time Period, in ISO 8601 format:\n" +
                "   <i>YYYY-MM-DD</i> or <i>YYYY-MM-DD</i>T<i>hh:mm:ss</i> (note the literal \"T\" between the date and time).\n" +
                "   For example, <kbd>2006-04-11T00:00:00</kbd>.\n" +
                "   <br>All times are in the Zulu (also known as UTC or GMT) time zone, not the local time zone.\n" +
                "   <br>The <kbd><i>centeredTimeValue</i></kbd> must exactly match the time of one of the options\n" +
                "     for the current DataSet and TimePeriod.\n" +
                "   <br>To see a list of options, use <kbd>centeredTime=</kbd> at the end of a query.\n" + 
                "   <br>The options are the same as the Centered Time options specific to a given\n" +
                "     Data Set and Time Period in the \n" +
                "     <a href=\"" + url + "?edit=" + GridOrVector + "%20Data\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + GridOrVector + " Data</kbd></a> section of the CoastWatch Browser, but with T's instead of spaces.\n" +
                "   <br>Or, put '~' at the beginning of a <kbd><i>centeredTimeValue</i></kbd>\n" +
                "     (for example, <kbd>~2006-04-11T01:17:33</kbd>) to\n" +
                "     get the <kbd>centeredTime</kbd> which is the closest available <kbd>centeredTime</kbd> to that time.\n" +
                "     WARNING: the closest available time may be far from what you request.\n" +
                "   <br>Or, use the special value, <kbd>latest</kbd>, to get the latest available data.\n" +
                "   <br>The <kbd><i>centeredTimeValue</i></kbd> will be part of the name of the file that you download,\n" +
                "     but with the dashes, the 'T', and the colons removed.\n" +
                "   <br>Or, you can use <kbd>endDate=<i>endDateValue</i></kbd> instead of centeredTime.\n" +
                "     For time periods of 0, 1, 25, or 33 hours, this is the end second (for example, ending in \":00:00\").\n" +
                "     For other time periods, this is the last date in the time period (inclusive).\n" +
                "     The program processes an <kbd><i>endDateValue</i></kbd> by converting it to a\n" +
                "     <kbd><i>centeredTimeValue</i></kbd>.\n" +
                "     You can put '~' at the beginning of an <kbd><i>endDateValue</i></kbd>\n" +
                "     (for example, <kbd>~2006-04-11T01:17:33</kbd>), to get the closest available data.\n" +
                "<li><kbd><i>minLonValue</i></kbd> is the minimum desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>-135.5</kbd> represents 135.5°W.\n" +
                "   <br><kbd>minLon=<i>minLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum longitude for this program (" + oneOf.regionMinX() + ").\n" +
                "   <br>You can specify <kbd><i>minLonValue</i></kbd> and <kbd><i>maxLonValue</i></kbd>\n" +
                "     in the range -180° to 180°, or 0° to 360°,\n" +
                "     regardless of the range of the original data. The program will automatically\n" +
                "     extract and, if necessary, convert the data to your desired range.\n" +
                "   <br>The program does the best it can with invalid \n" +
                "     <kbd><i>minLonValue, maxLonValue, minLatValue, maxLatValue</i></kbd>\n" +
                "     requests. For example, if the\n" +
                "     actual range of the data set is less than you specify, only available data will\n" +
                "     be returned.\n" +
                "   <br>If <kbd><i>minLonValue, maxLonValue, minLatValue,</i></kbd> and/or <kbd><i>maxLatValue</i></kbd>\n" +
                "     fall between two grid points, this system rounds to the nearest grid point.\n" +
                "     Rounding is most appropriate because each grid point represents the center\n" +
                "     a box. Thus the data for a given x,y point may be from a grid point just\n" +
                "     outside of the range you request.\n" +
                "     Similarly, if you request the data for one point (minX=maxX, minY=maxY),\n" +
                "     rounding will return the appropriate grid point (the minX,minY point is in\n" +
                "     a box; the procedure returns the value for the box).\n" +
                "   <br>Even though <kbd>minLon, maxLon, minLat,</kbd> and\n" +
                "     <kbd>maxLat</kbd> are optional, their use\n" +
                "     is STRONGLY RECOMMENDED to minimize the download time.\n" +
                "<li><kbd><i>maxLonValue</i></kbd> is the maximum desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>127.5</kbd> represents 127.5°E. \n" +
                "   <br><kbd>maxLon=<i>maxLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum longitude for this program (" + oneOf.regionMaxX() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>minLatValue</i></kbd> is the minimum desired latitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>-22.25</kbd> represents 22.25°S. \n" +
                "   <br><kbd>minLat=<i>minLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum latitude for this program (" + oneOf.regionMinY() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>maxLatValue</i></kbd> is the maximum desired latitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>40.75</kbd> represents 40.75°N. \n" +
                "   <br><kbd>maxLat=<i>maxLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum latitude for this program (" + oneOf.regionMaxY() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>nLonValue</i></kbd> is the desired number (an integer) of longitude points in the grid.\n" +
                "   <br>This is useful for reducing the amount of data downloaded if, for example,\n" +
                "     you only need the data to make an image that will be some number of pixels wide.\n" +
                "     For example, <kbd>400</kbd>.\n" +
                "   <br><kbd>nLon=<i>nLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum available.\n" +
                "   <br>If the data set doesn't have as many points as you request, the program\n" +
                "     will return the maximum available.\n" +
                "   <br>If the lon range of available data is less than the requested\n" +
                "     <kbd><i>minLonValue</i></kbd> and <kbd><i>maxLonValue</i></kbd>,\n" +
                "     <kbd><i>nLonValue</i></kbd> will be reduced proportionally.\n" +
                "   <br>[Details: Internally, the program uses the largest possible stride value that\n" +
                "     will return at least the number of data points requested.]\n" +
                "   <br>Even though <kbd><i>nLonValue</i></kbd> and <kbd><i>nLatValue</i></kbd> are optional, their use\n" +
                "     is STRONGLY RECOMMENDED to minimize the download time.\n" +
                "   <br>If <kbd>fileType</kbd> is a .png file, this is ignored.\n" +
                "<li><kbd><i>nLatValue</i></kbd> is the desired numer (an integer) of latitude points in the grid.\n" +
                "   For example, <kbd>200</kbd>.\n" +
                "   <br><kbd>nLat=<i>nLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum available.\n" +
                "   <br>See the comments above for <kbd><i>nLonValue</i></kbd>.\n" +
                "<li><kbd><i>fileTypeValue</i></kbd> is the type of data file that you want to download.\n" +
                "   <br>To see a list of options, use <kbd>fileType=</kbd> at the end of a query.\n" +
                "   <br>The file type <kbd>.ncHeader</kbd> is the ncdump-style file header showing all the metadata, but no data.\n" +
                "   <br>If you are using a browser, .ncHeader data will appear as plain text in your browser.\n" +
                "     Other file types will cause a \"Download File\" dialog box to pop up.\n" +
                "   <br>See <a href=\"" + oneOf.gridFileHelpUrl() + "\">a description of the grid data file types</a>.\n" +
                "<li>Matlab users can download data from within Matlab. Here is a 3-line example,\n" +
                "   <br>&nbsp;&nbsp;1) <kbd>link='" + fullUrl + "?" +
                    "get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-06-10" +
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() + 
                    "&amp;fileType=.mat';</kbd>\n" +
                "   <br>&nbsp;&nbsp;2) <kbd>F=urlwrite(link,'test.m');</kbd>\n" +
                "   <br>&nbsp;&nbsp;3) <kbd>load('-MAT',F);</kbd>\n" +
                "   <br>The first line of the example is very long and may be displayed as a few lines in your browser.\n" +
                "</ul>\n" +
                "\n" +
                "Examples of valid queries:\n" +
                "<ul>\n" +
                "<li>To see a list of <kbd>get</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get\"><kbd>" +
                  fullUrl + "?get</kbd></a>\n" +
                "<li>To see a list of <kbd>dataSet</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=</kbd></a>\n" +
                "<li>To see a list of <kbd>timePeriod</kbd> options for the \"" + dataSetFullExample + "\" data set, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=</kbd></a>\n" +
                "<li>To see a list of <kbd>centeredTime</kbd> options for the \"" + dataSetFullExample + "\" data set, 1 day composites, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=</kbd></a>\n" +
                "<li>To see a list of <kbd>fileType</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=</kbd></a>\n" +
                "<li>To get a NetCDF file with 1 day composite (for date/time 2006-04-11T12:00:00) \n" +
                "    of the \"" + dataSetFullExample + "\" data set,\n" +
                "    for a limited geographic range, and subsetted to just include at least\n" +
                "    100 longitude points and 125 latitude points, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-04-11T12:00:00" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;nLon=100&amp;nLat=125&amp;fileType=.nc" +
                  "\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-04-11T12:00:00" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;nLon=100&amp;nLat=125&amp;fileType=.nc" +
                  "</kbd></a>\n" +
                "<li>To get an xyz ASCII file with the data for the latest 1 day composite of the \"" + dataSetFullExample + "\" data set\n" +
                "    for the entire available geographic region, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.xyz\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.xyz</kbd></a>\n" +
                "<li>By using the default minLon, maxLon, minLat, maxLat, this example is equivalent to the previous example:\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=.xyz\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=.xyz</kbd></a>\n" +
                "<li>To get just the ncdump-style header information (metadata) for the data file you have specified:\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=.ncHeader\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=.ncHeader</kbd></a>\n" +
                "<li>To get the data closest to a single lat lon point, set minLon=maxLon and minLat=maxLat:\n" +
                  "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-05-02T12:00:00&amp;minLon=-134.95&amp;maxLon=-134.95&amp;minLat=40.1&amp;maxLat=40.1&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-05-02T12:00:00&amp;minLon=-134.95&amp;maxLon=-134.95&amp;minLat=40.1&amp;maxLat=40.1&amp;fileType=.asc</kbd></a>\n" +
                  "<br>That returns the same data as the query for the nearest actual grid point:\n" +
                  "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-05-02T12:00:00&amp;minLon=-134.925&amp;maxLon=-134.925&amp;minLat=40.125&amp;maxLat=40.125&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-05-02T12:00:00&amp;minLon=-134.925&amp;maxLon=-134.925&amp;minLat=40.125&amp;maxLat=40.125&amp;fileType=.asc</kbd></a>\n" +
                "<li>To get the data closest to a specified time, use a ~ before the centeredTime:\n" +
                  "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=~2006-05-02T00:17&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=~2006-05-02T00:17&amp;fileType=.asc</kbd></a>\n" +
                  "<br>That returns the same data as the query for the nearest actually available centeredTime:\n" +
                  "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-05-02T12:00:00&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=2006-05-02T12:00:00&amp;fileType=.asc</kbd></a>\n" +

                "</ul>\n" +
                "\n";
            }

            //******************************************************************************
            //flesh out the get=gridTimeSeries error message
            if (error != null && getGridTimeSeries) {
                String lonExample = String2.genEFormat6((oneOf.regionMinX() + oneOf.regionMaxX()) / 2);
                String latExample = String2.genEFormat6((oneOf.regionMinY() + oneOf.regionMaxY()) / 2);
                error = thankYou +
                "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                "<p><strong>There was an error in your <kbd>get=gridTimeSeries</kbd> query:</strong>\n" +
                "<br>" + error + "\n" +
                "<p><strong>General Information for <kbd>get=gridTimeSeries</kbd> Queries</strong>\n" +
                "    <br><kbd>get=gridTimeSeries</kbd> queries allow you to download a time series for one lat lon location\n" +
                "       from gridded data (for example, satellite data).\n" +
                "    <br>The format of a <kbd>get=gridTimeSeries</kbd> query is<kbd>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;?get=gridTimeSeries&amp;dataSet=<i>dataSetValue</i>&amp;timePeriod=<i>timePeriodValue</i>&amp;beginTime=<i>beginTimeValue</i>&amp;endTime=<i>endTimeValue</i>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;&amp;lon=<i>lonValue</i>&amp;lat=<i>latValue</i>&amp;fileType=<i>fileTypeValue</i></kbd>\n" +
                "\n" +
                "<p>Notes:<ul>\n" +
                "<li>The easiest way to build a correct query url is to start at\n" +
                "   <a href=\"" + url + "?get\">" + fullUrl + "?get</a> and repeatedly choose\n" +
                "   from the options on the error message web pages.\n" +
                "<li>Queries must not have any internal spaces.\n"+
                "<li>Queries are not case sensitive.\n" +
                "<li>[ ] is notation to denote an optional part of the query.\n" + //sp is needed for CencoosCurrents Browser's font
                "<li><kbd><i>italics</i></kbd> is notation to denote a value specific to your query.\n" +
                "<li><kbd><i>dataSetValue</i></kbd> is the name of a data set. \n" +
                "   <br>To see a list of options, use <kbd>dataSet=</kbd> at the end of a query.\n" +
                "   <br>There are two sets of options: the 7 character \"internal\" data set names\n" +
                "     (for example, <kbd>" + dataSet7Example + "</kbd>) and the Data Set options in the \n" +
                "     <a href=\"" + url + "?edit=Grid%20Data\">\n" +  //doesn't work for CencoosCurrents
                "     <kbd>Edit: Grid Data</kbd></a> section of the CoastWatch Browser, but with spaces removed\n" +
                "     (for example, <kbd>" + clean(dataSetFullExample) + "</kbd>).\n" +
                "     The 7 character \"internal\" names are preferred, since they are unlikely to ever change.\n" +
                "<li><kbd><i>timePeriodValue</i></kbd> is the name of a time period.\n" +
                "     For data files which represent composites of several day's worth of data,\n" +
                "     the <kbd>timePeriod</kbd> indicates the length of the composite.\n" +
                "     For example, an <kbd>3day</kbd> composite has the average of all data observed\n" +
                "     in an 3 day time period.\n" +
                "   <br>To see a list of options, use <kbd>timePeriod=</kbd> at the end of a query.\n" +
                "   <br>The options are the same as the Time Period options specific to a given Data Set in the \n" +
                "     <a href=\"" + url + "?edit=Grid%20Data\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: Grid Data</kbd></a> section of the CoastWatch Browser, but with spaces removed.\n" +
                "<li><kbd><i>beginTimeValue</i></kbd> is the earliest desired time of the Time Period, in ISO 8601 format:\n" +
                "   <i>YYYY-MM-DD</i> or <i>YYYY-MM-DD</i>T<i>hh:mm:ss</i> (note the literal \"T\" between the date and time).\n" +
                "   For example, <kbd>2006-04-11T00:00:00</kbd>.\n" +
                "   <br>All times are in the Zulu (also known as UTC or GMT) time zone, not the local time zone.\n" +
                "   <br>The <kbd><i>beginTimeValue</i></kbd> will be part of the name of the file that you download,\n" +
                "     but with the dashes, the 'T', and the colons removed.\n" +
                "   <br>For example, if the time period is \"3days\"\n" +
                "     and the 3 day composite files are created each day, and the\n" +
                "     <kbd><i>beginTimeValue</i></kbd> is <kbd>2006-04-11</kbd> and the\n" +
                "     <kbd><i>endTimeValue</i></kbd> is <kbd>2006-04-13</kbd>,\n" +
                "     you will get a time series with 2 points (from the composites with\n" + 
                "     centered times of <kbd>2006-04-11T12:00:00</kbd> and <kbd>2006-04-12T12:00:00</kbd>).\n" +
                "   <br>If <kbd><i>beginTimeValue</i></kbd> and <kbd><i>endTimeValue</i></kbd>\n" +
                "     are between two adjacent available times,\n" +
                "     the datum for the single closest time is returned.\n" +
                "   <br><kbd>beginTime</kbd> is optional. If you omit it, the beginTime is\n" +
                "     an appropriate amount of time before the end time (given the <kbd><i>timePeriodValue</i></kbd>).\n" +
                "<li><kbd><i>endTimeValue</i></kbd> is the latest desired time for the Time Period, in ISO 8601 format:\n" +
                "   <i>YYYY-MM-DD</i> or <i>YYYY-MM-DD</i>T<i>hh:mm:ss</i> (note the literal \"T\" between the date and time).\n" +
                "   For example, <kbd>2006-04-11T00:00:00</kbd>.\n" +
                "   <br>All times are in the Zulu (also known as UTC or GMT) time zone, not the local time zone.\n" +
                "   <br>The <kbd><i>endTimeValue</i></kbd> will be part of the name of the file that you download,\n" +
                "     but with the dashes, the 'T', and the colons removed.\n" +
                "   <br><kbd>endTime</kbd> is optional. If you omit it, the endTime is\n" +
                "     the latest available time for the current dataSet.\n" +
                "   <br>Or, use the special value, <kbd>latest</kbd>, to get the latest available data.\n" +
                "   <br>The <kbd><i>endTimeValue</i></kbd> will be part of the name of the file that you download,\n" +
                "     but with the dashes, the 'T', and the colons removed.\n" +
                "<li><kbd><i>lonValue</i></kbd> is the desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>-135.5</kbd> represents 135.5°W.\n" +
                "   <br>You can specify <kbd><i>lonValue</i></kbd>\n" +
                "     in the range -180° to 180°, or 0° to 360°,\n" +
                "     regardless of the range of the original data. The program will automatically\n" +
                "     extract and, if necessary, convert the data to your desired range.\n" +
                "   <br><kbd><i>lonValue</i></kbd> and <kbd><i>latValue</i></kbd> must be within the data's range.\n" +
                "   <br>If <kbd><i>lonValue</i></kbd> and/or <kbd><i>latValue</i></kbd>\n" +
                "     fall between two grid points, this system rounds to the nearest grid point.\n" +
                "     Rounding is most appropriate because each grid point represents the center\n" +
                "     a box. Thus the data for a given x,y point may be from a grid point just\n" +
                "     outside of the range you request.\n" +
                "<li><kbd><i>latValue</i></kbd> is the minimum desired latitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>-22.25</kbd> represents 22.25°S. \n" +
                "<li><kbd><i>fileTypeValue</i></kbd> is the type of data file that you want to download.\n" +
                "   <br>To see a list of options, use <kbd>fileType=</kbd> at the end of a query.\n" +
                "   <br>The file type <kbd>.ncHeader</kbd> is the ncdump-style file header showing all the metadata, but no data.\n" +
                "   <br>If you are using a browser, .ncHeader data will appear as plain text in your browser.\n" +
                "     Other file types will cause a \"Download File\" dialog box to pop up.\n" +
                "   <br>See <a href=\"" + oneOf.pointFileHelpUrl() + "\">a description of the point data file types</a>.\n" +
                "<li>Matlab users can download data from within Matlab. Here is a 3-line example,\n" +
                "   <br>&nbsp;&nbsp;1) <kbd>link='" + url + "?" +
                    "get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day" +
                    "&amp;beginTime=2006-06-10T12:00:00&amp;endTime=2006-06-10T12:00:00" +
                    "&amp;lon=" + lonExample + "&amp;lat=" + latExample + 
                    "&amp;fileType=.mat';</kbd>\n" +
                "   <br>&nbsp;&nbsp;2) <kbd>F=urlwrite(link,'test.m');</kbd>\n" +
                "   <br>&nbsp;&nbsp;3) <kbd>load('-MAT',F);</kbd>\n" +
                "   <br>The first line of the example is very long and may be displayed as a few lines in your browser.\n" +
                "</ul>\n" +
                "\n" +
                "Examples of valid queries:\n" +
                "<ul>\n" +
                "<li>To see a list of <kbd>get</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get\"><kbd>" +
                  fullUrl + "?get</kbd></a>\n" +
                "<li>To see a list of <kbd>dataSet</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=\"><kbd>" +
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=</kbd></a>\n" +
                "<li>To see a list of <kbd>timePeriod</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=\"><kbd>" +
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=</kbd></a>\n" +
                "<li>To see a list of <kbd>fileType</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;beginTime=2006-06-14&amp;endTime=2006-06-16T12:00:00&amp;lon=" + lonExample + "&amp;lat=" + latExample + "&amp;fileType=\"><kbd>" +
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;beginTime=2006-06-14&amp;endTime=2006-06-16T12:00:00&amp;lon=" + lonExample + "&amp;lat=" + latExample + "&amp;fileType=</kbd></a>\n" +
                "<li>To get a NetCDF file, use\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;beginTime=2006-06-14&amp;endTime=2006-06-16T12:00:00&amp;lon=" + lonExample + "&amp;lat=" + latExample + "&amp;fileType=.nc\"><kbd>" +
                  fullUrl + "?get=gridTimeSeries&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;beginTime=2006-06-14&amp;endTime=2006-06-16T12:00:00&amp;lon=" + lonExample + "&amp;lat=" + latExample + "&amp;fileType=.nc</kbd></a>\n" +
                "</ul>\n" +
                "\n";
            }

            //******************************************************************************
            //flesh out the get=bathymetryData error message
            if (error != null && getBathymetryData) {            

                error = thankYou +
                "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                "<p><strong>There was an error in your <kbd>get=bathymetryData</kbd> query:</strong>\n" +
                "<br>" + error + "\n" +
                "<p><strong>General Information for <kbd>get=bathymetryData</kbd> Queries</strong>\n";

                error += "    <br><kbd>get=bathymetryData</kbd> queries allow you to download lat lon gridded bathymetry data (ETOPO2v2).\n"; 

                error +=
                "    <br>The format of a <kbd>get=bathymetryData</kbd> query is<kbd>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;?get=bathymetryData\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;[&amp;minLon=<i>minLonValue</i>][&amp;maxLon=<i>maxLonValue</i>][&amp;minLat=<i>minLatValue</i>][&amp;maxLat=<i>maxLatValue</i>]\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;[&amp;nLon=<i>nLonValue</i>][&amp;nLat=<i>nLatValue</i>]\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;&amp;fileType=<i>fileTypeValue</i></kbd>\n" +
                "\n" +
                "<p>Notes:<ul>\n" +
                "<li>The easiest way to build a correct query url is to start at\n" +
                "   <a href=\"" + url + "?get\">" + fullUrl + "?get</a> and repeatedly choose\n" +
                "   from the options on the error message web pages.\n" +
                "<li>Queries must not have any internal spaces.\n"+
                "<li>Queries are not case sensitive.\n" +
                "<li>[ ] is notation to denote an optional part of the query.\n" + //sp is needed for CencoosCurrents Browser's font
                "<li><kbd><i>italics</i></kbd> is notation to denote a value specific to your query.\n" +
                "<li><kbd><i>minLonValue</i></kbd> is the minimum desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>-135.5</kbd> represents 135.5°W.\n" +
                "   <br><kbd>minLon=<i>minLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum longitude for this program (" + oneOf.regionMinX() + ").\n" +
                "   <br>You can specify <kbd><i>minLonValue</i></kbd> and <kbd><i>maxLonValue</i></kbd>\n" +
                "     in the range -180° to 180°, or 0° to 360°,\n" +
                "     regardless of the range of the original data. The program will automatically\n" +
                "     extract and, if necessary, convert the data to your desired range.\n" +
                "   <br>The resulting grid is created by adjusting the desired min/max lon/lat values and nLon,nLat value\n" +
                "     to the closest etopo2v2g data points and stride, and then populating the grid. \n" +
                "   <br>Even though <kbd>minLon, maxLon, minLat,</kbd> and\n" +
                "     <kbd>maxLat</kbd> are optional, their use\n" +
                "     is STRONGLY RECOMMENDED to minimize the download time.\n" +
                "<li><kbd><i>maxLonValue</i></kbd> is the maximum desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>127.5</kbd> represents 127.5°E. \n" +
                "   <br><kbd>maxLon=<i>maxLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum longitude for this program (" + oneOf.regionMaxX() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>minLatValue</i></kbd> is the minimum desired latitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>-22.25</kbd> represents 22.25°S. \n" +
                "   <br><kbd>minLat=<i>minLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum latitude for this program (" + oneOf.regionMinY() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>maxLatValue</i></kbd> is the maximum desired latitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>40.75</kbd> represents 40.75°N. \n" +
                "   <br><kbd>maxLat=<i>maxLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum latitude for this program (" + oneOf.regionMaxY() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>nLonValue</i></kbd> is the desired number (an integer) of longitude points in the grid.\n" +
                "   <br>This is useful for reducing the amount of data downloaded if, for example,\n" +
                "     you only need the data to make an image that will be some number of pixels wide.\n" +
                "     For example, <kbd>400</kbd>.\n" +
                "   <br><kbd>nLon=<i>nLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum available.\n" +
                "   <br>Currently, to avoid problems associated with very large requests, <kbd><i>nLonValue</i></kbd>\n" +
                "     is limited to 3601 points. <kbd><i>nLatValue</i></kbd> is limited to 1801 points.\n" +
                "     If you need all of the data, please get it from NGDC (see below).\n" +
                "   <br>If the data set doesn't have as many points as you request, the program\n" +
                "     will return the maximum available.\n" +
                "   <br>This procedure finds the lowest stride value which will return at least <kbd><i>nLonValue</i></kbd> points.\n" +
                "   <br>Even though <kbd><i>nLonValue</i></kbd> and <kbd><i>nLatValue</i></kbd> are optional, their use\n" +
                "     is STRONGLY RECOMMENDED to minimize the download time.\n" +
                "   <br>If <kbd>fileType</kbd> is a .png file, this is ignored.\n" +
                "<li><kbd><i>nLatValue</i></kbd> is the desired numer (an integer) of latitude points in the grid.\n" +
                "   For example, <kbd>200</kbd>.\n" +
                "   <br><kbd>nLat=<i>nLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum available.\n" +
                "   <br>See the comments above for <kbd><i>nLonValue</i></kbd>.\n" +
                "<li><kbd><i>fileTypeValue</i></kbd> is the type of data file that you want to download.\n" +
                "   <br>To see a list of options, use <kbd>fileType=</kbd> at the end of a query.\n" +
                "   <br>The file type <kbd>.ncHeader</kbd> is the ncdump-style file header showing all the metadata, but no data.\n" +
                "   <br>If you are using a browser, .ncHeader data will appear as plain text in your browser.\n" +
                "     Other file types will cause a \"Download File\" dialog box to pop up.\n" +
                "   <br>See <a href=\"" + oneOf.gridFileHelpUrl() + "\">a description of the grid data file types</a>.\n" +
                "<li>Currently, the bathymetry data source is the \n" +
                "      <a href=\"https://www.ngdc.noaa.gov/mgg/global/global.html\">ETOPO1</a>\n" +
                "      (Ice Surface, grid registered,\n" +
                "    <br>binary, 2 byte int: etopo1_ice_g_i2.zip) data\n" +
                "      <a href=\"https://www.ngdc.noaa.gov\">NOAA NGDC</a>." +
                "<li>Matlab users can download data from within Matlab. Here is a 3-line example,\n" +
                "   <br>&nbsp;&nbsp;1) <kbd>link='" + fullUrl + "?" +
                    "get=bathymetryData" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() + 
                    "&amp;fileType=.mat';</kbd>\n" +
                "   <br>&nbsp;&nbsp;2) <kbd>F=urlwrite(link,'test.m');</kbd>\n" +
                "   <br>&nbsp;&nbsp;3) <kbd>load('-MAT',F);</kbd>\n" +
                "   <br>The first line of the example is very long and may be displayed as a few lines in your browser.\n" +
                "</ul>\n" +
                "\n" +
                "Examples of valid queries:\n" +
                "<ul>\n" +
                "<li>To see a list of <kbd>get</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get\"><kbd>" +
                  fullUrl + "?get</kbd></a>\n" +
                "<li>To see a list of <kbd>fileType</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=bathymetryData&amp;fileType=\"><kbd>" +
                  fullUrl + "?get=bathymetryData&amp;fileType=</kbd></a>\n" +
                "<li>To get a NetCDF file for a limited geographic range, and subsetted to just include just\n" +
                "    100 longitude points and 125 latitude points, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=bathymetryData" +  
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;nLon=100&amp;nLat=125&amp;fileType=.nc" +
                  "\"><kbd>" +
                  fullUrl + "?get=bathymetryData" +  
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;nLon=100&amp;nLat=125&amp;fileType=.nc" +
                  "</kbd></a>\n" +
                "<li>To get the data closest to a single lat lon point, set minLon=maxLon and minLat=maxLat:\n" +
                  "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=bathymetryData&amp;minLon=-134.99&amp;maxLon=-134.99&amp;minLat=40.01&amp;maxLat=40.01&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=bathymetryData&amp;minLon=-134.99&amp;maxLon=-134.99&amp;minLat=40.01&amp;maxLat=40.01&amp;fileType=.asc</kbd></a>\n" +
                  "<br>That returns the same data as the query for the nearest actual grid point:\n" +
                  "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=bathymetryData&amp;minLon=-135&amp;maxLon=-135&amp;minLat=40&amp;maxLat=40&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=bathymetryData&amp;minLon=-135&amp;maxLon=-135&amp;minLat=40&amp;maxLat=40&amp;fileType=.asc</kbd></a>\n" +
                "</ul>\n" +
                "\n";
            }

            //******************************************************************************
            //flesh out the get=stationData or stationVector error message
            if (error != null && (getStationData || getStationVectorData)) {

                String screen = getStationData? "Station Data 1" : 
                    getStationVectorData? "Station Vector Data" :
                    null;
                String screen20 = String2.replaceAll(screen, " ", "%20");

                error = thankYou +
                "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                "<p><strong>There was an error in your <kbd>get=" + cleanGetValue + "</kbd> query:</strong>\n" +
                "<br>" + error + "\n" +
                "<p><strong>General Information for <kbd>get=" + cleanGetValue + "</kbd> Queries</strong>\n" +
                "    <br><kbd>get=" + cleanGetValue + "</kbd> queries allow you to download\n" +
                "    (averaged) time series " + (getStationVectorData? "vector " : "") + 
                    "data from stations (for example, buoys).\n" +
                "    <br>The format of a <kbd>get=" + cleanGetValue + "</kbd> query is<kbd>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;?get=" + cleanGetValue + "&amp;dataSet=<i>dataSetValue</i>&amp;timePeriod=<i>timePeriodValue</i>\n" + 
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;&amp;beginTime=<i>beginTimeValue</i>&amp;endTime=<i>endTimeValue</i>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;[&amp;minLon=<i>minLonValue</i>][&amp;maxLon=<i>maxLonValue</i>][&amp;minLat=<i>minLatValue</i>][&amp;maxLat=<i>maxLatValue</i>]\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;[&amp;minDepth=<i>minDepthValue</i>][&amp;maxDepth=<i>maxDepthValue</i>]&amp;fileType=<i>fileTypeValue</i></kbd>\n" +
                "\n" +
                "<p>Notes:<ul>\n"+
                "<li>The easiest way to build a correct query url is to start at\n" +
                "   <a href=\"" + url + "?get\">" + fullUrl + "?get</a> and repeatedly choose\n" +
                "   from the options on the error message web pages.\n" +
                "<li>Queries must not have any internal spaces.\n"+
                "<li>Queries are not case sensitive.\n" +
                "<li>[ ] is notation to denote an optional part of the query.\n" + //sp is needed for CencoosCurrents Browser's font
                "<li><kbd><i>italics</i></kbd> is notation to denote a value specific to your query.\n" +
                "<li><kbd><i>dataSetValue</i></kbd> is the name of a data set.\n" +
                "   <br>To see a list of options, use <kbd>dataSet=</kbd> at the end of a query.\n" +
                "   <br>There are two sets of options: the 7 character \"internal\" data set names\n" +
                "     (for example, <kbd>" + dataSet7Example + "</kbd>) and the Data Set options in the \n" +
                "     <a href=\"" + url + "?edit=" + screen20 + "\">\n" +  //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + screen + "</kbd></a> section of the CoastWatch Browser, but with spaces removed\n" +
                "     (for example, <kbd>\"" + clean(dataSetFullExample) + "\"</kbd>).\n" +
                "     The 7 character \"internal\" names are preferred, since they are unlikely to ever change.\n" +
                "<li><kbd><i>timePeriodValue</i></kbd> is the name of a time period.\n" +
                "     For data files which represent composites of several day's worth of data,\n" +
                "     the <kbd>timePeriod</kbd> indicates the length of the composite.\n" +
                "     For example, an <kbd>3day</kbd> composite has the average of all data observed\n" +
                "     in an 3 day time period.\n" +
                "   <br>To see a list of options, use <kbd>timePeriod=</kbd> at the end of a query.\n" +
                "   <br>The options are the same as the Time Period options specific to a given Data Set in the \n" +
                "     <a href=\"" + url + "?edit=" + screen20 + "\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + screen + "</kbd></a> section of the CoastWatch Browser, but with spaces removed.\n" +
                "<li><kbd><i>beginTimeValue</i></kbd> is the beginning centered date/time in ISO 8601 format:\n" +
                "     <i>YYYY-MM-DD</i> or <i>YYYY-MM-DD</i>T<i>hh:mm:ss</i> (note the literal \"T\" between the date and time).\n" +
                "     For example, <kbd>2006-04-11T00:00:00</kbd>.\n" +
                "   <br>All times are in the Zulu (also known as UTC or GMT) time zone, not the local time zone.\n" +
                "   <br>If you specify just a date, for example, <kbd>2006-04-11</kbd>, it will be\n" +
                "     interpreted as the start of that day, for example, <kbd>2006-04-11T00:00:00</kbd>.\n" +
                "   <br>The <kbd><i>beginTimeValue</i></kbd> you supply is adjusted to the nearest appropriate centered time,\n" +
                "      given the specified <kbd><i>timePeriodValue</i></kbd>.\n" +
                "   <br><kbd>beginTime</kbd> is optional. If you omit it, the beginTime is\n" +
                "     an appropriate amount of time before the end date (given the <kbd><i>timePeriodValue</i></kbd>).\n" +
                "   <br>The <kbd><i>beginTimeValue</i></kbd> will be part of the name of the file that you download,\n" +
                "     but with the dashes, the 'T', and the colons removed.\n" +
                "   <br>If the data's time values are evenly spaced,\n" +
                "     <kbd><i>beginTimeValue</i></kbd> and <kbd><i>endTimeValue</i></kbd>\n" +
                "     are rounded to be a multiple of the frequency of the data's collection.\n" +
                "     For example, if the data is hourly, they are rounded to the nearest hour.\n" + 
                "   <br>Note that data may not be available for the time you specify.\n" +
                "<li><kbd><i>endTimeValue</i></kbd> is the end centered date/time in ISO 8601 format:\n" +
                "     <i>YYYY-MM-DD</i> or <i>YYYY-MM-DD</i>T<i>hh:mm:ss</i> (note the literal \"T\" between the date and time).\n" +
                "     For example, <kbd>2006-04-12T04:00:00</kbd>.\n" +
                "   <br>All times are in the Zulu (also known as UTC or GMT) time zone, not the local time zone.\n" +
                "   <br>If you specify just a date, for example, <kbd>2006-04-12</kbd>, it will be\n" +
                "     interpreted as the start of that day, for example, <kbd>2006-04-12T00:00:00</kbd>.\n" +
                "   <br>The <kbd><i>endTimeValue</i></kbd> you supply is adjusted to the nearest appropriate centered time,\n" +
                "      given the specified <kbd><i>timePeriodValue</i></kbd>.\n" +
                "   <br><kbd>endTime</kbd> is optional. If you omit it, the endTime is\n" +
                "     the latest available time for any station in this dataSet.\n" +
                "     Note that most stations will not yet have data available for the latest time.\n" +
                "   <br>Or, use the special value, <kbd>latest</kbd>, to get the latest available data.\n" +
                "   <br>The <kbd><i>endTimeValue</i></kbd> will be part of the name of the file that you download,\n" +
                "     but with the dashes, the 'T', and the colons removed.\n" +
                "<li><kbd><i>minLonValue</i></kbd> is the minimum desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>-135.5</kbd> represents 135.5°W.\n" +
                "   <br><kbd>minLon=<i>minLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum longitude for this program (" + oneOf.regionMinX() + ").\n" +
                "   <br>You can specify <kbd><i>minLonValue</i></kbd> and <kbd><i>maxLonValue</i></kbd>\n" +
                "     in the range -180° to 180°, or 0° to 360°,\n" +
                "     regardless of the range of the original data. The program will automatically\n" +
                "     extract and, if necessary, convert the data to your desired range.\n" +
                "   <br>To get the data for a single station, use the station's longitude for <kbd>minLon</kbd>\n" +
                "     and <kbd>maxLon</kbd>, and the station's latitude for <kbd>minLat</kbd> and <kbd>maxLat</kbd>.\n" +
                "     To see a list of stations and their locations, see the <kbd>Plot time series for station</kbd> options in the\n" +
                "     <a href=\"" + url + "?edit=" + screen20 + "\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + screen + "</kbd></a> section of the CoastWatch Browser.\n" +
                "<li><kbd><i>maxLonValue</i></kbd> is the maximum desired longitude (x axis) value, in decimal degrees East. \n" +
                "   For example, <kbd>127.5</kbd> represents 127.5°E. \n" +
                "   <br><kbd>maxLon=<i>maxLonValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum longitude for this program (" + oneOf.regionMaxX() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>minLatValue</i></kbd> is the minimum desired longitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>-22.25</kbd> represents 22.25°S. \n" +
                "   <br><kbd>minLat=<i>minLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum latitude for this program (" + oneOf.regionMinY() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>maxLatValue</i></kbd> is the maximum desired longitude (y axis) value, in decimal degrees North. \n" +
                "   For example, <kbd>40.75</kbd> represents 40.75°N. \n" +
                "   <br><kbd>maxLat=<i>maxLatValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum latitude for this program (" + oneOf.regionMaxY() + ").\n" +
                "   <br>See the comments above for <kbd><i>minLonValue</i></kbd>.\n" +
                "<li><kbd><i>minDepthValue</i></kbd> is the minimum desired depth (z axis) value, in meters (positive = down). \n" +
                "   For example, <kbd>-5</kbd> represents 5 meters above sea level. \n" +
                "   <br><kbd>minDepth=<i>minDepthValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the minimum depth for the selected data set.\n" +
                "   <br>Any double value is valid.\n" +
                "<li><kbd><i>maxDepthValue</i></kbd> is the maximum desired depth (z axis) value, in meters (positive = down). \n" +
                "   For example, <kbd>10</kbd> represents 10 meters below sea level. \n" +
                "   <br><kbd>maxDepth=<i>maxDepthValue</i></kbd> doesn't have to be in your query;\n" +
                "     the default value is the maximum depth for the selected data set.\n" +
                "   <br>Any double value is valid.\n" +
                "<li><kbd><i>fileTypeValue</i></kbd> is the type of data file that you want to download.\n" +
                "   <br>To see a list of options, use <kbd>fileType=</kbd> at the end of a query.\n" +
                "   <br>The file type <kbd>.ncHeader</kbd> is the ncdump-style file header showing all the metadata, but no data.\n" +
                "   <br>If you are using a browser, .ncHeader data will appear as plain text in your browser.\n" +
                "     Other file types will cause a \"Download File\" dialog box to pop up.\n" +
                "   <br>See <a href=\"" + oneOf.pointFileHelpUrl() + "\">a description of the point data file types</a>.\n" +
                "   <br>If <kbd><i>fileTypeValue</i></kbd> is a .png file and <kbd><i>beginTimeValue</i></kbd> = <kbd><i>endTimeValue</i></kbd>,\n" +
                "     this generates a map plotting the various stations.  Otherwise, this plots a\n" +
                "     time series graph.\n" +
                "<li>Matlab users can download data from within Matlab. Here is a 3-line example,\n" +
                "   <br>&nbsp;&nbsp;1) <kbd>link='" + url + "?" +
                    "get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "" +
                    "&amp;beginTime=2006-04-09&amp;endTime=2006-04-12" +
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + "" +
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() + 
                    "&amp;fileType=.mat';</kbd>\n" +
                "   <br>&nbsp;&nbsp;2) <kbd>F=urlwrite(link,'test.m');</kbd>\n" +
                "   <br>&nbsp;&nbsp;3) <kbd>load('-MAT',F);</kbd>\n" +
                "   <br>The first line of the example is very long and may be displayed as a few lines in your browser.\n" +
                "</ul>\n" +
                "\n" +
                "Examples of valid queries:\n" +
                "<ul>\n" +
                "<li>To see a list of <kbd>get</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get\"><kbd>" +
                  fullUrl + "?get</kbd></a>\n" +
                "<li>To see a list of <kbd>dataSet</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=</kbd></a>\n" +
                "<li>To see a list of <kbd>fileType</kbd> options for one day's \"" + dataSetFullExample + "\" data, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-11&amp;endTime=2006-04-12&amp;fileType=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-11&amp;endTime=2006-04-12&amp;fileType=</kbd></a>\n" +
                "<li>To get a NetCDF file with the latest 24 hours of \"" + dataSetFullExample + "\" data\n" +
                "    for stations in a specific geographic range, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;fileType=.nc" +
                  "\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;fileType=.nc" +
                  "</kbd></a>\n" +
                "<li>To get a .asc ASCII file with 3 day's of \"" + dataSetFullExample + "\" data, use\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.asc</kbd></a>\n" +
                "<li>By using the default minLon, maxLon, minLat, maxLat, this example is equivalent to the previous example:\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.asc</kbd></a>\n" +
                "<li>To get just the ncdump-style header information (metadata) for the data file you have specified:\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.ncHeader\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.ncHeader</kbd></a>\n" +
                "</ul>\n" +
                "\n";
            }

            //******************************************************************************
            //flesh out the get=trajectoryData error message
            if (error != null && (getTrajectoryData)) {

                String screen = "Trajectory Data 1";
                String screen20 = String2.replaceAll(screen, " ", "%20");

                error = thankYou +
                "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                "<p><strong>There was an error in your <kbd>get=" + cleanGetValue + "</kbd> query:</strong>\n" +
                "<br>" + error + "\n" +
                "<p><strong>General Information for <kbd>get=" + cleanGetValue + "</kbd> Queries</strong>\n" +
                "    <br><kbd>get=" + cleanGetValue + "</kbd> queries allow you to download\n" +
                "    trajectory data (for example, from tagged animals).\n" +
                "    <br>The format of a <kbd>get=" + cleanGetValue + "</kbd> query is<kbd>\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;?get=" + cleanGetValue + "&amp;dataSet=<i>dataSetValue</i>\n" + 
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;&amp;individuals=<i>individualsValue</i>[&amp;dataVariables=<i>dataVariablesValue</i>]\n" +
                "    <br>&nbsp;&nbsp;&nbsp;&nbsp;&amp;fileType=<i>fileTypeValue</i></kbd>\n" +
                "\n" +
                "<p>Notes:<ul>\n"+
                "<li>The easiest way to build a correct query url is to start at\n" +
                "   <a href=\"" + url + "?get\">" + fullUrl + "?get</a> and repeatedly choose\n" +
                "   from the options on the error message web pages.\n" +
                "<li>Queries must not have any internal spaces.\n"+
                "<li>Queries are not case sensitive.\n" +
                "<li>[ ] is notation to denote an optional part of the query.\n" + //sp is needed for CencoosCurrents Browser's font
                "<li><kbd><i>italics</i></kbd> is notation to denote a value specific to your query.\n" +
                "<li><kbd><i>dataSetValue</i></kbd> is the name of a data set.\n" +
                "   <br>To see a list of options, use <kbd>dataSet=</kbd> at the end of a query.\n" +
                "   <br>There are two sets of options: the 7 character \"internal\" data set names\n" +
                //"     (for example, <kbd>" + dataSet7Example + "</kbd>)\n" +
                "     and the Data Set options in the \n" +
                "     <a href=\"" + url + "?edit=" + screen20 + "\">\n" +  //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + screen + "</kbd></a> section of the CoastWatch Browser, but with spaces removed.\n" +
                //"     (for example, <kbd>\"" + clean(dataSetFullExample) + "\"</kbd>).\n" +
                "     The 7 character \"internal\" names are preferred, since they are unlikely to ever change.\n" +
                "<li><kbd><i>individualsValue</i></kbd> is a comma-separated-value list of 1 or more individuals\n" +
                "     of the selected Data Set.\n" +
                "   <br>To see a list of options, use <kbd>individuals=</kbd> at the end of a query.\n" +
                "   <br>The options are the same as the Individuals options specific to a given Data Set\n" +
                "     in the <a href=\"" + url + "?edit=" + screen20 + "\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + screen + "</kbd></a> section of the CoastWatch Browser, but with spaces removed.\n" +
                "<li><kbd><i>dataVariablesValue</i></kbd> is a comma-separated-value list of 0 or more data variables\n" +
                "     of the selected Data Set.\n" +
                "   <br><kbd>dataVariables=</kbd> doesn't have to be in your query.\n" +
                "   <br>If <kbd>dataVariables=</kbd> isn't in your query, no data variables will be included in the results.\n" +
                "   <br>If <kbd>dataVariables=</kbd> is in your query but 0 data variables are specified, all data variables will be included in the results.\n" +
                "   <br>The non-data variables (LON, LAT, DEPTH, TIME, ID) are always included in the results.\n" +
                "   <br>To see a list of data variable options, use <kbd>dataVariables=</kbd> at the end of a query.\n" +
                "   <br>The options correspond to the different X Axis and Y Axis options (except for the non-data variables)\n" +
                "     for a given Data Set in the <a href=\"" + url + "?edit=" + screen20 + "\">\n" + //doesn't work for CencoosCurrents
                "     <kbd>Edit: " + screen + "</kbd></a> section of the CoastWatch Browser, but with spaces removed.\n" +
                "     The difference is: the options here are the variable names that are in the data files,\n" +
                "     whereas the CoastWatch Browser show you the long names for the variables.\n" + 
                "<li><kbd><i>fileTypeValue</i></kbd> is the type of data file that you want to download.\n" +
                "   <br>To see a list of options, use <kbd>fileType=</kbd> at the end of a query.\n" +
                "   <br>The file type <kbd>.ncHeader</kbd> is the ncdump-style file header showing all the metadata, but no data.\n" +
                "   <br>If you are using a browser, .ncHeader data will appear as plain text in your browser.\n" +
                "     Other file types will cause a \"Download File\" dialog box to pop up.\n" +
                "   <br>See <a href=\"" + oneOf.pointFileHelpUrl() + "\">a description of the point data file types</a>.\n" +
                "   <br>If <kbd><i>fileTypeValue</i></kbd> is a .png file and <kbd><i>beginTimeValue</i></kbd> = <kbd><i>endTimeValue</i></kbd>,\n" +
                "     this generates a map plotting the various stations.  Otherwise, this plots a\n" +
                "     time series graph.\n" +
                /* needs work (and a universally available dataset)
                "<li>Matlab users can download data from within Matlab. Here is a 3-line example,\n" +
                "   <br>&nbsp;&nbsp;1) <kbd>link='" + url + "?" +
                    "get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "" +
                    "&amp;beginTime=2006-04-09&amp;endTime=2006-04-12" +
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + "" +
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() + 
                    "&amp;fileType=.mat';</kbd>\n" +
                "   <br>&nbsp;&nbsp;2) <kbd>F=urlwrite(link,'test.m');</kbd>\n" +
                "   <br>&nbsp;&nbsp;3) <kbd>load('-MAT',F);</kbd>\n" +
                "   <br>The first line of the example is very long and may be displayed as a few lines in your browser.\n" +
                */
                "</ul>\n" +
                "\n" +
                "Examples of valid queries:\n" +
                "<ul>\n" +
                "<li>To see a list of <kbd>get</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get\"><kbd>" +
                  fullUrl + "?get</kbd></a>\n" +
                "<li>To see a list of <kbd>dataSet</kbd> options, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=</kbd></a>\n" +
                /* needs work  (and a universally available dataset)
                "<li>To see a list of <kbd>fileType</kbd> options for one day's \"" + dataSetFullExample + "\" data, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-11&amp;endTime=2006-04-12&amp;fileType=\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-11&amp;endTime=2006-04-12&amp;fileType=</kbd></a>\n" +
                "<li>To get a NetCDF file with the latest 24 hours of \"" + dataSetFullExample + "\" data\n" +
                "    for stations in a specific geographic range, use\n" +
                "  <br><a href=\"" + //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;fileType=.nc" +
                  "\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;fileType=.nc" +
                  "</kbd></a>\n" +
                "<li>To get a .asc ASCII file with 3 day's of \"" + dataSetFullExample + "\" data, use\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.asc</kbd></a>\n" +
                "<li>By using the default minLon, maxLon, minLat, maxLat, this example is equivalent to the previous example:\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.asc\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.asc</kbd></a>\n" +
                "<li>To get just the ncdump-style header information (metadata) for the data file you have specified:\n" +
                "  <br><a href=\"" +  //don't use \n for the following lines
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.ncHeader\"><kbd>" +
                  fullUrl + "?get=" + cleanGetValue + "&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;beginTime=2006-04-09&amp;endTime=2006-04-12&amp;fileType=.ncHeader</kbd></a>\n" +
                */
                "</ul>\n" +
                "\n";
            }



//****** DISABLED FOR NOW **************************************************************
        //I had trouble with these examples. 
        //Table.testOpendap (which uses these same examples), works fine on my computer.
        //But here they just lead to time outs.
        } /*else if (false &&    //"false" effectively comments out this section
                error == null && getOpendapSequence) { 
            String cleanQuery = "?get=" + getValue + "&amp;url=";

            //url
            String urlValue = null; 
            if (error == null) {                
                urlValue = (String)String2.alternateGetValue(alternate, "url");  //must be lowercase attName
                if (urlValue == null || urlValue.length() == 0) {   
                    String[] urlExamples = {
                        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?month&amp;unique()",
                        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0&#44;oxygen&amp;month=&quot;5&quot;", //was "5"
                        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3&#44;lat&#44;long",
                        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3&#44;lat&#44;long&amp;program=&quot;MESO_1&quot;", //was "MESO_1"
                        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?stn_id&amp;unique()",
                        "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?abund_m3&amp;stn_id=&quot;NH05&quot;" //was "NH05"
                        };
                    error = listError("url", urlValue, cleanQuery, false, 
                        urlExamples, null, "dimensionName");
                } else if (!urlValue.startsWith("http://")) {
                    error = "<kbd>urlValue</kbd> must begin with <kbd>http://</kbd> .";
                } else {
                    cleanQuery += urlValue + "&amp;dimensionName=";
                }
                String2.log("urlValue=" + urlValue + " error=" + error);
            }

            //dimensionName
            String dimensionName = null; 
            if (error == null) {                
                dimensionName = (String)String2.alternateGetValue(alternate, "dimensionname");  //must be lowercase attName
                if (dimensionName == null) {
                    dimensionName = "row";
                } else if (dimensionName.length() == 0) {   
                    String[] dimensionExamples = {"observation", "row", "station", "time"};
                    error = listError("dimensionName", dimensionName, cleanQuery, false, 
                        dimensionExamples, null, "fileType");
                } else {
                    cleanQuery += dimensionName + "&amp;fileType=";
                }
                String2.log("dimensionName=" + dimensionName + " error=" + error);
            }

            //fileType
            String extension = null;
            int extensionIndex = -1;
            if (error == null) {
                extension = (String)String2.alternateGetValue(alternate, "filetype");   //must be lowercase attName
                String cleanFileTypes[] = {".asc", ".nc"}; //these exactly match the Table.convert options[]
                extensionIndex = String2.caseInsensitiveIndexOf(cleanFileTypes, extension);
                if (extensionIndex < 0) 
                    error = listError("fileType", extension, cleanQuery, true, 
                        cleanFileTypes, null, null);
                else {
                    cleanQuery += extension; 
                    extension = extension.toLowerCase();
                }
                String2.log("extension=" + extension + " error=" + error);
            }

            //create the file
            String dir = oneOf.fullPrivateDirectory();
            String name = null;
            if (error == null) {
                //generate the data file name       
                //standard baseName e.g., LATsstaS1day_20030304   FILE_NAME_RELATED_CODE
                name = String2.md5Hex12(urlValue + dimensionName);
                try {
                    if (File2.touch(dir + name + extension)) {
                        //reuse existing file
                        String2.log("doQuery reusing " + name + extension);
                    } else {
                        //read the opendapSequence
                        //IF THIS IS MADE LIVE, USE SSR.percentEncode  INSTEAD ???
                        String tUrlValue = urlValue;
                        tUrlValue = String2.replaceAll(tUrlValue, "&amp;", "&");
                        tUrlValue = String2.replaceAll(tUrlValue, "&#44;", ",");
                        tUrlValue = String2.replaceAll(tUrlValue, "&quot;", "\"");
                        tUrlValue = String2.replaceAll(tUrlValue, "&lt;", "<");
                        tUrlValue = String2.replaceAll(tUrlValue, "&gt;", ">");
                        String2.log("tUrlValue=" + tUrlValue);
                        String2.log("          " + String2.makeString('-', tUrlValue.length()));
                        Table table = new Table();
                        table.readOpendapSequence(tUrlValue);
                        
                        //save the file (and zipIt?)
                        table.saveAs(dir + name + extension, extensionIndex, dimensionName, false);

                        //insurance
                        Test.ensureTrue(File2.isFile(dir + name + extension), ""); //will be caught below
                    }
                } catch (Exception e) {
                    error = String2.replaceAll(
                        "Error while creating the file \"" + name + extension + "\":\n" + 
                            MustBe.throwableToString(e),
                        "\n", "\n<br>");
                }
            }

            //file creation was successful, we're going to send the data,
            if (error == null) {
                //setContentType(mimeType)
                if (extension.equals(".asc")) {
                    response.setContentType("text/plain"); 
                } else { 
                    response.setContentType("application/x-download");  //or "application/octet" ?
                    //how specify file name in popup window that user is shown? 
                    //see http://forum.java.sun.com/thread.jspa?threadID=696263&messageID=4043287
                    response.addHeader("Content-Disposition","attachment;filename=" + 
                        name + extension);
                }

                //transfer the file to response.getOutputStream
                //Compress the output stream if user request says it is allowed.
                //See http://www.websiteoptimization.com/speed/tweak/compress/
                //This makes sending raw file (even ASCII) as efficient as sending a zipped file
                //   and user doesn't have to unzip the file.
                // /* not tested yet. test after other things are working. test them individually
                //Accept-Encoding should be a csv list of acceptable encodings.
                String encoding = request.getHeader("Accept-Encoding"); //case-insensitive
                encoding = encoding == null? "" : encoding.toLowerCase();
                OutputStream out = new BufferedOutputStream(response.getOutputStream());
                if (encoding.indexOf("compress") >= 0) {
                    response.addHeader("Content-Encoding", "compress");
                    out = new ZipOutputStream(out);
                    out.putNextEntry(new ZipEntry(name + extension));
                } else if (encoding.indexOf("gzip") >= 0) {
                    response.addHeader("Content-Encoding", "gzip");
                    out = new GZIPOutputStream(out);
                } else if (encoding.indexOf("deflate") >= 0) {
                    response.addHeader("Content-Encoding", "deflate");
                    out = new DeflaterOutputStream(out);
                }

                //transfer the file to response.getOutputStream
                try {
                    if (!File2.copy(dir + name + extension, out)) {
                        //outputStream contentType already set, so can't
                        //go back to html and display error message
                        String errorInMethod = String2.ERROR + " in " + 
                            oneOf.shortClassName() + ".doQuery(\n" +
                            query + "):\n";
                        error = errorInMethod + "error while transmitting " + name + extension;
                        String2.log(error);
                    } 
                } finally {
                    try {out.close();} catch (Exception e) {} //essential, to end compression
                }

                return true;
            }

            //flesh out the get=opendapSequence error message
            if (error != null) {
                error = thankYou +
                "<p><strong>Your query was:</strong> <kbd>?" + XML.encodeAsHTML(query) + "</kbd>\n" +
                "<p><strong>There was an error in your <kbd>get=opendapSequence</kbd> query:</strong>\n" +
                "<br>" + error + "\n" +
                "<p><strong>General Information:</strong> <kbd>get=opendapSequence</kbd> allows you to download a\n" +
                "    file with the results from an opendap sequence query.\n" +
                "    <br>The format of a <kbd>get=opendapSequence</kbd> query is\n" +
                "    <br><kbd>?get=opendapSequence&url=<i>urlValue</i>[&dimensionName=<i>dimensionNameValue</i>]&fileType=<i>fileTypeValue</i></kbd>\n" +
                "\n" +
                "<p>Notes:\n" +
                "<ul><li>Queries must not have any internal spaces.\n"+
                "<li>Queries are not case sensitive.\n" +
                "<li>[ ] is notation to denote an optional part of the query.\n" +
                "<li><kbd><i>italics</i></kbd> is notation to denote a value specific to your query.\n" +
                "<li><kbd><i>urlValue</i></kbd> is the opendap url + query for the data you want.\n" +
                "   In the <kbd><i>urlValue</i></kbd>, \n" +
                "   <br>&nbsp;&nbsp;'<kbd>&amp;</kbd>' must be replaced by '<kbd>&amp;amp;</kbd>',\n" +
                "   <br>&nbsp;&nbsp;'<kbd>,</kbd>' must be replaced by '<kbd>&amp;#044;</kbd>',\n" +
                "   <br>&nbsp;&nbsp;'<kbd>\"</kbd>' must be replaced by '<kbd>&amp;quot;</kbd>',\n" +
                "   <br>&nbsp;&nbsp;'<kbd>&lt;</kbd>' must be replaced by '<kbd>&amp;lt;</kbd>', and\n" +
                "   <br>&nbsp;&nbsp;'<kbd>&gt;</kbd>' must be replaced by '<kbd>&amp;gt;</kbd>'.\n" +
                "   <br>The <kbd><i>urlValue</i></kbd> must start with <kbd>http://</kbd> .\n" +
                "   <br>For example, <kbd></kbd>.\n" +
                "   <br>To see a list of examples, use <kbd>url=</kbd> at the end of a query.\n" +
                "<li><kbd><i>dimensionNameValue</i></kbd> is the name to be used for the dimension if you create a .nc file.\n" +
                "   <br>This doesn't change the data you will download, just the name used for the dimension.\n" +
                "   <br>Common names are <kbd>observation, row, station,</kbd> and <kbd>time</kbd>.\n" +
                "   <br>You don't have to specify a dimensionName. The default value is <kbd>row</kbd>.\n" +
                "   <br>To see a list of examples, use <kbd>timePeriod=</kbd> at the end of a query.\n" +
                "<li><kbd><i>fileTypeValue</i></kbd> is the type of data file that you want to get.\n" +
                "   <br>To see a list of options, use <kbd>fileType=</kbd> at the end of a query.\n" +
                "   <br>If you are using a browser, .ncHeader data will appear as plain text in your browser.\n" +
                "     Other file types will cause a \"Download File\" dialog box to pop up.\n" +
                "</ul>\n" +
                "\n" +
                "Examples of valid queries:\n" +
                "<ul>\n" +
                "<li>To see a list of <kbd>get</kbd> options, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get\"><kbd>" +
                  "?get</kbd></a>\n" +
                "<li>To see a list of <kbd>dataSet</kbd> options, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=\"><kbd>" +
                  "?get=gridData&amp;dataSet=</kbd></a>\n" +
                "<li>To see a list of <kbd>timePeriod</kbd> options for the GOES SST Data Set, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=</kbd></a>\n" +
                "<li>To see a list of <kbd>centeredTime</kbd> options for the GOES SST Data Set, 1 day composites, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=</kbd></a>\n" +
                "<li>To see a list of <kbd>fileType</kbd> options, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1day&amp;centeredTime=latest&amp;fileType=</kbd></a>\n" +
                "<li>To get a NetCDF file with 1 observation (for date/time 2006-04-11T00:00:00) \n" +
                "    of GOES SST data\n" +
                "    for a limited geographic range, and subsetted to just include at least\n" +
                "    100 longitude points and 125 latitude points, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;centeredTime=2006-04-11T00:00:00" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;nLon=100&amp;nLat=125&amp;fileType=.nc" +
                  "\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=1observation&amp;centeredTime=2006-04-11T00:00:00" + 
                    "&amp;minLon=" + oneOf.regionMinX() + "&amp;maxLon=" + oneOf.regionMaxX() + 
                    "&amp;minLat=" + oneOf.regionMinY() + "&amp;maxLat=" + oneOf.regionMaxY() +
                    "&amp;nLon=100&amp;nLat=125&amp;fileType=.nc" +
                  "</kbd></a>\n" +
                "<li>To get an xyz ASCII file with the data for the latest 8 day composite of GOES SST data\n" +
                "    for the entire available geographic region, use\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=8day&amp;centeredTime=latest&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.xyz" +
                  "\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=8day&amp;centeredTime=latest&amp;minLon=-180&amp;maxLon=180&amp;minLat=-90&amp;maxLat=90&amp;fileType=.xyz" +
                  "</kbd></a>\n" +
                "<li>By using the default minLon, maxLon, minLat, maxLat, this example is equivalent to the previous example:\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=8day&amp;centeredTime=latest&amp;fileType=.xyz" +
                  "\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=8day&amp;centeredTime=latest&amp;fileType=.xyz" +
                  "</kbd></a>\n" +
                "<li>To get just the ncdump-style header information for the data file you have specified:\n" +
                "  <br><a href=\"" + url + //don't use \n for the following lines
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=8day&amp;centeredTime=latest&amp;fileType=.ncHeader" +
                  "\"><kbd>" +
                  "?get=gridData&amp;dataSet=" + dataSet7Example + "&amp;timePeriod=8day&amp;centeredTime=latest&amp;fileType=.ncHeader" +
                  "</kbd></a>\n" +
                "</ul>\n" +
                "\n";
            }
        }*/

        //if you get here, error has been set        
        //display error message
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println(getStartHtmlHead());
        out.println(EmaClass.includeJavaScriptForEnter);
        out.println("</head>");
        out.println(oneOf.startHtmlBody());
        out.println(error);

        //add the actual diagnostic info  
        //if (oneOf.displayDiagnosticInfo()) {
        //    was from logStringBuilder 
        //}

        out.println(oneOf.endHtmlBody());
        out.println("</html>");

        String2.log("\\\\\\\\**** GET Query unsuccessfully finished. TOTAL TIME=" + 
            (System.currentTimeMillis() - queryTime) + "\n");
        return true;
    }

    /**
     * This generages an html error message with a 
     * space-separated list of the items in sar.
     *
     * @param attributeName e.g., "dataSet"
     * @param attributeValue e.g., "SST,NOAAGOESImager,DayandNight,0.05degrees,WestCoastofUS", 
     *    but perhaps misspelled (hence the need for the error message)
     * @param cleanQuery a clean version of the query so far, e.g., "?get=gridData,dataSet="
     * @param mustBe changes the message. true = "must be". false = "for example, ".
     * @param cleanOptions a required list of valid options
     * @param cleanOptions2 an optional list of aliases for cleanOptions.
     *    If this is present, the cleanOptions option is used for the actual link.
     * @param nextAttributeName  e.g., timePeriod", "fileType".
     *    null leads to a slightly different end message.
     * @return a string with the error message, including links to the next options
     */
    public String listError(String attributeName, String attributeValue, 
            String cleanQuery, boolean mustBe, String[] cleanOptions, 
            String[] cleanOptions2, String nextAttributeName) {

        StringBuilder sb = new StringBuilder();
        boolean longOptions = cleanOptions.length > 0 && cleanOptions[0].length() > 40;
        String tbr = longOptions? "" : "<br>";
        if (mustBe)
            sb.append(
                "The value of <kbd>" + attributeName + "</kbd> (which was \"<kbd>" + attributeValue +
                "</kbd>\") must be one of:"+tbr+"<kbd>\n");
        else sb.append(
                "The value of <kbd>" + attributeName + "</kbd> was not specified. Please specify it.\n" +
                "For example:"+tbr+"<kbd>\n");
        sb.append(
            "<!--BeginOptions-->\n");
            //Use a very clean format that would be easy to screen scrape.
            //Be very reluctant to change this format after it is first released.
        String start = "<span style=\"white-space:nowrap;\"><a href=\"" + oneOf.url() + cleanQuery;
        if (longOptions) 
            start = "<br>" + start;
        String middle = nextAttributeName == null? 
            "\">" : 
            "&amp;" + nextAttributeName + 
                (nextAttributeName.endsWith("=null")? "" : "=") + 
                "\">";
        int nOptions = cleanOptions.length;
        for (int i = 0; i < nOptions; i++) {
            //one option per line
            sb.append(start + cleanOptions[i] + middle + cleanOptions[i] + "</a></span>\n"); 
            if (cleanOptions2 != null)
                sb.append(" or " + start + cleanOptions[i] + middle + cleanOptions2[i] + "</a></span><br>\n"); 
        }
        sb.append("<!--EndOptions-->\n" + //identify the end of the options
            "</kbd><br>(Click on one of the options above to add it to your query and " +
            (nextAttributeName == null? "<strong>download the data</strong>.)" : "see subsequent options.)"));
        return sb.toString();
    }

    /**
     * This makes a new array in which the strings have been clean()ed.
     *
     * @param sar an array of string (usually active options like active dataSet names)
     * @return an array of sanitized GET-friendly strings 
     */
    public static String[] clean(String[] sar) {
        int n = sar.length;
        String sar2[] = new String[n];
        //String2.log("pre clean() sar=" + String2.toCSSVString(sar));
        for (int i = 0; i < n; i++) 
            sar2[i] = clean(sar[i]);
        return sar2;
    }

    /**
     * This returns the string with all 
     * spaces (because they are not allowed in urls), 
     * '&' (because it is the separator for named GET parameters), and
     * '+' (because it is the url encoding replacement for spaces)
     * removed and all html tags removed.
     *
     * @param s a string (usually an active option like an active dataSet name)
     * @return the sanitized, GET-friendly string
     */
    public static String clean(String s) {
        StringBuilder sb = new StringBuilder(
            XML.removeHTMLTags(s)); //do XML/charEntities first
        String2.replaceAll(sb, " ", "");
        String2.replaceAll(sb, "&", "");
        String2.replaceAll(sb, "+", "");
        return sb.toString();
    }

    /**
     * Save grid data as a png (for HTTP GET requests).
     */
    public void gridSaveAsPng(GridDataSet gridDataSet, Grid grid, 
        String dir, String name, String standardTimePeriodValue,
        String spaceCenteredTimeValue, int imageWidth, int imageHeight) throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package and coastline (huge!) and political outline files!
        //For now, keep separate.

        //make the cpt and other needed info
        String legendTime = gridDataSet.getLegendTime(
            standardTimePeriodValue, spaceCenteredTimeValue);
        String fullCptName = CompoundColorMap.makeCPT(
            oneOf.fullPaletteDirectory(), 
            gridDataSet.palette, gridDataSet.paletteScale, 
            String2.parseDouble(gridDataSet.paletteMin), //for std units
            String2.parseDouble(gridDataSet.paletteMax), //for std units
            -1, true, dir);

        //make the image
        BufferedImage bufferedImage = 
            SgtUtil.getBufferedImage(imageWidth, imageHeight);
        Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();
        
        //draw the map
        SgtMap.makeMap(
            SgtUtil.LEGEND_BELOW,
            oneOf.legendTitle1(),
            oneOf.legendTitle2(),
            oneOf.fullContextDirectory() + "images/", 
            oneOf.lowResLogoImageFile(),
            grid.lon[0], grid.lon[grid.lon.length - 1],
            grid.lat[0], grid.lat[grid.lat.length - 1],
            drawLandAsMask? "over" : "under",

            true, //grid
            grid, 
            1, //scaleFactor,
            1, //altScaleFactor,
            0, //altOffset,
            fullCptName, 
            gridDataSet.boldTitle,
            SgtUtil.getNewTitle2(
                DataHelper.makeUdUnitsReadable(gridDataSet.udUnits), 
                legendTime, 
                ""),  
            gridDataSet.courtesy.length() == 0?
                null : "Data courtesy of " + gridDataSet.courtesy,
            "",

            SgtMap.NO_LAKES_AND_RIVERS,

            g2D,
            0, 0, imageWidth, imageHeight,
            0,   //no coastline adjustment
            1); 

        //saveAsPng
        //name has "_small", "_medium", or "_large", so .png extension works for all whichImage
        //(the different files are distinguished)
        SgtUtil.saveAsPng(bufferedImage, dir + name);
    }

    /**
     * Save bathymetry data as a png (for HTTP GET requests).
     */
    public void bathymetrySaveAsPng(Grid grid, 
        String dir, String name, int imageWidth, int imageHeight) throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package and coastline (huge!) and political outline files!
        //For now, keep separate.

        //make the image
        BufferedImage bufferedImage = 
            SgtUtil.getBufferedImage(imageWidth, imageHeight);
        Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();
        
        //draw the map
        SgtMap.makeMap(
            SgtUtil.LEGEND_BELOW,
            oneOf.legendTitle1(),
            oneOf.legendTitle2(),
            oneOf.fullContextDirectory() + "images/", 
            oneOf.lowResLogoImageFile(),
            grid.lon[0], grid.lon[grid.lon.length - 1],
            grid.lat[0], grid.lat[grid.lat.length - 1],
            drawLandAsMask? "over" : "under",

            true, //grid
            grid, 
            1, //scaleFactor,
            1, //altScaleFactor,
            0, //altOffset,
            SgtMap.bathymetryCptFullName, 
            SgtMap.BATHYMETRY_BOLD_TITLE + " (" + SgtMap.BATHYMETRY_UNITS + ")",
            "Data courtesy of " + SgtMap.BATHYMETRY_COURTESY,
            "",
            "",

            SgtMap.NO_LAKES_AND_RIVERS,

            g2D,
            0, 0, imageWidth, imageHeight,
            0,   //no coastline adjustment
            1); 

        //saveAsPng
        //name has "_small", "_medium", or "_large", so .png extension works for all whichImage
        //(the different files are distinguished)
        SgtUtil.saveAsPng(bufferedImage, dir + name);
    }

    /**
     * Save grid vector data as a png (for HTTP GET requests).
     */
    public void gridVectorSaveAsPng(int absoluteVectorIndex,
        GridDataSet xGridDataSet, GridDataSet yGridDataSet, 
        Grid xGrid, Grid yGrid, String dir, String name, String standardTimePeriodValue,
        String spaceCenteredTimeValue, int imageWidth, int imageHeight) throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package and coastline (huge!) and political outline files!
        //For now, keep separate.

        //make the cpt and other needed info
        String legendTime = xGridDataSet.getLegendTime(
            standardTimePeriodValue, spaceCenteredTimeValue);
        String fullCptName = CompoundColorMap.makeCPT(
            oneOf.fullPaletteDirectory(), 
            xGridDataSet.palette, xGridDataSet.paletteScale, 
            String2.parseDouble(xGridDataSet.paletteMin), //for std units
            String2.parseDouble(xGridDataSet.paletteMax), //for std units
            -1, true, dir);

        //make the image
        BufferedImage bufferedImage = 
            SgtUtil.getBufferedImage(imageWidth, imageHeight);
        Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

        //make the graphDataLayer
        ArrayList graphDataLayers = new ArrayList();
        String tUnits = oneOf.vectorInfo()[absoluteVectorIndex][OneOf.VIUnits];
        graphDataLayers.add(new GraphDataLayer(-1, 
            -1, -1, -1, -1, -1, GraphDataLayer.DRAW_GRID_VECTORS, false, false,
            null, null, //x,y axis title
            oneOf.vectorInfo()[absoluteVectorIndex][OneOf.VIBoldTitle],             
            "(" + tUnits + ") " + legendTime,
            xGridDataSet.courtesy.length() == 0? 
                null : "Data courtesy of " + xGridDataSet.courtesy, //Vector data
            null, //title4
            null, xGrid, yGrid, 
            null, Color.black,
            GraphDataLayer.MARKER_TYPE_NONE, 0, 
            String2.parseDouble(oneOf.vectorInfo()[absoluteVectorIndex][OneOf.VISize]), // standardVector=e.g. 10m/s 
            -1));

        //draw the map
        SgtMap.makeMap(false, 
            SgtUtil.LEGEND_BELOW,
            oneOf.legendTitle1(),
            oneOf.legendTitle2(),
            oneOf.fullContextDirectory() + "images/", 
            oneOf.lowResLogoImageFile(),
            xGrid.lon[0], xGrid.lon[xGrid.lon.length - 1],
            xGrid.lat[0], xGrid.lat[xGrid.lat.length - 1],
            drawLandAsMask? "over" : "under",
            //grid 
            false, null, -1, -1, -1, null, null, null, null, null,
            SgtMap.NO_LAKES_AND_RIVERS,
            //contour
            false, null, -1, -1, -1, null, null, null, null, null, null, null,

            graphDataLayers,

            g2D,
            0, 0, imageWidth, imageHeight,
            0,   //no coastline adjustment
            1); 

        //saveAsPng
        //nLon and nLat are in name, so .png extension works for all whichImage
        SgtUtil.saveAsPng(bufferedImage, dir + name);
    }

    /**
     * Save grid time series data as a png (for HTTP GET requests).
     */
    public void gridTimeSeriesSaveAsPng(GridDataSet gridDataSet,  
        Table table, String dir, String name, String standardTimePeriodValue,
        String spaceCenteredTimeValue, double minLon, double minLat, int whichImage) 
        throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package.
        //For now, keep separate.

        //make the graphDataLayer
        String tUnits = DataHelper.makeUdUnitsReadable(gridDataSet.udUnits);
        GraphDataLayer graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            3, 5, -1, -1, -1, //time,data,  others not used
            GraphDataLayer.DRAW_MARKERS_AND_LINES, true, false,
            "Time", tUnits,
            gridDataSet.boldTitle,              
            (TimePeriods.getNHours(standardTimePeriodValue) == 0? "" :
                    "Time series of " + standardTimePeriodValue + " averages. ") + 
                String2.genEFormat10(minLat) + " N, " + 
                String2.genEFormat10(minLon) + " E. (Horizontal line = average)", //title2
            gridDataSet.courtesy.length() == 0? null : 
                "Data courtesy of " + gridDataSet.courtesy,
            null, //title4
            table, null, null,
            null, new Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_SMALL, 
            0,
            GraphDataLayer.REGRESS_MEAN);
        ArrayList graphDataLayers = new ArrayList();
        graphDataLayers.add(graphDataLayer);

        //make the image
        int imageWidth = oneOf.imageWidths()[whichImage];  
        int imageHeight = imageWidth / 2 + 
            //only need room for small legend, so don't use imageHeights
            (whichImage == 0? 115 : 60); //but smallest has narrow&higher legend
        BufferedImage bufferedImage = 
            SgtUtil.getBufferedImage(imageWidth, imageHeight);
        Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

        //draw the graph
        oneOf.sgtGraph().makeGraph(false,
            "Time", 
            DataHelper.makeUdUnitsReadable(gridDataSet.udUnits), 
            SgtUtil.LEGEND_BELOW,
            oneOf.legendTitle1(),
            oneOf.legendTitle2(),
            oneOf.fullContextDirectory() + "images/", 
            oneOf.lowResLogoImageFile(),
            Double.NaN, Double.NaN, true, true,  false, //x isAscending, isTime, isLog
            Double.NaN, Double.NaN, true, false, false,
            graphDataLayers,
            g2D,
            0, 0, imageWidth, imageHeight, 2, //graph width/height
            SgtGraph.DefaultBackgroundColor, 1); //fontScale

        //saveAsPng
        //nLon and nLat are in name, so .png extension works for all whichImage
        SgtUtil.saveAsPng(bufferedImage, dir + name);
    }

    /**
     * Save station data as a png (for HTTP GET requests).
     */
    public void stationSaveAsPng(PointDataSet pointDataSet, 
        Table table, String dir, String name, 
        double minLon, double maxLon, double minLat, double maxLat,
        String minDepthValue, String maxDepthValue,
        String spaceBeginTimeValue, String spaceEndTimeValue, String standardTimePeriodValue,
        int whichImage) throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package.
        //For now, keep separate.
        String unitsValue = 
            DataHelper.makeUdUnitsReadable(pointDataSet.unitsOptions[0]);  //for now always standard units      
        String depthString = "Depth = " +
            (minDepthValue.equals(maxDepthValue)? "" : minDepthValue + " to ") +
            maxDepthValue + " m."; 
        String courtesyValue =
            pointDataSet.courtesy.length() == 0? null: 
                "Data courtesy of " + pointDataSet.courtesy; //Station data


        //one point in time?
        if (Calendar2.isoStringToEpochSeconds(spaceBeginTimeValue) ==  //throws exception if trouble
            Calendar2.isoStringToEpochSeconds(spaceEndTimeValue) &&    //throws exception if trouble
            minLon != maxLon && minLat != maxLat) {

            //make a map with colored markers
            CompoundColorMap ccm = new CompoundColorMap(
                oneOf.fullPaletteDirectory(), pointDataSet.palette, pointDataSet.paletteScale,
                pointDataSet.paletteMin, pointDataSet.paletteMax, 
                -1, true, oneOf.fullPrivateDirectory());

            //make the graphDataLayer
            GraphDataLayer graphDataLayer = new GraphDataLayer(-1,
                0, 1, 5, 4, -1,  //x,y,data,id columns
                GraphDataLayer.DRAW_MARKERS, true, false,
                "Time", unitsValue,
                pointDataSet.boldTitle, 
                "(" + unitsValue + ") " + 
                    TimePeriods.getLegendTime(standardTimePeriodValue, spaceBeginTimeValue) + ". " + //==spaceEndTimeValue  
                    depthString,  //title2
                courtesyValue,
                null, //title4
                table, null, null,
                ccm, Color.black,
                GraphDataLayer.MARKER_TYPE_FILLED_SQUARE, 
                GraphDataLayer.MARKER_SIZE_SMALL, 
                0, //standardVector
                GraphDataLayer.REGRESS_NONE);
            ArrayList graphDataLayers = new ArrayList();
            graphDataLayers.add(graphDataLayer);

            //make the image
            int imageWidth = oneOf.imageWidths()[whichImage];  
            int imageHeight = oneOf.imageHeights()[whichImage];
            BufferedImage bufferedImage = 
                SgtUtil.getBufferedImage(imageWidth, imageHeight);
            Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

            //draw the map
            SgtMap.makeMap(false, 
                SgtUtil.LEGEND_BELOW,
                oneOf.legendTitle1(),
                oneOf.legendTitle2(),
                oneOf.fullContextDirectory() + "images/", 
                oneOf.lowResLogoImageFile(),
                minLon, maxLon, minLat, maxLat,
                drawLandAsMask? "over" : "under",

                //grid data
                false, null, 1, 1, 0,
                "", "", "", "", "",

                SgtMap.NO_LAKES_AND_RIVERS,

                //contour
                false, null, 1, 1, 1, 
                "", null, "", "", "", "", "",

                graphDataLayers,
                g2D,
                0, 0, imageWidth, imageHeight,
                0,   //no coastline adjustment
                1); 

            //saveAsPng
            //nLon and nLat are in name, so .png extension works for all whichImage
            SgtUtil.saveAsPng(bufferedImage, dir + name);

                                        
        } else {
            //make a time series graph
            //make the graphDataLayer
            GraphDataLayer graphDataLayer = new GraphDataLayer(
                -1, //which pointScreen
                3, 5, -1, -1, -1, //time, data, others not used
                GraphDataLayer.DRAW_LINES, true, false,
                "Time", unitsValue,
                pointDataSet.boldTitle + ", time series of " +
                    (TimePeriods.getNHours(standardTimePeriodValue) == 0? "raw data" :
                        standardTimePeriodValue + " averages"), 
                depthString + " " + //no legendTime
                    (minLat == maxLat? "" : String2.genEFormat10(minLat) + " to ") +
                    String2.genEFormat10(maxLat) + " N, " + 
                    (minLon == maxLon? "" : String2.genEFormat10(minLon) + " to ") +
                    String2.genEFormat10(maxLon) + " E. (Horizontal line = average)", //title2
                courtesyValue,
                null, //title4
                table, null, null,
                null, new Color(0x0099FF),
                GraphDataLayer.MARKER_TYPE_NONE, 0, 
                0,
                GraphDataLayer.REGRESS_MEAN);
            ArrayList graphDataLayers = new ArrayList();
            graphDataLayers.add(graphDataLayer);

            //make the image
            int imageWidth = oneOf.imageWidths()[whichImage];  
            int imageHeight = imageWidth / 2 + 
                //only need room for small legend, so don't use imageHeights
                (whichImage == 0? 115 : 60); //but smallest has narrow&higher legend
            BufferedImage bufferedImage = 
                SgtUtil.getBufferedImage(imageWidth, imageHeight);
            Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

            //draw the graph
            oneOf.sgtGraph().makeGraph(false,
                "Time", unitsValue,
                SgtUtil.LEGEND_BELOW,
                oneOf.legendTitle1(),
                oneOf.legendTitle2(),
                oneOf.fullContextDirectory() + "images/", 
                oneOf.lowResLogoImageFile(),
                Double.NaN, Double.NaN, true, true,  false, //x isAscending, isTime, isLog
                Double.NaN, Double.NaN, true, false, false, 
                graphDataLayers,
                g2D,
                0, 0, imageWidth, imageHeight, 2, //graph width/height
                SgtGraph.DefaultBackgroundColor, 1); //fontScale

            //saveAsPng
            //nLon and nLat are in name, so .png extension works for all whichImage
            SgtUtil.saveAsPng(bufferedImage, dir + name);
        }

    }

    /**
     * Save station vector data as a png (for HTTP GET requests).
     */
    public void stationVectorSaveAsPng(String pointVectorInfo[][], 
        int absolutePointVectorIndex, PointDataSet xPointDataSet,
        Table table, String dir, String name, 
        double minLon, double maxLon, double minLat, double maxLat, 
        String minDepthValue, String maxDepthValue,
        String spaceBeginTimeValue, String spaceEndTimeValue, String standardTimePeriodValue,
        int whichImage) throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package.
        //For now, keep separate.
        String depthString = "Depth = " +
            (minDepthValue.equals(maxDepthValue)? "" : minDepthValue + " to ") +
            maxDepthValue + " m"; 
        String courtesyValue =
            xPointDataSet.courtesy.length() == 0? null: 
                "Data courtesy of " + xPointDataSet.courtesy; //Point vector data

        //one point in time?
        if (Calendar2.isoStringToEpochSeconds(spaceBeginTimeValue) == //throws exception if trouble
            Calendar2.isoStringToEpochSeconds(spaceEndTimeValue) &&   //throws exception if trouble
            minLon != maxLon && minLat != maxLat) {

            //make a map with vectors

            //make the graphDataLayer
            String tUnits = pointVectorInfo[absolutePointVectorIndex][OneOf.PVIUnits];
            GraphDataLayer graphDataLayer = new GraphDataLayer(-1, 
                0, 1, 5, 6, -1, //x,y,u,v
                GraphDataLayer.DRAW_POINT_VECTORS, false, false,
                "", tUnits, //x,y axis title
                pointVectorInfo[absolutePointVectorIndex][OneOf.PVIBoldTitle],
                "(" + tUnits + ") " + 
                    spaceBeginTimeValue + ", " + //==spaceEndTimeValue
                    depthString, //title2
                courtesyValue,
                null, //title4
                table, null, null, 
                null, new Color(0xFF9900),
                GraphDataLayer.MARKER_TYPE_NONE, 0, 
                String2.parseDouble(pointVectorInfo[absolutePointVectorIndex][OneOf.PVISize]), // standardVector=e.g. 10m/s 
                -1);
            ArrayList graphDataLayers = new ArrayList();
            graphDataLayers.add(graphDataLayer);

            //make the image
            int imageWidth = oneOf.imageWidths()[whichImage];  
            int imageHeight = oneOf.imageHeights()[whichImage];
            BufferedImage bufferedImage = 
                SgtUtil.getBufferedImage(imageWidth, imageHeight);
            Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

            //draw the map
            SgtMap.makeMap(false, 
                SgtUtil.LEGEND_BELOW,
                oneOf.legendTitle1(),
                oneOf.legendTitle2(),
                oneOf.fullContextDirectory() + "images/", 
                oneOf.lowResLogoImageFile(),
                minLon, maxLon, minLat, maxLat,
                drawLandAsMask? "over" : "under",

                //grid data
                false, null, 1, 1, 0,
                "", "", "", "", "",

                SgtMap.NO_LAKES_AND_RIVERS,

                //contour
                false, null, 1, 1, 1, 
                "", null, "", "", "", "", "",

                graphDataLayers,
                g2D,
                0, 0, imageWidth, imageHeight,
                0,   //no coastline adjustment
                1); 

            //saveAsPng
            //nLon and nLat are in name, so .png extension works for all whichImage
            SgtUtil.saveAsPng(bufferedImage, dir + name);
            
        } else {
            String unitsValue = xPointDataSet.unitsOptions[0];  //for now always standard units      

            //make a time series graph
            //make the graphDataLayer
            GraphDataLayer graphDataLayer = new GraphDataLayer(
                -1,  //indicate pointVectorScreen
                3, 5, 6, -1, -1, //time,u,v  others not used
                GraphDataLayer.DRAW_STICKS, true, false,
                "Time", unitsValue, //not pointVector's units, which is std vector length (e.g., 10 m/s)
                pointVectorInfo[absolutePointVectorIndex][OneOf.PVIBoldTitle] + ", " + 
                    (minLat == maxLat? "" : String2.genEFormat10(minLat) + " to ") +
                    String2.genEFormat10(maxLat) + " N, " + 
                    (minLon == maxLon? "" : String2.genEFormat10(minLon) + " to ") +
                    String2.genEFormat10(maxLon) + " E. (Horizontal line = average)", 
                "Time series of " + 
                        (TimePeriods.getNHours(standardTimePeriodValue) == 0? "raw data" :
                        standardTimePeriodValue + " averages") + ". " +  //no legendTime
                    depthString + ". " +
                    "Stick length=speed, angle=direction.", //title2                        
                courtesyValue,
                null, //title4
                table, null, null,
                null, //colorMap
                new Color(0xFF9900),
                GraphDataLayer.MARKER_TYPE_NONE, 0,
                0, //standardMarkerSize
                GraphDataLayer.REGRESS_MEAN);

            ArrayList graphDataLayers = new ArrayList();
            graphDataLayers.add(graphDataLayer);

            //make the image
            int imageWidth = oneOf.imageWidths()[whichImage];  
            int imageHeight = imageWidth / 2 + 
                //only need room for small legend, so don't use imageHeights
                (whichImage == 0? 115 : 60); //but smallest has narrow&higher legend
            BufferedImage bufferedImage = 
                SgtUtil.getBufferedImage(imageWidth, imageHeight);
            Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

            //draw the graph
            oneOf.sgtGraph().makeGraph(false,
                "Time", unitsValue,
                SgtUtil.LEGEND_BELOW,
                oneOf.legendTitle1(),
                oneOf.legendTitle2(),
                oneOf.fullContextDirectory() + "images/", 
                oneOf.lowResLogoImageFile(),
                Double.NaN, Double.NaN, true, true,  false,//x isAscending, isTime, isLog
                Double.NaN, Double.NaN, true, false, false,
                graphDataLayers,
                g2D,
                0, 0, imageWidth, imageHeight, 2, //graph width/height
                SgtGraph.DefaultBackgroundColor, 1); //fontScale 

            //saveAsPng
            //nLon and nLat are in name, so .png extension works for all whichImage
            SgtUtil.saveAsPng(bufferedImage, dir + name);
        }
    }

    /**
     * Save trajectory data as a png (for HTTP GET requests).
     */
    public void trajectorySaveAsPng(TableDataSet tableDataSet, 
        Table table, String dir, String name, 
        String individualsValue[], int whichImage) throws Exception {

        //note: I considered making .png options part of Grid.saveAs
        //But then distributing gridSave as would require
        //distributing Sgt package.
        //For now, keep separate.
        String courtesyValue = tableDataSet.courtesy().length() == 0? null: 
            "Data courtesy of " + tableDataSet.courtesy(); 

        //make a map

        //make the graphDataLayer
        GraphDataLayer graphDataLayer = new GraphDataLayer(-1,
            0, 1, -1, 4, -1,  //x,y,data,id columns
            GraphDataLayer.DRAW_MARKERS, false, false,
            "", "",
            tableDataSet.datasetName(), 
            String2.toCSSVString(individualsValue), //title2
            courtesyValue,
            null, //title4
            table, null, null,
            null, Color.magenta,
            GraphDataLayer.MARKER_TYPE_FILLED_SQUARE, 
            GraphDataLayer.MARKER_SIZE_SMALL, 
            0, //standardVector
            GraphDataLayer.REGRESS_NONE);
        ArrayList graphDataLayers = new ArrayList();
        graphDataLayers.add(graphDataLayer);

        //make the image
        int imageWidth = oneOf.imageWidths()[whichImage];  
        int imageHeight = oneOf.imageHeights()[whichImage];
        BufferedImage bufferedImage = 
            SgtUtil.getBufferedImage(imageWidth, imageHeight);
        Graphics2D g2D = (Graphics2D)bufferedImage.getGraphics();

        //make lon,latRange
        double lonStats[] = table.getColumn(0).calculateStats();
        double latStats[] = table.getColumn(1).calculateStats();
        double lonRange[] = Math2.suggestLowHigh(
            lonStats[PrimitiveArray.STATS_MIN], 
            lonStats[PrimitiveArray.STATS_MAX]);
        double latRange[] = Math2.suggestLowHigh(
            latStats[PrimitiveArray.STATS_MIN], 
            latStats[PrimitiveArray.STATS_MAX]);
        if (oneOf.verbose()) String2.log(
            "  data lonMin=" + lonStats[PrimitiveArray.STATS_MIN] +
                  " lonMax=" + lonStats[PrimitiveArray.STATS_MAX] +
                  " latMin=" + latStats[PrimitiveArray.STATS_MIN] +
                  " latMax=" + latStats[PrimitiveArray.STATS_MAX] +
           "\n  map lonMin=" + lonRange[0] +
                  " lonMax=" + lonRange[1] +
                  " latMin=" + latRange[0] +
                  " latMax=" + latRange[1]);

        //if far from square, make the graph more square
        double lonDif = lonRange[1] - lonRange[0];
        double latDif = latRange[1] - latRange[0];
        if (latDif > lonDif * 2 && latDif < 40) {
            double center = (lonRange[0] + lonRange[1]) / 2;            
            lonRange = Math2.suggestLowHigh(center - latDif / 3, center + latDif / 3);
        } else if (lonDif > latDif * 2 && lonDif < 40) {
            double center = (latRange[0] + latRange[1]) / 2;
            latRange = Math2.suggestLowHigh(center - lonDif / 3, center + lonDif / 3);
        }
        if (oneOf.verbose()) String2.log(
            "  revised map lonMin=" + lonRange[0] +
                  " lonMax=" + lonRange[1] +
                  " latMin=" + latRange[0] +
                  " latMax=" + latRange[1]);


        //draw the map
        SgtMap.makeMap(false, 
            SgtUtil.LEGEND_BELOW,
            oneOf.legendTitle1(),
            oneOf.legendTitle2(),
            oneOf.fullContextDirectory() + "images/", 
            oneOf.lowResLogoImageFile(),
            lonRange[0], lonRange[1], latRange[0], latRange[1],
            drawLandAsMask? "over" : "under",

            //grid data
            false, null, 1, 1, 0,
            "", "", "", "", "",

            SgtMap.NO_LAKES_AND_RIVERS,

            //contour
            false, null, 1, 1, 1, 
            "", null, "", "", "", "", "",

            graphDataLayers,
            g2D,
            0, 0, imageWidth, imageHeight,
            0,   //no coastline adjustment
            1); 

        //saveAsPng
        //_small _medium _large is in file name, so .png extension works for all whichImage
        SgtUtil.saveAsPng(bufferedImage, dir + name);
    }

                                        
    /* junk pile
            int startPo = 0, endPo, queryLength = query.length();
        char searchFor = '=';
        while (startPo < queryLength) {
            if (query.charAt(startPo) == '"') {
                startPo++;
                endPo = query.indexOf("\"", startPo);
                if (endPo < 0) {  
                    error = "No closing quotes."; 
                    break;
                } else if (endPo + 1 < queryLength && query.charAt(endPo + 1) != searchFor) {       
                    error = "'" + searchFor + "' expected after close quotes at position " + (endPo+1) + ".";
                    break;
                } else {
                    alternate.add(query.substring(startPo, endPo));
                    startPo = endPo + 2; //after searchFor
                }
            } else {
                endPo = query.indexOf(searchFor, startPo);
                if (endPo < 0) 
                    endPo = queryLength;
                alternate.add(query.substring(startPo, endPo));
                startPo = endPo + 1; //after the searchFor char
            }
            searchFor = searchFor == '='? ',' : '=';
            String2.log("alternate=" + String2.toCSSVString(alternate.toArray()));
        }
        for (int i = 0; i < alternate.size(); i++) {
            if (Math2.odd(i))
                 alternate.set(i, ((String)alternate.get(i)).trim());
            else alternate.set(i, ((String)alternate.get(i)).trim().toLowerCase());
        }        
        if (Math2.odd(alternate.size()))
            alternate.add("");
    */
}
