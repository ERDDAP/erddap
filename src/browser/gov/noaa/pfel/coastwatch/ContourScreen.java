/* 
 * ContourScreen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.ema.*;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.IntObject;

import java.util.GregorianCalendar;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for the contour screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Contour").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class ContourScreen extends Screen {

    public EmaSelect  dataSet, units, timePeriod;
    public EmaDateTimeSelect2 centeredTime;
    public EmaColor   color;
    public EmaString  drawLinesAt;

    //set by the constructor
    public String dataSetLabel, unitsLabel, timePeriodLabel, 
        timeLabel, drawLinesAtLabel, getLabel, colorLabel;


    //set by validate
    public boolean plotData;
    public boolean plotBathymetryData; //true if plotting bathymetry data 

    public String dataSetValue;
    public int dataSetIndex;

    private GridDataSet gridDataSet;
    public double altScaleFactor;
    public double altOffset;
    public String boldTitle;
    public String courtesy;
    public boolean isClimatology;

    public String timePeriodValue;
    public int timePeriodIndex;

    public String timeValue;
    public int timeIndex;
    public GregorianCalendar startCalendar, endCalendar;
    private String lastModernTime = "9999-12-12 23:59:59";

    public String colorValue;

    public int unitsIndex;
    public String unitsValue;

    public String drawLinesAtValue; 

    public String legendTime;
    public boolean dataAccessAllowed;

    public String addToImageFileName;
    
    /** 
     * The constructor for ContourScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public ContourScreen(int editOption, OneOf oneOf, Shared shared, EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in ContourScreen(): ";

        //add the components to the emaClass
        emaClass.addAttribute(dataSet        = new EmaSelect(emaClass, "contourDataSet"));
        emaClass.addAttribute(units          = new EmaSelect(emaClass, "contourUnits"));
        emaClass.addAttribute(timePeriod     = new EmaSelect(emaClass, "contourTimePeriod"));
        emaClass.addAttribute(centeredTime   = new EmaDateTimeSelect2(emaClass, "contourCenteredTime"));
        emaClass.addAttribute(drawLinesAt    = new EmaString(emaClass, "contourDrawLinesAt"));
        emaClass.addAttribute(color          = new EmaColor( emaClass, "contourColor"));

        //get all the labels 
        dataSetLabel      = classRB2.getNotNullString("contourDataSet.label", errorInMethod);
        unitsLabel        = classRB2.getNotNullString("contourUnits.label", errorInMethod);
        timePeriodLabel   = classRB2.getNotNullString("contourTimePeriod.label", errorInMethod);
        timeLabel         = classRB2.getNotNullString("contourCenteredTime.label", errorInMethod);
        getLabel          = classRB2.getNotNullString("contourGet.label", errorInMethod);
        drawLinesAtLabel  = classRB2.getNotNullString("contourDrawLinesAt.label", errorInMethod);
        colorLabel        = classRB2.getNotNullString("contourColor.label", errorInMethod);

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
     * @param imageSizeIndex
     */
    public void validate(HttpSession session, int showEditOption, IntObject step, 
            IntObject rowNumber, StringBuilder htmlSB, 
            double currentMinX, double currentMaxX, double currentMinY, double currentMaxY, 
            String cWESNString, int imageSizeIndex) throws Exception {

        //use the latest info
        String dataSetOptions[] = shared.activeGridDataSetOptions();
        dataSet.setOptions(dataSetOptions); 

        //dataSet
        dataSetValue = dataSet.getValue(session);
        dataSetIndex = String2.indexOf(dataSetOptions, dataSetValue);
        if (dataSetIndex < 0) {
            //check for 7char name
            dataSetIndex = String2.indexOf(shared.activeGridDataSet7Names(), dataSetValue);
            if (dataSetIndex < 0)
                dataSetIndex = 0; //none
            dataSetValue = dataSetOptions[dataSetIndex];
            dataSet.setValue(session, dataSetValue);
        }
        if (oneOf.verbose()) String2.log("contourScreen dataSetValue = " + dataSetValue);
        plotData = dataSetIndex > 1; 
        plotBathymetryData = dataSetIndex == 1;
        if (doTally)
            oneOf.tally().add("Contour Data Set Usage:", dataSetValue);
        if (showEditOption == editOption)
            addTableEntry(dataSet, dataSetLabel, dataSetValue, rowNumber, step, htmlSB);

        //timePeriod
        gridDataSet     = null;
        boldTitle       = null;
        courtesy        = null;
        isClimatology   = false;
        timePeriodIndex = -1;
        if (plotData) {
            gridDataSet = (GridDataSet)shared.activeGridDataSets().get(dataSetIndex);
            String[] activeTimePeriodOptions = gridDataSet.activeTimePeriodOptions;
            String[] activeTimePeriodTitles  = gridDataSet.activeTimePeriodTitles;

            timePeriod.setOptions(activeTimePeriodOptions);
            timePeriod.setTitles(activeTimePeriodTitles);
            timePeriodIndex = TimePeriods.closestTimePeriod(
                timePeriod.getValue(session), activeTimePeriodOptions);
            timePeriodValue = gridDataSet.activeTimePeriodOptions[timePeriodIndex];
            timePeriod.setValue(session, timePeriodValue);
            if (oneOf.verbose()) String2.log("  timePeriodValue = " + timePeriodValue);
            if (showEditOption == editOption) 
                addTableEntry(timePeriod, timePeriodLabel, timePeriodValue, rowNumber, step, htmlSB);

            boldTitle = gridDataSet.boldTitle;
            courtesy = gridDataSet.courtesy;
            isClimatology = gridDataSet.isClimatology;
        }

        //time
        timeValue = null; //centered
        timeIndex = -1;
        startCalendar = null;
        endCalendar = null;
        if (plotData) {
            String[] activeContourTimeOptions = 
                (String[])gridDataSet.activeTimePeriodTimes.get(timePeriodIndex);
            centeredTime.setOptions(activeContourTimeOptions);
            timeValue = centeredTime.getValue(session);

            //if was climatology and now not
            if (timeValue == null)
                timeValue = lastModernTime;
            if (timeValue.startsWith("0001-") && !activeContourTimeOptions[0].startsWith("0001-"))
                timeValue = lastModernTime.substring(0, 5) +
                    timeValue.substring(5);
            //If was regular and now climatology, it is dealt with by gridDataSetClimatology.findClosestTime.
            //Synchronizing (non)climatology datasets is done by cwUser.synchTime.

            timeIndex = gridDataSet.binaryFindClosestTime(
                activeContourTimeOptions, String2.replaceAll(timeValue, "T", " "));
            timeValue = activeContourTimeOptions[timeIndex];
            centeredTime.setValue(session, timeValue);

            //store lastModernTime
            if (!timeValue.startsWith("0001-"))
                lastModernTime = timeValue;
            if (oneOf.verbose()) String2.log("  timeValue = " + timeValue);
            if (showEditOption == editOption)
                addTableEntry(centeredTime, timeLabel, timeValue, rowNumber, step, htmlSB);
            startCalendar = TimePeriods.getStartCalendar(timePeriodValue, timeValue, "");
            endCalendar   = TimePeriods.getEndCalendar(timePeriodValue, timeValue, "");
            //String2.log("contourScreen startCalendar=" + Calendar2.formatAsISODateTimeT(startCalendar) +
            //            " endCalendar=" + Calendar2.formatAsISODateTimeT(endCalendar));
        }

        //palette
        /*
        //no palette option -- too many options and most irrelevant
        //keep it under my control -- use a predefined palette for each dataset
        int paletteIndex = -1;
        if (plotData) {
            palette.setValue(session, gridDataSet.cptFile); //my selection
            paletteIndex = palette.indexOf(palette.getValue(session));
            if (paletteIndex < 0) {
                paletteIndex = 0;
                palette.setValue(session, paletteOptions[0]);
            }
            if (showEditOption == editOption)
                addTableEntry(palette, paletteLabel, palette.getValue(session), rowNumber, step, htmlSB);
            if (oneOf.verbose())
                String2.log("  paletteValue = " + palette.getValue(session));
        }
        */

        //color
        colorValue = color.getValue(session);
        if (plotData || plotBathymetryData) {
            if (color.isValid(colorValue).length() > 0) {
                colorValue = color.getDefaultValue();
                color.setValue(session, colorValue);
            }
            if (showEditOption == editOption)
                addTableEntry(color, colorLabel, colorValue, rowNumber, step, htmlSB);
            if (oneOf.verbose()) String2.log("  colorValue = " + colorValue);
        }

        //units
        unitsIndex = 0;
        unitsValue = "";
        altScaleFactor = 1;
        altOffset = 0;
        if (plotData) {
            String[] unitsOptions = gridDataSet.unitsOptions;
            units.setOptions(unitsOptions);
            unitsValue = units.getValue(session);
            unitsIndex = String2.indexOf(unitsOptions, unitsValue);
            if (unitsIndex < 0) {
                unitsIndex = gridDataSet.defaultUnits == 'A'? 1 : 0;
                unitsValue = gridDataSet.unitsOptions[unitsIndex];
                units.setValue(session, unitsValue);
                drawLinesAt.setValue(session, "");
            }
            if (unitsIndex == 1) {
                altScaleFactor = gridDataSet.altScaleFactor;
                altOffset = gridDataSet.altOffset;
            }
            if (oneOf.verbose()) String2.log("  unitsValue = " + unitsValue + " asf=" + altScaleFactor + " ao=" + altOffset);
            if (showEditOption == editOption)
                addTableEntry(units, unitsLabel, unitsValue, rowNumber, step, htmlSB);
        }

        //drawLinesAt
        drawLinesAtValue = drawLinesAt.getValue(session).trim(); 
        String cdlav = null;
        if (plotData || plotBathymetryData) {
            //ensure perfectly valid and in standard format (to avoid map redraw for minor difference)
            String defaultContourLinesAt = plotData?
                (unitsIndex <= 0 ? gridDataSet.contourLinesAt : gridDataSet.altContourLinesAt) :
                SgtMap.BATHYMETRY_LINES_AT; 
            drawLinesAtValue = String2.toCSSVString(String2.justFiniteValues(
                String2.csvToDoubleArray(drawLinesAtValue)));
            if (drawLinesAtValue == null || drawLinesAtValue.length() == 0) {
                String2.log("  resetting drawLinesAtValue");
                drawLinesAtValue = defaultContourLinesAt;
            }
            cdlav = String2.replaceAll(drawLinesAtValue, " ", "");
            cdlav = String2.replaceAll(cdlav, ",", "_");

            //store value in session
            drawLinesAt.setValue(session, drawLinesAtValue);
            if (oneOf.verbose()) 
                String2.log("  drawLinesAtValue = " + drawLinesAtValue);
            if (showEditOption == editOption) 
                addTableEntry(drawLinesAt, drawLinesAtLabel, drawLinesAtValue, rowNumber, step, htmlSB);
        }

        //generate file names
        //make file names easily parsed for specific values
        //FILE_NAME_RELATED_CODE
        legendTime         = "";
        dataAccessAllowed  = false;
        addToImageFileName = "";
        if (plotData) {
            String baseFileName = FileNameUtility.makeBaseName(
                gridDataSet.internalName, //e.g., LATssta
                unitsIndex == 1? 'A' : 'S', //e.g., A=Alternate units S=Standard units
                timePeriodValue, //e.g., 1day
                timeValue);         
            addToImageFileName += "_C" +  
                String2.md5Hex12(baseFileName + colorValue + cdlav); //will be same for same info; 
            dataAccessAllowed = gridDataSet.dataAccessAllowedCentered(timePeriodValue, timeValue); //centered

            //make legendTime String 
            legendTime = gridDataSet.getLegendTime(timePeriodValue, timeValue);
            //String2.log("  legendTime=" + legendTime);

        } else if (plotBathymetryData) {

            //set info if plotBathymetryData  
            boldTitle = SgtMap.BATHYMETRY_BOLD_TITLE + " (" + SgtMap.BATHYMETRY_UNITS + ")";
            courtesy = SgtMap.BATHYMETRY_COURTESY;
            isClimatology = false;
            unitsValue = ""; //added to boldTitle above
            String baseFileName = SgtMap.BATHYMETRY_7NAME;         
            addToImageFileName += "_C" + 
                String2.md5Hex12(baseFileName + colorValue + cdlav); 
            dataAccessAllowed = true;

            if (showEditOption == editOption)
                GridScreen.showGetBathymetryDataLinks(oneOf, htmlSB, rowNumber, step,
                    getLabel, currentMinX, currentMaxX, currentMinY, currentMaxY);
        } else {
            //if !plotData and !plotBathymetryData
        }

        //add the 'Get' options line to the form
        if (plotData && showEditOption == editOption) {
        
            GridScreen.showGetGridDataLinks(oneOf, htmlSB, rowNumber, step,
                getLabel, gridDataSet, timePeriodIndex, timeValue,
                dataAccessAllowed, 
                currentMinX, currentMaxX, currentMinY, currentMaxY);

        }

    }

    /**
     * This actually generates the image-resolution grid,
     * so it should be called after validate() if/when you actually need to use the data.
     *
     * @param minLon
     * @param maxLon
     * @param minLat
     * @param maxLat
     * @param imageWidth  from oneOf.imageWidths()[imageSizeIndex]
     * @param imageHeight
     * @return the grid, as best it can.
     *     If !plotData, this returns null.
     *     If trouble (e.g., no data in range), this returns null and sets plotData to false.
     */
    public Grid getGrid(double minLon, double maxLon, 
            double minLat, double maxLat, int imageWidth, int imageHeight) throws Exception { 
        try {
            if (plotData) 
                return gridDataSet.makeGrid(
                    timePeriodValue, timeValue, 
                    minLon, maxLon, minLat, maxLat, imageWidth, imageHeight);
            else if (plotBathymetryData) 
                return SgtMap.createTopographyGrid(oneOf.fullPrivateDirectory(),
                    minLon, maxLon, minLat, maxLat, imageWidth, imageHeight);
            else return null;
        } catch (Exception e) {
            String error = String2.ERROR + " in ContourScreen.getGrid:\n" + MustBe.throwableToString(e);
            String2.log(error);
            if (error.indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                oneOf.email(oneOf.emailEverythingTo(), 
                    String2.ERROR + " in " + oneOf.shortClassName(), error);
            plotData = false;
            plotBathymetryData = false;
            return null;
        }
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a Get button.
     *
     * @param submitter
     * @return false
     */
    public boolean submitterIsAGetButton(String submitter) {
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
