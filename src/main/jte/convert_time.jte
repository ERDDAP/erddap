@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.String2
@import com.cohort.util.XML
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/time.html"
@param HttpServletRequest request = null

@param String stringTime1 = ""
@param String units1 = ""
@param String stringTimeTooltip = ""
@param String unitsTooltip = ""
@param String numberTooltip = ""
@param boolean toNumeric = false
@param String toNumericResult = ""

@param String n2 = ""
@param String units2 = ""
@param boolean toString = false
@param String toStringResult = ""

@param String stringTime3 = ""
@param boolean cleanString = false
@param String cleanStringResult = ""

@param String units4 = ""
@param boolean cleanUnits = false
@param String cleanUnitsResult = ""

@param String tError = null

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <h2>${TemplateHelper.getMessage(Message.CONVERT_TIME, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_TIME_INTRO, language)}

    <form method="GET" action="${tErddapUrl}/convert/time.html" name="toNumeric">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_NUMERIC_TIME, language),
            "</strong>\n<input type=\"text\" name=\"stringTime\" value=\"" + XML.encodeAsHTMLAttribute(stringTime1) + "\" size=\"22\" maxlength=\"40\" title=\"" + XML.encodeAsHTMLAttribute(stringTimeTooltip) + "\"><br><strong>",
            "</strong>\n<input type=\"text\" name=\"units\" value=\"" + XML.encodeAsHTMLAttribute(units1) + "\" size=\"37\" maxlength=\"60\" title=\"" + XML.encodeAsHTMLAttribute(unitsTooltip) + "\">."
        )}
        <br>
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(toNumeric)
            @if(tError == null)
                <br><span class="N successColor">${toNumericResult}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/time.html" name="toString">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_STRING_TIME, language),
            "</strong>\n<br><input type=\"text\" name=\"n\" value=\"" + XML.encodeAsHTMLAttribute(n2) + "\" size=\"15\" maxlength=\"30\" title=\"" + XML.encodeAsHTMLAttribute(numberTooltip) + "\">\n<input type=\"text\" name=\"units\" value=\"" + XML.encodeAsHTMLAttribute(units2) + "\" size=\"37\" maxlength=\"60\" title=\"" + XML.encodeAsHTMLAttribute(unitsTooltip) + "\">\n<br><strong>"
        )}
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(toString)
            @if(tError == null)
                <br><span class="successColor">${toStringResult}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/time.html" name="cleanString">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_ANY_STRING_TIME, language),
            "</strong>\n<input type=\"text\" name=\"stringTime\" value=\"" + XML.encodeAsHTMLAttribute(stringTime3) + "\" size=\"22\" maxlength=\"40\" title=\"" + XML.encodeAsHTMLAttribute(stringTimeTooltip) + "\">\n<br><strong>"
        )}
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(cleanString)
            @if(tError == null)
                <br><span class="successColor">${cleanStringResult}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/time.html" name="cleanUnits">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_PROPER_TIME_UNITS, language),
            "</strong>\n<input type=\"text\" name=\"units\" value=\"" + XML.encodeAsHTMLAttribute(units4) + "\" size=\"37\" maxlength=\"60\" title=\"" + XML.encodeAsHTMLAttribute(unitsTooltip) + "\">\n<br><strong>"
        )}
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(cleanUnits)
            @if(tError == null)
                <br><span class="successColor">${cleanUnitsResult}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <br><a rel="bookmark" href="${tErddapUrl}/convert/time.html">${TemplateHelper.getMessage(Message.RESET_THE_FORM, language)}</a>
    <p>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}</p>
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_TIME_NOTES, language), tErddapUrl, TemplateHelper.getMessage(Message.CONVERT_TIME_UNITS_HELP, language))}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_TIME_SERVICE, language), tErddapUrl)}
</div>
`
)
