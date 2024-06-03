/* 
 * GridScreen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.PrimitiveArray;
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
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.CompoundColorMap;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.IntObject;

import java.awt.Color;
import java.util.GregorianCalendar;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for the Grid screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Grid Data").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class GridScreen extends Screen {

    public EmaSelect  dataSet, units, timePeriod, view, palette, paletteScale;
    public EmaDateTimeSelect2 beginTime, centeredTime;
    public EmaDouble paletteMin, paletteMax, timeSeriesLon, timeSeriesLat;
    public EmaColor   timeSeriesColor;


    //set by the constructor
    public String dataSetLabel, unitsLabel, timePeriodLabel, beginTimeLabel, 
        centeredTimeLabel, paletteLabel, getLabel, getAnomalyLabel,
        timeSeriesLonLabel, timeSeriesLatLabel, timeSeriesLonLatLabel, 
        timeSeriesLonLatOrLabel, timeSeriesColorLabel, getTSLabel,
        viewLabel;

    //set by validate
    public boolean plotData; //true if plotting GridDataSet data
    public boolean plotBathymetryData; //true if plotting bathymetry data 
    public String dataSetValue;
    public int dataSetIndex;

    private GridDataSet gridDataSet;
    public double altScaleFactor;
    public double altOffset;
    public String boldTitle;
    public String courtesy;
    public boolean isClimatology;

    public int timePeriodIndex;
    public String timePeriodValue;
    public int timePeriodNHours;

    public String beginTimeValue, centeredTimeValue;
    public int centeredTimeIndex;
    public GregorianCalendar startCalendar, endCalendar;
    private String lastModernTime = "9999-12-12 23:59:59";

    public int unitsIndex;
    public String unitsValue;

    public String paletteValue;
    public String paletteScaleValue;
    public String paletteMinValue;
    public String paletteMaxValue;
    public String fullDataSetCptName;

    public String startImageFileName;
    public String legendTime;
    public boolean dataAccessAllowed;

    public String timeSeriesLonValue;
    public String timeSeriesLatValue;
    public String timeSeriesFileName; //no directory at start or .nc at end; may be null
    public String timeSeriesColorValue;
    public double timeSeriesLatD, timeSeriesLonD;


    /** 
     * The constructor for GridScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public GridScreen(int editOption, OneOf oneOf, Shared shared, EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in GridScreen(): ";

        //add the components to the emaClass
        emaClass.addAttribute(dataSet                 = new EmaSelect(emaClass, "gridDataSet")); 
        emaClass.addAttribute(units                   = new EmaSelect(emaClass, "gridUnits"));
        emaClass.addAttribute(timePeriod              = new EmaSelect(emaClass, "gridTimePeriod"));
        emaClass.addAttribute(beginTime               = new EmaDateTimeSelect2(emaClass, "gridBeginTime"));
        emaClass.addAttribute(centeredTime            = new EmaDateTimeSelect2(emaClass, "gridCenteredTime"));
        emaClass.addAttribute(view                    = new EmaSelect(emaClass, "gridView"));
        emaClass.addAttribute(paletteMin              = new EmaDouble(emaClass, "gridPaletteMin"));
        emaClass.addAttribute(paletteMax              = new EmaDouble(emaClass, "gridPaletteMax"));
        emaClass.addAttribute(palette                 = new EmaSelect(emaClass, "gridPalette"));
        emaClass.addAttribute(paletteScale            = new EmaSelect(emaClass, "gridPaletteScale"));
        emaClass.addAttribute(timeSeriesLon           = new EmaDouble(emaClass, "gridTimeSeriesLon"));
        emaClass.addAttribute(timeSeriesLat           = new EmaDouble(emaClass, "gridTimeSeriesLat"));
        emaClass.addAttribute(timeSeriesColor         = new EmaColor( emaClass, "gridTimeSeriesColor"));

        //get all the labels 
        dataSetLabel                 = classRB2.getString("gridDataSet.label",  null);
        unitsLabel                   = classRB2.getString("gridUnits.label",  null);
        timePeriodLabel              = classRB2.getString("gridTimePeriod.label",  null);
        beginTimeLabel               = classRB2.getString("gridBeginTime.label",  null);
        centeredTimeLabel            = classRB2.getString("gridCenteredTime.label",  null);
        viewLabel                    = classRB2.getString("gridView.label",  null);
        paletteLabel                 = classRB2.getString("gridPalette.label",  null);
        getLabel                     = classRB2.getString("gridGet.label",  null);
        getAnomalyLabel              = classRB2.getString("gridGetAnomaly.label",  null);
        timeSeriesLonLabel           = classRB2.getString("gridTimeSeriesLon.label",  null);
        timeSeriesLatLabel           = classRB2.getString("gridTimeSeriesLat.label",  null);
        timeSeriesLonLatLabel        = classRB2.getString("gridTimeSeriesLonLat.label",  null);
        timeSeriesLonLatOrLabel      = classRB2.getString("gridTimeSeriesLonLatOr.label",  null);
        timeSeriesColorLabel         = classRB2.getString("gridTimeSeriesColor.label",  null);
        getTSLabel                   = classRB2.getString("gridGetTS.label",  null);

        Test.ensureNotNull(dataSetLabel,                 errorInMethod + "gridDataSet.label is null."); 
        Test.ensureNotNull(unitsLabel,                   errorInMethod + "gridUnits.label is null."); 
        Test.ensureNotNull(timePeriodLabel,              errorInMethod + "gridTimePeriod.label is null."); 
        Test.ensureNotNull(beginTimeLabel,               errorInMethod + "gridBeginTime.label is null."); 
        Test.ensureNotNull(centeredTimeLabel,            errorInMethod + "gridCenteredTime.label is null."); 
        Test.ensureNotNull(viewLabel,                    errorInMethod + "gridView.label is null."); 
        Test.ensureNotNull(paletteLabel,                 errorInMethod + "gridPalette.label is null."); 
        Test.ensureNotNull(getLabel,                     errorInMethod + "gridGet.label is null."); 
        Test.ensureNotNull(getAnomalyLabel,              errorInMethod + "gridGetAnomaly.label is null."); 
        Test.ensureNotNull(timeSeriesLonLabel,           errorInMethod + "gridTimeSeriesLon.label is null."); 
        Test.ensureNotNull(timeSeriesLatLabel,           errorInMethod + "gridTimeSeriesLat.label is null."); 
        Test.ensureNotNull(timeSeriesLonLatLabel,        errorInMethod + "gridTimeSeriesLonLat.label is null."); 
        Test.ensureNotNull(timeSeriesLonLatOrLabel,      errorInMethod + "gridTimeSeriesLonLatOr.label is null."); 
        Test.ensureNotNull(timeSeriesColorLabel,         errorInMethod + "gridTimeSeriesColor.label is null."); 
        Test.ensureNotNull(getTSLabel,                   errorInMethod + "gridGetTS.label is null."); 

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
        dataSet.setOptions(shared.activeGridDataSetOptions()); 

        //dataSet
        dataSetValue = dataSet.getValue(session);
        dataSetIndex = String2.indexOf(shared.activeGridDataSetOptions(), dataSetValue);
        //if (dataSetValue.startsWith("---")) {
        //    //deal with categories
        //    dataSetIndex++;
        //    dataSetValue = shared.activeGridDataSetOptions()[dataSetIndex];
        //    dataSetIndex = String2.indexOf(shared.activeGridDataSetOptions(), dataSetValue);
        //}
        if (dataSetIndex < 0) {
            //check for 7char name
            dataSetIndex = String2.indexOf(shared.activeGridDataSet7Names(), dataSetValue);
            if (dataSetIndex < 0)
                dataSetIndex = 1; //Bath
            dataSetValue = shared.activeGridDataSetOptions()[dataSetIndex];
            dataSet.setValue(session, dataSetValue);
        }
        if (oneOf.verbose()) 
            String2.log("gridScreen dataSetValue = " + dataSetValue);
        plotData = dataSetIndex > 1;
        plotBathymetryData = dataSetIndex == 1;
        if (showEditOption == editOption) {
            emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
            dataSet.setLabel(String2.substitute(dataSetLabel, "" + (step.i++), null, null));
            htmlSB.append(dataSet.getTableEntry(dataSetValue, displayErrorMessages));
        }
        if (doTally)
            oneOf.tally().add("Grid Data Set Usage:", dataSetValue);

        //timePeriod
        gridDataSet       = null;
        boldTitle         = null;
        courtesy          = null;
        isClimatology     = false;
        timePeriodIndex   = -1;
        timePeriodValue   = null;
        timePeriodNHours  = -1;
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
            timePeriodNHours = TimePeriods.getNHours(timePeriodValue);
            if (doTally)
                oneOf.tally().add("Grid Time Period Usage:", timePeriodValue);
            if (oneOf.verbose()) String2.log("  timePeriodValue = " + timePeriodValue);
            if (showEditOption == editOption) {
                emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
                timePeriod.setLabel(String2.substitute(timePeriodLabel, "" + (step.i++), null, null));
                htmlSB.append(timePeriod.getTableEntry(timePeriodValue, displayErrorMessages));
            }
            boldTitle = gridDataSet.boldTitle;
            courtesy = gridDataSet.courtesy;
            isClimatology = gridDataSet.isClimatology;
        }

        //centeredTime
        centeredTimeValue = null;
        centeredTimeIndex = -1;
        String[] activeTimeOptions = null;
        if (plotData) {
            activeTimeOptions = (String[])gridDataSet.activeTimePeriodTimes.get(timePeriodIndex);
            centeredTime.setOptions(activeTimeOptions);
            centeredTimeValue = centeredTime.getValue(session);
            //String2.log("  oCenteredTimeValue = " + centeredTimeValue);

            //if was climatology and now not
            if (centeredTimeValue == null)
                centeredTimeValue = lastModernTime;
            if (centeredTimeValue.startsWith("0001-") && !activeTimeOptions[0].startsWith("0001-"))
                centeredTimeValue = lastModernTime.substring(0, 5) +
                    centeredTimeValue.substring(5);
            //If was regular and now climatology, it is dealt with by gridDataSetClimatology.findClosestTime.
            //Synchronizing (non)climatology datasets is done by cwUser.synchTime.

            centeredTimeIndex = gridDataSet.binaryFindClosestTime(activeTimeOptions, 
                String2.replaceAll(centeredTimeValue, "T", " "));
            centeredTimeValue = activeTimeOptions[centeredTimeIndex];
            centeredTime.setValue(session, centeredTimeValue);

            //store lastModernTime
            if (!centeredTimeValue.startsWith("0001-"))
                lastModernTime = centeredTimeValue;
            if (oneOf.verbose()) String2.log("  centeredTimeValue = " + centeredTimeValue);
            if (showEditOption == editOption) {
                emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
                centeredTime.setLabel(String2.substitute(centeredTimeLabel, "" + (step.i++), null, null));
                htmlSB.append(centeredTime.getTableEntry(centeredTimeValue, displayErrorMessages));
            }
        }

        //view anomaly
        boolean viewAnomaly = false; 
        if (plotData) { //I think it is good that viewAnomaly gets set back to false if no data plotted

            //is anomaly available?
            boolean anomalyAvailable = false;
            String aID = gridDataSet.anomalyDataSet; 
            GridDataSet anomalyDataSet = null;
            int anomalyDataSetIndex = -1;
            String anomalyTimePeriodValue = null;
            int anomalyTimePeriodIndex = -1;
            String anomalyActiveTimeOptions[] = null;
            int anomalyCenteredTimeIndex = -1;
            if (aID.length() > 0) {
                //is the anomaly dataset available?
                anomalyDataSetIndex = String2.indexOf(shared.activeGridDataSet7Names(), aID);
                if (anomalyDataSetIndex >= 0) {
                    anomalyDataSet = (GridDataSet)shared.activeGridDataSets().get(anomalyDataSetIndex);
                    String[] anomalyTimePeriodOptions = anomalyDataSet.activeTimePeriodOptions;

                    //is time period exact match?
                    anomalyTimePeriodIndex = TimePeriods.closestTimePeriod(
                        timePeriodValue, anomalyTimePeriodOptions);
                    anomalyTimePeriodValue = anomalyTimePeriodOptions[anomalyTimePeriodIndex];
                    //String2.log("  anomalyTimePeriod=" + anomalyTimePeriodValue);
                    if (TimePeriods.getNHours(anomalyTimePeriodValue) ==
                        TimePeriods.getNHours(timePeriodValue)) {

                        //is there an anomaly time which matches the centeredTimeValue?
                        anomalyActiveTimeOptions = (String[])anomalyDataSet.activeTimePeriodTimes.get(anomalyTimePeriodIndex);
                        anomalyCenteredTimeIndex = String2.indexOf(anomalyActiveTimeOptions, centeredTimeValue);
                        if (anomalyCenteredTimeIndex >= 0) {
                            anomalyAvailable = true;
                        } else {
                            if (oneOf.verbose()) String2.log("  No matching anomaly centeredTime.");
                        }
                    } else {
                        if (oneOf.verbose()) String2.log("  No matching anomaly time period.");
                    }
                } else {
                    if (oneOf.verbose()) String2.log("  Anomaly dataset (" + aID + ") isn't active.");
                }
            } else {
                if (oneOf.verbose()) String2.log("  gridDataSet has no corresponding anomalyDataSet.");
            }
            if (oneOf.verbose()) String2.log("  anomalyAvailable=" + anomalyAvailable);               

            //if user was viewing anomaly, but now none available...
            //defaultValue always cooresponds to viewOriginalData
            String viewOriginalData = view.getDefaultValue();
            if (view.getSelectedIndex(session) < 0) 
                view.setValue(session, viewOriginalData); //
            String viewValue = view.getValue(session); 
            viewAnomaly = !viewValue.equals(viewOriginalData);           
            if (viewAnomaly && !anomalyAvailable) {
                viewAnomaly = false;
                viewValue = viewOriginalData;
                view.setValue(session, viewValue);

                //similar changes in CWUser deal with most of these changes
                units.setValue(session, ""); 
                palette.setValue(session, ""); 
                paletteScale.setValue(session, ""); 
                paletteMin.setValue(session, "");
                paletteMax.setValue(session, "");
            }

            //show widget
            //Note that I used EmaSelect, not EmaBoolean (which Dave requested), 
            //because gridScreen allows user to click on map -- 
            //in which case the hidden information saying 'viewAnomaly' is selected is lost. 
            //Yes/No radio buttons are the next best thing.
            if (oneOf.verbose()) String2.log("  viewAnomaly=" + viewAnomaly);
            if (showEditOption == editOption && anomalyAvailable) {
                emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
                view.setLabel(String2.substitute(viewLabel, "" + (step.i++), null, null));
                //view.setRightLabel("(based on \"" + XML.encodeAsHTML(anomalyDataSet.option) + "\")");
                htmlSB.append(view.getTableEntry(viewValue, displayErrorMessages)); 
            } 

            //if viewAnomaly, switch anomaly into in place of original grid info
            //*** This is kind of a tricky thing to do.
            if (viewAnomaly) {
                dataSetValue            = anomalyDataSet.option;
                dataSetIndex            = anomalyDataSetIndex;
                gridDataSet             = anomalyDataSet;
                boldTitle               = anomalyDataSet.boldTitle;
                courtesy                = anomalyDataSet.courtesy;

                timePeriodIndex         = anomalyTimePeriodIndex;
                timePeriodValue         = anomalyTimePeriodValue;
                //timePeriodNHours is always identical

                //centeredTimeValue is always identical
                activeTimeOptions       = anomalyActiveTimeOptions; 
                centeredTimeIndex       = anomalyCenteredTimeIndex;
            }
        }

        //units
        unitsIndex   = -1;
        unitsValue   = null;
        altScaleFactor = 1;
        altOffset = 0;
        startCalendar = null;
        endCalendar = null;
        if (plotData) {
            //do after viewAnomaly determined...
            //startCalendar (for creation of WARNING in cwuser) is affected by timePeriod and centeredTime.
            //beginTime below is for timeSeries only.
            startCalendar = TimePeriods.getStartCalendar(timePeriodValue, centeredTimeValue, ""); //centeredTimeValue is clean (from file name)
            endCalendar   = TimePeriods.getEndCalendar(timePeriodValue, centeredTimeValue, "");
            //String2.log("  gridScreen startCalendar=" + Calendar2.formatAsISODateTimeT(startCalendar) +
            //            " endCalendar=" + Calendar2.formatAsISODateTimeT(endCalendar));

            String[] unitsOptions = gridDataSet.unitsOptions;
            units.setOptions(unitsOptions);
            unitsValue = units.getValue(session);
            unitsIndex = String2.indexOf(unitsOptions, unitsValue);
            if (unitsIndex < 0) {
                unitsIndex = gridDataSet.defaultUnits == 'A'? 1 : 0;
                unitsValue = gridDataSet.unitsOptions[unitsIndex];
                units.setValue(session, unitsValue);
                paletteMin.setValue(session, "");
                paletteMax.setValue(session, "");
            }
            if (oneOf.verbose()) String2.log("  unitsValue=" + unitsValue);
            if (showEditOption == editOption) {
                emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
                units.setLabel(String2.substitute(unitsLabel, "" + (step.i++), null, null));
                htmlSB.append(units.getTableEntry(unitsValue, displayErrorMessages));
            }
            if (unitsIndex > 0) {
                altScaleFactor = gridDataSet.altScaleFactor;
                altOffset = gridDataSet.altOffset;
            }       
        }

        //palette, paletteMin, paletteMax, paletteScale
        paletteValue       = palette.getValue(session);
        paletteScaleValue  = paletteScale.getValue(session);
        paletteMinValue    = paletteMin.getValue(session);
        paletteMaxValue    = paletteMax.getValue(session);
        fullDataSetCptName = null;
        if (plotData) {
            //ensure valid
            String defaultMin = "" + (unitsIndex <= 0? gridDataSet.paletteMin : gridDataSet.altPaletteMin); 
            String defaultMax = "" + (unitsIndex <= 0? gridDataSet.paletteMax : gridDataSet.altPaletteMax); 
            //String2.log("  initial palette=" + paletteValue + " scale=" + paletteScaleValue + " min=" + paletteMinValue);
            if (palette.isValid(paletteValue).length() > 0) paletteValue = gridDataSet.palette;
            if (paletteScale.isValid(paletteScaleValue).length() > 0) paletteScaleValue = gridDataSet.paletteScale;
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
            if (oneOf.verbose()) String2.log(
                   "  paletteValue=" + paletteValue + " paletteScale=" + paletteScaleValue +
                    " paletteMin=" + paletteMinValue + " paletteMax=" + paletteMaxValue);

            if (showEditOption == editOption) {
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
            fullDataSetCptName = CompoundColorMap.makeCPT(
                oneOf.fullPaletteDirectory(), 
                paletteValue, paletteScaleValue, 
                String2.parseDouble(paletteMinValue), 
                String2.parseDouble(paletteMaxValue), 
                -1, true, oneOf.fullPrivateDirectory());

        }

        //generate file names 
        //make file names easily parsed for specific values
        //FILE_NAME_RELATED_CODE
        startImageFileName = ""; 
        legendTime          = null;
        dataAccessAllowed   = false;
        String baseFileName = "";
        if (plotData) {
            baseFileName = FileNameUtility.makeBaseName(
                gridDataSet.internalName, //e.g., LATssta
                unitsIndex == 1? 'A' : 'S', //e.g., A=Alternate units S=Standard units
                timePeriodValue, //e.g., 1 day
                centeredTimeValue);         
            startImageFileName = "" + imageSizeIndex + cWESNString + 
                File2.getNameNoExtension(fullDataSetCptName); //hashCode'd below
            dataAccessAllowed = gridDataSet.dataAccessAllowedCentered(timePeriodValue, 
                centeredTimeValue);

            //make legendTime String 
            legendTime = gridDataSet.getLegendTime(timePeriodValue, centeredTimeValue);
            //String2.log("  legendTime=" + legendTime);
        } 

        //set info if plotBathymetryData  
        if (plotBathymetryData) {
            boldTitle = SgtMap.BATHYMETRY_BOLD_TITLE + " (" + SgtMap.BATHYMETRY_UNITS + ")";
            courtesy = SgtMap.BATHYMETRY_COURTESY;
            isClimatology = false;
            fullDataSetCptName = SgtMap.bathymetryCptFullName;
            baseFileName = SgtMap.BATHYMETRY_7NAME;         
            unitsValue = ""; //appended to boldTitle above
            startImageFileName = "" + imageSizeIndex + cWESNString + 
                File2.getNameNoExtension(fullDataSetCptName); //hashCode'd below
            dataAccessAllowed = true;
            legendTime = "";

            if (showEditOption == editOption)
                showGetBathymetryDataLinks(oneOf, htmlSB, rowNumber, step,
                    getLabel, currentMinX, currentMaxX, currentMinY, currentMaxY);
        }


        //display the grid's Get options
        if (plotData && showEditOption == editOption) {
            showGetGridDataLinks(oneOf, htmlSB, rowNumber, step,
                viewAnomaly? getAnomalyLabel : getLabel, 
                gridDataSet, timePeriodIndex, centeredTimeValue, 
                dataAccessAllowed,  
                currentMinX, currentMaxX, currentMinY, currentMaxY);
        }

        //timeSeriesLon and timeSeriesLat
        timeSeriesLonValue = "";
        timeSeriesLatValue = "";
        timeSeriesFileName = null;
        timeSeriesColorValue = null;
        timeSeriesLatD = Double.NaN;
        timeSeriesLonD = Double.NaN;
        beginTimeValue = null;
        if (plotData && !gridDataSet.isClimatology) {  //no time series for climatologies
            //timeSeriesLon and lat gui stuff
            timeSeriesLonD = String2.parseDouble(timeSeriesLon.getValue(session));
            if (!Double.isFinite(timeSeriesLonD) || timeSeriesLonD < currentMinX || timeSeriesLonD > currentMaxX)
                timeSeriesLonD = Double.NaN;
            timeSeriesLonValue = Double.isNaN(timeSeriesLonD)? "" : String2.genEFormat10(timeSeriesLonD);
            timeSeriesLon.setValue(session, timeSeriesLonValue);

            timeSeriesLatD = String2.parseDouble(timeSeriesLat.getValue(session));
            if (!Double.isFinite(timeSeriesLatD) || timeSeriesLatD < currentMinY || timeSeriesLatD > currentMaxY)
                timeSeriesLatD = Double.NaN;
            timeSeriesLatValue = Double.isNaN(timeSeriesLatD)? "" : String2.genEFormat10(timeSeriesLatD);
            timeSeriesLat.setValue(session, timeSeriesLatValue);

            if (showEditOption == editOption) {
                htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" +
                    "      <td>" + String2.substitute(timeSeriesLonLatLabel, "" + (step.i++), null, null) + "</td>\n" +
                    "      <td>\n" +
                    "        " + timeSeriesLonLabel + "\n" + 
                    "        " + timeSeriesLon.getControl(timeSeriesLonValue) + "\n" +
                    "        " + timeSeriesLatLabel + "\n" +
                    "        " + timeSeriesLat.getControl(timeSeriesLatValue) + "\n" +
                    "        " + timeSeriesLonLatOrLabel + "\n" +
                    "      </td>\n" +
                    "    </tr>\n");
            }

            //prepare the graphDataLayer
            if (!Double.isNaN(timeSeriesLonD) && !Double.isNaN(timeSeriesLatD)) {
                //FUTURE: it would be better if the lon lat used for the file
                //name were the lon lat of the closest grid point (not just the
                //lon lat the user specified. But that implies gridDataSets
                //know their grid ranges and spacings, and that implies all
                //files and timePeriods use the same grid info. Eeeek.
                //A mess. Better to just use user lon lat for now.

                //beginTime
                //limit options to centeredTime and before
                String[] beginTimeOptions = new String[centeredTimeIndex + 1];
                System.arraycopy(activeTimeOptions, 0, beginTimeOptions, 0, centeredTimeIndex + 1);
                beginTime.setOptions(beginTimeOptions);
                beginTimeValue = TimePeriods.validateBeginTime(beginTime.getValue(session),
                    centeredTimeValue, timePeriodValue);
                int beginTimeIndex = Calendar2.binaryFindClosest(beginTimeOptions, 
                    String2.replaceAll(beginTimeValue, "T", " "));
                beginTimeValue = beginTimeOptions[beginTimeIndex];
                beginTime.setValue(session, beginTimeValue);
                if (oneOf.verbose()) String2.log("  beginTimeValue=" + beginTimeValue);
                if (showEditOption == editOption) {
                    emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
                    beginTime.setLabel(String2.substitute(beginTimeLabel, "" + (step.i++), null, null));
                    htmlSB.append(beginTime.getTableEntry(beginTimeValue, displayErrorMessages));
                }

                //make the file name
                timeSeriesFileName = FileNameUtility.makeAveragedGridTimeSeriesName(
                    gridDataSet.internalName, unitsIndex == 1? 'A' : 'S',
                    timeSeriesLonD, timeSeriesLatD, beginTimeValue, centeredTimeValue, timePeriodValue);
                String fullTimeSeriesFileName = oneOf.fullPrivateDirectory() + timeSeriesFileName + ".nc";

                //make the table of timeSeries values and store in datafile
                boolean thereIsTimeSeriesData = false;
                Table timeSeriesTable = null;
                if (File2.touch(fullTimeSeriesFileName)) {
                    if (oneOf.verbose()) String2.log("  timeSeries reusing " + timeSeriesFileName + ".nc");
                    thereIsTimeSeriesData = true;
                    timeSeriesTable = new Table();
                    timeSeriesTable.readFlatNc(fullTimeSeriesFileName, null, 1); //standardizeWhat=1
                } else {
                    //unresolved problem with relying on existing file:
                    //  file for composite of current (e.g.) month would change hourly as new data arrives
                    //  but with re-use of file, new data won't be added.
                    //table has 6 columns (x, y, z, t, id, data)
                    if (oneOf.verbose()) String2.log("  timeSeries creating " + timeSeriesFileName + ".nc");
                    try {
                        timeSeriesTable = gridDataSet.getTimeSeries(
                            oneOf.fullPrivateDirectory(), timeSeriesLonD, timeSeriesLatD, 
                            beginTimeValue, centeredTimeValue, timePeriodValue);
                        //String2.log("  timeSeriesTable=" + timeSeriesTable);
                    } catch (Exception e) {
                        String2.log("Exception caught in GridScreen.validate make timeSeriesTable:\n" +
                            MustBe.throwableToString(e));
                        timeSeriesTable = null;
                    }
    
                    //has data? 
                    double dataStats[] = null;
                    if (timeSeriesTable != null && timeSeriesTable.nRows() > 0) {
                        dataStats = timeSeriesTable.getColumn(5).calculateStats();
                    }

                    if (dataStats != null && dataStats[PrimitiveArray.STATS_N] > 0) {  //not all NaN

                        //convert to alt units
                        if (unitsIndex == 1) {
                            timeSeriesTable.getColumn(5).scaleAddOffset(
                                gridDataSet.altScaleFactor, gridDataSet.altOffset);
                        }

                        //don't remove missing values
                        //1) they are used to mark gaps in the time series line on the graph
                        //2) if a user wants to combine two data files, side by side,
                        //    e.g., xWind and yWind, it only works if the time steps
                        //    are identical and without missing value rows removed

                        //save as .nc file   
                        //String2.log("  timeSeriesTable " + tTimeSeriesFileName + ":\n" + timeSeriesTable.toString(100));
                        timeSeriesTable.saveAsFlatNc(fullTimeSeriesFileName, "time");
                        thereIsTimeSeriesData = true;
                    }                        
                } 

                //set up the graphGDL 
                if (thereIsTimeSeriesData) {

                    //timeSeriesColor
                    timeSeriesColorValue = timeSeriesColor.getValue(session);
                    if (timeSeriesColor.isValid(timeSeriesColorValue).length() > 0) {
                        timeSeriesColorValue = timeSeriesColor.getDefaultValue();
                        timeSeriesColor.setValue(session, timeSeriesColorValue);
                    }
                    if (showEditOption == editOption) {
                        emaClass.setBeginRow(oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)));
                        timeSeriesColor.setLabel(String2.substitute(timeSeriesColorLabel, "" + (step.i++), null, null));
                        htmlSB.append(timeSeriesColor.getTableEntry(timeSeriesColorValue, displayErrorMessages));
                    }
                    if (oneOf.verbose()) 
                        String2.log("  timeSeriesColorValue=" + timeSeriesColorValue);

                    //oneOf.fullPrivateDirectory(), fileName + ".nc");
                    startImageFileName += timeSeriesFileName + timeSeriesColorValue; //will be hashCoded below
                    //GraphDataLayer was made here
                } else {
                    timeSeriesFileName = null;
                }

                //display the point's GetTS options
                if (showEditOption == editOption) {
                    String googleEarthLink = dataAccessAllowed? 
                        getGridGetLink(oneOf, "Google Earth", 
                            "Download a .kml file, suitable for Google Earth, with a time line for a series of grid images. (It may take a minute. Please be patient.)",
                            gridDataSet, timePeriodIndex, beginTimeValue, centeredTimeValue, 
                            currentMinX, currentMaxX, currentMinY, currentMaxY) : 
                        "";

                    showGetGridTimeSeriesDataLinks(
                        oneOf, session, htmlSB, rowNumber, step,
                        getTSLabel, gridDataSet, 
                        thereIsTimeSeriesData, dataAccessAllowed,
                        emaClass, gridDataSet.internalName, 
                        String2.parseDouble(timeSeriesLonValue),
                        String2.parseDouble(timeSeriesLatValue),
                        beginTimeValue, centeredTimeValue, timePeriodValue,
                        googleEarthLink);
                }
            }
        }

        if (plotData || plotBathymetryData) {
            startImageFileName = baseFileName + "_" +
                String2.md5Hex12(startImageFileName); //will be same for same info
        } else { //!plotData      
            startImageFileName = imageSizeIndex + cWESNString;
        }
    }

    /**
     * This returns the graph (time series) GraphDataLayer (or null).
     * This is made as needed so garbage collected after used.
     *
     * @return the graph GraphDataLayer (or null)
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
                GraphDataLayer.DRAW_MARKERS_AND_LINES, true, false,
                "Time", unitsValue,
                gridDataSet.boldTitle, 
                "Time series of " + 
                    (timePeriodNHours == 0? "raw data. " :
                        timePeriodValue + " averages. ") + 
                    String2.genEFormat10(timeSeriesLatD) + " N, " + 
                    String2.genEFormat10(timeSeriesLonD) + " E. (Horizontal line = average)", //title2
                gridDataSet.courtesy.length() == 0? null : 
                    "Data courtesy of " + gridDataSet.courtesy,
                null, //title4
                timeSeriesTable, null, null,
                null,
                new Color(String2.parseInt("0x" + timeSeriesColorValue)),
                GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_SMALL, 
                0, //standardVector
                GraphDataLayer.REGRESS_MEAN);
        } catch (Exception e) {
            String error = "Exception caught in GridScreen.getGraphGDL:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), error);

            return null;            
        }
    }

    /**
     * This appends a row to the table so the user can choose to download 
     * bathymetry data.
     * It is used by this class and contourScreen.
     *
     */
    public static void showGetBathymetryDataLinks(OneOf oneOf, 
        StringBuilder htmlSB, IntObject rowNumber, IntObject step,
        String getLabel, 
        double minX, double maxX, double minY, double maxY) {

        htmlSB.append(
            "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
            "      <td>" + 
                String2.substitute(getLabel, "" + (step.i++), null, null) + 
                "</td>\n" +
            "      <td>\n");

        boolean needsPipe = true;
        boolean hasNoBr = false;

        String tUrl = "        <a href=\"" + oneOf.url() + 
            "?get=bathymetryData" +
            "&amp;minLon=" + String2.genEFormat10(minX) + 
            "&amp;maxLon=" + String2.genEFormat10(maxX) + 
            "&amp;minLat=" + String2.genEFormat10(minY) + 
            "&amp;maxLat=" + String2.genEFormat10(maxY) +
            "&amp;fileType="; //.nc" + "\">.nc</a>"

        String[] bathymetryGetOptions = oneOf.bathymetryGetOptions();
        String[] bathymetryGetTitles  = oneOf.bathymetryGetTitles();
        for (int i = 0; i < bathymetryGetOptions.length; i++) {
            //show non-gif and .png options   (but during development show gif options, too)
            //These options are useful in HTTP GET queries, but here they are misleading 
            //  because images don't use specified palette information
            if (oneOf.displayDiagnosticInfo() || 
                (bathymetryGetOptions[i].indexOf(".gif") < 0 && bathymetryGetOptions[i].indexOf(".png") < 0)) {
                htmlSB.append(
                    tUrl + Browser.clean(bathymetryGetOptions[i]) + "\" title=\"" + bathymetryGetTitles[i] + 
                    "\">" + bathymetryGetOptions[i]+ "</a> " + 
                    (i == bathymetryGetOptions.length - 1? " &nbsp; &nbsp; " : "|") + 
                    "\n");
            }
        }

        //file type help link
        htmlSB.append("        <span style=\"white-space:nowrap;\">\n"); 
        htmlSB.append(oneOf.gridFileHelpLink());

        //GETQuery help link
        htmlSB.append("        |\n");
        htmlSB.append(oneOf.GETQueryHelpLink());

        //give link to source
        htmlSB.append( 
            "        |\n" +
            "        <a href=\"" + SgtMap.BATHYMETRY_SOURCE_URL + "\"\n" +
            "          title=\"The source of the ETOPO2v2 bathymetry data.\">Info</a>\n");

        htmlSB.append(
            "        </span>\n" +
            "      </td>\n" +
            "    </tr>\n");
    }

    /**
     * This appends a row to the table so the user can choose to download 
     * grid data.
     * It is used by this class and contourScreen.
     *
     * @param beginTimeValue is only used by GoogleEarth link.
     *    Use null if not available.
     *
     */
    public static void showGetGridDataLinks(OneOf oneOf,  
        StringBuilder htmlSB, IntObject rowNumber, IntObject step,
        String getLabel, GridDataSet gridDataSet, int timePeriodIndex, 
        String centeredTimeValue,
        boolean dataAccessAllowed, 
        double minX, double maxX, double minY, double maxY) {


        htmlSB.append(
            "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
            "      <td>" + 
                String2.substitute(getLabel, "" + (step.i++), null, null) + 
                "</td>\n" +
            "      <td>\n");

        boolean needsPipe = true;
        boolean hasNoBr = false;
        if (dataAccessAllowed) {

            String[] gridGetOptions = oneOf.gridGetOptions();
            String[] gridGetTitles  = oneOf.gridGetTitles();
            for (int i = 0; i < gridGetOptions.length; i++) {

                //show non-gif and .png options   (but during development show gif options, too)
                //These options are useful in HTTP GET queries, but here they are misleading 
                //  because images don't use specified palette information
                if (oneOf.displayDiagnosticInfo() || 
                    (gridGetOptions[i].indexOf(".gif") < 0 && gridGetOptions[i].indexOf(".png") < 0)) {
                    if (gridGetOptions[i].equals(".tif") && "Log".equals(gridDataSet.paletteScale)) { 
                        //don't show geotiff option for log datasets
                    } else {
                        htmlSB.append(
                            getGridGetLink(oneOf, gridGetOptions[i], gridGetTitles[i], 
                                gridDataSet, timePeriodIndex, null, centeredTimeValue,
                                minX, maxX, minY, maxY) +
                            (i == gridGetOptions.length - 1? " &nbsp; &nbsp; " : "|") + 
                            "\n");
                    }
                }
            }

            //file type help link
            hasNoBr = true;
            htmlSB.append("        <span style=\"white-space:nowrap;\">\n"); 
            htmlSB.append(oneOf.gridFileHelpLink());

            //GETQuery help link
            htmlSB.append("        |\n");
            htmlSB.append(oneOf.GETQueryHelpLink());

            //give link to OPeNDAP
            String tUrl = gridDataSet.activeTimePeriodOpendapUrls[timePeriodIndex];
            if (tUrl != null) {
                htmlSB.append( 
                    "        |\n" +
                    "        <a href=\"" + oneOf.makePublicUrl(tUrl) + ".html\"\n" +
                    "          title=\"Download the selected data or related data via an OPeNDAP server.\">OPeNDAP</a>\n");
            }

        } else {
            htmlSB.append("        " + oneOf.dataAccessNotAllowed() + "\n");
        }

        //data set help link
        String infoUrl = oneOf.getInternalNameInfoUrl(gridDataSet.internalName);
        if (needsPipe && infoUrl.length() > 0)
            htmlSB.append("        |\n");
        htmlSB.append(infoUrl);
        if (hasNoBr)
            htmlSB.append("        </span>\n");

        htmlSB.append(  
             "      </td>\n" +
             "    </tr>\n");
    }

    /** This generates the actual Grid Get links. 
     * beginTimeValue is only used for Google Earth's option, and only used if not null.
     */
    private static String getGridGetLink(OneOf oneOf, 
        String getOption, String getTitle, GridDataSet gridDataSet, int timePeriodIndex, 
        String beginTimeValue, String centeredTimeValue,
        double minX, double maxX, double minY, double maxY) {

        String tBeginTime = beginTimeValue;
        if (tBeginTime != null) 
            tBeginTime = "&amp;beginTime=" + String2.replaceAll(beginTimeValue, ' ', 'T'); //so connector is 'T'

        //show non-gif and .png options   (but during development show gif options, too)
        //These options are useful in HTTP GET queries, but here they are misleading 
        //  because images don't use specified palette information
        return
            "        <a href=\"" + oneOf.url() + 
            "?get=gridData&amp;dataSet=" + gridDataSet.internalName +
            "&amp;timePeriod=" + Browser.clean(gridDataSet.activeTimePeriodOptions[timePeriodIndex]) +
            (getOption.indexOf("Google") >= 0 && tBeginTime != null? tBeginTime : "") +
            "&amp;centeredTime=" + String2.replaceAll(centeredTimeValue, ' ', 'T') + //so connector is 'T'
            "&amp;minLon=" + String2.genEFormat10(minX) + 
            "&amp;maxLon=" + String2.genEFormat10(maxX) + 
            "&amp;minLat=" + String2.genEFormat10(minY) + 
            "&amp;maxLat=" + String2.genEFormat10(maxY) +
            "&amp;fileType=" + //.nc" + "\">.nc</a>"
            Browser.clean(getOption) + "\" title=\"" + getTitle + 
            "\">" + getOption + "</a> ";
    }

    /**
     * This appends a row to the table so the user can choose to download 
     * grid time series point data.
     * It is used by this gridScreen.
     *
     * @param googleLink is an additional link for Google Earth (or null)
     */
    public static void showGetGridTimeSeriesDataLinks(
        OneOf oneOf, HttpSession session, 
        StringBuilder htmlSB, IntObject rowNumber, IntObject step,
        String getLabel, GridDataSet gridDataSet, boolean tableHasData, 
        boolean dataAccessAllowed,   EmaClass emaClass, 
        String dataSetInternalName, double x, double y, 
        String beginTimeValue, String endTimeValue, String timePeriodValue,
        String googleLink) {

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
                "?get=gridTimeSeries" +
                "&amp;dataSet=" + dataSetInternalName +
                "&amp;timePeriod=" + Browser.clean(timePeriodValue) +
                "&amp;beginTime=" + String2.replaceAll(beginTimeValue, ' ', 'T') + //so connector is 'T'
                "&amp;endTime="   + String2.replaceAll(endTimeValue,   ' ', 'T') + //so connector is 'T'
                "&amp;lon=" + String2.genEFormat10(x) + 
                "&amp;lat=" + String2.genEFormat10(y) +
                "&amp;fileType="; //.nc" + "\">.nc</a>"

            String[] getOptions = oneOf.gridGetTSOptions();
            String[] getTitles  = oneOf.gridGetTSTitles();
            for (int i = 0; i < getOptions.length; i++) 
                htmlSB.append(
                    tUrl + Browser.clean(getOptions[i]) + "\" title=\"" + getTitles[i] + 
                    "\">" + getOptions[i]+ "</a> " + 
                    (i == getOptions.length - 1? "" : "|") + 
                    "\n");
            if (googleLink != null) htmlSB.append(" | " + googleLink + "\n");
            htmlSB.append(" &nbsp; &nbsp; ");


            //file type help link
            hasNoBr = true;
            htmlSB.append("        <span style=\"white-space:nowrap;\">\n"); 
            htmlSB.append(oneOf.pointFileHelpLink());

            //GETQuery help link
            htmlSB.append("        |\n");
            htmlSB.append(oneOf.GETQueryHelpLink());

            //opendap link
            int timePeriodIndex = String2.indexOf(gridDataSet.activeTimePeriodOptions, timePeriodValue);
            tUrl = gridDataSet.activeTimePeriodOpendapUrls[timePeriodIndex];
            if ( tUrl != null) {
                htmlSB.append( 
                    "        |\n" +
                    "        <a href=\"" + oneOf.makePublicUrl(tUrl) + ".html\"\n" +
                    "  title=\"Download the selected data or related data via an OPeNDAP server.\">OPeNDAP</a>\n");
            }


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
            double minLat, double maxLat, int imageWidth, int imageHeight) 
            throws Exception { 
        try {
            if (plotData)
                return gridDataSet.makeGrid(timePeriodValue, centeredTimeValue, 
                    minLon, maxLon, minLat, maxLat, imageWidth, imageHeight);
            else if (plotBathymetryData) 
                return SgtMap.createTopographyGrid(oneOf.fullPrivateDirectory(),
                    minLon, maxLon, minLat, maxLat, imageWidth, imageHeight);
            else return null;
        } catch (Exception e) {
            String error = "Exception caught in GridScreen.getGrid:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            if (error.indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                oneOf.email(oneOf.emailEverythingTo(), 
                    String2.ERROR + " in " + oneOf.shortClassName(), error);
            plotBathymetryData = false;
            plotData = false;
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

        //error
        Test.error(String2.ERROR + 
            " in GridScreen.respondToSubmitter: Unexpected submitter: " + 
            submitter);

    }



}
