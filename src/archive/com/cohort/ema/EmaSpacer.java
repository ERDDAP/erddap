/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

/**
 * This class doesn't store a value but leaves a 1-row-high gap
 * in the form.
 * This class does not store a value; it is "transient".
 *
 * <p>The supported properties in the className+".properties" file are: [none].
 */
public class EmaSpacer extends EmaAttribute {

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaSpacer(EmaClass parent, String name) {
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
        return parent.getPlaceHolder();
    }

    //Use the default isValid(String aValue) which always returns "".

}
