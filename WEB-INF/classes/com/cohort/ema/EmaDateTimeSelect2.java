/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.Calendar2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying (see .display) 
 * a sorted list of ISO date (and optional time) options and 
 * giving the user two ways to
 * select the date: a drop-down list with all of the options,
 * or a series of drop-down lists (year, month, date, optional time).
 *
 * <p>It is not a problem if the order of the options changes 
 * from one version of the program to the next, or if new options are added
 * (since the current value is stored as a String).
 * But if the previously selected option is removed in a new version
 * of the program, the current selection will switch to the nearest option
 * (since they are sorted alphabetically).
 *
 * <p>If there are 0 options, no HTML control will be displayed.
 *
 * <p> Four buttons will be placed to the right of the main Select 
 *     control, marked "|&lt;", "-", "+", "&gt;|" which allow the user
 *     to increase or decrease the selection, or go to the first or last value.
 *     If "enterSubmitsForm", pressing the buttons also submits the form.
 *     These buttons require JavaScript to work.
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
 *      is the toolTip for this attribute.
 * <li> name+".suffix" (an HTML text string, default = "", suggestion = "Z")
 *      is the text printed to the right of the widget on the HTML form
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".value" (a String, default = "") 
 *     is the initial value stored by this object.
 * <li> name+".options" (a sorted, space-separated list of ISO 8601 strings; default = "")
 *     is the array of options. The strings may have a separator char
 *     (usually ' ' or 'T') and time values.
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
public class EmaDateTimeSelect2 extends EmaAttribute {

    protected String options[];
    protected String selectError;

    private final static String YEAR_NAME = "_Year", MONTH_NAME = "_Month",
        DAY_NAME = "_Day", TIME_NAME = "_Time";
    private String[] yearOptions, monthOptions, dayOptions, timeOptions;
    private String suffix = "";
    private String lastSelectedOption;

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaDateTimeSelect2(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        selectError = String2.substitute(parent.getRequiredSelectError(), 
            parent.removeColon(name), null, null); 

        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        options = classRB2.getStringArray(name + ".options", new String[]{""});
        suffix = classRB2.getString(name + ".suffix", "");

        //note that HTML Select does not force an item to be selected.   
    }

    /**
     * This adds the value to the session.
     *
     * @param session is the session associated with a user
     * @param value the value to be added to the session
     */
    public void setValue(HttpSession session, String value) {
        //String2.log("EmaDateTimeSelect2.setValue '" + value + "'");
        String baseName = parent.getFullClassName() + "." + name;

        String y = value.length() >= 4 ? value.substring(0,  4) : "";
        String m = value.length() >= 7 ? value.substring(5,  7) : "";
        String d = value.length() >= 10? value.substring(8, 10) : ""; 
        String t = value.length() >= 12? value.substring(11)    : "";

        session.setAttribute(baseName,              value);
        session.setAttribute(baseName + YEAR_NAME,  y);
        session.setAttribute(baseName + MONTH_NAME, m);
        session.setAttribute(baseName + DAY_NAME,   d);
        session.setAttribute(baseName + TIME_NAME,  t);
    }


    /**
     * This removes all of this attribute's values from the session.
     * EmaAttributes which use more than 1 control must overwrite this
     * to deal with separate sub-values.
     *
     * @param session is the session associated with a user
     */
    public void removeValue(HttpSession session) {
        String baseName = parent.getFullClassName() + "." + name;
        session.removeAttribute(baseName);
        session.removeAttribute(baseName + YEAR_NAME);
        session.removeAttribute(baseName + MONTH_NAME);
        session.removeAttribute(baseName + DAY_NAME);
        session.removeAttribute(baseName + TIME_NAME);
    }

    /**
     * This gets the value from widget #2, the separate ymdt SELECTs.
     *
     * @param session is the session associated with a user
     * @return the value (in String form) 
     * (or the default, if unexpectedly not in the session)
     */
    public String getValue2(HttpSession session) {
        String baseName = parent.getFullClassName() + "." + name;

        //get the connecting character, if any
        String value   = getValue(session);
        String connect = value.length() >= 11? value.substring(10, 11) : "";

        //get the widget #2 values
        String yValue = (String)session.getAttribute(baseName + YEAR_NAME);
        String mValue = (String)session.getAttribute(baseName + MONTH_NAME);
        String dValue = (String)session.getAttribute(baseName + DAY_NAME);
        String tValue = (String)session.getAttribute(baseName + TIME_NAME);
        if (yValue == null || mValue == null || dValue == null || tValue == null) {
            setValue(session, value);             
            return value;
        } else return yValue + "-" + mValue + "-" + dValue + connect + tValue;
    }

    /**
     * Whether this class is a JSP or a servlet, this handles
     * a "request" from a user, storing incoming attributes
     * as session values.
     * Since this has more than 1 control, I have to overwrite
     * super to handle widget#1 and widget#2.
     *
     * @param request 
     * @return true if all the values for this attribute are valid
     */
    public boolean processRequest(HttpServletRequest request) {   
        //get the oldValue1 before processing it
        HttpSession session = request.getSession();
        String oldValue = getValue(session); 

        //process widget #1
        boolean isValid1 = super.processRequest(request);

        //did widget #1 change?  if so, we're done
        //We'll process widget #2 only if widget #1 is unchanged. (ie widge#1 has priority)
        String newValue = getValue(session);
        if (!oldValue.equals(newValue))
            return isValid1;
        
        //get the oldValue2 before processing it
        String oldValue2 = getValue2(session); //do after super.processRequest so value (the default) is new value

        //get the connecting character, if any
        String value   = getValue(session);
        String connect = value.length() >= 11? value.substring(10, 11) : "";

        //process the widget #2 values
        String yValue = (String)request.getParameter(name + YEAR_NAME);
        String mValue = (String)request.getParameter(name + MONTH_NAME);
        String dValue = (String)request.getParameter(name + DAY_NAME);
        String tValue = (String)request.getParameter(name + TIME_NAME);
        if (yValue == null || mValue == null || dValue == null)
            return isValid1; //it wasn't on the form
        if (tValue == null) { //e.g., if date but not time options
            tValue = (String)session.getAttribute(name + TIME_NAME);
            if (tValue == null)
                tValue = "";
        }
        //String2.log("EmaDateTimeSelect2.processRequest yValue=" + yValue + 
        //    " mValue=" + mValue + " dValue=" + dValue + " tValue=" + tValue);
        String newValue2 = yValue + "-" + mValue + "-" + dValue + connect + tValue;

        //no change?
        if (oldValue2.equals(newValue2)) 
            return isValid1;

        //it was changed: record the change
        value = newValue2;
        setValue(session, value);
        if (verbose) String2.log("EmaAttribute.processRequest name=" + name + " value=" + value);

        String isValid = isValid(value);
        //if (isValid.length() > 0)
        //    String2.log("EmaAttribute.processRequest " + name +
        //        ".isValid(" + value + ") = " + isValid);
        return isValid.length() == 0;
    }

    /**
     * This resets the options (which all users will see).
     * If you have an arrayList or Vector instead of a String[], use arrayList.toArray.
     * 
     * WARNING: If you use this while the web application is running,
     * you should either use just one thread for the web application
     * (so this one change takes care of everything) or call this
     * for every request by every user.
     * 
     * @param options the new array of options
     *     (if null, treated as 0 elements)
     */
    public void setOptions(String options[]) {
        this.options = options == null? 
            new String[]{} :
            options;
        lastSelectedOption = null;  //this resets the cache of objects for widget #2
        //String2.log("EmaGDateTimeSelect2 setOptions n=" + this.options.length);
    }

    /**
     * This returns the requested option (or null if invalid).
     * 
     * @param which (0..)
     * @return option #which (or null if invalid)
     */
    public String getOption(int which) {
        return which < 0 || which >= options.length? null : options[which]; 
    }

    /**
     * This returns an array with the options.
     * 
     * @return an array of Strings
     */
    public String[] getOptions() {
        return options; 
    }

    /**
     * This implements the abstract createControl of EmaAttribute.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        if (options.length == 0)
            return "[no options]";

        StringBuilder sb = new StringBuilder();

        if (options.length > 1) 
            sb.append(    //td for <select>
                  "\n        <table class=\"erd\" style=\"width:2%;\"><tr>"); //padding=0. 'width' solves gap betwen buttons in opera 9

        //the main select widget
        String onChangeSubmitsFormHTML = getOnChangeSubmitsFormHTML();
        int selected = getDropDown(name, title, value, options, 
            onChangeSubmitsFormHTML, style, sb, options.length > 1);

        //the buttons
        if (options.length > 1) {
            String submit = enterSubmitsForm? " submitForm(this.form);" : "";
            sb.append(    
                //first
                //"type=button" buttons don't submit form; they just run their JavaScripts
                "        <td><input value=\"|&lt;\" type=button " + 
                    (selected == -1 || selected > 0? "" : "disabled ") +
                    "title=\"Select the first item.\"" +
                    style + "\n" +
                "          onMouseUp=\"this.form." + name + 
                    ".selectedIndex=0;" + submit + "\"></td>\n" +
                //-
                "        <td><input value=\"-\" type=button " + 
                    (selected == -1 || selected > 0? "" : "disabled ") +
                    "title=\"Select the previous item.\"" +
                    style + "\n" +
                "          onMouseUp=\"this.form." + name + 
                    ".selectedIndex-=1;" + submit + "\"></td>\n" +
                //+
                "        <td><input value=\"+\" type=button " + 
                    (selected == -1 || selected < options.length - 1? "" : "disabled ") +
                    "title=\"Select the next item.\"" +
                    style + "\n" +
                "          onMouseUp=\"this.form." + name + 
                    ".selectedIndex+=1;" + submit + "\"></td>\n" +
                //last
                "        <td><input value=\"&gt;|\" type=button " + 
                    (selected == -1 || selected < options.length - 1? "" : "disabled ") +
                    "title=\"Select the last item.\"" +
                    style + "\n" +
                "          onMouseUp=\"this.form." + name + 
                    ".selectedIndex=" + (options.length - 1) + 
                    ";" + submit + "\"></td>\n      ");
        }


        //the alternative YMDT select widgets
        //Note: I could make SUBMIT button so user enters y m d & t, then SUBMIT,
        //  but then t times would be inappropriate for the new y m d
        //  (although it would avoid generating a new map each time).
        //  Better to react to each change by updating all of the y m d t widgets.
        if (selected < 0)
            selected = 0; //that matches the default what-is-showing on the main select widget
        if (options.length > 1) {

            String selectedOption = options[selected];
            String y = selectedOption.length() >= 4 ? selectedOption.substring(0,  4) : "";
            String m = selectedOption.length() >= 7 ? selectedOption.substring(5,  7) : "";
            String d = selectedOption.length() >= 10? selectedOption.substring(8, 10) : ""; 
            String t = selectedOption.length() >= 12? selectedOption.substring(11)    : "";

            //need to make new ymdtOptions?
            if (lastSelectedOption == null || !selectedOption.equals(lastSelectedOption)) {

                long time = System.currentTimeMillis();
                lastSelectedOption = selectedOption;

                //find the current year, month, day and time
                int optionsLength = options.length;

                //make the y m d t sets
                HashSet<String> ySet = new HashSet();
                HashSet<String> mSet = new HashSet();
                HashSet<String> dSet = new HashSet();
                HashSet<String> tSet = new HashSet();

                //make the year set
                for (int i = 0; i < optionsLength; i++) {
                    String tOption = options[i];
                    String tY = tOption.length() >= 4 ? tOption.substring(0,  4) : "";
                    ySet.add(tY);

                    //for the current year, make the month set
                    if (tY.equals(y)) {
                        String tM = tOption.length() >= 7 ? tOption.substring(5,  7) : "";
                        mSet.add(tM);

                        //for the current month, make the day set
                        if (tM.equals(m)) {
                            String tD = tOption.length() >= 10? tOption.substring(8, 10) : ""; 
                            dSet.add(tD);

                            //for the current day, make the time set
                            if (tD.equals(d) && tOption.length() >= 12) {
                                tSet.add(tOption.substring(11));
                            }
                        }
                    }
                }

                //make y m d t sorted arrays
                yearOptions  = ySet.toArray(new String[0]); Arrays.sort(yearOptions);
                monthOptions = mSet.toArray(new String[0]); Arrays.sort(monthOptions);
                dayOptions   = dSet.toArray(new String[0]); Arrays.sort(dayOptions);
                timeOptions  = tSet.toArray(new String[0]); Arrays.sort(timeOptions);

                //time: ~1ms/1000 items on my computer. Scales ~linearly.
                //Although 1,000,000 taking 1 sec is annoying, the time to 
                //  transmit the html for the main select to the user will be 
                //  far longer.
                //String2.log("EmaGDateTimeSelect2 widget2 n=" + optionsLength + 
                //    " time=" + (System.currentTimeMillis() - time) + "ms");             
            }

            //make the year, month, day, and optional time widgets
            sb.append("        <td>&nbsp;Or,&nbsp;</td>");
            getDropDown(name + YEAR_NAME,  
                "It is best to select a year here, before you select a month (to the right).",  
                y, yearOptions,  onChangeSubmitsFormHTML, style, sb, true);
            getDropDown(name + MONTH_NAME, 
                "It is best to select a month here, before you select a day (to the right).", 
                m, monthOptions, onChangeSubmitsFormHTML, style, sb, true);
            getDropDown(name + DAY_NAME,   
                "It is best to select a day here, " + 
                (timeOptions.length > 0? 
                    "before you select a time (to the right)." : 
                    "after you select a month (to the left)."),   
                d, dayOptions,   onChangeSubmitsFormHTML, style, sb, true);
            if (timeOptions.length > 0) 
                getDropDown(name + TIME_NAME,  
                "It is best to select a time here, after you select a day (to the left).",  
                t, timeOptions,  onChangeSubmitsFormHTML, style, sb, true);           

        } 
        
        //clean up        
        if (options.length > 1) {
            sb.append(    
                //td good for spacing even if suffix is ""
                "          <td>" + (suffix.length() == 0? "&nbsp;" : suffix) + "</td>\n" + 
                "        </tr>\n" +
                "        </table>\n      ");
        } else sb.append("\n" +
            "      ");

        //return the results
        return sb.toString();
    }

    /**
     * This generates the HTML for a drop-down Select widget.
     *
     * @param tName the name of the widget (e.g., gridDate or gridDateMonth)
     * @param tTitle the title (balloon help) for this widget
     * @param tValue the currently selected value for this widget
     * @param tOptions the options for this widget
     * @param onChangeSubmitsFormHTML
     * @param style
     * @param sb a StringBuilder to which to add the HTML for a drop-down Select widget
     * @param addTd if true the HTML starts with &lt;td&gt; and ends with &lt;/td&gt;.
     * @return the index of the selected item (or -1 if not selected)
     */
    private static int getDropDown(String tName, String tTitle, String tValue,
            String tOptions[], String onChangeSubmitsFormHTML, String style, StringBuilder sb, 
            boolean addTd) {

        sb.append("\n        " + (addTd? "<td>" : "") +
                      "<select name=\"" + tName + "\" size=\"1\"" +
                  "\n          title=\"" + XML.encodeAsHTML(tTitle) + "\"" +
                  onChangeSubmitsFormHTML +
                  style + ">\n");
        int selected = -1;
        String spacer = tOptions.length < 20? "          " : ""; //save space if lots of options
        for (int i = 0; i < tOptions.length; i++) {
            String s = tOptions[i];
            boolean isSelected = tValue.equals(s);
            if (isSelected) 
                selected = i;
            sb.append(spacer + "<option" + 
                (isSelected? " selected=\"selected\">" : ">") + 
                s + "</option>\n");
        }
        sb.append("        </select>" + (addTd? "<td>" : "") + "\n");
        return selected;
    }

    /**
     * This indicates the index associated with aValue (or -1 if none)
     *
     * @param aValue the String form of a value to be tested
     * @return the index associated with aValue 
     *    (or -1 if aValue==null, or no options, or not matched).
     */
    public int indexOf(String aValue) {
        //if no options, -1
        if (options.length == 0)
            return -1;

        //no string?
        if (aValue == null)
            return -1;

        for (int i = 0; i < options.length; i++) {
            if (aValue.equals(options[i])) 
                return i;
        }
        return -1;        
    } 

    /**
     * This returns the index of the currently selected value (or -1 if none).
     * 
     * @param session the user's session
     * @return the index of the currently selected value (or -1 if none).
     */
    public int getSelectedIndex(HttpSession session) {
        return indexOf(getValue(session)); 
    }


    /**
     * This indicates if aString is one of the strings in options.
     *
     * @param aValue the String form of a value to be tested
     * @return an error string ("" if aValue is valid).
     */
    public String isValid(String aValue) {
        //if no options, consider anything valid     //is this ok?
        if (options.length == 0)
            return "";

        return indexOf(aValue) >= 0? "" : selectError;        
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
