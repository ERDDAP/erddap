/* 
 * HtmlWidgets Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.util.SSR;

import java.awt.Color;
import java.io.FileWriter;
import java.io.Writer;
import java.text.MessageFormat;

/**
 * HtmlWidgets has methods to simplify creation of widgets in
 * HTML forms.
 *
 * <p>"name" vs. "id" - for widgets, "name" is the required attributes.
 * "id" is not required and causes problems.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-09-04
 */
public class HtmlWidgets {

    /** Leave this set to false here, but change within a program if desired. */
    public static boolean debugMode = false;

    /** This is the standard start of an HTML 4.01 document up to and including the 'head' tag. */
    public final static String DOCTYPE_HTML_TRANSITIONAL = 
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \n" +
        "  \"http://www.w3.org/TR/html4/loose.dtd\"> \n" +
        "<html>\n" +
        "<head>\n";

   /** 
    * This is the standard start of an XHTML 1.0 document up to and including the 'head' tag.
    * This is specific to UTF-8. 
    * See comments about making xhtml render properly in browsers:
    *   Must use UTF-8; remove ?xml header; use mime type text/html;
    *   http://www.w3.org/TR/xhtml1/#guidelines
    * <br>But it is more important that it be proper xml -- so leave xml tag in.
    * <br>More useful clues from http://www.w3.org/MarkUp/Forms/2003/xforms-for-html-authors
    *   which is xhtml that renders correctly.
    *   It needs head/meta tags and head/style tags. (see below)
    *   And use xhmtl validator at http://validator.w3.org/#validate_by_input for testing.
    * <br>In OutputStreamFromHttpResponse, don't use response.setHeader("Content-Disposition","attachment;filename="
    *   for xhtml files.
    */
    public final static String DOCTYPE_XHTML_TRANSITIONAL = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
        "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" + //from www.w3.org documents -- it's visible in all browsers!
        "<head>\n" +
        "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n";// +

    public final static String SANS_SERIF_STYLE = 
        "font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;"; 

    /** This is the HTML for a mini form with a back button (which uses the browsers
     * history to determine what to go back to. */
    public final static String BACK_BUTTON =
        "<form action=\"\">\n" +
        "<input type=\"button\" value=\"Back\" onClick=\"history.go(-1);return true;\">\n" +
        "</form>\n"; 

    public final static int BUTTONS_0n = -1, BUTTONS_1 = -2, BUTTONS_100 = -8,
        BUTTONS_1000 = -16;
   
    /** One line: white,grays,black, then one rainbow. */
    public final static String PALETTE17[] = {
        "FFFFFF", "CCCCCC", "999999", "666666", "000000", 
        "FF0000", "FF9900", "FFFF00", "99FF00", "00FF00", "00FF99", 
        "00FFFF", "0099FF", "0000FF", "9900FF", "FF00FF", "FF99FF"};

    /** This will display a message to the user if JavaScript is not supported
     * or disabled. Last updated 2013-05-03. */
    public static String ifJavaScriptDisabled =
        "<noscript><p><font color=\"red\"><b>To work correctly, this web page requires that JavaScript be enabled in your browser.</b> Please:\n" +
        "<br>1) Enable JavaScript in your browser:\n" +
        "<br>&nbsp;&nbsp; &bull; Windows\n" +  
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Chrome: select \"Settings : Show advanced settings : Privacy / Content settings : JavaScript\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Firefox: select \"Options : Options : Content : Enable JavaScript : OK\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Internet Explorer: select \n" +  //true for IE 6 and 7 
        "   \"Tools : Internet Options : Security : Internet : Custom level :\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Sripting/ Active Scripting : Enable : OK : OK\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Opera: select \"Settings : Quick Preferences : Enable JavaScript\"\n" +
        "<br>&nbsp;&nbsp; &bull; Mac OS X\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Chrome: select \"Settings : Show advanced settings : Privacy / Content settings : JavaScript\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Firefox: select \"Firefox : Preferences : Content : Enable JavaScript : OK\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Internet Explorer (this is an out-of-date browser -- please consider switching): \n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; select \"Explorer : Preferences : Web Content : Enable Scripting : OK\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Opera: select \"Opera : Quick Preferences : Enable JavaScript\"\n" +
        "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &bull; Safari: select \"Safari : Preferences : Security : Enable JavaScript\"\n" + 
        "<br>2) Reload this web page.\n" +
        "<br>&nbsp;</font>\n" +
        "</noscript>\n";

    /** The default tooltip for twoClickMap. */
    public static String twoClickMapDefaultTooltip =
        "Specify a rectangle by clicking on two diagonal corners.  Do it again if needed.";

    public static String errorXWasntSpecified = "Error: \"{0}\" wasn''t specified.";
    public static String errorXWasTooLong = "\"{0}\" was more than {1} characters long.";

    /**
     * If you want to use Tip for big html tooltips (see below),
     * include this in the 'body' of a web page (preferably right after the body tag).
     * General info: http://wztip.info/index.php/Main_Page
     * (was http://www.walterzorn.com/tooltip/tooltip_e.htm) (LGPL License -- so give credit).
     * By making this a reference to a file (not inline text), the .js file is 
     * downloaded once to the user's browser, then cached if Expired header is set.
     * 
     * <p>I last downloaded wz_tooltip.js on 2009-06-19 ver. 5.31.
     * In wz_tooltip.js, I changed default FontSize from 8pt to 10pt
     * config. FontSize		= '10pt' 	// E.g. '9pt' or '12px' - unit is mandatory
     *
     * @param dir a public directory from the web page's point of view 
     *   (e.g., EDStatic.imageDirUrl(loggedInAs)) where wz_tooltip.js can be found.
     */
    public static String htmlTooltipScript(String dir) {
        return
        "<!-- Big HTML tooltips are generated with wz_tooltip from \n" +
        "    http://wztip.info/index.php/Main_Page (LGPL license) -->\n" +
        "<script type=\"text/javascript\" src=\"" + 
            XML.encodeAsHTMLAttribute(dir) + "wz_tooltip.js\"></script>\n" +
        "\n";
    }

    /** 
     * This is the width and height of the sliderLeft.gif, sliderCenter.gif, 
     * and sliderRight.gif images in imageDirUrl.
     * sliderBg.gif must have sliderThumbHeight.
     * (These were created in Paint, not CoPlot.)
     */
    public static int sliderThumbWidth = 15;
    public static int sliderThumbHeight = 17;

    /**
     * This style removes the extra padding on the left and right of a button.
     * But for ERDDAP, see CSS style input.skinny in setup.xml.
     */
    public static String skinnyButtonStyle = "style=\"margin:0px; padding:0px 1px\" ";  //top/bottom  left/right 

    /**
     * This returns the html to include Walter Zorn's image dragDrop script 
     * for the 'body' of a web page (preferably right after the body tag).
     * General info: [website is gone!]
     * (was http://www.walterzorn.com/dragdrop/dragdrop_e.htm) (LGPL License -- so give credit).
     * By making this a reference to a file (not inline text), the .js file is 
     * downloaded once to the user's browser, then cached if Expired header is set.
     * 
     * @param dir a public directory from the web page's point of view (e.g., EDStatic.imageDirUrl()) 
     *   where wz_dragdrop.js can be found.
     */
    public static String dragDropScript(String dir) {
        return
        "<!-- Drag and Drop is performed by wz_dragdrop from\n" +
        "     http://www.walterzorn.com/dragdrop/dragdrop_e.htm (LGPL license) -->\n" +
        "<script type=\"text/javascript\" src=\"" + 
            XML.encodeAsHTMLAttribute(dir) + "wz_dragdrop.js\"></script>\n" +
        "\n";
    }

    /** This is a standalone javascript which does minimal percent encoding of a string,
     *  similar to SSR.minimalPercentEncode.
     */
    public static String PERCENT_ENCODE_JS =
        //browser handles other chars, but needs help with + & " ' space, and thus %
        "<script type=\"text/javascript\"> \n" +
        "function percentEncode(s) { \n" + 
        "  var s2=\"\";\n" +
        "  for (var i = 0; i < s.length; i++) {\n" +
        "    var ch=s.charAt(i);\n" +
        "    if (ch == \"%\") s2+=\"%25\";\n" +
        "    else if (ch == \"&\") s2+=\"%26\";\n" +     //to distinguish & in value in &id=value
        "    else if (ch == \"\\\"\") s2+=\"%22\";\n" +  //avoids trouble with " in urls in javascript 
        "    else if (ch == \"'\") s2+=\"%27\";\n" +     //avoids trouble with ' in urls in javascript
        "    else if (ch == \"+\") s2+=\"%2B\";\n" +     //avoid trouble with +
        "    else if (ch == \" \" || ch == \"\\xA0\") s2+=\"%20\";\n" +  //safer than +   0xA0=nbsp, see select(encodeSpaces)
        //see slide 7 of https://www.owasp.org/images/b/ba/AppsecEU09_CarettoniDiPaola_v0.8.pdf
        //reserved=; / ? : @ & = + $ ,
        "    else if (ch == \"=\") s2+=\"%3D\";\n" +
        "    else if (ch == \"#\") s2+=\"%23\";\n" +  
        "    else if (ch == \"<\") s2+=\"%3C\";\n" +
        "    else if (ch == \">\") s2+=\"%3E\";\n" +
        "    else if (ch == \";\") s2+=\"%3B\";\n" +
        "    else if (ch == \"/\") s2+=\"%2F\";\n" +
        "    else if (ch == \"?\") s2+=\"%3F\";\n" +
        //"    else if (ch == \":\") s2+=\"%3A\";\n" +
        "    else if (ch == \"@\") s2+=\"%40\";\n" +
        "    else if (ch == \"$\") s2+=\"%24\";\n" +
        "    else s2+=ch;\n" +
        "  }\n" +
        "  return s2;\n" +
        "}\n" +
        "</script>\n";

    // *********************** set by constructor  ****************************

    //these can be changed as needed while creating a form
    public String style;
    public boolean htmlTooltips;
    public String imageDirUrl;

    //this is set by beginForm
    public String formName;

    /** If true, pressing Enter in a textField submits the form (default = false). */
    public boolean enterTextSubmitsForm = false;

    /** The default constructor. */
    public HtmlWidgets() {
        this("", false, "");
    }

    /** A constructor that lets you add 
     * style information (e.g., "")
     * and specify the number of items to be shown at a time by select() widgets.
     *
     * @param tStyle sets the style for widgets that need a style in addition to  
     *    the current document style, e.g., SANS_SERIF_STLYE or "" if none.
     *    Last I looked, no client uses this -- better to use CSS style in document head tag.
     * @param tHtmlTooltips if true, tooltip text can be any HTML text of any length.
     *     If true, caller must have called htmlTooltipScript above.
     *     If false, tooltips are plain text and should be (less than 60 char on some browsers).
     * @param tImageDirUrl the public url for the image dir which has the
     *     arrow, p... and m... .gif files for the 'select' buttons
     *     (or null or "" if not needed).  Usually EDStatic.imageDirUrl(loggedInAs)
     */
    public HtmlWidgets(String tStyle, boolean tHtmlTooltips, String tImageDirUrl) {
        style = tStyle;
        htmlTooltips = tHtmlTooltips;
        imageDirUrl = tImageDirUrl;
    }

    /**
     * This changes the style held by this class and returns the old style.
     * @param newStyle
     * @return oldStyle
     */    
    public String setStyle(String newStyle) {
        String oldStyle = style;
        style = newStyle;
        return oldStyle;
    }
 
    /** This is mostly used internally; it returns the formatted style. */
    public String completeStyle() {
        return completeStyle(style);
    }

    /** This is mostly used internally; it returns the formatted style. */
    public String completeStyle(String tStyle) {
        //note space at beginning and end
        return tStyle == null || tStyle.length() == 0? "" : 
            "\n      style=\"" + XML.encodeAsHTMLAttribute(tStyle) + "\" ";
    }

    /**
     * This is mostly used internally; it returns the formatted tooltip.
     *
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     */
    public String completeTooltip(String tooltip) {
        if (tooltip == null || tooltip.length() == 0)
            return ""; 
        //note space at beginning and end
        return htmlTooltips?
            "\n      " + htmlTooltip(tooltip) + " " :
            "\n      title=\"" + XML.encodeAsHTMLAttribute(tooltip) + "\" ";
    }

    /**
     * This creates the HTML code for the beginning of a form.
     *
     * @param name  make it unique for this page
     * @param method usually GET or POST
     * @param url  the url to which the form information will be submitted
     * @param other other attributes  (or "" if none)
     * @return the HTML code for the beginning of a form.
     */
    public String beginForm(String name, String method, String url, String other) {
        formName = name;
        return 
            "<form name=\"" + XML.encodeAsHTMLAttribute(name) + 
            "\" method=\"" + XML.encodeAsHTMLAttribute(method) + "\"\n" +
            "  action=\"" + XML.encodeAsHTMLAttribute(url) + "\" " + 
            other + " >\n";
    }

    /**
     * This creates the HTML code for the end of a form.
     *
     * @return the HTML code for the end of a form.
     */
    public String endForm() {
        return "</form>\n";
    }

    /** 
     * This creates the HTML code to begin a table.
     *
     * @param lineWidth  width of lines (in pixels, 0 = none)
     * @param cellPadding 0 for really tight, 2 for comfortable
     * @param other attributes e.g., "width=\"2%\" bgcolor=\"" + bgColor + "\" style=\"...\"" 
     * @return the HTML code to begin a table.
     */
    public String beginTable(int lineWidth, int cellPadding, String other) { 
        return 
            "<table border=\"" + lineWidth + "\" " + other + 
            " cellspacing=\"0\" cellpadding=\"" + cellPadding + "\">\n";
    }

    /** 
     * This creates the HTML code to begin a table.
     *
     * @param other attributes e.g., "style=\"width:100%;\" class=\"b0p0\"" 
     * @return the HTML code to begin a table.
     */
    public String beginTable(String other) { 
        return 
            "<table " + other + ">\n";
    }

    /** 
     * This creates the HTML code to end a table.
     *
     * @return the HTML code to end a table.
     */
    public String endTable() {
        return "  </table>\n";
    }

    /**
     * This creates the HTML code for a button.
     * "labelText" is displayed and name=labelText is returned if clicked
     *
     * @param type "submit" or "button".  
     *    "submit" buttons submit the form to the form's url.
     *    "button" buttons do "other" things (via 'other').
     * @param name of the widget (name=labelText is returned when the form is submitted)
     *    (or null or "" if you don't need anything submitted with form)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param labelText the plain text label visible to the user 
     * @param other e.g., "onclick=\"pleaseWait();\""
     * @return the HTML code for a button.
     */
    public String button(String type, String name, String tooltip, String labelText, 
        String other) {
        return 
            //note old-style "input" tag
            "<input type=\"" + type + "\"" +
            (name != null && name.length() > 0? " name=\"" + XML.encodeAsHTMLAttribute(name) + "\"" : "") +                
            " value=\"" + XML.encodeAsHTMLAttribute(labelText) + "\"" +
            " " + other + 
            completeStyle() +
            completeTooltip(tooltip)+ 
            " >\n"; 
    }

    /**
     * This creates the HTML code for a button widget.
     * The labelTextHtml is displayed and name=labelText is returned if clicked
     *
     * @param type "submit" or "button".  
     *    "submit" buttons submit the form to the form's url.
     *    "button" buttons do "other" things (via 'other').
     * @param name of the widget (name=labelText is returned when the form is submitted)
     *    (or null or "" if you don't need anything submitted with form)
     * @param value is the value returned when the form is submitted (name=value)
     *    (or null or "" if you don't need anything submitted with form)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param labelHtml the html which will label the button
     *     (e.g.,an img tag)
     * @param other e.g., "onclick=\"pleaseWait();\""
     * @return the HTML code for the button widget.
     */
    public String htmlButton(String type, String name, String value, String tooltip, 
        String labelHtml, String other) {
        return 
            //note new-style "button" tag
            "<button type=\"" + type + "\"" +
            (name  != null && name.length()  > 0? " name=\""  + XML.encodeAsHTMLAttribute(name) + "\"" : "") +                
            (value != null && value.length() > 0? " value=\"" + XML.encodeAsHTMLAttribute(value) + "\"" : "") +
            " " + other + 
            completeStyle() +
            completeTooltip(tooltip)+ 
            " >" + labelHtml + "</button>\n"; 
    }

    /**
     * This creates the HTML code for a checkbox.
     * This also makes a "hidden" widget called "previous_"+name with
     *    value=[checked] since un-checked checkboxes don't return info 
     *    with the form.
     *
     * @param name the name of the widget (if checked, name=value is returned when the form is submitted)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param checked the initial state of the checkbox
     * @param value (sometimes just "true")
     * @param rightLabelHtml "" if none
     * @param other e.g., "onclick=\"pleaseWait();\"".
     *    For checkbox(), use onclick, not onchange (which IE doesn't generate).
     * @return the HTML code for a checkbox.
     */
    public String checkbox(String name, String tooltip, 
        boolean checked, String value, String rightLabelHtml, String other) {

        return 
            "    <input type=\"checkbox\" name=\"" + XML.encodeAsHTMLAttribute(name) + "\"" +                
            " value=\"" + XML.encodeAsHTMLAttribute(value) + "\"" + 
            completeTooltip(tooltip) +
            (checked? "\n        checked=\"checked\"" : "") +
            " " + other + " > " + rightLabelHtml + "\n" +

            //add hidden so processRequest can distinguish notChecked from 
            //first time user (default)
            hidden("previous_" + name, "" + checked);
    }

    /**
     * This create the HTML code for a comment.
     *
     * @param commentText
     * @return the HTML code for a comment.
     */
    public String comment(String comment) {
        return "    <!-- " + XML.encodeAsHTML(comment) + " -->\n"; 
    }

    /**
     * This creates the HTML code for a hidden widget.
     *
     * @param name the name of the widget (name=value is returned when the form is submitted)
     * @param value is the value of this attribute
     * @return the HTML code for a hidden widget.
     */
    public String hidden(String name, String value) {
        return 
            "    <input type=\"hidden\" name=\"" + XML.encodeAsHTMLAttribute(name) + "\"" +                
            " value=\"" + XML.encodeAsHTMLAttribute(value) + "\" >\n";
    }


    /**
     * This creates the HTML code for a series of radio buttons.
     *
     * @param name the name of the widget (name=optionText is returned when the form is submitted)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param onARow  if true, the radio buttons will all be put on one row.
     *    Otherwise, they are put on separate lines separated by a linebreak.
     * @param optionTexts which provides the plain text to be displayed for each 
     *   of the options (special characters will be html-encoded).
     * @param selected the index of the selected item or -1 if none
     * @param other e.g., "onclick=\"pleaseWait();\""
     *    For radioButtons(), use onclick, not onchange (which IE doesn't generate).
     * @return the HTML code for a series of radio buttons.
     */
    public String radioButtons(String name, String tooltip, boolean onARow,
        String optionTexts[], int selected, String other) {

        StringBuilder sb = new StringBuilder();
        String br = onARow? "    " : "    <br>";
        for (int i = 0; i < optionTexts.length; i++) {
            sb.append(i == 0? "    " : br);              
            sb.append(radioButton(name, tooltip, optionTexts[i], i == selected, other));
        }
        return sb.toString();
    }

    /**
     * This creates the HTML code for one radio button.
     *
     * @param name the name of the widget (name=optionText is returned when the form is submitted)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param optionText which provides the plain text to be displayed for this option.
     * @param selected indicates if this item is selected
     * @param other e.g., "onclick=\"pleaseWait();\""
     *    For radioButton(), use onclick, not onchange (which IE doesn't generate).
     * @return the HTML code for one radio button.
     */
    public String radioButton(String name, String tooltip, 
        String optionText, boolean selected, String other) {

        String s = XML.encodeAsHTMLAttribute(optionText);
        return 
            //<span> avoids check box and value being separated by newline when lots of options
            "<span style=\"white-space: nowrap;\"><input type=\"radio\" name=\"" + 
            XML.encodeAsHTMLAttribute(name) + "\"" +                
            " value=\"" + s + "\"" + 
            (selected? " checked" : "") +
            completeTooltip(tooltip) +
            " " + other + " >" + s + "</span>\n";
    }

    /**
     * This creates the HTML code to display 17 radio buttons with colored backgrounds.
     *
     * @return the HTML code to display 17 radio buttons with colored backgrounds.
     */
    public String color17(String htmlLabel, String name, String tooltip, 
        int selected, String other) {
        return color(PALETTE17, 17, htmlLabel, name, tooltip, selected, other);
    }

    /**
     * This creates the HTML code to display radio buttons with colored backgrounds.
     *
     * @param htmlLabel is an optional label (e.g., "Color: "),
     *   or use "" for nothing.
     * @param colors e.g., PALETTE17, each must be an RRGGBB value
     * @param perRow e.g., 17
     * @param selected can be -1 if none should be selected initially
     * @param other e.g., "onclick=\"pleaseWait();\""
     *    For color(), use onclick, not onchange (which IE doesn't generate).
     * @return the HTML code to display radio buttons with colored backgrounds.
     */
    public String color(String[] colors, int perRow, 
        String htmlLabel, String name, String tooltip, int selected, String other) {

        StringBuilder sb = new StringBuilder();
        sb.append(
            "<table border=\"0\" cellspacing=\"0\" cellpadding=\"1\">\n" +
            "  <tr>\n");
        boolean hasLabel = htmlLabel != null && htmlLabel.length() > 0;
        if (hasLabel)
            sb.append("    <td nowrap>" + htmlLabel + "</td>\n");
        int inRow = 0;
        for (int i = 0; i < colors.length; i++) {
            //a radio button with the appropriate background color
            String checked = i == selected? " checked" : "";
            sb.append(
                "    <td bgcolor=\"#" + colors[i] + "\">" +
                    "<input type=\"radio\" name=\"" + XML.encodeAsHTMLAttribute(name) + 
                    "\" value=\"" + colors[i] + "\"" + checked + " " +
                    completeTooltip(tooltip) +
                    " " + other + "></td>\n");

            //new row?
            if (++inRow % perRow == 0 && i != colors.length - 1)
                sb.append(
                    "  </tr>\n" +
                    "  <tr>\n" +
                    (hasLabel? "    <td>&nbsp;</td>\n" : ""));
        }
        sb.append(
            "  </tr>\n" +
            "</table>\n");
        return sb.toString();
    }

    /**
     * This creates the HTML code for a list in a box or a dropdown list.
     * If nRows &lt; 0, this uses a table to bring the elements close together,
     * and so may need to be in an enclosing table if you want other items
     * on same line.
     *
     * @param name the name of the widget (name=value is returned when the form is submitted)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param nRows &gt; 1 (e.g. 8) to display many options,
     *   <br>1=1 row with no buttons, 
     *   <br>nRows is negative implies some combination of 
     *     BUTTONS_0n, BUTTONS_1, BUTTONS_100, BUTTONS_1000
     * @param options which provides the plain text to be displayed for each of the options.
     * @param selected the index of the selected item or -1 if none
     * @param other e.g., "onchange=\"pleaseWait();\""
     *    For select(), use onchange, not onclick.
     * @return the HTML code for a list in a box or a dropdown list.
     */
    public String select(String name, String tooltip, int nRows,
        String options[], int selected, String other) {

        return select(name, tooltip, nRows, options, selected, other, false, "");
    }

    /**
     * This variant of select deals with a special case where 
     * JavaScript code that transfers an option to a textfield
     * e.g., document.form1.val0_0.value=this.options[this.selectedIndex].text
     * converts internal &gt;1 space ("ab   c") into 1 space ("ab c").
     * This also solves the problem of leading and trailing spaces being trimmed 
     * by the JavaScript code.
     * Set encodeSpaces=true for these special situations.
     * This requires the little change I made in percentEncode above:
     *   nbsp (char A0) is now percent encoded as %20 (a space).
     */    
    public String select(String name, String tooltip, int nRows,
        String options[], int selected, String other, boolean encodeSpaces) {

        return select(name, tooltip, nRows, options, selected, other, encodeSpaces, "");
     }

    /**
     * This variant of select deals with a special case where 
     * JavaScript code that transfers an option to a textfield
     * e.g., document.form1.val0_0.value=this.options[this.selectedIndex].text
     * converts internal &gt;1 space ("ab   c") into 1 space ("ab c").
     * This also solves the problem of leading and trailing spaces being trimmed 
     * by the JavaScript code.
     * Set encodeSpaces=true for these special situations.
     * This requires the little change I made in percentEncode above:
     *   nbsp (char A0) is now percent encoded as %20 (a space).
     */    
    public String select(String name, String tooltip, int nRows,
        String options[], int selected, String other, boolean encodeSpaces, 
        String buttonJS) {

        StringBuilder sb = new StringBuilder();
        int nOptions = options.length;
        if (selected < 0 || selected >= nOptions)
            selected = -1;
        if (nRows > nOptions)
            nRows = nOptions;

        //if buttons visible, put select and buttons in a table (to pack close together)
        if (nRows < 0 && nOptions > 1) {
            sb.append(beginTable(0, 0, "width=\"2%\"")); 
            sb.append("      <tr><td>");//td for <select>
        }

        //the main 'select' widget
        sb.append("\n    <select name=\"" + XML.encodeAsHTMLAttribute(name) + 
            "\" size=\"" + Math.max(1, nRows) + "\"" +
            completeTooltip(tooltip) +
            completeStyle() + 
            " " + other + " >\n");

        String spacer = nOptions < 20? "      " : ""; //save space if lots of options
        for (int i = 0; i < nOptions; i++) {
            //Security issue: the options are not user-specified.  And they are not HTML attributes.
            //I don't think encodeAsHTMLAttributes is warranted and I know it will cause problems with encodeSpaces.
            String opt = XML.encodeAsHTML(options[i]);  
            if (encodeSpaces)
                opt = XML.minimalEncodeSpaces(opt);
            sb.append(spacer + "<option" + 
                (i == selected? " selected=\"selected\"" : "") + 
                //If option is "", Win IE 7 needs 'value' to be explicitly set 
                //  for (name).value to work in JavaScript
                //  and for value to be returned when form is submitted.
                //  I tried 'label' to be used as label, text, and value; didn't work.
                //Leading and trailing spaces are always ignored.
                //  Adding value= attribute doesn't solve the problem
                //  ((options[i].startsWith(" ") || options[i].endsWith(" "))?
                //    " value=\"" + XML.encodeAsHTML(options[i]) + "\"" : "") + 
                //2010-07-28 I changed from .text to .value in javascript [bad idea!].
                //2010-08-24 I changed back to .text in javascript, 
                //  see http://jszen.blogspot.com/2007/01/ie6-select-value-gotcha.html
                //  so I removed
                //  (opt.length() == 0? " value=\"\"" : "") +
                //2012-04-18 javascript code that transfers an option to a textfield
                //  e.g., document.form1.val0_0.value=this.options[this.selectedIndex].text
                //  converts internal >1 space ("ab   c") into 1 space ("ab c").
                ">" + opt +
                ("".equals(opt)? "</option>" : "") +
                //</option> is often not used and is not required.  
                "\n"); 
        }
        sb.append("    </select>\n");

        //the buttons
        if (nRows < 0 && nOptions > 1) {
            sb.append("        </td>\n"); //end of <select>'s td

            //        selectButton(name, nRows, test,    nOptions, minNOptions, incr,            img,             tooltip);
            sb.append(selectButton(name, nRows, BUTTONS_0n,   nOptions, 2,    Integer.MIN_VALUE, "arrowLL.gif",   "Select the first item.",    buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_1000, nOptions, 1100, -1000,             "minus1000.gif", "Jump back 1000 items.",     buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_100,  nOptions, 110,  -100,              "minus100.gif",  "Jump back 100 items.",      buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_1,    nOptions, 2,    -1,                "minus.gif",     "Select the previous item.", buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_1,    nOptions, 2,    1,                 "plus.gif",      "Select the next item.",     buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_100,  nOptions, 110,  100,               "plus100.gif",   "Jump forward 100 items.",   buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_1000, nOptions, 1100, 1000,              "plus1000.gif",  "Jump forward 1000 items.",  buttonJS));
            sb.append(selectButton(name, nRows, BUTTONS_0n,   nOptions, 2,    Integer.MAX_VALUE, "arrowRR.gif",   "Select the last item.",     buttonJS));

            sb.append(
                "\n        </tr>" +
                "\n      </table>\n");
        }
        return sb.toString();
    }

    /**
     * This creates the HTML to make a button to assist select().
     * This is used by select() to make the e.g., -100, +100 buttons.
     *
     * @param name the name of the select widget
     * @param nRows &gt; 1 (e.g. 8) to display many options,
     *   <br>1=1 row with no buttons, 
     *   <br>nRows is negative implies some combination of 
     *     BUTTONS_0n, BUTTONS_1, BUTTONS_100, BUTTONS_1000
     * @param test works with nRows to determine if this button should be created
     * @param nOptions select.length
     * @param minNOptions the minimum number of options for this to be shown
     * @param incr the effect of the button, forward or back incr items
     * @param imageName e.g., plus100.gif
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param buttonJS additional javascript for the button
     * @return the HTML to make a button to assist select().
     */
    private String selectButton(String name, int nRows, int test, 
        int nOptions, int minNOptions, int incr, String imageName, String tooltip,
        String buttonJS) {

        StringBuilder sb = new StringBuilder();
        if ((-nRows & -test) != 0 && nOptions >= minNOptions) {
            String rhs = "0";
            if (incr == Integer.MIN_VALUE)      {}  //stay with 0
            else if (incr < 0)                  rhs  = "Math.max(0,sel.selectedIndex-" + -incr + ")";
            else if (incr == Integer.MAX_VALUE) rhs  = "sel.length-1"; 
            else if (incr > 0)                  rhs  = "Math.min(sel.length-1, sel.selectedIndex+" + incr + ")";
            sb.append(
                "<td>" +
                "<img src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + imageName) + "\"\n" +
                " " + completeTooltip(tooltip) +
                " alt=\"" + 
                    (nRows == Integer.MIN_VALUE? "|&lt;" : nRows == Integer.MAX_VALUE? "&gt;|" : 
                     incr >=0? "+" + incr : "" + incr) + "\"\n" +
                //onMouseUp works much better than onClick and onDblClick
                " onMouseUp=\"var sel=" +formName + "." + name + "; sel.selectedIndex=" + rhs + ";" +   
                    buttonJS + " \" >\n" +
                "</td>\n");
        }
        return sb.toString();
    }

/*    private String selectButton(String name, int nRows, int test, 
        int nOptions, int minNOptions, int incr, String imageName, String tooltip) {

        StringBuilder sb = new StringBuilder();
        if ((-nRows & -test) != 0 && nOptions >= minNOptions) {
            String si = formName + "." + name + ".selectedIndex";
            String rhs = "0";
            if (incr == Integer.MIN_VALUE)      {}  //stay 0
            else if (incr < 0)                  rhs  = "Math.max(0," + si + "-" + -incr + ")";
            else if (incr == Integer.MAX_VALUE) rhs  = "" + (nOptions-1); 
            else if (incr > 0)                  rhs  = "Math.min(" + (nOptions-1) + "," + si + "+" + incr + ")";
            sb.append(
                "<td>" +
                "<img src=\"" + imageDirUrl + imageName + "\"\n" +
                "  " + completeTooltip(tooltip) +
                "  alt=\"" + 
                    (nRows == Integer.MIN_VALUE? "|&lt;" : nRows == Integer.MAX_VALUE? "&gt;|" : "" + nRows) + "\"" +
                    " onMouseUp=\"" + si + "=" + rhs + ";\" >\n" + //works much better than onClick and onDblClick
                "</td>\n");
        }
        return sb.toString();
    }
*/
    /**
     * This create the HTML code for a textField.
     * If user pressed Enter, enterTextSubmitsForm determines if the form is submitted.
     *
     * <p>You can make the edges disappear with 
     * <tt>style=\"border:0px; background:" + bgColor + "\" </tt>
     *
     * <p>You can make the text bold with <tt>style=\"font-weight:bold;"\" </tt>
     *
     * @param name the name of the widget (name=newValue is returned when the form is submitted)
     * @param tooltip If htmlTooltips is true, this is already html.
     *     If it is false, this is plain text.  Or "" if no tooltip.
     * @param fieldLength  the size of the field, in mspaces(?), e.g., 10.
     *     If the fieldLength <= 0, no 'size' value is specified in the html.
     * @param maxLength    usually 255
     * @param initialTextValue the initial text value, or null or "" if none
     * @param other e.g., "onchange=\"pleaseWait();\""
     *    For textField(), use onchange, not onclick.
     * @return the HTML code for a textField.
     */
    public String textField(String name, String tooltip, 
        int fieldLength, int maxLength, String initialTextValue, String other) {
        
        return 
            "    <input type=\"text\" name=\"" + XML.encodeAsHTMLAttribute(name) + "\"" +
            " alt=\"" + XML.encodeAsHTMLAttribute(name) + "\"" + //An accessibility aid. USGS requested to pass Acunetix scan.
            " value=\"" + 
            (initialTextValue == null? "" : XML.encodeAsHTMLAttribute(initialTextValue)) + 
            "\"" +
            //"\n      onkeypress=\"return !enter(event);\"" + 
            //supress Enter->submit
            //was the keypress event's keycode 'Enter'?
            //see http://www.mredkj.com/tutorials/validate.html
            (enterTextSubmitsForm? "" : 
                "\n      onkeypress='" +  //supress submission
                    " var key = window.event? event.keyCode : event.which? event.which : 0; " + //'?' deals with IE vs. Netscape vs. ? 
                    " return key != 13;' \n") +
            completeTooltip(tooltip) +
            completeStyle() +              
            "\n      " +
            (fieldLength > 0? "size=\"" + fieldLength + "\" " : "") + 
            "maxlength=\"" + maxLength + "\"" +
            " " + other + " >\n";
    }

    /**
     * This creates the html to draw an image (e.g., question mark) that has a big html tooltip.
     * htmlTooltipScript (see above) must be already in the document.
     * See tip().
     *
     * @param imageRef the reference for the question mark image 
     *   (e.g., EDStatic.imageDirUrl(loggedInAs) + EDStatic.questionMarkImageFile)
     * @param alt the alt text to be displayed, e.g., "?"  (not yet encoded)
     * @param html  the html tooltip text, e.g., "Hi,<br>there!".
     *     It needs explicit br tags to set window width correctly.
     *     For plain text, use XML.encodeAsPreHTML(plainText, 100).
     * @param other e.g., "onclick=\"pleaseWait();\""
     * @return the html to draw an image (e.g., question mark) that has a big html tooltip.
     */
    public static String htmlTooltipImage(String imageRef, String alt, String html, String other) {
         return "<img src=\"" + XML.encodeAsHTMLAttribute(imageRef) + 
             "\" alt=\"" + XML.encodeAsHTMLAttribute(alt) + "\"" + 
             htmlTooltip(html) + " " + other + ">\n";
    }


    /**
     * This creates the onmouseover and onmouseout html to add a big html tooltip to any widget.
     * htmlTooltipScript (see above) must be already in the document.
     *
     * @param html  the html tooltip text, e.g., "Hi,<br>there!".
     *     It needs explicit br tags to set window width correctly.
     *     For plain text, generate html from XML.encodeAsPreHTML(plainText, 100).
     * @return the onmouseover and onmouseout html to add a big html tooltip to any widget.
     */
    public static String htmlTooltip(String html) {
         if (html == null || html.length() == 0)
             return "";

         //example from http://www.walterzorn.com/tooltip/tooltip_e.htm:
         //<a href="index.htm" onmouseover="Tip('Text with <img src=\"pics/image.jpg\" width=\"60\">image.')" onmouseout="UnTip()"> Homepage</a>
         //tooltip = XML.encodeAsHTML(tooltip); //didn't work
         StringBuilder sb = new StringBuilder();
         int n = html.length();
         for (int i = 0; i < n; i++) {
             char ch = html.charAt(i);
             if      (ch == '\\') sb.append("\\\\");
             else if (ch == '\"') sb.append("&quot;");
             else if (ch == '\'') sb.append("&#39;");
             else if (ch == '\n') sb.append(' '); //causes problem: quotes not closed at end of line
             else sb.append(ch);
         }
         String2.replaceAll(sb, "&#39;", "\\&#39;");
         String2.replaceAll(sb, "  ", "&nbsp;&nbsp;");
         return " onmouseover=\"Tip('" + sb.toString() + "')\" onmouseout=\"UnTip()\" "; //with space at beginning and end
    }

    /** 
     * This returns the html for a white image which takes up the specified space.
     *
     * @param width in pixels
     * @param height in pixels
     * @param other other html parameters, e.g., "align=\"left\""
     */
    public String spacer(int width, int height, String other) {
        StringBuilder sb = new StringBuilder(
            "    <img src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + "spacer.gif") + "\" " +
                "width=\"" + width + "\" height=\"" + height + "\" " + other + 
                " alt=\"");
        int n = Math.max(1, width / 8);
        for (int i = 0; i < n; i++)
            sb.append("&nbsp;");
        sb.append("\">\n");
        return sb.toString();
    }

    /**
     * This returns the html for the images which make up a 1-thumb slider.
     * See also sliderScript.
     *
     * <p>The source images must be in imageDirUrl.
     * Their sizes must be as defined by sliderThumbWidth and sliderThumbHeight.
     *
     * @param sliderNumber e.g., 0.. n-1.
     *    In the HTML, the images will be named 
     *    sliderLeft[sliderNumber], sliderBg[sliderNumber].
     * @param bgWidth the width of the track, in pixels  (e.g., 500).
     *    All sliders on a form must use the same bgWidth.
     * @param other   e.g., align="left"
     * @return the html to describe a 1-thumb slider.
     */
    public String slider(int sliderNumber, int bgWidth, String other) {
        return //order is important
            "      <img name=\"sliderLeft" + sliderNumber + 
                   "\" src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderCenter.gif") + "\" \n" +
            "        width=\"" + sliderThumbWidth + "\" height=\"" + sliderThumbHeight + "\" " + other + 
                " alt=\"" + XML.encodeAsHTMLAttribute("<") + "\">\n" +
            "      <img name=\"sliderBg"   + sliderNumber + 
                   "\" src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderBg.gif") + "\" \n" +
            "        width=\"" + bgWidth          + "\" height=\"" + sliderThumbHeight + "\" " + other + 
                " alt=\"" + XML.encodeAsHTMLAttribute("<") + "\">\n";
    }

    /**
     * This returns the html for the images which make up a 2-thumb slider.
     * See also sliderScript.
     *
     * <p>The source images must be in imageDirUrl.
     *   sliderLeft.gif and sliderRight.gif must have width=15, height=17.
     *   sliderBg.gif must have height = 17.
     *
     * @param sliderNumber e.g., 0.. n-1.
     *    In the HTML, the images will be named 
     *    sliderLeft[sliderNumber], sliderBg[sliderNumber], sliderRight[sliderNumber].
     * @param bgWidth the width of the track, in pixels  (e.g., 500)
     * @param other   e.g., align="left"
     * @return the html to describe a 2-thumb slider.
     */
    public String dualSlider(int sliderNumber, int bgWidth, String other) {
        return //order is important
            "      <img name=\"sliderLeft"  + sliderNumber + 
                   "\" src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderLeft.gif") + "\" \n" +
            "        width=\"" + sliderThumbWidth + "\" height=\"" + sliderThumbHeight + "\" " + other + 
                " alt=\"" + XML.encodeAsHTMLAttribute("<") + "\">\n" +
            "      <img name=\"sliderBg"    + sliderNumber + 
                   "\" src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderBg.gif") + "\" \n" +
            "        width=\"" + bgWidth          + "\" height=\"" + sliderThumbHeight + "\" " + other + 
                " alt=\"" + XML.encodeAsHTMLAttribute("slider") + "\">\n" +
            "      <img name=\"sliderRight" + sliderNumber + 
                  "\" src=\"" + XML.encodeAsHTMLAttribute(imageDirUrl + "sliderRight.gif") + "\" \n" +
            "        width=\"" + sliderThumbWidth + "\" height=\"" + sliderThumbHeight + "\" " + other + 
                " alt=\"" + XML.encodeAsHTMLAttribute(">") + "\">\n";
    }


    /**
     * This returns the html for the javascript which controls a series of sliders.
     * <br>dragDropScript() must be right after the 'body' tag.
     * <br>Use slider() or dualSlider() repeatedly in the form.
     * <br>Put sliderScript() right before the '/body' tag.
     * <br>See documentation in http://www.walterzorn.com/dragdrop/commands_e.htm
     *   and http://www.walterzorn.com/dragdrop/api_e.htm .
     *
     * @param fromTextFields the names of all of the 'from' textFields, 
     *    preceded by their formName + "." (e.g., "f1.from0", "f1.from1").
     *    1-thumb sliders connect to this textField only.
     *    For no slider, there should still be a placeholder entry (e.g., "").
     * @param toTextFields the names of all of the 'to' textFields , 
     *    preceded by their formName + "." (e.g., "f1.to0", "f1.to1").
     *    For no slider or 1-thumb sliders, there should still be a placeholder entry (e.g., "").
     * @param nThumbs is the number of thumbs for each slider
     *    (0=inactive, 1, or 2)
     * @param userValuesCsvs a csv list of values for each slider.
     *    <br>If a given nThumbs == 0, its csv is irrelevant.
     *    <br>Each list can be any length (at least 1), but only up to bgWidth+1 will be used.
     * @param initFromPositions the initial values for the left (or only) sliders.
     *    <br>If a given slider is inactive, there should still be a placeholder value.
     *    <br>These are usually calculated as Math.roung(index * bgWidth / (nIndices-1.0))
     * @param initToPositions the initial values for the right sliders.
     *    <br>If a given slider is inactive or has 1-thumb, there should still be a placeholder value.
     *    <br>These are usually calculated as Math.roung(index * bgWidth / (nIndices-1.0))
     * @param bgWidth the width of the track, in pixels  (e.g., 500).
     *    All sliders on a form must use the same bgWidth.
     * @return the html for the javascript which controls a series of sliders.
     * @throws RuntimeException if trouble
     */
    public String sliderScript(String fromTextFields[], String toTextFields[],
        int nThumbs[], 
        String userValuesCsvs[], int initFromPositions[], int initToPositions[], int bgWidth) {

        int nSliders = nThumbs.length;
        if (nSliders == 0)
            return "";
        if (nSliders != fromTextFields.length)
            throw new RuntimeException(
                "nThumbs.length=" + nSliders + " != fromTextFields.length=" + fromTextFields.length);
        if (nSliders != toTextFields.length)
            throw new RuntimeException(
                "nThumbs.length=" + nSliders + " != toTextFields.length=" + toTextFields.length);
        if (nSliders != userValuesCsvs.length)
            throw new RuntimeException(
                "nThumbs.length=" + nSliders + " != userValuesCsvs.length=" + userValuesCsvs.length);
        if (nSliders != initFromPositions.length)
            throw new RuntimeException(
                "nThumbs.length=" + nSliders + " != initFromPositions.length=" + initFromPositions.length);
        if (nSliders != initToPositions.length)
            throw new RuntimeException(
                "nThumbs.length=" + nSliders + " != initToPositions.length=" + initToPositions.length);
        StringBuilder sb = new StringBuilder();
        sb.append(
"\n" +
"<!-- start of sliderScript -->\n" +
"<script type=\"text/javascript\"> \n" +
"<!--\n" +
"var fromTextFields = [");
for (int s = 0; s < nSliders; s++) 
    sb.append(String2.toJson(fromTextFields[s]) + (s < nSliders-1? ", " : "];\n"));
sb.append(
"var toTextFields = [");
for (int s = 0; s < nSliders; s++) 
    sb.append(String2.toJson(toTextFields[s]) + (s < nSliders-1? ", " : "];\n"));
sb.append(
"var userValues = [\n");
for (int s = 0; s < nSliders; s++) 
    sb.append("  [" + userValuesCsvs[s] + "]" + (s < nSliders-1? ",\n" : "];\n"));
sb.append(
"var initFromPositions = [\n");
for (int s = 0; s < nSliders; s++) 
    sb.append(initFromPositions[s] + (s < nSliders-1? ", " : "];\n"));
sb.append(
"var initToPositions = [\n");
for (int s = 0; s < nSliders; s++) 
    sb.append(initToPositions[s] + (s < nSliders-1? ", " : "];\n"));
sb.append(
"\n");

//SET_DHTML
sb.append(
"SET_DHTML(CURSOR_DEFAULT, NO_ALT, SCROLL, \n");
for (int s = 0; s < nSliders; s++) {
    if (nThumbs[s] > 0)  sb.append(
"  \"sliderBg"    + s + "\"+NO_DRAG,\n" +
"  \"sliderLeft"  + s + "\"+HORIZONTAL+MAXOFFLEFT+0,\n");
    if (nThumbs[s] == 2) sb.append(
"  \"sliderRight" + s + "\"+HORIZONTAL+MAXOFFRIGHT+" + bgWidth + ",\n");
}
sb.setLength(sb.length() - 2); //remove ,\n
sb.append(");\n" +
"\n" +
"var el = dd.elements;\n" +
"\n");

//log      
sb.append(
"function log(msg) {\n");
if (debugMode) sb.append(
"  if (typeof(console) != \"undefined\") console.log(msg);\n"); //for debugging only
sb.append(
"}\n" +
"\n");

//toUserValue
sb.append(
"function toUserValue(which, val) {\n" +
"  var nuv = userValues[which].length;\n" +
"  var index = Math.floor((val * nuv) / " + bgWidth + ");\n" +
"  return userValues[which][Math.min(Math.max(0, index), (nuv - 1))];\n" +
"};\n" +
"\n");

//updateUI
sb.append(
"function updateUI(left, which) {\n" +
"  log(\"left=\" + left + \" which=\" + which); \n" +
//"  //get the widgets\n" +
"  var leftS  = eval(\"el.sliderLeft\" + which);  \n" +
"  var rightS = eval(\"el.sliderRight\" + which);\n" +
"  if (left) {\n" +
"    var val = leftS.x - leftS.defx;\n" +
//"    log(\"x=\" + leftS.x + \" val=\" + val);  //for development only\n" +
"    var fromW  = eval(\"document.\" + fromTextFields[which]);\n" +
"    fromW.value = toUserValue(which, val);\n" +
"    if (typeof(rightS) != \"undefined\") rightS.maxoffl = -val;\n" + //positive is to the left!
"  } else {\n" +
"    var val = rightS.x - rightS.defx;\n" +
//"    log(\"x=\" + rightS.x + \" val=\" + val); \n" +
"    var toW    = eval(\"document.\" + toTextFields[which]);\n" +
"    toW.value = toUserValue(which, val);\n" +
"    if (typeof(leftS) != \"undefined\") leftS.maxoffr = val;\n" +
"  }\n" +
"};\n" +
"\n");

//my_DragFunc
sb.append(
"function my_DragFunc() {\n" +
"  try {\n");
for (int s = 0; s < nSliders; s++) {
    if (nThumbs[s] > 0)  sb.append(
"    if (dd.obj.name == 'sliderLeft"  + s + "') updateUI(true, " + s + ");\n");
    if (nThumbs[s] > 1)  sb.append(
"    if (dd.obj.name == 'sliderRight" + s + "') updateUI(false, " + s + ");\n");
}
sb.append(
"  } catch (ex) {\n" +
"    " + (debugMode? "alert" : "log") + "(ex.toString());\n" +
"  }\n" +
"}\n" +
"\n");

//initSlider    //do last
sb.append(
"function initSlider(which) {\n" +
"  var bgS    = eval(\"el.sliderBg\" + which);  \n" +
"  var leftS  = eval(\"el.sliderLeft\" + which);  \n" +
"  var rightS = eval(\"el.sliderRight\" + which);\n" +
" \n" +
"  var oneThumb = typeof(rightS) == \"undefined\";\n" +
"  leftS.setZ(bgS.z+1); \n" +
"  bgS.addChild(\"sliderLeft\" + which); \n" +
"  leftS.defx = bgS.x - Math.round(" + sliderThumbWidth + " / (oneThumb? 2 : 1));\n" +
"  leftS.moveTo(leftS.defx + initFromPositions[which], bgS.y); \n" +
"  leftS.maxoffr = initToPositions[which];\n" +
"\n" +
"  if (!oneThumb) {\n" +
"    rightS.setZ(bgS.z+1); \n" +
"    bgS.addChild(\"sliderRight\" + which); \n" +
"    rightS.defx = bgS.x;\n" +
"    rightS.moveTo(rightS.defx + initToPositions[which], bgS.y); \n" +
"    rightS.maxoffl = -initFromPositions[which];\n" +  //positive is to the left!
"  }\n" +
"}\n" +
"\n" +
"try {\n");
for (int s = 0; s < nSliders; s++) {
    if (nThumbs[s] > 0)  sb.append(
"  initSlider(" + s + ");\n");
}
sb.append(
"} catch (ex) {\n" +
"  " + (debugMode? "alert" : "log") + "(ex.toString());\n" +
"}\n" +

"\n" +
"//-->\n" +
"</script>\n" +
"<!-- end of sliderScript -->\n" +
"\n");
        return sb.toString();
    }

    /** A common use of twoClickMap to show a lon=-180 to 540 map. */
    public static String[] myTwoClickMap540Big(String formName, String imageUrl, 
        String debugInBrowser) {
        return twoClickMap(
            formName, "minLon", "maxLon", "minLat", "maxLat",
            imageUrl, 412, 155,  //image w, h
            17, 12, 380, 127,  //map left, top, graph width, height  (via subtraction)
            540, -180, 180, 90,
            null, debugInBrowser);
    }

    /** A common use of twoClickMap to show a lon=-180 to 180 map. */
    public static String[] myTwoClickMap180Big(String formName, String imageUrl, 
        String debugInBrowser) {
        return twoClickMap(
            formName, "minLon", "maxLon", "minLat", "maxLat",
            imageUrl, 285, 155,
            17, 12, 253, 127,
            360, -180, 180, 90,
            null, debugInBrowser);
    }

    /** A common use of twoClickMap to show a lon=0 to 360 map. */
    public static String[] myTwoClickMap360Big(String formName, String imageUrl, 
        String debugInBrowser) {
        return twoClickMap(
            formName, "minLon", "maxLon", "minLat", "maxLat",
            imageUrl, 284, 155,
            16, 12, 253, 127,
            360, 0, 180, 90,
            null, debugInBrowser);
    }

    /** A common use of twoClickMap to show a lon=-180 to 540 map. */
    public static String[] myTwoClickMap540(String formName, String imageUrl, 
        String debugInBrowser) {
        return twoClickMap(
            formName, "minLon", "maxLon", "minLat", "maxLat",
            imageUrl, 293, 113,
            18, 10, 260, 86,
            540, -180, 180, 90,
            null, debugInBrowser);
    }

    /** A common use of twoClickMap to show a lon=-180 to 180 map. */
    public static String[] myTwoClickMap180(String formName, String imageUrl, 
        String debugInBrowser) {
        return twoClickMap(
            formName, "minLon", "maxLon", "minLat", "maxLat",
            imageUrl, 205, 113,
            18, 10, 173, 86,
            360, -180, 180, 90,
            null, debugInBrowser);
    }

    /** A common use of twoClickMap to show a lon=0 to 360 map. */
    public static String[] myTwoClickMap360(String formName, String imageUrl, 
        String debugInBrowser) {
        return twoClickMap(
            formName, "minLon", "maxLon", "minLat", "maxLat",
            imageUrl, 205, 113,
            18, 10, 173, 86,
            360, 0, 180, 90,
            null, debugInBrowser);
    }

    /** 
     * This generates the HTML and JavaScript to show a (world) map on image 
     * which the user can click on twice to specify a rectangle,
     * the coordinates of which will be put in 4 textfields (rounded to nearest int).
     *
     * <p>There can be only one twoClickMap per web page.
     *
     * @param formName e.g., f1
     * @param minLonTF the minLon textFieldName, e.g., minLon
     * @param maxLonTF the maxLon textFieldName
     * @param minLatTF the minLat textFieldName
     * @param maxLatTF the maxLat textFieldName
     * @param imageUrl
     * @param imageWidth
     * @param imageHeight
     * @param mapX theLeftPixelNumber of the map in the image
     * @param mapY theTopPixelNumber  of the map in the image
     * @param mapWidth  theRightPixel# - theLeftPixelNumber
     * @param mapHeight theBottomPixel# - theTopPixelNumber
     * @param lonRange e.g., 360 or 540
     * @param lonMin   e.g., -180 or 0
     * @param latRange usually 180
     * @param latMax   usually 90
     * @param tooltip use null for the default tooltip
     * @param debugInBrowser  if this is FF, IE, or Opera, debug code will be generated
     *    for that browser.  (use null or "" for no debugging)
     * @return [0] is the string to be put in the form (rowSpan=5)
     *    [1] is the string with the map (usually right after [0])
     *    and [2] is the string to be put after the end of the form.
     */
    public static String[] twoClickMap(
         String formName, String minLonTF, String maxLonTF, String minLatTF, String maxLatTF,
         String imageUrl, int imageWidth, int imageHeight,
         int mapX, int mapY, int mapWidth, int mapHeight,
         int lonRange, int lonMin, int latRange, int latMax,
         String tooltip, String debugInBrowser) {

if (debugInBrowser != null &&
    !debugInBrowser.equals("FF") &&
    !debugInBrowser.equals("IE") &&
    !debugInBrowser.equals("Opera"))
    debugInBrowser = null;

if (tooltip == null) 
    tooltip = twoClickMapDefaultTooltip;

StringBuilder sb0 = new StringBuilder();
sb0.append(
"\n" +
"<!-- start of twoClickMap[0] -->\n" +
"<img \n" +
"   name=\"worldImage\" id=\"worldImage\" src=\"" + XML.encodeAsHTMLAttribute(imageUrl) + 
"\" border=\"0\" usemap=\"#worldImageMap\"\n" +
"    alt=\"worldImage\"\n" +
"  title=\"" + tooltip + "\" \n" +
"  style=\"cursor:crosshair;\" align=\"middle\">\n" +
"<!-- end of twoClickMap[0] -->\n");

StringBuilder sb1 = new StringBuilder();
sb1.append(
"<!-- start of twoClickMap[1] -->\n" +
"<map name=\"worldImageMap\">\n" +  
"  <area nohref=\"nohref\" shape=RECT coords=\"0,0," + (imageWidth-1) + "," + (imageHeight-1) + 
    "\" style=\"cursor:crosshair;\"\n" +
"    alt=\"" + tooltip + "\"\n" +
"  title=\"" + tooltip + "\" \n" +
"        onClick=\"return rubber(true, event)\" \n" +
"    onMouseMove=\"return rubber(false, event)\" > \n" +
"</map>\n" +
"<!-- end of twoClickMap[1] -->\n");

StringBuilder sb2 = new StringBuilder();
sb2.append(
"\n" +
"<!-- start of twoClickMap[2] -->\n" +
"<div id=\"rubberBand\" style=\"position:absolute; visibility:hidden; width:0px; height:0px; " +
    "font-size:1px; line-height:0; border:1px solid red; cursor:crosshair;\" \n" +
"      onClick=\"return rubber(true, event)\" \n" +
"  onMouseMove=\"return rubber(false, event)\" ></div>\n" +
"\n" +
"<script type=\"text/javascript\">\n" +
"<!--\n" +
"var tcNextI = 0;\n" +
"var tcCx = new Array(0,0);\n" +
"var tcCy = new Array(0,0);\n" +
"\n");

if (debugInBrowser != null) 
    sb2.append(
"function log(msg) {\n" +
(debugInBrowser.equals("FF")?    "  console.debug(msg);\n"   : "") +
(debugInBrowser.equals("IE")?    "  alert(msg);\n"           : "") +
(debugInBrowser.equals("Opera")? "  opera.postError(msg);\n" : "") +
"}\n" +
"\n");

sb2.append(
"//findPos from http://blog.firetree.net/2005/07/04/javascript-find-position/\n" +
"function findPosX(obj) {\n" +
"  var curleft = 0;\n" +
"  if(obj.offsetParent)\n" +
"    for (var i = 0; i < 20; i++) {\n" +
"      curleft += obj.offsetLeft;\n" +
"      if(!obj.offsetParent)\n" +
"        break;\n" +
"      obj = obj.offsetParent;\n" +
"    }\n" +
"  else if(obj.x)\n" +
"    curleft += obj.x;\n" +
"  return curleft;\n" +
"}\n" +
"\n" +
"function findPosY(obj) {\n" +
"  var curtop = 0;\n" +
"  if(obj.offsetParent)\n" +
"    for (var i = 0; i < 20; i++) {\n" +
"      curtop += obj.offsetTop;\n" +
"      if(!obj.offsetParent)\n" +
"        break;\n" +
"      obj = obj.offsetParent;\n" +
"    }\n" +
"  else if(obj.y)\n" +
"    curtop += obj.y;\n" +
"  return curtop;\n" +
"}\n" +
"\n" +
"function rubber(clicked, evt) {\n" +
(debugInBrowser == null? "" : 
"  log(\"click=\" + clicked);\n") +
"  if (!clicked && tcNextI == 0)\n" +
"    return true;\n" +
"  var cx = tcCx;\n" +  //local cx should now point to global tcCx array
"  var cy = tcCy;\n" +
"  var IE = document.all? true: false;\n" +
"  var im = IE? document.all.worldImage : document.getElementById('worldImage');\n" +
"  var tx=-1, ty=-1;\n" +
"  if (IE) {\n" +
"    tx = document.body.scrollLeft + evt.x;\n" +
"    ty = document.body.scrollTop  + evt.y;\n" +
"  } else if (evt.pageX) {\n" +
"    tx = evt.pageX;\n" +
"    ty = evt.pageY;\n" +
"  } else return true;\n" +
"  var imx = findPosX(im);\n" +
"  var imy = findPosY(im);\n" +
"  tx = Math.max(tx, imx + " + mapX + ");\n" +
"  tx = Math.min(tx, imx + " + mapX + " + " + (mapWidth - 1) + ");\n" +  
"  ty = Math.max(ty, imy + " + mapY + ");\n" +
"  ty = Math.min(ty, imy + " + mapY + " + " + (mapHeight - 1) + ")\n" +
"  cx[tcNextI] = tx;\n" +
"  cy[tcNextI] = ty;\n" +
(debugInBrowser == null? "" : 
"  log(\"  \" + (IE?\"\":\"non-\") + \"IE: tcNextI=\" + tcNextI + \" cx=\" + tx + \" cy=\" + ty);\n") +
"  if (clicked) {\n" +
"    tcNextI = tcNextI == 0? 1 : 0;\n" +
"    if (tcNextI == 1) {\n" +
"      cx[1] = cx[0];\n" +
"      cy[1] = cy[0];\n" +
"    }\n" +
"  }\n" +
"\n" +
"  updateRubber();\n" +    
"  document." + formName + "." + minLonTF + ".value = Math.round(((Math.min(cx[0], cx[1]) - " + mapX + " - imx) / " + (mapWidth - 1)  + ") * " + lonRange  + " + " + lonMin + ");\n" + 
"  document." + formName + "." + maxLonTF + ".value = Math.round(((Math.max(cx[0], cx[1]) - " + mapX + " - imx) / " + (mapWidth - 1)  + ") * " + lonRange  + " + " + lonMin + ");\n" +
"  document." + formName + "." + minLatTF + ".value = Math.round(((Math.max(cy[0], cy[1]) - " + mapY + " - imy) / " + (mapHeight - 1) + ") * " + -latRange + " + " + latMax + ");\n" +
"  document." + formName + "." + maxLatTF + ".value = Math.round(((Math.min(cy[0], cy[1]) - " + mapY + " - imy) / " + (mapHeight - 1) + ") * " + -latRange + " + " + latMax + ");\n" +
(debugInBrowser == null? "" : 
"  log(\"  done\");\n") +
"  return true;\n" +
"}\n" +
"\n" +
"function updateRubber() {\n" +
"  var cx = tcCx;\n" + //local cx should now point to global tcCx array
"  var cy = tcCy;\n" +
"  var rb = (document.all)? document.all.rubberBand : document.getElementById('rubberBand');\n" +
"  var rbs = rb.style;\n" +
"  rbs.left = Math.min(cx[0], cx[1]) + 'px';\n" +
"  rbs.top  = Math.min(cy[0], cy[1]) + 'px';\n" +
"  rbs.width  = Math.abs(cx[1] - cx[0]) + 'px';\n" +
"  rbs.height = Math.abs(cy[1] - cy[0]) + 'px';\n" +
"  rbs.visibility = 'visible';\n" +
"}\n" +
"\n" +
"function initRubber() {\n" +
"  var initLon0 = parseFloat(document." + formName + "." + minLonTF + ".value);\n" +
"  var initLon1 = parseFloat(document." + formName + "." + maxLonTF + ".value);\n" +
"  var initLat0 = parseFloat(document." + formName + "." + minLatTF + ".value);\n" +
"  var initLat1 = parseFloat(document." + formName + "." + maxLatTF + ".value);\n" +
"  if (isFinite(initLon0) && isFinite(initLon1) &&\n" +
"      isFinite(initLat0) && isFinite(initLat1)) {\n" +
(debugInBrowser == null? "" : 
"    log(\"initRubber\");\n") +
"    var im = document.all? document.all.worldImage : document.getElementById('worldImage');\n" +
"    var imx = findPosX(im);\n" +
"    var imy = findPosY(im);\n" +
"    var lon0 = Math.min(1, Math.max(0, (initLon0 - " + lonMin + ") / " + lonRange  + "));\n" +
"    var lon1 = Math.min(1, Math.max(0, (initLon1 - " + lonMin + ") / " + lonRange  + "));\n" +
"    var lat0 = Math.min(1, Math.max(0, (initLat0 - " + latMax + ") / " + -latRange + "));\n" +
"    var lat1 = Math.min(1, Math.max(0, (initLat1 - " + latMax + ") / " + -latRange + "));\n" +
"    tcCx[0] = Math.round(lon0 * " + (mapWidth  - 1) + " + " + mapX + " + imx);\n" +
"    tcCx[1] = Math.round(lon1 * " + (mapWidth  - 1) + " + " + mapX + " + imx);\n" +
"    tcCy[0] = Math.round(lat0 * " + (mapHeight - 1) + " + " + mapY + " + imy);\n" +
"    tcCy[1] = Math.round(lat1 * " + (mapHeight - 1) + " + " + mapY + " + imy);\n" +
"    updateRubber();\n" +
(debugInBrowser == null? "" : 
"    log(\"  init cx=\" + tcCx[0] + \", \" + tcCx[1] + \" cy=\" + tcCy[0] + \", \" + tcCy[1]);\n") +
"  }\n" +
"}\n" +
"\n" +
"try {\n" +
"  initRubber(); \n" +
"} catch (ex) {\n" + 
(debugInBrowser == null? "" : 
"  log('initRubber exception: ' + ex.toString());\n") +
"}\n" +
"--> \n" +
"</script>\n" +
"<!-- end of twoClickMap[2] -->\n" +
"\n");

return new String[]{sb0.toString(), sb1.toString(), sb2.toString()};
}

    /* *  NOT FINISHED
     * This looks for cross site scripting attempts 
     * (e.g., &lt;script&gt; &lt;object&gt; &lt;applet&gt; &lt;embed&gt; &lt;form&gt; 
     * &lt;img&gt; &lt;iframe&gt; &lt;/a&gt; &lt;&gt; 
     * in textField input or RESTful parameter value from a user.
     * See http://www.technicalinfo.net/papers/CSS.html
     *
     * @param text the text received from a user (usually via a textField or REST parameter).
     * @return either the original text (if no sign of trouble) or "[]" if there was any sign or trouble. 
     *   null returns null.
     * /
    public static String cleanInput(String text) {
        if (text == null)
            return text;
        String s = text.toLowerCase();
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength);
        for (int i = 0; i< sLength; i++) {
            char c = s.charAt(i);
            if (String2.isWhite(c))
                continue;
*/

    /**
     * This ensure that the string value isn't null and isn't too long. 
     * 
     * @param cumulativeHtmlErrorMsg (may be null)
     * @return the corrected value (default (if it was null), previous value.trim(), or 
     *    previous value shortened to maxLength)
     */
    public static String validateNotNullNotTooLong(String name, String defaultValue, 
        String value, int maxLength, StringBuilder cumulativeHtmlErrorMsg) {
       
        if (value == null) {
            if (cumulativeHtmlErrorMsg != null)
                cumulativeHtmlErrorMsg.append("<br>&bull; " + 
                    MessageFormat.format(errorXWasntSpecified, XML.encodeAsHTML(name)) + 
                    "\n");
            return defaultValue;
        } else {
            value = value.trim();
        }
        if (value.length() > maxLength) {
            if (cumulativeHtmlErrorMsg != null)
                cumulativeHtmlErrorMsg.append("<br>&bull; " + 
                    MessageFormat.format(errorXWasTooLong, XML.encodeAsHTML(name), "" + maxLength) + 
                    "\n");
            return value.substring(0, maxLength);
        }
        return value;
    }

    /**
     * This ensure that the string value isSomething and isn't too long. 
     * 
     * @param cumulativeHtmlErrorMsg (may be null)
     * @return the corrected value (default (if value was null or ""), previous value.trim(), or 
     *    previous value shortened to maxLength)
     */
    public static String validateIsSomethingNotTooLong(String name, String defaultValue, 
        String value, int maxLength, StringBuilder cumulativeHtmlErrorMsg) {
       
        if (!String2.isSomething(value)) {
            if (cumulativeHtmlErrorMsg != null)
                cumulativeHtmlErrorMsg.append("<br>&bull; " + 
                    MessageFormat.format(errorXWasntSpecified, XML.encodeAsHTML(name)) + 
                    "\n");
            return defaultValue;
        } else {
            value = value.trim();
        }
        if (value.length() > maxLength) {
            if (cumulativeHtmlErrorMsg != null)
                cumulativeHtmlErrorMsg.append("<br>&bull; " + 
                    MessageFormat.format(errorXWasTooLong, XML.encodeAsHTML(name), "" + maxLength) + 
                    "\n");
            return value.substring(0, maxLength);
        }
        return value;
    }

    /**
     * This ensure that the string value isn't too long. 
     * 
     * @param cumulativeHtmlErrorMsg (may be null)
     * @return the corrected value (default (if value was null), previous value.trim(), or 
     *    previous value shortened to maxLength)
     */
    public static String validateNotTooLong(String name, String defaultValue, 
        String value, int maxLength, StringBuilder cumulativeHtmlErrorMsg) {
       
        if (value == null)
            value = defaultValue;
        else
            value = value.trim();
        if (value.length() > maxLength) {
            if (cumulativeHtmlErrorMsg != null)
                cumulativeHtmlErrorMsg.append("<br>&bull; " + 
                    MessageFormat.format(errorXWasTooLong, XML.encodeAsHTML(name), "" + maxLength) + 
                    "\n");
            return value.substring(0, maxLength);
        }
        return value;
    }

    /**
     * This ensure that value is &gt;=0 and &lt;=maxValue. 
     * 
     * @param cumulativeHtmlErrorMsg (may be null)
     * @return the corrected value (default (if it was null), previous value, or 
     *    previous value shortened to maxLength)
     */
    public static int validate0ToMax(String name, int defaultValue, 
        int value, int max, StringBuilder cumulativeHtmlErrorMsg) {
       
        if (value < 0 || value > max) {
            if (cumulativeHtmlErrorMsg != null)
                cumulativeHtmlErrorMsg.append("<br>&bull; " + 
                    MessageFormat.format(errorXWasntSpecified, XML.encodeAsHTML(name)) + 
                    "\n");
            return defaultValue;
        }
        return value;
    }

    /**
     * This makes a test document and displays it in the browser.
     */
    public static void test() throws Throwable {
        boolean oDebugMode = debugMode;
        debugMode = true;
        String fullName = SSR.getTempDirectory() + "TestHtmlWidgets.html";
        String imageDir = "file://c:/programs/tomcat/webapps/cwexperimental/images/";
        File2.delete(fullName);
        Writer writer = new FileWriter(fullName);
        boolean tHtmlTooltips = true;
        HtmlWidgets widgets = new HtmlWidgets("", tHtmlTooltips, 
            imageDir);
//            "http://127.0.0.1:8080/cwexperimental/images/");
//            "http://coastwatch.pfeg.noaa.gov/erddap/images/"); //SANS_SERIF_STYLE);
        writer.write(
            DOCTYPE_HTML_TRANSITIONAL +
            "  <title>Test Html Widgets</title>\n" +
            //THIS MIMICS erddap.css  (so duplicate any changes here/there)
            "<style type=\"text/css\">\n" +
            "  input[type=button], button {margin:0px; padding:1px 3px; }\n" + //TB LR
            "  input.skinny {padding:0px 1px; }\n" +
            "  input[type=checkbox], input[type=password], input[type=text], select, textarea {\n" +
            "    margin:0px; padding:0px; }\n" +
            "  input[type=radio] {margin:0px 2px; padding:0px; }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body" + 
            " style=\"" + XML.encodeAsHTMLAttribute(SANS_SERIF_STYLE) + "\"" +
            ">\n" +
            htmlTooltipScript(imageDir) + //this test uses non-https url
            dragDropScript(imageDir) +    //this test uses non-https url
            "<h1>Test Html Widgets</h1>\n" +
            ifJavaScriptDisabled);
        writer.write("This is some text in the SANS_SERIF_STYLE.\n");
        writer.write(htmlTooltipImage(imageDir + "QuestionMark.jpg", "?",
            "Hi <b>bold</b>\n<br>there \\/'\"&amp;&brvbar;!" +
            "<br><img src=\"http://www.cohort.com/wings.gif\" alt=\"wings\">", ""));
        String formName = "f1";
        writer.write(widgets.beginForm(formName, "GET", "testUrl", ""));
        writer.write(
            widgets.checkbox("checkboxName1", "checkboxTooltip literal: &lt;&gt;&amp;\"!", true, "checkboxValue1", "rightLabel1", "") +
            "\n" +
            widgets.textField("textFieldName1", "textFieldTooltip literal: &lt;&gt;&amp;\"!", 10, 255, "initialValue", "") +
            "<br>\n" +
            widgets.checkbox("checkboxName2", "checkboxTooltip literal: &lt;&gt;&amp;\"!", true, "checkboxValue2", "rightLabel2", "") +
            "\n" +
            widgets.textField("textFieldName2", "textFieldTooltip literal: &lt;&gt;&amp;\"!", 10, 255, "initialValue", "") +
            "<br>\n" +
            widgets.checkbox("checkboxName3", "checkboxTooltip literal: &lt;&gt;&amp;\"!", true, "checkboxValue3", "rightLabel3", "") +
            "<br>\n" +
            widgets.checkbox("checkboxName4", "checkboxTooltip literal: &lt;&gt;&amp;\"!", true, "checkboxValue4", "rightLabel4", "") +
            widgets.comment("This is a comment."));
        String options[] = new String[1200];
        for (int i = 0; i < 1200; i++)
            options[i] = i==1? "1 (2 is empty)" : i==2? "" : "option" + i;
        writer.write("<br>\n" +
            widgets.radioButtons("radioName1", "radioTooltip literal: &lt;&gt;&amp;\"!", true, new String[]{"apple", "banana", "cucumber"}, 1, "") +
            "<br>\n" +
            widgets.radioButtons("radioName2", "radioTooltip literal: &lt;&gt;&amp;\"!", true, new String[]{"apple", "banana", "cucumber"}, 2, "") +
            widgets.select("dropdownName", "dropdownTooltip literal: &lt;&gt;&amp;\"!", BUTTONS_0n + BUTTONS_1 + BUTTONS_100 + BUTTONS_1000, options, 37, "") +
            widgets.select("selectName", "selectTooltip literal: &lt;&gt;&amp;\"!", 6, options, 137, "") +
            "<br>Password: \n" +
            "<input type=\"password\" name=\"mypassword\">\n" +
            "<br>Testfield1: \n" + 
            widgets.textField("textFieldName1", "textFieldTooltip literal: &lt;&gt;&amp;\"!", 10, 255, 
                "initialValue<script>prompt(123)</script>", "") +
            "<br>Textfield2: \n" +
            widgets.textField("textFieldName2", "", 20, 255, "has big tooltip <script>prompt(123)</script>", 
                htmlTooltip("Hi <b>bold</b>!\n<br>There \\/'\"&amp;&brvbar;!")) +
            "<br>\n" +
            "<textarea name=\"address\" cols=\"40\" rows=\"4\" maxlength=\"160\" wrap=\"soft\">" +
            "John Smith\n123 Main St\nAnytown, CA 94025\n" +
            XML.encodeAsHTMLAttribute("<>&\"<script>prompt(123)</script>\n") +
            "</textarea>\n" +
            "<br>\n" +
            widgets.hidden("testHiddenName", "hiddenValue") +
            widgets.button("button", "button1", "button tooltip  literal: &lt;&gt;&amp;\"!", "Skinny Button", "class=\"skinny\"") +
            //make a button that looks like a submit button but doesn't submit the form
            "<br>\n" +
            widgets.button("button", "submit1Name", "submit tooltip  literal: &lt;&gt;&amp;\"!", "Submit", "onclick=\"alert('alert!');\"") +
            "<br>\n" +
            widgets.htmlButton("button", "htmlbutton1", "value1", "tooltip", "labelHtml", "") +
            "<br>\n" +
            widgets.htmlButton("button", "htmlbutton2", "value2", "tooltip", "labelHtml", "") +
            "<br>\n" +
            widgets.color17("Color1: ", "color1", "Pick a color.", 2, "") +
            widgets.color17("Color2: ", "color2", "Pick a color.", 6, "") +
            "some other text\n");
         
        //sliders
        String fromNames[] = {"f1.from0", "f1.from1", "", "f1.from3", "f1.from4", "f1.from5"};
        String toNames[] = {"f1.to0", "", "", "f1.to3", "f1.to4", "f1.to5"};
        int nThumbs[] = {2, 1, 0, 2, 2, 1};
        String userValuesCsvs[] = {
            "\"a\",\"b\",\"c\",\"d\"",
            "\"B\",\"o\",\"b\"",
            null,
            "1,2,4,8,16,32,64,128",
            "\"only\"",
            "\"only\""};
        int initFromPositions[] = {200, 100, 0,   0,   0, 100};
        int initToPositions[]   = {300, 500, 0, 500, 500, 500};
        int bgWidth = 500;
//see sliderScript below

        writer.write(
"<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
"\n" +
"<tr><td>Raw values: \n" +
"Min: " + widgets.textField("from0", "", 10, 10, "sliderFrom0", "") + "\n" +
"Max: " + widgets.textField("to0",   "", 10, 10, "sliderTo0",   "") + "\n" +
"</td></tr>\n" +
"<tr><td>\n" +
widgets.dualSlider(0, bgWidth, "align=\"left\"") +
"</td></tr>\n" +
"\n" +

"<tr><td>Raw values: \n" +
"Min: " + widgets.textField("from1", "", 10, 10, "sliderFrom1", "") + "\n" +
"</td></tr>\n" +
"\n" +

"<tr><td>\n" +
widgets.slider(1, bgWidth, "align=\"left\"") +
"</td></tr>\n" +
"\n" +

"<tr><td>Raw values: \n" +
"Min: " + widgets.textField("from3", "", 10, 10, "sliderFrom3", "") + "\n" +
"Max: " + widgets.textField("to3",   "", 10, 10, "sliderTo3", "") + "\n" +
"</td></tr>\n" +
"<tr><td>\n" +
widgets.dualSlider(3, bgWidth, "align=\"left\"") +
"</td></tr>\n" +

"<tr><td>Raw values: \n" +
"Min: " + widgets.textField("from4", "", 10, 10, "sliderFrom4", "") + "\n" +
"Max: " + widgets.textField("to4",   "", 10, 10, "sliderTo4", "") + "\n" +
"</td></tr>\n" +
"<tr><td>\n" +
widgets.dualSlider(4, bgWidth, "align=\"left\"") +
"</td></tr>\n" +

"<tr><td>Raw values: \n" +
"Min: " + widgets.textField("from5", "", 10, 10, "sliderFrom5", "") + "\n" +
"</td></tr>\n" +
"<tr><td>\n" +
widgets.slider(5, bgWidth, "align=\"left\"") +
"</td></tr>\n" +

"\n" +
"<tr><td>\n" +
"Something Else\n" +
"</td></tr>\n" +
"<tr><td>\n" +
"  <img src=\"" + imageDir + "arrow2U.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrow2D.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrow2L.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrow2R.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowU.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowD.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowL.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowR.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowUU.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowDD.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowLL.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "arrowRR.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "minus.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "minus10.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "minus100.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "minus1000.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "minusminus.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "plus.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "plus10.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "plus100.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "plus1000.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "plusplus.gif\" alt=\"anImage\">\n" +
"<br>\n" +
"  <img src=\"" + imageDir + "data.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "i.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "resize.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "source.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "title.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "URL.gif\" alt=\"anImage\">\n" +
"  <img src=\"" + imageDir + "x.gif\" alt=\"anImage\">\n" + 
"</td></tr>\n" +
"</table>\n");

//twoClickMap
//String twoClickMap[] = myTwoClickMap540(formName, imageDir + "world540.png", null); // "FF"); //debugInBrowser   e.g., null, FF, IE, Opera
//String twoClickMap[] = myTwoClickMap180Big(formName, imageDir + "worldPM180Big.png", null); // "FF"); //debugInBrowser   e.g., null, FF, IE, Opera
//String twoClickMap[] = myTwoClickMap360Big(formName, imageDir + "world0360Big.png", null); // "FF"); //debugInBrowser   e.g., null, FF, IE, Opera
String twoClickMap[] = myTwoClickMap540Big(formName, imageDir + "world540Big.png", null); // "FF"); //debugInBrowser   e.g., null, FF, IE, Opera

writer.write(
"<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
"<tr>\n" +
"  <td>MinLon: " + widgets.textField("minLon", "", 5, 5, "220", "") + "</td>\n" +
"  <td rowSpan=7 nowrap>" + twoClickMap[0] + "text on same row!" + twoClickMap[1] +
   "</td>\n" +
"</tr>\n" +
"<tr><td>MaxLon: " + widgets.textField("maxLon", "", 5, 5, "250", "") + "</td></tr>\n" +
"<tr><td>MinLat: " + widgets.textField("minLat", "", 5, 5, "20", "") + "</td></tr>\n" +
"<tr><td>MaxLat: " + widgets.textField("maxLat", "", 5, 5, "50", "") + "</td></tr>\n" +
"<tr><td>Row 5</td></tr>\n" +
"<tr><td>Row 6</td></tr>\n" +
"<tr><td>Row 7</td></tr>\n" +
"</table>\n");
        writer.write(widgets.endForm());        
writer.write(twoClickMap[2]);
        //writer.write("<script src=\"http://gmodules.com/ig/ifr?url=http://hosting.gmodules.com/ig/gadgets/file/104311141343481305007/erddap_windvectors_qsSQ_8day.xml&amp;synd=open&amp;w=280&amp;h=345&amp;title=ERDDAP%3A+Wind+Vectors%2C+QuikSCAT%2C+Science+Quality+(8+Day+Composite)&amp;border=http%3A%2F%2Fgmodules.com%2Fig%2Fimages%2F&amp;output=js\"></script>\n");
        //writer.write("<script src=\"http://gmodules.com/ig/ifr?url=http://hosting.gmodules.com/ig/gadgets/file/104311141343481305007/erddap_sst_goes_8day.xml&amp;synd=open&amp;output=js\"></script>\n");
        //writer.write("<img src=\"http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdGAssta8day.png?sst[(last)][(0.0)][(22):(50)][(225):(255)]%26.draw=surface%26.vars=longitude|latitude|sst%26.colorBar=Rainbow|C|Linear|8|32|\" alt=\"ERDDAP: SST, GOES Imager (8 Day Composite)\">\n");

        writer.write(widgets.sliderScript(fromNames, toNames, nThumbs, userValuesCsvs, 
            initFromPositions, initToPositions, bgWidth));

        writer.write(
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "<p>&nbsp;\n" +
            "</body>\n" +
            "</html>\n");
        writer.close();


        SSR.displayInBrowser("file://" + fullName);
        debugMode = oDebugMode;
    }


}



