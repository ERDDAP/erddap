/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying an int.
 * This class never uses a thousands separator.
 * Forms that use this class need EmaClass.IncludeJavaScript in the "head" 
 * section of the HTML page.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false", 
 *     the default is the enclosing EmaClass's defaultDoubleWide) 
 *     indicates if this attribute should be displayed
 *     in wide format (spanning two columns in the HTML table),
 *     or standard format (label in the left column, component in the right).
 * <li> name+".required" ("true" or "false", default = true) 
 *     indicates if a value must be provided for this attribute
 *     to be considered valid. If false, a value of "" is considered valid.
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, 
 *      default = "Please enter an integer between {0} and {1}.",
 *      where {0} and {1} are replaced by the min and max) 
 *      is the toolTip for this attribute 
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".value" (an int, default = 0) 
 *      is the initial value stored by this object 
 * <li> name+".size" (an int &gt; 0, default = 10)
 *      is the width of the textField (in em-spaces).
 *      This matches the HTML attribute name.
 * <li> name+".min" (an int, default = Integer.MIN_VALUE) 
 *      is the minimum allowed value
 * <li> name+".max" (an int, default = Integer.MAX_VALUE) 
 *      is the maximum allowed value
 * <li> name+".increment" (an int, default = 0)
 *      is the amount to decrease or increase the value when the - or +
 *      buttons are clicked. 0 and below have special meanings:
 *      0 = de/increase the most significant digit).
 * <li> name+".enterSubmitsForm" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user presses Enter in the HTML textfield.
 * <li> name+".buttonsVisible" ("true" or "false", default = "true"),
 *     specifies if - and + buttons should be placed to the right of the control
 *     which allow the user to increase or decrease the value by "increment".
 *     If "enterSubmitsForm", pressing the buttons also submits the form.
 *     These buttons require JavaScript to work.
 * </ul>
 *
 */
public class EmaInt extends EmaAttribute {

    protected int size;
    protected int min;
    protected int max;
    protected int increment;
    protected String intError;
    protected boolean buttonsVisible;


    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaInt(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        title = parent.getIntError(); //before getStandardProperties
        getStandardProperties();
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        size = classRB2.getInt(name + ".size", 10);
        min = classRB2.getInt(name + ".min", Integer.MIN_VALUE);
        max = classRB2.getInt(name + ".max", Integer.MAX_VALUE);
        increment = classRB2.getInt(name + ".increment", 0);
        title = String2.substitute(title, //after min, max set
            parent.removeColon(name), "" + min, "" + max); 
        intError = String2.substitute(parent.getIntError(), 
            parent.removeColon(name), "" + min, "" + max); 
        buttonsVisible = classRB2.getBoolean(name + ".buttonsVisible", true);
    }

    /**
     * This gets the int value of this EmaInt from a user's session.
     *
     * @param session is the session associated with a user
     * @return the value (in int form) 
     *    (or Integer.MAX_VALUE, if String value can't be parsed to int)
     *    (or the default, if unexpectedly not in the session)
     */
    public int getInt(HttpSession session) {
        return String2.parseInt(getValue(session));
    }

    /**
     * This gets 'max'.
     *
     * @return 'max'
     */
    public int getMax() {
        return max;
    }

    /**
     * This gets 'min'.
     *
     * @return 'min'
     */
    public int getMin() {
        return min;
    }

    /**
     * This gets 'increment'.
     *
     * @return the increment
     */
    public int getIncrement() {
        return increment;
    }

    /**
     * This implements the abstract createControl of EmaAttribute.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        StringBuilder sb = new StringBuilder();

        //this fixes: 'enter' acted like next 'submit' button
        String submit = enterSubmitsForm? "if (enter(event)) submitForm(this.form); " : "";

        //use hard-coded <table>, not the properties version, which may change
        sb.append("\n");
        if (buttonsVisible) {
            sb.append("        <table class=\"erd\" style=\"width:2%;\">\n"); //padding=0. 'width' solves gap betwen buttons in opera 9
            sb.append("          <tr>\n");
            sb.append("            <td>\n");
        }
        
        //the textfield   
        sb.append("            <input type=\"text\" name=\"" + name + 
            "\" value=\"" + XML.encodeAsHTML(value) + "\"\n");
        sb.append("              onkeypress=\"" + submit + "return !enter(event);\"\n"); //supress Enter->submit
        if (title.length() > 0 || enterSubmitsForm)
            sb.append("              title=\"" + XML.encodeAsHTML(title) + 
                (enterSubmitsForm? "  " + parent.getPressEnterToSubmit() : "") + "\"\n");
        sb.append("              size=\"" + size + "\" maxlength=\"24\" " +
            style + ">\n");

        if (buttonsVisible) {
            sb.append("            </td>\n");

            //- button
            sb.append("            <td><input type=\"button\" value=\"-\"\n"); 
            sb.append("              title=\"" + parent.getClickMinus(increment) + "\"\n");
            sb.append("              onMouseUp=\"" + name + ".value = " + 
                (increment <= 0 ?
                    "smallerInt(" + name + ".value, " + min + ", '" + defaultValue + "');" :
                    "incrementMinus(" + name + ".value, " + increment + ", " + 
                        min + ", '" + defaultValue + "');") + 
                (enterSubmitsForm? " submitForm(this.form);" : "") + "\"" +
                style + 
                ">\n");
            sb.append("            </td>\n");

            //+ button
            sb.append("            <td><input type=\"button\" value=\"+\"\n"); 
            sb.append("              title=\"" + parent.getClickPlus(increment) + "\"\n");
            sb.append("              onMouseUp=\"" + name + ".value = " + 
                (increment <= 0 ?
                    "biggerInt(" + name + ".value, " + max + ", '" + defaultValue + "');" :
                    "incrementPlus(" + name + ".value, " + increment + ", " + 
                        max + ", '" + defaultValue + "');") + 
                (enterSubmitsForm? " submitForm(this.form);" : "") + "\"" +
                style + 
                ">\n");
            sb.append("            </td>\n");

            sb.append("          </tr>\n");
            //use hard-coded </table>, not the properties version, which may change
            sb.append("        </table>\n");
        }

        sb.append("      ");
        return sb.toString();
    }

    /**
     * This tests if aValue represents a valid int between min and max
     * (or is not required and is "").
     *
     * @param aValue the String form of a value to be tested
     * @return an error string ("" if aValue is between min and max)
     */
    public String isValid(String aValue) {
        //no string?
        if (aValue == null || aValue.length() == 0)
            return required? parent.createRequiredTextError(name) : "";

        //parse it and check min and max
        try {
            int i = Integer.parseInt(aValue);
            if (i >= min && i <= max)
                return "";
        } catch (Exception e) {
        }
        return intError;
    } 

}
