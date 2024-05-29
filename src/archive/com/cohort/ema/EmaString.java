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
 * This class holds the properties for displaying a one-line String value 
 * (often in a specific format, specified by regex).
 * This is also the base class (superclass) for EmaSSN and others.
 * Forms that use this class need EmaClass.IncludeJavaScript in the "head" 
 * section of the HTML page.
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
 *      The title is also used as the error message if the value is invalid.
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".value" (a String, default = "")
 *      is the initial value for the String stored by this object
 * <li> name+".size" (an int &gt; 0, default = 20 which matches the HTML default)
 *      is the width of the textField (in em-spaces) (20 is common).
 *      This matches the HTML attribute name.
 * <li> name+".minlength" (an int &gt;= 0, default = 0)
 *      is the minimum number of characters to be considered valid
 * <li> name+".maxlength" (an int &gt; 0, default = 256)
 *      is the maximum number of characters allowed in the textfield
 *      This matches the HTML attribute name.
 * <li> name+".regex" (a String, default = "")
 *     is the regular expression which must be matched
 *     for the value to be considered valid. "" = no regex.
 *     See java.util.regex.Pattern and java.util.regex.Matcher.
 * <li> name+".enterSubmitsForm" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user presses Enter in the HTML textfield.
 * </ul>
 *
 * 
 */
public class EmaString extends EmaAttribute {

    protected int size;
    protected int minlength;
    protected int maxlength;
    protected String regex; //"" if not used
    protected String lengthError;

    /**
     * A constructor.
     * 
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaString(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        
        //get size, maxlength, and regex now, 
        //    so subclass constructors can overwrite them
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        size = classRB2.getInt(name + ".size", 20);
        minlength = classRB2.getInt(name + ".minlength", 0);
        maxlength = classRB2.getInt(name + ".maxlength", 256);
        regex = classRB2.getString(name + ".regex", "");
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

        //this fixes: 'enter' acted like next 'submit' button
        String submit = enterSubmitsForm? "if (enter(event)) submitForm(this.form); " : "";

        StringBuilder sb = new StringBuilder();
        sb.append("<input type=\"text\" name=\"" + name + "\" value=\"" + 
            XML.encodeAsHTML(value) + "\"\n");
        sb.append("        onkeypress=\"" + submit + "return !enter(event);\"\n"); //supress Enter->submit
        if (title.length() > 0 || enterSubmitsForm)
            sb.append("        title=\"" + XML.encodeAsHTML(title) + 
                (enterSubmitsForm? "  " + parent.getPressEnterToSubmit() : "") + "\"\n");
        sb.append("        size=\"" + size + "\" maxlength=\"" + maxlength + "\" " +
            style + ">");
        return sb.toString();
    }

    /**
     * This tests if aValue conforms with regex (if it has been specified)
     * (or is not required and is "").
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

        //matches regex
        if (regex.length() > 0 && !aValue.matches(regex))
            return title;

        //passed tests -> valid
        return "";
    } 

}
