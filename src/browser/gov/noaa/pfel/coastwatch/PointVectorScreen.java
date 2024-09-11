/* 
 * PointVectorScreen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;

import com.cohort.ema.*;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.pointdata.*;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.util.IntObject;

import java.awt.Color;
import java.util.GregorianCalendar;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for the point vector screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Station Vector").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-05-05
 */
public class PointVectorScreen extends Screen {

    public EmaSelect  dataSet, depth, timePeriod, timeSeries;
    public EmaDateTimeText2 centeredTime, beginTime;
    public EmaColor   color;

    //set by the constructor
    public String dataSetLabel, depthLabel, timePeriodLabel, centeredTimeLabel, beginTimeLabel, 
        getAverageLabel, getTSLabel, 
        colorLabel, timeSeriesLabel;

    //set by validate
    public String dataSetValue;
    public int absoluteDataSetIndex;  //if no vectors plotted, this will be 0
    public boolean plotData;

    public String depthValue;

    public String timePeriodValue;
    public int timePeriodNHours;
    public String centeredTimeValue;
    public String beginTimeValue;
    public GregorianCalendar startCalendar, endCalendar;

    public String colorValue;

    public String addToImageFileName;
    public GraphDataLayer vectorGraphDataLayer; //or null
    public PointDataSetFromStationVariables uPointDataSet;
    public PointDataSetFromStationVariables vPointDataSet;
    public String legendTime;
    public String legendCourtesy;
    public boolean dataAccessAllowed;
    public String averageFileName;
    public String fullAverageFileName;

    public int unitsIndex;
    public int timeSeriesIndex;
    public String timeSeriesValue;
    public String timeSeriesFileName; //may be null
    public String timeSeriesID;
    public double timeSeriesLat, timeSeriesLon;
    public String[] timeSeriesOptions;

    /** 
     * The constructor for VectorScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public PointVectorScreen(int editOption, OneOf oneOf, Shared shared, EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in PointVectorScreen(): ";

        //add the components to the emaClass
        emaClass.addAttribute(dataSet      = new EmaSelect(emaClass, "pointVectorDataSet"));
        emaClass.addAttribute(depth        = new EmaSelect(emaClass, "pointVectorDepth")); 
        emaClass.addAttribute(timePeriod   = new EmaSelect(emaClass, "pointVectorTimePeriod"));
        emaClass.addAttribute(centeredTime = new EmaDateTimeText2(emaClass, "pointVectorCenteredTime"));
        emaClass.addAttribute(beginTime    = new EmaDateTimeText2(emaClass, "pointVectorBeginTime"));
        emaClass.addAttribute(color        = new EmaColor( emaClass, "pointVectorColor"));
        emaClass.addAttribute(timeSeries   = new EmaSelect(emaClass, "pointVectorTimeSeries"));

        //timePeriod options are the same for all pointDataSets
        timePeriod.setOptions(PointDataSet.timePeriodOptions);
        timePeriod.setTitles( PointDataSet.timePeriodTitles);

        //get all the labels 
        dataSetLabel      = classRB2.getNotNullString("pointVectorDataSet.label", errorInMethod);
        depthLabel        = classRB2.getNotNullString("pointVectorDepth.label", errorInMethod);
        timePeriodLabel   = classRB2.getNotNullString("pointVectorTimePeriod.label", errorInMethod);
        beginTimeLabel    = classRB2.getNotNullString("pointVectorBeginTime.label", errorInMethod);
        centeredTimeLabel = classRB2.getNotNullString("pointVectorCenteredTime.label", errorInMethod);
        colorLabel        = classRB2.getNotNullString("pointVectorColor.label", errorInMethod);
        timeSeriesLabel   = classRB2.getNotNullString("pointVectorTimeSeries.label", errorInMethod);
        getAverageLabel   = classRB2.getNotNullString("pointVectorGetAvg.label", errorInMethod);
        getTSLabel        = classRB2.getNotNullString("pointVectorGetTS.label", errorInMethod);

    }

    /**
     * This validates the user's choices for this screen, and, if htmlSB isn't null,
     * adds the html code for the screen to htmlSB.
     *
     * @param session the user's session
     * @param showEditOption  &gt;=0 for User.EDIT_XXX screens 
     *    or &lt;0 for responses to getXxx buttons 
     * @param step the next step number on the form (e.g., 1), 2), 3), ...).
     * @param rowNumber the number of the next row in the html form.
     * @param htmlSB the StringBuilder collecting the html to be sent to the users.
     * @param currentMinX the minimum longitude of the current region
     * @param currentMaxX the maximum longitude of the current region
     * @param currentMinY the minimum latitude of the current region
     * @param currentMaxY the maximum latitude of the current region
     * @param cWESNString the WESN part of a custom file name
     * @param gifSizeIndex
     */
    public void validate(HttpSession session, int showEditOption, IntObject step, 
            IntObject rowNumber, StringBuilder htmlSB, 
            double currentMinX, double currentMaxX, double currentMinY, double currentMaxY, 
            String cWESNString, int gifSizeIndex) throws Exception {

        //use the latest info
        dataSet.setOptions(shared.activePointVectorOptions()); 

        dataSetValue = dataSet.getValue(session);
        int whichVector = String2.indexOf(shared.activePointVectorOptions(), dataSetValue);
        if (whichVector < 0) {
            if (whichVector < 0)  //check for 7char name
                whichVector = String2.indexOf(shared.activePointVector7Names(), dataSetValue);
            if (whichVector < 0)
                whichVector = 0; //none
            dataSetValue = shared.activePointVectorOptions()[whichVector];
            dataSet.setValue(session, dataSetValue);
        }
        if (oneOf.verbose()) String2.log("pointVectorScreen dataSetValue = " + dataSetValue);
        absoluteDataSetIndex = String2.indexOf(oneOf.pointVectorOptions(), dataSetValue); //this 'absolute' is okay
        String vectorInternalName = oneOf.pointVectorInfo()[absoluteDataSetIndex][OneOf.PVIInternalName]; //e.g., PVPNBwsp
        plotData = whichVector > 0;
        if (doTally)
            oneOf.tally().add("Point Vector Data Set Usage:", dataSetValue);
        if (showEditOption == editOption) 
            addTableEntry(dataSet, dataSetLabel, dataSetValue, rowNumber, step, htmlSB);

        depthValue = depth.getValue(session);
        double depthDouble = Double.NaN;
        unitsIndex = 0; //constant for now
        colorValue = null;
        timePeriodValue = null;
        String timePeriodInFileName = null;
        timePeriodNHours = -1;
        int tExactTimePeriod = -1;
        beginTimeValue = null;
        centeredTimeValue = null;
        startCalendar = null;
        endCalendar = null;
        uPointDataSet = null;
        vPointDataSet = null;
        legendTime = "";
        legendCourtesy = "";
        dataAccessAllowed = false;
        averageFileName = null;
        fullAverageFileName = null;
        Table averageTable = null;
        addToImageFileName = "";  //append stuff, then hash at end
        if (plotData) { 
            int dataSetIndex = dataSet.indexOf(dataSetValue);
            uPointDataSet = (PointDataSetFromStationVariables)shared.activePointDataSets().get(shared.activePointVectorXDataSetIndexes()[dataSetIndex]);
            vPointDataSet = (PointDataSetFromStationVariables)shared.activePointDataSets().get(shared.activePointVectorYDataSetIndexes()[dataSetIndex]);

            //depth
            depthValue = depth.setOptionsAndValidate(uPointDataSet.depthLevels(), session, 0);
            depthDouble = String2.parseDouble(depthValue);
            if (oneOf.verbose()) String2.log("  depthValue = " + depthValue);
            if (showEditOption == editOption) {
                depth.setLabel(String2.substitute(depthLabel, "" + (step.i++), null, null));
                htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
                    "      <td>" + depth.getLabel() + "</td>\n" +
                    "      <td>" + depth.getControl(depthValue) + " meters</td>\n" +
                    "    " + emaClass.getEndRow() + "\n");
            }

            //timePeriod
            int timePeriodIndex = -1;
            timePeriodIndex = PointDataSet.closestTimePeriod(timePeriod.getValue(session));
            timePeriodValue = PointDataSet.timePeriodOptions[timePeriodIndex];
            timePeriod.setValue(session, timePeriodValue);
            if (oneOf.verbose()) String2.log("  timePeriodValue = " + timePeriodValue);
            if (showEditOption == editOption)
                addTableEntry(timePeriod, timePeriodLabel, timePeriodValue, rowNumber, step, htmlSB);
            timePeriodInFileName = TimePeriods.getInFileName(timePeriodValue);
            timePeriodNHours = TimePeriods.getNHours(timePeriodValue);
            tExactTimePeriod = TimePeriods.exactTimePeriod(timePeriodValue);

            //centeredTime
            GregorianCalendar tFirstTime = uPointDataSet.firstTime.after(vPointDataSet.firstTime)?
                uPointDataSet.firstTime : vPointDataSet.firstTime;
            GregorianCalendar tLastTime = uPointDataSet.lastTime.after(vPointDataSet.lastTime)?
                uPointDataSet.lastTime : vPointDataSet.lastTime;
            centeredTime.setMinMaxDateTime(tFirstTime, tLastTime);
            //setShow before findClosestTime since it affects formatting (used by findClosestTime)
            centeredTime.setShow(TimePeriods.PICK_FROM[tExactTimePeriod]);
            centeredTime.setIncrement(TimePeriods.INCREMENT[tExactTimePeriod]);
            centeredTimeValue = centeredTime.findClosestDate(centeredTime.getValue(session)); //ensure in range, not null
            centeredTimeValue = TimePeriods.getCleanCenteredTime(timePeriodValue, centeredTimeValue); //clean it
            centeredTimeValue = centeredTime.findClosestDate(centeredTimeValue); //ensure in range
            centeredTime.setValue(session, centeredTimeValue);
            if (oneOf.verbose()) String2.log("  centeredTimeValue = " + centeredTimeValue);
            if (showEditOption == editOption)
                addTableEntry(centeredTime, centeredTimeLabel, centeredTimeValue, rowNumber, step, htmlSB);
            startCalendar = TimePeriods.getStartCalendar(timePeriodValue, centeredTimeValue, "");
            endCalendar   = TimePeriods.getEndCalendar(timePeriodValue, centeredTimeValue, "");
            dataAccessAllowed = 
                OneOf.dataAccessAllowed(
                    uPointDataSet.daysTillDataAccessAllowed, Calendar2.formatAsISODateTimeT(endCalendar)) &&
                OneOf.dataAccessAllowed(
                    vPointDataSet.daysTillDataAccessAllowed, Calendar2.formatAsISODateTimeT(endCalendar));

            //color
            colorValue = color.getValue(session);
            if (color.isValid(colorValue).length() > 0) {
                colorValue = color.getDefaultValue();
                color.setValue(session, colorValue);
            }
            if (showEditOption == editOption) 
                addTableEntry(color, colorLabel, colorValue, rowNumber, step, htmlSB);
            if (oneOf.verbose()) String2.log("  colorValue = " + colorValue);

            //make legend Strings
            legendTime = TimePeriods.getLegendTime(timePeriodValue, centeredTimeValue);
            legendCourtesy = uPointDataSet.courtesy;

            //make fileNames
            averageFileName = FileNameUtility.makeAveragedPointTimeSeriesName(
                vectorInternalName, 
                'S', //S=always standard units
                currentMinX, currentMaxX, currentMinY, currentMaxY, 
                String2.parseDouble(depthValue), 
                String2.parseDouble(depthValue),
                centeredTimeValue, centeredTimeValue,
                timePeriodValue);
            fullAverageFileName = oneOf.fullPrivateDirectory() + averageFileName + ".nc";

            //make first part of addToImageFileName  //full info for now, hashed below
            addToImageFileName = averageFileName + colorValue;

            //reuse or generate file: fullAverageFileName
            //Note average is "component mean".
            //It is calculated from uMean and vMean (not from mean of vector lengths).
            //This matches method used to plot REGRESS_MEAN on the graph (in SgtGraph).
            //And this matches how NDBC calculates the "average" for a given hour
            // (see https://www.ndbc.noaa.gov/measdes.shtml#stdmet).
            if (File2.touch(fullAverageFileName)) {
                //read the table from an existing file
                //unresolved problem with relying on existing file:
                //  file for composite of current month would change hourly as new data arrives
                //  but with re-use of file, new data won't be added.
                if (oneOf.verbose()) String2.log("  averageTable reusing " + 
                    fullAverageFileName);
                averageTable = new Table();
                averageTable.readFlatNc(fullAverageFileName, null, 1); //standardizeWhat=1
            } else {
                long tTime = System.currentTimeMillis();
                if (oneOf.verbose()) String2.log("  averageTable creating " + 
                    fullAverageFileName);

                averageTable = PointVectors.makeAveragedTimeSeries(
                    uPointDataSet, vPointDataSet, 
                    currentMinX, currentMaxX, currentMinY, currentMaxY, 
                    depthDouble, depthDouble, centeredTimeValue, centeredTimeValue, 
                    timePeriodValue);

                if (averageTable.nRows() == 0) {
                    plotData = false;
                    fullAverageFileName = null;
                    averageFileName =  null;
                    addToImageFileName = "";
                } else {
                    averageTable.saveAsFlatNc(fullAverageFileName, "row"); 
                }
            } 

            //display the 'getAvg' options 
            if (showEditOption == editOption) {
                PointScreen.showGetPointDataLinks(
                    "stationVectorData", oneOf.pointGetAvgOptions(), oneOf.pointGetAvgTitles(),
                    oneOf, session, htmlSB, rowNumber, step,
                    getAverageLabel, 
                    plotData && averageTable.nRows() > 0, //plotData was set to false if averageTable.nRows()==0
                    dataAccessAllowed,
                    emaClass, vectorInternalName, 
                    currentMinX, currentMaxX, currentMinY, currentMaxY,
                    depthValue, depthValue,
                    centeredTimeValue, centeredTimeValue, timePeriodValue);
            }               
        }

        //timeSeries
        timeSeriesIndex = -1;
        timeSeriesValue = null;
        timeSeriesFileName = null;
        timeSeriesOptions = null;
        timeSeriesLon = Double.NaN;
        timeSeriesLat = Double.NaN;
        timeSeriesID  = null;
        //note that plotData may have been true, but set to false in section above
        if (plotData) {
            StringArray timeSeriesOptionsSA = new StringArray();
            timeSeriesOptionsSA.add("");
            //if (oneOf.verbose()) String2.log("averageTable right before make timeSeriesOptions: " + averageTable.toString(100));
            //averageTable has 7 columns (x,y,z,t,id,uData,vData).
            int n = averageTable.nRows();
            for (int i = 0; i < n; i++) {
                timeSeriesOptionsSA.add(
                    "Station " + averageTable.getStringData(4, i) + " (" + 
                    String2.genEFormat10(averageTable.getDoubleData(1, i)) + " N, " + 
                    String2.genEFormat10(averageTable.getDoubleData(0, i)) + " E)");
            }
            //averaged table is sorted by ID
            //don't sort here, because connection to averageTable used below

            timeSeriesOptions = timeSeriesOptionsSA.toArray();
            //String2.log("  timeSeriesOptions=" + String2.toCSSVString(timeSeriesOptions));
            timeSeriesValue = timeSeries.setOptionsAndValidate(timeSeriesOptions, session, 0);
            timeSeriesIndex = timeSeries.indexOf(timeSeriesValue);
            if (doTally)
                oneOf.tally().add("Point Vector Time Series Usage:", timeSeriesValue);
            if (oneOf.verbose()) String2.log("  timeSeriesValue = " + timeSeriesValue);

            //do the timeSeries GUI stuff
            if (showEditOption == editOption) {
                timeSeries.setLabel(String2.substitute(timeSeriesLabel, "" + (step.i++), null, null));
                if (timeSeriesOptions.length > 1) //there's always a 0 option: ""
                    htmlSB.append(
                        "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
                        "      <td>" + timeSeries.getLabel() + "</td>\n" +
                        "      <td>of 'Time Period' averages, for\n        " + 
                        timeSeries.getControl(timeSeriesValue) + "\n" +
                        "        Or, click on the base of a station vector on the map.</td>\n" +
                        "    " + emaClass.getEndRow() + "\n");
                else htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
                    "      <td>" + timeSeries.getLabel() + "</td>\n" +
                    "      <td>" + oneOf.noDataAvailable() + "</td>\n" +
                    "    " + emaClass.getEndRow() + "\n");
            }

            //make the graph GraphDataLayer
            if (timeSeriesIndex > 0) {
                timeSeriesLon = averageTable.getDoubleData(0, timeSeriesIndex - 1); //-1 because of 0=none option
                timeSeriesLat = averageTable.getDoubleData(1, timeSeriesIndex - 1); //-1 because of 0=none option
                timeSeriesID  = averageTable.getStringData(4, timeSeriesIndex - 1); //-1 because of 0=none option

                //beginTime
                double tMinTime = uPointDataSet.getStationMinTime(timeSeriesID);
                double tMaxTime = Calendar2.isoStringToEpochSeconds(centeredTimeValue); //throws exception if trouble
                if (Double.isNaN(tMinTime)) tMinTime = tMaxTime;
                else tMinTime = Math.min(tMinTime, tMaxTime);
                beginTime.setMinMaxDateTime(
                    Calendar2.epochSecondsToGc(tMinTime),
                    Calendar2.epochSecondsToGc(tMaxTime));
                //setShow before findClosestTime since it affects formatting (used by findClosestTime)
                beginTime.setShow(TimePeriods.PICK_FROM[tExactTimePeriod]);
                beginTime.setIncrement(TimePeriods.INCREMENT[tExactTimePeriod]);
                beginTimeValue = beginTime.findClosestDate(beginTime.getValue(session)); //ensure in range, not null
                beginTimeValue = TimePeriods.validateBeginTime(beginTimeValue, //adjust relative to centeredTime
                    centeredTimeValue, timePeriodValue);
                beginTimeValue = beginTime.findClosestDate(beginTimeValue); //ensure in range, not null
                beginTime.setValue(session, beginTimeValue);
                if (oneOf.verbose()) String2.log("  beginTimeValue = " + beginTimeValue);
                if (showEditOption == editOption)
                    addTableEntry(beginTime, beginTimeLabel, beginTimeValue, rowNumber, step, htmlSB);

                //make timeSeriesFileName 
                timeSeriesFileName = FileNameUtility.makeAveragedPointTimeSeriesName(
                    vectorInternalName, 
                    'S', //S=always standard units
                    timeSeriesLon, timeSeriesLon, timeSeriesLat, timeSeriesLat, 
                    String2.parseDouble(depthValue), 
                    String2.parseDouble(depthValue),
                    beginTimeValue, centeredTimeValue,
                    timePeriodValue);
                String fullTimeSeriesFileName = oneOf.fullPrivateDirectory() + timeSeriesFileName + ".nc";
         
                addToImageFileName += timeSeriesFileName; //will be hashCoded below

                //make the table of timeSeries values and store in datafile
                Table timeSeriesTable;
                if (File2.touch(fullTimeSeriesFileName)) {
                    if (oneOf.verbose()) String2.log("  timeSeries reusing " + timeSeriesFileName + ".nc");
                    timeSeriesTable = new Table();
                    timeSeriesTable.readFlatNc(fullTimeSeriesFileName, null, 1); //standardizeWhat=1
                } else {
                    //unresolved problem with relying on existing file:
                    //  file for composite of current month would change hourly as new data arrives
                    //  but with re-use of file, new data won't be added.
                    //table has 7 columns (x,y,z,t,id,uData,vData).
                    if (oneOf.verbose()) String2.log("  timeSeries creating " + timeSeriesFileName + ".nc");
                    timeSeriesTable = PointVectors.makeAveragedTimeSeries(
                        uPointDataSet, vPointDataSet, 
                        timeSeriesLon, timeSeriesLon,  
                        timeSeriesLat, timeSeriesLat, 
                        depthDouble, depthDouble, beginTimeValue, centeredTimeValue, 
                        timePeriodValue);

                    //save as .nc file   
                    timeSeriesTable.saveAsFlatNc(fullTimeSeriesFileName, "time");
                    //String2.log("  timeSeries table=" + timeSeriesTable.toString(20));
                } 

                //graph GraphDataLayer was created here
                 
                //display the 'getTS' options 
                if (showEditOption == editOption) {
                    PointScreen.showGetPointDataLinks(
                        "stationVectorData", oneOf.pointGetTSOptions(), oneOf.pointGetTSTitles(),
                        oneOf, session, htmlSB, rowNumber, step,
                        getTSLabel, timeSeriesTable.nRows() > 0, dataAccessAllowed,
                        emaClass, vectorInternalName, 
                        timeSeriesLon, timeSeriesLon, 
                        timeSeriesLat, timeSeriesLat,
                        depthValue, depthValue,
                        beginTimeValue, centeredTimeValue, timePeriodValue);
                }               
            }        
        }

        //finish up addToImageFileName
        if (plotData) {
            addToImageFileName = "_PV" +  
                String2.md5Hex12(addToImageFileName); //will be same for same info
        } else addToImageFileName = "";

    }

    /**
     * This returns the graph (time series) GraphDataLayer (or null).
     * This is made as needed so garbage collected after used.
     *
     * @return the time series GraphDataLayer or null if trouble (this won't throw an exception)
     */
    public GraphDataLayer getGraphGDL() {
        if (timeSeriesFileName == null)
            return null;

        try {
            Table timeSeriesTable = new Table();
            String fullTimeSeriesFileName = oneOf.fullPrivateDirectory() + timeSeriesFileName + ".nc";
            timeSeriesTable.readFlatNc(fullTimeSeriesFileName, null, 1); //standardizeWhat=1

            //set up the graphGDL 
            //(with data for all visible stations, averaged over time)
            String tUnits = uPointDataSet.unitsOptions[unitsIndex]; //not pointVector's units, which is std vector length (e.g., 10 m/s)

            return new GraphDataLayer(
                editOption, //sourceID
                3, 5, 6, -1, -1, //time,u,v  others not used
                GraphDataLayer.DRAW_STICKS, true, false,
                "Time", tUnits, //xAxisTitle
                oneOf.pointVectorInfo()[absoluteDataSetIndex][OneOf.PVIBoldTitle] + 
                    " Station " + timeSeriesID + ", " + 
                    String2.genEFormat10(timeSeriesLat) + " N, " + 
                    String2.genEFormat10(timeSeriesLon) + " E",
                "Time series of " + 
                    (timePeriodNHours == 0? "raw data. " :
                        timePeriodValue + " averages. ") + 
                    "Depth = " + depthValue + " meters. " + //no legendTime, 
                    "Stick length=speed, angle=direction.", //no room: (Horizontal line = average)", //title2                        
                uPointDataSet.courtesy.length() == 0? null : "Data courtesy of " + uPointDataSet.courtesy, //Station vector data
                null, //title4
                timeSeriesTable, null, null,
                null, //colorMap
                new Color(String2.parseInt("0x" + colorValue)),
                GraphDataLayer.MARKER_TYPE_NONE, 0,
                0, //standardMarkerSize
                GraphDataLayer.REGRESS_MEAN);
        } catch (Exception e) {
            String error = "Exception caught in PointVectorScreen.getGraphGDL:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), error);

            return null;            
        }
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a Get button (e.g., getAvgAsc).
     *
     * @param submitter
     * @return false 
     */
    public boolean submitterIsAGetButton(String submitter) {
        return false;        
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a getAvg button (e.g., getAvgAsc).
     *
     * @param submitter
     * @return false
     */
    public boolean submitterIsAGetAvgButton(String submitter) {
        return false;
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a getTS button (e.g., getTSAsc).
     *
     * @param submitter
     * @return false
     */
    public boolean submitterIsAGetTSButton(String submitter) {
        return false;
    }

    /**
     * This responds to a submitter button, preparing the file and the 
     * html.
     *
     * @param submitter the name of the submitter button
     * @param htmlSB the StringBuilder which is collecting the html
     * @param backButtonForm the html for a form with just a back button
     * @param throws Exception if trouble
     */
    public void respondToSubmitter(String submitter, StringBuilder htmlSB, 
            String backButtonForm) throws Exception {

        //failure
        Test.error(String2.ERROR + ": Unexpected submitter: " + submitter);
    }



}
