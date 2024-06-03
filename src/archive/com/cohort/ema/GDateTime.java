/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying some or all of a 
 * Gregorian Calendar date and/or a time using different types of components.
 *
 * Internally, and via some methods, this class uses GregorianCalendar
 * objects, not Date objects. 
 * Note that you can get a java.util.Date object from
 * a GregorianCalendar object via gregorianCalendar.getTime().
 * See the Calendar2.newGCalendarLocal/Zulu options for making a 
 * GregorianCalendar object from a java.util.Date object.
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
 * <li> name+".mode" (a String matching one of the "MODES" constant Strings below,
 *      default = "MODE_YMDHMS") 
 *      determines which parts of the dateTime are displayed
 * <li> name+".showAmPm" ("true" or "false", default = "true")
 *      determines if the GUI component displays am/pm and hours 12, 1-11,
 *      or if it displays hours 0..23. 
 *      If Hour is not being displayed, this setting is irrelevant.
 * <li> name+".buttonsVisible" ("true" or "false", default = "true")
 *      determines if the first,+,-,last buttons should be visible.
 * </ul>
 *
 */
public abstract class GDateTime extends EmaAttribute {
//future static convenience method to get a Date from other string formats
//  or convert a Date to other string formats
//**future should local fields (hour, min, sec) be names .hour or _Hour?

    protected GregorianCalendar
        minDateTime = Calendar2.newGCalendarZulu(),
        maxDateTime = Calendar2.newGCalendarZulu();
    protected int mode;
    protected boolean showAmPm;
    protected int hourToUse; //calculated from showAmPm
    protected String dateError;
    protected boolean buttonsVisible;

    protected final static String className = "EmaGDateTime";
    protected final static int
        SHOW_YEAR   = 1,
        SHOW_MONTH  = 2,
        SHOW_DATE   = 4,
        SHOW_HOUR   = 8,
        SHOW_MINUTE = 16,
        SHOW_SECOND = 32;
    public final static int
        MODE_YMDHMS = 0,
        MODE_YMDHM  = 1,
        MODE_YMDH   = 2,
        MODE_YMD    = 3,
        MODE_YM     = 4,
        MODE_MD     = 5, //Feb has 29 days
        MODE_HM     = 6,
        MODE_Y      = 7,
        MODE_M      = 8,
        MODE_D      = 9,
        MODE_HMS    = 10;     
    public final static String MODES[] = {
        "MODE_YMDHMS",
        "MODE_YMDHM",
        "MODE_YMDH",
        "MODE_YMD",
        "MODE_YM",
        "MODE_MD", 
        "MODE_HM",
        "MODE_Y",
        "MODE_M",
        "MODE_D",
        "MODE_HMS"};     
    protected final static int[] SHOW = { //parallel MODE_XXXX constants
        SHOW_YEAR + SHOW_MONTH + SHOW_DATE + SHOW_HOUR + SHOW_MINUTE + SHOW_SECOND,
        SHOW_YEAR + SHOW_MONTH + SHOW_DATE + SHOW_HOUR + SHOW_MINUTE,
        SHOW_YEAR + SHOW_MONTH + SHOW_DATE + SHOW_HOUR,
        SHOW_YEAR + SHOW_MONTH + SHOW_DATE,
        SHOW_YEAR + SHOW_MONTH,
        SHOW_MONTH + SHOW_DATE, 
        SHOW_HOUR + SHOW_MINUTE,
        SHOW_YEAR,
        SHOW_MONTH,
        SHOW_DATE,
        SHOW_HOUR + SHOW_MINUTE + SHOW_SECOND};     

    //useful static variables
    public final static int YEAR        = Calendar.YEAR;
    public final static int MONTH       = Calendar.MONTH;
    public final static int DATE        = Calendar.DATE;
    public final static int HOUR        = Calendar.HOUR;  //0..11
    public final static int HOUR_OF_DAY = Calendar.HOUR_OF_DAY; //0..23
    public final static int MINUTE      = Calendar.MINUTE;
    public final static int SECOND      = Calendar.SECOND;
    public final static int AM_PM       = Calendar.AM_PM;
    public static String YMDOrder = "Y-M-D"; //permutation of these letters
//FUTURE: set Preferences should modify YMDOrder as desired
//or set automatically based on locale info

    //for thread safety, always use:  synchronized(<itself>) {<use...>}
    //do before defaultValue=  (below)
    private final static SimpleDateFormat isoDateTimeFormat = 
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private final static SimpleDateFormat isoDateFormat = 
        new SimpleDateFormat("yyyy-MM-dd");

    public static String[] MONTHS = new String[] {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    public final static String[] AM_PM_ARRAY = new String[] {
        "am", "pm"};  //0=am,1=pm matches GregorianCalendar constants
    public static String defaultValue = 
        Calendar2.formatAsISODateTimeSpace(Calendar2.newGCalendarZulu()); //zulu avoid time zone issues


    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public GDateTime(EmaClass parent, String name) {
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
        setMode(classRB2.getString(name + ".mode", MODES[0]));
        setShowAmPm(classRB2.getBoolean(name + ".showAmPm", true));
        setButtonsVisible(classRB2.getBoolean(name + ".buttonsVisible", true));

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
     * This determines which date/time fields get displayed.
     *
     * @param tMode e.g., "MODE_YMD"
     */
    public void setMode(String tMode) {
        mode = Math.max(0, String2.indexOf(MODES, tMode));
        setShowAmPm(showAmPm);
    }

    /**
     * This lets you specify if am/pm should be visible.
     * Do this after setMode().
     *
     * @param tShowAmPm
     */
    public void setShowAmPm(boolean tShowAmPm) {
        showAmPm = ((SHOW[mode] & SHOW_HOUR) == 0)?
            false : //hour not shown? don't show amPm
            tShowAmPm;        
        hourToUse = showAmPm? HOUR : HOUR_OF_DAY;
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
     * This removes all of this attribute's values from the session.
     *
     * @param session is the session associated with a user
     */
    public void removeValue(HttpSession session) {
        String baseName = parent.getFullClassName() + "." + name;
        int show = SHOW[mode];
        if ((show & SHOW_YEAR) != 0) 
            session.removeAttribute(baseName + "_Year");
        if ((show & SHOW_MONTH) != 0) 
            session.removeAttribute(baseName + "_Month"); 
        if ((show & SHOW_DATE) != 0)  
            session.removeAttribute(baseName + "_Date");
        if ((show & SHOW_HOUR) != 0) 
            session.removeAttribute(baseName + "_Hour");
        if ((show & SHOW_MINUTE) != 0)  
            session.removeAttribute(baseName + "_Minute");
        if ((show & SHOW_SECOND) != 0)  
            session.removeAttribute(baseName + "_Second");
        if (showAmPm)  
            session.removeAttribute(baseName + "_AmPm");
    }


    /**
     * This gets the value of one of the EmaAttributes.
     *
     * @param session is the session associated with a user
     * @return the value (a full ISO datetime, in String form) 
     * (or the default, if unexpectedly not in the session)
     */
    public String getValue(HttpSession session) {

        //build the newDateTime
        String baseName = parent.getFullClassName() + "." + name;
        GregorianCalendar newDateTime = getValidGC(super.getValue(session));
        int show = SHOW[mode];
        if ((show & SHOW_YEAR) != 0) {
            //String2.log(baseName + "_Year=" + (String)session.getAttribute(baseName + "_Year"));
            try {
                newDateTime.set(YEAR, Integer.parseInt(
                    (String)session.getAttribute(baseName + "_Year")));
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        if ((show & SHOW_MONTH) != 0) {
            //String2.log(baseName + "_Month=" + (String)session.getAttribute(baseName + "_Month"));
            try {
                String m = ((String)session.getAttribute(baseName + "_Month")).trim();
                //is it a number?
                if (Character.isDigit(m.charAt(0)))
                    newDateTime.set(MONTH, Integer.parseInt(m) -1); //0..
                //or a text Month?
                else {
                    for (int month = 0; month < 12; month++) {
                        if (MONTHS[month].equals(m)) {
                           newDateTime.set(MONTH, month); //0..
                           break;
                        }
                    }
                }
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        //do date after year and month since valid nDays/month varies
        if ((show & SHOW_DATE) != 0) { 
            //String2.log(baseName + "_Date=" + (String)session.getAttribute(baseName + "_Date"));
            try {
                newDateTime.set(DATE, Integer.parseInt(
                    (String)session.getAttribute(baseName + "_Date"))); //1..
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        if ((show & SHOW_HOUR) != 0) {
            //String2.log(baseName + "_Hour=" + (String)session.getAttribute(baseName + "_Hour"));
            try {
                int h = Integer.parseInt((String)session.getAttribute(baseName + "_Hour"));
                if (showAmPm && h == 12) 
                    h = 0;
                newDateTime.set(hourToUse, h); 
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        if ((show & SHOW_MINUTE) != 0) {
            //String2.log(baseName + "_Minute=" + (String)session.getAttribute(baseName + "_Minute"));
            try {
                newDateTime.set(MINUTE, Integer.parseInt(
                    (String)session.getAttribute(baseName + "_Minute")));
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        if ((show & SHOW_SECOND) != 0) {
            //String2.log(baseName + "_Second=" + (String)session.getAttribute(baseName + "_Second"));
            try {
                newDateTime.set(SECOND, Integer.parseInt(
                    (String)session.getAttribute(baseName + "_Second")));
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        if (showAmPm) {
            //String2.log(baseName + "_AmPm=" + (String)session.getAttribute(baseName + "_AmPm"));
            try {
                newDateTime.set(AM_PM, 
                    ((String)session.getAttribute(baseName + "_AmPm")).equals(AM_PM_ARRAY[0])?
                        0 : 1);
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }

        //make sure it is valid
        validate(newDateTime);

        //return String version
        //String2.log(name + " GDateTime.getValue = " + calendarToString(newDateTime));
        return Calendar2.formatAsISODateTimeSpace(newDateTime);
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
     *
     * @param dateValue if invalid, it is assumed to be the default date
     *    (or current datetime if default is invalid)
     */
    public String findClosestDate(String dateValue) {
        GregorianCalendar gc = getValidGC(dateValue);
        if (gc.getTimeInMillis() <= minDateTime.getTimeInMillis())
            gc = (GregorianCalendar)minDateTime.clone();
        else if (gc.getTimeInMillis() >= maxDateTime.getTimeInMillis())
            gc = (GregorianCalendar)maxDateTime.clone();
        else {
            int show = SHOW[mode];
            if ((show & SHOW_SECOND) == 0) 
                gc.set(Calendar2.SECOND, 0);
            if ((show & SHOW_MINUTE) == 0) 
                gc.set(Calendar2.MINUTE, 0);
            if ((show & SHOW_HOUR) == 0) 
                gc.set(Calendar2.HOUR, 0);
        }

        return Calendar2.formatAsISODateTimeSpace(gc);
    }

    //The subclass should implement getControl

    /**
     * This tests if String is a properly formatted date between minDateTime
     * and maxDateTime.
     *
     * @param aValue
     * @return an error string ("" if the date is between min and max)
     */
    public String isValid(String aValue) {
        //no string?
        if (aValue == null || aValue.length() == 0)
            return required? parent.createRequiredTextError(name) : "";

        try { 
            GregorianCalendar gc = Calendar2.parseISODateTimeZulu(aValue);
            return isValid(gc);
        } catch (Exception e) {
            return dateError;
        }
    }

    /**
     * This tests if the current date is between min and max.
     *
     * @param dateTime
     * @return an error String 
     *    ("" if dateTime is between minDateTime and maxDateTime)
     */
    public String isValid(GregorianCalendar dateTime) {
        if (dateTime.before(minDateTime) ||
            dateTime.after(maxDateTime))
            return dateError;
        return "";
    }

    /**
     * This makes sure dateTime is valid (between minDateTime and maxDateTime)
     * by setting it to min or max if needed.
     *
     * @param dateTime the dateTime that will be valid afterwards
     */
    public void validate(GregorianCalendar dateTime) {
        long dateTimeMillis = dateTime.getTimeInMillis();
        long tl = minDateTime.getTimeInMillis();
        if (dateTimeMillis < tl)
            dateTime.setTimeInMillis(tl);
        tl = maxDateTime.getTimeInMillis();
        if (dateTimeMillis > tl)
            dateTime.setTimeInMillis(tl);
        dateTime.get(YEAR); //avoid trouble by forcing recalculation
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

    /**
     * Whether this class is a JSP or a servlet, this handles
     * a "request" from a user, storing incoming attributes
     * as session values.
     *
     * @param request 
     * @return true if all the values for this attribute are valid
     */
    public boolean processRequest(HttpServletRequest request) {   
        if (isTransient) 
            return true;

        //Use a different approach than in EmaAttribute since this has multiple values:
        //  start with defaults from session, then replace with whatever is in the request.
        //  Eventually, check validity and store back in session.
        //yes, use short name for request.getParameter
        HttpSession session = request.getSession();
        GregorianCalendar dateTime = getValidGC(getValue(session)); //throws Exception if trouble
        //String2.log(
        //    name + " GDateTime.processRequest from Session = " + getValue(session));

        //try to get the attribute's values from the request and save it in dateTime
        int show = SHOW[mode];
        if ((show & SHOW_YEAR) != 0) {
            try {
                dateTime.set(YEAR, 
                    Integer.parseInt((String)request.getParameter(name + "_Year")));
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }

        if ((show & SHOW_MONTH) != 0) {
            try {
                String m = ((String)request.getParameter(name + "_Month")).trim();
                //is it a number?
                if (Character.isDigit(m.charAt(0)))
                    dateTime.set(MONTH, Integer.parseInt(m) -1); //0..
                //or a text Month?
                else {
                    for (int month = 0; month < 12; month++) {
                        if (MONTHS[month].equals(m)) {
                           dateTime.set(MONTH, month); //0..
                           break;
                        }
                    }
                }
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }

        if ((show & SHOW_DATE) != 0) {
            try {
                dateTime.set(DATE, 
                    Integer.parseInt((String)request.getParameter(name + "_Date"))); //both 1..
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }

        if ((show & SHOW_HOUR) != 0) {
            try {
                int h = Integer.parseInt((String)request.getParameter(name + "_Hour"));
                if (showAmPm && h == 12)
                    h = 0;
                dateTime.set(hourToUse, h); 
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }

        if ((show & SHOW_MINUTE) != 0) {
            try {
                dateTime.set(MINUTE, 
                    Integer.parseInt((String)request.getParameter(name + "_Minute")));
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }

        if ((show & SHOW_SECOND) != 0) {
            try {
                dateTime.set(SECOND, 
                    Integer.parseInt((String)request.getParameter(name + "_Second")));
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }


        if (showAmPm) {
            try {
                dateTime.set(AM_PM, 
                    ((String)request.getParameter(name + "_AmPm")).equals(AM_PM_ARRAY[0])?
                    0 : 1);
            } catch (Exception e) {
                String2.log(MustBe.throwable(className, e));
            }
        }
        
        //save in session  (this ensures it is valid)
        setValue(session, Calendar2.formatAsISODateTimeSpace(dateTime));
        //String2.log(name + " GDateTime.processRequest after reading request = " + 
        //    calendarToString(dateTime));

        //return isValid
        return isValid(dateTime).length() == 0;   
    }

}