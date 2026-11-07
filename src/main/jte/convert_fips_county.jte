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
@param String endOfRequest = "convert/fipscounty.html"
@param HttpServletRequest request = null

@param String codeValue = "06053"
@param String codeTooltip = "The 5-digit FIPS county code, for example, \"06053\"."
@param boolean toCounty = false
@param String answerCode = ""
@param String answerCounty = ""

@param String countySelectHtml = ""
@param boolean toCode = false

@param String fipsFileTypes = ""
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
    <h2>${TemplateHelper.getMessage(Message.CONVERT_FIPS_COUNTY, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_FIPS_COUNTY_INTRO, language)}

    <form method="GET" action="${tErddapUrl}/convert/fipscounty.html" name="getCounty">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_A_COUNTY_NAME, language),
            "</strong>\n<input type=\"text\" name=\"code\" value=\"" + XML.encodeAsHTMLAttribute(codeValue) + "\" size=\"10\" maxlength=\"15\" title=\"" + XML.encodeAsHTMLAttribute(codeTooltip) + "\">\n<strong>"
        )}
        &nbsp;&nbsp;
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(toCounty)
            @if(tError == null)
                <br><span class="successColor">${answerCode} = ${answerCounty}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/fipscounty.html" name="getCode">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_A_FIPS_CODE, language),
            "</strong>\n" + countySelectHtml + "\n<strong>"
        )}
        @if(toCode)
            @if(tError == null)
                <br><span class="successColor">${answerCounty} = ${answerCode}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <p><a rel="bookmark" href="${tErddapUrl}/convert/fipscounty.html">${TemplateHelper.getMessage(Message.RESET_THE_FORM, language)}</a></p>
    <p>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}</p>
    <p>Or, view/download the entire FIPS county list in these file types:<br>
    $unsafe{fipsFileTypes}</p>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_FIPS_COUNTY_NOTES, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_FIPS_COUNTY_SERVICE, language), tErddapUrl)}
</div>
`
)
