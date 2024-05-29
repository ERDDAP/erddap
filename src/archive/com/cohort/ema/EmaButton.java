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
 * This class displays a button (displaying the label string) 
 * which, when pressed, does something.
 * The 'value' for this class is the title for the button; it doesn't change; 
 * it is transient.
 *
 * <p>The supported properties in the className+".properties" file are:
 * <ul>
 * <li> name+".doubleWide" ("true" or "false") 
 *     indicates if this attribute should be displayed
 *     in wide format (the button appears on the left),
 *     or standard format (the button appears on the right).
 *     The default is specified by the enclosing EmaClass's getDefaultDoubleWide.
 * <li> name+".label" (an HTML text string, default = "")
 *      is the label on the HTML form (usually "" for buttons)
 * <li> name+".value" (default = "")
 *      is the HTML label for the button.
 * <li> name+".title" (a plain text string, default = "")
 *      is the tooltip for the button
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".type" ("submit", "reset", "image", "button", the default is "submit")
 *      is the type of HTML button.
 *      Currently, image and button not supported.
 * </ul>
 *
 * This class is unusual: in double wide format, it doesn't put a blank
 * row after itself in the table.
 *
 */
public class EmaButton extends EmaAttribute {

    protected String type = "submit";

    /**
     * A constructor.
     *
     * @param parent is the emaClass which holds this emaAttribute
     * @param name is the name for this attribute within the HTML form.
     *     It also serves as the basis for the attributes properties
     *     in the resource bundle (e.g., name+".label").
     */
    public EmaButton(EmaClass parent, String name) {
        this.parent = parent;
        this.name = name;
        defaultValue = "Submit"; //overwrite the EmaAttribute default of ""
        getStandardProperties();
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();
        type = classRB2.getString(name + ".type", type); 
        isTransient = true;
    }

    /**
     * This implements the abstract getControl of EmaAttribute.
     * This is implemented with the input type="submit|reset|button".
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control
     * @see EmaAttribute#getControl
     */
    public String getControl(String value) {
        //Future this needs more work to support other options (e.g., type=image)

        //create a row with the button in the right column
        //If this button is pressed, the form includes <name>=XML.encodeAsHTML(<value>).
        return
            //"value" is displayed and name=value is returned if clicked
            "<input type=\"" + type + "\" name=\"" + name + "\"" +                
                (type.equals("submit")? " onclick=\"pleaseWait();\"" : "") +
                " value=\"" + XML.encodeAsHTML(value) + "\"" + 
                style +
                (title.length() == 0? 
                    "" : 
                    "\n          title=\"" + XML.encodeAsHTML(title) + "\"") + 
            ">" ;

            //I started with the newer "button" control, but had problems
            //  detecting which pressed.
/*            "<button type=\"" + type + "\" name=\"" + name + "\"" +                
                " value=\"" + name + "\"" + //yes, name: if clicked, name=name will be returned
                (title.length() == 0? 
                    "" : 
                    "\n          title=\"" + XML.encodeAsHTML(title) + "\"") + 
            ">" + value + "</button>";
*/
        }

    //Use the default isValid(String aValue) which always returns "".


}
