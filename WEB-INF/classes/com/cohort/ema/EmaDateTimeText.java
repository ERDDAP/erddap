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
 * and/or a time using separate textfields for Y, M, D, h, m, s.
 * This does not support buttonsVisible.
 *
 */
public class EmaDateTimeText extends GDateTime {

    private String suffix = "";

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaDateTimeText(EmaClass parent, String name) {
        super(parent, name);
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        suffix = classRB2.getString(name + ".suffix", "");
    }

    /**
     * This adds the attribute's value to the session.
     *
     * @param session is the session associated with a user
     * @param value the value to be added to the session
     *    (if invalid, it is treated as default value 
     *    or, it that's bad, current date time)
     */
    public void setValue(HttpSession session, String value) {
        String baseName = parent.getFullClassName() + "." + name;
        GregorianCalendar dateTime = getValidGC(value);
        int show = SHOW[mode];
        if ((show & SHOW_YEAR) != 0) 
            session.setAttribute(baseName + "_Year", "" + dateTime.get(YEAR));
        //show month as number
        if ((show & SHOW_MONTH) != 0) 
            session.setAttribute(baseName + "_Month", "" + (dateTime.get(MONTH)+1)); 
        if ((show & SHOW_DATE) != 0)  
            session.setAttribute(baseName + "_Date", "" + dateTime.get(DATE));
        if ((show & SHOW_HOUR) != 0) {
            int h = dateTime.get(hourToUse);
            if (showAmPm && h == 0)
                h = 12;
            session.setAttribute(baseName + "_Hour", "" + h);
        }
        if ((show & SHOW_MINUTE) != 0)  
            session.setAttribute(baseName + "_Minute", "" + dateTime.get(MINUTE));
        if ((show & SHOW_SECOND) != 0)  
            session.setAttribute(baseName + "_Second", "" + dateTime.get(SECOND));
        if (showAmPm)  
            session.setAttribute(baseName + "_AmPm", AM_PM_ARRAY[dateTime.get(AM_PM)]);
    }

    /**
     * This returns the HTML code for an HTML control 
     * which can be placed in an HTML
     * form and which allows the user to view and change the attribute.
     * 
     * @param value is the value of this attribute, as stored in the session.
     *    It should be already valid. If not, it is treated as current datetime.
     * @return the HTML code for a control which allows the user to 
     *     view and change the attribute.
     */
    public String getControl(String value) {
        //make a table with the Select controls in one row
        StringBuilder sb = new StringBuilder();
        int show = SHOW[mode];
        int selected;
        GregorianCalendar dateTime = getValidGC(value);
        boolean somethingVisible = false;
        sb.append("\n");
        for (int i = 0; i < YMDOrder.length(); i++) {
            char ch = YMDOrder.charAt(i);
            if (ch == 'Y') {
                if ((show & SHOW_YEAR) != 0) {
                    sb.append(
                        "      <input type=\"text\" name=\"" + name + "_Year\"\n" +
                        "        value=\"" + dateTime.get(YEAR) + "\" " +
                            "onkeypress=\"return !enter(event)\"\n" +
                        "        title=\"Year (" + minDateTime.get(YEAR) + 
                            " - " + maxDateTime.get(YEAR)+ ")\" " +
                            "size=\"4\" maxlength=\"4\" " +
                            style + ">\n");
                    somethingVisible = true;
                }
            } else if (ch == 'M') {
                if ((show & SHOW_MONTH) != 0) {
                    sb.append(
                        "      <input type=\"text\" name=\"" + name + "_Month\"\n" +
                        "        value=\"" + 
                            (dateTime.get(MONTH) + 1) + "\" " + //0..->1..
                            "onkeypress=\"return !enter(event)\"\n" +
                        "        title=\"Month (1 - 12)\" size=\"2\" maxlength=\"2\" " +
                            style + ">\n");
                    somethingVisible = true;
                }
            } else if (ch == 'D') {
                if ((show & SHOW_DATE) != 0) {
                    sb.append(
                        "      <input type=\"text\" name=\"" + name + "_Date\"\n" +
                        "        value=\"" + dateTime.get(DATE) + "\" " +
                            "onkeypress=\"return !enter(event)\"\n" +
                        "        title=\"Date (1 - 31)\" size=\"2\" maxlength=\"2\" " +
                            style + ">\n");
                    somethingVisible = true;
                }
            } else { //a literal character
                if (somethingVisible)
                    sb.append(
                        "      " + ch + "\n");
            }
        }
        if ((show & SHOW_HOUR) != 0) {
            //add a spacer between date and hour if both used
            if (somethingVisible) 
                sb.append(
                    "      &nbsp;&nbsp;\n");
            int hour = dateTime.get(hourToUse); //0..
            String hourString = showAmPm && hour == 0? 
                "12" : 
                String2.zeroPad("" + hour, 2);
            String titleString = showAmPm? 
                "Hour (12, 1 - 11)" : 
                "Hour (0 - 23)";
            sb.append(
                "      <input type=\"text\" name=\"" + name + "_Hour\"\n" +
                "        value=\"" + hourString + "\" " +
                    "onkeypress=\"return !enter(event)\"\n" +
                "        title=\"" + titleString + "\" " +
                    "size=\"2\" maxlength=\"2\" " +
                    style + ">\n");
            somethingVisible = true;
        }
        if ((show & SHOW_MINUTE) != 0) {
            if (somethingVisible) 
                sb.append(
                    "      :\n");
            sb.append(
                "      <input type=\"text\" name=\"" + name + "_Minute\"\n" +
                "        value=\"" + 
                    String2.zeroPad("" + dateTime.get(MINUTE), 2) + "\" " +
                    "onkeypress=\"return !enter(event)\"\n" +
                "        title=\"Minute (0  - 59)\" " +
                    "size=\"2\" maxlength=\"2\" " +
                    style + ">\n");
            somethingVisible = true;
        }
        if ((show & SHOW_SECOND) != 0) {
            if (somethingVisible) 
                sb.append(
                    "        :\n");
            sb.append(
                "      <input type=\"text\" name=\"" + name + "_Second\"\n" +
                "        value=\"" + 
                    String2.zeroPad("" + dateTime.get(SECOND), 2) + "\" " +
                    "onkeypress=\"return !enter(event)\"\n" +
                "        title=\"Second (0 - 59)\" " +
                    "size=\"2\" maxlength=\"2\" " +
                    style + ">\n");
        }
        if (showAmPm) {
            if (somethingVisible) 
                sb.append(
                    "        &nbsp;\n");
            sb.append(
                "      <input type=\"text\" name=\"" + name + "_AmPm\"\n" +
                "        value=\"" + 
                    AM_PM_ARRAY[dateTime.get(AM_PM)] + "\" " +
                    "onkeypress=\"return !enter(event)\"\n" +
                "        title=\"am or pm\" size=\"2\" maxlength=\"2\" " +
                    style + ">\n");
        }

        // good for spacing even if suffix is ""
        sb.append("            " + (suffix.length() == 0? "&nbsp;" : suffix) + "\n");

        sb.append("    ");
        return sb.toString();
    }


}