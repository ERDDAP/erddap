@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.String2
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/units.html"
@param HttpServletRequest request = null

@param String tUdunits = ""
@param String rUcum = ""
@param String tUcum = ""
@param String rUdunits = ""
@param String tStandardizeUdunits = ""
@param String rStandardizeUdunits = ""
@param String unitsStandard = "UDUNITS"

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <h2>${TemplateHelper.getMessage(Message.CONVERT_UNITS, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_UNITS_INTRO, language)}

    <form method="GET" action="${tErddapUrl}/convert/units.html" name="getUcum">
        ${TemplateHelper.getMessage(Message.CONVERT_FROM_UDUNITS_TO_UCUM, language)}
        <br>UDUNITS:
        <input type="text" name="UDUNITS" value="${String2.isSomething(tUdunits) ? tUdunits : String2.isSomething(rStandardizeUdunits) ? rStandardizeUdunits : String2.isSomething(rUdunits) ? rUdunits : "degC/meter"}" size="40" maxlength="100" title="For example, degC/meter">
        <button type="submit" value="Convert to UCUM">
            <strong>${TemplateHelper.getMessage(Message.CONVERT_TO_UCUM, language)}</strong>
        </button>
        @if(String2.isSomething(tUdunits))
            <br><span class="successColor">UDUNITS "${tUdunits}" &rarr; UCUM "${rUcum}"</span>
        @else
            <br>&nbsp;
        @endif
    </form>

    <form method="GET" action="${tErddapUrl}/convert/units.html" name="getUdunits">
        ${TemplateHelper.getMessage(Message.CONVERT_FROM_UCUM_TO_UDUNITS, language)}
        <br>UCUM:
        <input type="text" name="UCUM" value="${String2.isSomething(tUcum) ? tUcum : String2.isSomething(rUcum) ? rUcum : "Cel.m-1"}" size="40" maxlength="100" title="For example, Cel.m-1">
        <button type="submit" value="Convert to UDUNITS">
            <strong>${TemplateHelper.getMessage(Message.CONVERT_TO_UDUNITS, language)}</strong>
        </button>
        @if(String2.isSomething(tUcum))
            <br><span class="successColor">UCUM "${tUcum}" &rarr; UDUNITS "${rUdunits}"</span>
        @else
            <br>&nbsp;
        @endif
    </form>

    <form method="GET" action="${tErddapUrl}/convert/units.html" name="standarizeUdunits">
        <strong>${TemplateHelper.getMessage(Message.OR_COMMA, language)}</strong>
        ${TemplateHelper.getMessage(Message.CONVERT_STANDARDIZE_UDUNITS, language)}
        <br>UDUNITS:
        <input type="text" name="STANDARDIZE_UDUNITS" value="${String2.isSomething(tStandardizeUdunits) ? tStandardizeUdunits : String2.isSomething(rUdunits) ? rUdunits : String2.isSomething(tUdunits) ? tUdunits : "degC/meter"}" size="40" maxlength="100" title="For example, deg_C/meter">
        <button type="submit" value="Standardize UDUNITS">
            <strong>${TemplateHelper.getMessage(Message.CONVERT_STANDARDIZE_UDUNITS, language)}</strong>
        </button>
        @if(String2.isSomething(tStandardizeUdunits))
            <br><span class="successColor">UDUNITS "${tStandardizeUdunits}" &rarr; UDUNITS "${rStandardizeUdunits}"</span>
        @else
            <br>&nbsp;
        @endif
    </form>

    <p>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}</p>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_UNITS_NOTES, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_UNITS_SERVICE, language), tErddapUrl)}
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_UNITS_COMPARISON, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_UNITS_FILTER, language), tErddapUrl, unitsStandard)}
</div>
`
)
