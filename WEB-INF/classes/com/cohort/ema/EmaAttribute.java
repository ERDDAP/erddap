/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Classes which extend EmaAttribute describe an attribute and
 * how it will be displayed to the user on an HTML page.
 * This class does not hold the value of the attribute; that is handled
 * by the session.
 *
 * The properties of this attribute must be specified in the resourceBundle 
 * for the EmaClass containing this attribute.
 * The key used for searching the resourceBundle for each property is 
 * constructed by concatenating the name (provided in the constructor),
 * plus ".", plus the name of the property. 
 * For example, the name of the label property an attribute named "address" 
 * would be "address.label".
 * If the properties aren't found, the default is used.
 * All EmaAttributes have these properties:
 * <ul>
 * <li> name+".label" specifies the label which will appear
 *     to the left of (if !doubleWide) or above (if doubleWide) the HTML component. 
 *     The default is "", but it is almost always specified in the resourceBundle.
 *     It is usually less than 20 characters.
 *     It is always html text.
 * <li> name+".value" specifies the initial value for the attribute. 
 *     The default varies with each subclass of EmaAttribute.
 *      This matches the HTML attribute name.
 * <li> name+".title" specifies the tooltip for the HTML component for the
 *     attribute. The defaults vary with each subclass of EmaAttribute.
 *     This matches the HTML attribute name.
 * <li> name+".style" specifies additional attributes (usually a style attribute)
 *     for the HTML component for the attribute. 
 *     The style information is used for tags where the text is not affected 
 *     by normal HTML tags.  For example, <tt>style="font-style:italic"</tt> .
 *     The default is "".
 * <li> name+".required" ("true" or "false", default = "true")
 *     specifies if this attribute must be specified to be considered valid.
 *     If false, the field may be left blank and will be considered valid.
 *     Not all EmaAttribute subclasses support this (for example, 
 *     EmaBoolean doesn't). 
 * <li> name+".doubleWide" ("true" or "false") 
 *     specifies if the label and component should
 *     each span both columns in the standard layout grid.
 *     If false, the label appears in the left column and the component in
 *     the right column.
 *     The default is specified by the enclosing EmaClass's getDefaultDoubleWide.
 * </ul>
 */
public abstract class EmaAttribute {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false;

    /** These values are specified in the constructor. */
    protected EmaClass parent;
    protected String name = ""; //never null

    /** These default values may be overridden via the properties file. */
    protected String label = ""; //never null
    protected String defaultValue = ""; //never null
    protected String title = ""; //never null
    protected String style = ""; //never null
    protected boolean required;
    protected boolean doubleWide;
    protected boolean enterSubmitsForm;

    /** This is set programmatically in the constructor. */
    protected boolean isTransient = false;

    
    /**
     * This is used by the constructor to get values from the properties file.
     */
    protected void getStandardProperties() {
        ResourceBundle2 classRB2 = parent.getClassResourceBundle2();

        //use the existing values (from above or from subclass) as defaults
        defaultValue     = classRB2.getString(name + ".value", defaultValue);
        label            = classRB2.getString(name + ".label", label);
        title            = classRB2.getString(name + ".title", title);
        style            = classRB2.getString(name + ".style", null);
        if (style == null)
            style = parent.getComponentStyle();
        else style = "\n              " + style + " "; //put on a separate line in the component's html tag

        //isTransient is set by the constructor
        required         = classRB2.getBoolean(name + ".required",
                               parent.getDefaultRequired());
        doubleWide       = classRB2.getBoolean(name + ".doubleWide", 
                               parent.getDefaultDoubleWide()); 
        enterSubmitsForm = classRB2.getBoolean(name + ".enterSubmitsForm", 
                               parent.getDefaultEnterSubmitsForm());
    }

    /**
     * This returns the name to be used for the component on the form
     * and as the base name for related keys in the resource bundle. 
     */
    public String getName() {
        return name;
    }

    /**
     * This returns the defaultValue.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * This sets the defaultValue.
     */
    public void setDefaultValue(String newDefault) {
        defaultValue = newDefault == null? "" : newDefault;
    }

    /**
     * This returns the label (for the HTML form) for this attribute.
     */
    public String getLabel() {
        return label;
    }    

    /**
     * This sets the label.
     */
    public void setLabel(String newLabel) {
        label = newLabel == null? "" : newLabel;
    }

    /**
     * This returns the title (the tooltip) for this attribute.
     */
    public String getTitle() {
        return title;
    }    

    /**
     * This sets the title.
     */
    public void setTitle(String newTitle) {
        title = newTitle == null? "" : newTitle;
    }

    /**
     * This indicates if the attribute has no value that needs to 
     * be stored. For example, EmaLabel doesn't store a value; so it is 
     * marked as transient.
     */
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * This indicates if this attribute must be specified
     * (and meet the isValid requirements) to be considered valid.
     *
     * @return isRequired
     */
    public boolean isRequired() {
        return required;
    }    

    /**
     * This indicates if the label and the component for this attribute 
     * on separate rows, with each spanning the two columns in the
     * table in the standard HTML form.
     */
    public boolean isDoubleWide() {
        return doubleWide;
    }    

    /**
     * This indicates if the HTML form should be submitted
     * when the user presses Enter in this component (usually a textfield).
     */
    public boolean getEnterSubmitsForm() {
        return enterSubmitsForm;
    }

    /**
     * This adds all of the default attribute values to the session.
     * EmaAttributes which use more than 1 control must overwrite this
     * to deal with separate sub-values.
     *
     * @param session is the session associated with a user
     * @param value the value to be saved in the session
     */
    public void setValue(HttpSession session, String value) {
        if (!isTransient)
            session.setAttribute(parent.getFullClassName() + "." + name,  
                value);
    }

    /**
     * This removes all of this attribute's values from the session.
     * EmaAttributes which use more than 1 control must overwrite this
     * to deal with separate sub-values.
     *
     * @param session is the session associated with a user
     */
    public void removeValue(HttpSession session) {
        if (!isTransient)
            session.removeAttribute(parent.getFullClassName() + "." + name);
    }

    /**
     * This gets the value of one of the EmaAttributes.
     * EmaAttributes which use more than one control must overwrite this
     * to construct the value String from all of the separate sub-values.
     * This is called for every EmaAttribute every time the user submits a form.
     *
     * @param session is the session associated with a user
     * @return the value (in String form) 
     * (or the default, if unexpectedly not in the session)
     */
    public String getValue(HttpSession session) {
        String value = getDefaultValue(); //EmaLabel is transient but uses 'label'
        if (!isTransient()) {
            value = (String)session.getAttribute(
                parent.getFullClassName() + "." + name);
            if (value == null) {
                value = getDefaultValue();
                //this should only happen if the component wasn't displayed on the web page
                //String2.log(  
                //    String2.ERROR + ": " + parent.getShortClassName() + 
                //    ".getValue(" + name + ") = null.");
            }
        }
        return value;
    }

    /**
     * This returns the HTML code for an HTML control 
     * which can be placed in an HTML
     * form and which allows the user to view and change the attribute.
     * 
     * This may return a simple standard control
     * or a table with several controls on it.
     *
     * @param value is the value of this attribute, as stored in the session
     * @return the HTML code for a control which allows the user to 
     *     view and change the attribute.
     */
    public abstract String getControl(String value);

    /**
     * Get a hidden control which indicates the current (which will be the previous) 
     * value for this EmaAttribute.
     * Use this instead of getControl if your temporarily don't want to make
     * the control visible, but you want the value to be part of the form.
     *
     * <p> This is particularly useful for EmaBoolean, 
     * because checkboxes don't return values in the response if they aren't
     * checked. So this lets processRequest use the previous value
     * or use the default if the previous isn't present.
     * For EmaBoolean, the value should be "true" or "false".

     * @param value 
     * @return the html for a hidden control which indicates the current
     *    (what will be the previous) value.
     */
    public String getHiddenControl(String value) {
        return "      <input type=\"hidden\" name=\"" + name + "\" value=\"" + 
            value + "\" >\n";
    }

    /**
     * This returns newline + a line of HTML code which calls
     * "onChange=\"submitForm(this.form);\"" if enterSubmitsForm is true; else "".
     *
     * @return the HTML code to call this.form.submit();
     */
    public String getOnChangeSubmitsFormHTML() {
        return enterSubmitsForm? 
            "\n          onChange=\"submitForm(this.form);\"" : 
            "";
    }

    /**
     * This returns newline + a line of HTML code which calls
     * "onClick=\"submitForm(this.form);\"" if enterSubmitsForm is true; else "".
     *
     * @return the HTML code to call submitForm(this.form);
     */
    public String getOnClickSubmitsFormHTML() {
        return enterSubmitsForm? 
            "\n          onClick=\"submitForm(this.form);\"" : 
            "";
    }

    /**
     * This returns the HTML code for the entry in the table
     * for the label and the control for this attribute.
     * If doubleWide, this creates two rows (colspan=2) in the table.
     * If not doubleWide, this creates two columns in the table.
     * 
     * @param value is the value of this attribute, as stored in the session
     * @param displayErrorMessages if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the HTML code for insertion in an HTML table.
     */
    public String getTableEntry(String value, boolean displayErrorMessages) {

        String error = "";
        if (displayErrorMessages) {
            error = isValid(value);
            if (error.length() > 0) 
                error = 
                    "    " + parent.getBeginRow() + "\n" +
                    "      <td colspan=\"2\">" + parent.getBeginError() + 
                              error + parent.getEndError() + "</td>\n" +
                    "    " + parent.getEndRow() + "\n";
        }
        if (doubleWide) {
            return
                error +
                (label.length() > 0?
                    "    " + parent.getBeginRow() + "\n" +
                    "      <td colspan=\"2\">" + label + "</td>\n" +
                    "    " + parent.getEndRow() + "\n" : 
                    "") +
                "    " + parent.getBeginRow() + "\n" +
                "      <td colspan=\"2\">" + getControl(value) + "</td>\n" +
                "    " + parent.getEndRow() + "\n" +
                (parent.getSpacerAfterDoubleWide()? 
                    "    " + parent.getBeginRow() + "\n" +
                    "      <td>" + parent.getPlaceHolder() + "</td>\n" +
                    "    " + parent.getEndRow() + "\n" :
                    "");
        } else {
            return
                error +
                "    " + parent.getBeginRow() + "\n" + 
                "      <td>" + label + "</td>\n" +
                "      <td>" + getControl(value) + "</td>\n" +
                "    " + parent.getEndRow() + "\n";
        }
    }

    /**
     * This almost always overridden method
     * tests if aValue is a valid value for this object.
     * For example, for EmaInt, this indicates if aValue is 
     * an integer between min and max. 
     * If !isRequired and aValue is "", it is considered valid.
     *
     * @param aValue the String form of a value to be tested
     * @return an error string ("" if aValue is valid).
     *     The default implementation always returns "".
     */
    public String isValid(String aValue) {
        return "";
    }
    
    /**
     * Whether this class is a JSP or a servlet, this handles
     * a "request" from a user, storing incoming attributes
     * as session values.
     * EmaAttributes which have more than 1 control have to overwrite
     * this to handle the sub-values.
     *
     * @param request 
     * @return true if all the values for this attribute are valid
     */
    public boolean processRequest(HttpServletRequest request) {   
        if (isTransient) 
            return true;

        //get the attribute's value from the request 
        String value = (String)request.getParameter(name); //yes, short name here

        //if the attribute exists, save it in the session 
        HttpSession session = request.getSession();
        if (value == null) 
            value = getValue(session); //else get stored value
            if (reallyVerbose) String2.log("EmaAttributes.processRequest from session: " + name + "=" + value);
        else {
            if (reallyVerbose) String2.log("EmaAttributes.processRequest from request: " + name + "=" + value);
            setValue(session, value);
        }
        String isValid = isValid(value);
        //if (isValid.length() > 0)
        //    String2.log("EmaAttribute.processRequest " + name +
        //        ".isValid(" + value + ") = " + isValid);
        return isValid.length() == 0;
    }

}
