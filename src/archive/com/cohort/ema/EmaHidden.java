/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

/**
 * This class holds the properties for generating a HIDDEN HTML control.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".value" is the initial value for the hidden 
 *     String stored by this object
 * </ul>
 *
 */
public class EmaHidden extends EmaAttribute {

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaHidden(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
    }

    /**
     * This implements the abstract getControl of EmaAttribute.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        return "<input type=\"hidden\" name=\"" + name + 
                   "\" value=\"" + value + "\" >\n";
    }

    /**
     * This returns the HTML code for the entry in the table
     * for this attribute.
     * This overwrites EmaAttribute.getTableEntry.
     *
     * <p>!!! This shouldn't be called because this doesn't result in a
     * row for a table and so shouldn't be in a table. Instead,
     * this should be added to the addedHtml for 
     * emaClass.getEndOfHTMLForm(long startMillis, String addedHtml)
     * so it will be inserted after the table is finished but before
     * the form is finished.
     * ???So should this method throw EXCEPTION???
     * 
     * @param value is the value of this attribute, as stored in the session
     * @param displayErrorMessage if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the HTML code for insertion in an HTML table.
     */
    public String getTableEntry(String value, boolean displayErrorMessage) {
        return "    " + getControl(value);
    }

    //Use the default isValid(String aString) which always returns "".

}
