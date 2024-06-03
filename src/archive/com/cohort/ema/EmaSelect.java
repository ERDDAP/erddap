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
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying (see .display) 
 * a list of options.
 * The selection may be displayed as radio buttons, a drop-down list,
 * or a list with several rows always visible.
 *
 * <p>It is not a problem if the order of the options changes 
 * from one version of the program to the next, or if new options are added
 * (since the current value is stored as a String).
 * But if the previously selected option is removed in a new version
 * of the program, the current selection will switch to the first option.
 *
 * <p>If there are 0 options, no HTML control will be displayed.
 * 
 * <p> !!!None of the options should have 2 adjacent spaces.
 * If that item is selected and the form is submitted,
 * the value submitted just has 1 space, so the item isn't picked
 * and the default value is used. See String2.combineSpaces.
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
 *     If "display" is &lt; 1, "required" is forced to be true,
 *     since HTML radio buttons always force an item to be picked.
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
 *     is the initial value stored by this object.
 *     For display "rows" or "columns", if it isn't one of the options, 
 *     the first option is used.
 * <li> name+".options" (a \f-separated list of strings, default = "")
 *     is the array of options
 * <li> name+".display" 
 *     specifies how the options will be displayed:
 *     "tabs" (display the options as a row of tabs -- almost always used with
 *       enterSubmitsForm=true),
 *     "column" (display the options as radio buttons in a column),
 *     "row" (display the options as radio buttons in a row),
 *     or an integer &gt; 0 indicating the number of rows always visible.
 *     The default is 1, which generates a dropdown list.
 *     Display=tabs is based on ideas from
 *     http://www.johanneslerch.at/space/html+tabs+with+css+and+javascript .
 * <li> name+".enterSubmitsForm" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user changes the selection.
 *     If this is true, the JavaScript script in EmaClass.IncludeJavaScript 
 *     must be in the "head" section of the HTML page
 *     that contains the HTML form.
 * <li> name+".buttonsVisible" ("true" or "false", default = "false"),
 *     specifies if 4 buttons should be placed to the right of the Select 
 *     control, marked "|&lt;", "-", "+", "&gt;|" which allow the user
 *     to increase or decrease the selection, or go to the first or last value.
 *     If "enterSubmitsForm", pressing the buttons also submits the form.
 *     The buttons are only displayed if "display" = 1;
 *     These buttons require JavaScript to work.
 * </ul>
 * <br>FUTURE/ISSUE recommend translating the options?
 *  How does underlying app deal with translated selection?
 *
 */
public class EmaSelect extends EmaAttribute {

    public final static int DISPLAY_TABS = -2;
    public final static int DISPLAY_ROW = -1;
    public final static int DISPLAY_COLUMN = 0;

    /** If 'display=tabs', this needs to be in the HTML 
      document's HEAD section for EmaSelect's to work. 
      And EmaTabRight.gif, EmaTabLeft.gif, EmaTabRightSelected.gif, and
      EmaTabLeftSelected.gif need to be in the images subdirectory of the
      .html document. */
    public static String HEAD_CSS =
"<style type=\"text/css\">\n" +
"<!-- \n" +
"#EmaTabRow {float: left; width: 100%;}\n" +
".EmaTab {\n" +
"   display: block;\n" +
"   float: left;\n" +
"   cursor: pointer;\n" +
"   padding: 0px;\n" +
"   margin: 2px;\n" +
"   background:url(\"images/EmaTabRight.gif\") no-repeat right top;\n" +
"}\n" +
".EmaTabInner { \n" +
"   display: block;\n" +
"   text-decoration: none;\n" +
"   color: black;\n" +
"   background:url(\"images/EmaTabLeft.gif\") no-repeat left top;\n" +
"   padding: 2px 15px 2px 15px;\n" +
"}\n" +
".EmaTabSelected {\n" +
"   display: block;\n" +
"   float: left;\n" +
"   margin: 2px 2px 2px 0px;\n" +
"   padding: 0px;\n" +
"   background:url(\"images/EmaTabRightSelected.gif\") no-repeat right top;\n" +
"}\n" +
".EmaTabSelectedInner {\n" +
"   display:block;\n" +
"   background:url(\"images/EmaTabLeftSelected.gif\") no-repeat left top;\n" +
"   padding: 2px 15px 3px 15px;\n" +
"}\n" +
"--> \n" +
"</style>\n";

    protected Object options[];
    protected int display;
    protected String selectError;
    protected String optionTitles[];
    protected boolean buttonsVisible;

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaSelect(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        selectError = String2.substitute(parent.getRequiredSelectError(), 
            parent.removeColon(name), null, null); 
        setTitle(title);

        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        options = classRB2.getStringArray(name + ".options", new String[]{""});

        buttonsVisible = 
            classRB2.getBoolean(name + ".buttonsVisible", false);
        String tDisplay = classRB2.getString(name + ".display", "1");
        if (tDisplay.equals("tabs"))
            display = DISPLAY_TABS;
        else if (tDisplay.equals("row"))
            display = DISPLAY_ROW;
        else if (tDisplay.equals("column"))
            display = DISPLAY_COLUMN;
        else {
            try {
                display = Math.max(1, Integer.parseInt(tDisplay));
            } catch (Exception e) {
                display = 1;
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
        String t = optionIndex < -1 || optionIndex >= optionTitles.length-1?
            "":
            optionTitles[optionIndex + 1];
        return "\n          title=\"" + XML.encodeAsHTML(t) + "\"";
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

        //tabs
        if (display == DISPLAY_TABS) {
            sb.append("<div id=\"emaTabRow\">\n");
            String thisForm = "document." + parent.getFormName();
            for (int i = 0; i < options.length; i++) {
                String s = options[i].toString();
                String tTitle = getOptionTitle(optionTitles.length > 1? i : -1);
                if (value.equals(s)) 
                    sb.append("        <span class=\"EmaTabSelected\" " + tTitle + ">" +
                        "<span class=\"EmaTabSelectedInner\">" + s + "</span></span>\n");
                else 
                    sb.append("        <span class=\"EmaTab\" " + tTitle + ">" +
                        "<span class=\"EmaTabInner\" " +
                        (enterSubmitsForm? 
                            //change the value for the hidden element below to this option
                            "\n          onMouseUp=\"" +
                                thisForm + "." + name + ".value='" + XML.encodeAsHTML(s) + "';" +
                                "submitForm(" + thisForm + ");\"" : 
                            "") +
                        ">" + s + "</span></span>\n");
            }
            sb.append("      </div>\n" +
                "      <input type=\"hidden\" name=\"" + name + "\" value=\"" + 
                    XML.encodeAsHTML(value) + "\" >\n" +
                "      ");

        //radioButtons
        } else if (display == DISPLAY_COLUMN || display == DISPLAY_ROW) { 
            sb.append("\n");
            String br = display == DISPLAY_COLUMN? "<br>" : "";
            for (int i = 0; i < options.length; i++) {
                String s = options[i].toString();
                sb.append(
                    //<span> avoids check box and value being separated by newline when lots of options
                    "        " + (i == 0? "" : br) + 
                        "<span style=\"white-space:nowrap;\"><input type=\"radio\" name=\"" + name + 
                        "\" value=\"" + XML.encodeAsHTML(s) + "\"" + 
                    getOnClickSubmitsFormHTML() + //onChange doesn't work
                    (getOptionTitle(optionTitles.length > 1? i : -1)) + "\n          " +                    
                    (value.equals(s)? "checked>" : ">") + s + "</span>\n");
            }
            sb.append("      ");
        }

        //a Select control
        else {
            if (buttonsVisible && options.length > 1) 
                sb.append(    //td for <select>
                      "\n        <table class=\"erd\" style=\"width:2%;\"><tr><td>"); //padding=0
            sb.append("\n        <select name=\"" + name + 
                "\" size=\"" + display + "\"" + 
                getOptionTitle(-1) + 
                getOnChangeSubmitsFormHTML() +
                style +
                ">\n");
            int selected = -1;
            String spacer = options.length < 20? "          " : ""; //save space if lots of options
            for (int i = 0; i < options.length; i++) {
                String s = options[i].toString();
                boolean isSelected = value.equals(s);
                if (isSelected) 
                    selected = i;
                sb.append(spacer + "<option" + 
                    (isSelected? " selected=\"selected\">" : ">") + 
                    s + "</option>\n");
            }
            sb.append("        </select>\n");
            if (buttonsVisible && options.length > 1) {
                String submit = enterSubmitsForm? " submitForm(this.form);" : "";
                sb.append(    
                    "        </td>\n" + //end of <select>'s td
                    //first
                    //"type=button" buttons don't submit form; they just run their JavaScripts
                    "        <td><input value=\"|&lt;\" type=button " + 
                        (selected == -1 || selected > 0? "" : "disabled ") +
                        "title=\"Select the first item.\"" +
                        style + "\n" +
                    "          onMouseUp=\"this.form." + name + 
                        ".selectedIndex=0;" + submit + "\"></td>\n" +
                    //-1000
                    (options.length > 1000? 
                        "        <td><input value=\"-1000\" type=button " + 
                            (selected > 0? "" : "disabled ") +
                            "title=\"Jump back 1000 items.\"" +
                            style + "\n" +
                        "          onMouseUp=\"this.form." + name + 
                            ".selectedIndex-=" + Math.min(selected , 1000) + 
                            ";" + submit + "\"></td>\n" :
                        "") +
                    //-100
                    (options.length > 150? 
                        "        <td><input value=\"-100\" type=button " + 
                            (selected > 0? "" : "disabled ") +
                            "title=\"Jump back 100 items.\"" +
                            style + "\n" +
                        "          onMouseUp=\"this.form." + name + 
                            ".selectedIndex-=" + Math.min(selected , 100) + 
                            ";" + submit + "\"></td>\n" :
                        "") +
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
                    //+100
                    (options.length > 150?
                        "        <td><input value=\"+100\" type=button " + 
                            (selected < options.length - 1? "" : "disabled ") +
                            "title=\"Jump ahead 100 items.\"" +
                            style + "\n" +
                        "          onMouseUp=\"this.form." + name + 
                            ".selectedIndex+=" + Math.min(options.length - 1 - selected, 100) + 
                            ";" + submit + "\"></td>\n" :
                        "") +
                    //+1000
                    (options.length > 1000?
                        "        <td><input value=\"+1000\" type=button " + 
                            (selected < options.length - 1? "" : "disabled ") +
                            "title=\"Jump ahead 1000 items.\"" +
                            style + "\n" +
                        "          onMouseUp=\"this.form." + name + 
                            ".selectedIndex+=" + Math.min(options.length - 1 - selected, 1000) + 
                            ";" + submit + "\"></td>\n" :
                        "") +
                    //last
                    "        <td><input value=\"&gt;|\" type=button " + 
                        (selected == -1 || selected < options.length - 1? "" : "disabled ") +
                        "title=\"Select the last item.\"" + 
                        style + "\n" +
                    "          onMouseUp=\"this.form." + name + 
                        ".selectedIndex=" + (options.length - 1) + 
                        ";" + submit + "\"></td>\n      " +
                    "        </tr>\n" +
                    "        </table>\n      ");
            } else sb.append("\n" +
                "      ");

        }

        //return the results
        return sb.toString();
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
            if (aValue.equals(options[i].toString())) 
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
     * This is a convenience method if the options change frequently. This:
     * <ul>
     * <li>Sets the options to newOptions.
     * <li>Gets the value from session.
     * <li>If the value isn't valid, this selects the defaultIndex, and sets the value in the session.
     * <li>Returns the valid value.
     * </ul>
     *
     * @param newOptions
     * @param session
     * @param defaultIndex
     * @return the valid value
     */
    public String setOptionsAndValidate(String newOptions[], HttpSession session, int defaultIndex) {
        setOptions(newOptions);
        String value = getValue(session);
        int index = String2.indexOf(options, value);
        if (index < 0) {
            value = options[defaultIndex].toString();
            setValue(session, value);
        }
        return value;
    }

}
