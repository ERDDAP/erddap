/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

/**
 * This class displays a label (displaying the Value string) 
 * in the right side column of the standard form.
 * This class does not store a value; it is "transient".
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false") 
 *     indicates if this attribute should be displayed
 *     in wide format (spanning two columns in the HTML table),
 *     or standard format (label in the left column, component in the right).
 * <li> name+".label" (an HTML text string, default = "")
 *      is the initial value for the left (or upper) label on the HTML form
 * <li> name+".value" (an HTML text string, default = "")
 *      is the initial value for the right (or lower) label on the HTML form
 * </ul>
 *
 * This class is unusual: in double wide format, it doesn't put a blank
 * row after itself in the table.
 *
 */
public class EmaLabel extends EmaAttribute {

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaLabel(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        getStandardProperties();
        isTransient = true;
    }

    /**
     * This implements the abstract getControl of EmaAttribute.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        return value;
    }

    //Use the default isValid(String aString) which always returns 'true'.

}
