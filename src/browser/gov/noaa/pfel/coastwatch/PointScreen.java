/* 
 * PointScreen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.IntArray;
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
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.griddata.GridDataSet;
import gov.noaa.pfel.coastwatch.pointdata.*;
import gov.noaa.pfel.coastwatch.sgt.CompoundColorMap;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.util.IntObject;

import java.awt.Color;
import java.util.GregorianCalendar;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for a Point screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Point Data").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class PointScreen extends Screen {

    public boolean verbose = true;
    int whichPointScreen1; //1..OneOf.nPointScreens()

    public EmaSelect  dataSet, depth, units, timePeriod, palette, paletteScale, timeSeries;
    //public EmaDateTimeSelect date;
    public EmaDateTimeText2 beginTime, centeredTime;
    public EmaColor color;
    public EmaDouble paletteMin, paletteMax;

    //set by the constructor
    public String dataSetLabel, depthLabel, unitsLabel, timePeriodLabel, beginTimeLabel, centeredTimeLabel, 
        paletteLabel, colorLabel, timeSeriesLabel, getAvgLabel, getTSLabel;

    //set by validate
    public boolean plotData;
    public String dataSetValue;
    public int dataSetIndex;
    public PointDataSetFromStationVariables pointDataSet;

    public String depthValue;

    public int timePeriodIndex;
    public String timePeriodValue;
    public int timePeriodNHours;

    public String beginTimeValue, centeredTimeValue;
    public GregorianCalendar startCalendar, endCalendar;

    public int unitsIndex;
    public String unitsValue;

    public String paletteValue;
    public String paletteScaleValue;
    public String paletteMinValue;
    public String paletteMaxValue;
    public String fullDataSetCptName;
    public String paletteID;

    public int timeSeriesIndex;
    public String timeSeriesValue;
    public String timeSeriesOptions[];
    public String timeSeriesFileName; //no directory at start or .nc at end; may be null
    public String timeSeriesID;
    public double timeSeriesLat, timeSeriesLon;

    public String colorValue;

    public String addToImageFileName;
    public String averageFileName; //doesn't have directory at start or .nc at end; may be null
    public String legendTime;
    public boolean dataAccessAllowed;



    /** 
     * The constructor for PointScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param whichPointScreen1 1..OneOf.nPointScreens()
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public PointScreen(int editOption, int whichPointScreen1, OneOf oneOf, Shared shared, 
            EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.whichPointScreen1 = whichPointScreen1;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in PointScreen" + whichPointScreen1 + ": ";
        verbose = oneOf.verbose();
        if (verbose) String2.log("create PointScreen" + whichPointScreen1);

        //add the components to the emaClass
        emaClass.addAttribute(dataSet         = new EmaSelect(emaClass, "pointDataSet" + whichPointScreen1)); 
        emaClass.addAttribute(depth           = new EmaSelect(emaClass, "pointDepth" + whichPointScreen1)); 
        emaClass.addAttribute(units           = new EmaSelect(emaClass, "pointUnits" + whichPointScreen1));
        emaClass.addAttribute(timePeriod      = new EmaSelect(emaClass, "pointTimePeriod" + whichPointScreen1));
        emaClass.addAttribute(beginTime       = new EmaDateTimeText2(emaClass, "pointBeginTime" + whichPointScreen1));
        emaClass.addAttribute(centeredTime    = new EmaDateTimeText2(emaClass, "pointCenteredTime" + whichPointScreen1));
        emaClass.addAttribute(paletteMin      = new EmaDouble(emaClass, "pointPaletteMin" + whichPointScreen1));
        emaClass.addAttribute(paletteMax      = new EmaDouble(emaClass, "pointPaletteMax" + whichPointScreen1));
        emaClass.addAttribute(palette         = new EmaSelect(emaClass, "pointPalette" + whichPointScreen1));
        emaClass.addAttribute(paletteScale    = new EmaSelect(emaClass, "pointPaletteScale" + whichPointScreen1));
        emaClass.addAttribute(timeSeries      = new EmaSelect(emaClass, "pointTimeSeries" + whichPointScreen1));
        emaClass.addAttribute(color           = new EmaColor( emaClass, "pointColor" + whichPointScreen1));

        //timePeriod options are the same for all pointDataSets
        timePeriod.setOptions(PointDataSet.timePeriodOptions);
        timePeriod.setTitles(PointDataSet.timePeriodTitles);

        //get all the labels 
        dataSetLabel      = classRB2.getNotNullString("pointDataSet" + whichPointScreen1 + ".label",  errorInMethod);
        depthLabel        = classRB2.getNotNullString("pointDepth" + whichPointScreen1 + ".label", errorInMethod);
        unitsLabel        = classRB2.getNotNullString("pointUnits" + whichPointScreen1 + ".label", errorInMethod);
        timePeriodLabel   = classRB2.getNotNullString("pointTimePeriod" + whichPointScreen1 + ".label", errorInMethod);
        beginTimeLabel    = classRB2.getNotNullString("pointBeginTime" + whichPointScreen1 + ".label", errorInMethod);
        centeredTimeLabel = classRB2.getNotNullString("pointCenteredTime" + whichPointScreen1 + ".label", errorInMethod);
        paletteLabel      = classRB2.getNotNullString("pointPalette" + whichPointScreen1 + ".label", errorInMethod);
        colorLabel        = classRB2.getNotNullString("pointColor" + whichPointScreen1 + ".label", errorInMethod);
        timeSeriesLabel   = classRB2.getNotNullString("pointTimeSeries" + whichPointScreen1 + ".label", errorInMethod);
        getAvgLabel       = classRB2.getNotNullString("pointGetAvg" + whichPointScreen1 + ".label", errorInMethod);
        getTSLabel        = classRB2.getNotNullString("pointGetTS" + whichPointScreen1 + ".label", errorInMethod);

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
     */
    public void validate(HttpSession session, int showEditOption, IntObject step, 
            IntObject rowNumber, StringBuilder htmlSB, 
            double currentMinX, double currentMaxX, double currentMinY, double currentMaxY, 
            String cWESNString) throws Exception {

        boolean showThisScreen = showEditOption == editOption;
        if (verbose) String2.log("pointScreen" + whichPointScreen1 + ".validate showEditOption=" + showThisScreen);

        //use the latest info
        dataSet.setOptions(shared.activePointDataSetOptions()); 

        //dataSet
        dataSetValue = dataSet.getValue(session);
        dataSetIndex = String2.indexOf(shared.activePointDataSetOptions(), dataSetValue);
        if (dataSetIndex < 0) {
            if (dataSetIndex < 0)  //check for 7char name
                dataSetIndex = String2.indexOf(shared.activePointDataSet7Names(), dataSetValue);
            if (dataSetIndex < 0)
                dataSetIndex = 0; //none
            dataSetValue = shared.activePointDataSetOptions()[dataSetIndex];
            dataSet.setValue(session, dataSetValue);
        }
        if (verbose) String2.log("  dataSetValue = " + dataSetValue);
        plotData = dataSetIndex > 0; 
        if (doTally && plotData)   //if allow None, it appears once for each PointScreen
            oneOf.tally().add("Station Data Set Usage:", dataSetValue);
        if (showThisScreen) 
            addTableEntry(dataSet, dataSetLabel, dataSetValue, rowNumber, step, htmlSB);

        //depth
        depthValue = depth.getValue(session);
        double depthDouble = Double.NaN;
        pointDataSet = null;
        if (plotData) {
            pointDataSet = (PointDataSetFromStationVariables)shared.activePointDataSets().get(dataSetIndex);
            depthValue = depth.setOptionsAndValidate(pointDataSet.depthLevels(), session, 0);
            depthDouble = String2.parseDouble(depthValue);
            if (verbose) String2.log("  depthValue = " + depthValue);
            if (showThisScreen) {
                depth.setLabel(String2.substitute(depthLabel, "" + (step.i++), null, null));
                htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
                    "      <td>" + depth.getLabel() + "</td>\n" +
                    "      <td>" + depth.getControl(depthValue) + " meters</td>\n" +
                    "    " + emaClass.getEndRow() + "\n");
            }
        }

        //timePeriod
        timePeriodIndex = -1;
        timePeriodValue = null;
        String timePeriodInFileName = null;
        timePeriodNHours = -1;
        int tExactTimePeriod = -1;
        if (plotData) {
            timePeriodIndex = PointDataSet.closestTimePeriod(timePeriod.getValue(session));
            timePeriodValue = PointDataSet.timePeriodOptions[timePeriodIndex];
            timePeriod.setValue(session, timePeriodValue);
            timePeriodInFileName = TimePeriods.getInFileName(timePeriodValue);
            timePeriodNHours = TimePeriods.getNHours(timePeriodValue);
            tExactTimePeriod = TimePeriods.exactTimePeriod(timePeriodValue);
            if (verbose) String2.log("  timePeriodValue = " + timePeriodValue);
            if (showThisScreen) 
                addTableEntry(timePeriod, timePeriodLabel, timePeriodValue, rowNumber, step, htmlSB);
        }

        //centeredTime   end of timePeriod (for averages on map) and end of time series graph
        centeredTimeValue = null;
        dataAccessAllowed = false;
        startCalendar = null;
        endCalendar = null;
        if (plotData) {
            centeredTime.setMinMaxDateTime(pointDataSet.firstTime, pointDataSet.lastTime);
            
            //setShow before findClosestDate since it affects formatting (used by findClosestTime)
            centeredTime.setShow(TimePeriods.PICK_FROM[tExactTimePeriod]);
            centeredTime.setIncrement(TimePeriods.INCREMENT[tExactTimePeriod]);
            centeredTimeValue = centeredTime.findClosestDate(centeredTime.getValue(session)); //ensure in range, not null
            centeredTimeValue = TimePeriods.getCleanCenteredTime(timePeriodValue, centeredTimeValue); //clean it
            centeredTimeValue = centeredTime.findClosestDate(centeredTimeValue); //ensure in range, "" -> last value
            centeredTime.setValue(session, centeredTimeValue);
            if (verbose) String2.log("  centeredTimeValue = " + centeredTimeValue);
            if (showThisScreen) 
                addTableEntry(centeredTime, centeredTimeLabel, centeredTimeValue, rowNumber, step, htmlSB);

            //startCalendar (for creation of WARNING in cwuser) is affected by timePeriod and centeredTime.
            //beginTime below is for timeSeries only.
            startCalendar = TimePeriods.getStartCalendar(timePeriodValue, centeredTimeValue, "");
            endCalendar   = TimePeriods.getEndCalendar(timePeriodValue, centeredTimeValue, "");
            dataAccessAllowed = OneOf.dataAccessAllowed(
                pointDataSet.daysTillDataAccessAllowed, Calendar2.formatAsISODateTimeT(endCalendar));
        }

        //units
        unitsIndex   = -1;
        unitsValue   = null;
        if (plotData) {
            unitsValue = units.setOptionsAndValidate(pointDataSet.unitsOptions, 
                session, pointDataSet.defaultUnits == 'A'? 1 : 0);
            unitsIndex = units.indexOf(unitsValue);
            if (verbose) String2.log("  unitsValue = " + unitsValue);
            if (showThisScreen) 
                addTableEntry(units, unitsLabel, unitsValue, rowNumber, step, htmlSB);
        }

        //palette, paletteMin, paletteMax, paletteScale
        paletteValue       = palette.getValue(session);
        paletteScaleValue  = paletteScale.getValue(session);
        paletteMinValue    = paletteMin.getValue(session);
        paletteMaxValue    = paletteMax.getValue(session);
        fullDataSetCptName = null;
        paletteID          = null;
        if (plotData) {
            //ensure valid
            String defaultMin = "" + (unitsIndex <= 0? pointDataSet.paletteMin : pointDataSet.altPaletteMin); 
            String defaultMax = "" + (unitsIndex <= 0? pointDataSet.paletteMax : pointDataSet.altPaletteMax); 
            if (palette.isValid(paletteValue).length() > 0) paletteValue = pointDataSet.palette;
            if (paletteScale.isValid(paletteScaleValue).length() > 0) paletteScaleValue   = pointDataSet.paletteScale;
            if (paletteMin.isValid(paletteMinValue).length() > 0) paletteMinValue = defaultMin;
            if (paletteMax.isValid(paletteMaxValue).length() > 0) paletteMaxValue = defaultMax;
            if (Math2.almostEqual(9, String2.parseDouble(paletteMinValue), String2.parseDouble(paletteMaxValue))) {
                paletteMinValue = defaultMin;
                paletteMaxValue = defaultMax;
            }
            if (paletteScaleValue.equals("Log") &&
                Math.min(String2.parseDouble(paletteMinValue), String2.parseDouble(paletteMaxValue)) <= 0) 
                paletteScaleValue = "Linear";

            //ensure standard number formatting (to avoid excess map creation for minor difference)
            //and store in session
            palette.setValue(session, paletteValue);
            paletteScale.setValue(session, paletteScaleValue);
            paletteMin.setValue(session, paletteMinValue = "" + String2.parseDouble(paletteMinValue)); 
            paletteMax.setValue(session, paletteMaxValue = "" + String2.parseDouble(paletteMaxValue)); 
            if (verbose)String2.log( 
                "  paletteValue=" + paletteValue + " paletteScale=" + paletteScaleValue +
                 " paletteMin=" + paletteMinValue + " paletteMax=" + paletteMaxValue);

            if (showThisScreen) {
                palette.setLabel(String2.substitute(paletteLabel, "" + (step.i++), null, null));
                htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" +
                    "      <td>" + palette.getLabel() + "</td>\n" +
                    "      <td>\n" +
                    "        <table class=\"erd\">\n" + //padding=0
                    "          <tr>\n" +
                    "            <td>" + palette.getControl(paletteValue) + "</td>\n" + 
                    "            <td>" + paletteScale.getLabel() + "</td>\n" +
                    "            <td>" + paletteScale.getControl(paletteScaleValue) + "</td>\n" +
                    "            <td>" + paletteMin.getLabel() + "</td>\n" +
                    "            <td>" + paletteMin.getControl(paletteMinValue) + "</td>\n" +
                    "            <td>" + paletteMax.getLabel() + "</td>\n" +
                    "            <td>" + paletteMax.getControl(paletteMaxValue) + "</td>\n" +
                    "          </tr>\n" +
                    "        </table>\n" +
                    "      </td>\n" +
                    "    </tr>\n");
            }
            /*paletteID = paletteValue + "_" + paletteScaleValue + "_" + paletteMinValue + "_" + paletteMaxValue;
            fullDataSetCptName = oneOf.fullPrivateDirectory() + paletteID + ".cpt";
            CompoundColorMap.makeCPT(
                oneOf.fullPaletteDirectory() + paletteValue + ".cpt", 
                paletteScaleValue, String2.parseDouble(paletteMinValue), String2.parseDouble(paletteMaxValue), 
                true, fullDataSetCptName);*/
            fullDataSetCptName = CompoundColorMap.makeCPT(
                oneOf.fullPaletteDirectory(), 
                paletteValue, paletteScaleValue, 
                String2.parseDouble(paletteMinValue), 
                String2.parseDouble(paletteMaxValue), 
                -1, true, oneOf.fullPrivateDirectory());
        }

        //color
        colorValue = color.getValue(session);
        if (plotData) {
            if (color.isValid(colorValue).length() > 0) {
                colorValue = color.getDefaultValue();
                color.setValue(session, colorValue);
            }
            if (showThisScreen) 
                addTableEntry(color, colorLabel, colorValue, rowNumber, step, htmlSB);
            if (verbose) String2.log("  colorValue=" + colorValue);
        }

        
        legendTime = null;
        if (plotData) {
            //make dateTime String 
            legendTime = TimePeriods.getLegendTime(timePeriodValue, centeredTimeValue);
        }

        //generate file names 
        //make file names easily parsed for specific values
        //FILE_NAME_RELATED_CODE
        averageFileName = null;
        addToImageFileName = "";  //append stuff, then hash at end
        Table averageTable = null;
        if (plotData) {
            //make the table of averaged values and store in datafile
            averageFileName = FileNameUtility.makeAveragedPointTimeSeriesName(
                pointDataSet.internalName, //e.g., PNBwspd
                unitsIndex == 1? 'A' : 'S', //e.g., A=Alternate units S=Standard units
                currentMinX, currentMaxX, currentMinY, currentMaxY, 
                depthDouble, depthDouble, centeredTimeValue, centeredTimeValue, timePeriodValue);
               
            String fullAverageName = oneOf.fullPrivateDirectory() + averageFileName + ".nc";

            //make first part of addToImageFileName  //full info for now, hashed below
            addToImageFileName = averageFileName + 
                paletteValue + paletteScaleValue + paletteMinValue + paletteMaxValue +
                colorValue;

            //reuse or generate file: fullAverageName  
            if (File2.touch(fullAverageName)) {
                //read the table from an existing file
                //unresolved problem with relying on existing file:
                //  file for composite of current month would change hourly as new data arrives
                //  but with re-use of file, new data won't be added.
                if (verbose) String2.log("  averageTable reusing " + averageFileName + ".nc");
                averageTable = new Table();
                averageTable.readFlatNc(fullAverageName, null, 1); //standardizeWhat=1
            } else {
                //make the table with 6 columns (x, y, z, t, id, data).
                //(almost identical code in Browser for getStationAverage)
                if (verbose) String2.log("  averageTable creating " + averageFileName + ".nc");
                averageTable = pointDataSet.makeAveragedTimeSeries(currentMinX, currentMaxX,
                    currentMinY, currentMaxY, depthDouble, depthDouble, centeredTimeValue, centeredTimeValue, 
                    timePeriodValue);
                //String2.log("  columnNames=" + String2.toCSSVString(averageTable.getColumnNames()));

                //convert to alt units
                if (unitsIndex == 1) {
                    averageTable.getColumn(5).scaleAddOffset(
                        pointDataSet.altScaleFactor, pointDataSet.altOffset);
                }


                //don't remove stations with no valid data 
                //mv indicates the station exists but has no data
                //they may have valid data in longer time series,
                //  and presence in this file gets station into timeSeries option list

                //save as .nc file   
                if (averageTable.nRows() == 0) {
                    addToImageFileName = "";
                    averageFileName =  null;
                } else {
                    //String2.log("  averageTable=" + averageTable);
                    averageTable.saveAsFlatNc(fullAverageName, "row"); //was "station", not "time"
                }
            } 


            //averageGraphDataLayer was created here
        }

        //display the point's Get links
        if (plotData && showThisScreen) {
            //display the point's Get averaged data file options
            showGetPointDataLinks(
                "stationData", oneOf.pointGetAvgOptions(), oneOf.pointGetAvgTitles(),
                oneOf, session, htmlSB, rowNumber, step,
                getAvgLabel, averageTable.nRows() > 0, dataAccessAllowed, 
                emaClass, pointDataSet.internalName, 
                currentMinX, currentMaxX, currentMinY, currentMaxY, 
                depthValue, depthValue,
                centeredTimeValue, centeredTimeValue, timePeriodValue);
        }

        //timeSeries
        timeSeriesIndex = -1;
        timeSeriesValue = null;
        timeSeriesFileName = null;
        timeSeriesOptions = null;
        timeSeriesLon = Double.NaN;
        timeSeriesLat = Double.NaN;
        timeSeriesID = null;
        beginTimeValue = null;
        if (plotData && averageTable.nRows() > 0) { //averageTable nRows tests if stations in range
            StringArray timeSeriesOptionsSA = new StringArray();
            timeSeriesOptionsSA.add("");
            //subsetTable has 6 columns (x, y, z, t, id, data).
            //if (verbose) String2.log("  subsetTable right before make timeSeriesOptions: " + subsetTable.toString(100));
            //include all relevant stations, even if data for timePeriod is mv
            int n = averageTable.nRows();
            for (int i = 0; i < n; i++) {
                double tData = averageTable.getDoubleData(5, i); 
                timeSeriesOptionsSA.add(
                    "Station " + averageTable.getStringData(4, i) + " (" + 
                    String2.genEFormat10(averageTable.getDoubleData(1, i)) + " N, " + 
                    String2.genEFormat10(averageTable.getDoubleData(0, i)) + " E)");
            }
            //averaged table is sorted by ID
            //don't sort here, because connection to averageTable used below

            //do the timeSeries GUI stuff
            timeSeriesOptions = timeSeriesOptionsSA.toArray();
            //String2.log("  timeSeriesOptions" + whichPointScreen1 + "=" + String2.toCSSVString(timeSeriesOptions));
            //String2.log("  session timeSeries=" + timeSeries.getValue(session));
            timeSeriesValue = timeSeries.setOptionsAndValidate(timeSeriesOptions, session, 0);
            timeSeriesIndex = timeSeries.indexOf(timeSeriesValue);
            if (doTally)
                oneOf.tally().add("Station Time Series Usage:", timeSeriesValue);
            if (verbose) String2.log("  timeSeriesValue = " + timeSeriesValue);
            if (showThisScreen) {
                timeSeries.setLabel(String2.substitute(timeSeriesLabel, "" + (step.i++), null, null));
                if (timeSeriesOptions.length > 1) //there's always a 0 option: ""
                    htmlSB.append(
                        "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
                        "      <td>" + timeSeries.getLabel() + "</td>\n" +
                        "      <td> of 'Time Period' averages, for\n        " + 
                        timeSeries.getControl(timeSeriesValue) + "\n" +
                        "        Or, click on a station marker on the map.</td>\n" +
                        "    " + emaClass.getEndRow() + "\n");
                else htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
                    "      <td>" + timeSeries.getLabel() + "</td>\n" +
                    "      <td>" + oneOf.noDataAvailable() + "</td>\n" +
                    "    " + emaClass.getEndRow() + "\n");
            }

            if (timeSeriesIndex > 0) {
                timeSeriesID = averageTable.getStringData(4, timeSeriesIndex - 1); //-1 because of 0=none option
                timeSeriesLat = averageTable.getDoubleData(1, timeSeriesIndex - 1); //-1 because of 0=none option
                timeSeriesLon = averageTable.getDoubleData(0, timeSeriesIndex - 1); //-1 because of 0=none option

                //beginTime
                double tMinTime = pointDataSet.getStationMinTime(timeSeriesID);
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
                if (verbose) String2.log("  beginTimeValue = " + beginTimeValue);
                if (showThisScreen) 
                    addTableEntry(beginTime, beginTimeLabel, beginTimeValue, rowNumber, step, htmlSB);
            
                //make the graph GraphDataLayer
                //averageTable has 6 columns (x, y, z, t, id, data).
                timeSeriesFileName = FileNameUtility.makeAveragedPointTimeSeriesName(
                    pointDataSet.internalName, 
                    (unitsIndex == 1? 'A' : 'S'), //e.g., A=Alternate units S=Standard units
                    timeSeriesLon, timeSeriesLon, 
                    timeSeriesLat, timeSeriesLat, depthDouble, depthDouble,
                    beginTimeValue, centeredTimeValue, timePeriodValue);

                String fullTimeSeriesFileName = oneOf.fullPrivateDirectory() + timeSeriesFileName + ".nc";
         
                //oneOf.fullPrivateDirectory(), fileName + ".nc");
                addToImageFileName += timeSeriesFileName; //will be hashCoded below

                //make the table of timeSeries values and store in datafile
                Table timeSeriesTable = null;
                if (File2.touch(fullTimeSeriesFileName)) {
                    if (verbose) String2.log("  timeSeries reusing " + timeSeriesFileName + ".nc");
                    timeSeriesTable = new Table();
                    timeSeriesTable.readFlatNc(fullTimeSeriesFileName, null, 1); //standardizeWhat=1
                } else {
                    //unresolved problem with relying on existing file:
                    //  file for composite of current month would change hourly as new data arrives
                    //  but with re-use of file, new data won't be added.
                    //table has 6 columns (x, y, z, t, id, data)
                    if (verbose) String2.log("  timeSeries creating " + timeSeriesFileName + ".nc");
                    timeSeriesTable = pointDataSet.makeAveragedTimeSeries(
                        timeSeriesLon, timeSeriesLon,
                        timeSeriesLat, timeSeriesLat, 
                        depthDouble, depthDouble, 
                        beginTimeValue, centeredTimeValue, timePeriodValue);
                    //String2.log("  timeSeriesTable=" + timeSeriesTable);

                    //convert to alt units
                    if (unitsIndex == 1) {
                        timeSeriesTable.getColumn(5).scaleAddOffset(
                            pointDataSet.altScaleFactor, pointDataSet.altOffset);
                    }

                    //don't remove missing values
                    //1) they are used to mark gaps in the time series line on the graph
                    //2) if a user wants to combine two data files, side by side,
                    //    e.g., xWind and yWind, it only works if the time steps
                    //    are identical and without missing value rows removed

                    //save as .nc file   
                    //String2.log("  timeSeriesTable " + fullTimeSeriesFileName + ":\n" + timeSeriesTable.toString(100));
                    if (timeSeriesTable.nRows() == 0) {
                        timeSeriesFileName = null;
                    } else {
                        timeSeriesTable.saveAsFlatNc(fullTimeSeriesFileName, "row");
                    }
                } 

                //graph GraphDataLayer was created here

                //time series point's Get links
                if (plotData && showThisScreen) {
                    showGetPointDataLinks(
                        "stationData", oneOf.pointGetTSOptions(), oneOf.pointGetTSTitles(),
                        oneOf, session, htmlSB, rowNumber, step,
                        getTSLabel, timeSeriesFileName != null, dataAccessAllowed,
                        emaClass, pointDataSet.internalName, 
                        timeSeriesLon, timeSeriesLon, 
                        timeSeriesLat, timeSeriesLat, 
                        depthValue, depthValue,
                        beginTimeValue, centeredTimeValue, timePeriodValue);
                }

            }
        }

        if (plotData) {
            addToImageFileName = "_P" + whichPointScreen1 + 
                String2.md5Hex12(addToImageFileName); //will be same for same info
        } else addToImageFileName = "";

    }

    /**
     * This returns the map GraphDataLayer with averaged data (or null).
     * This is made as needed so garbage collected after used.
     *
     * @return the average GraphDataLayer or null if trouble (this won't throw an exception)
     */
    public GraphDataLayer getMapGDL() {
        if (averageFileName == null)
            return null;

        try {
            Table averageTable = new Table();
            String fullAverageFileName = oneOf.fullPrivateDirectory() + averageFileName + ".nc";
            averageTable.readFlatNc(fullAverageFileName, null, 1); //standardizeWhat=1
            //String2.log("PointScreen" + whichPointScreen1 + ".getMapGDL table=\n" + averageTable.toString());
            return new GraphDataLayer(
                editOption, //sourceID
                0, 1, 5, 4, -1,  //x,y,data,id columns
                GraphDataLayer.DRAW_MARKERS, false, false,
                null, null, //x,yAxisTitle
                pointDataSet.boldTitle, 
                "(" + unitsValue + ") " + legendTime + ". Depth = " + depthValue + " meters.",  //title2
                pointDataSet.courtesy.length() == 0? null: "Data courtesy of " + pointDataSet.courtesy, //Station 
                null, //title4
                averageTable, null, null,
                new CompoundColorMap(fullDataSetCptName), 
                new Color(String2.parseInt("0x" + colorValue)),
                GraphDataLayer.MARKER_TYPE_FILLED_SQUARE, GraphDataLayer.MARKER_SIZE_SMALL, 
                0, //standardVector
                GraphDataLayer.REGRESS_NONE);
        } catch (Exception e) {
            String error = "Exception caught in PointScreen.getMapGDL:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), error);

            return null;            
        }
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
            return new GraphDataLayer(
                editOption, //sourceID
                3, 5, -1, -1, -1, //time,data, others not used
                GraphDataLayer.DRAW_LINES, true, false,
                "Time", unitsValue, //x,yAxisTitle
                pointDataSet.boldTitle + " Station " + timeSeriesID + ", " + 
                    String2.genEFormat10(timeSeriesLat) + " N, " + 
                    String2.genEFormat10(timeSeriesLon) + " E",
                "Time series of " + 
                    (timePeriodNHours == 0? "raw data" :
                        timePeriodValue + " averages") + //Centered Time
                    ". Depth = " + depthValue + " meters. (Horizontal line = average)", //title2
                pointDataSet.courtesy.length() == 0? null : "Data courtesy of " + pointDataSet.courtesy, //Station data
                null, //title4
                timeSeriesTable, null, null,
                null, new Color(String2.parseInt("0x" + colorValue)),
                GraphDataLayer.MARKER_TYPE_NONE, 0, 
                0, //standardVector
                GraphDataLayer.REGRESS_MEAN);
      
        } catch (Exception e) {
            String error = "Exception caught in PointScreen.getGraphGDL:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), error);

            return null;            
        }
    }

    /**
     * This appends a row to the table so the user can choose to download 
     * point data.
     * It is used by this class, gridScreen, and vectorPointScreen.
     *
     */
    public static void showGetPointDataLinks(
        String getWhat, String[] getOptions, String[] getTitles,
        OneOf oneOf, HttpSession session, 
        StringBuilder htmlSB, IntObject rowNumber, IntObject step,
        String getLabel, boolean tableHasData, boolean dataAccessAllowed,
        EmaClass emaClass, String dataSetInternalName, 
        double minX, double maxX, double minY, double maxY, 
        String minDepth, String maxDepth,
        String beginTimeValue, String endTimeValue, String timePeriodValue) {

        htmlSB.append(
            "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
            "      <td>" + 
                String2.substitute(getLabel, "" + (step.i++), null, null) + 
                "</td>\n" +
            "      <td style=\"width:90%;\">\n"); //force this column to expand as much as possible
        boolean hasNoBr = false;
        if (!tableHasData) {
            htmlSB.append("        " + oneOf.noDataAvailable() + "\n");
        } else if (!dataAccessAllowed) {
            htmlSB.append("        " + oneOf.dataAccessNotAllowed() + "\n");
        } else {
            //htmlSB.append(
            //    "        " + getAsc.getControl(getAsc.getValue(session)) + "\n" +
            //    "        " + getNc.getControl( getNc.getValue(session))  + "\n");
            String tUrl = "        <a href=\"" + oneOf.url() + 
                "?get=" + getWhat +
                "&amp;dataSet=" + dataSetInternalName +
                "&amp;timePeriod=" + String2.replaceAll(timePeriodValue, " ", "") + 
                "&amp;minLon=" + String2.genEFormat10(minX) + 
                "&amp;maxLon=" + String2.genEFormat10(maxX) + 
                "&amp;minLat=" + String2.genEFormat10(minY) + 
                "&amp;maxLat=" + String2.genEFormat10(maxY) +
                "&amp;minDepth=" + minDepth + "&amp;maxDepth=" + maxDepth +
                "&amp;beginTime=" + String2.replaceAll(beginTimeValue, ' ', 'T') + //so connector is 'T'
                "&amp;endTime="   + String2.replaceAll(endTimeValue,   ' ', 'T') + //so connector is 'T'
                "&amp;fileType="; //.nc" + "\">.nc</a>"

            for (int i = 0; i < getOptions.length; i++) 
                htmlSB.append(
                    tUrl + String2.replaceAll(getOptions[i], " ", "") + 
                    "\" title=\"" + getTitles[i] + 
                    "\">" + getOptions[i]+ "</a> " + 
                    (i == getOptions.length - 1? " &nbsp; &nbsp; " : "|") + 
                    "\n");


            //someday: opendap link  ???

            //file type help link
            hasNoBr = true;
            htmlSB.append("        <span style=\"white-space:nowrap;\">\n"); 
            htmlSB.append(oneOf.pointFileHelpLink());

            //GETQuery help link
            htmlSB.append("        |\n");
            htmlSB.append(oneOf.GETQueryHelpLink());


        }

        //data set help link
        String infoUrl = oneOf.getInternalNameInfoUrl(dataSetInternalName);
        if (infoUrl.length() > 0)
            htmlSB.append("        |\n");
        htmlSB.append(infoUrl);
        if (hasNoBr)
            htmlSB.append("        </span>\n");

        htmlSB.append("      </td>\n" +
                  "    </tr>\n");
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a Get button.
     *
     * @param submitter
     * @return true if the submitter's name matches the name of 
     *    a Get button (e.g., getAsc); else false. 
     */
    public boolean submitterIsAGetButton(String submitter) {
        return false;
    }

}
