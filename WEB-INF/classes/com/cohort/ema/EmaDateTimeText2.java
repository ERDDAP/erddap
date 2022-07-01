/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.Calendar2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying a Gregorian Calendar date
 * and/or a time using one textfield.
 * This class holds the properties for displaying some or all of a 
 * Gregorian Calendar date and/or a time using different types of components.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false") 
 *     indicates if this attribute should be displayed
 *     in wide format (spanning two columns in the HTML table),
 *     or standard format (label in the left column, component in the right).
 * <li> name+".required" ("true" or "false", default = true) 
 *     indicates if a value must be provided for this attribute
 *     to be considered valid. If false, a value of "" is considered valid.
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, default = "") 
 *      is the toolTip for this attribute 
 * <li> name+".suffix" (an HTML text string, default = "", suggestion = "Z")
 *      is the text printed to the right of the widget on the HTML form
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".value" (a dateTime in the ISO format: YYYY-MM-DDTHH:MM:SS)
 *      is the initial value stored by this object.
 *      A missing or improperly formatted dateTime is converted to today's dateTime.
 * <li> name+".min" (a dateTime in the ISO format: YYYY-MM-DDTHH:MM:SS")
 *      is the minimum allowed value.
 *      Jan 1 of some year is recommended.
 *      A missing or improperly formatted min is converted to today's dateTime.
 *      The default is "1900-01-01T00:00:00".
 * <li> name+".max" (a dateTime in the ISO format: YYYY-MM-DDTHH:MM:SS")
 *      is the maximum allowed value.
 *      Dec 31 of some year is recommended.
 *      A missing or improperly formatted max is converted to today's dateTime.
 *      The default is "2099-12-31T23:59:59".
 * <li> name+".show" (YMDhms, but perhaps with some characters missing at the end,
 *      default = "YMDhms". This is case sensitive.) 
 *      determines which parts of the dateTime are displayed
 * <li> name+".buttonsVisible" ("true" or "false", default = "true")
 *      determines if the first,+,-,last buttons should be visible.
 * <li> name+".increment" sets the Calendar field that +,- buttons (if visible)
 *      affect. Values are 13=Calendar.SECOND, 12=Calendar.MINUTE, 10=Calendar.HOUR, 
 *      5=Calendar.DATE, 2=Calendar.MONTH, 1=Calendar.YEAR.
 *      The default is Calendar.HOUR.
 * </ul>
 */
public class EmaDateTimeText2 extends EmaAttribute{

    protected GregorianCalendar
        minDateTime = Calendar2.newGCalendarZulu(),
        maxDateTime = Calendar2.newGCalendarZulu();
    protected String show; //YMDhms but perhaps with characters removed from end
    protected int increment;
    protected boolean showSeconds, showMinutes, showHours, showDates, showMonths, showYears;
    protected boolean buttonsVisible;
    protected String dateError;
    private String suffix = "";

    public final static int YEAR        = Calendar.YEAR;
    public final static int MONTH       = Calendar.MONTH;
    public final static int DATE        = Calendar.DATE;
    public final static int HOUR_OF_DAY = Calendar.HOUR_OF_DAY; //0..23
    public final static int MINUTE      = Calendar.MINUTE;
    public final static int SECOND      = Calendar.SECOND;

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaDateTimeText2(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        title = parent.getDateError();
        getStandardProperties();
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        try {
            minDateTime = Calendar2.parseISODateTimeZulu( //zulu avoids time zone issues
                classRB2.getString(name + ".min", "1900-01-01T00:00:00"));
        } catch (Exception e) {
            minDateTime = Calendar2.parseISODateTimeZulu("1900-01-01T00:00:00");
        }
        try {
            maxDateTime = Calendar2.parseISODateTimeZulu(
                classRB2.getString(name + ".max", "2099-12-31T23:59:59"));
        } catch (Exception e) { 
            maxDateTime = Calendar2.parseISODateTimeZulu("2099-12-31T23:59:59");
        }
        setMinMaxDateTime(minDateTime, maxDateTime);
        setShow(classRB2.getString(name + ".show", "YMDhms"));
        setButtonsVisible(classRB2.getBoolean(name + ".buttonsVisible", true));
        suffix = classRB2.getString(name + ".suffix", "");
        setIncrement(classRB2.getInt(name + ".increment", Calendar.HOUR));

        title = String2.substitute(title, //after min, max set
            parent.removeColon(name), 
            Calendar2.formatAsISODateTimeSpace(minDateTime), 
            Calendar2.formatAsISODateTimeSpace(maxDateTime)); 
        dateError = String2.substitute(parent.getDateError(), 
            parent.removeColon(name), 
            Calendar2.formatAsISODateTimeSpace(minDateTime), 
            Calendar2.formatAsISODateTimeSpace(maxDateTime)); 
    }

    /**
     * This lets you specify if the first,-,+,last buttons should be visible.
     *
     * @param tButtonsVisible
     */
    public void setButtonsVisible(boolean tButtonsVisible) {
        buttonsVisible = tButtonsVisible;
    }

    /**
     * This lets you set show, which determines what parts of the date time
     * are visible.
     *
     * @param tShow "YMDhms" but perhaps with some characters removed at the end
     */
    public void setShow(String tShow) {

        if (tShow.length() > 0 && tShow.length() <= 6 && tShow.equals("YMDhms".substring(0, tShow.length())))
            show = tShow;
        else show = "YMDhms";
        showSeconds = show.indexOf('s') >= 0;
        showMinutes = show.indexOf('m') >= 0;
        showHours   = show.indexOf('h') >= 0;
        showDates   = show.indexOf('D') >= 0;
        showMonths  = show.indexOf('M') >= 0;
        showYears   = true; //always
    }

    /**
     * This lets you set increment, the Calendar field that +,- buttons (if visible)
     *      affect.
     *
     * @param tIncrement the Calendar field that +,- buttons (if visible)
     *      affect. Values are 13=Calendar.SECOND, 12=Calendar.MINUTE, 10=Calendar.HOUR, 
     *      5=Calendar.DATE, 2=Calendar.MONTH, 1=Calendar.YEAR.
     *      Invalid values are set to Calendar.HOUR.
     */
    public void setIncrement(int tIncrement) {
        if (String2.indexOf(new int[]{Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, 
            Calendar.DATE, Calendar.MONTH, Calendar.YEAR}, tIncrement, 0) >= 0)
            increment = tIncrement;
        else increment = Calendar.HOUR;
    }

    /**
     * This sets the range of allowed Dates.
     *
     * @param newMinDateTime the minimum allowed date/time.
     * @param newMaxDateTime the maximum allowed date/time.
     * @return an error message ("" if no error)
     */
    public String setMinMaxDateTime(GregorianCalendar newMinDateTime, 
            GregorianCalendar newMaxDateTime) {
        if (newMinDateTime != null)
            minDateTime = newMinDateTime;

        if (newMaxDateTime != null)
            maxDateTime = newMaxDateTime;

        //are the date's reversed?
        if (maxDateTime.before(minDateTime)) {
            //swap them
            GregorianCalendar temp = minDateTime;
            minDateTime = maxDateTime;
            maxDateTime = temp;
        }

        return "";
    }    

    /**
     * This finds the the closest valid date.
     * If dateValue is invalid, it is set to the maxDatetime.
     *
     * @param dateValue
     * @return a nicely formatted valid date
     */
    public String findClosestDate(String dateValue) {
        //make gc in range
        GregorianCalendar gc;
        try {
            gc = Calendar2.parseISODateTimeZulu(dateValue); //zulu avoids time zone trouble
        } catch (Exception e) {
            gc = null;
        }
        if (gc == null || //occurs if dateValue has invalid format
            gc.after(maxDateTime))
            gc = (GregorianCalendar)maxDateTime.clone();
        else if (gc.before(minDateTime))
            gc = (GregorianCalendar)minDateTime.clone();

        return format(gc);
    }

    /** 
     * This formats the date/time based on the current SHOW[mode].
     *
     * @param dateTime
     * @return a correctly formatted (not necessarily valid) value.
     */
    public String format(GregorianCalendar dateTime) {
        if (dateTime == null)
            return "";
        StringBuilder sb = new StringBuilder();
        if (showYears  ) sb.append(dateTime.get(YEAR));
        if (showMonths ) sb.append("-" + String2.zeroPad("" + (dateTime.get(MONTH) + 1), 2)); 
        if (showDates  ) sb.append("-" + String2.zeroPad("" + dateTime.get(DATE), 2));
        if (showHours  ) sb.append(" " + String2.zeroPad("" + dateTime.get(HOUR_OF_DAY), 2));
        if (showMinutes) sb.append(":" + String2.zeroPad("" + dateTime.get(MINUTE), 2));
        if (showSeconds) sb.append(":" + String2.zeroPad("" + dateTime.get(SECOND), 2));
        return sb.toString();
    }

    /**
     * This returns the HTML code for an HTML control 
     * which can be placed in an HTML
     * form and which allows the user to view and change the attribute.
     * 
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control which allows the user to 
     *     view and change the attribute.
     */
    public String getControl(String value) {

        //this fixes: 'enter' acted like next 'submit' button
        String submit = enterSubmitsForm? "if (enter(event)) submitForm(this.form); " : "";

        GregorianCalendar gc = getValidGC(value); 
        String formattedValue = format(gc);
        StringBuilder sb = new StringBuilder();
        //use hard-coded <table>, not the properties version, which may change
        sb.append("\n        <table class=\"erd\" style=\"width:2%;\">\n"); //padding=0. 'width' solves gap betwen buttons in opera 9
        sb.append("          <tr>\n");
        sb.append(
            "            <td><input type=\"text\" name=\"" + name + "\"\n" +
            "        onkeypress=\"" + submit + "return !enter(event);\"\n" + //supress Enter->submit
            "              value=\"" + formattedValue + "\" " +
                    "size=\"" + Math.max(5, formattedValue.length() + 2) + "\"\n" +
            "              title=\"Specify a date" + 
                     (showHours? "Time": "") + 
                     " between " + 
                     format(minDateTime) + " and " + format(maxDateTime) + ". " +
                     (enterSubmitsForm? "  " + parent.getPressEnterToSubmit() : "") + "\" " +
                     style + "></td>\n" +
            "          ");

        //display the buttons
        if (buttonsVisible) {
            //set non-visible fields to min value
            //String2.log(" edit value=" + value + " closest=" + findClosestDate(value));
            GregorianCalendar truncMin = (GregorianCalendar)minDateTime.clone();
            GregorianCalendar truncMax = (GregorianCalendar)maxDateTime.clone();
            value = findClosestDate(value); //should return valid value
            GregorianCalendar validDateTime; 
            try {
                validDateTime = Calendar2.parseISODateTimeZulu( //zulu avoids time zone issues
                    findClosestDate(value)); //findClosestDate should return valid value
            } catch (Exception e) {
                validDateTime = getValidGC(value);
            }
            if (!showSeconds) {truncMin.set(SECOND, 0); truncMax.set(SECOND, 0); validDateTime.set(SECOND, 0);}
            if (!showMinutes) {truncMin.set(MINUTE, 0); truncMax.set(MINUTE, 0); validDateTime.set(MINUTE, 0);}
            if (!showHours  ) {truncMin.set(HOUR_OF_DAY,0);truncMax.set(HOUR_OF_DAY,0);validDateTime.set(HOUR_OF_DAY,   0);}
            if (!showDates  ) {truncMin.set(DATE,   1); truncMax.set(DATE,   1); validDateTime.set(DATE,   1);}
            if (!showMonths ) {truncMin.set(MONTH,  0); truncMax.set(MONTH,  0); validDateTime.set(MONTH,  0);}
            //year always visible
            //make changes take effect
            truncMin.get(YEAR); //force recalculations
            truncMax.get(YEAR); //force recalculations
            validDateTime.get(YEAR); //force recalculations
            //String2.log(" edit truncMin=" + Calendar2.formatAsISODateTimeT(truncMin) + 
            //    " truncMax=" + Calendar2.formatAsISODateTimeT(truncMax) + 
            //    " validDateTime=" + Calendar2.formatAsISODateTimeT(validDateTime));
            if (validDateTime.before(truncMin)) validDateTime = truncMin;
            if (validDateTime.after( truncMax)) validDateTime = truncMax;
            String validatedValue = format(validDateTime);

            submit = enterSubmitsForm? " submitForm(this.form);" : "";
            sb.append("  <td>&nbsp;</td>\n"); 
            //"type=button" buttons don't submit form; they just run their JavaScripts

            //rewind button
            sb.append(    
                "            <td><input value=\"|&lt;\" type=\"button\" " + 
                    (validDateTime.compareTo(truncMin) <= 0? "disabled " : "") +
                    "title=\"Select the first available date/time.\"\n" +
                "              onMouseUp=\"this.form." + name + ".value='" + format(truncMin) + "';" +
                    submit + "\"" +
                    style + "></td>\n");

            //previous    decrement
            GregorianCalendar tDateTime = (GregorianCalendar)validDateTime.clone();
            tDateTime.add(increment, -1);
            sb.append(    
                "            <td><input value=\"-\" type=\"button\" " + 
                    (validDateTime.compareTo(truncMin) <= 0? "disabled " : "") +
                    "title=\"Select the previous date/time.\"\n" +
                "              onMouseUp=\"this.form." + name + ".value='" + format(tDateTime) + "';" +
                    submit + "\"" +
                    style + "></td>\n");

            //next    increment
            tDateTime = (GregorianCalendar)validDateTime.clone();
            tDateTime.add(increment, 1);
            sb.append(    
                "            <td><input value=\"+\" type=\"button\" " + 
                    (validDateTime.compareTo(truncMax) >= 0? "disabled " : "") +
                    "title=\"Select the next date/time.\"\n" +
                "              onMouseUp=\"this.form." + name + ".value='" + format(tDateTime) + "';" +
                    submit + "\"" +
                    style + "></td>\n");

            //fast forward button
            sb.append(    
                "            <td><input value=\"&gt;|\" type=\"button\" " + 
                    (validDateTime.compareTo(truncMax) >= 0? "disabled " : "") +
                    "title=\"Select the last available date/time.\"\n" +
                "              onMouseUp=\"this.form." + name + ".value='" + format(truncMax) + "';" +
                    submit + "\"" + 
                    style + "></td>\n");

        }

        //td good for spacing even if suffix is ""
        sb.append("            <td>" + (suffix.length() == 0? "&nbsp;" : suffix) + "</td>\n");
        sb.append("          </tr>\n");
        //use hard-coded </table>, not the properties version, which may change
        sb.append("        </table>\n");
        sb.append("      ");
        return sb.toString();
    }

    /**
     * Get a valid gc value (from value, or default, or current datetime).
     *
     * @param dateValue
     * @return a valid gc value (this won't throw an exception)
     */
    public GregorianCalendar getValidGC(String dateValue) {

        try {
            return Calendar2.parseISODateTimeZulu(dateValue);
        } catch (Exception e) {
            try {
                return Calendar2.parseISODateTimeZulu(getDefaultValue());
            } catch (Exception e2) {
                return Calendar2.newGCalendarZulu(); //zulu: avoid time zone problems
            }
        }
    }


}