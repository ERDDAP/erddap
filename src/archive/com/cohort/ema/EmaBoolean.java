/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This class holds the properties for displaying a boolean.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false") 
 *     indicates if the component should span both columns of the form.
 *     The default is specified by the resourceBundle property "defaultDoubleWide".
 * <li> name+".required" ("true" or "false", default = true) 
 *     indicates if a value must be provided for this attribute
 *     to be considered valid. If false, a value of "" is considered valid.
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, default = "") 
 *      is the toolTip for this attribute 
 * <li> name+".value" ("true" or "false", default = false)
 *      is the initial value stored by this object
 * <li> name+".enterSubmitsForm" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user changes the boolean value.
 *     If this is true, the JavaScript script in EmaClass.IncludeJavaScript 
 *     must be in the "head" section of the HTML page
 *     that contains the HTML form.
 * <li> name+".rightLabel" (an HTML text string, default = "")
 *      is the label that appears to the right of the checkbox on the HTML form
 * </ul>
 *
 */
public class EmaBoolean extends EmaAttribute {

    public final static String TRUE = "true";
    public final static String FALSE = "false";
    private String rightLabel = "";

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaBoolean(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        rightLabel = parent.classRB2.getString(name + ".rightLabel", "");
    }

    /**
     * Set a new rightLabel.
     * @param rightLabel
     */
    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel == null? "" : rightLabel;
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
        sb.append("<input type=\"checkbox\" name=\"" + name + 
            "\" value=\"true\""); //the value is used when the checkbox is checked
        if (title.length() > 0)
            sb.append("\n        title=\"" + XML.encodeAsHTML(title) + "\"");
        if (value.toLowerCase().equals(TRUE))
            sb.append("\n        checked=\"checked\"");
        //onClick works everywhere; onChange works in Opera but not IE 
        sb.append(getOnClickSubmitsFormHTML() + 
            " > " + rightLabel);

        //add hidden so processRequest can distinguish notChecked from 
        //first time user (default)
        sb.append("\n      <input type=\"hidden\" name=\"previous_" + name + "\" value=\"" + 
            value + "\" >");
        return sb.toString();
    }

    //Use the default isValid(String aValue) which always returns "".

    /**
     * Whether this class is a JSP or a servlet, this handles
     * a "request" from a user, storing incoming attributes
     * as session values.
     * This overwrites EmaAttribute's version because unchecked checkboxes
     * are not returned in the request.
     *
     * @param request 
     * @return true if all the values for this attribute are valid
     */
    public boolean processRequest(HttpServletRequest request) {   
        //get the attribute's value from the request 
        String value = (String)request.getParameter(name); //yes, short name here
        //String2.log("EmaBoolean.processRequest " + name + " = " + value);

        //value may be null because checkbox isn't checked. Is there a previous value?
        if (value == null) {
            String tValue = (String)request.getParameter("previous_" + name); //yes, short name here
            //if previous is present, user un-checked the checkbox
            if (tValue != null) 
                value = FALSE;
            //String2.log("EMABOOLEAN " + name + " was present but not checked.");
        }

        //still nothing? it's a first time user, use the default
        if (value == null) {
            value = defaultValue; //special for checkbox: assume default if not in request
            //String2.log("EMABOOLEAN " + name + " was not present; using default: " + defaultValue);
        }


        //if the attribute exists, save it in the session 
        HttpSession session = request.getSession();
        setValue(session, value); //and always reset value in session
        //String2.log("EmaBoolean.processRequest " + name +
        //    ".isValid(" + value + ") = " + isValid(value));
        return isValid(value).length() == 0;
    }
}
