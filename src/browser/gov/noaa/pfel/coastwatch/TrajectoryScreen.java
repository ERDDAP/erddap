/* 
 * TrajectoryScreen Copyright 2007, NOAA.
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
import gov.noaa.pfel.coastwatch.sgt.MonoColorMap;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.IntObject;

import gov.noaa.pmel.sgt.ColorMap;

import java.awt.Color;
import java.util.GregorianCalendar;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for a Trajectory screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Trajectory Data").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-03-12
 */
public class TrajectoryScreen extends Screen {

    public boolean verbose = true;
    int whichTrajectoryScreen1; //1..OneOf.nTrajectoryScreens()

    public EmaSelect  dataSet, individual, colorBar, xAxis, yAxis;
    public EmaDateTimeText2 beginTime, endTime;

    //set by the constructor
    public String dataSetLabel, individualLabel, beginTimeLabel, endTimeLabel, 
        graphLabel, colorBarLabel, xAxisLabel, yAxisLabel, getSelectedLabel, getAllLabel;
    public Color defaultLineColor;

    //set by validate
    public boolean plotData;
    public String dataSetValue;
    public TableDataSet tableDataSet;

    public String individualValue;

    public String beginTimeValue, endTimeValue;
    public GregorianCalendar beginCalendar, endCalendar;

    public String colorBarValue;
    public String xAxisValue;
    public String yAxisValue;
    public String fullDataSetCptName;

    public String addToImageFileName;
    public String selectedFileName; //doesn't have directory at start or .nc at end; may be null
    public String allFileName; //no directory at start or .nc at end; may be null
    public String legendTime;
    public boolean dataAccessAllowed;


    /** 
     * The constructor for TrajectoryScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param whichTrajectoryScreen1 1..OneOf.nTrajectoryScreens()
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public TrajectoryScreen(int editOption, int whichTrajectoryScreen1, OneOf oneOf, Shared shared, 
            EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.whichTrajectoryScreen1 = whichTrajectoryScreen1;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in TrajectoryScreen(" + whichTrajectoryScreen1 + "): ";
        verbose = oneOf.verbose();
        if (verbose) String2.log("create TrajectoryScreen" + whichTrajectoryScreen1);

        //add the components to the emaClass
        emaClass.addAttribute(dataSet         = new EmaSelect(emaClass, "trajectoryDataSet" + whichTrajectoryScreen1)); 
        emaClass.addAttribute(individual      = new EmaSelect(emaClass, "trajectoryIndividual" + whichTrajectoryScreen1));
        emaClass.addAttribute(beginTime       = new EmaDateTimeText2(emaClass, "trajectoryBeginTime" + whichTrajectoryScreen1));
        emaClass.addAttribute(endTime         = new EmaDateTimeText2(emaClass, "trajectoryEndTime" + whichTrajectoryScreen1));
        emaClass.addAttribute(colorBar        = new EmaSelect(emaClass, "trajectoryColorBar" + whichTrajectoryScreen1));
        emaClass.addAttribute(xAxis           = new EmaSelect(emaClass, "trajectoryXAxis" + whichTrajectoryScreen1));
        emaClass.addAttribute(yAxis           = new EmaSelect(emaClass, "trajectoryYAxis" + whichTrajectoryScreen1));

        //get all the labels 
        dataSetLabel      = classRB2.getNotNullString("trajectoryDataSet" + whichTrajectoryScreen1 + ".label", errorInMethod);
        individualLabel   = classRB2.getNotNullString("trajectoryIndividual" + whichTrajectoryScreen1 + ".label", errorInMethod);
        beginTimeLabel    = classRB2.getNotNullString("trajectoryBeginTime" + whichTrajectoryScreen1 + ".label", errorInMethod);
        endTimeLabel      = classRB2.getNotNullString("trajectoryEndTime" + whichTrajectoryScreen1 + ".label", errorInMethod);
        colorBarLabel     = classRB2.getNotNullString("trajectoryColorBar" + whichTrajectoryScreen1 + ".label", errorInMethod);
        xAxisLabel        = classRB2.getNotNullString("trajectoryXAxis" + whichTrajectoryScreen1 + ".label", errorInMethod);
        yAxisLabel        = classRB2.getNotNullString("trajectoryYAxis" + whichTrajectoryScreen1 + ".label", errorInMethod);
        //no Ema classes for these, so nothing stored with different screens, so no need for whichTrajectoryScreen1
        graphLabel        = classRB2.getNotNullString("trajectoryGraph.label", errorInMethod);
        getSelectedLabel  = classRB2.getNotNullString("trajectoryGetSelected.label", errorInMethod);
        getAllLabel       = classRB2.getNotNullString("trajectoryGetAll.label", errorInMethod);

        //get the defaultLineColor
        String tLineColors = classRB2.getNotNullString("trajectoryLineColors", errorInMethod);
        String tLineColorsAr[] = String2.split(tLineColors, '`');
        int colorIndex = String2.indexOf(TableDataSet.COLOR_NAMES, tLineColorsAr[whichTrajectoryScreen1 - 1]);
        Test.ensureTrue(colorIndex >= 0, errorInMethod + "unknown trajectoryLineColor=" +
            tLineColorsAr[whichTrajectoryScreen1 - 1]);
        defaultLineColor = TableDataSet.COLORS[colorIndex];

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
        if (verbose) String2.log("trajectoryScreen" + whichTrajectoryScreen1 + ".validate showEditOption=" + showThisScreen);

        //dataSet
        dataSet.setOptions(shared.activeTrajectoryDataSetOptions()); 
        dataSetValue = dataSet.getValue(session);
        int dataSetIndex = String2.indexOf(shared.activeTrajectoryDataSetOptions(), dataSetValue);
        if (dataSetIndex < 0) {
            if (dataSetIndex < 0)  //check for 7char name
                dataSetIndex = String2.indexOf(shared.activeTrajectoryDataSet7Names(), dataSetValue);
            if (dataSetIndex < 0)
                dataSetIndex = 0; //none
            dataSetValue = shared.activeTrajectoryDataSetOptions()[dataSetIndex];
            dataSet.setValue(session, dataSetValue);
        }
        if (verbose) String2.log("  dataSetValue = " + dataSetValue);
        plotData = dataSetIndex > 0; 
        if (doTally && plotData)   //if allow None, it appears once for each TrajectoryScreen
            oneOf.tally().add("Trajectory Data Set Usage:", dataSetValue);
        if (showThisScreen) 
            addTableEntry(dataSet, dataSetLabel, dataSetValue, rowNumber, step, htmlSB);

        //individual
        tableDataSet = null;
        individualValue = null;
        dataAccessAllowed = false;
        if (plotData) {
            tableDataSet = (TableDataSet)shared.activeTrajectoryDataSets().get(dataSetIndex);
            individualValue = individual.setOptionsAndValidate(tableDataSet.individuals(), session, 0);
            if (verbose) String2.log("  individualValue = " + individualValue);
            if (showThisScreen) 
                addTableEntry(individual, individualLabel, individualValue, rowNumber, step, htmlSB);

            //for trajectory, if data access isn't allowed, don't even graph the data
            dataAccessAllowed = OneOf.dataAccessAllowed(
                tableDataSet.daysTillDataAccessAllowed(individualValue),
                Calendar2.getCurrentISODateStringZulu());
            if (!dataAccessAllowed) {
                plotData = false;
                if (showThisScreen) htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" +
                    "      <td>" + String2.substitute(getSelectedLabel, "" + (step.i++), null, null) + "</td>\n" +
                    "      <td>" + oneOf.dataAccessNotAllowed() + "</td>\n" +
                    "    </tr>\n");
            }
        }

        //colorBar
        colorBarValue = null;
        if (plotData) {
            colorBarValue = colorBar.setOptionsAndValidate(tableDataSet.longNamesAndColors(), 
                session, 3);  //3=time
            if (verbose) String2.log(" colorBarValue=" + colorBarValue);
            if (showThisScreen) 
                addTableEntry(colorBar, colorBarLabel, colorBarValue, rowNumber, step, htmlSB);
        }

        //xAxis, yAxis
        xAxisValue = null;
        yAxisValue = null;
        if (plotData) {
            xAxisValue = xAxis.setOptionsAndValidate(tableDataSet.longNames(), session, 3); //3=time
            yAxisValue = yAxis.setOptionsAndValidate(tableDataSet.longNames(), session, 
                tableDataSet.dataVariableNames().length > 0? 4 : 2); //4=first data variable; 2=depth (~goofy, but what to do if not data vars)?
            if (verbose) String2.log("  xAxisValue=" + xAxisValue +
                " yAxisValue=" + yAxisValue);
            if (showThisScreen) {
                htmlSB.append(
                    "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" +
                    "      <td>" + String2.substitute(graphLabel, "" + (step.i++), null, null) + "</td>\n" +
                    "      <td>\n" +
                    "        <table class=\"erd\" style=\"width:2%;\">\n" + //padding=0
                    "          <tr>\n" +
                    "            <td>" + xAxis.getLabel() + "</td>\n" +
                    "            <td>" + xAxis.getControl(xAxisValue) + "</td>\n" + 
                    "            <td>&nbsp;" + yAxis.getLabel() + "</td>\n" +
                    "            <td>" + yAxis.getControl(yAxisValue) + "</td>\n" + 
                    "          </tr>\n" +
                    "        </table>\n" +
                    "      </td>\n" +
                    "    </tr>\n");
            }
        }


/*
        //beginTime endTime  
        beginTimeValue = null;
        endTimeValue = null;
        beginCalendar = null;
        endCalendar = null;
        if (plotData) {
            endTime.setMinMaxDateTime(tableDataSet.firstTime, tableDataSet.lastTime);
            
            //setShow before findClosestTime since it affects formatting (used by findClosestTime)
            endTime.setShow(TimePeriods.PICK_FROM[tExactXAxis]);
            endTime.setIncrement(TimePeriods.INCREMENT[tExactXAxis]);
            endTimeValue = endTime.findClosestDate(endTime.getValue(session)); //ensure in range, not null
            endTimeValue = TimePeriods.getCleanEndTime(xAxisValue, endTimeValue); //clean it
            endTimeValue = endTime.findClosestDate(endTimeValue); //ensure in range, "" -> last value
            endTime.setValue(session, endTimeValue);
            if (verbose) String2.log("  endTimeValue = " + endTimeValue);
            if (showThisScreen)
                addTableEntry(endTime, endTimeLabel, endTimeValue, rowNumber, step, htmlSB);

            //beginCalendar (for creation of WARNING in cwuser) is affected by xAxis and endTime.
            //beginTime below is for timeSeries only.
            beginCalendar = TimePeriods.getStartCalendar(xAxisValue, endTimeValue, "");
            endCalendar   = TimePeriods.getEndCalendar(xAxisValue, endTimeValue, "");
        }

        legendTime = null;
        if (plotData) {
            //make dateTime String 
            legendTime = TimePeriods.getLegendTime(xAxisValue, endTimeValue);
        }
*/

        addToImageFileName = "";  //append stuff, then hash at end
        if (plotData) {
            addToImageFileName = 
                dataSetValue + individualValue +
                //beginTimeValue, endTimeValue;
                colorBarValue + xAxisValue + yAxisValue;
            addToImageFileName = "_T" + whichTrajectoryScreen1 + 
                String2.md5Hex12(addToImageFileName); // will be same for same info             
        }

        //display the trajectory's Get data file links
        if (plotData && showThisScreen) {

            //selected data columns
            showGetTrajectoryDataLinks(
                "trajectoryData", 
                oneOf.trajectoryGetSelectedOptions(), oneOf.trajectoryGetSelectedTitles(),
                oneOf, session, htmlSB, rowNumber, step,
                getSelectedLabel, dataAccessAllowed, 
                emaClass, tableDataSet, 
                individualValue, false, //getAll
                colorBarValue, xAxisValue, yAxisValue);

            //all data columns
            showGetTrajectoryDataLinks(
                "trajectoryData", 
                oneOf.trajectoryGetAllOptions(), oneOf.trajectoryGetAllTitles(),
                oneOf, session, htmlSB, rowNumber, step,
                getAllLabel, dataAccessAllowed,
                emaClass, tableDataSet, 
                individualValue, true, //getSelected
                colorBarValue, xAxisValue, yAxisValue);
        }

    }

    /**
     * This returns the GraphDataLayer for the map (or null).
     * This is made as needed so garbage collected after used.
     *
     * @return the map GraphDataLayer or null if trouble (this won't throw an exception)
     */
    public GraphDataLayer getMapGDL() {
        if (!plotData)
            return null;

        try {
            String tXAxisValue = DataHelper.TABLE_LONG_NAMES[0]; //lon
            String tYAxisValue = DataHelper.TABLE_LONG_NAMES[1]; //lat
            int xColumnNumber = 0;
            int yColumnNumber = 1;
            int cColumnNumber = 3; //for now, always time
            int idColumnNumber = 4;

            //collect desired DATA VariableNames  (not interested in coordinate variables or colors)
            String[] dataVariableNames = tableDataSet.dataVariableNames();
            String[] dataLongNames = tableDataSet.dataLongNames();
            StringArray tDataVariables = new StringArray();
            int dataC = String2.indexOf(dataLongNames, colorBarValue);
            if (dataC >= 0) tDataVariables.add(dataVariableNames[dataC]);  //<0 is color

            //make the table
            Table table = tableDataSet.makeSubset(
                "0001-01-01", "3000-01-01", //data from all time
                new String[]{individualValue},
                tDataVariables.toArray());
            //String2.log(table.toString(10));

            //make the colorMap
            Color color = null;
            ColorMap colorMap = null;
            int colorIndex = String2.indexOf(tableDataSet.colorNames(), colorBarValue);
            String addToBoldTitle = "";
            if (colorIndex >= 0) {
                //colorBarValue is plain color
                color = tableDataSet.colors()[colorIndex];

                //make monoColorMap
                //colorMap = new MonoColorMap(oneOf.sgtMap().oceanColor);
                //cColumnNumber = 0; //has to point to a variable, doesn't matter which as long as valid values

            } else {
                //colorBarValue is a numeric variable, so make a colorMap
                String cVariableName = tableDataSet.convertLongNameToVariableName(colorBarValue);
                cColumnNumber = table.findColumnNumber(cVariableName);
                if (cColumnNumber < 0)
                    Test.error("cVariableName=" + cVariableName + " not in table:" +
                        String2.toCSSVString(table.getColumnNames()));
                PrimitiveArray colorCol = table.getColumn(cColumnNumber);
                double cStats[] = colorCol.calculateStats();
                double cMin = cStats[PrimitiveArray.STATS_MIN];
                double cMax = cStats[PrimitiveArray.STATS_MAX];
                double cRange[] = SgtUtil.suggestPaletteRange(cMin, cMax);
                int longNameIndex = String2.indexOf(tableDataSet.longNames(), colorBarValue);
                if (Double.isNaN(cMin)) {
                    addToBoldTitle = ", " + colorBarValue + " (No Data)";
                } else if (colorBarValue.equals(DataHelper.TABLE_LONG_NAMES[3])) { 
                    //Time
                    colorMap = new CompoundColorMap(oneOf.fullPaletteDirectory(), 
                        "Rainbow", false, //dataIsMillis, 
                         cMin, cMax, 7, false, //atLeastNPieces, continuous, 
                         oneOf.fullPrivateDirectory());
                    addToBoldTitle = ", Time";
                } else {
                    //non-time 
                    colorMap = new CompoundColorMap(oneOf.fullPaletteDirectory(), 
                        SgtUtil.suggestPalette(cMin, cMax), 
                        SgtUtil.suggestPaletteScale(cMin, cMax), 
                        cRange[0], cRange[1], 
                        -1, true, oneOf.fullPrivateDirectory());
                    addToBoldTitle = ", " + colorBarValue + 
                        " (" + tableDataSet.units(longNameIndex) + ")";
                }
                color = defaultLineColor;
            }

            //make the graphDataLayer
            //String2.log("TrajectoryScreen color=0x" + Integer.toHexString(color.getRGB()));
            return new GraphDataLayer(
                editOption, //sourceID
                xColumnNumber, yColumnNumber, cColumnNumber, idColumnNumber, -1, //column #'s for variables
                GraphDataLayer.DRAW_MARKERS_AND_LINES, //will draw either varying color or solid color
                false, false, //x,yIsTimeAxis
                null, null, //x,yAxisTitle
                tableDataSet.datasetName() + ", " + individualValue + addToBoldTitle, //bold title
                "", //title2
                tableDataSet.courtesy().length() == 0? null : "Data courtesy of " + tableDataSet.courtesy(), 
                null, //title4
                table, null, null,
                colorMap, color,
                GraphDataLayer.MARKER_TYPE_FILLED_SQUARE, 
                GraphDataLayer.MARKER_SIZE_MEDIUM,
                0, //standardVector
                GraphDataLayer.REGRESS_NONE);
      
        } catch (Exception e) {
            String error = "Exception caught in TrajectoryScreen" + whichTrajectoryScreen1 + 
                ".getMapGDL:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), error);

            return null;            
        }

    }

    /**
     * This returns the GraphDataLayer for a graph (or null).
     * This is made as needed so garbage collected after used.
     *
     * @return the graph GraphDataLayer or null if trouble (this won't throw an exception)
     */
    public GraphDataLayer getGraphGDL() {

        if (!plotData)
            return null;

        try {
            //if x=lon y=lat, no need for separate graph
            if (xAxisValue.equals(DataHelper.TABLE_LONG_NAMES[0]) &&
                yAxisValue.equals(DataHelper.TABLE_LONG_NAMES[1])) {
                if (verbose) String2.log("TrajectoryScreen.getGraphGDL: x=Lon and y=Lat, so no separate graph.");
                return null;
            }

            //collect desiredDataVariableNames  (not interested in coordinate variables or colors)
            String[] dataVariableNames = tableDataSet.dataVariableNames();
            String[] dataLongNames = tableDataSet.dataLongNames();
            StringArray tDataVariables = new StringArray();
            int dataX = String2.indexOf(dataLongNames, xAxisValue);
            int dataY = String2.indexOf(dataLongNames, yAxisValue);
            //int dataC = String2.indexOf(dataLongNames, colorBarValue);
            if (dataX >= 0) tDataVariables.add(dataVariableNames[dataX]);
            if (dataY >= 0 && dataY != dataX) tDataVariables.add(dataVariableNames[dataY]);
            //if (dataC >= 0 && dataC != dataY && dataC != dataX) tDataVariables.add(dataVariableNames[dataC]);

            //make the table
            Table table = tableDataSet.makeSubset(
                "0001-01-01", "3000-01-01", //data from all time
                new String[]{individualValue},
                tDataVariables.toArray());
            String xVariableName = tableDataSet.convertLongNameToVariableName(xAxisValue);
            String yVariableName = tableDataSet.convertLongNameToVariableName(yAxisValue);
            int xColumnNumber = table.findColumnNumber(xVariableName);
            int yColumnNumber = table.findColumnNumber(yVariableName);
            int cColumnNumber = -1; //may be adjusted below
            int idColumnNumber = 4;
            if (xColumnNumber < 0)
                Test.error("xVariableName=" + xVariableName + " not in table:" +
                    String2.toCSSVString(table.getColumnNames()));
            if (yColumnNumber < 0)
                Test.error("yVariableName=" + yVariableName + " not in table:" +
                    String2.toCSSVString(table.getColumnNames()));
            //String2.log(table.toString(10));

            //make colorMap 
            Color color = defaultLineColor;
            ColorMap colorMap = null;
            /*
            Color color = null;
            int colorIndex = String2.indexOf(tableDataSet.colorNames(), colorBarValue);
            if (colorIndex >= 0) {
                //colorBarValue is plain color
                color = tableDataSet.colors()[colorIndex];
            } else {
                //colorBarValue is a numeric variable
                String cVariableName = tableDataSet.convertLongNameToVariableName(colorBarValue);
                cColumnNumber = table.findColumnNumber(cVariableName);
                if (cColumnNumber < 0)
                    Test.error("cVariableName=" + cVariableName + " not in table:" +
                        String2.toCSSVString(table.getColumnNames()));
                PrimitiveArray colorCol = table.getColumn(cColumnNumber);
                double cStats[] = colorCol.calculateStats();
                double cMin = cStats[PrimitiveArray.STATS_MIN];
                double cMax = cStats[PrimitiveArray.STATS_MAX];
                double cRange[] = SgtUtil.suggestPaletteRange(cMin, cMax);
                colorMap = new CompoundColorMap(
                    oneOf.fullPaletteDirectory(), 
                    SgtUtil.suggestPalette(cMin, cMax), 
                    SgtUtil.suggestPaletteScale(cMin, cMax), 
                    cRange[0], cRange[1], 
                    -1, true, oneOf.fullPrivateDirectory());
                color = tableDataSet.colors()[
                    String2.indexOf(tableDataSet.colorNames(), colorBar.getDefaultValue())];
            }
            */

            //make the graphDataLayer
            int lnx = String2.indexOf(tableDataSet.longNames(), xAxisValue);
            int lny = String2.indexOf(tableDataSet.longNames(), yAxisValue);
            return new GraphDataLayer(
                editOption, //sourceID
                xColumnNumber, yColumnNumber, cColumnNumber, idColumnNumber, -1, //column #'s for variables
                GraphDataLayer.DRAW_LINES,
                xColumnNumber == 3, yColumnNumber == 3, //x,yIsTimeAxis
                xAxisValue + (xColumnNumber == 3? "" : " " + tableDataSet.formattedUnits(lnx)), //xAxisTitle
                yAxisValue + (yColumnNumber == 3? "" : " " + tableDataSet.formattedUnits(lny)), //yAxisTitle
                tableDataSet.datasetName() + ", " + individualValue, //bold title
                "", //title2
                tableDataSet.courtesy().length() == 0? null : "Data courtesy of " + tableDataSet.courtesy(), 
                null, //title4
                table, null, null,
                colorMap, color,
                GraphDataLayer.MARKER_TYPE_FILLED_SQUARE, GraphDataLayer.MARKER_SIZE_SMALL, 
                0, //standardVector
                GraphDataLayer.REGRESS_NONE);
      
        } catch (Exception e) {
            String error = "Exception caught in TrajectoryScreen" + whichTrajectoryScreen1 + 
                ".getGraphGDL:\n" + 
                MustBe.throwableToString(e);
            String2.log(error);
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), error);

            return null;            
        }

    }

    /**
     * This appends a row to the table so the user can choose to download 
     * trajectory data.
     * It is used by this class, gridScreen, and vectorTrajectoryScreen.
     *
     */
    public static void showGetTrajectoryDataLinks(
        String getWhat, String[] getOptions, String[] getTitles,
        OneOf oneOf, HttpSession session, 
        StringBuilder htmlSB, IntObject rowNumber, IntObject step,
        String getLabel, boolean dataAccessAllowed,
        EmaClass emaClass, TableDataSet tableDataSet, 
        String individualValue, 
        boolean getAll, //vs. getSelected
        String colorBarValue, String xAxisValue, String yAxisValue) {

        htmlSB.append(
            "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
            "      <td>" + 
                String2.substitute(getLabel, "" + (step.i++), null, null) + 
                "</td>\n" +
            "      <td style=\"width:90%;\">\n"); //force this column to expand as much as possible
        boolean hasNoBr = false;
        //if (!tableHasData) {
        //    htmlSB.append("        " + oneOf.noDataAvailable() + "\n");
        //} else 
        if (!dataAccessAllowed) {
            htmlSB.append("        " + oneOf.dataAccessNotAllowed() + "\n");
        } else {
            //collect dataVariableNames as csvString
            //???!!! specify via variableNames or longNames?
            String[] dataVariableNames = tableDataSet.dataVariableNames();
            String[] dataLongNames = tableDataSet.dataLongNames();
            StringBuilder sb = new StringBuilder();
            if (getAll) {
                sb.append(String2.toCSSVString(dataVariableNames));
            } else {
                int dataX = String2.indexOf(dataLongNames, xAxisValue);
                int dataY = String2.indexOf(dataLongNames, yAxisValue);
                int dataC = String2.indexOf(dataLongNames, colorBarValue);
                if (dataX >= 0) sb.append(dataVariableNames[dataX]);
                if (dataY >= 0 && dataY != dataX) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(dataVariableNames[dataY]);
                }
                if (dataC >= 0 && dataC != dataX && dataC != dataY) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(dataVariableNames[dataC]);
                }
            }
            String2.replaceAll(sb, " ", "");
            String dataVariablesString = sb.toString();

            String tUrl = "        <a href=\"" + oneOf.url() + 
                "?get=" + getWhat +
                "&amp;dataSet=" + tableDataSet.internalName() +
                //it supports multiple individuals; I only request 1 here
                "&amp;individuals=" + String2.replaceAll(individualValue, " ", "") + 
                "&amp;dataVariables=" + dataVariablesString +
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
            htmlSB.append(oneOf.pointFileHelpLink());  //they are point files, too

            //GETQuery help link
            htmlSB.append("        |\n");
            htmlSB.append(oneOf.GETQueryHelpLink());

        }

        //data set help link
        String infoUrl = oneOf.getInternalNameInfoUrl(tableDataSet.internalName());
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

    /**
     * This responds to a submitter button, preparing the file and the 
     * html.
     *
     * @param submitter the name of the submitter button
     * @param htmlSB the StringBuilder which is collecting the html
     * @param backButtonForm the html for a form with just a back button
     * @param throws Exception if trouble
     */
/*    public void respondToSubmitter(String submitter, StringBuilder htmlSB, 
            String backButtonForm) throws Exception {

        //failure
        Test.error(String2.ERROR + ": Unexpected submitter: " + submitter);
    }
*/

}
