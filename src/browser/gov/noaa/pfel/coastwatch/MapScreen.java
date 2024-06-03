/* 
 * MapScreen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.ema.*;
import com.cohort.util.Math2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.IntObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for the map screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Map").
 * Each UserCW has its own instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class MapScreen extends Screen {

    public EmaSelect  region, imageSize, animationN, animationTimePeriod, animationFPS;
    public EmaDouble  minX, maxX, minY, maxY;
    public EmaButton  zoomIn, zoomOut, moveNorth, moveSouth, moveWest, moveEast, 
        getPdf, animationViewIt;
    public EmaBoolean synchronizeTimes;

    //set by the constructor
    public String  synchronizeTimesLabel, imageSizeLabel, animationNLabel, animationViewItLabel;

    public String getLabel; 

    //set by validate()
    public boolean synchronizeTimesValue;
    public String  imageSizeValue;
    public int     imageSizeIndex;
    public int     imageWidth;
    public int     imageHeight;
    public String  animationNValue;
    public String  animationTimePeriodValue;
    public String  animationFPSValue;

    public String  hiddenInputStillPending;

    /** 
     * The constructor for MapScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public MapScreen(int editOption, OneOf oneOf, Shared shared, EmaClass emaClass, boolean doTally) {
        this.editOption = editOption;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in MapScreen(): ";

        //add the components to the emaClass
        //The buttons here are unique so don't need prefix "map"
        emaClass.addAttribute(imageSize            = new EmaSelect(emaClass, "imageSize")); 
        emaClass.addAttribute(region               = new EmaSelect(emaClass, "region"));
        emaClass.addAttribute(minX                 = new EmaDouble(emaClass, "minX"));
        emaClass.addAttribute(maxX                 = new EmaDouble(emaClass, "maxX"));
        emaClass.addAttribute(minY                 = new EmaDouble(emaClass, "minY"));
        emaClass.addAttribute(maxY                 = new EmaDouble(emaClass, "maxY"));
        emaClass.addAttribute(zoomIn               = new EmaButton(emaClass, "zoomIn"));
        emaClass.addAttribute(zoomOut              = new EmaButton(emaClass, "zoomOut"));
        emaClass.addAttribute(moveNorth            = new EmaButton(emaClass, "moveNorth"));
        emaClass.addAttribute(moveSouth            = new EmaButton(emaClass, "moveSouth"));
        emaClass.addAttribute(moveWest             = new EmaButton(emaClass, "moveWest"));
        emaClass.addAttribute(moveEast             = new EmaButton(emaClass, "moveEast"));
        emaClass.addAttribute(synchronizeTimes     = new EmaBoolean(emaClass,"synchronizeTimes"));
        emaClass.addAttribute(getPdf               = new EmaButton(emaClass, "getPdf"));
        emaClass.addAttribute(animationN           = new EmaSelect(emaClass, "animationN")); 
        emaClass.addAttribute(animationTimePeriod  = new EmaSelect(emaClass, "animationTimePeriod")); 
        emaClass.addAttribute(animationFPS         = new EmaSelect(emaClass, "animationFPS")); 
        emaClass.addAttribute(animationViewIt      = new EmaButton(emaClass, "animationViewIt"));

        if (classRB2.getString("minX.value", null) == null) minX.setDefaultValue("" + oneOf.regionMinX());
        if (classRB2.getString("maxX.value", null) == null) maxX.setDefaultValue("" + oneOf.regionMaxX());
        if (classRB2.getString("minY.value", null) == null) minY.setDefaultValue("" + oneOf.regionMinY());
        if (classRB2.getString("maxY.value", null) == null) maxY.setDefaultValue("" + oneOf.regionMaxY());

        if (classRB2.getString("minX.title", null) == null) 
            minX.setTitle("Enter the minimum longitude to be plotted (" + oneOf.regionMinX() + 
                " to " + oneOf.regionMaxX() + " decimal degrees, blank = default).");
        if (classRB2.getString("maxX.title", null) == null) 
            maxX.setTitle("Enter the maximum longitude to be plotted (" + oneOf.regionMinX() + 
                " to " + oneOf.regionMaxX() + " decimal degrees, blank = default).");
        if (classRB2.getString("minY.title", null) == null) 
            minY.setTitle("Enter the minimum latitude to be plotted (" + oneOf.regionMinY() + 
                " to " + oneOf.regionMaxY() + " decimal degrees, blank = default).");
        if (classRB2.getString("maxY.title", null) == null) 
            maxY.setTitle("Enter the maximum latitude to be plotted (" + oneOf.regionMinY() + 
                " to " + oneOf.regionMaxY() + " decimal degrees, blank = default).");

        if (classRB2.getString("minX.min", null) == null) minX.setMin(oneOf.regionMinX());
        if (classRB2.getString("maxX.min", null) == null) maxX.setMin(oneOf.regionMinX());
        if (classRB2.getString("minY.min", null) == null) minY.setMin(oneOf.regionMinY());
        if (classRB2.getString("maxY.min", null) == null) maxY.setMin(oneOf.regionMinY());

        if (classRB2.getString("minX.max", null) == null) minX.setMax(oneOf.regionMaxX());
        if (classRB2.getString("maxX.max", null) == null) maxX.setMax(oneOf.regionMaxX());
        if (classRB2.getString("minY.max", null) == null) minY.setMax(oneOf.regionMaxY());
        if (classRB2.getString("maxY.max", null) == null) maxY.setMax(oneOf.regionMaxY());

        Test.ensureNotNull(imageSize.getLabel(),           "imageSize.getLabel is null."); 
        Test.ensureNotNull(region.getLabel(),              "region.getLabel is null.");
        Test.ensureNotNull(minX.getLabel(),                "minX.getLabel is null.");
        Test.ensureNotNull(maxX.getLabel(),                "maxX.getLabel is null.");
        Test.ensureNotNull(minY.getLabel(),                "minY.getLabel is null.");
        Test.ensureNotNull(maxY.getLabel(),                "maxY.getLabel is null.");
        Test.ensureNotNull(zoomIn.getLabel(),              "zoomIn.getLabel is null.");
        Test.ensureNotNull(zoomOut.getLabel(),             "zoomOut.getLabel is null.");
        Test.ensureNotNull(moveNorth.getLabel(),           "moveNorth.getLabel is null.");
        Test.ensureNotNull(moveSouth.getLabel(),           "moveSouth.getLabel is null.");
        Test.ensureNotNull(moveWest.getLabel(),            "moveWest.getLabel is null.");
        Test.ensureNotNull(moveEast.getLabel(),            "moveEast.getLabel is null.");
        Test.ensureNotNull(synchronizeTimes.getLabel(),    "synchronizeTimes.getLabel is null.");
        Test.ensureNotNull(getPdf.getLabel(),              "getPdf.getLabel is null.");
        Test.ensureNotNull(animationN.getLabel(),          "animationN.getLabel is null.");
        Test.ensureNotNull(animationTimePeriod.getLabel(), "animationTimePeriod.getLabel is null.");
        Test.ensureNotNull(animationFPS.getLabel(),        "animationFPS.getLabel is null.");
        Test.ensureNotNull(animationViewIt.getLabel(),     "animationViewIt.getLabel is null.");

        //get all the labels 
        imageSizeLabel = imageSize.getLabel();
        animationNLabel = animationN.getLabel();
        animationViewItLabel = animationViewIt.getLabel();
        synchronizeTimesLabel = synchronizeTimes.getLabel();

        //get 'get' properties
        getLabel = classRB2.getNotNullString("mapGet.label", errorInMethod);

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
     */
    public void validate(HttpSession session, int showEditOption, IntObject step, 
            IntObject rowNumber, StringBuilder htmlSB) {

        //region
        //min/max/X/Y are set to valid values in processRequest
        String tMinX = minX.getValue(session);
        String tMaxX = maxX.getValue(session);
        String tMinY = minY.getValue(session);
        String tMaxY = maxY.getValue(session);
        if (doTally) {
            int index = region.indexOf(region.getValue(session));
            if (index >= 0) 
                oneOf.tally().add("Region Usage:", region.getValue(session));
        }
        if (showEditOption == editOption) {
            //set a suitable increment (for the min/max/X/Y +- buttons)
            double range = String2.parseDouble(tMaxX) - String2.parseDouble(tMinX);
            double inc = Double.isFinite(range)? Math2.suggestDivisions(Math.abs(range))[0] : 1;        
            minX.setIncrement(inc);
            maxX.setIncrement(inc);
            range = String2.parseDouble(tMaxY) - String2.parseDouble(tMinY);
            inc = Double.isFinite(range)? Math2.suggestDivisions(Math.abs(range))[0] : 1;        
            minY.setIncrement(inc);
            maxY.setIncrement(inc);

            region.setLabel(String2.substitute(region.getLabel(), "" + (step.i++), null, null));
            String beginRowTag = oneOf.getBeginRowTag(Math2.odd(rowNumber.i++));
            htmlSB.append(
                //standard regions
                "    " + beginRowTag + "\n" +
                "      <td>" + region.getLabel() + "</td>\n" +
                "      <td>" + region.getControl(region.getValue(session)) + "</td>\n" +
                "    </tr>\n" +

                //or specify min/max/X/Y
                //  ---- maxY ----
                //  minX ---- maxX
                //  ---- minY ----
                "    " + beginRowTag + "\n" + 
                "      <td ><span style=\"white-space:nowrap;\">&nbsp;&nbsp;&nbsp;&nbsp;<small>(or specify ...)</small>&nbsp;</span></td>\n" +
                "      <td>\n" +
                "        <table class=\"erd\" style=\"width:2%;\">\n" + //padding=0. 'width' solves gap betwen buttons in opera 9
                "          <tr>\n" +
                "            <td> &nbsp; </td>\n" +
                "            <td class=\"R\">" + maxY.getLabel() + "</td>\n" +
                "            <td>"  + maxY.getControl(tMaxY) + "</td>\n" +
                "            <td> &nbsp; </td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td>" + minX.getLabel() + "</td>\n" +
                "            <td>" + minX.getControl(tMinX) + "</td>\n" +
                "            <td class=\"R\">" + maxX.getLabel() + "</td>\n" +
                "            <td>" + maxX.getControl(tMaxX) + "</td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td> &nbsp; </td>\n" +
                "            <td class=\"R\">" + minY.getLabel() + "</td>\n" +
                "            <td>"  + minY.getControl(tMinY) + "</td>\n" +
                "            <td> &nbsp; </td>\n" +
                "          </tr>\n" +
                "        </table>\n" + 
                "      </td>\n" +

                //or use the other buttons
                "    " + beginRowTag + "\n" +
                "      <td>&nbsp;&nbsp;&nbsp;&nbsp;<small>(or ...)</small></td>\n" +
                "      <td class=\"N\">Zoom: " + zoomIn.getControl(zoomIn.getValue(session)) + "\n" +
                "        " + zoomOut.getControl(zoomOut.getValue(session)) + "</span>\n" +
                "        <br>View the region to the: " + moveNorth.getControl(moveNorth.getValue(session)) + "\n" +
                "        " + moveSouth.getControl(moveSouth.getValue(session)) + "\n" +
                "        " + moveWest.getControl(moveWest.getValue(session)) + "\n" +
                "        " + moveEast.getControl(moveEast.getValue(session)) + "</td>\n" +
                "    </tr>\n");
        }

        //synchronizeTimes
        String synchronizeTimesString = synchronizeTimes.getValue(session);
        synchronizeTimesValue = synchronizeTimesString.equals(EmaBoolean.TRUE); 
        hiddenInputStillPending = "";
        if (oneOf.verbose()) String2.log("mapScreen synchronizeTimes = " + synchronizeTimesValue);
        if (showEditOption == editOption) 
            addTableEntry(synchronizeTimes, synchronizeTimesLabel, synchronizeTimesString, rowNumber, step, htmlSB);
        else  //put on form as hidden, so info not lost when not visible
            hiddenInputStillPending = synchronizeTimes.getHiddenControl(synchronizeTimesString);

        //imageSize
        imageSizeValue = imageSize.getValue(session);
        if (imageSize.isValid(imageSizeValue).length() > 0) {
            imageSizeValue = imageSize.getDefaultValue();
            imageSize.setValue(session, imageSizeValue);
        }
        imageSizeIndex = imageSize.indexOf(imageSizeValue);
        imageWidth  = oneOf.imageWidths()[imageSizeIndex];
        imageHeight = oneOf.imageHeights()[imageSizeIndex];
        if (doTally)
            oneOf.tally().add("Gif Size Usage:", imageSizeValue);
        if (showEditOption == editOption) 
            addTableEntry(imageSize, imageSizeLabel, imageSizeValue, rowNumber, step, htmlSB);

        //animation
        animationNValue = animationN.getValue(session);
        if (animationN.isValid(animationNValue).length() > 0) {
            animationNValue = animationN.getDefaultValue();
            animationN.setValue(session, animationNValue);
        }
        animationTimePeriodValue = animationTimePeriod.getValue(session);
        if (animationTimePeriod.isValid(animationTimePeriodValue).length() > 0) {
            animationTimePeriodValue = animationTimePeriod.getDefaultValue();
            animationTimePeriod.setValue(session, animationTimePeriodValue);
        }       

        animationFPSValue = animationFPS.getValue(session);
        if (animationFPS.isValid(animationFPSValue).length() > 0) {
            animationFPSValue = animationFPS.getDefaultValue();
            animationFPS.setValue(session, animationFPSValue);
        }       
    }

    /**
     * This determines if the submitter's name matches the name of 
     * a Get button (e.g., getAsc).
     *
     * @param submitter
     * @return true if the submitter's name matches the name of 
     *    a Get button (e.g., getAsc); else false. 
     */
    public boolean submitterIsAGetButton(String submitter) {
        return submitter.equals(getPdf.getName()); 
    }

    /**
     * This adds the 'Get' options line to the form. 
     * This is only called if showEditOption==editOption.
     *
     * @param session the user's session
     * @param showEditOption  &gt;=0 for User.EDIT_XXX screens 
     *    or &lt;0 for responses to getXxx buttons 
     * @param step the next step number on the form (e.g., 1), 2), 3), ...).
     * @param rowNumber the number of the next row in the html form.
     * @param htmlSB the StringBuilder collecting the html to be sent to the users
     *    (won't be null).
     * @param imageFileName (without the directory, but with the extension)
     */
    public void displayGetOptions(HttpSession session, int showEditOption, IntObject step, 
            IntObject rowNumber, StringBuilder htmlSB, String imageFileName) {
        htmlSB.append(
            "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" + 
            "      <td>" + 
                String2.substitute(getLabel, "" + (step.i++), null, null) + 
                "</td>\n" +
            "      <td>" +
            getPdf.getControl(getPdf.getValue(session)) + "\n" +   
            "      </td>\n" +
            "    </tr>\n");

        //animation
        if (showEditOption == editOption) {
            animationN.setLabel(String2.substitute(animationNLabel, "" + (step.i++), null, null));
            htmlSB.append(
                //standard regions
                "    " + oneOf.getBeginRowTag(Math2.odd(rowNumber.i++)) + "\n" +
                "      <td>" + animationN.getLabel() + "</td>\n" +
                "      <td>\n" +
                "        " + animationN.getControl(animationNValue) + 
                "        " + animationTimePeriod.getControl(animationTimePeriodValue) + 
                "        " + animationTimePeriod.getLabel() + 
                "        " + animationFPS.getControl(animationFPSValue) + 
                "        " + animationFPS.getLabel() + 
                "        &nbsp;&nbsp;" + animationViewIt.getControl(animationViewIt.getValue(session)) +
                "        " + animationViewItLabel + "\n" +
                "      </td>\n" +
                "    </tr>\n");
        }

    }


}
