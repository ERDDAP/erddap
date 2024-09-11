/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.XML;

/**
 * This class holds the properties for displaying a multiline String 
 * which can be edited in a textarea.
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
 * <li> name+".value" is the initial value for the plain text String 
 *      stored by this object (which may contain "\n" to indicate line breaks)
 * <li> name+".cols" (an int &gt; 0, default = 20)
 *      is the number of columns visible in the textarea
 * <li> name+".rows" (an int &gt; 0, default = 3)
 *      is the number of rows visible in the textarea
 * <li> name+".minlength" (an int &gt;= 0, default = 0)
 *      is the minimum number of characters to be considered valid
 * <li> name+".maxlength" (an int &gt; 0, default = 100000)
 *      is the maximum number of characters allowed in the textfield
 *      This matches the HTML attribute name.
 * </ul>
 *
 */
public class EmaStringBox extends EmaAttribute {
//FUTURE: needs minlength and maxlength settings

    protected int rows, cols, minlength, maxlength;
    protected String lengthError;

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaStringBox(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        cols = classRB2.getInt(name + ".cols", 20);
        rows = classRB2.getInt(name + ".rows", 3);
        minlength = classRB2.getInt(name + ".minlength", 0);
        maxlength = classRB2.getInt(name + ".maxlength", 100000);
        lengthError = String2.substitute(parent.getLengthError(), 
            parent.removeColon(name), "" + minlength, "" + maxlength); 
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
        sb.append("<textarea name=\"" + name + 
            "\" rows=\"" + rows + 
            "\" cols=\"" + cols + 
            //'wrap' is non-standard. see http://www.idocs.com/tags/forms/_TEXTAREA_WRAP.html
            "\" maxlength=\"" + (rows * cols) + "\" wrap=\"SOFT\"");  //was "virtual"
        if (title.length() > 0)
            sb.append("\n        title=\"" + XML.encodeAsHTML(title) + "\"");
        //style not used here 
        //  because component font is already special (monospaced)
        sb.append(">\n" +
            XML.encodeAsHTML(value) + "</textarea>");
        return sb.toString();
    }

    /**
     * This returns "" if there is at least 1 character (or if !required).
     *
     * @param aValue the String form of a value to be tested
     * @return an error string ("" if aValue is valid).
     */
    public String isValid(String aValue) {
        //no string?
        if (aValue == null)
            aValue = "";
        if (!required && aValue.length() == 0)
            return "";

        //length
        if (aValue.length() < minlength || aValue.length() > maxlength)
            return lengthError;

        return "";
    } 

}
