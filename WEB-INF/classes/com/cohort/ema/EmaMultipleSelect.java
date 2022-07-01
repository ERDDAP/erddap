/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import java.util.ArrayList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying (see .display) 
 * a list of options and allowing 0 or more items to be selected.
 * The selection may be displayed as a series of checkboxes or 
 * a list of checkboxes where several rows always visible.
 *
 * <p>If display &gt;= 3, this uses some CSS features. This works fine
 * on all browsers I have tested (Windows and Mac) except MS IE on Mac
 * (problem with scroll bars, maybe just because display was 3; 
 * a larger number may work find).
 *
 * <p>If there are 0 options, no HTML control will be displayed.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false") 
 *     indicates if this attribute should be displayed
 *     in wide format (spanning two columns in the HTML table),
 *     or standard format (label in the left column, component in the right).
 * <li> name+".required" ("true" or "false", default = true) 
 *     indicates if at least one item must be selected for this attribute
 *     to be considered valid. If false, a value of "" is considered valid.
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, default = "") 
 *      is the toolTip for this attribute.
 *      Unusual option for radio button Select: 
 *      If you enter a string with a series of 
 *      titles, each separated by '\f', they will be applied to the
 *      to each of the options in turn.
 *      (#0=main title, #1 for first option, #2 for second option, ...)
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".value" (a String, default = "") 
 *     is the initial \f-separated list of values stored by this object.
 *     For display "rows" or "columns", if it isn't one of the options, 
 *     the first option is used.
 * <li> name+".options" (\f-separated list of strings, default = "")
 *     is the array of options
 * <li> name+".display" 
 *     specifies how the options will be displayed:
 *     "column" (display the options as checkboxes in a column),
 *     "row" (display the options as radio buttons in a row),
 *     or an integer &gt;= 3 indicating the number of rows always visible.
 *     The default is 4.
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
public class EmaMultipleSelect extends EmaAttribute {

    public final static int DISPLAY_ROW = -1;
    public final static int DISPLAY_COLUMN = 0;

    public final static String TRUE = "true";
    public final static String FALSE = "false";

    /** If 'display' &gt;=3, this needs to be in the HTML document's HEAD section for EmaMultiSelect's to work.*/
    public static String HEAD_CSS =
//this is needed for EmaMultipleSelect
"  <style type=\"text/css\">\n" +
"    <!--\n" +
//overflow: auto causes y scrollbar to appear
//overflow-x: hidden; and width: auto  discourage x scrollbar from appearing
".EmaMultiUL {border: 1px solid #000; width: auto; overflow: auto; overflow-x: hidden;\n" + //was width: auto;
"      list-style-type: none; margin: 0; padding: 0; } \n" +
".EmaMultiLI {margin: 0; padding: 0; } \n" +  //was no width attribute
//all the label color stuff removed because it causes Safari to blank the text
".EmaMultiLABEL {margin: 0; padding: 0; } \n" + //was width: 100%; display: block; color: WindowText; background-color: Window; 
//"    label#emaMulti:hover {background-color: Highlight; color: HighlightText; } \n" + //was no width attribute
"    -->\n" +
"  </style> \n";

    protected Object options[];
    protected int display;
    protected String selectError;
    protected String optionTitles[];

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaMultipleSelect(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        selectError = String2.substitute(parent.getRequiredSelectError(), 
            parent.removeColon(name), null, null); 
        setTitle(title);

        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        options = classRB2.getStringArray(name + ".options", new String[]{""});

        String tDisplay = classRB2.getString(name + ".display", "1");
        if (tDisplay.equals("row"))
            display = DISPLAY_ROW;
        else if (tDisplay.equals("column"))
            display = DISPLAY_COLUMN;
        else {
            try {
                display = Math.max(3, Integer.parseInt(tDisplay));
            } catch (Exception e) {
                display = 4;
            }
        }

        //complexity: HTML radio buttons force an item to be selected. [not true]
        //   Select and List do not.  
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
    public void setOptions(Object options[]) {
        this.options = options == null? 
            new String[]{} :
            options;
    }

    /**
     * This returns the requested option (or null if invalid).
     * 
     * @param which (0..)
     * @return option #which (or null if invalid)
     */
    public String getOption(int which) {
        return which < 0 || which >= options.length? null : options[which].toString(); 
    }

    /**
     * This returns an array with the options.
     * 
     * @return an array of Objects (usually Strings)
     */
    public Object[] getOptions() {
        return options; 
    }

    /**
     * This resets the titles (which all users will see).
     * If you have an arrayList or Vector instead of a String[], use arrayList.toArray.
     * 
     * WARNING: If you use this while the web application is running,
     * you should either use just one thread for the web application
     * (so this one change takes care of everything) or call this
     * for every request by every user.
     * 
     * @param titles the new array of titles
     *     (if null, treated as 0 elements)
     */
    public void setTitles(String titles[]) {
        if (titles == null || titles.length == 0) {
            setTitle("");
            return;
        }
        title = titles[0];
        optionTitles = titles;
    }

    /**
     * This resets the title or optionTitles.
     * 
     * WARNING: If you use this while the web application is running,
     * you should either use just one thread for the web application
     * (so this one change takes care of everything) or call this
     * for every request by every user.
     * 
     * @param tTitle is the new title or \f-separated optionTitles
     *    (interpreted as: mainTitle, title for element 0, title for element 1, ...).
     */
    public void setTitle(String tTitle) {
        if (tTitle == null)
            tTitle = "";
        title = tTitle;
        optionTitles = title.split("\f");
    }


    /**
     * This gets the html for the title for the specified option.
     *
     * @param optionIndex #-1 for main title, #0 for option 0, #1 for option 1, ...
     * @return the html for the title for the specified option 
     *     (or "" if !hasOptionTitles or if no optionTitle for optionIndex)
     */
    protected String getOptionTitle(int optionIndex) {
        return optionIndex < -1 || optionIndex >= optionTitles.length-1?
            "" :
            "\n          title=\"" + XML.encodeAsHTML(optionTitles[optionIndex + 1]) + "\"";
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
        String values[] = getValues(value);

        sb.append("\n");
        String br = display == DISPLAY_ROW? "" : 
                    display == DISPLAY_COLUMN? "<br>" : 
                    "";
        if (display > 0) 
            //basic approach from http://krijnhoetmer.nl/stuff/html/select-multiple-checkbox-list/
            sb.append("<ul class=\"EmaMultiUL\" style=\"height: " + (display * 19) + "px\" >\n");

        StringBuilder hidden = new StringBuilder();
        for (int i = 0; i < options.length; i++) {
            String tOption = options[i].toString();
            String tTitle = getOptionTitle(optionTitles.length > 1? i : -1);
            boolean isSelected = String2.indexOf(values, tOption) >= 0;
            if (display == DISPLAY_COLUMN) {
                if (i > 0) sb.append("<br>"); 
            } else if (display > 0) 
                //<li> because this is set up as a list
                //<label>  associates checkbox and the text to the right (for highlight or not, and catch click) 
                //'id' to utilize css attributes for this type of label
                //'for' to name the checkbox this is associated with
                sb.append("<li class=\"EmaMultiLI\" >");
            sb.append("<label class=\"EmaMultiLABEL\" for=\"" + name + i + "\" " + tTitle + ">" +
                "<input type=\"checkbox\" name=\"" + name + i + "\" " + 
                "id=\"" + name + i + "\" " +
                getOnClickSubmitsFormHTML() + " ");
            if (isSelected)
                sb.append(" checked=\"checked\"");
            sb.append(">" + tOption + "</label>"); //">" is end of input tag 
            if (display > 0) sb.append("</li>");
            sb.append("\n");

            //add hidden so processRequest can distinguish notChecked from 
            //first time user (default)
            hidden.append("<input type=\"hidden\" name=\"previous_" + name + i + "\" value=\"" + 
                (isSelected? TRUE : FALSE) + "\" >\n");
        }
        if (display > 0) 
            sb.append("</ul>\n");
        sb.append(hidden.toString());
        sb.append("      ");


        //return the results
        return sb.toString();
    }

    /**
     * Whether this class is a JSP or a servlet, this handles
     * a "request" from a user, storing incoming attributes
     * as session values.
     * <p>This overwrites EmaAttribute's version because unchecked checkboxes
     * are not returned in the request.
     * <p>Also, this combines all the separate name+i check box widget values
     * into the \f separated 'value' for the overall widget 'name'.
     *
     * @param request 
     * @return true if all the values for this attribute are valid
     */
    public boolean processRequest(HttpServletRequest request) {   
        int defaults[] = getValidIndexes(defaultValue);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.length; i++) {
            //get the attribute's value from the request 
            String value = (String)request.getParameter(name + i); //yes, short name here
            //String2.log("EmaMultipleSelect.processRequest " + name + " = " + value);

            //value may be null because checkbox isn't checked. Is there a previous value?
            if (value == null) {
                String tValue = (String)request.getParameter("previous_" + name + i); //yes, short name here
                //if previous is present, user un-checked the checkbox
                if (tValue != null) 
                    value = FALSE;
                //String2.log("EmaMultipleSelect " + name + i + " was present but not checked.");
            }

            //still nothing? it's a first time user, use the default
            if (value == null) {
                //special for checkbox: assume default if not in request
                value = String2.indexOf(defaults, i) >= 0? TRUE : FALSE; 
                //String2.log("EmaMultipleSelect " + name + i + " was not present; using default: " + defaultValue);
            }


            //if the attribute is TRUE, save it in sb
            if (value.equals(TRUE)) {
                if (sb.length() > 0) sb.append('\f');
                sb.append(options[i]);
            }

        }
        HttpSession session = request.getSession();
        setValue(session, sb.toString()); //and always set value in session
        //String2.log("EmaMultipleSelect.processRequest " + name + " value= " + sb.toString());
        return true; //always valid after this procedure
    }

    /**
     * This gets the value from a session and converts it into a String[].
     * This could be static but isn't for convenience.
     *
     * @param session
     * @return a String[]. 
     *   value=null and value="" return String[0].
     *   The individual values may or may not be valid.
     */
    public String[] getValues(HttpSession session) {
        return getValues(getValue(session));
    }

    /**
     * This converts a \f separated value string into a String[].
     * This could be static but isn't for convenience.
     *
     * @param value a \f separated value string
     * @return a String[]. 
     *   value=null and value="" return String[0]
     *   The individual values may or may not be valid.
     */
    public String[] getValues(String value) {
        return value == null || value.length() == 0? new String[0] : 
            String2.split(value, '\f');
    }

    /**
     * This indicates the index associated with aValue (or -1 if none)
     *
     * @param aValue the String form of a single value to be tested
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
            if (aValue.equals(options[i].toString())) 
                return i;
        }
        return -1;        
    } 

    /**
     * This returns the indexes of the valid values in a \f separated value String.
     * This could be static, but isn't for convenience.
     * 
     * @param value a \f separated value String
     * @return the indexes of the currently selected valid values (or int[0] if none).
     */
    public int[] getValidIndexes(String value) {
        String values[] = getValues(value);
        int indexes[] = new int[values.length];
        int nGood = 0;
        for (int i = 0; i < values.length; i++) {
            int index = indexOf(values[i]);
            if (index >= 0)
                indexes[nGood++] = index;
        }
        int allGood[] = new int[nGood];
        System.arraycopy(indexes, 0, allGood, 0, nGood);
        return allGood;
    }

    /**
     * This returns a \f separated String with just valid values.
     * 
     * @param value a \f separated value String
     * @return a \f separated String with just valid values ("" if no valid values)
     */
    public String getValidValue(String value) {
        int indexes[] = getValidIndexes(value);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexes.length; i++) {
            if (i > 0) 
                sb.append('\f');
            sb.append(options[indexes[i]]);
        }
        return sb.toString();
    }


    /**
     * This indicates if all the values in 'value' are among the strings in options.
     *
     * @param value the String form of a value to be tested
     * @return an error string ("" if value is all valid).
     */
    public String isValid(String value) {
        //if no options, consider anything valid     //is this ok?
        if (options.length == 0)
            return "";
        String values[] = getValues(value);
        for (int i = 0; i < values.length; i++)
            if (indexOf(values[i]) < 0)
                return selectError;      
        return "";
    } 

    /**
     * This is a convenience method if the options change frequently. This:
     * <ul>
     * <li>Sets the options to newOptions.
     * <li>Gets the valid value from session.
     * <li>Returns the valid value.
     * </ul>
     *
     * @param newOptions
     * @param session
     * @return the valid \f separated value String
     */
    public String setOptionsAndValidate(String newOptions[], HttpSession session) {
        setOptions(newOptions);
        String value = getValidValue(getValue(session));
        setValue(session, value);
        return value;
    }

}
