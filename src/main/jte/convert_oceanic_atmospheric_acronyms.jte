@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.XML
@import jakarta.servlet.http.HttpServletRequest
@import java.text.MessageFormat

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/oceanicAtmosphericAcronyms.html"
@param HttpServletRequest request = null

@param String acronymValue = "NOAA"
@param String acronymTooltip = "The acronym, for example, \"NOAA\"."
@param boolean toFullName = false
@param String answerAcronym = ""
@param String answerFullName = ""

@param String fullNameSelectHtml = ""
@param boolean toAcronym = false

@param String acronymFileTypes = ""
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
    <h2>${TemplateHelper.getMessage(Message.CONVERT_OA_ACRONYMS_TO_FROM, language)}</h2>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_OA_ACRONYMS_INTRO, language)}

    @template.components.noscript()

    <form method="GET" action="${tErddapUrl}/convert/oceanicAtmosphericAcronyms.html" name="getFullName">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_A_FULL_NAME, language),
            "</strong>\n<input type=\"text\" name=\"acronym\" value=\"" + XML.encodeAsHTMLAttribute(acronymValue) + "\" size=\"20\" maxlength=\"40\" title=\"" + XML.encodeAsHTMLAttribute(acronymTooltip) + "\">\n<strong>"
        )}
        &nbsp;&nbsp;
        <button type="submit" value="Convert">
            <strong>${TemplateHelper.getMessage(Message.CONVERT, language)}</strong>
        </button>
        @if(toFullName)
            @if(tError == null)
                <br><span class="successColor">${answerAcronym} = ${answerFullName}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <form method="GET" action="${tErddapUrl}/convert/oceanicAtmosphericAcronyms.html" name="getAcronym">
        $unsafe{MessageFormat.format(
            TemplateHelper.getMessage(Message.CONVERT_TO_AN_ACRONYM, language),
            "<br></strong>\n" + fullNameSelectHtml + "\n<br><strong>"
        )}
        @if(toAcronym)
            @if(tError == null)
                <br><span class="successColor">${answerFullName} = ${answerAcronym}</span>
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @else
            <br>&nbsp;
        @endif
        <br>
    </form>

    <p><a rel="bookmark" href="${tErddapUrl}/convert/oceanicAtmosphericAcronyms.html">${TemplateHelper.getMessage(Message.RESET_THE_FORM, language)}</a></p>
    <p>$unsafe{TemplateHelper.getMessage(Message.CONVERT_BYPASS, language)}</p>
    <p>Or, view/download the entire oceanic/atmospheric acronyms list in these file types:<br>
    $unsafe{acronymFileTypes}</p>
    $unsafe{TemplateHelper.getMessage(Message.CONVERT_OA_ACRONYMS_NOTES, language)}
    $unsafe{MessageFormat.format(TemplateHelper.getMessage(Message.CONVERT_OA_ACRONYMS_SERVICE, language), tErddapUrl)}
</div>
`
)
