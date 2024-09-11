/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.XML;

/**
 * This class does not store a value, but puts
 * subsequent EmaAttributes (until EmaGroupEnd) into a group.
 * This class does not store a value; it is "transient".
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, default = "") 
 *      is the toolTip which appears in empty space inside the group
 * </ul>
 *
 */
public class EmaGroupBegin extends EmaAttribute {

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaGroupBegin(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        isTransient = true;
    }

    /**
     * This implements the abstract getControl of EmaAttribute.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code to begin a group (in HTML terms: fieldset)
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        return 
            "  " + parent.getEndTable() + "\n" +
            "  <fieldset" + 
                (title.length() == 0? 
                    "" : 
                    " title=\"" + XML.encodeAsHTML(title) + "\"") + 
                ">\n" +
            (label.length() == 0? 
                "" : 
                "  <legend>" + label + "</legend>\n") +
            "  " + parent.getBeginTable() + "\n";
    }

    /**
     * This returns the HTML code for the entry in the table
     * for this attribute.
     * This overwrites EmaAttribute.getTableEntry.
     * 
     * @param value is the value of this attribute, as stored in the session
     * @param displayErrorMessage if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the HTML code for insertion in an HTML table.
     */
    public String getTableEntry(String value, boolean displayErrorMessage) {
        return getControl(value);
    }


    //Use the default isValid(String aValue) which always returns "".

}
