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
 * This class holds the properties for displaying a password.
 * Users see just *'s as they type the string.
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
 * <li> name+".label" (an HTML text string, default = "Password:")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, 
 *      default = "Enter a password between {0} and {1} characters long.",
 *      where {0} and {1} are replaced with minlength and maxlength)
 *      is the toolTip for this attribute 
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".value" (a String, default = "")
 *      is the initial value for the String stored by this object
 * <li> name+".size" (an int &gt; 0, default = 20 which matches the HTML default)
 *      is the width of the textField (in em-spaces) (20 is common)
 * <li> name+".minlength" (an int &gt;= 0, default = 6)
 *      is the minimum number of characters to be considered valid
 *      (a number less than 0 is converted to the default)
 * <li> name+".maxlength" (an int &gt; 0, default = 10)
 *      is the maximum number of characters allowed in the textfield
 *      (a number less than or equal to 0 is converted to the default)
 * <li> name+".enterSubmitsForm" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user presses Enter in the HTML textfield.
 * </ul>
 *
 */
public class EmaPassword extends EmaAttribute {
//FUTURE: optional regex to ensure a suitable password?

    protected int size;
    protected int minlength;
    protected int maxlength;
    protected String lengthError;

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaPassword(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        label = "Password:";
        title = parent.getLengthError(); //before getStandardProps
        getStandardProperties();

        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        size = classRB2.getInt(name + ".size", 20);
        minlength = classRB2.getInt(name + ".minlength", 6);
        maxlength = classRB2.getInt(name + ".maxlength", 10);

        //last thing...
        title = String2.substitute(title, 
            parent.removeColon(name), "" + minlength, "" + maxlength); 
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
        //this fixes: 'enter' acted like next 'submit' button
        String submit = enterSubmitsForm? "if (enter(event)) submitForm(this.form); " : "";

        sb.append("<input type=\"password\" autocomplete=\"off\" name=\"" + name + "\" value=\"" + 
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
     * This tests if aValue meets the requirements
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

        //passed tests -> valid
        return "";
    } 

}
