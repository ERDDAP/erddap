/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import java.awt.Color;
import java.util.ArrayList;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying radio buttons so the user
 * can select a color.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false") 
 *     indicates if this attribute should be displayed
 *     in wide format (spanning two columns in the HTML table),
 *     or standard format (label in the left column, component in the right).
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, default = "") 
 *      is the toolTip for this attribute.
 * <li> name+".value" (a String, default = "") 
 *     is the initial value stored by this object.
 *     If it isn't one of the options, the first option is selected.
 *     Currently, all colors are case sensitive 6 digit hex strings 
 *     (e.g., "00FF00") representing RRGGBB values.
 * <li> name+".display" 
 *     specifies which palette will be displayed. 
 *     Currently, the options are: 17 (the default), 17dark (on one row, like 17, 
 *     but darker), 28 (on two rows), and 42 (on 3 rows).
 * <li> name+".enterSubmitsForm" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user changes the selection.
 *     If this is true, the JavaScript script in EmaClass.IncludeJavaScript 
 *     must be in the "head" section of the HTML page
 *     that contains the HTML form.
 * </ul>
 * <br>FUTURE/ISSUE recommend translating the options?
 *  How does underlying app deal with translated selection?
 *
 */
public class EmaColor extends EmaAttribute {

    protected String display;
    protected String selectError;

    protected String[] palette;
    protected int perRow, perGroup;

    //best if palettes always use colors from standard web palette
    //where r,g,b are one of 00, 33, 66, 99, CC, or FF.
    // /** One line: white, gray, black, 3 shades of each rainbow color. */
    /*public final static String palette21[] = {
        "FFFFFF", "999999", "000000", 
        "FF9999", "FF0000", "990000", 
        "FFFF99", "FFFF00", "999900",
        "99FF99", "00FF00", "009900",
        "99FFFF", "00FFFF", "009999",
        "9999FF", "0000FF", "000099",
        "FF99FF", "FF00FF", "990099"};
        */

    /** One line: white,grays,black, then one rainbow. 
     * If changing, change colors in gov.noaa.pfel.coastwatch.pointdata.TableDataSet, too
     */
    public final static String palette17[] = {
        "FFFFFF", "CCCCCC", "999999", "666666", "000000", 
        "FF0000", "FF9900", "FFFF00", "99FF00", "00FF00", "00FF99", 
        "00FFFF", "0099FF", "0000FF", "9900FF", "FF00FF", "FF99FF"};

    /** One line: white,grays,black, then one dark rainbow. */
    public final static String palette17dark[] = {
        "FFFFFF", "CCCCCC", "999999", "666666", "000000", 
        "990000", "996600", "999900", "669900", "009900", "009966", 
        "009999", "006699", "000099", "660099", "990099", "996699"};

    /** Two lines: white+lightGray+lightRainbow, black+gray+rainbow. */
    public final static String palette28[] = {
        "FFFFFF", "CCCCCC", //white, light gray, then light rainbow
        "FF9999", "FFCC99", "FFFF99", "CCFF99", "99FF99", "99FFCC", 
        "99FFFF", "99CCFF", "9999FF", "CC99FF", "FF99FF", "FFCCFF",
        "000000", "999999", //black, gray,  then rainbow
        "FF0000", "FF9900", "FFFF00", "99FF00", "00FF00", "00FF99", 
        "00FFFF", "0099FF", "0000FF", "9900FF", "FF00FF", "FF99FF"};

    /** Three lines: white+lightRainbow, gray+rainbow, black+darkRainbow. */
    public final static String palette42[] = {
        "FFFFFF", "CCCCCC", //white, light gray, then light rainbow
        "FF9999", "FFCC99", "FFFF99", "CCFF99", "99FF99", "99FFCC", 
        "99FFFF", "99CCFF", "9999FF", "CC99FF", "FF99FF", "FFCCFF",
        "666666", "999999", //darkGray, gray  then rainbow
        "FF0000", "FF9900", "FFFF00", "99FF00", "00FF00", "00FF99", 
        "00FFFF", "0099FF", "0000FF", "9900FF", "FF00FF", "FF99FF",
        "000000", "333333", //black, darker gray, then dark rainbow
        "990000", "996600", "999900", "669900", "009900", "009966", 
        "009999", "006699", "000099", "660099", "990099", "996699"};

    //adding a new palette? make changes in constructor (perGroup, perRow), too.

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaColor(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        selectError = String2.substitute(parent.getRequiredSelectError(), 
            parent.removeColon(name), null, null); 

        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        display = classRB2.getString(name + ".display", "17");
        perGroup = 999; //all one group   (otherwise, gap between groups)
        perRow   = 999; //all one row
        if (display.equals("42")) {
            palette  = palette42;
            perRow = 14;
        } else if (display.equals("28")) {
            palette  = palette28;
            perRow = 14;
        //} else if (display == 21) {
        //    palette  = palette21;
        //    perGroup = 3;
        } else if (display.equals("17dark")) {  //default = palette17
            palette  = palette17dark;
        } else {  //default = palette17
            display  = "17";
            palette  = palette17;
        }

    }

    /**
     * This implements the abstract createControl of EmaAttribute.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        StringBuilder sb = new StringBuilder(
            "\n" +
            "        <table class=\"erd\">\n" + //padding=1
            "          <tr>\n");

        sb.append("\n");
        int inGroup = 0;
        int inRow   = 0;
        for (int i = 0; i < palette.length; i++) {
            //a radio button with the appropriate background color
            String checked = value.equals(palette[i])? " checked" : "";
            sb.append(
                "          <td style=\"background-color:#" + palette[i] + "\">" +
                    "<input type=\"radio\" name=\"" + name + 
                    "\" value=\"" + palette[i] + "\"" + checked + " " +
                    getOnClickSubmitsFormHTML() + "\n" + //onChange doesn't work
                "            title=\"" + XML.encodeAsHTML(title) + " [0x" + palette[i] + "]" + checked + "\"" + 
                    "></td>\n");

            //small gap between groups
            if (++inGroup % perGroup == 0 && i != palette.length - 1)
                sb.append("          <td style=\"background-color:#FFFFFF\">&nbsp;</td>\n");

            //new row
            if (++inRow % perRow == 0 && i != palette.length - 1)
                sb.append(
                    "          </tr>\n" +
                    "          <tr>\n");
        }
        sb.append(
            "          </tr>\n" +
            "        </table>\n" +
            "      ");

        //return the results
        return sb.toString();
    }


    /**
     * This indicates if aString is one of the strings in options.
     *
     * @param aValue the String form of a value to be tested
     * @return an error string ("" if aValue is valid).
     */
    public String isValid(String aValue) {
        return String2.indexOf(palette, aValue) >= 0? "" : selectError;        
    } 

}
