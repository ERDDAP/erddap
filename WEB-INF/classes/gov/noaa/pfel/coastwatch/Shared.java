/* 
 * Shared Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;


import com.cohort.array.IntArray;
import com.cohort.array.StringArray;

import com.cohort.ema.*;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.hdf.SdsWriter;
import gov.noaa.pfel.coastwatch.pointdata.*;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.IntObject;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Vector;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;

/**
 * This class holds things that are shared throughout the CWBrowser program
 * and by all users, but which will be reset at least every resetMaxMinutes.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class Shared extends Thread {

    // ************ things set by run()   
    private String runError = ""; //set by run() if trouble
    private long resetTime;
    private OneOf oneOf;

    private Vector activeGridDataSets;
    private String activeGridDataSetOptions[]; 
    private String activeGridDataSet7Names[]; 
    private String activeGridDataSetTitles[];  

    private String activeVectorOptions[];
    private String activeVector7Names[];
    private int    activeVectorXDataSetIndexes[];
    private int    activeVectorYDataSetIndexes[];
    private Vector activeVectorContents;

    private String activePointDataSetOptions[]; 
    private String activePointDataSet7Names[]; 
    private Vector activePointDataSets;

    private String activePointVectorOptions[];
    private String activePointVector7Names[];
    private int    activePointVectorXDataSetIndexes[];
    private int    activePointVectorYDataSetIndexes[];
    private IntArray activePointVectorOriginalIndex = new IntArray();

    private String activeTrajectoryDataSetOptions[]; 
    private String activeTrajectoryDataSet7Names[]; 
    private Vector activeTrajectoryDataSets; //all are TableDataSets

    //stats and info from run()
    private long grdDirTime = 0;
    private long opendapFailTime = 0;
    private long newOpendapTime = 0; 
    private long getIndexInfoTime = 0;
    private int localGrdFileCount = 0;
    private int opendapDatesCount = 0;

    /** 
     * The constructor for Shared to prepare for run. 
     */
    public Shared(OneOf oneOf) {
        this.oneOf = oneOf;
    }

    /**
     * This reads properties from the resourceBundle and 
     * looks on the hard drive and via OPeNDAP 
     * to see what data is currently available.
     */
    public void run() {
        ResourceBundle2 classRB2 = oneOf.classRB2();
        Math2.gcAndWait("Shared.run"); Math2.gcAndWait("Shared.run"); //before getMemoryInUse() in run()  //so getMemoryInUse more accurate
        long memoryInUse = Math2.getMemoryInUse();
        try {
            long startTime = System.currentTimeMillis();
            long tTime;

            String errorInMethod = String2.ERROR + " in Shared.run for " + 
                oneOf.shortClassName() + ":\n";
            String2.log("\n" + String2.makeString('*', 80) +  
                "\nShared.run for " + oneOf.shortClassName() + " " + 
                Calendar2.getCurrentISODateTimeStringLocalTZ());
            String2.log(Math2.memoryString());

            //update the cached point datasets first, since it takes a long time and since
            //ndbc point datasets rely on its information
            int updateCachedDataSetsEveryNMinutes = 
                classRB2.getInt("updateCachedDataSetsEveryNMinutes", Integer.MAX_VALUE);
            boolean updateCachedDataSets = false;
            if (updateCachedDataSetsEveryNMinutes < 
                (System.currentTimeMillis() - oneOf.getCachedDataSetsLastUpdated()) / Calendar2.MILLIS_PER_MINUTE) { 

                if (oneOf.getCachedDataSetsLastUpdated() == 0) {
                    //don't do update on first run of shared
                    oneOf.setCachedDataSetsLastUpdated(1); //do it next time
                } else {
                    //do the update
                    //point datasets are updated below, based on updateCachedDataSets
                    updateCachedDataSets = true; 
                    oneOf.setCachedDataSetsLastUpdated(System.currentTimeMillis());
                    try {

                        NdbcMetStation.verbose = oneOf.verbose();
                        NdbcMetStation.addLastNDaysInfo(oneOf.pointsDir() + "ndbcMet/", 
                            5, //5 does 5day if possible, else 45 day if possible
                            false); //not testMode
                    } catch (Exception e) {
                        String msg = errorInMethod + 
                            String2.ERROR + " in NdbcMetStation.addLastNDaysInfo:\n" + 
                            MustBe.throwableToString(e);
                        String2.log(msg);
                        oneOf.email(oneOf.emailEverythingTo(), 
                            "NdbcMet update failed for " + oneOf.shortClassName(), msg);
                    }
                }
            }

            //test of email
            //Test.error("This is a test of emailing an error in Shared.run.");

            //get palette options
            String[] paletteOptions = String2.split(classRB2.getString("gridPalette.options", null), '\f');
            for (int i = 0; i < paletteOptions.length; i++)
                Test.ensureTrue(
                    File2.isFile(oneOf.fullPaletteDirectory() + paletteOptions[i] + ".cpt"), 
                    "The .cpt file for gridPalette.option " + paletteOptions[i] + " was not found.");
            String[] gridPaletteScaleOptions   = String2.split(classRB2.getString("gridPaletteScale.options",   null), '\f');

            //prepare to look for available datasets
            //  if local, find out what local grd files are available and store in ActiveXxx vectors
            //  if opendap, find out what files are available and store in ActiveXxx vectors
            StringArray tActiveGridDataSetOptions = new StringArray(); //holds active dataSet options
            StringArray tActiveGridDataSet7Names = new StringArray(); //holds active dataSet 7char names
            activeGridDataSets = new Vector(); //holds []'s with activeTimePeriodOptions+Contains for each dataset

            //add "(None)" dataset
            tActiveGridDataSetOptions.add(OneOf.NO_DATA);
            tActiveGridDataSet7Names.add("L!!none");
            activeGridDataSets.add(null);

            //add "Bathymetry" dataset
            tActiveGridDataSetOptions.add(SgtMap.BATHYMETRY_BOLD_TITLE);
            tActiveGridDataSet7Names.add(SgtMap.BATHYMETRY_7NAME);
            activeGridDataSets.add(null);

            //check before (and after) to ensure that the DataServer server is up
            oneOf.ensureDataServerIsUp();

            //*** set up grid datasets 
            String dataSetList[] = String2.split(classRB2.getString("dataSetList",  null), '`');
            String lastCategory = ""; //matches no-category from fileNameUtility.getCategory
            for (int dataSetI = OneOf.N_DUMMY_GRID_DATASETS; dataSetI < dataSetList.length; dataSetI++) { //2: skip 0=None 1=Bathymetry
                String2.log("\n*** " + oneOf.shortClassName() + 
                    " start dataSetI=" + dataSetI + "=" + dataSetList[dataSetI] + " " + Math2.memoryString());
                if (isInterrupted()) {
                    String2.log("*** Shared isInterrupted, so it is stopping.");
                    return;
                }

                //ensure InfoUrlExists   //outside of try catch    error stops run()
                oneOf.fileNameUtility().ensureInfoUrlExists(dataSetList[dataSetI]);
/*
                //insert category separator?
                String category = oneOf.fileNameUtility().getCategory(dataSetList[dataSetI]);
String2.log("!!!Category=" + category);
                if (!category.equals(lastCategory)) {
                    String dashes = String2.makeString('-', (72 - category.length()) / 2);
                    tActiveGridDataSetOptions.add(dashes + " " + category + " " + dashes);
                    tActiveGridDataSet7Names.add("L!!none");
                    activeGridDataSets.add(null);
                    lastCategory = category;
                }
*/
                boolean success;
                //uncomment next line to test switchover
                //if (dataSetList[dataSetI].charAt(0) == 'O') success = false; else  //normally, this line is commented out
                success = tryToLoad(dataSetList[dataSetI], paletteOptions, gridPaletteScaleOptions, 
                        tActiveGridDataSetOptions, tActiveGridDataSet7Names, activeGridDataSets);  
                char char0 = dataSetList[dataSetI].charAt(0); 
                /* This is no longer a good idea.
                if (!success && (char0 == 'T' || char0 == 'U')) {
                    //try to load the local version of that dataset
                    String local = 'L' + dataSetList[dataSetI].substring(1);

                    try {
                        //ensure InfoUrlExists   //inside try catch   error stops 'L' attempt, doesn't stop run()
                        oneOf.fileNameUtility().ensureInfoUrlExists(local);

                        success = tryToLoad(local, paletteOptions, gridPaletteScaleOptions, 
                            tActiveGridDataSetOptions, tActiveGridDataSet7Names, activeGridDataSets);  
                        String2.log("SWITCH OVER: loading Opendap/Thredds dataset " + dataSetList[dataSetI] + " failed.\n" +
                            "  Loading local dataset " + local + " success=" + success);
                    } catch (Exception e) {
                        String2.log(MustBe.throwableToString(e));
                    }
                }*/

            } //end of dataSets loop

            //check (before and) after to ensure that the DataServer server is up
            oneOf.ensureDataServerIsUp();

            //store the dataSet info
            if (tActiveGridDataSetOptions.size() <= 1)
                throw new RuntimeException(errorInMethod + "no valid datasets!");
            activeGridDataSetOptions = tActiveGridDataSetOptions.toArray();
            activeGridDataSet7Names  = tActiveGridDataSet7Names.toArray();
            if (oneOf.verbose())
                String2.log("activeGridDataSetOptions: " + 
                    String2.toNewlineString(tActiveGridDataSetOptions.toArray()));

            //make sure no duplicate options
            int n = activeGridDataSetOptions.length; 
            String tStrings[] = new String[n];
            System.arraycopy(activeGridDataSetOptions, 0, tStrings, 0, n);
            Arrays.sort(tStrings);
            for (int i = 1; i < n; i++) {   //1..   since compare i to i-1
                if (tStrings[i].equals(tStrings[i-1])) 
                    Test.error(errorInMethod + "two grid datasets with same Option: " + 
                        tStrings[i] + "\n" + "sorted options:\n" + String2.toNewlineString(tStrings));
            }

            //call gc
            //Math2.incgc(100); //2013-12-05 commented out. Let Java handle memory.

            //*** look for available vector info
            long vectorInfoTime = System.currentTimeMillis();
            Vector tActiveVectorOptions = new Vector(); //holds active vector options
            Vector tActiveVector7Names = new Vector(); //holds active vector 7names
            Vector tActiveVectorXDataSetIndexes = new Vector(); //holds active vectorXDataset indexes 
            Vector tActiveVectorYDataSetIndexes = new Vector(); //holds active vectorYDataset indexes
            activeVectorContents = new Vector(); //holds []'s with activeTimePeriodOptions+Titles+Dates for each dataset
            int nVectors = oneOf.vectorOptions().length;
            for (int vectorI = 0; vectorI < nVectors; vectorI++) {
                if (isInterrupted()) {
                    String2.log("*** Shared isInterrupted, so it is stopping.");
                    return;
                }

                //0 is always "(None)"
                if (vectorI == 0) {         
                    //this mirrors code for normal success below
                    tActiveVectorOptions.add(oneOf.vectorOptions()[vectorI]);
                    tActiveVector7Names.add(oneOf.vectorInfo()[vectorI][OneOf.VIInternalName]);
                    tActiveVectorXDataSetIndexes.add(Integer.valueOf(-1)); //-1 is place holder for XDataSet
                    tActiveVectorYDataSetIndexes.add(Integer.valueOf(-1)); 
                    activeVectorContents.add(new Object[]{
                        new String[]{},   //String2.toStringArray(tActiveVectorTimePeriodOptions), 
                        new String[]{""}, //String2.toStringArray(tActiveVectorTimePeriodTitles), 0=main title
                        null});
                    continue;
                }

                //what is the number of the x and y activeGridDataSet for this vector?
                int xDataSetIndex = -1;
                int yDataSetIndex = -1;
                String xDataSetInternalName = oneOf.vectorInfo()[vectorI][OneOf.VIXDataSet];
                String yDataSetInternalName = oneOf.vectorInfo()[vectorI][OneOf.VIYDataSet];
                for (int dataSetI = OneOf.N_DUMMY_GRID_DATASETS; dataSetI < activeGridDataSets.size(); dataSetI++) { //skip 0=none, 1=Bathymetry
                    String ts = ((GridDataSet)activeGridDataSets.get(dataSetI)).internalName;
                    if (ts.equals(xDataSetInternalName)) xDataSetIndex = dataSetI;
                    if (ts.equals(yDataSetInternalName)) yDataSetIndex = dataSetI;
                }
                if (xDataSetIndex == -1 || yDataSetIndex == -1) {
                    String2.log(String2.ERROR + ": vector " + vectorI + 
                        " xDataSet=" + xDataSetInternalName + 
                        " or yDataSet=" + yDataSetInternalName + " not found.");
                    continue;
                }

                //get the relevant activeGridDataSets 
                GridDataSet xGridDataSet = (GridDataSet)activeGridDataSets.get(xDataSetIndex);
                String[] xActiveTimePeriodOptions   = xGridDataSet.activeTimePeriodOptions;
                String[] xActiveTimePeriodTitles    = xGridDataSet.activeTimePeriodTitles;
                Vector   xActiveTimePeriodTimes     = xGridDataSet.activeTimePeriodTimes;
                GridDataSet yGridDataSet = (GridDataSet)activeGridDataSets.get(yDataSetIndex);
                String[] yActiveTimePeriodOptions   = yGridDataSet.activeTimePeriodOptions;
                String[] yActiveTimePeriodTitles    = yGridDataSet.activeTimePeriodTitles;
                Vector   yActiveTimePeriodTimes     = yGridDataSet.activeTimePeriodTimes;

                //for each xActiveTimePeriodOption
                Vector tActiveVectorTimePeriodOptions    = new Vector();//holds active vector's timePeriodOptions
                Vector tActiveVectorTimePeriodTitles     = new Vector();//holds active vector's timePeriodTitles 
                Vector tActiveVectorTimePeriodTimes      = new Vector();//holds String[]'s with activeVTimePeriodTimes for each timePeriod
                tActiveVectorTimePeriodTitles.add(""); //always add a blank main title
                for (int xATPOIndex = 0; xATPOIndex < xActiveTimePeriodOptions.length; xATPOIndex++) {
                    //find the matching yActiveTimePeriodOptions
                    String xATPO = xActiveTimePeriodOptions[xATPOIndex];
                    int yATPOIndex = String2.indexOf(yActiveTimePeriodOptions, xATPO);
                    if (yATPOIndex < 0)
                        continue;

                    //look for dates where both are available
                    String[] xDates   = (String[])xActiveTimePeriodTimes.get(xATPOIndex);
                    String[] yDates   = (String[])yActiveTimePeriodTimes.get(yATPOIndex);
                    Vector commonDates = new Vector();

                    //do it like a merge sort (but all dates must be valid)
                    //An invalid high value would cause all subsequent dates to be not matched.
                    long commonTime = System.currentTimeMillis();
                    int xDateIndex = 0, yDateIndex = 0;
                    int nXDates = xDates.length, nYDates = yDates.length;
                    while (xDateIndex < nXDates && yDateIndex < nYDates) {
                        int dif = xDates[xDateIndex].compareTo(yDates[yDateIndex]);
                        if (dif < 0) xDateIndex++;
                        else if (dif > 0) yDateIndex++;
                        else {
                            commonDates.add(xDates[xDateIndex]);
                            xDateIndex++;
                            yDateIndex++;
                        }
                    }
                    //String2.log("Shared vector commonTime=" + (System.currentTimeMillis() - commonTime));
                    //if some common dates were found, save the info
                    if (commonDates.size() > 0) {
                        tActiveVectorTimePeriodOptions.add(xATPO);
                        tActiveVectorTimePeriodTitles.add(xActiveTimePeriodTitles[xATPOIndex + 1]); //+1 since 0 is main title
                        tActiveVectorTimePeriodTimes.add(String2.toStringArray(commonDates.toArray()));
                    //if (oneOf.verbose()) String2.log("vectorI=" + vectorI + 
                    //    " activeVectorTimeOption=" + xATPO
                    //    //+ " dates=" + String2.noLongerThanDots(
                    //    //    String2.toCSSVString(String2.toStringArray(commonDates.toArray())), 200)
                    //    );
                    }
                } //end of xATPOIndex loop

                //if there were activeTimePeriods, save 
                //later usage: activeGridDatasets.get(i) 
                //  -> a dataSet name and a list of its activeTimePeriods 
                if (!tActiveVectorTimePeriodOptions.isEmpty()) {         
                    String tOption = oneOf.vectorOptions()[vectorI];
                    tActiveVectorOptions.add(tOption);
                    tActiveVector7Names.add(oneOf.vectorInfo()[vectorI][OneOf.VIInternalName]);
                    tActiveVectorXDataSetIndexes.add(Integer.valueOf(xDataSetIndex));
                    tActiveVectorYDataSetIndexes.add(Integer.valueOf(yDataSetIndex));
                    activeVectorContents.add(new Object[]{
                        String2.toStringArray(tActiveVectorTimePeriodOptions.toArray()), 
                        String2.toStringArray(tActiveVectorTimePeriodTitles.toArray()), 
                        tActiveVectorTimePeriodTimes});
                    if (oneOf.verbose()) String2.log("vectorI=" + vectorI + 
                        " xDataSet=" + xDataSetInternalName + 
                        " yDataSet=" + yDataSetInternalName + 
                        " activeVectorTimeOptions=" + 
                        String2.toCSSVString(String2.toStringArray(
                            tActiveVectorTimePeriodOptions.toArray())));                    
                }
            } //end of Vector loop

            //store the Vector info
            //if (tActiveVectorOptions.size() == 0)
            //    throw new RuntimeException(errorInMethod + "no valid vector datasets!");
            activeVectorOptions = String2.toStringArray(tActiveVectorOptions.toArray());
            activeVector7Names = String2.toStringArray(tActiveVector7Names.toArray());
            activeVectorXDataSetIndexes = String2.toIntArray(tActiveVectorXDataSetIndexes.toArray());
            activeVectorYDataSetIndexes = String2.toIntArray(tActiveVectorYDataSetIndexes.toArray());
            if (oneOf.verbose()) {
                String2.log(
                    "activeVectorOptions: " + String2.toCSSVString(activeVectorOptions) + "\n" +
                    "activeVector7Names: " + String2.toCSSVString(activeVector7Names) + "\n" +
                    "activeVectorXDataSetIndexes: " + String2.toCSSVString(activeVectorXDataSetIndexes) + "\n" +
                    "activeVectorYDataSetIndexes: " + String2.toCSSVString(activeVectorYDataSetIndexes));
            }
            vectorInfoTime = System.currentTimeMillis() - vectorInfoTime;

            //call gc
            //Math2.incgc(100); //2013-12-05 commented out. Let Java handle memory.

            //*** generate the PointDataSet info
            String2.log("pointsDir=" + oneOf.pointsDir());
            activePointDataSets = new Vector();
            try {
                if (oneOf.pointsDir() != null) 
                    NdbcMetStation.addPointDataSets(activePointDataSets, 
                        oneOf.pointsDir() + "ndbcMet/",
                        oneOf.regionMinX(), oneOf.regionMaxX(),
                        oneOf.regionMinY(), oneOf.regionMaxY());
            } catch (Exception e) {
                String msg = errorInMethod + 
                    String2.ERROR + " in NdbcMetStation.addPointDataSets:\n" + 
                    MustBe.throwableToString(e);
                String2.log(msg);
                oneOf.email(oneOf.emailEverythingTo(), 
                    "NdbcMet failure in " + oneOf.shortClassName(), msg);
            }

            if (oneOf.pointsDir() != null) {

                /* As of 2008-07-07, MBARI datasets are inactive (they need work)
                ArrayList activeCaches = new ArrayList();
                long cTime = System.currentTimeMillis();
                try {
                    PointDataSetFromStationVariables.makeMbariSqCachesAndDataSets(
                        oneOf.pointsDir(), 
                        oneOf.regionMinX(), oneOf.regionMaxX(),
                        oneOf.regionMinY(), oneOf.regionMaxY(), 
                        //update=true applies regardless of whether station is accepted/rejected based on min/max/X/Y.
                        updateCachedDataSets, 
                        false, //throwExceptionIfAnyTrouble
                        activeCaches, activePointDataSets); 
                } catch (Exception e) {
                    String msg = errorInMethod + 
                        String2.ERROR + " in makeMbariSqCachesAndDataSets:\n" + 
                        MustBe.throwableToString(e);
                    String2.log(msg);
                    oneOf.email(oneOf.emailEverythingTo(), 
                        "Mbari failure in " + oneOf.shortClassName(), msg);
                }

                try {              
                    PointDataSetFromStationVariables.makeMbariNrtCachesAndDataSets(
                        oneOf.pointsDir(), 
                        oneOf.regionMinX(), oneOf.regionMaxX(),
                        oneOf.regionMinY(), oneOf.regionMaxY(), 
                        updateCachedDataSets,
                        false, //throwExceptionIfAnyTrouble
                        activeCaches, activePointDataSets); 
                } catch (Exception e) {
                    String msg = errorInMethod + 
                        String2.ERROR + " in makeMbariNrtCachesAndDataSets:\n" + 
                        MustBe.throwableToString(e);
                    String2.log(msg);
                    oneOf.email(oneOf.emailEverythingTo(), 
                        "Mbari failure in " + oneOf.shortClassName(), msg);
                }

                //Send email if update took >10 minutes.
                //That is indicative of trouble: 
                //  1 big or >1 small data sets needed to be regenerated.
                //Typical time for updateCachedDataSets=true and 0 regenerated is 25 s.
                cTime = System.currentTimeMillis() - cTime;
                if (cTime > 10 * Calendar2.MILLIS_PER_MINUTE) {
                    oneOf.email(oneOf.emailEverythingTo(), 
                        "Shared.run for " + oneOf.shortClassName(),
                        "Updating MBARI caches in shared.run for " + 
                            oneOf.shortClassName() + " took " + 
                            Calendar2.elapsedTimeString(cTime));
                }
                */
            }

            //sort them by option name
            Collections.sort(activePointDataSets);
            //then insert the "0)None" option
            activePointDataSets.add(0, new PointDataSetNone());
            //generate arrays of options and titles
            activePointDataSetOptions = new String[activePointDataSets.size()];
            activePointDataSet7Names  = new String[activePointDataSets.size()];
            for (int i = 0; i < activePointDataSets.size(); i++) {
                PointDataSet pds = (PointDataSet)activePointDataSets.get(i);
                activePointDataSetOptions[i] = pds.option;
                activePointDataSet7Names[i] = pds.internalName;
            }
            String2.log("activePointDataSetOptions=" + String2.toCSSVString(activePointDataSetOptions));

            //call gc
            //Math2.incgc(100); //2013-12-05 commented out. Let Java handle memory.

            //*** look for available pointVector info
            long pointVectorInfoTime = System.currentTimeMillis();
            Vector tActivePointVectorOptions = new Vector(); //holds active point vector options
            Vector tActivePointVector7Names = new Vector(); //holds active point vector 7names
            Vector tActivePointVectorXDataSetIndexes = new Vector(); //holds active pointVectorXDataset indexes 
            Vector tActivePointVectorYDataSetIndexes = new Vector(); //holds active pointVectorYDataset indexes
            int nPointVectors = oneOf.pointVectorOptions().length;
            for (int pointVectorI = 0; pointVectorI < nPointVectors; pointVectorI++) {
                if (isInterrupted()) {
                    String2.log("*** Shared isInterrupted, so it is stopping.");
                    return;
                }

                String tOption = oneOf.pointVectorOptions()[pointVectorI];

                //always allow "(None)"
                if (pointVectorI == 0) {         
                    //this mirrors code for normal success below
                    tActivePointVectorOptions.add(tOption);
                    tActivePointVector7Names.add(oneOf.pointVectorInfo()[pointVectorI][OneOf.PVIInternalName]);
                    tActivePointVectorXDataSetIndexes.add(Integer.valueOf(-1)); //-1 is place holder for XDataSet
                    tActivePointVectorYDataSetIndexes.add(Integer.valueOf(-1)); 
                    activePointVectorOriginalIndex.add(pointVectorI);
                    continue;
                }

                //what is the number of the x and y activeGridDataSet for this pointVector?
                int xDataSetIndex = -1;
                int yDataSetIndex = -1;
                String xDataSetInternalName = oneOf.pointVectorInfo()[pointVectorI][OneOf.PVIXDataSet];
                String yDataSetInternalName = oneOf.pointVectorInfo()[pointVectorI][OneOf.PVIYDataSet];
                for (int dataSetI = OneOf.N_DUMMY_OTHER_DATASETS; dataSetI < activePointDataSets.size(); dataSetI++) { //skip 0=none
                    String ts = ((PointDataSet)activePointDataSets.get(dataSetI)).internalName;
                    if (ts.equals(xDataSetInternalName)) xDataSetIndex = dataSetI;
                    if (ts.equals(yDataSetInternalName)) yDataSetIndex = dataSetI;
                }
                if (xDataSetIndex == -1 || yDataSetIndex == -1) {
                    String2.log(String2.ERROR + ": pointVector " + pointVectorI + " " + tOption + 
                            " xDataSet=" + xDataSetInternalName + "(" + xDataSetIndex + 
                        ") or yDataSet=" + yDataSetInternalName + "(" + yDataSetIndex + ") not found.");
                    continue;
                }

                //get relevant the activePointDataSets 
                PointDataSet xPointDataSet = (PointDataSet)activePointDataSets.get(xDataSetIndex);
                PointDataSet yPointDataSet = (PointDataSet)activePointDataSets.get(yDataSetIndex);

                //ensure the time data is available for x and yPointDataSet overlap
                if (xPointDataSet.firstTime.after(yPointDataSet.lastTime) ||
                    yPointDataSet.firstTime.after(xPointDataSet.lastTime)) {
                    String2.log(String2.ERROR + ": pointVector " + pointVectorI + " " + tOption +
                        " x/y first/last times don't overlap:\n" +
                        " xFirst=" + Calendar2.formatAsISODateTimeT(xPointDataSet.firstTime) + 
                        " xLast="  + Calendar2.formatAsISODateTimeT(xPointDataSet.lastTime) + 
                        " yFirst=" + Calendar2.formatAsISODateTimeT(yPointDataSet.firstTime) + 
                        " yLast="  + Calendar2.formatAsISODateTimeT(yPointDataSet.lastTime));
                    continue;
                }

                //save this pointVector
                //later usage: activeGridDatasets.get(i) 
                //  -> a dataSet name 
                tActivePointVectorOptions.add(tOption);
                tActivePointVector7Names.add(oneOf.pointVectorInfo()[pointVectorI][OneOf.PVIInternalName]);
                tActivePointVectorXDataSetIndexes.add(Integer.valueOf(xDataSetIndex));
                tActivePointVectorYDataSetIndexes.add(Integer.valueOf(yDataSetIndex));
                activePointVectorOriginalIndex.add(pointVectorI);
                if (oneOf.verbose()) 
                    String2.log("pointVectorI=" + pointVectorI + " " + tOption + " is active."); 
            } //end of PointVector loop

            //store the PointVector info
            //if (tActivePointVectorOptions.size() == 0)
            //    throw new RuntimeException(errorInMethod + "no valid PointVector datasets!");
            activePointVectorOptions = String2.toStringArray(tActivePointVectorOptions.toArray());
            activePointVector7Names = String2.toStringArray(tActivePointVector7Names.toArray());
            activePointVectorXDataSetIndexes = String2.toIntArray(tActivePointVectorXDataSetIndexes.toArray());
            activePointVectorYDataSetIndexes = String2.toIntArray(tActivePointVectorYDataSetIndexes.toArray());
            if (oneOf.verbose()) {
                String2.log(
                    "activePointVectorOptions: " + String2.toCSSVString(activePointVectorOptions) + "\n" +
                    "activePointVector7Names: " + String2.toCSSVString(activePointVector7Names) + "\n" +
                    "activePointVectorXDataSetIndexes: " + String2.toCSSVString(activePointVectorXDataSetIndexes) + "\n" +
                    "activePointVectorYDataSetIndexes: " + String2.toCSSVString(activePointVectorYDataSetIndexes));
            }
            pointVectorInfoTime = System.currentTimeMillis() - pointVectorInfoTime;

            //call gc
            //Math2.incgc(100); //2013-12-05 commented out. Let Java handle memory.

            //*** generate the TrajectoryDataSet info
            activeTrajectoryDataSets = new Vector();
            if (oneOf.pointsDir() != null) {
                //put each dataset in its own try/catch
                try {
                    activeTrajectoryDataSets.add(new TableDataSet4DNc(
                        "4NBmeto", "NDBC Meteorological",
                        oneOf.pointsDir() + "ndbcMet/",
                        "NDBC_.+_met\\.nc"));

                } catch (Exception e) {
                    String msg = String2.ERROR + " while generating ndbcMet Trajectory data sets:\n" + 
                        MustBe.throwableToString(e);
                    String2.log(msg);
                    oneOf.email(oneOf.emailEverythingTo(), 
                        "NdbcMet trajectory failed for " + oneOf.shortClassName(), msg);
                }
            }

            //sort them by option name
            Collections.sort(activeTrajectoryDataSets);
            //then insert the "0)None" option
            activeTrajectoryDataSets.add(0, new TableDataSetNone());
            //generate arrays of options and titles
            activeTrajectoryDataSetOptions = new String[activeTrajectoryDataSets.size()];
            activeTrajectoryDataSet7Names  = new String[activeTrajectoryDataSets.size()];
            for (int i = 0; i < activeTrajectoryDataSets.size(); i++) {
                TableDataSet tds = (TableDataSet)activeTrajectoryDataSets.get(i);
                activeTrajectoryDataSetOptions[i] = tds.datasetName();
                activeTrajectoryDataSet7Names[i] = tds.internalName();
            }
            String2.log("activeTrajectoryDataSetOptions=" + String2.toCSSVString(activeTrajectoryDataSetOptions));

            if (isInterrupted()) {
                String2.log("*** Shared isInterrupted, so it is stopping.");
                return;
            }


            //*** print lots of useful information
            resetTime = System.currentTimeMillis() - startTime;
            Math2.gcAndWait("Shared.run"); Math2.gcAndWait("Shared.run"); //before getMemoryInUse() in run()   //so getMemoryInUse more accurate
            String2.log( 
                "Shared.run ending stats:\n" +
                "  Memory used (KB) = " + ((Math2.getMemoryInUse() - memoryInUse) / 1024) + "\n" +
                "  Local: grdDirTime = " + grdDirTime + "\n" +
                "    dirTimes broken down: getTime = " + RegexFilenameFilter.getTime + 
                    ", matchRegexTime = " + RegexFilenameFilter.matchTime + 
                    ", sortTime = " + RegexFilenameFilter.sortTime + "\n" +
                "  OOpendap: newOpendapTime = " + newOpendapTime + 
                    ", getIndexInfoTime = " + getIndexInfoTime + 
                    ", opendapFailTime = " + opendapFailTime + "\n" +
                "  VectorInfoTime = " + vectorInfoTime + 
                    ", PointVectorInfoTime = " + pointVectorInfoTime + "\n" +
                "Shared.run for " + oneOf.shortClassName() + " done. TOTAL TIME=" + 
                    Calendar2.elapsedTimeString(resetTime) + "\n");
        } catch (Exception e) {
            runError = MustBe.throwable(String2.ERROR + " in Shared.run.", e);
            String2.log(runError.toString());
        }
    }

    /**
     * This tries to load a dataset.
     *
     * @param internalName
     * @param paletteOptions the valid palettes
     * @param paletteScaleOptions the valid palette scales
     * @param tActiveGridDataSetOptions  receives some of the results (if successfull)
     * @param tActiveGridDataSet7Names  receives some of the results (if successfull)
     * @param activeGridDataSets receives some of the results (if successfull)
     * @return true if data was found
     */
    private boolean tryToLoad(String internalName, String[] paletteOptions, String[] paletteScaleOptions, 
            StringArray tActiveGridDataSetOptions, StringArray tActiveGridDataSet7Names,
            Vector activeGridDataSets) {

        String errorInMethod = String2.ERROR + " in Shared.tryToLoad for " + internalName + ":\n";

        try {
            //all types of datasets have XXXxxxxFileInfo in DataSet.properties
            ResourceBundle2 classRB2 = oneOf.classRB2();
            ResourceBundle2 dataSetRB2 = oneOf.dataSetRB2();
            char firstChar = internalName.charAt(0);
            String dataSetFileInfo[] = String2.split(dataSetRB2.getString(internalName + "FileInfo",  null), '`');

            //get optional dataSetFGCD substitutions
            //substring(1) because info is same for "L" and "O" datasets,
            //so info is stored without "L" or "O"
            //This works for my "T" datasets, but wouldn't normally.
            String sixName = internalName.substring(1);
            String tFGDC = dataSetRB2.getString(sixName + "FGDC",  null); //null means none available
            if (tFGDC == null) String2.log("NO FGDC DATA FOR " + sixName);

            GridDataSet tGridDataSet = null;
            if (firstChar == 'U') {
                //special handling for Thredds datasets 
                //it doesn't rely on anything else in the .properties file
                String2.log(oneOf.shortClassName() + " shared.tryToLoad a Thredds dataset: " + internalName);
                Test.ensureEqual(dataSetFileInfo.length, 13, 
                    errorInMethod + "Incorrect FileInfo length in DataSet.properties.");
                tGridDataSet = new GridDataSetThredds(oneOf.fileNameUtility(),
                    internalName, dataSetFileInfo[0], dataSetFileInfo[12], dataSetFileInfo[1], 
                    dataSetFileInfo[2], dataSetFileInfo[3], 
                    String2.parseInt(dataSetFileInfo[4]), //good: null -> Integer.MAX_VALUE, and restricts data access
                    dataSetFileInfo[5],
                    tFGDC, oneOf.fullResetFlagDirectory(),
                    dataSetFileInfo[6], 
                    String2.parseDouble(dataSetFileInfo[7]), 
                    String2.parseDouble(dataSetFileInfo[8]), 
                    dataSetFileInfo[9], 
                    String2.parseDouble(dataSetFileInfo[10]), 
                    String2.parseDouble(dataSetFileInfo[11]));
            } else if (firstChar == 'T') {
                //special handling for Opendap datasets 
                //This relies on 7nameFileInfo, 7nameInfoUrl, (opt) 6nameFGDC, and (opt) 6nameBoldTitle, 
                //  but nothing else in the .properties file.
                //public GridDataSetOpendap(String internalName, 
                //  String gridName, String title,
                //  String baseUrl, String[] timePeriodUrls, int[] timePeriodNHours,
                //  String tPalette, String tPaletteScale, String tPaletteMin, String tPaletteMax, 
                //  int tDaysTillDataAccessAllowed, String tAnomalyDataSet, String fgdc,
                //  String flagDirectory,
                //  String tDefaultUnits, double tAltScaleFactor, double tAltOffset,
                //  String tAltUnits, double tAltMin, double tAltMax) throws Exception {
                // Fields: 0=gridVarName` 1=baseUrl` 2=timeUrlCsv` 3=nHoursCsv` 
                //   4=scale` 5=paletteMin` 6=paletteMax` 7=daysTillDataAccessAllowed` 8=anomaly dataset`
                //   9=defaultUnits (either 'S'= standard SI units (the units in the original file), 'A'=alternate units) ` 
                //   10=altScaleFactor (1st step to get data from standard units to altUnits)` 11=altOffset (2nd step)` 
                //   12=altUDUnits` 13=altMin` 14=altMax `15=Palette
                String2.log(oneOf.shortClassName() + " shared.tryToLoad an Opendap dataset: " + internalName);
                Test.ensureEqual(dataSetFileInfo.length, 16, 
                    errorInMethod + "Incorrect FileInfo length in DataSet.properties.");
                tGridDataSet = new GridDataSetOpendap(
                    internalName, dataSetFileInfo[0], dataSetRB2.getString(sixName + "BoldTitle",  null),
                    dataSetFileInfo[1], //url
                    String2.split(dataSetFileInfo[2], ','), 
                    String2.toIntArray(String2.split(dataSetFileInfo[3], ',')), 
                    dataSetFileInfo[15], dataSetFileInfo[4], //palette
                    dataSetFileInfo[5], dataSetFileInfo[6], 
                    String2.parseInt(dataSetFileInfo[7]), //access   good that null -> Integer.MAX_VALUE, and restricts data access
                    dataSetFileInfo[8], //anom
                    tFGDC, oneOf.fullResetFlagDirectory(), //flag
                    dataSetFileInfo[9], //units
                    String2.parseDouble(dataSetFileInfo[10]), 
                    String2.parseDouble(dataSetFileInfo[11]), 
                    dataSetFileInfo[12], //alt units
                    String2.parseDouble(dataSetFileInfo[13]), 
                    String2.parseDouble(dataSetFileInfo[14]));
            } else if (firstChar == 'A') {
                //special handling for Anomaly datasets 
                //it doesn't rely on anything else in the .properties file
                String2.log(oneOf.shortClassName() + " shared.tryToLoad an Anomaly dataset: " + internalName);
                Test.ensureEqual(dataSetFileInfo.length, 6, 
                    errorInMethod + "Incorrect FileInfo length in DataSet.properties.");

                //find last matching gridDataSet6Name  
                String gridDataSet6Name = dataSetFileInfo[OneOf.AIGridDataSet6Name];
                String climatologyDataSet6Name = dataSetFileInfo[OneOf.AIClimatologyDataSet6Name];
                int gridDataSetIndex = -1;
                int climatologyDataSetIndex = -1;
                for (int i = OneOf.N_DUMMY_GRID_DATASETS; i < tActiveGridDataSet7Names.size(); i++) { //2.. since 0=none 1=bath
                    if (tActiveGridDataSet7Names.get(i).substring(1).equals(gridDataSet6Name))
                        gridDataSetIndex = i;
                    if (tActiveGridDataSet7Names.get(i).substring(1).equals(climatologyDataSet6Name))
                        climatologyDataSetIndex = i;
                }
                Test.ensureNotEqual(gridDataSetIndex, -1, 
                    errorInMethod + "No active '" + gridDataSet6Name + "' gridDataSet found.");
                Test.ensureNotEqual(climatologyDataSetIndex, -1, 
                    errorInMethod + "No active '" + climatologyDataSet6Name + "' climatologyDataSet found.");
                  
                //construct the dataset
                tGridDataSet = new GridDataSetAnomaly(internalName, oneOf.fileNameUtility(),
                    (GridDataSet)activeGridDataSets.get(gridDataSetIndex),
                    (GridDataSet)activeGridDataSets.get(climatologyDataSetIndex), 
                    dataSetFileInfo[OneOf.AIBoldTitle], 
                    tActiveGridDataSetOptions.get(gridDataSetIndex).endsWith("*")? "*" : "", //optionAdd
                    dataSetFileInfo[OneOf.AIPaletteMax]);

            } else {
                //other datasets types

                //get the otherInfo
                String dsoi = dataSetRB2.getString(sixName + "Info",  null);
                Test.ensureNotNull(dsoi, errorInMethod + sixName + "Info is null.");
                String dataSetOtherInfo[] = String2.split(dsoi, '`');

                //get the dataSetFileInfo
                if (oneOf.verbose()) 
                    String2.log(
                        oneOf.shortClassName() + " shared.tryToLoad internalName=" + internalName + "\n" +
                        "  fileInfo: " + String2.toCSSVString(dataSetFileInfo) + "\n" +
                        "  otherInfo: " + String2.toCSSVString(dataSetOtherInfo) //+ "\n" +
                        //"  fgdc: " + (tFGDC == null? tFGDC : XNL.encodeAsTerminal(tFGDC))
                        );

                //handle local climatology dataset
                if (firstChar == 'C') {
                    tGridDataSet = new GridDataSetCWLocalClimatology(oneOf.fileNameUtility(), 
                         internalName, oneOf.localDataSetBaseDirectory(), 
                        oneOf.fullPrivateDirectory());
                    grdDirTime        += ((GridDataSetCWLocalClimatology)tGridDataSet).grdDirTime;
                    localGrdFileCount += ((GridDataSetCWLocalClimatology)tGridDataSet).localGrdFileCount;

                //handle local dataset
                } else if (firstChar == 'L') {
                    tGridDataSet = new GridDataSetCWLocal(oneOf.fileNameUtility(), 
                        internalName, oneOf.localDataSetBaseDirectory(), 
                        oneOf.fullPrivateDirectory());
                    grdDirTime        += ((GridDataSetCWLocal)tGridDataSet).grdDirTime;
                    localGrdFileCount += ((GridDataSetCWLocal)tGridDataSet).localGrdFileCount;

                //handle Pathfinder dataSet 
                /* } else if (firstChar == 'P') {
                    Test.ensureEqual(dataSetFileInfo.length, ?, 
                        errorInMethod + internalName);
                    tGridDataSet = new GridDataSetPathfinder(
                        internalName, dataSetOtherInfo, tFGDC, verbose);   
                */

                //else unknown
                } else throw new RuntimeException(errorInMethod + "unknown dataset type: " + firstChar + "." );

                //ensure other parts are valid
                Test.ensureNotEqual(
                    String2.indexOf(paletteOptions, tGridDataSet.palette), 
                    -1,
                    tGridDataSet.palette + " is not a valid palette. " + internalName);
                Test.ensureNotEqual(
                    String2.indexOf(paletteScaleOptions, tGridDataSet.paletteScale), 
                    -1,
                    tGridDataSet.paletteScale + " is not a valid paletteScale. " + internalName);
            }

            //if there were activeTimePeriods, save 
            //later usage: activeGridDatasets.get(i) 
            //  -> a dataSet name and a list of its activeTimePeriods 
            if (tGridDataSet.activeTimePeriodOptions.length > 0) {
                //make all the time option strings canonical 
                tGridDataSet.makeTimeOptionsCanonical();

                //save this gridDataSet
                tActiveGridDataSetOptions.add(tGridDataSet.option);
                tActiveGridDataSet7Names.add(tGridDataSet.internalName);
                activeGridDataSets.add(tGridDataSet);
                return true;
            } else return false;
        } catch (Exception e) {
            String2.log("about to display Shared.tryToLoad exception for " + internalName);
            String2.log(MustBe.throwable(errorInMethod, e));
            return false;
        }
    }

    /** The error message from run() (or "" if no error). */
    public String runError() {return runError;}

    /** The time it took to run Shared.run. */
    public long resetTime() {return resetTime;}

    /** The options strings for the grid data sets. */
    public String[] activeGridDataSetOptions() {
        if (activeGridDataSetOptions == null) 
            activeGridDataSetOptions = new String[]{OneOf.NO_DATA};
        return activeGridDataSetOptions;
    }

    /** The 7char names for the grid data sets. */
    public String[] activeGridDataSet7Names() {
        if (activeGridDataSet7Names == null)
            activeGridDataSet7Names = new String[]{"L!!none"};
        return activeGridDataSet7Names;
    }

    /** The GridDataset for the grid data sets. */
    public Vector activeGridDataSets() {
        if (activeGridDataSets == null) {
            activeGridDataSets = new Vector();
            activeGridDataSets.add(null);
        }
        return activeGridDataSets;
    }

    /** The options strings for the active (i.e., available) vector data sets. */
    public String[] activeVectorOptions() {
        if (activeVectorOptions == null) 
            activeVectorOptions = new String[]{OneOf.NO_DATA};
        return activeVectorOptions;
    }

    /** The 7 char internal name strings for the active (i.e., available) vector data sets. */
    public String[] activeVector7Names() {
        if (activeVector7Names == null)
            activeVector7Names = new String[]{"VNone"};
        return activeVector7Names;
    }

    /** The indexes for the X grid data sets for each of the active (i.e., available) vector data sets. */
    public int[] activeVectorXDataSetIndexes() {
        if (activeVectorXDataSetIndexes == null) 
            activeVectorXDataSetIndexes = new int[]{-1};
        return activeVectorXDataSetIndexes;
    }

    /** The indexes for the Y grid data sets for each of the active (i.e., available) vector data sets. */
    public int[] activeVectorYDataSetIndexes() {
        if (activeVectorYDataSetIndexes == null) 
            activeVectorYDataSetIndexes = new int[]{-1};
        return activeVectorYDataSetIndexes;
    }

    /** The vector with information for each of the active (i.e., available) vector data sets. */
    public Vector activeVectorContents() {
        if (activeVectorContents == null) {
            activeVectorContents = new Vector();
            activeVectorContents.add(new Object[]{
                new String[]{},   //String2.toStringArray(tActiveVectorTimePeriodOptions), 
                new String[]{""}, //String2.toStringArray(tActiveVectorTimePeriodTitles), 0=main title
                null});
        }
        return activeVectorContents;
    }

    /** The options strings for the point data sets. */
    public String[] activePointDataSetOptions() {
        if (activePointDataSetOptions == null) 
            activePointDataSetOptions = new String[]{OneOf.NO_DATA};
        return activePointDataSetOptions;
    }

    /** The 7 character internal name strings for the point data sets. */
    public String[] activePointDataSet7Names() {
        if (activePointDataSet7Names == null)
            activePointDataSet7Names = new String[]{"VNone"};
        return activePointDataSet7Names;
     }

    /** The GridDataset for the point data sets. */
    public Vector activePointDataSets() {
        if (activePointDataSets == null) {
            activePointDataSets = new Vector();
            activePointDataSets.add(new PointDataSetNone());
        }
        return activePointDataSets;
    }

    /** The options strings for the active (i.e., available) point vector data sets. */
    public String[] activePointVectorOptions() {
        if (activePointVectorOptions == null) 
            activePointVectorOptions = new String[]{OneOf.NO_DATA};
        return activePointVectorOptions;
    }

    /** The 7 character internal name strings for the active (i.e., available) point vector data sets. */
    public String[] activePointVector7Names() {
        if (activePointVector7Names == null) 
            activePointVector7Names = new String[]{"PVNone"};
        return activePointVector7Names;
    }

    /** The indexes for the X grid data sets for each of the active (i.e., available) point vector data sets. */
    public int[] activePointVectorXDataSetIndexes() {
        if (activePointVectorXDataSetIndexes == null) 
            activePointVectorXDataSetIndexes = new int[]{-1};
        return activePointVectorXDataSetIndexes;
    }

    /** The indexes for the Y grid data sets for each of the active (i.e., available) point vector data sets. */
    public int[] activePointVectorYDataSetIndexes() {
        if (activePointVectorYDataSetIndexes == null) 
            activePointVectorYDataSetIndexes = new int[]{-1};
        return activePointVectorYDataSetIndexes;
    }

    /** The original PointVector index the specified activePointVector. */
    public int activePointVectorOriginalIndex(int whichActivePointVector) {
        return activePointVectorOriginalIndex.get(whichActivePointVector);}

    /** The options strings for the trajectory data sets. */
    public String[] activeTrajectoryDataSetOptions() {
        if (activeTrajectoryDataSetOptions == null) 
            activeTrajectoryDataSetOptions = new String[]{OneOf.NO_DATA};
        return activeTrajectoryDataSetOptions;
    }

    /** The 7 character internal name strings for the trajectory data sets. */
    public String[] activeTrajectoryDataSet7Names() {
        return activeTrajectoryDataSet7Names;
    }

    /** The TableDataset for the trajectory data sets. */
    public Vector activeTrajectoryDataSets() {
        return activeTrajectoryDataSets;
    }

    /** 
     * Indicates if two grid datasets are similar (e.g., same palette, scale, units, 
     * min, max, contourLinesAt, altUnits, altMin, altMax, altContourLinesAt).
     * For example, two SST datasets are probably similar,
     * but SST and SST Anomaly aren't (different min and max).
     *
     * @param dataSetOption1 the option String for dataSet1
     * @param dataSetOption2 the option String for dataSet2
     * @return true if the dataSets are similar
     */
    public boolean dataSetsAreSimilar(String dataSetOption1, String dataSetOption2) {
        boolean similar = false;
        int dsIndex1 = String2.indexOf(activeGridDataSetOptions, dataSetOption1);
        int dsIndex2 = String2.indexOf(activeGridDataSetOptions, dataSetOption2);
        if (dsIndex1 > 0 && dsIndex2 > 0) { //'else' shouldn't happen, but might under extreme circumstances
            GridDataSet ds1 = (GridDataSet)activeGridDataSets.get(dsIndex1);
            GridDataSet ds2 = (GridDataSet)activeGridDataSets.get(dsIndex2);
            if (ds1 != null && ds2 != null) { //(e.g., null if bathymetry 
                similar = 
                    ds1.palette.equals(ds2.palette) &&
                    ds1.paletteScale.equals(ds2.paletteScale) &&
                    ds1.udUnits.equals(ds2.udUnits) &&
                    ds1.paletteMin.equals(ds2.paletteMin) &&
                    ds1.paletteMax.equals(ds2.paletteMax) &&
                    ds1.contourLinesAt.equals(ds2.contourLinesAt) &&
                    ds1.altUdUnits.equals(ds2.altUdUnits) &&
                    ds1.altPaletteMin.equals(ds2.altPaletteMin) &&
                    ds1.altPaletteMax.equals(ds2.altPaletteMax) &&
                    ds1.altContourLinesAt.equals(ds2.altContourLinesAt);
            }
        }
        if (oneOf.verbose())
            String2.log("Shared.dataSetsAreSimilar(" + 
                dataSetOption1 + ", " + dataSetOption2 + ") = " + similar);
        return similar;
    }
}
