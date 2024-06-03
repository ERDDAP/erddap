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
 * This class holds the properties for displaying a date
 * and/or a time using drop-down lists (separate HTML Select 
 * components for year, month, date, ...).
 * The value string is always an iso formatted date[time] (e.g., 2006-01-21 23:59:59).
 * Currently, this does not support buttons visible.
 */
public class EmaDateTimeSelect extends GDateTime {

    private String suffix = "";

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaDateTimeSelect(EmaClass parent, String name) {
        super(parent, name);
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        suffix = classRB2.getString(name + ".suffix", "");
    }


    /**
     * This adds attribute's values to the session.
     * If the value isn't valid, it is interpreted as current time.
     *
     * @param session is the session associated with a user
     * @param value the value to be added to the session
     *    (an iso date[Time]).
     *    If value is bad, it is converted to devault value, 
     *    or if that is bad to current datetime.
     */
    public void setValue(HttpSession session, String value) {
        String baseName = parent.getFullClassName() + "." + name;
        GregorianCalendar dateTime = getValidGC(value);

        //ensure valid
        validate(dateTime);

        int show = SHOW[mode];
        if ((show & SHOW_YEAR) != 0) 
            session.setAttribute(baseName + "_Year", "" + dateTime.get(YEAR));
        //show month as string
        if ((show & SHOW_MONTH) != 0) 
            session.setAttribute(baseName + "_Month", MONTHS[dateTime.get(MONTH)]); //both 0..
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
     * @param value is the value of this attribute, as stored in the session
     *     (an iso date[time]).
     *     If should be already valid. If not, it is treated as default,
     *     or if that is bad, current datetime.
     * @return the HTML code for a control which allows the user to 
     *     view and change the attribute.
     */
    public String getControl(String value) {
        //make a table with the Select controls in one row
        StringBuilder sb = new StringBuilder();
        //use hard-coded <table>, not the properties version, which may change
        sb.append("\n        <table class=\"erd\">\n"); //padding=0
        sb.append("          <tr>\n");
        
        GregorianCalendar dateTime = getValidGC(value);
        int show = SHOW[mode];
        int selected;
        int selectedYear = dateTime.get(YEAR);
        int selectedMonth = dateTime.get(MONTH); 
        int selectedDate = dateTime.get(DATE); 
        int selectedHour = dateTime.get(HOUR); 
        int selectedMinute = dateTime.get(MINUTE); 
        int selectedSecond = dateTime.get(SECOND); 
        int lastYearIndex = 0, lastMonthIndex = 0, lastDateIndex = 0,
            lastHourIndex = 0, lastMinuteIndex = 0, lastSecondIndex = 0;
        int selectedYearIndex = 0, selectedMonthIndex = 0, selectedDateIndex = 0,
            selectedHourIndex = 0, selectedMinuteIndex = 0, selectedSecondIndex = 0;
        boolean somethingVisible = false;
        for (int i = 0; i < YMDOrder.length(); i++) {
            char ch = YMDOrder.charAt(i);
            if (ch == 'Y') {
                if ((show & SHOW_YEAR) != 0) {
                    sb.append("            <td>\n" +
                              "              <select name=\"" + 
                              name + "_Year\" title=\"Year\"" + 
                              getOnChangeSubmitsFormHTML() + 
                              style +
                              ">\n");
                    int min = minDateTime.get(YEAR);
                    int max = maxDateTime.get(YEAR);
                    lastYearIndex = max - min;
                    selectedYearIndex = selectedYear - min;
                    for (int year = min; year <= max; year++)
                        sb.append("                <option" + 
                                  (year == selectedYear? " selected=\"selected\">" : ">") + 
                                   year + "</option>\n");
                    sb.append("              </select>\n" + 
                              "            </td>\n");
                    somethingVisible = true;
                }
            } else if (ch == 'M') {
                if ((show & SHOW_MONTH) != 0) {
                    //all month numbers are 0..
                    int min = 0;
                    int max = 11;
                    if (enterSubmitsForm) {
                        //limit months shown if first year or last year
                        if (selectedYear == minDateTime.get(YEAR))
                            min = minDateTime.get(MONTH);
                        if (selectedYear == maxDateTime.get(YEAR))
                            max = maxDateTime.get(MONTH);
                    }
                    lastMonthIndex = max - min;
                    selectedMonthIndex = selectedMonth - min;
    
                    //display the options
                    sb.append("            <td>\n" +
                              "              <select name=\"" + 
                              name + "_Month\" title=\"Month\"" + 
                              getOnChangeSubmitsFormHTML() + 
                              style +
                              ">\n");
                    for (int month = min; month <= max; month++)
                        sb.append("                <option" + 
                                  (month == selectedMonth? " selected=\"selected\">" : ">") + 
                                  MONTHS[month] + "</option>\n");
                    sb.append("              </select>\n" + 
                              "            </td>\n");
                    somethingVisible = true;
                }                
            } else if (ch == 'D') {
                if ((show & SHOW_DATE) != 0) {
                    //all date numbers are 1..
                    int min = 1;
                    int max = 31;
                    if (enterSubmitsForm) {
                        //limit dates shown if first month/year or last month/year
                        max = dateTime.getActualMaximum(DATE);
                        if (selectedYear  == minDateTime.get(YEAR) &&
                            selectedMonth == minDateTime.get(MONTH))
                            min = minDateTime.get(DATE);
                        if (selectedYear  == maxDateTime.get(YEAR) &&
                            selectedMonth == maxDateTime.get(MONTH))
                            max = maxDateTime.get(DATE);
                    }
                    lastDateIndex = max - min;
                    selectedDateIndex = selectedDate - min;

                    //display the options
                    sb.append("            <td>\n" +
                              "              <select name=\"" + 
                              name + "_Date\" title=\"Date\"" + 
                              getOnChangeSubmitsFormHTML() + 
                              style +
                              ">\n"); 
                    for (int date = min; date <= max; date++)
                        sb.append("                <option" + 
                                  (date == selectedDate? " selected=\"selected\">" : ">") + 
                                  date + "</option>\n");
                    sb.append("              </select>\n" + 
                              "            </td>\n");
                    somethingVisible = true;
                }
            } else { //don't display literal characters
                //if (somethingVisible)
                //    sb.append(
                //        "        <td>" + ch + "</td>\n");
            }
        }
        if ((show & SHOW_HOUR) != 0) {
            //add a spacer 
            if (somethingVisible)
                sb.append("            <td>&nbsp;&nbsp;</td>\n");

            selected = dateTime.get(hourToUse); //0..
            sb.append("            <td>\n" +
                      "              <select name=\"" + 
                      name + "_Hour\" title=\"Hour\"" + 
                      getOnChangeSubmitsFormHTML() + 
                      style +
                      ">\n");
            //eek! min and max should be adjusted by min/maxDateTime if relevant
            if (showAmPm) {
                lastHourIndex = 11;
                selectedHourIndex = selectedHour;
                for (int hour = 0; hour < 12; hour++) {
                    
                    String hourString = hour == 0? 
                        "12" : 
                        String2.zeroPad("" + hour, 2);
                    sb.append("                <option" + 
                              (hour == selected? " selected=\"selected\">" : ">") + 
                              hourString + "</option>\n");
                }
            } else {
                lastHourIndex = 23;  //this should adjust for min/maxDateTime
                selectedHourIndex = selectedHour;
                for (int hour = 0; hour < 24; hour++)
                    sb.append("                <option" + 
                              (hour == selected? " selected=\"selected\">" : ">") + 
                              String2.zeroPad("" + hour, 2) + "</option>\n");
            }
            sb.append("              </select>\n" + 
                      "            </td>\n");
        }
        if ((show & SHOW_MINUTE) != 0) {
            selected = dateTime.get(MINUTE);
            sb.append("            <td>\n" +
                      "              <select name=\"" + 
                      name + "_Minute\" title=\"Minute\"" + 
                      getOnChangeSubmitsFormHTML() + 
                      style +
                      ">\n");
            //eek! min and max should be adjusted by min/maxDateTime if relevant
            lastMinuteIndex = 59; //this should adjust for min/maxDateTime
            selectedMinuteIndex = selectedMinute;
            for (int minute = 0; minute < 60; minute++)
                sb.append("                <option" + 
                          (minute == selected? " selected=\"selected\">" : ">") + 
                          String2.zeroPad("" + minute, 2) + "</option>\n");
            sb.append("              </select>\n" + 
                      "            </td>\n");
        }
        if ((show & SHOW_SECOND) != 0) {
            selected = dateTime.get(SECOND);
            sb.append("            <td>\n" +
                      "              <select name=\"" + 
                      name + "_Second\" title=\"Second\"" + 
                      getOnChangeSubmitsFormHTML() + 
                      style +
                      ">\n");
            //eek! min and max should be adjusted by min/maxDateTime if relevant
            lastSecondIndex = 59; //this should adjust for min/maxDateTime
            selectedSecondIndex = selectedSecond;
            for (int second = 0; second < 60; second++)
                sb.append("                <option" + 
                          (second == selected? " selected=\"selected\">" : ">") + 
                          String2.zeroPad("" + second, 2) + "</option>\n");
            sb.append("              </select>\n" + 
                      "            </td>\n");
        }
        if (showAmPm) {
            selected = dateTime.get(AM_PM);
            sb.append("            <td>\n" +
                      "              <select name=\"" + 
                      name + "_AmPm\" title=\"am or pm\"" + 
                      getOnChangeSubmitsFormHTML() + 
                      style +
                      ">\n");
            for (int amPm = 0; amPm < 2; amPm++)
                sb.append("                <option" + 
                          (amPm == selected? " selected=\"selected\">" : ">") + 
                          AM_PM_ARRAY[amPm] + "</option>\n");
            sb.append("              </select>\n" + 
                      "            </td>\n");
        }

        //not finished; need to figure out how to specify next/previous min/maxDateTime
        // Trouble with '-' button: March 1 -> Feb (previous month) 30 (last date in March).
        // and Feb 30 -> March 2. So you are in a loop. 
        // Or Feb 1 -> Jan 28 (since you can't select 31 since it isn't a feb option).
        //The trouble is that it
        // is not aware of nDays per month; and even if it were, you can only set
        // to things visible on the screen.
        /*if (buttonsVisible) {
            String submit = enterSubmitsForm? " submitForm(this.form);" : "";
            sb.append("        <td>&nbsp;</td>\n"); 
            //"type=button" buttons don't submit form; they just run their JavaScripts

            //rewind button;   this may go before minTime, but validate will set it to minTime
            sb.append(    
                "        <td><input value=\"|&lt;\" type=button " + 
                    (dateTime.compareTo(minDateTime) <= 0? "disabled " : "") +
                    "title=\"Select the first available date/time.\"\n" +
                    "          onMouseUp=\"");
            if ((show & SHOW_YEAR)   != 0) sb.append("this.form." + name + "_Year.selectedIndex=0;");
            if ((show & SHOW_MONTH)  != 0) sb.append("this.form." + name + "_Month.selectedIndex=0;");
            if ((show & SHOW_DATE)   != 0) sb.append("this.form." + name + "_Date.selectedIndex=0;");
            if ((show & SHOW_HOUR)   != 0) sb.append("this.form." + name + "_Hour.selectedIndex=0;");
            if ((show & SHOW_MINUTE) != 0) sb.append("this.form." + name + "_Minute.selectedIndex=0;");
            if ((show & SHOW_SECOND) != 0) sb.append("this.form." + name + "_Second.selectedIndex=0;");
            if (showAmPm)                  sb.append("this.form." + name + "_AmPm.selectedIndex=0;");
            sb.append(submit + "\"></td>\n");

            //- button   
            //The 'selected' numbers are set here (not at user's computer) 
            //   so this only works with enterSubmitsForm=true.
            //This isn't perfect but does best possible.
            sb.append(
                "        <td><input value=\"-\" type=button " + 
                (dateTime.compareTo(minDateTime) <= 0? "disabled " : "") +
                "title=\"Select the previous item.\"\n" +
                "          onMouseUp=\"");
            //change smallest unit visible;   decrement smallest visible unit where selectedIndex > 0
            boolean done = false;
            if (!done && ((show & SHOW_SECOND) != 0)) {
                sb.append("this.form." + name + "_Second.selectedIndex");
                if (selectedSecondIndex > 0) {sb.append("-=1;"); done=true;
                } else {                      sb.append("=" + lastSecondIndex + ";");
                }
            }
            if (!done && ((show & SHOW_MINUTE) != 0)) {
                sb.append("this.form." + name + "_Minute.selectedIndex");
                if (selectedMinuteIndex > 0) {sb.append("-=1;"); done=true;
                } else {                      sb.append("=" + lastMinuteIndex + ";");
                }
            }
            if (!done && ((show & SHOW_HOUR)   != 0)) {
                sb.append("this.form." + name + "_Hour.selectedIndex");
                if (selectedHourIndex > 0)   {sb.append("-=1;"); done=true;
                } else {                      sb.append("=" + lastHourIndex + ";");
                }
            }
            if (!done && ((show & SHOW_DATE)   != 0)) {
                sb.append("this.form." + name + "_Date.selectedIndex");
                if (selectedDateIndex > 0)   {sb.append("-=1;"); done=true;
                } else {                      sb.append("=" + lastDateIndex + ";");
                }
            }
            if (!done && ((show & SHOW_MONTH)  != 0)) {
                sb.append("this.form." + name + "_Month.selectedIndex");
                if (selectedMonthIndex > 0)  {sb.append("-=1;"); done=true;
                } else {                      sb.append("=" + lastMonthIndex + ";");
                }
            }
            if (!done && ((show & SHOW_YEAR)   != 0)) {
                sb.append("this.form." + name + "_Year.selectedIndex");
                if (selectedYearIndex > 0)   {sb.append("-=1;"); done=true;
                } else {                      sb.append("=" + lastYearIndex + ";");
                }
            }
            sb.append(submit + "\"></td>\n");

            //+ button
            //The 'selected' numbers are set here (not at user's computer) 
            //   so this only works with enterSubmitsForm=true.
            //This isn't perfect but does best possible.
            sb.append(
                "        <td><input value=\"+\" type=button " + 
                (dateTime.compareTo(maxDateTime) >= 0? "disabled " : "") +
                "title=\"Select the previous item.\"\n" +
                "          onMouseUp=\"");
            //change smallest unit visible;   increment smallest visible unit where selectedIndex < lastIndex
            done = false;
            if (!done && ((show & SHOW_SECOND) != 0)) {
                sb.append("this.form." + name + "_Second.selectedIndex");
                if (selectedSecondIndex < lastSecondIndex) {sb.append("+=1;"); done=true;
                } else {                                    sb.append("=0;");
                }
            }
            if (!done && ((show & SHOW_MINUTE) != 0)) {
                sb.append("this.form." + name + "_Minute.selectedIndex");
                if (selectedMinuteIndex < lastMinuteIndex) {sb.append("+=1;"); done=true;
                } else {                                    sb.append("=0;");
                }
            }
            if (!done && ((show & SHOW_HOUR)   != 0)) {
                sb.append("this.form." + name + "_Hour.selectedIndex");
                if (selectedHourIndex   < lastHourIndex)   {sb.append("+=1;"); done=true;
                } else {                                    sb.append("=0;");
                }
            }
            if (!done && ((show & SHOW_DATE)   != 0)) {
                sb.append("this.form." + name + "_Date.selectedIndex");
                if (selectedDateIndex   < lastDateIndex)   {sb.append("+=1;"); done=true;
                } else {                                    sb.append("=0;");
                }
            }
            if (!done && ((show & SHOW_MONTH)  != 0)) {
                sb.append("this.form." + name + "_Month.selectedIndex");
                if (selectedMonthIndex  < lastMonthIndex)  {sb.append("+=1;"); done=true;
                } else {                                    sb.append("=0;");
                }
            }
            if (!done && ((show & SHOW_YEAR)   != 0)) {
                sb.append("this.form." + name + "_Year.selectedIndex");
                if (selectedYearIndex   < lastYearIndex)   {sb.append("+=1;"); done=true;
                } else {                                    sb.append("=0;");
                }
            }
            sb.append(submit + "\"></td>\n");

            //fast forward button;   this may go after maxTime, but validate will set it to maxTime
            sb.append(
                "        <td><input value=\"&gt;|\" type=button " + 
                    (dateTime.compareTo(maxDateTime) >= 0? "disabled " : "") +
                    "title=\"Select the last available date/time.\"\n" +
                "          onMouseUp=\"");
            if ((show & SHOW_YEAR)   != 0) sb.append("this.form." + name + "_Year.selectedIndex="  + lastYearIndex + ";");
            if ((show & SHOW_MONTH)  != 0) sb.append("this.form." + name + "_Month.selectedIndex=" + lastMonthIndex + ";");
            if ((show & SHOW_DATE)   != 0) sb.append("this.form." + name + "_Date.selectedIndex="  + lastDateIndex + ";");
            if ((show & SHOW_HOUR)   != 0) sb.append("this.form." + name + "_Hour.selectedIndex="  + lastHourIndex + ";");
            if ((show & SHOW_MINUTE) != 0) sb.append("this.form." + name + "_Minute.selectedIndex="+ lastMinuteIndex + ";");
            if ((show & SHOW_SECOND) != 0) sb.append("this.form." + name + "_Second.selectedIndex="+ lastSecondIndex + ";");
            if (showAmPm)                  sb.append("this.form." + name + "_AmPm.selectedIndex=1;");
            sb.append(submit + "\"></td>\n");
        } */

        //td good for spacing even if suffix is ""
        sb.append("            <td>" + (suffix.length() == 0? "&nbsp;" : suffix) + "</td>\n");
        sb.append("          </tr>\n");
        //use hard-coded </table>, not the properties version, which may change
        sb.append("        </table>\n");
        sb.append("      ");
        return sb.toString();
    }


}