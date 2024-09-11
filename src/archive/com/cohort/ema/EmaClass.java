/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.ema;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.GregorianCalendar;
import java.util.Vector;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.ServletException;

/**
 * You can make subclasses of this class to store groups of attributes.
 * For each subclass you must also make a properties file 
 * (called MyPackage/MyClassName.properies, where MyPackage/MyClassName 
 * parallels the fullClasName specified in the constructor for this class) 
 * with values for the following keys
 * <ul>
 * <li> "url" - the URL is called when the form is submitted.
 *      It must be the URL for this page, so the form can be processed.
 *      It is usually a relative url, e.g., "contextPath/PageName.jsp".
 *      You must specify this.
 * <li> "windowTitle" is the plain text for the title of the HTML page
 *      (which appears on the window's title bar) (default = "")
 * <li> "formTitle" is the HTML text for the title of the HTML form (default = "")
 * <li> "formName" is the name of the HTML form (default = "emaForm").
 *      It must be length&gt;1 with no spaces (like a variable name).
 * <li> (optional) There are several default strings inherited from 
 *      com/cohort/ema/Ema.properties that you can overwrite by specifying
 *      a new value in the properties file:
 *    <ul>
 *    <li> "placeHolder" overwrites placeHolder. If the background color
 *       of the form isn't white, change this to refer to the correct color.
 *    <li> "beginError" overwrites beginError.
 *    <li> "endError" overwrites endError.
 *    <li> "beginTable" overwrites beginTable.
 *        Specify this to change the appearance of the table.
 *    <li> "endTable" overwrites endTable.
 *        Specify this to change the appearance of the table.
 *    <li> "clickPlus" overwrites clickPlus.
 *    <li> "clickMinus" overwrites clickMinus.
 *    <li> "requiredTextError" specifies an error message to be displayed
 *        if no text has been entered in a required text field.
 *        If the string contains "{0}", the attribute's name will be substituted.
 *    <li> "requiredSelectError" specifies an error message to be displayed
 *        if no item has been selected for a required EmaSelect attribute.
 *        If the string contains "{0}", the attribute's name will be substituted.
 *    <li> "intError" specifies an error message to be displayed
 *        if the value is not a valid int between min and max.
 *        If the string contains "{0}", the attribute's name will be substituted.
 *        If the string contains "{1}", the min allowed value will be substituted.
 *        If the string contains "{2}", the max allowed value will be substituted.
 *    <li> "doubleError" specifies an error message to be displayed
 *        if the value is not a valid floating-point number between min and max.
 *        If the string contains "{0}", the attribute's name will be substituted.
 *        If the string contains "{1}", the min allowed value will be substituted.
 *        If the string contains "{2}", the max allowed value will be substituted.
 *    <li> "dateError" specifies an error message to be displayed
 *        if the value is not a valid date/time between min and max.
 *        If the string contains "{0}", the attribute's name will be substituted.
 *        If the string contains "{1}", the min allowed date will be substituted.
 *        If the string contains "{2}", the max allowed date will be substituted.
 *    <li> "lengthError" specifies an error message to be displayed
 *        if the value doesn't have between minlength and maxlength characters.
 *        If the string contains "{0}", the attribute's name will be substituted.
 *        If the string contains "{1}", the minlength value will be substituted.
 *        If the string contains "{2}", the maxlength value will be substituted.
 *    <li> "defaultRequired" specifies if all attributes should be 
 *        required by default. If not specified, this defaults to "true".
 *    <li> "defaultDoubleWide" specifies if all attributes should be 
 *        doubleWide by default. If not specified, this defaults to "false".
 *    <li> "defaultEnterSubmitsForm" specifies the enterSubmitsForm for all
 *        attributes. If not specified, this defaults to "false".
 *    <li> "spacerAfterDoubleWide" indicates if an empty row should be
 *        put in the table after each doubleWide attribute. 
 *        If not specified, this defaults to "true".
 *    </ul>
 * <li> A large number of keys/values associated with the EmaAttributes
 *    owned by this class.
 * </ul>
 */
public class EmaClass extends HttpServlet {
//future options: 
//* option get/post
//* replace url with dynamically generated url

    protected String fullClassName;

    /** Useful for statistics */
    protected long instantiationTime;
    protected int nSessions = 0;
    protected long totalFormCreationTime = 0;
    protected long maxFormCreationTime = 0;
    protected int totalNFormsCreated = 0;
    public final static String N_REQUESTS_THIS_SESSION = "nRequestsThisSession";
    //public boolean verbose = false;

    //the thread-safe containers for holding all the attributes
    /** attributeList holds the EmaAttributes in the order added */
    protected Vector attributeList = new Vector(); //thread-safe 
    /** attributeMap makes it easy to access the EmaAttributes by name */
    protected ConcurrentHashMap attributeMap = new ConcurrentHashMap(16, 0.75f, 4); //thread-safe 

    /** This value must be specified in the class's properties file. */
    protected String url;

    /** These values should be specified in the class's properties file. */
    protected String windowTitle = "";
    protected String formTitle = "";
    protected String formName = "emaForm"; //the default

    /** 
     * These default values may be overridden in the class's properties file 
     * (which would overwrite the defaults in Ema.properties). 
     */
    protected String clickPlusMSD; 
    protected String clickMinusMSD; 
    protected String clickPlusX;
    protected String clickMinusX;
    protected String placeHolder;
    protected String beginError;
    protected String endError;
    protected String beginTable;
    protected String endTable;
    protected String additionalJavaScript;
    protected String requiredTextError;
    protected String requiredSelectError;
    protected String intError;
    protected String doubleError;
    protected String dateError;
    protected String lengthError;
    protected String componentStyle;

    /** You can overwrite the defaults for these values in the class's properties file. */
    protected boolean defaultRequired;
    protected boolean defaultDoubleWide;
    protected boolean defaultEnterSubmitsForm;
    protected String  pressEnterToSubmit;
    protected boolean spacerAfterDoubleWide;

    protected String beginRow = "<tr style=\"text-align:left\">";
    protected String endRow = "</tr>";

    /** This is the JavaScript the needs to be in the Head section of the HTML page 
     * for just the 'enter' function. */
    public static String includeJavaScriptForEnter =
        "<script>\n" +
        "<!--\n" + //hide from browsers without javascript
        //was the keypress event's keycode 'Enter'?
        //see http://www.mredkj.com/tutorials/validate.html
        "function enter(e) {\n" +  //pass it the event
        "    var key = window.event? e.keyCode : e.which? e.which : 0;\n" + //IE, Netscape, ? 
        "    return key == 13;\n" +
        "}\n" +
        "//-->\n" + //end of hide from browser's without javascript
        "</script>\n";

    /** This standard JavaScript needs to be in the Head section of the HTML page. */
    public static String includeJavaScript =
        //EnterSubmitsForm
        "<script>\n" +
        "<!--\n" + //hide from browsers without javascript
        //was the keypress event's keycode 'Enter'?
        //see http://www.mredkj.com/tutorials/validate.html
        "function enter(e) {\n" +  //pass it the event
        "    var key = window.event? e.keyCode : e.which? e.which : 0;\n" + //IE, Netscape, ? 
        "    return key == 13;\n" +
        "}\n" +
        //This returns the exponent of abs(d) (e.g., 543.2 -> 100, -534.2 -> 100)
        //For d=0 this returns 1.
        "function exponent(d) {\n" +
        "    if (!isFinite(d) || d == 0) return 1;\n" +
        "    return Math.pow(10, Math.floor( Math.log(Math.abs(d)) / Math.LN10));\n" +
        "}\n" +
        //This increases d to the next multiple of increment
        //goofy spelling of "default" avoids trouble (it is a keyword)
        "function incrementPlus(d, increment, max, defaultt) {\n" +
        "    if (!isFinite(d)) return defaultt;\n" +
        "    return Math.min((Math.round(d / increment) + 1) * increment, max);\n" +
        "}\n" +
        //This decreases d to the next multiple of increment
        //goofy spelling of "default" avoids trouble (it is a keyword)
        "function incrementMinus(d, increment, min, defaultt) {\n" +
        "    if (!isFinite(d)) return defaultt;\n" +
        "    return Math.max((Math.round(d / increment) - 1) * increment, min);\n" +
        "}\n" +
        //This increases the most significant digit of d (but no bigger than max)
        //goofy spelling of "default" avoids trouble (it is a keyword)
        "function bigger(d, max, defaultt) {\n" +
        "    if (!isFinite(d)) return defaultt;\n" +
        "    if (d < 0) return -smaller(-d, -max);\n" +
        "    var exp = exponent(d);\n" +
        "    return Math.min((Math.floor(d / exp) + 1) * exp , max);\n" +
        "}\n" +
        //This decreases the most significant digit of d (but no smaller than min)
        "function smaller(d, min, defaultt) {\n" +
        "    if (!isFinite(d)) return defaultt;\n" +
        "    if (d < 0) return -bigger(-d, -min);\n" +
        "    if (d == 0) return -1;\n" +
        "    var exp = exponent(d);\n" +
        "    var man = Math.floor(d / exp) - 1; //ceil causes round of problems with 0.6\n" +
        "    return Math.max(man == 0? (9 * exp / 10) : (man * exp), min);\n" +
        "}\n" +
        //This increases the most significant digit of d (but no bigger than max)
        "function biggerInt(d, max, defaultt) {\n" +
        "    if (!isFinite(d)) return defaultt;\n" +
        "    if (exponent(d) <= 1) d = Math.floor(d) + 1;\n" +
        "    else d = bigger(d, max);\n" +
        "    return Math.min(d, max);\n" +
        "}\n" +
        //This decreases the most significant digit of d (but no smaller than min)
        "function smallerInt(d, min, defaultt) {\n" +
        "    if (!isFinite(d)) return defaultt;\n" +
        "    if (exponent(d) <= 1) d = Math.floor(d) - 1;\n" +
        "    else d = smaller(d, min);\n" +
        "    return Math.max(d, min);\n" +
        "}\n" +
        /*
        //DigitsOnly
        "<script>\n" +
        "function digitsOnly(textfield) {\n" +
        "  var s1 = textfield.value;\n" +
        "  var s2 = \"\";\n" +
        "  var chr;\n" +
        "  for (i = 0; i < s1.length; i++) {\n" +
        "    chr = s1.substring(i,i+1);\n" +
        "    if (chr < \"0\" || chr > \"9\") {\n" +
        "      s2 += chr;\n" +
        "    }\n" +
        "  }\n" +
        "  textfield.value = s;\n" +
        "}\n" +
        */
        //If an HTMLElement with id="EmaWaitMessage" exists, this sets it to 'visible'.
        //If not, no problem.
        //see JavaScript book pg 345 (section 18.4.3)
        "function pleaseWait() {\n" +
        "  var e = null;\n" +
        "  if (document.getElementById) e = document.getElementById(\"EmaWaitMessage\");\n" + //none support it yet?
        "  else if (document.all) e = document.all[\"EmaWaitMessage\"];\n" + //most
        "  else if (document.layers) e = document.layers[\"EmaWaitMessage\"];\n" + //safari, Mac Netscape 7.1
        "  if (e) e.style.visibility = \"visible\";\n" +
        "  return;\n" +
        "}\n" +
        //This calls pleaseWait() and then submits this form.   
        "function submitForm(form) {\n" +
        "  pleaseWait();\n" +
        "  form.submit();\n" +
        "  return;\n" +
        "}\n" +
        "//-->\n" + //end of hide from browser's without javascript
        "</script>\n";

    /** This will display a message to the user if JavaScript is not supported
     * or disabled. Last verified 2007-10-11. */
    public static String javaScriptDisabled =
        "  <noscript><span style=\"color:red\"><strong>This web page works much better when\n" +
        "    JavaScript is enabled.</strong> Please:</span>\n" +
        "    <ol>\n" +
        "    <li>Enable JavaScript in your browser:\n" +
        "      <ul>\n" + 
        "      <li> Windows\n" +  
        "        <ul>\n" + 
        "        <li> Firefox: select \"Tools : Options : Content : Enable JavaScript : OK\"\n" +
        "        <li> Internet Explorer: select \n" +  //true for IE 6 and 7 
        "          \"Tools : Internet Options : Security : Internet : Custom level :\n" +
        "          Sripting/ Active Scripting : Enable : OK : OK\"\n" +
        "        <li> Opera: select \"Tools : Quick Preferences : Enable JavaScript\"\n" +
        "        </ul>\n" +
        "      <li> Mac OS X\n" +
        "        <ul>\n" +
        "        <li> Firefox: select \"Firefox : Preferences : Content : Enable JavaScript : OK\"\n" +
        "        <li> Internet Explorer (this is an out-of-date browser -- please consider switching): \n" +
        "          <br>select \"Explorer : Preferences : Web Content : Enable Scripting : OK\"\n" +
        "        <li> Opera: select \"Opera : Quick Preferences : Enable JavaScript\"\n" +
        "        <li> Safari: select \"Safari : Preferences : Security : Enable JavaScript\"\n" + 
        "        </ul>\n" +
        "      </ul>\n" +
        "    <li>Reload this web page.\n" +
        "    </ol>\n" +
        "  </noscript>\n";

    public String emaClassConstructorDiagnostic;
    protected ResourceBundle2 emaRB2;
    protected ResourceBundle2 classRB2;

    /**
     * The standard constructor.
     *
     * @param tFullClassName is the package+name of this class,
     *    which is used to find the name+".properties" file
     *    and the attribute's properties within it.
     */
    public EmaClass(String tFullClassName) {
        this(tFullClassName, 
            new ResourceBundle2("com.cohort.ema.Ema"),
            new ResourceBundle2(tFullClassName));
    }

    /**
     * The less commonly used constructor.
     *
     * @param tFullClassName is the package+name of this class,
     *    which is used to find the name+".properties" file
     *    and the attribute's properties within it.
     * @param emaRB2 is the resourceBundle2 for ema
     * @param classRB2 is the resourceBundle2 for the class
     */
    public EmaClass(String tFullClassName, ResourceBundle2 emaRB2, ResourceBundle2 classRB2) {
        //set the resource bundles
        this.emaRB2   = emaRB2;
        this.classRB2 = classRB2;
        Test.ensureNotNull(emaRB2,   "emaRB2");
        Test.ensureNotNull(classRB2, "classRB2");

        fullClassName = tFullClassName;
        emaClassConstructorDiagnostic = "Instantiating an EmaClass: " + 
            fullClassName + "\n" + Math2.memoryString() + "\n" +
            String2.standardHelpAboutMessage() + "\n";
        instantiationTime = System.currentTimeMillis();

        //read values from the class resource bundle
        url                    = classRB2.getString("url", url);
        windowTitle            = classRB2.getString("windowTitle", windowTitle);
        formTitle              = classRB2.getString("formTitle", formTitle);
        formName               = classRB2.getString("formName", formName);

        //These default values may be overridden in the Ema.properties file
        //  or in the class's properties file
        placeHolder            = getFromBothRB2("placeHolder", "");
        beginError             = getFromBothRB2("beginError", "");
        endError               = getFromBothRB2("endError", "");
        beginTable             = getFromBothRB2("beginTable", "");
        endTable               = getFromBothRB2("endTable", "");
        additionalJavaScript   = getFromBothRB2("additionalJavaScript", "");
        clickPlusMSD           = getFromBothRB2("clickPlusMSD", "");
        clickMinusMSD          = getFromBothRB2("clickMinusMSD", "");
        clickPlusX             = getFromBothRB2("clickPlusX", "");
        clickMinusX            = getFromBothRB2("clickMinusX", "");
        pressEnterToSubmit     = getFromBothRB2("pressEnterToSubmit", "");
        requiredTextError      = getFromBothRB2("requiredTextError", "");
        requiredSelectError    = getFromBothRB2("requiredSelectError", "");
        intError               = getFromBothRB2("intError", "");
        doubleError            = getFromBothRB2("doubleError", "");
        dateError              = getFromBothRB2("dateError", "");
        lengthError            = getFromBothRB2("lengthError", "");
        componentStyle         = getFromBothRB2("componentStyle", "");
        if (componentStyle.length() > 0)
            componentStyle = "\n              " + componentStyle + " "; //put on a separate line in the components

        //These default values may be overridden in the class's properties file
        defaultRequired        = classRB2.getBoolean("defaultRequired", true);
        defaultDoubleWide      = classRB2.getBoolean("defaultDoubleWide", false);
        defaultEnterSubmitsForm= classRB2.getBoolean("defaultEnterSubmitsForm", false);
        spacerAfterDoubleWide  = classRB2.getBoolean("spacerAfterDoubleWide", true);

        emaClassConstructorDiagnostic += "Finished instantiating EmaClass: " + 
            fullClassName + "\n" + Math2.memoryString() + "\n";
    }

    private String getFromBothRB2(String propName, String theDefault) {
        return classRB2.getString(propName, emaRB2.getString(propName, theDefault));
    }

    /** This returns the fullClassName. */
    public String getFullClassName() {
        return fullClassName;
    }

    /** This returns the resourceBundle2 for this class. */
    public ResourceBundle2 getClassResourceBundle2() {
        return classRB2;
    }

    /** This returns the servlet or JSP's URL. */
    public String getUrl() {
        return url;
    }

    /** This returns the title for the window. */
    public String getWindowTitle() {
        return windowTitle;
    }

    /** This returns the title for the form. */
    public String getFormTitle() {
        return formTitle;
    }

    /** This returns the name for the form -- useful for JavaScript references
     * to the form ("document." + getFormName()) or an input element of
     * the form ("document." + getFormName() + "." + elementName). 
     * It will be a string of length&gt;1 with no spaces. */
    public String getFormName() {
        return formName;
    }

    /** 
     * This returns the place holder (which takes up space in an HTML table
     * but displays nothing. 
     */
    public String getPlaceHolder() {
        return placeHolder;
    }

    /** This returns the HTML style codes for the beginning of an error message. */
    public String getBeginError() {
        return beginError;
    }

    /** This returns the HTML style codes for the end of an error message. */
    public String getEndError() {
        return endError;
    }

    /** This returns the HTML code to begin a table. */
    public String getBeginTable() {
        return beginTable;
    }

    /** This returns the HTML code to end a table. */
    public String getEndTable() {
        return endTable;
    }

    /** 
     * This returns the standard HEAD_CSS, JavaScript, and additionalJavaScrip,
     * all of which need to be in the HTML document's HEAD section in 
     * order for the form to work correctly.
     */
    public String getJavaScript() {
        return EmaSelect.HEAD_CSS + EmaMultipleSelect.HEAD_CSS + 
            includeJavaScript + additionalJavaScript;
    }

    /** 
     * This returns the title for the "+" buttons to the right of 
     * EmaInt, EmaLong, and EmaDouble textfields. 
     *
     * @param increment the increment value (0 is special)
     * @return the appropriate title for the '+' button
     */
    public String getClickPlus(double increment) {
        if (increment <= 0)
            return clickPlusMSD;
        
        int inci = Math2.roundToInt(increment);
        String incS = increment == inci? "" + inci : "" + increment; //avoid ".0" on integers
        return String2.substitute(clickPlusX, incS, null, null);
    }

    /** 
     * This returns the title for the "-" buttons to the right of 
     * EmaInt, EmaLong, and EmaDouble textfields. 
     *
     * @param increment the increment value (0 is special)
     * @return the appropriate title for the '+' button
     */
    public String getClickMinus(double increment) {
        if (increment <= 0)
            return clickMinusMSD;
        
        int inci = Math2.roundToInt(increment);
        String incS = increment == inci? "" + inci : "" + increment; //avoid ".0" on integers
        return String2.substitute(clickMinusX, incS, null, null);
    }

    /** This returns requiredTextError still containing "{0}". */
    public String getRequiredTextError() {
        return requiredTextError;
    }

    /** This returns requiredSelectError still containing "{0}". */
    public String getRequiredSelectError() {
        return requiredSelectError;
    }

    /** This returns intError still containing "{0}", "{1}", and "{2}". */
    public String getIntError() {
        return intError;
    }

    /** This returns doubleError still containing "{0}", "{1}", and "{2}". */
    public String getDoubleError() {
        return doubleError;
    }

    /** This returns dateError still containing "{0}", "{1}", and "{2}". */
    public String getDateError() {
        return dateError;
    }

    /** This returns lengthError still containing "{0}", "{1}", and "{2}". */
    public String getLengthError() {
        return lengthError;
    }

    /** This returns the componentStyle (for example, style="font-family:arial,helvetica; font-size: smaller;")
     * which is applied to the text in components that isn't affected by 
     * normal html tags in the servlet/jsp. 
     */
    public String getComponentStyle() {
        return componentStyle;
    }

    /** 
     * This indicates the default value of the 'required' EmaAttribute property. 
     */
    public boolean getDefaultRequired() {
        return defaultRequired;
    }

    /** 
     * This indicates the default value of 'doubleWide' EmaAttribute property. 
     */
    public boolean getDefaultDoubleWide() {
        return defaultDoubleWide;
    }

    /** 
     * This indicates the default value of 'enterSubmitsForm' EmaAttribute property. 
     */
    public boolean getDefaultEnterSubmitsForm() {
        return defaultEnterSubmitsForm;
    }

    /** This returns the default string pressEnterToSubmit (used in titles as advice to users). */
    public String getPressEnterToSubmit() {
        return pressEnterToSubmit;
    }

    /** 
     * This indicates if there should be a blank row in forms after
     * doubleWide attributes. 
     */
    public boolean getSpacerAfterDoubleWide() {
        return spacerAfterDoubleWide;
    }

    /**
     * This trims the string and, if the last character is a colon, removes 
     * the colon.
     *
     * @param attributeName
     * @return the modified String
     */
    public String removeColon(String attributeName) {
        attributeName = attributeName.trim();
        return attributeName.endsWith(":")?
            attributeName.substring(0, attributeName.length()-1) :
            attributeName;
    }

    /**
     * This creates an error message indicating that no text has
     * been entered for a required textfield.
     */
    public String createRequiredTextError(String attributeName) {
        return String2.substitute(requiredTextError, 
            removeColon(attributeName), null, null);
    }

    /** 
     * This lets you change the HTML code for beginning a row of 
     * the table (normally &lt;tr style="align:left;"&gt;). 
     */
    public void setBeginRow(String code) {
        beginRow = code;
    }

    /** 
     * This returns the current HTML code for beginning a row of 
     * the table (normally &lt;tr&gt;). 
     */
    public String getBeginRow() {
        return beginRow;
    }

    /** 
     * This lets you change the HTML code for ending a row of 
     * the table (normally &lt;/tr&gt;). 
     */
    public void setEndRow(String code) {
        endRow = code;
    }

    /** 
     * This returns the current HTML code for ending a row of 
     * the table (normally &lt;tr&gt;). 
     */
    public String getEndRow() {
        return endRow;
    }

    /**
     * This adds an attribute to the internal list of attributes.
     * On the HTML form for this class, attributes will be listed
     * in the order in which they are added.
     *
     * @param newAttribute is the EmaAttribute that should be added
     *    to the attributeList for this class.
     * @throws InstantiationError if the newAttribute.getName
     *    is "", or not a valid XML name, or a duplicate of an 
     *    existing name.  This is a programming error and will
     *    be caught the first time the class is populated.
     */
    public void addAttribute(EmaAttribute newAttribute) {

        //add newAttribute to the attributeList
        attributeList.add(newAttribute);

        //ensure newAttribute.getName() is valid and add att to newAttributeMap
        String name = newAttribute.getName();
        String error = null;
        if (name.length() == 0) {
            error = 
                String2.ERROR + ": EmaClass \"" + fullClassName + "\": " +
                    "an attribute has no name.";
        } else if (!name.equals(XML.textToXMLName(name))) {
            error = 
                String2.ERROR + ": EmaClass \"" + fullClassName + "\" " +
                    "attribute name \"" + name + "\"\n" +
                "should contain only letters, digits, \"_\", \"-\", and \".\",\n" +
                "and must start with a letter or \"_\".";
        //a control named 'submit' makes onClickSubmitsForm (and onChange...) not work
        } else if (name.toLowerCase().equals("submit")) {
            error = 
                String2.ERROR + ": EmaClass \"" + fullClassName + "\" :" + name +
                "\" is a reserved attribute name. Use another name (e.g., \"submitForm\").";
        } else if (name.toLowerCase().equals("EmaWaitMessage")) {
            error = 
                String2.ERROR + ": EmaClass \"" + fullClassName + "\" :" + name +
                "\" is a reserved attribute name. Use another name (e.g., \"submitForm\").";
        //a control named 'EmaButton' interferes with EmaButtons if using name="EmaButton" for all buttons
        //} else if (name.toLowerCase().equals("emabutton")) {
        //    error = 
        //        String2.ERROR + ": EmaClass \"" + fullClassName + "\" :" + name +
        //        "\" is a reserved attribute name. Use another name (e.g., \"myButton\").";
        } else if (name.toLowerCase().equals(N_REQUESTS_THIS_SESSION)) {
            error = 
                String2.ERROR + ": EmaClass \"" + fullClassName + "\" :" + name +
                "\" is a reserved attribute name. Use another name (e.g., \"nRequests\").";
        }
        Object previousValue = attributeMap.put(name, newAttribute);
        if (previousValue != null) {
            error = 
                String2.ERROR + ": EmaClass \"" + fullClassName + "\" has two\n" +
                     "attributes named \"" + name + "\".";
        }

        //throw an exception if invalid
        if (error != null) 
            throw new InstantiationError(error);
        
    }

    /**
     * This gets one of the EmaAttributes.
     *
     * @param name the name of the attribute
     * @return an EmaAttribute (or null if the name isn't associated
     *    with an EmaAttribute)
     */
    public EmaAttribute getAttribute(String name) {

        Object o = attributeMap.get(name);
        return o instanceof EmaAttribute ea? ea : null;
    }

    /**
     * This gets the HTML code for the error message for one of the EmaAttributes.
     *
     * @param name the name of the attribute
     * @param session is the session associated with a user
     * @return the HTML code for the error message for that attribute 
     *    (or "" if the name isn't associated with an EmaAttribute
     *    or if the attribute's value is valid)
     */
    public String getErrorMessage(String name, HttpSession session) {

        EmaAttribute att = getAttribute(name);
        String error = att == null? "" : 
            att.isValid(getValue(att.getName(), session));
        return error.length() == 0? "" : 
            beginError + error + endError;
    }

    /**
     * This gets the HTML label for one of the EmaAttributes.
     *
     * @param name the name of the attribute
     * @return the HTML label for that attribute 
     *    (or "" if the name isn't associated with an EmaAttribute)
     */
    public String getLabel(String name) {

        EmaAttribute att = getAttribute(name);
        return att == null? "" : att.getLabel();
    }

    /**
     * This gets the value of one of the EmaAttributes.
     *
     * @param name the name of the attribute
     * @param session is the session associated with a user
     * @return the value (in String form) 
     *    (or "" if the name isn't associated with an EmaAttribute)
     */
    public String getValue(String name, HttpSession session) {
        EmaAttribute att = getAttribute(name);
        if (att == null) 
            return "";
        return att.getValue(session);
    }

    /**
     * This gets the HTML control for one of the EmaAttributes.
     *
     * @param name the name of the attribute
     * @param session is the session associated with a user
     * @return the HTML control for that attribute 
     *    (or "" if the name isn't associated with an EmaAttribute)
     */
    public String getControl(String name, HttpSession session) {

        EmaAttribute att = getAttribute(name);
        return att == null? "" : 
            att.getControl(att.getValue(session));
    }

    /**
     * This returns the start of the form (using the current formName and url).
     * If you need multiple forms on one document, use different emaclasses
     * to set them up (so the different input elements are associated with 
     * the different emaClasses).
     *
     * @return the HTML code for the start of the form (including beginTable
     *     and the formTitle).
     */
    public String getStartOfHTMLForm() {
        StringBuilder sb = new StringBuilder();
        sb.append("<form name=\"" + formName + "\" method=\"GET\" action=\"" + url + "\">\n");
        //onClick and onChange depend on javaScript
        sb.append(javaScriptDisabled);
        sb.append("  " + beginTable + "\n");

        //put the form title on the first row
        if (formTitle.length() > 0) 
            sb.append("    <tr><td colspan=\"2\">" + formTitle + "</td></tr>\n");

        return sb.toString();
    }

    /**
     * This returns the end of the form.
     *
     * @param startMillis the start time for processing 
     *    (or 0 if not known)
     * @param addedHtml is html that need to be put between the 
     *    and of the table and the end of the form (e.g., hidden INPUT tags)
     * @return the HTML code for the end of the form (including endTable).
     */
    public String getEndOfHTMLForm(long startMillis, String addedHtml) {
        //update statistics
        totalNFormsCreated++;
        long tTime = startMillis > 0 ? System.currentTimeMillis() - startMillis : 0;
        totalFormCreationTime += tTime;
        if (tTime > maxFormCreationTime) 
            maxFormCreationTime = tTime;

        StringBuilder sb = new StringBuilder();
        sb.append("  " + endTable + "\n" +
                  addedHtml +
                  "</form>\n");
        sb.append("<!-- This form generated " + 
            Calendar2.getCurrentISODateTimeStringLocalTZ() + 
//            " by Ema (www.cohortsoftware.com)" +
            (startMillis > 0 ? 
                " in " + tTime + " ms" :
                "") +
            ". -->\n");
        return sb.toString();
    }

    /**
     * This creates the POST HTML form with the EmaAttributes.
     * Override this is you want to format the form differently.
     * This is separate from processRequest because the form may be
     * a small portion of the web page.
     *
     * @param request is a request from a user
     * @param displayErrorMessages if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the HTML code for the form.
     *    The line separator is the newline character.
     */
    public String getHTMLForm(HttpServletRequest request, boolean displayErrorMessages) {
        long startTime = System.currentTimeMillis();
        HttpSession session = request.getSession();
        StringBuilder sb = new StringBuilder(getStartOfHTMLForm());
     
        //create the rows of the table with the attributes
        int nAtts = attributeList.size();
        StringBuilder hiddenInfo = new StringBuilder();
        for (int i = 0; i < nAtts; i++) {
            EmaAttribute att = (EmaAttribute)(attributeList.get(i));
            if (att instanceof EmaHidden) 
                hiddenInfo.append(att.getControl(getValue(att.getName(), session)));
            else sb.append(
                att.getTableEntry(getValue(att.getName(), session), displayErrorMessages));
        }

        //end of table, end of form
        sb.append(getEndOfHTMLForm(startTime, hiddenInfo.toString()));

        return sb.toString();
    }


    /**
     * This creates a complete HTML page with the HTML form for this class.
     * Override this is you want to put other things on the page.
     *
     * @param request is a request from a user
     * @param displayErrorMessages if false (for example, the first time the
     *     user sees the page), error messages aren't displayed
     * @return the HTML code for the web page
     */
    public String getHTMLPage(HttpServletRequest request, boolean displayErrorMessages) {
        StringBuilder sb = new StringBuilder();
        //I note that this 4.01 DOCTYPE tag causes radio buttons in Mac IE to have the 
        //wrong background color.  But I still think it is the proper thing to do.
        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
            "\"https://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n");
        if (windowTitle.length() > 0) 
            sb.append("<title>" + windowTitle + "</title>\n"); 
        sb.append(getJavaScript());
        sb.append("</head>\n");
        sb.append("\n");
        sb.append("<body style=\"font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n");
        sb.append(getHTMLForm(request, displayErrorMessages));
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    /**
     * This adds all of the default attribute values to the session
     * and sets N_REQUESTS_THIS_SESSION to 0.
     *
     * @return this returns the number of times this has been called total
     *   (not just for this session)
     */
    public int addDefaultsToSession(HttpSession session) {
        session.setAttribute(fullClassName + "." + N_REQUESTS_THIS_SESSION, "0");

        int n = attributeList.size();
        for (int i = 0; i < n; i++) {
            EmaAttribute att = (EmaAttribute)attributeList.get(i);
            att.setValue(session, att.getDefaultValue());
        }
        return ++nSessions;
    }

    /**
     * This removes all of this classes attributes from the session.
     */
    public void removeAttributesFromSession(HttpSession session) {
        session.removeAttribute(fullClassName + "." + N_REQUESTS_THIS_SESSION);

        int n = attributeList.size();
        for (int i = 0; i < n; i++) {
            ((EmaAttribute)attributeList.get(i)).removeValue(session);
        }
    }

    /**
     * Whether this class is a JSP or a servlet, this handles
     * a "request" from a user, storing incoming attributes
     * as session values.
     * This updates totalNRequests, totalProcessingTime, maxProcessingTime.
     *
     * @param request 
     * @return true if all the values on the form are valid
     */
    public boolean processRequest(HttpServletRequest request) {   
        int nrts = getNRequestsThisSession(request.getSession());
        //log("EmaClass.processRequest.nRequestsThisSession = " + nrts);
        request.getSession().setAttribute(
            fullClassName + "." + N_REQUESTS_THIS_SESSION, "" + (nrts + 1));

        boolean allValid = true;
        int nAtts = attributeList.size();
        String2.log("processRequest nAtts=n");
        for (int i = 0; i < nAtts; i++) {
            if (!((EmaAttribute)attributeList.get(i)).processRequest(request))
                allValid = false;
        }

        return allValid;
    }

    /**
     * This returns the time (millis) since instantiation. 
     *
     * @return the time (in milis) since instantiation.
     */
    public long getTimeSinceInstantiation() {   
        return System.currentTimeMillis() - instantiationTime;
    }

    /**
     * This returns the total number of times getHTMLForm has been called since
     * instantiation (all sessions).
     * This is updated in getEndOfHTMLForm.
     *
     * @return the total number of times getHTMLForm has been called
     */
    public int getTotalNFormsCreated() {   
        return totalNFormsCreated;
    }

    /**
     * This returns the total time spent in getHTMLForm since
     * instantiation (all sessions).
     * This is updated in getEndOfHTMLForm.
     *
     * @return the total time in getHTMLForm (in millis) since instantiation
     */
    public long getTotalFormCreationTime() {   
        return totalFormCreationTime;
    }

    /**
     * This returns the max time spent in getHTMLForm since
     * instantiation (all sessions).
     * This is updated in getEndOfHTMLForm.
     *
     * @return the max time in getHTMLForm (in millis) since instantiation
     */
    public long getMaxFormCreationTime() {   
        return maxFormCreationTime;
    }

    /**
     * This returns a string of statistics about the usage of this web app.
     *
     * @return a string with statistics about the usage of this web app.
     */
    public String getUsageStatistics() {
        long time = Math.max(1, getTimeSinceInstantiation());
        return "Usage statistics for " + fullClassName + " (" + 
                Calendar2.getCurrentISODateTimeStringLocalTZ() + "):\n" +
            "  time since instantiation = " + Calendar2.elapsedTimeString(time) + "\n" +
            "  total number of forms created since instantiation = " + totalNFormsCreated + 
                " (average = " + 
                ((totalNFormsCreated * 86400000L) / time) + "/day).\n" +       
            "  average time to create a form = " + 
                ((totalFormCreationTime * 1000 / Math.max(1, totalNFormsCreated)) / 1000.0) + " ms\n" +       
            "  max time to create a form = " + 
                maxFormCreationTime + " ms\n" +       
            "  number of user sessions created since instantiation = " + nSessions +
                " (average = " + 
                ((nSessions * 86400000L) / time) + "/day).";       
    }

    /**
     * This attempts to get the name of the button (or other HTML control) 
     * which was used to submit the form.
     *
     * @param request 
     * @return the name of the button which submitted the form
     *    (or "" if it was some other attribute, e.g., with enterSubmitsForm)
     */
    public String getSubmitterButtonName(HttpServletRequest request) {   

/*
        int i, nAtts = attributeList.size();
        String value = (String)request.getParameter("EmaButton");
        if (value == null || value.length() == 0)
            return "";
        for (i = 0; i < nAtts; i++) {
            EmaAttribute att = (EmaAttribute)attributeList.get(i);
            if (att instanceof EmaButton) {
                if (value.equals(att.getDefaultValue())) {
                    return att.getName();
                }
            }
        }
        return "";
*/
        int i, nAtts = attributeList.size();
        for (i = 0; i < nAtts; i++) {
            EmaAttribute att = (EmaAttribute)attributeList.get(i);
            if (att instanceof EmaButton) {
                //if this is what user clicked, name=name will be returned in the request
                String name  = att.getName();
                String value = (String)request.getParameter(name);
                if (value != null && att.getDefaultValue().equals(value)) {
                    return att.getName();
                }
            }
        }
        return "";
    }

    /**
     * This returns the number of requests made in this user session.
     *
     * @param session usually created with request.getSession()
     * @return the number of requests made in this user session
     *    (or -1 if defaults haven't even been set)
     */
    public int getNRequestsThisSession(HttpSession session) {
        return String2.parseInt((String)session.getAttribute(
            fullClassName + "." + N_REQUESTS_THIS_SESSION), -1);
    }

    /**
     * When this class is used as a servlet, this responds to 
     * a "get" request from the user.
     *
     * @param request 
     * @param response
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        //process the request
        //String2.log("EmaClass.doGet");
        processRequest(request);

        //output the response html page 
        response.setContentType("text/html");
        response.setCharacterEncoding(File2.UTF_8);
        Writer out = File2.getBufferedWriterUtf8(response.getOutputStream());
        out.write(getHTMLPage(request, request.getContentLength() > 0)); //displayErrorMessages
        out.flush(); //close it???
    }

    /**
     * When this class is used as a servlet, this responds to
     * a "post" request from the user.
     *
     * @param request 
     * @param response
     */
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {
        doGet(request, response);
    }

}
