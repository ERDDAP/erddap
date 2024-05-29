/* 
 * MapScreen Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.ema.*;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.IntObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This handles the user interface for the bathymetry screen (the part of the form in 
 * CWBrowser below the "Edit" line if the user selects "Edit : Bathymetry").
 * Each web page request will generate a new instance of this.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class BathymetryScreen extends Screen {

    public EmaSelect  gifSize;
    public EmaColor   lineColor;
    public EmaBoolean drawColors, drawLines;
    public EmaString  drawLinesAt;

    //set by the constructor
    public String getLabel, getTitle, drawColorsLabel, drawLinesLabel, drawLinesAtLabel, lineColorLabel;
        
    //set by validate
    public String drawColorsValue, drawLinesValue; 
    public boolean plotColors; 
    public boolean plotLines; 

    public String lineColorValue;

    public String drawLinesAtValue; 
    public String hiddenInputStillPending;


    /** 
     * The constructor for BathymetryScreen. 
     *
     * @param editOption the edit option number for this screen
     * @param oneOf the oneOf class
     * @param shared the shared class 
     * @param emaClass the emaClass to which this belongs
     * @param doTally indicates if activity of this user should be tallied
     *     (see dontTallyIPAddresses in properties file)
     */
    public BathymetryScreen(int editOption, OneOf oneOf, Shared shared, EmaClass emaClass, boolean doTally)  {
        this.editOption = editOption;
        this.oneOf = oneOf;
        this.shared = shared;
        this.emaClass = emaClass;
        this.doTally = doTally;
        ResourceBundle2 classRB2 = oneOf.classRB2(); 
        String errorInMethod = String2.ERROR + " in BathymetryScreen(): ";

        //add the components to the emaClass
        //emaClass.addAttribute(drawColors  = new EmaBoolean(emaClass, "bathymetryDrawColors"));
        emaClass.addAttribute(drawLines   = new EmaBoolean(emaClass, "bathymetryDrawLines"));
        emaClass.addAttribute(drawLinesAt = new EmaString( emaClass, "bathymetryDrawLinesAt"));
        emaClass.addAttribute(lineColor   = new EmaColor(  emaClass, "bathymetryLineColor"));

        //get all the labels 
        //drawColorsLabel  = classRB2.getNotNullString("bathymetryDrawColors.label", errorInMethod);
        drawLinesLabel   = classRB2.getNotNullString("bathymetryDrawLines.label", errorInMethod);
        lineColorLabel   = classRB2.getNotNullString("bathymetryLineColor.label", errorInMethod);
        drawLinesAtLabel = classRB2.getNotNullString("bathymetryDrawLinesAt.label", errorInMethod);
        getLabel         = classRB2.getNotNullString("bathymetryGet.label", errorInMethod);
        getTitle         = classRB2.getNotNullString("bathymetryGet.title", errorInMethod);

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

        hiddenInputStillPending = "";

        //drawColors
        /*drawColorsValue = drawColors.getValue(session); 
        plotColors = String2.parseBoolean(drawColorsValue); 
        if (oneOf.verbose()) String2.log("bathymetryScreen plotColors = " + plotColors);
        if (doTally)
            oneOf.tally().add("Bathymetry Colors Drawn:", plotColors? "Yes": "No");
        if (showEditOption == editOption) 
            addTableEntry(drawColors, drawColorsLabel, drawColorsValue, rowNumber, step, htmlSB);
        else  //put on form as hidden, so info not lost when not visible
            hiddenInputStillPending += drawColors.getHiddenControl(drawColorsValue);
        */

        //drawLines
        drawLinesValue = drawLines.getValue(session); 
        plotLines = String2.parseBoolean(drawLinesValue); 
        if (oneOf.verbose()) String2.log("bathymetryScreen plotLines = " + plotLines);
        if (doTally)
            oneOf.tally().add("Bathymetry Lines Drawn:", plotLines? "Yes": "No");
        if (showEditOption == editOption) 
            addTableEntry(drawLines, drawLinesLabel, drawLinesValue, rowNumber, step, htmlSB);
        else  //put on form as hidden, so info not lost when not visible
            hiddenInputStillPending += drawLines.getHiddenControl(drawLinesValue);

        //lineColor
        lineColorValue = lineColor.getValue(session);
        if (plotLines) {
            if (lineColor.isValid(lineColorValue).length() > 0) {
                lineColorValue = lineColor.getDefaultValue();
                lineColor.setValue(session, lineColorValue);
            }
            if (showEditOption == editOption)
                addTableEntry(lineColor, lineColorLabel, lineColorValue, rowNumber, step, htmlSB);
            if (oneOf.verbose()) String2.log("  lineColorValue = " + lineColorValue);
        }

        //drawLinesAt
        drawLinesAtValue = drawLinesAt.getValue(session).trim(); 
        if (plotLines) {
            //ensure perfectly valid and in standard format (to avoid map redraw for minor difference)
            drawLinesAtValue = String2.toCSSVString(String2.justFiniteValues(
                String2.csvToDoubleArray(drawLinesAtValue)));
            if (drawLinesAtValue == null || drawLinesAtValue.length() == 0) {
                if (oneOf.verbose()) String2.log("  resetting drawLinesAtValue"); 
                drawLinesAtValue = String2.toCSSVString(String2.justFiniteValues(
                    String2.csvToDoubleArray(drawLinesAt.getDefaultValue())));
            }

            //store value in session
            drawLinesAt.setValue(session, drawLinesAtValue);
            if (oneOf.verbose()) String2.log("  drawLinesAtValue = " + drawLinesAtValue);
            if (showEditOption == editOption)
                addTableEntry(drawLinesAt, drawLinesAtLabel, drawLinesAtValue, rowNumber, step, htmlSB);
        }

        //'get' options 
        if (plotLines && showEditOption == editOption) {
            /*
            emaClass.setBeginRow(Screen.beginRowArray[Math2.odd(rowNumber.i++)? 1 : 0]);
            sb.append(
                "    " + emaClass().getBeginRow() + "\n" + 
                "      <td>" + 
                    String2.substitute(getLabel, "" + (step.i++), null, null) + 
                    "&nbsp;</td>\n" +
                "      <td><a href=\"" + 
                    "images/" + highResBathymetryFileName + ".zip\"\n" +  
                "        title=\"" + getTitle + "\">" + 
                    highResBathymetryFileName + ".zip</a></td>\n" +
                "    </tr>\n");
            */
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
        return false;
    }


}
