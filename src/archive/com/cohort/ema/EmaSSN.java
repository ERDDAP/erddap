/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

/**
 * This class holds the properties for displaying a United States-style 
 * Social Security Number in the format "###-##-####".
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
 * <li> name+".label" (an HTML text string, 
 *      default = "Social Security Number:")
 *      is the label on the HTML form
 * <li> name+".title" (a plain text String, 
 *      default = "The SSN must have the format \"###-##-####\".") 
 *      is the toolTip for this attribute.
 *      The title is also used as the error message if the value is invalid.
 * <li> name+".value" is the value for the SSN (which must be in the
 *    form "###-##-####") stored by this object
 * <li> name+".submitOnEnter" ("true" or "false", default = "false"),
 *     specifies if the HTML form should be submitted if the
 *     user presses Enter in the HTML textfield.
 *     If this is false, the JavaScript script in EmaClass.IncludeJavaScript 
 *     must be in the "head" section of the HTML page
 *     that contains the HTML form.
 * <li> This class automatically sets the size, maxlength, and
 *      regex properties inherited from EmaString.
 * </ul>
 *
 */
public class EmaSSN extends EmaString {

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaSSN(EmaClass parent, String name) {
        super(parent, name);

        label = "Social Security Number:"; //before getStandardProp to overwrite default
        title = "Please enter a social security number with the format \"###-##-####\"."; 
        getStandardProperties();  //redo with new defaults
       
        size = 12; 
        maxlength = 24; //allow for editing
        regex = "[0-9]{3}-[0-9]{2}-[0-9]{4}";

    }
  
 
}