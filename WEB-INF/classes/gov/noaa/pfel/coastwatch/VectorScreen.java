/* 
 * VectorScreen Copyright 2005, NOAA.
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
import gov.noaa.pfel.coastwatch.util.IntObject;

import java.util.GregorianCalendar;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for the vector screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Vector").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class VectorScreen extends Screen {

    public EmaSelect  dataSet, timePeriod;
    public EmaDateTimeSelect2 centeredTime;
    public EmaColor   color;

    //set by the constructor
    public String dataSetLabel, timePeriodLabel,
        centeredTimeLabel, getLabel, colorLabel;

    //set by validate
    public String dataSetValue;
    public int absoluteDataSetIndex;  //if no vectors plotted, this will be 0
    public boolean plotData;

    public String timePeriodValue;
    public String timeValue;
    public GregorianCalendar startCalendar, endCalendar;
    private String lastModernTime = "9999-12-12 23:59:59";

    public String colorValue;

    public GridDataSet xGridDataSet;
    public GridDataSet yGridDataSet;
    public String legendTime;
    public boolean xDataAccessAllowed;
    public boolean yDataAccessAllowed;

    public String addToImageFileName;


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
    public VectorScreen(int editOption, OneOf oneOf, Shared shared, EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in VectorScreen(): ";

        //add the components to the emaClass
        emaClass.addAttribute(dataSet        = new EmaSelect(emaClass, "vectorDataSet"));
        emaClass.addAttribute(timePeriod     = new EmaSelect(emaClass, "vectorTimePeriod"));
        emaClass.addAttribute(centeredTime   = new EmaDateTimeSelect2(emaClass, "vectorCenteredTime"));
        emaClass.addAttribute(color          = new EmaColor( emaClass, "vectorColor"));

        //get all the labels 
        dataSetLabel       = classRB2.getNotNullString("vectorDataSet.label", errorInMethod);
        timePeriodLabel    = classRB2.getNotNullString("vectorTimePeriod.label", errorInMethod);
        centeredTimeLabel  = classRB2.getNotNullString("vectorCenteredTime.label", errorInMethod);
        getLabel           = classRB2.getNotNullString("gridVectorGet.label", errorInMethod);
        colorLabel         = classRB2.getNotNullString("vectorColor.label", errorInMethod);

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
        dataSet.setOptions(shared.activeVectorOptions()); 

        dataSetValue = dataSet.getValue(session);
        int whichVector = String2.indexOf(shared.activeVectorOptions(), dataSetValue);
        if (whichVector < 0) {
            if (whichVector < 0)  //check for 7char name
                whichVector = String2.indexOf(shared.activeVector7Names(), dataSetValue);
            if (whichVector < 0)
                whichVector = 0; //none
            dataSetValue = shared.activeVectorOptions()[whichVector];
            dataSet.setValue(session, dataSetValue);
        }
        if (oneOf.verbose()) String2.log("vectorScreen dataSetValue = " + dataSetValue);
        absoluteDataSetIndex = String2.indexOf(oneOf.vectorOptions(), dataSetValue); //this 'absolute' is okay
        plotData = whichVector > 0;
        if (doTally)
            oneOf.tally().add("Vector Data Set Usage:", dataSetValue);
        if (showEditOption == editOption)
            addTableEntry(dataSet, dataSetLabel, dataSetValue, rowNumber, step, htmlSB);

        timePeriodValue = null;
        startCalendar = null;
        endCalendar = null;
        colorValue = null;
        xGridDataSet = null;
        yGridDataSet = null;
        legendTime = "";
        xDataAccessAllowed = false;
        yDataAccessAllowed = false;
        addToImageFileName = "";  //append stuff, then hash at end
        int unitsIndex = 0; //constant for now

        //if not "(None)"...
        timeValue = null;

        if (plotData) { 
            int dataSetIndex = dataSet.indexOf(dataSetValue);
            xGridDataSet = (GridDataSet)shared.activeGridDataSets().get(shared.activeVectorXDataSetIndexes()[dataSetIndex]);
            yGridDataSet = (GridDataSet)shared.activeGridDataSets().get(shared.activeVectorYDataSetIndexes()[dataSetIndex]);

            //timePeriod
            Object oar[] = (Object[])shared.activeVectorContents().get(whichVector);
            String[] activeVectorTimePeriodOptions          = (String[])oar[0];
            String[] activeVectorTimePeriodTitles           = (String[])oar[1];
            Vector   activeVectorTimePeriodTimes            = (Vector)oar[2]; 
            timePeriod.setOptions(activeVectorTimePeriodOptions);
            timePeriod.setTitles( activeVectorTimePeriodTitles);
            int timePeriodIndex = TimePeriods.closestTimePeriod(
                timePeriod.getValue(session), activeVectorTimePeriodOptions);
            timePeriodValue = activeVectorTimePeriodOptions[timePeriodIndex];
            timePeriod.setValue(session, timePeriodValue);
            if (oneOf.verbose()) String2.log("vectorScreen timePeriodValue = " + timePeriodValue);
            if (showEditOption == editOption) 
                addTableEntry(timePeriod, timePeriodLabel, timePeriodValue, rowNumber, step, htmlSB);

            //time
            String[] activeVectorTimeOptions = (String[])activeVectorTimePeriodTimes.get(timePeriodIndex);
            centeredTime.setOptions(activeVectorTimeOptions);
            timeValue = centeredTime.getValue(session);

            //if was climatology and now not
            if (timeValue == null)
                timeValue = lastModernTime;
            if (timeValue.startsWith("0001-") && !activeVectorTimeOptions[0].startsWith("0001-"))
                timeValue = lastModernTime.substring(0, 5) +
                    timeValue.substring(5);
            //If was regular and now climatology, it is dealt with by gridDataSetClimatology.findClosestTime.
            //Synchronizing (non)climatology datasets is done by cwUser.synchTime.

            int timeIndex = xGridDataSet.binaryFindClosestTime(activeVectorTimeOptions, 
                String2.replaceAll(timeValue, "T", " "));
            timeValue = activeVectorTimeOptions[timeIndex];
            centeredTime.setValue(session, timeValue);

            //store lastModernTime
            if (!timeValue.startsWith("0001-"))
                lastModernTime = timeValue;
            if (oneOf.verbose()) String2.log("  timeValue = " + timeValue);
            if (showEditOption == editOption) 
                addTableEntry(centeredTime, centeredTimeLabel, timeValue, rowNumber, step, htmlSB);
            startCalendar = TimePeriods.getStartCalendar(timePeriodValue, timeValue, "");
            endCalendar   = TimePeriods.getEndCalendar(timePeriodValue, timeValue, "");

            //color
            colorValue = color.getValue(session);
            if (color.isValid(colorValue).length() > 0) {
                colorValue = color.getDefaultValue();
                color.setValue(session, colorValue);
            }
            if (showEditOption == editOption)
                addTableEntry(color, colorLabel, colorValue, rowNumber, step, htmlSB);
            if (oneOf.verbose()) String2.log("  colorValue = " + colorValue);

            //make fileNames
            //FILE_NAME_RELATED_CODE
            int xTimePeriodIndex = String2.indexOf(xGridDataSet.activeTimePeriodOptions, timePeriodValue);
            int yTimePeriodIndex = String2.indexOf(yGridDataSet.activeTimePeriodOptions, timePeriodValue);
            //unusual: custom file name is always 'S' units
            xDataAccessAllowed = xGridDataSet.dataAccessAllowedCentered(timePeriodValue, timeValue);
            yDataAccessAllowed = yGridDataSet.dataAccessAllowedCentered(timePeriodValue, timeValue);

            //make legendTime String 
            legendTime = xGridDataSet.getLegendTime(timePeriodValue, timeValue);

            //display the 'get' options 
            if (showEditOption == editOption) {
                showGetGridVectorDataLinks(oneOf, session, 
                    htmlSB, rowNumber, step, getLabel, 
                    oneOf.vectorInfo()[absoluteDataSetIndex][OneOf.PVIInternalName],
                    xGridDataSet, yGridDataSet, xTimePeriodIndex, timeValue,
                    xDataAccessAllowed && yDataAccessAllowed, emaClass, 
                    currentMinX, currentMaxX, currentMinY, currentMaxY,
                    true); //show .gif options
            } 

            //make addToImageFileName  
            String baseFileName = FileNameUtility.makeBaseName(
                xGridDataSet.internalName, //e.g., LATssta
                unitsIndex == 1? 'A' : 'S', //e.g., A=Alternate units S=Standard units
                timePeriodValue, //e.g., 1 day
                timeValue);         
            addToImageFileName = "_V" + 
                String2.md5Hex12(baseFileName + colorValue);

        }
    }

    /**
     * This actually generates the x image-resolution grid,
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
    public Grid getXGrid(double minLon, double maxLon, 
            double minLat, double maxLat, int imageWidth, int imageHeight) 
            throws Exception { 
        if (!plotData)
            return null;
        try {
            return xGridDataSet.makeGrid(timePeriodValue, timeValue, 
                minLon, maxLon, minLat, maxLat, imageWidth, imageHeight);
        } catch (Exception e) {
            String error = "Exception caught in VectorScreen.getXGrid:\n" + MustBe.throwableToString(e);
            String2.log(error);
            if (error.indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                oneOf.email(oneOf.emailEverythingTo(), 
                    String2.ERROR + " in " + oneOf.shortClassName(), error);
            plotData = false;
            return null;
        }
    }

    /**
     * This actually generates the y image-resolution grid,
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
    public Grid getYGrid(double minLon, double maxLon, 
            double minLat, double maxLat, int imageWidth, int imageHeight) 
            throws Exception { 
        if (!plotData)
            return null;
        try {
            return yGridDataSet.makeGrid(timePeriodValue, timeValue, 
                minLon, maxLon, minLat, maxLat, imageWidth, imageHeight);
        } catch (Exception e) {
            String error = "Exception caught in VectorScreen.getYGrid:\n" + MustBe.throwableToString(e);
            String2.log(error);
            if (error.indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                oneOf.email(oneOf.emailEverythingTo(), 
                    String2.ERROR + " in " + oneOf.shortClassName(), error);
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
        return submitterIsAnXGetButton(submitter) ||
               submitterIsAYGetButton(submitter);        
    }

    /**
     * This determines if the submitter's name matches the name of 
     * an X Get button.
     *
     * @param submitter
     * @return false
     */
    public boolean submitterIsAnXGetButton(String submitter) {
        return false;
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a Y Get button.
     *
     * @param submitter
     * @return false 
     */
    public boolean submitterIsAYGetButton(String submitter) {
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

    /**
     * This appends a row to the table so the user can choose to download 
     * the vector data.
     *
     * @param timePeriodIndex the selected index of the xGridDataSet's time periods.
     * @return true if xGridDataSet has data in range
     */
    public static void showGetGridVectorDataLinks(OneOf oneOf, HttpSession session, 
        StringBuilder htmlSB, IntObject rowNumber, IntObject step,
        String getLabel, String internalName, GridDataSet xGridDataSet, GridDataSet yGridDataSet, 
        int timePeriodIndex, String timeValue,
        boolean dataAccessAllowed, EmaClass emaClass,
        double minX, double maxX, double minY, double maxY,
        boolean showGifOptions) {


        htmlSB.append(
            "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
            "      <td>" + 
                String2.substitute(getLabel, "" + (step.i++), null, null) + 
                "</td>\n" +
            "      <td>\n");

        boolean needsPipe = true;
        boolean hasNoBr = false;
        if (dataAccessAllowed) {

            String tCenteredTime = xGridDataSet.isClimatology?
                String2.replaceAll(timeValue, " ", "") :
                String2.replaceAll(timeValue, ' ', 'T'); //so connector is 'T'
            String tUrl = "        <a href=\"" + oneOf.url() + 
                "?get=gridVectorData&amp;dataSet=" + internalName +
                "&amp;timePeriod=" + Browser.clean(xGridDataSet.activeTimePeriodOptions[timePeriodIndex]) +
                "&amp;centeredTime=" + tCenteredTime +
                "&amp;minLon=" + String2.genEFormat10(minX) + 
                "&amp;maxLon=" + String2.genEFormat10(maxX) + 
                "&amp;minLat=" + String2.genEFormat10(minY) + 
                "&amp;maxLat=" + String2.genEFormat10(maxY) +
                "&amp;fileType="; //.nc" + "\">.nc</a>"

            String[] gridVectorGetOptions = oneOf.gridVectorGetOptions();
            String[] gridVectorGetTitles  = oneOf.gridVectorGetTitles();
            for (int i = 0; i < gridVectorGetOptions.length; i++) {
                //show non-gif options 
                //.gif is useful in HTTP GET queries, but here it is misleading 
                //  because gif doesn't use specified palette information
                if (showGifOptions || gridVectorGetOptions[i].indexOf(".gif") < 0) {
                    htmlSB.append(
                        tUrl + Browser.clean(gridVectorGetOptions[i]) + "\" title=\"" + gridVectorGetTitles[i] + 
                        "\">" + gridVectorGetOptions[i]+ "</a> " + 
                        (i == gridVectorGetOptions.length - 1? " &nbsp; &nbsp; " : "|") + 
                        "\n");
                }
            }

            //file type help link
            hasNoBr = true;
            htmlSB.append("        <span style=\"white-space:nowrap;\">\n"); 
            htmlSB.append(oneOf.gridFileHelpLink());

            //GETQuery help link
            htmlSB.append("        |\n");
            htmlSB.append(oneOf.GETQueryHelpLink());

            //give link to u OPeNDAP
            tUrl = xGridDataSet.activeTimePeriodOpendapUrls[timePeriodIndex];
            if (tUrl != null) {
                htmlSB.append( 
                    "        |\n" +
                    "        <a href=\"" + oneOf.makePublicUrl(tUrl) + ".html\"\n" +
                    "          title=\"Download the selected u data or related data via an OPeNDAP server.\">u OPeNDAP</a>\n");
            }

            //give link to v OPeNDAP
            tUrl = yGridDataSet.activeTimePeriodOpendapUrls[timePeriodIndex];
            if (tUrl != null) {
                htmlSB.append( 
                    "        |\n" +
                    "        <a href=\"" + oneOf.makePublicUrl(tUrl) + ".html\"\n" +
                    "          title=\"Download the selected v data or related data via an OPeNDAP server.\">v OPeNDAP</a>\n");
            }

        } else {
            htmlSB.append("        " + oneOf.dataAccessNotAllowed() + "\n");
        }

        //data set help link
        String infoUrl = oneOf.getInternalNameInfoUrl(xGridDataSet.internalName);
        if (needsPipe && infoUrl.length() > 0)
            htmlSB.append("        |\n");
        htmlSB.append(infoUrl);
        if (hasNoBr)
            htmlSB.append("        </span>\n");

        htmlSB.append(  
             "      </td>\n" +
             "    </tr>\n");
    }


}
